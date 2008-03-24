/*
 * Copyright 1996-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

#include "windows.h"
#include <windowsx.h>
#include <zmouse.h>

#include "jlong.h"
#include "awt_AWTEvent.h"
#include "awt_Component.h"
#include "awt_Cursor.h"
#include "awt_Dimension.h"
#include "awt_Frame.h"
#include "awt_InputEvent.h"
#include "awt_InputTextInfor.h"
#include "awt_Insets.h"
#include "awt_KeyEvent.h"
#include "awt_KeyboardFocusManager.h"
#include "awt_MenuItem.h"
#include "awt_MouseEvent.h"
#include "awt_Palette.h"
#include "awt_Toolkit.h"
#include "awt_Unicode.h"
#include "awt_Window.h"
#include "awt_Win32GraphicsDevice.h"
#include "ddrawUtils.h"
#include "Hashtable.h"
#include "ComCtl32Util.h"

#include <Region.h>

#include <jawt.h>

#include <java_awt_Toolkit.h>
#include <java_awt_FontMetrics.h>
#include <java_awt_Color.h>
#include <java_awt_Event.h>
#include <java_awt_event_KeyEvent.h>
#include <java_awt_Insets.h>
#include <java_awt_KeyboardFocusManager.h>
#include <sun_awt_windows_WPanelPeer.h>
#include <java_awt_event_InputEvent.h>
#include <java_awt_event_InputMethodEvent.h>
#include <sun_awt_windows_WInputMethod.h>
#include <java_awt_event_MouseEvent.h>
#include <java_awt_event_MouseWheelEvent.h>

// Begin -- Win32 SDK include files
#include <tchar.h>
#include <imm.h>
#include <ime.h>
// End -- Win32 SDK include files

#ifndef GET_KEYSTATE_WPARAM     // defined for (_WIN32_WINNT >= 0x0400)
#define GET_KEYSTATE_WPARAM(wParam)     (LOWORD(wParam))
#endif

#ifndef GET_WHEEL_DELTA_WPARAM  // defined for (_WIN32_WINNT >= 0x0500)
#define GET_WHEEL_DELTA_WPARAM(wParam)  ((short)HIWORD(wParam))
#endif

// <XXX> <!-- TEMPORARY HACK TO TEST AGAINST OLD VC INLCUDES -->
#if !defined(__int3264)
#define GetWindowLongPtr GetWindowLong
#define SetWindowLongPtr SetWindowLong
#define GWLP_USERDATA GWL_USERDATA
#define GWLP_WNDPROC  GWL_WNDPROC
typedef __int32 LONG_PTR;
typedef unsigned __int32 ULONG_PTR;
#endif // __int3264
// </XXX>

#include <awt_DnDDT.h>

LPCTSTR szAwtComponentClassName = TEXT("SunAwtComponent");
// register a message that no other window in the process (even in a plugin
// scenario) will be using
const UINT AwtComponent::WmAwtIsComponent =
    ::RegisterWindowMessage(szAwtComponentClassName);

static HWND g_hwndDown = NULL;
static DCList activeDCList;
static DCList passiveDCList;

extern void CheckFontSmoothingSettings(HWND);

extern "C" {
    // Remember the input language has changed by some user's action
    // (Alt+Shift or through the language icon on the Taskbar) to control the
    // race condition between the toolkit thread and the AWT event thread.
    // This flag remains TRUE until the next WInputMethod.getNativeLocale() is
    // issued.
    BOOL g_bUserHasChangedInputLang = FALSE;
}

BOOL AwtComponent::sm_suppressFocusAndActivation;
HWND AwtComponent::sm_focusOwner;
HWND AwtComponent::sm_focusedWindow;
HWND AwtComponent::sm_realFocusOpposite;
BOOL AwtComponent::sm_bMenuLoop = FALSE;
AwtComponent* AwtComponent::sm_getComponentCache = NULL;

/************************************************************************/
// Struct for _Reshape() and ReshapeNoCheck() methods
struct ReshapeStruct {
    jobject component;
    jint x, y;
    jint w, h;
};
// Struct for _NativeHandleEvent() method
struct NativeHandleEventStruct {
    jobject component;
    jobject event;
};
// Struct for _SetForeground() and _SetBackground() methods
struct SetColorStruct {
    jobject component;
    jint rgb;
};
// Struct for _SetFont() method
struct SetFontStruct {
    jobject component;
    jobject font;
};
// Struct for _RequestFocus() method
struct RequestFocusStruct {
    jobject component;
    jobject lightweightChild;
    jboolean temporary;
    jboolean focusedWindowChangeAllowed;
    jlong time;
    jobject cause;
};
// Struct for _CreatePrintedPixels() method
struct CreatePrintedPixelsStruct {
    jobject component;
    int srcx, srcy;
    int srcw, srch;
};
// Struct for _SetRectangularShape() method
struct SetRectangularShapeStruct {
    jobject component;
    jint x1, x2, y1, y2;
    jobject region;
};
/************************************************************************/

//////////////////////////////////////////////////////////////////////////

/*************************************************************************
 * AwtComponent fields
 */


jfieldID AwtComponent::peerID;
jfieldID AwtComponent::xID;
jfieldID AwtComponent::yID;
jfieldID AwtComponent::widthID;
jfieldID AwtComponent::heightID;
jfieldID AwtComponent::visibleID;
jfieldID AwtComponent::backgroundID;
jfieldID AwtComponent::foregroundID;
jfieldID AwtComponent::enabledID;
jfieldID AwtComponent::parentID;
jfieldID AwtComponent::graphicsConfigID;
jfieldID AwtComponent::peerGCID;
jfieldID AwtComponent::focusableID;
jfieldID AwtComponent::appContextID;
jfieldID AwtComponent::cursorID;
jfieldID AwtComponent::hwndID;

jmethodID AwtComponent::getFontMID;
jmethodID AwtComponent::getToolkitMID;
jmethodID AwtComponent::isEnabledMID;
jmethodID AwtComponent::getLocationOnScreenMID;
jmethodID AwtComponent::replaceSurfaceDataMID;
jmethodID AwtComponent::replaceSurfaceDataLaterMID;

HKL    AwtComponent::m_hkl = ::GetKeyboardLayout(0);
LANGID AwtComponent::m_idLang = LOWORD(::GetKeyboardLayout(0));
UINT   AwtComponent::m_CodePage
                       = AwtComponent::LangToCodePage(m_idLang);

BOOL AwtComponent::m_isWin95 = IS_WIN95;
BOOL AwtComponent::m_isWin2000 = IS_WIN2000;
BOOL AwtComponent::m_isWinNT = IS_NT;

static BOOL bLeftShiftIsDown = false;
static BOOL bRightShiftIsDown = false;
static UINT lastShiftKeyPressed = 0; // init to safe value

// Added by waleed to initialize the RTL Flags
BOOL AwtComponent::sm_rtl = PRIMARYLANGID(GetInputLanguage()) == LANG_ARABIC ||
                            PRIMARYLANGID(GetInputLanguage()) == LANG_HEBREW;
BOOL AwtComponent::sm_rtlReadingOrder =
    PRIMARYLANGID(GetInputLanguage()) == LANG_ARABIC;

UINT AwtComponent::sm_95WheelMessage = WM_NULL;
UINT AwtComponent::sm_95WheelSupport = WM_NULL;

HWND AwtComponent::sm_cursorOn;
BOOL AwtComponent::m_QueryNewPaletteCalled = FALSE;

CriticalSection windowMoveLock;
BOOL windowMoveLockHeld = FALSE;

int AwtComponent::sm_wheelRotationAmount = 0;

/************************************************************************
 * AwtComponent methods
 */

AwtComponent::AwtComponent()
{
    m_callbacksEnabled = FALSE;
    m_hwnd = NULL;

    m_colorForeground = 0;
    m_colorBackground = 0;
    m_backgroundColorSet = FALSE;
    m_penForeground = NULL;
    m_brushBackground = NULL;
    m_DefWindowProc = NULL;
    m_nextControlID = 1;
    m_childList = NULL;
    m_myControlID = 0;
    m_mouseDragState = 0;
    m_hdwp = NULL;
    m_validationNestCount = 0;

    m_dropTarget = NULL;

    m_InputMethod = NULL;
    m_useNativeCompWindow = TRUE;
    m_PendingLeadByte = 0;
    m_skipNextSetFocus = FALSE;
    m_bitsCandType = 0;

    windowMoveLockPosX = 0;
    windowMoveLockPosY = 0;
    windowMoveLockPosCX = 0;
    windowMoveLockPosCY = 0;

    m_hCursorCache = NULL;

    m_bSubclassed = FALSE;

    m_MessagesProcessing = 0;
}

AwtComponent::~AwtComponent()
{
    DASSERT(AwtToolkit::IsMainThread());

    /* Disconnect all links. */
    UnlinkObjects();

    /*
     * All the messages for this component are processed, native
     * resources are freed, and Java object is not connected to
     * the native one anymore. So we can safely destroy component's
     * handle.
     */
    AwtToolkit::DestroyComponentHWND(m_hwnd);
    m_hwnd = NULL;

    if (sm_getComponentCache == this) {
        sm_getComponentCache = NULL;
    }
}

void AwtComponent::Dispose()
{
    if (sm_focusOwner == GetHWnd()) {
        ::SetFocus(NULL);
    }
    if (sm_focusedWindow == GetHWnd()) {
        sm_focusedWindow = NULL;
    }
    if (sm_realFocusOpposite == GetHWnd()) {
        sm_realFocusOpposite = NULL;
    }

    if (m_hdwp != NULL) {
    // end any deferred window positioning, regardless
    // of m_validationNestCount
        ::EndDeferWindowPos(m_hdwp);
    }

    // Send final message to release all DCs associated with this component
    SendMessage(WM_AWT_RELEASE_ALL_DCS);

    /* Stop message filtering. */
    UnsubclassHWND();

    /* Release global ref to input method */
    SetInputMethod(NULL, TRUE);

    if (m_childList != NULL)
        delete m_childList;

    DestroyDropTarget();

    if (m_myControlID != 0) {
        AwtComponent* parent = GetParent();
        if (parent != NULL)
            parent->RemoveChild(m_myControlID);
    }

    ::RemoveProp(GetHWnd(), DrawingStateProp);

    /* Release any allocated resources. */
    if (m_penForeground != NULL) {
        m_penForeground->Release();
        m_penForeground = NULL;
    }
    if (m_brushBackground != NULL) {
        m_brushBackground->Release();
        m_brushBackground = NULL;
    }

    // The component instance is deleted using AwtObject::Dispose() method
    AwtObject::Dispose();
}

/* store component pointer in window extra bytes */
void AwtComponent::SetComponentInHWND() {
    DASSERT(::GetWindowLongPtr(GetHWnd(), GWLP_USERDATA) == NULL);
    ::SetWindowLongPtr(GetHWnd(), GWLP_USERDATA, (LONG_PTR)this);
}

/*
 * static function to get AwtComponent pointer from hWnd --
 * you don't want to call this from inside a wndproc to avoid
 * infinite recursion
 */
AwtComponent* AwtComponent::GetComponent(HWND hWnd) {
    // Requests for Toolkit hwnd resolution happen pretty often. Check first.
    if (hWnd == AwtToolkit::GetInstance().GetHWnd()) {
        return NULL;
    }
    if (sm_getComponentCache && sm_getComponentCache->GetHWnd() == hWnd) {
        return sm_getComponentCache;
    }

    // check that it's an AWT component from the same toolkit as the caller
    if (::IsWindow(hWnd) &&
        AwtToolkit::MainThread() == ::GetWindowThreadProcessId(hWnd, NULL))
    {
        DASSERT(WmAwtIsComponent != 0);
        if (::SendMessage(hWnd, WmAwtIsComponent, 0, 0L)) {
            return sm_getComponentCache = GetComponentImpl(hWnd);
        }
    }
    return NULL;
}

/*
 * static function to get AwtComponent pointer from hWnd--
 * different from GetComponent because caller knows the
 * hwnd is an AWT component hwnd
 */
AwtComponent* AwtComponent::GetComponentImpl(HWND hWnd) {
    AwtComponent *component =
        (AwtComponent *)::GetWindowLongPtr(hWnd, GWLP_USERDATA);
    DASSERT( !IsBadReadPtr(component, sizeof(AwtComponent)) );
    DASSERT( component->GetHWnd() == hWnd );
    return component;
}

/*
 * Single window proc for all the components. Delegates real work to
 * the component's WindowProc() member function.
 */
LRESULT CALLBACK AwtComponent::WndProc(HWND hWnd, UINT message,
                                       WPARAM wParam, LPARAM lParam)
{
    TRY;

    AwtComponent * self = AwtComponent::GetComponentImpl(hWnd);
    if (self == NULL || self->GetHWnd() != hWnd) {
        return ComCtl32Util::GetInstance().DefWindowProc(NULL, hWnd, message, wParam, lParam);
    } else {
        return self->WindowProc(message, wParam, lParam);
    }

    CATCH_BAD_ALLOC_RET(0);
}

BOOL AwtComponent::IsFocusable() {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jobject peer = GetPeer(env);
    jobject target = env->GetObjectField(peer, AwtObject::targetID);
    BOOL res = env->GetBooleanField(target, focusableID);
    res &= GetContainer()->IsFocusableWindow();
    env->DeleteLocalRef(target);
    return res;
}

/************************************************************************
 * AwtComponent dynamic methods
 *
 * Window class registration routines
 */

/*
 * Fix for 4964237: Win XP: Changing theme changes java dialogs title icon
 */
void AwtComponent::FillClassInfo(WNDCLASSEX *lpwc)
{
    lpwc->cbSize        = sizeof(WNDCLASSEX);
    lpwc->style         = 0L;//CS_OWNDC;
    lpwc->lpfnWndProc   = (WNDPROC)::DefWindowProc;
    lpwc->cbClsExtra    = 0;
    lpwc->cbWndExtra    = 0;
    lpwc->hInstance     = AwtToolkit::GetInstance().GetModuleHandle(),
    lpwc->hIcon         = AwtToolkit::GetInstance().GetAwtIcon();
    lpwc->hCursor       = NULL;
    lpwc->hbrBackground = NULL;
    lpwc->lpszMenuName  = NULL;
    lpwc->lpszClassName = GetClassName();
    //Fixed 6233560: PIT: Java Cup Logo on the title bar of top-level windows look blurred, Win32
    lpwc->hIconSm       = AwtToolkit::GetInstance().GetAwtIconSm();
}

void AwtComponent::RegisterClass()
{
    WNDCLASSEX wc;
    if (!::GetClassInfoEx(AwtToolkit::GetInstance().GetModuleHandle(), GetClassName(), &wc)) {
        FillClassInfo(&wc);
        ATOM ret = ::RegisterClassEx(&wc);
        DASSERT(ret != 0);
    }
}

void AwtComponent::UnregisterClass()
{
    ::UnregisterClass(GetClassName(), AwtToolkit::GetInstance().GetModuleHandle());
}

/*
 * Copy the graphicsConfig reference from Component into WComponentPeer
 */
void AwtComponent::InitPeerGraphicsConfig(JNIEnv *env, jobject peer)
{
    jobject target = env->GetObjectField(peer, AwtObject::targetID);
    //Get graphicsConfig object ref from Component
    jobject compGC = env->GetObjectField(target,
                      AwtComponent::graphicsConfigID);

    //Set peer's graphicsConfig to Component's graphicsConfig
    if (compGC != NULL) {
        jclass win32GCCls = env->FindClass("sun/awt/Win32GraphicsConfig");
        DASSERT(win32GCCls != NULL);
        DASSERT(env->IsInstanceOf(compGC, win32GCCls));
        env->SetObjectField(peer, AwtComponent::peerGCID, compGC);
    }
}

void
AwtComponent::CreateHWnd(JNIEnv *env, LPCWSTR title,
                         DWORD windowStyle,
                         DWORD windowExStyle,
                         int x, int y, int w, int h,
                         HWND hWndParent, HMENU hMenu,
                         COLORREF colorForeground,
                         COLORREF colorBackground,
                         jobject peer)
{
    if (env->EnsureLocalCapacity(2) < 0) {
        return;
    }

    /*
     * The window class of multifont label must be "BUTTON" because
     * "STATIC" class can't get WM_DRAWITEM message, and m_peerObject
     * member is referred in the GetClassName method of AwtLabel class.
     * So m_peerObject member must be set here.
     */
    m_peerObject = env->NewGlobalRef(peer);
    RegisterClass();

    jobject target = env->GetObjectField(peer, AwtObject::targetID);
    jboolean visible = env->GetBooleanField(target, AwtComponent::visibleID);
    m_visible = visible;

    if (visible) {
        windowStyle |= WS_VISIBLE;
    } else {
        windowStyle &= ~WS_VISIBLE;
    }

    InitPeerGraphicsConfig(env, peer);

    SetLastError(0);
    HWND hwnd = ::CreateWindowEx(windowExStyle,
                                 GetClassName(),
                                 title,
                                 windowStyle,
                                 x, y, w, h,
                                 hWndParent,
                                 hMenu,
                                 AwtToolkit::GetInstance().GetModuleHandle(),
                                 NULL);

    // fix for 5088782
    // check if CreateWindowsEx() returns not null value and if it does -
    //   create an InternalError or OutOfMemoryError based on GetLastError().
    //   This error is set to createError field of WObjectPeer and then
    //   checked and thrown in WComponentPeer constructor. We can't throw an
    //   error here because this code is invoked on Toolkit thread
    if (hwnd == NULL)
    {
        DWORD dw = ::GetLastError();
        jobject createError = NULL;
        if (dw == ERROR_OUTOFMEMORY)
        {
            jstring errorMsg = env->NewStringUTF("too many window handles");
            createError = JNU_NewObjectByName(env, "java/lang/OutOfMemoryError",
                                                      "(Ljava/lang/String;)V",
                                                      errorMsg);
            env->DeleteLocalRef(errorMsg);
        }
        else
        {
            TCHAR *buf;
            FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
                NULL, dw, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                (LPTSTR)&buf, 0, NULL);
            jstring s = JNU_NewStringPlatform(env, buf);
            createError = JNU_NewObjectByName(env, "java/lang/InternalError",
                                                  "(Ljava/lang/String;)V", s);
            LocalFree(buf);
            env->DeleteLocalRef(s);
        }
        env->SetObjectField(peer, AwtObject::createErrorID, createError);
        if (createError != NULL)
        {
            env->DeleteLocalRef(createError);
        }
        env->DeleteLocalRef(target);
        return;
    }

    m_hwnd = hwnd;

    ImmAssociateContext(NULL);

    SetDrawState((jint)JAWT_LOCK_SURFACE_CHANGED |
        (jint)JAWT_LOCK_BOUNDS_CHANGED |
        (jint)JAWT_LOCK_CLIP_CHANGED);

    LinkObjects(env, peer);

    /* Subclass the window now so that we can snoop on its messages */
    SubclassHWND();

    /*
      * Fix for 4046446.
      */
    SetWindowPos(GetHWnd(), 0, x, y, w, h, SWP_NOZORDER | SWP_NOCOPYBITS | SWP_NOACTIVATE);

    /* Set default colors. */
    m_colorForeground = colorForeground;
    m_colorBackground = colorBackground;

    /*
     * Only set background color if the color is actually set on the
     * target -- this avoids inheriting a parent's color unnecessarily,
     * and has to be done here because there isn't an API to get the
     * real background color from outside the AWT package.
     */
    jobject bkgrd = env->GetObjectField(target, AwtComponent::backgroundID) ;
    if (bkgrd != NULL) {
        JNU_CallMethodByName(env, NULL, peer, "setBackground",
                             "(Ljava/awt/Color;)V", bkgrd);
        DASSERT(!safe_ExceptionOccurred(env));
    }
    env->DeleteLocalRef(target);
    env->DeleteLocalRef(bkgrd);
}

/*
 * Returns hwnd for target on non Toolkit thread
 */
HWND
AwtComponent::GetHWnd(JNIEnv* env, jobject target) {
    if (JNU_IsNull(env, target)) {
        return 0;
    }
    jobject peer = env->GetObjectField(target, AwtComponent::peerID);
    if (JNU_IsNull(env, peer)) {
        return 0;
    }
    HWND hwnd = reinterpret_cast<HWND>(static_cast<LONG_PTR> (
        env->GetLongField(peer, AwtComponent::hwndID)));
    env->DeleteLocalRef(peer);
    return hwnd;
}
//
// Propagate the background color to synchronize Java field and peer's field.
// This is needed to fix 4148334
//
void AwtComponent::UpdateBackground(JNIEnv *env, jobject target)
{
    if (env->EnsureLocalCapacity(1) < 0) {
        return;
    }

    jobject bkgrnd = env->GetObjectField(target, AwtComponent::backgroundID);

    if (bkgrnd == NULL) {
        bkgrnd = JNU_NewObjectByName(env, "java/awt/Color", "(III)V",
                                     GetRValue(m_colorBackground),
                                     GetGValue(m_colorBackground),
                                     GetBValue(m_colorBackground));
        if (bkgrnd != NULL) {
            env->SetObjectField(target, AwtComponent::backgroundID, bkgrnd);
        }
    }
    env->DeleteLocalRef(bkgrnd);
}

/*
 * Install our window proc as the proc for our HWND, and save off the
 * previous proc as the default
 */
void AwtComponent::SubclassHWND()
{
    if (m_bSubclassed) {
        return;
    }
    const WNDPROC wndproc = WndProc; // let compiler type check WndProc
    m_DefWindowProc = ComCtl32Util::GetInstance().SubclassHWND(GetHWnd(), wndproc);
    m_bSubclassed = TRUE;
}

/*
 * Reinstall the original window proc as the proc for our HWND
 */
void AwtComponent::UnsubclassHWND()
{
    if (!m_bSubclassed) {
        return;
    }
    ComCtl32Util::GetInstance().UnsubclassHWND(GetHWnd(), WndProc, m_DefWindowProc);
    m_bSubclassed = FALSE;
}

/////////////////////////////////////
// (static method)
// Determines the top-level ancestor for a given window. If the given
// window is a top-level window, return itself.
//
// 'Top-level' includes dialogs as well.
//
HWND AwtComponent::GetTopLevelParentForWindow(HWND hwndDescendant) {
    if (hwndDescendant == NULL) {
        return NULL;
    }

    DASSERT(IsWindow(hwndDescendant));
    HWND hwnd = hwndDescendant;
    for(;;) {
        DWORD style = ::GetWindowLong(hwnd, GWL_STYLE);
        // a) found a non-child window so terminate
        // b) found real toplevel window (e.g. EmbeddedFrame
        //    that is child though)
        if ( (style & WS_CHILD) == 0 ||
             AwtComponent::IsTopLevelHWnd(hwnd) )
        {
            break;
        }
        hwnd = ::GetParent(hwnd);
    }

    return hwnd;
}
////////////////////

jobject AwtComponent::FindHeavyweightUnderCursor(BOOL useCache) {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(1) < 0) {
        return NULL;
    }

    HWND hit = NULL;
    POINT p = { 0, 0 };
    AwtComponent *comp = NULL;

    if (useCache) {
        if (sm_cursorOn == NULL) {
            return NULL;
        }


        DASSERT(::IsWindow(sm_cursorOn));
        VERIFY(::GetCursorPos(&p));
        /*
         * Fix for BugTraq ID 4304024.
         * Allow a non-default cursor only for the client area.
         */
        comp = AwtComponent::GetComponent(sm_cursorOn);
        if (comp != NULL &&
            ::SendMessage(sm_cursorOn, WM_NCHITTEST, 0,
                          MAKELPARAM(p.x, p.y)) == HTCLIENT) {
            goto found;
        }
    }

    ::GetCursorPos(&p);
    hit = ::WindowFromPoint(p);
    while (hit != NULL) {
        comp = AwtComponent::GetComponent(hit);

        if (comp != NULL) {
            INT nHittest = (INT)::SendMessage(hit, WM_NCHITTEST,
                                          0, MAKELPARAM(p.x, p.y));
            /*
             * Fix for BugTraq ID 4304024.
             * Allow a non-default cursor only for the client area.
             */
            if (nHittest != HTCLIENT) {
                /*
                 * When over the non-client area, send WM_SETCURSOR
                 * to revert the cursor to an arrow.
                 */
                ::SendMessage(hit, WM_SETCURSOR, (WPARAM)hit,
                              MAKELPARAM(nHittest, WM_MOUSEMOVE));
                return NULL;
            } else {
              sm_cursorOn = hit;
              goto found;
            }
        }

        if ((::GetWindowLong(hit, GWL_STYLE) & WS_CHILD) == 0) {
            return NULL;
        }
        hit = ::GetParent(hit);
    }

    return NULL;

found:
    jobject localRef = comp->GetTarget(env);
    jobject globalRef = env->NewGlobalRef(localRef);
    env->DeleteLocalRef(localRef);
    return globalRef;
}

void AwtComponent::SetColor(COLORREF c)
{
    int screen = AwtWin32GraphicsDevice::DeviceIndexForWindow(GetHWnd());
    int grayscale = AwtWin32GraphicsDevice::GetGrayness(screen);
    if (grayscale != GS_NOTGRAY) {
        int g;

        g = (int) (.299 * (c & 0xFF) + .587 * ((c >> 8) & 0xFF) +
            .114 * ((c >> 16) & 0xFF) + 0.5);
        // c = g | (g << 8) | (g << 16);
        c = PALETTERGB(g, g, g);
    }

    if (m_colorForeground == c) {
        return;
    }

    m_colorForeground = c;
    if (m_penForeground != NULL) {
        m_penForeground->Release();
        m_penForeground = NULL;
    }
    VERIFY(::InvalidateRect(GetHWnd(), NULL, FALSE));
}

void AwtComponent::SetBackgroundColor(COLORREF c)
{
    int screen = AwtWin32GraphicsDevice::DeviceIndexForWindow(GetHWnd());
    int grayscale = AwtWin32GraphicsDevice::GetGrayness(screen);
    if (grayscale != GS_NOTGRAY) {
        int g;

        g = (int) (.299 * (c & 0xFF) + .587 * ((c >> 8) & 0xFF) +
            .114 * ((c >> 16) & 0xFF) + 0.5);
        // c = g | (g << 8) | (g << 16);
        c = PALETTERGB(g, g, g);
    }

    if (m_colorBackground == c) {
        return;
    }
    m_colorBackground = c;
    m_backgroundColorSet = TRUE;
    if (m_brushBackground != NULL) {
        m_brushBackground->Release();
        m_brushBackground = NULL;
    }
    VERIFY(::InvalidateRect(GetHWnd(), NULL, TRUE));
}

HPEN AwtComponent::GetForegroundPen()
{
    if (m_penForeground == NULL) {
        m_penForeground = AwtPen::Get(m_colorForeground);
    }
    return (HPEN)m_penForeground->GetHandle();
}

COLORREF AwtComponent::GetBackgroundColor()
{
    if (m_backgroundColorSet == FALSE) {
        AwtComponent* c = this;
        while ((c = c->GetParent()) != NULL) {
            if (c->IsBackgroundColorSet()) {
                return c->GetBackgroundColor();
            }
        }
    }
    return m_colorBackground;
}

HBRUSH AwtComponent::GetBackgroundBrush()
{
    if (m_backgroundColorSet == FALSE) {
        if (m_brushBackground != NULL) {
            m_brushBackground->Release();
            m_brushBackground = NULL;
        }
          AwtComponent* c = this;
          while ((c = c->GetParent()) != NULL) {
              if (c->IsBackgroundColorSet()) {
                  m_brushBackground =
                      AwtBrush::Get(c->GetBackgroundColor());
                  break;
              }
          }
    }
    if (m_brushBackground == NULL) {
        m_brushBackground = AwtBrush::Get(m_colorBackground);
    }
    return (HBRUSH)m_brushBackground->GetHandle();
}

void AwtComponent::SetFont(AwtFont* font)
{
    DASSERT(font != NULL);
    if (font->GetAscent() < 0) {
        AwtFont::SetupAscent(font);
    }
    SendMessage(WM_SETFONT, (WPARAM)font->GetHFont(), MAKELPARAM(FALSE, 0));
    VERIFY(::InvalidateRect(GetHWnd(), NULL, TRUE));
}

AwtComponent* AwtComponent::GetParent()
{
    HWND hwnd = ::GetParent(GetHWnd());
    if (hwnd == NULL) {
        return NULL;
    }
    return GetComponent(hwnd);
}

AwtWindow* AwtComponent::GetContainer()
{
    AwtComponent* comp = this;
    while (comp != NULL) {
        if (comp->IsContainer()) {
            return (AwtWindow*)comp;
        }
        comp = comp->GetParent();
    }
    return NULL;
}

void AwtComponent::Show()
{
    m_visible = true;
    ::ShowWindow(GetHWnd(), SW_SHOWNA);
}

void AwtComponent::Hide()
{
    m_visible = false;
    ::ShowWindow(GetHWnd(), SW_HIDE);
}

BOOL
AwtComponent::SetWindowPos(HWND wnd, HWND after,
                           int x, int y, int w, int h, UINT flags)
{
    // Conditions we shouldn't handle:
    // z-order changes, correct window dimensions
    if (after != NULL || (w < 32767 && h < 32767)
        || ((::GetWindowLong(wnd, GWL_STYLE) & WS_CHILD) == 0))
    {
        return ::SetWindowPos(wnd, after, x, y, w, h, flags);
    }
    WINDOWPLACEMENT wp;
    ::ZeroMemory(&wp, sizeof(wp));

    wp.length = sizeof(wp);
    ::GetWindowPlacement(wnd, &wp);
    wp.rcNormalPosition.left = x;
    wp.rcNormalPosition.top = y;
    wp.rcNormalPosition.right = x + w;
    wp.rcNormalPosition.bottom = y + h;
    if ( flags & SWP_NOACTIVATE ) {
        wp.showCmd = SW_SHOWNOACTIVATE;
    }
    ::SetWindowPlacement(wnd, &wp);
    return 1;
}


void AwtComponent::Reshape(int x, int y, int w, int h)
{
#if defined(DEBUG)
    RECT        rc;
    ::GetWindowRect(GetHWnd(), &rc);
    ::MapWindowPoints(HWND_DESKTOP, ::GetParent(GetHWnd()), (LPPOINT)&rc, 2);
    DTRACE_PRINTLN4("AwtComponent::Reshape from %d, %d, %d, %d", rc.left, rc.top, rc.right-rc.left, rc.bottom-rc.top);
#endif
    AwtWindow* container = GetContainer();
    AwtComponent* parent = GetParent();
    if (container != NULL && container == parent) {
        container->SubtractInsetPoint(x, y);
    }
    DTRACE_PRINTLN4("AwtComponent::Reshape to %d, %d, %d, %d", x, y, w, h);
    UINT flags = SWP_NOACTIVATE | SWP_NOZORDER;

    RECT        r;

    ::GetWindowRect(GetHWnd(), &r);
    // if the component size is changing , don't copy window bits
    if (r.right - r.left != w || r.bottom - r.top != h) {
        flags |= SWP_NOCOPYBITS;
    }

    if (parent && _tcscmp(parent->GetClassName(), TEXT("SunAwtScrollPane")) == 0) {
        if (x > 0) {
            x = 0;
        }
        if (y > 0) {
            y = 0;
        }
    }
    if (m_hdwp != NULL) {
        m_hdwp = ::DeferWindowPos(m_hdwp, GetHWnd(), 0, x, y, w, h, flags);
        DASSERT(m_hdwp != NULL);
    } else {
        /*
         * Fox for 4046446
         * If window has dimensions above the short int limit, ::SetWindowPos doesn't work.
         * We should use SetWindowPlacement instead.
         */
        SetWindowPos(GetHWnd(), 0, x, y, w, h, flags);
    }
}

