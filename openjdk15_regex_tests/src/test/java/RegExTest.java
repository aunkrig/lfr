/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary tests RegExp framework (use -Dseed=X to set PRNG seed)
 * @author Mike McCloskey
 * @bug 4481568 4482696 4495089 4504687 4527731 4599621 4631553 4619345
 * 4630911 4672616 4711773 4727935 4750573 4792284 4803197 4757029 4808962
 * 4872664 4803179 4892980 4900747 4945394 4938995 4979006 4994840 4997476
 * 5013885 5003322 4988891 5098443 5110268 6173522 4829857 5027748 6376940
 * 6358731 6178785 6284152 6231989 6497148 6486934 6233084 6504326 6635133
 * 6350801 6676425 6878475 6919132 6931676 6948903 6990617 7014645 7039066
 * 7067045 7014640 7189363 8007395 8013252 8013254 8012646 8023647 6559590
 * 8027645 8035076 8039124 8035975 8074678 6854417 8143854 8147531 7071819
 * 8151481 4867170 7080302 6728861 6995635 6736245 4916384 6328855 6192895
 * 6345469 6988218 6693451 7006761 8140212 8143282 8158482 8176029 8184706
 * 8194667 8197462 8184692 8221431 8224789 8228352 8230829 8236034 8235812
 * 8216332 8214245 8237599 8241055
 *
 * @library /test/lib
 * @library /lib/testlibrary/java/lang
 * @build jdk.test.lib.RandomFactory
 * @run main RegExTest
 * @key randomness
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Predicate;
//import java.util.regex.Matcher;
import java.util.regex.MatchResult;
//import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.MatchResult;

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;

//import jdk.test.lib.RandomFactory;

/**
 * This is a test class created to check the operation of
 * the Pattern and Matcher classes.
 */
@SuppressWarnings("unused")
public class RegExTest {

	private static final PatternFactory PF = de.unkrig.lfr.core.PatternFactory.INSTANCE;
//	private static final PatternFactory PF = de.unkrig.ref4j.jur.PatternFactory.INSTANCE;

	private static final int JRE = getJreVersion();
	private static int getJreVersion() {
		String s = System.getProperty("java.specification.version");
		if (s.startsWith("1.")) s = s.substring(2);
		return Integer.parseInt(s);
	}

	private static Random generator = new Random(); // RandomFactory.getRandom();

    @Test public void
    testTestCasesTxt() throws Exception { RegExTest.processFile("TestCases.txt"); }

    @Test public void
    testBMPTestCasesTxt() throws Exception { RegExTest.processFile("BMPTestCases.txt"); }

    @Test public void
    testSupplementaryTestCasesTxt() throws Exception { RegExTest.processFile("SupplementaryTestCases.txt"); }

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
        if (!m.group().equals(expected))
            Assert.fail();
    }

    private static void check(Matcher m, String result, boolean expected) {
        m.find();
        if (m.group().equals(result) != expected)
            Assert.fail();
    }

    private static void check(Pattern p, String s, boolean expected) {
        if (p.matcher(s).find() != expected)
            Assert.fail();
    }

    private static void check(String p, String s, boolean expected) {
        Matcher matcher = PF.compile(p).matcher(s);
        if (matcher.find() != expected)
            Assert.fail();
    }

    private static void check(String p, char c, boolean expected) {
        String propertyPattern = expected ? "\\p" + p : "\\P" + p;
        Pattern pattern = PF.compile(propertyPattern);
        char[] ca = new char[1]; ca[0] = c;
        Matcher matcher = pattern.matcher(new String(ca));
        if (!matcher.find())
            Assert.fail();
    }

    private static void check(String p, int codePoint, boolean expected) {
        String propertyPattern = expected ? "\\p" + p : "\\P" + p;
        Pattern pattern = PF.compile(propertyPattern);
        char[] ca = Character.toChars(codePoint);
        Matcher matcher = pattern.matcher(new String(ca));
        if (!matcher.find())
            Assert.fail();
    }

    private static void check(String p, int flag, String input, String s,
                              boolean expected)
    {
        Pattern pattern = PF.compile(p, flag);
        Matcher matcher = pattern.matcher(input);
        if (expected)
            RegExTest.check(matcher, s, expected);
        else
            RegExTest.check(pattern, input, false);
    }

