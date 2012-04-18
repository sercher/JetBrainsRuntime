/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.parser;

import java.util.*;

import com.sun.source.tree.MemberReferenceTree.ReferenceMode;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.parser.Tokens.*;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import static com.sun.tools.javac.parser.Tokens.TokenKind.*;
import static com.sun.tools.javac.parser.Tokens.TokenKind.ASSERT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.CASE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.CATCH;
import static com.sun.tools.javac.parser.Tokens.TokenKind.EQ;
import static com.sun.tools.javac.parser.Tokens.TokenKind.GT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.IMPORT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LT;
import static com.sun.tools.javac.util.ListBuffer.lb;
import static com.sun.tools.javac.tree.JCTree.Tag.*;

/** The parser maps a token sequence into an abstract syntax
 *  tree. It operates by recursive descent, with code derived
 *  systematically from an LL(1) grammar. For efficiency reasons, an
 *  operator precedence scheme is used for parsing binary operation
 *  expressions.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavacParser implements Parser {

    /** The number of precedence levels of infix operators.
     */
    private static final int infixPrecedenceLevels = 10;

    /** The scanner used for lexical analysis.
     */
    protected Lexer S;

    /** The factory to be used for abstract syntax tree construction.
     */
    protected TreeMaker F;

    /** The log to be used for error diagnostics.
     */
    private Log log;

    /** The Source language setting. */
    private Source source;

    /** The name table. */
    private Names names;

    /** End position mappings container */
    private final AbstractEndPosTable endPosTable;

    /** Construct a parser from a given scanner, tree factory and log.
     */
    protected JavacParser(ParserFactory fac,
                     Lexer S,
                     boolean keepDocComments,
                     boolean keepLineMap,
                     boolean keepEndPositions) {
        this.S = S;
        nextToken(); // prime the pump
        this.F = fac.F;
        this.log = fac.log;
        this.names = fac.names;
        this.source = fac.source;
        this.allowGenerics = source.allowGenerics();
        this.allowVarargs = source.allowVarargs();
        this.allowAsserts = source.allowAsserts();
        this.allowEnums = source.allowEnums();
        this.allowForeach = source.allowForeach();
        this.allowStaticImport = source.allowStaticImport();
        this.allowAnnotations = source.allowAnnotations();
        this.allowTWR = source.allowTryWithResources();
        this.allowDiamond = source.allowDiamond();
        this.allowMulticatch = source.allowMulticatch();
        this.allowStringFolding = fac.options.getBoolean("allowStringFolding", true);
        this.allowLambda = source.allowLambda() &&
                fac.options.isSet("allowLambda");
        this.allowMethodReferences = source.allowMethodReferences() &&
                fac.options.isSet("allowMethodReferences");
        this.keepDocComments = keepDocComments;
        docComments = keepDocComments ? new HashMap<JCTree,String>() : null;
        this.keepLineMap = keepLineMap;
        this.errorTree = F.Erroneous();
        endPosTable = newEndPosTable(keepEndPositions);
    }

    protected AbstractEndPosTable newEndPosTable(boolean keepEndPositions) {
        return  keepEndPositions
                ? new SimpleEndPosTable()
                : new EmptyEndPosTable();
    }
    /** Switch: Should generics be recognized?
     */
    boolean allowGenerics;

    /** Switch: Should diamond operator be recognized?
     */
    boolean allowDiamond;

    /** Switch: Should multicatch clause be accepted?
     */
    boolean allowMulticatch;

    /** Switch: Should varargs be recognized?
     */
    boolean allowVarargs;

    /** Switch: should we recognize assert statements, or just give a warning?
     */
    boolean allowAsserts;

    /** Switch: should we recognize enums, or just give a warning?
     */
    boolean allowEnums;

    /** Switch: should we recognize foreach?
     */
    boolean allowForeach;

    /** Switch: should we recognize foreach?
     */
    boolean allowStaticImport;

    /** Switch: should we recognize annotations?
     */
    boolean allowAnnotations;

    /** Switch: should we recognize try-with-resources?
     */
    boolean allowTWR;

    /** Switch: should we fold strings?
     */
    boolean allowStringFolding;

    /** Switch: should we recognize lambda expressions?
     */
    boolean allowLambda;

    /** Switch: should we allow method/constructor references?
     */
    boolean allowMethodReferences;

    /** Switch: should we keep docComments?
     */
    boolean keepDocComments;

    /** Switch: should we keep line table?
     */
    boolean keepLineMap;

    /** When terms are parsed, the mode determines which is expected:
     *     mode = EXPR        : an expression
     *     mode = TYPE        : a type
     *     mode = NOPARAMS    : no parameters allowed for type
     *     mode = TYPEARG     : type argument
     */
    static final int EXPR = 0x1;
    static final int TYPE = 0x2;
    static final int NOPARAMS = 0x4;
    static final int TYPEARG = 0x8;
    static final int DIAMOND = 0x10;

    /** The current mode.
     */
    private int mode = 0;

    /** The mode of the term that was parsed last.
     */
    private int lastmode = 0;

    /* ---------- token management -------------- */

    protected Token token;

    protected void nextToken() {
        S.nextToken();
        token = S.token();
    }

    protected boolean peekToken(TokenKind tk) {
        return S.token(1).kind == tk;
    }

    protected boolean peekToken(TokenKind tk1, TokenKind tk2) {
        return S.token(1).kind == tk1 &&
                S.token(2).kind == tk2;
    }

    protected boolean peekToken(TokenKind tk1, TokenKind tk2, TokenKind tk3) {
        return S.token(1).kind == tk1 &&
                S.token(2).kind == tk2 &&
                S.token(3).kind == tk3;
    }

    protected boolean peekToken(TokenKind... kinds) {
        for (int lookahead = 0 ; lookahead < kinds.length ; lookahead++) {
            if (S.token(lookahead + 1).kind != kinds[lookahead]) {
                return false;
            }
        }
        return true;
    }

    /* ---------- error recovery -------------- */

    private JCErroneous errorTree;

    /** Skip forward until a suitable stop token is found.
     */
    private void skip(boolean stopAtImport, boolean stopAtMemberDecl, boolean stopAtIdentifier, boolean stopAtStatement) {
         while (true) {
             switch (token.kind) {
                case SEMI:
                    nextToken();
                    return;
                case PUBLIC:
                case FINAL:
                case ABSTRACT:
                case MONKEYS_AT:
                case EOF:
                case CLASS:
                case INTERFACE:
                case ENUM:
                    return;
                case IMPORT:
                    if (stopAtImport)
                        return;
                    break;
                case LBRACE:
                case RBRACE:
                case PRIVATE:
                case PROTECTED:
                case STATIC:
                case TRANSIENT:
                case NATIVE:
                case VOLATILE:
                case SYNCHRONIZED:
                case STRICTFP:
                case LT:
                case BYTE:
                case SHORT:
                case CHAR:
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE:
                case BOOLEAN:
                case VOID:
                    if (stopAtMemberDecl)
                        return;
                    break;
                case IDENTIFIER:
                   if (stopAtIdentifier)
                        return;
                    break;
                case CASE:
                case DEFAULT:
                case IF:
                case FOR:
                case WHILE:
                case DO:
                case TRY:
                case SWITCH:
                case RETURN:
                case THROW:
                case BREAK:
                case CONTINUE:
                case ELSE:
                case FINALLY:
                case CATCH:
                    if (stopAtStatement)
                        return;
                    break;
            }
            nextToken();
        }
    }

    private JCErroneous syntaxError(int pos, String key, TokenKind... args) {
        return syntaxError(pos, List.<JCTree>nil(), key, args);
    }

    private JCErroneous syntaxError(int pos, List<JCTree> errs, String key, TokenKind... args) {
        setErrorEndPos(pos);
        JCErroneous err = F.at(pos).Erroneous(errs);
        reportSyntaxError(err, key, (Object[])args);
        if (errs != null) {
            JCTree last = errs.last();
            if (last != null)
                storeEnd(last, pos);
        }
        return toP(err);
    }

    private int errorPos = Position.NOPOS;

    /**
     * Report a syntax using the given the position parameter and arguments,
     * unless one was already reported at the same position.
     */
    private void reportSyntaxError(int pos, String key, Object... args) {
        JCDiagnostic.DiagnosticPosition diag = new JCDiagnostic.SimpleDiagnosticPosition(pos);
        reportSyntaxError(diag, key, args);
    }

    /**
     * Report a syntax error using the given DiagnosticPosition object and
     * arguments, unless one was already reported at the same position.
     */
    private void reportSyntaxError(JCDiagnostic.DiagnosticPosition diagPos, String key, Object... args) {
        int pos = diagPos.getPreferredPosition();
        if (pos > S.errPos() || pos == Position.NOPOS) {
            if (token.kind == EOF) {
                error(diagPos, "premature.eof");
            } else {
                error(diagPos, key, args);
            }
        }
        S.errPos(pos);
        if (token.pos == errorPos)
            nextToken(); // guarantee progress
        errorPos = token.pos;
    }


    /** Generate a syntax error at current position unless one was already
     *  reported at the same position.
     */
    private JCErroneous syntaxError(String key) {
        return syntaxError(token.pos, key);
    }

    /** Generate a syntax error at current position unless one was
     *  already reported at the same position.
     */
    private JCErroneous syntaxError(String key, TokenKind arg) {
        return syntaxError(token.pos, key, arg);
    }

    /** If next input token matches given token, skip it, otherwise report
     *  an error.
     */
    public void accept(TokenKind tk) {
        if (token.kind == tk) {
            nextToken();
        } else {
            setErrorEndPos(token.pos);
            reportSyntaxError(S.prevToken().endPos, "expected", tk);
        }
    }

    /** Report an illegal start of expression/type error at given position.
     */
    JCExpression illegal(int pos) {
        setErrorEndPos(pos);
        if ((mode & EXPR) != 0)
            return syntaxError(pos, "illegal.start.of.expr");
        else
            return syntaxError(pos, "illegal.start.of.type");

    }

    /** Report an illegal start of expression/type error at current position.
     */
    JCExpression illegal() {
        return illegal(token.pos);
    }

    /** Diagnose a modifier flag from the set, if any. */
    void checkNoMods(long mods) {
        if (mods != 0) {
            long lowestMod = mods & -mods;
            error(token.pos, "mod.not.allowed.here",
                      Flags.asFlagSet(lowestMod));
        }
    }

/* ---------- doc comments --------- */

    /** A hashtable to store all documentation comments
     *  indexed by the tree nodes they refer to.
     *  defined only if option flag keepDocComment is set.
     */
    private final Map<JCTree, String> docComments;

    /** Make an entry into docComments hashtable,
     *  provided flag keepDocComments is set and given doc comment is non-null.
     *  @param tree   The tree to be used as index in the hashtable
     *  @param dc     The doc comment to associate with the tree, or null.
     */
    void attach(JCTree tree, String dc) {
        if (keepDocComments && dc != null) {
//          System.out.println("doc comment = ");System.out.println(dc);//DEBUG
            docComments.put(tree, dc);
        }
    }

/* -------- source positions ------- */

    private void setErrorEndPos(int errPos) {
        endPosTable.setErrorEndPos(errPos);
    }

    private void storeEnd(JCTree tree, int endpos) {
        endPosTable.storeEnd(tree, endpos);
    }

    private <T extends JCTree> T to(T t) {
        return endPosTable.to(t);
    }

    private <T extends JCTree> T toP(T t) {
        return endPosTable.toP(t);
    }

    /** Get the start position for a tree node.  The start position is
     * defined to be the position of the first character of the first
     * token of the node's source text.
     * @param tree  The tree node
     */
    public int getStartPos(JCTree tree) {
        return TreeInfo.getStartPos(tree);
    }

    /**
     * Get the end position for a tree node.  The end position is
     * defined to be the position of the last character of the last
     * token of the node's source text.  Returns Position.NOPOS if end
     * positions are not generated or the position is otherwise not
     * found.
     * @param tree  The tree node
     */
    public int getEndPos(JCTree tree) {
        return endPosTable.getEndPos(tree);
    }



