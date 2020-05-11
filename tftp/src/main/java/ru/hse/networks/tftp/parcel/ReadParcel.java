package ru.hse.networks.tftp.parcel;

import java.nio.ByteBuffer;

final public class ReadParcel extends ReadWriteParcel {
    public ReadParcel(Mode mode, String fileName) {
        super(mode, fileName, OpCode.READ);
    }

    public static ReadParcel fromBytes(ByteBuffer buffer) {
        final var fileName = ByteBufferUtils.getNullTerminatedString(buffer);
        final var mode = ByteBufferUtils.getNullTerminatedString(buffer).toUpperCase();
        return new ReadParcel(Mode.valueOf(mode), fileName);
    }
}
