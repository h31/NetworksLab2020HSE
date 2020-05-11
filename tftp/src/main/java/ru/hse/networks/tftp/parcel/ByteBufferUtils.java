package ru.hse.networks.tftp.parcel;

import java.nio.ByteBuffer;
import java.util.ArrayList;

final public class ByteBufferUtils {

    static String getNullTerminatedString(ByteBuffer buffer) {
        final var list = new ArrayList<Byte>();
        var b = buffer.get();
        while (b != 0) {
            list.add(b);
            b = buffer.get();
        }
        final var bytes = new Byte[list.size()];
        list.toArray(bytes);
        return new String(toPrimitives(bytes));
    }

    public static byte[] getBytes(ByteBuffer buffer) {
        final var bytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(bytes);
        return bytes;
    }

    private static byte[] toPrimitives(Byte[] oBytes) {
        byte[] bytes = new byte[oBytes.length];
        for (int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }
        return bytes;

    }
}
