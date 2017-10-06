
/*
 * de.unkrig.lfr - A super-fast regular expression evaluator
 *
 * Copyright (c) 2017, Arno Unkrig
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

package de.unkrig.lfr.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.unkrig.commons.lang.Characters;
import de.unkrig.commons.lang.PrettyPrinter;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.util.collections.CollectionUtil;
import de.unkrig.commons.util.collections.Sets;

/**
 * Utility methods that create all kinds of {@link CharacterClass}s.
 */
final
class CharacterClasses {

    /**
     * Always causes a match to fail.
     */
    public static final CharacterClass
    FAIL = new CharacterClass(Integer.MAX_VALUE, -1) {

        @Override public boolean   matches(int c)                { return false;  }
        @Override public int       find(MatcherImpl matcherImpl) { return -1;     }
        @Override public Sequence  concat(Sequence that)         { return this;   }
        @Override public String    toString()                    { return "fail"; }
        @Override protected String toStringWithoutNext()         { return "???";  }
    };

    private CharacterClasses() {}

    /**
     * Representation of a literal <em>BMP</em> (one-char) code point, like {@code "a"} or {@code "\."}.
     */
    public static
    class LiteralChar extends MultivalentCharClass {

        /**
         * The literal character that this sequence represents.
         */
        final int c;

        LiteralChar(char c) {
            super(Collections.singleton((int) c));
            this.c = c;
        }

        @Override public boolean
        matches(int subject) { return subject == this.c; }

        @Override public int lowerBound() { return this.c;     }
        @Override public int upperBound() { return this.c + 1; }
        @Override public int sizeBound()  { return 1;          }

        @Override public String
        toStringWithoutNext() { return "'" + new String(Character.toChars(this.c)) + '\''; }
    }

    /**
     * Delegates to an {@link Predicate Predicate&lt;Integer>}, where the subject is the character's code point.
     */
    public static CharacterClass
    characterClass(final Predicate<Integer> predicate) {

        return new CharacterClass() {
            @Override public boolean matches(int subject)  { return predicate.evaluate(subject); }
            @Override public String  toStringWithoutNext() { return predicate.toString();        }
        };
    }

    /**
     * Checks whether the code point's block equals the given <var>block</var>.
     *
     * @see Character.UnicodeBlock#of(int)
     */
    public static CharacterClass
    inUnicodeBlock(final Character.UnicodeBlock block) {

        return new CharacterClass() {

            @Override public boolean
            matches(int subject) { return Character.UnicodeBlock.of(subject) == block; }

            @Override public String
            toStringWithoutNext() { return "inUnicodeBlock(" + block + ")"; }
        };
    }

    /**
     * Checks whether the code point's "general category" equals the given <var>generalCategory</var>.
     *
     * @param generalCategory One of the "general category" constants in {@link Character}
     */
    public static CharacterClass
    inUnicodeGeneralCategory(final int generalCategory) {

        return new CharacterClass() {

            @Override public boolean
            matches(int subject) { return Character.getType(subject) == generalCategory; }

            @Override public String
            toStringWithoutNext() { return "inUnicodeGeneralCategory(" + generalCategory + ")"; }
        };
    }

    /**
     * Checks whether a code point equals the given <var>codePoint</var>
     */
    public static CharacterClass
    literalCharacter(final int codePoint) {

        // Optimize for non-supplementary code points.
        if (!Character.isSupplementaryCodePoint(codePoint)) return new LiteralChar((char) codePoint);

        return new CharacterClass(Character.charCount(codePoint)) {

            @Override public boolean
            matches(int c) { return c == codePoint; }

            @Override protected String
            toStringWithoutNext() { return "\\x{" + Integer.toHexString(codePoint) + "}"; }
        };
    }

