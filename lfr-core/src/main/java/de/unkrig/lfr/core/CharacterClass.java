
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
 * A {@link CompositeSequence} that implements {@link #matches(MatcherImpl, int)} by applying {@link
 * #matches(int)} onto itself.
 */
public abstract
class CharacterClass extends CompositeSequence {

    @Override public final int
    matches(MatcherImpl matcher, int offset) {

        if (offset >= matcher.regionEnd) {
            matcher.hitEnd = true;
            return -1;
        }

        int cp = matcher.subject.charAt(offset++);

        // Special handling for UTF-16 surrogates.
        if (Character.isHighSurrogate((char) cp)) {
            if (offset < matcher.regionEnd) {
                char ls = matcher.subject.charAt(offset);
                if (Character.isLowSurrogate(ls)) {
                    cp = Character.toCodePoint((char) cp, ls);
                    offset++;
                }
            }
        }

        return this.matches(cp) ? this.next.matches(matcher, offset) : -1;
    }

    /**
     * @return Whether <var>c</var> matches this character class
     */
    public abstract boolean
    matches(int c);

    /**
     * {@link #matches(int)} is guaranteed to return {@code false} for all subjects smaller than {@link #lowerBound()}.
     */
    @SuppressWarnings("static-method") public int
    lowerBound() { return 0; }

    /**
     * {@link #matches(int)} is guaranteed to return {@code false} for all subjects greater than or equal to {@link
     * #upperBound()}.
     */
    @SuppressWarnings("static-method") public int
    upperBound() { return Integer.MAX_VALUE; }

    /**
     * @return The number of values for which {@link #matches(int)} returns {@code true}, or more
     */
    @SuppressWarnings("static-method") public int
    sizeBound() { return Integer.MAX_VALUE; }
}
