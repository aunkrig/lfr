
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
import java.util.Arrays;
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
//        | Pattern.UNIX_LINES
//        | Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final EnumSet<ScannerState>
    IN_CHAR_CLASS = EnumSet.of(ScannerState.CHAR_CLASS1, ScannerState.CHAR_CLASS2, ScannerState.CHAR_CLASS3);

    // Interestingly, the following two are equal!
    private static final String LINE_BREAK_CHARACTERS          = "\r\n\u000B\f\u0085\u2028\u2029";
    private static final String VERTICAL_WHITESPACE_CHARACTERS = "\r\n\u000B\f\u0085\u2028\u2029";
    private static final String UNIX_LINE_BREAK_CHARACTERS     = "\n";
    private static final String WHITESPACE_CHARACTERS          = " \t\n\u000B\f\r";

    int                        flags;
    final String               pattern;
    final Sequence             sequence;
    final int                  groupCount;
    final Map<String, Integer> namedGroups;

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

        /**
         * @see java.util.regex.Matcher#start(String)
         */
        int start(String name);

        /**
         * @see java.util.regex.Matcher#start(int)
         */
        int start(int group);

        /**
         * @see java.util.regex.Matcher#end()
         */
        int end();

        /**
         * @see java.util.regex.Matcher#end(int)
         */
        int end(int group);

        /**
         * @see java.util.regex.Matcher#end(String)
         */
        int end(String name);

        /**
         * @see java.util.regex.Matcher#group()
         */
        String group();

        /**
         * @see java.util.regex.Matcher#group(int)
         */
        String group(int group);

        /**
         * @see java.util.regex.Matcher#group(String)
         */
        String group(String name);

        /**
         * @see java.util.regex.Matcher#groupCount()
         */
        int groupCount();

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

//        /**
//         * @see java.util.regex.Matcher#quoteReplacement(String)
//         */
//        static String quoteReplacement(String s) { return java.util.regex.Matcher.quoteReplacement(s); }

//        /**
//         * @see java.util.regex.Matcher#appendReplacement(StrigBuffer, String)
//         */
//        Matcher appendReplacement(StringBuffer sb, String replacement);
//
//        /**
//         * @see java.util.regex.Matcher#appendTail(StringBuffer)
//         */
//        StringBuffer appendTail(StringBuffer sb);
//
//        /**
//         * @see java.util.regex.Matcher#replaceAll(String)
//         */
//        String replaceAll(String replacement);
//
//        /**
//         * @see java.util.regex.Matcher#replaceFirst(String)
//         */
//        String replaceFirst(String replacement);
//
//        /**
//         * @see java.util.regex.Matcher#region(int, int)
//         */
//        Matcher region(int start, int end);
//
//        /**
//         * @see java.util.regex.Matcher#regionStart()
//         */
//        int regionStart();
//
//        /**
//         * @see java.util.regex.Matcher#regionEnd()
//         */
//        int regionEnd();
//
//        /**
//         * @see java.util.regex.Matcher#hasTransparentBounds()
//         */
//        boolean hasTransparentBounds();
//
//        /**
//         * @see java.util.regex.Matcher#useTransparentBounds(boolean)
//         */
//        Matcher useTransparentBounds(boolean b);
//
//        /**
//         * @see java.util.regex.Matcher#hasAnchoringBounds()
//         */
//        boolean hasAnchoringBounds();
//
//        /**
//         * @see java.util.regex.Matcher#useAnchoringBounds(boolean)
//         */
//        Matcher useAnchoringBounds(boolean b);

        /**
         * @see java.util.regex.Matcher#toString()
         */
        @Override String toString();

        /**
         * @see java.util.regex.Matcher#hitEnd()
         */
        boolean hitEnd();

