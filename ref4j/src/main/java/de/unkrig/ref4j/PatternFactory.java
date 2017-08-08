
/*
 * ref4j - Regular Expression Facade for Java
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

package de.unkrig.ref4j;

import java.util.Iterator;
import java.util.regex.PatternSyntaxException;

import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.EnhancedServiceLoader;

/**
 * A facade or abstraction for various regular expression frameworks.
 * <p>
 *   To get
 * </p>
 * <pre>
 * {@code ServiceLoader<PatternFactory> patternFactory = ServiceLoader.load(PatternFactory.class);}
 * </pre>
 */
public abstract
class PatternFactory {

    /**
     * Returns the pattern factory configured in this JVM.
     * <ul>
     *   <li>
     *     The class named by the system property "de.unkrig.ref4j.PatternFactory" (if the system property is set
     *     and the class is registered as a pattern factory)
     *   </li>
     *   <li>
     *     The first of all registered pattern factories on the classpath (if any)
     *   </li>
     *   <li>
     *     The {@code java.util.regex} pattern factory
     *   </li>
     * </ul>
     *
     * @see EnhancedServiceLoader#load(Class)
     */
    public static PatternFactory
    get() {

        String patternFactoryClassName = System.getProperty("de.unkrig.ref4j.PatternFactory");
        if (patternFactoryClassName != null) return PatternFactory.get(patternFactoryClassName);

        Iterator<PatternFactory> it = EnhancedServiceLoader.DEFAULT.load(PatternFactory.class).iterator();

        return it.hasNext() ? it.next() : de.unkrig.ref4j.jur.PatternFactory.INSTANCE;
    }

    /**
     * @return The class that has the given name and extends the {@link PatternFactory} class
     */
    @Nullable public static PatternFactory
    get(String fullyQualifiedClassName) {

        for (PatternFactory pf : EnhancedServiceLoader.DEFAULT.load(PatternFactory.class)) {
            if (pf.getClass().getName().equals(fullyQualifiedClassName)) return pf;
        }

        return null;
    }

    /**
     * @return The union of all flags supported by {@code this} engine.
     */
    public abstract int
    getSupportedFlags();

    /**
     * Compiles the given <var>regex<var> into a {@link Pattern} with the given flags.
     *
     * @param flags The or'ed compilation flag constants declared by {@link Pattern}
     * @see         java.util.regex.Pattern#compile(String)
     * @see         Pattern#CANON_EQ
     * @see         Pattern#CASE_INSENSITIVE
     * @see         Pattern#COMMENTS
     * @see         Pattern#DOTALL
     * @see         Pattern#LITERAL
     * @see         Pattern#MULTILINE
     * @see         Pattern#UNICODE_CASE
     * @see         Pattern#UNICODE_CHARACTER_CLASS
     * @see         Pattern#UNIX_LINES
     */
    public Pattern
    compile(String regex) throws PatternSyntaxException { return this.compile(regex, 0); }

    /**
     * Compiles the given <var>regex<var> into a {@link Pattern}.
     *
     * @throws IllegalArgumentException A flag was given that is not set in {@link #getSupportedFlags()}
     * @see                             java.util.regex.Pattern#compile(String, int)
     */
    public abstract Pattern
    compile(String regex, int flags) throws PatternSyntaxException;

    /**
     * @see java.util.regex.Pattern#matches(String, CharSequence)
     */
    public abstract boolean
    matches(String regex, CharSequence input);

    /**
     * @see java.util.regex.Pattern#quote(String)
     */
    public String
    quote(String s) { return java.util.regex.Pattern.quote(s); }
}
