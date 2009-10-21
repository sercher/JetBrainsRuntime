/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */


package sun.awt;

import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import sun.awt.Win32GraphicsEnvironment;
import sun.awt.windows.WFontConfiguration;
import sun.font.FontManager;
import sun.font.SunFontManager;
import sun.font.TrueTypeFont;
import sun.java2d.HeadlessGraphicsEnvironment;
import sun.java2d.SunGraphicsEnvironment;

/**
 * The X11 implementation of {@link FontManager}.
 */
public class Win32FontManager extends SunFontManager {

    private static String[] defaultPlatformFont = null;

    private static TrueTypeFont eudcFont;

    static {

        AccessController.doPrivileged(new PrivilegedAction() {

                public Object run() {
                    String eudcFile = getEUDCFontFile();
                    if (eudcFile != null) {
                        try {
                            eudcFont = new TrueTypeFont(eudcFile, null, 0,
                                                        true);
                        } catch (FontFormatException e) {
                        }
                    }
                    return null;
                }

            });
    }

    /* Used on Windows to obtain from the windows registry the name
     * of a file containing the system EUFC font. If running in one of
     * the locales for which this applies, and one is defined, the font
     * defined by this file is appended to all composite fonts as a
     * fallback component.
     */
    private static native String getEUDCFontFile();

    public Win32FontManager() {
        super();
        AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {

                    /* Register the JRE fonts so that the native platform can
                     * access them. This is used only on Windows so that when
                     * printing the printer driver can access the fonts.
                     */
                    registerJREFontsWithPlatform(jreFontDirName);
                    return null;
                }
            });
    }

    /* Unlike the shared code version, this expects a base file name -
     * not a full path name.
     * The font configuration file has base file names and the FontConfiguration
     * class reports these back to the GraphicsEnvironment, so these
     * are the componentFileNames of CompositeFonts.
     */
    protected void registerFontFile(String fontFileName, String[] nativeNames,
                                    int fontRank, boolean defer) {

        // REMIND: case compare depends on platform
        if (registeredFontFiles.contains(fontFileName)) {
            return;
        }
        registeredFontFiles.add(fontFileName);

        int fontFormat;
        if (getTrueTypeFilter().accept(null, fontFileName)) {
            fontFormat = SunFontManager.FONTFORMAT_TRUETYPE;
        } else if (getType1Filter().accept(null, fontFileName)) {
            fontFormat = SunFontManager.FONTFORMAT_TYPE1;
        } else {
            /* on windows we don't use/register native fonts */
            return;
        }

        if (fontPath == null) {
            fontPath = getPlatformFontPath(noType1Font);
        }

        /* Look in the JRE font directory first.
         * This is playing it safe as we would want to find fonts in the
         * JRE font directory ahead of those in the system directory
         */
        String tmpFontPath = jreFontDirName+File.pathSeparator+fontPath;
        StringTokenizer parser = new StringTokenizer(tmpFontPath,
                                                     File.pathSeparator);

        boolean found = false;
        try {
            while (!found && parser.hasMoreTokens()) {
                String newPath = parser.nextToken();
                File theFile = new File(newPath, fontFileName);
                if (theFile.canRead()) {
                    found = true;
                    String path = theFile.getAbsolutePath();
                    if (defer) {
                        registerDeferredFont(fontFileName, path,
                                             nativeNames,
                                             fontFormat, true,
                                             fontRank);
                    } else {
                        registerFontFile(path, nativeNames,
                                         fontFormat, true,
                                         fontRank);
                    }
                    break;
                }
            }
        } catch (NoSuchElementException e) {
            System.err.println(e);
        }
        if (!found) {
            addToMissingFontFileList(fontFileName);
        }
    }

    @Override
    protected FontConfiguration createFontConfiguration() {

       FontConfiguration fc = new WFontConfiguration(this);
       fc.init();
       return fc;
    }

    @Override
    public FontConfiguration createFontConfiguration(boolean preferLocaleFonts,
            boolean preferPropFonts) {

        return new WFontConfiguration(this,
                                      preferLocaleFonts,preferPropFonts);
    }

    protected void
        populateFontFileNameMap(HashMap<String,String> fontToFileMap,
                                HashMap<String,String> fontToFamilyNameMap,
                                HashMap<String,ArrayList<String>>
                                familyToFontListMap,
                                Locale locale) {

        populateFontFileNameMap0(fontToFileMap, fontToFamilyNameMap,
                                 familyToFontListMap, locale);

    }

    private static native void
        populateFontFileNameMap0(HashMap<String,String> fontToFileMap,
                                 HashMap<String,String> fontToFamilyNameMap,
                                 HashMap<String,ArrayList<String>>
                                     familyToFontListMap,
                                 Locale locale);

    public synchronized native String getFontPath(boolean noType1Fonts);

    public String[] getDefaultPlatformFont() {

        if (defaultPlatformFont != null) {
            return defaultPlatformFont;
        }

        String[] info = new String[2];
        info[0] = "Arial";
        info[1] = "c:\\windows\\fonts";
        final String[] dirs = getPlatformFontDirs(true);
        if (dirs.length > 1) {
            String dir = (String)
                AccessController.doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            for (int i=0; i<dirs.length; i++) {
                                String path =
                                    dirs[i] + File.separator + "arial.ttf";
                                File file = new File(path);
                                if (file.exists()) {
                                    return dirs[i];
                                }
                            }
                            return null;
                        }
                    });
            if (dir != null) {
                info[1] = dir;
            }
        } else {
            info[1] = dirs[0];
        }
        info[1] = info[1] + File.separator + "arial.ttf";
        defaultPlatformFont = info;
        return defaultPlatformFont;
    }

    /* register only TrueType/OpenType fonts
     * Because these need to be registed just for use when printing,
     * we defer the actual registration and the static initialiser
     * for the printing class makes the call to registerJREFontsForPrinting()
     */
    static String fontsForPrinting = null;
    protected void registerJREFontsWithPlatform(String pathName) {
        fontsForPrinting = pathName;
    }

    public static void registerJREFontsForPrinting() {
        final String pathName;
        synchronized (Win32GraphicsEnvironment.class) {
            GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (fontsForPrinting == null) {
                return;
            }
            pathName = fontsForPrinting;
            fontsForPrinting = null;
        }
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction() {
                public Object run() {
                    File f1 = new File(pathName);
                    String[] ls = f1.list(SunFontManager.getInstance().
                            getTrueTypeFilter());
                    if (ls == null) {
                        return null;
                    }
                    for (int i=0; i <ls.length; i++ ) {
                        File fontFile = new File(f1, ls[i]);
                        registerFontWithPlatform(fontFile.getAbsolutePath());
                    }
                    return null;
                }
         });
    }

    protected static native void registerFontWithPlatform(String fontName);

    protected static native void deRegisterFontWithPlatform(String fontName);

}
