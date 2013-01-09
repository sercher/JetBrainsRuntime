/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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

#include "util.h"
#include "transport.h"
#include "debugLoop.h"
#include "sys.h"

static jdwpTransportEnv *transport;
static jrawMonitorID listenerLock;
static jrawMonitorID sendLock;

/*
 * data structure used for passing transport info from thread to thread
 */
typedef struct TransportInfo {
    char *name;
    jdwpTransportEnv *transport;
    char *address;
    long timeout;
} TransportInfo;

static struct jdwpTransportCallback callback = {jvmtiAllocate, jvmtiDeallocate};

/*
 * Print the last transport error
 */
static void
printLastError(jdwpTransportEnv *t, jdwpTransportError err)
{
    char  *msg;
    jbyte *utf8msg;
    jdwpTransportError rv;

    msg     = NULL;
    utf8msg = NULL;
    rv = (*t)->GetLastError(t, &msg); /* This is a platform encoded string */
    if ( msg != NULL ) {
        int len;
        int maxlen;

        /* Convert this string to UTF8 */
        len = (int)strlen(msg);
        maxlen = len+len/2+2; /* Should allow for plenty of room */
        utf8msg = (jbyte*)jvmtiAllocate(maxlen+1);
        (void)(gdata->npt->utf8FromPlatform)(gdata->npt->utf,
            msg, len, utf8msg, maxlen);
        utf8msg[maxlen] = 0;
    }
    if (rv == JDWPTRANSPORT_ERROR_NONE) {
        ERROR_MESSAGE(("transport error %d: %s",err, utf8msg));
    } else if ( msg!=NULL ) {
        ERROR_MESSAGE(("transport error %d: %s",err, utf8msg));
    } else {
        ERROR_MESSAGE(("transport error %d: %s",err, "UNKNOWN"));
    }
    jvmtiDeallocate(msg);
    jvmtiDeallocate(utf8msg);
}

/* Find OnLoad symbol */
static jdwpTransport_OnLoad_t
findTransportOnLoad(void *handle)
{
    jdwpTransport_OnLoad_t onLoad;

    onLoad = (jdwpTransport_OnLoad_t)NULL;
    if (handle == NULL) {
        return onLoad;
    }
    onLoad = (jdwpTransport_OnLoad_t)
                 dbgsysFindLibraryEntry(handle, "jdwpTransport_OnLoad");
    return onLoad;
}

/* Load transport library (directory=="" means do system search) */
static void *
loadTransportLibrary(char *libdir, char *name)
{
    void *handle;
    char libname[MAXPATHLEN+2];
    char buf[MAXPATHLEN*2+100];
    char *plibdir;

    /* Convert libdir from UTF-8 to platform encoding */
    plibdir = NULL;
    if ( libdir != NULL ) {
        int  len;

        len = (int)strlen(libdir);
        (void)(gdata->npt->utf8ToPlatform)(gdata->npt->utf,
            (jbyte*)libdir, len, buf, (int)sizeof(buf));
        plibdir = buf;
    }

    /* Construct library name (simple name or full path) */
    dbgsysBuildLibName(libname, sizeof(libname), plibdir, name);

    /* dlopen (unix) / LoadLibrary (windows) the transport library */
    handle = dbgsysLoadLibrary(libname, buf, sizeof(buf));
    return handle;
}

/*
 * loadTransport() is adapted from loadJVMHelperLib() in
 * JDK 1.2 javai.c v1.61
 */
