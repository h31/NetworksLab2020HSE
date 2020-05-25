package ru.spbau.team.vnc.messages.outcoming;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FormattedByteArrayWriter extends OutputStream {

    private final ByteArrayOutputStream outputStream;

    public FormattedByteArrayWriter(ByteArrayOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    private static byte[] toBigEndian32(int x) {
        return ByteBuffer
            .allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(x)
            .array();
    }

    private static byte[] toBigEndian16(int x) {
        byte[] bigEndian32 = toBigEndian32(x);
        return new byte[] { bigEndian32[2], bigEndian32[3] };
    }

    private static byte[] toLittleEndian16(int x) {
        byte[] bigEndian16 = toBigEndian16(x);
        return new byte[] { bigEndian16[1], bigEndian16[0] };
    }

    private static byte toByte(boolean x) {
        if (x) {
            return 1;
        } else {
            return 0;
        }
    }

    public void writeU16BigEndian(int number) throws IOException {
        outputStream.write(toBigEndian16(number));
    }

    public void writeS32BigEndian(int number) throws IOException {
        outputStream.write(toBigEndian32(number));
    }

    public void writeByte(int number) throws IOException {
        outputStream.write(number);
    }

    public void writeBytes(byte[] bytes) {
        outputStream.writeBytes(bytes);
    }

    public void writeBoolean(boolean value) {
        outputStream.write(toByte(value));
    }

    @Override
    public void write(int b) throws IOException {
        writeByte(b);
    }

    public byte[] toByteArray() {
        return outputStream.toByteArray();
    }
}
