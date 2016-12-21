
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
import static de.unkrig.lfr.core.Pattern.TokenType.CC_POSIX;
import static de.unkrig.lfr.core.Pattern.TokenType.CC_PREDEFINED;
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
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.parser.AbstractParser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.commons.text.scanner.StatefulScanner;

/**
 * A drop-in replacement for {@link java.util.regex.Pattern}.
 */
public final
class Pattern {

    private final Node node;

    static { AssertionUtil.enableAssertionsForThisClass(); }

    /**
     * A drop-in replacement for {@link java.util.regex.Matcher}.
     */
    public
    interface Matcher { 
        
//        /**
//         * @see java.util.regex.Matcher#pattern()
//         */
//        Pattern pattern();
//
//        /**
//         * @see java.util.regex.Matcher#toMatchResult()
//         */
//        MatchResult toMatchResult();
//
//        /**
//         * @see java.util.regex.Matcher#usePattern(java.util.regex.Pattern)
//         */
//        Matcher usePattern(Pattern newPattern);
//
//        /**
//         * @see java.util.regex.Matcher#reset()
//         */
//        Matcher reset();
//
//        /**
//         * @see java.util.regex.Matcher#reset(CharSequence)
//         */
//        Matcher reset(CharSequence input);

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
//
//        /**
//         * @see java.util.regex.Matcher#group()
//         */
//        String group();
//
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

//        /**
//         * @see java.util.regex.Matcher#find(int)
//         */
//        boolean find(int start);

        /**
         * @see java.util.regex.Matcher#lookingAt()
         */
        boolean lookingAt();
    }

    // SUPPRESS CHECKSTYLE JavadocVariable:51
    enum TokenType {

        // Literals.
        LITERAL_CHARACTER,
        LITERAL_OCTAL,
        LITERAL_HEXADECIMAL,
        LITERAL_CONTROL,

        // Character classes.
        LEFT_BRACKET,
        RIGHT_BRACKET,
        CC_INTERSECTION,
        CC_ANY,
        CC_PREDEFINED,
        CC_POSIX,
        CC_JAVA,
        CC_UNICODE,

        // Matchers.
        BOUNDARY_MATCHER,
        LINEBREAK_MATCHER,

        // Quantifiers.
        GREEDY_QUANTIFIER,
        RELUCTANT_QUANTIFIER,
        POSSESSIVE_QUANTIFIER,

        // Logical operators.
        EITHER_OR,
        CAPTURING_GROUP,
        END_GROUP,
        CAPTURING_GROUP_BACK_REFERENCE,
        NAMED_CAPTURING_GROUP_BACK_REFERENCE,
        NAMED_CAPTURING_GROUP,
        NON_CAPTURING_GROUP,
        INDEPENDENT_NON_CAPTURING_GROUP,

        // Quotations.
        QUOTED_CHARACTER,
        QUOTATION_BEGIN,
        QUOTATION_END,

        // Setting flags.
        MATCH_FLAGS,
        MATCH_FLAGS_CAPTURING_GROUP,

        // Lookahead / lookbehind.
        POSITIVE_LOOKAHEAD,
        NEGATIVE_LOOKAHEAD,
        POSITIVE_LOOKBEHIND,
        NEGATIVE_LOOKBEHIND,
    }

    enum State { CHAR_CLASS1, CHAR_CLASS2, CHAR_CLASS3, IN_QUOTATION } // SUPPRESS CHECKSTYLE JavadocVariable

    static
    class Match {

        /**
         * The offset within the subject where this match ended.
         */
        int end;

        Match(int end) { this.end = end; }

        @Nullable Match next() { return null; }
    }

    private
    interface Node {

        /**
         * Checks whether the <var>subject</var>, starting at the given <var>offset</var>, matches.
         *
         * @return The offset <em>behind</em> the successful match, or {@code -1} iff the <var>subject</var> does not
         *         match
         */
        @Nullable Match
        bestMatch(CharSequence subject, int offset);
    }

    private abstract static 
    class CharacterClassNode implements Node, Predicate<Character> {

        @Override @Nullable public final Match
        bestMatch(CharSequence subject, int offset) {
            return offset < subject.length() && this.evaluate(subject.charAt(offset)) ? new Match(offset + 1) : null;
        }
    }

    static
    class Sequence implements Node {

        private final Node prefix, suffix;

