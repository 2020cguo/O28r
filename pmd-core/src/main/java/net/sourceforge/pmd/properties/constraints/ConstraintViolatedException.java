/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.properties.constraints;

/**
 * Thrown when a property constraint is violated. Detected while parsing
 * values from XML.
 *
 * @author Clément Fournier
 */
public class ConstraintViolatedException extends IllegalArgumentException {

    public ConstraintViolatedException(String message) {
        super(message);
    }
}
