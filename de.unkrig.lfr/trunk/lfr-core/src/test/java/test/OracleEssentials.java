
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

import org.junit.Assert;

/**
 * @author Arno
 *
 */
public class OracleEssentials {

    private static final boolean ALSO_COMPARE_PERFORMANCE = true;

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

            Assert.assertEquals(message + ", lookingAt()", matcher1.lookingAt(), matcher2.lookingAt());
            OracleEssentials.assertEqualState(message + ", lookingAt()", matcher1, matcher2);
            matcher1.reset();
            matcher2.reset();

            Assert.assertEquals(message + ", matches()", matcher1.matches(), matcher2.matches());
            OracleEssentials.assertEqualState(message + ", matches()", matcher1, matcher2);
            matcher1.reset();
            matcher2.reset();

            for (int matchCount = 0;; matchCount++) {
                String message2 = message + ", Match #" + (matchCount + 1);

                boolean found1 = matcher1.find();
                boolean found2 = matcher2.find();
                Assert.assertEquals(message2 + ", find()", found1, found2);

                if (!found1 || !found2) break;

                OracleEssentials.assertEqualStateAfterMatch(message2, matcher1, matcher2);
            }

            OracleEssentials.assertEqualState(message, matcher1, matcher2);
        }

        if (OracleEssentials.ALSO_COMPARE_PERFORMANCE) {

            long ms1 = 0, ms2 = 0;
            int N2 = 100000;
            int N1 = 30000;
            {
                long start = 0;
                for (int i = 0; i < N2; i++) {

                    if (i == N1) start = System.currentTimeMillis();

                    for (java.util.regex.Matcher m = pattern1.matcher(subject); m.find();) {
                        m.group();
                        m.start();
                        m.end();
                    }
                }
                ms1 = System.currentTimeMillis() - start;
            }
            {
                long start = 0;
                for (int i = 0; i < N2; i++) {

                    if (i == N1) start = System.currentTimeMillis();

                    for (de.unkrig.lfr.core.Pattern.Matcher m = pattern2.matcher(subject); m.find();) {
                        m.group();
                        m.start();
                        m.end();
                    }
                }
                ms2 = System.currentTimeMillis() - start;
            }

            double gain = (double) ms1 / ms2;

            OracleEssentials.gainSum     += gain;
            OracleEssentials.gainProduct *= gain;
            OracleEssentials.totalCount++;

            System.out.printf("%-15s %-20s %6d %6d %6.0f%%%n", OracleEssentials.asJavaLiteral(regex), OracleEssentials.asJavaLiteral(subject), ms1, ms2, 100 * gain);
        }
    }

    private static String
    asJavaLiteral(String s) {

        StringBuilder sb = new StringBuilder().append('"');

        for (char c : s.toCharArray()) {
            int idx;
            if ((idx = idx = "\r\n\b\t\\".indexOf(c)) != -1) {
                sb.append('\\').append("rnbt\\".charAt(idx));
            } else {
                sb.append(c);
            }
        }

        return sb.append('"').toString();
    }

    private static void
    assertEqualState(String message, java.util.regex.Matcher matcher1, de.unkrig.lfr.core.Pattern.Matcher matcher2) {

//        Assert.assertEquals(message + ", hitEnd()", matcher1.hitEnd(), matcher2.hitEnd());
    }

    private static void
    assertEqualStateAfterMatch(
        String                             message,
        java.util.regex.Matcher            matcher1,
        de.unkrig.lfr.core.Pattern.Matcher matcher2
    ) {

        OracleEssentials.assertEqualState(message, matcher1, matcher2);

        Assert.assertEquals(message + ", groupCount()", matcher1.groupCount(), matcher2.groupCount());

        for (int i = 0; i < matcher1.groupCount(); i++) {
            Assert.assertEquals(message + ", group(" + i + ")", matcher1.group(i), matcher2.group(i));
            Assert.assertEquals(message + ", start(" + i + ")", matcher1.start(i), matcher2.start(i));
            Assert.assertEquals(message + ", end(" + i + ")",   matcher1.end(i),   matcher2.end(i));
        }
    }

    public static void
    beginStatistics() {
        Assert.assertEquals(-1, OracleEssentials.totalCount);
        System.out.println("Regex           Subject           ms: jur.P dulc.P    Gain (>100% means dulc is faster)");
        System.out.println("---------------------------------------------------------------------------------------");
        OracleEssentials.gainSum     = 0;
        OracleEssentials.gainProduct = 1;
        OracleEssentials.totalCount  = 0;
    }

    public static void
    endStatistics() {
        Assert.assertNotEquals(-1, OracleEssentials.totalCount);
        System.out.println("------------------------------------------------------------------------------");
        System.out.printf(
            "Average gain:           %6.0f%%%n",
            100 * OracleEssentials.gainSum / OracleEssentials.totalCount
        );
        System.out.printf(
            "Geometric average gain: %6.0f%%%n",
            100 * Math.pow(OracleEssentials.gainProduct, 1. / OracleEssentials.totalCount)
        );
        System.out.println();

        OracleEssentials.totalCount = -1;
    }
}
