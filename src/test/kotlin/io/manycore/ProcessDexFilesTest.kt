package io.manycore

import io.manycore.debug.PrettyPrint
import io.manycore.utils.Dexes
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class ProcessDexFilesTest {

    @Test
    fun test() {

        val outputDir = File("src/test/kotlin/io/manycore/dexesOut")
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        val p = ProcessDexFiles(File("src/test/kotlin/io/manycore/dexesJustPrivateer"), outputDir, 14)
        println("asd")
        p.process()
        println("asd2")

        val dexes = Dexes(outputDir, 14)
        val cd = dexes.lookup("Lio/manycore/privatetest/Privateer;")
        var testedPrivateTest = false
        for (m in cd.methods) {
            if (m.name.equals("privateTest")) {

                val codeAsStrings = PrettyPrint.prettyPrintMethod(m!!)
                for (s in codeAsStrings) {
                    println(s)
                }

                Assertions.assertTrue(codeAsStrings[10].contains("INVOKE_VIRTUAL"))
                Assertions.assertTrue(codeAsStrings[18].contains("INVOKE_DIRECT"))
                testedPrivateTest = true

            }
        }

        Assertions.assertEquals(true, testedPrivateTest)
        outputDir.deleteRecursively()
    }

}
