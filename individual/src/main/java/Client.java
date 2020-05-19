import org.apache.commons.cli.CommandLine;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {

    private final CommandLine options;

    public Client(CommandLine options) {
        this.options = options;
    }

    public void start() {
        try (var session = new ClientSession(options)) {
            session.run();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
