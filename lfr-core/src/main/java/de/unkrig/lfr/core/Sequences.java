
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

    /**
     * Matches the empty string, and is the last element of any {@link CompositeSequence} chain.
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
        toString() { return "terminal"; }
    };

    /**
     * Implements greedy and reluctant (but <em>not</em> possessive) quantifiers.
     */
    public static Sequence
    quantifier(Sequence operand, final int min, final int max, final int counterIndex, final boolean greedy) {

        if (min > max)            return CharacterClasses.FAIL;
        if (max == 0)             return Sequences.TERMINAL;
        if (min == 1 && max == 1) return operand;

        if ((min == 0 || min == 1) && max == Integer.MAX_VALUE) {

            // Optimization for "x*" and "x+": We can save the overhead of counting repetitions.

            //          +----------+
            //          |  result  |
            //          +----------+
            //            |  or  |
            //            v      v
            //   +----------+  +--------+   +--------+
            //   | operand  |=>|   cs   |-->|  next  |
            //   +----------+  +--------+   +--------+
            //          ^             |
            //          |             |
            //          +-------------+

            final Sequence[] operand2   = { operand };
            final String     opToString = operand.toString();

            final CompositeSequence cs = new CompositeSequence() {

                @Override public int
                matches(MatcherImpl matcher, int offset) {

                    if (greedy) {

                        int[] savedCounters = matcher.counters;
                        {
                            int result = operand2[0].matches(matcher, offset);
                            if (result != -1) return result;
                        }
                        matcher.counters = savedCounters;

                        return this.next.matches(matcher, offset);
                    } else {

                        int[] savedCounters = matcher.counters;
                        {
                            int result = this.next.matches(matcher, offset);
                            if (result != -1) return result;
                        }
                        matcher.counters = savedCounters;

                        return operand2[0].matches(matcher, offset);
                    }
                }

                @Override public Sequence
                reverse() { return Sequences.TERMINAL; }

                @Override protected String
                toStringWithoutNext() { return "???cs"; }
            };

            operand2[0] = operand.concat(cs);

            return new AbstractSequence() {

                @Override public int
                matches(MatcherImpl matcher, int offset) {

                    if (min == 0) {
                        (matcher.counters = matcher.counters.clone())[counterIndex] = 0;
                        return cs.matches(matcher, offset);
                    } else {
                        (matcher.counters = matcher.counters.clone())[counterIndex] = 1;
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
                            ? Sequences.greedyQuantifierOfAnyChar(min, Integer.MAX_VALUE, ls)
                            : Sequences.reluctantQuantifierOfAnyChar(min, Integer.MAX_VALUE, ls)
                        ).concat(ls.next);
                    }

                    cs.concat(that);
                    return this;
                }

                @Override public Sequence
                reverse() {
                    return cs.next.reverse().concat(
                        Sequences.quantifier(operand2[0].reverse(), min, max, counterIndex, greedy)
                    );
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

        //          +----------+
        //          |  result  |
        //          +----------+
        //            |  or  |
        //            v      v
        //   +----------+  +--------+   +--------+
        //   | operand  |=>|   cs   |-->|  next  |
        //   +----------+  +--------+   +--------+
        //          ^             |
        //          |             |
        //          +-------------+

        final Sequence[] operand2   = { operand };
        final String     opToString = operand.toString();

        final CompositeSequence cs = new CompositeSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (greedy) {
                    int ic = matcher.counters[counterIndex];

                    if (ic == max) return this.next.matches(matcher, offset);

                    (matcher.counters = matcher.counters.clone())[counterIndex] = ic + 1;

                    if (ic < min) return operand2[0].matches(matcher, offset);

                    int[] savedCounters = matcher.counters;
                    {
                        int result = operand2[0].matches(matcher, offset);
                        if (result != -1) return result;
                    }
                    matcher.counters = savedCounters;

                    return this.next.matches(matcher, offset);
                } else {
                    int ic = matcher.counters[counterIndex];

                    if (ic >= min) {

                        int[] savedCounters = matcher.counters;
                        {
                            int result = this.next.matches(matcher, offset);
                            if (result != -1) return result;
                        }
                        matcher.counters = savedCounters;
                    }

                    if (++ic > max) return -1;

                    (matcher.counters = matcher.counters.clone())[counterIndex] = ic;

                    return operand2[0].matches(matcher, offset);
                }
            }

            @Override public Sequence
            reverse() { return Sequences.TERMINAL; }

            @Override protected String
            toStringWithoutNext() { return "???cs"; }
        };

        operand2[0] = operand.concat(cs);

        return new AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (min == 0) {
                    (matcher.counters = matcher.counters.clone())[counterIndex] = 0;
                    return cs.matches(matcher, offset);
                } else {
                    (matcher.counters = matcher.counters.clone())[counterIndex] = 1;
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
                        ? Sequences.greedyQuantifierOfAnyChar(min, max, ls)
                        : Sequences.reluctantQuantifierOfAnyChar(min, max, ls)
                    ).concat(ls.next);
                }

                cs.concat(that);
                return this;
            }

            @Override public Sequence
            reverse() {
                return cs.next.reverse().concat(
                    Sequences.quantifier(operand2[0].reverse(), min, max, counterIndex, greedy)
                );
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
    reluctantQuantifierOfAnyChar(final int min, final int max, final LiteralString ls) {

        return new CompositeSequence() {

            final String  s       = ls.get();
            final int     len     = this.s.length();
            final IndexOf indexOf = StringUtil.newIndexOf(this.s);

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
                    "reluctantQuantifierSequenceAnyChar(min="
                    + min
                    + ", max="
                    + Sequences.maxToString(max)
                    + ", ls="
                    + ls
                    + ")"
                );
            }
        };
    }

    /**
     * Implements a "possessive" quantifier for an operand.
     */
    public static Sequence
    possessiveQuantifier(final Sequence operand, final int min, final int max) {

        return new CompositeSequence() {

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
                                "possessiveQuantifierSequenceOfAnyChar(min="
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
                    "possessiveQuantifierSequence("
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

            this.set(new StringBuilder(this.s).reverse().toString());

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
                        int result = alternatives[i].matches(matcher, offset);
                        if (result != -1) {
                            result = this.next.matches(matcher, result);
                            if (result != -1) return result;
                        }
                    }
                    matcher.groups = savedGroups;
                }

                return -1;
            }

            @Override public Sequence
            reverse() {

                for (int i = 0; i < alternatives.length; i++) alternatives[i] = alternatives[i].reverse();

                return this;
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
                    int result = a.matches(matcher, offset);
                    if (result != -1) return this.next.matches(matcher, result);
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

            @Override public Sequence
            reverse() { return this.next.reverse().concat(Sequences.capturingGroupEnd(groupNumber)); }

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

            @Override public Sequence
            reverse() { return Sequences.capturingGroupStart(groupNumber).concat(this.next.reverse()); }

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

                offset = matcher.peekRead(offset, matcher.subject.subSequence(start, end));
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

            @Override public Sequence
            reverse() {
                Sequence result = this.next.reverse();
                result.concat(Sequences.endOfInput());
                return result;
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

            @Override public Sequence
            reverse() { return this.next.reverse().concat(Sequences.endOfLine()); }

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

            @Override public Sequence
            reverse() { return this.next.reverse().concat(Sequences.endOfUnixLine()); }

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

            @Override public Sequence
            reverse() {
                Sequence result = this.next.reverse();
                result.concat(Sequences.beginningOfInput());
                return result;
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

            @Override public Sequence
            reverse() {
                Sequence result = this.next.reverse();
                result.concat(Sequences.beginningOfInput());
                return result;
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

            @Override public Sequence
            reverse() { return this.next.reverse().concat(Sequences.beginningOfLine()); }

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

            @Override public Sequence
            reverse() { return this.next.reverse().concat(Sequences.beginningOfUnixLine()); }

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

            @Override public Sequence
            reverse() {
                char tmp = this.c1;
                this.c1 = this.c2;
                this.c2 = tmp;
                return super.reverse();
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

            @Override public String
            toStringWithoutNext() { return "positiveLookbehind(" + op + ")"; }
        };
    }

    /**
     * Optimized version of {@link #greedyQuantifierSequence(Sequence, int, int)} when the operand is a bare
     * character predicate, e.g. a character class.
     */
    public static Sequence
    greedyQuantifierOfCharacterClass(final CharacterClass operand, final int min, final int max) {

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

            @Override public String
            toStringWithoutNext() {
                return "greedyQuantifier(" + operand + ", min=" + min + ", max=" + Sequences.maxToString(max) + ")";
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
    greedyQuantifierOfAnyChar(final int min, final int max, final LiteralString ls) {

        return new CompositeSequence() {

            final String  s           = ls.s;
            final IndexOf indexOf     = StringUtil.newIndexOf(this.s);
            final int     infixLength = this.s.length();

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

            @Override public Sequence
            reverse() {
                return this.next.reverse().concat(Sequences.negate(op.reverse()));
            }

            @Override public String
            toStringWithoutNext() { return "negate(" + op + ")"; }
        };
    }


    private static String
    maxToString(int n) { return n == Integer.MAX_VALUE ? "infinite" : Integer.toString(n); }
}
