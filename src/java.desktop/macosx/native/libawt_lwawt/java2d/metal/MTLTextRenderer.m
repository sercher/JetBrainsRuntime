/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef HEADLESS

#include <stdlib.h>
#include <limits.h>
#include <math.h>
#include <jlong.h>

#include "sun_java2d_metal_MTLTextRenderer.h"

#include "SurfaceData.h"
#include "MTLContext.h"
#include "MTLRenderQueue.h"
#include "MTLTextRenderer.h"
#include "MTLVertexCache.h"
#include "MTLGlyphCache.h"
#include "MTLBlitLoops.h"

/**
 * The following constants define the inner and outer bounds of the
 * accelerated glyph cache.
 */
#define MTLTR_CACHE_WIDTH       1024
#define MTLTR_CACHE_HEIGHT      1024
#define MTLTR_CACHE_CELL_WIDTH  32
#define MTLTR_CACHE_CELL_HEIGHT 32

/**
 * The current "glyph mode" state.  This variable is used to track the
 * codepath used to render a particular glyph.  This variable is reset to
 * MODE_NOT_INITED at the beginning of every call to MTLTR_DrawGlyphList().
 * As each glyph is rendered, the glyphMode variable is updated to reflect
 * the current mode, so if the current mode is the same as the mode used
 * to render the previous glyph, we can avoid doing costly setup operations
 * each time.
 */
typedef enum {
    MODE_NOT_INITED,
    MODE_USE_CACHE_GRAY,
    MODE_USE_CACHE_LCD,
    MODE_NO_CACHE_GRAY,
    MODE_NO_CACHE_LCD,
    MODE_NO_CACHE_COLOR
} GlyphMode;
static GlyphMode glyphMode = MODE_NOT_INITED;

/**
 * There are two separate glyph caches: for AA and for LCD.
 * Once one of them is initialized as either GRAY or LCD, it
 * stays in that mode for the duration of the application.  It should
 * be safe to use this one glyph cache for all screens in a multimon
 * environment, since the glyph cache texture is shared between all contexts,
 * and (in theory) OpenGL drivers should be smart enough to manage that
 * texture across all screens.
 */

static MTLGlyphCacheInfo *glyphCacheLCD = NULL;
static MTLGlyphCacheInfo *glyphCacheAA = NULL;

/**
 * The handle to the LCD text fragment program object.
 */
static GLhandleARB lcdTextProgram = 0;

/**
 * This value tracks the previous LCD contrast setting, so if the contrast
 * value hasn't changed since the last time the gamma uniforms were
 * updated (not very common), then we can skip updating the unforms.
 */
static jint lastLCDContrast = -1;

/**
 * This value tracks the previous LCD rgbOrder setting, so if the rgbOrder
 * value has changed since the last time, it indicates that we need to
 * invalidate the cache, which may already store glyph images in the reverse
 * order.  Note that in most real world applications this value will not
 * change over the course of the application, but tests like Font2DTest
 * allow for changing the ordering at runtime, so we need to handle that case.
 */
static jboolean lastRGBOrder = JNI_TRUE;

/**
 * This constant defines the size of the tile to use in the
 * MTLTR_DrawLCDGlyphNoCache() method.  See below for more on why we
 * restrict this value to a particular size.
 */
#define MTLTR_NOCACHE_TILE_SIZE 32

/**
 * These constants define the size of the "cached destination" texture.
 * This texture is only used when rendering LCD-optimized text, as that
 * codepath needs direct access to the destination.  There is no way to
 * access the framebuffer directly from an OpenGL shader, so we need to first
 * copy the destination region corresponding to a particular glyph into
 * this cached texture, and then that texture will be accessed inside the
 * shader.  Copying the destination into this cached texture can be a very
 * expensive operation (accounting for about half the rendering time for
 * LCD text), so to mitigate this cost we try to bulk read a horizontal
 * region of the destination at a time.  (These values are empirically
 * derived for the common case where text runs horizontally.)
 *
 * Note: It is assumed in various calculations below that:
 *     (MTLTR_CACHED_DEST_WIDTH  >= MTLTR_CACHE_CELL_WIDTH)  &&
 *     (MTLTR_CACHED_DEST_WIDTH  >= MTLTR_NOCACHE_TILE_SIZE) &&
 *     (MTLTR_CACHED_DEST_HEIGHT >= MTLTR_CACHE_CELL_HEIGHT) &&
 *     (MTLTR_CACHED_DEST_HEIGHT >= MTLTR_NOCACHE_TILE_SIZE)
 */
