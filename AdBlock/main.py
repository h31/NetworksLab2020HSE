import re
from http.server import BaseHTTPRequestHandler, HTTPServer
import logging

import requests


class ProxyServer(BaseHTTPRequestHandler):
    @staticmethod
    def isRestricted(url):
        with open('block.txt', 'r') as f:
            lines = f.read().splitlines()
        return any(re.match("^%s$" % s, url) for s in lines)

    def do_GET(self):
        url = self.path
        if self.isRestricted(url):
            self.send_response(403, "Proxy restriction")
            return
        self.do_REQUEST()

    def do_REQUEST(self):
        try:
            url = self.path if "http://" in self.path else "http://" + self.path
            length = self.headers['Content-Length']
            if length is None:
                response = requests.request(self.command, url, headers=self.headers)
            else:
                response = requests.request(self.command, url, headers=self.headers,
                                            data=self.rfile.read(int(length)))
            self.send_response(response.status_code, response.reason)
            self.wfile.write(response.content)
        except BaseException as e:
            print(e)

    def do_CONNECT(self):
        self.do_REQUEST()

    def do_POST(self):
        self.do_REQUEST()

    def do_PUT(self):
        self.do_REQUEST()

    def do_HEAD(self):
        self.do_REQUEST()

    def do_DELETE(self):
        self.do_REQUEST()

    def do_OPTIONS(self):
        self.do_REQUEST()

    def do_PATCH(self):
        self.do_REQUEST()


def run(port, server_class=HTTPServer, handler_class=ProxyServer):
    server_address = ('', port)
    httpd = server_class(server_address, handler_class)
    print("Proxy runs on port %d" % port)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()


if __name__ == '__main__':
    from sys import argv

    run(port=int(argv[1]))
