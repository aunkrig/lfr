
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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
        OracleEssentials.harness("abc",   "abc");
        OracleEssentials.harness("abc",   "xxabcxx");
        OracleEssentials.harness("abc",   "xxaBcxx");
        OracleEssentials.harness("a.c",   "xxabcxx");
        OracleEssentials.harness("a.*b",  "xxaxxbxxbxxbxx");
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
        OracleEssentials.harness(".",     " \r  ");
        OracleEssentials.harness(".",     " \r  ", de.unkrig.lfr.core.Pattern.DOTALL);
        OracleEssentials.harness("(?s).", " \r  ");
    }

    @Test public void
    testLiteralRegex() {
        OracleEssentials.harness("$\\*", "$\\*xxx$\\*xxx", de.unkrig.lfr.core.Pattern.LITERAL);
        OracleEssentials.harness("a\\", "a\\xxxA\\xxx",    de.unkrig.lfr.core.Pattern.LITERAL | de.unkrig.lfr.core.Pattern.CASE_INSENSITIVE); // SUPPRESS CHECKSTYLE LineLength
    }

    @Test public void
    testBoundaries() {
        OracleEssentials.harness("^.", "___\r___\r\n___\u2028___");
        OracleEssentials.harness(".$", "___\r___\r\n___\u2028___");
        OracleEssentials.harness("^.", "___\r___\r\n___\u2028___", de.unkrig.lfr.core.Pattern.MULTILINE);
        OracleEssentials.harness(".$", "___\r___\r\n___\u2028___", de.unkrig.lfr.core.Pattern.MULTILINE);
        OracleEssentials.harness("\\b",  " a b c");
        OracleEssentials.harness("\\B",  " a b c");
        OracleEssentials.harness("\\A",  "bla\rbla");
        OracleEssentials.harness("\\Ga", "aaabbb");
        OracleEssentials.harness(".\\Z", "abc");
        OracleEssentials.harness(".\\Z", "abc\n");
        OracleEssentials.harness(".\\Z", "abc\r\nd");
        OracleEssentials.harness(".\\z", "abc\n");
        OracleEssentials.harness(".\\z", "abc\r\nd");

        OracleEssentials.harness(".", "abc",      de.unkrig.lfr.core.Pattern.MULTILINE);
        OracleEssentials.harness(".", "abc\n",    de.unkrig.lfr.core.Pattern.MULTILINE);
        OracleEssentials.harness(".", "abc\r\nd", de.unkrig.lfr.core.Pattern.MULTILINE);
    }

    @Test public void
    testMatchFlagsCapturingGroup() {
        OracleEssentials.harness("(?i:a)b", " ab Ab aB AB ");
    }

    @Test public void
    testAlternatives() {
        OracleEssentials.harness("a|b", " a b c ");
    }

    @Test public void
    testPosixCharacterClasses() {
        OracleEssentials.harness("\\p{Lower}", " a B c ä Ä ");
        OracleEssentials.harness("\\P{Lower}", " a B c ä Ä ");
    }

    @Test public void
    testJavaCharacterClasses() {

        OracleEssentials.harness("\\p{javaLowerCase}", " a B c ä Ä ");
        OracleEssentials.harness("\\P{javaLowerCase}", " a B c ä Ä ");

//        OracleEssentials.harness("\\p{IsLatin}",       " a B c ä Ä ");  Unicode scripts are not implemented

        OracleEssentials.harness("\\p{InBasicLatin}",  " a B c ä Ä ");
        OracleEssentials.harness("\\P{InBasicLatin}",  " a B c ä Ä ");

        OracleEssentials.harness("\\p{Lu}",            " a B c ä Ä ");
        OracleEssentials.harness("\\P{Lu}",            " a B c ä Ä ");

//        OracleEssentials.harness("\\p{IsAlphabetic}",  " a B c ä Ä ");  Unicode binary properties are not implementd
    }

    @Test public void
    testNamedCapturingGroups() {
        OracleEssentials.harness("(<xxx>a+)", " a aa aaa");

        de.unkrig.lfr.core.Pattern.Matcher
        matcher = de.unkrig.lfr.core.Pattern.compile("(?<xxx>a+)").matcher(" a aa aaa");

        Assert.assertTrue(matcher.find());
        Assert.assertEquals("a", matcher.group("xxx"));

        Assert.assertTrue(matcher.find());
        Assert.assertEquals("aa", matcher.group("xxx"));

        Assert.assertTrue(matcher.find());
        Assert.assertEquals("aaa", matcher.group("xxx"));

        Assert.assertFalse(matcher.find());
    }

    @Test public void
    testPositiveLookahead() {
        OracleEssentials.harness("a(?=b)",   " a aba abba a");
        OracleEssentials.harness("a(?=(b))", " a aba abba a");
    }

    @Test public void
    testNegativeLookahead() {
        OracleEssentials.harness("a(?!b)",   " a aba abba a");

        // JREs 6, 7 and 8 falsely report for the second match (10-11, "a") a $1 of 7-8 (" "). Obviously the $1 is not
        // cleared after the mismatch 7-8.
//        OracleEssentials.harness("a(?!(b))", " a aba abba a");
    }

    @Test public void
    testPositiveLookbehind() {
        OracleEssentials.harness("(?<=b)a",   " a aba abba a");

        // JREs 6, 7 and 8 falsely report for the first match 4-6 ("ba"), insstead of 5-6 ("a")
//        OracleEssentials.harness("(?<=(b))a", " a aba abba a");
    }

    @Test public void
    testNegativeLookbehind() {
        OracleEssentials.harness("(?<!b)a",   " a aba abba a");

        // JREs 6, 7 and 8 falsely report for the third match (7-8, "a") a $1 of 4-5 ("b"). Obviously the $1 is not
        // cleared after the mismatch 5-6.
//        OracleEssentials.harness("(?<!(b))a", " a aba abba a");
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
