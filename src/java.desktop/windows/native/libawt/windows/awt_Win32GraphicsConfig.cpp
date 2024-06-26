/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "awt.h"
#include <sun_awt_Win32GraphicsConfig.h>
#include "awt_Win32GraphicsConfig.h"
#include "awt_Canvas.h"
#include "awt_Win32GraphicsDevice.h"
#include "Devices.h"

//Info for building a ColorModel
#include "java_awt_image_DataBuffer.h"


//Local utility functions
static int shiftToMask(int numBits, int shift);

/*
 * AwtWin32GraphicsConfig fields
 */

jfieldID AwtWin32GraphicsConfig::win32GCVisualID;

/*
 * Class:     sun_awt_Win32GraphicsConfig
 * Method:    initIDs
 * Signature: ()V
 */

JNIEXPORT void JNICALL
Java_sun_awt_Win32GraphicsConfig_initIDs
    (JNIEnv *env, jclass thisCls)
{
    TRY;
    AwtWin32GraphicsConfig::win32GCVisualID = env->GetFieldID(thisCls,
         "visual", "I");
    DASSERT(AwtWin32GraphicsConfig::win32GCVisualID);
        CATCH_BAD_ALLOC;
}

/*
 *  shiftToMask:
 *  This function converts between cXXXBits and cXXXShift
 *  fields in the Windows GDI PIXELFORMATDESCRIPTOR and the mask
 *  fields passed to the DirectColorModel constructor.
 */
inline int shiftToMask(int numBits, int shift) {
    int mask = 0xFFFFFFFF;

    //Shift in numBits 0s
    mask = mask << numBits;
    mask = ~mask;
    //shift left by value of shift
    mask = mask << shift;
    return mask;
}
