package ru.spbau.team.vnc;

import ru.spbau.team.vnc.messages.incoming.VersionSelectMessage;
import ru.spbau.team.vnc.messages.outcoming.OutcomingMessage;
import ru.spbau.team.vnc.messages.outcoming.ProtocolVersionMessage;

import java.io.IOException;
import java.net.Socket;

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
        protocolVersionHandshake();
    }

    private void protocolVersionHandshake() throws IOException {
        sendMessage(new ProtocolVersionMessage(Server.MAJOR_VERSION, Server.MINOR_VERSION));
        VersionSelectMessage.fromInputStream(socket.getInputStream());
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
