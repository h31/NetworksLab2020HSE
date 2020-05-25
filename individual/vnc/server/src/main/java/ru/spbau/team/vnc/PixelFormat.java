package ru.spbau.team.vnc;

import ru.spbau.team.vnc.messages.Utils;
import ru.spbau.team.vnc.messages.outcoming.FormattedByteArrayWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PixelFormat {

    private final int bitsPerPixel;
    private final int depth;
    private final boolean bigEndian;
    private final boolean trueColor;
    private final int redMax;
    private final int greenMax;
    private final int blueMax;
    private final int redShift;
    private final int greenShift;
    private final int blueShift;

    public PixelFormat(int bitsPerPixel, int depth, boolean bigEndian, boolean trueColor,
                       int redMax, int greenMax, int blueMax,
                       int redShift, int greenShift, int blueShift) {
        this.bitsPerPixel = bitsPerPixel;
        this.depth = depth;
        this.bigEndian = bigEndian;
        this.trueColor = trueColor;
        this.redMax = redMax;
        this.greenMax = greenMax;
        this.blueMax = blueMax;
        this.redShift = redShift;
        this.greenShift = greenShift;
        this.blueShift = blueShift;
    }

    public static PixelFormat getDefault() {
        return new PixelFormat(32, 24, true, true,
            (1 << 8) - 1, (1 << 8) - 1, (1 << 8) - 1,
            16, 8, 0);
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public boolean isTrueColor() {
        return trueColor;
    }

    public int getRedMax() {
        return redMax;
    }

    public int getGreenMax() {
        return greenMax;
    }

    public int getBlueMax() {
        return blueMax;
    }

    public int getRedShift() {
        return redShift;
    }

    public int getGreenShift() {
        return greenShift;
    }

    public int getBlueShift() {
        return blueShift;
    }

    public byte[] toByteArray() throws IOException {
        try (var outputStream = new FormattedByteArrayWriter(new ByteArrayOutputStream())) {
            outputStream.writeByte(bitsPerPixel);
            outputStream.writeByte(depth);
            outputStream.writeBoolean(bigEndian);
            outputStream.writeBoolean(trueColor);
            outputStream.writeU16BigEndian(redMax);
            outputStream.writeU16BigEndian(greenMax);
            outputStream.writeU16BigEndian(blueMax);
            outputStream.writeByte(redShift);
            outputStream.writeByte(greenShift);
            outputStream.writeByte(blueShift);
            // padding
            outputStream.writeBytes(new byte[] { 0, 0, 0 });

            return outputStream.toByteArray();
        }
    }
}
