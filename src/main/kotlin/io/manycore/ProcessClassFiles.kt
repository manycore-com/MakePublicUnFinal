package io.manycore

import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.ClassGen
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class ProcessClassFiles {

    private val inputDir: File?
    private val outputDir: File?

    private val filename: String?
    private val inStream: InputStream?
    private val outStream: OutputStream?

    constructor(inputDir: File, outputDir: File) {
        assert (inputDir.exists())
        assert (outputDir.parentFile.exists())

        this.inputDir = inputDir
        this.outputDir = outputDir

        this.filename = null
        this.inStream = null
        this.outStream = null
    }

    // Note: you need to close streams outside this class.
    constructor(filename: String, inStream: InputStream, outStream: OutputStream) {
        this.inputDir = null
        this.outputDir = null

        this.filename = filename
        this.inStream = inStream
        this.outStream = outStream
    }

    fun process() {
        if (inStream != null) {
            processClass(filename!!, inStream, outStream!!)
            println("Number of .class files processed: 1")
            return
        }

        if (inputDir!!.isFile) {
            assert(! outputDir!!.isDirectory)
            processClass(inputDir, outputDir)
            println("Number of .class files processed: 1")
            return
        }

        val lenToRemove = inputDir.absolutePath.length
        var nbrFilesProcessed = 0
        inputDir.walk().forEach fit@ {
            if (! it.isFile) {
                return@fit
            }

            if (! it.path.endsWith(".class")) {
                return@fit
            }

            var targetFile = File(outputDir!!.absolutePath + File.separator + it.absolutePath.drop(lenToRemove))
            if (!targetFile.parentFile.exists()) {
                targetFile.parentFile.mkdirs()
            }

            processClass(it, targetFile)
            nbrFilesProcessed++
        }

        println("Number of .class files processed: " + nbrFilesProcessed)
    }

    private fun processClass(inFile: File, outFile: File) {
        val outFileTmp = File(outFile.absolutePath + "_tmp")
        val inStream = FileInputStream(inFile.absolutePath)
        val outStream = FileOutputStream(outFileTmp)
        processClass(inFile.path, inStream, outStream)
        outStream.flush()
        outStream.close()
        if (outFile.isFile) {
            outFile.delete()
        }
        outFileTmp.renameTo(outFile)
    }

    // Open and close all streams outside!
    private fun processClass(filename: String, inStream: InputStream, outStream: OutputStream) {
        var javaClass: JavaClass = processJavaClass(ClassParser(inStream, filename).parse())
        outStream.write(javaClass.bytes)
    }

    private fun processJavaClass(jc: JavaClass): JavaClass {
        var cg: ClassGen = ClassGen(jc)
        cg.isPublic(true)
        cg.isProtected(false)
        cg.isPrivate(false)
        cg.isFinal(false)

        cg.methods.forEach {
            it.isPublic(true)
            it.isProtected(false)
            it.isPrivate(false)
            it.isFinal(false)
        }

        cg.fields.forEach {
            it.isPublic(true)
            it.isProtected(false)
            it.isPrivate(false)
            it.isFinal(false)
        }

        return cg.javaClass
    }

}