/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.properties;

import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.properties.xml.XmlMapper;


/**
 * Describes a property of a rule or a renderer. Provides validation,
 * serialization, and default values for a datatype {@code <T>}.
 * Property descriptors are immutable and can be shared freely.
 *
 * <p>Usage of this API is described on {@link PropertyFactory}.
 *
 * <h1>Upcoming API changes to the properties framework</h1>
 * see <a href="https://github.com/pmd/pmd/issues/1432">pmd/pmd#1432</a>
 *
 * @param <T> Type of the property's value.
 *
 * @see PropertyFactory
 * @see PropertyBuilder
 *
 * @author Brian Remedios
 * @author Clément Fournier
 * @version 7.0.0
 */
public interface PropertyDescriptor<T> {

    /**
     * The name of the property without spaces as it serves as the key into the property map.
     *
     * @return String
     */
    String name();


    /**
     * Describes the property and the role it plays within the rule it is specified for. Could be used in a tooltip.
     *
     * @return String
     */
    String description();


    /**
     * Default value to use when the user hasn't specified one or when they wish to revert to a known-good state.
     *
     * @return Object
     */
    T defaultValue();


    /**
     * Returns the strategy used to read and write this property to XML.
     * May support strings too.
     */
    XmlMapper<T> xmlStrategy();


    /**
     * Validation function that returns a diagnostic error message for a sample property value. Returns null if the
     * value is acceptable.
     *
     * @param value The value to check.
     *
     * @return A diagnostic message.
     *
     * @deprecated PMD 7.0.0 will change the return type to {@code Optional<String>}
     */
    @Deprecated
    default String errorFor(T value) {
        return null;
    }


    /**
     * Returns the type ID which was used to define this property. Returns
     * null if this property was defined in Java code and not in XML.
     */
    default @Nullable PropertyTypeId getTypeId() {
        return null;
    }


    /**
     * Returns the value represented by this string.
     *
     * @param propertyString The string to parse
     *
     * @return The value represented by the string
     *
     * @throws IllegalArgumentException if the given string cannot be parsed
     * @throws UnsupportedOperationException If operation is not supported
     * @deprecated PMD 7.0.0 will use a more powerful scheme to represent values than
     * simple strings, this method won't be general enough
     */
    @Deprecated
    default T valueFrom(String propertyString) throws IllegalArgumentException {
        return xmlStrategy().fromString(propertyString);
    }


    /**
     * Formats the object onto a string suitable for storage within the property map.
     *
     * @param value Object
     *
     * @return String
     *
     * @deprecated PMD 7.0.0 will use a more powerful scheme to represent values than
     * simple strings, this method won't be general enough
     */
    @Deprecated
    default String asDelimitedString(T value) {
        return xmlStrategy().toString(value);
    }


}
