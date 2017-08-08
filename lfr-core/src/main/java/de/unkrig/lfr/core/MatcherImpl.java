
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
import java.util.Arrays;
import java.util.regex.MatchResult;

import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * {@code de.unkrig.lfr.core}'s implementation of {@link Matcher}.
 */
final
class MatcherImpl implements Matcher {

    // CONFIGURATION

    /**
     * @see #END_OF_SUBJECT
     * @see #ANY
     */
    enum End {

        /**
         * The match is successful if the pattern matches the entire rest of the sucject.
         */
        END_OF_SUBJECT,

        /**
         * The match is successful if the pattern matches the next characters of the sucject.
         */
        ANY,
    }

    private Pattern pattern;
    private boolean reverseSubject;
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

    int transparentRegionStart, transparentRegionEnd;

    int anchoringRegionStart, anchoringRegionEnd;

    // STATE

    /**
     * The offsets of the captured groups within the {@link #subject}. The offset of the first character of the
     * <var>n</var>th group is 2*<var>n</var>; the offset <em>after</em> the <var>n</var>th group is 2*<var>n</var> +
     * 1. These offsets are {@code -1} if the group did not match anything.
     * <p>
     *   This field is modified bei {@link Sequence#matches(MatcherImpl, int)}, but the <em>elements</em> of this
     *   array are <em>never</em> modified! This makes it easy to "remember" (and later restore) the groups.
     * </p>
     */
    int[] groups;

    /**
     * The template to store in {@link #groups} when the matching begins.
     */
    int[] initialGroups;

    /**
     * Whether an attempt was made to peek <em>before</em> the {@link #regionStart}.
     */
    boolean hitStart;

    /**
     * Whether an attempt was made to peek <em>before</em> the {@link #transparentRegionStart}.
     */
    boolean requireStart;

    /**
     * Whether an attempt was made to peek at or behind the {@link #regionEnd}.
     */
    boolean hitEnd;

    /**
     * Whether an attempt was made to peek at or behind the {@link #transparentRegionEnd}.
     */
    boolean requireEnd;

    /**
     * The index <em>behind</em> the preceding match, or -1 if no matching was done, or the preceding match had
     * failed.
     */
    int endOfPreviousMatch = -1;

    private int lastAppendPosition;

    @Nullable MatcherImpl.End end;

    MatcherImpl(Pattern pattern, CharSequence subject) {
        this.pattern   = pattern;
        this.subject   = subject;
        this.regionEnd = (this.anchoringRegionEnd = (this.transparentRegionEnd = subject.length()));
        this.groups    = (this.initialGroups = new int[2 + 2 * pattern.groupCount]);
        Arrays.fill(this.groups, -1);
    }

    @Override public MatchResult
    toMatchResult() { return this; }

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
    reset() {
        this.regionStart = 0;
        this.regionEnd   = this.subject.length();
        this.updateTransparentBounds();
        this.updateAnchoringBounds();

        this.hitStart           = false;
        this.requireStart       = false;
        this.hitEnd             = false;
        this.requireEnd         = false;
        this.endOfPreviousMatch = -1;
        this.lastAppendPosition = 0;
        return this;
    }

    @Override public Matcher
    reset(CharSequence input) {
        this.subject = input;
        return this.reset();
    }

    @Override public int
    start(int groupNumber) {

        if (this.endOfPreviousMatch == -1) throw new IllegalStateException("No match available");

        return this.groups[2 * groupNumber];
    }

    @Override public int
    start(String groupName) {

        if (this.endOfPreviousMatch == -1) throw new IllegalStateException("No match available");

        Integer groupNumber = this.pattern.namedGroups.get(groupName);
        if (groupNumber == null) throw new IllegalArgumentException("Invalid group name \"" + groupName + "\"");

        return this.groups[2 * groupNumber];
    }

    @Override public int
    end(int groupNumber) {

        if (this.endOfPreviousMatch == -1) throw new IllegalStateException("No match available");

        return this.groups[2 * groupNumber + 1];
    }

    @Override public int
    end(String groupName) {

        if (this.endOfPreviousMatch == -1) throw new IllegalStateException("No match available");

        Integer groupNumber = this.pattern.namedGroups.get(groupName);
        if (groupNumber == null) throw new IllegalArgumentException("Invalid group name \"" + groupName + "\"");

        return this.groups[2 * groupNumber + 1];
    }

    @Nullable @Override public String
    group(int groupNumber) {

        if (this.endOfPreviousMatch == -1) throw new IllegalStateException("No match available");

        int[] gs = this.groups;

        int start = gs[2 * groupNumber];
        int end   = gs[2 * groupNumber + 1];

        return start == -1 ? null : this.subject.subSequence(start, end).toString();
    }

