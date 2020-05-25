package ru.spbau.team.vnc;

import ru.spbau.team.vnc.messages.outcoming.Utils;

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
        return new PixelFormat(32, 24, true, false,
            (1 << 8) - 1, (1 << 8) - 1, (1 << 8) - 1,
            0, 8, 16);
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
        try (var outputStream = new ByteArrayOutputStream()) {
            outputStream.write(bitsPerPixel);
            outputStream.write(depth);
            outputStream.write(Utils.toByte(bigEndian));
            outputStream.write(Utils.toByte(trueColor));
            outputStream.writeBytes(Utils.toBigEndian16(redMax));
            outputStream.writeBytes(Utils.toBigEndian16(greenMax));
            outputStream.writeBytes(Utils.toBigEndian16(blueMax));
            outputStream.write(redShift);
            outputStream.write(greenShift);
            outputStream.write(blueShift);
            // padding
            outputStream.writeBytes(new byte[] { 0, 0, 0 });

            return outputStream.toByteArray();
        }
    }
}
