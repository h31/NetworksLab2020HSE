package ru.spbau.team.vnc;

import ru.spbau.team.vnc.exceptions.ClientDisconnectedException;
import ru.spbau.team.vnc.messages.incoming.FormattedReader;
import ru.spbau.team.vnc.messages.incoming.ClientInitMessage;
import ru.spbau.team.vnc.messages.incoming.SecuritySelectMessage;
import ru.spbau.team.vnc.messages.incoming.VersionSelectMessage;
import ru.spbau.team.vnc.messages.incoming.routine.RoutineMessage;
import ru.spbau.team.vnc.messages.outcoming.*;
import ru.spbau.team.vnc.messages.outcoming.update.FramebufferUpdateRectangle;
import ru.spbau.team.vnc.messages.outcoming.update.encodings.RawEncodedRectangle;
import ru.spbau.team.vnc.security.SecurityType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Connection {

    private final Socket socket;
    private final Server server;
    private final Parameters parameters;
    private FormattedReader inputStream;

    public Connection(Socket socket, Server server, Parameters parameters) {
        this.socket = socket;
        this.server = server;
        this.parameters = parameters;
    }

    public void run() {
        try {
            inputStream = new FormattedReader(socket.getInputStream());
            if (!initConnection()) {
                disconnect();
                return;
            }
            routine();
        } catch (IOException | AWTException | ClientDisconnectedException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            disconnect();
        }
    }

    public void sendRawUpdate() throws AWTException, IOException {
        BufferedImage capture = new Robot().createScreenCapture(
            new Rectangle(0, 0, parameters.getFramebufferWidth(), parameters.getFramebufferHeight()));
        var updateRectangles = new ArrayList<FramebufferUpdateRectangle>();
        for (int row = 0; row < parameters.getFramebufferHeight(); ++row) {
            var rowRGB = new int[parameters.getFramebufferWidth()];
            for (int column = 0; column < parameters.getFramebufferWidth(); ++column) {
                rowRGB[column] = Integer.reverseBytes(capture.getRGB(column, row));
            }
            var encodedRectangle = new RawEncodedRectangle(rowRGB, parameters.getPixelFormat());
            var updateRectangle = new FramebufferUpdateRectangle(0, row,
                    parameters.getFramebufferWidth(), 1, encodedRectangle);
            updateRectangles.add(updateRectangle);
        }
        sendMessage(new FramebufferUpdateMessage(updateRectangles));
    }

    private boolean initConnection() throws IOException, ClientDisconnectedException {
        var selectedVersion = protocolVersionHandshake();
        if (!securityHandshake(selectedVersion)) {
            return false;
        }
        initialization();
        return true;
    }

    private void routine() throws IOException, AWTException, ClientDisconnectedException {
        while (true) {
            var routineMessage = RoutineMessage.fromInputStream(inputStream);
            if (routineMessage != null) {
                routineMessage.execute(this);
            }
        }
    }

    private VersionSelectMessage protocolVersionHandshake() throws IOException {
        sendMessage(new ProtocolVersionMessage(Server.MAJOR_VERSION, Server.MINOR_VERSION));
        return VersionSelectMessage.fromInputStream(socket.getInputStream());
    }

    private boolean securityHandshake(VersionSelectMessage selectedVersion)
            throws IOException, ClientDisconnectedException {
        if (versionIsNotSupported(selectedVersion)) {
            sendMessage(new SecurityTypesMessage(Collections.emptyList()));
            String errorMessage = "Version " + selectedVersion.getMajorVersion() + "." +
                    selectedVersion.getMinorVersion() + " is not supported";
            sendMessage(new SecurityFailureMessage(errorMessage));
            System.err.println(errorMessage);
            return false;
        } else {
            if (selectedVersion.getMinorVersion() == 3) {
                sendMessage(new SecurityResultMessage(true));
                return true;
            }

            sendMessage(new SecurityTypesMessage(Arrays.asList(SecurityType.INVALID, SecurityType.NONE)));
            var security = SecuritySelectMessage.fromInputStream(inputStream);

            if (security.getSecurityType() == SecurityType.INVALID) {
                sendMessage(new SecurityResultMessage(false));
                String errorMessage = "Invalid security code";
                sendMessage(new SecurityFailureMessage(errorMessage));
                System.err.println(errorMessage);
                return false;
            } else if (security.getSecurityType() == SecurityType.NONE) {
                if (selectedVersion.getMinorVersion() == 7) {
                    return true;
                }
                sendMessage(new SecurityResultMessage(true));
            } else {
                // TODO: Authentication
            }
        }
        return true;
    }

    private void initialization() throws IOException, ClientDisconnectedException {
        var isShared = ClientInitMessage.fromInputStream(inputStream).isShared();
        if (isShared) {
            // TODO: disconnect others
        }
        sendMessage(new ServerInitMessage(parameters));
    }

    private boolean versionIsNotSupported(VersionSelectMessage selectedVersion) {
        return selectedVersion.getMajorVersion() != 3 || (selectedVersion.getMinorVersion() != 3 &&
                selectedVersion.getMinorVersion() != 7 && selectedVersion.getMinorVersion() != 8);
    }

    private void sendMessage(OutcomingMessage message) throws IOException {
        socket.getOutputStream().write(message.toByteArray());
    }

    void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.disconnect(this);
        }
    }
}
