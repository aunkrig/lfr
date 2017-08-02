
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

import java.util.Random;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.Transformer;
import de.unkrig.commons.nullanalysis.Nullable;
import test.RegexTest.MatcherAssertion;

public
class PatternTest {

    @BeforeClass public static void
    setUpBeforeClass() { OracleEssentials.beginStatistics(); }

    @AfterClass public static void
    tearDownAfterClass() { OracleEssentials.endStatistics(); }

    @SuppressWarnings("static-method") @Test public void
    testMatches() {
        harnessMatches("abc", "abc");
        harnessMatches("abc", "abcxx");
        harnessMatches("abc", "xxabc");
        harnessMatches("a.c", "aBc");
        harnessMatches("a.c", "aBcxx");
        harnessMatches("a.*c", "axxxc");
        harnessMatches("a.*c", "axxxcxxxc");
        harnessMatches("a.*c", "axxx");
    }

    @SuppressWarnings("static-method") @Test public void
    testLiteralOctals() {
        harnessMatches("\\00xx",   "\0xx");
        harnessMatches("\\01xx",   "\01xx");
        harnessMatches("\\011xx",  "\011xx");
        harnessMatches("\\0101xx",  "Axx");
        harnessMatches("\\0111xx",  "\0111xx");
    }

    @SuppressWarnings("static-method") @Test public void
    testLongStringLiterals() {

        String infix = "ABCDEFGHIJKLMNOP";

        Producer<String> sp = randomSubjectProducer(infix);

//        // Verify that the "long literal string" optimization has NOT taken place yet.
//        Assert.assertFalse(classIsLoaded("de.unkrig.commons.lang.StringUtil$6"));

        for (int i = 0; i < 1000; i++) {
            OracleEssentials.harnessFull(infix, AssertionUtil.notNull(sp.produce()));
        }

//        // Verify that the "long literal string" optimization HAS taken place now.
//        Assert.assertTrue(classIsLoaded("de.unkrig.commons.lang.StringUtil$6"));
    }

