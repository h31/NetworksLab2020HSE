package ru.spbau.team.vnc.messages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {
    public static byte[] toBigEndian32(int x) {
        return ByteBuffer
            .allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(x)
            .array();
    }

    public static byte[] toBigEndian16(int x) {
        byte[] bigEndian32 = toBigEndian32(x);
        return new byte[] { bigEndian32[2], bigEndian32[3] };
    }

    public static byte[] toLittleEndian16(int x) {
        byte[] bigEndian16 = toBigEndian16(x);
        return new byte[] { bigEndian16[1], bigEndian16[0] };
    }

    public static byte toByte(boolean x) {
        if (x) {
            return 1;
        } else {
            return 0;
        }
    }

    private static int fromBigEndian32(byte[] bigEndian) {
        return ByteBuffer
            .allocate(4)
            .put(bigEndian)
            .order(ByteOrder.BIG_ENDIAN)
            .flip()
            .getInt();
    }

    private static int fromBigEndian16(byte[] bigEndian) {
        return ByteBuffer
            .allocate(4)
            .put(new byte[] { 0, 0 })
            .put(bigEndian)
            .order(ByteOrder.BIG_ENDIAN)
            .flip()
            .getInt();
    }

    public static int readU16BigEndian(InputStream inputStream) throws IOException {
        byte[] buffer = inputStream.readNBytes(2);
        // TODO: Check read 2 bytes
        return fromBigEndian16(buffer);
    }

    public static int readU8(InputStream inputStream) throws IOException {
        byte[] buffer = inputStream.readNBytes(1);
        // TODO: Check read 1 byte
        // TODO: normal cast to int. NB! just cast doesn't work because of sign
        return fromBigEndian16(new byte[] { 0, buffer[0] });
    }

    public static boolean readBoolean(InputStream inputStream) throws IOException {
        int result = readU8(inputStream);
        return result != 0;
    }
}
