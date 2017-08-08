
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

/**
 * A facade or abstraction for various regular expression frameworks.
 */
public
interface Pattern {

    // Copy flag constants from java.util.regex.Pattern for convenience.

    /** @see java.util.regex.Pattern#CANON_EQ */
    public static final int CANON_EQ = java.util.regex.Pattern.CANON_EQ;
    /** @see java.util.regex.Pattern#CASE_INSENSITIVE */
    public static final int CASE_INSENSITIVE = java.util.regex.Pattern.CASE_INSENSITIVE;
    /** @see java.util.regex.Pattern#COMMENTS */
    public static final int COMMENTS = java.util.regex.Pattern.COMMENTS;
    /** @see java.util.regex.Pattern#DOTALL */
    public static final int DOTALL = java.util.regex.Pattern.DOTALL;
    /** @see java.util.regex.Pattern#LITERAL */
    public static final int LITERAL = java.util.regex.Pattern.LITERAL;
    /** @see java.util.regex.Pattern#MULTILINE */
    public static final int MULTILINE = java.util.regex.Pattern.MULTILINE;
    /** @see java.util.regex.Pattern#UNICODE_CASE */
    public static final int UNICODE_CASE = java.util.regex.Pattern.UNICODE_CASE;
    /** @see java.util.regex.Pattern#UNIX_LINES */
    public static final int UNIX_LINES = java.util.regex.Pattern.UNIX_LINES;
    /** @see java.util.regex.Pattern#UNICODE_CHARACTER_CLASS */
    public static final int UNICODE_CHARACTER_CLASS = 256; // Since Java 7

    /**
     * @see java.util.regex.Pattern#pattern()
     */
    public abstract String
    pattern();

    /**
     * @see java.util.regex.Pattern#matcher(CharSequence)
     */
    public abstract Matcher
    matcher(CharSequence subject);

    /**
     * @see java.util.regex.Pattern#flags()
     */
    public abstract int
    flags();

    /**
     * @return Whether the suffix starting at position <var>offset</var> matches {@code this} pattern
     * @see    java.util.regex.Pattern#matches(String, CharSequence)
     */
    public abstract boolean
    matches(CharSequence subject, int offset);

    /**
     * @see java.util.regex.Pattern#split(CharSequence)
     */
    public abstract String[]
    split(CharSequence input);

    /**
     * @see java.util.regex.Pattern#quote(String)
     */
    public abstract String
    quote(String s);
}
