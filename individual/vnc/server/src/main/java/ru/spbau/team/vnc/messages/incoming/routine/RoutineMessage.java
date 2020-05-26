package ru.spbau.team.vnc.messages.incoming.routine;

import ru.spbau.team.vnc.Connection;
import ru.spbau.team.vnc.exceptions.ClientDisconnectedException;
import ru.spbau.team.vnc.messages.incoming.FormattedReader;

import java.awt.*;
import java.io.IOException;

public abstract class RoutineMessage {

    public static RoutineMessage fromInputStream(FormattedReader inputStream)
            throws IOException, ClientDisconnectedException {
        int messageType = inputStream.readU8();

        System.out.println("Got message with code " + messageType);
        if (messageType == 0) {
            // TODO SetPixelFormat
            inputStream.readNBytes(19);
        } else if (messageType == 2) {
            return SetEncodingsMessage.fromInputStream(inputStream);
        } else if (messageType == 3) {
            return FrameBufferUpdateRequestMessage.fromInputStream(inputStream);
        } else if (messageType == 4) {
            // TODO KeyEvent
            inputStream.readNBytes(7);
        } else if (messageType == 5) {
            // TODO PointerEvent
            inputStream.readNBytes(5);
        } else if (messageType == 6) {
            // TODO ClientCutText
        } else {
            // TODO: throw something
        }
        return null;
    }

    abstract public void execute(Connection connection) throws AWTException, IOException;
}
