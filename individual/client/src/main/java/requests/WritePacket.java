package requests;

import fields.OpCode;

import java.nio.ByteBuffer;

public class WritePacket extends Packet {
    private final String filename;
    private final String mode;

    public WritePacket(String filename, String mode) {
        super(OpCode.WRQ);
        this.filename = filename;
        this.mode = mode;
    }

    @Override
    public int size() {
        return Character.BYTES + filename.length() + 1 + mode.length() + 1;
    }

    @Override
    public byte[] serialize() {
        var buf = ByteBuffer.allocate(size());
        buf.putShort(getOpCode());
        buf.put(filename.getBytes());
        buf.put((byte)0);
        buf.put(mode.getBytes());
        buf.put((byte)0);
        return buf.array();
    }
}
