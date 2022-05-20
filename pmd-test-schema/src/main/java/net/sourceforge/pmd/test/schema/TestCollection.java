/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.test.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Clément Fournier
 */
public class TestCollection {

    private final List<TestDescriptor> tests = new ArrayList<>();

    public void addTest(TestDescriptor descriptor) {
        tests.add(Objects.requireNonNull(descriptor));
    }


    public List<TestDescriptor> getTests() {
        return Collections.unmodifiableList(tests);
    }

}
