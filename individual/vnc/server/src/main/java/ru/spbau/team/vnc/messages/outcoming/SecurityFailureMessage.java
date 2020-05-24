package ru.spbau.team.vnc.messages.outcoming;

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
        try (var outputStream = new ByteArrayOutputStream()) {
            byte[] bigEndianLength = Utils.toBigEndian(reason.length());
            outputStream.writeBytes(bigEndianLength);
            outputStream.writeBytes(reason.getBytes(StandardCharsets.US_ASCII));
            return outputStream.toByteArray();
        }
    }
}
