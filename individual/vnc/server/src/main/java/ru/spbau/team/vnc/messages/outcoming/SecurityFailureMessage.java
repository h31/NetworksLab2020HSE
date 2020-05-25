package ru.spbau.team.vnc.messages.outcoming;

import ru.spbau.team.vnc.messages.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SecurityFailureMessage implements OutcomingMessage {

    private final String reason;

    public SecurityFailureMessage(String reason) {
        this.reason = reason;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        try (var outputStream = new FormattedByteArrayWriter(new ByteArrayOutputStream())) {
            outputStream.writeS32BigEndian(reason.length());
            outputStream.writeBytes(reason.getBytes(StandardCharsets.US_ASCII));
            return outputStream.toByteArray();
        }
    }
}