void AwtComponent::SetScrollValues(UINT bar, int min, int value, int max)
{
    int minTmp, maxTmp;

    ::GetScrollRange(GetHWnd(), bar, &minTmp, &maxTmp);
    if (min == INT_MAX) {
        min = minTmp;
    }
    if (value == INT_MAX) {
        value = ::GetScrollPos(GetHWnd(), bar);
    }
    if (max == INT_MAX) {
        max = maxTmp;
    }
    if (min == max) {
        max++;
    }
    ::SetScrollRange(GetHWnd(), bar, min, max, FALSE);
    ::SetScrollPos(GetHWnd(), bar, value, TRUE);
}

/*
 * Save Global Reference of sun.awt.windows.WInputMethod object
 */
void AwtComponent::SetInputMethod(jobject im, BOOL useNativeCompWindow)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (m_InputMethod!=NULL)
        env->DeleteGlobalRef(m_InputMethod);

    if (im!=NULL){
        m_InputMethod = env->NewGlobalRef(im);
        m_useNativeCompWindow = useNativeCompWindow;
    } else {
        m_InputMethod = NULL;
        m_useNativeCompWindow = TRUE;
    }

}

/*
 * Opportunity to process and/or eat a message before it is dispatched
 */
MsgRouting AwtComponent::PreProcessMsg(MSG& msg)
{
    return mrPassAlong;
}

static UINT lastMessage = WM_NULL;

#ifndef SPY_MESSAGES
#define SpyWinMessage(hwin,msg,str)
#else

#define FMT_MSG(x,y) case x: _stprintf(szBuf, \
    "0x%8.8x(%s):%s\n", hwnd, szComment, y); break;
#define WIN_MSG(x) FMT_MSG(x,#x)

void SpyWinMessage(HWND hwnd, UINT message, LPCTSTR szComment) {

    TCHAR szBuf[256];

    switch (message) {
        WIN_MSG(WM_NULL)
        WIN_MSG(WM_CREATE)
        WIN_MSG(WM_DESTROY)
        WIN_MSG(WM_MOVE)
        WIN_MSG(WM_SIZE)
        WIN_MSG(WM_ACTIVATE)
        WIN_MSG(WM_SETFOCUS)
        WIN_MSG(WM_KILLFOCUS)
        WIN_MSG(WM_ENABLE)
        WIN_MSG(WM_SETREDRAW)
        WIN_MSG(WM_SETTEXT)
        WIN_MSG(WM_GETTEXT)
        WIN_MSG(WM_GETTEXTLENGTH)
        WIN_MSG(WM_PAINT)
        WIN_MSG(WM_CLOSE)
        WIN_MSG(WM_QUERYENDSESSION)
        WIN_MSG(WM_QUIT)
        WIN_MSG(WM_QUERYOPEN)
        WIN_MSG(WM_ERASEBKGND)
        WIN_MSG(WM_SYSCOLORCHANGE)
        WIN_MSG(WM_ENDSESSION)
        WIN_MSG(WM_SHOWWINDOW)
        FMT_MSG(WM_WININICHANGE,"WM_WININICHANGE/WM_SETTINGCHANGE")
        WIN_MSG(WM_DEVMODECHANGE)
        WIN_MSG(WM_ACTIVATEAPP)
        WIN_MSG(WM_FONTCHANGE)
        WIN_MSG(WM_TIMECHANGE)
        WIN_MSG(WM_CANCELMODE)
        WIN_MSG(WM_SETCURSOR)
        WIN_MSG(WM_MOUSEACTIVATE)
        WIN_MSG(WM_CHILDACTIVATE)
        WIN_MSG(WM_QUEUESYNC)
        WIN_MSG(WM_GETMINMAXINFO)
        WIN_MSG(WM_PAINTICON)
        WIN_MSG(WM_ICONERASEBKGND)
        WIN_MSG(WM_NEXTDLGCTL)
        WIN_MSG(WM_SPOOLERSTATUS)
        WIN_MSG(WM_DRAWITEM)
        WIN_MSG(WM_MEASUREITEM)
        WIN_MSG(WM_DELETEITEM)
        WIN_MSG(WM_VKEYTOITEM)
        WIN_MSG(WM_CHARTOITEM)
        WIN_MSG(WM_SETFONT)
        WIN_MSG(WM_GETFONT)
        WIN_MSG(WM_SETHOTKEY)
        WIN_MSG(WM_GETHOTKEY)
        WIN_MSG(WM_QUERYDRAGICON)
        WIN_MSG(WM_COMPAREITEM)
        FMT_MSG(0x003D, "WM_GETOBJECT")
        WIN_MSG(WM_COMPACTING)
        WIN_MSG(WM_COMMNOTIFY)
        WIN_MSG(WM_WINDOWPOSCHANGING)
        WIN_MSG(WM_WINDOWPOSCHANGED)
        WIN_MSG(WM_POWER)
        WIN_MSG(WM_COPYDATA)
        WIN_MSG(WM_CANCELJOURNAL)
        WIN_MSG(WM_NOTIFY)
        WIN_MSG(WM_INPUTLANGCHANGEREQUEST)
        WIN_MSG(WM_INPUTLANGCHANGE)
        WIN_MSG(WM_TCARD)
        WIN_MSG(WM_HELP)
        WIN_MSG(WM_USERCHANGED)
        WIN_MSG(WM_NOTIFYFORMAT)
        WIN_MSG(WM_CONTEXTMENU)
        WIN_MSG(WM_STYLECHANGING)
        WIN_MSG(WM_STYLECHANGED)
        WIN_MSG(WM_DISPLAYCHANGE)
        WIN_MSG(WM_GETICON)
        WIN_MSG(WM_SETICON)
        WIN_MSG(WM_NCCREATE)
        WIN_MSG(WM_NCDESTROY)
        WIN_MSG(WM_NCCALCSIZE)
        WIN_MSG(WM_NCHITTEST)
        WIN_MSG(WM_NCPAINT)
        WIN_MSG(WM_NCACTIVATE)
        WIN_MSG(WM_GETDLGCODE)
        WIN_MSG(WM_SYNCPAINT)
        WIN_MSG(WM_NCMOUSEMOVE)
        WIN_MSG(WM_NCLBUTTONDOWN)
        WIN_MSG(WM_NCLBUTTONUP)
        WIN_MSG(WM_NCLBUTTONDBLCLK)
        WIN_MSG(WM_NCRBUTTONDOWN)
        WIN_MSG(WM_NCRBUTTONUP)
        WIN_MSG(WM_NCRBUTTONDBLCLK)
        WIN_MSG(WM_NCMBUTTONDOWN)
        WIN_MSG(WM_NCMBUTTONUP)
        WIN_MSG(WM_NCMBUTTONDBLCLK)
        WIN_MSG(WM_KEYDOWN)
        WIN_MSG(WM_KEYUP)
        WIN_MSG(WM_CHAR)
        WIN_MSG(WM_DEADCHAR)
        WIN_MSG(WM_SYSKEYDOWN)
        WIN_MSG(WM_SYSKEYUP)
        WIN_MSG(WM_SYSCHAR)
        WIN_MSG(WM_SYSDEADCHAR)
        WIN_MSG(WM_IME_STARTCOMPOSITION)
        WIN_MSG(WM_IME_ENDCOMPOSITION)
        WIN_MSG(WM_IME_COMPOSITION)
        WIN_MSG(WM_INITDIALOG)
        WIN_MSG(WM_COMMAND)
        WIN_MSG(WM_SYSCOMMAND)
        WIN_MSG(WM_TIMER)
        WIN_MSG(WM_HSCROLL)
        WIN_MSG(WM_VSCROLL)
        WIN_MSG(WM_INITMENU)
        WIN_MSG(WM_INITMENUPOPUP)
        WIN_MSG(WM_MENUSELECT)
        WIN_MSG(WM_MENUCHAR)
        WIN_MSG(WM_ENTERIDLE)
        FMT_MSG(0x0122, "WM_MENURBUTTONUP")
        FMT_MSG(0x0123, "WM_MENUDRAG")
        FMT_MSG(0x0124, "WM_MENUGETOBJECT")
        FMT_MSG(0x0125, "WM_UNINITMENUPOPUP")
        FMT_MSG(0x0126, "WM_MENUCOMMAND")
        WIN_MSG(WM_CTLCOLORMSGBOX)
        WIN_MSG(WM_CTLCOLOREDIT)
        WIN_MSG(WM_CTLCOLORLISTBOX)
        WIN_MSG(WM_CTLCOLORBTN)
        WIN_MSG(WM_CTLCOLORDLG)
        WIN_MSG(WM_CTLCOLORSCROLLBAR)
        WIN_MSG(WM_CTLCOLORSTATIC)
        WIN_MSG(WM_MOUSEMOVE)
        WIN_MSG(WM_LBUTTONDOWN)
        WIN_MSG(WM_LBUTTONUP)
        WIN_MSG(WM_LBUTTONDBLCLK)
        WIN_MSG(WM_RBUTTONDOWN)
        WIN_MSG(WM_RBUTTONUP)
        WIN_MSG(WM_RBUTTONDBLCLK)
        WIN_MSG(WM_MBUTTONDOWN)
        WIN_MSG(WM_MBUTTONUP)
        WIN_MSG(WM_MBUTTONDBLCLK)
        WIN_MSG(WM_MOUSEWHEEL)
        WIN_MSG(WM_PARENTNOTIFY)
        WIN_MSG(WM_ENTERMENULOOP)
        WIN_MSG(WM_EXITMENULOOP)
        WIN_MSG(WM_NEXTMENU)
        WIN_MSG(WM_SIZING)
        WIN_MSG(WM_CAPTURECHANGED)
        WIN_MSG(WM_MOVING)
        WIN_MSG(WM_POWERBROADCAST)
        WIN_MSG(WM_DEVICECHANGE)
        WIN_MSG(WM_MDICREATE)
        WIN_MSG(WM_MDIDESTROY)
        WIN_MSG(WM_MDIACTIVATE)
        WIN_MSG(WM_MDIRESTORE)
        WIN_MSG(WM_MDINEXT)
        WIN_MSG(WM_MDIMAXIMIZE)
        WIN_MSG(WM_MDITILE)
        WIN_MSG(WM_MDICASCADE)
        WIN_MSG(WM_MDIICONARRANGE)
        WIN_MSG(WM_MDIGETACTIVE)
        WIN_MSG(WM_MDISETMENU)
        WIN_MSG(WM_ENTERSIZEMOVE)
        WIN_MSG(WM_EXITSIZEMOVE)
        WIN_MSG(WM_DROPFILES)
        WIN_MSG(WM_MDIREFRESHMENU)
        WIN_MSG(WM_IME_SETCONTEXT)
        WIN_MSG(WM_IME_NOTIFY)
        WIN_MSG(WM_IME_CONTROL)
        WIN_MSG(WM_IME_COMPOSITIONFULL)
        WIN_MSG(WM_IME_SELECT)
        WIN_MSG(WM_IME_CHAR)
        FMT_MSG(0x0288, "WM_IME_REQUEST")
        WIN_MSG(WM_IME_KEYDOWN)
        WIN_MSG(WM_IME_KEYUP)
        FMT_MSG(0x02A1, "WM_MOUSEHOVER")
        FMT_MSG(0x02A3, "WM_MOUSELEAVE")
        WIN_MSG(WM_CUT)
        WIN_MSG(WM_COPY)
        WIN_MSG(WM_PASTE)
        WIN_MSG(WM_CLEAR)
        WIN_MSG(WM_UNDO)
        WIN_MSG(WM_RENDERFORMAT)
        WIN_MSG(WM_RENDERALLFORMATS)
        WIN_MSG(WM_DESTROYCLIPBOARD)
        WIN_MSG(WM_DRAWCLIPBOARD)
        WIN_MSG(WM_PAINTCLIPBOARD)
        WIN_MSG(WM_VSCROLLCLIPBOARD)
        WIN_MSG(WM_SIZECLIPBOARD)
        WIN_MSG(WM_ASKCBFORMATNAME)
        WIN_MSG(WM_CHANGECBCHAIN)
        WIN_MSG(WM_HSCROLLCLIPBOARD)
        WIN_MSG(WM_QUERYNEWPALETTE)
        WIN_MSG(WM_PALETTEISCHANGING)
        WIN_MSG(WM_PALETTECHANGED)
        WIN_MSG(WM_HOTKEY)
        WIN_MSG(WM_PRINT)
        WIN_MSG(WM_PRINTCLIENT)
        WIN_MSG(WM_HANDHELDFIRST)
        WIN_MSG(WM_HANDHELDLAST)
        WIN_MSG(WM_AFXFIRST)
        WIN_MSG(WM_AFXLAST)
        WIN_MSG(WM_PENWINFIRST)
        WIN_MSG(WM_PENWINLAST)
        WIN_MSG(WM_AWT_COMPONENT_CREATE)
        WIN_MSG(WM_AWT_DESTROY_WINDOW)
        WIN_MSG(WM_AWT_MOUSEENTER)
        WIN_MSG(WM_AWT_MOUSEEXIT)
        WIN_MSG(WM_AWT_COMPONENT_SHOW)
        WIN_MSG(WM_AWT_COMPONENT_HIDE)
        WIN_MSG(WM_AWT_COMPONENT_SETFOCUS)
        WIN_MSG(WM_AWT_LIST_SETMULTISELECT)
        WIN_MSG(WM_AWT_HANDLE_EVENT)
        WIN_MSG(WM_AWT_PRINT_COMPONENT)
        WIN_MSG(WM_AWT_RESHAPE_COMPONENT)
        WIN_MSG(WM_AWT_SETALWAYSONTOP)
        WIN_MSG(WM_AWT_BEGIN_VALIDATE)
        WIN_MSG(WM_AWT_END_VALIDATE)
        WIN_MSG(WM_AWT_FORWARD_CHAR)
        WIN_MSG(WM_AWT_FORWARD_BYTE)
        WIN_MSG(WM_AWT_SET_SCROLL_INFO)
        WIN_MSG(WM_AWT_CREATECONTEXT)
        WIN_MSG(WM_AWT_DESTROYCONTEXT)
        WIN_MSG(WM_AWT_ASSOCIATECONTEXT)
        WIN_MSG(WM_AWT_PRE_KEYDOWN)
        WIN_MSG(WM_AWT_PRE_KEYUP)
        WIN_MSG(WM_AWT_PRE_SYSKEYDOWN)
        WIN_MSG(WM_AWT_PRE_SYSKEYUP)
        WIN_MSG(WM_AWT_ENDCOMPOSITION,)
        WIN_MSG(WM_AWT_DISPOSE,)
        WIN_MSG(WM_AWT_DELETEOBJECT,)
        WIN_MSG(WM_AWT_SETCONVERSIONSTATUS,)
        WIN_MSG(WM_AWT_GETCONVERSIONSTATUS,)
        WIN_MSG(WM_AWT_SETOPENSTATUS,)
        WIN_MSG(WM_AWT_GETOPENSTATUS)
        WIN_MSG(WM_AWT_ACTIVATEKEYBOARDLAYOUT)
        WIN_MSG(WM_AWT_OPENCANDIDATEWINDOW)
        WIN_MSG(WM_AWT_DLG_SHOWMODAL,)
        WIN_MSG(WM_AWT_DLG_ENDMODAL,)
        WIN_MSG(WM_AWT_SETCURSOR,)
        WIN_MSG(WM_AWT_WAIT_FOR_SINGLE_OBJECT,)
        WIN_MSG(WM_AWT_INVOKE_METHOD,)
        WIN_MSG(WM_AWT_INVOKE_VOID_METHOD,)
        WIN_MSG(WM_AWT_EXECUTE_SYNC,)
        WIN_MSG(WM_AWT_CURSOR_SYNC)
        WIN_MSG(WM_AWT_GETDC)
        WIN_MSG(WM_AWT_RELEASEDC)
        WIN_MSG(WM_AWT_RELEASE_ALL_DCS)
        WIN_MSG(WM_AWT_SHOWCURSOR)
        WIN_MSG(WM_AWT_HIDECURSOR)
        WIN_MSG(WM_AWT_CREATE_PRINTED_PIXELS)
        WIN_MSG(WM_AWT_OBJECTLISTCLEANUP)
        default:
            sprintf(szBuf, "0x%8.8x(%s):Unknown message 0x%8.8x\n",
                hwnd, szComment, message);
            break;
    }
    printf(szBuf);
}

#endif /* SPY_MESSAGES */

/*
 * Dispatch messages for this window class--general component
 */
LRESULT AwtComponent::WindowProc(UINT message, WPARAM wParam, LPARAM lParam)
{
    CounterHelper ch(&m_MessagesProcessing);

    JNILocalFrame lframe(AwtToolkit::GetEnv(), 10);
    SpyWinMessage(GetHWnd(), message,
        (message == WM_AWT_RELEASE_ALL_DCS) ? TEXT("Disposed Component") : GetClassName());

    LRESULT retValue = 0;
    MsgRouting mr = mrDoDefault;
    AwtToolkit::GetInstance().eventNumber++;

    static BOOL ignoreNextLBTNUP = FALSE; //Ignore next LBUTTONUP msg?

    lastMessage = message;

    if (message == WmAwtIsComponent) {
    // special message to identify AWT HWND's without using
    // resource hogging ::SetProp
        return (LRESULT)TRUE;
    }

    UINT switchMessage;
    if (IS_WIN95 && !IS_WIN98 && message == Wheel95GetMsg()) {
        // Wheel message is generated dynamically on 95.  A quick swap and
        // we're good to go.
        DTRACE_PRINTLN1("got wheel event on 95.  msg is %i\n", message);
        switchMessage = WM_MOUSEWHEEL;
    }
    else {
        switchMessage = message;
    }

    switch (switchMessage) {
      case WM_AWT_GETDC:
      {
            HDC hDC;
            // First, release the DCs scheduled for deletion
            ReleaseDCList(GetHWnd(), passiveDCList);

            GetDCReturnStruct *returnStruct = new GetDCReturnStruct;
            returnStruct->gdiLimitReached = FALSE;
            if (AwtGDIObject::IncrementIfAvailable()) {
                hDC = ::GetDCEx(GetHWnd(), NULL,
                                DCX_CACHE | DCX_CLIPCHILDREN |
                                DCX_CLIPSIBLINGS);
                if (hDC != NULL) {
                    // Add new DC to list of DC's associated with this Component
                    activeDCList.AddDC(hDC, GetHWnd());
                } else {
                    // Creation failed; decrement counter in AwtGDIObject
                    AwtGDIObject::Decrement();
                }
            } else {
                hDC = NULL;
                returnStruct->gdiLimitReached = TRUE;
            }
            returnStruct->hDC = hDC;
            retValue = (LRESULT)returnStruct;
            mr = mrConsume;
            break;
      }
      case WM_AWT_RELEASEDC:
      {
            HDC hDC = (HDC)wParam;
            MoveDCToPassiveList(hDC);
            ReleaseDCList(GetHWnd(), passiveDCList);
            mr = mrConsume;
            break;
      }
      case WM_AWT_RELEASE_ALL_DCS:
      {
            // Called during Component destruction.  Gets current list of
            // DC's associated with Component and releases each DC.
            ReleaseDCList(GetHWnd(), activeDCList);
            ReleaseDCList(GetHWnd(), passiveDCList);
            mr = mrConsume;
            break;
      }
      case WM_AWT_SHOWCURSOR:
          ::ShowCursor(TRUE);
          break;
      case WM_AWT_HIDECURSOR:
          ::ShowCursor(FALSE);
          break;
      case WM_CREATE: mr = WmCreate(); break;
      case WM_CLOSE:      mr = WmClose(); break;
      case WM_DESTROY:    mr = WmDestroy(); break;

      case WM_ERASEBKGND:
          mr = WmEraseBkgnd((HDC)wParam, *(BOOL*)&retValue); break;
      case WM_PAINT:
          CheckFontSmoothingSettings(GetHWnd());
          /* Set draw state */
          SetDrawState(GetDrawState() | JAWT_LOCK_CLIP_CHANGED);
          mr = WmPaint((HDC)wParam);
          break;

      case WM_GETMINMAXINFO:
          mr = WmGetMinMaxInfo((LPMINMAXINFO)lParam);
          break;

      case WM_WINDOWPOSCHANGING:
      {
          // We process this message so that we can synchronize access to
          // a moving window.  The Scale/Blt functions in Win32BlitLoops
          // take the same windowMoveLock to ensure that a window is not
          // moving while we are trying to copy pixels into it.
          WINDOWPOS *lpPosInfo = (WINDOWPOS *)lParam;
          if ((lpPosInfo->flags & (SWP_NOMOVE | SWP_NOSIZE)) !=
              (SWP_NOMOVE | SWP_NOSIZE))
          {
              // Move or Size command.
              // Windows tends to send erroneous events that the window
              // is about to move when the coordinates are exactly the
              // same as the last time.  This can cause problems with
              // our windowMoveLock CriticalSection because we enter it
              // here and never get to WM_WINDOWPOSCHANGED to release it.
              // So make sure this is a real move/size event before bothering
              // to grab the critical section.
              BOOL takeLock = FALSE;
              if (!(lpPosInfo->flags & SWP_NOMOVE) &&
                  ((windowMoveLockPosX != lpPosInfo->x) ||
                   (windowMoveLockPosY != lpPosInfo->y)))
              {
                  // Real move event
                  takeLock = TRUE;
                  windowMoveLockPosX = lpPosInfo->x;
                  windowMoveLockPosY = lpPosInfo->y;
              }
              if (!(lpPosInfo->flags & SWP_NOSIZE) &&
                  ((windowMoveLockPosCX != lpPosInfo->cx) ||
                   (windowMoveLockPosCY != lpPosInfo->cy)))
              {
                  // Real size event
                  takeLock = TRUE;
                  windowMoveLockPosCX = lpPosInfo->cx;
                  windowMoveLockPosCY = lpPosInfo->cy;
              }
              if (takeLock) {
                  if (!windowMoveLockHeld) {
                      windowMoveLock.Enter();
                      windowMoveLockHeld = TRUE;
                  }
              }
          }
          mr = WmWindowPosChanging(lParam);
          break;
      }
      case WM_WINDOWPOSCHANGED:
      {
          // Release lock grabbed in the POSCHANGING message
          if (windowMoveLockHeld) {
              windowMoveLockHeld = FALSE;
              windowMoveLock.Leave();
          }
          mr = WmWindowPosChanged(lParam);
          break;
      }
      case WM_MOVE: {
          RECT r;
          ::GetWindowRect(GetHWnd(), &r);
          mr = WmMove(r.left, r.top);
          break;
      }
      case WM_SIZE:
      {
          RECT r;
          // fix 4128317 : use GetClientRect for full 32-bit int precision and
          // to avoid negative client area dimensions overflowing 16-bit params - robi
          ::GetClientRect( GetHWnd(), &r );
          mr = WmSize(static_cast<UINT>(wParam), r.right - r.left, r.bottom - r.top);
          //mr = WmSize(wParam, LOWORD(lParam), HIWORD(lParam));
          if (ImmGetContext() != NULL) {
              SetCompositionWindow(r);
          }
          break;
      }
      case WM_SIZING:
          mr = WmSizing();
          break;
      case WM_SHOWWINDOW:
          mr = WmShowWindow(static_cast<BOOL>(wParam),
                            static_cast<UINT>(lParam)); break;
      case WM_SYSCOMMAND:
          mr = WmSysCommand(static_cast<UINT>(wParam & 0xFFF0),
                            GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam));
          break;
      case WM_EXITSIZEMOVE:
          mr = WmExitSizeMove();
          break;
      // Bug #4039858 (Selecting menu item causes bogus mouse click event)
      case WM_ENTERMENULOOP:
          mr = WmEnterMenuLoop((BOOL)wParam);
          sm_bMenuLoop = TRUE;
          // we need to release grab if menu is shown
          if (AwtWindow::GetGrabbedWindow() != NULL) {
              AwtWindow::GetGrabbedWindow()->Ungrab();
          }
          break;
      case WM_EXITMENULOOP:
          mr = WmExitMenuLoop((BOOL)wParam);
          sm_bMenuLoop = FALSE;
          break;

      case WM_SETFOCUS:
          mr = (!sm_suppressFocusAndActivation && !m_skipNextSetFocus)
              ? WmSetFocus((HWND)wParam) : mrConsume;
          m_skipNextSetFocus = FALSE;
          break;
      case WM_KILLFOCUS:
          mr = (!sm_suppressFocusAndActivation)
              ? WmKillFocus((HWND)wParam) : mrConsume;
          break;
      case WM_ACTIVATE:
      {
          UINT nState = LOWORD(wParam);
          BOOL fMinimized = (BOOL)HIWORD(wParam);
          if (!sm_suppressFocusAndActivation &&
              (!fMinimized || (nState == WA_INACTIVE)))
          {
              mr = WmActivate(nState, fMinimized, (HWND)lParam);
              m_skipNextSetFocus = FALSE;
              // When the window is deactivated, send WM_IME_ENDCOMPOSITION
              // message to deactivate the composition window so that
              // it won't receive keyboard input focus.
              if (ImmGetContext() != NULL) {
                  DefWindowProc(WM_IME_ENDCOMPOSITION, 0, 0);
              }
          } else {
              if (!sm_suppressFocusAndActivation
                  && fMinimized && (nState != WA_INACTIVE))
              {
                  m_skipNextSetFocus = TRUE;
              }
              mr = mrConsume;
          }
      }
      break;
    case WM_MOUSEACTIVATE: {
        AwtWindow * window = (AwtWindow*)GetComponent((HWND)wParam);
        if (window != NULL) {
            if (!window->IsFocusableWindow()) {
                // if it is non-focusable window we can return
                // MA_NOACTIVATExxx and it will not be activated. We
                // return NOACTIVATE for a client part of the window so we
                // receive mouse event responsible for activation. We
                // return NOACTIVEA for Frame's non-client so user be able
                // to resize and move frames by title and borders. We
                // return NOACTIVATEANDEAT for Window non-client area as
                // there is noone to listen for this event.
                mr = mrConsume;
                if ((window == this) && LOWORD(lParam) != HTCLIENT ) {
                    if (window->IsSimpleWindow()) {
                        retValue = MA_NOACTIVATEANDEAT;
                    } else {
                        retValue = MA_NOACTIVATE;
                    }
                } else {
                    retValue = MA_NOACTIVATE;
                }
            }
        }
        break;
    }

      case WM_CTLCOLORMSGBOX:
      case WM_CTLCOLOREDIT:
      case WM_CTLCOLORLISTBOX:
      case WM_CTLCOLORBTN:
      case WM_CTLCOLORDLG:
      case WM_CTLCOLORSCROLLBAR:
      case WM_CTLCOLORSTATIC:
          mr = WmCtlColor((HDC)wParam, (HWND)lParam,
                          message-WM_CTLCOLORMSGBOX+CTLCOLOR_MSGBOX,
                          *(HBRUSH*)&retValue);
          break;
      case WM_HSCROLL:
          mr = WmHScroll(LOWORD(wParam), HIWORD(wParam), (HWND)lParam);
          break;
      case WM_VSCROLL:
          mr = WmVScroll(LOWORD(wParam), HIWORD(wParam), (HWND)lParam);
          break;
      // 4664415: We're seeing a WM_LBUTTONUP when the user releases the
      // mouse button after a WM_NCLBUTTONDBLCLK.  We want to ignore this
      // WM_LBUTTONUP, so we set a flag in WM_NCLBUTTONDBLCLK and look for the
      // flag on a WM_LBUTTONUP.  -bchristi
      case WM_NCLBUTTONDBLCLK:
          mr = WmNcMouseDown(wParam, GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam), LEFT_BUTTON | DBL_CLICK);
          if (mr == mrDoDefault) {
              ignoreNextLBTNUP = TRUE;
          }
          break;
      case WM_NCLBUTTONDOWN:
          mr = WmNcMouseDown(wParam, GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam), LEFT_BUTTON);
          ignoreNextLBTNUP = FALSE;
          break;
      case WM_NCLBUTTONUP:
          mr = WmNcMouseUp(wParam, GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam), LEFT_BUTTON);
          break;
      case WM_NCRBUTTONDOWN:
           mr = WmNcMouseDown(wParam, GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam), RIGHT_BUTTON);
           break;
      case WM_LBUTTONUP:
          if (ignoreNextLBTNUP) {
              ignoreNextLBTNUP = FALSE;
              return mrDoDefault;
          }
          //fall-through
      case WM_LBUTTONDOWN:
          ignoreNextLBTNUP = FALSE;
          //fall-through
      case WM_LBUTTONDBLCLK:
      case WM_RBUTTONDOWN:
      case WM_RBUTTONDBLCLK:
      case WM_RBUTTONUP:
      case WM_MBUTTONDOWN:
      case WM_MBUTTONDBLCLK:
      case WM_MBUTTONUP:
      case WM_MOUSEMOVE:
      case WM_MOUSEWHEEL:
      case WM_AWT_MOUSEENTER:
      case WM_AWT_MOUSEEXIT:
      {
          DWORD curPos = ::GetMessagePos();
          POINT myPos;
          myPos.x = GET_X_LPARAM(curPos);
          myPos.y = GET_Y_LPARAM(curPos);
          ::ScreenToClient(GetHWnd(), &myPos);
          switch(switchMessage) {
          case WM_AWT_MOUSEENTER:
              mr = WmMouseEnter(static_cast<UINT>(wParam), myPos.x, myPos.y); break;
          case WM_LBUTTONDOWN:
          case WM_LBUTTONDBLCLK:
                mr = WmMouseDown(static_cast<UINT>(wParam), myPos.x, myPos.y,
                           LEFT_BUTTON); break;
            case WM_LBUTTONUP:
                mr = WmMouseUp(static_cast<UINT>(wParam), myPos.x, myPos.y,
                               LEFT_BUTTON); break;
            case WM_MOUSEMOVE:
                mr = WmMouseMove(static_cast<UINT>(wParam), myPos.x, myPos.y); break;
      case WM_MBUTTONDOWN:
      case WM_MBUTTONDBLCLK:
                mr = WmMouseDown(static_cast<UINT>(wParam), myPos.x, myPos.y,
                           MIDDLE_BUTTON); break;
      case WM_RBUTTONDOWN:
      case WM_RBUTTONDBLCLK:
                mr = WmMouseDown(static_cast<UINT>(wParam), myPos.x, myPos.y,
                           RIGHT_BUTTON); break;
      case WM_RBUTTONUP:
                mr = WmMouseUp(static_cast<UINT>(wParam), myPos.x, myPos.y,
                         RIGHT_BUTTON);
          break;
      case WM_MBUTTONUP:
                mr = WmMouseUp(static_cast<UINT>(wParam), myPos.x, myPos.y,
                         MIDDLE_BUTTON);
          break;
      case WM_AWT_MOUSEEXIT:
                mr = WmMouseExit(static_cast<UINT>(wParam), myPos.x, myPos.y);
          break;
      case  WM_MOUSEWHEEL:
          if (IS_WIN95 && !IS_WIN98) {
              // On 95, the wParam doesn't contain the keystate flags, just
              // the wheel rotation.  The keystates are fetched in WmMouseWheel
              // using GetJavaModifiers().
              mr = WmMouseWheel(0,
                                GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam),
                                (int)wParam);
              return FALSE;
          }
          else {
              mr = WmMouseWheel(GET_KEYSTATE_WPARAM(wParam),
                                GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam),
                                GET_WHEEL_DELTA_WPARAM(wParam));
          }
          break;
          }
      }
          break;

      case WM_SETCURSOR:
          mr = mrDoDefault;
          if (LOWORD(lParam) == HTCLIENT) {
              if (AwtComponent* comp =
                                    AwtComponent::GetComponent((HWND)wParam)) {
                  AwtCursor::UpdateCursor(comp);
                  mr = mrConsume;
              }
          }
          break;

      case WM_KEYDOWN:
          mr = WmKeyDown(static_cast<UINT>(wParam),
                         LOWORD(lParam), HIWORD(lParam), FALSE);
          break;
      case WM_KEYUP:
          mr = WmKeyUp(static_cast<UINT>(wParam),
                       LOWORD(lParam), HIWORD(lParam), FALSE);
          break;
      case WM_SYSKEYDOWN:
          mr = WmKeyDown(static_cast<UINT>(wParam),
                         LOWORD(lParam), HIWORD(lParam), TRUE);
          break;
      case WM_SYSKEYUP:
          mr = WmKeyUp(static_cast<UINT>(wParam),
                       LOWORD(lParam), HIWORD(lParam), TRUE);
          break;
      case WM_IME_SETCONTEXT:
          // lParam is passed as pointer and it can be modified.
          mr = WmImeSetContext(static_cast<BOOL>(wParam), &lParam);
          CallProxyDefWindowProc(message, wParam, lParam, retValue, mr);
          break;
      case WM_IME_NOTIFY:
          mr = WmImeNotify(wParam, lParam);
          CallProxyDefWindowProc(message, wParam, lParam, retValue, mr);
          break;
      case WM_IME_STARTCOMPOSITION:
          mr = WmImeStartComposition();
          CallProxyDefWindowProc(message, wParam, lParam, retValue, mr);
          break;
      case WM_IME_ENDCOMPOSITION:
          mr = WmImeEndComposition();
          CallProxyDefWindowProc(message, wParam, lParam, retValue, mr);
          break;
      case WM_IME_COMPOSITION: {
          WORD dbcschar = static_cast<WORD>(wParam);
          mr = WmImeComposition(dbcschar, lParam);
          CallProxyDefWindowProc(message, wParam, lParam, retValue, mr);
          break;
      }
      case WM_IME_CONTROL:
      case WM_IME_COMPOSITIONFULL:
      case WM_IME_SELECT:
      case WM_IME_KEYUP:
      case WM_IME_KEYDOWN:
      case 0x0288: // WM_IME_REQUEST
          CallProxyDefWindowProc(message, wParam, lParam, retValue, mr);
          break;
      case WM_CHAR:
          mr = WmChar(static_cast<UINT>(wParam),
                      LOWORD(lParam), HIWORD(lParam), FALSE);
          break;
      case WM_SYSCHAR:
          mr = WmChar(static_cast<UINT>(wParam),
                      LOWORD(lParam), HIWORD(lParam), TRUE);
          break;
      case WM_IME_CHAR:
          mr = WmIMEChar(static_cast<UINT>(wParam),
                         LOWORD(lParam), HIWORD(lParam), FALSE);
          break;

      case WM_INPUTLANGCHANGEREQUEST: {
          DTRACE_PRINTLN4("WM_INPUTLANGCHANGEREQUEST: hwnd = 0x%X (%s);"//
                          "0x%08X -> 0x%08X",
                          GetHWnd(), GetClassName(),
                          (UINT_PTR)GetKeyboardLayout(), (UINT_PTR)lParam);
          // 4267428: make sure keyboard layout is turned undead.
          static BYTE keyboardState[AwtToolkit::KB_STATE_SIZE];
          AwtToolkit::GetKeyboardState(keyboardState);
          WORD ignored;
          ::ToAsciiEx(VK_SPACE, ::MapVirtualKey(VK_SPACE, 0),
                      keyboardState, &ignored, 0, GetKeyboardLayout());

          // Set this flag to block ActivateKeyboardLayout from
          // WInputMethod.activate()
          g_bUserHasChangedInputLang = TRUE;
          CallProxyDefWindowProc(message, wParam, lParam, retValue, mr);
          break;
      }
      case WM_INPUTLANGCHANGE:
          DTRACE_PRINTLN3("WM_INPUTLANGCHANGE: hwnd = 0x%X (%s);"//
                          "new = 0x%08X",
                          GetHWnd(), GetClassName(), (UINT)lParam);
          mr = WmInputLangChange(static_cast<UINT>(wParam), reinterpret_cast<HKL>(lParam));
          CallProxyDefWindowProc(message, wParam, lParam, retValue, mr);
          // should return non-zero if we process this message
          retValue = 1;
          break;

      case WM_AWT_FORWARD_CHAR:
          mr = WmForwardChar(LOWORD(wParam), lParam, HIWORD(wParam));
          break;

      case WM_AWT_FORWARD_BYTE:
          mr = HandleEvent( (MSG *) lParam, (BOOL) wParam);
          break;

      case WM_PASTE:
          mr = WmPaste();
          break;
      case WM_TIMER:
          mr = WmTimer(wParam);
          break;

      case WM_COMMAND:
          mr = WmCommand(LOWORD(wParam), (HWND)lParam, HIWORD(wParam));
          break;
      case WM_COMPAREITEM:
          mr = WmCompareItem(static_cast<UINT>(wParam),
                             *(COMPAREITEMSTRUCT*)lParam, retValue);
          break;
      case WM_DELETEITEM:
          mr = WmDeleteItem(static_cast<UINT>(wParam),
                            *(DELETEITEMSTRUCT*)lParam);
          break;
      case WM_DRAWITEM:
          mr = WmDrawItem(static_cast<UINT>(wParam),
                          *(DRAWITEMSTRUCT*)lParam);
          break;
      case WM_MEASUREITEM:
          mr = WmMeasureItem(static_cast<UINT>(wParam),
                             *(MEASUREITEMSTRUCT*)lParam);
          break;

      case WM_AWT_HANDLE_EVENT:
          mr = HandleEvent( (MSG *) lParam, (BOOL) wParam);
          break;

      case WM_PRINT:
          mr = WmPrint((HDC)wParam, lParam);
          break;
      case WM_PRINTCLIENT:
          mr = WmPrintClient((HDC)wParam, lParam);
          break;

      case WM_NCCALCSIZE:
          mr = WmNcCalcSize((BOOL)wParam, (LPNCCALCSIZE_PARAMS)lParam,
                            retValue);
          break;
      case WM_NCPAINT:
          mr = WmNcPaint((HRGN)wParam);
          break;
      case WM_NCHITTEST:
          mr = WmNcHitTest(LOWORD(lParam), HIWORD(lParam), retValue);
          break;

      case WM_AWT_RESHAPE_COMPONENT: {
          RECT* r = (RECT*)lParam;
          WPARAM checkEmbedded = wParam;
          if (checkEmbedded == CHECK_EMBEDDED && IsEmbeddedFrame()) {
              ::OffsetRect(r, -r->left, -r->top);
          }
          Reshape(r->left, r->top, r->right - r->left, r->bottom - r->top);
          delete r;
          mr = mrConsume;
          break;
      }

      case WM_AWT_SETALWAYSONTOP: {
        AwtWindow* w = (AwtWindow*)lParam;
        BOOL value = (BOOL)wParam;
        ::SetWindowPos(w->GetHWnd(), (value != 0 ? HWND_TOPMOST : HWND_NOTOPMOST),
                       0,0,0,0, SWP_NOMOVE|SWP_NOSIZE);
        break;
      }

      case WM_AWT_BEGIN_VALIDATE:
          BeginValidate();
          mr = mrConsume;
          break;
      case WM_AWT_END_VALIDATE:
          EndValidate();
          mr = mrConsume;
          break;

      case WM_PALETTEISCHANGING:
          mr = WmPaletteIsChanging((HWND)wParam);
          mr = mrDoDefault;
          break;
      case WM_QUERYNEWPALETTE:
          mr = WmQueryNewPalette(retValue);
          break;
      case WM_PALETTECHANGED:
          mr = WmPaletteChanged((HWND)wParam);
          break;
      case WM_STYLECHANGED:
          mr = WmStyleChanged(static_cast<int>(wParam), (LPSTYLESTRUCT)lParam);
          break;
      case WM_SETTINGCHANGE:
          CheckFontSmoothingSettings(NULL);
          mr = WmSettingChange(static_cast<UINT>(wParam), (LPCTSTR)lParam);
          break;
      case WM_CONTEXTMENU:
          mr = WmContextMenu((HWND)wParam,
                             GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam));
          break;

          /*
           * These messages are used to route Win32 calls to the
           * creating thread, since these calls fail unless executed
           * there.
           */
      case WM_AWT_COMPONENT_SHOW:
          Show();
          mr = mrConsume;
          break;
      case WM_AWT_COMPONENT_HIDE:
          Hide();
          mr = mrConsume;
          break;

      case WM_AWT_COMPONENT_SETFOCUS:
          retValue = (LRESULT)WmComponentSetFocus((WmComponentSetFocusData *)wParam);
          mr = mrConsume;
          break;

      case WM_AWT_SET_SCROLL_INFO: {
          SCROLLINFO *si = (SCROLLINFO *) lParam;
          ::SetScrollInfo(GetHWnd(), (int) wParam, si, TRUE);
          delete si;
          mr = mrConsume;
          break;
      }
      case WM_AWT_CREATE_PRINTED_PIXELS:
          retValue = (LRESULT)CreatePrintedPixels(*((SIZE *)wParam),
                                                  *((SIZE *)lParam));
          mr = mrConsume;
          break;
      case WM_AWT_DD_CREATE_SURFACE:
      {
          return (LRESULT)WmDDCreateSurface((Win32SDOps*)wParam);
      }
      case WM_AWT_DD_ENTER_FULLSCREEN:
      {
          mr = WmDDEnterFullScreen((HMONITOR)wParam);
          break;
      }
      case WM_AWT_DD_EXIT_FULLSCREEN:
      {
          mr = WmDDExitFullScreen((HMONITOR)wParam);
          break;
      }
      case WM_AWT_DD_SET_DISPLAY_MODE:
      {
          mr = WmDDSetDisplayMode((HMONITOR)wParam, (DDrawDisplayMode*)lParam);
          break;
      }
      case WM_UNDOCUMENTED_CLICKMENUBAR:
      {
          if (::IsWindow(AwtWindow::GetModalBlocker(GetHWnd()))) {
              mr = mrConsume;
          }
      }
    }

    /*
     * If not a specific Consume, it was a specific DoDefault, or a
     * PassAlong (since the default is the next in chain), then call the
     * default proc.
     */
    if (mr != mrConsume) {
        retValue = DefWindowProc(message, wParam, lParam);
    }

    return retValue;
}
/*
 * Call this instance's default window proc, or if none set, call the stock
 * Window's one.
 */
