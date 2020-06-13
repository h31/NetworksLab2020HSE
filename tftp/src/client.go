package main

import (
	"bytes"
	"encoding/binary"
	"net"
	"./tftp"
)

type Client struct {
	host *net.UDPAddr
	conn *net.UDPConn
}

func NewClient(hostAddr string) (*Client, error) {
	host, err := net.ResolveUDPAddr("udp", hostAddr)
	if err != nil {
		return nil, err
	}

	return &Client{host: host}, nil
}

func (c *Client) Serve() error {
	return nil
}

func (c *Client) readRoutine(filename string, mode tftp.Mode) {
	for {
		initPacket := tftp.NewReadPacket(filename, mode)
		c.sendPacket(initPacket, c.host)
	}

}

func (c *Client) writeRoutine(filename string, mode tftp.Mode) {
	for {
		initPacket := tftp.NewWritePacket(filename, mode)
		c.sendPacket(initPacket, c.host)
	}
}

func (c *Client) sendPacket(packet tftp.Packet, addr *net.UDPAddr) error {
	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, packet)

	if _, err := c.conn.WriteToUDP(buf.Bytes(), addr); err != nil {
		return err
	}
	return nil
}

func main() {
	cl, _ := NewClient(":6969", )
}
