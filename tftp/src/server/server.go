package main

import (
	"../tftp"
	"fmt"
	"io/ioutil"
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

		_, err = tftp.GetOpCode(buf)
		if err != nil {
			srv.sendPacket(*tftp.NewErrorPacket(tftp.ErrorIllegalOperation, ""), addr)
			continue
		}
		packet, err := tftp.Parse(buf)
		if err != nil {
			srv.sendPacket(*tftp.NewErrorPacket(tftp.ErrorNotDefined, err.Error()), addr)
			continue
		}

		fmt.Println("WOW")
		switch (*packet).OpCode() {
		case tftp.RRQ:
			//go srv.readRoutine((*packet).(*tftp.ReadRequestPacket).Filename, addr)
			srv.readRoutine((*packet).(*tftp.ReadRequestPacket).Filename, addr)
		case tftp.WRQ:
			//go srv.writeRoutine((*packet).(*tftp.WriteRequestPacket).Filename, addr)
			srv.writeRoutine((*packet).(*tftp.WriteRequestPacket).Filename, addr)
		default:
			srv.sendPacket(*tftp.NewErrorPacket(tftp.ErrorIllegalOperation, ""), addr)
		}

	}
	return nil
}

func (srv *Server) sendPacket(packet tftp.Packet, addr *net.UDPAddr) error {
	if _, err := srv.conn.WriteToUDP(packet.Bytes(), addr); err != nil {
		return err
	}
	return nil
}

func (srv *Server) readRoutine(filename string, addr *net.UDPAddr) {
	file, err := os.Open(filename)
	if err != nil {
		srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorFileNotFound, err.Error()), addr)
		return
	}
	defer file.Close()

	buf, err := ioutil.ReadFile(filename)
	if err != nil {
		srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorAccessViolation, err.Error()), addr)
	}

	var block uint16 = 0
	finish := len(buf)
	for {
		first := block * 512
		block += 1
		last := int(math.Min(float64(block * 512), float64(finish)))
		for {
			srv.sendPacket(tftp.NewDataPacket(block, buf[first:last]), addr)
			time.Sleep(time.Second)

			got := make([]byte, 1000)
			_, addrGot, err := srv.conn.ReadFromUDP(got[0:])
			if err != nil {
				return
			}
			if addrGot.String() != addr.String() {
				srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorUnknownTid, ""), addrGot)
				continue
			}

			op, err := tftp.GetOpCode(got)
			if err != nil {
				srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorIllegalOperation, ""), addr)
				continue
			}
			if *op != tftp.ACK {
				srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorNotDefined, ""), addr)
				continue
			}
			packet, err := tftp.Parse(got)
			if (*packet).(*tftp.AcknowledgementPacket).Block != block {
				continue
			}

			break
		}
		if last == finish {
			break
		}
	}
}

func (srv *Server) writeRoutine(filename string, addr *net.UDPAddr) {
	//_, err := os.Open(filename)
	//if err == nil {
	//	srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorFileAlreadyExists, ""), addr)
	//	return
	//}
	file, err := os.Create(filename)
	if err != nil {
		srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorAccessViolation, err.Error()), addr)
		return
	}
	defer file.Close()

	srv.sendPacket(tftp.NewAcknowledgementPacket(0), addr)

	var block uint16 = 0
	for {
		got := make([]byte, 1000)
		_, addrGot, err := srv.conn.ReadFromUDP(got[0:])
		if err != nil {
			return
		}
		if addrGot.String() != addr.String() {
			srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorUnknownTid, ""), addrGot)
			continue
		}

		op, err := tftp.GetOpCode(got)
		if err != nil {
			srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorIllegalOperation, ""), addr)
			continue
		}

		if *op != tftp.DATA {
			srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorNotDefined, ""), addr)
			continue
		}

		packet, err := tftp.Parse(got)
		if err != nil {
			srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorNotDefined, ""), addr)
			continue
		}
		if (*packet).(*tftp.DataPacket).Block > block + 1 {
			srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorNotDefined, ""), addr)
			continue
		}
		if (*packet).(*tftp.DataPacket).Block < block {
			srv.sendPacket(tftp.NewAcknowledgementPacket((*packet).(*tftp.DataPacket).Block), addr)
			continue
		}

		//  Write to file
		//fmt.Println((*packet).(*tftp.DataPacket).Data)
		_, err = file.Write((*packet).(*tftp.DataPacket).Data)
		if err != nil {
			srv.sendPacket(tftp.NewErrorPacket(tftp.ErrorDiskFill, err.Error()), addr)
			return
		}

		// Send ACK
		block += 1
		srv.sendPacket(tftp.NewAcknowledgementPacket(block), addr)

		// finish conn
		if len((*packet).(*tftp.DataPacket).Data) < 512 {
			break
		}
	}
}

func main() {
	srv, _ := NewServer(":6969")
	srv.Serve()
}