/* ---------- parsing -------------- */

    /**
     * Ident = IDENTIFIER
     */
    Name ident() {
        if (token.kind == IDENTIFIER) {
            Name name = token.name();
            nextToken();
            return name;
        } else if (token.kind == ASSERT) {
            if (allowAsserts) {
                error(token.pos, "assert.as.identifier");
                nextToken();
                return names.error;
            } else {
                warning(token.pos, "assert.as.identifier");
                Name name = token.name();
                nextToken();
                return name;
            }
        } else if (token.kind == ENUM) {
            if (allowEnums) {
                error(token.pos, "enum.as.identifier");
                nextToken();
                return names.error;
            } else {
                warning(token.pos, "enum.as.identifier");
                Name name = token.name();
                nextToken();
                return name;
            }
        } else {
            accept(IDENTIFIER);
            return names.error;
        }
}

    /**
     * Qualident = Ident { DOT Ident }
     */
    public JCExpression qualident() {
        JCExpression t = toP(F.at(token.pos).Ident(ident()));
        while (token.kind == DOT) {
            int pos = token.pos;
            nextToken();
            t = toP(F.at(pos).Select(t, ident()));
        }
        return t;
    }

    JCExpression literal(Name prefix) {
        return literal(prefix, token.pos);
    }

    /**
     * Literal =
     *     INTLITERAL
     *   | LONGLITERAL
     *   | FLOATLITERAL
     *   | DOUBLELITERAL
     *   | CHARLITERAL
     *   | STRINGLITERAL
     *   | TRUE
     *   | FALSE
     *   | NULL
     */
    JCExpression literal(Name prefix, int pos) {
        JCExpression t = errorTree;
        switch (token.kind) {
        case INTLITERAL:
            try {
                t = F.at(pos).Literal(
                    TypeTags.INT,
                    Convert.string2int(strval(prefix), token.radix()));
            } catch (NumberFormatException ex) {
                error(token.pos, "int.number.too.large", strval(prefix));
            }
            break;
        case LONGLITERAL:
            try {
                t = F.at(pos).Literal(
                    TypeTags.LONG,
                    new Long(Convert.string2long(strval(prefix), token.radix())));
            } catch (NumberFormatException ex) {
                error(token.pos, "int.number.too.large", strval(prefix));
            }
            break;
        case FLOATLITERAL: {
            String proper = token.radix() == 16 ?
                    ("0x"+ token.stringVal()) :
                    token.stringVal();
            Float n;
            try {
                n = Float.valueOf(proper);
            } catch (NumberFormatException ex) {
                // error already reported in scanner
                n = Float.NaN;
            }
            if (n.floatValue() == 0.0f && !isZero(proper))
                error(token.pos, "fp.number.too.small");
            else if (n.floatValue() == Float.POSITIVE_INFINITY)
                error(token.pos, "fp.number.too.large");
            else
                t = F.at(pos).Literal(TypeTags.FLOAT, n);
            break;
        }
        case DOUBLELITERAL: {
            String proper = token.radix() == 16 ?
                    ("0x"+ token.stringVal()) :
                    token.stringVal();
            Double n;
            try {
                n = Double.valueOf(proper);
            } catch (NumberFormatException ex) {
                // error already reported in scanner
                n = Double.NaN;
            }
            if (n.doubleValue() == 0.0d && !isZero(proper))
                error(token.pos, "fp.number.too.small");
            else if (n.doubleValue() == Double.POSITIVE_INFINITY)
                error(token.pos, "fp.number.too.large");
            else
                t = F.at(pos).Literal(TypeTags.DOUBLE, n);
            break;
        }
        case CHARLITERAL:
            t = F.at(pos).Literal(
                TypeTags.CHAR,
                token.stringVal().charAt(0) + 0);
            break;
        case STRINGLITERAL:
            t = F.at(pos).Literal(
                TypeTags.CLASS,
                token.stringVal());
            break;
        case TRUE: case FALSE:
            t = F.at(pos).Literal(
                TypeTags.BOOLEAN,
                (token.kind == TRUE ? 1 : 0));
            break;
        case NULL:
            t = F.at(pos).Literal(
                TypeTags.BOT,
                null);
            break;
        default:
            Assert.error();
        }
        if (t == errorTree)
            t = F.at(pos).Erroneous();
        storeEnd(t, token.endPos);
        nextToken();
        return t;
    }
//where
        boolean isZero(String s) {
            char[] cs = s.toCharArray();
            int base = ((cs.length > 1 && Character.toLowerCase(cs[1]) == 'x') ? 16 : 10);
            int i = ((base==16) ? 2 : 0);
            while (i < cs.length && (cs[i] == '0' || cs[i] == '.')) i++;
            return !(i < cs.length && (Character.digit(cs[i], base) > 0));
        }

        String strval(Name prefix) {
            String s = token.stringVal();
            return prefix.isEmpty() ? s : prefix + s;
        }

    /** terms can be either expressions or types.
     */
    public JCExpression parseExpression() {
        return term(EXPR);
    }

    public JCExpression parseType() {
        return term(TYPE);
    }

    JCExpression term(int newmode) {
        int prevmode = mode;
        mode = newmode;
        JCExpression t = term();
        lastmode = mode;
        mode = prevmode;
        return t;
    }

    /**
     *  Expression = Expression1 [ExpressionRest]
     *  ExpressionRest = [AssignmentOperator Expression1]
     *  AssignmentOperator = "=" | "+=" | "-=" | "*=" | "/=" |
     *                       "&=" | "|=" | "^=" |
     *                       "%=" | "<<=" | ">>=" | ">>>="
     *  Type = Type1
     *  TypeNoParams = TypeNoParams1
     *  StatementExpression = Expression
     *  ConstantExpression = Expression
     */
    JCExpression term() {
        JCExpression t = term1();
        if ((mode & EXPR) != 0 &&
            token.kind == EQ || PLUSEQ.compareTo(token.kind) <= 0 && token.kind.compareTo(GTGTGTEQ) <= 0)
            return termRest(t);
        else
            return t;
    }

    JCExpression termRest(JCExpression t) {
        switch (token.kind) {
        case EQ: {
            int pos = token.pos;
            nextToken();
            mode = EXPR;
            JCExpression t1 = term();
            return toP(F.at(pos).Assign(t, t1));
        }
        case PLUSEQ:
        case SUBEQ:
        case STAREQ:
        case SLASHEQ:
        case PERCENTEQ:
        case AMPEQ:
        case BAREQ:
        case CARETEQ:
        case LTLTEQ:
        case GTGTEQ:
        case GTGTGTEQ:
            int pos = token.pos;
            TokenKind tk = token.kind;
            nextToken();
            mode = EXPR;
            JCExpression t1 = term();
            return F.at(pos).Assignop(optag(tk), t, t1);
        default:
            return t;
        }
    }

    /** Expression1   = Expression2 [Expression1Rest]
     *  Type1         = Type2
     *  TypeNoParams1 = TypeNoParams2
     */
    JCExpression term1() {
        JCExpression t = term2();
        if ((mode & EXPR) != 0 && token.kind == QUES) {
            mode = EXPR;
            return term1Rest(t);
        } else {
            return t;
        }
    }

    /** Expression1Rest = ["?" Expression ":" Expression1]
     */
    JCExpression term1Rest(JCExpression t) {
        if (token.kind == QUES) {
            int pos = token.pos;
            nextToken();
            JCExpression t1 = term();
            accept(COLON);
            JCExpression t2 = term1();
            return F.at(pos).Conditional(t, t1, t2);
        } else {
            return t;
        }
    }

    /** Expression2   = Expression3 [Expression2Rest]
     *  Type2         = Type3
     *  TypeNoParams2 = TypeNoParams3
     */
    JCExpression term2() {
        JCExpression t = term3();
        if ((mode & EXPR) != 0 && prec(token.kind) >= TreeInfo.orPrec) {
            mode = EXPR;
            return term2Rest(t, TreeInfo.orPrec);
        } else {
            return t;
        }
    }

    /*  Expression2Rest = {infixop Expression3}
     *                  | Expression3 instanceof Type
     *  infixop         = "||"
     *                  | "&&"
     *                  | "|"
     *                  | "^"
     *                  | "&"
     *                  | "==" | "!="
     *                  | "<" | ">" | "<=" | ">="
     *                  | "<<" | ">>" | ">>>"
     *                  | "+" | "-"
     *                  | "*" | "/" | "%"
     */
    JCExpression term2Rest(JCExpression t, int minprec) {
        List<JCExpression[]> savedOd = odStackSupply.elems;
        JCExpression[] odStack = newOdStack();
        List<Token[]> savedOp = opStackSupply.elems;
        Token[] opStack = newOpStack();

        // optimization, was odStack = new Tree[...]; opStack = new Tree[...];
        int top = 0;
        odStack[0] = t;
        int startPos = token.pos;
        Token topOp = Tokens.DUMMY;
        while (prec(token.kind) >= minprec) {
            opStack[top] = topOp;
            top++;
            topOp = token;
            nextToken();
            odStack[top] = (topOp.kind == INSTANCEOF) ? parseType() : term3();
            while (top > 0 && prec(topOp.kind) >= prec(token.kind)) {
                odStack[top-1] = makeOp(topOp.pos, topOp.kind, odStack[top-1],
                                        odStack[top]);
                top--;
                topOp = opStack[top];
            }
        }
        Assert.check(top == 0);
        t = odStack[0];

        if (t.hasTag(JCTree.Tag.PLUS)) {
            StringBuffer buf = foldStrings(t);
            if (buf != null) {
                t = toP(F.at(startPos).Literal(TypeTags.CLASS, buf.toString()));
            }
        }

        odStackSupply.elems = savedOd; // optimization
        opStackSupply.elems = savedOp; // optimization
        return t;
    }
