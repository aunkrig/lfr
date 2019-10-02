
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;

/**
 * This test case executes all the regex examples presented in <a
 * href="https://docs.oracle.com/javase/tutorial/essential/regex/index.html">Essential Classes: Lesson: Regular
 * Expressions</a>.
 */
@RunWith(Parameterized.class) public
class OracleEssentialsTest extends OracleEssentials {

    public
    OracleEssentialsTest(PatternFactory patternFactory, String patternFactoryId) { super(patternFactory); }

    @Test public void testStringLiterals1() { this.harnessFull("foo", "foofoofoo"); }
    @Test public void testStringLiterals2() { this.harnessFull("cat.", "cats"); }

    @Test public void
    testSimpleClasses() {
        this.harnessFull("[bcr]at", "bat");
    }

    @Test public void testNegation1() { this.harnessFull("[^bcr]at", "bat"); }
    @Test public void testNegation2() { this.harnessFull("[^bcr]at", "cat"); }
    @Test public void testNegation3() { this.harnessFull("[^bcr]at", "rat"); }
    @Test public void testNegation4() { this.harnessFull("[^bcr]at", "hat"); }

    @Test public void testRanges1() { this.harnessFull("[a-c]", "a"); }
    @Test public void testRanges2() { this.harnessFull("[a-c]", "b"); }
    @Test public void testRanges3() { this.harnessFull("[a-c]", "c"); }
    @Test public void testRanges4() { this.harnessFull("[a-c]", "d"); }
    @Test public void testRanges5() { this.harnessFull("foo[1-5]", "foo1"); }
    @Test public void testRanges6() { this.harnessFull("foo[1-5]", "foo5"); }
    @Test public void testRanges7() { this.harnessFull("foo[1-5]", "foo6"); }
    @Test public void testRanges8() { this.harnessFull("foo[^1-5]", "foo1"); }
    @Test public void testRanges9() { this.harnessFull("foo[^1-5]", "foo6"); }

    @Test public void testUnions1() { this.harnessFull("[0-4[6-8]]", "0"); }
    @Test public void testUnions2() { this.harnessFull("[0-4[6-8]]", "5"); }
    @Test public void testUnions3() { this.harnessFull("[0-4[6-8]]", "6"); }
    @Test public void testUnions4() { this.harnessFull("[0-4[6-8]]", "8"); }
    @Test public void testUnions5() { this.harnessFull("[0-4[6-8]]", "9"); }

    @Test public void testIntersections1() { this.harnessFull("[2-8&&[4-6]]", "3"); }
    @Test public void testIntersections2() { this.harnessFull("[2-8&&[4-6]]", "4"); }
    @Test public void testIntersections3() { this.harnessFull("[2-8&&[4-6]]", "5"); }
    @Test public void testIntersections4() { this.harnessFull("[2-8&&[4-6]]", "6"); }
    @Test public void testIntersections5() { this.harnessFull("[2-8&&[4-6]]", "7"); }

    @Test public void testSubtractions1() { this.harnessFull("[0-9&&[^345]]", "2"); }
    @Test public void testSubtractions2() { this.harnessFull("[0-9&&[^345]]", "3"); }
    @Test public void testSubtractions3() { this.harnessFull("[0-9&&[^345]]", "4"); }
    @Test public void testSubtractions4() { this.harnessFull("[0-9&&[^345]]", "5"); }
    @Test public void testSubtractions5() { this.harnessFull("[0-9&&[^345]]", "6"); }
    @Test public void testSubtractions6() { this.harnessFull("[0-9&&[^345]]", "9"); }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pre_char_classes.html">Essential Classes:
     * Lesson: Regular Expressions: Predefined Character Classes</a>
     */
    @Test public void testPredefinedCharacterClasses1()  { this.harnessFull(".",   "@"); }
    @Test public void testPredefinedCharacterClasses2()  { this.harnessFull(".",   "1"); }
    @Test public void testPredefinedCharacterClasses3()  { this.harnessFull(".",   "a"); }
    @Test public void testPredefinedCharacterClasses4()  { this.harnessFull("\\d", "1"); }
    @Test public void testPredefinedCharacterClasses5()  { this.harnessFull("\\d", "a"); }
    @Test public void testPredefinedCharacterClasses6()  { this.harnessFull("\\D", "1"); }
    @Test public void testPredefinedCharacterClasses7()  { this.harnessFull("\\D", "a"); }
    @Test public void testPredefinedCharacterClasses8()  { this.harnessFull("\\s", " "); }
    @Test public void testPredefinedCharacterClasses9()  { this.harnessFull("\\s", "a"); }
    @Test public void testPredefinedCharacterClasses10() { this.harnessFull("\\S", " "); }
    @Test public void testPredefinedCharacterClasses11() { this.harnessFull("\\S", "a"); }
    @Test public void testPredefinedCharacterClasses12() { this.harnessFull("\\w", "a"); }
    @Test public void testPredefinedCharacterClasses13() { this.harnessFull("\\w", "!"); }
    @Test public void testPredefinedCharacterClasses14() { this.harnessFull("\\W", "a"); }
    @Test public void testPredefinedCharacterClasses15() { this.harnessFull("\\W", "!"); }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/quant.html">Essential Classes:
     * Lesson: Regular Expressions: Quantifiers</a>
     */
    @Test public void testQuantifiers1() { this.harnessFull("a?", ""); }
    @Test public void testQuantifiers2() { this.harnessFull("a*", ""); }
    @Test public void testQuantifiers3() { this.harnessFull("a+", ""); }

