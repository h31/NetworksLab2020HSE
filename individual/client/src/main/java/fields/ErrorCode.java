package fields;

public class ErrorCode {
    /** Not defined, see error message (if any). **/
    static final short NOT_DEFINED = 0;

    /** File not found. **/
    static final short FILE_NOT_FOUND = 1;

    /** Access violation. **/
    static final short ACCESS_VIOLATION = 2;

    /** Disk full or allocation exceeded. **/
    static final short MEMORY_ALLOCATION = 3;

    /** Illegal TFTP operation. **/
    static final short ILLEGAL_OPERATION = 4;

    /** Unknown transfer ID. **/
    static final short UNKNOWN_TID = 5;

    /** File already exists. **/
    static final short FILE_EXISTS = 6;

    /** No such user. **/
    static final short NO_SUCH_USER = 7;

    static final String[] definition = {
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
