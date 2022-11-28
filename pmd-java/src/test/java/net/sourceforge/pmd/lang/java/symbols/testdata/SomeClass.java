/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.symbols.testdata;

@AnnotWithDefaults(valueNoDefault = "ohio",
                   stringArrayDefault = {})
public class SomeClass {


    void m1(int a, @AnnotWithDefaults(valueNoDefault = "param") final String foo) {

    }

    void m4(final int x) {

    }

}
