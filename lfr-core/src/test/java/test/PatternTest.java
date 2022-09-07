
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
class PatternTest extends ParameterizedWithPatternFactory {

    public
    PatternTest(PatternFactory patternFactory, String patternFactoryId) { super(patternFactory); }

    @Test public void testMatches1() {  Assert.assertTrue(this.patternFactory.compile("abc",  0).matcher("abc").matches());       }
    @Test public void testMatches2() { Assert.assertFalse(this.patternFactory.compile("abc",  0).matcher("abcxx").matches());     }
    @Test public void testMatches3() { Assert.assertFalse(this.patternFactory.compile("abc",  0).matcher("xxabc").matches());     }
    @Test public void testMatches4() {  Assert.assertTrue(this.patternFactory.compile("a.c",  0).matcher("aBc").matches());       }
    @Test public void testMatches5() { Assert.assertFalse(this.patternFactory.compile("a.c",  0).matcher("aBcxx").matches());     }
    @Test public void testMatches6() {  Assert.assertTrue(this.patternFactory.compile("a.*c", 0).matcher("axxxc").matches());     }
    @Test public void testMatches7() {  Assert.assertTrue(this.patternFactory.compile("a.*c", 0).matcher("axxxcxxxc").matches()); }
    @Test public void testMatches8() { Assert.assertFalse(this.patternFactory.compile("a.*c", 0).matcher("axxx").matches());      }

    @Test public void testLiteralOctals1() {  Assert.assertTrue(this.patternFactory.compile("\\00xx",   0).matcher("\0xx").matches());    }
    @Test public void testLiteralOctals2() {  Assert.assertTrue(this.patternFactory.compile("\\01xx",   0).matcher("\01xx").matches());   }
    @Test public void testLiteralOctals3() {  Assert.assertTrue(this.patternFactory.compile("\\011xx",  0).matcher("\011xx").matches());  }
    @Test public void testLiteralOctals4() {  Assert.assertTrue(this.patternFactory.compile("\\0101xx", 0).matcher("Axx").matches());     }
    @Test public void testLiteralOctals5() { Assert.assertFalse(this.patternFactory.compile("\\0101xx", 0).matcher("\0111xx").matches()); }
    
    @Test public void testFastMatches1() {  Assert.assertTrue(this.patternFactory.compile("ABC",      0).matches("   ABC   ", 3, 6)); }
    @Test public void testFastMatches2() {  Assert.assertTrue(this.patternFactory.compile("^ABC$",    0).matches("   ABC   ", 3, 6)); }
    @Test public void testFastMatches3() { Assert.assertFalse(this.patternFactory.compile("ABC(?=D)", 0).matches("   ABCD  ", 3, 6)); }

