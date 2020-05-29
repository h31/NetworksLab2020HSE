package ru.spbau.team.vnc.messages.incoming.routine;

import ru.spbau.team.vnc.Connection;
import ru.spbau.team.vnc.exceptions.ClientDisconnectedException;
import ru.spbau.team.vnc.messages.incoming.FormattedReader;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class PointerEventMessage extends RoutineMessage {
    private final List<PointerEventType> eventTypes;
    private final int xPosition;
    private final int yPosition;

    public PointerEventMessage(int xPosition, int yPosition, List<PointerEventType> eventTypes) {
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.eventTypes = eventTypes;
    }

    public static PointerEventMessage fromInputStream(FormattedReader inputStream)
            throws IOException, ClientDisconnectedException {
        int mask = inputStream.readU8();
        List<PointerEventType> eventTypes = fromMask((byte)mask);
        int xPosition = inputStream.readU16BigEndian();
        int yPosition = inputStream.readU16BigEndian();

        return new PointerEventMessage(xPosition, yPosition, eventTypes);
    }

    @Override
    public void execute(Connection connection) throws AWTException {
        Robot robot = new Robot();
        robot.mouseMove(xPosition, yPosition);
        for (PointerEventType eventType : eventTypes) {
            if (eventType == PointerEventType.LEFT_DOWN) {
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            } else if (eventType == PointerEventType.LEFT_UP) {
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            } else if (eventType == PointerEventType.MIDDLE_DOWN) {
                robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
            } else if (eventType == PointerEventType.MIDDLE_UP) {
                robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
            } else if (eventType == PointerEventType.RIGHT_DOWN) {
                robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            } else if (eventType == PointerEventType.RIGHT_UP) {
                robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
            } else if (eventType == PointerEventType.WHEEL_DOWNWARDS_DOWN) {
                robot.mouseWheel(1);
            } else if (eventType == PointerEventType.WHEEL_UPWARDS_DOWN) {
                robot.mouseWheel(-1);
            }
        }
    }


    public static List<PointerEventType> fromMask(byte mask) {
        BitSet bits = BitSet.valueOf(new byte[] {mask});

        System.out.println("Incoming pointer event mask: " + bits.toString());

        List<PointerEventType> result = new ArrayList<>();
        if (bits.get(0)) {
            result.add(PointerEventType.LEFT_DOWN);
        } else {
            result.add(PointerEventType.LEFT_UP);
        }

        if (bits.get(1)) {
            result.add(PointerEventType.MIDDLE_DOWN);
        } else {
            result.add(PointerEventType.MIDDLE_UP);
        }

        if (bits.get(2)) {
            result.add(PointerEventType.RIGHT_DOWN);
        } else {
            result.add(PointerEventType.RIGHT_UP);
        }

        if (bits.get(3)) {
            result.add(PointerEventType.WHEEL_UPWARDS_DOWN);
        } else {
            result.add(PointerEventType.WHEEL_UPWARDS_UP);
        }

        if (bits.get(4)) {
            result.add(PointerEventType.WHEEL_DOWNWARDS_DOWN);
        } else {
            result.add(PointerEventType.WHEEL_DOWNWARDS_UP);
        }

        return result;
    }
}

