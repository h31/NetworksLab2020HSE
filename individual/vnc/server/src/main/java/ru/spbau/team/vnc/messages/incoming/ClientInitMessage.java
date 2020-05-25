package ru.spbau.team.vnc.messages.incoming;

import java.io.IOException;
import java.io.InputStream;

public class ClientInitMessage {
    private final boolean shared;

    private static final int expectedBytes = 1;

    private ClientInitMessage(boolean shared) {
        this.shared = shared;
    }

    public boolean isShared() {
        return shared;
    }

    public static ClientInitMessage fromInputStream(InputStream inputStream) throws IOException {
        var buffer = inputStream.readNBytes(expectedBytes);
        // TODO check number of bytes read
        return new ClientInitMessage(buffer[0] != 0);
    }
}
