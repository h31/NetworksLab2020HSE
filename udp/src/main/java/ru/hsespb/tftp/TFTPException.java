package ru.hsespb.tftp;

public class TFTPException extends RuntimeException {
    private final ErrorType errorType;
    private final String message;

    public TFTPException(ErrorType errorType, String message) {
        this.errorType = errorType;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
