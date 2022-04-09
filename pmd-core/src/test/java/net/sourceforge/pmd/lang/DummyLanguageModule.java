/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang;

import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.lang.ast.DummyRoot;
import net.sourceforge.pmd.lang.ast.Parser;
import net.sourceforge.pmd.reporting.ViolationDecorator;

/**
 * Dummy language used for testing PMD.
 */
public class DummyLanguageModule extends BaseLanguageModule {

    public static final String NAME = "Dummy";
    public static final String TERSE_NAME = "dummy";

    public DummyLanguageModule() {
        super(NAME, null, TERSE_NAME, "dummy");
        addVersion("1.0", new Handler());
        addVersion("1.1", new Handler());
        addVersion("1.2", new Handler());
        addVersion("1.3", new Handler());
        addVersion("1.4", new Handler());
        addVersion("1.5", new Handler(), "5");
        addVersion("1.6", new Handler(), "6");
        addDefaultVersion("1.7", new Handler(), "7");
        addVersion("1.8", new Handler(), "8");
        addVersion("1.9-throws", new HandlerWithParserThatThrows());
    }

    public static class Handler extends AbstractPmdLanguageVersionHandler {


        @Override
        public Parser getParser() {
            return task -> {
                DummyRoot node = new DummyRoot();
                node.setCoords(1, 1, 1, 1);
                node.setImage("Foo");
                node.withFileName(task.getFileDisplayName());
                node.withLanguage(task.getLanguageVersion());
                node.withSourceText(task.getSourceText());
                return node;
            };
        }

        @Override
        public ViolationDecorator getViolationDecorator() {
            return (node, data) -> data.put(RuleViolation.PACKAGE_NAME, "foo");
        }
    }

    public static class HandlerWithParserThatThrows extends Handler {
        @Override
        public Parser getParser() {
            return task -> {
                throw new AssertionError("test error while parsing");
            };
        }
    }
}
