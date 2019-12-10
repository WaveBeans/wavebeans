package io.wavebeans.cli

import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import java.io.OutputStream
import java.io.PrintStream


fun main() {
    val terminal = TerminalBuilder
            .builder()
            .dumb(true)
            .build()!!
    val lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .build()!!

    Cli(lineReader, System.out).run()
}

class Cli(private val lineReader: LineReader, outputStream: OutputStream) : Runnable {

    private val out = PrintStream(outputStream)

    override fun run() {
    }
}