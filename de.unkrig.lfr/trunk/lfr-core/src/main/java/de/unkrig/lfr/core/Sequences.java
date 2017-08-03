
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

import java.util.Arrays;

import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.StringUtil.IndexOf;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Classes and methods related to {@link Sequence}s.
 */
final
class Sequences {

    private Sequences() {}

    /**
     * Matches the empty string, and is the last element of any {@link LinkedAbstractSequence} chain.
     */
    public static final Sequence
    TERMINAL = new AbstractSequence() {

        @Override public int
        matches(MatcherImpl matcher, int offset) { return offset; }

        @Override public Sequence
        concat(Sequence that) { return that; }

        @Override public Sequence
        reverse() { return this; }

        @Override public String
        toString() { return "[TERM]"; }
    };

    /**
     * Implements a "greedy" quantifier for an operand.
     */
    public static Sequence
    greedyQuantifierSequence(final Sequence operand, final int min, final int max) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // The operand MUST match (min) times;
                for (int i = 0; i < min; i++) {
                    offset = operand.matches(matcher, offset);
                    if (offset == -1) return -1;
                }

                // Now try to match the operand (max-min) more times.
                int     limit       = max - min;
                int[]   savedOffset = new int[Math.min(limit, 20)];

                for (int i = 0; i < limit; i++) {

                    if (i >= savedOffset.length) savedOffset = Arrays.copyOf(savedOffset, 2 * i);

                    savedOffset[i] = offset;

                    offset = operand.matches(matcher, offset);

                    if (offset == -1) {
                        for (; i >= 0; i--) {
                            offset = this.next.matches(matcher, savedOffset[i]);
                            if (offset != -1) return offset;
                        }
                        return -1;
                    }
                }

