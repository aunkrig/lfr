
/*
 * de.unkrig.lfr - A super-fast regular expression evaluator
 *
 * Copyright (c) 2016, Arno Unkrig
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

// SUPPRESS CHECKSTYLE Javadoc:9999

package de.unkrig.lfr.core;

import static de.unkrig.lfr.core.Pattern.TokenType.ASTERISK;
import static de.unkrig.lfr.core.Pattern.TokenType.BEGINNING_OF_INPUT;
import static de.unkrig.lfr.core.Pattern.TokenType.BEGINNING_OF_LINE;
import static de.unkrig.lfr.core.Pattern.TokenType.CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.CAPTURING_GROUP_BACK_REFERENCE;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_ANY;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_INTERSECTION;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_NAMED;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_PREDEFINED;
import static de.unkrig.lfr.core.Pattern.TokenType.EITHER_OR;
import static de.unkrig.lfr.core.Pattern.TokenType.END_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.END_OF_INPUT;
import static de.unkrig.lfr.core.Pattern.TokenType.END_OF_INPUT_BUT_FINAL_TERMINATOR;
import static de.unkrig.lfr.core.Pattern.TokenType.END_OF_LINE;
import static de.unkrig.lfr.core.Pattern.TokenType.END_OF_PREVIOUS_MATCH;
import static de.unkrig.lfr.core.Pattern.TokenType.INDEPENDENT_NON_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.INVALID_SEQUENCE;
import static de.unkrig.lfr.core.Pattern.TokenType.LEFT_BRACKET;
import static de.unkrig.lfr.core.Pattern.TokenType.LINEBREAK;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_CHARACTER;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_CONTROL1;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_CONTROL2;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_HEXADECIMAL1;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_HEXADECIMAL2;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_HEXADECIMAL3;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_NAMED;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_OCTAL;
import static de.unkrig.lfr.core.Pattern.TokenType.MATCH_FLAGS;
import static de.unkrig.lfr.core.Pattern.TokenType.MATCH_FLAGS_NON_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.NAMED_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.NAMED_CAPTURING_GROUP_BACK_REFERENCE;
import static de.unkrig.lfr.core.Pattern.TokenType.NEGATIVE_LOOKAHEAD;
import static de.unkrig.lfr.core.Pattern.TokenType.NEGATIVE_LOOKBEHIND;
import static de.unkrig.lfr.core.Pattern.TokenType.NON_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.NON_UNICODE_EXTENDED_GRAPHEME_CLUSTER_BOUNDARY;
import static de.unkrig.lfr.core.Pattern.TokenType.NON_WORD_BOUNDARY;
import static de.unkrig.lfr.core.Pattern.TokenType.PLUS;
import static de.unkrig.lfr.core.Pattern.TokenType.POSITIVE_LOOKAHEAD;
import static de.unkrig.lfr.core.Pattern.TokenType.POSITIVE_LOOKBEHIND;
import static de.unkrig.lfr.core.Pattern.TokenType.QUESTION;
import static de.unkrig.lfr.core.Pattern.TokenType.QUOTATION_BEGIN;
import static de.unkrig.lfr.core.Pattern.TokenType.QUOTATION_END;
import static de.unkrig.lfr.core.Pattern.TokenType.QUOTED_CHARACTER;
import static de.unkrig.lfr.core.Pattern.TokenType.RIGHT_BRACKET;
import static de.unkrig.lfr.core.Pattern.TokenType.UNICODE_EXTENDED_GRAPHEME;
import static de.unkrig.lfr.core.Pattern.TokenType.UNICODE_EXTENDED_GRAPHEME_CLUSTER_BOUNDARY;
import static de.unkrig.lfr.core.Pattern.TokenType.WORD_BOUNDARY;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.scanner.StatefulScanner;

/**
 * A drop-in replacement for {@link java.util.regex.Pattern}.
 */
public
class Pattern implements de.unkrig.ref4j.Pattern, Serializable {

    private static final long serialVersionUID = 1L;

