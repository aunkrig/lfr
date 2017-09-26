
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

/**
 * Implements {@link #find(MatcherImpl)} through {@link #matches(MatcherImpl)}.
 */
abstract
class Sequence {

    /**
     * The minimum value by which {@link #matches(MatcherImpl)} increases {@link MatcherImpl#offset} iff it returns
     * {@code true}. This is useful for many optimizations to check whether there are enough input characters before
     * executing the (typically expensive) match.
     * <p>
     *   May be {@link Integer#MAX_VALUE} to indicate that the sequence can impossibly match any input.
     * </p>
     * <p>
     *   This field is initialized by {@link #Sequence(int, int)}, and must afterwards constantly be updated by the
     *   derived class, esp. when {@link #concat(Sequence)} is called.
     * </p>
     */
    int minMatchLength;

    /**
     * The maximum value by which {@link #matches(MatcherImpl)} increases {@link MatcherImpl#offset} iff it returns
     * {@code true}. This is useful for many optimizations to check whether there are too many input characters before
     * executing the (typically expensive) match.
     * <p>
     *   May be {@code -1} to indicate that the sequence can impossibly match any input.
     * </p>
     * <p>
     *   This field is initialized by {@link #Sequence(int, int)}, and must afterwards constantly be updated by the
     *   derived class, esp. when {@link #concat(Sequence)} is called.
     * </p>
     */
    int maxMatchLength;

    Sequence(int minMatchLength, int maxMatchLength) {
        this.minMatchLength = minMatchLength;
        this.maxMatchLength = maxMatchLength;
    }

    /**
     * Checks whether this sequence matches the subject of the <var>matcher</var>, starting at the current {@link
     * MatcherImpl#offset}.
     * <p>
     *   If this sequence matches, then the method advances the {@link MatcherImpl#offset} to the offset of the first
     *   character <em>after</em> the match, replaces the {@link MatcherImpl#groups} with an array that reflects the
     *   groups after the matching, and returns {@code true}.
     * </p>
     * <p>
     *   Otherwise, if this sequence does <em>not</em> match, then the {@link MatcherImpl#groups} remain unchanged, the
     *   {@link MatcherImpl#offset} is undefined, and the method returns {@code false}.
     * </p>
     * <p>
     *   In both cases, {@link MatcherImpl#hitEnd} is set to {@code true} iff an attempt was made to peek past the
     *   <var>matcher</var>'s region.
     * </p>
     *
     * @see Matcher#region(int, int)
     * @see MatcherImpl#groups
     * @see MatcherImpl#hitEnd
     */
    abstract boolean
    matches(MatcherImpl matcher);

    /**
     * Searches for the <var>next</var> match, starting at the current {@link MatcherImpl#offset}, and, iff it finds
     * one, updates {@code groups[0]} and {@code groups[1]}, and returns {@code true}.
     * <p>
     *   Derived classes may override this method if there is a faster implementation.
     * </p>
     */
    public boolean
    find(MatcherImpl matcher) {

        final int re = matcher.regionEnd;

        for (int o = matcher.offset;;) {

            if (this.matches(matcher)) {
                matcher.groups[0] = o;
                matcher.groups[1] = matcher.offset;
                return true;
            }

            if (o >= re) {
                matcher.hitEnd = true;
                return false;
            }

            if (
                Character.isHighSurrogate(matcher.subject.charAt(o++))
                && o < re
                && Character.isLowSurrogate(matcher.subject.charAt(o))
            ) o++;

            matcher.offset = o;
        }
    }

    /**
     * Concatenates {@code this} sequence with <var>that</var>. This operation may leave {@code this} and
     * <var>that</var> sequence in an invalid state; only the <em>returned</em> sequence may subsequently be used.
     */
    abstract Sequence
    concat(Sequence that);

    /**
     * Returns an unambiguous string form of {@code this} sequence; practical for verifying a compiled sequence e.g.
     * for correctness, efficiency, etc. The syntax resembles Java.
     */
    @Override public abstract String
    toString();
}
