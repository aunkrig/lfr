
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static test.OracleEssentials.harness;
import static test.OracleEssentials.patternSyntaxException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public
class PatternTest {

    @BeforeClass public static void
    setUpBeforeClass() { OracleEssentials.beginStatistics(); }

    @AfterClass public static void
    tearDownAfterClass() { OracleEssentials.endStatistics(); }

    @SuppressWarnings("static-method") @Test public void
    testMatches() {
        verifyMatches("abc", "abc");
        verifyMatches("abc", "abcxx");
        verifyMatches("abc", "xxabc");
        verifyMatches("a.c", "aBc");
        verifyMatches("a.c", "aBcxx");
        verifyMatches("a.*c", "axxxc");
        verifyMatches("a.*c", "axxxcxxxc");
        verifyMatches("a.*c", "axxx");
    }

    @SuppressWarnings("static-method") @Test public void
    testLiteralOctals() {
        verifyMatches("\\00xx",   "\0xx");
        verifyMatches("\\01xx",   "\01xx");
        verifyMatches("\\011xx",  "\011xx");
        verifyMatches("\\0101xx",  "Axx");
        verifyMatches("\\0111xx",  "\0111xx");
    }

    @SuppressWarnings("static-method") @Test public void
    testFind() {
        harness("abc",   "abc");
        harness("abc",   "xxabcxx");
        harness("abc",   "xxaBcxx");
        harness("a.c",   "xxabcxx");
        harness("a.*b",  "xxaxxbxxbxxbxx");
        harness("a.*?b", "xxaxxbxxbxxbxx");
        harness("a.*+b", "xxaxxbxxbxxbxx");
    }

    @Test @SuppressWarnings("static-method") public void
    testLookingAt() {
        verifyLookingAt("abc", "abcdef");
        verifyLookingAt("aBc", "abcdef");
        verifyLookingAt("a.c", "abcdef");
    }

    @Test @SuppressWarnings("static-method") public void
    testCaseInsensitive() {
        harness("(?i)A", "xxxAxxx");
        harness("(?i)A", "xxxaxxx");
        harness("(?i)Ä", "xxxäxxx");
        assertTrue(de.unkrig.lfr.core.Pattern.matches("(?i)Ä", "Ä"));
        assertFalse(de.unkrig.lfr.core.Pattern.matches("(?i)Ä", "ä"));
    }

    @Test @SuppressWarnings("static-method") public void
    testUnicodeCaseInsensitive() {
        harness("(?ui)A", "xxxAxxx");
        harness("(?ui)A", "xxxaxxx");
        harness("(?ui)Ä", "xxxäxxx");
        assertTrue(de.unkrig.lfr.core.Pattern.matches("(?ui)Ä", "Ä"));
        assertTrue(de.unkrig.lfr.core.Pattern.matches("(?ui)Ä", "ä"));
    }

    @Test @SuppressWarnings("static-method") public void
    testDotall() {
        harness(".",     " \r  ");
        harness(".",     " \r  ", de.unkrig.lfr.core.Pattern.DOTALL);
        harness("(?s).", " \r  ");
    }

    @Test @SuppressWarnings("static-method") public void
    testLiteralRegex() {
        harness("$\\*", "$\\*xxx$\\*xxx", de.unkrig.lfr.core.Pattern.LITERAL);
        harness("a\\", "a\\xxxA\\xxx",    de.unkrig.lfr.core.Pattern.LITERAL | de.unkrig.lfr.core.Pattern.CASE_INSENSITIVE); // SUPPRESS CHECKSTYLE LineLength
        harness(".\\Q.\\E.", " ___ ");
        harness(".\\Q.\\E.", " _._ ");
    }

