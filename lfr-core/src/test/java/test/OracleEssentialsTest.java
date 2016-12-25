
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

// SUPPRESS CHECKSTYLE Javadoc:9999

package test;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

/**
 * This test case executes all the regex examples presented in <a
 * href="https://docs.oracle.com/javase/tutorial/essential/regex/index.html">Essential Classes: Lesson: Regular
 * Expressions</a>.
 */
public
class OracleEssentialsTest {

    @Test public void
    testStringLiterals() {
        OracleEssentialsTest.harness("foo", "foofoofoo");
        OracleEssentialsTest.harness("cat.", "cats");
    }

    @Test public void
    testSimpleClasses() {
        OracleEssentialsTest.harness("[bcr]at", "bat");
    }

    @Test public void
    testNegation() {
        OracleEssentialsTest.harness("[^bcr]at", "bat");
        OracleEssentialsTest.harness("[^bcr]at", "cat");
        OracleEssentialsTest.harness("[^bcr]at", "rat");
        OracleEssentialsTest.harness("[^bcr]at", "hat");
    }

    @Test public void
    testRanges() {
        OracleEssentialsTest.harness("[a-c]", "a");
        OracleEssentialsTest.harness("[a-c]", "b");
        OracleEssentialsTest.harness("[a-c]", "c");
        OracleEssentialsTest.harness("[a-c]", "d");
        OracleEssentialsTest.harness("foo[1-5]", "foo1");
        OracleEssentialsTest.harness("foo[1-5]", "foo5");
        OracleEssentialsTest.harness("foo[1-5]", "foo6");
        OracleEssentialsTest.harness("foo[^1-5]", "foo1");
        OracleEssentialsTest.harness("foo[^1-5]", "foo6");
    }

    @Test public void
    testUnions() {
        OracleEssentialsTest.harness("[0-4[6-8]]", "0");
        OracleEssentialsTest.harness("[0-4[6-8]]", "5");
        OracleEssentialsTest.harness("[0-4[6-8]]", "6");
        OracleEssentialsTest.harness("[0-4[6-8]]", "8");
        OracleEssentialsTest.harness("[0-4[6-8]]", "9");
    }

    @Test public void
    testIntersections() {
        OracleEssentialsTest.harness("[2-8&&[4-6]]", "3");
        OracleEssentialsTest.harness("[2-8&&[4-6]]", "4");
        OracleEssentialsTest.harness("[2-8&&[4-6]]", "5");
        OracleEssentialsTest.harness("[2-8&&[4-6]]", "6");
        OracleEssentialsTest.harness("[2-8&&[4-6]]", "7");
    }

