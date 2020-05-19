import org.apache.commons.cli.CommandLine;
import requests.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static parameters.OpCode.*;

public class ClientSession extends Session {

    private final short sessionType;
    private final String srcFile;
    private final String dstFile;
    private final String mode;

    public ClientSession(CommandLine options) throws UnknownHostException, SocketException {
        super(InetAddress.getByName(options.getOptionValue("hostname")), Integer.parseInt(options.getOptionValue("port")));
        switch (options.getOptionValues("operation")[0]) {
            case "GET":
                sessionType = RRQ;
                break;
            case "PUT":
                sessionType = WRQ;
                break;
            default:
                throw new IllegalArgumentException();
        }
        mode = options.getOptionValue("mode");
        srcFile = options.getOptionValues("operation")[1];
        dstFile = options.getOptionValues("operation")[2];
    }

    @Override
    protected void handleRRQ() throws IOException, TftpException {
        var request = sendRRQ(new ReadPacket(srcFile, mode));
        blockNumber = 1;

        try (var fileWriter = new FileWriter(mode, dstFile)) {
            loop:
            while (!isLastPacket) {
                var response = receive(request);

                if (established && !validate(response)) {
                    handleUnknown(response);
                    continue;
                }

                var packet = PacketFactory.create(response);

                switch (packet.getOpCode()) {
                    case DATA:
                        var dataPacket = (DataPacket) packet;
                        if (dataPacket.getBlockNumber() == blockNumber) {
                            if (blockNumber == 1) {
                                establishConnection(response);
                            }
                            handleData(dataPacket, fileWriter);
                            request = sendAck();
                            ++blockNumber;
                            continue loop;
                        }
                        break;
                    case ERROR:
                        handleError((ErrorPacket) packet);
                    default:
                        handleUnexpected(packet);
                }
            }
        }
    }

    @Override
    protected void handleWRQ() throws IOException, TftpException {
        var request = sendWRQ(new WritePacket(dstFile, mode));

        try (var fileReader = new FileReader(mode, srcFile)) {
            loop:
            while (true) {
                var response = receive(request);

                if (established && !validate(response)) {
                    handleUnknown(response);
                    continue;
                }

                var packet = PacketFactory.create(response);

                switch (packet.getOpCode()) {
                    case ACK:
                        var ackPacket = (AckPacket) packet;
                        if (ackPacket.getBlockNumber() == blockNumber) {
                            if (blockNumber == 0) {
                                establishConnection(response);
                            }
                            ++blockNumber;
                            if (isLastPacket) {
                                return;
                            }
                            request = sendData(fileReader.read());
                            continue loop;
                        }
                        break;
                    case ERROR:
                        handleError((ErrorPacket) packet);
                    default:
                        handleUnexpected(packet);
                }
            }
        }
    }

    public void run() {
        try {
            switch (sessionType) {
                case RRQ:
                    handleRRQ();
                    break;
                case WRQ:
                    handleWRQ();
                    break;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
