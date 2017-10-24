/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @summary tests RegExp framework
 * @author Mike McCloskey
 * @bug 4481568 4482696 4495089 4504687 4527731 4599621 4631553 4619345
 * 4630911 4672616 4711773 4727935 4750573 4792284 4803197 4757029 4808962
 * 4872664 4803179 4892980 4900747 4945394 4938995 4979006 4994840 4997476
 * 5013885 5003322 4988891 5098443 5110268 6173522 4829857 5027748 6376940
 * 6358731 6178785 6284152 6231989 6497148 6486934 6233084 6504326 6635133
 * 6350801 6676425 6878475 6919132 6931676 6948903 6990617 7014645 7039066
 * 7067045 7014640 7189363 8007395 8013252 8013254 8012646 8023647 6559590
 * 8027645 6854417 8169056
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.MatchResult;
import java.util.regex.PatternSyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.PrettyPrinter;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;
import test.FunctionalityEquivalencePatternFactory;

/**
 * This is a test class created to check the operation of
 * the Pattern and Matcher classes.
 */
public class RegExTest {

    private static final PatternFactory PF = de.unkrig.lfr.core.PatternFactory.INSTANCE;
//    private static final PatternFactory PF = de.unkrig.ref4j.jur.PatternFactory.INSTANCE;
//    private static final PatternFactory PF = new FunctionalityEquivalencePatternFactory(
//        de.unkrig.ref4j.jur.PatternFactory.INSTANCE,
//        de.unkrig.lfr.core.PatternFactory.INSTANCE
//    );
//    private static final PatternFactory PF = new PerformanceMeasurementPatternFactory(
//        de.unkrig.ref4j.jur.PatternFactory.INSTANCE,
//        de.unkrig.lfr.core.PatternFactory.INSTANCE
//    );

    private static final boolean
    LFR = RegExTest.PF.getClass().getName().indexOf(".lfr.") != -1 || RegExTest.PF.getClass().getSimpleName().equals("FunctionalityEquivalencePatternFactory");

    private static Random generator = new Random(222);

    @Test public void
    processFileSupplementaryTestCasesTxt() throws Exception {
        RegExTest.processFile("SupplementaryTestCases.txt");
    }

    @Test public void
    processFileBMPTestCasesTxt() throws Exception {
        RegExTest.processFile("BMPTestCases.txt");
    }

    @Test public void
    processFileTestCasesTxt() throws Exception {
        RegExTest.processFile("TestCases.txt");
    }

    // Utility functions

    private static String getRandomAlphaString(int length) {
        StringBuffer buf = new StringBuffer(length);
        for (int i=0; i<length; i++) {
            char randChar = (char)(97 + RegExTest.generator.nextInt(26));
            buf.append(randChar);
        }
        return buf.toString();
    }

    private static void check(Matcher m, String expected) {
        m.find();
        Assert.assertEquals(expected, m.group());
    }

    private static void check(Matcher m, String result, boolean expected) {
        m.find();
        if (expected) {
            Assert.assertEquals(result, m.group());
        } else {
            Assert.assertNotEquals(result, m.group());
        }
    }

    /**
     * Verifies that {@code p.matcher(s).find()} returns <var>expected</var>.
     */
    private static void checkFind(Pattern p, String s, boolean expected) {
        Assert.assertEquals("\"" + p + "\" \"" + s + "\"", expected, p.matcher(s).find());
    }

    private static void check(String p, String s, boolean expected) {
        Matcher matcher = RegExTest.PF.compile(p).matcher(s);
        Assert.assertFalse(matcher.find() != expected);
    }

    private static void check(String p, char c, boolean expected) {
        String propertyPattern = expected ? "\\p" + p : "\\P" + p;
        Pattern pattern = RegExTest.PF.compile(propertyPattern);
        char[] ca = new char[1]; ca[0] = c;
        Matcher matcher = pattern.matcher(new String(ca));
        Assert.assertTrue(matcher.find());
    }

    private static void check(String p, int codePoint, boolean expected) {
        String propertyPattern = expected ? "\\p" + p : "\\P" + p;
        Pattern pattern = RegExTest.PF.compile(propertyPattern);
        char[] ca = Character.toChars(codePoint);
        Matcher matcher = pattern.matcher(new String(ca));
        Assert.assertTrue(matcher.find());
    }

    private static void check(String p, int flag, String input, String s,
                              boolean expected)
    {
        Pattern pattern = RegExTest.PF.compile(p, flag);
        Matcher matcher = pattern.matcher(input);
        if (expected)
            RegExTest.check(matcher, s, expected);
        else
            RegExTest.checkFind(pattern, input, false);
    }

    /**
     * Converts ASCII alphabet characters [A-Za-z] in the given 's' to
     * supplementary characters. This method does NOT fully take care
     * of the regex syntax.
     */
    private static String toSupplementaries(String s) {
        int length = s.length();
        StringBuffer sb = new StringBuffer(length * 2);

        for (int i = 0; i < length; ) {
            char c = s.charAt(i++);
            if (c == '\\') {
                sb.append(c);
                if (i < length) {
                    c = s.charAt(i++);
                    sb.append(c);
                    if (c == 'u') {
                        // assume no syntax error
                        sb.append(s.charAt(i++));
                        sb.append(s.charAt(i++));
                        sb.append(s.charAt(i++));
                        sb.append(s.charAt(i++));
                    }
                }
            } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                sb.append('\ud800').append((char)('\udc00'+c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Regular expression tests

    // This is for bug 6178785
    // Test if an expected NPE gets thrown when passing in a null argument
    private static boolean check(Runnable test) {
        try {
            test.run();
            Assert.fail();
            return false;
        } catch (NullPointerException npe) {
            return true;
        }
    }

    @Test public void
    nullArgumentTest() {

        // Null Argument
        RegExTest.check(new Runnable() { public void run() { RegExTest.PF.compile(null);                } });
        RegExTest.check(new Runnable() { public void run() { RegExTest.PF.matches(null, null);          } });
        RegExTest.check(new Runnable() { public void run() { RegExTest.PF.matches("xyz", null);         } });
        RegExTest.check(new Runnable() { public void run() { RegExTest.PF.quote(null);                  } });
        RegExTest.check(new Runnable() { public void run() { RegExTest.PF.compile("xyz").split(null);   } });
        RegExTest.check(new Runnable() { public void run() { RegExTest.PF.compile("xyz").matcher(null); } });

        final Matcher m = RegExTest.PF.compile("xyz").matcher("xyz");
        m.matches();
        RegExTest.check(new Runnable() { public void run() { m.appendTail(null);                  } });
        RegExTest.check(new Runnable() { public void run() { m.replaceAll(null);                  } });
        RegExTest.check(new Runnable() { public void run() { m.replaceFirst(null);                } });
        RegExTest.check(new Runnable() { public void run() { m.appendReplacement(null, null);     } });
        RegExTest.check(new Runnable() { public void run() { m.reset(null);                       } });
        RegExTest.check(new Runnable() { public void run() { RegExTest.PF.quoteReplacement(null); } });
        //check(new Runnable() { public void run() { m.usePattern(null);}});
    }

    // This is for bug6635133
    // Test if surrogate pair in Unicode escapes can be handled correctly.
    @Test public void
    surrogatesInClassTest() throws Exception {

        // Surrogate pair in Unicode escape
        Pattern pattern = RegExTest.PF.compile("[\\ud834\\udd21-\\ud834\\udd24]");
        Matcher matcher = pattern.matcher("\ud834\udd22");
        Assert.assertTrue(matcher.find());
    }

    // This is for bug6990617
    // Test if Pattern.RemoveQEQuoting works correctly if the octal unicode
    // char encoding is only 2 or 3 digits instead of 4 and the first quoted
    // char is an octal digit.
    @Test public void
    removeQEQuotingTest() throws Exception {

        // Remove Q/E Quoting
        Pattern pattern = RegExTest.PF.compile("\\011\\Q1sometext\\E\\011\\Q2sometext\\E");
        Matcher matcher = pattern.matcher("\t1sometext\t2sometext");
        Assert.assertTrue(matcher.find());
    }

    // This is for bug 4988891
    // Test toMatchResult to see that it is a copy of the Matcher
    // that is not affected by subsequent operations on the original
    @Test public void
    toMatchResultTest() throws Exception {

        // toMatchResult is a copy
        Pattern pattern = RegExTest.PF.compile("squid");
        Matcher matcher = pattern.matcher("agiantsquidofdestinyasmallsquidoffate");

        Assert.assertTrue(matcher.find());
        MatchResult mr = matcher.toMatchResult();
        Assert.assertNotSame(matcher, mr);
        Assert.assertEquals(mr.start(), matcher.start());

        Assert.assertTrue(matcher.find());
        int matcherStart2 = matcher.start();
        int resultStart2 = mr.start();
        Assert.assertNotEquals(matcherStart2, resultStart2);
        Assert.assertEquals(mr.start(), resultStart2);
        MatchResult mr2 = matcher.toMatchResult();
        Assert.assertNotEquals(mr, mr2);
        Assert.assertEquals(mr2.start(), matcherStart2);

        Assert.assertFalse(matcher.find());
    }

    // This is for bug 5013885
    // Must test a slice to see if it reports hitEnd correctly
    @Test public void
    hitEndTest1() throws Exception {

        // Basic test of Slice node
        Pattern p = RegExTest.PF.compile("^squidattack");
        Matcher m = p.matcher("squack");

        Assert.assertFalse(m.find());
        Assert.assertFalse(m.hitEnd());
    }

    @Test public void
    hitEndTest2() throws Exception {

        Pattern p = RegExTest.PF.compile("^squidattack");
        Matcher m = p.matcher("squid");

        Assert.assertFalse(m.find());
        Assert.assertTrue(m.hitEnd());
    }

    @Test public void
    hitEndTest3() throws Exception {

        // Test Slice, SliceA and SliceU nodes
        for (int flags : new int[] { 0, Pattern.CASE_INSENSITIVE, Pattern.UNICODE_CASE }) {

            Matcher m = RegExTest.PF.compile("^abc", flags).matcher("ad");

            Assert.assertFalse("flags=" + flags, m.find());
            Assert.assertFalse("flags=" + flags, m.hitEnd());
        }
    }

    @Test public void
    hitEndTest4() throws Exception {

        // Test Slice, SliceA and SliceU nodes
        for (int flags : new int[] { 0, Pattern.CASE_INSENSITIVE, Pattern.UNICODE_CASE }) {

            Matcher m = RegExTest.PF.compile("^abc", flags).matcher("ab");

            Assert.assertFalse("flags=" + flags, m.find());
            Assert.assertTrue("flags=" + flags, m.hitEnd());
        }
    }

    @Test public void
    hitEndTest5() throws Exception {

        // Test Boyer-Moore node
        Pattern p = RegExTest.PF.compile("catattack");
        Matcher m = p.matcher("attack");
        m.find();
        Assert.assertTrue(m.hitEnd());
    }

    @Test public void
    hitEndTest6() throws Exception {

        // hitEnd from a Slice
        Pattern p = RegExTest.PF.compile("catattack");
        Matcher m = p.matcher("attackattackattackcatatta");
        m.find();
        Assert.assertTrue(m.hitEnd());
    }

    // This is for bug 4997476
    // It is weird code submitted by customer demonstrating a regression
    @Test public void
    wordSearchTest() throws Exception {

        // Customer word search
        String testString = new String("word1 word2 word3");
        Pattern p = RegExTest.PF.compile("\\b");
        Matcher m = p.matcher(testString);
        int position = 0;

        int count = 1;
        try {
            for (; m.find(position); count++) {
            	int start = m.start();
                if (start == testString.length()) break;
                if (m.find(start + 1)) {
                    position = m.start();
                } else {
                    position = testString.length();
                }
                if (testString.substring(start, position).equals(" ")) continue;
                Assert.assertTrue(testString.substring(start, position-1).startsWith("word"));
            }
        } catch (AssertionError ae) {
            throw ExceptionUtil.wrap("match #" + count, ae);
        }
    }

    // This is for bug 4994840
    @Test public void
    caretAtEndTest() throws Exception {

        // Caret at end

        // Problem only occurs with multiline patterns
        // containing a beginning-of-line caret "^" followed
        // by an expression that also matches the empty string.
        Pattern pattern = RegExTest.PF.compile("^x?", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher("\r");
        matcher.find();
        matcher.find();
    }

    // This test is for 4979006
    // Check to see if word boundary construct properly handles unicode
    // non spacing marks
    @Test public void
    unicodeWordBoundsTest() throws Exception {

        // Unicode word boundary
        String spaces   = "  ";
        String wordChar = "a";
        String nsm      = "\u030a";

        assert (Character.getType('\u030a') == Character.NON_SPACING_MARK);

        Pattern pattern = RegExTest.PF.compile("\\b");
        Matcher matcher = pattern.matcher("");

        // S=other B=word character N=non spacing mark .=word boundary

        // SS.BB.SS
        RegExTest.twoFindIndexes(spaces + wordChar + wordChar + spaces, matcher, 2, 4);

        // SS.BBN.SS
        RegExTest.twoFindIndexes(spaces + wordChar + wordChar + nsm + spaces, matcher, 2, 5);

        // SS.BN.SS
        RegExTest.twoFindIndexes(spaces + wordChar + nsm + spaces, matcher, 2, 4);

        // SS.BNN.SS
        RegExTest.twoFindIndexes(spaces + wordChar + nsm + nsm + spaces, matcher, 2, 5);

        // SSN.BB.SS
        RegExTest.twoFindIndexes(spaces + nsm + wordChar + wordChar + spaces, matcher, 3, 5);

        // SS.BNB.SS
        RegExTest.twoFindIndexes(spaces + wordChar + nsm + wordChar + spaces, matcher, 2, 5);

        // SSNNSS
        matcher.reset(spaces + nsm + nsm + spaces);
        Assert.assertFalse(matcher.find());

        // SSN.BBN.SS
        RegExTest.twoFindIndexes(spaces + nsm + wordChar + wordChar + nsm + spaces, matcher, 3, 6);
    }

    private static void
    twoFindIndexes(String input, Matcher matcher, int a, int b) throws Exception {
        matcher.reset(input);

        Assert.assertTrue(matcher.find());
        Assert.assertEquals(a, matcher.start());

        Assert.assertTrue(matcher.find());
        Assert.assertEquals(b, matcher.start());

        Assert.assertFalse(matcher.find());
    }

    // This test is for 6284152
    static void check(String regex, String input, String[] expected) {
        List<String> result = new ArrayList<>();
        Pattern p = RegExTest.PF.compile(regex);
        Matcher m = p.matcher(input);

        int count = 1;
        try {
            while (m.find()) {
                result.add(m.group());
                count++;
            }
        } catch (AssertionError ae) {
            throw ExceptionUtil.wrap("match #" + count, ae);
        }
        Assert.assertEquals(result, Arrays.asList(expected));
    }

    @Test public void
    lookbehindTest1() throws Exception {

        // Lookbehind
        //Positive
        RegExTest.check("(?<=%.{0,5})foo\\d",
              "%foo1\n%bar foo2\n%bar  foo3\n%blahblah foo4\nfoo5",
              new String[]{"foo1", "foo2", "foo3"});
    }

    @Test public void
    lookbehindTest2() throws Exception {

        //boundary at end of the lookbehind sub-regex should work consistently
        //with the boundary just after the lookbehind sub-regex
        System.setProperty("FIX_REQUIRE_END", "tf tf");
        System.setProperty("FIX_HIT_END",     "tf tf");
        RegExTest.check("(?<=.*\\b)foo", "abcd foo", new String[] { "foo" });
    }

    @Test public void
    lookbehindTest3() throws Exception {

        // JUR regex say "hitEnd=true", but how should more chars change the match result?!
        System.setProperty("FIX_HIT_END", "tf tf");
        RegExTest.check("(?<=.*)\\bfoo", "abcd foo", new String[] { "foo" });
    }

    @Test public void
    lookbehindTest4() throws Exception {
        RegExTest.check("(?<!abc )\\bfoo", "abc foo", new String[0]);
    }

    @Test public void
    lookbehindTest5() throws Exception {
        RegExTest.check("(?<!abc \\b)foo", "abc foo", new String[0]);
    }

    @Test public void
    lookbehindTest6() throws Exception {

        //Negative
        RegExTest.check("(?<!%.{0,5})foo\\d",
              "%foo1\n%bar foo2\n%bar  foo3\n%blahblah foo4\nfoo5",
              new String[] {"foo4", "foo5"});
    }

    @Test public void
    lookbehindTest7() throws Exception {

        //Positive greedy
        RegExTest.check("(?<=%b{1,4})foo", "%bbbbfoo", new String[] {"foo"});
    }

    @Test public void
    lookbehindTest8() throws Exception {

        //Positive reluctant
        RegExTest.check("(?<=%b{1,4}?)foo", "%bbbbfoo", new String[] {"foo"});
    }

    @Test public void
    lookbehindTest9() throws Exception {

        //supplementary
        RegExTest.check("(?<=%b{1,4})fo\ud800\udc00o", "%bbbbfo\ud800\udc00o", new String[] { "fo\ud800\udc00o" });
    }

    @Test public void
    lookbehindTest10() throws Exception {
        RegExTest.check("(?<=%b{1,4}?)fo\ud800\udc00o", "%bbbbfo\ud800\udc00o", new String[] { "fo\ud800\udc00o" });
    }

    @Test public void
    lookbehindTest11() throws Exception {
        RegExTest.check("(?<!%b{1,4})fo\ud800\udc00o", "%afo\ud800\udc00o", new String[] { "fo\ud800\udc00o" });
    }

    @Test public void
    lookbehindTest12() throws Exception {
        RegExTest.check("(?<!%b{1,4}?)fo\ud800\udc00o", "%afo\ud800\udc00o", new String[] { "fo\ud800\udc00o" });
    }

    // This test is for 4938995
    // Check to see if weak region boundaries are transparent to
    // lookahead and lookbehind constructs
    @Test public void
    boundsTest1() throws Exception {
        String fullMessage = "catdogcat";
        Pattern pattern = RegExTest.PF.compile("(?<=cat)dog(?=cat)");
        Matcher matcher = pattern.matcher("catdogca");
        matcher.useTransparentBounds(true);
        Assert.assertFalse(matcher.find());
        matcher.reset("atdogcat");
        Assert.assertFalse(matcher.find());
        matcher.reset(fullMessage);
        Assert.assertTrue(matcher.find());
        matcher.reset(fullMessage);
        matcher.region(0,9);
        Assert.assertTrue(matcher.find());
        matcher.reset(fullMessage);
        matcher.region(0,6);
        Assert.assertTrue(matcher.find());
        matcher.reset(fullMessage);
        matcher.region(3,6);
        Assert.assertTrue(matcher.find());
        matcher.useTransparentBounds(false);
        Assert.assertFalse(matcher.find());
    }

    @Test public void
    boundsTest2() throws Exception {

        // Region bounds transparency

        // Negative lookahead/lookbehind
        Pattern pattern = RegExTest.PF.compile("(?<!cat)dog(?!cat)");
        Matcher matcher = pattern.matcher("dogcat");
        matcher.useTransparentBounds(true);
        matcher.region(0, 3);

        Assert.assertFalse(matcher.find());

        matcher.reset("catdog");
        matcher.region(3,6);
        Assert.assertFalse(matcher.find());

        matcher.useTransparentBounds(false);
        matcher.reset("dogcat");
        matcher.region(0, 3);
        Assert.assertTrue(matcher.find());

        matcher.reset("catdog");
        matcher.region(3, 6);
        Assert.assertTrue(matcher.find());
    }

    // This test is for 4945394
    @Test public void
    findFromTest() throws Exception {

        // Check for alternating find

        String  subject = "This is 40 $0 message.";
        Pattern pattern = RegExTest.PF.compile("\\$0");
        Matcher matcher = pattern.matcher(subject);

        Assert.assertTrue(matcher.find());
        Assert.assertEquals(11, matcher.start());
        Assert.assertEquals(13, matcher.end());
        Assert.assertEquals("$0", matcher.group());

        Assert.assertFalse(matcher.find());
    }

    // This test is for 4872664 and 4892980
    @Test public void
    negatedCharClassTest() throws Exception {

        // Negated Character Class

        Pattern pattern = RegExTest.PF.compile("[^>]");
        Matcher matcher = pattern.matcher("\u203A");
        Assert.assertTrue(matcher.matches());
        pattern = RegExTest.PF.compile("[^fr]");
        matcher = pattern.matcher("a");
        Assert.assertTrue(matcher.find());
        matcher.reset("\u203A");
        Assert.assertTrue(matcher.find());
        String s = "for";
        String result[] = s.split("[^fr]");
        Assert.assertEquals("f", result[0]);
        Assert.assertEquals("r", result[1]);
        s = "f\u203Ar";
        result = s.split("[^fr]");
        Assert.assertEquals("f", result[0]);
        Assert.assertEquals("r", result[1]);

        // Test adding to bits, subtracting a node, then adding to bits again
        pattern = RegExTest.PF.compile("[^f\u203Ar]");
        matcher = pattern.matcher("a");
        Assert.assertTrue(matcher.find());
        matcher.reset("f");
        Assert.assertFalse(matcher.find());
        matcher.reset("\u203A");
        Assert.assertFalse(matcher.find());
        matcher.reset("r");
        Assert.assertFalse(matcher.find());
        matcher.reset("\u203B");
        Assert.assertTrue(matcher.find());

        // Test subtracting a node, adding to bits, subtracting again
        pattern = RegExTest.PF.compile("[^\u203Ar\u203B]");
        matcher = pattern.matcher("a");
        Assert.assertTrue(matcher.find());
        matcher.reset("\u203A");
        Assert.assertFalse(matcher.find());
        matcher.reset("r");
        Assert.assertFalse(matcher.find());
        matcher.reset("\u203B");
        Assert.assertFalse(matcher.find());
        matcher.reset("\u203C");
        Assert.assertTrue(matcher.find());
    }

    // This test is for 4628291
    @Test public void
    toStringTest() throws Exception {

        // toString
        Pattern pattern = RegExTest.PF.compile("b+");
        Assert.assertEquals("b+", pattern.toString());
    }

    // This test is for 4808962
    @Test public void
    literalPatternTest1() throws Exception {
        Pattern pattern = RegExTest.PF.compile("abc\\t$^", Pattern.LITERAL);
        RegExTest.checkFind(pattern, "abc\\t$^", true);
    }

    @Test public void
    literalPatternTest2() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.PF.quote("abc\\t$^"));
        RegExTest.checkFind(pattern, "abc\\t$^", true);
    }

    @Test public void
    literalPatternTest3() throws Exception {
        Pattern pattern = RegExTest.PF.compile("\\Qa^$bcabc\\E", Pattern.LITERAL);
        RegExTest.checkFind(pattern, "\\Qa^$bcabc\\E", true);
        RegExTest.checkFind(pattern, "a^$bcabc", false);
    }

    @Test public void
    literalPatternTest4() throws Exception {
        Pattern pattern = RegExTest.PF.compile("\\\\Q\\\\E");
        RegExTest.checkFind(pattern, "\\Q\\E", true);
    }

    @Test public void
    literalPatternTest5() throws Exception {
        Pattern pattern = RegExTest.PF.compile("\\Qabc\\Eefg\\\\Q\\\\Ehij");
        RegExTest.checkFind(pattern, "abcefg\\Q\\Ehij", true);
    }

    @Test public void
    literalPatternTest6() throws Exception {
        Pattern pattern = RegExTest.PF.compile("\\\\\\Q\\\\E");
        RegExTest.checkFind(pattern, "\\\\\\\\", true);
    }

    @Test public void
    literalPatternTest7() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.PF.quote("\\Qa^$bcabc\\E"));
        RegExTest.checkFind(pattern, "\\Qa^$bcabc\\E", true);
        RegExTest.checkFind(pattern, "a^$bcabc", false);
    }

    @Test public void
    literalPatternTest8() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.PF.quote("\\Qabc\\Edef"));
        RegExTest.checkFind(pattern, "\\Qabc\\Edef", true);
        RegExTest.checkFind(pattern, "abcdef", false);
    }

    @Test public void
    literalPatternTest9() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.PF.quote("abc\\Edef"));
        RegExTest.checkFind(pattern, "abc\\Edef", true);
        RegExTest.checkFind(pattern, "abcdef", false);
    }

    @Test public void
    literalPatternTest10() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.PF.quote("\\E"));
        RegExTest.checkFind(pattern, "\\E", true);
    }

    @Test public void
    literalPatternTest11() throws Exception {
        Pattern pattern = RegExTest.PF.compile("((((abc.+?:)", Pattern.LITERAL);
        RegExTest.checkFind(pattern, "((((abc.+?:)", true);
    }

    @Test public void
    literalPatternTest12() throws Exception {
        Pattern pattern = RegExTest.PF.compile("^cat$", Pattern.LITERAL | Pattern.MULTILINE);
        RegExTest.checkFind(pattern, "abc^cat$def", true);
        RegExTest.checkFind(pattern, "cat", false);
    }

    @Test public void
    literalPatternTest13() throws Exception {
        Pattern pattern = RegExTest.PF.compile("abcdef", Pattern.LITERAL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        RegExTest.checkFind(pattern, "ABCDEF", true);
        RegExTest.checkFind(pattern, "AbCdEf", true);
    }

    @Test public void
    literalPatternTest14() throws Exception {
        Pattern pattern = RegExTest.PF.compile("a...b", Pattern.LITERAL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        RegExTest.checkFind(pattern, "A...b", true);
        RegExTest.checkFind(pattern, "Axxxb", false);
    }

    // This one is totally broken.
//    @Test public void
//    literalPatternTest15() throws Exception {
//
//        if (RegExTest.LFR) return; // LFR does not support CANON_EQ.
//
//        Pattern pattern = RegExTest.PF.compile(
//            "testa\u030a",
//            Pattern.LITERAL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.CANON_EQ
//        );
//        RegExTest.check(pattern, "testa\u030a", false);
//        RegExTest.check(pattern, "test\u00e5", false);
//    }

    @Test public void
    literalPatternTest16() throws Exception {

        // Supplementary character test
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("abc\\t$^"), Pattern.LITERAL);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abc\\t$^"), true);
    }

    @Test public void
    literalPatternTest17() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.PF.quote(RegExTest.toSupplementaries("abc\\t$^")));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abc\\t$^"), true);
    }

    @Test public void
    literalPatternTest18() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("\\Qa^$bcabc\\E"), Pattern.LITERAL);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("\\Qa^$bcabc\\E"), true);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("a^$bcabc"), false);
    }

    @Test public void
    literalPatternTest19() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.PF.quote(RegExTest.toSupplementaries("\\Qa^$bcabc\\E")));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("\\Qa^$bcabc\\E"), true);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("a^$bcabc"), false);
    }

    @Test public void
    literalPatternTest20() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.PF.quote(RegExTest.toSupplementaries("\\Qabc\\Edef")));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("\\Qabc\\Edef"), true);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abcdef"), false);
    }

    @Test public void
    literalPatternTest21() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.PF.quote(RegExTest.toSupplementaries("abc\\Edef")));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abc\\Edef"), true);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abcdef"), false);
    }

    @Test public void
    literalPatternTest22() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("((((abc.+?:)"), Pattern.LITERAL);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("((((abc.+?:)"), true);
    }

    @Test public void
    literalPatternTest23() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("^cat$"), Pattern.LITERAL | Pattern.MULTILINE);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abc^cat$def"), true);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("cat"), false);
    }

    @Test public void
    literalPatternTest24() throws Exception {

        // note: this is case-sensitive.
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("a...b"), Pattern.LITERAL | Pattern.MULTILINE | Pattern.DOTALL);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("a...b"), true);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("axxxb"), false);
    }

    // This one is totally broken.
