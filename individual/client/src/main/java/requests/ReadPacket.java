package requests;

import fields.OpCode;

import java.nio.ByteBuffer;

public class ReadPacket extends Packet {
    private final String filename;
    private final String mode;

    public ReadPacket(String filename, String mode) {
        super(OpCode.RRQ);
        this.filename = filename;
        this.mode = mode;
    }

    public int size() {
        return Character.BYTES + filename.length() + 1 + mode.length() + 1;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(size());
        buf.putInt(getOpCode());
        buf.put(filename.getBytes());
        buf.put((byte)0);
        buf.put(mode.getBytes());
        buf.put((byte)0);
        return buf.array();
    }
}
