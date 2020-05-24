package ru.spbau.team.vnc.messages.incoming;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class VersionSelectMessage {

    private final int majorVersion;
    private final int minorVersion;

    private static final int expectedBytes = "RFB xxx.yyy\n".length();

    private VersionSelectMessage(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public static VersionSelectMessage fromInputStream(InputStream inputStream) throws IOException {
        var buffer = inputStream.readNBytes(expectedBytes);
        var versionSelectString = new String(buffer, StandardCharsets.US_ASCII);
        System.out.println("Received version select message:\n" + versionSelectString);
        var majorVersion = Integer.parseInt(versionSelectString.substring(4, 7));
        var minorVersion = Integer.parseInt(versionSelectString.substring(8, 11));
        return new VersionSelectMessage(majorVersion, minorVersion);
    }
}