LRESULT AwtComponent::DefWindowProc(UINT msg, WPARAM wParam, LPARAM lParam)
{
    return ComCtl32Util::GetInstance().DefWindowProc(m_DefWindowProc, GetHWnd(), msg, wParam, lParam);
}

/*
 * This message should only be received when a window is destroyed by
 * Windows, and not Java.  Window termination has been reworked so
 * this method should never be called during termination.
 */
MsgRouting AwtComponent::WmDestroy()
{
    // fix for 6259348: we should enter the SyncCall critical section before
    // disposing the native object, that is value 1 of lParam is intended for
    AwtToolkit::GetInstance().SendMessage(WM_AWT_DISPOSE, (WPARAM)this, (LPARAM)1);

    return mrConsume;
}

MsgRouting AwtComponent::WmGetMinMaxInfo(LPMINMAXINFO lpmmi)
{
    return mrDoDefault;
}

MsgRouting AwtComponent::WmMove(int x, int y)
{
    SetDrawState(GetDrawState() | static_cast<jint>(JAWT_LOCK_BOUNDS_CHANGED)
        | static_cast<jint>(JAWT_LOCK_CLIP_CHANGED));
    return mrDoDefault;
}

MsgRouting AwtComponent::WmSize(UINT type, int w, int h)
{
    SetDrawState(GetDrawState() | static_cast<jint>(JAWT_LOCK_BOUNDS_CHANGED)
        | static_cast<jint>(JAWT_LOCK_CLIP_CHANGED));
    return mrDoDefault;
}

MsgRouting AwtComponent::WmSizing()
{
    return mrDoDefault;
}

MsgRouting AwtComponent::WmSysCommand(UINT uCmdType, int xPos, int yPos)
{
    return mrDoDefault;
}

MsgRouting AwtComponent::WmExitSizeMove()
{
    return mrDoDefault;
}

MsgRouting AwtComponent::WmEnterMenuLoop(BOOL isTrackPopupMenu)
{
    return mrDoDefault;
}

MsgRouting AwtComponent::WmExitMenuLoop(BOOL isTrackPopupMenu)
{
    return mrDoDefault;
}

MsgRouting AwtComponent::WmShowWindow(BOOL show, UINT status)
{
    // NULL-InputContext is associated to all window just after they created.
    // ( see CreateHWnd() )
    // But to TextField and TextArea on Win95, valid InputContext is associated
    // by system after that. This is not happen on NT4.0
    // For workaround, force context to NULL here.

    // Fix for 4730228
    // Check if we already have Java-associated input method
    HIMC context = 0;
    if (m_InputMethod != NULL) {
        // If so get the appropriate context from it and use it instead of empty context
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        context = (HIMC)(UINT_PTR)(JNU_GetFieldByName(env, NULL, m_InputMethod, "context", "I").i);
    }

    if (ImmGetContext() != 0 && ImmGetContext() != context) {
        ImmAssociateContext(context);
    }

    return mrDoDefault;
}

MsgRouting AwtComponent::WmSetFocus(HWND hWndLostFocus)
{
    if (sm_focusOwner == GetHWnd()) {
        sm_realFocusOpposite = NULL;
        return mrConsume;
    }

    HWND toplevelHWnd = AwtComponent::GetTopLevelParentForWindow(GetHWnd());
    AwtComponent *comp = AwtComponent::GetComponent(toplevelHWnd);

    if (comp && comp->IsEmbeddedFrame() &&
        !((AwtFrame*)comp)->activateEmbeddedFrameOnSetFocus(hWndLostFocus))
    {
        // Fix for 6562716.
        // In order that AwtSetFocus() returns FALSE.
        sm_suppressFocusAndActivation = TRUE;
        ::SetFocus(NULL);
        sm_suppressFocusAndActivation = FALSE;

        return mrConsume;
    }

    sm_focusOwner = GetHWnd();
    sm_focusedWindow = toplevelHWnd;

    if (sm_realFocusOpposite != NULL) {
        hWndLostFocus = sm_realFocusOpposite;
        sm_realFocusOpposite = NULL;
    }

    sm_wheelRotationAmount = 0;

    SendFocusEvent(java_awt_event_FocusEvent_FOCUS_GAINED, hWndLostFocus);

    return mrDoDefault;
}

MsgRouting AwtComponent::WmKillFocus(HWND hWndGotFocus)
{
    if (sm_focusOwner != NULL && sm_focusOwner == hWndGotFocus) {
        return mrConsume;
    }

    if (sm_focusOwner != GetHWnd()) {
        if (sm_focusOwner != NULL) {
            if (hWndGotFocus != NULL &&
                AwtComponent::GetComponent(hWndGotFocus) != NULL)
            {
                sm_realFocusOpposite = sm_focusOwner;
            }
            ::SendMessage(sm_focusOwner, WM_KILLFOCUS, (WPARAM)hWndGotFocus,
                          0);
        }
        return mrConsume;
    }

    AwtComponent *comp = AwtComponent::GetComponent(sm_focusedWindow);

    if (comp && comp->IsEmbeddedFrame()) {
        ((AwtFrame*)comp)->deactivateEmbeddedFrameOnKillFocus(hWndGotFocus);
    }

    sm_focusOwner = NULL;
    sm_wheelRotationAmount = 0;

    SendFocusEvent(java_awt_event_FocusEvent_FOCUS_LOST, hWndGotFocus);
    return mrDoDefault;
}

jboolean
AwtComponent::WmComponentSetFocus(WmComponentSetFocusData *data)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(1) < 0) {
        env->DeleteGlobalRef(data->lightweightChild);
        delete data;
        return JNI_FALSE;
    }

    jboolean result = JNI_FALSE;

    BOOL setSuppressFocusAndActivation = FALSE;

    /*
     * This is a fix for 4628933.
     * If sm_suppressFocusAndActivation is TRUE here then
     * this means that we dispatch WM_COMPONENT_SET_FOCUS inside
     * dispatching bounce activation, this unlikely but possible.
     * So we reset sm_suppressFocusAndActivation to give a chance
     * to dispatch focus events which will generate due this focus
     * request to Java.
     *
     * son@sparc.spb.su
     */
    if (sm_suppressFocusAndActivation) {
        sm_suppressFocusAndActivation = FALSE;
        setSuppressFocusAndActivation = TRUE;
    }

    jobject heavyweight = GetTarget(env);
    jint retval = env->CallStaticIntMethod
        (AwtKeyboardFocusManager::keyboardFocusManagerCls,
         AwtKeyboardFocusManager::shouldNativelyFocusHeavyweightMID,
         heavyweight, data->lightweightChild, data->temporary,
         data->focusedWindowChangeAllowed, data->time, data->cause);

    if (retval == java_awt_KeyboardFocusManager_SNFH_SUCCESS_HANDLED) {
        result = JNI_TRUE;
    } else if (retval == java_awt_KeyboardFocusManager_SNFH_SUCCESS_PROCEED) {
        result = (AwtSetFocus()) ? JNI_TRUE : JNI_FALSE;
        if (result == JNI_FALSE) {
            env->CallStaticVoidMethod
                (AwtKeyboardFocusManager::keyboardFocusManagerCls,
                 AwtKeyboardFocusManager::removeLastFocusRequestMID,
                 heavyweight);
        }
    } else {
        DASSERT(retval == java_awt_KeyboardFocusManager_SNFH_FAILURE);
        result = JNI_FALSE;
    }
    env->DeleteLocalRef(heavyweight);

    /*
     * Set sm_suppressFocusAndActivation back to TRUE if needed.
     * Fix for 4628933 (son@sparc.spb.su)
     */
   if (setSuppressFocusAndActivation) {
        sm_suppressFocusAndActivation = TRUE;
    }

    env->DeleteGlobalRef(data->lightweightChild);
    delete data;
    return result;
}

BOOL
AwtComponent::AwtSetFocus()
{
    HWND hwnd = GetHWnd();

    if (sm_focusOwner == hwnd) {
        return TRUE;
    }

    HWND fgWindow = ::GetForegroundWindow();
    if (NULL != fgWindow) {
        DWORD fgProcessID;
        ::GetWindowThreadProcessId(fgWindow, &fgProcessID);

        if (fgProcessID != ::GetCurrentProcessId()
            && !AwtToolkit::GetInstance().IsEmbedderProcessId(fgProcessID))
        {
            // fix for 6458497.  we shouldn't request focus if it is out of both
            // our and embedder process.
            return FALSE;
        }
    }

    AwtFrame *owner = GetContainer()->GetOwningFrameOrDialog();

    if (owner == NULL) {
        ::SetFocus(hwnd);
        if (::GetFocus() != hwnd) {
            return FALSE;
        }
    } else {
        HWND oldFocusOwner = sm_focusOwner;
        if (oldFocusOwner != NULL) {
            ::SendMessage(oldFocusOwner, WM_KILLFOCUS, (WPARAM)hwnd, 0);
        }

        sm_suppressFocusAndActivation = TRUE;
        ::SetActiveWindow(owner->GetHWnd());
        ::SetFocus(owner->GetProxyFocusOwner());
        sm_suppressFocusAndActivation = FALSE;

        sm_focusedWindow = GetTopLevelParentForWindow(GetHWnd());
        ::SendMessage(hwnd, WM_SETFOCUS, (WPARAM)oldFocusOwner, 0);
    }

    return TRUE;
}

MsgRouting AwtComponent::WmCtlColor(HDC hDC, HWND hCtrl,
                                    UINT ctlColor, HBRUSH& retBrush)
{
    AwtComponent* child = AwtComponent::GetComponent(hCtrl);
    if (child) {
        ::SetBkColor(hDC, child->GetBackgroundColor());
        ::SetTextColor(hDC, child->GetColor());
        retBrush = child->GetBackgroundBrush();
        return mrConsume;
    }
    return mrDoDefault;
/*
    switch (ctlColor) {
        case CTLCOLOR_MSGBOX:
        case CTLCOLOR_EDIT:
        case CTLCOLOR_LISTBOX:
        case CTLCOLOR_BTN:
        case CTLCOLOR_DLG:
        case CTLCOLOR_SCROLLBAR:
        case CTLCOLOR_STATIC:
    }
*/
}

MsgRouting AwtComponent::WmHScroll(UINT scrollCode, UINT pos,
                                   HWND hScrollbar) {
    if (hScrollbar && hScrollbar != GetHWnd()) {
        /* the last test should never happen */
        AwtComponent* sb = GetComponent(hScrollbar);
        if (sb) {
            sb->WmHScroll(scrollCode, pos, hScrollbar);
        }
    }
    return mrDoDefault;
}

MsgRouting AwtComponent::WmVScroll(UINT scrollCode, UINT pos, HWND hScrollbar)
{
    if (hScrollbar && hScrollbar != GetHWnd()) {
        /* the last test should never happen */
        AwtComponent* sb = GetComponent(hScrollbar);
        if (sb) {
            sb->WmVScroll(scrollCode, pos, hScrollbar);
        }
    }
    return mrDoDefault;
}

namespace TimeHelper {
    // Sometimes the message belongs to another event queue and
    // GetMessageTime() may return wrong non-zero value (the case is
    // the TrayIcon peer). Using TimeHelper::windowsToUTC(::GetTickCount())
    // could help there.
    static DWORD getMessageTimeWindows(){
        DWORD time = ::GetMessageTime();
        // The following 'if' seems to be a unneeded hack.
        // Consider removing it.
        if (time == 0) {
            time = ::GetTickCount();
        }
        return time;
    }

    jlong getMessageTimeUTC() {
        return windowsToUTC(getMessageTimeWindows());
    }

    // If calling order of GetTickCount and JVM_CurrentTimeMillis
    // is swapped, it would sometimes give different result.
    // Anyway, we would not always have determinism
    // and sortedness of time conversion here (due to Windows's
    // timers peculiarities). Having some euristic algorithm might
    // help here.
    jlong windowsToUTC(DWORD windowsTime) {
        jlong offset = ::GetTickCount() - windowsTime;
        jlong jvm_time = ::JVM_CurrentTimeMillis(NULL, 0);
        return jvm_time - offset;
    }
} //TimeHelper

MsgRouting AwtComponent::WmPaint(HDC)
{
    /* Get the rectangle that covers all update regions, if any exist. */
    RECT r;
    if (::GetUpdateRect(GetHWnd(), &r, FALSE)) {
        if ((r.right-r.left) > 0 && (r.bottom-r.top) > 0 &&
            m_peerObject != NULL && m_callbacksEnabled) {
            /*
             * Always call handlePaint, because the underlying control
             * will have painted itself (the "background") before any
             * paint method is called.
             */
            DoCallback("handlePaint", "(IIII)V",
                       r.left, r.top, r.right-r.left, r.bottom-r.top);
        }
    }
    return mrDoDefault;
}

void AwtComponent::PaintUpdateRgn(const RECT *insets)
{
    // Fix 4530093: Don't Validate if can't actually paint
    if (m_peerObject == NULL || !m_callbacksEnabled) {

        // Fix 4745222: If we dont ValidateRgn,  windows will keep sending
        // WM_PAINT messages until we do. This causes java to go into
        // a tight loop that increases CPU to 100% and starves main
        // thread which needs to complete initialization, but cant.
        ::ValidateRgn(GetHWnd(), NULL);

        return;
    }

    HRGN rgn = ::CreateRectRgn(0,0,1,1);
    int updated = ::GetUpdateRgn(GetHWnd(), rgn, FALSE);
    /*
     * Now remove all update regions from this window -- do it
     * here instead of after the Java upcall, in case any new
     * updating is requested.
     */
    ::ValidateRgn(GetHWnd(), NULL);

    if (updated == COMPLEXREGION || updated == SIMPLEREGION) {
        if (insets != NULL) {
            ::OffsetRgn(rgn, insets->left, insets->top);
        }
        int size = ::GetRegionData(rgn, 0, NULL);
        if (size == 0) {
            ::DeleteObject((HGDIOBJ)rgn);
            return;
        }
        char* buffer = new char[size];
        memset(buffer, 0, size);
        LPRGNDATA rgndata = (LPRGNDATA)buffer;
        rgndata->rdh.dwSize = sizeof(RGNDATAHEADER);
        rgndata->rdh.iType = RDH_RECTANGLES;
        int retCode = ::GetRegionData(rgn, size, rgndata);
        VERIFY(retCode);
        if (retCode == 0) {
            delete [] buffer;
            ::DeleteObject((HGDIOBJ)rgn);
            return;
        }
        /*
         * Updating rects are divided into mostly vertical and mostly horizontal
         * Each group is united together and if not empty painted separately
         */
        RECT* r = (RECT*)(buffer + rgndata->rdh.dwSize);
        RECT* un[2] = {0, 0};
    DWORD i;
    for (i = 0; i < rgndata->rdh.nCount; i++, r++) {
            int width = r->right-r->left;
            int height = r->bottom-r->top;
            if (width > 0 && height > 0) {
                int toAdd = (width > height) ? 0: 1;
                if (un[toAdd] != 0) {
                    ::UnionRect(un[toAdd], un[toAdd], r);
                } else {
                    un[toAdd] = r;
                }
            }
        }
        for(i = 0; i < 2; i++) {
            if (un[i] != 0) {
                DoCallback("handleExpose", "(IIII)V", un[i]->left, un[i]->top,
                    un[i]->right-un[i]->left, un[i]->bottom-un[i]->top);
            }
        }
        delete [] buffer;
    }
    ::DeleteObject((HGDIOBJ)rgn);
}

MsgRouting AwtComponent::WmMouseEnter(UINT flags, int x, int y)
{
    SendMouseEvent(java_awt_event_MouseEvent_MOUSE_ENTERED,
                   TimeHelper::getMessageTimeUTC(), x, y, GetJavaModifiers(), 0, JNI_FALSE);
    if ((flags & ALL_MK_BUTTONS) == 0) {
        AwtCursor::UpdateCursor(this);
    }
    sm_cursorOn = GetHWnd();
    return mrConsume;   /* Don't pass our synthetic event on! */
}

MSG*
AwtComponent::CreateMessage(UINT message, WPARAM wParam, LPARAM lParam,
                            int x = 0, int y = 0)
{
    MSG* pMsg = new MSG;
    InitMessage(pMsg, message, wParam, lParam, x, y);
    return pMsg;
}


jint
AwtComponent::GetDrawState(HWND hwnd) {
    return (jint)(INT_PTR)(::GetProp(hwnd, DrawingStateProp));
}

void
AwtComponent::SetDrawState(HWND hwnd, jint state) {
    ::SetProp(hwnd, DrawingStateProp, (HANDLE)(INT_PTR)state);
}

void
AwtComponent::InitMessage(MSG* msg, UINT message, WPARAM wParam, LPARAM lParam,
                            int x = 0, int y = 0)
{
    msg->message = message;
    msg->wParam = wParam;
    msg->lParam = lParam;
    msg->time = TimeHelper::getMessageTimeWindows();
    msg->pt.x = x;
    msg->pt.y = y;
}

MsgRouting AwtComponent::WmNcMouseDown(WPARAM hitTest, int x, int y, int button) {
    return mrDoDefault;
}
MsgRouting AwtComponent::WmNcMouseUp(WPARAM hitTest, int x, int y, int button) {
    return mrDoDefault;
}

MsgRouting AwtComponent::WmWindowPosChanging(LPARAM windowPos) {
    return mrDoDefault;
}
MsgRouting AwtComponent::WmWindowPosChanged(LPARAM windowPos) {
    return mrDoDefault;
}

/* Double-click variables. */
static jlong multiClickTime = ::GetDoubleClickTime();
static int multiClickMaxX = ::GetSystemMetrics(SM_CXDOUBLECLK);
static int multiClickMaxY = ::GetSystemMetrics(SM_CYDOUBLECLK);
static AwtComponent* lastClickWnd = NULL;
static jlong lastTime = 0;
static int lastClickX = 0;
static int lastClickY = 0;
static int lastButton = 0;
static int clickCount = 0;

// A static method that makes the clickCount available in the derived classes
// overriding WmMouseDown().
int AwtComponent::GetClickCount()
{
    return clickCount;
}

MsgRouting AwtComponent::WmMouseDown(UINT flags, int x, int y, int button)
{
    jlong now = TimeHelper::getMessageTimeUTC();

    if (lastClickWnd == this &&
        lastButton == button &&
        (now - lastTime) <= multiClickTime &&
        abs(x - lastClickX) <= multiClickMaxX &&
        abs(y - lastClickY) <= multiClickMaxY)
    {
        clickCount++;
    } else {
        clickCount = 1;
        lastClickWnd = this;
        lastButton = button;
        lastClickX = x;
        lastClickY = y;
    }
    lastTime = now;
    // it's needed only if WM_LBUTTONUP doesn't come for some reason
    m_mouseDragState &= ~GetButtonMK(button);

    MSG msg;
    InitMessage(&msg, lastMessage, flags, MAKELPARAM(x, y), x, y);

    SendMouseEvent(java_awt_event_MouseEvent_MOUSE_PRESSED, now, x, y,
                   GetJavaModifiers(), clickCount, JNI_FALSE,
                   GetButton(button), &msg);
    /*
     * NOTE: this call is intentionally placed after all other code,
     * since AwtComponent::WmMouseDown() assumes that the cached id of the
     * latest retrieved message (see lastMessage in awt_Component.cpp)
     * matches the mouse message being processed.
     * SetCapture() sends WM_CAPTURECHANGED and breaks that
     * assumption.
     */
    SetDragCapture(flags);

    AwtWindow * owner = (AwtWindow*)GetComponent(GetTopLevelParentForWindow(GetHWnd()));
    if (AwtWindow::GetGrabbedWindow() != NULL && owner != NULL) {
        if (!AwtWindow::GetGrabbedWindow()->IsOneOfOwnersOf(owner)) {
            AwtWindow::GetGrabbedWindow()->Ungrab();
        }
    }

    return mrConsume;
}

MsgRouting AwtComponent::WmMouseUp(UINT flags, int x, int y, int button)
{
    MSG msg;
    InitMessage(&msg, lastMessage, flags, MAKELPARAM(x, y), x, y);

    SendMouseEvent(java_awt_event_MouseEvent_MOUSE_RELEASED, TimeHelper::getMessageTimeUTC(),
                   x, y, GetJavaModifiers(), clickCount,
                   (GetButton(button) == java_awt_event_MouseEvent_BUTTON3 ?
                    TRUE : FALSE), GetButton(button), &msg);
    /*
     * If no movement, then report a click following the button release
     */
    if (!(m_mouseDragState & GetButtonMK(button))) { // No up-button in the drag-state
        SendMouseEvent(java_awt_event_MouseEvent_MOUSE_CLICKED,
                       TimeHelper::getMessageTimeUTC(), x, y, GetJavaModifiers(),
                       clickCount, JNI_FALSE, GetButton(button));
    }
    m_mouseDragState &= ~GetButtonMK(button); // Exclude the up-button from the drag-state

    if ((flags & ALL_MK_BUTTONS) == 0) {
        // only update if all buttons have been released
        AwtCursor::UpdateCursor(this);
    }
    /*
     * NOTE: this call is intentionally placed after all other code,
     * since AwtComponent::WmMouseUp() assumes that the cached id of the
     * latest retrieved message (see lastMessage in awt_Component.cpp)
     * matches the mouse message being processed.
     * ReleaseCapture() sends WM_CAPTURECHANGED and breaks that
     * assumption.
     */
    ReleaseDragCapture(flags);

    return mrConsume;
}

