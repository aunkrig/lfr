
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

import static de.unkrig.lfr.core.Pattern.TokenType.BEGINNING_OF_INPUT;
import static de.unkrig.lfr.core.Pattern.TokenType.BEGINNING_OF_LINE;
import static de.unkrig.lfr.core.Pattern.TokenType.CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.CAPTURING_GROUP_BACK_REFERENCE;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_ANY;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_INTERSECTION;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_JAVA;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_NEGATION;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_POSIX;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_PREDEFINED;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_UNICODE_BLOCK;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_UNICODE_CATEGORY;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_UNICODE_SCRIPT_OR_BINARY_PROPERTY;
import static de.unkrig.lfr.core.Pattern.TokenType.EITHER_OR;
import static de.unkrig.lfr.core.Pattern.TokenType.END_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.END_OF_INPUT;
import static de.unkrig.lfr.core.Pattern.TokenType.END_OF_INPUT_BUT_FINAL_TERMINATOR;
import static de.unkrig.lfr.core.Pattern.TokenType.END_OF_LINE;
import static de.unkrig.lfr.core.Pattern.TokenType.END_OF_PREVIOUS_MATCH;
import static de.unkrig.lfr.core.Pattern.TokenType.GREEDY_QUANTIFIER;
import static de.unkrig.lfr.core.Pattern.TokenType.INDEPENDENT_NON_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.LEFT_BRACKET;
import static de.unkrig.lfr.core.Pattern.TokenType.LINEBREAK_MATCHER;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_CHARACTER;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_CONTROL;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_HEXADECIMAL;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_OCTAL;
import static de.unkrig.lfr.core.Pattern.TokenType.MATCH_FLAGS;
import static de.unkrig.lfr.core.Pattern.TokenType.MATCH_FLAGS_NON_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.NAMED_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.NAMED_CAPTURING_GROUP_BACK_REFERENCE;
import static de.unkrig.lfr.core.Pattern.TokenType.NEGATIVE_LOOKAHEAD;
import static de.unkrig.lfr.core.Pattern.TokenType.NEGATIVE_LOOKBEHIND;
import static de.unkrig.lfr.core.Pattern.TokenType.NON_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.NON_WORD_BOUNDARY;
import static de.unkrig.lfr.core.Pattern.TokenType.POSITIVE_LOOKAHEAD;
import static de.unkrig.lfr.core.Pattern.TokenType.POSITIVE_LOOKBEHIND;
import static de.unkrig.lfr.core.Pattern.TokenType.POSSESSIVE_QUANTIFIER;
import static de.unkrig.lfr.core.Pattern.TokenType.QUOTATION_BEGIN;
import static de.unkrig.lfr.core.Pattern.TokenType.QUOTATION_END;
import static de.unkrig.lfr.core.Pattern.TokenType.QUOTED_CHARACTER;
import static de.unkrig.lfr.core.Pattern.TokenType.RELUCTANT_QUANTIFIER;
import static de.unkrig.lfr.core.Pattern.TokenType.RIGHT_BRACKET;
import static de.unkrig.lfr.core.Pattern.TokenType.WORD_BOUNDARY;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unkrig.commons.text.scanner.StatefulScanner;

/**
 * A drop-in replacement for {@link java.util.regex.Pattern}.
 */
public
class Pattern implements de.unkrig.ref4j.Pattern {

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
    DEFAULT_STATES = EnumSet.of(
        ScannerState.DEFAULT,
        ScannerState.DEFAULT_X
    );
    private static final EnumSet<ScannerState>
    IN_CHAR_CLASS = EnumSet.of(
        ScannerState.CHAR_CLASS1,
        ScannerState.CHAR_CLASS2,
        ScannerState.CHAR_CLASS3,
        ScannerState.CHAR_CLASS1_X,
        ScannerState.CHAR_CLASS2_X,
        ScannerState.CHAR_CLASS3_X
    );
    private static final EnumSet<ScannerState>
    IN_COMMENTS_MODE = EnumSet.of(
        ScannerState.DEFAULT_X,
        ScannerState.CHAR_CLASS1_X,
        ScannerState.CHAR_CLASS2_X,
        ScannerState.CHAR_CLASS3_X
    );

