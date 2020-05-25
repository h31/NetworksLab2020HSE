package ru.spbau.team.vnc.messages.incoming.routine;

import ru.spbau.team.vnc.Connection;
import ru.spbau.team.vnc.exceptions.ClientDisconnectedException;
import ru.spbau.team.vnc.messages.incoming.FormattedReader;

import java.io.IOException;

public class SetEncodingsMessage extends RoutineMessage {

    private final int[] encodings;

    public SetEncodingsMessage(int[] encodings) {
        this.encodings = encodings;
    }

    public static SetEncodingsMessage fromInputStream(FormattedReader inputStream) throws IOException, ClientDisconnectedException {
        int padding = inputStream.readU8();
        int numberOfEncodings = inputStream.readU16BigEndian();
        System.out.println("Have to set " + numberOfEncodings);
        var encodings = new int[numberOfEncodings];
        for (int encodingIndex = 0; encodingIndex < numberOfEncodings; ++encodingIndex) {
            encodings[encodingIndex] = inputStream.readS32BigEndian();
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
