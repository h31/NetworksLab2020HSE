package requests;

import fields.OpCode;

import java.nio.ByteBuffer;

public class AckPacket extends Packet {

    private final short blockNumber;

    public AckPacket(short blockNumber) {
        super(OpCode.ACK);
        this.blockNumber = blockNumber;
    }

    @Override
    public int size() {
        return Character.BYTES + Character.BYTES;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(size());
        buf.putShort(getOpCode());
        buf.putChar((char)blockNumber);
        return buf.array();
    }
}
