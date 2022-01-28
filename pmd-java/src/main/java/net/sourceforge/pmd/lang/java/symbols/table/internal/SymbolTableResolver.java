/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */


package net.sourceforge.pmd.lang.java.symbols.table.internal;

import static net.sourceforge.pmd.lang.java.symbols.table.internal.PatternBindingsUtil.bindersOfExpr;
import static net.sourceforge.pmd.lang.java.symbols.table.internal.PatternBindingsUtil.canCompleteNormally;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PSet;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.NodeStream;
import net.sourceforge.pmd.lang.java.ast.ASTAmbiguousName;
import net.sourceforge.pmd.lang.java.ast.ASTAnonymousClassDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTBlock;
import net.sourceforge.pmd.lang.java.ast.ASTBreakStatement;
import net.sourceforge.pmd.lang.java.ast.ASTCatchClause;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceType;
import net.sourceforge.pmd.lang.java.ast.ASTCompactConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTConstructorCall;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTForStatement;
import net.sourceforge.pmd.lang.java.ast.ASTForeachStatement;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameter;
import net.sourceforge.pmd.lang.java.ast.ASTIfStatement;
import net.sourceforge.pmd.lang.java.ast.ASTImportDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTInfixExpression;
import net.sourceforge.pmd.lang.java.ast.ASTInitializer;
import net.sourceforge.pmd.lang.java.ast.ASTLabeledStatement;
import net.sourceforge.pmd.lang.java.ast.ASTLambdaExpression;
import net.sourceforge.pmd.lang.java.ast.ASTLambdaParameter;
import net.sourceforge.pmd.lang.java.ast.ASTLocalClassStatement;
import net.sourceforge.pmd.lang.java.ast.ASTLocalVariableDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodOrConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTModifierList;
import net.sourceforge.pmd.lang.java.ast.ASTResource;
import net.sourceforge.pmd.lang.java.ast.ASTResourceList;
import net.sourceforge.pmd.lang.java.ast.ASTStatement;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchExpression;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchFallthroughBranch;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchLike;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchStatement;
import net.sourceforge.pmd.lang.java.ast.ASTTryStatement;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.java.ast.ASTWhileStatement;
import net.sourceforge.pmd.lang.java.ast.BinaryOp;
import net.sourceforge.pmd.lang.java.ast.InternalApiBridge;
import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.lang.java.ast.JavaVisitorBase;
import net.sourceforge.pmd.lang.java.internal.JavaAstProcessor;
import net.sourceforge.pmd.lang.java.rule.internal.JavaRuleUtil;
import net.sourceforge.pmd.lang.java.symbols.table.JSymbolTable;
import net.sourceforge.pmd.lang.java.symbols.table.internal.PatternBindingsUtil.BindSet;
import net.sourceforge.pmd.lang.java.types.JClassType;


/**
 * Visitor that builds all symbol table stacks for a compilation unit.
 * It's bound to a compilation unit and cannot be reused for several ACUs.
 *
 * @since 7.0.0
 */
public final class SymbolTableResolver {

    private SymbolTableResolver() {
        // façade
    }

    public static void traverse(JavaAstProcessor processor, ASTCompilationUnit root) {
        SymTableFactory helper = new SymTableFactory(root.getPackageName(), processor);
        ReferenceCtx ctx = ReferenceCtx.root(processor, root);
        Set<DeferredNode> todo = Collections.singleton(new DeferredNode(root, ctx, SymbolTableImpl.EMPTY));
        do {
            Set<DeferredNode> newDeferred = new HashSet<>();
            for (DeferredNode deferred : todo) {
                MyVisitor visitor = new MyVisitor(helper, todo, newDeferred);
                visitor.traverse(deferred);
            }
            todo = newDeferred;
        } while (!todo.isEmpty());
    }

    private static class DeferredNode {

        final JavaNode node;
        // this is data used to resume the traversal
        final ReferenceCtx enclosingCtx;
        final JSymbolTable localStackTop;

        private DeferredNode(JavaNode node, ReferenceCtx enclosingCtx, JSymbolTable localStackTop) {
            this.node = node;
            this.enclosingCtx = enclosingCtx;
            this.localStackTop = localStackTop;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DeferredNode that = (DeferredNode) o;
            return node.equals(that.node);
        }

