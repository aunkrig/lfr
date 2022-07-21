
/*
 * ref4j - Regular Expression Facade for Java
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

package de.unkrig.ref4j;

import java.util.function.Predicate;
import java.util.stream.Stream;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

@NotNullByDefault public
interface Pattern {

    // Copy flag constants from java.util.regex.Pattern for convenience.

    /** @see java.util.regex.Pattern#CANON_EQ */
    int CANON_EQ = java.util.regex.Pattern.CANON_EQ;
    /** @see java.util.regex.Pattern#CASE_INSENSITIVE */
    int CASE_INSENSITIVE = java.util.regex.Pattern.CASE_INSENSITIVE;
    /** @see java.util.regex.Pattern#COMMENTS */
    int COMMENTS = java.util.regex.Pattern.COMMENTS;
    /** @see java.util.regex.Pattern#DOTALL */
    int DOTALL = java.util.regex.Pattern.DOTALL;
    /** @see java.util.regex.Pattern#LITERAL */
    int LITERAL = java.util.regex.Pattern.LITERAL;
    /** @see java.util.regex.Pattern#MULTILINE */
    int MULTILINE = java.util.regex.Pattern.MULTILINE;
    /** @see java.util.regex.Pattern#UNICODE_CASE */
    int UNICODE_CASE = java.util.regex.Pattern.UNICODE_CASE;
    /** @see java.util.regex.Pattern#UNIX_LINES */
    int UNIX_LINES = java.util.regex.Pattern.UNIX_LINES;
    /** @see java.util.regex.Pattern#UNICODE_CHARACTER_CLASS */
    int UNICODE_CHARACTER_CLASS = 256; // Since Java 7

    /**
     * @see java.util.regex.Pattern#pattern()
     */
    String pattern();

    /**
     * @see java.util.regex.Pattern#matcher(CharSequence)
     */
    Matcher matcher(CharSequence subject);

    /**
     * @see java.util.regex.Pattern#flags()
     */
    int flags();

    /**
     * @return Whether the suffix starting at position <var>offset</var> matches {@code this} pattern
     * @see    java.util.regex.Pattern#matches(String, CharSequence)
     */
    boolean matches(CharSequence subject, int offset);

    /**
     * @see java.util.regex.Pattern#split(CharSequence)
     */
    String[] split(CharSequence input);

    /**
     * @see java.util.regex.Pattern#split(CharSequence, int)
     */
    String[] split(CharSequence input, int limit);

    /**
     * Creates a predicate that tests if this pattern is found in a given input string.
     * <p>
     *   API Note: This method creates a predicate that behaves as if it creates a matcher from the input sequence
     *   and then calls find, for example a predicate of the form:
     * </p>
     * <pre>
     *     s -> matcher(s).find();
     * </pre>
     *
     * @return The predicate which can be used for finding a match on a subsequence of a string
     * @since  JRE 1.8
     */
    Predicate<String> asPredicate();

    /**
     * Creates a predicate that tests if this pattern matches a given input string.
     * <p>
     *   API Note: This method creates a predicate that behaves as if it creates a matcher from the input sequence and
     *   then calls matches, for example a predicate of the form:
     * </p>
     * <pre>
     *     s -> matcher(s).matches();
     * </pre>
     *
     * @return The predicate which can be used for matching an input string against this pattern.
     * @since  JRE 11
     */
    Predicate<String> asMatchPredicate();

    /**
     * Creates a stream from the given input sequence around matches of this pattern. The stream returned by this
     * method contains each substring of the input sequence that is terminated by another subsequence that matches
     * this pattern or is terminated by the end of the input sequence. The substrings in the stream are in the order
     * in which they occur in the input. Trailing empty strings will be discarded and not encountered in the stream.
     * <p>
     *   If this pattern does not match any subsequence of the input then the resulting stream has just one element,
     *   namely the input sequence in string form.
     * </p>
     * <p>
     *   When there is a positive-width match at the beginning of the input sequence then an empty leading substring
     *   is included at the beginning of the stream. A zero-width match at the beginning however never produces such
     *   empty leading substring.
     * </p>
     * <p>
     *   If the input sequence is mutable, it must remain constant during the execution of the terminal stream
     *   operation. Otherwise, the result of the terminal stream operation is undefined.
     * </p>
     *
     * @param input The character sequence to be split
     * @return      The stream of strings computed by splitting the input around matches of this pattern
     * @since       JRE 1.8
    */
    Stream<String> splitAsStream(CharSequence input);
}
