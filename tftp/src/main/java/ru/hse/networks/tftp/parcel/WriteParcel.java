package ru.hse.networks.tftp.parcel;

import java.nio.ByteBuffer;

final public class WriteParcel extends ReadWriteParcel {
    public WriteParcel(Mode mode, String fileName) {
        super(mode, fileName, OpCode.WRITE);
    }

    public static WriteParcel fromBytes(ByteBuffer buffer) {
        final var fileName = ByteBufferUtils.getNullTerminatedString(buffer);
        final var mode = ByteBufferUtils.getNullTerminatedString(buffer).toUpperCase();
        return new WriteParcel(Mode.valueOf(mode), fileName);
    }
}
