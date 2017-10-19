
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
import java.util.HashSet;
import java.util.Set;

import de.unkrig.commons.lang.CharSequences;
import de.unkrig.commons.lang.Characters;
import de.unkrig.commons.lang.PrettyPrinter;
import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.StringUtil.IndexOf;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.ArrayUtil;

/**
 * Methods that create all kinds of {@link Sequence}s.
 */
public final
class Sequences {

    private Sequences() {}

    enum QuantifierNature { GREEDY, RELUCTANT, POSSESSIVE } // SUPPRESS CHECKSTYLE JavadocVariable

    /**
     * Matches depending on the {@link MatcherImpl#offset} and the value of {@link MatcherImpl#end}.
     */
    public static final Sequence
    TERMINAL = new Sequence(0, 0) {

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
     * A sequence of fixed length, where all elements are {@link MultivalentCharClass}es, i.e. a set of BMP (one-char)
     * code points. These sets of chars are also called "the needle", as a metaphor for the needle that is to be found
     * in a haystack.
     *
     * @see #getNeedle()
     */
    abstract static
    class AbstractMultivalentSequence extends CompositeSequence implements MultivalentSequence {

        AbstractMultivalentSequence(int matchLengthWithoutNext) {
            super(matchLengthWithoutNext);
        }
    }

    /**
     * Constructs a sequence from a <var>needle</var>, and implements lots of optimizations.
     */
    static Sequence
    multivalentSequence(final char[][] needle) {

        if (needle.length == 0) return Sequences.TERMINAL;

        if (needle.length == 1) {

            Set<Integer> chars = new HashSet<Integer>(needle[0].length);
            for (char c : needle[0]) chars.add((int) c);

            return CharacterClasses.oneOfManyChars(chars);
        }

        // Optimize the special case where all "needle[n].length == 1".
        UNIVALENT: {
            for (char[] n : needle) {
                if (n.length != 1) break UNIVALENT;
            }

            return new LiteralString(new String(ArrayUtil.mirror(needle)[0]));
        }

        // Now we have the general case where "needle[n]" have ANY length.

        class MyMultivalentSequence extends CompositeSequence implements MultivalentSequence {

            final StringUtil.IndexOf indexOf = StringUtil.indexOf(needle);

            MyMultivalentSequence() { super(needle.length); }

            @Override public char[][]
            getNeedle() { return needle; }

            @Override boolean
            matches(MatcherImpl matcher) {

                if (matcher.offset + needle.length > matcher.regionEnd) {

                    // There are not enough characters left in the region for a match; check whether there is a
                    // _partial_ patch, and, if so, set "hitEnd" before returning FALSE.
                    for (int o = matcher.offset, i = 0; o < matcher.regionEnd; o++, i++) {
                        char   c = matcher.subject.charAt(o);
                        char[] n = needle[i];

                        NC: {
                            for (char nc : n) {
                                if (c == nc) break NC;
                            }
                            return false;
                        }
                    }
                    matcher.hitEnd = true;
                    return false;
                }

                for (char[] n : needle) {
                    char c = matcher.subject.charAt(matcher.offset++);
                    NC: {
                        for (char nc : n) {
                            if (c == nc) break NC;
                        }
                        return false;
                    }
                }
                return true;
            }

            @Override public int
            find(MatcherImpl matcher) {

                final int maxIndex = matcher.regionEnd - needle.length;

                for (int o = matcher.offset;;) {

                    o = this.indexOf.indexOf(matcher.subject, o, maxIndex);
                    if (o == -1) {
                        matcher.hitEnd = true; // TODO incorrect!
                        return -1;
                    }

                    matcher.offset = o + needle.length;
                    if (this.next.matches(matcher)) return o;

                    o++;
                }
            }

            @Override protected String
            toStringWithoutNext() { return this.indexOf.toString(); }
        }

        return new MyMultivalentSequence();
    }

    /**
     * Implements quantifiers (greedy, reluctant and possessive).
     *
     * @param min The "minimum count" of the quantifier
     * @param max The "maximum count" of the quantifier (may be {@link Integer#MAX_VALUE})
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
                    new Sequences.LiteralString(StringUtil.repeat(min, Character.toChars(lc.c)))
                    .concat(Sequences.quantifier(lc, 0, max - min, counterIndex, nature))
                );
            }
        }

        // Optimize "(?:abc){3,5}" into "abcabcabc(?:abc){0,2}".
        if (min >= 2 && operand instanceof Sequences.LiteralString) {
            Sequences.LiteralString ls = (Sequences.LiteralString) operand;
            if (ls.next == Sequences.TERMINAL && min * ls.cs.length() <= 1000) {
                return (
                    new Sequences.LiteralString(StringUtil.repeat(min, ls.cs))
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
            if (operand instanceof CharacterClass) {
                CharacterClass cc = (CharacterClass) operand;
                if (cc.next == Sequences.TERMINAL) {
                    return Sequences.reluctantQuantifierOnCharacterClass(cc, min, max);
                }
            }
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

        final int minml = Sequences.mul(min, operand.minMatchLength);
        final int maxml = Sequences.mul(max, operand.maxMatchLength);

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

            final CompositeSequence cs = new CompositeSequence(0, Integer.MAX_VALUE) {

                @Override public boolean
                matches(MatcherImpl matcher) {

                    // Optimize: TODO: Probably this optimization is not possible
                    if (matcher.regionEnd - matcher.offset < this.next.minMatchLength) return false;

                    int savedOffset = matcher.offset;

                    if (savedOffset == matcher.regionEnd) matcher.hitEnd = true;

                    if (greedy) {

                        if (operand2[0].matches(matcher)) return true;

                        matcher.offset = savedOffset;

                        return this.next.matches(matcher);
                    } else {

                        if (this.next.matches(matcher)) return true;

                        matcher.offset = savedOffset;

                        return operand2[0].matches(matcher);
                    }
                }

                @Override protected String
                toStringWithoutNext() { return "???cs"; }
            };

            operand2[0] = operand.concat(cs);

            return new Sequence(minml, maxml) {

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

                    cs.concat(that);

                    this.minMatchLength = Sequences.add(minml, that.minMatchLength);
                    this.maxMatchLength = Sequences.add(maxml, that.maxMatchLength);

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

        // Here we have the "general case", where min and max can have any value.

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

        final CompositeSequence cs = new CompositeSequence(
            Sequences.mul(operand.minMatchLength, min == 0 ? 0   : min - 1), // minMatchLength
            Sequences.mul(operand.maxMatchLength, min == 0 ? max : max - 1)  // maxMatchLength
        ) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                if (greedy) {

//                    // Optimize: TODO: Probably not correct
//                    if (matcher.regionEnd - matcher.offset < this.next.minMatchLength) return false;

                    int ic = matcher.counters[counterIndex];

                    if (ic == max) return this.next.matches(matcher);

                    matcher.counters[counterIndex] = ic + 1;
                    {
                        if (ic < min) return operand2[0].matches(matcher);

                        int savedOffset = matcher.offset;
                        {
                            if (operand2[0].matches(matcher)) return true;
                        }
                        matcher.offset = savedOffset;
                    }
                    matcher.counters[counterIndex] = ic;

                    return this.next.matches(matcher);
                } else {
                    int ic = matcher.counters[counterIndex];

                    if (ic >= min) {
                        int savedOffset = matcher.offset;
                        {
                            if (this.next.matches(matcher)) return true;
                        }
                        matcher.offset = savedOffset;
                    }

                    if (ic >= max) return false;

                    matcher.counters[counterIndex] = ic + 1;
                    {
                        if (operand2[0].matches(matcher)) return true;
                    }

                    matcher.counters[counterIndex] = ic;
                    return false;
                }
            }

            @Override protected String
            toStringWithoutNext() { return "???cs"; }
        };

        operand2[0] = operand.concat(cs);

        return new Sequence(minml, maxml) {

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
                        ? Sequences.greedyQuantifierOnAnyCharAndLiteralString(min, max, ls.cs)
                        : Sequences.reluctantQuantifierOnAnyCharAndLiteralString(min, max, ls.cs)
                    ).concat(ls.next);
                }

                cs.concat(that);

                this.minMatchLength = Sequences.add(minml, that.minMatchLength);
                this.maxMatchLength = Sequences.add(maxml, that.maxMatchLength);

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
     *
     * @param min The "minimum count" of the quantifier
     * @param max The "maximum count" of the quantifier (may be {@link Integer#MAX_VALUE})
     */
    public static CompositeSequence
    reluctantQuantifierOnAnyCharAndLiteralString(final int min, final int max, final CharSequence ls) {

        return new CompositeSequence(
            Sequences.add(min, ls.length()),
            Sequences.add(max, ls.length())
        ) {

            final int     len     = ls.length();
            final IndexOf indexOf = StringUtil.indexOf(ls);

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
                    "reluctantQuantifierOnAnyCharAndLiteralString(min="
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
     *
     * @param min The "minimum count" of the quantifier
     * @param max The "maximum count" of the quantifier (may be {@link Integer#MAX_VALUE})
     */
    private static Sequence
    possessiveQuantifier(final Sequence operand, final int min, final int max) {

        return new CompositeSequence(
            Sequences.mul(min, operand.minMatchLength),
            Sequences.mul(max, operand.maxMatchLength)
        ) {

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

                Sequence result = super.concat(that);

                // Optimize for "." operand.
                if (
                    operand instanceof CharacterClasses.AnyCharacter
                    && ((CompositeSequence) operand).next == Sequences.TERMINAL
                ) {

                    // Replace the possessive quantifier element.
                    return new CompositeSequence(
                        Sequences.add(Sequences.mul(min, 1), result.minMatchLength),
                        Sequences.add(Sequences.mul(max, 2), result.maxMatchLength)
                    ) {

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
    class LiteralString extends CompositeSequence implements MultivalentSequence {

        final CharSequence               cs;
        private final StringUtil.IndexOf indexOf;

        /**
         * @param cs The literal string that this sequence represents
         */
        LiteralString(CharSequence cs) {
            super(cs.length());
            this.cs      = cs;
            this.indexOf = StringUtil.indexOf(this.cs);
        }

        @Override public char[][]
        getNeedle() { return ArrayUtil.mirror(new char[][] { CharSequences.toCharArray(this.cs) }); }

        @Override public boolean
        matches(MatcherImpl matcher) {
            return matcher.peekRead(this.cs) && this.next.matches(matcher);
        }

        @Override public int
        find(MatcherImpl matcher) {

            int o = matcher.offset;

            while (o < matcher.regionEnd) {

                // Find the next occurrence of the literal string.
                o = this.indexOf.indexOf(matcher.subject, o, matcher.regionEnd - this.cs.length());
                if (o == -1) break;

                // See if the rest of the pattern matches.
                matcher.offset = o + this.cs.length();
                if (this.next.matches(matcher)) return o;

                // Rest of pattern didn't match; continue at the second character of the literal string match.
                o++;
            }

            matcher.hitEnd = true;
            return -1;
        }

        @Override public String
        toStringWithoutNext() { return this.indexOf.toString(); }
    }

    /**
     * Creates and returns {@link Sequence} that returns the <em>first</em> match of one <var>alternatives</var> plus
     * <em>this</em> sequence's successor.
     * For example, {@code "a(b|bb)c"} will match both {@code "abc"} and {@code "abbc"}. (In the second case, matching
     * {@code "abc"} fails, but matching {@code "abbc"} eventually succeeds.)
     *
     * @param firstSubsequentGroup The number of the first capturing group that starts <em>within</em> or
     *                             <em>after</em> the alternatives
     * @see                        #independentNonCapturingGroup(Sequence[])
     */
    public static Sequence
    alternatives(final Sequence[] alternatives, final int firstSubsequentGroup) {

        if (alternatives.length == 0) return Sequences.TERMINAL;

        if (alternatives.length == 1) return alternatives[0];

        int minMlw = Integer.MAX_VALUE, maxMlw = 0;
        for (Sequence a : alternatives) {
            if (a.minMatchLength < minMlw) minMlw = a.minMatchLength;
            if (a.maxMatchLength > maxMlw) maxMlw = a.maxMatchLength;
        }

        // Optimize the case where all alternatives are multivalent sequences.
        OPTIMIZE_MULTIVALENT_ALTERNATIVES: {

            final char[][][] needles = new char[alternatives.length][][];
            for (int i = 0; i < alternatives.length; i++) {
                Sequence a = alternatives[i];

                if (!(a instanceof MultivalentSequence)) break OPTIMIZE_MULTIVALENT_ALTERNATIVES;

                MultivalentSequence ms = (MultivalentSequence) a;

                if (((CompositeSequence) ms).next != Sequences.TERMINAL) break OPTIMIZE_MULTIVALENT_ALTERNATIVES;

                needles[i] = ms.getNeedle();
            }

            final int minMlwm1 = minMlw - 1;

            // Compute the "safe skip" array.
            final int[] safeSkip = new int[256];
            Arrays.fill(safeSkip, minMlw);
            for (char[][] n : needles) {
                for (int j = 0; j < minMlw; j++) {
                    for (char c : n[j]) safeSkip[0xff & c] = minMlwm1 - j;
                }
            }

            return new CompositeSequence(minMlw, maxMlw) {

                @Override public boolean
                matches(MatcherImpl matcher) {

                    final int             savedOffset = matcher.offset;
                    final MatcherImpl.End savedEnd    = matcher.end;

                    for (int i = 0; i < alternatives.length; i++) {

                        matcher.end = MatcherImpl.End.ANY;
                        boolean am = alternatives[i].matches(matcher);
                        matcher.end = savedEnd;

                        if (am && this.next.matches(matcher)) return true;

                        // Restore offset and captured groups.
                        matcher.offset = savedOffset;
                        Arrays.fill(matcher.groups, 2 * firstSubsequentGroup, matcher.groups.length, -1);
                    }

                    return false;
                }

                @Override public int
                find(MatcherImpl matcher) {

                    for (int o = matcher.offset + minMlwm1; o < matcher.regionEnd;) {

                        int ss = safeSkip[0xff & matcher.subject.charAt(o)];

                        if (ss > 0) {
                            o += ss;
                        } else {

                            // Check which needles match.
                            NEEDLES:
                            for (char[][] n : needles) {

                                for (int i = 0; i < n.length; i++) {
                                    char c = matcher.subject.charAt(o - minMlwm1 + i);
                                    MULTI: {
                                        for (char c2 : n[i]) {
                                            if (c2 == c) break MULTI;
                                        }
                                        continue NEEDLES;
                                    }
                                }

                                // Needle matches!
                                matcher.offset = o - minMlwm1 + n.length;
                                if (this.next.matches(matcher)) return o - minMlwm1;
                            }

                            // None of the needles match; continue at next char position.
                            o++;
                        }
                    }

                    matcher.hitEnd = true;
                    return -1;
                }

                @Override protected String
                toStringWithoutNext() {
                    return "boyerMooreHorspoolAlternatives(" + PrettyPrinter.toJavaArrayInitializer(needles) + ")";
                }
            };
        }

        return new CompositeSequence(minMlw, maxMlw) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                final int             savedOffset = matcher.offset;
                final MatcherImpl.End savedEnd    = matcher.end;

                for (int i = 0; i < alternatives.length; i++) {

                    matcher.end = MatcherImpl.End.ANY;
                    boolean am = alternatives[i].matches(matcher);
                    matcher.end = savedEnd;

                    if (am && this.next.matches(matcher)) return true;

                    // Restore offset and captured groups.
                    matcher.offset = savedOffset;
                    Arrays.fill(matcher.groups, 2 * firstSubsequentGroup, matcher.groups.length, -1);
                }

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

        return new CompositeSequence(0, Integer.MAX_VALUE) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                final MatcherImpl.End savedEnd    = matcher.end;
                {
                    matcher.end = MatcherImpl.End.ANY;

                    final int savedOffset = matcher.offset;

                    for (Sequence a : alternatives) {
                        if (a.matches(matcher)) return this.next.matches(matcher);

                        // Alternative did not match; restore offset and try next alternative.
                        matcher.offset = savedOffset;
                    }
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

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int[] gs              = matcher.groups;
                int   idx             = 2 * groupNumber;

                final int savedGroupStart = gs[idx];
                final int savedGroupEnd   = gs[idx + 1];
                gs[idx] = matcher.offset;


                // The following logic is (not only a bit...) strange, but that's how JUR's capturing groups
                // behave:
                if (!this.next.matches(matcher)) {
                    gs[idx]     = savedGroupStart;
                    gs[idx + 1] = savedGroupEnd;
                    return false;
                }

                // Verify that the successor chain contained an "end" for the same capturing group.
                assert gs[idx + 1] != -1;

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

        return new CompositeSequence(0) {

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

        return new CompositeSequence(subsequence.minMatchLength, subsequence.maxMatchLength) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int start = matcher.offset;
                if (!subsequence.matches(matcher)) return false;
                int end   = matcher.offset;

                // Copy "this.groups" and store group start and end.
                int[] gs = matcher.groups;

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

        return new CompositeSequence(0, Integer.MAX_VALUE) {

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

        return new CompositeSequence(0, Integer.MAX_VALUE) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int[] gs    = matcher.groups;
                int   start = gs[2 * groupNumber];
                int   end   = gs[2 * groupNumber + 1];

                // If the referenced group didn't match, then neither does this back reference.
                if (start == -1) return false;

                return matcher.caseInsensitivePeekRead(matcher.subject, start, end) && this.next.matches(matcher);
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

        return new CompositeSequence(0, Integer.MAX_VALUE) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int[] gs    = matcher.groups;
                int   start = gs[2 * groupNumber];
                int   end   = gs[2 * groupNumber + 1];

                // If the referenced group didn't match, then neither does this back reference.
                if (start == -1) return false;

                if (!matcher.unicodeCaseInsensitivePeekRead(matcher.subject, start, end)) return false;

                return this.next.matches(matcher);
            }

            @Override public String
            toStringWithoutNext() { return "caseInsensitiveCapturingGroupBackreference(" + groupNumber + ")"; }
        };
    }

    /**
     * Implements {@code "^"} with !{@link de.unkrig.ref4j.Pattern#MULTILINE}, and {@code "\A"}.
     */
    public static Sequence
    beginningOfInput() {

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {
                return (
                    matcher.offset == (matcher.hasAnchoringBounds() ? matcher.regionStart : 0)
                    && this.next.matches(matcher)
                );
            }

            @Override public Sequence
            concat(Sequence that) {

                Sequence result = super.concat(that);

                // Set "minMatchLength" to 0, which enforces that "find()" (below) is always invoked, and has a chance
                // to set "hitEnd = false".
                this.minMatchLength = 0;

                return result;
            }

            // Override "AbstractSequence.find()" such that we give the match only one shot.
            @Override public int
            find(MatcherImpl matcher) {

                int savedOffset = matcher.offset;

                if (this.matches(matcher)) return savedOffset;

                return -1;
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

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (o == (matcher.hasAnchoringBounds() ? matcher.regionStart : 0)) return this.next.matches(matcher);

                if (o == (matcher.hasAnchoringBounds() ? matcher.regionEnd : matcher.subject.length())) {
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

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (
                    o == (matcher.hasAnchoringBounds() ? matcher.regionStart : 0)
                    || (
                        matcher.subject.charAt(o - 1) == '\n'
                        && o != (matcher.hasAnchoringBounds() ? matcher.regionEnd : matcher.subject.length())
                    )
                ) return this.next.matches(matcher);

                if (o == (matcher.hasAnchoringBounds() ? matcher.regionEnd : matcher.subject.length())) {
                    matcher.hitEnd = true;
                }

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

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                int are = matcher.hasAnchoringBounds() ? matcher.regionEnd : matcher.subject.length();
                if (o >= are) {
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

                if (o == are - 1) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher);
                }

                if (
                    c == '\r'
                    && matcher.subject.charAt(o + 1) == '\n'
                    && o == are - 2
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

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o   = matcher.offset;
                int are = matcher.hasAnchoringBounds() ? matcher.regionEnd : matcher.subject.length();

                if (o >= are) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher);
                }

                if (
                    o == are - 1
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

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                // No anchoring bound, no match.
                if (!matcher.hasAnchoringBounds()) return false;

                // Are we at the end of the region?
                if (matcher.offset < matcher.regionEnd) return false;

                // Yes.
                matcher.hitEnd     = true;
                matcher.requireEnd = true;

                return this.next.matches(matcher);
            }

            @Override public String
            toStringWithoutNext() { return "endOfInput"; }
        };
    }

    /**
     * Implements {@code "$"} with {@link de.unkrig.ref4j.Pattern#MULTILINE}.
     */
    public static Sequence
    endOfLine() {

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (o == (matcher.hasAnchoringBounds() ? matcher.regionEnd : matcher.subject.length())) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.next.matches(matcher);
                }

                char c = matcher.subject.charAt(o);
                return (
                    (c == '\n' && (
                        o == (matcher.hasAnchoringBounds() ? matcher.regionStart : 0)
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
     * Implements {@code "$"} with {@link de.unkrig.ref4j.Pattern#MULTILINE} and {@link
     * de.unkrig.ref4j.Pattern#UNIX_LINES}.
     */
    public static Sequence
    endOfUnixLine() {

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (matcher.hasAnchoringBounds() && o == matcher.regionEnd) {
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

        return new CompositeSequence(1, 2) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                if (o >= matcher.regionEnd) {
                    matcher.hitEnd = true;
                    return false;
                }

                char c = matcher.subject.charAt(o);

                // Check for linebreak characters in a highly optimized manner.
                if (c <= 0x0d) {
                    if (
                        c == '\r'
                        && o < matcher.regionEnd - 1
                        && matcher.subject.charAt(o + 1) == '\n'
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
     * <p>
     *   Notice that, for compatibility with JUR, this is independant of the {@link
     *   de.unkrig.ref4j.Pattern#UNICODE_CHARACTER_CLASS}flag!
     * </p>
     */
    public static Sequence
    wordBoundary() {

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                // The "Non-spacing mark" character is sometimes a word character (iff its left neighbor is a word
                // character), and sometimes it is not (iff its left neighbor is a non-word character).

                int trs, tre;
                if (matcher.hasTransparentBounds()) {
                    trs = 0;
                    tre = matcher.subject.length();
                } else {
                    trs = matcher.regionStart;
                    tre = matcher.regionEnd;
                }

                if (o >= tre) {

                    // At end of transparent region.
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return (
                        o != trs // Zero-length region.
                        && Characters.isUnicodeWord(Character.codePointBefore(matcher.subject, o))
                        && this.next.matches(matcher)
                    );
                }

                if (o <= trs) {

                    // At start of transparent region.
                    return (
                        Characters.isUnicodeWord(Character.codePointAt(matcher.subject, o))
                        && this.next.matches(matcher)
                    );
                }

                if (matcher.subject.charAt(o) == '\u030a') return false;

                // IN transparent region (neither at its start nor at its end).
                int cpBefore = Character.codePointBefore(matcher.subject, o);
                int cpAt     = Character.codePointAt(matcher.subject, o);

                if (cpBefore == '\u030a') {
                    for (int i = o - 1;; i--) {
                        if (i <= trs) {
                            if (!Characters.isUnicodeWord(cpAt)) return false;
                            break;
                        }

                        cpBefore = Character.codePointBefore(matcher.subject, i);
                        if (cpBefore != '\u030a') break;
                    }
                }
                return (
                    (Characters.isUnicodeWord(cpBefore) != Characters.isUnicodeWord(cpAt))
                    && this.next.matches(matcher)
                );
            }

            @Override public String
            toStringWithoutNext() { return "wordBoundary"; }
        };
    }

    /**
     * Implements {@code "\G"}.
     */
    public static Sequence
    endOfPreviousMatch() {

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                // The documentation of java.util.regex is totally unclear about the following case, but this seems to
                // be how it works:
                if (matcher.endOfPreviousMatch < 0) return this.next.matches(matcher);
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

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                boolean lookaheadMatches;

                final int             savedOffset    = matcher.offset;
                final MatcherImpl.End savedEnd       = matcher.end;
                final int             savedRegionEnd = matcher.regionEnd;
                {
                    matcher.end = MatcherImpl.End.ANY;

                    if (matcher.hasTransparentBounds()) matcher.regionEnd = matcher.subject.length();

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

        return new CompositeSequence(0) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                final int savedOffset = matcher.offset;
                int       start       = matcher.hasTransparentBounds() ? 0 : matcher.regionStart;

                // Check whether there enough chars between the transparent region's start and the current offset.
                if (op.minMatchLength > savedOffset - start) return false;

                // Optimize: Start the matching as far to the right as possible.
                int opmaxml = op.maxMatchLength;
                if (opmaxml < savedOffset - start) start = savedOffset - opmaxml;

                {
                    final MatcherImpl.End savedEnd                  = matcher.end;
                    final boolean         savedHitEnd               = matcher.hitEnd;
                    final int             savedRegionEnd            = matcher.regionEnd;
                    final boolean         savedHasTransparentBounds = matcher.hasTransparentBounds;
                    try {

                        matcher.end         = MatcherImpl.End.END_OF_REGION;
                        matcher.regionEnd   = savedOffset;
                        matcher.offset      = start;
                        matcher.useTransparentBounds(true);

                        if (op.find(matcher) < 0) return false;
                    } finally {
                        matcher.end                  = savedEnd;
                        matcher.hitEnd               = savedHitEnd;
                        matcher.regionEnd            = savedRegionEnd;
                        matcher.hasTransparentBounds = savedHasTransparentBounds;
                    }
                }

                matcher.offset = savedOffset;

                return this.next.matches(matcher);
            }

            @Override public String
            toStringWithoutNext() { return "positiveLookbehind(" + op + ")"; }
        };
    }

    /**
     * Optimized version of {@link #greedyOrReluctantQuantifier(Sequence, int, int, int, boolean)} when the operand is
     * a bare character class.
     * <p>
     *   This method, opposed to {@link #greedyOrReluctantQuantifier(Sequence, int, int, int, boolean)}, uses iteration
     *   instead of recursion for backtracking and thus saves a considerable number of method calls and call stack
     *   depth.
     * </p>
     *
     * @param min The "minimum count" of the quantifier
     * @param max The "maximum count" of the quantifier (may be {@link Integer#MAX_VALUE})
     */
    public static Sequence
    greedyQuantifierOnCharacterClass(final CharacterClass operand, final int min, final int max) {

        if (operand instanceof CharacterClasses.LiteralChar) {
            return Sequences.greedyQuantifierOnChar((char) ((CharacterClasses.LiteralChar) operand).c, min, max);
        }

        return new CompositeSequence(min * operand.minMatchLength, Sequences.mul(max, operand.maxMatchLength)) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                int limit = matcher.regionEnd; // TODO - this.next.minMatchLength;

                // The operand MUST match (min) times;
                int i;
                for (i = 0; i < min; i++) {

                    if (o >= limit) {
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

                    if (o >= limit) {
                        matcher.hitEnd = true;
                        break;
                    }

                    int o2 = o;

                    int c = matcher.subject.charAt(o2++);

                    // Special handling for UTF-16 surrogates.
                    if (Character.isHighSurrogate((char) c) && o2 < matcher.regionEnd) {
                        char c2 = matcher.subject.charAt(o2);
                        if (Character.isLowSurrogate(c2)) {
                            c = Character.toCodePoint((char) c, c2);
                            o2++;
                        }
                    }

                    if (!operand.matches(c)) break;

                    o = o2;
                }

                // Now track back to the longest possible match.
                for (;; i--) {

                    matcher.offset = o;
                    if (this.next.matches(matcher)) return true;

                    if (i <= min) break;

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
                    return Sequences.greedyQuantifierOnAnyCharAndLiteralString(min, Integer.MAX_VALUE, ls.cs);
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
     * Optimized version of {@link #greedyOrReluctantQuantifier(Sequence, int, int, int, boolean)} when the operand is
     * a bare (basic plane) {@code char}.
     * <p>
     *   This method, opposed to {@link #greedyOrReluctantQuantifier(Sequence, int, int, int, boolean)}, uses iteration
     *   instead of recursion for backtracking and thus saves a considerable number of method calls and call stack
     *   depth.
     * </p>
     *
     * @param min The "minimum count" of the quantifier
     * @param max The "maximum count" of the quantifier (may be {@link Integer#MAX_VALUE})
     */
    public static Sequence
    greedyQuantifierOnChar(final char operand, final int min, final int max) {

        return new CompositeSequence(min, max) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o = matcher.offset;

                // The char MUST match (min) times;
                int i;
                for (i = 0; i < min; i++) {

                    if (o >= matcher.regionEnd) {
                        matcher.hitEnd = true;
                        return false;
                    }

                    char c = matcher.subject.charAt(o++);

                    matcher.offset = o;
                    if (operand != c) return false;
                }

                // Now try to match the char (max-min) more times.
                for (; i < max; i++) {

                    if (o >= matcher.regionEnd) {
                        matcher.hitEnd = true;
                        break;
                    }

                    if (matcher.subject.charAt(o) != operand) break;

                    o++;
                }

                // Now track back to the longest possible match.
                for (;; i--) {

                    matcher.offset = o;
                    if (this.next.matches(matcher)) return true;

                    if (i <= min) break;

                    o--;
                }

                return false;
            }

            @Override public String
            toStringWithoutNext() {
                return (
                    "greedyQuantifierOnChar(operand='"
                    + operand
                    + "', min="
                    + min
                    + ", max="
                    + Sequences.maxToString(max)
                    + ")"
                );
            }
        };
    }

    /**
     * Optimized version of {@link #greedyOrReluctantQuantifier(Sequence, int, int, int, boolean)} when the operand is
     * a bare character class.
     * <p>
     *   This method, opposed to {@link #greedyOrReluctantQuantifier(Sequence, int, int, int, boolean)}, uses iteration
     *   instead of recursion for backtracking and thus saves a considerable number of method calls and call stack
     *   depth.
     * </p>
     *
     * @param min The "minimum count" of the quantifier
     * @param max The "maximum count" of the quantifier (may be {@link Integer#MAX_VALUE})
     */
    public static Sequence
    reluctantQuantifierOnCharacterClass(final CharacterClass operand, final int min, final int max) {

        return new CompositeSequence(min * operand.minMatchLength, Sequences.mul(max, operand.maxMatchLength)) {

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

                    int cp = matcher.subject.charAt(o++);

                    // Special handling for UTF-16 surrogates.
                    if (Character.isHighSurrogate((char) cp) && o < matcher.regionEnd) {
                        char c2 = matcher.subject.charAt(o);
                        if (Character.isLowSurrogate(c2)) {
                            cp = Character.toCodePoint((char) cp, c2);
                            o++;
                        }
                    }

                    matcher.offset = o;
                    if (!operand.matches(cp)) return false;
                }

                // Now try to match the operand (max-min) more times.
                for (;; i++) {

                    if (this.next.matches(matcher)) return true;

                    if (i == max) return false;

                    if (o >= matcher.regionEnd) {
                        matcher.hitEnd = true;
                        return false;
                    }

                    int cp = matcher.subject.charAt(o++);
                    if (
                        Character.isHighSurrogate((char) cp)
                        && o < matcher.regionEnd
                    ) {
                        char c2 = matcher.subject.charAt(o);
                        if (Character.isLowSurrogate(c2)) {
                            cp = Character.toCodePoint((char) cp, c2);
                            o++;
                        }
                    }

                    matcher.offset = o;
                    if (!operand.matches(cp)) return false;
                }
            }

            @Override public Sequence
            concat(Sequence that) {

                // Optimize the special case ".{min,}?literalstring".
                if (
                    operand instanceof CharacterClasses.AnyCharacter
                    && ((CompositeSequence) operand).next == Sequences.TERMINAL
                    && that instanceof LiteralString
                ) {
                    LiteralString ls = (LiteralString) that;
                    return Sequences.reluctantQuantifierOnAnyCharAndLiteralString(min, Integer.MAX_VALUE, ls.cs);
                }

                return super.concat(that);
            }

            @Override public String
            toStringWithoutNext() {
                return (
                    "reluctantQuantifierOnCharacterClass(operand="
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
     * Optimized version of {@link #reluctantQuantifierOnCharacterClass(CharacterClass, int, int)} when the operand is
     * a bare (basic plane) {@code char}.
     * <p>
     *   This method, opposed to {@link #greedyOrReluctantQuantifier(Sequence, int, int, int, boolean)}, uses iteration
     *   instead of recursion for backtracking and thus saves a considerable number of method calls and call stack
     *   depth.
     * </p>
     *
     * @param min The "minimum count" of the quantifier
     * @param max The "maximum count" of the quantifier (may be {@link Integer#MAX_VALUE})
     */
    public static Sequence
    reluctantQuantifierOnChar(final char operand, final int min, final int max) {

        return new CompositeSequence(min, max) {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o     = matcher.offset;
                int limit = matcher.regionEnd - this.next.minMatchLength;

                // The operand MUST match (min) times;
                int i;
                for (i = 0; i < min; i++) {

                    if (o >= limit) {
                        matcher.hitEnd = true;
                        return false;
                    }

                    char c = matcher.subject.charAt(o++);

                    if (c != operand) return false;
                }

                // Now try to match the operand (max-min) more times.
                for (;; i++) {

                    if (o >= limit) {
                        matcher.hitEnd = true;
                        return false;
                    }

                    matcher.offset = o;
                    if (this.next.matches(matcher)) return true;

                    if (i == max) break;

                    o++;
                }

                return false;
            }

            @Override public String
            toStringWithoutNext() {
                return (
                    "reluctantQuantifierOnChar(operand='"
                    + operand
                    + "', min="
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
     *
     * @param min The "minimum count" of the quantifier
     * @param max The "maximum count" of the quantifier (may be {@link Integer#MAX_VALUE})
     */
    public static Sequence
    greedyQuantifierOnAnyCharAndLiteralString(final int min, final int max, final CharSequence ls) {

        return new CompositeSequence(min * ls.length(), Sequences.mul(max, ls.length())) {

            final IndexOf indexOf     = StringUtil.indexOf(ls);
            final int     infixLength = ls.length();

            @Override public boolean
            matches(MatcherImpl matcher) {

                int o        = matcher.offset;
                int maxIndex = matcher.regionEnd - this.infixLength;

                if (maxIndex - o > max) maxIndex = o + max; // Beware of overflow!

                int minIndex = o + min;

                matcher.hitEnd = true;

                while (maxIndex >= minIndex) {

                    // Find next match of the infix withing the subject string.
                    maxIndex = this.indexOf.lastIndexOf(matcher.subject, minIndex, maxIndex);
                    if (maxIndex == -1) return false;

                    // See if the successor matches the rest of the subject.
                    matcher.offset = maxIndex + this.infixLength;
                    if (this.next.matches(matcher)) return true;

                    // Successor didn't match, continue with next character position.
                    maxIndex--;
                }

                return false;
            }

            @Override public String
            toStringWithoutNext() {
                return (
                    "greedyQuantifierOnAnyCharAndLiteralString(min="
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

        return new CompositeSequence(0) {

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

    /**
     * @return <var>op1</var> {@code +} <var>op2</var> iff neither is {@link Integer#MAX_VALUE}, otherwise {@link
     *         Integer#MAX_VALUE}
     */
    static int
    add(int op1, int op2) {
        return (op1 == Integer.MAX_VALUE || op2 == Integer.MAX_VALUE) ? Integer.MAX_VALUE : op1 + op2;
    }

    /**
     * @return <var>op1</var> {@code *} <var>op2</var> iff neither is {@link Integer#MAX_VALUE}, otherwise {@link
     *         Integer#MAX_VALUE}
     */
    static int
    mul(int op1, int op2) {
        return (op1 == Integer.MAX_VALUE || op2 == Integer.MAX_VALUE) ? Integer.MAX_VALUE : op1 * op2;
    }
}
