
/*
 * de.unkrig.lfr - A super-fast regular expression evaluator
 *
 * Copyright (c) 2016, Arno Unkrig
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

// SUPPRESS CHECKSTYLE Javadoc:9999

package de.unkrig.lfr.core;

import static de.unkrig.lfr.core.Pattern.TokenType.CC_INTERSECTION;
import static de.unkrig.lfr.core.Pattern.TokenType.EITHER_OR;
import static de.unkrig.lfr.core.Pattern.TokenType.END_GROUP;
import static de.unkrig.lfr.core.Pattern.TokenType.RIGHT_BRACKET;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import de.unkrig.commons.lang.Characters;
import de.unkrig.commons.lang.protocol.Predicate;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.lang.protocol.ProducerWhichThrows;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.parser.AbstractParser;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.scanner.AbstractScanner.Token;
import de.unkrig.commons.text.scanner.ScanException;
import de.unkrig.lfr.core.Pattern.RegexScanner;
import de.unkrig.lfr.core.Pattern.ScannerState;
import de.unkrig.lfr.core.Pattern.TokenType;

/**
 * A drop-in replacement for {@link java.util.regex.Pattern}.
 */
public final
class PatternFactory extends de.unkrig.ref4j.PatternFactory {

    private PatternFactory() {}

    /**
     * The singleton {@link de.unkrig.ref4j.PatternFactory} that implements the LFR regex engine.
     */
    public static final PatternFactory INSTANCE = new PatternFactory();

    private static final Comparator<CharacterClass>
    COMPARE_BY_UPPER_BOUND = new Comparator<CharacterClass>() {

        @Override @NotNullByDefault(false) public int
        compare(CharacterClass cc1, CharacterClass cc2) { return cc1.upperBound() - cc2.lowerBound(); }
    };

    @Override public int
    getSupportedFlags() { return Pattern.SUPPORTED_FLAGS; }

    /**
     * @see java.util.regex.Pattern#compile(String)
     */
    @Override public Pattern
    compile(String regex) throws PatternSyntaxException { return this.compile(regex, 0); }

    /**
     * @see java.util.regex.Pattern#compile(String, int)
     */
    @Override public Pattern
    compile(String regex, int flags) throws PatternSyntaxException {

        if ((flags & ~Pattern.SUPPORTED_FLAGS) != 0) {
            throw new IllegalArgumentException("Unsupported flag " + (flags & ~Pattern.SUPPORTED_FLAGS));
        }

        // With the "LITERAL" flag, use the "literal scanner" instead of the normal regex scanner.
        RegexScanner rs = new RegexScanner(
            (flags & de.unkrig.ref4j.Pattern.LITERAL) == 0
            ? Pattern.REGEX_SCANNER
            : Pattern.LITERAL_SCANNER
        );

        // With the "COMMENTS" flag, start in the "_X" default state.
        if (
            (flags & (de.unkrig.ref4j.Pattern.LITERAL | de.unkrig.ref4j.Pattern.COMMENTS))
            == de.unkrig.ref4j.Pattern.COMMENTS
        ) rs.setCurrentState(ScannerState.DEFAULT_X);

        rs.setInput(regex);

        Sequence sequence;
        try {
            sequence = PatternFactory.parse(rs, flags);
        } catch (ParseException pe) {
            PatternSyntaxException pse = new PatternSyntaxException(pe.getMessage(), regex, rs.getOffset());
            pse.initCause(pe);
            throw pse;
        }

        return new Pattern(regex, flags, sequence, rs.groupCount, rs.namedGroups, rs.greatestQuantifierNesting);
    }

    /**
     * @see java.util.regex.Pattern#matches(String, CharSequence)
     */
    @Override public boolean
    matches(String regex, CharSequence input) { return this.compile(regex).matches(input, 0); }

