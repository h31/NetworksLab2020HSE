import requests.*;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

import static parameters.OpCode.*;

public class PacketFactory {
    private static final byte END = '\0';

    private static short getShort(ByteBuffer buffer) {
        return buffer.getShort();
    }

    private static String getString(ByteBuffer buffer) {
        StringBuilder builder = new StringBuilder();
        char ch;
        while ((ch = (char) buffer.get()) != END) {
            builder.append(ch);
        }
        return builder.toString();
    }

    private static byte[] getData(ByteBuffer buffer) {
        byte[] b = new byte[buffer.remaining()];
        buffer.get(b);
        return b;
    }

    public static Packet create(DatagramPacket packet) throws TftpException {
        ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
        buffer.limit(packet.getLength());
        var opcode = getShort(buffer);
        switch (opcode) {
            case RRQ:
                return new ReadPacket(getString(buffer), getString(buffer));

            case WRQ:
                return new WritePacket(getString(buffer), getString(buffer));

            case DATA:
                return new DataPacket(getShort(buffer), getData(buffer));

            case ACK:
                return new AckPacket(getShort(buffer));

            case ERROR:
                return new ErrorPacket(getShort(buffer), getString(buffer));

            default:
                throw new TftpException(String.format("Unsupported operation: %d", +opcode));
        }
    }
}
