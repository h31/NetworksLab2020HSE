package ru.spbau.team.vnc.utils;

import java.awt.event.KeyEvent;

public class KeyConverter {
    private static final int BackSpace	= 0xFF08;
    private static final int Tab		= 0xFF09;
    private static final int Clear		= 0xFF0B;
    private static final int Return		= 0xFF0D;
    private static final int Pause		= 0xFF13;
    private static final int ScrollLock	= 0xFF14;
    private static final int Escape		= 0xFF1B;

    private static final int Delete		= 0xFFFF;

    private static final int Home		= 0xFF50;
    private static final int Left		= 0xFF51;
    private static final int Up			= 0xFF52;
    private static final int Right		= 0xFF53;
    private static final int Down		= 0xFF54;
    private static final int PageUp		= 0xFF55;
    private static final int PageDown	= 0xFF56;
    private static final int End		= 0xFF57;

    private static final int Print		= 0xFF61;
    private static final int Insert		= 0xFF63;

    private static final int Cancel		= 0xFF69;
    private static final int Help		= 0xFF6A;

    private static final int KpSpace	= 0xFF80;
    private static final int KpTab		= 0xFF89;
    private static final int KpEnter	= 0xFF8D;

    private static final int KpHome		= 0xFF95;
    private static final int KpLeft		= 0xFF96;
    private static final int KpUp		= 0xFF97;
    private static final int KpRight	= 0xFF98;
    private static final int KpDown		= 0xFF99;
    private static final int KpPageUp	= 0xFF9A;
    private static final int KpPageDown	= 0xFF9B;
    private static final int KpEnd		= 0xFF9C;
    private static final int KpInsert	= 0xFF9E;
    private static final int KpDelete	= 0xFF9F;
    private static final int KpEqual	= 0xFFBD;
    private static final int KpMultiply	= 0xFFAA;
    private static final int KpAdd		= 0xFFAB;
    private static final int KpSeparator = 0xFFAC;
    private static final int KpSubtract	= 0xFFAD;
    private static final int KpDecimal	= 0xFFAE;
    private static final int KpDivide	= 0xFFAF;

    private static final int KpF1		= 0xFF91;
    private static final int KpF2		= 0xFF92;
    private static final int KpF3		= 0xFF93;
    private static final int KpF4		= 0xFF94;

    private static final int Kp0		= 0xFFB0;
    private static final int Kp1		= 0xFFB1;
    private static final int Kp2		= 0xFFB2;
    private static final int Kp3		= 0xFFB3;
    private static final int Kp4		= 0xFFB4;
    private static final int Kp5		= 0xFFB5;
    private static final int Kp6	    = 0xFFB6;
    private static final int Kp7		= 0xFFB7;
    private static final int Kp8		= 0xFFB8;
    private static final int Kp9		= 0xFFB9;

    private static final int F1			= 0xFFBE;
    private static final int F2			= 0xFFBF;
    private static final int F3			= 0xFFC0;
    private static final int F4			= 0xFFC1;
    private static final int F5			= 0xFFC2;
    private static final int F6			= 0xFFC3;
    private static final int F7			= 0xFFC4;
    private static final int F8			= 0xFFC5;
    private static final int F9			= 0xFFC6;
    private static final int F10		= 0xFFC7;
    private static final int F11		= 0xFFC8;
    private static final int F12		= 0xFFC9;

    private static final int ShiftL		= 0xFFE1;
    private static final int ShiftR		= 0xFFE2;
    private static final int ControlL	= 0xFFE3;
    private static final int ControlR	= 0xFFE4;
    private static final int MetaL		= 0xFFE7;
    private static final int MetaR		= 0xFFE8;
    private static final int AltL		= 0xFFE9;
    private static final int AltR		= 0xFFEA;