    static final int SUPPORTED_FLAGS = (
        0
//        | Pattern.CANON_EQ   <= currently not implemented
        | Pattern.CASE_INSENSITIVE
        | Pattern.COMMENTS
        | Pattern.DOTALL
        | Pattern.LITERAL
        | Pattern.MULTILINE
        | Pattern.UNICODE_CASE
        | Pattern.UNIX_LINES
        | Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final EnumSet<ScannerState>
    DEFAULT_STATES = EnumSet.of(ScannerState.DEFAULT, ScannerState.DEFAULT_X);

    private static final EnumSet<ScannerState>
    IN_CHAR_CLASS = EnumSet.of(ScannerState.CHAR_CLASS, ScannerState.CHAR_CLASS_X);

    private static final EnumSet<ScannerState>
    IN_NON_COMMENTS_MODE = EnumSet.of(ScannerState.DEFAULT, ScannerState.CHAR_CLASS);

    private static final EnumSet<ScannerState>
    IN_COMMENTS_MODE = EnumSet.of(ScannerState.DEFAULT_X, ScannerState.CHAR_CLASS_X);

    private static final EnumSet<ScannerState>
    ANY_BUT_IN_QUOTATION = EnumSet.of(
        ScannerState.DEFAULT,
        ScannerState.CHAR_CLASS,
        ScannerState.DEFAULT_X,
        ScannerState.CHAR_CLASS_X
    );

    /**
     * The flags configured at compile time.
     *
     * @see PatternFactory#compile(String, int)
     */
    int flags;

    /**
     * The uncompiled regular expression; only needed for {@link #pattern()} and {@link #toString()}.
     */
    final String pattern;

    /**
     * Internal representation of the parsed regular expression.
     */
    transient Sequence sequence;

    /**
     * The number of capturing groups that this regular expression declares (zero or more).
     */
    transient int groupCount;

    /**
     * The mapping of named capturing group to group number.
     */
    transient Map<String, Integer> namedGroups;

    /**
     * The number of "capturing quantifiers", i.e. those of the form <code>"{m,n}"</code>. The counts of these are
     * available through {@link Matcher#count(int)}.
     */
    transient int capturingQuantifierCount;

    // SUPPRESS CHECKSTYLE JavadocVariable:59
    enum TokenType {

        // Literals.
        /** <var>x</var>  (including surrogate pairs) */
        LITERAL_CHARACTER,
        /** {@code \0}<var>nnn</var> */
        LITERAL_OCTAL,
        /** {@code \x}<var>hh</var> */
        LITERAL_HEXADECIMAL1,
        /** <code>&#92;u</code><var>hhhh</var> */
        LITERAL_HEXADECIMAL2,
        /** <code>&#92;x{</code><var>h...h</var><code>}</code> */
        LITERAL_HEXADECIMAL3,
        /** <code>\N{</code><var>name</var><code>}</code> (Since Java 9) */
        LITERAL_NAMED,
        /** {@code \t \n \r \f \a \e} */
        LITERAL_CONTROL1,
        /** {@code \c}<var>x</var> */
        LITERAL_CONTROL2,

        // Character classes. Notice that "-" is not a metacharacter!
        /** {@code [} */
        LEFT_BRACKET,
        /** {@code ]} */
        RIGHT_BRACKET,
        /** {@code ^} */
        // Not a token, because a '^' in not-first position is a LITERAL.
        /** {@code &&} */
        CC_INTERSECTION,
        /** {@code .} */
        CC_ANY,
        /** {@code \d \D \h \H \s \S \v \V \w \W} */
        CC_PREDEFINED,

        /**
         * Represents all "named" character classes, i.e. those of the form
         * <code>"\p{</code><var>name</var><code>}</code>.
         * <p>
         *   Concrete uses are:
         * </p>
         * <dl>
         *   <dt><code>\p{Lower}</code>, <code>\p{Upper}</code> etc.</dt>
         *   <dd>POSIX character classes</dd>
         *
         *   <dt><code>\p{javaLowerCase}</code>, <code>\p{javaUpperCase}</code>, etc.</dt>
         *   <dd>Java character classes</dd>
         *
         *   <dt><code>\p{Is</code><var>script</var><code>}</code></dt>
         *   <dd>A script character, e.g. <code>\p{IsLatin}</code> for the Latin script</dd>
         *
         *   <dt><code>\p{Is</code><var>property</var><code>}</code></dt>
         *   <dd>A binary property, e.g. <code>\p{IsAlphabetic}</code></dd>
         *
         *   <dt><code>\p{In</code><var>block</var><code>}</code></dt>
         *   <dd>A block character, e.g. <code>\p{InGreek}</code> for the Greek block</dd>
         *
         *   <dt><code>\p{</code><var>category</var><code>}</code></dt>
         *   <dd>
         *     A character category, e.g. <code>\p{Lu}</code> for uppercase letters or <code>\p{Sc}</code> for currency
         *     symbols.
         *   </dd>
         * </dl>
         */
        CC_NAMED,

        // Boundary matchers.
        /** {@code ^} */
        BEGINNING_OF_LINE,
        /** {@code $} */
        END_OF_LINE,
        /** {@code \b} */
        WORD_BOUNDARY,
        /** <code>\b{g}</code> */
        UNICODE_EXTENDED_GRAPHEME_CLUSTER_BOUNDARY,
        /** <code>\B{g}</code> */
        NON_UNICODE_EXTENDED_GRAPHEME_CLUSTER_BOUNDARY,
        /** {@code \B} */
        NON_WORD_BOUNDARY,
        /** {@code \A} */
        BEGINNING_OF_INPUT,
        /** {@code \G} */
        END_OF_PREVIOUS_MATCH,
        /** {@code \Z} */
        END_OF_INPUT_BUT_FINAL_TERMINATOR,
        /** {@code \z} */
        END_OF_INPUT,

        // Linebreak matcher.
        /** {@code \R} */
        LINEBREAK,

        // Unicode extended grapheme matcher.
        /** {@code \X} */
        UNICODE_EXTENDED_GRAPHEME,

        // Quantifiers.
        /** {@code ?} */
        QUESTION,
        /** {@code *} */
        ASTERISK,
        /** {@code +} */
        PLUS,
        /** <code>{min,max}</code> */
        CAPTURING_QUANTIFIER,

        // Logical operators.
        /** {@code X|Y} */
        EITHER_OR,
        /** {@code (} */
        CAPTURING_GROUP,
        /** {@code )} */
        END_GROUP,
        /** {@code \}<var>d</var> */
        CAPTURING_GROUP_BACK_REFERENCE,
        /** {@code \k<}<var>name</var>{@code >} */
        NAMED_CAPTURING_GROUP_BACK_REFERENCE,
        /** {@code (?<}<var>name</var>{@code >} */
        NAMED_CAPTURING_GROUP,
        /** {@code (?:} */
        NON_CAPTURING_GROUP,
        /** {@code (?>} */
        INDEPENDENT_NON_CAPTURING_GROUP,

        // Quotations.
        /** {@code \\ \}<var>x</var> (including surrogate pairs) */
        QUOTED_CHARACTER,
        /** {@code \Q} */
        QUOTATION_BEGIN,
        /** {@code \E} */
        QUOTATION_END,

        // Setting flags.
        /** {@code (?i}<var>dmsuxUc</var>{@code -}<var>idmsuxUc</var>{@code )} */
        MATCH_FLAGS,
        /** {@code (?}<var>idmsuxc</var>{@code -}<var>idmsucx</var>{@code :}<var>X</var>{@code )} */
        MATCH_FLAGS_NON_CAPTURING_GROUP,

        // Lookahead / lookbehind.
        /** {@code (?=} */
        POSITIVE_LOOKAHEAD,
        /** {@code (?!} */
        NEGATIVE_LOOKAHEAD,
        /** {@code (?<=} */
        POSITIVE_LOOKBEHIND,
        /** {@code (?<!} */
        NEGATIVE_LOOKBEHIND,

        COMMENT,

        INVALID_SEQUENCE,
    }

    enum ScannerState {

        DEFAULT, CHAR_CLASS, IN_QUOTATION,

        // Scanner states if "comments" are enabled (Pattern.COMMENTS or "(?x)").
        DEFAULT_X, CHAR_CLASS_X, IN_QUOTATION_X,
    }

    // ==========================================================

    /**
     * Notice that when this constructor is used, the pattern object is <em>not</em> completely initialized - you must
     * call {@link #init(Sequence, int, Map, int)} first!
     */
    Pattern(String pattern, int flags) {

        this.flags   = flags;
        this.pattern = pattern;

        // The following are only initialized by "init()", but we don't want to add any NULL checks.
        this.sequence    = ObjectUtil.almostNull();
        this.namedGroups = ObjectUtil.almostNull();
    }

    void
    init(
        Sequence             sequence,
        int                  groupCount,
        Map<String, Integer> namedGroups,
        int                  capturingQuantifierCount
    ) {
        this.sequence                 = sequence;
        this.groupCount               = groupCount;
        this.namedGroups              = namedGroups;
        this.capturingQuantifierCount = capturingQuantifierCount;
    }

    static
    class RegexScanner extends StatefulScanner<TokenType, ScannerState> {

        int                        groupCount;
        final Map<String, Integer> namedGroups = new HashMap<String, Integer>();
        int                        capturingQuantifierCount;

        RegexScanner() { super(ScannerState.class); }

        RegexScanner(RegexScanner that) {
            super(that);

            // We don't use the "default state" feature, but only EXPLICIT states.
            this.setCurrentState(ScannerState.DEFAULT);
        }
    }

    /**
     * This scanner is intended to be cloned by {@link RegexScanner#RegexScanner(RegexScanner)} when a regex scanner is
     * needed.
     */
    static final RegexScanner REGEX_SCANNER = new RegexScanner();
    static {
        StatefulScanner<TokenType, ScannerState> ss = Pattern.REGEX_SCANNER;

        // SUPPRESS CHECKSTYLE LineLength:210

        // Ignore "#..." comments and whitespace in "comments mode".
        ss.addRule(Pattern.IN_COMMENTS_MODE, "#[^\n\u000B\f\r\u0085\u2028\u2029]*", TokenType.COMMENT).goTo(ss.REMAIN);
        ss.addRule(Pattern.IN_COMMENTS_MODE, "\\s+",                                TokenType.COMMENT).goTo(ss.REMAIN);

        // Characters
        // x         The character x
        // See below: +++
        // \\        The backslash character
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\\\\\",                                  QUOTED_CHARACTER).goTo(ss.REMAIN);
        // \0n       The character with octal value 0n (0 <= n <= 7)
        // \0nn      The character with octal value 0nn (0 <= n <= 7)
        // \0mnn     The character with octal value 0mnn (0 <= m <= 3, 0 <= n <= 7)
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\0(?:[0-3][0-7][0-7]|[0-7][0-7]|[0-7])", LITERAL_OCTAL).goTo(ss.REMAIN);
        // \xhh      The character with hexadecimal value 0xhh
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\x[0-9a-fA-F]{2}",                       LITERAL_HEXADECIMAL1).goTo(ss.REMAIN);
        // /uhhhh    The character with hexadecimal value 0xhhhh
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\u[0-9a-fA-F]{4}",                       LITERAL_HEXADECIMAL2).goTo(ss.REMAIN);
        // \x{h...h} The character with hexadecimal value 0xh...h
        //                                    (Character.MIN_CODE_POINT  <= 0xh...h <=  Character.MAX_CODE_POINT)
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\x\\{.*?(?:}|$)",                        LITERAL_HEXADECIMAL3).goTo(ss.REMAIN);
        // \N{name}  The character with Unicode character name 'name'
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\N\\{.*?(?:}|$)",                        LITERAL_NAMED).goTo(ss.REMAIN);
        // \t        The tab character ('/u0009')
        // \n        The newline (line feed) character ('/u000A')
        // \r        The carriage-return character ('/u000D')
        // \f        The form-feed character ('/u000C')
        // \a        The alert (bell) character ('/u0007')
        // \e        The escape character ('/u001B')
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\[tnrfae]",                              LITERAL_CONTROL1).goTo(ss.REMAIN);
        // \cx The control character corresponding to x
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\c[A-Za-z]",                             LITERAL_CONTROL2).goTo(ss.REMAIN);

        // Character classes
        // [abc]       a, b, or c (simple class)
        ss.addRule(Pattern.IN_NON_COMMENTS_MODE, "\\[", LEFT_BRACKET).push(ScannerState.CHAR_CLASS);
        ss.addRule(ScannerState.CHAR_CLASS,      "]",   RIGHT_BRACKET).pop();
        ss.addRule(Pattern.IN_COMMENTS_MODE,     "\\[", LEFT_BRACKET).push(ScannerState.CHAR_CLASS_X);
        ss.addRule(ScannerState.CHAR_CLASS_X,    "]",   RIGHT_BRACKET).pop();
        // [^abc]      Any character except a, b, or c (negation)
        // '^' is not a meta character, because a '^' in not-first-position is a LITAERAL
        // [a-zA-Z]    a through z or A through Z, inclusive (range) -- "-" is not a metacharacter!
        //ss.addRule(Pattern.IN_CHAR_CLASS,     "-",   CC_RANGE,        ss.REMAIN);
        // [a-d[m-p]]  a through d, or m through p: [a-dm-p] (union)
        // [a-z&&[def]]    d, e, or f (intersection)
        // [a-z&&[^bc]]    a through z, except for b and c: [ad-z] (subtraction)
        // [a-z&&[^m-p]]   a through z, and not m through p: [a-lq-z] (subtraction)
        ss.addRule(Pattern.IN_CHAR_CLASS,     "&&",  CC_INTERSECTION).goTo(ss.REMAIN);

        // Predefined character classes
        // .   Any character (may or may not match line terminators)
        ss.addRule(Pattern.DEFAULT_STATES, "\\.",    CC_ANY).goTo(ss.REMAIN);
        // \d  A digit: [0-9]
        // \D  A non-digit: [^0-9]
        // \h  A horizontal whitespace character: [ \t\xA0/u1680/u180e/u2000-/u200a/u202f/u205f/u3000]
        // \H  A non-horizontal whitespace character: [^\h]
        // \s  A whitespace character: [ \t\n\x0B\f\r]
        // \S  A non-whitespace character: [^\s]
        // \v  A vertical whitespace character: [\n\x0B\f\r\x85/u2028/u2029]
        // \V  A non-vertical whitespace character: [^\v]
        // \w  A word character: [a-zA-Z_0-9]
        // \W  A non-word character: [^\w]
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\[dDhHsSvVwW]", CC_PREDEFINED).goTo(ss.REMAIN);

        // POSIX character classes (US-ASCII only)
        //   \p{Lower}   A lower-case alphabetic character: [a-z]
        //   \p{Upper}   An upper-case alphabetic character:[A-Z]
        //   \p{ASCII}   All ASCII:[\x00-\x7F]
        //   \p{Alpha}   An alphabetic character:[\p{Lower}\p{Upper}]
        //   \p{Digit}   A decimal digit: [0-9]
        //   \p{Alnum}   An alphanumeric character:[\p{Alpha}\p{Digit}]
        //   \p{Punct}   Punctuation: One of !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
        //   \p{Graph}   A visible character: [\p{Alnum}\p{Punct}]
        //   \p{Print}   A printable character: [\p{Graph}\x20]
        //   \p{Blank}   A space or a tab: [ \t]
        //   \p{Cntrl}   A control character: [\x00-\x1F\x7F]
        //   \p{XDigit}  A hexadecimal digit: [0-9a-fA-F]
        //   \p{Space}   A whitespace character: [ \t\n\x0B\f\r]
        //
        // java.lang.Character classes (simple java character type)
        //   \p{javaLowerCase}   Equivalent to java.lang.Character.isLowerCase()
        //   \p{javaUpperCase}   Equivalent to java.lang.Character.isUpperCase()
        //   \p{javaWhitespace}  Equivalent to java.lang.Character.isWhitespace()
        //   \p{javaMirrored}    Equivalent to java.lang.Character.isMirrored()
        //
        // Classes for Unicode scripts, blocks, categories and binary properties
        //   \p{IsLatin}        A Latin script character (script)
        //   \p{IsAlphabetic}   An alphabetic character (binary property)
        //   \p{InGreek}        A character in the Greek block (block)
        //   \p{Lu}             An uppercase letter (category)
        //   \p{Sc}             A currency symbol
        //   \P{InGreek}        Any character except one in the Greek block (negation)
        //   [\p{L}&&[^\p{Lu}]] Any letter except an uppercase letter (subtraction)
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\([pP])\\{([^}]+)}", CC_NAMED).goTo(ss.REMAIN);
        // Undocumented JUR feature: Single-letter named character classes also work WITHOUT the curly braces.
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\([pP])(\\w)",       CC_NAMED).goTo(ss.REMAIN);

        // Boundary matchers
        // ^      The beginning of a line
        ss.addRule(Pattern.DEFAULT_STATES, "\\^",          BEGINNING_OF_LINE).goTo(ss.REMAIN);
        // $      The end of a line
        ss.addRule(Pattern.DEFAULT_STATES, "\\$",          END_OF_LINE).goTo(ss.REMAIN);
        // \b{g}  A grapheme cluster boundary
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\b\\{g\\}", UNICODE_EXTENDED_GRAPHEME_CLUSTER_BOUNDARY).goTo(ss.REMAIN);
        // \B{g}  A non-grapheme cluster boundary
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\B\\{g\\}", NON_UNICODE_EXTENDED_GRAPHEME_CLUSTER_BOUNDARY).goTo(ss.REMAIN);
        // \b     A word boundary
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\b",        WORD_BOUNDARY).goTo(ss.REMAIN);
        // \B     A non-word boundary
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\B",        NON_WORD_BOUNDARY).goTo(ss.REMAIN);
        // \A     The beginning of the input
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\A",        BEGINNING_OF_INPUT).goTo(ss.REMAIN);
        // \G     The end of the previous match
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\G",        END_OF_PREVIOUS_MATCH).goTo(ss.REMAIN);
        // \Z     The end of the input but for the final terminator, if any
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\Z",        END_OF_INPUT_BUT_FINAL_TERMINATOR).goTo(ss.REMAIN);
        // \z     The end of the input
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\z",        END_OF_INPUT).goTo(ss.REMAIN);

        // Linebreak matcher
        // \R  Any Unicode linebreak sequence, is equivalent to
        //                          /u000D/u000A|[/u000A/u000B/u000C/u000D/u0085/u2028/u2029]
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\R", LINEBREAK).goTo(ss.REMAIN);

        // Unicode Extended Grapheme matcher
        // \X   Any Unicode extended grapheme cluster
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\X", UNICODE_EXTENDED_GRAPHEME).goTo(ss.REMAIN);

        // Greedy quantifiers
        //   X?         X, once or not at all
        //   X*         X, zero or more times
        //   X+         X, one or more times
        //   X{n}       X, exactly n times
        //   X{min,}    X, at least min times
        //   X{min,max} X, at least min but not more than max times
        // Reluctant quantifiers
        //   X??         X, once or not at all
        //   X*?         X, zero or more times
        //   X+?         X, one or more times
        //   X{n}?       X, exactly n times (identical with X{n})
        //   X{min,}?    X, at least min times
        //   X{min,max}? X, at least min but not more than max times
        // Possessive quantifiers
        //   X?+         X, once or not at all
        //   X*+         X, zero or more times
        //   X++         X, one or more times
        //   X{n}+       X, exactly n times (identical with X{n})
        //   X{min,}+    X, at least min times
        //   X{min,max}+ X, at least min but not more than max times
        ss.addRule(Pattern.DEFAULT_STATES, "\\?", QUESTION).goTo(ss.REMAIN);
        ss.addRule(Pattern.DEFAULT_STATES, "\\*", ASTERISK).goTo(ss.REMAIN);
        ss.addRule(Pattern.DEFAULT_STATES, "\\+", PLUS).goTo(ss.REMAIN);
        ss.addRule(
            Pattern.DEFAULT_STATES,
            "\\{(\\d+)(?:(,)(\\d+)?)?}", // $1=min, $2="," (opt), $3=max (opt)
            TokenType.CAPTURING_QUANTIFIER
        ).goTo(ss.REMAIN);

        // Logical operators
        // XY  X followed by Y
        // X|Y Either X or Y
        ss.addRule(Pattern.DEFAULT_STATES, "\\|", EITHER_OR).goTo(ss.REMAIN);
        // (X) X, as a capturing group
        ss.addRule(ScannerState.DEFAULT,   "\\((?!\\?)",      CAPTURING_GROUP).goTo(ss.REMAIN);
        ss.addRule(ScannerState.DEFAULT_X, "\\(\\s*+(?!\\?)", CAPTURING_GROUP).goTo(ss.REMAIN);
        ss.addRule(Pattern.DEFAULT_STATES, "\\)",             END_GROUP).goTo(ss.REMAIN);

        // Back references
        // \n       Whatever the nth capturing group matched
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\(\\d+)",                    CAPTURING_GROUP_BACK_REFERENCE).goTo(ss.REMAIN);
        // \k<name> Whatever the named-capturing group "name" matched
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\k<([A-Za-z][A-Za-z0-9]*)>", NAMED_CAPTURING_GROUP_BACK_REFERENCE).goTo(ss.REMAIN);
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\k",                         INVALID_SEQUENCE);

        // Quotation
        // \   Nothing, but quotes the following character
        ss.addRule(Pattern.ANY_BUT_IN_QUOTATION, "\\\\[^QE]", QUOTED_CHARACTER).goTo(ss.REMAIN);
        // \Q  Nothing, but quotes all characters until \E
        ss.addRule(Pattern.IN_NON_COMMENTS_MODE, "\\\\Q",     QUOTATION_BEGIN).push(ScannerState.IN_QUOTATION);
        ss.addRule(Pattern.IN_COMMENTS_MODE,     "\\\\Q",     QUOTATION_BEGIN).push(ScannerState.IN_QUOTATION_X);
        // \E  Nothing, but ends quoting started by \Q
        ss.addRule(ScannerState.IN_QUOTATION,    "\\\\E",     QUOTATION_END).pop();
        ss.addRule(ScannerState.IN_QUOTATION_X,  "\\\\E",     QUOTATION_END).pop();
        ss.addRule(ScannerState.IN_QUOTATION,    ".",         LITERAL_CHARACTER).goTo(ScannerState.IN_QUOTATION);
        ss.addRule(ScannerState.IN_QUOTATION_X,  ".",         LITERAL_CHARACTER).goTo(ScannerState.IN_QUOTATION_X);

        // Special constructs (named-capturing and non-capturing)
        // (?<name>X)          X, as a named-capturing group
        ss.addRule(ScannerState.DEFAULT,   "\\(\\?<([A-Za-z][A-Za-z0-9]*)>",                        NAMED_CAPTURING_GROUP).goTo(ss.REMAIN);
        ss.addRule(ScannerState.DEFAULT_X, "\\(\\s*+\\?<\\s*+([A-Za-z][A-Za-z0-9]*)>",              NAMED_CAPTURING_GROUP).goTo(ss.REMAIN);
        // (?:X)               X, as a non-capturing group
        ss.addRule(ScannerState.DEFAULT,   "\\(\\?:",                                               NON_CAPTURING_GROUP).goTo(ss.REMAIN);
        ss.addRule(ScannerState.DEFAULT_X, "\\(\\s*+\\?\\s*+:",                                     NON_CAPTURING_GROUP).goTo(ss.REMAIN);
        // (?idmsuxU-idmsuxU)  Nothing, but turns match flags i d m s u x U on - off
        ss.addRule(ScannerState.DEFAULT,   "\\(\\?[idmsuxUc]*(?:-[idmsuxUc]+)?\\)",                 MATCH_FLAGS).goTo(ss.REMAIN);
        ss.addRule(ScannerState.DEFAULT_X, "\\(\\s*+\\?\\s*+[idmsuxUc]*(?:-[idmsuxUc]+)?\\)",       MATCH_FLAGS).goTo(ss.REMAIN);
        // (?idmsux-idmsux:X)  X, as a non-capturing group with the given flags i d m s u x on - off
        ss.addRule(ScannerState.DEFAULT,   "\\(\\?[idmsuxc]*(?:-[idmsuxc]*)?:",                     MATCH_FLAGS_NON_CAPTURING_GROUP).goTo(ss.REMAIN);
        ss.addRule(ScannerState.DEFAULT_X, "\\(\\s*+\\?\\s*+[idmsuxUc]*(?:-[idmsuxUc]*)?\\s*+:",    MATCH_FLAGS_NON_CAPTURING_GROUP).goTo(ss.REMAIN);
        // (?=X)               X, via zero-width positive lookahead
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?=",                                               POSITIVE_LOOKAHEAD).goTo(ss.REMAIN);
        // (?!X)               X, via zero-width negative lookahead
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?!",                                               NEGATIVE_LOOKAHEAD).goTo(ss.REMAIN);
        // (?<=X)              X, via zero-width positive lookbehind
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?<=",                                              POSITIVE_LOOKBEHIND).goTo(ss.REMAIN);
        // (?<!X)              X, via zero-width negative lookbehind
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?<!",                                              NEGATIVE_LOOKBEHIND).goTo(ss.REMAIN);
        // (?>X)               X, as an independent, non-capturing group
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?>",                                               INDEPENDENT_NON_CAPTURING_GROUP).goTo(ss.REMAIN);
        ss.addRule(Pattern.DEFAULT_STATES, "\\(",                                                   INVALID_SEQUENCE);

        // Any literal character. Notice that different sets of metacharacters are in effect in sequences and character
        // classes.
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\[A-Za-z]", INVALID_SEQUENCE);
        ss.addRule(Pattern.DEFAULT_STATES, "[^{\\\\(*?+]", LITERAL_CHARACTER).goTo(ss.REMAIN);
        ss.addRule(Pattern.IN_CHAR_CLASS,  "[^\\\\]",      LITERAL_CHARACTER).goTo(ss.REMAIN);
    }

