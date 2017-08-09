
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
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import test.RegexTest.MatcherAssertion;

public
class PatternTest {

    @BeforeClass public static void
    setUpBeforeClass() { OracleEssentials.beginStatistics(); }

    @AfterClass public static void
    tearDownAfterClass() { OracleEssentials.endStatistics(); }

    @SuppressWarnings("static-method") @Test public void
    testMatches() {
        PatternTest.harnessMatches("abc", "abc");
        PatternTest.harnessMatches("abc", "abcxx");
        PatternTest.harnessMatches("abc", "xxabc");
        PatternTest.harnessMatches("a.c", "aBc");
        PatternTest.harnessMatches("a.c", "aBcxx");
        PatternTest.harnessMatches("a.*c", "axxxc");
        PatternTest.harnessMatches("a.*c", "axxxcxxxc");
        PatternTest.harnessMatches("a.*c", "axxx");
    }

    @SuppressWarnings("static-method") @Test public void
    testLiteralOctals() {
        PatternTest.harnessMatches("\\00xx",   "\0xx");
        PatternTest.harnessMatches("\\01xx",   "\01xx");
        PatternTest.harnessMatches("\\011xx",  "\011xx");
        PatternTest.harnessMatches("\\0101xx",  "Axx");
        PatternTest.harnessMatches("\\0111xx",  "\0111xx");
    }

