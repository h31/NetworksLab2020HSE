package ru.hse.networks.tftp.parcel;

import java.nio.ByteBuffer;

public interface Parcel {
    ByteBuffer toBytes();

    int size();
}
