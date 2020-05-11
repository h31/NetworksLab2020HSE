package ru.hse.networks.tftp.parcel;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

final public class ParcelFactory {
    private static final Map<Short, Function<ByteBuffer, Parcel>> map = new HashMap<>();

    private ParcelFactory() {
    }

    static {
        map.put(OpCode.READ.getCode(), ReadParcel::fromBytes);
        map.put(OpCode.WRITE.getCode(), WriteParcel::fromBytes);
        map.put(OpCode.DATA.getCode(), DataParcel::fromBytes);
        map.put(OpCode.ACKNOWLEDGEMENT.getCode(), AcknowledgementParcel::fromBytes);
        map.put(OpCode.ERROR.getCode(), ErrorParcel::fromBytes);
    }

    public static Parcel fromBytes(ByteBuffer buffer) {
        final var opCode = buffer.getShort();
        final var creator = map.get(opCode);
        if (creator == null) {
            throw new IllegalStateException("Invalid OpCode");
        }
        return creator.apply(buffer);
    }
}
