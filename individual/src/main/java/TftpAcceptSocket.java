import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

class TftpAcceptSocket implements Closeable {

    private static final int BUFFER_SIZE = 1024;
    private final DatagramSocket socket;

    TftpAcceptSocket(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }

    public DatagramPacket accept() throws IOException {
        var buffer = new byte[BUFFER_SIZE];
        var packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    @Override
    public void close() {
        socket.close();
    }
}