MsgRouting AwtComponent::WmMouseMove(UINT flags, int x, int y)
{
    static AwtComponent* lastComp = NULL;
    static int lastX = 0;
    static int lastY = 0;

    /*
     * Only report mouse move and drag events if a move or drag
     * actually happened -- Windows sends a WM_MOUSEMOVE in case the
     * app wants to modify the cursor.
     */
    if (lastComp != this || x != lastX || y != lastY) {
        lastComp = this;
        lastX = x;
        lastY = y;

        if ( (flags & ALL_MK_BUTTONS) != 0 ) {
            // 6404008 : if Dragged event fired we shouldn't fire
            // Clicked event: m_firstDragSent set to TRUE.
            // This is a partial backout of 5039416 fix.
            MSG msg;
            InitMessage(&msg, lastMessage, flags, MAKELPARAM(x, y), x, y);
            SendMouseEvent(java_awt_event_MouseEvent_MOUSE_DRAGGED, TimeHelper::getMessageTimeUTC(), x, y,
                           GetJavaModifiers(), 0, JNI_FALSE,
                           java_awt_event_MouseEvent_NOBUTTON, &msg);
            m_mouseDragState = flags;
        } else {
            MSG msg;
            InitMessage(&msg, lastMessage, flags, MAKELPARAM(x, y), x, y);
            SendMouseEvent(java_awt_event_MouseEvent_MOUSE_MOVED, TimeHelper::getMessageTimeUTC(), x, y,
                           GetJavaModifiers(), 0, JNI_FALSE,
                           java_awt_event_MouseEvent_NOBUTTON, &msg);
        }
    }

    return mrConsume;
}

MsgRouting AwtComponent::WmMouseExit(UINT flags, int x, int y)
{
    SendMouseEvent(java_awt_event_MouseEvent_MOUSE_EXITED, TimeHelper::getMessageTimeUTC(), x,
                   y, GetJavaModifiers(), 0, JNI_FALSE);
    sm_cursorOn = NULL;
    return mrConsume;   /* Don't pass our synthetic event on! */
}

MsgRouting AwtComponent::WmMouseWheel(UINT flags, int x, int y,
                                      int wheelRotation)
{
    // convert coordinates to be Component-relative, not screen relative
    // for wheeling when outside the window, this works similar to
    // coordinates during a drag
    POINT eventPt;
    eventPt.x = x;
    eventPt.y = y;
    DTRACE_PRINT2("  original coords: %i,%i\n", x, y);
    ::ScreenToClient(GetHWnd(), &eventPt);
    DTRACE_PRINT2("  new coords: %i,%i\n\n", eventPt.x, eventPt.y);

    // set some defaults
    jint scrollType = java_awt_event_MouseWheelEvent_WHEEL_UNIT_SCROLL;
    jint scrollLines = 3;

    BOOL result;
    UINT platformLines;

    sm_wheelRotationAmount += wheelRotation;

    // AWT interprets wheel rotation differently than win32, so we need to
    // decode wheel amount.
    jint roundedWheelRotation = sm_wheelRotationAmount / (-1 * WHEEL_DELTA);
    jdouble preciseWheelRotation = (jdouble) wheelRotation / (-1 * WHEEL_DELTA);

    MSG msg;

    if (IS_WIN95 && !IS_WIN98) {
        // 95 doesn't understand the SPI_GETWHEELSCROLLLINES - get the user
        // preference by other means
        DTRACE_PRINTLN("WmMouseWheel: using 95 branch");
        platformLines = Wheel95GetScrLines();
        result = true;
        InitMessage(&msg, lastMessage, wheelRotation, MAKELPARAM(x, y));
    }
    else {
        result = ::SystemParametersInfo(SPI_GETWHEELSCROLLLINES, 0,
                                        &platformLines, 0);
        InitMessage(&msg, lastMessage, MAKEWPARAM(flags, wheelRotation),
                            MAKELPARAM(x, y));
    }

    if (result) {
        if (platformLines == WHEEL_PAGESCROLL) {
            scrollType = java_awt_event_MouseWheelEvent_WHEEL_BLOCK_SCROLL;
            scrollLines = 1;
        }
        else {
            scrollType = java_awt_event_MouseWheelEvent_WHEEL_UNIT_SCROLL;
            scrollLines = platformLines;
        }
    }

    DTRACE_PRINTLN("calling SendMouseWheelEvent");

    SendMouseWheelEvent(java_awt_event_MouseEvent_MOUSE_WHEEL, TimeHelper::getMessageTimeUTC(),
                        eventPt.x, eventPt.y, GetJavaModifiers(), 0, 0, scrollType,
                        scrollLines, roundedWheelRotation, preciseWheelRotation, &msg);

    sm_wheelRotationAmount %= WHEEL_DELTA;
    return mrConsume;
}

jint AwtComponent::GetKeyLocation(UINT wkey, UINT flags) {
    // Rector+Newcomer page 413
    // The extended keys are the Alt and Control on the right of
    // the space bar, the non-Numpad arrow keys, the non-Numpad
    // Insert, PageUp, etc. keys, and the Numpad Divide and Enter keys.
    // Note that neither Shift key is extended.
    // Although not listed in Rector+Newcomer, both Windows keys
    // (91 and 92) are extended keys, the Context Menu key
    // (property key or application key - 93) is extended,
    // and so is the NumLock key.

    // wkey is the wParam, flags is the HIWORD of the lParam

    // "Extended" bit is 24th in lParam, so it's 8th in flags = HIWORD(lParam)
    BOOL extended = ((1<<8) & flags);

    if (IsNumPadKey(wkey, extended)) {
        return java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD;
    }

    switch (wkey) {
      case VK_SHIFT:
        return AwtComponent::GetShiftKeyLocation(wkey, flags);
      case VK_CONTROL: // fall through
      case VK_MENU:
        if (extended) {
            return java_awt_event_KeyEvent_KEY_LOCATION_RIGHT;
        } else {
            return java_awt_event_KeyEvent_KEY_LOCATION_LEFT;
        }
      case VK_LWIN:
        return java_awt_event_KeyEvent_KEY_LOCATION_LEFT;
      case VK_RWIN:
        return java_awt_event_KeyEvent_KEY_LOCATION_RIGHT;
      default:
        break;
    }

    // REMIND: if we add keycodes for the windows keys, we'll have to
    // include left/right discrimination code for them.

    return java_awt_event_KeyEvent_KEY_LOCATION_STANDARD;
}

jint AwtComponent::GetShiftKeyLocation(UINT vkey, UINT flags)
{
    // init scancodes to safe values
    UINT leftShiftScancode = 0;
    UINT rightShiftScancode = 0;

    // First 8 bits of flags is the scancode
    UINT keyScanCode = flags & 0xFF;

    DTRACE_PRINTLN3(
      "AwtComponent::GetShiftKeyLocation  vkey = %d = 0x%x  scan = %d",
      vkey, vkey, keyScanCode);

    if (m_isWinNT) {
        leftShiftScancode = ::MapVirtualKey(VK_LSHIFT, 0);
        rightShiftScancode = ::MapVirtualKey(VK_RSHIFT, 0);

        if (keyScanCode == leftShiftScancode) {
            return java_awt_event_KeyEvent_KEY_LOCATION_LEFT;
        }
        if (keyScanCode == rightShiftScancode) {
            return java_awt_event_KeyEvent_KEY_LOCATION_RIGHT;
        }

        DASSERT(false);
        // Note: the above should not fail on NT (or 2000),
        // but just in case it does, try the more complicated method
        // we use for Win9x below.
    }

    // "Transition" bit = 0 if keyPressed, 1 if keyReleased
    BOOL released = ((1<<15) & flags);

    DTRACE_PRINTLN2(
      "AwtComponent::GetShiftKeyLocation  bLeftShiftIsDown = %d  bRightShiftIsDown == %d",
      bLeftShiftIsDown, bRightShiftIsDown);
    DTRACE_PRINTLN2(
      "AwtComponent::GetShiftKeyLocation  lastShiftKeyPressed = %d  released = %d",
      lastShiftKeyPressed, released);

    jint keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN;

    // It is possible for somebody to hold down one or both
    // Shift keys, causing repeat key events.  We need to
    // handle all the cases.
    //
    // Just a side-note: if two or more keys are being held down,
    // and then one key is released, whether more key presses are
    // generated for the keys that are still held down depends on
    // which keys they are, and whether you released the right or
    // the left shift/ctrl/etc. key first.  This also differs
    // between Win9x and NT.  Just plain screwy.
    //
    // Note: on my PC, the repeat count is always 1.  Yup, we need
    // 16 bits to handle that, all right.

    // Handle the case where only one of the Shift keys
    // was down before this event took place
    if (bLeftShiftIsDown && !bRightShiftIsDown) {
        if (released) {
            // This is a left Shift release
            keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_LEFT;
        } else {
            // This is a right Shift press
            keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_RIGHT;
        }
    } else if (!bLeftShiftIsDown && bRightShiftIsDown) {
        if (released) {
            // This is a right Shift release
            keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_RIGHT;
        } else {
            // This is a left Shift press
            keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_LEFT;
        }
    }

    // Handle the case where neither of the Shift keys
    // were down before this event took place
    if (!bLeftShiftIsDown && !bRightShiftIsDown) {
        DASSERT(!released);
        if (HIBYTE(::GetKeyState(VK_LSHIFT)) != 0) {
            // This is a left Shift press
            keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_LEFT;
        } else if (HIBYTE(::GetKeyState(VK_RSHIFT)) != 0) {
            // This is a right Shift press
            keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_RIGHT;
        } else {
            DASSERT(false);
        }
    }

    // Handle the case where both Shift keys were down before
    // this event took place
    if (bLeftShiftIsDown && bRightShiftIsDown) {
        // If this is a key release event, we can just check to see
        // what the keyboard state is after the event
        if (released) {
            if (HIBYTE(::GetKeyState(VK_RSHIFT)) == 0) {
                // This is a right Shift release
                keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_RIGHT;
            } else if (HIBYTE(::GetKeyState(VK_LSHIFT)) == 0) {
                // This is a left Shift release
                keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_LEFT;
            } else {
                DASSERT(false);
            }
        } else {
            // If this is a key press event, and both Shift keys were
            // already down, this is going to be a repeat of the last
            // Shift press
            if (lastShiftKeyPressed == VK_LSHIFT) {
                keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_LEFT;
            } else if (lastShiftKeyPressed == VK_RSHIFT) {
                keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_RIGHT;
            } else {
                DASSERT(false);
            }
        }
    }

    if (keyLocation == java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN) {
        // Nothing we tried above worked for some reason.  Sigh.
        // Make a last-ditch effort to guess what happened:
        // guess that the Shift scancodes are usually the same
        // from system to system, even though this isn't guaranteed.
        DTRACE_PRINTLN("Last-ditch effort at guessing Shift keyLocation");

        // Tested on a couple of Windows keyboards: these are standard values
        leftShiftScancode = 42;
        rightShiftScancode = 54;

        if (keyScanCode == leftShiftScancode) {
            keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_LEFT;
        } else if (keyScanCode == rightShiftScancode) {
            keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_RIGHT;
        }
    }

    // Set the Shift flags with the new key state.
    bLeftShiftIsDown = (HIBYTE(::GetKeyState(VK_LSHIFT)) != 0);
    bRightShiftIsDown = (HIBYTE(::GetKeyState(VK_RSHIFT)) != 0);

    // Update lastShiftKeyPressed
    if (released) {
        // At most one shift key is down now, so just check which one
        if (bLeftShiftIsDown) {
            lastShiftKeyPressed = VK_LSHIFT;
            DASSERT(!bRightShiftIsDown);
        } else if (bRightShiftIsDown) {
            lastShiftKeyPressed = VK_RSHIFT;
        } else {
            lastShiftKeyPressed = 0;
        }
    } else {
        // It was a press, so at least one shift key is down now
        if (keyLocation == java_awt_event_KeyEvent_KEY_LOCATION_LEFT) {
            lastShiftKeyPressed = VK_LSHIFT;
        } else if (keyLocation == java_awt_event_KeyEvent_KEY_LOCATION_RIGHT) {
            lastShiftKeyPressed = VK_RSHIFT;
        }
    }

    return keyLocation;
}

/* Returns Java extended InputEvent modifieres.
 * Since ::GetKeyState returns current state and Java modifiers represent
 * state before event, modifier on changed key are inverted.
 */
jint
AwtComponent::GetJavaModifiers()
{
    jint modifiers = 0;

    if (HIBYTE(::GetKeyState(VK_CONTROL)) != 0) {
        modifiers |= java_awt_event_InputEvent_CTRL_DOWN_MASK;
    }
    if (HIBYTE(::GetKeyState(VK_SHIFT)) != 0) {
        modifiers |= java_awt_event_InputEvent_SHIFT_DOWN_MASK;
    }
    if (HIBYTE(::GetKeyState(VK_MENU)) != 0) {
        modifiers |= java_awt_event_InputEvent_ALT_DOWN_MASK;
    }
    if (HIBYTE(::GetKeyState(VK_MBUTTON)) != 0) {
        modifiers |= java_awt_event_InputEvent_BUTTON2_DOWN_MASK;
    }
    if (HIBYTE(::GetKeyState(VK_RBUTTON)) != 0) {
        modifiers |= java_awt_event_InputEvent_BUTTON3_DOWN_MASK;
    }
    if (HIBYTE(::GetKeyState(VK_LBUTTON)) != 0) {
        modifiers |= java_awt_event_InputEvent_BUTTON1_DOWN_MASK;
    }
    return modifiers;
}

jint
AwtComponent::GetButton(int mouseButton)
{
    /* Mouse buttons are already set correctly for left/right handedness */
    switch(mouseButton) {
    case LEFT_BUTTON:
        return java_awt_event_MouseEvent_BUTTON1;
    case MIDDLE_BUTTON:
        return java_awt_event_MouseEvent_BUTTON2;
    case RIGHT_BUTTON:
        return java_awt_event_MouseEvent_BUTTON3;
    }
    return java_awt_event_MouseEvent_NOBUTTON;
}

UINT
AwtComponent::GetButtonMK(int mouseButton)
{
    switch(mouseButton) {
    case LEFT_BUTTON:
        return MK_LBUTTON;
    case MIDDLE_BUTTON:
        return MK_MBUTTON;
    case RIGHT_BUTTON:
        return MK_RBUTTON;
    }
    return 0;
}

// FIXME: Keyboard related stuff has grown so big and hairy that we
// really need to move it into a class of its own.  And, since
// keyboard is a shared resource, AwtComponent is a bad place for it.

// These constants are defined in the Japanese version of VC++5.0,
// but not the US version
#ifndef VK_CONVERT
#define VK_KANA           0x15
#define VK_KANJI          0x19
#define VK_CONVERT        0x1C
#define VK_NONCONVERT     0x1D
#endif

typedef struct {
    UINT javaKey;
    UINT windowsKey;
} KeyMapEntry;

// Static table, arranged more or less spatially.
KeyMapEntry keyMapTable[] = {
    // Modifier keys
    {java_awt_event_KeyEvent_VK_CAPS_LOCK,        VK_CAPITAL},
    {java_awt_event_KeyEvent_VK_SHIFT,            VK_SHIFT},
    {java_awt_event_KeyEvent_VK_CONTROL,          VK_CONTROL},
    {java_awt_event_KeyEvent_VK_ALT,              VK_MENU},
    {java_awt_event_KeyEvent_VK_NUM_LOCK,         VK_NUMLOCK},

    // Miscellaneous Windows keys
    {java_awt_event_KeyEvent_VK_WINDOWS,          VK_LWIN},
    {java_awt_event_KeyEvent_VK_WINDOWS,          VK_RWIN},
    {java_awt_event_KeyEvent_VK_CONTEXT_MENU,     VK_APPS},

    // Alphabet
    {java_awt_event_KeyEvent_VK_A,                'A'},
    {java_awt_event_KeyEvent_VK_B,                'B'},
    {java_awt_event_KeyEvent_VK_C,                'C'},
    {java_awt_event_KeyEvent_VK_D,                'D'},
    {java_awt_event_KeyEvent_VK_E,                'E'},
    {java_awt_event_KeyEvent_VK_F,                'F'},
    {java_awt_event_KeyEvent_VK_G,                'G'},
    {java_awt_event_KeyEvent_VK_H,                'H'},
    {java_awt_event_KeyEvent_VK_I,                'I'},
    {java_awt_event_KeyEvent_VK_J,                'J'},
    {java_awt_event_KeyEvent_VK_K,                'K'},
    {java_awt_event_KeyEvent_VK_L,                'L'},
    {java_awt_event_KeyEvent_VK_M,                'M'},
    {java_awt_event_KeyEvent_VK_N,                'N'},
    {java_awt_event_KeyEvent_VK_O,                'O'},
    {java_awt_event_KeyEvent_VK_P,                'P'},
    {java_awt_event_KeyEvent_VK_Q,                'Q'},
    {java_awt_event_KeyEvent_VK_R,                'R'},
    {java_awt_event_KeyEvent_VK_S,                'S'},
    {java_awt_event_KeyEvent_VK_T,                'T'},
    {java_awt_event_KeyEvent_VK_U,                'U'},
    {java_awt_event_KeyEvent_VK_V,                'V'},
    {java_awt_event_KeyEvent_VK_W,                'W'},
    {java_awt_event_KeyEvent_VK_X,                'X'},
    {java_awt_event_KeyEvent_VK_Y,                'Y'},
    {java_awt_event_KeyEvent_VK_Z,                'Z'},

    // Standard numeric row
    {java_awt_event_KeyEvent_VK_0,                '0'},
    {java_awt_event_KeyEvent_VK_1,                '1'},
    {java_awt_event_KeyEvent_VK_2,                '2'},
    {java_awt_event_KeyEvent_VK_3,                '3'},
    {java_awt_event_KeyEvent_VK_4,                '4'},
    {java_awt_event_KeyEvent_VK_5,                '5'},
    {java_awt_event_KeyEvent_VK_6,                '6'},
    {java_awt_event_KeyEvent_VK_7,                '7'},
    {java_awt_event_KeyEvent_VK_8,                '8'},
    {java_awt_event_KeyEvent_VK_9,                '9'},

    // Misc key from main block
    {java_awt_event_KeyEvent_VK_ENTER,            VK_RETURN},
    {java_awt_event_KeyEvent_VK_SPACE,            VK_SPACE},
    {java_awt_event_KeyEvent_VK_BACK_SPACE,       VK_BACK},
    {java_awt_event_KeyEvent_VK_TAB,              VK_TAB},
    {java_awt_event_KeyEvent_VK_ESCAPE,           VK_ESCAPE},

    // NumPad with NumLock off & extended block (rectangular)
    {java_awt_event_KeyEvent_VK_INSERT,           VK_INSERT},
    {java_awt_event_KeyEvent_VK_DELETE,           VK_DELETE},
    {java_awt_event_KeyEvent_VK_HOME,             VK_HOME},
    {java_awt_event_KeyEvent_VK_END,              VK_END},
    {java_awt_event_KeyEvent_VK_PAGE_UP,          VK_PRIOR},
    {java_awt_event_KeyEvent_VK_PAGE_DOWN,        VK_NEXT},
    {java_awt_event_KeyEvent_VK_CLEAR,            VK_CLEAR}, // NumPad 5

    // NumPad with NumLock off & extended arrows block (triangular)
    {java_awt_event_KeyEvent_VK_LEFT,             VK_LEFT},
    {java_awt_event_KeyEvent_VK_RIGHT,            VK_RIGHT},
    {java_awt_event_KeyEvent_VK_UP,               VK_UP},
    {java_awt_event_KeyEvent_VK_DOWN,             VK_DOWN},

    // NumPad with NumLock on: numbers
    {java_awt_event_KeyEvent_VK_NUMPAD0,          VK_NUMPAD0},
    {java_awt_event_KeyEvent_VK_NUMPAD1,          VK_NUMPAD1},
    {java_awt_event_KeyEvent_VK_NUMPAD2,          VK_NUMPAD2},
    {java_awt_event_KeyEvent_VK_NUMPAD3,          VK_NUMPAD3},
    {java_awt_event_KeyEvent_VK_NUMPAD4,          VK_NUMPAD4},
    {java_awt_event_KeyEvent_VK_NUMPAD5,          VK_NUMPAD5},
    {java_awt_event_KeyEvent_VK_NUMPAD6,          VK_NUMPAD6},
    {java_awt_event_KeyEvent_VK_NUMPAD7,          VK_NUMPAD7},
    {java_awt_event_KeyEvent_VK_NUMPAD8,          VK_NUMPAD8},
    {java_awt_event_KeyEvent_VK_NUMPAD9,          VK_NUMPAD9},

    // NumPad with NumLock on
    {java_awt_event_KeyEvent_VK_MULTIPLY,         VK_MULTIPLY},
    {java_awt_event_KeyEvent_VK_ADD,              VK_ADD},
    {java_awt_event_KeyEvent_VK_SEPARATOR,        VK_SEPARATOR},
    {java_awt_event_KeyEvent_VK_SUBTRACT,         VK_SUBTRACT},
    {java_awt_event_KeyEvent_VK_DECIMAL,          VK_DECIMAL},
    {java_awt_event_KeyEvent_VK_DIVIDE,           VK_DIVIDE},

    // Functional keys
    {java_awt_event_KeyEvent_VK_F1,               VK_F1},
    {java_awt_event_KeyEvent_VK_F2,               VK_F2},
    {java_awt_event_KeyEvent_VK_F3,               VK_F3},
    {java_awt_event_KeyEvent_VK_F4,               VK_F4},
    {java_awt_event_KeyEvent_VK_F5,               VK_F5},
    {java_awt_event_KeyEvent_VK_F6,               VK_F6},
    {java_awt_event_KeyEvent_VK_F7,               VK_F7},
    {java_awt_event_KeyEvent_VK_F8,               VK_F8},
    {java_awt_event_KeyEvent_VK_F9,               VK_F9},
    {java_awt_event_KeyEvent_VK_F10,              VK_F10},
    {java_awt_event_KeyEvent_VK_F11,              VK_F11},
    {java_awt_event_KeyEvent_VK_F12,              VK_F12},
    {java_awt_event_KeyEvent_VK_F13,              VK_F13},
    {java_awt_event_KeyEvent_VK_F14,              VK_F14},
    {java_awt_event_KeyEvent_VK_F15,              VK_F15},
    {java_awt_event_KeyEvent_VK_F16,              VK_F16},
    {java_awt_event_KeyEvent_VK_F17,              VK_F17},
    {java_awt_event_KeyEvent_VK_F18,              VK_F18},
    {java_awt_event_KeyEvent_VK_F19,              VK_F19},
    {java_awt_event_KeyEvent_VK_F20,              VK_F20},
    {java_awt_event_KeyEvent_VK_F21,              VK_F21},
    {java_awt_event_KeyEvent_VK_F22,              VK_F22},
    {java_awt_event_KeyEvent_VK_F23,              VK_F23},
    {java_awt_event_KeyEvent_VK_F24,              VK_F24},

    {java_awt_event_KeyEvent_VK_PRINTSCREEN,      VK_SNAPSHOT},
    {java_awt_event_KeyEvent_VK_SCROLL_LOCK,      VK_SCROLL},
    {java_awt_event_KeyEvent_VK_PAUSE,            VK_PAUSE},
    {java_awt_event_KeyEvent_VK_CANCEL,           VK_CANCEL},
    {java_awt_event_KeyEvent_VK_HELP,             VK_HELP},

    // Japanese
    {java_awt_event_KeyEvent_VK_CONVERT,          VK_CONVERT},
    {java_awt_event_KeyEvent_VK_NONCONVERT,       VK_NONCONVERT},
    {java_awt_event_KeyEvent_VK_INPUT_METHOD_ON_OFF, VK_KANJI},
    {java_awt_event_KeyEvent_VK_ALPHANUMERIC,     VK_DBE_ALPHANUMERIC},
    {java_awt_event_KeyEvent_VK_KATAKANA,         VK_DBE_KATAKANA},
    {java_awt_event_KeyEvent_VK_HIRAGANA,         VK_DBE_HIRAGANA},
    {java_awt_event_KeyEvent_VK_FULL_WIDTH,       VK_DBE_DBCSCHAR},
    {java_awt_event_KeyEvent_VK_HALF_WIDTH,       VK_DBE_SBCSCHAR},
    {java_awt_event_KeyEvent_VK_ROMAN_CHARACTERS, VK_DBE_ROMAN},

    {java_awt_event_KeyEvent_VK_UNDEFINED,        0}
};


// Dynamic mapping table for OEM VK codes.  This table is refilled
// by BuildDynamicKeyMapTable when keyboard layout is switched.
// (see NT4 DDK src/input/inc/vkoem.h for OEM VK_ values).
struct DynamicKeyMapEntry {
    UINT windowsKey;            // OEM VK codes known in advance
    UINT javaKey;               // depends on input langauge (kbd layout)
};