        Sequence(Node prefix, Node suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override @Nullable public Match
        bestMatch(final CharSequence subject, int offset) {

            Match prefixMatch = this.prefix.bestMatch(subject, offset);
            if (prefixMatch == null) return null;

            Match suffixMatch = this.suffix.bestMatch(subject, prefixMatch.end);
            while (suffixMatch == null) {
                prefixMatch = prefixMatch.next();
                if (prefixMatch == null) return null;
                suffixMatch = this.suffix.bestMatch(subject, prefixMatch.end);
            }

            final Match prefixMatch2 = prefixMatch, suffixMatch2 = suffixMatch;
            return new Match(suffixMatch.end) {

                Match pm = prefixMatch2, sm = suffixMatch2;

                @Override @Nullable public Match
                next() {
                    Match tmp = this.sm.next();
                    if (tmp != null) return (this.sm = tmp);

                    for (;;) {
                        tmp = this.pm.next();
                        if (tmp == null) return null;
                        this.pm = tmp;
                        tmp     = Sequence.this.suffix.bestMatch(subject, this.pm.end);
                        if (tmp != null) return (this.sm = tmp);
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
        bestMatch(CharSequence subject, int offset) {
            Iterator<Node> it = this.alternatives.iterator();
            while (it.hasNext()) {
                Match m = it.next().bestMatch(subject, offset);
                if (m != null) return m;
            }
            return null;
        }
    }

    /**
     * Representation of literal characters like "a" or "\.".
     */
    public static
    class LiteralCharacter extends CharacterClassNode {

        private char c;

        public
        LiteralCharacter(char c) { this.c = c; }

        @Override public boolean
        evaluate(Character subject) throws RuntimeException { return subject.equals(this.c); }
    }

    private
    Pattern(Node node) { this.node = node; }
        
    /**
     * This scanner is intended to be cloned by {@link StatefulScanner#StatefulScanner(StatefulScanner)} when a
     * regex scanner is needed.
     */
    static final StatefulScanner<TokenType, State>
    REGEX_SCANNER = new StatefulScanner<TokenType, State>(State.class);
    static {
        StatefulScanner<TokenType, State> ss = REGEX_SCANNER;

        // Characters
        // x         The character x
        ss.addRule(ss.ANY_STATE, "[^\\\\\\[.\\^\\$\\(\\{\\*]",            LITERAL_CHARACTER,   null);
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
        // \cx The control character corresponding to x
        ss.addRule(ss.ANY_STATE, "\\\\x\\{[0-9a-fA-F]+}",                 LITERAL_HEXADECIMAL, null);
        // \t        The tab character ('/u0009')
        // \n        The newline (line feed) character ('/u000A')
        // \r        The carriage-return character ('/u000D')
        // \f        The form-feed character ('/u000C')
        // \a        The alert (bell) character ('/u0007')
        ss.addRule(ss.ANY_STATE, "\\\\[tnrfae]",                          LITERAL_CONTROL,     null);
        // \e        The escape character ('/u001B')
        ss.addRule(ss.ANY_STATE, "\\\\c[A-Za-z]",                         LITERAL_CONTROL,     null);

        // Character classes
        // [abc]       a, b, or c (simple class)
        // [^abc]      Any character except a, b, or c (negation)
        // [a-zA-Z]    a through z or A through Z, inclusive (range)
        // [a-d[m-p]]  a through d, or m through p: [a-dm-p] (union)
        ss.addRule("\\[",                    LEFT_BRACKET,  State.CHAR_CLASS1);
        ss.addRule(State.CHAR_CLASS1, "\\[", LEFT_BRACKET,  State.CHAR_CLASS2);
        ss.addRule(State.CHAR_CLASS2, "\\[", LEFT_BRACKET,  State.CHAR_CLASS3);
        ss.addRule(State.CHAR_CLASS3, "]",   RIGHT_BRACKET, State.CHAR_CLASS2);
        ss.addRule(State.CHAR_CLASS2, "]",   RIGHT_BRACKET, State.CHAR_CLASS1);
        ss.addRule(State.CHAR_CLASS1, "]",   RIGHT_BRACKET);
        // [a-z&&[def]]    d, e, or f (intersection)
        // [a-z&&[^bc]]    a through z, except for b and c: [ad-z] (subtraction)
        // [a-z&&[^m-p]]   a through z, and not m through p: [a-lq-z] (subtraction)
        ss.addRule(State.CHAR_CLASS1, "&&", CC_INTERSECTION, State.CHAR_CLASS1);
        ss.addRule(State.CHAR_CLASS2, "&&", CC_INTERSECTION, State.CHAR_CLASS2);
        ss.addRule(State.CHAR_CLASS3, "&&", CC_INTERSECTION, State.CHAR_CLASS3);

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
        ss.addRule(ss.ANY_STATE, "\\\\[^0-9A-Za-z]",    QUOTED_CHARACTER,  null);
        ss.addRule("\\\\Q",                     QUOTATION_BEGIN,   State.IN_QUOTATION);
        ss.addRule(State.IN_QUOTATION, "\\\\E", QUOTATION_END,     State.IN_QUOTATION);
        ss.addRule(State.IN_QUOTATION, ".",     LITERAL_CHARACTER, State.IN_QUOTATION);

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
        ss.addRule("\\(\\?[idmsuxU]*(?:-[idmsuxU]*):",     MATCH_FLAGS_CAPTURING_GROUP);
        ss.addRule("\\(\\?=",                              POSITIVE_LOOKAHEAD);
        ss.addRule("\\(\\?!",                              NEGATIVE_LOOKAHEAD);
        ss.addRule("\\(\\?<=",                             POSITIVE_LOOKBEHIND);
        ss.addRule("\\(\\?<!",                             NEGATIVE_LOOKBEHIND);
        ss.addRule("\\(\\?",                               INDEPENDENT_NON_CAPTURING_GROUP);

        ss.addRule(ss.ANY_STATE, ".", LITERAL_CHARACTER, null);
    }

    /**
     * @see java.util.regex.Pattern#compile(String)
     */
    public static Pattern
    compile(String s) throws PatternSyntaxException {

        StatefulScanner<TokenType, State> ss = new StatefulScanner<TokenType, State>(REGEX_SCANNER);
        
        ss.setInput(s);

        class RegexParser extends AbstractParser<TokenType> {

            RegexParser(ProducerWhichThrows<? extends Token<TokenType>, ? extends ScanException> scanner) {
                super(scanner);
            }

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

                if (this.peek() == null || this.peekRead("|")) return new Node() {
                    @Override public Match bestMatch(CharSequence subject, int offset) { return new Match(offset); }
                };

                return new Sequence(this.parseQuantified(), this.parseSequence());
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
                            min = Integer.parseInt(t.text.substring(1, idx1));
                            max = Integer.parseInt(t.text.substring(idx1 + 1, idx2));
                        }
                        break;
                    default:
                        throw new AssertionError(t);
                    }
                    
                    switch (t.type) {
                    
                    case GREEDY_QUANTIFIER:
                        return new Node() {
                            
                            @Override @Nullable public Match
                            bestMatch(final CharSequence subject, final int offset) {
                                
                                return new Match(0) {
                                    
                                    Match[] state = new Match[Math.min(max, 10)];
                                    int     i;

                                    @Override @Nullable public Match
                                    next() {
                                        for (;;) {
                                            this.state[this.i] = (
                                                this.state[this.i] == null
                                                ? op.bestMatch(
                                                    subject,
                                                    this.i == 0 ? offset : this.state[this.i - 1].end
                                                )
                                                : this.state[this.i].next()
                                            );
                                            if (this.state[this.i] != null) {
                                                if (this.i == max) {
                                                    this.end = this.state[this.i].end;
                                                    return this;
                                                }
                                                this.i++;
                                                if (this.i >= this.state.length) {
                                                    this.state = Arrays.copyOf(this.state, 2 * this.i);
                                                }
                                            } else {
                                                if (this.i == 0) return null;
                                                if (this.i >= min) {
                                                    this.end = this.state[--this.i].end;
                                                    return this;
                                                }
                                            }
                                        }
                                    }
                                }.next();
                            }
                        };
                        
                    case RELUCTANT_QUANTIFIER:
                        return new Node() {
                            
                            @Override @Nullable public Match
                            bestMatch(final CharSequence subject, final int offset) {
                                
                                return new Match(0) {
                                    
                                    int     curr  = min;
                                    Match[] state = new Match[this.curr];
                                    int     i;

                                    @Override @Nullable public Match
                                    next() {
                                        for (;;) {
                                            if (this.i == this.curr) {
                                                if (this.curr == max) return null;
                                                this.end   = this.i == 0 ? offset : this.state[this.i - 1].end;
                                                this.state = new Match[++this.curr];
                                                this.i     = 0;
                                                return this;
                                            }
                                            if (this.state[this.i] == null) {
                                                this.state[this.i] = (
                                                    op.bestMatch(subject, this.i == 0
                                                    ? offset
                                                    : this.state[this.i - 1].end)
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
                            bestMatch(final CharSequence subject, int offset) {
                                int i = 0;
                                for (; i < min; i++) {
                                    Match m = op.bestMatch(subject, offset);
                                    if (m == null) return null;
                                    offset = m.end;
                                }
                                for (; i < max; i++) {
                                    Match m = op.bestMatch(subject, offset);
                                    if (m == null) return new Match(offset);
                                    offset = m.end;
                                }
                                return new Match(offset);
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
                if (result == null) throw new ParseException("Primary expected");
                return result;
            }

            @Nullable private Node
            parseOptionalPrimary() throws ParseException {

                Token<TokenType> t = this.peek();
                if (t == null) return null;

                switch (t.type) {

                case LITERAL_CHARACTER:
                    this.read();
                    return new LiteralCharacter(t.text.charAt(0));

                case LITERAL_CONTROL:
                    this.read();
                    {
                        int idx = "ctnrfae".indexOf(t.text.charAt(1));
                        assert idx != -1;
                        if (idx == 0) return new LiteralCharacter((char) (t.text.charAt(2) & 0x1f));
                        return new LiteralCharacter("c\t\n\r\f\u0007\u001b".charAt(idx));
                    }

                case LITERAL_HEXADECIMAL:
                    this.read();
                    return new LiteralCharacter((char) Integer.parseInt(
                        t.text.charAt(2) == '{'
                        ? t.text.substring(3, t.text.length() - 1)
                        : t.text.substring(2)
                    ));

                case LITERAL_OCTAL:
                    this.read();
                    return new LiteralCharacter((char) Integer.parseInt(t.text.substring(2, 8)));

                case CC_ANY:
                    this.read();
                    return new CharacterClassNode() {
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
                    return new LiteralCharacter(t.text.charAt(1));

                case QUOTATION_BEGIN:
                    this.read();
                    Node result = this.parseSequence();
                    if (this.peek() != null) this.read(TokenType.QUOTATION_END);
                    return result;
                    
                case BOUNDARY_MATCHER:
                case CAPTURING_GROUP:
                case CAPTURING_GROUP_BACK_REFERENCE:
                case CC_INTERSECTION:
                case CC_JAVA:
                case CC_POSIX:
                case CC_PREDEFINED:
                case CC_UNICODE:
                case INDEPENDENT_NON_CAPTURING_GROUP:
                case LEFT_BRACKET:
                case LINEBREAK_MATCHER:
                case MATCH_FLAGS:
                case MATCH_FLAGS_CAPTURING_GROUP:
                case NAMED_CAPTURING_GROUP:
                case NAMED_CAPTURING_GROUP_BACK_REFERENCE:
                case NEGATIVE_LOOKAHEAD:
                case NEGATIVE_LOOKBEHIND:
                case NON_CAPTURING_GROUP:
                case POSITIVE_LOOKAHEAD:
                case POSITIVE_LOOKBEHIND:
                    throw new ParseException("\"" + t.text + "\" is not yet implemented");
                }

                throw new AssertionError(t);
            }
        }

        try {
            return new Pattern(new RegexParser(ss).parse());
        } catch (ParseException pe) {
            PatternSyntaxException pse = new PatternSyntaxException(pe.getMessage(), s, ss.getOffset());
            pse.initCause(pe);
            throw pse;
        }
    }

//    /**
//     * @see java.util.regex.Pattern#compile(String, int)
//     */
//    public static Pattern compile(String regex, int flags) {}
//
//    /**
//     * @see java.util.regex.Pattern#pattern()
//     */
//    public String pattern();
//
//    /**
//     * @see java.util.regex.Pattern#toString()
//     */
//    public String toString();

    /**
     * @see java.util.regex.Pattern#matcher(CharSequence)
     */
    public Matcher
    matcher(final CharSequence subject) {

        return new Matcher() {
            
            private int offset;
            private int start = -1, end;

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

            @Override public boolean
            matches() {
                if (!Pattern.this.matches(subject, 0)) return false;
                this.start = 0;
                this.end   = subject.length();
                return true;
            }

            @Override public boolean
            find() {
                for (; this.offset < subject.length(); this.offset++) {
                    Match m = Pattern.this.node.bestMatch(subject, this.offset);
                    if (m != null) {
                        this.start  = this.offset;
                        this.offset = (this.end = m.end);
                        return true;
                    }
                }
                this.start = -1;
                return false;
            }

            @Override public boolean
            lookingAt() {
                
                Match m = Pattern.this.node.bestMatch(subject, 0);
                if (m == null) return false;
                
                this.start = 0;
                this.end   = m.end;
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
        for (Match m = Pattern.this.node.bestMatch(subject, offset); m != null; m = m.next()) {
            if (m.end == subject.length()) return true;
        }
        return false;
    }
}
