/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.cpd;

import net.sourceforge.pmd.cpd.internal.JavaCCTokenizer;
import net.sourceforge.pmd.lang.TokenManager;
import net.sourceforge.pmd.lang.ast.CharStream;
import net.sourceforge.pmd.lang.ast.impl.javacc.CharStreamFactory;
import net.sourceforge.pmd.lang.ast.impl.javacc.JavaccToken;
import net.sourceforge.pmd.lang.document.TextDocument;
import net.sourceforge.pmd.lang.jsp.ast.JspTokenKinds;

public class JSPTokenizer extends JavaCCTokenizer {

    @Override
    protected TokenManager<JavaccToken> makeLexerImpl(CharStream sourceCode) {
        return JspTokenKinds.newTokenManager(sourceCode);
    }

    @Override
    protected CharStream makeCharStream(TextDocument sourceCode) {
        return CharStreamFactory.javaCharStream(sourceCode);
    }
}
