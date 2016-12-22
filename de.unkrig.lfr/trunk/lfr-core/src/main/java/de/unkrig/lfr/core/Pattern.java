
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

package de.unkrig.lfr.core;

import static de.unkrig.lfr.core.Pattern.TokenType.BOUNDARY_MATCHER;
import static de.unkrig.lfr.core.Pattern.TokenType.CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.CAPTURING_GROUP_BACK_REFERENCE;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_ANY;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_INTERSECTION;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_JAVA;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_NEGATION;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_POSIX;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_PREDEFINED;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_RANGE;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_UNICODE;
import static de.unkrig.lfr.core.Pattern.TokenType.EITHER_OR;
import static de.unkrig.lfr.core.Pattern.TokenType.END_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.GREEDY_QUANTIFIER;
import static de.unkrig.lfr.core.Pattern.TokenType.INDEPENDENT_NON_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.LEFT_BRACKET;
import static de.unkrig.lfr.core.Pattern.TokenType.LINEBREAK_MATCHER;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_CHARACTER;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_CONTROL;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_HEXADECIMAL;
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_OCTAL;
import static de.unkrig.lfr.core.Pattern.TokenType.MATCH_FLAGS;
import static de.unkrig.lfr.core.Pattern.TokenType.MATCH_FLAGS_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.NAMED_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.NAMED_CAPTURING_GROUP_BACK_REFERENCE;
import static de.unkrig.lfr.core.Pattern.TokenType.NEGATIVE_LOOKAHEAD;
import static de.unkrig.lfr.core.Pattern.TokenType.NEGATIVE_LOOKBEHIND;
import static de.unkrig.lfr.core.Pattern.TokenType.NON_CAPTURING_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.POSITIVE_LOOKAHEAD;
import static de.unkrig.lfr.core.Pattern.TokenType.POSITIVE_LOOKBEHIND;
import static de.unkrig.lfr.core.Pattern.TokenType.POSSESSIVE_QUANTIFIER;
import static de.unkrig.lfr.core.Pattern.TokenType.QUOTATION_BEGIN;
import static de.unkrig.lfr.core.Pattern.TokenType.QUOTATION_END;
import static de.unkrig.lfr.core.Pattern.TokenType.QUOTED_CHARACTER;
import static de.unkrig.lfr.core.Pattern.TokenType.RELUCTANT_QUANTIFIER;
import static de.unkrig.lfr.core.Pattern.TokenType.RIGHT_BRACKET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.parser.AbstractParser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.StatefulScanner;

/**
 * A drop-in replacement for {@link java.util.regex.Pattern}.
 */
public final
class Pattern {

    private final String pattern;
    private final Node   node;
    private int          groupCount;

    static { AssertionUtil.enableAssertionsForThisClass(); }

    /**
     * A drop-in replacement for {@link java.util.regex.Matcher}.
     */
    public
    interface Matcher {

        /**
         * @see java.util.regex.Matcher#pattern()
         */
        Pattern pattern();

//        /**
//         * @see java.util.regex.Matcher#toMatchResult()
//         */
//        MatchResult toMatchResult();

        /**
         * @see java.util.regex.Matcher#usePattern(java.util.regex.Pattern)
         */
        Matcher usePattern(Pattern newPattern);

        /**
         * @see java.util.regex.Matcher#reset()
         */
        Matcher reset();

        /**
         * @see java.util.regex.Matcher#reset(CharSequence)
         */
        Matcher reset(CharSequence input);

        /**
         * @see java.util.regex.Matcher#start()
         */
        int start();

//        /**
//         * @see java.util.regex.Matcher#start(int)
//         */
//        int start(int group);

        /**
         * @see java.util.regex.Matcher#end()
         */
        int end();

//        /**
//         * @see java.util.regex.Matcher#end(int)
//         */
//        int end(int group);

        /**
         * @see java.util.regex.Matcher#group()
         */
        String group();

//        /**
//         * @see java.util.regex.Matcher#group(int)
//         */
//        String group(int group);
//
//        /**
//         * @see java.util.regex.Matcher#groupCount()
//         */
//        int groupCount();

        /**
         * @see java.util.regex.Matcher#matches()
         */
        boolean matches();

        /**
         * @see java.util.regex.Matcher#find()
         */
        boolean find();

        /**
         * @see java.util.regex.Matcher#find(int)
         */
        boolean find(int start);

        /**
         * @see java.util.regex.Matcher#lookingAt()
         */
        boolean lookingAt();
    }

    // SUPPRESS CHECKSTYLE JavadocVariable:59
    enum TokenType {

        // Literals.
        LITERAL_CHARACTER,
        /** {@code \0}<var>nnn</var> */
        LITERAL_OCTAL,
        /** {@code \x}<var>hh</var> <code>&#92;u</code><var>hhhh</var> <code>&#92;u{</code><var>hhhh</var><code>}</code> */
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
        CC_ANY,
        /** {@code \d \D \h \H \s \S \v \V \w \W} */
        CC_PREDEFINED,
        CC_POSIX,
        CC_JAVA,
        CC_UNICODE,

