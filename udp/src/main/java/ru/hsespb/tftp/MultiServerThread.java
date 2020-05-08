package ru.hsespb.tftp;

import java.io.*;
import java.net.*;
import java.util.Arrays;

public class MultiServerThread implements Runnable {
    private static final int DATA_BUFFER_SIZE = 512;

    private final byte[] initData;
    private final NetworkService networkService;

    public MultiServerThread(DatagramPacket initPacket) throws SocketException {
        initData = initPacket.getData();
        networkService = new NetworkService(initPacket.getAddress(), initPacket.getPort());
    }

    public void run() {
        try {
            processInitData();
        } catch (TFTPException e) {
            try {
                networkService.sendError(e.getErrorType(), e.getMessage());
            } catch (IOException ioException) {
                System.err.println("Unable to send an error message.");
                ioException.printStackTrace();
            }
        } catch (Exception e) {
            try {
                networkService.sendError(ErrorType.UNKNOWN, e.getMessage());
            } catch (IOException ioException) {
                System.err.println("Unable to send an error message.");
                ioException.printStackTrace();
            }
        }
    }

    private void processInitData() throws IOException {
        var operationType = OperationType.get(initData[0], initData[1]);
        if (operationType == OperationType.RRQ) {
            processRRQ();
        } else if (operationType == OperationType.WRQ) {
            processWRQ();
        } else {
            throw new TFTPException(ErrorType.ILLEGAL_OPERATION,
                    "Init operation must be either RRQ or WRQ. Found: " + operationType.name());
        }
    }

    private void processRRQ() throws IOException {
        var file = getFileFromReadRequest(initData);
        int currentBlock = 1;

        try (var reader = new BufferedInputStream(new FileInputStream(file))) {
            while (true) {
                var textBuffer = new byte[DATA_BUFFER_SIZE];
                var bytesRead = reader.read(textBuffer);
                var dataMessage = new DataMessage(Arrays.copyOf(textBuffer, bytesRead), currentBlock);
                var lastPacket = networkService.sendData(dataMessage);

                var packet = networkService.receive(lastPacket);
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
                    if (dataMessage.isLast()) {
                        break;
                    }
                } else if (operationType == OperationType.ERR) {
                    processErrorOnRead(reader, data);
                    return;
                } else {
                    throw new TFTPException(ErrorType.ILLEGAL_OPERATION,
                            "An unexpected operation found during RRQ: " + operationType.name());
                }
            }
        }
    }

    private void processWRQ() throws IOException {
        var file = getFileFromWriteRequest(initData);
        int currentBlock = 0;
        var lastPacket = networkService.sendAcknowledgement(currentBlock);
        currentBlock++;

        try (var writer = new BufferedOutputStream(new FileOutputStream(file))) {
            while (true) {
                var packet = networkService.receive(lastPacket);
                if (networkService.isInvalidPacketSource(packet)) {
                    continue;
                }

                var data = networkService.getPacketData(packet);
                var operationType = OperationType.get(data[0], data[1]);

                if (operationType == OperationType.DATA) {
                    var dataMessage = new DataMessage(data);
                    var sentPacket = processWriteData(dataMessage, currentBlock, writer);
                    if (sentPacket != null) {
                        lastPacket = sentPacket;
                        currentBlock++;
                    }
                    if (dataMessage.isLast()) {
                        break;
                    }
                } else if (operationType == OperationType.ERR) {
                    processErrorOnWrite(file, writer, data);
                    return;
                } else {
                    throw new TFTPException(ErrorType.ILLEGAL_OPERATION,
                            "An unexpected operation found during WRQ: " + operationType.name());
                }
            }
        }
    }

    private File getFileFromReadRequest(byte[] initData) {
        var requestMessage = new RequestMessage(initData);
        var fileName = requestMessage.getFileName();
        var file = new File(fileName);

        if (!file.exists()) {
            throw new TFTPException(ErrorType.FILE_NOT_FOUND, "File not found: " + fileName);
        }
        return file;
    }

    private File getFileFromWriteRequest(byte[] initData) {
        var requestMessage = new RequestMessage(initData);
        var fileName = requestMessage.getFileName();
        var file = new File(fileName);

        if (file.exists()) {
            throw new TFTPException(ErrorType.FILE_EXISTS, "File already exists: " + fileName);
        }
        return file;
    }

    private void processErrorOnWrite(File file, BufferedOutputStream writer, byte[] data) throws IOException {
        var errorMessage = new ErrorMessage(data);
        System.err.println("Received an error " + errorMessage.getErrorType().name() +
                " during WRQ with message: " + errorMessage.getMessage());
        writer.close();
        file.delete();
    }

    private void processErrorOnRead(BufferedInputStream reader, byte[] data) throws IOException {
        var errorMessage = new ErrorMessage(data);
        System.err.println("Received an error " + errorMessage.getErrorType().name() +
                " during RRQ with message: " + errorMessage.getMessage());
        reader.close();
    }

    private DatagramPacket processWriteData(DataMessage dataMessage, int currentBlock,
                                            BufferedOutputStream writer) throws IOException {
        var messageBlock = dataMessage.getBlockNumber();
        if (messageBlock == currentBlock) {
            writer.write(dataMessage.getData());
            return networkService.sendAcknowledgement(currentBlock);
        }
        return null;
    }
}
