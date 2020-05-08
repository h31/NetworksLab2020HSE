package ru.hsespb.tftp;

import java.nio.ByteBuffer;

public class DataMessage {
    private static final OperationType OPERATION_TYPE = OperationType.DATA;

    private final int blockNumber;
    private final byte[] data;

    public DataMessage(byte[] dataBytes) {
        var buffer = ByteBuffer.wrap(dataBytes);
        var operationType = OperationType.get(buffer.get(), buffer.get());
        blockNumber = buffer.getShort();

        if (operationType != OPERATION_TYPE) {
            throw new RuntimeException("An attempt to create a request message " +
                    "with invalid operation type: " + operationType.name());
        }

        data = new byte[buffer.remaining()];
        buffer.get(data);
    }

    public DataMessage(byte[] data, int blockNumber) {
        this.blockNumber = blockNumber;
        this.data = data;
    }

    public byte[] build() {
        return ByteBuffer.allocate(2 + 2 + data.length)
                .put(OPERATION_TYPE.getOperationCode())
                .putShort((short)blockNumber)
                .put(data)
                .array();
    }

    public byte[] getData() {
        return data;
    }

    public boolean isLast() {
        return data.length < 512;
    }

    public int getBlockNumber() {
        return blockNumber;
    }
}
