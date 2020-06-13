package tftp

import (
	"bytes"
	"encoding/binary"
)

type Packet interface {
	Parse([]byte) error
	OpCode() OpCode
}

func Parse(buf []byte) (Packet, error) {
	op, err := GetOpCode(buf)
	if err != nil {
		return nil, err
	}

	var packet Packet
	switch *op {
	case RRQ:
		err = packet.(ReadRequestPacket).Parse(buf)
	case WRQ:
		err = packet.(WriteRequestPacket).Parse(buf)
	case ACK:
		err = packet.(AcknowledgementPacket).Parse(buf)
	case DATA:
		err = packet.(DataPacket).Parse(buf)
	case ERROR:
		err = packet.(ErrorPacket).Parse(buf)
	}

	if err != nil {
		return nil, err
	} else {
		return packet, nil
	}
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

func (p ReadRequestPacket) OpCode() OpCode {
	return p.Code
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

func (p WriteRequestPacket) OpCode() OpCode {
	return p.Code
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

func (p DataPacket) OpCode() OpCode {
	return p.Code
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

func (p AcknowledgementPacket) OpCode() OpCode {
	return p.Code
}

func NewAcknowledgementPacket(block uint16) *AcknowledgementPacket {
	return &AcknowledgementPacket{
		Code: ACK,
		Block: block,
	}
}

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

func (p ErrorPacket) OpCode() OpCode {
	return p.Code
}

func NewErrorPacket(code ErrorCode, msg string) *ErrorPacket {
	return &ErrorPacket{
		Code: ERROR,
		Error: code,
		Message: msg,
		EmptyByte: 0,
	}
}
