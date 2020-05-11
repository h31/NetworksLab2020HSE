package requests;

public abstract class Packet {
    private final short opCode;

    public Packet(short opCode) {
        this.opCode = opCode;
    }

    public short getOpCode() {
        return opCode;
    }
    public abstract int size();

    public abstract byte[] serialize();
}
