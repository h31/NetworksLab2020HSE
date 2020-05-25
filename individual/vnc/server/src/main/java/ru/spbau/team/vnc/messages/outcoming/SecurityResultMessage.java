package ru.spbau.team.vnc.messages.outcoming;

import ru.spbau.team.vnc.security.SecurityType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SecurityResultMessage implements OutcomingMessage {

    private final boolean isSuccess;

    public SecurityResultMessage(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            int result;
            if (isSuccess) {
                result = 0;
            } else {
                result = 1;
            }
            outputStream.writeBytes(Utils.toBigEndian32(result));
            return outputStream.toByteArray();
        }
    }
}
