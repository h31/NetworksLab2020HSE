package ru.hsespb.tftp;

import java.nio.ByteBuffer;

public class ErrorMessage {
    private static final OperationType OPERATION_TYPE = OperationType.ERR;

    private final ErrorType errorType;
    private final String message;

    public ErrorMessage(byte[] data) {
        var buffer = ByteBuffer.wrap(data);
        var operationType = OperationType.get(buffer.get(), buffer.get());

        if (operationType != OPERATION_TYPE) {
            throw new RuntimeException("An attempt to create a request message " +
                    "with invalid operation type: " + operationType.name());
        }

        errorType = ErrorType.get(buffer.get(), buffer.get());
        message = ByteReader.readString(buffer);
    }

    public ErrorMessage(ErrorType errorType, String message) {
        this.errorType = errorType;
        this.message = message;
    }

    public byte[] build() {
        return ByteBuffer.allocate(2 + 2 + message.length() + 1)
                .put(OPERATION_TYPE.getOperationCode())
                .put(errorType.getErrorCode())
                .put(message.getBytes())
                .put((byte) 0)
                .array();
    }

    public String getMessage() {
        return message;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
