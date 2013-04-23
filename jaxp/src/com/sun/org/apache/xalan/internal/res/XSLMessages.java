/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: XSLMessages.java,v 1.2.4.1 2005/09/09 07:41:10 pvedula Exp $
 */
package com.sun.org.apache.xalan.internal.res;

import com.sun.org.apache.xalan.internal.utils.SecuritySupport;
import java.util.ListResourceBundle;

import com.sun.org.apache.xpath.internal.res.XPATHMessages;

/**
 * Sets things up for issuing error messages. This class is misnamed, and should
 * be called XalanMessages, or some such.
 *
 * @xsl.usage internal
 */
public class XSLMessages extends XPATHMessages {

    /**
     * The language specific resource object for Xalan messages.
     */
    private static ListResourceBundle XSLTBundle = null;
    /**
     * The class name of the Xalan error message string table.
     */
    private static final String XSLT_ERROR_RESOURCES =
            "com.sun.org.apache.xalan.internal.res.XSLTErrorResources";

    /**
     * Creates a message from the specified key and replacement arguments,
     * localized to the given locale.
     *
     * @param msgKey The key for the message text.
     * @param args The arguments to be used as replacement text in the message
     * created.
     *
     * @return The formatted message string.
     */
    public static String createMessage(String msgKey, Object args[]) //throws Exception
    {
        if (XSLTBundle == null) {
            XSLTBundle = SecuritySupport.getResourceBundle(XSLT_ERROR_RESOURCES);
        }

        if (XSLTBundle != null) {
            return createMsg(XSLTBundle, msgKey, args);
        } else {
            return "Could not load any resource bundles.";
        }
    }

    /**
     * Creates a message from the specified key and replacement arguments,
     * localized to the given locale.
     *
     * @param msgKey The key for the message text.
     * @param args The arguments to be used as replacement text in the message
     * created.
     *
     * @return The formatted warning string.
     */
    public static String createWarning(String msgKey, Object args[]) //throws Exception
    {
        if (XSLTBundle == null) {
            XSLTBundle = SecuritySupport.getResourceBundle(XSLT_ERROR_RESOURCES);
        }

        if (XSLTBundle != null) {
            return createMsg(XSLTBundle, msgKey, args);
        } else {
            return "Could not load any resource bundles.";
        }
    }
}
