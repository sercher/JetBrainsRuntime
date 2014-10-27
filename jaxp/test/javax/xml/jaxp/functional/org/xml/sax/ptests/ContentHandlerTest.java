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
package org.xml.sax.ptests;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import static jaxp.library.JAXPTestUtilities.compareWithGold;
import static jaxp.library.JAXPTestUtilities.failCleanup;
import static jaxp.library.JAXPTestUtilities.failUnexpected;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import static org.xml.sax.ptests.SAXTestConst.CLASS_DIR;
import static org.xml.sax.ptests.SAXTestConst.GOLDEN_DIR;
import static org.xml.sax.ptests.SAXTestConst.XML_DIR;

/**
 * Class registers a content event handler to XMLReader. Content event handler
 * transverses XML and print all visited node  when XMLreader parses XML. Test
 * verifies output is same as the golden file.
 */
public class ContentHandlerTest {
    /**
     * Content event handler visit all nodes to print to output file.
     */
    @Test
    public void testcase01() {
        String outputFile = CLASS_DIR + "Content.out";
        String goldFile = GOLDEN_DIR + "ContentGF.out";
        String xmlFile = XML_DIR + "namespace1.xml";

        try(FileInputStream instream = new FileInputStream(xmlFile)) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            ContentHandler cHandler = new MyContentHandler(outputFile);
            xmlReader.setContentHandler(cHandler);
            InputSource is = new InputSource(instream);
            xmlReader.parse(is);
            assertTrue(compareWithGold(goldFile, outputFile));
        } catch( IOException | SAXException | ParserConfigurationException ex) {
            failUnexpected(ex);
        } finally {
            try {
                Path outputPath = Paths.get(outputFile);
                if(Files.exists(outputPath))
                    Files.delete(outputPath);
            } catch (IOException ex) {
                failCleanup(ex, outputFile);
            }
        }
    }
}

/**
 * A content write out handler.
 */
class MyContentHandler extends XMLFilterImpl {
    /**
     * Prefix to every exception.
     */
    private final static String WRITE_ERROR = "bWriter error";

    /**
     * FileWriter to write string to output file.
     */
    private final BufferedWriter bWriter;

    /**
     * Default document locator.
     */
    private Locator locator;

    /**
     * Initiate FileWriter when construct a MyContentHandler.
     * @param outputFileName output file name.
     * @throws SAXException creation of FileWriter failed.
     */
    public MyContentHandler(String outputFileName) throws SAXException {
        try {
            bWriter = new BufferedWriter(new FileWriter(outputFileName));
        } catch (IOException ex) {
            throw new SAXException(ex);
        }
    }

    /**
     * Write characters tag along with content of characters when meet
     * characters event.
     * @throws IOException error happen when writing file.
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String s = new String(ch, start, length);
        println("characters...\n" + s);
    }

    /**
     * Write endDocument tag then flush the content and close the file when meet
     * endDocument event.
     * @throws IOException error happen when writing file or closing file.
     */
    @Override
    public void endDocument() throws SAXException {
        try {
            println("endDocument...");
            bWriter.flush();
            bWriter.close();
        } catch (IOException ex) {
            throw new SAXException(WRITE_ERROR, ex);
        }
    }

    /**
     * Write endElement tag with namespaceURI, localName, qName to the file when
     * meet endElement event.
     * @throws IOException error happen when writing file.
     */
    @Override
    public void endElement(String namespaceURI,String localName,String qName) throws SAXException{
        println("endElement...\n" + "namespaceURI: " + namespaceURI +
                " localName: "+ localName + " qName: " + qName);
    }

    /**
     * Write endPrefixMapping tag along with prefix to the file when meet
     * endPrefixMapping event.
     * @throws IOException error happen when writing file.
     */
    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        println("endPrefixMapping...\n" + "prefix: " + prefix);
    }

    /**
     * Write ignorableWhitespace tag along with white spaces when meet
     * ignorableWhitespace event.
     * @throws IOException error happen when writing file.
     */
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        String s = new String(ch, start, length);
        println("ignorableWhitespace...\n" + s +
                " ignorable white space string length: " + s.length());
    }

    /**
     * Write processingInstruction tag along with target name and target data
     * when meet processingInstruction event.
     * @throws IOException error happen when writing file.
     */
    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        println("processingInstruction...target:" + target +
                " data: " + data);
    }

    /**
     * Write setDocumentLocator tag when meet setDocumentLocator event.
     */
    @Override
    public void setDocumentLocator(Locator locator) {
        try {
            this.locator = locator;
            println("setDocumentLocator...");
        } catch (SAXException ex) {
            System.err.println(WRITE_ERROR + ex);
        }
    }

    /**
     * Write skippedEntity tag along with entity name when meet skippedEntity
     * event.
     * @throws IOException error happen when writing file.
     */
    @Override
    public void skippedEntity(String name) throws SAXException {
        println("skippedEntity...\n" + "name: " + name);
    }

    /**
     * Write startDocument tag when meet startDocument event.
     * @throws IOException error happen when writing file.
     */
    @Override
    public void startDocument() throws SAXException {
        println("startDocument...");
    }

    /**
     * Write startElement tag along with namespaceURI, localName, qName, number
     * of attributes and line number when meet startElement event.
     * @throws IOException error happen when writing file.
     */
    @Override
    public void startElement(String namespaceURI, String localName,
                        String qName, Attributes atts) throws SAXException {
        println("startElement...\n" + "namespaceURI: " +  namespaceURI +
                " localName: " + localName +  " qName: " + qName +
                " Number of Attributes: " + atts.getLength() +
                " Line# " + locator.getLineNumber());
    }

    /**
     * Write startPrefixMapping tag along with prefix and uri when meet
     * startPrefixMapping event.
     * @throws IOException error happen when writing file.
     */
    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        println("startPrefixMapping...\n" + "prefix: " + prefix +
                " uri: " + uri);
    }

    /**
     * Write outString to file.
     * @param outString String to be written to File
     * @throws SAXException if write file failed
     */
    private void println(String outString) throws SAXException {
        try {
            bWriter.write( outString, 0, outString.length());
            bWriter.newLine();
        } catch (IOException ex) {
            throw new SAXException(WRITE_ERROR, ex);
        }
    }
}
