package ru.hse.anstkras.tftp.packet

import java.nio.ByteBuffer

interface Parsable<T> {
    fun parse(byteBuffer: ByteBuffer): T
}