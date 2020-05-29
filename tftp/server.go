package main

import (
	"bytes"
	"encoding/binary"
	"net"
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
	//defer conn.Close()
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

		op, err := GetOpCode(buf)
		if err != nil {
			srv.sendPacket(*NewErrorPacket(ErrorIllegalOperation, ""), addr)
		}

		switch *op {
		case RRQ:
			srv.sendPacket(*NewAcknowledgementPacket(0), addr)
			go srv.readRoutine()
		case WRQ:
			srv.sendPacket(*NewAcknowledgementPacket(0), addr)
			go srv.writeRoutine()
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

func (srv *Server) readRoutine() {

}

func (srv *Server) writeRoutine() {

}

func init() {
	srv, _ := NewServer(":69")
	srv.Serve()
}