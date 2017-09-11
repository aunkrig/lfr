
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

    enum QuantifierNature { GREEDY, RELUCTANT, POSSESSIVE } // SUPPRESS CHECKSTYLE JavadocVariable

    /**
     * Matches depending on the <var>offset</var>, and the value of {@code matcher.end}.
     */
    public static final Sequence
    TERMINAL = new AbstractSequence() {

        @Override public boolean
        matches(MatcherImpl matcher) {
            return matcher.end == MatcherImpl.End.ANY || matcher.offset >= matcher.regionEnd;
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

                @Override public boolean
                matches(MatcherImpl matcher) {

                    if (greedy) {

                        int savedOffset = matcher.offset;

                        if (operand2[0].matches(matcher)) return true;

                        matcher.offset = savedOffset;

                        return this.next.matches(matcher);
                    } else {

                        int savedOffset = matcher.offset;

                        if (this.next.matches(matcher)) return true;

                        matcher.offset = savedOffset;

                        return operand2[0].matches(matcher);
                    }
                }

                @Override protected String
                toStringWithoutNext() { return "???cs"; }
            };

            operand2[0] = operand.concat(cs);

            return new AbstractSequence() {

                @Override public boolean
                matches(MatcherImpl matcher) {

                    if (min == 0) {
                        matcher.counters[counterIndex] = 0;
                        return cs.matches(matcher);
                    } else {
                        matcher.counters[counterIndex] = 1;
                        return operand2[0].matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                if (greedy) {
                    int ic = matcher.counters[counterIndex];

                    if (ic == max) return this.next.matches(matcher);

                    matcher.counters[counterIndex] = ic + 1;

                    if (ic < min) return operand2[0].matches(matcher);

                    int savedOffset = matcher.offset;

                    if (operand2[0].matches(matcher)) return true;

                    matcher.offset = savedOffset;

                    return this.next.matches(matcher);
                } else {
                    int ic = matcher.counters[counterIndex];

                    if (ic >= min) {
                        int savedOffset = matcher.offset;
                        if (this.next.matches(matcher)) return true;
                        matcher.offset = savedOffset;
                    }

                    if (++ic > max) return false;

                    matcher.counters[counterIndex] = ic;

                    return operand2[0].matches(matcher);
                }
            }

            @Override protected String
            toStringWithoutNext() { return "???cs"; }
        };

        operand2[0] = operand.concat(cs);

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {

                if (min == 0) {
                    matcher.counters[counterIndex] = 0;
                    return cs.matches(matcher);
                } else {
                    matcher.counters[counterIndex] = 1;
                    return operand2[0].matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o     = matcher.offset;
                int limit = matcher.regionEnd;

                if (limit - o > max) limit = o + max; // Beware of overflow!

                o += min;

                while (o <= limit) {

                    // Find next match of the infix withing the subject string.
                    o = this.indexOf.indexOf(matcher.subject, o, limit);
                    if (o == -1) break;

                    // See if the successor matches the rest of the subject.
                    matcher.offset = o + this.len;
                    if (this.next.matches(matcher)) return true;

                    // Successor didn't match, continue with next character position.
                    o++;
                }

                matcher.hitEnd = true;
                return false;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                MatcherImpl.End savedEnd = matcher.end;
                try {

                    matcher.end = MatcherImpl.End.ANY;

                    // The operand MUST match (min) times;
                    for (int i = 0; i < min; i++) {
                        if (!operand.matches(matcher)) return false;
                    }

                    // Now try to match the operand (max-min) more times.
                    int limit = max - min;

                    for (int i = 0; i < limit; i++) {
                        int o = matcher.offset;
                        if (!operand.matches(matcher)) {
                            matcher.offset = o;
                            break;
                        }
                    }
                } finally {
                    matcher.end = savedEnd;
                }

                return this.next.matches(matcher);
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

                        @Override public boolean
                        matches(MatcherImpl matcher) {

                            int o = matcher.offset;

                            if (max > matcher.regionEnd - o) {
                                o = matcher.regionEnd;
                            } else {
                                o += max;
                            }

                            matcher.offset = o;

                            return this.next.matches(matcher);
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

        @Override public boolean
        matches(MatcherImpl matcher) {
            return matcher.peekRead(this.s) && this.next.matches(matcher);
        }

        @Override public boolean
        find(MatcherImpl matcher) {

            int o = matcher.offset;

            while (o < matcher.regionEnd) {

                // Find the next occurrence of the literal string.
                o = this.indexOf.indexOf(matcher.subject, o, matcher.regionEnd - this.s.length());
                if (o == -1) break;

                // See if the rest of the pattern matches.
                matcher.offset = o + this.s.length();
                if (this.next.matches(matcher)) {
                    matcher.groups[0] = o;
                    matcher.groups[1] = matcher.offset;
                    return true;
                }

                // Rest of pattern didn't match; continue at the second character of the literal string match.
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                final int             savedOffset = matcher.offset;
                final int[]           savedGroups = matcher.groups;
                final MatcherImpl.End savedEnd    = matcher.end;

                matcher.end = MatcherImpl.End.ANY;

                for (int i = 0; i < alternatives.length; i++) {

                    if (alternatives[i].matches(matcher) && this.next.matches(matcher)) return true;

                    matcher.offset = savedOffset;
                    matcher.groups = savedGroups;
                }

                matcher.end = savedEnd;

                return false;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                final int             savedOffset = matcher.offset;
                final MatcherImpl.End savedEnd    = matcher.end;
                final int[]           savedGroups = matcher.groups;

                matcher.end = MatcherImpl.End.ANY;

                for (Sequence a : alternatives) {
                    if (a.matches(matcher)) return this.next.matches(matcher);

                    // Alternative did not match; restore original capturing groups.
                    matcher.groups = savedGroups;
                    matcher.offset = savedOffset;
                }

                matcher.end = savedEnd;

                return false;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int[] newGroups = Arrays.copyOf(matcher.groups, matcher.groups.length);
                newGroups[2 * groupNumber] = matcher.offset;

                final int[] savedGroups = matcher.groups;
                matcher.groups = newGroups;

                if (!this.next.matches(matcher)) {
                    matcher.groups = savedGroups;
                    return false;
                }

                // Verify that the successor chain contained an "end" for the same capturing group.
                assert matcher.groups[2 * groupNumber + 1] != -1;

                return true;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                matcher.groups[2 * groupNumber + 1] = matcher.offset;

                return this.next.matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int start = matcher.offset;
                if (!subsequence.matches(matcher)) return false;
                int end   = matcher.offset;

                // Copy "this.groups" and store group start and end.
                int[] gs = (matcher.groups = Arrays.copyOf(matcher.groups, matcher.groups.length));

                // Record the group start and end in the matcher.
                gs[2 * groupNumber]     = start;
                gs[2 * groupNumber + 1] = end;

                // Match the rest of the sequence.
                return this.next.matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int[] gs    = matcher.groups;
                int   start = gs[2 * groupNumber];
                int   end   = gs[2 * groupNumber + 1];

                // If the referenced group didn't match, then neither does this back reference.
                if (start == -1) return false;

                return matcher.peekRead(matcher.subject, start, end) && this.next.matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int[] gs    = matcher.groups;
                int   start = gs[2 * groupNumber];
                int   end   = gs[2 * groupNumber + 1];

                // If the referenced group didn't match, then neither does this back reference.
                if (start == -1) return false;

                return (
                    matcher.caseInsensitivePeekRead(matcher.subject.subSequence(start, end))
                    && this.next.matches(matcher)
                );
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int[] gs    = matcher.groups;
                int   start = gs[2 * groupNumber];
                int   end   = gs[2 * groupNumber + 1];

                // If the referenced group didn't match, then neither does this back reference.
                if (start == -1) return false;

                if (!matcher.unicodeCaseInsensitivePeekRead(matcher.subject.subSequence(start, end))) return false;

                return this.next.matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {
                return matcher.offset == matcher.anchoringRegionStart && this.next.matches(matcher);
            }

            // Override "AbstractSequence.find()" such that we give the match only one shot.
            @Override public boolean
            find(MatcherImpl matcher) {

                matcher.hitEnd = false;

                if (this.matches(matcher)) {
                    matcher.groups[0] = matcher.anchoringRegionStart;
                    matcher.groups[1] = matcher.offset;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (o == matcher.anchoringRegionStart) return this.next.matches(matcher);

                if (o == matcher.anchoringRegionEnd) {
                    matcher.hitEnd = true;
                    return false;
                }

                char c = matcher.subject.charAt(o - 1);
                return (
                    (c == '\r' && matcher.subject.charAt(o) != '\n')
                    || c == '\n'
                    || c == '\u000B'
                    || c == '\f'
                    || c == '\u0085'
                    || c == '\u2028'
                    || c == '\u2029'
                ) && this.next.matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (
                    o == matcher.anchoringRegionStart
                    || (matcher.subject.charAt(o - 1) == '\n' && o != matcher.anchoringRegionEnd)
                ) return this.next.matches(matcher);

                if (o == matcher.anchoringRegionEnd) matcher.hitEnd = true;

                return false;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (o >= matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher);
                }

                char c = matcher.subject.charAt(o);
                if (!(
                    (c <= 0x0d && c >= 0x0a)
                    || c == 0x85
                    || (c >= 0x2028 && c <= 0x2029)
                )) return false;

                if (o == matcher.anchoringRegionEnd - 1) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher);
                }

                if (
                    c == '\r'
                    && matcher.subject.charAt(o + 1) == '\n'
                    && o == matcher.anchoringRegionEnd - 2
                ) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher);
                }

                return false;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (o >= matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher);
                }

                if (
                    o == matcher.anchoringRegionEnd - 1
                    && matcher.subject.charAt(o) == '\n'
                ) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher);
                }

                return false;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                if (matcher.offset < matcher.anchoringRegionEnd) return false;

                matcher.hitEnd     = true;
                matcher.requireEnd = true;

                return this.next.matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (o == matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher);
                }

                char c = matcher.subject.charAt(o);
                return (
                    (c == '\n' && (
                        o == matcher.anchoringRegionStart
                        || Character.codePointBefore(matcher.subject, o) != '\r'
                    ))
                    || c == '\r'
                    || c == '\u000B'
                    || c == '\f'
                    || c == '\u0085'
                    || c == '\u2028'
                    || c == '\u2029'
                ) && this.next.matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (o == matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher);
                }

                return matcher.subject.charAt(o) == '\n' && this.next.matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (o >= matcher.regionEnd) return false;

                char c = matcher.subject.charAt(o);

                // Check for linebreak characters in a highly optimized manner.
                if (c <= 0x0d) {
                    if (
                        c == this.c1
                        && o < matcher.regionEnd - 1
                        && matcher.subject.charAt(o + 1) == this.c2
                    ) {
                        matcher.offset = o + 2;
                        return this.next.matches(matcher);
                    }
                    if (c >= 0x0a) {
                        matcher.offset = o + 1;
                        return this.next.matches(matcher);
                    }
                }

                if (c == 0x85 || c == 0x2028 || c == 0x2029) {
                    matcher.offset = o + 1;
                    return this.next.matches(matcher);
                }

                return false;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                // The "Non-spacing mark" character is sometimes a word character (iff its left neighbor is a word
                // character), and sometimes it is not (iff its left neighbor is a non-word character).

                if (o >= matcher.transparentRegionEnd) {

                    // At end of transparent region.
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return (
                        o != matcher.transparentRegionStart // Zero-length region.
                        && isWordCharacter.evaluate(Character.codePointBefore(matcher.subject, o))
                        && this.next.matches(matcher)
                    );
                }

                if (o <= matcher.transparentRegionStart) {

                    // At start of transparent region.
                    return (
                        isWordCharacter.evaluate(Character.codePointAt(matcher.subject, o))
                        && this.next.matches(matcher)
                    );
                }

                if (matcher.subject.charAt(o) == '\u030a') return false;

                // IN transparent region (neither at its start nor at its end).
                int cpBefore = Character.codePointBefore(matcher.subject, o);
                int cpAt     = Character.codePointAt(matcher.subject, o);

                if (cpBefore == '\u030a') {
                    for (int i = o - 1;; i--) {
                        if (i <= matcher.transparentRegionStart) {
                            if (!isWordCharacter.evaluate(cpAt)) return false;
                            break;
                        }

                        cpBefore = Character.codePointBefore(matcher.subject, i);
                        if (cpBefore != '\u030a') break;
                    }
                }
                return (
                    (isWordCharacter.evaluate(cpBefore) != isWordCharacter.evaluate(cpAt))
                    && this.next.matches(matcher)
                );
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                // The documentation of java.util.regex is totally unclear about the following case, but this seems to
                // be how it works:
                if (matcher.endOfPreviousMatch == -1) return this.next.matches(matcher);
