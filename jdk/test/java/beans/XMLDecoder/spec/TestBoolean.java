/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * @test
 * @summary Tests <boolean> element
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;

public final class TestBoolean extends AbstractTest {
    public static final String XML
            = "<java>\n"
            + " <boolean>true</boolean>\n"
            + " <boolean>false</boolean>\n"
            + "</java>";

    public static void main(String[] args) {
        new TestBoolean().test(true);
    }

    @Override
    protected void validate(XMLDecoder decoder) {
        if (!Boolean.TRUE.equals(decoder.readObject())) {
            throw new Error("true expected");
        }
        if (!Boolean.FALSE.equals(decoder.readObject())) {
            throw new Error("false expected");
        }
    }
}
