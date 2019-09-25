
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

package test;

import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;

public final
class OracleEssentials {

    private OracleEssentials() {}

    /**
     * The {@code java.util.regex} pattern factory.
     */
    public static final PatternFactory
    JUR = de.unkrig.ref4j.jur.PatternFactory.INSTANCE;

    /**
     * The {@code LFR} pattern factory.
     */
    public static final de.unkrig.lfr.core.PatternFactory
    LFR = de.unkrig.lfr.core.PatternFactory.INSTANCE;

    /**
     * The pattern factory that verifies the functional equality of JUR and LFR.
     */
    public static final PatternFactory
    PF = new FunctionalityEquivalencePatternFactory(OracleEssentials.JUR, OracleEssentials.LFR);
//    PF = new PerformanceMeasurementPatternFactory(OracleEssentials.JUR, OracleEssentials.LFR);

    /**
     * Shorthand for "{@link #harnessFull(String, String, int, Integer, int, Boolean, Boolean) harness(regex, subject,
     * 0, null, 0, null, null)}".
     */
    public static void
    harnessFull(String regex, String subject) { OracleEssentials.harnessFull(regex, subject, 0, null, 0, null, null); }

    /**
     * Shorthand for "{@link #harnessFull(String, String, int, Integer, int, Boolean, Boolean) harness(regex, subject,
     * flags, null, 0, null, null)}".
     */
    public static void
    harnessFull(String regex, String subject, int flags) {
        OracleEssentials.harnessFull(regex, subject, flags, null, 0, null, null);
    }

    /**
     * Shorthand for "{@link #harnessFull(String, String, int, Integer, int, Boolean, Boolean) harness(regex, subject,
     * flags, regionStart, regionEnd, null, null)}".
     */
    public static void
    harnessFull(String regex, String subject, int flags, int regionStart, int regionEnd) {
        OracleEssentials.harnessFull(regex, subject, flags, regionStart, regionEnd, null, null);
    }

    /**
     * Shorthand for "{@link #harnessFull(String, String, int, Integer, int, Boolean, Boolean) harness(regex, subject,
     * flags, regionStart, regionEnd, transparentBounds, null)}".
     */
    public static void
    harnessFull(String regex, String subject, int flags, int regionStart, int regionEnd, boolean transparentBounds) {
        OracleEssentials.harnessFull(regex, subject, flags, regionStart, regionEnd, transparentBounds, null);
    }

    /**
     * Verifies that the <var>regex</var> and the <var>subject</var> yield exactly the same matches with
     * {@code java.util.regex} and {@code de.unkrig.lfr.core} regular expressions.
     *
     * @param flags             Regex compilation flags, see {@link java.util.regex.Pattern#compile(String, int)}
     * @param regionStart       Optional: The non-default region to use for the matcher; see {@link
     *                          java.util.regex.Matcher#region(int, int)}
     * @param regionEnd         Honored only when <var>regionStart</var> {@code != null}
     * @param transparentBounds Optional: Call {@link java.util.regex.Matcher#useTransparentBounds(boolean)}
     * @param anchoringBounds   Optional: Call {@link java.util.regex.Matcher#useAnchoringBounds(boolean)}
     */
    public static void
    harnessFull(
        String            regex,
        final String      subject,
        int               flags,
        @Nullable Integer regionStart,
        int               regionEnd,
        @Nullable Boolean transparentBounds,
        @Nullable Boolean anchoringBounds
    ) {

        Pattern pattern = OracleEssentials.PF.compile(regex, flags);

        Matcher m = pattern.matcher(subject);

        if (regionStart       != null) m.region(regionStart, regionEnd);
        if (transparentBounds != null) m.useTransparentBounds(transparentBounds);
        if (anchoringBounds   != null) m.useAnchoringBounds(anchoringBounds);

        m.lookingAt();

        m.matches();

        // "matches()", if unsuccessful, leaves the matcher in a very strange state: The next invocation of "find()"
        // (a few lines below), will will NOT start at the beginning of the region, but where "lookingAt()" left off!?
        // The simple workaround is to reset the matcher here.
        m.reset();

        int matchNumber = 1;
        try {
            while (m.find()) matchNumber++;
        } catch (RuntimeException re) {
            throw ExceptionUtil.wrap("find(): Match #" + matchNumber, re);
        } catch (Error e) { // SUPPRESS CHECKSTYLE IllegalCatch
            throw ExceptionUtil.wrap("find(): Match #" + matchNumber, e);
        }
    }
}
