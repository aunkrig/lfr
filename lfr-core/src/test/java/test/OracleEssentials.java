
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

import java.util.Arrays;
import java.util.Locale;

import org.junit.Assert;

import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;
import test.Sampler.CallTree;

public final
class OracleEssentials {

    private OracleEssentials() {}

    private static final boolean ALSO_COMPARE_PERFORMANCE = false;
    private static final boolean ALSO_DO_PROFILING        = false;
    private static final int     CHUNK_COUNT              = 100;
    private static final int     CHUNK_SIZE               = 1000;

    private static final Locale LOCALE = Locale.US;

    private static long   totalNs1, totalNs2;
    private static double gainSum;
    private static double gainProduct;
    private static int    totalCount = -1;

    public static final PatternFactory                    JUR = de.unkrig.ref4j.jur.PatternFactory.INSTANCE;
    public static final de.unkrig.lfr.core.PatternFactory LFR = de.unkrig.lfr.core.PatternFactory.INSTANCE;

    /**
     * Shorthand for "{@link #harnessFull(String, String, int, Integer, int, Boolean, Boolean) harness(regex, subject,
     * 0, null, 0, null, null)}".
     */
    static void
    harnessFull(String regex, String subject) { OracleEssentials.harnessFull(regex, subject, 0, null, 0, null, null); }

    /**
     * Shorthand for "{@link #harnessFull(String, String, int, Integer, int, Boolean, Boolean) harness(regex, subject,
     * flags, null, 0, null, null)}".
     */
    static void
    harnessFull(String regex, String subject, int flags) {
        OracleEssentials.harnessFull(regex, subject, flags, null, 0, null, null);
    }

    /**
     * Shorthand for "{@link #harnessFull(String, String, int, Integer, int, Boolean, Boolean) harness(regex, subject,
     * flags, regionStart, regionEnd, null, null)}".
     */
    static void
    harnessFull(String regex, String subject, int flags, int regionStart, int regionEnd) {
        OracleEssentials.harnessFull(regex, subject, flags, regionStart, regionEnd, null, null);
    }

