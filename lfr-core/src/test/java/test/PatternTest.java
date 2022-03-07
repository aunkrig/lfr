
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

import java.util.Locale;
import java.util.Random;
import java.util.regex.PatternSyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;

/**
 * Tests the {@link Pattern} class (and its relative {@link Matcher}).
 */
@RunWith(Parameterized.class) public
class PatternTest extends OracleEssentials {

    public
    PatternTest(PatternFactory patternFactory, String patternFactoryId) { super(patternFactory); }

    /**
     * 6, 7, 8, ...
     */
    private static final int JRE_VERSION;
    static {

        String jsv = System.getProperty("java.specification.version");

        // For Java 1.0 through 8, the string has the formt "1.x"; since Java 9 "x".
        if (jsv.startsWith("1.")) jsv = jsv.substring(2);

        JRE_VERSION = Integer.parseInt(jsv);
    }

    @Test public void testMatches1() { this.patternFactory.compile("abc", 0).matcher("abc").matches();        }
    @Test public void testMatches2() { this.patternFactory.compile("abc", 0).matcher("abcxx").matches();      }
    @Test public void testMatches3() { this.patternFactory.compile("abc", 0).matcher("xxabc").matches();      }
    @Test public void testMatches4() { this.patternFactory.compile("a.c", 0).matcher("aBc").matches();        }
    @Test public void testMatches5() { this.patternFactory.compile("a.c", 0).matcher("aBcxx").matches();      }
    @Test public void testMatches6() { this.patternFactory.compile("a.*c", 0).matcher("axxxc").matches();     }
    @Test public void testMatches7() { this.patternFactory.compile("a.*c", 0).matcher("axxxcxxxc").matches(); }
    @Test public void testMatches8() { this.patternFactory.compile("a.*c", 0).matcher("axxx").matches();      }

    @Test public void testLiteralOctals1() { this.patternFactory.compile("\\00xx",   0).matcher("\0xx").matches();    }
    @Test public void testLiteralOctals2() { this.patternFactory.compile("\\01xx",   0).matcher("\01xx").matches();   }
    @Test public void testLiteralOctals3() { this.patternFactory.compile("\\011xx",  0).matcher("\011xx").matches();  }
    @Test public void testLiteralOctals4() { this.patternFactory.compile("\\0101xx", 0).matcher("Axx").matches();     }
    @Test public void testLiteralOctals5() { this.patternFactory.compile("\\0111xx", 0).matcher("\0111xx").matches(); }

