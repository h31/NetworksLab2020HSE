package ru.spbau.team.vnc.messages.outcoming.update;

import ru.spbau.team.vnc.messages.outcoming.FormattedByteArrayWriter;
import ru.spbau.team.vnc.messages.outcoming.update.encodings.EncodedRectangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class FramebufferUpdateRectangle {

    private final int xPosition;
    private final int yPosition;
    private final int width;
    private final int height;
    private final EncodedRectangle rectangle;

    public FramebufferUpdateRectangle(int xPosition, int yPosition, int width, int height,
                                      EncodedRectangle rectangle) {
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.width = width;
        this.height = height;
        this.rectangle = rectangle;
    }

    public byte[] toByteArray() throws IOException {
        try (var outputStream = new FormattedByteArrayWriter(new ByteArrayOutputStream())) {
            outputStream.writeU16BigEndian(xPosition);
            outputStream.writeU16BigEndian(yPosition);
            outputStream.writeU16BigEndian(width);
            outputStream.writeU16BigEndian(height);
            outputStream.writeS32BigEndian(rectangle.getEncodingType());
            outputStream.writeBytes(rectangle.toByteArray());

            return outputStream.toByteArray();
        }
    }
}
