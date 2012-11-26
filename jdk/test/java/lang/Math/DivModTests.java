/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @test Test Math and StrictMath Floor Div / Modulo operations.
 * @bug 6282196
 * @summary Basic tests for Floor division and modulo methods for both Math
 * and StrictMath for int and long datatypes.
 */
public class DivModTests {

    /**
     * The count of test errors.
     */
    private static int errors = 0;

    /**
     * @param args the command line arguments are unused
     */
    public static void main(String[] args) {
        errors = 0;
        testIntFloorDivMod();
        testLongFloorDivMod();

        if (errors > 0) {
            throw new RuntimeException(errors + " errors found in DivMod methods.");
        }
    }

    /**
     * Report a test failure and increment the error count.
     * @param message the formatting string
     * @param args the variable number of arguments for the message.
     */
    static void fail(String message, Object... args) {
        errors++;
        System.out.printf(message, args);
    }

    /**
     * Test the integer floorDiv and floorMod methods.
     * Math and StrictMath tested and the same results are expected for both.
     */
    static void testIntFloorDivMod() {
        testIntFloorDivMod(4, 0, new ArithmeticException("/ by zero"), new ArithmeticException("/ by zero")); // Should throw ArithmeticException
        testIntFloorDivMod(4, 3, 1, 1);
        testIntFloorDivMod(3, 3, 1, 0);
        testIntFloorDivMod(2, 3, 0, 2);
        testIntFloorDivMod(1, 3, 0, 1);
        testIntFloorDivMod(0, 3, 0, 0);
        testIntFloorDivMod(4, -3, -2, -2);
        testIntFloorDivMod(3, -3, -1, 0);
        testIntFloorDivMod(2, -3, -1, -1);
        testIntFloorDivMod(1, -3, -1, -2);
        testIntFloorDivMod(0, -3, 0, 0);
        testIntFloorDivMod(-1, 3, -1, 2);
        testIntFloorDivMod(-2, 3, -1, 1);
        testIntFloorDivMod(-3, 3, -1, 0);
        testIntFloorDivMod(-4, 3, -2, 2);
        testIntFloorDivMod(-1, -3, 0, -1);
        testIntFloorDivMod(-2, -3, 0, -2);
        testIntFloorDivMod(-3, -3, 1, 0);
        testIntFloorDivMod(-4, -3, 1, -1);
        testIntFloorDivMod(Integer.MAX_VALUE, 1, Integer.MAX_VALUE, 0);
        testIntFloorDivMod(Integer.MAX_VALUE, -1, -Integer.MAX_VALUE, 0);
        testIntFloorDivMod(Integer.MAX_VALUE, 3, 715827882, 1);
        testIntFloorDivMod(Integer.MAX_VALUE - 1, 3, 715827882, 0);
        testIntFloorDivMod(Integer.MIN_VALUE, 3, -715827883, 1);
        testIntFloorDivMod(Integer.MIN_VALUE + 1, 3, -715827883, 2);
        testIntFloorDivMod(Integer.MIN_VALUE + 1, -1, Integer.MAX_VALUE, 0);
        // Special case of integer overflow
        testIntFloorDivMod(Integer.MIN_VALUE, -1, Integer.MIN_VALUE, 0);
    }

    /**
     * Test FloorDiv and then FloorMod with int data.
     */
    static void testIntFloorDivMod(int x, int y, Object divExpected, Object modExpected) {
        testIntFloorDiv(x, y, divExpected);
        testIntFloorMod(x, y, modExpected);
    }

    /**
     * Test FloorDiv with int data.
     */
    static void testIntFloorDiv(int x, int y, Object expected) {
        Object result = doFloorDiv(x, y);
        if (!resultEquals(result, expected)) {
            fail("FAIL: Math.floorDiv(%d, %d) = %s; expected %s%n", x, y, result, expected);
        }

        Object strict_result = doStrictFloorDiv(x, y);
        if (!resultEquals(strict_result, expected)) {
            fail("FAIL: StrictMath.floorDiv(%d, %d) = %s; expected %s%n", x, y, strict_result, expected);
        }
    }

    /**
     * Test FloorMod with int data.
     */
    static void testIntFloorMod(int x, int y, Object expected) {
        Object result = doFloorMod(x, y);
        if (!resultEquals(result, expected)) {
            fail("FAIL: Math.floorMod(%d, %d) = %s; expected %s%n", x, y, result, expected);
        }

        Object strict_result = doStrictFloorMod(x, y);
        if (!resultEquals(strict_result, expected)) {
            fail("FAIL: StrictMath.floorMod(%d, %d) = %s; expected %s%n", x, y, strict_result, expected);
        }

        try {
            // Verify result against double precision floor function
            int tmp = x / y;     // Force ArithmeticException for divide by zero
            double ff = x - Math.floor((double)x / (double)y) * y;
            int fr = (int)ff;
            if (fr != result) {
                fail("FAIL: Math.floorMod(%d, %d) = %s differs from Math.floor(x, y): %d%n", x, y, result, fr);
            }
        } catch (ArithmeticException ae) {
            if (y != 0) {
                fail("FAIL: Math.floorMod(%d, %d); unexpected %s%n", x, y, ae);
            }
        }
    }

