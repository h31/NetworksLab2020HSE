package ru.spbau.team.vnc.messages.incoming;

import ru.spbau.team.vnc.security.SecurityType;

import java.io.IOException;
import java.io.InputStream;

public class SecuritySelectMessage {

    private SecurityType securityType;

    private static final int expectedBytes = 1;

    private SecuritySelectMessage(int code) {
        securityType = SecurityType.INVALID;
        for (SecurityType type : SecurityType.values()) {
            if (type.getCode() == code) {
                securityType = type;
                break;
            }
        }
        System.out.println(securityType);
    }

    public SecurityType getSecurityType() {
        return securityType;
    }

    public static SecuritySelectMessage fromInputStream(InputStream inputStream) throws IOException {
        var buffer = inputStream.readNBytes(expectedBytes);
        // TODO check number of bytes read
        return new SecuritySelectMessage(buffer[0]);
    }
}

