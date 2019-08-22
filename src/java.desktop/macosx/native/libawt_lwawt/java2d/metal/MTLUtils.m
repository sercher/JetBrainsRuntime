#include "MTLUtils.h"

#include <jni.h>
#include <simd/simd.h>
#include "common.h"
#include "Trace.h"

extern void J2dTraceImpl(int level, jboolean cr, const char *string, ...);
void J2dTraceTraceVector(simd_float4 pt) {
    J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_FALSE, "[%lf %lf %lf %lf]", pt.x, pt.y, pt.z, pt.w);
}

void checkTransform(float * position, simd_float4x4 transform4x4) {
    J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_FALSE, "check transform: ");

    simd_float4 fpt = simd_make_float4(position[0], position[1], position[2], 1.f);
    simd_float4 fpt_trans = simd_mul(transform4x4, fpt);
    J2dTraceTraceVector(fpt);
    J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_FALSE, "  ===>>>  ");
    J2dTraceTraceVector(fpt_trans);
    J2dTraceLn(J2D_TRACE_VERBOSE, " ");
}

static void traceMatrix(simd_float4x4 * mtx) {
    for (int row = 0; row < 4; ++row) {
        J2dTraceLn4(J2D_TRACE_VERBOSE, "  [%lf %lf %lf %lf]",
                    mtx->columns[0][row], mtx->columns[1][row], mtx->columns[2][row], mtx->columns[3][row]);
    }
}

void traceRaster(char * p, int width, int height, int stride) {
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            char pix0 = p[y*stride + x*4];
            char pix1 = p[y*stride + x*4 + 1];
            char pix2 = p[y*stride + x*4 + 2];
            char pix3 = p[y*stride + x*4 + 3];
            J2dTrace4(J2D_TRACE_INFO, "[%d,%d,%d,%d], ", pix0, pix1, pix2, pix3);
        }
        J2dTraceLn(J2D_TRACE_INFO, "");
    }
}

void tracePoints(jint nPoints, jint *xPoints, jint *yPoints) {
    for (int i = 0; i < nPoints; i++)
        J2dTraceLn2(J2D_TRACE_INFO, "\t(%d, %d)", *(xPoints++), *(yPoints++));
}
