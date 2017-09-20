
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.protocol.Mapping;
import de.unkrig.commons.lang.protocol.Mappings;
import de.unkrig.commons.lang.protocol.NoException;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.PredicateWhichThrows;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.EvaluationException;
import de.unkrig.commons.text.expression.Expression;
import de.unkrig.commons.text.expression.ExpressionEvaluator;
import de.unkrig.commons.text.parser.ParseException;

/**
 * {@code de.unkrig.lfr.core}'s implementation of {@link Matcher}.
 */
final
class MatcherImpl implements Matcher {

    // CONFIGURATION

    /**
     * @see #END_OF_REGION
     * @see #ANY
     */
    enum End {

        /**
         * The match is successful if the pattern matches the entire rest of the region.
         */
        END_OF_REGION,

        /**
         * The match is successful if the pattern matches the next characters of the subject.
         */
        ANY,
    }

    private Pattern pattern;
    private boolean hasTransparentBounds;
    private boolean hasAnchoringBounds = true;

    /**
     * The "subject" string, i.e. the string that is currently subject to pattern matching.
     */
    CharSequence subject;

    /**
     * The region within the {@link #subject} that is subject to pattern matching.
     */
    int regionStart, regionEnd;

    // STATE

    /**
     * The offsets of the captured groups within the {@link #subject}. The offset of the first character of the
     * <var>n</var>th group is 2*<var>n</var>; the offset <em>after</em> the <var>n</var>th group is 2*<var>n</var> +
     * 1. These offsets are {@code -1} if the group did not match anything.
     * <p>
     *   This field is modified bei {@link Sequence#matches(MatcherImpl)}, but the <em>elements</em> of this
     *   array are <em>never</em> modified! This makes it easy to "remember" (and later restore) the groups.
     * </p>
     */
    int[] groups;

    /**
     * The template to store in {@link #groups} when the matching begins.
     */
    int[] initialGroups;

    /**
     * The counters for the currently executing iterations.
     */
    final int[] counters;

    /**
     * Whether an attempt was made to peek at or behind the {@link #regionEnd}.
     */
    boolean hitEnd;

    /**
     * Whether an attempt was made to peek at or behind the {@link #transparentRegionEnd}.
     */
    boolean requireEnd;

    /**
     * Carries state information between invocations of {@link #find()} and {@link #find(int)}.
     * <dl>
     *   <dt>-1:</dt>
     *   <dd>None of {@link #find()} and {@link #find(int)} have been called yet</dd>
     *
     *   <dt>-2:</dt>
     *   <dd>The preceding invocation had returned {@code false}
     *
     *   <dt>Positive values:</dt>
     *   <dd>The index <em>behind</em> the preceding successful match</dd>
     * </dl>
     */
    int endOfPreviousMatch = -1;

    private int lastAppendPosition;

    /**
     * Whether the current matching must end at {@link End#END_OF_REGION} or {@link End#ANY}where.
     */
    @Nullable MatcherImpl.End end;

    /**
     * Designates the current matching position.
     *
     * @see Sequence#find(MatcherImpl)
     * @see Sequence#matches(MatcherImpl)
     */
    public int offset;

    MatcherImpl(Pattern pattern, CharSequence subject) {
        this.pattern   = pattern;
        this.subject   = subject;
        this.regionEnd = subject.length();

        this.groups = (this.initialGroups = new int[2 + 2 * pattern.groupCount]);
        Arrays.fill(this.groups, -1);

        this.counters = new int[pattern.quantifierNesting];
    }

