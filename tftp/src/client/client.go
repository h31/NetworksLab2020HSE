package main

import (
	"../tftp"
	"fmt"
	"io/ioutil"
	"log"
	"math"
	"net"
	"time"
)

type Client struct {
	host *net.UDPAddr
	conn *net.UDPConn
}

func NewClient(addr, hostAddr string) (*Client, error) {
	host, err := net.ResolveUDPAddr("udp", hostAddr)
	if err != nil {
		return nil, err
	}
	addrUDP, err := net.ResolveUDPAddr("udp", addr)
	if err != nil {
		return nil, err
	}

	conn, err := net.ListenUDP("udp", addrUDP)
	if err != nil {
		return nil, err
	}

	return &Client{host: host, conn: conn}, nil
}

func (c *Client) Read(filename string, mode tftp.Mode) {
	initPacket := tftp.NewReadPacket(filename, mode)
	c.sendPacket(initPacket, c.host)

	// get DATA
	var block uint16 = 0
	for {
		got := make([]byte, 1000)
		_, addrGot, err := c.conn.ReadFromUDP(got[0:])
		if err != nil {
			return
		}
		if addrGot.String() != c.host.String() {
			c.sendPacket(tftp.NewErrorPacket(tftp.ErrorUnknownTid, ""), addrGot)
			continue
		}

		//check is it DATA
		op, err := tftp.GetOpCode(got)
		if err != nil {
			c.sendPacket(tftp.NewErrorPacket(tftp.ErrorIllegalOperation, ""), addrGot)
			continue
		}
		if *op != tftp.DATA {
			c.sendPacket(tftp.NewErrorPacket(tftp.ErrorNotDefined, ""), addrGot)
			continue
		}
		//check block number and send previous ACK
		packet, err := tftp.Parse(got)
		if (*packet).(*tftp.DataPacket).Block != block+1 {
			c.sendPacket(tftp.NewAcknowledgementPacket(block), c.host)
			continue
		}

		//save read data
		fmt.Println(string(got))

		//send ACk
		c.sendPacket(tftp.NewAcknowledgementPacket(block), c.host)
	}
}

func (c *Client) Write(buf []byte, filename string, mode tftp.Mode) {
	initPacket := tftp.NewWritePacket(filename, mode)
	// get ACK for init
	for {
		c.sendPacket(initPacket, c.host)
		time.Sleep(time.Second)

		//get answer
		got := make([]byte, 1000)
		_, addrGot, err := c.conn.ReadFromUDP(got[0:])
		if err != nil {
			return
		}
		if addrGot.String() != c.host.String() {
			c.sendPacket(tftp.NewErrorPacket(tftp.ErrorUnknownTid, ""), addrGot)
			continue
		}

		//check is it ACK
		op, err := tftp.GetOpCode(got)
		if err != nil {
			c.sendPacket(tftp.NewErrorPacket(tftp.ErrorIllegalOperation, ""), addrGot)
			continue
		}
		if *op != tftp.ACK {
			c.sendPacket(tftp.NewErrorPacket(tftp.ErrorNotDefined, ""), addrGot)
			continue
		}
		//check is it the right ACK
		packet, err := tftp.Parse(got)
		if (*packet).(*tftp.AcknowledgementPacket).Block != 0 {
			continue
		}
		// here we sure and stop trying
		break
	}

	//do writing
	var block uint16 = 0
	finish := len(buf)
	for {
		first := block * 512
		block += 1
		last := int(math.Min(float64(block * 512), float64(finish)))

		// seding and getting ACK
		for {
			c.sendPacket(tftp.NewDataPacket(block, buf[first:last]), c.host)
			time.Sleep(time.Second)

			got := make([]byte, 1000)
			_, addrGot, err := c.conn.ReadFromUDP(got[0:])
			if err != nil {
				return
			}
			if addrGot.String() != c.host.String() {
				c.sendPacket(tftp.NewErrorPacket(tftp.ErrorUnknownTid, ""), addrGot)
				continue
			}

			op, err := tftp.GetOpCode(got)
			if err != nil {
				c.sendPacket(tftp.NewErrorPacket(tftp.ErrorIllegalOperation, ""), c.host)
				continue
			}
			if *op != tftp.ACK {
				c.sendPacket(tftp.NewErrorPacket(tftp.ErrorNotDefined, ""), c.host)
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

func (c *Client) sendPacket(packet tftp.Packet, addr *net.UDPAddr) error {
	if _, err := c.conn.WriteToUDP(packet.Bytes(), addr); err != nil {
		return err
	}
	return nil
}

func main() {
	cl, err := NewClient(":8080", "127.0.0.1:6969")
	if err != nil {
		log.Fatal(err)
	}
	//file, _ := os.Open("test.txt")
	//var buf []byte
	//file.Read(buf)
	buf, err := ioutil.ReadFile("test.txt")
	if err != nil {
		log.Fatal(err)
	}
	cl.Write(buf, "test.txt", tftp.NETASCII)
}
