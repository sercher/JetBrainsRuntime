/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2008-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package test.java.time.format;

import java.time.format.*;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.TemporalField;
import test.java.time.temporal.MockFieldValue;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test TextPrinterParser.
 */
@Test(groups={"implementation"})
public class TestTextPrinter extends AbstractTestPrinterParser {

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=DateTimeException.class)
    public void test_print_emptyCalendrical() throws Exception {
        getFormatter(DAY_OF_WEEK, TextStyle.FULL).printTo(EMPTY_DTA, buf);
    }

    public void test_print_append() throws Exception {
        buf.append("EXISTING");
        getFormatter(DAY_OF_WEEK, TextStyle.FULL).printTo(LocalDate.of(2012, 4, 18), buf);
        assertEquals(buf.toString(), "EXISTINGWednesday");
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="print")
    Object[][] provider_dow() {
        return new Object[][] {
            {DAY_OF_WEEK, TextStyle.FULL, 1, "Monday"},
            {DAY_OF_WEEK, TextStyle.FULL, 2, "Tuesday"},
            {DAY_OF_WEEK, TextStyle.FULL, 3, "Wednesday"},
            {DAY_OF_WEEK, TextStyle.FULL, 4, "Thursday"},
            {DAY_OF_WEEK, TextStyle.FULL, 5, "Friday"},
            {DAY_OF_WEEK, TextStyle.FULL, 6, "Saturday"},
            {DAY_OF_WEEK, TextStyle.FULL, 7, "Sunday"},

            {DAY_OF_WEEK, TextStyle.SHORT, 1, "Mon"},
            {DAY_OF_WEEK, TextStyle.SHORT, 2, "Tue"},
            {DAY_OF_WEEK, TextStyle.SHORT, 3, "Wed"},
            {DAY_OF_WEEK, TextStyle.SHORT, 4, "Thu"},
            {DAY_OF_WEEK, TextStyle.SHORT, 5, "Fri"},
            {DAY_OF_WEEK, TextStyle.SHORT, 6, "Sat"},
            {DAY_OF_WEEK, TextStyle.SHORT, 7, "Sun"},

            {DAY_OF_WEEK, TextStyle.NARROW, 1, "M"},
            {DAY_OF_WEEK, TextStyle.NARROW, 2, "T"},
            {DAY_OF_WEEK, TextStyle.NARROW, 3, "W"},
            {DAY_OF_WEEK, TextStyle.NARROW, 4, "T"},
            {DAY_OF_WEEK, TextStyle.NARROW, 5, "F"},
            {DAY_OF_WEEK, TextStyle.NARROW, 6, "S"},
            {DAY_OF_WEEK, TextStyle.NARROW, 7, "S"},

            {DAY_OF_MONTH, TextStyle.FULL, 1, "1"},
            {DAY_OF_MONTH, TextStyle.FULL, 2, "2"},
            {DAY_OF_MONTH, TextStyle.FULL, 3, "3"},
            {DAY_OF_MONTH, TextStyle.FULL, 28, "28"},
            {DAY_OF_MONTH, TextStyle.FULL, 29, "29"},
            {DAY_OF_MONTH, TextStyle.FULL, 30, "30"},
            {DAY_OF_MONTH, TextStyle.FULL, 31, "31"},

            {DAY_OF_MONTH, TextStyle.SHORT, 1, "1"},
            {DAY_OF_MONTH, TextStyle.SHORT, 2, "2"},
            {DAY_OF_MONTH, TextStyle.SHORT, 3, "3"},
            {DAY_OF_MONTH, TextStyle.SHORT, 28, "28"},
            {DAY_OF_MONTH, TextStyle.SHORT, 29, "29"},
            {DAY_OF_MONTH, TextStyle.SHORT, 30, "30"},
            {DAY_OF_MONTH, TextStyle.SHORT, 31, "31"},

            {MONTH_OF_YEAR, TextStyle.FULL, 1, "January"},
            {MONTH_OF_YEAR, TextStyle.FULL, 2, "February"},
            {MONTH_OF_YEAR, TextStyle.FULL, 3, "March"},
            {MONTH_OF_YEAR, TextStyle.FULL, 4, "April"},
            {MONTH_OF_YEAR, TextStyle.FULL, 5, "May"},
            {MONTH_OF_YEAR, TextStyle.FULL, 6, "June"},
            {MONTH_OF_YEAR, TextStyle.FULL, 7, "July"},
            {MONTH_OF_YEAR, TextStyle.FULL, 8, "August"},
            {MONTH_OF_YEAR, TextStyle.FULL, 9, "September"},
            {MONTH_OF_YEAR, TextStyle.FULL, 10, "October"},
            {MONTH_OF_YEAR, TextStyle.FULL, 11, "November"},
            {MONTH_OF_YEAR, TextStyle.FULL, 12, "December"},

            {MONTH_OF_YEAR, TextStyle.SHORT, 1, "Jan"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 2, "Feb"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 3, "Mar"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 4, "Apr"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 5, "May"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 6, "Jun"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 7, "Jul"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 8, "Aug"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 9, "Sep"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 10, "Oct"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 11, "Nov"},
            {MONTH_OF_YEAR, TextStyle.SHORT, 12, "Dec"},
       };
    }

    @Test(dataProvider="print")
    public void test_print(TemporalField field, TextStyle style, int value, String expected) throws Exception {
        getFormatter(field, style).printTo(new MockFieldValue(field, value), buf);
        assertEquals(buf.toString(), expected);
    }

    //-----------------------------------------------------------------------
    public void test_print_french_long() throws Exception {
        getFormatter(MONTH_OF_YEAR, TextStyle.FULL).withLocale(Locale.FRENCH).printTo(LocalDate.of(2012, 1, 1), buf);
        assertEquals(buf.toString(), "janvier");
    }

    public void test_print_french_short() throws Exception {
        getFormatter(MONTH_OF_YEAR, TextStyle.SHORT).withLocale(Locale.FRENCH).printTo(LocalDate.of(2012, 1, 1), buf);
        assertEquals(buf.toString(), "janv.");
    }

    //-----------------------------------------------------------------------
    public void test_toString1() throws Exception {
        assertEquals(getFormatter(MONTH_OF_YEAR, TextStyle.FULL).toString(), "Text(MonthOfYear)");
    }

    public void test_toString2() throws Exception {
        assertEquals(getFormatter(MONTH_OF_YEAR, TextStyle.SHORT).toString(), "Text(MonthOfYear,SHORT)");
    }

}
