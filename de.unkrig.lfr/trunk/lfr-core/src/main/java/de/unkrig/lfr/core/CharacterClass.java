
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
 * A {@link LinkedAbstractSequence} that implements {@link #matches(MatcherImpl, int)} by applying {@link
 * IntPredicate#evaluate(int)} onto itself.
 */
public abstract
class CharacterClass extends LinkedAbstractSequence implements IntPredicate {

    @Override public final int
    matches(MatcherImpl matcher, int offset) {

        if (offset >= matcher.regionEnd) {
            matcher.hitEnd = true;
            return -1;
        }

        char c = matcher.charAt(offset++);

        // Special handling for UTF-16 surrogates.
        if (Character.isHighSurrogate(c)) {
            if (offset < matcher.regionEnd) {
                char c2 = matcher.charAt(offset);
                if (Character.isLowSurrogate(c2)) {
                    return (
                        this.evaluate(Character.toCodePoint(c, c2))
                        ? this.next.matches(matcher, offset + 1)
                        : -1
                    );
                }
            }
        }

        return this.evaluate(c) ? this.next.matches(matcher, offset) : -1;
    }
}
