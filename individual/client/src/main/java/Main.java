import org.apache.commons.cli.*;

public class Main {
    private static Options getOptions() {
        // [--port <num>]
        // [--timeout <msec>]
        // [--attempts <num>]
        // [--mode <mode>]
        // <host> {get|put} <source> <destination>

        Options options = new Options();
        Option port = new Option("p", "port", true, "client port");
        port.setRequired(true);
        port.setType(Integer.class);
        port.setArgName("PORT");
        options.addOption(port);

        Option timeout = new Option("t", "timeout", true, "request timeout in ms. Default: 3000ms");
        timeout.setRequired(false);
        timeout.setType(Integer.class);
        timeout.setArgName("TIMEOUT");
        options.addOption(timeout);

        Option attempts = new Option("a", "attempts", true, "number of retries. Default: 5");
        attempts.setRequired(false);
        attempts.setType(Integer.class);
        attempts.setArgName("ATTEMPTS");
        options.addOption(attempts);

        Option mode = new Option("m", "mode", true, "[NETASCII | OCTET]. Default: OCTET");
        mode.setRequired(false);
        mode.setType(String.class);
        mode.setArgName("MODE");
        options.addOption(mode);

        Option host = new Option("h", "hostname", true, "Server hostname");
        host.setRequired(true);
        host.setArgName("HOSTNAME");
        options.addOption(host);

        Option operation = new Option("o", "operation", true, "[GET | PUT]");
        operation.setArgs(3);
        operation.setRequired(true);
        operation.setArgName("OPERATION");
        options.addOption(operation);

        return options;
    }

    public static void main(String[] args) {

        var options = getOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            var cmd = parser.parse(options, args);
            new ClientSession(cmd).begin();
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
}
