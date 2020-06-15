import sys
import socket

sys.path.append('../')
from tftp import *


class tftpClient:
    MAX_SIZE = 4098

    def __init__(self, hostname, port, timeout):
        self.addr = hostname, port
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.settimeout(timeout)

    def read_file(self, file_name, src_file, mode="octet"):
        self.socket.sendto(ReadRequest(file_name, mode).serialize(), self.addr)

        cur_block_num = 1
        while True:
            raw_ans = self.socket.recv(self.MAX_SIZE)
            ans = deserialize_msg(raw_ans)
            if ans is not Data or ans.block_num != cur_block_num:
                raise Exception("unexpected answer", ans)

            self.socket.sendto(Acknowledgment(cur_block_num).serialize(), self.addr)
            src_file.write(ans.data)

            if len(ans.data) < 512:
                return
            cur_block_num += 1

    def write_file(self, file_name, src_file, mode="octet",):
        self.socket.sendto(WriteRequest(file_name, mode).serialize(), self.addr)

        raw_ans = self.socket.recv(self.MAX_SIZE)
        ans = deserialize_msg(raw_ans)
        if ans is not Acknowledgment or ans.block_num != 0:
            raise Exception("unexpected answer", ans)

        cur_block_num = 1
        while True:
            data = src_file.read(512)
            self.socket.sendto(Data(cur_block_num, data).serialize(), self.addr)

            raw_ans = self.socket.recv(self.MAX_SIZE)
            ans = deserialize_msg(raw_ans)
            if ans is not Acknowledgment or ans.block_num != cur_block_num:
                raise Exception("unexpected answer", ans)

            if len(data) < 512:
                return


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("missing host or port")
        exit(1)

    host = sys.argv[1]
    port = int(sys.argv[2])
    client = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    address = (host, port)
