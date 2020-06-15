package ru.spb.hse.isomethane.tftp.server

object Constants {
    const val DATA_SIZE = 512
    const val PACKET_SIZE = 516

    const val RETRANSMISSION_TIMEOUT = 1000
    const val FULL_TIMEOUT = 20000
    const val MAX_RETRIES = 20
    const val MAX_FILE_SIZE = DATA_SIZE * (1.shl(16) - 1)
}