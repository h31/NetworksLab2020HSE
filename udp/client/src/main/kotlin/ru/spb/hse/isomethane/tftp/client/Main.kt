package ru.spb.hse.isomethane.tftp.client

import java.lang.Exception
import java.net.InetAddress
import java.util.*

fun main() {
    var toExit = false
    val scanner = Scanner(System.`in`)

    val client = Client()

    printUsage()
    while (!toExit) {
        print("> ")
        when (scanner.nextLine()) {
            "connect" -> {
                print("host: ")
                val host = scanner.nextLine()
                print("port (leave empty for default): ")
                val port = scanner.nextLine().checkEmpty()?.toInt()
                try {
                    client.connect(InetAddress.getByName(host), port)
                    println("Connected")
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
            "mode" -> {
                print("new mode (ascii or binary): ")
                when (scanner.nextLine()) {
                    "ascii" -> client.mode = "netascii"
                    "binary" -> client.mode = "octet"
                    else -> println("Unknown mode")
                }
            }
            "put" -> {
                print("local file name: ")
                val fromFile = scanner.nextLine()
                print("remote file name (leave empty for default): ")
                val toFile = scanner.nextLine().checkEmpty()
                try {
                    client.put(fromFile, toFile)
                    println("Put file successfully")
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
            "get" -> {
                print("remote file name: ")
                val fromFile = scanner.nextLine()
                print("local file name (leave empty for default): ")
                val toFile = scanner.nextLine().checkEmpty()
                try {
                    client.get(fromFile, toFile)
                    println("Got file successfully")
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
            "exit" -> toExit = true
            "help" -> printUsage()
            else -> println("Unknown command")
        }
    }
}

fun String.checkEmpty() = if (this.isEmpty()) null else this

fun printUsage() {
    println("Usage:")
    println("connect - connect to server")
    println("mode - change mode (ascii/binary), binary by default")
    println("put - send file to server")
    println("get - receive file from server")
    println("exit - stop client")
    println("help - print this list")
}