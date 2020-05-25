package ru.spbau.team.vnc.messages.incoming.routine;

import ru.spbau.team.vnc.Connection;
import ru.spbau.team.vnc.messages.FormattedReader;

import java.awt.*;
import java.io.IOException;

public class FrameBufferUpdateRequestMessage extends RoutineMessage {

    private final boolean incremental;
    private final int xPosition;
    private final int yPosition;
    private final int width;
    private final int height;

    public FrameBufferUpdateRequestMessage(boolean incremental, int xPosition, int yPosition, int width, int height) {
        this.incremental = incremental;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.width = width;
        this.height = height;

        System.out.println("Update " + incremental + " " + xPosition + " " + yPosition + " " + width + " " + height);
    }

    public static FrameBufferUpdateRequestMessage fromInputStream(FormattedReader inputStream) throws IOException {
        boolean incremental = inputStream.readBoolean();
        int xPosition = inputStream.readU16BigEndian();
        int yPosition = inputStream.readU16BigEndian();
        int width = inputStream.readU16BigEndian();
        int height = inputStream.readU16BigEndian();

        return new FrameBufferUpdateRequestMessage(incremental, xPosition, yPosition, width, height);
    }

    @Override
    public void execute(Connection connection) throws AWTException, IOException {
        // TODO: Normal send
        connection.sendRawUpdate();
    }

    public boolean isIncremental() {
        return incremental;
    }

    public int getXPosition() {
        return xPosition;
    }

    public int getYPosition() {
        return yPosition;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
