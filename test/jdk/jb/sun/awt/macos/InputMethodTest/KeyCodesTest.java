/*
 * Copyright (c) 2000-2023 JetBrains s.r.o.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @summary Regression test for JBR-5173 macOS keyboard support rewrite
 * @modules java.desktop/sun.lwawt.macosx
 * @run main InputMethodTest KeyCodesTest
 * @requires (jdk.version.major >= 8 & os.family == "mac")
 */

import static java.awt.event.KeyEvent.*;

public class KeyCodesTest implements Runnable {
    static private final int ROBOT_KEYCODE_BACK_QUOTE_ISO = 0x2000132;
    static private final int ROBOT_KEYCODE_RIGHT_COMMAND = 0x2000036;
    static private final int ROBOT_KEYCODE_RIGHT_SHIFT = 0x200003C;
    static private final int ROBOT_KEYCODE_RIGHT_CONTROL = 0x200003E;
    static private final int ROBOT_KEYCODE_YEN_SYMBOL_JIS = 0x200025D;
    static private final int ROBOT_KEYCODE_CIRCUMFLEX_JIS = 0x2000218;
    static private final int ROBOT_KEYCODE_NUMPAD_COMMA_JIS = 0x200025F;
    static private final int ROBOT_KEYCODE_NUMPAD_ENTER = 0x200004C;
    static private final int ROBOT_KEYCODE_NUMPAD_EQUALS = 0x2000051;
    static private final int VK_SECTION = 0x01000000+0x00A7;
    @Override
    public void run() {
        // ordinary non-letter character with VK_ key codes
        verify('!', VK_EXCLAMATION_MARK, "com.apple.keylayout.French-PC", VK_SLASH);
        verify('"', VK_QUOTEDBL, "com.apple.keylayout.French-PC", VK_3);
        verify('#', VK_NUMBER_SIGN, "com.apple.keylayout.British-PC", VK_BACK_SLASH);
        verify('$', VK_DOLLAR, "com.apple.keylayout.French-PC", VK_CLOSE_BRACKET);
        verify('&', VK_AMPERSAND, "com.apple.keylayout.French-PC", VK_1);
        verify('\'', VK_QUOTE, "com.apple.keylayout.French-PC", VK_4);
        verify('(', VK_LEFT_PARENTHESIS, "com.apple.keylayout.French-PC", VK_5);
        verify(')', VK_RIGHT_PARENTHESIS, "com.apple.keylayout.French-PC", VK_MINUS);
        verify('*', VK_ASTERISK, "com.apple.keylayout.French-PC", VK_BACK_SLASH);
        verify('+', VK_PLUS, "com.apple.keylayout.German", VK_CLOSE_BRACKET);
        verify(',', VK_COMMA, "com.apple.keylayout.ABC", VK_COMMA);
        verify('-', VK_MINUS, "com.apple.keylayout.ABC", VK_MINUS);
        verify('.', VK_PERIOD, "com.apple.keylayout.ABC", VK_PERIOD);
        verify('/', VK_SLASH, "com.apple.keylayout.ABC", VK_SLASH);
        verify(':', VK_COLON, "com.apple.keylayout.French-PC", VK_PERIOD);
        verify(';', VK_SEMICOLON, "com.apple.keylayout.ABC", VK_SEMICOLON);
        verify('<', VK_LESS, "com.apple.keylayout.French-PC", VK_BACK_QUOTE);
        verify('=', VK_EQUALS, "com.apple.keylayout.ABC", VK_EQUALS);
        verify('>', VK_GREATER, "com.apple.keylayout.Turkish", VK_CLOSE_BRACKET);
        verify('@', VK_AT, "com.apple.keylayout.Norwegian", VK_BACK_SLASH);
        verify('[', VK_OPEN_BRACKET, "com.apple.keylayout.ABC", VK_OPEN_BRACKET);
        verify('\\', VK_BACK_SLASH, "com.apple.keylayout.ABC", VK_BACK_SLASH);
        verify(']', VK_CLOSE_BRACKET, "com.apple.keylayout.ABC", VK_CLOSE_BRACKET);
        verify('^', VK_CIRCUMFLEX, "com.apple.keylayout.ABC", ROBOT_KEYCODE_CIRCUMFLEX_JIS);
        verify('_', VK_UNDERSCORE, "com.apple.keylayout.French-PC", VK_8);
        verify('`', VK_BACK_QUOTE, "com.apple.keylayout.ABC", VK_BACK_QUOTE);
        verify('{', VK_BRACELEFT, "com.apple.keylayout.LatinAmerican", VK_QUOTE);
        verify('}', VK_BRACERIGHT, "com.apple.keylayout.LatinAmerican", VK_BACK_SLASH);
        verify('\u00a1', VK_INVERTED_EXCLAMATION_MARK, "com.apple.keylayout.Spanish-ISO", VK_EQUALS);
        // TODO: figure out which keyboard layout has VK_EURO_SIGN as a key on the primary layer
        verify(' ', VK_SPACE, "com.apple.keylayout.ABC", VK_SPACE);

        // control characters
        verify('\t', VK_TAB, "com.apple.keylayout.ABC", VK_TAB);
        verify('\n', VK_ENTER, "com.apple.keylayout.ABC", VK_ENTER);
        verify('\0', VK_BACK_SPACE, "com.apple.keylayout.ABC", VK_BACK_SPACE);
        verify('\0', VK_ESCAPE, "com.apple.keylayout.ABC", VK_ESCAPE);

        // keypad
        verify('/', VK_DIVIDE, "com.apple.keylayout.ABC", VK_DIVIDE, VK_SLASH, KEY_LOCATION_NUMPAD, 0);
        verify('*', VK_MULTIPLY, "com.apple.keylayout.ABC", VK_MULTIPLY, VK_ASTERISK, KEY_LOCATION_NUMPAD, 0);
        verify('+', VK_ADD, "com.apple.keylayout.ABC", VK_ADD, VK_PLUS, KEY_LOCATION_NUMPAD, 0);
        verify('-', VK_SUBTRACT, "com.apple.keylayout.ABC", VK_SUBTRACT, VK_MINUS, KEY_LOCATION_NUMPAD, 0);
        verify('\0', VK_CLEAR, "com.apple.keylayout.ABC", VK_CLEAR, VK_UNDEFINED, KEY_LOCATION_NUMPAD, 0);
        verify('\n', VK_ENTER, "com.apple.keylayout.ABC", ROBOT_KEYCODE_NUMPAD_ENTER, VK_ENTER, KEY_LOCATION_NUMPAD, 0);
        verify(',', VK_COMMA, "com.apple.keylayout.ABC", ROBOT_KEYCODE_NUMPAD_COMMA_JIS, VK_COMMA, KEY_LOCATION_NUMPAD, 0);
        verify('=', VK_EQUALS, "com.apple.keylayout.ABC", ROBOT_KEYCODE_NUMPAD_EQUALS, VK_EQUALS, KEY_LOCATION_NUMPAD, 0);
        verify('.', VK_DECIMAL, "com.apple.keylayout.ABC", VK_DECIMAL, VK_PERIOD, KEY_LOCATION_NUMPAD, 0);

        // keypad numbers
        for (int i = 0; i < 10; ++i) {
            verify((char)('0' + i), VK_NUMPAD0 + i, "com.apple.keylayout.ABC", VK_NUMPAD0 + i, VK_0 + i, KEY_LOCATION_NUMPAD, 0);
        }

        // function keys
        verify('\0', VK_F1, "com.apple.keylayout.ABC", VK_F1);
        verify('\0', VK_F19, "com.apple.keylayout.ABC", VK_F19);

        // Test ANSI/ISO/JIS keyboard weirdness
        verify('\u00a7', 0x01000000+0x00A7, "com.apple.keylayout.ABC", VK_SECTION);
        verify('\u00b2', 0x01000000+0x00B2, "com.apple.keylayout.French-PC", VK_SECTION);
        verify('#', VK_NUMBER_SIGN, "com.apple.keylayout.CanadianFrench-PC", VK_SECTION);
        verify('\u00ab', 0x01000000+0x00AB, "com.apple.keylayout.CanadianFrench-PC", ROBOT_KEYCODE_BACK_QUOTE_ISO);
        verify('#', VK_NUMBER_SIGN, "com.apple.keylayout.CanadianFrench-PC", VK_BACK_QUOTE);
        verify('\u00a5', 0x01000000+0x00A5, "com.apple.keylayout.ABC", ROBOT_KEYCODE_YEN_SYMBOL_JIS);

        // Test extended key codes that don't match the unicode char
        verify('\u00e4', 0x01000000+0x00C4, "com.apple.keylayout.German", VK_QUOTE);
        verify('\u00e5', 0x01000000+0x00C5, "com.apple.keylayout.Norwegian", VK_OPEN_BRACKET);
        verify('\u00e6', 0x01000000+0x00C6, "com.apple.keylayout.Norwegian", VK_QUOTE);
        verify('\u00e7', 0x01000000+0x00C7, "com.apple.keylayout.French-PC", VK_9);
        verify('\u00f1', 0x01000000+0x00D1, "com.apple.keylayout.Spanish-ISO", VK_SEMICOLON);
        verify('\u00f6', 0x01000000+0x00D6, "com.apple.keylayout.German", VK_SEMICOLON);
        verify('\u00f8', 0x01000000+0x00D8, "com.apple.keylayout.Norwegian", VK_SEMICOLON);

        // test modifier keys
        verify('\0', VK_ALT, "com.apple.keylayout.ABC", VK_ALT, VK_UNDEFINED, KEY_LOCATION_LEFT, ALT_DOWN_MASK);
        verify('\0', VK_ALT_GRAPH, "com.apple.keylayout.ABC", VK_ALT_GRAPH, VK_UNDEFINED, KEY_LOCATION_RIGHT, ALT_GRAPH_DOWN_MASK);
        verify('\0', VK_META, "com.apple.keylayout.ABC", VK_META, VK_UNDEFINED, KEY_LOCATION_LEFT, META_DOWN_MASK);
        verify('\0', VK_META, "com.apple.keylayout.ABC", ROBOT_KEYCODE_RIGHT_COMMAND, VK_UNDEFINED, KEY_LOCATION_RIGHT, META_DOWN_MASK);
        verify('\0', VK_CONTROL, "com.apple.keylayout.ABC", VK_CONTROL, VK_UNDEFINED, KEY_LOCATION_LEFT, CTRL_DOWN_MASK);
        verify('\0', VK_CONTROL, "com.apple.keylayout.ABC", ROBOT_KEYCODE_RIGHT_CONTROL, VK_UNDEFINED, KEY_LOCATION_RIGHT, CTRL_DOWN_MASK);
        verify('\0', VK_SHIFT, "com.apple.keylayout.ABC", VK_SHIFT, VK_UNDEFINED, KEY_LOCATION_LEFT, SHIFT_DOWN_MASK);
        verify('\0', VK_SHIFT, "com.apple.keylayout.ABC", ROBOT_KEYCODE_RIGHT_SHIFT, VK_UNDEFINED, KEY_LOCATION_RIGHT, SHIFT_DOWN_MASK);
    }

