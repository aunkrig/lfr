
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

import de.unkrig.commons.lang.Characters;
import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.StringUtil.IndexOf;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Methods that create all kinds of {@link Sequence}s.
 */
public final
class Sequences {

    private Sequences() {}

    enum QuantifierNature { GREEDY, RELUCTANT, POSSESSIVE }

    /**
     * Matches depending on the <var>offset</var>, and the value of {@code matcher.end}.
     */
    public static final Sequence
    TERMINAL = new AbstractSequence() {

        @Override public int
        matches(MatcherImpl matcher, int offset) {
            return matcher.end == MatcherImpl.End.ANY || offset >= matcher.regionEnd ? offset : -1;
        }

        @Override public Sequence
        concat(Sequence that) { return that; }

        @Override public String
        toString() { return "terminal"; }
    };

    /**
     * Implements greedy and reluctant (but <em>not</em> possessive) quantifiers.
     */
    public static Sequence
    quantifier(Sequence operand, final int min, final int max, final int counterIndex, QuantifierNature nature) {

        if (min > max)            return CharacterClasses.FAIL;
        if (max == 0)             return Sequences.TERMINAL;
        if (min == 1 && max == 1) return operand;

        // Optimize "a{3,5}" into "aaaa{0,2}".
        if (min >= 2 && min <= 1000 && operand instanceof CharacterClasses.LiteralChar) {
            CharacterClasses.LiteralChar lc = (CharacterClasses.LiteralChar) operand;
            if (lc.next == Sequences.TERMINAL) {
                return (
                    new Sequences.LiteralString(StringUtil.repeat(min, lc.c))
                    .concat(Sequences.quantifier(lc, 0, max - min, counterIndex, nature))
                );
            }
        }

        // Optimize "(?:abc){3,5}" into "abcabcabc(?:abc){0,2}".
        if (min >= 2 && operand instanceof Sequences.LiteralString) {
            Sequences.LiteralString ls = (Sequences.LiteralString) operand;
            if (ls.next == Sequences.TERMINAL && min * ls.s.length() <= 1000) {
                return (
                    new Sequences.LiteralString(StringUtil.repeat(min, ls.s))
                    .concat(Sequences.quantifier(ls, 0, max - min, counterIndex, nature))
                );
            }
        }

        switch (nature) {

        case GREEDY:
            if (operand instanceof CharacterClass) {
                CharacterClass cc = (CharacterClass) operand;
                if (cc.next == Sequences.TERMINAL) {
                    return Sequences.greedyQuantifierOnCharacterClass(cc, min, max);
                }
            }
            return Sequences.greedyOrReluctantQuantifier(operand, min, max, counterIndex, true);

        case RELUCTANT:
            return Sequences.greedyOrReluctantQuantifier(operand, min, max, counterIndex, false);

        case POSSESSIVE:
            return Sequences.possessiveQuantifier(operand, min, max);

        default:
            throw new AssertionError(nature);
        }
    }

