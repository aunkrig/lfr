
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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    private Pattern pattern; // Cannot be FINAL because of "usePattern()".

    boolean         hasTransparentBounds;
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
     * The offsets of the captured groups within the {@link #subject}. The "start" offset of the <var>n</var>th group
     * is {@code 2*}<var>n</var>; the end offset of the <var>n</var>th group is {@code 2*}<var>n</var> {@code + 1}.
     * These offsets are {@code -1} if the group did not match anything.
     * <p>
     *   This field is modified bei {@link MatcherImpl#usePattern(de.unkrig.ref4j.Pattern)}, so it cannot be FINAL.
     * </p>
     */
    int[] groups;

    /**
     * The counters for the currently executing iterations.
     * <p>
     *   This field cannot be FINAL only because of {@link #usePattern(de.unkrig.ref4j.Pattern)}.
     * </p>
     */
    int[] counters;

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

    /**
     * State information for {@link #appendReplacement(Appendable, String)} and {@link #appendTail(Appendable)}.
     */
    private int lastAppendPosition;

    /**
     * Whether the current matching must end at the {@link End#END_OF_REGION}, or may end {@link End#ANY}where.
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

        this.groups = new int[2 + 2 * pattern.groupCount];
        Arrays.fill(this.groups, -1);

        this.counters = new int[pattern.capturingQuantifierCount];
    }

    @Override public MatchResult
    toMatchResult() {

        // It is not clearly documented, but the MR is available also in the non-match case, and has weird properties...
        if (this.groups[0] < 0) {
            return new MatchResult() {

                RuntimeException ise() { return new IllegalStateException("No match found"); }

                @Override public int              groupCount()           { return MatcherImpl.this.groups.length / 2 - 1; }
                @Override public int              start()                { throw this.ise(); }
                @Override public int              end()                  { throw this.ise(); }
                @Override @Nullable public String group()                { throw this.ise(); }
                @Override public int              start(int groupNumber) { throw this.ise(); }
                @Override public int              end(int groupNumber)   { throw this.ise(); }
                @Override @Nullable public String group(int groupNumber) { throw this.ise(); }
            };
        }

        return new MatchResult() {

            final CharSequence subject = MatcherImpl.this.subject;
            final int[]        groups  = MatcherImpl.this.groups.clone();

            @Override public int              groupCount()           { return this.groups.length / 2 - 1;       }
            @Override public int              start()                { return this.start(0);                    }
            @Override public int              end()                  { return this.end(0);                      }
            @Override @Nullable public String group()                { return this.group(0);                    }
            @Override public int              start(int groupNumber) { return this.groups[2 * groupNumber];     }
            @Override public int              end(int groupNumber)   { return this.groups[2 * groupNumber + 1]; }

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

        this.pattern  = (Pattern) newPattern;
        this.counters = new int[this.pattern.capturingQuantifierCount];
        this.groups   = new int[2 + 2 * this.pattern.groupCount];
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
        if (start == -1) return null;

        int end = gs[2 * groupNumber + 1];
        assert end >= 0 : this.toString();
        return this.subject.subSequence(start, end).toString();
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

        Arrays.fill(this.groups, -1);
        this.hitEnd     = false;
        this.requireEnd = false;
        this.end        = MatcherImpl.End.END_OF_REGION;
        this.offset     = this.regionStart;

        {
            int regionLength = this.regionEnd - this.regionStart;

            // Optimization: Test whether there are enough characters left so that the sequence can possibly match.
            if (this.pattern.sequence.minMatchLength > regionLength) {
                this.endOfPreviousMatch = -1;
                this.hitEnd             = true;
                return false;
            }

            // Optimization: Test whether the sequence can possibly match all remaining chars.
            if (this.pattern.sequence.maxMatchLength < regionLength) {
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

        Arrays.fill(this.groups, -1);
        this.hitEnd     = false;
        this.requireEnd = false;
        this.end        = MatcherImpl.End.ANY;
        this.offset     = this.regionStart;

        // Optimization: Test whether there are enough chars lefts so that the sequence can possibly match.
        if (this.pattern.sequence.minMatchLength > this.regionEnd - this.regionStart) {
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

    @Override public String
    quoteReplacement(String s) { return java.util.regex.Matcher.quoteReplacement(s); }

    @Override public boolean
    find() {

        if (this.endOfPreviousMatch == -2) {

            // The preceding invocation had returned FALSE.
            return false;
        }

        if (this.endOfPreviousMatch == -1) {

            // First invocation (since the last reset); start at the region start.
            return this.find(this.regionStart);
        }

        int start = this.groups[1];

        if (start == this.groups[0]) {

            // The previous match is a zero-length match. To prevent an endless series of these matches, advance
            // the start position by one.
            if (start >= this.regionEnd) {
                this.endOfPreviousMatch = -2;

                // Actually, we should set "hitEnd", but for compatibility with JRE we don't.
                //this.hitEnd = true;

                return false;
            }
            start++;
        }

        if (!this.find(start)) {
            this.endOfPreviousMatch = -2;
            return false;
        }

        return true;
    }

    @Override public boolean
    find(int start) {

        if (start < 0 || start > this.regionEnd) throw new IndexOutOfBoundsException(Integer.toString(start));

        this.hitEnd     = false;
        this.requireEnd = false;

        Arrays.fill(this.groups, -1);
        this.offset = start;
        this.end    = MatcherImpl.End.ANY;

        int matchStart = this.pattern.sequence.find(this);

        if (matchStart < 0) {
            this.endOfPreviousMatch = -1;
            return false;
        }

        this.groups[0] = matchStart;
        this.groups[1] = (this.endOfPreviousMatch = this.offset);

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
        this.compileReplacement(replacement).appendReplacement(appendable);
        return this;
    }

    @Override public CompiledReplacement
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
        isValidVariableName = Mappings.<String, Object>containsKeyPredicate(variables);

        final List<Producer<String>> segments = new ArrayList<Producer<String>>();

        for (int cursor = 0; cursor < replacement.length();) {

            char c = replacement.charAt(cursor);

            if (c == '$') {
                cursor++;

                if (cursor == replacement.length()) {
                    throw new IllegalArgumentException("Illegal group reference: group index is missing");
                }

                if (replacement.charAt(cursor) == '{') {

                    // "${expr}".
                    final Expression expression;
                    {
                        final int[] offset = { cursor + 1 };
                        try {
                            expression = new ExpressionEvaluator(isValidVariableName).parsePart(replacement, offset);
                        } catch (ParseException pe) {
                            throw ExceptionUtil.wrap((
                                "Parsing expression \""
                                + replacement.substring(cursor + 1)
                                + "\" in replacement string"
                            ), pe, IllegalArgumentException.class);
                        }
                        cursor = offset[0];
                    }

                    // Skip closing "}".
                    if (cursor == replacement.length() || replacement.charAt(cursor) != '}') {
                        throw new IllegalArgumentException("expression is missing trailing '}'");
                    }
                    cursor++;

                    segments.add(new Producer<String>() {

                        @Override @Nullable public String
                        produce() {
                            try {
                                return expression.evaluateTo(variables, String.class);
                            } catch (EvaluationException ee) {
                                throw ExceptionUtil.wrap((
                                    "Evaluating expression \""
                                    + expression
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
                    segments.add(new Producer<String>() {

                        @Override public String
                        produce() {
                            String g = MatcherImpl.this.group(finalGroupNumber);
                            return g != null ? g : "";
                        }
                    });
                }
            } else {

                // Literal segment, including escape sequences like "\Q...\E".
                StringBuilder sb = new StringBuilder();
                do {
                    cursor = unescape(replacement, cursor, replacement.length(), sb);
                } while (cursor < replacement.length() && replacement.charAt(cursor) != '$');

                segments.add(ProducerUtil.constantProducer(sb.toString()));
            }
        }

        return new CompiledReplacement() {

            @Override public void
            appendReplacement(Appendable appendable) {

                if (MatcherImpl.this.endOfPreviousMatch < 0) throw new IllegalStateException("No match available");

                // Expand the replacement string.
                String s;
                switch (segments.size()) {

                case 0:
                    s = "";
                    break;

                case 1:
                    s = String.valueOf(segments.get(0).produce());
                    break;

                default:
                    StringBuilder result = new StringBuilder();
                    for (Producer<?> segment : segments) result.append(segment.produce());
                    s = result.toString();
                    break;
                }

                try {

                    // Append text between the previous match and THIS match.
                    appendable.append(
                        MatcherImpl.this.subject.subSequence(
                            MatcherImpl.this.lastAppendPosition,
                            MatcherImpl.this.groups[0]
                        )
                    );

                    // Append the expanded replacement string.
                    appendable.append(s);

                    MatcherImpl.this.lastAppendPosition = MatcherImpl.this.groups[1];
                } catch (IOException ioe) {
                    throw new AssertionError(ioe);
                }
            }

            @Override public String
            replaceAll() {

                MatcherImpl.this.reset();

                if (!MatcherImpl.this.find()) return MatcherImpl.this.subject.toString();

                StringBuilder sb = new StringBuilder();
                do {
                    this.appendReplacement(sb);
                } while (MatcherImpl.this.find());

                return MatcherImpl.this.appendTail(sb).toString();
            }

            @Override public String
            replaceFirst() {

                MatcherImpl.this.reset();

                if (!MatcherImpl.this.find()) return MatcherImpl.this.subject.toString();

                StringBuilder sb = new StringBuilder();
                this.appendReplacement(sb);
                return MatcherImpl.this.appendTail(sb).toString();
            }
        };
    }

    private static final java.util.regex.Pattern OCTAL_LITERAL = java.util.regex.Pattern.compile("\\\\0([0-7]{0,3})");
    private static final java.util.regex.Pattern HEX_LITERAL   = java.util.regex.Pattern.compile("\\\\x([0-9A-Fa-f]{2})");
    private static final java.util.regex.Pattern U_LITERAL     = java.util.regex.Pattern.compile("\\\\u([0-9A-Fa-f]{4})");
    private static final java.util.regex.Pattern CP_LITERAL    = java.util.regex.Pattern.compile("\\\\x\\{([0-9A-Fa-f]{1,8})\\}");
    private static final java.util.regex.Pattern QE_LITERAL    = java.util.regex.Pattern.compile("\\\\Q(.*?)\\\\E");
    private static final java.util.regex.Pattern Q_LITERAL     = java.util.regex.Pattern.compile("\\\\Q(.*+)");
    private static final java.util.regex.Pattern ESCAPE        = java.util.regex.Pattern.compile("\\\\([tnrfaeb])");
    private static final java.util.regex.Pattern CONTROL       = java.util.regex.Pattern.compile("\\\\c([A-Z])");
    private static final java.util.regex.Pattern MASKED        = java.util.regex.Pattern.compile("\\\\([^0xuQc])");
    
    /**
     * Scans the <var>input</var>, starting at <var>offset</var>, and adds one codepoint to the <var>result</var>. The
     * {@code "\Q...\E"} escape sequence adds multiple codepoints to the <var>result</var>.
     *
     * @return The offset of the first character <em>after</em> the parsed codepoint
     * @throws IllegalArgumentException <var>offset</var> >= <var>end</var>
     * @throws IllegalArgumentException Invalid escape sequence at <var>offset</var>
     */
    private static int
    unescape(String input, int offset, int end, StringBuilder result) {

        if (offset >= end) throw new IllegalArgumentException("End of input");
        
        int cp = input.codePointAt(offset);

        if (cp != '\\') {
            result.appendCodePoint(cp);
            return offset + Character.charCount(cp);
        }

        java.util.regex.Matcher m;
        
        // \0 \0n \0nn \0mnn
        m = OCTAL_LITERAL.matcher(input);
        m.region(offset, end);
        if (m.lookingAt()) {
            result.append((char) Integer.parseInt(m.group(1), 8));
            return m.end();
        }
        
        // \xhh
        m = HEX_LITERAL.matcher(input);
        m.region(offset, end);
        if (m.lookingAt()) {
            result.append((char) Integer.parseInt(m.group(1), 16));
            return m.end();
        }

        // \ uhhhh
        m = U_LITERAL.matcher(input);
        m.region(offset, end);
        if (m.lookingAt()) {
            result.append((char) Integer.parseInt(m.group(1), 16));
            return m.end();
        }
        
        // \x{h...h}
        m = CP_LITERAL.matcher(input);
        m.region(offset, end);
        if (m.lookingAt()) {
            result.appendCodePoint(Integer.parseInt(m.group(1), 16));
            return m.end();
        }
        
        // \Q...\E
        m = QE_LITERAL.matcher(input);
        m.region(offset, end);
        if (m.lookingAt()) {
            result.append(m.group(1));
            return m.end();
        }
        
        // \Q...
        m = Q_LITERAL.matcher(input);
        m.region(offset, end);
        if (m.lookingAt()) {
            result.append(m.group(1));
            return m.end();
        }
        
        // \t \n \r \f \a \e \b  (TAB NL CR FF BEL ESC BACKSPACE)
        m = ESCAPE.matcher(input);
        m.region(offset, end);
        if (m.lookingAt()) {
            result.append("\t\n\r\f\07\033\b".charAt("tnrfaeb".indexOf(m.group(1).charAt(0))));
            return m.end();
        }
        
        // \cx
        m = CONTROL.matcher(input);
        m.region(offset, end);
        if (m.lookingAt()) {
            result.append((char) (31 & m.group(1).charAt(0)));
            return m.end();
        }

        // \ <any>
        m = MASKED.matcher(input);
        m.region(offset, end);
        if (m.lookingAt()) {
            result.append(m.group(1));
            return m.end();
        }
        
        throw new IllegalArgumentException("Invalid escape sequence starting with \"" + input.substring(offset, Math.min(end, offset + 2)) + "\"");
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
        return this.compileReplacement(replacement).replaceAll();
    }

    @Override public String
    replaceAll(Function<MatchResult, String> replacer) {

        this.reset();

        if (!this.find()) return this.subject.toString();

        StringBuilder sb = new StringBuilder();
        do {
            this.appendReplacement(sb, replacer.apply(this));
        } while (this.find());
        this.appendTail(sb);

        return sb.toString();
    }

    @Override public Stream<MatchResult>
    results() {

        class MatchResultIterator implements Iterator<MatchResult> {

            // 0 for "unknown", 1 for "not found", 2 for "found"
            int state;

            @Override public MatchResult
            next() {

                if (!this.hasNext()) throw new NoSuchElementException();

                this.state = 0;
                return MatcherImpl.this.toMatchResult();
            }

            @Override public boolean
            hasNext() {
                if (this.state == 1) return false;
                if (this.state == 2) return true;

                boolean found = MatcherImpl.this.find();
                this.state = found ? 2 : 1;
                return found;
            }
        }

        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(new MatchResultIterator(), Spliterator.ORDERED | Spliterator.NONNULL),
            false
        );
    }

    @Override public String
    replaceFirst(String replacement) {
        return this.compileReplacement(replacement).replaceFirst();
    }
    
    @Override public String
    replaceFirst(Function<MatchResult, String> replacer) {

        this.reset();

        if (!this.find()) return this.subject.toString();

        StringBuilder sb = new StringBuilder();
        this.appendReplacement(sb, replacer.apply(this));
        this.appendTail(sb);

        return sb.toString();
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

    @Override public int
    count(int number) { return this.counters[number]; }

    // =====================================

    /**
     * If the subject infix ranging from the <var>offset</var> to the region end starts with the <var>cs</var>,
     * then the offset is advanced and {@code true} is returned.
     * <p>
     *   As a possible side effect, this method my set {@link #hitEnd} to {@code true}.
     * </p>
     *
     * @see #peekRead(CharSequence, int, int)
     */
    boolean
    peekRead(CharSequence cs) { return this.peekRead(cs, 0, cs.length()); }

    /**
     * If the subject infix ranging from the {@link #offset} to the region end starts with the sequence designated by
     * <var>cs</var>, <var>start</var> and <var>end</var>, then the {@link #offset} is advanced and {@code true} is
     * returned.
     * <p>
     *   As a possible side effect, this method my set {@link #hitEnd} to {@code true}.
     * </p>
     */
    boolean
    peekRead(CharSequence cs, int start, int end) {

        int o = this.offset;

        for (int i = start; i < end; i++, o++) {

            if (o >= this.regionEnd) {
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
     * <p>
     *   As a possible side effect, this method my set {@link #hitEnd} to {@code true}.
     * </p>
     *
     * @see #caseInsensitivePeekRead(CharSequence, int, int)
     */
    boolean
    caseInsensitivePeekRead(CharSequence cs) { return this.caseInsensitivePeekRead(cs, 0, cs.length()); }

    /**
     * If the subject infix ranging from the <var>offset</var> to the region end starts with the sequence designated by
     * <var>cs</var>, <var>start</var> and <var>end</var>, then the {@link #offset} is advanced and {@code true} is
     * returned.
     * <p>
     *   As a possible side effect, this method my set {@link #hitEnd} to {@code true}.
     * </p>
     */
    boolean
    caseInsensitivePeekRead(CharSequence cs, int start, int end) {

        int o = this.offset;

        if (o + end - start > this.regionEnd) {

            // Not enough chars left.
            this.hitEnd = true;
            return false;
        }

        for (int i = start; i < end; i++, o++) {

            // Notice: Don't need to worry about supplementary code points, because the case sensitive character
            // ranges are all in the basic UNICODE plane.
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
     * <p>
     *   As a possible side effect, this method my set {@link #hitEnd} to {@code true}.
     * </p>
     */
    boolean
    unicodeCaseInsensitivePeekRead(CharSequence cs) { return this.unicodeCaseInsensitivePeekRead(cs, 0, cs.length()); }

    /**
     * If the subject infix ranging from the {@link #offset} to the region end starts with the sequence designated by
     * <var>cs</var>, <var>start</var> and <var>end</var>, then the {@link #offset} is advanced and {@code true} is
     * returned.
     * <p>
     *   As a possible side effect, this method my set {@link #hitEnd} to {@code true}.
     * </p>
     */
    boolean
    unicodeCaseInsensitivePeekRead(CharSequence cs, int start, int end) {

        int o = this.offset;

        // Knowing that "toUpperCase()", "toLowerCase()" and "toTitleCase" are either ALL in the basic plane or are
        // ALL supplementary chars, the following optimization is possible:
        if (o + end - start > this.regionEnd) {

            // Not enough chars left.
            this.hitEnd = true;
            return false;
        }

        for (int i = start; i < end;) {

            char c1 = this.subject.charAt(o++);

            // Some highly optimized magic here for supplementary code points.
            {
                char ls;

                if (
                    Character.isHighSurrogate(c1)
                    && o < this.regionEnd
                    && Character.isLowSurrogate((ls = this.subject.charAt(o)))
                ) {
                    o++;

                    if (
                        !Character.isHighSurrogate(cs.charAt(i))
                        || !Character.isLowSurrogate(cs.charAt(i + 1))
                    ) return false;

                    int cp1 = Character.toCodePoint((char) c1, ls);
                    int cp2 = Character.toCodePoint(cs.charAt(i++), cs.charAt(i++));

                    if (!MatcherImpl.equalsIgnoreCase(cp1, cp2)) return false;
                    continue;
                }
            }

            // We're in the basic plane... everything is very simple now.
            if (!MatcherImpl.equalsIgnoreCase(c1, cs.charAt(i++))) return false;
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
        if (Character.toLowerCase(c1) == Character.toLowerCase(c2)) return true;

        return false;
    }

    /**
     * @param predicate Notably the type argument is {@link Character}, so this method cannot be used to process
     *                  supplementary code points
     * @return          Whether the <var>predicate</var> evaluates for the character at the given offset
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
            .append("[pattern=\"")
            .append(this.pattern())
            .append("\" region=")
            .append(this.regionStart)
            .append(",")
            .append(this.regionEnd)
            .append(" subject=\"")
            .append(this.subject)
            .append("\" endOfPreviousMatch=")
            .append(this.endOfPreviousMatch)
        );

        if (this.endOfPreviousMatch >= 0) {
            sb.append(" lastmatch=").append(this.subject.subSequence(this.groups[0], this.groups[1]));
        }

        return sb.append(']').toString();
    }

    int
    sequenceEndMatches(int offset) { return this.end == MatcherImpl.End.ANY || offset == this.regionEnd ? offset : -1; }

    public int
    positionPlus1(int pos) {

        assert pos >= this.regionStart && pos < this.regionEnd : this.regionStart + "<=" + pos + "<" + this.regionEnd;

        if (
            Character.isHighSurrogate(this.subject.charAt(pos++))
            && pos < this.regionEnd
            && Character.isLowSurrogate(this.subject.charAt(pos))
        ) pos++;

        return pos;
    }

    public int
    positionMinus1(int pos) {

        assert pos > this.regionStart : pos + ">" + this.regionStart;

        if (
            Character.isLowSurrogate(this.subject.charAt(--pos))
            && pos > this.regionStart
            && Character.isHighSurrogate(this.subject.charAt(pos - 1))
        ) pos--;

        return pos;
    }

    /**
     * Gets and returns the code point at {@code this.offset} and increases {@code this.offset} by one (or two, for a
     * non-BMP code point).
     */
    public int
    readChar() {

        assert this.regionStart <= this.offset && this.offset < this.regionEnd : this.regionStart + "<=" + this.offset + "<" + this.regionEnd;

        int cp = this.subject.charAt(this.offset++);
        if (Character.isHighSurrogate((char) cp) && this.offset < this.regionEnd) {
            char ls = this.subject.charAt(this.offset);
            if (Character.isLowSurrogate(ls)) {
                cp = Character.toCodePoint((char) cp, ls);
                this.offset++;
            }
        }
        return cp;
    }

    /**
     * Gets and returns the code point at {@code this.offset} and leaves {@code this.offset} unchanged.
     */
    public int
    peekChar() {

        int o = this.offset;
        assert this.regionStart <= o && o < this.regionEnd : this.regionStart + "<=" + o + "<" + this.regionEnd;

        int cp = this.subject.charAt(o);
        if (Character.isHighSurrogate((char) cp) && o + 1 < this.regionEnd) {
            char ls = this.subject.charAt(o + 1);
            if (Character.isLowSurrogate(ls)) {
                cp = Character.toCodePoint((char) cp, ls);
            }
        }
        return cp;
    }

    /**
     * Gets and returns the code point before {@code this.offset} and leaves {@code this.offset} unchanged.
     */
    public int
    peekPreviousChar() {

        int o = this.offset;
        assert o > this.regionStart : o + ">" + this.regionStart;

        int cp = this.subject.charAt(o - 1);
        if (Character.isLowSurrogate((char) cp) && o >= this.regionStart + 2) {
            char hs = this.subject.charAt(o - 2);
            if (Character.isHighSurrogate(hs)) {
                cp = Character.toCodePoint(hs, (char) cp);
            }
        }
        return cp;
    }
}
