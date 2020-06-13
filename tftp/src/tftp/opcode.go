package tftp

import (
	"bytes"
	"encoding/binary"
	"errors"
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
