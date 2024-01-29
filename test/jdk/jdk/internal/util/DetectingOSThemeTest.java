/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @test
 * @summary DetectingOSThemeTest checks that JBR could correctly detect OS theme and notify about theme changing in time
 * @run main DetectingOSThemeTest
 * @requires (os.family == "linux")
 */
public class DetectingOSThemeTest {
    private static String currentTheme() {
        Boolean val = (Boolean) Toolkit.getDefaultToolkit().getDesktopProperty("awt.os.theme.isDark");
        if (val == null) {
            return "Undefined";
        }
        return (val) ? "Dark" : "Light";
    }

    private static void setOsDarkTheme(String val) {
        try {
            if (val.equals("Dark")) {
                Runtime.getRuntime().exec("gsettings set org.gnome.desktop.interface gtk-theme 'Adwaita-dark'");
                Runtime.getRuntime().exec("gsettings set org.gnome.desktop.interface color-scheme 'prefer-dark'");
            } else {
                Runtime.getRuntime().exec("gsettings set org.gnome.desktop.interface gtk-theme 'Adwaita'");
                Runtime.getRuntime().exec("gsettings set org.gnome.desktop.interface color-scheme 'default'");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int iter = 0;
    private static final int TIME_TO_WAIT = 2000;

    public static void main(String[] args) throws Exception {
        setOsDarkTheme("Light");
        if (!currentTheme().equals("Light")) {
            throw new RuntimeException("Test Failed! Initial OS theme supposed to be Light");
        }

        ArrayList<String> themesOrder = new ArrayList<>(Arrays.asList("Dark", "Light", "Dark"));
        Toolkit.getDefaultToolkit().addPropertyChangeListener("awt.os.theme.isDark", evt -> {
            if (!currentTheme().equals(themesOrder.get(iter++))) {
                throw new RuntimeException("Test Failed! Current OS theme doesn't matched with detected");
            }
        });

        for (int i = 0; i < 3; i++) {
            setOsDarkTheme(themesOrder.get(i));
            Thread.sleep(TIME_TO_WAIT);
            if (i + 1 != iter) {
                throw new RuntimeException("Test Failed! Changing OS theme was not detected");
            }
        }
    }
}