    /**
     * The flags configured at compile time.
     *
     * @see #compile(String, int)
     */
    int flags;

    /**
     * The uncompiled regular expression; only needed for {@link #pattern()} and {@link #toString()}.
     */
    final String pattern;

    /**
     * Internal representation of the parsed regular expression.
     */
    final Sequence sequence;

    /**
     * The number of capturing groups that this regular expression declares (zero or more).
     */
    final int groupCount;

    /**
     * The mapping of named capturing group to group number.
     */
    final Map<String, Integer> namedGroups;

    // SUPPRESS CHECKSTYLE JavadocVariable:59
    enum TokenType {

        // Literals.
        LITERAL_CHARACTER,
        /** {@code \0}<var>nnn</var> */
        LITERAL_OCTAL,
        /** {@code \x}<var>hh</var> <code>&#92;u</code><var>hhhh</var> <code>&#92;u{</code><var>hhhh</var><code>}</code> */ // SUPPRESS CHECKSTYLE LineLength
        LITERAL_HEXADECIMAL,
        /** {@code \t \n \r \f \a \e \c}<var>x</var> */
        LITERAL_CONTROL,

        // Character classes. Notice that "-" is not a metacharacter!
        /** {@code [} */
        LEFT_BRACKET,
        /** {@code ]} */
        RIGHT_BRACKET,
        /** {@code ^} */
        CC_NEGATION,
        /** {@code &&} */
        CC_INTERSECTION,
        /** {@code .} */
        CC_ANY,
        /** {@code \d \D \h \H \s \S \v \V \w \W} */
        CC_PREDEFINED,
        /** <code>\p{Lower}</code>, <code>\p{Upper}</code> etc. */
        CC_POSIX,
        /**
         * <code>\p{javaLowerCase}</code>
         * <code>\p{javaUpperCase}</code>
         * <code>\p{javaWhitespace}</code>
         * <code>\p{javaMirrored}</code>
         */
        CC_JAVA,
        /**
         * <code>\p{Is</code><var>script</var><code>}</code> A script character, e.g. <code>\p{IsLatin}</code> for the
         * Latin script.
         * <br />
         * <code>\p{Is</code><var>property</var><code>}</code> A binary property, e.g. <code>\p{IsAlphabetic}</code>.
         */
        CC_UNICODE_SCRIPT_OR_BINARY_PROPERTY,
        /**
         * <code>\p{In</code><var>block</var><code>}</code> A block character, e.g. <code>\p{InGreek}</code> for the
         * Greek block.
         */
        CC_UNICODE_BLOCK,
        /**
         * <code>\p{</code><var>cc</var><code>}</code> A character category, e.g. <code>\p{Lu}</code> for uppercase
         * letters or <code>\p{Sc}</code> for currency symbols.
         */
        CC_UNICODE_CATEGORY,

        // Matchers.
        /** {@code ^} */
        BEGINNING_OF_LINE,
        /** {@code $} */
        END_OF_LINE,
        /** {@code \b} */
        WORD_BOUNDARY,
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
        /** {@code \R} */
        LINEBREAK_MATCHER,

        // Quantifiers.
        /** <code>X? X* X+ X{n} X{min,} X{min,max}</code> */
        GREEDY_QUANTIFIER,
        /** <code>X?? X*? X+? X{n}? X{min,}? X{min,max}?</code> */
        RELUCTANT_QUANTIFIER,
        /** <code>X?+ X*+ X++ X{n}+ X{min,}+ X{min,max}+</code> */
        POSSESSIVE_QUANTIFIER,

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
        /** {@code \\ \}<var>x</var> */
        QUOTED_CHARACTER,
        /** {@code \Q} */
        QUOTATION_BEGIN,
        /** {@code \E} */
        QUOTATION_END,

