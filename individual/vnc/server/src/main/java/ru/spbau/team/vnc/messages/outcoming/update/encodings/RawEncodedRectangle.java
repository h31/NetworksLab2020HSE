package ru.spbau.team.vnc.messages.outcoming.update.encodings;

import ru.spbau.team.vnc.PixelFormat;
import ru.spbau.team.vnc.messages.outcoming.FormattedByteArrayWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RawEncodedRectangle implements EncodedRectangle {

    private static final int encodingType = 0;
    private final int[] pixels;
    private final PixelFormat pixelFormat;

    public RawEncodedRectangle(int[] pixels, PixelFormat pixelFormat) {
        this.pixels = pixels;
        this.pixelFormat = pixelFormat;
    }

    @Override
    public int getEncodingType() {
        return encodingType;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        try (var outputStream = new FormattedByteArrayWriter(new ByteArrayOutputStream())) {
            for (int pixel : pixels) {
                int printingPixel = pixel;
                if (!pixelFormat.isBigEndian()) {
                    printingPixel = Integer.reverseBytes(printingPixel);
                }
                if (pixelFormat.getBitsPerPixel() == 32) {
                    outputStream.writeS32BigEndian(printingPixel);
                } else if (pixelFormat.getBitsPerPixel() == 16) {
                    outputStream.writeU16BigEndian(printingPixel);
                } else if (pixelFormat.getBitsPerPixel() == 8) {
                    outputStream.writeByte(printingPixel);
                } else {
                    // TODO: throw
                }
            }
            return outputStream.toByteArray();
        }
    }
}
