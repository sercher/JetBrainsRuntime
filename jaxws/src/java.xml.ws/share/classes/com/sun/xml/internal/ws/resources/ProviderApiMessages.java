/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.xml.internal.ws.resources;

import java.util.Locale;
import java.util.ResourceBundle;
import javax.annotation.Generated;
import com.sun.istack.internal.localization.Localizable;
import com.sun.istack.internal.localization.LocalizableMessageFactory;
import com.sun.istack.internal.localization.LocalizableMessageFactory.ResourceBundleSupplier;
import com.sun.istack.internal.localization.Localizer;


/**
 * Defines string formatting method for each constant in the resource file
 *
 */
@Generated("com.sun.istack.internal.maven.ResourceGenMojo")
public final class ProviderApiMessages {

    private final static String BUNDLE_NAME = "com.sun.xml.internal.ws.resources.providerApi";
    private final static LocalizableMessageFactory MESSAGE_FACTORY = new LocalizableMessageFactory(BUNDLE_NAME, new ProviderApiMessages.BundleSupplier());
    private final static Localizer LOCALIZER = new Localizer();

    public static Localizable localizableNULL_SERVICE() {
        return MESSAGE_FACTORY.getMessage("null.service");
    }

    /**
     * serviceName can't be null when portName is specified
     *
     */
    public static String NULL_SERVICE() {
        return LOCALIZER.localize(localizableNULL_SERVICE());
    }

    public static Localizable localizableNULL_ADDRESS_SERVICE_ENDPOINT() {
        return MESSAGE_FACTORY.getMessage("null.address.service.endpoint");
    }

    /**
     * Address in an EPR cannot be null, when serviceName or portName is null
     *
     */
    public static String NULL_ADDRESS_SERVICE_ENDPOINT() {
        return LOCALIZER.localize(localizableNULL_ADDRESS_SERVICE_ENDPOINT());
    }

    public static Localizable localizableNULL_PORTNAME() {
        return MESSAGE_FACTORY.getMessage("null.portname");
    }

    /**
     * EPR doesn't have EndpointName in the Metadata
     *
     */
    public static String NULL_PORTNAME() {
        return LOCALIZER.localize(localizableNULL_PORTNAME());
    }

    public static Localizable localizableNULL_WSDL() {
        return MESSAGE_FACTORY.getMessage("null.wsdl");
    }

    /**
     * EPR doesn't have WSDL Metadata which is needed for the current operation
     *
     */
    public static String NULL_WSDL() {
        return LOCALIZER.localize(localizableNULL_WSDL());
    }

    public static Localizable localizableNO_WSDL_NO_PORT(Object arg0) {
        return MESSAGE_FACTORY.getMessage("no.wsdl.no.port", arg0);
    }

    /**
     * WSDL Metadata not available to create the proxy, either Service instance or ServiceEndpointInterface {0} should have WSDL information
     *
     */
    public static String NO_WSDL_NO_PORT(Object arg0) {
        return LOCALIZER.localize(localizableNO_WSDL_NO_PORT(arg0));
    }

    public static Localizable localizableNOTFOUND_PORT_IN_WSDL(Object arg0, Object arg1, Object arg2) {
        return MESSAGE_FACTORY.getMessage("notfound.port.in.wsdl", arg0, arg1, arg2);
    }

    /**
     * Port: {0} not a valid port in Service: {1} in WSDL: {2}
     *
     */
    public static String NOTFOUND_PORT_IN_WSDL(Object arg0, Object arg1, Object arg2) {
        return LOCALIZER.localize(localizableNOTFOUND_PORT_IN_WSDL(arg0, arg1, arg2));
    }

    public static Localizable localizableNOTFOUND_SERVICE_IN_WSDL(Object arg0, Object arg1) {
        return MESSAGE_FACTORY.getMessage("notfound.service.in.wsdl", arg0, arg1);
    }

    /**
     * Service: {0} not found in WSDL: {1}
     *
     */
    public static String NOTFOUND_SERVICE_IN_WSDL(Object arg0, Object arg1) {
        return LOCALIZER.localize(localizableNOTFOUND_SERVICE_IN_WSDL(arg0, arg1));
    }

    public static Localizable localizableNULL_EPR() {
        return MESSAGE_FACTORY.getMessage("null.epr");
    }

    /**
     * EndpointReference is null
     *
     */
    public static String NULL_EPR() {
        return LOCALIZER.localize(localizableNULL_EPR());
    }

    public static Localizable localizableNULL_ADDRESS() {
        return MESSAGE_FACTORY.getMessage("null.address");
    }

    /**
     * Address in an EPR cannot be null
     *
     */
    public static String NULL_ADDRESS() {
        return LOCALIZER.localize(localizableNULL_ADDRESS());
    }

    public static Localizable localizableERROR_WSDL(Object arg0) {
        return MESSAGE_FACTORY.getMessage("error.wsdl", arg0);
    }

    /**
     * Error in parsing WSDL: {0}
     *
     */
    public static String ERROR_WSDL(Object arg0) {
        return LOCALIZER.localize(localizableERROR_WSDL(arg0));
    }

    private static class BundleSupplier
        implements ResourceBundleSupplier
    {


        public ResourceBundle getResourceBundle(Locale locale) {
            return ResourceBundle.getBundle(BUNDLE_NAME, locale);
        }

    }

}
