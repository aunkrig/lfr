
/*
 * ref4j - Regular Expression Facade for Java
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

package de.unkrig.ref4j.jur;

import java.io.IOException;
import java.util.regex.MatchResult;
import java.util.regex.PatternSyntaxException;

import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;

/**
 * Adapter for the {@code java.util.regex} engine.
 */
public class PatternFactory extends de.unkrig.ref4j.PatternFactory {

    private static final double JAVA_SPECIFICATION_VERSION = Double.parseDouble(System.getProperty("java.specification.version"));

    private static final int SUPPORTED_FLAGS = (
        0
        | Pattern.CANON_EQ
        | Pattern.CASE_INSENSITIVE
        | Pattern.COMMENTS
        | Pattern.DOTALL
        | Pattern.LITERAL
        | Pattern.MULTILINE
        | Pattern.UNICODE_CASE
        | Pattern.UNIX_LINES
        | (PatternFactory.JAVA_SPECIFICATION_VERSION >= 1.7 ? Pattern.UNICODE_CHARACTER_CLASS : 0)
    );

    private PatternFactory() {}

    public static final PatternFactory INSTANCE = new PatternFactory();

    @Override public int
    getSupportedFlags() { return PatternFactory.SUPPORTED_FLAGS; }

    @Override public Pattern
    compile(final String regex, final int flags) throws PatternSyntaxException {

        class JurPattern implements Pattern {

            final java.util.regex.Pattern jurPattern = java.util.regex.Pattern.compile(regex, flags);

            @Override public String
            pattern() { return this.jurPattern.pattern(); }

            @Override public Matcher
            matcher(CharSequence subject) {

                final java.util.regex.Matcher m = this.jurPattern.matcher(subject);
                return new Matcher() {

                    @Override public Matcher     useTransparentBounds(boolean b)  { m.useTransparentBounds(b); return this; }
                    @Override public Matcher     usePattern(Pattern newPattern)   { m.usePattern(((JurPattern) newPattern).jurPattern); return this; }
                    @Override public Matcher     useAnchoringBounds(boolean b)    { m.useAnchoringBounds(b); return this; }
                    @Override public MatchResult toMatchResult()                  { return m.toMatchResult(); }
                    @Override public int         start(int group)                 { return m.start(group); }
                    @Override public int         start(String name)               { throw new UnsupportedOperationException("N/A"); }
                    @Override public int         start()                          { return m.start(); }
                    @Override public Matcher     reset(CharSequence input)        { m.reset(input); return this; }
                    @Override public Matcher     reset()                          { m.reset(); return this; }
                    @Override public boolean     requireEnd()                     { return m.requireEnd(); }
                    @Override public String      replaceFirst(String replacement) { return m.replaceFirst(replacement); }
                    @Override public String      replaceAll(String replacement)   { return m.replaceAll(replacement); }
                    @Override public int         regionStart()                    { return m.regionStart(); }
                    @Override public int         regionEnd()                      { return m.regionEnd(); }
                    @Override public Matcher     region(int start, int end)       { m.region(start, end); return this; }
                    @Override public Pattern     pattern()                        { return JurPattern.this; }
                    @Override public boolean     matches()                        { return m.matches(); }
                    @Override public boolean     lookingAt()                      { return m.lookingAt(); }
                    @Override public boolean     hitEnd()                         { return m.hitEnd(); }
                    @Override public boolean     hasTransparentBounds()           { return m.hasTransparentBounds(); }
                    @Override public boolean     hasAnchoringBounds()             { return m.hasAnchoringBounds(); }
                    @Override public int         groupCount()                     { return m.groupCount(); }
                    @Override public String      group(String name)               { throw new UnsupportedOperationException("N/A"); }
                    @Override public String      group(int group)                 { return m.group(group); }
                    @Override public String      group()                          { return m.group(); }
                    @Override public boolean     find(int start)                  { return m.find(start); }
                    @Override public boolean     find()                           { return m.find(); }
                    @Override public int         end(String name)                 { throw new UnsupportedOperationException("N/A"); }
                    @Override public int         end(int group)                   { return m.end(group); }
                    @Override public int         end()                            { return m.end(); }

                    @Override public <T extends Appendable> T
                    appendTail(T appendable) {

                        if (appendable instanceof StringBuffer) {
                            @SuppressWarnings("unchecked") T result = (T) m.appendTail((StringBuffer) appendable);
                            return result;
                        }

                        try {
                            StringBuffer sb = new StringBuffer();
                            m.appendTail(sb);
                            appendable.append(sb);
                            return appendable;
                        } catch (IOException ioe) {
                            throw new AssertionError(ioe);
                        }
                    }

                    @Override public Matcher
                    appendReplacement(Appendable appendable, String replacement) {

                        if (appendable instanceof StringBuffer) {
                            m.appendReplacement((StringBuffer) appendable, replacement);
                            return this;
                        }

                        try {
                            StringBuffer sb = new StringBuffer();
                            m.appendReplacement(sb, replacement);
                            appendable.append(sb);
                            return this;
                        } catch (IOException ioe) {
                            throw new AssertionError(ioe);
                        }
                    }
                };
            }

            @Override public int
            flags() { return this.jurPattern.flags(); }

            @Override public boolean
            matches(CharSequence subject, int offset) {
                return this.jurPattern.matcher(subject.subSequence(offset, subject.length())).matches();
            }

            @Override public String[]
            split(CharSequence input) { return this.jurPattern.split(input); }

            @Override public String
            quote(String s) { return java.util.regex.Pattern.quote(s); }
        }

        return new JurPattern();
    }

    @Override public boolean
    matches(String regex, CharSequence input) { return java.util.regex.Pattern.matches(regex, input); }
}
