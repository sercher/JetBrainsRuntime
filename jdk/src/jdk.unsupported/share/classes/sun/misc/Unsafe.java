/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.misc;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.misc.VM;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

import java.lang.reflect.Field;
import java.security.ProtectionDomain;


/**
 * A collection of methods for performing low-level, unsafe operations.
 * Although the class and all methods are public, use of this class is
 * limited because only trusted code can obtain instances of it.
 *
 * <em>Note:</em> It is the resposibility of the caller to make sure
 * arguments are checked before methods of this class are
 * called. While some rudimentary checks are performed on the input,
 * the checks are best effort and when performance is an overriding
 * priority, as when methods of this class are optimized by the
 * runtime compiler, some or all checks (if any) may be elided. Hence,
 * the caller must not rely on the checks and corresponding
 * exceptions!
 *
 * @author John R. Rose
 * @see #getUnsafe
 */

public final class Unsafe {

    static {
        Reflection.registerMethodsToFilter(Unsafe.class, "getUnsafe");
    }

    private Unsafe() {}

    private static final Unsafe theUnsafe = new Unsafe();
    private static final jdk.internal.misc.Unsafe theInternalUnsafe = jdk.internal.misc.Unsafe.getUnsafe();

    /**
     * Provides the caller with the capability of performing unsafe
     * operations.
     *
     * <p>The returned {@code Unsafe} object should be carefully guarded
     * by the caller, since it can be used to read and write data at arbitrary
     * memory addresses.  It must never be passed to untrusted code.
     *
     * <p>Most methods in this class are very low-level, and correspond to a
     * small number of hardware instructions (on typical machines).  Compilers
     * are encouraged to optimize these methods accordingly.
     *
     * <p>Here is a suggested idiom for using unsafe operations:
     *
     * <pre> {@code
     * class MyTrustedClass {
     *   private static final Unsafe unsafe = Unsafe.getUnsafe();
     *   ...
     *   private long myCountAddress = ...;
     *   public int getCount() { return unsafe.getByte(myCountAddress); }
     * }}</pre>
     *
     * (It may assist compilers to make the local variable {@code final}.)
     *
     * @throws  SecurityException  if a security manager exists and its
     *          {@code checkPropertiesAccess} method doesn't allow
     *          access to the system properties.
     */
    @CallerSensitive
    public static Unsafe getUnsafe() {
        Class<?> caller = Reflection.getCallerClass();
        if (!VM.isSystemDomainLoader(caller.getClassLoader()))
            throw new SecurityException("Unsafe");
        return theUnsafe;
    }

    /// peek and poke operations
    /// (compilers should optimize these to memory ops)

    // These work on object fields in the Java heap.
    // They will not work on elements of packed arrays.

    /**
     * Fetches a value from a given Java variable.
     * More specifically, fetches a field or array element within the given
     * object {@code o} at the given offset, or (if {@code o} is null)
     * from the memory address whose numerical value is the given offset.
     * <p>
     * The results are undefined unless one of the following cases is true:
     * <ul>
     * <li>The offset was obtained from {@link #objectFieldOffset} on
     * the {@link java.lang.reflect.Field} of some Java field and the object
     * referred to by {@code o} is of a class compatible with that
     * field's class.
     *
     * <li>The offset and object reference {@code o} (either null or
     * non-null) were both obtained via {@link #staticFieldOffset}
     * and {@link #staticFieldBase} (respectively) from the
     * reflective {@link Field} representation of some Java field.
     *
     * <li>The object referred to by {@code o} is an array, and the offset
     * is an integer of the form {@code B+N*S}, where {@code N} is
     * a valid index into the array, and {@code B} and {@code S} are
     * the values obtained by {@link #arrayBaseOffset} and {@link
     * #arrayIndexScale} (respectively) from the array's class.  The value
     * referred to is the {@code N}<em>th</em> element of the array.
     *
     * </ul>
     * <p>
     * If one of the above cases is true, the call references a specific Java
     * variable (field or array element).  However, the results are undefined
     * if that variable is not in fact of the type returned by this method.
     * <p>
     * This method refers to a variable by means of two parameters, and so
     * it provides (in effect) a <em>double-register</em> addressing mode
     * for Java variables.  When the object reference is null, this method
     * uses its offset as an absolute address.  This is similar in operation
     * to methods such as {@link #getInt(long)}, which provide (in effect) a
     * <em>single-register</em> addressing mode for non-Java variables.
     * However, because Java variables may have a different layout in memory
     * from non-Java variables, programmers should not assume that these
     * two addressing modes are ever equivalent.  Also, programmers should
     * remember that offsets from the double-register addressing mode cannot
     * be portably confused with longs used in the single-register addressing
     * mode.
     *
     * @param o Java heap object in which the variable resides, if any, else
     *        null
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     * @return the value fetched from the indicated Java variable
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     */
    @ForceInline
    public int getInt(Object o, long offset) {
        return theInternalUnsafe.getInt(o, offset);
    }

