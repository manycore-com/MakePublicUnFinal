package io.manycore

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class ProcessJarFiles {

    private var inputDir: File
    private var outputDir: File
    private var nbrJarFilesProcessed: Int = 0

    constructor(inputDir: File, outputDir: File) {
        assert (inputDir.exists())
        if (inputDir.isFile) {
            assert(outputDir.parentFile.isDirectory)
        } else {
            assert(outputDir.isDirectory)
        }

        this.inputDir = inputDir
        this.outputDir = outputDir
    }

    fun process() {
        if (inputDir.isFile) {
            assert(! outputDir.isDirectory)
            processJar(inputDir, outputDir)
            nbrJarFilesProcessed++
            return
        }

        val lenToRemove = inputDir.absolutePath.length
        inputDir.walk().forEach fit@ {
            if (! it.isFile) {  // ie we're not going recursively.
                return@fit
            }

            if (! (it.path.lowercase().endsWith(".jar") || it.path.lowercase().endsWith(".war"))) {  // war et al too?
                return@fit
            }

            var targetFile = File(outputDir.absolutePath + File.separator + it.absolutePath.drop(lenToRemove))
            if (targetFile.exists()) {
                targetFile.delete()
            }

            processJar(it, targetFile)
            nbrJarFilesProcessed++
        }
    }

    private fun processJar(inFile: File, outFile: File) {
        println("processJar")
        assert(inFile.isFile)

        // if infile == outfile
        val tmpOutFile = File(outFile.absolutePath + ".build")

        val fileInputStream = FileInputStream(inFile)
        fileInputStream.use {
            val bufferedInputStream = BufferedInputStream(fileInputStream)
            bufferedInputStream.use {
                val zipInputStream = ZipInputStream(bufferedInputStream)
                zipInputStream.use {
                    val fileOutputStream = FileOutputStream(tmpOutFile)
                    fileOutputStream.use {
                        val zipOutputStream = ZipOutputStream(fileOutputStream)
                        zipOutputStream.use {
                            processJar(zipInputStream, zipOutputStream)
                            fileOutputStream.flush()
                        }
                    }
                }
            }
        }

        if (outFile.exists()) {
            outFile.delete()
        }

        tmpOutFile.renameTo(outFile)
    }

    private fun processJar(zipInputStream: ZipInputStream, zipOutputStream: ZipOutputStream) {
        var inZipEntry: ZipEntry? = zipInputStream.nextEntry
        while (inZipEntry != null) {
            if (! inZipEntry.isDirectory) {
                zipOutputStream.putNextEntry(ZipEntry(inZipEntry.name))
                if (inZipEntry.name.lowercase().endsWith(".jar") || inZipEntry.name.lowercase().endsWith(".war")) {
                    println("recursive jar call! " + inZipEntry.name)

                    // create a zipInputStream from the
                    var recursiveZipInputStream = ZipInputStream(zipInputStream)
                    var recursiveZipOutputStream = ZipOutputStream(zipOutputStream)
                    processJar(recursiveZipInputStream, recursiveZipOutputStream)
                } else if (inZipEntry.name.lowercase().endsWith(".class")) {
                    ProcessClassFiles(inZipEntry.name, zipInputStream, zipOutputStream).process()
                } else {
                    // Default: just copy. It's assets etc.
                    zipInputStream.copyTo(zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }

            inZipEntry = zipInputStream.nextEntry
        }
    }

}