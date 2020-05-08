package ru.hsespb.tftp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 69;
    private static final int BUFFER_SIZE = 1024;

    private final ExecutorService pool;

    public Server() {
        pool = Executors.newCachedThreadPool();
    }

    public void start() {
        try (var serverSocket = new DatagramSocket(PORT)) {
            while (true) {
                acceptConnection(serverSocket);
                Thread.sleep(1000);
            }
        } catch(Exception e) {
            pool.shutdownNow();
            System.err.println("Error while accepting a connection. " + e.getMessage());
            System.exit(-1);
        }
    }

    private void acceptConnection(DatagramSocket serverSocket) throws IOException {
        var buffer = new byte[BUFFER_SIZE];
        var datagramPacket = new DatagramPacket(buffer, buffer.length);
        serverSocket.receive(datagramPacket);
        pool.execute(new MultiServerThread(datagramPacket));

    }
}
