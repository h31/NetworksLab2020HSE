import org.apache.commons.cli.*;

public class Main {

    private static Options getClientOptions() {
        Options options = new Options();

        Option client = new Option("c", "client", false, "Run client");
        client.setRequired(true);
        options.addOption(client);

        Option port = new Option("p", "port", true, "client port");
        port.setRequired(true);
        port.setType(Integer.class);
        options.addOption(port);

        Option host = new Option("h", "host", true, "Server hostname");
        host.setRequired(true);
        options.addOption(host);

        Option operation = new Option("o", "operation", true, "operation type {GET | PUT}");
        operation.setArgs(3);
        operation.setRequired(true);
        options.addOption(operation);

        Option mode = new Option("m", "mode", true, "file mode {NETASCII | OCTET}");
        mode.setRequired(true);
        mode.setType(String.class);
        options.addOption(mode);
        return options;
    }

    private static Options getServerOptions() {
        Options options = new Options();

        Option server = new Option("s", "sever", false, "Run server");
        server.setRequired(true);
        options.addOption(server);

        Option port = new Option("p", "port", true, "server port");
        port.setRequired(true);
        port.setType(Integer.class);
        options.addOption(port);
        return options;
    }

    private static Options getRunOptions() {
        Options options = new Options();

        Option client = new Option("c", "client", false, "Run client");
        client.setRequired(false);
        options.addOption(client);

        Option server = new Option("s", "sever", false, "Run server");
        server.setRequired(false);
        options.addOption(server);


        return options;
    }

    private static void printHelp() {

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("./gradlew run", getClientOptions());
        formatter.printHelp("./gradlew run", getServerOptions());
    }

    public static void main(String[] args) {

        Options options;
        CommandLineParser parser = new DefaultParser();
        CommandLine cmdOptions;
        try {
            cmdOptions = parser.parse(getRunOptions(), new String[]{args[0]});
            if (cmdOptions.hasOption("c")) {
                options = getClientOptions();
                new Client(parser.parse(options, args)).start();
            } else if (cmdOptions.hasOption("s")) {
                options = getServerOptions();
                new Server(parser.parse(options, args)).start();
            } else {
                printHelp();
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            printHelp();
            System.exit(1);
        }
    }
}