static jdwpError
loadTransport(char *name, jdwpTransportEnv **transportPtr)
{
    JNIEnv                 *env;
    jdwpTransport_OnLoad_t  onLoad;
    void                   *handle;
    char                   *libdir;

    /* Make sure library name is not empty */
    if (name == NULL) {
        ERROR_MESSAGE(("library name is empty"));
        return JDWP_ERROR(TRANSPORT_LOAD);
    }

    /* First, look in sun.boot.library.path. This should find the standard
     *  dt_socket and dt_shmem transport libraries, or any library
     *  that was delivered with the J2SE.
     *  Note: Since 6819213 fixed, Java property sun.boot.library.path can
     *  contain multiple paths. Dll_dir is the first entry and
     *  -Dsun.boot.library.path entries are appended.
     */
    libdir = gdata->property_sun_boot_library_path;
    if (libdir == NULL) {
        ERROR_MESSAGE(("Java property sun.boot.library.path is not set"));
        return JDWP_ERROR(TRANSPORT_LOAD);
    }
    handle = loadTransportLibrary(libdir, name);
    if (handle == NULL) {
        /* Second, look along the path used by the native dlopen/LoadLibrary
         *  functions. This should effectively try and load the simple
         *  library name, which will cause the default system library
         *  search technique to happen.
         *  We should only reach here if the transport library wasn't found
         *  in the J2SE directory, e.g. it's a custom transport library
         *  not installed in the J2SE like dt_socket and dt_shmem is.
         *
         *  Note: Why not use java.library.path? Several reasons:
         *        a) This matches existing agentlib search
         *        b) These are technically not JNI libraries
         */
        handle = loadTransportLibrary("", name);
    }

    /* See if a library was found with this name */
    if (handle == NULL) {
        ERROR_MESSAGE(("transport library not found: %s", name));
        return JDWP_ERROR(TRANSPORT_LOAD);
    }

    /* Find the onLoad address */
    onLoad = findTransportOnLoad(handle);
    if (onLoad == NULL) {
        ERROR_MESSAGE(("transport library missing onLoad entry: %s", name));
        return JDWP_ERROR(TRANSPORT_LOAD);
    }

    /* Get transport interface */
    env = getEnv();
    if ( env != NULL ) {
        jdwpTransportEnv *t;
        JavaVM           *jvm;
        jint              ver;

        JNI_FUNC_PTR(env,GetJavaVM)(env, &jvm);
        ver = (*onLoad)(jvm, &callback, JDWPTRANSPORT_VERSION_1_0, &t);
        if (ver != JNI_OK) {
            switch (ver) {
                case JNI_ENOMEM :
                    ERROR_MESSAGE(("insufficient memory to complete initialization"));
                    break;

                case JNI_EVERSION :
                    ERROR_MESSAGE(("transport doesn't recognize version %x",
                        JDWPTRANSPORT_VERSION_1_0));
                    break;

                case JNI_EEXIST :
                    ERROR_MESSAGE(("transport doesn't support multiple environments"));
                    break;

                default:
                    ERROR_MESSAGE(("unrecognized error %d from transport", ver));
                    break;
            }

            return JDWP_ERROR(TRANSPORT_INIT);
        }
        *transportPtr = t;
    } else {
        return JDWP_ERROR(TRANSPORT_LOAD);
    }

    return JDWP_ERROR(NONE);
}

static void
connectionInitiated(jdwpTransportEnv *t)
{
    jint isValid = JNI_FALSE;

    debugMonitorEnter(listenerLock);

    /*
     * Don't allow a connection until initialization is complete
     */
    debugInit_waitInitComplete();

    /* Are we the first transport to get a connection? */

    if (transport == NULL) {
        transport = t;
        isValid = JNI_TRUE;
    } else {
        if (transport == t) {
            /* connected with the same transport as before */
            isValid = JNI_TRUE;
        } else {
            /*
             * Another transport got a connection - multiple transports
             * not fully supported yet so shouldn't get here.
             */
            (*t)->Close(t);
            JDI_ASSERT(JNI_FALSE);
        }
    }

    if (isValid) {
        debugMonitorNotifyAll(listenerLock);
    }

    debugMonitorExit(listenerLock);

    if (isValid) {
        debugLoop_run();
    }

}

/*
 * Set the transport property (sun.jdwp.listenerAddress) to the
 * specified value.
 */
static void
setTransportProperty(JNIEnv* env, char* value) {
    char* prop_value = (value == NULL) ? "" : value;
    setAgentPropertyValue(env, "sun.jdwp.listenerAddress", prop_value);
}

void
transport_waitForConnection(void)
{
    /*
     * If the VM is suspended on debugger initialization, we wait
     * for a connection before continuing. This ensures that all
     * events are delivered to the debugger. (We might as well do this
     * this since the VM won't continue until a remote debugger attaches
     * and resumes it.) If not suspending on initialization, we must
     * just drop any packets (i.e. events) so that the VM can continue
     * to run. The debugger may not attach until much later.
     */
    if (debugInit_suspendOnInit()) {
        debugMonitorEnter(listenerLock);
        while (transport == NULL) {
            debugMonitorWait(listenerLock);
        }
        debugMonitorExit(listenerLock);
    }
}

