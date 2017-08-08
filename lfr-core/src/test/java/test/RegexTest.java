
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

import java.util.regex.PatternSyntaxException;

import org.junit.Assert;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Transformer;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;

public final
class RegexTest {

    /**
     * @see #assertMatchers(java.util.regex.Matcher, de.unkrig.lfr.core.Matcher)
     */
    interface MatcherAssertion {

        /**
         * @see #assertMatchers(java.util.regex.Matcher, de.unkrig.lfr.core.Matcher)
         */
        void assertMatchers(Matcher matcher1, Matcher matcher2);
    }

    public static final MatcherAssertion
    ASSERT_LOOKING_AT = new MatcherAssertion() {

        @Override public void
        assertMatchers(Matcher expected, Matcher actual) {
            Assert.assertEquals("lookingAt()", expected.lookingAt(), actual.lookingAt());
        }
    };

    public static final MatcherAssertion
    ASSERT_MATCHES = new MatcherAssertion() {

        @Override public void
        assertMatchers(Matcher expected, Matcher actual) {
            Assert.assertEquals("matches()", expected.matches(), actual.matches());
        }
    };

    public static final MatcherAssertion ASSERT_FIND = new MatcherAssertion() {

        @Override public void
        assertMatchers(Matcher expected, Matcher actual) {

            int matchCount;
            for (matchCount = 0;; matchCount++) {
                String message2 = "Pattern \"" + expected.pattern() + "\", Match #" + (matchCount + 1);

                boolean found1 = expected.find();
                boolean found2 = actual.find();
                Assert.assertEquals(message2 + ", find()", found1, found2);

                if (!found1 || !found2) break;

                RegexTest.ASSERT_EQUAL_STATE.assertMatchers(expected, actual);
            }
        }
    };

    /**
     * Whether this running JVM is 1.6.
     */
    private static final boolean IS_JAVA_6 = "1.6".equals(System.getProperty("java.specification.version"));

    /**
     * Whether this running JVM is 1.7.
     */
    private static final boolean IS_JAVA_7 = "1.7".equals(System.getProperty("java.specification.version"));

    private final String      regex;
    @Nullable private Integer flags;
    @Nullable private Boolean transparentBounds;
    @Nullable private Boolean anchoringBounds;
    @Nullable private Integer regionStart;
    private int               regionEnd;

    static { AssertionUtil.enableAssertionsForThisClass(); }

    public
    RegexTest(String regex) {
        this.regex = regex;
    }

    /**
     * Use the given <var>flags</var> to compile the regex.
     *
     * @param flags {@code null} means "don't use flags to compile the regex"
     */
    public void
    setFlags(@Nullable Integer flags) { this.flags = flags; }

    /**
     * @param transparentBounds {@code null} means "don't set the transparent bounds flag"
     */
    public void
    setTransparentBounds(@Nullable Boolean transparentBounds) { this.transparentBounds = transparentBounds; }

    /**
     * @param anchoringBounds {@code null} means "don't set the anchoring bounds flag"
     */
    public void
    setAnchoringBounds(@Nullable Boolean anchoringBounds) { this.anchoringBounds = anchoringBounds;   }

    /**
     * @param regionStart {@code null} means "don't set the region"
     */
    public void
    setRegion(@Nullable Integer regionStart, int regionEnd) {
        this.regionStart = regionStart;
        this.regionEnd   = regionEnd;
    }

    public void
    patternSyntaxException() {

        this.patternSyntaxExceptionJur();
        this.patternSyntaxExceptionDulc();
    }

    public void
    patternSyntaxExceptionJur() {

        // Ignore tests that use constructs that are not supported by java.util.regex.
        if (RegexTest.regexUsesFeatureNotSupportedByJur(this.regex)) return;

        try {
            if (this.flags != null) {
                java.util.regex.Pattern.compile(this.regex, this.flags);
            } else {
                java.util.regex.Pattern.compile(this.regex);
            }
            Assert.fail("JUR did not throw a PatternSyntaxException");
        } catch (PatternSyntaxException pse) {
            ;
        }
    }

    public void
    patternSyntaxExceptionDulc() {

        try {
            if (this.flags != null) {
                OracleEssentials.LFR.compile(this.regex, this.flags);
            } else {
                OracleEssentials.LFR.compile(this.regex);
            }
            Assert.fail("LFR did not throw a PatternSyntaxException");
        } catch (PatternSyntaxException pse) {
            ;
        }
    }

