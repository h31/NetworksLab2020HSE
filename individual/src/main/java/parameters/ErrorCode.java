package parameters;

public class ErrorCode {
    /**
     * Not defined, see error message (if any).
     **/
    public static final short NOT_DEFINED = 0;

    /**
     * File not found.
     **/
    public static final short FILE_NOT_FOUND = 1;

    /**
     * Access violation.
     **/
    public static final short ACCESS_VIOLATION = 2;

    /**
     * Disk full or allocation exceeded.
     **/
    public static final short MEMORY_ALLOCATION = 3;

    /**
     * Illegal TFTP operation.
     **/
    public static final short ILLEGAL_OPERATION = 4;

    /**
     * Unknown transfer ID.
     **/
    public static final short UNKNOWN_TID = 5;

    /**
     * File already exists.
     **/
    public static final short FILE_EXISTS = 6;

    /**
     * No such user.
     **/
    public static final short NO_SUCH_USER = 7;

    public static final String[] definition = {
            "Not defined, see error message (if any)",
            "File not found",
            "Access violation",
            "Disk full or allocation exceeded",
            "Illegal TFTP operation",
            "Unknown transfer ID",
            "File already exists",
            "No such user"
    };
}
