/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sun.lwawt.macosx.LWCToolkit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class InputMethodTest {
    private static JFrame frame;
    private static JTextArea textArea;
    private static Robot robot;
    private static String currentTest = "";
    private static String currentSection = "";
    private static String initialLayout;
    private static boolean success = true;

    private enum TestCases {
        IDEA_271898 (new IDEA_271898())
        ;

        private Runnable test;

        TestCases(Runnable runnable) {
            test = runnable;
        }

        public void run() {
            test.run();
        }
    }

    public static void main(String[] args) {
        init();
        for (String arg : args) {
            runTest(arg);
        }
        LWCToolkit.switchKeyboardLayout(initialLayout);

        if (!success) {
            throw new RuntimeException("Some tests failed");
        }
    }

    private static void init() {
        try {
            robot = new Robot();
            robot.setAutoDelay(50);
        } catch (AWTException e) {
            e.printStackTrace();
            System.exit(1);
        }

        initialLayout = LWCToolkit.getKeyboardLayoutId();

        frame = new JFrame("InputMethodTest");
        frame.setVisible(true);
        frame.setSize(300, 300);
        frame.setLocationRelativeTo(null);

        textArea = new JTextArea();

        frame.setLayout(new BorderLayout());
        frame.getContentPane().add(textArea, BorderLayout.CENTER);

        textArea.grabFocus();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}
    }

    private static void runTest(String name) {
        currentTest = name;
        TestCases.valueOf(name).run();
    }

    public static void section(String description) {
        currentSection = description;
        textArea.setText("");
    }

    public static void layout(String name) {
        LWCToolkit.switchKeyboardLayout(name);
    }

    public static void type(int key, int modifiers) {
        List<Integer> modKeys = new ArrayList<>();

        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            modKeys.add(KeyEvent.VK_ALT);
        }

        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            modKeys.add(KeyEvent.VK_CONTROL);
        }

        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            modKeys.add(KeyEvent.VK_SHIFT);
        }

        if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
            modKeys.add(KeyEvent.VK_META);
        }

        for (var modKey : modKeys) {
            robot.keyPress(modKey);
        }

        robot.keyPress(key);
        robot.keyRelease(key);

        for (var modKey : modKeys) {
            robot.keyRelease(modKey);
        }
    }

    public static void expect(String expectedValue) {
        var actualValue = textArea.getText();
        if (actualValue.equals(expectedValue)) {
            System.out.printf("Test %s (%s) passed, got '%s'\n", currentTest, currentSection, actualValue);
        } else {
            success = false;
            System.out.printf("Test %s (%s) failed, expected '%s', got '%s'\n", currentTest, currentSection, expectedValue, actualValue);
        }
    }
}
