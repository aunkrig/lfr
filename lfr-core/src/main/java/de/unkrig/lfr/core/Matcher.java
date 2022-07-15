
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

import de.unkrig.commons.text.expression.Parser;

/**
 * Adds some "advanced" methods to the {@link de.unkrig.ref4j.Matcher} interface.
 */
public
interface Matcher extends de.unkrig.ref4j.Matcher {

    /**
     * Pre-parsing a "replacement string" saves considerable overhead compared to repeatedly calling {@link
     * Matcher#appendReplacement(Appendable, String)}.
     *
     * @see Matcher#compileReplacement(String)
     */
    public
    interface CompiledReplacement {

        /**
         * An optimized version of {@link Matcher#appendReplacement(Appendable, String)} that uses this pre-compiled
         * replacement.
         *
         * @see #compileReplacement(String)
         */
        void
        appendReplacement(Appendable appendable);

        /**
         * An optimized version of {@link Matcher#replaceAll(String)} that uses this pre-compiled replacement.
         * <p>
         *   Example:
         * </p>
         * <pre>
         *   de.unkrig.lfr.core.Pattern                     p  = de.unkrig.lfr.core.Pattern.compile(regex);
         *   de.unkrig.lfr.core.Matcher                     m  = p.matcher(subject1);
         *   de.unkrig.lfr.core.Matcher.CompiledReplacement cr = m.compileReplacement(replacement);
         *
         *   String result1 = cr.replaceAll();
         *   m.reset(subject2);
         *   String result2 = cr.replaceAll(); // Re-use the compiled replacement.
         * </pre>
         *
         * @see #compileReplacement(String)
         */
        String
        replaceAll();

        /**
         * An optimized version of {@link Matcher#replaceFirst(String)} that uses this pre-compiled replacement.
         * <p>
         *   Example:
         * </p>
         * <pre>
         *   de.unkrig.lfr.core.Pattern                     p  = de.unkrig.lfr.core.Pattern.compile(regex);
         *   de.unkrig.lfr.core.Matcher                     m  = p.matcher(subject1);
         *   de.unkrig.lfr.core.Matcher.CompiledReplacement cr = m.compileReplacement(replacement);
         *
         *   String result1 = cr.replaceFirst();
         *   m.reset(subject2);
         *   String result2 = cr.replaceFirst(); // Re-use the compiled replacement.
         * </pre>
         *
         * @see #compileReplacement(String)
         */
        String
        replaceFirst();
    }

    /**
     * Pre-compiles a replacement string for later use by {@link #appendReplacement(Appendable, String)}, {@link
     * #replaceAll(String)} and {@link #replaceFirst(String)}.
     *
     * <p>
     *   Supports the following JRE6-compatible constructs:
     * </p>
     *
     * <dl>
     *   <dt>
     *     {@code \}<var>x</var>
     *     <br />
     *     <var>x</var>
     *   </dt>
     *   <dd>
     *     The character <var>x</var>, literally.
     *   </dd>
     *   <dt>{@code $}<var>n</var></dt>
     *   <dd>
     *     The value of the <var>n</var>th capturing group, or {@code ""} if that group is not set.
     *   </dd>
     * </dl>
     *
     * <p>
     *   Supports the following JRE7-compatible constructs:
     * </p>
     *
     * <dl>
     *   <dt><code>${</code><var>name</var><code>}</code></dt>
     *   <dd>
     *     The value of the named capturing group "<var>name</var>", or {@code ""} if that group is not set.
     *   </dd>
     * </dl>
     *
     * <p>
     *   Supports the following additional constructs:
     * </p>
     * <dl>
     *   <dt><code>${</code><var>expression</var><code>}</code></dt>
     *   <dd>
     *     The value of the <var>expression</var>. The named groups of the pattern are available as expression
     *     variables; the matcher is available as variable "m". For the expression syntax, see {@link Parser}.
     *   </dd>
     * </dl>
     */
    CompiledReplacement
    compileReplacement(String replacement);

    /**
     * Returns, after a successful match, the value of the designated "capturing quantifier". A "capturing quantifier"
     * has the form "<code>{m,n}</code>", where the comma and {@code n} are optional.
     * <p>
     *   Capturing quantifier numbers start at zero, and increase left-to-right.
     * </p>
     * <p>
     *   Example:
     * </p>
     * <p>
     *   The regex is <code>"a{1,}b{1,}c{1,}"</code>, the subject string is {@code " abc aabbcc abbccc "}. There are
     *   three matches, and {@link #count(int)} returns the following values:
     * </p>
     * <table border="1">
     *   <tr><th>Match #</th><th>{@code count(0)}</th><th>{@code count(1)}</th><th>{@code count(2)}</th></tr>
     *   <tr><td>1</td><td>1</td><td>1</td><td>1</td></tr>
     *   <tr><td>2</td><td>2</td><td>2</td><td>2</td></tr>
     *   <tr><td>3</td><td>1</td><td>2</td><td>3</td></tr>
     * </table>
     * <p>
     *   The return value is undefined if there was no previous match, or if the quantifier was not executed during
     *   the match.
     * </p>
     */
    int
    count(int number);

    /**
     * @see java.util.regex.Matcher#quoteReplacement(String)
     */
    @Override public String
    quoteReplacement(String s);
}