#define MTLTR_CACHED_DEST_WIDTH  1024
#define MTLTR_CACHED_DEST_HEIGHT (MTLTR_CACHE_CELL_HEIGHT * 2)

/**
 * The handle to the "cached destination" texture object.
 */
static GLuint cachedDestTextureID = 0;

/**
 * The current bounds of the "cached destination" texture, in destination
 * coordinate space.  The width/height of these bounds will not exceed the
 * MTLTR_CACHED_DEST_WIDTH/HEIGHT values defined above.  These bounds are
 * only considered valid when the isCachedDestValid flag is JNI_TRUE.
 */
static SurfaceDataBounds cachedDestBounds;

/**
 * This flag indicates whether the "cached destination" texture contains
 * valid data.  This flag is reset to JNI_FALSE at the beginning of every
 * call to MTLTR_DrawGlyphList().  Once we copy valid destination data
 * into the cached texture, this flag is set to JNI_TRUE.  This way, we can
 * limit the number of times we need to copy destination data, which is a
 * very costly operation.
 */
static jboolean isCachedDestValid = JNI_FALSE;

/**
 * The bounds of the previously rendered LCD glyph, in destination
 * coordinate space.  We use these bounds to determine whether the glyph
 * currently being rendered overlaps the previously rendered glyph (i.e.
 * its bounding box intersects that of the previously rendered glyph).  If
 * so, we need to re-read the destination area associated with that previous
 * glyph so that we can correctly blend with the actual destination data.
 */
static SurfaceDataBounds previousGlyphBounds;

/**
 * Initializes the one glyph cache (texture and data structure).
 * If lcdCache is JNI_TRUE, the texture will contain RGB data,
 * otherwise we will simply store the grayscale/monochrome glyph images
 * as intensity values (which work well with the GL_MODULATE function).
 */
static jboolean
MTLTR_InitGlyphCache(MTLContext *mtlc)
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_InitGlyphCache");

    MTLGlyphCacheInfo *gcinfo;
    // init glyph cache data structure
    gcinfo = MTLGlyphCache_Init(MTLTR_CACHE_WIDTH,
                                MTLTR_CACHE_HEIGHT,
                                MTLTR_CACHE_CELL_WIDTH,
                                MTLTR_CACHE_CELL_HEIGHT,
                                MTLVertexCache_FlushGlyphVertexCache);

    if (gcinfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "MTLTR_InitGlyphCache: could not init MTL glyph cache");
        return JNI_FALSE;
    }

    MTLTextureDescriptor *textureDescriptor =
        [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatA8Unorm
                                                            width:MTLTR_CACHE_WIDTH
                                                            height:MTLTR_CACHE_HEIGHT
                                                            mipmapped:NO];

    gcinfo->texture = [mtlc.device newTextureWithDescriptor:textureDescriptor];
    [textureDescriptor release];

    glyphCacheAA = gcinfo;

    return JNI_TRUE;
}

id<MTLTexture>
MTLTR_GetGlyphCacheTexture()
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_GetGlyphCacheTexture");
    if (glyphCacheAA != NULL) {
        return glyphCacheAA->texture;
    }
    return NULL;
}

/**
 * Adds the given glyph to the glyph cache (texture and data structure)
 * associated with the given MTLContext.
 */
