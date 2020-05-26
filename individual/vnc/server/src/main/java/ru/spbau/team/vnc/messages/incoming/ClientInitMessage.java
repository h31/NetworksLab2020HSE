package ru.spbau.team.vnc.messages.incoming;

import ru.spbau.team.vnc.exceptions.ClientDisconnectedException;

import java.io.IOException;

public class ClientInitMessage {
    private final boolean shared;

    private ClientInitMessage(boolean shared) {
        this.shared = shared;
    }

    public boolean isShared() {
        return shared;
    }

    public static ClientInitMessage fromInputStream(FormattedReader inputStream)
            throws IOException, ClientDisconnectedException {
        boolean shared = inputStream.readBoolean();
        return new ClientInitMessage(shared);
    }
}
