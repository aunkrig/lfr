
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

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Classes and methods related to {@link Sequence}s.
 */
class Sequences {

    /**
     * Representation of a sequence of literal, case-sensitive characters.
     */
    static
    class LiteralString extends Pattern.AbstractSequence {

        String s;

        LiteralString(String s) { this.s = s; }

        @Override public int
        matches(MatcherImpl matcher, int offset) {
            offset = matcher.peekRead(offset, this.s);
            if (offset == -1) return -1;
            return this.successor.matches(matcher, offset);
        }

        @Override
        public Sequence reverse() {

            this.s = new StringBuilder(this.s).reverse().toString();

            return super.reverse();
        }

        @Override public String
        toString() { return this.s + this.successor; }
    }

    public static Sequence
    emptyStringSequence() { return new Pattern.EmptyStringSequence(); }

    public static Sequence
    literalString(final String s) { return new LiteralString(s); }

    /**
     * Representation of the "|" operator.
     */
    public static Sequence
    alternatives(final Sequence[] alternatives) {

        if (alternatives.length == 0) return Sequences.emptyStringSequence();

        if (alternatives.length == 1) return alternatives[0];

        return new Pattern.AbstractSequence() {

            boolean successorAppended;

            @Override public void
            append(Sequence that) {

                // On the first invocation, we append "that" to all alternatives. On esubsequent invocations, we append
                // "that" only to ONE alternative, because otherwise it would be added N times.
                if (this.successorAppended) {
                    alternatives[0].append(that);
                } else {
                    for (Sequence s : alternatives) s.append(that);
                    this.successorAppended = true;
                }
            }

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int result = alternatives[0].matches(matcher, offset);
                if (result != -1) return result;

                for (int i = 1; i < alternatives.length; i++) {
                    result = alternatives[i].matches(matcher, offset);
                    if (result != -1) return result;
                }

                return -1;
            }

            @Override public String
            toString() { return Sequences.join(alternatives, '|'); }
        };
    }

    public static Sequence
    independenNonCapturingGroup(final Sequence[] alternatives) {

        if (alternatives.length == 0) return Sequences.emptyStringSequence();

        if (alternatives.length == 1) return alternatives[0];

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                for (Sequence a : alternatives) {
                    int result = a.matches(matcher, offset);
                    if (result != -1) return this.successor.matches(matcher, result);
                }

                return -1;
            }

            @Override public String
            toString() { return "(?>" + Sequences.join(alternatives, '|') + ')' + this.successor; }
        };
    }

    public static Sequence
    capturingGroupStart(final int groupNumber) {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int[] newGroups = Arrays.copyOf(matcher.groups, matcher.groups.length);

                int[] savedGroups = matcher.groups;
                newGroups[2 * groupNumber] = offset;
                matcher.groups = newGroups;

                int result = this.successor.matches(matcher, offset);

                if (result == -1) {
                    matcher.groups = savedGroups;
                    return -1;
                }

                return result;
            }

            @Override public Sequence
            reverse() {
                Sequence result = this.successor.reverse();
                result.append(Sequences.capturingGroupEnd(groupNumber));
                return result;
            }

            @Override public String
            toString() { return "(" + this.successor; }
        };
    }

    public static Sequence
    capturingGroupEnd(final int groupNumber) {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                matcher.groups[2 * groupNumber + 1] = offset;

                return this.successor.matches(matcher, offset);
            }

            @Override public Sequence
            reverse() {
                if (this.successor == Pattern.TERMINAL) {
                    return Sequences.capturingGroupStart(groupNumber);
                }
                Sequence result = this.successor.reverse();
                result.append(Sequences.capturingGroupStart(groupNumber));
                return result;
            }

            @Override public String
            toString() { return ")" + this.successor; }
        };
    }

    /**
     * @param groupNumber 1...<var>groupCount</var>
     */
    public static Sequence
    capturingGroup(final int groupNumber, final Sequence subsequence) {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int result = subsequence.matches(matcher, offset);
                if (result == -1) return -1;

                // Copy "this.groups" and store group start and end.
                int[] gs = (matcher.groups = Arrays.copyOf(matcher.groups, matcher.groups.length));
                gs[2 * groupNumber]     = offset;
                gs[2 * groupNumber + 1] = result;

                return this.successor.matches(matcher, result);
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
    public static Sequence
    capturingGroupBackReference(final int groupNumber) {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int[]        gs    = matcher.groups;
                CharSequence group = matcher.subject.subSequence(
                    gs[2 * groupNumber],
                    gs[2 * groupNumber + 1]
                );

                offset = matcher.peekRead(offset, group);
                if (offset == -1) return -1;

                return this.successor.matches(matcher, offset);
            }

            @Override public String
            toString() { return "\\" + groupNumber; }
        };
    }

    public static Sequence
    beginningOfInput() {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {
                return offset != matcher.anchoringRegionStart ? -1 : this.successor.matches(matcher, offset);
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
                Sequence result = this.successor.reverse();
                result.append(Sequences.endOfInput());
                return result;
            }

            @Override public String
            toString() { return "^" + this.successor; }
        };
    }

    public static Sequence
    beginningOfLine(final String lineBreakCharacters) {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {
                if (
                    offset == matcher.anchoringRegionStart
                    || lineBreakCharacters.indexOf(matcher.charAt(offset - 1)) != -1
                ) return this.successor.matches(matcher, offset);

                if (offset == matcher.anchoringRegionEnd) matcher.hitEnd = true;

                return -1;
            }

            @Override public Sequence
            reverse() {
                Sequence result = this.successor.reverse();
                result.append(Sequences.endOfLine(lineBreakCharacters));
                return result;
            }

            @Override public String
            toString() { return "^" + this.successor; }
        };
    }

    public static Sequence
    endOfInputButFinalTerminator() {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset >= matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.successor.matches(matcher, offset);
                }

                char c = matcher.charAt(offset);
                if (Pattern.LINE_BREAK_CHARACTERS.indexOf(c) == -1) return -1;

                if (offset == matcher.anchoringRegionEnd - 1) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.successor.matches(matcher, offset);
                }

                if (c == '\r' && matcher.charAt(offset + 1) == '\n') {
                    if (offset == matcher.anchoringRegionEnd - 2) {
                        matcher.hitEnd = true;
                        return this.successor.matches(matcher, offset);
                    }
                }

                return -1;
            }

            @Override public Sequence
            reverse() {
                Sequence result = this.successor.reverse();
                result.append(Sequences.beginningOfInput());
                return result;
            }

            @Override public String
            toString() { return "\\Z" + this.successor; }
        };
    }

    public static Sequence
    endOfInput() {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset < matcher.anchoringRegionEnd) return -1;

                matcher.hitEnd     = true;
                matcher.requireEnd = true;

                return this.successor.matches(matcher, offset);
            }

            @Override public Sequence
            reverse() {
                Sequence result = this.successor.reverse();
                result.append(Sequences.beginningOfInput());
                return result;
            }

            @Override public String
            toString() { return "\\z" + this.successor; }
        };
    }

    public static Sequence
    endOfLine(final String lineBreakCharacters) {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset == matcher.anchoringRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    return this.successor.matches(matcher, offset);
                }

                if (lineBreakCharacters.indexOf(matcher.charAt(offset)) == -1) return -1;

                return this.successor.matches(matcher, offset);
            }

            @Override public Sequence
            reverse() {
                Sequence result = this.successor.reverse();
                result.append(Sequences.beginningOfLine(lineBreakCharacters));
                return result;
            }

            @Override public String
            toString() { return "$" + this.successor; }
        };
    }

    public static Sequence
    wordBoundary() {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset >= matcher.transparentRegionEnd) {
                    matcher.hitEnd     = true;
                    matcher.requireEnd = true;
                    if (offset == matcher.transparentRegionStart) return -1; // Zero-length region.
                    if (!Pattern.isWordCharacter(matcher.charAt(offset - 1))) return -1;
                } else
                if (offset <= matcher.transparentRegionStart) {
                    if (!Pattern.isWordCharacter(matcher.charAt(offset))) return -1;
                } else
                if (
                    Pattern.isWordCharacter(matcher.charAt(offset - 1))
                    == Pattern.isWordCharacter(matcher.charAt(offset))
                ) {
                    return -1;
                }

                return this.successor.matches(matcher, offset);
            }

            @Override public String
            toString() { return "\\b" + this.successor; }
        };
    }

    public static Sequence
        endOfPreviousMatch() {

            return new Pattern.AbstractSequence() {

                boolean reverse;

                @Override public int
                matches(MatcherImpl matcher, int offset) {

                    // The documentation of java.util.regex is totally unclear about the following case, but this seems to
                    // be how it works:
                    if (matcher.endOfPreviousMatch == -1) return this.successor.matches(matcher, offset);
    //                if (matcher.endOfPreviousMatch == -1) return -1;

                    if (offset != matcher.endOfPreviousMatch) return -1;

                    return this.successor.matches(matcher, offset);
                }

                @Override public Sequence
                reverse() {
                    this.reverse = !this.reverse;
                    return super.reverse();
                }

                @Override public String
                toString() { return "\\G" + this.successor; }
            };
        }

    public static Sequence
    positiveLookahead(final Sequence op) {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                boolean lookaheadMatches;

                Pattern.End savedEnd       = matcher.end;
                int         savedRegionEnd = matcher.regionEnd;
                {
                    matcher.end       = Pattern.End.ANY;
                    matcher.regionEnd = matcher.transparentRegionEnd;

                    lookaheadMatches = op.matches(matcher, offset) != -1;
                    matcher.requireEnd = matcher.hitEnd;
                }
                matcher.end       = savedEnd;
                matcher.regionEnd = savedRegionEnd;

                return lookaheadMatches ? this.successor.matches(matcher, offset) : -1;
            }
        };
    }

    public static Sequence
    positiveLookbehind(final Sequence op) {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int l1 = matcher.subject.length();

                boolean lookbehindMatches;

                Pattern.End savedEnd          = matcher.end;
                boolean     savedHitStart     = matcher.hitStart;
                boolean     savedRequireStart = matcher.requireStart;
                int         savedRegionStart  = matcher.regionStart;
                {
                    matcher.reverse();
                    {
                        matcher.end        = Pattern.End.ANY;
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

                return lookbehindMatches ? this.successor.matches(matcher, offset) : -1;
            }
        };
    }

    public static Sequence
    greedyQuantifierSequence(final Sequence op, final int min, final int max) {

        // Optimize for bare character classes, e.g. "." or "[...]".
        if (op instanceof Pattern.CharacterClass) {
            final Pattern.CharacterClass cc = (Pattern.CharacterClass) op;
            if (cc.successor == Pattern.TERMINAL) {
                return Sequences.greedyQuantifierCharacterPredicate(min, max, cc);
            }
        }

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // The operand MUST match (min) times;
                for (int i = 0; i < min; i++) {
                    offset = op.matches(matcher, offset);
                    if (offset == -1) return -1;
                }

                // Now try to match the operand (max-min-1) more times.
                int     limit       = max - min;
                int[]   savedOffset = new int[Math.min(limit, 20)];

                for (int i = 0; i < limit; i++) {

                    if (i >= savedOffset.length) savedOffset = Arrays.copyOf(savedOffset, 2 * i);

                    savedOffset[i] = offset;

                    offset = op.matches(matcher, offset);

                    if (offset == -1) {
                        for (; i >= 0; i--) {
                            offset = this.successor.matches(matcher, savedOffset[i]);
                            if (offset != -1) return offset;
                        }
                        return -1;
                    }
                }

                return this.successor.matches(matcher, offset);
            }

            @Override public String
            toString() { return op + "{" + min + "," + max + "}" + this.successor; }
        };
    }

    /**
     * Optimized version of {@link #greedyQuantifierSequence(Sequence, int, int)} when the operand is a bary
     * character predicate, e.g. "." or "[...]".
     */
    public static Sequence
    greedyQuantifierCharacterPredicate(final int min, final int max, final IntPredicate operand) {

        return new Pattern.AbstractSequence() {

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
                int offsetAfterMin = offset;

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

                    int offset2 = this.successor.matches(matcher, offset);
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
            toString() { return operand + "{" + min + "," + max + "}" + this.successor; }
        };
    }

    public static Sequence
    reluctantQuantifierSequence(final Sequence op, final int min, final int max) {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                // The operand MUST match min times;
                int i;
                for (i = 0; i < min; i++) {
                    offset = op.matches(matcher, offset);
                    if (offset == -1) return -1;
                }

                // Now try to match the operand (max-min) more times.
                for (;; i++) {

                    int offset2 = this.successor.matches(matcher, offset);
                    if (offset2 != -1) return offset2;

                    if (i >= max) return -1;

                    offset = op.matches(matcher, offset);
                    if (offset == -1) return -1;
                }
            }

            @Override public String
            toString() { return op + "{" + min + "," + max + "}?" + this.successor; }
        };
    }

    public static Sequence
    possessiveQuantifierSequence(final Sequence op, final int min, final int max) {

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                int i = 0;
                for (; i < min; i++) {
                    offset = op.matches(matcher, offset);
                    if (offset == -1) return -1;
                }

                for (; i < max; i++) {

                    int offset2 = op.matches(matcher, offset);

                    if (offset2 == -1) return this.successor.matches(matcher, offset);

                    offset = offset2;
                }

                return this.successor.matches(matcher, offset);
            }

            @Override public String
            toString() { return op + "{" + min + "," + max + "}+" + this.successor; }
        };
    }

    static String
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

        return new Pattern.AbstractSequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                boolean operandMatches;

                Pattern.End savedEnd = matcher.end;
                {
                    matcher.end = Pattern.End.ANY;

                    operandMatches = op.matches(matcher, offset) != -1;
                }
                matcher.end = savedEnd;

                return operandMatches ? -1 : this.successor.matches(matcher, offset);
            }
        };
    }
}