    /**
     * Shorthand for "{@link #harnessFull(String, String, int, Integer, int, Boolean, Boolean) harness(regex, subject,
     * flags, regionStart, regionEnd, transparentBounds, null)}".
     */
    static void
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
    static void
    harnessFull(
        String            regex,
        final String      subject,
        int               flags,
        @Nullable Integer regionStart,
        int               regionEnd,
        @Nullable Boolean transparentBounds,
        @Nullable Boolean anchoringBounds
    ) {

        RegexTest rt = new RegexTest(regex);
        rt.setFlags(flags);
        rt.setRegion(regionStart, regionEnd);
        rt.setTransparentBounds(transparentBounds);
        rt.setAnchoringBounds(anchoringBounds);

        // Test "Matcher.lookingAt()".
        rt.assertMatchers(subject, RegexTest.ASSERT_LOOKING_AT);

        // Test "Matcher.matches()".
        rt.assertMatchers(subject, RegexTest.ASSERT_MATCHES);

        // Test "Matcher.find()".
        rt.assertMatchers(subject, RegexTest.ASSERT_FIND);

        if (OracleEssentials.ALSO_COMPARE_PERFORMANCE) {

            final Pattern pattern1 = OracleEssentials.JUR.compile(regex, flags);
            final Pattern pattern2 = OracleEssentials.LFR.compile(regex, flags);

            Runnable r1 = new Runnable() {

                @Override public void
                run() {

                    Matcher m = pattern1.matcher(subject);

                    while (m.find()) {
                        m.group();
                        m.start();
                        m.end();
                    }
                }
            };

            Runnable r2 = new Runnable() {

                @Override public void
                run() {

                    Matcher m = pattern2.matcher(subject);

                    while (m.find()) {
                        m.group();
                        m.start();
                        m.end();
                    }
                }
            };

            long ns1 = OracleEssentials.measure(r1);
            long ns2 = OracleEssentials.measure(r2);

            final double gain = (double) ns1 / ns2;

            OracleEssentials.totalNs1    += ns1;
            OracleEssentials.totalNs2    += ns2;
            OracleEssentials.gainSum     += gain;
            OracleEssentials.gainProduct *= gain;
            OracleEssentials.totalCount++;

            double cs = OracleEssentials.CHUNK_SIZE;

            System.out.printf(
                OracleEssentials.LOCALE,
                "%-20s %-24s %,8.1f %,8.1f %4.0f%%%n",
                OracleEssentials.asJavaLiteral(regex),
                OracleEssentials.asJavaLiteral(subject),
                ns1 / cs,
                ns2 / cs,
                100 * gain
            );
        }

        if (OracleEssentials.ALSO_DO_PROFILING) {

            final Pattern pattern2 = OracleEssentials.LFR.compile(regex, flags);

            Runnable r2 = new Runnable() {

                @Override public void
                run() {

                    Matcher m = pattern2.matcher(subject);

                    while (m.find()) {
                        m.group();
                        m.start();
                        m.end();
                    }
                }
            };

            long end = System.nanoTime() + 1000000000;
            Sampler.startSampling(OracleEssentials.class.getName(), "harness");

            do {
                for (int i = 0; i < 10000; i++) r2.run();
            } while (System.nanoTime() < end);

            CallTree t;
            try {
                t = Sampler.stopSampling();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            Sampler.printCallTree(t);
        }
    }

    public static long
    measure(Runnable r) {

        for (;;) {

            long[] realNs = new long[OracleEssentials.CHUNK_COUNT];
            for (int j = 0; j < OracleEssentials.CHUNK_COUNT; j++) {

                long startRealTime = System.nanoTime();
                {
                    for (int i = 0; i < OracleEssentials.CHUNK_SIZE; i++) {
                        r.run();
                    }
                }
                long endRealTime = System.nanoTime();

                realNs[j] = endRealTime - startRealTime;
            }

            Arrays.sort(realNs);

//            for (long x : realNs) {
//                System.out.printf("%8d %s%n", x, StringUtil.repeat((int) (10 * Math.log10(x)), '#'));
//            }

            double divergence = 100 * ((double) realNs[10] / realNs[0] - 1);
//            System.out.printf("Divergence: %6.1f%%%n", divergence);
//            return realNs[0];
            if (divergence <= 1) return realNs[0];
        }
//        long result = Long.MAX_VALUE;
//        for (long x : realNs) {
//            if (x < result) result = x;
//        }
//        return result;
    }

    private static String
    asJavaLiteral(String s) {

        StringBuilder sb = new StringBuilder().append('"');

        for (char c : s.toCharArray()) {
            int idx;
            if ((idx = "\r\n\b\t\\".indexOf(c)) != -1) {
                sb.append('\\').append("rnbt\\".charAt(idx));
            } else
            if (c > 255) {
                sb.append(String.format("\\u%04x", (int) c));
            } else
            {
                sb.append(c);
            }
        }

        return sb.append('"').toString();
    }

    public static void
    beginStatistics() {

        if (!OracleEssentials.ALSO_COMPARE_PERFORMANCE) return;

        Assert.assertEquals(-1, OracleEssentials.totalCount);
        System.out.println("Regex                Subject                   jur[ns] dulc[ns]  Performance gain");
        System.out.println("---------------------------------------------------------------------------------");
        OracleEssentials.totalNs1    = 0;
        OracleEssentials.totalNs2    = 0;
        OracleEssentials.gainSum     = 0;
        OracleEssentials.gainProduct = 1;
        OracleEssentials.totalCount  = 0;
    }

    public static void
    endStatistics() {

        if (!OracleEssentials.ALSO_COMPARE_PERFORMANCE) return;

        Assert.assertNotEquals(-1, OracleEssentials.totalCount);
        System.out.println("---------------------------------------------------------------------------------");
        System.out.printf(
            OracleEssentials.LOCALE,
            "                                              %,8.1f %,8.1f%n",
            OracleEssentials.totalNs1 / (double) OracleEssentials.CHUNK_SIZE,
            OracleEssentials.totalNs2 / (double) OracleEssentials.CHUNK_SIZE
        );
        System.out.printf(
            OracleEssentials.LOCALE,
            "Average gain:           %6.0f%%%n",
            100 * OracleEssentials.gainSum / OracleEssentials.totalCount
        );
        System.out.printf(
            OracleEssentials.LOCALE,
            "Geometric average gain: %6.0f%%%n",
            100 * Math.pow(OracleEssentials.gainProduct, 1. / OracleEssentials.totalCount)
        );
        System.out.println();

        OracleEssentials.totalCount = -1;
    }
}