//where
        /** Construct a binary or type test node.
         */
        private JCExpression makeOp(int pos,
                                    TokenKind topOp,
                                    JCExpression od1,
                                    JCExpression od2)
        {
            if (topOp == INSTANCEOF) {
                return F.at(pos).TypeTest(od1, od2);
            } else {
                return F.at(pos).Binary(optag(topOp), od1, od2);
            }
        }
        /** If tree is a concatenation of string literals, replace it
         *  by a single literal representing the concatenated string.
         */
        protected StringBuffer foldStrings(JCTree tree) {
            if (!allowStringFolding)
                return null;
            List<String> buf = List.nil();
            while (true) {
                if (tree.hasTag(LITERAL)) {
                    JCLiteral lit = (JCLiteral) tree;
                    if (lit.typetag == TypeTags.CLASS) {
                        StringBuffer sbuf =
                            new StringBuffer((String)lit.value);
                        while (buf.nonEmpty()) {
                            sbuf.append(buf.head);
                            buf = buf.tail;
                        }
                        return sbuf;
                    }
                } else if (tree.hasTag(JCTree.Tag.PLUS)) {
                    JCBinary op = (JCBinary)tree;
                    if (op.rhs.hasTag(LITERAL)) {
                        JCLiteral lit = (JCLiteral) op.rhs;
                        if (lit.typetag == TypeTags.CLASS) {
                            buf = buf.prepend((String) lit.value);
                            tree = op.lhs;
                            continue;
                        }
                    }
                }
                return null;
            }
        }

        /** optimization: To save allocating a new operand/operator stack
         *  for every binary operation, we use supplys.
         */
        ListBuffer<JCExpression[]> odStackSupply = new ListBuffer<JCExpression[]>();
        ListBuffer<Token[]> opStackSupply = new ListBuffer<Token[]>();

        private JCExpression[] newOdStack() {
            if (odStackSupply.elems == odStackSupply.last)
                odStackSupply.append(new JCExpression[infixPrecedenceLevels + 1]);
            JCExpression[] odStack = odStackSupply.elems.head;
            odStackSupply.elems = odStackSupply.elems.tail;
            return odStack;
        }

        private Token[] newOpStack() {
            if (opStackSupply.elems == opStackSupply.last)
                opStackSupply.append(new Token[infixPrecedenceLevels + 1]);
            Token[] opStack = opStackSupply.elems.head;
            opStackSupply.elems = opStackSupply.elems.tail;
            return opStack;
        }

    /** Expression3    = PrefixOp Expression3
     *                 | "(" Expr | TypeNoParams ")" Expression3
     *                 | Primary {Selector} {PostfixOp}
     *  Primary        = "(" Expression ")"
     *                 | Literal
     *                 | [TypeArguments] THIS [Arguments]
     *                 | [TypeArguments] SUPER SuperSuffix
     *                 | NEW [TypeArguments] Creator
     *                 | "(" Arguments ")" "->" ( Expression | Block )
     *                 | Ident "->" ( Expression | Block )
     *                 | Ident { "." Ident }
     *                 | Expression3 MemberReferenceSuffix
     *                   [ "[" ( "]" BracketsOpt "." CLASS | Expression "]" )
     *                   | Arguments
     *                   | "." ( CLASS | THIS | [TypeArguments] SUPER Arguments | NEW [TypeArguments] InnerCreator )
     *                   ]
     *                 | BasicType BracketsOpt "." CLASS
     *  PrefixOp       = "++" | "--" | "!" | "~" | "+" | "-"
     *  PostfixOp      = "++" | "--"
     *  Type3          = Ident { "." Ident } [TypeArguments] {TypeSelector} BracketsOpt
     *                 | BasicType
     *  TypeNoParams3  = Ident { "." Ident } BracketsOpt
     *  Selector       = "." [TypeArguments] Ident [Arguments]
     *                 | "." THIS
     *                 | "." [TypeArguments] SUPER SuperSuffix
     *                 | "." NEW [TypeArguments] InnerCreator
     *                 | "[" Expression "]"
     *  TypeSelector   = "." Ident [TypeArguments]
     *  SuperSuffix    = Arguments | "." Ident [Arguments]
     */
    protected JCExpression term3() {
        int pos = token.pos;
        JCExpression t;
        List<JCExpression> typeArgs = typeArgumentsOpt(EXPR);
        switch (token.kind) {
        case QUES:
            if ((mode & TYPE) != 0 && (mode & (TYPEARG|NOPARAMS)) == TYPEARG) {
                mode = TYPE;
                return typeArgument();
            } else
                return illegal();
        case PLUSPLUS: case SUBSUB: case BANG: case TILDE: case PLUS: case SUB:
            if (typeArgs == null && (mode & EXPR) != 0) {
                TokenKind tk = token.kind;
                nextToken();
                mode = EXPR;
                if (tk == SUB &&
                    (token.kind == INTLITERAL || token.kind == LONGLITERAL) &&
                    token.radix() == 10) {
                    mode = EXPR;
                    t = literal(names.hyphen, pos);
                } else {
                    t = term3();
                    return F.at(pos).Unary(unoptag(tk), t);
                }
            } else return illegal();
            break;
        case LPAREN:
            if (typeArgs == null && (mode & EXPR) != 0) {
                if (peekToken(FINAL) ||
                        peekToken(RPAREN) ||
                        peekToken(IDENTIFIER, COMMA) ||
                        peekToken(IDENTIFIER, RPAREN, ARROW)) {
                    //implicit n-ary lambda
                    t = lambdaExpressionOrStatement(true, peekToken(FINAL), pos);
                    break;
                } else {
                    nextToken();
                    mode = EXPR | TYPE | NOPARAMS;
                    t = term3();
                    if ((mode & TYPE) != 0 && token.kind == LT) {
                        // Could be a cast to a parameterized type
                        JCTree.Tag op = JCTree.Tag.LT;
                        int pos1 = token.pos;
                        nextToken();
                        mode &= (EXPR | TYPE);
                        mode |= TYPEARG;
                        JCExpression t1 = term3();
                        if ((mode & TYPE) != 0 &&
                            (token.kind == COMMA || token.kind == GT)) {
                            mode = TYPE;
                            ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
                            args.append(t1);
                            while (token.kind == COMMA) {
                                nextToken();
                                args.append(typeArgument());
                            }
                            accept(GT);
                            t = toP(F.at(pos1).TypeApply(t, args.toList()));
                            checkGenerics();
                            mode = EXPR | TYPE; //could be a lambda or a method ref or a cast to a type
                            t = term3Rest(t, typeArgs);
                            if (token.kind == IDENTIFIER || token.kind == ELLIPSIS) {
                                //explicit lambda (w/ generic type)
                                mode = EXPR;
                                JCModifiers mods = F.at(token.pos).Modifiers(Flags.PARAMETER);
                                if (token.kind == ELLIPSIS) {
                                    mods.flags = Flags.VARARGS;
                                    t = to(F.at(token.pos).TypeArray(t));
                                    nextToken();
                                }
                                t = lambdaExpressionOrStatement(variableDeclaratorId(mods, t), pos);
                                break;
                            }
                        } else if ((mode & EXPR) != 0) {
                            mode = EXPR;
                            JCExpression e = term2Rest(t1, TreeInfo.shiftPrec);
                            t = F.at(pos1).Binary(op, t, e);
                            t = termRest(term1Rest(term2Rest(t, TreeInfo.orPrec)));
                        } else {
                            accept(GT);
                        }
                    } else if ((mode & TYPE) != 0 &&
                            (token.kind == IDENTIFIER || token.kind == ELLIPSIS)) {
                        //explicit lambda (w/ non-generic type)
                        mode = EXPR;
                        JCModifiers mods = F.at(token.pos).Modifiers(Flags.PARAMETER);
                        if (token.kind == ELLIPSIS) {
                            mods.flags = Flags.VARARGS;
                            t = to(F.at(token.pos).TypeArray(t));
                            nextToken();
                        }
                        t = lambdaExpressionOrStatement(variableDeclaratorId(mods, t), pos);
                        break;
                    } else {
                        t = termRest(term1Rest(term2Rest(t, TreeInfo.orPrec)));
                    }
                }

                accept(RPAREN);
                lastmode = mode;
                mode = EXPR;
                if ((lastmode & EXPR) == 0) {
                    JCExpression t1 = term3();
                    return F.at(pos).TypeCast(t, t1);
                } else if ((lastmode & TYPE) != 0) {
                    switch (token.kind) {
                    /*case PLUSPLUS: case SUBSUB: */
                    case BANG: case TILDE:
                    case LPAREN: case THIS: case SUPER:
                    case INTLITERAL: case LONGLITERAL: case FLOATLITERAL:
                    case DOUBLELITERAL: case CHARLITERAL: case STRINGLITERAL:
                    case TRUE: case FALSE: case NULL:
                        case NEW: case IDENTIFIER: case ASSERT: case ENUM:
                    case BYTE: case SHORT: case CHAR: case INT:
                    case LONG: case FLOAT: case DOUBLE: case BOOLEAN: case VOID:
                        JCExpression t1 = term3();
                        return F.at(pos).TypeCast(t, t1);
                    }
                }
            } else {
                return illegal();
            }
            t = toP(F.at(pos).Parens(t));
            break;
        case THIS:
            if ((mode & EXPR) != 0) {
                mode = EXPR;
                t = to(F.at(pos).Ident(names._this));
                nextToken();
                if (typeArgs == null)
                    t = argumentsOpt(null, t);
                else
                    t = arguments(typeArgs, t);
                typeArgs = null;
            } else return illegal();
            break;
        case SUPER:
            if ((mode & EXPR) != 0) {
                mode = EXPR;
                t = to(F.at(pos).Ident(names._super));
                t = superSuffix(typeArgs, t);
                typeArgs = null;
            } else return illegal();
            break;
        case INTLITERAL: case LONGLITERAL: case FLOATLITERAL: case DOUBLELITERAL:
        case CHARLITERAL: case STRINGLITERAL:
        case TRUE: case FALSE: case NULL:
            if (typeArgs == null && (mode & EXPR) != 0) {
                mode = EXPR;
                t = literal(names.empty);
            } else return illegal();
            break;
        case NEW:
            if (typeArgs != null) return illegal();
            if ((mode & EXPR) != 0) {
                mode = EXPR;
                nextToken();
                if (token.kind == LT) typeArgs = typeArguments(false);
                t = creator(pos, typeArgs);
                typeArgs = null;
            } else return illegal();
            break;
        case IDENTIFIER: case ASSERT: case ENUM:
            if (typeArgs != null) return illegal();
            if ((mode & EXPR) != 0 && peekToken(ARROW)) {
                t = lambdaExpressionOrStatement(false, false, pos);
            } else {
                t = toP(F.at(token.pos).Ident(ident()));
                loop: while (true) {
                    pos = token.pos;
                    switch (token.kind) {
                    case LBRACKET:
                        nextToken();
                        if (token.kind == RBRACKET) {
                            nextToken();
                            t = bracketsOpt(t);
                            t = toP(F.at(pos).TypeArray(t));
                            t = bracketsSuffix(t);
                        } else {
                            if ((mode & EXPR) != 0) {
                                mode = EXPR;
                                JCExpression t1 = term();
                                t = to(F.at(pos).Indexed(t, t1));
                            }
                            accept(RBRACKET);
                        }
                        break loop;
                    case LPAREN:
                        if ((mode & EXPR) != 0) {
                            mode = EXPR;
                            t = arguments(typeArgs, t);
                            typeArgs = null;
                        }
                        break loop;
                    case DOT:
                        nextToken();
                        int oldmode = mode;
                        mode &= ~NOPARAMS;
                        typeArgs = typeArgumentsOpt(EXPR);
                        mode = oldmode;
                        if ((mode & EXPR) != 0) {
                            switch (token.kind) {
                            case CLASS:
                                if (typeArgs != null) return illegal();
                                mode = EXPR;
                                t = to(F.at(pos).Select(t, names._class));
                                nextToken();
                                break loop;
                            case THIS:
                                if (typeArgs != null) return illegal();
                                mode = EXPR;
                                t = to(F.at(pos).Select(t, names._this));
                                nextToken();
                                break loop;
                            case SUPER:
                                mode = EXPR;
                                t = to(F.at(pos).Select(t, names._super));
                                t = superSuffix(typeArgs, t);
                                typeArgs = null;
                                break loop;
                            case NEW:
                                if (typeArgs != null) return illegal();
                                mode = EXPR;
                                int pos1 = token.pos;
                                nextToken();
                                if (token.kind == LT) typeArgs = typeArguments(false);
                                t = innerCreator(pos1, typeArgs, t);
                                typeArgs = null;
                                break loop;
                            }
                        }
                        // typeArgs saved for next loop iteration.
                        t = toP(F.at(pos).Select(t, ident()));
                        break;
                    case LT:
                        if ((mode & TYPE) == 0 && isUnboundMemberRef()) {
                            //this is an unbound method reference whose qualifier
                            //is a generic type i.e. A<S>#m
                            int pos1 = token.pos;
                            accept(LT);
                            ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
                            args.append(typeArgument());
                            while (token.kind == COMMA) {
                                nextToken();
                                args.append(typeArgument());
                            }
                            accept(GT);
                            t = toP(F.at(pos1).TypeApply(t, args.toList()));
                            checkGenerics();
                            while (token.kind == DOT) {
                                nextToken();
                                mode = TYPE;
                                t = toP(F.at(token.pos).Select(t, ident()));
                                t = typeArgumentsOpt(t);
                            }
                            if (token.kind != HASH) {
                                //method reference expected here
                                t = illegal();
                            }
                            mode = EXPR;
                            return term3Rest(t, typeArgs);
                        }
                        break loop;
                    default:
                        break loop;
                    }
                }
            }
            if (typeArgs != null) illegal();
            t = typeArgumentsOpt(t);
            break;
        case BYTE: case SHORT: case CHAR: case INT: case LONG: case FLOAT:
        case DOUBLE: case BOOLEAN:
            if (typeArgs != null) illegal();
            t = bracketsSuffix(bracketsOpt(basicType()));
            break;
        case VOID:
            if (typeArgs != null) illegal();
            if ((mode & EXPR) != 0) {
                nextToken();
                if (token.kind == DOT) {
                    JCPrimitiveTypeTree ti = toP(F.at(pos).TypeIdent(TypeTags.VOID));
                    t = bracketsSuffix(ti);
                } else {
                    return illegal(pos);
                }
            } else {
                // Support the corner case of myMethodHandle.<void>invoke() by passing
                // a void type (like other primitive types) to the next phase.
                // The error will be reported in Attr.attribTypes or Attr.visitApply.
                JCPrimitiveTypeTree ti = to(F.at(pos).TypeIdent(TypeTags.VOID));
                nextToken();
                return ti;
                //return illegal();
            }
            break;
        default:
            return illegal();
        }
        return term3Rest(t, typeArgs);
    }

    JCExpression term3Rest(JCExpression t, List<JCExpression> typeArgs) {
        if (typeArgs != null) illegal();
        while (true) {
            int pos1 = token.pos;
            if (token.kind == LBRACKET) {
                nextToken();
                if ((mode & TYPE) != 0) {
                    int oldmode = mode;
                    mode = TYPE;
                    if (token.kind == RBRACKET) {
                        nextToken();
                        t = bracketsOpt(t);
                        t = toP(F.at(pos1).TypeArray(t));
                        return t;
                    }
                    mode = oldmode;
                }
                if ((mode & EXPR) != 0) {
                    mode = EXPR;
                    JCExpression t1 = term();
                    t = to(F.at(pos1).Indexed(t, t1));
                }
                accept(RBRACKET);
            } else if (token.kind == DOT) {
                nextToken();
                typeArgs = typeArgumentsOpt(EXPR);
                if (token.kind == SUPER && (mode & EXPR) != 0) {
                    mode = EXPR;
                    t = to(F.at(pos1).Select(t, names._super));
                    nextToken();
                    t = arguments(typeArgs, t);
                    typeArgs = null;
                } else if (token.kind == NEW && (mode & EXPR) != 0) {
                    if (typeArgs != null) return illegal();
                    mode = EXPR;
                    int pos2 = token.pos;
                    nextToken();
                    if (token.kind == LT) typeArgs = typeArguments(false);
                    t = innerCreator(pos2, typeArgs, t);
                    typeArgs = null;
                } else {
                    t = toP(F.at(pos1).Select(t, ident()));
                    t = argumentsOpt(typeArgs, typeArgumentsOpt(t));
                    typeArgs = null;
                }
            } else if ((mode & EXPR) != 0 && token.kind == HASH) {
                mode = EXPR;
                if (typeArgs != null) return illegal();
                accept(HASH);
                t = memberReferenceSuffix(pos1, t);
            } else {
                break;
            }
        }
        while ((token.kind == PLUSPLUS || token.kind == SUBSUB) && (mode & EXPR) != 0) {
            mode = EXPR;
            t = to(F.at(token.pos).Unary(
                  token.kind == PLUSPLUS ? POSTINC : POSTDEC, t));
            nextToken();
        }
        return toP(t);
    }

    /**
     * If we see an identifier followed by a '&lt;' it could be an unbound
     * method reference or a binary expression. To disambiguate, look for a
     * matching '&gt;' and see if the subsequent terminal is either '.' or '#'.
     */
    @SuppressWarnings("fallthrough")
    boolean isUnboundMemberRef() {
        int pos = 0, depth = 0;
        for (Token t = S.token(pos) ; ; t = S.token(++pos)) {
            switch (t.kind) {
                case IDENTIFIER: case QUES: case EXTENDS: case SUPER:
                case DOT: case RBRACKET: case LBRACKET: case COMMA:
                case BYTE: case SHORT: case INT: case LONG: case FLOAT:
                case DOUBLE: case BOOLEAN: case CHAR:
                    break;
                case LT:
                    depth++; break;
                case GTGTGT:
                    depth--;
                case GTGT:
                    depth--;
                case GT:
                    depth--;
                    if (depth == 0) {
                        return
                            S.token(pos + 1).kind == TokenKind.DOT ||
                            S.token(pos + 1).kind == TokenKind.HASH;
                    }
                    break;
                default:
                    return false;
            }
        }
    }

    JCExpression lambdaExpressionOrStatement(JCVariableDecl firstParam, int pos) {
        ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
        params.append(firstParam);
        JCVariableDecl lastParam = firstParam;
        while ((lastParam.mods.flags & Flags.VARARGS) == 0 && token.kind == COMMA) {
            nextToken();
            params.append(lastParam = formalParameter());
        }
        accept(RPAREN);
        return lambdaExpressionOrStatementRest(params.toList(), pos);
    }

    JCExpression lambdaExpressionOrStatement(boolean hasParens, boolean explicitParams, int pos) {
        List<JCVariableDecl> params = explicitParams ?
                formalParameters() :
                implicitParameters(hasParens);

        return lambdaExpressionOrStatementRest(params, pos);
    }

    JCExpression lambdaExpressionOrStatementRest(List<JCVariableDecl> args, int pos) {
        if (token.kind != ARROW) {
            //better error recovery
            return F.at(pos).Erroneous(args);
        }

        checkLambda();
        accept(ARROW);

        return token.kind == LBRACE ?
            lambdaStatement(args, pos, pos) :
            lambdaExpression(args, pos);
    }

    JCExpression lambdaStatement(List<JCVariableDecl> args, int pos, int pos2) {
        JCBlock block = block(pos2, 0);
        return toP(F.at(pos).Lambda(args, block));
    }

    JCExpression lambdaExpression(List<JCVariableDecl> args, int pos) {
        JCTree expr = parseExpression();
        return toP(F.at(pos).Lambda(args, expr));
    }

    /** SuperSuffix = Arguments | "." [TypeArguments] Ident [Arguments]
     */
    JCExpression superSuffix(List<JCExpression> typeArgs, JCExpression t) {
        nextToken();
        if (token.kind == LPAREN || typeArgs != null) {
            t = arguments(typeArgs, t);
        } else if (token.kind == HASH) {
            if (typeArgs != null) return illegal();
            t = memberReferenceSuffix(t);
        } else {
            int pos = token.pos;
            accept(DOT);
            typeArgs = (token.kind == LT) ? typeArguments(false) : null;
            t = toP(F.at(pos).Select(t, ident()));
            t = argumentsOpt(typeArgs, t);
        }
        return t;
    }

    /** BasicType = BYTE | SHORT | CHAR | INT | LONG | FLOAT | DOUBLE | BOOLEAN
     */
    JCPrimitiveTypeTree basicType() {
        JCPrimitiveTypeTree t = to(F.at(token.pos).TypeIdent(typetag(token.kind)));
        nextToken();
        return t;
    }

    /** ArgumentsOpt = [ Arguments ]
     */
    JCExpression argumentsOpt(List<JCExpression> typeArgs, JCExpression t) {
        if ((mode & EXPR) != 0 && token.kind == LPAREN || typeArgs != null) {
            mode = EXPR;
            return arguments(typeArgs, t);
        } else {
            return t;
        }
    }

    /** Arguments = "(" [Expression { COMMA Expression }] ")"
     */
    List<JCExpression> arguments() {
        ListBuffer<JCExpression> args = lb();
        if (token.kind == LPAREN) {
            nextToken();
            if (token.kind != RPAREN) {
                args.append(parseExpression());
                while (token.kind == COMMA) {
                    nextToken();
                    args.append(parseExpression());
                }
            }
            accept(RPAREN);
        } else {
            syntaxError(token.pos, "expected", LPAREN);
        }
        return args.toList();
    }

    JCMethodInvocation arguments(List<JCExpression> typeArgs, JCExpression t) {
        int pos = token.pos;
        List<JCExpression> args = arguments();
        return toP(F.at(pos).Apply(typeArgs, t, args));
    }

    /**  TypeArgumentsOpt = [ TypeArguments ]
     */
    JCExpression typeArgumentsOpt(JCExpression t) {
        if (token.kind == LT &&
            (mode & TYPE) != 0 &&
            (mode & NOPARAMS) == 0) {
            mode = TYPE;
            checkGenerics();
            return typeArguments(t, false);
        } else {
            return t;
        }
    }
    List<JCExpression> typeArgumentsOpt() {
        return typeArgumentsOpt(TYPE);
    }

    List<JCExpression> typeArgumentsOpt(int useMode) {
        if (token.kind == LT) {
            checkGenerics();
            if ((mode & useMode) == 0 ||
                (mode & NOPARAMS) != 0) {
                illegal();
            }
            mode = useMode;
            return typeArguments(false);
        }
        return null;
    }

    /**  TypeArguments  = "<" TypeArgument {"," TypeArgument} ">"
     */
    List<JCExpression> typeArguments(boolean diamondAllowed) {
        if (token.kind == LT) {
            nextToken();
            if (token.kind == GT && diamondAllowed) {
                checkDiamond();
                mode |= DIAMOND;
                nextToken();
                return List.nil();
            } else {
                ListBuffer<JCExpression> args = ListBuffer.lb();
                args.append(((mode & EXPR) == 0) ? typeArgument() : parseType());
                while (token.kind == COMMA) {
                    nextToken();
                    args.append(((mode & EXPR) == 0) ? typeArgument() : parseType());
                }
                switch (token.kind) {

                case GTGTGTEQ: case GTGTEQ: case GTEQ:
                case GTGTGT: case GTGT:
                    token = S.split();
                    break;
                case GT:
                    nextToken();
                    break;
                default:
                    args.append(syntaxError(token.pos, "expected", GT));
                    break;
                }
                return args.toList();
            }
        } else {
            return List.<JCExpression>of(syntaxError(token.pos, "expected", LT));
        }
    }

    /** TypeArgument = Type
     *               | "?"
     *               | "?" EXTENDS Type {"&" Type}
     *               | "?" SUPER Type
     */
    JCExpression typeArgument() {
        if (token.kind != QUES) return parseType();
        int pos = token.pos;
        nextToken();
        if (token.kind == EXTENDS) {
            TypeBoundKind t = to(F.at(pos).TypeBoundKind(BoundKind.EXTENDS));
            nextToken();
            JCExpression bound = parseType();
            return F.at(pos).Wildcard(t, bound);
        } else if (token.kind == SUPER) {
            TypeBoundKind t = to(F.at(pos).TypeBoundKind(BoundKind.SUPER));
            nextToken();
            JCExpression bound = parseType();
            return F.at(pos).Wildcard(t, bound);
        } else if (token.kind == IDENTIFIER) {
            //error recovery
            TypeBoundKind t = F.at(Position.NOPOS).TypeBoundKind(BoundKind.UNBOUND);
            JCExpression wc = toP(F.at(pos).Wildcard(t, null));
            JCIdent id = toP(F.at(token.pos).Ident(ident()));
            JCErroneous err = F.at(pos).Erroneous(List.<JCTree>of(wc, id));
            reportSyntaxError(err, "expected3", GT, EXTENDS, SUPER);
            return err;
        } else {
            TypeBoundKind t = toP(F.at(pos).TypeBoundKind(BoundKind.UNBOUND));
            return toP(F.at(pos).Wildcard(t, null));
        }
    }

    JCTypeApply typeArguments(JCExpression t, boolean diamondAllowed) {
        int pos = token.pos;
        List<JCExpression> args = typeArguments(diamondAllowed);
        return toP(F.at(pos).TypeApply(t, args));
    }

    /** BracketsOpt = {"[" "]"}
     */
    private JCExpression bracketsOpt(JCExpression t) {
        if (token.kind == LBRACKET) {
            int pos = token.pos;
            nextToken();
            t = bracketsOptCont(t, pos);
            F.at(pos);
        }
        return t;
    }

    private JCArrayTypeTree bracketsOptCont(JCExpression t, int pos) {
        accept(RBRACKET);
        t = bracketsOpt(t);
        return toP(F.at(pos).TypeArray(t));
    }

    /** BracketsSuffixExpr = "." CLASS
     *  BracketsSuffixType =
     */
    JCExpression bracketsSuffix(JCExpression t) {
        if ((mode & EXPR) != 0 && token.kind == DOT) {
            mode = EXPR;
            int pos = token.pos;
            nextToken();
            accept(CLASS);
            if (token.pos == endPosTable.errorEndPos) {
                // error recovery
                Name name = null;
                if (token.kind == IDENTIFIER) {
                    name = token.name();
                    nextToken();
                } else {
                    name = names.error;
                }
                t = F.at(pos).Erroneous(List.<JCTree>of(toP(F.at(pos).Select(t, name))));
            } else {
                t = toP(F.at(pos).Select(t, names._class));
            }
        } else if ((mode & TYPE) != 0) {
            mode = TYPE;
        } else {
            syntaxError(token.pos, "dot.class.expected");
        }
        return t;
    }

    /**
     * MemberReferenceSuffix = "#" [TypeArguments] Ident
     *                       | "#" [TypeArguments] "new"
     */
    JCExpression memberReferenceSuffix(JCExpression t) {
        int pos1 = token.pos;
        accept(HASH);
        return memberReferenceSuffix(pos1, t);
    }

    JCExpression memberReferenceSuffix(int pos1, JCExpression t) {
        checkMethodReferences();
        mode = EXPR;
        List<JCExpression> typeArgs = null;
        if (token.kind == LT) {
            typeArgs = typeArguments(false);
        }
        Name refName = null;
        ReferenceMode refMode = null;
        if (token.kind == NEW) {
            refMode = ReferenceMode.NEW;
            refName = names.init;
            nextToken();
        } else {
            refMode = ReferenceMode.INVOKE;
            refName = ident();
        }
        return toP(F.at(t.getStartPosition()).Reference(refMode, refName, t, typeArgs));
    }

    /** Creator = Qualident [TypeArguments] ( ArrayCreatorRest | ClassCreatorRest )
     */
    JCExpression creator(int newpos, List<JCExpression> typeArgs) {
        switch (token.kind) {
        case BYTE: case SHORT: case CHAR: case INT: case LONG: case FLOAT:
        case DOUBLE: case BOOLEAN:
            if (typeArgs == null)
                return arrayCreatorRest(newpos, basicType());
            break;
        default:
        }
        JCExpression t = qualident();
        int oldmode = mode;
        mode = TYPE;
        boolean diamondFound = false;
        int lastTypeargsPos = -1;
        if (token.kind == LT) {
            checkGenerics();
            lastTypeargsPos = token.pos;
            t = typeArguments(t, true);
            diamondFound = (mode & DIAMOND) != 0;
        }
        while (token.kind == DOT) {
            if (diamondFound) {
                //cannot select after a diamond
                illegal();
            }
            int pos = token.pos;
            nextToken();
            t = toP(F.at(pos).Select(t, ident()));
            if (token.kind == LT) {
                lastTypeargsPos = token.pos;
                checkGenerics();
                t = typeArguments(t, true);
                diamondFound = (mode & DIAMOND) != 0;
            }
        }
        mode = oldmode;
        if (token.kind == LBRACKET) {
            JCExpression e = arrayCreatorRest(newpos, t);
            if (diamondFound) {
                reportSyntaxError(lastTypeargsPos, "cannot.create.array.with.diamond");
                return toP(F.at(newpos).Erroneous(List.of(e)));
            }
            else if (typeArgs != null) {
                int pos = newpos;
                if (!typeArgs.isEmpty() && typeArgs.head.pos != Position.NOPOS) {
                    // note: this should always happen but we should
                    // not rely on this as the parser is continuously
                    // modified to improve error recovery.
                    pos = typeArgs.head.pos;
                }
                setErrorEndPos(S.prevToken().endPos);
                JCErroneous err = F.at(pos).Erroneous(typeArgs.prepend(e));
                reportSyntaxError(err, "cannot.create.array.with.type.arguments");
                return toP(err);
            }
            return e;
        } else if (token.kind == LPAREN) {
            return classCreatorRest(newpos, null, typeArgs, t);
        } else {
            setErrorEndPos(token.pos);
            reportSyntaxError(token.pos, "expected2", LPAREN, LBRACKET);
            t = toP(F.at(newpos).NewClass(null, typeArgs, t, List.<JCExpression>nil(), null));
            return toP(F.at(newpos).Erroneous(List.<JCTree>of(t)));
        }
    }

    /** InnerCreator = Ident [TypeArguments] ClassCreatorRest
     */
    JCExpression innerCreator(int newpos, List<JCExpression> typeArgs, JCExpression encl) {
        JCExpression t = toP(F.at(token.pos).Ident(ident()));
        if (token.kind == LT) {
            int oldmode = mode;
            checkGenerics();
            t = typeArguments(t, true);
            mode = oldmode;
        }
        return classCreatorRest(newpos, encl, typeArgs, t);
    }

    /** ArrayCreatorRest = "[" ( "]" BracketsOpt ArrayInitializer
     *                         | Expression "]" {"[" Expression "]"} BracketsOpt )
     */
    JCExpression arrayCreatorRest(int newpos, JCExpression elemtype) {
        accept(LBRACKET);
        if (token.kind == RBRACKET) {
            accept(RBRACKET);
            elemtype = bracketsOpt(elemtype);
            if (token.kind == LBRACE) {
                return arrayInitializer(newpos, elemtype);
            } else {
                JCExpression t = toP(F.at(newpos).NewArray(elemtype, List.<JCExpression>nil(), null));
                return syntaxError(token.pos, List.<JCTree>of(t), "array.dimension.missing");
            }
        } else {
            ListBuffer<JCExpression> dims = new ListBuffer<JCExpression>();
            dims.append(parseExpression());
            accept(RBRACKET);
            while (token.kind == LBRACKET) {
                int pos = token.pos;
                nextToken();
                if (token.kind == RBRACKET) {
                    elemtype = bracketsOptCont(elemtype, pos);
                } else {
                    dims.append(parseExpression());
                    accept(RBRACKET);
                }
            }
            return toP(F.at(newpos).NewArray(elemtype, dims.toList(), null));
        }
    }

    /** ClassCreatorRest = Arguments [ClassBody]
     */
    JCNewClass classCreatorRest(int newpos,
                                  JCExpression encl,
                                  List<JCExpression> typeArgs,
                                  JCExpression t)
    {
        List<JCExpression> args = arguments();
        JCClassDecl body = null;
        if (token.kind == LBRACE) {
            int pos = token.pos;
            List<JCTree> defs = classOrInterfaceBody(names.empty, false);
            JCModifiers mods = F.at(Position.NOPOS).Modifiers(0);
            body = toP(F.at(pos).AnonymousClassDef(mods, defs));
        }
        return toP(F.at(newpos).NewClass(encl, typeArgs, t, args, body));
    }

    /** ArrayInitializer = "{" [VariableInitializer {"," VariableInitializer}] [","] "}"
     */
    JCExpression arrayInitializer(int newpos, JCExpression t) {
        accept(LBRACE);
        ListBuffer<JCExpression> elems = new ListBuffer<JCExpression>();
        if (token.kind == COMMA) {
            nextToken();
        } else if (token.kind != RBRACE) {
            elems.append(variableInitializer());
            while (token.kind == COMMA) {
                nextToken();
                if (token.kind == RBRACE) break;
                elems.append(variableInitializer());
            }
        }
        accept(RBRACE);
        return toP(F.at(newpos).NewArray(t, List.<JCExpression>nil(), elems.toList()));
    }

    /** VariableInitializer = ArrayInitializer | Expression
     */
    public JCExpression variableInitializer() {
        return token.kind == LBRACE ? arrayInitializer(token.pos, null) : parseExpression();
    }

    /** ParExpression = "(" Expression ")"
     */
    JCExpression parExpression() {
        int pos = token.pos;
        accept(LPAREN);
        JCExpression t = parseExpression();
        accept(RPAREN);
        return toP(F.at(pos).Parens(t));
    }

    /** Block = "{" BlockStatements "}"
     */
    JCBlock block(int pos, long flags) {
        accept(LBRACE);
        List<JCStatement> stats = blockStatements();
        JCBlock t = F.at(pos).Block(flags, stats);
        while (token.kind == CASE || token.kind == DEFAULT) {
            syntaxError("orphaned", token.kind);
            switchBlockStatementGroups();
        }
        // the Block node has a field "endpos" for first char of last token, which is
        // usually but not necessarily the last char of the last token.
        t.endpos = token.pos;
        accept(RBRACE);
        return toP(t);
    }

    public JCBlock block() {
        return block(token.pos, 0);
    }

    /** BlockStatements = { BlockStatement }
     *  BlockStatement  = LocalVariableDeclarationStatement
     *                  | ClassOrInterfaceOrEnumDeclaration
     *                  | [Ident ":"] Statement
     *  LocalVariableDeclarationStatement
     *                  = { FINAL | '@' Annotation } Type VariableDeclarators ";"
     */
    @SuppressWarnings("fallthrough")
    List<JCStatement> blockStatements() {
        //todo: skip to anchor on error(?)
        ListBuffer<JCStatement> stats = new ListBuffer<JCStatement>();
        while (true) {
            List<JCStatement> stat = blockStatement();
            if (stat.isEmpty()) {
                return stats.toList();
            } else {
                if (token.pos <= endPosTable.errorEndPos) {
                    skip(false, true, true, true);
                }
                stats.addAll(stat);
            }
        }
    }

    /*
     * This method parses a statement treating it as a block, relaxing the
     * JLS restrictions, allows us to parse more faulty code, doing so
     * enables us to provide better and accurate diagnostics to the user.
     */
    JCStatement parseStatementAsBlock() {
        int pos = token.pos;
        List<JCStatement> stats = blockStatement();
        if (stats.isEmpty()) {
            JCErroneous e = F.at(pos).Erroneous();
            error(e, "illegal.start.of.stmt");
            return F.at(pos).Exec(e);
        } else {
            JCStatement first = stats.head;
            String error = null;
            switch (first.getTag()) {
            case CLASSDEF:
                error = "class.not.allowed";
                break;
            case VARDEF:
                error = "variable.not.allowed";
                break;
            }
            if (error != null) {
                error(first, error);
                List<JCBlock> blist = List.of(F.at(first.pos).Block(0, stats));
                return toP(F.at(pos).Exec(F.at(first.pos).Erroneous(blist)));
            }
            return first;
        }
    }

    @SuppressWarnings("fallthrough")
    List<JCStatement> blockStatement() {
        //todo: skip to anchor on error(?)
        int pos = token.pos;
        switch (token.kind) {
        case RBRACE: case CASE: case DEFAULT: case EOF:
            return List.nil();
        case LBRACE: case IF: case FOR: case WHILE: case DO: case TRY:
        case SWITCH: case SYNCHRONIZED: case RETURN: case THROW: case BREAK:
        case CONTINUE: case SEMI: case ELSE: case FINALLY: case CATCH:
            return List.of(parseStatement());
        case MONKEYS_AT:
        case FINAL: {
            String dc = token.comment(CommentStyle.JAVADOC);
            JCModifiers mods = modifiersOpt();
            if (token.kind == INTERFACE ||
                token.kind == CLASS ||
                allowEnums && token.kind == ENUM) {
                return List.of(classOrInterfaceOrEnumDeclaration(mods, dc));
            } else {
                JCExpression t = parseType();
                ListBuffer<JCStatement> stats =
                        variableDeclarators(mods, t, new ListBuffer<JCStatement>());
                // A "LocalVariableDeclarationStatement" subsumes the terminating semicolon
                storeEnd(stats.elems.last(), token.endPos);
                accept(SEMI);
                return stats.toList();
            }
        }
        case ABSTRACT: case STRICTFP: {
            String dc = token.comment(CommentStyle.JAVADOC);
            JCModifiers mods = modifiersOpt();
            return List.of(classOrInterfaceOrEnumDeclaration(mods, dc));
        }
        case INTERFACE:
        case CLASS:
            String dc = token.comment(CommentStyle.JAVADOC);
            return List.of(classOrInterfaceOrEnumDeclaration(modifiersOpt(), dc));
        case ENUM:
        case ASSERT:
            if (allowEnums && token.kind == ENUM) {
                error(token.pos, "local.enum");
                dc = token.comment(CommentStyle.JAVADOC);
                return List.of(classOrInterfaceOrEnumDeclaration(modifiersOpt(), dc));
            } else if (allowAsserts && token.kind == ASSERT) {
                return List.of(parseStatement());
            }
            /* fall through to default */
        default:
            Token prevToken = token;
            JCExpression t = term(EXPR | TYPE);
            if (token.kind == COLON && t.hasTag(IDENT)) {
                nextToken();
                JCStatement stat = parseStatement();
                return List.<JCStatement>of(F.at(pos).Labelled(prevToken.name(), stat));
            } else if ((lastmode & TYPE) != 0 &&
                       (token.kind == IDENTIFIER ||
                        token.kind == ASSERT ||
                        token.kind == ENUM)) {
                pos = token.pos;
                JCModifiers mods = F.at(Position.NOPOS).Modifiers(0);
                F.at(pos);
                ListBuffer<JCStatement> stats =
                        variableDeclarators(mods, t, new ListBuffer<JCStatement>());
                // A "LocalVariableDeclarationStatement" subsumes the terminating semicolon
                storeEnd(stats.elems.last(), token.endPos);
                accept(SEMI);
                return stats.toList();
            } else {
                // This Exec is an "ExpressionStatement"; it subsumes the terminating semicolon
                JCExpressionStatement expr = to(F.at(pos).Exec(checkExprStat(t)));
                accept(SEMI);
                return List.<JCStatement>of(expr);
            }
        }
    }

    /** Statement =
     *       Block
     *     | IF ParExpression Statement [ELSE Statement]
     *     | FOR "(" ForInitOpt ";" [Expression] ";" ForUpdateOpt ")" Statement
     *     | FOR "(" FormalParameter : Expression ")" Statement
     *     | WHILE ParExpression Statement
     *     | DO Statement WHILE ParExpression ";"
     *     | TRY Block ( Catches | [Catches] FinallyPart )
     *     | TRY "(" ResourceSpecification ";"opt ")" Block [Catches] [FinallyPart]
     *     | SWITCH ParExpression "{" SwitchBlockStatementGroups "}"
     *     | SYNCHRONIZED ParExpression Block
     *     | RETURN [Expression] ";"
     *     | THROW Expression ";"
     *     | BREAK [Ident] ";"
     *     | CONTINUE [Ident] ";"
     *     | ASSERT Expression [ ":" Expression ] ";"
     *     | ";"
     *     | ExpressionStatement
     *     | Ident ":" Statement
     */
    @SuppressWarnings("fallthrough")
    public JCStatement parseStatement() {
        int pos = token.pos;
        switch (token.kind) {
        case LBRACE:
            return block();
        case IF: {
            nextToken();
            JCExpression cond = parExpression();
            JCStatement thenpart = parseStatementAsBlock();
            JCStatement elsepart = null;
            if (token.kind == ELSE) {
                nextToken();
                elsepart = parseStatementAsBlock();
            }
            return F.at(pos).If(cond, thenpart, elsepart);
        }
        case FOR: {
            nextToken();
            accept(LPAREN);
            List<JCStatement> inits = token.kind == SEMI ? List.<JCStatement>nil() : forInit();
            if (inits.length() == 1 &&
                inits.head.hasTag(VARDEF) &&
                ((JCVariableDecl) inits.head).init == null &&
                token.kind == COLON) {
                checkForeach();
                JCVariableDecl var = (JCVariableDecl)inits.head;
                accept(COLON);
                JCExpression expr = parseExpression();
                accept(RPAREN);
                JCStatement body = parseStatementAsBlock();
                return F.at(pos).ForeachLoop(var, expr, body);
            } else {
                accept(SEMI);
                JCExpression cond = token.kind == SEMI ? null : parseExpression();
                accept(SEMI);
                List<JCExpressionStatement> steps = token.kind == RPAREN ? List.<JCExpressionStatement>nil() : forUpdate();
                accept(RPAREN);
                JCStatement body = parseStatementAsBlock();
                return F.at(pos).ForLoop(inits, cond, steps, body);
            }
        }
        case WHILE: {
            nextToken();
            JCExpression cond = parExpression();
            JCStatement body = parseStatementAsBlock();
            return F.at(pos).WhileLoop(cond, body);
        }
        case DO: {
            nextToken();
            JCStatement body = parseStatementAsBlock();
            accept(WHILE);
            JCExpression cond = parExpression();
            JCDoWhileLoop t = to(F.at(pos).DoLoop(body, cond));
            accept(SEMI);
            return t;
        }
        case TRY: {
            nextToken();
            List<JCTree> resources = List.<JCTree>nil();
            if (token.kind == LPAREN) {
                checkTryWithResources();
                nextToken();
                resources = resources();
                accept(RPAREN);
            }
            JCBlock body = block();
            ListBuffer<JCCatch> catchers = new ListBuffer<JCCatch>();
            JCBlock finalizer = null;
            if (token.kind == CATCH || token.kind == FINALLY) {
                while (token.kind == CATCH) catchers.append(catchClause());
                if (token.kind == FINALLY) {
                    nextToken();
                    finalizer = block();
                }
            } else {
                if (allowTWR) {
                    if (resources.isEmpty())
                        error(pos, "try.without.catch.finally.or.resource.decls");
                } else
                    error(pos, "try.without.catch.or.finally");
            }
            return F.at(pos).Try(resources, body, catchers.toList(), finalizer);
        }
        case SWITCH: {
            nextToken();
            JCExpression selector = parExpression();
            accept(LBRACE);
            List<JCCase> cases = switchBlockStatementGroups();
            JCSwitch t = to(F.at(pos).Switch(selector, cases));
            accept(RBRACE);
            return t;
        }
        case SYNCHRONIZED: {
            nextToken();
            JCExpression lock = parExpression();
            JCBlock body = block();
            return F.at(pos).Synchronized(lock, body);
        }
        case RETURN: {
            nextToken();
            JCExpression result = token.kind == SEMI ? null : parseExpression();
            JCReturn t = to(F.at(pos).Return(result));
            accept(SEMI);
            return t;
        }
        case THROW: {
            nextToken();
            JCExpression exc = parseExpression();
            JCThrow t = to(F.at(pos).Throw(exc));
            accept(SEMI);
            return t;
        }
        case BREAK: {
            nextToken();
            Name label = (token.kind == IDENTIFIER || token.kind == ASSERT || token.kind == ENUM) ? ident() : null;
            JCBreak t = to(F.at(pos).Break(label));
            accept(SEMI);
            return t;
        }
        case CONTINUE: {
            nextToken();
            Name label = (token.kind == IDENTIFIER || token.kind == ASSERT || token.kind == ENUM) ? ident() : null;
            JCContinue t =  to(F.at(pos).Continue(label));
            accept(SEMI);
            return t;
        }
        case SEMI:
            nextToken();
            return toP(F.at(pos).Skip());
        case ELSE:
            return toP(F.Exec(syntaxError("else.without.if")));
        case FINALLY:
            return toP(F.Exec(syntaxError("finally.without.try")));
        case CATCH:
            return toP(F.Exec(syntaxError("catch.without.try")));
        case ASSERT: {
            if (allowAsserts && token.kind == ASSERT) {
                nextToken();
                JCExpression assertion = parseExpression();
                JCExpression message = null;
                if (token.kind == COLON) {
                    nextToken();
                    message = parseExpression();
                }
                JCAssert t = to(F.at(pos).Assert(assertion, message));
                accept(SEMI);
                return t;
            }
            /* else fall through to default case */
        }
        case ENUM:
        default:
            Token prevToken = token;
            JCExpression expr = parseExpression();
            if (token.kind == COLON && expr.hasTag(IDENT)) {
                nextToken();
                JCStatement stat = parseStatement();
                return F.at(pos).Labelled(prevToken.name(), stat);
            } else {
                // This Exec is an "ExpressionStatement"; it subsumes the terminating semicolon
                JCExpressionStatement stat = to(F.at(pos).Exec(checkExprStat(expr)));
                accept(SEMI);
                return stat;
            }
        }
    }

    /** CatchClause     = CATCH "(" FormalParameter ")" Block
     */
    protected JCCatch catchClause() {
        int pos = token.pos;
        accept(CATCH);
        accept(LPAREN);
        JCModifiers mods = optFinal(Flags.PARAMETER);
        List<JCExpression> catchTypes = catchTypes();
        JCExpression paramType = catchTypes.size() > 1 ?
                toP(F.at(catchTypes.head.getStartPosition()).TypeUnion(catchTypes)) :
                catchTypes.head;
        JCVariableDecl formal = variableDeclaratorId(mods, paramType);
        accept(RPAREN);
        JCBlock body = block();
        return F.at(pos).Catch(formal, body);
    }

    List<JCExpression> catchTypes() {
        ListBuffer<JCExpression> catchTypes = ListBuffer.lb();
        catchTypes.add(parseType());
        while (token.kind == BAR) {
            checkMulticatch();
            nextToken();
            catchTypes.add(qualident());
        }
        return catchTypes.toList();
    }

    /** SwitchBlockStatementGroups = { SwitchBlockStatementGroup }
     *  SwitchBlockStatementGroup = SwitchLabel BlockStatements
     *  SwitchLabel = CASE ConstantExpression ":" | DEFAULT ":"
     */
    List<JCCase> switchBlockStatementGroups() {
        ListBuffer<JCCase> cases = new ListBuffer<JCCase>();
        while (true) {
            int pos = token.pos;
            switch (token.kind) {
            case CASE: {
                nextToken();
                JCExpression pat = parseExpression();
                accept(COLON);
                List<JCStatement> stats = blockStatements();
                JCCase c = F.at(pos).Case(pat, stats);
                if (stats.isEmpty())
                    storeEnd(c, S.prevToken().endPos);
                cases.append(c);
                break;
            }
            case DEFAULT: {
                nextToken();
                accept(COLON);
                List<JCStatement> stats = blockStatements();
                JCCase c = F.at(pos).Case(null, stats);
                if (stats.isEmpty())
                    storeEnd(c, S.prevToken().endPos);
                cases.append(c);
                break;
            }
            case RBRACE: case EOF:
                return cases.toList();
            default:
                nextToken(); // to ensure progress
                syntaxError(pos, "expected3",
                    CASE, DEFAULT, RBRACE);
            }
        }
    }

    /** MoreStatementExpressions = { COMMA StatementExpression }
     */
    <T extends ListBuffer<? super JCExpressionStatement>> T moreStatementExpressions(int pos,
                                                                    JCExpression first,
                                                                    T stats) {
        // This Exec is a "StatementExpression"; it subsumes no terminating token
        stats.append(toP(F.at(pos).Exec(checkExprStat(first))));
        while (token.kind == COMMA) {
            nextToken();
            pos = token.pos;
            JCExpression t = parseExpression();
            // This Exec is a "StatementExpression"; it subsumes no terminating token
            stats.append(toP(F.at(pos).Exec(checkExprStat(t))));
        }
        return stats;
    }

    /** ForInit = StatementExpression MoreStatementExpressions
     *           |  { FINAL | '@' Annotation } Type VariableDeclarators
     */
    List<JCStatement> forInit() {
        ListBuffer<JCStatement> stats = lb();
        int pos = token.pos;
        if (token.kind == FINAL || token.kind == MONKEYS_AT) {
            return variableDeclarators(optFinal(0), parseType(), stats).toList();
        } else {
            JCExpression t = term(EXPR | TYPE);
            if ((lastmode & TYPE) != 0 &&
                (token.kind == IDENTIFIER || token.kind == ASSERT || token.kind == ENUM))
                return variableDeclarators(modifiersOpt(), t, stats).toList();
            else
                return moreStatementExpressions(pos, t, stats).toList();
        }
    }

    /** ForUpdate = StatementExpression MoreStatementExpressions
     */
    List<JCExpressionStatement> forUpdate() {
        return moreStatementExpressions(token.pos,
                                        parseExpression(),
                                        new ListBuffer<JCExpressionStatement>()).toList();
    }

    /** AnnotationsOpt = { '@' Annotation }
     */
    List<JCAnnotation> annotationsOpt() {
        if (token.kind != MONKEYS_AT) return List.nil(); // optimization
        ListBuffer<JCAnnotation> buf = new ListBuffer<JCAnnotation>();
        while (token.kind == MONKEYS_AT) {
            int pos = token.pos;
            nextToken();
            buf.append(annotation(pos));
        }
        return buf.toList();
    }

    /** ModifiersOpt = { Modifier }
     *  Modifier = PUBLIC | PROTECTED | PRIVATE | STATIC | ABSTRACT | FINAL
     *           | NATIVE | SYNCHRONIZED | TRANSIENT | VOLATILE | "@"
     *           | "@" Annotation
     */
    JCModifiers modifiersOpt() {
        return modifiersOpt(null);
    }
    protected JCModifiers modifiersOpt(JCModifiers partial) {
        long flags;
        ListBuffer<JCAnnotation> annotations = new ListBuffer<JCAnnotation>();
        int pos;
        if (partial == null) {
            flags = 0;
            pos = token.pos;
        } else {
            flags = partial.flags;
            annotations.appendList(partial.annotations);
            pos = partial.pos;
        }
        if (token.deprecatedFlag()) {
            flags |= Flags.DEPRECATED;
        }
        int lastPos = Position.NOPOS;
    loop:
        while (true) {
            long flag;
            switch (token.kind) {
            case PRIVATE     : flag = Flags.PRIVATE; break;
            case PROTECTED   : flag = Flags.PROTECTED; break;
            case PUBLIC      : flag = Flags.PUBLIC; break;
            case STATIC      : flag = Flags.STATIC; break;
            case TRANSIENT   : flag = Flags.TRANSIENT; break;
            case FINAL       : flag = Flags.FINAL; break;
            case ABSTRACT    : flag = Flags.ABSTRACT; break;
            case NATIVE      : flag = Flags.NATIVE; break;
            case VOLATILE    : flag = Flags.VOLATILE; break;
            case SYNCHRONIZED: flag = Flags.SYNCHRONIZED; break;
            case STRICTFP    : flag = Flags.STRICTFP; break;
            case MONKEYS_AT  : flag = Flags.ANNOTATION; break;
            case ERROR       : flag = 0; nextToken(); break;
            default: break loop;
            }
            if ((flags & flag) != 0) error(token.pos, "repeated.modifier");
            lastPos = token.pos;
            nextToken();
            if (flag == Flags.ANNOTATION) {
                checkAnnotations();
                if (token.kind != INTERFACE) {
                    JCAnnotation ann = annotation(lastPos);
                    // if first modifier is an annotation, set pos to annotation's.
                    if (flags == 0 && annotations.isEmpty())
                        pos = ann.pos;
                    annotations.append(ann);
                    lastPos = ann.pos;
                    flag = 0;
                }
            }
            flags |= flag;
        }
        switch (token.kind) {
        case ENUM: flags |= Flags.ENUM; break;
        case INTERFACE: flags |= Flags.INTERFACE; break;
        default: break;
        }

        /* A modifiers tree with no modifier tokens or annotations
         * has no text position. */
        if ((flags & (Flags.ModifierFlags | Flags.ANNOTATION)) == 0 && annotations.isEmpty())
            pos = Position.NOPOS;

        JCModifiers mods = F.at(pos).Modifiers(flags, annotations.toList());
        if (pos != Position.NOPOS)
            storeEnd(mods, S.prevToken().endPos);
        return mods;
    }

    /** Annotation              = "@" Qualident [ "(" AnnotationFieldValues ")" ]
     * @param pos position of "@" token
     */
    JCAnnotation annotation(int pos) {
        // accept(AT); // AT consumed by caller
        checkAnnotations();
        JCTree ident = qualident();
        List<JCExpression> fieldValues = annotationFieldValuesOpt();
        JCAnnotation ann = F.at(pos).Annotation(ident, fieldValues);
        storeEnd(ann, S.prevToken().endPos);
        return ann;
    }

    List<JCExpression> annotationFieldValuesOpt() {
        return (token.kind == LPAREN) ? annotationFieldValues() : List.<JCExpression>nil();
    }

    /** AnnotationFieldValues   = "(" [ AnnotationFieldValue { "," AnnotationFieldValue } ] ")" */
    List<JCExpression> annotationFieldValues() {
        accept(LPAREN);
        ListBuffer<JCExpression> buf = new ListBuffer<JCExpression>();
        if (token.kind != RPAREN) {
            buf.append(annotationFieldValue());
            while (token.kind == COMMA) {
                nextToken();
                buf.append(annotationFieldValue());
            }
        }
        accept(RPAREN);
        return buf.toList();
    }

    /** AnnotationFieldValue    = AnnotationValue
     *                          | Identifier "=" AnnotationValue
     */
    JCExpression annotationFieldValue() {
        if (token.kind == IDENTIFIER) {
            mode = EXPR;
            JCExpression t1 = term1();
            if (t1.hasTag(IDENT) && token.kind == EQ) {
                int pos = token.pos;
                accept(EQ);
                JCExpression v = annotationValue();
                return toP(F.at(pos).Assign(t1, v));
            } else {
                return t1;
            }
        }
        return annotationValue();
    }

    /* AnnotationValue          = ConditionalExpression
     *                          | Annotation
     *                          | "{" [ AnnotationValue { "," AnnotationValue } ] [","] "}"
     */
    JCExpression annotationValue() {
        int pos;
        switch (token.kind) {
        case MONKEYS_AT:
            pos = token.pos;
            nextToken();
            return annotation(pos);
        case LBRACE:
            pos = token.pos;
            accept(LBRACE);
            ListBuffer<JCExpression> buf = new ListBuffer<JCExpression>();
            if (token.kind != RBRACE) {
                buf.append(annotationValue());
                while (token.kind == COMMA) {
                    nextToken();
                    if (token.kind == RBRACE) break;
                    buf.append(annotationValue());
                }
            }
            accept(RBRACE);
            return toP(F.at(pos).NewArray(null, List.<JCExpression>nil(), buf.toList()));
        default:
            mode = EXPR;
            return term1();
        }
    }

    /** VariableDeclarators = VariableDeclarator { "," VariableDeclarator }
     */
    public <T extends ListBuffer<? super JCVariableDecl>> T variableDeclarators(JCModifiers mods,
                                                                         JCExpression type,
                                                                         T vdefs)
    {
        return variableDeclaratorsRest(token.pos, mods, type, ident(), false, null, vdefs);
    }

    /** VariableDeclaratorsRest = VariableDeclaratorRest { "," VariableDeclarator }
     *  ConstantDeclaratorsRest = ConstantDeclaratorRest { "," ConstantDeclarator }
     *
     *  @param reqInit  Is an initializer always required?
     *  @param dc       The documentation comment for the variable declarations, or null.
     */
    <T extends ListBuffer<? super JCVariableDecl>> T variableDeclaratorsRest(int pos,
                                                                     JCModifiers mods,
                                                                     JCExpression type,
                                                                     Name name,
                                                                     boolean reqInit,
                                                                     String dc,
                                                                     T vdefs)
    {
        vdefs.append(variableDeclaratorRest(pos, mods, type, name, reqInit, dc));
        while (token.kind == COMMA) {
            // All but last of multiple declarators subsume a comma
            storeEnd((JCTree)vdefs.elems.last(), token.endPos);
            nextToken();
            vdefs.append(variableDeclarator(mods, type, reqInit, dc));
        }
        return vdefs;
    }

    /** VariableDeclarator = Ident VariableDeclaratorRest
     *  ConstantDeclarator = Ident ConstantDeclaratorRest
     */
    JCVariableDecl variableDeclarator(JCModifiers mods, JCExpression type, boolean reqInit, String dc) {
        return variableDeclaratorRest(token.pos, mods, type, ident(), reqInit, dc);
    }

    /** VariableDeclaratorRest = BracketsOpt ["=" VariableInitializer]
     *  ConstantDeclaratorRest = BracketsOpt "=" VariableInitializer
     *
     *  @param reqInit  Is an initializer always required?
     *  @param dc       The documentation comment for the variable declarations, or null.
     */
    JCVariableDecl variableDeclaratorRest(int pos, JCModifiers mods, JCExpression type, Name name,
                                  boolean reqInit, String dc) {
        type = bracketsOpt(type);
        JCExpression init = null;
        if (token.kind == EQ) {
            nextToken();
            init = variableInitializer();
        }
        else if (reqInit) syntaxError(token.pos, "expected", EQ);
        JCVariableDecl result =
            toP(F.at(pos).VarDef(mods, name, type, init));
        attach(result, dc);
        return result;
    }

    /** VariableDeclaratorId = Ident BracketsOpt
     */
    JCVariableDecl variableDeclaratorId(JCModifiers mods, JCExpression type) {
        int pos = token.pos;
        Name name = ident();
        if ((mods.flags & Flags.VARARGS) != 0 &&
                token.kind == LBRACKET) {
            log.error(token.pos, "varargs.and.old.array.syntax");
        }
        type = bracketsOpt(type);
        return toP(F.at(pos).VarDef(mods, name, type, null));
    }

    /** Resources = Resource { ";" Resources }
     */
    List<JCTree> resources() {
        ListBuffer<JCTree> defs = new ListBuffer<JCTree>();
        defs.append(resource());
        while (token.kind == SEMI) {
            // All but last of multiple declarators must subsume a semicolon
            storeEnd(defs.elems.last(), token.endPos);
            int semiColonPos = token.pos;
            nextToken();
            if (token.kind == RPAREN) { // Optional trailing semicolon
                                       // after last resource
                break;
            }
            defs.append(resource());
        }
        return defs.toList();
    }

    /** Resource = VariableModifiersOpt Type VariableDeclaratorId = Expression
     */
    protected JCTree resource() {
        JCModifiers optFinal = optFinal(Flags.FINAL);
        JCExpression type = parseType();
        int pos = token.pos;
        Name ident = ident();
        return variableDeclaratorRest(pos, optFinal, type, ident, true, null);
    }

    /** CompilationUnit = [ { "@" Annotation } PACKAGE Qualident ";"] {ImportDeclaration} {TypeDeclaration}
     */
    public JCTree.JCCompilationUnit parseCompilationUnit() {
        Token firstToken = token;
        JCExpression pid = null;
        JCModifiers mods = null;
        boolean consumedToplevelDoc = false;
        boolean seenImport = false;
        boolean seenPackage = false;
        List<JCAnnotation> packageAnnotations = List.nil();
        if (token.kind == MONKEYS_AT)
            mods = modifiersOpt();

        if (token.kind == PACKAGE) {
            seenPackage = true;
            if (mods != null) {
                checkNoMods(mods.flags);
                packageAnnotations = mods.annotations;
                mods = null;
            }
            nextToken();
            pid = qualident();
            accept(SEMI);
        }
        ListBuffer<JCTree> defs = new ListBuffer<JCTree>();
        boolean checkForImports = true;
        boolean firstTypeDecl = true;
        while (token.kind != EOF) {
            if (token.pos <= endPosTable.errorEndPos) {
                // error recovery
                skip(checkForImports, false, false, false);
                if (token.kind == EOF)
                    break;
            }
            if (checkForImports && mods == null && token.kind == IMPORT) {
                seenImport = true;
                defs.append(importDeclaration());
            } else {
                String docComment = token.comment(CommentStyle.JAVADOC);
                if (firstTypeDecl && !seenImport && !seenPackage) {
                    docComment = firstToken.comment(CommentStyle.JAVADOC);
                    consumedToplevelDoc = true;
                }
                JCTree def = typeDeclaration(mods, docComment);
                if (def instanceof JCExpressionStatement)
                    def = ((JCExpressionStatement)def).expr;
                defs.append(def);
                if (def instanceof JCClassDecl)
                    checkForImports = false;
                mods = null;
                firstTypeDecl = false;
            }
        }
        JCTree.JCCompilationUnit toplevel = F.at(firstToken.pos).TopLevel(packageAnnotations, pid, defs.toList());
        if (!consumedToplevelDoc)
            attach(toplevel, firstToken.comment(CommentStyle.JAVADOC));
        if (defs.elems.isEmpty())
            storeEnd(toplevel, S.prevToken().endPos);
        if (keepDocComments)
            toplevel.docComments = docComments;
        if (keepLineMap)
            toplevel.lineMap = S.getLineMap();
        toplevel.endPositions = this.endPosTable;
        return toplevel;
    }

    /** ImportDeclaration = IMPORT [ STATIC ] Ident { "." Ident } [ "." "*" ] ";"
     */
    JCTree importDeclaration() {
        int pos = token.pos;
        nextToken();
        boolean importStatic = false;
        if (token.kind == STATIC) {
            checkStaticImports();
            importStatic = true;
            nextToken();
        }
        JCExpression pid = toP(F.at(token.pos).Ident(ident()));
        do {
            int pos1 = token.pos;
            accept(DOT);
            if (token.kind == STAR) {
                pid = to(F.at(pos1).Select(pid, names.asterisk));
                nextToken();
                break;
            } else {
                pid = toP(F.at(pos1).Select(pid, ident()));
            }
        } while (token.kind == DOT);
        accept(SEMI);
        return toP(F.at(pos).Import(pid, importStatic));
    }

    /** TypeDeclaration = ClassOrInterfaceOrEnumDeclaration
     *                  | ";"
     */
    JCTree typeDeclaration(JCModifiers mods, String docComment) {
        int pos = token.pos;
        if (mods == null && token.kind == SEMI) {
            nextToken();
            return toP(F.at(pos).Skip());
        } else {
            return classOrInterfaceOrEnumDeclaration(modifiersOpt(mods), docComment);
        }
    }

    /** ClassOrInterfaceOrEnumDeclaration = ModifiersOpt
     *           (ClassDeclaration | InterfaceDeclaration | EnumDeclaration)
     *  @param mods     Any modifiers starting the class or interface declaration
     *  @param dc       The documentation comment for the class, or null.
     */
    JCStatement classOrInterfaceOrEnumDeclaration(JCModifiers mods, String dc) {
        if (token.kind == CLASS) {
            return classDeclaration(mods, dc);
        } else if (token.kind == INTERFACE) {
            return interfaceDeclaration(mods, dc);
        } else if (allowEnums) {
            if (token.kind == ENUM) {
                return enumDeclaration(mods, dc);
            } else {
                int pos = token.pos;
                List<JCTree> errs;
                if (token.kind == IDENTIFIER) {
                    errs = List.<JCTree>of(mods, toP(F.at(pos).Ident(ident())));
                    setErrorEndPos(token.pos);
                } else {
                    errs = List.<JCTree>of(mods);
                }
                return toP(F.Exec(syntaxError(pos, errs, "expected3",
                                              CLASS, INTERFACE, ENUM)));
            }
        } else {
            if (token.kind == ENUM) {
                error(token.pos, "enums.not.supported.in.source", source.name);
                allowEnums = true;
                return enumDeclaration(mods, dc);
            }
            int pos = token.pos;
            List<JCTree> errs;
            if (token.kind == IDENTIFIER) {
                errs = List.<JCTree>of(mods, toP(F.at(pos).Ident(ident())));
                setErrorEndPos(token.pos);
            } else {
                errs = List.<JCTree>of(mods);
            }
            return toP(F.Exec(syntaxError(pos, errs, "expected2",
                                          CLASS, INTERFACE)));
        }
    }

    /** ClassDeclaration = CLASS Ident TypeParametersOpt [EXTENDS Type]
     *                     [IMPLEMENTS TypeList] ClassBody
     *  @param mods    The modifiers starting the class declaration
     *  @param dc       The documentation comment for the class, or null.
     */
    protected JCClassDecl classDeclaration(JCModifiers mods, String dc) {
        int pos = token.pos;
        accept(CLASS);
        Name name = ident();

        List<JCTypeParameter> typarams = typeParametersOpt();

        JCExpression extending = null;
        if (token.kind == EXTENDS) {
            nextToken();
            extending = parseType();
        }
        List<JCExpression> implementing = List.nil();
        if (token.kind == IMPLEMENTS) {
            nextToken();
            implementing = typeList();
        }
        List<JCTree> defs = classOrInterfaceBody(name, false);
        JCClassDecl result = toP(F.at(pos).ClassDef(
            mods, name, typarams, extending, implementing, defs));
        attach(result, dc);
        return result;
    }

    /** InterfaceDeclaration = INTERFACE Ident TypeParametersOpt
     *                         [EXTENDS TypeList] InterfaceBody
     *  @param mods    The modifiers starting the interface declaration
     *  @param dc       The documentation comment for the interface, or null.
     */
    protected JCClassDecl interfaceDeclaration(JCModifiers mods, String dc) {
        int pos = token.pos;
        accept(INTERFACE);
        Name name = ident();

        List<JCTypeParameter> typarams = typeParametersOpt();

        List<JCExpression> extending = List.nil();
        if (token.kind == EXTENDS) {
            nextToken();
            extending = typeList();
        }
        List<JCTree> defs = classOrInterfaceBody(name, true);
        JCClassDecl result = toP(F.at(pos).ClassDef(
            mods, name, typarams, null, extending, defs));
        attach(result, dc);
        return result;
    }

    /** EnumDeclaration = ENUM Ident [IMPLEMENTS TypeList] EnumBody
     *  @param mods    The modifiers starting the enum declaration
     *  @param dc       The documentation comment for the enum, or null.
     */
    protected JCClassDecl enumDeclaration(JCModifiers mods, String dc) {
        int pos = token.pos;
        accept(ENUM);
        Name name = ident();

        List<JCExpression> implementing = List.nil();
        if (token.kind == IMPLEMENTS) {
            nextToken();
            implementing = typeList();
        }

        List<JCTree> defs = enumBody(name);
        mods.flags |= Flags.ENUM;
        JCClassDecl result = toP(F.at(pos).
            ClassDef(mods, name, List.<JCTypeParameter>nil(),
                null, implementing, defs));
        attach(result, dc);
        return result;
    }

    /** EnumBody = "{" { EnumeratorDeclarationList } [","]
     *                  [ ";" {ClassBodyDeclaration} ] "}"
     */
    List<JCTree> enumBody(Name enumName) {
        accept(LBRACE);
        ListBuffer<JCTree> defs = new ListBuffer<JCTree>();
        if (token.kind == COMMA) {
            nextToken();
        } else if (token.kind != RBRACE && token.kind != SEMI) {
            defs.append(enumeratorDeclaration(enumName));
            while (token.kind == COMMA) {
                nextToken();
                if (token.kind == RBRACE || token.kind == SEMI) break;
                defs.append(enumeratorDeclaration(enumName));
            }
            if (token.kind != SEMI && token.kind != RBRACE) {
                defs.append(syntaxError(token.pos, "expected3",
                                COMMA, RBRACE, SEMI));
                nextToken();
            }
        }
        if (token.kind == SEMI) {
            nextToken();
            while (token.kind != RBRACE && token.kind != EOF) {
                defs.appendList(classOrInterfaceBodyDeclaration(enumName,
                                                                false));
                if (token.pos <= endPosTable.errorEndPos) {
                    // error recovery
                   skip(false, true, true, false);
                }
            }
        }
        accept(RBRACE);
        return defs.toList();
    }

    /** EnumeratorDeclaration = AnnotationsOpt [TypeArguments] IDENTIFIER [ Arguments ] [ "{" ClassBody "}" ]
     */
    JCTree enumeratorDeclaration(Name enumName) {
        String dc = token.comment(CommentStyle.JAVADOC);
        int flags = Flags.PUBLIC|Flags.STATIC|Flags.FINAL|Flags.ENUM;
        if (token.deprecatedFlag()) {
            flags |= Flags.DEPRECATED;
        }
        int pos = token.pos;
        List<JCAnnotation> annotations = annotationsOpt();
        JCModifiers mods = F.at(annotations.isEmpty() ? Position.NOPOS : pos).Modifiers(flags, annotations);
        List<JCExpression> typeArgs = typeArgumentsOpt();
        int identPos = token.pos;
        Name name = ident();
        int createPos = token.pos;
        List<JCExpression> args = (token.kind == LPAREN)
            ? arguments() : List.<JCExpression>nil();
        JCClassDecl body = null;
        if (token.kind == LBRACE) {
            JCModifiers mods1 = F.at(Position.NOPOS).Modifiers(Flags.ENUM | Flags.STATIC);
            List<JCTree> defs = classOrInterfaceBody(names.empty, false);
            body = toP(F.at(identPos).AnonymousClassDef(mods1, defs));
        }
        if (args.isEmpty() && body == null)
            createPos = identPos;
        JCIdent ident = F.at(identPos).Ident(enumName);
        JCNewClass create = F.at(createPos).NewClass(null, typeArgs, ident, args, body);
        if (createPos != identPos)
            storeEnd(create, S.prevToken().endPos);
        ident = F.at(identPos).Ident(enumName);
        JCTree result = toP(F.at(pos).VarDef(mods, name, ident, create));
        attach(result, dc);
        return result;
    }

    /** TypeList = Type {"," Type}
     */
    List<JCExpression> typeList() {
        ListBuffer<JCExpression> ts = new ListBuffer<JCExpression>();
        ts.append(parseType());
        while (token.kind == COMMA) {
            nextToken();
            ts.append(parseType());
        }
        return ts.toList();
    }

    /** ClassBody     = "{" {ClassBodyDeclaration} "}"
     *  InterfaceBody = "{" {InterfaceBodyDeclaration} "}"
     */
    List<JCTree> classOrInterfaceBody(Name className, boolean isInterface) {
        accept(LBRACE);
        if (token.pos <= endPosTable.errorEndPos) {
            // error recovery
            skip(false, true, false, false);
            if (token.kind == LBRACE)
                nextToken();
        }
        ListBuffer<JCTree> defs = new ListBuffer<JCTree>();
        while (token.kind != RBRACE && token.kind != EOF) {
            defs.appendList(classOrInterfaceBodyDeclaration(className, isInterface));
            if (token.pos <= endPosTable.errorEndPos) {
               // error recovery
               skip(false, true, true, false);
           }
        }
        accept(RBRACE);
        return defs.toList();
    }

    /** ClassBodyDeclaration =
     *      ";"
     *    | [STATIC] Block
     *    | ModifiersOpt
     *      ( Type Ident
     *        ( VariableDeclaratorsRest ";" | MethodDeclaratorRest )
     *      | VOID Ident MethodDeclaratorRest
     *      | TypeParameters (Type | VOID) Ident MethodDeclaratorRest
     *      | Ident ConstructorDeclaratorRest
     *      | TypeParameters Ident ConstructorDeclaratorRest
     *      | ClassOrInterfaceOrEnumDeclaration
     *      )
     *  InterfaceBodyDeclaration =
     *      ";"
     *    | ModifiersOpt Type Ident
     *      ( ConstantDeclaratorsRest | InterfaceMethodDeclaratorRest ";" )
     */
    protected List<JCTree> classOrInterfaceBodyDeclaration(Name className, boolean isInterface) {
        if (token.kind == SEMI) {
            nextToken();
            return List.<JCTree>nil();
        } else {
            String dc = token.comment(CommentStyle.JAVADOC);
            int pos = token.pos;
            JCModifiers mods = modifiersOpt();
            if (token.kind == CLASS ||
                token.kind == INTERFACE ||
                allowEnums && token.kind == ENUM) {
                return List.<JCTree>of(classOrInterfaceOrEnumDeclaration(mods, dc));
            } else if (token.kind == LBRACE && !isInterface &&
                       (mods.flags & Flags.StandardFlags & ~Flags.STATIC) == 0 &&
                       mods.annotations.isEmpty()) {
                return List.<JCTree>of(block(pos, mods.flags));
            } else {
                pos = token.pos;
                List<JCTypeParameter> typarams = typeParametersOpt();
                // if there are type parameters but no modifiers, save the start
                // position of the method in the modifiers.
                if (typarams.nonEmpty() && mods.pos == Position.NOPOS) {
                    mods.pos = pos;
                    storeEnd(mods, pos);
                }
                Token tk = token;
                pos = token.pos;
                JCExpression type;
                boolean isVoid = token.kind == VOID;
                if (isVoid) {
                    type = to(F.at(pos).TypeIdent(TypeTags.VOID));
                    nextToken();
                } else {
                    type = parseType();
                }
                if (token.kind == LPAREN && !isInterface && type.hasTag(IDENT)) {
                    if (isInterface || tk.name() != className)
                        error(pos, "invalid.meth.decl.ret.type.req");
                    return List.of(methodDeclaratorRest(
                        pos, mods, null, names.init, typarams,
                        isInterface, true, dc));
                } else {
                    pos = token.pos;
                    Name name = ident();
                    if (token.kind == LPAREN) {
                        return List.of(methodDeclaratorRest(
                            pos, mods, type, name, typarams,
                            isInterface, isVoid, dc));
                    } else if (!isVoid && typarams.isEmpty()) {
                        List<JCTree> defs =
                            variableDeclaratorsRest(pos, mods, type, name, isInterface, dc,
                                                    new ListBuffer<JCTree>()).toList();
                        storeEnd(defs.last(), token.endPos);
                        accept(SEMI);
                        return defs;
                    } else {
                        pos = token.pos;
                        List<JCTree> err = isVoid
                            ? List.<JCTree>of(toP(F.at(pos).MethodDef(mods, name, type, typarams,
                                List.<JCVariableDecl>nil(), List.<JCExpression>nil(), null, null)))
                            : null;
                        return List.<JCTree>of(syntaxError(token.pos, err, "expected", LPAREN));
                    }
                }
            }
        }
    }

    /** MethodDeclaratorRest =
     *      FormalParameters BracketsOpt [Throws TypeList] ( MethodBody | [DEFAULT AnnotationValue] ";")
     *  VoidMethodDeclaratorRest =
     *      FormalParameters [Throws TypeList] ( MethodBody | ";")
     *  InterfaceMethodDeclaratorRest =
     *      FormalParameters BracketsOpt [THROWS TypeList] ";"
     *  VoidInterfaceMethodDeclaratorRest =
     *      FormalParameters [THROWS TypeList] ";"
     *  ConstructorDeclaratorRest =
     *      "(" FormalParameterListOpt ")" [THROWS TypeList] MethodBody
     */
    protected JCTree methodDeclaratorRest(int pos,
                              JCModifiers mods,
                              JCExpression type,
                              Name name,
                              List<JCTypeParameter> typarams,
                              boolean isInterface, boolean isVoid,
                              String dc) {
        List<JCVariableDecl> params = formalParameters();
        if (!isVoid) type = bracketsOpt(type);
        List<JCExpression> thrown = List.nil();
        if (token.kind == THROWS) {
            nextToken();
            thrown = qualidentList();
        }
        JCBlock body = null;
        JCExpression defaultValue;
        if (token.kind == LBRACE) {
            body = block();
            defaultValue = null;
        } else {
            if (token.kind == DEFAULT) {
                accept(DEFAULT);
                defaultValue = annotationValue();
            } else {
                defaultValue = null;
            }
            accept(SEMI);
            if (token.pos <= endPosTable.errorEndPos) {
                // error recovery
                skip(false, true, false, false);
                if (token.kind == LBRACE) {
                    body = block();
                }
            }
        }

        JCMethodDecl result =
            toP(F.at(pos).MethodDef(mods, name, type, typarams,
                                    params, thrown,
                                    body, defaultValue));
        attach(result, dc);
        return result;
    }

    /** QualidentList = Qualident {"," Qualident}
     */
    List<JCExpression> qualidentList() {
        ListBuffer<JCExpression> ts = new ListBuffer<JCExpression>();
        ts.append(qualident());
        while (token.kind == COMMA) {
            nextToken();
            ts.append(qualident());
        }
        return ts.toList();
    }

    /** TypeParametersOpt = ["<" TypeParameter {"," TypeParameter} ">"]
     */
    List<JCTypeParameter> typeParametersOpt() {
        if (token.kind == LT) {
            checkGenerics();
            ListBuffer<JCTypeParameter> typarams = new ListBuffer<JCTypeParameter>();
            nextToken();
            typarams.append(typeParameter());
            while (token.kind == COMMA) {
                nextToken();
                typarams.append(typeParameter());
            }
            accept(GT);
            return typarams.toList();
        } else {
            return List.nil();
        }
    }

    /** TypeParameter = TypeVariable [TypeParameterBound]
     *  TypeParameterBound = EXTENDS Type {"&" Type}
     *  TypeVariable = Ident
     */
    JCTypeParameter typeParameter() {
        int pos = token.pos;
        Name name = ident();
        ListBuffer<JCExpression> bounds = new ListBuffer<JCExpression>();
        if (token.kind == EXTENDS) {
            nextToken();
            bounds.append(parseType());
            while (token.kind == AMP) {
                nextToken();
                bounds.append(parseType());
            }
        }
        return toP(F.at(pos).TypeParameter(name, bounds.toList()));
    }

    /** FormalParameters = "(" [ FormalParameterList ] ")"
     *  FormalParameterList = [ FormalParameterListNovarargs , ] LastFormalParameter
     *  FormalParameterListNovarargs = [ FormalParameterListNovarargs , ] FormalParameter
     */
    List<JCVariableDecl> formalParameters() {
        ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
        JCVariableDecl lastParam = null;
        accept(LPAREN);
        if (token.kind != RPAREN) {
            params.append(lastParam = formalParameter());
            while ((lastParam.mods.flags & Flags.VARARGS) == 0 && token.kind == COMMA) {
                nextToken();
                params.append(lastParam = formalParameter());
            }
        }
        accept(RPAREN);
        return params.toList();
    }

    List<JCVariableDecl> implicitParameters(boolean hasParens) {
        if (hasParens) {
            accept(LPAREN);
        }
        ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
        if (token.kind != RPAREN && token.kind != ARROW) {
            params.append(implicitParameter());
            while (token.kind == COMMA) {
                nextToken();
                params.append(implicitParameter());
            }
        }
        if (hasParens) {
            accept(RPAREN);
        }
        return params.toList();
    }

    JCModifiers optFinal(long flags) {
        JCModifiers mods = modifiersOpt();
        checkNoMods(mods.flags & ~(Flags.FINAL | Flags.DEPRECATED));
        mods.flags |= flags;
        return mods;
    }

    /** FormalParameter = { FINAL | '@' Annotation } Type VariableDeclaratorId
     *  LastFormalParameter = { FINAL | '@' Annotation } Type '...' Ident | FormalParameter
     */
    protected JCVariableDecl formalParameter() {
        JCModifiers mods = optFinal(Flags.PARAMETER);
        JCExpression type = parseType();
        if (token.kind == ELLIPSIS) {
            checkVarargs();
            mods.flags |= Flags.VARARGS;
            type = to(F.at(token.pos).TypeArray(type));
            nextToken();
        }
        return variableDeclaratorId(mods, type);
    }

    protected JCVariableDecl implicitParameter() {
        JCModifiers mods = F.at(token.pos).Modifiers(Flags.PARAMETER);
        return variableDeclaratorId(mods, null);
    }