//        /**
//         * @see java.util.regex.Matcher#requireEnd()
//         */
//        boolean requireEnd();
    }

    static final
    class MatcherImpl implements Matcher {

        // CONFIGURATION
        Pattern      pattern;
        CharSequence subject;

        // STATE
        int             offset;
        int[]           groups, initialGroups;
        private boolean hitEnd;
        boolean         hadMatch;

        @Nullable End end;

        MatcherImpl(Pattern pattern, CharSequence subject) {
            this.pattern = pattern;
            this.subject = subject;
            this.groups  = (this.initialGroups = new int[2 + 2 * pattern.groupCount]);
            Arrays.fill(this.groups, -1);
        }

        @Override public Pattern
        pattern() { return this.pattern; }

        @Override public Matcher
        usePattern(Pattern newPattern) {
            this.pattern = newPattern;
            this.groups  = (this.initialGroups = new int[2 + 2 * newPattern.groupCount]);
            Arrays.fill(this.groups, -1);
            return this;
        }

        @Override public Matcher
        reset() {
            this.offset   = 0;
            this.hadMatch = false;
            this.hitEnd   = false;
            return this;
        }

        @Override public Matcher
        reset(CharSequence input) {
            this.subject  = input;
            this.offset   = 0;
            this.hadMatch = false;
            this.hitEnd   = false;
            return this;
        }

        @Override public int
        start(int groupNumber) {

            if (!this.hadMatch) throw new IllegalStateException("No match available");

            return this.groups[2 * groupNumber];
        }

        @Override public int
        start(String groupName) {

            if (!this.hadMatch) throw new IllegalStateException("No match available");

            Integer groupNumber = this.pattern.namedGroups.get(groupName);
            if (groupNumber == null) throw new IllegalArgumentException("Invalid group name \"" + groupName + "\"");

            return this.groups[2 * groupNumber];
        }

        @Override public int
        end(int groupNumber) {

            if (!this.hadMatch) throw new IllegalStateException("No match available");

            return this.groups[2 * groupNumber + 1];
        }

        @Override public int
        end(String groupName) {

            if (!this.hadMatch) throw new IllegalStateException("No match available");

            Integer groupNumber = this.pattern.namedGroups.get(groupName);
            if (groupNumber == null) throw new IllegalArgumentException("Invalid group name \"" + groupName + "\"");

            return this.groups[2 * groupNumber + 1];
        }

        @Override public String
        group(int groupNumber) {

            if (!this.hadMatch) throw new IllegalStateException("No match available");

            int[] gs = this.groups;
            return this.subject.subSequence(gs[2 * groupNumber], gs[2 * groupNumber + 1]).toString();
        }

        @Override public String
        group(String groupName) {

            if (!this.hadMatch) throw new IllegalStateException("No match available");

            Integer groupNumber = this.pattern.namedGroups.get(groupName);
            if (groupNumber == null) throw new IllegalArgumentException("Invalid group name \"" + groupName + "\"");

            int[] gs = this.groups;
            return this.subject.subSequence(gs[2 * groupNumber], gs[2 * groupNumber + 1]).toString();
        }

        @Override public int    start() { return this.start(0); }
        @Override public int    end()   { return this.end(0);   }
        @Override public String group() { return this.group(0); }

        @Override public int     groupCount() { return this.pattern().groupCount; }
        @Override public boolean hitEnd()     { return this.hitEnd;             }

        @Override public boolean
        matches() { return this.matches(0, End.END_OF_SUBJECT); }

        @Override public boolean
        lookingAt() { return this.matches(0, End.ANY); }

        @Override public boolean
        find() {

            this.hitEnd = false;

            for (; this.offset <= this.subject.length(); this.offset++) {
                if (this.matches(this.offset, End.ANY)) return true;
            }

            return false;
        }

        @Override public boolean
        find(int start) {
            this.offset = start;
            return this.find();
        }

        // =====================================

        private boolean
        matches(int start, End end) {

            if (this.hadMatch && start == this.groups[1] && start == this.groups[0]) {
                if (start >= this.subject.length()) {
                    this.hadMatch = false;
                    this.hitEnd   = true;
                    return false;
                }
                start++;
            }

            this.offset = start;
            this.groups = this.initialGroups;
            this.end    = end;

            if (this.pattern.sequence.matches(this)) {

                this.groups[0] = start;
                this.groups[1] = this.offset;
                this.hadMatch  = true;

                return true;
            }

            this.hadMatch = false;

            return false;
        }

        public Character
        read() { return this.subject.charAt(this.offset++); }

        /**
         * If the next characters in this matcher equal <var>cs</var>, then the offset is advanced and {@code true} is
         * returned.
         */
        public boolean
        peekRead(CharSequence cs) {

            int          sLength = cs.length();
            CharSequence subject = this.subject;
            int          offset  = this.offset;

            if (offset + sLength > subject.length()) {

                // Not enough chars left.
                this.hitEnd = true;
                return false;
            }

            for (int i = 0; i < sLength; i++) {
                if (subject.charAt(offset + i) != cs.charAt(i)) return false;
            }

            this.offset += sLength;
            return true;
        }

        /**
         * If the <var>predicate</var> evaluates for the next character in this matcher equal <var>s</var>, then the
         * offset is advanced and {@code true} is returned.
         */
        public boolean
        peekRead(Predicate<Character> predicate) {

            if (this.offset >= this.subject.length()) {
                this.hitEnd = true;
                return false;
            }

            if (!predicate.evaluate(this.subject.charAt(this.offset))) return false;

            this.offset++;
            return true;
        }

        public char
        peek(int delta) { return this.subject.charAt(this.offset + delta); }

        public char
        peek() { return this.subject.charAt(this.offset); }

        public boolean
        atStart() { return this.offset == 0; }

        public boolean
        atEnd() { return this.offset >= this.subject.length(); }

        public int
        remaining() { this.hitEnd = true; return this.subject.length() - this.offset; }

        @Override public String
        toString() {
            StringBuilder sb = (
                new StringBuilder()
                .append("de.unkrig.lfr.core.Matcher[pattern=")
                .append(this.pattern())
                .append(" lastmatch=")
            );

            if (this.hadMatch) sb.append(this.subject.subSequence(this.groups[0], this.groups[1]));

            return sb.append(']').toString();
        }

        boolean
        sequenceEndMatches() { return this.end == End.ANY || this.atEnd(); }
    }

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
        /** {@code ^ $ \b \B \A \G \Z \z} */
        BEGINNING_OF_LINE,
        END_OF_LINE,
        WORD_BOUNDARY,
        NON_WORD_BOUNDARY,
        BEGINNING_OF_INPUT,
        END_OF_PREVIOUS_MATCH,
        END_OF_INPUT_BUT_FINAL_TERMINATOR,
        END_OF_INPUT,
        LINEBREAK_MATCHER,

        // Quantifiers.
        GREEDY_QUANTIFIER,
        RELUCTANT_QUANTIFIER,
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

    /**
     * A thing that can match a character sequence.
     */
    interface Sequence {

        /**
         * Checks whether the subject of the <var>matcher</var>, starting at the {@code currentOffset} of the
         * <var>matcher</var>, matches.
         * <p>
         *   If this sequence matches, then the method returns {@code true}, and the <var>matcher</var> reflects the
         *   state <em>after</em> the match (in its {@code currentOffset} and {@code groups} fields).
         * </p>
         * <p>
         *   Otherwise, if this sequence does <em>not</em> match, then this method returns {@code false}, and the state
         *   of the matcher is undefined.
         * </p>
         */
        boolean
        matches(MatcherImpl matcher);

        /**
         * Must be called exactly once, before the first invocation of {@link #matches(Matcher)}.
         */
        void
        linkTo(Sequence successor);

        boolean
        successorMatches(MatcherImpl matcher);
    }

    abstract static
    class AbstractSequence implements Sequence {

        @Nullable private Sequence successor;

        @Override public void
        linkTo(Sequence newSuccessor) {
            Sequence successor = this.successor;
            if (successor == null) {
                this.successor = newSuccessor;
            } else {
                successor.linkTo(newSuccessor);
            }
        }

        @Override public final boolean
        successorMatches(MatcherImpl matcher) {
            assert this.successor != null;
            return this.successor.matches(matcher);
        }
    }

    abstract static
    class CharacterClass extends AbstractSequence implements Predicate<Character> {

        @Override public boolean
        matches(MatcherImpl matcher) { return matcher.peekRead(this) && this.successorMatches(matcher); }
    }

    public static CharacterClass
    characterClass(final Predicate<Character> predicate) {

        return new CharacterClass() {
            @Override public boolean evaluate(Character subject) { return predicate.evaluate(subject); }
        };
    }

    public static CharacterClass
    ccInUnicodeBlock(final Character.UnicodeBlock block) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(Character subject) { return Character.UnicodeBlock.of(subject) == block; }
        };
    }

    /**
     * @param generalCategory One of the "general category" constants in {@link Character}
     */
    public static CharacterClass
    ccInUnicodeGeneralCategory(final int generalCategory) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(Character subject) { return Character.getType(subject) == generalCategory; }
        };
    }

    /**
     * Representation of literal characters like "a" or "\.".
     */
    public static
    class CcLiteralCharacter extends CharacterClass {

        private char c;

        public
        CcLiteralCharacter(char c) { this.c = c; }

        @Override public boolean
        evaluate(Character subject) { return subject == this.c; }

        @Override public String
        toString() { return new String(new char[] { this.c }); }
    }

    /**
     * Representation of a sequence of literal, case-sensitive characters.
     */
    static
    class LiteralString extends AbstractSequence {

        private String s;

        LiteralString(String s) { this.s = s; }

        @Override public boolean
        matches(MatcherImpl matcher) { return matcher.peekRead(this.s) && this.successorMatches(matcher); }

        @Override public String
        toString() { return this.s; }
    }

    // ============================================== Sequences ================================================

    public static Sequence
    emptyStringSequence() { return new EmptyStringSequence(); }

    public static Sequence
    literalString(final String s) { return new LiteralString(s); }

    /**
     * Representation of the "|" operator.
     */
    public static Sequence
    alternatives(final Sequence[] alternatives) {

        if (alternatives.length == 0) return Pattern.emptyStringSequence();

        if (alternatives.length == 1) return alternatives[0];

        return new AbstractSequence() {

            @Override public void
            linkTo(Sequence successor) {
                super.linkTo(successor);
                for (Sequence s : alternatives) s.linkTo(successor);
            }

            @Override public boolean
            matches(MatcherImpl matcher) {

                final int   savedOffset = matcher.offset;
                final int[] savedGroups = matcher.groups;

                if (alternatives[0].matches(matcher)) return true;

                for (int i = 1; i < alternatives.length; i++) {

                    matcher.offset = savedOffset;
                    matcher.groups = savedGroups;

                    if (alternatives[i].matches(matcher)) return true;
                }

                return false;
            }

            @Override public String
            toString() { return Pattern.join(alternatives, '|'); }
        };
    }

    /**
     * @param groupNumber 1...<var>groupCount</var>
     */
    public static Sequence
    capturingGroup(final int groupNumber, final Sequence subsequence) {

        subsequence.linkTo(Pattern.TERMINAL);

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {

                final int savedOffset = matcher.offset;

                if (!subsequence.matches(matcher)) return false;

                // Copy "this.groups" and store group start and end.
                int[] gs = (matcher.groups = Arrays.copyOf(matcher.groups, matcher.groups.length));
                gs[2 * groupNumber]     = savedOffset;
                gs[2 * groupNumber + 1] = matcher.offset;

                return this.successorMatches(matcher);
            }

            @Override public String
            toString() { return "(" + subsequence + ")"; }
        };
    }

    /**
     * Representation of a capturing group backreference, e.g. {@code \3} .
     *
     * @param groupNumber 1...<var>groupCount</var>
     */
    public static Sequence
    capturingGroupBackReference(final int groupNumber) {

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {

                int[]        gs    = matcher.groups;
                CharSequence group = matcher.subject.subSequence(
                    gs[2 * groupNumber],
                    gs[2 * groupNumber + 1]
                );

                return matcher.peekRead(group) && this.successorMatches(matcher);
            }

            @Override public String
            toString() { return "\\" + groupNumber; }
        };
    }

    public static Sequence
    beginningOfInput() {

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) { return matcher.atStart() && this.successorMatches(matcher); }

            @Override public String
            toString() { return "^"; }
        };
    }

    public static Sequence
    beginningOfLine(final String lineBreakCharacters) {

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {
                if ((
                    matcher.atStart()
                    || lineBreakCharacters.indexOf(matcher.peek(-1)) != -1
                ) && this.successorMatches(matcher)) return true;

                if (matcher.atEnd()) matcher.hitEnd = true;

                return false;
            }

            @Override public String
            toString() { return "^"; }
        };
    }

    public static Sequence
    endOfInputButFinalTerminator() {

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {

                if (matcher.atEnd()) {
                    matcher.hitEnd = true;
                    return this.successorMatches(matcher);
                }

                char c = matcher.peek();
                if (Pattern.LINE_BREAK_CHARACTERS.indexOf(c) == -1) return false;

                int savedOffset = matcher.offset;

                matcher.read();

                if (matcher.atEnd()) {
                    matcher.offset = savedOffset;
                    matcher.hitEnd = true;
                    return this.successorMatches(matcher);
                }

                if (c == '\r' && matcher.peek() == '\n') {
                    matcher.read();
                    if (matcher.atEnd()) {
                        matcher.offset = savedOffset;
                        matcher.hitEnd = true;
                        return this.successorMatches(matcher);
                    }
                }

                matcher.offset = savedOffset;
                return false;
            }

            @Override public String
            toString() { return "^"; }
        };
    }

    public static Sequence
    endOfInput() {

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {

                if (!matcher.atEnd()) return false;

                matcher.hitEnd = true;
                return this.successorMatches(matcher);
            }

            @Override public String
            toString() { return "^"; }
        };
    }

    public static Sequence
    endOfLine(final String lineBreakCharacters) {

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {
                if (matcher.atEnd()) {
                    matcher.hitEnd = true;
                    return this.successorMatches(matcher);
                }
                return lineBreakCharacters.indexOf(matcher.peek()) != -1 && this.successorMatches(matcher);
            }

            @Override public String
            toString() { return "^"; }
        };
    }

    public static Sequence
    wordBoundary() {

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {

                if (matcher.atEnd()) {
                    matcher.hitEnd = true;
                    return (
                        (matcher.atStart() || Pattern.isWordCharacter(matcher.peek(-1)))
                        && this.successorMatches(matcher)
                    );
                }

                if (matcher.atStart()) {
                    return Pattern.isWordCharacter(matcher.peek()) && this.successorMatches(matcher);
                }

                return (
                    Pattern.isWordCharacter(matcher.peek(-1))
                    ^ Pattern.isWordCharacter(matcher.peek())
                ) && this.successorMatches(matcher);
            }

            @Override public String
            toString() { return "^"; }
        };
    }

    public static Sequence
    nonWordBoundary() {

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {

                if (matcher.atEnd()) {
                    matcher.hitEnd = true;
                    return (
                        !matcher.atStart()
                        && !Pattern.isWordCharacter(matcher.peek(-1))
                        && this.successorMatches(matcher)
                    );
                }

                if (matcher.atStart()) {
                    return !Pattern.isWordCharacter(matcher.peek()) && this.successorMatches(matcher);
                }

                return (
                    !(Pattern.isWordCharacter(matcher.peek(-1)) ^ Pattern.isWordCharacter(matcher.peek()))
                    && this.successorMatches(matcher)
                );
            }

            @Override public String
            toString() { return "^"; }
        };
    }

    public static Sequence
    endOfPreviousMatch() {

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {

                // The documentation of java.util.regex is totally unclear about the following case, but this seems to
                // be how it works:
                if (!matcher.hadMatch) return this.successorMatches(matcher);

                return matcher.offset == matcher.groups[1] && this.successorMatches(matcher);
            }

            @Override public String
            toString() { return "^"; }
        };
    }

    // ====================================== Character classes ===========================================

    /**
     * Representation of literal characters like "a" or "\.".
     */
    public static CharacterClass
    ccLiteralCharacter(char c) { return new CcLiteralCharacter(c); }

    /**
     * Representation of a two-characters union, e.g. "[oO]".
     */
    public static CharacterClass
    ccOneOf(final char c1, final char c2) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(Character subject) { return subject == c1 || subject == c2; }

            @Override public String
            toString() { return "[" + c1 + c2 + ']'; }
        };
    }

    public static CharacterClass
    ccOneOf(final String chars) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(Character subject) { return chars.indexOf(subject) != -1; }

            @Override public String
            toString() { return '[' + chars + ']'; }
        };
    }

    /**
     * Representation of an (ASCII-)case-insensitive literal character.
     */
    public static CharacterClass
    ccCaseInsensitiveLiteralCharacter(final char c) {
        if (c >= 'A' && c <= 'Z') return Pattern.ccOneOf(c, (char) (c + 32));
        if (c >= 'a' && c <= 'z') return Pattern.ccOneOf(c, (char) (c - 32));
        return Pattern.ccLiteralCharacter(c);
    }

    /**
     * Representation of a (UNICODE-)case-insensitive literal character.
     */
    public static CharacterClass
    ccUnicodeCaseInsensitiveLiteralCharacter(final char c) {
        if (Character.isLowerCase(c)) return Pattern.ccOneOf(c, Character.toUpperCase(c));
        if (Character.isUpperCase(c)) return Pattern.ccOneOf(c, Character.toLowerCase(c));
        return Pattern.ccLiteralCharacter(c);
    }

    /**
     * Representation of a character class intersection like {@code "\w&&[^abc]"}.
     */
    public static CharacterClass
    ccIntersection(final Predicate<Character> lhs, final Predicate<Character> rhs) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(Character subject) { return lhs.evaluate(subject) && rhs.evaluate(subject); }

            @Override public String
            toString() { return lhs + "&&" + rhs; }
        };
    }

    /**
     * Representation of a character class union like {@code "ab"}.
     */
    public static CharacterClass
    ccUnion(final Predicate<Character> lhs, final Predicate<Character> rhs) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(Character subject) { return lhs.evaluate(subject) || rhs.evaluate(subject); }

            @Override public String
            toString() { return lhs.toString() + rhs; }
        };
    }

    /**
     * Representation of a character class range like {@code "a-k"}.
     */
    public static CharacterClass
    ccRange(final char lhs, final char rhs) {

        return new CharacterClass() {

            @Override public boolean
            evaluate(Character subject) { return subject >= lhs && subject <= rhs; }

            @Override public String
            toString() { return lhs + "-" + rhs; }
        };
    }

    /**  A digit: [0-9] */
    public static final CharacterClass
    ccIsDigit() {
        return new CharacterClass() {
            @Override public boolean evaluate(Character subject) { char c = subject; return c >= '0' && c <= '9'; }
            @Override public String  toString()                  { return "\\d"; }
        };
    }

    /**  A non-digit: [^0-9] */
    public static final CharacterClass
    ccIsNonDigit() { return Pattern.ccNegate(Pattern.ccIsDigit(), "\\D"); }

    public static CharacterClass
    ccNegate(final CharacterClass delegate, final String toString) {
        return new CharacterClass() {

            @Override public boolean
            evaluate(Character subject) { return !delegate.evaluate(subject); }

            @Override public String
            toString() { return toString; }
        };
    }

    /**  A horizontal whitespace character: <code>[ \t\xA0&#92;u1680&#92;u180e&#92;u2000-&#92;u200a&#92;u202f&#92;u205f&#92;u3000]</code> */ // SUPPRESS CHECKSTYLE LineLength
    public static final CharacterClass
    ccIsHorizontalWhitespace() {

        return new CharacterClass() {

            @Override public boolean
            evaluate(Character subject) {
                return (
                    "\t\u00A0\u1680\u180e\u202f\u205f\u3000".indexOf(subject) != -1
                    || (subject >= '\u2000' && subject <= '\u200a')
                );
            }

            @Override public String
            toString() { return "\\h"; }
        };
    }

    /**  A non-horizontal whitespace character: [^\h] */
    public static CharacterClass
    ccIsNonHorizontalWhitespace() { return Pattern.ccNegate(Pattern.ccIsHorizontalWhitespace(), "\\H"); }

    /**  A whitespace character: [ \t\n\x0B\f\r] */
    public static final CharacterClass
    ccIsWhitespace() {
        return Pattern.ccOneOf(Pattern.WHITESPACE_CHARACTERS);
    }

    /**  A non-whitespace character: [^\s] */
    public static final CharacterClass
    ccIsNonWhitespace() { return Pattern.ccNegate(Pattern.ccIsWhitespace(), "\\S"); }

    /**  A vertical whitespace character: [\n\x0B\f\r\x85/u2028/u2029] */
    public static final CharacterClass
    ccIsVerticalWhitespace() { return Pattern.ccOneOf(Pattern.VERTICAL_WHITESPACE_CHARACTERS); }

    /**  A non-vertical whitespace character: [^\v] */
    public static final CharacterClass
    ccIsNonVerticalWhitespace() { return Pattern.ccNegate(Pattern.ccIsVerticalWhitespace(), "\\V"); }

    /**  A word character: [a-zA-Z_0-9] */
    public static final CharacterClass
    ccIsWord() {
        return new CharacterClass() {
            @Override public boolean evaluate(Character subject) { return Pattern.isWordCharacter(subject); }
            @Override public String  toString()                  { return "\\w";                            }
        };
    }

    /**  A non-word character: [^\w] */
    public static final CharacterClass
    ccIsNonWord() { return Pattern.ccNegate(Pattern.ccIsWord(), "\\W"); }

    public static final CharacterClass
    ccAnyChar() {
        return new CharacterClass() {
            @Override public boolean evaluate(Character subject) { return true; }
            @Override public String  toString()                  { return ".";  }
        };
    }

    // ==========================================================

    Pattern(String pattern, int flags, Sequence sequence, int groupCount, Map<String, Integer> namedGroups) {

        this.flags       = flags;
        this.pattern     = pattern;
        this.namedGroups = namedGroups;

        sequence.linkTo(new Sequence() {

            @Override public boolean
            matches(MatcherImpl matcher) { return matcher.sequenceEndMatches(); }

            @Override public boolean successorMatches(MatcherImpl matcher) { throw new UnsupportedOperationException(); } // SUPPRESS CHECKSTYLE LineLength:2
            @Override public void    linkTo(Sequence successor)            { throw new UnsupportedOperationException(); }

            @Override public String toString() { return "[END]"; }
        });

        this.sequence   = sequence;
        this.groupCount = groupCount;
    }

    private static String
    join(@Nullable Object[] oa, char glue) {

        if (oa == null || oa.length == 0) return "";

        if (oa.length == 1) return String.valueOf(oa[0]);

        StringBuilder sb = new StringBuilder().append(oa[0]).append(glue).append(oa[1]);
        for (int i = 2; i < oa.length; i++) sb.append(glue).append(oa[i]);
        return sb.toString();
    }

    static
    class RegexScanner extends StatefulScanner<TokenType, ScannerState> {

        int                                  groupCount;
        final protected Map<String, Integer> namedGroups = new HashMap<String, Integer>();

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
        ss.addRule(ss.ANY_STATE, "\\\\p\\{(?:Lower|Upper|ASCII|Alpha|Digit|Alnum|Punct|Graph|Print|Blank|Cntrl|XDigit|Space)}", CC_POSIX, null); // SUPPRESS CHECKSTYLE LineLength

        // java.lang.Character classes (simple java character type)
        // \p{javaLowerCase}   Equivalent to java.lang.Character.isLowerCase()
        // \p{javaUpperCase}   Equivalent to java.lang.Character.isUpperCase()
        // \p{javaWhitespace}  Equivalent to java.lang.Character.isWhitespace()
        // \p{javaMirrored}    Equivalent to java.lang.Character.isMirrored()
        ss.addRule(ss.ANY_STATE, "\\\\p\\{(?:javaLowerCase|javaUpperCase|javaWhitespace|javaMirrored)}", CC_JAVA, null);

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

        // "Quotation"
        // Quotation
        // \   Nothing, but quotes the following character
        // \Q  Nothing, but quotes all characters until \E
        // \E  Nothing, but ends quoting started by \Q
        ss.addRule(ss.ANY_STATE,       "\\\\[^0-9A-Za-z]", QUOTED_CHARACTER,  null);
        ss.addRule("\\\\Q",                                QUOTATION_BEGIN,   ScannerState.IN_QUOTATION);
        ss.addRule(ScannerState.IN_QUOTATION, "\\\\E",            QUOTATION_END,     ScannerState.IN_QUOTATION);
        ss.addRule(ScannerState.IN_QUOTATION, ".",                LITERAL_CHARACTER, ScannerState.IN_QUOTATION);

        // "Special constructs (named-capturing and non-capturing)"
        // Special constructs (named-capturing and non-capturing)
        // (?<name>X)          X, as a named-capturing group
        // (?:X)               X, as a non-capturing group
        // (?idmsuxU-idmsuxU)  Nothing, but turns match flags i d m s u x U on - off
        // (?idmsux-idmsux:X)  X, as a non-capturing group with the given flags i d m s u x on - off
        // (?=X)               X, via zero-width positive lookahead
        // (?!X)               X, via zero-width negative lookahead
        // (?<=X)              X, via zero-width positive lookbehind
        // (?<!X)              X, via zero-width negative lookbehind
        // (?>X)               X, as an independent, non-capturing group
        ss.addRule("\\(\\?<\\w+>",                        NAMED_CAPTURING_GROUP);
        ss.addRule("\\(\\?:",                             NON_CAPTURING_GROUP);
        ss.addRule("\\(\\?[idmsuxU]*(?:-[idmsuxU]+)?\\)", MATCH_FLAGS);
        ss.addRule("\\(\\?[idmsux]*(?:-[idmsux]*)?:",     MATCH_FLAGS_NON_CAPTURING_GROUP);
        ss.addRule("\\(\\?=",                             POSITIVE_LOOKAHEAD);
        ss.addRule("\\(\\?!",                             NEGATIVE_LOOKAHEAD);
        ss.addRule("\\(\\?<=",                            POSITIVE_LOOKBEHIND);
        ss.addRule("\\(\\?<!",                            NEGATIVE_LOOKBEHIND);
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

    public static boolean
    isWordCharacter(char subject) {
        return (
            (subject >= '0' && subject <= '9')
            || (subject >= 'A' && subject <= 'Z')
            || (subject >= 'a' && subject <= 'z')
        );
    }

    /**
     * A sequence that begins with a linebreak.
     */
    static final Sequence
    linebreakSequence() {

        return new AbstractSequence() {

            @Override public boolean
            matches(MatcherImpl matcher) {

                if (matcher.atEnd()) return false;

                char c = matcher.peek();

                if (c == '\r') {
                    if (!matcher.atEnd() && matcher.peek() == '\n') matcher.read();
                    return this.successorMatches(matcher);
                }

                if (Pattern.LINE_BREAK_CHARACTERS.indexOf(c) != -1) {
                    matcher.read();
                    return this.successorMatches(matcher);
                }

                return false;
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

        @Override public boolean
        matches(MatcherImpl matcher) { return this.successorMatches(matcher); }

        @Override public String toString() { return ""; }
    }

    public static final Sequence
    TERMINAL = new Sequence() {

        @Override public boolean
        matches(MatcherImpl matcher) { return true; }

        @Override public void
        linkTo(Sequence successor) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean
        successorMatches(MatcherImpl matcher) { throw new UnsupportedOperationException(); }
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

        mi.offset = offset;
        mi.end    = End.END_OF_SUBJECT;

        return this.sequence.matches(mi);
    }

    /**
     * Parses a regular expression into a {@link Sequence}.
     */
    private static Sequence
    parse(final RegexScanner rs, final int flags) throws ParseException {

        return new AbstractParser<TokenType>(rs) {

            int currentFlags = flags;

            public Sequence
            parse() throws ParseException { return this.parseAlternatives(); }

            private Sequence
            parseAlternatives() throws ParseException {

                Sequence op1 = this.parseSequence();
                if (!this.peekRead("|")) return op1;

                List<Sequence> alternatives = new ArrayList<Sequence>();
                alternatives.add(op1);
                alternatives.add(this.parseSequence());
                while (this.peekRead(EITHER_OR) != null) alternatives.add(this.parseSequence());
                return Pattern.alternatives(alternatives.toArray(new Sequence[alternatives.size()]));
            }

            private Sequence
            parseSequence() throws ParseException {

                if (this.peek() == null || this.peekRead("|")) return Pattern.emptyStringSequence();

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
                for (int i = 0; i < elements.size() - 1; i++) elements.get(i).linkTo(elements.get(i + 1));

                return elements.get(0);
            }

            @Nullable private Sequence
            merge(Sequence s1, Sequence s2) {

                if (s1 instanceof CcLiteralCharacter && s2 instanceof CcLiteralCharacter) {
                    return Pattern.literalString(new String(new char[] {
                        ((CcLiteralCharacter) s1).c,
                        ((CcLiteralCharacter) s2).c,
                    }));
                }

                if (s1 instanceof LiteralString && s2 instanceof CcLiteralCharacter) {
                    return Pattern.literalString(((LiteralString) s1).s + ((CcLiteralCharacter) s2).c);
                }

                if (s1 instanceof CcLiteralCharacter && s2 instanceof LiteralString) {
                    return Pattern.literalString(((CcLiteralCharacter) s1).c + ((LiteralString) s2).s);
                }

                if (s1 instanceof LiteralString && s2 instanceof LiteralString) {
                    return Pattern.literalString(((LiteralString) s1).s + ((LiteralString) s2).s);
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

                    op.linkTo(Pattern.TERMINAL);

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
                        return new AbstractSequence() {

                            @Override public boolean
                            matches(MatcherImpl matcher) {

                                for (int i = 0; i < min; i++) {
                                    if (!op.matches(matcher)) return false;
                                }

                                int   longestMatchOffset = 0;
                                int[] longestMatchGroups = null;

                                final int limit = max - min;

                                for (int i = 0;; i++) {

                                    final int   savedOffset = matcher.offset;
                                    final int[] savedGroups = matcher.groups;
                                    {
                                        if (this.successorMatches(matcher)) {
                                            longestMatchOffset = matcher.offset;
                                            longestMatchGroups = matcher.groups;
                                        }
                                    }
                                    matcher.offset = savedOffset;
                                    matcher.groups = savedGroups;

                                    if (i >= limit) break;

                                    if (!op.matches(matcher)) break;
                                }

                                if (longestMatchGroups == null) return false;

                                matcher.offset = longestMatchOffset;
                                matcher.groups = longestMatchGroups;
                                return true;
                            }

                            @Override public String
                            toString() { return op + "{" + min + "," + max + "}"; }
                        };

                    case RELUCTANT_QUANTIFIER:
                        return new AbstractSequence() {

                            @Override public boolean
                            matches(MatcherImpl matcher) {

                                for (int i = 0; i < min; i++) {
                                    if (!op.matches(matcher)) return false;
                                }

                                final int limit = max - min;

                                for (int i = 0;; i++) {

                                    final int   savedOffset = matcher.offset;
                                    final int[] savedGroups = matcher.groups;
                                    {
                                        if (this.successorMatches(matcher)) return true;
                                    }
                                    matcher.offset = savedOffset;
                                    matcher.groups = savedGroups;

                                    if (i >= limit) break;

                                    if (!op.matches(matcher)) return false;
                                }

                                return false;
                            }

                            @Override public String
                            toString() { return op + "{" + min + "," + max + "}?"; }
                        };

                    case POSSESSIVE_QUANTIFIER:
                        return new AbstractSequence() {

                            @Override public boolean
                            matches(MatcherImpl matcher) {

                                for (int i = 0; i < min; i++) {
                                    if (!op.matches(matcher)) return false;
                                }

                                int limit = Math.min(max - min, matcher.remaining());

                                for (int i = 0; i < limit; i++) {

                                    final int   savedOffset = matcher.offset;
                                    final int[] savedGroups = matcher.groups;

                                    if (!op.matches(matcher)) {
                                        matcher.offset = savedOffset;
                                        matcher.groups = savedGroups;
                                        return this.successorMatches(matcher);
                                    }
                                }

                                return this.successorMatches(matcher);
                            }

                            @Override public String
                            toString() { return op + "{" + min + "," + max + "}+"; }
                        };

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
                        (this.currentFlags & Pattern.DOTALL)     != 0 ? Pattern.ccAnyChar() :
                        (this.currentFlags & Pattern.UNIX_LINES) != 0 ? Pattern.ccNegate(Pattern.ccLiteralCharacter('\n'), ".") : // SUPPRESS CHECKSTYLE LineLength
                        Pattern.ccNegate(Pattern.ccOneOf(Pattern.LINE_BREAK_CHARACTERS), ".")
                    );

                case QUOTED_CHARACTER:
                    return Pattern.ccLiteralCharacter(t.text.charAt(1));

                case QUOTATION_BEGIN:
                    {
                        Sequence result = this.parseSequence();
                        if (this.peek() != null) this.read(TokenType.QUOTATION_END);
                        return result;
                    }

                case CAPTURING_GROUP:
                    {
                        Sequence result = Pattern.capturingGroup(++rs.groupCount, this.parseAlternatives());
                        this.read(")");
                        return result;
                    }

                case NON_CAPTURING_GROUP:
                    {
                        final Sequence result = this.parseAlternatives();
                        this.read(")");
                        return result;
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
                        return Pattern.capturingGroupBackReference(groupNumber);
                    }

                case BEGINNING_OF_LINE:
                    return (
                        (flags & Pattern.MULTILINE) == 0
                        ? Pattern.beginningOfInput()
                        : Pattern.beginningOfLine(
                            (flags & Pattern.UNIX_LINES) != 0
                            ? Pattern.UNIX_LINE_BREAK_CHARACTERS
                            : Pattern.LINE_BREAK_CHARACTERS
                        )
                    );

                case END_OF_LINE:
                    return (
                        (flags & Pattern.MULTILINE) == 0
                        ? Pattern.endOfInput()
                        : Pattern.endOfLine(
                            (flags & Pattern.UNIX_LINES) != 0
                            ? Pattern.UNIX_LINE_BREAK_CHARACTERS
                            : Pattern.LINE_BREAK_CHARACTERS
                        )
                    );

                case WORD_BOUNDARY:
                    return Pattern.wordBoundary();

                case NON_WORD_BOUNDARY:
                    return Pattern.nonWordBoundary();

                case BEGINNING_OF_INPUT:
                    return Pattern.beginningOfInput();

                case END_OF_PREVIOUS_MATCH:
                    return Pattern.endOfPreviousMatch();

                case END_OF_INPUT_BUT_FINAL_TERMINATOR:
                    return Pattern.endOfInputButFinalTerminator();

                case END_OF_INPUT:
                    return Pattern.endOfInput();

                case MATCH_FLAGS:
                    this.currentFlags = this.parseFlags(this.currentFlags, t.text.substring(2, t.text.length() - 1));
                    return Pattern.emptyStringSequence();

                case LINEBREAK_MATCHER:
                    return Pattern.linebreakSequence();

                case NAMED_CAPTURING_GROUP:
                    {
                        int groupNumber = ++rs.groupCount;

                        String groupName = t.text.substring(3, t.text.length() - 1);
                        if (rs.namedGroups.put(groupName, groupNumber) != null) {
                            throw new ParseException("Duplicate captuting group name \"" + groupName + "\"");
                        }

                        Sequence result = Pattern.capturingGroup(groupNumber, this.parseAlternatives());
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
                        return Pattern.capturingGroupBackReference(groupNumber);
                    }

                case INDEPENDENT_NON_CAPTURING_GROUP:
                case NEGATIVE_LOOKAHEAD:
                case NEGATIVE_LOOKBEHIND:
                case POSITIVE_LOOKAHEAD:
                case POSITIVE_LOOKBEHIND:
                    throw new ParseException("\"" + t + "\" (" + t.type + ") is not yet implemented");

                case CC_NEGATION:
                case CC_RANGE:
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
                    char c = t.text.charAt(0);
                    return (
                        (this.currentFlags & Pattern.CASE_INSENSITIVE) == 0
                        ? Pattern.ccLiteralCharacter(c)
                        : (this.currentFlags & Pattern.UNICODE_CASE) == 0
                        ? Pattern.ccCaseInsensitiveLiteralCharacter(c)
                        : Pattern.ccUnicodeCaseInsensitiveLiteralCharacter(c)
                    );

                case LITERAL_CONTROL:
                    {
                        int idx = "ctnrfae".indexOf(t.text.charAt(1));
                        assert idx != -1;
                        if (idx == 0) return Pattern.ccLiteralCharacter((char) (t.text.charAt(2) & 0x1f));
                        return Pattern.ccLiteralCharacter("c\t\n\r\f\u0007\u001b".charAt(idx));
                    }

                case LITERAL_HEXADECIMAL:
                    return Pattern.ccLiteralCharacter((char) Integer.parseInt(
                        t.text.charAt(2) == '{'
                        ? t.text.substring(3, t.text.length() - 1)
                        : t.text.substring(2)
                    ));

                case LITERAL_OCTAL:
                    return Pattern.ccLiteralCharacter((char) Integer.parseInt(t.text.substring(2, 8)));

                case LEFT_BRACKET:
                    boolean        negate = this.peekRead("^");
                    CharacterClass op     = this.parseCcIntersection();
                    this.read("]");
                    return negate ? Pattern.ccNegate(op, '^' + op.toString()) : op;

                case CC_PREDEFINED:
                    switch (t.text.charAt(1)) {
                    case 'd': return Pattern.ccIsDigit();
                    case 'D': return Pattern.ccIsNonDigit();
                    case 'h': return Pattern.ccIsHorizontalWhitespace();
                    case 'H': return Pattern.ccIsNonHorizontalWhitespace();
                    case 's': return Pattern.ccIsWhitespace();
                    case 'S': return Pattern.ccIsNonWhitespace();
                    case 'v': return Pattern.ccIsVerticalWhitespace();
                    case 'V': return Pattern.ccIsNonVerticalWhitespace();
                    case 'w': return Pattern.ccIsWord();
                    case 'W': return Pattern.ccIsNonWord();
                    default:  throw new AssertionError(t);
                    }

                case CC_POSIX:
                    if ("\\p{Lower}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_LOWER);
                    if ("\\p{Upper}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_UPPER);
                    if ("\\p{ASCII}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_ASCII);
                    if ("\\p{Alpha}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_ALPHA);
                    if ("\\p{Digit}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_DIGIT);
                    if ("\\p{Alnum}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_ALNUM);
                    if ("\\p{Punct}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_PUNCT);
                    if ("\\p{Graph}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_GRAPH);
                    if ("\\p{Print}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_PRINT);
                    if ("\\p{Blank}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_BLANK);
                    if ("\\p{Cntrl}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_CNTRL);
                    if ("\\p{XDigit}".equals(t.text)) return Pattern.characterClass(Characters.IS_POSIX_XDIGIT);
                    if ("\\p{Space}".equals(t.text))  return Pattern.characterClass(Characters.IS_POSIX_SPACE);
                    throw new AssertionError(t.toString());

                case CC_JAVA:
                    if ("\\p{javaLowerCase}".equals(t.text))  return Pattern.characterClass(Characters.IS_LOWER_CASE);
                    if ("\\p{javaUpperCase}".equals(t.text))  return Pattern.characterClass(Characters.IS_UPPER_CASE);
                    if ("\\p{javaWhitespace}".equals(t.text)) return Pattern.characterClass(Characters.IS_WHITESPACE);
                    if ("\\p{javaMirrored}".equals(t.text))   return Pattern.characterClass(Characters.IS_MIRRORED);
                    throw new AssertionError(t.toString());

                case CC_UNICODE_SCRIPT_OR_BINARY_PROPERTY:
                    throw new AssertionError("Unicode scripts and binary properties are not implemented");

                case CC_UNICODE_BLOCK:
                    UnicodeBlock block = Character.UnicodeBlock.forName(t.text.substring(5, t.text.length() - 1));
                    return Pattern.ccInUnicodeBlock(block);

                case CC_UNICODE_CATEGORY:
                    String  gcName = t.text.substring(3, 5);
                    Byte    gc     = Pattern.getGeneralCategory(gcName);
                    if (gc == null) throw new ParseException("Unknown general cateogry \"" + gcName + "\"");
                    return Pattern.ccInUnicodeGeneralCategory(gc);

                default:
                    throw new ParseException("Character class expected instead of \"" + t + "\" (" + t.type + ")");
                }
            }

            private CharacterClass
            parseCcIntersection() throws ParseException {

                CharacterClass result = this.parseCcUnion();

                while (this.peekRead(CC_INTERSECTION) != null) {
                    result = Pattern.ccIntersection(result, this.parseCcUnion());
                }

                return result;
            }

            private CharacterClass
            parseCcUnion() throws ParseException {

                CharacterClass result = this.parseCcRange();

                while (this.peek(RIGHT_BRACKET, CC_INTERSECTION) == -1) {
                    result = Pattern.ccUnion(result, this.parseCcRange());
                }

                return result;
            }

            private CharacterClass
            parseCcRange() throws ParseException {
                String lhs = this.peekRead(LITERAL_CHARACTER);
                if (lhs == null) return this.parseCharacterClass();
                if (!this.peekRead("-")) return Pattern.ccLiteralCharacter(lhs.charAt(0));
                String rhs = this.read(LITERAL_CHARACTER);
                return Pattern.ccRange(lhs.charAt(0), rhs.charAt(0));
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
