
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
import static de.unkrig.lfr.core.Pattern.TokenType.CC_RANGE;
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

import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import de.unkrig.commons.lang.Characters;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.parser.AbstractParser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.StatefulScanner;

/**
 * A drop-in replacement for {@link java.util.regex.Pattern}.
 */
public
class Pattern {

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

    private static final int ALL_FLAGS = (
        Pattern.CANON_EQ
        | Pattern.CASE_INSENSITIVE
        | Pattern.COMMENTS
        | Pattern.DOTALL
        | Pattern.LITERAL
        | Pattern.MULTILINE
        | Pattern.UNICODE_CASE
        | Pattern.UNIX_LINES
        | Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final int SUPPORTED_FLAGS = (
        0
//        | Pattern.CANON_EQ
        | Pattern.CASE_INSENSITIVE
//        | Pattern.COMMENTS
        | Pattern.DOTALL
        | Pattern.LITERAL
        | Pattern.MULTILINE
        | Pattern.UNICODE_CASE
        | Pattern.UNIX_LINES
//        | Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final EnumSet<ScannerState>
    IN_CHAR_CLASS = EnumSet.of(ScannerState.CHAR_CLASS1, ScannerState.CHAR_CLASS2, ScannerState.CHAR_CLASS3);

    // Interestingly, the following two are equal!
    public static final String LINE_BREAK_CHARACTERS          = "\r\n\u000B\f\u0085\u2028\u2029";
    public static final String VERTICAL_WHITESPACE_CHARACTERS = "\r\n\u000B\f\u0085\u2028\u2029";
    public static final String UNIX_LINE_BREAK_CHARACTERS     = "\n";
    public static final String WHITESPACE_CHARACTERS          = " \t\n\u000B\f\r";

    int                        flags;
    final String               pattern;
    final Sequence             sequence;
    final int                  groupCount;
    final Map<String, Integer> namedGroups;

    enum End { END_OF_SUBJECT, ANY }

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

        // Character classes.
        /** {@code [} */
        LEFT_BRACKET,
        /** {@code ]} */
        RIGHT_BRACKET,
        /** {@code ^} */
        CC_NEGATION,
        /** {@code -} */
        CC_RANGE,
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
    }

    enum ScannerState { CHAR_CLASS1, CHAR_CLASS2, CHAR_CLASS3, IN_QUOTATION } // SUPPRESS CHECKSTYLE JavadocVariable

    abstract static
    class AbstractSequence implements Sequence {

        Sequence successor = Pattern.TERMINAL;

        @Override public void
        append(Sequence newSuccessor) {
            if (this.successor == Pattern.TERMINAL) {
                this.successor = newSuccessor;
            } else {
                this.successor.append(newSuccessor);
            }
        }

        @Override public Sequence
        reverse() {

            if (this.successor == Pattern.TERMINAL) return this;

            Sequence result = this.successor.reverse();
            this.successor = Pattern.TERMINAL;
            result.append(this);
            return result;
        }

        @Override public boolean
        find(MatcherImpl matcher, int start) {

            final int re = matcher.regionEnd;
            for (; start <= re; start++) {

                int newOffset = this.matches(matcher, start);

                if (newOffset != -1) {
                    matcher.groups[0] = start;
                    matcher.groups[1] = newOffset;
                    return true;
                }
            }

            matcher.hitEnd = true;
            return false;
        }
    }

    abstract static
    class CharacterClass extends AbstractSequence implements IntPredicate {

        @Override public final int
        matches(MatcherImpl matcher, int offset) {

            if (offset >= matcher.regionEnd) {
                matcher.hitEnd = true;
                return -1;
            }

            char c = matcher.charAt(offset++);

            // Special handling for UTF-16 surrogates.
            if (Character.isHighSurrogate(c)) {
                if (offset < matcher.regionEnd) {
                    char c2 = matcher.charAt(offset);
                    if (Character.isLowSurrogate(c2)) {
                        return (
                            this.evaluate(Character.toCodePoint(c, c2))
                            ? this.successor.matches(matcher, offset + 1)
                            : -1
                        );
                    }
                }
            }

            return this.evaluate(c) ? this.successor.matches(matcher, offset) : -1;
        }
    }

    // ==========================================================

    Pattern(String pattern, int flags, Sequence sequence, int groupCount, Map<String, Integer> namedGroups) {

        this.flags       = flags;
        this.pattern     = pattern;
        this.namedGroups = namedGroups;

        sequence.append(new Sequence() {

            @Override public int
            matches(MatcherImpl matcher, int offset) {
                return matcher.end == End.ANY || offset >= matcher.regionEnd ? offset : -1;
            }

            @Override public void
            append(Sequence that) { throw new UnsupportedOperationException(); }

            @Override public Sequence
            reverse() { return this; }

            @Override public boolean
            find(MatcherImpl matcherImpl, int start) { throw new UnsupportedOperationException(); }

            @Override public String
            toString() { return "[END]"; }
        });

        this.sequence   = sequence;
        this.groupCount = groupCount;
    }

    public static boolean
    isWordCharacter(int c) {
        return (
            (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || (c >= '0' && c <= '9')
            || c == '_'
        );
    }

    static
    class RegexScanner extends StatefulScanner<TokenType, ScannerState> {

        int                        groupCount;
        final Map<String, Integer> namedGroups = new HashMap<String, Integer>();

        RegexScanner()                  { super(ScannerState.class); }
        RegexScanner(RegexScanner that) { super(that);        }
    }

    /**
     * This scanner is intended to be cloned by {@link RegexScanner#RegexScanner(RegexScanner)} when a regex scanner is
     * needed.
     */
    static final RegexScanner REGEX_SCANNER = new RegexScanner();
    static {
        StatefulScanner<TokenType, ScannerState> ss = Pattern.REGEX_SCANNER;

        // Characters
        // x         The character x
        // See below: +++
        // \\        The backslash character
        ss.addRule(ss.ANY_STATE, "\\\\\\\\",                              QUOTED_CHARACTER,    null);
        // \0n       The character with octal value 0n (0 <= n <= 7)
        // \0nn      The character with octal value 0nn (0 <= n <= 7)
        // \0mnn     The character with octal value 0mnn (0 <= m <= 3, 0 <= n <= 7)
        ss.addRule(ss.ANY_STATE, "\\\\0(?:0[0-3][0-8][0-7]|[0-7][0-7]?)", LITERAL_OCTAL,       null);
        // \xhh      The character with hexadecimal value 0xhh
        ss.addRule(ss.ANY_STATE, "\\\\u[0-9a-fA-F]{4}",                   LITERAL_HEXADECIMAL, null);
        // /uhhhh    The character with hexadecimal value 0xhhhh
        ss.addRule(ss.ANY_STATE, "\\\\x[0-9a-fA-F]{2}",                   LITERAL_HEXADECIMAL, null);
        // \x{h...h} The character with hexadecimal value 0xh...h
        //                                    (Character.MIN_CODE_POINT  <= 0xh...h <=  Character.MAX_CODE_POINT)
        ss.addRule(ss.ANY_STATE, "\\\\x\\{[0-9a-fA-F]+}",                 LITERAL_HEXADECIMAL, null);
        // \t        The tab character ('/u0009')
        // \n        The newline (line feed) character ('/u000A')
        // \r        The carriage-return character ('/u000D')
        // \f        The form-feed character ('/u000C')
        // \a        The alert (bell) character ('/u0007')
        // \e        The escape character ('/u001B')
        ss.addRule(ss.ANY_STATE, "\\\\[tnrfae]",                          LITERAL_CONTROL,     null);
        // \cx The control character corresponding to x
        ss.addRule(ss.ANY_STATE, "\\\\c[A-Za-z]",                         LITERAL_CONTROL,     null);

        // Character classes
        // [abc]       a, b, or c (simple class)
        ss.addRule("\\[",                    LEFT_BRACKET,  ScannerState.CHAR_CLASS1);
        ss.addRule(ScannerState.CHAR_CLASS1, "\\[", LEFT_BRACKET,  ScannerState.CHAR_CLASS2);
        ss.addRule(ScannerState.CHAR_CLASS2, "\\[", LEFT_BRACKET,  ScannerState.CHAR_CLASS3);
        ss.addRule(ScannerState.CHAR_CLASS3, "]",   RIGHT_BRACKET, ScannerState.CHAR_CLASS2);
        ss.addRule(ScannerState.CHAR_CLASS2, "]",   RIGHT_BRACKET, ScannerState.CHAR_CLASS1);
        ss.addRule(ScannerState.CHAR_CLASS1, "]",   RIGHT_BRACKET);
        // [^abc]      Any character except a, b, or c (negation)
        ss.addRule(Pattern.IN_CHAR_CLASS, "\\^", CC_NEGATION, null);
        // [a-zA-Z]    a through z or A through Z, inclusive (range)
        ss.addRule(Pattern.IN_CHAR_CLASS, "-", CC_RANGE, null);
        // [a-d[m-p]]  a through d, or m through p: [a-dm-p] (union)
        // [a-z&&[def]]    d, e, or f (intersection)
        // [a-z&&[^bc]]    a through z, except for b and c: [ad-z] (subtraction)
        // [a-z&&[^m-p]]   a through z, and not m through p: [a-lq-z] (subtraction)
        ss.addRule(Pattern.IN_CHAR_CLASS, "&&", CC_INTERSECTION, null);

        // Predefined character classes
        // .   Any character (may or may not match line terminators)
        ss.addRule("\\.", CC_ANY);
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
        ss.addRule(ss.ANY_STATE, "\\\\[dDhHsSvVwW]", CC_PREDEFINED, null);

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
        ss.addRule(ss.ANY_STATE, "\\\\[pP]\\{(?:Lower|Upper|ASCII|Alpha|Digit|Alnum|Punct|Graph|Print|Blank|Cntrl|XDigit|Space)}", CC_POSIX, null); // SUPPRESS CHECKSTYLE LineLength

        // java.lang.Character classes (simple java character type)
        // \p{javaLowerCase}   Equivalent to java.lang.Character.isLowerCase()
        // \p{javaUpperCase}   Equivalent to java.lang.Character.isUpperCase()
        // \p{javaWhitespace}  Equivalent to java.lang.Character.isWhitespace()
        // \p{javaMirrored}    Equivalent to java.lang.Character.isMirrored()
        ss.addRule(ss.ANY_STATE, "\\\\[pP]\\{(?:javaLowerCase|javaUpperCase|javaWhitespace|javaMirrored)}", CC_JAVA, null);

        // Classes for Unicode scripts, blocks, categories and binary properties
        // \p{IsLatin}        A Latin script character (script)
        // \p{IsAlphabetic}   An alphabetic character (binary property)
        ss.addRule(ss.ANY_STATE, "\\\\[pP]\\{Is\\w+}", CC_UNICODE_SCRIPT_OR_BINARY_PROPERTY, null);
        // \p{InGreek}        A character in the Greek block (block)
        ss.addRule(ss.ANY_STATE, "\\\\[pP]\\{In\\w+}", CC_UNICODE_BLOCK, null);
        // \p{Lu}             An uppercase letter (category)
        // \p{Sc}             A currency symbol
        ss.addRule(ss.ANY_STATE, "\\\\[pP]\\{\\w\\w}", CC_UNICODE_CATEGORY, null);
        // \P{InGreek}        Any character except one in the Greek block (negation)
        // [\p{L}&&[^\p{Lu}]] Any letter except an uppercase letter (subtraction)

        // Boundary matchers
        // ^   The beginning of a line
        ss.addRule("\\^",   BEGINNING_OF_LINE);
        // $   The end of a line
        ss.addRule("\\$",   END_OF_LINE);
        // \b  A word boundary
        ss.addRule("\\\\b", WORD_BOUNDARY);
        // \B  A non-word boundary
        ss.addRule("\\\\B", NON_WORD_BOUNDARY);
        // \A  The beginning of the input
        ss.addRule("\\\\A", BEGINNING_OF_INPUT);
        // \G  The end of the previous match
        ss.addRule("\\\\G", END_OF_PREVIOUS_MATCH);
        // \Z  The end of the input but for the final terminator, if any
        ss.addRule("\\\\Z", END_OF_INPUT_BUT_FINAL_TERMINATOR);
        // \z  The end of the input
        ss.addRule("\\\\z", END_OF_INPUT);

        // Linebreak matcher
        // \R  Any Unicode linebreak sequence, is equivalent to
        //                          /u000D/u000A|[/u000A/u000B/u000C/u000D/u0085/u2028/u2029]
        ss.addRule("\\\\R", LINEBREAK_MATCHER);

        // Greedy quantifiers
        // X?         X, once or not at all
        // X*         X, zero or more times
        // X+         X, one or more times
        // X{n}       X, exactly n times
        // X{min,}    X, at least n times
        // X{min,max} X, at least n but not more than m times
        ss.addRule("(?:\\?|\\*|\\+|\\{\\d+(?:,(?:\\d+)?)?})(?![?+])", GREEDY_QUANTIFIER);

        // Reluctant quantifiers
        // X??     X, once or not at all
        // X*?     X, zero or more times
        // X+?     X, one or more times
        // X{n}?   X, exactly n times
        // X{n,}?  X, at least n times
        // X{n,m}? X, at least n but not more than m times
        ss.addRule("(?:\\?|\\*|\\+|\\{\\d+(?:,(?:\\d+)?)?})\\?", RELUCTANT_QUANTIFIER);

        // Possessive quantifiers
        // X?+     X, once or not at all
        // X*+     X, zero or more times
        // X++     X, one or more times
        // X{n}+   X, exactly n times
        // X{n,}+  X, at least n times
        // X{n,m}+ X, at least n but not more than m times
        ss.addRule("(?:\\?|\\*|\\+|\\{\\d+(?:,(?:\\d+)?)?})\\+", POSSESSIVE_QUANTIFIER);

        // Logical operators
        // XY  X followed by Y
        // X|Y Either X or Y
        ss.addRule("\\|",           EITHER_OR);
        // (X) X, as a capturing group
        ss.addRule("\\((?![\\?<])", CAPTURING_GROUP);
        ss.addRule("\\)",           END_GROUP);

        // Back references
        // \n       Whatever the nth capturing group matched
        ss.addRule("\\\\\\d",     CAPTURING_GROUP_BACK_REFERENCE);
        // \k<name> Whatever the named-capturing group "name" matched
        ss.addRule("\\\\k<\\w+>", NAMED_CAPTURING_GROUP_BACK_REFERENCE);

        // Quotation
        // \   Nothing, but quotes the following character
        // \Q  Nothing, but quotes all characters until \E
        // \E  Nothing, but ends quoting started by \Q
        ss.addRule(ss.ANY_STATE,              "\\\\[^0-9A-Za-z]", QUOTED_CHARACTER,  null);
        ss.addRule("\\\\Q",                                       QUOTATION_BEGIN,   ScannerState.IN_QUOTATION);
        ss.addRule(ScannerState.IN_QUOTATION, "\\\\E",            QUOTATION_END,     ScannerState.IN_QUOTATION);
        ss.addRule(ScannerState.IN_QUOTATION, ".",                LITERAL_CHARACTER, ScannerState.IN_QUOTATION);

        // Special constructs (named-capturing and non-capturing)
        // (?<name>X)          X, as a named-capturing group
        ss.addRule("\\(\\?<\\w+>",                        NAMED_CAPTURING_GROUP);
        // (?:X)               X, as a non-capturing group
        ss.addRule("\\(\\?:",                             NON_CAPTURING_GROUP);
        // (?idmsuxU-idmsuxU)  Nothing, but turns match flags i d m s u x U on - off
        ss.addRule("\\(\\?[idmsuxU]*(?:-[idmsuxU]+)?\\)", MATCH_FLAGS);
        // (?idmsux-idmsux:X)  X, as a non-capturing group with the given flags i d m s u x on - off
        ss.addRule("\\(\\?[idmsux]*(?:-[idmsux]*)?:",     MATCH_FLAGS_NON_CAPTURING_GROUP);
        // (?=X)               X, via zero-width positive lookahead
        ss.addRule("\\(\\?=",                             POSITIVE_LOOKAHEAD);
        // (?!X)               X, via zero-width negative lookahead
        ss.addRule("\\(\\?!",                             NEGATIVE_LOOKAHEAD);
        // (?<=X)              X, via zero-width positive lookbehind
        ss.addRule("\\(\\?<=",                            POSITIVE_LOOKBEHIND);
        // (?<!X)              X, via zero-width negative lookbehind
        ss.addRule("\\(\\?<!",                            NEGATIVE_LOOKBEHIND);
        // (?>X)               X, as an independent, non-capturing group
        ss.addRule("\\(\\?>",                             INDEPENDENT_NON_CAPTURING_GROUP);

        ss.addRule(ss.ANY_STATE, "[^\\\\]", LITERAL_CHARACTER, null); // +++
    }

    /**
     * This scanner is intended to be cloned by {@link RegexScanner#RegexScanner(RegexScanner)} when a literal scanner
     * is needed.
     */
    static final RegexScanner LITERAL_SCANNER = new RegexScanner();
    static {
        Pattern.LITERAL_SCANNER.addRule(".", LITERAL_CHARACTER);
    }

    /**
     * A sequence that matches a linebreak.
     */
    static Sequence
    linebreakSequence() {

        return new AbstractSequence() {

            char c1 = '\r', c2 = '\n';

            @Override public int
            matches(MatcherImpl matcher, int offset) {

                if (offset >= matcher.regionEnd) return -1;

                char c = matcher.charAt(offset);

                // Check for LInebreAK characters in a highly optimized manner.
                if (c <= 0x0d) {
                    if (c == this.c1 && offset < matcher.regionEnd - 1 && matcher.charAt(offset + 1) == this.c2) {
                        return this.successor.matches(matcher, offset + 2);
                    }
                    return c >= 0x0a ? this.successor.matches(matcher, offset + 1) : -1;
                }

                if (c == 0x85 || (c >= 0x2028 && c <= 0x2029)) {
                    return this.successor.matches(matcher, offset + 1);
                }

                return -1;
            }

            @Override public Sequence
            reverse() {
                char tmp = this.c1;
                this.c1 = this.c2;
                this.c2 = tmp;
                return super.reverse();
            }


            @Override public String
            toString() { return "\\R"; }
        };
    }

    /**
     * A sequence that simply delegates to its successor.
     */
    public static final
    class EmptyStringSequence extends AbstractSequence {

        @Override public int
        matches(MatcherImpl matcher, int offset) { return this.successor.matches(matcher, offset); }

        @Override public String toString() { return ""; }
    }

    public static final Sequence
    TERMINAL = new Sequence() {

        @Override public int
        matches(MatcherImpl matcher, int offset) { return offset; }

        @Override public void
        append(Sequence that) { throw new UnsupportedOperationException(); }

        @Override public boolean
        find(MatcherImpl matcherImpl, int start) { throw new UnsupportedOperationException(); }

        @Override public Sequence
        reverse() { return this; }

        @Override public String
        toString() { return "[TERM]"; }
    };

    /**
     * @see java.util.regex.Pattern#compile(String)
     */
    public static Pattern
    compile(String regex) throws PatternSyntaxException { return Pattern.compile(regex, 0); }

    /**
     * @see java.util.regex.Pattern#compile(String, int)
     */
    public static Pattern
    compile(String regex, int flags) throws PatternSyntaxException {

        if ((flags & ~Pattern.ALL_FLAGS) != 0) {
            throw new IllegalArgumentException("Disallowed flag " + (flags & ~Pattern.ALL_FLAGS));
        }
        if ((flags & ~Pattern.SUPPORTED_FLAGS) != 0) {
            throw new IllegalArgumentException("Unsupported flag " + (flags & ~Pattern.SUPPORTED_FLAGS));
        }

        RegexScanner rs = new RegexScanner(
            (flags & Pattern.LITERAL) == 0
            ? Pattern.REGEX_SCANNER
            : Pattern.LITERAL_SCANNER
        );
        rs.setInput(regex);

        Sequence sequence;
        try {
            sequence = Pattern.parse(rs, flags);
        } catch (ParseException pe) {
            PatternSyntaxException pse = new PatternSyntaxException(pe.getMessage(), regex, rs.getOffset());
            pse.initCause(pe);
            throw pse;
        }

        return new Pattern(regex, flags, sequence, rs.groupCount, rs.namedGroups);
    }

    /**
     * @see java.util.regex.Pattern#pattern()
     */
    public String pattern() { return this.pattern; }

    /**
     * @see java.util.regex.Pattern#toString()
     */
    @Override public String toString() { return this.pattern; }

    /**
     * @see java.util.regex.Pattern#matcher(CharSequence)
     */
    public Matcher
    matcher(CharSequence subject) { return new MatcherImpl(this, subject); }

    /**
     * @see java.util.regex.Pattern#flags()
     */
    public int flags() { return this.flags; }

    /**
     * @see java.util.regex.Pattern#matches(String, CharSequence)
     */
    public static boolean
    matches(String regex, CharSequence input) { return Pattern.compile(regex).matches(input, 0); }

//    /**
//     * @see java.util.regex.Pattern#split(CharSequence, int)
//     */
//    public String[] split(CharSequence input, int limit);

    /**
     * @see java.util.regex.Pattern#split(CharSequence)
     */
    public String[]
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
    public static String
    quote(String s) { return java.util.regex.Pattern.quote(s); }

    /**
     * @return Whether the suffix starting at position <var>offset</var> matches this pattern
     */
    public boolean
    matches(CharSequence subject, int offset) {

        MatcherImpl mi = new MatcherImpl(this, subject);

        mi.end = End.END_OF_SUBJECT;

        return this.sequence.matches(mi, offset) != -1;
    }

    /**
     * Parses a regular expression into a {@link Sequence}.
     */
    private static Sequence
    parse(final RegexScanner rs, final int flags) throws ParseException {

        return new AbstractParser<TokenType>(rs) {

            int currentFlags = flags;

            public Sequence
            parse() throws ParseException {

                Sequence result = this.parseAlternatives();

                // Check for trailing garbage.
                this.read(new Object[1]);

                return result;
            }

            private Sequence
            parseAlternatives() throws ParseException {

                Sequence op1 = this.parseSequence();
                if (!this.peekRead("|")) return op1;

                List<Sequence> alternatives = new ArrayList<Sequence>();
                alternatives.add(op1);
                alternatives.add(this.parseSequence());
                while (this.peekRead(EITHER_OR) != null) alternatives.add(this.parseSequence());
                return Sequences.alternatives(alternatives.toArray(new Sequence[alternatives.size()]));
            }

            private Sequence
            parseSequence() throws ParseException {

                if (this.peek() == null || this.peekRead("|")) return Sequences.emptyStringSequence();

                Sequence first = this.parseQuantified();
                if (this.peek(null, "|", ")") != -1) return first;

                // Parse all elements into a list.
                List<Sequence> elements = new ArrayList<Sequence>();
                elements.add(first);
                do {
                    elements.add(this.parseQuantified());
                } while (this.peek(null, "|", ")") == -1);

                // Now do some optimization on the list:

                // Remove EmptyStringSequences.
                for (Iterator<Sequence> it = elements.iterator(); it.hasNext();) {
                    if (it.next() instanceof EmptyStringSequence) it.remove();
                }

                // Merge consecutive literals into one string literal.
                for (int i = 0; i < elements.size() - 1;) {

                    Sequence merged = this.merge(elements.get(i), elements.get(i + 1));
                    if (merged != null) {
                        elements.set(i, merged);
                        elements.remove(i + 1);
                    } else {
                        i++;
                    }
                }

                // Now link all sequence elements one to another.
                for (int i = 0; i < elements.size() - 1; i++) elements.get(i).append(elements.get(i + 1));

                return elements.get(0);
            }

            @Nullable private Sequence
            merge(Sequence s1, Sequence s2) {

                if (s1 instanceof CharacterClasses.LiteralCharacter && s2 instanceof CharacterClasses.LiteralCharacter) {
                    int c1 = ((CharacterClasses.LiteralCharacter) s1).c;
                    int c2 = ((CharacterClasses.LiteralCharacter) s2).c;
                    return Sequences.literalString(
                        new StringBuilder(4).appendCodePoint(c1).appendCodePoint(c2).toString()
                    );
                }

                if (s1 instanceof Sequences.LiteralString && s2 instanceof CharacterClasses.LiteralCharacter) {
                    String lhs = ((Sequences.LiteralString)      s1).s;
                    int    rhs = ((CharacterClasses.LiteralCharacter) s2).c;
                    return Sequences.literalString(new StringBuilder(lhs).appendCodePoint(rhs).toString());
                }

                if (s1 instanceof CharacterClasses.LiteralCharacter && s2 instanceof Sequences.LiteralString) {
                    int    lhs = ((CharacterClasses.LiteralCharacter) s1).c;
                    String rhs = ((Sequences.LiteralString)      s2).s;
                    return Sequences.literalString(
                        new StringBuilder(rhs.length() + 2).appendCodePoint(lhs).append(rhs).toString()
                    );
                }

                if (s1 instanceof Sequences.LiteralString && s2 instanceof Sequences.LiteralString) {
                    return Sequences.literalString(((Sequences.LiteralString) s1).s + ((Sequences.LiteralString) s2).s);
                }

                return null;
            }

            private Sequence
            parseQuantified() throws ParseException {

                final Sequence op = this.parsePrimary();

                Token<TokenType> t = this.peek();
                if (t == null) return op;

                switch (t.type) {

                case GREEDY_QUANTIFIER:
                case RELUCTANT_QUANTIFIER:
                case POSSESSIVE_QUANTIFIER:
                    this.read();

                    op.append(Pattern.TERMINAL);

                    final int min, max;
                    switch (t.text.charAt(0)) {

                    case '?':
                        min = 0;
                        max = 1;
                        break;

                    case '*':
                        min = 0;
                        max = Integer.MAX_VALUE - 1;
                        break;

                    case '+':
                        min = 1;
                        max = Integer.MAX_VALUE - 1;
                        break;

                    case '{':
                        int idx1 = t.text.indexOf(',');
                        int idx2 = t.text.indexOf('}', idx1 + 1);
                        if (idx1 == -1) {
                            min = (max = Integer.parseInt(t.text.substring(1, idx2)));
                        } else {
                            min = Integer.parseInt(t.text.substring(1, idx1++));
                            max = idx1 == idx2 ? Integer.MAX_VALUE - 1 : Integer.parseInt(t.text.substring(idx1, idx2));
                        }
                        break;
                    default:
                        throw new AssertionError(t);
                    }

                    switch (t.type) {

                    case GREEDY_QUANTIFIER:
                        return Sequences.greedyQuantifierSequence(op, min, max);

                    case RELUCTANT_QUANTIFIER:
                        return Sequences.reluctantQuantifierSequence(op, min, max);

                    case POSSESSIVE_QUANTIFIER:
                        return Sequences.possessiveQuantifierSequence(op, min, max);

                    default:
                        throw new AssertionError(t);
                    }

                default:
                    return op;
                }
            }

            private Sequence
            parsePrimary() throws ParseException {

                Token<TokenType> t = this.peek();

                if (t != null) {
                    TokenType tt = t.type;


                    if (
                        tt == TokenType.LITERAL_CHARACTER
                        || tt == TokenType.LITERAL_CONTROL
                        || tt == TokenType.LITERAL_HEXADECIMAL
                        || tt == TokenType.LITERAL_OCTAL
                        || tt == TokenType.LEFT_BRACKET
                        || tt == TokenType.CC_PREDEFINED
                        || tt == TokenType.CC_JAVA
                        || tt == TokenType.CC_POSIX
                        || tt == TokenType.CC_UNICODE_SCRIPT_OR_BINARY_PROPERTY
                        || tt == TokenType.CC_UNICODE_BLOCK
                        || tt == TokenType.CC_UNICODE_CATEGORY

                    ) return this.parseCharacterClass();
                }

                t = this.read();

                switch (t.type) {

                case LITERAL_CHARACTER:
                case LITERAL_CONTROL:
                case LITERAL_HEXADECIMAL:
                case LITERAL_OCTAL:
                case LEFT_BRACKET:
                case CC_PREDEFINED:
                case CC_JAVA:
                case CC_POSIX:
                case CC_UNICODE_SCRIPT_OR_BINARY_PROPERTY:
                case CC_UNICODE_BLOCK:
                case CC_UNICODE_CATEGORY:
                    // These were handled before.
                    throw new AssertionError();

                case CC_ANY:
                    return (
                        (this.currentFlags & Pattern.DOTALL) != 0
                        ? CharacterClasses.anyCharacter()
                        : (this.currentFlags & Pattern.UNIX_LINES) != 0
                        ? CharacterClasses.negate(CharacterClasses.literalCharacter('\n', "?"), ".")
                        : CharacterClasses.negate(CharacterClasses.lineBreakCharacter(), ".")
                    );

                case QUOTED_CHARACTER:
                    return CharacterClasses.literalCharacter(t.text.codePointAt(1), t.text);

                case QUOTATION_BEGIN:
                    {
                        Sequence result = this.parseSequence();
                        if (this.peek() != null) this.read(TokenType.QUOTATION_END);
                        return result;
                    }

                case CAPTURING_GROUP:
                    {
                        int groupNumber = ++rs.groupCount;
                        Sequence result = Sequences.capturingGroupStart(groupNumber);
                        result.append(this.parseAlternatives());
                        result.append(Sequences.capturingGroupEnd(groupNumber));
                        this.read(")");
                        return result;
                    }

                case NON_CAPTURING_GROUP:
                    {
                        final Sequence result = this.parseAlternatives();
                        this.read(")");
                        return result;
                    }

                case INDEPENDENT_NON_CAPTURING_GROUP:
                    {
                        List<Sequence> alternatives = new ArrayList<Sequence>();
                        alternatives.add(this.parseSequence());
                        while (this.peekRead(EITHER_OR) != null) alternatives.add(this.parseSequence());

                        this.read(")");

                        return Sequences.independenNonCapturingGroup(
                            alternatives.toArray(new Sequence[alternatives.size()])
                        );
                    }

                case MATCH_FLAGS_NON_CAPTURING_GROUP:
                    {
                        int savedFlags = this.currentFlags;
                        this.currentFlags = (
                            this.parseFlags(this.currentFlags, t.text.substring(2, t.text.length() - 1))
                        );

                        final Sequence result = this.parseAlternatives();

                        this.currentFlags = savedFlags;

                        this.read(")");

                        return result;
                    }

                case CAPTURING_GROUP_BACK_REFERENCE:
                    {
                        int groupNumber = Integer.parseInt(t.text.substring(1));
                        if (groupNumber > rs.groupCount) {
                            throw new ParseException("Group number " + groupNumber + " too big");
                        }
                        return Sequences.capturingGroupBackReference(groupNumber);
                    }

                case BEGINNING_OF_LINE:
                    return (
                        (this.currentFlags & Pattern.MULTILINE) == 0
                        ? Sequences.beginningOfInput()
                        : Sequences.beginningOfLine(
                            (this.currentFlags & Pattern.UNIX_LINES) != 0
                            ? Pattern.UNIX_LINE_BREAK_CHARACTERS
                            : Pattern.LINE_BREAK_CHARACTERS
                        )
                    );

                case END_OF_LINE:
                    return (
                        (this.currentFlags & Pattern.MULTILINE) == 0
                        ? Sequences.endOfInput()
                        : Sequences.endOfLine(
                            (this.currentFlags & Pattern.UNIX_LINES) != 0
                            ? Pattern.UNIX_LINE_BREAK_CHARACTERS
                            : Pattern.LINE_BREAK_CHARACTERS
                        )
                    );

                case WORD_BOUNDARY:
                    return Sequences.wordBoundary();

                case NON_WORD_BOUNDARY:
                    return Sequences.negate(Sequences.wordBoundary());

                case BEGINNING_OF_INPUT:
                    return Sequences.beginningOfInput();

                case END_OF_PREVIOUS_MATCH:
                    return Sequences.endOfPreviousMatch();

                case END_OF_INPUT_BUT_FINAL_TERMINATOR:
                    return Sequences.endOfInputButFinalTerminator();

                case END_OF_INPUT:
                    return Sequences.endOfInput();

                case MATCH_FLAGS:
                    this.currentFlags = this.parseFlags(this.currentFlags, t.text.substring(2, t.text.length() - 1));
                    return Sequences.emptyStringSequence();

                case LINEBREAK_MATCHER:
                    return Pattern.linebreakSequence();

                case NAMED_CAPTURING_GROUP:
                    {
                        int groupNumber = ++rs.groupCount;

                        String groupName = t.text.substring(3, t.text.length() - 1);
                        if (rs.namedGroups.put(groupName, groupNumber) != null) {
                            throw new ParseException("Duplicate captuting group name \"" + groupName + "\"");
                        }

                        Sequence result = Sequences.capturingGroup(groupNumber, this.parseAlternatives());
                        this.read(")");
                        return result;
                    }

                case NAMED_CAPTURING_GROUP_BACK_REFERENCE:
                    {
                        String  groupName   = t.text.substring(3, t.text.length() - 1);
                        Integer groupNumber = rs.namedGroups.get(groupName);
                        if (groupNumber > rs.groupCount) {
                            throw new ParseException("Unknown group name \"" + groupName + "\"");
                        }
                        return Sequences.capturingGroupBackReference(groupNumber);
                    }

                case POSITIVE_LOOKAHEAD:
                    {
                        final Sequence op = this.parseAlternatives();
                        this.read(TokenType.END_GROUP);
                        return Sequences.positiveLookahead(op);
                    }

                case NEGATIVE_LOOKAHEAD:
                    {
                        final Sequence op = this.parseAlternatives();
                        this.read(TokenType.END_GROUP);
                        return Sequences.negate(Sequences.positiveLookahead(op));
                    }

                case POSITIVE_LOOKBEHIND:
                    {
                        final Sequence op = this.parseAlternatives().reverse();
                        this.read(TokenType.END_GROUP);
                        return Sequences.positiveLookbehind(op);
                    }

                case NEGATIVE_LOOKBEHIND:
                    {
                        final Sequence op = this.parseAlternatives().reverse();
                        this.read(TokenType.END_GROUP);
                        return Sequences.negate(Sequences.positiveLookbehind(op));
                    }

                case CC_NEGATION:
                case CC_RANGE:
                    // These can only appear inside a character class, like "[^abc]" or "[a-k]".
                    throw new AssertionError(t);

                case CC_INTERSECTION:
                case EITHER_OR:
                case END_GROUP:
                case RIGHT_BRACKET:
                case QUOTATION_END:
                case GREEDY_QUANTIFIER:
                case POSSESSIVE_QUANTIFIER:
                case RELUCTANT_QUANTIFIER:
                default:
                    throw new AssertionError(t);
                }
            }

            /**
             * @param spec {@code idmsuxU-idmsuxU}
             */
            public int
            parseFlags(int oldFlags, String spec) throws ParseException {

                int idx = spec.indexOf('-');

                if (idx == -1) return oldFlags | this.parseFlags(spec);

                int positiveFlags = this.parseFlags(spec.substring(0, idx));
                int negativeFlags = this.parseFlags(spec.substring(idx + 1));
                if ((positiveFlags & negativeFlags) != 0) {
                    throw new ParseException("Contradictory embedded flags \"" + spec + "\"");
                }

                return (oldFlags | positiveFlags) & ~negativeFlags;
            }

            /**
             * @param spec {@code idmsuxU}
             */
            private int
            parseFlags(String spec) throws ParseException {
                int result = 0;
                for (int i = 0; i < spec.length(); i++) {
                    char c = spec.charAt(i);

                    int f;
                    switch (c) {
                    case 'i': f = Pattern.CASE_INSENSITIVE;        break;
                    case 'd': f = Pattern.UNIX_LINES;              break;
                    case 'm': f = Pattern.MULTILINE;               break;
                    case 's': f = Pattern.DOTALL;                  break;
                    case 'u': f = Pattern.UNICODE_CASE;            break;
                    case 'x': f = Pattern.COMMENTS;                break;
                    case 'U': f = Pattern.UNICODE_CHARACTER_CLASS; break;
                    default:  throw new ParseException("Invalid embedded flag '" + c + "'");
                    }
                    if ((Pattern.SUPPORTED_FLAGS & f) == 0) {
                        throw new ParseException("Unsupported embedded flag '" + c + "'");
                    }
                    if ((result & f) != 0) throw new ParseException("Duplicate embedded flag '" + c + "'");
                    result |= f;
                }
                return result;
            }

            CharacterClass
            parseCharacterClass() throws ParseException {

                Token<TokenType> t = this.read();

                switch (t.type) {

                case LITERAL_CHARACTER:
                    int c = t.text.codePointAt(0);
                    return (
                        (this.currentFlags & Pattern.CASE_INSENSITIVE) == 0
                        ? CharacterClasses.literalCharacter(c, t.text)
                        : (this.currentFlags & Pattern.UNICODE_CASE) == 0
                        ? CharacterClasses.caseInsensitiveLiteralCharacter(c)
                        : CharacterClasses.unicodeCaseInsensitiveLiteralCharacter(c)
                    );

                case LITERAL_CONTROL:
                    {
                        int idx = "ctnrfae".indexOf(t.text.charAt(1));
                        assert idx != -1;
                        if (idx == 0) return CharacterClasses.literalCharacter((char) (t.text.charAt(2) & 0x1f), t.text);
                        return CharacterClasses.literalCharacter("c\t\n\r\f\u0007\u001b".charAt(idx), t.text);
                    }

                case LITERAL_HEXADECIMAL:
                    return CharacterClasses.literalCharacter(Integer.parseInt(
                        t.text.charAt(2) == '{'
                        ? t.text.substring(3, t.text.length() - 1)
                        : t.text.substring(2)
                    ), t.text);

                case LITERAL_OCTAL:
                    return CharacterClasses.literalCharacter(Integer.parseInt(t.text.substring(2, 8)), t.text);

                case LEFT_BRACKET:
                    {
                        boolean        negate = this.peekRead("^");
                        CharacterClass cc     = this.parseCcIntersection();
                        this.read("]");

                        if (negate) cc = CharacterClasses.negate(cc, '^' + cc.toString());
                        return cc;
                    }

                case CC_PREDEFINED:
                    {
                        CharacterClass result;
                        switch (t.text.charAt(1)) {
                        case 'd': case 'D': result = CharacterClasses.isDigit();                break;
                        case 'h': case 'H': result = CharacterClasses.isHorizontalWhitespace(); break;
                        case 's': case 'S': result = CharacterClasses.isWhitespace();           break;
                        case 'v': case 'V': result = CharacterClasses.isVerticalWhitespace();   break;
                        case 'w': case 'W': result = CharacterClasses.isWord();                 break;
                        default:            throw new AssertionError(t);
                        }

                        if (Character.isUpperCase(t.text.charAt(1))) result = CharacterClasses.negate(result, t.text);

                        return result;
                    }

                case CC_POSIX:
                    {
                        String             ccName = t.text.substring(3, t.text.length() - 1);
                        Predicate<Integer> codePointPredicate = (
                            "Lower".equals(ccName)  ? Characters.IS_POSIX_LOWER  :
                            "Upper".equals(ccName)  ? Characters.IS_POSIX_UPPER  :
                            "ASCII".equals(ccName)  ? Characters.IS_POSIX_ASCII  :
                            "Alpha".equals(ccName)  ? Characters.IS_POSIX_ALPHA  :
                            "Digit".equals(ccName)  ? Characters.IS_POSIX_DIGIT  :
                            "Alnum".equals(ccName)  ? Characters.IS_POSIX_ALNUM  :
                            "Punct".equals(ccName)  ? Characters.IS_POSIX_PUNCT  :
                            "Graph".equals(ccName)  ? Characters.IS_POSIX_GRAPH  :
                            "Print".equals(ccName)  ? Characters.IS_POSIX_PRINT  :
                            "Blank".equals(ccName)  ? Characters.IS_POSIX_BLANK  :
                            "Cntrl".equals(ccName)  ? Characters.IS_POSIX_CNTRL  :
                            "XDigit".equals(ccName) ? Characters.IS_POSIX_XDIGIT :
                            "Space".equals(ccName)  ? Characters.IS_POSIX_SPACE  :
                            null
                        );
                        assert codePointPredicate != null;

                        CharacterClass result = CharacterClasses.characterClass(codePointPredicate, t.text);

                        if (t.text.charAt(1) == 'P') result = CharacterClasses.negate(result, t.text);

                        return result;
                    }

                case CC_JAVA:
                    {
                        String ccName = t.text.substring(3, t.text.length() - 1);
                        Predicate<Integer> codePointPredicate = (
                            "javaLowerCase".equals(ccName)  ? Characters.IS_LOWER_CASE :
                            "javaUpperCase".equals(ccName)  ? Characters.IS_UPPER_CASE :
                            "javaWhitespace".equals(ccName) ? Characters.IS_WHITESPACE :
                            "javaMirrored".equals(ccName)   ? Characters.IS_MIRRORED   :
                            null
                        );
                        assert codePointPredicate != null;

                        CharacterClass result = CharacterClasses.characterClass(codePointPredicate, t.text);

                        if (t.text.charAt(1) == 'P') result = CharacterClasses.negate(result, t.text);

                        return result;
                    }

                case CC_UNICODE_SCRIPT_OR_BINARY_PROPERTY:
                    throw new AssertionError("Unicode scripts and binary properties are not implemented");

                case CC_UNICODE_BLOCK:
                    {
                        String unicodeBlockName = t.text.substring(5, t.text.length() - 1);

                        UnicodeBlock block;
                        try {
                            block = Character.UnicodeBlock.forName(unicodeBlockName);
                        } catch (IllegalArgumentException iae) {
                            throw new ParseException("Invalid unicode block \"" + unicodeBlockName + "\"");
                        }

                        CharacterClass result = CharacterClasses.inUnicodeBlock(block);

                        if (t.text.charAt(1) == 'P') result = CharacterClasses.negate(result, t.text);

                        return result;
                    }

                case CC_UNICODE_CATEGORY:
                    {
                        String  gcName = t.text.substring(3, 5);
                        Byte    gc     = Pattern.getGeneralCategory(gcName);
                        if (gc == null) throw new ParseException("Unknown general cateogry \"" + gcName + "\"");

                        CharacterClass result = CharacterClasses.inUnicodeGeneralCategory(gc);

                        if (t.text.charAt(1) == 'P') result = CharacterClasses.negate(result, t.text);

                        return result;
                    }

                default:
                    throw new ParseException("Character class expected instead of \"" + t + "\" (" + t.type + ")");
                }
            }

            private CharacterClass
            parseCcIntersection() throws ParseException {

                CharacterClass result = this.parseCcUnion();

                while (this.peekRead(CC_INTERSECTION) != null) {
                    result = CharacterClasses.intersection(result, this.parseCcUnion());
                }

                return result;
            }

            private CharacterClass
            parseCcUnion() throws ParseException {

                CharacterClass result = this.parseCcRange();

                while (this.peek(RIGHT_BRACKET, CC_INTERSECTION) == -1) {
                    result = CharacterClasses.union(result, this.parseCcRange());
                }

                return result;
            }

            private CharacterClass
            parseCcRange() throws ParseException {

                String lhs = this.peekRead(LITERAL_CHARACTER);
                if (lhs == null) return this.parseCharacterClass();

                int lhsCp = lhs.codePointAt(0);

                if (!this.peekRead("-")) return CharacterClasses.literalCharacter(lhsCp, lhs);

                int rhsCp = this.read(LITERAL_CHARACTER).codePointAt(0);

                return CharacterClasses.range(lhsCp, rhsCp);
            }
        }.parse();
    }

    @Nullable private static Byte
    getGeneralCategory(String name) {

        Map<String, Byte> m = Pattern.generalCategories;
        if (m == null) {
            m = new HashMap<String, Byte>();

            // The JRE provides no way to translate GC names int GC values.
            m.put("Cn", Character.UNASSIGNED);
            m.put("Lu", Character.UPPERCASE_LETTER);
            m.put("Ll", Character.LOWERCASE_LETTER);
            m.put("Lt", Character.TITLECASE_LETTER);
            m.put("Lm", Character.MODIFIER_LETTER);
            m.put("Lo", Character.OTHER_LETTER);
            m.put("Mn", Character.NON_SPACING_MARK);
            m.put("Me", Character.ENCLOSING_MARK);
            m.put("Mc", Character.COMBINING_SPACING_MARK);
            m.put("Nd", Character.DECIMAL_DIGIT_NUMBER);
            m.put("Nl", Character.LETTER_NUMBER);
            m.put("No", Character.OTHER_NUMBER);
            m.put("Zs", Character.SPACE_SEPARATOR);
            m.put("Zl", Character.LINE_SEPARATOR);
            m.put("Zp", Character.PARAGRAPH_SEPARATOR);
            m.put("Cc", Character.CONTROL);
            m.put("Cf", Character.FORMAT);
            m.put("Co", Character.PRIVATE_USE);
            m.put("Cs", Character.SURROGATE);
            m.put("Pd", Character.DASH_PUNCTUATION);
            m.put("Ps", Character.START_PUNCTUATION);
            m.put("Pe", Character.END_PUNCTUATION);
            m.put("Pc", Character.CONNECTOR_PUNCTUATION);
            m.put("Po", Character.OTHER_PUNCTUATION);
            m.put("Sm", Character.MATH_SYMBOL);
            m.put("Sc", Character.CURRENCY_SYMBOL);
            m.put("Sk", Character.MODIFIER_SYMBOL);
            m.put("So", Character.OTHER_SYMBOL);
            m.put("Pi", Character.INITIAL_QUOTE_PUNCTUATION);
            m.put("Pf", Character.FINAL_QUOTE_PUNCTUATION);

            Pattern.generalCategories = Collections.unmodifiableMap(m);
        }

        return m.get(name);
    }
    @Nullable private static Map<String, Byte> generalCategories;
}
