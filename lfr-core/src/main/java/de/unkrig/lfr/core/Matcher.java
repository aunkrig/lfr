
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
     * <p>
     *   Supports the following constructs:
     *  </p>
     * <table border=1>
     *   <tr><td>{@code $}<var>n</var></td><td>Capturing group reference</td></tr>
     *   <tr><td><code>${</code><var>capturing-group-name</var><code>}</code></td><td>Named capturing group reference</td></tr>
     *   <tr><td><code>${</code><var>expr</var><code>}</code></td><td>Java-like expression</td></tr>
     *   <tr><td>{@code \0} {@code \0}<var>n</var> {@code \0}<var>nn</var> {@code \0}<var>mnn</var></td><td>Octal literal</td></tr>
     *   <tr><td>{@code \x}<var>hh</var></td><td>Hex literal (0-0xFF)</td></tr>
     *   <tr><td>{@code \}{@code u}<var>hhhh</var></td><td>Hex literal (0-0xFFFF)</td></tr>
     *   <tr><td><code>\x{</code><var>h...h</var><code>}</code></td><td>Hex literal (0-0x110000)</td></tr>
     *   <tr><td>{@code \Q}<var>any</var>{@code \E}</td><td>Literal text</td></tr>
     *   <tr><td>{@code \Q}<var>any</var></td><td>Literal text</td></tr>
     *   <tr><td>{@code \t \n \r \f \a \e \b}</td><td>TAB NL CR FF BEL ESC BACKSPACE</td></tr>
     *   <tr><td>{@code \c}<var>C</var></td><td>Control character (A-Z)</td></tr>
     * </table>
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