    /**
     * Implements greedy and reluctant (but <em>not</em> possessive) quantifiers.
     */
    private static Sequence
    greedyOrReluctantQuantifier(
        Sequence      operand,
        final int     min,
        final int     max,
        final int     counterIndex,
        final boolean greedy
    ) {

        if ((min == 0 || min == 1) && max == Integer.MAX_VALUE) {

            // Optimization for "x*" and "x+": We can save the overhead of counting repetitions.

            //         +-----------+
            //         |  result   |
            //         +-----------+
            //           |  or   |
            //           v       v
            //   +---------+   +--------+   +--------+
            //   | operand |-->|   cs   |-->|  next  |
            //   +---------+   +--------+   +--------+
            //        ^            |
            //        |            |
            //        +------------+

            final Sequence[] operand2   = { operand };
            final String     opToString = operand.toString();

            final CompositeSequence cs = new CompositeSequence() {

                @Override public int
                matches(MatcherImpl matcher, int offset) {

                    if (greedy) {

                        int result = operand2[0].matches(matcher, offset);
                        if (result != -1) return result;

                        return this.next.matches(matcher, offset);
                    } else {

                        int result = this.next.matches(matcher, offset);
                        if (result != -1) return result;

                        return operand2[0].matches(matcher, offset);
                    }
                }

                @Override protected String
                toStringWithoutNext() { return "???cs"; }
            };

            operand2[0] = operand.concat(cs);

            return new AbstractSequence() {

                @Override public int
                matches(MatcherImpl matcher, int offset) {

                    if (min == 0) {
                        matcher.counters[counterIndex] = 0;
                        return cs.matches(matcher, offset);
                    } else {
                        matcher.counters[counterIndex] = 1;
                        return operand2[0].matches(matcher, offset);
                    }
                }

                @Override public Sequence
                concat(final Sequence that) {

                    // Optimize the special case ".{min,}literalstring".
                    if (
                        operand2[0] instanceof CharacterClasses.AnyCharacter
                        && ((CompositeSequence) operand2[0]).next == cs
                        && that instanceof LiteralString
                    ) {
                        LiteralString ls = (LiteralString) that;

                        return (
                            greedy
                            ? Sequences.greedyQuantifierOnAnyCharAndLiteralString(min, Integer.MAX_VALUE, ls.s)
                            : Sequences.reluctantQuantifierOnAnyCharAndLiteralString(min, Integer.MAX_VALUE, ls.s)
                        ).concat(ls.next);
                    }

                    cs.concat(that);
                    return this;
                }

                @Override public String
                toString() {
                    return (
                        (greedy ? "greedyQuantifier(operand=" : "reluctantQuantifier(operand=")
                        + opToString
                        + ", min="
                        + min
                        + ")"
                        + (cs.next == Sequences.TERMINAL ? "" : " . " + cs.next)
                    );
                }
            };
        }

        //         +-----------+
        //         |  result   |
        //         +-----------+
        //           |  or   |
        //           v       v
        //   +---------+   +--------+   +--------+
        //   | operand |-->|   cs   |-->|  next  |
        //   +---------+   +--------+   +--------+
        //        ^            |
        //        |            |
        //        +------------+

        final Sequence[] operand2   = { operand };
        final String     opToString = operand.toString();

        final CompositeSequence cs = new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (greedy) {
                    int ic = matcher.counters[counterIndex];

                    if (ic == max) return this.next.matches(matcher, offset);

                    matcher.counters[counterIndex] = ic + 1;

                    if (ic < min) return operand2[0].matches(matcher, offset);

                    int result = operand2[0].matches(matcher, offset);
                    if (result != -1) return result;

                    return this.next.matches(matcher, offset);
                } else {
                    int ic = matcher.counters[counterIndex];

                    if (ic >= min) {

                        int result = this.next.matches(matcher, offset);
                        if (result != -1) return result;
                    }

                    if (++ic > max) return -1;

                    matcher.counters[counterIndex] = ic;

                    return operand2[0].matches(matcher, offset);
                }
            }