    /**
     * Test the floorDiv and floorMod methods for primitive long.
     */
    static void testLongFloorDivMod() {
        testLongFloorDivMod(4L, 0L, new ArithmeticException("/ by zero"), new ArithmeticException("/ by zero")); // Should throw ArithmeticException
        testLongFloorDivMod(4L, 3L, 1L, 1L);
        testLongFloorDivMod(3L, 3L, 1L, 0L);
        testLongFloorDivMod(2L, 3L, 0L, 2L);
        testLongFloorDivMod(1L, 3L, 0L, 1L);
        testLongFloorDivMod(0L, 3L, 0L, 0L);
        testLongFloorDivMod(4L, -3L, -2L, -2L);
        testLongFloorDivMod(3L, -3L, -1L, 0l);
        testLongFloorDivMod(2L, -3L, -1L, -1L);
        testLongFloorDivMod(1L, -3L, -1L, -2L);
        testLongFloorDivMod(0L, -3L, 0L, 0L);
        testLongFloorDivMod(-1L, 3L, -1L, 2L);
        testLongFloorDivMod(-2L, 3L, -1L, 1L);
        testLongFloorDivMod(-3L, 3L, -1L, 0L);
        testLongFloorDivMod(-4L, 3L, -2L, 2L);
        testLongFloorDivMod(-1L, -3L, 0L, -1L);
        testLongFloorDivMod(-2L, -3L, 0L, -2L);
        testLongFloorDivMod(-3L, -3L, 1L, 0L);
        testLongFloorDivMod(-4L, -3L, 1L, -1L);

        testLongFloorDivMod(Long.MAX_VALUE, 1, Long.MAX_VALUE, 0L);
        testLongFloorDivMod(Long.MAX_VALUE, -1, -Long.MAX_VALUE, 0L);
        testLongFloorDivMod(Long.MAX_VALUE, 3L, Long.MAX_VALUE / 3L, 1L);
        testLongFloorDivMod(Long.MAX_VALUE - 1L, 3L, (Long.MAX_VALUE - 1L) / 3L, 0L);
        testLongFloorDivMod(Long.MIN_VALUE, 3L, Long.MIN_VALUE / 3L - 1L, 1L);
        testLongFloorDivMod(Long.MIN_VALUE + 1L, 3L, Long.MIN_VALUE / 3L - 1L, 2L);
        testLongFloorDivMod(Long.MIN_VALUE + 1, -1, Long.MAX_VALUE, 0L);
        // Special case of integer overflow
        testLongFloorDivMod(Long.MIN_VALUE, -1, Long.MIN_VALUE, 0L);
    }

    /**
     * Test the integer floorDiv and floorMod methods.
     * Math and StrictMath are tested and the same results are expected for both.
     */
    static void testLongFloorDivMod(long x, long y, Object divExpected, Object modExpected) {
        testLongFloorDiv(x, y, divExpected);
        testLongFloorMod(x, y, modExpected);
    }

    /**
     * Test FloorDiv with long arguments against expected value.
     * The expected value is usually a Long but in some cases  is
     * an ArithmeticException.
     *
     * @param x dividend
     * @param y modulus
     * @param expected expected value,
     */
    static void testLongFloorDiv(long x, long y, Object expected) {
        Object result = doFloorDiv(x, y);
        if (!resultEquals(result, expected)) {
            fail("FAIL: long Math.floorDiv(%d, %d) = %s; expected %s%n", x, y, result, expected);
        }

        Object strict_result = doStrictFloorDiv(x, y);
        if (!resultEquals(strict_result, expected)) {
            fail("FAIL: long StrictMath.floorDiv(%d, %d) = %s; expected %s%n", x, y, strict_result, expected);
        }
    }

