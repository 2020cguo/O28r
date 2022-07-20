/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.properties.AbstractPropertySource;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyFactory;
import net.sourceforge.pmd.util.CollectionUtil;

/**
 * @author Clément Fournier
 */
public class LanguagePropertyBundle extends AbstractPropertySource {

    public static final PropertyDescriptor<String> SUPPRESS_MARKER
        = PropertyFactory.stringProperty("suppressMarker")
                         .desc("Suppress marker to use in comments")
                         .defaultValue(PMDConfiguration.DEFAULT_SUPPRESS_MARKER)
                         .build();
    public static final String LANGUAGE_VERSION = "version";

    private final PropertyDescriptor<LanguageVersion> languageVersion;
    private final Language language;

    public LanguagePropertyBundle(Language language) {
        this.language = language;

        definePropertyDescriptor(SUPPRESS_MARKER);

        languageVersion =
            PropertyFactory.enumProperty(
                               LANGUAGE_VERSION,
                               CollectionUtil.associateBy(language.getVersions(), LanguageVersion::getVersion)
                           )
                           .desc("Language version to use")

                           .defaultValue(language.getDefaultVersion())
                           .build();

        definePropertyDescriptor(languageVersion);
    }

    public void setLanguageVersion(String string) {
        setProperty(languageVersion, languageVersion.valueFrom(string));
    }

    @Override
    protected String getPropertySourceType() {
        return "Language";
    }

    @Override
    public String getName() {
        return language.getName();
    }

    public Language getLanguage() {
        return language;
    }

    public LanguageVersion getLanguageVersion() {
        return getProperty(languageVersion);
    }

    public String getSuppressMarker() {
        return getProperty(SUPPRESS_MARKER);
    }
}