//    @Test public void
//    literalPatternTest25() throws Exception {
//
//        if (RegExTest.LFR) return; // LFR does not support CANON_EQ.
//
//        String t = RegExTest.toSupplementaries("test");
//        Pattern pattern = RegExTest.PF.compile(
//            t + "a\u030a",
//            Pattern.LITERAL | Pattern.MULTILINE | Pattern.DOTALL | Pattern.CANON_EQ
//        );
//        RegExTest.check(pattern, t + "a\u030a", false);
//        RegExTest.check(pattern, t + "\u00e5", false);
//    }

    // This test is for 4803179
    // This test is also for 4808962, replacement parts
    @Test public void
    literalReplacementTest() throws Exception {

        // Literal replacement

        int flags = Pattern.LITERAL;

        Pattern pattern = RegExTest.PF.compile("abc", flags);
        Matcher matcher = pattern.matcher("zzzabczzz");
        String replaceTest = "$0";
        String result = matcher.replaceAll(replaceTest);
        Assert.assertEquals("zzzabczzz", result);

        matcher.reset();
        String literalReplacement = RegExTest.PF.quoteReplacement(replaceTest);
        result = matcher.replaceAll(literalReplacement);
        Assert.assertEquals("zzz$0zzz", result);

        matcher.reset();
        replaceTest = "\\t$\\$";
        literalReplacement = RegExTest.PF.quoteReplacement(replaceTest);
        result = matcher.replaceAll(literalReplacement);
        Assert.assertEquals("zzz\\t$\\$zzz", result);

        // Supplementary character test
        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("abc"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("zzzabczzz"));
        replaceTest = "$0";
        result = matcher.replaceAll(replaceTest);
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabczzz"), result);

        matcher.reset();
        literalReplacement = RegExTest.PF.quoteReplacement(replaceTest);
        result = matcher.replaceAll(literalReplacement);
        Assert.assertEquals(RegExTest.toSupplementaries("zzz$0zzz"), result);

        matcher.reset();
        replaceTest = "\\t$\\$";
        literalReplacement = RegExTest.PF.quoteReplacement(replaceTest);
        result = matcher.replaceAll(literalReplacement);
        Assert.assertEquals(RegExTest.toSupplementaries("zzz\\t$\\$zzz"), result);

        // IAE should be thrown if backslash or '$' is the last character
        // in replacement string
        try {
            "\uac00".replaceAll("\uac00", "$");
            Assert.fail();
        } catch (IllegalArgumentException iie) {
            ;
        }
        try {
            "\uac00".replaceAll("\uac00", "\\");
            Assert.fail();
        } catch (IllegalArgumentException iie) {
            ;
        }
    }

    // This test is for 4757029
    @Test public void
    regionTest() throws Exception {

        // Regions

        Pattern pattern = RegExTest.PF.compile("abc");
        Matcher matcher = pattern.matcher("abcdefabc");

        matcher.region(0,9);
        Assert.assertTrue(matcher.find());
        Assert.assertTrue(matcher.find());

        matcher.region(0,3);
        Assert.assertTrue(matcher.find());

        matcher.region(3,6);
        Assert.assertFalse(matcher.find());

        matcher.region(0,2);
        Assert.assertFalse(matcher.find());

        RegExTest.expectRegionFail(matcher, 1, -1);
        RegExTest.expectRegionFail(matcher, -1, -1);
        RegExTest.expectRegionFail(matcher, -1, 1);
        RegExTest.expectRegionFail(matcher, 5, 3);
        RegExTest.expectRegionFail(matcher, 5, 12);
        RegExTest.expectRegionFail(matcher, 12, 12);

        pattern = RegExTest.PF.compile("^abc$");
        matcher = pattern.matcher("zzzabczzz");
        matcher.region(0,9);
        Assert.assertFalse(matcher.find());
        matcher.region(3,6);
        Assert.assertTrue(matcher.find());
        matcher.region(3,6);
        matcher.useAnchoringBounds(false);
        Assert.assertFalse(matcher.find());

        // Supplementary character test
        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("abc"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("abcdefabc"));
        matcher.region(0,9*2);
        Assert.assertTrue(matcher.find());
        Assert.assertTrue(matcher.find());
        matcher.region(0,3*2);
        Assert.assertTrue(matcher.find());
        matcher.region(1,3*2);
        Assert.assertFalse(matcher.find());
        matcher.region(3*2,6*2);
        Assert.assertFalse(matcher.find());
        matcher.region(0,2*2);
        Assert.assertFalse(matcher.find());
        matcher.region(0,2*2+1);
        Assert.assertFalse(matcher.find());

        RegExTest.expectRegionFail(matcher, 1*2, -1);
        RegExTest.expectRegionFail(matcher, -1, -1);
        RegExTest.expectRegionFail(matcher, -1, 1*2);
        RegExTest.expectRegionFail(matcher, 5*2, 3*2);
        RegExTest.expectRegionFail(matcher, 5*2, 12*2);
        RegExTest.expectRegionFail(matcher, 12*2, 12*2);

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("^abc$"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("zzzabczzz"));
        matcher.region(0,9*2);
        Assert.assertFalse(matcher.find());
        matcher.region(3*2,6*2);
        Assert.assertTrue(matcher.find());
        matcher.region(3*2+1,6*2);
        Assert.assertFalse(matcher.find());
        matcher.region(3*2,6*2-1);
        Assert.assertFalse(matcher.find());
        matcher.region(3*2,6*2);
        matcher.useAnchoringBounds(false);
        Assert.assertFalse(matcher.find());
    }

    private static void expectRegionFail(Matcher matcher, int index1,
                                         int index2)
    {
        try {
            matcher.region(index1, index2);
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Correct result
        } catch (IllegalStateException ise) {
            // Correct result
        }
    }

    // This test is for 4803197
    @Test public void
    escapedSegmentTest() throws Exception {

        // Escaped segment

        Pattern pattern = RegExTest.PF.compile("\\Qdir1\\dir2\\E");
        RegExTest.checkFind(pattern, "dir1\\dir2", true);

        pattern = RegExTest.PF.compile("\\Qdir1\\dir2\\\\E");
        RegExTest.checkFind(pattern, "dir1\\dir2\\", true);

        pattern = RegExTest.PF.compile("(\\Qdir1\\dir2\\\\E)");
        RegExTest.checkFind(pattern, "dir1\\dir2\\", true);

        // Supplementary character test
        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("\\Qdir1\\dir2\\E"));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("dir1\\dir2"), true);

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("\\Qdir1\\dir2")+"\\\\E");
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("dir1\\dir2\\"), true);

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(\\Qdir1\\dir2")+"\\\\E)");
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("dir1\\dir2\\"), true);
    }

    // This test is for 4792284
    @Test public void
    nonCaptureRepetitionTest() throws Exception {
        String input = "abcdefgh;";

        String[] patterns = new String[] {
            "(?:\\w{4})+;",
            "(?:\\w{8})*;",
            "(?:\\w{2}){2,4};",
            "(?:\\w{4}){2,};",   // only matches the
            ".*?(?:\\w{5})+;",   //     specified minimum
            ".*?(?:\\w{9})*;",   //     number of reps - OK
            "(?:\\w{4})+?;",     // lazy repetition - OK
            "(?:\\w{4})++;",     // possessive repetition - OK
            "(?:\\w{2,}?)+;",    // non-deterministic - OK
            "(\\w{4})+;",        // capturing group - OK
        };

        for (int i = 0; i < patterns.length; i++) {
            // Check find()
            RegExTest.check(patterns[i], 0, input, input, true);
            // Check matches()
            Pattern p = RegExTest.PF.compile(patterns[i]);
            Matcher m = p.matcher(input);

            if (m.matches()) {
                Assert.assertEquals(input, m.group(0));
            } else {
                Assert.fail();
            }
        }
    }

    // This test is for 6358731
    @Test public void
    notCapturedGroupCurlyMatchTest() throws Exception {

        Pattern pattern = RegExTest.PF.compile("(abc)+|(abcd)+");
        Matcher matcher = pattern.matcher("abcd");

        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("abcd", matcher.group(2));
        Assert.assertNull(matcher.group(1));
    }

    // This test is for 4706545
    @Test public void
    javaCharClassTest() throws Exception {
        for (int i=0; i<1000; i++) {
            char c = (char)RegExTest.generator.nextInt();
            RegExTest.check("{javaLowerCase}", c, Character.isLowerCase(c));
            RegExTest.check("{javaUpperCase}", c, Character.isUpperCase(c));
            RegExTest.check("{javaUpperCase}+", c, Character.isUpperCase(c));
            RegExTest.check("{javaTitleCase}", c, Character.isTitleCase(c));
            RegExTest.check("{javaDigit}", c, Character.isDigit(c));
            RegExTest.check("{javaDefined}", c, Character.isDefined(c));
            RegExTest.check("{javaLetter}", c, Character.isLetter(c));
            RegExTest.check("{javaLetterOrDigit}", c, Character.isLetterOrDigit(c));
            RegExTest.check("{javaJavaIdentifierStart}", c,
                  Character.isJavaIdentifierStart(c));
            RegExTest.check("{javaJavaIdentifierPart}", c,
                  Character.isJavaIdentifierPart(c));
            RegExTest.check("{javaUnicodeIdentifierStart}", c,
                  Character.isUnicodeIdentifierStart(c));
            RegExTest.check("{javaUnicodeIdentifierPart}", c,
                  Character.isUnicodeIdentifierPart(c));
            RegExTest.check("{javaIdentifierIgnorable}", c,
                  Character.isIdentifierIgnorable(c));
            RegExTest.check("{javaSpaceChar}", c, Character.isSpaceChar(c));
            RegExTest.check("{javaWhitespace}", c, Character.isWhitespace(c));
            RegExTest.check("{javaISOControl}", c, Character.isISOControl(c));
            RegExTest.check("{javaMirrored}", c, Character.isMirrored(c));

        }

        // Supplementary character test
        for (int i=0; i<1000; i++) {
            int c = RegExTest.generator.nextInt(Character.MAX_CODE_POINT
                                      - Character.MIN_SUPPLEMENTARY_CODE_POINT)
                        + Character.MIN_SUPPLEMENTARY_CODE_POINT;
            RegExTest.check("{javaLowerCase}", c, Character.isLowerCase(c));
            RegExTest.check("{javaUpperCase}", c, Character.isUpperCase(c));
            RegExTest.check("{javaUpperCase}+", c, Character.isUpperCase(c));
            RegExTest.check("{javaTitleCase}", c, Character.isTitleCase(c));
            RegExTest.check("{javaDigit}", c, Character.isDigit(c));
            RegExTest.check("{javaDefined}", c, Character.isDefined(c));
            RegExTest.check("{javaLetter}", c, Character.isLetter(c));
            RegExTest.check("{javaLetterOrDigit}", c, Character.isLetterOrDigit(c));
            RegExTest.check("{javaJavaIdentifierStart}", c,
                  Character.isJavaIdentifierStart(c));
            RegExTest.check("{javaJavaIdentifierPart}", c,
                  Character.isJavaIdentifierPart(c));
            RegExTest.check("{javaUnicodeIdentifierStart}", c,
                  Character.isUnicodeIdentifierStart(c));
            RegExTest.check("{javaUnicodeIdentifierPart}", c,
                  Character.isUnicodeIdentifierPart(c));
            RegExTest.check("{javaIdentifierIgnorable}", c,
                  Character.isIdentifierIgnorable(c));
            RegExTest.check("{javaSpaceChar}", c, Character.isSpaceChar(c));
            RegExTest.check("{javaWhitespace}", c, Character.isWhitespace(c));
            RegExTest.check("{javaISOControl}", c, Character.isISOControl(c));
            RegExTest.check("{javaMirrored}", c, Character.isMirrored(c));
        }
    }

    // This test is for 4523620
    /*
    private static void numOccurrencesTest() throws Exception {
        Pattern pattern = PF.compile("aaa");

        if (pattern.numOccurrences("aaaaaa", false) != 2)
            failCount++;
        if (pattern.numOccurrences("aaaaaa", true) != 4)
            failCount++;

        pattern = PF.compile("^");
        if (pattern.numOccurrences("aaaaaa", false) != 1)
            failCount++;
        if (pattern.numOccurrences("aaaaaa", true) != 1)
            failCount++;

        report("Number of Occurrences");
    }
    */

    // This test is for 4776374
    @Test public void
    caretBetweenTerminatorsTest() throws Exception {
        int flags1 = Pattern.DOTALL;
        int flags2 = Pattern.DOTALL | Pattern.UNIX_LINES;
        int flags3 = Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.MULTILINE;
        int flags4 = Pattern.DOTALL | Pattern.MULTILINE;

        RegExTest.check("^....", flags1, "test\ntest", "test", true);
        RegExTest.check(".....^", flags1, "test\ntest", "test", false);
        RegExTest.check(".....^", flags1, "test\n", "test", false);
        RegExTest.check("....^", flags1, "test\r\n", "test", false);

        RegExTest.check("^....", flags2, "test\ntest", "test", true);
        RegExTest.check("....^", flags2, "test\ntest", "test", false);
        RegExTest.check(".....^", flags2, "test\n", "test", false);
        RegExTest.check("....^", flags2, "test\r\n", "test", false);

        RegExTest.check("^....", flags3, "test\ntest", "test", true);
        RegExTest.check(".....^", flags3, "test\ntest", "test\n", true);
        RegExTest.check(".....^", flags3, "test\u0085test", "test\u0085", false);
        RegExTest.check(".....^", flags3, "test\n", "test", false);
        RegExTest.check(".....^", flags3, "test\r\n", "test", false);
        RegExTest.check("......^", flags3, "test\r\ntest", "test\r\n", true);

        RegExTest.check("^....", flags4, "test\ntest", "test", true);
        RegExTest.check(".....^", flags3, "test\ntest", "test\n", true);
        RegExTest.check(".....^", flags4, "test\u0085test", "test\u0085", true);
        RegExTest.check(".....^", flags4, "test\n", "test\n", false);
        RegExTest.check(".....^", flags4, "test\r\n", "test\r", false);

        // Supplementary character test
        String t = RegExTest.toSupplementaries("test");
        RegExTest.check("^....", flags1, t+"\n"+t, t, true);
        RegExTest.check(".....^", flags1, t+"\n"+t, t, false);
        RegExTest.check(".....^", flags1, t+"\n", t, false);
        RegExTest.check("....^", flags1, t+"\r\n", t, false);

        RegExTest.check("^....", flags2, t+"\n"+t, t, true);
        RegExTest.check("....^", flags2, t+"\n"+t, t, false);
        RegExTest.check(".....^", flags2, t+"\n", t, false);
        RegExTest.check("....^", flags2, t+"\r\n", t, false);

        RegExTest.check("^....", flags3, t+"\n"+t, t, true);
        RegExTest.check(".....^", flags3, t+"\n"+t, t+"\n", true);
        RegExTest.check(".....^", flags3, t+"\u0085"+t, t+"\u0085", false);
        RegExTest.check(".....^", flags3, t+"\n", t, false);
        RegExTest.check(".....^", flags3, t+"\r\n", t, false);
        RegExTest.check("......^", flags3, t+"\r\n"+t, t+"\r\n", true);

        RegExTest.check("^....", flags4, t+"\n"+t, t, true);
        RegExTest.check(".....^", flags3, t+"\n"+t, t+"\n", true);
        RegExTest.check(".....^", flags4, t+"\u0085"+t, t+"\u0085", true);
        RegExTest.check(".....^", flags4, t+"\n", t+"\n", false);
        RegExTest.check(".....^", flags4, t+"\r\n", t+"\r", false);
    }

    // This test is for 4727935
    @Test public void
    dollarAtEndTest() throws Exception {

        int flags1 = Pattern.DOTALL;                      // "$" matches before line sep and at eoi.
        int flags2 = Pattern.DOTALL | Pattern.UNIX_LINES; // "$" matches before "\n" and at eoi.
        int flags3 = Pattern.DOTALL | Pattern.MULTILINE;  // Same as flags1!?

        // The JRE documentation falsely states that, in non-MULTILINE mode, "$" evaluates to "\z" (end-of-input),
        // however actually it evaluates to "\z" (end-of-input but for the final terminator, if any).
        RegExTest.check("....$",       flags1, "test\n",       "test",         true); // !?!?!?
        RegExTest.check("....$",       flags1, "test\r\n",     "test",         true); // !?!?!?
        RegExTest.check(".....$",      flags1, "test\n",       "test\n",       true);
        RegExTest.check(".....$",      flags1, "test\u0085",   "test\u0085",   true);
        RegExTest.check("....$",       flags1, "test\u0085",   "test",         true); // !?!?!?

        RegExTest.check("....$",       flags3, "test\n",       "test",         true);
        RegExTest.check("....$",       flags3, "test\r\n",     "test",         true);
        RegExTest.check(".....$",      flags3, "test\n",       "test\n",       true);
        RegExTest.check(".....$",      flags3, "test\u0085",   "test\u0085",   true);
        RegExTest.check("....$",       flags3, "test\u0085",   "test",         true);

        RegExTest.check("....$",       flags2, "test\n",       "test",         true);
        RegExTest.check(".....$",      flags2, "test\n",       "test\n",       true);
        RegExTest.check(".....$",      flags2, "test\u0085",   "test\u0085",   true);
        RegExTest.check("....$",       flags2, "test\u0085",   "est\u0085",    true);

        RegExTest.check("....$.blah",  flags3, "test\nblah",   "test\nblah",   true);
        RegExTest.check(".....$.blah", flags3, "test\n\nblah", "test\n\nblah", true);
        RegExTest.check("....$blah",   flags3, "test\nblah",   "!!!!",         false);
        RegExTest.check(".....$blah",  flags3, "test\nblah",   "!!!!",         false);

        // Supplementary character test
        String t = RegExTest.toSupplementaries("test");
        String b = RegExTest.toSupplementaries("blah");
        RegExTest.check("....$",  flags1, t+"\n",     t,          true);
        RegExTest.check("....$",  flags1, t+"\r\n",   t,          true);
        RegExTest.check(".....$", flags1, t+"\n",     t+"\n",     true);
        RegExTest.check(".....$", flags1, t+"\u0085", t+"\u0085", true);
        RegExTest.check("....$",  flags1, t+"\u0085", t,          true);

        RegExTest.check("....$",  flags2, t+"\n",     t,                                        true);
        RegExTest.check(".....$", flags2, t+"\n",     t+"\n",                                   true);
        RegExTest.check(".....$", flags2, t+"\u0085", t+"\u0085",                               true);
        RegExTest.check("....$",  flags2, t+"\u0085", RegExTest.toSupplementaries("est\u0085"), true);

        RegExTest.check("....$."+b,  flags3, t+"\n"+b,   t+"\n"+b,   true);
        RegExTest.check(".....$."+b, flags3, t+"\n\n"+b, t+"\n\n"+b, true);
        RegExTest.check("....$"+b,   flags3, t+"\n"+b,   "!!!!",     false);
        RegExTest.check(".....$"+b,  flags3, t+"\n"+b,   "!!!!",     false);
    }

    // This test is for 4711773
    @Test public void
    multilineDollarTest() throws Exception {
        Pattern findCR = RegExTest.PF.compile("$", Pattern.MULTILINE);
        Matcher matcher = findCR.matcher("first bit\nsecond bit");
        matcher.find();
        Assert.assertFalse(matcher.start(0) != 9);
        matcher.find();
        Assert.assertFalse(matcher.start(0) != 20);

        // Supplementary character test
        matcher = findCR.matcher(RegExTest.toSupplementaries("first  bit\n second  bit")); // double BMP chars
        matcher.find();
        Assert.assertFalse(matcher.start(0) != 9*2);
        matcher.find();
        Assert.assertFalse(matcher.start(0) != 20*2);
    }

    @Test public void reluctantRepetitionTest1() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("1(\\s\\S+?){1,3}?[\\s,]2"), "1 word word 2",      true); }
    @Test public void reluctantRepetitionTest2() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("1(\\s\\S+?){1,3}?[\\s,]2"), "1 word word word 2", true); }
    @Test public void reluctantRepetitionTest3() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("1(\\s\\S+?){1,3}?[\\s,]2"), "1 wor wo w 2",       true); }
    @Test public void reluctantRepetitionTest4() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("1(\\s\\S+?){1,3}?[\\s,]2"), "1 word word 2",      true); }
    @Test public void reluctantRepetitionTest5() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("1(\\s\\S+?){1,3}?[\\s,]2"), "1 word 2",           true); }
    @Test public void reluctantRepetitionTest6() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("1(\\s\\S+?){1,3}?[\\s,]2"), "1 wo w w 2",         true); }
    @Test public void reluctantRepetitionTest7() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("1(\\s\\S+?){1,3}?[\\s,]2"), "1 wo w 2",           true); }
    @Test public void reluctantRepetitionTest8() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("1(\\s\\S+?){1,3}?[\\s,]2"), "1 wor w 2",          true); }

    @Test public void
    reluctantRepetitionTest9() throws Exception {

        Pattern p = RegExTest.PF.compile("([a-z])+?c");
        Matcher m = p.matcher("ababcdefdec");
        RegExTest.check(m, "ababc");

        // Supplementary character test
        p = RegExTest.PF.compile(RegExTest.toSupplementaries("([a-z])+?c"));
        m = p.matcher(RegExTest.toSupplementaries("ababcdefdec"));
        RegExTest.check(m, RegExTest.toSupplementaries("ababc"));
    }

    @Test public void
    serializeTest() throws Exception {

        de.unkrig.lfr.core.Pattern pattern = de.unkrig.lfr.core.PatternFactory.INSTANCE.compile("(b)");
        String ss = "capturingGroupStart(1) . 'b' . capturingGroupEnd(1)";
        Assert.assertEquals(ss, pattern.sequenceToString());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream    oos  = new ObjectOutputStream(baos);
        oos.writeObject(pattern);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        de.unkrig.lfr.core.Pattern serializedPattern = (de.unkrig.lfr.core.Pattern) ois.readObject();
        ois.close();

        Assert.assertEquals(ss, serializedPattern.sequenceToString());
        Matcher matcher = serializedPattern.matcher("b");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals(1, matcher.groupCount());
    }

    @Test public void
    gTest() {
        Pattern pattern = RegExTest.PF.compile("\\G\\w");
        Matcher matcher = pattern.matcher("abc#x#x");
        matcher.find();
        matcher.find();
        matcher.find();
        Assert.assertFalse(matcher.find());

        pattern = RegExTest.PF.compile("\\GA*");
        matcher = pattern.matcher("1A2AA3");
        matcher.find();
        Assert.assertFalse(matcher.find());

        pattern = RegExTest.PF.compile("\\GA*");
        matcher = pattern.matcher("1A2AA3");
        Assert.assertTrue(matcher.find(1));
        matcher.find();
        Assert.assertFalse(matcher.find());
    }

    @Test public void
    zTest() {
        Pattern pattern = RegExTest.PF.compile("foo\\Z");
        // Positives
        RegExTest.checkFind(pattern, "foo\u0085", true);
        RegExTest.checkFind(pattern, "foo\u2028", true);
        RegExTest.checkFind(pattern, "foo\u2029", true);
        RegExTest.checkFind(pattern, "foo\n", true);
        RegExTest.checkFind(pattern, "foo\r", true);
        RegExTest.checkFind(pattern, "foo\r\n", true);
        // Negatives
        RegExTest.checkFind(pattern, "fooo", false);
        RegExTest.checkFind(pattern, "foo\n\r", false);

        pattern = RegExTest.PF.compile("foo\\Z", Pattern.UNIX_LINES);
        // Positives
        RegExTest.checkFind(pattern, "foo", true);
        RegExTest.checkFind(pattern, "foo\n", true);
        // Negatives
        RegExTest.checkFind(pattern, "foo\r", false);
        RegExTest.checkFind(pattern, "foo\u0085", false);
        RegExTest.checkFind(pattern, "foo\u2028", false);
        RegExTest.checkFind(pattern, "foo\u2029", false);
    }

    @Test public void
    replaceFirstTest() {
        Pattern pattern = RegExTest.PF.compile("(ab)(c*)");
        Matcher matcher = pattern.matcher("abccczzzabcczzzabccc");
        Assert.assertEquals("testzzzabcczzzabccc", matcher.replaceFirst("test"));

        matcher.reset("zzzabccczzzabcczzzabccczzz");
        Assert.assertEquals("zzztestzzzabcczzzabccczzz", matcher.replaceFirst("test"));

        matcher.reset("zzzabccczzzabcczzzabccczzz");
        String result = matcher.replaceFirst("$1");
        Assert.assertEquals("zzzabzzzabcczzzabccczzz", result);

        matcher.reset("zzzabccczzzabcczzzabccczzz");
        result = matcher.replaceFirst("$2");
        Assert.assertEquals("zzzccczzzabcczzzabccczzz", result);

        pattern = RegExTest.PF.compile("a*");
        matcher = pattern.matcher("aaaaaaaaaa");
        Assert.assertEquals("test", matcher.replaceFirst("test"));

        pattern = RegExTest.PF.compile("a+");
        matcher = pattern.matcher("zzzaaaaaaaaaa");
        Assert.assertEquals("zzztest", matcher.replaceFirst("test"));

        // Supplementary character test
        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(ab)(c*)"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("abccczzzabcczzzabccc"));
        if (!matcher.replaceFirst(RegExTest.toSupplementaries("test"))
                .equals(RegExTest.toSupplementaries("testzzzabcczzzabccc")))
            Assert.fail();

        matcher.reset(RegExTest.toSupplementaries("zzzabccczzzabcczzzabccczzz"));
        if (!matcher.replaceFirst(RegExTest.toSupplementaries("test")).
            equals(RegExTest.toSupplementaries("zzztestzzzabcczzzabccczzz")))
            Assert.fail();

        matcher.reset(RegExTest.toSupplementaries("zzzabccczzzabcczzzabccczzz"));
        result = matcher.replaceFirst("$1");
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabzzzabcczzzabccczzz"), result);

        matcher.reset(RegExTest.toSupplementaries("zzzabccczzzabcczzzabccczzz"));
        result = matcher.replaceFirst("$2");
        Assert.assertEquals(RegExTest.toSupplementaries("zzzccczzzabcczzzabccczzz"), result);

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("a*"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("aaaaaaaaaa"));
        Assert.assertEquals(RegExTest.toSupplementaries("test"), matcher.replaceFirst(RegExTest.toSupplementaries("test")));

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("a+"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("zzzaaaaaaaaaa"));
        Assert.assertEquals(RegExTest.toSupplementaries("zzztest"), matcher.replaceFirst(RegExTest.toSupplementaries("test")));
    }

    @Test public void
    unixLinesTest() {
        Pattern pattern = RegExTest.PF.compile(".*");
        Matcher matcher = pattern.matcher("aa\u2028blah");
        matcher.find();
        Assert.assertEquals("aa", matcher.group(0));

        pattern = RegExTest.PF.compile(".*", Pattern.UNIX_LINES);
        matcher = pattern.matcher("aa\u2028blah");
        matcher.find();
        Assert.assertEquals("aa\u2028blah", matcher.group(0));

        pattern = RegExTest.PF.compile("[az]$",
                                  Pattern.MULTILINE | Pattern.UNIX_LINES);
        matcher = pattern.matcher("aa\u2028zz");
        RegExTest.check(matcher, "a\u2028", false);

        // Supplementary character test
        pattern = RegExTest.PF.compile(".*");
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa\u2028blah"));
        matcher.find();
        Assert.assertEquals(RegExTest.toSupplementaries("aa"), matcher.group(0));

        pattern = RegExTest.PF.compile(".*", Pattern.UNIX_LINES);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa\u2028blah"));
        matcher.find();
        Assert.assertEquals(RegExTest.toSupplementaries("aa\u2028blah"), matcher.group(0));

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("[az]$"),
                                  Pattern.MULTILINE | Pattern.UNIX_LINES);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa\u2028zz"));
        RegExTest.check(matcher, RegExTest.toSupplementaries("a\u2028"), false);
    }

    @Test public void
    commentsTest() {
        int flags = Pattern.COMMENTS;

        Pattern pattern = RegExTest.PF.compile("aa \\# aa", flags);
        Matcher matcher = pattern.matcher("aa#aa");
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile("aa  # blah", flags);
        matcher = pattern.matcher("aa");
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile("aa blah", flags);
        matcher = pattern.matcher("aablah");
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile("aa  # blah blech  ", flags);
        matcher = pattern.matcher("aa");
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile("aa  # blah\n  ", flags);
        matcher = pattern.matcher("aa");
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile("aa  # blah\nbc # blech", flags);
        matcher = pattern.matcher("aabc");
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile("aa  # blah\nbc# blech", flags);
        matcher = pattern.matcher("aabc");
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile("aa  # blah\nbc\\# blech", flags);
        matcher = pattern.matcher("aabc#blech");
        Assert.assertTrue(matcher.matches());

        // Supplementary character test
        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("aa \\# aa"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa#aa"));
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("aa  # blah"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa"));
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("aa blah"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aablah"));
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("aa  # blah blech  "), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa"));
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("aa  # blah\n  "), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa"));
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("aa  # blah\nbc # blech"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aabc"));
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("aa  # blah\nbc# blech"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aabc"));
        Assert.assertTrue(matcher.matches());

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("aa  # blah\nbc\\# blech"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aabc#blech"));
        Assert.assertTrue(matcher.matches());
    }

    @Test public void
    caseFoldingTest1() { // bug 4504687
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern pattern = RegExTest.PF.compile("aa", flags);
        Matcher matcher = pattern.matcher("ab");
        Assert.assertFalse(matcher.matches());
    }

    @Test public void
    caseFoldingTest2() {
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern pattern = RegExTest.PF.compile("aA", flags);
        Matcher matcher = pattern.matcher("ab");
        Assert.assertFalse(matcher.matches());
    }

    @Test public void
    caseFoldingTest3() {
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern pattern = RegExTest.PF.compile("aa", flags);
        Matcher matcher = pattern.matcher("aB");
        Assert.assertFalse(matcher.matches());
        matcher = pattern.matcher("Ab");
        Assert.assertFalse(matcher.matches());
    }

    @Test public void
    caseFoldingTest4() {

        // ASCII               "a"
        // Latin-1 Supplement  "a" + grave
        // Cyrillic            "a"
        String[] patterns = new String[] {
            "a",      "\u00e0",          "\u0430",          // single
            "ab",     "\u00e0\u00e1",    "\u0430\u0431",    // slice
            "[a]",    "[\u00e0]",        "[\u0430]",        // class single
            "[a-b]",  "[\u00e0-\u00e5]", "[\u0430-\u0431]", // class range
            "(a)\\1", "(\u00e0)\\1",     "(\u0430)\\1",     // back reference
        };

        String[] texts = new String[] {
            "A",  "\u00c0",       "\u0410",
            "AB", "\u00c0\u00c1", "\u0410\u0411",
            "A",  "\u00c0",       "\u0410",
            "B",  "\u00c2",       "\u0411",
            "aA", "\u00e0\u00c0", "\u0430\u0410"
        };

        boolean[] expected = new boolean[] {
            true, false, false,
            true, false, false,
            true, false, false,
            true, false, false,
            true, false, false
        };

        int flags = Pattern.CASE_INSENSITIVE;
        for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = RegExTest.PF.compile(patterns[i], flags);
            Matcher matcher = pattern.matcher(texts[i]);
            Assert.assertEquals(i + ": \"" + patterns[i] + "\" \"" + texts[i] + "\"", expected[i], matcher.matches());
        }

        flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = RegExTest.PF.compile(patterns[i], flags);
            Matcher matcher = pattern.matcher(texts[i]);
            if (!matcher.matches()) {
                System.out.println("<2> Failed at " + i);
                Assert.fail();
            }
        }
        // flag unicode_case alone should do nothing
        flags = Pattern.UNICODE_CASE;
        for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = RegExTest.PF.compile(patterns[i], flags);
            Matcher matcher = pattern.matcher(texts[i]);
            if (matcher.matches()) {
                System.out.println("<3> Failed at " + i);
                Assert.fail();
            }
        }
    }

    @Test public void
    caseFoldingTest5() {

        char x = Character.toUpperCase('\u0131');
        x = Character.toLowerCase(x);

        x = Character.toUpperCase('\u0130');
        x = Character.toLowerCase(x);

        // Special cases: i, I, u+0131 and u+0130
        int flags = Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE;
        Pattern pattern = RegExTest.PF.compile("[h-j]+", flags);
        Assert.assertTrue(pattern.matcher("\u0131\u0130").matches());
    }

    @Test public void
    appendTest1() {

        Pattern pattern = RegExTest.PF.compile("(ab)(cd)");
        Matcher matcher = pattern.matcher("abcd");
        String result = matcher.replaceAll("$2$1");
        Assert.assertEquals("cdab", result);
    }

    @Test public void
    appendTest2() {

        Pattern pattern = RegExTest.PF.compile("([a-z]+)( *= *)([0-9]+)");
        String  replacement = "$3$2$1";

        {
            Matcher matcher = pattern.matcher("Swap all: first = 123, second = 456");
            Assert.assertEquals("Swap all: 123 = first, 456 = second", matcher.replaceAll(replacement));
        }

        {
            Matcher matcher = pattern.matcher("Swap one: first = 123, second = 456");

            Assert.assertTrue(matcher.find());
            StringBuffer sb = new StringBuffer();
            matcher.appendReplacement(sb, replacement).appendTail(sb);

            Assert.assertEquals("Swap one: 123 = first, second = 456", sb.toString());
        }
    }

    @Test public void
    appendTest3() {

        // Supplementary character test
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(ab)(cd)"));
        Matcher matcher = pattern.matcher(RegExTest.toSupplementaries("abcd"));
        String result = matcher.replaceAll("$2$1");
        Assert.assertEquals(RegExTest.toSupplementaries("cdab"), result);

        String s1 = RegExTest.toSupplementaries("Swap all: first = 123, second = 456");
        String s2 = RegExTest.toSupplementaries("Swap one: first = 123, second = 456");
        String r  = RegExTest.toSupplementaries("$3$2$1");
        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("([a-z]+)( *= *)([0-9]+)"));
        matcher = pattern.matcher(s1);

        result = matcher.replaceAll(r);
        Assert.assertEquals(RegExTest.toSupplementaries("Swap all: 123 = first, 456 = second"), result);

        matcher = pattern.matcher(s2);

        if (matcher.find()) {
            StringBuffer sb = new StringBuffer();
            matcher.appendReplacement(sb, r);
            matcher.appendTail(sb);
            result = sb.toString();
            Assert.assertEquals(RegExTest.toSupplementaries("Swap one: 123 = first, second = 456"), result);
        }
    }

    @Test public void
    splitTest1() {

        Pattern pattern = RegExTest.PF.compile(":");
        String[] result = pattern.split("foo:and:boo", 2);
        Assert.assertEquals("foo",     result[0]);
        Assert.assertEquals("and:boo", result[1]);

        // Supplementary character test
        Pattern patternX = RegExTest.PF.compile(RegExTest.toSupplementaries("X"));
        result = patternX.split(RegExTest.toSupplementaries("fooXandXboo"), 2);
        Assert.assertEquals(RegExTest.toSupplementaries("foo"), result[0]);
        Assert.assertEquals(RegExTest.toSupplementaries("andXboo"), result[1]);

        CharBuffer cb = CharBuffer.allocate(100);
        cb.put("foo:and:boo");
        cb.flip();
        result = pattern.split(cb);
        Assert.assertEquals("foo", result[0]);
        Assert.assertEquals("and", result[1]);
        Assert.assertEquals("boo", result[2]);

        // Supplementary character test
        CharBuffer cbs = CharBuffer.allocate(100);
        cbs.put(RegExTest.toSupplementaries("fooXandXboo"));
        cbs.flip();
        result = patternX.split(cbs);
        Assert.assertEquals(RegExTest.toSupplementaries("foo"), result[0]);
        Assert.assertEquals(RegExTest.toSupplementaries("and"), result[1]);
        Assert.assertEquals(RegExTest.toSupplementaries("boo"), result[2]);

        String source = "0123456789";
        for (int limit=-2; limit<3; limit++) {
            for (int x=0; x<10; x++) {
                result = source.split(Integer.toString(x), limit);
                int expectedLength = limit < 1 ? 2 : limit;

                if ((limit == 0) && (x == 9)) {
                    // expected dropping of ""
                    Assert.assertFalse(result.length != 1);
                    if (!result[0].equals("012345678")) {
                        Assert.fail();
                    }
                } else {
                    if (result.length != expectedLength) {
                        Assert.fail();
                    }
                    if (!result[0].equals(source.substring(0,x))) {
                        if (limit != 1) {
                            Assert.fail();
                        } else {
                            if (!result[0].equals(source.substring(0,10))) {
                                Assert.fail();
                            }
                        }
                    }
                    if (expectedLength > 1) { // Check segment 2
                        Assert.assertEquals(source.substring(x+1,10), result[1]);
                    }
                }
            }
        }
        // Check the case for no match found
        for (int limit=-2; limit<3; limit++) {
            result = source.split("e", limit);
            Assert.assertFalse(result.length != 1);
            Assert.assertEquals(source, result[0]);
        }
        // Check the case for limit == 0, source = "";
        // split() now returns 0-length for empty source "" see #6559590
        source = "";
        result = source.split("e", 0);
        Assert.assertFalse(result.length != 1);
        Assert.assertEquals(source, result[0]);
    }

    @Test public void
    splitTest2() {

        // Check both split() and splitAsStream(), especially for zero-lenth
        // input and zero-lenth match cases

        // normal non-zero-match
        this.assertSplit(" ",           "Abc Efg Hij",  "Abc", "Efg", "Hij");
        // leading empty str for non-zero-match
        this.assertSplit(" ",           " Abc Efg Hij", "",    "Abc", "Efg", "Hij");
        // non-zero-match in the middle
        this.assertSplit(" ",           "Abc  Efg Hij", "Abc", "",    "Efg", "Hij");

        // no leading empty str for zero-match
        // This is a JUR8 feature/change/bug fix:
        // "A zero-width match at the beginning however never produces such empty leading substring."
        this.assertSplit("(?=\\p{Lu})", "AbcEfgHij",    "Abc", "Efg", "Hij");
        this.assertSplit("(?=\\p{Lu})", "AbcEfg",       "Abc", "Efg");
        this.assertSplit("(?=\\p{Lu})", "Abc",          "Abc");

        // zero-length input
        this.assertSplit(" ",           "",             "");
        this.assertSplit(".*",          "",             "");
    }

    @Test public void
    splitTest3() {

        // some tests from PatternStreamTest.java
        this.assertSplit("4",        "awgqwefg1fefw4vssv1vvv1",                                    "awgqwefg1fefw", "vssv1vvv1");
        this.assertSplit("\u00a3a",  "afbfq\u00a3abgwgb\u00a3awngnwggw\u00a3a\u00a3ahjrnhneerh",   "afbfq", "bgwgb", "wngnwggw", "", "hjrnhneerh");
        this.assertSplit("1",        "awgqwefg1fefw4vssv1vvv1",                                    "awgqwefg", "fefw4vssv", "vvv");
        this.assertSplit("1",        "a\u4ebafg1fefw\u4eba4\u9f9cvssv\u9f9c1v\u672c\u672cvv",      "a\u4ebafg", "fefw\u4eba4\u9f9cvssv\u9f9c", "v\u672c\u672cvv");
        this.assertSplit("\u56da",   "1\u56da23\u56da456\u56da7890",                               "1", "23", "456", "7890");
        this.assertSplit("\u56da",   "1\u56da23\u9f9c\u672c\u672c\u56da456\u56da\u9f9c\u672c7890", "1", "23\u9f9c\u672c\u672c", "456", "\u9f9c\u672c7890");
        this.assertSplit("\u56da",   "",                                                           "");
        //multiple septs
        this.assertSplit("[ \t,:.]", "This is,testing: with\tdifferent separators.",               "This", "is", "testing", "", "with", "different", "separators");
        this.assertSplit("o",        "boo:and:foo",                                                "b", "", ":and:f");
        this.assertSplit("o",        "booooo:and:fooooo",                                          "b", "", "", "", "", ":and:f");
        this.assertSplit("o",        "fooooo:",                                                    "f", "", "", "", "", ":");
    }

    public void assertSplit(String regex, String input, String... expected) throws ArrayComparisonFailure {

        Pattern pattern = RegExTest.PF.compile(regex);

        {
            String[] actual = pattern.split(input);

            Assert.assertArrayEquals(
                Arrays.toString(expected) + " / " + Arrays.toString(actual),
                expected,
                actual
            );
        }

//        // splitAsStream() return empty resulting array for zero-length input for now (??)
//        if (input.length() > 0) {
//
//            Assert.assertArrayEquals(expected, pattern.splitAsStream(input).toArray());
//        }
    }

    @Test public void
    negationTest() {
        Pattern pattern = RegExTest.PF.compile("[\\[@^]+");
        Matcher matcher = pattern.matcher("@@@@[[[[^^^^");
        Assert.assertTrue(matcher.find());
        Assert.assertEquals("@@@@[[[[^^^^", matcher.group(0));
        pattern = RegExTest.PF.compile("[@\\[^]+");
        matcher = pattern.matcher("@@@@[[[[^^^^");
        Assert.assertTrue(matcher.find());
        Assert.assertEquals("@@@@[[[[^^^^", matcher.group(0));
        pattern = RegExTest.PF.compile("[@\\[^@]+");
        matcher = pattern.matcher("@@@@[[[[^^^^");
        Assert.assertTrue(matcher.find());
        Assert.assertEquals("@@@@[[[[^^^^", matcher.group(0));

        pattern = RegExTest.PF.compile("\\)");
        matcher = pattern.matcher("xxx)xxx");
        Assert.assertTrue(matcher.find());
    }

    @Test public void
    ampersandTest() {
        Pattern pattern = RegExTest.PF.compile("[&@]+");
        RegExTest.checkFind(pattern, "@@@@&&&&", true);

        pattern = RegExTest.PF.compile("[@&]+");
        RegExTest.checkFind(pattern, "@@@@&&&&", true);

        pattern = RegExTest.PF.compile("[@\\&]+");
        RegExTest.checkFind(pattern, "@@@@&&&&", true);
    }

    @Test public void
    octalTest() throws Exception {
        Pattern pattern = RegExTest.PF.compile("\\u0007");
        Matcher matcher = pattern.matcher("\u0007");
        Assert.assertTrue(matcher.matches());
        pattern = RegExTest.PF.compile("\\07");
        matcher = pattern.matcher("\u0007");
        Assert.assertTrue(matcher.matches());
        pattern = RegExTest.PF.compile("\\007");
        matcher = pattern.matcher("\u0007");
        Assert.assertTrue(matcher.matches());
        pattern = RegExTest.PF.compile("\\0007");
        matcher = pattern.matcher("\u0007");
        Assert.assertTrue(matcher.matches());
        pattern = RegExTest.PF.compile("\\040");
        matcher = pattern.matcher("\u0020");
        Assert.assertTrue(matcher.matches());
        pattern = RegExTest.PF.compile("\\0403");
        matcher = pattern.matcher("\u00203");
        Assert.assertTrue(matcher.matches());
        pattern = RegExTest.PF.compile("\\0103");
        matcher = pattern.matcher("\u0043");
        Assert.assertTrue(matcher.matches());
    }

    @Test public void
    longPatternTest() throws Exception {
        try {
            @SuppressWarnings("unused")
            Pattern pattern = RegExTest.PF.compile(
                "a 32-character-long pattern xxxx");
            pattern = RegExTest.PF.compile("a 33-character-long pattern xxxxx");
            pattern = RegExTest.PF.compile("a thirty four character long regex");
            StringBuffer patternToBe = new StringBuffer(101);
            for (int i=0; i<100; i++)
                patternToBe.append((char)(97 + i%26));
            pattern = RegExTest.PF.compile(patternToBe.toString());
        } catch (PatternSyntaxException e) {
            Assert.fail();
        }

        // Supplementary character test
        try {
            @SuppressWarnings("unused")
            Pattern pattern = RegExTest.PF.compile(
                RegExTest.toSupplementaries("a 32-character-long pattern xxxx"));
            pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("a 33-character-long pattern xxxxx"));
            pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("a thirty four character long regex"));
            StringBuffer patternToBe = new StringBuffer(101*2);
            for (int i=0; i<100; i++)
                patternToBe.append(Character.toChars(Character.MIN_SUPPLEMENTARY_CODE_POINT
                                                     + 97 + i%26));
            pattern = RegExTest.PF.compile(patternToBe.toString());
        } catch (PatternSyntaxException e) {
            Assert.fail();
        }
    }

    @Test public void
    group0Test() throws Exception {
        Pattern pattern = RegExTest.PF.compile("(tes)ting");
        Matcher matcher = pattern.matcher("testing");
        RegExTest.check(matcher, "testing");

        matcher.reset("testing");
        if (matcher.lookingAt()) {
            Assert.assertEquals("testing", matcher.group(0));
        } else {
            Assert.fail();
        }

        matcher.reset("testing");
        if (matcher.matches()) {
            Assert.assertEquals("testing", matcher.group(0));
        } else {
            Assert.fail();
        }

        pattern = RegExTest.PF.compile("(tes)ting");
        matcher = pattern.matcher("testing");
        if (matcher.lookingAt()) {
            Assert.assertEquals("testing", matcher.group(0));
        } else {
            Assert.fail();
        }

        pattern = RegExTest.PF.compile("^(tes)ting");
        matcher = pattern.matcher("testing");
        if (matcher.matches()) {
            Assert.assertEquals("testing", matcher.group(0));
        } else {
            Assert.fail();
        }

        // Supplementary character test
        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(tes)ting"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("testing"));
        RegExTest.check(matcher, RegExTest.toSupplementaries("testing"));

        matcher.reset(RegExTest.toSupplementaries("testing"));
        if (matcher.lookingAt()) {
            Assert.assertEquals(RegExTest.toSupplementaries("testing"), matcher.group(0));
        } else {
            Assert.fail();
        }

        matcher.reset(RegExTest.toSupplementaries("testing"));
        if (matcher.matches()) {
            Assert.assertEquals(RegExTest.toSupplementaries("testing"), matcher.group(0));
        } else {
            Assert.fail();
        }

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(tes)ting"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("testing"));
        if (matcher.lookingAt()) {
            Assert.assertEquals(RegExTest.toSupplementaries("testing"), matcher.group(0));
        } else {
            Assert.fail();
        }

        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("^(tes)ting"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("testing"));
        if (matcher.matches()) {
            Assert.assertEquals(RegExTest.toSupplementaries("testing"), matcher.group(0));
        } else {
            Assert.fail();
        }
    }

    @Test public void
    findIntTest() throws Exception {
        Pattern p = RegExTest.PF.compile("blah");
        Matcher m = p.matcher("zzzzblahzzzzzblah");
        boolean result = m.find(2);
        Assert.assertTrue(result);

        p = RegExTest.PF.compile("$");
        m = p.matcher("1234567890");
        result = m.find(10);
        Assert.assertTrue(result);
        try {
            result = m.find(11);
            Assert.fail();
        } catch (IndexOutOfBoundsException e) {
            // correct result
        }

        // Supplementary character test
        p = RegExTest.PF.compile(RegExTest.toSupplementaries("blah"));
        m = p.matcher(RegExTest.toSupplementaries("zzzzblahzzzzzblah"));
        result = m.find(2);
        Assert.assertTrue(result);
    }

    @Test public void
    emptyPatternTest() throws Exception {
        Pattern p = RegExTest.PF.compile("");
        Matcher m = p.matcher("foo");

        // Should find empty pattern at beginning of input
        Assert.assertTrue(m.find());
        Assert.assertEquals(0, m.start());
        Assert.assertEquals(0, m.end());
        Assert.assertEquals("", m.group());

        // Should not match entire input if input is not empty
        m.reset();
        Assert.assertFalse(m.matches());

        try {
            m.start(0);
            Assert.fail();
        } catch (IllegalStateException e) {
            // Correct result
        }

        // Should match entire input if input is empty
        m.reset("");
        Assert.assertTrue(m.matches());

        Assert.assertTrue(RegExTest.PF.matches("", ""));

        Assert.assertFalse(RegExTest.PF.matches("", "foo"));
    }

    @Test public void
    charClassTest1() throws Exception {
        Pattern pattern = RegExTest.PF.compile("blah[ab]]blech");
        RegExTest.checkFind(pattern, "blahb]blech", true);
    }

    @Test public void
    charClassTest2() throws Exception {
        Pattern pattern = RegExTest.PF.compile("[abc[def]]");
        RegExTest.checkFind(pattern, "b", true);
    }

    @Test public void
    charClassTest3() throws Exception {
        // Supplementary character tests
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("blah[ab]]blech"));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("blahb]blech"), true);
    }

    @Test public void
    charClassTest4() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("[abc[def]]"));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("b"), true);
    }

    @Test public void
    charClassTest5() throws Exception {
//        try {
            // u00ff when UNICODE_CASE
            Pattern pattern = RegExTest.PF.compile("[ab\u00ffcd]",
                                      Pattern.CASE_INSENSITIVE|
                                      Pattern.UNICODE_CASE);
            RegExTest.checkFind(pattern, "ab\u00ffcd", true);
            RegExTest.checkFind(pattern, "Ab\u0178Cd", true);
    }

    @Test public void
    charClassTest6() throws Exception {
            // u00b5 when UNICODE_CASE
            Pattern pattern = RegExTest.PF.compile("[ab\u00b5cd]",
                                      Pattern.CASE_INSENSITIVE|
                                      Pattern.UNICODE_CASE);
            RegExTest.checkFind(pattern, "ab\u00b5cd", true);
            RegExTest.checkFind(pattern, "Ab\u039cCd", true);
//        } catch (Exception e) { Assert.fail(); }
    }

    @Test public void
    charClassTest7() throws Exception {
        /* Special cases
           (1)LatinSmallLetterLongS u+017f            toUpperCase() => 'S'
           (2)LatinSmallLetterDotlessI u+0131         toUpperCase() => 'I'
           (3)LatineCapitalLetterIWithDotAbove u+0130 toLowerCase() => 'i'
           (4)KelvinSign u+212a                       toLowerCase() => 'k'
           (5)AngstromSign u+212b                     toLowerCase() => 0xe5 = 'å'
        */
        int flags = Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE;
//        Pattern pattern = RegExTest.PF.compile("[sik\u00c5]+", flags);
//        Assert.assertTrue(pattern.matcher("\u017f\u0130\u0131\u212a\u212b").matches());
        Pattern pattern = RegExTest.PF.compile("[sik\u00c5]", flags);
        System.out.printf("%x%n", Character.toUpperCase(0x212b));
        System.out.printf("%x%n", Character.toLowerCase(0x212b));
        System.out.printf("%x%n", Character.toTitleCase(0x212b));
        System.out.printf("%c%n", 0xe5);
        Assert.assertTrue(pattern.matcher("\u017f").matches());
        Assert.assertTrue(pattern.matcher("\u0130").matches());
        Assert.assertTrue(pattern.matcher("\u0131").matches());
        Assert.assertTrue(pattern.matcher("\u212a").matches());
        Assert.assertTrue(pattern.matcher("\u212b").matches());
    }

    @Test public void
    charClassTest8() throws Exception {
        // /* AU */ This one fails for JDK 1.8.0_45
//        try {
//            PF.compile("[", Pattern.CANON_EQ);
//            failCount++;
//        } catch (PatternSyntaxException expected) {
//        } catch (Exception unexpected) {
//            failCount++;
//        }
    }

    @Test public void
    caretTest() throws Exception {
        Pattern pattern = RegExTest.PF.compile("\\w*");
        Matcher matcher = pattern.matcher("a#bc#def##g");
        RegExTest.check(matcher, "a");
        RegExTest.check(matcher, "");
        RegExTest.check(matcher, "bc");
        RegExTest.check(matcher, "");
        RegExTest.check(matcher, "def");
        RegExTest.check(matcher, "");
        RegExTest.check(matcher, "");
        RegExTest.check(matcher, "g");
        RegExTest.check(matcher, "");
        Assert.assertFalse(matcher.find());

        pattern = RegExTest.PF.compile("^\\w*");
        matcher = pattern.matcher("a#bc#def##g");
        RegExTest.check(matcher, "a");
        Assert.assertFalse(matcher.find());

        pattern = RegExTest.PF.compile("\\w");
        matcher = pattern.matcher("abc##x");
        RegExTest.check(matcher, "a");
        RegExTest.check(matcher, "b");
        RegExTest.check(matcher, "c");
        RegExTest.check(matcher, "x");
        Assert.assertFalse(matcher.find());

        pattern = RegExTest.PF.compile("^\\w");
        matcher = pattern.matcher("abc##x");
        RegExTest.check(matcher, "a");
        Assert.assertFalse(matcher.find());

        pattern = RegExTest.PF.compile("\\A\\p{Alpha}{3}");
        matcher = pattern.matcher("abcdef-ghi\njklmno");
        RegExTest.check(matcher, "abc");
        Assert.assertFalse(matcher.find());

        pattern = RegExTest.PF.compile("^\\p{Alpha}{3}", Pattern.MULTILINE);
        matcher = pattern.matcher("abcdef-ghi\njklmno");
        RegExTest.check(matcher, "abc");
        RegExTest.check(matcher, "jkl");
        Assert.assertFalse(matcher.find());

        pattern = RegExTest.PF.compile("^", Pattern.MULTILINE);
        matcher = pattern.matcher("this is some text");
        String result = matcher.replaceAll("X");
        Assert.assertEquals("Xthis is some text", result);

        pattern = RegExTest.PF.compile("^");
        matcher = pattern.matcher("this is some text");
        result = matcher.replaceAll("X");
        Assert.assertEquals("Xthis is some text", result);

        pattern = RegExTest.PF.compile("^", Pattern.MULTILINE | Pattern.UNIX_LINES);
        matcher = pattern.matcher("this is some text\n");
        Assert.assertEquals("Xthis is some text\n", matcher.replaceAll("X"));
    }

    @Test public void
    groupCaptureTest() throws Exception {
        // Independent group
        Pattern pattern = RegExTest.PF.compile("x+(?>y+)z+");
        Matcher matcher = pattern.matcher("xxxyyyzzz");
        matcher.find();
        try {
            @SuppressWarnings("unused")
            String blah = matcher.group(1);
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Good result
        }
        // Pure group
        pattern = RegExTest.PF.compile("x+(?:y+)z+");
        matcher = pattern.matcher("xxxyyyzzz");
        matcher.find();
        try {
            @SuppressWarnings("unused")
            String blah = matcher.group(1);
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Good result
        }

        // Supplementary character tests
        // Independent group
        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("x+(?>y+)z+"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("xxxyyyzzz"));
        matcher.find();
        try {
            @SuppressWarnings("unused")
            String blah = matcher.group(1);
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Good result
        }
        // Pure group
        pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("x+(?:y+)z+"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("xxxyyyzzz"));
        matcher.find();
        try {
            @SuppressWarnings("unused")
            String blah = matcher.group(1);
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Good result
        }
    }

    @Test public void
    backRefTest1() throws Exception {
        Pattern pattern = RegExTest.PF.compile("(a*)bc\\1");
        RegExTest.checkFind(pattern, "zzzaabcazzz", true);
    }

    @Test public void
    backRefTest2() throws Exception {
        Pattern pattern = RegExTest.PF.compile("(a*)bc\\1");
        RegExTest.checkFind(pattern, "zzzaabcaazzz", true);
    }

    @Test public void
    backRefTest3() throws Exception {
        Pattern pattern = RegExTest.PF.compile("(abc)(def)\\1");
        RegExTest.checkFind(pattern, "abcdefabc", true);
    }

    @Test public void
    backRefTest4() throws Exception {
        Pattern pattern = RegExTest.PF.compile("(abc)(def)\\3");
        RegExTest.checkFind(pattern, "abcdefabc", false);
    }

    @Test public void
    backRefTest5() throws Exception {
//        if (RegExTest.JUR) {
            try {
                for (int i = 1; i < 10; i++) {
                    // Make sure backref 1-9 are always accepted
                    Pattern pattern = RegExTest.PF.compile("abcdef\\" + i);
                    // and fail to match if the target group does not exit
                    RegExTest.checkFind(pattern, "abcdef", false);
                }
            } catch(PatternSyntaxException e) {
                Assert.fail();
            }
//        }
    }

    @Test public void
    backRefTest6() throws Exception {
        Pattern pattern = RegExTest.PF.compile("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)\\11");
        RegExTest.checkFind(pattern, "abcdefghija", false);
        RegExTest.checkFind(pattern, "abcdefghija1", true);
    }

    @Test public void
    backRefTest7() throws Exception {
        Pattern pattern = RegExTest.PF.compile("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(k)\\11");
        RegExTest.checkFind(pattern, "abcdefghijkk", true);
    }

    @Test public void
    backRefTest8() throws Exception {
        Pattern pattern = RegExTest.PF.compile("(a)bcdefghij\\11");
        RegExTest.checkFind(pattern, "abcdefghija1", true);
    }

    @Test public void
    backRefTest9() throws Exception {
        // Supplementary character tests
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(a*)bc\\1"));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("zzzaabcazzz"), true);
    }

    @Test public void
    backRefTest10() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(a*)bc\\1"));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("zzzaabcaazzz"), true);
    }

    @Test public void
    backRefTest11() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(abc)(def)\\1"));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abcdefabc"), true);
    }

    @Test public void
    backRefTest12() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(abc)(def)\\3"));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abcdefabc"), false);
    }

    @Test public void
    backRefTest13() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)\\11"));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abcdefghija"), false);
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abcdefghija1"), true);
    }

    @Test public void
    backRefTest14() throws Exception {
        Pattern pattern = RegExTest.PF.compile(RegExTest.toSupplementaries("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(k)\\11"));
        RegExTest.checkFind(pattern, RegExTest.toSupplementaries("abcdefghijkk"), true);
    }

    /**
     * Unicode Technical Report #18, section 2.6 End of Line
     * There is no empty line to be matched in the sequence \u000D\u000A
     * but there is an empty line in the sequence \u000A\u000D.
     */
    @Test public void
    anchorTest1() throws Exception {
        Pattern p = RegExTest.PF.compile("^.*$", Pattern.MULTILINE);
        Matcher m = p.matcher("blah1\r\nblah2");

        Assert.assertTrue(m.find());
        Assert.assertEquals(0, m.start());
        Assert.assertEquals(5, m.end());
        Assert.assertEquals("blah1", m.group());

        Assert.assertTrue(m.find());
        Assert.assertEquals(7, m.start());
        Assert.assertEquals(12, m.end());
        Assert.assertEquals("blah2", m.group());

        Assert.assertFalse(m.find());

        m.reset("blah1\n\rblah2");
        m.find();
        m.find();
        m.find();
        Assert.assertEquals("blah2", m.group());
    }

    @Test public void
    anchorTest2() throws Exception {

        // Test behavior of $ with \r\n at end of input
        Pattern p = RegExTest.PF.compile(".+$");
        Matcher m = p.matcher("blah1\r\n");

        Assert.assertTrue(m.find());
        Assert.assertEquals(0, m.start());
        Assert.assertEquals(5, m.end());
        Assert.assertEquals("blah1", m.group());

        Assert.assertFalse(m.find());
    }

    @Test public void
    anchorTest3() throws Exception {

        // Test behavior of $ with \r\n at end of input in multiline
        Pattern p = RegExTest.PF.compile(".+$", Pattern.MULTILINE);
        Matcher m = p.matcher("blah1\r\n");

        Assert.assertTrue(m.find());

        Assert.assertFalse(m.find());
    }

    @Test public void
    anchorTest4() throws Exception {

        // Test for $ recognition of \u0085 for bug 4527731
        Pattern p = RegExTest.PF.compile(".+$", Pattern.MULTILINE);
        Matcher m = p.matcher("blah1\u0085");

        Assert.assertTrue(m.find());
    }

    @Test public void
    anchorTest5() throws Exception {

        // Supplementary character test
        Pattern p = RegExTest.PF.compile("^.*$", Pattern.MULTILINE);
        Matcher m = p.matcher(RegExTest.toSupplementaries("blah1\r\nblah2"));

        m.find();

        m.find();
        Assert.assertEquals(RegExTest.toSupplementaries("blah2"), m.group());

        m.reset(RegExTest.toSupplementaries("blah1\n\rblah2"));
        m.find();
        m.find();
        m.find();
        Assert.assertEquals(RegExTest.toSupplementaries("blah2"), m.group());
    }

    @Test public void
    anchorTest7() throws Exception {

        // Test behavior of $ with \r\n at end of input
        Pattern p = RegExTest.PF.compile(".+$");
        Matcher m = p.matcher(RegExTest.toSupplementaries("blah1\r\n"));

        Assert.assertTrue(m.find());
        Assert.assertEquals(0, m.start());
        Assert.assertEquals(9, m.end());
        Assert.assertEquals(RegExTest.toSupplementaries("blah1"), m.group());

        Assert.assertFalse(m.find());
    }

    @Test public void
    anchorTest8() throws Exception {

        // Test behavior of $ with \r\n at end of input in multiline
        Pattern p = RegExTest.PF.compile(".+$", Pattern.MULTILINE);
        Matcher m = p.matcher(RegExTest.toSupplementaries("blah1\r\n"));

        Assert.assertTrue(m.find());

        Assert.assertFalse(m.find());
    }

    @Test public void
    anchorTest9() throws Exception {

        // Test for $ recognition of \u0085 for bug 4527731
        Pattern p = RegExTest.PF.compile(".+$", Pattern.MULTILINE);
        Matcher m = p.matcher(RegExTest.toSupplementaries("blah1\u0085"));

        Assert.assertTrue(m.find());
    }

    /**
     * A basic sanity test of Matcher.lookingAt().
     */
    @Test public void
    lookingAtTest1() throws Exception {
        Pattern p = RegExTest.PF.compile("(ab)(c*)");
        Matcher m = p.matcher("abccczzzabcczzzabccc");

        Assert.assertTrue(m.lookingAt());
        Assert.assertEquals(m.group(0), m.group());

        m = p.matcher("zzzabccczzzabcczzzabccczzz");
        Assert.assertFalse(m.lookingAt());
    }

    @Test public void
    lookingAtTest2() throws Exception {

        // Supplementary character test
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("(ab)(c*)"));
        Matcher m = p.matcher(RegExTest.toSupplementaries("abccczzzabcczzzabccc"));

        Assert.assertTrue(m.lookingAt());

        Assert.assertEquals(m.group(0), m.group());

        m = p.matcher(RegExTest.toSupplementaries("zzzabccczzzabcczzzabccczzz"));
        Assert.assertFalse(m.lookingAt());
    }

    /**
     * A basic sanity test of Matcher.matches().
     */
    @Test public void
    matchesTest1() throws Exception {
        // matches()
        Pattern p = RegExTest.PF.compile("ulb(c*)");
        Matcher m = p.matcher("ulbcccccc");
        Assert.assertTrue(m.matches());

        // find() but not matches()
        m.reset("zzzulbcccccc");
        Assert.assertFalse(m.matches());

        // lookingAt() but not matches()
        m.reset("ulbccccccdef");
        Assert.assertFalse(m.matches());
    }

    @Test public void
    matchesTest2() throws Exception {

        // matches()
        Pattern p = RegExTest.PF.compile("a|ad");
        Matcher m = p.matcher("ad");
        Assert.assertTrue(m.matches());
    }

    @Test public void
    matchesTest3() throws Exception {

        // Supplementary character test
        // matches()
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("ulb(c*)"));
        Matcher m = p.matcher(RegExTest.toSupplementaries("ulbcccccc"));
        Assert.assertTrue(m.matches());

        // find() but not matches()
        m.reset(RegExTest.toSupplementaries("zzzulbcccccc"));
        Assert.assertFalse(m.matches());

        // lookingAt() but not matches()
        m.reset(RegExTest.toSupplementaries("ulbccccccdef"));
        Assert.assertFalse(m.matches());
    }

    @Test public void
    matchesTest4() throws Exception {

        // matches()
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("a|ad"));
        Matcher m = p.matcher(RegExTest.toSupplementaries("ad"));
        Assert.assertTrue(m.matches());
    }

    /**
     * A basic sanity test of PF.matches().
     */
    @Test public void
    patternMatchesTest() throws Exception {
        // matches()
        if (!RegExTest.PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                             RegExTest.toSupplementaries("ulbcccccc")))
            Assert.fail();

        // find() but not matches()
        if (RegExTest.PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                            RegExTest.toSupplementaries("zzzulbcccccc")))
            Assert.fail();

        // lookingAt() but not matches()
        if (RegExTest.PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                            RegExTest.toSupplementaries("ulbccccccdef")))
            Assert.fail();

        // Supplementary character test
        // matches()
        if (!RegExTest.PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                             RegExTest.toSupplementaries("ulbcccccc")))
            Assert.fail();

        // find() but not matches()
        if (RegExTest.PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                            RegExTest.toSupplementaries("zzzulbcccccc")))
            Assert.fail();

        // lookingAt() but not matches()
        Assert.assertFalse(
            RegExTest.PF.matches(RegExTest.toSupplementaries("ulb(c*)"), RegExTest.toSupplementaries("ulbccccccdef"))
        );
    }

    /**
     * Canonical equivalence testing. Tests the ability of the engine
     * to match sequences that are not explicitly specified in the
     * pattern when they are considered equivalent by the Unicode Standard.
     */
    @Test public void
    ceTest1() throws Exception {

        if (RegExTest.LFR) return; // LFR does not support CANON_EQ.

        // Decomposed char outside char classes
        Pattern p = RegExTest.PF.compile("testa\u030a", Pattern.CANON_EQ);
        Matcher m = p.matcher("test\u00e5");
        Assert.assertTrue(m.matches());

        m.reset("testa\u030a");
        Assert.assertTrue(m.matches());
    }

    @Test public void
    ceTest2() throws Exception {

        if (RegExTest.LFR) return; // LFR does not support CANON_EQ.

        // Composed char outside char classes
        Pattern p = RegExTest.PF.compile("test\u00e5", Pattern.CANON_EQ);
        Matcher m = p.matcher("test\u00e5");
        Assert.assertTrue(m.matches());

        m.reset("testa\u030a");
        Assert.assertTrue(m.find());
    }

    @Test public void
    ceTest3() throws Exception {

        if (RegExTest.LFR) return; // LFR does not support CANON_EQ.

        // Decomposed char inside a char class
        Pattern p = RegExTest.PF.compile("test[abca\u030a]", Pattern.CANON_EQ);
        Matcher m = p.matcher("test\u00e5");
        Assert.assertTrue(m.find());

        m.reset("testa\u030a");
        Assert.assertTrue(m.find());
    }

    @Test public void
    ceTest4() throws Exception {

        if (RegExTest.LFR) return; // LFR does not support CANON_EQ.

        // Composed char inside a char class
        Pattern p = RegExTest.PF.compile("test[abc\u00e5def\u00e0]", Pattern.CANON_EQ);
        Matcher m = p.matcher("test\u00e5");
        Assert.assertTrue(m.find());

        m.reset("testa\u0300");
        Assert.assertTrue(m.find());

        m.reset("testa\u030a");
        Assert.assertTrue(m.find());
    }

    @Test public void
    ceTest5() throws Exception {

        if (RegExTest.LFR) return; // LFR does not support CANON_EQ.

        // Marks that cannot legally change order and be equivalent
        Pattern p = RegExTest.PF.compile("testa\u0308\u0300", Pattern.CANON_EQ);
        RegExTest.checkFind(p, "testa\u0308\u0300", true);
        RegExTest.checkFind(p, "testa\u0300\u0308", false);
    }

    @Test public void
    ceTest6() throws Exception {

        if (RegExTest.LFR) return; // LFR does not support CANON_EQ.

        // Marks that can legally change order and be equivalent
        Pattern p = RegExTest.PF.compile("testa\u0308\u0323", Pattern.CANON_EQ);
        RegExTest.checkFind(p, "testa\u0308\u0323", true);
        RegExTest.checkFind(p, "testa\u0323\u0308", true);
    }

    @Test public void
    ceTest7() throws Exception {

        if (RegExTest.LFR) return; // LFR does not support CANON_EQ.

        // Test all equivalences of the sequence a\u0308\u0323\u0300
        Pattern p = RegExTest.PF.compile("testa\u0308\u0323\u0300", Pattern.CANON_EQ);
        RegExTest.checkFind(p, "testa\u0308\u0323\u0300", true);
        RegExTest.checkFind(p, "testa\u0323\u0308\u0300", true);
        RegExTest.checkFind(p, "testa\u0308\u0300\u0323", true);
        RegExTest.checkFind(p, "test\u00e4\u0323\u0300", true);
        RegExTest.checkFind(p, "test\u00e4\u0300\u0323", true);

        /*
         * The following canonical equivalence tests don't work. Bug id: 4916384.
         *
        // Decomposed hangul (jamos)
        p = PF.compile("\u1100\u1161", Pattern.CANON_EQ);
        m = p.matcher("\u1100\u1161");
        if (!m.matches())
            failCount++;

        m.reset("\uac00");
        if (!m.matches())
            failCount++;

        // Composed hangul
        p = PF.compile("\uac00", Pattern.CANON_EQ);
        m = p.matcher("\u1100\u1161");
        if (!m.matches())
            failCount++;

        m.reset("\uac00");
        if (!m.matches())
            failCount++;

        // Decomposed supplementary outside char classes
        p = PF.compile("test\ud834\uddbc\ud834\udd6f", Pattern.CANON_EQ);
        m = p.matcher("test\ud834\uddc0");
        if (!m.matches())
            failCount++;

        m.reset("test\ud834\uddbc\ud834\udd6f");
        if (!m.matches())
            failCount++;

        // Composed supplementary outside char classes
        p = PF.compile("test\ud834\uddc0", Pattern.CANON_EQ);
        m.reset("test\ud834\uddbc\ud834\udd6f");
        if (!m.matches())
            failCount++;

        m = p.matcher("test\ud834\uddc0");
        if (!m.matches())
            failCount++;

        */
    }

    /**
     * A basic sanity test of Matcher.replaceAll().
     */
    @Test public void
    globalSubstitute1() throws Exception {
        // Global substitution with a literal
        Pattern p = RegExTest.PF.compile("(ab)(c*)");
        Matcher m = p.matcher("abccczzzabcczzzabccc");
        Assert.assertEquals("testzzztestzzztest", m.replaceAll("test"));

        m.reset("zzzabccczzzabcczzzabccczzz");
        Assert.assertEquals("zzztestzzztestzzztestzzz", m.replaceAll("test"));

        // Global substitution with groups
        m.reset("zzzabccczzzabcczzzabccczzz");
        String result = m.replaceAll("$1");
        Assert.assertEquals("zzzabzzzabzzzabzzz", result);
    }

    @Test public void
    globalSubstitute2() throws Exception {

        // Supplementary character test
        // Global substitution with a literal
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("(ab)(c*)"));
        Matcher m = p.matcher(RegExTest.toSupplementaries("abccczzzabcczzzabccc"));
        if (!m.replaceAll(RegExTest.toSupplementaries("test")).
            equals(RegExTest.toSupplementaries("testzzztestzzztest")))
            Assert.fail();

        m.reset(RegExTest.toSupplementaries("zzzabccczzzabcczzzabccczzz"));
        if (!m.replaceAll(RegExTest.toSupplementaries("test")).
            equals(RegExTest.toSupplementaries("zzztestzzztestzzztestzzz")))
            Assert.fail();

        // Global substitution with groups
        m.reset(RegExTest.toSupplementaries("zzzabccczzzabcczzzabccczzz"));
        String result = m.replaceAll("$1");
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabzzzabzzzabzzz"), result);
    }

    /**
     * Tests the usage of Matcher.appendReplacement() with literal
     * and group substitutions.
     */
    @Test public void
    stringbufferSubstitute1() throws Exception {

        // SB substitution with literal
        String blah = "zzzblahzzz";
        Pattern p = RegExTest.PF.compile("blah");
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        try {
            m.appendReplacement(result, "blech");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "blech");
        Assert.assertEquals("zzzblech", result.toString());

        m.appendTail(result);
        Assert.assertEquals("zzzblechzzz", result.toString());
    }

    @Test public void
    stringbufferSubstitute2() throws Exception {

        // SB substitution with groups
        String blah = "zzzabcdzzz";
        Pattern p = RegExTest.PF.compile("(ab)(cd)*");
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        Assert.assertEquals("zzzab", result.toString());

        m.appendTail(result);
        Assert.assertEquals("zzzabzzz", result.toString());
    }

    @Test public void
    stringbufferSubstitute3() throws Exception {

        // SB substitution with 3 groups
        String blah = "zzzabcdcdefzzz";
        Pattern p = RegExTest.PF.compile("(ab)(cd)*(ef)");
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        try {
            m.appendReplacement(result, "$1w$2w$3");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1w$2w$3");
        Assert.assertEquals("zzzabwcdwef", result.toString());

        m.appendTail(result);
        Assert.assertEquals("zzzabwcdwefzzz", result.toString());
    }

    @Test public void
    stringbufferSubstitute4() throws Exception {

        // SB substitution with groups and three matches
        // skipping middle match
        String blah = "zzzabcdzzzabcddzzzabcdzzz";
        Pattern p = RegExTest.PF.compile("(ab)(cd*)");
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        Assert.assertEquals("zzzab", result.toString());

        m.find();
        m.find();
        m.appendReplacement(result, "$2");
        Assert.assertEquals("zzzabzzzabcddzzzcd", result.toString());

        m.appendTail(result);
        Assert.assertEquals("zzzabzzzabcddzzzcdzzz", result.toString());
    }

    @Test public void
    stringbufferSubstitute5() throws Exception {

        // Check to make sure escaped $ is ignored
        String blah = "zzzabcdcdefzzz";
        Pattern p = RegExTest.PF.compile("(ab)(cd)*(ef)");
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        m.find();
        m.appendReplacement(result, "$1w\\$2w$3");
        Assert.assertEquals("zzzabw$2wef", result.toString());

        m.appendTail(result);
        Assert.assertEquals("zzzabw$2wefzzz", result.toString());
    }

    @Test public void
    stringbufferSubstitute6() throws Exception {

        // Check to make sure a reference to nonexistent group causes error
        String blah = "zzzabcdcdefzzz";
        Pattern p = RegExTest.PF.compile("(ab)(cd)*(ef)");
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        m.find();
        try {
            m.appendReplacement(result, "$1w$5w$3");
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Correct result
        }
    }

    @Test public void
    stringbufferSubstitute7() throws Exception {

        // Check double digit group references
        String blah = "zzz123456789101112zzz";
        Pattern p = RegExTest.PF.compile("(1)(2)(3)(4)(5)(6)(7)(8)(9)(10)(11)");
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        m.find();
        m.appendReplacement(result, "$1w$11w$3");
        Assert.assertEquals("zzz1w11w3", result.toString());
    }

    @Test public void
    stringbufferSubstitute8() throws Exception {

        // Check to make sure it backs off $15 to $1 if only three groups
        String blah = "zzzabcdcdefzzz";
        Pattern p = RegExTest.PF.compile("(ab)(cd)*(ef)");
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        m.find();
        m.appendReplacement(result, "$1w$15w$3");
        Assert.assertEquals("zzzabwab5wef", result.toString());
    }

    @Test public void
    stringbufferSubstitute9() throws Exception {

        // Supplementary character test
        // SB substitution with literal
        String blah = RegExTest.toSupplementaries("zzzblahzzz");
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("blah"));
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        try {
            m.appendReplacement(result, RegExTest.toSupplementaries("blech"));
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("blech"));
        Assert.assertEquals(RegExTest.toSupplementaries("zzzblech"), result.toString());

        m.appendTail(result);
        Assert.assertEquals(RegExTest.toSupplementaries("zzzblechzzz"), result.toString());
    }

    @Test public void
    stringbufferSubstitute10() throws Exception {

        // SB substitution with groups
        String blah = RegExTest.toSupplementaries("zzzabcdzzz");
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("(ab)(cd)*"));
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        Assert.assertEquals(RegExTest.toSupplementaries("zzzab"), result.toString());

        m.appendTail(result);
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabzzz"), result.toString());
    }

    @Test public void
    stringbufferSubstitute11() throws Exception {

        // SB substitution with 3 groups
        String blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        try {
            m.appendReplacement(result, RegExTest.toSupplementaries("$1w$2w$3"));
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w$2w$3"));
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabwcdwef"), result.toString());

        m.appendTail(result);
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabwcdwefzzz"), result.toString());
    }

    @Test public void
    stringbufferSubstitute12() throws Exception {

        // SB substitution with groups and three matches
        // skipping middle match
        String blah = RegExTest.toSupplementaries("zzzabcdzzzabcddzzzabcdzzz");
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("(ab)(cd*)"));
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        Assert.assertEquals(RegExTest.toSupplementaries("zzzab"), result.toString());

        m.find();
        m.find();
        m.appendReplacement(result, "$2");
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabzzzabcddzzzcd"), result.toString());

        m.appendTail(result);
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabzzzabcddzzzcdzzz"), result.toString());
    }

    @Test public void
    stringbufferSubstitute13() throws Exception {

        // Check to make sure escaped $ is ignored
        String blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w\\$2w$3"));
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabw$2wef"), result.toString());

        m.appendTail(result);
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabw$2wefzzz"), result.toString());
    }

    @Test public void
    stringbufferSubstitute14() throws Exception {

        // Check to make sure a reference to nonexistent group causes error
        String blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        m.find();
        try {
            m.appendReplacement(result, RegExTest.toSupplementaries("$1w$5w$3"));
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Correct result
        }
    }

    @Test public void
    stringbufferSubstitute15() throws Exception {

        // Check double digit group references
        String blah = RegExTest.toSupplementaries("zzz123456789101112zzz");
        Pattern p = RegExTest.PF.compile("(1)(2)(3)(4)(5)(6)(7)(8)(9)(10)(11)");
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w$11w$3"));
        Assert.assertEquals(RegExTest.toSupplementaries("zzz1w11w3"), result.toString());
    }

    @Test public void
    stringbufferSubstitute16() throws Exception {

        // Check to make sure it backs off $15 to $1 if only three groups
        String blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        Pattern p = RegExTest.PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w$15w$3"));
        Assert.assertEquals(RegExTest.toSupplementaries("zzzabwab5wef"), result.toString());
    }

    @Test public void
    stringbufferSubstitute17() throws Exception {

        // Check nothing has been appended into the output buffer if
        // the replacement string triggers IllegalArgumentException.
        Pattern p = RegExTest.PF.compile("(abc)");
        Matcher m = p.matcher("abcd");
        StringBuffer result = new StringBuffer();
        m.find();
        try {
            m.appendReplacement(result, ("xyz$g"));
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            Assert.assertEquals(0, result.length());
        }
    }

    /*
     * 5 groups of characters are created to make a substitution string.
     * A base string will be created including random lead chars, the
     * substitution string, and random trailing chars.
     * A pattern containing the 5 groups is searched for and replaced with:
     * random group + random string + random group.
     * The results are checked for correctness.
     */
    @Test public void
    substitutionBasher() {
        for (int runs = 0; runs<1000; runs++) {
            // Create a base string to work in
            int leadingChars = RegExTest.generator.nextInt(10);
            StringBuffer baseBuffer = new StringBuffer(100);
            String leadingString = RegExTest.getRandomAlphaString(leadingChars);
            baseBuffer.append(leadingString);

            // Create 5 groups of random number of random chars
            // Create the string to substitute
            // Create the pattern string to search for
            StringBuffer bufferToSub = new StringBuffer(25);
            StringBuffer bufferToPat = new StringBuffer(50);
            String[] groups = new String[5];
            for(int i=0; i<5; i++) {
                int aGroupSize = RegExTest.generator.nextInt(5)+1;
                groups[i] = RegExTest.getRandomAlphaString(aGroupSize);
                bufferToSub.append(groups[i]);
                bufferToPat.append('(');
                bufferToPat.append(groups[i]);
                bufferToPat.append(')');
            }
            String stringToSub = bufferToSub.toString();
            String pattern = bufferToPat.toString();

            // Place sub string into working string at random index
            baseBuffer.append(stringToSub);

            // Append random chars to end
            int trailingChars = RegExTest.generator.nextInt(10);
            String trailingString = RegExTest.getRandomAlphaString(trailingChars);
            baseBuffer.append(trailingString);
            String baseString = baseBuffer.toString();

            // Create test pattern and matcher
            Pattern p = RegExTest.PF.compile(pattern);
            Matcher m = p.matcher(baseString);

            // Reject candidate if pattern happens to start early
            m.find();
            if (m.start() < leadingChars)
                continue;

            // Reject candidate if more than one match
            if (m.find())
                continue;

            // Construct a replacement string with :
            // random group + random string + random group
            StringBuffer bufferToRep = new StringBuffer();
            int groupIndex1 = RegExTest.generator.nextInt(5);
            bufferToRep.append("$" + (groupIndex1 + 1));
            String randomMidString = RegExTest.getRandomAlphaString(5);
            bufferToRep.append(randomMidString);
            int groupIndex2 = RegExTest.generator.nextInt(5);
            bufferToRep.append("$" + (groupIndex2 + 1));
            String replacement = bufferToRep.toString();

            // Do the replacement
            String result = m.replaceAll(replacement);

            // Construct expected result
            StringBuffer bufferToRes = new StringBuffer();
            bufferToRes.append(leadingString);
            bufferToRes.append(groups[groupIndex1]);
            bufferToRes.append(randomMidString);
            bufferToRes.append(groups[groupIndex2]);
            bufferToRes.append(trailingString);
            String expectedResult = bufferToRes.toString();

            // Check results
            Assert.assertEquals(expectedResult, result);
        }
    }

    /**
     * Checks the handling of some escape sequences that the Pattern
     * class should process instead of the java compiler. These are
     * not in the file because the escapes should be be processed
     * by the Pattern class when the regex is compiled.
     */
    @Test public void
    escapes1() throws Exception {
        Pattern p = RegExTest.PF.compile("\\043");
        Matcher m = p.matcher("#");
        Assert.assertTrue(m.find());
    }

    @Test public void
    escapes2() throws Exception {

        Pattern p = RegExTest.PF.compile("\\x23");
        Matcher m = p.matcher("#");
        Assert.assertTrue(m.find());
    }

    @Test public void
    escapes3() throws Exception {

        Pattern p = RegExTest.PF.compile("\\u0023");
        Matcher m = p.matcher("#");
        Assert.assertTrue(m.find());
    }

    /**
     * Checks the handling of blank input situations. These
     * tests are incompatible with my test file format.
     */
    @Test public void
    blankInput1() throws Exception {
        Pattern p = RegExTest.PF.compile("abc", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher("");
        Assert.assertFalse(m.find());
    }

    @Test public void
    blankInput2() throws Exception {

        Pattern p = RegExTest.PF.compile("a*", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher("");
        Assert.assertTrue(m.find());
    }

    @Test public void
    blankInput3() throws Exception {

        Pattern p = RegExTest.PF.compile("abc");
        Matcher m = p.matcher("");
        Assert.assertFalse(m.find());
    }

    @Test public void
    blankInput4() throws Exception {

        Pattern p = RegExTest.PF.compile("a*");
        Matcher m = p.matcher("");
        Assert.assertTrue(m.find());
    }

    /**
     * Tests the Boyer-Moore pattern matching of a character sequence
     * on randomly generated patterns.
     */
    @Test public void
    bm1() throws Exception { RegExTest.doBnM('a'); }

    @Test public void
    bm2() throws Exception { RegExTest.doBnM(Character.MIN_SUPPLEMENTARY_CODE_POINT - 10); }

    private static void doBnM(int baseCharacter) throws Exception {

        Method method = Random.class.getDeclaredMethod("seedUniquifier");
        method.setAccessible(true);
//        long seed = (Long) method.invoke(null) ^ System.nanoTime();
//        System.out.println("seed=" + seed);
//        Random generator = new Random(seed);
//        Random generator = new Random();
        Random generator = new Random(3620582950354432161L);

        for (int i=0; i<100; i++) {
            // Create a short pattern to search for
            int patternLength = generator.nextInt(7) + 4;
            StringBuffer patternBuffer = new StringBuffer(patternLength);
            String pattern;
            retry: for (;;) {
                for (int x=0; x<patternLength; x++) {
                    int ch = baseCharacter + generator.nextInt(26);
                    if (Character.isSupplementaryCodePoint(ch)) {
                        patternBuffer.append(Character.toChars(ch));
                    } else {
                        patternBuffer.append((char)ch);
                    }
                }
                pattern = patternBuffer.toString();

                // Avoid patterns that start and end with the same substring
                // See JDK-6854417
                for (int x=1; x <patternLength; x++) {
                    if (pattern.startsWith(pattern.substring(x)))
                        continue retry;
                }
                break;
            }
            Pattern p = RegExTest.PF.compile(pattern);

            // Create a buffer with random ASCII chars that does
            // not match the sample
            String toSearch = null;
            StringBuffer s = null;
            Matcher m = p.matcher("");
            do {
                s = new StringBuffer(100);
                for (int x=0; x<100; x++) {
                    int ch = baseCharacter + generator.nextInt(26);
                    if (Character.isSupplementaryCodePoint(ch)) {
                        s.append(Character.toChars(ch));
                    } else {
                        s.append((char)ch);
                    }
                }
                toSearch = s.toString();
                m.reset(toSearch);
            } while (m.find());

            // Insert the pattern at a random spot
            int insertIndex = generator.nextInt(99);
            if (Character.isLowSurrogate(s.charAt(insertIndex)))
                insertIndex++;
            s = s.insert(insertIndex, pattern);
            toSearch = s.toString();

            // Make sure that the pattern is found
            m.reset(toSearch);
            Assert.assertTrue(m.find());

            // Make sure that the match text is the pattern
            Assert.assertEquals(pattern, m.group());

            // Make sure match occured at insertion point
            Assert.assertFalse(m.start() != insertIndex);
        }
    }

    /**
     * Tests the matching of slices on randomly generated patterns.
     * The Boyer-Moore optimization is not done on these patterns
     * because it uses unicode case folding.
     */
    @Test public void
    slice1() throws Exception { RegExTest.doSlice(Character.MAX_VALUE); }

    @Test public void
    slice2() throws Exception { RegExTest.doSlice(Character.MAX_CODE_POINT); }

    private static void doSlice(int maxCharacter) throws Exception {

//        Random generator = new Random();
        Random generator = new Random(123);

        for (int i=0; i<100; i++) {
            // Create a short pattern to search for
            int patternLength = generator.nextInt(7) + 4;
            StringBuffer patternBuffer = new StringBuffer(patternLength);
            for (int x=0; x<patternLength; x++) {
                int randomChar = 0;
                while (!Character.isLetterOrDigit(randomChar))
                    randomChar = generator.nextInt(maxCharacter);
                if (Character.isSupplementaryCodePoint(randomChar)) {
                    patternBuffer.append(Character.toChars(randomChar));
                } else {
                    patternBuffer.append((char) randomChar);
                }
            }
            String pattern =  patternBuffer.toString();
            Pattern p = RegExTest.PF.compile(pattern, Pattern.UNICODE_CASE);

            // Create a buffer with random chars that does not match the sample
            String toSearch = null;
            StringBuffer s = null;
            Matcher m = p.matcher("");
            do {
                s = new StringBuffer(100);
                for (int x=0; x<100; x++) {
                    int randomChar = 0;
                    while (!Character.isLetterOrDigit(randomChar))
                        randomChar = generator.nextInt(maxCharacter);
                    if (Character.isSupplementaryCodePoint(randomChar)) {
                        s.append(Character.toChars(randomChar));
                    } else {
                        s.append((char) randomChar);
                    }
                }
                toSearch = s.toString();
                m.reset(toSearch);
            } while (m.find());

            // Insert the pattern at a random spot
            int insertIndex = generator.nextInt(99);
            if (Character.isLowSurrogate(s.charAt(insertIndex)))
                insertIndex++;
            s = s.insert(insertIndex, pattern);
            toSearch = s.toString();

            // Make sure that the pattern is found
            m.reset(toSearch);
            Assert.assertTrue(m.find());

            // Make sure that the match text is the pattern
            Assert.assertEquals(pattern, m.group());

            // Make sure match occured at insertion point
            Assert.assertFalse(m.start() != insertIndex);
        }
    }

    private static void
    explainFailure(
        String    resourceName,
        int       lineNumber,
        Pattern   pattern,
        String    subject,
        Throwable t
    ) {
        System.err.println("----------------------------------------");
        System.err.println(resourceName + ", line " + lineNumber);
        System.err.println("Pattern:  " + pattern);
        if (pattern instanceof de.unkrig.lfr.core.Pattern) {
            System.err.println("Sequence: " + ((de.unkrig.lfr.core.Pattern) pattern).sequenceToString());
        }
        System.err.println("Subject:  " + PrettyPrinter.toString(subject));
        t.printStackTrace(System.err);
    }

    // Testing examples from a file

    /**
     * Goes through the file "TestCases.txt" and creates many patterns
     * described in the file, matching the patterns against input lines in
     * the file, and comparing the results against the correct results
     * also found in the file. The file format is described in comments
     * at the head of the file.
     */
    private static void processFile(String fileName) throws Exception {
        File testCases = new File(System.getProperty("test.src", "."),
                                  fileName);




//        FileInputStream in = new FileInputStream(testCases);
        String resourceName = testCases.getName();
        LineNumberReader lnr = new LineNumberReader(
            new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName))
        );

        // Process next test case.
        @SuppressWarnings("unused")
        String aLine;
        while((aLine = lnr.readLine()) != null) {

            String patternString  = RegExTest.grabLine(lnr);

            for (;;) {
                if (patternString.startsWith("FIX_HIT_END ")) {
                    int i;
                    for (i = 11; i < patternString.length() && Character.isWhitespace(patternString.charAt(i)); i++);
                    System.setProperty("FIX_HIT_END", patternString.substring(i));
                } else
                if (patternString.startsWith("FIX_REQUIRE_END ")) {
                    int i;
                    for (i = 15; i < patternString.length() && Character.isWhitespace(patternString.charAt(i)); i++);
                    System.setProperty("FIX_REQUIRE_END", patternString.substring(i));
                } else
                {
                    break;
                }
                patternString  = RegExTest.grabLine(lnr);
            }

            String dataString     = RegExTest.grabLine(lnr);
            String expectedResult = RegExTest.grabLine(lnr);

            try {

                Pattern p;
                try {
                    p = RegExTest.compileTestPattern(patternString);
                } catch (PatternSyntaxException e) {
                    if (expectedResult.startsWith("error"))
                        continue;
                    RegExTest.explainFailure(resourceName, lnr.getLineNumber(), null, dataString, e);
                    Assert.fail();
                    continue;
                }

                // Read a line for input string
                Matcher m = p.matcher(dataString);
                StringBuffer result = new StringBuffer();

                // Check for IllegalStateExceptions before a match
                Assert.assertEquals(0, RegExTest.preMatchInvariants(m));

                boolean found = m.find();

                if (found)
                    Assert.assertEquals(0, RegExTest.postTrueMatchInvariants(m));
                else
                    Assert.assertEquals(0, RegExTest.postFalseMatchInvariants(m));

                if (found) {
                    result.append("true ");
                    result.append(m.group(0) + " ");
                } else {
                    result.append("false ");
                }

                result.append(m.groupCount());

                if (found) {
                    for (int i = 1; i <= m.groupCount(); i++) {
                        if (m.group(i) != null) result.append(" " + m.group(i));
                    }
                }

                Assert.assertEquals("result", expectedResult, result.toString());

                if (RegExTest.PF instanceof FunctionalityEquivalencePatternFactory) {
                    Assert.assertNull("Long FIX_HIT_END system property value",     System.getProperty("FIX_HIT_END"));
                    Assert.assertNull("Long FIX_REQUIRE_END system property value", System.getProperty("FIX_REQUIRE_END"));
                }
            } catch (RuntimeException re) {
                throw ExceptionUtil.wrap(resourceName + ":" + lnr.getLineNumber(), re);
            } catch (Error e) {
                throw ExceptionUtil.wrap(resourceName + ":" + lnr.getLineNumber(), e);
            }
        }
    }

    private static int preMatchInvariants(Matcher m) {
        int failCount = 0;
        try {
            m.start();
            failCount++;
        } catch (IllegalStateException ise) {}
        try {
            m.end();
            failCount++;
        } catch (IllegalStateException ise) {}
        try {
            m.group();
            failCount++;
        } catch (IllegalStateException ise) {}
        return failCount;
    }

    private static int postFalseMatchInvariants(Matcher m) {
        int failCount = 0;
        try {
            m.group();
            failCount++;
        } catch (IllegalStateException ise) {}
        try {
            m.start();
            failCount++;
        } catch (IllegalStateException ise) {}
        try {
            m.end();
            failCount++;
        } catch (IllegalStateException ise) {}
        return failCount;
    }

    private static int postTrueMatchInvariants(Matcher m) {
        int failCount = 0;
        //assert(m.start() = m.start(0);
        if (m.start() != m.start(0))
            failCount++;
        //assert(m.end() = m.end(0);
        if (m.start() != m.start(0))
            failCount++;
        //assert(m.group() = m.group(0);
        if (!m.group().equals(m.group(0)))
            failCount++;
        try {
            m.group(50);
            failCount++;
        } catch (IndexOutOfBoundsException ise) {}

        return failCount;
    }

    private static Pattern compileTestPattern(String patternString) {
        if (!patternString.startsWith("'")) {
            return RegExTest.PF.compile(patternString);
        }

        int break1 = patternString.lastIndexOf("'");
        String flagString = patternString.substring(
                                          break1+1, patternString.length());
        patternString = patternString.substring(1, break1);

        if (flagString.equals("i"))
            return RegExTest.PF.compile(patternString, Pattern.CASE_INSENSITIVE);

        if (flagString.equals("m"))
            return RegExTest.PF.compile(patternString, Pattern.MULTILINE);

        return RegExTest.PF.compile(patternString);
    }

    /**
     * Reads a line from the input file. Keeps reading lines until a non
     * empty non comment line is read. If the line contains a \n then
     * these two characters are replaced by a newline char. If a \\uxxxx
     * sequence is read then the sequence is replaced by the unicode char.
     */
    private static String grabLine(BufferedReader r) throws Exception {
        int index = 0;
        String line = r.readLine();
        while (line.startsWith("//") || line.length() < 1)
            line = r.readLine();
        while ((index = line.indexOf("\\n")) != -1) {
            StringBuffer temp = new StringBuffer(line);
            temp.replace(index, index+2, "\n");
            line = temp.toString();
        }
        while ((index = line.indexOf("\\u")) != -1) {
            StringBuffer temp = new StringBuffer(line);
            String value = temp.substring(index+2, index+6);
            char aChar = (char)Integer.parseInt(value, 16);
            String unicodeChar = "" + aChar;
            temp.replace(index, index+6, unicodeChar);
            line = temp.toString();
        }

        return line;
    }

    private static void check(Pattern p, String s, String g, String expected) {

        Matcher m = p.matcher(s);

        Assert.assertTrue(m.find());

        Assert.assertEquals(expected,                               m.group(g));
        Assert.assertEquals(expected.charAt(0),                     s.charAt(m.start(g)));
        Assert.assertEquals(expected.charAt(expected.length() - 1), s.charAt(m.end(g) - 1));
    }

    private static void checkReplaceFirst(String p, String s, String r, String expected)
    {
        if (!expected.equals(RegExTest.PF.compile(p)
                                    .matcher(s)
                                    .replaceFirst(r)))
            Assert.fail();
    }

    private static void checkReplaceAll(String p, String s, String r, String expected)
    {
        if (!expected.equals(RegExTest.PF.compile(p)
                                    .matcher(s)
                                    .replaceAll(r)))
            Assert.fail();
    }

    private static void
    expectPatternSyntaxException(String p) {
        try {
            RegExTest.PF.compile(p);
            Assert.fail();
        } catch (PatternSyntaxException pse) {
            ;
        }
    }

    private static void checkExpectedIAE(Matcher m, String g) {
        m.find();
        try {
            m.group(g);
        } catch (IllegalArgumentException x) {
            try {
                m.start(g);
            } catch (IllegalArgumentException xx) {
                try {
                    m.start(g);
                } catch (IllegalArgumentException xxx) {
                    return;
                }
            }
        }
        Assert.fail();
    }

    private static void checkExpectedNPE(Matcher m) {
        m.find();
        try {
            m.group(null);
        } catch (NullPointerException x) {
            try {
                m.start(null);
            } catch (NullPointerException xx) {
                try {
                    m.end(null);
                } catch (NullPointerException xxx) {
                    return;
                }
            }
        }
        Assert.fail();
    }

    @Test public void namedGroupCaptureTest1() throws Exception { RegExTest.check(RegExTest.PF.compile("x+(?<gname>y+)z+"),  "xxxyyyzzz", "gname",  "yyy"); }
    @Test public void namedGroupCaptureTest2() throws Exception { RegExTest.check(RegExTest.PF.compile("x+(?<gname8>y+)z+"), "xxxyyyzzz", "gname8", "yyy"); }
    //backref
    @Test public void namedGroupCaptureTest3() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("(a*)bc\\1"),                                           "zzzaabcazzz",  true); }
    @Test public void namedGroupCaptureTest4() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("(?<gname>a*)bc\\k<gname>"),                            "zzzaabcaazzz", true); }
    @Test public void namedGroupCaptureTest5() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("(?<gname>abc)(def)\\k<gname>"),                        "abcdefabc",    true); }
    @Test public void namedGroupCaptureTest6() throws Exception { RegExTest.checkFind(RegExTest.PF.compile("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(?<gname>k)\\k<gname>"), "abcdefghijkk", true); }

    // Supplementary character tests
    @Test public void namedGroupCaptureTest7()  throws Exception { RegExTest.checkFind(RegExTest.PF.compile("(?<gname>" + RegExTest.toSupplementaries("a*)bc") + "\\k<gname>"),                                                              RegExTest.toSupplementaries("zzzaabcazzz"),  true); }
    @Test public void namedGroupCaptureTest8()  throws Exception { RegExTest.checkFind(RegExTest.PF.compile("(?<gname>" + RegExTest.toSupplementaries("a*)bc") + "\\k<gname>"),                                                              RegExTest.toSupplementaries("zzzaabcaazzz"), true); }
    @Test public void namedGroupCaptureTest9()  throws Exception { RegExTest.checkFind(RegExTest.PF.compile("(?<gname>" + RegExTest.toSupplementaries("abc)(def)") + "\\k<gname>"),                                                          RegExTest.toSupplementaries("abcdefabc"),    true); }
    @Test public void namedGroupCaptureTest10() throws Exception { RegExTest.checkFind(RegExTest.PF.compile(RegExTest.toSupplementaries("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)") + "(?<gname>" + RegExTest.toSupplementaries("k)") + "\\k<gname>"), RegExTest.toSupplementaries("abcdefghijkk"), true); }
    @Test public void namedGroupCaptureTest11() throws Exception { RegExTest.check    (RegExTest.PF.compile("x+(?<gname>y+)z+\\k<gname>"),                                                                                                   "xxxyyyzzzyyy", "gname", "yyy"); }

    //replaceFirst/All
    @Test public void namedGroupCaptureTest12() throws Exception { RegExTest.checkReplaceFirst("(?<gn>ab)(c*)",                                                                                      "abccczzzabcczzzabccc",                              "${gn}",  "abzzzabcczzzabccc");                               }
    @Test public void namedGroupCaptureTest13() throws Exception { RegExTest.checkReplaceAll  ("(?<gn>ab)(c*)",                                                                                      "abccczzzabcczzzabccc",                              "${gn}",  "abzzzabzzzab");                                    }
    @Test public void namedGroupCaptureTest14() throws Exception { RegExTest.checkReplaceFirst("(?<gn>ab)(c*)",                                                                                      "zzzabccczzzabcczzzabccczzz",                        "${gn}",  "zzzabzzzabcczzzabccczzz");                         }
    @Test public void namedGroupCaptureTest15() throws Exception { RegExTest.checkReplaceAll  ("(?<gn>ab)(c*)",                                                                                      "zzzabccczzzabcczzzabccczzz",                        "${gn}",  "zzzabzzzabzzzabzzz");                              }
    @Test public void namedGroupCaptureTest16() throws Exception { RegExTest.checkReplaceFirst("(?<gn1>ab)(?<gn2>c*)",                                                                               "zzzabccczzzabcczzzabccczzz",                        "${gn2}", "zzzccczzzabcczzzabccczzz");                        }
    @Test public void namedGroupCaptureTest17() throws Exception { RegExTest.checkReplaceAll  ("(?<gn1>ab)(?<gn2>c*)",                                                                               "zzzabccczzzabcczzzabccczzz",                        "${gn2}", "zzzccczzzcczzzccczzz");                            }
    @Test public void namedGroupCaptureTest18() throws Exception { RegExTest.checkReplaceFirst("(?<gn1>" + RegExTest.toSupplementaries("ab") + ")(?<gn2>" + RegExTest.toSupplementaries("c") + "*)", RegExTest.toSupplementaries("abccczzzabcczzzabccc"), "${gn1}", RegExTest.toSupplementaries("abzzzabcczzzabccc"));  }
    @Test public void namedGroupCaptureTest19() throws Exception { RegExTest.checkReplaceAll  ("(?<gn1>" + RegExTest.toSupplementaries("ab") + ")(?<gn2>" + RegExTest.toSupplementaries("c") + "*)", RegExTest.toSupplementaries("abccczzzabcczzzabccc"), "${gn1}", RegExTest.toSupplementaries("abzzzabzzzab"));       }
    @Test public void namedGroupCaptureTest20() throws Exception { RegExTest.checkReplaceFirst("(?<gn1>" + RegExTest.toSupplementaries("ab") + ")(?<gn2>" + RegExTest.toSupplementaries("c") + "*)", RegExTest.toSupplementaries("abccczzzabcczzzabccc"), "${gn2}", RegExTest.toSupplementaries("ccczzzabcczzzabccc")); }
    @Test public void namedGroupCaptureTest21() throws Exception { RegExTest.checkReplaceAll  ("(?<gn1>" + RegExTest.toSupplementaries("ab") + ")(?<gn2>" + RegExTest.toSupplementaries("c") + "*)", RegExTest.toSupplementaries("abccczzzabcczzzabccc"), "${gn2}", RegExTest.toSupplementaries("ccczzzcczzzccc"));     }
    @Test public void namedGroupCaptureTest22() throws Exception { RegExTest.checkReplaceFirst("(?<dog>Dog)AndCat",                                                                                  "zzzDogAndCatzzzDogAndCatzzz",                       "${dog}", "zzzDogzzzDogAndCatzzz");                           }
    @Test public void namedGroupCaptureTest23() throws Exception { RegExTest.checkReplaceAll  ("(?<dog>Dog)AndCat",                                                                                  "zzzDogAndCatzzzDogAndCatzzz",                       "${dog}", "zzzDogzzzDogzzz");                                 }

    // backref in Matcher & String
    @Test public void namedGroupCaptureTest24() throws Exception { Assert.assertEquals("abefij",   "abcdefghij".replaceFirst("cd(?<gn>ef)gh", "${gn}")); }
    @Test public void namedGroupCaptureTest25() throws Exception { Assert.assertEquals("abcdefgh", "abbbcbdbefgh".replaceAll("(?<gn>[a-e])b", "${gn}")); }

    // negative
    @Test public void namedGroupCaptureTest26() throws Exception { RegExTest.expectPatternSyntaxException("(?<groupnamehasnoascii.in>abc)(def)");    }
    @Test public void namedGroupCaptureTest27() throws Exception { RegExTest.expectPatternSyntaxException("(?<groupnamehasnoascii_in>abc)(def)");    }
    @Test public void namedGroupCaptureTest28() throws Exception { RegExTest.expectPatternSyntaxException("(?<6groupnamestartswithdigit>abc)(def)"); }
    @Test public void namedGroupCaptureTest29() throws Exception { RegExTest.expectPatternSyntaxException("(?<gname>abc)(def)\\k<gnameX>");          }
    @Test public void namedGroupCaptureTest30() throws Exception { RegExTest.expectPatternSyntaxException("(?<gname>abc)(?<gname>def)\\k<gnameX>");  }
    @Test public void namedGroupCaptureTest31() throws Exception { RegExTest.checkExpectedIAE(RegExTest.PF.compile("(?<gname>abc)(def)").matcher("abcdef"), "gnameX"); }
    @Test public void namedGroupCaptureTest32() throws Exception { RegExTest.checkExpectedNPE(RegExTest.PF.compile("(?<gname>abc)(def)").matcher("abcdef"));           }

    // This is for bug 6969132
    @Test public void
    nonBmpClassComplementTest1() throws Exception {
        Pattern p = RegExTest.PF.compile("\\P{Lu}");
        Matcher m = p.matcher(new String(new int[] {0x1d400}, 0, 1));
        Assert.assertFalse(m.find() && m.start() == 1);
    }

    @Test public void
    nonBmpClassComplementTest2() throws Exception {

        // from a unicode category
        Pattern p = RegExTest.PF.compile("\\P{Lu}");
        Matcher m = p.matcher(new String(new int[] {0x1d400}, 0, 1));
        Assert.assertFalse(m.find());
        Assert.assertTrue(m.hitEnd());
    }

    @Test public void
    nonBmpClassComplementTest3() throws Exception {

        // block
        Pattern p = RegExTest.PF.compile("\\P{InMathematicalAlphanumericSymbols}");
        Matcher m = p.matcher(new String(new int[] {0x1d400}, 0, 1));
        Assert.assertFalse(m.find() && m.start() == 1);
    }

    @Test public void unicodePropertiesTest1()  throws Exception { Assert.assertTrue(RegExTest.PF.compile("\\p{IsLu}"               ).matcher("A").matches()); }
    @Test public void unicodePropertiesTest2()  throws Exception { Assert.assertTrue(RegExTest.PF.compile("\\p{Lu}"                 ).matcher("A").matches()); }
    @Test public void unicodePropertiesTest3()  throws Exception { Assert.assertTrue(RegExTest.PF.compile("\\p{gc=Lu}"              ).matcher("A").matches()); }
    @Test public void unicodePropertiesTest4()  throws Exception { Assert.assertTrue(RegExTest.PF.compile("\\p{general_category=Lu}").matcher("A").matches()); }
    @Test public void unicodePropertiesTest5()  throws Exception { Assert.assertTrue(RegExTest.PF.compile("\\p{IsLatin}"            ).matcher("B").matches()); }
    @Test public void unicodePropertiesTest6()  throws Exception { Assert.assertTrue(RegExTest.PF.compile("\\p{sc=Latin}"           ).matcher("B").matches()); }
    @Test public void unicodePropertiesTest7()  throws Exception { Assert.assertTrue(RegExTest.PF.compile("\\p{script=Latin}"       ).matcher("B").matches()); }
    @Test public void unicodePropertiesTest8()  throws Exception { Assert.assertTrue(RegExTest.PF.compile("\\p{InBasicLatin}"       ).matcher("c").matches()); }
    @Test public void unicodePropertiesTest9()  throws Exception { Assert.assertTrue(RegExTest.PF.compile("\\p{blk=BasicLatin}"     ).matcher("c").matches()); }
    @Test public void unicodePropertiesTest10() throws Exception { Assert.assertTrue(RegExTest.PF.compile("\\p{block=BasicLatin}"   ).matcher("c").matches()); }

    @Test public void
    unicodePropertiesTest11() throws Exception {

    	Matcher common  = RegExTest.PF.compile("\\p{script=Common}").matcher("");
        Matcher lastSM  = common;
        Matcher unknown = RegExTest.PF.compile("\\p{IsUnknown}").matcher("");
        Character.UnicodeScript lastScript = Character.UnicodeScript.of(0);

        for (int cp = 1; cp < Character.MAX_CODE_POINT; cp += cp < 3000 ? 1 : 100) {
            if (cp >= 0x30000 && (cp & 0x70) == 0){
                continue;  // only pick couple code points, they are the same
            }

            // Unicode Script
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            Matcher m;
            String str = new String(Character.toChars(cp));
            if (script == lastScript) {
                 m = lastSM;
                 m.reset(str);
            } else {
                 m  = RegExTest.PF.compile("\\p{Is" + script.name() + "}").matcher(str);
            }
            if (!m.matches()) {
                Assert.fail();
            }
            Matcher other = (script == Character.UnicodeScript.COMMON)? unknown : common;
            other.reset(str);
            if (other.matches()) {
                Assert.fail();
            }
            lastSM = m;
            lastScript = script;
        }
    }

    @Test public void
    unicodePropertiesTest12() throws Exception {

        final Matcher latin  = RegExTest.PF.compile("\\p{block=basic_latin}").matcher("");
        final Matcher greek  = RegExTest.PF.compile("\\p{InGreek}").matcher("");

        Character.UnicodeBlock lastBlock = Character.UnicodeBlock.of(0);
        Matcher                lastBM    = latin;
        for (int cp = 1; cp < Character.MAX_CODE_POINT; cp += cp < 3000 ? 1 : 100) {
            if (cp >= 0x30000 && (cp & 0x70) == 0){
                continue;  // only pick couple code points, they are the same
            }

            // Unicode Block
            Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
            if (block == null) {
                //System.out.printf("Not a Block: cp=%x%n", cp);
                continue;
            }

            Matcher m;
            String str = new String(Character.toChars(cp));

            if (block == lastBlock) {
                 m = lastBM;
                 m.reset(str);
            } else {
                 m  = RegExTest.PF.compile("\\p{block=" + block.toString() + "}").matcher(str);
            }
            if (!m.matches()) {
                Assert.fail();
            }
            Matcher other = (block == Character.UnicodeBlock.BASIC_LATIN)? greek : latin;
            other.reset(str);
            if (other.matches()) {
                Assert.fail();
            }
            lastBM = m;
            lastBlock = block;
        }
    }

    @Test public void unicodeHexNotationTest1() throws Exception { RegExTest.expectPatternSyntaxException("\\x{-23}");    }
    @Test public void unicodeHexNotationTest2() throws Exception { RegExTest.expectPatternSyntaxException("\\x{110000}"); }
    @Test public void unicodeHexNotationTest3() throws Exception { RegExTest.expectPatternSyntaxException("\\x{}");       }
    @Test public void unicodeHexNotationTest4() throws Exception { RegExTest.expectPatternSyntaxException("\\x{AB[ef]");  }

    @Test public void unicodeHexNotationTest5()  throws Exception { RegExTest.check("^\\uD800\\uDF3c$",       "\uD800\uDF3C", true);  }
    @Test public void unicodeHexNotationTest6()  throws Exception { RegExTest.check("^\\x{1033c}$",           "\uD800\uDF3C", true);  }
    @Test public void unicodeHexNotationTest7()  throws Exception { RegExTest.check("^\\xF0\\x90\\x8C\\xBC$", "\uD800\uDF3C", false); }

    // Literal surrogates match only BARE (unpaired) surrogates, but not valid surrogate pairs!
    @Test public void unicodeHexNotationTest8()  throws Exception { RegExTest.check("^\\x{D800}\\x{DF3c}$",   "\uD800\uDF3C", false); }
    @Test public void unicodeHexNotationTest9()  throws Exception { RegExTest.check("^\\x{D800}",             "\uD800\uDF3C", false); }
    @Test public void unicodeHexNotationTest10() throws Exception { RegExTest.check("^\\x{D800}\\x{DF3c}+$",  "\uD800\uDF3C", false); }

    @Test public void unicodeHexNotationTest11() throws Exception { RegExTest.check("^[\\x{D800}\\x{DF3c}]+$",   "\uD800\uDF3C", false); }
    @Test public void unicodeHexNotationTest12() throws Exception { RegExTest.check("^[\\xF0\\x90\\x8C\\xBC]+$", "\uD800\uDF3C", false); }
    @Test public void unicodeHexNotationTest13() throws Exception { RegExTest.check("^[\\x{D800}\\x{DF3C}]+$",   "\uD800\uDF3C", false); }
    @Test public void unicodeHexNotationTest14() throws Exception { RegExTest.check("^[\\x{DF3C}\\x{D800}]+$",   "\uD800\uDF3C", false); }
    @Test public void unicodeHexNotationTest15() throws Exception { RegExTest.check("^[\\x{D800}\\x{DF3C}]+$",   "\uDF3C\uD800", true);  }

    @Test public void
    unicodeHexNotationTest16() throws Exception {
        RegExTest.check("^[\\x{DF3C}\\x{D800}]+$",   "\uDF3C\uD800", true);

        for (int cp = 0; cp <= 0x10FFFF; cp += 200) { // ++) {   /* AU */
             String s = "A" + new String(Character.toChars(cp)) + "B";

             String hexUTF16 = (
                 cp <= 0xFFFF
                 ? String.format("\\u%04x", cp)
                 : String.format(
                     "\\u%04x\\u%04x",
                     (int) Character.toChars(cp)[0],
                     (int) Character.toChars(cp)[1]
                 )
             );
             Assert.assertTrue(hexUTF16     + "/" + cp, RegExTest.PF.matches("A"  + hexUTF16     + "B",  s));
             Assert.assertTrue(hexUTF16     + "/" + cp, RegExTest.PF.matches("A[" + hexUTF16     + "]B", s));

             String hexCodePoint = "\\x{" + Integer.toHexString(cp) + "}";
             Assert.assertTrue(hexCodePoint + "/" + cp, RegExTest.PF.matches("A"  + hexCodePoint + "B",  s));
             Assert.assertTrue(hexCodePoint + "/" + cp, RegExTest.PF.matches("A[" + hexCodePoint + "]B", s));
         }
    }

    @Test public void
    unicodeClassesTest1() throws Exception {

        Matcher lower    = RegExTest.PF.compile("\\p{Lower}").matcher("");
        Matcher upper    = RegExTest.PF.compile("\\p{Upper}").matcher("");
        Matcher ASCII    = RegExTest.PF.compile("\\p{ASCII}").matcher("");
        Matcher alpha    = RegExTest.PF.compile("\\p{Alpha}").matcher("");
        Matcher digit    = RegExTest.PF.compile("\\p{Digit}").matcher("");
        Matcher alnum    = RegExTest.PF.compile("\\p{Alnum}").matcher("");
        Matcher punct    = RegExTest.PF.compile("\\p{Punct}").matcher("");
        Matcher graph    = RegExTest.PF.compile("\\p{Graph}").matcher("");
        Matcher print    = RegExTest.PF.compile("\\p{Print}").matcher("");
        Matcher blank    = RegExTest.PF.compile("\\p{Blank}").matcher("");
        Matcher cntrl    = RegExTest.PF.compile("\\p{Cntrl}").matcher("");
        Matcher xdigit   = RegExTest.PF.compile("\\p{XDigit}").matcher("");
        Matcher space    = RegExTest.PF.compile("\\p{Space}").matcher("");
        Matcher word     = RegExTest.PF.compile("\\w++").matcher("");
        // UNICODE_CHARACTER_CLASS
        Matcher lowerU   = RegExTest.PF.compile("\\p{Lower}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher upperU   = RegExTest.PF.compile("\\p{Upper}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher ASCIIU   = RegExTest.PF.compile("\\p{ASCII}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher alphaU   = RegExTest.PF.compile("\\p{Alpha}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher digitU   = RegExTest.PF.compile("\\p{Digit}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher alnumU   = RegExTest.PF.compile("\\p{Alnum}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher punctU   = RegExTest.PF.compile("\\p{Punct}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher graphU   = RegExTest.PF.compile("\\p{Graph}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher printU   = RegExTest.PF.compile("\\p{Print}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher blankU   = RegExTest.PF.compile("\\p{Blank}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher cntrlU   = RegExTest.PF.compile("\\p{Cntrl}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher xdigitU  = RegExTest.PF.compile("\\p{XDigit}", Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher spaceU   = RegExTest.PF.compile("\\p{Space}",  Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher boundU   = RegExTest.PF.compile("\\b",         Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher wordU    = RegExTest.PF.compile("\\w",         Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        // embedded flag (?U)
        Matcher lowerEU  = RegExTest.PF.compile("(?U)\\p{Lower}", Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher graphEU  = RegExTest.PF.compile("(?U)\\p{Graph}", Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher wordEU   = RegExTest.PF.compile("(?U)\\w",        Pattern.UNICODE_CHARACTER_CLASS).matcher("");

        Matcher bwb      = RegExTest.PF.compile("\\b\\w\\b").matcher("");
        Matcher bwbU     = RegExTest.PF.compile("\\b\\w++\\b",     Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        Matcher bwbEU    = RegExTest.PF.compile("(?U)\\b\\w++\\b", Pattern.UNICODE_CHARACTER_CLASS).matcher("");
        // properties
        Matcher lowerP   = RegExTest.PF.compile("\\p{IsLowerCase}").matcher("");
        Matcher upperP   = RegExTest.PF.compile("\\p{IsUpperCase}").matcher("");
        Matcher titleP   = RegExTest.PF.compile("\\p{IsTitleCase}").matcher("");
        Matcher letterP  = RegExTest.PF.compile("\\p{IsLetter}").matcher("");
        Matcher alphaP   = RegExTest.PF.compile("\\p{IsAlphabetic}").matcher("");
        Matcher ideogP   = RegExTest.PF.compile("\\p{IsIdeographic}").matcher("");
        Matcher cntrlP   = RegExTest.PF.compile("\\p{IsControl}").matcher("");
        Matcher spaceP   = RegExTest.PF.compile("\\p{IsWhiteSpace}").matcher("");
        Matcher definedP = RegExTest.PF.compile("\\p{IsAssigned}").matcher("");
        Matcher nonCCPP  = RegExTest.PF.compile("\\p{IsNoncharacterCodePoint}").matcher("");
        Matcher joinCrtl = RegExTest.PF.compile("\\p{IsJoinControl}").matcher("");

        // javaMethod
        Matcher lowerJ  = RegExTest.PF.compile("\\p{javaLowerCase}").matcher("");
        Matcher upperJ  = RegExTest.PF.compile("\\p{javaUpperCase}").matcher("");
        Matcher alphaJ  = RegExTest.PF.compile("\\p{javaAlphabetic}").matcher("");
        Matcher ideogJ  = RegExTest.PF.compile("\\p{javaIdeographic}").matcher("");

        for (int cp = 1; cp < 0x30000; cp += cp <= 1000 ? 1 : 100) {

            String subject = new String(Character.toChars(cp));
            int    type    = Character.getType(cp);
            String message = PrettyPrinter.codePointToJavaLiteral(cp);

            // lower
            this.assertMatches(message, POSIX_ASCII.isLower(cp),                   lower,   subject);
            this.assertMatches(message, Character.isLowerCase(cp),                 lowerU,  subject);
            this.assertMatches(message, Character.isLowerCase(cp),                 lowerP,  subject);
            this.assertMatches(message, Character.isLowerCase(cp),                 lowerEU, subject);
            this.assertMatches(message, Character.isLowerCase(cp),                 lowerJ,  subject);
            // upper
            this.assertMatches(message, POSIX_ASCII.isUpper(cp),                   upper,   subject);
            this.assertMatches(message, POSIX_Unicode.isUpper(cp),                 upperU,  subject);
            this.assertMatches(message, Character.isUpperCase(cp),                 upperP,  subject);
            this.assertMatches(message, Character.isUpperCase(cp),                 upperJ,  subject);
            // alpha
            this.assertMatches(message, POSIX_ASCII.isAlpha(cp),                   alpha,   subject);
            this.assertMatches(message, POSIX_Unicode.isAlpha(cp),                 alphaU,  subject);
            this.assertMatches(message, Character.isAlphabetic(cp),                alphaP,  subject);
            this.assertMatches(message, Character.isAlphabetic(cp),                alphaJ,  subject);
            // digit
            this.assertMatches(message, POSIX_ASCII.isDigit(cp),                   digit,   subject);
            this.assertMatches(message, Character.isDigit(cp),                     digitU,  subject);
            // alnum
            this.assertMatches(message, POSIX_ASCII.isAlnum(cp),                   alnum,   subject);
            this.assertMatches(message, POSIX_Unicode.isAlnum(cp),                 alnumU,  subject);
            // punct
            this.assertMatches(message, POSIX_ASCII.isPunct(cp),                   punct,   subject);
            this.assertMatches(message, POSIX_Unicode.isPunct(cp),                 punctU,  subject);
            // graph
            this.assertMatches(message, POSIX_ASCII.isGraph(cp),                   graph,   subject);
            this.assertMatches(message, POSIX_Unicode.isGraph(cp),                 graphU,  subject);
            this.assertMatches(message, POSIX_Unicode.isGraph(cp),                 graphEU, subject);
            // blank
            this.assertMatches(message, POSIX_ASCII.isType(cp, POSIX_ASCII.BLANK), blank,   subject);
            this.assertMatches(message, POSIX_Unicode.isBlank(cp),                 blankU,  subject);
            // print
            this.assertMatches(message, POSIX_ASCII.isPrint(cp),                   print,   subject);
            this.assertMatches(message, POSIX_Unicode.isPrint(cp),                 printU,  subject);
            // cntrl
            this.assertMatches(message, POSIX_ASCII.isCntrl(cp),                   cntrl,    subject);
            this.assertMatches(message, POSIX_Unicode.isCntrl(cp),                 cntrlU,   subject);
            this.assertMatches(message, Character.CONTROL == type,                 cntrlP,   subject);
            // hexdigit
            this.assertMatches(message, POSIX_ASCII.isHexDigit(cp),                xdigit,   subject);
            this.assertMatches(message, POSIX_Unicode.isHexDigit(cp),              xdigitU,  subject);
            // space
            this.assertMatches(message, POSIX_ASCII.isSpace(cp),                   space,    subject);
            this.assertMatches(message, POSIX_Unicode.isSpace(cp),                 spaceU,   subject);
            this.assertMatches(message, POSIX_Unicode.isSpace(cp),                 spaceP,   subject);
            // word
            this.assertMatches(message, POSIX_ASCII.isWord(cp),                    word,     subject);
            this.assertMatches(message, POSIX_Unicode.isWord(cp),                  wordU,    subject);
            this.assertMatches(message, POSIX_Unicode.isWord(cp),                  wordEU,   subject);
            // bwordb
            this.assertMatches(message, POSIX_ASCII.isWord(cp),                    bwb,      subject);
            this.assertMatches(message, POSIX_Unicode.isWord(cp),                  bwbU,     subject);
            // properties
            this.assertMatches(message, Character.isTitleCase(cp),                 titleP,   subject);
            this.assertMatches(message, Character.isLetter(cp),                    letterP,  subject);
            this.assertMatches(message, Character.isIdeographic(cp),               ideogP,   subject);
            this.assertMatches(message, Character.isIdeographic(cp),               ideogJ,   subject);
            this.assertMatches(message, type != Character.UNASSIGNED,              definedP, subject);
            this.assertMatches(message, POSIX_Unicode.isNoncharacterCodePoint(cp), nonCCPP,  subject);
            this.assertMatches(message, POSIX_Unicode.isJoinControl(cp),           joinCrtl, subject);
        }
    }

    public void assertMatches(String message, boolean expected, @Nullable Matcher matcher, String subject) {

        if (matcher == null) return;

        Assert.assertEquals(message, expected, matcher.reset(subject).matches());
    }

    // bounds/word align
    @Test public void
    unicodeClassesTest2() throws Exception {

        // 0x0180 == "LATIN SMALL LETTER B WITH STROKE"
        // 0x0400 == "CYRILLIV CAPITAL LETTER IE WITH GRAVE"
        RegExTest.twoFindIndexes(" \u0180sherman\u0400 ", RegExTest.PF.compile("\\b").matcher(""), 1, 10);
    }

    // bounds/word align
    @Test public void
    unicodeClassesTest3() throws Exception {

    	// 0x0180 == "LATIN SMALL LETTER B WITH STROKE"
        // 0x0345 == "COMBINING GREEK YPOGEGRAMMENI"
    	// 0x0400 == "CYRILLIV CAPITAL LETTER IE WITH GRAVE"
        RegExTest.twoFindIndexes(" \u0180sh\u0345erman\u0400 ", RegExTest.PF.compile("\\b").matcher(""), 1, 11);
    }

    // bounds/word align
    @Test public void
    unicodeClassesTest4() throws Exception {

        // 0x0724 == "SYRIAC LETTER FINAL SEMKATH"
        // 0x0739 == "SYRIAC DOTTED ZLEMA ANGULAR"
        RegExTest.twoFindIndexes(" \u0724\u0739\u0724 ", RegExTest.PF.compile("\\b").matcher(""), 1,  4);
    }

    @Test public void
    unicodeClassesTest5() throws Exception {

        Matcher bwbU  = RegExTest.PF.compile("\\b\\w++\\b", Pattern.UNICODE_CHARACTER_CLASS).matcher("");

        Assert.assertTrue(bwbU.reset("\u0180sherman\u0400"      ).matches());
        Assert.assertTrue(bwbU.reset("\u0180sh\u0345erman\u0400").matches());
    }

    @Test public void
    unicodeClassesTest6() throws Exception {

    	Matcher bwbU = RegExTest.PF.compile("\\b\\w++\\b",     Pattern.UNICODE_CHARACTER_CLASS).matcher("");
    	Assert.assertTrue(bwbU.reset("\u0724\u0739\u0724").matches());
    }

    @Test public void
    unicodeClassesTest7() throws Exception {

    	Matcher bwbEU = RegExTest.PF.compile("(?U)\\b\\w++\\b", Pattern.UNICODE_CHARACTER_CLASS).matcher("");
    	Assert.assertTrue(bwbEU.reset("\u0724\u0739\u0724").matches());
    }

    @Test public void
    horizontalAndVerticalWSTest() throws Exception {
        String hws = new String(new char[] {
            0x09, 0x20, 0xa0, 0x1680, 0x180e,
            0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200a,
            0x202f, 0x205f, 0x3000,
        });
        String vws = new String(new char[] { 0x0a, 0x0b, 0x0c, 0x0d, 0x85, 0x2028, 0x2029 });

        Assert.assertTrue (RegExTest.PF.compile("\\h+"  ).matcher(hws).matches());
        Assert.assertTrue (RegExTest.PF.compile("[\\h]+").matcher(hws).matches());
        Assert.assertFalse(RegExTest.PF.compile("\\H"   ).matcher(hws).find());
        Assert.assertFalse(RegExTest.PF.compile("[\\H]" ).matcher(hws).find());
        Assert.assertTrue (RegExTest.PF.compile("\\v+"  ).matcher(vws).matches());
        Assert.assertTrue (RegExTest.PF.compile("[\\v]+").matcher(vws).matches());
        Assert.assertFalse(RegExTest.PF.compile("\\V"   ).matcher(vws).find());
        Assert.assertFalse(RegExTest.PF.compile("[\\V]" ).matcher(vws).find());

        String prefix = "abcd";
        String suffix = "efgh";
        String ng     = "A";
        for (int i = 0; i < hws.length(); i++) {

            String infix = String.valueOf(hws.charAt(i));

            {
                Matcher m = RegExTest.PF.compile("\\h").matcher(prefix + infix + suffix);
                Assert.assertTrue(m.find());
                Assert.assertTrue(infix.equals(m.group()));
            }

            {
                Matcher m = RegExTest.PF.compile("[\\h]").matcher(prefix + infix + suffix);
                Assert.assertTrue(m.find());
                Assert.assertTrue(infix.equals(m.group()));
            }

            {
                Matcher m = RegExTest.PF.compile("\\H").matcher(hws.substring(0, i) + ng + hws.substring(i));
                Assert.assertTrue(m.find());
                Assert.assertTrue(ng.equals(m.group()));
            }

            {
                Matcher m = RegExTest.PF.compile("[\\H]").matcher(hws.substring(0, i) + ng + hws.substring(i));
                Assert.assertTrue(m.find());
                Assert.assertTrue(ng.equals(m.group()));
            }
        }
        for (int i = 0; i < vws.length(); i++) {

            String c = String.valueOf(vws.charAt(i));
            {
                Matcher m = RegExTest.PF.compile("\\v").matcher(prefix + c + suffix);
                Assert.assertTrue(m.find());
                Assert.assertTrue(c.equals(m.group()));
            }

            {
                Matcher m = RegExTest.PF.compile("[\\v]").matcher(prefix + c + suffix);
                Assert.assertTrue(m.find());
                Assert.assertTrue(c.equals(m.group()));
            }

            {
                Matcher m = RegExTest.PF.compile("\\V").matcher(vws.substring(0, i) + ng + vws.substring(i));
                Assert.assertTrue(m.find());
                Assert.assertTrue(ng.equals(m.group()));
            }

            {
                Matcher m = RegExTest.PF.compile("[\\V]").matcher(vws.substring(0, i) + ng + vws.substring(i));
                Assert.assertTrue(m.find());
                Assert.assertTrue(ng.equals(m.group()));
            }
        }

        // \v in range is interpreted as 0x0B. This is the undocumented behavior
        Assert.assertTrue(RegExTest.PF.compile("[\\v-\\v]").matcher(String.valueOf((char) 0x0B)).matches());
    }

    @Test public void
    linebreakTest1() throws Exception {

        String linebreaks = new String (new char[] { 0x0A, 0x0B, 0x0C, 0x0D, 0x85, 0x2028, 0x2029 });
        Assert.assertTrue(RegExTest.PF.compile("\\R+").matcher(linebreaks).matches());
    }

    @Test public void
    linebreakTest2() throws Exception {

        String crnl = "\r\n";
        Assert.assertTrue(RegExTest.PF.compile("\\R").matcher(crnl).matches());
        Assert.assertFalse(RegExTest.PF.compile("\\R\\R").matcher(crnl).matches());
    }

    // #7189363
    @Test public void
    branchTest() throws Exception {
        Assert.assertTrue(RegExTest.PF.compile("(a)?bc|d"  ).matcher("d" ).find());     // greedy
        Assert.assertTrue(RegExTest.PF.compile("(a)+bc|d"  ).matcher("d" ).find());
        Assert.assertTrue(RegExTest.PF.compile("(a)*bc|d"  ).matcher("d" ).find());
        Assert.assertTrue(RegExTest.PF.compile("(a)??bc|d" ).matcher("d" ).find());    // reluctant
        Assert.assertTrue(RegExTest.PF.compile("(a)+?bc|d" ).matcher("d" ).find());
        Assert.assertTrue(RegExTest.PF.compile("(a)*?bc|d" ).matcher("d" ).find());
        Assert.assertTrue(RegExTest.PF.compile("(a)?+bc|d" ).matcher("d" ).find());    // possessive
        Assert.assertTrue(RegExTest.PF.compile("(a)++bc|d" ).matcher("d" ).find());
        Assert.assertTrue(RegExTest.PF.compile("(a)*+bc|d" ).matcher("d" ).find());
        Assert.assertTrue(RegExTest.PF.compile("(a)?bc|d"  ).matcher("d" ).matches());  // greedy
        Assert.assertTrue(RegExTest.PF.compile("(a)+bc|d"  ).matcher("d" ).matches());
        Assert.assertTrue(RegExTest.PF.compile("(a)*bc|d"  ).matcher("d" ).matches());
        Assert.assertTrue(RegExTest.PF.compile("(a)??bc|d" ).matcher("d" ).matches()); // reluctant
        Assert.assertTrue(RegExTest.PF.compile("(a)+?bc|d" ).matcher("d" ).matches());
        Assert.assertTrue(RegExTest.PF.compile("(a)*?bc|d" ).matcher("d" ).matches());
        Assert.assertTrue(RegExTest.PF.compile("(a)?+bc|d" ).matcher("d" ).matches()); // possessive
        Assert.assertTrue(RegExTest.PF.compile("(a)++bc|d" ).matcher("d" ).matches());
        Assert.assertTrue(RegExTest.PF.compile("(a)*+bc|d" ).matcher("d" ).matches());
        Assert.assertTrue(RegExTest.PF.compile("(a)?bc|de" ).matcher("de").find());   // others
        Assert.assertTrue(RegExTest.PF.compile("(a)??bc|de").matcher("de").find());
        Assert.assertTrue(RegExTest.PF.compile("(a)?bc|de" ).matcher("de").matches());
    }

    // This test is for 8007395
    @Test public void
    groupCurlyNotFoundSuppTest() throws Exception {

        String input = "test this as \ud83d\ude0d";
        for (String pStr : new String[] {
            "test(.)+(@[a-zA-Z.]+)",
            "test(.)*(@[a-zA-Z.]+)",
            "test([^B])+(@[a-zA-Z.]+)",
            "test([^B])*(@[a-zA-Z.]+)",
            "test(\\P{IsControl})+(@[a-zA-Z.]+)",
            "test(\\P{IsControl})*(@[a-zA-Z.]+)",
        }) {
            Matcher m = RegExTest.PF.compile(pStr, Pattern.CASE_INSENSITIVE).matcher(input);
            try {
                if (m.find()) {
                    Assert.fail(pStr);
                }
            } catch (Exception x) {
                Assert.fail(pStr);
            }
        }
    }

    // This test is for 8023647
    @Test public void
    groupCurlyBackoffTest() throws Exception {

        if (!"abc1c".matches("(\\w)+1\\1") ||
            "abc11".matches("(\\w)+1\\1")) {
            Assert.fail();
        }
    }

//    // This test is for 8012646
//    @Test public
//    void patternAsPredicate() throws Exception {
//        Predicate<String> p = RegExTest.PF.compile("[a-z]+").asPredicate();
//
//        if (p.test("")) {
//            Assert.fail();
//        }
//        if (!p.test("word")) {
//            Assert.fail();
//        }
//        if (p.test("1234")) {
//            Assert.fail();
//        }
//        RegExTest.report("Pattern.asPredicate");
//    }
}
