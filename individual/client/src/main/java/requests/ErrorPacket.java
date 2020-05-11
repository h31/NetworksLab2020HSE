package requests;

import fields.OpCode;

import java.nio.ByteBuffer;

public class ErrorPacket extends Packet {

    private final String message;
    private final short error;

    public ErrorPacket(short error, String message) {
        super(OpCode.ERROR);
        this.error = error;
        this.message = message;
    }

    @Override
    public int size() {
        return Character.BYTES + Character.BYTES + message.length() + 1;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(size());
        buf.putShort(getOpCode());
        buf.putShort(error);
        buf.put(message.getBytes());
        buf.put((byte)0);
        return buf.array();
    }
}
