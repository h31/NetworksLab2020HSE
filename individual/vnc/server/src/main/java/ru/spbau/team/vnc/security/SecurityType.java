package ru.spbau.team.vnc.security;

public enum SecurityType {
    INVALID(0),
    NONE(1);

    private final int code;

    public int getCode() {
        return code;
    }

    SecurityType(int code) {
        this.code = code;
    }
}
