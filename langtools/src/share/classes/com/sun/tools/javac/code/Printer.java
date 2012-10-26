/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.code;

import java.util.Locale;

import com.sun.tools.javac.api.Messages;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.comp.DeferredAttr.DeferredType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

import static com.sun.tools.javac.code.TypeTags.*;
import static com.sun.tools.javac.code.BoundKind.*;
import static com.sun.tools.javac.code.Flags.*;

/**
 * A combined type/symbol visitor for generating non-trivial localized string
 * representation of types and symbols.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public abstract class Printer implements Type.Visitor<String, Locale>, Symbol.Visitor<String, Locale> {

    List<Type> seenCaptured = List.nil();
    static final int PRIME = 997;  // largest prime less than 1000

    protected Printer() { }

    /**
     * This method should be overriden in order to provide proper i18n support.
     *
     * @param locale the locale in which the string is to be rendered
     * @param key the key corresponding to the message to be displayed
     * @param args a list of optional arguments
     * @return localized string representation
     */
    protected abstract String localize(Locale locale, String key, Object... args);

    /**
     * Maps a captured type into an unique identifier.
     *
     * @param t the captured type for which an id is to be retrieved
     * @param locale locale settings
     * @return unique id representing this captured type
     */
    protected abstract String capturedVarId(CapturedType t, Locale locale);

    /**
     * Create a printer with default i18n support provided by Messages. By default,
     * captured types ids are generated using hashcode.
     *
     * @param messages Messages class to be used for i18n
     * @return printer visitor instance
     */
    public static Printer createStandardPrinter(final Messages messages) {
        return new Printer() {
            @Override
            protected String localize(Locale locale, String key, Object... args) {
                return messages.getLocalizedString(locale, key, args);
            }

            @Override
            protected String capturedVarId(CapturedType t, Locale locale) {
                return (t.hashCode() & 0xFFFFFFFFL) % PRIME + "";
        }};
    }

    /**
     * Get a localized string representation for all the types in the input list.
     *
     * @param ts types to be displayed
     * @param locale the locale in which the string is to be rendered
     * @return localized string representation
     */
    public String visitTypes(List<Type> ts, Locale locale) {
        ListBuffer<String> sbuf = ListBuffer.lb();
        for (Type t : ts) {
            sbuf.append(visit(t, locale));
        }
        return sbuf.toList().toString();
    }

    /**
     * * Get a localized string representation for all the symbols in the input list.
     *
     * @param ts symbols to be displayed
     * @param locale the locale in which the string is to be rendered
     * @return localized string representation
     */
    public String visitSymbols(List<Symbol> ts, Locale locale) {
        ListBuffer<String> sbuf = ListBuffer.lb();
        for (Symbol t : ts) {
            sbuf.append(visit(t, locale));
        }
        return sbuf.toList().toString();
    }

    /**
     * Get a localized string represenation for a given type.
     *
     * @param ts type to be displayed
     * @param locale the locale in which the string is to be rendered
     * @return localized string representation
     */
    public String visit(Type t, Locale locale) {
        return t.accept(this, locale);
    }

    /**
     * Get a localized string represenation for a given symbol.
     *
     * @param ts symbol to be displayed
     * @param locale the locale in which the string is to be rendered
     * @return localized string representation
     */
    public String visit(Symbol s, Locale locale) {
        return s.accept(this, locale);
    }

    @Override
    public String visitCapturedType(CapturedType t, Locale locale) {
        if (seenCaptured.contains(t))
            return localize(locale, "compiler.misc.type.captureof.1",
                capturedVarId(t, locale));
        else {
            try {
                seenCaptured = seenCaptured.prepend(t);
                return localize(locale, "compiler.misc.type.captureof",
                    capturedVarId(t, locale),
                    visit(t.wildcard, locale));
            }
            finally {
                seenCaptured = seenCaptured.tail;
            }
        }
    }

    @Override
    public String visitForAll(ForAll t, Locale locale) {
        return "<" + visitTypes(t.tvars, locale) + ">" + visit(t.qtype, locale);
    }

    @Override
    public String visitUndetVar(UndetVar t, Locale locale) {
        if (t.inst != null) {
            return visit(t.inst, locale);
        } else {
            return visit(t.qtype, locale) + "?";
        }
    }

    @Override
    public String visitArrayType(ArrayType t, Locale locale) {
        return visit(t.elemtype, locale) + "[]";
    }

    @Override
    public String visitClassType(ClassType t, Locale locale) {
        StringBuffer buf = new StringBuffer();
        if (t.getEnclosingType().tag == CLASS && t.tsym.owner.kind == Kinds.TYP) {
            buf.append(visit(t.getEnclosingType(), locale));
            buf.append(".");
            buf.append(className(t, false, locale));
        } else {
            buf.append(className(t, true, locale));
        }
        if (t.getTypeArguments().nonEmpty()) {
            buf.append('<');
            buf.append(visitTypes(t.getTypeArguments(), locale));
            buf.append(">");
        }
        return buf.toString();
    }

    @Override
    public String visitMethodType(MethodType t, Locale locale) {
        return "(" + printMethodArgs(t.argtypes, false, locale) + ")" + visit(t.restype, locale);
    }

    @Override
    public String visitPackageType(PackageType t, Locale locale) {
        return t.tsym.getQualifiedName().toString();
    }

    @Override
    public String visitWildcardType(WildcardType t, Locale locale) {
        StringBuffer s = new StringBuffer();
        s.append(t.kind);
        if (t.kind != UNBOUND) {
            s.append(visit(t.type, locale));
        }
        return s.toString();
    }

    @Override
    public String visitErrorType(ErrorType t, Locale locale) {
        return visitType(t, locale);
    }

    @Override
    public String visitTypeVar(TypeVar t, Locale locale) {
        return visitType(t, locale);
    }

    public String visitType(Type t, Locale locale) {
        String s = (t.tsym == null || t.tsym.name == null)
                ? localize(locale, "compiler.misc.type.none")
                : t.tsym.name.toString();
        return s;
    }

    /**
     * Converts a class name into a (possibly localized) string. Anonymous
     * inner classes gets converted into a localized string.
     *
     * @param t the type of the class whose name is to be rendered
     * @param longform if set, the class' fullname is displayed - if unset the
     * short name is chosen (w/o package)
     * @param locale the locale in which the string is to be rendered
     * @return localized string representation
     */
    protected String className(ClassType t, boolean longform, Locale locale) {
        Symbol sym = t.tsym;
        if (sym.name.length() == 0 && (sym.flags() & COMPOUND) != 0) {
            StringBuffer s = new StringBuffer(visit(t.supertype_field, locale));
            for (List<Type> is = t.interfaces_field; is.nonEmpty(); is = is.tail) {
                s.append("&");
                s.append(visit(is.head, locale));
            }
            return s.toString();
        } else if (sym.name.length() == 0) {
            String s;
            ClassType norm = (ClassType) t.tsym.type;
            if (norm == null) {
                s = localize(locale, "compiler.misc.anonymous.class", (Object) null);
            } else if (norm.interfaces_field != null && norm.interfaces_field.nonEmpty()) {
                s = localize(locale, "compiler.misc.anonymous.class",
                        visit(norm.interfaces_field.head, locale));
            } else {
                s = localize(locale, "compiler.misc.anonymous.class",
                        visit(norm.supertype_field, locale));
            }
            return s;
        } else if (longform) {
            return sym.getQualifiedName().toString();
        } else {
            return sym.name.toString();
        }
    }

    /**
     * Converts a set of method argument types into their corresponding
     * localized string representation.
     *
     * @param args arguments to be rendered
     * @param varArgs if true, the last method argument is regarded as a vararg
     * @param locale the locale in which the string is to be rendered
     * @return localized string representation
     */
    protected String printMethodArgs(List<Type> args, boolean varArgs, Locale locale) {
        if (!varArgs) {
            return visitTypes(args, locale);
        } else {
            StringBuffer buf = new StringBuffer();
            while (args.tail.nonEmpty()) {
                buf.append(visit(args.head, locale));
                args = args.tail;
                buf.append(',');
            }
            if (args.head.tag == ARRAY) {
                buf.append(visit(((ArrayType) args.head).elemtype, locale));
                buf.append("...");
            } else {
                buf.append(visit(args.head, locale));
            }
            return buf.toString();
        }
    }

    @Override
    public String visitClassSymbol(ClassSymbol sym, Locale locale) {
        return sym.name.isEmpty()
                ? localize(locale, "compiler.misc.anonymous.class", sym.flatname)
                : sym.fullname.toString();
    }

    @Override
    public String visitMethodSymbol(MethodSymbol s, Locale locale) {
        if (s.isStaticOrInstanceInit()) {
            return s.owner.name.toString();
        } else {
            String ms = (s.name == s.name.table.names.init)
                    ? s.owner.name.toString()
                    : s.name.toString();
            if (s.type != null) {
                if (s.type.tag == FORALL) {
                    ms = "<" + visitTypes(s.type.getTypeArguments(), locale) + ">" + ms;
                }
                ms += "(" + printMethodArgs(
                        s.type.getParameterTypes(),
                        (s.flags() & VARARGS) != 0,
                        locale) + ")";
            }
            return ms;
        }
    }

    @Override
    public String visitOperatorSymbol(OperatorSymbol s, Locale locale) {
        return visitMethodSymbol(s, locale);
    }

    @Override
    public String visitPackageSymbol(PackageSymbol s, Locale locale) {
        return s.isUnnamed()
                ? localize(locale, "compiler.misc.unnamed.package")
                : s.fullname.toString();
    }

    @Override
    public String visitTypeSymbol(TypeSymbol s, Locale locale) {
        return visitSymbol(s, locale);
    }

    @Override
    public String visitVarSymbol(VarSymbol s, Locale locale) {
        return visitSymbol(s, locale);
    }

    @Override
    public String visitSymbol(Symbol s, Locale locale) {
        return s.name.toString();
    }
}
