package requests;

import java.nio.ByteBuffer;

import static parameters.OpCode.ERROR;

public class ErrorPacket extends Packet {

    private final String message;
    private final short error;

    public ErrorPacket(short error, String message) {
        super(ERROR);
        this.error = error;
        this.message = message;
    }

    @Override
    public int size() {
        return Character.BYTES + Character.BYTES + message.length() + 1;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(size());
        buf.putShort(getOpCode());
        buf.putShort(error);
        buf.put(message.getBytes());
        buf.put((byte) 0);
        return buf.array();
    }
}
