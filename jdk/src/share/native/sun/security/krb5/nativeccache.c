/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

#import "sun_security_krb5_Credentials.h"
#import <Kerberos/Kerberos.h>

/*
 * Based largely on klist.c,
 *
 * Created by Scott Kovatch on 8/12/04.
 *
 * See http://www.opensource.apple.com/darwinsource/10.3.3/Kerberos-47/KerberosClients/klist/Sources/klist.c

 */

/*
 * Statics for this module
 */

static jclass derValueClass = NULL;
static jclass ticketClass = NULL;
static jclass principalNameClass = NULL;
static jclass encryptionKeyClass = NULL;
static jclass ticketFlagsClass = NULL;
static jclass kerberosTimeClass = NULL;
static jclass javaLangStringClass = NULL;
static jclass javaLangIntegerClass = NULL;
static jclass hostAddressClass = NULL;
static jclass hostAddressesClass = NULL;

static jmethodID derValueConstructor = 0;
static jmethodID ticketConstructor = 0;
static jmethodID principalNameConstructor = 0;
static jmethodID encryptionKeyConstructor = 0;
static jmethodID ticketFlagsConstructor = 0;
static jmethodID kerberosTimeConstructor = 0;
static jmethodID krbcredsConstructor = 0;
static jmethodID integerConstructor = 0;
static jmethodID hostAddressConstructor = 0;
static jmethodID hostAddressesConstructor = 0;

/*
 * Function prototypes for internal routines
 */

static jobject BuildTicket(JNIEnv *env, krb5_data *encodedTicket);
static jobject BuildClientPrincipal(JNIEnv *env, krb5_context kcontext, krb5_principal principalName);
static jobject BuildEncryptionKey(JNIEnv *env, krb5_keyblock *cryptoKey);
static jobject BuildTicketFlags(JNIEnv *env, krb5_flags flags);
static jobject BuildKerberosTime(JNIEnv *env, krb5_timestamp kerbtime);
static jobject BuildAddressList(JNIEnv *env, krb5_address **kerbtime);

static void printiferr (errcode_t err, const char *format, ...);

static jclass FindClass(JNIEnv *env, char *className)
{
    jclass cls = (*env)->FindClass(env, className);

    if (cls == NULL) {
        printf("Couldn't find %s\n", className);
        return NULL;
    }
#ifdef DEBUG
    printf("Found %s\n", className);
#endif /* DEBUG */

    jobject returnValue = (*env)->NewWeakGlobalRef(env,cls);
    return returnValue;
}
/*
 * Class:     sun_security_krb5_KrbCreds
 * Method:    JNI_OnLoad
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    JNIEnv *env;

    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_4)) {
        return JNI_EVERSION; /* JNI version not supported */
    }

    ticketClass = FindClass(env, "sun/security/krb5/internal/Ticket");
    if (ticketClass == NULL) return JNI_ERR;

    principalNameClass = FindClass(env, "sun/security/krb5/PrincipalName");
    if (principalNameClass == NULL) return JNI_ERR;

    derValueClass = FindClass(env, "sun/security/util/DerValue");
    if (derValueClass == NULL) return JNI_ERR;

    encryptionKeyClass = FindClass(env, "sun/security/krb5/EncryptionKey");
    if (encryptionKeyClass == NULL) return JNI_ERR;

    ticketFlagsClass = FindClass(env,"sun/security/krb5/internal/TicketFlags");
    if (ticketFlagsClass == NULL) return JNI_ERR;

    kerberosTimeClass = FindClass(env,"sun/security/krb5/internal/KerberosTime");
    if (kerberosTimeClass == NULL) return JNI_ERR;

    javaLangStringClass = FindClass(env,"java/lang/String");
    if (javaLangStringClass == NULL) return JNI_ERR;

    javaLangIntegerClass = FindClass(env,"java/lang/Integer");
    if (javaLangIntegerClass == NULL) return JNI_ERR;

    hostAddressClass = FindClass(env,"sun/security/krb5/internal/HostAddress");
    if (hostAddressClass == NULL) return JNI_ERR;

    hostAddressesClass = FindClass(env,"sun/security/krb5/internal/HostAddresses");
    if (hostAddressesClass == NULL) return JNI_ERR;

    derValueConstructor = (*env)->GetMethodID(env, derValueClass, "<init>", "([B)V");
    if (derValueConstructor == 0) {
        printf("Couldn't find DerValue constructor\n");
        return JNI_ERR;
    }