static void
MTLTR_AddToGlyphCache(GlyphInfo *glyph, MTLContext *mtlc)
{
    MTLCacheCellInfo *ccinfo;
    MTLGlyphCacheInfo *gcinfo;

    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_AddToGlyphCache");
    gcinfo = glyphCacheAA;

    if ((gcinfo == NULL) || (glyph->image == NULL)) {
        return;
    }

    bool isCacheFull = MTLGlyphCache_IsCacheFull(gcinfo, glyph);
    if (isCacheFull) {
        MTLGlyphCache_Free(gcinfo);
        MTLTR_InitGlyphCache(mtlc);
        gcinfo = glyphCacheAA;
    }
    MTLGlyphCache_AddGlyph(gcinfo, glyph);
    ccinfo = (MTLCacheCellInfo *) glyph->cellInfo;

    if (ccinfo != NULL) {
        // store glyph image in texture cell
        MTLRegion region = {
                {ccinfo->x,  ccinfo->y,   0},
                {glyph->width, glyph->height, 1}
        };
        NSUInteger bytesPerRow = 1 * glyph->width;
        [gcinfo->texture replaceRegion:region
                         mipmapLevel:0
                         withBytes:glyph->image
                         bytesPerRow:bytesPerRow];
    }
}

static MTLRenderPipelineDescriptor * templateRenderPipelineDesc = nil;
static MTLRenderPipelineDescriptor * templateLCDPipelineDesc = nil;

/**
 * Enables the LCD text shader and updates any related state, such as the
 * gamma lookup table textures.
 */
static jboolean
MTLTR_EnableLCDGlyphModeState(id<MTLRenderCommandEncoder> encoder,
                              MTLContext *mtlc, 
                              MTLSDOps *dstOps,
                              MTLPooledTextureHandle *glyphTexture,
                              id<MTLTexture> dstTexture,
                              jint contrast, jint x, jint y, jint w, jint h)
{
    // create the LCD text shader, if necessary
    if (templateLCDPipelineDesc == nil) {

        MTLVertexDescriptor *vertDesc = [[MTLVertexDescriptor new] autorelease];
        vertDesc.attributes[VertexAttributePosition].format = MTLVertexFormatFloat2;
        vertDesc.attributes[VertexAttributePosition].offset = 0;
        vertDesc.attributes[VertexAttributePosition].bufferIndex = MeshVertexBuffer;
        vertDesc.layouts[MeshVertexBuffer].stride = sizeof(struct Vertex);
        vertDesc.layouts[MeshVertexBuffer].stepRate = 1;
        vertDesc.layouts[MeshVertexBuffer].stepFunction = MTLVertexStepFunctionPerVertex;

        templateLCDPipelineDesc = [[MTLRenderPipelineDescriptor new] autorelease];
        templateLCDPipelineDesc.sampleCount = 1;
        templateLCDPipelineDesc.vertexDescriptor = vertDesc;
        templateLCDPipelineDesc.colorAttachments[0].pixelFormat = MTLPixelFormatBGRA8Unorm;
        templateLCDPipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].format = MTLVertexFormatFloat2;
        templateLCDPipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].offset = 2*sizeof(float);
        templateLCDPipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].bufferIndex = MeshVertexBuffer;
        templateLCDPipelineDesc.vertexDescriptor.layouts[MeshVertexBuffer].stride = sizeof(struct TxtVertex);
        templateLCDPipelineDesc.vertexDescriptor.layouts[MeshVertexBuffer].stepRate = 1;
        templateLCDPipelineDesc.vertexDescriptor.layouts[MeshVertexBuffer].stepFunction = MTLVertexStepFunctionPerVertex;
        templateLCDPipelineDesc.label = @"template_lcd";
    }

    id<MTLRenderPipelineState> pipelineState = 
                [mtlc.pipelineStateStorage 
                    getPipelineState:templateLCDPipelineDesc
                    vertexShaderId:@"vert_txt"
                    fragmentShaderId:@"lcd_color"
                   ];


    NSLog(@"shader %@", [mtlc.pipelineStateStorage getShader:@"fragmentShaderId"]);
    [encoder setRenderPipelineState:pipelineState];

    double g = 0;
    double ig = 0;

    // update the current contrast setting, if necessary
    if (lastLCDContrast != contrast) {
        g = ((double)contrast) / 100.0;
        ig = 1.0 / g;
        lastLCDContrast = contrast;
    }

    // update the current color settings
    double gamma = ((double)contrast) / 100.0;
    jfloat radj, gadj, badj;
    jfloat clr[4];
    jint col = [mtlc.paint getColor];
    clr[0] = ((col >> 16) & 0xFF)/255.0f;
    clr[1] = ((col >> 8) & 0xFF)/255.0f;
    clr[2] = ((col) & 0xFF)/255.0f;

    // gamma adjust the primary color
    radj = (float)pow(clr[0], gamma);
    gadj = (float)pow(clr[1], gamma);
    badj = (float)pow(clr[2], gamma);

    struct LCDFrameUniforms uf = {
            {radj, gadj, badj},
            {g, g, g},
            {ig, ig, ig}};
    [encoder setFragmentBytes:&uf length:sizeof(uf) atIndex:0];//FrameUniformBuffer];


    [encoder setFragmentTexture:glyphTexture.texture atIndex:0];
    [encoder setFragmentTexture:dstTexture atIndex:1];

    return JNI_TRUE;
}

