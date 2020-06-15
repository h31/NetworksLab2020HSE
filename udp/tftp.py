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
        arr = bytearray([0, self.opcode])
        arr.extend(self.filename.encode("ascii"))
        arr.append(0)
        arr.extend(self.mode.encode("ascii"))
        arr.append(0)
        return arr


class ReadRequest(Request):
    def __init__(self, filename, mode="octet"):
        super().__init__(1, filename, mode)

    def __repr__(self):
        return 'ReadRequest(%s, %s)' % (self.filename, self.mode)


class WriteRequest(Request):
    def __init__(self, filename, mode="octet"):
        super().__init__(2, filename, mode)

    def __repr__(self):
        return 'WriteRequest(%s, %s)' % (self.filename, self.mode)


class Data(tftpMassage):
    def __init__(self, block_num, data):
        super().__init__(3)
        self.block_num = block_num
        self.data = data

    def __repr__(self):
        return 'Data(%s, %s)' % (self.block_num, self.data)

    def serialize(self):
        arr = bytearray([0, self.opcode, 0, self.block_num])
        arr.extend(self.data)
        return arr


class Acknowledgment(tftpMassage):
    def __init__(self, block_num):
        super().__init__(4)
        self.block_num = block_num

    def __repr__(self):
        return 'Acknowledgment(%s)' % self.block_num

    def serialize(self):
        arr = bytearray([0, self.opcode, 0, self.block_num])
        return arr


class Error(tftpMassage):
    def __init__(self, err_num, err_msg):
        super().__init__(5)
        self.err_msg = err_msg
        self.err_num = err_num

    def __repr__(self):
        return 'Error(%s, %s)' % (self.err_num, self.err_msg)

    def serialize(self):
        arr = bytearray([0, self.opcode, 0, self.err_num])
        arr.extend(self.err_msg.encode("ascii"))
        arr.append(0)
        return arr


def deserialize_msg(msg_bytes):
    opcode = int.from_bytes(msg_bytes[:2], byteorder="big")
    # print(opcode)
    if opcode == 1 or opcode == 2:
        strings = msg_bytes[2:].split(b'\0', 2)
        file_name = strings[0].decode("ascii")
        mode = strings[1].decode("ascii")
        if mode != Request.NETASCII_MODE and mode != Request.OCTET_MODE:
            raise Exception("unknown request mode", mode)
        if opcode == 1:
            return ReadRequest(file_name, mode)
        else:
            return WriteRequest(file_name, mode)
    elif opcode == 3:
        block_num = int.from_bytes(msg_bytes[2:4], byteorder="big")
        data = msg_bytes[4:]
        return Data(block_num, data)
    elif opcode == 4:
        block_num = int.from_bytes(msg_bytes[2:4], byteorder="big")
        return Acknowledgment(block_num)
    elif opcode == 5:
        err_num = int.from_bytes(msg_bytes[2:4], byteorder="big")
        err_msg = msg_bytes[4:].split(b'\0')[0]
        return Error(err_num, err_msg)
    else:
        raise Exception("unknown opcode", opcode)
