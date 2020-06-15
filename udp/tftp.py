import socket


class tftpMassage:

    def __init__(self, opcode):
        self.opcode = opcode

    def serialize(self):
        pass


class Request(tftpMassage):
    OCTET_MODE = "octet"
    NETASCII_MODE = "netascii"

    def __init__(self, opcode, filename, mode=OCTET_MODE):
        super().__init__(opcode)
        self.filename = filename
        self.mode = mode

    def serialize(self):
        arr = bytearray([self.opcode])
        arr.extend(self.filename.encode("ascii"))
        arr.append(0)
        arr.extend(self.mode.encode("ascii"))
        arr.append(0)
        return arr


class ReadRequest(Request):
    def __init__(self, filename, mode="octet"):
        super().__init__(1, filename, mode)


class WriteRequest(Request):
    def __init__(self, filename, mode="octet"):
        super().__init__(2, filename, mode)


class Data(tftpMassage):
    def __init__(self, block_num, data):
        super().__init__(3)
        self.block_num = block_num
        self.data = data

    def serialize(self):
        arr = bytearray([self.opcode, self.block_num])
        arr.extend(self.data)
        return arr


class Acknowledgment(tftpMassage):
    def __init__(self, block_num):
        super().__init__(4)
        self.block_num = block_num

    def serialize(self):
        arr = bytearray([self.opcode, self.block_num])
        return arr


class Error(tftpMassage):
    def __init__(self, err_num, err_msg):
        super().__init__(4)
        self.err_msg = err_msg
        self.err_num = err_num

    def serialize(self):
        arr = bytearray([self.opcode, self.err_num])
        arr.extend(self.err_msg.encode("ascii"))
        arr.append(0)
        return arr


def deserialize_msg(msg_bytes):
    opcode = int(msg_bytes[:2])
    if opcode == 1 or opcode == 2:
        strings = msg_bytes[2:].split(b'\0', 2)
        file_name = strings[0].decode("ascii")
        mode = strings[1].decode("ascii")
        if opcode == 1:
            return ReadRequest(file_name, mode)
        else:
            return WriteRequest(file_name, mode)
    elif opcode == 3:
        block_num = int(msg_bytes[2:4])
        data = msg_bytes[4:]
        return Data(block_num, data)
    elif opcode == 4:
        block_num = int(msg_bytes[2:4])
        return Acknowledgment(block_num)
    elif opcode == 5:
        err_num = int(msg_bytes[2:4])
        err_msg = msg_bytes[4:].split('\0')[0]
    else:
        return None

#
# def recv_msg(sct, bufsz, repeat):
#     for _ in range(repeat):
#         try:
#             raw_ans = sct.recv(bufsz)
#         except socket.timeout:
#             continue
#
#         return deserialize_msg(raw_ans)
#
#     return None