#ifdef DEBUG
    printf("Found DerValue constructor\n");
#endif /* DEBUG */

    ticketConstructor = (*env)->GetMethodID(env, ticketClass, "<init>", "(Lsun/security/util/DerValue;)V");
    if (derValueConstructor == 0) {
        printf("Couldn't find Ticket constructor\n");
        return JNI_ERR;
    }
#ifdef DEBUG
    printf("Found Ticket constructor\n");
#endif /* DEBUG */

    principalNameConstructor = (*env)->GetMethodID(env, principalNameClass, "<init>", "(Ljava/lang/String;I)V");
    if (principalNameConstructor == 0) {
        printf("Couldn't find PrincipalName constructor\n");
        return JNI_ERR;
    }
#ifdef DEBUG
    printf("Found PrincipalName constructor\n");
#endif /* DEBUG */

    encryptionKeyConstructor = (*env)->GetMethodID(env, encryptionKeyClass, "<init>", "(I[B)V");
    if (encryptionKeyConstructor == 0) {
        printf("Couldn't find EncryptionKey constructor\n");
        return JNI_ERR;
    }
#ifdef DEBUG
    printf("Found EncryptionKey constructor\n");
#endif /* DEBUG */

    ticketFlagsConstructor = (*env)->GetMethodID(env, ticketFlagsClass, "<init>", "(I[B)V");
    if (ticketFlagsConstructor == 0) {
        printf("Couldn't find TicketFlags constructor\n");
        return JNI_ERR;
    }
#ifdef DEBUG
    printf("Found TicketFlags constructor\n");
#endif /* DEBUG */

    kerberosTimeConstructor = (*env)->GetMethodID(env, kerberosTimeClass, "<init>", "(J)V");
    if (kerberosTimeConstructor == 0) {
        printf("Couldn't find KerberosTime constructor\n");
        return JNI_ERR;
    }
#ifdef DEBUG
    printf("Found KerberosTime constructor\n");
#endif /* DEBUG */

    integerConstructor = (*env)->GetMethodID(env, javaLangIntegerClass, "<init>", "(I)V");
    if (integerConstructor == 0) {
        printf("Couldn't find Integer constructor\n");
        return JNI_ERR;
    }
#ifdef DEBUG
    printf("Found Integer constructor\n");
#endif /* DEBUG */

    hostAddressConstructor = (*env)->GetMethodID(env, hostAddressClass, "<init>", "(I[B)V");
    if (hostAddressConstructor == 0) {
        printf("Couldn't find HostAddress constructor\n");
        return JNI_ERR;
    }
#ifdef DEBUG
    printf("Found HostAddress constructor\n");
#endif /* DEBUG */

    hostAddressesConstructor = (*env)->GetMethodID(env, hostAddressesClass, "<init>", "([Lsun/security/krb5/internal/HostAddress;)V");
    if (hostAddressesConstructor == 0) {
        printf("Couldn't find HostAddresses constructor\n");
        return JNI_ERR;
    }
#ifdef DEBUG
    printf("Found HostAddresses constructor\n");
#endif /* DEBUG */

#ifdef DEBUG
    printf("Finished OnLoad processing\n");
#endif /* DEBUG */

    return JNI_VERSION_1_2;
}

