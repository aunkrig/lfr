
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
import java.util.regex.PatternSyntaxException;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;

/**
 * Tests the {@link Pattern} class (and its relative {@link Matcher}).
 */
@SuppressWarnings("static-method") public
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
    public static final PatternFactory
    PF = OracleEssentials.PF;

    private static final boolean JRE6 = System.getProperty("java.specification.version").equals("1.6");

    @Test public void testMatches1() { PatternTest.PF.compile("abc", 0).matcher("abc").matches(); }
    @Test public void testMatches2() { PatternTest.PF.compile("abc", 0).matcher("abcxx").matches(); }
    @Test public void testMatches3() { PatternTest.PF.compile("abc", 0).matcher("xxabc").matches(); }
    @Test public void testMatches4() { PatternTest.PF.compile("a.c", 0).matcher("aBc").matches(); }
    @Test public void testMatches5() { PatternTest.PF.compile("a.c", 0).matcher("aBcxx").matches(); }
    @Test public void testMatches6() { PatternTest.PF.compile("a.*c", 0).matcher("axxxc").matches(); }
    @Test public void testMatches7() { PatternTest.PF.compile("a.*c", 0).matcher("axxxcxxxc").matches(); }
    @Test public void testMatches8() { PatternTest.PF.compile("a.*c", 0).matcher("axxx").matches(); }

    @Test public void testLiteralOctals1() { PatternTest.PF.compile("\\00xx", 0).matcher("\0xx").matches(); }
    @Test public void testLiteralOctals2() { PatternTest.PF.compile("\\01xx", 0).matcher("\01xx").matches(); }
    @Test public void testLiteralOctals3() { PatternTest.PF.compile("\\011xx", 0).matcher("\011xx").matches(); }
    @Test public void testLiteralOctals4() { PatternTest.PF.compile("\\0101xx", 0).matcher("Axx").matches(); }
    @Test public void testLiteralOctals5() { PatternTest.PF.compile("\\0111xx", 0).matcher("\0111xx").matches(); }

    @Test public void
    testShortStringLiterals() {

        String infix = "ABCDEFGHIJKLMNO";

        String regex = infix;

        PatternTest.assertSequenceToString("naive(\"ABCDEFGHIJKLMNO\")", regex);

        Producer<String> sp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 10; i++) {
            OracleEssentials.harnessFull(regex, AssertionUtil.notNull(sp.produce()));
        }
    }

    @Test public void
    testLongStringLiterals() {

        String infix = "ABCDEFGHIJKLMNOP";

        String regex = infix;

        PatternTest.assertSequenceToString("knuthMorrisPratt(\"ABCDEFGHIJKLMNOP\")", regex);

        Producer<String> sp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 10; i++) {
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

    @Test public void testFind1() { OracleEssentials.harnessFull("abc",   "abc"); }
    @Test public void testFind2() { OracleEssentials.harnessFull("abc",   "xxabcxx"); }
    @Test public void testFind3() { OracleEssentials.harnessFull("abc",   "xxaBcxx"); }
    @Test public void testFind4() { OracleEssentials.harnessFull("a.c",   "xxabcxx"); }
    @Test public void testFind5() { OracleEssentials.harnessFull("a.*b",  "xxaxxbxxbxxbxx"); }
    @Test public void testFind6() { OracleEssentials.harnessFull("a.*?b", "xxaxxbxxbxxbxx"); }
    @Test public void testFind7() { OracleEssentials.harnessFull("a.*+b", "xxaxxbxxbxxbxx"); }

    @Test public void testLookingAt1() { PatternTest.PF.compile("abc").matcher("abcdef").lookingAt(); }
    @Test public void testLookingAt2() { PatternTest.PF.compile("aBc").matcher("abcdef").lookingAt(); }
    @Test public void testLookingAt3() { PatternTest.PF.compile("a.c").matcher("abcdef").lookingAt(); }

    @Test public void testCaseInsensitive1() { OracleEssentials.harnessFull("(?i)A", "xxxAxxx"); }
    @Test public void testCaseInsensitive2() { OracleEssentials.harnessFull("(?i)A", "xxxaxxx"); }
    @Test public void testCaseInsensitive3() { OracleEssentials.harnessFull("(?i)Ä", "xxxäxxx"); }
    @Test public void testCaseInsensitive4() { Assert.assertTrue(OracleEssentials.LFR.matches("(?i)Ä", "Ä")); }
    @Test public void testCaseInsensitive5() { Assert.assertFalse(OracleEssentials.LFR.matches("(?i)Ä", "ä")); }

    @Test public void testUnicodeCaseInsensitive1() { OracleEssentials.harnessFull("(?ui)A", "xxxAxxx"); }
    @Test public void testUnicodeCaseInsensitive2() { OracleEssentials.harnessFull("(?ui)A", "xxxaxxx"); }
    @Test public void testUnicodeCaseInsensitive3() { OracleEssentials.harnessFull("(?ui)Ä", "xxxäxxx"); }
    @Test public void testUnicodeCaseInsensitive4() { Assert.assertTrue(OracleEssentials.LFR.matches("(?ui)Ä", "Ä")); }
    @Test public void testUnicodeCaseInsensitive5() { Assert.assertTrue(OracleEssentials.LFR.matches("(?ui)Ä", "ä")); }

    @Test public void testDotall1() { OracleEssentials.harnessFull(".",     " \r  "); }
    @Test public void testDotall2() { OracleEssentials.harnessFull(".",     " \r  ", Pattern.DOTALL); }
    @Test public void testDotall3() { OracleEssentials.harnessFull("(?s).", " \r  "); }

    @Test public void testLiteralRegex1() { OracleEssentials.harnessFull("$\\*", "$\\*xxx$\\*xxx", Pattern.LITERAL); }
    @Test public void testLiteralRegex2() { OracleEssentials.harnessFull("a\\", "a\\xxxA\\xxx",    Pattern.LITERAL | Pattern.CASE_INSENSITIVE); } // SUPPRESS CHECKSTYLE LineLength
    @Test public void testLiteralRegex3() { OracleEssentials.harnessFull(".\\Q.\\E.", " ___ "); }
    @Test public void testLiteralRegex4() { OracleEssentials.harnessFull(".\\Q.\\E.", " _._ "); }

    @Test public void testBoundaries1()  { OracleEssentials.harnessFull("^.", "___\r___\r\n___\u2028___"); }
    @Test public void testBoundaries2()  { OracleEssentials.harnessFull(".$", "___\r___\r\n___\u2028___"); }
    @Test public void testBoundaries3()  { OracleEssentials.harnessFull("^.", "___\r___\r\n___\u2028___", Pattern.MULTILINE); } // SUPPRESS CHECKSTYLE LineLength:2
    @Test public void testBoundaries4()  { OracleEssentials.harnessFull(".$", "___\r___\r\n___\u2028___", Pattern.MULTILINE); }
    @Test public void testBoundaries5()  { OracleEssentials.harnessFull("\\b",  " a b c"); }
    @Test public void testBoundaries6()  { OracleEssentials.harnessFull("\\B",  " a b c"); }
    @Test public void testBoundaries7()  { OracleEssentials.harnessFull("\\A",  "bla\rbla"); }
    @Test public void testBoundaries8()  { OracleEssentials.harnessFull("\\Ga", "aaabbb"); }
    @Test public void testBoundaries9()  { OracleEssentials.harnessFull(".\\Z", "abc"); }
    @Test public void testBoundaries10() { OracleEssentials.harnessFull(".\\Z", "abc\n"); }
    @Test public void testBoundaries11() { OracleEssentials.harnessFull(".\\Z", "abc\r\nd"); }
    @Test public void testBoundaries12() { OracleEssentials.harnessFull(".\\z", "abc\n"); }
    //@Test public void testBoundaries13() { OracleEssentials.harnessFull(".\\z", "abc\r\nd"); } JRE says !requireEnd !?
    @Test public void testBoundaries14() { OracleEssentials.harnessFull(".", "abc",      Pattern.MULTILINE); }
    @Test public void testBoundaries15() { OracleEssentials.harnessFull(".", "abc\n",    Pattern.MULTILINE); }
    @Test public void testBoundaries16() { OracleEssentials.harnessFull(".", "abc\r\nd", Pattern.MULTILINE); }

    @Test public void
    testMatchFlagsGroup() {
        OracleEssentials.harnessFull("a(?i)b", " ab Ab aB AB ");
    }

    @Test public void
    testMatchFlagsCapturingGroup() {
        OracleEssentials.harnessFull("a((?i)b)c",       " abc abC aBc aBC Abc AbC ABc ABC ");
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("a(?<xxx>(?i)b)c", " abc abC aBc aBC Abc AbC ABc ABC ");
    }

    @Test public void
    testMatchFlagsNonCapturingGroup() {
        OracleEssentials.harnessFull("a(?i:b)c", " abc abC aBc aBC Abc AbC ABc ABC ");
    }

    @Test public void
    testAlternatives() {
        OracleEssentials.harnessFull("a|b",        " a b c ");
        OracleEssentials.harnessFull("a(?:b|bb)c", " ac abc abbc abbbc ");
    }

    @Test public void
    testIndependentGroup() {
        OracleEssentials.harnessFull("(?>a|b)",    " a b c ");
        OracleEssentials.harnessFull("a(?>b|bb)c", " ac abc abbc abbbc ");
    }

    // ======================================== CHARACTER CLASSES ========================================

    @Test public void testPredefinedCharacterClasses1() {                        OracleEssentials.harnessFull("\\w",     " abc äöü "); } // SUPPRESS CHECKSTYLE LineLength:3
    @Test public void testPredefinedCharacterClasses2() { if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?U)\\w", " abc äöü "); }
    @Test public void testPredefinedCharacterClasses3() {                        OracleEssentials.harnessFull("\\W",     " abc äöü "); }
    @Test public void testPredefinedCharacterClasses4() { if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?U)\\W", " abc äöü "); }

    @Test public void testPosixCharacterClasses1() {                        OracleEssentials.harnessFull("\\p{Lower}",     " abc äöü "); } // SUPPRESS CHECKSTYLE LineLength:3
    @Test public void testPosixCharacterClasses2() { if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?U)\\p{Lower}", " abc äöü "); }
    @Test public void testPosixCharacterClasses3() {                        OracleEssentials.harnessFull("\\P{Lower}",     " abc äöü "); }
    @Test public void testPosixCharacterClasses4() { if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?U)\\P{Lower}", " abc äöü "); }

    @Test public void
    testJavaCharacterClasses1() {
        OracleEssentials.harnessFull("\\p{javaLowerCase}", " a B c ä Ä ");
    }

    @Test public void
    testJavaCharacterClasses2() {
        OracleEssentials.harnessFull("\\P{javaLowerCase}", " a B c ä Ä ");
    }

    @Test public void
    testUnicodeCharacterClasses1() {

        // By "UNICODE script":
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("\\p{IsLatin}",       " a B c ä Ä ");
    }

    @Test public void
    testUnicodeCharacterClasses2() {

        // By "UNICODE block":
        OracleEssentials.harnessFull("\\p{InGreek}",       " \u03b1 ");
    }

    @Test public void
    testUnicodeCharacterClasses3() {
        OracleEssentials.harnessFull("\\p{InBasicLatin}",  " a B c ä Ä ");
    }

    @Test public void
    testUnicodeCharacterClasses4() {
        OracleEssentials.harnessFull("\\P{InBasicLatin}",  " a B c ä Ä ");
    }

    @Test public void
    testUnicodeCharacterClasses5() {

        // By "UNICODE category":
        OracleEssentials.harnessFull("\\p{Lu}",            " a B c ä Ä ");
    }

    @Test public void
    testUnicodeCharacterClasses6() {
        OracleEssentials.harnessFull("\\P{Lu}",            " a B c ä Ä ");
    }

    @Test public void
    testUnicodeCharacterClasses7() {
        OracleEssentials.harnessFull("\\p{Sc}",            " a $ ");
    }

    @Test public void
    testUnicodeCharacterClasses8() {
        OracleEssentials.harnessFull("\\P{Sc}",            " a $ ");
    }

    @Test public void
    testUnicodeCharacterClasses9() {

        // By "UNICODE property":
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("\\p{IsLowerCASE}",  " abc äöü ");
    }

    @Test public void
    testUnicodeCharacterClasses10() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("\\p{IsAlphabetic}", " abc äöü ");
    }

    // ======================================== END OF CHARACTER CLASSES ========================================

    @Test public void
    testCapturingGroups() {
        OracleEssentials.harnessFull("((a+)(b+))", " abbb aabb aaab ");
    }

    @Test public void
    testNamedCapturingGroups1() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?<xxx>a+)", " a aa aaa");
    }

    @Test public void
    testNamedCapturingGroups2() {

        if (PatternTest.JRE6) return;

        Matcher matcher = OracleEssentials.LFR.compile("(?<xxx>a+)").matcher(" a aa aaa");

        Assert.assertTrue(matcher.find());
        Assert.assertEquals("a", matcher.group("xxx"));

        Assert.assertTrue(matcher.find());
        Assert.assertEquals("aa", matcher.group("xxx"));

        Assert.assertTrue(matcher.find());
        Assert.assertEquals("aaa", matcher.group("xxx"));

        Assert.assertFalse(matcher.find());
    }

    @Test public void
    testCapturingGroupsBackreference() {

        // "\2" is an invalid backreference, which results in a match failure.
        PatternTest.PF.compile("(\\d\\d)\\2").matcher(" a aa aaa").replaceAll("x");
    }

    @Test public void
    testNamedCapturingGroupsBackreference1() {

        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?<first>\\w)\\k<first>", " a aa aaa");
    }

    @Test public void
    testNamedCapturingGroupsBackreference2() {

        // Backreference to inexistent named group.
        if (!PatternTest.JRE6) PatternTest.assertPatternSyntaxException("(?<first>\\w)\\k<bla>");
    }

    @Test public void
    testPositiveLookahead1() {
        OracleEssentials.harnessFull("a(?=b)",   " a aba abba a");
    }

    @Test public void
    testPositiveLookahead2() {
        OracleEssentials.harnessFull("a(?=(b))", " a aba abba a");
    }

    @Test public void
    testNegativeLookahead1() {
        OracleEssentials.harnessFull("a(?!b)",   " a aba abba a");
    }

    @Test public void
    testNegativeLookahead2() {
        OracleEssentials.harnessFull("a(?!(b))", " a aba abba a");
    }

    @Test public void
    testPositiveLookbehind1() {
        OracleEssentials.harnessFull("(?<=b)a",    " a aba abba a");
    }

    @Test public void
    testPositiveLookbehind2() {
        OracleEssentials.harnessFull("(?<=(b))a",  " a aba abba a");
    }

    @Test public void
    testPositiveLookbehind3() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?<=\\R )a", " \r\n a ");
    }

    @Test public void
    testPositiveLookbehind4() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?<=\\R )a", " \r a ");
    }

    @Test public void
    testPositiveLookbehind5() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?<=\\R )a", " \n a ");
    }

    @Test public void
    testPositiveLookbehind6() {
        OracleEssentials.harnessFull("(?<=^\t*)\t", "\t\t\tpublic static void main()");
    }

    @Test public void
    testPositiveLookbehind7() {
        PatternTest.PF.compile("(?<=^\\s*)    ").matcher("        public static void main()").replaceAll("\t");
    }

    @Test public void
    testNegativeLookbehind1() {
        OracleEssentials.harnessFull("(?<!b)a",   " a aba abba a");
    }

    @Test public void
    testNegativeLookbehind2() {
        OracleEssentials.harnessFull("(?<!(b))a", " a aba abba a");
    }

    @Test public void
    testNegativeLookbehind3() {
        OracleEssentials.harnessFull("(?<!(?:b))a", " a aba abba a");
    }

    @Test public void
    testNegativeLookbehind4() {
        OracleEssentials.harnessFull("(?<!b)a", " a aba abba a");
    }

    @Test public void
    testRegion1() {
        OracleEssentials.harnessFull("a", "__a__ a aba abba __a__", 0, 5, 17);
    }

    @Test public void
    testRegion2() {
        OracleEssentials.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17);
    }

    @Test public void
    testRegion3() {
        OracleEssentials.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, false, false);
    }

    @Test public void
    testRegion4() {
        OracleEssentials.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, false, true);
    }

    @Test public void
    testRegion5() {
        OracleEssentials.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, true,  false);
    }

    @Test public void
    testRegion6() {
        OracleEssentials.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, true,  true);
    }

    @Test public void
    testTransparentBounds1() {

        // The for the nineth, failed, match, jur returns "false" for "hitEnd()"!?
//        harness("\\b",     "__a__ a aba abba __a__", 0, 5, 17, true);
    }

    @Test public void
    testTransparentBounds2() {

        // Lookahead.
        OracleEssentials.harnessFull(" (?=_)",  "__a__ a aba abba __a__", 0, 5, 17, true);
    }

    @Test public void
    testTransparentBounds3() {
        OracleEssentials.harnessFull(" (?!_)",  "__a__ a aba abba __a__", 0, 5, 17, true);
    }

    @Test public void
    testTransparentBounds4() {

        // Lookbehind.
        OracleEssentials.harnessFull("(?<=_) ", "__a__ a aba abba __a__", 0, 5, 17, true);
    }

    @Test public void
    testTransparentBounds5() {
        OracleEssentials.harnessFull("(?<!_) ", "__a__ a aba abba __a__", 0, 5, 17, true);
    }

    @Test public void
    testAnchoringBounds1() {
        OracleEssentials.harnessFull("^",  "__a__ a aba abba __a__", 0, 5, 17, null, false);
    }

    @Test public void
    testAnchoringBounds2() {
        OracleEssentials.harnessFull("$",  "__a__ a aba abba __a__", 0, 5, 17, null, false);
    }

    @Test public void
    testUnixLines1() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("\\R",  "  \n  \r\n \u2028 ");
    }

    @Test public void
    testUnixLines2() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("\\R",  "  \n  \r\n \u2028 ", Pattern.UNIX_LINES);
    }

    @Test public void
    testUnixLines3() {
        OracleEssentials.harnessFull("^",    "  \n  \r\n \u2028 ");
    }

    @Test public void
    testUnixLines4() {
        OracleEssentials.harnessFull("^",    "  \n  \r\n \u2028 ", Pattern.UNIX_LINES);
    }

    @Test public void
    testQuantifiers1() {
        OracleEssentials.harnessFull("a?",  " aaa ");
//        OracleEssentials.harnessFull("a??", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        OracleEssentials.harnessFull("a?+", " aaa ");
    }

    @Test public void
    testQuantifiers2() {
        OracleEssentials.harnessFull("a*",  " aaa ");
//        OracleEssentials.harnessFull("a*?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        OracleEssentials.harnessFull("a*+", " aaa ");
    }

    @Test public void
    testQuantifiers3() {
        OracleEssentials.harnessFull("a+",  " aaa ");
        OracleEssentials.harnessFull("a+?", " aaa ");
        OracleEssentials.harnessFull("a++", " aaa ");
    }

    @Test public void
    testQuantifiers4() {
//        OracleEssentials.harnessFull("a{0}",  " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
//        OracleEssentials.harnessFull("a{0}?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
//        OracleEssentials.harnessFull("a{0}+", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
    }

    @Test public void
    testQuantifiers5() {
        OracleEssentials.harnessFull("a{1}",  " aaa ");
        OracleEssentials.harnessFull("a{1}?", " aaa ");
        OracleEssentials.harnessFull("a{1}+", " aaa ");
    }

    @Test public void
    testQuantifiers6() {
        OracleEssentials.harnessFull("a{2}",  " aaa ");
        OracleEssentials.harnessFull("a{2}?", " aaa ");
        OracleEssentials.harnessFull("a{2}+", " aaa ");
    }

    @Test public void
    testQuantifiers7() {
        OracleEssentials.harnessFull("a{0,}",  " aaa ");
//        OracleEssentials.harnessFull("a{0,}?", " aaa "); // JRE says !hitEnd after the 7th, failed, match!?
        OracleEssentials.harnessFull("a{0,}+", " aaa ");
    }

    @Test public void
    testLongLiteralString() {

        String infix = "ABCDEFGHIJKLMNOP";

        String regex = infix;

        PatternTest.assertSequenceToString("knuthMorrisPratt(\"ABCDEFGHIJKLMNOP\")", regex);

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 10; i++) {
            String subject = AssertionUtil.notNull(rsp.produce());
            Matcher m = PatternTest.PF.compile(regex, Pattern.DOTALL).matcher(subject);
            while (m.find());
        }
    }

    @Test public void
    testGreedyQuantifierFollowedByLongLiteralString() {

        String infix = "ABCDEFGHIJKLMNOP";

        String regex = ".*" + infix;

        PatternTest.assertSequenceToString((
            "greedyQuantifierOnCharacterClass(operand=anyCharButLineBreak, min=0, max=infinite)"
            + " . "
            + "knuthMorrisPratt(\"ABCDEFGHIJKLMNOP\")"
        ), regex);

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 10; i++) {
            String subject = AssertionUtil.notNull(rsp.produce());
            PatternTest.PF.compile(regex, Pattern.DOTALL).matcher(subject).matches();
        }
    }

    @Test public void
    testReluctantQuantifierFollowedByLongLiteralString() {

        final String infix = "ABCDEFGHIJKLMNOP";

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);

        PatternTest.assertSequenceToString("knuthMorrisPratt(\"ABCDEFGHIJKLMNOP\")", infix);

        Pattern p = PatternTest.PF.compile(".*?" + infix, Pattern.DOTALL);

        for (int i = 0; i < 10; i++) {
            String  subject = AssertionUtil.notNull(rsp.produce());
            Matcher matcher = p.matcher(subject);
            matcher.matches();
        }
    }

    static int    clef              = 0x1d120;
    static char   clefHighSurrogate = PatternTest.highSurrogateOf(PatternTest.clef);
    static char   clefLowSurrogate  = PatternTest.lowSurrogateOf(PatternTest.clef);
    static String clefUnicode       = "" + PatternTest.clefHighSurrogate + PatternTest.clefLowSurrogate;

    @Test public void
    testSurrogates1() {
        OracleEssentials.harnessFull(PatternTest.clefUnicode,       PatternTest.clefUnicode);
    }

    @Test public void
    testSurrogates2() {
        OracleEssentials.harnessFull(PatternTest.clefUnicode + "?", "");
    }

    @Test public void
    testSurrogates3() {
        OracleEssentials.harnessFull(PatternTest.clefUnicode + "?", "" + PatternTest.clefHighSurrogate);
    }

    @Test public void
    testSurrogates4() {
        OracleEssentials.harnessFull(PatternTest.clefUnicode + "?", "" + PatternTest.clefLowSurrogate);
    }

    @Test public void
    testSurrogates5() {
        OracleEssentials.harnessFull(
            PatternTest.clefUnicode + "?",
            PatternTest.clefUnicode
        );
    }

    @Test public void
    testSurrogates6() {
        OracleEssentials.harnessFull(
            PatternTest.clefUnicode + "?",
            "" + PatternTest.clefLowSurrogate + PatternTest.clefHighSurrogate // <= high/low surrogates reversed!
        );
    }

    @Test public void
    testPreviousMatchBoundary() {

        // From: http://stackoverflow.com/questions/2708833
        OracleEssentials.harnessFull(
            "(?<=\\G\\d{3})(?=\\d)" + "|" + "(?<=^-?\\d{1,3})(?=(?:\\d{3})+(?!\\d))",
            "-1234567890.1234567890"
        );
    }

    @Test public void
    testAtomicGroups1() {
        OracleEssentials.harnessFull("^a(bc|b)c$",   "abc");
    }

    @Test public void
    testAtomicGroups2() {
        OracleEssentials.harnessFull("^a(bc|b)c$",   "abcc");
    }

    @Test public void
    testAtomicGroups3() {
        OracleEssentials.harnessFull("^a(?>bc|b)c$", "abc");
    }

    @Test public void
    testAtomicGroups4() {
        OracleEssentials.harnessFull("^a(?>bc|b)c$", "abcc");
    }

    /**
     * @see <a href="http://stackoverflow.com/questions/17618812">Clarification about requireEnd Matcher's method</a>
     */
    @Test public void
    testRequireEnd1() {
        OracleEssentials.harnessFull("cat$", "I have a cat");
    }

    @Test public void
    testRequireEnd2() {
        OracleEssentials.harnessFull("cat$", "I have a catflap");
    }

    @Test public void
    testRequireEnd3() {
        OracleEssentials.harnessFull("cat",  "I have a cat");
    }

    @Test public void
    testRequireEnd4() {
        OracleEssentials.harnessFull("cat",  "I have a catflap");
    }

    @Test public void
    testRequireEnd5() {
        OracleEssentials.harnessFull("\\d+\\b|[><]=?", "1234");
    }

    @Test public void
    testRequireEnd6() {
        OracleEssentials.harnessFull("\\d+\\b|[><]=?", ">=");
    }

    @Test public void
    testRequireEnd7() {
        OracleEssentials.harnessFull("\\d+\\b|[><]=?", "<");
    }

    @Test public void
    testComments1() {
        OracleEssentials.harnessFull(" a# comment \nb ",    " ab a# comment \nb", Pattern.COMMENTS);
    }

    @Test public void
    testComments2() {
        OracleEssentials.harnessFull("(?x)  a  ",           " a ");
    }

    @Test public void
    testComments3() {
        OracleEssentials.harnessFull("(?x)  a  (?-x) b",    " ab ");
    }

    @Test public void
    testComments4() {
        OracleEssentials.harnessFull("(?x)  a  (?-x) b",    " a b ");
    }

    @Test public void
    testComments5() {
        OracleEssentials.harnessFull("(?x)  a#\n  (?-x) b", " ab ");
    }

    @Test public void
    testComments6() {
        OracleEssentials.harnessFull("(?x)  a#\n  (?-x) b", " a b ");
    }

    @Test public void
    testComments7() {
        OracleEssentials.harnessFull("(?x)  (a)", " a b ");
    }

    @Test public void
    testComments8() {
        OracleEssentials.harnessFull("(?x)  (?:a)", " a b ");
    }

    @Test public void
    testComments9() {
        OracleEssentials.harnessFull("(?x)  ( ?:a)", " a b ");
    }

    @Test public void
    testComments10() {
        OracleEssentials.harnessFull("(?x)  (?: a)", " a b ");
    }

    @Test public void
    testComments11() {
        OracleEssentials.harnessFull("(?x)  (? : a)", " a b ");
    }

    @Test public void
    testComments12() {
        OracleEssentials.harnessFull("(?x)  ( ? :a)", " a b ");
    }

    @Test public void
    testComments13() {
        OracleEssentials.harnessFull("(?x)  ( ?: a)", " a b ");
    }

    @Test public void
    testComments14() {
        OracleEssentials.harnessFull("(?x)  ( ? : a)", " a b ");
    }

    @Test public void
    testComments15() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?x)  (?<name>a)", " a b ");
    }

    @Test public void
    testComments16() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?x)  ( ?<name>a)", " a b ");
    }

    @Test public void
    testComments17() {
        if (!PatternTest.JRE6) PatternTest.assertPatternSyntaxException("(?x)  (? <name>a)");
    }

    @Test public void
    testComments18() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?x)  (?< name>a)", " a b ");
    }

    @Test public void
    testComments19() {
        if (!PatternTest.JRE6) PatternTest.assertPatternSyntaxException("(?x)  (? < name>a)");
    }

    @Test public void
    testComments20() {
        if (!PatternTest.JRE6) OracleEssentials.harnessFull("(?x)  ( ?< name>a)", " a b ");
    }

    @Test public void
    testComments21() {
        if (!PatternTest.JRE6) PatternTest.assertPatternSyntaxException("(?x)  ( ? < name>a)");
    }

    @Test public void
    testReplaceAll1() {
        Assert.assertEquals(" Xbc ",     PF.compile("a").matcher(" abc ").replaceAll("X"));
    }

    @Test public void
    testReplaceAll2() {
        Assert.assertEquals(" <<a>>bc ", PF.compile("(a)").matcher(" abc ").replaceAll("<<$1>>"));
    }

    @Test public void
    testReplaceAll3() {
        Assert.assertEquals(
            " <<a>>bc ",
            (JRE6 ? LFR : PF).compile("(?<grp>a)").matcher(" abc ").replaceAll("<<${grp}>>")
        );
    }

    @Test public void
    testReplaceAll4() {

        // "Replacement-with-expression" is only supported with LFR.
        Assert.assertEquals(" <<null>>bc ", LFR.compile("(a)").matcher(" abc ").replaceAll("<<${null}>>"));
    }

    @Test public void
    testReplaceAll5() {

        // "Replacement-with-expression" is only supported with LFR.
        Assert.assertEquals(
            " <<a>>bc ",
            LFR.compile("(a)").matcher(" abc ").replaceAll("${\"<<\" + m.group() + \">>\"}")
        );
    }

    @Test public void
    testReplaceAll6() {

        // "Replacement-with-expression" is only supported with LFR.
        Assert.assertEquals(" <<a>>bc ", LFR.compile("(a)").matcher(" abc ").replaceAll("<<${m.group()}>>"));
    }

    @Test public void
    testReplaceAll7() {
        Assert.assertEquals(
            " 7a1bc ",
            LFR.compile("(?<grp>a)").matcher(" abc ").replaceAll("${\"\" + 7 + grp + m.groupCount()}")
        );
    }

    @Test public void
    testAppendReplacementTail1() {

        // Verify that "appendReplacement()" without a preceding match throws an Exception.
        try {
            PatternTest.PF.compile("foo").matcher(" Hello foo and foo!").appendReplacement(new StringBuffer(), "bar");
            Assert.fail();
        } catch (IllegalStateException ise) {
            ;
        }
    }

    @Test public void
    testAppendReplacementTail2() {

        // Verify that "appendReplacement()" and "appendTail()" work.
        Matcher m = PatternTest.PF.compile("foo").matcher(" Hello foo and foo!");

        Assert.assertTrue(m.find());
        StringBuffer sb = new StringBuffer("==");
        m.appendReplacement(sb, "bar");
        Assert.assertEquals("== Hello bar", sb.toString());

        m.appendTail(sb);
        Assert.assertEquals("== Hello bar and foo!", sb.toString());
    }

    @Test public void
    testCharacterClassOptimizations1() {
        PatternTest.assertSequenceToString("'A'",                                            "[A]");
    }

    @Test public void
    testCharacterClassOptimizations2() {
        PatternTest.assertSequenceToString("oneOfTwo('A', 'B')",                             "[AB]");
    }

    @Test public void
    testCharacterClassOptimizations3() {
        PatternTest.assertSequenceToString("oneOfTwo('A', 'K')",                             "[AK]");
    }

    @Test public void
    testCharacterClassOptimizations4() {
        PatternTest.assertSequenceToString("bitSet('A', 'C', 'E', 'G', 'I', 'K')",           "[ACEGIK]");
    }

    @Test public void
    testCharacterClassOptimizations5() {
        PatternTest.assertSequenceToString("range('A' - 'E')",                               "[A-E]");
    }

    @Test public void
    testCharacterClassOptimizations6() {
        PatternTest.assertSequenceToString("bitSet('D', 'E', 'F', 'G', 'H', 'I', 'J', 'K')", "[A-K&&D-Z]");
    }

    @Test public void
    testCharacterClassOptimizations7() {
        PatternTest.assertSequenceToString(PatternTest.jurpc("set\\('.'(?:, '.'){63}\\)"),   "[A-Za-z0-9_\u0400]");
    }

    @Test public void
    testQuantifierOptimizations1() {
        PatternTest.assertSequenceToString("'A'", "A");
    }

    @Test public void
    testQuantifierOptimizations2() {
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierOnCharacterClass(operand=anyCharButLineBreak, min=0, max=infinite) . 'B'",
            "A.*B"
        );
    }

    @Test public void
    testQuantifierOptimizations3() {
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierOnCharacterClass(operand=anyCharButLineBreak, min=0, max=infinite) . naive(\"BC\")",
            "A.*BC"
        );
    }

    @Test public void
    testQuantifierOptimizations4() {
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=naive(\"BC\"))",
            "A.*BC",
            Pattern.DOTALL
        );
    }

    @Test public void
    testQuantifierOptimizations5() {
        PatternTest.assertSequenceToString(
            "'A' . reluctantQuantifierOnAnyChar(min=0, max=infinite, ls=naive(\"BC\"))",
            "A.*?BC",
            Pattern.DOTALL
        );
    }

    @Test public void
    testQuantifierOptimizations6() {
        PatternTest.assertSequenceToString(
            "'A' . possessiveQuantifierOnAnyChar(min=0, max=infinite) . naive(\"BC\")",
            "A.*+BC",
            Pattern.DOTALL
        );
    }

    @Test public void
    testQuantifierOptimizations7() {

        // Naive string search, because the string literal is only 14 characters long.
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=naive(\"abcdefghijklmno\"))",
            "A.*abcdefghijklmno",
            Pattern.DOTALL
        );
    }

    @Test public void
    testQuantifierOptimizations8() {

        // Knuth-Morris-Pratt string search, because the string literal is 15 characters long.
        PatternTest.assertSequenceToString(
            "'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=knuthMorrisPratt(\"abcdefghijklmnop\"))",
            "A.*abcdefghijklmnop",
            Pattern.DOTALL
        );
    }

    @Test public void
    testQuantifierOptimizations9() {
        PatternTest.assertSequenceToString("naive(\"aaa\")", "a{3}");
    }

    @Test public void
    testQuantifierOptimizations10() {
        PatternTest.assertSequenceToString(
            "naive(\"aaa\") . greedyQuantifierOnCharacterClass(operand='a', min=0, max=2)",
            "a{3,5}"
        );
    }

    @Test public void
    testQuantifierOptimizations11() {
        PatternTest.assertSequenceToString(
            "naive(\"aaa\") . reluctantQuantifier(operand='a', min=0, max=2)",
            "a{3,5}?"
        );
    }

    @Test public void
    testQuantifierOptimizations12() {
        PatternTest.assertSequenceToString(
            "naive(\"aaa\") . possessiveQuantifier(operand='a', min=0, max=2)",
            "a{3,5}+"
        );
    }

    @Test public void
    testQuantifierOptimizations13() {
        PatternTest.assertSequenceToString(
            "naive(\"abcabcabc\") . greedyQuantifier(operand=naive(\"abc\"), min=0, max=2)",
            "(?:abc){3,5}"
        );
    }

    @Test public void
    testQuantifierOptimizations14() {
        PatternTest.assertSequenceToString(
            "naive(\"abcabcabc\") . reluctantQuantifier(operand=naive(\"abc\"), min=0, max=2)",
            "(?:abc){3,5}?"
        );
    }

    @Test public void
    testQuantifierOptimizations15() {
        PatternTest.assertSequenceToString(
            "naive(\"abcabcabc\") . possessiveQuantifier(operand=naive(\"abc\"), min=0, max=2)",
            "(?:abc){3,5}+"
        );
    }

    @Test public void
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

    public static void
    assertPatternSyntaxException(String regex) {
        PatternTest.assertPatternSyntaxException(regex, 0);
    }

    public static void
    assertPatternSyntaxException(String regex, int flags) {

        try {
            PatternTest.PF.compile(regex, flags);
            Assert.fail();
        } catch (PatternSyntaxException pse) {
            return;
        }
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
