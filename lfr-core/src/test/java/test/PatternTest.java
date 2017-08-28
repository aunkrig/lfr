
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

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;

public
class PatternTest {

    /**
     * The {@code java.util.regex} pattern factory.
     */
    public static final PatternFactory
    JUR = de.unkrig.ref4j.jur.PatternFactory.INSTANCE;

    /**
     * The {@code LFR} pattern factory.
     */
    public static final de.unkrig.lfr.core.PatternFactory
    LFR = de.unkrig.lfr.core.PatternFactory.INSTANCE;

    /**
     * The pattern factory that verfies the functional equality of JUR and LFR.
     */
    public static final FunctionalityEquivalencePatternFactory
    FE  = OracleEssentials.FE;

    private static final boolean JRE6 = System.getProperty("java.specification.version").equals("1.6");

    @SuppressWarnings("static-method") @Test public void
    testMatches() {
        PatternTest.FE.compile("abc", 0).matcher("abc").matches();
        PatternTest.FE.compile("abc", 0).matcher("abcxx").matches();
        PatternTest.FE.compile("abc", 0).matcher("xxabc").matches();
        PatternTest.FE.compile("a.c", 0).matcher("aBc").matches();
        PatternTest.FE.compile("a.c", 0).matcher("aBcxx").matches();
        PatternTest.FE.compile("a.*c", 0).matcher("axxxc").matches();
        PatternTest.FE.compile("a.*c", 0).matcher("axxxcxxxc").matches();
        PatternTest.FE.compile("a.*c", 0).matcher("axxx").matches();
    }