//                if (matcher.endOfPreviousMatch == -1) return -1;

                return matcher.offset == matcher.endOfPreviousMatch && this.next.matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                boolean lookaheadMatches;

                final int             savedOffset    = matcher.offset;
                final MatcherImpl.End savedEnd       = matcher.end;
                final int             savedRegionEnd = matcher.regionEnd;
                {
                    matcher.end       = MatcherImpl.End.ANY;
                    matcher.regionEnd = matcher.transparentRegionEnd;

                    lookaheadMatches   = op.matches(matcher);
                    matcher.requireEnd = matcher.hitEnd;
                }
                matcher.end       = savedEnd;
                matcher.regionEnd = savedRegionEnd;

                if (!lookaheadMatches) return false;

                matcher.offset = savedOffset;
                return this.next.matches(matcher);
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                // In most cases, it is more efficient to FIRST check whether the rest of the pattern matches, and
                // only THEN check if the lookbehind matches.
                final int savedOffset1 = matcher.offset;
                if (!this.next.matches(matcher)) return false;
                final int savedOffset2 = matcher.offset;

                boolean lookbehindMatches;
                {
                    final MatcherImpl.End savedEnd         = matcher.end;
                    final boolean         savedHitEnd      = matcher.hitEnd;
                    final int             savedRegionStart = matcher.regionStart;
                    final int             savedRegionEnd   = matcher.regionEnd;
                    try {
                        matcher.end         = MatcherImpl.End.END_OF_REGION;
                        matcher.regionStart = matcher.transparentRegionStart;
                        matcher.regionEnd   = savedOffset1;
                        matcher.offset      = matcher.transparentRegionStart;
                        lookbehindMatches   = op.find(matcher);
                    } finally {
                        matcher.end         = savedEnd;
                        matcher.hitEnd      = savedHitEnd;
                        matcher.regionStart = savedRegionStart;
                        matcher.regionEnd   = savedRegionEnd;
                    }
                }

                if (!lookbehindMatches) return false;

                matcher.offset = savedOffset2;
                return true;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                // The operand MUST match (min) times;
                int i;
                for (i = 0; i < min; i++) {

                    if (o >= matcher.regionEnd) {
                        matcher.hitEnd = true;
                        return false;
                    }

                    int c = matcher.subject.charAt(o++);

                    // Special handling for UTF-16 surrogates.
                    if (Character.isHighSurrogate((char) c) && o < matcher.regionEnd) {
                        char c2 = matcher.subject.charAt(o);
                        if (Character.isLowSurrogate(c2)) {
                            c = Character.toCodePoint((char) c, c2);
                            o++;
                        }
                    }

                    matcher.offset = o;
                    if (!operand.matches(c)) return false;
                }
                final int offsetAfterMin = o;

                // Now try to match the operand (max-min) more times.
                for (; i < max; i++) {

                    if (o >= matcher.regionEnd) {
                        matcher.hitEnd = true;
                        break;
                    }

                    int offset2 = o;
                    int c       = matcher.subject.charAt(offset2++);

                    // Special handling for UTF-16 surrogates.
                    if (Character.isHighSurrogate((char) c) && offset2 < matcher.regionEnd) {
                        char c2 = matcher.subject.charAt(offset2);
                        if (Character.isLowSurrogate(c2)) {
                            c = Character.toCodePoint((char) c, c2);
                            offset2++;
                        }
                    }

                    matcher.offset = offset2;
                    if (!operand.matches(c)) break;

                    o = offset2;
                }

                for (;; i--) {

                    matcher.offset = o;
                    if (this.next.matches(matcher)) return true;

                    if (i == min) break;

                    if (
                        Character.isLowSurrogate(matcher.subject.charAt(--o))
                        && o > offsetAfterMin
                        && Character.isHighSurrogate(matcher.subject.charAt(o - 1))
                    ) o--;
                }

                return false;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o         = matcher.offset;
                int fromIndex = matcher.regionEnd - this.infixLength;

                if (fromIndex - o > max) fromIndex = o + max; // Beware of overflow!

                int toIndex = o + min;

                matcher.hitEnd = true;

                while (fromIndex >= toIndex) {

                    // Find next match of the infix withing the subject string.
                    fromIndex = this.indexOf.indexOf(matcher.subject, fromIndex, toIndex);
                    if (fromIndex == -1) return false;

                    // See if the successor matches the rest of the subject.
                    matcher.offset = fromIndex + this.infixLength;
                    if (this.next.matches(matcher)) return true;

                    // Successor didn't match, continue with next character position.
                    fromIndex--;
                }

                return false;
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

            @Override public boolean
            matches(MatcherImpl matcher) {

                boolean operandMatches;

                final int       savedOffset = matcher.offset;
                MatcherImpl.End savedEnd    = matcher.end;
                {
                    matcher.end = MatcherImpl.End.ANY;

                    operandMatches = op.matches(matcher);
                }
                matcher.end = savedEnd;

                if (operandMatches) return false;

                matcher.offset = savedOffset;
                return this.next.matches(matcher);
            }

            @Override public String
            toStringWithoutNext() { return "negate(" + op + ")"; }
        };
    }


    private static String
    maxToString(int n) { return n == Integer.MAX_VALUE ? "infinite" : Integer.toString(n); }
}
