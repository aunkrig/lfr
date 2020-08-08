# Lightning-fast Regular Expressions

Lightning-fast Regular Expressions ("LFR") is a 99.9%-complete reimplementation of `java.util.regex` ("JUR") with better `match()` and `find()` performance. Yet the design is much cleaner and easier to understand and extend.

LFR is (successfully) tested against the official OpenJDK 8 regex regression test suite.

## Differences between LFR and JUR
  
### FUNCTIONAL DIFFERENCES
  
All features of JUR are available and functionally identical, except for the following differences:

Minus:

* `Pattern.CANON_EQ` (a really obscure, hopefully rarely used feature) is not implemented. You get an `IllegalArgumentException` when you invoke LFR `Pattern.compile()` with this flag.

Plus:

* Lookbehinds are no longer limited to fixed-length expressions.

* LFR's `Matcher.replaceFirst/All()` methods can not only replace with numered group (`$1`) or named group (`${name}`), but also with a Java-like expression; e.g.

  &nbsp;&nbsp;&nbsp;`PatternFactory.INSTANCE.compile("(?<grp>a)").matcher("abc").replaceAll("${3 + 4 + grp + m.groupCount()}")`

  returns

  &nbsp;&nbsp;&nbsp;`"7a1bc"`

The expression syntax is described [here](https://aunkrig.github.io/lfr/apidocs/de/unkrig/lfr/core/Matcher.html#compileReplacement-java.lang.String-).

### API DIFFERENCES

The classes `Pattern` and `Matcher` were duplicated from JUR (package `java.util.regex`) to LFR (package `de.unkrig.lfr.core`) with identical fields and methods.

The JUR `MatchResult` and `PatternSyntaxException` were re-used instead of being duplicated.

All static methods of the `Pattern` class (`compile()`, `matches()` and `quote()`) were moved to the new `PatternFactory` class. E.g., instead of "`Pattern.compile(...)`" you now have to write "`PatternFactory.INSTANCE.compile(...)`".

Analogously, the static method `Matcher.quoteReplacement()` was also moved to the `PatternFactory` class.

There are the following differences in the API:

Plus:

* The LFR `Pattern` class has an additional method `matches(CharSequence subject, int offset)`, which is particularly fast because it does not expose the `Matcher` and can thus save some overhead.

* The LFR `Pattern` class has an additional method `sequenceToString()` which returns a human-readable form of the compiled regex. For example, `compile("A.*abcdefghijklmnop", DOTALL).sequenceToString()` returns

  &nbsp;&nbsp;&nbsp;`'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=knuthMorrisPratt("abcdefghijklmnop"))`
  
  This is useful for testing how a regex compiled, and especially which optimizations have taken place.

* LFR only requires JRE 1.6+, but makes some later features available for earlier JREs.
  * JUR features that appeared in JRE 1.7:
    * `\x{h...h}` (code point escapes)
    * UNICODE scripts like `\p{IsLatin}`
    * UNICODE binary properties like `\p{IsAlphabetic}`
    * Named groups (e.g. `(?<name>X)`) and named group backreferences (e.g. `\k<name>`)
    * The `UNICODE_CHARACTER_CLASS` compilation flag, and its in-line equivalent `(?U)`
  * JUR features that appeared in JRE 1.8:
    * Additional predefined character classes: `\h`, `\v`, and their upper-case counterparts
    * The linebreak matcher `\R`
    * New binary property "Unicode Join Control"
  * JUR features that appeared in JRE 1.9:
    * Named Unicode characters, e.g. `\N{LATIN SMALL LETTER O}` (only if executed in a JRE 9+)
    * (Unicode extended graphemes -- whatever that may be -- are not (yet) supported.)
  * JUR features that appeared in JREs 10,11, 12 and 13:
    * (None.)

## Performance

Minus:

* Regex <em>compilation</em> performance was not measured and is probably much slower than JUR. There is surely a lot of room for optimization in this area, if someone needs it.

Plus:

* Regex <em>evaluation</em> (`Matcher.matches()`, `find()`, `lookingAt()`, ...) is roughly 30% faster than with JUR. This was measured with the LFR test case suite; other use cases (other regexes, other subjects, other API calls, ...) may yield different results.

* LFR drastically improves the evaluation performance for  the following special cases:

  * Patterns that start with 16 or more literal characters (for `Matcher.find()`)

  * Patterns that contain a greedy or reluctant quantifier of ANY, followed by 16 or more literal characters; e.g. `"xxx.*ABCDEFGHIJKLMNOPxxx"` or `"xxx.{4,39}?ABCDEFGHIJKLMNOPxxx"`

  * Patterns that contain a possessive quantifier of ANY; e.g. `"xxx.++xxx"`

  "ANY" means the "." pattern, and the DOTALL flag being active. ("." *without* the DOTALL flag being active means "any character except a line terminator".)

## Facade

If you want to switch between JUR and LFR (and other, not yet written RE implementations) at *runtime*, you can use "`de.unkrig.ref4j`", the "regular expressions facade for Java".

## Integration

All versions of LFR are available on [MAVEN CENTRAL](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.unkrig.lfr%22); download the latest JAR file from there, or add it as a MAVEN dependency.

JAVADOC can be found [here](https://aunkrig.github.io/lfr/apidocs/index.html).

## License

de.unkrig.lfr - A super-fast regular expression evaluator

Copyright (c) 2017, Arno Unkrig
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and thefollowing disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