    /**
     * Stores a value into a given Java variable.
     * <p>
     * The first two parameters are interpreted exactly as with
     * {@link #getInt(Object, long)} to refer to a specific
     * Java variable (field or array element).  The given value
     * is stored into that variable.
     * <p>
     * The variable must be of the same type as the method
     * parameter {@code x}.
     *
     * @param o Java heap object in which the variable resides, if any, else
     *        null
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     * @param x the value to store into the indicated Java variable
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     */
    @ForceInline
    public void putInt(Object o, long offset, int x) {
        theInternalUnsafe.putInt(o, offset, x);
    }

    /**
     * Fetches a reference value from a given Java variable.
     * @see #getInt(Object, long)
     */
    @ForceInline
    public Object getObject(Object o, long offset) {
        return theInternalUnsafe.getObject(o, offset);
    }

    /**
     * Stores a reference value into a given Java variable.
     * <p>
     * Unless the reference {@code x} being stored is either null
     * or matches the field type, the results are undefined.
     * If the reference {@code o} is non-null, card marks or
     * other store barriers for that object (if the VM requires them)
     * are updated.
     * @see #putInt(Object, long, int)
     */
    @ForceInline
    public void putObject(Object o, long offset, Object x) {
        theInternalUnsafe.putObject(o, offset, x);
    }

    /** @see #getInt(Object, long) */
    @ForceInline
    public boolean getBoolean(Object o, long offset) {
        return theInternalUnsafe.getBoolean(o, offset);
    }

    /** @see #putInt(Object, long, int) */
    @ForceInline
    public void putBoolean(Object o, long offset, boolean x) {
        theInternalUnsafe.putBoolean(o, offset, x);
    }

    /** @see #getInt(Object, long) */
    @ForceInline
    public byte getByte(Object o, long offset) {
        return theInternalUnsafe.getByte(o, offset);
    }

    /** @see #putInt(Object, long, int) */
    @ForceInline
    public void putByte(Object o, long offset, byte x) {
        theInternalUnsafe.putByte(o, offset, x);
    }

    /** @see #getInt(Object, long) */
    @ForceInline
    public short getShort(Object o, long offset) {
        return theInternalUnsafe.getShort(o, offset);
    }

    /** @see #putInt(Object, long, int) */
    @ForceInline
    public void putShort(Object o, long offset, short x) {
        theInternalUnsafe.putShort(o, offset, x);
    }

    /** @see #getInt(Object, long) */
    @ForceInline
    public char getChar(Object o, long offset) {
        return theInternalUnsafe.getChar(o, offset);
    }

    /** @see #putInt(Object, long, int) */
    @ForceInline
    public void putChar(Object o, long offset, char x) {
        theInternalUnsafe.putChar(o, offset, x);
    }

    /** @see #getInt(Object, long) */
    @ForceInline
    public long getLong(Object o, long offset) {
        return theInternalUnsafe.getLong(o, offset);
    }

    /** @see #putInt(Object, long, int) */
    @ForceInline
    public void putLong(Object o, long offset, long x) {
        theInternalUnsafe.putLong(o, offset, x);
    }

    /** @see #getInt(Object, long) */
    @ForceInline
    public float getFloat(Object o, long offset) {
        return theInternalUnsafe.getFloat(o, offset);
    }

    /** @see #putInt(Object, long, int) */
    @ForceInline
    public void putFloat(Object o, long offset, float x) {
        theInternalUnsafe.putFloat(o, offset, x);
    }

    /** @see #getInt(Object, long) */
    @ForceInline
    public double getDouble(Object o, long offset) {
        return theInternalUnsafe.getDouble(o, offset);
    }

    /** @see #putInt(Object, long, int) */
    @ForceInline
    public void putDouble(Object o, long offset, double x) {
        theInternalUnsafe.putDouble(o, offset, x);
    }


    // These read VM internal data.

    /**
     * Fetches an uncompressed reference value from a given native variable
     * ignoring the VM's compressed references mode.
     *
     * @param address a memory address locating the variable
     * @return the value fetched from the indicated native variable
     */
    @ForceInline
    public Object getUncompressedObject(long address) {
        return theInternalUnsafe.getUncompressedObject(address);
    }

