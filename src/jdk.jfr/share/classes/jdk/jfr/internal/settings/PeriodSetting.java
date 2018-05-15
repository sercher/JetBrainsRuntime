/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.settings;

import java.util.Objects;
import java.util.Set;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.Name;
import jdk.jfr.internal.PlatformEventType;
import jdk.jfr.internal.Control;
import jdk.jfr.internal.Type;
import jdk.jfr.internal.Utils;

@MetadataDefinition
@Label("Period")
@Description("Record event at interval")
@Name(Type.SETTINGS_PREFIX + "Period")
public final class PeriodSetting extends Control {
    private static final long typeId = Type.getTypeId(PeriodSetting.class);

    public static final String EVERY_CHUNK = "everyChunk";
    public static final String BEGIN_CHUNK = "beginChunk";
    public static final String END_CHUNK = "endChunk";
    public static final String NAME = "period";
    private final PlatformEventType eventType;
    private String value = EVERY_CHUNK;

    public PeriodSetting(PlatformEventType eventType, String defaultValue) {
        super(defaultValue);
        this.eventType = Objects.requireNonNull(eventType);
    }

    @Override
    public String combine(Set<String> values) {
        long min = Long.MAX_VALUE;
        boolean beginChunk = false;
        boolean endChunk = false;
        String text = EVERY_CHUNK;
        for (String value : values) {
            switch (value) {
            case EVERY_CHUNK:
                beginChunk = true;
                endChunk = true;
                break;
            case BEGIN_CHUNK:
                beginChunk = true;
                break;
            case END_CHUNK:
                endChunk = true;
                break;
            default:
                long l = Utils.parseTimespan(value);
                if (l < min) {
                    text = value;
                    min = l;
                }
            }
        }
        if (min != Long.MAX_VALUE) {
            return text;
        }
        if (beginChunk && !endChunk) {
            return BEGIN_CHUNK;
        }
        if (!beginChunk && endChunk) {
            return END_CHUNK;
        }
        return text;
    }

    @Override
    public void setValue(String value) {
        switch (value) {
        case EVERY_CHUNK:
            eventType.setPeriod(0, true, true);
            break;
        case BEGIN_CHUNK:
            eventType.setPeriod(0, true, false);
            break;
        case END_CHUNK:
            eventType.setPeriod(0, false, true);
            break;
        default:
            eventType.setPeriod(Utils.parseTimespan(value) / 1_000_000, false, false);
        }
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static boolean isType(long typeId) {
        return PeriodSetting.typeId == typeId;
    }
}
