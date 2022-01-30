/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.rules;

import static net.sourceforge.pmd.internal.util.xml.SchemaConstants.PROPERTY_TYPE;
import static net.sourceforge.pmd.internal.util.xml.SchemaConstants.PROPERTY_VALUE;
import static net.sourceforge.pmd.internal.util.xml.XmlErrorMessages.ERR__INVALID_LANG_VERSION;
import static net.sourceforge.pmd.internal.util.xml.XmlErrorMessages.ERR__INVALID_LANG_VERSION_NO_NAMED_VERSION;
import static net.sourceforge.pmd.internal.util.xml.XmlErrorMessages.ERR__PROPERTY_DOES_NOT_EXIST;
import static net.sourceforge.pmd.internal.util.xml.XmlErrorMessages.ERR__UNSUPPORTED_VALUE_ATTRIBUTE;
import static net.sourceforge.pmd.internal.util.xml.XmlErrorMessages.IGNORED__DUPLICATE_PROPERTY_SETTER;
import static net.sourceforge.pmd.internal.util.xml.XmlUtil.getSingleChildIn;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.RuleSetReference;
import net.sourceforge.pmd.annotation.InternalApi;
import net.sourceforge.pmd.internal.util.xml.SchemaConstants;
import net.sourceforge.pmd.internal.util.xml.XmlErrorMessages;
import net.sourceforge.pmd.internal.util.xml.XmlUtil;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.rule.RuleReference;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyTypeId;
import net.sourceforge.pmd.properties.PropertyTypeId.BuilderAndMapper;
import net.sourceforge.pmd.properties.xml.XmlMapper;
import net.sourceforge.pmd.util.ResourceLoader;
import net.sourceforge.pmd.util.StringUtil;

import com.github.oowekyala.ooxml.DomUtils;
import com.github.oowekyala.ooxml.messages.XmlErrorReporter;