static void JNICALL
acceptThread(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg)
{
    TransportInfo *info;
    jdwpTransportEnv *t;
    jdwpTransportError rc;

    LOG_MISC(("Begin accept thread"));

    info = (TransportInfo*)(void*)arg;
    t = info->transport;

    rc = (*t)->Accept(t, info->timeout, 0);

    /* System property no longer needed */
    setTransportProperty(jni_env, NULL);

    if (rc != JDWPTRANSPORT_ERROR_NONE) {
        /*
         * If accept fails it probably means a timeout, or another fatal error
         * We thus exit the VM after stopping the listener.
         */
        printLastError(t, rc);
        (*t)->StopListening(t);
        EXIT_ERROR(JVMTI_ERROR_NONE, "could not connect, timeout or fatal error");
    } else {
        (*t)->StopListening(t);
        connectionInitiated(t);
    }

    LOG_MISC(("End accept thread"));
}

static void JNICALL
attachThread(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg)
{
    LOG_MISC(("Begin attach thread"));
    connectionInitiated((jdwpTransportEnv *)(void*)arg);
    LOG_MISC(("End attach thread"));
}

void
transport_initialize(void)
{
    transport = NULL;
    listenerLock = debugMonitorCreate("JDWP Transport Listener Monitor");
    sendLock = debugMonitorCreate("JDWP Transport Send Monitor");
}

void
transport_reset(void)
{
    /*
     * Reset the transport by closing any listener (will silently fail
     * with JDWPTRANSPORT_ERROR_ILLEGAL_STATE if not listening), and
     * closing any connection (will also fail silently if not
     * connected).
     *
     * Note: There's an assumption here that we don't yet support
     * multiple transports. When we do then we need a clear transition
     * from the current transport to the new transport.
     */
    if (transport != NULL) {
        setTransportProperty(getEnv(), NULL);
        (*transport)->StopListening(transport);
        (*transport)->Close(transport);
    }
}

static jdwpError
launch(char *command, char *name, char *address)
{
    jint rc;
    char *buf;
    char *commandLine;
    int  len;

    /* Construct complete command line (all in UTF-8) */
    commandLine = jvmtiAllocate((int)strlen(command) +
                                 (int)strlen(name) +
                                 (int)strlen(address) + 3);
    if (commandLine == NULL) {
        return JDWP_ERROR(OUT_OF_MEMORY);
    }
    (void)strcpy(commandLine, command);
    (void)strcat(commandLine, " ");
    (void)strcat(commandLine, name);
    (void)strcat(commandLine, " ");
    (void)strcat(commandLine, address);

    /* Convert commandLine from UTF-8 to platform encoding */
    len = (int)strlen(commandLine);
    buf = jvmtiAllocate(len*3+3);
    (void)(gdata->npt->utf8ToPlatform)(gdata->npt->utf,
        (jbyte*)commandLine, len, buf, len*3+3);

    /* Exec commandLine */
    rc = dbgsysExec(buf);

    /* Free up buffers */
    jvmtiDeallocate(buf);
    jvmtiDeallocate(commandLine);

    /* And non-zero exit status means we had an error */
    if (rc != SYS_OK) {
        return JDWP_ERROR(TRANSPORT_INIT);
    }
    return JDWP_ERROR(NONE);
}