        // Matchers.
        /** {@code ^ $ \b \B \A \G \Z \z} */
        BOUNDARY_MATCHER,
        LINEBREAK_MATCHER,

        // Quantifiers.
        GREEDY_QUANTIFIER,
        RELUCTANT_QUANTIFIER,
        POSSESSIVE_QUANTIFIER,

        // Logical operators.
        EITHER_OR,
        CAPTURING_GROUP,
        /** {@code )} */
        END_GROUP,
        /** {@code \}<var>d</var> */
        CAPTURING_GROUP_BACK_REFERENCE,
        /** {@code \<}<var>name</var>{@code >} */
        NAMED_CAPTURING_GROUP_BACK_REFERENCE,
        NAMED_CAPTURING_GROUP,
        NON_CAPTURING_GROUP,
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
        MATCH_FLAGS_CAPTURING_GROUP,

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

    enum State { CHAR_CLASS1, CHAR_CLASS2, CHAR_CLASS3, IN_QUOTATION } // SUPPRESS CHECKSTYLE JavadocVariable

    static
    class Match implements Cloneable {

        /**
         * The indices of all matched capturing group, as start-end pairs. "-1" indicates a missing group. The last
         * element indicates whether the groups array is shared between multiple {@link Match} instances.
         */
        private int[] groups;

        private final CharSequence subject;

        private int offset;

        Match(int groupCount, CharSequence subject, int offset) {
            this.subject = subject;
            this.offset = offset;
            this.groups  = new int[groupCount * 2 + 1];
            Arrays.fill(this.groups, 0, groupCount * 2, -1);
        }

        Match(Match that) {
            this.groups  = that.groups;
            this.subject = that.subject;
            this.offset  = that.offset;
            this.setGroupsShared(true);
        }

        char
        peek() { return this.subject.charAt(this.offset); }

        char
        read() { return this.subject.charAt(this.offset++); }

        boolean
        atEnd() { return this.offset >= this.subject.length(); }

        Match
        setFrom(Match that) {
            assert this.subject == that.subject;
            this.groups  = that.groups;
            this.offset  = that.offset;
            this.setGroupsShared(true);
            return this;
        }

        @Override public Match
        clone() {
            try {
                return ((Match) super.clone()).setGroupsShared(true);
            } catch (CloneNotSupportedException cnse) {
                throw new AssertionError(cnse);
            }
        }

        public Match
        setGroupStart(int groupIndex) {
            if (this.isGroupsShared()) {
                this.groups = Arrays.copyOf(this.groups, this.groups.length);
                this.setGroupsShared(false);
            }
            this.groups[2 * groupIndex] = this.offset;
            return this;
        }

        public Match
        setGroupEnd(int groupIndex) {
            if (this.isGroupsShared()) {
                this.groups = Arrays.copyOf(this.groups, this.groups.length);
                this.setGroupsShared(false);
            }
            this.groups[2 * groupIndex + 1] = this.offset;
            return this;
        }

        @Nullable Match next() { return null; }

        private Match
        setGroupsShared(boolean value) { this.groups[this.groups.length - 1] = value ? 1 : 0; return this; }

        private boolean
        isGroupsShared() { return this.groups[this.groups.length - 1] != 0; }
    }

    private
    interface Node {

        /**
         * Checks whether the <var>subject</var>, starting at the given <var>match</var>, matches.
         *
         * @return The state a successful match, or {@code null} iff the <var>subject</var> does not match
         */
        @Nullable Match
        bestMatch(Match match);
    }

    private abstract static
    class CcNode implements Node, Predicate<Character> {

        @Override @Nullable public final Match
        bestMatch(Match match) { return !match.atEnd() && this.evaluate(match.read()) ? match : null; }
    }

    public static
    class CcNegation extends CcNode {

        private final Predicate<Character> delegate;

        public CcNegation(Predicate<Character> delegate) { this.delegate = delegate; }

        @Override public boolean evaluate(Character subject) { return !this.delegate.evaluate(subject); }
    }

    static
    class Sequence implements Node {

        private final Node prefix, suffix;

        Sequence(Node prefix, Node suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override @Nullable public Match
        bestMatch(Match match) {

            Match prefixMatch = this.prefix.bestMatch(match.clone());
            if (prefixMatch == null) return null;

            Match suffixMatch = this.suffix.bestMatch(prefixMatch.clone());
            while (suffixMatch == null) {
                prefixMatch = prefixMatch.next();
                if (prefixMatch == null) return null;
                suffixMatch = this.suffix.bestMatch(prefixMatch.clone());
            }

            final Match prefixMatch2 = prefixMatch, suffixMatch2 = suffixMatch;
            return new Match(suffixMatch) {

                Match pm = prefixMatch2, sm = suffixMatch2;

                @Override @Nullable public Match
                next() {
                    Match tmp = this.sm.next();
                    if (tmp != null) return this.setFrom(this.sm);

                    for (;;) {
                        tmp = this.pm.next();
                        if (tmp == null) return null;
                        this.pm = tmp;

                        tmp = Sequence.this.suffix.bestMatch(tmp);
                        if (tmp != null) return this.setFrom(this.sm);
                    }
                }
            };
        }
    }