    @Override public MatchResult
    toMatchResult() {

        if (this.endOfPreviousMatch < 0) throw new IllegalStateException("No match available");

        return new MatchResult() {

            CharSequence subject = MatcherImpl.this.subject;
            int[]        groups = MatcherImpl.this.groups.clone();

            @Override public int groupCount() { return this.groups.length / 2 - 1; }

            @Override public int              start() { return this.start(0); }
            @Override public int              end()   { return this.end(0);   }
            @Override @Nullable public String group() { return this.group(0); }

            @Override public int
            start(int groupNumber) { return this.groups[2 * groupNumber];     }

            @Override public int
            end(int groupNumber)   { return this.groups[2 * groupNumber + 1]; }

            @Override @Nullable public String
            group(int groupNumber) {
                return (
                    this.start(groupNumber) == -1
                    ? null
                    : this.subject.subSequence(this.start(groupNumber), this.end(groupNumber)).toString()
                );
            }
        };
    }

    @Override public Pattern
    pattern() { return this.pattern; }

    @Override public Matcher
    usePattern(de.unkrig.ref4j.Pattern newPattern) {
        this.pattern = (Pattern) newPattern;
        this.groups  = (this.initialGroups = new int[2 + 2 * this.pattern.groupCount]);
        Arrays.fill(this.groups, -1);
        return this;
    }

    @Override public Matcher
    reset() { return this.region(0, this.subject.length()); }

    @Override public Matcher
    reset(CharSequence input) {
        this.subject = input;
        return this.reset();
    }

    @Override public int
    start(int groupNumber) {

        if (this.endOfPreviousMatch < 0) throw new IllegalStateException("No match available");

        return this.groups[2 * groupNumber];
    }

    @Override public int
    start(@Nullable String groupName) {

        if (groupName == null) throw new NullPointerException();

        if (this.endOfPreviousMatch < 0) throw new IllegalStateException("No match available");

        Integer groupNumber = this.pattern.namedGroups.get(groupName);
        if (groupNumber == null) throw new IllegalArgumentException("Invalid group name \"" + groupName + "\"");

        return this.groups[2 * groupNumber];
    }

    @Override public int
    end(int groupNumber) {

        if (this.endOfPreviousMatch < 0) throw new IllegalStateException("No match available");

        return this.groups[2 * groupNumber + 1];
    }

    @Override public int
    end(@Nullable String groupName) {

        if (groupName == null) throw new NullPointerException();

        if (this.endOfPreviousMatch < 0) throw new IllegalStateException("No match available");

        Integer groupNumber = this.pattern.namedGroups.get(groupName);
        if (groupNumber == null) throw new IllegalArgumentException("Invalid group name \"" + groupName + "\"");

        return this.groups[2 * groupNumber + 1];
    }

    @Nullable @Override public String
    group(int groupNumber) {

        if (this.endOfPreviousMatch == -1) throw new IllegalStateException("No match available");
        if (this.endOfPreviousMatch == -2) return null;

        int[] gs = this.groups;

        // Ironically, JUR throws an IndexOutOfBoundsException, not an ArrayIndexOutOfBoundsException...
        if (2 * groupNumber >= gs.length) throw new IndexOutOfBoundsException(Integer.toString(groupNumber));

        int start = gs[2 * groupNumber];
        int end   = gs[2 * groupNumber + 1];

        return start == -1 ? null : this.subject.subSequence(start, end).toString();
    }

    @Override @Nullable public String
    group(@Nullable String groupName) {

        if (groupName == null) throw new NullPointerException();

        if (this.endOfPreviousMatch < 0) throw new IllegalStateException("No match available");

        Integer groupNumber = this.pattern.namedGroups.get(groupName);
        if (groupNumber == null) throw new IllegalArgumentException("Invalid group name \"" + groupName + "\"");

        int[] gs = this.groups;

        int start = gs[2 * groupNumber];
        int end   = gs[2 * groupNumber + 1];

        return start == -1 ? null : this.subject.subSequence(start, end).toString();
    }

    @Override public int              start()      { return this.start(0);             }
    @Override public int              end()        { return this.end(0);               }
    @Override @Nullable public String group()      { return this.group(0);             }
    @Override public int              groupCount() { return this.pattern().groupCount; }
    @Override public boolean          hitEnd()     { return this.hitEnd;               }
    @Override public boolean          requireEnd() { return this.requireEnd;           }

