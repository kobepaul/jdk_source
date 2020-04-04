/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Locale;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.internal.HotSpotIntrinsicCandidate;

import static java.lang.String.LATIN1;
import static java.lang.String.UTF16;
import static java.lang.String.checkOffset;

final class StringLatin1 {

    public static char charAt(byte[] value, int index) {
        if (index < 0 || index >= value.length) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return (char)(value[index] & 0xff);
    }

    public static boolean canEncode(int cp) {
        return cp >>> 8 == 0;
    }

    public static int length(byte[] value) {
        return value.length;
    }

    public static int codePointAt(byte[] value, int index, int end) {
        return value[index] & 0xff;
    }

    public static int codePointBefore(byte[] value, int index) {
        return value[index - 1] & 0xff;
    }

    public static int codePointCount(byte[] value, int beginIndex, int endIndex) {
        return endIndex - beginIndex;
    }

    public static char[] toChars(byte[] value) {
        char[] dst = new char[value.length];
        inflate(value, 0, dst, 0, value.length);
        return dst;
    }

    public static byte[] inflate(byte[] value, int off, int len) {
        byte[] ret = java.lang.StringUTF16.newBytesFor(len);
        inflate(value, off, ret, 0, len);
        return ret;
    }

    public static void getChars(byte[] value, int srcBegin, int srcEnd, char dst[], int dstBegin) {
        inflate(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    public static void getBytes(byte[] value, int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        java.lang.System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    @HotSpotIntrinsicCandidate
    public static boolean equals(byte[] value, byte[] other) {
        if (value.length == other.length) {
            for (int i = 0; i < value.length; i++) {
                if (value[i] != other[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @HotSpotIntrinsicCandidate
    public static int compareTo(byte[] value, byte[] other) {
        int len1 = value.length;
        int len2 = other.length;
        return compareTo(value, other, len1, len2);
    }

    public static int compareTo(byte[] value, byte[] other, int len1, int len2) {
        int lim = java.lang.Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            if (value[k] != other[k]) {
                return getChar(value, k) - getChar(other, k);
            }
        }
        return len1 - len2;
    }

    @HotSpotIntrinsicCandidate
    public static int compareToUTF16(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = java.lang.StringUTF16.length(other);
        return compareToUTF16Values(value, other, len1, len2);
    }

    /*
     * Checks the boundary and then compares the byte arrays.
     */
    public static int compareToUTF16(byte[] value, byte[] other, int len1, int len2) {
        checkOffset(len1, length(value));
        checkOffset(len2, java.lang.StringUTF16.length(other));

        return compareToUTF16Values(value, other, len1, len2);
    }

    private static int compareToUTF16Values(byte[] value, byte[] other, int len1, int len2) {
        int lim = java.lang.Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            char c1 = getChar(value, k);
            char c2 = java.lang.StringUTF16.getChar(other, k);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    public static int compareToCI(byte[] value, byte[] other) {
        int len1 = value.length;
        int len2 = other.length;
        int lim = java.lang.Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            if (value[k] != other[k]) {
                char c1 = (char) CharacterDataLatin1.instance.toUpperCase(getChar(value, k));
                char c2 = (char) CharacterDataLatin1.instance.toUpperCase(getChar(other, k));
                if (c1 != c2) {
                    c1 = java.lang.Character.toLowerCase(c1);
                    c2 = java.lang.Character.toLowerCase(c2);
                    if (c1 != c2) {
                        return c1 - c2;
                    }
                }
            }
        }
        return len1 - len2;
    }

    public static int compareToCI_UTF16(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = java.lang.StringUTF16.length(other);
        int lim = java.lang.Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            char c1 = getChar(value, k);
            char c2 = java.lang.StringUTF16.getChar(other, k);
            if (c1 != c2) {
                c1 = java.lang.Character.toUpperCase(c1);
                c2 = java.lang.Character.toUpperCase(c2);
                if (c1 != c2) {
                    c1 = java.lang.Character.toLowerCase(c1);
                    c2 = java.lang.Character.toLowerCase(c2);
                    if (c1 != c2) {
                        return c1 - c2;
                    }
                }
            }
        }
        return len1 - len2;
    }

    public static int hashCode(byte[] value) {
        int h = 0;
        for (byte v : value) {
            h = 31 * h + (v & 0xff);
        }
        return h;
    }

    public static int indexOf(byte[] value, int ch, int fromIndex) {
        if (!canEncode(ch)) {
            return -1;
        }
        int max = value.length;
        if (fromIndex < 0) {
            fromIndex = 0;
        } else if (fromIndex >= max) {
            // Note: fromIndex might be near -1>>>1.
            return -1;
        }
        byte c = (byte)ch;
        for (int i = fromIndex; i < max; i++) {
            if (value[i] == c) {
               return i;
            }
        }
        return -1;
    }

    @HotSpotIntrinsicCandidate
    public static int indexOf(byte[] value, byte[] str) {
        if (str.length == 0) {
            return 0;
        }
        if (value.length == 0) {
            return -1;
        }
        return indexOf(value, value.length, str, str.length, 0);
    }

    @HotSpotIntrinsicCandidate
    public static int indexOf(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex) {
        byte first = str[0];
        int max = (valueCount - strCount);
        for (int i = fromIndex; i <= max; i++) {
            // Look for first character.
            if (value[i] != first) {
                while (++i <= max && value[i] != first);
            }
            // Found first character, now look at the rest of value
            if (i <= max) {
                int j = i + 1;
                int end = j + strCount - 1;
                for (int k = 1; j < end && value[j] == str[k]; j++, k++);
                if (j == end) {
                    // Found whole string.
                    return i;
                }
            }
        }
        return -1;
    }

    public static int lastIndexOf(byte[] src, int srcCount,
                                  byte[] tgt, int tgtCount, int fromIndex) {
        int min = tgtCount - 1;
        int i = min + fromIndex;
        int strLastIndex = tgtCount - 1;
        char strLastChar = (char)(tgt[strLastIndex] & 0xff);

  startSearchForLastChar:
        while (true) {
            while (i >= min && (src[i] & 0xff) != strLastChar) {
                i--;
            }
            if (i < min) {
                return -1;
            }
            int j = i - 1;
            int start = j - strLastIndex;
            int k = strLastIndex - 1;
            while (j > start) {
                if ((src[j--] & 0xff) != (tgt[k--] & 0xff)) {
                    i--;
                    continue startSearchForLastChar;
                }
            }
            return start + 1;
        }
    }

    public static int lastIndexOf(final byte[] value, int ch, int fromIndex) {
        if (!canEncode(ch)) {
            return -1;
        }
        int off  = java.lang.Math.min(fromIndex, value.length - 1);
        for (; off >= 0; off--) {
            if (value[off] == (byte)ch) {
                return off;
            }
        }
        return -1;
    }

    public static java.lang.String replace(byte[] value, char oldChar, char newChar) {
        if (canEncode(oldChar)) {
            int len = value.length;
            int i = -1;
            while (++i < len) {
                if (value[i] == (byte)oldChar) {
                    break;
                }
            }
            if (i < len) {
                if (canEncode(newChar)) {
                    byte buf[] = new byte[len];
                    for (int j = 0; j < i; j++) {    // TBD arraycopy?
                        buf[j] = value[j];
                    }
                    while (i < len) {
                        byte c = value[i];
                        buf[i] = (c == (byte)oldChar) ? (byte)newChar : c;
                        i++;
                    }
                    return new java.lang.String(buf, LATIN1);
                } else {
                    byte[] buf = java.lang.StringUTF16.newBytesFor(len);
                    // inflate from latin1 to UTF16
                    inflate(value, 0, buf, 0, i);
                    while (i < len) {
                        char c = (char)(value[i] & 0xff);
                        java.lang.StringUTF16.putChar(buf, i, (c == oldChar) ? newChar : c);
                        i++;
                    }
                    return new java.lang.String(buf, UTF16);
                }
            }
        }
        return null; // for string to return this;
    }

    // case insensitive
    public static boolean regionMatchesCI(byte[] value, int toffset,
                                          byte[] other, int ooffset, int len) {
        int last = toffset + len;
        while (toffset < last) {
            char c1 = (char)(value[toffset++] & 0xff);
            char c2 = (char)(other[ooffset++] & 0xff);
            if (c1 == c2) {
                continue;
            }
            char u1 = java.lang.Character.toUpperCase(c1);
            char u2 = java.lang.Character.toUpperCase(c2);
            if (u1 == u2) {
                continue;
            }
            if (java.lang.Character.toLowerCase(u1) == java.lang.Character.toLowerCase(u2)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public static boolean regionMatchesCI_UTF16(byte[] value, int toffset,
                                                byte[] other, int ooffset, int len) {
        int last = toffset + len;
        while (toffset < last) {
            char c1 = (char)(value[toffset++] & 0xff);
            char c2 = java.lang.StringUTF16.getChar(other, ooffset++);
            if (c1 == c2) {
                continue;
            }
            char u1 = java.lang.Character.toUpperCase(c1);
            char u2 = java.lang.Character.toUpperCase(c2);
            if (u1 == u2) {
                continue;
            }
            if (java.lang.Character.toLowerCase(u1) == java.lang.Character.toLowerCase(u2)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public static java.lang.String toLowerCase(java.lang.String str, byte[] value, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        int first;
        final int len = value.length;
        // Now check if there are any characters that need to be changed, or are surrogate
        for (first = 0 ; first < len; first++) {
            int cp = value[first] & 0xff;
            if (cp != java.lang.Character.toLowerCase(cp)) {  // no need to check Character.ERROR
                break;
            }
        }
        if (first == len)
            return str;
        java.lang.String lang = locale.getLanguage();
        if (lang == "tr" || lang == "az" || lang == "lt") {
            return toLowerCaseEx(str, value, first, locale, true);
        }
        byte[] result = new byte[len];
        java.lang.System.arraycopy(value, 0, result, 0, first);  // Just copy the first few
                                                       // lowerCase characters.
        for (int i = first; i < len; i++) {
            int cp = value[i] & 0xff;
            cp = java.lang.Character.toLowerCase(cp);
            if (!canEncode(cp)) {                      // not a latin1 character
                return toLowerCaseEx(str, value, first, locale, false);
            }
            result[i] = (byte)cp;
        }
        return new java.lang.String(result, LATIN1);
    }

    private static java.lang.String toLowerCaseEx(java.lang.String str, byte[] value,
                                                  int first, Locale locale, boolean localeDependent)
    {
        byte[] result = java.lang.StringUTF16.newBytesFor(value.length);
        int resultOffset = 0;
        for (int i = 0; i < first; i++) {
            java.lang.StringUTF16.putChar(result, resultOffset++, value[i] & 0xff);
        }
        for (int i = first; i < value.length; i++) {
            int srcChar = value[i] & 0xff;
            int lowerChar;
            char[] lowerCharArray;
            if (localeDependent) {
                lowerChar = ConditionalSpecialCasing.toLowerCaseEx(str, i, locale);
            } else {
                lowerChar = java.lang.Character.toLowerCase(srcChar);
            }
            if (java.lang.Character.isBmpCodePoint(lowerChar)) {    // Character.ERROR is not a bmp
                java.lang.StringUTF16.putChar(result, resultOffset++, lowerChar);
            } else {
                if (lowerChar == java.lang.Character.ERROR) {
                    lowerCharArray = ConditionalSpecialCasing.toLowerCaseCharArray(str, i, locale);
                } else {
                    lowerCharArray = java.lang.Character.toChars(lowerChar);
                }
                /* Grow result if needed */
                int mapLen = lowerCharArray.length;
                if (mapLen > 1) {
                    byte[] result2 = java.lang.StringUTF16.newBytesFor((result.length >> 1) + mapLen - 1);
                    java.lang.System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                for (int x = 0; x < mapLen; ++x) {
                    java.lang.StringUTF16.putChar(result, resultOffset++, lowerCharArray[x]);
                }
            }
        }
        return java.lang.StringUTF16.newString(result, 0, resultOffset);
    }

    public static java.lang.String toUpperCase(java.lang.String str, byte[] value, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        int first;
        final int len = value.length;

        // Now check if there are any characters that need to be changed, or are surrogate
        for (first = 0 ; first < len; first++ ) {
            int cp = value[first] & 0xff;
            if (cp != java.lang.Character.toUpperCaseEx(cp)) {   // no need to check Character.ERROR
                break;
            }
        }
        if (first == len) {
            return str;
        }
        java.lang.String lang = locale.getLanguage();
        if (lang == "tr" || lang == "az" || lang == "lt") {
            return toUpperCaseEx(str, value, first, locale, true);
        }
        byte[] result = new byte[len];
        java.lang.System.arraycopy(value, 0, result, 0, first);  // Just copy the first few
                                                       // upperCase characters.
        for (int i = first; i < len; i++) {
            int cp = value[i] & 0xff;
            cp = java.lang.Character.toUpperCaseEx(cp);
            if (!canEncode(cp)) {                      // not a latin1 character
                return toUpperCaseEx(str, value, first, locale, false);
            }
            result[i] = (byte)cp;
        }
        return new java.lang.String(result, LATIN1);
    }

    private static java.lang.String toUpperCaseEx(java.lang.String str, byte[] value,
                                                  int first, Locale locale, boolean localeDependent)
    {
        byte[] result = java.lang.StringUTF16.newBytesFor(value.length);
        int resultOffset = 0;
        for (int i = 0; i < first; i++) {
            java.lang.StringUTF16.putChar(result, resultOffset++, value[i] & 0xff);
        }
        for (int i = first; i < value.length; i++) {
            int srcChar = value[i] & 0xff;
            int upperChar;
            char[] upperCharArray;
            if (localeDependent) {
                upperChar = ConditionalSpecialCasing.toUpperCaseEx(str, i, locale);
            } else {
                upperChar = java.lang.Character.toUpperCaseEx(srcChar);
            }
            if (java.lang.Character.isBmpCodePoint(upperChar)) {
                java.lang.StringUTF16.putChar(result, resultOffset++, upperChar);
            } else {
                if (upperChar == java.lang.Character.ERROR) {
                    if (localeDependent) {
                        upperCharArray =
                            ConditionalSpecialCasing.toUpperCaseCharArray(str, i, locale);
                    } else {
                        upperCharArray = java.lang.Character.toUpperCaseCharArray(srcChar);
                    }
                } else {
                    upperCharArray = java.lang.Character.toChars(upperChar);
                }
                /* Grow result if needed */
                int mapLen = upperCharArray.length;
                if (mapLen > 1) {
                    byte[] result2 = java.lang.StringUTF16.newBytesFor((result.length >> 1) + mapLen - 1);
                    java.lang.System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                for (int x = 0; x < mapLen; ++x) {
                    java.lang.StringUTF16.putChar(result, resultOffset++, upperCharArray[x]);
                }
            }
        }
        return java.lang.StringUTF16.newString(result, 0, resultOffset);
    }

    public static java.lang.String trim(byte[] value) {
        int len = value.length;
        int st = 0;
        while ((st < len) && ((value[st] & 0xff) <= ' ')) {
            st++;
        }
        while ((st < len) && ((value[len - 1] & 0xff) <= ' ')) {
            len--;
        }
        return ((st > 0) || (len < value.length)) ?
            newString(value, st, len - st) : null;
    }

    public static int indexOfNonWhitespace(byte[] value) {
        int length = value.length;
        int left = 0;
        while (left < length) {
            char ch = (char)(value[left] & 0xff);
            if (ch != ' ' && ch != '\t' && !java.lang.Character.isWhitespace(ch)) {
                break;
            }
            left++;
        }
        return left;
    }

    public static int lastIndexOfNonWhitespace(byte[] value) {
        int length = value.length;
        int right = length;
        while (0 < right) {
            char ch = (char)(value[right - 1] & 0xff);
            if (ch != ' ' && ch != '\t' && !java.lang.Character.isWhitespace(ch)) {
                break;
            }
            right--;
        }
        return right;
    }

    public static java.lang.String strip(byte[] value) {
        int left = indexOfNonWhitespace(value);
        if (left == value.length) {
            return "";
        }
        int right = lastIndexOfNonWhitespace(value);
        return ((left > 0) || (right < value.length)) ? newString(value, left, right - left) : null;
    }

    public static java.lang.String stripLeading(byte[] value) {
        int left = indexOfNonWhitespace(value);
        if (left == value.length) {
            return "";
        }
        return (left != 0) ? newString(value, left, value.length - left) : null;
    }

    public static java.lang.String stripTrailing(byte[] value) {
        int right = lastIndexOfNonWhitespace(value);
        if (right == 0) {
            return "";
        }
        return (right != value.length) ? newString(value, 0, right) : null;
    }

    private final static class LinesSpliterator implements Spliterator<java.lang.String> {
        private byte[] value;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index

        LinesSpliterator(byte[] value) {
            this(value, 0, value.length);
        }

        LinesSpliterator(byte[] value, int start, int length) {
            this.value = value;
            this.index = start;
            this.fence = start + length;
        }

        private int indexOfLineSeparator(int start) {
            for (int current = start; current < fence; current++) {
                byte ch = value[current];
                if (ch == '\n' || ch == '\r') {
                    return current;
                }
            }
            return fence;
        }

        private int skipLineSeparator(int start) {
            if (start < fence) {
                if (value[start] == '\r') {
                    int next = start + 1;
                    if (next < fence && value[next] == '\n') {
                        return next + 1;
                    }
                }
                return start + 1;
            }
            return fence;
        }

        private java.lang.String next() {
            int start = index;
            int end = indexOfLineSeparator(start);
            index = skipLineSeparator(end);
            return newString(value, start, end - start);
        }

        @java.lang.Override
        public boolean tryAdvance(Consumer<? super java.lang.String> action) {
            if (action == null) {
                throw new NullPointerException("tryAdvance action missing");
            }
            if (index != fence) {
                action.accept(next());
                return true;
            }
            return false;
        }

        @java.lang.Override
        public void forEachRemaining(Consumer<? super java.lang.String> action) {
            if (action == null) {
                throw new NullPointerException("forEachRemaining action missing");
            }
            while (index != fence) {
                action.accept(next());
            }
        }

        @java.lang.Override
        public Spliterator<java.lang.String> trySplit() {
            int half = (fence + index) >>> 1;
            int mid = skipLineSeparator(indexOfLineSeparator(half));
            if (mid < fence) {
                int start = index;
                index = mid;
                return new LinesSpliterator(value, start, mid - start);
            }
            return null;
        }

        @java.lang.Override
        public long estimateSize() {
            return fence - index + 1;
        }

        @java.lang.Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;
        }
    }

    static Stream<java.lang.String> lines(byte[] value) {
        return StreamSupport.stream(new LinesSpliterator(value), false);
    }

    public static void putChar(byte[] val, int index, int c) {
        //assert (canEncode(c));
        val[index] = (byte)(c);
    }

    public static char getChar(byte[] val, int index) {
        return (char)(val[index] & 0xff);
    }

    public static byte[] toBytes(int[] val, int off, int len) {
        byte[] ret = new byte[len];
        for (int i = 0; i < len; i++) {
            int cp = val[off++];
            if (!canEncode(cp)) {
                return null;
            }
            ret[i] = (byte)cp;
        }
        return ret;
    }

    public static byte[] toBytes(char c) {
        return new byte[] { (byte)c };
    }

    public static java.lang.String newString(byte[] val, int index, int len) {
        return new java.lang.String(Arrays.copyOfRange(val, index, index + len),
                          LATIN1);
    }

    public static void fillNull(byte[] val, int index, int end) {
        Arrays.fill(val, index, end, (byte)0);
    }

    // inflatedCopy byte[] -> char[]
    @HotSpotIntrinsicCandidate
    public static void inflate(byte[] src, int srcOff, char[] dst, int dstOff, int len) {
        for (int i = 0; i < len; i++) {
            dst[dstOff++] = (char)(src[srcOff++] & 0xff);
        }
    }

    // inflatedCopy byte[] -> byte[]
    @HotSpotIntrinsicCandidate
    public static void inflate(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        java.lang.StringUTF16.inflate(src, srcOff, dst, dstOff, len);
    }

    static class CharsSpliterator implements Spliterator.OfInt {
        private final byte[] array;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index
        private final int cs;

        CharsSpliterator(byte[] array, int acs) {
            this(array, 0, array.length, acs);
        }

        CharsSpliterator(byte[] array, int origin, int fence, int acs) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.cs = acs | Spliterator.ORDERED | Spliterator.SIZED
                      | Spliterator.SUBSIZED;
        }

        @java.lang.Override
        public OfInt trySplit() {
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid)
                   ? null
                   : new CharsSpliterator(array, lo, index = mid, cs);
        }

        @java.lang.Override
        public void forEachRemaining(IntConsumer action) {
            byte[] a; int i, hi; // hoist accesses and checks from loop
            if (action == null)
                throw new NullPointerException();
            if ((a = array).length >= (hi = fence) &&
                (i = index) >= 0 && i < (index = hi)) {
                do { action.accept(a[i] & 0xff); } while (++i < hi);
            }
        }

        @java.lang.Override
        public boolean tryAdvance(IntConsumer action) {
            if (action == null)
                throw new NullPointerException();
            if (index >= 0 && index < fence) {
                action.accept(array[index++] & 0xff);
                return true;
            }
            return false;
        }

        @java.lang.Override
        public long estimateSize() { return (long)(fence - index); }

        @java.lang.Override
        public int characteristics() {
            return cs;
        }
    }
}
