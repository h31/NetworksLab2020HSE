package ru.hse.networks.tftp;

import ru.hse.networks.tftp.parcel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Client {

    private final int tid = TFTP.createTid();
    private int remoteTid = TFTP.STANDARD_TID;
    private final InetAddress host;
    private final TFTP tftp = new TFTP();

    public Client(String host) throws UnknownHostException {
        this.host = InetAddress.getByName(host);
    }

    /**
     * Send file to server.
     *
     * @return true on success, false on fail
     */
    public boolean send(String remoteFileName, File file, Mode mode) {
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
        var socket = new DatagramSocket(tid);
        var request = new ReadParcel(mode, remoteFileName);
        tftp.send(request, socket, host, remoteTid);
        byte[] data;
        do {
            var packet = tftp.receive(socket);
            remoteTid = packet.getPort();
            var parcel = ParcelFactory.fromBytes(ByteBuffer.wrap(packet.getData()));
            if (parcel instanceof DataParcel) {
                var dataParcel = (DataParcel) parcel;
                tftp.send(new AcknowledgementParcel(dataParcel.getBlockNumber()), socket, host, remoteTid);
                data = dataParcel.getBytes();
                switch (mode) {
                    case NETASCII:
                        try (var os = new FileWriter(file)) {
                            os.write(new String(data));
                        }
                        break;
                    case OCTET:
                        try (var os = new FileOutputStream(file)) {
                            os.write(data);
                        }
                        break;
                }
            } else if (parcel instanceof ErrorParcel) {
                var errorParcel = (ErrorParcel) parcel;
                System.out.println("Error " + errorParcel.getErrorCode() + ": " + errorParcel.getErrorMessage());
                return false;
            } else {
                throw new IllegalStateException("Unexpected parcel: " + parcel.toString());
            }
        } while (data.length == TFTP.MAX_DATA_SIZE);
        return true;
    }

}
