package requests;

import fields.OpCode;

import java.nio.ByteBuffer;

public class DataPacket extends Packet {

    private final short blockNumber;

    private final String data;

    public DataPacket(short blockNumber, String data) {
        super(OpCode.DATA);
        this.blockNumber = blockNumber;
        this.data = data;
    }

    @Override
    public int size() {
        return Character.BYTES  + Character.BYTES  + data.length();
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(size());
        buf.putShort(getOpCode());
        buf.putChar((char)blockNumber);
        buf.put(data.getBytes());
        return buf.array();
    }
}
