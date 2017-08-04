
Lightning-fast Regular Expressions
==================================

Lightning-fast Regular Expressions ("LFR") is a 99%-complete reimplementation of java.util.regex ("JUR") with better
"match()" / "find()" performance. Yet the design is much cleaner and easier to understand and extend (only 3,300 LOC
compared to 7,850 in JRE 8).

Differences:

* FEATURES:
  All features of JUR are available and functionally identical, except for the following differences:
   (-) Pattern.CANON_EQ is not implemented (a really obscure, hopefully rarely used feature). You get an
       IllegalArgumentException when you invoke LFR "Pattern.compile()" with this flag.
   (-) Unicode scripts (e.g. "\p{IsLatin}") are NOT implemented. All other Unicode character classes (blocks,
       categories and properties) ARE supported.
   (-) In a few, obscure cases, LFR "Matcher.hitEnd()" produces different results; to me it seems that the JUR
       implementation is buggy.
   (-) Because the LFR "Matcher" is an INTERFACE (as opposed to JUR, where it is a class), it cannot declare the static
       method "quoteReplacement()"; you'd have to use the JUR method instead.
   (*) JUR compiles invalid back references (e.g. "\9" if there are less than 9 capturing groups) without an error and
       treats them as "no match" when evaluated, while LFR throws a PatternSyntaxException at compile time.
   (+) Lookbehinds are no longer limited to fixed-length expressions.

* API:
  Classes "Pattern" and "Matcher" were duplicated from JUR to LFR with identical fields and methods. The JUR
  "MatchResult" and "PatternSyntaxException" were re-used instead of being duplicated.
  There are the following differences in the API:
   (+) The LFR "Matcher" has additional methods "hitStart()" and "requireStart()", as counterparts for the
       "hit/requireEnd()" methods (useful for regexes with lookbehinds).
   (+) The LFR "Pattern" interface has an additional method "matches(CharSequence subject, int offset)", which is
       particularly fast because it does not expose the "Matcher" and can thus save some overhead.
   (+) The LFR "Pattern" interface has an additional method "sequenceToString()" which returns a human-readable form of
       the compiled regex, e.g. for "compile("A.*abcdefghijklmnop", DOTALL)":
           'A' . greedyQuantifierAnyChar(min=0, max=infinite, ls=knuthMorrisPratt("abcdefghijklmnop")) . end
       This is useful for testing how a regex compiled, and especially which optimizations have taken place.

* Performance:
   (-) Regex COMPILATION was not measured and is probably much slower than JUR. There is surely a lot of room for
       optimization in this aera, if someone needs it.
   (+) Regex EVALUATION ("Matcher.matches()", "find()", "lookingAt()", ...) is roughly 30% faster than with JUR. This
       was measured with the LFR test case suite; other use cases (other regexes, other subjects, other API calls,
       ...) may yield different results.
   (+) LFR drastically improves the evaluation performance of the following special cases:
        * Patterns that start with 16 or more literal characters (for "Matcher.find()")
        * Patterns that contain a greedy or reluctant quantifier of ANY, followed by 16 or more literal characters;
          e.g. "xxx.*ABCDEFGHIJKLMNOPxxx" or "xxx.{4,39}?ABCDEFGHIJKLMNOPxxx"
        * Patterns that contain a possessive quantifier of ANY; e.g. "xxxA++xxx"
       ("ANY" means the "." pattern, and the DOTALL flag being active.)
