package ru.spbau.team.vnc.messages.outcoming.update.encodings;

import java.io.IOException;

public interface EncodedRectangle {
    int getEncodingType();
    byte[] toByteArray() throws IOException;
}
