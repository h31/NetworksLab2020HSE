package ru.hse.networks.tftp;

import ru.hse.networks.tftp.parcel.Mode;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length >= 1) {
            if (args[0].equals("client") && args.length == 7) {
                final var host = args[1];
                final var port = Integer.parseInt(args[2]);
                final var operation = args[3];
                final var remoteFile = args[4];
                final var file = new File(args[5]);
                final var mode = Mode.valueOf(args[6]);
                try {
                    var client = new Client(host, port);
                    if (operation.equals("read")) {
                        var result = client.load(remoteFile, file, mode);
                        System.out.println(result);
                    } else if (operation.equals("write")) {
                        var result = client.send(remoteFile, file, mode);
                        System.out.println(result);
                    }
                    return;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            } else if (args[0].equals("server") && args.length == 2) {
                final var port = Integer.parseInt(args[1]);
                try {
                    new Server(port).start();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Usage:\nclient [HOST] [PORT] [OPERATION: read|write] [REMOTE FILE] [LOCAL FILE] [MODE: NETASCII|OCTET]\n"
                + "server [PORT]");
    }
}
