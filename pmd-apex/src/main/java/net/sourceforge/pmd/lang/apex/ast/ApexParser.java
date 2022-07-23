/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.apex.ast;

import net.sourceforge.pmd.annotation.InternalApi;
import net.sourceforge.pmd.lang.apex.ApexJorjeLogging;
import net.sourceforge.pmd.lang.apex.ApexLanguageProcessor;
import net.sourceforge.pmd.lang.ast.ParseException;
import net.sourceforge.pmd.lang.ast.Parser;

import apex.jorje.data.Locations;
import apex.jorje.semantic.ast.compilation.Compilation;

@InternalApi
public final class ApexParser implements Parser {

    private final ApexLanguageProcessor proc;

    public ApexParser(ApexLanguageProcessor proc) {
        this.proc = proc;
        ApexJorjeLogging.disableLogging();
        Locations.useIndexFactory();
    }

    @Override
    public ASTApexFile parse(final ParserTask task) {
        try {

            final Compilation astRoot = CompilerService.INSTANCE.parseApex(task.getTextDocument());

            if (astRoot == null) {
                throw new ParseException("Couldn't parse the source - there is not root node - Syntax Error??");
            }

            final ApexTreeBuilder treeBuilder = new ApexTreeBuilder(task, proc);
            return treeBuilder.buildTree(astRoot);
        } catch (apex.jorje.services.exception.ParseException e) {
            throw new ParseException(e);
        }
    }
}
