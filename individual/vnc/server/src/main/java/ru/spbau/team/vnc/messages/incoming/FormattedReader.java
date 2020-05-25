package ru.spbau.team.vnc.messages.incoming;

import ru.spbau.team.vnc.exceptions.ClientDisconnectedException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FormattedReader extends InputStream {

    private final InputStream inputStream;

    public FormattedReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    private static int fromBigEndian32(byte[] bigEndian) {
        return ByteBuffer
            .allocate(4)
            .put(bigEndian)
            .order(ByteOrder.BIG_ENDIAN)
            .flip()
            .getInt();
    }

    private static int fromBigEndian16(byte[] bigEndian) {
        return ByteBuffer
            .allocate(4)
            .put(new byte[] { 0, 0 })
            .put(bigEndian)
            .order(ByteOrder.BIG_ENDIAN)
            .flip()
            .getInt();
    }

    private static long fromBigEndianU32(byte[] bigEndian) {
        return ByteBuffer
            .allocate(8)
            .put(new byte[] { 0, 0, 0, 0 })
            .put(bigEndian)
            .order(ByteOrder.BIG_ENDIAN)
            .flip()
            .getLong();
    }

    public long readU32BigEndian() throws IOException, ClientDisconnectedException {
        byte[] buffer = inputStream.readNBytes(4);
        if (buffer.length != 4) {
            throw new ClientDisconnectedException();
        }
        return fromBigEndianU32(buffer);
    }

    public int readS32BigEndian() throws IOException, ClientDisconnectedException {
        byte[] buffer = inputStream.readNBytes(4);
        if (buffer.length != 4) {
            throw new ClientDisconnectedException();
        }
        return fromBigEndian32(buffer);
    }

    public int readU16BigEndian() throws IOException, ClientDisconnectedException {
        byte[] buffer = inputStream.readNBytes(2);
        if (buffer.length != 2) {
            throw new ClientDisconnectedException();
        }
        return fromBigEndian16(buffer);
    }

    public int readU8() throws IOException, ClientDisconnectedException {
        byte[] buffer = inputStream.readNBytes(1);
        if (buffer.length != 1) {
            throw new ClientDisconnectedException();
        }
        // TODO: normal cast to int. NB! just cast doesn't work because of sign
        return fromBigEndian16(new byte[] { 0, buffer[0] });
    }

    public boolean readBoolean() throws IOException, ClientDisconnectedException {
        int result = readU8();
        return result != 0;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }
}
