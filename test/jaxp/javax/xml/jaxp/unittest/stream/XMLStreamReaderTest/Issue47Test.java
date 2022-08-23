/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package stream.XMLStreamReaderTest;

import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6631265
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.Issue47Test
 * @run testng/othervm stream.XMLStreamReaderTest.Issue47Test
 * @summary Test XMLStreamReader.standaloneSet() presents if input document has a value for "standalone" attribute in xml declaration.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Issue47Test {

    @Test
    public void testStandaloneSet() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><prefix:root xmlns=\"\" xmlns:null=\"\"></prefix:root>";

        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader r = xif.createXMLStreamReader(new StringReader(xml));
            Assert.assertTrue(!r.standaloneSet() && !r.isStandalone());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStandaloneSet1() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><prefix:root xmlns=\"\" xmlns:null=\"\"></prefix:root>";

        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader r = xif.createXMLStreamReader(new StringReader(xml));
            Assert.assertTrue(r.standaloneSet() && !r.isStandalone());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testStandaloneSet2() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><prefix:root xmlns=\"\" xmlns:null=\"\"></prefix:root>";

        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader r = xif.createXMLStreamReader(new StringReader(xml));
            AssertJUnit.assertTrue(r.standaloneSet() && r.isStandalone());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }
}
