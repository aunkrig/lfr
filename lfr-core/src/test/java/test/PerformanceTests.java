
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

import java.io.FileReader;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.unkrig.commons.io.Readers;
import de.unkrig.commons.lang.ObjectUtil;

/**
 * A Java re-implementation of the <a href="http://sljit.sourceforge.net/regex_perf.html">Performance comparison of
 * regular expression engines</a> of the SLJIT project.
 */
public
class PerformanceTests {

    private String subject = ObjectUtil.almostNull();

    @Before public void
    setUp() throws IOException {
        this.subject = Readers.readAll(new FileReader("../regex-test/mtent12.txt"), true);
    }

    /**
     * These tests require the "mtent12.txt" file, which can be found <a
     * href="http://www.gutenberg.org/files/3200/old/mtent12.zip">here</a>.
     */
    @Test public void test1()  { PerformanceTests.findAll("Twain",                               this.subject); }
    @Test public void test2()  { PerformanceTests.findAll("(?i)Twain",                           this.subject); }
    @Test public void test3()  { PerformanceTests.findAll("[a-z]shing",                          this.subject); }
    @Test public void test4()  { PerformanceTests.findAll("Huck[a-zA-Z]+|Saw[a-zA-Z]+",          this.subject); }
    @Test public void test5()  { PerformanceTests.findAll("\\b\\w+nn\\b",                        this.subject); }
    @Test public void test6()  { PerformanceTests.findAll("[a-q][^u-z]{13}x",                    this.subject); }
    @Test public void test7()  { PerformanceTests.findAll("Tom|Sawyer|Huckleberry|Finn",         this.subject); }
    @Test public void test8()  { PerformanceTests.findAll("(?i)Tom|Sawyer|Huckleberry|Finn",     this.subject); }
    @Test public void test8a() { PerformanceTests.findAll("(Tom|Sawyer|Huckleberry|Finn)",       this.subject); }
    @Test public void test9()  { PerformanceTests.findAll(".{0,2}(Tom|Sawyer|Huckleberry|Finn)", this.subject); }
    @Test public void test10() { PerformanceTests.findAll(".{2,4}(Tom|Sawyer|Huckleberry|Finn)", this.subject); }
    @Test public void test11() { PerformanceTests.findAll("Tom.{10,25}river|river.{10,25}Tom",   this.subject); }
    @Test public void test12() { PerformanceTests.findAll("[a-zA-Z]+ing",                        this.subject); }
    @Test public void test13() { PerformanceTests.findAll("\\s[a-zA-Z]{0,12}ing\\s",             this.subject); }
    @Test public void test14() { PerformanceTests.findAll("([A-Za-z]awyer|[A-Za-z]inn)\\s",      this.subject); }
    @Test public void test15() { PerformanceTests.findAll("[\"'][^\"']{0,30}[?!\\.][\"']",       this.subject); }

    private static void
    findAll(String regex, String subject) {

        System.out.printf("%n");
        System.out.printf("%s():%n", Thread.currentThread().getStackTrace()[2]);
        System.out.printf("  Regex:                                    JUR [ns]:       LFR [ns]:         Sequence:%n");

        java.util.regex.Matcher    jurMatcher = java.util.regex.Pattern.compile(regex).matcher("");

        de.unkrig.lfr.core.Matcher lfrMatcher = de.unkrig.lfr.core.PatternFactory.INSTANCE.compile(regex).matcher("");

        for (int i = 0; i < 7; i++) {

            long nsJur;
            {
                jurMatcher.reset(subject);
                long start = System.nanoTime();
                while (jurMatcher.find());
                nsJur = System.nanoTime() - start;
            }

            long nsLfr;
            {
                lfrMatcher.reset(subject);
                long start = System.nanoTime();
                while (lfrMatcher.find());
                nsLfr = System.nanoTime() - start;
            }

            System.out.printf(
                "  %-35s %,15d %,15d %6.2f%% %s%n",
                regex,
                nsJur,
                nsLfr,
                100. * nsLfr / nsJur,
                de.unkrig.lfr.core.PatternFactory.INSTANCE.compile(regex).sequenceToString()
            );
        }
    }
}
