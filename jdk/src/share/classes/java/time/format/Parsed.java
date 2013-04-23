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
 * Copyright (c) 2008-2013, Stephen Colebourne & Michael Nascimento Santos
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
package java.time.format;

import static java.time.temporal.ChronoField.AMPM_OF_DAY;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_DAY;
import static java.time.temporal.ChronoField.HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * A store of parsed data.
 * <p>
 * This class is used during parsing to collect the data. Part of the parsing process
 * involves handling optional blocks and multiple copies of the data get created to
 * support the necessary backtracking.
 * <p>
 * Once parsing is completed, this class can be used as the resultant {@code TemporalAccessor}.
 * In most cases, it is only exposed once the fields have been resolved.
 *
 * <h3>Specification for implementors</h3>
 * This class is a mutable context intended for use from a single thread.
 * Usage of the class is thread-safe within standard parsing as a new instance of this class
 * is automatically created for each parse and parsing is single-threaded
 *
 * @since 1.8
 */
final class Parsed implements TemporalAccessor {
    // some fields are accessed using package scope from DateTimeParseContext

    /**
     * The parsed fields.
     */
    final Map<TemporalField, Long> fieldValues = new HashMap<>();
    /**
     * The parsed zone.
     */
    ZoneId zone;
    /**
     * The parsed chronology.
     */
    Chronology chrono;
    /**
     * The effective chronology.
     */
    Chronology effectiveChrono;
    /**
     * The resolver style to use.
     */
    private ResolverStyle resolverStyle;
    /**
     * The resolved date.
     */
    private ChronoLocalDate<?> date;
    /**
     * The resolved time.
     */
    private LocalTime time;

    /**
     * Creates an instance.
     */
    Parsed() {
    }

    /**
     * Creates a copy.
     */
    Parsed copy() {
        // only copy fields used in parsing stage
        Parsed cloned = new Parsed();
        cloned.fieldValues.putAll(this.fieldValues);
        cloned.zone = this.zone;
        cloned.chrono = this.chrono;
        return cloned;
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TemporalField field) {
        if (fieldValues.containsKey(field) ||
                (date != null && date.isSupported(field)) ||
                (time != null && time.isSupported(field))) {
            return true;
        }
        return field != null && (field instanceof ChronoField == false) && field.isSupportedBy(this);
    }