        @Override
        public int hashCode() {
            return Objects.hash(node);
        }
    }

    private static class MyVisitor extends JavaVisitorBase<@NonNull ReferenceCtx, Void> {

        private final SymTableFactory f;
        private final Deque<JSymbolTable> stack = new ArrayDeque<>();

        private final Deque<ASTAnyTypeDeclaration> enclosingType = new ArrayDeque<>();

        private final Set<DeferredNode> deferredInPrevRound;
        private final Set<DeferredNode> newDeferred;
        private final StatementVisitor stmtVisitor = new StatementVisitor();

        MyVisitor(SymTableFactory helper, Set<DeferredNode> deferredInPrevRound, Set<DeferredNode> newDeferred) {
            f = helper;
            this.deferredInPrevRound = deferredInPrevRound;
            this.newDeferred = newDeferred;
        }


        /**
         * Start the analysis.
         */
        void traverse(DeferredNode task) {
            assert stack.isEmpty()
                : "Stack should be empty when starting the traversal";

            stack.push(task.localStackTop);
            task.node.acceptVisitor(this, task.enclosingCtx);
            JSymbolTable last = stack.pop();

            assert last == task.localStackTop  // NOPMD CompareObjectsWithEquals
                : "Unbalanced stack push/pop! Started with " + task.localStackTop + ", finished on " + last;
        }

        @Override
        public Void visit(ASTClassOrInterfaceType node, @NonNull ReferenceCtx data) {
            // all types are disambiguated in this resolver, because
            // the symbols available inside the body of an anonymous class
            // depend on the type of the superclass/superinterface (the Runnable in `new Runnable() { }`).
            // This type may be
            // 1. a diamond (`new Function<>() { ... }`),
            // 2. qualified (`expr.new Inner() { ... }`)
            // 3. both
            // For case 2, resolution of the symbol of Inner needs full
            // type resolution of the qualifying `expr`, which may depend
            // on the disambiguation of arbitrary type nodes (eg method
            // parameters, local var types).
            // Which means, as early as in this visitor, we may need the
            // symbols of all class types. For that reason we disambiguate
            // them early.
            // Todo test ambig names in expressions depended on by the qualifier
            f.disambig(NodeStream.of(node), data);
            return null;
        }

        @Override
        public Void visit(ASTAmbiguousName node, @NonNull ReferenceCtx data) {
            // see comment in visit(ClassType)
            f.disambig(NodeStream.of(node), data);
            return null;
        }

        @Override
        public Void visit(ASTModifierList node, @NonNull ReferenceCtx ctx) {
            // do nothing
            return null;
        }

        @Override
        public Void visit(ASTCompilationUnit node, @NonNull ReferenceCtx ctx) {
            Map<Boolean, List<ASTImportDeclaration>> isImportOnDemand = node.children(ASTImportDeclaration.class)
                                                                            .collect(Collectors.partitioningBy(ASTImportDeclaration::isImportOnDemand));

            int pushed = 0;
            pushed += pushOnStack(f.importsOnDemand(top(), isImportOnDemand.get(true)));
            pushed += pushOnStack(f.javaLangSymTable(top()));
            pushed += pushOnStack(f.samePackageSymTable(top()));
            pushed += pushOnStack(f.singleImportsSymbolTable(top(), isImportOnDemand.get(false)));

            NodeStream<ASTAnyTypeDeclaration> typeDecls = node.getTypeDeclarations();

            // types declared inside the compilation unit
            pushed += pushOnStack(f.typesInFile(top(), typeDecls));

            setTopSymbolTable(node);

            for (ASTAnyTypeDeclaration td : typeDecls) {
                // preprocess all sibling types
                processTypeHeader(td, ctx);
            }

            // All of the header symbol tables belong to the CompilationUnit
            visitChildren(node, ctx);

            popStack(pushed);

            return null;
        }


