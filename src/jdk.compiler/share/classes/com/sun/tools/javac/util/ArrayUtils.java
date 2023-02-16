/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import java.lang.reflect.Array;

/** <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ArrayUtils {

    private static int calculateNewLength(int currentLength, int maxIndex) {
        if (maxIndex == Integer.MAX_VALUE)
            maxIndex--;                         // avoid negative overflow
        while (currentLength < maxIndex + 1) {
            currentLength = currentLength * 2;
            if (currentLength <= 0) {           // avoid infinite loop and negative overflow
                currentLength = maxIndex + 1;
                break;
            }
        }
        return currentLength;
    }

    /**
     * Ensure the given array has length at least {@code maxIndex + 1}.
     *
     * @param array original array
     * @param maxIndex exclusive lower bound for desired length
     * @return possibly reallocated array of length at least {@code maxIndex + 1}
     * @throws NullPointerException if {@code array} is null
     * @throws IllegalArgumentException if {@code maxIndex} is negative
     */
    public static <T> T[] ensureCapacity(T[] array, int maxIndex) {
        if (maxIndex < 0)
            throw new IllegalArgumentException("maxIndex=" + maxIndex);
        if (maxIndex < array.length) {
            return array;
        } else {
            int newLength = calculateNewLength(array.length, maxIndex);
            @SuppressWarnings("unchecked")
            T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), newLength);
            System.arraycopy(array, 0, result, 0, array.length);
            return result;
        }
    }

    /**
     * Ensure the given array has length at least {@code maxIndex + 1}.
     *
     * @param array original array
     * @param maxIndex exclusive lower bound for desired length
     * @return possibly reallocated array of length at least {@code maxIndex + 1}
     * @throws NullPointerException if {@code array} is null
     * @throws IllegalArgumentException if {@code maxIndex} is negative
     */
    public static byte[] ensureCapacity(byte[] array, int maxIndex) {
        if (maxIndex < 0)
            throw new IllegalArgumentException("maxIndex=" + maxIndex);
        if (maxIndex < array.length) {
            return array;
        } else {
            int newLength = calculateNewLength(array.length, maxIndex);
            byte[] result = new byte[newLength];
            System.arraycopy(array, 0, result, 0, array.length);
            return result;
        }
    }

    /**
     * Ensure the given array has length at least {@code maxIndex + 1}.
     *
     * @param array original array
     * @param maxIndex exclusive lower bound for desired length
     * @return possibly reallocated array of length at least {@code maxIndex + 1}
     * @throws NullPointerException if {@code array} is null
     * @throws IllegalArgumentException if {@code maxIndex} is negative
     */
    public static char[] ensureCapacity(char[] array, int maxIndex) {
        if (maxIndex < 0)
            throw new IllegalArgumentException("maxIndex=" + maxIndex);
        if (maxIndex < array.length) {
            return array;
        } else {
            int newLength = calculateNewLength(array.length, maxIndex);
            char[] result = new char[newLength];
            System.arraycopy(array, 0, result, 0, array.length);
            return result;
        }
    }

    /**
     * Ensure the given array has length at least {@code maxIndex + 1}.
     *
     * @param array original array
     * @param maxIndex exclusive lower bound for desired length
     * @return possibly reallocated array of length at least {@code maxIndex + 1}
     * @throws NullPointerException if {@code array} is null
     * @throws IllegalArgumentException if {@code maxIndex} is negative
     */
    public static int[] ensureCapacity(int[] array, int maxIndex) {
        if (maxIndex < 0)
            throw new IllegalArgumentException("maxIndex=" + maxIndex);
        if (maxIndex < array.length) {
            return array;
        } else {
            int newLength = calculateNewLength(array.length, maxIndex);
            int[] result = new int[newLength];
            System.arraycopy(array, 0, result, 0, array.length);
            return result;
        }
    }

}