    @Test public void
    testShortStringLiterals() {

        String infix = "ABCDEFGHIJKLMNO";

        String regex = infix;

        this.assertSequenceToString("naive(\"ABCDEFGHIJKLMNO\")", regex);

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);
        this.assertFind(6, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(3, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(5, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(2, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(2, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(2, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(2, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(1, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(2, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(3, regex, AssertionUtil.notNull(rsp.produce()));
    }

    @Test public void
    testLongStringLiterals() {

        String infix = "ABCDEFGHIJKLMNOP";

        String regex = infix;

        this.assertSequenceToString("boyerMooreHorspool(\"ABCDEFGHIJKLMNOP\")", regex);

        Producer<String> rsp = PatternTest.randomSubjectProducer(infix);
        this.assertFind(3, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(0, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(1, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(3, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(0, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(2, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(1, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(2, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(4, regex, AssertionUtil.notNull(rsp.produce()));
        this.assertFind(3, regex, AssertionUtil.notNull(rsp.produce()));
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

    @Test public void testFind1() { this.assertMatches(true, "abc",   "abc");            }
    @Test public void testFind2() { this.assertMatches(false, "abc",   "xxabcxx");        }
    @Test public void testFind3() { this.assertMatches(false, "abc",   "xxaBcxx");        }
    @Test public void testFind4() { this.assertMatches(false, "a.c",   "xxabcxx");        }
    @Test public void testFind5() { this.assertMatches(false, "a.*b",  "xxaxxbxxbxxbxx"); }
    @Test public void testFind6() { this.assertMatches(false, "a.*?b", "xxaxxbxxbxxbxx"); }
    @Test public void testFind7() { this.assertMatches(false, "a.*+b", "xxaxxbxxbxxbxx"); }

    @Test public void testLookingAt1() { this.assertLookingAt(true,  "abc", "abcdef"); }
    @Test public void testLookingAt2() { this.assertLookingAt(false, "aBc", "abcdef"); }
    @Test public void testLookingAt3() { this.assertLookingAt(true,  "a.c", "abcdef"); }

    @Test public void testCaseInsensitive1() { this.assertMatches(false, "(?i)A", "xxxAxxx"); }
    @Test public void testCaseInsensitive2() { this.assertMatches(false, "(?i)A", "xxxaxxx"); }
    @Test public void testCaseInsensitive3() { this.assertMatches(false, "(?i)Ä", "xxxäxxx"); }
    @Test public void testCaseInsensitive4() { this.assertMatches(true,  "(?i)Ä",  "Ä"     ); }
    @Test public void testCaseInsensitive5() { this.assertMatches(false, "(?i)Ä",  "ä"     ); }

    @Test public void
    testCaseInsensitive6() {
        this.assertSequenceToString("[Aa]|[Bb]", "ab", Pattern.CASE_INSENSITIVE);
    }

    @Test public void
    testCaseInsensitive7() {
        this.assertSequenceToString("[Aa]|[Bb]|[Cc]", "abc", Pattern.CASE_INSENSITIVE);
    }

    @Test public void
    testCaseInsensitive8() {
        this.assertSequenceToString(
            "[Aa]|[Bb]|[Cc]|[Dd]|[Ee]|[Ff]|[Gg]|[Hh]|[Ii]|[Jj]|[Kk]|[Ll]|[Mm]|[Nn]|[Oo]|[Pp]|[Qq]|[Rr]|[Ss]|[Tt]|[Uu]|[Vv]|[Ww]|[Xx]|[Yy]|[Zz]",
            "abcdefghijklmnopqrstuvwxyz",
            Pattern.CASE_INSENSITIVE
        );
    }

    @Test public void
    testBoyerMooreHorspool1() {
        this.assertSequenceToString("[ak]|[,]|[Öä]", "[ak][,,,,,][äÖ]");
    }

    @Test public void
    testBoyerMooreHorspool2() {
        this.assertSequenceToString("[ak]|[abc]|[Öä]", "[ak][abc][äÖ]");
    }

    @Test public void testUnicodeCaseInsensitive1() { this.assertFind(1, "(?ui)A", "xxxAxxx");    }
    @Test public void testUnicodeCaseInsensitive2() { this.assertFind(1, "(?ui)A", "xxxaxxx");    }
    @Test public void testUnicodeCaseInsensitive3() { this.assertFind(1, "(?ui)Ä", "xxxäxxx");    }
    @Test public void testUnicodeCaseInsensitive4() { this.assertMatches(true, "(?ui)Ä", "Ä"); }
    @Test public void testUnicodeCaseInsensitive5() { this.assertMatches(true, "(?ui)Ä", "ä"); }

    @Test public void testDotall1() { this.assertFind(3, ".",     0,              " \r  "); }
    @Test public void testDotall2() { this.assertFind(4, ".",     Pattern.DOTALL, " \r  "); }
    @Test public void testDotall3() { this.assertFind(4, "(?s).", 0,              " \r  "); }

    @Test public void testLiteralRegex1() { this.assertFind(2, "$\\*",      Pattern.LITERAL,                            "$\\*xxx$\\*xxx"); }
    @Test public void testLiteralRegex2() { this.assertFind(2, "a\\",       Pattern.LITERAL | Pattern.CASE_INSENSITIVE, "a\\xxxA\\xxx");   }
    @Test public void testLiteralRegex3() { this.assertFind(0, ".\\Q.\\E.", 0,                                          " ___ ");          }
    @Test public void testLiteralRegex4() { this.assertFind(1, ".\\Q.\\E.", 0,                                          " _._ ");          }

    @Test public void testBoundaries1()  { this.assertFind(1, "^.",   0,                 "___\r___\r\n___\u2028___"); }
    @Test public void testBoundaries2()  { this.assertFind(1, ".$",   0,                 "___\r___\r\n___\u2028___"); }
    @Test public void testBoundaries3()  { this.assertFind(4, "^.",   Pattern.MULTILINE, "___\r___\r\n___\u2028___"); }
    @Test public void testBoundaries4()  { this.assertFind(4, ".$",   Pattern.MULTILINE, "___\r___\r\n___\u2028___"); }
    @Test public void testBoundaries5()  { this.assertFind(6, "\\b",  0,                 " a b c");                   }
    @Test public void testBoundaries6()  { this.assertFind(1, "\\B",  0,                 " a b c");                   }
    @Test public void testBoundaries7()  { this.assertFind(1, "\\A",  0,                 "bla\rbla");                 }
    @Test public void testBoundaries8()  { this.assertFind(3, "\\Ga", 0,                 "aaabbb");                   }
    @Test public void testBoundaries9()  { this.assertFind(1, ".\\Z", 0,                 "abc");                      }
    @Test public void testBoundaries10() { this.assertFind(1, ".\\Z", 0,                 "abc\n");                    }
    @Test public void testBoundaries11() { this.assertFind(1, ".\\Z", 0,                 "abc\r\nd");                 }
    @Test public void testBoundaries12() { this.assertFind(0, ".\\z", 0,                 "abc\n");                    }
    //@Test public void testBoundaries13() { this.assertFind(2, ".\\z", "abc\r\nd");                                    } JRE says !requireEnd !?
    @Test public void testBoundaries14() { this.assertFind(3, ".",    Pattern.MULTILINE, "abc");                      }
    @Test public void testBoundaries15() { this.assertFind(3, ".",    Pattern.MULTILINE, "abc\n");                    }
    @Test public void testBoundaries16() { this.assertFind(4, ".",    Pattern.MULTILINE, "abc\r\nd");                 }

    @Test public void testMatchFlagsGroup() { this.assertFind(2, "a(?i)b", " ab Ab aB AB "); }

    @Test public void testMatchFlagsCapturingGroup1() { this.assertFind(2, "a((?i)b)c",       " abc abC aBc aBC Abc AbC ABc ABC "); }

    @Test public void testMatchFlagsCapturingGroup2() { this.assertFind(2, "a(?<xxx>(?i)b)c", " abc abC aBc aBC Abc AbC ABc ABC "); }

    @Test public void
    testMatchFlagsNonCapturingGroup() {
        String regex = "a(?i:b)c";
        this.assertSequenceToString("[a]|[Bb]|[c]", regex);
        this.assertFind(2, regex, " abc abC aBc aBC Abc AbC ABc ABC ");
    }

    @Test public void
    testAlternatives1() {
        this.assertFind(2, "a|b",        " a b c ");
        this.assertFind(2, "a(?:b|bb)c", " ac abc abbc abbbc ");
    }

    @Test public void testAlternatives2a() { this.assertFind(4, "a|aa|aaa", " aaaa "); }
    @Test public void testAlternatives2b() { this.assertFind(4, "a|aaa|aa", " aaaa "); }
    @Test public void testAlternatives2c() { this.assertFind(2, "aa|a|aaa", " aaaa "); }
    @Test public void testAlternatives2d() { this.assertFind(2, "aa|aaa|a", " aaaa "); }
    @Test public void testAlternatives2e() { this.assertFind(2, "aaa|a|aa", " aaaa "); }
    @Test public void testAlternatives2f() { this.assertFind(2, "aaa|aa|a", " aaaa "); }

    @Test public void testIndependentGroup1() { this.assertFind(2, "(?>a|b)",    " a b c ");             }
    @Test public void testIndependentGroup2() { this.assertFind(1, "a(?>b|bb)c", " ac abc abbc abbbc "); }

    // ======================================== CHARACTER CLASSES ========================================

    @Test public void testPredefinedCharacterClasses1() { this.assertFind(3, "\\w",     " abc äöü "); }
    @Test public void testPredefinedCharacterClasses2() { this.assertFind(6, "(?U)\\w", " abc äöü "); }
    @Test public void testPredefinedCharacterClasses3() { this.assertFind(6, "\\W",     " abc äöü "); }
    @Test public void testPredefinedCharacterClasses4() { this.assertFind(3, "(?U)\\W", " abc äöü "); }

    @Test public void testPosixCharacterClasses1() { this.assertFind(3, "\\p{Lower}",     " abc äöü "); }
    @Test public void testPosixCharacterClasses2() { this.assertFind(6, "(?U)\\p{Lower}", " abc äöü "); }
    @Test public void testPosixCharacterClasses3() { this.assertFind(6, "\\P{Lower}",     " abc äöü "); }
    @Test public void testPosixCharacterClasses4() { this.assertFind(3, "(?U)\\P{Lower}", " abc äöü "); }

    @Test public void testJavaCharacterClasses1() { this.assertFind(3, "\\p{javaLowerCase}", " a B c ä Ä "); }

    @Test public void testJavaCharacterClasses2a() { this.assertFind(8, "\\P{javaLowerCase}", " a B c ä Ä ");   }
    @Test public void testJavaCharacterClasses2b() { this.assertPatternSyntaxException("\\P{JavaLowerCase}");   }
    @Test public void testJavaCharacterClasses2c() { this.assertPatternSyntaxException("\\P{JAVALOWERCASE}");   }
    @Test public void testJavaCharacterClasses2e() { this.assertPatternSyntaxException("\\P{javalowercase}");   }
    @Test public void testJavaCharacterClasses2f() { this.assertPatternSyntaxException("\\P{IsJavaLowerCase}"); }

    // By "UNICODE script":
    @Test public void testUnicodeCharacterClasses1() { this.assertFind(5, "\\p{IsLatin}",       " a B c ä Ä "); }

    // By "UNICODE block":
    @Test public void testUnicodeCharacterClasses2() { this.assertFind(1, "\\p{InGreek}",       " \u03b1 ");    }
    @Test public void testUnicodeCharacterClasses3() { this.assertFind(9, "\\p{InBasicLatin}",  " a B c ä Ä "); }
    @Test public void testUnicodeCharacterClasses4() { this.assertFind(2, "\\P{InBasicLatin}",  " a B c ä Ä "); }

    // By "UNICODE category":
    @Test public void testUnicodeCharacterClasses5() { this.assertFind(2, "\\p{Lu}",            " a B c ä Ä "); }
    @Test public void testUnicodeCharacterClasses6() { this.assertFind(9, "\\P{Lu}",            " a B c ä Ä "); }
    @Test public void testUnicodeCharacterClasses7() { this.assertFind(1, "\\p{Sc}",            " a $ ");       }
    @Test public void testUnicodeCharacterClasses8() { this.assertFind(4, "\\P{Sc}",            " a $ ");       }

    // By "UNICODE property":
    @Test public void testUnicodeCharacterClasses9()  { this.assertFind(6, "\\p{IsLowerCASE}",  " abc äöü "); }
    @Test public void testUnicodeCharacterClasses10() { this.assertFind(6, "\\p{IsAlphabetic}", " abc äöü "); }

    @Test public void
    testSupplementaryCharacterClasses() {

        // PILE OF POO
        // = 0x0001F4A9 - 0x00010000
        // = 0x0000F4A9
        // = 0000 1111 0100 1010 1001
        // = HS 00 0011 1101 + LS 00 1010 1001
        // = D800+3D + DC00+A9
        // = D83D+DCA9

        Assert.assertTrue(this.patternFactory.matches("[x\uD83D\uDCA9]", "x"));
        Assert.assertFalse(this.patternFactory.matches("[x\uD83D\uDCA9]", "\uD83D"));
        Assert.assertFalse(this.patternFactory.matches("[x\uD83D\uDCA9]", "\uDCA9"));
        Assert.assertTrue(this.patternFactory.matches("[x\uD83D\uDCA9]", "\uD83D\uDCA9"));

        Assert.assertTrue(this.patternFactory.matches("[x\\uD83D\\uDCA9]", "x"));
        Assert.assertFalse(this.patternFactory.matches("[x\\uD83D\\uDCA9]", "\uD83D"));
        Assert.assertFalse(this.patternFactory.matches("[x\\uD83D\\uDCA9]", "\uDCA9"));
        Assert.assertTrue(this.patternFactory.matches("[x\\uD83D\\uDCA9]", "\uD83D\uDCA9"));

        Assert.assertTrue(this.patternFactory.matches("[x\\x{1F4A9}]", "x"));
        Assert.assertFalse(this.patternFactory.matches("[x\\x{1F4A9}]", "\uD83D"));
        Assert.assertFalse(this.patternFactory.matches("[x\\x{1F4A9}]", "\uDCA9"));
        Assert.assertTrue(this.patternFactory.matches("[x\\x{1F4A9}]", "\uD83D\uDCA9"));
    }

    // ======================================== END OF CHARACTER CLASSES ========================================

    @Test public void testCapturingGroups() { this.assertFind(3, "((a+)(b+))", " abbb aabb aaab "); }

    @Test public void testNamedCapturingGroups1() { this.assertFind(3, "(?<xxx>a+)", " a aa aaa"); }

    @Test public void
    testNamedCapturingGroups2() {

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

    @Test public void testNamedCapturingGroupsBackreference1() { this.assertFind(2, "(?<first>\\w)\\k<first>", " a aa aaa"); }

    @Test public void
    testNamedCapturingGroupsBackreference2() {

        // Backreference to inexistent named group.
        this.assertPatternSyntaxException("(?<first>\\w)\\k<bla>");
    }

    @Test public void testPositiveLookahead1() { this.assertFind(2, "a(?=b)",   " a aba abba a"); }
    @Test public void testPositiveLookahead2() { this.assertFind(2, "a(?=(b))", " a aba abba a"); }

    @Test public void testNegativeLookahead1() { this.assertFind(4, "a(?!b)",   " a aba abba a"); }
    @Test public void testNegativeLookahead2() { this.assertFind(4, "a(?!(b))", " a aba abba a"); }

    @Test public void testPositiveLookbehind1() { this.assertFind(2, "(?<=b)a",     " a aba abba a"); }
    @Test public void testPositiveLookbehind2() { this.assertFind(2, "(?<=(b))a",   " a aba abba a"); }
    @Test public void testPositiveLookbehind3() { this.assertFind(1, "(?<=\\R )a",  " \r\n a ");      }
    @Test public void testPositiveLookbehind4() { this.assertFind(1, "(?<=\\R )a",  " \r a ");        }
    @Test public void testPositiveLookbehind5() { this.assertFind(1, "(?<=\\R )a",  " \n a ");        }

    @Test public void testPositiveLookbehind6() { this.assertFind(3, "(?<=^\t*)\t", "\t\t\tpublic static void main()"); }

    @Test public void
    testPositiveLookbehind7() {
        this.patternFactory.compile("(?<=^\\s*)    ").matcher("        public static void main()").replaceAll("\t");
    }

    @Test public void testNegativeLookbehind1() { this.assertFind(4, "(?<!b)a",     " a aba abba a"); }
    @Test public void testNegativeLookbehind2() { this.assertFind(4, "(?<!(b))a",   " a aba abba a"); }
    @Test public void testNegativeLookbehind3() { this.assertFind(4, "(?<!(?:b))a", " a aba abba a"); }
    @Test public void testNegativeLookbehind4() { this.assertFind(4, "(?<!b)a",     " a aba abba a"); }

    @Test public void testRegion1() { this.assertFind(5, "a", 0, "__a__ a aba abba __a__", 5, 17);               }
    @Test public void testRegion2() { this.assertFind(1, "^", 0, "__a__ a aba abba __a__", 5, 17);               }
    @Test public void testRegion3() { this.assertFind(0, "^", 0, "__a__ a aba abba __a__", 5, 17, false, false); }
    @Test public void testRegion4() { this.assertFind(1, "^", 0, "__a__ a aba abba __a__", 5, 17, false, true);  }
    @Test public void testRegion5() { this.assertFind(0, "^", 0, "__a__ a aba abba __a__", 5, 17, true,  false); }
    @Test public void testRegion6() { this.assertFind(1, "^", 0, "__a__ a aba abba __a__", 5, 17, true,  true);  }

    @Test public void testTransparentBounds1() { this.assertFind(8, "\\b",     0, "__a__ a aba abba __a__", 5, 17, true); }

    // Lookahead.
    @Test public void testTransparentBounds2() { this.assertFind(1, " (?=_)",  0, "__a__ a aba abba __a__", 5, 17, true); }
    @Test public void testTransparentBounds3() { this.assertFind(3, " (?!_)",  0, "__a__ a aba abba __a__", 5, 17, true); }

    // Lookbehind.
    @Test public void testTransparentBounds4() { this.assertFind(1, "(?<=_) ", 0, "__a__ a aba abba __a__", 5, 17, true); }
    @Test public void testTransparentBounds5() { this.assertFind(3, "(?<!_) ", 0, "__a__ a aba abba __a__", 5, 17, true); }

    @Test public void testAnchoringBounds1() { this.assertFind(0, "^", 0, "__a__ a aba abba __a__", 5, 17, null, false); }
    @Test public void testAnchoringBounds2() { this.assertFind(0, "$", 0, "__a__ a aba abba __a__", 5, 17, null, false); }

    @Test public void testUnixLines1() { this.assertFind(3, "\\R", 0,                  "  \n  \r\n \u2028 "); }
    @Test public void testUnixLines2() { this.assertFind(3, "\\R", Pattern.UNIX_LINES, "  \n  \r\n \u2028 "); }
    @Test public void testUnixLines3() { this.assertFind(1, "^",   0,                  "  \n  \r\n \u2028 "); }
    @Test public void testUnixLines4() { this.assertFind(1, "^",   Pattern.UNIX_LINES, "  \n  \r\n \u2028 "); }

    @Test public void testQuantifiers1a() { this.assertFind(6, "a?",     " aaa "); }
    @Test public void testQuantifiers1b() { this.assertFind(6, "a??",    " aaa "); }
    @Test public void testQuantifiers1c() { this.assertFind(6, "a?+",    " aaa "); }
    @Test public void testQuantifiers2a() { this.assertFind(4, "a*",     " aaa "); }
    @Test public void testQuantifiers2b() { this.assertFind(6, "a*?",    " aaa "); }
    @Test public void testQuantifiers2c() { this.assertFind(4, "a*+",    " aaa "); }
    @Test public void testQuantifiers3a() { this.assertFind(1, "a+",     " aaa "); }
    @Test public void testQuantifiers3b() { this.assertFind(3, "a+?",    " aaa "); }
    @Test public void testQuantifiers3c() { this.assertFind(1, "a++",    " aaa "); }
    @Test public void testQuantifiers4a() { this.assertFind(6, "a{0}",   " aaa "); }
    @Test public void testQuantifiers4b() { this.assertFind(6, "a{0}?",  " aaa "); }
    @Test public void testQuantifiers4c() { this.assertFind(6, "a{0}+",  " aaa "); }
    @Test public void testQuantifiers5a() { this.assertFind(3, "a{1}",   " aaa "); }
    @Test public void testQuantifiers5b() { this.assertFind(3, "a{1}?",  " aaa "); }
    @Test public void testQuantifiers5c() { this.assertFind(3, "a{1}+",  " aaa "); }
    @Test public void testQuantifiers6a() { this.assertFind(1, "a{2}",   " aaa "); }
    @Test public void testQuantifiers6b() { this.assertFind(1, "a{2}?",  " aaa "); }
    @Test public void testQuantifiers6c() { this.assertFind(1, "a{2}+",  " aaa "); }
    @Test public void testQuantifiers7a() { this.assertFind(4, "a{0,}",  " aaa "); }
    @Test public void testQuantifiers7b() { this.assertFind(6, "a{0,}?", " aaa "); }
    @Test public void testQuantifiers7c() { this.assertFind(4, "a{0,}+", " aaa "); }

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

    @Test public void testSurrogates1() { this.assertFind(1, PatternTest.clefUnicode,       PatternTest.clefUnicode);            }
    @Test public void testSurrogates2() { this.assertFind(1, PatternTest.clefUnicode + "?", "");                                 }
//    @Test public void testSurrogates3() { this.assertFind(1, PatternTest.clefUnicode + "?", "" + PatternTest.clefHighSurrogate); } 
    @Test public void testSurrogates4() { this.assertFind(2, PatternTest.clefUnicode + "?", "" + PatternTest.clefLowSurrogate);  } 
    @Test public void testSurrogates5() { this.assertFind(2, PatternTest.clefUnicode + "?", PatternTest.clefUnicode);            }

    @Test public void
    testSurrogates6() {
//        this.assertFind(1, 
//            PatternTest.clefUnicode + "?",
//            "" + PatternTest.clefLowSurrogate + PatternTest.clefHighSurrogate // <= high/low surrogates reversed!
//        );
    }

    @Test public void
    testPreviousMatchBoundary() {

        // From: http://stackoverflow.com/questions/2708833
        this.assertFind(
            3,
            "(?<=\\G\\d{3})(?=\\d)" + "|" + "(?<=^-?\\d{1,3})(?=(?:\\d{3})+(?!\\d))",
            "-1234567890.1234567890"
        );
    }

    @Test public void testAtomicGroups1() { this.assertFind(1, "^a(bc|b)c$",   "abc");  }
    @Test public void testAtomicGroups2() { this.assertFind(1, "^a(bc|b)c$",   "abcc"); }
    @Test public void testAtomicGroups3() { this.assertFind(0, "^a(?>bc|b)c$", "abc");  }
    @Test public void testAtomicGroups4() { this.assertFind(1, "^a(?>bc|b)c$", "abcc"); }

    /**
     * @see <a href="http://stackoverflow.com/questions/17618812">Clarification about requireEnd Matcher's method</a>
     */
    @Test public void testRequireEnd1() { this.assertFind(1, "cat$",           "I have a cat");     }
    @Test public void testRequireEnd2() { this.assertFind(0, "cat$",           "I have a catflap"); }
    @Test public void testRequireEnd3() { this.assertFind(1, "cat",            "I have a cat");     }
    @Test public void testRequireEnd4() { this.assertFind(1, "cat",            "I have a catflap"); }
    @Test public void testRequireEnd5() { this.assertFind(1, "\\d+\\b|[><]=?", "1234");             }
    @Test public void testRequireEnd6() { this.assertFind(1, "\\d+\\b|[><]=?", ">=");               }
    @Test public void testRequireEnd7() { this.assertFind(1, "\\d+\\b|[><]=?", "<");                }

    @Test public void testComments1()  { this.assertFind(1, " a# comment \nb ",    Pattern.COMMENTS, " ab a# comment \nb"); }
    @Test public void testComments2()  { this.assertFind(1, "(?x)  a  ",           0,                " a ");                                  }
    @Test public void testComments3()  { this.assertFind(0, "(?x)  a  (?-x) b",    0,                " ab ");                                 }
    @Test public void testComments4()  { this.assertFind(1, "(?x)  a  (?-x) b",    0,                " a b ");                                }
    @Test public void testComments5()  { this.assertFind(0, "(?x)  a#\n  (?-x) b", 0,                " ab ");                                 }
    @Test public void testComments6()  { this.assertFind(1, "(?x)  a#\n  (?-x) b", 0,                " a b ");                                }
    @Test public void testComments7()  { this.assertFind(1, "(?x)  (a)",           0,                " a b ");                                }
    @Test public void testComments8()  { this.assertFind(1, "(?x)  (?:a)",         0,                " a b ");                                }
    @Test public void testComments9()  { this.assertFind(1, "(?x)  ( ?:a)",        0,                " a b ");                                }
    @Test public void testComments10() { this.assertFind(1, "(?x)  (?: a)",        0,                " a b ");                                }
    @Test public void testComments11() { this.assertFind(1, "(?x)  (? : a)",       0,                " a b ");                                }
    @Test public void testComments12() { this.assertFind(1, "(?x)  ( ? :a)",       0,                " a b ");                                }
    @Test public void testComments13() { this.assertFind(1, "(?x)  ( ?: a)",       0,                " a b ");                                }
    @Test public void testComments14() { this.assertFind(1, "(?x)  ( ? : a)",      0,                " a b ");                                }
    @Test public void testComments15() { this.assertFind(1, "(?x)  (?<name>a)",    0,                " a b ");                                }
    @Test public void testComments16() { this.assertFind(1, "(?x)  ( ?<name>a)",   0,                " a b ");                                }
    @Test public void testComments17() { this.assertPatternSyntaxException("(?x)  (? <name>a)");   }
    @Test public void testComments18() { this.assertFind(1, "(?x)  (?< name>a)",   0,                " a b ");                                }
    @Test public void testComments19() { this.assertPatternSyntaxException("(?x)  (? < name>a)");  }
    @Test public void testComments20() { this.assertFind(1, "(?x)  ( ?< name>a)",  0,                " a b ");                                }
    @Test public void testComments21() { this.assertPatternSyntaxException("(?x)  ( ? < name>a)"); }

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

        Assert.assertEquals(
            " <<a>>bc ",
            this.patternFactory.compile("(?<grp>a)").matcher(" abc ").replaceAll("<<${grp}>>")
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

    @Test public void testCharacterClassOptimizations1() { this.assertSequenceToString("'A'",                                            "[A]");                }
    @Test public void testCharacterClassOptimizations2() { this.assertSequenceToString("oneOfTwoChars('A', 'B')",                        "[AB]");               }
    @Test public void testCharacterClassOptimizations3() { this.assertSequenceToString("oneOfTwoChars('A', 'K')",                        "[AK]");               }
    @Test public void testCharacterClassOptimizations4() { this.assertSequenceToString("bitSet('A', 'C', 'E', 'G', 'I', 'K')",           "[ACEGIK]");           }
    @Test public void testCharacterClassOptimizations5() { this.assertSequenceToString("charRange('A' - 'E')",                           "[A-E]");              }
    @Test public void testCharacterClassOptimizations6() { this.assertSequenceToString("bitSet('D', 'E', 'F', 'G', 'H', 'I', 'J', 'K')", "[A-K&&D-Z]");         }
    @Test public void testCharacterClassOptimizations7() { this.assertSequenceToString(PatternTest.jurpc("set\\('.'(?:, '.'){63}\\)"),   "[A-Za-z0-9_\u0400]"); }

    @Test public void testQuantifierOptimizations1()  { this.assertSequenceToString("'A'",                                                                                                      "A");                      }
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
            this.assertFind(1, new String(new char[] { c }), 0,                        new String(tripleCaseLetters));
            this.assertFind(1, new String(new char[] { c }), Pattern.CASE_INSENSITIVE, new String(tripleCaseLetters));
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

        Matcher m = this.patternFactory.compile("(?<=((abc)(def)))ghi").matcher("   abcdefghi   ");
        Assert.assertTrue(m.find());
        Assert.assertEquals(3, m.groupCount());

        Assert.assertEquals("ghi", m.group());

        Assert.assertEquals("abcdef", m.group(1));
        Assert.assertEquals("abc", m.group(2));
        Assert.assertEquals("def", m.group(3));
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

    @Test public void
    testReplacements() {

        this.assertReplaceAllThrows(IllegalArgumentException.class, "(?<ncg>.)", "a", "${a + + b}"); // Parse erroe
        this.assertReplaceAllThrows(IllegalArgumentException.class, "(?<ncg>.)", "a", "${x}");       // Unknown variable

        this.assertReplaceAllEquals("A", "(?<ncg>.)", "a",      "A"); // Simple replacement
        this.assertReplaceAllEquals("a", "(?<ncg>.)", "a", "${ncg}"); // Named capturing group reference
        this.assertReplaceAllEquals("z", "(?<ncg>.)", "a",    "\\z"); // Redundant backslash (\z has no special meaning)
        this.assertReplaceAllEquals("$", "(?<ncg>.)", "a",    "\\$"); // Masked dollar sign

        if (!this.patternFactory.getId().equals("java.util.regex")) {

            // Extended replacement constructs (supported by lfr, but not by jur).
            this.assertReplaceAllEquals("A",                 "(?<ncg>.)", "a", "\\0101");                      // Octal literal
            this.assertReplaceAllEquals("A",                 "(?<ncg>.)", "a", "${m.group(1).toUpperCase()}"); // Expression
            this.assertReplaceAllEquals("A",                 "(?<ncg>.)", "a", "\\x41");                       // Hex literal
            this.assertReplaceAllEquals("A",                 "(?<ncg>.)", "a", "\\u0041");                     // Hex literal
            this.assertReplaceAllEquals("A",                 "(?<ncg>.)", "a", "\\x{41}");                     // Hex literal
            this.assertReplaceAllEquals("A",                 "(?<ncg>.)", "a", "\\x{00000041}");               // Hex literal
            this.assertReplaceAllEquals("\\\\",              "(?<ncg>.)", "a", "\\Q\\\\E\\\\");                // Literal text
            this.assertReplaceAllEquals("\\",                "(?<ncg>.)", "a", "\\Q\\");                       // Literal text
            this.assertReplaceAllEquals("\t\n\r\f\07\033\b", "(?<ncg>.)", "a", "\\t\\n\\r\\f\\a\\e\\b");       // TAB NL CR FF BEL ESC BACKSPACE
            this.assertReplaceAllEquals("\3",                "(?<ncg>.)", "a", "\\cC");                        // Control character
            this.assertReplaceAllEquals("_A_A_\\x41_X_",     "(?<ncg>.)", "a", "_\\0101_${m.group(1).toUpperCase()}_\\Q\\x41\\E_\\X_");
        }
    }

    private void
    assertReplaceAllEquals(String expected, String regex, String subject, String replacement) {
        Assert.assertEquals(expected, this.patternFactory.compile(regex).matcher(subject).replaceAll(replacement));
    }

    private void
    assertReplaceAllThrows(Class<? extends Throwable> expectedException, String regex, String subject, String replacement) {
        try {
            this.patternFactory.compile(regex).matcher(subject).replaceAll(replacement);
            Assert.fail("Exception expected");
        } catch (Exception e) {
            if (expectedException.isAssignableFrom(e.getClass())) {
                ;
            } else {
                Assert.fail(expectedException.getName() + " expected instead of " + e.getClass().getName());
            }
        }
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

    private Matcher
    assertFind(int expected, String regex, String subject) {
        return this.assertFind(expected, regex, 0 /*flags*/, subject);
    }
    
    private Matcher
    assertFind(int expected, String regex, int flags, String subject) {
        return this.assertFind(expected, regex, flags, subject, null /*regionStart*/, -1 /*regionEnd*/);
    }
    
    private Matcher
    assertFind(int expected, String regex, int flags, String subject, @Nullable Integer regionStart, int regionEnd) {
        return this.assertFind(
            expected,
            regex,
            flags,
            subject,
            regionStart,
            regionEnd,
            null /*transparentBounds*/
        );
    }
    
    private Matcher
    assertFind(
        int               expected,
        String            regex,
        int               flags,
        String            subject,
        @Nullable Integer regionStart,
        int               regionEnd,
        @Nullable Boolean transparentBounds
    ) {
        return this.assertFind(
            expected,
            regex,
            flags,
            subject,
            regionStart,
            regionEnd,
            transparentBounds,
            null /*anchoringBounds*/
        );
    }

    private Matcher
    assertFind(
        int               expected,
        String            regex,
        int               flags,
        String            subject,
        @Nullable Integer regionStart,
        int               regionEnd,
        @Nullable Boolean transparentBounds,
        @Nullable Boolean anchoringBounds
    ) {
        assert subject != null;
        
        Matcher m = this.patternFactory.compile(regex, flags).matcher(subject);
        
        if (regionStart != null) m.region(regionStart, regionEnd);

        if (transparentBounds != null) m.useTransparentBounds(transparentBounds);
        if (anchoringBounds   != null) m.useAnchoringBounds(anchoringBounds);

        int count = 0;
        while (m.find()) count++;
        Assert.assertEquals(expected, count);
        
        return m;
    }

    private Matcher
    assertMatches(boolean expected, String regex, String subject) {
        Matcher m = this.patternFactory.compile(regex).matcher(subject);
        Assert.assertEquals(expected, m.matches());
        return m;
    }
    
    private Matcher
    assertLookingAt(boolean expected, String regex, String subject) {
        Matcher m = this.patternFactory.compile(regex).matcher(subject);
        Assert.assertEquals(expected, m.lookingAt());
        return m;
    }
}
