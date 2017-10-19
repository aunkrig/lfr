
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

/**
 * A Java re-implementation of the <a href="http://sljit.sourceforge.net/regex_perf.html">Performance comparison of
 * regular expression engines</a> of the SLJIT project.
 */
public
class PerformanceTests {

    @Test public void
    test() throws FileNotFoundException, IOException {

        System.out.printf("Regex:                                    JUR [ns]:       LFR [ns]:%n");

        String s = Readers.readAll(new FileReader("../regex-test/mtent12.txt"), true);

        this.findAll("Twain",                               s);
        this.findAll("(?i)Twain",                           s);
        this.findAll("[a-z]shing",                          s);
        this.findAll("Huck[a-zA-Z]+|Saw[a-zA-Z]+",          s);
        this.findAll("\\b\\w+nn\\b",                        s);
        this.findAll("[a-q][^u-z]{13}x",                    s);
        this.findAll("Tom|Sawyer|Huckleberry|Finn",         s);
        this.findAll("(?i)Tom|Sawyer|Huckleberry|Finn",     s);
        this.findAll(".{0,2}(Tom|Sawyer|Huckleberry|Finn)", s);
        this.findAll(".{2,4}(Tom|Sawyer|Huckleberry|Finn)", s);
        this.findAll("Tom.{10,25}river|river.{10,25}Tom",   s);
        this.findAll("[a-zA-Z]+ing",                        s);
        this.findAll("\\s[a-zA-Z]{0,12}ing\\s",             s);
        this.findAll("([A-Za-z]awyer|[A-Za-z]inn)\\s",      s);
        this.findAll("[\"'][^\"']{0,30}[?!\\.][\"']",       s);
    }

    private void
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
        long nsLfr;
        {
            de.unkrig.lfr.core.Matcher m = de.unkrig.lfr.core.PatternFactory.INSTANCE.compile(regex).matcher(subject);

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

        System.out.printf("%-35s %,15d %,15d %6.2f%%%n", regex, nsJur, nsLfr, 100. * nsLfr / nsJur);
    }
}