static DynamicKeyMapEntry dynamicKeyMapTable[] = {
    {0x00BA,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_1
    {0x00BB,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_PLUS
    {0x00BC,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_COMMA
    {0x00BD,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_MINUS
    {0x00BE,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_PERIOD
    {0x00BF,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_2
    {0x00C0,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_3
    {0x00DB,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_4
    {0x00DC,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_5
    {0x00DD,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_6
    {0x00DE,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_7
    {0x00DF,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_8
    {0x00E2,  java_awt_event_KeyEvent_VK_UNDEFINED}, // VK_OEM_102
    {0, 0}
};



// Auxiliary tables used to fill the above dynamic table.  We first
// find the character for the OEM VK code using ::MapVirtualKey and
// then go through these auxiliary tables to map it to Java VK code.

struct CharToVKEntry {
    WCHAR c;
    UINT  javaKey;
};

static const CharToVKEntry charToVKTable[] = {
    {L'!',   java_awt_event_KeyEvent_VK_EXCLAMATION_MARK},
    {L'"',   java_awt_event_KeyEvent_VK_QUOTEDBL},
    {L'#',   java_awt_event_KeyEvent_VK_NUMBER_SIGN},
    {L'$',   java_awt_event_KeyEvent_VK_DOLLAR},
    {L'&',   java_awt_event_KeyEvent_VK_AMPERSAND},
    {L'\'',  java_awt_event_KeyEvent_VK_QUOTE},
    {L'(',   java_awt_event_KeyEvent_VK_LEFT_PARENTHESIS},
    {L')',   java_awt_event_KeyEvent_VK_RIGHT_PARENTHESIS},
    {L'*',   java_awt_event_KeyEvent_VK_ASTERISK},
    {L'+',   java_awt_event_KeyEvent_VK_PLUS},
    {L',',   java_awt_event_KeyEvent_VK_COMMA},
    {L'-',   java_awt_event_KeyEvent_VK_MINUS},
    {L'.',   java_awt_event_KeyEvent_VK_PERIOD},
    {L'/',   java_awt_event_KeyEvent_VK_SLASH},
    {L':',   java_awt_event_KeyEvent_VK_COLON},
    {L';',   java_awt_event_KeyEvent_VK_SEMICOLON},
    {L'<',   java_awt_event_KeyEvent_VK_LESS},
    {L'=',   java_awt_event_KeyEvent_VK_EQUALS},
    {L'>',   java_awt_event_KeyEvent_VK_GREATER},
    {L'@',   java_awt_event_KeyEvent_VK_AT},
    {L'[',   java_awt_event_KeyEvent_VK_OPEN_BRACKET},
    {L'\\',  java_awt_event_KeyEvent_VK_BACK_SLASH},
    {L']',   java_awt_event_KeyEvent_VK_CLOSE_BRACKET},
    {L'^',   java_awt_event_KeyEvent_VK_CIRCUMFLEX},
    {L'_',   java_awt_event_KeyEvent_VK_UNDERSCORE},
    {L'`',   java_awt_event_KeyEvent_VK_BACK_QUOTE},
    {L'{',   java_awt_event_KeyEvent_VK_BRACELEFT},
    {L'}',   java_awt_event_KeyEvent_VK_BRACERIGHT},
    {0x00A1, java_awt_event_KeyEvent_VK_INVERTED_EXCLAMATION_MARK},
    {0x20A0, java_awt_event_KeyEvent_VK_EURO_SIGN}, // ????
    {0,0}
};

// For dead accents some layouts return ASCII punctuation, while some
// return spacing accent chars, so both should be listed.  NB: MS docs
// say that conversion routings return spacing accent character, not
// combining.
static const CharToVKEntry charToDeadVKTable[] = {
    {L'`',   java_awt_event_KeyEvent_VK_DEAD_GRAVE},
    {L'\'',  java_awt_event_KeyEvent_VK_DEAD_ACUTE},
    {0x00B4, java_awt_event_KeyEvent_VK_DEAD_ACUTE},
    {L'^',   java_awt_event_KeyEvent_VK_DEAD_CIRCUMFLEX},
    {L'~',   java_awt_event_KeyEvent_VK_DEAD_TILDE},
    {0x02DC, java_awt_event_KeyEvent_VK_DEAD_TILDE},
    {0x00AF, java_awt_event_KeyEvent_VK_DEAD_MACRON},
    {0x02D8, java_awt_event_KeyEvent_VK_DEAD_BREVE},
    {0x02D9, java_awt_event_KeyEvent_VK_DEAD_ABOVEDOT},
    {L'"',   java_awt_event_KeyEvent_VK_DEAD_DIAERESIS},
    {0x00A8, java_awt_event_KeyEvent_VK_DEAD_DIAERESIS},
    {0x02DA, java_awt_event_KeyEvent_VK_DEAD_ABOVERING},
    {0x02DD, java_awt_event_KeyEvent_VK_DEAD_DOUBLEACUTE},
    {0x02C7, java_awt_event_KeyEvent_VK_DEAD_CARON},            // aka hacek
    {L',',   java_awt_event_KeyEvent_VK_DEAD_CEDILLA},
    {0x00B8, java_awt_event_KeyEvent_VK_DEAD_CEDILLA},
    {0x02DB, java_awt_event_KeyEvent_VK_DEAD_OGONEK},
    {0x037A, java_awt_event_KeyEvent_VK_DEAD_IOTA},             // ASCII ???
    {0x309B, java_awt_event_KeyEvent_VK_DEAD_VOICED_SOUND},
    {0x309C, java_awt_event_KeyEvent_VK_DEAD_SEMIVOICED_SOUND},
    {0,0}
};


void
AwtComponent::InitDynamicKeyMapTable()
{
    static BOOL kbdinited = FALSE;

    if (!kbdinited) {
        AwtComponent::BuildDynamicKeyMapTable();
        kbdinited = TRUE;
    }
}

void
AwtComponent::BuildDynamicKeyMapTable()
{
    HKL hkl = GetKeyboardLayout();

    DTRACE_PRINTLN2("Building dynamic VK mapping tables: HKL = %08X (CP%d)",
                    hkl, AwtComponent::GetCodePage());

    // Will need this to reset layout after dead keys.
    UINT spaceScanCode = ::MapVirtualKeyEx(VK_SPACE, 0, hkl);

    // Entries in dynamic table that maps between Java VK and Windows
    // VK are built in three steps:
    //   1. Map windows VK to ANSI character (cannot map to unicode
    //      directly, since ::ToUnicode is not implemented on win9x)
    //   2. Convert ANSI char to Unicode char
    //   3. Map Unicode char to Java VK via two auxilary tables.

    for (DynamicKeyMapEntry *dynamic = dynamicKeyMapTable;
         dynamic->windowsKey != 0;
         ++dynamic)
    {
        // Defaults to VK_UNDEFINED
        dynamic->javaKey = java_awt_event_KeyEvent_VK_UNDEFINED;

        BYTE kbdState[AwtToolkit::KB_STATE_SIZE];
        AwtToolkit::GetKeyboardState(kbdState);

        kbdState[dynamic->windowsKey] |=  0x80; // Press the key.

        // Unpress modifiers, since they are most likely pressed as
        // part of the keyboard switching shortcut.
        kbdState[VK_CONTROL] &= ~0x80;
        kbdState[VK_SHIFT]   &= ~0x80;
        kbdState[VK_MENU]    &= ~0x80;

        char cbuf[2] = { '\0', '\0'};
        UINT scancode = ::MapVirtualKeyEx(dynamic->windowsKey, 0, hkl);
        int nchars = ::ToAsciiEx(dynamic->windowsKey, scancode, kbdState,
                                 (WORD*)cbuf, 0, hkl);

        // Auxiliary table used to map Unicode character to Java VK.
        // Will assign a different table for dead keys (below).
        const CharToVKEntry *charMap = charToVKTable;

        if (nchars < 0) { // Dead key
            // Use a different table for dead chars since different layouts
            // return different characters for the same dead key.
            charMap = charToDeadVKTable;

            // We also need to reset layout so that next translation
            // is unaffected by the dead status.  We do this by
            // translating <SPACE> key.
            kbdState[dynamic->windowsKey] &= ~0x80;
            kbdState[VK_SPACE] |= 0x80;

            char junkbuf[2] = { '\0', '\0'};
            ::ToAsciiEx(VK_SPACE, spaceScanCode, kbdState,
                        (WORD*)junkbuf, 0, hkl);
        }

#ifdef DEBUG
        if (nchars == 0) {
            DTRACE_PRINTLN1("VK 0x%02X -> cannot convert to ANSI char",
                            dynamic->windowsKey);
            continue;
        }
        else if (nchars > 1) {  // can't happen, see reset code below
            DTRACE_PRINTLN3("VK 0x%02X -> converted to <0x%02X,0x%02X>",
                            dynamic->windowsKey,
                            (UCHAR)cbuf[0], (UCHAR)cbuf[1]);
            continue;
        }
#endif

        WCHAR ucbuf[2] = { L'\0', L'\0' };
        int nconverted = ::MultiByteToWideChar(AwtComponent::GetCodePage(), 0,
                                               cbuf, 1, ucbuf, 2);
#ifdef DEBUG
        if (nconverted < 0) {
            DTRACE_PRINTLN3("VK 0x%02X -> ANSI 0x%02X -> MultiByteToWideChar failed (0x%X)",
                            dynamic->windowsKey, (UCHAR)cbuf[0],
                            ::GetLastError());
            continue;
        }
#endif

        WCHAR uc = ucbuf[0];
        for (const CharToVKEntry *map = charMap;  map->c != 0;  ++map) {
            if (uc == map->c) {
                dynamic->javaKey = map->javaKey;
                break;
            }
        }

        DTRACE_PRINTLN4("VK 0x%02X -> ANSI 0x%02X -> U+%04X -> Java VK 0x%X",
                        dynamic->windowsKey, (UCHAR)cbuf[0], (UINT)ucbuf[0],
                        dynamic->javaKey);
    } // for each VK_OEM_*
}


static BOOL isKanaLockAvailable()
{
    // This method is to determine whether the Kana Lock feature is
    // available on the attached keyboard.  Kana Lock feature does not
    // necessarily require that the real KANA keytop is available on
    // keyboard, so using MapVirtualKey(VK_KANA) is not sufficient for testing.
    // Instead of that we regard it as Japanese keyboard (w/ Kana Lock) if :-
    //
    // - the keyboard layout is Japanese (VK_KANA has the same value as VK_HANGUL)
    // - the keyboard is Japanese keyboard (keyboard type == 7).
    return (LOWORD(GetKeyboardLayout(0)) == MAKELANGID(LANG_JAPANESE, SUBLANG_DEFAULT))
        && (GetKeyboardType(0) == 7);
}

void AwtComponent::JavaKeyToWindowsKey(UINT javaKey,
                                       UINT *windowsKey, UINT *modifiers, UINT originalWindowsKey)
{
    // Handle the few cases where a Java VK code corresponds to a Windows
    // key/modifier combination or applies only to specific keyboard layouts
    switch (javaKey) {
        case java_awt_event_KeyEvent_VK_ALL_CANDIDATES:
            *windowsKey = VK_CONVERT;
            *modifiers = java_awt_event_InputEvent_ALT_DOWN_MASK;
            return;
        case java_awt_event_KeyEvent_VK_PREVIOUS_CANDIDATE:
            *windowsKey = VK_CONVERT;
            *modifiers = java_awt_event_InputEvent_SHIFT_DOWN_MASK;
            return;
        case java_awt_event_KeyEvent_VK_CODE_INPUT:
            *windowsKey = VK_DBE_ALPHANUMERIC;
            *modifiers = java_awt_event_InputEvent_ALT_DOWN_MASK;
            return;
        case java_awt_event_KeyEvent_VK_KANA_LOCK:
            if (isKanaLockAvailable()) {
                *windowsKey = VK_KANA;
                *modifiers = java_awt_event_InputEvent_CTRL_DOWN_MASK;
                return;
            }
    }

    // for the general case, use a bi-directional table
    for (int i = 0; keyMapTable[i].windowsKey != 0; i++) {
        if (keyMapTable[i].javaKey == javaKey) {
            *windowsKey = keyMapTable[i].windowsKey;
            *modifiers = 0;
            return;
        }
    }

    // Bug 4766655
    // Two Windows keys could map to the same Java key, so
    // give preference to the originalWindowsKey if it is
    // specified (not IGNORE_KEY).
    if (originalWindowsKey == IGNORE_KEY) {
        for (int j = 0; dynamicKeyMapTable[j].windowsKey != 0; j++) {
            if (dynamicKeyMapTable[j].javaKey == javaKey) {
                *windowsKey = dynamicKeyMapTable[j].windowsKey;
                *modifiers = 0;
                return;
            }
        }
    } else {
        BOOL found = false;
        for (int j = 0; dynamicKeyMapTable[j].windowsKey != 0; j++) {
            if (dynamicKeyMapTable[j].javaKey == javaKey) {
                *windowsKey = dynamicKeyMapTable[j].windowsKey;
                *modifiers = 0;
                found = true;
                if (*windowsKey == originalWindowsKey) {
                    return;   /* if ideal case found return, else keep looking */
                }
            }
        }
        if (found) {
            return;
        }
    }

    *windowsKey = 0;
    *modifiers = 0;
    return;
}

UINT AwtComponent::WindowsKeyToJavaKey(UINT windowsKey, UINT modifiers)
{
    // Handle the few cases where we need to take the modifier into
    // consideration for the Java VK code or where we have to take the keyboard
    // layout into consideration so that function keys can get
    // recognized in a platform-independent way.
    switch (windowsKey) {
        case VK_CONVERT:
            if ((modifiers & java_awt_event_InputEvent_ALT_DOWN_MASK) != 0) {
                return java_awt_event_KeyEvent_VK_ALL_CANDIDATES;
            }
            if ((modifiers & java_awt_event_InputEvent_SHIFT_DOWN_MASK) != 0) {
                return java_awt_event_KeyEvent_VK_PREVIOUS_CANDIDATE;
            }
            break;
        case VK_DBE_ALPHANUMERIC:
            if ((modifiers & java_awt_event_InputEvent_ALT_DOWN_MASK) != 0) {
                return java_awt_event_KeyEvent_VK_CODE_INPUT;
            }
            break;
        case VK_KANA:
            if (isKanaLockAvailable()) {
                return java_awt_event_KeyEvent_VK_KANA_LOCK;
            }
            break;
    };

    // for the general case, use a bi-directional table
    for (int i = 0; keyMapTable[i].windowsKey != 0; i++) {
        if (keyMapTable[i].windowsKey == windowsKey) {
            return keyMapTable[i].javaKey;
        }
    }

    for (int j = 0; dynamicKeyMapTable[j].windowsKey != 0; j++) {
        if (dynamicKeyMapTable[j].windowsKey == windowsKey) {
            return dynamicKeyMapTable[j].javaKey;
        }
    }

    return java_awt_event_KeyEvent_VK_UNDEFINED;
}

// determine if a key is a numpad key (distinguishes the numpad
// arrow keys from the non-numpad arrow keys, for example).
BOOL AwtComponent::IsNumPadKey(UINT vkey, BOOL extended)
{
    // Note: scancodes are the same for the numpad arrow keys and
    // the non-numpad arrow keys (also for PageUp, etc.).
    // The scancodes for the numpad divide and the non-numpad slash
    // are the same, but the wparams are different

    DTRACE_PRINTLN3("AwtComponent::IsNumPadKey  vkey = %d = 0x%x  extended = %d",
      vkey, vkey, extended);

    switch (vkey) {
      case VK_CLEAR:  // numpad 5 with numlock off
      case VK_NUMPAD0:
      case VK_NUMPAD1:
      case VK_NUMPAD2:
      case VK_NUMPAD3:
      case VK_NUMPAD4:
      case VK_NUMPAD5:
      case VK_NUMPAD6:
      case VK_NUMPAD7:
      case VK_NUMPAD8:
      case VK_NUMPAD9:
      case VK_MULTIPLY:
      case VK_ADD:
      case VK_SEPARATOR:  // numpad ,  not on US kbds
      case VK_SUBTRACT:
      case VK_DECIMAL:
      case VK_DIVIDE:
      case VK_NUMLOCK:
        return TRUE;
        break;
      case VK_END:
      case VK_PRIOR:  // PageUp
      case VK_NEXT:  // PageDown
      case VK_HOME:
      case VK_LEFT:
      case VK_UP:
      case VK_RIGHT:
      case VK_DOWN:
      case VK_INSERT:
      case VK_DELETE:
        // extended if non-numpad
        return (!extended);
        break;
      case VK_RETURN:  // extended if on numpad
        return (extended);
        break;
      default:
        break;
    }

    return FALSE;
}

UINT AwtComponent::WindowsKeyToJavaChar(UINT wkey, UINT modifiers, TransOps ops)
{
    static Hashtable transTable("VKEY translations");

    // Try to translate using last saved translation
    if (ops == LOAD) {
       void* value = transTable.remove(reinterpret_cast<void*>(static_cast<INT_PTR>(wkey)));
       if (value != NULL) {
           return static_cast<UINT>(reinterpret_cast<INT_PTR>(value));
       }
    }

    // If the windows key is a return, wkey will equal 13 ('\r')
    // In this case, we want to return 10 ('\n')
    // Since ToAscii would convert VK_RETURN to '\r', we need
    // to have a special case here.
    if (wkey == VK_RETURN)
        return '\n';

    // high order bit in keyboardState indicates whether the key is down
    static const BYTE KEY_STATE_DOWN = 0x80;
    BYTE    keyboardState[AwtToolkit::KB_STATE_SIZE];
    AwtToolkit::GetKeyboardState(keyboardState);

    // apply modifiers to keyboard state if necessary
    if (modifiers) {
        BOOL shiftIsDown = modifiers & java_awt_event_InputEvent_SHIFT_DOWN_MASK;
        BOOL altIsDown = modifiers & java_awt_event_InputEvent_ALT_DOWN_MASK;
        BOOL ctrlIsDown = modifiers & java_awt_event_InputEvent_CTRL_DOWN_MASK;

        // Windows treats AltGr as Ctrl+Alt
        if (modifiers & java_awt_event_InputEvent_ALT_GRAPH_DOWN_MASK) {
            altIsDown = TRUE;
            ctrlIsDown = TRUE;
        }

        if (shiftIsDown) {
            keyboardState[VK_SHIFT] |= KEY_STATE_DOWN;
        }

        // fix for 4623376,4737679,4501485,4740906,4708221 (4173679/4122715)
        // Here we try to resolve a conflict with ::ToAsciiEx's translating
        // ALT+number key combinations. kdm@sarc.spb.su
        keyboardState[VK_MENU] &= ~KEY_STATE_DOWN;

        if (ctrlIsDown)
        {
            if (altIsDown) {
                // bugid 4215009: don't mess with AltGr == Ctrl + Alt
                keyboardState[VK_CONTROL] |= KEY_STATE_DOWN;
            }
            else {
                // bugid 4098210: old event model doesn't have KEY_TYPED
                // events, so try to provide a meaningful character for
                // Ctrl+<key>.  Take Ctrl into account only when we know
                // that Ctrl+<key> will be an ASCII control.  Ignore by
                // default.
                keyboardState[VK_CONTROL] &= ~KEY_STATE_DOWN;

                // Letters have Ctrl+<letter> counterparts.  According to
                // <winuser.h> VK_A through VK_Z are the same as ASCII
                // 'A' through 'Z'.
                if (wkey >= 'A' && wkey <= 'Z') {
                    keyboardState[VK_CONTROL] |= KEY_STATE_DOWN;
                }
                else {
                    // Non-letter controls 033 to 037 are:
                    // ^[ (ESC), ^\ (FS), ^] (GS), ^^ (RS), and ^_ (US)

                    // Shift state bits returned by ::VkKeyScan in HIBYTE
                    static const UINT _VKS_SHIFT_MASK = 0x01;
                    static const UINT _VKS_CTRL_MASK = 0x02;
                    static const UINT _VKS_ALT_MASK = 0x04;

                    // Check to see whether there is a meaningful translation
                    TCHAR ch;
                    short vk;
                    for (ch = _T('\033'); ch < _T('\040'); ch++) {
                        vk = ::VkKeyScan(ch);
                        if (wkey == LOBYTE(vk)) {
                            UINT shiftState = HIBYTE(vk);
                            if ((shiftState & _VKS_CTRL_MASK) ||
                                (!(shiftState & _VKS_SHIFT_MASK)
                                == !shiftIsDown))
                            {
                                keyboardState[VK_CONTROL] |= KEY_STATE_DOWN;
                            }
                            break;
                        }
                    }
                }
            } // ctrlIsDown && altIsDown
        } // ctrlIsDown
    } // modifiers

    // instead of creating our own conversion tables, I'll let Win32
    // convert the character for me.
    WORD mbChar;
    UINT scancode = ::MapVirtualKey(wkey, 0);
    int converted = ::ToAsciiEx(wkey, scancode, keyboardState,
                                &mbChar, 0, GetKeyboardLayout());

    UINT translation;

    // Dead Key
    if (converted < 0) {
        translation = java_awt_event_KeyEvent_CHAR_UNDEFINED;
    } else
    // No translation available -- try known conversions or else punt.
    if (converted == 0) {
        if (wkey == VK_DELETE) {
            translation = '\177';
        } else
        if (wkey >= VK_NUMPAD0 && wkey <= VK_NUMPAD9) {
            translation = '0' + wkey - VK_NUMPAD0;
        } else {
            translation = java_awt_event_KeyEvent_CHAR_UNDEFINED;
        }
    } else
    // the caller expects a Unicode character.
    if (converted > 0) {
        WCHAR unicodeChar[2];
        VERIFY(::MultiByteToWideChar(GetCodePage(), MB_PRECOMPOSED,
        (LPCSTR)&mbChar, 1, unicodeChar, 1));

        translation = unicodeChar[0];
    }
    if (ops == SAVE) {
        transTable.put(reinterpret_cast<void*>(static_cast<INT_PTR>(wkey)),
                       reinterpret_cast<void*>(static_cast<INT_PTR>(translation)));
    }
    return translation;
}

MsgRouting AwtComponent::WmKeyDown(UINT wkey, UINT repCnt,
                                   UINT flags, BOOL system)
{
    // VK_PROCESSKEY is a special value which means
    //          "Current IME wants to consume this KeyEvent"
    // Real key code is saved by IMM32.DLL and can be retrieved by
    // calling ImmGetVirtualKey();
    if (wkey == VK_PROCESSKEY) {
        return mrDoDefault;
    }
    MSG msg;
    InitMessage(&msg, (system ? WM_SYSKEYDOWN : WM_KEYDOWN),
                             wkey, MAKELPARAM(repCnt, flags));

    UINT modifiers = GetJavaModifiers();
    jint keyLocation = GetKeyLocation(wkey, flags);
    UINT jkey = WindowsKeyToJavaKey(wkey, modifiers);
    UINT character = WindowsKeyToJavaChar(wkey, modifiers, SAVE);

    SendKeyEventToFocusOwner(java_awt_event_KeyEvent_KEY_PRESSED,
                             TimeHelper::windowsToUTC(msg.time), jkey, character,
                             modifiers, keyLocation, &msg);

    // bugid 4724007: Windows does not create a WM_CHAR for the Del key
    // for some reason, so we need to create the KEY_TYPED event on the
    // WM_KEYDOWN.  Use null msg so the character doesn't get sent back
    // to the native window for processing (this event is synthesized
    // for Java - we don't want Windows trying to process it).
    if (jkey == java_awt_event_KeyEvent_VK_DELETE) {
        SendKeyEventToFocusOwner(java_awt_event_KeyEvent_KEY_TYPED,
                                 TimeHelper::windowsToUTC(msg.time),
                                 java_awt_event_KeyEvent_VK_UNDEFINED,
                                 character, modifiers,
                                 java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN);
    }

    return mrConsume;
}

MsgRouting AwtComponent::WmKeyUp(UINT wkey, UINT repCnt,
                                 UINT flags, BOOL system)
{

    // VK_PROCESSKEY is a special value which means
    //          "Current IME wants to consume this KeyEvent"
    // Real key code is saved by IMM32.DLL and can be retrieved by
    // calling ImmGetVirtualKey();
    if (wkey == VK_PROCESSKEY) {
        return mrDoDefault;
    }
    MSG msg;
    InitMessage(&msg, (system ? WM_SYSKEYUP : WM_KEYUP),
                             wkey, MAKELPARAM(repCnt, flags));

    UINT modifiers = GetJavaModifiers();
    jint keyLocation = GetKeyLocation(wkey, flags);
    UINT jkey = WindowsKeyToJavaKey(wkey, modifiers);
    UINT character = WindowsKeyToJavaChar(wkey, modifiers, LOAD);

    SendKeyEventToFocusOwner(java_awt_event_KeyEvent_KEY_RELEASED,
                             TimeHelper::windowsToUTC(msg.time), jkey, character,
                             modifiers, keyLocation, &msg);
    return mrConsume;
}

MsgRouting AwtComponent::WmInputLangChange(UINT charset, HKL hKeyboardLayout)
{
    // Normally we would be able to use charset and TranslateCharSetInfo
    // to get a code page that should be associated with this keyboard
    // layout change. However, there seems to be an NT 4.0 bug associated
    // with the WM_INPUTLANGCHANGE message, which makes the charset parameter
    // unreliable, especially on Asian systems. Our workaround uses the
    // keyboard layout handle instead.
    m_hkl = hKeyboardLayout;
    m_idLang = LOWORD(hKeyboardLayout); // lower word of HKL is LANGID
    m_CodePage = LangToCodePage(m_idLang);
    BuildDynamicKeyMapTable();  // compute new mappings for VK_OEM
    return mrConsume;           // do not propagate to children
}

// Convert Language ID to CodePage
UINT AwtComponent::LangToCodePage(LANGID idLang)
{
    TCHAR strCodePage[MAX_ACP_STR_LEN];
    // use the LANGID to create a LCID
    LCID idLocale = MAKELCID(idLang, SORT_DEFAULT);
    // get the ANSI code page associated with this locale
    if (GetLocaleInfo(idLocale, LOCALE_IDEFAULTANSICODEPAGE, strCodePage, sizeof(strCodePage)/sizeof(TCHAR)) > 0 )
        return _ttoi(strCodePage);
    else
        return GetACP();
}


MsgRouting AwtComponent::WmIMEChar(UINT character, UINT repCnt, UINT flags, BOOL system)
{
    // We will simply create Java events here.
    WCHAR unicodeChar = character;
    MSG msg;
    InitMessage(&msg, WM_IME_CHAR, character,
                              MAKELPARAM(repCnt, flags));

    jint modifiers = GetJavaModifiers();
    SendKeyEventToFocusOwner(java_awt_event_KeyEvent_KEY_TYPED,
                             TimeHelper::windowsToUTC(msg.time),
                             java_awt_event_KeyEvent_VK_UNDEFINED,
                             unicodeChar, modifiers,
                             java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN,
                             &msg);
    return mrConsume;
}

MsgRouting AwtComponent::WmChar(UINT character, UINT repCnt, UINT flags,
                                BOOL system)
{
    // Will only get WmChar messages with DBCS if we create them for
    // an Edit class in the WmForwardChar method. These synthesized
    // DBCS chars are ok to pass on directly to the default window
    // procedure. They've already been filtered through the Java key
    // event queue. We will never get the trail byte since the edit
    // class will PeekMessage(&msg, hwnd, WM_CHAR, WM_CHAR,
    // PM_REMOVE).  I would like to be able to pass this character off
    // via WM_AWT_FORWARD_BYTE, but the Edit classes don't seem to
    // like that.

    // Begin pollution
    if (!m_isWinNT && IsDBCSLeadByteEx(GetCodePage(), BYTE(character))) {
        if (GetDBCSEditHandle() != NULL) {
            return mrDoDefault;
        } else {
            // Kludge: Some Chinese IMEs, e.g. QuanPin, sends two WM_CHAR
            // messages for some punctuations (e.g. full stop) without sending
            // WM_IME_CHAR message beforehand.
            if (m_PendingLeadByte == 0) {
                m_PendingLeadByte = character;
                return mrConsume;
            }
        }
    }
    // End pollution

    // We will simply create Java events here.
    UINT message = system ? WM_SYSCHAR : WM_CHAR;

    // The Alt modifier is reported in the 29th bit of the lParam,
    // i.e., it is the 13th bit of `flags' (which is HIWORD(lParam)).
    bool alt_is_down = (flags & (1<<13)) != 0;

    // Fix for bug 4141621, corrected by fix for bug 6223726: Alt+space doesn't invoke system menu
    // We should not pass this particular combination to Java.

    if (system && alt_is_down) {
        if (character == VK_SPACE) {
            return mrDoDefault;
        }
    }

    // If this is a WM_CHAR (non-system) message, then the Alt flag
    // indicates that the character was typed using an AltGr key
    // (which Windows treats as Ctrl+Alt), so in this case we do NOT
    // pass the Ctrl and Alt modifiers to Java, but instead we
    // replace them with Java's AltGraph modifier.  Note: the AltGraph
    // modifier does not exist in 1.1.x releases.
    jint modifiers = GetJavaModifiers();
    if (!system && alt_is_down) {
        // character typed with AltGraph
        modifiers &= ~(java_awt_event_InputEvent_ALT_DOWN_MASK
                       | java_awt_event_InputEvent_CTRL_DOWN_MASK);
        modifiers |= java_awt_event_InputEvent_ALT_GRAPH_DOWN_MASK;
    }

    WCHAR unicodeChar = character;

    // Kludge: Combine pending single byte with this char for some Chinese IMEs
    if (m_PendingLeadByte != 0) {
        character = (m_PendingLeadByte & 0x00ff) | (character << 8);
        m_PendingLeadByte = 0;
        ::MultiByteToWideChar(GetCodePage(), 0, (CHAR*)&character, 2,
                          &unicodeChar, 1);
    }

    if (unicodeChar == VK_RETURN) {
        // Enter key generates \r in windows, but \n is required in java
        unicodeChar = java_awt_event_KeyEvent_VK_ENTER;
    }
    MSG msg;
    InitMessage(&msg, message, character,
                              MAKELPARAM(repCnt, flags));
    SendKeyEventToFocusOwner(java_awt_event_KeyEvent_KEY_TYPED,
                             TimeHelper::windowsToUTC(msg.time),
                             java_awt_event_KeyEvent_VK_UNDEFINED,
                             unicodeChar, modifiers,
                             java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN,
                             &msg);
    return mrConsume;
}

MsgRouting AwtComponent::WmForwardChar(WCHAR character, LPARAM lParam,
                                       BOOL synthetic)
{
    if (m_isWinNT) {
        // just post WM_CHAR with unicode key value
        DefWindowProc(WM_CHAR, (WPARAM)character, lParam);
        return mrConsume;
    }

    // This message is sent from the Java key event handler.
    CHAR mbChar[2] = {'\0', '\0'};

    int cBytes = ::WideCharToMultiByte(GetCodePage(), 0, &character, 1, mbChar, 2, NULL, NULL);
    if (cBytes!=1 && cBytes!=2)    return mrConsume;

    HWND hDBCSEditHandle = GetDBCSEditHandle();

    if (hDBCSEditHandle != NULL && cBytes==2)
    {
        // The first WM_CHAR message will get handled by the WmChar, but
        // the second WM_CHAR message will get picked off by the Edit class.
        // WmChar will never see it.
        // If an Edit class gets a lead byte, it immediately calls PeekMessage
        // and pulls the trail byte out of the message queue.
        ::PostMessage(hDBCSEditHandle, WM_CHAR, mbChar[0] & 0x00ff, lParam);
        ::PostMessage(hDBCSEditHandle, WM_CHAR, mbChar[1] & 0x00ff, lParam);
    }
    else
    {
        MSG* pMsg;
        pMsg = CreateMessage(WM_CHAR, mbChar[0] & 0x00ff, lParam);
        ::PostMessage(GetHWnd(), WM_AWT_FORWARD_BYTE, (WPARAM)synthetic,
                      (LPARAM)pMsg);
        if (mbChar[1])
        {
            pMsg = CreateMessage(WM_CHAR, mbChar[1] & 0x00ff, lParam);
            ::PostMessage(GetHWnd(), WM_AWT_FORWARD_BYTE, (WPARAM)synthetic,
                          (LPARAM)pMsg);
        }
    }
    return mrConsume;
}

MsgRouting AwtComponent::WmPaste()
{
    return mrDoDefault;
}

// support IME Composition messages
void AwtComponent::SetCompositionWindow(RECT& r)
{
    HIMC hIMC = ImmGetContext();
    if (hIMC == NULL) {
        return;
    }
    COMPOSITIONFORM cf = {CFS_POINT, {0, r.bottom}, NULL};
    // Place the composition window right below the client Window
    ImmSetCompositionWindow(hIMC, &cf);
}

void AwtComponent::OpenCandidateWindow(int x, int y)
{
    UINT bits = 1;
    RECT rc;
    GetWindowRect(GetHWnd(), &rc);

    for (int iCandType=0; iCandType<32; iCandType++, bits<<=1) {
        if ( m_bitsCandType & bits )
            SetCandidateWindow(iCandType, x-rc.left, y-rc.top);
    }
    if (m_bitsCandType != 0) {
        DefWindowProc(WM_IME_NOTIFY, IMN_OPENCANDIDATE, m_bitsCandType);
    }
}

void AwtComponent::SetCandidateWindow(int iCandType, int x, int y)
{
    HIMC hIMC = ImmGetContext();
    CANDIDATEFORM cf;
    cf.dwIndex = iCandType;
    cf.dwStyle = CFS_CANDIDATEPOS;
    cf.ptCurrentPos.x = x;
    cf.ptCurrentPos.y = y;

    ImmSetCandidateWindow(hIMC, &cf);
}

MsgRouting AwtComponent::WmImeSetContext(BOOL fSet, LPARAM *lplParam)
{
    // This message causes native status window shown even it is disabled.  So don't
    // let DefWindowProc process this message if this IMC is disabled.
    HIMC hIMC = ImmGetContext();
    if (hIMC == NULL) {
        return mrConsume;
    }

    if (fSet) {
        LPARAM lParam = *lplParam;
        if (!m_useNativeCompWindow) {
            // stop to draw native composing window.
            *lplParam &= ~ISC_SHOWUICOMPOSITIONWINDOW;
        }
    }
    return mrDoDefault;
}

MsgRouting AwtComponent::WmImeNotify(WPARAM subMsg, LPARAM bitsCandType)
{
    if (!m_useNativeCompWindow && subMsg == IMN_OPENCANDIDATE) {
        m_bitsCandType = bitsCandType;
        InquireCandidatePosition();
        return mrConsume;
    }
    return mrDoDefault;
}

MsgRouting AwtComponent::WmImeStartComposition()
{
    if (m_useNativeCompWindow) {
        RECT rc;
        ::GetClientRect(GetHWnd(), &rc);
        SetCompositionWindow(rc);
        return mrDoDefault;
    } else
        return mrConsume;
}

MsgRouting AwtComponent::WmImeEndComposition()
{
    if (m_useNativeCompWindow)   return mrDoDefault;

    SendInputMethodEvent(
        java_awt_event_InputMethodEvent_INPUT_METHOD_TEXT_CHANGED,
        NULL, 0, NULL, NULL, 0, NULL, NULL, 0, 0, 0 );
    return mrConsume;
}

MsgRouting AwtComponent::WmImeComposition(WORD wChar, LPARAM flags)
{
    if (m_useNativeCompWindow)   return mrDoDefault;

    int*      bndClauseW = NULL;
    jstring*  readingClauseW = NULL;
    int*      bndAttrW = NULL;
    BYTE*     valAttrW = NULL;
    int       cClauseW = 0;
    AwtInputTextInfor* textInfor = NULL;

    try {
        HIMC hIMC = ImmGetContext();
        DASSERT(hIMC!=0);

        textInfor = new AwtInputTextInfor;
        textInfor->GetContextData(hIMC, flags);

        jstring jtextString = textInfor->GetText();
        /* The conditions to send the input method event to AWT EDT are:
           1. Whenever there is a composition message sent regarding whether
           the composition text is NULL or not. See details at bug 6222692.
           2. When there is a committed message sent, in which case, we have to
           check whether the committed string is NULL or not. If the committed string
           is NULL, there is no need to send any input method event.
           (Minor note: 'jtextString' returned is the merged string in the case of
           partial commit.)
        */
        if ((flags & GCS_RESULTSTR && jtextString != NULL) ||
            (flags & GCS_COMPSTR)) {
            int       cursorPosW = textInfor->GetCursorPosition();
            // In order not to delete the readingClauseW in the catch clause,
            // calling GetAttributeInfor before GetClauseInfor.
            int       cAttrW = textInfor->GetAttributeInfor(bndAttrW, valAttrW);
            cClauseW = textInfor->GetClauseInfor(bndClauseW, readingClauseW);

            /* Send INPUT_METHOD_TEXT_CHANGED event to the WInputMethod which in turn sends
               the event to AWT EDT.

               The last two paremeters are set to equal since we don't have recommendations for
               the visible position within the current composed text. See details at
               java.awt.event.InputMethodEvent.
            */
            SendInputMethodEvent(java_awt_event_InputMethodEvent_INPUT_METHOD_TEXT_CHANGED,
                                 jtextString,
                                 cClauseW, bndClauseW, readingClauseW,
                                 cAttrW, bndAttrW, valAttrW,
                                 textInfor->GetCommittedTextLength(),
                                 cursorPosW, cursorPosW);
        }
    } catch (...) {
        // since GetClauseInfor and GetAttributeInfor could throw exception, we have to release
        // the pointer here.
        delete [] bndClauseW;
        delete [] readingClauseW;
        delete [] bndAttrW;
        delete [] valAttrW;
        throw;
    }

    /* Free the storage allocated. Since jtextString won't be passed from threads
     *  to threads, we just use the local ref and it will be deleted within the destructor
     *  of AwtInputTextInfor object.
     */
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (cClauseW && readingClauseW) {
        for (int i = 0; i < cClauseW; i ++) {
            if (readingClauseW[i]) {
                env->DeleteLocalRef(readingClauseW[i]);
            }
        }
    }
    delete [] bndClauseW;
    delete [] readingClauseW;
    delete [] bndAttrW;
    delete [] valAttrW;
    delete textInfor;

    return mrConsume;
}

//
// generate and post InputMethodEvent
//
void AwtComponent::SendInputMethodEvent(jint id, jstring text,
                                        int cClause, int* rgClauseBoundary, jstring* rgClauseReading,
                                        int cAttrBlock, int* rgAttrBoundary, BYTE *rgAttrValue,
                                        int commitedTextLength, int caretPos, int visiblePos)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    // assumption for array type casting
    DASSERT(sizeof(int)==sizeof(jint));
    DASSERT(sizeof(BYTE)==sizeof(jbyte));

    // caluse information
    jintArray clauseBoundary = NULL;
    jobjectArray clauseReading = NULL;
    if (cClause && rgClauseBoundary && rgClauseReading) {
        // convert clause boundary offset array to java array
        clauseBoundary = env->NewIntArray(cClause+1);
        env->SetIntArrayRegion(clauseBoundary, 0, cClause+1, (jint *)rgClauseBoundary);
        DASSERT(!safe_ExceptionOccurred(env));

        // convert clause reading string array to java array
        clauseReading = env->NewObjectArray(cClause, JNU_ClassString(env), NULL);
        for (int i=0; i<cClause; i++)   env->SetObjectArrayElement(clauseReading, i, rgClauseReading[i]);
        DASSERT(!safe_ExceptionOccurred(env));
    }


    // attrubute value definition in WInputMethod.java must be equal to that in IMM.H
    DASSERT(ATTR_INPUT==sun_awt_windows_WInputMethod_ATTR_INPUT);
    DASSERT(ATTR_TARGET_CONVERTED==sun_awt_windows_WInputMethod_ATTR_TARGET_CONVERTED);
    DASSERT(ATTR_CONVERTED==sun_awt_windows_WInputMethod_ATTR_CONVERTED);
    DASSERT(ATTR_TARGET_NOTCONVERTED==sun_awt_windows_WInputMethod_ATTR_TARGET_NOTCONVERTED);
    DASSERT(ATTR_INPUT_ERROR==sun_awt_windows_WInputMethod_ATTR_INPUT_ERROR);

    // attribute information
    jintArray attrBoundary = NULL;
    jbyteArray attrValue = NULL;
    if (cAttrBlock && rgAttrBoundary && rgAttrValue) {
        // convert attribute boundary offset array to java array
        attrBoundary = env->NewIntArray(cAttrBlock+1);
        env->SetIntArrayRegion(attrBoundary, 0, cAttrBlock+1, (jint *)rgAttrBoundary);
        DASSERT(!safe_ExceptionOccurred(env));

        // convert attribute value byte array to java array
        attrValue = env->NewByteArray(cAttrBlock);
        env->SetByteArrayRegion(attrValue, 0, cAttrBlock, (jbyte *)rgAttrValue);
        DASSERT(!safe_ExceptionOccurred(env));
    }


    // get global reference of WInputMethod class (run only once)
    static jclass wInputMethodCls = NULL;
    if (wInputMethodCls == NULL) {
        jclass wInputMethodClsLocal = env->FindClass("sun/awt/windows/WInputMethod");
        DASSERT(wInputMethodClsLocal);
        if (wInputMethodClsLocal == NULL) {
            /* exception already thrown */
            return;
        }
        wInputMethodCls = (jclass)env->NewGlobalRef(wInputMethodClsLocal);
        env->DeleteLocalRef(wInputMethodClsLocal);
    }

    // get method ID of sendInputMethodEvent() (run only once)
    static jmethodID sendIMEventMid = 0;
    if (sendIMEventMid == 0) {
        sendIMEventMid =  env->GetMethodID(wInputMethodCls, "sendInputMethodEvent",
                                           "(IJLjava/lang/String;[I[Ljava/lang/String;[I[BIII)V");
        DASSERT(sendIMEventMid);
    }

    // call m_InputMethod.sendInputMethod()
    env->CallVoidMethod(m_InputMethod, sendIMEventMid, id, TimeHelper::getMessageTimeUTC(),
                        text, clauseBoundary, clauseReading, attrBoundary,
                        attrValue, commitedTextLength, caretPos, visiblePos);
    if (safe_ExceptionOccurred(env))   env->ExceptionDescribe();
    DASSERT(!safe_ExceptionOccurred(env));

}



//
// Inquires candidate position according to the composed text
//
void AwtComponent::InquireCandidatePosition()
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    // get global reference of WInputMethod class (run only once)
    static jclass wInputMethodCls = NULL;
    if (wInputMethodCls == NULL) {
        jclass wInputMethodClsLocal = env->FindClass("sun/awt/windows/WInputMethod");
        DASSERT(wInputMethodClsLocal);
        if (wInputMethodClsLocal == NULL) {
            /* exception already thrown */
            return;
        }
        wInputMethodCls = (jclass)env->NewGlobalRef(wInputMethodClsLocal);
        env->DeleteLocalRef(wInputMethodClsLocal);
    }

    // get method ID of sendInputMethodEvent() (run only once)
    static jmethodID inqCandPosMid = 0;
    if (inqCandPosMid == 0) {
        inqCandPosMid =  env->GetMethodID(wInputMethodCls, "inquireCandidatePosition",
                                           "()V");
        DASSERT(!safe_ExceptionOccurred(env));
        DASSERT(inqCandPosMid);
    }

    // call m_InputMethod.sendInputMethod()
    jobject candPos = env->CallObjectMethod(m_InputMethod, inqCandPosMid);
    DASSERT(!safe_ExceptionOccurred(env));
}

HIMC AwtComponent::ImmGetContext()
{
    HWND proxy = GetProxyFocusOwner();
    return ::ImmGetContext((proxy != NULL) ? proxy : GetHWnd());
}

HIMC AwtComponent::ImmAssociateContext(HIMC himc)
{
    HWND proxy = GetProxyFocusOwner();
    return ::ImmAssociateContext((proxy != NULL) ? proxy : GetHWnd(), himc);
}

HWND AwtComponent::GetProxyFocusOwner()
{
    AwtWindow * window = GetContainer();
    if (window != 0) {
        AwtFrame * owner = window->GetOwningFrameOrDialog();
        if (owner != 0) {
            return owner->GetProxyFocusOwner();
        }
    }

    return (HWND)NULL;
}

/* Call DefWindowProc for the focus proxy, if any */
void AwtComponent::CallProxyDefWindowProc(UINT message, WPARAM wParam,
    LPARAM lParam, LRESULT &retVal, MsgRouting &mr)
{
    if (mr != mrConsume)  {
        HWND proxy = GetProxyFocusOwner();
        if (proxy != NULL) {
            retVal = ComCtl32Util::GetInstance().DefWindowProc(NULL, proxy, message, wParam, lParam);
            mr = mrConsume;
        }
    }
}

MsgRouting AwtComponent::WmCommand(UINT id, HWND hWndChild, UINT notifyCode)
{
    /* Menu/Accelerator */
    if (hWndChild == 0) {
        AwtObject* obj = AwtToolkit::GetInstance().LookupCmdID(id);
        if (obj == NULL) {
            return mrConsume;
        }
        DASSERT(((AwtMenuItem*)obj)->GetID() == id);
        obj->DoCommand();
        return mrConsume;
    }
    /* Child id notification */
    else {
        AwtComponent* child = AwtComponent::GetComponent(hWndChild);
        if (child) {
            child->WmNotify(notifyCode);
        }
    }
    return mrDoDefault;
}

MsgRouting AwtComponent::WmNotify(UINT notifyCode)
{
    return mrDoDefault;
}

MsgRouting AwtComponent::WmCompareItem(UINT ctrlId,
                                       COMPAREITEMSTRUCT &compareInfo,
                                       LRESULT &result)
{
    AwtComponent* child = AwtComponent::GetComponent(compareInfo.hwndItem);
    if (child == this) {
        /* DoCallback("handleItemDelete", */
    }
    else if (child) {
        return child->WmCompareItem(ctrlId, compareInfo, result);
    }
    return mrConsume;
}

MsgRouting AwtComponent::WmDeleteItem(UINT ctrlId,
                                      DELETEITEMSTRUCT &deleteInfo)
{
    /*
     * Workaround for NT 4.0 bug -- if SetWindowPos is called on a AwtList
     * window, a WM_DELETEITEM message is sent to its parent with a window
     * handle of one of the list's child windows.  The property lookup
     * succeeds, but the HWNDs don't match.
     */
    if (deleteInfo.hwndItem == NULL) {
        return mrConsume;
    }
    AwtComponent* child = (AwtComponent *)AwtComponent::GetComponent(deleteInfo.hwndItem);

    if (child && child->GetHWnd() != deleteInfo.hwndItem) {
        return mrConsume;
    }

    if (child == this) {
        /*DoCallback("handleItemDelete", */
    }
    else if (child) {
        return child->WmDeleteItem(ctrlId, deleteInfo);
    }
    return mrConsume;
}

MsgRouting AwtComponent::WmDrawItem(UINT ctrlId, DRAWITEMSTRUCT &drawInfo)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (drawInfo.CtlType == ODT_MENU) {
        if (drawInfo.itemData != 0) {
            AwtMenu* menu = (AwtMenu*)(drawInfo.itemData);
            menu->DrawItem(drawInfo);
        }
    } else {
        return OwnerDrawItem(ctrlId, drawInfo);
    }
    return mrConsume;
}

MsgRouting AwtComponent::WmMeasureItem(UINT ctrlId,
                                       MEASUREITEMSTRUCT &measureInfo)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (measureInfo.CtlType == ODT_MENU) {
        if (measureInfo.itemData != 0) {
            AwtMenu* menu = (AwtMenu*)(measureInfo.itemData);
            HDC hDC = ::GetDC(GetHWnd());
            /* menu->MeasureItem(env, hDC, measureInfo); */
            menu->MeasureItem(hDC, measureInfo);
            ::ReleaseDC(GetHWnd(), hDC);
        }
    } else {
        return OwnerMeasureItem(ctrlId, measureInfo);
    }
    return mrConsume;
}

MsgRouting AwtComponent::OwnerDrawItem(UINT ctrlId,
    DRAWITEMSTRUCT &drawInfo)
{
    AwtComponent* child = AwtComponent::GetComponent(drawInfo.hwndItem);
    if (child == this) {
        /* DoCallback("handleItemDelete", */
    } else if (child != NULL) {
        return child->WmDrawItem(ctrlId, drawInfo);
    }
    return mrConsume;
}

MsgRouting AwtComponent::OwnerMeasureItem(UINT ctrlId,
    MEASUREITEMSTRUCT &measureInfo)
{
    HWND  hChild = ::GetDlgItem(GetHWnd(), measureInfo.CtlID);
    AwtComponent* child = AwtComponent::GetComponent(hChild);
    /*
     * If the parent cannot find the child's instance from its handle,
     * maybe the child is in its creation.  So the child must be searched
     * from the list linked before the child's creation.
     */
    if (child == NULL) {
        child = SearchChild((UINT)ctrlId);
    }

    if (child == this) {
    /* DoCallback("handleItemDelete",  */
    }
    else if (child) {
        return child->WmMeasureItem(ctrlId, measureInfo);
    }
    return mrConsume;
}

/* for WmDrawItem method of Label, Button and Checkbox */
void AwtComponent::DrawWindowText(HDC hDC, jobject font, jstring text,
                                  int x, int y)
{
    int nOldBkMode = ::SetBkMode(hDC,TRANSPARENT);
    DASSERT(nOldBkMode != 0);
    AwtFont::drawMFString(hDC, font, text, x, y, GetCodePage());
    VERIFY(::SetBkMode(hDC,nOldBkMode));
}

/*
 * Draw text in gray (the color being set to COLOR_GRAYTEXT) when the
 * component is disabled.  Used only for label, checkbox and button in
 * OWNER_DRAW.  It draws the text in emboss.
 */
void AwtComponent::DrawGrayText(HDC hDC, jobject font, jstring text,
                                int x, int y)
{
    ::SetTextColor(hDC, ::GetSysColor(COLOR_BTNHILIGHT));
    AwtComponent::DrawWindowText(hDC, font, text, x+1, y+1);
    ::SetTextColor(hDC, ::GetSysColor(COLOR_BTNSHADOW));
    AwtComponent::DrawWindowText(hDC, font, text, x, y);
}

/* for WmMeasureItem method of List and Choice */
jstring AwtComponent::GetItemString(JNIEnv *env, jobject target, jint index)
{
    jstring str = (jstring)JNU_CallMethodByName(env, NULL, target, "getItemImpl",
                                                "(I)Ljava/lang/String;",
                                                index).l;
    DASSERT(!safe_ExceptionOccurred(env));
    return str;
}

/* for WmMeasureItem method of List and Choice */
void AwtComponent::MeasureListItem(JNIEnv *env,
                                   MEASUREITEMSTRUCT &measureInfo)
{
    if (env->EnsureLocalCapacity(1) < 0) {
        return;
    }
    jobject dimension = PreferredItemSize(env);
    DASSERT(dimension);
    measureInfo.itemWidth =
      env->GetIntField(dimension, AwtDimension::widthID);
    measureInfo.itemHeight =
      env->GetIntField(dimension, AwtDimension::heightID);
    env->DeleteLocalRef(dimension);
}

/* for WmDrawItem method of List and Choice */
void AwtComponent::DrawListItem(JNIEnv *env, DRAWITEMSTRUCT &drawInfo)
{
    if (env->EnsureLocalCapacity(3) < 0) {
        return;
    }
    jobject peer = GetPeer(env);
    jobject target = env->GetObjectField(peer, AwtObject::targetID);

    HDC hDC = drawInfo.hDC;
    RECT rect = drawInfo.rcItem;

    BOOL bEnabled = isEnabled();
    BOOL unfocusableChoice = (drawInfo.itemState & ODS_COMBOBOXEDIT) && !IsFocusable();
    DWORD crBack, crText;
    if (drawInfo.itemState & ODS_SELECTED){
        /* Set background and text colors for selected item */
        crBack = ::GetSysColor (COLOR_HIGHLIGHT);
        crText = ::GetSysColor (COLOR_HIGHLIGHTTEXT);
    } else {
        /* Set background and text colors for unselected item */
        crBack = GetBackgroundColor();
        crText = bEnabled ? GetColor() : ::GetSysColor(COLOR_GRAYTEXT);
    }
    if (unfocusableChoice) {
        //6190728. Shouldn't draw selection field (edit control) of an owner-drawn combo box.
        crBack = GetBackgroundColor();
        crText = bEnabled ? GetColor() : ::GetSysColor(COLOR_GRAYTEXT);
    }

    /* Fill item rectangle with background color */
    HBRUSH hbrBack = ::CreateSolidBrush (crBack);
    DASSERT(hbrBack);
    /* 6190728. Shouldn't draw any kind of rectangle around selection field
     * (edit control) of an owner-drawn combo box while unfocusable
     */
    if (!unfocusableChoice){
        VERIFY(::FillRect (hDC, &rect, hbrBack));
    }
    VERIFY(::DeleteObject (hbrBack));

    /* Set current background and text colors */
    ::SetBkColor (hDC, crBack);
    ::SetTextColor (hDC, crText);

    /*draw string (with left margin of 1 point) */
    if ((int) (drawInfo.itemID) >= 0) {
            jobject font = GET_FONT(target, peer);
            jstring text = GetItemString(env, target, drawInfo.itemID);
            SIZE size = AwtFont::getMFStringSize(hDC, font, text);
            AwtFont::drawMFString(hDC, font, text,
                                  (GetRTL()) ? rect.right - size.cx - 1
                                             : rect.left + 1,
                                  (rect.top + rect.bottom - size.cy) / 2,
                                  GetCodePage());
            env->DeleteLocalRef(font);
            env->DeleteLocalRef(text);
    }
    if ((drawInfo.itemState & ODS_FOCUS)  &&
        (drawInfo.itemAction & (ODA_FOCUS | ODA_DRAWENTIRE))) {
      if (!unfocusableChoice){
          VERIFY(::DrawFocusRect(hDC, &rect));
      }
    }
    env->DeleteLocalRef(target);
}

/* for MeasureListItem method and WmDrawItem method of Checkbox */
jint AwtComponent::GetFontHeight(JNIEnv *env)
{
    if (env->EnsureLocalCapacity(4) < 0) {
        return NULL;
    }
    jobject self = GetPeer(env);
    jobject target = env->GetObjectField(self, AwtObject::targetID);

    jobject font = GET_FONT(target, self);
    jobject toolkit = env->CallObjectMethod(target,
                                            AwtComponent::getToolkitMID);

    DASSERT(!safe_ExceptionOccurred(env));

    jobject fontMetrics =
        env->CallObjectMethod(toolkit, AwtToolkit::getFontMetricsMID, font);

    DASSERT(!safe_ExceptionOccurred(env));

    jint height = env->CallIntMethod(fontMetrics, AwtFont::getHeightMID);
    DASSERT(!safe_ExceptionOccurred(env));

    env->DeleteLocalRef(target);
    env->DeleteLocalRef(font);
    env->DeleteLocalRef(toolkit);
    env->DeleteLocalRef(fontMetrics);

    return height;
}

// If you override WmPrint, make sure to save a copy of the DC on the GDI
// stack to be restored in WmPrintClient. Windows mangles the DC in
// ::DefWindowProc.
MsgRouting AwtComponent::WmPrint(HDC hDC, LPARAM flags)
{
    /*
     * DefWindowProc for WM_PRINT changes DC parameters, so we have
     * to restore it ourselves. Otherwise it will cause problems
     * when several components are printed to the same DC.
     */
    int nOriginalDC = ::SaveDC(hDC);
    DASSERT(nOriginalDC != 0);

    if (flags & PRF_NONCLIENT) {

        VERIFY(::SaveDC(hDC));

        DefWindowProc(WM_PRINT, (WPARAM)hDC,
                      (flags & (PRF_NONCLIENT
                                | PRF_CHECKVISIBLE | PRF_ERASEBKGND)));

        VERIFY(::RestoreDC(hDC, -1));

        // Special case for components with a sunken border. Windows does not
        // print the border correctly on PCL printers, so we have to do it ourselves.
        if (IS_WIN4X && (GetStyleEx() & WS_EX_CLIENTEDGE)) {
            RECT r;
            VERIFY(::GetWindowRect(GetHWnd(), &r));
            VERIFY(::OffsetRect(&r, -r.left, -r.top));
            VERIFY(::DrawEdge(hDC, &r, EDGE_SUNKEN, BF_RECT));
        }
    }

    if (flags & PRF_CLIENT) {

        /*
         * Special case for components with a sunken border.
         * Windows prints a client area without offset to a border width.
         * We will first print the non-client area with the original offset,
         * then the client area with a corrected offset.
         */
        if (IS_WIN4X && (GetStyleEx() & WS_EX_CLIENTEDGE)) {

            int nEdgeWidth = ::GetSystemMetrics(SM_CXEDGE);
            int nEdgeHeight = ::GetSystemMetrics(SM_CYEDGE);

            VERIFY(::OffsetWindowOrgEx(hDC, -nEdgeWidth, -nEdgeHeight, NULL));

            // Save a copy of the DC for WmPrintClient
            VERIFY(::SaveDC(hDC));

            DefWindowProc(WM_PRINT, (WPARAM) hDC,
                          (flags & (PRF_CLIENT
                                    | PRF_CHECKVISIBLE | PRF_ERASEBKGND)));

            VERIFY(::OffsetWindowOrgEx(hDC, nEdgeWidth, nEdgeHeight, NULL));

        } else {

            // Save a copy of the DC for WmPrintClient
            VERIFY(::SaveDC(hDC));
            DefWindowProc(WM_PRINT, (WPARAM) hDC,
                          (flags & (PRF_CLIENT
                                    | PRF_CHECKVISIBLE | PRF_ERASEBKGND)));
        }
    }

    if (flags & (PRF_CHILDREN | PRF_OWNED)) {
        DefWindowProc(WM_PRINT, (WPARAM) hDC,
                      (flags & ~PRF_CLIENT & ~PRF_NONCLIENT));
    }

    VERIFY(::RestoreDC(hDC, nOriginalDC));

    return mrConsume;
}

// If you override WmPrintClient, make sure to obtain a valid copy of
// the DC from the GDI stack. The copy of the DC should have been placed
// there by WmPrint. Windows mangles the DC in ::DefWindowProc.
MsgRouting AwtComponent::WmPrintClient(HDC hDC, LPARAM)
{
    // obtain valid DC from GDI stack
    ::RestoreDC(hDC, -1);

    return mrDoDefault;
}

MsgRouting AwtComponent::WmNcCalcSize(BOOL fCalcValidRects,
                                      LPNCCALCSIZE_PARAMS lpncsp,
                                      LRESULT &retVal)
{
    return mrDoDefault;
}

MsgRouting AwtComponent::WmNcPaint(HRGN hrgn)
{
    return mrDoDefault;
}

MsgRouting AwtComponent::WmNcHitTest(UINT x, UINT y, LRESULT &retVal)
{
    return mrDoDefault;
}

/**
 * WmQueryNewPalette is called whenever our component is coming to
 * the foreground; this gives us an opportunity to install our
 * custom palette.  If this install actually changes entries in
 * the system palette, then we get a further call to WmPaletteChanged
 * (but note that we only need to realize our palette once).
 */
MsgRouting AwtComponent::WmQueryNewPalette(LRESULT &retVal)
{
    int screen = AwtWin32GraphicsDevice::DeviceIndexForWindow(GetHWnd());
    m_QueryNewPaletteCalled = TRUE;
    HDC hDC = ::GetDC(GetHWnd());
    DASSERT(hDC);
    AwtWin32GraphicsDevice::SelectPalette(hDC, screen);
    AwtWin32GraphicsDevice::RealizePalette(hDC, screen);
    ::ReleaseDC(GetHWnd(), hDC);
    // We must realize the palettes of all of our DC's
    // There is sometimes a problem where the realization of
    // our temporary hDC here does not actually do what
    // we want.  Not clear why, but presumably fallout from
    // our use of several simultaneous hDC's.
    activeDCList.RealizePalettes(screen);
    // Do not invalidate here; if the palette
    // has not changed we will get an extra repaint
    retVal = TRUE;

    return mrDoDefault;
}

/**
 * We should not need to track this event since we handle our
 * palette management effectively in the WmQueryNewPalette and
 * WmPaletteChanged methods.  However, there seems to be a bug
 * on some win32 systems (e.g., NT4) whereby the palette
 * immediately after a displayChange is not yet updated to its
 * final post-display-change values (hence we adjust our palette
 * using the wrong system palette entries), then the palette is
 * updated, but a WM_PALETTECHANGED message is never sent.
 * By tracking the ISCHANGING message as well (and by tracking
 * displayChange events in the AwtToolkit object), we can account
 * for this error by forcing our WmPaletteChanged method to be
 * called and thereby realizing our logical palette and updating
 * our dynamic colorModel object.
 */
MsgRouting AwtComponent::WmPaletteIsChanging(HWND hwndPalChg)
{
    if (AwtToolkit::GetInstance().HasDisplayChanged()) {
        WmPaletteChanged(hwndPalChg);
        AwtToolkit::GetInstance().ResetDisplayChanged();
    }
    return mrDoDefault;
}

MsgRouting AwtComponent::WmPaletteChanged(HWND hwndPalChg)
{
    // We need to re-realize our palette here (unless we're the one
    // that was realizing it in the first place).  That will let us match the
    // remaining colors in the system palette as best we can.  We always
    // invalidate because the palette will have changed when we receive this
    // message.

    int screen = AwtWin32GraphicsDevice::DeviceIndexForWindow(GetHWnd());
    if (hwndPalChg != GetHWnd()) {
        HDC hDC = ::GetDC(GetHWnd());
        DASSERT(hDC);
        AwtWin32GraphicsDevice::SelectPalette(hDC, screen);
        AwtWin32GraphicsDevice::RealizePalette(hDC, screen);
        ::ReleaseDC(GetHWnd(), hDC);
        // We must realize the palettes of all of our DC's
        activeDCList.RealizePalettes(screen);
    }
    if (AwtWin32GraphicsDevice::UpdateSystemPalette(screen)) {
        AwtWin32GraphicsDevice::UpdateDynamicColorModel(screen);
    }
    Invalidate(NULL);
    return mrDoDefault;
}

MsgRouting AwtComponent::WmStyleChanged(int wStyleType, LPSTYLESTRUCT lpss)
{
    DASSERT(!IsBadReadPtr(lpss, sizeof(STYLESTRUCT)));
    return mrDoDefault;
}

MsgRouting AwtComponent::WmSettingChange(UINT wFlag, LPCTSTR pszSection)
{
    DASSERT(!IsBadStringPtr(pszSection, 20));
    DTRACE_PRINTLN2("WM_SETTINGCHANGE: wFlag=%d pszSection=%s", (int)wFlag, pszSection);
    return mrDoDefault;
}

HDC AwtComponent::GetDCFromComponent()
{
    GetDCReturnStruct *hdcStruct =
        (GetDCReturnStruct*)SendMessage(WM_AWT_GETDC);
    HDC hdc;
    if (hdcStruct) {
        if (hdcStruct->gdiLimitReached) {
            if (jvm != NULL) {
                JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
                if (env != NULL && !safe_ExceptionOccurred(env)) {
                    JNU_ThrowByName(env, "java/awt/AWTError",
                        "HDC creation failure - " \
                        "exceeded maximum GDI resources");
                }
            }
        }
        hdc = hdcStruct->hDC;
        delete hdcStruct;
    } else {
        hdc = NULL;
    }
    return hdc;
}

jintArray AwtComponent::CreatePrintedPixels(SIZE &loc, SIZE &size) {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (!::IsWindowVisible(GetHWnd())) {
        return NULL;
    }

    HDC hdc = GetDCFromComponent();
    if (!hdc) {
        return NULL;
    }
    HDC hMemoryDC = ::CreateCompatibleDC(hdc);
    HBITMAP hBitmap = ::CreateCompatibleBitmap(hdc, size.cx, size.cy);
    HBITMAP hOldBitmap = (HBITMAP)::SelectObject(hMemoryDC, hBitmap);
    SendMessage(WM_AWT_RELEASEDC, (WPARAM)hdc);

    RECT eraseR = { 0, 0, size.cx, size.cy };
    VERIFY(::FillRect(hMemoryDC, &eraseR, GetBackgroundBrush()));

    VERIFY(::SetWindowOrgEx(hMemoryDC, loc.cx, loc.cy, NULL));

    // Don't bother with PRF_CHECKVISIBLE because we called IsWindowVisible
    // above.
    SendMessage(WM_PRINT, (WPARAM)hMemoryDC, PRF_CLIENT | PRF_NONCLIENT);

    ::SelectObject(hMemoryDC, hOldBitmap);

    BITMAPINFO bmi;
    memset(&bmi, 0, sizeof(BITMAPINFO));
    bmi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bmi.bmiHeader.biWidth = size.cx;
    bmi.bmiHeader.biHeight = -size.cy;
    bmi.bmiHeader.biPlanes = 1;
    bmi.bmiHeader.biBitCount = 32;
    bmi.bmiHeader.biCompression = BI_RGB;

    jobject localPixelArray = env->NewIntArray(size.cx * size.cy);
    jintArray pixelArray = NULL;
    if (localPixelArray != NULL) {
        pixelArray = (jintArray)env->NewGlobalRef(localPixelArray);
        env->DeleteLocalRef(localPixelArray); localPixelArray = NULL;

        jboolean isCopy;
        jint *pixels = env->GetIntArrayElements(pixelArray, &isCopy);

        ::GetDIBits(hMemoryDC, hBitmap, 0, size.cy, (LPVOID)pixels, &bmi,
                    DIB_RGB_COLORS);

        env->ReleaseIntArrayElements(pixelArray, pixels, 0);
    }

    VERIFY(::DeleteObject(hBitmap));
    VERIFY(::DeleteDC(hMemoryDC));

    return pixelArray;
}

BOOL AwtComponent::WmDDCreateSurface(Win32SDOps* wsdo) {
    return DDCreateSurface(wsdo);
}

// This method is fully implemented in AwtWindow
MsgRouting AwtComponent::WmDDEnterFullScreen(HMONITOR monitor) {
    DASSERT(FALSE);
    return mrDoDefault;
}

// This method is fully implemented in AwtWindow
MsgRouting AwtComponent::WmDDExitFullScreen(HMONITOR monitor) {
    DASSERT(FALSE);
    return mrDoDefault;
}

MsgRouting AwtComponent::WmDDSetDisplayMode(HMONITOR monitor,
    DDrawDisplayMode* pDisplayMode) {

    DDSetDisplayMode(monitor, *pDisplayMode);

    delete pDisplayMode;
    return mrDoDefault;
}

void *
AwtComponent::GetNativeFocusOwner() {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    AwtComponent *comp =
        AwtComponent::GetComponent(AwtComponent::sm_focusOwner);
    return (comp != NULL) ? comp->GetTargetAsGlobalRef(env) : NULL;
}
void *
AwtComponent::GetNativeFocusedWindow() {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    AwtComponent *comp =
        AwtComponent::GetComponent(AwtComponent::sm_focusedWindow);
    return (comp != NULL) ? comp->GetTargetAsGlobalRef(env) : NULL;
}
void
AwtComponent::ClearGlobalFocusOwner() {
    if (AwtComponent::sm_focusOwner != NULL) {
        ::SetFocus(NULL);
    }
}

AwtComponent* AwtComponent::SearchChild(UINT id) {
    ChildListItem* child;
    for (child = m_childList; child != NULL;child = child->m_next) {
        if (child->m_ID == id)
            return child->m_Component;
    }
    /*
     * DASSERT(FALSE);
     * This should not be happend if all children are recorded
     */
    return NULL;        /* make compiler happy */
}

void AwtComponent::RemoveChild(UINT id) {
    ChildListItem* child = m_childList;
    ChildListItem* lastChild = NULL;
    while (child != NULL) {
        if (child->m_ID == id) {
            if (lastChild == NULL) {
                m_childList = child->m_next;
            } else {
                lastChild->m_next = child->m_next;
            }
            child->m_next = NULL;
            DASSERT(child != NULL);
            delete child;
            return;
        }
        lastChild = child;
        child = child->m_next;
    }
}

void AwtComponent::SendKeyEvent(jint id, jlong when, jint raw, jint cooked,
                                jint modifiers, jint keyLocation, MSG *pMsg)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    CriticalSection::Lock l(GetLock());
    if (GetPeer(env) == NULL) {
        /* event received during termination. */
        return;
    }

    static jclass keyEventCls;
    if (keyEventCls == NULL) {
        jclass keyEventClsLocal = env->FindClass("java/awt/event/KeyEvent");
        DASSERT(keyEventClsLocal);
        if (keyEventClsLocal == NULL) {
            /* exception already thrown */
            return;
        }
        keyEventCls = (jclass)env->NewGlobalRef(keyEventClsLocal);
        env->DeleteLocalRef(keyEventClsLocal);
    }

    static jmethodID keyEventConst;
    if (keyEventConst == NULL) {
        keyEventConst =  env->GetMethodID(keyEventCls, "<init>",
                                          "(Ljava/awt/Component;IJIICI)V");
        DASSERT(keyEventConst);
    }
    if (env->EnsureLocalCapacity(2) < 0) {
        return;
    }
    jobject target = GetTarget(env);
    jobject keyEvent = env->NewObject(keyEventCls, keyEventConst, target,
                                      id, when, modifiers, raw, cooked,
                                      keyLocation);
    if (safe_ExceptionOccurred(env)) env->ExceptionDescribe();
    DASSERT(!safe_ExceptionOccurred(env));
    DASSERT(keyEvent != NULL);
    if (pMsg != NULL) {
        AwtAWTEvent::saveMSG(env, pMsg, keyEvent);
    }
    SendEvent(keyEvent);

    env->DeleteLocalRef(keyEvent);
    env->DeleteLocalRef(target);
}

void
AwtComponent::SendKeyEventToFocusOwner(jint id, jlong when,
                                       jint raw, jint cooked,
                                       jint modifiers, jint keyLocation,
                                       MSG *msg)
{
    /*
     * if focus owner is null, but focused window isn't
     * we will send key event to focused window
     */
    HWND hwndTarget = ((sm_focusOwner != NULL) ? sm_focusOwner : sm_focusedWindow);

    if (hwndTarget == GetHWnd()) {
        SendKeyEvent(id, when, raw, cooked, modifiers, keyLocation, msg);
    } else {
        AwtComponent *target = NULL;
        if (hwndTarget != NULL) {
            target = AwtComponent::GetComponent(hwndTarget);
            if (target == NULL) {
                target = this;
            }
        }
        if (target != NULL) {
            target->SendKeyEvent(id, when, raw, cooked, modifiers,
              keyLocation, msg);
        }
    }
}

void AwtComponent::SetDragCapture(UINT flags)
{
    // don't want to interfere with other controls
    if (::GetCapture() == NULL) {
        ::SetCapture(GetHWnd());
    }
}

void AwtComponent::ReleaseDragCapture(UINT flags)
{
    if ((::GetCapture() == GetHWnd()) && ((flags & ALL_MK_BUTTONS) == 0)) {
        // user has released all buttons, so release the capture
        ::ReleaseCapture();
    }
}

void AwtComponent::SendMouseEvent(jint id, jlong when, jint x, jint y,
                                  jint modifiers, jint clickCount,
                                  jboolean popupTrigger, jint button,
                                  MSG *pMsg)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    CriticalSection::Lock l(GetLock());
    if (GetPeer(env) == NULL) {
        /* event received during termination. */
        return;
    }

    static jclass mouseEventCls;
    if (mouseEventCls == NULL) {
        jclass mouseEventClsLocal =
            env->FindClass("java/awt/event/MouseEvent");
        if (!mouseEventClsLocal) {
            /* exception already thrown */
            return;
        }
        mouseEventCls = (jclass)env->NewGlobalRef(mouseEventClsLocal);
        env->DeleteLocalRef(mouseEventClsLocal);
    }
    RECT insets;
    GetInsets(&insets);

    static jmethodID mouseEventConst;
    if (mouseEventConst == NULL) {
        mouseEventConst =
            env->GetMethodID(mouseEventCls, "<init>",
                 "(Ljava/awt/Component;IJIIIIIIZI)V");
        DASSERT(mouseEventConst);
    }
    if (env->EnsureLocalCapacity(2) < 0) {
        return;
    }
    jobject target = GetTarget(env);
    DWORD curMousePos = ::GetMessagePos();
    int xAbs = GET_X_LPARAM(curMousePos);
    int yAbs = GET_Y_LPARAM(curMousePos);
    jobject mouseEvent = env->NewObject(mouseEventCls, mouseEventConst,
                                        target,
                                        id, when, modifiers,
                                        x+insets.left, y+insets.top,
                    xAbs, yAbs,
                                        clickCount, popupTrigger, button);

    if (safe_ExceptionOccurred(env)) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

    DASSERT(mouseEvent != NULL);
    if (pMsg != 0) {
        AwtAWTEvent::saveMSG(env, pMsg, mouseEvent);
    }
    SendEvent(mouseEvent);

    env->DeleteLocalRef(mouseEvent);
    env->DeleteLocalRef(target);
}

void
AwtComponent::SendMouseWheelEvent(jint id, jlong when, jint x, jint y,
                                  jint modifiers, jint clickCount,
                                  jboolean popupTrigger, jint scrollType,
                                  jint scrollAmount, jint roundedWheelRotation,
                                  jdouble preciseWheelRotation, MSG *pMsg)
{
    /* Code based not so loosely on AwtComponent::SendMouseEvent */
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    CriticalSection::Lock l(GetLock());
    if (GetPeer(env) == NULL) {
        /* event received during termination. */
        return;
    }

    static jclass mouseWheelEventCls;
    if (mouseWheelEventCls == NULL) {
        jclass mouseWheelEventClsLocal =
            env->FindClass("java/awt/event/MouseWheelEvent");
        if (!mouseWheelEventClsLocal) {
            /* exception already thrown */
            return;
        }
        mouseWheelEventCls = (jclass)env->NewGlobalRef(mouseWheelEventClsLocal);
        env->DeleteLocalRef(mouseWheelEventClsLocal);
    }
    RECT insets;
    GetInsets(&insets);

    static jmethodID mouseWheelEventConst;
    if (mouseWheelEventConst == NULL) {
        mouseWheelEventConst =
            env->GetMethodID(mouseWheelEventCls, "<init>",
                           "(Ljava/awt/Component;IJIIIIIIZIIID)V");
        DASSERT(mouseWheelEventConst);
    }
    if (env->EnsureLocalCapacity(2) < 0) {
        return;
    }
    jobject target = GetTarget(env);
    DTRACE_PRINTLN("creating MWE in JNI");

    jobject mouseWheelEvent = env->NewObject(mouseWheelEventCls,
                                             mouseWheelEventConst,
                                             target,
                                             id, when, modifiers,
                                             x+insets.left, y+insets.top,
                                             0, 0,
                                             clickCount, popupTrigger,
                                             scrollType, scrollAmount,
                                             roundedWheelRotation, preciseWheelRotation);
    if (safe_ExceptionOccurred(env)) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    DASSERT(mouseWheelEvent != NULL);
    if (pMsg != NULL) {
        AwtAWTEvent::saveMSG(env, pMsg, mouseWheelEvent);
    }
    SendEvent(mouseWheelEvent);

    env->DeleteLocalRef(mouseWheelEvent);
    env->DeleteLocalRef(target);
}

void AwtComponent::SendFocusEvent(jint id, HWND opposite)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    CriticalSection::Lock l(GetLock());
    if (GetPeer(env) == NULL) {
        /* event received during termination. */
        return;
    }

    static jclass focusEventCls;
    if (focusEventCls == NULL) {
        jclass focusEventClsLocal
            = env->FindClass("java/awt/event/FocusEvent");
        DASSERT(focusEventClsLocal);
        if (focusEventClsLocal == NULL) {
            /* exception already thrown */
            return;
        }
        focusEventCls = (jclass)env->NewGlobalRef(focusEventClsLocal);
        env->DeleteLocalRef(focusEventClsLocal);
    }

    static jmethodID focusEventConst;
    if (focusEventConst == NULL) {
        focusEventConst =
            env->GetMethodID(focusEventCls, "<init>",
                             "(Ljava/awt/Component;IZLjava/awt/Component;)V");
        DASSERT(focusEventConst);
    }

    static jclass sequencedEventCls;
    if (sequencedEventCls == NULL) {
        jclass sequencedEventClsLocal =
            env->FindClass("java/awt/SequencedEvent");
        DASSERT(sequencedEventClsLocal);
        if (sequencedEventClsLocal == NULL) {
            /* exception already thrown */
            return;
        }
        sequencedEventCls =
            (jclass)env->NewGlobalRef(sequencedEventClsLocal);
        env->DeleteLocalRef(sequencedEventClsLocal);
    }

    static jmethodID sequencedEventConst;
    if (sequencedEventConst == NULL) {
        sequencedEventConst =
            env->GetMethodID(sequencedEventCls, "<init>",
                             "(Ljava/awt/AWTEvent;)V");
    }

    if (env->EnsureLocalCapacity(3) < 0) {
        return;
    }

    jobject target = GetTarget(env);
    jobject jOpposite = NULL;
    if (opposite != NULL) {
        AwtComponent *awtOpposite = AwtComponent::GetComponent(opposite);
        if (awtOpposite != NULL) {
            jOpposite = awtOpposite->GetTarget(env);
        }
    }
    jobject focusEvent = env->NewObject(focusEventCls, focusEventConst,
                                        target, id, JNI_FALSE, jOpposite);
    DASSERT(!safe_ExceptionOccurred(env));
    DASSERT(focusEvent != NULL);
    if (jOpposite != NULL) {
        env->DeleteLocalRef(jOpposite); jOpposite = NULL;
    }
    env->DeleteLocalRef(target); target = NULL;

    jobject sequencedEvent = env->NewObject(sequencedEventCls,
                                            sequencedEventConst,
                                            focusEvent);
    DASSERT(!safe_ExceptionOccurred(env));
    DASSERT(sequencedEvent != NULL);
    env->DeleteLocalRef(focusEvent); focusEvent = NULL;

    SendEvent(sequencedEvent);

    env->DeleteLocalRef(sequencedEvent);
}

/*
 * Forward a filtered event directly to the subclassed window.
 * This method is needed so that DefWindowProc is invoked on the
 * component's owning thread.
 */
MsgRouting AwtComponent::HandleEvent(MSG *msg, BOOL)
{
    DefWindowProc(msg->message, msg->wParam, msg->lParam);
    delete msg;
    return mrConsume;
}

/* Post a WM_AWT_HANDLE_EVENT message which invokes HandleEvent
   on the toolkit thread. This method may pre-filter the messages. */
BOOL AwtComponent::PostHandleEventMessage(MSG *msg, BOOL synthetic)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    // We should cut off keyboard events to disabled components
    // to avoid the components responding visually to keystrokes when disabled.
    // we shouldn't cut off WM_SYS* messages as they aren't used for normal activity
    // but to activate menus, close windows, etc
    switch(msg->message) {
        case WM_KEYDOWN:
        case WM_KEYUP:
        case WM_CHAR:
        case WM_DEADCHAR:
            {
                if (!isRecursivelyEnabled()) {
                    goto quit;
                }
                break;
            }
    }
    if (PostMessage(GetHWnd(), WM_AWT_HANDLE_EVENT,
        (WPARAM) synthetic, (LPARAM) msg)) {
            return TRUE;
    } else {
        JNU_ThrowInternalError(env, "Message not posted, native event queue may be full.");
    }
quit:
    delete msg;
    return FALSE;
}

void AwtComponent::SynthesizeKeyMessage(JNIEnv *env, jobject keyEvent)
{
    jint id = (env)->GetIntField(keyEvent, AwtAWTEvent::idID);
    UINT message;
    switch (id) {
      case java_awt_event_KeyEvent_KEY_PRESSED:
          message = WM_KEYDOWN;
          break;
      case java_awt_event_KeyEvent_KEY_RELEASED:
          message = WM_KEYUP;
          break;
      case java_awt_event_KeyEvent_KEY_TYPED:
          message = WM_CHAR;
          break;
      default:
          return;
    }

    /*
     * KeyEvent.modifiers aren't supported -- the Java apppwd must send separate
     * KEY_PRESSED and KEY_RELEASED events for the modifier virtual keys.
     */
    if (id == java_awt_event_KeyEvent_KEY_TYPED) {
        // WM_CHAR message must be posted using WM_AWT_FORWARD_CHAR
        // (for Edit control)
        jchar keyChar = (jchar)
          (env)->GetCharField(keyEvent, AwtKeyEvent::keyCharID);

        // Bugid 4724007.  If it is a Delete character, don't send the fake
        // KEY_TYPED we created back to the native window: Windows doesn't
        // expect a WM_CHAR for Delete in TextFields, so it tries to enter a
        // character after deleting.
        if (keyChar == '\177') { // the Delete character
            return;
        }

        // Disable forwarding WM_CHAR messages to disabled components
        if (isRecursivelyEnabled()) {
            if (!::PostMessage(GetHWnd(), WM_AWT_FORWARD_CHAR,
                MAKEWPARAM(keyChar, TRUE), 0)) {
                JNU_ThrowInternalError(env, "Message not posted, native event queue may be full.");
            }
        }
    } else {
        jint keyCode =
          (env)->GetIntField(keyEvent, AwtKeyEvent::keyCodeID);
        UINT key, modifiers;
        AwtComponent::JavaKeyToWindowsKey(keyCode, &key, &modifiers);
        MSG* msg = CreateMessage(message, key, 0);
        PostHandleEventMessage(msg, TRUE);
    }
}

void AwtComponent::SynthesizeMouseMessage(JNIEnv *env, jobject mouseEvent)
{
    /*    DebugBreak(); */
    jint button = (env)->GetIntField(mouseEvent, AwtMouseEvent::buttonID);
    jint modifiers = (env)->GetIntField(mouseEvent, AwtInputEvent::modifiersID);

    WPARAM wParam = 0;
    WORD wLow = 0;
    jint wheelAmt = 0;
    jint id = (env)->GetIntField(mouseEvent, AwtAWTEvent::idID);
    UINT message;
    switch (id) {
      case java_awt_event_MouseEvent_MOUSE_PRESSED: {
          switch (button) {
            case java_awt_event_MouseEvent_BUTTON1:
                message = WM_LBUTTONDOWN; break;
            case java_awt_event_MouseEvent_BUTTON3:
                message = WM_MBUTTONDOWN; break;
            case java_awt_event_MouseEvent_BUTTON2:
                message = WM_RBUTTONDOWN; break;
          }
          break;
      }
      case java_awt_event_MouseEvent_MOUSE_RELEASED: {
          switch (button) {
            case java_awt_event_MouseEvent_BUTTON1:
                message = WM_LBUTTONUP; break;
            case java_awt_event_MouseEvent_BUTTON3:
                message = WM_MBUTTONUP; break;
            case java_awt_event_MouseEvent_BUTTON2:
                message = WM_RBUTTONUP; break;
          }
          break;
      }
      case java_awt_event_MouseEvent_MOUSE_MOVED:
          /* MOUSE_DRAGGED events must first have sent a MOUSE_PRESSED event. */
      case java_awt_event_MouseEvent_MOUSE_DRAGGED:
          message = WM_MOUSEMOVE;
          break;
      case java_awt_event_MouseEvent_MOUSE_WHEEL:
          if (modifiers & java_awt_event_InputEvent_CTRL_DOWN_MASK) {
              wLow |= MK_CONTROL;
          }
          if (modifiers & java_awt_event_InputEvent_SHIFT_DOWN_MASK) {
              wLow |= MK_SHIFT;
          }
          if (modifiers & java_awt_event_InputEvent_BUTTON1_DOWN_MASK) {
              wLow |= MK_LBUTTON;
          }
          if (modifiers & java_awt_event_InputEvent_BUTTON2_DOWN_MASK) {
              wLow |= MK_RBUTTON;
          }
          if (modifiers & java_awt_event_InputEvent_BUTTON3_DOWN_MASK) {
              wLow |= MK_MBUTTON;
          }


          wheelAmt = (jint)JNU_CallMethodByName(env,
                                               NULL,
                                               mouseEvent,
                                               "getWheelRotation",
                                               "()I").i;
          DASSERT(!safe_ExceptionOccurred(env));
          //DASSERT(wheelAmt);
          DTRACE_PRINTLN1("wheelAmt = %i\n", wheelAmt);

          // convert Java wheel amount value to Win32
          wheelAmt *= -1 * WHEEL_DELTA;

          if (IS_WIN95 && !IS_WIN98) {
              // 95 doesn't understand WM_MOUSEWHEEL, so plug in value of
              // mouse wheel event on 95
              DTRACE_PRINTLN("awt_C::synthmm - 95 case");
              DASSERT(Wheel95GetMsg() != NULL);
              message = Wheel95GetMsg();
              wParam = wheelAmt;
          }
          else {
              message = WM_MOUSEWHEEL;
              wParam = MAKEWPARAM(wLow, wheelAmt);
          }

          break;
      default:
          return;
    }
    jint x = (env)->GetIntField(mouseEvent, AwtMouseEvent::xID);
    jint y = (env)->GetIntField(mouseEvent, AwtMouseEvent::yID);
    MSG* msg = CreateMessage(message, wParam, MAKELPARAM(x, y), x, y);
    // If the window is not focusable but if this is a focusing
    // message we should skip it then and perform our own actions.
    if (((AwtWindow*)GetContainer())->IsFocusableWindow() || !ActMouseMessage(msg)) {
        PostHandleEventMessage(msg, TRUE);
    } else {
        delete msg;
    }
}

BOOL AwtComponent::InheritsNativeMouseWheelBehavior() {return false;}

void AwtComponent::Invalidate(RECT* r)
{
    ::InvalidateRect(GetHWnd(), r, FALSE);
}

void AwtComponent::BeginValidate()
{
    DASSERT(m_validationNestCount >= 0 &&
           m_validationNestCount < 1000); // sanity check

    if (m_validationNestCount == 0) {
    // begin deferred window positioning if we're not inside
    // another Begin/EndValidate pair
        DASSERT(m_hdwp == NULL);
        m_hdwp = ::BeginDeferWindowPos(32);
    }

    m_validationNestCount++;
}

void AwtComponent::EndValidate()
{
    DASSERT(m_validationNestCount > 0 &&
           m_validationNestCount < 1000); // sanity check
    DASSERT(m_hdwp != NULL);

    m_validationNestCount--;
    if (m_validationNestCount == 0) {
    // if this call to EndValidate is not nested inside another
    // Begin/EndValidate pair, end deferred window positioning
        ::EndDeferWindowPos(m_hdwp);
        m_hdwp = NULL;
    }
}

/**
 * HWND, AwtComponent and Java Peer interaction
 */

/*
 *Link the C++, Java peer, and HWNDs together.
 */
void AwtComponent::LinkObjects(JNIEnv *env, jobject peer)
{
    /*
     * Bind all three objects together thru this C++ object, two-way to each:
     *     JavaPeer <-> C++ <-> HWND
     *
     * C++ -> JavaPeer
     */
    if (m_peerObject == NULL) {
        // This may have already been set up by CreateHWnd
        // And we don't want to create two references so we
        // will leave the prior one alone
        m_peerObject = env->NewGlobalRef(peer);
    }
    /* JavaPeer -> HWND */
    env->SetLongField(peer, AwtComponent::hwndID, reinterpret_cast<jlong>(m_hwnd));

    /* JavaPeer -> C++ */
    JNI_SET_PDATA(peer, this);

    /* HWND -> C++ */
    SetComponentInHWND();
}

/* Cleanup above linking */
void AwtComponent::UnlinkObjects()
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (m_peerObject) {
        env->SetLongField(m_peerObject, AwtComponent::hwndID, 0);
        JNI_SET_PDATA(m_peerObject, static_cast<PDATA>(NULL));
        JNI_SET_DESTROYED(m_peerObject);
        env->DeleteGlobalRef(m_peerObject);
        m_peerObject = NULL;
    }
}

void AwtComponent::Enable(BOOL bEnable)
{
    sm_suppressFocusAndActivation = TRUE;

    if (bEnable && IsTopLevel()) {
        // we should not enable blocked toplevels
        bEnable = !::IsWindow(AwtWindow::GetModalBlocker(GetHWnd()));
    }
    ::EnableWindow(GetHWnd(), bEnable);

    sm_suppressFocusAndActivation = FALSE;
    CriticalSection::Lock l(GetLock());
    VerifyState();
}

/* Initialization of MouseWheel support on Windows 95 */
void AwtComponent::Wheel95Init() {
    DASSERT(IS_WIN95 && !IS_WIN98);

    HWND mwHWND = NULL;
    UINT wheelMSG = WM_NULL;
    UINT suppMSG = WM_NULL;
    UINT linesMSG = WM_NULL;
    BOOL wheelActive;
    INT lines;

    mwHWND = HwndMSWheel(&wheelMSG, &suppMSG, &linesMSG, &wheelActive, &lines);
    if (mwHWND != WM_NULL) {
        sm_95WheelMessage = wheelMSG;
        sm_95WheelSupport = suppMSG;
    }
}

/* Win95 only
 * Return the user's preferred number of lines of test to scroll when the
 * mouse wheel is rotated.
 */
UINT AwtComponent::Wheel95GetScrLines() {
    DASSERT(IS_WIN95 && !IS_WIN98);
    DASSERT(sm_95WheelSupport != NULL);

    HWND mwHWND = NULL;
    UINT linesMSG = WM_NULL;
    INT numLines = 3;

    linesMSG = RegisterWindowMessage(MSH_SCROLL_LINES);
    mwHWND = FindWindow(MSH_WHEELMODULE_CLASS, MSH_WHEELMODULE_TITLE);

    if (mwHWND && linesMSG) {
        numLines = (INT)::SendMessage(mwHWND, linesMSG, 0, 0);
    }
    return numLines;
}

/*
 * associate an AwtDropTarget with this AwtComponent
 */

AwtDropTarget* AwtComponent::CreateDropTarget(JNIEnv* env) {
    m_dropTarget = new AwtDropTarget(env, this);
    m_dropTarget->RegisterTarget(TRUE);
    return m_dropTarget;
}

/*
 * disassociate an AwtDropTarget with this AwtComponent
 */

void AwtComponent::DestroyDropTarget() {
    if (m_dropTarget != NULL) {
        m_dropTarget->RegisterTarget(FALSE);
        m_dropTarget->Release();
        m_dropTarget = NULL;
    }
}

/**
 * Special procedure responsible for performing the actions which
 * usually happen with component when mouse buttons are being
 * pressed. It is required in case of non-focusable components - we
 * don't pass mouse messages directly to the windows because otherwise
 * it will try to focus component first which we don't want.  This
 * function receives MSG and should return TRUE if it processed the
 * message and no furhter processing is allowed, FALSE otherwise.
 * Default implementation returns TRUE it is the message on which
 * Windows try to focus the component.  Descendant components write
 * their own implementation of this procedure.
 */
BOOL AwtComponent::ActMouseMessage(MSG * pMsg) {
    if (IsFocusingMessage(pMsg->message)) {
        return TRUE;
    }
    return FALSE;
}

void AwtComponent::_Show(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    AwtComponent *p;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    p = (AwtComponent *)pData;
    if (::IsWindow(p->GetHWnd()))
    {
        p->SendMessage(WM_AWT_COMPONENT_SHOW);
    }
ret:
    env->DeleteGlobalRef(self);
}

void AwtComponent::_Hide(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    AwtComponent *p;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    p = (AwtComponent *)pData;
    if (::IsWindow(p->GetHWnd()))
    {
        p->SendMessage(WM_AWT_COMPONENT_HIDE);
    }
ret:
    env->DeleteGlobalRef(self);
}

void AwtComponent::_Enable(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    AwtComponent *p;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    p = (AwtComponent *)pData;
    if (::IsWindow(p->GetHWnd()))
    {
        p->Enable(TRUE);
    }
ret:
    env->DeleteGlobalRef(self);
}

void AwtComponent::_Disable(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    AwtComponent *p;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    p = (AwtComponent *)pData;
    if (::IsWindow(p->GetHWnd()))
    {
        p->Enable(FALSE);
    }
ret:
    env->DeleteGlobalRef(self);
}

jobject AwtComponent::_GetLocationOnScreen(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    jobject result = NULL;
    AwtComponent *p;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    p = (AwtComponent *)pData;
    if (::IsWindow(p->GetHWnd()))
    {
        RECT rect;
        VERIFY(::GetWindowRect(p->GetHWnd(),&rect));
        result = JNU_NewObjectByName(env, "java/awt/Point", "(II)V",
            rect.left, rect.top);
    }
ret:
    env->DeleteGlobalRef(self);

    if (result != NULL)
    {
        jobject resultGlobalRef = env->NewGlobalRef(result);
        env->DeleteLocalRef(result);
        return resultGlobalRef;
    }
    else
    {
        return NULL;
    }
}

void AwtComponent::_Reshape(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    ReshapeStruct *rs = (ReshapeStruct*)param;
    jobject self = rs->component;
    jint x = rs->x;
    jint y = rs->y;
    jint w = rs->w;
    jint h = rs->h;

    AwtComponent *p;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    p = (AwtComponent *)pData;
    if (::IsWindow(p->GetHWnd()))
    {
        RECT* r = new RECT;
        ::SetRect(r, x, y, x + w, y + h);
        p->SendMessage(WM_AWT_RESHAPE_COMPONENT, CHECK_EMBEDDED, (LPARAM)r);
    }
ret:
    env->DeleteGlobalRef(self);

    delete rs;
}

void AwtComponent::_ReshapeNoCheck(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    ReshapeStruct *rs = (ReshapeStruct*)param;
    jobject self = rs->component;
    jint x = rs->x;
    jint y = rs->y;
    jint w = rs->w;
    jint h = rs->h;

    AwtComponent *p;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    p = (AwtComponent *)pData;
    if (::IsWindow(p->GetHWnd()))
    {
        RECT* r = new RECT;
        ::SetRect(r, x, y, x + w, y + h);
        p->SendMessage(WM_AWT_RESHAPE_COMPONENT, DONT_CHECK_EMBEDDED, (LPARAM)r);
    }
ret:
    env->DeleteGlobalRef(self);

    delete rs;
}

void AwtComponent::_NativeHandleEvent(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    NativeHandleEventStruct *nhes = (NativeHandleEventStruct *)param;
    jobject self = nhes->component;
    jobject event = nhes->event;

    AwtComponent *p;

    PDATA pData;
    JNI_CHECK_NULL_GOTO(self, "peer", ret);
    pData = JNI_GET_PDATA(self);
    if (pData == NULL) {
        env->DeleteGlobalRef(self);
        if (event != NULL) {
            env->DeleteGlobalRef(event);
        }
        delete nhes;
        return;
    }
    JNI_CHECK_NULL_GOTO(event, "null AWTEvent", ret);

    p = (AwtComponent *)pData;
    if (::IsWindow(p->GetHWnd()))
    {
        if (env->EnsureLocalCapacity(1) < 0) {
            env->DeleteGlobalRef(self);
            env->DeleteGlobalRef(event);
            delete nhes;
            return;
        }
        jbyteArray bdata = (jbyteArray)(env)->GetObjectField(event, AwtAWTEvent::bdataID);
        int id = (env)->GetIntField(event, AwtAWTEvent::idID);
        DASSERT(!safe_ExceptionOccurred(env));
        if (bdata != 0) {
            MSG msg;
            (env)->GetByteArrayRegion(bdata, 0, sizeof(MSG), (jbyte *)&msg);
            (env)->DeleteLocalRef(bdata);
            static BOOL keyDownConsumed = FALSE;
            static BOOL bCharChanged = FALSE;
            static WCHAR modifiedChar;
            WCHAR unicodeChar;

            /* Remember if a KEY_PRESSED event is consumed, as an old model
             * program won't consume a subsequent KEY_TYPED event.
             */
            jboolean consumed =
                (env)->GetBooleanField(event, AwtAWTEvent::consumedID);
            DASSERT(!safe_ExceptionOccurred(env));

            if (consumed) {
                keyDownConsumed = (id == java_awt_event_KeyEvent_KEY_PRESSED);
                env->DeleteGlobalRef(self);
                env->DeleteGlobalRef(event);
                delete nhes;
                return;
            }

            /* Consume a KEY_TYPED event if a KEY_PRESSED had been, to support
             * the old model.
             */
            if ((id == java_awt_event_KeyEvent_KEY_TYPED) && keyDownConsumed) {
                keyDownConsumed = FALSE;
                env->DeleteGlobalRef(self);
                env->DeleteGlobalRef(event);
                delete nhes;
                return;
            }

            /* Modify any event parameters, if necessary. */
            if (self && pData &&
                id >= java_awt_event_KeyEvent_KEY_FIRST &&
                id <= java_awt_event_KeyEvent_KEY_LAST) {

                    AwtComponent* p = (AwtComponent*)pData;

                    jint keyCode =
                      (env)->GetIntField(event, AwtKeyEvent::keyCodeID);
                    jchar keyChar =
                      (env)->GetCharField(event, AwtKeyEvent::keyCharID);
                    jint modifiers =
                      (env)->GetIntField(event, AwtInputEvent::modifiersID);

                    DASSERT(!safe_ExceptionOccurred(env));

                /* Check to see whether the keyCode or modifiers were changed
                   on the keyPressed event, and tweak the following keyTyped
                   event (if any) accodingly.  */
                switch (id) {
                case java_awt_event_KeyEvent_KEY_PRESSED:
                {
                    UINT winKey = (UINT)msg.wParam;
                    bCharChanged = FALSE;

                    if (winKey == VK_PROCESSKEY) {
                        // Leave it up to IME
                        break;
                    }

                    if (keyCode != java_awt_event_KeyEvent_VK_UNDEFINED) {
                        UINT newWinKey, ignored;
                        p->JavaKeyToWindowsKey(keyCode, &newWinKey, &ignored, winKey);
                        if (newWinKey != 0) {
                            winKey = newWinKey;
                        }
                    }

                    modifiedChar = p->WindowsKeyToJavaChar(winKey, modifiers, AwtComponent::NONE);
                    bCharChanged = (keyChar != modifiedChar);
                }
                break;

                case java_awt_event_KeyEvent_KEY_RELEASED:
                {
                    keyDownConsumed = FALSE;
                    bCharChanged = FALSE;
                }
                break;

                case java_awt_event_KeyEvent_KEY_TYPED:
                {
                    if (bCharChanged)
                    {
                        unicodeChar = modifiedChar;
                    }
                    else
                    {
                        unicodeChar = keyChar;
                    }
                    bCharChanged = FALSE;

                    // Disable forwarding KEY_TYPED messages to peers of
                    // disabled components
                    if (p->isRecursivelyEnabled()) {
                        // send the character back to the native window for
                        // processing. The WM_AWT_FORWARD_CHAR handler will send
                        // this character to DefWindowProc
                        if (!::PostMessage(p->GetHWnd(), WM_AWT_FORWARD_CHAR,
                            MAKEWPARAM(unicodeChar, FALSE), msg.lParam)) {
                            JNU_ThrowInternalError(env, "Message not posted, native event queue may be full.");
                        }
                    }
                    env->DeleteGlobalRef(self);
                    env->DeleteGlobalRef(event);
                    delete nhes;
                    return;
                }
                break;

                default:
                    break;
                }
            }

            // ignore all InputMethodEvents
            if (self && (pData = JNI_GET_PDATA(self)) &&
                id >= java_awt_event_InputMethodEvent_INPUT_METHOD_FIRST &&
                id <= java_awt_event_InputMethodEvent_INPUT_METHOD_LAST) {
                env->DeleteGlobalRef(self);
                env->DeleteGlobalRef(event);
                delete nhes;
                return;
            }

            /* Post the message directly to the subclassed component. */
            if (self && (pData = JNI_GET_PDATA(self))) {
                AwtComponent* p = (AwtComponent*)pData;
                // If the window is not focusable but if this is a focusing
                // message we should skip it then and perform our own actions.
                if (((AwtWindow*)p->GetContainer())->IsFocusableWindow() || !p->ActMouseMessage(&msg)) {
                    // Create copy for local msg
                    MSG* pCopiedMsg = new MSG;
                    memmove(pCopiedMsg, &msg, sizeof(MSG));
                    // Event handler deletes msg
                    p->PostHandleEventMessage(pCopiedMsg, FALSE);
                }
            }
            env->DeleteGlobalRef(self);
            env->DeleteGlobalRef(event);
            delete nhes;
            return;
        }

        /* Forward any valid synthesized events.  Currently only mouse and
         * key events are supported.
         */
        if (self == NULL || (pData = JNI_GET_PDATA(self)) == NULL) {
            env->DeleteGlobalRef(self);
            env->DeleteGlobalRef(event);
            delete nhes;
            return;
        }

        AwtComponent* p = (AwtComponent*)pData;
        if (id >= java_awt_event_KeyEvent_KEY_FIRST &&
            id <= java_awt_event_KeyEvent_KEY_LAST) {
            p->SynthesizeKeyMessage(env, event);
        } else if (id >= java_awt_event_MouseEvent_MOUSE_FIRST &&
                   id <= java_awt_event_MouseEvent_MOUSE_LAST) {
            p->SynthesizeMouseMessage(env, event);
        }
    }

ret:
    if (self != NULL) {
        env->DeleteGlobalRef(self);
    }
    if (event != NULL) {
        env->DeleteGlobalRef(event);
    }

    delete nhes;
}

void AwtComponent::_SetForeground(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetColorStruct *scs = (SetColorStruct *)param;
    jobject self = scs->component;
    jint rgb = scs->rgb;

    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    c = (AwtComponent *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        c->SetColor(PALETTERGB((rgb>>16)&0xff,
                               (rgb>>8)&0xff,
                               (rgb)&0xff));
        c->VerifyState();
    }
ret:
    env->DeleteGlobalRef(self);

    delete scs;
}

void AwtComponent::_SetBackground(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetColorStruct *scs = (SetColorStruct *)param;
    jobject self = scs->component;
    jint rgb = scs->rgb;

    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    c = (AwtComponent *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        c->SetBackgroundColor(PALETTERGB((rgb>>16)&0xff,
                                         (rgb>>8)&0xff,
                                         (rgb)&0xff));
        c->VerifyState();
    }
ret:
    env->DeleteGlobalRef(self);

    delete scs;
}

void AwtComponent::_SetFont(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetFontStruct *sfs = (SetFontStruct *)param;
    jobject self = sfs->component;
    jobject font = sfs->font;

    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    JNI_CHECK_NULL_GOTO(font, "null font", ret);
    c = (AwtComponent *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        AwtFont *awtFont = (AwtFont *)env->GetLongField(font, AwtFont::pDataID);
        if (awtFont == NULL) {
        /*arguments of AwtFont::Create are changed for multifont component */
            awtFont = AwtFont::Create(env, font);
        }
        env->SetLongField(font, AwtFont::pDataID, (jlong)awtFont);

        c->SetFont(awtFont);
    }
ret:
    env->DeleteGlobalRef(self);
    env->DeleteGlobalRef(font);

    delete sfs;
}

jboolean AwtComponent::_RequestFocus(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    RequestFocusStruct *rfs = (RequestFocusStruct *)param;
    jobject self = rfs->component;
    jobject lightweightChild = rfs->lightweightChild;
    jboolean temporary = rfs->temporary;
    jboolean focusedWindowChangeAllowed = rfs->focusedWindowChangeAllowed;
    jlong time = rfs->time;
    jobject cause = rfs->cause;

    jboolean result = JNI_FALSE;
    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_NULL_GOTO(self, "peer", ret);
    pData = JNI_GET_PDATA(self);
    if (pData == NULL) {
        // do nothing just return false
        goto ret;
    }

    c = (AwtComponent *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        WmComponentSetFocusData *data = new WmComponentSetFocusData;
        data->lightweightChild = env->NewGlobalRef(lightweightChild);
        data->temporary = temporary;
        data->focusedWindowChangeAllowed = focusedWindowChangeAllowed;
        data->time = time;
        data->cause = cause;
        result = (jboolean)c->SendMessage(WM_AWT_COMPONENT_SETFOCUS, (WPARAM)data, 0);
        // data and global ref in it are deleted in WmComponentSetFocus
    }
ret:
    env->DeleteGlobalRef(self);
    env->DeleteGlobalRef(lightweightChild);
    env->DeleteGlobalRef(cause);

    delete rfs;

    return result;
}

void AwtComponent::_Start(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    c = (AwtComponent *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        jobject target = c->GetTarget(env);

        /* Disable window if specified -- windows are enabled by default. */
        jboolean enabled = (jboolean)env->GetBooleanField(target,
                                                          AwtComponent::enabledID);
        if (!enabled) {
            ::EnableWindow(c->GetHWnd(), FALSE);
        }

        /* The peer is now ready for callbacks, since this is the last
         * initialization call
         */
        c->EnableCallbacks(TRUE);

        // Fix 4745222: we need to invalidate region since we validated it before initialization.
        ::InvalidateRgn(c->GetHWnd(), NULL, FALSE);

        // Fix 4530093: WM_PAINT after EnableCallbacks
        ::UpdateWindow(c->GetHWnd());

        env->DeleteLocalRef(target);
    }
ret:
    env->DeleteGlobalRef(self);
}

void AwtComponent::_BeginValidate(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (AwtToolkit::IsMainThread()) {
        jobject self = (jobject)param;
        if (self != NULL) {
            PDATA pData = JNI_GET_PDATA(self);
            if (pData) {
                AwtComponent *c = (AwtComponent *)pData;
                if (::IsWindow(c->GetHWnd())) {
                    c->SendMessage(WM_AWT_BEGIN_VALIDATE);
                }
            }
            env->DeleteGlobalRef(self);
        }
    } else {
        AwtToolkit::GetInstance().InvokeFunction(AwtComponent::_BeginValidate, param);
    }
}

void AwtComponent::_EndValidate(void *param)
{
    if (AwtToolkit::IsMainThread()) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        jobject self = (jobject)param;
        if (self != NULL) {
            PDATA pData = JNI_GET_PDATA(self);
            if (pData) {
                AwtComponent *c = (AwtComponent *)pData;
                if (::IsWindow(c->GetHWnd())) {
                    c->SendMessage(WM_AWT_END_VALIDATE);
                }
            }
            env->DeleteGlobalRef(self);
        }
    } else {
        AwtToolkit::GetInstance().InvokeFunction(AwtComponent::_EndValidate, param);
    }
}

void AwtComponent::_UpdateWindow(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (AwtToolkit::IsMainThread()) {
        jobject self = (jobject)param;
        AwtComponent *c = NULL;
        PDATA pData;
        JNI_CHECK_PEER_GOTO(self, ret);
        c = (AwtComponent *)pData;
        if (::IsWindow(c->GetHWnd())) {
            ::UpdateWindow(c->GetHWnd());
        }
ret:
        env->DeleteGlobalRef(self);
    } else {
        AwtToolkit::GetInstance().InvokeFunction(AwtComponent::_UpdateWindow, param);
    }
}

jlong AwtComponent::_AddNativeDropTarget(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    jlong result = 0;
    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    c = (AwtComponent *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        result = (jlong)(c->CreateDropTarget(env));
    }
ret:
    env->DeleteGlobalRef(self);

    return result;
}

void AwtComponent::_RemoveNativeDropTarget(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    c = (AwtComponent *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        c->DestroyDropTarget();
    }
ret:
    env->DeleteGlobalRef(self);
}

jintArray AwtComponent::_CreatePrintedPixels(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    CreatePrintedPixelsStruct *cpps = (CreatePrintedPixelsStruct *)param;
    jobject self = cpps->component;
    jint srcx = cpps->srcx;
    jint srcy = cpps->srcy;
    jint srcw = cpps->srcw;
    jint srch = cpps->srch;

    jintArray result = NULL;
    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    c = (AwtComponent *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        SIZE loc = { srcx, srcy };
        SIZE size = { srcw, srch };

        result = (jintArray)
            c->SendMessage(WM_AWT_CREATE_PRINTED_PIXELS, (WPARAM)&loc,
                          (LPARAM)&size);
    }
ret:
    env->DeleteGlobalRef(self);

    delete cpps;

    if (result != NULL)
    {
        jintArray resultGlobalRef = (jintArray)env->NewGlobalRef(result);
        env->DeleteLocalRef(result);
        return resultGlobalRef;
    }
    else
    {
        return NULL;
    }
}

jboolean AwtComponent::_IsObscured(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    jboolean result = JNI_FALSE;
    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);

    c = (AwtComponent *)pData;

    if (::IsWindow(c->GetHWnd()))
    {
        HWND hWnd = c->GetHWnd();
        HDC hDC = ::GetDC(hWnd);
        RECT clipbox;
        int callresult = ::GetClipBox(hDC, &clipbox);
        switch(callresult) {
            case NULLREGION :
                result = JNI_FALSE;
                break;
            case SIMPLEREGION : {
                RECT windowRect;
                if (!::GetClientRect(hWnd, &windowRect)) {
                    result = JNI_TRUE;
                } else {
                    result  = (jboolean)((clipbox.bottom != windowRect.bottom)
                        || (clipbox.left != windowRect.left)
                        || (clipbox.right != windowRect.right)
                        || (clipbox.top != windowRect.top));
                }
                break;
            }
            case COMPLEXREGION :
            default :
                result = JNI_TRUE;
                break;
        }
        ::ReleaseDC(hWnd, hDC);
    }
ret:
    env->DeleteGlobalRef(self);

    return result;
}

jboolean AwtComponent::_NativeHandlesWheelScrolling(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    jboolean result = JNI_FALSE;
    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    c = (AwtComponent *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        result = (jboolean)c->InheritsNativeMouseWheelBehavior();
    }
ret:
    env->DeleteGlobalRef(self);

    return result;
}

void AwtComponent::SetParent(void * param) {
    if (AwtToolkit::IsMainThread()) {
        AwtComponent** comps = (AwtComponent**)param;
        if ((comps[0] != NULL) && (comps[1] != NULL)) {
            HWND selfWnd = comps[0]->GetHWnd();
            HWND parentWnd = comps[1]->GetHWnd();
            if (::IsWindow(selfWnd) && ::IsWindow(parentWnd)) {
                sm_suppressFocusAndActivation = TRUE;
                ::SetParent(selfWnd, parentWnd);
                sm_suppressFocusAndActivation = FALSE;
            }
        }
        delete[] comps;
    } else {
        AwtToolkit::GetInstance().InvokeFunction(AwtComponent::SetParent, param);
    }
}

void AwtComponent::_SetRectangularShape(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetRectangularShapeStruct *data = (SetRectangularShapeStruct *)param;
    jobject self = data->component;
    jint x1 = data->x1;
    jint x2 = data->x2;
    jint y1 = data->y1;
    jint y2 = data->y2;
    jobject region = data->region;

    AwtComponent *c = NULL;



    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    c = (AwtComponent *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        RGNDATA *pRgnData = NULL;
        RGNDATAHEADER *pRgnHdr;

        /* reserving memory for the worst case */
        size_t worstBufferSize = size_t(((x2 - x1) / 2 + 1) * (y2 - y1));
        pRgnData = (RGNDATA *) safe_Malloc(sizeof(RGNDATAHEADER) +
                sizeof(RECT_T) * worstBufferSize);
        pRgnHdr = (RGNDATAHEADER *) pRgnData;

        pRgnHdr->dwSize = sizeof(RGNDATAHEADER);
        pRgnHdr->iType = RDH_RECTANGLES;
        pRgnHdr->nRgnSize = 0;
        pRgnHdr->rcBound.top = 0;
        pRgnHdr->rcBound.left = 0;
        pRgnHdr->rcBound.bottom = LONG(y2 - y1);
        pRgnHdr->rcBound.right = LONG(x2 - x1);

        RECT_T * pRect = (RECT_T *) (((BYTE *) pRgnData) + sizeof(RGNDATAHEADER));
        pRgnHdr->nCount = RegionToYXBandedRectangles(env, x1, y1, x2, y2, region, &pRect, worstBufferSize);

        HRGN hRgn = ::ExtCreateRegion(NULL,
                sizeof(RGNDATAHEADER) + sizeof(RECT_T) * pRgnHdr->nCount, pRgnData);

        free(pRgnData);

        ::SetWindowRgn(c->GetHWnd(), hRgn, TRUE);
    }

ret:
    env->DeleteGlobalRef(self);
    if (region) {
        env->DeleteGlobalRef(region);
    }

    delete data;
}

void AwtComponent::PostUngrabEvent() {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jobject target = GetTarget(env);
    jobject event = JNU_NewObjectByName(env, "sun/awt/UngrabEvent", "(Ljava/awt/Component;)V",
                                        target);
    if (safe_ExceptionOccurred(env)) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    env->DeleteLocalRef(target);
    if (event != NULL) {
        SendEvent(event);
        env->DeleteLocalRef(event);
    }
}

/************************************************************************
 * Component native methods
 */

extern "C" {

/**
 * This method is called from the WGL pipeline when it needs to retrieve
 * the HWND associated with a ComponentPeer's C++ level object.
 */
HWND
AwtComponent_GetHWnd(JNIEnv *env, jlong pData)
{
    AwtComponent *p = (AwtComponent *)jlong_to_ptr(pData);
    if (p == NULL) {
        return (HWND)0;
    }
    return p->GetHWnd();
}

JNIEXPORT void JNICALL
Java_java_awt_Component_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    /* class ids */
    jclass peerCls = env->FindClass("sun/awt/windows/WComponentPeer");

    DASSERT(peerCls);

    /* field ids */
    AwtComponent::peerID =
      env->GetFieldID(cls, "peer", "Ljava/awt/peer/ComponentPeer;");
    AwtComponent::xID = env->GetFieldID(cls, "x", "I");
    AwtComponent::yID = env->GetFieldID(cls, "y", "I");
    AwtComponent::heightID = env->GetFieldID(cls, "height", "I");
    AwtComponent::widthID = env->GetFieldID(cls, "width", "I");
    AwtComponent::visibleID = env->GetFieldID(cls, "visible", "Z");
    AwtComponent::backgroundID =
        env->GetFieldID(cls, "background", "Ljava/awt/Color;");
    AwtComponent::foregroundID =
        env->GetFieldID(cls, "foreground", "Ljava/awt/Color;");
    AwtComponent::enabledID = env->GetFieldID(cls, "enabled", "Z");
    AwtComponent::parentID = env->GetFieldID(cls, "parent", "Ljava/awt/Container;");
    AwtComponent::graphicsConfigID =
     env->GetFieldID(cls, "graphicsConfig", "Ljava/awt/GraphicsConfiguration;");
    AwtComponent::focusableID = env->GetFieldID(cls, "focusable", "Z");

    AwtComponent::appContextID = env->GetFieldID(cls, "appContext",
                                                 "Lsun/awt/AppContext;");

    AwtComponent::peerGCID = env->GetFieldID(peerCls, "winGraphicsConfig",
                                        "Lsun/awt/Win32GraphicsConfig;");

    AwtComponent::hwndID = env->GetFieldID(peerCls, "hwnd", "J");

    AwtComponent::cursorID = env->GetFieldID(cls, "cursor", "Ljava/awt/Cursor;");

    /* method ids */
    AwtComponent::getFontMID =
        env->GetMethodID(cls, "getFont_NoClientCode", "()Ljava/awt/Font;");
    AwtComponent::getToolkitMID =
        env->GetMethodID(cls, "getToolkitImpl", "()Ljava/awt/Toolkit;");
    AwtComponent::isEnabledMID = env->GetMethodID(cls, "isEnabledImpl", "()Z");
    AwtComponent::getLocationOnScreenMID =
        env->GetMethodID(cls, "getLocationOnScreen_NoTreeLock", "()Ljava/awt/Point;");
    AwtComponent::replaceSurfaceDataMID =
        env->GetMethodID(peerCls, "replaceSurfaceData", "()V");
    AwtComponent::replaceSurfaceDataLaterMID =
        env->GetMethodID(peerCls, "replaceSurfaceDataLater", "()V");

    DASSERT(AwtComponent::xID);
    DASSERT(AwtComponent::yID);
    DASSERT(AwtComponent::heightID);
    DASSERT(AwtComponent::widthID);
    DASSERT(AwtComponent::visibleID);
    DASSERT(AwtComponent::backgroundID);
    DASSERT(AwtComponent::foregroundID);
    DASSERT(AwtComponent::enabledID);
    DASSERT(AwtComponent::parentID);
    DASSERT(AwtComponent::hwndID);

    DASSERT(AwtComponent::getFontMID);
    DASSERT(AwtComponent::getToolkitMID);
    DASSERT(AwtComponent::isEnabledMID);
    DASSERT(AwtComponent::getLocationOnScreenMID);
    DASSERT(AwtComponent::replaceSurfaceDataMID);
    DASSERT(AwtComponent::replaceSurfaceDataLaterMID);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * ComponentPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    pShow
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_pShow(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_Show, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _Show

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    hide
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_hide(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_Hide, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _Hide

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    enable
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_enable(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_Enable, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _Enable

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    disable
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_disable(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_Disable, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _Disable

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    getLocationOnScreen
 * Signature: ()Ljava/awt/Point;
 */
JNIEXPORT jobject JNICALL
Java_sun_awt_windows_WComponentPeer_getLocationOnScreen(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    jobject resultGlobalRef = (jobject)AwtToolkit::GetInstance().SyncCall(
        (void*(*)(void*))AwtComponent::_GetLocationOnScreen, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _GetLocationOnScreen
    if (resultGlobalRef != NULL)
    {
        jobject resultLocalRef = env->NewLocalRef(resultGlobalRef);
        env->DeleteGlobalRef(resultGlobalRef);
        return resultLocalRef;
    }

    return NULL;

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    reshape
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_reshape(JNIEnv *env, jobject self,
                                            jint x, jint y, jint w, jint h)
{
    TRY;

    ReshapeStruct *rs = new ReshapeStruct;
    rs->component = env->NewGlobalRef(self);
    rs->x = x;
    rs->y = y;
    rs->w = w;
    rs->h = h;

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_Reshape, rs);
    // global ref and rs are deleted in _Reshape

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    reshape
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_reshapeNoCheck(JNIEnv *env, jobject self,
                                            jint x, jint y, jint w, jint h)
{
    TRY;

    ReshapeStruct *rs = new ReshapeStruct;
    rs->component = env->NewGlobalRef(self);
    rs->x = x;
    rs->y = y;
    rs->w = w;
    rs->h = h;

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_ReshapeNoCheck, rs);
    // global ref and rs are deleted in _ReshapeNoCheck

    CATCH_BAD_ALLOC;
}


/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    nativeHandleEvent
 * Signature: (Ljava/awt/AWTEvent;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_nativeHandleEvent(JNIEnv *env,
                                                      jobject self,
                                                      jobject event)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);
    jobject eventGlobalRef = env->NewGlobalRef(event);

    NativeHandleEventStruct *nhes = new NativeHandleEventStruct;
    nhes->component = selfGlobalRef;
    nhes->event = eventGlobalRef;

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_NativeHandleEvent, nhes);
    // global refs and nhes are deleted in _NativeHandleEvent

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    _dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer__1dispose(JNIEnv *env, jobject self)
{
    TRY_NO_HANG;

    PDATA pData = JNI_GET_PDATA(self);
    AwtObject::_Dispose(pData);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    _setForeground
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer__1setForeground(JNIEnv *env, jobject self,
                                                    jint rgb)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    SetColorStruct *scs = new SetColorStruct;
    scs->component = selfGlobalRef;
    scs->rgb = rgb;

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_SetForeground, scs);
    // selfGlobalRef and scs are deleted in _SetForeground()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    _setBackground
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer__1setBackground(JNIEnv *env, jobject self,
                                                    jint rgb)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    SetColorStruct *scs = new SetColorStruct;
    scs->component = selfGlobalRef;
    scs->rgb = rgb;

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_SetBackground, scs);
    // selfGlobalRef and scs are deleted in _SetBackground()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    _setFont
 * Signature: (Ljava/awt/Font;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer__1setFont(JNIEnv *env, jobject self,
                        jobject font)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);
    jobject fontGlobalRef = env->NewGlobalRef(font);

    SetFontStruct *sfs = new SetFontStruct;
    sfs->component = selfGlobalRef;
    sfs->font = fontGlobalRef;

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_SetFont, sfs);
    // global refs and sfs are deleted in _SetFont()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    requestFocus
 * Signature: (Ljava/awt/Component;ZZJ)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_windows_WComponentPeer__1requestFocus
    (JNIEnv *env, jobject self, jobject lightweightChild, jboolean temporary,
     jboolean focusedWindowChangeAllowed, jlong time, jobject cause)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);
    jobject lightweightChildGlobalRef = env->NewGlobalRef(lightweightChild);

    RequestFocusStruct *rfs = new RequestFocusStruct;
    rfs->component = selfGlobalRef;
    rfs->lightweightChild = lightweightChildGlobalRef;
    rfs->temporary = temporary;
    rfs->focusedWindowChangeAllowed = focusedWindowChangeAllowed;
    rfs->time = time;
    rfs->cause = env->NewGlobalRef(cause);

    return (jboolean)AwtToolkit::GetInstance().SyncCall(
        (void*(*)(void*))AwtComponent::_RequestFocus, rfs);
    // global refs and rfs are deleted in _RequestFocus

    CATCH_BAD_ALLOC_RET(JNI_FALSE);
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    start
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_start(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_Start, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _Start

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    beginValidate
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_beginValidate(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_BeginValidate, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _BeginValidate

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    endValidate
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_endValidate(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_EndValidate, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _EndValidate

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_updateWindow(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_UpdateWindow, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _UpdateWindow

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    addNativeDropTarget
 * Signature: ()L
 */

JNIEXPORT jlong JNICALL
Java_sun_awt_windows_WComponentPeer_addNativeDropTarget(JNIEnv *env,
                                                        jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    return ptr_to_jlong(AwtToolkit::GetInstance().SyncCall(
        (void*(*)(void*))AwtComponent::_AddNativeDropTarget,
        (void *)selfGlobalRef));
    // selfGlobalRef is deleted in _AddNativeDropTarget

    CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    removeNativeDropTarget
 * Signature: ()V
 */

JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_removeNativeDropTarget(JNIEnv *env,
                                                           jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(
        AwtComponent::_RemoveNativeDropTarget, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _RemoveNativeDropTarget

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    getTargetGC
 * Signature: ()Ljava/awt/GraphicsConfiguration;
 */
JNIEXPORT jobject JNICALL
Java_sun_awt_windows_WComponentPeer_getTargetGC(JNIEnv* env, jobject theThis)
{
    TRY;

    jobject targetObj;
    jobject gc = 0;

    targetObj = env->GetObjectField(theThis, AwtObject::targetID);
    DASSERT(targetObj);

    gc = env->GetObjectField(targetObj, AwtComponent::graphicsConfigID);
    return gc;

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    createPrintedPixels
 * Signature: (IIIIII)I[
 */
JNIEXPORT jintArray JNICALL
Java_sun_awt_windows_WComponentPeer_createPrintedPixels(JNIEnv* env,
    jobject self, jint srcX, jint srcY, jint srcW, jint srcH)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    CreatePrintedPixelsStruct *cpps = new CreatePrintedPixelsStruct;
    cpps->component = selfGlobalRef;
    cpps->srcx = srcX;
    cpps->srcy = srcY;
    cpps->srcw = srcW;
    cpps->srch = srcH;

    jintArray globalRef = (jintArray)AwtToolkit::GetInstance().SyncCall(
        (void*(*)(void*))AwtComponent::_CreatePrintedPixels, cpps);
    // selfGlobalRef and cpps are deleted in _CreatePrintedPixels
    if (globalRef != NULL)
    {
        jintArray localRef = (jintArray)env->NewLocalRef(globalRef);
        env->DeleteGlobalRef(globalRef);
        return localRef;
    }
    else
    {
        return NULL;
    }

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    nativeHandlesWheelScrolling
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WComponentPeer_nativeHandlesWheelScrolling (JNIEnv* env,
    jobject self)
{
    TRY;

    return (jboolean)AwtToolkit::GetInstance().SyncCall(
        (void *(*)(void *))AwtComponent::_NativeHandlesWheelScrolling,
        env->NewGlobalRef(self));
    // global ref is deleted in _NativeHandlesWheelScrolling

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    isObscured
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WComponentPeer_isObscured(JNIEnv* env,
    jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    return (jboolean)AwtToolkit::GetInstance().SyncCall(
        (void*(*)(void*))AwtComponent::_IsObscured,
        (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _IsObscured

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_windows_WComponentPeer
 * Method:    wheelInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_wheelInit(JNIEnv *env, jclass cls)
{
    // Only necessary on Win95
    if (IS_WIN95 && !IS_WIN98) {
        AwtComponent::Wheel95Init();
    }
}

JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WComponentPeer_processSynchronousLightweightTransfer(JNIEnv *env, jclass cls,
                                                                          jobject heavyweight,
                                                                          jobject descendant,
                                                                          jboolean temporary,
                                                                          jboolean focusedWindowChangeAllowed,
                                                                          jlong time)
{
    TRY;

    return env->CallStaticBooleanMethod(AwtKeyboardFocusManager::keyboardFocusManagerCls,
                                        AwtKeyboardFocusManager::processSynchronousTransfer,
                                        heavyweight, descendant, temporary,
                                        focusedWindowChangeAllowed,
                                        time);

    CATCH_BAD_ALLOC_RET(JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_pSetParent(JNIEnv* env, jobject self, jobject parent) {
    TRY;

    typedef AwtComponent* PComponent;
    AwtComponent** comps = new PComponent[2];
    AwtComponent* comp = (AwtComponent*)JNI_GET_PDATA(self);
    AwtComponent* parentComp = (AwtComponent*)JNI_GET_PDATA(parent);
    comps[0] = comp;
    comps[1] = parentComp;

    AwtToolkit::GetInstance().SyncCall(AwtComponent::SetParent, comps);
    // comps is deleted in SetParent

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WComponentPeer_setRectangularShape(JNIEnv* env, jobject self,
        jint x1, jint y1, jint x2, jint y2, jobject region)
{
    TRY;

    SetRectangularShapeStruct * data = new SetRectangularShapeStruct;
    data->component = env->NewGlobalRef(self);
    data->x1 = x1;
    data->x2 = x2;
    data->y1 = y1;
    data->y2 = y2;
    if (region) {
        data->region = env->NewGlobalRef(region);
    } else {
        data->region = NULL;
    }

    AwtToolkit::GetInstance().SyncCall(AwtComponent::_SetRectangularShape, data);
    // global refs and data are deleted in _SetRectangularShape

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * Diagnostic routines
 */

#ifdef DEBUG

void AwtComponent::VerifyState()
{
    if (AwtToolkit::GetInstance().VerifyComponents() == FALSE) {
        return;
    }

    if (m_callbacksEnabled == FALSE) {
        /* Component is not fully setup yet. */
        return;
    }

    /* Get target bounds. */
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->PushLocalFrame(10) < 0)
        return;

    jobject target = GetTarget(env);

    jint x = env->GetIntField(target, AwtComponent::xID);
    jint y = env->GetIntField(target, AwtComponent::yID);
    jint width = env->GetIntField(target, AwtComponent::widthID);
    jint height = env->GetIntField(target, AwtComponent::heightID);

    /* Convert target origin to absolute coordinates */
    while (TRUE) {

        jobject parent = env->GetObjectField(target, AwtComponent::parentID);
        if (parent == NULL) {
            break;
        }
        x += env->GetIntField(parent, AwtComponent::xID);
        y += env->GetIntField(parent, AwtComponent::yID);

        /* If this component has insets, factor them in, but ignore
         * top-level windows.
         */
        jobject parent2 = env->GetObjectField(parent, AwtComponent::parentID);
        if (parent2 != NULL) {
            jobject peer = GetPeerForTarget(env, parent);
            if (peer != NULL &&
                JNU_IsInstanceOfByName(env, peer,
                                       "sun/awt/windows/WPanelPeer") > 0) {
                jobject insets =
                    JNU_CallMethodByName(env, NULL, peer,"insets",
                                         "()Ljava/awt/Insets;").l;
                x += (env)->GetIntField(insets, AwtInsets::leftID);
                y += (env)->GetIntField(insets, AwtInsets::topID);
            }
        }
        env->DeleteLocalRef(target);
        target = parent;
    }

    // Test whether component's bounds match the native window's
    RECT rect;
    VERIFY(::GetWindowRect(GetHWnd(), &rect));
#if 0
    DASSERT( (x == rect.left) &&
            (y == rect.top) &&
            (width == (rect.right-rect.left)) &&
            (height == (rect.bottom-rect.top)) );
#else
    BOOL fSizeValid = ( (x == rect.left) &&
            (y == rect.top) &&
            (width == (rect.right-rect.left)) &&
            (height == (rect.bottom-rect.top)) );
#endif

    // See if visible state matches
    BOOL wndVisible = ::IsWindowVisible(GetHWnd());
    jboolean targetVisible;
    // To avoid possibly running client code on the toolkit thread, don't
    // do the following check if we're running on the toolkit thread.
    if (AwtToolkit::MainThread() != ::GetCurrentThreadId()) {
        targetVisible = JNU_CallMethodByName(env, NULL, GetTarget(env),
                                                  "isShowing", "()Z").z;
        DASSERT(!safe_ExceptionOccurred(env));
    } else {
        targetVisible = wndVisible ? 1 : 0;
    }
#if 0
    DASSERT( (targetVisible && wndVisible) ||
            (!targetVisible && !wndVisible) );
#else
    BOOL fVisibleValid = ( (targetVisible && wndVisible) ||
            (!targetVisible && !wndVisible) );
#endif

    // Check enabled state
    BOOL wndEnabled = ::IsWindowEnabled(GetHWnd());
    jboolean enabled = (jboolean)env->GetBooleanField(target,
                                                      AwtComponent::enabledID);
#if 0
    DASSERT( (enabled && wndEnabled) ||
            (!enabled && !wndEnabled) );
#else
    BOOL fEnabledValid = ((enabled && wndEnabled) ||
                          (!(enabled && !wndEnabled) ));

    if (!fSizeValid || !fVisibleValid || !fEnabledValid) {
        printf("AwtComponent::ValidateState() failed:\n");
        // To avoid possibly running client code on the toolkit thread, don't
        // do the following call if we're running on the toolkit thread.
        if (AwtToolkit::MainThread() != ::GetCurrentThreadId()) {
            jstring targetStr =
                (jstring)JNU_CallMethodByName(env, NULL, GetTarget(env),
                                              "getName",
                                              "()Ljava/lang/String;").l;
            DASSERT(!safe_ExceptionOccurred(env));
            printf("\t%S\n", TO_WSTRING(targetStr));
        }
        printf("\twas:       [%d,%d,%dx%d]\n", x, y, width, height);
        if (!fSizeValid) {
            printf("\tshould be: [%d,%d,%dx%d]\n", rect.left, rect.top,
                   rect.right-rect.left, rect.bottom-rect.top);
        }
        if (!fVisibleValid) {
            printf("\tshould be: %s\n",
                   (targetVisible) ? "visible" : "hidden");
        }
        if (!fEnabledValid) {
            printf("\tshould be: %s\n",
                   enabled ? "enabled" : "disabled");
        }
    }
#endif
    env->PopLocalFrame(0);
}
#endif //DEBUG

// Methods for globally managed DC list

/**
 * Add a new DC to the DC list for this component.
 */
void DCList::AddDC(HDC hDC, HWND hWnd)
{
    DCItem *newItem = new DCItem;
    newItem->hDC = hDC;
    newItem->hWnd = hWnd;
    AddDCItem(newItem);
}

void DCList::AddDCItem(DCItem *newItem)
{
    listLock.Enter();
    newItem->next = head;
    head = newItem;
    listLock.Leave();
}

/**
 * Given a DC, remove it from the DC list and return
 * TRUE if it exists on the current list.  Otherwise
 * return FALSE.
 * A DC may not exist on the list because it has already
 * been released elsewhere (for example, the window
 * destruction process may release a DC while a rendering
 * thread may also want to release a DC when it notices that
 * its DC is obsolete for the current window).
 */
DCItem *DCList::RemoveDC(HDC hDC)
{
    listLock.Enter();
    DCItem **prevPtrPtr = &head;
    DCItem *listPtr = head;
    while (listPtr) {
        DCItem *nextPtr = listPtr->next;
        if (listPtr->hDC == hDC) {
            *prevPtrPtr = nextPtr;
            break;
        }
        prevPtrPtr = &listPtr->next;
        listPtr = nextPtr;
    }
    listLock.Leave();
    return listPtr;
}

/**
 * Remove all DCs from the DC list which are associated with
 * the same window as hWnd.  Return the list of those
 * DC's to the caller (which will then probably want to
 * call ReleaseDC() for the returned DCs).
 */
DCItem *DCList::RemoveAllDCs(HWND hWnd)
{
    listLock.Enter();
    DCItem **prevPtrPtr = &head;
    DCItem *listPtr = head;
    DCItem *newListPtr = NULL;
    BOOL ret = FALSE;
    while (listPtr) {
        DCItem *nextPtr = listPtr->next;
        if (listPtr->hWnd == hWnd) {
            *prevPtrPtr = nextPtr;
            listPtr->next = newListPtr;
            newListPtr = listPtr;
        } else {
            prevPtrPtr = &listPtr->next;
        }
        listPtr = nextPtr;
    }
    listLock.Leave();
    return newListPtr;
}


/**
 * Realize palettes of all existing HDC objects
 */
void DCList::RealizePalettes(int screen)
{
    listLock.Enter();
    DCItem *listPtr = head;
    while (listPtr) {
        AwtWin32GraphicsDevice::RealizePalette(listPtr->hDC, screen);
        listPtr = listPtr->next;
    }
    listLock.Leave();
}

void MoveDCToPassiveList(HDC hDC) {
    DCItem *removedDC;
    if ((removedDC = activeDCList.RemoveDC(hDC)) != NULL) {
        passiveDCList.AddDCItem(removedDC);
    }
}

void ReleaseDCList(HWND hwnd, DCList &list) {
    DCItem *removedDCs = list.RemoveAllDCs(hwnd);
    while (removedDCs) {
        DCItem *tmpDCList = removedDCs;
        DASSERT(::GetObjectType(tmpDCList->hDC) == OBJ_DC);
        int retValue = ::ReleaseDC(tmpDCList->hWnd, tmpDCList->hDC);
        VERIFY(retValue != 0);
        if (retValue != 0) {
            // Valid ReleaseDC call; need to decrement GDI object counter
            AwtGDIObject::Decrement();
        }
        removedDCs = removedDCs->next;
        delete tmpDCList;
    }
}
