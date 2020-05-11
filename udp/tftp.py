import socket 

BUFFER_SIZE = 256
RRQ, WRQ, DATA, ACK, ERROR = 1, 2, 3, 4, 5
ERROR_CODE = [
 "Not defined, see error message (if any).", 
 "File not found.",
 "Access violation.",
 "Disk full or allocation exceeded.",
 "Illegal TFTP operation.",
 "Unknown transfer ID.",
 "File already exists.",
 "No such user."]


def put(socket, fd):
    block = fd.read(BUFFER_SIZE)
    
    while len(block) > 0:
        data, server = socket.recieve()
        opcode = get_opcode(data)
        if opcode == ERROR:
            print_error(data)
            break
        elif opcode == ACK:
            data_request = socket.construct_data(data[2:4], block)
            socket.send(data_request, server)

def get(socket):
    block_num = 0
    result = bytearray()
    while True:
        data, server = socket.recieve()
        opcode = get_opcode(data)
        
        if opcode == ERROR:
            print_error(data)
            break
        elif opcode == DATA:
            ack_request = socket.construct_ack(data[2:4])
            socket.send(ack_request, server)
            
            result += data[4:]
            if len(data) < BUFFER_SIZE:
                break
            block_num += 1
    return result

def construct_rw(opcode, filename, mode="octet"):
    request = bytearray()
    request.append(0)
    request.append(opcode)
    request += bytearray(filename.encode('utf-8'))
    request.append(0)
    request += bytearray(bytes(mode, 'utf-8'))
    request.append(0)
    return request

def construct_ack(ack_data):
    ack = bytearray(ack_data)
    ack[0] = 0
    ack[1] = ACK
    return ack

def construct_data(block_num, data):
    request = bytearray()
    request.append(0)
    request.append(DATA)
    request += block_num.to_bytes(2, 'big')
    request += data
    return request

def get_block_num(data):
    return int.from_bytes(data[2:4], byteorder='big')

def get_opcode(data):
    return int.from_bytes(data[:2], byteorder='big') 

def print_error(data):
    error_code = int.from_bytes(data[2:4], byteorder='big')
    print(ERROR_CODE[error_code])

class SocketWrapper:
    def __init__(self, ip, port):
        self.udp_ip = ip
        self.udp_port = port
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    def send(self, message, server):
        self.socket.sendto(message, server)

    def send_wrq(self, filename):
        request = construct_rw(WRQ, filename)
        self.send(request, (self.udp_ip, self.udp_port))

    def send_rrq(self, filename):
        request = construct_rw(RRQ, filename)
        self.send(request, (self.udp_ip, self.udp_port))

    def recieve(self):
        return self.socket.recvfrom(BUFFER_SIZE)