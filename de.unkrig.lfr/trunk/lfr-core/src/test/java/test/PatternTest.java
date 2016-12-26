
/*
 * de.unkrig.lfr - A super-fast regular expression evaluator
 *
 * Copyright (c) 2016, Arno Unkrig
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

// SUPPRESS CHECKSTYLE RequireThis|Javadoc:9999

package test;

import org.junit.Assert;
import org.junit.Test;

public
class PatternTest {

    @Test public void
    testMatches() {
        this.verifyMatches("abc", "abc");
        this.verifyMatches("abc", "abcxx");
        this.verifyMatches("abc", "xxabc");
        this.verifyMatches("a.c", "aBc");
        this.verifyMatches("a.c", "aBcxx");
        this.verifyMatches("a.*c", "axxxc");
        this.verifyMatches("a.*c", "axxxcxxxc");
        this.verifyMatches("a.*c", "axxx");
    }

    private void
    verifyMatches(String regex, String subject) {
        java.util.regex.Matcher            m1 = java.util.regex.Pattern.compile(regex).matcher(subject);
        de.unkrig.lfr.core.Pattern.Matcher m2 = de.unkrig.lfr.core.Pattern.compile(regex).matcher(subject);
        Assert.assertEquals(m1.matches(), m2.matches());
        PatternTest.assertEqualState(m1, m2);
    }

    @Test public void
    testFind() {
        this.verifyFind("abc", "abc");
        this.verifyFind("abc", "xxabcxx");
        this.verifyFind("abc", "xxaBcxx");
        this.verifyFind("a.c", "xxabcxx");
        this.verifyFind("a.*b", "xxaxxbxxbxxbxx");
        this.verifyFind("a.*?b", "xxaxxbxxbxxbxx");
        this.verifyFind("a.*+b", "xxaxxbxxbxxbxx");
    }

    private void
    verifyFind(String regex, String subject) {
        this.verifyFind(regex, subject, 0);
    }

    private void
    verifyFind(String regex, String subject, int flags) {
        java.util.regex.Matcher            m1 = java.util.regex.Pattern.compile(regex, flags).matcher(subject);
        de.unkrig.lfr.core.Pattern.Matcher m2 = de.unkrig.lfr.core.Pattern.compile(regex, flags).matcher(subject);

        for (int i = 0;; i++) {
            boolean found1 = m1.find();
            boolean found2 = m2.find();
            Assert.assertEquals("Match #" + (i + 1) + ": " + subject + " => " + regex, found1, found2);
            PatternTest.assertEqualState(m1, m2);
            if (!found1) return;
        }
    }

    @Test public void
    testLookingAt() {
        this.verifyLookingAt("abc", "abcdef");
        this.verifyLookingAt("aBc", "abcdef");
        this.verifyLookingAt("a.c", "abcdef");
    }

    @Test public void
    testCaseInsensitive() {
        this.verifyFind("(?i)A", "xxxAxxx");
        this.verifyFind("(?i)A", "xxxaxxx");
        this.verifyFind("(?i)Ä", "xxxäxxx");
        Assert.assertTrue(de.unkrig.lfr.core.Pattern.matches("(?i)Ä", "Ä"));
        Assert.assertFalse(de.unkrig.lfr.core.Pattern.matches("(?i)Ä", "ä"));
    }

    @Test public void
    testUnicodeCaseInsensitive() {
        this.verifyFind("(?ui)A", "xxxAxxx");
        this.verifyFind("(?ui)A", "xxxaxxx");
        this.verifyFind("(?ui)Ä", "xxxäxxx");
        Assert.assertTrue(de.unkrig.lfr.core.Pattern.matches("(?ui)Ä", "Ä"));
        Assert.assertTrue(de.unkrig.lfr.core.Pattern.matches("(?ui)Ä", "ä"));
    }

    @Test public void
    testDotall() {
        this.verifyFind(" \r  ", ".");
        this.verifyFind(" \r  ", ".", de.unkrig.lfr.core.Pattern.DOTALL);
        this.verifyFind(" \r  ", "(?s).");
    }

    private void
    verifyLookingAt(String regex, String subject) {
        java.util.regex.Matcher            m1 = java.util.regex.Pattern.compile(regex).matcher(subject);
        de.unkrig.lfr.core.Pattern.Matcher m2 = de.unkrig.lfr.core.Pattern.compile(regex).matcher(subject);
        Assert.assertEquals(m1.lookingAt(), m2.lookingAt());
        PatternTest.assertEqualState(m1, m2);
    }

    private static void
    assertEqualState(java.util.regex.Matcher m1, de.unkrig.lfr.core.Pattern.Matcher m2) {

        int m1s;
        try {
            m1s = m1.start();
        } catch (IllegalStateException ise) {
            try { m1.end();   Assert.fail(); } catch (IllegalStateException ise2) {}
            try { m2.start(); Assert.fail(); } catch (IllegalStateException ise2) {}
            try { m2.end();   Assert.fail(); } catch (IllegalStateException ise2) {}
            return;
        }

        Assert.assertEquals(m1s,      m2.start());
        Assert.assertEquals(m1.end(), m2.end());
    }
}
