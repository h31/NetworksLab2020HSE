package main

import (
	"bytes"
	"encoding/binary"
	"errors"
	"strings"
)

//TFTP supports five types of packets, all of which have been mentioned above:
//
//opcode  operation
//  1     Read request (RRQ)
//  2     Write request (WRQ)
//  3     Data (DATA)
//  4     Acknowledgment (ACK)
//  5     Error (ERROR)

type OpCode uint16

const (
	RRQ   OpCode = 1
	WRQ   OpCode = 2
	DATA  OpCode = 3
	ACK   OpCode = 4
	ERROR OpCode = 5
)

func GetOpCode(buf []byte) (*OpCode, error)  {
	var op OpCode
	if err := binary.Read( bytes.NewBuffer(buf), binary.BigEndian, &op); err != nil {
		return nil, err
	}
	switch op {
	case RRQ:
		return &op, nil
	case WRQ:
		return &op, nil
	case DATA:
		return &op, nil
	case ACK:
		return &op, nil
	case ERROR:
		return &op, nil
	default:
		return nil, errors.New("Unknown operation code")
	}
}

type Mode string

const (
	NETASCII Mode = "netascii"
	OCTET    Mode = "octet"
	//MAIL Mode = "mail"
)

func (m Mode) Equals(mode Mode) bool {
	m_ := strings.ToLower(string(m))
	mode_ := strings.ToLower(string(mode))
	return m_ == mode_
}

type Packet interface {
	Parse([]byte) error
}

//        2 bytes    string   1 byte     string   1 byte
//       -----------------------------------------------
//RRQ/  | 01/02 |  Filename  |   0  |    Mode    |   0  |
//WRQ    -----------------------------------------------

type ReadRequestPacket struct {
	Code     OpCode
	Filename string
	EmptyByte1 int8
	Mode     Mode
	EmptyByte2 int8
}

func (p ReadRequestPacket) Parse(buf []byte) error {
	return nil
}

func NewReadPacket(filename string, mode Mode) *ReadRequestPacket {
	return &ReadRequestPacket{
		Code: RRQ,
		Filename: filename,
		EmptyByte1: 0,
		Mode: mode,
		EmptyByte2: 0,
	}
}

type WriteRequestPacket struct {
	Code     OpCode
	Filename string
	EmptyByte1 int8
	Mode     Mode
	EmptyByte2 int8
}

func (p WriteRequestPacket) Parse(buf []byte) error {
	return nil
}

func NewWritePacket(filename string, mode Mode) *WriteRequestPacket {
	return &WriteRequestPacket{
		Code: WRQ,
		Filename: filename,
		EmptyByte1: 0,
		Mode: mode,
		EmptyByte2: 0,
	}
}

//       2 bytes    2 bytes       n bytes
//       ---------------------------------
//DATA  | 03    |   Block #  |    Data    |
//       ---------------------------------

type DataPacket struct {
	Code  OpCode
	Block uint16
	Data  []byte
}

func (p DataPacket) Parse(buf []byte) error {
	return nil
}

func NewDataPacket(block uint16, data []byte) *DataPacket {
	return &DataPacket{
		Code: DATA,
		Block: block,
		Data: data,
	}
}

//       2 bytes    2 bytes
//        -------------------
//ACK   | 04    |   Block #  |
//       --------------------

type AcknowledgementPacket struct {
	Code  OpCode
	Block uint16
}

func (p AcknowledgementPacket) Parse(buf []byte) error {
	return nil
}

func NewAcknowledgementPacket(block uint16) *AcknowledgementPacket {
	return &AcknowledgementPacket{
		Code: ACK,
		Block: block,
	}
}

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

type ErrorCode int

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

//         2 bytes  2 bytes      string    1 byte
//        ----------------------------------------
// ERROR | 05    |  ErrorCode |   ErrMsg   |   0  |
//        ----------------------------------------

type ErrorPacket struct {
	Code    OpCode
	Error   ErrorCode
	Message string
	EmptyByte int8
}

func (p ErrorPacket) Parse(buf []byte) error {
	return binary.Read(bytes.NewBuffer(buf), binary.BigEndian, p)
}

func NewErrorPacket(code ErrorCode, msg string) *ErrorPacket {
	return &ErrorPacket{
		Code: ERROR,
		Error: code,
		Message: msg,
		EmptyByte: 0,
	}
}
