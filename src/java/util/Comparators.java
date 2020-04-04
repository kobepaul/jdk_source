/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * Package private supporting class for {@link java.util.Comparator}.
 */
class Comparators {
    private Comparators() {
        throw new AssertionError("no instances");
    }

    /**
     * Compares {@link Comparable} objects in natural order.
     *
     * @see Comparable
     */
    enum NaturalOrderComparator implements java.util.Comparator<Comparable<Object>> {
        INSTANCE;

        @Override
        public int compare(Comparable<Object> c1, Comparable<Object> c2) {
            return c1.compareTo(c2);
        }

        @Override
        public java.util.Comparator<Comparable<Object>> reversed() {
            return java.util.Comparator.reverseOrder();
        }
    }

    /**
     * Null-friendly comparators
     */
    static final class NullComparator<T> implements java.util.Comparator<T>, Serializable {
        private static final long serialVersionUID = -7569533591570686392L;
        private final boolean nullFirst;
        // if null, non-null Ts are considered equal
        private final java.util.Comparator<T> real;

        @SuppressWarnings("unchecked")
        NullComparator(boolean nullFirst, java.util.Comparator<? super T> real) {
            this.nullFirst = nullFirst;
            this.real = (java.util.Comparator<T>) real;
        }

        @Override
        public int compare(T a, T b) {
            if (a == null) {
                return (b == null) ? 0 : (nullFirst ? -1 : 1);
            } else if (b == null) {
                return nullFirst ? 1: -1;
            } else {
                return (real == null) ? 0 : real.compare(a, b);
            }
        }

        @Override
        public java.util.Comparator<T> thenComparing(java.util.Comparator<? super T> other) {
            Objects.requireNonNull(other);
            return new NullComparator<>(nullFirst, real == null ? other : real.thenComparing(other));
        }

        @Override
        public Comparator<T> reversed() {
            return new NullComparator<>(!nullFirst, real == null ? null : real.reversed());
        }
    }
}
