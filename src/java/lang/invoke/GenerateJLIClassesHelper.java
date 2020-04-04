/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import sun.invoke.util.Wrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * Helper class to assist the GenerateJLIClassesPlugin to get access to
 * generate classes ahead of time.
 */
class GenerateJLIClassesHelper {

    static byte[] generateBasicFormsClassBytes(String className) {
        ArrayList<java.lang.invoke.LambdaForm> forms = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        HashSet<String> dedupSet = new HashSet<>();
        for (java.lang.invoke.LambdaForm.BasicType type : java.lang.invoke.LambdaForm.BasicType.values()) {
            java.lang.invoke.LambdaForm zero = java.lang.invoke.LambdaForm.zeroForm(type);
            String name = zero.kind.defaultLambdaName
                   + "_" + zero.returnType().basicTypeChar();
            if (dedupSet.add(name)) {
                names.add(name);
                forms.add(zero);
            }

            java.lang.invoke.LambdaForm identity = java.lang.invoke.LambdaForm.identityForm(type);
            name = identity.kind.defaultLambdaName
                   + "_" + identity.returnType().basicTypeChar();
            if (dedupSet.add(name)) {
                names.add(name);
                forms.add(identity);
            }
        }
        return generateCodeBytesForLFs(className,
                names.toArray(new String[0]),
                forms.toArray(new java.lang.invoke.LambdaForm[0]));
    }

