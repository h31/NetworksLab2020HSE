package requests;

import java.nio.ByteBuffer;

import static parameters.OpCode.DATA;
import static parameters.TftpPacketInfo.DATA_MAX_LENGTH;

public class DataPacket extends Packet {
    private final short blockNumber;

    private final byte[] data;

    public DataPacket(short blockNumber, byte[] data) {
        super(DATA);
        this.blockNumber = blockNumber;
        this.data = data;
    }

    public short getBlockNumber() {
        return blockNumber;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isLastPacket() {
        return data.length != DATA_MAX_LENGTH;
    }

    @Override
    public int size() {
        return Character.BYTES + Character.BYTES + data.length;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(size());
        buf.putShort(getOpCode());
        buf.putChar((char) blockNumber);
        buf.put(data);
        return buf.array();
    }
}
