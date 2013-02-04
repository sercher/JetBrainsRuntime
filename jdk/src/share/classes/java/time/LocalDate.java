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
package java.time;

import static java.time.LocalTime.SECONDS_PER_DAY;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.EPOCH_MONTH;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.format.DateTimeBuilder;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatters;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Era;
import java.time.temporal.ISOChrono;
import java.time.temporal.OffsetDate;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdder;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalSubtractor;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;
import java.time.temporal.Year;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.Objects;

/**
 * A date without a time-zone in the ISO-8601 calendar system,
 * such as {@code 2007-12-03}.
 * <p>
 * {@code LocalDate} is an immutable date-time object that represents a date,
 * often viewed as year-month-day. Other date fields, such as day-of-year,
 * day-of-week and week-of-year, can also be accessed.
 * For example, the value "2nd October 2007" can be stored in a {@code LocalDate}.
 * <p>
 * This class does not store or represent a time or time-zone.
 * Instead, it is a description of the date, as used for birthdays.
 * It cannot represent an instant on the time-line without additional information
 * such as an offset or time-zone.
 * <p>
 * The ISO-8601 calendar system is the modern civil calendar system used today
 * in most of the world. It is equivalent to the proleptic Gregorian calendar
 * system, in which today's rules for leap years are applied for all time.
 * For most applications written today, the ISO-8601 rules are entirely suitable.
 * However, any application that makes use of historical dates, and requires them
 * to be accurate will find the ISO-8601 approach unsuitable.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 *
 * @since 1.8
 */
