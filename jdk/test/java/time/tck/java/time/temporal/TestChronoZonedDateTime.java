/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package tck.java.time.temporal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Chrono;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ChronoZonedDateTime;
import java.time.temporal.SimplePeriod;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeBuilder;
import java.time.temporal.TemporalAdder;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalSubtractor;
import java.time.temporal.ValueRange;
import java.time.temporal.ISOChrono;
import java.time.temporal.TemporalUnit;
import java.time.calendar.HijrahChrono;
import java.time.calendar.JapaneseChrono;
import java.time.calendar.MinguoChrono;
import java.time.calendar.ThaiBuddhistChrono;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test assertions that must be true for all built-in chronologies.
 */
@Test
public class TestChronoZonedDateTime {

    //-----------------------------------------------------------------------
    // regular data factory for names and descriptions of available calendars
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendars")
    Chrono<?>[][] data_of_calendars() {
        return new Chrono<?>[][]{
                    {HijrahChrono.INSTANCE},
                    {ISOChrono.INSTANCE},
                    {JapaneseChrono.INSTANCE},
                    {MinguoChrono.INSTANCE},
                    {ThaiBuddhistChrono.INSTANCE},
        };
    }

    @Test(groups={"tck"}, dataProvider="calendars")
    public void test_badWithAdjusterChrono(Chrono<?> chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chrono<?>[] clist : data_of_calendars()) {
            Chrono<?> chrono2 = clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalAdjuster adjuster = new FixedAdjuster(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.with(adjuster);
                    Assert.fail("WithAdjuster should have thrown a ClassCastException, "
                            + "required: " + czdt + ", supplied: " + czdt2);
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                ChronoZonedDateTime<?> result = czdt.with(adjuster);
                assertEquals(result, czdt2, "WithAdjuster failed to replace date");
            }
        }
    }

    @Test(groups={"tck"}, dataProvider="calendars")
    public void test_badPlusAdjusterChrono(Chrono<?> chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chrono<?>[] clist : data_of_calendars()) {
            Chrono<?> chrono2 = clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalAdder adjuster = new FixedAdjuster(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.plus(adjuster);
                    Assert.fail("WithAdjuster should have thrown a ClassCastException, "
                            + "required: " + czdt + ", supplied: " + czdt2);
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                // Same chronology,
                ChronoZonedDateTime<?> result = czdt.plus(adjuster);
                assertEquals(result, czdt2, "WithAdjuster failed to replace date time");
            }
        }
    }

    @Test(groups={"tck"}, dataProvider="calendars")
    public void test_badMinusAdjusterChrono(Chrono<?> chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chrono<?>[] clist : data_of_calendars()) {
            Chrono<?> chrono2 = clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalSubtractor adjuster = new FixedAdjuster(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.minus(adjuster);
                    Assert.fail("WithAdjuster should have thrown a ClassCastException, "
                            + "required: " + czdt + ", supplied: " + czdt2);
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                // Same chronology,
                ChronoZonedDateTime<?> result = czdt.minus(adjuster);
                assertEquals(result, czdt2, "WithAdjuster failed to replace date");
            }
        }
    }

    @Test(groups={"tck"}, dataProvider="calendars")
    public void test_badPlusTemporalUnitChrono(Chrono<?> chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chrono<?>[] clist : data_of_calendars()) {
            Chrono<?> chrono2 = clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalUnit adjuster = new FixedTemporalUnit(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.plus(1, adjuster);
                    Assert.fail("TemporalUnit.doPlus plus should have thrown a ClassCastException, " + czdt
                            + " can not be cast to " + czdt2);
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                // Same chronology,
                ChronoZonedDateTime<?> result = czdt.plus(1, adjuster);
                assertEquals(result, czdt2, "WithAdjuster failed to replace date");
            }
        }
    }

    @Test(groups={"tck"}, dataProvider="calendars")
    public void test_badMinusTemporalUnitChrono(Chrono<?> chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chrono<?>[] clist : data_of_calendars()) {
            Chrono<?> chrono2 = clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalUnit adjuster = new FixedTemporalUnit(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.minus(1, adjuster);
                    Assert.fail("TemporalUnit.doPlus minus should have thrown a ClassCastException, " + czdt.getClass()
                            + " can not be cast to " + czdt2.getClass());
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                // Same chronology,
                ChronoZonedDateTime<?> result = czdt.minus(1, adjuster);
                assertEquals(result, czdt2, "WithAdjuster failed to replace date");
            }
        }
    }

    @Test(groups={"tck"}, dataProvider="calendars")
    public void test_badTemporalFieldChrono(Chrono<?> chrono) {
        LocalDate refDate = LocalDate.of(1900, 1, 1);
        ChronoZonedDateTime<?> czdt = chrono.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
        for (Chrono<?>[] clist : data_of_calendars()) {
            Chrono<?> chrono2 = clist[0];
            ChronoZonedDateTime<?> czdt2 = chrono2.date(refDate).atTime(LocalTime.NOON).atZone(ZoneOffset.UTC);
            TemporalField adjuster = new FixedTemporalField(czdt2);
            if (chrono != chrono2) {
                try {
                    czdt.with(adjuster, 1);
                    Assert.fail("TemporalField doWith() should have thrown a ClassCastException, " + czdt.getClass()
                            + " can not be cast to " + czdt2.getClass());
                } catch (ClassCastException cce) {
                    // Expected exception; not an error
                }
            } else {
                // Same chronology,
                ChronoZonedDateTime<?> result = czdt.with(adjuster, 1);
                assertEquals(result, czdt2, "TemporalField doWith() failed to replace date");
            }
        }
    }

    //-----------------------------------------------------------------------
    // isBefore, isAfter, isEqual, INSTANT_COMPARATOR  test a Chrono<?> against the other Chronos
    //-----------------------------------------------------------------------
    @Test(groups={"tck"}, dataProvider="calendars")
    public void test_zonedDateTime_comparisons(Chrono<?> chrono) {
        List<ChronoZonedDateTime<?>> dates = new ArrayList<>();

        ChronoZonedDateTime<?> date = chrono.date(LocalDate.of(1900, 1, 1))
                .atTime(LocalTime.MIN)
                .atZone(ZoneOffset.UTC);

        // Insert dates in order, no duplicates
        dates.add(date.minus(100, ChronoUnit.YEARS));
        dates.add(date.minus(1, ChronoUnit.YEARS));
        dates.add(date.minus(1, ChronoUnit.MONTHS));
        dates.add(date.minus(1, ChronoUnit.WEEKS));
        dates.add(date.minus(1, ChronoUnit.DAYS));
        dates.add(date.minus(1, ChronoUnit.HOURS));
        dates.add(date.minus(1, ChronoUnit.MINUTES));
        dates.add(date.minus(1, ChronoUnit.SECONDS));
        dates.add(date.minus(1, ChronoUnit.NANOS));
        dates.add(date);
        dates.add(date.plus(1, ChronoUnit.NANOS));
        dates.add(date.plus(1, ChronoUnit.SECONDS));
        dates.add(date.plus(1, ChronoUnit.MINUTES));
        dates.add(date.plus(1, ChronoUnit.HOURS));
        dates.add(date.plus(1, ChronoUnit.DAYS));
        dates.add(date.plus(1, ChronoUnit.WEEKS));
        dates.add(date.plus(1, ChronoUnit.MONTHS));
        dates.add(date.plus(1, ChronoUnit.YEARS));
        dates.add(date.plus(100, ChronoUnit.YEARS));

        // Check these dates against the corresponding dates for every calendar
        for (Chrono<?>[] clist : data_of_calendars()) {
            List<ChronoZonedDateTime<?>> otherDates = new ArrayList<>();
            Chrono<?> chrono2 = ISOChrono.INSTANCE; //clist[0];
            for (ChronoZonedDateTime<?> d : dates) {
                otherDates.add(chrono2.date(d).atTime(d.getTime()).atZone(d.getZone()));
            }

            // Now compare  the sequence of original dates with the sequence of converted dates
            for (int i = 0; i < dates.size(); i++) {
                ChronoZonedDateTime<?> a = dates.get(i);
                for (int j = 0; j < otherDates.size(); j++) {
                    ChronoZonedDateTime<?> b = otherDates.get(j);
                    int cmp = ChronoZonedDateTime.INSTANT_COMPARATOR.compare(a, b);
                    if (i < j) {
                        assertTrue(cmp < 0, a + " compare " + b);
                        assertEquals(a.isBefore(b), true, a + " isBefore " + b);
                        assertEquals(a.isAfter(b), false, a + " ifAfter " + b);
                        assertEquals(a.isEqual(b), false, a + " isEqual " + b);
                    } else if (i > j) {
                        assertTrue(cmp > 0, a + " compare " + b);
                        assertEquals(a.isBefore(b), false, a + " isBefore " + b);
                        assertEquals(a.isAfter(b), true, a + " ifAfter " + b);
                        assertEquals(a.isEqual(b), false, a + " isEqual " + b);
                    } else {
                        assertTrue(cmp == 0, a + " compare " + b);
                        assertEquals(a.isBefore(b), false, a + " isBefore " + b);
                        assertEquals(a.isAfter(b), false, a + " ifAfter " + b);
                        assertEquals(a.isEqual(b), true, a + " isEqual " + b);
                    }
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // Test Serialization of ISO via chrono API
    //-----------------------------------------------------------------------
    @Test( groups={"tck"}, dataProvider="calendars")
    public <C extends Chrono<C>> void test_ChronoZonedDateTimeSerialization(C chrono) throws Exception {
        ZonedDateTime ref = LocalDate.of(2000, 1, 5).atTime(12, 1, 2, 3).atZone(ZoneId.of("GMT+01:23"));
        ChronoZonedDateTime<C> orginal = chrono.date(ref).atTime(ref.getTime()).atZone(ref.getZone());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(orginal);
        out.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        @SuppressWarnings("unchecked")
        ChronoZonedDateTime<C> ser = (ChronoZonedDateTime<C>) in.readObject();
        assertEquals(ser, orginal, "deserialized date is wrong");
    }


    /**
     * FixedAdjusted returns a fixed Temporal in all adjustments.
     * Construct an adjuster with the Temporal that should be returned from adjust.
     */
    static class FixedAdjuster implements TemporalAdjuster, TemporalAdder, TemporalSubtractor {
        private Temporal datetime;

        FixedAdjuster(Temporal datetime) {
            this.datetime = datetime;
        }

        @Override
        public Temporal adjustInto(Temporal ignore) {
            return datetime;
        }

        @Override
        public Temporal addTo(Temporal ignore) {
            return datetime;
        }

        @Override
        public Temporal subtractFrom(Temporal ignore) {
            return datetime;
        }

    }

    /**
     * FixedTemporalUnit returns a fixed Temporal in all adjustments.
     * Construct an FixedTemporalUnit with the Temporal that should be returned from doPlus.
     */
    static class FixedTemporalUnit implements TemporalUnit {
        private Temporal temporal;

        FixedTemporalUnit(Temporal temporal) {
            this.temporal = temporal;
        }

        @Override
        public String getName() {
            return "FixedTemporalUnit";
        }

        @Override
        public Duration getDuration() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDurationEstimated() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isSupported(Temporal temporal) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R doPlus(R dateTime, long periodToAdd) {
            return (R) this.temporal;
        }

        @Override
        public <R extends Temporal> SimplePeriod between(R dateTime1, R dateTime2) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    /**
     * FixedTemporalField returns a fixed Temporal in all adjustments.
     * Construct an FixedTemporalField with the Temporal that should be returned from doWith.
     */
    static class FixedTemporalField implements TemporalField {
        private Temporal temporal;
        FixedTemporalField(Temporal temporal) {
            this.temporal = temporal;
        }

        @Override
        public String getName() {
            return "FixedTemporalField";
        }

        @Override
        public TemporalUnit getBaseUnit() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public TemporalUnit getRangeUnit() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ValueRange range() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean doIsSupported(TemporalAccessor temporal) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ValueRange doRange(TemporalAccessor temporal) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long doGet(TemporalAccessor temporal) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R doWith(R temporal, long newValue) {
            return (R) this.temporal;
        }

        @Override
        public boolean resolve(DateTimeBuilder builder, long value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
