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

package sun.text.resources.pt;

import java.util.ListResourceBundle;

public class FormatData_pt extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "Janeiro", // january
                    "Fevereiro", // february
                    "Mar\u00e7o", // march
                    "Abril", // april
                    "Maio", // may
                    "Junho", // june
                    "Julho", // july
                    "Agosto", // august
                    "Setembro", // september
                    "Outubro", // october
                    "Novembro", // november
                    "Dezembro", // december
                    "" // month 13 if applicable
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "Jan", // abb january
                    "Fev", // abb february
                    "Mar", // abb march
                    "Abr", // abb april
                    "Mai", // abb may
                    "Jun", // abb june
                    "Jul", // abb july
                    "Ago", // abb august
                    "Set", // abb september
                    "Out", // abb october
                    "Nov", // abb november
                    "Dez", // abb december
                    "" // abb month 13 if applicable
                }
            },
            { "DayNames",
                new String[] {
                    "Domingo", // Sunday
                    "Segunda-feira", // Monday
                    "Ter\u00e7a-feira", // Tuesday
                    "Quarta-feira", // Wednesday
                    "Quinta-feira", // Thursday
                    "Sexta-feira", // Friday
                    "S\u00e1bado" // Saturday
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "Dom", // abb Sunday
                    "Seg", // abb Monday
                    "Ter", // abb Tuesday
                    "Qua", // abb Wednesday
                    "Qui", // abb Thursday
                    "Sex", // abb Friday
                    "S\u00e1b" // abb Saturday
                }
            },
            { "DayNarrows",
                new String[] {
                    "D",
                    "S",
                    "T",
                    "Q",
                    "Q",
                    "S",
                    "S",
                }
            },
            { "NumberElements",
                new String[] {
                    ",", // decimal al separator
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
                    "HH'H'mm'm' z", // full time pattern
                    "H:mm:ss z", // long time pattern
                    "H:mm:ss", // medium time pattern
                    "H:mm", // short time pattern
                }
            },
            { "DatePatterns",
                new String[] {
                    "EEEE, d' de 'MMMM' de 'yyyy", // full date pattern
                    "d' de 'MMMM' de 'yyyy", // long date pattern
                    "d/MMM/yyyy", // medium date pattern
                    "dd-MM-yyyy", // short date pattern
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}" // date-time pattern
                }
            },
            { "DateTimePatternChars", "GyMdkHmsSEDFwWahKzZ" },
            { "cldr.japanese.DatePatterns",
                new String[] {
                    "EEEE, G y MMMM dd",
                    "G y MMMM d",
                    "G y MMM d",
                    "d/M/yy",
                }
            },
            { "cldr.roc.DatePatterns",
                new String[] {
                    "EEEE, d 'de' MMMM 'de' y G",
                    "d 'de' MMMM 'de' y G",
                    "dd/MM/yyyy G",
                    "d/M/yyyy",
                }
            },
            { "roc.DatePatterns",
                new String[] {
                    "EEEE, d 'de' MMMM 'de' y GGGG",
                    "d 'de' MMMM 'de' y GGGG",
                    "dd/MM/yyyy GGGG",
                    "d/M/yyyy",
                }
            },
            { "cldr.islamic.DatePatterns",
                new String[] {
                    "EEEE, d 'de' MMMM 'de' y G",
                    "d 'de' MMMM 'de' y G",
                    "dd/MM/yyyy G",
                    "d/M/yyyy",
                }
            },
            { "islamic.DatePatterns",
                new String[] {
                    "EEEE, d 'de' MMMM 'de' y GGGG",
                    "d 'de' MMMM 'de' y GGGG",
                    "dd/MM/yyyy GGGG",
                    "d/M/yyyy",
                }
            },
            { "calendarname.islamic-civil", "Calend\u00e1rio Civil Isl\u00e2mico" },
            { "calendarname.islamicc", "Calend\u00e1rio Civil Isl\u00e2mico" },
            { "calendarname.islamic", "Calend\u00e1rio Isl\u00e2mico" },
            { "calendarname.japanese", "Calend\u00e1rio Japon\u00eas" },
            { "calendarname.gregorian", "Calend\u00e1rio Gregoriano" },
            { "calendarname.gregory", "Calend\u00e1rio Gregoriano" },
            { "calendarname.roc", "Calend\u00e1rio da Rep\u00fablica da China" },
            { "calendarname.buddhist", "Calend\u00e1rio Budista" },
            { "field.era", "Era" },
            { "field.year", "Ano" },
            { "field.month", "M\u00eas" },
            { "field.week", "Semana" },
            { "field.weekday", "Dia da semana" },
            { "field.dayperiod", "Per\u00edodo do dia" },
            { "field.hour", "Hora" },
            { "field.minute", "Minuto" },
            { "field.second", "Segundo" },
            { "field.zone", "Fuso" },
        };
    }
}
