
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

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link CharacterClass} that implements {@link #union(CharacterClass)} by setting up a linked list of
 * {@link CharacterClass}es, and implements {@link #matches(int)} by applying {@link
 * IntPredicate#evaluate(int)} on the list elements until one returns {@code true}.
 */
public abstract
class Union extends CharacterClass implements IntPredicate {

    protected CharacterClass companion = new CharacterClasses.EmptyCharacterClass();

    @Override public final boolean
    matches(int subject) { return this.evaluate(subject) || this.companion.matches(subject); }

    @Override public final int
    lowerBound() { return Math.min(this.lowerBoundWithoutCompanion(), this.companion.lowerBound()); }

    @Override public final int
    upperBound() { return Math.max(this.upperBoundWithoutCompanion(), this.companion.upperBound()); }

    @Override public final int
    sizeBound() {
        int sb  = this.sizeBoundWithoutCompanion();
        int csb = this.companion.upperBound();
        return sb > Integer.MAX_VALUE - csb ? Integer.MAX_VALUE : sb + csb;
    }

    @Override public final CharacterClass
    union(CharacterClass that) {

        this.companion = this.companion.union(that);

        // Now attempt to optimize this character class.

        // Some performance tests on JRE 8 with "-server":
        //   "c == this.c"                  => 1.1 us
        //   "c == this.c1 || c == this.c2" => 2.1 us
        //   "this.s.indexOf(c) == -1"      => 9.8 us  (s.length() == 16)
        //   "this.s.indexOf(c) == -1"      => 5.2 us  (s.length() == 3)
        //   "this.s.indexOf(c) == -1"      => 3.8 us  (s.length() == 1)
        //   "this.intArray[i] == c"        => identical as "s.indexOf(c)"
        //   "this.intHashSet.contains(c)"  => 6.0 us  (s.length() == 1)
        //   "this.intHashSet.contains(c)"  => 5.7 us  (s.length() == 3)
        //   "this.intHashSet.contains(c)"  => 5.7 us  (s.length() == 16)
        //   "this.bitSet.get(c)"           => 3.1 us

        // Summary:
        // For 1 character,          "c == this.c"                  is optimal (1.1 us)
        // For 2 characters,         "c == this.c1 || c == this.c2" is optimal (2.1 us)
        // For 3 or more characters, "this.bitSet.get(c)"           is optimal (3.1 us)
        // If bitSet cannot be used (c much greater than 256),
        //                           "this.intHashSet.contains()"   is optimal (5.7 us)
        // The following are optimal in none of the cases, and are thus not useful:
        //    "this.s.indexOf(c) == -1"
        //    "this.intArray[i] == c"

        // Determine the PRECISE minValue, maxValue and size.
        final int minValue, maxValue, size;
        {
            int lb = this.lowerBound(), ub = this.upperBound();

            if (ub - lb > 1000) {

                // Character class is too widely spread for optimization; give up.
                return this;
            }

            int min2 = Integer.MAX_VALUE, max2 = -1, size2 = 0;
            for (int i = lb; i < ub; i++) {
                if (this.matches(i)) {
                    if (i < min2) min2 = i;
                    if (i > max2) max2 = i;
                    size2++;
                }
            }

            minValue = min2;
            maxValue = max2;
            size     = size2;
        }

        if (size == 1) {
            assert minValue == maxValue;
            return (
                this instanceof CharacterClasses.LiteralCharacter
                ? this
                : CharacterClasses.literalCharacter(minValue)
            );
        }

        if (size == 2) {
            return (
                this instanceof CharacterClasses.OneOfTwoCharacterClass
                ? this
                : CharacterClasses.oneOfTwo(minValue, maxValue)
            );
        }

        if (maxValue < 256) {

            final Union outer = this;
            class BitSetUnion extends Union {

                final BitSet bitSet = new BitSet(maxValue + 1);

                {
                    for (int i = minValue; i <= maxValue; i++) {
                        if (outer.matches(i)) this.bitSet.set(i);
                    }
                }

                @Override public boolean evaluate(int c)              { return this.bitSet.get(c) || this.companion.matches(c); }
                @Override protected int  lowerBoundWithoutCompanion() { return minValue;           }
                @Override protected int  upperBoundWithoutCompanion() { return maxValue + 1;       }
                @Override protected int  sizeBoundWithoutCompanion()  { return size;               }

                @Override public String
                toStringWithoutCompanion() {
                    StringBuilder sb = null;
                    for (int i = 0; i < this.bitSet.size(); i++) {
                        if (this.bitSet.get(i)) {
                            sb = (sb == null ? new StringBuilder("bitSet('") : sb.append("', '")).append((char) i);
                        }
                    }
                    assert sb != null;
                    return sb.append("')").toString();
                }
            }

            return (
                this instanceof BitSetUnion && this.companion instanceof CharacterClasses.EmptyCharacterClass
                ? this
                : new BitSetUnion()
            );
        }

        if (size < 200) {

            final Union outer = this;

            class IntegerSetUnion extends Union {

                final Set<Integer> set = new HashSet<Integer>();

                {
                    for (int i = minValue; i <= maxValue; i++) {
                        if (outer.matches(i)) this.set.add(i);
                    }
                }

                @Override public boolean evaluate(int c)              { return this.set.contains(c) || this.companion.matches(c); }
                @Override protected int  lowerBoundWithoutCompanion() { return minValue;             }
                @Override protected int  upperBoundWithoutCompanion() { return maxValue + 1;         }
                @Override protected int  sizeBoundWithoutCompanion()  { return size;                 }

                @Override public String
                toStringWithoutCompanion() {
                    StringBuilder sb = null;
                    for (Integer i : this.set) {
                        sb = (sb == null ? new StringBuilder("set('") : sb.append("', '")).appendCodePoint(i);
                    }
                    assert sb != null;
                    return sb.append("')").toString();
                }
            }

            return (
                this instanceof IntegerSetUnion && this.companion instanceof CharacterClasses.EmptyCharacterClass
                ? this
                : new IntegerSetUnion()
            );
        }

        return this;
    }

    @Override public String
    toStringWithoutNext() {

        if (this.companion instanceof CharacterClasses.EmptyCharacterClass) return this.toStringWithoutCompanion();

        return "union(" + this.toStringWithoutCompanion() + ", " + this.companion + ")";
    }

    @SuppressWarnings("static-method") protected int
    lowerBoundWithoutCompanion() { return 0; }

    @SuppressWarnings("static-method") protected int
    upperBoundWithoutCompanion() { return Integer.MAX_VALUE; }

    @SuppressWarnings("static-method") protected int
    sizeBoundWithoutCompanion() { return Integer.MAX_VALUE; }

    public abstract String toStringWithoutCompanion();
}
