package ru.hse.networks.tftp;

import ru.hse.networks.tftp.parcel.ByteBufferUtils;
import ru.hse.networks.tftp.parcel.Parcel;
import ru.hse.networks.tftp.parcel.ParcelFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Random;

final public class TFTP {
    public static final int TIMEOUT_MS = 3000;
    public static final int STANDARD_TID = 69;
    public static final int MAX_TID = 65535;
    public static final int MAX_DATA_SIZE = 512;
    public static final int MAX_PACKET_SIZE = 516;
    public static final int MAX_RETRY_NUMBER = 3;

    private static final Random random = new Random();

    public static int createTid() {
        return random.nextInt(MAX_TID);
    }


    public boolean send(Parcel parcel, DatagramSocket socket, InetAddress address, int port) {
        var bytes = ByteBufferUtils.getBytes(parcel.toBytes());
        var packet = new DatagramPacket(bytes, bytes.length, address, port);
        try {
            socket.send(packet);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Receive parcel with timeout.
     *
     * @throws SocketTimeoutException if timeout has expired.
     * @throws IOException            if an I/O error occurs.
     */
    public DatagramPacket receive(DatagramSocket socket) throws IOException {
        var buffer = new byte[MAX_PACKET_SIZE];
        var packet = new DatagramPacket(buffer, buffer.length);
        socket.setSoTimeout(TIMEOUT_MS);
        socket.receive(packet);
        return packet;
    }
}