    @Override @Nullable public String
    group(String groupName) {

        if (this.endOfPreviousMatch == -1) throw new IllegalStateException("No match available");

        Integer groupNumber = this.pattern.namedGroups.get(groupName);
        if (groupNumber == null) throw new IllegalArgumentException("Invalid group name \"" + groupName + "\"");

        int[] gs = this.groups;

        int start = gs[2 * groupNumber];
        int end   = gs[2 * groupNumber + 1];

        return start == -1 ? null : this.subject.subSequence(start, end).toString();
    }

    @Override public int              start()        { return this.start(0);             }
    @Override public int              end()          { return this.end(0);               }
    @Override @Nullable public String group()        { return this.group(0);             }
    @Override public int              groupCount()   { return this.pattern().groupCount; }
    @Override public boolean          hitStart()     { return this.hitStart;             }
    @Override public boolean          requireStart() { return this.requireStart;         }
    @Override public boolean          hitEnd()       { return this.hitEnd;               }
    @Override public boolean          requireEnd()   { return this.requireEnd;           }

    @Override public boolean
    matches() {

        this.groups       = this.initialGroups;
        this.hitStart     = false;
        this.requireStart = false;
        this.hitEnd       = false;
        this.requireEnd   = false;

        this.end = MatcherImpl.End.END_OF_SUBJECT;
        int newOffset = this.pattern.sequence.matches(this, this.regionStart);

        if (newOffset == -1) {
            this.endOfPreviousMatch = -1;
            return false;
        }

        this.groups[0]          = this.regionStart;
        this.groups[1]          = newOffset;
        this.endOfPreviousMatch = newOffset;

        return true;
    }

    @Override public boolean
    lookingAt() {

        this.groups       = this.initialGroups;
        this.hitStart     = false;
        this.requireStart = false;
        this.hitEnd       = false;
        this.requireEnd   = false;

        this.end = MatcherImpl.End.ANY;
        int newOffset = this.pattern.sequence.matches(this, this.regionStart);

        if (newOffset == -1) {
            this.endOfPreviousMatch = -1;
            return false;
        }

        this.groups[0]          = this.regionStart;
        this.groups[1]          = newOffset;
        this.endOfPreviousMatch = newOffset;

        return true;
    }

    @Override public boolean
    find() { return this.find(this.endOfPreviousMatch == -1 ? this.regionStart : this.groups[1]); }

