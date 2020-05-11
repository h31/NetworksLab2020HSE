package ru.hse.networks.tftp.parcel;

import java.nio.ByteBuffer;

final public class DataParcel implements Parcel {
    private final short blockNumber;
    private final byte[] bytes;

    public DataParcel(short blockNumber, byte[] bytes) {
        this.blockNumber = blockNumber;
        this.bytes = bytes;
    }

    public short getBlockNumber() {
        return blockNumber;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public static DataParcel fromBytes(ByteBuffer buffer) {
        final var blockNumber = buffer.getShort();
        final var bytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(bytes);
        return new DataParcel(blockNumber, bytes);
    }

    @Override
    public ByteBuffer toBytes() {
        return ByteBuffer.allocate(size())
                .putShort(OpCode.DATA.getCode())
                .putShort(blockNumber)
                .put(bytes)
                .flip();
    }

    @Override
    public int size() {
        return 4 + bytes.length;
    }
}
