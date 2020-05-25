package ru.spbau.team.vnc.messages.incoming.routine;

import ru.spbau.team.vnc.Connection;
import ru.spbau.team.vnc.messages.Utils;

import java.io.IOException;
import java.io.InputStream;

public class SetEncodingsMessage extends RoutineMessage {

    private final int[] encodings;

    public SetEncodingsMessage(int[] encodings) {
        this.encodings = encodings;
    }

    public static SetEncodingsMessage fromInputStream(InputStream inputStream) throws IOException {
        int padding = Utils.readU8(inputStream);
        int numberOfEncodings = Utils.readU16BigEndian(inputStream);
        System.out.println("Have to set " + numberOfEncodings);
        var encodings = new int[numberOfEncodings];
        for (int encodingIndex = 0; encodingIndex < numberOfEncodings; ++encodingIndex) {
            encodings[encodingIndex] = Utils.readS32BigEndian(inputStream);
            System.out.println("Set encoding " + encodings[encodingIndex]);
        }

        return new SetEncodingsMessage(encodings);
    }

    @Override
    public void execute(Connection connection) {
        // TODO
    }

    public int[] getEncodings() {
        return encodings;
    }
}
