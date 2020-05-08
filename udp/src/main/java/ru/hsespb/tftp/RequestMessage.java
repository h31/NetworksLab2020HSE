package ru.hsespb.tftp;

import java.nio.ByteBuffer;

public class RequestMessage {
    private final OperationType operationType;
    private final String fileName;

    public RequestMessage(OperationType operationType, String fileName) {
        this.operationType = operationType;
        this.fileName = fileName;
    }

    public RequestMessage(byte[] data) {
        var buffer = ByteBuffer.wrap(data);
        operationType = OperationType.get(buffer.get(), buffer.get());

        if (operationType != OperationType.RRQ && operationType != OperationType.WRQ) {
            throw new RuntimeException("An attempt to create a request message " +
                    "with invalid operation type: " + operationType.name());
        }

        fileName = ByteReader.readString(buffer);
    }

    public byte[] build() {
        var fileNameBytes = fileName.getBytes();
        return ByteBuffer.allocate(2 + fileNameBytes.length + 1)
                .put(operationType.getOperationCode())
                .put(fileNameBytes)
                .put((byte)0)
                .array();
    }

    public String getFileName() {
        return fileName;
    }
}