    @Override public boolean
    matches() {

        this.groups     = this.initialGroups;
        this.hitEnd     = false;
        this.requireEnd = false;
        this.end        = MatcherImpl.End.END_OF_REGION;
        this.offset     = this.regionStart;

        {
            int regionLength = this.regionEnd - this.regionStart;

            // Optimization: Test whether there are enough characters left so that the sequence can possibly match.
            if (this.pattern.sequence.minMatchLength() > regionLength) {
                this.endOfPreviousMatch = -1;
                this.hitEnd             = true;
                return false;
            }

            // Optimization: Test whether the sequence can possibly match all remaining chars.
            if (this.pattern.sequence.maxMatchLength() < regionLength) {
                this.endOfPreviousMatch = -1;
                return false;
            }
        }

        if (!this.pattern.sequence.matches(this)) {
            this.endOfPreviousMatch = -1;
            return false;
        }

        this.groups[0]          = this.regionStart;
        this.groups[1]          = this.offset;
        this.endOfPreviousMatch = this.offset;

        return true;
    }

    @Override public boolean
    lookingAt() {

        this.groups     = this.initialGroups;
        this.hitEnd     = false;
        this.requireEnd = false;
        this.offset     = this.regionStart;
        this.end        = MatcherImpl.End.ANY;

        // Optimization: Test whether there are enough chars lefts so that the sequence can possibly match.
        if (this.pattern.sequence.minMatchLength() > this.regionEnd - this.regionStart) {
            this.endOfPreviousMatch = -1;
            this.hitEnd             = true;
            return false;
        }

        if (!this.pattern.sequence.matches(this)) {
            this.endOfPreviousMatch = -1;
            return false;
        }

        this.groups[0]          = this.regionStart;
        this.groups[1]          = this.offset;
        this.endOfPreviousMatch = this.offset;
        return true;
    }

    @Override public boolean
    find() {
        return (
            this.endOfPreviousMatch != -2
            && this.find(this.endOfPreviousMatch == -1 ? this.regionStart : this.groups[1])
        );
    }

    @Override public boolean
    find(int start) {

        if (start < 0 || start > this.regionEnd) throw new IndexOutOfBoundsException(Integer.toString(start));

        this.hitEnd     = false;
        this.requireEnd = false;

        if (this.endOfPreviousMatch >= 0 && start == this.groups[0]) {

            // The previous match is a zero-length match. To prevent an endless series of these matches, advance
            // the start position by one.
            if (start >= this.regionEnd) {
                this.endOfPreviousMatch = -2;
                this.hitEnd             = true;
                return false;
            }
            start++;
        }

        this.offset = start;
        this.groups = this.initialGroups;
        this.end    = MatcherImpl.End.ANY;

        // Optimization: Test whether there are enough chars left so that the sequence can possibly match.
        if (this.pattern.sequence.minMatchLength() > this.regionEnd - start) {
            this.endOfPreviousMatch = -1;
            this.hitEnd             = true;
            return false;
        }

        if (!this.pattern.sequence.find(this)) {
            this.endOfPreviousMatch = -1;
            return false;
        }

        this.endOfPreviousMatch = this.groups[1];
        return true;
    }

    // REGION GETTERS

    @Override public int regionStart() { return this.regionStart; }
    @Override public int regionEnd()   { return this.regionEnd;   }

    // BOUNDS GETTERS

    @Override public boolean hasTransparentBounds() { return this.hasTransparentBounds; }
    @Override public boolean hasAnchoringBounds()   { return this.hasAnchoringBounds;   }

    // SEARCH/REPLACE LOGIC

    @Override public Matcher
    appendReplacement(Appendable appendable, String replacement) {
        return this.appendReplacement(appendable, this.compileReplacement(replacement));
    }

