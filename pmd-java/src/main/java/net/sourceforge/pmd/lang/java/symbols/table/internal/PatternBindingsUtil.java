/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.symbols.table.internal;

import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

import net.sourceforge.pmd.internal.util.AssertionUtil;
import net.sourceforge.pmd.lang.java.ast.ASTBlock;
import net.sourceforge.pmd.lang.java.ast.ASTBreakStatement;
import net.sourceforge.pmd.lang.java.ast.ASTContinueStatement;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTIfStatement;
import net.sourceforge.pmd.lang.java.ast.ASTInfixExpression;
import net.sourceforge.pmd.lang.java.ast.ASTLabeledStatement;
import net.sourceforge.pmd.lang.java.ast.ASTPattern;
import net.sourceforge.pmd.lang.java.ast.ASTPatternExpression;
import net.sourceforge.pmd.lang.java.ast.ASTReturnStatement;
import net.sourceforge.pmd.lang.java.ast.ASTStatement;
import net.sourceforge.pmd.lang.java.ast.ASTSynchronizedStatement;
import net.sourceforge.pmd.lang.java.ast.ASTThrowStatement;
import net.sourceforge.pmd.lang.java.ast.ASTTypePattern;
import net.sourceforge.pmd.lang.java.ast.ASTUnaryExpression;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.java.ast.ASTWhileStatement;
import net.sourceforge.pmd.lang.java.ast.BinaryOp;
import net.sourceforge.pmd.lang.java.ast.UnaryOp;
import net.sourceforge.pmd.lang.java.rule.internal.JavaRuleUtil;

/**
 * Utilities to resolve scope of pattern binding variables.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-6.html#jls-6.3.1">Java Language Specification</a>
 */
final class PatternBindingsUtil {

    private PatternBindingsUtil() {
        // util class
    }

    /**
     * Returns whether the statement can complete normally. For instance,
     * an expression statement may complete normally, while a return or throw
     * always complete abruptly.
     */
    static boolean canCompleteNormally(ASTStatement stmt) {
        if (stmt instanceof ASTLabeledStatement) {
            // we need to remove labels
            return canCompleteNormally(((ASTLabeledStatement) stmt).getStatement());
        }

        return canCompleteNormallyImpl(stmt, new State(HashTreePSet.empty(), true, true));
    }

    /**
     * @param stmt Statement
     */
    private static boolean canCompleteNormallyImpl(ASTStatement stmt, State state) {
        /*
            TODO:
                - switches
                - do {} while(true);
                - while(true) {}
         */
        if (stmt instanceof ASTThrowStatement || stmt instanceof ASTReturnStatement) {

            return false;

        }

        if (stmt instanceof ASTBreakStatement) {

            String label = ((ASTBreakStatement) stmt).getLabel();
            return label == null && state.isBreakAllowed()
                || label != null && state.getLabelsInScope().contains(label);

        } else if (stmt instanceof ASTContinueStatement) {

            String label = ((ASTContinueStatement) stmt).getLabel();
            return label == null && state.isContinueAllowed()
                || label != null && state.getLabelsInScope().contains(label);

        } else if (stmt instanceof ASTBlock) {
            // A block can complete normally if all of its statements
            // in sequence can complete normally.
            // Since if a statement CANNOT complete normally, anything
            // that follows is dead code (and would be a compile-time
            // error if there is any), we could optimize this branch
            // by just checking that the last statement of the block
            // may complete normally, under the assumption that we're
            // handling only valid java source code. Let's do this later.

            for (ASTStatement child : (ASTBlock) stmt) {
                if (!canCompleteNormallyImpl(child, state)) {
                    return false;
                }
            }
            return true;
        } else if (stmt instanceof ASTIfStatement) {
            ASTIfStatement ifStmt = (ASTIfStatement) stmt;

            ASTStatement thenBranch = ifStmt.getThenBranch();
            ASTStatement elseBranch = ifStmt.getElseBranch();

            return elseBranch == null
                || canCompleteNormallyImpl(thenBranch, state)
                || canCompleteNormallyImpl(elseBranch, state);

        } else if (stmt instanceof ASTLabeledStatement) {
            ASTLabeledStatement labeledStmt = (ASTLabeledStatement) stmt;

            return canCompleteNormallyImpl(labeledStmt.getStatement(), state.withLabel(labeledStmt.getLabel()));

        } else if (stmt instanceof ASTSynchronizedStatement) {

            return canCompleteNormallyImpl(((ASTSynchronizedStatement) stmt).getBody(), state);

        } else if (stmt instanceof ASTWhileStatement) {

            ASTWhileStatement loop = (ASTWhileStatement) stmt;
            if (JavaRuleUtil.isBooleanLiteral(loop.getCondition(), true)) {
                // todo this does not really work
                //  should be "may complete abruptly", here it's "must complete abruptly"
                return !canCompleteNormallyImpl(loop.getBody(), new State(state.labelsInScope, true, true));
            }

            return true;
        } else {
            return true;
        }
    }