/*
 * Class:     sun_security_jgss_KrbCreds
 * Method:    JNI_OnUnload
 */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *jvm, void *reserved)
{
    JNIEnv *env;

    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
        return; /* Nothing else we can do */
    }

    if (ticketClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,ticketClass);
    }
    if (derValueClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,derValueClass);
    }
    if (principalNameClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,principalNameClass);
    }
    if (encryptionKeyClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,encryptionKeyClass);
    }
    if (ticketFlagsClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,ticketFlagsClass);
    }
    if (kerberosTimeClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,kerberosTimeClass);
    }
    if (javaLangStringClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,javaLangStringClass);
    }
    if (javaLangIntegerClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,javaLangIntegerClass);
    }
    if (hostAddressClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,hostAddressClass);
    }
    if (hostAddressesClass != NULL) {
        (*env)->DeleteWeakGlobalRef(env,hostAddressesClass);
    }

}

/*
 * Class:     sun_security_krb5_Credentials
 * Method:    acquireDefaultNativeCreds
 * Signature: ()Lsun/security/krb5/Credentials;
 */
JNIEXPORT jobject JNICALL Java_sun_security_krb5_Credentials_acquireDefaultNativeCreds
(JNIEnv *env, jclass krbcredsClass)
{
    jobject krbCreds = NULL;
    krb5_error_code err = 0;
    krb5_ccache ccache = NULL;
    krb5_cc_cursor cursor = NULL;
    krb5_creds creds;
    krb5_flags flags = 0;
    krb5_context kcontext = NULL;

    /* Initialize the Kerberos 5 context */
    err = krb5_init_context (&kcontext);

    if (!err) {
        err = krb5_cc_default (kcontext, &ccache);
    }

    if (!err) {
        err = krb5_cc_set_flags (kcontext, ccache, flags); /* turn off OPENCLOSE */
    }

    if (!err) {
        err = krb5_cc_start_seq_get (kcontext, ccache, &cursor);
    }

    if (!err) {
        while ((err = krb5_cc_next_cred (kcontext, ccache, &cursor, &creds)) == 0) {
            char *serverName = NULL;

            if (!err) {
                err = krb5_unparse_name (kcontext, creds.server, &serverName);
                printiferr (err, "while unparsing server name");
            }

            if (!err) {
                if (strncmp (serverName, "krbtgt", strlen("krbtgt")) == 0) {
                    jobject ticket, clientPrincipal, targetPrincipal, encryptionKey;
                    jobject ticketFlags, startTime, endTime;
                    jobject authTime, renewTillTime, hostAddresses;

                    ticket = clientPrincipal = targetPrincipal = encryptionKey = NULL;
                    ticketFlags = startTime = endTime = NULL;
                    authTime = renewTillTime = hostAddresses = NULL;

                    // For the default credentials we're only interested in the krbtgt server.
                    clientPrincipal = BuildClientPrincipal(env, kcontext, creds.client);
                    if (clientPrincipal == NULL) goto cleanup;

                    targetPrincipal = BuildClientPrincipal(env, kcontext, creds.server);
                    if (targetPrincipal == NULL) goto cleanup;

                    // Build a com.ibm.security.krb5.Ticket
                    ticket = BuildTicket(env, &creds.ticket);
                    if (ticket == NULL) goto cleanup;

                    // Get the encryption key
                    encryptionKey = BuildEncryptionKey(env, &creds.keyblock);
                    if (encryptionKey == NULL) goto cleanup;

                    // and the ticket flags
                    ticketFlags = BuildTicketFlags(env, creds.ticket_flags);
                    if (ticketFlags == NULL) goto cleanup;

                    // Get the timestamps out.
                    startTime = BuildKerberosTime(env, creds.times.starttime);
                    if (startTime == NULL) goto cleanup;

                    authTime = BuildKerberosTime(env, creds.times.authtime);
                    if (authTime == NULL) goto cleanup;

                    endTime = BuildKerberosTime(env, creds.times.endtime);
                    if (endTime == NULL) goto cleanup;

                    renewTillTime = BuildKerberosTime(env, creds.times.renew_till);
                    if (renewTillTime == NULL) goto cleanup;

                    // Create the addresses object.
                    hostAddresses = BuildAddressList(env, creds.addresses);

                    if (krbcredsConstructor == 0) {
                        krbcredsConstructor = (*env)->GetMethodID(env, krbcredsClass, "<init>",
                                                                  "(Lsun/security/krb5/internal/Ticket;Lsun/security/krb5/PrincipalName;Lsun/security/krb5/PrincipalName;Lsun/security/krb5/EncryptionKey;Lsun/security/krb5/internal/TicketFlags;Lsun/security/krb5/internal/KerberosTime;Lsun/security/krb5/internal/KerberosTime;Lsun/security/krb5/internal/KerberosTime;Lsun/security/krb5/internal/KerberosTime;Lsun/security/krb5/internal/HostAddresses;)V");
                        if (krbcredsConstructor == 0) {
                            printf("Couldn't find com.ibm.security.krb5.Credentials constructor\n");
                            break;
                        }
                    }

                    // and now go build a KrbCreds object
                    krbCreds = (*env)->NewObject(
                                                 env,
                                                 krbcredsClass,
                                                 krbcredsConstructor,
                                                 ticket,
                                                 clientPrincipal,
                                                 targetPrincipal,
                                                 encryptionKey,
                                                 ticketFlags,
                                                 authTime,
                                                 startTime,
                                                 endTime,
                                                 renewTillTime,
                                                 hostAddresses);
cleanup:
                    if (ticket) (*env)->DeleteLocalRef(env, ticket);
                    if (clientPrincipal) (*env)->DeleteLocalRef(env, clientPrincipal);
                    if (targetPrincipal) (*env)->DeleteLocalRef(env, targetPrincipal);
                    if (encryptionKey) (*env)->DeleteLocalRef(env, encryptionKey);
                    if (ticketFlags) (*env)->DeleteLocalRef(env, ticketFlags);
                    if (authTime) (*env)->DeleteLocalRef(env, authTime);
                    if (startTime) (*env)->DeleteLocalRef(env, startTime);
                    if (endTime) (*env)->DeleteLocalRef(env, endTime);
                    if (renewTillTime) (*env)->DeleteLocalRef(env, renewTillTime);
                    if (hostAddresses) (*env)->DeleteLocalRef(env, hostAddresses);
                }

            }

            if (serverName != NULL) { krb5_free_unparsed_name (kcontext, serverName); }

            krb5_free_cred_contents (kcontext, &creds);
        }

        if (err == KRB5_CC_END) { err = 0; }
        printiferr (err, "while retrieving a ticket");

    }

    if (!err) {
        err = krb5_cc_end_seq_get (kcontext, ccache, &cursor);
        printiferr (err, "while finishing ticket retrieval");
    }

    if (!err) {
        flags = KRB5_TC_OPENCLOSE; /* restore OPENCLOSE mode */
        err = krb5_cc_set_flags (kcontext, ccache, flags);
        printiferr (err, "while finishing ticket retrieval");
    }

    krb5_free_context (kcontext);
    return krbCreds;
}