    @Test public void
    testShortStringLiterals() {

        String infix = "ABCDEFGHIJKLMNO";

        String regex = infix;

        this.assertSequenceToString("naive(\"ABCDEFGHIJKLMNO\")", regex);

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 10; i++) {
            this.harnessFull(regex, AssertionUtil.notNull(rsp.produce()));
        }
    }

    @Test public void
    testLongStringLiterals() {

        String infix = "ABCDEFGHIJKLMNOP";

        String regex = infix;

        this.assertSequenceToString("boyerMooreHorspool(\"ABCDEFGHIJKLMNOP\")", regex);

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 10; i++) {
            this.harnessFull(regex, AssertionUtil.notNull(rsp.produce()));
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

    @Test public void testFind1() { this.harnessFull("abc",   "abc");            }
    @Test public void testFind2() { this.harnessFull("abc",   "xxabcxx");        }
    @Test public void testFind3() { this.harnessFull("abc",   "xxaBcxx");        }
    @Test public void testFind4() { this.harnessFull("a.c",   "xxabcxx");        }
    @Test public void testFind5() { this.harnessFull("a.*b",  "xxaxxbxxbxxbxx"); }
    @Test public void testFind6() { this.harnessFull("a.*?b", "xxaxxbxxbxxbxx"); }
    @Test public void testFind7() { this.harnessFull("a.*+b", "xxaxxbxxbxxbxx"); }

    @Test public void testLookingAt1() { this.patternFactory.compile("abc").matcher("abcdef").lookingAt(); }
    @Test public void testLookingAt2() { this.patternFactory.compile("aBc").matcher("abcdef").lookingAt(); }
    @Test public void testLookingAt3() { this.patternFactory.compile("a.c").matcher("abcdef").lookingAt(); }

    @Test public void testCaseInsensitive1() { this.harnessFull("(?i)A", "xxxAxxx");                          }
    @Test public void testCaseInsensitive2() { this.harnessFull("(?i)A", "xxxaxxx");                          }
    @Test public void testCaseInsensitive3() { this.harnessFull("(?i)Ä", "xxxäxxx");                          }
    @Test public void testCaseInsensitive4() { Assert.assertTrue(this.patternFactory.matches("(?i)Ä",  "Ä")); }
    @Test public void testCaseInsensitive5() { Assert.assertFalse(this.patternFactory.matches("(?i)Ä", "ä")); }

    @Test public void
    testCaseInsensitive6() {
        this.assertSequenceToString("naiveIndexOf(char[2][] { char[2] 'Aa', char[2] 'Bb' })", "ab", Pattern.CASE_INSENSITIVE); // SUPPRESS CHECKSTYLE LineLength
    }

    @Test public void
    testCaseInsensitive7() {
        this.assertSequenceToString(
            "boyerMooreHorspool(char[3][] { char[2] 'Aa', char[2] 'Bb', char[2] 'Cc' })",
            "abc",
            Pattern.CASE_INSENSITIVE
        );
    }

    @Test public void
    testCaseInsensitive8() {
        this.assertSequenceToString(
            "boyerMooreHorspool(char[26][] { char[2] 'Aa', char[2] 'Bb', char[2] 'Cc', char[2] 'Dd', char[2] 'Ee', char[2] 'Ff', char[2] 'Gg', char[2] 'Hh', char[2] 'Ii', char[2] 'Jj', ... })", // SUPPRESS CHECKSTYLE LineLength
            "abcdefghijklmnopqrstuvwxyz",
            Pattern.CASE_INSENSITIVE
        );
    }

    @Test public void
    testBoyerMooreHorspool1() {
        this.assertSequenceToString(
            "boyerMooreHorspool(char[3][] { char[2] 'ak', char[1] ',', char[2] 'Öä' })",
            "[ak][,,,,,][äÖ]"
        );
    }

    @Test public void
    testBoyerMooreHorspool2() {
        this.assertSequenceToString(
            "boyerMooreHorspool(char[3][] { char[2] 'ak', char[3] 'abc', char[2] 'Öä' })",
            "[ak][abc][äÖ]"
        );
    }

    @Test public void testUnicodeCaseInsensitive1() { this.harnessFull("(?ui)A", "xxxAxxx");                         }
    @Test public void testUnicodeCaseInsensitive2() { this.harnessFull("(?ui)A", "xxxaxxx");                         }
    @Test public void testUnicodeCaseInsensitive3() { this.harnessFull("(?ui)Ä", "xxxäxxx");                         }
    @Test public void testUnicodeCaseInsensitive4() { Assert.assertTrue(this.patternFactory.matches("(?ui)Ä", "Ä")); }
    @Test public void testUnicodeCaseInsensitive5() { Assert.assertTrue(this.patternFactory.matches("(?ui)Ä", "ä")); }

    @Test public void testDotall1() { this.harnessFull(".",     " \r  ");                 }
    @Test public void testDotall2() { this.harnessFull(".",     " \r  ", Pattern.DOTALL); }
    @Test public void testDotall3() { this.harnessFull("(?s).", " \r  ");                 }

    @Test public void testLiteralRegex1() { this.harnessFull("$\\*",      "$\\*xxx$\\*xxx", Pattern.LITERAL);                            } // SUPPRESS CHECKSTYLE LineLength:3
    @Test public void testLiteralRegex2() { this.harnessFull("a\\",       "a\\xxxA\\xxx",   Pattern.LITERAL | Pattern.CASE_INSENSITIVE); }
    @Test public void testLiteralRegex3() { this.harnessFull(".\\Q.\\E.", " ___ ");                                                      }
    @Test public void testLiteralRegex4() { this.harnessFull(".\\Q.\\E.", " _._ ");                                                      }

    @Test public void testBoundaries1()  { this.harnessFull("^.",   "___\r___\r\n___\u2028___");                    } // SUPPRESS CHECKSTYLE LineLength:15
    @Test public void testBoundaries2()  { this.harnessFull(".$",   "___\r___\r\n___\u2028___");                    }
    @Test public void testBoundaries3()  { this.harnessFull("^.",   "___\r___\r\n___\u2028___", Pattern.MULTILINE); }
    @Test public void testBoundaries4()  { this.harnessFull(".$",   "___\r___\r\n___\u2028___", Pattern.MULTILINE); }
    @Test public void testBoundaries5()  { this.harnessFull("\\b",  " a b c");                                      }
    @Test public void testBoundaries6()  { this.harnessFull("\\B",  " a b c");                                      }
    @Test public void testBoundaries7()  { this.harnessFull("\\A",  "bla\rbla");                                    }
    @Test public void testBoundaries8()  { this.harnessFull("\\Ga", "aaabbb");                                      }
    @Test public void testBoundaries9()  { this.harnessFull(".\\Z", "abc");                                         }
    @Test public void testBoundaries10() { this.harnessFull(".\\Z", "abc\n");                                       }
    @Test public void testBoundaries11() { this.harnessFull(".\\Z", "abc\r\nd");                                    }
    @Test public void testBoundaries12() { this.harnessFull(".\\z", "abc\n");                                       }
    //@Test public void testBoundaries13() { this.harnessFull(".\\z", "abc\r\nd");                                    } JRE says !requireEnd !?
    @Test public void testBoundaries14() { this.harnessFull(".",    "abc",                      Pattern.MULTILINE); }
    @Test public void testBoundaries15() { this.harnessFull(".",    "abc\n",                    Pattern.MULTILINE); }
    @Test public void testBoundaries16() { this.harnessFull(".",    "abc\r\nd",                 Pattern.MULTILINE); }

    @Test public void
    testMatchFlagsGroup() {
        this.harnessFull("a(?i)b", " ab Ab aB AB ");
    }

    @Test public void
    testMatchFlagsCapturingGroup1() {
        this.harnessFull("a((?i)b)c",       " abc abC aBc aBC Abc AbC ABc ABC ");
    }

    @Test public void
    testMatchFlagsCapturingGroup2() {
        if (PatternTest.JRE_VERSION < 7) return;
        this.harnessFull("a(?<xxx>(?i)b)c", " abc abC aBc aBC Abc AbC ABc ABC ");
    }

    @Test public void
    testMatchFlagsNonCapturingGroup() {
        String regex = "a(?i:b)c";
        this.assertSequenceToString("boyerMooreHorspool(char[3][] { char[1] 'a', char[2] 'Bb', char[1] 'c' })", regex); // SUPPRESS CHECKSTYLE LineLength
        this.harnessFull(regex, " abc abC aBc aBC Abc AbC ABc ABC ");
    }

    @Test public void
    testAlternatives1() {
        this.harnessFull("a|b",        " a b c ");
        this.harnessFull("a(?:b|bb)c", " ac abc abbc abbbc ");
    }

    @Test public void
    testAlternatives2() {
        this.harnessFull("a|aa|aaa", " aaaa ");
        this.harnessFull("a|aaa|aa", " aaaa ");
        this.harnessFull("aa|a|aaa", " aaaa ");
        this.harnessFull("aa|aaa|a", " aaaa ");
        this.harnessFull("aaa|a|aa", " aaaa ");
        this.harnessFull("aaa|aa|a", " aaaa ");
    }

    @Test public void
    testIndependentGroup() {
        this.harnessFull("(?>a|b)",    " a b c ");
        this.harnessFull("a(?>b|bb)c", " ac abc abbc abbbc ");
    }

    // ======================================== CHARACTER CLASSES ========================================

    @Test public void testPredefinedCharacterClasses1() {                                   this.harnessFull("\\w",     " abc äöü "); } // SUPPRESS CHECKSTYLE LineLength:3
    @Test public void testPredefinedCharacterClasses2() { if (PatternTest.JRE_VERSION >= 7) this.harnessFull("(?U)\\w", " abc äöü "); }
    @Test public void testPredefinedCharacterClasses3() {                                   this.harnessFull("\\W",     " abc äöü "); }
    @Test public void testPredefinedCharacterClasses4() { if (PatternTest.JRE_VERSION >= 7) this.harnessFull("(?U)\\W", " abc äöü "); }

    @Test public void testPosixCharacterClasses1() {                                   this.harnessFull("\\p{Lower}",     " abc äöü "); } // SUPPRESS CHECKSTYLE LineLength:3
    @Test public void testPosixCharacterClasses2() { if (PatternTest.JRE_VERSION >= 7) this.harnessFull("(?U)\\p{Lower}", " abc äöü "); }
    @Test public void testPosixCharacterClasses3() {                                   this.harnessFull("\\P{Lower}",     " abc äöü "); }
    @Test public void testPosixCharacterClasses4() { if (PatternTest.JRE_VERSION >= 7) this.harnessFull("(?U)\\P{Lower}", " abc äöü "); }

    @Test public void
    testJavaCharacterClasses1() {
        this.harnessFull("\\p{javaLowerCase}", " a B c ä Ä ");
    }

    @Test public void
    testJavaCharacterClasses2() {
        this.harnessFull("\\P{javaLowerCase}", " a B c ä Ä ");
        this.assertPatternSyntaxException("\\P{JavaLowerCase}");
        this.assertPatternSyntaxException("\\P{JAVALOWERCASE}");
        this.assertPatternSyntaxException("\\P{javalowercase}");
        this.assertPatternSyntaxException("\\P{IsJavaLowerCase}");
    }

    // By "UNICODE script":
    @Test public void testUnicodeCharacterClasses1() {  if (PatternTest.JRE_VERSION >= 7) this.harnessFull("\\p{IsLatin}",       " a B c ä Ä "); } // SUPPRESS CHECKSTYLE LineLength

    // By "UNICODE block":
    @Test public void testUnicodeCharacterClasses2() { this.harnessFull("\\p{InGreek}",       " \u03b1 ");    } // SUPPRESS CHECKSTYLE LineLength:2
    @Test public void testUnicodeCharacterClasses3() { this.harnessFull("\\p{InBasicLatin}",  " a B c ä Ä "); }
    @Test public void testUnicodeCharacterClasses4() { this.harnessFull("\\P{InBasicLatin}",  " a B c ä Ä "); }

    // By "UNICODE category":
    @Test public void testUnicodeCharacterClasses5() { this.harnessFull("\\p{Lu}",            " a B c ä Ä "); } // SUPPRESS CHECKSTYLE LineLength:3
    @Test public void testUnicodeCharacterClasses6() { this.harnessFull("\\P{Lu}",            " a B c ä Ä "); }
    @Test public void testUnicodeCharacterClasses7() { this.harnessFull("\\p{Sc}",            " a $ ");       }
    @Test public void testUnicodeCharacterClasses8() { this.harnessFull("\\P{Sc}",            " a $ ");       }

    // By "UNICODE property":
    @Test public void testUnicodeCharacterClasses9()  { if (PatternTest.JRE_VERSION >= 7) this.harnessFull("\\p{IsLowerCASE}",  " abc äöü "); } // SUPPRESS CHECKSTYLE LineLength:1
    @Test public void testUnicodeCharacterClasses10() { if (PatternTest.JRE_VERSION >= 7) this.harnessFull("\\p{IsAlphabetic}", " abc äöü "); }

    // ======================================== END OF CHARACTER CLASSES ========================================

    @Test public void
    testCapturingGroups() {
        this.harnessFull("((a+)(b+))", " abbb aabb aaab ");
    }

    @Test public void
    testNamedCapturingGroups1() {
        if (PatternTest.JRE_VERSION >= 7) this.harnessFull("(?<xxx>a+)", " a aa aaa");
    }

    @Test public void
    testNamedCapturingGroups2() {

        if (PatternTest.JRE_VERSION < 7) return;

        Matcher matcher = this.patternFactory.compile("(?<xxx>a+)").matcher(" a aa aaa");

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
        this.patternFactory.compile("(\\d\\d)\\2").matcher(" a aa aaa").replaceAll("x");
    }

    @Test public void
    testNamedCapturingGroupsBackreference1() {
        if (PatternTest.JRE_VERSION >= 7) this.harnessFull("(?<first>\\w)\\k<first>", " a aa aaa");
    }

    @Test public void
    testNamedCapturingGroupsBackreference2() {

        // Backreference to inexistent named group.
        if (PatternTest.JRE_VERSION >= 7) this.assertPatternSyntaxException("(?<first>\\w)\\k<bla>");
    }

    @Test public void testPositiveLookahead1() { this.harnessFull("a(?=b)",   " a aba abba a"); }
    @Test public void testPositiveLookahead2() { this.harnessFull("a(?=(b))", " a aba abba a"); }

    @Test public void testNegativeLookahead1() { this.harnessFull("a(?!b)",   " a aba abba a"); }
    @Test public void testNegativeLookahead2() { this.harnessFull("a(?!(b))", " a aba abba a"); }

    @Test public void testPositiveLookbehind1() {                                   this.harnessFull("(?<=b)a",     " a aba abba a"); } // SUPPRESS CHECKSTYLE LineLength:4
    @Test public void testPositiveLookbehind2() {                                   this.harnessFull("(?<=(b))a",   " a aba abba a"); }
    @Test public void testPositiveLookbehind3() { if (PatternTest.JRE_VERSION >= 8) this.harnessFull("(?<=\\R )a",  " \r\n a ");      }
    @Test public void testPositiveLookbehind4() { if (PatternTest.JRE_VERSION >= 8) this.harnessFull("(?<=\\R )a",  " \r a ");        }
    @Test public void testPositiveLookbehind5() { if (PatternTest.JRE_VERSION >= 8) this.harnessFull("(?<=\\R )a",  " \n a ");        }

    @Test public void
    testPositiveLookbehind6() {
        this.harnessFull("(?<=^\t*)\t", "\t\t\tpublic static void main()");
    }

    @Test public void
    testPositiveLookbehind7() {
        this.patternFactory.compile("(?<=^\\s*)    ").matcher("        public static void main()").replaceAll("\t");
    }

    @Test public void testNegativeLookbehind1() { this.harnessFull("(?<!b)a",     " a aba abba a"); }
    @Test public void testNegativeLookbehind2() { this.harnessFull("(?<!(b))a",   " a aba abba a"); }
    @Test public void testNegativeLookbehind3() { this.harnessFull("(?<!(?:b))a", " a aba abba a"); }
    @Test public void testNegativeLookbehind4() { this.harnessFull("(?<!b)a",     " a aba abba a"); }

    @Test public void testRegion1() { this.harnessFull("a", "__a__ a aba abba __a__", 0, 5, 17);               } // SUPPRESS CHECKSTYLE LineLength:5
    @Test public void testRegion2() { this.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17);               }
    @Test public void testRegion3() { this.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, false, false); }
    @Test public void testRegion4() { this.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, false, true);  }
    @Test public void testRegion5() { this.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, true,  false); }
    @Test public void testRegion6() { this.harnessFull("^", "__a__ a aba abba __a__", 0, 5, 17, true,  true);  }

    @Test public void testTransparentBounds1() { this.harnessFull("\\b",     "__a__ a aba abba __a__", 0, 5, 17, true); } // SUPPRESS CHECKSTYLE LineLength

    // Lookahead.
    @Test public void testTransparentBounds2() { this.harnessFull(" (?=_)",  "__a__ a aba abba __a__", 0, 5, 17, true); } // SUPPRESS CHECKSTYLE LineLength:1
    @Test public void testTransparentBounds3() { this.harnessFull(" (?!_)",  "__a__ a aba abba __a__", 0, 5, 17, true); }

    // Lookbehind.
    @Test public void testTransparentBounds4() { this.harnessFull("(?<=_) ", "__a__ a aba abba __a__", 0, 5, 17, true); } // SUPPRESS CHECKSTYLE LineLength:1
    @Test public void testTransparentBounds5() { this.harnessFull("(?<!_) ", "__a__ a aba abba __a__", 0, 5, 17, true); }

    @Test public void testAnchoringBounds1() { this.harnessFull("^",  "__a__ a aba abba __a__", 0, 5, 17, null, false); } // SUPPRESS CHECKSTYLE LineLength:1
    @Test public void testAnchoringBounds2() { this.harnessFull("$",  "__a__ a aba abba __a__", 0, 5, 17, null, false); }

    @Test public void testUnixLines1() { if (PatternTest.JRE_VERSION >= 8) this.harnessFull("\\R",  "  \n  \r\n \u2028 ");                     } // SUPPRESS CHECKSTYLE LineLength:3
    @Test public void testUnixLines2() { if (PatternTest.JRE_VERSION >= 8) this.harnessFull("\\R",  "  \n  \r\n \u2028 ", Pattern.UNIX_LINES); }
    @Test public void testUnixLines3() {                                   this.harnessFull("^",    "  \n  \r\n \u2028 ");                     }
    @Test public void testUnixLines4() {                                   this.harnessFull("^",    "  \n  \r\n \u2028 ", Pattern.UNIX_LINES); }

    @Test public void testQuantifiers1a() { this.harnessFull("a?",     " aaa "); }
    @Test public void testQuantifiers1b() { this.harnessFull("a??",    " aaa "); }
    @Test public void testQuantifiers1c() { this.harnessFull("a?+",    " aaa "); }
    @Test public void testQuantifiers2a() { this.harnessFull("a*",     " aaa "); }
    @Test public void testQuantifiers2b() { this.harnessFull("a*?",    " aaa "); }
    @Test public void testQuantifiers2c() { this.harnessFull("a*+",    " aaa "); }
    @Test public void testQuantifiers3a() { this.harnessFull("a+",     " aaa "); }
    @Test public void testQuantifiers3b() { this.harnessFull("a+?",    " aaa "); }
    @Test public void testQuantifiers3c() { this.harnessFull("a++",    " aaa "); }
    @Test public void testQuantifiers4a() { this.harnessFull("a{0}",   " aaa "); }
    @Test public void testQuantifiers4b() { this.harnessFull("a{0}?",  " aaa "); }
    @Test public void testQuantifiers4c() { this.harnessFull("a{0}+",  " aaa "); }
    @Test public void testQuantifiers5a() { this.harnessFull("a{1}",   " aaa "); }
    @Test public void testQuantifiers5b() { this.harnessFull("a{1}?",  " aaa "); }
    @Test public void testQuantifiers5c() { this.harnessFull("a{1}+",  " aaa "); }
    @Test public void testQuantifiers6a() { this.harnessFull("a{2}",   " aaa "); }
    @Test public void testQuantifiers6b() { this.harnessFull("a{2}?",  " aaa "); }
    @Test public void testQuantifiers6c() { this.harnessFull("a{2}+",  " aaa "); }
    @Test public void testQuantifiers7a() { this.harnessFull("a{0,}",  " aaa "); }
    @Test public void testQuantifiers7b() { this.harnessFull("a{0,}?", " aaa "); }
    @Test public void testQuantifiers7c() { this.harnessFull("a{0,}+", " aaa "); }

    @Test public void
    testLongLiteralString() {

        String infix = "ABCDEFGHIJKLMNOP";

        String regex = infix;

        this.assertSequenceToString("boyerMooreHorspool(\"ABCDEFGHIJKLMNOP\")", regex);

        Pattern pattern = this.patternFactory.compile(regex, Pattern.DOTALL);

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);
        for (int i = 0; i < 10; i++) {

            String  subject = AssertionUtil.notNull(rsp.produce());
            Matcher matcher = pattern.matcher(subject);
            while (matcher.find());
        }
    }

    @Test public void
    testGreedyQuantifierFollowedByLongLiteralString() {

        String infix = "ABCDEFGHIJKLMNOP";

        String regex = ".*" + infix;

        this.assertSequenceToString((
            "greedyQuantifierOnCharacterClass(operand=anyCharButLineBreak, min=0, max=infinite)"
            + " . "
            + "boyerMooreHorspool(\"ABCDEFGHIJKLMNOP\")"
        ), regex);

        Pattern pattern = this.patternFactory.compile(regex, Pattern.DOTALL);

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);

        for (int i = 0; i < 10; i++) {
            String subject = AssertionUtil.notNull(rsp.produce());
            pattern.matcher(subject).matches();
        }
    }

    @Test public void
    testReluctantQuantifierFollowedByLongLiteralString() {

        final String infix = "ABCDEFGHIJKLMNOP";

        String regex = ".*?" + infix;

        this.assertSequenceToString((
            "reluctantQuantifierOnCharacterClass(operand=anyCharButLineBreak, min=0, max=infinite)"
            + " . "
            + "boyerMooreHorspool(\"ABCDEFGHIJKLMNOP\")"
        ), regex);

        Pattern pattern = this.patternFactory.compile(regex, Pattern.DOTALL);

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);

        for (int i = 0; i < 5; i++) {
            String subject = AssertionUtil.notNull(rsp.produce());

            Matcher matcher = pattern.matcher(subject);
            matcher.matches();
        }
    }

    /** MUSICAL SYMBOL G CLEF OTTAVA BASSA */
    static int    clef              = 0x1d120;
    static char   clefHighSurrogate = PatternTest.highSurrogateOf(PatternTest.clef);
    static char   clefLowSurrogate  = PatternTest.lowSurrogateOf(PatternTest.clef);
    static String clefUnicode       = "" + PatternTest.clefHighSurrogate + PatternTest.clefLowSurrogate;

    @Test public void
    testSurrogates1() {
        this.harnessFull(PatternTest.clefUnicode,       PatternTest.clefUnicode);
    }

    @Test public void
    testSurrogates2() {
        this.harnessFull(PatternTest.clefUnicode + "?", "");
    }

    @Test public void
    testSurrogates3() {
//        this.harnessFull(PatternTest.clefUnicode + "?", "" + PatternTest.clefHighSurrogate);
    }

    @Test public void
    testSurrogates4() {
        this.harnessFull(PatternTest.clefUnicode + "?", "" + PatternTest.clefLowSurrogate);
    }

    @Test public void
    testSurrogates5() {
        this.harnessFull(
            PatternTest.clefUnicode + "?",
            PatternTest.clefUnicode
        );
    }

    @Test public void
    testSurrogates6() {
//        this.harnessFull(
//            PatternTest.clefUnicode + "?",
//            "" + PatternTest.clefLowSurrogate + PatternTest.clefHighSurrogate // <= high/low surrogates reversed!
//        );
    }

    @Test public void
    testPreviousMatchBoundary() {

        // From: http://stackoverflow.com/questions/2708833
        this.harnessFull(
            "(?<=\\G\\d{3})(?=\\d)" + "|" + "(?<=^-?\\d{1,3})(?=(?:\\d{3})+(?!\\d))",
            "-1234567890.1234567890"
        );
    }

    @Test public void testAtomicGroups1() { this.harnessFull("^a(bc|b)c$",   "abc");  }
    @Test public void testAtomicGroups2() { this.harnessFull("^a(bc|b)c$",   "abcc"); }
    @Test public void testAtomicGroups3() { this.harnessFull("^a(?>bc|b)c$", "abc");  }
    @Test public void testAtomicGroups4() { this.harnessFull("^a(?>bc|b)c$", "abcc"); }

    /**
     * @see <a href="http://stackoverflow.com/questions/17618812">Clarification about requireEnd Matcher's method</a>
     */
    @Test public void testRequireEnd1() { this.harnessFull("cat$",           "I have a cat");     }
    @Test public void testRequireEnd2() { this.harnessFull("cat$",           "I have a catflap"); }
    @Test public void testRequireEnd3() { this.harnessFull("cat",            "I have a cat");     }
    @Test public void testRequireEnd4() { this.harnessFull("cat",            "I have a catflap"); }
    @Test public void testRequireEnd5() { this.harnessFull("\\d+\\b|[><]=?", "1234");             }
    @Test public void testRequireEnd6() { this.harnessFull("\\d+\\b|[><]=?", ">=");               }
    @Test public void testRequireEnd7() { this.harnessFull("\\d+\\b|[><]=?", "<");                }

    @Test public void testComments1()  {                                   this.harnessFull(" a# comment \nb ",    " ab a# comment \nb", Pattern.COMMENTS); } // SUPPRESS CHECKSTYLE LineLength:20
    @Test public void testComments2()  {                                   this.harnessFull("(?x)  a  ",           " a ");                                  }
    @Test public void testComments3()  {                                   this.harnessFull("(?x)  a  (?-x) b",    " ab ");                                 }
    @Test public void testComments4()  {                                   this.harnessFull("(?x)  a  (?-x) b",    " a b ");                                }
    @Test public void testComments5()  {                                   this.harnessFull("(?x)  a#\n  (?-x) b", " ab ");                                 }
    @Test public void testComments6()  {                                   this.harnessFull("(?x)  a#\n  (?-x) b", " a b ");                                }
    @Test public void testComments7()  {                                   this.harnessFull("(?x)  (a)",           " a b ");                                }
    @Test public void testComments8()  {                                   this.harnessFull("(?x)  (?:a)",         " a b ");                                }
    @Test public void testComments9()  {                                   this.harnessFull("(?x)  ( ?:a)",        " a b ");                                }
    @Test public void testComments10() {                                   this.harnessFull("(?x)  (?: a)",        " a b ");                                }
    @Test public void testComments11() {                                   this.harnessFull("(?x)  (? : a)",       " a b ");                                }
    @Test public void testComments12() {                                   this.harnessFull("(?x)  ( ? :a)",       " a b ");                                }
    @Test public void testComments13() {                                   this.harnessFull("(?x)  ( ?: a)",       " a b ");                                }
    @Test public void testComments14() {                                   this.harnessFull("(?x)  ( ? : a)",      " a b ");                                }
    @Test public void testComments15() { if (PatternTest.JRE_VERSION >= 7) this.harnessFull("(?x)  (?<name>a)",    " a b ");                                }
    @Test public void testComments16() { if (PatternTest.JRE_VERSION >= 7) this.harnessFull("(?x)  ( ?<name>a)",   " a b ");                                }
    @Test public void testComments17() { if (PatternTest.JRE_VERSION >= 7) this.assertPatternSyntaxException("(?x)  (? <name>a)");                          }
    @Test public void testComments18() { if (PatternTest.JRE_VERSION >= 7) this.harnessFull("(?x)  (?< name>a)", " a b ");                                  }
    @Test public void testComments19() { if (PatternTest.JRE_VERSION >= 7) this.assertPatternSyntaxException("(?x)  (? < name>a)");                         }
    @Test public void testComments20() { if (PatternTest.JRE_VERSION >= 7) this.harnessFull("(?x)  ( ?< name>a)", " a b ");                                 }
    @Test public void testComments21() { if (PatternTest.JRE_VERSION >= 7) this.assertPatternSyntaxException("(?x)  ( ? < name>a)");                        }

    @Test public void
    testReplaceAll1() {
        Assert.assertEquals(" Xbc ",     this.patternFactory.compile("a").matcher(" abc ").replaceAll("X"));
    }

    @Test public void
    testReplaceAll2() {
        Assert.assertEquals(" <<a>>bc ", this.patternFactory.compile("(a)").matcher(" abc ").replaceAll("<<$1>>"));
    }

    @Test public void
    testReplaceAll3() {

        PatternFactory pf = PatternTest.JRE_VERSION < 7 ? this.patternFactory : this.patternFactory;

        Assert.assertEquals(
            " <<a>>bc ",
            pf.compile("(?<grp>a)").matcher(" abc ").replaceAll("<<${grp}>>")
        );
    }

    @Test public void
    testReplaceAll4() {

        if (this.isLfr()) {
            Assert.assertEquals(
                " <<null>>bc ",
                this.patternFactory.compile("(a)").matcher(" abc ").replaceAll("<<${null}>>")
            );
        }
    }

    @Test public void
    testReplaceAll5() {

        if (this.isLfr()) {
            Assert.assertEquals(
                " <<a>>bc ",
                this.patternFactory.compile("(a)").matcher(" abc ").replaceAll("${\"<<\" + m.group() + \">>\"}")
            );
        }
    }

    @Test public void
    testReplaceAll6() {

        if (this.isLfr()) {
            Assert.assertEquals(
                " <<a>>bc ",
                this.patternFactory.compile("(a)").matcher(" abc ").replaceAll("<<${m.group()}>>")
            );
        }
    }

    @Test public void
    testReplaceAll7() {
        if (this.isLfr()) {
            Assert.assertEquals(" 7a1bc ", (
                this
                .patternFactory
                .compile("(?<grp>a)")
                .matcher(" abc ")
                .replaceAll("${\"\" + 7 + grp + m.groupCount()}")
            ));
        }
    }

    @Test public void
    testAppendReplacementTail1() {

        // Verify that "appendReplacement()" without a preceding match throws an Exception.
        try {
            (
                this
                .patternFactory
                .compile("foo")
                .matcher(" Hello foo and foo!")
            ).appendReplacement(new StringBuffer(), "bar");
            Assert.fail();
        } catch (IllegalStateException ise) {
            ;
        }
    }

    @Test public void
    testAppendReplacementTail2() {

        // Verify that "appendReplacement()" and "appendTail()" work.
        Matcher m = this.patternFactory.compile("foo").matcher(" Hello foo and foo!");

        Assert.assertTrue(m.find());
        StringBuffer sb = new StringBuffer("==");
        m.appendReplacement(sb, "bar");
        Assert.assertEquals("== Hello bar", sb.toString());

        m.appendTail(sb);
        Assert.assertEquals("== Hello bar and foo!", sb.toString());
    }

    @Test public void testCharacterClassOptimizations1() { this.assertSequenceToString("'A'",                                            "[A]");                } // SUPPRESS CHECKSTYLE LineLength:6
    @Test public void testCharacterClassOptimizations2() { this.assertSequenceToString("oneOfTwoChars('A', 'B')",                        "[AB]");               }
    @Test public void testCharacterClassOptimizations3() { this.assertSequenceToString("oneOfTwoChars('A', 'K')",                        "[AK]");               }
    @Test public void testCharacterClassOptimizations4() { this.assertSequenceToString("bitSet('A', 'C', 'E', 'G', 'I', 'K')",           "[ACEGIK]");           }
    @Test public void testCharacterClassOptimizations5() { this.assertSequenceToString("charRange('A' - 'E')",                           "[A-E]");              }
    @Test public void testCharacterClassOptimizations6() { this.assertSequenceToString("bitSet('D', 'E', 'F', 'G', 'H', 'I', 'J', 'K')", "[A-K&&D-Z]");         }
    @Test public void testCharacterClassOptimizations7() { this.assertSequenceToString(PatternTest.jurpc("set\\('.'(?:, '.'){63}\\)"),   "[A-Za-z0-9_\u0400]"); }

    @Test public void testQuantifierOptimizations1()  { this.assertSequenceToString("'A'",                                                                                                      "A");                      } // SUPPRESS CHECKSTYLE LineLength:12
    @Test public void testQuantifierOptimizations2()  { this.assertSequenceToString("'A' . greedyQuantifierOnCharacterClass(operand=anyCharButLineBreak, min=0, max=infinite) . 'B'",           "A.*B");                   }
    @Test public void testQuantifierOptimizations3()  { this.assertSequenceToString("'A' . greedyQuantifierOnCharacterClass(operand=anyCharButLineBreak, min=0, max=infinite) . naive(\"BC\")", "A.*BC");                  }
    @Test public void testQuantifierOptimizations4()  { this.assertSequenceToString("'A' . greedyQuantifierOnAnyCharAndLiteralString(min=0, max=infinite, ls=naive(\"BC\"))",                   "A.*BC",  Pattern.DOTALL); }
    @Test public void testQuantifierOptimizations5()  { this.assertSequenceToString("'A' . reluctantQuantifierOnAnyCharAndLiteralString(min=0, max=infinite, ls=naive(\"BC\"))",                "A.*?BC", Pattern.DOTALL); }
    @Test public void testQuantifierOptimizations6()  { this.assertSequenceToString("'A' . possessiveQuantifierOnAnyChar(min=0, max=infinite) . naive(\"BC\")",                                 "A.*+BC", Pattern.DOTALL); }
    @Test public void testQuantifierOptimizations7()  { this.assertSequenceToString("naive(\"aaa\")",                                                                                           "a{3}");                   }
    @Test public void testQuantifierOptimizations8()  { this.assertSequenceToString("naive(\"aaa\") . greedyQuantifierOnChar(operand='a', min=0, max=2)",                                       "a{3,5}");                 }
    @Test public void testQuantifierOptimizations9()  { this.assertSequenceToString("naive(\"aaa\") . reluctantQuantifierOnCharacterClass(operand='a', min=0, max=2)",                          "a{3,5}?");                }
    @Test public void testQuantifierOptimizations10() { this.assertSequenceToString("naive(\"aaa\") . possessiveQuantifier(operand='a', min=0, max=2)",                                         "a{3,5}+");                }
    @Test public void testQuantifierOptimizations11() { this.assertSequenceToString("naive(\"abcabcabc\") . greedyQuantifier(operand=naive(\"abc\"), min=0, max=2)",                            "(?:abc){3,5}");           }
    @Test public void testQuantifierOptimizations12() { this.assertSequenceToString("naive(\"abcabcabc\") . reluctantQuantifier(operand=naive(\"abc\"), min=0, max=2)",                         "(?:abc){3,5}?");          }
    @Test public void testQuantifierOptimizations13() { this.assertSequenceToString("naive(\"abcabcabc\") . possessiveQuantifier(operand=naive(\"abc\"), min=0, max=2)",                        "(?:abc){3,5}+");          }

    @Test public void
    testQuantifierOptimizations14() {

        // Naive string search, because the string literal is only 14 characters long.
        this.assertSequenceToString(
            "'A' . greedyQuantifierOnAnyCharAndLiteralString(min=0, max=infinite, ls=naive(\"abcdefghijklmno\"))",
            "A.*abcdefghijklmno",
            Pattern.DOTALL
        );
    }

    @Test public void
    testQuantifierOptimizations15() {

        // Boyer-Moore-Horspool string search, because the string literal is 15 characters long.
        this.assertSequenceToString(
            (
                ""
                + "'A' . "
                + "greedyQuantifierOnAnyCharAndLiteralString("
                +     "min=0, "
                +     "max=infinite, "
                +     "ls=boyerMooreHorspool(\"abcdefghijklmnop\")"
                + ")"
            ),
            "A.*abcdefghijklmnop",
            Pattern.DOTALL
        );
    }

    @Test public void
    testCaseInsensitiveMatch() {
        char[] tripleCaseLetters = { 452, 453, 454, 455, 456, 457, 458, 459, 460, 497, 498, 499 };

        for (char c : tripleCaseLetters) {
            this.harnessFull(new String(new char[] { c }), new String(tripleCaseLetters));
            this.harnessFull(new String(new char[] { c }), new String(tripleCaseLetters), Pattern.CASE_INSENSITIVE); // SUPPRESS CHECKSTYLE LineLength
        }
    }

    @Test public void
    testPerformance() {

        // Takes    453 ms on my machine for 20 "x"s.
        // Takes  1,143 ms on my machine for 25 "x"s.
        // Takes  1,839 ms on my machine for 26 "x"s.
        // Takes  3,329 ms on my machine for 27 "x"s.
        // Takes 23,125 ms on my machine for 30 "x"s.
        // See https://blog.codinghorror.com/regex-performance/
        long start = System.currentTimeMillis();
        this.patternFactory.compile("(x+x+)+y").matcher("xxxxxxxxxxxxxxxxxxxxxxxxxxx").matches();
        long end = System.currentTimeMillis();

        System.out.printf(Locale.US, "Took %,d ms%n",  end - start);
    }

    @Test public void
    testCapturingQuantifiers() {

        // Method "Matcher.count(int)" is only available for LFR.
        if (this.isLfr()) {
            de.unkrig.lfr.core.Matcher matcher = (
                de.unkrig.lfr.core.PatternFactory.INSTANCE
                .compile("a{1,}b{1,}c{1,}")
                .matcher(" abc aabbcc abbccc ")
            );

            Assert.assertTrue(matcher.find());
            Assert.assertEquals(1, matcher.count(0));
            Assert.assertEquals(1, matcher.count(1));
            Assert.assertEquals(1, matcher.count(2));

            Assert.assertTrue(matcher.find());
            Assert.assertEquals(2, matcher.count(0));
            Assert.assertEquals(2, matcher.count(1));
            Assert.assertEquals(2, matcher.count(2));

            Assert.assertTrue(matcher.find());
            Assert.assertEquals(1, matcher.count(0));
            Assert.assertEquals(2, matcher.count(1));
            Assert.assertEquals(3, matcher.count(2));

            Assert.assertFalse(matcher.find());
        }
    }

    @Test public void
    testCapturingGroupsInLookPositiveAheads() {

        Matcher m = this.patternFactory.compile("(?<=(abc))(def)(?=(ghi))").matcher("   abcdefghi   ");
        Assert.assertTrue(m.find());
        Assert.assertEquals(3, m.groupCount());

        Assert.assertEquals("def", m.group());

        Assert.assertEquals("abc", m.group(1));
        Assert.assertEquals("def", m.group(2));
        Assert.assertEquals("ghi", m.group(3));
    }
    
    @Test public void
    testCapturingGroupsInLookPositiveAheads2() {
        
        Matcher m = this.patternFactory.compile("(?<=(abc)(def))(ghi)").matcher("   abcdefghi   ");
        Assert.assertTrue(m.find());
        Assert.assertEquals(3, m.groupCount());
        
        Assert.assertEquals("ghi", m.group());
        
        Assert.assertEquals("abc", m.group(1));
        Assert.assertEquals("def", m.group(2));
        Assert.assertEquals("ghi", m.group(3));
    }
    
    @Test public void
    testCapturingGroupsInLookNegativeAheads() {

        Matcher m = this.patternFactory.compile("(?<!(abc))(def)(?!(ghi))").matcher("   xxxdefxxx   ");
        Assert.assertTrue(m.find());
        Assert.assertEquals(3, m.groupCount());

        Assert.assertEquals("def", m.group());

        Assert.assertEquals(null,  m.group(1));
        Assert.assertEquals("def", m.group(2));
        Assert.assertEquals(null,  m.group(3));
    }

    // ========================================================================================================

    private void
    assertSequenceToString(String expected, String regex) {
        this.assertSequenceToString(expected, regex, 0);
    }

    private void
    assertSequenceToString(String expected, String regex, int flags) {
        if (this.isLfr()) {
            Assert.assertEquals(
                expected,
                de.unkrig.lfr.core.PatternFactory.INSTANCE.compile(regex, flags).sequenceToString()
            );
        }
    }

    private void
    assertSequenceToString(java.util.regex.Pattern expected, String regex) {
        this.assertSequenceToString(expected, regex, 0);
    }

    private void
    assertSequenceToString(java.util.regex.Pattern expected, String regex, int flags) {
        if (this.isLfr()) {
            String s = de.unkrig.lfr.core.PatternFactory.INSTANCE.compile(regex, flags).sequenceToString();
            Assert.assertTrue(
                "\"" + s + "\" does not match \"" + expected.toString() + "\"",
                expected.matcher(s).matches()
            );
        }
    }

    public void
    assertPatternSyntaxException(String regex) {
        this.assertPatternSyntaxException(regex, 0);
    }

    public void
    assertPatternSyntaxException(String regex, int flags) {

        try {
            this.patternFactory.compile(regex, flags);
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
