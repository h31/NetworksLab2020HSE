package ru.hse.anstkras.tftp

fun main(args: Array<String>) {
    try {
        when {
            args[0] == "client" -> {
                val host = args[1]
                val port = args[2].toInt()
                val mode = TFTPMode.valueOf(args[3].toUpperCase())
                val client = Client(host, port, mode)
                while (true) {
                    val command = readLine()!!
                    if (command == "exit") {
                        return
                    }
                    val commandSplitted = command.split(" ")
                    if (commandSplitted[0] == "read") {
                        client.readFile(commandSplitted[1])
                    } else if (commandSplitted[0] == "write") {
                        client.writeFile(commandSplitted[1])
                    } else {
                        println(
                            "usage: read/write filename\n" +
                                    "exit"
                        )

                    }
                }
            }
            args[0] == "server" -> {
                val port = args[1].toInt()
                val server = Server(port)
                server.start()
            }
            else -> {
                println("usage: ./tftp server port or ./tftp client host port mode")
            }
        }
    } catch (e : Exception) {
        println("usage: ./tftp server port or ./tftp client host port mode")
        e.printStackTrace()
    }
}