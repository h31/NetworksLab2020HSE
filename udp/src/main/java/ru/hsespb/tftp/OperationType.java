package ru.hsespb.tftp;

public enum OperationType {
    RRQ, WRQ, DATA, ACK, ERR;

    private static final byte OPCODE_RRQ = 1;
    private static final byte OPCODE_WRQ = 2;
    private static final byte OPCODE_DATA = 3;
    private static final byte OPCODE_ACK = 4;
    private static final byte OPCODE_ERR = 5;

    public static OperationType get(byte firstByte, byte opcodeByte) {
        if (firstByte != 0) {
            throw new TFTPException(ErrorType.ILLEGAL_OPERATION,
                    "The first byte of the opcode is not 0, found: " + firstByte);
        }
        if (opcodeByte == OPCODE_RRQ) {
            return RRQ;
        }
        if (opcodeByte == OPCODE_WRQ) {
            return WRQ;
        }
        if (opcodeByte == OPCODE_DATA) {
            return DATA;
        }
        if (opcodeByte == OPCODE_ACK) {
            return ACK;
        }
        if (opcodeByte == OPCODE_ERR) {
            return ERR;
        }

        throw new TFTPException(ErrorType.ILLEGAL_OPERATION,
                "Unknown second byte of opcode: " + opcodeByte);
    }

    public byte[] getOperationCode() {
        if (this == RRQ) {
            return new byte[]{0, OPCODE_RRQ};
        }
        if (this == WRQ) {
            return new byte[]{0, OPCODE_WRQ};
        }
        if (this == DATA) {
            return new byte[]{0, OPCODE_DATA};
        }
        if (this == ACK) {
            return new byte[]{0, OPCODE_ACK};
        }
        if (this == ERR) {
            return new byte[]{0, OPCODE_ERR};
        }

        throw new RuntimeException("Unexpected code type: " + this.name());
    }
}
