package requests;

import java.nio.ByteBuffer;

import static parameters.OpCode.ACK;

public class AckPacket extends Packet {

    private final short blockNumber;

    public AckPacket(short blockNumber) {
        super(ACK);
        this.blockNumber = blockNumber;
    }

    @Override
    public int size() {
        return Character.BYTES + Character.BYTES;
    }

    public short getBlockNumber() {
        return blockNumber;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(size());
        buf.putShort(getOpCode());
        buf.putChar((char) blockNumber);
        return buf.array();
    }
}
