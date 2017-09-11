
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
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.unkrig.commons.lang.Characters;
import de.unkrig.commons.lang.PrettyPrinter;
import de.unkrig.commons.lang.protocol.Predicate;

/**
 * Utility methods that create all kinds of {@link CharacterClass}s.
 */
final
class CharacterClasses {

    /**
     * Always causes a match to fail.
     */
    public static final CharacterClass
    FAIL = new CharacterClass() {

        @Override public boolean   matches(int c)                { return false;  }
        @Override public boolean   find(MatcherImpl matcherImpl) { return false;  }
        @Override public Sequence  concat(Sequence that)         { return this;   }
        @Override public String    toString()                    { return "fail"; }
        @Override protected String toStringWithoutNext()         { return "???";  }
    };

    private CharacterClasses() {}

    /**
     * Representation of a non-supplementary literal character, i.e. that fits into one {@code char}, like {@code "a"}
     * or {@code "\."}.
     */
    public static
    class LiteralChar extends CharacterClass {

        /**
         * The literal character that this sequence represents.
         */
        final char c;

        LiteralChar(char c) { this.c = c; }

        @Override public boolean
        matches(int subject) { return subject == this.c; }

        /**
         * Optimized version of {@link #find(MatcherImpl)}
         */
        @Override public boolean
        find(MatcherImpl matcher) {

            int o = matcher.offset;

            FIND:
            while (o < matcher.regionEnd) {

                // Find the next occurrence of the literal char.
                for (;; o++) {
                    if (o >= matcher.regionEnd) break FIND;
                    if (matcher.subject.charAt(o) == this.c) break;
                }

                // See if the rest of the pattern matches.
                matcher.offset = o + 1;
                if (this.next.matches(matcher)) {
                    matcher.groups[0] = o;
                    matcher.groups[1] = matcher.offset;
                    return true;
                }

                // Rest of pattern didn't match; continue right behind the character match.
                o++;
            }

            matcher.hitEnd = true;
            return false;
        }

        @Override public Sequence
        concat(Sequence that) {
            that = (this.next = this.next.concat(that));

            if (that instanceof CharacterClasses.LiteralChar) {
                CharacterClasses.LiteralChar thatLiteralCharacter = (CharacterClasses.LiteralChar) that;

                int lhs = this.c;
                int rhs = thatLiteralCharacter.c;

                String ls = new StringBuilder(4).appendCodePoint(lhs).appendCodePoint(rhs).toString();

                return new Sequences.LiteralString(ls).concat(thatLiteralCharacter.next);
            }

            if (that instanceof Sequences.LiteralString) {
                Sequences.LiteralString thatLiteralString = (Sequences.LiteralString) that;

                int    lhs = this.c;
                String rhs = thatLiteralString.get();

                String ls = new StringBuilder(rhs.length() + 2).appendCodePoint(lhs).append(rhs).toString();

                return new Sequences.LiteralString(ls).concat(thatLiteralString.next);
            }

            return this;
        }

        @Override public int lowerBound() { return this.c;     }
        @Override public int upperBound() { return this.c + 1; }
        @Override public int sizeBound()  { return 1;          }

        @Override public String
        toStringWithoutNext() { return "'" + this.c + '\''; }
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