        private void processTypeHeader(ASTAnyTypeDeclaration node, ReferenceCtx ctx) {
            setTopSymbolTable(node.getModifiers());

            int pushed = pushOnStack(f.selfType(top(), node.getTypeMirror()));
            pushed += pushOnStack(f.typeHeader(top(), node.getSymbol()));

            NodeStream<? extends JavaNode> notBody = node.children().drop(1).dropLast(1);
            for (JavaNode it : notBody) {
                setTopSymbolTable(it);
            }

            popStack(pushed - 1);

            // resolve the supertypes, necessary for TypeMemberSymTable
            f.disambig(notBody, ctx); // extends/implements

            setTopSymbolTable(node);
            popStack();
        }

        @Override
        public Void visitTypeDecl(ASTAnyTypeDeclaration node, @NonNull ReferenceCtx ctx) {
            int pushed = 0;

            enclosingType.push(node);
            ReferenceCtx bodyCtx = ctx.scopeDownToNested(node.getSymbol());

            // the following is just for the body
            pushed += pushOnStack(f.typeBody(top(), node.getTypeMirror()));

            setTopSymbolTable(node.getBody());

            // preprocess siblings
            node.getDeclarations(ASTAnyTypeDeclaration.class)
                .forEach(d -> processTypeHeader(d, bodyCtx));


            // process fields first, their type is needed for JSymbolTable#resolveValue
            f.disambig(node.getDeclarations(ASTFieldDeclaration.class)
                           .map(ASTFieldDeclaration::getTypeNode),
                       bodyCtx);
            visitChildren(node.getBody(), bodyCtx);

            enclosingType.pop();

            popStack(pushed);

            return null;
        }

        @Override
        public Void visit(ASTAnonymousClassDeclaration node, @NonNull ReferenceCtx ctx) {
            if (node.getParent() instanceof ASTConstructorCall) {
                // deferred to later, the symbol table for its body depends
                // on typeres of the ctor which may be qualified, and refer
                // to stuff that are declared later in the compilation unit,
                // and not yet disambiged.
                DeferredNode deferredSpec = new DeferredNode(node, ctx, top());
                if (!deferredInPrevRound.contains(deferredSpec)) {
                    newDeferred.add(deferredSpec);
                    return null;
                }
                // otherwise fallthrough
            }
            return visitTypeDecl(node, ctx);
        }

        @Override
        public Void visitMethodOrCtor(ASTMethodOrConstructorDeclaration node, @NonNull ReferenceCtx ctx) {
            setTopSymbolTable(node.getModifiers());
            int pushed = pushOnStack(f.bodyDeclaration(top(), enclosing(), node.getFormalParameters(), node.getTypeParameters()));
            setTopSymbolTableAndRecurse(node, ctx);
            popStack(pushed);
            return null;
        }

        @Override
        public Void visit(ASTInitializer node, @NonNull ReferenceCtx ctx) {
            int pushed = pushOnStack(f.bodyDeclaration(top(), enclosing(), null, null));
            setTopSymbolTableAndRecurse(node, ctx);
            popStack(pushed);
            return null;
        }


        @Override
        public Void visit(ASTCompactConstructorDeclaration node, @NonNull ReferenceCtx ctx) {
            setTopSymbolTable(node.getModifiers());
            int pushed = pushOnStack(f.recordCtor(top(), enclosing(), node.getSymbol()));
            setTopSymbolTableAndRecurse(node, ctx);
            popStack(pushed);
            return null;
        }


        @Override
        public Void visit(ASTLambdaExpression node, @NonNull ReferenceCtx ctx) {
            int pushed = pushOnStack(f.localVarSymTable(top(), enclosing(), formalsOf(node)));
            setTopSymbolTableAndRecurse(node, ctx);
            popStack(pushed);
            return null;
        }

        @Override
        public Void visit(ASTBlock node, @NonNull ReferenceCtx ctx) {
            return visitBlockLike(node, ctx);
        }

        @Override
        public Void visit(ASTSwitchStatement node, @NonNull ReferenceCtx ctx) {
            return visitSwitch(node, ctx);
        }

        @Override
        public Void visit(ASTSwitchExpression node, @NonNull ReferenceCtx ctx) {
            return visitSwitch(node, ctx);
        }

