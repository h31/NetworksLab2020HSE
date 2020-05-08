package ru.hse.lyubortk.tftp.server

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream

// LF -> CR,LF
// CR -> CR,NUL
class NetasciiInputStream(inner: InputStream) : InputStream() {
    private val inner = BufferedInputStream(inner)
    private var nextByte: Int? = null

    override fun read(): Int {
        val next = nextByte
        if (next != null) {
            nextByte = null
            return next
        }

        return when (val innerByte = inner.read()) {
            '\n'.toInt() -> {
                nextByte = '\n'.toInt()
                '\r'.toInt()
            }
            '\r'.toInt() -> {
                nextByte = 0
                '\r'.toInt()
            }
            else -> innerByte
        }
    }

    override fun close() {
        inner.close()
    }
}
// CR,NUL -> CR
// CR,LF => LF
class NetasciiOutputStream(inner: OutputStream) : OutputStream() {
    private val inner = BufferedOutputStream(inner)
    private var previousByte: Int? = null

    override fun write(b: Int) {
        val previous = previousByte
        if (previous == null) {
            previousByte = b
            return
        }
        previousByte = when (previous to b) {
            ('\r'.toInt() to 0) -> '\r'.toInt()
            ('\r'.toInt() to '\n'.toInt()) -> '\n'.toInt()
            else -> {
                inner.write(previous)
                b
            }
        }
    }

    override fun flush() {
        inner.flush()
    }

    override fun close() {
        val prev = previousByte
        if (prev != null) {
            inner.write(prev)
            previousByte = null
        }
        inner.close()
    }
}