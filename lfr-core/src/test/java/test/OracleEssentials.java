
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

import java.util.Locale;

import org.junit.Assert;

public final
class OracleEssentials {

    private OracleEssentials() {}

    private static final boolean ALSO_COMPARE_PERFORMANCE = true;
    private static final int     CHUNK_COUNT              = 50;
    private static final int     CHUNK_SIZE               = 5000;

    private static final Locale LOCALE = Locale.US;

    private static long   totalNs1, totalNs2;
    private static double gainSum;
    private static double gainProduct;
    private static int    totalCount = -1;

    static void
    harness(String regex, String subject) { OracleEssentials.harness(regex, subject, 0); }

    static void
    harness(String regex, String subject, int flags) {

        java.util.regex.Pattern    pattern1 = java.util.regex.Pattern.compile(regex, flags);
        de.unkrig.lfr.core.Pattern pattern2 = de.unkrig.lfr.core.Pattern.compile(regex, flags);

        String message = "regex=\"" + regex + "\", subject=\"" + subject + "\"";

        {
            java.util.regex.Matcher            matcher1 = pattern1.matcher(subject);
            de.unkrig.lfr.core.Pattern.Matcher matcher2 = pattern2.matcher(subject);

            {
                boolean lookingAt1 = matcher1.lookingAt();
                boolean lookingAt2 = matcher2.lookingAt();
                if (lookingAt1) {
                    String s = matcher1.group();
                    s.hashCode();
                }
                Assert.assertEquals(message + ", lookingAt()", lookingAt1, lookingAt2);

                if (lookingAt1) {
                    OracleEssentials.assertEqualStateAfterMatch(message + ", lookingAt()", matcher1, matcher2);
                } else {
                    OracleEssentials.assertEqualState(message + ", lookingAt()", matcher1, matcher2);
                }

                matcher1.reset();
                matcher2.reset();
            }

            {
                boolean matches1 = matcher1.matches();
                boolean matches2 = matcher2.matches();
                Assert.assertEquals(message + ", matches()", matches1, matches2);

                if (matches1) {
                    OracleEssentials.assertEqualStateAfterMatch(message + ", matches()", matcher1, matcher2);
                } else {
                    OracleEssentials.assertEqualState(message + ", matches()", matcher1, matcher2);
                }

                matcher1.reset();
                matcher2.reset();
            }

            int matchCount;
            for (matchCount = 0;; matchCount++) {
                String message2 = message + ", Match #" + (matchCount + 1);

                boolean found1 = matcher1.find();
                boolean found2 = matcher2.find();
                Assert.assertEquals(message2 + ", find()", found1, found2);

                if (!found1 || !found2) break;

                OracleEssentials.assertEqualStateAfterMatch(message2, matcher1, matcher2);
            }

            OracleEssentials.assertEqualState(message + ", after " + matchCount + " matches", matcher1, matcher2);
        }

        if (OracleEssentials.ALSO_COMPARE_PERFORMANCE) {

            long ns1 = Long.MAX_VALUE;
            for (int j = 0; j < OracleEssentials.CHUNK_COUNT; j++) {

                long start = System.nanoTime();
                for (int i = 0; i < OracleEssentials.CHUNK_SIZE; i++) {

                    for (java.util.regex.Matcher m = pattern1.matcher(subject); m.find();) {
                        m.group();
                        m.start();
                        m.end();
                    }
                }
                long ns = System.nanoTime() - start;

                if (ns < ns1) ns1 = ns;
            }

            long ns2 = Long.MAX_VALUE;
            for (int j = 0; j < OracleEssentials.CHUNK_COUNT; j++) {

                long start = System.nanoTime();
                for (int i = 0; i < OracleEssentials.CHUNK_SIZE; i++) {

                    for (de.unkrig.lfr.core.Pattern.Matcher m = pattern2.matcher(subject); m.find();) {
                        m.group();
                        m.start();
                        m.end();
                    }
                }
                long ns = System.nanoTime() - start;

                if (ns < ns2) ns2 = ns;
            }

            final double gain = (double) ns1 / ns2;

            OracleEssentials.totalNs1    += ns1;
            OracleEssentials.totalNs2    += ns2;
            OracleEssentials.gainSum     += gain;
            OracleEssentials.gainProduct *= gain;
            OracleEssentials.totalCount++;

            double cs = OracleEssentials.CHUNK_SIZE;

            System.out.printf(
                OracleEssentials.LOCALE,
                "%-15s %-21s %,8.1f %,8.1f %4.0f%%%n",
                OracleEssentials.asJavaLiteral(regex),
                OracleEssentials.asJavaLiteral(subject),
                ns1 / cs,
                ns2 / cs,
                100 * gain
            );
        }
    }

    private static String
    asJavaLiteral(String s) {

        StringBuilder sb = new StringBuilder().append('"');

        for (char c : s.toCharArray()) {
            int idx;
            if ((idx = "\r\n\b\t\\".indexOf(c)) != -1) {
                sb.append('\\').append("rnbt\\".charAt(idx));
            } else {
                sb.append(c);
            }
        }

        return sb.append('"').toString();
    }

    static void
    assertEqualState(String message, java.util.regex.Matcher matcher1, de.unkrig.lfr.core.Pattern.Matcher matcher2) {

        boolean hitEnd1 = matcher1.hitEnd();
        boolean hitEnd2 = matcher2.hitEnd();
        Assert.assertEquals(message + ", hitEnd()", hitEnd1, hitEnd2);
    }

    static void
    assertEqualStateAfterMatch(
        String                             message,
        java.util.regex.Matcher            matcher1,
        de.unkrig.lfr.core.Pattern.Matcher matcher2
    ) {

        Assert.assertEquals(message + ", groupCount()", matcher1.groupCount(), matcher2.groupCount());

        for (int i = 0; i <= matcher1.groupCount(); i++) {

            String group1 = matcher1.group(i);
            String group2 = matcher2.group(i);
            Assert.assertEquals(message + ", group(" + i + ")", group1, group2);

            int start1 = matcher1.start(i);
            int start2 = matcher2.start(i);
            Assert.assertEquals(message + ", start(" + i + ")", start1, start2);

            int end1 = matcher1.end(i);
            Assert.assertEquals(message + ", end(" + i + ")", end1, matcher2.end(i));
        }

        OracleEssentials.assertEqualState(message, matcher1, matcher2);
    }

    public static void
    beginStatistics() {
        Assert.assertEquals(-1, OracleEssentials.totalCount);
        System.out.println("Regex           Subject                jur[ns] dulc[ns]  Performance gain");
        System.out.println("-------------------------------------------------------------------------");
        OracleEssentials.totalNs1    = 0;
        OracleEssentials.totalNs2    = 0;
        OracleEssentials.gainSum     = 0;
        OracleEssentials.gainProduct = 1;
        OracleEssentials.totalCount  = 0;
    }

    public static void
    endStatistics() {
        Assert.assertNotEquals(-1, OracleEssentials.totalCount);
        System.out.println("-------------------------------------------------------------------------");
        System.out.printf(
            OracleEssentials.LOCALE,
            "                                      %,8.1f %,8.1f%n",
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
