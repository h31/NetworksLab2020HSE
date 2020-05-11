package ru.hse.networks.tftp;

import ru.hse.networks.tftp.parcel.Mode;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length >= 1) {
            if (args[0].equals("client") && args.length == 6) {
                final var host = args[1];
                final var operation = args[2];
                final var remoteFile = args[3];
                final var file = new File(args[4]);
                final var mode = Mode.valueOf(args[5]);
                try {
                    var client = new Client(host);
                    if (operation.equals("read")) {
                        var result = client.load(remoteFile, file, mode);
                        System.out.println(result);
                    } else if (operation.equals("write")) {
                        var result = client.send(remoteFile, file, mode);
                        System.out.println(result);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Usage: client [HOST] [OPERATION: read|write] [REMOTE FILE] [LOCAL FILE] [MODE: NETASCII|OCTET]");
    }
}
