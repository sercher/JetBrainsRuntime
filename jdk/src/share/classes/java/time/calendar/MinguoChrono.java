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
package java.time.calendar;

import static java.time.temporal.ChronoField.YEAR;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.Chrono;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoLocalDate;
import java.time.temporal.Era;
import java.time.temporal.ISOChrono;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The Minguo calendar system.
 * <p>
 * This chronology defines the rules of the Minguo calendar system.
 * This calendar system is primarily used in the Republic of China, often known as Taiwan.
 * Dates are aligned such that {@code 0001-01-01 (Minguo)} is {@code 1911-01-01 (ISO)}.
 * <p>
 * The fields are defined as follows:
 * <p><ul>
 * <li>era - There are two eras, the current 'Republic' (ERA_ROC) and the previous era (ERA_BEFORE_ROC).
 * <li>year-of-era - The year-of-era for the current era increases uniformly from the epoch at year one.
 *  For the previous era the year increases from one as time goes backwards.
 *  The value for the current era is equal to the ISO proleptic-year minus 1911.
 * <li>proleptic-year - The proleptic year is the same as the year-of-era for the
 *  current era. For the previous era, years have zero, then negative values.
 *  The value is equal to the ISO proleptic-year minus 1911.
 * <li>month-of-year - The Minguo month-of-year exactly matches ISO.
 * <li>day-of-month - The Minguo day-of-month exactly matches ISO.
 * <li>day-of-year - The Minguo day-of-year exactly matches ISO.
 * <li>leap-year - The Minguo leap-year pattern exactly matches ISO, such that the two calendars
 *  are never out of step.
 * </ul><p>
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 *
 * @since 1.8
 */
public final class MinguoChrono extends Chrono<MinguoChrono> implements Serializable {

    /**
     * Singleton instance for the Minguo chronology.
     */
    public static final MinguoChrono INSTANCE = new MinguoChrono();

    /**
     * The singleton instance for the era ROC.
     */
    public static final Era<MinguoChrono> ERA_ROC = MinguoEra.ROC;

    /**
     * The singleton instance for the era BEFORE_ROC.
     */
    public static final Era<MinguoChrono> ERA_BEFORE_ROC = MinguoEra.BEFORE_ROC;

    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1039765215346859963L;
    /**
     * The difference in years between ISO and Minguo.
     */
    static final int YEARS_DIFFERENCE = 1911;

    /**
     * Restricted constructor.
     */
    private MinguoChrono() {
    }

    /**
     * Resolve singleton.
     *
     * @return the singleton instance, not null
     */
    private Object readResolve() {
        return INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the ID of the chronology - 'Minguo'.
     * <p>
     * The ID uniquely identifies the {@code Chrono}.
     * It can be used to lookup the {@code Chrono} using {@link #of(String)}.
     *
     * @return the chronology ID - 'Minguo'
     * @see #getCalendarType()
     */
    @Override
    public String getId() {
        return "Minguo";
    }

    /**
     * Gets the calendar type of the underlying calendar system - 'roc'.
     * <p>
     * The calendar type is an identifier defined by the
     * <em>Unicode Locale Data Markup Language (LDML)</em> specification.
     * It can be used to lookup the {@code Chrono} using {@link #of(String)}.
     * It can also be used as part of a locale, accessible via
     * {@link Locale#getUnicodeLocaleType(String)} with the key 'ca'.
     *
     * @return the calendar system type - 'roc'
     * @see #getId()
     */
    @Override
    public String getCalendarType() {
        return "roc";
    }

    //-----------------------------------------------------------------------
    @Override
    public ChronoLocalDate<MinguoChrono> date(int prolepticYear, int month, int dayOfMonth) {
        return new MinguoDate(LocalDate.of(prolepticYear + YEARS_DIFFERENCE, month, dayOfMonth));
    }

    @Override
    public ChronoLocalDate<MinguoChrono> dateYearDay(int prolepticYear, int dayOfYear) {
        return new MinguoDate(LocalDate.ofYearDay(prolepticYear + YEARS_DIFFERENCE, dayOfYear));
    }

    @Override
    public ChronoLocalDate<MinguoChrono> date(TemporalAccessor temporal) {
        if (temporal instanceof MinguoDate) {
            return (MinguoDate) temporal;
        }
        return new MinguoDate(LocalDate.from(temporal));
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the specified year is a leap year.
     * <p>
     * Minguo leap years occur exactly in line with ISO leap years.
     * This method does not validate the year passed in, and only has a
     * well-defined result for years in the supported range.
     *
     * @param prolepticYear  the proleptic-year to check, not validated for range
     * @return true if the year is a leap year
     */
    @Override
    public boolean isLeapYear(long prolepticYear) {
        return ISOChrono.INSTANCE.isLeapYear(prolepticYear + YEARS_DIFFERENCE);
    }

    @Override
    public int prolepticYear(Era<MinguoChrono> era, int yearOfEra) {
        if (era instanceof MinguoEra == false) {
            throw new DateTimeException("Era must be MinguoEra");
        }
        return (era == MinguoEra.ROC ? yearOfEra : 1 - yearOfEra);
    }

    @Override
    public Era<MinguoChrono> eraOf(int eraValue) {
        return MinguoEra.of(eraValue);
    }

    @Override
    public List<Era<MinguoChrono>> eras() {
        return Arrays.<Era<MinguoChrono>>asList(MinguoEra.values());
    }

    //-----------------------------------------------------------------------
    @Override
    public ValueRange range(ChronoField field) {
        switch (field) {
            case YEAR_OF_ERA: {
                ValueRange range = YEAR.range();
                return ValueRange.of(1, range.getMaximum() - YEARS_DIFFERENCE, -range.getMinimum() + 1 + YEARS_DIFFERENCE);
            }
            case YEAR: {
                ValueRange range = YEAR.range();
                return ValueRange.of(range.getMinimum() - YEARS_DIFFERENCE, range.getMaximum() - YEARS_DIFFERENCE);
            }
        }
        return field.range();
    }

}
