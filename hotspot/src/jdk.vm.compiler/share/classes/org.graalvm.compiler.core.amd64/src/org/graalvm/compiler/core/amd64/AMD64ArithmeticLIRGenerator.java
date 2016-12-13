/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.compiler.core.amd64;

import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic.ADD;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic.AND;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic.CMP;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic.OR;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic.SUB;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic.XOR;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64MOp.NEG;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64MOp.NOT;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.BSF;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.BSR;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.LZCNT;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.MOV;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.MOVSD;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.MOVSS;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.MOVSX;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.MOVSXB;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.MOVSXD;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.MOVZX;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.MOVZXB;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.POPCNT;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.TEST;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.TESTB;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp.TZCNT;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64Shift.ROL;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64Shift.ROR;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64Shift.SAR;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64Shift.SHL;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64Shift.SHR;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize.BYTE;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize.DWORD;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize.PD;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize.PS;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize.QWORD;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize.SD;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize.SS;
import static org.graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize.WORD;
import static org.graalvm.compiler.core.common.GraalOptions.GeneratePIC;
import static org.graalvm.compiler.lir.LIRValueUtil.asConstantValue;
import static org.graalvm.compiler.lir.LIRValueUtil.asJavaConstant;
import static org.graalvm.compiler.lir.LIRValueUtil.isConstantValue;
import static org.graalvm.compiler.lir.LIRValueUtil.isJavaConstant;
import static org.graalvm.compiler.lir.amd64.AMD64Arithmetic.DREM;
import static org.graalvm.compiler.lir.amd64.AMD64Arithmetic.FREM;
import static org.graalvm.compiler.lir.amd64.AMD64MathIntrinsicUnaryOp.UnaryIntrinsicOpcode.COS;
import static org.graalvm.compiler.lir.amd64.AMD64MathIntrinsicUnaryOp.UnaryIntrinsicOpcode.LOG;
import static org.graalvm.compiler.lir.amd64.AMD64MathIntrinsicUnaryOp.UnaryIntrinsicOpcode.LOG10;
import static org.graalvm.compiler.lir.amd64.AMD64MathIntrinsicUnaryOp.UnaryIntrinsicOpcode.SIN;
import static org.graalvm.compiler.lir.amd64.AMD64MathIntrinsicUnaryOp.UnaryIntrinsicOpcode.TAN;
import static org.graalvm.compiler.lir.amd64.AMD64MathIntrinsicUnaryOp.UnaryIntrinsicOpcode.EXP;
import static org.graalvm.compiler.lir.amd64.AMD64MathIntrinsicBinaryOp.BinaryIntrinsicOpcode.POW;

import org.graalvm.compiler.asm.NumUtil;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64MIOp;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64MOp;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64MROp;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMIOp;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RRMOp;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.AMD64Shift;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.SSEOp;
import org.graalvm.compiler.asm.amd64.AMD64Assembler.AVXOp;
import org.graalvm.compiler.core.common.LIRKind;
import org.graalvm.compiler.core.common.calc.FloatConvert;
import org.graalvm.compiler.debug.GraalError;
import org.graalvm.compiler.lir.ConstantValue;
import org.graalvm.compiler.lir.LIRFrameState;
import org.graalvm.compiler.lir.LIRValueUtil;
import org.graalvm.compiler.lir.Variable;
import org.graalvm.compiler.lir.amd64.AMD64AddressValue;
import org.graalvm.compiler.lir.amd64.AMD64Arithmetic.FPDivRemOp;
import org.graalvm.compiler.lir.amd64.AMD64ArithmeticLIRGeneratorTool;
import org.graalvm.compiler.lir.amd64.AMD64Binary;
import org.graalvm.compiler.lir.amd64.AMD64BinaryConsumer;
import org.graalvm.compiler.lir.amd64.AMD64ClearRegisterOp;
import org.graalvm.compiler.lir.amd64.AMD64MathIntrinsicUnaryOp;
import org.graalvm.compiler.lir.amd64.AMD64MathIntrinsicBinaryOp;
import org.graalvm.compiler.lir.amd64.AMD64MulDivOp;
import org.graalvm.compiler.lir.amd64.AMD64ShiftOp;
import org.graalvm.compiler.lir.amd64.AMD64SignExtendOp;
import org.graalvm.compiler.lir.amd64.AMD64Unary;
import org.graalvm.compiler.lir.gen.ArithmeticLIRGenerator;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64.CPUFeature;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.VMConstant;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;
import jdk.vm.ci.code.TargetDescription;

/**
 * This class implements the AMD64 specific portion of the LIR generator.
 */
public class AMD64ArithmeticLIRGenerator extends ArithmeticLIRGenerator implements AMD64ArithmeticLIRGeneratorTool {

    private static final RegisterValue RCX_I = AMD64.rcx.asValue(LIRKind.value(AMD64Kind.DWORD));