    @SuppressWarnings("static-method") @Test public void
    testShortStringLiterals() {

        String infix = "ABCDEFGHIJKLMNO";

        String regex = infix;

        Assert.assertEquals(
            "naive(\"ABCDEFGHIJKLMNO\") . end",
            OracleEssentials.LFR.compile(regex).sequenceToString()
        );

        Producer<String> sp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 1000; i++) {
            OracleEssentials.harnessFull(regex, AssertionUtil.notNull(sp.produce()));
        }
    }

    @SuppressWarnings("static-method") @Test public void
    testLongStringLiterals() {

        String infix = "ABCDEFGHIJKLMNOP";

        String regex = infix;

        Assert.assertEquals(
            "knuthMorrisPratt(\"ABCDEFGHIJKLMNOP\") . end",
            OracleEssentials.LFR.compile(regex).sequenceToString()
        );

        Producer<String> sp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 1000; i++) {
            OracleEssentials.harnessFull(regex, AssertionUtil.notNull(sp.produce()));
        }
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
        PatternTest.harnessLookingAt("abc", "abcdef");
        PatternTest.harnessLookingAt("aBc", "abcdef");
        PatternTest.harnessLookingAt("a.c", "abcdef");
    }

    @Test @SuppressWarnings("static-method") public void
    testCaseInsensitive() {
        OracleEssentials.harnessFull("(?i)A", "xxxAxxx");
        OracleEssentials.harnessFull("(?i)A", "xxxaxxx");
        OracleEssentials.harnessFull("(?i)Ä", "xxxäxxx");
        Assert.assertTrue(OracleEssentials.LFR.matches("(?i)Ä", "Ä"));
        Assert.assertFalse(OracleEssentials.LFR.matches("(?i)Ä", "ä"));
    }

    @Test @SuppressWarnings("static-method") public void
    testUnicodeCaseInsensitive() {
        OracleEssentials.harnessFull("(?ui)A", "xxxAxxx");
        OracleEssentials.harnessFull("(?ui)A", "xxxaxxx");
        OracleEssentials.harnessFull("(?ui)Ä", "xxxäxxx");
        Assert.assertTrue(OracleEssentials.LFR.matches("(?ui)Ä", "Ä"));
        Assert.assertTrue(OracleEssentials.LFR.matches("(?ui)Ä", "ä"));
    }

    @Test @SuppressWarnings("static-method") public void
    testDotall() {
        OracleEssentials.harnessFull(".",     " \r  ");
        OracleEssentials.harnessFull(".",     " \r  ", Pattern.DOTALL);
        OracleEssentials.harnessFull("(?s).", " \r  ");
    }

    @Test @SuppressWarnings("static-method") public void
    testLiteralRegex() {
        OracleEssentials.harnessFull("$\\*", "$\\*xxx$\\*xxx", Pattern.LITERAL);
        OracleEssentials.harnessFull("a\\", "a\\xxxA\\xxx",    Pattern.LITERAL | Pattern.CASE_INSENSITIVE); // SUPPRESS CHECKSTYLE LineLength
        OracleEssentials.harnessFull(".\\Q.\\E.", " ___ ");
        OracleEssentials.harnessFull(".\\Q.\\E.", " _._ ");
    }

    @Test @SuppressWarnings("static-method") public void
    testBoundaries() {
        OracleEssentials.harnessFull("^.", "___\r___\r\n___\u2028___");
        OracleEssentials.harnessFull(".$", "___\r___\r\n___\u2028___");
        OracleEssentials.harnessFull("^.", "___\r___\r\n___\u2028___", Pattern.MULTILINE);
        OracleEssentials.harnessFull(".$", "___\r___\r\n___\u2028___", Pattern.MULTILINE);
        OracleEssentials.harnessFull("\\b",  " a b c");
        OracleEssentials.harnessFull("\\B",  " a b c");
        OracleEssentials.harnessFull("\\A",  "bla\rbla");
        OracleEssentials.harnessFull("\\Ga", "aaabbb");
        OracleEssentials.harnessFull(".\\Z", "abc");
        OracleEssentials.harnessFull(".\\Z", "abc\n");
        OracleEssentials.harnessFull(".\\Z", "abc\r\nd");
        OracleEssentials.harnessFull(".\\z", "abc\n");
//        OracleEssentials.harnessFull(".\\z", "abc\r\nd"); JRE says !requireEnd !?

        OracleEssentials.harnessFull(".", "abc",      Pattern.MULTILINE);
        OracleEssentials.harnessFull(".", "abc\n",    Pattern.MULTILINE);
        OracleEssentials.harnessFull(".", "abc\r\nd", Pattern.MULTILINE);
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

        Matcher matcher = OracleEssentials.LFR.compile("(?<xxx>a+)").matcher(" a aa aaa");

        Assert.assertTrue(matcher.find());
        Assert.assertEquals("a", matcher.group("xxx"));

        Assert.assertTrue(matcher.find());
        Assert.assertEquals("aa", matcher.group("xxx"));

        Assert.assertTrue(matcher.find());
        Assert.assertEquals("aaa", matcher.group("xxx"));

        Assert.assertFalse(matcher.find());
    }

    @Test @SuppressWarnings("static-method") public void
    testCapturingGroupsBackreference() {

        // JUR compiles invalid group references ok, and treats them as "no match". DULC, however, is more accurate
        // and reports invalid group references already at COMPILE TIME.
        String regex = "(\\d\\d)\\2";
        Assert.assertEquals("  12  ", java.util.regex.Pattern.compile(regex).matcher("  12  ").replaceAll("x"));

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

        OracleEssentials.harnessFull("(?<=^\t*)\t", "\t\t\tpublic static void main()");
        PatternTest.harnessReplaceAll("(?<=^\\s*)    ", "        public static void main()", "\t");
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
        OracleEssentials.harnessFull("\\R",  "  \n  \r\n \u2028 ", Pattern.UNIX_LINES);
        OracleEssentials.harnessFull("^",    "  \n  \r\n \u2028 ");
        OracleEssentials.harnessFull("^",    "  \n  \r\n \u2028 ", Pattern.UNIX_LINES);
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

        String regex = ".*" + infix;

        PatternTest.assertSequenceToString(
            "greedyQuantifier(negate(lineBreakCharacter), min=0, max=infinite) . knuthMorrisPratt(\"ABCDEFGHIJKLMNOP\") . end", // SUPPRESS CHECKSTYLE LineLength
            regex
        );

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 1000; i++) {
            String subject = AssertionUtil.notNull(rsp.produce());
            PatternTest.harnessMatches(regex, subject, Pattern.DOTALL);
        }
    }

    @Test @SuppressWarnings("static-method") public void
    testReluctantQuantifierFollowedByLongLiteralString() {

        String infix = "ABCDEFGHIJKLMNOP";

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);

        Assert.assertEquals(
            "knuthMorrisPratt(\"ABCDEFGHIJKLMNOP\") . end",
            OracleEssentials.LFR.compile(infix).sequenceToString()
        );

        for (int i = 0; i < 1000; i++) {
            PatternTest.harnessMatches(".*?" + infix, AssertionUtil.notNull(rsp.produce()), Pattern.DOTALL);
        }
    }

    @Test @SuppressWarnings("static-method") public void
    testSurrogates() {
        int    clef              = 0x1d120;
        char   clefHighSurrogate = PatternTest.highSurrogateOf(clef);
        char   clefLowSurrogate  = PatternTest.lowSurrogateOf(clef);
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
        OracleEssentials.harnessFull(" a# comment \nb ",    " ab a# comment \nb", Pattern.COMMENTS);
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
            new Transformer<Matcher, String>() {

                @Override public String
                transform(Matcher m) {
                    StringBuffer sb = new StringBuffer();
                    Assert.assertSame(m, m.appendReplacement(sb, "bar"));
                    return sb.toString();
                }
            },
            new Transformer<Matcher, String>() {

                @Override public String
                transform(Matcher m) {
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
            new Transformer<Matcher, Void>() {

                @Override public Void
                transform(Matcher m) {

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
            new Transformer<Matcher, Void>() {

                @Override public Void
                transform(Matcher m) {

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
        PatternTest.harnessReplaceAll("(a)b", " ababaabaaaba ", "$1+");
    }

    @Test @SuppressWarnings("static-method") public void
    testReplaceFirst() {
        PatternTest.harnessReplaceFirst("(a)b", " ababaabaaaba ", "$1+");
    }

    @Test @SuppressWarnings("static-method") public void
    testOptimizations() {
        PatternTest.assertSequenceToString("'A' . end", "A");

        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifier(negate(lineBreakCharacter), min=0, max=infinite) . 'B' . end",
            "A.*B"
        );

        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifier(negate(lineBreakCharacter), min=0, max=infinite) . naive(\"BC\") . end",
            "A.*BC"
        );

        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=naive(\"BC\")) . end",
            "A.*BC",
            Pattern.DOTALL
        );

        PatternTest.assertSequenceToString(
            "'A' . reluctantQuantifierSequenceAnyChar(min=0, max=infinite, ls=naive(\"BC\")) . end",
            "A.*?BC",
            Pattern.DOTALL
        );

        PatternTest.assertSequenceToString(
            "'A' . possessiveQuantifierSequenceOfAnyChar(min=0, max=infinite) . naive(\"BC\") . end",
            "A.*+BC",
            Pattern.DOTALL
        );

        // Naive string search, because the string literal is only 14 characters long.
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=naive(\"abcdefghijklmno\")) . end",
            "A.*abcdefghijklmno",
            Pattern.DOTALL
        );

        // Knuth-Morris-Pratt string search, because the string literal is 15 characters long.
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=knuthMorrisPratt(\"abcdefghijklmnop\")) . end",
            "A.*abcdefghijklmnop",
            Pattern.DOTALL
        );
    }

    // ===================================

    /**
     * Verifies that {@link java.util.regex.Matcher#matches()} and {@link de.unkrig.lfr.core.Matcher#matches()} yield
     * the same result.
     */
    private static void
    harnessMatches(String regex, String subject) { PatternTest.harnessMatches(regex, subject, 0); }

    /**
     * Verifies that {@link java.util.regex.Matcher#matches()} and {@link de.unkrig.lfr.core.Matcher#matches()} yield
     * the same result.
     */
    private static void
    harnessMatches(String regex, String subject, int flags) {

        Matcher matcher1 = OracleEssentials.JUR.compile(regex, flags).matcher(subject);
        Matcher matcher2 = OracleEssentials.LFR.compile(regex, flags).matcher(subject);

        boolean m1Matches = matcher1.matches();
        boolean m2Matches = matcher2.matches();
        Assert.assertEquals("regex=\"" + regex + "\", subject=\"" + subject + "\", matches()", m1Matches, m2Matches);

        RegexTest.ASSERT_EQUAL_STATE.assertMatchers(matcher1, matcher2);
    }

    /**
     * Verifies that {@link java.util.regex.Matcher#lookingAt()} and {@link de.unkrig.lfr.core.Matcher#lookingAt()}
     * yield the same result.
     */
    private static void
    harnessLookingAt(String regex, String subject) {
        Matcher m1 = OracleEssentials.JUR.compile(regex).matcher(subject);
        Matcher m2 = OracleEssentials.LFR.compile(regex).matcher(subject);
        RegexTest.ASSERT_LOOKING_AT.assertMatchers(m1, m2);
        RegexTest.ASSERT_EQUAL_STATE.assertMatchers(m1, m2);
    }

    private static void
    harnessReplaceAll(final String regex, final String subject, final String replacement) {
        PatternTest.harnessReplaceAll(regex, subject, replacement, 0);
    }

    private static void
    harnessReplaceAll(final String regex, final String subject, final String replacement, int flags) {

        RegexTest rt = new RegexTest(regex);
        rt.setFlags(flags);
        rt.assertMatchers(subject, new MatcherAssertion() {

            @Override public void
            assertMatchers(Matcher expected, Matcher actual) {

                String result1 = expected.replaceAll(replacement);
                String result2 = actual.replaceAll(replacement);

                Assert.assertEquals(
                    "regex=\"" + regex + "\", subject=\"" + subject + "\", replacement=\"" + replacement + "\"",
                    result1,
                    result2
                );
            }
        });
    }

    private static void
    harnessReplaceFirst(final String regex, final String subject, final String replacement) {
        PatternTest.harnessReplaceFirst(regex, subject, replacement, 0);
    }

    private static void
    harnessReplaceFirst(final String regex, final String subject, final String replacement, int flags) {

        RegexTest rt = new RegexTest(regex);
        rt.setFlags(flags);
        rt.assertMatchers(subject, new MatcherAssertion() {

            @Override public void
            assertMatchers(Matcher expected, Matcher actual) {

                String result1 = expected.replaceFirst(replacement);
                String result2 = actual.replaceFirst(replacement);

                Assert.assertEquals(
                    "regex=\"" + regex + "\", subject=\"" + subject + "\", replacement=\"" + replacement + "\"",
                    result1,
                    result2
                );
            }
        });
    }

    private static void
    assertSequenceToString(String expected, String regex) {
        PatternTest.assertSequenceToString(expected, regex, 0);
    }

    private static void
    assertSequenceToString(String expected, String regex, int flags) {
        Assert.assertEquals(expected, OracleEssentials.LFR.compile(regex, flags).sequenceToString());
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
}