    @Test @SuppressWarnings("static-method") public void
    testBoundaries() {
        harness("^.", "___\r___\r\n___\u2028___");
        harness(".$", "___\r___\r\n___\u2028___");
        harness("^.", "___\r___\r\n___\u2028___", de.unkrig.lfr.core.Pattern.MULTILINE);
        harness(".$", "___\r___\r\n___\u2028___", de.unkrig.lfr.core.Pattern.MULTILINE);
        harness("\\b",  " a b c");
        harness("\\B",  " a b c");
        harness("\\A",  "bla\rbla");
        harness("\\Ga", "aaabbb");
        harness(".\\Z", "abc");
        harness(".\\Z", "abc\n");
        harness(".\\Z", "abc\r\nd");
        harness(".\\z", "abc\n");
//        harness(".\\z", "abc\r\nd"); JRE says !requireEnd !?

        harness(".", "abc",      de.unkrig.lfr.core.Pattern.MULTILINE);
        harness(".", "abc\n",    de.unkrig.lfr.core.Pattern.MULTILINE);
        harness(".", "abc\r\nd", de.unkrig.lfr.core.Pattern.MULTILINE);
    }

    @Test @SuppressWarnings("static-method") public void
    testMatchFlagsCapturingGroup() {
        harness("(?i:a)b", " ab Ab aB AB ");
    }

    @Test @SuppressWarnings("static-method") public void
    testAlternatives() {
        harness("a|b", " a b c ");
    }

    // ======================================== CHARACTER CLASSES ========================================

    @Test @SuppressWarnings("static-method") public void
    testPredefinedCharacterClasses() {
        harness("\\w",     " abc äöü ");
        harness("(?U)\\w", " abc äöü ");
        harness("\\W",     " abc äöü ");
        harness("(?U)\\W", " abc äöü ");
    }

    @Test @SuppressWarnings("static-method") public void
    testPosixCharacterClasses() {
        harness("\\p{Lower}",     " abc äöü ");
        harness("(?U)\\p{Lower}", " abc äöü ");
        harness("\\P{Lower}",     " abc äöü ");
        harness("(?U)\\P{Lower}", " abc äöü ");
    }

    @Test @SuppressWarnings("static-method") public void
    testJavaCharacterClasses() {

        harness("\\p{javaLowerCase}", " a B c ä Ä ");
        harness("\\P{javaLowerCase}", " a B c ä Ä ");
    }

    @Test @SuppressWarnings("static-method") public void
    testUnicodeCharacterClasses() {

        // By "UNICODE script" - NYI:
//        harness("\\p{IsLatin}",       " a B c ä Ä ");

        // By "UNICODE block":
        harness("\\p{InGreek}",       " \u03b1 ");
        harness("\\p{InBasicLatin}",  " a B c ä Ä ");
        harness("\\P{InBasicLatin}",  " a B c ä Ä ");

        // By "UNICODE category":
        harness("\\p{Lu}",            " a B c ä Ä ");
        harness("\\P{Lu}",            " a B c ä Ä ");
        harness("\\p{Sc}",            " a $ ");
        harness("\\P{Sc}",            " a $ ");

        // By "UNICODE property":
        harness("\\p{IsLowerCASE}",  " abc äöü ");
        harness("\\p{IsAlphabetic}", " abc äöü ");
    }

    // ======================================== END OF CHARACTER CLASSES ========================================

    @Test @SuppressWarnings("static-method") public void
    testNamedCapturingGroups1() {
        harness("(?<xxx>a+)", " a aa aaa");

        de.unkrig.lfr.core.Matcher matcher = de.unkrig.lfr.core.Pattern.compile("(?<xxx>a+)").matcher(" a aa aaa");

        assertTrue(matcher.find());
        assertEquals("a", matcher.group("xxx"));

        assertTrue(matcher.find());
        assertEquals("aa", matcher.group("xxx"));

        assertTrue(matcher.find());
        assertEquals("aaa", matcher.group("xxx"));

        assertFalse(matcher.find());
    }

    @Test @SuppressWarnings("static-method") public void
    testNamedCapturingGroupsBackreference() {
        harness("(?<first>\\w)\\k<first>", " a aa aaa");
    }

    @Test @SuppressWarnings("static-method") public void
    testPositiveLookahead() {
        harness("a(?=b)",   " a aba abba a");
        harness("a(?=(b))", " a aba abba a");
    }

    @Test @SuppressWarnings("static-method") public void
    testNegativeLookahead() {
        harness("a(?!b)",   " a aba abba a");
        harness("a(?!(b))", " a aba abba a");
    }

