package ru.hsespb.tftp;

import java.nio.ByteBuffer;

public class AcknowledgementMessage {
    private static final OperationType OPERATION_TYPE = OperationType.ACK;
    private final int blockNumber;

    public AcknowledgementMessage(byte[] data) {
        var buffer = ByteBuffer.wrap(data);
        var operationType = OperationType.get(buffer.get(), buffer.get());
        blockNumber = buffer.getShort();

        if (operationType != OPERATION_TYPE) {
            throw new RuntimeException("An attempt to create a request message " +
                    "with invalid operation type: " + operationType.name());
        }
    }

    public AcknowledgementMessage(int blockNumber) {
        this.blockNumber = blockNumber;
    }

    public byte[] build() {
        return ByteBuffer.allocate(4)
                .put(OPERATION_TYPE.getOperationCode())
                .putShort((short)blockNumber)
                .array();
    }

    public int getBlockNumber() {
        return blockNumber;
    }
}