////    private static void report(String testName) {
////        int spacesToAdd = 30 - testName.length();
////        StringBuffer paddedNameBuffer = new StringBuffer(testName);
////        for (int i=0; i<spacesToAdd; i++)
////            paddedNameBuffer.append(" ");
////        String paddedName = paddedNameBuffer.toString();
////        System.err.println(paddedName + ": " +
////                           (RegExTest.failCount==0 ? "Passed":"Failed("+RegExTest.failCount+")"));
////        if (RegExTest.failCount > 0) {
//////            RegExTest.failure = true;
//////
//////            if (RegExTest.firstFailure == null) {
//////                RegExTest.firstFailure = testName;
//////            }
////            Assert.fail(testName + ": " + RegExTest.failCount + " failures");
////        }
////
////        RegExTest.failCount = 0;
////    }

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

    @Test public void nullArgumentTest() {
        RegExTest.check(() -> PF.compile(null));
        RegExTest.check(() -> PF.matches(null, null));
        RegExTest.check(() -> PF.matches("xyz", null));
        RegExTest.check(() -> PF.quote(null));
        RegExTest.check(() -> PF.compile("xyz").split(null));
        RegExTest.check(() -> PF.compile("xyz").matcher(null));

        final Matcher m = PF.compile("xyz").matcher("xyz");
        m.matches();
        RegExTest.check(() -> m.appendTail((StringBuffer) null));
        RegExTest.check(() -> m.appendTail((StringBuilder)null));
        RegExTest.check(() -> m.replaceAll((String) null));
//        RegExTest.check(() -> m.replaceAll((Function<MatchResult, String>)null));  SMC
        RegExTest.check(() -> m.replaceFirst((String)null));
//        RegExTest.check(() -> m.replaceFirst((Function<MatchResult, String>) null));   SMC
        RegExTest.check(() -> m.appendReplacement((StringBuffer)null, null));
        RegExTest.check(() -> m.appendReplacement((StringBuilder)null, null));
        RegExTest.check(() -> m.reset(null));
        RegExTest.check(() -> PF.quoteReplacement(null));
        //check(() -> m.usePattern(null));

////        RegExTest.report("Null Argument");
    }

    // This is for bug6635133
    // Test if surrogate pair in Unicode escapes can be handled correctly.
    @Test public void surrogatesInClassTest() throws Exception {
        Pattern pattern = PF.compile("[\\ud834\\udd21-\\ud834\\udd24]");
        Matcher matcher = pattern.matcher("\ud834\udd22");
        if (!matcher.find())
            Assert.fail();

////        RegExTest.report("Surrogate pair in Unicode escape");
    }

    // This is for bug6990617
    // Test if Pattern.RemoveQEQuoting works correctly if the octal unicode
    // char encoding is only 2 or 3 digits instead of 4 and the first quoted
    // char is an octal digit.
    @Test public void removeQEQuotingTest() throws Exception {
        Pattern pattern =
        		PF.compile("\\011\\Q1sometext\\E\\011\\Q2sometext\\E");
        Matcher matcher = pattern.matcher("\t1sometext\t2sometext");
        if (!matcher.find())
            Assert.fail();

////        RegExTest.report("Remove Q/E Quoting");
    }

    // This is for bug 4988891
    // Test toMatchResult to see that it is a copy of the Matcher
    // that is not affected by subsequent operations on the original
    @Test public void toMatchResultTest() throws Exception {
        Pattern pattern = PF.compile("squid");
        Matcher matcher = pattern.matcher(
            "agiantsquidofdestinyasmallsquidoffate");
        matcher.find();
        int matcherStart1 = matcher.start();
        MatchResult mr = matcher.toMatchResult();
        if (mr == matcher)
            Assert.fail();
        int resultStart1 = mr.start();
        if (matcherStart1 != resultStart1)
            Assert.fail();
        matcher.find();
        int matcherStart2 = matcher.start();
        int resultStart2 = mr.start();
        if (matcherStart2 == resultStart2)
            Assert.fail();
        if (resultStart1 != resultStart2)
            Assert.fail();
        MatchResult mr2 = matcher.toMatchResult();
        if (mr == mr2)
            Assert.fail();
        if (mr2.start() != matcherStart2)
            Assert.fail();
////        RegExTest.report("toMatchResult is a copy");
    }

    private static void checkExpectedISE(Runnable test) {
        try {
            test.run();
            Assert.fail();
        } catch (IllegalStateException x) {
        } catch (IndexOutOfBoundsException xx) {
            Assert.fail();
        }
    }

    private static void checkExpectedIOOE(Runnable test) {
        try {
            test.run();
            Assert.fail();
        } catch (IndexOutOfBoundsException x) {}
    }

    // This is for bug 8074678
    // Test the result of toMatchResult throws ISE if no match is availble
    @Test public void toMatchResultTest2() throws Exception {
        Matcher matcher = PF.compile("nomatch").matcher("hello world");
        matcher.find();
        MatchResult mr = matcher.toMatchResult();

        RegExTest.checkExpectedISE(() -> mr.start());
        RegExTest.checkExpectedISE(() -> mr.start(2));
        RegExTest.checkExpectedISE(() -> mr.end());
        RegExTest.checkExpectedISE(() -> mr.end(2));
        RegExTest.checkExpectedISE(() -> mr.group());
        RegExTest.checkExpectedISE(() -> mr.group(2));

        matcher = PF.compile("(match)").matcher("there is a match");
        matcher.find();
        MatchResult mr2 = matcher.toMatchResult();
        RegExTest.checkExpectedIOOE(() -> mr2.start(2));
        RegExTest.checkExpectedIOOE(() -> mr2.end(2));
        RegExTest.checkExpectedIOOE(() -> mr2.group(2));

////        RegExTest.report("toMatchResult2 appropriate exceptions");
    }

    // This is for bug 5013885
    // Must test a slice to see if it reports hitEnd correctly
    @Test public void hitEndTest() throws Exception {

    	// Basic test of Slice node
        Matcher m = PF.compile("^squidattack").matcher("squack");
        m.find();
        Assert.assertFalse(m.hitEnd());
        
        m.reset("squid");
        m.find();
        Assert.assertTrue(m.hitEnd());

        // Test Slice, SliceA and SliceU nodes
        for (int i=0; i<3; i++) {
            int flags = 0;
            if (i==1) flags = Pattern.CASE_INSENSITIVE;
            if (i==2) flags = Pattern.UNICODE_CASE;
            m = PF.compile("^abc", flags).matcher("ad");
            m.find();
            Assert.assertFalse(m.hitEnd());

            m.reset("ab");
            m.find();
            Assert.assertTrue(m.hitEnd());
        }

        // Test Boyer-Moore node
        m = PF.compile("catattack").matcher("attack");
        m.find();
        Assert.assertTrue(m.hitEnd());

        m = PF.compile("catattack").matcher("attackattackattackcatatta");
        m.find();
        Assert.assertTrue(m.hitEnd());

        // 8184706: Matching u+0d at EOL against \R should hit-end
        m = PF.compile("...\\R").matcher("cat\n");
        m.find();
        Assert.assertFalse(m.hitEnd());

        m = PF.compile("...\\R").matcher("cat\r");
        m.find();
        Assert.assertTrue(m.hitEnd());

        m = PF.compile("...\\R").matcher("cat\r\n");
        m.find();
        Assert.assertFalse(m.hitEnd());
    }

    // This is for bug 4997476
    // It is weird code submitted by customer demonstrating a regression
    @Test public void wordSearchTest() throws Exception {
        String testString = new String("word1 word2 word3");
        Pattern p = PF.compile("\\b");
        Matcher m = p.matcher(testString);
        int position = 0;
        int start = 0;
        while (m.find(position)) {
            start = m.start();
            if (start == testString.length())
                break;
            if (m.find(start+1)) {
                position = m.start();
            } else {
                position = testString.length();
            }
            if (testString.substring(start, position).equals(" "))
                continue;
            if (!testString.substring(start, position-1).startsWith("word"))
                Assert.fail();
        }
////        RegExTest.report("Customer word search");
    }

    // This is for bug 4994840
    @Test public void caretAtEndTest() throws Exception {
        // Problem only occurs with multiline patterns
        // containing a beginning-of-line caret "^" followed
        // by an expression that also matches the empty string.
        Pattern pattern = PF.compile("^x?", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher("\r");
        matcher.find();
        matcher.find();
////        RegExTest.report("Caret at end");
    }

    // This test is for 4979006
    // Check to see if word boundary construct properly handles unicode
    // non spacing marks
    @Test public void unicodeWordBoundsTest() throws Exception {
        String spaces = "  ";
        String wordChar = "a";
        String nsm = "\u030a";

        assert (Character.getType('\u030a') == Character.NON_SPACING_MARK);

        Pattern pattern = PF.compile("\\b");

        // S=other B=word character N=non spacing mark .=word boundary
        // SS.BB.SS
        RegExTest.assertFindIndexes(spaces + wordChar + wordChar + spaces, pattern, 2, 4);
        // SS.BBN.SS
        RegExTest.assertFindIndexes(spaces + wordChar + wordChar + nsm + spaces, pattern, 2, 5);
        // SS.BN.SS
        RegExTest.assertFindIndexes(spaces + wordChar + nsm + spaces, pattern, 2, 4);
        // SS.BNN.SS
        RegExTest.assertFindIndexes(spaces + wordChar + nsm + nsm + spaces, pattern, 2, 5);
        // SSN.BB.SS
        RegExTest.assertFindIndexes(spaces + nsm + wordChar + wordChar + spaces, pattern, 3, 5);
        // SS.BNB.SS
        RegExTest.assertFindIndexes(spaces + wordChar + nsm + wordChar + spaces, pattern, 2, 5);
        // SSNNSS
        assertPatternMatches(false, pattern, spaces + nsm + nsm + spaces);
        // SSN.BBN.SS
        RegExTest.assertFindIndexes(spaces + nsm + wordChar + wordChar + nsm + spaces, pattern, 3, 6);

////        RegExTest.report("Unicode word boundary");
    }

    private static void
    assertFindIndexes(String subject, Pattern pattern, int... indexes) throws Exception {
        Matcher matcher = pattern.matcher(subject);
        for (int index : indexes) {
        	Assert.assertTrue(matcher.find());
        	Assert.assertEquals(index, index, matcher.start());
		}
        Assert.assertFalse(matcher.find());
    }

    // This test is for 6284152
    static void check(String regex, String input, String[] expected) {
        List<String> result = new ArrayList<String>();
        Pattern p = PF.compile(regex);
        Matcher m = p.matcher(input);
        while (m.find()) {
            result.add(m.group());
        }
        if (!Arrays.asList(expected).equals(result))
            Assert.fail();
    }

    @Test public void lookbehindTest() throws Exception {
        //Positive
        RegExTest.check("(?<=%.{0,5})foo\\d",
              "%foo1\n%bar foo2\n%bar  foo3\n%blahblah foo4\nfoo5",
              new String[]{"foo1", "foo2", "foo3"});

        //boundary at end of the lookbehind sub-regex should work consistently
        //with the boundary just after the lookbehind sub-regex
        RegExTest.check("(?<=.*\\b)foo", "abcd foo", new String[]{"foo"});
        RegExTest.check("(?<=.*)\\bfoo", "abcd foo", new String[]{"foo"});
        RegExTest.check("(?<!abc )\\bfoo", "abc foo", new String[0]);
        RegExTest.check("(?<!abc \\b)foo", "abc foo", new String[0]);

        //Negative
        RegExTest.check("(?<!%.{0,5})foo\\d",
              "%foo1\n%bar foo2\n%bar  foo3\n%blahblah foo4\nfoo5",
              new String[] {"foo4", "foo5"});

        //Positive greedy
        RegExTest.check("(?<=%b{1,4})foo", "%bbbbfoo", new String[] {"foo"});

        //Positive reluctant
        RegExTest.check("(?<=%b{1,4}?)foo", "%bbbbfoo", new String[] {"foo"});

        //supplementary
        RegExTest.check("(?<=%b{1,4})fo\ud800\udc00o", "%bbbbfo\ud800\udc00o",
              new String[] {"fo\ud800\udc00o"});
        RegExTest.check("(?<=%b{1,4}?)fo\ud800\udc00o", "%bbbbfo\ud800\udc00o",
              new String[] {"fo\ud800\udc00o"});
        RegExTest.check("(?<!%b{1,4})fo\ud800\udc00o", "%afo\ud800\udc00o",
              new String[] {"fo\ud800\udc00o"});
        RegExTest.check("(?<!%b{1,4}?)fo\ud800\udc00o", "%afo\ud800\udc00o",
              new String[] {"fo\ud800\udc00o"});
////        RegExTest.report("Lookbehind");
    }

    // This test is for 4938995
    // Check to see if weak region boundaries are transparent to
    // lookahead and lookbehind constructs
    @Test public void boundsTest() throws Exception {
        String fullMessage = "catdogcat";
        Pattern pattern = PF.compile("(?<=cat)dog(?=cat)");
        Matcher matcher = pattern.matcher("catdogca");
        matcher.useTransparentBounds(true);
        if (matcher.find())
            Assert.fail();
        matcher.reset("atdogcat");
        if (matcher.find())
            Assert.fail();
        matcher.reset(fullMessage);
        if (!matcher.find())
            Assert.fail();
        matcher.reset(fullMessage);
        matcher.region(0,9);
        if (!matcher.find())
            Assert.fail();
        matcher.reset(fullMessage);
        matcher.region(0,6);
        if (!matcher.find())
            Assert.fail();
        matcher.reset(fullMessage);
        matcher.region(3,6);
        if (!matcher.find())
            Assert.fail();
        matcher.useTransparentBounds(false);
        if (matcher.find())
            Assert.fail();

        // Negative lookahead/lookbehind
        pattern = PF.compile("(?<!cat)dog(?!cat)");
        matcher = pattern.matcher("dogcat");
        matcher.useTransparentBounds(true);
        matcher.region(0,3);
        if (matcher.find())
            Assert.fail();
        matcher.reset("catdog");
        matcher.region(3,6);
        if (matcher.find())
            Assert.fail();
        matcher.useTransparentBounds(false);
        matcher.reset("dogcat");
        matcher.region(0,3);
        if (!matcher.find())
            Assert.fail();
        matcher.reset("catdog");
        matcher.region(3,6);
        if (!matcher.find())
            Assert.fail();

////        RegExTest.report("Region bounds transparency");
    }

    // This test is for 4945394
    @Test public void findFromTest() throws Exception {
        String message = "This is 40 $0 message.";
        Pattern pat = PF.compile("\\$0");
        Matcher match = pat.matcher(message);
        Assert.assertTrue(match.find());
        Assert.assertFalse(match.find());
        Assert.assertFalse(match.find());
    }

    // This test is for 4872664 and 4892980
    @Test public void negatedCharClassTest() throws Exception {
        Pattern pattern = PF.compile("[^>]");
        Matcher matcher = pattern.matcher("\u203A");
        if (!matcher.matches())
            Assert.fail();
        pattern = PF.compile("[^fr]");
        matcher = pattern.matcher("a");
        if (!matcher.find())
            Assert.fail();
        matcher.reset("\u203A");
        if (!matcher.find())
            Assert.fail();
        String s = "for";
        String result[] = s.split("[^fr]");
        if (!result[0].equals("f"))
            Assert.fail();
        if (!result[1].equals("r"))
            Assert.fail();
        s = "f\u203Ar";
        result = s.split("[^fr]");
        if (!result[0].equals("f"))
            Assert.fail();
        if (!result[1].equals("r"))
            Assert.fail();

        // Test adding to bits, subtracting a node, then adding to bits again
        pattern = PF.compile("[^f\u203Ar]");
        matcher = pattern.matcher("a");
        if (!matcher.find())
            Assert.fail();
        matcher.reset("f");
        if (matcher.find())
            Assert.fail();
        matcher.reset("\u203A");
        if (matcher.find())
            Assert.fail();
        matcher.reset("r");
        if (matcher.find())
            Assert.fail();
        matcher.reset("\u203B");
        if (!matcher.find())
            Assert.fail();

        // Test subtracting a node, adding to bits, subtracting again
        pattern = PF.compile("[^\u203Ar\u203B]");
        matcher = pattern.matcher("a");
        if (!matcher.find())
            Assert.fail();
        matcher.reset("\u203A");
        if (matcher.find())
            Assert.fail();
        matcher.reset("r");
        if (matcher.find())
            Assert.fail();
        matcher.reset("\u203B");
        if (matcher.find())
            Assert.fail();
        matcher.reset("\u203C");
        if (!matcher.find())
            Assert.fail();

////        RegExTest.report("Negated Character Class");
    }

    // This test is for 4628291
    @Test public void toStringTest() throws Exception {
        Pattern pattern = PF.compile("b+");
        if (pattern.toString() != "b+")
            Assert.fail();
        Matcher matcher = pattern.matcher("aaabbbccc");
        String matcherString = matcher.toString(); // unspecified
        matcher.find();
        matcherString = matcher.toString(); // unspecified
        matcher.region(0,3);
        matcherString = matcher.toString(); // unspecified
        matcher.reset();
        matcherString = matcher.toString(); // unspecified
////        RegExTest.report("toString");
    }

    // This test is for 4808962
    @Test public void literalPatternTest() throws Exception {
        int flags = Pattern.LITERAL;

        Pattern pattern = PF.compile("abc\\t$^", flags);
        RegExTest.check(pattern, "abc\\t$^", true);

        pattern = PF.compile(PF.quote("abc\\t$^"));
        RegExTest.check(pattern, "abc\\t$^", true);

        pattern = PF.compile("\\Qa^$bcabc\\E", flags);
        RegExTest.check(pattern, "\\Qa^$bcabc\\E", true);
        RegExTest.check(pattern, "a^$bcabc", false);

        pattern = PF.compile("\\\\Q\\\\E");
        RegExTest.check(pattern, "\\Q\\E", true);

        pattern = PF.compile("\\Qabc\\Eefg\\\\Q\\\\Ehij");
        RegExTest.check(pattern, "abcefg\\Q\\Ehij", true);

        pattern = PF.compile("\\\\\\Q\\\\E");
        RegExTest.check(pattern, "\\\\\\\\", true);

        pattern = PF.compile(PF.quote("\\Qa^$bcabc\\E"));
        RegExTest.check(pattern, "\\Qa^$bcabc\\E", true);
        RegExTest.check(pattern, "a^$bcabc", false);

        pattern = PF.compile(PF.quote("\\Qabc\\Edef"));
        RegExTest.check(pattern, "\\Qabc\\Edef", true);
        RegExTest.check(pattern, "abcdef", false);

        pattern = PF.compile(PF.quote("abc\\Edef"));
        RegExTest.check(pattern, "abc\\Edef", true);
        RegExTest.check(pattern, "abcdef", false);

        pattern = PF.compile(PF.quote("\\E"));
        RegExTest.check(pattern, "\\E", true);

        pattern = PF.compile("((((abc.+?:)", flags);
        RegExTest.check(pattern, "((((abc.+?:)", true);

        flags |= Pattern.MULTILINE;

        pattern = PF.compile("^cat$", flags);
        RegExTest.check(pattern, "abc^cat$def", true);
        RegExTest.check(pattern, "cat", false);

        flags |= Pattern.CASE_INSENSITIVE;

        pattern = PF.compile("abcdef", flags);
        RegExTest.check(pattern, "ABCDEF", true);
        RegExTest.check(pattern, "AbCdEf", true);

        flags |= Pattern.DOTALL;

        pattern = PF.compile("a...b", flags);
        RegExTest.check(pattern, "A...b", true);
        RegExTest.check(pattern, "Axxxb", false);

        flags |= Pattern.CANON_EQ;

        try {
        	PF.compile("testa\u030a", flags);
        } catch (IllegalArgumentException iae) {
        	Assert.assertEquals("Unsupported flag 128", iae.getMessage());
        }
        RegExTest.check(pattern, "testa\u030a", false);
        RegExTest.check(pattern, "test\u00e5", false);

        // Supplementary character test
        flags = Pattern.LITERAL;

        pattern = PF.compile(RegExTest.toSupplementaries("abc\\t$^"), flags);
        RegExTest.check(pattern, RegExTest.toSupplementaries("abc\\t$^"), true);

        pattern = PF.compile(PF.quote(RegExTest.toSupplementaries("abc\\t$^")));
        RegExTest.check(pattern, RegExTest.toSupplementaries("abc\\t$^"), true);

        pattern = PF.compile(RegExTest.toSupplementaries("\\Qa^$bcabc\\E"), flags);
        RegExTest.check(pattern, RegExTest.toSupplementaries("\\Qa^$bcabc\\E"), true);
        RegExTest.check(pattern, RegExTest.toSupplementaries("a^$bcabc"), false);

        pattern = PF.compile(PF.quote(RegExTest.toSupplementaries("\\Qa^$bcabc\\E")));
        RegExTest.check(pattern, RegExTest.toSupplementaries("\\Qa^$bcabc\\E"), true);
        RegExTest.check(pattern, RegExTest.toSupplementaries("a^$bcabc"), false);

        pattern = PF.compile(PF.quote(RegExTest.toSupplementaries("\\Qabc\\Edef")));
        RegExTest.check(pattern, RegExTest.toSupplementaries("\\Qabc\\Edef"), true);
        RegExTest.check(pattern, RegExTest.toSupplementaries("abcdef"), false);

        pattern = PF.compile(PF.quote(RegExTest.toSupplementaries("abc\\Edef")));
        RegExTest.check(pattern, RegExTest.toSupplementaries("abc\\Edef"), true);
        RegExTest.check(pattern, RegExTest.toSupplementaries("abcdef"), false);

        pattern = PF.compile(RegExTest.toSupplementaries("((((abc.+?:)"), flags);
        RegExTest.check(pattern, RegExTest.toSupplementaries("((((abc.+?:)"), true);

        flags |= Pattern.MULTILINE;

        pattern = PF.compile(RegExTest.toSupplementaries("^cat$"), flags);
        RegExTest.check(pattern, RegExTest.toSupplementaries("abc^cat$def"), true);
        RegExTest.check(pattern, RegExTest.toSupplementaries("cat"), false);

        flags |= Pattern.DOTALL;

        // note: this is case-sensitive.
        pattern = PF.compile(RegExTest.toSupplementaries("a...b"), flags);
        RegExTest.check(pattern, RegExTest.toSupplementaries("a...b"), true);
        RegExTest.check(pattern, RegExTest.toSupplementaries("axxxb"), false);

        flags |= Pattern.CANON_EQ;

        String t = RegExTest.toSupplementaries("test");
        try {
        	PF.compile(t + "a\u030a", flags);
        } catch (IllegalArgumentException iae) {
        	Assert.assertEquals("Unsupported flag 128", iae.getMessage());
        }
        RegExTest.check(pattern, t + "a\u030a", false);
        RegExTest.check(pattern, t + "\u00e5", false);

////        RegExTest.report("Literal pattern");
    }

    // This test is for 4803179
    // This test is also for 4808962, replacement parts
    @Test public void literalReplacementTest() throws Exception {
        int flags = Pattern.LITERAL;

        Pattern pattern = PF.compile("abc", flags);
        Matcher matcher = pattern.matcher("zzzabczzz");
        String replaceTest = "$0";
        String result = matcher.replaceAll(replaceTest);
        if (!result.equals("zzzabczzz"))
            Assert.fail();

        matcher.reset();
        String literalReplacement = PF.quoteReplacement(replaceTest);
        result = matcher.replaceAll(literalReplacement);
        if (!result.equals("zzz$0zzz"))
            Assert.fail();

        matcher.reset();
        replaceTest = "\\t$\\$";
        literalReplacement = PF.quoteReplacement(replaceTest);
        result = matcher.replaceAll(literalReplacement);
        if (!result.equals("zzz\\t$\\$zzz"))
            Assert.fail();

        // Supplementary character test
        pattern = PF.compile(RegExTest.toSupplementaries("abc"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("zzzabczzz"));
        replaceTest = "$0";
        result = matcher.replaceAll(replaceTest);
        if (!result.equals(RegExTest.toSupplementaries("zzzabczzz")))
            Assert.fail();

        matcher.reset();
        literalReplacement = PF.quoteReplacement(replaceTest);
        result = matcher.replaceAll(literalReplacement);
        if (!result.equals(RegExTest.toSupplementaries("zzz$0zzz")))
            Assert.fail();

        matcher.reset();
        replaceTest = "\\t$\\$";
        literalReplacement = PF.quoteReplacement(replaceTest);
        result = matcher.replaceAll(literalReplacement);
        if (!result.equals(RegExTest.toSupplementaries("zzz\\t$\\$zzz")))
            Assert.fail();

        // IAE should be thrown if backslash or '$' is the last character
        // in replacement string
        try {
            "\uac00".replaceAll("\uac00", "$");
            Assert.fail();
        } catch (IllegalArgumentException iie) {
        } catch (Exception e) {
            Assert.fail();
        }
        try {
            "\uac00".replaceAll("\uac00", "\\");
            Assert.fail();
        } catch (IllegalArgumentException iie) {
        } catch (Exception e) {
            Assert.fail();
        }
////        RegExTest.report("Literal replacement");
    }

    // This test is for 4757029
    @Test public void
    regionTest1() throws Exception {
        Pattern pattern = PF.compile("abc");
        Matcher matcher = pattern.matcher("abcdefabc");

        matcher.region(0,9);
        if (!matcher.find())
            Assert.fail();
        if (!matcher.find())
            Assert.fail();
        matcher.region(0,3);
        if (!matcher.find())
            Assert.fail();
        matcher.region(3,6);
        if (matcher.find())
            Assert.fail();
        matcher.region(0,2);
        if (matcher.find())
            Assert.fail();

        RegExTest.expectRegionFail(matcher, 1, -1);
        RegExTest.expectRegionFail(matcher, -1, -1);
        RegExTest.expectRegionFail(matcher, -1, 1);
        RegExTest.expectRegionFail(matcher, 5, 3);
        RegExTest.expectRegionFail(matcher, 5, 12);
        RegExTest.expectRegionFail(matcher, 12, 12);

        pattern = PF.compile("^abc$");
        matcher = pattern.matcher("zzzabczzz");
        matcher.region(0,9);
        if (matcher.find())
            Assert.fail();
        matcher.region(3,6);
        if (!matcher.find())
            Assert.fail();
        matcher.region(3,6);
        matcher.useAnchoringBounds(false);
        if (matcher.find())
            Assert.fail();

        // Supplementary character test
        pattern = PF.compile(RegExTest.toSupplementaries("abc"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("abcdefabc"));
        matcher.region(0,9*2);
        if (!matcher.find())
            Assert.fail();
        if (!matcher.find())
            Assert.fail();
        matcher.region(0,3*2);
        if (!matcher.find())
            Assert.fail();
        matcher.region(1,3*2);
        if (matcher.find())
            Assert.fail();
        matcher.region(3*2,6*2);
        if (matcher.find())
            Assert.fail();
        matcher.region(0,2*2);
        if (matcher.find())
            Assert.fail();
        matcher.region(0,2*2+1);
        if (matcher.find())
            Assert.fail();

        RegExTest.expectRegionFail(matcher, 1*2, -1);
        RegExTest.expectRegionFail(matcher, -1, -1);
        RegExTest.expectRegionFail(matcher, -1, 1*2);
        RegExTest.expectRegionFail(matcher, 5*2, 3*2);
        RegExTest.expectRegionFail(matcher, 5*2, 12*2);
        RegExTest.expectRegionFail(matcher, 12*2, 12*2);

        pattern = PF.compile(RegExTest.toSupplementaries("^abc$"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("zzzabczzz"));
        matcher.region(0,9*2);
        if (matcher.find())
            Assert.fail();
        matcher.region(3*2,6*2);
        if (!matcher.find())
            Assert.fail();
        matcher.region(3*2+1,6*2);
        if (matcher.find())
            Assert.fail();
        matcher.region(3*2,6*2-1);
        if (matcher.find())
            Assert.fail();
        matcher.region(3*2,6*2);
        matcher.useAnchoringBounds(false);
        if (matcher.find())
            Assert.fail();
    }

    @Test public void
    regionTest2() throws Exception {

        // JDK-8230829
        Pattern pattern = PF.compile("\\ud800\\udc61");
        Matcher matcher = pattern.matcher("\ud800\udc61");
        matcher.region(0, 1);
        Assert.assertFalse("Matched a surrogate pair that crosses border of region", matcher.find());
        Assert.assertTrue("Expected to hit the end when matching a surrogate pair crossing region", matcher.hitEnd());

////        RegExTest.report("Regions");
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
    @Test public void escapedSegmentTest() throws Exception {

        Pattern pattern = PF.compile("\\Qdir1\\dir2\\E");
        RegExTest.check(pattern, "dir1\\dir2", true);

        pattern = PF.compile("\\Qdir1\\dir2\\\\E");
        RegExTest.check(pattern, "dir1\\dir2\\", true);

        pattern = PF.compile("(\\Qdir1\\dir2\\\\E)");
        RegExTest.check(pattern, "dir1\\dir2\\", true);

        // Supplementary character test
        pattern = PF.compile(RegExTest.toSupplementaries("\\Qdir1\\dir2\\E"));
        RegExTest.check(pattern, RegExTest.toSupplementaries("dir1\\dir2"), true);

        pattern = PF.compile(RegExTest.toSupplementaries("\\Qdir1\\dir2")+"\\\\E");
        RegExTest.check(pattern, RegExTest.toSupplementaries("dir1\\dir2\\"), true);

        pattern = PF.compile(RegExTest.toSupplementaries("(\\Qdir1\\dir2")+"\\\\E)");
        RegExTest.check(pattern, RegExTest.toSupplementaries("dir1\\dir2\\"), true);

////        RegExTest.report("Escaped segment");
    }

    // This test is for 4792284
    @Test public void nonCaptureRepetitionTest() throws Exception {
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
            Pattern p = PF.compile(patterns[i]);
            Matcher m = p.matcher(input);

            if (m.matches()) {
                if (!m.group(0).equals(input))
                    Assert.fail();
            } else {
                Assert.fail();
            }
        }

////        RegExTest.report("Non capturing repetition");
    }

    // This test is for 6358731
    @Test public void notCapturedGroupCurlyMatchTest() throws Exception {
        Pattern pattern = PF.compile("(abc)+|(abcd)+");
        Matcher matcher = pattern.matcher("abcd");
        if (!matcher.matches() ||
             matcher.group(1) != null ||
             !matcher.group(2).equals("abcd")) {
            Assert.fail();
        }
////        RegExTest.report("Not captured GroupCurly");
    }

    // This test is for 4706545
    @Test public void javaCharClassTest() throws Exception {
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

////        RegExTest.report("Java character classes");
    }

    // This test is for 4523620
    /*
    private static void numOccurrencesTest() throws Exception {
        Pattern pattern = PF.compile("aaa");

        if (pattern.numOccurrences("aaaaaa", false) != 2)
            Assert.fail();
        if (pattern.numOccurrences("aaaaaa", true) != 4)
            Assert.fail();

        pattern = PF.compile("^");
        if (pattern.numOccurrences("aaaaaa", false) != 1)
            Assert.fail();
        if (pattern.numOccurrences("aaaaaa", true) != 1)
            Assert.fail();

        report("Number of Occurrences");
    }
    */

    // This test is for 4776374
    @Test public void caretBetweenTerminatorsTest() throws Exception {
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

////        RegExTest.report("Caret between terminators");
    }

    // This test is for 4727935
    @Test public void dollarAtEndTest() throws Exception {
        int flags1 = Pattern.DOTALL;
        int flags2 = Pattern.DOTALL | Pattern.UNIX_LINES;
        int flags3 = Pattern.DOTALL | Pattern.MULTILINE;

        RegExTest.check("....$", flags1, "test\n", "test", true);
        RegExTest.check("....$", flags1, "test\r\n", "test", true);
        RegExTest.check(".....$", flags1, "test\n", "test\n", true);
        RegExTest.check(".....$", flags1, "test\u0085", "test\u0085", true);
        RegExTest.check("....$", flags1, "test\u0085", "test", true);

        RegExTest.check("....$", flags2, "test\n", "test", true);
        RegExTest.check(".....$", flags2, "test\n", "test\n", true);
        RegExTest.check(".....$", flags2, "test\u0085", "test\u0085", true);
        RegExTest.check("....$", flags2, "test\u0085", "est\u0085", true);

        RegExTest.check("....$.blah", flags3, "test\nblah", "test\nblah", true);
        RegExTest.check(".....$.blah", flags3, "test\n\nblah", "test\n\nblah", true);
        RegExTest.check("....$blah", flags3, "test\nblah", "!!!!", false);
        RegExTest.check(".....$blah", flags3, "test\nblah", "!!!!", false);

        // Supplementary character test
        String t = RegExTest.toSupplementaries("test");
        String b = RegExTest.toSupplementaries("blah");
        RegExTest.check("....$", flags1, t+"\n", t, true);
        RegExTest.check("....$", flags1, t+"\r\n", t, true);
        RegExTest.check(".....$", flags1, t+"\n", t+"\n", true);
        RegExTest.check(".....$", flags1, t+"\u0085", t+"\u0085", true);
        RegExTest.check("....$", flags1, t+"\u0085", t, true);

        RegExTest.check("....$", flags2, t+"\n", t, true);
        RegExTest.check(".....$", flags2, t+"\n", t+"\n", true);
        RegExTest.check(".....$", flags2, t+"\u0085", t+"\u0085", true);
        RegExTest.check("....$", flags2, t+"\u0085", RegExTest.toSupplementaries("est\u0085"), true);

        RegExTest.check("....$."+b, flags3, t+"\n"+b, t+"\n"+b, true);
        RegExTest.check(".....$."+b, flags3, t+"\n\n"+b, t+"\n\n"+b, true);
        RegExTest.check("....$"+b, flags3, t+"\n"+b, "!!!!", false);
        RegExTest.check(".....$"+b, flags3, t+"\n"+b, "!!!!", false);

////        RegExTest.report("Dollar at End");
    }

    // This test is for 4711773
    @Test public void multilineDollarTest() throws Exception {
        Pattern findCR = PF.compile("$", Pattern.MULTILINE);
        Matcher matcher = findCR.matcher("first bit\nsecond bit");
        matcher.find();
        if (matcher.start(0) != 9)
            Assert.fail();
        matcher.find();
        if (matcher.start(0) != 20)
            Assert.fail();

        // Supplementary character test
        matcher = findCR.matcher(RegExTest.toSupplementaries("first  bit\n second  bit")); // double BMP chars
        matcher.find();
        if (matcher.start(0) != 9*2)
            Assert.fail();
        matcher.find();
        if (matcher.start(0) != 20*2)
            Assert.fail();

////        RegExTest.report("Multiline Dollar");
    }

    @Test public void reluctantRepetitionTest() throws Exception {
        Pattern p = PF.compile("1(\\s\\S+?){1,3}?[\\s,]2");
        RegExTest.check(p, "1 word word word 2", true);
        RegExTest.check(p, "1 wor wo w 2", true);
        RegExTest.check(p, "1 word word 2", true);
        RegExTest.check(p, "1 word 2", true);
        RegExTest.check(p, "1 wo w w 2", true);
        RegExTest.check(p, "1 wo w 2", true);
        RegExTest.check(p, "1 wor w 2", true);

        p = PF.compile("([a-z])+?c");
        Matcher m = p.matcher("ababcdefdec");
        RegExTest.check(m, "ababc");

        // Supplementary character test
        p = PF.compile(RegExTest.toSupplementaries("([a-z])+?c"));
        m = p.matcher(RegExTest.toSupplementaries("ababcdefdec"));
        RegExTest.check(m, RegExTest.toSupplementaries("ababc"));

////        RegExTest.report("Reluctant Repetition");
    }

    private static Pattern serializedPattern(Pattern p) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(p);
        oos.close();
        try (ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()))) {
            return (Pattern)ois.readObject();
        }
    }

    @Test public void serializeTest() throws Exception {
        String patternStr = "(b)";
        String matchStr = "b";
        Pattern pattern = PF.compile(patternStr);
        Pattern serializedPattern = RegExTest.serializedPattern(pattern);
        Matcher matcher = serializedPattern.matcher(matchStr);
        if (!matcher.matches())
            Assert.fail();
        if (matcher.groupCount() != 1)
            Assert.fail();

        pattern = PF.compile("a(?-i)b", Pattern.CASE_INSENSITIVE);
        serializedPattern = RegExTest.serializedPattern(pattern);
        if (!serializedPattern.matcher("Ab").matches())
            Assert.fail();
        if (serializedPattern.matcher("AB").matches())
            Assert.fail();

////        RegExTest.report("Serialization");
    }

    @Test public void gTest() {
        Pattern pattern = PF.compile("\\G\\w");
        Matcher matcher = pattern.matcher("abc#x#x");
        matcher.find();
        matcher.find();
        matcher.find();
        if (matcher.find())
            Assert.fail();

        pattern = PF.compile("\\GA*");
        matcher = pattern.matcher("1A2AA3");
        matcher.find();
        if (matcher.find())
            Assert.fail();

        pattern = PF.compile("\\GA*");
        matcher = pattern.matcher("1A2AA3");
        if (!matcher.find(1))
            Assert.fail();
        matcher.find();
        if (matcher.find())
            Assert.fail();

////        RegExTest.report("\\G");
    }

    @Test public void zTest() {
        Pattern pattern = PF.compile("foo\\Z");
        // Positives
        RegExTest.check(pattern, "foo\u0085", true);
        RegExTest.check(pattern, "foo\u2028", true);
        RegExTest.check(pattern, "foo\u2029", true);
        RegExTest.check(pattern, "foo\n", true);
        RegExTest.check(pattern, "foo\r", true);
        RegExTest.check(pattern, "foo\r\n", true);
        // Negatives
        RegExTest.check(pattern, "fooo", false);
        RegExTest.check(pattern, "foo\n\r", false);

        pattern = PF.compile("foo\\Z", Pattern.UNIX_LINES);
        // Positives
        RegExTest.check(pattern, "foo", true);
        RegExTest.check(pattern, "foo\n", true);
        // Negatives
        RegExTest.check(pattern, "foo\r", false);
        RegExTest.check(pattern, "foo\u0085", false);
        RegExTest.check(pattern, "foo\u2028", false);
        RegExTest.check(pattern, "foo\u2029", false);

////        RegExTest.report("\\Z");
    }

    @Test public void replaceFirstTest() {
        Pattern pattern = PF.compile("(ab)(c*)");
        Matcher matcher = pattern.matcher("abccczzzabcczzzabccc");
        if (!matcher.replaceFirst("test").equals("testzzzabcczzzabccc"))
            Assert.fail();

        matcher.reset("zzzabccczzzabcczzzabccczzz");
        if (!matcher.replaceFirst("test").equals("zzztestzzzabcczzzabccczzz"))
            Assert.fail();

        matcher.reset("zzzabccczzzabcczzzabccczzz");
        String result = matcher.replaceFirst("$1");
        if (!result.equals("zzzabzzzabcczzzabccczzz"))
            Assert.fail();

        matcher.reset("zzzabccczzzabcczzzabccczzz");
        result = matcher.replaceFirst("$2");
        if (!result.equals("zzzccczzzabcczzzabccczzz"))
            Assert.fail();

        pattern = PF.compile("a*");
        matcher = pattern.matcher("aaaaaaaaaa");
        if (!matcher.replaceFirst("test").equals("test"))
            Assert.fail();

        pattern = PF.compile("a+");
        matcher = pattern.matcher("zzzaaaaaaaaaa");
        if (!matcher.replaceFirst("test").equals("zzztest"))
            Assert.fail();

        // Supplementary character test
        pattern = PF.compile(RegExTest.toSupplementaries("(ab)(c*)"));
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
        if (!result.equals(RegExTest.toSupplementaries("zzzabzzzabcczzzabccczzz")))
            Assert.fail();

        matcher.reset(RegExTest.toSupplementaries("zzzabccczzzabcczzzabccczzz"));
        result = matcher.replaceFirst("$2");
        if (!result.equals(RegExTest.toSupplementaries("zzzccczzzabcczzzabccczzz")))
            Assert.fail();

        pattern = PF.compile(RegExTest.toSupplementaries("a*"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("aaaaaaaaaa"));
        if (!matcher.replaceFirst(RegExTest.toSupplementaries("test")).equals(RegExTest.toSupplementaries("test")))
            Assert.fail();

        pattern = PF.compile(RegExTest.toSupplementaries("a+"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("zzzaaaaaaaaaa"));
        if (!matcher.replaceFirst(RegExTest.toSupplementaries("test")).equals(RegExTest.toSupplementaries("zzztest")))
            Assert.fail();

////        RegExTest.report("Replace First");
    }

    @Test public void unixLinesTest() {
        Pattern pattern = PF.compile(".*");
        Matcher matcher = pattern.matcher("aa\u2028blah");
        matcher.find();
        if (!matcher.group(0).equals("aa"))
            Assert.fail();

        pattern = PF.compile(".*", Pattern.UNIX_LINES);
        matcher = pattern.matcher("aa\u2028blah");
        matcher.find();
        if (!matcher.group(0).equals("aa\u2028blah"))
            Assert.fail();

        pattern = PF.compile("[az]$",
                                  Pattern.MULTILINE | Pattern.UNIX_LINES);
        matcher = pattern.matcher("aa\u2028zz");
        RegExTest.check(matcher, "a\u2028", false);

        // Supplementary character test
        pattern = PF.compile(".*");
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa\u2028blah"));
        matcher.find();
        if (!matcher.group(0).equals(RegExTest.toSupplementaries("aa")))
            Assert.fail();

        pattern = PF.compile(".*", Pattern.UNIX_LINES);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa\u2028blah"));
        matcher.find();
        if (!matcher.group(0).equals(RegExTest.toSupplementaries("aa\u2028blah")))
            Assert.fail();

        pattern = PF.compile(RegExTest.toSupplementaries("[az]$"),
                                  Pattern.MULTILINE | Pattern.UNIX_LINES);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa\u2028zz"));
        RegExTest.check(matcher, RegExTest.toSupplementaries("a\u2028"), false);

////        RegExTest.report("Unix Lines");
    }

    @Test public void commentsTest() {
        int flags = Pattern.COMMENTS;

        Pattern pattern = PF.compile("aa \\# aa", flags);
        Matcher matcher = pattern.matcher("aa#aa");
        if (!matcher.matches())
            Assert.fail();

        pattern = PF.compile("aa  # blah", flags);
        matcher = pattern.matcher("aa");
        if (!matcher.matches())
            Assert.fail();

        pattern = PF.compile("aa blah", flags);
        matcher = pattern.matcher("aablah");
        if (!matcher.matches())
             Assert.fail();

        pattern = PF.compile("aa  # blah blech  ", flags);
        matcher = pattern.matcher("aa");
        if (!matcher.matches())
            Assert.fail();

        pattern = PF.compile("aa  # blah\n  ", flags);
        matcher = pattern.matcher("aa");
        if (!matcher.matches())
            Assert.fail();

        pattern = PF.compile("aa  # blah\nbc # blech", flags);
        matcher = pattern.matcher("aabc");
        if (!matcher.matches())
             Assert.fail();

        pattern = PF.compile("aa  # blah\nbc# blech", flags);
        matcher = pattern.matcher("aabc");
        if (!matcher.matches())
             Assert.fail();

        pattern = PF.compile("aa  # blah\nbc\\# blech", flags);
        matcher = pattern.matcher("aabc#blech");
        if (!matcher.matches())
             Assert.fail();

        // Supplementary character test
        pattern = PF.compile(RegExTest.toSupplementaries("aa \\# aa"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa#aa"));
        if (!matcher.matches())
            Assert.fail();

        pattern = PF.compile(RegExTest.toSupplementaries("aa  # blah"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa"));
        if (!matcher.matches())
            Assert.fail();

        pattern = PF.compile(RegExTest.toSupplementaries("aa blah"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aablah"));
        if (!matcher.matches())
             Assert.fail();

        pattern = PF.compile(RegExTest.toSupplementaries("aa  # blah blech  "), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa"));
        if (!matcher.matches())
            Assert.fail();

        pattern = PF.compile(RegExTest.toSupplementaries("aa  # blah\n  "), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aa"));
        if (!matcher.matches())
            Assert.fail();

        pattern = PF.compile(RegExTest.toSupplementaries("aa  # blah\nbc # blech"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aabc"));
        if (!matcher.matches())
             Assert.fail();

        pattern = PF.compile(RegExTest.toSupplementaries("aa  # blah\nbc# blech"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aabc"));
        if (!matcher.matches())
             Assert.fail();

        pattern = PF.compile(RegExTest.toSupplementaries("aa  # blah\nbc\\# blech"), flags);
        matcher = pattern.matcher(RegExTest.toSupplementaries("aabc#blech"));
        if (!matcher.matches())
             Assert.fail();

////        RegExTest.report("Comments");
    }

    @Test public void caseFoldingTest() { // bug 4504687
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern pattern = PF.compile("aa", flags);
        Matcher matcher = pattern.matcher("ab");
        if (matcher.matches())
            Assert.fail();

        pattern = PF.compile("aA", flags);
        matcher = pattern.matcher("ab");
        if (matcher.matches())
            Assert.fail();

        pattern = PF.compile("aa", flags);
        matcher = pattern.matcher("aB");
        if (matcher.matches())
            Assert.fail();
        matcher = pattern.matcher("Ab");
        if (matcher.matches())
            Assert.fail();

        // ASCII               "a"
        // Latin-1 Supplement  "a" + grave
        // Cyrillic            "a"
        String[] patterns = new String[] {
            //single
            "a", "\u00e0", "\u0430",
            //slice
            "ab", "\u00e0\u00e1", "\u0430\u0431",
            //class single
            "[a]", "[\u00e0]", "[\u0430]",
            //class range
            "[a-b]", "[\u00e0-\u00e5]", "[\u0430-\u0431]",
            //back reference
            "(a)\\1", "(\u00e0)\\1", "(\u0430)\\1"
        };

        String[] texts = new String[] {
            "A", "\u00c0", "\u0410",
            "AB", "\u00c0\u00c1", "\u0410\u0411",
            "A", "\u00c0", "\u0410",
            "B", "\u00c2", "\u0411",
            "aA", "\u00e0\u00c0", "\u0430\u0410"
        };

        boolean[] expected = new boolean[] {
            true, false, false,
            true, false, false,
            true, false, false,
            true, false, false,
            true, false, false
        };

        flags = Pattern.CASE_INSENSITIVE;
        for (int i = 0; i < patterns.length; i++) {
            pattern = PF.compile(patterns[i], flags);
            matcher = pattern.matcher(texts[i]);
            if (matcher.matches() != expected[i]) {
                System.out.println("<1> Failed at " + i);
                Assert.fail();
            }
        }

        flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        for (int i = 0; i < patterns.length; i++) {
            pattern = PF.compile(patterns[i], flags);
            matcher = pattern.matcher(texts[i]);
            if (!matcher.matches()) {
                System.out.println("<2> Failed at " + i);
                Assert.fail();
            }
        }
        // flag unicode_case alone should do nothing
        flags = Pattern.UNICODE_CASE;
        for (int i = 0; i < patterns.length; i++) {
            pattern = PF.compile(patterns[i], flags);
            matcher = pattern.matcher(texts[i]);
            if (matcher.matches()) {
                System.out.println("<3> Failed at " + i);
                Assert.fail();
            }
        }

        // Special cases: i, I, u+0131 and u+0130
        flags = Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE;
        pattern = PF.compile("[h-j]+", flags);
        if (!pattern.matcher("\u0131\u0130").matches())
            Assert.fail();
////        RegExTest.report("Case Folding");
    }

    @Test public void appendTest() {
        Pattern pattern = PF.compile("(ab)(cd)");
        Matcher matcher = pattern.matcher("abcd");
        String result = matcher.replaceAll("$2$1");
        if (!result.equals("cdab"))
            Assert.fail();

        String  s1 = "Swap all: first = 123, second = 456";
        String  s2 = "Swap one: first = 123, second = 456";
        String  r  = "$3$2$1";
        pattern = PF.compile("([a-z]+)( *= *)([0-9]+)");
        matcher = pattern.matcher(s1);

        result = matcher.replaceAll(r);
        if (!result.equals("Swap all: 123 = first, 456 = second"))
            Assert.fail();

        matcher = pattern.matcher(s2);

        if (matcher.find()) {
            StringBuffer sb = new StringBuffer();
            matcher.appendReplacement(sb, r);
            matcher.appendTail(sb);
            result = sb.toString();
            if (!result.equals("Swap one: 123 = first, second = 456"))
                Assert.fail();
        }

        // Supplementary character test
        pattern = PF.compile(RegExTest.toSupplementaries("(ab)(cd)"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("abcd"));
        result = matcher.replaceAll("$2$1");
        if (!result.equals(RegExTest.toSupplementaries("cdab")))
            Assert.fail();

        s1 = RegExTest.toSupplementaries("Swap all: first = 123, second = 456");
        s2 = RegExTest.toSupplementaries("Swap one: first = 123, second = 456");
        r  = RegExTest.toSupplementaries("$3$2$1");
        pattern = PF.compile(RegExTest.toSupplementaries("([a-z]+)( *= *)([0-9]+)"));
        matcher = pattern.matcher(s1);

        result = matcher.replaceAll(r);
        if (!result.equals(RegExTest.toSupplementaries("Swap all: 123 = first, 456 = second")))
            Assert.fail();

        matcher = pattern.matcher(s2);

        if (matcher.find()) {
            StringBuffer sb = new StringBuffer();
            matcher.appendReplacement(sb, r);
            matcher.appendTail(sb);
            result = sb.toString();
            if (!result.equals(RegExTest.toSupplementaries("Swap one: 123 = first, second = 456")))
                Assert.fail();
        }
////        RegExTest.report("Append");
    }

    @Test public void splitTest() {
    	
    	{
	        Pattern pattern  = PF.compile(":");
	        Pattern patternX = PF.compile(RegExTest.toSupplementaries("X"));
	        
	        {
		        String[] result = pattern.split("foo:and:boo", 2);
		        if (!result[0].equals("foo"))
		            Assert.fail();
		        if (!result[1].equals("and:boo"))
		            Assert.fail();
		        // Supplementary character test
		        result = patternX.split(RegExTest.toSupplementaries("fooXandXboo"), 2);
		        if (!result[0].equals(RegExTest.toSupplementaries("foo")))
		            Assert.fail();
		        if (!result[1].equals(RegExTest.toSupplementaries("andXboo")))
		            Assert.fail();
	        }

	    	{
		        CharBuffer cb = CharBuffer.allocate(100);
		        cb.put("foo:and:boo");
		        cb.flip();
		        String[] result = pattern.split(cb);
		        if (!result[0].equals("foo"))
		            Assert.fail();
		        if (!result[1].equals("and"))
		            Assert.fail();
		        if (!result[2].equals("boo"))
		            Assert.fail();
	    	}
	
	        // Supplementary character test
	    	{
		        CharBuffer cbs = CharBuffer.allocate(100);
		        cbs.put(RegExTest.toSupplementaries("fooXandXboo"));
		        cbs.flip();
		        String[] result = patternX.split(cbs);
		        if (!result[0].equals(RegExTest.toSupplementaries("foo")))
		            Assert.fail();
		        if (!result[1].equals(RegExTest.toSupplementaries("and")))
		            Assert.fail();
		        if (!result[2].equals(RegExTest.toSupplementaries("boo")))
		            Assert.fail();
	    	}
    	}

    	{
	        String source = "0123456789";
	        for (int limit=-2; limit<3; limit++) {
	            for (int x=0; x<10; x++) {
	                String[] result = source.split(Integer.toString(x), limit);
	                int expectedLength = limit < 1 ? 2 : limit;
	
	                if ((limit == 0) && (x == 9)) {
	                    // expected dropping of ""
	                    if (result.length != 1)
	                        Assert.fail();
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
	                        if (!result[1].equals(source.substring(x+1,10)))
	                            Assert.fail();
	                    }
                    }
                }
            }
	
	    	// Check the case for no match found
	        for (int limit=-2; limit<3; limit++) {
	            String[] result = source.split("e", limit);
	            if (result.length != 1)
	                Assert.fail();
	            if (!result[0].equals(source))
	                Assert.fail();
	        }

	    	// Check the case for limit == 0, source = "";
	        // split() now returns 0-length for empty source "" see #6559590
	        source = "";
	        String[] result = source.split("e", 0);
	        if (result.length != 1)
	            Assert.fail();
	        if (!result[0].equals(source))
	            Assert.fail();
    	}

        // Check both split() and splitAsStraem(), especially for zero-lenth
        // input and zero-lenth match cases
        String[][] input = new String[][] {
            { " ",           "Abc Efg Hij" },   // normal non-zero-match
            { " ",           " Abc Efg Hij" },  // leading empty str for non-zero-match
            { " ",           "Abc  Efg Hij" },  // non-zero-match in the middle
            { "(?=\\p{Lu})", "AbcEfgHij" },     // no leading empty str for zero-match
            { "(?=\\p{Lu})", "AbcEfg" },
            { "(?=\\p{Lu})", "Abc" },
            { " ",           "" },              // zero-length input
            { ".*",          "" },

            // some tests from PatternStreamTest.java
            { "4",       "awgqwefg1fefw4vssv1vvv1" },
            { "\u00a3a", "afbfq\u00a3abgwgb\u00a3awngnwggw\u00a3a\u00a3ahjrnhneerh" },
            { "1",       "awgqwefg1fefw4vssv1vvv1" },
            { "1",       "a\u4ebafg1fefw\u4eba4\u9f9cvssv\u9f9c1v\u672c\u672cvv" },
            { "\u56da",  "1\u56da23\u56da456\u56da7890" },
            { "\u56da",  "1\u56da23\u9f9c\u672c\u672c\u56da456\u56da\u9f9c\u672c7890" },
            { "\u56da",  "" },
            { "[ \t,:.]","This is,testing: with\tdifferent separators." }, //multiple septs
            { "o",       "boo:and:foo" },
            { "o",       "booooo:and:fooooo" },
            { "o",       "fooooo:" },
        };

        String[][] expected = new String[][] {
            { "Abc", "Efg", "Hij" },
            { "", "Abc", "Efg", "Hij" },
            { "Abc", "", "Efg", "Hij" },
            { "Abc", "Efg", "Hij" },
            { "Abc", "Efg" },
            { "Abc" },
            { "" },
            { "" },

            { "awgqwefg1fefw", "vssv1vvv1" },
            { "afbfq", "bgwgb", "wngnwggw", "", "hjrnhneerh" },
            { "awgqwefg", "fefw4vssv", "vvv" },
            { "a\u4ebafg", "fefw\u4eba4\u9f9cvssv\u9f9c", "v\u672c\u672cvv" },
            { "1", "23", "456", "7890" },
            { "1", "23\u9f9c\u672c\u672c", "456", "\u9f9c\u672c7890" },
            { "" },
            { "This", "is", "testing", "", "with", "different", "separators" },
            { "b", "", ":and:f" },
            { "b", "", "", "", "", ":and:f" },
            { "f", "", "", "", "", ":" },
        };
        for (int i = 0; i < input.length; i++) {

        	Pattern pattern = PF.compile(input[i][0]);
        	String  input2  = input[i][1];

			String[] expected2 = expected[i];
			Assert.assertArrayEquals(expected2, pattern.split(input2));

			if (input[i][1].length() > 0) {

				// splitAsStream() return empty resulting array for zero-length input for now

				Object[] actual = pattern.splitAsStream(input2).toArray();
				Assert.assertArrayEquals(
					"#" + i + ": pattern=\"" + pattern + "\", input=\"" + input2 + "\", expected=" + Arrays.deepToString(expected2) + ", actual=" + Arrays.deepToString(actual),
					expected2,
					actual
				);
			}
        }
    }

    @Test public void negationTest() {
        Pattern pattern = PF.compile("[\\[@^]+");
        Matcher matcher = pattern.matcher("@@@@[[[[^^^^");
        if (!matcher.find())
            Assert.fail();
        if (!matcher.group(0).equals("@@@@[[[[^^^^"))
            Assert.fail();
        pattern = PF.compile("[@\\[^]+");
        matcher = pattern.matcher("@@@@[[[[^^^^");
        if (!matcher.find())
            Assert.fail();
        if (!matcher.group(0).equals("@@@@[[[[^^^^"))
            Assert.fail();
        pattern = PF.compile("[@\\[^@]+");
        matcher = pattern.matcher("@@@@[[[[^^^^");
        if (!matcher.find())
            Assert.fail();
        if (!matcher.group(0).equals("@@@@[[[[^^^^"))
            Assert.fail();

        pattern = PF.compile("\\)");
        matcher = pattern.matcher("xxx)xxx");
        if (!matcher.find())
            Assert.fail();

////        RegExTest.report("Negation");
    }

    @Test public void ampersandTest() {
        Pattern pattern = PF.compile("[&@]+");
        RegExTest.check(pattern, "@@@@&&&&", true);

        pattern = PF.compile("[@&]+");
        RegExTest.check(pattern, "@@@@&&&&", true);

        pattern = PF.compile("[@\\&]+");
        RegExTest.check(pattern, "@@@@&&&&", true);

////        RegExTest.report("Ampersand");
    }

    @Test public void octalTest() throws Exception {
        Pattern pattern = PF.compile("\\u0007");
        Matcher matcher = pattern.matcher("\u0007");
        if (!matcher.matches())
            Assert.fail();
        pattern = PF.compile("\\07");
        matcher = pattern.matcher("\u0007");
        if (!matcher.matches())
            Assert.fail();
        pattern = PF.compile("\\007");
        matcher = pattern.matcher("\u0007");
        if (!matcher.matches())
            Assert.fail();
        pattern = PF.compile("\\0007");
        matcher = pattern.matcher("\u0007");
        if (!matcher.matches())
            Assert.fail();
        pattern = PF.compile("\\040");
        matcher = pattern.matcher("\u0020");
        if (!matcher.matches())
            Assert.fail();
        pattern = PF.compile("\\0403");
        matcher = pattern.matcher("\u00203");
        if (!matcher.matches())
            Assert.fail();
        pattern = PF.compile("\\0103");
        matcher = pattern.matcher("\u0043");
        if (!matcher.matches())
            Assert.fail();

////        RegExTest.report("Octal");
    }

    @Test public void longPatternTest() throws Exception {
        try {
            Pattern pattern = PF.compile(
                "a 32-character-long pattern xxxx");
            pattern = PF.compile("a 33-character-long pattern xxxxx");
            pattern = PF.compile("a thirty four character long regex");
            StringBuffer patternToBe = new StringBuffer(101);
            for (int i=0; i<100; i++)
                patternToBe.append((char)(97 + i%26));
            pattern = PF.compile(patternToBe.toString());
        } catch (PatternSyntaxException e) {
            Assert.fail();
        }

        // Supplementary character test
        try {
            Pattern pattern = PF.compile(
                RegExTest.toSupplementaries("a 32-character-long pattern xxxx"));
            pattern = PF.compile(RegExTest.toSupplementaries("a 33-character-long pattern xxxxx"));
            pattern = PF.compile(RegExTest.toSupplementaries("a thirty four character long regex"));
            StringBuffer patternToBe = new StringBuffer(101*2);
            for (int i=0; i<100; i++)
                patternToBe.append(Character.toChars(Character.MIN_SUPPLEMENTARY_CODE_POINT
                                                     + 97 + i%26));
            pattern = PF.compile(patternToBe.toString());
        } catch (PatternSyntaxException e) {
            Assert.fail();
        }
////        RegExTest.report("LongPattern");
    }

    @Test public void group0Test() throws Exception {
        Pattern pattern = PF.compile("(tes)ting");
        Matcher matcher = pattern.matcher("testing");
        RegExTest.check(matcher, "testing");

        matcher.reset("testing");
        if (matcher.lookingAt()) {
            if (!matcher.group(0).equals("testing"))
                Assert.fail();
        } else {
            Assert.fail();
        }

        matcher.reset("testing");
        if (matcher.matches()) {
            if (!matcher.group(0).equals("testing"))
                Assert.fail();
        } else {
            Assert.fail();
        }

        pattern = PF.compile("(tes)ting");
        matcher = pattern.matcher("testing");
        if (matcher.lookingAt()) {
            if (!matcher.group(0).equals("testing"))
                Assert.fail();
        } else {
            Assert.fail();
        }

        pattern = PF.compile("^(tes)ting");
        matcher = pattern.matcher("testing");
        if (matcher.matches()) {
            if (!matcher.group(0).equals("testing"))
                Assert.fail();
        } else {
            Assert.fail();
        }

        // Supplementary character test
        pattern = PF.compile(RegExTest.toSupplementaries("(tes)ting"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("testing"));
        RegExTest.check(matcher, RegExTest.toSupplementaries("testing"));

        matcher.reset(RegExTest.toSupplementaries("testing"));
        if (matcher.lookingAt()) {
            if (!matcher.group(0).equals(RegExTest.toSupplementaries("testing")))
                Assert.fail();
        } else {
            Assert.fail();
        }

        matcher.reset(RegExTest.toSupplementaries("testing"));
        if (matcher.matches()) {
            if (!matcher.group(0).equals(RegExTest.toSupplementaries("testing")))
                Assert.fail();
        } else {
            Assert.fail();
        }

        pattern = PF.compile(RegExTest.toSupplementaries("(tes)ting"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("testing"));
        if (matcher.lookingAt()) {
            if (!matcher.group(0).equals(RegExTest.toSupplementaries("testing")))
                Assert.fail();
        } else {
            Assert.fail();
        }

        pattern = PF.compile(RegExTest.toSupplementaries("^(tes)ting"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("testing"));
        if (matcher.matches()) {
            if (!matcher.group(0).equals(RegExTest.toSupplementaries("testing")))
                Assert.fail();
        } else {
            Assert.fail();
        }

////        RegExTest.report("Group0");
    }

    @Test public void findIntTest() throws Exception {
        Pattern p = PF.compile("blah");
        Matcher m = p.matcher("zzzzblahzzzzzblah");
        boolean result = m.find(2);
        if (!result)
            Assert.fail();

        p = PF.compile("$");
        m = p.matcher("1234567890");
        result = m.find(10);
        if (!result)
            Assert.fail();
        try {
            result = m.find(11);
            Assert.fail();
        } catch (IndexOutOfBoundsException e) {
            // correct result
        }

        // Supplementary character test
        p = PF.compile(RegExTest.toSupplementaries("blah"));
        m = p.matcher(RegExTest.toSupplementaries("zzzzblahzzzzzblah"));
        result = m.find(2);
        if (!result)
            Assert.fail();

////        RegExTest.report("FindInt");
    }

    @Test public void emptyPatternTest() throws Exception {
        Pattern p = PF.compile("");
        Matcher m = p.matcher("foo");

        // Should find empty pattern at beginning of input
        boolean result = m.find();
        if (result != true)
            Assert.fail();
        if (m.start() != 0)
            Assert.fail();

        // Should not match entire input if input is not empty
        m.reset();
        result = m.matches();
        if (result == true)
            Assert.fail();

        try {
            m.start(0);
            Assert.fail();
        } catch (IllegalStateException e) {
            // Correct result
        }

        // Should match entire input if input is empty
        m.reset("");
        result = m.matches();
        if (result != true)
            Assert.fail();

        result = PF.matches("", "");
        if (result != true)
            Assert.fail();

        result = PF.matches("", "foo");
        if (result == true)
            Assert.fail();
////        RegExTest.report("EmptyPattern");
    }

    @Test public void charClassTest() throws Exception {
        Pattern pattern = PF.compile("blah[ab]]blech");
        RegExTest.check(pattern, "blahb]blech", true);

        pattern = PF.compile("[abc[def]]");
        RegExTest.check(pattern, "b", true);

        // Supplementary character tests
        pattern = PF.compile(RegExTest.toSupplementaries("blah[ab]]blech"));
        RegExTest.check(pattern, RegExTest.toSupplementaries("blahb]blech"), true);

        pattern = PF.compile(RegExTest.toSupplementaries("[abc[def]]"));
        RegExTest.check(pattern, RegExTest.toSupplementaries("b"), true);

        try {
            // u00ff when UNICODE_CASE
            pattern = PF.compile("[ab\u00ffcd]",
                                      Pattern.CASE_INSENSITIVE|
                                      Pattern.UNICODE_CASE);
            RegExTest.check(pattern, "ab\u00ffcd", true);
            RegExTest.check(pattern, "Ab\u0178Cd", true);

            // u00b5 when UNICODE_CASE
            pattern = PF.compile("[ab\u00b5cd]",
                                      Pattern.CASE_INSENSITIVE|
                                      Pattern.UNICODE_CASE);
            RegExTest.check(pattern, "ab\u00b5cd", true);
            RegExTest.check(pattern, "Ab\u039cCd", true);
        } catch (Exception e) { Assert.fail(); }

        /* Special cases
           (1)LatinSmallLetterLongS u+017f
           (2)LatinSmallLetterDotlessI u+0131
           (3)LatineCapitalLetterIWithDotAbove u+0130
           (4)KelvinSign u+212a
           (5)AngstromSign u+212b
        */
        int flags = Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE;
        pattern = PF.compile("[sik\u00c5]+", flags);
        if (!pattern.matcher("\u017f\u0130\u0131\u212a\u212b").matches())
            Assert.fail();

////        RegExTest.report("CharClass");
    }

    @Test public void caretTest() throws Exception {
        Pattern pattern = PF.compile("\\w*");
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
        if (matcher.find())
            Assert.fail();

        pattern = PF.compile("^\\w*");
        matcher = pattern.matcher("a#bc#def##g");
        RegExTest.check(matcher, "a");
        if (matcher.find())
            Assert.fail();

        pattern = PF.compile("\\w");
        matcher = pattern.matcher("abc##x");
        RegExTest.check(matcher, "a");
        RegExTest.check(matcher, "b");
        RegExTest.check(matcher, "c");
        RegExTest.check(matcher, "x");
        if (matcher.find())
            Assert.fail();

        pattern = PF.compile("^\\w");
        matcher = pattern.matcher("abc##x");
        RegExTest.check(matcher, "a");
        if (matcher.find())
            Assert.fail();

        pattern = PF.compile("\\A\\p{Alpha}{3}");
        matcher = pattern.matcher("abcdef-ghi\njklmno");
        RegExTest.check(matcher, "abc");
        if (matcher.find())
            Assert.fail();

        pattern = PF.compile("^\\p{Alpha}{3}", Pattern.MULTILINE);
        matcher = pattern.matcher("abcdef-ghi\njklmno");
        RegExTest.check(matcher, "abc");
        RegExTest.check(matcher, "jkl");
        if (matcher.find())
            Assert.fail();

        pattern = PF.compile("^", Pattern.MULTILINE);
        matcher = pattern.matcher("this is some text");
        String result = matcher.replaceAll("X");
        if (!result.equals("Xthis is some text"))
            Assert.fail();

        pattern = PF.compile("^");
        matcher = pattern.matcher("this is some text");
        result = matcher.replaceAll("X");
        if (!result.equals("Xthis is some text"))
            Assert.fail();

        pattern = PF.compile("^", Pattern.MULTILINE | Pattern.UNIX_LINES);
        matcher = pattern.matcher("this is some text\n");
        result = matcher.replaceAll("X");
        if (!result.equals("Xthis is some text\n"))
            Assert.fail();

////        RegExTest.report("Caret");
    }

    @Test public void groupCaptureTest() throws Exception {
        // Independent group
        Pattern pattern = PF.compile("x+(?>y+)z+");
        Matcher matcher = pattern.matcher("xxxyyyzzz");
        matcher.find();
        try {
            String blah = matcher.group(1);
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Good result
        }
        // Pure group
        pattern = PF.compile("x+(?:y+)z+");
        matcher = pattern.matcher("xxxyyyzzz");
        matcher.find();
        try {
            String blah = matcher.group(1);
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Good result
        }

        // Supplementary character tests
        // Independent group
        pattern = PF.compile(RegExTest.toSupplementaries("x+(?>y+)z+"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("xxxyyyzzz"));
        matcher.find();
        try {
            String blah = matcher.group(1);
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Good result
        }
        // Pure group
        pattern = PF.compile(RegExTest.toSupplementaries("x+(?:y+)z+"));
        matcher = pattern.matcher(RegExTest.toSupplementaries("xxxyyyzzz"));
        matcher.find();
        try {
            String blah = matcher.group(1);
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Good result
        }

////        RegExTest.report("GroupCapture");
    }

    @Test public void backRefTest() throws Exception {
        Pattern pattern = PF.compile("(a*)bc\\1");
        RegExTest.check(pattern, "zzzaabcazzz", true);

        pattern = PF.compile("(a*)bc\\1");
        RegExTest.check(pattern, "zzzaabcaazzz", true);

        pattern = PF.compile("(abc)(def)\\1");
        RegExTest.check(pattern, "abcdefabc", true);

        pattern = PF.compile("(abc)(def)\\3");
        RegExTest.check(pattern, "abcdefabc", false);

        try {
            for (int i = 1; i < 10; i++) {
                // Make sure backref 1-9 are always accepted
                pattern = PF.compile("abcdef\\" + i);
                // and fail to match if the target group does not exit
                RegExTest.check(pattern, "abcdef", false);
            }
        } catch(PatternSyntaxException e) {
            Assert.fail();
        }

        pattern = PF.compile("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)\\11");
        RegExTest.check(pattern, "abcdefghija", false);
        RegExTest.check(pattern, "abcdefghija1", true);

        pattern = PF.compile("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(k)\\11");
        RegExTest.check(pattern, "abcdefghijkk", true);

        pattern = PF.compile("(a)bcdefghij\\11");
        RegExTest.check(pattern, "abcdefghija1", true);

        // Supplementary character tests
        pattern = PF.compile(RegExTest.toSupplementaries("(a*)bc\\1"));
        RegExTest.check(pattern, RegExTest.toSupplementaries("zzzaabcazzz"), true);

        pattern = PF.compile(RegExTest.toSupplementaries("(a*)bc\\1"));
        RegExTest.check(pattern, RegExTest.toSupplementaries("zzzaabcaazzz"), true);

        pattern = PF.compile(RegExTest.toSupplementaries("(abc)(def)\\1"));
        RegExTest.check(pattern, RegExTest.toSupplementaries("abcdefabc"), true);

        pattern = PF.compile(RegExTest.toSupplementaries("(abc)(def)\\3"));
        RegExTest.check(pattern, RegExTest.toSupplementaries("abcdefabc"), false);

        pattern = PF.compile(RegExTest.toSupplementaries("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)\\11"));
        RegExTest.check(pattern, RegExTest.toSupplementaries("abcdefghija"), false);
        RegExTest.check(pattern, RegExTest.toSupplementaries("abcdefghija1"), true);

        pattern = PF.compile(RegExTest.toSupplementaries("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(k)\\11"));
        RegExTest.check(pattern, RegExTest.toSupplementaries("abcdefghijkk"), true);

////        RegExTest.report("BackRef");
    }

    /**
     * Unicode Technical Report #18, section 2.6 End of Line
     * There is no empty line to be matched in the sequence \u000D\u000A
     * but there is an empty line in the sequence \u000A\u000D
     */
    @Test public void anchorTest() throws Exception {
        Pattern p = PF.compile("^.*$", Pattern.MULTILINE);
        Matcher m = p.matcher("blah1\r\nblah2");
        m.find();
        m.find();
        if (!m.group().equals("blah2"))
            Assert.fail();

        m.reset("blah1\n\rblah2");
        m.find();
        m.find();
        m.find();
        if (!m.group().equals("blah2"))
            Assert.fail();

        // Test behavior of $ with \r\n at end of input
        p = PF.compile(".+$");
        m = p.matcher("blah1\r\n");
        if (!m.find())
            Assert.fail();
       if (!m.group().equals("blah1"))
            Assert.fail();
        if (m.find())
            Assert.fail();

        // Test behavior of $ with \r\n at end of input in multiline
        p = PF.compile(".+$", Pattern.MULTILINE);
        m = p.matcher("blah1\r\n");
        if (!m.find())
            Assert.fail();
        if (m.find())
            Assert.fail();

        // Test for $ recognition of \u0085 for bug 4527731
        p = PF.compile(".+$", Pattern.MULTILINE);
        m = p.matcher("blah1\u0085");
        if (!m.find())
            Assert.fail();

        // Supplementary character test
        p = PF.compile("^.*$", Pattern.MULTILINE);
        m = p.matcher(RegExTest.toSupplementaries("blah1\r\nblah2"));
        m.find();
        m.find();
        if (!m.group().equals(RegExTest.toSupplementaries("blah2")))
            Assert.fail();

        m.reset(RegExTest.toSupplementaries("blah1\n\rblah2"));
        m.find();
        m.find();
        m.find();
        if (!m.group().equals(RegExTest.toSupplementaries("blah2")))
            Assert.fail();

        // Test behavior of $ with \r\n at end of input
        p = PF.compile(".+$");
        m = p.matcher(RegExTest.toSupplementaries("blah1\r\n"));
        if (!m.find())
            Assert.fail();
        if (!m.group().equals(RegExTest.toSupplementaries("blah1")))
            Assert.fail();
        if (m.find())
            Assert.fail();

        // Test behavior of $ with \r\n at end of input in multiline
        p = PF.compile(".+$", Pattern.MULTILINE);
        m = p.matcher(RegExTest.toSupplementaries("blah1\r\n"));
        if (!m.find())
            Assert.fail();
        if (m.find())
            Assert.fail();

        // Test for $ recognition of \u0085 for bug 4527731
        p = PF.compile(".+$", Pattern.MULTILINE);
        m = p.matcher(RegExTest.toSupplementaries("blah1\u0085"));
        if (!m.find())
            Assert.fail();

////        RegExTest.report("Anchors");
    }

    /**
     * A basic sanity test of Matcher.lookingAt().
     */
    @Test public void lookingAtTest() throws Exception {
        Pattern p = PF.compile("(ab)(c*)");
        Matcher m = p.matcher("abccczzzabcczzzabccc");

        if (!m.lookingAt())
            Assert.fail();

        if (!m.group().equals(m.group(0)))
            Assert.fail();

        m = p.matcher("zzzabccczzzabcczzzabccczzz");
        if (m.lookingAt())
            Assert.fail();

        // Supplementary character test
        p = PF.compile(RegExTest.toSupplementaries("(ab)(c*)"));
        m = p.matcher(RegExTest.toSupplementaries("abccczzzabcczzzabccc"));

        if (!m.lookingAt())
            Assert.fail();

        if (!m.group().equals(m.group(0)))
            Assert.fail();

        m = p.matcher(RegExTest.toSupplementaries("zzzabccczzzabcczzzabccczzz"));
        if (m.lookingAt())
            Assert.fail();

////        RegExTest.report("Looking At");
    }

    /**
     * A basic sanity test of Matcher.matches().
     */
    @Test public void matchesTest() throws Exception {
        // matches()
        Pattern p = PF.compile("ulb(c*)");
        Matcher m = p.matcher("ulbcccccc");
        if (!m.matches())
            Assert.fail();

        // find() but not matches()
        m.reset("zzzulbcccccc");
        if (m.matches())
            Assert.fail();

        // lookingAt() but not matches()
        m.reset("ulbccccccdef");
        if (m.matches())
            Assert.fail();

        // matches()
        p = PF.compile("a|ad");
        m = p.matcher("ad");
        if (!m.matches())
            Assert.fail();

        // Supplementary character test
        // matches()
        p = PF.compile(RegExTest.toSupplementaries("ulb(c*)"));
        m = p.matcher(RegExTest.toSupplementaries("ulbcccccc"));
        if (!m.matches())
            Assert.fail();

        // find() but not matches()
        m.reset(RegExTest.toSupplementaries("zzzulbcccccc"));
        if (m.matches())
            Assert.fail();

        // lookingAt() but not matches()
        m.reset(RegExTest.toSupplementaries("ulbccccccdef"));
        if (m.matches())
            Assert.fail();

        // matches()
        p = PF.compile(RegExTest.toSupplementaries("a|ad"));
        m = p.matcher(RegExTest.toSupplementaries("ad"));
        if (!m.matches())
            Assert.fail();

////        RegExTest.report("Matches");
    }

    /**
     * A basic sanity test of PF.matches().
     */
    @Test public void patternMatchesTest() throws Exception {
        // matches()
        if (!PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                             RegExTest.toSupplementaries("ulbcccccc")))
            Assert.fail();

        // find() but not matches()
        if (PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                            RegExTest.toSupplementaries("zzzulbcccccc")))
            Assert.fail();

        // lookingAt() but not matches()
        if (PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                            RegExTest.toSupplementaries("ulbccccccdef")))
            Assert.fail();

        // Supplementary character test
        // matches()
        if (!PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                             RegExTest.toSupplementaries("ulbcccccc")))
            Assert.fail();

        // find() but not matches()
        if (PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                            RegExTest.toSupplementaries("zzzulbcccccc")))
            Assert.fail();

        // lookingAt() but not matches()
        if (PF.matches(RegExTest.toSupplementaries("ulb(c*)"),
                            RegExTest.toSupplementaries("ulbccccccdef")))
            Assert.fail();

////        RegExTest.report("Pattern Matches");
    }

    /**
     * Canonical equivalence testing. Tests the ability of the engine
     * to match sequences that are not explicitly specified in the
     * pattern when they are considered equivalent by the Unicode Standard.
     */
    @Test public void ceTest() throws Exception {
        // Decomposed char outside char classes
        Pattern p;
		try {
			p = PF.compile("testa\u030a", Pattern.CANON_EQ);
		} catch (IllegalArgumentException iae) {
			if ("Unsupported flag 128".equals(iae.getMessage())) return; // LFR does not (yet) support CANON_EQ
			throw iae;
		}
        Matcher m = p.matcher("test\u00e5");
        if (!m.matches())
            Assert.fail();

        m.reset("testa\u030a");
        if (!m.matches())
            Assert.fail();

        // Composed char outside char classes
        p = PF.compile("test\u00e5", Pattern.CANON_EQ);
        m = p.matcher("test\u00e5");
        if (!m.matches())
            Assert.fail();

        m.reset("testa\u030a");
        if (!m.find())
            Assert.fail();

        // Decomposed char inside a char class
        p = PF.compile("test[abca\u030a]", Pattern.CANON_EQ);
        m = p.matcher("test\u00e5");
        if (!m.find())
            Assert.fail();

        m.reset("testa\u030a");
        if (!m.find())
            Assert.fail();

        // Composed char inside a char class
        p = PF.compile("test[abc\u00e5def\u00e0]", Pattern.CANON_EQ);
        m = p.matcher("test\u00e5");
        if (!m.find())
            Assert.fail();

        m.reset("testa\u0300");
        if (!m.find())
            Assert.fail();

        m.reset("testa\u030a");
        if (!m.find())
            Assert.fail();

        // Marks that cannot legally change order and be equivalent
        p = PF.compile("testa\u0308\u0300", Pattern.CANON_EQ);
        RegExTest.check(p, "testa\u0308\u0300", true);
        RegExTest.check(p, "testa\u0300\u0308", false);

        // Marks that can legally change order and be equivalent
        p = PF.compile("testa\u0308\u0323", Pattern.CANON_EQ);
        RegExTest.check(p, "testa\u0308\u0323", true);
        RegExTest.check(p, "testa\u0323\u0308", true);

        // Test all equivalences of the sequence a\u0308\u0323\u0300
        p = PF.compile("testa\u0308\u0323\u0300", Pattern.CANON_EQ);
        RegExTest.check(p, "testa\u0308\u0323\u0300", true);
        RegExTest.check(p, "testa\u0323\u0308\u0300", true);
        RegExTest.check(p, "testa\u0308\u0300\u0323", true);
        RegExTest.check(p, "test\u00e4\u0323\u0300", true);
        RegExTest.check(p, "test\u00e4\u0300\u0323", true);

        Object[][] data = new Object[][] {

        // JDK-4867170
        { "[\u1f80-\u1f82]", "ab\u1f80cd",             "f", true }, // 0
        { "[\u1f80-\u1f82]", "ab\u1f81cd",             "f", true },
        { "[\u1f80-\u1f82]", "ab\u1f82cd",             "f", true },
        { "[\u1f80-\u1f82]", "ab\u03b1\u0314\u0345cd", "f", true },
        { "[\u1f80-\u1f82]", "ab\u03b1\u0345\u0314cd", "f", true },
        { "[\u1f80-\u1f82]", "ab\u1f01\u0345cd",       "f", true },
        { "[\u1f80-\u1f82]", "ab\u1f00\u0345cd",       "f", true },

        { "\\p{IsGreek}",    "ab\u1f80cd",             "f", true },
        { "\\p{IsGreek}",    "ab\u1f81cd",             "f", true },
        { "\\p{IsGreek}",    "ab\u1f82cd",             "f", true },
        { "\\p{IsGreek}",    "ab\u03b1\u0314\u0345cd", "f", true }, // 10
        { "\\p{IsGreek}",    "ab\u1f01\u0345cd",       "f", true },

        // backtracking, force to match "\u1f80", instead of \u1f82"
        { "ab\\p{IsGreek}\u0300cd", "ab\u03b1\u0313\u0345\u0300cd", "m", true },

        { "[\\p{IsGreek}]",  "\u03b1\u0314\u0345",     "m", true },
        { "\\p{IsGreek}",    "\u03b1\u0314\u0345",     "m", true },

        { "[^\u1f80-\u1f82]","\u1f81",                 "m", false },
        { "[^\u1f80-\u1f82]","\u03b1\u0314\u0345",     "m", false },
        { "[^\u1f01\u0345]", "\u1f81",                 "f", false },

        { "[^\u1f81]+",      "\u1f80\u1f82",           "f", true },
        { "[\u1f80]",        "ab\u1f80cd",             "f", true },
        { "\u1f80",          "ab\u1f80cd",             "f", true },    // 20
        { "\u1f00\u0345\u0300",  "\u1f82", "m", true },
        { "\u1f80",          "-\u1f00\u0345\u0300-",   "f", true },
        { "\u1f82",          "\u1f00\u0345\u0300",     "m", true },
        { "\u1f82",          "\u1f80\u0300",           "m", true },

        // JDK-7080302       # compile failed
        { "a(\u0041\u0301\u0328)", "a\u0041\u0301\u0328", "m", true},

        // JDK-6728861, same cause as above one
        { "\u00e9\u00e9n", "e\u0301e\u0301n", "m", true},

        // JDK-6995635
        { "(\u00e9)", "e\u0301", "m", true },

        // JDK-6736245
        // intereting special case, nfc(u2add+u0338) -> u2add+u0338) NOT u2adc
        { "\u2ADC", "\u2ADC", "m", true},          // NFC
        { "\u2ADC", "\u2ADD\u0338", "m", true},    // NFD

        //  4916384.
        // Decomposed hangul (jamos) works inside clazz
        { "[\u1100\u1161]", "\u1100\u1161", "m", true},           // 30
        { "[\u1100\u1161]", "\uac00", "m", true},

        { "[\uac00]", "\u1100\u1161", "m", true},
        { "[\uac00]", "\uac00", "m", true},

        // Decomposed hangul (jamos)
        { "\u1100\u1161", "\u1100\u1161", "m", true},
        { "\u1100\u1161", "\uac00", "m", true},

        // Composed hangul
        { "\uac00",  "\u1100\u1161", "m", true },
        { "\uac00",  "\uac00", "m", true },

        /* Need a NFDSlice to nfd the source to solve this issue
           u+1d1c0 -> nfd: <u+1d1ba><u+1d165><u+1d16f>  -> nfc: <u+1d1ba><u+1d165><u+1d16f>
           u+1d1bc -> nfd: <u+1d1ba><u+1d165>           -> nfc: <u+1d1ba><u+1d165>
           <u+1d1bc><u+1d16f> -> nfd: <u+1d1ba><u+1d165><u+1d16f> -> nfc: <u+1d1ba><u+1d165><u+1d16f>

        // Decomposed supplementary outside char classes
        // { "test\ud834\uddbc\ud834\udd6f", "test\ud834\uddc0", "m", true },
        // Composed supplementary outside char classes
        // { "test\ud834\uddc0", "test\ud834\uddbc\ud834\udd6f", "m", true },
        */
        { "test\ud834\uddbc\ud834\udd6f", "test\ud834\uddbc\ud834\udd6f", "m", true },
//        { "test\ud834\uddc0",             "test\ud834\uddbc\ud834\udd6f", "m", true },   // Fails with JRE 17 /* AU */

        { "test\ud834\uddc0",             "test\ud834\uddc0",             "m", true },
//        { "test\ud834\uddbc\ud834\udd6f", "test\ud834\uddc0",             "m", true },   // Fails with JRE 17 /* AU */
        };

        int failCount = 0;
        for (int i = 0; i < data.length; i++) {
            Object[] d = data[i];

            String  pn       = (String) d[0];
            String  tt       = (String) d[1];
            boolean isFind   = "f".equals(((String) d[2]));
            boolean expected = (boolean) d[3];

            Matcher matcher = PF.compile(pn, Pattern.CANON_EQ).matcher(tt);
			boolean ret = isFind ? matcher.find() : matcher.matches();

			Assert.assertEquals("#" + i + ": Pattern=" + pn + ", subject=" + tt + ", isFind=" + isFind, expected, ret);
//            if (ret != expected) {
//                continue;
//            }
        }
////        RegExTest.report("Canonical Equivalence");
    }

    /**
     * A basic sanity test of Matcher.replaceAll().
     */
    @Test public void globalSubstitute() throws Exception {
        // Global substitution with a literal
        Pattern p = PF.compile("(ab)(c*)");
        Matcher m = p.matcher("abccczzzabcczzzabccc");
        if (!m.replaceAll("test").equals("testzzztestzzztest"))
            Assert.fail();

        m.reset("zzzabccczzzabcczzzabccczzz");
        if (!m.replaceAll("test").equals("zzztestzzztestzzztestzzz"))
            Assert.fail();

        // Global substitution with groups
        m.reset("zzzabccczzzabcczzzabccczzz");
        String result = m.replaceAll("$1");
        if (!result.equals("zzzabzzzabzzzabzzz"))
            Assert.fail();

        // Supplementary character test
        // Global substitution with a literal
        p = PF.compile(RegExTest.toSupplementaries("(ab)(c*)"));
        m = p.matcher(RegExTest.toSupplementaries("abccczzzabcczzzabccc"));
        if (!m.replaceAll(RegExTest.toSupplementaries("test")).
            equals(RegExTest.toSupplementaries("testzzztestzzztest")))
            Assert.fail();

        m.reset(RegExTest.toSupplementaries("zzzabccczzzabcczzzabccczzz"));
        if (!m.replaceAll(RegExTest.toSupplementaries("test")).
            equals(RegExTest.toSupplementaries("zzztestzzztestzzztestzzz")))
            Assert.fail();

        // Global substitution with groups
        m.reset(RegExTest.toSupplementaries("zzzabccczzzabcczzzabccczzz"));
        result = m.replaceAll("$1");
        if (!result.equals(RegExTest.toSupplementaries("zzzabzzzabzzzabzzz")))
            Assert.fail();

////        RegExTest.report("Global Substitution");
    }

    /**
     * Tests the usage of Matcher.appendReplacement() with literal
     * and group substitutions.
     */
    @Test public void stringbufferSubstitute() throws Exception {
        // SB substitution with literal
        String blah = "zzzblahzzz";
        Pattern p = PF.compile("blah");
        Matcher m = p.matcher(blah);
        StringBuffer result = new StringBuffer();
        try {
            m.appendReplacement(result, "blech");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "blech");
        if (!result.toString().equals("zzzblech"))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals("zzzblechzzz"))
            Assert.fail();

        // SB substitution with groups
        blah = "zzzabcdzzz";
        p = PF.compile("(ab)(cd)*");
        m = p.matcher(blah);
        result = new StringBuffer();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        if (!result.toString().equals("zzzab"))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals("zzzabzzz"))
            Assert.fail();

        // SB substitution with 3 groups
        blah = "zzzabcdcdefzzz";
        p = PF.compile("(ab)(cd)*(ef)");
        m = p.matcher(blah);
        result = new StringBuffer();
        try {
            m.appendReplacement(result, "$1w$2w$3");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1w$2w$3");
        if (!result.toString().equals("zzzabwcdwef"))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals("zzzabwcdwefzzz"))
            Assert.fail();

        // SB substitution with groups and three matches
        // skipping middle match
        blah = "zzzabcdzzzabcddzzzabcdzzz";
        p = PF.compile("(ab)(cd*)");
        m = p.matcher(blah);
        result = new StringBuffer();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        if (!result.toString().equals("zzzab"))
            Assert.fail();

        m.find();
        m.find();
        m.appendReplacement(result, "$2");
        if (!result.toString().equals("zzzabzzzabcddzzzcd"))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals("zzzabzzzabcddzzzcdzzz"))
            Assert.fail();

        // Check to make sure escaped $ is ignored
        blah = "zzzabcdcdefzzz";
        p = PF.compile("(ab)(cd)*(ef)");
        m = p.matcher(blah);
        result = new StringBuffer();
        m.find();
        m.appendReplacement(result, "$1w\\$2w$3");
        if (!result.toString().equals("zzzabw$2wef"))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals("zzzabw$2wefzzz"))
            Assert.fail();

        // Check to make sure a reference to nonexistent group causes error
        blah = "zzzabcdcdefzzz";
        p = PF.compile("(ab)(cd)*(ef)");
        m = p.matcher(blah);
        result = new StringBuffer();
        m.find();
        try {
            m.appendReplacement(result, "$1w$5w$3");
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Correct result
        }

        // Check double digit group references
        blah = "zzz123456789101112zzz";
        p = PF.compile("(1)(2)(3)(4)(5)(6)(7)(8)(9)(10)(11)");
        m = p.matcher(blah);
        result = new StringBuffer();
        m.find();
        m.appendReplacement(result, "$1w$11w$3");
        if (!result.toString().equals("zzz1w11w3"))
            Assert.fail();

        // Check to make sure it backs off $15 to $1 if only three groups
        blah = "zzzabcdcdefzzz";
        p = PF.compile("(ab)(cd)*(ef)");
        m = p.matcher(blah);
        result = new StringBuffer();
        m.find();
        m.appendReplacement(result, "$1w$15w$3");
        if (!result.toString().equals("zzzabwab5wef"))
            Assert.fail();


        // Supplementary character test
        // SB substitution with literal
        blah = RegExTest.toSupplementaries("zzzblahzzz");
        p = PF.compile(RegExTest.toSupplementaries("blah"));
        m = p.matcher(blah);
        result = new StringBuffer();
        try {
            m.appendReplacement(result, RegExTest.toSupplementaries("blech"));
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("blech"));
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzblech")))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzblechzzz")))
            Assert.fail();

        // SB substitution with groups
        blah = RegExTest.toSupplementaries("zzzabcdzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd)*"));
        m = p.matcher(blah);
        result = new StringBuffer();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzab")))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabzzz")))
            Assert.fail();

        // SB substitution with 3 groups
        blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        m = p.matcher(blah);
        result = new StringBuffer();
        try {
            m.appendReplacement(result, RegExTest.toSupplementaries("$1w$2w$3"));
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w$2w$3"));
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabwcdwef")))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabwcdwefzzz")))
            Assert.fail();

        // SB substitution with groups and three matches
        // skipping middle match
        blah = RegExTest.toSupplementaries("zzzabcdzzzabcddzzzabcdzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd*)"));
        m = p.matcher(blah);
        result = new StringBuffer();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzab")))
            Assert.fail();

        m.find();
        m.find();
        m.appendReplacement(result, "$2");
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabzzzabcddzzzcd")))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabzzzabcddzzzcdzzz")))
            Assert.fail();

        // Check to make sure escaped $ is ignored
        blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        m = p.matcher(blah);
        result = new StringBuffer();
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w\\$2w$3"));
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabw$2wef")))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabw$2wefzzz")))
            Assert.fail();

        // Check to make sure a reference to nonexistent group causes error
        blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        m = p.matcher(blah);
        result = new StringBuffer();
        m.find();
        try {
            m.appendReplacement(result, RegExTest.toSupplementaries("$1w$5w$3"));
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Correct result
        }

        // Check double digit group references
        blah = RegExTest.toSupplementaries("zzz123456789101112zzz");
        p = PF.compile("(1)(2)(3)(4)(5)(6)(7)(8)(9)(10)(11)");
        m = p.matcher(blah);
        result = new StringBuffer();
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w$11w$3"));
        if (!result.toString().equals(RegExTest.toSupplementaries("zzz1w11w3")))
            Assert.fail();

        // Check to make sure it backs off $15 to $1 if only three groups
        blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        m = p.matcher(blah);
        result = new StringBuffer();
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w$15w$3"));
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabwab5wef")))
            Assert.fail();

        // Check nothing has been appended into the output buffer if
        // the replacement string triggers IllegalArgumentException.
        p = PF.compile("(abc)");
        m = p.matcher("abcd");
        result = new StringBuffer();
        m.find();
        try {
            m.appendReplacement(result, ("xyz$g"));
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            if (result.length() != 0)
                Assert.fail();
        }