    /**
     * Representation of a two-characters union, e.g. "[oO]".
     */
    public static CharacterClass
    oneOfTwoCodePoints(final int cp1, final int cp2) {
        return (
            cp1 == cp2
            ? CharacterClasses.literalCharacter(cp1)
            : Character.isSupplementaryCodePoint(cp1) || Character.isSupplementaryCodePoint(cp2)
            ? new MultivalentCharacterClass(Sets.of(cp1, cp2)) {

                @Override public boolean
                matches(int subject) { return subject == cp1 || subject == cp2; }

                @Override public String
                toStringWithoutNext() {
                    return new StringBuilder("oneOfTwoCodePoints('").appendCodePoint(cp1).append("', '").appendCodePoint(cp2).append("')").toString(); // SUPPRESS CHECKSTYLE LineLength
                }
            }
            : new MultivalentCharClass(Sets.of(cp1, cp2)) {

                @Override public boolean
                matches(int subject) { return subject == cp1 || subject == cp2; }

                @Override public String
                toStringWithoutNext() {
                    return new StringBuilder("oneOfTwoChars('").appendCodePoint(cp1).append("', '").appendCodePoint(cp2).append("')").toString(); // SUPPRESS CHECKSTYLE LineLength
                }
            }
        );
    }

    /**
     * Representation of a three-characters union, e.g. "[abc]".
     */
    public static CharacterClass
    oneOfThreeCodePoints(final int cp1, final int cp2, final int cp3) {

        return (
            cp1 == cp2 ? CharacterClasses.oneOfTwoCodePoints(cp1, cp3) :
            cp1 == cp3 ? CharacterClasses.oneOfTwoCodePoints(cp1, cp2) :
            cp2 == cp3 ? CharacterClasses.oneOfTwoCodePoints(cp1, cp2) :
            (
                Character.isSupplementaryCodePoint(cp1)
                || Character.isSupplementaryCodePoint(cp2)
                || Character.isSupplementaryCodePoint(cp3)
            ) ? new MultivalentCharacterClass(Sets.of(cp1, cp2, cp3)) {

                @Override public boolean
                matches(int subject) { return subject == cp1 || subject == cp2 || subject == cp3; }

                @Override public String
                toStringWithoutNext() {
                    return "oneOfThreeCodePoints(" + PrettyPrinter.codePointToString(cp1) + ", " + PrettyPrinter.codePointToString(cp2) + ", " + PrettyPrinter.codePointToString(cp3) + ")"; // SUPPRESS CHECKSTYLE LineLength
                }
            } :
            new MultivalentCharClass(Sets.of(cp1, cp2, cp3)) {

                @Override public boolean
                matches(int subject) { return subject == cp1 || subject == cp2 || subject == cp3; }

                @Override public String
                toStringWithoutNext() {
                    return "oneOfThreeChars(" + PrettyPrinter.codePointToString(cp1) + ", " + PrettyPrinter.codePointToString(cp2) + ", " + PrettyPrinter.codePointToString(cp3) + ")"; // SUPPRESS CHECKSTYLE LineLength
                }
            }
        );
    }

    /**
     * Checks whether a code point is one of those in in <var>chars</var>.
     */
    public static CharacterClass
    oneOfManyCodePoints(final String chars) {

        if (chars.isEmpty()) return CharacterClasses.FAIL;

        Set<Integer> s = new HashSet<Integer>();
        for (int offset = 0; offset < chars.length();) {
            int cp = chars.codePointAt(offset);
            s.add(cp);
            offset += Character.charCount(cp);
        }

        return CharacterClasses.oneOfManyCodePoints(s);
    }

    /**
     * Checks whether a code point is one of those in in <var>chars</var>.
     */
    public static CharacterClass
    oneOfManyCodePoints(Collection<Integer> codePoints) {
        return CharacterClasses.oneOfManyCodePoints(
            codePoints instanceof HashSet
            ? (HashSet<Integer>) codePoints
            : new HashSet<Integer>(codePoints)
        );
    }

    /**
     * Checks whether a code point is one of the <var>codePoints</var>.
     */
    public static CharacterClass
    oneOfManyCodePoints(final HashSet<Integer> codePoints) {

        NO_SUPPLEMENTARIES: {
            for (int cp : codePoints) {
                if (Character.isSupplementaryCodePoint(cp)) break NO_SUPPLEMENTARIES;
            }

            return CharacterClasses.oneOfManyChars(codePoints);
        }

        Iterator<Integer> it = codePoints.iterator();
        switch (codePoints.size()) {

        case 0:  return CharacterClasses.FAIL;
        case 1:  return CharacterClasses.literalCharacter(codePoints.iterator().next());
        case 2:  return oneOfTwoCodePoints(it.next(), it.next());
        case 3:  return oneOfThreeCodePoints(it.next(), it.next(), it.next());
        default: return new MultivalentCharacterClass(codePoints);
        }
    }