void
MTLTR_EnableGlyphVertexCache(MTLContext *mtlc, BMTLSDOps *dstOps)
{
J2dTraceLn(J2D_TRACE_INFO, "MTLTR_EnableGlyphVertexCache");

    if (!MTLVertexCache_InitVertexCache()) {
        return;
    }

    if (glyphCacheAA == NULL) {
        if (!MTLTR_InitGlyphCache(mtlc)) {
            return;
        }
    }
    MTLVertexCache_CreateSamplingEncoder(mtlc, dstOps);
}

void
MTLTR_DisableGlyphVertexCache(MTLContext *mtlc)
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DisableGlyphVertexCache");
    MTLVertexCache_FlushGlyphVertexCache();
    MTLVertexCache_RestoreColorState(mtlc);
    MTLVertexCache_FreeVertexCache();
}

/**
 * Disables any pending state associated with the current "glyph mode".
 */
void
MTLTR_DisableGlyphModeState()
{
    // TODO : This is similar to OpenGL implementation
    // When LCD implementation is done weshould make
    // more changes.
    J2dTraceLn1(J2D_TRACE_VERBOSE,
                "MTLTR_DisableGlyphModeState: mode=%d", glyphMode);
    switch (glyphMode) {
    case MODE_NO_CACHE_LCD:
        // TODO : Along with LCD implementation
        // changes needs to be made
    case MODE_USE_CACHE_LCD:
        // TODO : Along with LCD implementation
        // changes needs to be made
        break;
    case MODE_NO_CACHE_GRAY:
    case MODE_USE_CACHE_GRAY:
    case MODE_NOT_INITED:
    default:
        break;
    }
}

static jboolean
MTLTR_DrawGrayscaleGlyphViaCache(MTLContext *mtlc,
                                 GlyphInfo *ginfo, jint x, jint y, BMTLSDOps *dstOps)
{
    MTLCacheCellInfo *cell;
    jfloat x1, y1, x2, y2;

    if (glyphMode != MODE_USE_CACHE_GRAY) {
        if (glyphMode == MODE_NO_CACHE_GRAY) {
            MTLVertexCache_DisableMaskCache(mtlc);
        }
        MTLTR_EnableGlyphVertexCache(mtlc, dstOps);
        glyphMode = MODE_USE_CACHE_GRAY;
    }

    if (ginfo->cellInfo == NULL) {
        // attempt to add glyph to accelerated glyph cache
        MTLTR_AddToGlyphCache(ginfo, mtlc);

        if (ginfo->cellInfo == NULL) {
            // we'll just no-op in the rare case that the cell is NULL
            return JNI_TRUE;
        }
    }

    cell = (MTLCacheCellInfo *) (ginfo->cellInfo);
    cell->timesRendered++;

    x1 = (jfloat)x;
    y1 = (jfloat)y;
    x2 = x1 + ginfo->width;
    y2 = y1 + ginfo->height;

    MTLVertexCache_AddGlyphQuad(mtlc,
                                cell->tx1, cell->ty1,
                                cell->tx2, cell->ty2,
                                x1, y1, x2, y2);

    return JNI_TRUE;
}

