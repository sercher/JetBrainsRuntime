/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

#include <jni_util.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include "hb.h"
#include "hb-jdk.h"
#include "hb-ot.h"
#include "scriptMapping.h"
#include "HBShaper.h"

static jclass gvdClass = 0;
static const char* gvdClassName = "sun/font/GlyphLayout$GVData";
static jfieldID gvdCountFID = 0;
static jfieldID gvdFlagsFID = 0;
static jfieldID gvdGlyphsFID = 0;
static jfieldID gvdPositionsFID = 0;
static jfieldID gvdIndicesFID = 0;
static jmethodID gvdGrowMID = 0;
static int jniInited = 0;

static void getFloat(JNIEnv* env, jobject pt, jfloat *x, jfloat *y) {
    *x = (*env)->GetFloatField(env, pt, sunFontIDs.xFID);
    *y = (*env)->GetFloatField(env, pt, sunFontIDs.yFID);
}

static void putFloat(JNIEnv* env, jobject pt, jfloat x, jfloat y) {
    (*env)->SetFloatField(env, pt, sunFontIDs.xFID, x);
    (*env)->SetFloatField(env, pt, sunFontIDs.yFID, y);
}

static int init_JNI_IDs(JNIEnv *env) {
    if (jniInited) {
        return jniInited;
    }
    CHECK_NULL_RETURN(gvdClass = (*env)->FindClass(env, gvdClassName), 0);
    CHECK_NULL_RETURN(gvdClass = (jclass)(*env)->NewGlobalRef(env, gvdClass), 0);
    CHECK_NULL_RETURN(gvdCountFID = (*env)->GetFieldID(env, gvdClass, "_count", "I"), 0);
    CHECK_NULL_RETURN(gvdFlagsFID = (*env)->GetFieldID(env, gvdClass, "_flags", "I"), 0);
    CHECK_NULL_RETURN(gvdGlyphsFID = (*env)->GetFieldID(env, gvdClass, "_glyphs", "[I"), 0);
    CHECK_NULL_RETURN(gvdPositionsFID = (*env)->GetFieldID(env, gvdClass, "_positions", "[F"), 0);
    CHECK_NULL_RETURN(gvdIndicesFID = (*env)->GetFieldID(env, gvdClass, "_indices", "[I"), 0);
    CHECK_NULL_RETURN(gvdGrowMID = (*env)->GetMethodID(env, gvdClass, "grow", "()V"), 0);
    jniInited = 1;
    return jniInited;
}

// gmask is the composite font slot mask
// baseindex is to be added to the character (code point) index.
jboolean storeGVData(JNIEnv* env,
                     jobject gvdata, jint slot, jint slotShift,
                     jint baseIndex, int offset, jobject startPt,
                     int charCount, int glyphCount, hb_glyph_info_t *glyphInfo,
                     hb_glyph_position_t *glyphPos, float devScale) {

    int i, needToGrow;
    float x=0, y=0;
    float startX, startY, advX, advY;
    float scale = 1.0f / HBFloatToFixedScale / devScale;
    unsigned int* glyphs;
    float* positions;
    int initialCount, glyphArrayLen, posArrayLen, maxGlyphs, storeadv, maxStore;
    unsigned int* indices;
    jarray glyphArray, posArray, inxArray;

    if (!init_JNI_IDs(env)) {
        return JNI_FALSE;
    }

    initialCount = (*env)->GetIntField(env, gvdata, gvdCountFID);
    do {
        glyphArray = (jarray)(*env)->GetObjectField(env, gvdata, gvdGlyphsFID);
        posArray = (jarray)(*env)->GetObjectField(env, gvdata, gvdPositionsFID);
        inxArray = (jarray)(*env)->GetObjectField(env, gvdata, gvdIndicesFID);
        if (glyphArray == NULL || posArray == NULL || inxArray == NULL) {
            JNU_ThrowArrayIndexOutOfBoundsException(env, "");
            return JNI_FALSE;
        }
        glyphArrayLen = (*env)->GetArrayLength(env, glyphArray);
        posArrayLen = (*env)->GetArrayLength(env, posArray);
        maxGlyphs = (charCount > glyphCount) ? charCount : glyphCount;
        maxStore = maxGlyphs + initialCount;
        needToGrow = (maxStore > glyphArrayLen) ||
                     (maxStore * 2 + 2 >  posArrayLen);
        if (needToGrow) {
            (*env)->CallVoidMethod(env, gvdata, gvdGrowMID);
            if ((*env)->ExceptionCheck(env)) {
                return JNI_FALSE;
            }
        }
    } while (needToGrow);

    getFloat(env, startPt, &startX, &startY);

    glyphs =
        (unsigned int*)(*env)->GetPrimitiveArrayCritical(env, glyphArray, NULL);
    if (glyphs == NULL) {
        return JNI_FALSE;
    }
    positions = (jfloat*)(*env)->GetPrimitiveArrayCritical(env, posArray, NULL);
    if (positions == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, glyphArray, glyphs, 0);
        return JNI_FALSE;
    }
    indices =
        (unsigned int*)(*env)->GetPrimitiveArrayCritical(env, inxArray, NULL);
    if (indices == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, glyphArray, glyphs, 0);
        (*env)->ReleasePrimitiveArrayCritical(env, posArray, positions, 0);
        return JNI_FALSE;
    }

    for (i = 0; i < glyphCount; i++) {
        int storei = i + initialCount;
        int cluster = glyphInfo[i].cluster - offset;
        indices[storei] = baseIndex + cluster;
        glyphs[storei] = (unsigned int)((glyphInfo[i].codepoint << slotShift) | slot);
        positions[storei*2] = startX + x + glyphPos[i].x_offset * scale;
        positions[(storei*2)+1] = startY + y - glyphPos[i].y_offset * scale;
        x += glyphPos[i].x_advance * scale;
        y += glyphPos[i].y_advance * scale;
        storei++;
    }
    storeadv = initialCount + glyphCount;
    // The final slot in the positions array is important
    // because when the GlyphVector is created from this
    // data it determines the overall advance of the glyphvector
    // and this is used in positioning the next glyphvector
    // during rendering where text is broken into runs.
    // We also need to report it back into "pt", so layout can
    // pass it back down for that next run in this code.
    advX = startX + x;
    advY = startY + y;
    positions[(storeadv*2)] = advX;
    positions[(storeadv*2)+1] = advY;
    (*env)->ReleasePrimitiveArrayCritical(env, glyphArray, glyphs, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, posArray, positions, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, inxArray, indices, 0);
    putFloat(env, startPt, advX, advY);
    (*env)->SetIntField(env, gvdata, gvdCountFID, storeadv);

    return JNI_TRUE;
}

