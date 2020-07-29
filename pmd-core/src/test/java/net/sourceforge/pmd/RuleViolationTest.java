/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Ignore;
import org.junit.Test;

import net.sourceforge.pmd.lang.ast.DummyNode;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.rule.MockRule;
import net.sourceforge.pmd.lang.rule.ParametricRuleViolation;

import junit.framework.JUnit4TestAdapter;

public class RuleViolationTest extends PmdContextualizedTest {

    @Test
    public void testConstructor1() {
        Rule rule = makeMockRule();
        RuleContext ctx = new RuleContext();
        ctx.setSourceCodeFile(new File("filename"));
        DummyNode s = new DummyNode();
        s.setCoords(2, 1, 2, 3);
        RuleViolation r = new ParametricRuleViolation<Node>(rule, ctx, s, rule.getMessage());
        assertEquals("object mismatch", rule, r.getRule());
        assertEquals("line number is wrong", 2, r.getBeginLine());
        assertEquals("filename is wrong", "filename", r.getFilename());
    }

    @NonNull
    public MockRule makeMockRule() {
        return dummyRule(new MockRule("name", "desc", "msg", "rulesetname"));
    }

    @Test
    public void testConstructor2() {
        Rule rule = makeMockRule();
        RuleContext ctx = new RuleContext();
        ctx.setSourceCodeFile(new File("filename"));
        DummyNode s = new DummyNode();
        s.setCoords(2, 1, 2, 3);
        RuleViolation r = new ParametricRuleViolation<Node>(rule, ctx, s, "description");
        assertEquals("object mismatch", rule, r.getRule());
        assertEquals("line number is wrong", 2, r.getBeginLine());
        assertEquals("filename is wrong", "filename", r.getFilename());
        assertEquals("description is wrong", "description", r.getDescription());
    }

    @Test
    public void testComparatorWithDifferentFilenames() {
        Rule rule = makeMockRule();
        RuleViolationComparator comp = RuleViolationComparator.INSTANCE;
        RuleContext ctx = new RuleContext();
        ctx.setSourceCodeFile(new File("filename1"));
        DummyNode s = new DummyNode();
        s.setCoords(10, 1, 11, 3);
        RuleViolation r1 = new ParametricRuleViolation<Node>(rule, ctx, s, "description");
        ctx.setSourceCodeFile(new File("filename2"));
        DummyNode s1 = new DummyNode();
        s1.setCoords(10, 1, 11, 3);
        RuleViolation r2 = new ParametricRuleViolation<Node>(rule, ctx, s1, "description");
        assertEquals(-1, comp.compare(r1, r2));
        assertEquals(1, comp.compare(r2, r1));
    }

    @Test
    public void testComparatorWithSameFileDifferentLines() {
        Rule rule = makeMockRule();
        RuleViolationComparator comp = RuleViolationComparator.INSTANCE;
        RuleContext ctx = new RuleContext();
        ctx.setSourceCodeFile(new File("filename"));
        DummyNode s = new DummyNode();
        s.setCoords(10, 1, 15, 10);
        DummyNode s1 = new DummyNode();
        s1.setCoords(20, 1, 25, 10);
        RuleViolation r1 = new ParametricRuleViolation<Node>(rule, ctx, s, "description");
        RuleViolation r2 = new ParametricRuleViolation<Node>(rule, ctx, s1, "description");
        assertTrue(comp.compare(r1, r2) < 0);
        assertTrue(comp.compare(r2, r1) > 0);
    }

    @Ignore
    @Test
    public void testComparatorWithSameFileSameLines() {
        Rule rule = makeMockRule();
        RuleViolationComparator comp = RuleViolationComparator.INSTANCE;
        RuleContext ctx = new RuleContext();
        ctx.setSourceCodeFile(new File("filename"));
        DummyNode s = new DummyNode();
        s.setCoords(10, 1, 15, 10);
        DummyNode s1 = new DummyNode();
        s.setCoords(10, 1, 15, 10);
        RuleViolation r1 = new ParametricRuleViolation<Node>(rule, ctx, s, "description");
        RuleViolation r2 = new ParametricRuleViolation<Node>(rule, ctx, s1, "description");
        assertEquals(1, comp.compare(r1, r2));
        assertEquals(1, comp.compare(r2, r1));
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(RuleViolationTest.class);
    }
}
