/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;

import jdk.internal.reflect.MethodAccessor;
import jdk.internal.reflect.ConstructorAccessor;

/** Package-private class implementing the
    sun.reflect.LangReflectAccess interface, allowing the java.lang
    package to instantiate objects in this package. */

class ReflectAccess implements jdk.internal.reflect.LangReflectAccess {
    public java.lang.reflect.Field newField(Class<?> declaringClass,
                                            String name,
                                            Class<?> type,
                                            int modifiers,
                                            int slot,
                                            String signature,
                                            byte[] annotations)
    {
        return new java.lang.reflect.Field(declaringClass,
                         name,
                         type,
                         modifiers,
                         slot,
                         signature,
                         annotations);
    }

    public java.lang.reflect.Method newMethod(Class<?> declaringClass,
                                              String name,
                                              Class<?>[] parameterTypes,
                                              Class<?> returnType,
                                              Class<?>[] checkedExceptions,
                                              int modifiers,
                                              int slot,
                                              String signature,
                                              byte[] annotations,
                                              byte[] parameterAnnotations,
                                              byte[] annotationDefault)
    {
        return new java.lang.reflect.Method(declaringClass,
                          name,
                          parameterTypes,
                          returnType,
                          checkedExceptions,
                          modifiers,
                          slot,
                          signature,
                          annotations,
                          parameterAnnotations,
                          annotationDefault);
    }

    public <T> java.lang.reflect.Constructor<T> newConstructor(Class<T> declaringClass,
                                                               Class<?>[] parameterTypes,
                                                               Class<?>[] checkedExceptions,
                                                               int modifiers,
                                                               int slot,
                                                               String signature,
                                                               byte[] annotations,
                                                               byte[] parameterAnnotations)
    {
        return new java.lang.reflect.Constructor<>(declaringClass,
                                  parameterTypes,
                                  checkedExceptions,
                                  modifiers,
                                  slot,
                                  signature,
                                  annotations,
                                  parameterAnnotations);
    }

    public MethodAccessor getMethodAccessor(java.lang.reflect.Method m) {
        return m.getMethodAccessor();
    }

    public void setMethodAccessor(java.lang.reflect.Method m, MethodAccessor accessor) {
        m.setMethodAccessor(accessor);
    }

    public ConstructorAccessor getConstructorAccessor(java.lang.reflect.Constructor<?> c) {
        return c.getConstructorAccessor();
    }

    public void setConstructorAccessor(java.lang.reflect.Constructor<?> c,
                                       ConstructorAccessor accessor)
    {
        c.setConstructorAccessor(accessor);
    }

    public int getConstructorSlot(java.lang.reflect.Constructor<?> c) {
        return c.getSlot();
    }

    public String getConstructorSignature(java.lang.reflect.Constructor<?> c) {
        return c.getSignature();
    }

    public byte[] getConstructorAnnotations(java.lang.reflect.Constructor<?> c) {
        return c.getRawAnnotations();
    }

    public byte[] getConstructorParameterAnnotations(java.lang.reflect.Constructor<?> c) {
        return c.getRawParameterAnnotations();
    }

    public byte[] getExecutableTypeAnnotationBytes(java.lang.reflect.Executable ex) {
        return ex.getTypeAnnotationBytes();
    }

    public Class<?>[] getExecutableSharedParameterTypes(Executable ex) {
        return ex.getSharedParameterTypes();
    }

    //
    // Copying routines, needed to quickly fabricate new Field,
    // Method, and Constructor objects from templates
    //
    public java.lang.reflect.Method copyMethod(java.lang.reflect.Method arg) {
        return arg.copy();
    }
    public java.lang.reflect.Method leafCopyMethod(Method arg) {
        return arg.leafCopy();
    }

    public java.lang.reflect.Field copyField(Field arg) {
        return arg.copy();
    }

    public <T> java.lang.reflect.Constructor<T> copyConstructor(Constructor<T> arg) {
        return arg.copy();
    }

    @SuppressWarnings("unchecked")
    public <T extends AccessibleObject> T getRoot(T obj) {
        return (T) obj.getRoot();
    }
}
