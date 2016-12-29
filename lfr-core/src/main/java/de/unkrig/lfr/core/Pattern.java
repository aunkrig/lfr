
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
import static de.unkrig.lfr.core.Pattern.TokenType.CC_UNICODE;
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
import static de.unkrig.lfr.core.Pattern.TokenType.MATCH_FLAGS_CAPTURING_GROUP;
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
    /** @see java.util.regex.Pattern#UNIX_LINES */
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

    int            flags;
    final String   pattern;
    final Sequence sequence;
    final int      groupCount;

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
//         * @see java.util.regex.Matcher#start(String)
//         */
//        int start(String name);

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

//        /**
//         * @see java.util.regex.Matcher#end(String)
//         */
//        int end(String name);

        /**
         * @see java.util.regex.Matcher#group()
         */
        String group();

        /**
         * @see java.util.regex.Matcher#group(int)
         */
        String group(int group);

//        /**
//         * @see java.util.regex.Matcher#group(String)
//         */
//        String group(String name);

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
        end(int groupNumber) {

            if (!this.hadMatch) throw new IllegalStateException("No match available");

            return this.groups[2 * groupNumber + 1];
        }

        @Override public String
        group(int groupNumber) {

            if (!this.hadMatch) throw new IllegalStateException("No match available");

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
        find() { return this.find(this.offset); }

        @Override public boolean
        find(int start) {
            for (; this.offset <= this.subject.length(); this.offset++) {
                if (this.matches(this.offset, End.ANY)) return true;
            }
            return false;
        }

        // =====================================

        private boolean
        matches(int start, End end) {

            if (this.hadMatch && start == this.groups[1] && start == this.groups[0]) {
                if (start >= this.subject.length()) {
                    this.hadMatch = false;
                    return false;
                }
                start++;
            }

            this.offset   = start;
            this.groups   = this.initialGroups;
            this.end      = end;

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

            if (
                this.offset >= this.subject.length()
                || !predicate.evaluate(this.subject.charAt(this.offset))
            ) return false;

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
        remaining() { return this.subject.length() - this.offset; }

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
        CC_ANY,
        /** {@code \d \D \h \H \s \S \v \V \w \W} */
        CC_PREDEFINED,
        CC_POSIX,
        CC_JAVA,
        CC_UNICODE,

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

    enum ScannerState { CHAR_CLASS1, CHAR_CLASS2, CHAR_CLASS3, IN_QUOTATION } // SUPPRESS CHECKSTYLE JavadocVariable

    static
    class Match implements Cloneable {

        /**
         * The indices of all matched capturing group, as start-end pairs. "-1" indicates a missing group. The last
         * element indicates whether the groups array is shared between multiple {@link Match} instances.
         */
        private int[] groups;

        private final CharSequence subject;

        int offset;

        protected boolean hitEnd;

        Match(int groupCount, CharSequence subject, int offset) {
            this.subject = subject;
            this.offset  = offset;
            this.groups  = new int[groupCount * 2 + 3];
            Arrays.fill(this.groups, 0, groupCount * 2 + 2, -1);
        }

        Match(Match that) {
            this.groups  = that.groups;
            this.subject = that.subject;
            this.offset  = that.offset;
            this.hitEnd  = that.hitEnd;
            this.setGroupsShared(true);
        }

        char
        peek() { return this.subject.charAt(this.offset); }

        public int
        peek(int offset) { return this.subject.charAt(this.offset + offset); }

        char
        read() { return this.subject.charAt(this.offset++); }

        /**
         * Iff the next characters equal the <var>ref</var>, then it consumes these characters and returns {@code
         * true}.
         */
        boolean
        peekRead(CharSequence ref) {
            int len = ref.length();
            if (this.offset + len > this.subject.length()) {

                // Not enough chars left.
                this.hitEnd = true;
                return false;
            }
            if (!ref.equals(this.subject.subSequence(this.offset, this.offset + len))) return false;
            this.offset += len;
            return true;
        }

        /**
         * @return Whether at least <var>n</var> characters are remaining
         */
        public boolean
        available(int n) { return this.offset + n <= this.subject.length(); }

        public boolean
        atStart() { return this.offset == 0; }

        boolean
        atEnd() {
            if (this.offset >= this.subject.length()) {
                this.hitEnd = true;
                return true;
            }
            return false;
        }

        public int
        remaining() {
            this.hitEnd = true;
            return this.subject.length() - this.offset;
        }

        Match
        setFrom(Match that) {
            assert this.subject == that.subject;
            this.groups  = that.groups;
            this.offset  = that.offset;
            this.hitEnd  = that.hitEnd;
            this.setGroupsShared(true);
            return this;
        }

        /**
         * @param groupNumber 0...<var>groupCount</var>
         */
        public Match
        setGroupStart(int groupNumber) {
            if (this.isGroupsShared()) {
                this.groups = Arrays.copyOf(this.groups, this.groups.length);
                this.setGroupsShared(false);
            }
            this.groups[2 * groupNumber] = this.offset;
            return this;
        }

        /**
         * @param groupNumber 0...<var>groupCount</var>
         */
        public Match
        setGroupEnd(int groupNumber) {
            if (this.isGroupsShared()) {
                this.groups = Arrays.copyOf(this.groups, this.groups.length);
                this.setGroupsShared(false);
            }
            this.groups[2 * groupNumber + 1] = this.offset;
            return this;
        }

        @Nullable Match next() { return null; }

        Match
        setGroupsShared(boolean value) { this.groups[this.groups.length - 1] = value ? 1 : 0; return this; }

        boolean
        isGroupsShared() { return this.groups[this.groups.length - 1] != 0; }

        /**
         * @param groupNumber Group zero denotes the entire match
         * @return            The offset after the last character captured by the group, or -1 if the match was
         *                    successful but the group itself did not match anything
         */
        public int
        start(int groupNumber) { return this.groups[2 * groupNumber]; }

        /**
         * @param groupNumber Group zero denotes the entire match
         * @return            The index of the first character captured by the group, or -1 if the match was successful
         *                    but the group itself did not match anything
         */
        public int
        end(int groupNumber) { return this.groups[2 * groupNumber + 1]; }

        /**
         * @return {@code null} iff the match was successful but the group failed to match any part of the input
         *         sequence
         */
        @Nullable public String
        group(int groupNumber) {
            int start = this.start(groupNumber);
            int end   = this.groups[2 * groupNumber + 1];
            if (start == -1 || end == -1) return null;
            return this.subject.subSequence(start, end).toString();
        }
    }

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
    class CcSequence extends AbstractSequence implements Predicate<Character>, Cloneable {

        @Override public boolean
        matches(MatcherImpl matcher) { return matcher.peekRead(this) && this.successorMatches(matcher); }

        @Override public CcSequence
        clone() {
            try {
                return (CcSequence) super.clone();
            } catch (CloneNotSupportedException cnse) {
                throw new AssertionError(cnse);
            }
        }
    }

    /**
     * Representation of a negated sequence.
     */
    public static
    class Negation extends AbstractSequence {

        private final Sequence delegate;

        public Negation(Sequence delegate) {
            this.delegate = delegate;
            delegate.linkTo(Pattern.TERMINAL);
        }

        @Override public boolean
        matches(MatcherImpl matcher) { return !this.delegate.matches(matcher) && this.successorMatches(matcher); }

        @Override public String
        toString() { return "!(" + this.delegate + ")"; }
    }

    /**
     * Representation of a negated character class, e.g. {@code ^\d}.
     */
    public static
    class CcNegation extends CcSequence {

        private final Predicate<Character> delegate;

        public CcNegation(Predicate<Character> delegate) { this.delegate = delegate; }

        @Override public boolean evaluate(Character subject) { return !this.delegate.evaluate(subject); }

        @Override public String
        toString() { return "^" + this.delegate; }
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

    /**
     * Representation of the "|" operator.
     */
    public static
    class Alternatives extends AbstractSequence {

        private final Sequence[] alternatives;

        public
        Alternatives(List<Sequence> alternatives) {
            this.alternatives = alternatives.toArray(new Sequence[alternatives.size()]);
        }

        @Override public void
        linkTo(Sequence successor) {
            super.linkTo(successor);
            for (Sequence s : this.alternatives) s.linkTo(successor);
        }

        @Override public boolean
        matches(MatcherImpl matcher) {

            if (this.alternatives.length == 0) return this.successorMatches(matcher);

            if (this.alternatives.length == 1) return this.alternatives[0].matches(matcher);

            final int   savedOffset = matcher.offset;
            final int[] savedGroups = matcher.groups;

            if (this.alternatives[0].matches(matcher)) return true;

            for (int i = 1; i < this.alternatives.length; i++) {

                matcher.offset = savedOffset;
                matcher.groups = savedGroups;

                if (this.alternatives[i].matches(matcher)) return true;
            }

            return false;
        }

        @Override public String
        toString() { return Pattern.join(this.alternatives, '|'); }
    }

    /**
     * Representation of a capturing group, e.g. {@code (abc)}.
     */
    public static
    class CapturingGroup extends AbstractSequence {

        private final int      groupNumber;
        private final Sequence subsequence;

        /**
         * @param groupNumber 1...<var>groupCount</var>
         */
        public
        CapturingGroup(int groupNumber, Sequence subsequence) {

            subsequence.linkTo(Pattern.TERMINAL);

            this.groupNumber = groupNumber;
            this.subsequence = subsequence;
        }

        @Override public boolean
        matches(MatcherImpl matcher) {

            final int savedOffset = matcher.offset;

            if (!this.subsequence.matches(matcher)) return false;

            // Copy "this.groups" and store group start and end.
            int[] gs = (matcher.groups = Arrays.copyOf(matcher.groups, matcher.groups.length));
            gs[2 * this.groupNumber]     = savedOffset;
            gs[2 * this.groupNumber + 1] = matcher.offset;

            return this.successorMatches(matcher);
        }

        @Override public String
        toString() { return "(" + this.subsequence + ")"; }
    }

    /**
     * Representation of a capturing group backreference, e.g. {@code \3} .
     */
    public static
    class CapturingGroupBackReference extends AbstractSequence {

        private final int groupNumber;

        /**
         * @param groupNumber 0...<var>groupCount</var>
         */
        public
        CapturingGroupBackReference(int groupNumber) { this.groupNumber = groupNumber; }

        @Override public boolean
        matches(MatcherImpl matcher) {

            int[]        gs    = matcher.groups;
            CharSequence group = matcher.subject.subSequence(
                gs[2 * this.groupNumber],
                gs[2 * this.groupNumber + 1]
            );

            return matcher.peekRead(group) && this.successorMatches(matcher);
        }

        @Override public String
        toString() { return "\\" + this.groupNumber; }
    }

    public static
    class BeginningOfInputMatcher extends AbstractSequence {

        @Override public boolean
        matches(MatcherImpl matcher) { return matcher.atStart() && this.successorMatches(matcher); }

        @Override public String
        toString() { return "^"; }
    }

    public static
    class BeginningOfLineMatcher extends AbstractSequence {

        @Override public boolean
        matches(MatcherImpl matcher) {
            return (
                matcher.atStart()
                || "\r\n\u000B\f\u0085\u2028\u2029".indexOf(matcher.peek(-1)) != -1
            ) && this.successorMatches(matcher);
        }

        @Override public String
        toString() { return "^"; }
    }

    public static
    class EndOfInputButFinalTerminatorMatcher extends AbstractSequence {

        @Override public boolean
        matches(MatcherImpl matcher) {
            return (
                matcher.atEnd()
                || (matcher.remaining() == 1 && "\r\n\u000B\f\u0085\u2028\u2029".indexOf(matcher.peek()) != -1)
                || (matcher.remaining() == 2 && matcher.peek() == '\r' && matcher.peek(1) == '\n')
            ) && this.successorMatches(matcher); }

        @Override public String
        toString() { return "^"; }
    }

    public static
    class EndOfInputMatcher extends AbstractSequence {

        @Override public boolean
        matches(MatcherImpl matcher) { return matcher.atEnd() && this.successorMatches(matcher); }

        @Override public String
        toString() { return "^"; }
    }

    public static
    class EndOfLineMatcher extends AbstractSequence {

        @Override public boolean
        matches(MatcherImpl matcher) {
            return (
                matcher.atEnd()
                || "\r\n\u000B\f\u0085\u2028\u2029".indexOf(matcher.peek()) != -1
            ) && this.successorMatches(matcher); }

        @Override public String
        toString() { return "^"; }
    }

    public static
    class WordBoundaryMatcher extends AbstractSequence {

        @Override public boolean
        matches(MatcherImpl matcher) {
            return (
                matcher.atStart()
                || matcher.atEnd()
                || (Pattern.isWordCharacter(matcher.peek(-1)) ^ Pattern.isWordCharacter(matcher.peek()))
            ) && this.successorMatches(matcher);
        }

        @Override public String
        toString() { return "^"; }
    }

    public static
    class EndOfPreviousMatchMatcher extends AbstractSequence {

        @Override public boolean
        matches(MatcherImpl matcher) {

            if (!matcher.hadMatch) throw new IllegalStateException("No match available");

            return matcher.offset == matcher.groups[1] && this.successorMatches(matcher);
        }

        @Override public String
        toString() { return "^"; }
    }

    /**
     * Representation of literal characters like "a" or "\.".
     */
    public static
    class CcLiteralCharacter extends CcSequence {

        private char c;

        public
        CcLiteralCharacter(char c) { this.c = c; }

        @Override public boolean
        evaluate(Character subject) { return subject == this.c; }

        @Override public String
        toString() { return new String(new char[] { this.c }); }
    }

    /**
     * Representation of an (ASCII-)case-insensitive literal character.
     */
    public static
    class CaseInsensitiveLiteralCharacter extends CcSequence {

        private char c;

        public
        CaseInsensitiveLiteralCharacter(char c) { this.c = c; }

        @Override public boolean
        evaluate(Character subject) {
            char c1 = this.c, c2 = subject;
            if (c1 == c2) return true;
            int diff = c1 - c2;
            if (diff == 32  && c1 >= 'a' && c1 <= 'z') return true;
            if (diff == -32 && c1 >= 'A' && c1 <= 'Z') return true;
            return false;
        }

        @Override public String
        toString() { return "(?i)" + this.c + "(?-i)"; }
    }

    /**
     * Representation of a (UNICODE-)case-insensitive literal character.
     */
    public static
    class UnicodeCaseInsensitiveLiteralCharacter extends CcSequence {

        private char c;

        public
        UnicodeCaseInsensitiveLiteralCharacter(char c) { this.c = c; }

        @Override public boolean
        evaluate(Character subject) { return Character.toUpperCase(this.c) == Character.toUpperCase(subject); }

        @Override public String
        toString() { return "(?iu)" + this.c + "(?-iu)"; }
    }

    /**
     * Representation of a character class intersection like {@code "\w&&[^abc]"}.
     */
    public static
    class CcIntersection extends CcSequence {

        private Predicate<Character> lhs, rhs;

        public CcIntersection(Predicate<Character> lhs, Predicate<Character> rhs) { this.lhs = lhs; this.rhs = rhs; }

        @Override public boolean
        evaluate(Character subject) { return this.lhs.evaluate(subject) && this.rhs.evaluate(subject); }

        @Override public String
        toString() { return this.lhs + "&&" + this.rhs; }
    }

    /**
     * Representation of a character class union like {@code "ab"}.
     */
    public static
    class CcUnion extends CcSequence {

        private Predicate<Character> lhs, rhs;

        public CcUnion(Predicate<Character> lhs, Predicate<Character> rhs) { this.lhs = lhs; this.rhs = rhs; }

        @Override public boolean
        evaluate(Character subject) { return this.lhs.evaluate(subject) || this.rhs.evaluate(subject); }

        @Override public String
        toString() { return this.lhs.toString() + this.rhs; }
    }

    /**
     * Representation of a character class range like {@code "a-k"}.
     */
    public static
    class CcRange extends CcSequence {

        private char lhs, rhs;

        public CcRange(char lhs, char rhs) { this.lhs = lhs; this.rhs = rhs; }

        @Override public boolean
        evaluate(Character subject) { return subject >= this.lhs && subject <= this.rhs; }

        @Override public String
        toString() { return this.lhs + "-" + this.rhs; }
    }

    Pattern(String pattern, int flags, Sequence sequence, int groupCount) {

        this.flags   = flags;
        this.pattern = pattern;

        sequence.linkTo(new Sequence() {

            @Override public boolean
            successorMatches(MatcherImpl matcher) { throw new UnsupportedOperationException(); }

            @Override public boolean matches(MatcherImpl matcher) { return matcher.sequenceEndMatches();       }
            @Override public void    linkTo(Sequence successor)   { throw new UnsupportedOperationException(); }

            @Override public String toString() { return "[END]"; }
        });

        this.sequence   = sequence;
        this.groupCount = groupCount;
    }

    /**
     * @param alternatives
     * @param glue
     * @return
     */
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

        int groupCount;

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
        // \p{InGreek}        A character in the Greek block (block)
        // \p{Lu}             An uppercase letter (category)
        // \p{IsAlphabetic}   An alphabetic character (binary property)
        // \p{Sc}             A currency symbol
        // \P{InGreek}        Any character except one in the Greek block (negation)
        // [\p{L}&&[^\p{Lu}]] Any letter except an uppercase letter (subtraction)
        ss.addRule(ss.ANY_STATE, "\\\\p\\{(?:IsLatin|InGreek|Lu|IsAlphabetic|Sc|InGreek)}", CC_UNICODE, null);

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
        ss.addRule("\\\\Q",                                QUOTATION_BEGIN,   ScannerState.IN_QUOTATION);
        ss.addRule(ScannerState.IN_QUOTATION, "\\\\E",            QUOTATION_END,     ScannerState.IN_QUOTATION);
        ss.addRule(ScannerState.IN_QUOTATION, ".",                LITERAL_CHARACTER, ScannerState.IN_QUOTATION);

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
        ss.addRule("\\(\\?<\\w+>",                        NAMED_CAPTURING_GROUP);
        ss.addRule("\\(\\?:",                             NON_CAPTURING_GROUP);
        ss.addRule("\\(\\?[idmsuxU]*(?:-[idmsuxU]+)?\\)", MATCH_FLAGS);
        ss.addRule("\\(\\?[idmsux]*(?:-[idmsux]*)?:",     MATCH_FLAGS_CAPTURING_GROUP);
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

    // ============================== CC SEQUENCES FOR "PREDEFINED CHARACTER CLASSES" ==============================

    /**  A digit: [0-9] */
    static final CcSequence
    CC_SEQUENCE_IS_DIGIT = new CcSequence() {
        @Override public boolean evaluate(Character subject) { return Character.isDigit(subject); }
        @Override public String  toString()                  { return "\\d"; }

    };

    /**  A non-digit: [^0-9] */
    static final CcSequence
    CC_SEQUENCE_IS_NON_DIGIT = new CcNegation(Pattern.CC_SEQUENCE_IS_DIGIT);

    /**  A horizontal whitespace character: <code>[ \t\xA0&#92;u1680&#92;u180e&#92;u2000-&#92;u200a&#92;u202f&#92;u205f&#92;u3000]</code> */ // SUPPRESS CHECKSTYLE LineLength
    static final CcSequence
    CC_SEQUENCE_IS_HORIZONTAL_WHITESPACE = new CcSequence() {
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

    /**  A non-horizontal whitespace character: [^\h] */
    static final CcSequence
    CC_SEQUENCE_IS_NON_HORIZONTAL_WHITESPACE  = new CcNegation(Pattern.CC_SEQUENCE_IS_HORIZONTAL_WHITESPACE);

    /**  A whitespace character: [ \t\n\x0B\f\r] */
    static final CcSequence
    CC_SEQUENCE_IS_WHITESPACE = new CcSequence() {
        @Override public boolean evaluate(Character subject) { return " \t\n\u000B\f\r".indexOf(subject) != -1; }
        @Override public String  toString()                  { return "\\s"; }

    };

    /**  A non-whitespace character: [^\s] */
    static final CcSequence
    CC_SEQUENCE_IS_NON_WHITESPACE = new CcNegation(Pattern.CC_SEQUENCE_IS_WHITESPACE);

    /**  A vertical whitespace character: [\n\x0B\f\r\x85/u2028/u2029] */
    static final CcSequence
    CC_SEQUENCE_IS_VERTICAL_WHITESPACE = new CcSequence() {

        @Override public boolean
        evaluate(Character subject) { return "\n\u000B\f\r\u0085\u2028\u2029".indexOf(subject) != -1; }

        @Override public String
        toString() { return "\\v"; }
    };

    /**  A non-vertical whitespace character: [^\v] */
    static final CcSequence
    CC_SEQUENCE_IS_NON_VERTICAL_WHITESPACE = new CcNegation(Pattern.CC_SEQUENCE_IS_VERTICAL_WHITESPACE);

    /**  A word character: [a-zA-Z_0-9] */
    static final CcSequence
    CC_SEQUENCE_IS_WORD = new CcSequence() {

        @Override public boolean
        evaluate(Character subject) { return Pattern.isWordCharacter(subject); }

        @Override public String
        toString() { return "\\w"; }
    };

    public static boolean
    isWordCharacter(char subject) {
        return (
            (subject >= '0' && subject <= '9')
            || (subject >= 'A' && subject <= 'Z')
            || (subject >= 'a' && subject <= 'z')
        );
    }

    /**  A non-word character: [^\w] */
    static final CcSequence
    CC_SEQUENCE_IS_NON_WORD = new CcNegation(Pattern.CC_SEQUENCE_IS_WORD);

    static final CcSequence
    CC_SEQUENCE_ANY_CHAR = new CcSequence() {

        @Override public boolean
        evaluate(Character subject) { return true; }

        @Override public String
        toString() { return "."; }
    };

    /**
     * A sequence that begins with a linebreak.
     */
    static final
    class CcLinebreakSequence extends CcSequence {

        @Override public boolean
        evaluate(Character subject) {
            return (
                subject == '\r'
                || subject == '\n'
                || subject == 0x0b
                || subject == 0x0c
                || subject == 0x85
                || subject == 0x2028
                || subject == 0x2029
            );
        }

        @Override public String
        toString() { return "\\R"; }
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

        return new Pattern(regex, flags, sequence, rs.groupCount);
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
                while (this.peekRead("|")) alternatives.add(this.parseSequence());
                return new Alternatives(alternatives);
            }

            private Sequence
            parseSequence() throws ParseException {

                if (this.peek() == null || this.peekRead("|")) return new EmptyStringSequence();

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

                    LiteralString merged = this.merge(elements.get(i), elements.get(i + 1));
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

            @Nullable private LiteralString
            merge(Sequence s1, Sequence s2) {

                if (s1 instanceof CcLiteralCharacter && s2 instanceof CcLiteralCharacter) {
                    return new LiteralString(new String(new char[] {
                        ((CcLiteralCharacter) s1).c,
                        ((CcLiteralCharacter) s2).c,
                    }));
                }

                if (s1 instanceof LiteralString && s2 instanceof CcLiteralCharacter) {
                    return new LiteralString(((LiteralString) s1).s + ((CcLiteralCharacter) s2).c);
                }

                if (s1 instanceof CcLiteralCharacter && s2 instanceof LiteralString) {
                    return new LiteralString(((CcLiteralCharacter) s1).c + ((LiteralString) s2).s);
                }

                if (s1 instanceof LiteralString && s2 instanceof LiteralString) {
                    return new LiteralString(((LiteralString) s1).s + ((LiteralString) s2).s);
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

                                int limit = Math.min(max - min, matcher.remaining());

                                int   longestMatchOffset = 0;
                                int[] longestMatchGroups = null;

                                for (int i = 0; i <= limit; i++) {

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

                                int limit = Math.min(max - min, matcher.remaining());

                                for (int i = 0; i < limit; i++) {

                                    final int   savedOffset = matcher.offset;
                                    final int[] savedGroups = matcher.groups;
                                    {
                                        if (this.successorMatches(matcher)) return true;
                                    }
                                    matcher.offset = savedOffset;
                                    matcher.groups = savedGroups;

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
                Sequence result = this.parseOptionalPrimary();
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

            @Nullable private Sequence
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
                    return (
                        (this.currentFlags & Pattern.DOTALL) != 0
                        ? Pattern.CC_SEQUENCE_ANY_CHAR
                        : new CcNegation(new CcLinebreakSequence())
                    );

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
                        Sequence result = this.parseSequence();
                        if (this.peek() != null) this.read(TokenType.QUOTATION_END);
                        return result;
                    }

                case CAPTURING_GROUP:
                    {
                        this.read();
                        Sequence result = new CapturingGroup(++rs.groupCount, this.parseAlternatives());
                        this.read(")");
                        return result;
                    }

                case CAPTURING_GROUP_BACK_REFERENCE:
                    this.read();
                    int groupNumber = Integer.parseInt(t.text.substring(1));
                    if (groupNumber > rs.groupCount) {
                        throw new ParseException("Group number " + groupNumber + " too big");
                    }
                    return new CapturingGroupBackReference(groupNumber);

                case BEGINNING_OF_LINE:
                    this.read();
                    return (
                        (flags & Pattern.MULTILINE) == 0
                        ? new BeginningOfInputMatcher()
                        : new BeginningOfLineMatcher()
                    );

                case END_OF_LINE:
                    this.read();
                    return (
                        (flags & Pattern.MULTILINE) == 0
                        ? new EndOfInputMatcher()
                        : new EndOfLineMatcher()
                    );

                case WORD_BOUNDARY:
                    return new WordBoundaryMatcher();

                case NON_WORD_BOUNDARY:
                    return new Negation(new WordBoundaryMatcher());

                case BEGINNING_OF_INPUT:
                    this.read();
                    return new BeginningOfInputMatcher();

                case END_OF_PREVIOUS_MATCH:
                    this.read();
                    return new EndOfPreviousMatchMatcher();

                case END_OF_INPUT_BUT_FINAL_TERMINATOR:
                    this.read();
                    return new EndOfInputButFinalTerminatorMatcher();

                case END_OF_INPUT:
                    this.read();
                    return new EndOfInputMatcher();

                case MATCH_FLAGS:
                    this.read();
                    this.currentFlags = this.parseFlags(this.currentFlags, t.text.substring(2, t.text.length() - 1));
                    return new EmptyStringSequence();

                case LINEBREAK_MATCHER:
                    this.read();
                    return new CcLinebreakSequence();

                case CC_INTERSECTION:
                case CC_JAVA:
                case CC_POSIX:
                case CC_UNICODE:
                case INDEPENDENT_NON_CAPTURING_GROUP:
                case MATCH_FLAGS_CAPTURING_GROUP:
                case NAMED_CAPTURING_GROUP:
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

            CcSequence
            parseCharacterClass() throws ParseException {

                Token<TokenType> t = this.read();

                switch (t.type) {

                case LITERAL_CHARACTER:
                    char c = t.text.charAt(0);
                    return (
                        (this.currentFlags & Pattern.CASE_INSENSITIVE) == 0 ? new CcLiteralCharacter(c)              :
                        (this.currentFlags & Pattern.UNICODE_CASE)     == 0 ? new CaseInsensitiveLiteralCharacter(c) :
                        new UnicodeCaseInsensitiveLiteralCharacter(c)
                    );

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
                    boolean    negate = this.peekRead("^");
                    CcSequence op     = this.parseCcIntersection();
                    this.read("]");
                    return negate ? new CcNegation(op) : op;

                case CC_PREDEFINED:
                    switch (t.text.charAt(1)) {
                    case 'd': return Pattern.CC_SEQUENCE_IS_DIGIT.clone();
                    case 'D': return Pattern.CC_SEQUENCE_IS_NON_DIGIT.clone();
                    case 'h': return Pattern.CC_SEQUENCE_IS_HORIZONTAL_WHITESPACE.clone();
                    case 'H': return Pattern.CC_SEQUENCE_IS_NON_HORIZONTAL_WHITESPACE.clone();
                    case 's': return Pattern.CC_SEQUENCE_IS_WHITESPACE.clone();
                    case 'S': return Pattern.CC_SEQUENCE_IS_NON_WHITESPACE.clone();
                    case 'v': return Pattern.CC_SEQUENCE_IS_VERTICAL_WHITESPACE.clone();
                    case 'V': return Pattern.CC_SEQUENCE_IS_NON_VERTICAL_WHITESPACE.clone();
                    case 'w': return Pattern.CC_SEQUENCE_IS_WORD.clone();
                    case 'W': return Pattern.CC_SEQUENCE_IS_NON_WORD.clone();
                    default:  throw new AssertionError(t);
                    }

                default:
                    throw new ParseException("Character class expected instead of \"" + t + "\" (" + t.type + ")");
                }
            }

            private CcSequence
            parseCcIntersection() throws ParseException {
                CcSequence result = this.parseCcUnion();
                while (this.peekRead("&&")) result = new CcIntersection(result, this.parseCcUnion());
                return result;
            }

            private CcSequence
            parseCcUnion() throws ParseException {
                CcSequence result = this.parseCcRange();
                while (this.peek("]", "&&") == -1) result = new CcUnion(result, this.parseCcRange());
                return result;
            }

            private CcSequence
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