    /**
     * This scanner is intended to be cloned by {@link RegexScanner#RegexScanner(RegexScanner)} when a literal scanner
     * is needed.
     *
     * @see #LITERAL
     */
    static final RegexScanner LITERAL_SCANNER = new RegexScanner();
    static {
        Pattern.LITERAL_SCANNER.addRule(
            Pattern.LITERAL_SCANNER.ANY_STATE,
            ".",
            LITERAL_CHARACTER
        ).goTo(Pattern.LITERAL_SCANNER.REMAIN);
    }

    /**
     * Equivalent with {@link PatternFactory#compile(String) PatternFactory.compile}{@code (}<var>regex</var>{@code )}.
     *
     * @see java.util.regex.Pattern#compile(String)
     */
    public static Pattern
    compile(String regex) { return PatternFactory.INSTANCE.compile(regex); }

    /**
     * Equivalent with {@link PatternFactory#compile(String) PatternFactory.compile}{@code (}<var>regex</var>{@code ,}
     * <var>flags</var>{@code )}.
     *
     * @see java.util.regex.Pattern#compile(String, int)
     */
    public static Pattern
    compile(String regex, int flags) { return PatternFactory.INSTANCE.compile(regex, flags); }

    /**
     * @see java.util.regex.Pattern#pattern()
     */
    @Override public String
    pattern() { return this.pattern; }

