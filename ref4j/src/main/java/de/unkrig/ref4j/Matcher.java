
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

package de.unkrig.ref4j;

import java.io.IOException;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.stream.Stream;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A wrapper for {@link java.util.regex.Matcher}.
 */
@NotNullByDefault public
interface Matcher extends java.util.regex.MatchResult {

    /**
     * @see java.util.regex.Matcher#pattern()
     */
    Pattern pattern();

    /**
     * @see java.util.regex.Matcher#toMatchResult()
     */
    java.util.regex.MatchResult toMatchResult();

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
     * @see java.util.regex.MatchResult#start()
     * @see java.util.regex.Matcher#start()
     */
    @Override int start();

    /**
     * @see java.util.regex.Matcher#start(String)
     */
    int start(String name);

    /**
     * @see java.util.regex.MatchResult#start(int)
     * @see java.util.regex.Matcher#start(int)
     */
    @Override int start(int group);

    /**
     * @see java.util.regex.MatchResult#end()
     * @see java.util.regex.Matcher#end()
     */
    @Override int end();

    /**
     * @see java.util.regex.MatchResult#end(int)
     * @see java.util.regex.Matcher#end(int)
     */
    @Override int end(int group);

    /**
     * @see java.util.regex.Matcher#end(String)
     */
    int end(String name);

    /**
     * @see java.util.regex.MatchResult#group()
     * @see java.util.regex.Matcher#group()
     */
    @Override @Nullable String group();

    /**
     * @see java.util.regex.MatchResult#group(int)
     * @see java.util.regex.Matcher#group(int)
     */
    @Override @Nullable String group(int group);

    /**
     * @throws NullPointerException <var>name</var> is {@code null}
     * @see                         java.util.regex.Matcher#group(String)
     */
    @Nullable String group(String name);

    /**
     * @see java.util.regex.MatchResult#groupCount()
     * @see java.util.regex.Matcher#groupCount()
     */
    @Override int groupCount();

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

    /**
     * @see java.util.regex.Matcher#quoteReplacement(String)
     */
    public String
    quoteReplacement(String s);

    /**
     * @param appendable Must not throw any {@link IOException}s
     * @see              java.util.regex.Matcher#appendReplacement(StringBuffer, String)
     */
    Matcher appendReplacement(Appendable appendable, String replacement);

    /**
     * @param appendable Must not throw any {@link IOException}s
     * @see              java.util.regex.Matcher#appendTail(StringBuffer)
     */
    <T extends Appendable> T appendTail(T appendable);

    /**
     * @see java.util.regex.Matcher#replaceAll(String)
     */
    String replaceAll(String replacement);

    /**
     * @see java.util.regex.Matcher#replaceAll(Function)
     */
    String replaceAll(Function<MatchResult, String> replacer);

    /**
     * @see java.util.regex.Matcher#results()
     */
    Stream<MatchResult> results();

    /**
     * @see java.util.regex.Matcher#replaceFirst(String)
     */
    String replaceFirst(String replacement);

    /**
     * @see java.util.regex.Matcher#replaceFirst(Function)
     */
    String replaceFirst(Function<MatchResult,String> replacer);

    /**
     * @see java.util.regex.Matcher#region(int, int)
     */
    Matcher region(int start, int end);

    /**
     * @see java.util.regex.Matcher#regionStart()
     */
    int regionStart();

    /**
     * @see java.util.regex.Matcher#regionEnd()
     */
    int regionEnd();

    /**
     * @see java.util.regex.Matcher#hasTransparentBounds()
     */
    boolean hasTransparentBounds();

    /**
     * @see java.util.regex.Matcher#useTransparentBounds(boolean)
     */
    Matcher useTransparentBounds(boolean b);

    /**
     * @see java.util.regex.Matcher#hasAnchoringBounds()
     */
    boolean hasAnchoringBounds();

    /**
     * @see java.util.regex.Matcher#useAnchoringBounds(boolean)
     */
    Matcher useAnchoringBounds(boolean b);

    /**
     * @see java.util.regex.Matcher#toString()
     */
    @Override String toString();

    /**
     * @see java.util.regex.Matcher#hitEnd()
     */
    boolean hitEnd();

    /**
     * @see java.util.regex.Matcher#requireEnd()
     */
    boolean requireEnd();
}