    private static Producer<String>
    randomSubjectProducer(final String infix) {

        return new Producer<String>() {

            Random r = new Random(123);

            @Override @Nullable public String
            produce() {

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 20; i++) {
                    for (int j = this.r.nextInt(64) - 32; j > 0; j--) sb.append('X');
                    sb.append(infix, 0, this.r.nextInt(infix.length() + 1));
                    for (int j = this.r.nextInt(64) - 32; j > 0; j--) sb.append('X');
                    sb.append(infix, this.r.nextInt(infix.length()), infix.length());
                }

                return sb.toString();
            }
        };
    }

    @SuppressWarnings("static-method") @Test public void
    testFind() {
        OracleEssentials.harnessFull("abc",   "abc");
        OracleEssentials.harnessFull("abc",   "xxabcxx");
        OracleEssentials.harnessFull("abc",   "xxaBcxx");
        OracleEssentials.harnessFull("a.c",   "xxabcxx");
        OracleEssentials.harnessFull("a.*b",  "xxaxxbxxbxxbxx");
        OracleEssentials.harnessFull("a.*?b", "xxaxxbxxbxxbxx");
        OracleEssentials.harnessFull("a.*+b", "xxaxxbxxbxxbxx");
    }

    @Test @SuppressWarnings("static-method") public void
    testLookingAt() {
        harnessLookingAt("abc", "abcdef");
        harnessLookingAt("aBc", "abcdef");
        harnessLookingAt("a.c", "abcdef");
    }

    @Test @SuppressWarnings("static-method") public void
    testCaseInsensitive() {
        OracleEssentials.harnessFull("(?i)A", "xxxAxxx");
        OracleEssentials.harnessFull("(?i)A", "xxxaxxx");
        OracleEssentials.harnessFull("(?i)Ä", "xxxäxxx");
        assertTrue(de.unkrig.lfr.core.Pattern.matches("(?i)Ä", "Ä"));
        assertFalse(de.unkrig.lfr.core.Pattern.matches("(?i)Ä", "ä"));
    }

    @Test @SuppressWarnings("static-method") public void
    testUnicodeCaseInsensitive() {
        OracleEssentials.harnessFull("(?ui)A", "xxxAxxx");
        OracleEssentials.harnessFull("(?ui)A", "xxxaxxx");
        OracleEssentials.harnessFull("(?ui)Ä", "xxxäxxx");
        assertTrue(de.unkrig.lfr.core.Pattern.matches("(?ui)Ä", "Ä"));
        assertTrue(de.unkrig.lfr.core.Pattern.matches("(?ui)Ä", "ä"));
    }

    @Test @SuppressWarnings("static-method") public void
    testDotall() {
        OracleEssentials.harnessFull(".",     " \r  ");
        OracleEssentials.harnessFull(".",     " \r  ", de.unkrig.lfr.core.Pattern.DOTALL);
        OracleEssentials.harnessFull("(?s).", " \r  ");
    }

    @Test @SuppressWarnings("static-method") public void
    testLiteralRegex() {
        OracleEssentials.harnessFull("$\\*", "$\\*xxx$\\*xxx", de.unkrig.lfr.core.Pattern.LITERAL);
        OracleEssentials.harnessFull("a\\", "a\\xxxA\\xxx",    de.unkrig.lfr.core.Pattern.LITERAL | de.unkrig.lfr.core.Pattern.CASE_INSENSITIVE); // SUPPRESS CHECKSTYLE LineLength
        OracleEssentials.harnessFull(".\\Q.\\E.", " ___ ");
        OracleEssentials.harnessFull(".\\Q.\\E.", " _._ ");
    }

    @Test @SuppressWarnings("static-method") public void
    testBoundaries() {
        OracleEssentials.harnessFull("^.", "___\r___\r\n___\u2028___");
        OracleEssentials.harnessFull(".$", "___\r___\r\n___\u2028___");
        OracleEssentials.harnessFull("^.", "___\r___\r\n___\u2028___", de.unkrig.lfr.core.Pattern.MULTILINE);
        OracleEssentials.harnessFull(".$", "___\r___\r\n___\u2028___", de.unkrig.lfr.core.Pattern.MULTILINE);
        OracleEssentials.harnessFull("\\b",  " a b c");
        OracleEssentials.harnessFull("\\B",  " a b c");
        OracleEssentials.harnessFull("\\A",  "bla\rbla");
        OracleEssentials.harnessFull("\\Ga", "aaabbb");
        OracleEssentials.harnessFull(".\\Z", "abc");
        OracleEssentials.harnessFull(".\\Z", "abc\n");
        OracleEssentials.harnessFull(".\\Z", "abc\r\nd");
        OracleEssentials.harnessFull(".\\z", "abc\n");
//        OracleEssentials.harnessFull(".\\z", "abc\r\nd"); JRE says !requireEnd !?

        OracleEssentials.harnessFull(".", "abc",      de.unkrig.lfr.core.Pattern.MULTILINE);
        OracleEssentials.harnessFull(".", "abc\n",    de.unkrig.lfr.core.Pattern.MULTILINE);
        OracleEssentials.harnessFull(".", "abc\r\nd", de.unkrig.lfr.core.Pattern.MULTILINE);
    }

    @Test @SuppressWarnings("static-method") public void
    testMatchFlagsGroup() {
        OracleEssentials.harnessFull("a(?i)b", " ab Ab aB AB ");
    }

    @Test @SuppressWarnings("static-method") public void
    testMatchFlagsCapturingGroup() {
        OracleEssentials.harnessFull("a((?i)b)c",       " abc abC aBc aBC Abc AbC ABc ABC ");
        OracleEssentials.harnessFull("a(?<xxx>(?i)b)c", " abc abC aBc aBC Abc AbC ABc ABC ");
    }

    @Test @SuppressWarnings("static-method") public void
    testMatchFlagsNonCapturingGroup() {
        OracleEssentials.harnessFull("a(?i:b)c", " abc abC aBc aBC Abc AbC ABc ABC ");
    }

    @Test @SuppressWarnings("static-method") public void
    testAlternatives() {
        OracleEssentials.harnessFull("a|b",        " a b c ");
        OracleEssentials.harnessFull("a(?:b|bb)c", " ac abc abbc abbbc ");
    }

    @Test @SuppressWarnings("static-method") public void
    testIndependentGroup() {
        OracleEssentials.harnessFull("(?>a|b)",    " a b c ");
        OracleEssentials.harnessFull("a(?>b|bb)c", " ac abc abbc abbbc ");
    }

    // ======================================== CHARACTER CLASSES ========================================

    @Test @SuppressWarnings("static-method") public void
    testPredefinedCharacterClasses() {
        OracleEssentials.harnessFull("\\w",     " abc äöü ");
        OracleEssentials.harnessFull("(?U)\\w", " abc äöü ");
        OracleEssentials.harnessFull("\\W",     " abc äöü ");
        OracleEssentials.harnessFull("(?U)\\W", " abc äöü ");
    }

    @Test @SuppressWarnings("static-method") public void
    testPosixCharacterClasses() {
        OracleEssentials.harnessFull("\\p{Lower}",     " abc äöü ");
        OracleEssentials.harnessFull("(?U)\\p{Lower}", " abc äöü ");
        OracleEssentials.harnessFull("\\P{Lower}",     " abc äöü ");
        OracleEssentials.harnessFull("(?U)\\P{Lower}", " abc äöü ");
    }

    @Test @SuppressWarnings("static-method") public void
    testJavaCharacterClasses() {

        OracleEssentials.harnessFull("\\p{javaLowerCase}", " a B c ä Ä ");
        OracleEssentials.harnessFull("\\P{javaLowerCase}", " a B c ä Ä ");
    }

    @Test @SuppressWarnings("static-method") public void
    testUnicodeCharacterClasses() {

        // By "UNICODE script" - NYI:
//        harness("\\p{IsLatin}",       " a B c ä Ä ");

        // By "UNICODE block":
        OracleEssentials.harnessFull("\\p{InGreek}",       " \u03b1 ");
        OracleEssentials.harnessFull("\\p{InBasicLatin}",  " a B c ä Ä ");
        OracleEssentials.harnessFull("\\P{InBasicLatin}",  " a B c ä Ä ");

        // By "UNICODE category":
        OracleEssentials.harnessFull("\\p{Lu}",            " a B c ä Ä ");
        OracleEssentials.harnessFull("\\P{Lu}",            " a B c ä Ä ");
        OracleEssentials.harnessFull("\\p{Sc}",            " a $ ");
        OracleEssentials.harnessFull("\\P{Sc}",            " a $ ");

        // By "UNICODE property":
        OracleEssentials.harnessFull("\\p{IsLowerCASE}",  " abc äöü ");
        OracleEssentials.harnessFull("\\p{IsAlphabetic}", " abc äöü ");
    }

    // ======================================== END OF CHARACTER CLASSES ========================================

    @Test @SuppressWarnings("static-method") public void
    testCapturingGroups() {
        OracleEssentials.harnessFull("((a+)(b+))", " abbb aabb aaab ");
    }

    @Test @SuppressWarnings("static-method") public void
    testNamedCapturingGroups() {
        OracleEssentials.harnessFull("(?<xxx>a+)", " a aa aaa");

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
    testCapturingGroupsBackreference() {

        // JUR compiles invalid group references ok, and treats them as "no match". DULC, however, is more accurate
        // and reports invalid group references already at COMPILE TIME.
        String regex = "(\\d\\d)\\2";
        assertEquals("  12  ", java.util.regex.Pattern.compile(regex).matcher("  12  ").replaceAll("x"));

        new RegexTest(regex).patternSyntaxExceptionDulc();
    }

    @Test @SuppressWarnings("static-method") public void
    testNamedCapturingGroupsBackreference() {
        OracleEssentials.harnessFull("(?<first>\\w)\\k<first>", " a aa aaa");

        // Backreference to inexistent named group.
        new RegexTest("(?<first>\\w)\\k<bla>").patternSyntaxException();
    }

    @Test @SuppressWarnings("static-method") public void
    testPositiveLookahead() {
        OracleEssentials.harnessFull("a(?=b)",   " a aba abba a");
        OracleEssentials.harnessFull("a(?=(b))", " a aba abba a");
    }

    @Test @SuppressWarnings("static-method") public void
    testNegativeLookahead() {
        OracleEssentials.harnessFull("a(?!b)",   " a aba abba a");
        OracleEssentials.harnessFull("a(?!(b))", " a aba abba a");
    }

    @Test @SuppressWarnings("static-method") public void
    testPositiveLookbehind() {
        OracleEssentials.harnessFull("(?<=b)a",    " a aba abba a");
        OracleEssentials.harnessFull("(?<=(b))a",  " a aba abba a");
        OracleEssentials.harnessFull("(?<=\\R )a", " \r\n a ");
        OracleEssentials.harnessFull("(?<=\\R )a", " \r a ");
        OracleEssentials.harnessFull("(?<=\\R )a", " \n a ");
    }

    @Test @SuppressWarnings("static-method") public void
    testNegativeLookbehind() {
        OracleEssentials.harnessFull("(?<!b)a",   " a aba abba a");
        OracleEssentials.harnessFull("(?<!(b))a", " a aba abba a");
    }

    @Test @SuppressWarnings("static-method") public void
    testRegion() {
        OracleEssentials.harnessFull("a", "__a__ a aba abba __a__", 0, 5, 17);
        OracleEssentials.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17);
        OracleEssentials.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, false, false);
        OracleEssentials.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, false, true);
        OracleEssentials.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, true,  false);
        OracleEssentials.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, true,  true);
    }

    @Test @SuppressWarnings("static-method") public void
    testTransparentBounds() {

        // The for the nineth, failed, match, jur returns "false" for "hitEnd()"!?
//        harness("\\b",     "__a__ a aba abba __a__", 0, 5, 17, true);

        // Lookahead.
        OracleEssentials.harnessFull(" (?=_)",  "__a__ a aba abba __a__", 0, 5, 17, true);
        OracleEssentials.harnessFull(" (?!_)",  "__a__ a aba abba __a__", 0, 5, 17, true);

        // Lookbehind.
        OracleEssentials.harnessFull("(?<=_) ", "__a__ a aba abba __a__", 0, 5, 17, true);
        OracleEssentials.harnessFull("(?<!_) ", "__a__ a aba abba __a__", 0, 5, 17, true);
    }

    @Test @SuppressWarnings("static-method") public void
    testAnchoringBounds() {
        OracleEssentials.harnessFull("^",  "__a__ a aba abba __a__", 0, 5, 17, null, false);
        OracleEssentials.harnessFull("$",  "__a__ a aba abba __a__", 0, 5, 17, null, false);
    }

    @Test @SuppressWarnings("static-method") public void
    testUnixLines() {
        OracleEssentials.harnessFull("\\R",  "  \n  \r\n \u2028 ");
        OracleEssentials.harnessFull("\\R",  "  \n  \r\n \u2028 ", de.unkrig.lfr.core.Pattern.UNIX_LINES);
        OracleEssentials.harnessFull("^",    "  \n  \r\n \u2028 ");
        OracleEssentials.harnessFull("^",    "  \n  \r\n \u2028 ", de.unkrig.lfr.core.Pattern.UNIX_LINES);
    }

    @Test @SuppressWarnings("static-method") public void
    testQuantifiers() {

        OracleEssentials.harnessFull("a?",  " aaa ");
//        OracleEssentials.harnessFull("a??", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        OracleEssentials.harnessFull("a?+", " aaa ");

        OracleEssentials.harnessFull("a*",  " aaa ");
//        OracleEssentials.harnessFull("a*?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        OracleEssentials.harnessFull("a*+", " aaa ");

        OracleEssentials.harnessFull("a+",  " aaa ");
        OracleEssentials.harnessFull("a+?", " aaa ");
        OracleEssentials.harnessFull("a++", " aaa ");

//        OracleEssentials.harnessFull("a{0}",  " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
//        OracleEssentials.harnessFull("a{0}?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
//        OracleEssentials.harnessFull("a{0}+", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?

        OracleEssentials.harnessFull("a{1}",  " aaa ");
        OracleEssentials.harnessFull("a{1}?", " aaa ");
        OracleEssentials.harnessFull("a{1}+", " aaa ");

        OracleEssentials.harnessFull("a{2}",  " aaa ");
        OracleEssentials.harnessFull("a{2}?", " aaa ");
        OracleEssentials.harnessFull("a{2}+", " aaa ");

        OracleEssentials.harnessFull("a{0,}",  " aaa ");
//        OracleEssentials.harnessFull("a{0,}?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        OracleEssentials.harnessFull("a{0,}+", " aaa ");
    }

    @Test @SuppressWarnings("static-method") public void
    testGreedyQuantifierFollowedByLongLiteralString() {

        String infix = "ABCDEFGHIJKLMNOP";

        Producer<String> rsp = randomSubjectProducer(infix);

//        // Verify that the "long literal string" optimization has NOT taken place yet.
//        Assert.assertFalse(classIsLoaded("de.unkrig.lfr.core.Pattern$5$1"));

        for (int i = 0; i < 1000; i++) {
            String subject = AssertionUtil.notNull(rsp.produce());
            harnessMatches(".*" + infix, subject, java.util.regex.Pattern.DOTALL);
        }

//        // Verify that the "long literal string" optimization HAS taken place now.
//        Assert.assertTrue(classIsLoaded("de.unkrig.lfr.core.Pattern$5$1"));
    }

    @Test @SuppressWarnings("static-method") public void
    testReluctantQuantifierFollowedByLongLiteralString() {

        String infix = "ABCDEFGHIJKLMNOP";

        Producer<String> rsp = randomSubjectProducer(infix);

//        // Verify that the "long literal string" optimization has NOT taken place yet.
//        Assert.assertFalse(classIsLoaded("de.unkrig.lfr.core.Pattern$5$1"));

        for (int i = 0; i < 1000; i++) {
            harnessMatches(".*?" + infix, AssertionUtil.notNull(rsp.produce()), java.util.regex.Pattern.DOTALL);
        }

//        // Verify that the "long literal string" optimization HAS taken place now.
//        Assert.assertTrue(classIsLoaded("de.unkrig.lfr.core.Pattern$5$1"));
    }

    @Test @SuppressWarnings("static-method") public void
    testSurrogates() {
        int    clef              = 0x1d120;
        char   clefHighSurrogate = highSurrogateOf(clef);
        char   clefLowSurrogate  = lowSurrogateOf(clef);
        String clefUnicode       = "" + clefHighSurrogate + clefLowSurrogate;

        OracleEssentials.harnessFull(clefUnicode,       clefUnicode);
        OracleEssentials.harnessFull(clefUnicode + "?", "");
        OracleEssentials.harnessFull(clefUnicode + "?", "" + clefHighSurrogate);
        OracleEssentials.harnessFull(clefUnicode + "?", "" + clefLowSurrogate);
        OracleEssentials.harnessFull(clefUnicode + "?", "" + clefHighSurrogate + clefLowSurrogate);
        OracleEssentials.harnessFull(clefUnicode + "?", "" + clefLowSurrogate + clefHighSurrogate);
    }

    @Test @SuppressWarnings("static-method") public void
    testPreviousMatchBoundary() {

        // From: http://stackoverflow.com/questions/2708833
        OracleEssentials.harnessFull(
            "(?<=\\G\\d{3})(?=\\d)" + "|" + "(?<=^-?\\d{1,3})(?=(?:\\d{3})+(?!\\d))",
            "-1234567890.1234567890"
        );
    }

    @Test @SuppressWarnings("static-method") public void
    testAtomicGroups() {
        OracleEssentials.harnessFull("^a(bc|b)c$",   "abc");
        OracleEssentials.harnessFull("^a(bc|b)c$",   "abcc");
        OracleEssentials.harnessFull("^a(?>bc|b)c$", "abc");
        OracleEssentials.harnessFull("^a(?>bc|b)c$", "abcc");
    }

    /**
     * @see <a href="http://stackoverflow.com/questions/17618812">Clarification about requireEnd Matcher's method</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testRequireEnd() {
        OracleEssentials.harnessFull("cat$", "I have a cat");
        OracleEssentials.harnessFull("cat$", "I have a catflap");
        OracleEssentials.harnessFull("cat",  "I have a cat");
        OracleEssentials.harnessFull("cat",  "I have a catflap");

        OracleEssentials.harnessFull("\\d+\\b|[><]=?", "1234");
        OracleEssentials.harnessFull("\\d+\\b|[><]=?", ">=");
        OracleEssentials.harnessFull("\\d+\\b|[><]=?", "<");
    }

    @Test @SuppressWarnings("static-method") public void
    testComments() {
        OracleEssentials.harnessFull(" a# comment \nb ",    " ab a# comment \nb", de.unkrig.lfr.core.Pattern.COMMENTS);
        OracleEssentials.harnessFull("(?x)  a  ",           " a ");
        OracleEssentials.harnessFull("(?x)  a  (?-x) b",    " ab ");
        OracleEssentials.harnessFull("(?x)  a  (?-x) b",    " a b ");
        OracleEssentials.harnessFull("(?x)  a#\n  (?-x) b", " ab ");
        OracleEssentials.harnessFull("(?x)  a#\n  (?-x) b", " a b ");

        OracleEssentials.harnessFull("(?x)  (a)", " a b ");
        OracleEssentials.harnessFull("(?x)  (?:a)", " a b ");
        OracleEssentials.harnessFull("(?x)  ( ?:a)", " a b ");
        OracleEssentials.harnessFull("(?x)  (?: a)", " a b ");
        OracleEssentials.harnessFull("(?x)  (? : a)", " a b ");
        OracleEssentials.harnessFull("(?x)  ( ? :a)", " a b ");
        OracleEssentials.harnessFull("(?x)  ( ?: a)", " a b ");
        OracleEssentials.harnessFull("(?x)  ( ? : a)", " a b ");
        OracleEssentials.harnessFull("(?x)  (?<name>a)", " a b ");
        OracleEssentials.harnessFull("(?x)  ( ?<name>a)", " a b ");
        new RegexTest("(?x)  (? <name>a)").patternSyntaxException();
        OracleEssentials.harnessFull("(?x)  (?< name>a)", " a b ");
        new RegexTest("(?x)  (? < name>a)").patternSyntaxException();
        OracleEssentials.harnessFull("(?x)  ( ?< name>a)", " a b ");
        new RegexTest("(?x)  ( ? < name>a)").patternSyntaxException();
    }

    @Test @SuppressWarnings("static-method") public void
    testAppendReplacementTail() {

        // Verify that "appendReplacement()" without a preceding match throws an Exception.
        new RegexTest("foo").assertEqual(
            " Hello foo and foo!",
            new Transformer<java.util.regex.Matcher, String>() {

                @Override public String
                transform(java.util.regex.Matcher m) {
                    StringBuffer sb = new StringBuffer();
                    Assert.assertSame(m, m.appendReplacement(sb, "bar"));
                    return sb.toString();
                }
            },
            new Transformer<de.unkrig.lfr.core.Matcher, String>() {

                @Override public String
                transform(de.unkrig.lfr.core.Matcher m) {
                    StringBuilder sb = new StringBuilder();
                    Assert.assertSame(m, m.appendReplacement(sb, "bar"));
                    return sb.toString();
                }
            }
        );

        // Verify that "appendReplacement()" and "appendTail()" work.
        final String replacement = "bar";
        new RegexTest("foo").assertEqual(
            " Hello foo and foo!",
            new Transformer<java.util.regex.Matcher, Void>() {

                @Override public Void
                transform(java.util.regex.Matcher m) {

                    {
                        Assert.assertTrue(m.find());
                        StringBuffer sb = new StringBuffer("==");
                        m.appendReplacement(sb, replacement);
                        Assert.assertEquals("== Hello bar", sb.toString());
                    }

                    {
                        Assert.assertTrue(m.find());
                        StringBuffer sb = new StringBuffer("==");
                        m.appendReplacement(sb, replacement);
                        Assert.assertEquals("== and bar", sb.toString());
                    }

                    {
                        Assert.assertFalse(m.find());
                        StringBuffer sb = new StringBuffer("==");
                        m.appendTail(sb);
                        Assert.assertEquals("==!", sb.toString());
                    }

                    return ObjectUtil.almostNull();
                }
            },
            new Transformer<de.unkrig.lfr.core.Matcher, Void>() {

                @Override public Void
                transform(de.unkrig.lfr.core.Matcher m) {

                    {
                        Assert.assertTrue(m.find());
                        StringBuilder sb = new StringBuilder("==");
                        m.appendReplacement(sb, replacement);
                        Assert.assertEquals("== Hello bar", sb.toString());
                    }

                    {
                        Assert.assertTrue(m.find());
                        StringBuilder sb = new StringBuilder("==");
                        m.appendReplacement(sb, replacement);
                        Assert.assertEquals("== and bar", sb.toString());
                    }

                    {
                        Assert.assertFalse(m.find());
                        StringBuilder sb = new StringBuilder("==");
                        m.appendTail(sb);
                        Assert.assertEquals("==!", sb.toString());
                    }

                    return ObjectUtil.almostNull();
                }
            }
        );
    }

    @Test @SuppressWarnings("static-method") public void
    testReplaceAll() {
        PatternTest.harnessReplaceAll("(a)b", " ababaabaaaba ", "$1+", 0);
    }

    @Test @SuppressWarnings("static-method") public void
    testReplaceFirst() {
        PatternTest.harnessReplaceFirst("(a)b", " ababaabaaaba ", "$1+", 0);
    }

    // ===================================

    /**
     * Verifies that {@link java.util.regex.Matcher#matches()} and {@link de.unkrig.lfr.core.Matcher#matches()} yield
     * the same result.
     */
    private static void
    harnessMatches(String regex, String subject) { harnessMatches(regex, subject, 0); }

    /**
     * Verifies that {@link java.util.regex.Matcher#matches()} and {@link de.unkrig.lfr.core.Matcher#matches()} yield
     * the same result.
     */
    private static void
    harnessMatches(String regex, String subject, int flags) {

        String message = "regex=\"" + regex + "\", subject=\"" + subject + "\"";

        java.util.regex.Matcher    matcher1 = java.util.regex.Pattern.compile(regex, flags).matcher(subject);
        de.unkrig.lfr.core.Matcher matcher2 = de.unkrig.lfr.core.Pattern.compile(regex, flags).matcher(subject);

        boolean m1Matches = matcher1.matches();
        boolean m2Matches = matcher2.matches();
        assertEquals("regex=\"" + regex + "\", subject=\"" + subject + "\", matches()", m1Matches, m2Matches);

        OracleEssentials.assertEqualState(message, matcher1, matcher2);
    }

    /**
     * Verifies that {@link java.util.regex.Matcher#lookingAt()} and {@link de.unkrig.lfr.core.Matcher#lookingAt()}
     * yield the same result.
     */
    private static void
    harnessLookingAt(String regex, String subject) {
        java.util.regex.Matcher    m1 = java.util.regex.Pattern.compile(regex).matcher(subject);
        de.unkrig.lfr.core.Matcher m2 = de.unkrig.lfr.core.Pattern.compile(regex).matcher(subject);
        assertEquals(m1.lookingAt(), m2.lookingAt());
        OracleEssentials.assertEqualState("regex=\"" + regex + "\", subject=\"" + subject + "\"", m1, m2);
    }

    private static void
    harnessReplaceAll(final String regex, final String subject, final String replacement, int flags) {

        RegexTest rt = new RegexTest(regex);
        rt.setFlags(flags);
        rt.assertMatchers(subject, new MatcherAssertion() {

            @Override public void
            assertMatchers(java.util.regex.Matcher matcher1, de.unkrig.lfr.core.Matcher matcher2) {

                String result1 = matcher1.replaceAll(replacement);
                String result2 = matcher2.replaceAll(replacement);

                Assert.assertEquals(
                    "regex=\"" + regex + "\", subject=\"" + subject + "\", replacement=\"" + replacement + "\"",
                    result1,
                    result2
                );
            }
        });
    }

    private static void
    harnessReplaceFirst(final String regex, final String subject, final String replacement, int flags) {

        RegexTest rt = new RegexTest(regex);
        rt.setFlags(flags);
        rt.assertMatchers(subject, new MatcherAssertion() {

            @Override public void
            assertMatchers(java.util.regex.Matcher matcher1, de.unkrig.lfr.core.Matcher matcher2) {

                String result1 = matcher1.replaceFirst(replacement);
                String result2 = matcher2.replaceFirst(replacement);

                Assert.assertEquals(
                    "regex=\"" + regex + "\", subject=\"" + subject + "\", replacement=\"" + replacement + "\"",
                    result1,
                    result2
                );
            }
        });
    }

    // =====================================

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

    private static boolean
    classIsLoaded(String className) {

        try {
            return FIND_LOADED_CLASS.invoke(ClassLoader.getSystemClassLoader(), className) != null;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
    private static final java.lang.reflect.Method FIND_LOADED_CLASS;
    static {
        try {
            FIND_LOADED_CLASS = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            FIND_LOADED_CLASS.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