    /**
     * Compiles the regex to two patterns, sets up two matchers for the <var>subject</var>, and invokes the
     * <var>matcherAssertion</var>.
     *
     * @throws PatternSyntaxException One of the two regex compilations failed
     */
    public void
    assertMatchers(String subject, MatcherAssertion matcherAssertion) {

        // Ignore tests that use constructs that are not supported by java.util.regex.
        if (RegexTest.regexUsesFeatureNotSupportedByJur(this.regex)) return;

        final Pattern pattern1 = (
            this.flags != null
            ? OracleEssentials.JUR.compile(this.regex, this.flags)
            : OracleEssentials.JUR.compile(this.regex)
        );

        final Pattern pattern2 = (
            this.flags != null
            ? OracleEssentials.LFR.compile(this.regex, this.flags)
            : OracleEssentials.LFR.compile(this.regex)
        );

        // FUNCTIONAL tests (as opposed to PERFORMANCE testing).

        // Set up the matchers.
        Matcher matcher1 = pattern1.matcher(subject);
        Matcher matcher2 = pattern2.matcher(subject);

        {
            Boolean tb = this.transparentBounds;
            if (tb != null) {
                matcher1.useTransparentBounds(tb);
                matcher2.useTransparentBounds(tb);
            }
        }

        {
            Boolean ab = this.anchoringBounds;
            if (ab != null) {
                matcher1.useAnchoringBounds(ab);
                matcher2.useAnchoringBounds(ab);
            }
        }

        {
            Integer rs = this.regionStart;
            if (rs != null) {
                matcher1.region(rs, this.regionEnd);
                matcher2.region(rs, this.regionEnd);
            }
        }

        matcherAssertion.assertMatchers(matcher1, matcher2);

        RegexTest.ASSERT_EQUAL_STATE.assertMatchers(matcher1, matcher2);
    }

    private static boolean
    regexUsesFeatureNotSupportedByJur(String regex) {

        if (RegexTest.IS_JAVA_6 && ( // Java 6 doesn't support...
            regex.contains("(?U")                    // ... the "U" flag (Pattern.UNICODE_CHARACTER_CLASS)
            || regex.matches(".*\\( ?\\?<\\s*\\w.*") // ... named capturing groups
            || regex.contains("\\R")                 // ... the linebreak matcher
            || regex.contains("\\p{Is")              // ... UNICODE properties
        )) return true;

        if (RegexTest.IS_JAVA_7 && ( // Java 7 doesn't support...
            regex.contains("\\R")                    // ... the linebreak matcher
        )) return true;

        return false;
    }

    /**
     * Compiles the regex to two patterns, sets up two matchers for the <var>subject</var>, and invokes the
     * two transformers, and verifies that they return the same result (or throw the same exception).
     *
     * @throws PatternSyntaxException One of the two regex compilations failed
     */
    public <T> void
    assertEqual(
        String                        subject,
        final Transformer<Matcher, T> transformer1,
        final Transformer<Matcher, T> transformer2
    ) {
        this.assertMatchers(subject, new MatcherAssertion() {

            @Override public void
            assertMatchers(Matcher expected, Matcher actual) {

                T         result1;
                Exception exception1;
                try {
                    result1    = transformer1.transform(expected);
                    exception1 = null;
                } catch (Exception e) {
                    result1    = null;
                    exception1 = e;
                }

                T         result2;
                Exception exception2;
                try {
                    result2    = transformer2.transform(actual);
                    exception2 = null;
                } catch (Exception e) {
                    result2    = null;
                    exception2 = e;
                }

                if (exception1 == null && exception2 == null) {
                    Assert.assertEquals(result1, result2);
                } else
                if (exception1 != null && exception2 != null) {
                    Assert.assertEquals(exception1.getClass(), exception2.getClass());
                } else
                {
                    Assert.assertNotNull(exception1);
                    Assert.assertNotNull(exception2);
                }
            }
        });
    }

    public static final MatcherAssertion
    ASSERT_EQUAL_STATE = new MatcherAssertion() {

        @Override public void
        assertMatchers(Matcher expected, Matcher actual) {

            boolean hasMatch1;
            try {
                hasMatch1 = expected.group() != null;
            } catch (IllegalStateException ise) {
                hasMatch1 = false;
            }

            boolean hasMatch2;
            try {
                hasMatch2 = actual.group() != null;
            } catch (IllegalStateException ise) {
                hasMatch2 = false;
            }

            Assert.assertEquals(expected + "/" + actual + ": hasMatch", hasMatch1, hasMatch2);

            if (hasMatch1) {

                Assert.assertEquals("groupCount()", expected.groupCount(), actual.groupCount());

                for (int i = 0; i <= expected.groupCount(); i++) {

                    int start1 = expected.start(i);
                    int start2 = actual.start(i);
                    Assert.assertEquals("start(" + i + ")", start1, start2);

                    int end1 = expected.end(i);
                    int end2 = actual.end(i);
                    Assert.assertEquals("end(" + i + ")", end1, end2);

                    String group1 = expected.group(i);
                    String group2 = actual.group(i);
                    Assert.assertEquals("group(" + i + ")", group1, group2);
                }

                Assert.assertEquals("requireEnd()", expected.requireEnd(), actual.requireEnd());
            }

            boolean hitEnd1 = expected.hitEnd();
            boolean hitEnd2 = actual.hitEnd();
            Assert.assertEquals("hitEnd()", hitEnd1, hitEnd2);
        }
    };
}
