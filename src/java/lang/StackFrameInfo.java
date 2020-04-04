/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.lang;

import jdk.internal.misc.JavaLangInvokeAccess;
import jdk.internal.misc.SharedSecrets;

import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodType;

class StackFrameInfo implements StackFrame {
    private final byte RETAIN_CLASS_REF = 0x01;

    private final static JavaLangInvokeAccess JLIA =
        SharedSecrets.getJavaLangInvokeAccess();

    private final byte flags;
    private final java.lang.Object memberName;
    private final short bci;
    private volatile StackTraceElement ste;

    /*
     * Create StackFrameInfo for StackFrameTraverser and LiveStackFrameTraverser
     * to use
     */
    StackFrameInfo(java.lang.StackWalker walker) {
        this.flags = walker.retainClassRef ? RETAIN_CLASS_REF : 0;
        this.bci = -1;
        this.memberName = JLIA.newMemberName();
    }

    // package-private called by StackStreamFactory to skip
    // the capability check
    java.lang.Class<?> declaringClass() {
        return JLIA.getDeclaringClass(memberName);
    }

    // ----- implementation of StackFrame methods

    @java.lang.Override
    public java.lang.String getClassName() {
        return declaringClass().getName();
    }

    @java.lang.Override
    public java.lang.Class<?> getDeclaringClass() {
        ensureRetainClassRefEnabled();
        return declaringClass();
    }

    @java.lang.Override
    public java.lang.String getMethodName() {
        return JLIA.getName(memberName);
    }

    @java.lang.Override
    public MethodType getMethodType() {
        ensureRetainClassRefEnabled();
        return JLIA.getMethodType(memberName);
    }

    @java.lang.Override
    public java.lang.String getDescriptor() {
        return JLIA.getMethodDescriptor(memberName);
    }

    @java.lang.Override
    public int getByteCodeIndex() {
        // bci not available for native methods
        if (isNativeMethod())
            return -1;

        return bci;
    }

    @java.lang.Override
    public java.lang.String getFileName() {
        return toStackTraceElement().getFileName();
    }

    @java.lang.Override
    public int getLineNumber() {
        // line number not available for native methods
        if (isNativeMethod())
            return -2;

        return toStackTraceElement().getLineNumber();
    }


    @java.lang.Override
    public boolean isNativeMethod() {
        return JLIA.isNative(memberName);
    }

    @java.lang.Override
    public java.lang.String toString() {
        return toStackTraceElement().toString();
    }

    @java.lang.Override
    public StackTraceElement toStackTraceElement() {
        StackTraceElement s = ste;
        if (s == null) {
            synchronized (this) {
                s = ste;
                if (s == null) {
                    ste = s = StackTraceElement.of(this);
                }
            }
        }
        return s;
    }

    private void ensureRetainClassRefEnabled() {
        if ((flags & RETAIN_CLASS_REF) == 0) {
            throw new UnsupportedOperationException("No access to RETAIN_CLASS_REFERENCE");
        }
    }
}
