package ru.spbau.team.vnc.messages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {

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

    private static long fromBigEndianU32(byte[] bigEndian) {
        return ByteBuffer
            .allocate(8)
            .put(new byte[] { 0, 0, 0, 0 })
            .put(bigEndian)
            .order(ByteOrder.BIG_ENDIAN)
            .flip()
            .getLong();
    }

    public static long readU32BigEndian(InputStream inputStream) throws IOException {
        byte[] buffer = inputStream.readNBytes(4);
        // TODO: Check read 4 bytes
        return fromBigEndianU32(buffer);
    }

    public static int readS32BigEndian(InputStream inputStream) throws IOException {
        byte[] buffer = inputStream.readNBytes(4);
        // TODO: Check read 4 bytes
        return fromBigEndian32(buffer);
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