    /**
     * Representation of the "|" operator.
     */
    public static
    class Alternatives implements Node {

        private final List<Node> alternatives;

        public
        Alternatives(List<Node> alternatives) { this.alternatives = alternatives; }

        @Override @Nullable public Match
        bestMatch(Match match) {
            Iterator<Node> it = this.alternatives.iterator();
            while (it.hasNext()) {
                Match m = it.next().bestMatch(match.clone());
                if (m != null) return m;
            }
            return null;
        }
    }

    public
    class CapturingGroup implements Node {

        private final int  groupIndex;
        private final Node subnode;

        /**
         * @param groupIndex 0==group 1, ...
         */
        public
        CapturingGroup(int groupIndex, Node subnode) { this.groupIndex = groupIndex; this.subnode = subnode; }

        @Override @Nullable public Match
        bestMatch(Match match) {

            match.setGroupStart(this.groupIndex);

            final Match bm = this.subnode.bestMatch(match);
            if (bm == null) return null;

            return new Match(bm) {

                @Override @Nullable Match
                next() {

                    final Match nm = bm.next();
                    if (nm == null) return null;

                    this.setFrom(nm);
                    this.setGroupEnd(CapturingGroup.this.groupIndex);

                    return this;
                }
            };
        }
    }

    /**
     * Representation of literal characters like "a" or "\.".
     */
    public static
    class CcLiteralCharacter extends CcNode {

        private char c;

        public
        CcLiteralCharacter(char c) { this.c = c; }

        @Override public boolean
        evaluate(Character subject) throws RuntimeException { return subject.equals(this.c); }
    }

    /**
     * Representation of a character class intersection like {@code "\w&&[^abc]"}.
     */
    public static
    class CcIntersection extends CcNode {

        private Predicate<Character> lhs, rhs;

        public CcIntersection(Predicate<Character> lhs, Predicate<Character> rhs) { this.lhs = lhs; this.rhs = rhs; }

        @Override public boolean
        evaluate(Character subject) { return this.lhs.evaluate(subject) && this.rhs.evaluate(subject); }
    }

    /**
     * Representation of a character class union like {@code "ab"}.
     */
    public static
    class CcUnion extends CcNode {

        private Predicate<Character> lhs, rhs;

        public CcUnion(Predicate<Character> lhs, Predicate<Character> rhs) { this.lhs = lhs; this.rhs = rhs; }

        @Override public boolean
        evaluate(Character subject) { return this.lhs.evaluate(subject) || this.rhs.evaluate(subject); }
    }

    /**
     * Representation of a character class range like {@code "a-k"}.
     */
    public static
    class CcRange extends CcNode {

        private char lhs, rhs;

        public CcRange(char lhs, char rhs) { this.lhs = lhs; this.rhs = rhs; }

        @Override public boolean
        evaluate(Character subject) { return subject >= this.lhs && subject <= this.rhs; }
    }

    private
    Pattern(String pattern, RegexScanner rs) throws ParseException {
        this.pattern    = pattern;
        this.node       = this.parse(rs);
        this.groupCount = rs.groupCount;
    }

    static
    class RegexScanner extends StatefulScanner<TokenType, State> {

        int groupCount;

        public RegexScanner()                  { super(State.class); }
        public RegexScanner(RegexScanner that) { super(that);        }
    }

