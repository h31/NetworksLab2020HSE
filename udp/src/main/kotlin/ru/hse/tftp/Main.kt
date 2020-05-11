package ru.hse.tftp

fun main(argc: Array<String>) {
    val command = argc[0]

    val newArgs = argc.drop(1).toTypedArray()

    when (command) {
        "server" -> ru.hse.tftp.server.main(newArgs)
        "client" -> ru.hse.tftp.client.main(newArgs)

        else -> {
            println("Usage: ./gradlew run --args=\'server/client...\'")
        }
    }
}