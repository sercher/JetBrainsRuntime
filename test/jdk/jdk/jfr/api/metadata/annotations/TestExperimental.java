/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.metadata.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Experimental;
import jdk.jfr.MetadataDefinition;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.annotations.TestExperimental
 */
public class TestExperimental {

    @MetadataDefinition
    @Experimental
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @interface ExperimentalAnnotation {
    }

    @ExperimentalAnnotation
    @Experimental
    static class ExperimentalEvent extends Event {
        @Experimental
        boolean experimentalField;
    }

    public static void main(String[] args) throws Exception {
        EventType t = EventType.getEventType(ExperimentalEvent.class);

        // @Experimental on event
        Experimental e = t.getAnnotation(Experimental.class);
        Asserts.assertTrue(e != null, "Expected @Experimental annotation on event");

        // @Experimental on annotation
        AnnotationElement a = Events.getAnnotationByName(t, ExperimentalAnnotation.class.getName());
        e = a.getAnnotation(Experimental.class);
        Asserts.assertTrue(e != null, "Expected @Experimental on annotation");

        // @Experimental on field
        a = Events.getAnnotation(t.getField("experimentalField"), Experimental.class);
        Asserts.assertTrue(e != null, "Expected @Experimental on field");
    }
}