    @Override public boolean
    find(int start) {

        this.hitStart     = false;
        this.requireStart = false;
        this.hitEnd       = false;
        this.requireEnd   = false;

        if (this.endOfPreviousMatch != -1 && start == this.groups[0]) {

            // The previous match is a zero-length match. To prevent an endless series of these matches, advance
            // start position by one.
            if (start >= this.regionEnd) {
                this.endOfPreviousMatch = -1;
                this.hitEnd             = true;
                return false;
            }
            start++;
        }

        this.groups = this.initialGroups;
        this.end    = MatcherImpl.End.ANY;
        if (this.pattern.sequence.find(this, start)) {
            this.endOfPreviousMatch = this.groups[1];
            return true;
        }

        this.endOfPreviousMatch = -1;
        return false;
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

        if (this.endOfPreviousMatch == -1) throw new IllegalStateException("No match available");

        try {

            // Append text between the previous match and THIS match.
            appendable.append(this.subject.subSequence(this.lastAppendPosition, this.groups[0]));

            // Append the (expanded) replacement string.
            for (int cursor = 0; cursor < replacement.length();) {
                char c = replacement.charAt(cursor++);

                if (c == '\\') {
                    appendable.append(replacement.charAt(cursor++));
                } else
                if (c == '$') {
                    int groupNumber = Character.digit(replacement.charAt(cursor++), 10);
                    if (groupNumber == -1) throw new IllegalArgumentException("Illegal group reference");

                    for (; cursor < replacement.length(); cursor++) {

                        int nextDigit = Character.digit(replacement.charAt(cursor), 10);
                        if (nextDigit == -1) break;

                        int newGroupNumber = groupNumber * 10 + nextDigit;
                        if (newGroupNumber > this.groupCount()) break;
                        groupNumber = newGroupNumber;
                    }

                    if (this.group(groupNumber) != null) appendable.append(this.group(groupNumber));
                } else
                {
                    appendable.append(c);
                }
            }

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

        if (this.hasAnchoringBounds) {
            this.anchoringRegionStart = start;
            this.anchoringRegionEnd   = end;
        }

        if (!this.hasTransparentBounds) {
            this.transparentRegionStart = start;
            this.transparentRegionEnd   = end;
        }

        return this;
    }

    @Override public Matcher
    useTransparentBounds(boolean b) {
        this.hasTransparentBounds = b;
        this.updateTransparentBounds();
        return this;
    }

    @Override public Matcher
    useAnchoringBounds(boolean b) {
        this.hasAnchoringBounds = b;
        this.updateAnchoringBounds();
        return this;
    }

    // =====================================

    private void
    updateTransparentBounds() {
        if (this.hasTransparentBounds) {
            this.transparentRegionStart = 0;
            this.transparentRegionEnd   = this.subject.length();
        } else {
            this.transparentRegionStart = this.regionStart;
            this.transparentRegionEnd   = this.regionEnd;
        }
    }

    private void
    updateAnchoringBounds() {
        if (this.hasAnchoringBounds) {
            this.anchoringRegionStart = this.regionStart;
            this.anchoringRegionEnd   = this.regionEnd;
        } else {
            this.anchoringRegionStart = 0;
            this.anchoringRegionEnd   = this.subject.length();
        }
    }

    /**
     * If the subject infix ranging from the <var>offset</var> to the region end starts with the <var>cs</var>,
     * then the offset is advanced and {@code true} is returned.
     *
     * @return The offset after the match, or -1 if <var>cs</var> does not match
     */
    int
    peekRead(int offset, CharSequence cs) {

        int csLength = cs.length();

        if (offset + csLength > this.regionEnd) {

            // Not enough chars left.
            this.hitEnd = true;
            return -1;
        }

        for (int i = 0; i < csLength; i++) {
            if (this.charAt(offset) != cs.charAt(i)) return -1;
            offset++;
        }

        return offset;
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

        return predicate.evaluate(this.charAt(offset));
    }

    /**
     * @see String#charAt(int)
     */
    char
    charAt(int offset) {
        return this.subject.charAt(this.reverseSubject ? this.subject.length() - 1 - offset : offset);
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
        );

        if (this.endOfPreviousMatch != -1) {
            sb.append(" lastmatch=").append(this.subject.subSequence(this.groups[0], this.groups[1]));
        }

        return sb.append(']').toString();
    }

    int
    sequenceEndMatches(int offset) { return this.end == MatcherImpl.End.ANY || offset == this.regionEnd ? offset : -1; }

    /**
     * Reverses this matcher, i.e. the subject and all capturing groups are reversed.
     * <p>
     *   Example: If the subject is {@code "abc"} and the capturing groups are $1=0-1 and $2=0-2, then afterwards
     *   the subject is {@code "cba"}, anf the capturing groups are $1=2-3 and $2=1-3.
     * </p>
     * <p>
     *   Matcher reversing is used by "lookbehind", where a sequence is in <em>reverse</em> direction.
     * </p>
     */
    void
    reverse() {

        // Reverse the subject.
        this.reverseSubject = !this.reverseSubject;

        int l = this.subject.length();

        // Reverse the "region".
        {
            int tmp = this.regionStart;
            this.regionEnd = l - this.regionEnd;
            this.regionEnd = l - tmp;
        }

        // Reverse the "transparent region".
        {
            int tmp = this.transparentRegionStart;
            this.transparentRegionEnd = l - this.transparentRegionEnd;
            this.transparentRegionEnd = l - tmp;
        }

        // Reverse the "anchoring region".
        {
            int tmp = this.anchoringRegionStart;
            this.anchoringRegionEnd = l - this.anchoringRegionEnd;
            this.anchoringRegionEnd = l - tmp;
        }

        // Reverse "hitStart" and "hitEnd".
        {
            boolean tmp = this.hitStart;
            this.hitStart = this.hitEnd;
            this.hitEnd   = tmp;
        }

        // Reverse "requireStart" and "requireEnd".
        {
            boolean tmp = this.requireStart;
            this.requireStart = this.requireEnd;
            this.requireEnd   = tmp;
        }

        // Reverse the groups.
        {
            int[] gs  = this.groups;
            if (gs.length > 2) {
                int gsl = gs.length;
                int i   = 2;
                do {

                    int tmp = gs[i];
                    if (tmp == -1) {
                        i += 2;
                        continue;
                    }

                    gs[i] = l - gs[i + 1];
                    i++;
                    gs[i] = l - tmp;
                    i++;
                } while (i < gsl);
            }
        }

        // Reverse the "endOfPreviousMatch".
        if (this.endOfPreviousMatch != -1) this.endOfPreviousMatch = l - this.endOfPreviousMatch;
    }
}