    @Override
    public Variable emitNegate(Value inputVal) {
        AllocatableValue input = getLIRGen().asAllocatable(inputVal);
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) input.getPlatformKind()) {
            case DWORD:
                getLIRGen().append(new AMD64Unary.MOp(NEG, DWORD, result, input));
                break;
            case QWORD:
                getLIRGen().append(new AMD64Unary.MOp(NEG, QWORD, result, input));
                break;
            case SINGLE:
                if (isAvx) {
                    getLIRGen().append(new AMD64Binary.DataThreeOp(AVXOp.XOR, PS, result, input, JavaConstant.forFloat(Float.intBitsToFloat(0x80000000)), 16));
                } else {
                    getLIRGen().append(new AMD64Binary.DataTwoOp(SSEOp.XOR, PS, result, input, JavaConstant.forFloat(Float.intBitsToFloat(0x80000000)), 16));
                }
                break;
            case DOUBLE:
                if (isAvx) {
                    getLIRGen().append(new AMD64Binary.DataThreeOp(AVXOp.XOR, PD, result, input, JavaConstant.forDouble(Double.longBitsToDouble(0x8000000000000000L)), 16));
                } else {
                    getLIRGen().append(new AMD64Binary.DataTwoOp(SSEOp.XOR, PD, result, input, JavaConstant.forDouble(Double.longBitsToDouble(0x8000000000000000L)), 16));
                }
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    @Override
    public Variable emitNot(Value inputVal) {
        AllocatableValue input = getLIRGen().asAllocatable(inputVal);
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        switch ((AMD64Kind) input.getPlatformKind()) {
            case DWORD:
                getLIRGen().append(new AMD64Unary.MOp(NOT, DWORD, result, input));
                break;
            case QWORD:
                getLIRGen().append(new AMD64Unary.MOp(NOT, QWORD, result, input));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    private Variable emitBinary(LIRKind resultKind, AMD64BinaryArithmetic op, OperandSize size, boolean commutative, Value a, Value b, boolean setFlags) {
        if (isJavaConstant(b)) {
            return emitBinaryConst(resultKind, op, size, commutative, getLIRGen().asAllocatable(a), asConstantValue(b), setFlags);
        } else if (commutative && isJavaConstant(a)) {
            return emitBinaryConst(resultKind, op, size, commutative, getLIRGen().asAllocatable(b), asConstantValue(a), setFlags);
        } else {
            return emitBinaryVar(resultKind, op.getRMOpcode(size), size, commutative, getLIRGen().asAllocatable(a), getLIRGen().asAllocatable(b));
        }
    }

    private Variable emitBinary(LIRKind resultKind, AMD64RMOp op, OperandSize size, boolean commutative, Value a, Value b) {
        if (isJavaConstant(b)) {
            return emitBinaryConst(resultKind, op, size, getLIRGen().asAllocatable(a), asJavaConstant(b));
        } else if (commutative && isJavaConstant(a)) {
            return emitBinaryConst(resultKind, op, size, getLIRGen().asAllocatable(b), asJavaConstant(a));
        } else {
            return emitBinaryVar(resultKind, op, size, commutative, getLIRGen().asAllocatable(a), getLIRGen().asAllocatable(b));
        }
    }

    private Variable emitBinary(LIRKind resultKind, AMD64RRMOp op, OperandSize size, boolean commutative, Value a, Value b) {
        if (isJavaConstant(b)) {
            return emitBinaryConst(resultKind, op, size, getLIRGen().asAllocatable(a), asJavaConstant(b));
        } else if (commutative && isJavaConstant(a)) {
            return emitBinaryConst(resultKind, op, size, getLIRGen().asAllocatable(b), asJavaConstant(a));
        } else {
            return emitBinaryVar(resultKind, op, size, commutative, getLIRGen().asAllocatable(a), getLIRGen().asAllocatable(b));
        }
    }

    private Variable emitBinaryConst(LIRKind resultKind, AMD64BinaryArithmetic op, OperandSize size, boolean commutative, AllocatableValue a, ConstantValue b, boolean setFlags) {
        long value = b.getJavaConstant().asLong();
        if (NumUtil.isInt(value)) {
            Variable result = getLIRGen().newVariable(resultKind);
            int constant = (int) value;

            if (!setFlags) {
                AMD64MOp mop = getMOp(op, constant);
                if (mop != null) {
                    getLIRGen().append(new AMD64Unary.MOp(mop, size, result, a));
                    return result;
                }
            }

            getLIRGen().append(new AMD64Binary.ConstOp(op, size, result, a, constant));
            return result;
        } else {
            return emitBinaryVar(resultKind, op.getRMOpcode(size), size, commutative, a, getLIRGen().asAllocatable(b));
        }
    }

    private static AMD64MOp getMOp(AMD64BinaryArithmetic op, int constant) {
        if (constant == 1) {
            if (op.equals(AMD64BinaryArithmetic.ADD)) {
                return AMD64MOp.INC;
            }
            if (op.equals(AMD64BinaryArithmetic.SUB)) {
                return AMD64MOp.DEC;
            }
        } else if (constant == -1) {
            if (op.equals(AMD64BinaryArithmetic.ADD)) {
                return AMD64MOp.DEC;
            }
            if (op.equals(AMD64BinaryArithmetic.SUB)) {
                return AMD64MOp.INC;
            }
        }
        return null;
    }

    private Variable emitBinaryConst(LIRKind resultKind, AMD64RMOp op, OperandSize size, AllocatableValue a, JavaConstant b) {
        Variable result = getLIRGen().newVariable(resultKind);
        getLIRGen().append(new AMD64Binary.DataTwoOp(op, size, result, a, b));
        return result;
    }

    private Variable emitBinaryConst(LIRKind resultKind, AMD64RRMOp op, OperandSize size, AllocatableValue a, JavaConstant b) {
        Variable result = getLIRGen().newVariable(resultKind);
        getLIRGen().append(new AMD64Binary.DataThreeOp(op, size, result, a, b));
        return result;
    }

    private Variable emitBinaryVar(LIRKind resultKind, AMD64RMOp op, OperandSize size, boolean commutative, AllocatableValue a, AllocatableValue b) {
        Variable result = getLIRGen().newVariable(resultKind);
        if (commutative) {
            getLIRGen().append(new AMD64Binary.CommutativeTwoOp(op, size, result, a, b));
        } else {
            getLIRGen().append(new AMD64Binary.TwoOp(op, size, result, a, b));
        }
        return result;
    }

    private Variable emitBinaryVar(LIRKind resultKind, AMD64RRMOp op, OperandSize size, boolean commutative, AllocatableValue a, AllocatableValue b) {
        Variable result = getLIRGen().newVariable(resultKind);
        if (commutative) {
            getLIRGen().append(new AMD64Binary.CommutativeThreeOp(op, size, result, a, b));
        } else {
            getLIRGen().append(new AMD64Binary.ThreeOp(op, size, result, a, b));
        }
        return result;
    }

    @Override
    protected boolean isNumericInteger(PlatformKind kind) {
        return ((AMD64Kind) kind).isInteger();
    }

    @Override
    public Variable emitAdd(LIRKind resultKind, Value a, Value b, boolean setFlags) {
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitBinary(resultKind, ADD, DWORD, true, a, b, setFlags);
            case QWORD:
                return emitBinary(resultKind, ADD, QWORD, true, a, b, setFlags);
            case SINGLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.ADD, SS, true, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.ADD, SS, true, a, b);
                }
            case DOUBLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.ADD, SD, true, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.ADD, SD, true, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitSub(LIRKind resultKind, Value a, Value b, boolean setFlags) {
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitBinary(resultKind, SUB, DWORD, false, a, b, setFlags);
            case QWORD:
                return emitBinary(resultKind, SUB, QWORD, false, a, b, setFlags);
            case SINGLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.SUB, SS, false, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.SUB, SS, false, a, b);
                }
            case DOUBLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.SUB, SD, false, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.SUB, SD, false, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private Variable emitIMULConst(OperandSize size, AllocatableValue a, ConstantValue b) {
        long value = b.getJavaConstant().asLong();
        if (NumUtil.isInt(value)) {
            int imm = (int) value;
            AMD64RMIOp op;
            if (NumUtil.isByte(imm)) {
                op = AMD64RMIOp.IMUL_SX;
            } else {
                op = AMD64RMIOp.IMUL;
            }

            Variable ret = getLIRGen().newVariable(LIRKind.combine(a, b));
            getLIRGen().append(new AMD64Binary.RMIOp(op, size, ret, a, imm));
            return ret;
        } else {
            return emitBinaryVar(LIRKind.combine(a, b), AMD64RMOp.IMUL, size, true, a, getLIRGen().asAllocatable(b));
        }
    }

    private Variable emitIMUL(OperandSize size, Value a, Value b) {
        if (isJavaConstant(b)) {
            return emitIMULConst(size, getLIRGen().asAllocatable(a), asConstantValue(b));
        } else if (isJavaConstant(a)) {
            return emitIMULConst(size, getLIRGen().asAllocatable(b), asConstantValue(a));
        } else {
            return emitBinaryVar(LIRKind.combine(a, b), AMD64RMOp.IMUL, size, true, getLIRGen().asAllocatable(a), getLIRGen().asAllocatable(b));
        }
    }

    @Override
    public Variable emitMul(Value a, Value b, boolean setFlags) {
        LIRKind resultKind = LIRKind.combine(a, b);
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitIMUL(DWORD, a, b);
            case QWORD:
                return emitIMUL(QWORD, a, b);
            case SINGLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.MUL, SS, true, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.MUL, SS, true, a, b);
                }
            case DOUBLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.MUL, SD, true, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.MUL, SD, true, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private RegisterValue moveToReg(Register reg, Value v) {
        RegisterValue ret = reg.asValue(v.getValueKind());
        getLIRGen().emitMove(ret, v);
        return ret;
    }

    private Value emitMulHigh(AMD64MOp opcode, OperandSize size, Value a, Value b) {
        AMD64MulDivOp mulHigh = getLIRGen().append(new AMD64MulDivOp(opcode, size, LIRKind.combine(a, b), moveToReg(AMD64.rax, a), getLIRGen().asAllocatable(b)));
        return getLIRGen().emitMove(mulHigh.getHighResult());
    }

    @Override
    public Value emitMulHigh(Value a, Value b) {
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitMulHigh(AMD64MOp.IMUL, DWORD, a, b);
            case QWORD:
                return emitMulHigh(AMD64MOp.IMUL, QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Value emitUMulHigh(Value a, Value b) {
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitMulHigh(AMD64MOp.MUL, DWORD, a, b);
            case QWORD:
                return emitMulHigh(AMD64MOp.MUL, QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public Value emitBinaryMemory(AMD64RMOp op, OperandSize size, AllocatableValue a, AMD64AddressValue location, LIRFrameState state) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(a));
        getLIRGen().append(new AMD64Binary.MemoryTwoOp(op, size, result, a, location, state));
        return result;
    }

    public Value emitBinaryMemory(AMD64RRMOp op, OperandSize size, AllocatableValue a, AMD64AddressValue location, LIRFrameState state) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(a));
        getLIRGen().append(new AMD64Binary.MemoryThreeOp(op, size, result, a, location, state));
        return result;
    }

    protected Value emitConvertMemoryOp(PlatformKind kind, AMD64RMOp op, OperandSize size, AMD64AddressValue address, LIRFrameState state) {
        Variable result = getLIRGen().newVariable(LIRKind.value(kind));
        getLIRGen().append(new AMD64Unary.MemoryOp(op, size, result, address, state));
        return result;
    }

    protected Value emitZeroExtendMemory(AMD64Kind memoryKind, int resultBits, AMD64AddressValue address, LIRFrameState state) {
        // Issue a zero extending load of the proper bit size and set the result to
        // the proper kind.
        Variable result = getLIRGen().newVariable(LIRKind.value(resultBits == 32 ? AMD64Kind.DWORD : AMD64Kind.QWORD));
        switch (memoryKind) {
            case BYTE:
                getLIRGen().append(new AMD64Unary.MemoryOp(MOVZXB, DWORD, result, address, state));
                break;
            case WORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(MOVZX, DWORD, result, address, state));
                break;
            case DWORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(MOV, DWORD, result, address, state));
                break;
            case QWORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(MOV, QWORD, result, address, state));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    private AMD64MulDivOp emitIDIV(OperandSize size, Value a, Value b, LIRFrameState state) {
        LIRKind kind = LIRKind.combine(a, b);

        AMD64SignExtendOp sx = getLIRGen().append(new AMD64SignExtendOp(size, kind, moveToReg(AMD64.rax, a)));
        return getLIRGen().append(new AMD64MulDivOp(AMD64MOp.IDIV, size, kind, sx.getHighResult(), sx.getLowResult(), getLIRGen().asAllocatable(b), state));
    }

    private AMD64MulDivOp emitDIV(OperandSize size, Value a, Value b, LIRFrameState state) {
        LIRKind kind = LIRKind.combine(a, b);

        RegisterValue rax = moveToReg(AMD64.rax, a);
        RegisterValue rdx = AMD64.rdx.asValue(kind);
        getLIRGen().append(new AMD64ClearRegisterOp(size, rdx));
        return getLIRGen().append(new AMD64MulDivOp(AMD64MOp.DIV, size, kind, rdx, rax, getLIRGen().asAllocatable(b), state));
    }

    public Value[] emitSignedDivRem(Value a, Value b, LIRFrameState state) {
        AMD64MulDivOp op;
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                op = emitIDIV(DWORD, a, b, state);
                break;
            case QWORD:
                op = emitIDIV(QWORD, a, b, state);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return new Value[]{getLIRGen().emitMove(op.getQuotient()), getLIRGen().emitMove(op.getRemainder())};
    }

    public Value[] emitUnsignedDivRem(Value a, Value b, LIRFrameState state) {
        AMD64MulDivOp op;
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                op = emitDIV(DWORD, a, b, state);
                break;
            case QWORD:
                op = emitDIV(QWORD, a, b, state);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return new Value[]{getLIRGen().emitMove(op.getQuotient()), getLIRGen().emitMove(op.getRemainder())};
    }

    @Override
    public Value emitDiv(Value a, Value b, LIRFrameState state) {
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        LIRKind resultKind = LIRKind.combine(a, b);
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                AMD64MulDivOp op = emitIDIV(DWORD, a, b, state);
                return getLIRGen().emitMove(op.getQuotient());
            case QWORD:
                AMD64MulDivOp lop = emitIDIV(QWORD, a, b, state);
                return getLIRGen().emitMove(lop.getQuotient());
            case SINGLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.DIV, SS, false, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.DIV, SS, false, a, b);
                }
            case DOUBLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.DIV, SD, false, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.DIV, SD, false, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Value emitRem(Value a, Value b, LIRFrameState state) {
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                AMD64MulDivOp op = emitIDIV(DWORD, a, b, state);
                return getLIRGen().emitMove(op.getRemainder());
            case QWORD:
                AMD64MulDivOp lop = emitIDIV(QWORD, a, b, state);
                return getLIRGen().emitMove(lop.getRemainder());
            case SINGLE: {
                Variable result = getLIRGen().newVariable(LIRKind.combine(a, b));
                getLIRGen().append(new FPDivRemOp(FREM, result, getLIRGen().load(a), getLIRGen().load(b)));
                return result;
            }
            case DOUBLE: {
                Variable result = getLIRGen().newVariable(LIRKind.combine(a, b));
                getLIRGen().append(new FPDivRemOp(DREM, result, getLIRGen().load(a), getLIRGen().load(b)));
                return result;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitUDiv(Value a, Value b, LIRFrameState state) {
        AMD64MulDivOp op;
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                op = emitDIV(DWORD, a, b, state);
                break;
            case QWORD:
                op = emitDIV(QWORD, a, b, state);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return getLIRGen().emitMove(op.getQuotient());
    }

    @Override
    public Variable emitURem(Value a, Value b, LIRFrameState state) {
        AMD64MulDivOp op;
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                op = emitDIV(DWORD, a, b, state);
                break;
            case QWORD:
                op = emitDIV(QWORD, a, b, state);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return getLIRGen().emitMove(op.getRemainder());
    }

    @Override
    public Variable emitAnd(Value a, Value b) {
        LIRKind resultKind = LIRKind.combine(a, b);
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitBinary(resultKind, AND, DWORD, true, a, b, false);
            case QWORD:
                return emitBinary(resultKind, AND, QWORD, true, a, b, false);
            case SINGLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.AND, PS, true, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.AND, PS, true, a, b);
                }
            case DOUBLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.AND, PD, true, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.AND, PD, true, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitOr(Value a, Value b) {
        LIRKind resultKind = LIRKind.combine(a, b);
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitBinary(resultKind, OR, DWORD, true, a, b, false);
            case QWORD:
                return emitBinary(resultKind, OR, QWORD, true, a, b, false);
            case SINGLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.OR, PS, true, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.OR, PS, true, a, b);
                }
            case DOUBLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.OR, PD, true, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.OR, PD, true, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitXor(Value a, Value b) {
        LIRKind resultKind = LIRKind.combine(a, b);
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitBinary(resultKind, XOR, DWORD, true, a, b, false);
            case QWORD:
                return emitBinary(resultKind, XOR, QWORD, true, a, b, false);
            case SINGLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.XOR, PS, true, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.XOR, PS, true, a, b);
                }
            case DOUBLE:
                if (isAvx) {
                    return emitBinary(resultKind, AVXOp.XOR, PD, true, a, b);
                } else {
                    return emitBinary(resultKind, SSEOp.XOR, PD, true, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private Variable emitShift(AMD64Shift op, OperandSize size, Value a, Value b) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(a, b).changeType(a.getPlatformKind()));
        AllocatableValue input = getLIRGen().asAllocatable(a);
        if (isJavaConstant(b)) {
            JavaConstant c = asJavaConstant(b);
            if (c.asLong() == 1) {
                getLIRGen().append(new AMD64Unary.MOp(op.m1Op, size, result, input));
            } else {
                /*
                 * c is implicitly masked to 5 or 6 bits by the CPU, so casting it to (int) is
                 * always correct, even without the NumUtil.is32bit() test.
                 */
                getLIRGen().append(new AMD64Binary.ConstOp(op.miOp, size, result, input, (int) c.asLong()));
            }
        } else {
            getLIRGen().emitMove(RCX_I, b);
            getLIRGen().append(new AMD64ShiftOp(op.mcOp, size, result, input, RCX_I));
        }
        return result;
    }

    @Override
    public Variable emitShl(Value a, Value b) {
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitShift(SHL, DWORD, a, b);
            case QWORD:
                return emitShift(SHL, QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitShr(Value a, Value b) {
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitShift(SAR, DWORD, a, b);
            case QWORD:
                return emitShift(SAR, QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitUShr(Value a, Value b) {
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitShift(SHR, DWORD, a, b);
            case QWORD:
                return emitShift(SHR, QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public Variable emitRol(Value a, Value b) {
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitShift(ROL, DWORD, a, b);
            case QWORD:
                return emitShift(ROL, QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public Variable emitRor(Value a, Value b) {
        switch ((AMD64Kind) a.getPlatformKind()) {
            case DWORD:
                return emitShift(ROR, DWORD, a, b);
            case QWORD:
                return emitShift(ROR, QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private AllocatableValue emitConvertOp(LIRKind kind, AMD64RMOp op, OperandSize size, Value input) {
        Variable result = getLIRGen().newVariable(kind);
        getLIRGen().append(new AMD64Unary.RMOp(op, size, result, getLIRGen().asAllocatable(input)));
        return result;
    }

    private AllocatableValue emitConvertOp(LIRKind kind, AMD64MROp op, OperandSize size, Value input) {
        Variable result = getLIRGen().newVariable(kind);
        getLIRGen().append(new AMD64Unary.MROp(op, size, result, getLIRGen().asAllocatable(input)));
        return result;
    }

    @Override
    public Value emitReinterpret(LIRKind to, Value inputVal) {
        ValueKind<?> from = inputVal.getValueKind();
        if (to.equals(from)) {
            return inputVal;
        }

        AllocatableValue input = getLIRGen().asAllocatable(inputVal);
        /*
         * Conversions between integer to floating point types require moves between CPU and FPU
         * registers.
         */
        AMD64Kind fromKind = (AMD64Kind) from.getPlatformKind();
        switch ((AMD64Kind) to.getPlatformKind()) {
            case DWORD:
                switch (fromKind) {
                    case SINGLE:
                        return emitConvertOp(to, AMD64MROp.MOVD, DWORD, input);
                }
                break;
            case QWORD:
                switch (fromKind) {
                    case DOUBLE:
                        return emitConvertOp(to, AMD64MROp.MOVQ, QWORD, input);
                }
                break;
            case SINGLE:
                switch (fromKind) {
                    case DWORD:
                        return emitConvertOp(to, AMD64RMOp.MOVD, DWORD, input);
                }
                break;
            case DOUBLE:
                switch (fromKind) {
                    case QWORD:
                        return emitConvertOp(to, AMD64RMOp.MOVQ, QWORD, input);
                }
                break;
        }
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public Value emitFloatConvert(FloatConvert op, Value input) {
        switch (op) {
            case D2F:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.SINGLE), SSEOp.CVTSD2SS, SD, input);
            case D2I:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.DWORD), SSEOp.CVTTSD2SI, DWORD, input);
            case D2L:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.QWORD), SSEOp.CVTTSD2SI, QWORD, input);
            case F2D:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.DOUBLE), SSEOp.CVTSS2SD, SS, input);
            case F2I:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.DWORD), SSEOp.CVTTSS2SI, DWORD, input);
            case F2L:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.QWORD), SSEOp.CVTTSS2SI, QWORD, input);
            case I2D:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.DOUBLE), SSEOp.CVTSI2SD, DWORD, input);
            case I2F:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.SINGLE), SSEOp.CVTSI2SS, DWORD, input);
            case L2D:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.DOUBLE), SSEOp.CVTSI2SD, QWORD, input);
            case L2F:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.SINGLE), SSEOp.CVTSI2SS, QWORD, input);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Value emitNarrow(Value inputVal, int bits) {
        if (inputVal.getPlatformKind() == AMD64Kind.QWORD && bits <= 32) {
            // TODO make it possible to reinterpret Long as Int in LIR without move
            return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.DWORD), AMD64RMOp.MOV, DWORD, inputVal);
        } else {
            return inputVal;
        }
    }

    @Override
    public Value emitSignExtend(Value inputVal, int fromBits, int toBits) {
        assert fromBits <= toBits && toBits <= 64;
        if (fromBits == toBits) {
            return inputVal;
        } else if (toBits > 32) {
            // sign extend to 64 bits
            switch (fromBits) {
                case 8:
                    return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.QWORD), MOVSXB, QWORD, inputVal);
                case 16:
                    return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.QWORD), MOVSX, QWORD, inputVal);
                case 32:
                    return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.QWORD), MOVSXD, QWORD, inputVal);
                default:
                    throw GraalError.unimplemented("unsupported sign extension (" + fromBits + " bit -> " + toBits + " bit)");
            }
        } else {
            // sign extend to 32 bits (smaller values are internally represented as 32 bit values)
            switch (fromBits) {
                case 8:
                    return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.DWORD), MOVSXB, DWORD, inputVal);
                case 16:
                    return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.DWORD), MOVSX, DWORD, inputVal);
                case 32:
                    return inputVal;
                default:
                    throw GraalError.unimplemented("unsupported sign extension (" + fromBits + " bit -> " + toBits + " bit)");
            }
        }
    }

    @Override
    public Value emitZeroExtend(Value inputVal, int fromBits, int toBits) {
        assert fromBits <= toBits && toBits <= 64;
        if (fromBits == toBits) {
            return inputVal;
        } else if (fromBits > 32) {
            assert inputVal.getPlatformKind() == AMD64Kind.QWORD;
            Variable result = getLIRGen().newVariable(LIRKind.combine(inputVal));
            long mask = CodeUtil.mask(fromBits);
            getLIRGen().append(new AMD64Binary.DataTwoOp(AND.getRMOpcode(QWORD), QWORD, result, getLIRGen().asAllocatable(inputVal), JavaConstant.forLong(mask)));
            return result;
        } else {
            LIRKind resultKind = LIRKind.combine(inputVal);
            if (toBits > 32) {
                resultKind = resultKind.changeType(AMD64Kind.QWORD);
            } else {
                resultKind = resultKind.changeType(AMD64Kind.DWORD);
            }

            /*
             * Always emit DWORD operations, even if the resultKind is Long. On AMD64, all DWORD
             * operations implicitly set the upper half of the register to 0, which is what we want
             * anyway. Compared to the QWORD oparations, the encoding of the DWORD operations is
             * sometimes one byte shorter.
             */
            switch (fromBits) {
                case 8:
                    return emitConvertOp(resultKind, MOVZXB, DWORD, inputVal);
                case 16:
                    return emitConvertOp(resultKind, MOVZX, DWORD, inputVal);
                case 32:
                    return emitConvertOp(resultKind, MOV, DWORD, inputVal);
            }

            // odd bit count, fall back on manual masking
            Variable result = getLIRGen().newVariable(resultKind);
            JavaConstant mask;
            if (toBits > 32) {
                mask = JavaConstant.forLong(CodeUtil.mask(fromBits));
            } else {
                mask = JavaConstant.forInt((int) CodeUtil.mask(fromBits));
            }
            getLIRGen().append(new AMD64Binary.DataTwoOp(AND.getRMOpcode(DWORD), DWORD, result, getLIRGen().asAllocatable(inputVal), mask));
            return result;
        }
    }

    @Override
    public Variable emitBitCount(Value value) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value).changeType(AMD64Kind.DWORD));
        assert ((AMD64Kind) value.getPlatformKind()).isInteger();
        if (value.getPlatformKind() == AMD64Kind.QWORD) {
            getLIRGen().append(new AMD64Unary.RMOp(POPCNT, QWORD, result, getLIRGen().asAllocatable(value)));
        } else {
            getLIRGen().append(new AMD64Unary.RMOp(POPCNT, DWORD, result, getLIRGen().asAllocatable(value)));
        }
        return result;
    }

    @Override
    public Variable emitBitScanForward(Value value) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value).changeType(AMD64Kind.DWORD));
        getLIRGen().append(new AMD64Unary.RMOp(BSF, QWORD, result, getLIRGen().asAllocatable(value)));
        return result;
    }

    @Override
    public Variable emitBitScanReverse(Value value) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value).changeType(AMD64Kind.DWORD));
        assert ((AMD64Kind) value.getPlatformKind()).isInteger();
        if (value.getPlatformKind() == AMD64Kind.QWORD) {
            getLIRGen().append(new AMD64Unary.RMOp(BSR, QWORD, result, getLIRGen().asAllocatable(value)));
        } else {
            getLIRGen().append(new AMD64Unary.RMOp(BSR, DWORD, result, getLIRGen().asAllocatable(value)));
        }
        return result;
    }

    @Override
    public Value emitCountLeadingZeros(Value value) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value).changeType(AMD64Kind.DWORD));
        assert ((AMD64Kind) value.getPlatformKind()).isInteger();
        if (value.getPlatformKind() == AMD64Kind.QWORD) {
            getLIRGen().append(new AMD64Unary.RMOp(LZCNT, QWORD, result, getLIRGen().asAllocatable(value)));
        } else {
            getLIRGen().append(new AMD64Unary.RMOp(LZCNT, DWORD, result, getLIRGen().asAllocatable(value)));
        }
        return result;
    }

    @Override
    public Value emitCountTrailingZeros(Value value) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value).changeType(AMD64Kind.DWORD));
        assert ((AMD64Kind) value.getPlatformKind()).isInteger();
        if (value.getPlatformKind() == AMD64Kind.QWORD) {
            getLIRGen().append(new AMD64Unary.RMOp(TZCNT, QWORD, result, getLIRGen().asAllocatable(value)));
        } else {
            getLIRGen().append(new AMD64Unary.RMOp(TZCNT, DWORD, result, getLIRGen().asAllocatable(value)));
        }
        return result;
    }

    @Override
    public Value emitMathAbs(Value input) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        switch ((AMD64Kind) input.getPlatformKind()) {
            case SINGLE:
                getLIRGen().append(new AMD64Binary.DataTwoOp(SSEOp.AND, PS, result, getLIRGen().asAllocatable(input), JavaConstant.forFloat(Float.intBitsToFloat(0x7FFFFFFF)), 16));
                break;
            case DOUBLE:
                getLIRGen().append(new AMD64Binary.DataTwoOp(SSEOp.AND, PD, result, getLIRGen().asAllocatable(input), JavaConstant.forDouble(Double.longBitsToDouble(0x7FFFFFFFFFFFFFFFL)), 16));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    @Override
    public Value emitMathSqrt(Value input) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        switch ((AMD64Kind) input.getPlatformKind()) {
            case SINGLE:
                getLIRGen().append(new AMD64Unary.RMOp(SSEOp.SQRT, SS, result, getLIRGen().asAllocatable(input)));
                break;
            case DOUBLE:
                getLIRGen().append(new AMD64Unary.RMOp(SSEOp.SQRT, SD, result, getLIRGen().asAllocatable(input)));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    @Override
    public Value emitMathLog(Value input, boolean base10) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        AllocatableValue stackSlot = getLIRGen().getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        getLIRGen().append(new AMD64MathIntrinsicUnaryOp(getAMD64LIRGen(), base10 ? LOG10 : LOG, result, getLIRGen().asAllocatable(input), stackSlot));
        return result;
    }

    @Override
    public Value emitMathCos(Value input) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        AllocatableValue stackSlot = getLIRGen().getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        getLIRGen().append(new AMD64MathIntrinsicUnaryOp(getAMD64LIRGen(), COS, result, getLIRGen().asAllocatable(input), stackSlot));
        return result;
    }

    @Override
    public Value emitMathSin(Value input) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        AllocatableValue stackSlot = getLIRGen().getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        getLIRGen().append(new AMD64MathIntrinsicUnaryOp(getAMD64LIRGen(), SIN, result, getLIRGen().asAllocatable(input), stackSlot));
        return result;
    }

    @Override
    public Value emitMathTan(Value input) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        AllocatableValue stackSlot = getLIRGen().getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        getLIRGen().append(new AMD64MathIntrinsicUnaryOp(getAMD64LIRGen(), TAN, result, getLIRGen().asAllocatable(input), stackSlot));
        return result;
    }

    @Override
    public Value emitMathExp(Value input) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        AllocatableValue stackSlot = getLIRGen().getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        getLIRGen().append(new AMD64MathIntrinsicUnaryOp(getAMD64LIRGen(), EXP, result, getLIRGen().asAllocatable(input), stackSlot));
        return result;
    }

    @Override
    public Value emitMathPow(Value input1, Value input2) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input1));
        getLIRGen().append(new AMD64MathIntrinsicBinaryOp(getAMD64LIRGen(), POW, result, getLIRGen().asAllocatable(input1), getLIRGen().asAllocatable(input2)));
        return result;
    }

    protected AMD64LIRGenerator getAMD64LIRGen() {
        return (AMD64LIRGenerator) getLIRGen();
    }

    @Override
    public Variable emitLoad(LIRKind kind, Value address, LIRFrameState state) {
        AMD64AddressValue loadAddress = getAMD64LIRGen().asAddressValue(address);
        Variable result = getLIRGen().newVariable(getLIRGen().toRegisterKind(kind));
        switch ((AMD64Kind) kind.getPlatformKind()) {
            case BYTE:
                getLIRGen().append(new AMD64Unary.MemoryOp(MOVSXB, DWORD, result, loadAddress, state));
                break;
            case WORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(MOVSX, DWORD, result, loadAddress, state));
                break;
            case DWORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(MOV, DWORD, result, loadAddress, state));
                break;
            case QWORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(MOV, QWORD, result, loadAddress, state));
                break;
            case SINGLE:
                getLIRGen().append(new AMD64Unary.MemoryOp(MOVSS, SS, result, loadAddress, state));
                break;
            case DOUBLE:
                getLIRGen().append(new AMD64Unary.MemoryOp(MOVSD, SD, result, loadAddress, state));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    protected void emitStoreConst(AMD64Kind kind, AMD64AddressValue address, ConstantValue value, LIRFrameState state) {
        Constant c = value.getConstant();
        if (JavaConstant.isNull(c)) {
            assert kind == AMD64Kind.DWORD || kind == AMD64Kind.QWORD;
            OperandSize size = kind == AMD64Kind.DWORD ? DWORD : QWORD;
            getLIRGen().append(new AMD64BinaryConsumer.MemoryConstOp(AMD64MIOp.MOV, size, address, 0, state));
            return;
        } else if (c instanceof VMConstant) {
            // only 32-bit constants can be patched
            if (kind == AMD64Kind.DWORD) {
                if (getLIRGen().target().inlineObjects || !(c instanceof JavaConstant)) {
                    // if c is a JavaConstant, it's an oop, otherwise it's a metaspace constant
                    assert !(c instanceof JavaConstant) || ((JavaConstant) c).getJavaKind() == JavaKind.Object;
                    getLIRGen().append(new AMD64BinaryConsumer.MemoryVMConstOp(AMD64MIOp.MOV, address, (VMConstant) c, state));
                    return;
                }
            }
        } else {
            JavaConstant jc = (JavaConstant) c;
            assert jc.getJavaKind().isPrimitive();

            AMD64MIOp op = AMD64MIOp.MOV;
            OperandSize size;
            long imm;

            switch (kind) {
                case BYTE:
                    op = AMD64MIOp.MOVB;
                    size = BYTE;
                    imm = jc.asInt();
                    break;
                case WORD:
                    size = WORD;
                    imm = jc.asInt();
                    break;
                case DWORD:
                    size = DWORD;
                    imm = jc.asInt();
                    break;
                case QWORD:
                    size = QWORD;
                    imm = jc.asLong();
                    break;
                case SINGLE:
                    size = DWORD;
                    imm = Float.floatToRawIntBits(jc.asFloat());
                    break;
                case DOUBLE:
                    size = QWORD;
                    imm = Double.doubleToRawLongBits(jc.asDouble());
                    break;
                default:
                    throw GraalError.shouldNotReachHere("unexpected kind " + kind);
            }

            if (NumUtil.isInt(imm)) {
                getLIRGen().append(new AMD64BinaryConsumer.MemoryConstOp(op, size, address, (int) imm, state));
                return;
            }
        }

        // fallback: load, then store
        emitStore(kind, address, getLIRGen().asAllocatable(value), state);
    }

    protected void emitStore(AMD64Kind kind, AMD64AddressValue address, AllocatableValue value, LIRFrameState state) {
        switch (kind) {
            case BYTE:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOVB, BYTE, address, value, state));
                break;
            case WORD:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOV, WORD, address, value, state));
                break;
            case DWORD:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOV, DWORD, address, value, state));
                break;
            case QWORD:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOV, QWORD, address, value, state));
                break;
            case SINGLE:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOVSS, SS, address, value, state));
                break;
            case DOUBLE:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOVSD, SD, address, value, state));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public void emitStore(ValueKind<?> lirKind, Value address, Value input, LIRFrameState state) {
        AMD64AddressValue storeAddress = getAMD64LIRGen().asAddressValue(address);
        AMD64Kind kind = (AMD64Kind) lirKind.getPlatformKind();
        if (isConstantValue(input)) {
            emitStoreConst(kind, storeAddress, asConstantValue(input), state);
        } else {
            emitStore(kind, storeAddress, getLIRGen().asAllocatable(input), state);
        }
    }

    @Override
    public void emitCompareOp(AMD64Kind cmpKind, Variable left, Value right) {
        OperandSize size;
        switch (cmpKind) {
            case BYTE:
                size = BYTE;
                break;
            case WORD:
                size = WORD;
                break;
            case DWORD:
                size = DWORD;
                break;
            case QWORD:
                size = QWORD;
                break;
            case SINGLE:
                getLIRGen().append(new AMD64BinaryConsumer.Op(SSEOp.UCOMIS, PS, left, getLIRGen().asAllocatable(right)));
                return;
            case DOUBLE:
                getLIRGen().append(new AMD64BinaryConsumer.Op(SSEOp.UCOMIS, PD, left, getLIRGen().asAllocatable(right)));
                return;
            default:
                throw GraalError.shouldNotReachHere("unexpected kind: " + cmpKind);
        }

        if (isConstantValue(right)) {
            Constant c = LIRValueUtil.asConstant(right);
            if (JavaConstant.isNull(c)) {
                getLIRGen().append(new AMD64BinaryConsumer.Op(TEST, size, left, left));
                return;
            } else if (c instanceof VMConstant) {
                VMConstant vc = (VMConstant) c;
                if (size == DWORD && !GeneratePIC.getValue()) {
                    getLIRGen().append(new AMD64BinaryConsumer.VMConstOp(CMP.getMIOpcode(DWORD, false), left, vc));
                } else {
                    getLIRGen().append(new AMD64BinaryConsumer.DataOp(CMP.getRMOpcode(size), size, left, vc));
                }
                return;
            } else if (c instanceof JavaConstant) {
                JavaConstant jc = (JavaConstant) c;
                if (jc.isDefaultForKind()) {
                    AMD64RMOp op = size == BYTE ? TESTB : TEST;
                    getLIRGen().append(new AMD64BinaryConsumer.Op(op, size, left, left));
                    return;
                } else if (NumUtil.is32bit(jc.asLong())) {
                    getLIRGen().append(new AMD64BinaryConsumer.ConstOp(CMP, size, left, (int) jc.asLong()));
                    return;
                }
            }
        }

        // fallback: load, then compare
        getLIRGen().append(new AMD64BinaryConsumer.Op(CMP.getRMOpcode(size), size, left, getLIRGen().asAllocatable(right)));
    }

    @Override
    public Value emitRound(Value value, RoundingMode mode) {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value));
        assert ((AMD64Kind) value.getPlatformKind()).isXMM();
        if (value.getPlatformKind() == AMD64Kind.SINGLE) {
            getLIRGen().append(new AMD64Binary.RMIOp(AMD64RMIOp.ROUNDSS, OperandSize.PD, result, getLIRGen().asAllocatable(value), mode.encoding));
        } else {
            getLIRGen().append(new AMD64Binary.RMIOp(AMD64RMIOp.ROUNDSD, OperandSize.PD, result, getLIRGen().asAllocatable(value), mode.encoding));
        }
        return result;
    }
}
