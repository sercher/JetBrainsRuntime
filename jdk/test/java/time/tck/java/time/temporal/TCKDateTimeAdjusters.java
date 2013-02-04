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
 * Copyright (c) 2007-2012, Stephen Colebourne & Michael Nascimento Santos
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
package tck.java.time.temporal;

import java.time.temporal.*;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Adjusters.
 */
@Test
public class TCKDateTimeAdjusters {

    //-----------------------------------------------------------------------
    // firstDayOfMonth()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_firstDayOfMonth() {
        assertNotNull(Adjusters.firstDayOfMonth());
    }

    @Test(groups={"tck"})
    public void test_firstDayOfMonth_nonLeap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                LocalDate date = date(2007, month, i);
                LocalDate test = (LocalDate) Adjusters.firstDayOfMonth().adjustInto(date);
                assertEquals(test.getYear(), 2007);
                assertEquals(test.getMonth(), month);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test(groups={"tck"})
    public void test_firstDayOfMonth_leap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                LocalDate date = date(2008, month, i);
                LocalDate test = (LocalDate) Adjusters.firstDayOfMonth().adjustInto(date);
                assertEquals(test.getYear(), 2008);
                assertEquals(test.getMonth(), month);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    //-----------------------------------------------------------------------
    // lastDayOfMonth()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_lastDayOfMonth() {
        assertNotNull(Adjusters.lastDayOfMonth());
    }

    @Test(groups={"tck"})
    public void test_lastDayOfMonth_nonLeap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                LocalDate date = date(2007, month, i);
                LocalDate test = (LocalDate) Adjusters.lastDayOfMonth().adjustInto(date);
                assertEquals(test.getYear(), 2007);
                assertEquals(test.getMonth(), month);
                assertEquals(test.getDayOfMonth(), month.length(false));
            }
        }
    }

    @Test(groups={"tck"})
    public void test_lastDayOfMonth_leap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                LocalDate date = date(2008, month, i);
                LocalDate test = (LocalDate) Adjusters.lastDayOfMonth().adjustInto(date);
                assertEquals(test.getYear(), 2008);
                assertEquals(test.getMonth(), month);
                assertEquals(test.getDayOfMonth(), month.length(true));
            }
        }
    }

    //-----------------------------------------------------------------------
    // firstDayOfNextMonth()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_firstDayOfNextMonth() {
        assertNotNull(Adjusters.firstDayOfNextMonth());
    }

    @Test(groups={"tck"})
    public void test_firstDayOfNextMonth_nonLeap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                LocalDate date = date(2007, month, i);
                LocalDate test = (LocalDate) Adjusters.firstDayOfNextMonth().adjustInto(date);
                assertEquals(test.getYear(), month == DECEMBER ? 2008 : 2007);
                assertEquals(test.getMonth(), month.plus(1));
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test(groups={"tck"})
    public void test_firstDayOfNextMonth_leap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                LocalDate date = date(2008, month, i);
                LocalDate test = (LocalDate) Adjusters.firstDayOfNextMonth().adjustInto(date);
                assertEquals(test.getYear(), month == DECEMBER ? 2009 : 2008);
                assertEquals(test.getMonth(), month.plus(1));
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    //-----------------------------------------------------------------------
    // firstDayOfYear()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_firstDayOfYear() {
        assertNotNull(Adjusters.firstDayOfYear());
    }

    @Test(groups={"tck"})
    public void test_firstDayOfYear_nonLeap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                LocalDate date = date(2007, month, i);
                LocalDate test = (LocalDate) Adjusters.firstDayOfYear().adjustInto(date);
                assertEquals(test.getYear(), 2007);
                assertEquals(test.getMonth(), Month.JANUARY);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test(groups={"tck"})
    public void test_firstDayOfYear_leap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                LocalDate date = date(2008, month, i);
                LocalDate test = (LocalDate) Adjusters.firstDayOfYear().adjustInto(date);
                assertEquals(test.getYear(), 2008);
                assertEquals(test.getMonth(), Month.JANUARY);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    //-----------------------------------------------------------------------
    // lastDayOfYear()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_lastDayOfYear() {
        assertNotNull(Adjusters.lastDayOfYear());
    }

    @Test(groups={"tck"})
    public void test_lastDayOfYear_nonLeap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                LocalDate date = date(2007, month, i);
                LocalDate test = (LocalDate) Adjusters.lastDayOfYear().adjustInto(date);
                assertEquals(test.getYear(), 2007);
                assertEquals(test.getMonth(), Month.DECEMBER);
                assertEquals(test.getDayOfMonth(), 31);
            }
        }
    }

    @Test(groups={"tck"})
    public void test_lastDayOfYear_leap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                LocalDate date = date(2008, month, i);
                LocalDate test = (LocalDate) Adjusters.lastDayOfYear().adjustInto(date);
                assertEquals(test.getYear(), 2008);
                assertEquals(test.getMonth(), Month.DECEMBER);
                assertEquals(test.getDayOfMonth(), 31);
            }
        }
    }

    //-----------------------------------------------------------------------
    // firstDayOfNextYear()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_firstDayOfNextYear() {
        assertNotNull(Adjusters.firstDayOfNextYear());
    }

    @Test(groups={"tck"})
    public void test_firstDayOfNextYear_nonLeap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                LocalDate date = date(2007, month, i);
                LocalDate test = (LocalDate) Adjusters.firstDayOfNextYear().adjustInto(date);
                assertEquals(test.getYear(), 2008);
                assertEquals(test.getMonth(), JANUARY);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    @Test(groups={"tck"})
    public void test_firstDayOfNextYear_leap() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(true); i++) {
                LocalDate date = date(2008, month, i);
                LocalDate test = (LocalDate) Adjusters.firstDayOfNextYear().adjustInto(date);
                assertEquals(test.getYear(), 2009);
                assertEquals(test.getMonth(), JANUARY);
                assertEquals(test.getDayOfMonth(), 1);
            }
        }
    }

    //-----------------------------------------------------------------------
    // dayOfWeekInMonth()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_dayOfWeekInMonth() {
        assertNotNull(Adjusters.dayOfWeekInMonth(1, MONDAY));
    }

    @Test(expectedExceptions=NullPointerException.class, groups={"tck"})
    public void factory_dayOfWeekInMonth_nullDayOfWeek() {
        Adjusters.dayOfWeekInMonth(1, null);
    }

    @DataProvider(name = "dayOfWeekInMonth_positive")
    Object[][] data_dayOfWeekInMonth_positive() {
        return new Object[][] {
            {2011, 1, TUESDAY, date(2011, 1, 4)},
            {2011, 2, TUESDAY, date(2011, 2, 1)},
            {2011, 3, TUESDAY, date(2011, 3, 1)},
            {2011, 4, TUESDAY, date(2011, 4, 5)},
            {2011, 5, TUESDAY, date(2011, 5, 3)},
            {2011, 6, TUESDAY, date(2011, 6, 7)},
            {2011, 7, TUESDAY, date(2011, 7, 5)},
            {2011, 8, TUESDAY, date(2011, 8, 2)},
            {2011, 9, TUESDAY, date(2011, 9, 6)},
            {2011, 10, TUESDAY, date(2011, 10, 4)},
            {2011, 11, TUESDAY, date(2011, 11, 1)},
            {2011, 12, TUESDAY, date(2011, 12, 6)},
        };
    }

    @Test(groups={"tck"}, dataProvider = "dayOfWeekInMonth_positive")
    public void test_dayOfWeekInMonth_positive(int year, int month, DayOfWeek dow, LocalDate expected) {
        for (int ordinal = 1; ordinal <= 5; ordinal++) {
            for (int day = 1; day <= Month.of(month).length(false); day++) {
                LocalDate date = date(year, month, day);
                LocalDate test = (LocalDate) Adjusters.dayOfWeekInMonth(ordinal, dow).adjustInto(date);
                assertEquals(test, expected.plusWeeks(ordinal - 1));
            }
        }
    }

    @DataProvider(name = "dayOfWeekInMonth_zero")
    Object[][] data_dayOfWeekInMonth_zero() {
        return new Object[][] {
            {2011, 1, TUESDAY, date(2010, 12, 28)},
            {2011, 2, TUESDAY, date(2011, 1, 25)},
            {2011, 3, TUESDAY, date(2011, 2, 22)},
            {2011, 4, TUESDAY, date(2011, 3, 29)},
            {2011, 5, TUESDAY, date(2011, 4, 26)},
            {2011, 6, TUESDAY, date(2011, 5, 31)},
            {2011, 7, TUESDAY, date(2011, 6, 28)},
            {2011, 8, TUESDAY, date(2011, 7, 26)},
            {2011, 9, TUESDAY, date(2011, 8, 30)},
            {2011, 10, TUESDAY, date(2011, 9, 27)},
            {2011, 11, TUESDAY, date(2011, 10, 25)},
            {2011, 12, TUESDAY, date(2011, 11, 29)},
        };
    }

    @Test(groups={"tck"}, dataProvider = "dayOfWeekInMonth_zero")
    public void test_dayOfWeekInMonth_zero(int year, int month, DayOfWeek dow, LocalDate expected) {
        for (int day = 1; day <= Month.of(month).length(false); day++) {
            LocalDate date = date(year, month, day);
            LocalDate test = (LocalDate) Adjusters.dayOfWeekInMonth(0, dow).adjustInto(date);
            assertEquals(test, expected);
        }
    }

    @DataProvider(name = "dayOfWeekInMonth_negative")
    Object[][] data_dayOfWeekInMonth_negative() {
        return new Object[][] {
            {2011, 1, TUESDAY, date(2011, 1, 25)},
            {2011, 2, TUESDAY, date(2011, 2, 22)},
            {2011, 3, TUESDAY, date(2011, 3, 29)},
            {2011, 4, TUESDAY, date(2011, 4, 26)},
            {2011, 5, TUESDAY, date(2011, 5, 31)},
            {2011, 6, TUESDAY, date(2011, 6, 28)},
            {2011, 7, TUESDAY, date(2011, 7, 26)},
            {2011, 8, TUESDAY, date(2011, 8, 30)},
            {2011, 9, TUESDAY, date(2011, 9, 27)},
            {2011, 10, TUESDAY, date(2011, 10, 25)},
            {2011, 11, TUESDAY, date(2011, 11, 29)},
            {2011, 12, TUESDAY, date(2011, 12, 27)},
        };
    }

    @Test(groups={"tck"}, dataProvider = "dayOfWeekInMonth_negative")
    public void test_dayOfWeekInMonth_negative(int year, int month, DayOfWeek dow, LocalDate expected) {
        for (int ordinal = 0; ordinal < 5; ordinal++) {
            for (int day = 1; day <= Month.of(month).length(false); day++) {
                LocalDate date = date(year, month, day);
                LocalDate test = (LocalDate) Adjusters.dayOfWeekInMonth(-1 - ordinal, dow).adjustInto(date);
                assertEquals(test, expected.minusWeeks(ordinal));
            }
        }
    }

    //-----------------------------------------------------------------------
    // firstInMonth()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_firstInMonth() {
        assertNotNull(Adjusters.firstInMonth(MONDAY));
    }

    @Test(expectedExceptions=NullPointerException.class, groups={"tck"})
    public void factory_firstInMonth_nullDayOfWeek() {
        Adjusters.firstInMonth(null);
    }

    @Test(groups={"tck"}, dataProvider = "dayOfWeekInMonth_positive")
    public void test_firstInMonth(int year, int month, DayOfWeek dow, LocalDate expected) {
        for (int day = 1; day <= Month.of(month).length(false); day++) {
            LocalDate date = date(year, month, day);
            LocalDate test = (LocalDate) Adjusters.firstInMonth(dow).adjustInto(date);
            assertEquals(test, expected, "day-of-month=" + day);
        }
    }

    //-----------------------------------------------------------------------
    // lastInMonth()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_lastInMonth() {
        assertNotNull(Adjusters.lastInMonth(MONDAY));
    }

    @Test(expectedExceptions=NullPointerException.class, groups={"tck"})
    public void factory_lastInMonth_nullDayOfWeek() {
        Adjusters.lastInMonth(null);
    }

    @Test(groups={"tck"}, dataProvider = "dayOfWeekInMonth_negative")
    public void test_lastInMonth(int year, int month, DayOfWeek dow, LocalDate expected) {
        for (int day = 1; day <= Month.of(month).length(false); day++) {
            LocalDate date = date(year, month, day);
            LocalDate test = (LocalDate) Adjusters.lastInMonth(dow).adjustInto(date);
            assertEquals(test, expected, "day-of-month=" + day);
        }
    }

    //-----------------------------------------------------------------------
    // next()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_next() {
        assertNotNull(Adjusters.next(MONDAY));
    }

    @Test(expectedExceptions = NullPointerException.class, groups={"tck"})
    public void factory_next_nullDayOfWeek() {
        Adjusters.next(null);
    }

    @Test(groups={"tck"})
    public void test_next() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                LocalDate date = date(2007, month, i);

                for (DayOfWeek dow : DayOfWeek.values()) {
                    LocalDate test = (LocalDate) Adjusters.next(dow).adjustInto(date);

                    assertSame(test.getDayOfWeek(), dow, date + " " + test);

                    if (test.getYear() == 2007) {
                        int dayDiff = test.getDayOfYear() - date.getDayOfYear();
                        assertTrue(dayDiff > 0 && dayDiff < 8);
                    } else {
                        assertSame(month, Month.DECEMBER);
                        assertTrue(date.getDayOfMonth() > 24);
                        assertEquals(test.getYear(), 2008);
                        assertSame(test.getMonth(), Month.JANUARY);
                        assertTrue(test.getDayOfMonth() < 8);
                    }
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // nextOrSame()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_nextOrCurrent() {
        assertNotNull(Adjusters.nextOrSame(MONDAY));
    }

    @Test(expectedExceptions = NullPointerException.class, groups={"tck"})
    public void factory_nextOrCurrent_nullDayOfWeek() {
        Adjusters.nextOrSame(null);
    }

    @Test(groups={"tck"})
    public void test_nextOrCurrent() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                LocalDate date = date(2007, month, i);

                for (DayOfWeek dow : DayOfWeek.values()) {
                    LocalDate test = (LocalDate) Adjusters.nextOrSame(dow).adjustInto(date);

                    assertSame(test.getDayOfWeek(), dow);

                    if (test.getYear() == 2007) {
                        int dayDiff = test.getDayOfYear() - date.getDayOfYear();
                        assertTrue(dayDiff < 8);
                        assertEquals(date.equals(test), date.getDayOfWeek() == dow);
                    } else {
                        assertFalse(date.getDayOfWeek() == dow);
                        assertSame(month, Month.DECEMBER);
                        assertTrue(date.getDayOfMonth() > 24);
                        assertEquals(test.getYear(), 2008);
                        assertSame(test.getMonth(), Month.JANUARY);
                        assertTrue(test.getDayOfMonth() < 8);
                    }
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // previous()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_previous() {
        assertNotNull(Adjusters.previous(MONDAY));
    }

    @Test(expectedExceptions = NullPointerException.class, groups={"tck"})
    public void factory_previous_nullDayOfWeek() {
        Adjusters.previous(null);
    }

    @Test(groups={"tck"})
    public void test_previous() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                LocalDate date = date(2007, month, i);

                for (DayOfWeek dow : DayOfWeek.values()) {
                    LocalDate test = (LocalDate) Adjusters.previous(dow).adjustInto(date);

                    assertSame(test.getDayOfWeek(), dow, date + " " + test);

                    if (test.getYear() == 2007) {
                        int dayDiff = test.getDayOfYear() - date.getDayOfYear();
                        assertTrue(dayDiff < 0 && dayDiff > -8, dayDiff + " " + test);
                    } else {
                        assertSame(month, Month.JANUARY);
                        assertTrue(date.getDayOfMonth() < 8);
                        assertEquals(test.getYear(), 2006);
                        assertSame(test.getMonth(), Month.DECEMBER);
                        assertTrue(test.getDayOfMonth() > 24);
                    }
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // previousOrSame()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_previousOrCurrent() {
        assertNotNull(Adjusters.previousOrSame(MONDAY));
    }

    @Test(expectedExceptions = NullPointerException.class, groups={"tck"})
    public void factory_previousOrCurrent_nullDayOfWeek() {
        Adjusters.previousOrSame(null);
    }

    @Test(groups={"tck"})
    public void test_previousOrCurrent() {
        for (Month month : Month.values()) {
            for (int i = 1; i <= month.length(false); i++) {
                LocalDate date = date(2007, month, i);

                for (DayOfWeek dow : DayOfWeek.values()) {
                    LocalDate test = (LocalDate) Adjusters.previousOrSame(dow).adjustInto(date);

                    assertSame(test.getDayOfWeek(), dow);

                    if (test.getYear() == 2007) {
                        int dayDiff = test.getDayOfYear() - date.getDayOfYear();
                        assertTrue(dayDiff <= 0 && dayDiff > -7);
                        assertEquals(date.equals(test), date.getDayOfWeek() == dow);
                    } else {
                        assertFalse(date.getDayOfWeek() == dow);
                        assertSame(month, Month.JANUARY);
                        assertTrue(date.getDayOfMonth() < 7);
                        assertEquals(test.getYear(), 2006);
                        assertSame(test.getMonth(), Month.DECEMBER);
                        assertTrue(test.getDayOfMonth() > 25);
                    }
                }
            }
        }
    }

    private LocalDate date(int year, Month month, int day) {
        return LocalDate.of(year, month, day);
    }

    private LocalDate date(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

}