public final class LocalDate
        implements Temporal, TemporalAdjuster, ChronoLocalDate<ISOChrono>, Serializable {

    /**
     * The minimum supported {@code LocalDate}, '-999999999-01-01'.
     * This could be used by an application as a "far past" date.
     */
    public static final LocalDate MIN = LocalDate.of(Year.MIN_VALUE, 1, 1);
    /**
     * The maximum supported {@code LocalDate}, '+999999999-12-31'.
     * This could be used by an application as a "far future" date.
     */
    public static final LocalDate MAX = LocalDate.of(Year.MAX_VALUE, 12, 31);

    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 2942565459149668126L;
    /**
     * The number of days in a 400 year cycle.
     */
    private static final int DAYS_PER_CYCLE = 146097;
    /**
     * The number of days from year zero to year 1970.
     * There are five 400 year cycles from year zero to 2000.
     * There are 7 leap years from 1970 to 2000.
     */
    static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);

    /**
     * The year.
     */
    private final int year;
    /**
     * The month-of-year.
     */
    private final short month;
    /**
     * The day-of-month.
     */
    private final short day;

    //-----------------------------------------------------------------------
    /**
     * Obtains the current date from the system clock in the default time-zone.
     * <p>
     * This will query the {@link Clock#systemDefaultZone() system clock} in the default
     * time-zone to obtain the current date.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @return the current date using the system clock and default time-zone, not null
     */
    public static LocalDate now() {
        return now(Clock.systemDefaultZone());
    }

    /**
     * Obtains the current date from the system clock in the specified time-zone.
     * <p>
     * This will query the {@link Clock#system(ZoneId) system clock} to obtain the current date.
     * Specifying the time-zone avoids dependence on the default time-zone.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @param zone  the zone ID to use, not null
     * @return the current date using the system clock, not null
     */
    public static LocalDate now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    /**
     * Obtains the current date from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current date - today.
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@link Clock dependency injection}.
     *
     * @param clock  the clock to use, not null
     * @return the current date, not null
     */
    public static LocalDate now(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        // inline OffsetDate factory to avoid creating object and InstantProvider checks
        final Instant now = clock.instant();  // called once
        ZoneOffset offset = clock.getZone().getRules().getOffset(now);
        long epochSec = now.getEpochSecond() + offset.getTotalSeconds();  // overflow caught later
        long epochDay = Math.floorDiv(epochSec, SECONDS_PER_DAY);
        return LocalDate.ofEpochDay(epochDay);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code LocalDate} from a year, month and day.
     * <p>
     * The day must be valid for the year and month, otherwise an exception will be thrown.
     *
     * @param year  the year to represent, from MIN_YEAR to MAX_YEAR
     * @param month  the month-of-year to represent, not null
     * @param dayOfMonth  the day-of-month to represent, from 1 to 31
     * @return the local date, not null
     * @throws DateTimeException if the value of any field is out of range
     * @throws DateTimeException if the day-of-month is invalid for the month-year
     */
    public static LocalDate of(int year, Month month, int dayOfMonth) {
        YEAR.checkValidValue(year);
        Objects.requireNonNull(month, "month");
        DAY_OF_MONTH.checkValidValue(dayOfMonth);
        return create(year, month, dayOfMonth);
    }

    /**
     * Obtains an instance of {@code LocalDate} from a year, month and day.
     * <p>
     * The day must be valid for the year and month, otherwise an exception will be thrown.
     *
     * @param year  the year to represent, from MIN_YEAR to MAX_YEAR
     * @param month  the month-of-year to represent, from 1 (January) to 12 (December)
     * @param dayOfMonth  the day-of-month to represent, from 1 to 31
     * @return the local date, not null
     * @throws DateTimeException if the value of any field is out of range
     * @throws DateTimeException if the day-of-month is invalid for the month-year
     */
    public static LocalDate of(int year, int month, int dayOfMonth) {
        YEAR.checkValidValue(year);
        MONTH_OF_YEAR.checkValidValue(month);
        DAY_OF_MONTH.checkValidValue(dayOfMonth);
        return create(year, Month.of(month), dayOfMonth);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code LocalDate} from a year and day-of-year.
     * <p>
     * The day-of-year must be valid for the year, otherwise an exception will be thrown.
     *
     * @param year  the year to represent, from MIN_YEAR to MAX_YEAR
     * @param dayOfYear  the day-of-year to represent, from 1 to 366
     * @return the local date, not null
     * @throws DateTimeException if the value of any field is out of range
     * @throws DateTimeException if the day-of-year is invalid for the month-year
     */
    public static LocalDate ofYearDay(int year, int dayOfYear) {
        YEAR.checkValidValue(year);
        DAY_OF_YEAR.checkValidValue(dayOfYear);
        boolean leap = ISOChrono.INSTANCE.isLeapYear(year);
        if (dayOfYear == 366 && leap == false) {
            throw new DateTimeException("Invalid date 'DayOfYear 366' as '" + year + "' is not a leap year");
        }
        Month moy = Month.of((dayOfYear - 1) / 31 + 1);
        int monthEnd = moy.firstDayOfYear(leap) + moy.length(leap) - 1;
        if (dayOfYear > monthEnd) {
            moy = moy.plus(1);
        }
        int dom = dayOfYear - moy.firstDayOfYear(leap) + 1;
        return create(year, moy, dom);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code LocalDate} from the epoch day count.
     * <p>
     * The Epoch Day count is a simple incrementing count of days
     * where day 0 is 1970-01-01. Negative numbers represent earlier days.
     *
     * @param epochDay  the Epoch Day to convert, based on the epoch 1970-01-01
     * @return the local date, not null
     * @throws DateTimeException if the epoch days exceeds the supported date range
     */
    public static LocalDate ofEpochDay(long epochDay) {
        long zeroDay = epochDay + DAYS_0000_TO_1970;
        // find the march-based year
        zeroDay -= 60;  // adjust to 0000-03-01 so leap day is at end of four year cycle
        long adjust = 0;
        if (zeroDay < 0) {
            // adjust negative years to positive for calculation
            long adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1;
            adjust = adjustCycles * 400;
            zeroDay += -adjustCycles * DAYS_PER_CYCLE;
        }
        long yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if (doyEst < 0) {
            // fix estimate
            yearEst--;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
        yearEst += adjust;  // reset any negative year
        int marchDoy0 = (int) doyEst;

        // convert march-based values back to january-based
        int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        int month = (marchMonth0 + 2) % 12 + 1;
        int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst += marchMonth0 / 10;

        // check year now we are certain it is correct
        int year = YEAR.checkValidIntValue(yearEst);
        return new LocalDate(year, month, dom);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code LocalDate} from a temporal object.
     * <p>
     * A {@code TemporalAccessor} represents some form of date and time information.
     * This factory converts the arbitrary temporal object to an instance of {@code LocalDate}.
     * <p>
     * The conversion extracts the {@link ChronoField#EPOCH_DAY EPOCH_DAY} field.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code LocalDate::from}.
     *
     * @param temporal  the temporal object to convert, not null
     * @return the local date, not null
     * @throws DateTimeException if unable to convert to a {@code LocalDate}
     */
    public static LocalDate from(TemporalAccessor temporal) {
        if (temporal instanceof LocalDate) {
            return (LocalDate) temporal;
        } else if (temporal instanceof LocalDateTime) {
            return ((LocalDateTime) temporal).getDate();
        } else if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).getDate();
        }
        // handle builder as a special case
        if (temporal instanceof DateTimeBuilder) {
            DateTimeBuilder builder = (DateTimeBuilder) temporal;
            LocalDate date = builder.extract(LocalDate.class);
            if (date != null) {
                return date;
            }
        }
        try {
            return ofEpochDay(temporal.getLong(EPOCH_DAY));
        } catch (DateTimeException ex) {
            throw new DateTimeException("Unable to obtain LocalDate from TemporalAccessor: " + temporal.getClass(), ex);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code LocalDate} from a text string such as {@code 2007-12-03}.
     * <p>
     * The string must represent a valid date and is parsed using
     * {@link java.time.format.DateTimeFormatters#isoLocalDate()}.
     *
     * @param text  the text to parse such as "2007-12-03", not null
     * @return the parsed local date, not null
     * @throws DateTimeParseException if the text cannot be parsed
     */
    public static LocalDate parse(CharSequence text) {
        return parse(text, DateTimeFormatters.isoLocalDate());
    }

    /**
     * Obtains an instance of {@code LocalDate} from a text string using a specific formatter.
     * <p>
     * The text is parsed using the formatter, returning a date.
     *
     * @param text  the text to parse, not null
     * @param formatter  the formatter to use, not null
     * @return the parsed local date, not null
     * @throws DateTimeParseException if the text cannot be parsed
     */
    public static LocalDate parse(CharSequence text, DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        return formatter.parse(text, LocalDate::from);
    }

    //-----------------------------------------------------------------------
    /**
     * Creates a local date from the year, month and day fields.
     *
     * @param year  the year to represent, validated from MIN_YEAR to MAX_YEAR
     * @param month  the month-of-year to represent, validated not null
     * @param dayOfMonth  the day-of-month to represent, validated from 1 to 31
     * @return the local date, not null
     * @throws DateTimeException if the day-of-month is invalid for the month-year
     */
    private static LocalDate create(int year, Month month, int dayOfMonth) {
        if (dayOfMonth > 28 && dayOfMonth > month.length(ISOChrono.INSTANCE.isLeapYear(year))) {
            if (dayOfMonth == 29) {
                throw new DateTimeException("Invalid date 'February 29' as '" + year + "' is not a leap year");
            } else {
                throw new DateTimeException("Invalid date '" + month.name() + " " + dayOfMonth + "'");
            }
        }
        return new LocalDate(year, month.getValue(), dayOfMonth);
    }

    /**
     * Resolves the date, resolving days past the end of month.
     *
     * @param year  the year to represent, validated from MIN_YEAR to MAX_YEAR
     * @param month  the month-of-year to represent, validated from 1 to 12
     * @param day  the day-of-month to represent, validated from 1 to 31
     * @return the resolved date, not null
     */
    private static LocalDate resolvePreviousValid(int year, int month, int day) {
        switch (month) {
            case 2:
                day = Math.min(day, ISOChrono.INSTANCE.isLeapYear(year) ? 29 : 28);
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                day = Math.min(day, 30);
                break;
        }
        return LocalDate.of(year, month, day);
    }

    /**
     * Constructor, previously validated.
     *
     * @param year  the year to represent, from MIN_YEAR to MAX_YEAR
     * @param month  the month-of-year to represent, not null
     * @param dayOfMonth  the day-of-month to represent, valid for year-month, from 1 to 31
     */
    private LocalDate(int year, int month, int dayOfMonth) {
        this.year = year;
        this.month = (short) month;
        this.day = (short) dayOfMonth;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the specified field is supported.
     * <p>
     * This checks if this date can be queried for the specified field.
     * If false, then calling the {@link #range(TemporalField) range} and
     * {@link #get(TemporalField) get} methods will throw an exception.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this date-time.
     * The supported fields are:
     * <ul>
     * <li>{@code DAY_OF_WEEK}
     * <li>{@code ALIGNED_DAY_OF_WEEK_IN_MONTH}
     * <li>{@code ALIGNED_DAY_OF_WEEK_IN_YEAR}
     * <li>{@code DAY_OF_MONTH}
     * <li>{@code DAY_OF_YEAR}
     * <li>{@code EPOCH_DAY}
     * <li>{@code ALIGNED_WEEK_OF_MONTH}
     * <li>{@code ALIGNED_WEEK_OF_YEAR}
     * <li>{@code MONTH_OF_YEAR}
     * <li>{@code EPOCH_MONTH}
     * <li>{@code YEAR_OF_ERA}
     * <li>{@code YEAR}
     * <li>{@code ERA}
     * </ul>
     * All other {@code ChronoField} instances will return false.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.doIsSupported(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the field is supported is determined by the field.
     *
     * @param field  the field to check, null returns false
     * @return true if the field is supported on this date, false if not
     */
    @Override  // override for Javadoc
    public boolean isSupported(TemporalField field) {
        return ChronoLocalDate.super.isSupported(field);
    }

    /**
     * Gets the range of valid values for the specified field.
     * <p>
     * The range object expresses the minimum and maximum valid values for a field.
     * This date is used to enhance the accuracy of the returned range.
     * If it is not possible to return the range, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return
     * appropriate range instances.
     * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.doRange(TemporalAccessor)}
     * passing {@code this} as the argument.
     * Whether the range can be obtained is determined by the field.
     *
     * @param field  the field to query the range for, not null
     * @return the range of valid values for the field, not null
     * @throws DateTimeException if the range for the field cannot be obtained
     */
    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (f.isDateField()) {
                switch (f) {
                    case DAY_OF_MONTH: return ValueRange.of(1, lengthOfMonth());
                    case DAY_OF_YEAR: return ValueRange.of(1, lengthOfYear());
                    case ALIGNED_WEEK_OF_MONTH: return ValueRange.of(1, getMonth() == Month.FEBRUARY && isLeapYear() == false ? 4 : 5);
                    case YEAR_OF_ERA:
                        return (getYear() <= 0 ? ValueRange.of(1, Year.MAX_VALUE + 1) : ValueRange.of(1, Year.MAX_VALUE));
                }
                return field.range();
            }
            throw new DateTimeException("Unsupported field: " + field.getName());
        }
        return field.doRange(this);
    }

    /**
     * Gets the value of the specified field from this date as an {@code int}.
     * <p>
     * This queries this date for the value for the specified field.
     * The returned value will always be within the valid range of values for the field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this date, except {@code EPOCH_DAY} and {@code EPOCH_MONTH}
     * which are too large to fit in an {@code int} and throw a {@code DateTimeException}.
     * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.doGet(TemporalAccessor)}
     * passing {@code this} as the argument. Whether the value can be obtained,
     * and what the value represents, is determined by the field.
     *
     * @param field  the field to get, not null
     * @return the value for the field
     * @throws DateTimeException if a value for the field cannot be obtained
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override  // override for Javadoc and performance
    public int get(TemporalField field) {
        if (field instanceof ChronoField) {
            return get0(field);
        }
        return ChronoLocalDate.super.get(field);
    }

    /**
     * Gets the value of the specified field from this date as a {@code long}.
     * <p>
     * This queries this date for the value for the specified field.
     * If it is not possible to return the value, because the field is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * If the field is a {@link ChronoField} then the query is implemented here.
     * The {@link #isSupported(TemporalField) supported fields} will return valid
     * values based on this date.
     * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.doGet(TemporalAccessor)}
     * passing {@code this} as the argument. Whether the value can be obtained,
     * and what the value represents, is determined by the field.
     *
     * @param field  the field to get, not null
     * @return the value for the field
     * @throws DateTimeException if a value for the field cannot be obtained
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            if (field == EPOCH_DAY) {
                return toEpochDay();
            }
            if (field == EPOCH_MONTH) {
                return getEpochMonth();
            }
            return get0(field);
        }
        return field.doGet(this);
    }

    private int get0(TemporalField field) {
        switch ((ChronoField) field) {
            case DAY_OF_WEEK: return getDayOfWeek().getValue();
            case ALIGNED_DAY_OF_WEEK_IN_MONTH: return ((day - 1) % 7) + 1;
            case ALIGNED_DAY_OF_WEEK_IN_YEAR: return ((getDayOfYear() - 1) % 7) + 1;
            case DAY_OF_MONTH: return day;
            case DAY_OF_YEAR: return getDayOfYear();
            case EPOCH_DAY: throw new DateTimeException("Field too large for an int: " + field);
            case ALIGNED_WEEK_OF_MONTH: return ((day - 1) / 7) + 1;
            case ALIGNED_WEEK_OF_YEAR: return ((getDayOfYear() - 1) / 7) + 1;
            case MONTH_OF_YEAR: return month;
            case EPOCH_MONTH: throw new DateTimeException("Field too large for an int: " + field);
            case YEAR_OF_ERA: return (year >= 1 ? year : 1 - year);
            case YEAR: return year;
            case ERA: return (year >= 1 ? 1 : 0);
        }
        throw new DateTimeException("Unsupported field: " + field.getName());
    }

    private long getEpochMonth() {
        return ((year - 1970) * 12L) + (month - 1);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the chronology of this date, which is the ISO calendar system.
     * <p>
     * The {@code Chrono} represents the calendar system in use.
     * The ISO-8601 calendar system is the modern civil calendar system used today
     * in most of the world. It is equivalent to the proleptic Gregorian calendar
     * system, in which todays's rules for leap years are applied for all time.
     *
     * @return the ISO chronology, not null
     */
    @Override
    public ISOChrono getChrono() {
        return ISOChrono.INSTANCE;
    }

    /**
     * Gets the era applicable at this date.
     * <p>
     * The official ISO-8601 standard does not define eras, however {@code ISOChrono} does.
     * It defines two eras, 'CE' from year one onwards and 'BCE' from year zero backwards.
     * Since dates before the Julian-Gregorian cutover are not in line with history,
     * the cutover between 'BCE' and 'CE' is also not aligned with the commonly used
     * eras, often referred to using 'BC' and 'AD'.
     * <p>
     * Users of this class should typically ignore this method as it exists primarily
     * to fulfill the {@link ChronoLocalDate} contract where it is necessary to support
     * the Japanese calendar system.
     * <p>
     * The returned era will be a singleton capable of being compared with the constants
     * in {@link ISOChrono} using the {@code ==} operator.
     *
     * @return the {@code ISOChrono} era constant applicable at this date, not null
     */
    @Override // override for Javadoc
    public Era<ISOChrono> getEra() {
        return ChronoLocalDate.super.getEra();
    }

    /**
     * Gets the year field.
     * <p>
     * This method returns the primitive {@code int} value for the year.
     * <p>
     * The year returned by this method is proleptic as per {@code get(YEAR)}.
     * To obtain the year-of-era, use {@code get(YEAR_OF_ERA}.
     *
     * @return the year, from MIN_YEAR to MAX_YEAR
     */
    public int getYear() {
        return year;
    }

    /**
     * Gets the month-of-year field from 1 to 12.
     * <p>
     * This method returns the month as an {@code int} from 1 to 12.
     * Application code is frequently clearer if the enum {@link Month}
     * is used by calling {@link #getMonth()}.
     *
     * @return the month-of-year, from 1 to 12
     * @see #getMonth()
     */
    public int getMonthValue() {
        return month;
    }

    /**
     * Gets the month-of-year field using the {@code Month} enum.
     * <p>
     * This method returns the enum {@link Month} for the month.
     * This avoids confusion as to what {@code int} values mean.
     * If you need access to the primitive {@code int} value then the enum
     * provides the {@link Month#getValue() int value}.
     *
     * @return the month-of-year, not null
     * @see #getMonthValue()
     */
    public Month getMonth() {
        return Month.of(month);
    }

    /**
     * Gets the day-of-month field.
     * <p>
     * This method returns the primitive {@code int} value for the day-of-month.
     *
     * @return the day-of-month, from 1 to 31
     */
    public int getDayOfMonth() {
        return day;
    }

    /**
     * Gets the day-of-year field.
     * <p>
     * This method returns the primitive {@code int} value for the day-of-year.
     *
     * @return the day-of-year, from 1 to 365, or 366 in a leap year
     */
    public int getDayOfYear() {
        return getMonth().firstDayOfYear(isLeapYear()) + day - 1;
    }

    /**
     * Gets the day-of-week field, which is an enum {@code DayOfWeek}.
     * <p>
     * This method returns the enum {@link DayOfWeek} for the day-of-week.
     * This avoids confusion as to what {@code int} values mean.
     * If you need access to the primitive {@code int} value then the enum
     * provides the {@link DayOfWeek#getValue() int value}.
     * <p>
     * Additional information can be obtained from the {@code DayOfWeek}.
     * This includes textual names of the values.
     *
     * @return the day-of-week, not null
     */
    public DayOfWeek getDayOfWeek() {
        int dow0 = (int)Math.floorMod(toEpochDay() + 3, 7);
        return DayOfWeek.of(dow0 + 1);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the year is a leap year, according to the ISO proleptic
     * calendar system rules.
     * <p>
     * This method applies the current rules for leap years across the whole time-line.
     * In general, a year is a leap year if it is divisible by four without
     * remainder. However, years divisible by 100, are not leap years, with
     * the exception of years divisible by 400 which are.
     * <p>
     * For example, 1904 is a leap year it is divisible by 4.
     * 1900 was not a leap year as it is divisible by 100, however 2000 was a
     * leap year as it is divisible by 400.
     * <p>
     * The calculation is proleptic - applying the same rules into the far future and far past.
     * This is historically inaccurate, but is correct for the ISO-8601 standard.
     *
     * @return true if the year is leap, false otherwise
     */
    @Override // override for Javadoc and performance
    public boolean isLeapYear() {
        return ISOChrono.INSTANCE.isLeapYear(year);
    }

    /**
     * Returns the length of the month represented by this date.
     * <p>
     * This returns the length of the month in days.
     * For example, a date in January would return 31.
     *
     * @return the length of the month in days
     */
    @Override
    public int lengthOfMonth() {
        switch (month) {
            case 2:
                return (isLeapYear() ? 29 : 28);
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    /**
     * Returns the length of the year represented by this date.
     * <p>
     * This returns the length of the year in days, either 365 or 366.
     *
     * @return 366 if the year is leap, 365 otherwise
     */
    @Override // override for Javadoc and performance
    public int lengthOfYear() {
        return (isLeapYear() ? 366 : 365);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns an adjusted copy of this date.
     * <p>
     * This returns a new {@code LocalDate}, based on this one, with the date adjusted.
     * The adjustment takes place using the specified adjuster strategy object.
     * Read the documentation of the adjuster to understand what adjustment will be made.
     * <p>
     * A simple adjuster might simply set the one of the fields, such as the year field.
     * A more complex adjuster might set the date to the last day of the month.
     * A selection of common adjustments is provided in {@link java.time.temporal.Adjusters}.
     * These include finding the "last day of the month" and "next Wednesday".
     * Key date-time classes also implement the {@code TemporalAdjuster} interface,
     * such as {@link Month} and {@link java.time.temporal.MonthDay MonthDay}.
     * The adjuster is responsible for handling special cases, such as the varying
     * lengths of month and leap years.
     * <p>
     * For example this code returns a date on the last day of July:
     * <pre>
     *  import static java.time.Month.*;
     *  import static java.time.temporal.Adjusters.*;
     *
     *  result = localDate.with(JULY).with(lastDayOfMonth());
     * </pre>
     * <p>
     * The result of this method is obtained by invoking the
     * {@link TemporalAdjuster#adjustInto(Temporal)} method on the
     * specified adjuster passing {@code this} as the argument.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param adjuster the adjuster to use, not null
     * @return a {@code LocalDate} based on {@code this} with the adjustment made, not null
     * @throws DateTimeException if the adjustment cannot be made
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public LocalDate with(TemporalAdjuster adjuster) {
        // optimizations
        if (adjuster instanceof LocalDate) {
            return (LocalDate) adjuster;
        }
        return (LocalDate) adjuster.adjustInto(this);
    }

    /**
     * Returns a copy of this date with the specified field set to a new value.
     * <p>
     * This returns a new {@code LocalDate}, based on this one, with the value
     * for the specified field changed.
     * This can be used to change any supported field, such as the year, month or day-of-month.
     * If it is not possible to set the value, because the field is not supported or for
     * some other reason, an exception is thrown.
     * <p>
     * In some cases, changing the specified field can cause the resulting date to become invalid,
     * such as changing the month from 31st January to February would make the day-of-month invalid.
     * In cases like this, the field is responsible for resolving the date. Typically it will choose
     * the previous valid date, which would be the last valid day of February in this example.
     * <p>
     * If the field is a {@link ChronoField} then the adjustment is implemented here.
     * The supported fields behave as follows:
     * <ul>
     * <li>{@code DAY_OF_WEEK} -
     *  Returns a {@code LocalDate} with the specified day-of-week.
     *  The date is adjusted up to 6 days forward or backward within the boundary
     *  of a Monday to Sunday week.
     * <li>{@code ALIGNED_DAY_OF_WEEK_IN_MONTH} -
     *  Returns a {@code LocalDate} with the specified aligned-day-of-week.
     *  The date is adjusted to the specified month-based aligned-day-of-week.
     *  Aligned weeks are counted such that the first week of a given month starts
     *  on the first day of that month.
     *  This may cause the date to be moved up to 6 days into the following month.
     * <li>{@code ALIGNED_DAY_OF_WEEK_IN_YEAR} -
     *  Returns a {@code LocalDate} with the specified aligned-day-of-week.
     *  The date is adjusted to the specified year-based aligned-day-of-week.
     *  Aligned weeks are counted such that the first week of a given year starts
     *  on the first day of that year.
     *  This may cause the date to be moved up to 6 days into the following year.
     * <li>{@code DAY_OF_MONTH} -
     *  Returns a {@code LocalDate} with the specified day-of-month.
     *  The month and year will be unchanged. If the day-of-month is invalid for the
     *  year and month, then a {@code DateTimeException} is thrown.
     * <li>{@code DAY_OF_YEAR} -
     *  Returns a {@code LocalDate} with the specified day-of-year.
     *  The year will be unchanged. If the day-of-year is invalid for the
     *  year, then a {@code DateTimeException} is thrown.
     * <li>{@code EPOCH_DAY} -
     *  Returns a {@code LocalDate} with the specified epoch-day.
     *  This completely replaces the date and is equivalent to {@link #ofEpochDay(long)}.
     * <li>{@code ALIGNED_WEEK_OF_MONTH} -
     *  Returns a {@code LocalDate} with the specified aligned-week-of-month.
     *  Aligned weeks are counted such that the first week of a given month starts
     *  on the first day of that month.
     *  This adjustment moves the date in whole week chunks to match the specified week.
     *  The result will have the same day-of-week as this date.
     *  This may cause the date to be moved into the following month.
     * <li>{@code ALIGNED_WEEK_OF_YEAR} -
     *  Returns a {@code LocalDate} with the specified aligned-week-of-year.
     *  Aligned weeks are counted such that the first week of a given year starts
     *  on the first day of that year.
     *  This adjustment moves the date in whole week chunks to match the specified week.
     *  The result will have the same day-of-week as this date.
     *  This may cause the date to be moved into the following year.
     * <li>{@code MONTH_OF_YEAR} -
     *  Returns a {@code LocalDate} with the specified month-of-year.
     *  The year will be unchanged. The day-of-month will also be unchanged,
     *  unless it would be invalid for the new month and year. In that case, the
     *  day-of-month is adjusted to the maximum valid value for the new month and year.
     * <li>{@code EPOCH_MONTH} -
     *  Returns a {@code LocalDate} with the specified epoch-month.
     *  The day-of-month will be unchanged, unless it would be invalid for the new month
     *  and year. In that case, the day-of-month is adjusted to the maximum valid value
     *  for the new month and year.
     * <li>{@code YEAR_OF_ERA} -
     *  Returns a {@code LocalDate} with the specified year-of-era.
     *  The era and month will be unchanged. The day-of-month will also be unchanged,
     *  unless it would be invalid for the new month and year. In that case, the
     *  day-of-month is adjusted to the maximum valid value for the new month and year.
     * <li>{@code YEAR} -
     *  Returns a {@code LocalDate} with the specified year.
     *  The month will be unchanged. The day-of-month will also be unchanged,
     *  unless it would be invalid for the new month and year. In that case, the
     *  day-of-month is adjusted to the maximum valid value for the new month and year.
     * <li>{@code ERA} -
     *  Returns a {@code LocalDate} with the specified era.
     *  The year-of-era and month will be unchanged. The day-of-month will also be unchanged,
     *  unless it would be invalid for the new month and year. In that case, the
     *  day-of-month is adjusted to the maximum valid value for the new month and year.
     * </ul>
     * <p>
     * In all cases, if the new value is outside the valid range of values for the field
     * then a {@code DateTimeException} will be thrown.
     * <p>
     * All other {@code ChronoField} instances will throw a {@code DateTimeException}.
     * <p>
     * If the field is not a {@code ChronoField}, then the result of this method
     * is obtained by invoking {@code TemporalField.doWith(Temporal, long)}
     * passing {@code this} as the argument. In this case, the field determines
     * whether and how to adjust the instant.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param field  the field to set in the result, not null
     * @param newValue  the new value of the field in the result
     * @return a {@code LocalDate} based on {@code this} with the specified field set, not null
     * @throws DateTimeException if the field cannot be set
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public LocalDate with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            f.checkValidValue(newValue);
            switch (f) {
                case DAY_OF_WEEK: return plusDays(newValue - getDayOfWeek().getValue());
                case ALIGNED_DAY_OF_WEEK_IN_MONTH: return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                case ALIGNED_DAY_OF_WEEK_IN_YEAR: return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                case DAY_OF_MONTH: return withDayOfMonth((int) newValue);
                case DAY_OF_YEAR: return withDayOfYear((int) newValue);
                case EPOCH_DAY: return LocalDate.ofEpochDay(newValue);
                case ALIGNED_WEEK_OF_MONTH: return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_MONTH));
                case ALIGNED_WEEK_OF_YEAR: return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_YEAR));
                case MONTH_OF_YEAR: return withMonth((int) newValue);
                case EPOCH_MONTH: return plusMonths(newValue - getLong(EPOCH_MONTH));
                case YEAR_OF_ERA: return withYear((int) (year >= 1 ? newValue : 1 - newValue));
                case YEAR: return withYear((int) newValue);
                case ERA: return (getLong(ERA) == newValue ? this : withYear(1 - year));
            }
            throw new DateTimeException("Unsupported field: " + field.getName());
        }
        return field.doWith(this, newValue);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this date with the year altered.
     * If the day-of-month is invalid for the year, it will be changed to the last valid day of the month.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param year  the year to set in the result, from MIN_YEAR to MAX_YEAR
     * @return a {@code LocalDate} based on this date with the requested year, not null
     * @throws DateTimeException if the year value is invalid
     */
    public LocalDate withYear(int year) {
        if (this.year == year) {
            return this;
        }
        YEAR.checkValidValue(year);
        return resolvePreviousValid(year, month, day);
    }

    /**
     * Returns a copy of this date with the month-of-year altered.
     * If the day-of-month is invalid for the year, it will be changed to the last valid day of the month.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param month  the month-of-year to set in the result, from 1 (January) to 12 (December)
     * @return a {@code LocalDate} based on this date with the requested month, not null
     * @throws DateTimeException if the month-of-year value is invalid
     */
    public LocalDate withMonth(int month) {
        if (this.month == month) {
            return this;
        }
        MONTH_OF_YEAR.checkValidValue(month);
        return resolvePreviousValid(year, month, day);
    }

    /**
     * Returns a copy of this date with the day-of-month altered.
     * If the resulting date is invalid, an exception is thrown.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param dayOfMonth  the day-of-month to set in the result, from 1 to 28-31
     * @return a {@code LocalDate} based on this date with the requested day, not null
     * @throws DateTimeException if the day-of-month value is invalid
     * @throws DateTimeException if the day-of-month is invalid for the month-year
     */
    public LocalDate withDayOfMonth(int dayOfMonth) {
        if (this.day == dayOfMonth) {
            return this;
        }
        return of(year, month, dayOfMonth);
    }

    /**
     * Returns a copy of this date with the day-of-year altered.
     * If the resulting date is invalid, an exception is thrown.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param dayOfYear  the day-of-year to set in the result, from 1 to 365-366
     * @return a {@code LocalDate} based on this date with the requested day, not null
     * @throws DateTimeException if the day-of-year value is invalid
     * @throws DateTimeException if the day-of-year is invalid for the year
     */
    public LocalDate withDayOfYear(int dayOfYear) {
        if (this.getDayOfYear() == dayOfYear) {
            return this;
        }
        return ofYearDay(year, dayOfYear);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this date with the specified period added.
     * <p>
     * This method returns a new date based on this date with the specified period added.
     * The adder is typically {@link Period} but may be any other type implementing
     * the {@link TemporalAdder} interface.
     * The calculation is delegated to the specified adjuster, which typically calls
     * back to {@link #plus(long, TemporalUnit)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param adder  the adder to use, not null
     * @return a {@code LocalDate} based on this date with the addition made, not null
     * @throws DateTimeException if the addition cannot be made
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public LocalDate plus(TemporalAdder adder) {
        return (LocalDate) adder.addTo(this);
    }

    /**
     * Returns a copy of this date with the specified period added.
     * <p>
     * This method returns a new date based on this date with the specified period added.
     * This can be used to add any period that is defined by a unit, for example to add years, months or days.
     * The unit is responsible for the details of the calculation, including the resolution
     * of any edge cases in the calculation.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToAdd  the amount of the unit to add to the result, may be negative
     * @param unit  the unit of the period to add, not null
     * @return a {@code LocalDate} based on this date with the specified period added, not null
     * @throws DateTimeException if the unit cannot be added to this type
     */
    @Override
    public LocalDate plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit f = (ChronoUnit) unit;
            switch (f) {
                case DAYS: return plusDays(amountToAdd);
                case WEEKS: return plusWeeks(amountToAdd);
                case MONTHS: return plusMonths(amountToAdd);
                case YEARS: return plusYears(amountToAdd);
                case DECADES: return plusYears(Math.multiplyExact(amountToAdd, 10));
                case CENTURIES: return plusYears(Math.multiplyExact(amountToAdd, 100));
                case MILLENNIA: return plusYears(Math.multiplyExact(amountToAdd, 1000));
                case ERAS: return with(ERA, Math.addExact(getLong(ERA), amountToAdd));
            }
            throw new DateTimeException("Unsupported unit: " + unit.getName());
        }
        return unit.doPlus(this, amountToAdd);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this {@code LocalDate} with the specified period in years added.
     * <p>
     * This method adds the specified amount to the years field in three steps:
     * <ol>
     * <li>Add the input years to the year field</li>
     * <li>Check if the resulting date would be invalid</li>
     * <li>Adjust the day-of-month to the last valid day if necessary</li>
     * </ol>
     * <p>
     * For example, 2008-02-29 (leap year) plus one year would result in the
     * invalid date 2009-02-29 (standard year). Instead of returning an invalid
     * result, the last valid day of the month, 2009-02-28, is selected instead.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param yearsToAdd  the years to add, may be negative
     * @return a {@code LocalDate} based on this date with the years added, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public LocalDate plusYears(long yearsToAdd) {
        if (yearsToAdd == 0) {
            return this;
        }
        int newYear = YEAR.checkValidIntValue(year + yearsToAdd);  // safe overflow
        return resolvePreviousValid(newYear, month, day);
    }

    /**
     * Returns a copy of this {@code LocalDate} with the specified period in months added.
     * <p>
     * This method adds the specified amount to the months field in three steps:
     * <ol>
     * <li>Add the input months to the month-of-year field</li>
     * <li>Check if the resulting date would be invalid</li>
     * <li>Adjust the day-of-month to the last valid day if necessary</li>
     * </ol>
     * <p>
     * For example, 2007-03-31 plus one month would result in the invalid date
     * 2007-04-31. Instead of returning an invalid result, the last valid day
     * of the month, 2007-04-30, is selected instead.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param monthsToAdd  the months to add, may be negative
     * @return a {@code LocalDate} based on this date with the months added, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public LocalDate plusMonths(long monthsToAdd) {
        if (monthsToAdd == 0) {
            return this;
        }
        long monthCount = year * 12L + (month - 1);
        long calcMonths = monthCount + monthsToAdd;  // safe overflow
        int newYear = YEAR.checkValidIntValue(Math.floorDiv(calcMonths, 12));
        int newMonth = (int)Math.floorMod(calcMonths, 12) + 1;
        return resolvePreviousValid(newYear, newMonth, day);
    }

    /**
     * Returns a copy of this {@code LocalDate} with the specified period in weeks added.
     * <p>
     * This method adds the specified amount in weeks to the days field incrementing
     * the month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2008-12-31 plus one week would result in 2009-01-07.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param weeksToAdd  the weeks to add, may be negative
     * @return a {@code LocalDate} based on this date with the weeks added, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public LocalDate plusWeeks(long weeksToAdd) {
        return plusDays(Math.multiplyExact(weeksToAdd, 7));
    }

    /**
     * Returns a copy of this {@code LocalDate} with the specified number of days added.
     * <p>
     * This method adds the specified amount to the days field incrementing the
     * month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2008-12-31 plus one day would result in 2009-01-01.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToAdd  the days to add, may be negative
     * @return a {@code LocalDate} based on this date with the days added, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public LocalDate plusDays(long daysToAdd) {
        if (daysToAdd == 0) {
            return this;
        }
        long mjDay = Math.addExact(toEpochDay(), daysToAdd);
        return LocalDate.ofEpochDay(mjDay);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this date with the specified period subtracted.
     * <p>
     * This method returns a new date based on this date with the specified period subtracted.
     * The subtractor is typically {@link Period} but may be any other type implementing
     * the {@link TemporalSubtractor} interface.
     * The calculation is delegated to the specified adjuster, which typically calls
     * back to {@link #minus(long, TemporalUnit)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param subtractor  the subtractor to use, not null
     * @return a {@code LocalDate} based on this date with the subtraction made, not null
     * @throws DateTimeException if the subtraction cannot be made
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public LocalDate minus(TemporalSubtractor subtractor) {
        return (LocalDate) subtractor.subtractFrom(this);
    }

    /**
     * Returns a copy of this date with the specified period subtracted.
     * <p>
     * This method returns a new date based on this date with the specified period subtracted.
     * This can be used to subtract any period that is defined by a unit, for example to subtract years, months or days.
     * The unit is responsible for the details of the calculation, including the resolution
     * of any edge cases in the calculation.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToSubtract  the amount of the unit to subtract from the result, may be negative
     * @param unit  the unit of the period to subtract, not null
     * @return a {@code LocalDate} based on this date with the specified period subtracted, not null
     * @throws DateTimeException if the unit cannot be added to this type
     */
    @Override
    public LocalDate minus(long amountToSubtract, TemporalUnit unit) {
        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit));
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this {@code LocalDate} with the specified period in years subtracted.
     * <p>
     * This method subtracts the specified amount from the years field in three steps:
     * <ol>
     * <li>Subtract the input years to the year field</li>
     * <li>Check if the resulting date would be invalid</li>
     * <li>Adjust the day-of-month to the last valid day if necessary</li>
     * </ol>
     * <p>
     * For example, 2008-02-29 (leap year) minus one year would result in the
     * invalid date 2007-02-29 (standard year). Instead of returning an invalid
     * result, the last valid day of the month, 2007-02-28, is selected instead.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param yearsToSubtract  the years to subtract, may be negative
     * @return a {@code LocalDate} based on this date with the years subtracted, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public LocalDate minusYears(long yearsToSubtract) {
        return (yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-yearsToSubtract));
    }

    /**
     * Returns a copy of this {@code LocalDate} with the specified period in months subtracted.
     * <p>
     * This method subtracts the specified amount from the months field in three steps:
     * <ol>
     * <li>Subtract the input months to the month-of-year field</li>
     * <li>Check if the resulting date would be invalid</li>
     * <li>Adjust the day-of-month to the last valid day if necessary</li>
     * </ol>
     * <p>
     * For example, 2007-03-31 minus one month would result in the invalid date
     * 2007-02-31. Instead of returning an invalid result, the last valid day
     * of the month, 2007-02-28, is selected instead.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param monthsToSubtract  the months to subtract, may be negative
     * @return a {@code LocalDate} based on this date with the months subtracted, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public LocalDate minusMonths(long monthsToSubtract) {
        return (monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1) : plusMonths(-monthsToSubtract));
    }

    /**
     * Returns a copy of this {@code LocalDate} with the specified period in weeks subtracted.
     * <p>
     * This method subtracts the specified amount in weeks from the days field decrementing
     * the month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2009-01-07 minus one week would result in 2008-12-31.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param weeksToSubtract  the weeks to subtract, may be negative
     * @return a {@code LocalDate} based on this date with the weeks subtracted, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public LocalDate minusWeeks(long weeksToSubtract) {
        return (weeksToSubtract == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeksToSubtract));
    }

    /**
     * Returns a copy of this {@code LocalDate} with the specified number of days subtracted.
     * <p>
     * This method subtracts the specified amount from the days field decrementing the
     * month and year fields as necessary to ensure the result remains valid.
     * The result is only invalid if the maximum/minimum year is exceeded.
     * <p>
     * For example, 2009-01-01 minus one day would result in 2008-12-31.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToSubtract  the days to subtract, may be negative
     * @return a {@code LocalDate} based on this date with the days subtracted, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public LocalDate minusDays(long daysToSubtract) {
        return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
    }

    //-----------------------------------------------------------------------
    /**
     * Queries this date using the specified query.
     * <p>
     * This queries this date using the specified query strategy object.
     * The {@code TemporalQuery} object defines the logic to be used to
     * obtain the result. Read the documentation of the query to understand
     * what the result of this method will be.
     * <p>
     * The result of this method is obtained by invoking the
     * {@link TemporalQuery#queryFrom(TemporalAccessor)} method on the
     * specified query passing {@code this} as the argument.
     *
     * @param <R> the type of the result
     * @param query  the query to invoke, not null
     * @return the query result, null may be returned (defined by the query)
     * @throws DateTimeException if unable to query (defined by the query)
     * @throws ArithmeticException if numeric overflow occurs (defined by the query)
     */
    @Override  // override for Javadoc
    public <R> R query(TemporalQuery<R> query) {
        return ChronoLocalDate.super.query(query);
    }

    /**
     * Adjusts the specified temporal object to have the same date as this object.
     * <p>
     * This returns a temporal object of the same observable type as the input
     * with the date changed to be the same as this.
     * <p>
     * The adjustment is equivalent to using {@link Temporal#with(TemporalField, long)}
     * passing {@link ChronoField#EPOCH_DAY} as the field.
     * <p>
     * In most cases, it is clearer to reverse the calling pattern by using
     * {@link Temporal#with(TemporalAdjuster)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisLocalDate.adjustInto(temporal);
     *   temporal = temporal.with(thisLocalDate);
     * </pre>
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param temporal  the target object to be adjusted, not null
     * @return the adjusted object, not null
     * @throws DateTimeException if unable to make the adjustment
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override  // override for Javadoc
    public Temporal adjustInto(Temporal temporal) {
        return ChronoLocalDate.super.adjustInto(temporal);
    }

    /**
     * Calculates the period between this date and another date in
     * terms of the specified unit.
     * <p>
     * This calculates the period between two dates in terms of a single unit.
     * The start and end points are {@code this} and the specified date.
     * The result will be negative if the end is before the start.
     * The {@code Temporal} passed to this method must be a {@code LocalDate}.
     * For example, the period in days between two dates can be calculated
     * using {@code startDate.periodUntil(endDate, DAYS)}.
     * <p>
     * The calculation returns a whole number, representing the number of
     * complete units between the two dates.
     * For example, the period in months between 2012-06-15 and 2012-08-14
     * will only be one month as it is one day short of two months.
     * <p>
     * This method operates in association with {@link TemporalUnit#between}.
     * The result of this method is a {@code long} representing the amount of
     * the specified unit. By contrast, the result of {@code between} is an
     * object that can be used directly in addition/subtraction:
     * <pre>
     *   long period = start.periodUntil(end, MONTHS);   // this method
     *   dateTime.plus(MONTHS.between(start, end));      // use in plus/minus
     * </pre>
     * <p>
     * The calculation is implemented in this method for {@link ChronoUnit}.
     * The units {@code DAYS}, {@code WEEKS}, {@code MONTHS}, {@code YEARS},
     * {@code DECADES}, {@code CENTURIES}, {@code MILLENNIA} and {@code ERAS}
     * are supported. Other {@code ChronoUnit} values will throw an exception.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method
     * is obtained by invoking {@code TemporalUnit.between(Temporal, Temporal)}
     * passing {@code this} as the first argument and the input temporal as
     * the second argument.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param endDate  the end date, which must be a {@code LocalDate}, not null
     * @param unit  the unit to measure the period in, not null
     * @return the amount of the period between this date and the end date
     * @throws DateTimeException if the period cannot be calculated
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public long periodUntil(Temporal endDate, TemporalUnit unit) {
        if (endDate instanceof LocalDate == false) {
            Objects.requireNonNull(endDate, "endDate");
            throw new DateTimeException("Unable to calculate period between objects of two different types");
        }
        LocalDate end = (LocalDate) endDate;
        if (unit instanceof ChronoUnit) {
            switch ((ChronoUnit) unit) {
                case DAYS: return daysUntil(end);
                case WEEKS: return daysUntil(end) / 7;
                case MONTHS: return monthsUntil(end);
                case YEARS: return monthsUntil(end) / 12;
                case DECADES: return monthsUntil(end) / 120;
                case CENTURIES: return monthsUntil(end) / 1200;
                case MILLENNIA: return monthsUntil(end) / 12000;
                case ERAS: return end.getLong(ERA) - getLong(ERA);
            }
            throw new DateTimeException("Unsupported unit: " + unit.getName());
        }
        return unit.between(this, endDate).getAmount();
    }

    long daysUntil(LocalDate end) {
        return end.toEpochDay() - toEpochDay();  // no overflow
    }

    private long monthsUntil(LocalDate end) {
        long packed1 = getEpochMonth() * 32L + getDayOfMonth();  // no overflow
        long packed2 = end.getEpochMonth() * 32L + end.getDayOfMonth();  // no overflow
        return (packed2 - packed1) / 32;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a local date-time formed from this date at the specified time.
     * <p>
     * This combines this date with the specified time to form a {@code LocalDateTime}.
     * All possible combinations of date and time are valid.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param time  the time to combine with, not null
     * @return the local date-time formed from this date and the specified time, not null
     */
    @Override
    public LocalDateTime atTime(LocalTime time) {
        return LocalDateTime.of(this, time);
    }

    /**
     * Returns a local date-time formed from this date at the specified time.
     * <p>
     * This combines this date with the specified time to form a {@code LocalDateTime}.
     * The individual time fields must be within their valid range.
     * All possible combinations of date and time are valid.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param hour  the hour-of-day to use, from 0 to 23
     * @param minute  the minute-of-hour to use, from 0 to 59
     * @return the local date-time formed from this date and the specified time, not null
     * @throws DateTimeException if the value of any field is out of range
     */
    public LocalDateTime atTime(int hour, int minute) {
        return atTime(LocalTime.of(hour, minute));
    }

    /**
     * Returns a local date-time formed from this date at the specified time.
     * <p>
     * This combines this date with the specified time to form a {@code LocalDateTime}.
     * The individual time fields must be within their valid range.
     * All possible combinations of date and time are valid.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param hour  the hour-of-day to use, from 0 to 23
     * @param minute  the minute-of-hour to use, from 0 to 59
     * @param second  the second-of-minute to represent, from 0 to 59
     * @return the local date-time formed from this date and the specified time, not null
     * @throws DateTimeException if the value of any field is out of range
     */
    public LocalDateTime atTime(int hour, int minute, int second) {
        return atTime(LocalTime.of(hour, minute, second));
    }

    /**
     * Returns a local date-time formed from this date at the specified time.
     * <p>
     * This combines this date with the specified time to form a {@code LocalDateTime}.
     * The individual time fields must be within their valid range.
     * All possible combinations of date and time are valid.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param hour  the hour-of-day to use, from 0 to 23
     * @param minute  the minute-of-hour to use, from 0 to 59
     * @param second  the second-of-minute to represent, from 0 to 59
     * @param nanoOfSecond  the nano-of-second to represent, from 0 to 999,999,999
     * @return the local date-time formed from this date and the specified time, not null
     * @throws DateTimeException if the value of any field is out of range
     */
    public LocalDateTime atTime(int hour, int minute, int second, int nanoOfSecond) {
        return atTime(LocalTime.of(hour, minute, second, nanoOfSecond));
    }

    /**
     * Returns an offset date formed from this date and the specified offset.
     * <p>
     * This combines this date with the specified offset to form an {@code OffsetDate}.
     * All possible combinations of date and offset are valid.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param offset  the offset to combine with, not null
     * @return the offset date formed from this date and the specified offset, not null
     */
    public OffsetDate atOffset(ZoneOffset offset) {
        return OffsetDate.of(this, offset);
    }

    /**
     * Returns a zoned date-time from this date at the earliest valid time according
     * to the rules in the time-zone.
     * <p>
     * Time-zone rules, such as daylight savings, mean that not every local date-time
     * is valid for the specified zone, thus the local date-time may not be midnight.
     * <p>
     * In most cases, there is only one valid offset for a local date-time.
     * In the case of an overlap, there are two valid offsets, and the earlier one is used,
     * corresponding to the first occurrence of midnight on the date.
     * In the case of a gap, the zoned date-time will represent the instant just after the gap.
     * <p>
     * If the zone ID is a {@link ZoneOffset}, then the result always has a time of midnight.
     * <p>
     * To convert to a specific time in a given time-zone call {@link #atTime(LocalTime)}
     * followed by {@link LocalDateTime#atZone(ZoneId)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param zone  the zone ID to use, not null
     * @return the zoned date-time formed from this date and the earliest valid time for the zone, not null
     */
    public ZonedDateTime atStartOfDay(ZoneId zone) {
        Objects.requireNonNull(zone, "zone");
        // need to handle case where there is a gap from 11:30 to 00:30
        // standard ZDT factory would result in 01:00 rather than 00:30
        LocalDateTime ldt = atTime(LocalTime.MIDNIGHT);
        if (zone instanceof ZoneOffset == false) {
            ZoneRules rules = zone.getRules();
            ZoneOffsetTransition trans = rules.getTransition(ldt);
            if (trans != null && trans.isGap()) {
                ldt = trans.getDateTimeAfter();
            }
        }
        return ZonedDateTime.of(ldt, zone);
    }

    //-----------------------------------------------------------------------
    @Override
    public long toEpochDay() {
        long y = year;
        long m = month;
        long total = 0;
        total += 365 * y;
        if (y >= 0) {
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
        } else {
            total -= y / -4 - y / -100 + y / -400;
        }
        total += ((367 * m - 362) / 12);
        total += day - 1;
        if (m > 2) {
            total--;
            if (isLeapYear() == false) {
                total--;
            }
        }
        return total - DAYS_0000_TO_1970;
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this date to another date.
     * <p>
     * The comparison is primarily based on the date, from earliest to latest.
     * It is "consistent with equals", as defined by {@link Comparable}.
     * <p>
     * If all the dates being compared are instances of {@code LocalDate},
     * then the comparison will be entirely based on the date.
     * If some dates being compared are in different chronologies, then the
     * chronology is also considered, see {@link java.time.temporal.ChronoLocalDate#compareTo}.
     *
     * @param other  the other date to compare to, not null
     * @return the comparator value, negative if less, positive if greater
     */
    @Override  // override for Javadoc and performance
    public int compareTo(ChronoLocalDate<?> other) {
        if (other instanceof LocalDate) {
            return compareTo0((LocalDate) other);
        }
        return ChronoLocalDate.super.compareTo(other);
    }

    int compareTo0(LocalDate otherDate) {
        int cmp = (year - otherDate.year);
        if (cmp == 0) {
            cmp = (month - otherDate.month);
            if (cmp == 0) {
                cmp = (day - otherDate.day);
            }
        }
        return cmp;
    }

    /**
     * Checks if this date is after the specified date.
     * <p>
     * This checks to see if this date represents a point on the
     * local time-line after the other date.
     * <pre>
     *   LocalDate a = LocalDate.of(2012, 6, 30);
     *   LocalDate b = LocalDate.of(2012, 7, 1);
     *   a.isAfter(b) == false
     *   a.isAfter(a) == false
     *   b.isAfter(a) == true
     * </pre>
     * <p>
     * This method only considers the position of the two dates on the local time-line.
     * It does not take into account the chronology, or calendar system.
     * This is different from the comparison in {@link #compareTo(ChronoLocalDate)},
     * but is the same approach as {@link #DATE_COMPARATOR}.
     *
     * @param other  the other date to compare to, not null
     * @return true if this date is after the specified date
     */
    @Override  // override for Javadoc and performance
    public boolean isAfter(ChronoLocalDate<?> other) {
        if (other instanceof LocalDate) {
            return compareTo0((LocalDate) other) > 0;
        }
        return ChronoLocalDate.super.isAfter(other);
    }

    /**
     * Checks if this date is before the specified date.
     * <p>
     * This checks to see if this date represents a point on the
     * local time-line before the other date.
     * <pre>
     *   LocalDate a = LocalDate.of(2012, 6, 30);
     *   LocalDate b = LocalDate.of(2012, 7, 1);
     *   a.isBefore(b) == true
     *   a.isBefore(a) == false
     *   b.isBefore(a) == false
     * </pre>
     * <p>
     * This method only considers the position of the two dates on the local time-line.
     * It does not take into account the chronology, or calendar system.
     * This is different from the comparison in {@link #compareTo(ChronoLocalDate)},
     * but is the same approach as {@link #DATE_COMPARATOR}.
     *
     * @param other  the other date to compare to, not null
     * @return true if this date is before the specified date
     */
    @Override  // override for Javadoc and performance
    public boolean isBefore(ChronoLocalDate<?> other) {
        if (other instanceof LocalDate) {
            return compareTo0((LocalDate) other) < 0;
        }
        return ChronoLocalDate.super.isBefore(other);
    }

    /**
     * Checks if this date is equal to the specified date.
     * <p>
     * This checks to see if this date represents the same point on the
     * local time-line as the other date.
     * <pre>
     *   LocalDate a = LocalDate.of(2012, 6, 30);
     *   LocalDate b = LocalDate.of(2012, 7, 1);
     *   a.isEqual(b) == false
     *   a.isEqual(a) == true
     *   b.isEqual(a) == false
     * </pre>
     * <p>
     * This method only considers the position of the two dates on the local time-line.
     * It does not take into account the chronology, or calendar system.
     * This is different from the comparison in {@link #compareTo(ChronoLocalDate)}
     * but is the same approach as {@link #DATE_COMPARATOR}.
     *
     * @param other  the other date to compare to, not null
     * @return true if this date is equal to the specified date
     */
    @Override  // override for Javadoc and performance
    public boolean isEqual(ChronoLocalDate<?> other) {
        if (other instanceof LocalDate) {
            return compareTo0((LocalDate) other) == 0;
        }
        return ChronoLocalDate.super.isEqual(other);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this date is equal to another date.
     * <p>
     * Compares this {@code LocalDate} with another ensuring that the date is the same.
     * <p>
     * Only objects of type {@code LocalDate} are compared, other types return false.
     * To compare the dates of two {@code TemporalAccessor} instances, including dates
     * in two different chronologies, use {@link ChronoField#EPOCH_DAY} as a comparator.
     *
     * @param obj  the object to check, null returns false
     * @return true if this is equal to the other date
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LocalDate) {
            return compareTo0((LocalDate) obj) == 0;
        }
        return false;
    }

    /**
     * A hash code for this date.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        int yearValue = year;
        int monthValue = month;
        int dayValue = day;
        return (yearValue & 0xFFFFF800) ^ ((yearValue << 11) + (monthValue << 6) + (dayValue));
    }

    //-----------------------------------------------------------------------
    /**
     * Outputs this date as a {@code String}, such as {@code 2007-12-03}.
     * <p>
     * The output will be in the ISO-8601 format {@code yyyy-MM-dd}.
     *
     * @return a string representation of this date, not null
     */
    @Override
    public String toString() {
        int yearValue = year;
        int monthValue = month;
        int dayValue = day;
        int absYear = Math.abs(yearValue);
        StringBuilder buf = new StringBuilder(10);
        if (absYear < 1000) {
            if (yearValue < 0) {
                buf.append(yearValue - 10000).deleteCharAt(1);
            } else {
                buf.append(yearValue + 10000).deleteCharAt(0);
            }
        } else {
            if (yearValue > 9999) {
                buf.append('+');
            }
            buf.append(yearValue);
        }
        return buf.append(monthValue < 10 ? "-0" : "-")
            .append(monthValue)
            .append(dayValue < 10 ? "-0" : "-")
            .append(dayValue)
            .toString();
    }

    /**
     * Outputs this date as a {@code String} using the formatter.
     * <p>
     * This date will be passed to the formatter
     * {@link DateTimeFormatter#print(TemporalAccessor) print method}.
     *
     * @param formatter  the formatter to use, not null
     * @return the formatted date string, not null
     * @throws DateTimeException if an error occurs during printing
     */
    @Override  // override for Javadoc
    public String toString(DateTimeFormatter formatter) {
        return ChronoLocalDate.super.toString(formatter);
    }

    //-----------------------------------------------------------------------
    /**
     * Writes the object using a
     * <a href="../../serialized-form.html#java.time.Ser">dedicated serialized form</a>.
     * <pre>
     *  out.writeByte(3);  // identifies this as a LocalDate
     *  out.writeInt(year);
     *  out.writeByte(month);
     *  out.writeByte(day);
     * </pre>
     *
     * @return the instance of {@code Ser}, not null
     */
    private Object writeReplace() {
        return new Ser(Ser.LOCAL_DATE_TYPE, this);
    }

    /**
     * Defend against malicious streams.
     * @return never
     * @throws InvalidObjectException always
     */
    private Object readResolve() throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeInt(year);
        out.writeByte(month);
        out.writeByte(day);
    }

    static LocalDate readExternal(DataInput in) throws IOException {
        int year = in.readInt();
        int month = in.readByte();
        int dayOfMonth = in.readByte();
        return LocalDate.of(year, month, dayOfMonth);
    }

}