    @Override
    public long getLong(TemporalField field) {
        Objects.requireNonNull(field, "field");
        Long value = fieldValues.get(field);
        if (value != null) {
            return value;
        }
        if (date != null && date.isSupported(field)) {
            return date.getLong(field);
        }
        if (time != null && time.isSupported(field)) {
            return time.getLong(field);
        }
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field.getName());
        }
        return field.getFrom(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQuery.zoneId()) {
            return (R) zone;
        } else if (query == TemporalQuery.chronology()) {
            return (R) chrono;
        } else if (query == TemporalQuery.localDate()) {
            return (R) (date != null ? LocalDate.from(date) : null);
        } else if (query == TemporalQuery.localTime()) {
            return (R) time;
        } else if (query == TemporalQuery.zone() || query == TemporalQuery.offset()) {
            return query.queryFrom(this);
        } else if (query == TemporalQuery.precision()) {
            return null;  // not a complete date/time
        }
        // inline TemporalAccessor.super.query(query) as an optimization
        // non-JDK classes are not permitted to make this optimization
        return query.queryFrom(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Resolves the fields in this context.
     *
     * @param resolverStyle  the resolver style, not null
     * @param resolverFields  the fields to use for resolving, null for all fields
     * @return this, for method chaining
     * @throws DateTimeException if resolving one field results in a value for
     *  another field that is in conflict
     */
    TemporalAccessor resolve(ResolverStyle resolverStyle, Set<TemporalField> resolverFields) {
        if (resolverFields != null) {
            fieldValues.keySet().retainAll(resolverFields);
        }
        this.resolverStyle = resolverStyle;
        chrono = effectiveChrono;
        resolveFields();
        resolveTimeLenient();
        crossCheck();
        return this;
    }

    //-----------------------------------------------------------------------
    private void resolveFields() {
        // resolve ChronoField
        resolveDateFields();
        resolveTimeFields();

        // if any other fields, handle them
        // any lenient date resolution should return epoch-day
        if (fieldValues.size() > 0) {
            boolean changed = false;
            outer:
            while (true) {
                for (Map.Entry<TemporalField, Long> entry : fieldValues.entrySet()) {
                    TemporalField targetField = entry.getKey();
                    Map<TemporalField, Long> changes = targetField.resolve(this, entry.getValue(), resolverStyle);
                    if (changes != null) {
                        changed = true;
                        resolveFieldsMakeChanges(targetField, changes);
                        fieldValues.remove(targetField);  // helps avoid infinite loops
                        continue outer;  // have to restart to avoid concurrent modification
                    }
                }
                break;
            }
            // if something changed then have to redo ChronoField resolve
            if (changed) {
                resolveDateFields();
                resolveTimeFields();
            }
        }
    }

    private void resolveFieldsMakeChanges(TemporalField targetField, Map<TemporalField, Long> changes) {
        for (Map.Entry<TemporalField, Long> change : changes.entrySet()) {
            TemporalField changeField = change.getKey();
            Long changeValue = change.getValue();
            Objects.requireNonNull(changeField, "changeField");
            if (changeValue != null) {
                updateCheckConflict(targetField, changeField, changeValue);
            } else {
                fieldValues.remove(changeField);
            }
        }
    }

    private void updateCheckConflict(TemporalField targetField, TemporalField changeField, Long changeValue) {
        Long old = fieldValues.put(changeField, changeValue);
        if (old != null && old.longValue() != changeValue.longValue()) {
            throw new DateTimeException("Conflict found: " + changeField + " " + old +
                    " differs from " + changeField + " " + changeValue +
                    " while resolving  " + targetField);
        }
    }

    //-----------------------------------------------------------------------
    private void resolveDateFields() {
        updateCheckConflict(chrono.resolveDate(fieldValues, resolverStyle));
    }

    private void updateCheckConflict(ChronoLocalDate<?> cld) {
        if (date != null) {
            if (cld != null && date.equals(cld) == false) {
                throw new DateTimeException("Conflict found: Fields resolved to two different dates: " + date + " " + cld);
            }
        } else {
            date = cld;
        }
    }

    //-----------------------------------------------------------------------
    private void resolveTimeFields() {
        // simplify fields
        if (fieldValues.containsKey(CLOCK_HOUR_OF_DAY)) {
            long ch = fieldValues.remove(CLOCK_HOUR_OF_DAY);
            updateCheckConflict(CLOCK_HOUR_OF_DAY, HOUR_OF_DAY, ch == 24 ? 0 : ch);
        }
        if (fieldValues.containsKey(CLOCK_HOUR_OF_AMPM)) {
            long ch = fieldValues.remove(CLOCK_HOUR_OF_AMPM);
            updateCheckConflict(CLOCK_HOUR_OF_AMPM, HOUR_OF_AMPM, ch == 12 ? 0 : ch);
        }
        if (fieldValues.containsKey(AMPM_OF_DAY) && fieldValues.containsKey(HOUR_OF_AMPM)) {
            long ap = fieldValues.remove(AMPM_OF_DAY);
            long hap = fieldValues.remove(HOUR_OF_AMPM);
            updateCheckConflict(AMPM_OF_DAY, HOUR_OF_DAY, ap * 12 + hap);
        }
        if (fieldValues.containsKey(MICRO_OF_DAY)) {
            long cod = fieldValues.remove(MICRO_OF_DAY);
            updateCheckConflict(MICRO_OF_DAY, SECOND_OF_DAY, cod / 1_000_000L);
            updateCheckConflict(MICRO_OF_DAY, MICRO_OF_SECOND, cod % 1_000_000L);
        }
        if (fieldValues.containsKey(MILLI_OF_DAY)) {
            long lod = fieldValues.remove(MILLI_OF_DAY);
            updateCheckConflict(MILLI_OF_DAY, SECOND_OF_DAY, lod / 1_000);
            updateCheckConflict(MILLI_OF_DAY, MILLI_OF_SECOND, lod % 1_000);
        }
        if (fieldValues.containsKey(SECOND_OF_DAY)) {
            long sod = fieldValues.remove(SECOND_OF_DAY);
            updateCheckConflict(SECOND_OF_DAY, HOUR_OF_DAY, sod / 3600);
            updateCheckConflict(SECOND_OF_DAY, MINUTE_OF_HOUR, (sod / 60) % 60);
            updateCheckConflict(SECOND_OF_DAY, SECOND_OF_MINUTE, sod % 60);
        }
        if (fieldValues.containsKey(MINUTE_OF_DAY)) {
            long mod = fieldValues.remove(MINUTE_OF_DAY);
            updateCheckConflict(MINUTE_OF_DAY, HOUR_OF_DAY, mod / 60);
            updateCheckConflict(MINUTE_OF_DAY, MINUTE_OF_HOUR, mod % 60);
        }

        // combine partial second fields strictly, leaving lenient expansion to later
        if (fieldValues.containsKey(NANO_OF_SECOND)) {
            long nos = fieldValues.get(NANO_OF_SECOND);
            if (fieldValues.containsKey(MICRO_OF_SECOND)) {
                long cos = fieldValues.remove(MICRO_OF_SECOND);
                nos = cos * 1000 + (nos % 1000);
                updateCheckConflict(MICRO_OF_SECOND, NANO_OF_SECOND, nos);
            }
            if (fieldValues.containsKey(MILLI_OF_SECOND)) {
                long los = fieldValues.remove(MILLI_OF_SECOND);
                updateCheckConflict(MILLI_OF_SECOND, NANO_OF_SECOND, los * 1_000_000L + (nos % 1_000_000L));
            }
        }

        // convert to time if possible
        if (fieldValues.containsKey(NANO_OF_DAY)) {
            long nod = fieldValues.remove(NANO_OF_DAY);
            updateCheckConflict(LocalTime.ofNanoOfDay(nod));
        }
        if (fieldValues.containsKey(HOUR_OF_DAY) && fieldValues.containsKey(MINUTE_OF_HOUR) &&
                fieldValues.containsKey(SECOND_OF_MINUTE) && fieldValues.containsKey(NANO_OF_SECOND)) {
            int hodVal = HOUR_OF_DAY.checkValidIntValue(fieldValues.remove(HOUR_OF_DAY));
            int mohVal = MINUTE_OF_HOUR.checkValidIntValue(fieldValues.remove(MINUTE_OF_HOUR));
            int somVal = SECOND_OF_MINUTE.checkValidIntValue(fieldValues.remove(SECOND_OF_MINUTE));
            int nosVal = NANO_OF_SECOND.checkValidIntValue(fieldValues.remove(NANO_OF_SECOND));
            updateCheckConflict(LocalTime.of(hodVal, mohVal, somVal, nosVal));
        }
    }

    private void resolveTimeLenient() {
        // leniently create a time from incomplete information
        // done after everything else as it creates information from nothing
        // which would break updateCheckConflict(field)

        if (time == null) {
            // can only get here if NANO_OF_SECOND not present
            if (fieldValues.containsKey(MILLI_OF_SECOND)) {
                long los = fieldValues.remove(MILLI_OF_SECOND);
                if (fieldValues.containsKey(MICRO_OF_SECOND)) {
                    // merge milli-of-second and micro-of-second for better error message
                    long cos = los * 1_000 + (fieldValues.get(MICRO_OF_SECOND) % 1_000);
                    updateCheckConflict(MILLI_OF_SECOND, MICRO_OF_SECOND, cos);
                    fieldValues.remove(MICRO_OF_SECOND);
                    fieldValues.put(NANO_OF_SECOND, cos * 1_000L);
                } else {
                    // convert milli-of-second to nano-of-second
                    fieldValues.put(NANO_OF_SECOND, los * 1_000_000L);
                }
            } else if (fieldValues.containsKey(MICRO_OF_SECOND)) {
                // convert micro-of-second to nano-of-second
                long cos = fieldValues.remove(MICRO_OF_SECOND);
                fieldValues.put(NANO_OF_SECOND, cos * 1_000L);
            }
        }

        // merge hour/minute/second/nano leniently
        Long hod = fieldValues.get(HOUR_OF_DAY);
        if (hod != null) {
            int hodVal = HOUR_OF_DAY.checkValidIntValue(hod);
            Long moh = fieldValues.get(MINUTE_OF_HOUR);
            Long som = fieldValues.get(SECOND_OF_MINUTE);
            Long nos = fieldValues.get(NANO_OF_SECOND);

            // check for invalid combinations that cannot be defaulted
            if (time == null) {
                if ((moh == null && (som != null || nos != null)) ||
                        (moh != null && som == null && nos != null)) {
                    return;
                }
            }

            // default as necessary and build time
            int mohVal = (moh != null ? MINUTE_OF_HOUR.checkValidIntValue(moh) : (time != null ? time.getMinute() : 0));
            int somVal = (som != null ? SECOND_OF_MINUTE.checkValidIntValue(som) : (time != null ? time.getSecond() : 0));
            int nosVal = (nos != null ? NANO_OF_SECOND.checkValidIntValue(nos) : (time != null ? time.getNano() : 0));
            updateCheckConflict(LocalTime.of(hodVal, mohVal, somVal, nosVal));
            fieldValues.remove(HOUR_OF_DAY);
            fieldValues.remove(MINUTE_OF_HOUR);
            fieldValues.remove(SECOND_OF_MINUTE);
            fieldValues.remove(NANO_OF_SECOND);
        }
    }

    private void updateCheckConflict(LocalTime lt) {
        if (time != null) {
            if (lt != null && time.equals(lt) == false) {
                throw new DateTimeException("Conflict found: Fields resolved to two different times: " + time + " " + lt);
            }
        } else {
            time = lt;
        }
    }

    //-----------------------------------------------------------------------
    private void crossCheck() {
        // only cross-check date, time and date-time
        // avoid object creation if possible
        if (date != null) {
            crossCheck(date);
        }
        if (time != null) {
            crossCheck(time);
            if (date != null && fieldValues.size() > 0) {
                crossCheck(date.atTime(time));
            }
        }
    }

    private void crossCheck(TemporalAccessor target) {
        for (Iterator<Entry<TemporalField, Long>> it = fieldValues.entrySet().iterator(); it.hasNext(); ) {
            Entry<TemporalField, Long> entry = it.next();
            TemporalField field = entry.getKey();
            long val1;
            try {
                val1 = target.getLong(field);
            } catch (RuntimeException ex) {
                continue;
            }
            long val2 = entry.getValue();
            if (val1 != val2) {
                throw new DateTimeException("Conflict found: Field " + field + " " + val1 +
                        " differs from " + field + " " + val2 + " derived from " + target);
            }
            it.remove();
        }
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        String str = fieldValues.toString() + "," + chrono + "," + zone;
        if (date != null || time != null) {
            str += " resolved to " + date + "," + time;
        }
        return str;
    }

}
