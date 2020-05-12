package ru.hse.networks.tftp;

import ru.hse.networks.tftp.parcel.*;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Client {

    private final int tid = TFTP.createTid();
    private int remoteTid = TFTP.STANDARD_TID;
    private final InetAddress host;
    private final TFTP tftp = new TFTP();
    private final DatagramSocket socket = new DatagramSocket(tid);

    public Client(String host) throws UnknownHostException, SocketException {
        this.host = InetAddress.getByName(host);
    }

    /**
     * Send file to server.
     *
     * @return true on success, false on fail
     */
    public boolean send(String remoteFileName, File file, Mode mode) throws IOException {
        if (!(file.exists() && file.canRead())) {
            throw new IOException();
        }
        var request = new WriteParcel(mode, remoteFileName);
        tftp.send(request, socket, host, remoteTid);
        byte[] data = null;
        short currentBlock = -1;
        short finalBlock = -1;
        retry:
        for (int i = 0; i < TFTP.MAX_RETRY_NUMBER; i++) {
            while (true) {
                DatagramPacket packet;
                try {
                    packet = tftp.receive(socket);
                } catch (SocketTimeoutException e) {
                    if (currentBlock == -1) {
                        tftp.send(request, socket, host, remoteTid);
                    } else {
                        sendData(currentBlock, data);
                    }
                    continue retry;
                }
                remoteTid = packet.getPort();
                final var parcel = ParcelFactory.fromBytes(ByteBuffer.wrap(packet.getData()));
                if (parcel instanceof AcknowledgementParcel) {
                    var ackParcel = (AcknowledgementParcel) parcel;
                    currentBlock = ackParcel.getBlockNumber();
                    if (currentBlock == finalBlock) {
                        return true;
                    }
                    data = mode.read(file, TFTP.MAX_DATA_SIZE * currentBlock);
                    if (data.length < TFTP.MAX_DATA_SIZE) {
                        finalBlock = (short) (currentBlock + 1);
                    }
                    System.out.println("Send packet " + currentBlock);
                    sendData((short) (currentBlock + 1), data);
                } else if (parcel instanceof ErrorParcel) {
                    processError((ErrorParcel) parcel);
                    return false;
                } else {
                    throw new IllegalStateException("Unexpected parcel: " + parcel.toString());
                }
            }
        }
        return false;
    }

    /**
     * Load file from server.
     *
     * @return true on success, false on fail
     */
    public boolean load(String remoteFileName, File file, Mode mode) throws IOException {
        if (!file.createNewFile()) {
            return false;
        }
        final var request = new ReadParcel(mode, remoteFileName);
        tftp.send(request, socket, host, remoteTid);
        byte[] data;
        short currentBlockNumber = 0;
        retry:
        for (int i = 0; i < TFTP.MAX_RETRY_NUMBER; i++) {
            do {
                DatagramPacket packet;
                try {
                    packet = tftp.receive(socket);
                } catch (SocketTimeoutException e) {
                    if (currentBlockNumber == 0) {
                        tftp.send(request, socket, host, remoteTid);
                    } else {
                        sendAcknowledgement(currentBlockNumber);
                    }
                    continue retry;
                }
                remoteTid = packet.getPort();
                final var parcel = ParcelFactory.fromBytes(ByteBuffer.wrap(Arrays.copyOf(packet.getData(), packet.getLength())));
                if (parcel instanceof DataParcel) {
                    final var dataParcel = (DataParcel) parcel;
                    currentBlockNumber = dataParcel.getBlockNumber();
                    sendAcknowledgement(currentBlockNumber);
                    data = dataParcel.getBytes();
                    mode.write(file, data);
                } else if (parcel instanceof ErrorParcel) {
                    processError((ErrorParcel) parcel);
                    return false;
                } else {
                    throw new IllegalStateException("Unexpected parcel: " + parcel.toString());
                }
            } while (data.length == TFTP.MAX_DATA_SIZE);
            return true;
        }
        return false;
    }

    private void sendAcknowledgement(short blockNumber) {
        tftp.sendAcknowledgement(blockNumber, socket, host, remoteTid);
    }

    private void sendData(short blockNumber, byte[] data) {
        tftp.send(new DataParcel(blockNumber, data), socket, host, remoteTid);
    }

    private void processError(ErrorParcel errorParcel) {
        System.out.println("Error " + errorParcel.getErrorCode() + ": " + errorParcel.getErrorMessage());
    }

}