/**
 * Evaluates to true if the rectangle defined by gx1/gy1/gx2/gy2 is
 * inside outerBounds.
 */
#define INSIDE(gx1, gy1, gx2, gy2, outerBounds) \
    (((gx1) >= outerBounds.x1) && ((gy1) >= outerBounds.y1) && \
     ((gx2) <= outerBounds.x2) && ((gy2) <= outerBounds.y2))

/**
 * Evaluates to true if the rectangle defined by gx1/gy1/gx2/gy2 intersects
 * the rectangle defined by bounds.
 */
#define INTERSECTS(gx1, gy1, gx2, gy2, bounds) \
    ((bounds.x2 > (gx1)) && (bounds.y2 > (gy1)) && \
     (bounds.x1 < (gx2)) && (bounds.y1 < (gy2)))

/**
 * This method checks to see if the given LCD glyph bounds fall within the
 * cached destination texture bounds.  If so, this method can return
 * immediately.  If not, this method will copy a chunk of framebuffer data
 * into the cached destination texture and then update the current cached
 * destination bounds before returning.
 */
static void
MTLTR_UpdateCachedDestination(MTLSDOps *dstOps, GlyphInfo *ginfo,
                              jint gx1, jint gy1, jint gx2, jint gy2,
                              jint glyphIndex, jint totalGlyphs)
{
    //TODO
}

static jboolean
MTLTR_DrawLCDGlyphViaCache(MTLContext *mtlc, MTLSDOps *dstOps,
                           GlyphInfo *ginfo, jint x, jint y,
                           jint glyphIndex, jint totalGlyphs,
                           jboolean rgbOrder, jint contrast,
                           id<MTLTexture> dstTextureID)
{
    //TODO
    return JNI_TRUE;
}

static jboolean
MTLTR_DrawGrayscaleGlyphNoCache(MTLContext *mtlc,
                                GlyphInfo *ginfo, jint x, jint y, BMTLSDOps *dstOps)
{
    jint tw, th;
    jint sx, sy, sw, sh;
    jint x0;
    jint w = ginfo->width;
    jint h = ginfo->height;

    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGrayscaleGlyphNoCache");
    if (glyphMode != MODE_NO_CACHE_GRAY) {
        if (glyphMode == MODE_USE_CACHE_GRAY) {
            MTLTR_DisableGlyphVertexCache(mtlc);
        }
        MTLVertexCache_EnableMaskCache(mtlc, dstOps);
        glyphMode = MODE_NO_CACHE_GRAY;
    }

    x0 = x;
    tw = MTLVC_MASK_CACHE_TILE_WIDTH;
    th = MTLVC_MASK_CACHE_TILE_HEIGHT;

    for (sy = 0; sy < h; sy += th, y += th) {
        x = x0;
        sh = ((sy + th) > h) ? (h - sy) : th;

        for (sx = 0; sx < w; sx += tw, x += tw) {
            sw = ((sx + tw) > w) ? (w - sx) : tw;

            J2dTraceLn7(J2D_TRACE_INFO, "sx = %d sy = %d x = %d y = %d sw = %d sh = %d w = %d", sx, sy, x, y, sw, sh, w);
            MTLVertexCache_AddMaskQuad(mtlc,
                                       sx, sy, x, y, sw, sh,
                                       w, ginfo->image,
                                       dstOps,
                                       ginfo->width);
        }
    }

    return JNI_TRUE;
}

