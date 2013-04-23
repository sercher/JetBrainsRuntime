/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * COPYRIGHT AND PERMISSION NOTICE
 *
 * Copyright (C) 1991-2012 Unicode, Inc. All rights reserved. Distributed under
 * the Terms of Use in http://www.unicode.org/copyright.html.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of the Unicode data files and any associated documentation (the "Data
 * Files") or Unicode software and any associated documentation (the
 * "Software") to deal in the Data Files or Software without restriction,
 * including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, and/or sell copies of the Data Files or Software, and
 * to permit persons to whom the Data Files or Software are furnished to do so,
 * provided that (a) the above copyright notice(s) and this permission notice
 * appear with all copies of the Data Files or Software, (b) both the above
 * copyright notice(s) and this permission notice appear in associated
 * documentation, and (c) there is clear notice in each modified Data File or
 * in the Software as well as in the documentation associated with the Data
 * File(s) or Software that the data or software has been modified.
 *
 * THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
 * THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS
 * INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR
 * CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
 * DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE
 * OF THE DATA FILES OR SOFTWARE.
 *
 * Except as contained in this notice, the name of a copyright holder shall not
 * be used in advertising or otherwise to promote the sale, use or other
 * dealings in these Data Files or Software without prior written authorization
 * of the copyright holder.
 */

package sun.text.resources.iw;

import sun.util.resources.ParallelListResourceBundle;

public class FormatData_iw extends ParallelListResourceBundle {
    /**
     * Overrides ParallelListResourceBundle
     */
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "\u05d9\u05e0\u05d5\u05d0\u05e8", // january
                    "\u05e4\u05d1\u05e8\u05d5\u05d0\u05e8", // february
                    "\u05de\u05e8\u05e5", // march
                    "\u05d0\u05e4\u05e8\u05d9\u05dc", // april
                    "\u05de\u05d0\u05d9", // may
                    "\u05d9\u05d5\u05e0\u05d9", // june
                    "\u05d9\u05d5\u05dc\u05d9", // july
                    "\u05d0\u05d5\u05d2\u05d5\u05e1\u05d8", // august
                    "\u05e1\u05e4\u05d8\u05de\u05d1\u05e8", // september
                    "\u05d0\u05d5\u05e7\u05d8\u05d5\u05d1\u05e8", // october
                    "\u05e0\u05d5\u05d1\u05de\u05d1\u05e8", // november
                    "\u05d3\u05e6\u05de\u05d1\u05e8", // december
                    "" // month 13 if applicable
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "\u05d9\u05e0\u05d5", // abb january
                    "\u05e4\u05d1\u05e8", // abb february
                    "\u05de\u05e8\u05e5", // abb march
                    "\u05d0\u05e4\u05e8", // abb april
                    "\u05de\u05d0\u05d9", // abb may
                    "\u05d9\u05d5\u05e0", // abb june
                    "\u05d9\u05d5\u05dc", // abb july
                    "\u05d0\u05d5\u05d2", // abb august
                    "\u05e1\u05e4\u05d8", // abb september
                    "\u05d0\u05d5\u05e7", // abb october
                    "\u05e0\u05d5\u05d1", // abb november
                    "\u05d3\u05e6\u05de", // abb december
                    "" // abb month 13 if applicable
                }
            },
            { "standalone.MonthAbbreviations",
                new String[] {
                    "\u05d9\u05e0\u05d5\u05f3",
                    "\u05e4\u05d1\u05e8\u05f3",
                    "\u05de\u05e8\u05e5",
                    "\u05d0\u05e4\u05e8\u05f3",
                    "\u05de\u05d0\u05d9",
                    "\u05d9\u05d5\u05e0\u05f3",
                    "\u05d9\u05d5\u05dc\u05f3",
                    "\u05d0\u05d5\u05d2\u05f3",
                    "\u05e1\u05e4\u05d8\u05f3",
                    "\u05d0\u05d5\u05e7\u05f3",
                    "\u05e0\u05d5\u05d1\u05f3",
                    "\u05d3\u05e6\u05de\u05f3",
                    "",
                }
            },
            { "MonthNarrows",
                new String[] {
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
                    "7",
                    "8",
                    "9",
                    "10",
                    "11",
                    "12",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "\u05d9\u05d5\u05dd \u05e8\u05d0\u05e9\u05d5\u05df", // Sunday
                    "\u05d9\u05d5\u05dd \u05e9\u05e0\u05d9", // Monday
                    "\u05d9\u05d5\u05dd \u05e9\u05dc\u05d9\u05e9\u05d9", // Tuesday
                    "\u05d9\u05d5\u05dd \u05e8\u05d1\u05d9\u05e2\u05d9", // Wednesday
                    "\u05d9\u05d5\u05dd \u05d7\u05de\u05d9\u05e9\u05d9", // Thursday
                    "\u05d9\u05d5\u05dd \u05e9\u05d9\u05e9\u05d9", // Friday
                    "\u05e9\u05d1\u05ea" // Saturday
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "\u05d0", // abb Sunday
                    "\u05d1", // abb Monday
                    "\u05d2", // abb Tuesday
                    "\u05d3", // abb Wednesday
                    "\u05d4", // abb Thursday
                    "\u05d5", // abb Friday
                    "\u05e9" // abb Saturday
                }
            },
            { "DayNarrows",
                new String[] {
                    "\u05d0",
                    "\u05d1",
                    "\u05d2",
                    "\u05d3",
                    "\u05d4",
                    "\u05d5",
                    "\u05e9",
                }
            },
            { "standalone.DayNarrows",
                new String[] {
                    "\u05d0",
                    "\u05d1",
                    "\u05d2",
                    "\u05d3",
                    "\u05d4",
                    "\u05d5",
                    "\u05e9",
                }
            },
            { "Eras",
                new String[] { // era strings
                    "\u05dc\u05e1\u05d4\"\u05e0",
                    "\u05dc\u05e4\u05e1\u05d4\"\u05e0"
                }
            },
            { "short.Eras",
                new String[] {
                    "\u05dc\u05e4\u05e0\u05d4\u05f4\u05e1",
                    "\u05dc\u05e1\u05d4\u05f4\u05e0",
                }
            },
            { "TimePatterns",
                new String[] {
                    "HH:mm:ss z", // full time pattern
                    "HH:mm:ss z", // long time pattern
                    "HH:mm:ss", // medium time pattern
                    "HH:mm", // short time pattern
                }
            },
            { "DatePatterns",
                new String[] {
                    "EEEE d MMMM yyyy", // full date pattern
                    "d MMMM yyyy", // long date pattern
                    "dd/MM/yyyy", // medium date pattern
                    "dd/MM/yy", // short date pattern
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{0} {1}" // date-time pattern
                }
            },
            { "DateTimePatternChars", "GanjkHmsSEDFwWxhKzZ" },
        };
    }
}
