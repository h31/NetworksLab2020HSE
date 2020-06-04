package ru.spbau.team.vnc.messages.outcoming;

import ru.spbau.team.vnc.security.SecurityType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class SecurityTypesMessage implements OutcomingMessage {

    private final List<SecurityType> securityTypes;

    public SecurityTypesMessage(List<SecurityType> securityTypes) {
        this.securityTypes = securityTypes;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        try (var outputStream = new FormattedByteArrayWriter(new ByteArrayOutputStream())) {
            outputStream.writeByte(securityTypes.size());
            for (SecurityType securityType : securityTypes) {
                outputStream.writeByte(securityType.getCode());
            }
            return outputStream.toByteArray();
        }
    }
}
