from tftp import *
import argparse

class Client():
    def __init__(self, socket):
        self.socket = socket

    def put_request(self, filename):
        file = open(filename, "rb")

        self.socket.send_wrq(filename)
        put(self.socket, filename)

        file.close()


    def get_request(self, filename):
        file = open(filename, "wb")

        self.socket.send_rrq(filename)
        content = get(self.socket)
        file.write(content)

        file.close()


def create_socket():
    parser = argparse.ArgumentParser(description='Simple TFTP python client.')
    parser.add_argument('--host', action='store', dest='host', 
            default='127.0.0.1', help='Server hostname')
    parser.add_argument('--port', action='store', dest='port', type=int,
            default=5005, help='Server port')
    args = parser.parse_args()
    return SocketWrapper(args.host, args.port)


def main():
    socket = create_socket()
    client = Client(socket)
    CAT, GET, PUT = "cat", "get", "put"
    while True:
        args = input().split()
        if len(args) != 2:
            print("Usage: <action>(cat|get|put) <filename>")
            continue
        action, filename = args
        
        if action == CAT:
            file = open(filename, "r")
            print(file.read())
            file.close()
        elif action == GET:
            client.get_request(filename)
        elif action == PUT:
            client.put_request(filename)
        else:
            print("Invalid action")


if __name__ == "__main__":
    main()