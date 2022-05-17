/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.test.schema;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.annotation.Experimental;

import com.github.oowekyala.ooxml.messages.NiceXmlMessageSpec;
import com.github.oowekyala.ooxml.messages.OoxmlFacade;
import com.github.oowekyala.ooxml.messages.PositionedXmlDoc;
import com.github.oowekyala.ooxml.messages.XmlException;
import com.github.oowekyala.ooxml.messages.XmlMessageReporter;
import com.github.oowekyala.ooxml.messages.XmlMessageReporterBase;
import com.github.oowekyala.ooxml.messages.XmlPosition;
import com.github.oowekyala.ooxml.messages.XmlPositioner;
import com.github.oowekyala.ooxml.messages.XmlSeverity;


/**
 * Entry point to parse a test file.
 *
 * @author Clément Fournier
 */
@Experimental
public class TestSchemaParser {

    private final TestSchemaVersion version;

    public TestSchemaParser(TestSchemaVersion version) {
        this.version = version;
    }

    /**
     * Entry point to parse a test file.
     *
     * @param rule        Rule which owns the tests
     * @param inputSource Where to access the test file to parse
     *
     * @return A test collection, possibly incomplete
     *
     * @throws IOException  If parsing throws this
     * @throws XmlException If parsing throws this
     */
    public TestCollection parse(Rule rule, InputSource inputSource) throws IOException, XmlException {
        OoxmlFacade ooxml = new OoxmlFacade();
        PositionedXmlDoc doc = ooxml.parse(newDocumentBuilder(), inputSource);

        try (PmdXmlReporterImpl err = new PmdXmlReporterImpl(ooxml, doc.getPositioner())) {
            TestCollection collection = version.getParserImpl().parseDocument(rule, doc.getDocument(), err);
            if (err.hasError()) {
                // todo maybe add a way not to throw here
                throw new IllegalStateException("Errors were encountered while parsing XML tests");
            }
            return collection;
        }
    }

    interface PmdXmlReporter extends XmlMessageReporter<Reporter> {

        boolean hasError();

        PmdXmlReporter newScope();
    }

    private static class PmdXmlReporterImpl
        extends XmlMessageReporterBase<Reporter>
        implements PmdXmlReporter {

        private boolean hasError;

        protected PmdXmlReporterImpl(OoxmlFacade ooxml,
                                     XmlPositioner positioner) {
            super(ooxml, positioner);
        }

        @Override
        protected Reporter create2ndStage(XmlPosition position, XmlPositioner positioner, Consumer<XmlException> handleEx) {
            return new Reporter(position, positioner, ooxml, handleEx.andThen(this::consumeEx));
        }

        protected void consumeEx(XmlException e) {
            hasError |= e.getSeverity() == XmlSeverity.ERROR;
        }

        @Override
        public PmdXmlReporter newScope() {
            return new PmdXmlReporterImpl(ooxml, positioner) {
                @Override
                protected void consumeEx(XmlException e) {
                    super.consumeEx(e);
                    PmdXmlReporterImpl.this.consumeEx(e);
                }
            };
        }

        @Override
        public boolean hasError() {
            return hasError;
        }

        @Override
        public void close() {
        }
    }

    private DocumentBuilder newDocumentBuilder() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setSchema(version.getSchema());
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    static class Reporter {

        private final XmlPosition position;
        private final XmlPositioner positioner;
        private final OoxmlFacade ooxml;

        private final Consumer<XmlException> handler;

        private Reporter(XmlPosition position, XmlPositioner positioner, OoxmlFacade ooxml, Consumer<XmlException> handler) {
            this.position = position;
            this.positioner = positioner;
            this.ooxml = ooxml;
            this.handler = handler;
        }

        public void warn(String messageFormat, Object... args) {
            reportImpl(XmlSeverity.WARNING, MessageFormat.format(messageFormat, args));

        }

        public void error(String messageFormat, Object... args) {
            reportImpl(XmlSeverity.ERROR, MessageFormat.format(messageFormat, args));
        }

        private void reportImpl(XmlSeverity severity, String formattedMessage) {
            NiceXmlMessageSpec spec =
                new NiceXmlMessageSpec(position, formattedMessage)
                    .withSeverity(severity);
            String fullMessage = ooxml.getFormatter().formatSpec(ooxml, spec, positioner);
            XmlException ex = new XmlException(spec, fullMessage);
            handler.accept(ex);
        }
    }

}
