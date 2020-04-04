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

package java.util.regex;

import java.util.HashMap;
import java.util.regex.Pattern.CharPredicate;
import static java.util.regex.ASCII.*;

/**
 * A utility class to print out the pattern node tree.
 */

class PrintPattern {

    private static HashMap<java.util.regex.Pattern.Node, Integer> ids = new HashMap<>();

    private static void print(java.util.regex.Pattern.Node node, String text, int depth) {
        if (!ids.containsKey(node))
            ids.put(node, ids.size());
        print("%6d:%" + (depth==0? "": depth<<1) + "s<%s>", ids.get(node), "", text);
        if (ids.containsKey(node.next))
            print(" (=>%d)", ids.get(node.next));
        print("%n");
    }

    private static void print(String s, int depth) {
        print("       %" + (depth==0?"":depth<<1) + "s<%s>%n", "", s);
    }

    private static void print(String fmt, Object ... args) {
        System.err.printf(fmt, args);
    }

    private static String toStringCPS(int[] cps) {
        StringBuilder sb = new StringBuilder(cps.length);
        for (int cp : cps)
            sb.append(toStringCP(cp));
        return sb.toString();
    }

    private static String toStringCP(int cp) {
        return (isPrint(cp) ? "" + (char)cp
                            : "\\u" + Integer.toString(cp, 16));
    }

    private static String toStringRange(int min, int max) {
       if (max == java.util.regex.Pattern.MAX_REPS) {
           if (min == 0)
               return " * ";
           else if (min == 1)
               return " + ";
           return "{" + min + ", max}";
       }
       return "{" + min + ", " +  max + "}";
    }

    private static String toStringCtype(int type) {
        switch(type) {
        case UPPER:  return "ASCII.UPPER";
        case LOWER:  return "ASCII.LOWER";
        case DIGIT:  return "ASCII.DIGIT";
        case SPACE:  return "ASCII.SPACE";
        case PUNCT:  return "ASCII.PUNCT";
        case CNTRL:  return "ASCII.CNTRL";
        case BLANK:  return "ASCII.BLANK";
        case UNDER:  return "ASCII.UNDER";
        case ASCII:  return "ASCII.ASCII";
        case ALPHA:  return "ASCII.ALPHA";
        case ALNUM:  return "ASCII.ALNUM";
        case GRAPH:  return "ASCII.GRAPH";
        case WORD:   return "ASCII.WORD";
        case XDIGIT: return "ASCII.XDIGIT";
        default: return "ASCII ?";
        }
    }

    private static String toString(java.util.regex.Pattern.Node node) {
        String name = node.getClass().getName();
        return name.substring(name.lastIndexOf('$') + 1);
    }

    static HashMap<CharPredicate, String> pmap;
    static {
        pmap = new HashMap<>();
        pmap.put(java.util.regex.Pattern.ALL(), "All");
        pmap.put(java.util.regex.Pattern.DOT(), "Dot");
        pmap.put(java.util.regex.Pattern.UNIXDOT(), "UnixDot");
        pmap.put(java.util.regex.Pattern.VertWS(), "VertWS");
        pmap.put(java.util.regex.Pattern.HorizWS(), "HorizWS");

        pmap.put(CharPredicates.ASCII_DIGIT(), "ASCII.DIGIT");
        pmap.put(CharPredicates.ASCII_WORD(),  "ASCII.WORD");
        pmap.put(CharPredicates.ASCII_SPACE(), "ASCII.SPACE");
    }

