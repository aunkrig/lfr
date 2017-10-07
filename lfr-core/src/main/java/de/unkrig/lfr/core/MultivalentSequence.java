
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
 * A sequence of fixed length, where each position is linked with a (typically small) set of {@code char}s.
 * These sets of chars are also called "the needle", as a metaphor for a needle that is to be found in a haystack.
 * <p>
 *   Examples for multivalent sequences:
 * </p>
 * <dl>
 *   <dd><code>"abc"</code></dd>
 *   <dd><code>"abc"</code>, case-insensitive</dd>
 *   <dd><code>"a[bc]d"</code> (match length is 3)</dd>
 *   <dd><code>"\s"</code> (whitespace characters are all in the BMP, and are a small set)</dd>
 *   <dd><code>"abc|def"</code> (both alternatives have the same length)</dd>
 *   <dd><code>"a\x{10000}|def"</code> (both alternatives have the same length)</dd>
 * </dl>
 * <p>
 *   The following are <em>not</em> multivalent sequences:
 * </p>
 * <dl>
 *   <dd><code>"[b\x{10000}]"</code> (<code>"b"</code> has length 1, and <code>"\x{10000}"</code> has length 2)</dd>
 *   <dd><code>"\S"</code> (non-whitespace characters; have different lengths)</dd>
 *   <dd><code>\p{IsAlphabetic}</code> (have different lengths)</dd>
 *   <dd><code>\p{InGreek}</code> (not a small set)</dd>
 *   <dd><code>A?</code> (length is not fixed)</dd>
 *   <dd><code>.*</code> (length is not fixed)</dd>
 * </dl>
 *
 * @see #getNeedle()
 */
interface MultivalentSequence {

    /**
     * Returns the character sets that represent a match, e.g. "<code>{ { 'a', 'A' }, { 'b', 'B' }, { 'c', 'C' }
     * }</code>".
     * <p>
     *   Notice that {@code getNeedle()[n].length} is often different for different values of {@code n}.
     * </p>
     */
    char[][]
    getNeedle();
}
