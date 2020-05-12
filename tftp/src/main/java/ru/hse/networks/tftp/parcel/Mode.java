package ru.hse.networks.tftp.parcel;

import ru.hse.networks.tftp.TFTP;

import java.io.*;
import java.util.Arrays;

public enum Mode {
    NETASCII,
    OCTET;

    public void write(File file, byte[] data) throws IOException {
        switch (this) {
            case NETASCII:
                try (var os = new FileWriter(file, true)) {
                    os.write(new String(data));
                }
                break;
            case OCTET:
                try (var os = new FileOutputStream(file, true)) {
                    os.write(data);
                }
                break;
        }
    }

    public byte[] read(File file, int offset) throws IOException {
        switch (this) {
            case NETASCII:
                var charBuffer = new char[TFTP.MAX_DATA_SIZE];
                try (var is = new FileReader(file)) {
                    is.skip(offset);
                    var count = is.read(charBuffer);
                    return new String(charBuffer, 0, count).getBytes();
                }
            case OCTET:
                var buffer = new byte[TFTP.MAX_DATA_SIZE];
                try (var is = new FileInputStream(file)) {
                    is.skip(offset);
                    var count = is.read(buffer);
                    return Arrays.copyOf(buffer, count);
                }
            default:
                return null;
        }
    }
}
