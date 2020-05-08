package ru.hsespb.tftp;

import java.nio.ByteBuffer;

public class ByteReader {
    public static String readString(ByteBuffer buffer) {
        var stringBuilder = new StringBuilder();
        byte currentByte = buffer.get();
        while (currentByte != 0) {
            stringBuilder.append((char)currentByte);
            currentByte = buffer.get();
        }
        return stringBuilder.toString();
    }
}
