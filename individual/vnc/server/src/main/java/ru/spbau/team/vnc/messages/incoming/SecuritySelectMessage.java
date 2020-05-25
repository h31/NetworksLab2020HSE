package ru.spbau.team.vnc.messages.incoming;

import ru.spbau.team.vnc.messages.Utils;
import ru.spbau.team.vnc.security.SecurityType;

import java.io.IOException;
import java.io.InputStream;

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

    public static SecuritySelectMessage fromInputStream(InputStream inputStream) throws IOException {
        int code = Utils.readU8(inputStream);
        return new SecuritySelectMessage(code);
    }
}