    /**
     * Fetches the {@link java.lang.Class} Java mirror for the given native
     * metaspace {@code Klass} pointer.
     *
     * @param metaspaceKlass a native metaspace {@code Klass} pointer
     * @return the {@link java.lang.Class} Java mirror
     */
    @ForceInline
    public Class<?> getJavaMirror(long metaspaceKlass) {
        return theInternalUnsafe.getJavaMirror(metaspaceKlass);
    }

    /**
     * Fetches a native metaspace {@code Klass} pointer for the given Java
     * object.
     *
     * @param o Java heap object for which to fetch the class pointer
     * @return a native metaspace {@code Klass} pointer
     */
    @ForceInline
    public long getKlassPointer(Object o) {
        return theInternalUnsafe.getKlassPointer(o);
    }

    // These work on values in the C heap.

    /**
     * Fetches a value from a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     *
     * @see #allocateMemory
     */
    @ForceInline
    public byte getByte(long address) {
        return theInternalUnsafe.getByte(address);
    }

    /**
     * Stores a value into a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     *
     * @see #getByte(long)
     */
    @ForceInline
    public void putByte(long address, byte x) {
        theInternalUnsafe.putByte(address, x);
    }

    /** @see #getByte(long) */
    @ForceInline
    public short getShort(long address) {
        return theInternalUnsafe.getShort(address);
    }

    /** @see #putByte(long, byte) */
    @ForceInline
    public void putShort(long address, short x) {
        theInternalUnsafe.putShort(address, x);
    }

    /** @see #getByte(long) */
    @ForceInline
    public char getChar(long address) {
        return theInternalUnsafe.getChar(address);
    }

    /** @see #putByte(long, byte) */
    @ForceInline
    public void putChar(long address, char x) {
        theInternalUnsafe.putChar(address, x);
    }

    /** @see #getByte(long) */
    @ForceInline
    public int getInt(long address) {
        return theInternalUnsafe.getInt(address);
    }

    /** @see #putByte(long, byte) */
    @ForceInline
    public void putInt(long address, int x) {
        theInternalUnsafe.putInt(address, x);
    }

    /** @see #getByte(long) */
    @ForceInline
    public long getLong(long address) {
        return theInternalUnsafe.getLong(address);
    }

    /** @see #putByte(long, byte) */
    @ForceInline
    public void putLong(long address, long x) {
        theInternalUnsafe.putLong(address, x);
    }

    /** @see #getByte(long) */
    @ForceInline
    public float getFloat(long address) {
        return theInternalUnsafe.getFloat(address);
    }

    /** @see #putByte(long, byte) */
    @ForceInline
    public void putFloat(long address, float x) {
        theInternalUnsafe.putFloat(address, x);
    }

    /** @see #getByte(long) */
    @ForceInline
    public double getDouble(long address) {
        return theInternalUnsafe.getDouble(address);
    }

    /** @see #putByte(long, byte) */
    @ForceInline
    public void putDouble(long address, double x) {
        theInternalUnsafe.putDouble(address, x);
    }


    /**
     * Fetches a native pointer from a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     *
     * <p>If the native pointer is less than 64 bits wide, it is extended as
     * an unsigned number to a Java long.  The pointer may be indexed by any
     * given byte offset, simply by adding that offset (as a simple integer) to
     * the long representing the pointer.  The number of bytes actually read
     * from the target address may be determined by consulting {@link
     * #addressSize}.
     *
     * @see #allocateMemory
     */
    @ForceInline
    public long getAddress(long address) {
        return theInternalUnsafe.getAddress(address);
    }

    /**
     * Stores a native pointer into a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     *
     * <p>The number of bytes actually written at the target address may be
     * determined by consulting {@link #addressSize}.
     *
     * @see #getAddress(long)
     */
    @ForceInline
    public void putAddress(long address, long x) {
        theInternalUnsafe.putAddress(address, x);
    }


    /// wrappers for malloc, realloc, free:

    /**
     * Allocates a new block of native memory, of the given size in bytes.  The
     * contents of the memory are uninitialized; they will generally be
     * garbage.  The resulting native pointer will never be zero, and will be
     * aligned for all value types.  Dispose of this memory by calling {@link
     * #freeMemory}, or resize it with {@link #reallocateMemory}.
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if the size is negative or too large
     *         for the native size_t type
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *
     * @see #getByte(long)
     * @see #putByte(long, byte)
     */
    @ForceInline
    public long allocateMemory(long bytes) {
        return theInternalUnsafe.allocateMemory(bytes);
    }

