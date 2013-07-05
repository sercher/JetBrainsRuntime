/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.keys.keyresolver;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;

public class InvalidKeyResolverException extends XMLSecurityException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor InvalidKeyResolverException
     *
     */
    public InvalidKeyResolverException() {
        super();
    }

    /**
     * Constructor InvalidKeyResolverException
     *
     * @param msgID
     */
    public InvalidKeyResolverException(String msgID) {
        super(msgID);
    }

    /**
     * Constructor InvalidKeyResolverException
     *
     * @param msgID
     * @param exArgs
     */
    public InvalidKeyResolverException(String msgID, Object exArgs[]) {
        super(msgID, exArgs);
    }

    /**
     * Constructor InvalidKeyResolverException
     *
     * @param msgID
     * @param originalException
     */
    public InvalidKeyResolverException(String msgID, Exception originalException) {
        super(msgID, originalException);
    }

    /**
     * Constructor InvalidKeyResolverException
     *
     * @param msgID
     * @param exArgs
     * @param originalException
     */
    public InvalidKeyResolverException(String msgID, Object exArgs[], Exception originalException) {
        super(msgID, exArgs, originalException);
    }
}