    /**
     * Returns the parsed {@link Sequence} in an internal syntax. This is useful for testing how a pattern was
     * compiled, e.g., whether certain optimizations have taken place.
     */
    public String
    sequenceToString() { return this.sequence.toString().replace(" . terminal", ""); }

    /**
     * @return The uncompiled regular expression
     * @see    java.util.regex.Pattern#toString()
     * @see    #pattern()
     */
    @Override public String
    toString() { return this.pattern; }

    /**
     * @see java.util.regex.Pattern#matcher(CharSequence)
     */
    @Override public Matcher
    matcher(CharSequence subject) { return new MatcherImpl(this, subject); }

    /**
     * @see java.util.regex.Pattern#flags()
     */
    @Override public int
    flags() { return this.flags; }

    /**
     * Equivalent with {@link #compile(String) Pattern.compile(regex)}{@code .}{@link #matcher(CharSequence)
     * matcher(input)}{@code .}{@link Matcher#matches() matches()}.
     * <p>
     *   If a pattern is to be used multiple times, compiling it once and reusing it will be more efficient than
     *   invoking this method each time.
     * </p>
     *
     * @see java.util.regex.Pattern#matches(String, CharSequence)
     */
    public static boolean
    matches(String regex, CharSequence input) {
        return PatternFactory.INSTANCE.compile(regex).matcher(input).matches();
    }

