/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright IBM Corp. 1996 - 1999 - All Rights Reserved
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

package sun.text.resources.zh;

import java.util.ListResourceBundle;

public class FormatData_zh extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "\u4e00\u6708", // january
                    "\u4e8c\u6708", // february
                    "\u4e09\u6708", // march
                    "\u56db\u6708", // april
                    "\u4e94\u6708", // may
                    "\u516d\u6708", // june
                    "\u4e03\u6708", // july
                    "\u516b\u6708", // august
                    "\u4e5d\u6708", // september
                    "\u5341\u6708", // october
                    "\u5341\u4e00\u6708", // november
                    "\u5341\u4e8c\u6708", // december
                    "" // month 13 if applicable
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "\u4e00\u6708", // abb january
                    "\u4e8c\u6708", // abb february
                    "\u4e09\u6708", // abb march
                    "\u56db\u6708", // abb april
                    "\u4e94\u6708", // abb may
                    "\u516d\u6708", // abb june
                    "\u4e03\u6708", // abb july
                    "\u516b\u6708", // abb august
                    "\u4e5d\u6708", // abb september
                    "\u5341\u6708", // abb october
                    "\u5341\u4e00\u6708", // abb november
                    "\u5341\u4e8c\u6708", // abb december
                    "" // abb month 13 if applicable
                }
            },
            { "standalone.MonthNarrows",
                new String[] {
                    "1\u6708",
                    "2\u6708",
                    "3\u6708",
                    "4\u6708",
                    "5\u6708",
                    "6\u6708",
                    "7\u6708",
                    "8\u6708",
                    "9\u6708",
                    "10\u6708",
                    "11\u6708",
                    "12\u6708",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "\u661f\u671f\u65e5", // Sunday
                    "\u661f\u671f\u4e00", // Monday
                    "\u661f\u671f\u4e8c", // Tuesday
                    "\u661f\u671f\u4e09", // Wednesday
                    "\u661f\u671f\u56db", // Thursday
                    "\u661f\u671f\u4e94", // Friday
                    "\u661f\u671f\u516d" // Saturday
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "\u661f\u671f\u65e5", // abb Sunday
                    "\u661f\u671f\u4e00", // abb Monday
                    "\u661f\u671f\u4e8c", // abb Tuesday
                    "\u661f\u671f\u4e09", // abb Wednesday
                    "\u661f\u671f\u56db", // abb Thursday
                    "\u661f\u671f\u4e94", // abb Friday
                    "\u661f\u671f\u516d" // abb Saturday
                }
            },
            { "DayNarrows",
                new String[] {
                    "\u65e5",
                    "\u4e00",
                    "\u4e8c",
                    "\u4e09",
                    "\u56db",
                    "\u4e94",
                    "\u516d",
                }
            },
            { "AmPmMarkers",
                new String[] {
                    "\u4e0a\u5348", // am marker
                    "\u4e0b\u5348" // pm marker
                }
            },
            { "Eras",
                new String[] { // era strings
                    "\u516c\u5143\u524d",
                    "\u516c\u5143"
                }
            },
            { "TimePatterns",
                new String[] {
                    "ahh'\u65f6'mm'\u5206'ss'\u79d2' z", // full time pattern
                    "ahh'\u65f6'mm'\u5206'ss'\u79d2'", // long time pattern
                    "H:mm:ss", // medium time pattern
                    "ah:mm", // short time pattern
                }
            },
            { "DatePatterns",
                new String[] {
                    "yyyy'\u5e74'M'\u6708'd'\u65e5' EEEE", // full date pattern
                    "yyyy'\u5e74'M'\u6708'd'\u65e5'", // long date pattern
                    "yyyy-M-d", // medium date pattern
                    "yy-M-d", // short date pattern
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}" // date-time pattern
                }
            },
            { "cldr.buddhist.DatePatterns",
                new String[] {
                    "Gy\u5e74M\u6708d\u65e5EEEE",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gyyyy-M-d",
                    "Gy-M-d",
                }
            },
            { "cldr.japanese.DatePatterns",
                new String[] {
                    "Gy\u5e74M\u6708d\u65e5EEEE",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gyy-MM-dd",
                }
            },
            { "roc.Eras",
                new String[] {
                    "\u6c11\u56fd\u524d",
                    "\u6c11\u56fd",
                }
            },
            { "cldr.roc.DatePatterns",
                new String[] {
                    "Gy\u5e74M\u6708d\u65e5EEEE",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gy-M-d",
                    "Gy-M-d",
                }
            },
            { "roc.DatePatterns",
                new String[] {
                    "GGGGy\u5e74M\u6708d\u65e5EEEE",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGy-M-d",
                    "GGGGy-M-d",
                }
            },
            { "cldr.islamic.DatePatterns",
                new String[] {
                    "Gy\u5e74M\u6708d\u65e5EEEE",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gy\u5e74M\u6708d\u65e5",
                    "Gyy-MM-dd",
                }
            },
            { "islamic.DatePatterns",
                new String[] {
                    "GGGGy\u5e74M\u6708d\u65e5EEEE",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGy\u5e74M\u6708d\u65e5",
                    "GGGGyy-MM-dd",
                }
            },
            { "calendarname.islamic-civil", "\u4f0a\u65af\u5170\u5e0c\u5409\u6765\u5386" },
            { "calendarname.islamicc", "\u4f0a\u65af\u5170\u5e0c\u5409\u6765\u5386" },
            { "calendarname.islamic", "\u4f0a\u65af\u5170\u65e5\u5386" },
            { "calendarname.japanese", "\u65e5\u672c\u65e5\u5386" },
            { "calendarname.gregorian", "\u516c\u5386" },
            { "calendarname.gregory", "\u516c\u5386" },
            { "calendarname.roc", "\u6c11\u56fd\u65e5\u5386" },
            { "calendarname.buddhist", "\u4f5b\u6559\u65e5\u5386" },
            { "field.era", "\u65f6\u671f" },
            { "field.year", "\u5e74" },
            { "field.month", "\u6708" },
            { "field.week", "\u5468" },
            { "field.weekday", "\u5468\u5929" },
            { "field.dayperiod", "\u4e0a\u5348/\u4e0b\u5348" },
            { "field.hour", "\u5c0f\u65f6" },
            { "field.minute", "\u5206\u949f" },
            { "field.second", "\u79d2\u949f" },
            { "field.zone", "\u533a\u57df" },
        };
    }
}
