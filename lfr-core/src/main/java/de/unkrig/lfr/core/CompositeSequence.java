
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

import de.unkrig.commons.util.ArrayUtil;

/**
 * An {@link Sequence} that implements {@link #concat(Sequence)} by setting up a linked list of {@link Sequence}es.
 * <p>
 *   Notice that implementations' {@link #matches(MatcherImpl)} methods must always honor the {@link #next}!
 * </p>
 */
abstract
class CompositeSequence extends Sequence {

    /**
     * Reference to the "next" sequence.
     */
    Sequence          next;
    private final int minMatchLengthWithoutNext;
    private final int maxMatchLengthWithoutNext;

    CompositeSequence(int matchLengthWithoutNext) {
        super(matchLengthWithoutNext, matchLengthWithoutNext);
        this.next                      = Sequences.TERMINAL;
        this.minMatchLengthWithoutNext = matchLengthWithoutNext;
        this.maxMatchLengthWithoutNext = matchLengthWithoutNext;
    }

    CompositeSequence(
        int minMatchLengthWithoutNext,
        int maxMatchLengthWithoutNext
    ) {
        super(minMatchLengthWithoutNext, maxMatchLengthWithoutNext);
        this.next                      = Sequences.TERMINAL;
        this.minMatchLengthWithoutNext = minMatchLengthWithoutNext;
        this.maxMatchLengthWithoutNext = maxMatchLengthWithoutNext;
    }

    @Override public Sequence
    concat(Sequence that) {

        this.next = this.next.concat(that);

        this.minMatchLength = Sequences.add(this.minMatchLengthWithoutNext, this.next.minMatchLength);
        this.maxMatchLength = Sequences.add(this.maxMatchLengthWithoutNext, this.next.maxMatchLength);

        // Join adjacent MultivalentSequences.
        if (this instanceof MultivalentSequence) {
            MultivalentSequence ms1 = (MultivalentSequence) this;
            CompositeSequence   cs1 = (CompositeSequence)   this;

            if (cs1.next instanceof MultivalentSequence) {
                MultivalentSequence ms2 = (MultivalentSequence) cs1.next;
                CompositeSequence   cs2 = (CompositeSequence)   cs1.next;

                return Sequences.multivalentSequence(ArrayUtil.append(
                    ms1.getNeedle(),
                    ms2.getNeedle()
                )).concat(cs2.next);
            }
        }

        return this;
    }

    /**
     * @return A human-readable form of {@code this} sequence, but without the {@link #next} sequence
     */
    protected abstract String
    toStringWithoutNext();

    @Override public String
    toString() {
        return (
            this.next == Sequences.TERMINAL
            ? this.toStringWithoutNext()
            : this.toStringWithoutNext() + " . " + this.next.toString()
        );
    }
}
