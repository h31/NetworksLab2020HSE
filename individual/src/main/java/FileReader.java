import java.io.*;

import static parameters.Mode.NETASCII;
import static parameters.Mode.OCTET;

public class FileReader implements Closeable {
    static final byte NUL = 0x00;
    static final byte CR = 0x0D;
    static final byte LF = 0x0A;

    private final InputStream stream;

    FileReader(String mode, String filename) throws FileNotFoundException, TftpException {
        switch (mode) {
            case NETASCII:
                this.stream = new NetASCIIInputStream(filename);
                break;
            case OCTET:
                this.stream = new FileInputStream(filename);
                break;
            default:
                throw new TftpException(String.format("Unsupported file mode %s", mode));
        }
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    byte[] read() throws IOException {
        return stream.readNBytes(parameters.TftpPacketInfo.DATA_MAX_LENGTH);
    }

    static class NetASCIIInputStream extends InputStream {

        private final InputStream in;

        private boolean lastWasCR = false;
        private int nextByte;

        public NetASCIIInputStream(String filename) throws FileNotFoundException {
            in = new FileInputStream(filename);
        }

        @Override
        public int read() throws IOException {
            if (lastWasCR) {
                lastWasCR = false;
                return nextByte;
            }
            nextByte = in.read();
            switch (nextByte) {
                case (CR):
                    lastWasCR = true;
                    nextByte = NUL;
                    return CR;
                case (LF):
                    lastWasCR = true;
                    nextByte = LF;
                    return CR;
                default:
                    return nextByte;
            }
        }
    }
}
