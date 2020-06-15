import sys
import os
import socket

sys.path.append('../')
from tftp import *


class tftpClient:
    MAX_SIZE = 4098
    DEFAULT_PORT = 69

    def __init__(self, hostname, timeout):
        self.host = hostname
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.settimeout(timeout)

    def get(self, file_name, src_file, mode):
        self.socket.sendto(ReadRequest(file_name, mode).serialize(), (self.host, self.DEFAULT_PORT))

        cur_block_num = 1
        while True:
            raw_ans, addr = self.socket.recvfrom(self.MAX_SIZE)
            ans = deserialize_msg(raw_ans)
            # print(ans.block_num, cur_block_num)
            if type(ans) is not Data or ans.block_num != cur_block_num:
                raise Exception("unexpected answer", ans)

            self.socket.sendto(Acknowledgment(cur_block_num).serialize(), addr)
            if mode == Request.OCTET_MODE:
                src_file.write(ans.data)
            else:
                decoded_str = ans.data.decode("ascii")
                decoded_str = decoded_str.replace("\r\n", "\n")
                src_file.write(decoded_str)

            if len(ans.data) < 512:
                return
            cur_block_num += 1

    def put(self, file_name, src_file, mode):
        self.socket.sendto(WriteRequest(file_name, mode).serialize(), (self.host, self.DEFAULT_PORT))

        raw_ans, addr = self.socket.recvfrom(self.MAX_SIZE)
        ans = deserialize_msg(raw_ans)
        if type(ans) is not Acknowledgment or ans.block_num != 0:
            raise Exception("unexpected answer", ans)
        cur_block_num = 1
        while True:
            data = src_file.read(512)
            data = data.encode("ascii")
            print(len(data))
            print(Data(cur_block_num, data).serialize())
            self.socket.sendto(Data(cur_block_num, data).serialize(), addr)

            raw_ans, addr = self.socket.recvfrom(self.MAX_SIZE)
            ans = deserialize_msg(raw_ans)
            if type(ans) is not Acknowledgment or ans.block_num != cur_block_num:
                raise Exception("unexpected answer at block #%s" % cur_block_num, ans)

            if len(data) < 512:
                return
            cur_block_num += 1


if __name__ == '__main__':
    if len(sys.argv) != 6:
        print("usage: python main.py host [GET|PUT] [octet|netascii] remote_file local_file")
        exit(1)

    host = sys.argv[1]
    command = sys.argv[2]
    mode = sys.argv[3]
    remote_file = sys.argv[4]
    local_file = sys.argv[5]

    client = tftpClient(host, 10)

    if mode == Request.OCTET_MODE:
        bin_opt = "b"
        encoding = None
    else:
        bin_opt = ""
        encoding = "ascii"

    if command == "GET":
        with open(local_file, "w" + bin_opt, encoding=encoding) as file:
            client.get(remote_file, file, mode)
    elif command == "PUT":
        with open(local_file, "r" + bin_opt, encoding=encoding) as file:
            client.put(remote_file, file, mode)
