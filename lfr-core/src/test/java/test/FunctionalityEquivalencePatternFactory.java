
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

package test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.PatternSyntaxException;

import org.junit.Assert;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;

/**
 * A {@link PatternFactory} that verifies that all operations on a pattern (and its matcher) produce identical
 * results for two different {@link PatternFactory}s.
 */
public
class FunctionalityEquivalencePatternFactory extends PatternFactory {

    private final PatternFactory reference;
    private final PatternFactory subject;

    /**
     * @param reference The {@link PatternFactory} for the "expected" values
     * @param subject   The {@link PatternFactory} for the "actual" values
     */
    public
    FunctionalityEquivalencePatternFactory(PatternFactory reference, PatternFactory subject) {
        this.reference = reference;
        this.subject   = subject;
    }

    @Override public int
    getSupportedFlags() { throw new UnsupportedOperationException(); }

    @Override public Pattern
    compile(final String regex, final int flags) throws PatternSyntaxException {

        final Pattern referencePattern = this.reference.compile(regex, flags);
        final Pattern subjectPattern   = this.subject.compile(regex,   flags);

        return new Pattern() {

            @Override public String pattern() { return regex; }
            @Override public int    flags()   { return flags; }

            @Override public Matcher
            matcher(final CharSequence subject) {

                final Matcher referenceMatcher = referencePattern.matcher(subject);
                final Matcher subjectMatcher   = subjectPattern.matcher(subject);

                final Pattern pattern = this;

                return (Matcher) Proxy.newProxyInstance(
                    Matcher.class.getClassLoader(),
                    new Class[] { Matcher.class },
                    new InvocationHandler() {

                        @Override @NotNullByDefault(false) public Object
                        invoke(Object proxy, final Method method, final Object[] arguments) throws Throwable {

                            Object    referenceResult    = null;
                            Throwable referenceException = null;
                            try {
                                referenceResult = method.invoke(referenceMatcher, arguments);
                            } catch (InvocationTargetException ite) {
                                referenceException = ite.getTargetException();
                            } catch (RuntimeException re) {
                                referenceException = re;
                            }

                            // KLUDGE: DUPLICATE the Appendable argument here, so that the method does not append
                            // its result TWICE.
                            if (arguments != null && arguments.length >= 1 && arguments[0] instanceof Appendable) {
                                arguments[0] = new StringBuilder(arguments[0].toString());
                            }

                            Object    subjectResult    = null;
                            Throwable subjectException = null;
                            try {
                                subjectResult = method.invoke(subjectMatcher, arguments);
                            } catch (InvocationTargetException ite) {
                                subjectException = ite.getTargetException();
                            } catch (RuntimeException re) {
                                subjectException = re;
                            }

                            Assert.assertEquals(
                                referenceException == null ? null : referenceException.getClass(),
                                subjectException   == null ? null : subjectException.getClass()
                            );

                            if (referenceResult instanceof Matcher && subjectResult instanceof Matcher) return proxy;

                            if (referenceResult instanceof Pattern && subjectResult instanceof Pattern) return pattern;

                            Assert.assertEquals(method.toString(), referenceResult, subjectResult);

                            assertEqualState(method.getName() + "()");

                            return referenceResult;
                        }

                        private void
                        assertEqualState(String message) {

                            String  referenceGroup;
                            boolean refrenceGroupThrowsException;
                            try {
                                referenceGroup               = referenceMatcher.group();
                                refrenceGroupThrowsException = false;
                            } catch (IllegalStateException ise) {
                                referenceGroup               = null;
                                refrenceGroupThrowsException = true;
                            }

                            String  subjectGroup;
                            boolean subjectGroupThrowsException;
                            try {
                                subjectGroup                = subjectMatcher.group();
                                subjectGroupThrowsException = false;
                            } catch (IllegalStateException ise) {
                                subjectGroup                = null;
                                subjectGroupThrowsException = true;
                            }

                            Assert.assertEquals(
                                message + ": group() throws ISE (rg=" + referenceGroup + ", sg=" + subjectGroup + ")",
                                refrenceGroupThrowsException,
                                subjectGroupThrowsException
                            );


                            Assert.assertEquals(message + ": group()", referenceGroup, subjectGroup);

                            if (referenceGroup != null) {

                                Assert.assertEquals(
                                    message + ": groupCount()",
                                    referenceMatcher.groupCount(),
                                    subjectMatcher.groupCount()
                                );

                                for (int i = 0; i <= referenceMatcher.groupCount(); i++) {
                                    Assert.assertEquals(message + ": start(" + i + ")", referenceMatcher.start(i), subjectMatcher.start(i)); // SUPPRESS CHECKSTYLE LineLength:3
                                    Assert.assertEquals(message + ": end("   + i + ")", referenceMatcher.end(i),   subjectMatcher.end(i));
                                    Assert.assertEquals(message + ": group(" + i + ")", referenceMatcher.group(i), subjectMatcher.group(i));
                                }

                                Assert.assertEquals(
                                    message + ": requireEnd()",
                                    referenceMatcher.requireEnd(),
                                    subjectMatcher.requireEnd()
                                );
                            }

                            Assert.assertEquals(
                                message + ": hitEnd()",
                                referenceMatcher.hitEnd(),
                                subjectMatcher.hitEnd()
                            );
                        }
                    }
                );
            }

            @Override public boolean
            matches(final CharSequence subject, final int offset) {

                boolean expected = referencePattern.matches(subject, offset);
                boolean actual   = subjectPattern.matches(subject, offset);

                Assert.assertEquals(expected, actual);
                return expected;
            }

            @Override public String[]
            split(CharSequence input) { return this.split(input, 0); }

            @Override public String[]
            split(CharSequence input, int limit) {

                String[] expected = referencePattern.split(input, limit);
                String[] actual   = subjectPattern.split(input, limit);

                Assert.assertArrayEquals(expected, actual);

                return expected;
            }
        };
    }

    @Override public boolean
    matches(String regex, CharSequence input) { return this.compile(regex).matcher(input).matches(); }

    public void
    assertPatternSyntaxException(String regex) {
        this.assertPatternSyntaxException(regex, 0);
    }

    public void
    assertPatternSyntaxException(String regex, int flags) {

        boolean referenceThrows;
        try {
            this.reference.compile(regex, flags);
            referenceThrows = false;
        } catch (PatternSyntaxException pse) {
            referenceThrows = true;
        }

        boolean subjectThrows;
        try {
            this.subject.compile(regex, flags);
            subjectThrows = false;
        } catch (PatternSyntaxException pse) {
            subjectThrows = true;
        }

        Assert.assertEquals(referenceThrows, subjectThrows);
    }
}
