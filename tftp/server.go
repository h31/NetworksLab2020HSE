package main

import (
	"bytes"
	"encoding/binary"
	"math"
	"net"
	"os"
	"time"
)

type Server struct {
	conn *net.UDPConn
	addr *net.UDPAddr
}

func NewServer(addr string) (*Server, error) {
	addrUDP, err := net.ResolveUDPAddr("udp", addr)
	if err != nil {
		return nil, err
	}
	conn, err := net.ListenUDP("udp", addrUDP)
	if err != nil {
		return nil, err
	}
	return &Server{conn: conn, addr: addrUDP}, nil
}

func (srv *Server) Serve() error {
	buf := make([]byte, 1000)
	for {
		_, addr, err := srv.conn.ReadFromUDP(buf[0:])
		if err != nil {
			return err
		}
		if addr != srv.addr {
			go srv.sendPacket(*NewErrorPacket(ErrorUnknownTid, ""), addr)
		}

		_, err = GetOpCode(buf)
		if err != nil {
			srv.sendPacket(*NewErrorPacket(ErrorIllegalOperation, ""), addr)
		}
		packet, err := Parse(buf)
		if err != nil {
			srv.sendPacket(*NewErrorPacket(ErrorNotDefined, err.Error()), addr)
		}

		switch packet.OpCode() {
		case RRQ:
			//srv.sendPacket(*NewAcknowledgementPacket(0), addr)
			go srv.readRoutine(packet.(ReadRequestPacket).Filename, addr)
		case WRQ:
			go srv.writeRoutine(packet.(WriteRequestPacket).Filename, addr)
		default:
			srv.sendPacket(*NewErrorPacket(ErrorIllegalOperation, ""), addr)
		}

	}
	return nil
}

func (srv *Server) sendPacket(packet Packet, addr *net.UDPAddr) error {
	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, packet)

	if _, err := srv.conn.WriteToUDP(buf.Bytes(), addr); err != nil {
		return err
	}
	return nil
}

func (srv *Server) readRoutine(filename string, addr *net.UDPAddr) {
	file, err := os.Open(filename)
	if err != nil {
		srv.sendPacket(NewErrorPacket(ErrorFileNotFound, err.Error()), addr)
		return
	}
	defer file.Close()

	var buf []byte
	n, err := file.Read(buf)
	if err != nil {
		srv.sendPacket(NewErrorPacket(ErrorAccessViolation, err.Error()), addr)
	}

	var block uint16 = 0
	for {
		start := block * 512
		block += 1
		end := int(math.Min(float64(block * 512), float64(n)))
		for {
			srv.sendPacket(NewDataPacket(block, buf[start:end]), addr)
			time.Sleep(time.Second)

			got := make([]byte, 1000)
			_, addrGot, err := srv.conn.ReadFromUDP(got[0:])
			if err != nil {
				return
			}
			if addrGot != nil {
				srv.sendPacket(NewErrorPacket(ErrorUnknownTid, ""), addrGot)
				continue
			}

			op, err := GetOpCode(got)
			if err != nil {
				srv.sendPacket(NewErrorPacket(ErrorIllegalOperation, ""), addr)
				continue
			}
			if *op != ACK {
				srv.sendPacket(NewErrorPacket(ErrorNotDefined, ""), addr)
				continue
			}
			packet, err := Parse(got)
			if packet.(AcknowledgementPacket).Block != block {
				continue
			}

			break
		}
	}
}

func (srv *Server) writeRoutine(filename string, addr *net.UDPAddr) {
	_, err := os.Open(filename)
	if err == nil {
		srv.sendPacket(NewErrorPacket(ErrorFileAlreadyExists, ""), addr)
		return
	}
	file, err := os.Create(filename)
	if err != nil {
		srv.sendPacket(NewErrorPacket(ErrorAccessViolation, error.Error()), addr)
		return
	}

	srv.sendPacket(NewAcknowledgementPacket(0), addr)

	var block uint16 = 0
	for {
		got := make([]byte, 1000)
		_, addrGot, err := srv.conn.ReadFromUDP(got[0:])
		if err != nil {
			return
		}
		if addrGot != addr {
			srv.sendPacket(NewErrorPacket(ErrorUnknownTid, ""), addrGot)
			continue
		}

		op, err := GetOpCode(got)
		if err != nil {
			srv.sendPacket(NewErrorPacket(ErrorIllegalOperation, ""), addr)
			continue
		}

		if *op != DATA {
			srv.sendPacket(NewErrorPacket(ErrorNotDefined, ""), addr)
			continue
		}

		packet, err := Parse(got)
		if err != nil {
			srv.sendPacket(NewErrorPacket(ErrorNotDefined, ""), addr)
			continue
		}
		if packet.(DataPacket).Block > block + 1 {
			srv.sendPacket(NewErrorPacket(ErrorNotDefined, ""), addr)
			continue
		}
		if packet.(DataPacket).Block < block {
			srv.sendPacket(NewAcknowledgementPacket(packet.(DataPacket).Block), addr)
			continue
		}

		//  Write to file
		file.Write(packet.(DataPacket).Data)

		// Send ACK
		block += 1
		srv.sendPacket(NewAcknowledgementPacket(block), addr)

		// finish conn
		if len(packet.(DataPacket).Data) < 512 {
			break
		}
	}
}

func init() {
	srv, _ := NewServer(":69")
	srv.Serve()
}