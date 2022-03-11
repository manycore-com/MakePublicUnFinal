package io.manycore

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import java.io.File
import kotlin.system.exitProcess

data class Arguments(
    val inClassDir: String?,
    val outClassDir: String?,
    val inJarDir: String?,
    val outJarDir: String?,
) {
    override fun toString(): String {
        return "Arguments(inClassDir=$inClassDir, outClassDir=$outClassDir, inJarDir=$inJarDir, outJarDir=$outJarDir)"
    }
}

fun parseCommandLine(args: Array<String>): Arguments {

    val parser = ArgParser("Publicator")
    val inClassDir by parser.option(ArgType.String, fullName = "inClassDir", description = "directory root with input .class files")
    val outClassDir by parser.option(ArgType.String, fullName = "outClassDir", description = "directory root with output .class files. Can be same as --inClassDir")

    val inJarDir by parser.option(ArgType.String, fullName = "inJarDir", description = "directory root with input .jar files, or point to a jar file directly")
    val outJarDir by parser.option(ArgType.String, fullName = "outJarDir", description = "directory root with output .jar files. Can be same as --inClassDir")
    parser.parse(args)

    return Arguments(inClassDir, outClassDir, inJarDir, outJarDir)
}

fun main(args: Array<String>) {
    val arguments = parseCommandLine(args)

    if (arguments.inClassDir.isNullOrBlank() xor arguments.outClassDir.isNullOrBlank()) {
        println("You need to specify both inClassDir and outClassDir")
        exitProcess(-1)
    } else if (! arguments.inClassDir.isNullOrBlank() && ! arguments.outClassDir.isNullOrBlank()) {
        var pcf = ProcessClassFiles(File(arguments.inClassDir), File(arguments.outClassDir))
        pcf.process()
    }

    if (arguments.inJarDir.isNullOrBlank() xor arguments.outJarDir.isNullOrBlank()) {
        println("You need to specify both inDexDir and outDexDir")
        exitProcess(-1)
    } else if (! arguments.inJarDir.isNullOrBlank() && !arguments.outJarDir.isNullOrBlank()) {
        var pjf = ProcessJarFiles(File(arguments.inJarDir), File(arguments.outJarDir))
        pjf.process()
    }

}
