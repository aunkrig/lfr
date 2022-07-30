
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
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
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

    @Override public String
    getId() { throw new UnsupportedOperationException(); }

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
        StackTraceElement   ste  = null;
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
        System.out.printf(Locale.US, "                                  JUR:       LFR:%n");

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

                            invocations.add(new Invocation(method, arguments));

                            final Object[] result = new Object[1];

                            PerformanceMeasurement.probe(
                                method.getName() + "()",
                                patterns,                                                            // inputs
                                new TransformerWhichThrows<Pattern, Matcher, RuntimeException>() {   // prepare

                                    @Override @NotNullByDefault public Matcher
                                    transform(Pattern p) {

                                        // Create the Matcher object.
                                        Matcher m = p.matcher(subject);
                                        return m;
                                    }
                                },
                                new ConsumerWhichThrows<Matcher, RuntimeException>() {               // execute

                                    @Override @NotNullByDefault public void
                                    consume(Matcher m) {
                                        m.reset(subject);

                                        // "Replay" all method invocations on the Matcher.
                                        for (Invocation inv : invocations) {
                                            result[0] = inv.<RuntimeException>invoke(m);
                                        }
                                    }
                                }
                            );

                            return result[0];
                        }
                    }
                );
            }

            @Override public boolean
            matches(CharSequence subject) {
                
                PerformanceMeasurement.probe(
                    "Pattern.matches()",
                    patterns,
                    new ConsumerWhichThrows<Pattern, RuntimeException>() {
                        @Override public void consume(Pattern pattern) { pattern.matches(subject); }
                    }
                );
                
                return PerformanceMeasurementPatternFactory.this.delegates[0].matches(regex, subject);
            }
            @Override public boolean
            matches(final CharSequence subject, final int regionStart) {

                PerformanceMeasurement.probe(
                    "Pattern.matches()",
                    patterns,
                    new ConsumerWhichThrows<Pattern, RuntimeException>() {
                        @Override public void consume(Pattern pattern) { pattern.matches(subject, regionStart); }
                    }
                );

                return PerformanceMeasurementPatternFactory.this.delegates[0].matches(regex, subject);
            }
            @Override public boolean
            matches(CharSequence subject, int regionStart, int regionEnd) {
                
                PerformanceMeasurement.probe(
                    "Pattern.matches()",
                    patterns,
                    new ConsumerWhichThrows<Pattern, RuntimeException>() {
                        @Override public void consume(Pattern pattern) { pattern.matches(subject, regionStart, regionEnd); }
                    }
                );
                
                return PerformanceMeasurementPatternFactory.this.delegates[0].matches(regex, subject);
            }

            @Override public String[]
            split(CharSequence input) { throw new UnsupportedOperationException(); }

            @Override public String[]
            split(CharSequence input, int limit) { throw new UnsupportedOperationException(); }

            @Override public Predicate<String>
            asPredicate() {

                PerformanceMeasurement.probe(
                    "Pattern.asPredicate()",
                    patterns,
                    new ConsumerWhichThrows<Pattern, RuntimeException>() {
                        @Override public void consume(Pattern pattern) { pattern.asPredicate(); }
                    }
                );

                return patterns[0].asPredicate();
            }

            @Override public Predicate<String>
            asMatchPredicate() {

                PerformanceMeasurement.probe(
                    "Pattern.asMatchPredicate()",
                    patterns,
                    new ConsumerWhichThrows<Pattern, RuntimeException>() {
                        @Override public void consume(Pattern pattern) { pattern.asMatchPredicate(); }
                    }
                );

                return patterns[0].asMatchPredicate();
            }

            @Override public Stream<String>
            splitAsStream(final CharSequence input) {

                PerformanceMeasurement.probe(
                    "Pattern.splitAsStream()",
                    patterns,
                    new ConsumerWhichThrows<Pattern, RuntimeException>() {
                        @Override public void consume(Pattern pattern) { pattern.splitAsStream(input); }
                    }
                );

                return patterns[0].splitAsStream(input);
            }
        };
    }

    @Override public boolean
    matches(String regex, CharSequence input) {
        // TODO Auto-generated method stub
        return false;
    }
}
