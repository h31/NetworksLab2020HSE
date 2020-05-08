package ru.hsespb.tftp;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class NetworkService {
    private static final int BUFFER_SIZE = 1024;
    private static final int RETRY = 5;
    private static final int TIMEOUT_MILLIS = 1000000;

    private final DatagramSocket datagramSocket;
    private final InetAddress remoteAddress;
    private int remotePort;

    public NetworkService(InetAddress remoteAddress, int remotePort) throws SocketException {
        datagramSocket = new DatagramSocket();
        datagramSocket.setSoTimeout(TIMEOUT_MILLIS);
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
    }

    public boolean isInvalidPacketSource(DatagramPacket packet) throws IOException {
        if (!packet.getAddress().equals(remoteAddress) || packet.getPort() != remotePort) {
            sendError(ErrorType.UNKNOWN_TID,
                    "Invalid packet source: " + packet.getAddress() + ":" + remotePort);
            return true;
        }
        return false;
    }

    public void setRemotePort(int port) {
        remotePort = port;
    }

    public byte[] getPacketData(DatagramPacket packet) {
        return Arrays.copyOf(packet.getData(), packet.getLength());
    }

    public DatagramPacket sendWRQ(String fileName) throws IOException {
        var message = new RequestMessage(OperationType.WRQ, fileName);
        return send(message.build());
    }

    public DatagramPacket sendRRQ(String fileName) throws IOException {
        var message = new RequestMessage(OperationType.RRQ, fileName);
        return send(message.build());
    }

    public DatagramPacket sendData(DataMessage dataMessage) throws IOException {
        return send(dataMessage.build());
    }

    public DatagramPacket sendAcknowledgement(int blockNumber) throws IOException {
        var message = new AcknowledgementMessage(blockNumber);
        return send(message.build());
    }

    public void sendError(ErrorType errorType, String errorMessage) throws IOException {
        var message = new ErrorMessage(errorType, errorMessage);
        send(message.build());
    }

    public DatagramPacket send(byte[] buffer) throws IOException {
        var packet = new DatagramPacket(buffer, buffer.length, remoteAddress, remotePort);
        datagramSocket.send(packet);
        return packet;
    }

    public DatagramPacket receive(DatagramPacket lastPacket) throws IOException {
        var buffer = new byte[BUFFER_SIZE];
        var packet = new DatagramPacket(buffer, buffer.length, remoteAddress, remotePort);
        for (int i = 0; i < RETRY; i++) {
            try {
                datagramSocket.receive(packet);
                return packet;
            } catch (SocketTimeoutException ignored) {
                System.err.println("Timeout occurred, trying once again...");
                datagramSocket.send(lastPacket);
            }
        }
        throw new SocketTimeoutException("A timeout occurred for " + RETRY + " times. Aborting.");
    }
}
