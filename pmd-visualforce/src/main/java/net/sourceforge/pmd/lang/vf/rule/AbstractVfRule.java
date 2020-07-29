/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.vf.rule;

import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.rule.AbstractRule;
import net.sourceforge.pmd.lang.rule.ImmutableLanguage;
import net.sourceforge.pmd.lang.vf.ast.VfNode;
import net.sourceforge.pmd.lang.vf.ast.VfParserVisitor;

public abstract class AbstractVfRule extends AbstractRule implements VfParserVisitor, ImmutableLanguage {

    @Override
    public void apply(Node target, RuleContext ctx) {
        ((VfNode) target).jjtAccept(this, ctx);
    }

}