    /**
     * Returns the binding set declared by the expression. Note that
     * only some expressions contribute bindings, and every other form
     * of expression does not contribute anything (meaning, all subexpressions
     * are not processed).
     */
    static BindSet bindersOfExpr(ASTExpression e) {
        /*
           JLS 17§6.3.1
           If an expression is not a conditional-and expression, conditional-or
           expression, logical complement expression, conditional expression,
           instanceof expression, switch expression, or parenthesized
           expression, then no scope rules apply.
         */

        if (e instanceof ASTUnaryExpression) {
            ASTUnaryExpression unary = (ASTUnaryExpression) e;
            return unary.getOperator() == UnaryOp.NEGATION
                   ? bindersOfExpr(unary.getOperand()).negate()
                   : BindSet.EMPTY;

        } else if (e instanceof ASTInfixExpression) {
            BinaryOp op = ((ASTInfixExpression) e).getOperator();
            ASTExpression left = ((ASTInfixExpression) e).getLeftOperand();
            ASTExpression right = ((ASTInfixExpression) e).getRightOperand();

            if (op == BinaryOp.INSTANCEOF && right instanceof ASTPatternExpression) {

                return collectBindings(((ASTPatternExpression) right).getPattern());

            } else if (op == BinaryOp.CONDITIONAL_AND) { // &&
                // A pattern variable is introduced by a && b when true iff either
                // (i) it is introduced by a when true or
                // (ii) it is introduced by b when true.

                return BindSet.whenTrue(
                    bindersOfExpr(left).trueBindings.plusAll(bindersOfExpr(right).trueBindings)
                );

            } else if (op == BinaryOp.CONDITIONAL_OR) { // ||
                // A pattern variable is introduced by a || b when false iff either
                // (i) it is introduced by a when false or
                // (ii) it is introduced by b when false.

                return BindSet.whenFalse(
                    bindersOfExpr(left).falseBindings.plusAll(bindersOfExpr(right).falseBindings)
                );

            } else {
                return BindSet.EMPTY;
            }
        }
        return BindSet.EMPTY;
    }

    static BindSet collectBindings(ASTPattern pattern) {
        if (pattern instanceof ASTTypePattern) {
            return BindSet.EMPTY.addBinding(((ASTTypePattern) pattern).getVarId());
        } else {
            throw AssertionUtil.shouldNotReachHere("no other instances of pattern should exist");
        }
    }

    /**
     * A set of bindings contributed by a (boolean) expression. Different
     * bindings are introduced if the expr evaluates to false or true, which
     * is relevant for the scope of bindings introduced in if stmt conditions.
     */
    static final class BindSet {

        static final BindSet EMPTY = new BindSet(HashTreePSet.empty(),
                                                 HashTreePSet.empty());

        private final PSet<ASTVariableDeclaratorId> trueBindings;
        private final PSet<ASTVariableDeclaratorId> falseBindings;

        static PSet<ASTVariableDeclaratorId> noBindings() {
            return HashTreePSet.empty();
        }

        BindSet(PSet<ASTVariableDeclaratorId> trueBindings,
                PSet<ASTVariableDeclaratorId> falseBindings) {
            this.trueBindings = trueBindings;
            this.falseBindings = falseBindings;
        }

        public PSet<ASTVariableDeclaratorId> getTrueBindings() {
            return trueBindings;
        }

        public PSet<ASTVariableDeclaratorId> getFalseBindings() {
            return falseBindings;
        }

        BindSet negate() {
            return isEmpty() ? this : new BindSet(falseBindings, trueBindings);
        }

        boolean isEmpty() {
            return this == EMPTY;
        }

        BindSet addBinding(ASTVariableDeclaratorId e) {
            return new BindSet(trueBindings.plus(e), falseBindings);
        }

        static BindSet whenTrue(PSet<ASTVariableDeclaratorId> bindings) {
            return new BindSet(bindings, HashTreePSet.empty());
        }

        static BindSet whenFalse(PSet<ASTVariableDeclaratorId> bindings) {
            return new BindSet(HashTreePSet.empty(), bindings);
        }

        static BindSet union(BindSet first, BindSet other) {
            if (first.isEmpty()) {
                return other;
            } else if (other.isEmpty()) {
                return first;
            }
            return new BindSet(first.trueBindings.plusAll(other.trueBindings),
                               first.falseBindings.plusAll(other.falseBindings));
        }
    }

    /**
     * Tracks exploration state of an expression.
     */
    private static class State {

        private final PSet<String> labelsInScope;
        private final boolean breakAllowed;
        private final boolean continueAllowed;

        /**
         * @param labelsInScope Labels to which breaking is ok, because they're
         *                      in a strict descendant of the toplevel node.
         */
        State(PSet<String> labelsInScope, boolean breakAllowed, boolean continueAllowed) {
            this.labelsInScope = labelsInScope;
            this.breakAllowed = breakAllowed;
            this.continueAllowed = continueAllowed;
        }

        public PSet<String> getLabelsInScope() {
            return labelsInScope;
        }

        public boolean isBreakAllowed() {
            return breakAllowed;
        }

        public boolean isContinueAllowed() {
            return continueAllowed;
        }

        public State withLabel(String label) {
            return new State(labelsInScope.plus(label), breakAllowed, continueAllowed);
        }
    }
}
