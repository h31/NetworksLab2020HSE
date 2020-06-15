import sys
from pathlib import Path

sys.path.append('../')
from tftp import *
from threading import Thread


class ReadHandler(Thread):

    def __init__(self, read_request, addr):
        super().__init__()
        self.addr = addr
        self.read_request = read_request

    def run(self):
        local_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

        path_to_file = Path(self.read_request.filename)
        if not path_to_file.exists():
            local_socket.sendto(Error(1, "File not found.").serialize(), self.addr)

        if self.read_request.mode == Request.OCTET_MODE:
            bin_opt = "b"
            encoding = None
        else:
            bin_opt = ""
            encoding = "ascii"

        with open(self.read_request.filename, "r" + bin_opt, encoding=encoding) as file:
            cur_block_num = 1
            while True:
                data = file.read(512)
                if self.read_request.mode == Request.NETASCII_MODE:
                    data = data.encode("ascii")

                local_socket.sendto(Data(cur_block_num, data).serialize(), self.addr)

                raw_ans = local_socket.recv(tftpServer.MAX_SIZE)
                ans = deserialize_msg(raw_ans)
                if type(ans) is not Acknowledgment or ans.block_num != cur_block_num:
                    raise Exception("unexpected answer at block #%s" % cur_block_num, ans)

                if len(data) < 512:
                    return
                cur_block_num += 1


class WriteHandler(Thread):

    def __init__(self, write_request, addr):
        super().__init__()
        self.addr = addr
        self.write_request = write_request

    def run(self):
        print("start writing")
        local_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

        if self.write_request.mode == Request.OCTET_MODE:
            bin_opt = "b"
            encoding = None
        else:
            bin_opt = ""
            encoding = "ascii"

        with open(self.write_request.filename, "w" + bin_opt, encoding=encoding) as file:
            cur_block_num = 0
            local_socket.sendto(Acknowledgment(cur_block_num).serialize(), self.addr)
            while True:
                cur_block_num += 1

                raw_ans = local_socket.recv(tftpServer.MAX_SIZE)
                print(raw_ans)
                ans = deserialize_msg(raw_ans)
                # print(ans.block_num, cur_block_num)
                if type(ans) is not Data or ans.block_num != cur_block_num:
                    raise Exception("unexpected answer", ans)

                local_socket.sendto(Acknowledgment(cur_block_num).serialize(), self.addr)

                if self.write_request.mode == Request.OCTET_MODE:
                    file.write(ans.data)
                else:
                    decoded_str = ans.data.decode("ascii")
                    decoded_str = decoded_str.replace("\r\n", "\n")
                    file.write(decoded_str)

                if len(ans.data) < 512:
                    return


class tftpServer:
    MAX_SIZE = 4098
    DEFAULT_PORT = 69

    def __init__(self, timeout):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.settimeout(timeout)
        self.socket.bind(("localhost", self.DEFAULT_PORT))

    def run(self):
        while True:
            print("wait new request")
            raw_request, addr = self.socket.recvfrom(self.MAX_SIZE)
            request = deserialize_msg(raw_request)
            if not issubclass(type(request), Request):
                print("Illegal TFTP operation from %s" % addr)
                self.socket.sendto(Error(4, "Illegal TFTP operation").serialize(), addr)
                continue
            if issubclass(type(request), ReadRequest):
                print("start read")
                ReadHandler(request, addr).start()
            else:
                print("start write")
                WriteHandler(request, addr).start()


if __name__ == '__main__':
    tftpServer(600).run()