        // Setting flags.
        /** {@code (?i}<var>dmsuxU</var>{@code -}<var>idmsuxU</var>{@code )} */
        MATCH_FLAGS,
        /** {@code (?}<var>idmsux</var>{@code -}<var>idmsux</var>{@code :}<var>X</var>{@code )} */
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
    }

    enum ScannerState {

        // Allow char classes ("[...]") nested up three levels.
        DEFAULT, CHAR_CLASS1, CHAR_CLASS2, CHAR_CLASS3, IN_QUOTATION,

        // Scanner states if "comments" are enabled (Pattern.COMMENTS or "(?x)").
        DEFAULT_X, CHAR_CLASS1_X, CHAR_CLASS2_X, CHAR_CLASS3_X, IN_QUOTATION_X,
    }

    // ==========================================================

    Pattern(String pattern, int flags, Sequence sequence, int groupCount, Map<String, Integer> namedGroups) {

        this.flags       = flags;
        this.pattern     = pattern;
        this.namedGroups = namedGroups;

        sequence.concat(new Sequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {
                return matcher.end == MatcherImpl.End.ANY || offset >= matcher.regionEnd ? offset : -1;
            }

            @Override public Sequence
            concat(Sequence that) { throw new UnsupportedOperationException(); }

            @Override public Sequence
            reverse() { return this; }

            @Override public boolean
            find(MatcherImpl matcherImpl, int start) { throw new UnsupportedOperationException(); }

            @Override public String
            toString() { return "end"; }
        });

