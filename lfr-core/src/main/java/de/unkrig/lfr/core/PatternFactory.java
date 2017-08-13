
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
import static de.unkrig.lfr.core.Pattern.TokenType.LITERAL_CHARACTER;
import static de.unkrig.lfr.core.Pattern.TokenType.QUOTATION_END;
import static de.unkrig.lfr.core.Pattern.TokenType.RIGHT_BRACKET;

import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.Characters;
import de.unkrig.commons.lang.protocol.Predicate;
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

        return new Pattern(regex, flags, sequence, rs.groupCount, rs.namedGroups);
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
            @Override public boolean evaluate(Token<TokenType> subject) { return subject.type != TokenType.COMMENT; }
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

                if (this.peek(null, EITHER_OR, END_GROUP, QUOTATION_END) != -1) return Sequences.TERMINAL;

                Sequence result = this.parseQuantified();
                if (this.peek(null, EITHER_OR, END_GROUP, QUOTATION_END) != -1) return result;

                List<Sequence> tmp = new ArrayList<Sequence>();
                tmp.add(result);
                do {
                    tmp.add(this.parseQuantified());
                } while (this.peek(null, EITHER_OR, END_GROUP, QUOTATION_END) == -1);

                result = Sequences.TERMINAL;
                for (int i = tmp.size() - 1; i >= 0; i--) {
                    result = tmp.get(i).concat(result);
                }
                return result;
            }

            private Sequence
            parseQuantified() throws ParseException {

                final Sequence op = this.parsePrimary();

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
                            String[] c = AssertionUtil.notNull(t.captured);
                            min = Integer.parseInt(c[0]);
                            max = c[1] == null ? min : c[2] == null ? Integer.MAX_VALUE : Integer.parseInt(c[2]);
                        }
                        break;

                    default:
                        throw new AssertionError(t);
                    }

                    switch (t.type) {

                    case GREEDY_QUANTIFIER:     return Sequences.greedyQuantifierSequence(op, min, max);
                    case RELUCTANT_QUANTIFIER:  return Sequences.reluctantQuantifierSequence(op, min, max);
                    case POSSESSIVE_QUANTIFIER: return Sequences.possessiveQuantifierSequence(op, min, max);

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

                String t;
                {
                    Token<TokenType> token = this.peek();
                    if (token == null) throw new ParseException("Unexpected end-of-input");
                    t = token.text;
                }

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

                if (this.peekRead(TokenType.QUOTATION_BEGIN) != null) {
                    Sequence result = this.parseSequence();
                    if (this.peek() != null) this.read(TokenType.QUOTATION_END);
                    return result;
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
                    int groupNumber = Integer.parseInt(t.substring(1));
                    if (groupNumber > rs.groupCount) {
                        throw new ParseException("Group number " + groupNumber + " too big");
                    }
                    return Sequences.capturingGroupBackReference(groupNumber);
                }

                if (this.peekRead(TokenType.BEGINNING_OF_LINE) != null) {
                    return (
                        (this.currentFlags & de.unkrig.ref4j.Pattern.MULTILINE) == 0
                        ? Sequences.beginningOfInput()
                        : Sequences.beginningOfLine(
                            (this.currentFlags & de.unkrig.ref4j.Pattern.UNIX_LINES) != 0
                            ? Sequences.UNIX_LINE_BREAK_CHARACTERS
                            : Sequences.LINE_BREAK_CHARACTERS
                        )
                    );
                }

                if (this.peekRead(TokenType.END_OF_LINE) != null) {
                    return (
                        (this.currentFlags & de.unkrig.ref4j.Pattern.MULTILINE) == 0
                        ? Sequences.endOfInput()
                        : Sequences.endOfLine(
                            (this.currentFlags & de.unkrig.ref4j.Pattern.UNIX_LINES) != 0
                            ? Sequences.UNIX_LINE_BREAK_CHARACTERS
                            : Sequences.LINE_BREAK_CHARACTERS
                        )
                    );
                }

                if (this.peekRead(TokenType.WORD_BOUNDARY) != null) {
                    return Sequences.wordBoundary();
                }

                if (this.peekRead(TokenType.NON_WORD_BOUNDARY) != null) {
                    return Sequences.negate(Sequences.wordBoundary());
                }

                if (this.peekRead(TokenType.BEGINNING_OF_INPUT) != null) {
                    return Sequences.beginningOfInput();
                }

                if (this.peekRead(TokenType.END_OF_PREVIOUS_MATCH) != null) {
                    return Sequences.endOfPreviousMatch();
                }

                if (this.peekRead(TokenType.END_OF_INPUT_BUT_FINAL_TERMINATOR) != null) {
                    return Sequences.endOfInputButFinalTerminator();
                }

                if (this.peekRead(TokenType.END_OF_INPUT) != null) {
                    return Sequences.endOfInput();
                }

                if (this.peekRead(TokenType.MATCH_FLAGS) != null) {
                    this.setCurrentFlags(this.parseFlags(this.currentFlags, t.substring(2, t.length() - 1)));
                    return Sequences.TERMINAL;
                }

                if (this.peekRead(TokenType.LINEBREAK_MATCHER) != null) {
                    return Sequences.linebreakSequence();
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

            @Nullable CharacterClass
            parseOptionalCharacterClass() throws ParseException {

                String t;

                if ((t = this.peekRead(TokenType.LITERAL_CHARACTER)) != null) {
                    return this.literalCharacter(t.codePointAt(0));
                }

                if ((t = this.peekRead(TokenType.QUOTED_CHARACTER)) != null) {
                    return this.literalCharacter(t.codePointAt(1));
                }

                if ((t = this.peekRead(TokenType.LITERAL_CONTROL1)) != null) {
                    return this.literalCharacter("\t\n\r\f\u0007\u001b".charAt("tnrfae".indexOf(t.charAt(1))));
                }

                if ((t = this.peekRead(TokenType.LITERAL_CONTROL2)) != null) {
                    return this.literalCharacter(t.charAt(2) & 0x1f);
                }

                if ((t = this.peekRead(TokenType.LITERAL_HEXADECIMAL1)) != null) {
                    return this.literalCharacter(Integer.parseInt(t.substring(2), 16));
                }

                if ((t = this.peekRead(TokenType.LITERAL_HEXADECIMAL2)) != null) {
                    char c1 = (char) Integer.parseInt(t.substring(2), 16);

                    // Check for "\\uxxxx\\uyyyy" surrogate pair.
                    if (Character.isHighSurrogate(c1)) {
                        String t2 = this.peek(TokenType.LITERAL_HEXADECIMAL2);
                        if (t2 != null) {
                            char c2 = (char) Integer.parseInt(t2.substring(2), 16);
                            if (Character.isLowSurrogate(c2)) {
                                this.read();
                                return this.literalCharacter(Character.toCodePoint(c1, c2));
                            }
                        }
                    }

                    return this.literalCharacter(c1);
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
                    return this.literalCharacter(cp);
                }

                if ((t = this.peekRead(TokenType.LITERAL_OCTAL)) != null) {
                    return this.literalCharacter(Integer.parseInt(t.substring(2), 8));
                }

                if ((t = this.peekRead(TokenType.LEFT_BRACKET)) != null) {
                    boolean        negate = this.peekRead("^");
                    CharacterClass cc     = this.parseCcIntersection();
                    this.read("]");

                    if (negate) cc = CharacterClasses.negate(cc, '^' + cc.toString());

                    return cc;
                }

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

                if ((t = this.peekRead(TokenType.CC_POSIX)) != null) {
                    String  ccName = t.substring(3, t.length() - 1);
                    boolean u      = (this.currentFlags & de.unkrig.ref4j.Pattern.UNICODE_CHARACTER_CLASS) != 0;

                    Predicate<Integer> codePointPredicate = (
                        "Lower".equals(ccName)  ? (u ? Characters.IS_UNICODE_LOWER       : Characters.IS_POSIX_LOWER)  :  // SUPPRESS CHECKSTYLE LineLength:12
                        "Upper".equals(ccName)  ? (u ? Characters.IS_UNICODE_UPPER       : Characters.IS_POSIX_UPPER)  :
                        "ASCII".equals(ccName)  ? Characters.IS_POSIX_ASCII                                            :
                        "Alpha".equals(ccName)  ? (u ? Characters.IS_UNICODE_ALPHA       : Characters.IS_POSIX_ALPHA)  :
                        "Digit".equals(ccName)  ? (u ? Characters.IS_UNICODE_DIGIT       : Characters.IS_POSIX_DIGIT)  :
                        "Alnum".equals(ccName)  ? (u ? Characters.IS_UNICODE_ALNUM       : Characters.IS_POSIX_ALNUM)  :
                        "Punct".equals(ccName)  ? (u ? Characters.IS_UNICODE_PUNCT       : Characters.IS_POSIX_PUNCT)  :
                        "Graph".equals(ccName)  ? (u ? Characters.IS_UNICODE_GRAPH       : Characters.IS_POSIX_GRAPH)  :
                        "Print".equals(ccName)  ? (u ? Characters.IS_UNICODE_PRINT       : Characters.IS_POSIX_PRINT)  :
                        "Blank".equals(ccName)  ? (u ? Characters.IS_UNICODE_BLANK       : Characters.IS_POSIX_BLANK)  :
                        "Cntrl".equals(ccName)  ? (u ? Characters.IS_UNICODE_CNTRL       : Characters.IS_POSIX_CNTRL)  :
                        "XDigit".equals(ccName) ? (u ? Characters.IS_UNICODE_HEX_DIGIT   : Characters.IS_POSIX_XDIGIT) :
                        "Space".equals(ccName)  ? (u ? Characters.IS_UNICODE_WHITE_SPACE : Characters.IS_POSIX_SPACE)  :
                        null
                    );
                    assert codePointPredicate != null;

                    CharacterClass result = CharacterClasses.characterClass(codePointPredicate);

                    if (t.charAt(1) == 'P') result = CharacterClasses.negate(result, t);

                    return result;
                }

                if ((t = this.peekRead(TokenType.CC_JAVA)) != null) {
                    String             ccName             = t.substring(3, t.length() - 1);
                    Predicate<Integer> codePointPredicate = (
                        "javaLowerCase".equals(ccName)  ? Characters.IS_LOWER_CASE :
                        "javaUpperCase".equals(ccName)  ? Characters.IS_UPPER_CASE :
                        "javaWhitespace".equals(ccName) ? Characters.IS_WHITESPACE :
                        "javaMirrored".equals(ccName)   ? Characters.IS_MIRRORED   :
                        null
                    );
                    assert codePointPredicate != null;

                    CharacterClass result = CharacterClasses.characterClass(codePointPredicate);

                    if (t.charAt(1) == 'P') result = CharacterClasses.negate(result, t);

                    return result;
                }

                if ((t = this.peekRead(TokenType.CC_UNICODE_SCRIPT_OR_BINARY_PROPERTY)) != null) {
                    String name = t.substring(5, t.length() - 1);

                    // A UNICODE preperty, e.g. "TITLECASE"?
                    Predicate<Integer> codePointPredicate = Characters.unicodePropertyFromName(name);
                    if (codePointPredicate != null) {
                        return CharacterClasses.characterClass(codePointPredicate);
                    }

                    // A UNICODE character property, e.g. category "Lu"?
                    Byte gc = PatternFactory.getGeneralCategory(name);
                    if (gc != null) return CharacterClasses.inUnicodeGeneralCategory(gc);

                    // A Unicode "script"?
                    // (Class UnicodeScript only available from Java 7.)

                    throw new AssertionError((
                        "Invalid or unimplemented Unicode script or property \""
                        + name
                        + "\""
                    ));
                }

                if ((t = this.peekRead(TokenType.CC_UNICODE_BLOCK)) != null) {
                    String unicodeBlockName = t.substring(5, t.length() - 1);

                    UnicodeBlock block;
                    try {
                        block = Character.UnicodeBlock.forName(unicodeBlockName);
                    } catch (IllegalArgumentException iae) {
                        throw new ParseException("Invalid unicode block \"" + unicodeBlockName + "\"");
                    }

                    CharacterClass result = CharacterClasses.inUnicodeBlock(block);

                    if (t.charAt(1) == 'P') result = CharacterClasses.negate(result, t);

                    return result;
                }

                if ((t = this.peekRead(TokenType.CC_UNICODE_CATEGORY)) != null) {
                    String  gcName = t.substring(3, 5);
                    Byte    gc     = PatternFactory.getGeneralCategory(gcName);
                    if (gc == null) throw new ParseException("Unknown general cateogry \"" + gcName + "\"");

                    CharacterClass result = CharacterClasses.inUnicodeGeneralCategory(gc);

                    if (t.charAt(1) == 'P') result = CharacterClasses.negate(result, t);

                    return result;
                }

                return null;
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
             *        | literal-character                       // x   => 'x'
             *        | '-'                                     // -   => '-'
             *        | literal-character '-'                   // x-  => 'x' or '-'
             *        | '-' '-'                                 // --  => '-' or '-'      (effectively: '-')
             *        | literal-character '-' literal-character // x-y => 'x' through 'y'
             *        | '-' '-' literal-character               // --x => '-' through 'x'
             *        | literal-character '-' '-'               // x-- => 'x' through '-'
             *        | '-' '-' '-'                             // --- => '-' through '-' (effectively: '-')
             * </pre>
             */
            private CharacterClass
            parseCcRange() throws ParseException {

                // Parse range start character.
                String lhs = this.peekRead(LITERAL_CHARACTER);
                if (lhs == null) return this.parseCharacterClass();
                int lhsCp = lhs.codePointAt(0);

                // Parse hyphen.
                if (!this.peekRead("-")) return CharacterClasses.literalCharacter(lhsCp);

                // Parse range end character.
                String rhs = this.peekRead(LITERAL_CHARACTER);
                if (rhs == null) return CharacterClasses.oneOfTwo(lhsCp, '-');

                return CharacterClasses.range(lhsCp, rhs.codePointAt(0));
            }
        }.parse();
    }

    @Nullable private static Byte
    getGeneralCategory(String name) {

        Map<String, Byte> m = PatternFactory.generalCategories;
        if (m == null) {
            m = new HashMap<String, Byte>();

            // The JRE provides no way to translate GC names int GC values.
            m.put("CN", Character.UNASSIGNED);
            m.put("LU", Character.UPPERCASE_LETTER);
            m.put("LL", Character.LOWERCASE_LETTER);
            m.put("LT", Character.TITLECASE_LETTER);
            m.put("LM", Character.MODIFIER_LETTER);
            m.put("LO", Character.OTHER_LETTER);
            m.put("MN", Character.NON_SPACING_MARK);
            m.put("ME", Character.ENCLOSING_MARK);
            m.put("MC", Character.COMBINING_SPACING_MARK);
            m.put("ND", Character.DECIMAL_DIGIT_NUMBER);
            m.put("NL", Character.LETTER_NUMBER);
            m.put("NO", Character.OTHER_NUMBER);
            m.put("ZS", Character.SPACE_SEPARATOR);
            m.put("ZL", Character.LINE_SEPARATOR);
            m.put("ZP", Character.PARAGRAPH_SEPARATOR);
            m.put("CC", Character.CONTROL);
            m.put("CF", Character.FORMAT);
            m.put("CO", Character.PRIVATE_USE);
            m.put("CS", Character.SURROGATE);
            m.put("PD", Character.DASH_PUNCTUATION);
            m.put("PS", Character.START_PUNCTUATION);
            m.put("PE", Character.END_PUNCTUATION);
            m.put("PC", Character.CONNECTOR_PUNCTUATION);
            m.put("PO", Character.OTHER_PUNCTUATION);
            m.put("SM", Character.MATH_SYMBOL);
            m.put("SC", Character.CURRENCY_SYMBOL);
            m.put("SK", Character.MODIFIER_SYMBOL);
            m.put("SO", Character.OTHER_SYMBOL);
            m.put("PI", Character.INITIAL_QUOTE_PUNCTUATION);
            m.put("PF", Character.FINAL_QUOTE_PUNCTUATION);

            PatternFactory.generalCategories = Collections.unmodifiableMap(m);
        }

        return m.get(name.toUpperCase(Locale.ENGLISH));
    }
    @Nullable private static Map<String, Byte> generalCategories;

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
