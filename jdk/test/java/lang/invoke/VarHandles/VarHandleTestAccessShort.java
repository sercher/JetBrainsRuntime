/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @run testng/othervm -Diters=10    -Xint                   VarHandleTestAccessShort
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 VarHandleTestAccessShort
 * @run testng/othervm -Diters=20000                         VarHandleTestAccessShort
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  VarHandleTestAccessShort
 */

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

public class VarHandleTestAccessShort extends VarHandleBaseTest {
    static final short static_final_v = (short)0x0123;

    static short static_v;

    final short final_v = (short)0x0123;

    short v;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessShort.class, "final_v", short.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessShort.class, "v", short.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessShort.class, "static_final_v", short.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessShort.class, "static_v", short.class);

        vhArray = MethodHandles.arrayElementVarHandle(short[].class);
    }


    @DataProvider
    public Object[][] varHandlesProvider() throws Exception {
        List<VarHandle> vhs = new ArrayList<>();
        vhs.add(vhField);
        vhs.add(vhStaticField);
        vhs.add(vhArray);

        return vhs.stream().map(tc -> new Object[]{tc}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "varHandlesProvider")
    public void testIsAccessModeSupported(VarHandle vh) {
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_VOLATILE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET_VOLATILE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_OPAQUE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET_OPAQUE));

        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_EXCHANGE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_EXCHANGE_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_EXCHANGE_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET_VOLATILE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_SET));

        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_ADD));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.ADD_AND_GET));
    }


    @DataProvider
    public Object[][] typesProvider() throws Exception {
        List<Object[]> types = new ArrayList<>();
        types.add(new Object[] {vhField, Arrays.asList(VarHandleTestAccessShort.class)});
        types.add(new Object[] {vhStaticField, Arrays.asList()});
        types.add(new Object[] {vhArray, Arrays.asList(short[].class, int.class)});

        return types.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "typesProvider")
    public void testTypes(VarHandle vh, List<Class<?>> pts) {
        assertEquals(vh.varType(), short.class);

        assertEquals(vh.coordinateTypes(), pts);

        testTypes(vh);
    }


    @Test
    public void testLookupInstanceToStatic() {
        checkIAE("Lookup of static final field to instance final field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessShort.class, "final_v", short.class);
        });

        checkIAE("Lookup of static field to instance field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessShort.class, "v", short.class);
        });
    }

    @Test
    public void testLookupStaticToInstance() {
        checkIAE("Lookup of instance final field to static final field", () -> {
            MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessShort.class, "static_final_v", short.class);
        });

        checkIAE("Lookup of instance field to static field", () -> {
            vhStaticField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessShort.class, "static_v", short.class);
        });
    }


    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance final field",
                                              vhFinalField, vh -> testInstanceFinalField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance final field unsupported",
                                              vhFinalField, vh -> testInstanceFinalFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static final field",
                                              vhStaticFinalField, VarHandleTestAccessShort::testStaticFinalField));
        cases.add(new VarHandleAccessTestCase("Static final field unsupported",
                                              vhStaticFinalField, VarHandleTestAccessShort::testStaticFinalFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance field unsupported",
                                              vhField, vh -> testInstanceFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestAccessShort::testStaticField));
        cases.add(new VarHandleAccessTestCase("Static field unsupported",
                                              vhStaticField, VarHandleTestAccessShort::testStaticFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestAccessShort::testArray));
        cases.add(new VarHandleAccessTestCase("Array unsupported",
                                              vhArray, VarHandleTestAccessShort::testArrayUnsupported,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array index out of bounds",
                                              vhArray, VarHandleTestAccessShort::testArrayIndexOutOfBounds,
                                              false));

        // Work around issue with jtreg summary reporting which truncates
        // the String result of Object.toString to 30 characters, hence
        // the first dummy argument
        return cases.stream().map(tc -> new Object[]{tc.toString(), tc}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "accessTestCaseProvider")
    public <T> void testAccess(String desc, AccessTestCase<T> atc) throws Throwable {
        T t = atc.get();
        int iters = atc.requiresLoop() ? ITERS : 1;
        for (int c = 0; c < iters; c++) {
            atc.testAccess(t);
        }
    }




    static void testInstanceFinalField(VarHandleTestAccessShort recv, VarHandle vh) {
        // Plain
        {
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "get short value");
        }


        // Volatile
        {
            short x = (short) vh.getVolatile(recv);
            assertEquals(x, (short)0x0123, "getVolatile short value");
        }

        // Lazy
        {
            short x = (short) vh.getAcquire(recv);
            assertEquals(x, (short)0x0123, "getRelease short value");
        }

        // Opaque
        {
            short x = (short) vh.getOpaque(recv);
            assertEquals(x, (short)0x0123, "getOpaque short value");
        }
    }

    static void testInstanceFinalFieldUnsupported(VarHandleTestAccessShort recv, VarHandle vh) {
        checkUOE(() -> {
            vh.set(recv, (short)0x4567);
        });

        checkUOE(() -> {
            vh.setVolatile(recv, (short)0x4567);
        });

        checkUOE(() -> {
            vh.setRelease(recv, (short)0x4567);
        });

        checkUOE(() -> {
            vh.setOpaque(recv, (short)0x4567);
        });


    }


    static void testStaticFinalField(VarHandle vh) {
        // Plain
        {
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "get short value");
        }


        // Volatile
        {
            short x = (short) vh.getVolatile();
            assertEquals(x, (short)0x0123, "getVolatile short value");
        }

        // Lazy
        {
            short x = (short) vh.getAcquire();
            assertEquals(x, (short)0x0123, "getRelease short value");
        }

        // Opaque
        {
            short x = (short) vh.getOpaque();
            assertEquals(x, (short)0x0123, "getOpaque short value");
        }
    }

    static void testStaticFinalFieldUnsupported(VarHandle vh) {
        checkUOE(() -> {
            vh.set((short)0x4567);
        });

        checkUOE(() -> {
            vh.setVolatile((short)0x4567);
        });

        checkUOE(() -> {
            vh.setRelease((short)0x4567);
        });

        checkUOE(() -> {
            vh.setOpaque((short)0x4567);
        });


    }


    static void testInstanceField(VarHandleTestAccessShort recv, VarHandle vh) {
        // Plain
        {
            vh.set(recv, (short)0x0123);
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "set short value");
        }


        // Volatile
        {
            vh.setVolatile(recv, (short)0x4567);
            short x = (short) vh.getVolatile(recv);
            assertEquals(x, (short)0x4567, "setVolatile short value");
        }

        // Lazy
        {
            vh.setRelease(recv, (short)0x0123);
            short x = (short) vh.getAcquire(recv);
            assertEquals(x, (short)0x0123, "setRelease short value");
        }

        // Opaque
        {
            vh.setOpaque(recv, (short)0x4567);
            short x = (short) vh.getOpaque(recv);
            assertEquals(x, (short)0x4567, "setOpaque short value");
        }

        vh.set(recv, (short)0x0123);

        // Compare
        {
            boolean r = vh.compareAndSet(recv, (short)0x0123, (short)0x4567);
            assertEquals(r, true, "success compareAndSet short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "success compareAndSet short value");
        }

        {
            boolean r = vh.compareAndSet(recv, (short)0x0123, (short)0x89AB);
            assertEquals(r, false, "failing compareAndSet short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "failing compareAndSet short value");
        }

        {
            short r = (short) vh.compareAndExchange(recv, (short)0x4567, (short)0x0123);
            assertEquals(r, (short)0x4567, "success compareAndExchange short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "success compareAndExchange short value");
        }

        {
            short r = (short) vh.compareAndExchange(recv, (short)0x4567, (short)0x89AB);
            assertEquals(r, (short)0x0123, "failing compareAndExchange short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "failing compareAndExchange short value");
        }

        {
            short r = (short) vh.compareAndExchangeAcquire(recv, (short)0x0123, (short)0x4567);
            assertEquals(r, (short)0x0123, "success compareAndExchangeAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "success compareAndExchangeAcquire short value");
        }

        {
            short r = (short) vh.compareAndExchangeAcquire(recv, (short)0x0123, (short)0x89AB);
            assertEquals(r, (short)0x4567, "failing compareAndExchangeAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "failing compareAndExchangeAcquire short value");
        }

        {
            short r = (short) vh.compareAndExchangeRelease(recv, (short)0x4567, (short)0x0123);
            assertEquals(r, (short)0x4567, "success compareAndExchangeRelease short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "success compareAndExchangeRelease short value");
        }

        {
            short r = (short) vh.compareAndExchangeRelease(recv, (short)0x4567, (short)0x89AB);
            assertEquals(r, (short)0x0123, "failing compareAndExchangeRelease short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "failing compareAndExchangeRelease short value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(recv, (short)0x0123, (short)0x4567);
            }
            assertEquals(success, true, "weakCompareAndSet short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "weakCompareAndSet short value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(recv, (short)0x4567, (short)0x0123);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "weakCompareAndSetAcquire short");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(recv, (short)0x0123, (short)0x4567);
            }
            assertEquals(success, true, "weakCompareAndSetRelease short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "weakCompareAndSetRelease short");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetVolatile(recv, (short)0x4567, (short)0x0123);
            }
            assertEquals(success, true, "weakCompareAndSetVolatile short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "weakCompareAndSetVolatile short value");
        }

        // Compare set and get
        {
            short o = (short) vh.getAndSet(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndSet short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "getAndSet short value");
        }

        vh.set(recv, (short)0x0123);

        // get and add, add and get
        {
            short o = (short) vh.getAndAdd(recv, (short)0x89AB);
            assertEquals(o, (short)0x0123, "getAndAdd short");
            short c = (short) vh.addAndGet(recv, (short)0x89AB);
            assertEquals(c, (short)((short)0x0123 + (short)0x89AB + (short)0x89AB), "getAndAdd short value");
        }
    }

    static void testInstanceFieldUnsupported(VarHandleTestAccessShort recv, VarHandle vh) {

    }


    static void testStaticField(VarHandle vh) {
        // Plain
        {
            vh.set((short)0x0123);
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "set short value");
        }


        // Volatile
        {
            vh.setVolatile((short)0x4567);
            short x = (short) vh.getVolatile();
            assertEquals(x, (short)0x4567, "setVolatile short value");
        }

        // Lazy
        {
            vh.setRelease((short)0x0123);
            short x = (short) vh.getAcquire();
            assertEquals(x, (short)0x0123, "setRelease short value");
        }

        // Opaque
        {
            vh.setOpaque((short)0x4567);
            short x = (short) vh.getOpaque();
            assertEquals(x, (short)0x4567, "setOpaque short value");
        }

        vh.set((short)0x0123);

        // Compare
        {
            boolean r = vh.compareAndSet((short)0x0123, (short)0x4567);
            assertEquals(r, true, "success compareAndSet short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "success compareAndSet short value");
        }

        {
            boolean r = vh.compareAndSet((short)0x0123, (short)0x89AB);
            assertEquals(r, false, "failing compareAndSet short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "failing compareAndSet short value");
        }

        {
            short r = (short) vh.compareAndExchange((short)0x4567, (short)0x0123);
            assertEquals(r, (short)0x4567, "success compareAndExchange short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "success compareAndExchange short value");
        }

        {
            short r = (short) vh.compareAndExchange((short)0x4567, (short)0x89AB);
            assertEquals(r, (short)0x0123, "failing compareAndExchange short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "failing compareAndExchange short value");
        }

        {
            short r = (short) vh.compareAndExchangeAcquire((short)0x0123, (short)0x4567);
            assertEquals(r, (short)0x0123, "success compareAndExchangeAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "success compareAndExchangeAcquire short value");
        }

        {
            short r = (short) vh.compareAndExchangeAcquire((short)0x0123, (short)0x89AB);
            assertEquals(r, (short)0x4567, "failing compareAndExchangeAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "failing compareAndExchangeAcquire short value");
        }

        {
            short r = (short) vh.compareAndExchangeRelease((short)0x4567, (short)0x0123);
            assertEquals(r, (short)0x4567, "success compareAndExchangeRelease short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "success compareAndExchangeRelease short value");
        }

        {
            short r = (short) vh.compareAndExchangeRelease((short)0x4567, (short)0x89AB);
            assertEquals(r, (short)0x0123, "failing compareAndExchangeRelease short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "failing compareAndExchangeRelease short value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet((short)0x0123, (short)0x4567);
            }
            assertEquals(success, true, "weakCompareAndSet short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "weakCompareAndSet short value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire((short)0x4567, (short)0x0123);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "weakCompareAndSetAcquire short");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease((short)0x0123, (short)0x4567);
            }
            assertEquals(success, true, "weakCompareAndSetRelease short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "weakCompareAndSetRelease short");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease((short)0x4567, (short)0x0123);
            }
            assertEquals(success, true, "weakCompareAndSetVolatile short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "weakCompareAndSetVolatile short");
        }

        // Compare set and get
        {
            short o = (short) vh.getAndSet((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndSet short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "getAndSet short value");
        }

        vh.set((short)0x0123);

        // get and add, add and get
        {
            short o = (short) vh.getAndAdd( (short)0x89AB);
            assertEquals(o, (short)0x0123, "getAndAdd short");
            short c = (short) vh.addAndGet((short)0x89AB);
            assertEquals(c, (short)((short)0x0123 + (short)0x89AB + (short)0x89AB), "getAndAdd short value");
        }
    }

    static void testStaticFieldUnsupported(VarHandle vh) {

    }


    static void testArray(VarHandle vh) {
        short[] array = new short[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                vh.set(array, i, (short)0x0123);
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "get short value");
            }


            // Volatile
            {
                vh.setVolatile(array, i, (short)0x4567);
                short x = (short) vh.getVolatile(array, i);
                assertEquals(x, (short)0x4567, "setVolatile short value");
            }

            // Lazy
            {
                vh.setRelease(array, i, (short)0x0123);
                short x = (short) vh.getAcquire(array, i);
                assertEquals(x, (short)0x0123, "setRelease short value");
            }

            // Opaque
            {
                vh.setOpaque(array, i, (short)0x4567);
                short x = (short) vh.getOpaque(array, i);
                assertEquals(x, (short)0x4567, "setOpaque short value");
            }

            vh.set(array, i, (short)0x0123);

            // Compare
            {
                boolean r = vh.compareAndSet(array, i, (short)0x0123, (short)0x4567);
                assertEquals(r, true, "success compareAndSet short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "success compareAndSet short value");
            }

            {
                boolean r = vh.compareAndSet(array, i, (short)0x0123, (short)0x89AB);
                assertEquals(r, false, "failing compareAndSet short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "failing compareAndSet short value");
            }

            {
                short r = (short) vh.compareAndExchange(array, i, (short)0x4567, (short)0x0123);
                assertEquals(r, (short)0x4567, "success compareAndExchange short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "success compareAndExchange short value");
            }

            {
                short r = (short) vh.compareAndExchange(array, i, (short)0x4567, (short)0x89AB);
                assertEquals(r, (short)0x0123, "failing compareAndExchange short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "failing compareAndExchange short value");
            }

            {
                short r = (short) vh.compareAndExchangeAcquire(array, i, (short)0x0123, (short)0x4567);
                assertEquals(r, (short)0x0123, "success compareAndExchangeAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "success compareAndExchangeAcquire short value");
            }

            {
                short r = (short) vh.compareAndExchangeAcquire(array, i, (short)0x0123, (short)0x89AB);
                assertEquals(r, (short)0x4567, "failing compareAndExchangeAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "failing compareAndExchangeAcquire short value");
            }

            {
                short r = (short) vh.compareAndExchangeRelease(array, i, (short)0x4567, (short)0x0123);
                assertEquals(r, (short)0x4567, "success compareAndExchangeRelease short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "success compareAndExchangeRelease short value");
            }

            {
                short r = (short) vh.compareAndExchangeRelease(array, i, (short)0x4567, (short)0x89AB);
                assertEquals(r, (short)0x0123, "failing compareAndExchangeRelease short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "failing compareAndExchangeRelease short value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSet(array, i, (short)0x0123, (short)0x4567);
                }
                assertEquals(success, true, "weakCompareAndSet short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "weakCompareAndSet short value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetAcquire(array, i, (short)0x4567, (short)0x0123);
                }
                assertEquals(success, true, "weakCompareAndSetAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "weakCompareAndSetAcquire short");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetRelease(array, i, (short)0x0123, (short)0x4567);
                }
                assertEquals(success, true, "weakCompareAndSetRelease short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "weakCompareAndSetRelease short");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetVolatile(array, i, (short)0x4567, (short)0x0123);
                }
                assertEquals(success, true, "weakCompareAndSetVolatile short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "weakCompareAndSetVolatile short");
            }

            // Compare set and get
            {
                short o = (short) vh.getAndSet(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndSet short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "getAndSet short value");
            }

            vh.set(array, i, (short)0x0123);

            // get and add, add and get
            {
                short o = (short) vh.getAndAdd(array, i, (short)0x89AB);
                assertEquals(o, (short)0x0123, "getAndAdd short");
                short c = (short) vh.addAndGet(array, i, (short)0x89AB);
                assertEquals(c, (short)((short)0x0123 + (short)0x89AB + (short)0x89AB), "getAndAdd short value");
            }
        }
    }

    static void testArrayUnsupported(VarHandle vh) {
        short[] array = new short[10];

        int i = 0;

    }

    static void testArrayIndexOutOfBounds(VarHandle vh) throws Throwable {
        short[] array = new short[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            checkIOOBE(() -> {
                short x = (short) vh.get(array, ci);
            });

            checkIOOBE(() -> {
                vh.set(array, ci, (short)0x0123);
            });

            checkIOOBE(() -> {
                short x = (short) vh.getVolatile(array, ci);
            });

            checkIOOBE(() -> {
                vh.setVolatile(array, ci, (short)0x0123);
            });

            checkIOOBE(() -> {
                short x = (short) vh.getAcquire(array, ci);
            });

            checkIOOBE(() -> {
                vh.setRelease(array, ci, (short)0x0123);
            });

            checkIOOBE(() -> {
                short x = (short) vh.getOpaque(array, ci);
            });

            checkIOOBE(() -> {
                vh.setOpaque(array, ci, (short)0x0123);
            });

            checkIOOBE(() -> {
                boolean r = vh.compareAndSet(array, ci, (short)0x0123, (short)0x4567);
            });

            checkIOOBE(() -> {
                short r = (short) vh.compareAndExchange(array, ci, (short)0x4567, (short)0x0123);
            });

            checkIOOBE(() -> {
                short r = (short) vh.compareAndExchangeAcquire(array, ci, (short)0x4567, (short)0x0123);
            });

            checkIOOBE(() -> {
                short r = (short) vh.compareAndExchangeRelease(array, ci, (short)0x4567, (short)0x0123);
            });

            checkIOOBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, (short)0x0123, (short)0x4567);
            });

            checkIOOBE(() -> {
                boolean r = vh.weakCompareAndSetVolatile(array, ci, (short)0x0123, (short)0x4567);
            });

            checkIOOBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, (short)0x0123, (short)0x4567);
            });

            checkIOOBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, (short)0x0123, (short)0x4567);
            });

            checkIOOBE(() -> {
                short o = (short) vh.getAndSet(array, ci, (short)0x0123);
            });

            checkIOOBE(() -> {
                short o = (short) vh.getAndAdd(array, ci, (short)0x89AB);
            });

            checkIOOBE(() -> {
                short o = (short) vh.addAndGet(array, ci, (short)0x89AB);
            });
        }
    }
}

