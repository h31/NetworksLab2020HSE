package ru.spbau.smirnov.tftp.client

import com.beust.jcommander.JCommander
import java.net.InetAddress

class Client(
    host: String,
    private val rootPath: String,
    private val serverPort: Int
) {
    private var currentMode = TFTPMode.OCTET
    private val serverAddress = InetAddress.getByName(host)

    fun run() {
        while (true) {
            print("tftp> ")
            val input = readLine() ?: break
            when {
                input == "?" -> {
                    println(
                        """
                            | put <FILENAME> -- WRQ file to server
                            | get <FILENAME> -- RRQ file from server
                            | octet -- change mode to octet
                            | netascii -- change mode to netascii
                        """.trimMargin()
                    )
                }
                input.startsWith("put ") -> {
                    val filename = input.substring(4..input.lastIndex)
                    Connection(
                        serverAddress,
                        serverPort,
                        WriteRequest(
                            filename,
                            currentMode
                        ),
                        rootPath
                    ).run()
                }
                input.startsWith("get ") -> {
                    val filename = input.substring(4..input.lastIndex)
                    Connection(
                        serverAddress,
                        serverPort,
                        ReadRequest(
                            filename,
                            currentMode
                        ),
                        rootPath
                    ).run()
                }
                input == "octet" -> {
                    currentMode = TFTPMode.OCTET
                }
                input == "netascii" -> {
                    currentMode = TFTPMode.NETASCII
                }
                else -> {
                    println("Unexpected input. Type \"?\" to see all commands")
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    val arguments = Arguments()
    JCommander.newBuilder()
        .addObject(arguments)
        .build()
        .parse(*args)

    Client(
        arguments.host,
        arguments.rootPath,
        arguments.port
    ).run()
}