#pragma mark -

jobject BuildTicket(JNIEnv *env, krb5_data *encodedTicket)
{
    /* To build a Ticket, we first need to build a DerValue out of the EncodedTicket.
    * But before we can do that, we need to make a byte array out of the ET.
    */

    jobject derValue, ticket;
    jbyteArray ary;

    ary = (*env)->NewByteArray(env, encodedTicket->length);
    if ((*env)->ExceptionOccurred(env)) {
        return (jobject) NULL;
    }

    (*env)->SetByteArrayRegion(env, ary, (jsize) 0, encodedTicket->length, (jbyte *)encodedTicket->data);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->DeleteLocalRef(env, ary);
        return (jobject) NULL;
    }

    derValue = (*env)->NewObject(env, derValueClass, derValueConstructor, ary);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->DeleteLocalRef(env, ary);
        return (jobject) NULL;
    }

    (*env)->DeleteLocalRef(env, ary);
    ticket = (*env)->NewObject(env, ticketClass, ticketConstructor, derValue);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->DeleteLocalRef(env, derValue);
        return (jobject) NULL;
    }
    (*env)->DeleteLocalRef(env, derValue);
    return ticket;
}

jobject BuildClientPrincipal(JNIEnv *env, krb5_context kcontext, krb5_principal principalName) {
    // Get the full principal string.
    char *principalString = NULL;
    jobject principal = NULL;
    int err = krb5_unparse_name (kcontext, principalName, &principalString);

    if (!err) {
        // Make a PrincipalName from the full string and the type.  Let the PrincipalName class parse it out.
        jstring principalStringObj = (*env)->NewStringUTF(env, principalString);
        principal = (*env)->NewObject(env, principalNameClass, principalNameConstructor, principalStringObj, principalName->type);
        if (principalString != NULL) { krb5_free_unparsed_name (kcontext, principalString); }
        (*env)->DeleteLocalRef(env, principalStringObj);
    }

    return principal;
}