    /**
     * Parses a regular expression into a {@link Sequence}.
     */
    private static Sequence
    parse(final RegexScanner rs, final int flags) throws ParseException {

        // Skip COMMENT tokens.
        ProducerWhichThrows<Token<TokenType>, ScanException>
        filteredRs = ProducerUtil.filter(rs, new Predicate<Token<TokenType>>() {
            @Override public boolean
            evaluate(Token<TokenType> subject) {
                return (
                    subject.type != TokenType.COMMENT
                    && subject.type != TokenType.QUOTATION_BEGIN
                    && subject.type != TokenType.QUOTATION_END
                );
            }
        });

        return new AbstractParser<TokenType>(filteredRs) {

            int currentFlags;
            { this.setCurrentFlags(flags); }

            void
            setCurrentFlags(int newFlags) {

                ScannerState currentState = rs.getCurrentState();
                if (currentState != ScannerState.DEFAULT && currentState != ScannerState.DEFAULT_X) {
                    throw new IllegalStateException("Cannot set flags in this state");
                }

                // Switch between COMMENTS mode and non-COMMENTS mode.
                if ((newFlags & (de.unkrig.ref4j.Pattern.LITERAL | de.unkrig.ref4j.Pattern.COMMENTS)) == de.unkrig.ref4j.Pattern.COMMENTS) {
                    rs.setCurrentState(ScannerState.DEFAULT_X);
                } else {
                    rs.setCurrentState(ScannerState.DEFAULT);
                }

                this.currentFlags = newFlags;
            }

            Sequence
            parse() throws ParseException {

                Sequence result = this.parseAlternatives();

                // Check for trailing garbage.
                this.eoi();

                return result;
            }

            private Sequence
            parseAlternatives() throws ParseException {

                Sequence op1 = this.parseSequence();
                if (!this.peekRead("|")) return op1;

                List<Sequence> alternatives = new ArrayList<Sequence>();
                alternatives.add(op1);
                alternatives.add(this.parseSequence());
                while (this.peekRead(EITHER_OR) != null) alternatives.add(this.parseSequence());
                return Sequences.alternatives(alternatives.toArray(new Sequence[alternatives.size()]));
            }

            private Sequence
            parseSequence() throws ParseException {

                if (this.peek(null, EITHER_OR, END_GROUP) != -1) return Sequences.TERMINAL;

                Sequence result = this.parseQuantified();
                if (this.peek(null, EITHER_OR, END_GROUP) != -1) return result;

                List<Sequence> tmp = new ArrayList<Sequence>();
                tmp.add(result);
                do {
                    tmp.add(this.parseQuantified());
                } while (this.peek(null, EITHER_OR, END_GROUP) == -1);

                result = Sequences.TERMINAL;
                for (int i = tmp.size() - 1; i >= 0; i--) {
                    result = tmp.get(i).concat(result);
                }
                return result;
            }

            private Sequence
            parseQuantified() throws ParseException {

                if (++rs.currentQuantifierNesting > rs.greatestQuantifierNesting) {
                    rs.greatestQuantifierNesting = rs.currentQuantifierNesting;
                }

                final Sequence op = this.parsePrimary();

                --rs.currentQuantifierNesting;

                Token<TokenType> t = this.peek();
                if (t == null) return op;

                switch (t.type) {

                case GREEDY_QUANTIFIER:
                case RELUCTANT_QUANTIFIER:
                case POSSESSIVE_QUANTIFIER:
                    this.read();

                    final int min, max;
                    switch (t.text.charAt(0)) {

                    case '?': min = 0; max = 1;                 break;
                    case '*': min = 0; max = Integer.MAX_VALUE; break;
                    case '+': min = 1; max = Integer.MAX_VALUE; break;

                    case '{':
                        {
                            min = Integer.parseInt(t.captured[0]);
                            max = (
                                t.captured[1] == null ? min :
                                t.captured[2] == null ? Integer.MAX_VALUE :
                                Integer.parseInt(t.captured[2])
                            );
                        }
                        break;

                    default:
                        throw new AssertionError(t);
                    }

                    switch (t.type) {

                    case GREEDY_QUANTIFIER:     return Sequences.quantifier(op, min, max, rs.currentQuantifierNesting, true);
                    case RELUCTANT_QUANTIFIER:  return Sequences.quantifier(op, min, max, rs.currentQuantifierNesting, false);
                    case POSSESSIVE_QUANTIFIER: return Sequences.possessiveQuantifier(op, min, max);

                    default:
                        throw new AssertionError(t);
                    }

                default:
                    return op;
                }
            }

            private Sequence
            parsePrimary() throws ParseException {

                {
                    CharacterClass result = this.parseOptionalCharacterClass();
                    if (result != null) return result;
                }

                Token<TokenType> token = this.peek();
                if (token == null) throw new ParseException("Unexpected end-of-input");

                String t = token.text;

                if (this.peekRead(TokenType.CC_ANY) != null) {
                    return (
                        (this.currentFlags & de.unkrig.ref4j.Pattern.DOTALL) != 0
                        ? new CharacterClasses.AnyCharacter()
                        : (this.currentFlags & de.unkrig.ref4j.Pattern.UNIX_LINES) != 0
                        ? CharacterClasses.negate(CharacterClasses.literalCharacter('\n'), ".")
                        : CharacterClasses.negate(CharacterClasses.lineBreakCharacter(), ".")
                    );
                }

                if (this.peekRead(TokenType.QUOTED_CHARACTER) != null) {
                    return CharacterClasses.literalCharacter(t.codePointAt(1));
                }

                if (this.peekRead(TokenType.CAPTURING_GROUP) != null) {
                    int      groupNumber = ++rs.groupCount;
                    Sequence result      = Sequences.capturingGroupStart(groupNumber);

                    int savedFlags = this.currentFlags;
                    {
                        result.concat(this.parseAlternatives());
                    }
                    this.setCurrentFlags(savedFlags);

                    result.concat(Sequences.capturingGroupEnd(groupNumber));
                    this.read(")");
                    return result;
                }

                if (this.peekRead(TokenType.NON_CAPTURING_GROUP) != null) {
                    final Sequence result = this.parseAlternatives();
                    this.read(")");
                    return result;
                }

                if (this.peekRead(TokenType.INDEPENDENT_NON_CAPTURING_GROUP) != null) {
                    List<Sequence> alternatives = new ArrayList<Sequence>();
                    alternatives.add(this.parseSequence());
                    while (this.peekRead(EITHER_OR) != null) alternatives.add(this.parseSequence());

                    this.read(")");

                    return Sequences.independentNonCapturingGroup(
                        alternatives.toArray(new Sequence[alternatives.size()])
                    );
                }

                if (this.peekRead(TokenType.MATCH_FLAGS_NON_CAPTURING_GROUP) != null) {
                    final Sequence result;

                    int savedFlags = this.currentFlags;
                    {
                        this.setCurrentFlags(this.parseFlags(this.currentFlags, t.substring(2, t.length() - 1)));
                        result = this.parseAlternatives();
                    }
                    this.setCurrentFlags(savedFlags);

                    this.read(")");

                    return result;
                }

                if (this.peekRead(TokenType.CAPTURING_GROUP_BACK_REFERENCE) != null) {

                    // Scanning backreferences is tricky, because it depends on the number of capturing groups: Thus,
                    // we scan as many digits as possible, and, if they turn out to be "too many", we convert the
                    // excess digits to a LiteralString.

                    String prefix = token.captured[0];
                    String suffix = "";

                    int groupNumber;
                    for (;;) {
                        groupNumber = Integer.parseInt(prefix);

                        if (groupNumber <= rs.groupCount) break;

                        // An invalid group number 1...9 results in a match failure (this fact is missing from the JUR
                        // documentation).
                        if (groupNumber <= 9) return CharacterClasses.FAIL;

                        // Move the last character of the prefix to the beginning of the suffix and retry.
                        suffix = prefix.charAt(prefix.length() - 1) + suffix;
                        prefix = prefix.substring(0, prefix.length() - 1);
                    }

                    Sequence result = this.capturingGroupBackReference(groupNumber);

                    if (!suffix.isEmpty()) result = result.concat(new Sequences.LiteralString(suffix));

                    return result;
                }

                if (this.peekRead(TokenType.BEGINNING_OF_LINE) != null) {
                    return (
                        (this.currentFlags & de.unkrig.ref4j.Pattern.MULTILINE) == 0
                        ? Sequences.beginningOfInput()
                        : (this.currentFlags & de.unkrig.ref4j.Pattern.UNIX_LINES) != 0
                        ? Sequences.beginningOfUnixLine()
                        : Sequences.beginningOfLine()
                    );
                }

                if (this.peekRead(TokenType.END_OF_LINE) != null) {
                    return (
                        (this.currentFlags & de.unkrig.ref4j.Pattern.MULTILINE) == 0

                        // The JRE documentation falsely states that, in non-MULTILINE mode, "$" evaluates to "\z"
                        // (end-of-input), however actually it evaluates to "\z" (end-of-input but for the final
                        // terminator, if any). So for the sake of JUR compatibility, we implement this wrong
                        // behavior:
                        ? this.endOfInputButFinalTerminator() // endOfInput()

                        : (this.currentFlags & de.unkrig.ref4j.Pattern.UNIX_LINES) != 0
                        ? Sequences.endOfUnixLine()
                        : Sequences.endOfLine()
                    );
                }

                if (this.peekRead(TokenType.WORD_BOUNDARY) != null) {
                    return this.wordBoundary();
                }

                if (this.peekRead(TokenType.NON_WORD_BOUNDARY) != null) {
                    return Sequences.negate(this.wordBoundary());
                }

                if (this.peekRead(TokenType.BEGINNING_OF_INPUT) != null) {
                    return Sequences.beginningOfInput();
                }

                if (this.peekRead(TokenType.END_OF_PREVIOUS_MATCH) != null) {
                    return Sequences.endOfPreviousMatch();
                }

                if (this.peekRead(TokenType.END_OF_INPUT_BUT_FINAL_TERMINATOR) != null) {
                    return this.endOfInputButFinalTerminator();
                }

                if (this.peekRead(TokenType.END_OF_INPUT) != null) {
                    return Sequences.endOfInput();
                }

                if (this.peekRead(TokenType.MATCH_FLAGS) != null) {
                    this.setCurrentFlags(this.parseFlags(this.currentFlags, t.substring(2, t.length() - 1)));
                    return Sequences.TERMINAL;
                }

                if (this.peekRead(TokenType.LINEBREAK_MATCHER) != null) {
                    return Sequences.linebreak();
                }

                if (this.peekRead(TokenType.NAMED_CAPTURING_GROUP) != null) {
                    int groupNumber = ++rs.groupCount;

                    String s         = PatternFactory.removeSpaces(t);
                    String groupName = s.substring(3, s.length() - 1);
                    if (rs.namedGroups.put(groupName, groupNumber) != null) {
                        throw new ParseException("Duplicate capturing group name \"" + groupName + "\"");
                    }

                    Sequence result;

                    int savedFlags = this.currentFlags;
                    {
                        result = Sequences.capturingGroup(groupNumber, this.parseAlternatives());
                    }
                    this.setCurrentFlags(savedFlags);

                    this.read(")");
                    return result;
                }

                if (this.peekRead(TokenType.NAMED_CAPTURING_GROUP_BACK_REFERENCE) != null) {
                    String  s           = PatternFactory.removeSpaces(t);
                    String  groupName   = s.substring(3, s.length() - 1);
                    Integer groupNumber = rs.namedGroups.get(groupName);
                    if (groupNumber == null) {
                        throw new ParseException("Unknown group name \"" + groupName + "\"");
                    }
                    return Sequences.capturingGroupBackReference(groupNumber);
                }

                if (this.peekRead(TokenType.POSITIVE_LOOKAHEAD) != null) {
                    final Sequence op = this.parseAlternatives();
                    this.read(TokenType.END_GROUP);
                    return Sequences.positiveLookahead(op);
                }

                if (this.peekRead(TokenType.NEGATIVE_LOOKAHEAD) != null) {
                    final Sequence op = this.parseAlternatives();
                    this.read(TokenType.END_GROUP);
                    return Sequences.negate(Sequences.positiveLookahead(op));
                }

                if (this.peekRead(TokenType.POSITIVE_LOOKBEHIND) != null) {
                    final Sequence op = this.parseAlternatives().reverse();
                    this.read(TokenType.END_GROUP);
                    return Sequences.positiveLookbehind(op);
                }

                if (this.peekRead(TokenType.NEGATIVE_LOOKBEHIND) != null) {
                    final Sequence op = this.parseAlternatives().reverse();
                    this.read(TokenType.END_GROUP);
                    return Sequences.negate(Sequences.positiveLookbehind(op));
                }

                throw new AssertionError("\"" + this.peek() + "\"");
            }

            /**
             * @return A back references, based on the currently effective CASE_INSENSITIVE and UNICODE_CASE flags
             */
            public Sequence
            capturingGroupBackReference(int groupNumber) {
                return (this.currentFlags & de.unkrig.ref4j.Pattern.CASE_INSENSITIVE) == 0
                ? Sequences.capturingGroupBackReference(groupNumber)
                : (this.currentFlags & de.unkrig.ref4j.Pattern.UNICODE_CASE) == 0
                ? Sequences.caseInsensitiveCapturingGroupBackReference(groupNumber)
                : Sequences.unicodeCaseInsensitiveCapturingGroupBackReference(groupNumber);
            }

            private Sequence
            wordBoundary() {
                return (
                    (this.currentFlags & de.unkrig.ref4j.Pattern.UNICODE_CHARACTER_CLASS) == 0
                    ? Sequences.wordBoundary()
                    : Sequences.unicodeWordBoundary()
                );
            }

            /**
             * @return A {@link CharacterClass} that matches the <var>codePoint</var>, honoring surrogates and {@code
             *         this} matcher's case-sensitivity flags
             */
            private CharacterClass
            literalCharacter(int cp) {

                return (
                    (this.currentFlags & de.unkrig.ref4j.Pattern.CASE_INSENSITIVE) == 0
                    ? CharacterClasses.literalCharacter(cp)
                    : (this.currentFlags & de.unkrig.ref4j.Pattern.UNICODE_CASE) == 0
                    ? CharacterClasses.caseInsensitiveLiteralCharacter(cp)
                    : CharacterClasses.unicodeCaseInsensitiveLiteralCharacter(cp)
                );
            }

            /**
             * @return A range, based on the currently effective CASE_INSENSITIVE and UNICODE_CASE flags
             */
            public CharacterClass
            range(int lhsCp, int rhsCp) {

                return (
                    (this.currentFlags & de.unkrig.ref4j.Pattern.CASE_INSENSITIVE) == 0
                    ? CharacterClasses.range(lhsCp, rhsCp)
                    : (this.currentFlags & de.unkrig.ref4j.Pattern.UNICODE_CASE) == 0
                    ? CharacterClasses.caseInsensitiveRange(lhsCp, rhsCp)
                    : CharacterClasses.unicodeCaseInsensitiveRange(lhsCp, rhsCp)
                );
            }

            public Sequence
            endOfInputButFinalTerminator() {
                return (
                    (this.currentFlags & de.unkrig.ref4j.Pattern.UNIX_LINES) != 0
                    ? Sequences.endOfInputButFinalUnixTerminator()
                    : Sequences.endOfInputButFinalTerminator()
                );
            }

            /**
             * @param spec {@code idmsuxU-idmsuxU}
             */
            int
            parseFlags(int oldFlags, String spec) throws ParseException {

                int idx = spec.indexOf('-');

                if (idx == -1) return oldFlags | this.parseFlags(spec);

                int positiveFlags = this.parseFlags(spec.substring(0, idx));
                int negativeFlags = this.parseFlags(spec.substring(idx + 1));
                if ((positiveFlags & negativeFlags) != 0) {
                    throw new ParseException("Contradictory embedded flags \"" + spec + "\"");
                }

                return (oldFlags | positiveFlags) & ~negativeFlags;
            }

            /**
             * @param spec One or more of "{@code idmsuxU}"
             */
            private int
            parseFlags(String spec) throws ParseException {

                int result = 0;
                for (int i = 0; i < spec.length(); i++) {
                    char c = spec.charAt(i);

                    int f;
                    switch (c) {
                    case 'i': f = de.unkrig.ref4j.Pattern.CASE_INSENSITIVE;        break;
                    case 'd': f = de.unkrig.ref4j.Pattern.UNIX_LINES;              break;
                    case 'm': f = de.unkrig.ref4j.Pattern.MULTILINE;               break;
                    case 's': f = de.unkrig.ref4j.Pattern.DOTALL;                  break;
                    case 'u': f = de.unkrig.ref4j.Pattern.UNICODE_CASE;            break;
                    case 'x': f = de.unkrig.ref4j.Pattern.COMMENTS;                break;
                    case 'U': f = de.unkrig.ref4j.Pattern.UNICODE_CHARACTER_CLASS; break;
                    default:  throw new ParseException("Invalid embedded flag '" + c + "'");
                    }
                    if ((Pattern.SUPPORTED_FLAGS & f) == 0) {
                        throw new ParseException("Unsupported embedded flag '" + c + "'");
                    }
                    if ((result & f) != 0) throw new ParseException("Duplicate embedded flag '" + c + "'");
                    result |= f;
                }
                return result;
            }

            CharacterClass
            parseCharacterClass() throws ParseException {

                CharacterClass result = this.parseOptionalCharacterClass();
                if (result == null) {
                    throw new ParseException("Character class expected instead of \"" + this.peek() + "\"");
                }

                return result;
            }

            /**
             * @return {@code null} iff the next token is not a character class
             */
            @Nullable CharacterClass
            parseOptionalCharacterClass() throws ParseException {

                int cp = this.parseOptionalCharacterLiteral();
                if (cp != -1) return this.literalCharacter(cp);

                if (this.peekRead(TokenType.LEFT_BRACKET) != null) {
                    boolean        negate = this.peekRead("^");
                    CharacterClass cc     = this.parseCcIntersection();
                    this.read("]");

                    if (negate) cc = CharacterClasses.negate(cc, '^' + cc.toString());

                    return cc;
                }

                String t;
                if ((t = this.peekRead(TokenType.CC_PREDEFINED)) != null) {
                    boolean unicode = (this.currentFlags & de.unkrig.ref4j.Pattern.UNICODE_CHARACTER_CLASS) != 0;

                    CharacterClass result;
                    switch (t.charAt(1)) {
                    case 'd': case 'D': result = CharacterClasses.digit(unicode);         break;
                    case 'h': case 'H': result = CharacterClasses.horizontalWhitespace(); break;
                    case 's': case 'S': result = CharacterClasses.whitespace(unicode);    break;
                    case 'v': case 'V': result = CharacterClasses.verticalWhitespace();   break;
                    case 'w': case 'W': result = CharacterClasses.word(unicode);          break;
                    default:            throw new AssertionError(t);
                    }

                    if (Character.isUpperCase(t.charAt(1))) result = CharacterClasses.negate(result, t);

                    return result;
                }

                Token<TokenType> token = this.peek();
                if (token != null && token.type == TokenType.CC_NAMED) {

                    this.read();

                    Predicate<Integer> result = this.namedCharacterClassPredicate(token.captured[1]);

                    if (token.captured[0].equals("P")) {
                        result = PredicateUtil.not(result);
                    }

                    return CharacterClasses.characterClass(result);
                }

                return null;
            }

            /**
             * @return The code point expressed by the character literal, or {@code -1} iff the next token is not a
             *         character literal
             */
            int
            parseOptionalCharacterLiteral() throws ParseException {

                String t;

                if ((t = this.peekRead(TokenType.LITERAL_CHARACTER)) != null) {
                    return t.codePointAt(0);
                }

                if ((t = this.peekRead(TokenType.QUOTED_CHARACTER)) != null) {
                    return t.codePointAt(1);
                }

                if ((t = this.peekRead(TokenType.LITERAL_CONTROL1)) != null) {
                    return "\t\n\r\f\u0007\u001b".charAt("tnrfae".indexOf(t.charAt(1)));
                }

                if ((t = this.peekRead(TokenType.LITERAL_CONTROL2)) != null) {
                    return t.charAt(2) & 0x1f;
                }

                if ((t = this.peekRead(TokenType.LITERAL_HEXADECIMAL1)) != null) {
                    return Integer.parseInt(t.substring(2), 16);
                }

                if ((t = this.peekRead(TokenType.LITERAL_HEXADECIMAL2)) != null) {
                    char c = (char) Integer.parseInt(t.substring(2), 16);

                    // Check for "\\uxxxx\\uyyyy" surrogate pair.
                    if (Character.isHighSurrogate(c)) {
                        String t2 = this.peek(TokenType.LITERAL_HEXADECIMAL2);
                        if (t2 != null) {
                            char ls = (char) Integer.parseInt(t2.substring(2), 16);
                            if (Character.isLowSurrogate(ls)) {
                                this.read();
                                return Character.toCodePoint(c, ls);
                            }
                        }
                    }

                    return c;
                }

                if ((t = this.peekRead(TokenType.LITERAL_HEXADECIMAL3)) != null) {
                    if (t.charAt(3) == '-') throw new ParseException(t);
                    int cp;
                    try {
                        cp = Integer.parseInt(t.substring(3, t.length() - 1), 16);
                    } catch (NumberFormatException nfe) {
                        throw new ParseException(t);
                    }
                    if (cp < Character.MIN_CODE_POINT || cp > Character.MAX_CODE_POINT) {
                        throw new ParseException("Invalid code point " + cp);
                    }
                    return cp;
                }

                if ((t = this.peekRead(TokenType.LITERAL_OCTAL)) != null) {
                    return Integer.parseInt(t.substring(2), 8);
                }

                return -1;
            }

            private CharacterClass
            parseCcIntersection() throws ParseException {

                CharacterClass result = this.parseCcUnion();

                while (this.peekRead(CC_INTERSECTION) != null) {
                    result = CharacterClasses.intersection(result, this.parseCcUnion());
                    result = CharacterClasses.optimize(result);
                }

                return result;
            }

            private CharacterClass
            parseCcUnion() throws ParseException {

                CharacterClass result = this.parseCcRange();

                if (this.peek(RIGHT_BRACKET, CC_INTERSECTION) != -1) {

                    // It's a one-element union; simply return the element.
                    return result;
                }

                // Parse all union elements into a list.
                List<CharacterClass> elements = new ArrayList<CharacterClass>();
                elements.add(result);
                while (this.peek(RIGHT_BRACKET, CC_INTERSECTION) == -1) elements.add(this.parseCcRange());

                // Sort the list by ascending upper bound; this makes the following optimization more likely to
                // be successful.
                Collections.sort(elements, PatternFactory.COMPARE_BY_UPPER_BOUND);

                // Create a tree of "union" CharacterClass objects.
                result = elements.get(0);
                for (int i = 1; i < elements.size(); i++) {
                    result = CharacterClasses.union(result, elements.get(i));

                    // In many cases, this will collapse a tree of union character classes into one, (bit)set-based
                    // character class.
                    result = CharacterClasses.optimize(result);
                }

                return result;
            }

            /**
             * <pre>
             *   cc-range :=
             *        character-class
             *        | character-literal                       // x   => 'x'
             *        | '-'                                     // -   => '-'
             *        | character-literal '-'                   // x-  => 'x' or '-'
             *        | '-' '-'                                 // --  => '-' or '-'      (effectively: '-')
             *        | character-literal '-' character-literal // x-y => 'x' through 'y'
             *        | '-' '-' character-literal               // --x => '-' through 'x'
             *        | character-literal '-' '-'               // x-- => 'x' through '-'
             *        | '-' '-' '-'                             // --- => '-' through '-' (effectively: '-')
             * </pre>
             *
             * @see #parseOptionalCharacterLiteral()
             */
            private CharacterClass
            parseCcRange() throws ParseException {

                // Parse range start character.
                int lhs = this.parseOptionalCharacterLiteral();
                if (lhs == -1) return this.parseCharacterClass();

                // Parse hyphen.
                if (!this.peekRead("-")) return this.literalCharacter(lhs);

                // Parse range end character.
                int rhs = this.parseOptionalCharacterLiteral();
                if (rhs == -1) {
                    return CharacterClasses.union(this.literalCharacter(lhs), CharacterClasses.literalCharacter('-'));
                }

                return this.range(lhs, rhs);
            }

            Predicate<Integer>
            namedCharacterClassPredicate(String name) throws ParseException {

                int                eq;
                Predicate<Integer> result;

                if ((eq = name.indexOf('=')) != -1) {
                    String prefix = name.substring(0, eq);
                    name = name.substring(eq + 1);

                    if ("sc".equals(prefix) || "script".equals(prefix)) {
                        if ((result = Characters.unicodeScriptPredicate(name)) != null) return result;
                        new ParseException("Unknown UNICODE script \"" + name + "\"");
                    }
                    if ("gc".equals(prefix) || "general_category".equals(prefix)) {
                        if ((result = Characters.unicodeCategoryFromName(name)) != null) return result;
                        new ParseException("Unknown UNICODE general category \"" + name + "\"");
                    }
                    if ("blk".equals(prefix) || "block".equals(prefix)) {
                        if ((result = Characters.unicodeBlockFromName(name)) != null) return result;
                        new ParseException("Unknown UNICODE block \"" + name + "\"");
                    }

                    throw new ParseException(
                        "Invalid character familiy qualifier \""
                        + prefix
                        + "\"; valid qualifiers are \"script\" (JRE 1.7+ only), \"general_category\" and \"block\""
                    );
                }

                if (name.startsWith("In")) {
                    name = name.substring(2);

                    // A unicode block?
                    if ((result = Characters.unicodeBlockFromName(name)) != null) return result;
                } else
                {
                    if (name.startsWith("Is")) name = name.substring(2);

                    // A POSIX character class?
                    boolean isUnicode = (this.currentFlags & de.unkrig.ref4j.Pattern.UNICODE_CHARACTER_CLASS) != 0;
                    if ((result = (
                        isUnicode
                        ? Characters.unicodePredefinedCharacterClassFromName(name)
                        : Characters.posixCharacterClassFromName(name)
                    )) != null) return result;

                    // A Java character class?
                    if ((result = Characters.javaCharacterClassFromName(name)) != null) return result;

                    // A UNICODE property, e.g. "TITLECASE"?
                    if ((result = Characters.unicodeBinaryPropertyFromName(name)) != null) return result;

                    // A UNICODE character category, e.g. category "Lu"?
                    if ((result = Characters.unicodeCategoryFromName(name)) != null) return result;

                    // A Unicode "script"?
                    // (Class UnicodeScript only available from Java 7.)
                    // For JRE 1.6 compatibility, go through reflection.
                    if (Characters.unicodeScriptAvailable()) {
                        if ((result = Characters.unicodeScriptPredicate(name)) != null) return result;
                    }

                    // A unicode block?
                    if ((result = Characters.unicodeBlockFromName(name)) != null) return result;

                    // General category?
                    if ((result = Characters.unicodeCategoryFromName(name)) != null) return result;

                }

                throw new ParseException((
                    "Invalid or unimplemented UNICODE property, category, script or block \""
                    + name
                    + "\""
                ));
            }
        }.parse();
    }

    private static String
    removeSpaces(String subject) {

        int len = subject.length();

        for (int i = 0; i < len; i++) {
            if (Character.isWhitespace(subject.charAt(i))) {
                int j;
                for (j = i + 1; j < len && Character.isWhitespace(subject.charAt(j)); j++);
                subject = subject.substring(0, i) + subject.substring(j);
                len     -= j - i;
            }
        }
        return subject;
    }

    /**
     * @return The code point represented by a {@link TokenType#LITERAL_HEXADECIMAL} token
     */
    public static int
    literalHexadecimal(String s) throws ParseException {

        int cp = Integer.parseInt((
            s.charAt(2) == '{'
            ? s.substring(3, s.length() - 1)
            : s.substring(2)
        ), 16);

        if (cp < Character.MIN_CODE_POINT || cp > Character.MAX_CODE_POINT) {
            throw new ParseException("Invalid code point " + cp);
        }

        return cp;
    }
}
