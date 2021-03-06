/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
import sun.invoke.util.ValueConversions;

import java.util.ArrayList;
import java.util.List;

import static java.lang.invoke.LambdaForm.BasicType;
import static java.lang.invoke.LambdaForm.BasicType.*;
import static java.lang.invoke.LambdaForm.BasicType.V_TYPE_NUM;
import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;
import static java.lang.invoke.MethodHandleNatives.Constants.*;
import static java.lang.invoke.MethodHandleStatics.newInternalError;
import static java.lang.invoke.MethodHandleStatics.uncaughtException;

/**
 * The flavor of method handle which emulates an invoke instruction
 * on a predetermined argument.  The JVM dispatches to the correct method
 * when the handle is created, not when it is invoked.
 *
 * All bound arguments are encapsulated in dedicated species.
 */
/*non-public*/ abstract class BoundMethodHandle extends java.lang.invoke.MethodHandle {

    /*non-public*/ BoundMethodHandle(java.lang.invoke.MethodType type, java.lang.invoke.LambdaForm form) {
        super(type, form);
        assert(speciesData() == speciesDataFor(form));
    }

    //
    // BMH API and internals
    //

    static BoundMethodHandle bindSingle(java.lang.invoke.MethodType type, java.lang.invoke.LambdaForm form, BasicType xtype, Object x) {
        // for some type signatures, there exist pre-defined concrete BMH classes
        try {
            switch (xtype) {
            case L_TYPE:
                return bindSingle(type, form, x);  // Use known fast path.
            case I_TYPE:
                return (BoundMethodHandle) SPECIALIZER.topSpecies().extendWith(I_TYPE_NUM).factory().invokeBasic(type, form, ValueConversions.widenSubword(x));
            case J_TYPE:
                return (BoundMethodHandle) SPECIALIZER.topSpecies().extendWith(J_TYPE_NUM).factory().invokeBasic(type, form, (long) x);
            case F_TYPE:
                return (BoundMethodHandle) SPECIALIZER.topSpecies().extendWith(F_TYPE_NUM).factory().invokeBasic(type, form, (float) x);
            case D_TYPE:
                return (BoundMethodHandle) SPECIALIZER.topSpecies().extendWith(D_TYPE_NUM).factory().invokeBasic(type, form, (double) x);
            default : throw newInternalError("unexpected xtype: " + xtype);
            }
        } catch (Throwable t) {
            throw uncaughtException(t);
        }
    }

    /*non-public*/
    java.lang.invoke.LambdaFormEditor editor() {
        return form.editor();
    }

    static BoundMethodHandle bindSingle(java.lang.invoke.MethodType type, java.lang.invoke.LambdaForm form, Object x) {
        return Species_L.make(type, form, x);
    }

    @Override // there is a default binder in the super class, for 'L' types only
    /*non-public*/
    BoundMethodHandle bindArgumentL(int pos, Object value) {
        return editor().bindArgumentL(this, pos, value);
    }

    /*non-public*/
    BoundMethodHandle bindArgumentI(int pos, int value) {
        return editor().bindArgumentI(this, pos, value);
    }
    /*non-public*/
    BoundMethodHandle bindArgumentJ(int pos, long value) {
        return editor().bindArgumentJ(this, pos, value);
    }
    /*non-public*/
    BoundMethodHandle bindArgumentF(int pos, float value) {
        return editor().bindArgumentF(this, pos, value);
    }
    /*non-public*/
    BoundMethodHandle bindArgumentD(int pos, double value) {
        return editor().bindArgumentD(this, pos, value);
    }
    @Override
    BoundMethodHandle rebind() {
        if (!tooComplex()) {
            return this;
        }
        return makeReinvoker(this);
    }

    private boolean tooComplex() {
        return (fieldCount() > FIELD_COUNT_THRESHOLD ||
                form.expressionCount() > FORM_EXPRESSION_THRESHOLD);
    }
    private static final int FIELD_COUNT_THRESHOLD = 12;      // largest convenient BMH field count
    private static final int FORM_EXPRESSION_THRESHOLD = 24;  // largest convenient BMH expression count

    /**
     * A reinvoker MH has this form:
     * {@code lambda (bmh, arg*) { thismh = bmh[0]; invokeBasic(thismh, arg*) }}
     */
    static BoundMethodHandle makeReinvoker(java.lang.invoke.MethodHandle target) {
        java.lang.invoke.LambdaForm form = DelegatingMethodHandle.makeReinvokerForm(
                target, java.lang.invoke.MethodTypeForm.LF_REBIND,
                Species_L.BMH_SPECIES, Species_L.BMH_SPECIES.getterFunction(0));
        return Species_L.make(target.type(), form, target);
    }

    /**
     * Return the {@link SpeciesData} instance representing this BMH species. All subclasses must provide a
     * static field containing this value, and they must accordingly implement this method.
     */
    /*non-public*/ abstract SpeciesData speciesData();

    /*non-public*/ static SpeciesData speciesDataFor(java.lang.invoke.LambdaForm form) {
        Object c = form.names[0].constraint;
        if (c instanceof SpeciesData) {
            return (SpeciesData) c;
        }
        // if there is no BMH constraint, then use the null constraint
        return SPECIALIZER.topSpecies();
    }

    /**
     * Return the number of fields in this BMH.  Equivalent to speciesData().fieldCount().
     */
    /*non-public*/ final int fieldCount() { return speciesData().fieldCount(); }

    @Override
    Object internalProperties() {
        return "\n& BMH="+internalValues();
    }

    @Override
    final String internalValues() {
        int count = fieldCount();
        if (count == 1) {
            return "[" + arg(0) + "]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; ++i) {
            sb.append("\n  ").append(i).append(": ( ").append(arg(i)).append(" )");
        }
        return sb.append("\n]").toString();
    }

    /*non-public*/ final Object arg(int i) {
        try {
            Class<?> fieldType = speciesData().fieldTypes().get(i);
            switch (BasicType.basicType(fieldType)) {
                case L_TYPE: return          speciesData().getter(i).invokeBasic(this);
                case I_TYPE: return (int)    speciesData().getter(i).invokeBasic(this);
                case J_TYPE: return (long)   speciesData().getter(i).invokeBasic(this);
                case F_TYPE: return (float)  speciesData().getter(i).invokeBasic(this);
                case D_TYPE: return (double) speciesData().getter(i).invokeBasic(this);
            }
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        throw new InternalError("unexpected type: " + speciesData().key()+"."+i);
    }

    //
    // cloning API
    //

    /*non-public*/ abstract BoundMethodHandle copyWith(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf);
    /*non-public*/ abstract BoundMethodHandle copyWithExtendL(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, Object narg);
    /*non-public*/ abstract BoundMethodHandle copyWithExtendI(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, int    narg);
    /*non-public*/ abstract BoundMethodHandle copyWithExtendJ(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, long   narg);
    /*non-public*/ abstract BoundMethodHandle copyWithExtendF(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, float  narg);
    /*non-public*/ abstract BoundMethodHandle copyWithExtendD(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, double narg);

    //
    // concrete BMH classes required to close bootstrap loops
    //

    private  // make it private to force users to access the enclosing class first
    static final class Species_L extends BoundMethodHandle {

        final Object argL0;

        private Species_L(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, Object argL0) {
            super(mt, lf);
            this.argL0 = argL0;
        }

        @Override
        /*non-public*/ java.lang.invoke.BoundMethodHandle.SpeciesData speciesData() {
            return BMH_SPECIES;
        }

        /*non-public*/ static @Stable
        java.lang.invoke.BoundMethodHandle.SpeciesData BMH_SPECIES;

        /*non-public*/ static BoundMethodHandle make(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, Object argL0) {
            return new java.lang.invoke.BoundMethodHandle.Species_L(mt, lf, argL0);
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWith(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf) {
            return new java.lang.invoke.BoundMethodHandle.Species_L(mt, lf, argL0);
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWithExtendL(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, Object narg) {
            try {
                return (BoundMethodHandle) BMH_SPECIES.extendWith(L_TYPE_NUM).factory().invokeBasic(mt, lf, argL0, narg);
            } catch (Throwable ex) {
                throw uncaughtException(ex);
            }
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWithExtendI(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, int narg) {
            try {
                return (BoundMethodHandle) BMH_SPECIES.extendWith(I_TYPE_NUM).factory().invokeBasic(mt, lf, argL0, narg);
            } catch (Throwable ex) {
                throw uncaughtException(ex);
            }
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWithExtendJ(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, long narg) {
            try {
                return (BoundMethodHandle) BMH_SPECIES.extendWith(J_TYPE_NUM).factory().invokeBasic(mt, lf, argL0, narg);
            } catch (Throwable ex) {
                throw uncaughtException(ex);
            }
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWithExtendF(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, float narg) {
            try {
                return (BoundMethodHandle) BMH_SPECIES.extendWith(F_TYPE_NUM).factory().invokeBasic(mt, lf, argL0, narg);
            } catch (Throwable ex) {
                throw uncaughtException(ex);
            }
        }
        @Override
        /*non-public*/ final BoundMethodHandle copyWithExtendD(java.lang.invoke.MethodType mt, java.lang.invoke.LambdaForm lf, double narg) {
            try {
                return (BoundMethodHandle) BMH_SPECIES.extendWith(D_TYPE_NUM).factory().invokeBasic(mt, lf, argL0, narg);
            } catch (Throwable ex) {
                throw uncaughtException(ex);
            }
        }
    }

    //
    // BMH species meta-data
    //

    /*non-public*/
    static final class SpeciesData extends java.lang.invoke.ClassSpecializer<BoundMethodHandle, String, SpeciesData>.SpeciesData {
        // This array is filled in lazily, as new species come into being over time.
        @Stable final private SpeciesData[] extensions = new SpeciesData[ARG_TYPE_LIMIT];

        public SpeciesData(Specializer outer, String key) {
            outer.super(key);
        }

        @Override
        protected String deriveClassName() {
            String typeString = deriveTypeString();
            if (typeString.isEmpty()) {
                return SimpleMethodHandle.class.getName();
            }
            return BoundMethodHandle.class.getName() + "$Species_" + typeString;
        }

        @Override
        protected List<Class<?>> deriveFieldTypes(String key) {
            ArrayList<Class<?>> types = new ArrayList<>(key.length());
            for (int i = 0; i < key.length(); i++) {
                types.add(basicType(key.charAt(i)).basicTypeClass());
            }
            return types;
        }

        @Override
        protected String deriveTypeString() {
            // (If/when we have to add nominal types, just inherit the more complex default.)
            return key();
        }

        @Override
        protected MethodHandle deriveTransformHelper(java.lang.invoke.MemberName transform, int whichtm) {
            if (whichtm == Specializer.TN_COPY_NO_EXTEND) {
                return factory();
            } else if (whichtm < ARG_TYPE_LIMIT) {
                return extendWith((byte) whichtm).factory();
            } else {
                throw newInternalError("bad transform");
            }
        }

        @Override
        protected <X> List<X> deriveTransformHelperArguments(java.lang.invoke.MemberName transform, int whichtm, List<X> args, List<X> fields) {
            assert(verifyTHAargs(transform, whichtm, args, fields));
            // The rule is really simple:  Keep the first two arguments
            // the same, then put in the fields, then put any other argument.
            args.addAll(2, fields);
            return args;
        }

        private boolean verifyTHAargs(java.lang.invoke.MemberName transform, int whichtm, List<?> args, List<?> fields) {
            assert(transform == Specializer.BMH_TRANSFORMS.get(whichtm));
            assert(args.size() == transform.getMethodType().parameterCount());
            assert(fields.size() == this.fieldCount());
            final int MH_AND_LF = 2;
            if (whichtm == Specializer.TN_COPY_NO_EXTEND) {
                assert(transform.getMethodType().parameterCount() == MH_AND_LF);
            } else if (whichtm < ARG_TYPE_LIMIT) {
                assert(transform.getMethodType().parameterCount() == MH_AND_LF+1);
                final BasicType type = basicType((byte) whichtm);
                assert(transform.getParameterTypes()[MH_AND_LF] == type.basicTypeClass());
            } else {
                return false;
            }
            return true;
        }

        /*non-public*/ SpeciesData extendWith(byte typeNum) {
            SpeciesData sd = extensions[typeNum];
            if (sd != null)  return sd;
            sd = SPECIALIZER.findSpecies(key() + BasicType.basicType(typeNum).basicTypeChar());
            extensions[typeNum] = sd;
            return sd;
        }
    }

    /*non-public*/
    static final Specializer SPECIALIZER = new Specializer();
    static {
        SimpleMethodHandle.BMH_SPECIES = BoundMethodHandle.SPECIALIZER.findSpecies("");
        Species_L.BMH_SPECIES = BoundMethodHandle.SPECIALIZER.findSpecies("L");
    }

    /*non-public*/
    static final class Specializer extends java.lang.invoke.ClassSpecializer<BoundMethodHandle, String, SpeciesData> {

        private static final java.lang.invoke.MemberName SPECIES_DATA_ACCESSOR;

        static {
            try {
                SPECIES_DATA_ACCESSOR = IMPL_LOOKUP.resolveOrFail(REF_invokeVirtual, BoundMethodHandle.class,
                        "speciesData", java.lang.invoke.MethodType.methodType(BoundMethodHandle.SpeciesData.class));
            } catch (ReflectiveOperationException ex) {
                throw newInternalError("Bootstrap link error", ex);
            }
        }

        private Specializer() {
            super(  // Reified type parameters:
                    BoundMethodHandle.class, String.class, BoundMethodHandle.SpeciesData.class,
                    // Principal constructor type:
                    java.lang.invoke.MethodType.methodType(void.class, java.lang.invoke.MethodType.class, java.lang.invoke.LambdaForm.class),
                    // Required linkage between class and species:
                    SPECIES_DATA_ACCESSOR,
                    "BMH_SPECIES",
                    BMH_TRANSFORMS);
        }

        @Override
        protected String topSpeciesKey() {
            return "";
        }

        @Override
        protected BoundMethodHandle.SpeciesData newSpeciesData(String key) {
            return new BoundMethodHandle.SpeciesData(this, key);
        }

        static final List<java.lang.invoke.MemberName> BMH_TRANSFORMS;
        static final int TN_COPY_NO_EXTEND = V_TYPE_NUM;
        static {
            final Class<BoundMethodHandle> BMH = BoundMethodHandle.class;
            // copyWithExtendLIJFD + copyWith
            try {
                BMH_TRANSFORMS = List.of(
                        IMPL_LOOKUP.resolveOrFail(REF_invokeVirtual, BMH, "copyWithExtendL", java.lang.invoke.MethodType.methodType(BMH, java.lang.invoke.MethodType.class, java.lang.invoke.LambdaForm.class, Object.class)),
                        IMPL_LOOKUP.resolveOrFail(REF_invokeVirtual, BMH, "copyWithExtendI", java.lang.invoke.MethodType.methodType(BMH, java.lang.invoke.MethodType.class, java.lang.invoke.LambdaForm.class, int.class)),
                        IMPL_LOOKUP.resolveOrFail(REF_invokeVirtual, BMH, "copyWithExtendJ", java.lang.invoke.MethodType.methodType(BMH, java.lang.invoke.MethodType.class, java.lang.invoke.LambdaForm.class, long.class)),
                        IMPL_LOOKUP.resolveOrFail(REF_invokeVirtual, BMH, "copyWithExtendF", java.lang.invoke.MethodType.methodType(BMH, java.lang.invoke.MethodType.class, java.lang.invoke.LambdaForm.class, float.class)),
                        IMPL_LOOKUP.resolveOrFail(REF_invokeVirtual, BMH, "copyWithExtendD", java.lang.invoke.MethodType.methodType(BMH, java.lang.invoke.MethodType.class, java.lang.invoke.LambdaForm.class, double.class)),
                        IMPL_LOOKUP.resolveOrFail(REF_invokeVirtual, BMH, "copyWith", java.lang.invoke.MethodType.methodType(BMH, MethodType.class, java.lang.invoke.LambdaForm.class))
                );
            } catch (ReflectiveOperationException ex) {
                throw newInternalError("Failed resolving copyWith methods", ex);
            }

            // as it happens, there is one transform per BasicType including V_TYPE
            assert(BMH_TRANSFORMS.size() == TYPE_LIMIT);
        }

        /**
         * Generation of concrete BMH classes.
         *
         * A concrete BMH species is fit for binding a number of values adhering to a
         * given type pattern. Reference types are erased.
         *
         * BMH species are cached by type pattern.
         *
         * A BMH species has a number of fields with the concrete (possibly erased) types of
         * bound values. Setters are provided as an API in BMH. Getters are exposed as MHs,
         * which can be included as names in lambda forms.
         */
        class Factory extends java.lang.invoke.ClassSpecializer<BoundMethodHandle, String, BoundMethodHandle.SpeciesData>.Factory {
            @Override
            protected String chooseFieldName(Class<?> type, int index) {
                return "arg" + super.chooseFieldName(type, index);
            }
        }

        @Override
        protected Factory makeFactory() {
            return new Factory();
        }
      }

    static SpeciesData speciesData_L()      { return Species_L.BMH_SPECIES; }
    static SpeciesData speciesData_LL()     { return SPECIALIZER.findSpecies("LL"); }
    static SpeciesData speciesData_LLL()    { return SPECIALIZER.findSpecies("LLL"); }
    static SpeciesData speciesData_LLLL()   { return SPECIALIZER.findSpecies("LLLL"); }
    static SpeciesData speciesData_LLLLL()  { return SPECIALIZER.findSpecies("LLLLL"); }
}
