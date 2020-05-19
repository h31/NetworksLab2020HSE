import requests.*;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

import static parameters.ErrorCode.UNKNOWN_TID;

public abstract class Session implements Closeable {

    private final TftpSocket tftpSocket;
    protected short blockNumber = 0;
    protected boolean isLastPacket = false;
    protected boolean established = false;

    public Session(InetAddress address, int port) throws SocketException {
        this.tftpSocket = new TftpSocket(address, port);
        System.out.println(String.format("Accepted new connection host: %s port %d", address.getHostAddress(), port));
    }

    protected void handleUnexpected(Packet packet) throws TftpException {
        throw new TftpException(String.format("Unsupported response: %d", +packet.getOpCode()));
    }

    protected void handleUnknown(DatagramPacket response) throws IOException {
        tftpSocket.send(new ErrorPacket(UNKNOWN_TID, String.format("Message received from unknown tid: %d", response.getPort())));
    }

    protected void handleError(ErrorPacket packet) throws TftpException {
        throw new TftpException(packet.getMessage());
    }

    protected void handleData(DataPacket packet, FileWriter fileWriter) throws IOException {
        isLastPacket = packet.isLastPacket();
        fileWriter.write(packet.getData());
    }

    protected DatagramPacket sendAck() throws IOException {
        return tftpSocket.send(new AckPacket(blockNumber));
    }

    protected void establishConnection(DatagramPacket response) {
        established = true;
        tftpSocket.establish(response.getPort());
    }

    protected DatagramPacket sendData(byte[] data) throws IOException {
        var dataPacket = new DataPacket(blockNumber, data);
        isLastPacket = dataPacket.isLastPacket();
        return tftpSocket.send(dataPacket);
    }

    protected DatagramPacket sendError(ErrorPacket errorPacket) throws IOException {
        return tftpSocket.send(errorPacket);
    }

    protected DatagramPacket sendRRQ(ReadPacket readPacket) throws IOException {
        return tftpSocket.send(readPacket);
    }

    protected DatagramPacket sendWRQ(WritePacket writePacket) throws IOException {
        return tftpSocket.send(writePacket);
    }

    protected DatagramPacket receive(DatagramPacket request) throws IOException, TftpException {
        return tftpSocket.receive(request);
    }

    protected boolean validate(DatagramPacket response) {
        return tftpSocket.validate(response);
    }

    @Override
    public void close() {
        tftpSocket.close();
    }

    protected abstract void handleRRQ() throws IOException, TftpException;

    protected abstract void handleWRQ() throws IOException, TftpException;
}
