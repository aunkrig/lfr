
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.PatternFactory;

@RunWith(Parameterized.class) public
class Misc extends ParameterizedWithPatternFactory {

    public
    Misc(PatternFactory patternFactory, String patternFactoryId) { super(patternFactory); }
    
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
        if (this.isLfr()) {
            Assert.assertEquals(
                "7a1bc",
                this.patternFactory.compile("(?<grp>a)").matcher("abc").replaceAll("${3 + 4 + grp + m.groupCount()}")
            );
        }
    }

    @Test public void
    testUnicodeCharacterNames() {
        
        if (JRE_VERSION < 9) return;
        
        assertMatches("\\N{LATIN SMALL LETTER O}",      "o");
        assertMatches("\\N{LATIN SmalL LETTER O}",      "o");
        assertMatches("\\N{  LATIN SmalL LETTER O   }", "o");
        assertPatternSyntaxException("\\N{LATIN SMALL LETTERx O}");
    }

    @Test public void
    testUnicodeExtendedGraphemeClusterBoundary() {
        assertMatches(true,  ".\\b{g}.", "ab");
        assertMatches(false, ".\\b{g}.", "a\u0308");
        if (this.isLfr()) {
            assertMatches(false, ".\\B{g}.", "ab");
            assertMatches(true,  ".\\B{g}.", "a\u0308");
        }
    }

    @Test public void
    testUnicodeExtendedGraphemeCluster() {
        
        // ANY one-char sequence is a grapheme.
        for (int cp = 1; cp < 1000000; cp++) {
            assertMatches("\\X", new String(Character.toChars(cp)));
        }
        assertMatches(true,  "\\X",    "a\u0308"); // 'a' + TREMA is ONE grapheme
        assertMatches(false, "\\X",    "\u0308a"); // TREMA + 'a' is TWO graphemes
        assertMatches(true,  "\\X\\X", "\u0308a");
        
        // The different kinds of line separator are ONE grapheme, ...
        assertMatches(true,  "\\X", "\r");
        assertMatches(true,  "\\X",   "\n");
        assertMatches(true,  "\\X",    "\r\n");
        // ... but "\r\r" are TWO.
        assertMatches(false, "\\X",    "\r\r");
        assertMatches(true,  "\\X\\X", "\r\r");
        
        // Hangeul-Jamo grapheme. See: https://en.wikipedia.org/wiki/Hangul_Jamo_(Unicode_block)
        assertMatches(true,  "\\X",    "\u1100\u1161\u11a8");
    }

    // -----------------------------
    
    private void
    assertMatches(String regex, String subject) {
        assertMatches(regex, subject, 0);
    }

    private void
    assertMatches(String regex, String subject, int flags) {
        assertMatches(true, regex, subject, flags);
    }
    
    private void
    assertMatches(boolean expected, String regex, String subject) {
        assertMatches(expected, regex, subject, 0);
    }
    
    private void
    assertMatches(boolean expected, String regex, String subject, int flags) {
        
        boolean actual = this.patternFactory.compile(regex, flags).matcher(subject).matches();
        
        if (expected && !actual) {
            Assert.fail("\"" + subject + "\" does not match regex \"" + regex + "\"");
        } else
        if (!expected && actual) {
            Assert.fail("\"" + subject + "\" should not match regex \"" + regex + "\"");
        }
    }
    
    // -----------------------------
    
    @SuppressWarnings("unused") private void
    assertFind(String regex, String subject) {
        assertFind(regex, subject, 0);
    }
    
    private void
    assertFind(String regex, String subject, int flags) {
        assertFind(-1, regex, subject, flags);
    }
    
    @SuppressWarnings("unused") private void
    assertFind(int expected, String regex, String subject) {
        assertFind(expected, regex, subject, 0);
    }
    
    /**
     * @param expected Special value -1 means: Once or multiply
     */
    private void
    assertFind(int expected, String regex, String subject, int flags) {
        
        Matcher m = this.patternFactory.compile(regex, flags).matcher(subject);
        
        int actual = 0;
        while (m.find()) actual++;
        
        if (expected != 0 && actual == 0) {
            Assert.fail("Regex \"" + regex + "\" not found in \"" + subject + "\"");
        } else
        if (expected == 0 && actual > 0) {
            Assert.fail("Regex \"" + regex + "\" should not be found in \"" + subject+ "\"");
        } else
        if (expected == -1 && actual > 0) {
            ;
        } else
        if (actual != expected) {
            Assert.fail(
                "Regex \""
                + regex
                + "\" should be found "
                + expected
                + " times in \""
                + subject
                + "\", but was found "
                + actual
                + " times"
            );
        }
    }
    
    public void
    assertPatternSyntaxException(String regex) {
        this.assertPatternSyntaxException(regex, 0);
    }

    public void
    assertPatternSyntaxException(String regex, int flags) {

        try {
            this.patternFactory.compile(regex, flags);
            Assert.fail();
        } catch (PatternSyntaxException pse) {
            return;
        }
    }
}