    private void verify(char ch, int vk, String layout, int key, int charKeyCode, int location, int modifiers) {
        InputMethodTest.section("Key code test: " + vk + ", char: " + String.format("U+%04X", (int)ch));
        InputMethodTest.layout(layout);
        InputMethodTest.type(key, 0);

        var events = InputMethodTest.getTriggeredEvents();
        var pressed = events.stream().filter(e -> e.getID() == KEY_PRESSED).toList();
        var released = events.stream().filter(e -> e.getID() == KEY_RELEASED).toList();

        if (pressed.size() == 1) {
            var keyCode = pressed.get(0).getKeyCode();
            InputMethodTest.expectTrue(keyCode == vk, "key press, actual key code: " + keyCode + ", expected: " + vk);

            var keyLocation = pressed.get(0).getKeyLocation();
            InputMethodTest.expectTrue(keyLocation == location, "key press, actual key location: " + keyLocation + ", expected: " + location);

            var keyModifiers = pressed.get(0).getModifiersEx();
            InputMethodTest.expectTrue(keyModifiers == modifiers, "key press, actual key modifiers: " + keyModifiers + ", expected: " + modifiers);
        } else {
            InputMethodTest.fail("expected exactly one KEY_PRESSED event, got " + pressed.size());
        }

        if (released.size() == 1) {
            var keyCode = released.get(0).getKeyCode();
            InputMethodTest.expectTrue(keyCode == vk, "key release, actual key code: " + keyCode + ", expected: " + vk);

            var keyLocation = released.get(0).getKeyLocation();
            InputMethodTest.expectTrue(keyLocation == location, "key release, actual key location: " + keyLocation + ", expected: " + location);

            var keyModifiers = released.get(0).getModifiersEx();
            InputMethodTest.expectTrue(keyModifiers == 0, "key release, actual key modifiers: " + keyModifiers + ", expected: 0");
        } else {
            InputMethodTest.fail("expected exactly one KEY_RELEASED event, got " + released.size());
        }

        if (ch != 0) {
            InputMethodTest.expect(String.valueOf(ch));
            InputMethodTest.expectTrue(getExtendedKeyCodeForChar(ch) == charKeyCode, "getExtendedKeyCodeForChar");
        }
    }

    private void verify(char ch, int vk, String layout, int key) {
        verify(ch, vk, layout, key, vk, KEY_LOCATION_STANDARD, 0);
    }
}
