
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
 * A drop-in replacement for {@link java.util.regex.Matcher}.
 */
public
interface Matcher extends de.unkrig.ref4j.Matcher {

    /**
     * Returns {@code true} iff the start of the transparent region was hit by the search engine in the last match
     * operation performed by this matcher; this can only happen if the pattern starts with a boundary matcher or
     * contains lookbehind constructs.
     * <p>
     *   When this method returns {@code true}, then it is possible that more input <em>before</em> the capturing
     *   region would have changed the result of the last search.
     * </p>
     */
    boolean requireStart();

    /**
     * Returns {@code true} iff the start of input was hit by the search engine in the last match operation
     * performed by this matcher; this can only happen if the pattern starts with a boundary matcher or contains
     * lookbehind constructs.
     * <p>
     *   When this method returns {@code true}, then it is possible that more input <em>before</em> the capturing
     *   region would have changed the result of the last search.
     * </p>
     */
    boolean hitStart();
}
