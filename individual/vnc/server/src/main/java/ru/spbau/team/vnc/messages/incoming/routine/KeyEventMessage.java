package ru.spbau.team.vnc.messages.incoming.routine;

import ru.spbau.team.vnc.Connection;
import ru.spbau.team.vnc.exceptions.ClientDisconnectedException;
import ru.spbau.team.vnc.messages.incoming.FormattedReader;
import ru.spbau.team.vnc.utils.KeyConverter;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class KeyEventMessage extends RoutineMessage {
    private final boolean downFlag;
    private final int key;

    public KeyEventMessage(boolean downFlag, int key) {
        this.downFlag = downFlag;
        this.key = key;
    }

    public static KeyEventMessage fromInputStream(FormattedReader inputStream)
            throws IOException, ClientDisconnectedException {
        boolean downFlag = inputStream.readU8() != 0;
        inputStream.readU8();
        inputStream.readU8();
        int key = (int) inputStream.readU32BigEndian();

        return new KeyEventMessage(downFlag, key);
    }

    @Override
    public void execute(Connection connection) throws AWTException {
        Robot robot = new Robot();
        int virtualKey = KeyConverter.toVirtualKey(key);
        if (virtualKey == KeyEvent.VK_UNDEFINED) {
            return;
        }

        if (downFlag) {
            robot.keyPress(virtualKey);
        } else {
            robot.keyRelease(virtualKey);
        }
    }
}
