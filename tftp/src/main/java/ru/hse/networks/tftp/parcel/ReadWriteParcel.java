package ru.hse.networks.tftp.parcel;

import java.nio.ByteBuffer;

public abstract class ReadWriteParcel implements Parcel {
    private final Mode mode;
    private final String fileName;
    private final OpCode code;

    public ReadWriteParcel(Mode mode, String fileName, OpCode code) {
        this.mode = mode;
        this.fileName = fileName;
        this.code = code;
    }

    @Override
    public ByteBuffer toBytes() {
        return ByteBuffer.allocate(size())
                .putShort(code.getCode())
                .put(fileName.getBytes())
                .put((byte) 0)
                .put(mode.name().getBytes())
                .put((byte) 0)
                .flip();
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public int size() {
        return 2 + fileName.length() + 1 + mode.name().length() + 1;
    }
}