static jboolean
MTLTR_DrawLCDGlyphNoCache(MTLContext *mtlc, BMTLSDOps *dstOps,
                          GlyphInfo *ginfo, jint x, jint y,
                          jint rowBytesOffset,
                          jboolean rgbOrder, jint contrast,
                          id<MTLTexture> dstTexture)
{
    jfloat tx1, ty1, tx2, ty2;
    jfloat dtx1, dty1, dtx2, dty2;
    jint tw, th;
    jint sx, sy, sw, sh, dxadj, dyadj;
    jint x0;
    jint w = ginfo->width;
    jint h = ginfo->height;

    J2dTraceLn2(J2D_TRACE_INFO, "MTLTR_DrawLCDGlyphNoCache x %d, y%d", x, y);
    J2dTraceLn3(J2D_TRACE_INFO, "MTLTR_DrawLCDGlyphNoCache rowBytesOffset=%d, rgbOrder=%d, contrast=%d", rowBytesOffset, rgbOrder, contrast);

    MTLPooledTextureHandle *blitTexture = [mtlc.texturePool getTexture:MTLC_BLIT_TILE_SIZE height:MTLC_BLIT_TILE_SIZE format:MTLPixelFormatBGRA8Unorm];
    if (blitTexture == nil) {
        J2dTraceLn(J2D_TRACE_ERROR, "can't obtain temporary texture object from pool");
        return JNI_FALSE;
    }

    id<MTLRenderCommandEncoder> encoder = nil;

    if (glyphMode != MODE_NO_CACHE_LCD) {
        if (glyphMode == MODE_USE_CACHE_GRAY) {
            MTLTR_DisableGlyphVertexCache(mtlc);
        }
        MTLVertexCache_EnableMaskCache(mtlc, dstOps);
        encoder = [mtlc.encoderManager getTextureEncoder:dstTexture isSrcOpaque:YES isDstOpaque:YES];
        if (!MTLTR_EnableLCDGlyphModeState(encoder, mtlc, dstOps, blitTexture,
                                           dstTexture, contrast, x, y, w, h))
        {
            return JNI_FALSE;
        }
        glyphMode = MODE_NO_CACHE_LCD;
    }

    x0 = x;
    tx1 = 0.0f;
    ty1 = 0.0f;
    dtx1 = 0.0f;
    dty2 = 0.0f;
    tw = MTLTR_NOCACHE_TILE_SIZE;
    th = MTLTR_NOCACHE_TILE_SIZE;

    for (sy = 0; sy < h; sy += th, y += th) {
        x = x0;
        sh = ((sy + th) > h) ? (h - sy) : th;

        for (sx = 0; sx < w; sx += tw, x += tw) {
            sw = ((sx + tw) > w) ? (w - sx) : tw;

            J2dTraceLn7(J2D_TRACE_INFO, "sx = %d sy = %d x = %d y = %d sw = %d sh = %d w = %d", sx, sy, x, y, sw, sh, w);
            // copy LCD mask into glyph texture tile
            MTLRegion region = {
                {0,  0,   0},
                {sw, sh, 1}
            };
            NSUInteger bytesPerRow = 4 * ginfo->width;
            [blitTexture.texture replaceRegion:region
                         mipmapLevel:0
                         withBytes:ginfo->image + rowBytesOffset
                         bytesPerRow:bytesPerRow];

    struct Vertex vertices[4] = {
        {{x, y}},
        {{x + sw, y}},
        {{x + sw, y + sh}},
        {{x, y + sh}},
    };
    [encoder setVertexBytes:vertices length:sizeof(vertices) atIndex:MeshVertexBuffer];

            // update the lower-right glyph texture coordinates
            tx2 = ((jfloat)sw) / MTLC_BLIT_TILE_SIZE;
            ty2 = ((jfloat)sh) / MTLC_BLIT_TILE_SIZE;

            // this accounts for lower-left origin of the destination region
            dxadj = x;
            dyadj = dstOps->height - (y + sh);

            J2dTraceLn4(J2D_TRACE_INFO, "MTLTR_DrawLCDGlyphNoCache xOffset %d yOffset %d, dxadj %d, dyadj %d", dstOps->xOffset, dstOps->yOffset, dxadj, dyadj);

            dtx1 = ((jfloat)dxadj) / dstOps->textureWidth;
            dtx2 = ((float)dxadj + sw) / dstOps->textureWidth;
  
            dty1 = ((jfloat)dyadj + sh) / dstOps->textureHeight;
            dty2 = ((jfloat)dyadj) / dstOps->textureHeight;

            MTLRegion region1 = {
                {dtx1,  dty1,   0},
                {dtx2, dty2, 1}
            };
           J2dTraceLn4(J2D_TRACE_INFO, "OGLTR_DrawLCDGlyphNoCache tx1 %f, ty1 %f, tx2 %f, ty2 %f", tx1, ty1, tx2, ty2);
           J2dTraceLn2(J2D_TRACE_INFO, "MTLTR_DrawLCDGlyphNoCache textureWidth %d textureHeight %d", dstOps->textureWidth, dstOps->textureHeight);
           J2dTraceLn4(J2D_TRACE_INFO, "MTLTR_DrawLCDGlyphNoCache dtx1 %f, dty1 %f, dtx2 %f, dty2 %f", dtx1, dty1, dtx2, dty2);

            id<MTLTexture> tex = dstOps->pTexture;
            //[tex replaceRegion:region1
              //           mipmapLevel:0
                //         withBytes:blitTexture.texture
                  //       bytesPerRow:bytesPerRow];

            drawTex2Tex(mtlc, dstTexture, dstOps->pTexture, YES, YES, 0, 0, sw, sh, dxadj, dyadj+sh, dxadj+sw, dyadj);

        }
    }
            
    return JNI_TRUE;
}