            @Override protected String
            toStringWithoutNext() { return "???cs"; }
        };

        operand2[0] = operand.concat(cs);

        return new AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (min == 0) {
                    matcher.counters[counterIndex] = 0;
                    return cs.matches(matcher, offset);
                } else {
                    matcher.counters[counterIndex] = 1;
                    return operand2[0].matches(matcher, offset);
                }
            }

            @Override public Sequence
            concat(final Sequence that) {

                // Optimize the special case ".{min,max}literalstring".
                if (
                    operand2[0] instanceof CharacterClasses.AnyCharacter
                    && ((CompositeSequence) operand2[0]).next == cs
                    && that instanceof LiteralString
                ) {
                    LiteralString ls = (LiteralString) that;

                    return (
                        greedy
                        ? Sequences.greedyQuantifierOnAnyCharAndLiteralString(min, max, ls.s)
                        : Sequences.reluctantQuantifierOnAnyCharAndLiteralString(min, max, ls.s)
                    ).concat(ls.next);
                }

                cs.concat(that);
                return this;
            }

            @Override public String
            toString() {
                return (
                    (greedy ? "greedyQuantifier(operand=" : "reluctantQuantifier(operand=")
                    + opToString
                    + ", min="
                    + min
                    + ", max="
                    + Sequences.maxToString(max)
                    + ")"
                    + (cs.next == Sequences.TERMINAL ? "" : " . " + cs.next)
                );
            }
        };
    }

    /**
     * Implements a reluctant quantifier on an "any char" operand, followed by a literal String.
     * <p>
     *   Examples: <code>".*?ABC" ".{3,17}?ABC"</code>
     * </p>
     */
    public static CompositeSequence
    reluctantQuantifierOnAnyCharAndLiteralString(final int min, final int max, final String s) {

        return new CompositeSequence() {

            final int     len     = s.length();
            final IndexOf indexOf = StringUtil.newIndexOf(s);

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
                    int result = this.next.matches(matcher, offset + this.len);
                    if (result != -1) return result;

                    // Successor didn't match, continue with next character position.
                    offset++;
                }

                matcher.hitEnd = true;
                return -1;
            }

            @Override public String
            toStringWithoutNext() {
                return (
                    "reluctantQuantifierOnAnyChar(min="
                    + min
                    + ", max="
                    + Sequences.maxToString(max)
                    + ", ls="
                    + this.indexOf
                    + ")"
                );
            }
        };
    }

    /**
     * Implements a "possessive" quantifier for an operand.
     */
    private static Sequence
    possessiveQuantifier(final Sequence operand, final int min, final int max) {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                MatcherImpl.End savedEnd = matcher.end;
                try {

                    matcher.end = MatcherImpl.End.ANY;

                    // The operand MUST match (min) times;
                    for (int i = 0; i < min; i++) {
                        offset = operand.matches(matcher, offset);
                        if (offset == -1) return -1;
                    }

                    // Now try to match the operand (max-min) more times.
                    int limit = max - min;

                    for (int i = 0; i < limit; i++) {

                        int offset2 = operand.matches(matcher, offset);

                        if (offset2 == -1) break;

                        offset = offset2;
                    }
                } finally {
                    matcher.end = savedEnd;
                }

                return this.next.matches(matcher, offset);
            }

            @Override public Sequence
            concat(Sequence that) {
                that = (this.next = this.next.concat(that));

                // Optimize for "." operand.
                if (
                    operand instanceof CharacterClasses.AnyCharacter
                    && ((CompositeSequence) operand).next == Sequences.TERMINAL
                ) {

                    // Replace the possessive quantifier element.
                    return new CompositeSequence() {

                        @Override public int
                        matches(MatcherImpl matcher, int offset) {

                            if (max > matcher.regionEnd - offset) {
                                offset = matcher.regionEnd;
                            } else {
                                offset += max;
                            }

                            return this.next.matches(matcher, offset);
                        }

                        @Override public String
                        toStringWithoutNext() {
                            return (
                                "possessiveQuantifierOnAnyChar(min="
                                + min
                                + ", max="
                                + Sequences.maxToString(max)
                                + ")"
                            );
                        }
                    }.concat(this.next);
                }

                return this;
            }

            @Override public String
            toStringWithoutNext() {
                return (
                    "possessiveQuantifier(operand="
                    + operand
                    + ", min="
                    + min
                    + ", max="
                    + Sequences.maxToString(max)
                    + ")"
                );
            }
        };
    }

    /**
     * Representation of a sequence of literal, case-sensitive characters.
     */
    public static
    class LiteralString extends CompositeSequence {

        private String             s;
        private StringUtil.IndexOf indexOf;

        /**
         * @param s The literal string that this sequence represents
         */
        LiteralString(String s) {
            this.s       = s;
            this.indexOf = StringUtil.newIndexOf(this.s);
        }

        /**
         * @return The literal string that this sequence represents
         */
        public String get() { return this.s; }

        /**
         * Changes the literal string that this sequence represents.
         */
        public void
        set(String s) {
            this.s       = s;
            this.indexOf = StringUtil.newIndexOf(this.s);
        }

        @Override public int
        matches(MatcherImpl matcher, int offset) {
            offset = matcher.peekRead(offset, this.s);
            if (offset == -1) return -1;
            return this.next.matches(matcher, offset);
        }

        @Override public boolean
        find(MatcherImpl matcher, int start) {

            while (start < matcher.regionEnd) {

                // Find the next occurrence of the literal string.
                int offset = this.indexOf.indexOf(matcher.subject, start, matcher.regionEnd - this.s.length());
                if (offset == -1) break;

                // See if the rest of the pattern matches.
                int result = this.next.matches(matcher, offset + this.s.length());
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
        concat(Sequence that) {
            that = (this.next = this.next.concat(that));

            if (that instanceof CharacterClasses.LiteralChar) {
                CharacterClasses.LiteralChar thatLiteralCharacter = (CharacterClasses.LiteralChar) that;

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
        toStringWithoutNext() { return this.indexOf.toString(); }
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

        if (alternatives.length == 0) return Sequences.TERMINAL;

        if (alternatives.length == 1) return alternatives[0];

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                for (int i = 0; i < alternatives.length; i++) {

                    final int[] savedGroups = matcher.groups;
                    {

                        int result;

                        final MatcherImpl.End savedEnd = matcher.end;
                        try {
                            matcher.end = MatcherImpl.End.ANY;
                            result      = alternatives[i].matches(matcher, offset);
                        } finally {
                            matcher.end = savedEnd;
                        }

                        if (result != -1) {
                            result = this.next.matches(matcher, result);
                            if (result != -1) return result;
                        }
                    }

                    // Alternative did not match; restore original capturing groups.
                    matcher.groups = savedGroups;
                }

                return -1;
            }

            @Override public String
            toStringWithoutNext() { return "alternatives(" + Sequences.join(alternatives, ", ") + ")"; }
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

        if (alternatives.length == 0) return Sequences.TERMINAL;

        if (alternatives.length == 1) return alternatives[0];

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                for (Sequence a : alternatives) {
                    MatcherImpl.End savedEnd = matcher.end;
                    try {
                        matcher.end = MatcherImpl.End.ANY;
                        int[] savedGroups = matcher.groups;
                        {
                            int result = a.matches(matcher, offset);
                            if (result != -1) return this.next.matches(matcher, result);
                        }

                        // Alternative did not match; restore original capturing groups.
                        matcher.groups = savedGroups;
                    } finally {
                        matcher.end = savedEnd;
                    }
                }

                return -1;
            }

            @Override public String
            toStringWithoutNext() { return "independentNonCapturingGroup(" + Sequences.join(alternatives, ", ") + ")"; }
        };
    }

    /**
     * Implements {@code "("}.
     */
    public static Sequence
    capturingGroupStart(final int groupNumber) {

        return new CompositeSequence() {

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

            @Override public String
            toStringWithoutNext() { return "capturingGroupStart(" + groupNumber + ")"; }
        };
    }

    /**
     * Implements {@code ")"}.
     */
    public static Sequence
    capturingGroupEnd(final int groupNumber) {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                matcher.groups[2 * groupNumber + 1] = offset;

                return this.next.matches(matcher, offset);
            }

            @Override public String
            toStringWithoutNext() { return "capturingGroupEnd(" + groupNumber + ")"; }
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

        return new CompositeSequence() {

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
            toStringWithoutNext() { return "capturingGroup(" + subsequence + ")"; }
        };
    }

    /**
     * Representation of a capturing group backreference, e.g. {@code \3} .
     *
     * @param groupNumber 1...<var>groupCount</var>
     */
    static Sequence
    capturingGroupBackReference(final int groupNumber) {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int[] gs    = matcher.groups;
                int   start = gs[2 * groupNumber];
                int   end   = gs[2 * groupNumber + 1];

                // If the referenced group didn't match, then neither does this back reference.
                if (start == -1) return -1;

                offset = matcher.peekRead(offset, matcher.subject, start, end);
                if (offset == -1) return -1;

                return this.next.matches(matcher, offset);
            }

            @Override public String
            toStringWithoutNext() { return "capturingGroupBackreference(" + groupNumber + ")"; }
        };
    }

    /**
     * Representation of a case-insensitive capturing group backreference, e.g. {@code \3} .
     *
     * @param groupNumber 1...<var>groupCount</var>
     */
    static Sequence
    caseInsensitiveCapturingGroupBackReference(final int groupNumber) {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int[] gs    = matcher.groups;
                int   start = gs[2 * groupNumber];
                int   end   = gs[2 * groupNumber + 1];

                // If the referenced group didn't match, then neither does this back reference.
                if (start == -1) return -1;

                offset = matcher.caseInsensitivePeekRead(offset, matcher.subject.subSequence(start, end));
                if (offset == -1) return -1;

                return this.next.matches(matcher, offset);
            }

            @Override public String
            toStringWithoutNext() { return "caseInsensitiveCapturingGroupBackreference(" + groupNumber + ")"; }
        };
    }

    /**
     * Representation of a UNICODE-case-insensitive capturing group backreference, e.g. {@code \3} .
     *
     * @param groupNumber 1...<var>groupCount</var>
     */
    static Sequence
    unicodeCaseInsensitiveCapturingGroupBackReference(final int groupNumber) {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int[] gs    = matcher.groups;
                int   start = gs[2 * groupNumber];
                int   end   = gs[2 * groupNumber + 1];

                // If the referenced group didn't match, then neither does this back reference.
                if (start == -1) return -1;

                offset = matcher.unicodeCaseInsensitivePeekRead(offset, matcher.subject.subSequence(start, end));
                if (offset == -1) return -1;

                return this.next.matches(matcher, offset);
            }

            @Override public String
            toStringWithoutNext() { return "caseInsensitiveCapturingGroupBackreference(" + groupNumber + ")"; }
        };
    }

    /**
     * Implements {@code "^"} with !MULTILINE, and {@code "\A"}.
     */
    public static Sequence
    beginningOfInput() {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {
                return offset != matcher.anchoringRegionStart ? -1 : this.next.matches(matcher, offset);
            }

            // Override "AbstractSequence.find()" such that we give the match only one shot.
            @Override public boolean
            find(MatcherImpl matcher, int start) {

                matcher.hitEnd = false;

                int newOffset = this.matches(matcher, start);
                if (newOffset != -1) {
                    matcher.groups[0] = start;
                    matcher.groups[1] = newOffset;
                    return true;
                }

                return false;
            }

            @Override public String
            toStringWithoutNext() { return "beginningOfInput"; }
        };
    }

    /**
     * Implements {@code "^"} with MULTILINE.
     */
    public static Sequence
    beginningOfLine() {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset == matcher.anchoringRegionStart) return this.next.matches(matcher, offset);

                if (offset == matcher.anchoringRegionEnd) {
                    matcher.hitEnd = true;
                    return -1;
                }

                char c = matcher.subject.charAt(offset - 1);
                if (
                    (c == '\r' && matcher.subject.charAt(offset) != '\n')
                    || c == '\n'
                    || c == '\u000B'
                    || c == '\f'
                    || c == '\u0085'
                    || c == '\u2028'
                    || c == '\u2029'
                ) return this.next.matches(matcher, offset);

                return -1;
            }

            @Override public String
            toStringWithoutNext() { return "beginningOfLine"; }
        };
    }

    /**
     * Implements {@code "^"} with MULTILINE and UNIX_LINES.
     */
    public static Sequence
    beginningOfUnixLine() {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (
                    offset == matcher.anchoringRegionStart
                    || (matcher.subject.charAt(offset - 1) == '\n' && offset != matcher.anchoringRegionEnd)
                ) return this.next.matches(matcher, offset);

                if (offset == matcher.anchoringRegionEnd) matcher.hitEnd = true;

                return -1;
            }

            @Override public String
            toStringWithoutNext() { return "beginningOfUnixLine"; }
        };
    }

    /**
     * Implements {@code "\Z"}.
     */
    public static Sequence
    endOfInputButFinalTerminator() {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset >= matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher, offset);
                }

                char c = matcher.subject.charAt(offset);
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

                if (c == '\r' && matcher.subject.charAt(offset + 1) == '\n') {
                    if (offset == matcher.anchoringRegionEnd - 2) {
                        matcher.hitEnd     = true;
                        matcher.requireEnd = true;
                        return this.next.matches(matcher, offset);
                    }
                }

                return -1;
            }

            @Override public String
            toStringWithoutNext() { return "endOfInputButFinalTerminator"; }
        };
    }

    /**
     * Implements {@code "\Z"}.
     */
    public static Sequence
    endOfInputButFinalUnixTerminator() {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset >= matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher, offset);
                }

                if (
                    offset == matcher.anchoringRegionEnd - 1
                    && matcher.subject.charAt(offset) == '\n'
                ) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher, offset);
                }

                return -1;
            }

            @Override public String
            toStringWithoutNext() { return "endOfInputButFinalUnixTerminator"; }
        };
    }

    /**
     * Implements {@code "$"} with !MULTILINE, and {@code "\z"}.
     */
    public static Sequence
    endOfInput() {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset < matcher.anchoringRegionEnd) return -1;

                matcher.hitEnd     = true;
                matcher.requireEnd = true;

                return this.next.matches(matcher, offset);
            }

            @Override public String
            toStringWithoutNext() { return "endOfInput"; }
        };
    }

    /**
     * Implements {@code "$"} with MULTILINE.
     */
    public static Sequence
    endOfLine() {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset == matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher, offset);
                }

                char c = matcher.subject.charAt(offset);
                return (
                    (c == '\n' && (
                        offset == matcher.anchoringRegionStart
                        || Character.codePointBefore(matcher.subject, offset) != '\r'
                    ))
                    || c == '\r'
                    || c == '\u000B'
                    || c == '\f'
                    || c == '\u0085'
                    || c == '\u2028'
                    || c == '\u2029'
                ) ? this.next.matches(matcher, offset) : -1;
            }

            @Override public String
            toStringWithoutNext() { return "endOfLine"; }
        };
    }

    /**
     * Implements {@code "$"} with MULTILINE and UNIX_LINES.
     */
    public static Sequence
    endOfUnixLine() {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset == matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher, offset);
                }

                return matcher.subject.charAt(offset) == '\n' ? this.next.matches(matcher, offset) : -1;
            }

            @Override public String
            toStringWithoutNext() { return "endOfLine"; }
        };
    }

    /**
     * A sequence that matches a linebreak; implements {@code "\R"}.
     */
    public static Sequence
    linebreak() {

        return new CompositeSequence() {

            char c1 = '\r', c2 = '\n';

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset >= matcher.regionEnd) return -1;

                char c = matcher.subject.charAt(offset);

                // Check for linebreak characters in a highly optimized manner.
                if (c <= 0x0d) {
                    if (
                        c == this.c1
                        && offset < matcher.regionEnd - 1
                        && matcher.subject.charAt(offset + 1) == this.c2
                    ) {
                        return this.next.matches(matcher, offset + 2);
                    }
                    return c >= 0x0a ? this.next.matches(matcher, offset + 1) : -1;
                }

                if (c == 0x85 || c == 0x2028 || c == 0x2029) {
                    return this.next.matches(matcher, offset + 1);
                }

                return -1;
            }

            @Override public String
            toStringWithoutNext() { return "linebreak"; }
        };
    }

    /**
     * Implements {@code "\b"}, and, negated, {@code "\B"}.
     */
    public static Sequence
    wordBoundary() { return Sequences.wordBoundary(Characters.IS_WORD, "wordBoundary"); }

    /**
     * Implements {@code "\b"}, and, negated, {@code "\B"}.
     */
    public static Sequence
    unicodeWordBoundary() { return Sequences.wordBoundary(Characters.IS_UNICODE_WORD, "unicodeWordBoundary"); }

    /**
     * Implements {@code "\b"}, and, negated, {@code "\B"}.
     */
    private static Sequence
    wordBoundary(final Predicate<Integer> isWordCharacter, final String toString) {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // The "Non-spacing mark" character is sometimes a word character (iff its left neighbor is a word
                // character), and sometimes it is not (iff its left neighbor is a non-word character).

                boolean result;
                if (offset >= matcher.transparentRegionEnd) {

                    // At end of transparent region.
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    if (offset == matcher.transparentRegionStart) return -1; // Zero-length region.
                    result = isWordCharacter.evaluate(Character.codePointBefore(matcher.subject, offset));
                } else
                if (offset <= matcher.transparentRegionStart) {

                    // At start of transparent region.
                    result = isWordCharacter.evaluate(Character.codePointAt(matcher.subject, offset));
                } else
                if (matcher.subject.charAt(offset) == '\u030a') {
                    result = false;
                } else
                {

                    // IN transparent region (not at its start nor at its end).
                    int cpBefore = Character.codePointBefore(matcher.subject, offset);
                    int cpAt     = Character.codePointAt(matcher.subject, offset);

                    if (cpBefore == '\u030a') {
                        for (int i = offset - 1;; i--) {
                            if (i <= matcher.transparentRegionStart) {
                                if (!isWordCharacter.evaluate(cpAt)) return -1;
                                break;
                            }

                            cpBefore = Character.codePointBefore(matcher.subject, i);
                            if (cpBefore != '\u030a') break;
                        }
                    }
                    result = isWordCharacter.evaluate(cpBefore) != isWordCharacter.evaluate(cpAt);
                }

                return result ? this.next.matches(matcher, offset) : -1;
            }

            @Override public String
            toStringWithoutNext() { return toString; }
        };
    }

    /**
     * Implements {@code "\G"}.
     */
    public static Sequence
    endOfPreviousMatch() {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // The documentation of java.util.regex is totally unclear about the following case, but this seems to
                // be how it works:
                if (matcher.endOfPreviousMatch == -1) return this.next.matches(matcher, offset);
//                if (matcher.endOfPreviousMatch == -1) return -1;

                if (offset != matcher.endOfPreviousMatch) return -1;

                return this.next.matches(matcher, offset);
            }

            @Override public String
            toStringWithoutNext() { return "endOfPreviousMatch"; }
        };
    }

    /**
     * Implements {@code "(?=X)"}.
     */
    public static Sequence
    positiveLookahead(final Sequence op) {

        return new CompositeSequence() {

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

            @Override public String
            toStringWithoutNext() { return "positiveLookahead(" + op + ")"; }
        };
    }

    /**
     * Implements {@code "(?<=X)"}.
     */
    public static Sequence
    positiveLookbehind(final Sequence op) {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // In most cases, it is more efficient to FIRST check whether the rest of the pattern matches, and
                // only THEN check if the lookbehind matches.
                int result = this.next.matches(matcher, offset);
                if (result == -1) return -1;

                boolean lookbehindMatches;
                {
                    final MatcherImpl.End savedEnd         = matcher.end;
                    final boolean         savedHitEnd      = matcher.hitEnd;
                    final int             savedRegionStart = matcher.regionStart;
                    final int             savedRegionEnd   = matcher.regionEnd;
                    try {
                        matcher.end        = MatcherImpl.End.END_OF_REGION;
                        matcher.regionEnd  = offset;
                        lookbehindMatches  = op.find(matcher, matcher.transparentRegionStart);
                    } finally {
                        matcher.end         = savedEnd;
                        matcher.hitEnd      = savedHitEnd;
                        matcher.regionStart = savedRegionStart;
                        matcher.regionEnd   = savedRegionEnd;
                    }
                }

                return lookbehindMatches ? result : -1;
            }

            @Override public String
            toStringWithoutNext() { return "positiveLookbehind(" + op + ")"; }
        };
    }

    /**
     * Optimized version of {@link #greedyOrReluctantQuantifier(Sequence, int, int, int, boolean)} when the operand is
     * a bare character class.
     */
    public static Sequence
    greedyQuantifierOnCharacterClass(final CharacterClass operand, final int min, final int max) {

        return new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // The operand MUST match (min) times;
                int i;
                for (i = 0; i < min; i++) {

                    if (offset >= matcher.regionEnd) {
                        matcher.hitEnd = true;
                        return -1;
                    }

                    int c = matcher.subject.charAt(offset++);

                    // Special handling for UTF-16 surrogates.
                    if (Character.isHighSurrogate((char) c) && offset < matcher.regionEnd) {
                        char c2 = matcher.subject.charAt(offset);
                        if (Character.isLowSurrogate(c2)) {
                            c = Character.toCodePoint((char) c, c2);
                            offset++;
                        }
                    }

                    if (!operand.matches(c)) return -1;
                }
                final int offsetAfterMin = offset;

                // Now try to match the operand (max-min) more times.
                for (; i < max; i++) {

                    if (offset >= matcher.regionEnd) {
                        matcher.hitEnd = true;
                        break;
                    }

                    int offset2 = offset;
                    int c       = matcher.subject.charAt(offset2++);

                    // Special handling for UTF-16 surrogates.
                    if (Character.isHighSurrogate((char) c) && offset2 < matcher.regionEnd) {
                        char c2 = matcher.subject.charAt(offset2);
                        if (Character.isLowSurrogate(c2)) {
                            c = Character.toCodePoint((char) c, c2);
                            offset2++;
                        }
                    }

                    if (!operand.matches(c)) break;

                    offset = offset2;
                }

                for (;; i--) {

                    int offset2 = this.next.matches(matcher, offset);
                    if (offset2 != -1) return offset2;

                    if (i == min) break;

                    if (
                        Character.isLowSurrogate(matcher.subject.charAt(--offset))
                        && offset > offsetAfterMin
                        && Character.isHighSurrogate(matcher.subject.charAt(offset - 1))
                    ) offset--;
                }

                return -1;
            }

            @Override public Sequence
            concat(Sequence that) {

                // Optimize the special case ".{min,}literalstring".
                if (
                    operand instanceof CharacterClasses.AnyCharacter
                    && ((CompositeSequence) operand).next == Sequences.TERMINAL
                    && that instanceof LiteralString
                ) {
                    LiteralString ls = (LiteralString) that;
                    return Sequences.greedyQuantifierOnAnyCharAndLiteralString(min, Integer.MAX_VALUE, ls.s);
                }

                return super.concat(that);
            }

            @Override public String
            toStringWithoutNext() {
                return (
                    "greedyQuantifierOnCharacterClass(operand="
                    + operand
                    + ", min="
                    + min
                    + ", max="
                    + Sequences.maxToString(max)
                    + ")"
                );
            }
        };
    }

    /**
     * Implements a greedy quantifier on an "any char" operand, followed by a literal String.
     * <p>
     *   Examples: <code>".*ABC" ".{3,17}ABC"</code>
     * </p>
     */
    public static Sequence
    greedyQuantifierOnAnyCharAndLiteralString(final int min, final int max, final String s) {

        return new CompositeSequence() {

            final IndexOf indexOf     = StringUtil.newIndexOf(s);
            final int     infixLength = s.length();

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

            @Override public String
            toStringWithoutNext() {
                return (
                    "greedyQuantifierAnyChar(min="
                    + min
                    + ", max="
                    + Sequences.maxToString(max)
                    + ", ls="
                    + this.indexOf
                    + ")"
                );
            }
        };
    }

    private static String
    join(@Nullable Object[] oa, String glue) {

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

        return new CompositeSequence() {

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

            @Override public String
            toStringWithoutNext() { return "negate(" + op + ")"; }
        };
    }


    private static String
    maxToString(int n) { return n == Integer.MAX_VALUE ? "infinite" : Integer.toString(n); }
}
