
/*
 * de.unkrig.lfr - A super-fast regular expression evaluator
 *
 * Copyright (c) 2019, Arno Unkrig
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

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.Parameterized.Parameters;

import de.unkrig.ref4j.PatternFactory;

public abstract
class ParameterizedWithPatternFactory {

    protected final PatternFactory patternFactory;
    
    public
    ParameterizedWithPatternFactory(PatternFactory patternFactory) { this.patternFactory = patternFactory; }

    @Parameters(name = "PatternFactory={1}") public static Iterable<Object[]>
    patternFactories() throws Exception {
        
        List<Object[]> result = new ArrayList<Object[]>();
        
        for (PatternFactory pf : new PatternFactory[] {
            de.unkrig.lfr.core.PatternFactory.INSTANCE,
            de.unkrig.ref4j.jur.PatternFactory.INSTANCE,
        }) result.add(new Object[] { pf, pf.getId() });

        return result;
    }
    
    public boolean
    isLfr() { return this.patternFactory.getId().equals("de.unkrig.lfr"); }
}