static jboolean
MTLTR_DrawColorGlyphNoCache(MTLContext *mtlc, GlyphInfo *ginfo, jint x, jint y)
{
    //TODO
    return JNI_TRUE;
}


// see DrawGlyphList.c for more on this macro...
#define FLOOR_ASSIGN(l, r) \
    if ((r)<0) (l) = ((int)floor(r)); else (l) = ((int)(r))

void
MTLTR_DrawGlyphList(JNIEnv *env, MTLContext *mtlc, BMTLSDOps *dstOps,
                    jint totalGlyphs, jboolean usePositions,
                    jboolean subPixPos, jboolean rgbOrder, jint lcdContrast,
                    jfloat glyphListOrigX, jfloat glyphListOrigY,
                    unsigned char *images, unsigned char *positions)
{
    int glyphCounter;

    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList");

    RETURN_IF_NULL(mtlc);
    RETURN_IF_NULL(dstOps);
    RETURN_IF_NULL(images);
    if (usePositions) {
        RETURN_IF_NULL(positions);
    }

    glyphMode = MODE_NOT_INITED;
    isCachedDestValid = JNI_FALSE;
    J2dTraceLn1(J2D_TRACE_INFO, "totalGlyphs = %d", totalGlyphs);

    for (glyphCounter = 0; glyphCounter < totalGlyphs; glyphCounter++) {
        J2dTraceLn(J2D_TRACE_INFO, "Entered for loop for glyph list");
        jint x, y;
        jfloat glyphx, glyphy;
        jboolean grayscale, ok;
        GlyphInfo *ginfo = (GlyphInfo *)jlong_to_ptr(NEXT_LONG(images));

        if (ginfo == NULL) {
            // this shouldn't happen, but if it does we'll just break out...
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                          "MTLTR_DrawGlyphList: glyph info is null");
            break;
        }

        grayscale = (ginfo->rowBytes == ginfo->width);

        if (usePositions) {
            jfloat posx = NEXT_FLOAT(positions);
            jfloat posy = NEXT_FLOAT(positions);
            glyphx = glyphListOrigX + posx + ginfo->topLeftX;
            glyphy = glyphListOrigY + posy + ginfo->topLeftY;
            FLOOR_ASSIGN(x, glyphx);
            FLOOR_ASSIGN(y, glyphy);
        } else {
            glyphx = glyphListOrigX + ginfo->topLeftX;
            glyphy = glyphListOrigY + ginfo->topLeftY;
            FLOOR_ASSIGN(x, glyphx);
            FLOOR_ASSIGN(y, glyphy);
            glyphListOrigX += ginfo->advanceX;
            glyphListOrigY += ginfo->advanceY;
        }

        if (ginfo->image == NULL) {
            J2dTraceLn(J2D_TRACE_INFO, "Glyph image is null");
            continue;
        }

        J2dTraceLn2(J2D_TRACE_INFO, "Glyph width = %d height = %d", ginfo->width, ginfo->height);
        //TODO : Right now we have initial texture mapping logic
        // as we implement LCD, cache usage add new selection condition.
        if (grayscale) {
            // grayscale or monochrome glyph data
            if (ginfo->width <= MTLTR_CACHE_CELL_WIDTH &&
                ginfo->height <= MTLTR_CACHE_CELL_HEIGHT)
            {
                J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList Grayscale cache");
                ok = MTLTR_DrawGrayscaleGlyphViaCache(mtlc, ginfo, x, y, dstOps);
            } else {
                J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList Grayscale no cache");
                ok = MTLTR_DrawGrayscaleGlyphNoCache(mtlc, ginfo, x, y, dstOps);
            }
        } else {
            void* dstTexture = dstOps->textureLCD;

            // LCD-optimized glyph data
            jint rowBytesOffset = 0;

            if (subPixPos) {
                jint frac = (jint)((glyphx - x) * 3);
                if (frac != 0) {
                    rowBytesOffset = 3 - frac;
                    x += 1;
                }
            }

            // TODO: Implement LCD text rendering
            if (rowBytesOffset == 0 &&
                ginfo->width <= MTLTR_CACHE_CELL_WIDTH &&
                ginfo->height <= MTLTR_CACHE_CELL_HEIGHT)
            {
                J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList LCD cache -- :TODO");
                ok = MTLTR_DrawLCDGlyphViaCache(mtlc, dstOps,
                                                ginfo, x, y,
                                                glyphCounter, totalGlyphs,
                                                rgbOrder, lcdContrast,
                                                dstTexture);
            } else {
                J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList LCD no cache");
                ok = MTLTR_DrawLCDGlyphNoCache(mtlc, dstOps,
                                               ginfo, x, y,
                                               rowBytesOffset,
                                               rgbOrder, lcdContrast,
                                               dstTexture);
            }
        }

        if (!ok) {
            break;
        }
    }
    /*
     * This state management needs to be extended for other glyphmodes
     * when they are implemented.
     */
    if (glyphMode == MODE_NO_CACHE_GRAY ||
        glyphMode == MODE_NO_CACHE_LCD) {
        MTLVertexCache_DisableMaskCache(mtlc);
    } else {
        MTLTR_DisableGlyphVertexCache(mtlc);
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_metal_MTLTextRenderer_drawGlyphList
    (JNIEnv *env, jobject self,
     jint numGlyphs, jboolean usePositions,
     jboolean subPixPos, jboolean rgbOrder, jint lcdContrast,
     jfloat glyphListOrigX, jfloat glyphListOrigY,
     jlongArray imgArray, jfloatArray posArray)
{
    unsigned char *images;

    J2dTraceLn(J2D_TRACE_INFO, "MTLTextRenderer_drawGlyphList");

    images = (unsigned char *)
        (*env)->GetPrimitiveArrayCritical(env, imgArray, NULL);
    if (images != NULL) {
        MTLContext *mtlc = MTLRenderQueue_GetCurrentContext();
        BMTLSDOps *dstOps = MTLRenderQueue_GetCurrentDestination();

        if (usePositions) {
            unsigned char *positions = (unsigned char *)
                (*env)->GetPrimitiveArrayCritical(env, posArray, NULL);
            if (positions != NULL) {
                MTLTR_DrawGlyphList(env, mtlc, dstOps,
                                    numGlyphs, usePositions,
                                    subPixPos, rgbOrder, lcdContrast,
                                    glyphListOrigX, glyphListOrigY,
                                    images, positions);
                (*env)->ReleasePrimitiveArrayCritical(env, posArray,
                                                      positions, JNI_ABORT);
            }
        } else {
            MTLTR_DrawGlyphList(env, mtlc, dstOps,
                                numGlyphs, usePositions,
                                subPixPos, rgbOrder, lcdContrast,
                                glyphListOrigX, glyphListOrigY,
                                images, NULL);
        }

        (*env)->ReleasePrimitiveArrayCritical(env, imgArray,
                                              images, JNI_ABORT);
    }
}

#endif /* !HEADLESS */