    /**
     * Sorts the <var>chars</var> and implements a character class that is based on a binary search in that
     * array.
     */
    public static CharacterClass
    oneOfManyChars(final Set<Integer> chars) {

        final int[] chars2 = CollectionUtil.toIntArray(chars);
        Arrays.sort(chars2);

        return new MultivalentCharClass(chars) {

            @Override public boolean
            matches(int subject) { return Arrays.binarySearch(chars2, subject) >= 0; }

            @Override protected String
            toStringWithoutNext() {
                return "oneOfManyCharsBinarySearch(" + Arrays.toString(chars2) + ")";
            }
        };
    }

    /**
     * Sorts the <var>codePoints</var> and implements a character class that is based on a binary search in that
     * array.
     */
    public static CharacterClass
    oneOfManyCodePoints(final int[] codePoints) {

        Arrays.sort(codePoints);

        return new CharacterClass() {

            @Override public boolean
            matches(int subject) { return Arrays.binarySearch(codePoints, subject) >= 0; }

            @Override protected String
            toStringWithoutNext() {
                return "oneOfManyCodePointsBinarySearch(" + PrettyPrinter.toString(codePoints) + ")";
            }
        };
    }

    /**
     * Representation of an (ASCII-)case-insensitive literal character.
     */
    public static CharacterClass
    caseInsensitiveLiteralCharacter(final int c) {
        if (c >= 'A' && c <= 'Z') return CharacterClasses.oneOfTwoCodePoints(c, c + 32);
        if (c >= 'a' && c <= 'z') return CharacterClasses.oneOfTwoCodePoints(c, c - 32);
        return CharacterClasses.literalCharacter(c);
    }

    /**
     * Representation of a (UNICODE-)case-insensitive literal character.
     */
    public static CharacterClass
    unicodeCaseInsensitiveLiteralCharacter(final int cp) {

        String s = Characters.caseInsensitivelyEqualCharacters(cp);

        if (s == null) return CharacterClasses.literalCharacter(cp);

        return CharacterClasses.oneOfManyCodePoints(s);
    }

    /**
     * @return The (highly optimized) union of the <var>lhs</var> and the <var>rhs</var> character classes
     */
    public static CharacterClass
    union(final CharacterClass lhs, final CharacterClass rhs) {

        if (lhs == CharacterClasses.FAIL) return rhs;

        if (rhs == CharacterClasses.FAIL) return lhs;

        // Optimization when both operands are multivalent code points.
        if (lhs instanceof MultivalentCharacterClass && rhs instanceof MultivalentCharacterClass) {
            MultivalentCharacterClass lhs2 = (MultivalentCharacterClass) lhs;
            MultivalentCharacterClass rhs2 = (MultivalentCharacterClass) rhs;

            return oneOfManyCodePoints(Sets.union(lhs2.codePoints, rhs2.codePoints));
        }

        final int lb = Math.min(lhs.lowerBound(), rhs.lowerBound());
        final int ub = Math.max(lhs.upperBound(), rhs.upperBound());
        final int sb = lhs.sizeBound() + rhs.sizeBound();

        return new CharacterClass(Character.charCount(lb), Character.charCount(ub - 1)) {

            @Override public boolean
            matches(int c) { return lhs.matches(c) || rhs.matches(c); }

            @Override public int lowerBound() { return lb; }
            @Override public int upperBound() { return ub; }
            @Override public int sizeBound()  { return sb; }

            @Override protected String
            toStringWithoutNext() { return "union(" + lhs + ", " + rhs + ')'; }
        };
    }

