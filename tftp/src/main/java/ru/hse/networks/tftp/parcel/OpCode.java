package ru.hse.networks.tftp.parcel;

public enum OpCode {
    READ(1),
    WRITE(2),
    DATA(3),
    ACKNOWLEDGEMENT(4),
    ERROR(5);

    private final short code;

    OpCode(int code) {
        this.code = (short) code;
    }

    public short getCode() {
        return code;
    }
}
