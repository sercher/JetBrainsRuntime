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
 * Copyright (c) 2012, Stephen Colebourne & Michael Nascimento Santos
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
import java.util.Set;

import java.time.temporal.Chrono;
import java.time.temporal.ChronoField;
import java.time.calendar.HijrahChrono;
import java.time.calendar.JapaneseChrono;
import java.time.calendar.MinguoChrono;
import java.time.calendar.ThaiBuddhistChrono;
import java.time.temporal.ChronoLocalDate;
import java.time.temporal.ISOChrono;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Chrono class.
 */
@Test
public class TestChrono {

    @BeforeMethod(groups="tck")
    public void setUp() {
        // Ensure each of the classes are initialized (until initialization is fixed)
        Chrono<?> c;
        c = HijrahChrono.INSTANCE;
        c = ISOChrono.INSTANCE;
        c = JapaneseChrono.INSTANCE;
        c = MinguoChrono.INSTANCE;
        c = ThaiBuddhistChrono.INSTANCE;
        c.toString();  // avoids variable being marked as unused
    }

    //-----------------------------------------------------------------------
    // regular data factory for names and descriptions of available calendars
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendars")
    Object[][] data_of_calendars() {
        return new Object[][] {
                    {"Hijrah", "islamicc", "Hijrah calendar"},
                    {"ISO", "iso8601", "ISO calendar"},
                    {"Japanese", "japanese", "Japanese calendar"},
                    {"Minguo", "roc", "Minguo Calendar"},
                    {"ThaiBuddhist", "buddhist", "ThaiBuddhist calendar"},
                };
    }

    @Test(dataProvider = "calendars")
    public void test_getters(String chronoId, String calendarSystemType, String description) {
        Chrono<?> chrono = Chrono.of(chronoId);
        assertNotNull(chrono, "Required calendar not found by ID: " + chronoId);
        assertEquals(chrono.getId(), chronoId);
        assertEquals(chrono.getCalendarType(), calendarSystemType);
    }

    @Test(dataProvider = "calendars")
    public void test_required_calendars(String chronoId, String calendarSystemType, String description) {
        Chrono<?> chrono = Chrono.of(chronoId);
        assertNotNull(chrono, "Required calendar not found by ID: " + chronoId);
        chrono = Chrono.of(calendarSystemType);
        assertNotNull(chrono, "Required calendar not found by type: " + chronoId);
        Set<Chrono<?>> cals = Chrono.getAvailableChronologies();
        assertTrue(cals.contains(chrono), "Required calendar not found in set of available calendars");
    }

    @Test(groups="tck")
    public void test_calendar_list() {
        Set<Chrono<?>> chronos = Chrono.getAvailableChronologies();
        assertNotNull(chronos, "Required list of calendars must be non-null");
        for (Chrono<?> chrono : chronos) {
            Chrono<?> lookup = Chrono.of(chrono.getId());
            assertNotNull(lookup, "Required calendar not found: " + chrono);
        }
        assertEquals(chronos.size() >= data_of_calendars().length, true, "Chrono.getAvailableChronologies().size = " + chronos.size()
                + ", expected >= " + data_of_calendars().length);
    }

    /**
     * Compute the number of days from the Epoch and compute the date from the number of days.
     */
    @Test(dataProvider = "calendars", groups="tck")
    public void test_epoch(String name, String alias, String description) {
        Chrono<?> chrono = Chrono.of(name); // a chronology. In practice this is rarely hardcoded
        ChronoLocalDate<?> date1 = chrono.dateNow();
        long epoch1 = date1.getLong(ChronoField.EPOCH_DAY);
        ChronoLocalDate<?> date2 = date1.with(ChronoField.EPOCH_DAY, epoch1);
        assertEquals(date1, date2, "Date from epoch day is not same date: " + date1 + " != " + date2);
        long epoch2 = date1.getLong(ChronoField.EPOCH_DAY);
        assertEquals(epoch1, epoch2, "Epoch day not the same: " + epoch1 + " != " + epoch2);
    }

    //-----------------------------------------------------------------------
    // locale based lookup
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendarsystemtype")
    Object[][] data_CalendarType() {
        return new Object[][] {
            {HijrahChrono.INSTANCE, "islamicc"},
            {ISOChrono.INSTANCE, "iso8601"},
            {JapaneseChrono.INSTANCE, "japanese"},
            {MinguoChrono.INSTANCE, "roc"},
            {ThaiBuddhistChrono.INSTANCE, "buddhist"},
        };
    }

    @Test(dataProvider = "calendarsystemtype", groups="tck")
    public void test_getCalendarType(Chrono<?> chrono, String calendarType) {
        assertEquals(chrono.getCalendarType(), calendarType);
    }

    @Test(dataProvider = "calendarsystemtype", groups="tck")
    public void test_lookupLocale(Chrono<?> chrono, String calendarType) {
        Locale locale = new Locale.Builder().setLanguage("en").setRegion("CA").setUnicodeLocaleKeyword("ca", calendarType).build();
        assertEquals(Chrono.ofLocale(locale), chrono);
    }


    //-----------------------------------------------------------------------
    // serialization; serialize and check each calendar system
    //-----------------------------------------------------------------------
    @Test(groups={"implementation"}, dataProvider = "calendarsystemtype")
    public <C extends Chrono<C>> void test_chronoSerializationSingleton(C chrono, String calendarType) throws Exception {
        C orginal = chrono;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(orginal);
        out.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        @SuppressWarnings("unchecked")
        C ser = (C) in.readObject();
        assertSame(ser, chrono, "Deserialized Chrono is not the singleton serialized");
    }

}
