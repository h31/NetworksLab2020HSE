import java.io.*;

import static parameters.Mode.NETASCII;
import static parameters.Mode.OCTET;

public class FileWriter implements Closeable {
    static final byte NUL = 0x00;
    static final byte CR = 0x0D;
    static final byte LF = 0x0A;

    private final OutputStream stream;
    private final File file;

    public FileWriter(String mode, String filename) throws FileNotFoundException, TftpException {
        file = new File(filename);
        switch (mode) {
            case NETASCII:
                this.stream = new NetASCIIOutputStream(filename);
                break;
            case OCTET:
                this.stream = new FileOutputStream(filename);
                break;
            default:
                throw new TftpException(String.format("Unsupported file mode %s", mode));
        }
    }

    @Override
    public void close() throws IOException {
        stream.close();
        file.delete();
    }

    public void write(byte[] data) throws IOException {
        stream.write(data);
    }

    static class NetASCIIOutputStream extends OutputStream {

        private final OutputStream out;

        private boolean lastWasCR = false;

        public NetASCIIOutputStream(String filename) throws FileNotFoundException {
            out = new FileOutputStream(filename);
        }

        @Override
        public void write(int b) throws IOException {
            switch (b) {
                case CR:
                    lastWasCR = true;
                    break;
                case LF:
                    if (lastWasCR) {
                        out.write(CR);
                        out.write(LF);
                        lastWasCR = false;
                        break;
                    }
                    lastWasCR = false;
                    out.write(LF);
                    break;
                default:
                    if (lastWasCR) {
                        out.write(CR);
                        out.write(NUL);
                        lastWasCR = false;
                    }
                    out.write(b);
                    break;
            }
        }
    }
}