    /**
     * @see java.util.regex.Pattern#split(CharSequence)
     */
    @Override public String[]
    split(CharSequence input) { return this.split(input, 0); }

    /**
     * @see java.util.regex.Pattern#split(CharSequence)
     */
    @Override public String[]
    split(CharSequence input, int limit) {

        Matcher m = this.matcher(input);
        if (!m.find()) return new String[] { input.toString() };

        // "A zero-width match at the beginning however never produces such empty leading substring."
        if (m.end() == 0) {
            if (!m.find()) return new String[] { input.toString() };
        }

        List<String> result = new ArrayList<String>();
        int          eopm   = 0; // "End of previous match"
        for (int i = 0;; i++) {

            if (limit > 0 && i >= limit - 1) {
                result.add(input.subSequence(eopm, input.length()).toString());
                break;
            }

            result.add(input.subSequence(eopm, m.start()).toString());
            eopm = m.end();

            // "If n is zero then [...] trailing empty strings will be discarded."
            if (limit == 0 && eopm == input.length()) break;

            if (!m.find()) {
                result.add(input.subSequence(eopm, input.length()).toString());
                break;
            }
        }

        // "If n is zero then [...] trailing empty strings will be discarded."
        if (limit == 0) {
            for (int i = result.size() - 1; i >= 0; i--) {
                if (!result.get(i).isEmpty()) break;
                result.remove(i);
            }
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * @see java.util.regex.Pattern#quote(String)
     */
    public static String
    quote(String s) { return java.util.regex.Pattern.quote(s); }

    @Override public boolean
    matches(CharSequence subject) { return this.matches(subject, 0, subject.length()); }

    @Override public boolean
    matches(CharSequence subject, int regionStart) { return this.matches(subject, regionStart, subject.length()); }

    @Override public boolean
    matches(CharSequence subject, int regionStart, int regionEnd) {

        if (regionStart < 0)              throw new IndexOutOfBoundsException();
        if (regionEnd < regionStart)      throw new IndexOutOfBoundsException();
        if (regionEnd > subject.length()) throw new IndexOutOfBoundsException();

        int regionLength = regionEnd - regionStart;

        // Optimization: Test whether there are enough characters left so that the sequence can possibly match.
        if (this.sequence.minMatchLength > regionLength) return false;

        // Optimization: Test whether the sequence can possibly match all remaining chars.
        if (this.sequence.maxMatchLength < regionLength) return false;

        MatcherImpl mi = new MatcherImpl(this, subject);
        mi.regionStart = regionStart;
        mi.regionEnd   = regionEnd;
        mi.offset      = regionStart;
        mi.end         = MatcherImpl.End.END_OF_REGION;

        return this.sequence.matches(mi);
    }

    @Override public Predicate<String>
    asPredicate() { return subject -> matcher(subject).find(); }

    @Override public Predicate<String>
    asMatchPredicate() { return subject -> matcher(subject).matches(); } // Retrofitted with JRE 8

    @Override public Stream<String>
    splitAsStream(final CharSequence input) {

        class MatcherIterator implements Iterator<String> {

            private final Matcher matcher = Pattern.this.matcher(input);

            // The start position of the next sub-sequence of input
            // when current == input.length there are no more elements
            private int current;

            // null if the next element, if any, needs to obtained
            @Nullable private String nextElement;

            // > 0 if there are N next empty elements
            private int emptyElementCount = input.length() == 0 ? 1 : 0;

            @Override public String
            next() {

                if (!hasNext()) throw new NoSuchElementException();

                if (this.emptyElementCount > 0) {
                    this.emptyElementCount--;
                    return "";
                }

                String n = this.nextElement;
                assert n != null;
                this.nextElement = null;
                return n;
            }

            @Override public boolean
            hasNext() {

                if (this.nextElement != null || this.emptyElementCount > 0) return true;

                if (this.current == input.length()) return false;

                // Consume the next matching element
                // Count sequence of matching empty elements
                while (this.matcher.find()) {

                    String ne = (this.nextElement = input.subSequence(this.current, this.matcher.start()).toString());
                    this.current = this.matcher.end();
                    if (!ne.isEmpty()) return true;

                    if (this.current > 0) this.emptyElementCount++;
                }

                // Consume last matching element
                String ne = (this.nextElement = input.subSequence(this.current, input.length()).toString());
                this.current = input.length();
                if (!ne.isEmpty()) return true;

                // Ignore a terminal sequence of matching empty elements
                this.emptyElementCount = 0;
                this.nextElement       = null;
                return false;
            }
        }

        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(new MatcherIterator(), Spliterator.ORDERED | Spliterator.NONNULL), // spliterator
            false                                                                                                  // parallel
        );
    }

    private void
    readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {

        ois.defaultReadObject();

        PatternFactory.compile2(this);
    }
}
