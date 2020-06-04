package ru.spbau.team.vnc.messages.outcoming;

import ru.spbau.team.vnc.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ServerInitMessage implements OutcomingMessage {

    private final Parameters serverParameters;

    public ServerInitMessage(Parameters parameters) {
        serverParameters = parameters;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        try (var outputStream = new FormattedByteArrayWriter(new ByteArrayOutputStream())) {
            outputStream.writeU16BigEndian(serverParameters.getFramebufferWidth());
            outputStream.writeU16BigEndian(serverParameters.getFramebufferWidth());
            outputStream.writeBytes(serverParameters.getPixelFormat().toByteArray());
            outputStream.writeS32BigEndian(serverParameters.getName().length());
            outputStream.writeBytes(serverParameters.getName().getBytes(StandardCharsets.US_ASCII));

            return outputStream.toByteArray();
        }
    }
}
