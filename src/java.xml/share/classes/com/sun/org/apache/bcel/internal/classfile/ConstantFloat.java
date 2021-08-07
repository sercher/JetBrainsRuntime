/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * This class is derived from the abstract {@link Constant}
 * and represents a reference to a float object.
 *
 * @see     Constant
 * @LastModified: Jun 2019
 */
public final class ConstantFloat extends Constant implements ConstantObject {

    private float bytes;


    /**
     * @param bytes Data
     */
    public ConstantFloat(final float bytes) {
        super(Const.CONSTANT_Float);
        this.bytes = bytes;
    }


    /**
     * Initialize from another object. Note that both objects use the same
     * references (shallow copy). Use clone() for a physical copy.
     */
    public ConstantFloat(final ConstantFloat c) {
        this(c.getBytes());
    }


    /**
     * Initialize instance from file data.
     *
     * @param file Input stream
     * @throws IOException
     */
    ConstantFloat(final DataInput file) throws IOException {
        this(file.readFloat());
    }


    /**
     * Called by objects that are traversing the nodes of the tree implicitely
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitConstantFloat(this);
    }


    /**
     * Dump constant float to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    @Override
    public void dump( final DataOutputStream file ) throws IOException {
        file.writeByte(super.getTag());
        file.writeFloat(bytes);
    }


    /**
     * @return data, i.e., 4 bytes.
     */
    public float getBytes() {
        return bytes;
    }


    /**
     * @param bytes the raw bytes that represent this float
     */
    public void setBytes( final float bytes ) {
        this.bytes = bytes;
    }


    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return super.toString() + "(bytes = " + bytes + ")";
    }


    /** @return Float object
     */
    @Override
    public Object getConstantValue( final ConstantPool cp ) {
        return bytes;
    }
}
