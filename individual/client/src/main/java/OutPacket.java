import requests.Packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class OutPacket {
    private static final int PACKET_SIZE = 512;
    private DatagramPacket packet;
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);

    OutPacket(DatagramSocket socket, InetAddress address, int port) {
        this.socket = socket;
        this.address = address;
        this.port = port;
    }

    private OutPacket append(byte data) {
        this.buffer.put(data);
        return this;
    }

    private OutPacket append(byte[] data) {
        buffer.put(data);
        return this;
    }

    private OutPacket append(short data) {
        buffer.putShort(data);
        return this;
    }

    private byte[] getByteArray() {
        return buffer.array();
    }

    OutPacket send(Packet packet) throws Exception {
        this.packet = new DatagramPacket(packet.serialize(), packet.size(), address, port);

        return this;
    }

    OutPacket addOpcode(short opcode) {
        return append(opcode);
    }

    OutPacket addBlockNumber(short blockNumber) {
        return append(blockNumber);
    }

    OutPacket addDataBytes(byte[] data) {
        return append(data);
    }

    OutPacket addString(String string) {
        return append(string.getBytes());
    }

    OutPacket addErrorCode(short code) {
        return append(code);
    }

    OutPacket addNullByte() {
        return append((byte) 0x00);
    }
}