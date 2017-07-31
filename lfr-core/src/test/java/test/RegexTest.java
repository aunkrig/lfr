
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

public final
class RegexTest {

    /**
     * @see #assertMatchers(java.util.regex.Matcher, de.unkrig.lfr.core.Matcher)
     */
    interface MatcherAssertion {

        /**
         * @see #assertMatchers(java.util.regex.Matcher, de.unkrig.lfr.core.Matcher)
         */
        void assertMatchers(java.util.regex.Matcher matcher1, de.unkrig.lfr.core.Matcher matcher2);
    }

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
        if (regexUsesFeatureNotSupportedByJur(this.regex)) return;

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
                de.unkrig.lfr.core.Pattern.compile(this.regex, this.flags);
            } else {
                de.unkrig.lfr.core.Pattern.compile(this.regex);
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
        if (regexUsesFeatureNotSupportedByJur(this.regex)) return;

        final java.util.regex.Pattern pattern1 = (
            this.flags != null
            ? java.util.regex.Pattern.compile(this.regex, this.flags)
            : java.util.regex.Pattern.compile(this.regex)
        );

        final de.unkrig.lfr.core.Pattern pattern2 = (
            this.flags != null
            ? de.unkrig.lfr.core.Pattern.compile(this.regex, this.flags)
            : de.unkrig.lfr.core.Pattern.compile(this.regex)
        );

        final String message = "regex=\"" + this.regex + "\", subject=\"" + subject + "\"";

        // FUNCTIONAL tests (as opposed to PERFORMANCE testing).

        // Set up the matchers.
        java.util.regex.Matcher    matcher1 = pattern1.matcher(subject);
        de.unkrig.lfr.core.Matcher matcher2 = pattern2.matcher(subject);

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

        OracleEssentials.assertEqualState(message, matcher1, matcher2);
    }

    private static boolean
    regexUsesFeatureNotSupportedByJur(String regex) {

        if (IS_JAVA_6 && ( // Java 6 doesn't support...
            regex.contains("(?U")                    // ... the "U" flag (Pattern.UNICODE_CHARACTER_CLASS)
            || regex.matches(".*\\( ?\\?<\\s*\\w.*") // ... named capturing groups
            || regex.contains("\\R")                 // ... the linebreak matcher
            || regex.contains("\\p{Is")              // ... UNICODE properties
        )) return true;

        if (IS_JAVA_7 && ( // Java 7 doesn't support...
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
        String                                           subject,
        final Transformer<java.util.regex.Matcher, T>    transformer1,
        final Transformer<de.unkrig.lfr.core.Matcher, T> transformer2
    ) {
        this.assertMatchers(subject, new MatcherAssertion() {

            @Override public void
            assertMatchers(java.util.regex.Matcher matcher1, de.unkrig.lfr.core.Matcher matcher2) {

                T         result1;
                Exception exception1;
                try {
                    result1    = transformer1.transform(matcher1);
                    exception1 = null;
                } catch (Exception e) {
                    result1    = null;
                    exception1 = e;
                }

                T         result2;
                Exception exception2;
                try {
                    result2    = transformer2.transform(matcher2);
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
}
