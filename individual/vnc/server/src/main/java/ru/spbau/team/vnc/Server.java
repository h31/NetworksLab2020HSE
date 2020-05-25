package ru.spbau.team.vnc;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    public final static int MAJOR_VERSION = 3;
    public final static int MINOR_VERSION = 8;

    private final ServerSocket socket;
    /** True if there is client with false as shared flag */
    private volatile boolean hasPrivateClient = false;
    private final List<Connection> connections = new ArrayList<>();
    private final Parameters defaultParameters;

    public Server(int port, String name) throws IOException {
        socket = new ServerSocket(port);
        defaultParameters = Parameters.getDefaultWithName(name);
    }

    public void start() {
        while (true) {
            try {
                var newConnection = new Connection(socket.accept(), this, defaultParameters.clone());
                if (hasPrivateClient) {
                    newConnection.disconnect();
                    continue;
                }
                // Synchronized because in future we will delete from connections list from other thread
                synchronized (connections) {
                    connections.add(newConnection);
                }
                new Thread(newConnection::run).start();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {
        Server server;
        try {
            server = new Server(5900, "TODO: name");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        new Thread(server::start).start();

        while (true) {
            // TODO
        }
    }
}
