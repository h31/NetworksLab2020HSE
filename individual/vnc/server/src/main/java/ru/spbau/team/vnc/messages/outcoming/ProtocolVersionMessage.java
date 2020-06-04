package ru.spbau.team.vnc.messages.outcoming;

import java.nio.charset.StandardCharsets;

public class ProtocolVersionMessage implements OutcomingMessage {

    private final int majorVersion;
    private final int minorVersion;

    public ProtocolVersionMessage(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    private String buildVersion(int version) {
        var versionString = new StringBuilder(String.valueOf(version));
        while (versionString.length() < 3) {
            versionString.insert(0, "0");
        }
        return versionString.toString();
    }

    @Override
    public byte[] toByteArray() {
        return ("RFB " + buildVersion(majorVersion) + "." + buildVersion(minorVersion) + "\n")
                .getBytes(StandardCharsets.US_ASCII);
    }
}
