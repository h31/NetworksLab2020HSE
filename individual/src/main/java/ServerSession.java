import requests.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;

import static parameters.ErrorCode.ILLEGAL_OPERATION;
import static parameters.OpCode.*;

public class ServerSession extends Session implements Runnable {

    public DatagramPacket request;

    public ServerSession(DatagramPacket initRequest) throws SocketException, UnknownHostException {
        super(initRequest.getAddress(), initRequest.getPort());
        request = initRequest;
    }

    @Override
    public void run() {
        try {
            var packet = PacketFactory.create(request);
            switch (packet.getOpCode()) {
                case RRQ:
                    handleRRQ();
                    break;
                case WRQ:
                    handleWRQ();
                    break;
            }
        } catch (TftpException | IOException e) {
            var errorPacket = new ErrorPacket(ILLEGAL_OPERATION, e.getMessage());
            try {
                sendError(errorPacket);
            } catch (IOException ioException) {
                System.out.println(String.format("Failed to send ErrorPacket: %s", errorPacket.getMessage()));
            }
        }
        close();
    }

    @Override
    protected void handleRRQ() throws IOException, TftpException {
        var readPacket = (ReadPacket) PacketFactory.create(request);
        blockNumber = 1;

        try (var fileReader = new FileReader(readPacket.getMode(), readPacket.getFilename())) {
            loop:
            while (true) {
                request = sendData(fileReader.read());
                var response = receive(request);

                if (validate(response)) {
                    continue;
                }
                var packet = PacketFactory.create(response);
                switch (packet.getOpCode()) {
                    case ACK:
                        var ackPacket = (AckPacket) packet;
                        if (ackPacket.getBlockNumber() == blockNumber) {
                            ++blockNumber;
                            if (isLastPacket) {
                                return;
                            }
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
        var writePacket = (WritePacket) PacketFactory.create(request);
        sendAck();
        ++blockNumber;

        try (var fileWriter = new FileWriter(writePacket.getMode(), writePacket.getFilename())) {
            loop:
            while (!isLastPacket) {
                var response = receive(request);

                if (!validate(response)) {
                    handleUnknown(response);
                    continue;
                }

                var packet = PacketFactory.create(response);

                switch (packet.getOpCode()) {
                    case DATA:
                        var dataPacket = (DataPacket) packet;
                        if (dataPacket.getBlockNumber() == blockNumber) {
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
}