/**
 * Builds rules from rule XML nodes.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
@InternalApi
@Deprecated
public class RuleFactory {

    private static final String DEPRECATED = "deprecated";
    private static final String NAME = "name";
    private static final String LANGUAGE = "language";
    private static final String MESSAGE = "message";
    private static final String EXTERNAL_INFO_URL = "externalInfoUrl";
    private static final String MINIMUM_LANGUAGE_VERSION = "minimumLanguageVersion";
    private static final String MAXIMUM_LANGUAGE_VERSION = "maximumLanguageVersion";
    private static final String SINCE = "since";
    private static final String PROPERTIES = "properties";
    public static final String PRIORITY = "priority";

    public static final String RULE = "rule";
    private static final String EXAMPLE = "example";
    public static final String DESCRIPTION = "description";
    private static final String CLASS = "class";

    private final ResourceLoader resourceLoader;

    /**
     * @deprecated Use {@link #RuleFactory(ResourceLoader)} instead.
     */
    @Deprecated
    public RuleFactory() {
        this(new ResourceLoader());
    }

    /**
     * @param resourceLoader The resource loader to load the rule from jar
     */
    public RuleFactory(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Decorates a referenced rule with the metadata that are overridden in the given rule element.
     *
     * <p>Declaring a property in the overriding element throws an exception (the property must exist in the referenced
     * rule).
     *
     * @param referencedRule   Referenced rule
     * @param ruleSetReference the ruleset, where the referenced rule is defined
     * @param ruleElement      Element overriding some metadata about the rule
     * @param err              Error reporter
     *
     * @return A rule reference to the referenced rule
     */
    public RuleReference decorateRule(Rule referencedRule, RuleSetReference ruleSetReference, Element ruleElement, XmlErrorReporter err) {
        RuleReference ruleReference = new RuleReference(referencedRule, ruleSetReference);

        DomUtils.getAttributeOpt(ruleElement, DEPRECATED).map(Boolean::parseBoolean).ifPresent(ruleReference::setDeprecated);
        DomUtils.getAttributeOpt(ruleElement, NAME).ifPresent(ruleReference::setName);
        DomUtils.getAttributeOpt(ruleElement, MESSAGE).ifPresent(ruleReference::setMessage);
        DomUtils.getAttributeOpt(ruleElement, EXTERNAL_INFO_URL).ifPresent(ruleReference::setExternalInfoUrl);

        for (Element node : DomUtils.elementsIn(ruleElement)) {
            switch (node.getNodeName()) {
            case DESCRIPTION:
                ruleReference.setDescription(XmlUtil.parseTextNode(node));
                break;
            case EXAMPLE:
                ruleReference.addExample(XmlUtil.parseTextNode(node));
                break;
            case PRIORITY:
                ruleReference.setPriority(RulePriority.valueOf(Integer.parseInt(XmlUtil.parseTextNode(node))));
                break;
            case PROPERTIES:
                setPropertyValues(ruleReference, node, err);
                break;
            default:
                throw err.error(
                    node,
                    XmlErrorMessages.ERR__UNEXPECTED_ELEMENT_IN,
                    "rule " + ruleReference.getName()
                );
            }
        }

        return ruleReference;
    }

    /**
     * Parses a rule element and returns a new rule instance.
     *
     * <p>Notes: The ruleset name is not set here. Exceptions raised from this method indicate invalid XML structure,
     * with regards to the expected schema, while RuleBuilder validates the semantics.
     *
     * @param ruleElement The rule element to parse
     *
     * @return A new instance of the rule described by this element
     *
     * @throws IllegalArgumentException if the element doesn't describe a valid rule.
     */
    public Rule buildRule(Element ruleElement, XmlErrorReporter err) {

        Rule rule;
        try {
            String clazz = getNonBlankAttribute(ruleElement, err, CLASS);
            rule = resourceLoader.loadRuleFromClassPath(clazz);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw err.fatal(ruleElement.getAttributeNode(CLASS), e);
        }

        rule.setName(getNonBlankAttribute(ruleElement, err, NAME));
        if (rule.getLanguage() == null) {
            setLanguage(ruleElement, err, rule);
        }
        Language language = rule.getLanguage();
        assert language != null;

        rule.setMinimumLanguageVersion(getLanguageVersion(ruleElement, err, language, MINIMUM_LANGUAGE_VERSION));
        rule.setMaximumLanguageVersion(getLanguageVersion(ruleElement, err, language, MAXIMUM_LANGUAGE_VERSION));
        checkVersionsAreOrdered(ruleElement, err, rule);

        DomUtils.getAttributeOpt(ruleElement, SINCE).ifPresent(rule::setSince);
        DomUtils.getAttributeOpt(ruleElement, MESSAGE).ifPresent(rule::setMessage);
        DomUtils.getAttributeOpt(ruleElement, EXTERNAL_INFO_URL).ifPresent(rule::setExternalInfoUrl);
        rule.setDeprecated(SchemaConstants.DEPRECATED.getAsBooleanAttr(ruleElement, false));

        for (Element node : DomUtils.elementsIn(ruleElement)) {
            switch (node.getNodeName()) {
            case DESCRIPTION:
                rule.setDescription(XmlUtil.parseTextNode(node));
                break;
            case EXAMPLE:
                rule.addExample(XmlUtil.parseTextNode(node));
                break;
            case PRIORITY:
                RulePriority rp = parsePriority(err, node, XmlUtil.parseTextNode(node));
                rule.setPriority(rp);
                break;
            case PROPERTIES:
                parsePropertiesForDefinitions(rule, node, err);
                setPropertyValues(rule, node, err);
                break;
            default:
                throw err.error(node,
                                XmlErrorMessages.ERR__UNEXPECTED_ELEMENT_IN,
                                "rule " + ruleElement.getAttribute(NAME));
            }
        }

        return rule;
    }

    private void checkVersionsAreOrdered(Element ruleElement, XmlErrorReporter err, Rule rule) {
        if (rule.getMinimumLanguageVersion() != null && rule.getMaximumLanguageVersion() != null
            && rule.getMinimumLanguageVersion().compareTo(rule.getMaximumLanguageVersion()) > 0) {
            throw err.fatal(
                ruleElement.getAttributeNode(MINIMUM_LANGUAGE_VERSION),
                XmlErrorMessages.ERR__INVALID_VERSION_RANGE,
                rule.getMinimumLanguageVersion(),
                rule.getMaximumLanguageVersion()
            );
        }
    }


    private @NonNull RulePriority parsePriority(XmlErrorReporter err, Element node, String trim) {
        try {
            int i = Integer.parseInt(trim.trim());
            RulePriority rp = RulePriority.valueOfNullable(i);
            if (rp == null) {
                err.warn(node, XmlErrorMessages.WARN__INVALID_PRIORITY_VALUE, i);
                return RulePriority.MEDIUM;
            } else {
                return rp;
            }
        } catch (NumberFormatException e) {
            err.warn(node, XmlErrorMessages.WARN__INVALID_PRIORITY_VALUE, trim);
            return RulePriority.MEDIUM;
        }
    }

    private LanguageVersion getLanguageVersion(Element ruleElement, XmlErrorReporter err, Language language, String attrName) {
        if (ruleElement.hasAttribute(attrName)) {
            String attrValue = ruleElement.getAttribute(attrName);
            LanguageVersion version = language.getVersion(attrValue);
            if (version == null) {
                String supportedVersions = language.getVersions().stream()
                                                   .map(LanguageVersion::getVersion)
                                                   .filter(it -> !it.isEmpty())
                                                   .map(StringUtil::inSingleQuotes)
                                                   .collect(Collectors.joining(", "));
                String message = supportedVersions.isEmpty()
                                 ? ERR__INVALID_LANG_VERSION_NO_NAMED_VERSION
                                 : ERR__INVALID_LANG_VERSION;
                throw err.fatal(
                    ruleElement.getAttributeNode(attrName),
                    message,
                    attrValue,
                    language.getTerseName(),
                    supportedVersions
                );
            }
            return version;
        }
        return null;
    }

    private void setLanguage(Element ruleElement, XmlErrorReporter err, Rule rule) {
        String langId = getNonBlankAttribute(ruleElement, err, LANGUAGE);
        Language lang = LanguageRegistry.findLanguageByTerseName(langId);
        if (lang == null) {
            throw err.fatal(ruleElement.getAttributeNode(LANGUAGE), "Invalid language ''{0}'', possible values are {1}", langId, supportedLanguages());
        }
        rule.setLanguage(lang);
    }

    private @NonNull String supportedLanguages() {
        return LanguageRegistry.getLanguages().stream().map(Language::getTerseName).map(StringUtil::inSingleQuotes).collect(Collectors.joining(", "));
    }

    private @NonNull String getNonBlankAttribute(Element ruleElement, XmlErrorReporter err, String attrName) {
        String clazz = ruleElement.getAttribute(attrName);
        if (StringUtils.isBlank(clazz)) {
            Attr attr = ruleElement.getAttributeNode(attrName);
            if (attr == null) {
                throw err.fatal(ruleElement, "Missing {0} attribute", attrName);
            } else {
                throw err.fatal(attr, "This attribute may not be blank");
            }
        }
        return clazz;
    }

    /**
     * Parses the properties node and adds property definitions to the builder. Doesn't care for value overriding, that
     * will be handled after the rule instantiation.
     *
     * @param rule        Rule builder
     * @param propertiesNode Node to parse
     * @param err            Error reporter
     */
    private void parsePropertiesForDefinitions(Rule rule, Element propertiesNode, @NonNull XmlErrorReporter err) {
        for (Element child : SchemaConstants.PROPERTY_ELT.getElementChildrenNamedReportOthers(propertiesNode, err)) {
            if (isPropertyDefinition(child)) {
                rule.definePropertyDescriptor(parsePropertyDefinition(child, err));
            }
        }
    }

    /**
     * Overrides the rule's properties with the values defined in the element.
     *
     * @param rule          The rule
     * @param propertiesElt The {@literal <properties>} element
     */
    private void setPropertyValues(Rule rule, Element propertiesElt, XmlErrorReporter err) {
        Set<String> overridden = new HashSet<>();

        for (Element element : SchemaConstants.PROPERTY_ELT.getElementChildrenNamedReportOthers(propertiesElt, err)) {
            String name = SchemaConstants.NAME.getAttributeOrThrow(element, err);
            if (!overridden.add(name)) {
                err.warn(element, IGNORED__DUPLICATE_PROPERTY_SETTER, name);
                continue;
            }

            PropertyDescriptor<?> desc = rule.getPropertyDescriptor(name);
            if (desc == null) {
                // todo just warn and ignore
                throw err.error(element, ERR__PROPERTY_DOES_NOT_EXIST, name, rule.getName());
            }
            setRulePropertyCapture(rule, desc, element, err);
        }
    }

    private <T> void setRulePropertyCapture(Rule rule, PropertyDescriptor<T> descriptor, Element propertyElt, XmlErrorReporter err) {
        T value = parsePropertyValue(propertyElt, err, descriptor.xmlMapper());
        rule.setProperty(descriptor, value);
    }

    /**
     * Finds out if the property element defines a property.
     *
     * @param node Property element
     *
     * @return True if this element defines a new property, false if this is just stating a value
     */
    private static boolean isPropertyDefinition(Element node) {
        return SchemaConstants.PROPERTY_TYPE.hasAttribute(node);
    }

    /**
     * Parses a property definition node and returns the defined property descriptor.
     *
     * @param propertyElement Property node to parse
     * @param err             Error reporter
     *
     * @return The property descriptor
     */
    private static PropertyDescriptor<?> parsePropertyDefinition(Element propertyElement, XmlErrorReporter err) {

        String typeId = SchemaConstants.PROPERTY_TYPE.getAttributeOrThrow(propertyElement, err);

        PropertyTypeId factory = PropertyTypeId.lookupMnemonic(typeId);
        if (factory == null) {
            throw err.fatal(
                PROPERTY_TYPE.getAttributeNode(propertyElement),
                "Unsupported property type ''{0}''",
                typeId
            );
        }

        return propertyDefCapture(propertyElement, err, factory.getBuilderUtils());
    }

    private static <T> PropertyDescriptor<T> propertyDefCapture(Element propertyElement,
                                                                XmlErrorReporter err,
                                                                BuilderAndMapper<T> factory) {
        // TODO support constraints like numeric range

        String name = SchemaConstants.NAME.getNonBlankAttributeOrThrow(propertyElement, err);
        String description = SchemaConstants.DESCRIPTION.getNonBlankAttributeOrThrow(propertyElement, err);


        try {
            return factory.newBuilder(name)
                          .desc(description)
                          .defaultValue(parsePropertyValue(propertyElement, err, factory.getXmlMapper()))
                          .build();

        } catch (IllegalArgumentException e) {
            // builder threw, rethrow with XML location
            throw err.error(propertyElement, e.getMessage());
        }
    }

    private static <T> T parsePropertyValue(Element propertyElt, XmlErrorReporter err, XmlMapper<T> syntax) {
        @Nullable String defaultAttr = PROPERTY_VALUE.getAttributeOpt(propertyElt);
        if (defaultAttr != null) {
            Attr attrNode = PROPERTY_VALUE.getAttributeNode(propertyElt);

            // the attribute syntax could be deprecated.
            //   err.warn(attrNode,
            //            WARN__DEPRECATED_USE_OF_ATTRIBUTE,
            //            PROPERTY_VALUE.xmlName(),
            //            String.join("\nor\n", syntax.getExamples()));

            try {
                return syntax.fromString(defaultAttr);
            } catch (IllegalArgumentException e) {
                throw err.error(attrNode, e);
            } catch (UnsupportedOperationException e) {
                throw err.error(attrNode,
                                ERR__UNSUPPORTED_VALUE_ATTRIBUTE,
                                String.join("\nor\n", syntax.getExamples()));
            }

        } else {
            Element child = getSingleChildIn(propertyElt, true, err, syntax.getReadElementNames());
            // this will report the correct error if any
            return syntax.fromXml(child, err);
        }
    }
}
