/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package validation;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.TypeInfoProvider;
import javax.xml.validation.ValidatorHandler;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 4970402
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug4970402
 * @run testng/othervm validation.Bug4970402
 * @summary Test TypeInfoProvider's attribute accessing methods throw IndexOutOfBoundsException when index parameter is invalid.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4970402 {

    public static final String XSD = "<?xml version='1.0'?>\n" + "<schema xmlns='http://www.w3.org/2001/XMLSchema'\n" + "        xmlns:test='jaxp13_test'\n"
            + "        targetNamespace='jaxp13_test'\n" + "        elementFormDefault='qualified'>\n" + "    <element name='test'>\n"
            + "        <complexType>\n" + "            <sequence>\n" + "                <element name='child' type='string'/>\n" + "            </sequence>\n"
            + "            <attribute name='id' />\n" + "        </complexType>\n" + "    </element>\n" + "</schema>\n";

    public static final String XML = "<?xml version='1.0'?>\n" + "<ns:test xmlns:ns='jaxp13_test' id='2003-12-02'>\n" + "  <ns:child>123abc</ns:child>\n"
            + "</ns:test>\n";

    private ValidatorHandler createValidatorHandler(String xsd) throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        StringReader reader = new StringReader(xsd);
        StreamSource xsdSource = new StreamSource(reader);

        Schema schema = schemaFactory.newSchema(xsdSource);
        return schema.newValidatorHandler();
    }

    private XMLReader createXMLReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        return parserFactory.newSAXParser().getXMLReader();
    }

    private void parse(XMLReader xmlReader, String xml) throws SAXException, IOException {
        StringReader reader = new StringReader(xml);
        InputSource inSource = new InputSource(reader);

        xmlReader.parse(inSource);
    }

    @Test
    public void test() throws Exception {
        XMLReader xmlReader = createXMLReader();
        final ValidatorHandler validatorHandler = createValidatorHandler(XSD);
        xmlReader.setContentHandler(validatorHandler);

        DefaultHandler handler = new DefaultHandler() {
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if (!"ns:test".equals(qName)) {
                    return;
                }

                TypeInfoProvider infoProvider = null;
                synchronized (validatorHandler) {
                    infoProvider = validatorHandler.getTypeInfoProvider();
                }
                Assert.assertTrue(infoProvider != null, "Can't obtain TypeInfoProvider object.");

                try {
                    infoProvider.getAttributeTypeInfo(-1);
                    Assert.fail("IndexOutOfBoundsException was not thrown.");
                } catch (IndexOutOfBoundsException e) {
                    ; // as expected
                }

                try {
                    infoProvider.isIdAttribute(-1);
                    Assert.fail("IndexOutOfBoundsException was not thrown.");
                } catch (IndexOutOfBoundsException e) {
                    ; // as expected
                }
            }
        };
        validatorHandler.setContentHandler(handler);

        parse(xmlReader, XML);
    }
}
