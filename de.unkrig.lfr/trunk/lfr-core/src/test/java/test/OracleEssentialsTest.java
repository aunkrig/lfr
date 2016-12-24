
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
        harness("foo", "foofoofoo");
        harness("cat.", "cats");
    }

    @Test public void
    testSimpleClasses() {
        harness("[bcr]at", "bat");
    }

    @Test public void
    testNegation() {
        harness("[^bcr]at", "bat");
        harness("[^bcr]at", "cat");
        harness("[^bcr]at", "rat");
        harness("[^bcr]at", "hat");
    }

    @Test public void
    testRanges() {
        harness("[a-c]", "a");
        harness("[a-c]", "b");
        harness("[a-c]", "c");
        harness("[a-c]", "d");
        harness("foo[1-5]", "foo1");
        harness("foo[1-5]", "foo5");
        harness("foo[1-5]", "foo6");
        harness("foo[^1-5]", "foo1");
        harness("foo[^1-5]", "foo6");
    }

    @Test public void
    testUnions() {
        harness("[0-4[6-8]]", "0");
        harness("[0-4[6-8]]", "5");
        harness("[0-4[6-8]]", "6");
        harness("[0-4[6-8]]", "8");
        harness("[0-4[6-8]]", "9");
    }

    @Test public void
    testIntersections() {
        harness("[2-8&&[4-6]]", "3");
        harness("[2-8&&[4-6]]", "4");
        harness("[2-8&&[4-6]]", "5");
        harness("[2-8&&[4-6]]", "6");
        harness("[2-8&&[4-6]]", "7");
    }

    @Test public void
    testSubtractions() {
        harness("[0-9&&[^345]]", "2");
        harness("[0-9&&[^345]]", "3");
        harness("[0-9&&[^345]]", "4");
        harness("[0-9&&[^345]]", "5");
        harness("[0-9&&[^345]]", "6");
        harness("[0-9&&[^345]]", "9");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pre_char_classes.html">Essential Classes:
     * Lesson: Regular Expressions: Predefined Character Classes</a>
     */
    @Test public void
    testPredefinedCharacterClasses() {
        harness(".", "@");
        harness(".", "1");
        harness(".", "a");
        harness("\\d", "1");
        harness("\\d", "a");
        harness("\\D", "1");
        harness("\\D", "a");
        harness("\\s", " ");
        harness("\\s", "a");
        harness("\\S", " ");
        harness("\\S", "a");
        harness("\\w", "a");
        harness("\\w", "!");
        harness("\\W", "a");
        harness("\\W", "!");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/quant.html">Essential Classes:
     * Lesson: Regular Expressions: Quantifiers</a>
     */
    @Test public void
    testQuantifiers() {
        harness("a?", "");
        harness("a*", "");
        harness("a+", "");
    }

    @Test public void
    testZeroLengthMatches() {
        harness("a?",     "a");
        harness("a*",     "a");
        harness("a+",     "a");
        harness("a?",     "aaaaa");
        harness("a*",     "aaaaa");
        harness("a+",     "aaaaa");
        harness("a?",     "ababaaaab");
        harness("a*",     "ababaaaab");
        harness("a+",     "ababaaaab");
        harness("a{3}",   "aa");
        harness("a{3}",   "aaa");
        harness("a{3}",   "aaaa");
        harness("a{3}",   "aaaaaaaaa");
        harness("a{3,}",  "aaaaaaaaa");
        harness("a{3,6}", "aaaaaaaaa");
    }

    @Test public void
    testCapturingGroupsAndCharacterClassesWithQuantifiers() {
        harness("(dog){3}", "dogdogdogdogdogdog");
        harness("dog{3}",   "dogdogdogdogdogdog");
        harness("[abc]{3}", "abccabaaaccbbbc");
    }

    @Test public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers() {
        harness(".*foo",  "xfooxxxxxxfoo");
        harness(".*?foo", "xfooxxxxxxfoo");
        harness(".*+foo", "xfooxxxxxxfoo");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/groups.html">Essential Classes:
     * Lesson: Regular Expressions: Capturing groups</a>
     */
    @Test public void
    testBackreferences() {
        harness("(\\d\\d)\\1", "1212");
        harness("(\\d\\d)\\1", "1234");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/bounds.html">Essential Classes:
     * Lesson: Regular Expressions: Boundary Matchers</a>
     */
    @Test public void
    testBoundaryMatchers() {
        harness("^dog$",    "dog");
        harness("^dog$",    "   dog");
        harness("\\s*dog$", "   dog");
        harness("^dog\\w*", "dogblahblah");
    }

    // ==========================================================================================================

    private static void
    harness(String regex, String subject) {

        java.util.regex.Matcher            matcher1 = java.util.regex.Pattern.compile(regex).matcher(subject);
        de.unkrig.lfr.core.Pattern.Matcher matcher2 = de.unkrig.lfr.core.Pattern.compile(regex).matcher(subject);

        for (int matchCount = 0;; matchCount++) {
            String message = "Match #" + (matchCount + 1);

            boolean found1 = matcher1.find();
            boolean found2 = matcher2.find();
            assertEquals(message, found1, found2);
            if (!found1 || !found2) break;

            assertEquals(message, matcher1.group(), matcher2.group());
            assertEquals(message, matcher1.start(), matcher2.start());
            assertEquals(message, matcher1.end(), matcher2.end());
        }
    }
}