    /**
     * This scanner is intended to be cloned by {@link RegexScanner#RegexScanner(RegexScanner)} when a regex scanner is
     * needed.
     */
    static final RegexScanner REGEX_SCANNER = new RegexScanner();
    static {
        StatefulScanner<TokenType, State> ss = REGEX_SCANNER;

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
        ss.addRule("\\[",                    LEFT_BRACKET,  State.CHAR_CLASS1);
        ss.addRule(State.CHAR_CLASS1, "\\[", LEFT_BRACKET,  State.CHAR_CLASS2);
        ss.addRule(State.CHAR_CLASS2, "\\[", LEFT_BRACKET,  State.CHAR_CLASS3);
        ss.addRule(State.CHAR_CLASS3, "]",   RIGHT_BRACKET, State.CHAR_CLASS2);
        ss.addRule(State.CHAR_CLASS2, "]",   RIGHT_BRACKET, State.CHAR_CLASS1);
        ss.addRule(State.CHAR_CLASS1, "]",   RIGHT_BRACKET);
        // [^abc]      Any character except a, b, or c (negation)
        ss.addRule(ss.ANY_STATE, "\\^", CC_NEGATION, null);
        // [a-zA-Z]    a through z or A through Z, inclusive (range)
        ss.addRule(EnumSet.allOf(State.class), "-", CC_RANGE, null);
        // [a-d[m-p]]  a through d, or m through p: [a-dm-p] (union)
        // [a-z&&[def]]    d, e, or f (intersection)
        // [a-z&&[^bc]]    a through z, except for b and c: [ad-z] (subtraction)
        // [a-z&&[^m-p]]   a through z, and not m through p: [a-lq-z] (subtraction)
        ss.addRule(EnumSet.allOf(State.class), "&&", CC_INTERSECTION, null);

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
        ss.addRule(ss.ANY_STATE, "\\\\p\\{(?:Lower|Upper|ASCII|Alpha|Digit|Alnum|Punct|Graph|Print|Blank|Cntrl|XDigit|Space)}", CC_POSIX, null); // SUPPRESS CHECKSTYLE LineLength

        // java.lang.Character classes (simple java character type)
        // \p{javaLowerCase}   Equivalent to java.lang.Character.isLowerCase()
        // \p{javaUpperCase}   Equivalent to java.lang.Character.isUpperCase()
        // \p{javaWhitespace}  Equivalent to java.lang.Character.isWhitespace()
        // \p{javaMirrored}    Equivalent to java.lang.Character.isMirrored()
        ss.addRule(ss.ANY_STATE, "\\\\p\\{(?:javaLowerCase|javaUpperCase|javaWhitespace|javaMirrored)}", CC_JAVA, null);

        // Classes for Unicode scripts, blocks, categories and binary properties
        // \p{IsLatin}        A Latin script character (script)
        // \p{InGreek}        A character in the Greek block (block)
        // \p{Lu}             An uppercase letter (category)
        // \p{IsAlphabetic}   An alphabetic character (binary property)
        // \p{Sc}             A currency symbol
        // \P{InGreek}        Any character except one in the Greek block (negation)
        // [\p{L}&&[^\p{Lu}]] Any letter except an uppercase letter (subtraction)
        ss.addRule(ss.ANY_STATE, "\\\\p\\{(?:IsLatin|InGreek|Lu|IsAlphabetic|Sc|InGreek)}", CC_UNICODE, null);

        // Boundary matchers
        // ^   The beginning of a line
        ss.addRule("\\^",          BOUNDARY_MATCHER);
        // $   The end of a line
        ss.addRule("\\$",          BOUNDARY_MATCHER);
        // \b  A word boundary
        // \B  A non-word boundary
        // \A  The beginning of the input
        // \G  The end of the previous match
        // \Z  The end of the input but for the final terminator, if any
        // \z  The end of the input
        ss.addRule("\\\\[bBAGZz]", BOUNDARY_MATCHER);

        // Linebreak matcher
        // \R  Any Unicode linebreak sequence, is equivalent to
        //                          /u000D/u000A|[/u000A/u000B/u000C/u000D/u0085/u2028/u2029]
        ss.addRule("\r\n|[\n\u000B\u000C\r\u0085\u2028\u2029]", LINEBREAK_MATCHER);

        // Greedy quantifiers
        // X?      X, once or not at all
        // X*      X, zero or more times
        // X+      X, one or more times
        // X{n}    X, exactly n times
        // X{n,}   X, at least n times
        // X{n,m}  X, at least n but not more than m times
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
        ss.addRule("\\|", EITHER_OR);
        // (X) X, as a capturing group
        ss.addRule("\\((?!\\?)", CAPTURING_GROUP);
        ss.addRule("\\)",        END_GROUP);

        // Back references
        // \n       Whatever the nth capturing group matched
        ss.addRule("\\\\\\d",    CAPTURING_GROUP_BACK_REFERENCE);
        // \k<name> Whatever the named-capturing group "name" matched
        ss.addRule("\\\\<\\w+>", NAMED_CAPTURING_GROUP_BACK_REFERENCE);

        // "Quotation"
        // Quotation
        // \   Nothing, but quotes the following character
        // \Q  Nothing, but quotes all characters until \E
        // \E  Nothing, but ends quoting started by \Q
        ss.addRule(ss.ANY_STATE,       "\\\\[^0-9A-Za-z]", QUOTED_CHARACTER,  null);
        ss.addRule("\\\\Q",                                QUOTATION_BEGIN,   State.IN_QUOTATION);
        ss.addRule(State.IN_QUOTATION, "\\\\E",            QUOTATION_END,     State.IN_QUOTATION);
        ss.addRule(State.IN_QUOTATION, ".",                LITERAL_CHARACTER, State.IN_QUOTATION);

        // "Special constructs (named-capturing and non-capturing)"
        // Special constructs (named-capturing and non-capturing)
        // (?<name>X)  X, as a named-capturing group
        // (?:X)   X, as a non-capturing group
        // (?idmsuxU-idmsuxU)  Nothing, but turns match flags i d m s u x U on - off
        // (?idmsux-idmsux:X)      X, as a non-capturing group with the given flags i d m s u x on - off
        // (?=X)   X, via zero-width positive lookahead
        // (?!X)   X, via zero-width negative lookahead
        // (?<=X)  X, via zero-width positive lookbehind
        // (?<!X)  X, via zero-width negative lookbehind
        // (?>X)   X, as an independent, non-capturing group
        ss.addRule("\\(\\?<\\w+>",                         NAMED_CAPTURING_GROUP);
        ss.addRule("\\(\\?:",                              NON_CAPTURING_GROUP);
        ss.addRule("\\(\\?[idmsuxU]*(?:-[idmsuxU]*)(?!:)", MATCH_FLAGS);
        ss.addRule("\\(\\?[idmsux]*(?:-[idmsux]*):",       MATCH_FLAGS_CAPTURING_GROUP);
        ss.addRule("\\(\\?=",                              POSITIVE_LOOKAHEAD);
        ss.addRule("\\(\\?!",                              NEGATIVE_LOOKAHEAD);
        ss.addRule("\\(\\?<=",                             POSITIVE_LOOKBEHIND);
        ss.addRule("\\(\\?<!",                             NEGATIVE_LOOKBEHIND);
        ss.addRule("\\(\\?",                               INDEPENDENT_NON_CAPTURING_GROUP);

        ss.addRule(ss.ANY_STATE, ".", LITERAL_CHARACTER, null); // +++
    }

