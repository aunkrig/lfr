
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

import static de.unkrig.lfr.core.Pattern.CASE_INSENSITIVE;
import static de.unkrig.lfr.core.Pattern.LITERAL;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.unkrig.lfr.core.Pattern;

public
class PatternTest {

    @BeforeClass public static void
    setUpBeforeClass() { OracleEssentials.beginStatistics(); }

    @AfterClass public static void
    tearDownAfterClass() { OracleEssentials.endStatistics(); }

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

        String message = "regex=\"" + regex + "\", subject=\"" + subject + "\"";

        java.util.regex.Matcher            matcher1 = java.util.regex.Pattern.compile(regex).matcher(subject);
        de.unkrig.lfr.core.Pattern.Matcher matcher2 = de.unkrig.lfr.core.Pattern.compile(regex).matcher(subject);

        boolean m1Matches = matcher1.matches();
        boolean m2Matches = matcher2.matches();
        Assert.assertEquals(m1Matches, m2Matches);

        if (m1Matches) {
            OracleEssentials.assertEqualStateAfterMatch(message, matcher1, matcher2);
        } else {
            OracleEssentials.assertEqualState(message, matcher1, matcher2);
        }
    }

    @Test public void
    testFind() {
        OracleEssentials.harness("abc", "abc");
        OracleEssentials.harness("abc", "xxabcxx");
        OracleEssentials.harness("abc", "xxaBcxx");
        OracleEssentials.harness("a.c", "xxabcxx");
        OracleEssentials.harness("a.*b", "xxaxxbxxbxxbxx");
        OracleEssentials.harness("a.*?b", "xxaxxbxxbxxbxx");
        OracleEssentials.harness("a.*+b", "xxaxxbxxbxxbxx");
    }

    @Test public void
    testLookingAt() {
        this.verifyLookingAt("abc", "abcdef");
        this.verifyLookingAt("aBc", "abcdef");
        this.verifyLookingAt("a.c", "abcdef");
    }

    @Test public void
    testCaseInsensitive() {
        OracleEssentials.harness("(?i)A", "xxxAxxx");
        OracleEssentials.harness("(?i)A", "xxxaxxx");
        OracleEssentials.harness("(?i)Ä", "xxxäxxx");
        Assert.assertTrue(de.unkrig.lfr.core.Pattern.matches("(?i)Ä", "Ä"));
        Assert.assertFalse(de.unkrig.lfr.core.Pattern.matches("(?i)Ä", "ä"));
    }

    @Test public void
    testUnicodeCaseInsensitive() {
        OracleEssentials.harness("(?ui)A", "xxxAxxx");
        OracleEssentials.harness("(?ui)A", "xxxaxxx");
        OracleEssentials.harness("(?ui)Ä", "xxxäxxx");
        Assert.assertTrue(de.unkrig.lfr.core.Pattern.matches("(?ui)Ä", "Ä"));
        Assert.assertTrue(de.unkrig.lfr.core.Pattern.matches("(?ui)Ä", "ä"));
    }

    @Test public void
    testDotall() {
        OracleEssentials.harness(" \r  ", ".");
        OracleEssentials.harness(" \r  ", ".", de.unkrig.lfr.core.Pattern.DOTALL);
        OracleEssentials.harness(" \r  ", "(?s).");
    }

    @Test public void
    testLiteralRegex() {
        OracleEssentials.harness("$\\*xxx$\\*xxx", "$\\*", Pattern.LITERAL);
        OracleEssentials.harness("a\\xxxA\\xxx",   "a\\",    Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
    }

    // ===================================

    private void
    verifyLookingAt(String regex, String subject) {
        java.util.regex.Matcher            m1 = java.util.regex.Pattern.compile(regex).matcher(subject);
        de.unkrig.lfr.core.Pattern.Matcher m2 = de.unkrig.lfr.core.Pattern.compile(regex).matcher(subject);
        Assert.assertEquals(m1.lookingAt(), m2.lookingAt());
        OracleEssentials.assertEqualState("regex=\"" + regex + "\", subject=\"" + subject + "\"", m1, m2);
    }
}
