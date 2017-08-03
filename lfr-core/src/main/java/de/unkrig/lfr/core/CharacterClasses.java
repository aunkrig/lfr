
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
 * Classes and utility methods related to {@link CharacterClass}es.
 */
final
class CharacterClasses {

    private CharacterClasses() {}

    /**
     * Representation of a literal character, like "a" or "\.".
     */
    public static
    class LiteralCharacter extends CharacterClass {

        /**
         * The literal character that this sequence represents.
         */
        final int c;

        private final String toString;

        LiteralCharacter(int c, String toString) {
            this.c        = c;
            this.toString = toString;
        }

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
                String rhs = thatLiteralString.s;

                String ls = new StringBuilder(rhs.length() + 2).appendCodePoint(lhs).append(rhs).toString();

                return new Sequences.LiteralString(ls).concat(thatLiteralString.next);
            }

            return this;
        }

        @Override public String
        toString() { return this.toString + this.next; }
    }

    /**
     * Delegates to an {@link Predicate Predicate&lt;Integer>}, where the subject is the character's code point.
     */
    public static CharacterClass
    characterClass(final Predicate<Integer> predicate, final String toString) {

        return new CharacterClass() {
            @Override public boolean evaluate(int subject) { return predicate.evaluate(subject); }
            @Override public String  toString()            { return toString + this.next;   }
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
            evaluate(int subject) { return Character.UnicodeBlock.of(subject) == block; }
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
            evaluate(int subject) { return Character.getType(subject) == generalCategory; }
        };
    }

    /**
     * Checks whether a code point equals the given <var>codePoint</var>
     */
    public static CharacterClass
    literalCharacter(int codePoint, String toString) { return new LiteralCharacter(codePoint, toString); }

    /**
     * Representation of a two-characters union, e.g. "[oO]".
     */
    public static CharacterClass
    oneOf(final int c1, final int c2) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(int subject) { return subject == c1 || subject == c2; }

            @Override public String
            toString() { return "[" + c1 + c2 + ']' + this.next; }
        };
    }

    /**
     * Checks whether a code point is one of those in in <var>chars</var>.
     */
    public static CharacterClass
    oneOf(final String chars) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(int subject) { return chars.indexOf(subject) != -1; }

            @Override public String
            toString() { return '[' + chars + ']' + this.next; }
        };
    }

    /**
     * Representation of an (ASCII-)case-insensitive literal character.
     */
    public static CharacterClass
    caseInsensitiveLiteralCharacter(final int c) {
        if (c >= 'A' && c <= 'Z') return CharacterClasses.oneOf(c, c + 32);
        if (c >= 'a' && c <= 'z') return CharacterClasses.oneOf(c, c - 32);
        return CharacterClasses.literalCharacter(c, new String(new int[] { c }, 0, 1));
    }

    /**
     * Representation of a (UNICODE-)case-insensitive literal character.
     */
    public static CharacterClass
    unicodeCaseInsensitiveLiteralCharacter(final int c) {
        if (Character.isLowerCase(c)) return CharacterClasses.oneOf(c, Character.toUpperCase(c));
        if (Character.isUpperCase(c)) return CharacterClasses.oneOf(c, Character.toLowerCase(c));
        return CharacterClasses.literalCharacter(c, new String(new int[] { c }, 0, 1));
    }

    /**
     * Representation of a character class intersection like {@code "\w&&[^abc]"}.
     */
    public static CharacterClass
    intersection(final IntPredicate lhs, final IntPredicate rhs) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(int subject) { return lhs.evaluate(subject) && rhs.evaluate(subject); }

            @Override public String
            toString() { return lhs + "&&" + rhs + this.next; }
        };
    }

    /**
     * Representation of a character class union like {@code "ab"}.
     */
    public static CharacterClass
    union(final IntPredicate lhs, final IntPredicate rhs) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(int subject) { return lhs.evaluate(subject) || rhs.evaluate(subject); }

            @Override public String
            toString() { return lhs.toString() + rhs + this.next; }
        };
    }

    /**
     * Representation of a character class range like {@code "a-k"}.
     */
    public static CharacterClass
    range(final int lhs, final int rhs) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(int subject) { return subject >= lhs && subject <= rhs; }

            @Override public String
            toString() { return lhs + "-" + rhs + this.next; }
        };
    }

    /**  An (ASCII) digit: [0-9] */
    public static CharacterClass
    isDigit(boolean unicode) {

        if (unicode) return CharacterClasses.characterClass(Characters.IS_UNICODE_DIGIT, "\\d");

        return new CharacterClass() {
            @Override public boolean evaluate(int subject) { return subject >= '0' && subject <= '9'; }
            @Override public String  toString()            { return "\\d" + this.next;           }
        };
    }

    /**
     * @return A character class that evaluates to the inversion of the <var>delegate</var>
     */
    public static CharacterClass
    negate(final CharacterClass delegate, final String toString) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(int subject) { return !delegate.evaluate(subject); }

            @Override public String
            toString() { return toString + this.next; }
        };
    }

    /**
     * A horizontal whitespace character:
     * <code>[ \t\xA0&#92;u1680&#92;u180e&#92;u2000-&#92;u200a&#92;u202f&#92;u205f&#92;u3000]</code>
     */
    public static CharacterClass
    isHorizontalWhitespace() {

        return new CharacterClass() {

            @Override public boolean
            evaluate(int subject) {
                return (
                    "\t\u00A0\u1680\u180e\u202f\u205f\u3000".indexOf(subject) != -1
                    || (subject >= '\u2000' && subject <= '\u200a')
                );
            }

            @Override public String
            toString() { return "\\h" + this.next; }
        };
    }

    /**  A whitespace character: [ \t\n\x0B\f\r] */
    public static CharacterClass
    isWhitespace(boolean unicode) {

        if (unicode) return CharacterClasses.characterClass(Characters.IS_UNICODE_WHITE_SPACE, "\\s");

        return CharacterClasses.oneOf(Pattern.WHITESPACE_CHARACTERS);
    }

    /**  A vertical whitespace character: [\n\x0B\f\r\x85/u2028/u2029] */
    public static CharacterClass
    isVerticalWhitespace() { return CharacterClasses.oneOf(Pattern.VERTICAL_WHITESPACE_CHARACTERS); }

    /**  A word character: [a-zA-Z_0-9] */
    public static CharacterClass
    isWord(final boolean unicode) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(int subject) {

                if (unicode) {
                    int type = Character.getType(subject);
                    return (
                        type == Character.UPPERCASE_LETTER
                        || type == Character.LOWERCASE_LETTER
                        || type == Character.TITLECASE_LETTER
                        || type == Character.MODIFIER_LETTER
                        || type == Character.OTHER_LETTER
                        || type == Character.LETTER_NUMBER
                        || type == Character.NON_SPACING_MARK
                        || type == Character.ENCLOSING_MARK
                        || type == Character.COMBINING_SPACING_MARK
                        || Character.isDigit(subject)
                        || type == Character.CONNECTOR_PUNCTUATION
                        // || join_control  -- What is that??
                    );
                }

                return Sequences.isWordCharacter(subject);
            }

            @Override public String
            toString() { return "\\w";  }
        };
    }

    /**
     * Implements {@code "."} (negated) iff !DOTALL and !UNIX_LINES
     */
    public static CharacterClass
    lineBreakCharacter() {

        return new CharacterClass() {

            @Override public boolean
            evaluate(int subject) {
                return (
                    (subject <= 0x0d && subject >= 0x0a)
                    || subject == 0x85
                    || (subject >= 0x2028 && subject <= 0x2029)
                );
            }
        };
    }

    /**
     * Matches <em>any</em> character.
     */
    public static
    class AnyCharacter extends CharacterClass {
        @Override public boolean evaluate(int subject) { return true; }
        @Override public String  toString()            { return ".";  }
    }
}
