package ru.spbau.team.vnc.messages.incoming.routine;

import ru.spbau.team.vnc.messages.Utils;
import ru.spbau.team.vnc.messages.incoming.ClientInitMessage;

import java.io.IOException;
import java.io.InputStream;

public abstract class RoutineMessage {
    private static final int headerSize = 1;

    public static RoutineMessage fromInputStream(InputStream inputStream) throws IOException {
        int messageType = Utils.readU8(inputStream);

        System.out.println("Got message with code " + messageType);
        if (messageType == 0) {
            // TODO SetPixelFormat
        } else if (messageType == 2) {
            // TODO SetEncodings
        } else if (messageType == 3) {
            return FrameBufferUpdateRequestMessage.fromInputStream(inputStream);
        } else if (messageType == 4) {
            // TODO KeyEvent
        } else if (messageType == 5) {
            // TODO PointerEvent
        } else if (messageType == 6) {
            // TODO ClientCutText
        } else {
            // TODO: throw something
        }
        return null;
    }
}
