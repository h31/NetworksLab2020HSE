package ru.spbau.team.vnc;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Server {

    public final static int MAJOR_VERSION = 3;
    public final static int MINOR_VERSION = 8;

    private final ServerSocket socket;
    /* True if there is client with false as shared flag */
    private volatile boolean hasPrivateClient = false;
    private volatile Connection privateConnection;
    private volatile boolean stopped;
    private final Map<Connection, Thread> connections = new HashMap<>();
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

                synchronized (connections) {
                    connections.put(newConnection, new Thread(() -> {
                        newConnection.run();
                        disconnect(newConnection);
                    }));
                }
                connections.get(newConnection).start();
            } catch (IOException e) {
                e.printStackTrace();
                if (stopped) {
                    List<Connection> serverConnections = List.copyOf(connections.keySet());
                    for (Connection serverConnection : serverConnections) {
                        serverConnection.stop();
                        disconnect(serverConnection);
                    }
                }
                return;
            }
        }
    }

    public static void main(String[] args) {
        Server server;
        try {
            server = new Server(5900, "VNC Server");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Thread acceptThread = new Thread(server::start);
        acceptThread.start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            if ("/exit".equals(scanner.next())) {
                try {
                    server.stopped = true;
                    server.socket.close();
                    acceptThread.join();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public void disconnect(Connection connection) {
        synchronized (connections) {
            if (connection == privateConnection) {
                privateConnection = null;
                hasPrivateClient = false;
            }
            connections.remove(connection);
        }
    }

    public boolean tryShare(Connection connection) {
        synchronized (connections) {
            if (hasPrivateClient) {
                return false;
            }

            hasPrivateClient = true;
            privateConnection = connection;
            List<Connection> serverConnections = List.copyOf(connections.keySet());
            for (Connection serverConnection : serverConnections) {
                if (serverConnection != connection) {
                    disconnect(serverConnection);
                }
            }
        }
        return true;
    }
}
