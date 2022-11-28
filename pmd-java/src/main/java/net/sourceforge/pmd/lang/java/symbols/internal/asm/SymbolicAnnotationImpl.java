/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.symbols.internal.asm;

import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.lang.java.symbols.JClassSymbol;
import net.sourceforge.pmd.lang.java.symbols.JMethodSymbol;
import net.sourceforge.pmd.lang.java.symbols.SymbolicValue;
import net.sourceforge.pmd.lang.java.symbols.SymbolicValue.SymAnnot;
import net.sourceforge.pmd.lang.java.symbols.internal.SymbolEquality;

/**
 * An annotation parsed from a class file.
 */
final class SymbolicAnnotationImpl implements SymAnnot {

    private final JClassSymbol typeStub;
    /** Many annotations have no attributes so this remains the singleton emptyMap in this case. */
    private @NonNull Map<String, SymbolicValue> explicitAttrs = Collections.emptyMap();
    private final boolean runtimeVisible;

    SymbolicAnnotationImpl(AsmSymbolResolver resolver, boolean runtimeVisible, String descriptor) {
        this.runtimeVisible = runtimeVisible;
        this.typeStub = resolver.resolveFromInternalNameCannotFail(ClassNamesUtil.classDescriptorToInternalName(descriptor));
    }

    void addAttribute(String name, SymbolicValue value) {
        if (explicitAttrs.isEmpty()) {
            explicitAttrs = new HashMap<>(); // make it modifiable
        }
        explicitAttrs.put(name, value);
    }

    @Override
    public Set<String> getAttributeNames() {
        return typeStub.getAnnotationAttributeNames();
    }

    @Override
    public @Nullable SymbolicValue getAttribute(String attrName) {
        SymbolicValue value = explicitAttrs.get(attrName);
        if (value != null) {
            return value;
        }
        return getDefaultValue(attrName);
    }

    private @Nullable SymbolicValue getDefaultValue(String attrName) {
        if (!typeStub.isAnnotation()) {
            return null; // path taken for invalid stuff & unresolved classes
        }
        for (JMethodSymbol m : typeStub.getDeclaredMethods()) {
            if (m.getSimpleName().equals(attrName)
                && m.getArity() == 0
                && !m.isStatic()) {
                return m.getDefaultAnnotationValue(); // nullable
            }
        }
        return null;
    }

    @Override
    public RetentionPolicy getRetention() {
        return runtimeVisible ? RetentionPolicy.RUNTIME
                              : RetentionPolicy.CLASS;
    }

    @Override
    public boolean isOfType(String binaryName) {
        return typeStub.getBinaryName().equals(binaryName);
    }

    @Override
    public String getBinaryName() {
        return typeStub.getBinaryName();
    }

    @Override
    public String getSimpleName() {
        return typeStub.getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        return SymbolEquality.ANNOTATION.equals(this, o);
    }

    @Override
    public int hashCode() {
        return SymbolEquality.ANNOTATION.hash(this);
    }

    @Override
    public String toString() {
        return "@" + typeStub + explicitAttrs;
    }
}
