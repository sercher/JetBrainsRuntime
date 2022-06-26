/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

/* remember that the API requires a Font use a
 * consistent glyph id. for a code point, and this is a
 * problem if a particular strike uses native scaler sometimes
 * and the JDK scaler others. That needs to be dealt with somewhere, but
 * here we can just always get the same glyph code without
 * needing a strike.
 */

public class CompositeGlyphMapper extends CharToGlyphMapper {

    public static final int SLOTMASK =  0xff000000;
    public static final int GLYPHMASK = 0x00ffffff;

    public static final int NBLOCKS = 512; // covering BMP and SMP
    public static final int BLOCKSZ = 256;
    public static final int MAXUNICODE = NBLOCKS*BLOCKSZ;


    CompositeFont font;
    CharToGlyphMapper[] slotMappers;
    int[][] glyphMaps;
    private boolean hasExcludes;

    public CompositeGlyphMapper(CompositeFont compFont) {
        font = compFont;
        initMapper();
        /* This is often false which saves the overhead of a
         * per-mapped char method call.
         */
        hasExcludes = compFont.exclusionRanges != null &&
                      compFont.maxIndices != null;
    }

    public int compositeGlyphCode(int slot, int glyphCode) {
        return (slot << 24 | (glyphCode & GLYPHMASK));
    }

    private void initMapper() {
        if (missingGlyph == CharToGlyphMapper.UNINITIALIZED_GLYPH) {
            if (glyphMaps == null) {
                glyphMaps = new int[NBLOCKS][];
            }
            slotMappers = new CharToGlyphMapper[font.numSlots];
            /* This requires that slot 0 is never empty. */
            missingGlyph = font.getSlotFont(0).getMissingGlyphCode();
            missingGlyph = compositeGlyphCode(0, missingGlyph);
        }
    }

    private int getCachedGlyphCode(int unicode) {
        if (unicode >= MAXUNICODE) {
            return UNINITIALIZED_GLYPH;
        }
        int[] gmap;
        if ((gmap = glyphMaps[unicode >> 8]) == null) {
            return UNINITIALIZED_GLYPH;
        }
        return gmap[unicode & 0xff];
    }

    void setCachedGlyphCode(int unicode, int glyphCode) {
        if (unicode >= MAXUNICODE) {
            return;
        }
        int index0 = unicode >> 8;
        if (glyphMaps[index0] == null) {
            glyphMaps[index0] = new int[BLOCKSZ];
            for (int i=0;i<BLOCKSZ;i++) {
                glyphMaps[index0][i] = UNINITIALIZED_GLYPH;
            }
        }
        glyphMaps[index0][unicode & 0xff] = glyphCode;
    }

    private CharToGlyphMapper getSlotMapper(int slot) {
        CharToGlyphMapper mapper = slotMappers[slot];
        if (mapper == null) {
            mapper = font.getSlotFont(slot).getMapper();
            slotMappers[slot] = mapper;
        }
        return mapper;
    }

    protected int convertToGlyph(int unicode) {
        return convertToGlyph(unicode, 0);
    }

    protected int convertToGlyph(int unicode, int variationSelector) {

        for (int slot = 0; slot < font.numSlots; slot++) {
            if (!hasExcludes || !font.isExcludedChar(slot, unicode)) {
                CharToGlyphMapper mapper = getSlotMapper(slot);
                int glyphCode = mapper.charToVariationGlyph(unicode, variationSelector);
                if (glyphCode != mapper.getMissingGlyphCode()) {
                    glyphCode = compositeGlyphCode(slot, glyphCode);
                    if (variationSelector == 0) setCachedGlyphCode(unicode, glyphCode);
                    return glyphCode;
                }
            }
        }
        return missingGlyph;
    }

    @Override
    public int charToVariationGlyph(int unicode, int variationSelector) {
        if (variationSelector == 0) return charToGlyph(unicode);
        else {
            int glyph = convertToGlyph(unicode, variationSelector);
            // Glyph variation not found, fallback to base glyph.
            // In fallback from variation glyph we ignore excluded chars,
            // this is needed for proper display of monochrome emoji (\ufe0e)
            if (glyph == missingGlyph) {
                for (int slot = 0; slot < font.numSlots; slot++) {
                    CharToGlyphMapper mapper = getSlotMapper(slot);
                    glyph = mapper.charToGlyph(unicode);
                    if (glyph != mapper.getMissingGlyphCode()) {
                        glyph = compositeGlyphCode(slot, glyph);
                        break;
                    }
                }
            }
            return glyph;
        }
    }

