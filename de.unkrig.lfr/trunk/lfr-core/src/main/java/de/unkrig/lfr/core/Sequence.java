
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
 * A thing that can "match" a {@link CharSequence}.
 *
 * @see #matches(MatcherImpl, int)
 */
interface Sequence {

    /**
     * Checks whether this sequence matches the subject of the <var>matcher</var>, starting at the
     * <var>offset</var>.
     * <p>
     *   If this sequence matches, then the method returns the offset of the first character <em>after</em> the
     *   match, and <var>matcher</var>.{@link MatcherImpl#groups groups} is replaced with an array that reflects
     *   the groups after the match.
     * </p>
     * <p>
     *   Otherwise, if this sequence does <em>not</em> match, then the method returns {@code -1}, and the
     *   <var>matcher</var>.{@link MatcherImpl#groups groups} remain unchanged.
     * </p>
     * <p>
     *   In both cases, <var>matcher</var>.{@link MatcherImpl#hitEnd hitEnd} is set to {@code true} iff an attempt
     *   was made to peek past the <var>matcher</var>'s region, and <var>matcher</var>.{@link MatcherImpl#hitStart
     *   hitStart} is set to {@code true} iff an attempt was made to peek <em>before</em> the <var>matcher</var>'s
     *   region.
     * </p>
     *
     * @return If this sequence matches, the offset of the first character <em>after</em> the match, otherwise {@code
     *         -1}
     *
     * @see Matcher#region(int, int)
     * @see MatcherImpl#groups
     * @see MatcherImpl#hitEnd
     */
    int
    matches(MatcherImpl matcher, int offset);

    /**
     * Searches for the next occurrence, and, iff it finds one, updates {@code groups[0]} and {@code groups[1]},
     * and returns {@code true}.
     */
    boolean
    find(MatcherImpl matcherImpl, int start);

    /**
     * @return A sequence that is composed of {@code this} and <var>that</var>
     */
    Sequence
    concat(Sequence that);

    /**
     * Returns a sequence that, when matching the reverse subject, produces the reverse order of reverse matches.
     * Reversion of sequences is extremely useful when implementing look-behinds.
     * <p>
     *   This method may leave {@code this} object in an undefined state; only the <em>returned</em> object can be
     *   used afterwards. This agreement makes it possible for implementations to either modify {@code this} object,
     *   or create and return a new one, whichever is more efficient.
     * </p>
     */
    Sequence
    reverse();

    /**
     * Returns an unambiguous string form of {@code this} sequence; practical for verifying a compiled sequence e.g.
     * for correctness, efficiency, etc. The syntax resembles Java.
     */
    @Override String
    toString();
}
