/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef MTLSurfaceDataBase_h_Included
#define MTLSurfaceDataBase_h_Included

#include "java_awt_image_AffineTransformOp.h"
#include "sun_java2d_pipe_hw_AccelSurface.h"

#include "SurfaceData.h"
#include "Trace.h"

/**
 * The MTLSDOps structure describes a native Metal surface and contains all
 * information pertaining to the native surface.  Some information about
 * the more important/different fields:
 *
 *     void* privOps;
 * Pointer to native-specific (Metal) SurfaceData info, such as the
 * native Drawable handle and GraphicsConfig data.
 *
 *     jint drawableType;
 * The surface type; can be any one of the surface type constants defined
 * below (MTLSD_WINDOW, MTLSD_TEXTURE, etc).
 *
 *     jboolean isOpaque;
 * If true, the surface should be treated as being fully opaque.  If
 * the underlying surface (e.g. MTLTexture/MTLBuffer) has an alpha channel and
 * isOpaque is true, then we should take appropriate action to ensure that the
 * surface remains fully opaque.
 *
 *     jint width/height;
 * The cached surface bounds.  For offscreen surface types (
 * MTLSD_TEXTURE, MTLSD_RT_TEXTURE etc.) these values must remain constant.
 * Onscreen window surfaces (MTLSD_WINDOW, MTLSD_FLIP_BACKBUFFER, etc.) may
 * have their bounds changed in response to a programmatic or user-initiated
 * event, so these values represent the last known dimensions. To determine the
 * true current bounds of this surface, query the native Drawable through the
 * privOps field.
 *
 *     void* pTexture;
 * The texture object handle, as generated by MTLTextureDescriptor(). If this
 * value is null, the texture has not yet been initialized.
 *
 *     void* pStencilTexture;
 * The byte buffer stencil mask used in rendering Metal rendering pass.
 */
typedef struct {
    SurfaceDataOps               sdOps;
    void*                        privOps;
    jobject                      graphicsConfig;
    jint                         drawableType;
    jboolean                     isOpaque;
    jint                         width;
    jint                         height;
    void*                        pTexture;
    void*                        pOutTexture;
    void*                        pStencilData;      // stencil data to be rendered to this buffer
    void*                        pStencilTexture;   // stencil texture byte buffer stencil mask used in main rendering
} BMTLSDOps;

#define MTLSD_UNDEFINED       sun_java2d_pipe_hw_AccelSurface_UNDEFINED
#define MTLSD_WINDOW          sun_java2d_pipe_hw_AccelSurface_WINDOW
#define MTLSD_TEXTURE         sun_java2d_pipe_hw_AccelSurface_TEXTURE
#define MTLSD_FLIP_BACKBUFFER sun_java2d_pipe_hw_AccelSurface_FLIP_BACKBUFFER
#define MTLSD_RT_TEXTURE      sun_java2d_pipe_hw_AccelSurface_RT_TEXTURE

/**
 * These are shorthand names for the filtering method constants used by
 * image transform methods.
 */
#define MTLSD_XFORM_DEFAULT 0
#define MTLSD_XFORM_NEAREST_NEIGHBOR \
    java_awt_image_AffineTransformOp_TYPE_NEAREST_NEIGHBOR
#define MTLSD_XFORM_BILINEAR \
    java_awt_image_AffineTransformOp_TYPE_BILINEAR

/**
 * The SurfaceRasterFlags structure contains information about raster (of some MTLTexture):
 *
 *     jboolean isOpaque;
 * If true, indicates that this pixel format hasn't alpha component (and values of this component can contain garbage).
 *
 *     jboolean isPremultiplied;
 * If true, indicates that this pixel format contains color components that have been pre-multiplied by their
 * corresponding alpha component.
*/
typedef struct {
    jboolean isOpaque;
    jboolean isPremultiplied;
} SurfaceRasterFlags;

/**
 * Exported methods.
 */
jint MTLSD_Lock(JNIEnv *env,
                SurfaceDataOps *ops, SurfaceDataRasInfo *pRasInfo,
                jint lockflags);
void MTLSD_GetRasInfo(JNIEnv *env,
                      SurfaceDataOps *ops, SurfaceDataRasInfo *pRasInfo);
void MTLSD_Unlock(JNIEnv *env,
                  SurfaceDataOps *ops, SurfaceDataRasInfo *pRasInfo);
void MTLSD_Dispose(JNIEnv *env, SurfaceDataOps *ops);
void MTLSD_Delete(JNIEnv *env, BMTLSDOps *mtlsdo);
jint MTLSD_NextPowerOfTwo(jint val, jint max);

#endif /* MTLSurfaceDataBase_h_Included */
