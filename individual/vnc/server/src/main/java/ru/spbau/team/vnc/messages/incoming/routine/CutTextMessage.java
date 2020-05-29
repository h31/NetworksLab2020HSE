package ru.spbau.team.vnc.messages.incoming.routine;

import ru.spbau.team.vnc.Connection;
import ru.spbau.team.vnc.exceptions.ClientDisconnectedException;
import ru.spbau.team.vnc.messages.incoming.FormattedReader;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;

public class CutTextMessage extends RoutineMessage {
    private final String text;

    public CutTextMessage(String text) {
        this.text = text;
    }

    public static CutTextMessage fromInputStream(FormattedReader inputStream)
            throws IOException, ClientDisconnectedException {
        for (int i = 0; i < 3; i++) {
            inputStream.readU8();
        }

        long size = inputStream.readU32BigEndian();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append((char)inputStream.readU8());
        }

        return new CutTextMessage(sb.toString());
    }

    @Override
    public void execute(Connection connection) {
        StringSelection ss = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
    }
}