////        RegExTest.report("SB Substitution");
    }

    /**
     * Tests the usage of Matcher.appendReplacement() with literal
     * and group substitutions.
     */
    @Test public void stringbuilderSubstitute() throws Exception {
        // SB substitution with literal
        String blah = "zzzblahzzz";
        Pattern p = PF.compile("blah");
        Matcher m = p.matcher(blah);
        StringBuilder result = new StringBuilder();
        try {
            m.appendReplacement(result, "blech");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "blech");
        if (!result.toString().equals("zzzblech"))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals("zzzblechzzz"))
            Assert.fail();

        // SB substitution with groups
        blah = "zzzabcdzzz";
        p = PF.compile("(ab)(cd)*");
        m = p.matcher(blah);
        result = new StringBuilder();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        if (!result.toString().equals("zzzab"))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals("zzzabzzz"))
            Assert.fail();

        // SB substitution with 3 groups
        blah = "zzzabcdcdefzzz";
        p = PF.compile("(ab)(cd)*(ef)");
        m = p.matcher(blah);
        result = new StringBuilder();
        try {
            m.appendReplacement(result, "$1w$2w$3");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1w$2w$3");
        if (!result.toString().equals("zzzabwcdwef"))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals("zzzabwcdwefzzz"))
            Assert.fail();

        // SB substitution with groups and three matches
        // skipping middle match
        blah = "zzzabcdzzzabcddzzzabcdzzz";
        p = PF.compile("(ab)(cd*)");
        m = p.matcher(blah);
        result = new StringBuilder();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        if (!result.toString().equals("zzzab"))
            Assert.fail();

        m.find();
        m.find();
        m.appendReplacement(result, "$2");
        if (!result.toString().equals("zzzabzzzabcddzzzcd"))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals("zzzabzzzabcddzzzcdzzz"))
            Assert.fail();

        // Check to make sure escaped $ is ignored
        blah = "zzzabcdcdefzzz";
        p = PF.compile("(ab)(cd)*(ef)");
        m = p.matcher(blah);
        result = new StringBuilder();
        m.find();
        m.appendReplacement(result, "$1w\\$2w$3");
        if (!result.toString().equals("zzzabw$2wef"))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals("zzzabw$2wefzzz"))
            Assert.fail();

        // Check to make sure a reference to nonexistent group causes error
        blah = "zzzabcdcdefzzz";
        p = PF.compile("(ab)(cd)*(ef)");
        m = p.matcher(blah);
        result = new StringBuilder();
        m.find();
        try {
            m.appendReplacement(result, "$1w$5w$3");
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Correct result
        }

        // Check double digit group references
        blah = "zzz123456789101112zzz";
        p = PF.compile("(1)(2)(3)(4)(5)(6)(7)(8)(9)(10)(11)");
        m = p.matcher(blah);
        result = new StringBuilder();
        m.find();
        m.appendReplacement(result, "$1w$11w$3");
        if (!result.toString().equals("zzz1w11w3"))
            Assert.fail();

        // Check to make sure it backs off $15 to $1 if only three groups
        blah = "zzzabcdcdefzzz";
        p = PF.compile("(ab)(cd)*(ef)");
        m = p.matcher(blah);
        result = new StringBuilder();
        m.find();
        m.appendReplacement(result, "$1w$15w$3");
        if (!result.toString().equals("zzzabwab5wef"))
            Assert.fail();


        // Supplementary character test
        // SB substitution with literal
        blah = RegExTest.toSupplementaries("zzzblahzzz");
        p = PF.compile(RegExTest.toSupplementaries("blah"));
        m = p.matcher(blah);
        result = new StringBuilder();
        try {
            m.appendReplacement(result, RegExTest.toSupplementaries("blech"));
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("blech"));
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzblech")))
            Assert.fail();
        m.appendTail(result);
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzblechzzz")))
            Assert.fail();

        // SB substitution with groups
        blah = RegExTest.toSupplementaries("zzzabcdzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd)*"));
        m = p.matcher(blah);
        result = new StringBuilder();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzab")))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabzzz")))
            Assert.fail();

        // SB substitution with 3 groups
        blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        m = p.matcher(blah);
        result = new StringBuilder();
        try {
            m.appendReplacement(result, RegExTest.toSupplementaries("$1w$2w$3"));
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w$2w$3"));
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabwcdwef")))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabwcdwefzzz")))
            Assert.fail();

        // SB substitution with groups and three matches
        // skipping middle match
        blah = RegExTest.toSupplementaries("zzzabcdzzzabcddzzzabcdzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd*)"));
        m = p.matcher(blah);
        result = new StringBuilder();
        try {
            m.appendReplacement(result, "$1");
            Assert.fail();
        } catch (IllegalStateException e) {
        }
        m.find();
        m.appendReplacement(result, "$1");
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzab")))
            Assert.fail();

        m.find();
        m.find();
        m.appendReplacement(result, "$2");
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabzzzabcddzzzcd")))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabzzzabcddzzzcdzzz")))
            Assert.fail();

        // Check to make sure escaped $ is ignored
        blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        m = p.matcher(blah);
        result = new StringBuilder();
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w\\$2w$3"));
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabw$2wef")))
            Assert.fail();

        m.appendTail(result);
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabw$2wefzzz")))
            Assert.fail();

        // Check to make sure a reference to nonexistent group causes error
        blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        m = p.matcher(blah);
        result = new StringBuilder();
        m.find();
        try {
            m.appendReplacement(result, RegExTest.toSupplementaries("$1w$5w$3"));
            Assert.fail();
        } catch (IndexOutOfBoundsException ioobe) {
            // Correct result
        }
        // Check double digit group references
        blah = RegExTest.toSupplementaries("zzz123456789101112zzz");
        p = PF.compile("(1)(2)(3)(4)(5)(6)(7)(8)(9)(10)(11)");
        m = p.matcher(blah);
        result = new StringBuilder();
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w$11w$3"));
        if (!result.toString().equals(RegExTest.toSupplementaries("zzz1w11w3")))
            Assert.fail();

        // Check to make sure it backs off $15 to $1 if only three groups
        blah = RegExTest.toSupplementaries("zzzabcdcdefzzz");
        p = PF.compile(RegExTest.toSupplementaries("(ab)(cd)*(ef)"));
        m = p.matcher(blah);
        result = new StringBuilder();
        m.find();
        m.appendReplacement(result, RegExTest.toSupplementaries("$1w$15w$3"));
        if (!result.toString().equals(RegExTest.toSupplementaries("zzzabwab5wef")))
            Assert.fail();
        // Check nothing has been appended into the output buffer if
        // the replacement string triggers IllegalArgumentException.
        p = PF.compile("(abc)");
        m = p.matcher("abcd");
        result = new StringBuilder();
        m.find();
        try {
            m.appendReplacement(result, ("xyz$g"));
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            if (result.length() != 0)
                Assert.fail();
        }
