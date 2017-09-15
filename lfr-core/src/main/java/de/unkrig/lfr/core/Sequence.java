
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
 * A thing that can "match" (or not match) the {@link MatcherImpl#subject}. While matching, it modifies the
 * {@link MatcherImpl}'s state, in particular the {@link MatcherImpl#offset}.
 *
 * @see #matches(MatcherImpl)
 * @see #find(MatcherImpl)
 */
interface Sequence {

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
     *   <var>matcher</var>'s region, and {@link MatcherImpl#hitStart} is set to {@code true} iff an attempt was made
     *   to peek <em>before</em> the <var>matcher</var>'s region.
     * </p>
     *
     * @see Matcher#region(int, int)
     * @see MatcherImpl#groups
     * @see MatcherImpl#hitEnd
     */
    boolean
    matches(MatcherImpl matcher);

    /**
     * Searches for the <var>next</var> match, starting at the current {@link MatcherImpl#offset}, and, iff it finds
     * one, updates {@code groups[0]} and {@code groups[1]}, and returns {@code true}.
     */
    boolean
    find(MatcherImpl matcherImpl);

    /**
     * Concatenates {@code this} sequence with <var>that</var>. This operation may leave {@code this} and
     * <var>that</var> sequence in an invalid state; only the <em>returned</em> sequence must be used after this
     * operation.
     */
    Sequence
    concat(Sequence that);

    /**
     * Computes and returns the minimum value by which {@link #matches(MatcherImpl)} increases {@link
     * MatcherImpl#offset} iff it returns {@code true}. This is useful for many optimizations to check whether there
     * are enough input characters before executing the (relatively expensive) match.
     * <p>
     *   May return {@link Integer#MAX_VALUE} iff the sequence can impossibly match.
     * </p>
     */
    int
    minMatchLength();

    /**
     * Computes and returns the maximum value by which {@link #matches(MatcherImpl)} increases {@link
     * MatcherImpl#offset} iff it returns {@code true}. This is useful for many optimizations to check whether there
     * are too many input characters before executing the (relatively expensive) match.
     * <p>
     *   May return {@code -1} iff the sequence can impossibly match.
     * </p>
     */
    int
    maxMatchLength();

    /**
     * Returns an unambiguous string form of {@code this} sequence; practical for verifying a compiled sequence e.g.
     * for correctness, efficiency, etc. The syntax resembles Java.
     */
    @Override String
    toString();
}