    @Test public void
    testSubtractions() {
        OracleEssentialsTest.harness("[0-9&&[^345]]", "2");
        OracleEssentialsTest.harness("[0-9&&[^345]]", "3");
        OracleEssentialsTest.harness("[0-9&&[^345]]", "4");
        OracleEssentialsTest.harness("[0-9&&[^345]]", "5");
        OracleEssentialsTest.harness("[0-9&&[^345]]", "6");
        OracleEssentialsTest.harness("[0-9&&[^345]]", "9");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pre_char_classes.html">Essential Classes:
     * Lesson: Regular Expressions: Predefined Character Classes</a>
     */
    @Test public void
    testPredefinedCharacterClasses() {
        OracleEssentialsTest.harness(".", "@");
        OracleEssentialsTest.harness(".", "1");
        OracleEssentialsTest.harness(".", "a");
        OracleEssentialsTest.harness("\\d", "1");
        OracleEssentialsTest.harness("\\d", "a");
        OracleEssentialsTest.harness("\\D", "1");
        OracleEssentialsTest.harness("\\D", "a");
        OracleEssentialsTest.harness("\\s", " ");
        OracleEssentialsTest.harness("\\s", "a");
        OracleEssentialsTest.harness("\\S", " ");
        OracleEssentialsTest.harness("\\S", "a");
        OracleEssentialsTest.harness("\\w", "a");
        OracleEssentialsTest.harness("\\w", "!");
        OracleEssentialsTest.harness("\\W", "a");
        OracleEssentialsTest.harness("\\W", "!");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/quant.html">Essential Classes:
     * Lesson: Regular Expressions: Quantifiers</a>
     */
    @Test public void
    testQuantifiers() {
        OracleEssentialsTest.harness("a?", "");
        OracleEssentialsTest.harness("a*", "");
        OracleEssentialsTest.harness("a+", "");
    }

    @Test public void
    testZeroLengthMatches() {
        OracleEssentialsTest.harness("a?",     "a");
        OracleEssentialsTest.harness("a*",     "a");
        OracleEssentialsTest.harness("a+",     "a");
        OracleEssentialsTest.harness("a?",     "aaaaa");
        OracleEssentialsTest.harness("a*",     "aaaaa");
        OracleEssentialsTest.harness("a+",     "aaaaa");
        OracleEssentialsTest.harness("a?",     "ababaaaab");
        OracleEssentialsTest.harness("a*",     "ababaaaab");
        OracleEssentialsTest.harness("a+",     "ababaaaab");
        OracleEssentialsTest.harness("a{3}",   "aa");
        OracleEssentialsTest.harness("a{3}",   "aaa");
        OracleEssentialsTest.harness("a{3}",   "aaaa");
        OracleEssentialsTest.harness("a{3}",   "aaaaaaaaa");
        OracleEssentialsTest.harness("a{3,}",  "aaaaaaaaa");
        OracleEssentialsTest.harness("a{3,6}", "aaaaaaaaa");
    }

    @Test public void
    testCapturingGroupsAndCharacterClassesWithQuantifiers() {
        OracleEssentialsTest.harness("(dog){3}", "dogdogdogdogdogdog");
        OracleEssentialsTest.harness("dog{3}",   "dogdogdogdogdogdog");
        OracleEssentialsTest.harness("[abc]{3}", "abccabaaaccbbbc");
    }

    @Test public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers() {
        OracleEssentialsTest.harness(".*foo",  "xfooxxxxxxfoo");
        OracleEssentialsTest.harness(".*?foo", "xfooxxxxxxfoo");
        OracleEssentialsTest.harness(".*+foo", "xfooxxxxxxfoo");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/groups.html">Essential Classes:
     * Lesson: Regular Expressions: Capturing groups</a>
     */
    @Test public void
    testBackreferences() {
        OracleEssentialsTest.harness("(\\d\\d)\\1", "1212");
        OracleEssentialsTest.harness("(\\d\\d)\\1", "1234");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/bounds.html">Essential Classes:
     * Lesson: Regular Expressions: Boundary Matchers</a>
     */
    @Test public void
    testBoundaryMatchers() {
        OracleEssentialsTest.harness("^dog$",    "dog");
        OracleEssentialsTest.harness("^dog$",    "   dog");
        OracleEssentialsTest.harness("\\s*dog$", "   dog");
        OracleEssentialsTest.harness("^dog\\w*", "dogblahblah");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Methods of the Pattern Class</a>
     */
    @Test public void
    testCreatingAPatternWithFlags() {
        OracleEssentialsTest.harness("dog", "DoGDOg", java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html#embedded">Essential Classes:
     * Lesson: Regular Expressions: Embedded Flag Expressions</a>
     */
    @Test public void
    testEmbeddedFlagExpressions() {
        OracleEssentialsTest.harness("(?i)foo", "FOOfooFoOfoO");
        OracleEssentialsTest.harness("foo", "FOOfooFoOfoO");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Using the matches(String,CharSequence) Method</a>
     */
    @Test public void
    testUsingTheMatchesMethod() {
        Assert.assertTrue(de.unkrig.lfr.core.Pattern.matches("\\d","1"));
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Using the split(String) Method</a>
     */
    @Test public void
    testUsingTheSplitMethod() {

        Assert.assertArrayEquals(
            new Object[] { "one", "two", "three", "four", "five" },
            de.unkrig.lfr.core.Pattern.compile(":").split("one:two:three:four:five")
        );

        Assert.assertArrayEquals(
            new Object[] { "one", "two", "three", "four", "five" },
            de.unkrig.lfr.core.Pattern.compile("\\d").split("one9two4three7four1five")
        );
    }

    // ==========================================================================================================

    private static void
    harness(String regex, String subject) { OracleEssentialsTest.harness(regex, subject, 0); }

    private static void
    harness(String regex, String subject, int flags) {

        java.util.regex.Matcher            matcher1 = java.util.regex.Pattern.compile(regex, flags).matcher(subject);
        de.unkrig.lfr.core.Pattern.Matcher matcher2 = de.unkrig.lfr.core.Pattern.compile(regex, flags).matcher(subject);

        for (int matchCount = 0;; matchCount++) {
            String message = "Match #" + (matchCount + 1);

            boolean found1 = matcher1.find();
            boolean found2 = matcher2.find();
            Assert.assertEquals(message, found1, found2);
            if (!found1 || !found2) break;

            Assert.assertEquals(message, matcher1.group(), matcher2.group());
            Assert.assertEquals(message, matcher1.start(), matcher2.start());
            Assert.assertEquals(message, matcher1.end(), matcher2.end());
        }
    }
}
