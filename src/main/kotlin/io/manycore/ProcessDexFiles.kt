package io.manycore

import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.builder.instruction.BuilderInstruction3rc
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction35c
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction3rc
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Field
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.MethodImplementation
import org.jf.dexlib2.immutable.ImmutableClassDef
import org.jf.dexlib2.immutable.ImmutableDexFile
import org.jf.dexlib2.immutable.ImmutableField
import org.jf.dexlib2.immutable.ImmutableMethod
import java.io.File
import java.io.IOException

class ProcessDexFiles {

    private var inputDir: File
    private var outputDir: File
    private var minSdkVersion: Int

    constructor(inputDir: File, outputDir: File, minSdkVersion: Int) {
        assert (inputDir.exists())
        assert (outputDir.exists())

        this.inputDir = inputDir
        this.outputDir = outputDir
        this.minSdkVersion = minSdkVersion
    }

    fun process() {
        walker(inputDir)
    }

    private fun walker(inputFile: File) {
        println ("looking in: " + inputFile.path)
        val lenToRemove = inputDir.absolutePath.length
        inputFile.walk().forEach fit@ {
            println("considering: " + it)

            if (inputFile == it) {
                return@fit
            }

            if (it.isDirectory) {
                walker(it)
            } else {
                if (!it.isFile) {
                    return@fit
                }

                if (!it.path.endsWith(".dex")) {
                    return@fit
                }

                var classDefs = loadDex(it.path)
                var updatedClassDefs = updateClassDefs(classDefs)
                var targetFile = File(outputDir.absolutePath + File.separator + it.absolutePath.drop(lenToRemove))

                if (!targetFile.parentFile.exists()) {
                    targetFile.parentFile.mkdirs()
                }

                println("Target would be " + targetFile.path)

                //FIXME support other API
                val imf = ImmutableDexFile(Opcodes.forApi(minSdkVersion), updatedClassDefs)
                DexFileFactory.writeDexFile(targetFile.absolutePath, imf)
            }
        }
    }


    @Throws(IOException::class)
    private fun loadDex(dex: String): List<ClassDef> {
        val res: MutableList<ClassDef> = ArrayList()

        val dexFile = DexFileFactory.loadDexFile(dex, Opcodes.forApi(minSdkVersion))
        for (cd in dexFile.classes) {  // ClassDef
            res.add(cd)
        }
        return res
    }

    private fun updateClassDefs(loadedClassDefs: List<ClassDef>): List<ClassDef> {
        val res: MutableList<ClassDef> = ArrayList()

        for (cd in loadedClassDefs) {
            var newMethods = publicFunctions(cd)
            var newFields = publicFields(cd)

            var accessFlags = makePublic(cd.accessFlags)

            var newCd = ImmutableClassDef(
                cd.type,
                accessFlags,
                cd.superclass,
                cd.interfaces,
                cd.sourceFile,
                cd.annotations,
                newFields,
                newMethods
            )

            res.add(newCd)
        }

        return res
    }

    private fun publicFields(cd: ClassDef): MutableList<Field> {
        var newFields: MutableList<Field> = ArrayList()

        for (f in cd.fields) {
            var accessFlags = makePublic(f.accessFlags)

            val immutableField = ImmutableField(
                cd.type,
                f.name,
                f.type,
                accessFlags,
                f.initialValue,
                f.annotations,
                f.hiddenApiRestrictions
            )

            newFields.add(immutableField)
        }

        return newFields
    }

    private fun publicFunctions(cd: ClassDef): MutableList<Method> {
        var newMethods: MutableList<Method> = ArrayList()

        for (m in cd.methods) {
            if (null != m.implementation) {
                var mmi = updateImplementation(m)


                var accessFlags = makePublic(m.accessFlags)

                // Create new Method
                val immutableMethod = ImmutableMethod(
                    cd.type,   // defining class
                    m.name,    // name
                    m.parameters.asIterable(),
                    m.returnType,
                    accessFlags,
                    m.annotations,
                    m.hiddenApiRestrictions,
                    mmi
                )

                newMethods.add(immutableMethod)
            } else {
                newMethods.add(m)
            }
        }

        return newMethods
    }

    private fun makePublic(orgAccessFlags: Int): Int {
        var accessFlags = orgAccessFlags;
        accessFlags = setFlag(AccessFlags.PRIVATE.value, false, accessFlags)
        accessFlags = setFlag(AccessFlags.PROTECTED.value, false, accessFlags)
        accessFlags = setFlag(AccessFlags.PUBLIC.value, true, accessFlags)
        return accessFlags
    }

    private fun setFlag(flag: Int, set: Boolean, access_flags: Int): Int {
        if (access_flags and flag != 0) { // Flag is set already
            if (!set) {
                return access_flags xor flag
            }
        } else { // Flag not set
            if (set) {
                return access_flags or flag
            }
        }

        return access_flags
    }

    private fun updateImplementation(m: Method): MethodImplementation? {
        if (m.implementation == null) {
            return null
        }

        // Private calls are done with invoke-direct as they don't need a method pointer via vtab.
        // When we change private to non-private we also need to change invoke-direct to invoke-virtual.
        // This is not necessary with JBC, it's Dalvik specific behavior.
        val mmi = MutableMethodImplementation(m.implementation!!)
        var instructions = m.implementation!!.instructions.toMutableSet()
        for ((index, instr) in instructions.withIndex()) {
            if (instr.opcode.name.equals("invoke-direct")) {
                val iinstr = instr as DexBackedInstruction35c
                if ((iinstr.reference as DexBackedMethodReference).name != "<init>") {
                    val newInstruction = BuilderInstruction35c(Opcode.INVOKE_VIRTUAL, iinstr.registerCount, iinstr.registerC, iinstr.registerD, iinstr.registerE, iinstr.registerF, iinstr.registerG, iinstr.reference)
                    mmi.replaceInstruction(index, newInstruction)
                }
            } else if (instr.opcode.name.equals("invoke-direct/range")) {
                val iinstr = instr as DexBackedInstruction3rc
                if ((iinstr.reference as DexBackedMethodReference).name != "<init>") {
                    val newInstruction = BuilderInstruction3rc(Opcode.INVOKE_VIRTUAL_RANGE, iinstr.startRegister, iinstr.registerCount, iinstr.reference)
                    mmi.replaceInstruction(index, newInstruction)
                }
            }
        }

        return mmi
    }


}