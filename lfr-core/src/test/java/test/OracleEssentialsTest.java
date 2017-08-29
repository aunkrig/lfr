
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

import org.junit.Assert;
import org.junit.Test;

import de.unkrig.ref4j.Pattern;

/**
 * This test case executes all the regex examples presented in <a
 * href="https://docs.oracle.com/javase/tutorial/essential/regex/index.html">Essential Classes: Lesson: Regular
 * Expressions</a>.
 */
public
class OracleEssentialsTest {

    @Test @SuppressWarnings("static-method") public void
    testStringLiterals() {
        OracleEssentials.harnessFull("foo", "foofoofoo");
        OracleEssentials.harnessFull("cat.", "cats");
    }

    @Test @SuppressWarnings("static-method") public void
    testSimpleClasses() {
        OracleEssentials.harnessFull("[bcr]at", "bat");
    }

    @Test @SuppressWarnings("static-method") public void
    testNegation() {
        OracleEssentials.harnessFull("[^bcr]at", "bat");
        OracleEssentials.harnessFull("[^bcr]at", "cat");
        OracleEssentials.harnessFull("[^bcr]at", "rat");
        OracleEssentials.harnessFull("[^bcr]at", "hat");
    }

    @Test @SuppressWarnings("static-method") public void
    testRanges() {
        OracleEssentials.harnessFull("[a-c]", "a");
        OracleEssentials.harnessFull("[a-c]", "b");
        OracleEssentials.harnessFull("[a-c]", "c");
        OracleEssentials.harnessFull("[a-c]", "d");
        OracleEssentials.harnessFull("foo[1-5]", "foo1");
        OracleEssentials.harnessFull("foo[1-5]", "foo5");
        OracleEssentials.harnessFull("foo[1-5]", "foo6");
        OracleEssentials.harnessFull("foo[^1-5]", "foo1");
        OracleEssentials.harnessFull("foo[^1-5]", "foo6");
    }

    @Test @SuppressWarnings("static-method") public void
    testUnions() {
        OracleEssentials.harnessFull("[0-4[6-8]]", "0");
        OracleEssentials.harnessFull("[0-4[6-8]]", "5");
        OracleEssentials.harnessFull("[0-4[6-8]]", "6");
        OracleEssentials.harnessFull("[0-4[6-8]]", "8");
        OracleEssentials.harnessFull("[0-4[6-8]]", "9");
    }

    @Test @SuppressWarnings("static-method") public void
    testIntersections() {
        OracleEssentials.harnessFull("[2-8&&[4-6]]", "3");
        OracleEssentials.harnessFull("[2-8&&[4-6]]", "4");
        OracleEssentials.harnessFull("[2-8&&[4-6]]", "5");
        OracleEssentials.harnessFull("[2-8&&[4-6]]", "6");
        OracleEssentials.harnessFull("[2-8&&[4-6]]", "7");
    }