    @SuppressWarnings("static-method") @Test public void
    testLiteralOctals() {
        PatternTest.FE.compile("\\00xx", 0).matcher("\0xx").matches();
        PatternTest.FE.compile("\\01xx", 0).matcher("\01xx").matches();
        PatternTest.FE.compile("\\011xx", 0).matcher("\011xx").matches();
        PatternTest.FE.compile("\\0101xx", 0).matcher("Axx").matches();
        PatternTest.FE.compile("\\0111xx", 0).matcher("\0111xx").matches();
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
        FE.compile("abc").matcher("abcdef").lookingAt();
        FE.compile("aBc").matcher("abcdef").lookingAt();
        FE.compile("a.c").matcher("abcdef").lookingAt();
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
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("a(?<xxx>(?i)b)c", " abc abC aBc aBC Abc AbC ABc ABC ");
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
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?U)\\w", " abc äöü ");
        OracleEssentials.harnessFull("\\W",     " abc äöü ");
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?U)\\W", " abc äöü ");
    }

    @Test @SuppressWarnings("static-method") public void
    testPosixCharacterClasses() {
        OracleEssentials.harnessFull("\\p{Lower}",     " abc äöü ");
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?U)\\p{Lower}", " abc äöü ");
        OracleEssentials.harnessFull("\\P{Lower}",     " abc äöü ");
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?U)\\P{Lower}", " abc äöü ");
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
        if (!PatternTest.JRE6) {
            OracleEssentials.harnessFull("\\p{IsLowerCASE}",  " abc äöü ");
            OracleEssentials.harnessFull("\\p{IsAlphabetic}", " abc äöü ");
        }
    }

    // ======================================== END OF CHARACTER CLASSES ========================================

    @Test @SuppressWarnings("static-method") public void
    testCapturingGroups() {
        OracleEssentials.harnessFull("((a+)(b+))", " abbb aabb aaab ");
    }

    @Test @SuppressWarnings("static-method") public void
    testNamedCapturingGroups() {

        if (PatternTest.JRE6) return;

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

        // "\2" is an invalid backreference, which results in a match failure.
        PatternTest.FE.compile("(\\d\\d)\\2").matcher(" a aa aaa").replaceAll("x");
    }

    @Test @SuppressWarnings("static-method") public void
    testNamedCapturingGroupsBackreference() {

        if (PatternTest.JRE6) return;

        OracleEssentials.harnessFull("(?<first>\\w)\\k<first>", " a aa aaa");

        // Backreference to inexistent named group.
        PatternTest.FE.assertPatternSyntaxException("(?<first>\\w)\\k<bla>");
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
        if (!PatternTest.JRE6) {
            OracleEssentials.harnessFull("(?<=\\R )a", " \r\n a ");
            OracleEssentials.harnessFull("(?<=\\R )a", " \r a ");
            OracleEssentials.harnessFull("(?<=\\R )a", " \n a ");
        }

        OracleEssentials.harnessFull("(?<=^\t*)\t", "\t\t\tpublic static void main()");
        PatternTest.FE.compile("(?<=^\\s*)    ").matcher("        public static void main()").replaceAll("\t");
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
        if (!PatternTest.JRE6) {
            OracleEssentials.harnessFull("\\R",  "  \n  \r\n \u2028 ");
            OracleEssentials.harnessFull("\\R",  "  \n  \r\n \u2028 ", Pattern.UNIX_LINES);
        }
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
            "greedyQuantifier(operand=negate(lineBreakCharacter), min=0) . knuthMorrisPratt(\"ABCDEFGHIJKLMNOP\") . end", // SUPPRESS CHECKSTYLE LineLength
            regex
        );

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 1000; i++) {
            String subject = AssertionUtil.notNull(rsp.produce());
            PatternTest.FE.compile(regex, Pattern.DOTALL).matcher(subject).matches();
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
            String subject = rsp.produce();
            PatternTest.FE.compile(".*?" + infix, Pattern.DOTALL).matcher(AssertionUtil.notNull(subject)).matches();
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
        if (!PatternTest.JRE6) {
            OracleEssentials.harnessFull("(?x)  (?<name>a)", " a b ");
            OracleEssentials.harnessFull("(?x)  ( ?<name>a)", " a b ");
            PatternTest.FE.assertPatternSyntaxException("(?x)  (? <name>a)");
            OracleEssentials.harnessFull("(?x)  (?< name>a)", " a b ");
            PatternTest.FE.assertPatternSyntaxException("(?x)  (? < name>a)");
            OracleEssentials.harnessFull("(?x)  ( ?< name>a)", " a b ");
            PatternTest.FE.assertPatternSyntaxException("(?x)  ( ? < name>a)");
        }
    }

    @Test @SuppressWarnings("static-method") public void
    testAppendReplacementTail() {

        // Verify that "appendReplacement()" without a preceding match throws an Exception.
        PatternTest.FE.compile("foo").matcher(" Hello foo and foo!").appendReplacement(new StringBuffer(), "bar");

        // Verify that "appendReplacement()" and "appendTail()" work.
        Matcher m = PatternTest.FE.compile("foo").matcher(" Hello foo and foo!");
        Assert.assertTrue(m.find());
        StringBuffer sb = new StringBuffer("==");
        m.appendReplacement(sb, "bar");
        Assert.assertEquals("== Hello bar", sb.toString());
    }

    @Test @SuppressWarnings("static-method") public void
    testCharacterClassOptimizations() {
        PatternTest.assertSequenceToString("'A' . end",                                            "[A]");
        PatternTest.assertSequenceToString("oneOfTwo('A', 'B') . end",                             "[AB]");
        PatternTest.assertSequenceToString("oneOfTwo('A', 'K') . end",                             "[AK]");
        PatternTest.assertSequenceToString("bitSet('A', 'C', 'E', 'G', 'I', 'K') . end",           "[ACEGIK]");
        PatternTest.assertSequenceToString("range('A' - 'E') . end",                               "[A-E]");
        PatternTest.assertSequenceToString("bitSet('D', 'E', 'F', 'G', 'H', 'I', 'J', 'K') . end", "[A-K&&D-Z]");
        PatternTest.assertSequenceToString(PatternTest.jurpc("set\\('.'(?:, '.'){63}\\) . end"),   "[A-Za-z0-9_\u0400]"); // SUPPRESS CHECKSTYLE LineLength
    }

    @Test @SuppressWarnings("static-method") public void
    testQuantifierOptimizations1() {
        PatternTest.assertSequenceToString("'A' . end", "A");
    }

    @Test @SuppressWarnings("static-method") public void
    testQuantifierOptimizations2() {
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifier(operand=negate(lineBreakCharacter), min=0) . 'B' . end",
            "A.*B"
        );
    }

    @Test @SuppressWarnings("static-method") public void
    testQuantifierOptimizations3() {
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifier(operand=negate(lineBreakCharacter), min=0) . naive(\"BC\") . end",
            "A.*BC"
        );
    }

    @Test @SuppressWarnings("static-method") public void
    testQuantifierOptimizations4() {
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=naive(\"BC\")) . end",
            "A.*BC",
            Pattern.DOTALL
        );
    }

    @Test @SuppressWarnings("static-method") public void
    testQuantifierOptimizations5() {
        PatternTest.assertSequenceToString(
            "'A' . reluctantQuantifierSequenceAnyChar(min=0, max=infinite, ls=naive(\"BC\")) . end",
            "A.*?BC",
            Pattern.DOTALL
        );
    }

    @Test @SuppressWarnings("static-method") public void
    testQuantifierOptimizations6() {
        PatternTest.assertSequenceToString(
            "'A' . possessiveQuantifierSequenceOfAnyChar(min=0, max=infinite) . naive(\"BC\") . end",
            "A.*+BC",
            Pattern.DOTALL
        );
    }

    @Test @SuppressWarnings("static-method") public void
    testQuantifierOptimizations7() {

        // Naive string search, because the string literal is only 14 characters long.
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=naive(\"abcdefghijklmno\")) . end",
            "A.*abcdefghijklmno",
            Pattern.DOTALL
        );
    }

    @Test @SuppressWarnings("static-method") public void
    testQuantifierOptimizations8() {

        // Knuth-Morris-Pratt string search, because the string literal is 15 characters long.
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=knuthMorrisPratt(\"abcdefghijklmnop\")) . end",
            "A.*abcdefghijklmnop",
            Pattern.DOTALL
        );
    }

    @Test @SuppressWarnings("static-method") public void
    testCaseInsensitiveMatch() {
        char[] tripleCaseLetters = { 452, 453, 454, 455, 456, 457, 458, 459, 460, 497, 498, 499 };

        for (char c : tripleCaseLetters) {
            OracleEssentials.harnessFull(new String(new char[] { c }), new String(tripleCaseLetters));
            OracleEssentials.harnessFull(new String(new char[] { c }), new String(tripleCaseLetters), Pattern.CASE_INSENSITIVE); // SUPPRESS CHECKSTYLE LineLength
        }
    }

    private static void
    assertSequenceToString(String expected, String regex) {
        PatternTest.assertSequenceToString(expected, regex, 0);
    }

    private static void
    assertSequenceToString(String expected, String regex, int flags) {
        Assert.assertEquals(expected, OracleEssentials.LFR.compile(regex, flags).sequenceToString());
    }

    private static void
    assertSequenceToString(java.util.regex.Pattern expected, String regex) {
        PatternTest.assertSequenceToString(expected, regex, 0);
    }

    private static void
    assertSequenceToString(java.util.regex.Pattern expected, String regex, int flags) {
        String s = OracleEssentials.LFR.compile(regex, flags).sequenceToString();
        Assert.assertTrue(
            "\"" + s + "\" does not match \"" + expected.toString() + "\"",
            expected.matcher(s).matches()
        );
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

    private static java.util.regex.Pattern
    jurpc(String regex) { return java.util.regex.Pattern.compile(regex); }
}
