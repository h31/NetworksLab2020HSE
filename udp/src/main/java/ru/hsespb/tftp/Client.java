package ru.hsespb.tftp;

import java.io.*;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private static final int SERVER_INIT_PORT = 69;
    private static final int DATA_BUFFER_SIZE = 512;
    private static final String EXIT_COMMAND = "exit";

    private NetworkService networkService;

    public void start() {
        var scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.print(">> ");
                var command = scanner.nextLine();
                command = command.trim();
                if (command.equals(EXIT_COMMAND)) {
                    break;
                }
                var request = new ClientRequest(command);
                if (request.getRequestType() == ClientRequestType.READ) {
                    processReadRequest(request);
                } else if (request.getRequestType() == ClientRequestType.WRITE) {
                    processWriteRequest(request);
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void processReadRequest(ClientRequest request) throws IOException {
        networkService = new NetworkService(request.getRemoteAddress(), SERVER_INIT_PORT);
        var file = new File(request.getLocalFileName());
        var lastPacket = networkService.sendRRQ(request.getRemoteFileName());
        var currentBlock = 1;

        var packet = receiveEstablishingPacket(lastPacket);

        try (var writer = new BufferedOutputStream(new FileOutputStream(file))) {
            while (true) {
                if (networkService.isInvalidPacketSource(packet)) {
                    continue;
                }

                var data = networkService.getPacketData(packet);
                var operationType = OperationType.get(data[0], data[1]);

                if (operationType == OperationType.DATA) {
                    var dataMessage = new DataMessage(data);
                    var sentPacket = processReadData(dataMessage, currentBlock, writer);
                    if (sentPacket != null) {
                        lastPacket = sentPacket;
                        currentBlock++;
                    }
                    if (dataMessage.isLast()) {
                        break;
                    }
                } else if (operationType == OperationType.ERR) {
                    processErrorOnRead(file, writer, data);
                    return;
                } else {
                    throw new TFTPException(ErrorType.ILLEGAL_OPERATION,
                            "An unexpected operation found during WRQ: " + operationType.name());
                }

                packet = networkService.receive(lastPacket);
            }
        }
    }

    private DatagramPacket receiveEstablishingPacket(DatagramPacket lastPacket) throws IOException {
        DatagramPacket packet;
        packet = networkService.receive(lastPacket);
        networkService.setRemotePort(packet.getPort());
        return packet;
    }

    private void processWriteRequest(ClientRequest request) throws IOException {
        networkService = new NetworkService(request.getRemoteAddress(), SERVER_INIT_PORT);
        var file = new File(request.getLocalFileName());
        var lastPacket = networkService.sendWRQ(request.getRemoteFileName());
        var currentBlock = 0;

        var packet = receiveEstablishingPacket(lastPacket);
        boolean lastMessageSent = false;

        try (var reader = new BufferedInputStream(new FileInputStream(file))) {
            while (true) {
                if (networkService.isInvalidPacketSource(packet)) {
                    continue;
                }

                var data = networkService.getPacketData(packet);
                var operationType = OperationType.get(data[0], data[1]);

                if (operationType == OperationType.ACK) {
                    var acknowledgementMessage = new AcknowledgementMessage(data);
                    if (acknowledgementMessage.getBlockNumber() == currentBlock) {
                        currentBlock++;
                    }
                    if (lastMessageSent) {
                        break;
                    }
                } else if (operationType == OperationType.ERR) {
                    processErrorOnWrite(reader, data);
                    return;
                } else {
                    throw new TFTPException(ErrorType.ILLEGAL_OPERATION,
                            "An unexpected operation found during RRQ: " + operationType.name());
                }

                var textBuffer = new byte[DATA_BUFFER_SIZE];
                var bytesRead = reader.read(textBuffer);
                var dataMessage = new DataMessage(Arrays.copyOf(textBuffer, bytesRead), currentBlock);
                lastPacket = networkService.sendData(dataMessage);

                if (dataMessage.isLast()) {
                    lastMessageSent = true;
                }

                packet = networkService.receive(lastPacket);
            }
        }
    }

    private void processErrorOnRead(File file, BufferedOutputStream writer, byte[] data)
            throws IOException {
        var errorMessage = new ErrorMessage(data);
        System.err.println("Received an error " + errorMessage.getErrorType().name() +
                " during RRQ with message: " + errorMessage.getMessage());
        writer.close();
        file.delete();
    }

    private void processErrorOnWrite(BufferedInputStream reader, byte[] data) throws IOException {
        var errorMessage = new ErrorMessage(data);
        System.err.println("Received an error " + errorMessage.getErrorType().name() +
                " during WRQ with message: " + errorMessage.getMessage());
        reader.close();
    }

    private DatagramPacket processReadData(DataMessage dataMessage, int currentBlock,
                                           BufferedOutputStream writer) throws IOException {
        var messageBlock = dataMessage.getBlockNumber();
        if (messageBlock == currentBlock) {
            writer.write(dataMessage.getData());
            return networkService.sendAcknowledgement(currentBlock);
        }
        return null;
    }
}