        private Void visitSwitch(ASTSwitchLike node, @NonNull ReferenceCtx ctx) {
            setTopSymbolTable(node);
            visitChildren(node.getTestedExpression(), ctx);
            visitBlockLike(stmtsOfSwitchBlock(node), ctx);
            return null;
        }


        private Void visitBlockLike(Iterable<? extends ASTStatement> node, @NonNull ReferenceCtx ctx) {
            /*
             * Process the statements of a block in a sequence. Each local
             * var/class declaration is only in scope for the following
             * statements (and its own initializer).
             */
            int pushed = 0;
            for (ASTStatement st : node) {
                if (st instanceof ASTLocalVariableDeclaration) {
                    pushed += pushOnStack(f.localVarSymTable(top(), enclosing(), ((ASTLocalVariableDeclaration) st).getVarIds()));
                } else if (st instanceof ASTLocalClassStatement) {
                    ASTAnyTypeDeclaration local = ((ASTLocalClassStatement) st).getDeclaration();
                    pushed += pushOnStack(f.localTypeSymTable(top(), local.getTypeMirror()));
                    processTypeHeader(local, ctx);
                }

                setTopSymbolTable(st);
                // those vars are the one produced by pattern bindings
                PSet<ASTVariableDeclaratorId> newVars = st.acceptVisitor(this.stmtVisitor, ctx);
                pushed += pushOnStack(f.localVarSymTable(top(), enclosing(), newVars));
            }

            popStack(pushed);
            return null;
        }

        @Override
        public Void visit(ASTForeachStatement node, @NonNull ReferenceCtx ctx) {
            // the varId is only in scope in the body and not the iterable expr
            setTopSymbolTableAndRecurse(node.getIterableExpr(), ctx);

            ASTVariableDeclaratorId varId = node.getVarId();
            acceptIfNotNull(varId.getTypeNode(), ctx);

            int pushed = pushOnStack(f.localVarSymTable(top(), enclosing(), varId.getSymbol()));
            ASTStatement body = node.getBody();
            // unless it's a block the body statement may never set a
            // symbol table that would have this table as parent,
            // so the table would be dangling
            setTopSymbolTableAndRecurse(body, ctx);
            popStack(pushed);
            return null;
        }

        void acceptIfNotNull(@Nullable JavaNode node, ReferenceCtx ctx) {
            if (node != null) {
                node.acceptVisitor(this, ctx);
            }
        }

        @Override
        public Void visit(ASTTryStatement node, @NonNull ReferenceCtx ctx) {

            ASTResourceList resources = node.getResources();
            if (resources != null) {
                NodeStream<ASTStatement> union =
                    NodeStream.union(
                        stmtsOfResources(resources),
                        // use the body instead of unwrapping it so
                        // that it has the correct symbol table too
                        NodeStream.of(node.getBody())
                    );
                visitBlockLike(union, ctx);

                for (Node child : node.getBody().asStream().followingSiblings()) {
                    child.acceptVisitor(this, ctx);
                }
            } else {
                visitChildren(node, ctx);
            }

            return null;
        }

        @Override
        public Void visit(ASTCatchClause node, @NonNull ReferenceCtx ctx) {
            int pushed = pushOnStack(f.localVarSymTable(top(), enclosing(), node.getParameter().getVarId().getSymbol()));
            setTopSymbolTableAndRecurse(node, ctx);
            popStack(pushed);
            return null;
        }

        @Override
        public Void visit(ASTInfixExpression node, @NonNull ReferenceCtx ctx) {
            // need to account for pattern bindings.
            // visit left operand first. Maybe it introduces bindings in the rigt operand.

            node.getLeftOperand().acceptVisitor(this, ctx);

            BinaryOp op = node.getOperator();
            if (op == BinaryOp.CONDITIONAL_AND) {

                PSet<ASTVariableDeclaratorId> trueBindings = bindersOfExpr(node.getLeftOperand()).getTrueBindings();
                if (!trueBindings.isEmpty()) {
                    int pushed = pushOnStack(f.localVarSymTable(top(), enclosing(), trueBindings));
                    setTopSymbolTableAndRecurse(node.getRightOperand(), ctx);
                    popStack(pushed);
                    return null;
                }

            } else if (op == BinaryOp.CONDITIONAL_OR) {

                PSet<ASTVariableDeclaratorId> falseBindings = bindersOfExpr(node.getLeftOperand()).getFalseBindings();
                if (!falseBindings.isEmpty()) {
                    int pushed = pushOnStack(f.localVarSymTable(top(), enclosing(), falseBindings));
                    setTopSymbolTableAndRecurse(node.getRightOperand(), ctx);
                    popStack(pushed);
                    return null;
                }

            }

            // not a special case, finish visiting right operand
            return node.getRightOperand().acceptVisitor(this, ctx);
        }

