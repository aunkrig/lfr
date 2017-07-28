
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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test case executes all the regex examples presented in <a
 * href="https://docs.oracle.com/javase/tutorial/essential/regex/index.html">Essential Classes: Lesson: Regular
 * Expressions</a>.
 */
public
class OracleEssentialsTest {

    @BeforeClass public static void
    setupBeforeClass() {
        OracleEssentials.beginStatistics();
    }

    @AfterClass public static void
    shutdownBeforeClass() {
        OracleEssentials.endStatistics();
    }

    @Test @SuppressWarnings("static-method") public void
    testStringLiterals() {
        OracleEssentials.harness("foo", "foofoofoo");
        OracleEssentials.harness("cat.", "cats");
    }

    @Test @SuppressWarnings("static-method") public void
    testSimpleClasses() {
        OracleEssentials.harness("[bcr]at", "bat");
    }

    @Test @SuppressWarnings("static-method") public void
    testNegation() {
        OracleEssentials.harness("[^bcr]at", "bat");
        OracleEssentials.harness("[^bcr]at", "cat");
        OracleEssentials.harness("[^bcr]at", "rat");
        OracleEssentials.harness("[^bcr]at", "hat");
    }

    @Test @SuppressWarnings("static-method") public void
    testRanges() {
        OracleEssentials.harness("[a-c]", "a");
        OracleEssentials.harness("[a-c]", "b");
        OracleEssentials.harness("[a-c]", "c");
        OracleEssentials.harness("[a-c]", "d");
        OracleEssentials.harness("foo[1-5]", "foo1");
        OracleEssentials.harness("foo[1-5]", "foo5");
        OracleEssentials.harness("foo[1-5]", "foo6");
        OracleEssentials.harness("foo[^1-5]", "foo1");
        OracleEssentials.harness("foo[^1-5]", "foo6");
    }

    @Test @SuppressWarnings("static-method") public void
    testUnions() {
        OracleEssentials.harness("[0-4[6-8]]", "0");
        OracleEssentials.harness("[0-4[6-8]]", "5");
        OracleEssentials.harness("[0-4[6-8]]", "6");
        OracleEssentials.harness("[0-4[6-8]]", "8");
        OracleEssentials.harness("[0-4[6-8]]", "9");
    }

    @Test @SuppressWarnings("static-method") public void
    testIntersections() {
        OracleEssentials.harness("[2-8&&[4-6]]", "3");
        OracleEssentials.harness("[2-8&&[4-6]]", "4");
        OracleEssentials.harness("[2-8&&[4-6]]", "5");
        OracleEssentials.harness("[2-8&&[4-6]]", "6");
        OracleEssentials.harness("[2-8&&[4-6]]", "7");
    }

    @Test @SuppressWarnings("static-method") public void
    testSubtractions() {
        OracleEssentials.harness("[0-9&&[^345]]", "2");
        OracleEssentials.harness("[0-9&&[^345]]", "3");
        OracleEssentials.harness("[0-9&&[^345]]", "4");
        OracleEssentials.harness("[0-9&&[^345]]", "5");
        OracleEssentials.harness("[0-9&&[^345]]", "6");
        OracleEssentials.harness("[0-9&&[^345]]", "9");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pre_char_classes.html">Essential Classes:
     * Lesson: Regular Expressions: Predefined Character Classes</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testPredefinedCharacterClasses() {
        OracleEssentials.harness(".",   "@");
        OracleEssentials.harness(".",   "1");
        OracleEssentials.harness(".",   "a");
        OracleEssentials.harness("\\d", "1");
        OracleEssentials.harness("\\d", "a");
        OracleEssentials.harness("\\D", "1");
        OracleEssentials.harness("\\D", "a");
        OracleEssentials.harness("\\s", " ");
        OracleEssentials.harness("\\s", "a");
        OracleEssentials.harness("\\S", " ");
        OracleEssentials.harness("\\S", "a");
        OracleEssentials.harness("\\w", "a");
        OracleEssentials.harness("\\w", "!");
        OracleEssentials.harness("\\W", "a");
        OracleEssentials.harness("\\W", "!");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/quant.html">Essential Classes:
     * Lesson: Regular Expressions: Quantifiers</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testQuantifiers() {
        OracleEssentials.harness("a?", "");
        OracleEssentials.harness("a*", "");
        OracleEssentials.harness("a+", "");
    }

    @Test @SuppressWarnings("static-method") public void
    testZeroLengthMatches() {
        OracleEssentials.harness("a?",     "a");
        OracleEssentials.harness("a*",     "a");
        OracleEssentials.harness("a+",     "a");
        OracleEssentials.harness("a?",     "aaaaa");
        OracleEssentials.harness("a*",     "aaaaa");
        OracleEssentials.harness("a+",     "aaaaa");
        OracleEssentials.harness("a?",     "ababaaaab");
        OracleEssentials.harness("a*",     "ababaaaab");
        OracleEssentials.harness("a+",     "ababaaaab");
        OracleEssentials.harness("a{3}",   "aa");
        OracleEssentials.harness("a{3}",   "aaa");
        OracleEssentials.harness("a{3}",   "aaaa");
        OracleEssentials.harness("a{3}",   "aaaaaaaaa");
        OracleEssentials.harness("a{3,}",  "aaaaaaaaa");
        OracleEssentials.harness("a{3,6}", "aaaaaaaaa");
    }

    @Test @SuppressWarnings("static-method") public void
    testCapturingGroupsAndCharacterClassesWithQuantifiers() {
        OracleEssentials.harness("(dog){3}", "dogdogdogdogdogdog");
        OracleEssentials.harness("dog{3}",   "dogdogdogdogdogdog");
        OracleEssentials.harness("[abc]{3}", "abccabaaaccbbbc");
    }

    @Test @SuppressWarnings("static-method") public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers() {
        OracleEssentials.harness(".*foo",  "xfooxxxxxxfoo");
        OracleEssentials.harness(".*?foo", "xfooxxxxxxfoo");
        OracleEssentials.harness(".*+foo", "xfooxxxxxxfoo");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/groups.html">Essential Classes:
     * Lesson: Regular Expressions: Capturing groups</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testBackreferences() {
        OracleEssentials.harness("(\\d\\d)\\1", "1212");
        OracleEssentials.harness("(\\d\\d)\\1", "1234");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/bounds.html">Essential Classes:
     * Lesson: Regular Expressions: Boundary Matchers</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testBoundaryMatchers() {
        OracleEssentials.harness("^dog$",    "dog");
        OracleEssentials.harness("^dog$",    "   dog");
        OracleEssentials.harness("\\s*dog$", "   dog");
        OracleEssentials.harness("^dog\\w*", "dogblahblah");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Methods of the Pattern Class</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testCreatingAPatternWithFlags() {
        OracleEssentials.harness("dog", "DoGDOg", java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html#embedded">Essential Classes:
     * Lesson: Regular Expressions: Embedded Flag Expressions</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testEmbeddedFlagExpressions() {
        OracleEssentials.harness("foo",     "FOOfooFoOfoO", de.unkrig.lfr.core.Pattern.CASE_INSENSITIVE);
        OracleEssentials.harness("(?i)foo", "FOOfooFoOfoO");
        OracleEssentials.harness("foo",     "FOOfooFoOfoO");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Using the matches(String,CharSequence) Method</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testUsingTheMatchesMethod() {
        Assert.assertTrue(de.unkrig.lfr.core.Pattern.matches("\\d", "1"));
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Using the split(String) Method</a>
     */
    @Test @SuppressWarnings("static-method") public void
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
}
