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

        if (messageType == 0) {
            return SetPixelFormatMessage.fromInputStream(inputStream);
        } else if (messageType == 2) {
            return SetEncodingsMessage.fromInputStream(inputStream);
        } else if (messageType == 3) {
            return FrameBufferUpdateRequestMessage.fromInputStream(inputStream);
        } else if (messageType == 4) {
            return KeyEventMessage.fromInputStream(inputStream);
        } else if (messageType == 5) {
            return PointerEventMessage.fromInputStream(inputStream);
        } else if (messageType == 6) {
            return CutTextMessage.fromInputStream(inputStream);
        } else {
            return null;
        }
    }

    public abstract void execute(Connection connection) throws AWTException, IOException;
}
