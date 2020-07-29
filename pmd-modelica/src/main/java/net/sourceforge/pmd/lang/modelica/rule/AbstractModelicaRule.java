/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.modelica.rule;

import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.ast.AstProcessingStage;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.modelica.ast.ModelicaNode;
import net.sourceforge.pmd.lang.modelica.ast.ModelicaParserVisitor;
import net.sourceforge.pmd.lang.modelica.internal.ModelicaProcessingStage;
import net.sourceforge.pmd.lang.rule.AbstractRule;
import net.sourceforge.pmd.lang.rule.ImmutableLanguage;

/**
 * Base class for rules for Modelica language.
 */
public abstract class AbstractModelicaRule extends AbstractRule implements ModelicaParserVisitor, ImmutableLanguage {

    @Override
    public void apply(Node target, RuleContext ctx) {
        ((ModelicaNode) target).jjtAccept(this, ctx);
    }

    @Override
    public Object visit(ModelicaNode node, Object data) {
        for (ModelicaNode child : node.children()) {
            child.jjtAccept(this, data);
        }
        return data;
    }

    @Override
    public boolean dependsOn(AstProcessingStage<?> stage) {
        if (!(stage instanceof ModelicaProcessingStage)) {
            throw new IllegalArgumentException("Processing stage wasn't a Modelica one: " + stage);
        }
        return true;
    }

}