    static void walk(java.util.regex.Pattern.Node node, int depth) {
        depth++;
        while(node != null) {
            String name = toString(node);
            String str;
            if (node instanceof java.util.regex.Pattern.Prolog) {
                print(node, name, depth);
                // print the loop here
                java.util.regex.Pattern.Loop loop = ((java.util.regex.Pattern.Prolog)node).loop;
                name = toString(loop);
                str = name + " " + toStringRange(loop.cmin, loop.cmax);
                print(loop, str, depth);
                walk(loop.body, depth);
                print("/" + name, depth);
                node = loop;
            } else if (node instanceof java.util.regex.Pattern.Loop) {
                return;  // stop here, body.next -> loop
            } else if (node instanceof java.util.regex.Pattern.Curly) {
                java.util.regex.Pattern.Curly c = (java.util.regex.Pattern.Curly)node;
                str = "Curly " + c.type + " " + toStringRange(c.cmin, c.cmax);
                print(node, str, depth);
                walk(c.atom, depth);
                print("/Curly", depth);
            } else if (node instanceof java.util.regex.Pattern.GroupCurly) {
                java.util.regex.Pattern.GroupCurly gc = (java.util.regex.Pattern.GroupCurly)node;
                str = "GroupCurly " + gc.groupIndex / 2 +
                      ", " + gc.type + " " + toStringRange(gc.cmin, gc.cmax);
                print(node, str, depth);
                walk(gc.atom, depth);
                print("/GroupCurly", depth);
            } else if (node instanceof java.util.regex.Pattern.GroupHead) {
                java.util.regex.Pattern.GroupHead head = (java.util.regex.Pattern.GroupHead)node;
                java.util.regex.Pattern.GroupTail tail = head.tail;
                print(head, "Group.head " + (tail.groupIndex / 2), depth);
                walk(head.next, depth);
                print(tail, "/Group.tail " + (tail.groupIndex / 2), depth);
                node = tail;
            } else if (node instanceof java.util.regex.Pattern.GroupTail) {
                return;  // stopper
            } else if (node instanceof java.util.regex.Pattern.Ques) {
                print(node, "Ques " + ((java.util.regex.Pattern.Ques)node).type, depth);
                walk(((java.util.regex.Pattern.Ques)node).atom, depth);
                print("/Ques", depth);
            } else if (node instanceof java.util.regex.Pattern.Branch) {
                java.util.regex.Pattern.Branch b = (java.util.regex.Pattern.Branch)node;
                print(b, name, depth);
                int i = 0;
                while (true) {
                    if (b.atoms[i] != null) {
                        walk(b.atoms[i], depth);
                    } else {
                        print("  (accepted)", depth);
                    }
                    if (++i == b.size)
                        break;
                    print("-branch.separator-", depth);
                }
                node = b.conn;
                print(node, "/Branch", depth);
            } else if (node instanceof java.util.regex.Pattern.BranchConn) {
                return;
            } else if (node instanceof java.util.regex.Pattern.CharProperty) {
                str = pmap.get(((java.util.regex.Pattern.CharProperty)node).predicate);
                if (str == null)
                    str = toString(node);
                else
                    str = "Single \"" + str + "\"";
                print(node, str, depth);
            } else if (node instanceof java.util.regex.Pattern.SliceNode) {
                str = name + "  \"" +
                      toStringCPS(((java.util.regex.Pattern.SliceNode)node).buffer) + "\"";
                print(node, str, depth);
            } else if (node instanceof java.util.regex.Pattern.CharPropertyGreedy) {
                java.util.regex.Pattern.CharPropertyGreedy gcp = (java.util.regex.Pattern.CharPropertyGreedy)node;
                String pstr = pmap.get(gcp.predicate);
                if (pstr == null)
                    pstr = gcp.predicate.toString();
                else
                    pstr = "Single \"" + pstr + "\"";
                str = name + " " + pstr + ((gcp.cmin == 0) ? "*" : "+");
                print(node, str, depth);
            } else if (node instanceof java.util.regex.Pattern.BackRef) {
                str = "GroupBackRef " + ((java.util.regex.Pattern.BackRef)node).groupIndex / 2;
                print(node, str, depth);
            } else if (node instanceof java.util.regex.Pattern.LastNode) {
                print(node, "END", depth);
            } else if (node == java.util.regex.Pattern.accept) {
                return;
            } else {
                print(node, name, depth);
            }
            node = node.next;
        }
    }

    public static void main(String[] args) {
        java.util.regex.Pattern p = Pattern.compile(args[0]);
        System.out.println("   Pattern: " + p);
        walk(p.root, 0);
    }
}