    static byte[] generateDirectMethodHandleHolderClassBytes(String className,
                                                             java.lang.invoke.MethodType[] methodTypes, int[] types) {
        ArrayList<java.lang.invoke.LambdaForm> forms = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < methodTypes.length; i++) {
            java.lang.invoke.LambdaForm form = java.lang.invoke.DirectMethodHandle
                    .makePreparedLambdaForm(methodTypes[i], types[i]);
            forms.add(form);
            names.add(form.kind.defaultLambdaName);
        }
        for (Wrapper wrapper : Wrapper.values()) {
            if (wrapper == Wrapper.VOID) {
                continue;
            }
            for (byte b = java.lang.invoke.DirectMethodHandle.AF_GETFIELD; b < java.lang.invoke.DirectMethodHandle.AF_LIMIT; b++) {
                int ftype = java.lang.invoke.DirectMethodHandle.ftypeKind(wrapper.primitiveType());
                java.lang.invoke.LambdaForm form = java.lang.invoke.DirectMethodHandle
                        .makePreparedFieldLambdaForm(b, /*isVolatile*/false, ftype);
                if (form.kind != java.lang.invoke.LambdaForm.Kind.GENERIC) {
                    forms.add(form);
                    names.add(form.kind.defaultLambdaName);
                }
                // volatile
                form = java.lang.invoke.DirectMethodHandle
                        .makePreparedFieldLambdaForm(b, /*isVolatile*/true, ftype);
                if (form.kind != java.lang.invoke.LambdaForm.Kind.GENERIC) {
                    forms.add(form);
                    names.add(form.kind.defaultLambdaName);
                }
            }
        }
        return generateCodeBytesForLFs(className,
                names.toArray(new String[0]),
                forms.toArray(new java.lang.invoke.LambdaForm[0]));
    }

    static byte[] generateDelegatingMethodHandleHolderClassBytes(String className,
            java.lang.invoke.MethodType[] methodTypes) {

        HashSet<java.lang.invoke.MethodType> dedupSet = new HashSet<>();
        ArrayList<java.lang.invoke.LambdaForm> forms = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < methodTypes.length; i++) {
            // generate methods representing the DelegatingMethodHandle
            if (dedupSet.add(methodTypes[i])) {
                // reinvokers are variant with the associated SpeciesData
                // and shape of the target LF, but we can easily pregenerate
                // the basic reinvokers associated with Species_L. Ultimately we
                // may want to consider pregenerating more of these, which will
                // require an even more complex naming scheme
                java.lang.invoke.LambdaForm reinvoker = makeReinvokerFor(methodTypes[i]);
                forms.add(reinvoker);
                String speciesSig = java.lang.invoke.BoundMethodHandle.speciesDataFor(reinvoker).key();
                assert(speciesSig.equals("L"));
                names.add(reinvoker.kind.defaultLambdaName + "_" + speciesSig);

                java.lang.invoke.LambdaForm delegate = makeDelegateFor(methodTypes[i]);
                forms.add(delegate);
                names.add(delegate.kind.defaultLambdaName);
            }
        }
        return generateCodeBytesForLFs(className,
                names.toArray(new String[0]),
                forms.toArray(new java.lang.invoke.LambdaForm[0]));
    }

    static byte[] generateInvokersHolderClassBytes(String className,
                                                   java.lang.invoke.MethodType[] invokerMethodTypes, java.lang.invoke.MethodType[] callSiteMethodTypes) {

        HashSet<java.lang.invoke.MethodType> dedupSet = new HashSet<>();
        ArrayList<java.lang.invoke.LambdaForm> forms = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        int[] types = {
            java.lang.invoke.MethodTypeForm.LF_EX_LINKER,
            java.lang.invoke.MethodTypeForm.LF_EX_INVOKER,
            java.lang.invoke.MethodTypeForm.LF_GEN_LINKER,
            java.lang.invoke.MethodTypeForm.LF_GEN_INVOKER
        };

        for (int i = 0; i < invokerMethodTypes.length; i++) {
            // generate methods representing invokers of the specified type
            if (dedupSet.add(invokerMethodTypes[i])) {
                for (int type : types) {
                    java.lang.invoke.LambdaForm invokerForm = java.lang.invoke.Invokers.invokeHandleForm(invokerMethodTypes[i],
                            /*customized*/false, type);
                    forms.add(invokerForm);
                    names.add(invokerForm.kind.defaultLambdaName);
                }
            }
        }

        dedupSet = new HashSet<>();
        for (int i = 0; i < callSiteMethodTypes.length; i++) {
            // generate methods representing invokers of the specified type
            if (dedupSet.add(callSiteMethodTypes[i])) {
                java.lang.invoke.LambdaForm callSiteForm = java.lang.invoke.Invokers.callSiteForm(callSiteMethodTypes[i], true);
                forms.add(callSiteForm);
                names.add(callSiteForm.kind.defaultLambdaName);

                java.lang.invoke.LambdaForm methodHandleForm = java.lang.invoke.Invokers.callSiteForm(callSiteMethodTypes[i], false);
                forms.add(methodHandleForm);
                names.add(methodHandleForm.kind.defaultLambdaName);
            }
        }

        return generateCodeBytesForLFs(className,
                names.toArray(new String[0]),
                forms.toArray(new java.lang.invoke.LambdaForm[0]));
    }

    /*
     * Generate customized code for a set of LambdaForms of specified types into
     * a class with a specified name.
     */
    private static byte[] generateCodeBytesForLFs(String className,
            String[] names, java.lang.invoke.LambdaForm[] forms) {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                className, null, InvokerBytecodeGenerator.INVOKER_SUPER_NAME, null);
        cw.visitSource(className.substring(className.lastIndexOf('/') + 1), null);

        for (int i = 0; i < forms.length; i++) {
            addMethod(className, names[i], forms[i],
                    forms[i].methodType(), cw);
        }
        return cw.toByteArray();
    }

    private static void addMethod(String className, String methodName, java.lang.invoke.LambdaForm form,
                                  java.lang.invoke.MethodType type, ClassWriter cw) {
        InvokerBytecodeGenerator g
                = new InvokerBytecodeGenerator(className, methodName, form, type);
        g.setClassWriter(cw);
        g.addMethod();
    }

    private static java.lang.invoke.LambdaForm makeReinvokerFor(java.lang.invoke.MethodType type) {
        java.lang.invoke.MethodHandle emptyHandle = java.lang.invoke.MethodHandles.empty(type);
        return java.lang.invoke.DelegatingMethodHandle.makeReinvokerForm(emptyHandle,
                java.lang.invoke.MethodTypeForm.LF_REBIND,
                java.lang.invoke.BoundMethodHandle.speciesData_L(),
                java.lang.invoke.BoundMethodHandle.speciesData_L().getterFunction(0));
    }

    private static java.lang.invoke.LambdaForm makeDelegateFor(MethodType type) {
        MethodHandle handle = MethodHandles.empty(type);
        return java.lang.invoke.DelegatingMethodHandle.makeReinvokerForm(
                handle,
                java.lang.invoke.MethodTypeForm.LF_DELEGATE,
                java.lang.invoke.DelegatingMethodHandle.class,
                java.lang.invoke.DelegatingMethodHandle.NF_getTarget);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Map.Entry<String, byte[]> generateConcreteBMHClassBytes(final String types) {
        for (char c : types.toCharArray()) {
            if ("LIJFD".indexOf(c) < 0) {
                throw new IllegalArgumentException("All characters must "
                        + "correspond to a basic field type: LIJFD");
            }
        }
        final java.lang.invoke.BoundMethodHandle.SpeciesData species = java.lang.invoke.BoundMethodHandle.SPECIALIZER.findSpecies(types);
        final String className = species.speciesCode().getName();
        final java.lang.invoke.ClassSpecializer.Factory factory = java.lang.invoke.BoundMethodHandle.SPECIALIZER.factory();
        final byte[] code = factory.generateConcreteSpeciesCodeFile(className, species);
        return Map.entry(className.replace('.', '/'), code);
    }

}
