Lightning-fast Regular Expressions

LFR is a 99%-complete reimplementation of java.util.regex with better "match()" / "find()" performance. Yet the
design is much cleaner and easier to understand and extend (3300 LOC compared to 7850 in JRE 8).

Differences:

* FEATURES: All features of java.util.Regex ("JUR") are available and functionally identical, except for the following
  differences:
   (-) Pattern.CANON_EQ is not implemented (a really obscure, hopefully rarely used feature). You get an
       IllegalArgumentException when you invoke "Pattern.compile()" with this flag.
   (-) Unicode scripts (e.g. "\p{IsLatin}") are NOT implemented. All other Unicode character classes (blocks,
       categories and properties) ARE supported.
   (+) Lookbehinds are no longer limited to fixed-length expressions.
 * JUR compiles invalid back references (e.g. "\9" if there are less than 9 capturing groups) and treats them as "no
   match" when evaluated, while DULC throws a PatternSyntaxException at compile time.
 * Because d.u.l.c.Matcher is an INTERFACE (as opposed to j.u.r.Matcher), it cannot declare the static method
   "quoteReplacement()"; you'd have to use the JUR method instead.

* API: Classes Pattern and Matcher were duplicated from "java.util.regex" ("JUR") to "de.unkrig.lfr.core" ("DULC")
  with identical fields and methods. The j.u.r.MatchResult and the j.u.r.PatternSyntaxException were re-used instead of
  being duplicated.
  There are the following differences in the API:
   (+) The DULC Matcher has additional methods "hitStart()" and "requireStart()", as counterparts for the
       "hit/requireEnd()" methods (useful for regexes with lookbehinds).
   (+) The d.u.l.c.Pattern has an additional method "matches(CharSequence subject, int offset)" (not to be confused
       with the "static boolean matches(String regex, CharSequence input)" method), which is particularly fast because
       it does not expose the Matcher and can thus save some overhead.
   (-) In a few, obscure cases, "Matcher.hitEnd()" produces different results; to me it seems that the JUR
       implementation is buggy.

* Performance:
   (+) Regex EVALUATION ("Matcher.matches()", "find()", "lookingAt()", ...) is roughly 30% faster than with JUR. This
       was measured with the DULC test case suite; other use cases (other regexes, other subjects, other API calls,
       ...) may yield different results.
   (-) Regex COMPILATION was not measured and is probably much slower than JUR. There is surely a lot of room for
       optimization in this aera, if someone needs it.
   (+) DULC drastically improves the evaluation performance of the following special cases:
        * Patterns that start with 16 or more literal characters (only "Matcher.find()")
        * Patterns that contain a greedy or reluctant quantifier of ANY, followed by 16 or more literal characters;
          e.g. "xxx.*ABCDEFGHIJKLMNOPxxx" or "xxx.{4,39}?ABCDEFGHIJKLMNOPxxx"
        * Patterns that contain a possessive quantifier of ANY; e.g. "xxxA++xxx"
       "ANY" means the "." pattern, and the DOTALL flag being active.