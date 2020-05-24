package ru.spbau.team.vnc;

import ru.spbau.team.vnc.messages.incoming.SecuritySelectMessage;
import ru.spbau.team.vnc.messages.incoming.VersionSelectMessage;
import ru.spbau.team.vnc.messages.outcoming.*;
import ru.spbau.team.vnc.security.SecurityType;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Connection {

    private final Socket socket;
    private final Server server;

    public Connection(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            initConnection();
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    private void initConnection() throws IOException {
        var selectedVersion = protocolVersionHandshake();
        securityHandshake(selectedVersion);

    }

    private VersionSelectMessage protocolVersionHandshake() throws IOException {
        sendMessage(new ProtocolVersionMessage(Server.MAJOR_VERSION, Server.MINOR_VERSION));
        return VersionSelectMessage.fromInputStream(socket.getInputStream());
    }

    private void securityHandshake(VersionSelectMessage selectedVersion) throws IOException {
        if (versionIsNotSupported(selectedVersion)) {
            sendMessage(new SecurityTypesMessage(Collections.emptyList()));
            String errorMessage = "Version " + selectedVersion.getMajorVersion() + "." + selectedVersion.getMinorVersion() + " is not supported";
            sendMessage(new SecurityFailureMessage(errorMessage));
            // TODO throw
        } else {
            sendMessage(new SecurityTypesMessage(Arrays.asList(SecurityType.INVALID, SecurityType.NONE)));
            var security = SecuritySelectMessage.fromInputStream(socket.getInputStream());
            // TODO: use security
            if (security.getSecurityType().equals(SecurityType.INVALID)) {
                sendMessage(new SecurityResultMessage(false));
                sendMessage(new SecurityFailureMessage("Invalid security code"));
            } else  {
                sendMessage(new SecurityResultMessage(true));
            }
        }
    }

    private boolean versionIsNotSupported(VersionSelectMessage selectedVersion) {
        // TODO: support 3.3, 3.7, 3.x
        return selectedVersion.getMajorVersion() != 3 || selectedVersion.getMinorVersion() != 8;
    }

    private void sendMessage(OutcomingMessage message) throws IOException {
        socket.getOutputStream().write(message.toByteArray());
    }

    void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
