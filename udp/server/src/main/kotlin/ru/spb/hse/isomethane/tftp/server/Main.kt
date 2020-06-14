package ru.spb.hse.isomethane.tftp.server

import java.io.File
import java.util.*

fun main(args: Array<String>) {
    if (args.isEmpty() || args.size > 2) {
        println("Arguments: port [root directory]")
        return
    }
    val port = args[0].toInt()
    val root = File(if (args.size == 1) "." else args[1])
    val server = Server(port, root)
    println("Server started on port $port.")
    val scanner = Scanner(System.`in`)
    do {
        println("Write exit to stop server.")
    } while (scanner.nextLine() != "exit")
    server.stop()
}