package ru.spbau.team.vnc.messages.outcoming;

import ru.spbau.team.vnc.messages.outcoming.update.FramebufferUpdateRectangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class FramebufferUpdateMessage implements OutcomingMessage {

    private static final int MESSAGE_CODE = 0;

    private final List<FramebufferUpdateRectangle> rectangles;

    public FramebufferUpdateMessage(List<FramebufferUpdateRectangle> rectangles) {
        this.rectangles = rectangles;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        try (var outputStream = new FormattedByteArrayWriter(new ByteArrayOutputStream())) {
            outputStream.writeByte(MESSAGE_CODE);
            outputStream.writeByte(0); // padding
            outputStream.writeU16BigEndian(rectangles.size());

            for (FramebufferUpdateRectangle rectangle : rectangles) {
                outputStream.writeBytes(rectangle.toByteArray());
            }

            return outputStream.toByteArray();
        }
    }
}
