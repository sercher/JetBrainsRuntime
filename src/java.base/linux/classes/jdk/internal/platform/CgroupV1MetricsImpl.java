/*
 * Copyright (c) 2020, Red Hat Inc.
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

package jdk.internal.platform;

/**
 * Cgroup v1 Metrics extensions
 *
 */
public class CgroupV1MetricsImpl extends CgroupMetrics implements CgroupV1Metrics {

    private final CgroupV1Metrics metrics;

    CgroupV1MetricsImpl(CgroupV1Metrics metrics) {
        super((CgroupSubsystem)metrics);
        this.metrics = metrics;
    }

    @Override
    public long getMemoryMaxUsage() {
        return metrics.getMemoryMaxUsage();
    }

    @Override
    public long getKernelMemoryFailCount() {
        return metrics.getKernelMemoryFailCount();
    }

    @Override
    public long getKernelMemoryLimit() {
        return metrics.getKernelMemoryLimit();
    }

    @Override
    public long getKernelMemoryMaxUsage() {
        return metrics.getKernelMemoryMaxUsage();
    }

    @Override
    public long getKernelMemoryUsage() {
        return metrics.getKernelMemoryUsage();
    }

    @Override
    public long getTcpMemoryFailCount() {
        return metrics.getTcpMemoryFailCount();
    }

    @Override
    public long getTcpMemoryLimit() {
        return metrics.getTcpMemoryLimit();
    }

    @Override
    public long getTcpMemoryMaxUsage() {
        return metrics.getTcpMemoryMaxUsage();
    }

    @Override
    public long getMemoryAndSwapFailCount() {
        return metrics.getMemoryAndSwapFailCount();
    }

    @Override
    public long getMemoryAndSwapMaxUsage() {
        return metrics.getMemoryAndSwapMaxUsage();
    }

    @Override
    public Boolean isMemoryOOMKillEnabled() {
        return metrics.isMemoryOOMKillEnabled();
    }

    @Override
    public double getCpuSetMemoryPressure() {
        return metrics.getCpuSetMemoryPressure();
    }

    @Override
    public Boolean isCpuSetMemoryPressureEnabled() {
        return metrics.isCpuSetMemoryPressureEnabled();
    }

}
