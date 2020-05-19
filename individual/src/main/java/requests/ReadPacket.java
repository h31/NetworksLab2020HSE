package requests;

import java.nio.ByteBuffer;

import static parameters.OpCode.RRQ;

public class ReadPacket extends Packet {
    private final String filename;
    private final String mode;

    public ReadPacket(String filename, String mode) {
        super(RRQ);
        this.filename = filename;
        this.mode = mode;
    }

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
        ByteBuffer buf = ByteBuffer.allocate(size());
        buf.putShort(getOpCode());
        buf.put(filename.getBytes());
        buf.put((byte) 0);
        buf.put(mode.getBytes());
        buf.put((byte) 0);
        return buf.array();
    }
}
