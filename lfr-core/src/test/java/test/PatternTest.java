
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

        java.util.regex.Matcher    matcher1 = java.util.regex.Pattern.compile(regex).matcher(subject);
        de.unkrig.lfr.core.Matcher matcher2 = de.unkrig.lfr.core.Pattern.compile(regex).matcher(subject);

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
        OracleEssentials.harness(".\\Q.\\E.", " ___ ");
        OracleEssentials.harness(".\\Q.\\E.", " _._ ");
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
//        OracleEssentials.harness(".\\z", "abc\r\nd"); JRE says !requireEnd !?

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

    // ======================================== CHARACTER CLASSES ========================================

    @Test public void
    testPredefinedCharacterClasses() {
        OracleEssentials.harness("\\w",     " abc äöü ");
        OracleEssentials.harness("(?U)\\w", " abc äöü ");
        OracleEssentials.harness("\\W",     " abc äöü ");
        OracleEssentials.harness("(?U)\\W", " abc äöü ");
    }

    @Test public void
    testPosixCharacterClasses() {
        OracleEssentials.harness("\\p{Lower}",     " abc äöü ");
        OracleEssentials.harness("(?U)\\p{Lower}", " abc äöü ");
        OracleEssentials.harness("\\P{Lower}",     " abc äöü ");
        OracleEssentials.harness("(?U)\\P{Lower}", " abc äöü ");
    }

    @Test public void
    testJavaCharacterClasses() {

        OracleEssentials.harness("\\p{javaLowerCase}", " a B c ä Ä ");
        OracleEssentials.harness("\\P{javaLowerCase}", " a B c ä Ä ");
    }

    @Test public void
    testUnicodeCharacterClasses() {

        // By "UNICODE script" - NYI:
//        OracleEssentials.harness("\\p{IsLatin}",       " a B c ä Ä ");

        // By "UNICODE block":
        OracleEssentials.harness("\\p{InGreek}",       " \u03b1 ");
        OracleEssentials.harness("\\p{InBasicLatin}",  " a B c ä Ä ");
        OracleEssentials.harness("\\P{InBasicLatin}",  " a B c ä Ä ");

        // By "UNICODE category":
        OracleEssentials.harness("\\p{Lu}",            " a B c ä Ä ");
        OracleEssentials.harness("\\P{Lu}",            " a B c ä Ä ");
        OracleEssentials.harness("\\p{Sc}",            " a $ ");
        OracleEssentials.harness("\\P{Sc}",            " a $ ");

        // By "UNICODE property":
        OracleEssentials.harness("\\p{IsLowerCASE}",  " abc äöü ");
        OracleEssentials.harness("\\p{IsAlphabetic}", " abc äöü ");
    }

    // ======================================== END OF CHARACTER CLASSES ========================================

    @Test public void
    testNamedCapturingGroups() {
        OracleEssentials.harness("(?<xxx>a+)", " a aa aaa");

        de.unkrig.lfr.core.Matcher matcher = de.unkrig.lfr.core.Pattern.compile("(?<xxx>a+)").matcher(" a aa aaa");

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
        OracleEssentials.harness("a(?!(b))", " a aba abba a");
    }

    @Test public void
    testPositiveLookbehind() {
        OracleEssentials.harness("(?<=b)a",    " a aba abba a");
        OracleEssentials.harness("(?<=(b))a",  " a aba abba a");
        OracleEssentials.harness("(?<=\\R )a", " \r\n a ");
        OracleEssentials.harness("(?<=\\R )a", " \r a ");
        OracleEssentials.harness("(?<=\\R )a", " \n a ");
    }

    @Test public void
    testNegativeLookbehind() {
        OracleEssentials.harness("(?<!b)a",   " a aba abba a");
        OracleEssentials.harness("(?<!(b))a", " a aba abba a");
    }

    @Test public void
    testRegion() {
        OracleEssentials.harness("a", "__a__ a aba abba __a__", 0, 5, 17);
        OracleEssentials.harness("^", "__a__ a aba abba __a__", 0, 5, 17);
        OracleEssentials.harness("^", "__a__ a aba abba __a__", 0, 5, 17, false, false);
        OracleEssentials.harness("^", "__a__ a aba abba __a__", 0, 5, 17, false, true);
        OracleEssentials.harness("^", "__a__ a aba abba __a__", 0, 5, 17, true,  false);
        OracleEssentials.harness("^", "__a__ a aba abba __a__", 0, 5, 17, true,  true);
    }

    @Test public void
    testTransparentBounds() {

        // The for the nineth, failed, match, jur returns "false" for "hitEnd()"!?
//        OracleEssentials.harness("\\b",     "__a__ a aba abba __a__", 0, 5, 17, true);

        // Lookahead.
        OracleEssentials.harness(" (?=_)",  "__a__ a aba abba __a__", 0, 5, 17, true);
        OracleEssentials.harness(" (?!_)",  "__a__ a aba abba __a__", 0, 5, 17, true);

        // Lookbehind.
        OracleEssentials.harness("(?<=_) ", "__a__ a aba abba __a__", 0, 5, 17, true);
        OracleEssentials.harness("(?<!_) ", "__a__ a aba abba __a__", 0, 5, 17, true);
    }

    @Test public void
    testAnchoringBounds() {
        OracleEssentials.harness("^",  "__a__ a aba abba __a__", 0, 5, 17, null, false);
        OracleEssentials.harness("$",  "__a__ a aba abba __a__", 0, 5, 17, null, false);
    }

    @Test public void
    testUnixLines() {
        OracleEssentials.harness("\\R",  "  \n  \r\n \u2028 ");
        OracleEssentials.harness("\\R",  "  \n  \r\n \u2028 ", de.unkrig.lfr.core.Pattern.UNIX_LINES);
        OracleEssentials.harness("^",    "  \n  \r\n \u2028 ");
        OracleEssentials.harness("^",    "  \n  \r\n \u2028 ", de.unkrig.lfr.core.Pattern.UNIX_LINES);
    }

    @Test public void
    testQuantifiers() {

        OracleEssentials.harness("a?",  " aaa ");
//        OracleEssentials.harness("a??", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        OracleEssentials.harness("a?+", " aaa ");

        OracleEssentials.harness("a*",  " aaa ");
//        OracleEssentials.harness("a*?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        OracleEssentials.harness("a*+", " aaa ");

        OracleEssentials.harness("a+",  " aaa ");
        OracleEssentials.harness("a+?", " aaa ");
        OracleEssentials.harness("a++", " aaa ");

//        OracleEssentials.harness("a{0}",  " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
//        OracleEssentials.harness("a{0}?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
//        OracleEssentials.harness("a{0}+", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?

        OracleEssentials.harness("a{1}",  " aaa ");
        OracleEssentials.harness("a{1}?", " aaa ");
        OracleEssentials.harness("a{1}+", " aaa ");

        OracleEssentials.harness("a{2}",  " aaa ");
        OracleEssentials.harness("a{2}?", " aaa ");
        OracleEssentials.harness("a{2}+", " aaa ");

        OracleEssentials.harness("a{0,}",  " aaa ");
//        OracleEssentials.harness("a{0,}?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        OracleEssentials.harness("a{0,}+", " aaa ");
    }

    @Test public void
    testSurrogates() {
        int    clef              = 0x1d120;
        char   clefHighSurrogate = PatternTest.highSurrogateOf(clef);
        char   clefLowSurrogate  = PatternTest.lowSurrogateOf(clef);
        String clefUnicode       = "" + clefHighSurrogate + clefLowSurrogate;

        OracleEssentials.harness(clefUnicode,       clefUnicode);
        OracleEssentials.harness(clefUnicode + "?", "");
        OracleEssentials.harness(clefUnicode + "?", "" + clefHighSurrogate);
        OracleEssentials.harness(clefUnicode + "?", "" + clefLowSurrogate);
        OracleEssentials.harness(clefUnicode + "?", "" + clefHighSurrogate + clefLowSurrogate);
        OracleEssentials.harness(clefUnicode + "?", "" + clefLowSurrogate + clefHighSurrogate);
    }

    @Test public void
    testPreviousMatchBoundary() {

        // From: http://stackoverflow.com/questions/2708833
        OracleEssentials.harness(
            "(?<=\\G\\d{3})(?=\\d)" + "|" + "(?<=^-?\\d{1,3})(?=(?:\\d{3})+(?!\\d))",
            "-1234567890.1234567890"
        );
    }

    @Test public void
    testAtomicGroups() {
        OracleEssentials.harness("^a(bc|b)c$",   "abc");
        OracleEssentials.harness("^a(bc|b)c$",   "abcc");
        OracleEssentials.harness("^a(?>bc|b)c$", "abc");
        OracleEssentials.harness("^a(?>bc|b)c$", "abcc");
    }

    /**
     * @see <a href="http://stackoverflow.com/questions/17618812">Clarification about requireEnd Matcher's method</a>
     */
    @Test public void
    testRequireEnd() {
        OracleEssentials.harness("cat$", "I have a cat");
        OracleEssentials.harness("cat$", "I have a catflap");
        OracleEssentials.harness("cat",  "I have a cat");
        OracleEssentials.harness("cat",  "I have a catflap");

        OracleEssentials.harness("\\d+\\b|[><]=?", "1234");
        OracleEssentials.harness("\\d+\\b|[><]=?", ">=");
        OracleEssentials.harness("\\d+\\b|[><]=?", "<");
    }

    @Test public void
    testComments() {
        OracleEssentials.harness(" a# comment \nb ",    " ab a# comment \nb", de.unkrig.lfr.core.Pattern.COMMENTS);
        OracleEssentials.harness("(?x)  a  ",           " a ");
        OracleEssentials.harness("(?x)  a  (?-x) b",    " ab ");
        OracleEssentials.harness("(?x)  a  (?-x) b",    " a b ");
        OracleEssentials.harness("(?x)  a#\n  (?-x) b", " ab ");
        OracleEssentials.harness("(?x)  a#\n  (?-x) b", " a b ");
    }

    // ===================================

    private static char
    highSurrogateOf(int codepoint) {
        if (codepoint < 0x10000 || codepoint > 0x10FFFF) throw new IllegalArgumentException();
        return (char) (((codepoint - 0x10000) >> 10) + 0xD800);
    }

    private static char
    lowSurrogateOf(int codepoint) {
        if (codepoint < 0x10000 || codepoint > 0x10FFFF) throw new IllegalArgumentException();
        return (char) (((codepoint - 0x10000) & 0x3ff) + 0xDC00);
    }

    private void
    verifyLookingAt(String regex, String subject) {
        java.util.regex.Matcher    m1 = java.util.regex.Pattern.compile(regex).matcher(subject);
        de.unkrig.lfr.core.Matcher m2 = de.unkrig.lfr.core.Pattern.compile(regex).matcher(subject);
        Assert.assertEquals(m1.lookingAt(), m2.lookingAt());
        OracleEssentials.assertEqualState("regex=\"" + regex + "\", subject=\"" + subject + "\"", m1, m2);
    }
}