    /**
     * @return <var>cc</var>, or a perfomance-optimized equivalent of it
     */
    public static CharacterClass
    optimize(CharacterClass cc) {

        // Some performance tests on JRE 8 with "-server":
        //   "c == this.c"                  => 1.1 us
        //   "c == this.c1 || c == this.c2" => 2.1 us
        //   "this.s.indexOf(c) == -1"      => 9.8 us  (s.length() == 16)
        //   "this.s.indexOf(c) == -1"      => 5.2 us  (s.length() == 3)
        //   "this.s.indexOf(c) == -1"      => 3.8 us  (s.length() == 1)
        //   "this.intArray[i] == c"        => identical as "s.indexOf(c)"
        //   "this.intHashSet.contains(c)"  => 6.0 us  (s.length() == 1)
        //   "this.intHashSet.contains(c)"  => 5.7 us  (s.length() == 3)
        //   "this.intHashSet.contains(c)"  => 5.7 us  (s.length() == 16)
        //   "this.bitSet.get(c)"           => 3.1 us

        // Summary:
        // For 1 character,          "c == this.c"                  is optimal (1.1 us)
        // For 2 characters,         "c == this.c1 || c == this.c2" is optimal (2.1 us)
        // For 3 or more characters, "this.bitSet.get(c)"           is optimal (3.1 us)
        // If bitSet cannot be used (c much greater than 256),
        //                           "this.intHashSet.contains()"   is optimal (5.7 us)
        // The following are optimal in none of the cases, and are thus not useful:
        //    "this.s.indexOf(c) == -1"
        //    "this.intArray[i] == c"

        // Convert the character class into a Set<Integer>, and determine the minimum and maximum value.
        final Set<Integer> integerSet;
        final int          minValue, maxValue;
        {
            final int lb = cc.lowerBound(), ub = cc.upperBound();

            if (ub - lb > 1000) {

                // Character class is too widely spread for set optimization.
                return cc;
            }

            integerSet = new HashSet<Integer>();

            int min = Integer.MAX_VALUE, max = 0;
            for (int c = lb; c < ub; c++) {
                if (cc.matches(c)) {
                    integerSet.add(c);
                    if (c < min) min = c;
                    if (c > max) max = c;
                }
            }

            minValue = min;
            maxValue = max;
        }

        // Transform the "integerSet" into a character class.
        final int size = integerSet.size();
        switch (size) {

        case 0:
            return CharacterClasses.FAIL;

        case 1:
            return CharacterClasses.literalCharacter(integerSet.iterator().next());

        case 2:
            {
                Iterator<Integer> it = integerSet.iterator();
                return CharacterClasses.oneOfTwoCodePoints(it.next(), it.next());
            }

        case 3:
            {
                Iterator<Integer> it = integerSet.iterator();
                return CharacterClasses.oneOfThreeCodePoints(it.next(), it.next(), it.next());
            }
        }

        if (maxValue < 256) {

            // Optimize into a "BitSet" if the maxValue is small enough.
            final BitSet bitSet = new BitSet(maxValue + 1);
            for (int c : integerSet) bitSet.set(c);

            return new CharacterClass(1) {

                @Override public boolean matches(int c) { return bitSet.get(c); }
                @Override public int     lowerBound()   { return minValue;      }
                @Override public int     upperBound()   { return maxValue + 1;  }
                @Override public int     sizeBound()    { return size;          }

                @Override public String
                toStringWithoutNext() {
                    StringBuilder sb = new StringBuilder("bitSet(");
                    for (int i = minValue; i <= maxValue; i++) {
                        if (bitSet.get(i)) {
                            (sb.length() == 7 ? sb.append('\'') : sb.append(", '")).append((char) i).append('\'');
                        }
                    }
                    return sb.append(')').toString();
                }
            };
        } else {

            return new CharacterClass(1, 2) {

                @Override public boolean matches(int c) { return integerSet.contains(c); }
                @Override public int     lowerBound()   { return minValue;               }
                @Override public int     upperBound()   { return maxValue + 1;           }
                @Override public int     sizeBound()    { return size;                   }

                @Override public String
                toStringWithoutNext() {

                    ArrayList<Integer> l = new ArrayList<Integer>(integerSet);
                    Collections.sort(l);

                    StringBuilder sb = new StringBuilder("set('").appendCodePoint(l.get(0));
                    for (int i = 1; i < l.size(); i++) sb.append("', '").appendCodePoint(l.get(i));
                    return sb.append("')").toString();
                }
            };
        }
    }

