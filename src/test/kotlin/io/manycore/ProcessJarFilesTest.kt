package io.manycore

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile

class ProcessJarFilesTest {

    @Test
    fun test() {
        // Note: File has two jars that will process recursively
        val inputJar = File("src/test/java/io/manycore/files/privateerjar/privateer.jar")

        val outputJar = File("src/test/kotlin/io/manycore/jarsOut/klaskatt.jar")
        if (outputJar.parentFile.exists()) {
            outputJar.parentFile.deleteRecursively()
        }
        outputJar.parentFile.mkdirs()

        val p = ProcessJarFiles(inputJar, outputJar)
        p.process()

        Assertions.assertTrue(outputJar.isFile)

        var nbrFiles: Int = 0;
        for (entry in ZipFile(outputJar).entries()) {
            if (!entry.isDirectory) {
                println(entry.name)
                nbrFiles++
            }
        }

        Assertions.assertEquals(4, nbrFiles)

        outputJar.parentFile.deleteRecursively()
    }

}