static float euclidianDistance(float a, float b)
{
    float root;
    if (a < 0) {
        a = -a;
    }

    if (b < 0) {
        b = -b;
    }

    if (a == 0) {
        return b;
    }

    if (b == 0) {
        return a;
    }

    /* Do an initial approximation, in root */
    root = a > b ? a + (b / 2) : b + (a / 2);

    /* An unrolled Newton-Raphson iteration sequence */
    root = (root + (a * (a / root)) + (b * (b / root)) + 1) / 2;
    root = (root + (a * (a / root)) + (b * (b / root)) + 1) / 2;
    root = (root + (a * (a / root)) + (b * (b / root)) + 1) / 2;

    return root;
}

JDKFontInfo*
     createJDKFontInfo(JNIEnv *env,
                       jobject font2D,
                       jobject fontStrike,
                       jfloat ptSize,
                       jfloatArray matrix) {


    JDKFontInfo *fi = (JDKFontInfo*)malloc(sizeof(JDKFontInfo));
    if (!fi) {
       return NULL;
    }
    fi->env = env; // this is valid only for the life of this JNI call.
    fi->font2D = font2D;
    fi->fontStrike = fontStrike;
    (*env)->GetFloatArrayRegion(env, matrix, 0, 4, fi->matrix);
    fi->ptSize = ptSize;
    fi->xPtSize = euclidianDistance(fi->matrix[0], fi->matrix[1]);
    fi->yPtSize = euclidianDistance(fi->matrix[2], fi->matrix[3]);
    if (getenv("HB_NODEVTX") != NULL) {
        fi->devScale = fi->xPtSize / fi->ptSize;
    } else {
        fi->devScale = 1.0f;
    }
    return fi;
}

hb_buffer_t *create_buffer(int script, int ltrDirection) {
    hb_buffer_t *buffer = hb_buffer_create();

    hb_direction_t direction = ltrDirection ? HB_DIRECTION_LTR : HB_DIRECTION_RTL;
    hb_buffer_set_script(buffer, getHBScriptCode(script));
    hb_buffer_set_language(buffer, hb_ot_tag_to_language(HB_OT_TAG_DEFAULT_LANGUAGE));
    hb_buffer_set_direction(buffer, direction);
    hb_buffer_set_cluster_level(buffer, HB_BUFFER_CLUSTER_LEVEL_MONOTONE_CHARACTERS);

    return buffer;
}

bool shape_full(hb_font_t* hbfont, hb_buffer_t *buffer, const char *featuresStr) {
    if (featuresStr == NULL) {
        return false;
    }

    const char SEPARATOR = ';';
    int featuresCount = 1;
    int featuresStrLength = strlen(featuresStr);
    for (int i = 0; i < featuresStrLength; i++) {
        featuresCount += featuresStr[i] == SEPARATOR ? 1 : 0;
    }
    hb_feature_t *features = calloc(featuresCount, sizeof(hb_feature_t));
    jboolean res = true;
    if (features == NULL) {
        return false;
    }

    const char *iter = featuresStr;
    for (int i = 0; i < featuresCount; i++) {
        char *next = strchr(iter, SEPARATOR);
        if (!hb_feature_from_string(iter, next != NULL ? next - iter : -1, &features[i])) {
            res = false;
            goto cleanup;
        }
        iter = next + 1;
    }

    res = hb_shape_full(hbfont, buffer, features, featuresCount, 0);
cleanup:
    free(features);
    return res;
}

