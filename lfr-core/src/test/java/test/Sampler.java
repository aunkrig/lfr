
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

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Taken from <a href="stackoverflow.com/questions/19850695">the "Sampler" class</a>.
 */
@NotNullByDefault(false) public
class Sampler {

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    private static String                   className, methodName;
    private static CallTree                 root;
    private static ScheduledExecutorService executor;

    public static synchronized void
    startSampling(String className, String methodName) {

        if (Sampler.executor != null) throw new IllegalStateException("sampling in progress");

        System.out.println("sampling started");
        Sampler.className    = className;
        Sampler.methodName   = methodName;

        Sampler.executor = Executors.newScheduledThreadPool(1);

        // "fixed delay" reduces overhead, "fixed rate" raises precision
        Sampler.executor.scheduleWithFixedDelay(new Runnable() {

            @Override public void
            run() { Sampler.newSample(); }
        }, 5/*150*/, 10/*75*/, TimeUnit.MILLISECONDS);
    }

    public static synchronized CallTree
    stopSampling() throws InterruptedException {

        if (Sampler.executor == null) throw new IllegalStateException("no sampling in progress");

        Sampler.executor.shutdown();
        Sampler.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        Sampler.executor = null;

        final CallTree result = Sampler.root;
        Sampler.root = null;
        return result;
    }

    public static void
    printCallTree(CallTree t) {
        if (t == null) {
            System.out.println("method not seen");
        } else {
            Sampler.printCallTree(t, "", 100);
        }
    }

    private static void
    printCallTree(CallTree t, String indent, long percent) {

        long num = 0;
        for (CallTree ch : t.values()) num += ch.count;
        if (num == 0) return;

        for (Map.Entry<String, CallTree> e : t.entrySet()) {
            String   key     = e.getKey();
            CallTree subtree = e.getValue();

            System.out.println(
                indent
                + (subtree.count * percent / num)
                + "% ("
                + (subtree.cpu * percent / num)
                + "% cpu) "
                + key
            );

            Sampler.printCallTree(subtree, indent + "  ", subtree.count * percent / num);
        }
    }

    static
    class CallTree extends HashMap<String, CallTree> {

        private static final long serialVersionUID = 1L;

        long count = 1, cpu;

        CallTree(boolean cpu) { if (cpu) this.cpu++; }

        CallTree
        getOrAdd(StackTraceElement ste, boolean cpu) {

            String key = ste.getClassName() + '.' + ste.getMethodName() + "():" + ste.getLineNumber();

            CallTree t = this.get(key);
            if (t != null) {
                t.count++;
                if (cpu) t.cpu++;
            } else {
                this.put(key, (t = new CallTree(cpu)));
            }

            return t;
        }
    }

    static void
    newSample() {

        for (ThreadInfo ti : Sampler.THREAD_MX_BEAN.dumpAllThreads(false, false)) {

            final boolean cpu = ti.getThreadState() == State.RUNNABLE;

            StackTraceElement[] stack = ti.getStackTrace();

            int ix;
            for (ix = stack.length - 1; ix >= 0; ix--) {
                StackTraceElement ste = stack[ix];

                if (ste.getClassName().equals(Sampler.className) && ste.getMethodName().equals(Sampler.methodName)) {
                    break;
                }
            }

            CallTree t = Sampler.root;
            if (t == null) Sampler.root = (t = new CallTree(cpu));

            for (ix--; ix >= 0; ix--) {
                StackTraceElement ste = stack[ix];
                t = t.getOrAdd(ste, cpu);
            }
        }
    }
}