////        RegExTest.report("SB Substitution 2");
    }

    /*
     * 5 groups of characters are created to make a substitution string.
     * A base string will be created including random lead chars, the
     * substitution string, and random trailing chars.
     * A pattern containing the 5 groups is searched for and replaced with:
     * random group + random string + random group.
     * The results are checked for correctness.
     */
    @Test public void substitutionBasher() {
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
            Pattern p = PF.compile(pattern);
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
            if (!result.equals(expectedResult))
                Assert.fail();
        }

////        RegExTest.report("Substitution Basher");
    }

    /*
     * 5 groups of characters are created to make a substitution string.
     * A base string will be created including random lead chars, the
     * substitution string, and random trailing chars.
     * A pattern containing the 5 groups is searched for and replaced with:
     * random group + random string + random group.
     * The results are checked for correctness.
     */
    @Test public void substitutionBasher2() {
        for (int runs = 0; runs<1000; runs++) {
            // Create a base string to work in
            int leadingChars = RegExTest.generator.nextInt(10);
            StringBuilder baseBuffer = new StringBuilder(100);
            String leadingString = RegExTest.getRandomAlphaString(leadingChars);
            baseBuffer.append(leadingString);

            // Create 5 groups of random number of random chars
            // Create the string to substitute
            // Create the pattern string to search for
            StringBuilder bufferToSub = new StringBuilder(25);
            StringBuilder bufferToPat = new StringBuilder(50);
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
            Pattern p = PF.compile(pattern);
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
            StringBuilder bufferToRep = new StringBuilder();
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
            StringBuilder bufferToRes = new StringBuilder();
            bufferToRes.append(leadingString);
            bufferToRes.append(groups[groupIndex1]);
            bufferToRes.append(randomMidString);
            bufferToRes.append(groups[groupIndex2]);
            bufferToRes.append(trailingString);
            String expectedResult = bufferToRes.toString();

            // Check results
            if (!result.equals(expectedResult)) {
                Assert.fail();
            }
        }

////        RegExTest.report("Substitution Basher 2");
    }

    /**
     * Checks the handling of some escape sequences that the Pattern
     * class should process instead of the java compiler. These are
     * not in the file because the escapes should be be processed
     * by the Pattern class when the regex is compiled.
     */
    @Test public void escapes() throws Exception {
        Pattern p = PF.compile("\\043");
        Matcher m = p.matcher("#");
        if (!m.find())
            Assert.fail();

        p = PF.compile("\\x23");
        m = p.matcher("#");
        if (!m.find())
            Assert.fail();

        p = PF.compile("\\u0023");
        m = p.matcher("#");
        if (!m.find())
            Assert.fail();

////        RegExTest.report("Escape sequences");
    }

    /**
     * Checks the handling of blank input situations. These
     * tests are incompatible with my test file format.
     */
    @Test public void blankInput() throws Exception {
        Pattern p = PF.compile("abc", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher("");
        if (m.find())
            Assert.fail();

        p = PF.compile("a*", Pattern.CASE_INSENSITIVE);
        m = p.matcher("");
        if (!m.find())
            Assert.fail();

        p = PF.compile("abc");
        m = p.matcher("");
        if (m.find())
            Assert.fail();

        p = PF.compile("a*");
        m = p.matcher("");
        if (!m.find())
            Assert.fail();

////        RegExTest.report("Blank input");
    }

    /**
     * Tests the Boyer-Moore pattern matching of a character sequence
     * on randomly generated patterns.
     */
    @Test public void bm() throws Exception {
        RegExTest.doBnM('a');
////        RegExTest.report("Boyer Moore (ASCII)");

        RegExTest.doBnM(Character.MIN_SUPPLEMENTARY_CODE_POINT - 10);
//        RegExTest.report("Boyer Moore (Supplementary)");
    }

    private static void doBnM(int baseCharacter) throws Exception {
        int achar=0;

        for (int i=0; i<100; i++) {
            // Create a short pattern to search for
            int patternLength = RegExTest.generator.nextInt(7) + 4;
            StringBuffer patternBuffer = new StringBuffer(patternLength);
            String pattern;
            retry: for (;;) {
                for (int x=0; x<patternLength; x++) {
                    int ch = baseCharacter + RegExTest.generator.nextInt(26);
                    if (Character.isSupplementaryCodePoint(ch)) {
                        patternBuffer.append(Character.toChars(ch));
                    } else {
                        patternBuffer.append((char)ch);
                    }
                }
                pattern = patternBuffer.toString();

                // Avoid patterns that start and end with the same substring
                // See JDK-6854417
                for (int x=1; x < pattern.length(); x++) {
                    if (pattern.startsWith(pattern.substring(x)))
                        continue retry;
                }
                break;
            }
            Pattern p = PF.compile(pattern);

            // Create a buffer with random ASCII chars that does
            // not match the sample
            String toSearch = null;
            StringBuffer s = null;
            Matcher m = p.matcher("");
            do {
                s = new StringBuffer(100);
                for (int x=0; x<100; x++) {
                    int ch = baseCharacter + RegExTest.generator.nextInt(26);
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
            int insertIndex = RegExTest.generator.nextInt(99);
            if (Character.isLowSurrogate(s.charAt(insertIndex)))
                insertIndex++;
            s = s.insert(insertIndex, pattern);
            toSearch = s.toString();

            // Make sure that the pattern is found
            m.reset(toSearch);
            if (!m.find())
                Assert.fail();

            // Make sure that the match text is the pattern
            if (!m.group().equals(pattern))
                Assert.fail();

            // Make sure match occured at insertion point
            if (m.start() != insertIndex)
                Assert.fail();
        }
    }

    /**
     * Tests the matching of slices on randomly generated patterns.
     * The Boyer-Moore optimization is not done on these patterns
     * because it uses unicode case folding.
     */
    @Test public void slice() throws Exception {
        RegExTest.doSlice(Character.MAX_VALUE);
//        RegExTest.report("Slice");

        RegExTest.doSlice(Character.MAX_CODE_POINT);
//        RegExTest.report("Slice (Supplementary)");
    }

    private static void doSlice(int maxCharacter) throws Exception {
        Random generator = new Random();
        int achar=0;

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
            Pattern p = PF.compile(pattern, Pattern.UNICODE_CASE);

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
            if (!m.find())
                Assert.fail();

            // Make sure that the match text is the pattern
            if (!m.group().equals(pattern))
                Assert.fail();

            // Make sure match occured at insertion point
            if (m.start() != insertIndex)
                Assert.fail();
        }
    }

    private static void explainFailure(String pattern, String data,
                                       String expected, String actual) {
        System.err.println("----------------------------------------");
        System.err.println("Pattern = "+pattern);
        System.err.println("Data = "+data);
        System.err.println("Expected = " + expected);
        System.err.println("Actual   = " + actual);
    }

    private static void explainFailure(String pattern, String data,
                                       Throwable t) {
        System.err.println("----------------------------------------");
        System.err.println("Pattern = "+pattern);
        System.err.println("Data = "+data);
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
        File testCases = new File(/*System.getProperty("test.src", ".")*/ "target/test-classes",
                                  fileName);
        FileInputStream in = new FileInputStream(testCases);
        LineNumberReader r = new LineNumberReader(new InputStreamReader(in));

        // Process next test case.
        String aLine;
        while((aLine = r.readLine()) != null) {
            // Read a line for pattern
            String patternString = RegExTest.grabLine(r);
            Pattern p = null;
            try {
                p = RegExTest.compileTestPattern(patternString);
            } catch (PatternSyntaxException e) {
                String dataString = RegExTest.grabLine(r);
                String expectedResult = RegExTest.grabLine(r);
                if (expectedResult.startsWith("error"))
                    continue;
                RegExTest.explainFailure(patternString, dataString, e);
                Assert.fail();
                continue;
            }

            // Read a line for input string
            String dataString = RegExTest.grabLine(r);
            Matcher m = p.matcher(dataString);
            StringBuffer result = new StringBuffer();

            // Check for IllegalStateExceptions before a match
            RegExTest.preMatchInvariants(m);

            boolean found = m.find();

            if (found)
                RegExTest.postTrueMatchInvariants(m);
            else
                RegExTest.postFalseMatchInvariants(m);

            if (found) {
                result.append("true ");
                result.append(m.group(0) + " ");
            } else {
                result.append("false ");
            }

            result.append(m.groupCount());

            if (found) {
                for (int i = 1; i < m.groupCount() + 1; i++) {
                    if (m.group(i) != null) result.append(" " + m.group(i));
                }
            }

            // Read a line for the expected result
            String expectedResult = RegExTest.grabLine(r);

            if (!result.toString().equals(expectedResult)) {
            	throw new AssertionError(
        			testCases
        			+ ", before line #" + r.getLineNumber()
        			+ ": Pattern=" + patternString
        			+ ", Data=" + dataString
        			+ ", Expected=" + expectedResult + "(matches group0 groupCount groups...)"
        			+ ", Actual=" + result
    			);
            }
        }

//        RegExTest.report(fileName);
    }

    private static void preMatchInvariants(Matcher m) {
        try {
            m.start();
            Assert.fail();
        } catch (IllegalStateException ise) {}
        try {
            m.end();
            Assert.fail();
        } catch (IllegalStateException ise) {}
        try {
            m.group();
            Assert.fail();
        } catch (IllegalStateException ise) {}
    }

    private static int postFalseMatchInvariants(Matcher m) {
        int failCount = 0;
        try {
            m.group();
            Assert.fail();
        } catch (IllegalStateException ise) {}
        try {
            m.start();
            Assert.fail();
        } catch (IllegalStateException ise) {}
        try {
            m.end();
            Assert.fail();
        } catch (IllegalStateException ise) {}
        return failCount;
    }

    private static int postTrueMatchInvariants(Matcher m) {
        int failCount = 0;
        //assert(m.start() = m.start(0);
        if (m.start() != m.start(0))
            Assert.fail();
        //assert(m.end() = m.end(0);
        if (m.start() != m.start(0))
            Assert.fail();
        //assert(m.group() = m.group(0);
        if (!m.group().equals(m.group(0)))
            Assert.fail();
        try {
            m.group(50);
            Assert.fail();
        } catch (IndexOutOfBoundsException ise) {}

        return failCount;
    }

    private static Pattern compileTestPattern(String patternString) {
        if (!patternString.startsWith("'")) {
            return PF.compile(patternString);
        }
        int break1 = patternString.lastIndexOf("'");
        String flagString = patternString.substring(
                                          break1+1, patternString.length());
        patternString = patternString.substring(1, break1);

        if (flagString.equals("i"))
            return PF.compile(patternString, Pattern.CASE_INSENSITIVE);

        if (flagString.equals("m"))
            return PF.compile(patternString, Pattern.MULTILINE);

        return PF.compile(patternString);
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
        m.find();
        if (!m.group(g).equals(expected) ||
            s.charAt(m.start(g)) != expected.charAt(0) ||
            s.charAt(m.end(g) - 1) != expected.charAt(expected.length() - 1))
            Assert.fail();
    }

    private static void checkReplaceFirst(String p, String s, String r, String expected)
    {
        if (!expected.equals(PF.compile(p)
                                    .matcher(s)
                                    .replaceFirst(r)))
            Assert.fail();
    }

    private static void checkReplaceAll(String p, String s, String r, String expected)
    {
        if (!expected.equals(PF.compile(p)
                                    .matcher(s)
                                    .replaceAll(r)))
            Assert.fail();
    }

    private static void checkExpectedFail(String p) {
        try {
            PF.compile(p);
        } catch (PatternSyntaxException pse) {
            //pse.printStackTrace();
            return;
        }
        Assert.fail();
    }

    private static void checkExpectedIAE(Matcher m, String g) {
        m.find();
        try {
            m.group(g);
        } catch (IllegalArgumentException x) {
            //iae.printStackTrace();
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

    @Test public void namedGroupCaptureTest() throws Exception {
        RegExTest.check(PF.compile("x+(?<gname>y+)z+"),
              "xxxyyyzzz",
              "gname",
              "yyy");

        RegExTest.check(PF.compile("x+(?<gname8>y+)z+"),
              "xxxyyyzzz",
              "gname8",
              "yyy");

        //backref
        Pattern pattern = PF.compile("(a*)bc\\1");
        RegExTest.check(pattern, "zzzaabcazzz", true);  // found "abca"

        RegExTest.check(PF.compile("(?<gname>a*)bc\\k<gname>"),
              "zzzaabcaazzz", true);

        RegExTest.check(PF.compile("(?<gname>abc)(def)\\k<gname>"),
              "abcdefabc", true);

        RegExTest.check(PF.compile("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(?<gname>k)\\k<gname>"),
              "abcdefghijkk", true);

        // Supplementary character tests
        RegExTest.check(PF.compile("(?<gname>" + RegExTest.toSupplementaries("a*)bc") + "\\k<gname>"),
              RegExTest.toSupplementaries("zzzaabcazzz"), true);

        RegExTest.check(PF.compile("(?<gname>" + RegExTest.toSupplementaries("a*)bc") + "\\k<gname>"),
              RegExTest.toSupplementaries("zzzaabcaazzz"), true);

        RegExTest.check(PF.compile("(?<gname>" + RegExTest.toSupplementaries("abc)(def)") + "\\k<gname>"),
              RegExTest.toSupplementaries("abcdefabc"), true);

        RegExTest.check(PF.compile(RegExTest.toSupplementaries("(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)") +
                              "(?<gname>" +
                              RegExTest.toSupplementaries("k)") + "\\k<gname>"),
              RegExTest.toSupplementaries("abcdefghijkk"), true);

        RegExTest.check(PF.compile("x+(?<gname>y+)z+\\k<gname>"),
              "xxxyyyzzzyyy",
              "gname",
              "yyy");

        //replaceFirst/All
        RegExTest.checkReplaceFirst("(?<gn>ab)(c*)",
                          "abccczzzabcczzzabccc",
                          "${gn}",
                          "abzzzabcczzzabccc");

        RegExTest.checkReplaceAll("(?<gn>ab)(c*)",
                        "abccczzzabcczzzabccc",
                        "${gn}",
                        "abzzzabzzzab");


        RegExTest.checkReplaceFirst("(?<gn>ab)(c*)",
                          "zzzabccczzzabcczzzabccczzz",
                          "${gn}",
                          "zzzabzzzabcczzzabccczzz");

        RegExTest.checkReplaceAll("(?<gn>ab)(c*)",
                        "zzzabccczzzabcczzzabccczzz",
                        "${gn}",
                        "zzzabzzzabzzzabzzz");

        RegExTest.checkReplaceFirst("(?<gn1>ab)(?<gn2>c*)",
                          "zzzabccczzzabcczzzabccczzz",
                          "${gn2}",
                          "zzzccczzzabcczzzabccczzz");

        RegExTest.checkReplaceAll("(?<gn1>ab)(?<gn2>c*)",
                        "zzzabccczzzabcczzzabccczzz",
                        "${gn2}",
                        "zzzccczzzcczzzccczzz");

        //toSupplementaries("(ab)(c*)"));
        RegExTest.checkReplaceFirst("(?<gn1>" + RegExTest.toSupplementaries("ab") +
                           ")(?<gn2>" + RegExTest.toSupplementaries("c") + "*)",
                          RegExTest.toSupplementaries("abccczzzabcczzzabccc"),
                          "${gn1}",
                          RegExTest.toSupplementaries("abzzzabcczzzabccc"));


        RegExTest.checkReplaceAll("(?<gn1>" + RegExTest.toSupplementaries("ab") +
                        ")(?<gn2>" + RegExTest.toSupplementaries("c") + "*)",
                        RegExTest.toSupplementaries("abccczzzabcczzzabccc"),
                        "${gn1}",
                        RegExTest.toSupplementaries("abzzzabzzzab"));

        RegExTest.checkReplaceFirst("(?<gn1>" + RegExTest.toSupplementaries("ab") +
                           ")(?<gn2>" + RegExTest.toSupplementaries("c") + "*)",
                          RegExTest.toSupplementaries("abccczzzabcczzzabccc"),
                          "${gn2}",
                          RegExTest.toSupplementaries("ccczzzabcczzzabccc"));


        RegExTest.checkReplaceAll("(?<gn1>" + RegExTest.toSupplementaries("ab") +
                        ")(?<gn2>" + RegExTest.toSupplementaries("c") + "*)",
                        RegExTest.toSupplementaries("abccczzzabcczzzabccc"),
                        "${gn2}",
                        RegExTest.toSupplementaries("ccczzzcczzzccc"));

        RegExTest.checkReplaceFirst("(?<dog>Dog)AndCat",
                          "zzzDogAndCatzzzDogAndCatzzz",
                          "${dog}",
                          "zzzDogzzzDogAndCatzzz");


        RegExTest.checkReplaceAll("(?<dog>Dog)AndCat",
                          "zzzDogAndCatzzzDogAndCatzzz",
                          "${dog}",
                          "zzzDogzzzDogzzz");

        // backref in Matcher & String
        if (!"abcdefghij".replaceFirst("cd(?<gn>ef)gh", "${gn}").equals("abefij") ||
            !"abbbcbdbefgh".replaceAll("(?<gn>[a-e])b", "${gn}").equals("abcdefgh"))
            Assert.fail();

        // negative
        RegExTest.checkExpectedFail("(?<groupnamehasnoascii.in>abc)(def)");
        RegExTest.checkExpectedFail("(?<groupnamehasnoascii_in>abc)(def)");
        RegExTest.checkExpectedFail("(?<6groupnamestartswithdigit>abc)(def)");
        RegExTest.checkExpectedFail("(?<gname>abc)(def)\\k<gnameX>");
        RegExTest.checkExpectedFail("(?<gname>abc)(?<gname>def)\\k<gnameX>");
        RegExTest.checkExpectedIAE(PF.compile("(?<gname>abc)(def)").matcher("abcdef"),
                         "gnameX");
        RegExTest.checkExpectedNPE(PF.compile("(?<gname>abc)(def)").matcher("abcdef"));
//        RegExTest.report("NamedGroupCapture");
    }

    // This is for bug 6919132
    @Test public void nonBmpClassComplementTest() throws Exception {

    	Pattern p = PF.compile("\\P{Lu}");
        Matcher m = p.matcher(new String(new int[] {0x1d400}, 0, 1));

        if (m.find()) Assert.assertNotEquals(1, m.start());

        // from a unicode category
        p = PF.compile("\\P{Lu}");
        m = p.matcher(new String(new int[] {0x1d400}, 0, 1));
        Assert.assertFalse(m.find());
        Assert.assertTrue(m.hitEnd());

        // block
        p = PF.compile("\\P{InMathematicalAlphanumericSymbols}");
        m = p.matcher(new String(new int[] {0x1d400}, 0, 1));
        if (m.find()) Assert.assertNotEquals(1, m.start());

        // JRE 8 doesn't know script "GRANTHA".
        if (JRE > 8) {
	        p = PF.compile("\\P{sc=GRANTHA}");
	        m = p.matcher(new String(new int[] {0x11350}, 0, 1));
	        if (m.find()) Assert.assertNotEquals(1, m.start());
        }

//        RegExTest.report("NonBmpClassComplement");
    }

    @Test public void unicodePropertiesTest() throws Exception {
        // different forms
        if (!PF.compile("\\p{IsLu}").matcher("A").matches() ||
            !PF.compile("\\p{Lu}").matcher("A").matches() ||
            !PF.compile("\\p{gc=Lu}").matcher("A").matches() ||
            !PF.compile("\\p{general_category=Lu}").matcher("A").matches() ||
            !PF.compile("\\p{IsLatin}").matcher("B").matches() ||
            !PF.compile("\\p{sc=Latin}").matcher("B").matches() ||
            !PF.compile("\\p{script=Latin}").matcher("B").matches() ||
            !PF.compile("\\p{InBasicLatin}").matcher("c").matches() ||
            !PF.compile("\\p{blk=BasicLatin}").matcher("c").matches() ||
            !PF.compile("\\p{block=BasicLatin}").matcher("c").matches())
            Assert.fail();

        Matcher common  = PF.compile("\\p{script=Common}").matcher("");
        Matcher unknown = PF.compile("\\p{IsUnknown}").matcher("");
        Matcher lastSM  = common;
        Character.UnicodeScript lastScript = Character.UnicodeScript.of(0);

        Matcher latin  = PF.compile("\\p{block=basic_latin}").matcher("");
        Matcher greek  = PF.compile("\\p{InGreek}").matcher("");
        Matcher lastBM = latin;
        Character.UnicodeBlock lastBlock = Character.UnicodeBlock.of(0);

        for (int cp = 1; cp < Character.MAX_CODE_POINT; cp++) {
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
                 m  = PF.compile("\\p{Is" + script.name() + "}").matcher(str);
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

            // Unicode Block
            Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
            if (block == null) {
                //System.out.printf("Not a Block: cp=%x%n", cp);
                continue;
            }
            if (block == lastBlock) {
                 m = lastBM;
                 m.reset(str);
            } else {
                 m  = PF.compile("\\p{block=" + block.toString() + "}").matcher(str);
            }
            if (!m.matches()) {
                Assert.fail();
            }
            other = (block == Character.UnicodeBlock.BASIC_LATIN)? greek : latin;
            other.reset(str);
            if (other.matches()) {
                Assert.fail();
            }
            lastBM = m;
            lastBlock = block;
        }
//        RegExTest.report("unicodeProperties");
    }

    @Test public void unicodeHexNotationTest() throws Exception {

        // negative
        RegExTest.checkExpectedFail("\\x{-23}");
        RegExTest.checkExpectedFail("\\x{110000}");
        RegExTest.checkExpectedFail("\\x{}");
        RegExTest.checkExpectedFail("\\x{AB[ef]");

        // codepoint
        RegExTest.check("^\\x{1033c}$",              "\uD800\uDF3C", true);
        RegExTest.check("^\\xF0\\x90\\x8C\\xBC$",    "\uD800\uDF3C", false);
        RegExTest.check("^\\x{D800}\\x{DF3c}+$",     "\uD800\uDF3C", false);
        RegExTest.check("^\\xF0\\x90\\x8C\\xBC$",    "\uD800\uDF3C", false);

        // in class
        RegExTest.check("^[\\x{D800}\\x{DF3c}]+$",   "\uD800\uDF3C", false);
        RegExTest.check("^[\\xF0\\x90\\x8C\\xBC]+$", "\uD800\uDF3C", false);
        RegExTest.check("^[\\x{D800}\\x{DF3C}]+$",   "\uD800\uDF3C", false);
        RegExTest.check("^[\\x{DF3C}\\x{D800}]+$",   "\uD800\uDF3C", false);
        RegExTest.check("^[\\x{D800}\\x{DF3C}]+$",   "\uDF3C\uD800", true);
        RegExTest.check("^[\\x{DF3C}\\x{D800}]+$",   "\uDF3C\uD800", true);

        for (int cp = 0; cp <= 0x10FFFF; cp += 23) { // cp++) {
             String s = "A" + new String(Character.toChars(cp)) + "B";
             String hexUTF16 = (cp <= 0xFFFF)? String.format("\\u%04x", cp)
                                             : String.format("\\u%04x\\u%04x",
                                               (int) Character.toChars(cp)[0],
                                               (int) Character.toChars(cp)[1]);
             String hexCodePoint = "\\x{" + Integer.toHexString(cp) + "}";
             if (!PF.matches("A" + hexUTF16 + "B", s))
                 Assert.fail();
             if (!PF.matches("A[" + hexUTF16 + "]B", s))
                 Assert.fail();
             if (!PF.matches("A" + hexCodePoint + "B", s))
                 Assert.fail();
             if (!PF.matches("A[" + hexCodePoint + "]B", s))
                 Assert.fail();
         }
//         RegExTest.report("unicodeHexNotation");
    }

    @Test public void unicodeClassesTest() throws Exception {

        Pattern lower  = PF.compile("\\p{Lower}");
        Pattern upper  = PF.compile("\\p{Upper}");
        Pattern ASCII  = PF.compile("\\p{ASCII}");
        Pattern alpha  = PF.compile("\\p{Alpha}");
        Pattern digit  = PF.compile("\\p{Digit}");
        Pattern alnum  = PF.compile("\\p{Alnum}");
        Pattern punct  = PF.compile("\\p{Punct}");
        Pattern graph  = PF.compile("\\p{Graph}");
        Pattern print  = PF.compile("\\p{Print}");
        Pattern blank  = PF.compile("\\p{Blank}");
        Pattern cntrl  = PF.compile("\\p{Cntrl}");
        Pattern xdigit = PF.compile("\\p{XDigit}");
        Pattern space  = PF.compile("\\p{Space}");
        Pattern bound  = PF.compile("\\b");
        Pattern word   = PF.compile("\\w++");
        // UNICODE_CHARACTER_CLASS
        Pattern lowerU  = PF.compile("\\p{Lower}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern upperU  = PF.compile("\\p{Upper}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern ASCIIU  = PF.compile("\\p{ASCII}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern alphaU  = PF.compile("\\p{Alpha}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern digitU  = PF.compile("\\p{Digit}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern alnumU  = PF.compile("\\p{Alnum}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern punctU  = PF.compile("\\p{Punct}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern graphU  = PF.compile("\\p{Graph}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern printU  = PF.compile("\\p{Print}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern blankU  = PF.compile("\\p{Blank}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern cntrlU  = PF.compile("\\p{Cntrl}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern xdigitU = PF.compile("\\p{XDigit}", Pattern.UNICODE_CHARACTER_CLASS);
        Pattern spaceU  = PF.compile("\\p{Space}",  Pattern.UNICODE_CHARACTER_CLASS);
        Pattern boundU  = PF.compile("\\b",         Pattern.UNICODE_CHARACTER_CLASS);
        Pattern wordU   = PF.compile("\\w",         Pattern.UNICODE_CHARACTER_CLASS);
        // embedded flag (?U)
        Pattern lowerEU  = PF.compile("(?U)\\p{Lower}", Pattern.UNICODE_CHARACTER_CLASS);
        Pattern graphEU  = PF.compile("(?U)\\p{Graph}", Pattern.UNICODE_CHARACTER_CLASS);
        Pattern wordEU   = PF.compile("(?U)\\w",        Pattern.UNICODE_CHARACTER_CLASS);

        Pattern bwb    = PF.compile("\\b\\w\\b");
        Pattern bwbU   = PF.compile("\\b\\w++\\b",     Pattern.UNICODE_CHARACTER_CLASS);
        Pattern bwbEU  = PF.compile("(?U)\\b\\w++\\b", Pattern.UNICODE_CHARACTER_CLASS);
        // properties
        Pattern lowerP  = PF.compile("\\p{IsLowerCase}");
        Pattern upperP  = PF.compile("\\p{IsUpperCase}");
        Pattern titleP  = PF.compile("\\p{IsTitleCase}");
        Pattern letterP = PF.compile("\\p{IsLetter}");
        Pattern alphaP  = PF.compile("\\p{IsAlphabetic}");
        Pattern ideogP  = PF.compile("\\p{IsIdeographic}");
        Pattern cntrlP  = PF.compile("\\p{IsControl}");
        Pattern spaceP  = PF.compile("\\p{IsWhiteSpace}");
        Pattern definedP = PF.compile("\\p{IsAssigned}");
        Pattern nonCCPP = PF.compile("\\p{IsNoncharacterCodePoint}");
        Pattern joinCrtl = PF.compile("\\p{IsJoinControl}");
        // javaMethod
        Pattern lowerJ  = PF.compile("\\p{javaLowerCase}");
        Pattern upperJ  = PF.compile("\\p{javaUpperCase}");
        Pattern alphaJ  = PF.compile("\\p{javaAlphabetic}");
        Pattern ideogJ  = PF.compile("\\p{javaIdeographic}");
        // GC/C
        Pattern gcC  = PF.compile("\\p{C}");

        for (int cp = 1; cp < 0x30000; cp++) {
            String str = new String(Character.toChars(cp));
            int type = Character.getType(cp);

            // lower
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isLower(cp),                   lower);
            assertPatternMatchesCodepoint(cp, Character.isLowerCase(cp),                 lowerU);
            assertPatternMatchesCodepoint(cp, Character.isLowerCase(cp),                 lowerP);
            assertPatternMatchesCodepoint(cp, Character.isLowerCase(cp),                 lowerEU);
            assertPatternMatchesCodepoint(cp, Character.isLowerCase(cp),                 lowerJ);
            // upper
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isUpper(cp),                   upper);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isUpper(cp),                 upperU);
            assertPatternMatchesCodepoint(cp, Character.isUpperCase(cp),                 upperP);
            assertPatternMatchesCodepoint(cp, Character.isUpperCase(cp),                 upperJ);
            // alpha
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isAlpha(cp),                   alpha);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isAlpha(cp),                 alphaU);
            assertPatternMatchesCodepoint(cp, Character.isAlphabetic(cp),                alphaP);
            assertPatternMatchesCodepoint(cp, Character.isAlphabetic(cp),                alphaJ);
            // digit
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isDigit(cp),                   digit);
            assertPatternMatchesCodepoint(cp, Character.isDigit(cp),                     digitU);
            // alnum
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isAlnum(cp),                   alnum);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isAlnum(cp),                 alnumU);
            // punct
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isPunct(cp),                   punct);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isPunct(cp),                 punctU);
            // graph
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isGraph(cp),                   graph);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isGraph(cp),                 graphU);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isGraph(cp),                 graphEU);
            // blank
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isType(cp, POSIX_ASCII.BLANK), blank);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isBlank(cp),                 blankU);
            // print
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isPrint(cp),                   print);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isPrint(cp),                 printU);
            // cntrl
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isCntrl(cp),                   cntrl);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isCntrl(cp),                 cntrlU);
            assertPatternMatchesCodepoint(cp, Character.CONTROL == type,                 cntrlP);
            // hexdigit
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isHexDigit(cp),                xdigit);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isHexDigit(cp),              xdigitU);
            // space
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isSpace(cp),                   space);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isSpace(cp),                 spaceU);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isSpace(cp),                 spaceP);
            // word
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isWord(cp),                    word);
if (cp == 127280) {
	System.currentTimeMillis();
}
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isWord(cp),                  wordU);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isWord(cp),                  wordEU);
            // bwordb
            assertPatternMatchesCodepoint(cp, POSIX_ASCII.isWord(cp),                    bwb);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isWord(cp),                  bwbU);
            // properties
            assertPatternMatchesCodepoint(cp, Character.isTitleCase(cp),                 titleP);
            assertPatternMatchesCodepoint(cp, Character.isLetter(cp),                    letterP);
            assertPatternMatchesCodepoint(cp, Character.isIdeographic(cp),               ideogP);
            assertPatternMatchesCodepoint(cp, Character.isIdeographic(cp),               ideogJ);
            assertPatternMatchesCodepoint(cp, type != Character.UNASSIGNED,              definedP);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isNoncharacterCodePoint(cp), nonCCPP);
            assertPatternMatchesCodepoint(cp, POSIX_Unicode.isJoinControl(cp),           joinCrtl);
            // gc_C
			assertPatternMatchesCodepoint(cp, (
				type == Character.CONTROL
				|| type == Character.FORMAT
				|| type == Character.PRIVATE_USE
				|| type == Character.SURROGATE
				|| type == Character.UNASSIGNED
			), gcC);
            Assert.assertEquals(
            	String.format("Code point %d (%08x) pattern=%s", cp, cp, gcC.pattern()),
        		(
    				type == Character.CONTROL
    				|| type == Character.FORMAT
    				|| type == Character.PRIVATE_USE
    				|| type == Character.SURROGATE
    				|| type == Character.UNASSIGNED
				),
                gcC.matcher(str).matches()
            );
        }

        // bounds/word align
        RegExTest.assertFindIndexes(" \u0180sherman\u0400 ", bound, 1, 10);
        assertPatternMatches(true, bwbU, "\u0180sherman\u0400");
        RegExTest.assertFindIndexes(" \u0180sh\u0345erman\u0400 ", bound, 1, 11);
        assertPatternMatches(true, bwbU, "\u0180sh\u0345erman\u0400");
        RegExTest.assertFindIndexes(" \u0724\u0739\u0724 ", bound, 1, 4);
        assertPatternMatches(true, bwbU, "\u0724\u0739\u0724");
        assertPatternMatches(true, bwbEU, "\u0724\u0739\u0724");
    }

    private void
    assertPatternMatches(boolean expected, Pattern pattern, String subject) {
        Assert.assertEquals(
        	String.format("Subject \"%s\" pattern=%s", subject, pattern),
    		expected,
            pattern.matcher(subject).matches()
        );
	}

	private void
    assertPatternMatchesCodepoint(int codePoint, boolean expected, Pattern pattern) {
        Assert.assertEquals(
        	String.format("Code point=%d(%08x, type %d) Pattern=\"%s\"", codePoint, codePoint, Character.getType(codePoint), pattern),
    		expected,
            pattern.matcher(new String(Character.toChars(codePoint))).matches()
        );
	}

	@Test public void unicodeCharacterNameTest() throws Exception {

		try {
	        for (int cp = 0; cp < Character.MAX_CODE_POINT; cp++) {
	
	        	if (!Character.isValidCodePoint(cp) || Character.getType(cp) == Character.UNASSIGNED) continue;
	
	        	String str = new String(Character.toChars(cp));
	
	            // single
            	Assert.assertTrue(PF.compile("\\N{" + Character.getName(cp) + "}").matcher(str).matches());

	            // class[c]
            	Assert.assertTrue(PF.compile("[\\N{" + Character.getName(cp) + "}]").matcher(str).matches());
	        }
		} catch (PatternSyntaxException pse) {
			Assert.assertTrue(pse.getMessage().contains("\"\\N{name}\" is only supported for JRE >= 9"));
		}

        // range
        try {
	        for (int i = 0; i < 10; i++) {
	            int start = RegExTest.generator.nextInt(20);
	            int end = start + RegExTest.generator.nextInt(200);
	            String p = "[\\N{" + Character.getName(start) + "}-\\N{" + Character.getName(end) + "}]";
	            for (int cp = start; cp < end; cp++) {
	            	// CP in range:
	                Assert.assertTrue(
                		"pattern=" + p + ", cp=" + cp,
                		PF.compile(p).matcher(new String(Character.toChars(cp))).matches()
            		);
	            }
	            // CP out of range:
                int cp = end + 10;
				String s = new String(Character.toChars(cp));
				Assert.assertFalse(
					"pattern=" + p + ", cp=" + cp,
					PF.compile(p).matcher(s).matches()
				);
	        }
        } catch (PatternSyntaxException pse) {
        	Assert.assertTrue(pse.getMessage().contains("\"\\N{name}\" is only supported for JRE >= 9"));
        }

        // slice
        for (int i = 0; i < 10; i++) {
            int n = RegExTest.generator.nextInt(256);
            int[] buf = new int[n];
            StringBuffer sb = new StringBuffer(1024);
            for (int j = 0; j < n; j++) {
                int cp = RegExTest.generator.nextInt(1000);
                if (!Character.isValidCodePoint(cp) || Character.getType(cp) == Character.UNASSIGNED) {
                    cp = 0x4e00;    // just use 4e00
                }
                sb.append("\\N{" + Character.getName(cp) + "}");
                buf[j] = cp;
            }
            String regex   = sb.toString();
            String subject = new String(buf, 0, buf.length);
            try {
                Assert.assertTrue(PF.compile(regex).matcher(subject).matches());
            } catch (PatternSyntaxException pse) {
            	Assert.assertTrue(pse.getMessage().contains("\"\\N{name}\" is only supported for JRE >= 9"));
            }
        }
//        RegExTest.report("unicodeCharacterName");
    }

    @Test public void horizontalAndVerticalWSTest() throws Exception {
        String hws = new String (new char[] {
                                     0x09, 0x20, 0xa0, 0x1680, 0x180e,
                                     0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005,
                                     0x2006, 0x2007, 0x2008, 0x2009, 0x200a,
                                     0x202f, 0x205f, 0x3000 });
        String vws = new String (new char[] {
                                     0x0a, 0x0b, 0x0c, 0x0d, 0x85, 0x2028, 0x2029 });
        if (!PF.compile("\\h+").matcher(hws).matches() ||
            !PF.compile("[\\h]+").matcher(hws).matches())
            Assert.fail();
        if (PF.compile("\\H").matcher(hws).find() ||
            PF.compile("[\\H]").matcher(hws).find())
            Assert.fail();
        if (!PF.compile("\\v+").matcher(vws).matches() ||
            !PF.compile("[\\v]+").matcher(vws).matches())
            Assert.fail();
        if (PF.compile("\\V").matcher(vws).find() ||
            PF.compile("[\\V]").matcher(vws).find())
            Assert.fail();
        String prefix = "abcd";
        String suffix = "efgh";
        String ng = "A";
        for (int i = 0; i < hws.length(); i++) {
            String c = String.valueOf(hws.charAt(i));
            Matcher m = PF.compile("\\h").matcher(prefix + c + suffix);
            if (!m.find() || !c.equals(m.group()))
                Assert.fail();
            m = PF.compile("[\\h]").matcher(prefix + c + suffix);
            if (!m.find() || !c.equals(m.group()))
                Assert.fail();

            m = PF.compile("\\H").matcher(hws.substring(0, i) + ng + hws.substring(i));
            if (!m.find() || !ng.equals(m.group()))
                Assert.fail();
            m = PF.compile("[\\H]").matcher(hws.substring(0, i) + ng + hws.substring(i));
            if (!m.find() || !ng.equals(m.group()))
                Assert.fail();
        }
        for (int i = 0; i < vws.length(); i++) {
            String c = String.valueOf(vws.charAt(i));
            Matcher m = PF.compile("\\v").matcher(prefix + c + suffix);
            if (!m.find() || !c.equals(m.group()))
                Assert.fail();
            m = PF.compile("[\\v]").matcher(prefix + c + suffix);
            if (!m.find() || !c.equals(m.group()))
                Assert.fail();

            m = PF.compile("\\V").matcher(vws.substring(0, i) + ng + vws.substring(i));
            if (!m.find() || !ng.equals(m.group()))
                Assert.fail();
            m = PF.compile("[\\V]").matcher(vws.substring(0, i) + ng + vws.substring(i));
            if (!m.find() || !ng.equals(m.group()))
                Assert.fail();
        }
        // \v in range is interpreted as 0x0B. This is the undocumented behavior
        if (!PF.compile("[\\v-\\v]").matcher(String.valueOf((char)0x0B)).matches())
            Assert.fail();
//        RegExTest.report("horizontalAndVerticalWSTest");
    }

    @Test public void linebreakTest() throws Exception {

    	String linebreaks = new String (new char[] { 0x0A, 0x0B, 0x0C, 0x0D, 0x85, 0x2028, 0x2029 });
        String crnl       = "\r\n";

        Assert.assertTrue(PF.compile("\\R+").matcher(linebreaks).matches());
		Assert.assertTrue(PF.compile("\\R").matcher(crnl).matches());
		Assert.assertTrue(PF.compile("\\Rabc").matcher(crnl + "abc").matches());
		Assert.assertTrue(PF.compile("\\Rabc").matcher("\rabc").matches());
		Assert.assertTrue(PF.compile("\\R\\R").matcher(crnl).matches());  // backtracking
		Assert.assertTrue(PF.compile("\\R\\n").matcher(crnl).matches());
		
		Assert.assertFalse(PF.compile("((?<!\\R)\\s)*").matcher(crnl).matches()); // WTF!?
    }

    // #7189363
    @Test public void branchTest() throws Exception {
        if (!PF.compile("(a)?bc|d").matcher("d").find() ||     // greedy
            !PF.compile("(a)+bc|d").matcher("d").find() ||
            !PF.compile("(a)*bc|d").matcher("d").find() ||
            !PF.compile("(a)??bc|d").matcher("d").find() ||    // reluctant
            !PF.compile("(a)+?bc|d").matcher("d").find() ||
            !PF.compile("(a)*?bc|d").matcher("d").find() ||
            !PF.compile("(a)?+bc|d").matcher("d").find() ||    // possessive
            !PF.compile("(a)++bc|d").matcher("d").find() ||
            !PF.compile("(a)*+bc|d").matcher("d").find() ||
            !PF.compile("(a)?bc|d").matcher("d").matches() ||  // greedy
            !PF.compile("(a)+bc|d").matcher("d").matches() ||
            !PF.compile("(a)*bc|d").matcher("d").matches() ||
            !PF.compile("(a)??bc|d").matcher("d").matches() || // reluctant
            !PF.compile("(a)+?bc|d").matcher("d").matches() ||
            !PF.compile("(a)*?bc|d").matcher("d").matches() ||
            !PF.compile("(a)?+bc|d").matcher("d").matches() || // possessive
            !PF.compile("(a)++bc|d").matcher("d").matches() ||
            !PF.compile("(a)*+bc|d").matcher("d").matches() ||
            !PF.compile("(a)?bc|de").matcher("de").find() ||   // others
            !PF.compile("(a)??bc|de").matcher("de").find() ||
            !PF.compile("(a)?bc|de").matcher("de").matches() ||
            !PF.compile("(a)??bc|de").matcher("de").matches())
            Assert.fail();
//        RegExTest.report("branchTest");
    }

    // This test is for 8007395
    @Test public void groupCurlyNotFoundSuppTest() throws Exception {
        String input = "test this as \ud83d\ude0d";
        for (String pStr : new String[] { "test(.)+(@[a-zA-Z.]+)",
                                          "test(.)*(@[a-zA-Z.]+)",
                                          "test([^B])+(@[a-zA-Z.]+)",
                                          "test([^B])*(@[a-zA-Z.]+)",
                                          "test(\\P{IsControl})+(@[a-zA-Z.]+)",
                                          "test(\\P{IsControl})*(@[a-zA-Z.]+)",
                                        }) {
            Matcher m = PF.compile(pStr, Pattern.CASE_INSENSITIVE)
                               .matcher(input);
            try {
                if (m.find()) {
                    Assert.fail();
                }
            } catch (Exception x) {
                Assert.fail();
            }
        }
//        RegExTest.report("GroupCurly NotFoundSupp");
    }

    // This test is for 8023647
    @Test public void groupCurlyBackoffTest() throws Exception {
        if (!"abc1c".matches("(\\w)+1\\1") ||
            "abc11".matches("(\\w)+1\\1")) {
            Assert.fail();
        }
//        RegExTest.report("GroupCurly backoff");
    }

    // This test is for 8012646
    @Test public void
    patternAsPredicate() throws Exception {
        Predicate<String> p = PF.compile("[a-z]+").asPredicate();   // SMC

        Assert.assertFalse(p.test(""));
        Assert.assertTrue(p.test("word"));
        Assert.assertFalse(p.test("1234"));
        Assert.assertTrue(p.test("word1234"));
    }

    // This test is for 8184692
    @Test public void
    patternAsMatchPredicate() throws Exception {
        Predicate<String> p = PF.compile("[a-z]+").asMatchPredicate();    // SMC

        Assert.assertFalse(p.test(""));
        Assert.assertTrue(p.test("word"));
        Assert.assertFalse(p.test("1234word"));
        Assert.assertFalse(p.test("1234"));
    }


    // This test is for 8035975
    @Test public void invalidFlags() throws Exception {
        for (int flag = 1; flag != 0; flag <<= 1) {
            switch (flag) {
            case Pattern.CASE_INSENSITIVE:
            case Pattern.MULTILINE:
            case Pattern.DOTALL:
            case Pattern.UNICODE_CASE:
            case Pattern.CANON_EQ:
            case Pattern.UNIX_LINES:
            case Pattern.LITERAL:
            case Pattern.UNICODE_CHARACTER_CLASS:
            case Pattern.COMMENTS:
                // valid flag, continue
                break;
            default:
                try {
                    PF.compile(".", flag);
                    Assert.fail();
                } catch (IllegalArgumentException expected) {
                }
            }
        }
//        RegExTest.report("Invalid compile flags");
    }

    // This test is for 8158482
    @Test public void embeddedFlags() throws Exception {
        for (String regex : new String[] {
            "(?i).(?-i).",
            "(?m).(?-m).",
            "(?s).(?-s).",
            "(?d).(?-d).",
            "(?u).(?-u).",
            "(?c).(?-c).",
            "(?x).(?-x).",
            "(?U).(?-U).",
            "(?imsducxU).(?-imsducxU).",
        }) {
            try {
                PF.compile(regex);
            } catch (PatternSyntaxException x) {
            	if (!x.getMessage().startsWith("Flag \"c\" (CANON_EQ) is not supported")) Assert.fail(x.getMessage());
            }
        }
    }

    @Test public void grapheme() throws Exception {
        final int[] lineNumber = new int[1];

//        Stream.concat(
//            Files.lines(UCDFiles.GRAPHEME_BREAK_TEST),
//            Files.lines(Paths.get(System.getProperty("test.src", "."), "GraphemeTestCases.txt"))
//        )
        (
    		Files
    		.lines(Paths.get(/*System.getProperty("test.src", ".")*/ "target/test-classes", "GraphemeTestCases.txt"))
		).forEach(ln -> {
            lineNumber[0]++;
            if (ln.length() == 0 || ln.startsWith("#")) {
                return;
            }
            ln = ln.replaceAll("\\s+|\\([a-zA-Z]+\\)|\\[[a-zA-Z]]+\\]|#.*", "");
            // System.out.println(str);
            String[] strs = ln.split("\u00f7|\u00d7");
            StringBuilder src = new StringBuilder();
            ArrayList<String> graphemes = new ArrayList<>();
            StringBuilder buf = new StringBuilder();
            int offBk = 0;
            for (String str : strs) {
                if (str.length() == 0)  // first empty str
                    continue;
                int cp = Integer.parseInt(str, 16);
                src.appendCodePoint(cp);
                buf.appendCodePoint(cp);
                offBk += (str.length() + 1);
                if (ln.charAt(offBk) == '\u00f7') {    // DIV
                    graphemes.add(buf.toString());
                    buf = new StringBuilder();
                }
            }
            try {
	            Pattern p = PF.compile("\\X");
	            // (1) test \X directly
	            Matcher m = p.matcher(src.toString());
	            for (String g : graphemes) {
	                // System.out.printf("     grapheme:=[%s]%n", g);
	                String group = null;
	                Assert.assertTrue("Not found \\X [" + ln + "] (line " + lineNumber[0] + ")", m.find());
	                
	                // Fails with JUR 17 /* AU */:
//	                Assert.assertEquals("Group 0 \\X [" + ln + "] (line " + lineNumber[0] + ")", g, m.group());
	            }
	            // Fails with JUR 17 /* AU */:
//            	Assert.assertFalse(m.find());

	            // test \b{g} without \X via Pattern
	            Pattern pbg = PF.compile("\\b{g}");
	            m = pbg.matcher(src.toString());
	            m.find();
	            int prev = m.end();
	            for (String g : graphemes) {
	                String group = null;
	                Assert.assertTrue("Not found \\b{g} [" + ln + "] (line " + lineNumber[0] + ")", m.find());
	                // Fails with JUR 17 /* AU */:
//	                Assert.assertEquals("Group 0 \\b{g} [" + ln + "] (line " + lineNumber[0] + ")", g, group);
	                if (!"".equals(m.group())) {
	                    Assert.fail();
	                }
	                prev = m.end();
	            }
	            // Fails with JUR 17 /* AU */:
//      	      Assert.assertFalse(m.find());
            } catch (UnsupportedOperationException uoe) {
            	Assert.assertEquals("Graphemes only available in Java 9+", uoe.getMessage());
            }

// Cannot adapt to j.u.Scanner.hasNext(Pattern)
//            // (2) test \b{g} + \X  via Scanner
//            try (Scanner s = new Scanner(src.toString()).useDelimiter("\\b{g}")) {
//                for (String g : graphemes) {
//                    String next = null;
//                    if (!s.hasNext(p) || !(next = s.next(p)).equals(g)) {
//                        System.out.println("Failed \\b{g} [" + ln + "] : "
//                                + "expected: " + g + " - actual: " + next
//                                + " (line " + lineNumber[0] + ")");
//                        Assert.fail();
//                    }
//                }
//                if (s.hasNext(p)) {
//                    Assert.fail();
//                }
//            }

            // "Scanner.useDelimiter(regex)" is hard-wired to JUR; don't test it /* AU */
            // test \b{g} without \X via Scanner
//            try (Scanner s = new Scanner(src.toString()).useDelimiter("\\b{g}")) {
//                for (String g : graphemes) {
//                    Assert.assertTrue(s.hasNext());
//                    Assert.assertEquals("Failed \\b{g} [" + ln + "] : (line " + lineNumber[0] + ")", g, s.next());
//                }
//                Assert.assertFalse(s.hasNext());
//            }
        });

        // some sanity checks
        try {
	        if (!PF.compile("\\X{10}").matcher("abcdefghij").matches() ||
	            !PF.compile("\\b{g}(?:\\X\\b{g}){5}\\b{g}").matcher("abcde").matches() ||
	            !PF.compile("(?:\\X\\b{g}){2}").matcher("\ud800\udc00\ud801\udc02").matches())
	            Assert.fail();
        } catch (UnsupportedOperationException uoe) {
        	// Graphemes are only supported in JRE 9+. /* AU */
        	Assert.assertEquals("Graphemes only available in Java 9+", uoe.getMessage());
        }
        // make sure "\b{n}" still works
        if (!PF.compile("\\b{1}hello\\b{1} \\b{1}world\\b{1}").matcher("hello world").matches())
            Assert.fail();
//            RegExTest.report("Unicode extended grapheme cluster");
    }

    // hangup/timeout if go into exponential backtracking
    @Test public void expoBacktracking0() throws Exception {
    	// 6328855
    	expoBacktracking(
			"(.*\n*)*",
            "this little fine string lets\r\njava.lang.String.matches\r\ncrash\r\n(We don't know why but adding \r* to the regex makes it work again)",
            false
        );
    }
    @Test public void expoBacktracking1() throws Exception {
        // 6192895
    	expoBacktracking(
            " *([a-zA-Z0-9/\\-\\?:\\(\\)\\.,'\\+\\{\\}]+ *)+",
            "Hello World this is a test this is a test this is a test A",
            true
        );
    }
    @Test public void expoBacktracking2() throws Exception {
    	expoBacktracking(
            " *([a-zA-Z0-9/\\-\\?:\\(\\)\\.,'\\+\\{\\}]+ *)+",
            "Hello World this is a test this is a test this is a test \u4e00 ",
            false
        );
    }
    @Test public void expoBacktracking3() throws Exception {
    	expoBacktracking(
            " *([a-z0-9]+ *)+",
            "hello world this is a test this is a test this is a test A",
            false
        );
    }
    @Test public void expoBacktracking4() throws Exception {
    	// 4771934 [FIXED] #5013651?
    	expoBacktracking(
            "^(\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,4})+[,;]?)+$",
            "abc@efg.abc,efg@abc.abc,abc@xyz.mno;abc@sdfsd.com",
            true
        );
    }
    @Test public void expoBacktracking5() throws Exception {
    	// 4866249 [FIXED]
    	expoBacktracking(
            "<\\s*" + "(meta|META)" + "(\\s|[^>])+" + "(CHARSET|charset)=" + "(\\s|[^>])+>",
            "<META http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-5\">",
            true
        );
    }
    @Test public void expoBacktracking6() throws Exception {
    	expoBacktracking(
            "^(\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,4})+[,;]?)+$",
            "abc@efg.abc,efg@abc.abc,abc@xyz.mno;sdfsd.com",
            false
        );
    }
    @Test public void expoBacktracking7() throws Exception {
    	// 6345469
    	expoBacktracking(
            "((<[^>]+>)?(((\\s)?)*(\\&nbsp;)?)*((\\s)?)*)+",
            "&nbsp;&nbsp; < br/> &nbsp; < / p> <p> <html> <adfasfdasdf>&nbsp; </p>",
            true // --> matched
        );
    }
    @Test public void expoBacktracking8() throws Exception {
    	expoBacktracking(
            "((<[^>]+>)?(((\\s)?)*(\\&nbsp;)?)*((\\s)?)*)+",
            "&nbsp;&nbsp; < br/> &nbsp; < / p> <p> <html> <adfasfdasdf>&nbsp; p </p>",
            false
        );
    }
    @Test public void expoBacktracking9() throws Exception {
    	// 5026912
    	expoBacktracking(
            "^\\s*" + "(\\w|\\d|[\\xC0-\\xFF]|/)+" + "\\s+|$",
            "156580451111112225588087755221111111566969655555555",
            false
        );
    }
    @Test public void expoBacktracking10() throws Exception {
    	// 6988218
    	expoBacktracking(
            "^([+-]?((0[xX](\\p{XDigit}+))|(((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)))|[n|N]?'([^']*(?:'')*[^']*)*')",
            "'%)) order by ANGEBOT.ID",
            false    // find
        );
    }
    @Test public void expoBacktracking11() throws Exception {
    	// 6693451
    	expoBacktracking(
            "^(\\s*foo\\s*)*$",
            "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo",
            true
        );
    }
    @Test public void expoBacktracking12() throws Exception {
    	expoBacktracking(
            "^(\\s*foo\\s*)*$",
            "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo fo",
            false
        );
    }
    @Test public void expoBacktracking13() throws Exception {
    	// 7006761
    	expoBacktracking("(([0-9A-Z]+)([_]?+)*)*", "FOOOOO_BAAAR_FOOOOOOOOO_BA_", true);
    }
    @Test public void expoBacktracking14() throws Exception {
    	expoBacktracking("(([0-9A-Z]+)([_]?+)*)*", "FOOOOO_BAAAR_FOOOOOOOOO_BA_ ", false);
    }
    @Test public void expoBacktracking15() throws Exception {
    	// 8140212
    	expoBacktracking(
            "(?<before>.*)\\{(?<reflection>\\w+):(?<innerMethod>\\w+(\\.?\\w+(\\(((?<args>(('[^']*')|((/|\\w)+))(,(('[^']*')|((/|\\w)+)))*))?\\))?)*)\\}(?<after>.*)",
              "{CeGlobal:getSodCutoff.getGui.getAmqp.getSimpleModeEnabled()",
              false
        );
    }
    @Test public void expoBacktracking16() throws Exception {
    	expoBacktracking("^(a+)+$", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", true);
    }
    @Test public void expoBacktracking17() throws Exception {
    	expoBacktracking("^(a+)+$", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!", false);
    }
    @Test public void expoBacktracking18() throws Exception {
    	expoBacktracking("(x+)*y",  "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxy", true);
    }
    @Test public void expoBacktracking19() throws Exception {
    	expoBacktracking("(x+)*y",  "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxz", false);
    }
    @Test public void expoBacktracking20() throws Exception {
    	expoBacktracking("(x+x+)+y", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxy", true);
    }
    @Test public void expoBacktracking21() throws Exception {
    	expoBacktracking("(x+x+)+y", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxz", false);
    }
    @Test public void expoBacktracking22() throws Exception {
    	expoBacktracking("(([0-9A-Z]+)([_]?+)*)*", "--------------------------------------", false);
    }
    /* not fixed
    @Test public void expoBacktracking23() throws Exception {
    	expoBacktracking(

            //8132141   --->    second level exponential backtracking
            "(h|h|ih(((i|a|c|c|a|i|i|j|b|a|i|b|a|a|j))+h)ahbfhba|c|i)*",
            "hchcchicihcchciiicichhcichcihcchiihichiciiiihhcchicchhcihchcihiihciichhccciccichcichiihcchcihhicchcciicchcccihiiihhihihihichicihhcciccchihhhcchichchciihiicihciihcccciciccicciiiiiiiiicihhhiiiihchccchchhhhiiihchihcccchhhiiiiiiiicicichicihcciciihichhhhchihciiihhiccccccciciihhichiccchhicchicihihccichicciihcichccihhiciccccccccichhhhihihhcchchihihiihhihihihicichihiiiihhhhihhhchhichiicihhiiiiihchccccchichci"
		);
	}
    */

	private static void expoBacktracking(String regex, String subject, boolean expected) {
            
//System.out.printf("regex=%-20s, subject=%-20s expected=%s%n", "\"" + regex + "\"", "\"" + subject + "\"", expected);
        
        Assert.assertEquals(
    		"regex=" + regex + ", subject=" + subject,
    		expected,
    		PF.compile(regex).matcher(subject).matches()
		);
    }

    @Test public void invalidGroupName() {
        // Invalid start of a group name
        for (String groupName : new String[] {
    		"",
    		".",
    		"0",
    		"\u0040",
    		"\u005b",
    		"\u0060",
    		"\u007b",
    		"\u0416",
        }) {
            for (String pat : new String[] {
        		"(?<" + groupName + ">)",
                "\\k<" + groupName + ">",
            }) {
                try {
                    PF.compile(pat);
                    Assert.fail("PatternSyntaxException expected: pat=\"" + pat + "\"");
                } catch (PatternSyntaxException e) {
                    String message = e.getMessage();
					Assert.assertTrue(
                		message,
                		(
            				message.startsWith("capturing group name does not start with a Latin letter")
            				|| message.contains("Invalid character sequence")
        				)
            		);
                }
            }
        }
        // Invalid char in a group name
        for (String groupName : new String[] {
    		"a.",
    		"b\u0040",
    		"c\u005b",
            "d\u0060",
            "e\u007b",
            "f\u0416",
        }) {
            for (String pat : new String[] {
        		"(?<" + groupName + ">)",
                "\\k<" + groupName + ">"
            }) {
                try {
                    PF.compile(pat);
                    Assert.fail();
                } catch (PatternSyntaxException e) {
                	String message = e.getMessage();
					Assert.assertTrue(
						message,
            			(
        					message.startsWith("named capturing group is missing trailing '>'")
							|| message.contains("Invalid character sequence")
						)
                    );
                }
            }
        }
//        RegExTest.report("Invalid capturing group names");
    }

    @Test public void illegalRepetitionRange() {
        // huge integers > (2^31 - 1)
        String n = BigInteger.valueOf(1L << 32)
            .toString();
        String m = BigInteger.valueOf(1L << 31)
            .add(new BigInteger(80, RegExTest.generator))
            .toString();
        for (String rep : new String[] {
//    		"", "x", ".", ",", "-1", "2,1",
            n, n + ",", "0," + n, n + "," + m, m, m + ",", "0," + m
        }) {
            String pat = ".{" + rep + "}";
            try {
                PF.compile(pat);
                Assert.fail("Expected to fail. Pattern: " + pat);
            } catch (PatternSyntaxException e) {
                Assert.assertTrue(
            		"Unexpected error message: " + e.getMessage(),
            		(
        				e.getMessage().startsWith("Illegal repetition")
        				|| e.getMessage().contains("Unexpected character \"{\"")
        				|| e.getMessage().contains("max<min")
        				|| e.getMessage().startsWith("For input string: \"")
    				)
        		);
            } catch (Exception t) {
            	t.printStackTrace();
                Assert.fail("Unexpected exception: " + t);
            }
        }
//        RegExTest.report("illegalRepetitionRange");
    }

    @Test public void surrogatePairWithCanonEq() {
        try {
            PF.compile("\ud834\udd21", Pattern.CANON_EQ);
        } catch (IllegalArgumentException iae) {
        	if (!iae.getMessage().equals("Unsupported flag 128")) throw iae;
        }
    }

    // This test is for 8235812
    @Test public void lineBreakWithQuantifier() {
        // key:    pattern
        // value:  lengths of input that must match the pattern
        Object[] cases = {
//TODO TMP            "\\R?",      new Integer[] { 0, 1 },       // 0
            "\\R*",      new Integer[] { 0, 1, 2, 3 },
            "\\R+",      new Integer[] { 1, 2, 3 },
            "\\R{0}",    new Integer[] { 0 },
            "\\R{1}",    new Integer[] { 1 },
//            "\\R{2}",    new Integer[] { 2 },                // Fails for JRE 17 /* AU */
//            "\\R{3}",    new Integer[] { 3 },                // Fails for JRE 17 /* AU */
            "\\R{0,}",   new Integer[] { 0, 1, 2, 3 }, // 5
            "\\R{1,}",   new Integer[] { 1, 2, 3 },
//            "\\R{2,}",   new Integer[] { 2, 3 },             // Fails for JRE 17 /* AU */
//            "\\R{3,}",   new Integer[] { 3 },                // Fails for JRE 17 /* AU */
            "\\R{0,0}",  new Integer[] { 0 },
            "\\R{0,1}",  new Integer[] { 0, 1 },
            "\\R{0,2}",  new Integer[] { 0, 1, 2 },
            "\\R{0,3}",  new Integer[] { 0, 1, 2, 3 }, // 10
            "\\R{1,1}",  new Integer[] { 1 },
            "\\R{1,2}",  new Integer[] { 1, 2 },
            "\\R{1,3}",  new Integer[] { 1, 2, 3 },
//            "\\R{2,2}",  new Integer[] { 2 },                // Fails for JRE 17 /* AU */
//            "\\R{2,3}",  new Integer[] { 2, 3 },             // Fails for JRE 17 /* AU */
//            "\\R{3,3}",  new Integer[] { 3 },                // Fails for JRE 17 /* AU */
            "\\R",       new Integer[] { 1 },
            "\\R\\R",    new Integer[] { 2 },          // 15
            "\\R\\R\\R", new Integer[] { 3 },
        };

        // key:    length of input
        // value:  all possible inputs of given length
        Map<Integer, List<String>> inputs = new HashMap<>();
        String[] Rs = { "\r\n", "\r", "\n",
                        "\u000B", "\u000C", "\u0085", "\u2028", "\u2029" };
        StringBuilder sb = new StringBuilder();
        for (int len = 0; len <= 3; ++len) {
            int[] idx = new int[len + 1];
            do {
                sb.setLength(0);
                for (int j = 0; j < len; ++j)
                    sb.append(Rs[idx[j]]);
                inputs.computeIfAbsent(len, ArrayList::new).add(sb.toString());
                idx[0]++;
                for (int j = 0; j < len; ++j) {
                    if (idx[j] < Rs.length)
                        break;
                    idx[j] = 0;
                    idx[j+1]++;
                }
            } while (idx[len] == 0);
        }

        // exhaustive testing
        for (int i = 0; i < cases.length; i += 2) {

        	String    patStr = (String)    cases[i];
			Integer[] lens   = (Integer[]) cases[i + 1];

            Pattern[] pats = patStr.endsWith("R")
                ? new Pattern[] { PF.compile(patStr) }  // no quantifiers
                : new Pattern[] { PF.compile(patStr),          // greedy
                                  PF.compile(patStr + "?") };  // reluctant
            Matcher m = pats[0].matcher("");
            for (Pattern p : pats) {
                m.usePattern(p);
                for (int len : lens) {
                    for (String in : inputs.get(len)) {
                        Assert.assertTrue(
                    		"Case #" + (i / 2) + ": Expected to match '" + in + "' (length " + len + ") =~ /" + p + "/",
                    		m.reset(in).matches()
                		);
                    }
                }
            }
        }
//        RegExTest.report("lineBreakWithQuantifier");
    }

    // This test is for 8214245
    @Test public void caseInsensitivePMatch() {
        for (String input : new String[] { "abcd", "AbCd", "ABCD" }) {
            for (String pattern : new String[] {
        		"abcd", "aBcD", "[a-d]{4}",
                "(?:a|b|c|d){4}",
//                "\\p{Lower}{4}", "\\p{Ll}{4}", "\\p{IsLl}{4}", "\\p{gc=Ll}{4}",         // Does not match for JRE 17 /* AU */
//                "\\p{general_category=Ll}{4}", "\\p{IsLowercase}{4}", "\\p{javaLowerCase}{4}",

//                "\\p{Upper}{4}", "\\p{Lu}{4}", "\\p{IsLu}{4}", "\\p{gc=Lu}{4}",
//                "\\p{general_category=Lu}{4}", "\\p{IsUppercase}{4}", "\\p{javaUpperCase}{4}",

//                "\\p{Lt}{4}", "\\p{IsLt}{4}", "\\p{gc=Lt}{4}", "\\p{general_category=Lt}{4}",
//                "\\p{IsTitlecase}{4}", "\\p{javaTitleCase}{4}",

//                "[\\p{Lower}]{4}", "[\\p{Ll}]{4}", "[\\p{IsLl}]{4}", "[\\p{gc=Ll}]{4}",
//                "[\\p{general_category=Ll}]{4}", "[\\p{IsLowercase}]{4}", "[\\p{javaLowerCase}]{4}",

//                "[\\p{Upper}]{4}", "[\\p{Lu}]{4}", "[\\p{IsLu}]{4}", "[\\p{gc=Lu}]{4}",
//                "[\\p{general_category=Lu}]{4}", "[\\p{IsUppercase}]{4}", "[\\p{javaUpperCase}]{4}",
                
//                "[\\p{Lt}]{4}", "[\\p{IsLt}]{4}", "[\\p{gc=Lt}]{4}", "[\\p{general_category=Lt}]{4}",
//                "[\\p{IsTitlecase}]{4}", "[\\p{javaTitleCase}]{4}",
            }) {
                Assert.assertTrue(
            		"Expected to match: '" + input + "' =~ /" + pattern + "/",
            		PF.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(input).matches()
        		);
            }
        }

        for (String input : new String[] {
    		"\u01c7", // LATIN CAPITAL LETTER LJ
    		"\u01c8", // LATIN CAPITAL LETTER L WITH SMALL LETTER J
    		"\u01c9"  // LATIN SMALL LETTER LJ
        }) {
            for (String pattern : new String[] {
        		"\u01c7", "\u01c8", "\u01c9",
                "[\u01c7\u01c8]", "[\u01c7\u01c9]", "[\u01c8\u01c9]",
                "[\u01c7-\u01c8]", "[\u01c8-\u01c9]", "[\u01c7-\u01c9]",

//                "\\p{Lower}", "\\p{Ll}", "\\p{IsLl}", "\\p{gc=Ll}",      // Fails with JUR 17
//                "\\p{general_category=Ll}", "\\p{IsLowercase}", "\\p{javaLowerCase}",

//                "\\p{Upper}", "\\p{Lu}", "\\p{IsLu}", "\\p{gc=Lu}",
//                "\\p{general_category=Lu}", "\\p{IsUppercase}", "\\p{javaUpperCase}",

//                "\\p{Lt}", "\\p{IsLt}", "\\p{gc=Lt}",
//                "\\p{general_category=Lt}", "\\p{IsTitlecase}", "\\p{javaTitleCase}",

//                "[\\p{Lower}]", "[\\p{Ll}]", "[\\p{IsLl}]", "[\\p{gc=Ll}]",
//                "[\\p{general_category=Ll}]", "[\\p{IsLowercase}]", "[\\p{javaLowerCase}]",
                
//                "[\\p{Upper}]", "[\\p{Lu}]", "[\\p{IsLu}]", "[\\p{gc=Lu}]",
//                "[\\p{general_category=Lu}]", "[\\p{IsUppercase}]", "[\\p{javaUpperCase}]",
                
//                "[\\p{Lt}]", "[\\p{IsLt}]",
//                "[\\p{gc=Lt}]", "[\\p{general_category=Lt}]",
//                "[\\p{IsTitlecase}]", "[\\p{javaTitleCase}]",
            }) {
                Assert.assertTrue(
            		"Expected to match: " + toJavaLiteral(input) + " =~ /" + toJavaLiteral(pattern) + "/",
            		PF.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS) .matcher(input) .matches()
        		);
            }
        }
//        RegExTest.report("caseInsensitivePMatch");
    }

    private static String
    toJavaLiteral(String s) {
    	StringBuilder sb = new StringBuilder("\"");
    	for (int i = 0; i < s.length(); i++) {
    		char c = s.charAt(i);

    		int idx = "\r\n\\\t".indexOf(c);
			if (idx != -1) {
    			sb.append('\\').append("rn\\t".charAt(idx));
    		} else
    		if (c >= ' ' && c <= '\377') {
    			sb.append(c);
    		} else
    		{
    			sb.append("\\u").append(String.format("%04x", 0xffff & c));
    		}
    	}
    	return sb.append('"').toString();
    }
    // This test is for 8237599
    @Test public void surrogatePairOverlapRegion() {
        String input = "\ud801\udc37";

        Pattern p = PF.compile(".+");
        Matcher m = p.matcher(input);
        m.region(0, 1);

        boolean ok = m.find();
        if (!ok || !m.group(0).equals(input.substring(0, 1)))
        {
            Assert.fail();
            System.out.println("Input \"" + input + "\".substr(0, 1)" +
                    " expected to match pattern \"" + p + "\"");
            if (ok) {
                System.out.println("group(0): \"" + m.group(0) + "\"");
            }
        } else if (!m.hitEnd()) {
            Assert.fail();
            System.out.println("Expected m.hitEnd() == true");
        }

        p = PF.compile(".*(.)");
        m = p.matcher(input);
        m.region(1, 2);

        ok = m.find();
        if (!ok || !m.group(0).equals(input.substring(1, 2))
                || !m.group(1).equals(input.substring(1, 2)))
        {
            Assert.fail();
            System.out.println("Input \"" + input + "\".substr(1, 2)" +
                    " expected to match pattern \"" + p + "\"");
            if (ok) {
                System.out.println("group(0): \"" + m.group(0) + "\"");
                System.out.println("group(1): \"" + m.group(1) + "\"");
            }
        }
//        RegExTest.report("surrogatePairOverlapRegion");
    }
}
