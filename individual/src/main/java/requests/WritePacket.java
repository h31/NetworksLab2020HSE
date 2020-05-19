package requests;

import java.nio.ByteBuffer;

import static parameters.OpCode.WRQ;

public class WritePacket extends Packet {
    private final String filename;
    private final String mode;

    public WritePacket(String filename, String mode) {
        super(WRQ);
        this.filename = filename;
        this.mode = mode;
    }

    @Override
    public int size() {
        return Character.BYTES + filename.length() + 1 + mode.length() + 1;
    }

    public String getFilename() {
        return filename;
    }

    public String getMode() {
        return mode;
    }

    @Override
    public byte[] serialize() {
        var buf = ByteBuffer.allocate(size());
        buf.putShort(getOpCode());
        buf.put(filename.getBytes());
        buf.put((byte) 0);
        buf.put(mode.getBytes());
        buf.put((byte) 0);
        return buf.array();
    }
}
