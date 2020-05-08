package ru.hsespb.tftp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class ClientRequest {
    private static final String FORMAT = "(.*) (.*) tftp:\\/\\/(.*)\\/(.*)";

    private final ClientRequestType requestType;
    private final String localFileName;
    private final String remoteFileName;
    private final InetAddress remoteAddress;

    public ClientRequest(String requestString) throws UnknownHostException {
        var pattern = Pattern.compile(FORMAT);
        var matcher = pattern.matcher(requestString);
        if (matcher.find()) {
            requestType = ClientRequestType.get(matcher.group(1));
            localFileName = matcher.group(2);
            remoteAddress = InetAddress.getByName(matcher.group(3));
            remoteFileName = matcher.group(4);
        } else {
            throw new RuntimeException("Invalid user input: " + requestString + "\n" +
                    "Usage: \"<read|write> <local_file> tftp://remote_host/remote_file\"");
        }
    }

    public ClientRequestType getRequestType() {
        return requestType;
    }

    public String getLocalFileName() {
        return localFileName;
    }

    public String getRemoteFileName() {
        return remoteFileName;
    }

    public InetAddress getRemoteAddress() {
        return remoteAddress;
    }
}
