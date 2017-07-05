/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintJobAttribute;

/**
 * Class Finishings is a printing attribute class, an enumeration, that
 * identifies whether the printer applies a finishing operation of some kind
 * of binding to each copy of each printed document in the job. For multidoc
 * print jobs (jobs with multiple documents), the
 * {@link MultipleDocumentHandling
 * MultipleDocumentHandling} attribute determines what constitutes a "copy"
 * for purposes of finishing.
 * <P>
 * Standard Finishings values are:
 * <TABLE BORDER=0 CELLPADDING=0 CELLSPACING=0 style="width:100%" SUMMARY="layout">
 * <TR>
 * <TD STYLE="WIDTH:10%">
 * &nbsp;
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #NONE NONE}
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #STAPLE STAPLE}
 * </TD>
 * <TD STYLE="WIDTH:36%">
 * {@link #EDGE_STITCH EDGE_STITCH}
 * </TD>
 * </TR>
 * <TR>
 * <TD>
 * &nbsp;
 * </TD>
 * <TD>
 * {@link #BIND BIND}
 * </TD>
 * <TD>
 * {@link #SADDLE_STITCH SADDLE_STITCH}
 * </TD>
 * <TD>
 * {@link #COVER COVER}
 * </TD>
 * <TD>
 * &nbsp;
 * </TD>
 * </TR>
 * </TABLE>
 * <P>
 * The following Finishings values are more specific; they indicate a
 * corner or an edge as if the document were a portrait document:
 * <TABLE BORDER=0 CELLPADDING=0 CELLSPACING=0 style="width:100%" SUMMARY="layout">
 * <TR>
 * <TD STYLE="WIDTH:10%">
 * &nbsp;
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #STAPLE_TOP_LEFT STAPLE_TOP_LEFT}
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #EDGE_STITCH_LEFT EDGE_STITCH_LEFT}
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #STAPLE_DUAL_LEFT STAPLE_DUAL_LEFT}
 * </TD>
 * <TD STYLE="WIDTH:9%">
 * &nbsp;
 * </TD>
 * </TR>
 * <TR>
 * <TD STYLE="WIDTH:10%">
 * &nbsp;
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #STAPLE_BOTTOM_LEFT STAPLE_BOTTOM_LEFT}
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #EDGE_STITCH_TOP EDGE_STITCH_TOP}
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #STAPLE_DUAL_TOP STAPLE_DUAL_TOP}
 * </TD>
 * <TD STYLE="WIDTH:9%">
 * &nbsp;
 * </TD>
 * </TR>
 * <TR>
 * <TD STYLE="WIDTH:10%">
 * &nbsp;
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #STAPLE_TOP_RIGHT STAPLE_TOP_RIGHT}
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #EDGE_STITCH_RIGHT EDGE_STITCH_RIGHT}
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #STAPLE_DUAL_RIGHT STAPLE_DUAL_RIGHT}
 * </TD>
 * <TD STYLE="WIDTH:9%">
 * &nbsp;
 * </TD>
 * </TR>
 * <TR>
 * <TD STYLE="WIDTH:10%">
 * &nbsp;
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #STAPLE_BOTTOM_RIGHT STAPLE_BOTTOM_RIGHT}
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #EDGE_STITCH_BOTTOM EDGE_STITCH_BOTTOM}
 * </TD>
 * <TD STYLE="WIDTH:27%">
 * {@link #STAPLE_DUAL_BOTTOM STAPLE_DUAL_BOTTOM}
 * </TD>
 * <TD STYLE="WIDTH:9%">
 * &nbsp;
 * </TD>
 * </TR>
 * </TABLE>
 * <P>
 * The STAPLE_<I>XXX</I> values are specified with respect to the
 * document as if the document were a portrait document. If the document is
 * actually a landscape or a reverse-landscape document, the client supplies the
 * appropriate transformed value. For example, to position a staple in the upper
 * left hand corner of a landscape document when held for reading, the client
 * supplies the STAPLE_BOTTOM_LEFT value (since landscape is
 * defined as a +90 degree rotation from portrait, i.e., anti-clockwise). On the
 * other hand, to position a staple in the upper left hand corner of a
 * reverse-landscape document when held for reading, the client supplies the
 * STAPLE_TOP_RIGHT value (since reverse-landscape is defined as a
 * -90 degree rotation from portrait, i.e., clockwise).
 * <P>
 * The angle (vertical, horizontal, angled) of each staple with respect to the
 * document depends on the implementation which may in turn depend on the value
 * of the attribute.
 * <P>
 * The effect of a Finishings attribute on a multidoc print job (a job
 * with multiple documents) depends on whether all the docs have the same
 * binding specified or whether different docs have different bindings
 * specified, and on the (perhaps defaulted) value of the {@link
 * MultipleDocumentHandling MultipleDocumentHandling} attribute.
 * <UL>
 * <LI>
 * If all the docs have the same binding specified, then any value of {@link
 * MultipleDocumentHandling MultipleDocumentHandling} makes sense, and the
 * printer's processing depends on the {@link MultipleDocumentHandling
 * MultipleDocumentHandling} value:
 * <UL>
 * <LI>
 * SINGLE_DOCUMENT -- All the input docs will be bound together as one output
 * document with the specified binding.
 *
 * <LI>
 * SINGLE_DOCUMENT_NEW_SHEET -- All the input docs will be bound together as one
 * output document with the specified binding, and the first impression of each
 * input doc will always start on a new media sheet.
 *
 * <LI>
 * SEPARATE_DOCUMENTS_UNCOLLATED_COPIES -- Each input doc will be bound
 * separately with the specified binding.
 *
 * <LI>
 * SEPARATE_DOCUMENTS_COLLATED_COPIES -- Each input doc will be bound separately
 * with the specified binding.
 * </UL>
 *
 * <LI>
 * If different docs have different bindings specified, then only two values of
 * {@link MultipleDocumentHandling MultipleDocumentHandling} make sense, and the
 * printer reports an error when the job is submitted if any other value is
 * specified:
 * <UL>
 * <LI>
 * SEPARATE_DOCUMENTS_UNCOLLATED_COPIES -- Each input doc will be bound
 * separately with its own specified binding.
 *
 * <LI>
 * SEPARATE_DOCUMENTS_COLLATED_COPIES -- Each input doc will be bound separately
 * with its own specified binding.
 * </UL>
 * </UL>
 * <P>
 * <B>IPP Compatibility:</B> Class Finishings encapsulates some of the
 * IPP enum values that can be included in an IPP "finishings" attribute, which
 * is a set of enums. The category name returned by
 * {@code getName()} is the IPP attribute name.  The enumeration's
 * integer value is the IPP enum value.  The {@code toString()} method
 * returns the IPP string representation of the attribute value.
 * In IPP Finishings is a multi-value attribute, this API currently allows
 * only one binding to be specified.
 *
 * @author  Alan Kaminsky
 */
