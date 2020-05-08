package ru.hsespb.tftp;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1 || (!args[0].equals("--client") && !args[0].equals("--server"))) {
            System.err.println("Usage: <--client|--server>");
            System.exit(-1);
        }

        if (args[0].equals("--client")) {
            new Client().start();
        } else {
            new Server().start();
        }
    }
}