    public static int toVirtualKey(int keysym)
    {
        if ((keysym >= 0x0020 && keysym <= 0x007e) || (keysym >= 0x00a0 && keysym <= 0x00ff)) {
            return KeyEvent.getExtendedKeyCodeForChar((char)keysym);
        }

        if ((keysym & 0xff000000) == 0x01000000) {
            return KeyEvent.getExtendedKeyCodeForChar((char)(keysym & 0x00ffffff));
        }

        switch(keysym)
        {
            case Tab:
            case KpTab:
                return KeyEvent.VK_TAB;
            case Clear:
                return KeyEvent.VK_CLEAR;
            case Return:
            case KpEnter:
                return KeyEvent.VK_ENTER;
            case Pause:
                return KeyEvent.VK_PAUSE;
            case ScrollLock:
                return KeyEvent.VK_SCROLL_LOCK;
            case Escape:
                return KeyEvent.VK_ESCAPE;

            case 0:
            case Delete:
            case KpDelete:
                return KeyEvent.VK_DELETE;

            case Home:
            case KpHome:
                return KeyEvent.VK_HOME;
            case Left:
            case KpLeft:
                return KeyEvent.VK_LEFT;
            case Up:
            case KpUp:
                return KeyEvent.VK_UP;
            case Right:
            case KpRight:
                return KeyEvent.VK_RIGHT;
            case Down:
            case KpDown:
                return KeyEvent.VK_DOWN;
            case PageUp:
            case KpPageUp:
                return KeyEvent.VK_PAGE_UP;
            case PageDown:
            case KpPageDown:
                return KeyEvent.VK_PAGE_DOWN;
            case End:
            case KpEnd:
                return KeyEvent.VK_END;
            case Print:
                return KeyEvent.VK_PRINTSCREEN;
            case Insert:
            case KpInsert:
                return KeyEvent.VK_INSERT;
            case Cancel:
                return KeyEvent.VK_CANCEL;
            case Help:
                return KeyEvent.VK_HELP;
            case KpSpace:
                return KeyEvent.VK_SPACE;
            case KpEqual: 
                return KeyEvent.VK_EQUALS;
            case KpMultiply: 
                return KeyEvent.VK_MULTIPLY;
            case KpAdd: 
                return KeyEvent.VK_ADD;
            case KpSeparator: 
                return KeyEvent.VK_SEPARATOR;
            case KpSubtract: 
                return KeyEvent.VK_SUBTRACT;
            case KpDecimal: 
                return KeyEvent.VK_DECIMAL;
            case KpDivide: 
                return KeyEvent.VK_DIVIDE;

            case KpF1:

            case F1:
                return KeyEvent.VK_F1;
            case KpF2:
            case F2:
                return KeyEvent.VK_F2;
            case KpF3:
            case F3:
                return KeyEvent.VK_F3;
            case KpF4:
            case F4:
                return KeyEvent.VK_F4;

            case Kp0: 
                return KeyEvent.VK_NUMPAD0;
            case Kp1: 
                return KeyEvent.VK_NUMPAD1;
            case Kp2: 
                return KeyEvent.VK_NUMPAD2;
            case Kp3: 
                return KeyEvent.VK_NUMPAD3;
            case Kp4: 
                return KeyEvent.VK_NUMPAD4;
            case Kp5: 
                return KeyEvent.VK_NUMPAD5;
            case Kp6: 
                return KeyEvent.VK_NUMPAD6;
            case Kp7: 
                return KeyEvent.VK_NUMPAD7;
            case Kp8: 
                return KeyEvent.VK_NUMPAD8;
            case Kp9: 
                return KeyEvent.VK_NUMPAD9;

            case F5: 
                return KeyEvent.VK_F5; 
            case F6: 
                return KeyEvent.VK_F6; 
            case F7: 
                return KeyEvent.VK_F7; 
            case F8: 
                return KeyEvent.VK_F8; 
            case F9: 
                return KeyEvent.VK_F9; 
            case F10: 
                return KeyEvent.VK_F10; 
            case F11: 
                return KeyEvent.VK_F11; 
            case F12:
                return KeyEvent.VK_F12;

            case BackSpace: 
                return KeyEvent.VK_BACK_SPACE; 
            case ShiftL:
            case ShiftR:
                return KeyEvent.VK_SHIFT;
            case ControlL:
            case ControlR:
                return KeyEvent.VK_CONTROL;
            case MetaL:
            case MetaR:
                return KeyEvent.VK_META;
            case AltL:
            case AltR:
                return KeyEvent.VK_ALT;

            default: 
                return KeyEvent.VK_UNDEFINED;
        }
    }
}
