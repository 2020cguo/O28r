/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.xml.rule;

import static net.sourceforge.pmd.lang.xml.XmlParserOptions.LOOKUP_DESCRIPTOR_DTD;

import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.ParserOptions;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.rule.AbstractRule;
import net.sourceforge.pmd.lang.rule.ImmutableLanguage;
import net.sourceforge.pmd.lang.xml.XmlLanguageModule;
import net.sourceforge.pmd.lang.xml.XmlParserOptions;
import net.sourceforge.pmd.lang.xml.ast.XmlNode;
import net.sourceforge.pmd.properties.PropertyDescriptor;

/**
 * This is a base class for XML Java bases rules. Subclasses should override
 * {@link #visit(XmlNode, RuleContext)} and can call <code>super</code> to visit
 * children.
 */
public class AbstractXmlRule extends AbstractRule implements ImmutableLanguage {

    @Deprecated
    public static final PropertyDescriptor<Boolean> COALESCING_DESCRIPTOR = XmlParserOptions.COALESCING_DESCRIPTOR;
    @Deprecated
    public static final PropertyDescriptor<Boolean> EXPAND_ENTITY_REFERENCES_DESCRIPTOR = XmlParserOptions.EXPAND_ENTITY_REFERENCES_DESCRIPTOR;
    @Deprecated
    public static final PropertyDescriptor<Boolean> IGNORING_COMMENTS_DESCRIPTOR = XmlParserOptions.IGNORING_COMMENTS_DESCRIPTOR;
    @Deprecated
    public static final PropertyDescriptor<Boolean> IGNORING_ELEMENT_CONTENT_WHITESPACE_DESCRIPTOR = XmlParserOptions.IGNORING_ELEMENT_CONTENT_WHITESPACE_DESCRIPTOR;
    @Deprecated
    public static final PropertyDescriptor<Boolean> NAMESPACE_AWARE_DESCRIPTOR = XmlParserOptions.NAMESPACE_AWARE_DESCRIPTOR;
    @Deprecated
    public static final PropertyDescriptor<Boolean> VALIDATING_DESCRIPTOR = XmlParserOptions.VALIDATING_DESCRIPTOR;
    @Deprecated
    public static final PropertyDescriptor<Boolean> XINCLUDE_AWARE_DESCRIPTOR = XmlParserOptions.XINCLUDE_AWARE_DESCRIPTOR;

    public AbstractXmlRule() {
        super.setLanguage(LanguageRegistry.getLanguage(XmlLanguageModule.NAME));
        defineProperties();
    }

    protected AbstractXmlRule(Language language) {
        super.setLanguage(language);
        defineProperties();
    }

    private void defineProperties() {
        definePropertyDescriptor(COALESCING_DESCRIPTOR);
        definePropertyDescriptor(EXPAND_ENTITY_REFERENCES_DESCRIPTOR);
        definePropertyDescriptor(IGNORING_COMMENTS_DESCRIPTOR);
        definePropertyDescriptor(IGNORING_ELEMENT_CONTENT_WHITESPACE_DESCRIPTOR);
        definePropertyDescriptor(NAMESPACE_AWARE_DESCRIPTOR);
        definePropertyDescriptor(VALIDATING_DESCRIPTOR);
        definePropertyDescriptor(XINCLUDE_AWARE_DESCRIPTOR);
        definePropertyDescriptor(LOOKUP_DESCRIPTOR_DTD);
    }

    @Override
    public ParserOptions getParserOptions() {
        return new XmlParserOptions(this);
    }

    @Override
    public void apply(Node target, RuleContext ctx) {
        visit((XmlNode) target, ctx);
    }

    protected void visit(XmlNode node, RuleContext ctx) {
        final int numChildren = node.getNumChildren();
        for (int i = 0; i < numChildren; i++) {
            XmlNode child = (XmlNode) node.getChild(i);
            visit(child, ctx);
        }
    }
}
