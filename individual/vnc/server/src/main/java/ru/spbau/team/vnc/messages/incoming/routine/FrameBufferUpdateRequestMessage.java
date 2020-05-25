package ru.spbau.team.vnc.messages.incoming.routine;

import ru.spbau.team.vnc.messages.Utils;

import java.io.IOException;
import java.io.InputStream;

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

    public static FrameBufferUpdateRequestMessage fromInputStream(InputStream inputStream) throws IOException {
        boolean incremental = Utils.readBoolean(inputStream);
        int xPosition = Utils.readU16BigEndian(inputStream);
        int yPosition = Utils.readU16BigEndian(inputStream);
        int width = Utils.readU16BigEndian(inputStream);
        int height = Utils.readU16BigEndian(inputStream);

        return new FrameBufferUpdateRequestMessage(incremental, xPosition, yPosition, width, height);
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
