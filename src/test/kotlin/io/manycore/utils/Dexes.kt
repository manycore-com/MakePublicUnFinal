package io.manycore.utils

import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import java.io.File

class Dexes {

    private val inputDir: File
    private val minSdkVersion: Int
    private val classDefMap = mutableMapOf<String, ClassDef>()

    constructor(inputDir: File, minSdkVersion: Int) {
        assert (inputDir.exists())

        this.inputDir = inputDir
        this.minSdkVersion = minSdkVersion

        if (this.inputDir.isFile && this.inputDir.name.lowercase().endsWith(".dex")) {
            addDex(this.inputDir)
        } else if (this.inputDir.isDirectory) {
            inputDir.walk().forEach fit@ {
                if (! it.isFile) {
                    return@fit
                }

                if (! it.path.endsWith(".dex")) {
                    return@fit
                }

                addDex(it)
            }
        } else {
            throw IllegalArgumentException("Input file is not a dex? " + this.inputDir.name)
        }

    }

    fun lookup(className: String): ClassDef {
        return classDefMap.get(className)!!
    }

    private fun addDex(dexFile: File) {
        println("Loading dex: " + dexFile.name)

        val dexFile = DexFileFactory.loadDexFile(dexFile, Opcodes.forApi(minSdkVersion))
        for (cd in dexFile.classes) {  // ClassDef
            classDefMap[cd.type] = cd
            classDefMap[cd.type.substring(1, cd.type.length -1).replace('/', '.')] = cd
        }
    }

}