    @Test @SuppressWarnings("static-method") public void
    testSubtractions() {
        OracleEssentials.harnessFull("[0-9&&[^345]]", "2");
        OracleEssentials.harnessFull("[0-9&&[^345]]", "3");
        OracleEssentials.harnessFull("[0-9&&[^345]]", "4");
        OracleEssentials.harnessFull("[0-9&&[^345]]", "5");
        OracleEssentials.harnessFull("[0-9&&[^345]]", "6");
        OracleEssentials.harnessFull("[0-9&&[^345]]", "9");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pre_char_classes.html">Essential Classes:
     * Lesson: Regular Expressions: Predefined Character Classes</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testPredefinedCharacterClasses() {
        OracleEssentials.harnessFull(".",   "@");
        OracleEssentials.harnessFull(".",   "1");
        OracleEssentials.harnessFull(".",   "a");
        OracleEssentials.harnessFull("\\d", "1");
        OracleEssentials.harnessFull("\\d", "a");
        OracleEssentials.harnessFull("\\D", "1");
        OracleEssentials.harnessFull("\\D", "a");
        OracleEssentials.harnessFull("\\s", " ");
        OracleEssentials.harnessFull("\\s", "a");
        OracleEssentials.harnessFull("\\S", " ");
        OracleEssentials.harnessFull("\\S", "a");
        OracleEssentials.harnessFull("\\w", "a");
        OracleEssentials.harnessFull("\\w", "!");
        OracleEssentials.harnessFull("\\W", "a");
        OracleEssentials.harnessFull("\\W", "!");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/quant.html">Essential Classes:
     * Lesson: Regular Expressions: Quantifiers</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testQuantifiers() {
        OracleEssentials.harnessFull("a?", "");
        OracleEssentials.harnessFull("a*", "");
        OracleEssentials.harnessFull("a+", "");
    }

    @Test @SuppressWarnings("static-method") public void
    testZeroLengthMatches() {
        OracleEssentials.harnessFull("a?",     "a");
        OracleEssentials.harnessFull("a*",     "a");
        OracleEssentials.harnessFull("a+",     "a");
        OracleEssentials.harnessFull("a?",     "aaaaa");
        OracleEssentials.harnessFull("a*",     "aaaaa");
        OracleEssentials.harnessFull("a+",     "aaaaa");
        OracleEssentials.harnessFull("a?",     "ababaaaab");
        OracleEssentials.harnessFull("a*",     "ababaaaab");
        OracleEssentials.harnessFull("a+",     "ababaaaab");
        OracleEssentials.harnessFull("a{3}",   "aa");
        OracleEssentials.harnessFull("a{3}",   "aaa");
        OracleEssentials.harnessFull("a{3}",   "aaaa");
        OracleEssentials.harnessFull("a{3}",   "aaaaaaaaa");
        OracleEssentials.harnessFull("a{3,}",  "aaaaaaaaa");
        OracleEssentials.harnessFull("a{3,6}", "aaaaaaaaa");
    }

    @Test @SuppressWarnings("static-method") public void
    testCapturingGroupsAndCharacterClassesWithQuantifiers() {
        OracleEssentials.harnessFull("(dog){3}", "dogdogdogdogdogdog");
        OracleEssentials.harnessFull("dog{3}",   "dogdogdogdogdogdog");
        OracleEssentials.harnessFull("[abc]{3}", "abccabaaaccbbbc");
    }

    @Test @SuppressWarnings("static-method") public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers1() {
        OracleEssentials.harnessFull(".*foo",  "xfooxxxxxxfoo");
    }

    @Test @SuppressWarnings("static-method") public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers2() {
        OracleEssentials.harnessFull(".*?foo", "xfooxxxxxxfoo");
    }

    @Test @SuppressWarnings("static-method") public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers3() {
        OracleEssentials.harnessFull(".*+foo", "xfooxxxxxxfoo");
    }

    @Test @SuppressWarnings("static-method") public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers4() {
        OracleEssentials.harnessFull("x*+foo", "xfooxxxxxxfoo");
    }

    @Test @SuppressWarnings("static-method") public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers5() {
        OracleEssentials.harnessFull("x*+foo", "xfooxxxxxxfooo");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/groups.html">Essential Classes:
     * Lesson: Regular Expressions: Capturing groups</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testBackreferences() {
        OracleEssentials.harnessFull("(\\d\\d)\\1", "1212");
        OracleEssentials.harnessFull("(\\d\\d)\\1", "1234");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/bounds.html">Essential Classes:
     * Lesson: Regular Expressions: Boundary Matchers</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testBoundaryMatchers() {
        OracleEssentials.harnessFull("^dog$",    "dog");
        OracleEssentials.harnessFull("^dog$",    "   dog");
        OracleEssentials.harnessFull("\\s*dog$", "   dog");
        OracleEssentials.harnessFull("^dog\\w*", "dogblahblah");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Methods of the Pattern Class</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testCreatingAPatternWithFlags() {
        OracleEssentials.harnessFull("dog", "DoGDOg", java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html#embedded">Essential Classes:
     * Lesson: Regular Expressions: Embedded Flag Expressions</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testEmbeddedFlagExpressions() {
        OracleEssentials.harnessFull("foo",     "FOOfooFoOfoO");
        OracleEssentials.harnessFull("foo",     "FOOfooFoOfoO", Pattern.CASE_INSENSITIVE);
        OracleEssentials.harnessFull("(?i)foo", "FOOfooFoOfoO");
        OracleEssentials.harnessFull("foo",     "FOOfooFoOfoO");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Using the matches(String,CharSequence) Method</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testUsingTheMatchesMethod() {
        Assert.assertTrue(OracleEssentials.LFR.matches("\\d", "1"));
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Using the split(String) Method</a>
     */
    @Test @SuppressWarnings("static-method") public void
    testUsingTheSplitMethod() {

        Assert.assertArrayEquals(
            new Object[] { "one", "two", "three", "four", "five" },
            OracleEssentials.LFR.compile(":").split("one:two:three:four:five")
        );

        Assert.assertArrayEquals(
            new Object[] { "one", "two", "three", "four", "five" },
            OracleEssentials.LFR.compile("\\d").split("one9two4three7four1five")
        );
    }
}
