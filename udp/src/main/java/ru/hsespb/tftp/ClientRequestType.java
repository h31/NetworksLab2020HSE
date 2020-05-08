package ru.hsespb.tftp;

public enum ClientRequestType {
    READ, WRITE;

    private static final String READ_COMMAND = "read";
    private static final String WRITE_COMMAND = "write";

    public static ClientRequestType get(String command) {
        if (command.equals(READ_COMMAND)) {
            return READ;
        }
        if (command.equals(WRITE_COMMAND)) {
            return WRITE;
        }
        throw new RuntimeException("Invalid command from user: " + command);
    }
}