    @Test @SuppressWarnings("static-method") public void
    testPositiveLookbehind() {
        harness("(?<=b)a",    " a aba abba a");
        harness("(?<=(b))a",  " a aba abba a");
        harness("(?<=\\R )a", " \r\n a ");
        harness("(?<=\\R )a", " \r a ");
        harness("(?<=\\R )a", " \n a ");
    }

    @Test @SuppressWarnings("static-method") public void
    testNegativeLookbehind() {
        harness("(?<!b)a",   " a aba abba a");
        harness("(?<!(b))a", " a aba abba a");
    }

    @Test @SuppressWarnings("static-method") public void
    testRegion() {
        harness("a", "__a__ a aba abba __a__", 0, 5, 17);
        harness("^", "__a__ a aba abba __a__", 0, 5, 17);
        harness("^", "__a__ a aba abba __a__", 0, 5, 17, false, false);
        harness("^", "__a__ a aba abba __a__", 0, 5, 17, false, true);
        harness("^", "__a__ a aba abba __a__", 0, 5, 17, true,  false);
        harness("^", "__a__ a aba abba __a__", 0, 5, 17, true,  true);
    }

    @Test @SuppressWarnings("static-method") public void
    testTransparentBounds() {

        // The for the nineth, failed, match, jur returns "false" for "hitEnd()"!?
//        harness("\\b",     "__a__ a aba abba __a__", 0, 5, 17, true);

        // Lookahead.
        harness(" (?=_)",  "__a__ a aba abba __a__", 0, 5, 17, true);
        harness(" (?!_)",  "__a__ a aba abba __a__", 0, 5, 17, true);

        // Lookbehind.
        harness("(?<=_) ", "__a__ a aba abba __a__", 0, 5, 17, true);
        harness("(?<!_) ", "__a__ a aba abba __a__", 0, 5, 17, true);
    }

    @Test @SuppressWarnings("static-method") public void
    testAnchoringBounds() {
        harness("^",  "__a__ a aba abba __a__", 0, 5, 17, null, false);
        harness("$",  "__a__ a aba abba __a__", 0, 5, 17, null, false);
    }

    @Test @SuppressWarnings("static-method") public void
    testUnixLines() {
        harness("\\R",  "  \n  \r\n \u2028 ");
        harness("\\R",  "  \n  \r\n \u2028 ", de.unkrig.lfr.core.Pattern.UNIX_LINES);
        harness("^",    "  \n  \r\n \u2028 ");
        harness("^",    "  \n  \r\n \u2028 ", de.unkrig.lfr.core.Pattern.UNIX_LINES);
    }

    @Test @SuppressWarnings("static-method") public void
    testQuantifiers() {

        harness("a?",  " aaa ");
//        harness("a??", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        harness("a?+", " aaa ");

        harness("a*",  " aaa ");
//        harness("a*?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        harness("a*+", " aaa ");

        harness("a+",  " aaa ");
        harness("a+?", " aaa ");
        harness("a++", " aaa ");

//        harness("a{0}",  " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
//        harness("a{0}?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
//        harness("a{0}+", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?

        harness("a{1}",  " aaa ");
        harness("a{1}?", " aaa ");
        harness("a{1}+", " aaa ");

        harness("a{2}",  " aaa ");
        harness("a{2}?", " aaa ");
        harness("a{2}+", " aaa ");

        harness("a{0,}",  " aaa ");
//        harness("a{0,}?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        harness("a{0,}+", " aaa ");
    }

    @Test @SuppressWarnings("static-method") public void
    testSurrogates() {
        int    clef              = 0x1d120;
        char   clefHighSurrogate = highSurrogateOf(clef);
        char   clefLowSurrogate  = lowSurrogateOf(clef);
        String clefUnicode       = "" + clefHighSurrogate + clefLowSurrogate;

        harness(clefUnicode,       clefUnicode);
        harness(clefUnicode + "?", "");
        harness(clefUnicode + "?", "" + clefHighSurrogate);
        harness(clefUnicode + "?", "" + clefLowSurrogate);
        harness(clefUnicode + "?", "" + clefHighSurrogate + clefLowSurrogate);
        harness(clefUnicode + "?", "" + clefLowSurrogate + clefHighSurrogate);
    }

