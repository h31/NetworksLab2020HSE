import fields.OpCode;
import requests.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

class InPacket {
    private static final int PACKET_SIZE = 512;
    private ByteBuffer buffer;
    private DatagramPacket packet;
    private DatagramSocket socket;

    InPacket(DatagramSocket socket) {
        this.socket = socket;
    }

    private short getShort() {
        return buffer.getShort();
    }

    private String getString() {
        StringBuilder builder = new StringBuilder();
        char ch;
        while ((ch = (char) buffer.get()) != '\0') {
            builder.append(ch);
        }
        return builder.toString();
    }

    private String getData() {
        StringBuilder builder = new StringBuilder();
        while (buffer.position() != buffer.limit()) {
            builder.append(buffer.get());
        }
        return builder.toString();
    }

    public Packet receive() throws Exception {
        buffer = ByteBuffer.allocate(PACKET_SIZE);
        packet = new DatagramPacket(buffer.array(), buffer.limit());
        socket.receive(packet);
        return process();
    }

    private Packet process() throws Exception {
        var opcode = buffer.getShort();
        switch (opcode) {
            case OpCode.RRQ:
                return new ReadPacket(getString(), getString());

            case OpCode.WRQ:
                return new WritePacket(getString(), getString());

            case OpCode.DATA:
                return new DataPacket(getShort(), getData());

            case OpCode.ACK:
                return new AckPacket(getShort());

            case OpCode.ERROR:
                return new ErrorPacket(getShort(), getData());

            default:
                throw new Exception();
        }
    }
}