package ru.hse.networks.tftp.parcel;

import java.nio.ByteBuffer;

final public class AcknowledgementParcel implements Parcel {
    private final short blockNumber;
    private static final int SIZE = 4;

    public AcknowledgementParcel(short blockNumber) {
        this.blockNumber = blockNumber;
    }

    static public AcknowledgementParcel fromBytes(ByteBuffer buffer) {
        final var blockNumber = buffer.getShort();
        return new AcknowledgementParcel(blockNumber);
    }

    @Override
    public ByteBuffer toBytes() {
        return ByteBuffer.allocate(size())
                .putShort(OpCode.ACKNOWLEDGEMENT.getCode())
                .putShort(blockNumber)
                .flip();
    }

    @Override
    public int size() {
        return SIZE;
    }
}