    @Test @SuppressWarnings("static-method") public void
    testPreviousMatchBoundary() {

        // From: http://stackoverflow.com/questions/2708833
        harness(
            "(?<=\\G\\d{3})(?=\\d)" + "|" + "(?<=^-?\\d{1,3})(?=(?:\\d{3})+(?!\\d))",
            "-1234567890.1234567890"
        );
    }

    @Test @SuppressWarnings("static-method") public void
    testAtomicGroups() {
        harness("^a(bc|b)c$",   "abc");
        harness("^a(bc|b)c$",   "abcc");
        harness("^a(?>bc|b)c$", "abc");
        harness("^a(?>bc|b)c$", "abcc");
    }

    /**
     * @see <a href="http://stackoverflow.com/questions/17618812">Clarification about requireEnd Matcher's method</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testRequireEnd() {
        harness("cat$", "I have a cat");
        harness("cat$", "I have a catflap");
        harness("cat",  "I have a cat");
        harness("cat",  "I have a catflap");

        harness("\\d+\\b|[><]=?", "1234");
        harness("\\d+\\b|[><]=?", ">=");
        harness("\\d+\\b|[><]=?", "<");
    }

    @Test @SuppressWarnings("static-method") public void
    testComments() {
        harness(" a# comment \nb ",    " ab a# comment \nb", de.unkrig.lfr.core.Pattern.COMMENTS);
        harness("(?x)  a  ",           " a ");
        harness("(?x)  a  (?-x) b",    " ab ");
        harness("(?x)  a  (?-x) b",    " a b ");
        harness("(?x)  a#\n  (?-x) b", " ab ");
        harness("(?x)  a#\n  (?-x) b", " a b ");

        harness("(?x)  (a)", " a b ");
        harness("(?x)  (?:a)", " a b ");
        harness("(?x)  ( ?:a)", " a b ");
        harness("(?x)  (?: a)", " a b ");
        harness("(?x)  (? : a)", " a b ");
        harness("(?x)  ( ? :a)", " a b ");
        harness("(?x)  ( ?: a)", " a b ");
        harness("(?x)  ( ? : a)", " a b ");
        harness("(?x)  (?<name>a)", " a b ");
        harness("(?x)  ( ?<name>a)", " a b ");
        patternSyntaxException("(?x)  (? <name>a)");
        harness("(?x)  (?< name>a)", " a b ");
        patternSyntaxException("(?x)  (? < name>a)");
        harness("(?x)  ( ?< name>a)", " a b ");
        patternSyntaxException("(?x)  ( ? < name>a)");
    }

    // ===================================

    /**
     * Verifies that {@link java.util.regex.Matcher#matches()} and {@link de.unkrig.lfr.core.Matcher#matches()} yield
     * the same result.
     */
    private static void
    verifyMatches(String regex, String subject) {

        String message = "regex=\"" + regex + "\", subject=\"" + subject + "\"";

        java.util.regex.Matcher    matcher1 = java.util.regex.Pattern.compile(regex).matcher(subject);
        de.unkrig.lfr.core.Matcher matcher2 = de.unkrig.lfr.core.Pattern.compile(regex).matcher(subject);

        boolean m1Matches = matcher1.matches();
        boolean m2Matches = matcher2.matches();
        assertEquals(m1Matches, m2Matches);

        if (m1Matches) {
            OracleEssentials.assertEqualStateAfterMatch(message, matcher1, matcher2);
        } else {
            OracleEssentials.assertEqualState(message, matcher1, matcher2);
        }
    }

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

    /**
     * Verifies that {@link java.util.regex.Matcher#lookingAt()} and {@link de.unkrig.lfr.core.Matcher#lookingAt()}
     * yield the same result.
     */
    private static void
    verifyLookingAt(String regex, String subject) {
        java.util.regex.Matcher    m1 = java.util.regex.Pattern.compile(regex).matcher(subject);
        de.unkrig.lfr.core.Matcher m2 = de.unkrig.lfr.core.Pattern.compile(regex).matcher(subject);
        assertEquals(m1.lookingAt(), m2.lookingAt());
        OracleEssentials.assertEqualState("regex=\"" + regex + "\", subject=\"" + subject + "\"", m1, m2);
    }
}
