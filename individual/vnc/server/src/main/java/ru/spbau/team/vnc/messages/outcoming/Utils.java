package ru.spbau.team.vnc.messages.outcoming;

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
}