        return new CharacterClass() {

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
    oneOfTwo(int c1, int c2) {
        return c1 == c2 ? CharacterClasses.literalCharacter(c1) : new OneOfTwoCharacterClass(c1, c2);
    }

    public static final
    class OneOfTwoCharacterClass extends CharacterClass {

        private final int c1, c2;

        private
        OneOfTwoCharacterClass(int c1, int c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override public boolean
        matches(int subject) { return subject == this.c1 || subject == this.c2; }

        @Override public int lowerBound() { return Math.min(this.c1, this.c2);     }
        @Override public int upperBound() { return Math.max(this.c1, this.c2) + 1; }
        @Override public int sizeBound()  { return 2;                              }

        @Override public String
        toStringWithoutNext() {
            return (
                new StringBuilder("oneOfTwo('")
                .appendCodePoint(this.c1)
                .append("', '")
                .appendCodePoint(this.c2)
                .append("')")
                .toString()
            );
        }
    }

    /**
     * Representation of a three-characters union, e.g. "[abc]".
     */
    public static CharacterClass
    oneOfThree(final int c1, final int c2, final int c3) {

        return (
            c1 == c2 ? CharacterClasses.oneOfTwo(c1, c3) :
            c1 == c3 ? CharacterClasses.oneOfTwo(c1, c2) :
            c2 == c3 ? CharacterClasses.oneOfTwo(c1, c2) :
            new OneOfThreeCharacterClass(c1, c2, c3)
        );
    }

    static
    class OneOfThreeCharacterClass extends CharacterClass {

        private final int cp1, cp2, cp3;

        OneOfThreeCharacterClass(int cp1, int cp2, int cp3) {
            this.cp1 = cp1;
            this.cp2 = cp2;
            this.cp3 = cp3;
        }

        @Override public boolean
        matches(int subject) { return subject == this.cp1 || subject == this.cp2 || subject == this.cp3; }

        @Override public int
        lowerBound() { return CharacterClasses.min(this.cp1, this.cp2, this.cp3); }

        @Override public int
        upperBound() { return CharacterClasses.max(this.cp1, this.cp2, this.cp3) + 1; }

        @Override public int
        sizeBound() { return 3; }

        @Override public String
        toStringWithoutNext() {
            return (
                "oneOfThree("
                + PrettyPrinter.codePointToString(this.cp1)
                + ", "
                + PrettyPrinter.codePointToString(this.cp2)
                + ", "
                + PrettyPrinter.codePointToString(this.cp3)
                + ")"
            );
        }
    }

    /**
     * Checks whether a code point is one of those in in <var>chars</var>.
     */
    public static CharacterClass
    oneOf(final String chars) {

        if (chars.isEmpty()) return CharacterClasses.FAIL;

        SortedSet<Integer> s = new TreeSet<Integer>();
        for (int offset = 0; offset < chars.length();) {
            int cp = chars.codePointAt(offset);
            s.add(cp);
            offset += Character.charCount(cp);
        }

        return CharacterClasses.oneOf(s);
    }

    /**
     * Checks whether a code point is one of those in in <var>chars</var>.
     */
    public static CharacterClass
    oneOf(Collection<Integer> chars) {
        return CharacterClasses.oneOf(new HashSet<Integer>(chars));
    }

    /**
     * Checks whether a code point is one of those in in <var>chars</var>.
     */
    public static CharacterClass
    oneOf(final Set<Integer> chars) {

        if (chars.isEmpty()) return CharacterClasses.FAIL;

        Iterator<Integer> it = chars.iterator();
        switch (chars.size()) {
        case 1: return literalCharacter(chars.iterator().next());
        case 2: return new OneOfTwoCharacterClass(it.next(), it.next());
        case 3: return new OneOfThreeCharacterClass(it.next(), it.next(), it.next());
        }

        return new CharacterClass() {

            @Override public boolean
            matches(int c) { return chars.contains(c); }

            @Override protected String
            toStringWithoutNext() {
                return (
                    "oneOf("
                    + PrettyPrinter.toString(new String(CharacterClasses.unwrap(chars), 0, chars.size()))
                    + ")"
                );
            }
        };
    }

    public static final
    class OneOfManyCharacterClass extends CharacterClass {

        private final String chars;

        private
        OneOfManyCharacterClass(String chars) { this.chars = chars; }

        @Override public boolean
        matches(int subject) { return this.chars.indexOf(subject) != -1; }

        @Override public int
        lowerBound() {
            int result = Integer.MAX_VALUE;
            for (int i = this.chars.length() - 1; i >= 0; i--) {
                int c = this.chars.codePointAt(i);
                if (c < result) result = c;
            }
            return result;
        }

        @Override public int
        upperBound() {
            int result = 0;
            for (int i = this.chars.length() - 1; i >= 0; i--) {
                int c = this.chars.codePointAt(i);
                if (c > result) result = c;
            }
            return result;
        }

        @Override public int
        sizeBound() { return this.chars.length(); }

        @Override public String
        toStringWithoutNext() { return "oneOf(\"" + this.chars + "\")"; }
    }

    /**
     * Representation of an (ASCII-)case-insensitive literal character.
     */
    public static CharacterClass
    caseInsensitiveLiteralCharacter(final int c) {
        if (c >= 'A' && c <= 'Z') return CharacterClasses.oneOfTwo(c, c + 32);
        if (c >= 'a' && c <= 'z') return CharacterClasses.oneOfTwo(c, c - 32);
        return CharacterClasses.literalCharacter(c);
    }

    /**
     * Representation of a (UNICODE-)case-insensitive literal character.
     */
    public static CharacterClass
    unicodeCaseInsensitiveLiteralCharacter(final int cp) {

        String s = Characters.caseInsensitivelyEqualCharacters(cp);

        if (s == null) return CharacterClasses.literalCharacter(cp);

        return CharacterClasses.oneOf(s);
    }

    /**
     * @return The (highly optimized) union of the <var>lhs</var> and the <var>rhs</var> character classes
     */
    public static CharacterClass
    union(final CharacterClass lhs, final CharacterClass rhs) {

        if (lhs instanceof EmptyCharacterClass) return rhs;

        if (rhs instanceof EmptyCharacterClass) return lhs;

        if (lhs instanceof LiteralChar && rhs instanceof LiteralChar) {
            return CharacterClasses.oneOfTwo(((LiteralChar) lhs).c, ((LiteralChar) rhs).c);
        }

        if (lhs instanceof LiteralChar && rhs instanceof OneOfTwoCharacterClass) {
            LiteralChar            lc  = (LiteralChar)       lhs;
            OneOfTwoCharacterClass oot = (OneOfTwoCharacterClass) rhs;
            return CharacterClasses.oneOfThree(lc.c, oot.c1, oot.c2);
        }

        if (lhs instanceof OneOfTwoCharacterClass && rhs instanceof LiteralChar) {
            OneOfTwoCharacterClass oot = (OneOfTwoCharacterClass) lhs;
            LiteralChar            lc  = (LiteralChar)       rhs;
            return CharacterClasses.oneOfThree(oot.c1, oot.c2, lc.c);
        }

        return new CharacterClass() {
            @Override public boolean   matches(int c)        { return lhs.matches(c) || rhs.matches(c);             }
            @Override public int       lowerBound()          { return Math.min(lhs.lowerBound(), rhs.lowerBound()); }
            @Override public int       upperBound()          { return Math.max(lhs.upperBound(), rhs.upperBound()); }
            @Override public int       sizeBound()           { return lhs.sizeBound() + rhs.sizeBound();            }
            @Override protected String toStringWithoutNext() { return "union(" + lhs + ", " + rhs + ')';            }
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
                return CharacterClasses.oneOfTwo(it.next(), it.next());
            }

        case 3:
            {
                Iterator<Integer> it = integerSet.iterator();
                return CharacterClasses.oneOfThree(it.next(), it.next(), it.next());
            }
        }

        if (maxValue < 256) {

            // Optimize into a "BitSet" if the maxValue is small enough.
            final BitSet bitSet = new BitSet(maxValue + 1);
            for (int c : integerSet) bitSet.set(c);

            return new CharacterClass() {


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

            return new CharacterClass() {

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

        return new CharacterClass() {

            @Override public boolean
            matches(int subject) { return lhs.matches(subject) && rhs.matches(subject); }

            @Override public int lowerBound() { return Math.max(lhs.lowerBound(), rhs.lowerBound()); }
            @Override public int upperBound() { return Math.min(lhs.upperBound(), rhs.upperBound()); }
            @Override public int sizeBound()  { return Math.min(lhs.sizeBound(), rhs.sizeBound());   }

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

        if (lhs > rhs) return new CharacterClasses.EmptyCharacterClass();

        return new CharacterClass() {

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

        if (lhs > rhs) return new CharacterClasses.EmptyCharacterClass();

        return new CharacterClass() {

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

        if (operand instanceof CharacterClasses.AnyCharacter)        return new CharacterClasses.EmptyCharacterClass();
        if (operand instanceof CharacterClasses.EmptyCharacterClass) return new CharacterClasses.AnyCharacter();

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

        return (
            unicode
            ? CharacterClasses.characterClass(Characters.IS_UNICODE_WHITE_SPACE)
            : CharacterClasses.oneOf(" \t\n\u000B\f\r")
        );
    }

    /**  A vertical whitespace character: [\n\x0B\f\r\x85/u2028/u2029] */
    public static CharacterClass
    verticalWhitespace() { return CharacterClasses.oneOf("\r\n\u000B\f\u0085\u2028\u2029"); }

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

        return new CharacterClass() {

            @Override public int lowerBound() { return 0x0a;   }
            @Override public int upperBound() { return 0x202a; }
            @Override public int sizeBound()  { return 7;      }

            @Override public boolean
            matches(int c) {
                return (c <= 0x0d && c >= 0x0a) || c == 0x85 || (c >= 0x2028 && c <= 0x2029);
            }

            @Override public String
            toStringWithoutNext() { return "lineBreakCharacter"; }
        };
    }

    /**
     * Matches <em>any</em> character.
     */
    public static
    class AnyCharacter extends CharacterClass {
        @Override public boolean matches(int subject)  { return true;           }
        @Override public String  toStringWithoutNext() { return "anyCharacter"; }
    }

    public static
    class EmptyCharacterClass extends CharacterClass {
        @Override public boolean  matches(int subject)  { return false;                 }
        @Override public int      lowerBound()          { return Integer.MAX_VALUE;     }
        @Override public int      upperBound()          { return 0;                     }
        @Override public int      sizeBound()           { return 0;                     }
        @Override public Sequence concat(Sequence that) { return that;                  }
        @Override public String   toStringWithoutNext() { return "emptyCharacterClass"; }
    }

    private static int
    min(int i1, int i2, int i3) {
        if (i1 < i2) return i1 < i3 ? i1 : i3;
        return i2 < i3 ? i2 : i3;
    }

    private static int
    max(int i1, int i2, int i3) {
        if (i1 > i2) return i1 > i3 ? i1 : i3;
        return i2 > i3 ? i2 : i3;
    }

    /**
     * @return The elements of the given {@link Integer} collection
     */
    private static int[]
    unwrap(Collection<Integer> c) {
        int[] result = new int[c.size()];
        int   idx    = 0;
        for (int i : c) result[idx++] = i;
        return result;
    }
}