static hb_tag_t *createFeatureTags(hb_face_t *face, int tag, int *count) {
    *count = hb_ot_layout_table_get_feature_tags(face, tag, 0, NULL, NULL);
    if (*count == 0) {
        return NULL;
    }
    hb_tag_t *res = calloc(*count, sizeof(hb_tag_t));

    hb_ot_layout_table_get_feature_tags(face, tag, 0, (unsigned int *)count, res);
    for (int i = 0; i < *count; i++) {
        hb_tag_to_string(*(res + i), (char*)(res + i));
    }
    return res;
}

JNIEXPORT jstring JNICALL Java_sun_font_SunLayoutEngine_getFeatures
        (JNIEnv *env, jclass cls, jlong pFace) {

    int gposFeatureCount, gsubFeatureCount;

    hb_face_t *hbface = (hb_face_t*) jlong_to_ptr(pFace);
    hb_tag_t *gposFeatureTags = createFeatureTags(hbface, HB_OT_TAG_GPOS, &gposFeatureCount);
    hb_tag_t *gsubFeatureTags = createFeatureTags(hbface, HB_OT_TAG_GSUB, &gsubFeatureCount);

    char *res = malloc((gposFeatureCount + gsubFeatureCount) * sizeof(hb_tag_t) + 1);
    memcpy(res, gposFeatureTags, gposFeatureCount * sizeof(hb_tag_t));
    memcpy(res + gposFeatureCount * sizeof(hb_tag_t), gsubFeatureTags, gsubFeatureCount * sizeof(hb_tag_t));
    *(res + (gposFeatureCount + gsubFeatureCount) * sizeof(hb_tag_t)) = '\0';

    jstring jStrRes = (*env)->NewStringUTF(env,res);
    free(gposFeatureTags);
    free(gsubFeatureTags);
    free(res);

    return jStrRes;
}

JNIEXPORT jboolean JNICALL Java_sun_font_SunLayoutEngine_shape
    (JNIEnv *env, jclass cls,
     jobject font2D,
     jobject fontStrike,
     jfloat ptSize,
     jfloatArray matrix,
     jlong pFace,
     jcharArray text,
     jobject gvdata,
     jint script,
     jint offset,
     jint limit,
     jint baseIndex,
     jobject startPt,
     jboolean ltrDirection,
     jstring features,
     jint slot,
     jint slotShift) {

     hb_buffer_t *buffer;
     hb_face_t* hbface;
     hb_font_t* hbfont;
     jchar  *chars;
     jsize len;
     int glyphCount;
     hb_glyph_info_t *glyphInfo;
     hb_glyph_position_t *glyphPos;
     const char *featuresPtr = NULL;
     jboolean ret = JNI_TRUE;
     unsigned int buflen;

     JDKFontInfo *jdkFontInfo =
         createJDKFontInfo(env, font2D, fontStrike, ptSize, matrix);
     if (!jdkFontInfo) {
        return JNI_FALSE;
     }
     jdkFontInfo->env = env; // this is valid only for the life of this JNI call.
     jdkFontInfo->font2D = font2D;
     jdkFontInfo->fontStrike = fontStrike;

     hbface = (hb_face_t*) jlong_to_ptr(pFace);
     hbfont = hb_jdk_font_create(hbface, jdkFontInfo, NULL);

     buffer = create_buffer(script, ltrDirection);

     chars = (*env)->GetCharArrayElements(env, text, NULL);
     if (chars == NULL || (*env)->ExceptionCheck(env)) {
         ret = JNI_FALSE;
         goto cleanup;
     }
     len = (*env)->GetArrayLength(env, text);
     hb_buffer_add_utf16(buffer, chars, len, offset, limit-offset);

     featuresPtr = (*env)->GetStringUTFChars(env, features, NULL);
     if (!shape_full(hbfont, buffer, featuresPtr)) {
         ret = JNI_FALSE;
         goto cleanup;
     }

     glyphCount = hb_buffer_get_length(buffer);
     glyphInfo = hb_buffer_get_glyph_infos(buffer, 0);
     glyphPos = hb_buffer_get_glyph_positions(buffer, &buflen);

     ret = storeGVData(env, gvdata, slot, slotShift, baseIndex, offset, startPt,
                       limit - offset, glyphCount, glyphInfo, glyphPos,
                       jdkFontInfo->devScale);

cleanup:
     if (featuresPtr) {
         (*env)->ReleaseStringUTFChars(env, features, featuresPtr);
     }
     if (chars) {
         (*env)->ReleaseCharArrayElements(env, text, chars, JNI_ABORT);
     }
     hb_buffer_destroy(buffer);
     hb_font_destroy(hbfont);
     free((void*)jdkFontInfo);
     return ret;
}

