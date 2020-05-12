package ru.hse.networks.tftp;

import ru.hse.networks.tftp.parcel.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;

public class Server {
    private final DatagramSocket socket;
    private final TFTP tftp = new TFTP();

    public Server(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }

    public void start() {
        var buffer = new byte[TFTP.MAX_PACKET_SIZE];
        //noinspection InfiniteLoopStatement
        while (true) {
            var packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);

                final var parcel = ParcelFactory.fromBytes(ByteBuffer.wrap(packet.getData()));
                if (parcel instanceof ReadParcel) {
                    new Thread(new ClientReadHandler((ReadParcel) parcel, packet.getPort(), packet.getAddress())).start();
                } else if (parcel instanceof WriteParcel) {
                    new Thread(new ClientWriteHandler((WriteParcel) parcel, packet.getPort(), packet.getAddress())).start();
                } else {
                    var error = new ErrorParcel((short) 4);
                    tftp.send(error, socket, packet.getAddress(), packet.getPort());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private abstract static class ClientHandler implements Runnable {
        protected final File file;
        protected final int clientTid;
        protected final InetAddress address;
        protected final TFTP tftp = new TFTP();
        protected final DatagramSocket socket = new DatagramSocket(TFTP.createTid());

        ClientHandler(ReadWriteParcel parcel, int port, InetAddress address) throws SocketException {
            file = new File(parcel.getFileName());
            clientTid = port;
            this.address = address;
        }

        protected void sendData(short blockNumber, byte[] data) {
            tftp.send(new DataParcel(blockNumber, data), socket, address, clientTid);
        }

        protected void sendError(String message) {
            var error = new ErrorParcel((short) 0, message);
            tftp.send(error, socket, address, clientTid);
        }

        protected void sendError(short code) {
            var error = new ErrorParcel(code);
            tftp.send(error, socket, address, clientTid);
        }
    }

    final private static class ClientReadHandler extends ClientHandler {
        ClientReadHandler(ReadParcel parcel, int port, InetAddress address) throws SocketException {
            super(parcel, port, address);
        }

        @Override
        public void run() {
            byte[] data;
            short currentBlock = 1;
            short finalBlock = -1;
            retry:
            for (int i = 0; i < TFTP.MAX_RETRY_NUMBER; i++) {
                do {
                    try {
                        data = Mode.OCTET.read(file, TFTP.MAX_DATA_SIZE * (currentBlock - 1));
                    } catch (FileNotFoundException e) {
                        sendError((short) 1);
                        break retry;
                    } catch (AccessDeniedException e) {
                        sendError((short) 2);
                        break retry;
                    } catch (IOException e) {
                        sendError(e.getLocalizedMessage());
                        break retry;
                    }
                    if (data.length < TFTP.MAX_DATA_SIZE) {
                        finalBlock = (short) (currentBlock + 1);
                    }
                    System.out.println("Send packet " + currentBlock);
                    sendData(currentBlock, data);
                    DatagramPacket packet;
                    try {
                        packet = tftp.receive(socket);
                    } catch (SocketTimeoutException e) {
                        continue retry;
                    } catch (IOException e) {
                        sendError(e.getLocalizedMessage());
                        break retry;
                    }
                    final var parcel = ParcelFactory.fromBytes(ByteBuffer.wrap(packet.getData()));
                    if (parcel instanceof AcknowledgementParcel) {
                        var ackParcel = (AcknowledgementParcel) parcel;
                        currentBlock = (short) (ackParcel.getBlockNumber() + 1);
                        if (currentBlock == finalBlock) {
                            break retry;
                        }
                    } else if (parcel instanceof ErrorParcel) {
                        sendError(((ErrorParcel) parcel).getErrorMessage());
                        break retry;
                    } else {
                        sendError((short) 4);
                        break retry;
                    }
                } while (data.length == TFTP.MAX_DATA_SIZE);
            }
            socket.close();
        }
    }

    final private static class ClientWriteHandler extends ClientHandler {
        ClientWriteHandler(WriteParcel parcel, int port, InetAddress address) throws SocketException {
            super(parcel, port, address);
        }

        private void exit(ErrorParcel error) {
            if (error != null) {
                tftp.send(error, socket, address, clientTid);
            }
            socket.close();
        }

        private void sendAcknowledgement(short blockNumber) {
            tftp.sendAcknowledgement(blockNumber, socket, address, clientTid);
        }

        @Override
        public void run() {
            ErrorParcel errorParcel = null;
            try {
                if (file.exists()) {
                    errorParcel = new ErrorParcel((short) 6);
                } else if (!file.createNewFile()) {
                    errorParcel = new ErrorParcel((short) 0, "Cannot create file.");
                }
            } catch (FileNotFoundException e) {
                errorParcel = new ErrorParcel((short) 1);
            } catch (AccessDeniedException e) {
                errorParcel = new ErrorParcel((short) 2);
            } catch (IOException e) {
                errorParcel = new ErrorParcel((short) 0, e.getLocalizedMessage());
            }
            if (errorParcel != null) {
                exit(errorParcel);
                return;
            }

            byte[] data;
            short currentBlockNumber = 0;
            sendAcknowledgement(currentBlockNumber);
            retry:
            for (int i = 0; i < TFTP.MAX_RETRY_NUMBER; i++) {
                do {
                    DatagramPacket packet;
                    try {
                        packet = tftp.receive(socket);
                    } catch (SocketTimeoutException e) {
                        sendAcknowledgement(currentBlockNumber);
                        continue retry;
                    } catch (IOException e) {
                        exit(new ErrorParcel((short) 0, e.getLocalizedMessage()));
                        return;
                    }
                    final var parcel = ParcelFactory.fromBytes(ByteBuffer.wrap(Arrays.copyOf(packet.getData(), packet.getLength())));
                    if (parcel instanceof DataParcel) {
                        final var dataParcel = (DataParcel) parcel;
                        currentBlockNumber = dataParcel.getBlockNumber();
                        sendAcknowledgement(currentBlockNumber);
                        data = dataParcel.getBytes();
                        try {
                            Mode.OCTET.write(file, data);
                            System.out.println("Load packet " + currentBlockNumber);
                        } catch (IOException e) {
                            exit(new ErrorParcel((short) 0, e.getLocalizedMessage()));
                            return;
                        }
                    } else {
                        exit(new ErrorParcel((short) 4));
                        return;
                    }
                } while (data.length == TFTP.MAX_DATA_SIZE);
            }
            exit(null);
        }
    }
}