    /**
     * Representation of a character class intersection like {@code "\w&&[^abc]"}.
     */
    public static CharacterClass
    intersection(final CharacterClass lhs, final CharacterClass rhs) {

        assert lhs.next == Sequences.TERMINAL;
        assert rhs.next == Sequences.TERMINAL;

        // Optimization when both operands are multivalent character classes.
        if (lhs instanceof MultivalentCharacterClass && rhs instanceof MultivalentCharacterClass) {
            MultivalentCharacterClass lhs2 = (MultivalentCharacterClass) lhs;
            MultivalentCharacterClass rhs2 = (MultivalentCharacterClass) rhs;

            return oneOfManyCodePoints(Sets.intersection(lhs2.codePoints, rhs2.codePoints));
        }

        final int lb = Math.max(lhs.lowerBound(), rhs.lowerBound());
        final int ub = Math.min(lhs.upperBound(), rhs.upperBound());
        final int sb = Math.min(lhs.sizeBound(),  rhs.sizeBound());

        return new CharacterClass(Character.charCount(lb), Character.charCount(ub - 1)) {

            @Override public boolean
            matches(int subject) { return lhs.matches(subject) && rhs.matches(subject); }

            @Override public int lowerBound() { return lb; }
            @Override public int upperBound() { return ub; }
            @Override public int sizeBound()  { return sb; }

            @Override public String
            toStringWithoutNext() { return "intersection(" + lhs + ", " + rhs + ")"; }
        };
    }

    /**
     * Representation of a character class range like {@code "a-k"}.
     */
    public static CharacterClass
    range(final int lhs, final int rhs) {

        if (lhs == rhs) return CharacterClasses.literalCharacter(lhs);

        if (lhs > rhs) return CharacterClasses.FAIL;

        return new CharacterClass(Character.charCount(lhs), Character.charCount(rhs)) {

            @Override public boolean
            matches(int subject) { return (subject >= lhs && subject <= rhs); }

            @Override public int lowerBound() { return lhs;           }
            @Override public int upperBound() { return rhs + 1;       }
            @Override public int sizeBound()  { return rhs - lhs + 1; }

            @Override public String
            toStringWithoutNext() {
                return (
                    new StringBuilder("range('")
                    .appendCodePoint(lhs)
                    .append("' - '")
                    .appendCodePoint(rhs)
                    .append("')")
                    .toString()
                );
            }
        };
    }

    /**
     * Representation of a case-insensitive character class range like {@code "a-k"}.
     */
    public static CharacterClass
    caseInsensitiveRange(final int lhs, final int rhs) {

        if (lhs == rhs) return CharacterClasses.literalCharacter(lhs);

        if (lhs > rhs) return CharacterClasses.FAIL;

        return new CharacterClass(Character.charCount(lhs), Character.charCount(rhs)) {

            @Override public boolean
            matches(int subject) {

                if (subject >= 'A' && subject <= 'Z' && subject + 32 >= lhs && subject + 32 <= rhs) return true;

                if (subject >= 'a' && subject <= 'z' && subject - 32 >= lhs && subject - 32 <= rhs) return true;

                return subject >= lhs && subject <= rhs;
            }

            @Override public String
            toStringWithoutNext() {
                return (
                    new StringBuilder("caseInsensitiveRange('")
                    .appendCodePoint(lhs)
                    .append("' - '")
                    .appendCodePoint(rhs)
                    .append("')")
                    .toString()
                );
            }
        };
    }

    /**
     * Representation of a case-insensitive character class range like {@code "a-k"}.
     */
    public static CharacterClass
    unicodeCaseInsensitiveRange(final int lhs, final int rhs) {

        if (lhs == rhs) return CharacterClasses.literalCharacter(lhs);

        if (lhs > rhs) return CharacterClasses.FAIL;

        return new CharacterClass() {

            @Override public boolean
            matches(int subject) {

                if (subject >= lhs && subject <= rhs) return true;

                subject = Character.toUpperCase(subject);
                if (subject >= lhs && subject <= rhs) return true;

                subject = Character.toLowerCase(subject);
                if (subject >= lhs && subject <= rhs) return true;

                return false;
            }

            @Override public String
            toStringWithoutNext() {
                return (
                    new StringBuilder("unicodeCaseInsensitiveRange('")
                    .appendCodePoint(lhs)
                    .append("' - '")
                    .appendCodePoint(rhs)
                    .append("')")
                    .toString()
                );
            }
        };
    }

    /**  An (ASCII) digit: [0-9] */
    public static CharacterClass
    digit(boolean unicode) {
        return CharacterClasses.characterClass(unicode ? Characters.IS_UNICODE_DIGIT : Characters.IS_POSIX_DIGIT);
    }

