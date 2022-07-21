
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Assert;

import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.PrettyPrinter;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;

/**
 * A {@link PatternFactory} that verifies that all operations on a pattern (and its matcher) produce identical
 * results for two different {@link PatternFactory}s.
 * <p>
 *   This class honors two system properties, {@code "FIX_HIT_END"} and {@code "FIX_REQUIRE_END"} to work around the
 *   many bugs in JUR's {@link java.util.regex.Matcher#hitEnd()} and {@link java.util.regex.Matcher#hitEnd()}.
 * </p>
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

    @Override public String
    getId() { throw new UnsupportedOperationException(); }

    @Override public int
    getSupportedFlags() { throw new UnsupportedOperationException(); }

    @Override public Pattern
    compile(final String regex, final int flags) throws PatternSyntaxException {

        // Compile the two patterns; verify that either BOTH compile, or NEITHER compiles.
        final Pattern referencePattern, subjectPattern;
        try {
            referencePattern = this.reference.compile(regex, flags);
            try {
                subjectPattern = this.subject.compile(regex, flags);
            } catch (PatternSyntaxException pse) {
                throw new AssertionError("Unexpected PatternSyntaxException: " + pse);
            }
        } catch (PatternSyntaxException pse) {
            this.subject.compile(regex, flags);
            throw new AssertionError("Expected a PatternSyntaxException: " + pse);
        }

        return new Pattern() {

            @Override public String pattern() { return regex; }
            @Override public int    flags()   { return flags; }


            @Override public String
            toString() {
                String referenceToString = referencePattern.toString();
                String subjectToString   = subjectPattern.toString();
                Assert.assertEquals(referenceToString, subjectToString);
                return referenceToString;
            }

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
                        invoke(Object proxy, final Method method, Object[] arguments) throws Throwable {

                            if (arguments == null) arguments = new Object[0];

                            // KLUDGE: DUPLICATE the Appendable argument here, so that the method does not append
                            // its result TWICE.
                            Object arg0 = (
                                arguments.length >= 1 && arguments[0] instanceof Appendable
                                ? new StringBuilder(arguments[0].toString())
                                : null
                            );

                            Object    referenceResult    = null;
                            Throwable referenceException = null;
                            try {
                                referenceResult = method.invoke(referenceMatcher, arguments);
                            } catch (InvocationTargetException ite) {
                                referenceException = ite.getTargetException();
                            } catch (RuntimeException re) {
                                referenceException = re;
                            }

                            if (arg0 != null) arguments[0] = arg0;

                            Object    subjectResult    = null;
                            Throwable subjectException = null;
                            try {
                                subjectResult = method.invoke(subjectMatcher, arguments);
                            } catch (InvocationTargetException ite) {
                                subjectException = ite.getTargetException();
                            } catch (RuntimeException re) {
                                subjectException = re;
                            }

                            if (referenceException != null) {
                                if (subjectException == null) {
                                    Error e = new AssertionError(
                                        "Regex \""
                                        + regex
                                        + "\": Expected a "
                                        + referenceException.getClass()
                                    );
                                    e.initCause(referenceException);
                                    throw e;
                                }
                                Assert.assertEquals(
                                    method.toString(),
                                    referenceException.getClass(),
                                    subjectException.getClass()
                                );
                                throw subjectException;
                            }
                            if (subjectException != null) {
                                throw subjectException;
                            }

                            if (referenceResult instanceof Matcher && subjectResult instanceof Matcher) return proxy;

                            if (referenceResult instanceof Pattern && subjectResult instanceof Pattern) return pattern;

                            if (referenceResult instanceof MatchResult && subjectResult instanceof MatchResult) {
                                FunctionalityEquivalencePatternFactory.assertEquals(
                                    (MatchResult) referenceResult,
                                    (MatchResult) subjectResult
                                );
                                return referenceResult;
                            }

                            if (referenceResult instanceof CharSequence && subjectResult instanceof CharSequence) {
                                Assert.assertEquals(
                                    method.toString(),
                                    referenceResult.toString(),
                                    subjectResult.toString()
                                );
                            } else {
                                if (!ObjectUtil.equals(referenceResult, subjectResult)) {
                                    Assert.assertEquals(
                                        (
                                            "regex="
                                            + PrettyPrinter.toJavaStringLiteral(regex)
                                            + ", subject="
                                            + PrettyPrinter.toJavaStringLiteral(subject)
                                            + ", method="
                                            + method.toString()
                                        ),
                                        referenceResult,
                                        subjectResult
                                    );
                                }
                            }

                            this.assertEqualState(
                                "Pattern "
                                + PrettyPrinter.toJavaStringLiteral(pattern.toString())
                                + ", "
                                + (flags == 0 ? "" : "flags=" + flags + ", ")
                                + "subject "
                                + PrettyPrinter.toJavaStringLiteral(subject)
                                + ", "
                                + method.getName()
                                + "() => "
                                + subjectResult
                            );

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
                                    Assert.assertEquals(
                                        message + ": group #" + i,
                                        "start=" + referenceMatcher.start(i) + ", end=" + referenceMatcher.end(i),
                                        "start=" + subjectMatcher.start(i)   + ", end=" + subjectMatcher.end(i)
                                    );
                                }

                                // Check the return value of "requireEnd()".
                                // JUR's "requireEnd()" method is notoriously buggy... test cases may set a system
                                // property "FIX_REQUIRE_END" to work around these bugs.
                                FunctionalityEquivalencePatternFactory.assertEqual(
                                    message + "; requireEnd(): ",
                                    referenceMatcher.requireEnd(),
                                    subjectMatcher.requireEnd(),
                                    "FIX_REQUIRE_END"
                                );
                            }

                            // Check the return value of "hitEnd()".
                            // JUR's "hitEnd()" method is notoriously buggy... test cases may set a system
                            // property "FIX_HIT_END" to work around these bugs.
                            FunctionalityEquivalencePatternFactory.assertEqual(
                                message + "; hitEnd(): ",
                                referenceMatcher.hitEnd(),
                                subjectMatcher.hitEnd(),
                                "FIX_HIT_END"
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

                Assert.assertArrayEquals(
                    input + " => expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual),
                    expected,
                    actual
                );

                return expected;
            }

            @Override public Predicate<String>
            asPredicate() {

                final Predicate<String> referencePredicate = referencePattern.asPredicate();
                final Predicate<String> subjectPredicate   = subjectPattern.asPredicate();

                return subject -> {
                    boolean expected = referencePredicate.test(subject);
                    boolean actual   = subjectPredicate.test(subject);
                    Assert.assertEquals(expected, actual);
                    return expected;
                };
            }

            @Override public
            Predicate<String>
            asMatchPredicate() {

                final Predicate<String> referenceMatchPredicate = referencePattern.asMatchPredicate();
                final Predicate<String> subjectMatchPredicate   = subjectPattern.asMatchPredicate();

                return subject -> {
                    boolean expected = referenceMatchPredicate.test(subject);
                    boolean actual   = subjectMatchPredicate.test(subject);
                    Assert.assertEquals(expected, actual);
                    return expected;
                };
            }

            @Override public Stream<String>
            splitAsStream(CharSequence input) {
                final Iterator<String> referenceIterator = referencePattern.splitAsStream(input).iterator();
                final Iterator<String> subjectIterator   = subjectPattern.splitAsStream(input).iterator();

                Iterator<String> matcherIterator = new Iterator<String>() {

                    @Override public String
                    next() {
                        String referenceNext = referenceIterator.next();
                        String subjectNext   = subjectIterator.next();
                        Assert.assertEquals(referenceNext, subjectNext);
                        return referenceNext;
                    }

                    @Override public boolean
                    hasNext() {
                        boolean referenceHasNext = referenceIterator.hasNext();
                        boolean subjectHasNext   = subjectIterator.hasNext();
                        Assert.assertEquals(referenceHasNext, subjectHasNext);
                        return referenceHasNext;
                    }
                };
                return StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(matcherIterator, Spliterator.ORDERED | Spliterator.NONNULL), // spliterator
                    false                                                                                            // parallel
                );
            }
        };
    }

    @Override public boolean
    matches(String regex, CharSequence input) { return this.compile(regex).matcher(input).matches(); }

    private static void
    assertEquals(MatchResult expected, MatchResult actual) {
        int groupCount = expected.groupCount();
        Assert.assertEquals(groupCount, actual.groupCount());

        Assert.assertEquals(expected.start(), actual.start());
        for (int i = 0; i <= groupCount; i++) {
            Assert.assertEquals(expected.start(i), actual.start(i));
        }

        Assert.assertEquals(expected.end(), actual.end());
        for (int i = 0; i < groupCount; i++) {
            Assert.assertEquals(expected.end(i), actual.end(i));
        }

        Assert.assertEquals(expected.group(), actual.group());
        for (int i = 0; i < groupCount; i++) {
            Assert.assertEquals(expected.group(i), actual.group(i));
        }
    }

    private static void
    assertEqual(String message, boolean expected, boolean actual, String propertyName) {

        String pv = System.getProperty(propertyName);
        if (pv == null) {
            Assert.assertEquals(
                message + " CONSIDER SETTING THE SYSTEM PROPERTY \"" + propertyName + "\"",
                expected,
                actual
            );
            return;
        }

        final boolean ev = pv.charAt(0) == 't';
        final boolean av = pv.charAt(1) == 't';

        int i;
        for (i = 2; i < pv.length() && Character.isWhitespace(pv.charAt(i)); i++);

        if (i == pv.length()) {
            System.clearProperty(propertyName);
        } else {
            System.setProperty(propertyName, pv.substring(i));
        }

        String msg = message + pv + " " + expected + "/" + actual;
        Assert.assertEquals(msg, ev, expected);
        Assert.assertEquals(msg, av, actual);
    }
}
