
/*
 * de.unkrig.lfr - A super-fast regular expression evaluator
 *
 * Copyright (c) 2019, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package test;

import java.util.regex.PatternSyntaxException;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.ref4j.PatternFactory;

@SuppressWarnings("static-method")
public
class Misc {

    /**
     * The pattern factory that verfies the functional equality of JUR and LFR.
     */
    public static final PatternFactory
    PF = PatternFactory.get();

    /**
     * 6, 7, 8, ...
     */
    private static final int JRE_VERSION;
    static {

        String jsv = System.getProperty("java.specification.version");

        // For Java 1.0 through 8, the string has the formt "1.x"; since Java 9 "x".
        if (jsv.startsWith("1.")) jsv = jsv.substring(2);
        
        JRE_VERSION = Integer.parseInt(jsv);
    }

    @Test public void
    testWebsite() {
        Assert.assertEquals(
            "7a1bc",
            PF.compile("(?<grp>a)").matcher("abc").replaceAll("${3 + 4 + grp + m.groupCount()}")
        );
    }

    @Test public void
    testUnicodeCharacterNames() {
        
        if (JRE_VERSION < 9) return;
        
        assertMatches("\\N{LATIN SMALL LETTER O}",      "o");
        assertMatches("\\N{LATIN SmalL LETTER O}",      "o");
        assertMatches("\\N{  LATIN SmalL LETTER O   }", "o");
        assertPatternSyntaxException("\\N{LATIN SMALL LETTERx O}");
    }
    
    // -----------------------------
    
    private static void
    assertMatches(String regex, String subject) { assertMatches(regex, subject, 0); }
    
    private static void
    assertMatches(String regex, String subject, int flags) {
        Assert.assertTrue(
            "\"" + subject + "\" does not match regex \"" + regex + "\"",
            PF.compile(regex, flags).matcher(subject).matches()
        );
    }

    public static void
    assertPatternSyntaxException(String regex) {
        Misc.assertPatternSyntaxException(regex, 0);
    }

    public static void
    assertPatternSyntaxException(String regex, int flags) {

        try {
            Misc.PF.compile(regex, flags);
            Assert.fail();
        } catch (PatternSyntaxException pse) {
            return;
        }
    }

}
