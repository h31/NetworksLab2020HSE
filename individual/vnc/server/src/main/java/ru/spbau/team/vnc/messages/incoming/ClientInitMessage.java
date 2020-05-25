package ru.spbau.team.vnc.messages.incoming;

import ru.spbau.team.vnc.messages.Utils;

import java.io.IOException;
import java.io.InputStream;

public class ClientInitMessage {
    private final boolean shared;

    private ClientInitMessage(boolean shared) {
        this.shared = shared;
    }

    public boolean isShared() {
        return shared;
    }

    public static ClientInitMessage fromInputStream(InputStream inputStream) throws IOException {
        boolean shared = Utils.readBoolean(inputStream);
        return new ClientInitMessage(shared);
    }
}