                return this.next.matches(matcher, offset);
            }

            @Override public Sequence
            concat(Sequence that) {
                that = (this.next = this.next.concat(that));

                // Optimize for operand ".", followed by a literal string.
                if (
                    operand instanceof CharacterClasses.AnyCharacter
                    && ((LinkedAbstractSequence) operand).next == Sequences.TERMINAL
                ) {

                    if (that instanceof LiteralString) {
                        final LiteralString ls = (LiteralString) that;

                        return greedyQuantifierAnyCharLiteralString(ls.s, min, max).concat(ls.next);
                    }
                }

                // Optimize for bare character class operands, e.g. "[...]".
                if (operand instanceof CharacterClass) {
                    CharacterClass cc = (CharacterClass) operand;
                    if (cc.next == Sequences.TERMINAL) {
                        return Sequences.greedyQuantifierCharacterClass(cc, min, max).concat(this.next);
                    }
                }

                return this;
            }

            @Override public String
            toString() { return operand + "{" + min + "," + max + "}" + this.next; }
        };
    }

    /**
     * Implements a "reluctant" quantifier for an operand.
     */
    public static Sequence
    reluctantQuantifierSequence(final Sequence operand, final int min, final int max) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // The operand MUST match min times;
                int i;
                for (i = 0; i < min; i++) {
                    offset = operand.matches(matcher, offset);
                    if (offset == -1) return -1;
                }

                // Now try to match the operand (max-min) more times.
                for (;; i++) {

                    int offset2 = this.next.matches(matcher, offset);
                    if (offset2 != -1) return offset2;

                    if (i >= max) return -1;

                    offset = operand.matches(matcher, offset);
                    if (offset == -1) return -1;
                }
            }

            @Override public Sequence
            concat(Sequence that) {
                that = (this.next = this.next.concat(that));

                // Is the reluctant quantifier's operand "any char"?
                if (!(operand instanceof CharacterClasses.AnyCharacter)) return this;
                if (((LinkedAbstractSequence) operand).next != Sequences.TERMINAL) return this;

                // Is the successor a literal string?
                Sequence e1 = this.next;
                if (!(e1 instanceof LiteralString)) return this;
                final LiteralString ls = (LiteralString) e1;

                final String infix       = ls.s;
                final int    infixLength = infix.length();

                return new LinkedAbstractSequence() {

                    final IndexOf indexOf = StringUtil.newIndexOf(infix);

                    @Override public int
                    matches(MatcherImpl matcher, int offset) {

                        int limit = matcher.regionEnd;

                        if (limit - offset > max) limit = offset + max; // Beware of overflow!

                        offset += min;

                        while (offset <= limit) {

                            // Find next match of the infix withing the subject string.
                            offset = this.indexOf.indexOf(matcher.subject, offset, limit);
                            if (offset == -1) break;

                            // See if the successor matches the rest of the subject.
                            int result = this.next.matches(matcher, offset + infixLength);
                            if (result != -1) return result;

                            // Successor didn't match, continue with next character position.
                            offset++;
                        }

                        matcher.hitEnd = true;
                        return -1;
                    }
                }.concat(ls.next);
            }

            @Override public String
            toString() { return operand + "{" + min + "," + max + "}?" + this.next; }
        };
    }

    /**
     * Implements a "possessive" quantifier for an operand.
     */
    public static Sequence
    possessiveQuantifierSequence(final Sequence operand, final int min, final int max) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // The operand MUST match (min) times;
                for (int i = 0; i < min; i++) {
                    offset = operand.matches(matcher, offset);
                    if (offset == -1) return -1;
                }

                // Now try to match the operand (max-min) more times.
                int limit = max - min;

                for (int i = 0; i < limit; i++) {

                    int offset2 = operand.matches(matcher, offset);

                    if (offset2 == -1) return this.next.matches(matcher, offset);

                    offset = offset2;
                }

                return this.next.matches(matcher, offset);
            }

            @Override public Sequence
            concat(Sequence that) {
                that = (this.next = this.next.concat(that));

                // Optimize for "." operand.
                if (
                    operand instanceof CharacterClasses.AnyCharacter
                    && ((LinkedAbstractSequence) operand).next == Sequences.TERMINAL
                ) {

                    // Replace the possessive quantifier element.
                    return new LinkedAbstractSequence() {

                        @Override public int
                        matches(MatcherImpl matcher, int offset) {

                            if (max > matcher.regionEnd - offset) {
                                offset = matcher.regionEnd;
                            } else {
                                offset += max;
                            }

                            return this.next.matches(matcher, offset);
                        }
                    }.concat(this.next);
                }

                return this;
            }

            @Override public String
            toString() { return operand + "{" + min + "," + max + "}+" + this.next; }
        };
    }

    /**
     * Representation of a sequence of literal, case-sensitive characters.
     */
    public static
    class LiteralString extends LinkedAbstractSequence {

        /**
         * The literal string that this sequence represents.
         */
        String s;

        @Nullable private StringUtil.IndexOf indexOf;

        LiteralString(String s) { this.s = s; }

        @Override public int
        matches(MatcherImpl matcher, int offset) {
            offset = matcher.peekRead(offset, this.s);
            if (offset == -1) return -1;
            return this.next.matches(matcher, offset);
        }

        @Override public boolean
        find(MatcherImpl matcher, int start) {

            IndexOf io = this.indexOf;
            if (io == null) io = (this.indexOf = StringUtil.newIndexOf(this.s));

            while (start < matcher.regionEnd) {

                // Find the next occurrence of the literal string.
                int offset = io.indexOf(matcher.subject, start, matcher.regionEnd);
                if (offset == -1) break;

                // See if the rest of the pattern matches.
                int result = LiteralString.this.next.matches(matcher, offset + this.s.length());
                if (result != -1) {
                    matcher.groups[0] = offset;
                    matcher.groups[1] = result;
                    return true;
                }

                // Rest of pattern didn't match; continue at the second character of the literal string match.
                start = offset + 1;
            }

            matcher.hitEnd = true;
            return false;
        }

        @Override public Sequence
        reverse() {

            this.s = new StringBuilder(this.s).reverse().toString();

            return super.reverse();
        }

        @Override public Sequence
        concat(Sequence that) {
            that = (this.next = this.next.concat(that));

            if (that instanceof CharacterClasses.LiteralCharacter) {
                CharacterClasses.LiteralCharacter thatLiteralCharacter = (CharacterClasses.LiteralCharacter) that;

                String lhs = this.s;
                int    rhs = thatLiteralCharacter.c;

                return (
                    new Sequences.LiteralString(new StringBuilder(lhs).appendCodePoint(rhs).toString())
                    .concat(thatLiteralCharacter.next)
                );
            }

            if (that instanceof Sequences.LiteralString) {
                Sequences.LiteralString thatLiteralString = (Sequences.LiteralString) that;

                String lhs = this.s;
                String rhs = thatLiteralString.s;

                return new Sequences.LiteralString(lhs + rhs).concat(thatLiteralString.next);
            }

            return this;
        }

        @Override public String
        toString() { return this.s + this.next; }
    }

    /**
     * A sequence that simply delegates to its successor.
     */
    public static final
    class EmptyStringSequence extends LinkedAbstractSequence {

        @Override public int
        matches(MatcherImpl matcher, int offset) { return this.next.matches(matcher, offset); }

        @Override public Sequence
        concat(Sequence that) { return that; }

        @Override public String
        toString() { return ""; }
    }

    /**
     * Creates and returns {@link Sequence} that returns the first match of one <var>alternatives</var> plus
     * <em>this</em> sequence's successor.
     * For example, {@code "a(b|bb)c"} will match both {@code "abc"} and {@code "abbc"}. (In the second case, matching
     * {@code "abc"} fails, but matching {@code "abbc"} eventually succeeds.)
     *
     * @see #independentNonCapturingGroup(Sequence[])
     */
    public static Sequence
    alternatives(final Sequence[] alternatives) {

        if (alternatives.length == 0) return new EmptyStringSequence();

        if (alternatives.length == 1) return alternatives[0];

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                for (int i = 0; i < alternatives.length; i++) {
                    int result = alternatives[i].matches(matcher, offset);
                    if (result != -1) {
                        result = this.next.matches(matcher, result);
                        if (result != -1) return result;
                    }
                }

                return -1;
            }

            @Override public Sequence
            reverse() {

                for (int i = 0; i < alternatives.length; i++) alternatives[i] = alternatives[i].reverse();

                return this;
            }

            @Override public String
            toString() { return Sequences.join(alternatives, '|'); }
        };
    }

    /**
     * Creates and returns a {@link Sequence} that matches the <var>alternatives</var>, and, after the first matching
     * alternative, matches its successor.
     * For example, {@code "a(?>b|bb)c"} will match {@code "abc"} but <em>not</em> {@code "abbc"}. (In the second case,
     * matching {@code "ab"} succeeds, {@code "abb"} is never tried.)
     *
     * @see #alternatives(Sequence[])
     */
    public static Sequence
    independentNonCapturingGroup(final Sequence[] alternatives) {

        if (alternatives.length == 0) return new EmptyStringSequence();

        if (alternatives.length == 1) return alternatives[0];

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                for (Sequence a : alternatives) {
                    int result = a.matches(matcher, offset);
                    if (result != -1) return this.next.matches(matcher, result);
                }

                return -1;
            }

            @Override public String
            toString() { return "(?>" + Sequences.join(alternatives, '|') + ')' + this.next; }
        };
    }

    /**
     * Implements {@code "("}.
     */
    public static Sequence
    capturingGroupStart(final int groupNumber) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int[] newGroups = Arrays.copyOf(matcher.groups, matcher.groups.length);
                newGroups[2 * groupNumber] = offset;

                final int[] savedGroups = matcher.groups;
                matcher.groups = newGroups;

                int result = this.next.matches(matcher, offset);

                if (result == -1) {
                    matcher.groups = savedGroups;
                    return -1;
                }

                // Verify that the successor chain contained an "end" for the same capturing group.
                assert matcher.groups[2 * groupNumber + 1] != -1;

                return result;
            }

            @Override public Sequence
            reverse() { return this.next.reverse().concat(Sequences.capturingGroupEnd(groupNumber)); }

            @Override public String
            toString() { return "(" + this.next; }
        };
    }

    /**
     * Implements {@code ")"}.
     */
    public static Sequence
    capturingGroupEnd(final int groupNumber) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                matcher.groups[2 * groupNumber + 1] = offset;

                return this.next.matches(matcher, offset);
            }

            @Override public Sequence
            reverse() { return Sequences.capturingGroupStart(groupNumber).concat(this.next.reverse()); }

            @Override public String
            toString() { return ")" + this.next; }
        };
    }

    /**
     * Creates and returns a sequence that matches the <var>subsequence</var> and then its successor. Iff the
     * <var>subsequence</var> matches, the beginning and the end of the match are recorded in the matcher.
     *
     * @param groupNumber 1...<var>groupCount</var>
     * @return            Iff the <var>subsequence</var> and the successor match, the offset of the first character
     *                    after the match; otherwise {@code 0}
     */
    static Sequence
    capturingGroup(final int groupNumber, final Sequence subsequence) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int start = offset;
                int end   = subsequence.matches(matcher, start);

                if (end == -1) return -1;

                // Copy "this.groups" and store group start and end.
                int[] gs = (matcher.groups = Arrays.copyOf(matcher.groups, matcher.groups.length));

                // Record the group start and end in the matcher.
                gs[2 * groupNumber]     = start;
                gs[2 * groupNumber + 1] = end;

                // Match the rest of the sequence.
                return this.next.matches(matcher, end);
            }

            @Override public String
            toString() { return "(" + subsequence + ")"; }
        };
    }

    /**
     * Representation of a capturing group backreference, e.g. {@code \3} .
     *
     * @param groupNumber 1...<var>groupCount</var>
     */
    static Sequence
    capturingGroupBackReference(final int groupNumber) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int[] gs    = matcher.groups;
                int   start = gs[2 * groupNumber];
                int   end   = gs[2 * groupNumber + 1];

                // If the referenced group didn't match, then neither does this back reference.
                if (start == -1) return -1;

                offset = matcher.peekRead(offset, matcher.subject.subSequence(start, end));
                if (offset == -1) return -1;

                return this.next.matches(matcher, offset);
            }

            @Override public String
            toString() { return "\\" + groupNumber; }
        };
    }

    /**
     * Implements {@code "^"} with !MULTILINE, and {@code "\A"}.
     */
    public static Sequence
    beginningOfInput() {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {
                return offset != matcher.anchoringRegionStart ? -1 : this.next.matches(matcher, offset);
            }

            // Override "AbstractSequence.find()" such that we give the match only one shot.
            @Override public boolean
            find(MatcherImpl matcher, int start) {

                int newOffset = this.matches(matcher, start);
                if (newOffset != -1) {
                    matcher.groups[0] = start;
                    matcher.groups[1] = newOffset;
                    return true;
                }

                return false;
            }

            @Override public Sequence
            reverse() {
                Sequence result = this.next.reverse();
                result.concat(Sequences.endOfInput());
                return result;
            }

            @Override public String
            toString() { return "^" + this.next; }
        };
    }

    /**
     * Implements {@code "^"} with MULTILINE.
     */
    public static Sequence
    beginningOfLine(final String lineBreakCharacters) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {
                if (
                    offset == matcher.anchoringRegionStart
                    || lineBreakCharacters.indexOf(matcher.charAt(offset - 1)) != -1
                ) return this.next.matches(matcher, offset);

                if (offset == matcher.anchoringRegionEnd) matcher.hitEnd = true;

                return -1;
            }

            @Override public Sequence
            reverse() {
                Sequence result = this.next.reverse();
                result.concat(Sequences.endOfLine(lineBreakCharacters));
                return result;
            }

            @Override public String
            toString() { return "^" + this.next; }
        };
    }

    /**
     * Implements {@code "\Z"}.
     */
    public static Sequence
    endOfInputButFinalTerminator() {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset >= matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher, offset);
                }

                char c = matcher.charAt(offset);
                if (!(
                    (c <= 0x0d && c >= 0x0a)
                    || c == 0x85
                    || (c >= 0x2028 && c <= 0x2029)
                )) return -1;

                if (offset == matcher.anchoringRegionEnd - 1) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher, offset);
                }

                if (c == '\r' && matcher.charAt(offset + 1) == '\n') {
                    if (offset == matcher.anchoringRegionEnd - 2) {
                        matcher.hitEnd = true;
                        return this.next.matches(matcher, offset);
                    }
                }

                return -1;
            }

            @Override public Sequence
            reverse() {
                Sequence result = this.next.reverse();
                result.concat(Sequences.beginningOfInput());
                return result;
            }

            @Override public String
            toString() { return "\\Z" + this.next; }
        };
    }

    /**
     * Implements {@code "$"} with !MULTILINE, and {@code "\z"}.
     */
    public static Sequence
    endOfInput() {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset < matcher.anchoringRegionEnd) return -1;

                matcher.hitEnd     = true;
                matcher.requireEnd = true;

                return this.next.matches(matcher, offset);
            }

            @Override public Sequence
            reverse() {
                Sequence result = this.next.reverse();
                result.concat(Sequences.beginningOfInput());
                return result;
            }

            @Override public String
            toString() { return "\\z" + this.next; }
        };
    }

    /**
     * Implements {@code "$"} with MULTILINE.
     */
    public static Sequence
    endOfLine(final String lineBreakCharacters) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset == matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher, offset);
                }

                if (lineBreakCharacters.indexOf(matcher.charAt(offset)) == -1) return -1;

                return this.next.matches(matcher, offset);
            }

            @Override public Sequence
            reverse() {
                Sequence result = this.next.reverse();
                result.concat(Sequences.beginningOfLine(lineBreakCharacters));
                return result;
            }

            @Override public String
            toString() { return "$" + this.next; }
        };
    }

    /**
     * Implements {@code "\b"}, and, negated, {@code "\B"}.
     */
    public static Sequence
    wordBoundary() {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset >= matcher.transparentRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    if (offset == matcher.transparentRegionStart) return -1; // Zero-length region.
                    if (!Sequences.isWordCharacter(matcher.charAt(offset - 1))) return -1;
                } else
                if (offset <= matcher.transparentRegionStart) {
                    if (!Sequences.isWordCharacter(matcher.charAt(offset))) return -1;
                } else
                if (
                    Sequences.isWordCharacter(matcher.charAt(offset - 1))
                    == Sequences.isWordCharacter(matcher.charAt(offset))
                ) {
                    return -1;
                }

                return this.next.matches(matcher, offset);
            }

            @Override public String
            toString() { return "\\b" + this.next; }
        };
    }

    /**
     * Implements {@code "\G"}.
     */
    public static Sequence
    endOfPreviousMatch() {

        return new LinkedAbstractSequence() {

            boolean reverse;

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // The documentation of java.util.regex is totally unclear about the following case, but this seems to
                // be how it works:
                if (matcher.endOfPreviousMatch == -1) return this.next.matches(matcher, offset);
//                if (matcher.endOfPreviousMatch == -1) return -1;

                if (offset != matcher.endOfPreviousMatch) return -1;

                return this.next.matches(matcher, offset);
            }

            @Override public Sequence
            reverse() {
                this.reverse = !this.reverse;
                return super.reverse();
            }

            @Override public String
            toString() { return "\\G" + this.next; }
        };
    }

    /**
     * Implements {@code "(?=X)"}.
     */
    public static Sequence
    positiveLookahead(final Sequence op) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                boolean lookaheadMatches;

                final MatcherImpl.End savedEnd       = matcher.end;
                final int             savedRegionEnd = matcher.regionEnd;
                {
                    matcher.end       = MatcherImpl.End.ANY;
                    matcher.regionEnd = matcher.transparentRegionEnd;

                    lookaheadMatches   = op.matches(matcher, offset) != -1;
                    matcher.requireEnd = matcher.hitEnd;
                }
                matcher.end       = savedEnd;
                matcher.regionEnd = savedRegionEnd;

                return lookaheadMatches ? this.next.matches(matcher, offset) : -1;
            }
        };
    }

    /**
     * Implements {@code "(?<=X)"}.
     */
    public static Sequence
    positiveLookbehind(final Sequence op) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                final int l1 = matcher.subject.length();

                boolean lookbehindMatches;

                final MatcherImpl.End savedEnd          = matcher.end;
                final boolean         savedHitStart     = matcher.hitStart;
                final boolean         savedRequireStart = matcher.requireStart;
                final int             savedRegionStart  = matcher.regionStart;
                {
                    matcher.reverse();
                    {
                        matcher.end        = MatcherImpl.End.ANY;
                        matcher.hitEnd     = false;
                        matcher.regionEnd  = matcher.transparentRegionEnd;
                        lookbehindMatches  = op.matches(matcher, l1 - offset) != -1;
                        matcher.requireEnd = matcher.hitEnd;
                    }
                    matcher.reverse();
                }

                matcher.end          = savedEnd;
                matcher.hitStart     = savedHitStart;
                matcher.requireStart = savedRequireStart;
                matcher.regionStart  = savedRegionStart;

                return lookbehindMatches ? this.next.matches(matcher, offset) : -1;
            }
        };
    }

    /**
     * Optimized version of {@link #greedyQuantifierSequence(Sequence, int, int)} when the operand is a bare
     * character predicate, e.g. a character class.
     */
    public static Sequence
    greedyQuantifierCharacterClass(final IntPredicate operand, final int min, final int max) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // The operand MUST match (min) times;
                int i;
                for (i = 0; i < min; i++) {

                    if (offset >= matcher.regionEnd) {
                        matcher.hitEnd = true;
                        return -1;
                    }

                    int c = matcher.charAt(offset++);

                    // Special handling for UTF-16 surrogates.
                    if (Character.isHighSurrogate((char) c) && offset < matcher.regionEnd) {
                        char c2 = matcher.charAt(offset);
                        if (Character.isLowSurrogate(c2)) {
                            c = Character.toCodePoint((char) c, c2);
                            offset++;
                        }
                    }

                    if (!operand.evaluate(c)) return -1;
                }
                final int offsetAfterMin = offset;

                // Now try to match the operand (max-min) more times.
                for (; i < max; i++) {

                    if (offset >= matcher.regionEnd) {
                        matcher.hitEnd = true;
                        break;
                    }

                    int offset2 = offset;
                    int c       = matcher.charAt(offset2++);

                    // Special handling for UTF-16 surrogates.
                    if (Character.isHighSurrogate((char) c) && offset2 < matcher.regionEnd) {
                        char c2 = matcher.charAt(offset2);
                        if (Character.isLowSurrogate(c2)) {
                            c = Character.toCodePoint((char) c, c2);
                            offset2++;
                        }
                    }

                    if (!operand.evaluate(c)) break;

                    offset = offset2;
                }

                for (;; i--) {

                    int offset2 = this.next.matches(matcher, offset);
                    if (offset2 != -1) return offset2;

                    if (i == min) break;

                    if (
                        Character.isLowSurrogate(matcher.charAt(--offset))
                        && offset > offsetAfterMin
                        && Character.isHighSurrogate(matcher.charAt(offset - 1))
                    ) offset--;
                }

                return -1;
            }

            @Override public String
            toString() { return operand + "{" + min + "," + max + "}" + this.next; }
        };
    }

    /**
     * Implements a greedy quantifier on an "any char" operand, followed by a literal String, e.g. ".*ABC" or
     * ".{3,17}ABC".
     */
    public static AbstractSequence
    greedyQuantifierAnyCharLiteralString(final String ls, final int min, final int max) {

        return new LinkedAbstractSequence() {

            final IndexOf indexOf     = StringUtil.newIndexOf(ls);
            final int     infixLength = ls.length();

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int fromIndex = matcher.regionEnd - this.infixLength;

                if (fromIndex - offset > max) fromIndex = offset + max; // Beware of overflow!

                int toIndex = offset + min;

                matcher.hitEnd = true;

                while (fromIndex >= toIndex) {

                    // Find next match of the infix withing the subject string.
                    fromIndex = this.indexOf.indexOf(matcher.subject, fromIndex, toIndex);
                    if (fromIndex == -1) break;

                    // See if the successor matches the rest of the subject.
                    int result = this.next.matches(matcher, fromIndex + this.infixLength);
                    if (result != -1) return result;

                    // Successor didn't match, continue with next character position.
                    fromIndex--;
                }

                return -1;
            }
        };
    }

    private static String
    join(@Nullable Object[] oa, char glue) {

        if (oa == null || oa.length == 0) return "";

        if (oa.length == 1) return String.valueOf(oa[0]);

        StringBuilder sb = new StringBuilder().append(oa[0]).append(glue).append(oa[1]);
        for (int i = 2; i < oa.length; i++) sb.append(glue).append(oa[i]);
        return sb.toString();
    }

    /**
     * Creates and returns a sequence that produces a zero-width match iff the <var>op</var> does <em>not</em> match,
     * and otherwise (iff the <var>op</var> <em>does</em> match) does <em>not</em> match.
     */
    public static Sequence
    negate(final Sequence op) {

        return new LinkedAbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                boolean operandMatches;

                MatcherImpl.End savedEnd = matcher.end;
                {
                    matcher.end = MatcherImpl.End.ANY;

                    operandMatches = op.matches(matcher, offset) != -1;
                }
                matcher.end = savedEnd;

                return operandMatches ? -1 : this.next.matches(matcher, offset);
            }

            @Override public Sequence
            reverse() {
                return this.next.reverse().concat(Sequences.negate(op.reverse()));
            }
        };
    }

    /**
     * @return Whether the given character is a "word character" in the sense of the "\b" pattern
     */
    public static boolean
    isWordCharacter(int c) {
        return (
            (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || (c >= '0' && c <= '9')
            || c == '_'
        );
    }
}