    public int getNumGlyphs() {
        int numGlyphs = 0;
        /* The number of glyphs in a composite is affected by
         * exclusion ranges and duplicates (ie the same code point is
         * mapped by two different fonts) and also whether or not to
         * count fallback fonts. A nearly correct answer would be very
         * expensive to generate. A rough ballpark answer would
         * just count the glyphs in all the slots. However this would
         * initialize mappers for all slots when they aren't necessarily
         * needed. For now just use the first slot as JDK 1.4 did.
         */
        for (int slot=0; slot<1 /*font.numSlots*/; slot++) {
           CharToGlyphMapper mapper = slotMappers[slot];
           if (mapper == null) {
               mapper = font.getSlotFont(slot).getMapper();
               slotMappers[slot] = mapper;
           }
           numGlyphs += mapper.getNumGlyphs();
        }
        return numGlyphs;
    }

    public int charToGlyph(int unicode) {

        int glyphCode = getCachedGlyphCode(unicode);
        if (glyphCode == UNINITIALIZED_GLYPH) {
            glyphCode = convertToGlyph(unicode);
        }
        return glyphCode;
    }

    public int charToGlyph(int unicode, int prefSlot) {
        if (prefSlot >= 0) {
            CharToGlyphMapper mapper = getSlotMapper(prefSlot);
            int glyphCode = mapper.charToGlyph(unicode);
            if (glyphCode != mapper.getMissingGlyphCode()) {
                return compositeGlyphCode(prefSlot, glyphCode);
            }
        }
        return charToGlyph(unicode);
    }

    public int charToGlyph(char unicode) {

        int glyphCode  = getCachedGlyphCode(unicode);
        if (glyphCode == UNINITIALIZED_GLYPH) {
            glyphCode = convertToGlyph(unicode);
        }
        return glyphCode;
    }

    /* This variant checks if shaping is needed and immediately
     * returns true if it does. A caller of this method should be expecting
     * to check the return type because it needs to know how to handle
     * the character data for display.
     */
    public boolean charsToGlyphsNS(int count, char[] unicodes, int[] glyphs) {

        for (int i=0; i<count; i++) {
            int code = unicodes[i]; // char is unsigned.

            if (code >= HI_SURROGATE_START &&
                code <= HI_SURROGATE_END && i < count - 1) {
                char low = unicodes[i + 1];

                if (low >= LO_SURROGATE_START &&
                    low <= LO_SURROGATE_END) {
                    code = (code - HI_SURROGATE_START) *
                        0x400 + low - LO_SURROGATE_START + 0x10000;
                    glyphs[i + 1] = INVISIBLE_GLYPH_ID;
                }
            }

            int gc = glyphs[i] = getCachedGlyphCode(code);
            if (gc == UNINITIALIZED_GLYPH) {
                glyphs[i] = convertToGlyph(code);
            }

            if (code < FontUtilities.MIN_LAYOUT_CHARCODE) {
                continue;
            }
            else if (FontUtilities.isComplexCharCode(code) ||
                     CharToGlyphMapper.isVariationSelector(code)) {
                return true;
            }
            else if (code >= 0x10000) {
                i += 1; // Empty glyph slot after surrogate
                continue;
            }
        }

        return false;
    }

    /* The conversion is not very efficient - looping as it does, converting
     * one char at a time. However the cache should fill very rapidly.
     */
    public void charsToGlyphs(int count, char[] unicodes, int[] glyphs) {
        for (int i=0; i<count; i++) {
            int code = unicodes[i]; // char is unsigned.

            if (code >= HI_SURROGATE_START &&
                code <= HI_SURROGATE_END && i < count - 1) {
                char low = unicodes[i + 1];

                if (low >= LO_SURROGATE_START &&
                    low <= LO_SURROGATE_END) {
                    code = (code - HI_SURROGATE_START) *
                        0x400 + low - LO_SURROGATE_START + 0x10000;

                    int gc = glyphs[i] = getCachedGlyphCode(code);
                    if (gc == UNINITIALIZED_GLYPH) {
                        glyphs[i] = convertToGlyph(code);
                    }
                    i += 1; // Empty glyph slot after surrogate
                    glyphs[i] = INVISIBLE_GLYPH_ID;
                    continue;
                }
            }

            int gc = glyphs[i] = getCachedGlyphCode(code);
            if (gc == UNINITIALIZED_GLYPH) {
                glyphs[i] = convertToGlyph(code);
            }
        }
    }

    public void charsToGlyphs(int count, int[] unicodes, int[] glyphs) {
        for (int i=0; i<count; i++) {
            int code = unicodes[i];

            glyphs[i] = getCachedGlyphCode(code);
            if (glyphs[i] == UNINITIALIZED_GLYPH) {
                glyphs[i] = convertToGlyph(code);
            }
        }
    }

}
