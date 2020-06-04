package ru.spbau.team.vnc.messages.incoming.routine;

import ru.spbau.team.vnc.Connection;
import ru.spbau.team.vnc.exceptions.ClientDisconnectedException;
import ru.spbau.team.vnc.messages.incoming.FormattedReader;

import java.io.IOException;

public class SetPixelFormatMessage extends RoutineMessage {
    private int pixelFormat;

    public SetPixelFormatMessage(int pixelFormat) {
        this.pixelFormat = pixelFormat;
    }

    public static SetPixelFormatMessage fromInputStream(FormattedReader inputStream)
            throws IOException, ClientDisconnectedException {
        for (int i = 0; i < 3; i++) {
            inputStream.readU8();
        }
        int pixelFormat = inputStream.readU16BigEndian();

        return new SetPixelFormatMessage(pixelFormat);
    }

    @Override
    public void execute(Connection connection) {
        // TODO: we need to support encodings first
    }
}