public class Finishings extends EnumSyntax
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -627840419548391754L;

    /**
     * Perform no binding.
     */
    public static final Finishings NONE = new Finishings(3);

    /**
     * Bind the document(s) with one or more staples. The exact number and
     * placement of the staples is site-defined.
     */
    public static final Finishings STAPLE = new Finishings(4);

    /**
     * This value is specified when it is desired to select a non-printed (or
     * pre-printed) cover for the document. This does not supplant the
     * specification of a printed cover (on cover stock medium) by the
     * document  itself.
     */
    public static final Finishings COVER = new Finishings(6);

    /**
     * This value indicates that a binding is to be applied to the document;
     * the type and placement of the binding is site-defined.
     */
    public static final Finishings BIND = new Finishings(7);

    /**
     * Bind the document(s) with one or more staples (wire stitches) along the
     * middle fold. The exact number and placement of the staples and the
     * middle fold is implementation- and/or site-defined.
     */
    public static final Finishings SADDLE_STITCH =
        new Finishings(8);

    /**
     * Bind the document(s) with one or more staples (wire stitches) along one
     * edge. The exact number and placement of the staples is implementation-
     * and/or site- defined.
     */
    public static final Finishings EDGE_STITCH =
        new Finishings(9);

    /**
     * Bind the document(s) with one or more staples in the top left corner.
     */
    public static final Finishings STAPLE_TOP_LEFT =
        new Finishings(20);

    /**
     * Bind the document(s) with one or more staples in the bottom left
     * corner.
     */
    public static final Finishings STAPLE_BOTTOM_LEFT =
        new Finishings(21);

    /**
     * Bind the document(s) with one or more staples in the top right corner.
     */
    public static final Finishings STAPLE_TOP_RIGHT =
        new Finishings(22);

    /**
     * Bind the document(s) with one or more staples in the bottom right
     * corner.
     */
    public static final Finishings STAPLE_BOTTOM_RIGHT =
        new Finishings(23);

    /**
     * Bind the document(s) with one or more staples (wire stitches) along the
     * left edge. The exact number and placement of the staples is
     * implementation- and/or site-defined.
     */
    public static final Finishings EDGE_STITCH_LEFT =
        new Finishings(24);

    /**
     * Bind the document(s) with one or more staples (wire stitches) along the
     * top edge. The exact number and placement of the staples is
     * implementation- and/or site-defined.
     */
    public static final Finishings EDGE_STITCH_TOP =
        new Finishings(25);

    /**
     * Bind the document(s) with one or more staples (wire stitches) along the
     * right edge. The exact number and placement of the staples is
     * implementation- and/or site-defined.
     */
    public static final Finishings EDGE_STITCH_RIGHT =
        new Finishings(26);

    /**
     * Bind the document(s) with one or more staples (wire stitches) along the
     * bottom edge. The exact number and placement of the staples is
     * implementation- and/or site-defined.
     */
    public static final Finishings EDGE_STITCH_BOTTOM =
        new Finishings(27);

    /**
     * Bind the document(s) with two staples (wire stitches) along the left
     * edge assuming a portrait document (see above).
     */
    public static final Finishings STAPLE_DUAL_LEFT =
        new Finishings(28);

    /**
     * Bind the document(s) with two staples (wire stitches) along the top
     * edge assuming a portrait document (see above).
     */
    public static final Finishings STAPLE_DUAL_TOP =
        new Finishings(29);

    /**
     * Bind the document(s) with two staples (wire stitches) along the right
     * edge assuming a portrait document (see above).
     */
    public static final Finishings STAPLE_DUAL_RIGHT =
        new Finishings(30);

    /**
     * Bind the document(s) with two staples (wire stitches) along the bottom
     * edge assuming a portrait document (see above).
     */
    public static final Finishings STAPLE_DUAL_BOTTOM =
        new Finishings(31);

    /**
     * Construct a new finishings binding enumeration value with the given
     * integer value.
     *
     * @param  value  Integer value.
     */
    protected Finishings(int value) {
        super(value);
    }

    private static final String[] myStringTable =
                {"none",
                 "staple",
                 null,
                 "cover",
                 "bind",
                 "saddle-stitch",
                 "edge-stitch",
                 null, // The next ten enum values are reserved.
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 "staple-top-left",
                 "staple-bottom-left",
                 "staple-top-right",
                 "staple-bottom-right",
                 "edge-stitch-left",
                 "edge-stitch-top",
                 "edge-stitch-right",
                 "edge-stitch-bottom",
                 "staple-dual-left",
                 "staple-dual-top",
                 "staple-dual-right",
                 "staple-dual-bottom"
                };

    private static final Finishings[] myEnumValueTable =
                {NONE,
                 STAPLE,
                 null,
                 COVER,
                 BIND,
                 SADDLE_STITCH,
                 EDGE_STITCH,
                 null, // The next ten enum values are reserved.
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 null,
                 STAPLE_TOP_LEFT,
                 STAPLE_BOTTOM_LEFT,
                 STAPLE_TOP_RIGHT,
                 STAPLE_BOTTOM_RIGHT,
                 EDGE_STITCH_LEFT,
                 EDGE_STITCH_TOP,
                 EDGE_STITCH_RIGHT,
                 EDGE_STITCH_BOTTOM,
                 STAPLE_DUAL_LEFT,
                 STAPLE_DUAL_TOP,
                 STAPLE_DUAL_RIGHT,
                 STAPLE_DUAL_BOTTOM
                };

    /**
     * Returns the string table for class Finishings.
     */
    protected String[] getStringTable() {
        return myStringTable.clone();
    }

    /**
     * Returns the enumeration value table for class Finishings.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return (EnumSyntax[])myEnumValueTable.clone();
    }

    /**
     * Returns the lowest integer value used by class Finishings.
     */
    protected int getOffset() {
        return 3;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <P>
     * For class Finishings and any vendor-defined subclasses, the
     * category is class Finishings itself.
     *
     * @return  Printing attribute class (category), an instance of class
     *          {@link java.lang.Class java.lang.Class}.
     */
    public final Class<? extends Attribute> getCategory() {
        return Finishings.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <P>
     * For class Finishings and any vendor-defined subclasses, the
     * category name is {@code "finishings"}.
     *
     * @return  Attribute category name.
     */
    public final String getName() {
        return "finishings";
    }

}