jobject BuildEncryptionKey(JNIEnv *env, krb5_keyblock *cryptoKey) {
    // First, need to build a byte array
    jbyteArray ary;
    jobject encryptionKey = NULL;

    ary = (*env)->NewByteArray(env,cryptoKey->length);
    (*env)->SetByteArrayRegion(env, ary, (jsize) 0, cryptoKey->length, (jbyte *)cryptoKey->contents);
    if (!(*env)->ExceptionOccurred(env)) {
        encryptionKey = (*env)->NewObject(env, encryptionKeyClass, encryptionKeyConstructor, cryptoKey->enctype, ary);
    }

    (*env)->DeleteLocalRef(env, ary);
    return encryptionKey;
}

jobject BuildTicketFlags(JNIEnv *env, krb5_flags flags) {
    jobject ticketFlags = NULL;
    jbyteArray ary;

    /*
     * Convert the bytes to network byte order before copying
     * them to a Java byte array.
     */
    unsigned long nlflags = htonl(flags);

    ary = (*env)->NewByteArray(env, sizeof(flags));
    (*env)->SetByteArrayRegion(env, ary, (jsize) 0, sizeof(flags), (jbyte *)&nlflags);

    if (!(*env)->ExceptionOccurred(env)) {
        ticketFlags = (*env)->NewObject(env, ticketFlagsClass, ticketFlagsConstructor, sizeof(flags)*8, ary);
    }

    (*env)->DeleteLocalRef(env, ary);
    return ticketFlags;
}

jobject BuildKerberosTime(JNIEnv *env, krb5_timestamp kerbtime) {
    jlong time = kerbtime;

    // Kerberos time is in seconds, but the KerberosTime class assumes milliseconds, so multiply by 1000.
    time *= 1000;
    return (*env)->NewObject(env, kerberosTimeClass, kerberosTimeConstructor, time);
}

jobject BuildAddressList(JNIEnv *env, krb5_address **addresses) {

    if (addresses == NULL) {
        return NULL;
    }

    int addressCount = 0;

    // See how many we have.
    krb5_address **p = addresses;

    while (*p != 0) {
        addressCount++;
        p++;
    }

    jobject address_list = (*env)->NewObjectArray(env, addressCount, hostAddressClass, NULL);

    // Create a new HostAddress object for each address block.
    // First, reset the iterator.
    p = addresses;
    jsize index = 0;
    while (*p != 0) {
        krb5_address *currAddress = *p;

        // HostAddres needs a byte array of the host data.
        jbyteArray ary = (*env)->NewByteArray(env, currAddress->length);

        if (ary == NULL) return NULL;

        (*env)->SetByteArrayRegion(env, ary, (jsize) 0, currAddress->length, (jbyte *)currAddress->contents);
        jobject address = (*env)->NewObject(env, hostAddressClass, hostAddressConstructor, currAddress->length, ary);

        (*env)->DeleteLocalRef(env, ary);

        // Add the HostAddress to the arrray.
        (*env)->SetObjectArrayElement(env, address_list, index, address);

        index++;
        p++;
    }

    return address_list;
}

#pragma mark - Utility methods -

static void printiferr (errcode_t err, const char *format, ...)
{
    if (err) {
        va_list pvar;

        va_start (pvar, format);
        com_err_va ("ticketParser:", err, format, pvar);
        va_end (pvar);
    }
}