    // ============================== CC NODES FOR "PREDEFINED CHARACTER CLASSES" ==============================

    /**  A digit: [0-9] */
    static final CcNode
    CC_NODE_IS_DIGIT = new CcNode() {
        @Override public boolean evaluate(Character subject) { return Character.isDigit(subject); }
    };

    /**  A non-digit: [^0-9] */
    static final CcNode
    CC_NODE_IS_NON_DIGIT = new CcNegation(CC_NODE_IS_DIGIT);

    /**  A horizontal whitespace character: <code>[ \t\xA0&#92;u1680&#92;u180e&#92;u2000-&#92;u200a&#92;u202f&#92;u205f&#92;u3000]</code> */
    static final CcNode
    CC_NODE_IS_HORIZONTAL_WHITESPACE = new CcNode() {
        @Override public boolean
        evaluate(Character subject) {
            return (
                "\t\u00A0\u1680\u180e\u202f\u205f\u3000".indexOf(subject) != -1
                || (subject >= '\u2000' && subject <= '\u200a')
            );
        }
    };

    /**  A non-horizontal whitespace character: [^\h] */
    static final CcNode
    CC_NODE_IS_NON_HORIZONTAL_WHITESPACE  = new CcNegation(CC_NODE_IS_HORIZONTAL_WHITESPACE);

    /**  A whitespace character: [ \t\n\x0B\f\r] */
    static final CcNode
    CC_NODE_IS_WHITESPACE = new CcNode() {
        @Override public boolean evaluate(Character subject) { return " \t\n\u000B\f\r".indexOf(subject) != -1; }
    };

    /**  A non-whitespace character: [^\s] */
    static final CcNode
    CC_NODE_IS_NON_WHITESPACE = new CcNegation(CC_NODE_IS_WHITESPACE);

    /**  A vertical whitespace character: [\n\x0B\f\r\x85/u2028/u2029] */
    static final CcNode
    CC_NODE_IS_VERTICAL_WHITESPACE = new CcNode() {
        @Override public boolean
        evaluate(Character subject) { return "\n\u000B\f\r\u0085\u2028\u2029".indexOf(subject) != -1; }
    };

    /**  A non-vertical whitespace character: [^\v] */
    static final CcNode
    CC_NODE_IS_NON_VERTICAL_WHITESPACE = new CcNegation(CC_NODE_IS_VERTICAL_WHITESPACE);

    /**  A word character: [a-zA-Z_0-9] */
    static final CcNode
    CC_NODE_IS_WORD = new CcNode() {
        @Override public boolean
        evaluate(Character subject) {
            return (
                (subject >= '0' && subject <= '9')
                || (subject >= 'A' && subject <= 'Z')
                || (subject >= 'a' && subject <= 'z')
            );
        }
    };

    /**  A non-word character: [^\w] */
    static final CcNode
    CC_NODE_IS_NON_WORD = new CcNegation(CC_NODE_IS_WORD);

    public static final Node EMPTY_SEQUENCE = new Node() {

        @Override public Match bestMatch(Match match) { return match; }
    };

