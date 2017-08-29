
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.PatternSyntaxException;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.TransformerUtil;
import de.unkrig.commons.lang.protocol.TransformerWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.ref4j.Matcher;
import de.unkrig.ref4j.Pattern;
import de.unkrig.ref4j.PatternFactory;

public
class PerformanceMeasurementPatternFactory extends PatternFactory {

    private final PatternFactory[] delegates;

    public
    PerformanceMeasurementPatternFactory(PatternFactory... delegates) {
        this.delegates = delegates;
    }

    @Override public int
    getSupportedFlags() {
        int result = -1;
        for (PatternFactory pf : this.delegates) result &= pf.getSupportedFlags();
        return result;
    }

    static
    class Invocation {

        private final Method   method;
        private final Object[] arguments;

        Invocation(Method method, Object[] arguments) {
            this.method    = method;
            this.arguments = arguments;
        }

        @SuppressWarnings("unchecked") public <EX extends Throwable> Object
        invoke(Object target) throws EX {
            try {
                return this.method.invoke(target, this.arguments);
            } catch (InvocationTargetException e) {
                throw (EX) e.getTargetException();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }

    @Override public Pattern
    compile(final String regex, final int flags) throws PatternSyntaxException {

        StackTraceElement[] stes = new Throwable().getStackTrace();
        StackTraceElement   ste = null;
        for (int i = 1; i < stes.length; i++) {
            if (stes[i].getMethodName().startsWith("test")) {
                ste = stes[i];
                break;
            }
        }

        System.out.println();
        if (ste == null) {
            System.out.printf(Locale.US, "???%n");
        } else {
            System.out.printf(Locale.US, "%s%n", ste.getClassName() + "." + ste.getMethodName() + "():");
        }

        final Pattern[] patterns = new Pattern[this.delegates.length];
        for (int i = 0; i < this.delegates.length; i++) patterns[i] = this.delegates[i].compile(regex, flags);

        final List<Invocation> invocations = new ArrayList<Invocation>();

        return new Pattern() {

            @Override public String pattern() { return regex; }
            @Override public int    flags()   { return flags; }

            @Override public Matcher
            matcher(final CharSequence subject) {

                return (Matcher) Proxy.newProxyInstance(
                    Matcher.class.getClassLoader(),
                    new Class[] { Matcher.class },
                    new InvocationHandler() {

                        @Override @NotNullByDefault(false) public Object
                        invoke(Object proxy, final Method method, final Object[] arguments) throws Throwable {

                            final Invocation newInv = new Invocation(method, arguments);

                            final Object[] result = new Object[1];

                            PerformanceMeasurementPatternFactory.<Pattern, Matcher>probe(
                                method.getName() + "()",
                                patterns,                                                            // inputs
                                new TransformerWhichThrows<Pattern, Matcher, RuntimeException>() {   // prepare

                                    @Override @NotNullByDefault public Matcher
                                    transform(Pattern p) {

                                        // Create the Matcher object.
                                        Matcher m = p.matcher(subject);

                                        // "Replay" all method invocations on the Matcher.
                                        for (Invocation inv : invocations) {
                                            inv.<RuntimeException>invoke(m);
                                        }
                                        return m;
                                    }
                                },
                                new ConsumerWhichThrows<Matcher, RuntimeException>() {               // execute

                                    @Override @NotNullByDefault public void
                                    consume(Matcher m) { result[0] = newInv.<RuntimeException>invoke(m); }
                                }
                            );

                            invocations.add(newInv);

                            return result[0];
                        }
                    }
                );
            }

            @Override public boolean
            matches(final CharSequence subject, final int offset) {

                probe(
                    "Pattern.matches()",
                    patterns,
                    new ConsumerWhichThrows<Pattern, RuntimeException>() {
                        @Override public void consume(Pattern pattern) { pattern.matches(subject, offset); }
                    }
                );

                return PerformanceMeasurementPatternFactory.this.delegates[0].matches(regex, subject);
            }

            @Override public String[]
            split(CharSequence input) { throw new UnsupportedOperationException(); }

            @Override public String[]
            split(CharSequence input, int limit) { throw new UnsupportedOperationException(); }
        };
    }

    /**
     * Lets <var>execute</var> consume each <var>input</var> (many times), measures the user cpu times, and prints
     * them to {@code System.out}.
     */
    private static <T> void
    probe(String message, T[] inputs, final ConsumerWhichThrows<T, RuntimeException> execute) {
        probe(message, inputs, TransformerUtil.<T, T>identity(), execute);
    }

    /**
     * Transforms each of the <var>inputs</var> with the <var>prepare</var> transformer, then lets <var>execute</var>
     * consume each transformed <var>input</var> (many times), measures the user cpu times, and prints them to {@code
     * System.out}.
     */
    private static <T1, T2> void
    probe(
        String                                                 message,
        T1[]                                                   inputs,
        final TransformerWhichThrows<T1, T2, RuntimeException> prepare,
        final ConsumerWhichThrows<T2, RuntimeException>        execute
    ) {

        System.out.printf(Locale.US, "  %-25s ", message);

        double[] durations = new double[inputs.length];

        for (int i = 0; i < inputs.length; i++) {

            T1 input = inputs[i];

            final T2 transformedInput = prepare.transform(input);

            durations[i] = probe(new Runnable() {
                @Override public void run() { execute.consume(transformedInput); }
            });

            System.out.printf(Locale.US, "%,7.1f ns ", 1E9 * durations[i]);
            if (i > 0) {
                System.out.printf(Locale.US, "%5.0f%% ", 100 * durations[i] / durations[0]);
            }
        }
        System.out.println();
    }

    /**
     * @return How many seconds <em>one</em> invocation of the <var>runnable</var> takes
     */
    private static double
    probe(Runnable runnable) {

        // "ThreadMXBean.getCurrentThreadUserTime()" has only 15.6 ms resolution (MS WINDOWS 7); way too coarse for
        // our purposes.
//        ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
//        long beginning = tmxb.getCurrentThreadUserTime();

        // Calibrate.
        int reps = 1000;
        for (;;) {
            double duration = probe(runnable, reps);
            if (duration >= 0.020) break;
            reps *= 3;
        }

        // Make several measurements, and determine the minimum duration.
        double min = Double.MAX_VALUE;
        for (int i = 0; i < 10; i++) {
            double duration = probe(runnable, reps);
            if (duration < min) min = duration;
        }

        return min / reps;
    }

    /**
     * @return How many seconds <var>reps</var> invocations (!) of the <var>runnable</var> took
     */
    private static double
    probe(Runnable runnable, int reps) {

        long beginning = System.currentTimeMillis();
        {
            for (int i = 0; i < reps; i++) runnable.run();
        }
        long end = System.currentTimeMillis();

        return 1E-3 * (end - beginning);
    }

    @Override public boolean
    matches(String regex, CharSequence input) {
        // TODO Auto-generated method stub
        return false;
    }
}
