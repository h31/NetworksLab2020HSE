package ru.spbau.team.vnc;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    public final static int MAJOR_VERSION = 3;
    public final static int MINOR_VERSION = 3;

    private final ServerSocket socket;
    private final List<Connection> connections = new ArrayList<>();

    public Server(int port) throws IOException {
        socket = new ServerSocket(port);
    }

    public void start() {
        while (true) {
            try {
                var newConnection = new Connection(socket.accept(), this);
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
            server = new Server(5900);
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