        this.sequence   = sequence;
        this.groupCount = groupCount;
    }

    static
    class RegexScanner extends StatefulScanner<TokenType, ScannerState> {

        int                        groupCount;
        final Map<String, Integer> namedGroups = new HashMap<String, Integer>();

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

        // Ignore "#..." comments and whitespace in "comments mode".
        ss.addRule(Pattern.IN_COMMENTS_MODE, "#[^\n\u000B\f\r\u0085\u2028\u2029]*", TokenType.COMMENT, ss.REMAIN);
        ss.addRule(Pattern.IN_COMMENTS_MODE, "\\s+",                                TokenType.COMMENT, ss.REMAIN);

        // Characters
        // x         The character x
        // See below: +++
        // \\        The backslash character
        ss.addRule(ss.ANY_STATE, "\\\\\\\\",                                  QUOTED_CHARACTER,    ss.REMAIN);
        // \0n       The character with octal value 0n (0 <= n <= 7)
        // \0nn      The character with octal value 0nn (0 <= n <= 7)
        // \0mnn     The character with octal value 0mnn (0 <= m <= 3, 0 <= n <= 7)
        ss.addRule(ss.ANY_STATE, "\\\\0(?:[0-3][0-7][0-7]|[0-7][0-7]|[0-7])", LITERAL_OCTAL,       ss.REMAIN);
        // \xhh      The character with hexadecimal value 0xhh
        ss.addRule(ss.ANY_STATE, "\\\\u[0-9a-fA-F]{4}",                       LITERAL_HEXADECIMAL, ss.REMAIN);
        // /uhhhh    The character with hexadecimal value 0xhhhh
        ss.addRule(ss.ANY_STATE, "\\\\x[0-9a-fA-F]{2}",                       LITERAL_HEXADECIMAL, ss.REMAIN);
        // \x{h...h} The character with hexadecimal value 0xh...h
        //                                    (Character.MIN_CODE_POINT  <= 0xh...h <=  Character.MAX_CODE_POINT)
        ss.addRule(ss.ANY_STATE, "\\\\x\\{[0-9a-fA-F]+}",                     LITERAL_HEXADECIMAL, ss.REMAIN);
        // \t        The tab character ('/u0009')
        // \n        The newline (line feed) character ('/u000A')
        // \r        The carriage-return character ('/u000D')
        // \f        The form-feed character ('/u000C')
        // \a        The alert (bell) character ('/u0007')
        // \e        The escape character ('/u001B')
        ss.addRule(ss.ANY_STATE, "\\\\[tnrfae]",                              LITERAL_CONTROL,     ss.REMAIN);
        // \cx The control character corresponding to x
        ss.addRule(ss.ANY_STATE, "\\\\c[A-Za-z]",                             LITERAL_CONTROL,     ss.REMAIN);

        // Character classes
        // [abc]       a, b, or c (simple class)
        ss.addRule(ScannerState.DEFAULT,      "\\[",  LEFT_BRACKET,    ScannerState.CHAR_CLASS1);
        ss.addRule(ScannerState.CHAR_CLASS1,  "\\[",  LEFT_BRACKET,    ScannerState.CHAR_CLASS2);
        ss.addRule(ScannerState.CHAR_CLASS2,  "\\[",  LEFT_BRACKET,    ScannerState.CHAR_CLASS3);
        ss.addRule(ScannerState.CHAR_CLASS3,  "]",    RIGHT_BRACKET,   ScannerState.CHAR_CLASS2);
        ss.addRule(ScannerState.CHAR_CLASS2,  "]",    RIGHT_BRACKET,   ScannerState.CHAR_CLASS1);
        ss.addRule(ScannerState.CHAR_CLASS1,  "]",    RIGHT_BRACKET,   ScannerState.DEFAULT);
        ss.addRule(ScannerState.DEFAULT_X,     "\\[", LEFT_BRACKET,    ScannerState.CHAR_CLASS1_X);
        ss.addRule(ScannerState.CHAR_CLASS1_X, "\\[", LEFT_BRACKET,    ScannerState.CHAR_CLASS2_X);
        ss.addRule(ScannerState.CHAR_CLASS2_X, "\\[", LEFT_BRACKET,    ScannerState.CHAR_CLASS3_X);
        ss.addRule(ScannerState.CHAR_CLASS3_X, "]",   RIGHT_BRACKET,   ScannerState.CHAR_CLASS2_X);
        ss.addRule(ScannerState.CHAR_CLASS2_X, "]",   RIGHT_BRACKET,   ScannerState.CHAR_CLASS1_X);
        ss.addRule(ScannerState.CHAR_CLASS1_X, "]",   RIGHT_BRACKET,   ScannerState.DEFAULT_X);
        // [^abc]      Any character except a, b, or c (negation)
        ss.addRule(Pattern.IN_CHAR_CLASS,     "\\^", CC_NEGATION,     ss.REMAIN);
        // [a-zA-Z]    a through z or A through Z, inclusive (range) -- "-" is not a metacharacter!
        //ss.addRule(Pattern.IN_CHAR_CLASS,     "-",   CC_RANGE,        ss.REMAIN);
        // [a-d[m-p]]  a through d, or m through p: [a-dm-p] (union)
        // [a-z&&[def]]    d, e, or f (intersection)
        // [a-z&&[^bc]]    a through z, except for b and c: [ad-z] (subtraction)
        // [a-z&&[^m-p]]   a through z, and not m through p: [a-lq-z] (subtraction)
        ss.addRule(Pattern.IN_CHAR_CLASS,     "&&",  CC_INTERSECTION, ss.REMAIN);

        // Predefined character classes
        // .   Any character (may or may not match line terminators)
        ss.addRule(Pattern.DEFAULT_STATES, "\\.",    CC_ANY,        ss.REMAIN);
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
        ss.addRule(ss.ANY_STATE, "\\\\[dDhHsSvVwW]", CC_PREDEFINED, ss.REMAIN);

        // POSIX character classes (US-ASCII only)
        // \p{Lower}   A lower-case alphabetic character: [a-z]
        // \p{Upper}   An upper-case alphabetic character:[A-Z]
        // \p{ASCII}   All ASCII:[\x00-\x7F]
        // \p{Alpha}   An alphabetic character:[\p{Lower}\p{Upper}]
        // \p{Digit}   A decimal digit: [0-9]
        // \p{Alnum}   An alphanumeric character:[\p{Alpha}\p{Digit}]
        // \p{Punct}   Punctuation: One of !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
        // \p{Graph}   A visible character: [\p{Alnum}\p{Punct}]
        // \p{Print}   A printable character: [\p{Graph}\x20]
        // \p{Blank}   A space or a tab: [ \t]
        // \p{Cntrl}   A control character: [\x00-\x1F\x7F]
        // \p{XDigit}  A hexadecimal digit: [0-9a-fA-F]
        // \p{Space}   A whitespace character: [ \t\n\x0B\f\r]
        ss.addRule(
            ss.ANY_STATE,
            "\\\\[pP]\\{(?:Lower|Upper|ASCII|Alpha|Digit|Alnum|Punct|Graph|Print|Blank|Cntrl|XDigit|Space)}",
            CC_POSIX,
            ss.REMAIN
        );

        // java.lang.Character classes (simple java character type)
        // \p{javaLowerCase}   Equivalent to java.lang.Character.isLowerCase()
        // \p{javaUpperCase}   Equivalent to java.lang.Character.isUpperCase()
        // \p{javaWhitespace}  Equivalent to java.lang.Character.isWhitespace()
        // \p{javaMirrored}    Equivalent to java.lang.Character.isMirrored()
        ss.addRule(
            ss.ANY_STATE,
            "\\\\[pP]\\{(?:javaLowerCase|javaUpperCase|javaWhitespace|javaMirrored)}",
            CC_JAVA,
            ss.REMAIN
        );

        // Classes for Unicode scripts, blocks, categories and binary properties
        // \p{IsLatin}        A Latin script character (script)
        // \p{IsAlphabetic}   An alphabetic character (binary property)
        ss.addRule(ss.ANY_STATE, "\\\\[pP]\\{Is\\w+}", CC_UNICODE_SCRIPT_OR_BINARY_PROPERTY, ss.REMAIN);
        // \p{InGreek}        A character in the Greek block (block)
        ss.addRule(ss.ANY_STATE, "\\\\[pP]\\{In\\w+}", CC_UNICODE_BLOCK,                     ss.REMAIN);
        // \p{Lu}             An uppercase letter (category)
        // \p{Sc}             A currency symbol
        ss.addRule(ss.ANY_STATE, "\\\\[pP]\\{\\w\\w}", CC_UNICODE_CATEGORY,                  ss.REMAIN);
        // \P{InGreek}        Any character except one in the Greek block (negation)
        // [\p{L}&&[^\p{Lu}]] Any letter except an uppercase letter (subtraction)

        // Boundary matchers
        // ^   The beginning of a line
        ss.addRule(Pattern.DEFAULT_STATES, "\\^",   BEGINNING_OF_LINE,                 ss.REMAIN);
        // $   The end of a line
        ss.addRule(Pattern.DEFAULT_STATES, "\\$",   END_OF_LINE,                       ss.REMAIN);
        // \b  A word boundary
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\b", WORD_BOUNDARY,                     ss.REMAIN);
        // \B  A non-word boundary
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\B", NON_WORD_BOUNDARY,                 ss.REMAIN);
        // \A  The beginning of the input
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\A", BEGINNING_OF_INPUT,                ss.REMAIN);
        // \G  The end of the previous match
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\G", END_OF_PREVIOUS_MATCH,             ss.REMAIN);
        // \Z  The end of the input but for the final terminator, if any
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\Z", END_OF_INPUT_BUT_FINAL_TERMINATOR, ss.REMAIN);
        // \z  The end of the input
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\z", END_OF_INPUT,                      ss.REMAIN);

        // Linebreak matcher
        // \R  Any Unicode linebreak sequence, is equivalent to
        //                          /u000D/u000A|[/u000A/u000B/u000C/u000D/u0085/u2028/u2029]
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\R", LINEBREAK_MATCHER, ss.REMAIN);

        // Greedy quantifiers
        // X?         X, once or not at all
        // X*         X, zero or more times
        // X+         X, one or more times
        // X{n}       X, exactly n times
        // X{min,}    X, at least min times
        // X{min,max} X, at least min but not more than max times
        ss.addRule(
            Pattern.DEFAULT_STATES,
            "(?:\\?|\\*|\\+|\\{(\\d+)(?:(,)(\\d+)?)?})(?![?+])", // $1=min, $2="," (opt), $3=max (opt)
            GREEDY_QUANTIFIER,
            ss.REMAIN
        );

        // Reluctant quantifiers
        // X??         X, once or not at all
        // X*?         X, zero or more times
        // X+?         X, one or more times
        // X{n}?       X, exactly n times (identical with X{n})
        // X{min,}?    X, at least min times
        // X{min,max}? X, at least min but not more than max times
        ss.addRule(
            Pattern.DEFAULT_STATES,
            "(?:\\?|\\*|\\+|\\{(\\d+)(?:(,)(\\d+)?)?})\\?", // $1=min, $2="," (opt), $3=max (opt)
            RELUCTANT_QUANTIFIER,
            ss.REMAIN
        );

        // Possessive quantifiers
        // X?+         X, once or not at all
        // X*+         X, zero or more times
        // X++         X, one or more times
        // X{n}+       X, exactly n times (identical with X{n})
        // X{min,}+    X, at least min times
        // X{min,max}+ X, at least min but not more than max times
        ss.addRule(
            Pattern.DEFAULT_STATES,
            "(?:\\?|\\*|\\+|\\{(\\d+)(?:(,)(\\d+)?)?})\\+", // $1=min, $2="," (opt), $3=max (opt)
            POSSESSIVE_QUANTIFIER,
            ss.REMAIN
        );

        // Logical operators
        // XY  X followed by Y
        // X|Y Either X or Y
        ss.addRule(Pattern.DEFAULT_STATES, "\\|", EITHER_OR, ss.REMAIN);
        // (X) X, as a capturing group
        ss.addRule(ScannerState.DEFAULT,   "\\((?![\\?<])",        CAPTURING_GROUP, ss.REMAIN);
        ss.addRule(ScannerState.DEFAULT_X, "\\(\\s*(?![\\?<\\s])", CAPTURING_GROUP, ss.REMAIN);
        ss.addRule(Pattern.DEFAULT_STATES, "\\)",                  END_GROUP,       ss.REMAIN);

        // Back references
        // \n       Whatever the nth capturing group matched
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\\\d",     CAPTURING_GROUP_BACK_REFERENCE,       ss.REMAIN);
        // \k<name> Whatever the named-capturing group "name" matched
        ss.addRule(Pattern.DEFAULT_STATES, "\\\\k<\\w+>", NAMED_CAPTURING_GROUP_BACK_REFERENCE, ss.REMAIN);

        // Quotation
        // \   Nothing, but quotes the following character
        ss.addRule(ss.ANY_STATE,                "\\\\[^0-9A-Za-z]", QUOTED_CHARACTER,  ss.REMAIN);
        // \Q  Nothing, but quotes all characters until \E
        ss.addRule(ScannerState.DEFAULT,        "\\\\Q",            QUOTATION_BEGIN,   ScannerState.IN_QUOTATION);
        ss.addRule(ScannerState.DEFAULT_X,      "\\\\Q",            QUOTATION_BEGIN,   ScannerState.IN_QUOTATION_X);
        // \E  Nothing, but ends quoting started by \Q
        ss.addRule(ScannerState.IN_QUOTATION,   "\\\\E",            QUOTATION_END,     ScannerState.DEFAULT);
        ss.addRule(ScannerState.IN_QUOTATION_X, "\\\\E",            QUOTATION_END,     ScannerState.DEFAULT_X);
        ss.addRule(ScannerState.IN_QUOTATION,   ".",                LITERAL_CHARACTER, ScannerState.IN_QUOTATION);
        ss.addRule(ScannerState.IN_QUOTATION_X, ".",                LITERAL_CHARACTER, ScannerState.IN_QUOTATION_X);

        // Special constructs (named-capturing and non-capturing)
        // (?<name>X)          X, as a named-capturing group
        ss.addRule(ScannerState.DEFAULT,   "\\(\\?<(\\w+)>",                      NAMED_CAPTURING_GROUP,           ss.REMAIN); // SUPPRESS CHECKSTYLE LineLength:18
        ss.addRule(ScannerState.DEFAULT_X, "\\(\\s*\\?<\\s*(\\w+)>",              NAMED_CAPTURING_GROUP,           ss.REMAIN);
        // (?:X)               X, as a non-capturing group
        ss.addRule(ScannerState.DEFAULT,   "\\(\\?:",                             NON_CAPTURING_GROUP,             ss.REMAIN);
        ss.addRule(ScannerState.DEFAULT_X, "\\(\\s*\\?\\s*:",                     NON_CAPTURING_GROUP,             ss.REMAIN);
        // (?idmsuxU-idmsuxU)  Nothing, but turns match flags i d m s u x U on - off
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?[idmsuxU]*(?:-[idmsuxU]+)?\\)", MATCH_FLAGS,                     ss.REMAIN);
        // (?idmsux-idmsux:X)  X, as a non-capturing group with the given flags i d m s u x on - off
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?[idmsux]*(?:-[idmsux]*)?:",     MATCH_FLAGS_NON_CAPTURING_GROUP, ss.REMAIN);
        // (?=X)               X, via zero-width positive lookahead
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?=",                             POSITIVE_LOOKAHEAD,              ss.REMAIN);
        // (?!X)               X, via zero-width negative lookahead
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?!",                             NEGATIVE_LOOKAHEAD,              ss.REMAIN);
        // (?<=X)              X, via zero-width positive lookbehind
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?<=",                            POSITIVE_LOOKBEHIND,             ss.REMAIN);
        // (?<!X)              X, via zero-width negative lookbehind
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?<!",                            NEGATIVE_LOOKBEHIND,             ss.REMAIN);
        // (?>X)               X, as an independent, non-capturing group
        ss.addRule(Pattern.DEFAULT_STATES, "\\(\\?>",                             INDEPENDENT_NON_CAPTURING_GROUP, ss.REMAIN);

        // Any literal character.
        ss.addRule(ss.ANY_STATE, "[^\\\\(*?+]", LITERAL_CHARACTER, ss.REMAIN);
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
            LITERAL_CHARACTER,
            Pattern.LITERAL_SCANNER.REMAIN
        );
    }

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
     * @see java.util.regex.Pattern#split(CharSequence)
     */
    @Override public String[]
    split(CharSequence input) {

        Matcher m = this.matcher(input);
        if (!m.find()) return new String[] { input.toString() };

        List<String> result = new ArrayList<String>();
        result.add(input.subSequence(0, m.start()).toString());
        for (;;) {
            int eopm = m.end(); // "End of previous match"
            if (!m.find()) {
                result.add(input.subSequence(eopm, input.length()).toString());
                return result.toArray(new String[result.size()]);
            }
            result.add(input.subSequence(eopm, m.start()).toString());
        }
    }

    /**
     * @see java.util.regex.Pattern#quote(String)
     */
    @Override public String
    quote(String s) { return java.util.regex.Pattern.quote(s); }

    /**
     * @return Whether the suffix starting at position <var>offset</var> matches this pattern
     * @see    java.util.regex.Pattern#matches(String, CharSequence)
     */
    @Override public boolean
    matches(CharSequence subject, int offset) {

        MatcherImpl mi = new MatcherImpl(this, subject);

        mi.end = MatcherImpl.End.END_OF_SUBJECT;

        return this.sequence.matches(mi, offset) != -1;
    }
}
