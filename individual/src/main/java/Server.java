import org.apache.commons.cli.CommandLine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final int port;

    public Server(CommandLine options) {
        port = Integer.parseInt(options.getOptionValue('p'));
    }

    public void start() {
        try (var socket = new TftpAcceptSocket(port)) {
            while (true) {
                pool.execute(new ServerSession(socket.accept()));
                Thread.sleep(3000);
            }
        } catch (Exception e) {
            pool.shutdownNow();
            System.err.println("Failed to start server. " + e.getMessage());
            System.exit(-1);
        }
    }
}
