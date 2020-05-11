package ru.hse.networks.tftp.parcel;

import java.nio.ByteBuffer;

final public class ErrorParcel implements Parcel {
    private final short errorCode;

    private final String errorMessage;

    public ErrorParcel(short errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = getStandardMessage(errorCode);
    }

    public ErrorParcel(short errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public short getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static ErrorParcel fromBytes(ByteBuffer buffer) {
        final var errorCode = buffer.getShort();
        final var message = ByteBufferUtils.getNullTerminatedString(buffer);
        return new ErrorParcel(errorCode, message);
    }

    @Override
    public ByteBuffer toBytes() {
        return ByteBuffer.allocate(size())
                .putShort(OpCode.ERROR.getCode())
                .putShort(errorCode)
                .put(errorMessage.getBytes())
                .put((byte) 0)
                .flip();
    }

    @Override
    public int size() {
        return 4 + errorMessage.length() + 1;
    }

    private static String getStandardMessage(short code) {
        switch (code) {
            case 0:
                return "Not defined, see error message (if any).";
            case 1:
                return "File not found.";
            case 2:
                return "Access violation.";
            case 3:
                return "Disk full or allocation exceeded.";
            case 4:
                return "Illegal TFTP operation.";
            case 5:
                return "Unknown transfer ID.";
            case 6:
                return "File already exists.";
            case 7:
                return "No such user.";
        }
        throw new IllegalArgumentException("Unknown error code.");
    }
}