    /**
     * @return A character class that evaluates to the inversion of the <var>delegate</var>
     */
    public static CharacterClass
    negate(final CharacterClass operand, final String toString) {

        if (operand instanceof CharacterClasses.AnyCharacter) return CharacterClasses.FAIL;
        if (operand == CharacterClasses.FAIL)                 return new CharacterClasses.AnyCharacter();

        return new CharacterClass() {

            @Override public boolean
            matches(int subject) { return !operand.matches(subject); }

            @Override public String
            toStringWithoutNext() { return "negate(" + operand + ")"; }
        };
    }

    /**
     * A horizontal whitespace character:
     * <code>[ \t\xA0&#92;u1680&#92;u180e&#92;u2000-&#92;u200a&#92;u202f&#92;u205f&#92;u3000]</code>
     */
    public static CharacterClass
    horizontalWhitespace() {
        return CharacterClasses.characterClass(Characters.IS_HORIZONTAL_WHITESPACE);
    }

    /**  A whitespace character: [ \t\n\x0B\f\r] */
    public static CharacterClass
    whitespace(boolean unicode) {

        return CharacterClasses.characterClass(
            unicode ? Characters.IS_UNICODE_WHITE_SPACE : Characters.IS_POSIX_SPACE
        );
    }

    /**  A vertical whitespace character: [\n\x0B\f\r\x85/u2028/u2029] */
    public static CharacterClass
    verticalWhitespace() { return CharacterClasses.oneOfManyCodePoints("\r\n\u000B\f\u0085\u2028\u2029"); }

    /**  A word character: [a-zA-Z_0-9] */
    public static CharacterClass
    word(final boolean unicode) {
        return CharacterClasses.characterClass(unicode ? Characters.IS_UNICODE_WORD : Characters.IS_WORD);
    }

    /**
     * Implements {@code "."} (negated) iff !DOTALL and !UNIX_LINES
     */
    public static CharacterClass
    lineBreakCharacter() {

        return new MultivalentCharClass(LINE_BREAK_CHARACTERS) {

            @Override public boolean
            matches(int c) { return (c <= 0x0d && c >= 0x0a) || c == 0x85 || (c >= 0x2028 && c <= 0x2029); }

            @Override public String
            toStringWithoutNext() { return "lineBreakCharacter"; }
        };
    }
    private static final Set<Integer> LINE_BREAK_CHARACTERS = Sets.of(0x0a, 0x0b, 0x0d, 0x85, 0x2028, 0x2029);

    /**
     * Matches <em>any</em> character.
     */
    public static
    class AnyCharacter extends CharacterClass {
        @Override public boolean matches(int subject)  { return true;           }
        @Override public String  toStringWithoutNext() { return "anyCharacter"; }
    }

    /**
     * Optimized version of "{@code negate(literalCharacter('\n'))}".
     */
    static CharacterClass
    anyButNewline() {
        return new CharacterClass() {
            @Override public boolean   matches(int c)        { return c != '\n';           }
            @Override protected String toStringWithoutNext() { return "anyCharButNewline"; }
        };
    }

    /**
     * Optimized version of "{@code negate(lineBreakCharacter()}".
     */
    static CharacterClass
    anyButLineBreak() {

        return new CharacterClass() {

            @Override public boolean
            matches(int c) { return (c > 0x0d || c < 0x0a) && c != 0x85 && (c < 0x2028 || c > 0x2029); }

            @Override protected String
            toStringWithoutNext() { return "anyCharButLineBreak"; }
        };
    }

    static int
    min(Set<Integer> set) {
        int result = Integer.MAX_VALUE;
        for (int i : set) {
            if (i < result) result = i;
        }
        return result;
    }

    static int
    max(Set<Integer> set) {
        int result = Integer.MIN_VALUE;
        for (int i : set) {
            if (i > result) result = i;
        }
        return result;
    }

    static int
    minCharCountOf(Set<Integer> codePoints) {
        for (int cp : codePoints) {
            if (Character.charCount(cp) == 1) return 1;
        }
        return 2;
    }

    static int
    maxCharCountOf(Set<Integer> codePoints) {
        for (int cp : codePoints) {
            if (Character.charCount(cp) == 2) return 2;
        }
        return 1;
    }
}
