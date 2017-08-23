
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
 * @author Arno
 *
 */
public abstract
class ReverseCharSequence implements CharSequence {

    protected abstract CharSequence
    original();

    public static CharSequence
    reverse(CharSequence cs) {
        return cs.length() <= 10 ? ReverseCharSequence.reverseByCopy(cs) : ReverseCharSequence.reverseInPlace(cs);
    }

    /**
     * Returns a string that is the "logical reverse" of the <var>cs</var>, i.e. surrogate pairs and CR-LF pairs are
     * <em>not</em> reversed.
     */
    public static String
    reverseByCopy(CharSequence cs) {

        StringBuilder sb = new StringBuilder(cs).reverse();

        // Un-reverse CR-LF sequences.
        for (int i = sb.length() - 2; i >= 0; i--) {
            if (sb.charAt(i) == '\n' && sb.charAt(i + 1) == '\r') {
                sb.setCharAt(i,     '\r');
                sb.setCharAt(i + 1, '\n');
                i--;
            }
        }

        return sb.toString();
    }

    /**
     * Returns a {@link CharSequence} that is the "logical reverse" of the <var>cs</var>, i.e. surrogate pairs and
     * CR-LF pairs are <em>not</em> reversed. The returned {@link CharSequence} is based on the original, thus, the
     * behavior of the returned {@link CharSequence} is undefined if the original changes.
     */
    public static CharSequence
    reverseInPlace(final CharSequence cs) {

        if (cs instanceof ReverseCharSequence) return ((ReverseCharSequence) cs).original();

        return new ReverseCharSequence() {

            final int len   = cs.length();
            final int lenm1 = this.len - 1;

            // IMPLEMENT ReverseCharSequence

            @Override protected CharSequence
            original() { return cs; }

            // IMPLEMENT CharSequence

            @Override public int
            length() { return this.len; }

            @Override public char
            charAt(int offset) {

                // Reverse the offset.
                offset = this.lenm1 - offset;

                char c = cs.charAt(offset);

                // Un-reverse CR-LF sequences.
                if (c == '\r' && offset < this.lenm1 && cs.charAt(offset + 1) == '\n') return '\n';
                if (c == '\n' && offset > 0          && cs.charAt(offset - 1) == '\r') return '\r';

                // Un-reverse high-surrogate-low-surrogate.
                char c2;
                if (
                    Character.isHighSurrogate(c)
                    && offset < this.lenm1
                    && Character.isLowSurrogate((c2 = cs.charAt(offset + 1)))
                ) return c2;
                if (
                    Character.isLowSurrogate(c)
                    && offset > 0
                    && Character.isHighSurrogate((c2 = cs.charAt(offset - 1)))
                ) return c2;

                return c;
            }

            @Override public CharSequence
            subSequence(int start, int end) { return ReverseCharSequence.subsequence(cs, start, end); }

            @Override
            public String toString() {
                // TODO Auto-generated method stub
                return super.toString();
            }


        };
    }

    static CharSequence
    subsequence(final CharSequence cs, final int start, final int end) {

        return new CharSequence() {

            @Override public int
            length() { return end - start; }

            @Override public char
            charAt(int index) { return cs.charAt(start + index); }

            @Override public CharSequence
            subSequence(int start2, int end2) {
                return ReverseCharSequence.subsequence(cs, start + start2, start + end2);
            }

            @Override public String
            toString() {
                char[] ca = new char[end - start];
                for (int i = start; i < end; i++) ca[i] = cs.charAt(start + i);
                return new String(ca);
            }
        };
    }
}
