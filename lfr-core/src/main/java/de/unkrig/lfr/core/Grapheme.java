
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

package de.unkrig.lfr.core;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.OptionalMethods;
import de.unkrig.commons.lang.OptionalMethods.MethodWrapper2;

public final
class Grapheme {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private Grapheme() {}

    private static final MethodWrapper2<?, Boolean, Integer, Integer, RuntimeException>
    GRAPHEME__IS_BOUNDARY = OptionalMethods.get2(
        "Graphemes only available in Java 9+", // message
        null,                                  // classLoader
        "java.util.regex.Grapheme",            // declaringClassName
        "isBoundary",                          // methodName
        int.class,                             // parameterType1
        int.class                              // parameterType2
    );

    public static boolean
    isBoundary(int cp0, int cp1) {
        Boolean result = GRAPHEME__IS_BOUNDARY.invoke(null, cp0, cp1);
        assert result != null;
        return result;
    }
}
