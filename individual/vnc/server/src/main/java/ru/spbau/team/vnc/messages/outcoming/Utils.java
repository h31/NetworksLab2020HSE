package ru.spbau.team.vnc.messages.outcoming;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {
    public static byte[] toBigEndian(int x) {
        return ByteBuffer
            .allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(x)
            .array();
    }
}
