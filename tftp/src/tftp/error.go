package tftp

//Error Codes:
//
//Value     Meaning
//
// 0        Not defined, see error message (if any).
// 1        File not found.
// 2        Access violation.
// 3        Disk full or allocation exceeded.
// 4        Illegal TFTP operation.
// 5        Unknown transfer ID.
// 6        File already exists.
// 7        No such user.

type ErrorCode uint16

const (
	ErrorNotDefined        ErrorCode = 0
	ErrorFileNotFound      ErrorCode = 1
	ErrorAccessViolation   ErrorCode = 2
	ErrorDiskFill          ErrorCode = 3
	ErrorIllegalOperation  ErrorCode = 4
	ErrorUnknownTid        ErrorCode = 5
	ErrorFileAlreadyExists ErrorCode = 6
	ErrorNoSuchUser        ErrorCode = 7
)