/* ---------- auxiliary methods -------------- */

    void error(int pos, String key, Object ... args) {
        log.error(DiagnosticFlag.SYNTAX, pos, key, args);
    }

    void error(DiagnosticPosition pos, String key, Object ... args) {
        log.error(DiagnosticFlag.SYNTAX, pos, key, args);
    }

    void warning(int pos, String key, Object ... args) {
        log.warning(pos, key, args);
    }

    /** Check that given tree is a legal expression statement.
     */
    protected JCExpression checkExprStat(JCExpression t) {
        switch(t.getTag()) {
        case PREINC: case PREDEC:
        case POSTINC: case POSTDEC:
        case ASSIGN:
        case BITOR_ASG: case BITXOR_ASG: case BITAND_ASG:
        case SL_ASG: case SR_ASG: case USR_ASG:
        case PLUS_ASG: case MINUS_ASG:
        case MUL_ASG: case DIV_ASG: case MOD_ASG:
        case APPLY: case NEWCLASS:
        case ERRONEOUS:
            return t;
        default:
            JCExpression ret = F.at(t.pos).Erroneous(List.<JCTree>of(t));
            error(ret, "not.stmt");
            return ret;
        }
    }

    /** Return precedence of operator represented by token,
     *  -1 if token is not a binary operator. @see TreeInfo.opPrec
     */
    static int prec(TokenKind token) {
        JCTree.Tag oc = optag(token);
        return (oc != NO_TAG) ? TreeInfo.opPrec(oc) : -1;
    }

    /**
     * Return the lesser of two positions, making allowance for either one
     * being unset.
     */
    static int earlier(int pos1, int pos2) {
        if (pos1 == Position.NOPOS)
            return pos2;
        if (pos2 == Position.NOPOS)
            return pos1;
        return (pos1 < pos2 ? pos1 : pos2);
    }

    /** Return operation tag of binary operator represented by token,
     *  No_TAG if token is not a binary operator.
     */
    static JCTree.Tag optag(TokenKind token) {
        switch (token) {
        case BARBAR:
            return OR;
        case AMPAMP:
            return AND;
        case BAR:
            return BITOR;
        case BAREQ:
            return BITOR_ASG;
        case CARET:
            return BITXOR;
        case CARETEQ:
            return BITXOR_ASG;
        case AMP:
            return BITAND;
        case AMPEQ:
            return BITAND_ASG;
        case EQEQ:
            return JCTree.Tag.EQ;
        case BANGEQ:
            return NE;
        case LT:
            return JCTree.Tag.LT;
        case GT:
            return JCTree.Tag.GT;
        case LTEQ:
            return LE;
        case GTEQ:
            return GE;
        case LTLT:
            return SL;
        case LTLTEQ:
            return SL_ASG;
        case GTGT:
            return SR;
        case GTGTEQ:
            return SR_ASG;
        case GTGTGT:
            return USR;
        case GTGTGTEQ:
            return USR_ASG;
        case PLUS:
            return JCTree.Tag.PLUS;
        case PLUSEQ:
            return PLUS_ASG;
        case SUB:
            return MINUS;
        case SUBEQ:
            return MINUS_ASG;
        case STAR:
            return MUL;
        case STAREQ:
            return MUL_ASG;
        case SLASH:
            return DIV;
        case SLASHEQ:
            return DIV_ASG;
        case PERCENT:
            return MOD;
        case PERCENTEQ:
            return MOD_ASG;
        case INSTANCEOF:
            return TYPETEST;
        default:
            return NO_TAG;
        }
    }

    /** Return operation tag of unary operator represented by token,
     *  No_TAG if token is not a binary operator.
     */
    static JCTree.Tag unoptag(TokenKind token) {
        switch (token) {
        case PLUS:
            return POS;
        case SUB:
            return NEG;
        case BANG:
            return NOT;
        case TILDE:
            return COMPL;
        case PLUSPLUS:
            return PREINC;
        case SUBSUB:
            return PREDEC;
        default:
            return NO_TAG;
        }
    }

    /** Return type tag of basic type represented by token,
     *  -1 if token is not a basic type identifier.
     */
    static int typetag(TokenKind token) {
        switch (token) {
        case BYTE:
            return TypeTags.BYTE;
        case CHAR:
            return TypeTags.CHAR;
        case SHORT:
            return TypeTags.SHORT;
        case INT:
            return TypeTags.INT;
        case LONG:
            return TypeTags.LONG;
        case FLOAT:
            return TypeTags.FLOAT;
        case DOUBLE:
            return TypeTags.DOUBLE;
        case BOOLEAN:
            return TypeTags.BOOLEAN;
        default:
            return -1;
        }
    }

    void checkGenerics() {
        if (!allowGenerics) {
            error(token.pos, "generics.not.supported.in.source", source.name);
            allowGenerics = true;
        }
    }
    void checkVarargs() {
        if (!allowVarargs) {
            error(token.pos, "varargs.not.supported.in.source", source.name);
            allowVarargs = true;
        }
    }
    void checkForeach() {
        if (!allowForeach) {
            error(token.pos, "foreach.not.supported.in.source", source.name);
            allowForeach = true;
        }
    }
    void checkStaticImports() {
        if (!allowStaticImport) {
            error(token.pos, "static.import.not.supported.in.source", source.name);
            allowStaticImport = true;
        }
    }
    void checkAnnotations() {
        if (!allowAnnotations) {
            error(token.pos, "annotations.not.supported.in.source", source.name);
            allowAnnotations = true;
        }
    }
    void checkDiamond() {
        if (!allowDiamond) {
            error(token.pos, "diamond.not.supported.in.source", source.name);
            allowDiamond = true;
        }
    }
    void checkMulticatch() {
        if (!allowMulticatch) {
            error(token.pos, "multicatch.not.supported.in.source", source.name);
            allowMulticatch = true;
        }
    }
    void checkTryWithResources() {
        if (!allowTWR) {
            error(token.pos, "try.with.resources.not.supported.in.source", source.name);
            allowTWR = true;
        }
    }
    void checkLambda() {
        if (!allowLambda) {
            log.error(token.pos, "lambda.not.supported.in.source", source.name);
            allowLambda = true;
        }
    }
    void checkMethodReferences() {
        if (!allowMethodReferences) {
            log.error(token.pos, "method.references.not.supported.in.source", source.name);
            allowMethodReferences = true;
        }
    }

    /*
     * a functional source tree and end position mappings
     */
    protected class SimpleEndPosTable extends AbstractEndPosTable {

        private final Map<JCTree, Integer> endPosMap;

        SimpleEndPosTable() {
            endPosMap = new HashMap<JCTree, Integer>();
        }

        protected void storeEnd(JCTree tree, int endpos) {
            endPosMap.put(tree, errorEndPos > endpos ? errorEndPos : endpos);
        }

        protected <T extends JCTree> T to(T t) {
            storeEnd(t, token.endPos);
            return t;
        }

        protected <T extends JCTree> T toP(T t) {
            storeEnd(t, S.prevToken().endPos);
            return t;
        }

        public int getEndPos(JCTree tree) {
            Integer value = endPosMap.get(tree);
            return (value == null) ? Position.NOPOS : value;
        }

        public int replaceTree(JCTree oldTree, JCTree newTree) {
            Integer pos = endPosMap.remove(oldTree);
            if (pos != null) {
                endPosMap.put(newTree, pos);
                return pos;
            }
            return Position.NOPOS;
        }
    }

    /*
     * a default skeletal implementation without any mapping overhead.
     */
    protected class EmptyEndPosTable extends AbstractEndPosTable {

        protected void storeEnd(JCTree tree, int endpos) { /* empty */ }

        protected <T extends JCTree> T to(T t) {
            return t;
        }

        protected <T extends JCTree> T toP(T t) {
            return t;
        }

        public int getEndPos(JCTree tree) {
            return Position.NOPOS;
        }

        public int replaceTree(JCTree oldTree, JCTree newTree) {
            return Position.NOPOS;
        }

    }

    protected abstract class AbstractEndPosTable implements EndPosTable {

        /**
         * Store the last error position.
         */
        protected int errorEndPos;

        /**
         * Store ending position for a tree, the value of which is the greater
         * of last error position and the given ending position.
         * @param tree   The tree.
         * @param endpos The ending position to associate with the tree.
         */
        protected abstract void storeEnd(JCTree tree, int endpos);

        /**
         * Store current token's ending position for a tree, the value of which
         * will be the greater of last error position and the ending position of
         * the current token.
         * @param t The tree.
         */
        protected abstract <T extends JCTree> T to(T t);

        /**
         * Store current token's ending position for a tree, the value of which
         * will be the greater of last error position and the ending position of
         * the previous token.
         * @param t The tree.
         */
        protected abstract <T extends JCTree> T toP(T t);

        /**
         * Set the error position during the parsing phases, the value of which
         * will be set only if it is greater than the last stored error position.
         * @param errPos The error position
         */
        protected void setErrorEndPos(int errPos) {
            if (errPos > errorEndPos) {
                errorEndPos = errPos;
            }
        }
    }
}
