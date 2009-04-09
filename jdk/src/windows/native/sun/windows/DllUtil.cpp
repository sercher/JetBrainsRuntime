/*
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 */


#include "DllUtil.h"

// Disable warning about using this in the initializer list.
#pragma warning( disable : 4355)

DllUtil::~DllUtil()
{
    if (module != NULL) {
        ::FreeLibrary(module);
        module = NULL;
    }
}

HMODULE DllUtil::GetModule()
{
    if (!module) {
        module = ::LoadLibrary(name);
    }
    return module;
}

FARPROC DllUtil::GetProcAddress(LPCSTR name)
{
    if (GetModule()) {
        return ::GetProcAddress(GetModule(), name);
    }
    throw LibraryUnavailableException();
}

DwmAPI & DwmAPI::GetInstance()
{
    static DwmAPI dll;
    return dll;
}

DwmAPI::DwmAPI() :
    DllUtil(_T("DWMAPI.DLL")),
    DwmIsCompositionEnabledFunction((DllUtil*)this, "DwmIsCompositionEnabled"),
    DwmGetWindowAttributeFunction((DllUtil*)this, "DwmGetWindowAttribute")
{
}

HRESULT DwmAPI::DwmIsCompositionEnabled(BOOL * pfEnabled)
{
    if (GetInstance().DwmIsCompositionEnabledFunction()) {
        return GetInstance().DwmIsCompositionEnabledFunction()(pfEnabled);
    }
    throw FunctionUnavailableException();
}

HRESULT DwmAPI::DwmGetWindowAttribute(HWND hwnd, DWORD dwAttribute,
        PVOID pvAttribute, DWORD cbAttribute)
{
    if (GetInstance().DwmGetWindowAttributeFunction()) {
        return GetInstance().DwmGetWindowAttributeFunction()(hwnd, dwAttribute,
                pvAttribute, cbAttribute);
    }
    throw FunctionUnavailableException();
}


