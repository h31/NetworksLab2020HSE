import argparse 
import subprocess as sp 
import os
import re
import http.server
from http.server import BaseHTTPRequestHandler, HTTPServer
from video import *
import threading

DIRECTORY = "all_files"
HOST = 'localhost'
PORT = 80

def run_server():
    if not os.path.exists(DIRECTORY):
        os.mkdir(DIRECTORY)
    web_dir = os.path.join(os.path.dirname(__file__), DIRECTORY)
    os.chdir(web_dir)

    Handler = http.server.SimpleHTTPRequestHandler
    HTTPServer((HOST, PORT), Handler).serve_forever()
    
def parse_start_args():
    parser = argparse.ArgumentParser(description='Start a simple HLS server')
    parser.add_argument('--host', type=str, default=HOST, dest='host')
    parser.add_argument('--port', type=int, default=PORT, dest='port')
    args = parser.parse_args()

    HOST = args.host
    PORT = args.port

def create_actions_parser():
    parser = argparse.ArgumentParser(description='Simple HLS server')
    parser.add_argument('filename', action='store', type=str, help="A file to create HLS catalog of")
    parser.add_argument('bitrates', type=int, nargs='*', help="Bitrates for the file playlists")

    return parser


def mainLoop():
    parse_start_args()
    pwd = os.path.abspath(os.path.dirname(__file__))
    parser = create_actions_parser()

    thread = threading.Thread(target=run_server)
    thread.start()
    
    while True:
        command = parser.parse_args(input().split())
        filename = command.filename
        if filename[0] != '/':
            filename = os.path.join(pwd, filename)
        rates = [360, 1000]
        if len(command.bitrates) > 0:
            rates = list(command.bitrates)
        
        cut_file(filename, rates)


if __name__ == "__main__":
    mainLoop()