    /**
     * Resizes a new block of native memory, to the given size in bytes.  The
     * contents of the new block past the size of the old block are
     * uninitialized; they will generally be garbage.  The resulting native
     * pointer will be zero if and only if the requested size is zero.  The
     * resulting native pointer will be aligned for all value types.  Dispose
     * of this memory by calling {@link #freeMemory}, or resize it with {@link
     * #reallocateMemory}.  The address passed to this method may be null, in
     * which case an allocation will be performed.
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if the size is negative or too large
     *         for the native size_t type
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *
     * @see #allocateMemory
     */
    @ForceInline
    public long reallocateMemory(long address, long bytes) {
        return theInternalUnsafe.reallocateMemory(address, bytes);
    }

    /**
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).
     *
     * <p>This method determines a block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * <p>The stores are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective address and
     * length are all even modulo 8, the stores take place in 'long' units.
     * If the effective address and length are (resp.) even modulo 4 or 2,
     * the stores take place in units of 'int' or 'short'.
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if any of the arguments is invalid
     *
     * @since 1.7
     */
    @ForceInline
    public void setMemory(Object o, long offset, long bytes, byte value) {
        theInternalUnsafe.setMemory(o, offset, bytes, value);
    }

    /**
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).  This provides a <em>single-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.
     *
     * <p>Equivalent to {@code setMemory(null, address, bytes, value)}.
     */
    @ForceInline
    public void setMemory(long address, long bytes, byte value) {
        theInternalUnsafe.setMemory(address, bytes, value);
    }

    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.
     *
     * <p>This method determines each block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * <p>The transfers are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective addresses and
     * length are all even modulo 8, the transfer takes place in 'long' units.
     * If the effective addresses and length are (resp.) even modulo 4 or 2,
     * the transfer takes place in units of 'int' or 'short'.
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if any of the arguments is invalid
     *
     * @since 1.7
     */
    @ForceInline
    public void copyMemory(Object srcBase, long srcOffset,
                           Object destBase, long destOffset,
                           long bytes) {
        theInternalUnsafe.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
    }

    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.  This provides a <em>single-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.
     *
     * Equivalent to {@code copyMemory(null, srcAddress, null, destAddress, bytes)}.
     */
    @ForceInline
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        theInternalUnsafe.copyMemory(srcAddress, destAddress, bytes);
    }

    /**
     * Disposes of a block of native memory, as obtained from {@link
     * #allocateMemory} or {@link #reallocateMemory}.  The address passed to
     * this method may be null, in which case no action is taken.
     *
     * <em>Note:</em> It is the resposibility of the caller to make
     * sure arguments are checked before the methods are called. While
     * some rudimentary checks are performed on the input, the checks
     * are best effort and when performance is an overriding priority,
     * as when methods of this class are optimized by the runtime
     * compiler, some or all checks (if any) may be elided. Hence, the
     * caller must not rely on the checks and corresponding
     * exceptions!
     *
     * @throws RuntimeException if any of the arguments is invalid
     *
     * @see #allocateMemory
     */
    @ForceInline
    public void freeMemory(long address) {
        theInternalUnsafe.freeMemory(address);
    }

    /// random queries

    /**
     * This constant differs from all results that will ever be returned from
     * {@link #staticFieldOffset}, {@link #objectFieldOffset},
     * or {@link #arrayBaseOffset}.
     */
    public static final int INVALID_FIELD_OFFSET = jdk.internal.misc.Unsafe.INVALID_FIELD_OFFSET;

    /**
     * Reports the location of a given field in the storage allocation of its
     * class.  Do not expect to perform any sort of arithmetic on this offset;
     * it is just a cookie which is passed to the unsafe heap memory accessors.
     *
     * <p>Any given field will always have the same offset and base, and no
     * two distinct fields of the same class will ever have the same offset
     * and base.
     *
     * <p>As of 1.4.1, offsets for fields are represented as long values,
     * although the Sun JVM does not use the most significant 32 bits.
     * However, JVM implementations which store static fields at absolute
     * addresses can use long offsets and null base pointers to express
     * the field locations in a form usable by {@link #getInt(Object,long)}.
     * Therefore, code which will be ported to such JVMs on 64-bit platforms
     * must preserve all bits of static field offsets.
     * @see #getInt(Object, long)
     */
    @ForceInline
    public long objectFieldOffset(Field f) {
        return theInternalUnsafe.objectFieldOffset(f);
    }

    /**
     * Reports the location of a given static field, in conjunction with {@link
     * #staticFieldBase}.
     * <p>Do not expect to perform any sort of arithmetic on this offset;
     * it is just a cookie which is passed to the unsafe heap memory accessors.
     *
     * <p>Any given field will always have the same offset, and no two distinct
     * fields of the same class will ever have the same offset.
     *
     * <p>As of 1.4.1, offsets for fields are represented as long values,
     * although the Sun JVM does not use the most significant 32 bits.
     * It is hard to imagine a JVM technology which needs more than
     * a few bits to encode an offset within a non-array object,
     * However, for consistency with other methods in this class,
     * this method reports its result as a long value.
     * @see #getInt(Object, long)
     */
    @ForceInline
    public long staticFieldOffset(Field f) {
        return theInternalUnsafe.staticFieldOffset(f);
    }

    /**
     * Reports the location of a given static field, in conjunction with {@link
     * #staticFieldOffset}.
     * <p>Fetch the base "Object", if any, with which static fields of the
     * given class can be accessed via methods like {@link #getInt(Object,
     * long)}.  This value may be null.  This value may refer to an object
     * which is a "cookie", not guaranteed to be a real Object, and it should
     * not be used in any way except as argument to the get and put routines in
     * this class.
     */
    @ForceInline
    public Object staticFieldBase(Field f) {
        return theInternalUnsafe.staticFieldBase(f);
    }

    /**
     * Detects if the given class may need to be initialized. This is often
     * needed in conjunction with obtaining the static field base of a
     * class.
     * @return false only if a call to {@code ensureClassInitialized} would have no effect
     */
    @ForceInline
    public boolean shouldBeInitialized(Class<?> c) {
        return theInternalUnsafe.shouldBeInitialized(c);
    }

    /**
     * Ensures the given class has been initialized. This is often
     * needed in conjunction with obtaining the static field base of a
     * class.
     */
    @ForceInline
    public void ensureClassInitialized(Class<?> c) {
        theInternalUnsafe.ensureClassInitialized(c);
    }

    /**
     * Reports the offset of the first element in the storage allocation of a
     * given array class.  If {@link #arrayIndexScale} returns a non-zero value
     * for the same class, you may use that scale factor, together with this
     * base offset, to form new offsets to access elements of arrays of the
     * given class.
     *
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    @ForceInline
    public int arrayBaseOffset(Class<?> arrayClass) {
        return theInternalUnsafe.arrayBaseOffset(arrayClass);
    }

    /** The value of {@code arrayBaseOffset(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_BOOLEAN_BASE_OFFSET;

    /** The value of {@code arrayBaseOffset(byte[].class)} */
    public static final int ARRAY_BYTE_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

    /** The value of {@code arrayBaseOffset(short[].class)} */
    public static final int ARRAY_SHORT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET;

    /** The value of {@code arrayBaseOffset(char[].class)} */
    public static final int ARRAY_CHAR_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_CHAR_BASE_OFFSET;

    /** The value of {@code arrayBaseOffset(int[].class)} */
    public static final int ARRAY_INT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_INT_BASE_OFFSET;

    /** The value of {@code arrayBaseOffset(long[].class)} */
    public static final int ARRAY_LONG_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_LONG_BASE_OFFSET;

    /** The value of {@code arrayBaseOffset(float[].class)} */
    public static final int ARRAY_FLOAT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_FLOAT_BASE_OFFSET;

    /** The value of {@code arrayBaseOffset(double[].class)} */
    public static final int ARRAY_DOUBLE_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_DOUBLE_BASE_OFFSET;

    /** The value of {@code arrayBaseOffset(Object[].class)} */
    public static final int ARRAY_OBJECT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_OBJECT_BASE_OFFSET;

    /**
     * Reports the scale factor for addressing elements in the storage
     * allocation of a given array class.  However, arrays of "narrow" types
     * will generally not work properly with accessors like {@link
     * #getByte(Object, long)}, so the scale factor for such classes is reported
     * as zero.
     *
     * @see #arrayBaseOffset
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    @ForceInline
    public int arrayIndexScale(Class<?> arrayClass) {
        return theInternalUnsafe.arrayIndexScale(arrayClass);
    }

    /** The value of {@code arrayIndexScale(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_BOOLEAN_INDEX_SCALE;

    /** The value of {@code arrayIndexScale(byte[].class)} */
    public static final int ARRAY_BYTE_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_BYTE_INDEX_SCALE;

    /** The value of {@code arrayIndexScale(short[].class)} */
    public static final int ARRAY_SHORT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_SHORT_INDEX_SCALE;

    /** The value of {@code arrayIndexScale(char[].class)} */
    public static final int ARRAY_CHAR_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_CHAR_INDEX_SCALE;

    /** The value of {@code arrayIndexScale(int[].class)} */
    public static final int ARRAY_INT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_INT_INDEX_SCALE;

    /** The value of {@code arrayIndexScale(long[].class)} */
    public static final int ARRAY_LONG_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_LONG_INDEX_SCALE;

    /** The value of {@code arrayIndexScale(float[].class)} */
    public static final int ARRAY_FLOAT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_FLOAT_INDEX_SCALE;

    /** The value of {@code arrayIndexScale(double[].class)} */
    public static final int ARRAY_DOUBLE_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_DOUBLE_INDEX_SCALE;

    /** The value of {@code arrayIndexScale(Object[].class)} */
    public static final int ARRAY_OBJECT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_OBJECT_INDEX_SCALE;

    /**
     * Reports the size in bytes of a native pointer, as stored via {@link
     * #putAddress}.  This value will be either 4 or 8.  Note that the sizes of
     * other primitive types (as stored in native memory blocks) is determined
     * fully by their information content.
     */
    @ForceInline
    public int addressSize() {
        return theInternalUnsafe.addressSize();
    }

    /** The value of {@code addressSize()} */
    public static final int ADDRESS_SIZE = theInternalUnsafe.addressSize();

    /**
     * Reports the size in bytes of a native memory page (whatever that is).
     * This value will always be a power of two.
     */
    @ForceInline
    public int pageSize() {
        return theInternalUnsafe.pageSize();
    }


    /// random trusted operations from JNI:

    /**
     * Tells the VM to define a class, without security checks.  By default, the
     * class loader and protection domain come from the caller's class.
     */
    @ForceInline
    public Class<?> defineClass(String name, byte[] b, int off, int len,
                                ClassLoader loader,
                                ProtectionDomain protectionDomain) {
        return theInternalUnsafe.defineClass(name, b, off, len, loader, protectionDomain);
    }

    /**
     * Defines a class but does not make it known to the class loader or system dictionary.
     * <p>
     * For each CP entry, the corresponding CP patch must either be null or have
     * the a format that matches its tag:
     * <ul>
     * <li>Integer, Long, Float, Double: the corresponding wrapper object type from java.lang
     * <li>Utf8: a string (must have suitable syntax if used as signature or name)
     * <li>Class: any java.lang.Class object
     * <li>String: any object (not just a java.lang.String)
     * <li>InterfaceMethodRef: (NYI) a method handle to invoke on that call site's arguments
     * </ul>
     * @param hostClass context for linkage, access control, protection domain, and class loader
     * @param data      bytes of a class file
     * @param cpPatches where non-null entries exist, they replace corresponding CP entries in data
     */
    @ForceInline
    public Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches) {
        return theInternalUnsafe.defineAnonymousClass(hostClass, data, cpPatches);
    }

    /**
     * Allocates an instance but does not run any constructor.
     * Initializes the class if it has not yet been.
     */
    @ForceInline
    public Object allocateInstance(Class<?> cls)
        throws InstantiationException {
        return theInternalUnsafe.allocateInstance(cls);
    }

    /** Throws the exception without telling the verifier. */
    @ForceInline
    public void throwException(Throwable ee) {
        theInternalUnsafe.throwException(ee);
    }

    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    @ForceInline
    public final boolean compareAndSwapObject(Object o, long offset,
                                              Object expected,
                                              Object x) {
        return theInternalUnsafe.compareAndSwapObject(o, offset, expected, x);
    }

    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    @ForceInline
    public final boolean compareAndSwapInt(Object o, long offset,
                                           int expected,
                                           int x) {
        return theInternalUnsafe.compareAndSwapInt(o, offset, expected, x);
    }

    /**
     * Atomically updates Java variable to {@code x} if it is currently
     * holding {@code expected}.
     *
     * <p>This operation has memory semantics of a {@code volatile} read
     * and write.  Corresponds to C11 atomic_compare_exchange_strong.
     *
     * @return {@code true} if successful
     */
    @ForceInline
    public final boolean compareAndSwapLong(Object o, long offset,
                                            long expected,
                                            long x) {
        return theInternalUnsafe.compareAndSwapLong(o, offset, expected, x);
    }

    /**
     * Fetches a reference value from a given Java variable, with volatile
     * load semantics. Otherwise identical to {@link #getObject(Object, long)}
     */
    @ForceInline
    public Object getObjectVolatile(Object o, long offset) {
        return theInternalUnsafe.getObjectVolatile(o, offset);
    }

    /**
     * Stores a reference value into a given Java variable, with
     * volatile store semantics. Otherwise identical to {@link #putObject(Object, long, Object)}
     */
    @ForceInline
    public void putObjectVolatile(Object o, long offset, Object x) {
        theInternalUnsafe.putObjectVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getInt(Object, long)}  */
    @ForceInline
    public int getIntVolatile(Object o, long offset) {
        return theInternalUnsafe.getIntVolatile(o, offset);
    }

    /** Volatile version of {@link #putInt(Object, long, int)}  */
    @ForceInline
    public void putIntVolatile(Object o, long offset, int x) {
        theInternalUnsafe.putIntVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getBoolean(Object, long)}  */
    @ForceInline
    public boolean getBooleanVolatile(Object o, long offset) {
        return theInternalUnsafe.getBooleanVolatile(o, offset);
    }

    /** Volatile version of {@link #putBoolean(Object, long, boolean)}  */
    @ForceInline
    public void putBooleanVolatile(Object o, long offset, boolean x) {
        theInternalUnsafe.putBooleanVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getByte(Object, long)}  */
    @ForceInline
    public byte getByteVolatile(Object o, long offset) {
        return theInternalUnsafe.getByteVolatile(o, offset);
    }

    /** Volatile version of {@link #putByte(Object, long, byte)}  */
    @ForceInline
    public void putByteVolatile(Object o, long offset, byte x) {
        theInternalUnsafe.putByteVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getShort(Object, long)}  */
    @ForceInline
    public short getShortVolatile(Object o, long offset) {
        return theInternalUnsafe.getShortVolatile(o, offset);
    }

    /** Volatile version of {@link #putShort(Object, long, short)}  */
    @ForceInline
    public void putShortVolatile(Object o, long offset, short x) {
        theInternalUnsafe.putShortVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getChar(Object, long)}  */
    @ForceInline
    public char getCharVolatile(Object o, long offset) {
        return theInternalUnsafe.getCharVolatile(o, offset);
    }

    /** Volatile version of {@link #putChar(Object, long, char)}  */
    @ForceInline
    public void putCharVolatile(Object o, long offset, char x) {
        theInternalUnsafe.putCharVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getLong(Object, long)}  */
    @ForceInline
    public long getLongVolatile(Object o, long offset) {
        return theInternalUnsafe.getLongVolatile(o, offset);
    }

    /** Volatile version of {@link #putLong(Object, long, long)}  */
    @ForceInline
    public void putLongVolatile(Object o, long offset, long x) {
        theInternalUnsafe.putLongVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getFloat(Object, long)}  */
    @ForceInline
    public float getFloatVolatile(Object o, long offset) {
        return theInternalUnsafe.getFloatVolatile(o, offset);
    }

    /** Volatile version of {@link #putFloat(Object, long, float)}  */
    @ForceInline
    public void putFloatVolatile(Object o, long offset, float x) {
        theInternalUnsafe.putFloatVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getDouble(Object, long)}  */
    @ForceInline
    public double getDoubleVolatile(Object o, long offset) {
        return theInternalUnsafe.getDoubleVolatile(o, offset);
    }

    /** Volatile version of {@link #putDouble(Object, long, double)}  */
    @ForceInline
    public void putDoubleVolatile(Object o, long offset, double x) {
        theInternalUnsafe.putDoubleVolatile(o, offset, x);
    }

    /**
     * Version of {@link #putObjectVolatile(Object, long, Object)}
     * that does not guarantee immediate visibility of the store to
     * other threads. This method is generally only useful if the
     * underlying field is a Java volatile (or if an array cell, one
     * that is otherwise only accessed using volatile accesses).
     *
     * Corresponds to C11 atomic_store_explicit(..., memory_order_release).
     */
    @ForceInline
    public void putOrderedObject(Object o, long offset, Object x) {
        theInternalUnsafe.putObjectRelease(o, offset, x);
    }

    /** Ordered/Lazy version of {@link #putIntVolatile(Object, long, int)}  */
    @ForceInline
    public void putOrderedInt(Object o, long offset, int x) {
        theInternalUnsafe.putIntRelease(o, offset, x);
    }

    /** Ordered/Lazy version of {@link #putLongVolatile(Object, long, long)} */
    @ForceInline
    public void putOrderedLong(Object o, long offset, long x) {
        theInternalUnsafe.putLongRelease(o, offset, x);
    }

    /**
     * Unblocks the given thread blocked on {@code park}, or, if it is
     * not blocked, causes the subsequent call to {@code park} not to
     * block.  Note: this operation is "unsafe" solely because the
     * caller must somehow ensure that the thread has not been
     * destroyed. Nothing special is usually required to ensure this
     * when called from Java (in which there will ordinarily be a live
     * reference to the thread) but this is not nearly-automatically
     * so when calling from native code.
     *
     * @param thread the thread to unpark.
     */
    @ForceInline
    public void unpark(Object thread) {
        theInternalUnsafe.unpark(thread);
    }

    /**
     * Blocks current thread, returning when a balancing
     * {@code unpark} occurs, or a balancing {@code unpark} has
     * already occurred, or the thread is interrupted, or, if not
     * absolute and time is not zero, the given time nanoseconds have
     * elapsed, or if absolute, the given deadline in milliseconds
     * since Epoch has passed, or spuriously (i.e., returning for no
     * "reason"). Note: This operation is in the Unsafe class only
     * because {@code unpark} is, so it would be strange to place it
     * elsewhere.
     */
    @ForceInline
    public void park(boolean isAbsolute, long time) {
        theInternalUnsafe.park(isAbsolute, time);
    }

    /**
     * Gets the load average in the system run queue assigned
     * to the available processors averaged over various periods of time.
     * This method retrieves the given {@code nelem} samples and
     * assigns to the elements of the given {@code loadavg} array.
     * The system imposes a maximum of 3 samples, representing
     * averages over the last 1,  5,  and  15 minutes, respectively.
     *
     * @param loadavg an array of double of size nelems
     * @param nelems the number of samples to be retrieved and
     *        must be 1 to 3.
     *
     * @return the number of samples actually retrieved; or -1
     *         if the load average is unobtainable.
     */
    @ForceInline
    public int getLoadAverage(double[] loadavg, int nelems) {
        return theInternalUnsafe.getLoadAverage(loadavg, nelems);
    }

    // The following contain CAS-based Java implementations used on
    // platforms not supporting native instructions

    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    @ForceInline
    public final int getAndAddInt(Object o, long offset, int delta) {
        return theInternalUnsafe.getAndAddInt(o, offset, delta);
    }

    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    @ForceInline
    public final long getAndAddLong(Object o, long offset, long delta) {
        return theInternalUnsafe.getAndAddLong(o, offset, delta);
    }

    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    @ForceInline
    public final int getAndSetInt(Object o, long offset, int newValue) {
        return theInternalUnsafe.getAndSetInt(o, offset, newValue);
    }

    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    @ForceInline
    public final long getAndSetLong(Object o, long offset, long newValue) {
        return theInternalUnsafe.getAndSetLong(o, offset, newValue);
    }

    /**
     * Atomically exchanges the given reference value with the current
     * reference value of a field or array element within the given
     * object {@code o} at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    @ForceInline
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        return theInternalUnsafe.getAndSetObject(o, offset, newValue);
    }


    /**
     * Ensures that loads before the fence will not be reordered with loads and
     * stores after the fence; a "LoadLoad plus LoadStore barrier".
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_acquire)
     * (an "acquire fence").
     *
     * A pure LoadLoad fence is not provided, since the addition of LoadStore
     * is almost always desired, and most current hardware instructions that
     * provide a LoadLoad barrier also provide a LoadStore barrier for free.
     * @since 1.8
     */
    @ForceInline
    public void loadFence() {
        theInternalUnsafe.loadFence();
    }

    /**
     * Ensures that loads and stores before the fence will not be reordered with
     * stores after the fence; a "StoreStore plus LoadStore barrier".
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_release)
     * (a "release fence").
     *
     * A pure StoreStore fence is not provided, since the addition of LoadStore
     * is almost always desired, and most current hardware instructions that
     * provide a StoreStore barrier also provide a LoadStore barrier for free.
     * @since 1.8
     */
    @ForceInline
    public void storeFence() {
        theInternalUnsafe.storeFence();
    }

    /**
     * Ensures that loads and stores before the fence will not be reordered
     * with loads and stores after the fence.  Implies the effects of both
     * loadFence() and storeFence(), and in addition, the effect of a StoreLoad
     * barrier.
     *
     * Corresponds to C11 atomic_thread_fence(memory_order_seq_cst).
     * @since 1.8
     */
    @ForceInline
    public void fullFence() {
        theInternalUnsafe.fullFence();
    }
}
