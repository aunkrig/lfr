
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

import java.util.Arrays;
import java.util.Locale;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.NoException;
import de.unkrig.commons.lang.protocol.TransformerUtil;
import de.unkrig.commons.lang.protocol.TransformerWhichThrows;

/**
 * A framework for reliable measurement of the CPU time consumed by a piece of code.
 */
public final
class PerformanceMeasurement {

    private PerformanceMeasurement() {}

    private static final int REP2 = 1000;

    /**
     * Lets <var>execute</var> consume each <var>input</var> (many times), measures the user cpu times, and prints
     * them to {@code System.out}.
     */
    public static <T> void
    probe(String message, T[] inputs, final ConsumerWhichThrows<T, RuntimeException> execute) {
        PerformanceMeasurement.probe(message, inputs, TransformerUtil.<T, T, NoException>identity(), execute);
    }

    /**
     * Transforms each of the <var>inputs</var> with the <var>prepare</var> transformer, then lets <var>execute</var>
     * consume each transformed <var>input</var> (many times), measures the user cpu times, and prints them to {@code
     * System.out}.
     */
    public static <T1, T2> void
    probe(
        String                                                           message,
        T1[]                                                             inputs,
        final TransformerWhichThrows<T1, T2, ? extends RuntimeException> prepare,
        final ConsumerWhichThrows<T2, ? extends RuntimeException>        execute
    ) {

        System.out.printf(Locale.US, "  %-25s ", message);

        double[][] vals = new double[inputs.length][10];
        double[]   avgs = new double[inputs.length];
        for (int j = 0; j < 20000; j++) {

            int mod = j % 100;

            double[] durations         = new double[inputs.length];
            boolean  allSigmasAreSmall = true;
            for (int k = 0; k < inputs.length; k++) {

                final T2 transformedInput = prepare.transform(inputs[k]);

//                durations[i] = probe(new Runnable() {
//                    @Override public void run() { execute.consume(transformedInput); }
//                });
                // Resolution of "System.nanoTime()": approx. 302 ns
                {
                    long start = System.nanoTime();
                    for (int i = 0; i < REP2; i++) execute.consume(transformedInput);
                    long end = System.nanoTime();

                    durations[k] = 1E-9 * (end - start) / REP2;
                }

                if (mod < vals[k].length) {
                    vals[k][mod] = durations[k];
//                    System.out.printf(Locale.US, "%d %5d: %,9.3f ns%n", k, j, 1E9 * vals[k][mod]);
                }

                if (mod == vals[k].length && j >= 500) {

                    Arrays.sort(vals[k]);
                    double[] vals2 = Arrays.copyOf(vals[k], vals[k].length / 2);

                    double sum = 0;
                    for (int i = 0; i < vals2.length; i++) sum += vals2[i];

                    avgs[k] = sum / vals2.length;

                    double sigma;
                    {
                        double tmp = 0;
                        for (int i = 0; i < vals2.length; i++) tmp += sq(vals2[i] - avgs[k]);
                        sigma = Math.sqrt(tmp) / vals2.length / avgs[k];
                    }

                    if (sigma > 0.005) allSigmasAreSmall = false;
                } else {
                    allSigmasAreSmall = false;
                }
            }

            if (allSigmasAreSmall) break;
        }

        System.out.printf(Locale.US, "%,7.1f ns ", 1E9 * avgs[0]);
        for (int k = 1; k < avgs.length; k++) {
            System.out.printf(Locale.US, "%,7.1f ns %5.0f%% ", 1E9 * avgs[k], 100 * avgs[k] / avgs[0]);
        }
        System.out.printf("%n");
    }

    private static double
    sq(double x) { return x * x; }
}
