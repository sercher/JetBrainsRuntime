/*
 * Copyright 2005-2007 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */

import java.io.*;
import java.util.*;

public class WinGammaPlatformVC8 extends WinGammaPlatformVC7 {

    String projectVersion() {return "8.00";};

}

class CompilerInterfaceVC8 extends CompilerInterfaceVC7 {

    Vector getBaseCompilerFlags(Vector defines, Vector includes, String outDir) {
        Vector rv = new Vector();

        getBaseCompilerFlags_common(defines,includes, outDir, rv);
        // Set /Yu option. 2 is pchUseUsingSpecific
        addAttr(rv, "UsePrecompiledHeader", "2");
        // Set /EHsc- option. 0 is cppExceptionHandlingNo
        addAttr(rv, "ExceptionHandling", "0");

        return rv;
    }


    Vector getDebugCompilerFlags(String opt) {
        Vector rv = new Vector();

        getDebugCompilerFlags_common(opt,rv);

        return rv;
    }

    Vector getProductCompilerFlags() {
        Vector rv = new Vector();

        getProductCompilerFlags_common(rv);

        return rv;
    }


}
