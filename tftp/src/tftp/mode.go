package tftp

import "strings"

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
