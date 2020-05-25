package ru.spbau.team.vnc.messages.incoming;

import ru.spbau.team.vnc.exceptions.ClientDisconnectedException;
import ru.spbau.team.vnc.security.SecurityType;

import java.io.IOException;

public class SecuritySelectMessage {

    private SecurityType securityType;

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

    public static SecuritySelectMessage fromInputStream(FormattedReader inputStream) throws IOException, ClientDisconnectedException {
        int code = inputStream.readU8();
        return new SecuritySelectMessage(code);
    }
}

