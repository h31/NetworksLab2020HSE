package ru.spbau.team.vnc.exceptions;

public class InvalidVersionException extends Exception {
    private final String errorMessage;

    public InvalidVersionException(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
