/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import jdk.internal.vm.annotation.Stable;

import static java.lang.invoke.LambdaForm.BasicType.*;
import static java.lang.invoke.MethodHandleStatics.*;

/**
 * A method handle whose behavior is determined only by its LambdaForm.
 * @author jrose
 */
final class SimpleMethodHandle extends java.lang.invoke.BoundMethodHandle {

    private SimpleMethodHandle(java.lang.invoke.MethodType type, java.lang.invoke.LambdaForm form) {
        super(type, form);
    }

    /*non-public*/ static java.lang.invoke.BoundMethodHandle make(java.lang.invoke.MethodType type, java.lang.invoke.LambdaForm form) {
        return new SimpleMethodHandle(type, form);
    }

    /*non-public*/ static @Stable java.lang.invoke.BoundMethodHandle.SpeciesData BMH_SPECIES;

    @Override
    /*non-public*/ java.lang.invoke.BoundMethodHandle.SpeciesData speciesData() {
            return BMH_SPECIES;
    }

    @Override
    /*non-public*/ java.lang.invoke.BoundMethodHandle copyWith(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf) {
        return make(mt, lf);
    }

    @Override
    String internalProperties() {
        return "\n& Class="+getClass().getSimpleName();
    }

    @Override
    /*non-public*/ final java.lang.invoke.BoundMethodHandle copyWithExtendL(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, Object narg) {
        return java.lang.invoke.BoundMethodHandle.bindSingle(mt, lf, narg); // Use known fast path.
    }
    @Override
    /*non-public*/ final java.lang.invoke.BoundMethodHandle copyWithExtendI(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, int narg) {
        try {
            return (java.lang.invoke.BoundMethodHandle) BMH_SPECIES.extendWith(I_TYPE_NUM).factory().invokeBasic(mt, lf, narg);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
    }
    @Override
    /*non-public*/ final java.lang.invoke.BoundMethodHandle copyWithExtendJ(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, long narg) {
        try {
            return (java.lang.invoke.BoundMethodHandle) BMH_SPECIES.extendWith(J_TYPE_NUM).factory().invokeBasic(mt, lf, narg);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
    }
    @Override
    /*non-public*/ final java.lang.invoke.BoundMethodHandle copyWithExtendF(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, float narg) {
        try {
            return (java.lang.invoke.BoundMethodHandle) BMH_SPECIES.extendWith(F_TYPE_NUM).factory().invokeBasic(mt, lf, narg);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
    }
    @Override
    /*non-public*/ final java.lang.invoke.BoundMethodHandle copyWithExtendD(MethodType mt, java.lang.invoke.LambdaForm lf, double narg) {
        try {
            return (java.lang.invoke.BoundMethodHandle) BMH_SPECIES.extendWith(D_TYPE_NUM).factory().invokeBasic(mt, lf, narg);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
    }
}
