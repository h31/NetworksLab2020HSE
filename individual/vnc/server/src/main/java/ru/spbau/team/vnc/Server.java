package ru.spbau.team.vnc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private final int port;
    private final ServerSocket socket;

    public Server(int port) throws IOException {
        this.port = port;
        socket = new ServerSocket(port);
    }

    public void start() {
        while (true) {
            try {
                Socket newConnection = socket.accept();

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {

    }
}