        // non-static
        // Every visit method returns the set of variables that are introduced by the statement
        // as defined in the JLS:
        //   https://cr.openjdk.java.net/~gbierman/jep394/jep394-20201012/specs/patterns-instanceof-jls.html#jls-6.3.1
        class StatementVisitor extends JavaVisitorBase<ReferenceCtx, PSet<ASTVariableDeclaratorId>> {

            @Override
            public PSet<ASTVariableDeclaratorId> visitJavaNode(JavaNode node, ReferenceCtx ctx) {
                throw new IllegalStateException(node + " should not have been visited by this");
            }

            // default to calling the method on the outer class
            @Override
            public PSet<ASTVariableDeclaratorId> visitStatement(ASTStatement node, ReferenceCtx ctx) {
                node.acceptVisitor(MyVisitor.this, ctx);
                return BindSet.noBindings();
            }

            @Override
            public PSet<ASTVariableDeclaratorId> visit(ASTIfStatement node, ReferenceCtx ctx) {
                BindSet bindSet = bindersOfExpr(node.getCondition());
                if (bindSet.isEmpty()) {
                    return super.visit(node, ctx);
                }

                ASTStatement thenBranch = node.getThenBranch();
                ASTStatement elseBranch = node.getElseBranch();

                node.getCondition().acceptVisitor(MyVisitor.this, ctx);

                // the true bindings of the condition are in scope in the then branch
                int pushed = pushOnStack(f.localVarSymTable(top(), enclosing(), bindSet.getTrueBindings()));
                setTopSymbolTableAndRecurse(thenBranch, ctx);
                popStack(pushed);

                if (elseBranch != null) {
                    // if there is an else, the false bindings are in scope in the else branch
                    pushed = pushOnStack(f.localVarSymTable(top(), enclosing(), bindSet.getFalseBindings()));
                    setTopSymbolTableAndRecurse(elseBranch, ctx);
                    popStack(pushed);

                    boolean thenCanCompleteNormally = canCompleteNormally(thenBranch);
                    boolean elseCanCompleteNormally = canCompleteNormally(elseBranch);

                    // the bindings are visible in the statements following this if/else
                    // if one of those conditions match
                    if (thenCanCompleteNormally && !elseCanCompleteNormally) {
                        return bindSet.getTrueBindings();
                    } else if (!thenCanCompleteNormally && elseCanCompleteNormally) {
                        return bindSet.getFalseBindings();
                    }
                } else if (!canCompleteNormally(thenBranch)) {
                    return bindSet.getFalseBindings();
                }
                return BindSet.noBindings();
            }

            @Override
            public PSet<ASTVariableDeclaratorId> visit(ASTWhileStatement node, ReferenceCtx ctx) {
                BindSet bindSet = bindersOfExpr(node.getCondition());
                if (bindSet.isEmpty()) {
                    return super.visit(node, ctx);
                }

                node.getCondition().acceptVisitor(MyVisitor.this, ctx);

                int pushed = pushOnStack(f.localVarSymTable(top(), enclosing(), NodeStream.fromIterable(bindSet.getTrueBindings())));
                setTopSymbolTableAndRecurse(node.getBody(), ctx);
                popStack(pushed);

                if (node.descendants(ASTBreakStatement.class).isEmpty()) {
                    // todo this is a bit too restrictive, we need to check the break target,
                    //  because if we break to after the following code, it's ok
                    //  JLS:
                    //  > V is introduced by while (e) S iff V is introduced by e when false, and S does not contains a reachable break statement whose break target contains S.
                    return bindSet.getFalseBindings();
                }
                return BindSet.noBindings();
            }

