package ru.spbau.team.vnc.messages.outcoming;

import java.io.IOException;

public interface OutcomingMessage {
    byte[] toByteArray() throws IOException;
}
