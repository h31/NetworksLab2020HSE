from tftp import *
import argparse

def get_file(socket, filename):
    file = open(filename, "wb")

    content = get(socket)
    file.write(content)

    file.close()

def put_file(socket, filename):
    file = open(filename, "rb")

    put(socket, file)

    file.close()


def get_filename(data):
    filename_end = 2
    while data[filename_end] != 0:
            filename_end += 1
    return data[2:filename_end]
            

def main():
    parser = argparse.ArgumentParser(description='Simple TFTP python server.')
    parser.add_argument('--port', action='store', dest='port', type=int,
            default=5005, help='Server port')
    args = parser.parse_args()

    server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_socket.bind(("", args.port))

    while True:
        data, port = server_socket.recvfrom(BUFFER_SIZE)
        opcode = get_opcode(data)
        if opcode == RRQ:
            filename = get_filename(data)
            put_file(port, filename)
        elif opcode == WRQ:
            filename = get_filename(data)
            get_file(port, filename)
        elif opcode == ERROR:
            print_error(data)

if __name__ == "__main__":
    main()