# Lightning-fast Regular Expressions

Lightning-fast Regular Expressions ("LFR") is a 99.9%-complete reimplementation of <code>java.util.regex</code> ("JUR") with better <code>match()</code> and <code>find()</code> performance. Yet the design is much cleaner and easier to understand and extend (only 5,400 LOC compared to 7,850 in JRE 8).

## Differences between LFR and JUR
  
### FUNCTIONAL DIFFERENCES
  
All features of JUR are available and functionally identical, except for the following differences:

Minus:

* <code>Pattern.CANON_EQ</code> (a really obscure, hopefully rarely used feature) is not implemented. You get an <code>IllegalArgumentException</code> when you invoke LFR <code>Pattern.compile()</code> with this flag.

* In a few, obscure cases, LFR <code>Matcher.hitEnd()</code> produces different results; to me it seems that the JUR implementation is buggy.

Plus:

* Lookbehinds are no longer limited to fixed-length expressions.

### API DIFFERENCES

The classes <code>Pattern</code> and <code>Matcher</code> were duplicated from JUR (package <code>java.util.regex</code> to LFR (package <code>de.unkrig.lfr.core</code>) with identical fields and methods.

The JUR <code>MatchResult</code> and <code>PatternSyntaxException</code> were re-used instead of being duplicated.

Because the LFR <code>Pattern</code> is an <em>interface</em> (as opposed to JUR, where it is a class), all its static methods were moved to the <code>PatternFactory</code> class. E.g., instead of "<code>Pattern.compile(...)</code>" you now have to write "<code>PatternFactory.INSTANCE.compile(...)</code>".

Because the LFR <code>Matcher</code> is an <em>interface</em> (as opposed to JUR, where it is a class), its static method <code>quoteReplacement()</code> was also moved to <code>PatternFactory</code>.

There are the following differences in the API:

Plus:

* The LFR <code>Matcher</code> has additional methods <code>hitStart()</code> and <code>requireStart()</code>, as counterparts for the <code>hit/requireEnd()</code> methods (useful for regexes with lookbehinds).

* The LFR <code>Pattern</code> interface has an additional method <code>matches(CharSequence subject, int offset)</code>, which is particularly fast because it does not expose the <code>Matcher</code> and can thus save some overhead.

* The LFR <code>Pattern</code> interface has an additional method <code>sequenceToString()</code> which returns a human-readable form of the compiled regex. For example, <code>compile("A.*abcdefghijklmnop", DOTALL).sequenceToString()</code> returns

  &nbsp;&nbsp;&nbsp;<code>'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=knuthMorrisPratt("abcdefghijklmnop")) . end</code>
  
  This is useful for testing how a regex compiled, and especially which optimizations have taken place.

* LFR can be used with JRE 1.6+, and makes some later features (like "named groups", available since JRE 1.7) available in earlier JREs.

## Performance

Minus:

* Regex <em>compilation</em> performance was not measured and is probably much slower than JUR. There is surely a lot of room for optimization in this aera, if someone needs it.

Plus:

* Regex <em>evaluation</em> (<code>Matcher.matches()</code>, <code>find()</code>, <code>lookingAt()</code>, ...) is roughly 30% faster than with JUR. This was measured with the LFR test case suite; other use cases (other regexes, other subjects, other API calls, ...) may yield different results.

* LFR drastically improves the evaluation performance for  the following special cases:

  * Patterns that start with 16 or more literal characters (for <code>Matcher.find()</code>)

  * Patterns that contain a greedy or reluctant quantifier of ANY, followed by 16 or more literal characters; e.g. <code>"xxx.*ABCDEFGHIJKLMNOPxxx"</code> or <code>"xxx.{4,39}?ABCDEFGHIJKLMNOPxxx"</code>

  * Patterns that contain a possessive quantifier of ANY; e.g. <code>"xxx.++xxx"</code>

  ("ANY" means the "." pattern, and the DOTALL flag being active.)

## Facade

If you want to switch between JUR and LFR at *runtime*, you can use "<code>de.unkrig.ref4j</code>", the "regular expressions facade for Java".

## Integration

All versions of LFR are available on [MAVEN CENTRAL](http://search.maven.org); download the latest JAR file from there, or add it as a dependency in MAVEN.

## License

de.unkrig.lfr - A super-fast regular expression evaluator

Copyright (c) 2017, Arno Unkrig
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and thefollowing disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
