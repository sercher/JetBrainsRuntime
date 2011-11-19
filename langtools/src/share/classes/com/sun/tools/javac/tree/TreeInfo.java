/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.tree;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import java.util.Map;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree.*;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.tree.JCTree.Tag.*;
import static com.sun.tools.javac.tree.JCTree.Tag.BLOCK;
import static com.sun.tools.javac.tree.JCTree.Tag.SYNCHRONIZED;

/** Utility class containing inspector methods for trees.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class TreeInfo {
    protected static final Context.Key<TreeInfo> treeInfoKey =
        new Context.Key<TreeInfo>();

    public static TreeInfo instance(Context context) {
        TreeInfo instance = context.get(treeInfoKey);
        if (instance == null)
            instance = new TreeInfo(context);
        return instance;
    }

    /** The names of all operators.
     */
    private Name[] opname = new Name[Tag.getNumberOfOperators()];

    private void setOpname(Tag tag, String name, Names names) {
         setOpname(tag, names.fromString(name));
     }
     private void setOpname(Tag tag, Name name) {
         opname[tag.operatorIndex()] = name;
     }

    private TreeInfo(Context context) {
        context.put(treeInfoKey, this);

        Names names = Names.instance(context);
        setOpname(POS, "+", names);
        setOpname(NEG, names.hyphen);
        setOpname(NOT, "!", names);
        setOpname(COMPL, "~", names);
        setOpname(PREINC, "++", names);
        setOpname(PREDEC, "--", names);
        setOpname(POSTINC, "++", names);
        setOpname(POSTDEC, "--", names);
        setOpname(NULLCHK, "<*nullchk*>", names);
        setOpname(OR, "||", names);
        setOpname(AND, "&&", names);
        setOpname(EQ, "==", names);
        setOpname(NE, "!=", names);
        setOpname(LT, "<", names);
        setOpname(GT, ">", names);
        setOpname(LE, "<=", names);
        setOpname(GE, ">=", names);
        setOpname(BITOR, "|", names);
        setOpname(BITXOR, "^", names);
        setOpname(BITAND, "&", names);
        setOpname(SL, "<<", names);
        setOpname(SR, ">>", names);
        setOpname(USR, ">>>", names);
        setOpname(PLUS, "+", names);
        setOpname(MINUS, names.hyphen);
        setOpname(MUL, names.asterisk);
        setOpname(DIV, names.slash);
        setOpname(MOD, "%", names);
    }


    /** Return name of operator with given tree tag.
     */
    public Name operatorName(JCTree.Tag tag) {
        return opname[tag.operatorIndex()];
    }

    /** Is tree a constructor declaration?
     */
    public static boolean isConstructor(JCTree tree) {
        if (tree.hasTag(METHODDEF)) {
            Name name = ((JCMethodDecl) tree).name;
            return name == name.table.names.init;
        } else {
            return false;
        }
    }

    /** Is there a constructor declaration in the given list of trees?
     */
    public static boolean hasConstructors(List<JCTree> trees) {
        for (List<JCTree> l = trees; l.nonEmpty(); l = l.tail)
            if (isConstructor(l.head)) return true;
        return false;
    }

    public static boolean isMultiCatch(JCCatch catchClause) {
        return catchClause.param.vartype.hasTag(TYPEUNION);
    }

    /** Is statement an initializer for a synthetic field?
     */
    public static boolean isSyntheticInit(JCTree stat) {
        if (stat.hasTag(EXEC)) {
            JCExpressionStatement exec = (JCExpressionStatement)stat;
            if (exec.expr.hasTag(ASSIGN)) {
                JCAssign assign = (JCAssign)exec.expr;
                if (assign.lhs.hasTag(SELECT)) {
                    JCFieldAccess select = (JCFieldAccess)assign.lhs;
                    if (select.sym != null &&
                        (select.sym.flags() & SYNTHETIC) != 0) {
                        Name selected = name(select.selected);
                        if (selected != null && selected == selected.table.names._this)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    /** If the expression is a method call, return the method name, null
     *  otherwise. */
    public static Name calledMethodName(JCTree tree) {
        if (tree.hasTag(EXEC)) {
            JCExpressionStatement exec = (JCExpressionStatement)tree;
            if (exec.expr.hasTag(APPLY)) {
                Name mname = TreeInfo.name(((JCMethodInvocation) exec.expr).meth);
                return mname;
            }
        }
        return null;
    }

    /** Is this a call to this or super?
     */
    public static boolean isSelfCall(JCTree tree) {
        Name name = calledMethodName(tree);
        if (name != null) {
            Names names = name.table.names;
            return name==names._this || name==names._super;
        } else {
            return false;
        }
    }

    /** Is this a call to super?
     */
    public static boolean isSuperCall(JCTree tree) {
        Name name = calledMethodName(tree);
        if (name != null) {
            Names names = name.table.names;
            return name==names._super;
        } else {
            return false;
        }
    }

    /** Is this a constructor whose first (non-synthetic) statement is not
     *  of the form this(...)?
     */
    public static boolean isInitialConstructor(JCTree tree) {
        JCMethodInvocation app = firstConstructorCall(tree);
        if (app == null) return false;
        Name meth = name(app.meth);
        return meth == null || meth != meth.table.names._this;
    }

    /** Return the first call in a constructor definition. */
    public static JCMethodInvocation firstConstructorCall(JCTree tree) {
        if (!tree.hasTag(METHODDEF)) return null;
        JCMethodDecl md = (JCMethodDecl) tree;
        Names names = md.name.table.names;
        if (md.name != names.init) return null;
        if (md.body == null) return null;
        List<JCStatement> stats = md.body.stats;
        // Synthetic initializations can appear before the super call.
        while (stats.nonEmpty() && isSyntheticInit(stats.head))
            stats = stats.tail;
        if (stats.isEmpty()) return null;
        if (!stats.head.hasTag(EXEC)) return null;
        JCExpressionStatement exec = (JCExpressionStatement) stats.head;
        if (!exec.expr.hasTag(APPLY)) return null;
        return (JCMethodInvocation)exec.expr;
    }

    /** Return true if a tree represents a diamond new expr. */
    public static boolean isDiamond(JCTree tree) {
        switch(tree.getTag()) {
            case TYPEAPPLY: return ((JCTypeApply)tree).getTypeArguments().isEmpty();
            case NEWCLASS: return isDiamond(((JCNewClass)tree).clazz);
            default: return false;
        }
    }

    /** Return true if a tree represents the null literal. */
    public static boolean isNull(JCTree tree) {
        if (!tree.hasTag(LITERAL))
            return false;
        JCLiteral lit = (JCLiteral) tree;
        return (lit.typetag == TypeTags.BOT);
    }

    /** The position of the first statement in a block, or the position of
     *  the block itself if it is empty.
     */
    public static int firstStatPos(JCTree tree) {
        if (tree.hasTag(BLOCK) && ((JCBlock) tree).stats.nonEmpty())
            return ((JCBlock) tree).stats.head.pos;
        else
            return tree.pos;
    }

    /** The end position of given tree, if it is a block with
     *  defined endpos.
     */
    public static int endPos(JCTree tree) {
        if (tree.hasTag(BLOCK) && ((JCBlock) tree).endpos != Position.NOPOS)
            return ((JCBlock) tree).endpos;
        else if (tree.hasTag(SYNCHRONIZED))
            return endPos(((JCSynchronized) tree).body);
        else if (tree.hasTag(TRY)) {
            JCTry t = (JCTry) tree;
            return endPos((t.finalizer != null)
                          ? t.finalizer
                          : t.catchers.last().body);
        } else
            return tree.pos;
    }


    /** Get the start position for a tree node.  The start position is
     * defined to be the position of the first character of the first
     * token of the node's source text.
     * @param tree  The tree node
     */
    public static int getStartPos(JCTree tree) {
        if (tree == null)
            return Position.NOPOS;

        switch(tree.getTag()) {
            case APPLY:
                return getStartPos(((JCMethodInvocation) tree).meth);
            case ASSIGN:
                return getStartPos(((JCAssign) tree).lhs);
            case BITOR_ASG: case BITXOR_ASG: case BITAND_ASG:
            case SL_ASG: case SR_ASG: case USR_ASG:
            case PLUS_ASG: case MINUS_ASG: case MUL_ASG:
            case DIV_ASG: case MOD_ASG:
                return getStartPos(((JCAssignOp) tree).lhs);
            case OR: case AND: case BITOR:
            case BITXOR: case BITAND: case EQ:
            case NE: case LT: case GT:
            case LE: case GE: case SL:
            case SR: case USR: case PLUS:
            case MINUS: case MUL: case DIV:
            case MOD:
                return getStartPos(((JCBinary) tree).lhs);
            case CLASSDEF: {
                JCClassDecl node = (JCClassDecl)tree;
                if (node.mods.pos != Position.NOPOS)
                    return node.mods.pos;
                break;
            }
            case CONDEXPR:
                return getStartPos(((JCConditional) tree).cond);
            case EXEC:
                return getStartPos(((JCExpressionStatement) tree).expr);
            case INDEXED:
                return getStartPos(((JCArrayAccess) tree).indexed);
            case METHODDEF: {
                JCMethodDecl node = (JCMethodDecl)tree;
                if (node.mods.pos != Position.NOPOS)
                    return node.mods.pos;
                if (node.typarams.nonEmpty()) // List.nil() used for no typarams
                    return getStartPos(node.typarams.head);
                return node.restype == null ? node.pos : getStartPos(node.restype);
            }
            case SELECT:
                return getStartPos(((JCFieldAccess) tree).selected);
            case TYPEAPPLY:
                return getStartPos(((JCTypeApply) tree).clazz);
            case TYPEARRAY:
                return getStartPos(((JCArrayTypeTree) tree).elemtype);
            case TYPETEST:
                return getStartPos(((JCInstanceOf) tree).expr);
            case POSTINC:
            case POSTDEC:
                return getStartPos(((JCUnary) tree).arg);
            case NEWCLASS: {
                JCNewClass node = (JCNewClass)tree;
                if (node.encl != null)
                    return getStartPos(node.encl);
                break;
            }
            case VARDEF: {
                JCVariableDecl node = (JCVariableDecl)tree;
                if (node.mods.pos != Position.NOPOS) {
                    return node.mods.pos;
                } else {
                    return getStartPos(node.vartype);
                }
            }
            case ERRONEOUS: {
                JCErroneous node = (JCErroneous)tree;
                if (node.errs != null && node.errs.nonEmpty())
                    return getStartPos(node.errs.head);
            }
        }
        return tree.pos;
    }

    /** The end position of given tree, given  a table of end positions generated by the parser
     */
    public static int getEndPos(JCTree tree, Map<JCTree, Integer> endPositions) {
        if (tree == null)
            return Position.NOPOS;

        if (endPositions == null) {
            // fall back on limited info in the tree
            return endPos(tree);
        }

        Integer mapPos = endPositions.get(tree);
        if (mapPos != null)
            return mapPos;

        switch(tree.getTag()) {
            case BITOR_ASG: case BITXOR_ASG: case BITAND_ASG:
            case SL_ASG: case SR_ASG: case USR_ASG:
            case PLUS_ASG: case MINUS_ASG: case MUL_ASG:
            case DIV_ASG: case MOD_ASG:
                return getEndPos(((JCAssignOp) tree).rhs, endPositions);
            case OR: case AND: case BITOR:
            case BITXOR: case BITAND: case EQ:
            case NE: case LT: case GT:
            case LE: case GE: case SL:
            case SR: case USR: case PLUS:
            case MINUS: case MUL: case DIV:
            case MOD:
                return getEndPos(((JCBinary) tree).rhs, endPositions);
            case CASE:
                return getEndPos(((JCCase) tree).stats.last(), endPositions);
            case CATCH:
                return getEndPos(((JCCatch) tree).body, endPositions);
            case CONDEXPR:
                return getEndPos(((JCConditional) tree).falsepart, endPositions);
            case FORLOOP:
                return getEndPos(((JCForLoop) tree).body, endPositions);
            case FOREACHLOOP:
                return getEndPos(((JCEnhancedForLoop) tree).body, endPositions);
            case IF: {
                JCIf node = (JCIf)tree;
                if (node.elsepart == null) {
                    return getEndPos(node.thenpart, endPositions);
                } else {
                    return getEndPos(node.elsepart, endPositions);
                }
            }
            case LABELLED:
                return getEndPos(((JCLabeledStatement) tree).body, endPositions);
            case MODIFIERS:
                return getEndPos(((JCModifiers) tree).annotations.last(), endPositions);
            case SYNCHRONIZED:
                return getEndPos(((JCSynchronized) tree).body, endPositions);
            case TOPLEVEL:
                return getEndPos(((JCCompilationUnit) tree).defs.last(), endPositions);
            case TRY: {
                JCTry node = (JCTry)tree;
                if (node.finalizer != null) {
                    return getEndPos(node.finalizer, endPositions);
                } else if (!node.catchers.isEmpty()) {
                    return getEndPos(node.catchers.last(), endPositions);
                } else {
                    return getEndPos(node.body, endPositions);
                }
            }
            case WILDCARD:
                return getEndPos(((JCWildcard) tree).inner, endPositions);
            case TYPECAST:
                return getEndPos(((JCTypeCast) tree).expr, endPositions);
            case TYPETEST:
                return getEndPos(((JCInstanceOf) tree).clazz, endPositions);
            case POS:
            case NEG:
            case NOT:
            case COMPL:
            case PREINC:
            case PREDEC:
                return getEndPos(((JCUnary) tree).arg, endPositions);
            case WHILELOOP:
                return getEndPos(((JCWhileLoop) tree).body, endPositions);
            case ERRONEOUS: {
                JCErroneous node = (JCErroneous)tree;
                if (node.errs != null && node.errs.nonEmpty())
                    return getEndPos(node.errs.last(), endPositions);
            }
        }
        return Position.NOPOS;
    }


    /** A DiagnosticPosition with the preferred position set to the
     *  end position of given tree, if it is a block with
     *  defined endpos.
     */
    public static DiagnosticPosition diagEndPos(final JCTree tree) {
        final int endPos = TreeInfo.endPos(tree);
        return new DiagnosticPosition() {
            public JCTree getTree() { return tree; }
            public int getStartPosition() { return TreeInfo.getStartPos(tree); }
            public int getPreferredPosition() { return endPos; }
            public int getEndPosition(Map<JCTree, Integer> endPosTable) {
                return TreeInfo.getEndPos(tree, endPosTable);
            }
        };
    }

    /** The position of the finalizer of given try/synchronized statement.
     */
    public static int finalizerPos(JCTree tree) {
        if (tree.hasTag(TRY)) {
            JCTry t = (JCTry) tree;
            Assert.checkNonNull(t.finalizer);
            return firstStatPos(t.finalizer);
        } else if (tree.hasTag(SYNCHRONIZED)) {
            return endPos(((JCSynchronized) tree).body);
        } else {
            throw new AssertionError();
        }
    }

    /** Find the position for reporting an error about a symbol, where
     *  that symbol is defined somewhere in the given tree. */
    public static int positionFor(final Symbol sym, final JCTree tree) {
        JCTree decl = declarationFor(sym, tree);
        return ((decl != null) ? decl : tree).pos;
    }

    /** Find the position for reporting an error about a symbol, where
     *  that symbol is defined somewhere in the given tree. */
    public static DiagnosticPosition diagnosticPositionFor(final Symbol sym, final JCTree tree) {
        JCTree decl = declarationFor(sym, tree);
        return ((decl != null) ? decl : tree).pos();
    }

    /** Find the declaration for a symbol, where
     *  that symbol is defined somewhere in the given tree. */
    public static JCTree declarationFor(final Symbol sym, final JCTree tree) {
        class DeclScanner extends TreeScanner {
            JCTree result = null;
            public void scan(JCTree tree) {
                if (tree!=null && result==null)
                    tree.accept(this);
            }
            public void visitTopLevel(JCCompilationUnit that) {
                if (that.packge == sym) result = that;
                else super.visitTopLevel(that);
            }
            public void visitClassDef(JCClassDecl that) {
                if (that.sym == sym) result = that;
                else super.visitClassDef(that);
            }
            public void visitMethodDef(JCMethodDecl that) {
                if (that.sym == sym) result = that;
                else super.visitMethodDef(that);
            }
            public void visitVarDef(JCVariableDecl that) {
                if (that.sym == sym) result = that;
                else super.visitVarDef(that);
            }
            public void visitTypeParameter(JCTypeParameter that) {
                if (that.type != null && that.type.tsym == sym) result = that;
                else super.visitTypeParameter(that);
            }
        }
        DeclScanner s = new DeclScanner();
        tree.accept(s);
        return s.result;
    }

    public static Env<AttrContext> scopeFor(JCTree node, JCCompilationUnit unit) {
        return scopeFor(pathFor(node, unit));
    }

    public static Env<AttrContext> scopeFor(List<JCTree> path) {
        // TODO: not implemented yet
        throw new UnsupportedOperationException("not implemented yet");
    }

    public static List<JCTree> pathFor(final JCTree node, final JCCompilationUnit unit) {
        class Result extends Error {
            static final long serialVersionUID = -5942088234594905625L;
            List<JCTree> path;
            Result(List<JCTree> path) {
                this.path = path;
            }
        }
        class PathFinder extends TreeScanner {
            List<JCTree> path = List.nil();
            public void scan(JCTree tree) {
                if (tree != null) {
                    path = path.prepend(tree);
                    if (tree == node)
                        throw new Result(path);
                    super.scan(tree);
                    path = path.tail;
                }
            }
        }
        try {
            new PathFinder().scan(unit);
        } catch (Result result) {
            return result.path;
        }
        return List.nil();
    }

    /** Return the statement referenced by a label.
     *  If the label refers to a loop or switch, return that switch
     *  otherwise return the labelled statement itself
     */
    public static JCTree referencedStatement(JCLabeledStatement tree) {
        JCTree t = tree;
        do t = ((JCLabeledStatement) t).body;
        while (t.hasTag(LABELLED));
        switch (t.getTag()) {
        case DOLOOP: case WHILELOOP: case FORLOOP: case FOREACHLOOP: case SWITCH:
            return t;
        default:
            return tree;
        }
    }

    /** Skip parens and return the enclosed expression
     */
    public static JCExpression skipParens(JCExpression tree) {
        while (tree.hasTag(PARENS)) {
            tree = ((JCParens) tree).expr;
        }
        return tree;
    }

    /** Skip parens and return the enclosed expression
     */
    public static JCTree skipParens(JCTree tree) {
        if (tree.hasTag(PARENS))
            return skipParens((JCParens)tree);
        else
            return tree;
    }

    /** Return the types of a list of trees.
     */
    public static List<Type> types(List<? extends JCTree> trees) {
        ListBuffer<Type> ts = new ListBuffer<Type>();
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail)
            ts.append(l.head.type);
        return ts.toList();
    }

    /** If this tree is an identifier or a field or a parameterized type,
     *  return its name, otherwise return null.
     */
    public static Name name(JCTree tree) {
        switch (tree.getTag()) {
        case IDENT:
            return ((JCIdent) tree).name;
        case SELECT:
            return ((JCFieldAccess) tree).name;
        case TYPEAPPLY:
            return name(((JCTypeApply) tree).clazz);
        default:
            return null;
        }
    }

    /** If this tree is a qualified identifier, its return fully qualified name,
     *  otherwise return null.
     */
    public static Name fullName(JCTree tree) {
        tree = skipParens(tree);
        switch (tree.getTag()) {
        case IDENT:
            return ((JCIdent) tree).name;
        case SELECT:
            Name sname = fullName(((JCFieldAccess) tree).selected);
            return sname == null ? null : sname.append('.', name(tree));
        default:
            return null;
        }
    }

    public static Symbol symbolFor(JCTree node) {
        node = skipParens(node);
        switch (node.getTag()) {
        case CLASSDEF:
            return ((JCClassDecl) node).sym;
        case METHODDEF:
            return ((JCMethodDecl) node).sym;
        case VARDEF:
            return ((JCVariableDecl) node).sym;
        default:
            return null;
        }
    }

    public static boolean isDeclaration(JCTree node) {
        node = skipParens(node);
        switch (node.getTag()) {
        case CLASSDEF:
        case METHODDEF:
        case VARDEF:
            return true;
        default:
            return false;
        }
    }

    /** If this tree is an identifier or a field, return its symbol,
     *  otherwise return null.
     */
    public static Symbol symbol(JCTree tree) {
        tree = skipParens(tree);
        switch (tree.getTag()) {
        case IDENT:
            return ((JCIdent) tree).sym;
        case SELECT:
            return ((JCFieldAccess) tree).sym;
        case TYPEAPPLY:
            return symbol(((JCTypeApply) tree).clazz);
        default:
            return null;
        }
    }

    /** Return true if this is a nonstatic selection. */
    public static boolean nonstaticSelect(JCTree tree) {
        tree = skipParens(tree);
        if (!tree.hasTag(SELECT)) return false;
        JCFieldAccess s = (JCFieldAccess) tree;
        Symbol e = symbol(s.selected);
        return e == null || (e.kind != Kinds.PCK && e.kind != Kinds.TYP);
    }

    /** If this tree is an identifier or a field, set its symbol, otherwise skip.
     */
    public static void setSymbol(JCTree tree, Symbol sym) {
        tree = skipParens(tree);
        switch (tree.getTag()) {
        case IDENT:
            ((JCIdent) tree).sym = sym; break;
        case SELECT:
            ((JCFieldAccess) tree).sym = sym; break;
        default:
        }
    }

    /** If this tree is a declaration or a block, return its flags field,
     *  otherwise return 0.
     */
    public static long flags(JCTree tree) {
        switch (tree.getTag()) {
        case VARDEF:
            return ((JCVariableDecl) tree).mods.flags;
        case METHODDEF:
            return ((JCMethodDecl) tree).mods.flags;
        case CLASSDEF:
            return ((JCClassDecl) tree).mods.flags;
        case BLOCK:
            return ((JCBlock) tree).flags;
        default:
            return 0;
        }
    }

    /** Return first (smallest) flag in `flags':
     *  pre: flags != 0
     */
    public static long firstFlag(long flags) {
        int flag = 1;
        while ((flag & StandardFlags) != 0 && (flag & flags) == 0)
            flag = flag << 1;
        return flag;
    }

    /** Return flags as a string, separated by " ".
     */
    public static String flagNames(long flags) {
        return Flags.toString(flags & StandardFlags).trim();
    }

    /** Operator precedences values.
     */
    public static final int
        notExpression = -1,   // not an expression
        noPrec = 0,           // no enclosing expression
        assignPrec = 1,
        assignopPrec = 2,
        condPrec = 3,
        orPrec = 4,
        andPrec = 5,
        bitorPrec = 6,
        bitxorPrec = 7,
        bitandPrec = 8,
        eqPrec = 9,
        ordPrec = 10,
        shiftPrec = 11,
        addPrec = 12,
        mulPrec = 13,
        prefixPrec = 14,
        postfixPrec = 15,
        precCount = 16;


    /** Map operators to their precedence levels.
     */
    public static int opPrec(JCTree.Tag op) {
        switch(op) {
        case POS:
        case NEG:
        case NOT:
        case COMPL:
        case PREINC:
        case PREDEC: return prefixPrec;
        case POSTINC:
        case POSTDEC:
        case NULLCHK: return postfixPrec;
        case ASSIGN: return assignPrec;
        case BITOR_ASG:
        case BITXOR_ASG:
        case BITAND_ASG:
        case SL_ASG:
        case SR_ASG:
        case USR_ASG:
        case PLUS_ASG:
        case MINUS_ASG:
        case MUL_ASG:
        case DIV_ASG:
        case MOD_ASG: return assignopPrec;
        case OR: return orPrec;
        case AND: return andPrec;
        case EQ:
        case NE: return eqPrec;
        case LT:
        case GT:
        case LE:
        case GE: return ordPrec;
        case BITOR: return bitorPrec;
        case BITXOR: return bitxorPrec;
        case BITAND: return bitandPrec;
        case SL:
        case SR:
        case USR: return shiftPrec;
        case PLUS:
        case MINUS: return addPrec;
        case MUL:
        case DIV:
        case MOD: return mulPrec;
        case TYPETEST: return ordPrec;
        default: throw new AssertionError();
        }
    }

    static Tree.Kind tagToKind(JCTree.Tag tag) {
        switch (tag) {
        // Postfix expressions
        case POSTINC:           // _ ++
            return Tree.Kind.POSTFIX_INCREMENT;
        case POSTDEC:           // _ --
            return Tree.Kind.POSTFIX_DECREMENT;

        // Unary operators
        case PREINC:            // ++ _
            return Tree.Kind.PREFIX_INCREMENT;
        case PREDEC:            // -- _
            return Tree.Kind.PREFIX_DECREMENT;
        case POS:               // +
            return Tree.Kind.UNARY_PLUS;
        case NEG:               // -
            return Tree.Kind.UNARY_MINUS;
        case COMPL:             // ~
            return Tree.Kind.BITWISE_COMPLEMENT;
        case NOT:               // !
            return Tree.Kind.LOGICAL_COMPLEMENT;

        // Binary operators

        // Multiplicative operators
        case MUL:               // *
            return Tree.Kind.MULTIPLY;
        case DIV:               // /
            return Tree.Kind.DIVIDE;
        case MOD:               // %
            return Tree.Kind.REMAINDER;

        // Additive operators
        case PLUS:              // +
            return Tree.Kind.PLUS;
        case MINUS:             // -
            return Tree.Kind.MINUS;

        // Shift operators
        case SL:                // <<
            return Tree.Kind.LEFT_SHIFT;
        case SR:                // >>
            return Tree.Kind.RIGHT_SHIFT;
        case USR:               // >>>
            return Tree.Kind.UNSIGNED_RIGHT_SHIFT;

        // Relational operators
        case LT:                // <
            return Tree.Kind.LESS_THAN;
        case GT:                // >
            return Tree.Kind.GREATER_THAN;
        case LE:                // <=
            return Tree.Kind.LESS_THAN_EQUAL;
        case GE:                // >=
            return Tree.Kind.GREATER_THAN_EQUAL;

        // Equality operators
        case EQ:                // ==
            return Tree.Kind.EQUAL_TO;
        case NE:                // !=
            return Tree.Kind.NOT_EQUAL_TO;

        // Bitwise and logical operators
        case BITAND:            // &
            return Tree.Kind.AND;
        case BITXOR:            // ^
            return Tree.Kind.XOR;
        case BITOR:             // |
            return Tree.Kind.OR;

        // Conditional operators
        case AND:               // &&
            return Tree.Kind.CONDITIONAL_AND;
        case OR:                // ||
            return Tree.Kind.CONDITIONAL_OR;

        // Assignment operators
        case MUL_ASG:           // *=
            return Tree.Kind.MULTIPLY_ASSIGNMENT;
        case DIV_ASG:           // /=
            return Tree.Kind.DIVIDE_ASSIGNMENT;
        case MOD_ASG:           // %=
            return Tree.Kind.REMAINDER_ASSIGNMENT;
        case PLUS_ASG:          // +=
            return Tree.Kind.PLUS_ASSIGNMENT;
        case MINUS_ASG:         // -=
            return Tree.Kind.MINUS_ASSIGNMENT;
        case SL_ASG:            // <<=
            return Tree.Kind.LEFT_SHIFT_ASSIGNMENT;
        case SR_ASG:            // >>=
            return Tree.Kind.RIGHT_SHIFT_ASSIGNMENT;
        case USR_ASG:           // >>>=
            return Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT;
        case BITAND_ASG:        // &=
            return Tree.Kind.AND_ASSIGNMENT;
        case BITXOR_ASG:        // ^=
            return Tree.Kind.XOR_ASSIGNMENT;
        case BITOR_ASG:         // |=
            return Tree.Kind.OR_ASSIGNMENT;

        // Null check (implementation detail), for example, __.getClass()
        case NULLCHK:
            return Tree.Kind.OTHER;

        default:
            return null;
        }
    }

    /**
     * Returns the underlying type of the tree if it is annotated type,
     * or the tree itself otherwise
     */
    public static JCExpression typeIn(JCExpression tree) {
        switch (tree.getTag()) {
        case IDENT: /* simple names */
        case TYPEIDENT: /* primitive name */
        case SELECT: /* qualified name */
        case TYPEARRAY: /* array types */
        case WILDCARD: /* wild cards */
        case TYPEPARAMETER: /* type parameters */
        case TYPEAPPLY: /* parameterized types */
            return tree;
        default:
            throw new AssertionError("Unexpected type tree: " + tree);
        }
    }

    public static JCTree innermostType(JCTree type) {
        switch (type.getTag()) {
        case TYPEARRAY:
            return innermostType(((JCArrayTypeTree)type).elemtype);
        case WILDCARD:
            return innermostType(((JCWildcard)type).inner);
        default:
            return type;
        }
    }
}
