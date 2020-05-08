package ru.hsespb.tftp;

public enum ErrorType {
    UNKNOWN, FILE_NOT_FOUND, ILLEGAL_OPERATION, UNKNOWN_TID, FILE_EXISTS;

    private static final byte UNKNOWN_CODE = 0;
    private static final byte FILE_NOT_FOUND_CODE = 1;
    private static final byte ILLEGAL_OPERATION_CODE = 4;
    private static final byte UNKNOWN_TID_CODE = 5;
    private static final byte FILE_EXISTS_CODE = 6;

    public static ErrorType get(byte firstByte, byte errorCode) {
        if (firstByte != 0) {
            throw new TFTPException(ErrorType.ILLEGAL_OPERATION,
                    "The first byte of the error code is not 0, found: " + firstByte);
        }
        if (errorCode == UNKNOWN_CODE) {
            return UNKNOWN;
        }
        if (errorCode == FILE_NOT_FOUND_CODE) {
            return FILE_NOT_FOUND;
        }
        if (errorCode == ILLEGAL_OPERATION_CODE) {
            return ILLEGAL_OPERATION;
        }
        if (errorCode == UNKNOWN_TID_CODE) {
            return UNKNOWN_TID;
        }
        if (errorCode == FILE_EXISTS_CODE) {
            return FILE_EXISTS;
        }

        throw new TFTPException(ErrorType.ILLEGAL_OPERATION,
                "Unknown second byte of error code: " + errorCode);
    }

    public byte[] getErrorCode() {
        byte code = -1;
        if (this == UNKNOWN) {
            code = UNKNOWN_CODE;
        }
        if (this == FILE_NOT_FOUND) {
            code = FILE_NOT_FOUND_CODE;
        }
        if (this == ILLEGAL_OPERATION) {
            code = ILLEGAL_OPERATION_CODE;
        }
        if (this == UNKNOWN_TID) {
            code = UNKNOWN_TID_CODE;
        }
        if (this == FILE_EXISTS) {
            code = FILE_EXISTS_CODE;
        }

        if (code == -1) {
            throw new RuntimeException("Unexpected error type: " + this.name());
        }
        return new byte[]{0, code};
    }
}
