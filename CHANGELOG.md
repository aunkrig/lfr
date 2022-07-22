### Version 1.2.0:

* Added missing JRE 8 methods to "Pattern" for JRE 8-17 compatibility.
* Gave up on JRE 6 compatibility - minimum JRE verseion is now 8.
* Fixed some scanning rules in "comment mode".
* Catch infinite quantities of zero-width operand (creates an endless look otherwise).
* Fixed the backtracking of "\r\n" sequences (may pose *two* (!) line breaks!).
* Fixed allowed characters in capturing group names.