    @Test public void testZeroLengthMatches1()  { this.harnessFull("a?",     "a");         }
    @Test public void testZeroLengthMatches2()  { this.harnessFull("a*",     "a");         }
    @Test public void testZeroLengthMatches3()  { this.harnessFull("a+",     "a");         }
    @Test public void testZeroLengthMatches4()  { this.harnessFull("a?",     "aaaaa");     }
    @Test public void testZeroLengthMatches5()  { this.harnessFull("a*",     "aaaaa");     }
    @Test public void testZeroLengthMatches6()  { this.harnessFull("a+",     "aaaaa");     }
    @Test public void testZeroLengthMatches7()  { this.harnessFull("a?",     "ababaaaab"); }
    @Test public void testZeroLengthMatches8()  { this.harnessFull("a*",     "ababaaaab"); }
    @Test public void testZeroLengthMatches9()  { this.harnessFull("a+",     "ababaaaab"); }
    @Test public void testZeroLengthMatches10() { this.harnessFull("a{3}",   "aa");        }
    @Test public void testZeroLengthMatches11() { this.harnessFull("a{3}",   "aaa");       }
    @Test public void testZeroLengthMatches12() { this.harnessFull("a{3}",   "aaaa");      }
    @Test public void testZeroLengthMatches13() { this.harnessFull("a{3}",   "aaaaaaaaa"); }
    @Test public void testZeroLengthMatches14() { this.harnessFull("a{3,}",  "aaaaaaaaa"); }
    @Test public void testZeroLengthMatches15() { this.harnessFull("a{3,6}", "aaaaaaaaa"); }

    // SUPPRESS CHECKSTYLE LineLength:3
    @Test public void testCapturingGroupsAndCharacterClassesWithQuantifiers1() { this.harnessFull("(dog){3}", "dogdogdogdogdogdog"); }
    @Test public void testCapturingGroupsAndCharacterClassesWithQuantifiers2() { this.harnessFull("dog{3}",   "dogdogdogdogdogdog"); }
    @Test public void testCapturingGroupsAndCharacterClassesWithQuantifiers3() { this.harnessFull("[abc]{3}", "abccabaaaccbbbc");    }

    @Test public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers1() {
        this.harnessFull(".*foo",  "xfooxxxxxxfoo");
    }

    @Test public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers2() {
        this.harnessFull(".*?foo", "xfooxxxxxxfoo");
    }

    @Test public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers3() {
        this.harnessFull(".*+foo", "xfooxxxxxxfoo");
    }

    @Test public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers4() {
        this.harnessFull("x*+foo", "xfooxxxxxxfoo");
    }

    @Test public void
    testDifferencesAmongGreedyReluctantAndPossessiveQuantifiers5() {
        this.harnessFull("x*+foo", "xfooxxxxxxfooo");
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/groups.html">Essential Classes:
     * Lesson: Regular Expressions: Capturing groups</a>
     */
    @Test public void testBackreferences1() { this.harnessFull("(\\d\\d)\\1", "1212"); }
    @Test public void testBackreferences2() { this.harnessFull("(\\d\\d)\\1", "1234"); }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/bounds.html">Essential Classes:
     * Lesson: Regular Expressions: Boundary Matchers</a>
     */
    @Test public void testBoundaryMatchers1() { this.harnessFull("^dog$",    "dog");         }
    @Test public void testBoundaryMatchers2() { this.harnessFull("^dog$",    "   dog");      }
    @Test public void testBoundaryMatchers3() { this.harnessFull("\\s*dog$", "   dog");      }
    @Test public void testBoundaryMatchers4() { this.harnessFull("^dog\\w*", "dogblahblah"); }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Methods of the Pattern Class</a>
     */
    @Test public void
    testCreatingAPatternWithFlags() {
        this.harnessFull("dog", "DoGDOg", java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html#embedded">Essential Classes:
     * Lesson: Regular Expressions: Embedded Flag Expressions</a>
     */
    @Test public void testEmbeddedFlagExpressions1() { this.harnessFull("foo",     "FOOfooFoOfoO"); }
    @Test public void testEmbeddedFlagExpressions2() { this.harnessFull("foo",     "FOOfooFoOfoO", Pattern.CASE_INSENSITIVE); } // SUPPRESS CHECKSTYLE LineLength
    @Test public void testEmbeddedFlagExpressions3() { this.harnessFull("(?i)foo", "FOOfooFoOfoO"); }
    @Test public void testEmbeddedFlagExpressions4() { this.harnessFull("foo",     "FOOfooFoOfoO"); }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Using the matches(String,CharSequence) Method</a>
     */
    @Test public void
    testUsingTheMatchesMethod() {
        Assert.assertTrue(this.patternFactory.matches("\\d", "1"));
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/pattern.html">Essential Classes:
     * Lesson: Regular Expressions: Using the split(String) Method</a>
     */
    @Test public void
    testUsingTheSplitMethod1() {
        Assert.assertArrayEquals(
            new Object[] { "one", "two", "three", "four", "five" },
            this.patternFactory.compile(":").split("one:two:three:four:five")
        );
    }

    @Test public void
    testUsingTheSplitMethod2() {

        Assert.assertArrayEquals(
            new Object[] { "one", "two", "three", "four", "five" },
            this.patternFactory.compile("\\d").split("one9two4three7four1five")
        );
    }
}