            @Override
            public PSet<ASTVariableDeclaratorId> visit(ASTForStatement node, @NonNull ReferenceCtx ctx) {
                int pushed = pushOnStack(f.localVarSymTable(top(), enclosing(), varsOfInit(node)));

                ASTExpression condition = node.getCondition();
                setTopSymbolTableAndRecurse(condition, ctx);

                BindSet bindSet = bindersOfExpr(condition);
                pushed += pushOnStack(f.localVarSymTable(top(), enclosing(), bindSet.getTrueBindings()));
                setTopSymbolTableAndRecurse(node.getUpdate(), ctx);
                setTopSymbolTableAndRecurse(node.getBody(), ctx);
                popStack(pushed);

                if (bindSet.getFalseBindings().isEmpty()) {
                    return BindSet.noBindings();
                } else {
                    // A pattern variable is introduced by a basic for statement iff
                    // (i) it is introduced by the condition expression when false and
                    // (ii) the contained statement, S, does not contain a reachable
                    // break statement whose break target contains S (§14.15).
                    Set<JavaNode> containingStatements = node.ancestorsOrSelf()
                                                             .filter(JavaRuleUtil::mayBeBreakTarget)
                                                             .collect(Collectors.toSet());
                    boolean hasNoBreaks = node.getBody()
                                              .descendants(ASTBreakStatement.class)
                                              .none(it -> containingStatements.contains(it.getTarget()));
                    if (hasNoBreaks) {
                        return bindSet.getFalseBindings();
                    } else {
                        return BindSet.noBindings();
                    }
                }
            }

            @Override
            public PSet<ASTVariableDeclaratorId> visit(ASTLabeledStatement node, @NonNull ReferenceCtx ctx) {
                // A pattern variable is introduced by a labeled statement
                // if and only if it is introduced by its immediately contained Statement.
                return node.getStatement().acceptVisitor(this, ctx);
            }
        }


        // <editor-fold defaultstate="collapsed" desc="Stack manipulation routines">

        private void setTopSymbolTable(JavaNode node) {
            InternalApiBridge.setSymbolTable(node, top());
        }

        private JClassType enclosing() {
            return enclosingType.getFirst().getTypeMirror();
        }

        private void setTopSymbolTableAndRecurse(JavaNode node, @NonNull ReferenceCtx ctx) {
            setTopSymbolTable(node);
            visitChildren(node, ctx);
        }

        private int pushOnStack(JSymbolTable table) {
            if (table == top()) { // NOPMD CompareObjectsWithEquals
                return 0; // and don't set the stack top
            }
            stack.push(table);
            return 1;
        }

        private JSymbolTable popStack() {
            return stack.pop();
        }

        private void popStack(int times) {
            assert stack.size() > times : "Stack is too small (" + times + ") " + stack;
            while (times-- > 0) {
                popStack();
            }
        }

        private JSymbolTable top() {
            return stack.getFirst();
        }


        // </editor-fold>

        // <editor-fold defaultstate="collapsed" desc="Convenience methods">


        static NodeStream<ASTStatement> stmtsOfSwitchBlock(ASTSwitchLike node) {
            return node.getBranches()
                       .filterIs(ASTSwitchFallthroughBranch.class)
                       .flatMap(ASTSwitchFallthroughBranch::getStatements);
        }


        static NodeStream<ASTLocalVariableDeclaration> stmtsOfResources(ASTResourceList node) {
            return node.toStream().map(ASTResource::asLocalVariableDeclaration);
        }


        static NodeStream<ASTVariableDeclaratorId> varsOfInit(ASTForStatement node) {
            return NodeStream.of(node.getInit())
                             .filterIs(ASTLocalVariableDeclaration.class)
                             .flatMap(ASTLocalVariableDeclaration::getVarIds);
        }

        static NodeStream<ASTVariableDeclaratorId> formalsOf(ASTLambdaExpression node) {
            return node.getParameters().toStream().map(ASTLambdaParameter::getVarId);
        }

        static NodeStream<ASTVariableDeclaratorId> formalsOf(ASTMethodOrConstructorDeclaration node) {
            return node.getFormalParameters().toStream().map(ASTFormalParameter::getVarId);
        }
        // </editor-fold>


    }

}