    /**
     * @see java.util.regex.Pattern#compile(String)
     */
    public static Pattern
    compile(String regex) throws PatternSyntaxException {

        RegexScanner rs = new RegexScanner(REGEX_SCANNER);

        try {
            rs.setInput(regex);
            return new Pattern(regex, rs);
        } catch (ParseException pe) {
            PatternSyntaxException pse = new PatternSyntaxException(pe.getMessage(), regex, rs.getOffset());
            pse.initCause(pe);
            throw pse;
        }
    }

//    /**
//     * @see java.util.regex.Pattern#compile(String, int)
//     */
//    public static Pattern compile(String regex, int flags) {}

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
    matcher(final CharSequence subject) {

        return new Matcher() {

            private CharSequence subject2 = subject;
            private Pattern      pattern = Pattern.this;
            private int          offset;
            private int          start = -1, end;

            @Override public Pattern
            pattern() { return Pattern.this; }

            @Override public Matcher
            usePattern(Pattern newPattern) {
                this.start   = -1;
                this.pattern = newPattern;
                return this;
            }

            @Override public Matcher
            reset() {
                this.offset = 0;
                this.start  = -1;
                return this;
            }

            @Override public Matcher
            reset(CharSequence input) {
                this.subject2 = input;
                this.offset   = 0;
                this.start    = -1;
                return this;
            }

            @Override public int
            start() {
                if (this.start == -1) throw new IllegalStateException("No match available");
                return this.start;
            }

            @Override public int
            end() {
                if (this.start == -1) throw new IllegalStateException("No match available");
                return this.end;
            }

            @Override public String
            group() {
                if (this.start == -1) throw new IllegalStateException("No match available");
                return this.subject2.subSequence(this.start, this.end).toString();
            }

            @Override public boolean
            matches() {
                if (!this.pattern.matches(this.subject2, 0)) return false;
                this.start = 0;
                this.end   = this.subject2.length();
                return true;
            }

            @Override public boolean
            find() { return this.find(this.offset); }

            @Override public boolean
            find(int start) {

                for (Match m = new Match(Pattern.this.groupCount, subject, start);;) {

                    Match m2 = this.pattern.node.bestMatch(m);
                    if (m2 != null) {
                        this.start  = start;
                        this.offset = (this.end = m2.offset);
                        if (this.start == this.end) m.read();
                        return true;
                    }

                    if (m.atEnd()) break;

                    m.read();
                }
                this.start = -1;
                return false;
            }

            @Override public boolean
            lookingAt() {

                Match m = this.pattern.node.bestMatch(new Match(Pattern.this.groupCount, subject, this.offset));
                if (m == null) return false;

                this.start = 0;
                this.end   = m.offset;
                return true;
            }
        };
    }

//    /**
//     * @see java.util.regex.Pattern#flags()
//     */
//  public int flags();

    /**
     * @see java.util.regex.Pattern#matches(String, CharSequence)
     * @see java.util.regex.Pattern#split(CharSequence, int)
     * @see java.util.regex.Pattern#split(CharSequence)
     * @see java.util.regex.Pattern#quote(String)
     */
    public static boolean
    matches(String regex, CharSequence input) { return Pattern.compile(regex).matches(input, 0); }

//    /**
//     * @see java.util.regex.Pattern#split(CharSequence, int)
//     */
//  public String[] split(CharSequence input, int limit);
//
//    /**
//     * @see java.util.regex.Pattern#split(CharSequence)
//     */
//  public String[] split(CharSequence input);
//
//    /**
//     * @see java.util.regex.Pattern#quote(String)
//     */
//  public static String quote(String s);

    /**
     * @return Whether the suffix starting at position <var>offset</var> matches this pattern
     */
    public boolean
    matches(CharSequence subject, int offset) {

        for (
            Match m = Pattern.this.node.bestMatch(new Match(Pattern.this.groupCount, subject, offset));
            m != null;
            m = m.next()
        ) {
            if (m.atEnd()) return true;
        }

        return false;
    }

