import fields.OpCode;
import org.apache.commons.cli.CommandLine;
import requests.DataPacket;
import requests.ErrorPacket;
import requests.ReadPacket;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;

public class ClientSession implements Runnable {

    private static final int DEFAULT_TIMEOUT = 3000;
    private static final int DEFAULT_ATTEMPTS = 5;
    private static final int TFTP_DEFAULT_PORT = 69;
    private final static int PACKET_SIZE = 516;

    private String hostname;
    private int port;
    private String operation;

    // About self
    private boolean sessionActive;
    private short sessionType;

    // Options
    private int timeout = DEFAULT_TIMEOUT;
    private int attempts = DEFAULT_ATTEMPTS;

    // Socket and packets
    private DatagramSocket socket;
    private int ownPort;
    private InPacket inPacket = null;
    private OutPacket outPacket = null;

    // Delivery and addressing
    private InetAddress address;
    private int serverPort = TFTP_DEFAULT_PORT;
    private String addrTIDPair = null;

    // File stuff
    private String srcFile;
    private String dstFile;
    private String fileMode;
    private FileReader fileReader;
    private FileWriter fileWriter;
    private byte[] fileBuffer;
    private int fileBufferSize;

    // Operation variables
    private int packetSendRetryCount;
    private int timeoutCount;

    private CommandLine options;

    public ClientSession(CommandLine options) throws UnknownHostException, SocketException {
        timeout = options.hasOption("TIMEOUT") ? Integer.parseInt(options.getOptionValue("TIMEOUT")) : DEFAULT_TIMEOUT;
        attempts = options.hasOption("ATTEMPTS") ? Integer.parseInt(options.getOptionValue("ATTEMPTS")) : DEFAULT_ATTEMPTS;
        port = Integer.parseInt(options.getOptionValue("PORT"));
        hostname = options.getOptionValue("HOSTNAME");
        fileMode = options.getOptionValue("MODE");

        switch (options.getOptionValues("OPERATION")[0]) {
            case "READ":
                sessionType = OpCode.RRQ;
                break;
            case "WRITE":
                sessionType = OpCode.WRQ;
                break;
            default:
                throw new IllegalArgumentException();
        }
        srcFile = options.getOptionValues("OPERATION")[1];
        dstFile = options.getOptionValues("OPERATION")[2];

        socket = new DatagramSocket(port);
        socket.setSoTimeout(timeout);
        outPacket = new OutPacket(socket, InetAddress.getByName(hostname), serverPort);
        inPacket = new InPacket(socket);
    }

    private void sendAcknowledgment(DataPacket response) {

    }

    private void handleData(DataPacket response) {

    }

    private void handleError(ErrorPacket response) {

    }

    private void get() throws Exception {
        outPacket.send(new ReadPacket(srcFile, fileMode));
        do {
            var response = inPacket.receive();
            switch (response.getOpCode()) {
                case OpCode.DATA:
                    handleData((DataPacket) response);
                    break;
                case OpCode.ERROR:
                    handleError((ErrorPacket) response);
                    break;
                default:
                    closeSession();
                    throw new Exception();
            }
        } while (sessionActive);
    }

    private void closeSession() {
    }

    private ByteArrayOutputStream receiveFile() throws IOException {
        ByteArrayOutputStream byteOutOS = new ByteArrayOutputStream();
        int block = 1;
        do {
            System.out.println("TFTP Packet count: " + block);
            block++;
            var bufferByteArray = new byte[PACKET_SIZE];
            var inBoundDatagramPacket = new DatagramPacket(bufferByteArray,
                    bufferByteArray.length, address, port);

            //STEP 2.1: receive packet from TFTP server
            socket.receive(inBoundDatagramPacket);

            // Getting the first 4 characters from the TFTP packet
            byte[] opCode = { bufferByteArray[0], bufferByteArray[1] };

            if (opCode[1] == OP_ERROR) {
                reportError();
            } else if (opCode[1] == OP_DATAPACKET) {
                // Check for the TFTP packets block number
                byte[] blockNumber = { bufferByteArray[2], bufferByteArray[3] };

                DataOutputStream dos = new DataOutputStream(byteOutOS);
                dos.write(inBoundDatagramPacket.getData(), 4,
                        inBoundDatagramPacket.getLength() - 4);

                //STEP 2.2: send ACK to TFTP server for received packet
                sendAcknowledgment(blockNumber);
            }

        } while (!isLastPacket(inBoundDatagramPacket));
        return byteOutOS;
    }

    private void put() {

    }

    @Override
    public void run() {

    }
}
