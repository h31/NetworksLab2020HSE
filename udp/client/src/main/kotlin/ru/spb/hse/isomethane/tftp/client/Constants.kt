package ru.spb.hse.isomethane.tftp.client

object Constants {
    const val DATA_SIZE = 512
    const val PACKET_SIZE = 516

    const val RETRANSMISSION_TIMEOUT = 500
    const val MAX_RETRIES = 40
    const val MAX_FILE_SIZE = DATA_SIZE * (1.shl(16) - 1)
}