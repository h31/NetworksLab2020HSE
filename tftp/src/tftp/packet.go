package tftp

import (
	"bytes"
	"encoding/binary"
)

type Packet interface {
	OpCode() OpCode
	Bytes() []byte
}

func Parse(buf []byte) (*Packet, error) {
	op, err := GetOpCode(buf)
	if err != nil {
		return nil, err
	}

	var packet Packet
	switch *op {
	case RRQ:
		packet, err = ParseReadRequestPacket(buf)
	case WRQ:
		packet, err = ParseWriteRequestPacket(buf)
	case ACK:
		packet, err = ParseAcknowledgementPacket(buf)
	case DATA:
		packet, err = ParseDataPacket(buf)
	case ERROR:
		packet, err = ParseErrorPacket(buf)
	}

	if err != nil {
		return nil, err
	} else {
		return &packet, nil
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

func ParseReadRequestPacket(buf []byte) (*ReadRequestPacket, error) {
	bytesBuffer := bytes.NewBuffer(buf)
	p := ReadRequestPacket{}

	var op OpCode
	if err := binary.Read(bytesBuffer, binary.BigEndian, &op); err != nil {
		return nil, err
	}
	p.Code = op

	filenameStr, err := bytesBuffer.ReadString(0x00)
	if err != nil {
		return nil, err
	}
	p.Filename = filenameStr[:len(filenameStr)-1]

	modeStr, err := bytesBuffer.ReadString(0x00)
	if err != nil {
		return nil, err
	}
	p.Mode = Mode(modeStr[:len(modeStr)-1])

	return &p, nil
}

func (p ReadRequestPacket) OpCode() OpCode {
	return p.Code
}

func (p ReadRequestPacket) Bytes() []byte {
	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, p.Code)
	buf.WriteString(p.Filename)
	binary.Write(&buf, binary.BigEndian, p.EmptyByte1)
	buf.WriteString(string(p.Mode))
	binary.Write(&buf, binary.BigEndian, p.EmptyByte2)
	return buf.Bytes()
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

func ParseWriteRequestPacket(buf []byte) (*WriteRequestPacket, error) {
	bytesBuffer := bytes.NewBuffer(buf)
	p := WriteRequestPacket{}

	var op OpCode
	if err := binary.Read(bytesBuffer, binary.BigEndian, &op); err != nil {
		return nil, err
	}
	p.Code = op

	filenameStr, err := bytesBuffer.ReadString(0x00)
	if err != nil {
		return nil, err
	}
	p.Filename = filenameStr[:len(filenameStr)-1]

	modeStr, err := bytesBuffer.ReadString(0x00)
	if err != nil {
		return nil, err
	}
	p.Mode = Mode(modeStr[:len(modeStr)-1])

	return &p, nil
}

func (p WriteRequestPacket) OpCode() OpCode {
	return p.Code
}

func (p WriteRequestPacket) Bytes() []byte {
	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, p.Code)
	buf.WriteString(p.Filename)
	binary.Write(&buf, binary.BigEndian, p.EmptyByte1)
	buf.WriteString(string(p.Mode))
	binary.Write(&buf, binary.BigEndian, p.EmptyByte2)
	return buf.Bytes()
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

func ParseDataPacket(buf []byte) (*DataPacket, error) {
	bytesBuffer := bytes.NewBuffer(buf)
	p := DataPacket{}

	var op OpCode
	if err := binary.Read(bytesBuffer, binary.BigEndian, &op); err != nil {
		return nil, err
	}
	p.Code = op

	if err := binary.Read(bytesBuffer, binary.BigEndian, &p.Block); err != nil {
		return nil, err
	}

	p.Data = bytesBuffer.Bytes()

	return &p, nil
}

func (p DataPacket) OpCode() OpCode {
	return p.Code
}

func (p DataPacket) Bytes() []byte {
	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, p.Code)
	binary.Write(&buf, binary.BigEndian, p.Block)
	buf.Write(p.Data)
	return buf.Bytes()
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

func ParseAcknowledgementPacket(buf []byte) (*AcknowledgementPacket, error) {
	bytesBuffer := bytes.NewBuffer(buf)
	p := AcknowledgementPacket{}

	var op OpCode
	if err := binary.Read(bytesBuffer, binary.BigEndian, &op); err != nil {
		return nil, err
	}
	p.Code = op

	if err := binary.Read(bytesBuffer, binary.BigEndian, &p.Block); err != nil {
		return nil, err
	}

	return &p, nil
}

func (p AcknowledgementPacket) OpCode() OpCode {
	return p.Code
}

func (p AcknowledgementPacket) Bytes() []byte {
	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, p.Code)
	binary.Write(&buf, binary.BigEndian, p.Block)
	return buf.Bytes()
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

func ParseErrorPacket(buf []byte) (*ErrorPacket, error) {
	bytesBuffer := bytes.NewBuffer(buf)
	p := ErrorPacket{}

	var op OpCode
	if err := binary.Read(bytesBuffer, binary.BigEndian, &op); err != nil {
		return nil, err
	}
	p.Code = op

	if err := binary.Read(bytesBuffer, binary.BigEndian, &p.Error); err != nil {
		return nil, err
	}

	errStr, err := bytesBuffer.ReadString(0x00)
	if err != nil {
		return nil, err
	}
	p.Message = errStr[:len(errStr)-1]

	return &p, nil
}

func (p ErrorPacket) OpCode() OpCode {
	return p.Code
}

func (p ErrorPacket) Bytes() []byte {
	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, p.Code)
	binary.Write(&buf, binary.BigEndian, p.Error)
	buf.WriteString(p.Message)
	binary.Write(&buf, binary.BigEndian, p.EmptyByte)
	return buf.Bytes()
}

func NewErrorPacket(code ErrorCode, msg string) *ErrorPacket {
	return &ErrorPacket{
		Code: ERROR,
		Error: code,
		Message: msg,
		EmptyByte: 0,
	}
}
