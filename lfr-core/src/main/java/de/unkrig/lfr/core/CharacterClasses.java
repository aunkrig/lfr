
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

import de.unkrig.commons.lang.Characters;
import de.unkrig.commons.lang.protocol.Predicate;

/**
 * Utility methods that create all kinds of {@link Union}s.
 */
final
class CharacterClasses {

	private CharacterClasses() {}

    /**
     * Representation of a literal character, like "a" or "\.".
     */
    public static
    class LiteralCharacter extends Union {

        /**
         * The literal character that this sequence represents.
         */
        final int c;

        LiteralCharacter(int c) { this.c = c; }

        @Override public boolean
        evaluate(int subject) { return subject == this.c; }

        @Override public Sequence
        concat(Sequence that) {
            that = (this.next = this.next.concat(that));

            if (that instanceof CharacterClasses.LiteralCharacter) {
                CharacterClasses.LiteralCharacter thatLiteralCharacter = (CharacterClasses.LiteralCharacter) that;

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

        @Override protected int lowerBoundWithoutCompanion() { return this.c;     }
        @Override protected int upperBoundWithoutCompanion() { return this.c + 1; }
        @Override protected int sizeBoundWithoutCompanion()  { return 1;          }

        @Override public String
        toStringWithoutCompanion() {
            return new StringBuilder(3).append('\'').appendCodePoint(this.c).append('\'').toString();
        }
    }

    /**
     * Delegates to an {@link Predicate Predicate&lt;Integer>}, where the subject is the character's code point.
     */
    public static Union
    characterClass(final Predicate<Integer> predicate) {

        return new Union() {
            @Override public boolean evaluate(int subject)      { return predicate.evaluate(subject);   }
            @Override public String  toStringWithoutCompanion() { return predicate + " . " + this.next; }
        };
    }

    /**
     * Checks whether the code point's block equals the given <var>block</var>.
     *
     * @see Character.UnicodeBlock#of(int)
     */
    public static Union
    inUnicodeBlock(final Character.UnicodeBlock block) {

        return new Union() {

            @Override public boolean
            evaluate(int subject) {
                return Character.UnicodeBlock.of(subject) == block || this.companion.matches(subject);
            }

            @Override public String
            toStringWithoutCompanion() { return "inUnicodeBlock(" + block + ")"; }
        };
    }

    /**
     * Checks whether the code point's "general category" equals the given <var>generalCategory</var>.
     *
     * @param generalCategory One of the "general category" constants in {@link Character}
     */
    public static Union
    inUnicodeGeneralCategory(final int generalCategory) {

        return new Union() {

            @Override public boolean
            evaluate(int subject) {
                return Character.getType(subject) == generalCategory || this.companion.matches(subject);
            }

            @Override public String
            toStringWithoutCompanion() { return "inUnicodeGeneralCategory(" + generalCategory + ")"; }
        };
    }

    /**
     * Checks whether a code point equals the given <var>codePoint</var>
     */
    public static Union
    literalCharacter(int codePoint) { return new LiteralCharacter(codePoint); }

    /**
     * Representation of a two-characters union, e.g. "[oO]".
     */
    public static Union
    oneOfTwo(final int c1, final int c2) { return new OneOfTwoCharacterClass(c1, c2); }

    public static final
    class OneOfTwoCharacterClass extends Union {

        private final int c1;
        private final int c2;

        private
        OneOfTwoCharacterClass(int c1, int c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override public boolean
        evaluate(int subject) { return subject == this.c1 || subject == this.c2; }

        @Override public int lowerBoundWithoutCompanion() { return Math.min(this.c1, this.c2);     }
        @Override public int upperBoundWithoutCompanion() { return Math.max(this.c1, this.c2) + 1; }
        @Override public int sizeBoundWithoutCompanion()  { return 2;                              }

        @Override public String
        toStringWithoutCompanion() {
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
     * Checks whether a code point is one of those in in <var>chars</var>.
     */
    public static Union
    oneOf(final String chars) { return new OneOfManyCharacterClass(chars); }

    public static final
    class OneOfManyCharacterClass extends Union {

        private final String chars;

        private
        OneOfManyCharacterClass(String chars) { this.chars = chars; }

        @Override public boolean
        evaluate(int subject) { return this.chars.indexOf(subject) != -1; }

        @Override protected int
        lowerBoundWithoutCompanion() {
            int result = Integer.MAX_VALUE;
            for (int i = this.chars.length() - 1; i >= 0; i--) {
                int c = this.chars.codePointAt(i);
                if (c < result) result = c;
            }
            return result;
        }

        @Override protected int
        upperBoundWithoutCompanion() {
            int result = 0;
            for (int i = this.chars.length() - 1; i >= 0; i--) {
                int c = this.chars.codePointAt(i);
                if (c > result) result = c;
            }
            return result;
        }

        @Override protected int
        sizeBoundWithoutCompanion() { return this.chars.length(); }

        @Override public String
        toStringWithoutCompanion() { return "oneOf(\"" + this.chars + "\")"; }
    }

    /**
     * Representation of an (ASCII-)case-insensitive literal character.
     */
    public static Union
    caseInsensitiveLiteralCharacter(final int c) {
        if (c >= 'A' && c <= 'Z') return CharacterClasses.oneOfTwo(c, c + 32);
        if (c >= 'a' && c <= 'z') return CharacterClasses.oneOfTwo(c, c - 32);
        return CharacterClasses.literalCharacter(c);
    }

    /**
     * Representation of a (UNICODE-)case-insensitive literal character.
     */
    public static Union
    unicodeCaseInsensitiveLiteralCharacter(final int c) {
        if (Character.isLowerCase(c)) return CharacterClasses.oneOfTwo(c, Character.toUpperCase(c));
        if (Character.isUpperCase(c)) return CharacterClasses.oneOfTwo(c, Character.toLowerCase(c));
        return CharacterClasses.literalCharacter(c);
    }

    /**
     * Representation of a character class intersection like {@code "\w&&[^abc]"}.
     */
    public static Union
    intersection(final CharacterClass lhs, final CharacterClass rhs) {

        return new Union() {

            @Override public boolean
            evaluate(int subject) { return lhs.matches(subject) && rhs.matches(subject); }

            @Override public int lowerBoundWithoutCompanion() { return Math.max(lhs.lowerBound(), rhs.lowerBound()); }
            @Override public int upperBoundWithoutCompanion() { return Math.min(lhs.upperBound(), rhs.upperBound()); }
            @Override public int sizeBoundWithoutCompanion()  { return Math.min(lhs.sizeBound(), rhs.sizeBound());   }

            @Override public String
            toStringWithoutCompanion() { return "intersection(" + lhs + ", " + rhs + ")"; }
        };
    }

    /**
     * Representation of a character class range like {@code "a-k"}.
     */
    public static CharacterClass
    range(final int lhs, final int rhs) {

        if (lhs == rhs) return CharacterClasses.literalCharacter(lhs);

        if (lhs > rhs) return new CharacterClasses.EmptyCharacterClass();

        return new Union() {

            @Override public boolean
            evaluate(int subject) { return (subject >= lhs && subject <= rhs); }

            @Override public int lowerBoundWithoutCompanion() { return lhs;           }
            @Override public int upperBoundWithoutCompanion() { return rhs + 1;       }
            @Override public int sizeBoundWithoutCompanion()  { return rhs - lhs + 1; }

            @Override public String
            toStringWithoutCompanion() {
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

    /**  An (ASCII) digit: [0-9] */
    public static Union
    digit(boolean unicode) {
        return CharacterClasses.characterClass(unicode ? Characters.IS_UNICODE_DIGIT : Characters.IS_DIGIT);
    }

    /**
     * @return A character class that evaluates to the inversion of the <var>delegate</var>
     */
    public static CharacterClass
    negate(final CharacterClass operand, final String toString) {

        if (operand instanceof CharacterClasses.AnyCharacter)        return new CharacterClasses.EmptyCharacterClass();
        if (operand instanceof CharacterClasses.EmptyCharacterClass) return new CharacterClasses.AnyCharacter();

        return new Union() {

            @Override public boolean
            evaluate(int subject) { return !operand.matches(subject); }

            @Override public String
            toStringWithoutCompanion() { return "negate(" + operand + ")"; }
        };
    }

    /**
     * A horizontal whitespace character:
     * <code>[ \t\xA0&#92;u1680&#92;u180e&#92;u2000-&#92;u200a&#92;u202f&#92;u205f&#92;u3000]</code>
     */
    public static Union
    horizontalWhitespace() {
        return CharacterClasses.characterClass(Characters.IS_HORIZONTAL_WHITESPACE);
    }

    /**  A whitespace character: [ \t\n\x0B\f\r] */
    public static Union
    whitespace(boolean unicode) {

        return (
            unicode
            ? CharacterClasses.characterClass(Characters.IS_UNICODE_WHITE_SPACE)
            : CharacterClasses.oneOf(" \t\n\u000B\f\r")
        );
    }

    /**  A vertical whitespace character: [\n\x0B\f\r\x85/u2028/u2029] */
    public static Union
    verticalWhitespace() { return CharacterClasses.oneOf("\r\n\u000B\f\u0085\u2028\u2029"); }

    /**  A word character: [a-zA-Z_0-9] */
    public static Union
    word(final boolean unicode) {
        return CharacterClasses.characterClass(unicode ? Characters.IS_UNICODE_WORD : Characters.IS_WORD);
    }

    /**
     * Implements {@code "."} (negated) iff !DOTALL and !UNIX_LINES
     */
    public static Union
    lineBreakCharacter() {

        return new Union() {

            @Override public int lowerBoundWithoutCompanion() { return 0x0a;   }
            @Override public int upperBoundWithoutCompanion() { return 0x202a; }
            @Override public int sizeBoundWithoutCompanion()  { return 7;      }

            @Override public boolean
            evaluate(int c) {
                return (c <= 0x0d && c >= 0x0a) || c == 0x85 || (c >= 0x2028 && c <= 0x2029);
            }

            @Override public String
            toStringWithoutCompanion() { return "lineBreakCharacter"; }
        };
    }

    /**
     * Matches <em>any</em> character.
     */
    public static
    class AnyCharacter extends CharacterClass {
        @Override public boolean        matches(int subject)       { return true;           }
        @Override public CharacterClass union(CharacterClass that) { return this;           }
        @Override public String         toStringWithoutNext()      { return "anyCharacter"; }
    }

    public static
    class EmptyCharacterClass extends CharacterClass {
        @Override public boolean        matches(int subject)       { return false;                 }
        @Override public int            lowerBound()               { return Integer.MAX_VALUE;     }
        @Override public int            upperBound()               { return 0;                     }
        @Override public int            sizeBound()                { return 0;                     }
        @Override public CharacterClass union(CharacterClass that) { return that;                  }
        @Override public Sequence       concat(Sequence that)      { return that;                  }
        @Override public String         toStringWithoutNext()      { return "emptyCharacterClass"; }
    }
}
