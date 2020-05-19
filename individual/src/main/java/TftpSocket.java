import requests.Packet;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

import static parameters.TftpPacketInfo.PACKET_MAX_LENGTH;

class TftpSocket {
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_ATTEMPTS = 5;

    private final int attempts;
    private final InetAddress dstAddress;
    private final DatagramSocket socket;
    private int dstPort;

    TftpSocket(InetAddress dstAddress, int dstPort) throws SocketException {
        socket = new DatagramSocket();
        socket.setSoTimeout(DEFAULT_TIMEOUT_MS);
        this.attempts = DEFAULT_ATTEMPTS;
        this.dstAddress = dstAddress;
        this.dstPort = dstPort;
    }

    public void establish(int newDstPort) {
        dstPort = newDstPort;
    }

    public boolean validate(DatagramPacket datagramPacket) {
        return datagramPacket.getAddress() == dstAddress && datagramPacket.getPort() == dstPort;
    }

    public DatagramPacket receive(DatagramPacket requestPacket) throws IOException, TftpException {
        var buffer = ByteBuffer.allocate(PACKET_MAX_LENGTH);
        var responsePacket = new DatagramPacket(buffer.array(), buffer.limit(), dstAddress, dstPort);
        for (var attempt = 0; attempt < attempts; ++attempt) {
            try {
                socket.receive(responsePacket);
                return responsePacket;
            } catch (SocketTimeoutException e) {
                System.err.println(String.format("Timeout exception: %d/%d attempt to receive packet", attempt + 1, attempts));
                socket.send(requestPacket);
            }
        }
        throw new TftpException("Timeout exception");
    }

    public DatagramPacket send(Packet packet) throws IOException {
        var datagramPacket = new DatagramPacket(packet.serialize(), packet.size(), dstAddress, dstPort);
        socket.send(datagramPacket);
        return datagramPacket;
    }

    public void close() {
        socket.close();
    }
}