    @Override public Producer<String>
    compileReplacement(String replacement) {

        final Mapping<String, Object> variables = new Mapping<String, Object>() {

            @Override public boolean
            containsKey(@Nullable Object key) {
                String variableName = (String) key;
                return (
                    MatcherImpl.this.pattern.namedGroups.containsKey(variableName)
                    || "m".equals(variableName)
                );
            }

            @Override @Nullable public Object
            get(@Nullable Object key) {

                String variableName = (String) key;

                if ("m".equals(variableName)) return MatcherImpl.this;

                Integer groupNumber = MatcherImpl.this.pattern.namedGroups.get(variableName);
                assert groupNumber != null;
                return MatcherImpl.this.group(groupNumber);
            }
        };

        final PredicateWhichThrows<String, NoException>
        isValidVariableName = Mappings.<String, Object, NoException>containsKeyPredicate(variables);

        final List<Producer<?>> segments = new ArrayList<Producer<?>>();

        for (int cursor = 0; cursor < replacement.length();) {

            char c = replacement.charAt(cursor++);

            if (c == '$') {
                if (cursor == replacement.length()) {
                    throw new IllegalArgumentException("Illegal group reference: group index is missing");
                }

                if (replacement.charAt(cursor) == '{') {

                    // "${expr}".
                    final CharSequence spec = replacement.subSequence(++cursor, replacement.length());
                    final Expression   expression;

                    {
                        final int[] offset = new int[1];
                        try {
                            expression = new ExpressionEvaluator(isValidVariableName).parsePart(spec, offset);
                        } catch (ParseException pe) {
                            throw ExceptionUtil.wrap((
                                "Parsing expression \""
                                + spec
                                + "\" in replacement string"
                            ), pe, IllegalArgumentException.class);
                        }
                        cursor += offset[0];
                    }

                    if (cursor == replacement.length() || replacement.charAt(cursor) != '}') {
                        throw new IllegalArgumentException("expression is missing trailing '}'");
                    }
                    cursor++;

                    segments.add(new Producer<Object>() {

                        @Override @Nullable public Object
                        produce() {
                            try {
                                return expression.evaluate(variables);
                            } catch (EvaluationException ee) {
                                throw ExceptionUtil.wrap((
                                    "Evaluating expression \""
                                    + spec
                                    + "\" in replacement string"
                                ), ee, IllegalArgumentException.class);
                            }
                        }
                    });
                } else
                {

                    // "$1".
                    int groupNumber = Character.digit(replacement.charAt(cursor++), 10);
                    if (groupNumber == -1) throw new IllegalArgumentException("Illegal group reference");

                    for (; cursor < replacement.length(); cursor++) {

                        int nextDigit = Character.digit(replacement.charAt(cursor), 10);
                        if (nextDigit == -1) break;

                        int newGroupNumber = groupNumber * 10 + nextDigit;
                        if (newGroupNumber > this.groupCount()) break;
                        groupNumber = newGroupNumber;
                    }

                    final int finalGroupNumber = groupNumber;
                    segments.add(new Producer<Object>() {

                        @Override public Object
                        produce() {
                            String g = MatcherImpl.this.group(finalGroupNumber);
                            return g != null ? g : "";
                        }
                    });
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (;; cursor++) {
                    if (c == '\\') {
                        sb.append(replacement.charAt(cursor++));
                    } else {
                        sb.append(c);
                    }
                    if (cursor >= replacement.length()) break;
                    c = replacement.charAt(cursor);
                    if (c == '$') break;
                }

                segments.add(ProducerUtil.constantProducer(sb.toString()));
            }
        }

        return new Producer<String>() {

            @Override @Nullable public String
            produce() {

                switch (segments.size()) {

                case 0:
                    return "";

                case 1:
                    return String.valueOf(segments.get(0).produce());

                default:
                    StringBuilder result = new StringBuilder();
                    for (Producer<?> segment : segments) result.append(segment.produce());
                    return result.toString();
                }
            }
        };
    }

    @Override public Matcher
    appendReplacement(Appendable appendable, Producer<String> replacement) {

        if (this.endOfPreviousMatch < 0) throw new IllegalStateException("No match available");

        // Expand the replacement string.
        String s = replacement.produce();

        try {

            // Append text between the previous match and THIS match.
            appendable.append(this.subject.subSequence(this.lastAppendPosition, this.groups[0]));

            // Append the expanded replacement string.
            appendable.append(s);

            this.lastAppendPosition = this.groups[1];
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }

        return this;
    }

    @Override public <T extends Appendable> T
    appendTail(T appendable) {

        try {
            appendable.append(this.subject, this.lastAppendPosition, this.subject.length());
            return appendable;
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }
    }

    @Override public String
    replaceAll(String replacement) {
        return replaceAll(compileReplacement(replacement));
    }

    @Override public String
    replaceAll(Producer<String> replacement) {

        this.reset();

        if (!this.find()) return this.subject.toString();

        StringBuilder sb = new StringBuilder();
        do {
            this.appendReplacement(sb, replacement);
        } while (this.find());

        return this.appendTail(sb).toString();
    }

    @Override public String
    replaceFirst(String replacement) {
        return replaceFirst(compileReplacement(replacement));
    }

    @Override public String
    replaceFirst(Producer<String> replacement) {

        this.reset();

        if (!this.find()) return this.subject.toString();

        StringBuilder sb = new StringBuilder();
        return this.appendReplacement(sb, replacement).appendTail(sb).toString();
    }

    // REGION/BOUNDS SETTERS

    @Override public Matcher
    region(int start, int end) {

        if (start < 0)                   throw new IndexOutOfBoundsException();
        if (end < start)                 throw new IndexOutOfBoundsException();
        if (end > this.subject.length()) throw new IndexOutOfBoundsException();

        this.regionStart = start;
        this.regionEnd   = end;

        // For JUR, "region()" does NOT reset "hitEnd" and "requireEnd", as one might expect!
//        this.hitEnd             = false;
//        this.requireEnd         = false;

        this.endOfPreviousMatch = -1;
        this.lastAppendPosition = 0;
        this.offset             = 0;

        return this;
    }

    @Override public Matcher
    useTransparentBounds(boolean b) {
        this.hasTransparentBounds = b;
        return this;
    }

    @Override public Matcher
    useAnchoringBounds(boolean b) {
        this.hasAnchoringBounds = b;
        return this;
    }

    // =====================================

    /**
     * If the subject infix ranging from the <var>offset</var> to the region end starts with the <var>cs</var>,
     * then the offset is advanced and {@code true} is returned.
     */
    boolean
    peekRead(CharSequence cs) { return this.peekRead(cs, 0, cs.length()); }

    /**
     * If the subject infix ranging from the {@link #offset} to the region end starts with the sequence designated by
     * <var>cs</var>, <var>start</var> and <var>end</var>, then the {@link #offset} is advanced and {@code true} is
     * returned.
     */
    boolean
    peekRead(CharSequence cs, int start, int end) {

        int o = this.offset;

        for (int i = start; i < end; i++, o++) {

            if (o >= this.regionEnd) {

                // Not enough chars left.
                this.hitEnd = true;
                return false;
            }

            if (this.subject.charAt(o) != cs.charAt(i)) return false;
        }

        this.offset = o;
        return true;
    }

    /**
     * If the subject infix ranging from the <var>offset</var> to the region end starts with the <var>cs</var>,
     * then the {@link #offset} is advanced and {@code true} is returned.
     */
    boolean
    caseInsensitivePeekRead(CharSequence cs) { return this.caseInsensitivePeekRead(cs, 0, cs.length()); }

    /**
     * If the subject infix ranging from the <var>offset</var> to the region end starts with the sequence designated by
     * <var>cs</var>, <var>start</var> and <var>end</var>, then the {@link #offset} is advanced and {@code true} is
     * returned.
     */
    boolean
    caseInsensitivePeekRead(CharSequence cs, int start, int end) {

        int o = this.offset;

        if (o + cs.length() > this.regionEnd) {

            // Not enough chars left.
            this.hitEnd = true;
            return false;
        }

        for (int i = start; i < end; i++, o++) {
            char c1 = this.subject.charAt(o);
            char c2 = cs.charAt(i);
            if (!(
                c1 == c2
                || (c1 + 32 == c2 && c1 >= 'A' && c1 <= 'Z')
                || (c1 - 32 == c2 && c1 >= 'a' && c1 <= 'z')
            )) return false;
        }

        this.offset = o;
        return true;
    }

    /**
     * If the subject infix ranging from the <var>offset</var> to the region end starts with the <var>cs</var>,
     * then the {@link #offset} is advanced and {@code true} is returned.
     */
    boolean
    unicodeCaseInsensitivePeekRead(CharSequence cs) { return this.unicodeCaseInsensitivePeekRead(cs, 0, cs.length()); }

    /**
     * If the subject infix ranging from the {@link #offset} to the region end starts with the sequence designated by
     * <var>cs</var>, <var>start</var> and <var>end</var>, then the {@link #offset} is advanced and {@code true} is
     * returned.
     */
    boolean
    unicodeCaseInsensitivePeekRead(CharSequence cs, int start, int end) {

        int o = this.offset;

        if (o + cs.length() > this.regionEnd) {

            // Not enough chars left.
            this.hitEnd = true;
            return false;
        }

        for (int i = start; i < end;) {

            if (o >= this.regionEnd) return false;

            int  c1;
            {
                c1 = this.subject.charAt(o++);
                char ls;
                if (
                    Character.isHighSurrogate((char) c1)
                    && o < this.regionEnd
                    && Character.isLowSurrogate((ls = this.subject.charAt(o)))
                ) {
                    c1 = Character.toCodePoint((char) c1, ls);
                    o++;
                }
            }

            int c2;
            {
                c2 = cs.charAt(i++);
                char ls;
                if (
                    Character.isHighSurrogate((char) c2)
                    && i < end
                    && Character.isLowSurrogate((ls = cs.charAt(i)))
                ) {
                    c2 = Character.toCodePoint((char) c2, ls);
                    i++;
                }
            }

            if (!MatcherImpl.equalsIgnoreCase(c1, c2)) return false;
        }

        this.offset = o;
        return true;
    }

    private static boolean
    equalsIgnoreCase(int c1, int c2) {

        if (c1 == c2) return true;

        c1 = Character.toUpperCase(c1);
        c2 = Character.toUpperCase(c2);
        if (c1 == c2) return true;

        // Also have to do THIS, see source code of "String.regionMatches()".
        c1 = Character.toLowerCase(c1);
        c2 = Character.toLowerCase(c2);
        if (c1 == c2) return true;

        return false;
    }

    /**
     * @return Whether the <var>predicate</var> evaluates for the character at the given offset
     */
    public boolean
    peekRead(int offset, Predicate<Character> predicate) {

        if (offset >= this.regionEnd) {
            this.hitEnd = true;
            return false;
        }

        return predicate.evaluate(this.subject.charAt(offset));
    }

    @Override public String
    toString() {

        StringBuilder sb = (
            new StringBuilder()
            .append("[pattern=")
            .append(this.pattern())
            .append(" region=")
            .append(this.regionStart)
            .append(",")
            .append(this.regionEnd)
            .append(" subject=")
            .append(this.subject)
            .append(" endOfPreviousMatch=")
            .append(this.endOfPreviousMatch)
        );

        if (this.endOfPreviousMatch >= 0) {
            sb.append(" lastmatch=").append(this.subject.subSequence(this.groups[0], this.groups[1]));
        }

        return sb.append(']').toString();
    }

    int
    sequenceEndMatches(int offset) { return this.end == MatcherImpl.End.ANY || offset == this.regionEnd ? offset : -1; }
}
