/*
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.tools.javac.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Printer;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.Types;

import static com.sun.tools.javac.code.TypeTags.*;
import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.util.LayoutCharacters.*;
import static com.sun.tools.javac.util.RichDiagnosticFormatter.RichConfiguration.*;

/**
 * A rich diagnostic formatter is a formatter that provides better integration
 * with javac's type system. A diagostic is first preprocessed in order to keep
 * track of each types/symbols in it; after these informations are collected,
 * the diagnostic is rendered using a standard formatter, whose type/symbol printer
 * has been replaced by a more refined version provided by this rich formatter.
 * The rich formatter currently enables three different features: (i) simple class
 * names - that is class names are displayed used a non qualified name (thus
 * omitting package info) whenever possible - (ii) where clause list - a list of
 * additional subdiagnostics that provide specific info about type-variables,
 * captured types, intersection types that occur in the diagnostic that is to be
 * formatted and (iii) type-variable disambiguation - when the diagnostic refers
 * to two different type-variables with the same name, their representation is
 * disambiguated by appending an index to the type variable name.
 */
public class RichDiagnosticFormatter extends
        ForwardingDiagnosticFormatter<JCDiagnostic, AbstractDiagnosticFormatter> {

    final Symtab syms;
    final Types types;
    final JCDiagnostic.Factory diags;
    final JavacMessages messages;

    /* name simplifier used by this formatter */
    ClassNameSimplifier nameSimplifier;

    /* map for keeping track of a where clause associated to a given type */
    Map<WhereClauseKind, Map<Type, JCDiagnostic>> whereClauses;

    /** Get the DiagnosticFormatter instance for this context. */
    public static RichDiagnosticFormatter instance(Context context) {
        RichDiagnosticFormatter instance = context.get(RichDiagnosticFormatter.class);
        if (instance == null)
            instance = new RichDiagnosticFormatter(context);
        return instance;
    }

    protected RichDiagnosticFormatter(Context context) {
        super((AbstractDiagnosticFormatter)Log.instance(context).getDiagnosticFormatter());
        this.formatter.setPrinter(printer);
        this.syms = Symtab.instance(context);
        this.diags = JCDiagnostic.Factory.instance(context);
        this.types = Types.instance(context);
        this.messages = JavacMessages.instance(context);
        whereClauses = new LinkedHashMap<WhereClauseKind, Map<Type, JCDiagnostic>>();
        configuration = new RichConfiguration(Options.instance(context), formatter);
        for (WhereClauseKind kind : WhereClauseKind.values())
            whereClauses.put(kind, new LinkedHashMap<Type, JCDiagnostic>());
    }

    @Override
    public String format(JCDiagnostic diag, Locale l) {
        StringBuilder sb = new StringBuilder();
        nameSimplifier = new ClassNameSimplifier();
        for (WhereClauseKind kind : WhereClauseKind.values())
            whereClauses.get(kind).clear();
        preprocessDiagnostic(diag);
        sb.append(formatter.format(diag, l));
        if (getConfiguration().isEnabled(RichFormatterFeature.WHERE_CLAUSES)) {
            List<JCDiagnostic> clauses = getWhereClauses();
            String indent = formatter.isRaw() ? "" :
                formatter.indentString(DetailsInc);
            for (JCDiagnostic d : clauses) {
                String whereClause = formatter.format(d, l);
                if (whereClause.length() > 0) {
                    sb.append('\n' + indent + whereClause);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Preprocess a given diagnostic by looking both into its arguments and into
     * its subdiagnostics (if any). This preprocessing is responsible for
     * generating info corresponding to features like where clauses, name
     * simplification, etc.
     *
     * @param diag the diagnostic to be preprocessed
     */
    protected void preprocessDiagnostic(JCDiagnostic diag) {
        for (Object o : diag.getArgs()) {
            if (o != null) {
                preprocessArgument(o);
            }
        }
        if (diag.isMultiline()) {
            for (JCDiagnostic d : diag.getSubdiagnostics())
                preprocessDiagnostic(d);
        }
    }

    /**
     * Preprocess a diagnostic argument. A type/symbol argument is
     * preprocessed by specialized type/symbol preprocessors.
     *
     * @param arg the argument to be translated
     */
    protected void preprocessArgument(Object arg) {
        if (arg instanceof Type) {
            preprocessType((Type)arg);
        }
        else if (arg instanceof Symbol) {
            preprocessSymbol((Symbol)arg);
        }
        else if (arg instanceof JCDiagnostic) {
            preprocessDiagnostic((JCDiagnostic)arg);
        }
        else if (arg instanceof Iterable<?>) {
            for (Object o : (Iterable<?>)arg) {
                preprocessArgument(o);
            }
        }
    }

    /**
     * Build a list of multiline diagnostics containing detailed info about
     * type-variables, captured types, and intersection types
     *
     * @return where clause list
     */
    protected List<JCDiagnostic> getWhereClauses() {
        List<JCDiagnostic> clauses = List.nil();
        for (WhereClauseKind kind : WhereClauseKind.values()) {
            List<JCDiagnostic> lines = List.nil();
            for (Map.Entry<Type, JCDiagnostic> entry : whereClauses.get(kind).entrySet()) {
                lines = lines.prepend(entry.getValue());
            }
            if (!lines.isEmpty()) {
                String key = kind.key();
                if (lines.size() > 1)
                    key += ".1";
                JCDiagnostic d = diags.fragment(key, whereClauses.get(kind).keySet());
                d = new JCDiagnostic.MultilineDiagnostic(d, lines.reverse());
                clauses = clauses.prepend(d);
            }
        }
        return clauses.reverse();
    }
    //where
    /**
     * This enum defines all posssible kinds of where clauses that can be
     * attached by a rich diagnostic formatter to a given diagnostic
     */
    enum WhereClauseKind {

        /** where clause regarding a type variable */
        TYPEVAR("where.description.typevar"),
        /** where clause regarding a captured type */
        CAPTURED("where.description.captured"),
        /** where clause regarding an intersection type */
        INTERSECTION("where.description.intersection");

        /** resource key for this where clause kind */
        private String key;

        WhereClauseKind(String key) {
            this.key = key;
        }

        String key() {
            return key;
        }
    }

    // <editor-fold defaultstate="collapsed" desc="name simplifier">
    /**
     * A name simplifier keeps track of class names usages in order to determine
     * whether a class name can be compacted or not. Short names are not used
     * if a conflict is detected, e.g. when two classes with the same simple
     * name belong to different packages - in this case the formatter reverts
     * to fullnames as compact names might lead to a confusing diagnostic.
     */
    class ClassNameSimplifier {

        /* table for keeping track of all short name usages */
        Map<Name, List<Symbol>> nameClashes = new HashMap<Name, List<Symbol>>();

        /**
         * Add a name usage to the simplifier's internal cache
         */
        protected void addUsage(Symbol sym) {
            Name n = sym.getSimpleName();
            List<Symbol> conflicts = nameClashes.get(n);
            if (conflicts == null) {
                conflicts = List.nil();
            }
            if (!conflicts.contains(sym))
                nameClashes.put(n, conflicts.append(sym));
        }

        public String simplify(Symbol s) {
            String name = s.getQualifiedName().toString();
            if (!s.type.isCompound()) {
                List<Symbol> conflicts = nameClashes.get(s.getSimpleName());
                if (conflicts == null ||
                    (conflicts.size() == 1 &&
                    conflicts.contains(s))) {
                    List<Name> l = List.nil();
                    Symbol s2 = s;
                    while (s2.type.getEnclosingType().tag == CLASS
                            && s2.owner.kind == Kinds.TYP) {
                        l = l.prepend(s2.getSimpleName());
                        s2 = s2.owner;
                    }
                    l = l.prepend(s2.getSimpleName());
                    StringBuilder buf = new StringBuilder();
                    String sep = "";
                    for (Name n2 : l) {
                        buf.append(sep);
                        buf.append(n2);
                        sep = ".";
                    }
                    name = buf.toString();
                }
            }
            return name;
        }
    };
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="rich printer">
    /**
     * Enhanced type/symbol printer that provides support for features like simple names
     * and type variable disambiguation. This enriched printer exploits the info
     * discovered during type/symbol preprocessing. This printer is set on the delegate
     * formatter so that rich type/symbol info can be properly rendered.
     */
    protected Printer printer = new Printer() {

        @Override
        public String localize(Locale locale, String key, Object... args) {
            return formatter.localize(locale, key, args);
        }

        @Override
        public String capturedVarId(CapturedType t, Locale locale) {
            return indexOf(t, WhereClauseKind.CAPTURED) + "";
        }

        @Override
        public String visitType(Type t, Locale locale) {
            String s = super.visitType(t, locale);
            if (t == syms.botType)
                s = localize(locale, "compiler.misc.type.null");
            return s;
        }

        @Override
        public String visitCapturedType(CapturedType t, Locale locale) {
            if (getConfiguration().isEnabled(RichFormatterFeature.WHERE_CLAUSES)) {
                return localize(locale,
                    "compiler.misc.captured.type",
                    indexOf(t, WhereClauseKind.CAPTURED));
            }
            else
                return super.visitCapturedType(t, locale);
        }

        @Override
        public String visitClassType(ClassType t, Locale locale) {
            if (t.isCompound() &&
                    getConfiguration().isEnabled(RichFormatterFeature.WHERE_CLAUSES)) {
                return localize(locale,
                        "compiler.misc.intersection.type",
                        indexOf(t, WhereClauseKind.INTERSECTION));
            }
            else
                return super.visitClassType(t, locale);
        }

        @Override
        protected String className(ClassType t, boolean longform, Locale locale) {
            Symbol sym = t.tsym;
            if (sym.name.length() == 0 ||
                    !getConfiguration().isEnabled(RichFormatterFeature.SIMPLE_NAMES)) {
                return super.className(t, longform, locale);
            }
            else if (longform)
                return nameSimplifier.simplify(sym).toString();
            else
                return sym.name.toString();
        }

        @Override
        public String visitTypeVar(TypeVar t, Locale locale) {
            if (unique(t) ||
                    !getConfiguration().isEnabled(RichFormatterFeature.UNIQUE_TYPEVAR_NAMES)) {
                return t.toString();
            }
            else {
                return localize(locale,
                        "compiler.misc.type.var",
                        t.toString(), indexOf(t, WhereClauseKind.TYPEVAR));
            }
        }

        private int indexOf(Type type, WhereClauseKind kind) {
            int index = 0;
            boolean found = false;
            for (Type t : whereClauses.get(kind).keySet()) {
                if (t == type) {
                    found = true;
                    break;
                }
                index++;
            }
            if (!found)
                throw new AssertionError("Missing symbol in where clause " + type);
            return index + 1;
        }

        private boolean unique(TypeVar typevar) {
            int found = 0;
            for (Type t : whereClauses.get(WhereClauseKind.TYPEVAR).keySet()) {
                if (t.toString().equals(typevar.toString())) {
                    found++;
                }
            }
            if (found < 1)
                throw new AssertionError("Missing type variable in where clause " + typevar);
            return found == 1;
        }

        @Override
        protected String printMethodArgs(List<Type> args, boolean varArgs, Locale locale) {
            return super.printMethodArgs(args, varArgs, locale);
        }

        @Override
        public String visitClassSymbol(ClassSymbol s, Locale locale) {
            String name = nameSimplifier.simplify(s);
            if (name.length() == 0 ||
                    !getConfiguration().isEnabled(RichFormatterFeature.SIMPLE_NAMES)) {
                return super.visitClassSymbol(s, locale);
            }
            else {
                return name;
            }
        }

        @Override
        public String visitMethodSymbol(MethodSymbol s, Locale locale) {
            String ownerName = visit(s.owner, locale);
            if ((s.flags() & BLOCK) != 0) {
               return ownerName;
            } else {
                String ms = (s.name == s.name.table.names.init)
                    ? ownerName
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
    };
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="type scanner">
    /**
     * Preprocess a given type looking for (i) additional info (where clauses) to be
     * added to the main diagnostic (ii) names to be compacted.
     */
    protected void preprocessType(Type t) {
        typePreprocessor.visit(t);
    }
    //where
    protected Types.UnaryVisitor<Void> typePreprocessor =
            new Types.UnaryVisitor<Void>() {

        public Void visit(List<Type> ts) {
            for (Type t : ts)
                visit(t);
            return null;
        }

        @Override
        public Void visitForAll(ForAll t, Void ignored) {
            visit(t.tvars);
            visit(t.qtype);
            return null;
        }

        @Override
        public Void visitMethodType(MethodType t, Void ignored) {
            visit(t.argtypes);
            visit(t.restype);
            return null;
        }

        @Override
        public Void visitErrorType(ErrorType t, Void ignored) {
            Type ot = t.getOriginalType();
            if (ot != null)
                visit(ot);
            return null;
        }

        @Override
        public Void visitArrayType(ArrayType t, Void ignored) {
            visit(t.elemtype);
            return null;
        }

        @Override
        public Void visitWildcardType(WildcardType t, Void ignored) {
            visit(t.type);
            return null;
        }

        public Void visitType(Type t, Void ignored) {
            return null;
        }

        @Override
        public Void visitCapturedType(CapturedType t, Void ignored) {
            if (!whereClauses.get(WhereClauseKind.CAPTURED).containsKey(t)) {
                String suffix = t.lower == syms.botType ? ".1" : "";
                JCDiagnostic d = diags.fragment("where.captured"+ suffix, t, t.bound, t.lower, t.wildcard);
                whereClauses.get(WhereClauseKind.CAPTURED).put(t, d);
                visit(t.wildcard);
                visit(t.lower);
                visit(t.bound);
            }
            return null;
        }

        @Override
        public Void visitClassType(ClassType t, Void ignored) {
            if (t.isCompound()) {
                if (!whereClauses.get(WhereClauseKind.INTERSECTION).containsKey(t)) {
                    Type supertype = types.supertype(t);
                    List<Type> interfaces = types.interfaces(t);
                    JCDiagnostic d = diags.fragment("where.intersection", t, interfaces.prepend(supertype));
                    whereClauses.get(WhereClauseKind.INTERSECTION).put(t, d);
                    visit(supertype);
                    visit(interfaces);
                }
            }
            nameSimplifier.addUsage(t.tsym);
            visit(t.getTypeArguments());
            if (t.getEnclosingType() != Type.noType)
                visit(t.getEnclosingType());
            return null;
        }

        @Override
        public Void visitTypeVar(TypeVar t, Void ignored) {
            if (!whereClauses.get(WhereClauseKind.TYPEVAR).containsKey(t)) {
                Type bound = t.bound;
                while ((bound instanceof ErrorType))
                    bound = ((ErrorType)bound).getOriginalType();
                List<Type> bounds  = types.getBounds(t);
                nameSimplifier.addUsage(t.tsym);

                boolean boundErroneous = bounds.head == null ||
                                         bounds.head.tag == NONE ||
                                         bounds.head.tag == ERROR;


                JCDiagnostic d = diags.fragment("where.typevar" +
                        (boundErroneous ? ".1" : ""), t, bounds,
                        Kinds.kindName(t.tsym.location()), t.tsym.location());
                whereClauses.get(WhereClauseKind.TYPEVAR).put(t, d);
                symbolPreprocessor.visit(t.tsym.location(), null);
                visit(bounds);
            }
            return null;
        }
    };
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="symbol scanner">
    /**
     * Preprocess a given symbol looking for (i) additional info (where clauses) to be
     * asdded to the main diagnostic (ii) names to be compacted
     */
    protected void preprocessSymbol(Symbol s) {
        symbolPreprocessor.visit(s, null);
    }
    //where
    protected Types.DefaultSymbolVisitor<Void, Void> symbolPreprocessor =
            new Types.DefaultSymbolVisitor<Void, Void>() {

        @Override
        public Void visitClassSymbol(ClassSymbol s, Void ignored) {
            nameSimplifier.addUsage(s);
            return null;
        }

        @Override
        public Void visitSymbol(Symbol s, Void ignored) {
            return null;
        }

        @Override
        public Void visitMethodSymbol(MethodSymbol s, Void ignored) {
            visit(s.owner, null);
            typePreprocessor.visit(s.type);
            return null;
        }
    };
    // </editor-fold>

    @Override
    public RichConfiguration getConfiguration() {
        //the following cast is always safe - see init
        return (RichConfiguration)configuration;
    }

    /**
     * Configuration object provided by the rich formatter.
     */
    public static class RichConfiguration extends ForwardingDiagnosticFormatter.ForwardingConfiguration {

        /** set of enabled rich formatter's features */
        protected java.util.EnumSet<RichFormatterFeature> features;

        @SuppressWarnings("fallthrough")
        public RichConfiguration(Options options, AbstractDiagnosticFormatter formatter) {
            super(formatter.getConfiguration());
            features = formatter.isRaw() ? EnumSet.noneOf(RichFormatterFeature.class) :
                EnumSet.of(RichFormatterFeature.SIMPLE_NAMES,
                    RichFormatterFeature.WHERE_CLAUSES,
                    RichFormatterFeature.UNIQUE_TYPEVAR_NAMES);
            String diagOpts = options.get("diags");
            if (diagOpts != null) {
                for (String args: diagOpts.split(",")) {
                    if (args.equals("-where")) {
                        features.remove(RichFormatterFeature.WHERE_CLAUSES);
                    }
                    else if (args.equals("where")) {
                        features.add(RichFormatterFeature.WHERE_CLAUSES);
                    }
                    if (args.equals("-simpleNames")) {
                        features.remove(RichFormatterFeature.SIMPLE_NAMES);
                    }
                    else if (args.equals("simpleNames")) {
                        features.add(RichFormatterFeature.SIMPLE_NAMES);
                    }
                    if (args.equals("-disambiguateTvars")) {
                        features.remove(RichFormatterFeature.UNIQUE_TYPEVAR_NAMES);
                    }
                    else if (args.equals("disambiguateTvars")) {
                        features.add(RichFormatterFeature.UNIQUE_TYPEVAR_NAMES);
                    }
                }
            }
        }

        /**
         * Returns a list of all the features supported by the rich formatter.
         * @return list of supported features
         */
        public RichFormatterFeature[] getAvailableFeatures() {
            return RichFormatterFeature.values();
        }

        /**
         * Enable a specific feature on this rich formatter.
         * @param feature feature to be enabled
         */
        public void enable(RichFormatterFeature feature) {
            features.add(feature);
        }

        /**
         * Disable a specific feature on this rich formatter.
         * @param feature feature to be disabled
         */
        public void disable(RichFormatterFeature feature) {
            features.remove(feature);
        }

        /**
         * Is a given feature enabled on this formatter?
         * @param feature feature to be tested
         */
        public boolean isEnabled(RichFormatterFeature feature) {
            return features.contains(feature);
        }

        /**
         * The advanced formatting features provided by the rich formatter
         */
        public enum RichFormatterFeature {
            /** a list of additional info regarding a given type/symbol */
            WHERE_CLAUSES,
            /** full class names simplification (where possible) */
            SIMPLE_NAMES,
            /** type-variable names disambiguation */
            UNIQUE_TYPEVAR_NAMES;
        }
    }
}
