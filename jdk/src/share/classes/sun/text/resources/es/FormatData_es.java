/*
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.text.resources.es;

import java.util.ListResourceBundle;

public class FormatData_es extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "enero", // january
                    "febrero", // february
                    "marzo", // march
                    "abril", // april
                    "mayo", // may
                    "junio", // june
                    "julio", // july
                    "agosto", // august
                    "septiembre", // september
                    "octubre", // october
                    "noviembre", // november
                    "diciembre", // december
                    "" // month 13 if applicable
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "ene", // abb january
                    "feb", // abb february
                    "mar", // abb march
                    "abr", // abb april
                    "may", // abb may
                    "jun", // abb june
                    "jul", // abb july
                    "ago", // abb august
                    "sep", // abb september
                    "oct", // abb october
                    "nov", // abb november
                    "dic", // abb december
                    "" // abb month 13 if applicable
                }
            },
            { "DayNames",
                new String[] {
                    "domingo", // Sunday
                    "lunes", // Monday
                    "martes", // Tuesday
                    "mi\u00e9rcoles", // Wednesday
                    "jueves", // Thursday
                    "viernes", // Friday
                    "s\u00e1bado" // Saturday
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "dom", // abb Sunday
                    "lun", // abb Monday
                    "mar", // abb Tuesday
                    "mi\u00e9", // abb Wednesday
                    "jue", // abb Thursday
                    "vie", // abb Friday
                    "s\u00e1b" // abb Saturday
                }
            },
            { "DayNarrows",
                new String[] {
                    "D",
                    "L",
                    "M",
                    "X",
                    "J",
                    "V",
                    "S",
                }
            },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###;-#,##0.###", // decimal pattern
                    "\u00A4#,##0.00;(\u00A4#,##0.00)", // currency pattern
                    "#,##0%" // percent pattern
                }
            },
            { "NumberElements",
                new String[] {
                    ",", // decimal separator
                    ".", // group (thousands) separator
                    ";", // list separator
                    "%", // percent sign
                    "0", // native 0 digit
                    "#", // pattern digit
                    "-", // minus sign
                    "E", // exponential
                    "\u2030", // per mille
                    "\u221e", // infinity
                    "\ufffd" // NaN
                }
            },
            { "TimePatterns",
                new String[] {
                    "HH'H'mm'' z", // full time pattern
                    "H:mm:ss z", // long time pattern
                    "H:mm:ss", // medium time pattern
                    "H:mm", // short time pattern
                }
            },
            { "DatePatterns",
                new String[] {
                    "EEEE d' de 'MMMM' de 'yyyy", // full date pattern
                    "d' de 'MMMM' de 'yyyy", // long date pattern
                    "dd-MMM-yyyy", // medium date pattern
                    "d/MM/yy", // short date pattern
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}" // date-time pattern
                }
            },
            { "DateTimePatternChars", "GyMdkHmsSEDFwWahKzZ" },
        };
    }
}
