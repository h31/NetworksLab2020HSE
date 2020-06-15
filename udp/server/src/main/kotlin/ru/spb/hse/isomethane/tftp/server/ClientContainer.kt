package ru.spb.hse.isomethane.tftp.server

import java.io.*
import java.net.DatagramPacket
import java.net.SocketAddress

class ClientContainer(private val rootDirectory: File) {
    private val clientSessions = hashMapOf<SocketAddress, ClientSession>()

    fun isEmpty() = clientSessions.isEmpty()

    fun processPacket(packet: DatagramPacket, time: Long): DatagramPacket? {
        val session = clientSessions[packet.socketAddress]
        return if (session == null) {
            when (val request = readTFTP(packet.data, packet.length)) {
                is ReadRequest -> {
                    val newSession = ReadSession(request, packet.socketAddress)
                    clientSessions[packet.socketAddress] = newSession
                    newSession.updateTime(time)
                    newSession.onCreate()
                }
                is WriteRequest -> {
                    val newSession = WriteSession(request, packet.socketAddress)
                    clientSessions[packet.socketAddress] = newSession
                    newSession.updateTime(time)
                    newSession.onCreate()
                }
                else -> {
                    val data = writeTFTP(ErrorMessage(0, "Incorrect request"))
                    DatagramPacket(data, data.size, packet.socketAddress)
                }
            }
        } else {
            session.updateTime(time)
            session.processPacket(packet)
        }
    }

    fun timedOutPackets(time: Long): List<DatagramPacket> {
        clientSessions.values.forEach { it.updateTime(time) }
        return clientSessions.values.toList().mapNotNull { it.retry() }
    }

    fun removeFinished() {
        val active = clientSessions.filterValues { !it.finished }
        clientSessions.clear()
        clientSessions.putAll(active)
    }

    private abstract class ClientSession(val address: SocketAddress) {
        var lastPacket: DatagramPacket? = null
        var lastTime: Long = -1
        var currentTime: Long = -1
        var retries = 0
        var finished = false

        fun updateTime(time: Long) {
            currentTime = time
        }

        abstract fun onCreate(): DatagramPacket

        abstract fun processPacket(packet: DatagramPacket): DatagramPacket?

        open fun retry(): DatagramPacket? {
            return if (currentTime - lastTime > Constants.RETRANSMISSION_TIMEOUT) {
                retries++
                if (retries >= Constants.MAX_RETRIES) {
                    finished = true
                }
                lastPacket
            } else {
                null
            }
        }

        protected fun updatePacket(packet: DatagramPacket): DatagramPacket {
            lastPacket = packet
            lastTime = currentTime
            return packet
        }
    }

    private inner class ReadSession(private val request: ReadRequest, address: SocketAddress) : ClientSession(address) {
        val file = File(rootDirectory, request.fileName)
        var block = 0
        var lastBlock = false
        lateinit var input: InputStream

        override fun onCreate(): DatagramPacket {
            return if (file.canRead() && file.length() <= Constants.MAX_FILE_SIZE) {
                initInput()
                return updatePacket(nextBlock())
            } else {
                finished = true
                val data = if (!file.exists()) {
                    writeTFTP(ErrorMessage(1, "No such file"))
                } else if (file.length() > Constants.MAX_FILE_SIZE) {
                    writeTFTP(ErrorMessage(0, "File is too large"))
                } else {
                    writeTFTP(ErrorMessage(2, "Access denied"))
                }
                DatagramPacket(data, data.size, address)
            }
        }

        override fun processPacket(packet: DatagramPacket): DatagramPacket? {
            val request = readTFTP(packet.data, packet.length)
            if (request is Acknowledgement && request.block == block) {
                if (lastBlock) {
                    finished = true
                    return null
                }
                return updatePacket(nextBlock())
            }
            return null
        }

        private fun initInput() {
            input = if (request.mode == "netascii") {
                val content = ByteArrayOutputStream()
                val writer = content.writer(Charsets.US_ASCII)
                val lines = String(file.readBytes(), Charsets.US_ASCII).lines()
                lines.forEachIndexed { index, line ->
                    writer.append(line)
                    if (index != lines.size - 1) {
                        writer.append("\r\n")
                    }
                }
                writer.close()
                ByteArrayInputStream(content.toByteArray())
            } else {
                ByteArrayInputStream(file.readBytes())
            }
        }

        private fun nextBlock(): DatagramPacket {
            val part = input.readNBytes(Constants.DATA_SIZE)
            if (part.size < Constants.DATA_SIZE) {
                lastBlock = true
            }
            val data = writeTFTP(Data(++block, part))
            return DatagramPacket(data, data.size, address)
        }
    }

    private inner class WriteSession(private val request: WriteRequest, address: SocketAddress) : ClientSession(address) {
        val file = File(rootDirectory, request.fileName)
        var block = 0
        var lastBlock = false
        val output = ByteArrayOutputStream()

        override fun onCreate(): DatagramPacket {
            finished = true
            val data = try {
                file.createNewFile()
                finished = false
                return updatePacket(nextBlock())
            } catch (e: SecurityException) {
                writeTFTP(ErrorMessage(2, "Access denied"))
            } catch (e: IOException) {
                writeTFTP(ErrorMessage(0, e.message ?: "Unknown I/O exception"))
            }
            return DatagramPacket(data, data.size, address)
        }

        override fun processPacket(packet: DatagramPacket): DatagramPacket? {
            val request = readTFTP(packet.data, packet.length)
            if (request is Data) {
                if (request.block == block - 1) {
                    return lastPacket
                }
                if (request.block == block) {
                    output.write(request.data)
                    if (request.data.size < Constants.DATA_SIZE) {
                        writeFile()
                        lastBlock = true
                    }
                }
                return updatePacket(nextBlock())
            }
            return null
        }

        override fun retry(): DatagramPacket? {
            return if (lastBlock) {
                if (currentTime - lastTime > Constants.FULL_TIMEOUT) {
                    finished = true
                }
                null
            } else {
                val result = super.retry()
                if (finished) {
                    file.delete()
                }
                result
            }
        }

        private fun writeFile() {
            if (request.mode == "netascii") {
                val fileOutput = file.outputStream()
                val writer = fileOutput.writer(Charsets.US_ASCII)
                val lines = String(output.toByteArray(), Charsets.US_ASCII).lines()
                lines.forEachIndexed { index, line ->
                    writer.append(line)
                    if (index != lines.size - 1) {
                        writer.appendln()
                    }
                }
                writer.close()
            } else {
                file.writeBytes(output.toByteArray())
            }
        }

        private fun nextBlock(): DatagramPacket {
            val data = writeTFTP(Acknowledgement(block++))
            return DatagramPacket(data, data.size, address)
        }
    }
}
