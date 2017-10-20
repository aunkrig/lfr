
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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import de.unkrig.commons.io.Readers;
import de.unkrig.lfr.core.Pattern;

/**
 * A Java re-implementation of the <a href="http://sljit.sourceforge.net/regex_perf.html">Performance comparison of
 * regular expression engines</a> of the SLJIT project.
 */
public
class PerformanceTests {

    /**
     * This test requires the "mtent12.txt" file, which can be found <a
     * href="http://www.gutenberg.org/files/3200/old/mtent12.zip">here</a>.
     */
    @Test @SuppressWarnings("static-method") public void
    test() throws FileNotFoundException, IOException {

        System.out.printf("Regex:                                    JUR [ns]:       LFR [ns]:         Sequence:%n");

        String s = Readers.readAll(new FileReader("../regex-test/mtent12.txt"), true);

        PerformanceTests.findAll("Twain",                               s);
        PerformanceTests.findAll("(?i)Twain",                           s);
        PerformanceTests.findAll("[a-z]shing",                          s);
        PerformanceTests.findAll("Huck[a-zA-Z]+|Saw[a-zA-Z]+",          s);
        PerformanceTests.findAll("\\b\\w+nn\\b",                        s);
        PerformanceTests.findAll("[a-q][^u-z]{13}x",                    s);
        PerformanceTests.findAll("Tom|Sawyer|Huckleberry|Finn",         s);
        PerformanceTests.findAll("(?i)Tom|Sawyer|Huckleberry|Finn",     s);
        PerformanceTests.findAll("(Tom|Sawyer|Huckleberry|Finn)",       s);
        PerformanceTests.findAll(".{0,2}(Tom|Sawyer|Huckleberry|Finn)", s);
        PerformanceTests.findAll(".{2,4}(Tom|Sawyer|Huckleberry|Finn)", s);
        PerformanceTests.findAll("Tom.{10,25}river|river.{10,25}Tom",   s);
        PerformanceTests.findAll("[a-zA-Z]+ing",                        s);
        PerformanceTests.findAll("\\s[a-zA-Z]{0,12}ing\\s",             s);
        PerformanceTests.findAll("([A-Za-z]awyer|[A-Za-z]inn)\\s",      s);
        PerformanceTests.findAll("[\"'][^\"']{0,30}[?!\\.][\"']",       s);
    }

    private static void
    findAll(String regex, String subject) {

        // Measure JUR.
        long nsJur;
        {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(subject);

            // Warm up.
            while (m.find());
            m.reset(subject);
            while (m.find());
            m.reset(subject);
            while (m.find());

            m.reset(subject);
            long start = System.nanoTime();
            {
                while (m.find());
            }
            nsJur = System.nanoTime() - start;
        }

        // Measure LFR.
        long   nsLfr;
        String seq;
        {
            Pattern pattern = de.unkrig.lfr.core.PatternFactory.INSTANCE.compile(regex);
            seq = pattern.sequenceToString();
            de.unkrig.lfr.core.Matcher m = pattern.matcher(subject);

            // Warm up.
            while (m.find());
            m.reset(subject);
            while (m.find());
            m.reset(subject);
            while (m.find());

            m.reset(subject);
            long start = System.nanoTime();
            {
                while (m.find());
            }
            nsLfr = System.nanoTime() - start;
        }

        System.out.printf("%-35s %,15d %,15d %6.2f%% %s%n", regex, nsJur, nsLfr, 100. * nsLfr / nsJur, seq);
    }
}
