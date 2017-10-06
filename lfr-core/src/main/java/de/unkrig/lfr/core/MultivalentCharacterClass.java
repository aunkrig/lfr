
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.unkrig.commons.util.ArrayUtil;

/**
 * A specialization of {@link CharacterClass} that matches a (typically relatively small) set of code points.
 */
class MultivalentCharacterClass extends CharacterClass {

    protected final Set<Integer> codePoints;
    protected final int          lowerBound, upperBound, sizeBound;

    /**
     * @param codePoints The set of code points that designate a positive match e.g. "<code>{ 'a', 'A' }</code>"
     */
    MultivalentCharacterClass(Set<Integer> codePoints) {
        super(CharacterClasses.minCharCountOf(codePoints), CharacterClasses.maxCharCountOf(codePoints));
        this.codePoints = codePoints;

        this.lowerBound = CharacterClasses.min(codePoints);
        this.upperBound = CharacterClasses.max(codePoints) + 1;
        this.sizeBound  = codePoints.size();
    }

    @Override public boolean
    matches(int cp) { return this.codePoints.contains(cp); }

    @Override public int lowerBound() { return this.lowerBound; }
    @Override public int upperBound() { return this.upperBound; }
    @Override public int sizeBound()  { return this.sizeBound;  }

    @Override public Sequence
    concat(Sequence that) {

        Sequence result = super.concat(that);
        if (result != this) return result;

        if (
            this.minMatchLength == this.maxMatchLength
            && this.next instanceof MultivalentCharacterClass
        ) {
            MultivalentCharacterClass
            next2 = (MultivalentCharacterClass) this.next;

            if (next2.minMatchLength == next2.maxMatchLength) {

                List<char[]> chars1 = new ArrayList<char[]>(this.codePoints.size());
                for (int cp : this.codePoints) chars1.add(Character.toChars(cp));

                List<char[]> chars2 = new ArrayList<char[]>(next2.codePoints.size());
                for (int cp : next2.codePoints) chars2.add(Character.toChars(cp));

                return Sequences.multivalentSequence(ArrayUtil.append(
                    ArrayUtil.mirror(chars1.toArray(new char[chars1.size()][])),
                    ArrayUtil.mirror(chars2.toArray(new char[chars2.size()][]))
                )).concat(next2.next);
            }
        }

        if (this.minMatchLength == this.maxMatchLength && this.next instanceof Sequences.MultivalentSequence) {
            Sequences.MultivalentSequence next2 = (Sequences.MultivalentSequence) this.next;

            List<char[]> chars = new ArrayList<char[]>();
            for (int cp : this.codePoints) chars.add(Character.toChars(cp));

            return Sequences.multivalentSequence(ArrayUtil.append(
                ArrayUtil.mirror(chars.toArray(new char[chars.size()][])),
                next2.getNeedle()
            )).concat(next2.next);
        }

        return this;
    }

    @Override protected String
    toStringWithoutNext() { return "oneOfManyCodePoints(" + this.codePoints + ")"; }
}