    /**
     * Parses a regular expression into a tree of nodes.
     */
    private Node
    parse(final RegexScanner rs) throws ParseException {

        return new AbstractParser<TokenType>(rs) {

            public Node
            parse() throws ParseException { return this.parseAlternatives(); }

            private Node
            parseAlternatives() throws ParseException {

                Node op1 = this.parseSequence();
                if (!this.peekRead("|")) return op1;

                List<Node> alternatives = new ArrayList<Node>();
                alternatives.add(op1);
                alternatives.add(this.parseSequence());
                while (this.peekRead("|")) alternatives.add(this.parseSequence());
                return new Alternatives(alternatives);
            }

            private Node
            parseSequence() throws ParseException {

                if (this.peek() == null || this.peekRead("|")) return EMPTY_SEQUENCE;

                Node result = this.parseQuantified();
                while (this.peek(null, "|", ")") == -1) result = new Sequence(result, this.parseQuantified());
                return result;
            }

            private Node
            parseQuantified() throws ParseException {

                final Node op = this.parsePrimary();

                Token<TokenType> t = this.peek();
                if (t == null) return op;

                switch (t.type) {

                case GREEDY_QUANTIFIER:
                case RELUCTANT_QUANTIFIER:
                case POSSESSIVE_QUANTIFIER:
                    this.read();
                    final int min, max;
                    switch (t.text.charAt(0)) {

                    case '?':
                        min = 0;
                        max = 1;
                        break;

                    case '*':
                        min = 0;
                        max = Integer.MAX_VALUE;
                        break;

                    case '+':
                        min = 1;
                        max = Integer.MAX_VALUE;
                        break;

                    case '{':
                        int idx1 = t.text.indexOf(',');
                        int idx2 = t.text.indexOf('}', idx1 + 1);
                        if (idx1 == -1) {
                            min = (max = Integer.parseInt(t.text.substring(1, idx2)));
                        } else {
                            min = Integer.parseInt(t.text.substring(1, idx1++));
                            max = idx1 == idx2 ? Integer.MAX_VALUE : Integer.parseInt(t.text.substring(idx1, idx2));
                        }
                        break;
                    default:
                        throw new AssertionError(t);
                    }

                    switch (t.type) {

                    case GREEDY_QUANTIFIER:
                        return new Node() {

                            @Override @Nullable public Match
                            bestMatch(Match match) {

                                return new Match(match) {

                                    Match[] state = new Match[Math.min(max, 10)];
                                    int     i;

                                    @Override @Nullable public Match
                                    next() {
                                        if (this.i < 0) return null;
                                        for (;;) {
                                            this.state[this.i] = (
                                                this.state[this.i] == null
                                                ? op.bestMatch(this.clone())
                                                : this.state[this.i].next()
                                            );
                                            if (this.state[this.i] != null) {
                                                if (this.i + 1 >= max) {
                                                    return this.setFrom(this.state[this.i]);
                                                }
                                                this.i++;
                                                if (this.i >= this.state.length) {
                                                    this.state = Arrays.copyOf(this.state, 2 * this.i);
                                                }
                                            } else {
                                                if (this.i < min) return null;
                                                this.i--;
                                                if (this.i >= 0) this.setFrom(this.state[this.i - 1]);
                                                return this;
                                            }
                                        }
                                    }
                                }.next();
                            }
                        };

                    case RELUCTANT_QUANTIFIER:
                        return new Node() {

                            @Override @Nullable public Match
                            bestMatch(Match match) {

                                return new Match(match) {

                                    int     curr  = min;
                                    Match[] state = new Match[this.curr];
                                    int     i;

                                    @Override @Nullable public Match
                                    next() {
                                        for (;;) {
                                            if (this.i == this.curr) {
                                                if (this.curr == max) return null;
                                                if (this.i > 0) this.setFrom(this.state[this.i - 1]);
                                                this.state = new Match[++this.curr];
                                                this.i     = 0;
                                                return this;
                                            }
                                            if (this.state[this.i] == null) {
                                                this.state[this.i] = (
                                                    op.bestMatch(this.i == 0 ? this : this.state[this.i - 1])
                                                );
                                                if (this.state[this.i] != null) {
                                                    this.i++;
                                                } else {
                                                    if (--this.i < 0) return null;
                                                }
                                            }
                                        }
                                    }
                                }.next();
                            }
                        };

                    case POSSESSIVE_QUANTIFIER:
                        return new Node() {

                            @Override @Nullable public Match
                            bestMatch(Match match) {

                                Match m = match.clone();

                                int i = 0;
                                for (; i < min; i++) {
                                    m = op.bestMatch(m);
                                    if (m == null) return null;
                                }
                                for (; i < max; i++) {
                                    m = op.bestMatch(m);
                                    if (m == null) return match;
                                }

                                return m;
                            }
                        };

                    default:
                        throw new AssertionError(t);
                    }

                default:
                    return op;
                }
            }

            private Node
            parsePrimary() throws ParseException {
                Node result = this.parseOptionalPrimary();
                if (result == null) {
                    Token<TokenType> t = this.peek();
                    throw new ParseException((
                        t == null
                        ? "Primary expected instead of end-of-input"
                        : "Primary expected instead of \"" + t + "\" (" + t.type + ")"
                    ));
                }
                return result;
            }

            @Nullable private Node
            parseOptionalPrimary() throws ParseException {

                Token<TokenType> t = this.peek();
                if (t == null) return null;

                switch (t.type) {

                case LITERAL_CHARACTER:
                case LITERAL_CONTROL:
                case LITERAL_HEXADECIMAL:
                case LITERAL_OCTAL:
                case LEFT_BRACKET:
                case CC_PREDEFINED:
                    return this.parseCharacterClass();

                case CC_ANY:
                    this.read();
                    return new CcNode() {
                        @Override public boolean evaluate(Character subject) throws RuntimeException { return true; }
                    };

                case EITHER_OR:
                case END_GROUP:
                case RIGHT_BRACKET:
                case QUOTATION_END:
                case GREEDY_QUANTIFIER:
                case POSSESSIVE_QUANTIFIER:
                case RELUCTANT_QUANTIFIER:
                    return null;

                case QUOTED_CHARACTER:
                    this.read();
                    return new CcLiteralCharacter(t.text.charAt(1));

                case QUOTATION_BEGIN:
                    {
                        this.read();
                        Node result = this.parseSequence();
                        if (this.peek() != null) this.read(TokenType.QUOTATION_END);
                        return result;
                    }

                case CAPTURING_GROUP:
                    {
                        this.read();
                        Node result = new CapturingGroup(rs.groupCount++, this.parseAlternatives());
                        this.read(")");
                        return result;
                    }

                case BOUNDARY_MATCHER:
                case CC_INTERSECTION:
                case CC_JAVA:
                case CC_POSIX:
                case CC_UNICODE:
                case INDEPENDENT_NON_CAPTURING_GROUP:
                case LINEBREAK_MATCHER:
                case MATCH_FLAGS:
                case MATCH_FLAGS_CAPTURING_GROUP:
                case NAMED_CAPTURING_GROUP:
                case CAPTURING_GROUP_BACK_REFERENCE:
                case NAMED_CAPTURING_GROUP_BACK_REFERENCE:
                case NEGATIVE_LOOKAHEAD:
                case NEGATIVE_LOOKBEHIND:
                case NON_CAPTURING_GROUP:
                case POSITIVE_LOOKAHEAD:
                case POSITIVE_LOOKBEHIND:
                    throw new ParseException("\"" + t + "\" (" + t.type + ") is not yet implemented");

                case CC_NEGATION:
                case CC_RANGE:
                    throw new AssertionError(t);

                default:
                    break;
                }

                throw new AssertionError(t);
            }

            CcNode
            parseCharacterClass() throws ParseException {
                Token<TokenType> t = this.read();
                switch (t.type) {

                case LITERAL_CHARACTER:
                    return new CcLiteralCharacter(t.text.charAt(0));

                case LITERAL_CONTROL:
                    {
                        int idx = "ctnrfae".indexOf(t.text.charAt(1));
                        assert idx != -1;
                        if (idx == 0) return new CcLiteralCharacter((char) (t.text.charAt(2) & 0x1f));
                        return new CcLiteralCharacter("c\t\n\r\f\u0007\u001b".charAt(idx));
                    }

                case LITERAL_HEXADECIMAL:
                    return new CcLiteralCharacter((char) Integer.parseInt(
                        t.text.charAt(2) == '{'
                        ? t.text.substring(3, t.text.length() - 1)
                        : t.text.substring(2)
                    ));

                case LITERAL_OCTAL:
                    return new CcLiteralCharacter((char) Integer.parseInt(t.text.substring(2, 8)));

                case LEFT_BRACKET:
                    boolean negate = this.peekRead("^");
                    CcNode  op     = this.parseCcIntersection();
                    this.read("]");
                    return negate ? new CcNegation(op) : op;

                case CC_PREDEFINED:
                    switch (t.text.charAt(1)) {
                    case 'd': return CC_NODE_IS_DIGIT;
                    case 'D': return CC_NODE_IS_NON_DIGIT;
                    case 'h': return CC_NODE_IS_HORIZONTAL_WHITESPACE;
                    case 'H': return CC_NODE_IS_NON_HORIZONTAL_WHITESPACE;
                    case 's': return CC_NODE_IS_WHITESPACE;
                    case 'S': return CC_NODE_IS_NON_WHITESPACE;
                    case 'v': return CC_NODE_IS_VERTICAL_WHITESPACE;
                    case 'V': return CC_NODE_IS_NON_VERTICAL_WHITESPACE;
                    case 'w': return CC_NODE_IS_WORD;
                    case 'W': return CC_NODE_IS_NON_WORD;
                    default:  throw new AssertionError(t);
                    }

                default:
                    throw new ParseException("Character class expected instead of \"" + t + "\" (" + t.type + ")");
                }
            }

            private CcNode
            parseCcIntersection() throws ParseException {
                CcNode result = this.parseCcUnion();
                while (this.peekRead("&&")) result = new CcIntersection(result, this.parseCcUnion());
                return result;
            }

            private CcNode
            parseCcUnion() throws ParseException {
                CcNode result = this.parseCcRange();
                while (this.peek("]", "&&") == -1) result = new CcUnion(result, this.parseCcRange());
                return result;
            }

            private CcNode
            parseCcRange() throws ParseException {
                String lhs = this.peekRead(LITERAL_CHARACTER);
                if (lhs == null) return this.parseCharacterClass();
                if (!this.peekRead("-")) return new CcLiteralCharacter(lhs.charAt(0));
                String rhs = this.read(LITERAL_CHARACTER);
                return new CcRange(lhs.charAt(0), rhs.charAt(0));
            }
        }.parse();
    }
}