    /**
     * Test FloorMod of long arguments against expected value.
     * The expected value is usually a Long but in some cases  is
     * an ArithmeticException.
     *
     * @param x dividend
     * @param y modulus
     * @param expected expected value
     */
    static void testLongFloorMod(long x, long y, Object expected) {
        Object result = doFloorMod(x, y);
        if (!resultEquals(result, expected)) {
            fail("FAIL: long Math.floorMod(%d, %d) = %s; expected %s%n", x, y, result, expected);
        }

        Object strict_result = doStrictFloorMod(x, y);
        if (!resultEquals(strict_result, expected)) {
            fail("FAIL: long StrictMath.floorMod(%d, %d) = %s; expected %s%n", x, y, strict_result, expected);
        }

        try {
            // Verify the result against BigDecimal rounding mode.
            BigDecimal xD = new BigDecimal(x);
            BigDecimal yD = new BigDecimal(y);
            BigDecimal resultD = xD.divide(yD, RoundingMode.FLOOR);
            resultD = resultD.multiply(yD);
            resultD = xD.subtract(resultD);
            long fr = resultD.longValue();
            if (fr != result) {
                fail("FAIL: Long.floorMod(%d, %d) = %d is different than BigDecimal result: %d%n",x, y, result, fr);

            }
        } catch (ArithmeticException ae) {
            if (y != 0) {
                fail("FAIL: long Math.floorMod(%d, %d); unexpected ArithmeticException from bigdecimal");
            }
        }
    }

    /**
     * Invoke floorDiv and return the result or any exception.
     * @param x the x value
     * @param y the y value
     * @return the result Integer or an exception.
     */
    static Object doFloorDiv(int x, int y) {
        try {
            return Math.floorDiv(x, y);
        } catch (ArithmeticException ae) {
            return ae;
        }
    }

    /**
     * Invoke floorDiv and return the result or any exception.
     * @param x the x value
     * @param y the y value
     * @return the result Integer or an exception.
     */
    static Object doFloorDiv(long x, long y) {
        try {
            return Math.floorDiv(x, y);
        } catch (ArithmeticException ae) {
            return ae;
        }
    }

    /**
     * Invoke floorDiv and return the result or any exception.
     * @param x the x value
     * @param y the y value
     * @return the result Integer or an exception.
     */
    static Object doFloorMod(int x, int y) {
        try {
            return Math.floorMod(x, y);
        } catch (ArithmeticException ae) {
            return ae;
        }
    }

    /**
     * Invoke floorDiv and return the result or any exception.
     * @param x the x value
     * @param y the y value
     * @return the result Integer or an exception.
     */
    static Object doFloorMod(long x, long y) {
        try {
            return Math.floorMod(x, y);
        } catch (ArithmeticException ae) {
            return ae;
        }
    }

    /**
     * Invoke floorDiv and return the result or any exception.
     * @param x the x value
     * @param y the y value
     * @return the result Integer or an exception.
     */
    static Object doStrictFloorDiv(int x, int y) {
        try {
            return StrictMath.floorDiv(x, y);
        } catch (ArithmeticException ae) {
            return ae;
        }
    }

    /**
     * Invoke floorDiv and return the result or any exception.
     * @param x the x value
     * @param y the y value
     * @return the result Integer or an exception.
     */
    static Object doStrictFloorDiv(long x, long y) {
        try {
            return StrictMath.floorDiv(x, y);
        } catch (ArithmeticException ae) {
            return ae;
        }
    }

    /**
     * Invoke floorDiv and return the result or any exception.
     * @param x the x value
     * @param y the y value
     * @return the result Integer or an exception.
     */
    static Object doStrictFloorMod(int x, int y) {
        try {
            return StrictMath.floorMod(x, y);
        } catch (ArithmeticException ae) {
            return ae;
        }
    }

    /**
     * Invoke floorDiv and return the result or any exception.
     * @param x the x value
     * @param y the y value
     * @return the result Integer or an exception.
     */
    static Object doStrictFloorMod(long x, long y) {
        try {
            return StrictMath.floorMod(x, y);
        } catch (ArithmeticException ae) {
            return ae;
        }
    }

    /**
     * Returns a boolean by comparing the result and the expected value.
     * The equals method is not defined for ArithmeticException but it is
     * desirable to have equals return true if the expected and the result
     * both threw the same exception (class and message.)
     *
     * @param result the result from testing the method
     * @param expected the expected value
     * @return true if the result is equal to the expected values; false otherwise.
     */
    static boolean resultEquals(Object result, Object expected) {
        if (result.getClass() != expected.getClass()) {
            fail("FAIL: Result type mismatch, %s; expected: %s%n",
                    result.getClass().getName(), expected.getClass().getName());
            return false;
        }

        if (result.equals(expected)) {
            return true;
        }
        // Handle special case to compare ArithmeticExceptions
        if (result instanceof ArithmeticException && expected instanceof ArithmeticException) {
            ArithmeticException ae1 = (ArithmeticException)result;
            ArithmeticException ae2 = (ArithmeticException)expected;
            return ae1.getMessage().equals(ae2.getMessage());
        }
        return false;
    }

}