jdwpError
transport_startTransport(jboolean isServer, char *name, char *address,
                         long timeout)
{
    jvmtiStartFunction func;
    jdwpTransportEnv *trans;
    char threadName[MAXPATHLEN + 100];
    jint err;
    jdwpError serror;

    /*
     * If the transport is already loaded then use it
     * Note: We're assuming here that we don't support multiple
     * transports - when we do then we need to handle the case
     * where the transport library only supports a single environment.
     * That probably means we have a bag a transport environments
     * to correspond to the transports bag.
     */
    if (transport != NULL) {
        trans = transport;
    } else {
        serror = loadTransport(name, &trans);
        if (serror != JDWP_ERROR(NONE)) {
            return serror;
        }
    }

    if (isServer) {

        char *retAddress;
        char *launchCommand;
        TransportInfo *info;
        jvmtiError error;
        int len;
        char* prop_value;

        info = jvmtiAllocate(sizeof(*info));
        if (info == NULL) {
            return JDWP_ERROR(OUT_OF_MEMORY);
        }
        info->name = jvmtiAllocate((int)strlen(name)+1);
        (void)strcpy(info->name, name);
        info->address = NULL;
        info->timeout = timeout;
        if (info->name == NULL) {
            serror = JDWP_ERROR(OUT_OF_MEMORY);
            goto handleError;
        }
        if (address != NULL) {
            info->address = jvmtiAllocate((int)strlen(address)+1);
            (void)strcpy(info->address, address);
            if (info->address == NULL) {
                serror = JDWP_ERROR(OUT_OF_MEMORY);
                goto handleError;
            }
        }

        info->transport = trans;

        err = (*trans)->StartListening(trans, address, &retAddress);
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            printLastError(trans, err);
            serror = JDWP_ERROR(TRANSPORT_INIT);
            goto handleError;
        }

        /*
         * Record listener address in a system property
         */
        len = (int)strlen(name) + (int)strlen(retAddress) + 2; /* ':' and '\0' */
        prop_value = (char*)jvmtiAllocate(len);
        strcpy(prop_value, name);
        strcat(prop_value, ":");
        strcat(prop_value, retAddress);
        setTransportProperty(getEnv(), prop_value);
        jvmtiDeallocate(prop_value);


        (void)strcpy(threadName, "JDWP Transport Listener: ");
        (void)strcat(threadName, name);

        func = &acceptThread;
        error = spawnNewThread(func, (void*)info, threadName);
        if (error != JVMTI_ERROR_NONE) {
            serror = map2jdwpError(error);
            goto handleError;
        }

        launchCommand = debugInit_launchOnInit();
        if (launchCommand != NULL) {
            serror = launch(launchCommand, name, retAddress);
            if (serror != JDWP_ERROR(NONE)) {
                goto handleError;
            }
        } else {
            if ( ! gdata->quiet ) {
                TTY_MESSAGE(("Listening for transport %s at address: %s",
                    name, retAddress));
            }
        }
        return JDWP_ERROR(NONE);

handleError:
        jvmtiDeallocate(info->name);
        jvmtiDeallocate(info->address);
        jvmtiDeallocate(info);
    } else {
        /*
         * Note that we don't attempt to do a launch here. Launching
         * is currently supported only in server mode.
         */

        /*
         * If we're connecting to another process, there shouldn't be
         * any concurrent listens, so its ok if we block here in this
         * thread, waiting for the attach to finish.
         */
         err = (*trans)->Attach(trans, address, timeout, 0);
         if (err != JDWPTRANSPORT_ERROR_NONE) {
             printLastError(trans, err);
             serror = JDWP_ERROR(TRANSPORT_INIT);
             return serror;
         }

         /*
          * Start the transport loop in a separate thread
          */
         (void)strcpy(threadName, "JDWP Transport Listener: ");
         (void)strcat(threadName, name);

         func = &attachThread;
         err = spawnNewThread(func, (void*)trans, threadName);
         serror = map2jdwpError(err);
    }
    return serror;
}

void
transport_close(void)
{
    if ( transport != NULL ) {
        (*transport)->Close(transport);
    }
}

jboolean
transport_is_open(void)
{
    jboolean is_open = JNI_FALSE;

    if ( transport != NULL ) {
        is_open = (*transport)->IsOpen(transport);
    }
    return is_open;
}

jint
transport_sendPacket(jdwpPacket *packet)
{
    jdwpTransportError err = JDWPTRANSPORT_ERROR_NONE;
    jint rc = 0;

    if (transport != NULL) {
        if ( (*transport)->IsOpen(transport) ) {
            debugMonitorEnter(sendLock);
            err = (*transport)->WritePacket(transport, packet);
            debugMonitorExit(sendLock);
        }
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            if ((*transport)->IsOpen(transport)) {
                printLastError(transport, err);
            }

            /*
             * The users of transport_sendPacket except 0 for
             * success; non-0 otherwise.
             */
            rc = (jint)-1;
        }

    } /* else, bit bucket */

    return rc;
}

jint
transport_receivePacket(jdwpPacket *packet)
{
    jdwpTransportError err;

    err = (*transport)->ReadPacket(transport, packet);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        /*
         * If transport has been closed return EOF
         */
        if (!(*transport)->IsOpen(transport)) {
            packet->type.cmd.len = 0;
            return 0;
        }

        printLastError(transport, err);

        /*
         * Users of transport_receivePacket expect 0 for success,
         * non-0 otherwise.
         */
        return (jint)-1;
    }
    return 0;
}
