/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.ast;

@Deprecated
public class ASTPrimarySuffix extends AbstractJavaTypeNode {

    private boolean isArguments;
    private boolean isArrayDereference;

    ASTPrimarySuffix(int id) {
        super(id);
    }

    public boolean isArrayDereference() {
        return isArrayDereference;
    }


    public boolean isArguments() {
        return this.isArguments;
    }

    /**
     * Get the number of arguments for this primary suffix. One should call
     * {@link #isArguments()} to see if there are arguments. If this method is
     * called when there are no arguments it returns <code>-1</code>.
     *
     * @return A non-negative argument number when there are arguments,
     *     <code>-1</code> otherwise.
     */
    public int getArgumentCount() {
        if (!this.isArguments()) {
            return -1;
        }
        return ((ASTArguments) getChild(getNumChildren() - 1)).size();
    }


    @Override
    protected <P, R> R acceptVisitor(JavaVisitor<? super P, ? extends R> visitor, P data) {
        throw new UnsupportedOperationException("Node was removed from grammar");
    }
}
