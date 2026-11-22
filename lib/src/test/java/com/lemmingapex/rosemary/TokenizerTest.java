package com.lemmingapex.rosemary;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Splitting datetime strings into tokens.  Tokens are separated by spaces in the expected values, and an empty value means no tokens.
 */
class TokenizerTest {

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		`Mar 5, 2008 7:02 pm`                  | MAR 5 2008 7:02 PM
		2008-03-05                             | 2008 03 05
		01-02-03 04:05                         | 01 02 03 04:05
		3/5/24                                 | 3 5 24
		AMERICA/DENVER                         | AMERICA/DENVER
		AMERICA/ARGENTINA/BUENOS_AIRES         | AMERICA/ARGENTINA/BUENOS_AIRES
		AMERICA/PORT-AU-PRINCE                 | AMERICA/PORT-AU-PRINCE
		ETC/GMT+5                              | ETC/GMT+5
		7:02:10 -05:00                         | 7:02:10 -05:00
		(GMT  -7)                              | GMT -7
		AM(UTC-4)                              | AM UTC-4
		THIRTY-FIRST                           | THIRTY-FIRST
		`MARCH -- 5 - 2008`                    | MARCH 5 2008
		`"2022-06-16 17:55:09.866 EDT"`        | 2022 06 16 17:55:09.866 EDT
		`'08`                                  | 08
		`WED. MARCH 5TH`                       | WED. MARCH 5TH
		`\t2008\n03 05 `                  | 2008 03 05
		`---`                                  | ``
		``                                     | ``
		""")
	void splitsIntoTokens(String text, String expected) {
		final List<String> expectedTokens = expected.isEmpty() ? List.of() : Arrays.asList(expected.split(" "));
		assertEquals(expectedTokens, Tokenizer.tokenize(text).stream().map(Tokenizer.Token::text).toList());
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		`Mar 5, 2008 7:02 PM`        | 0 4 7 12 17
		`  March, , 5   2008  `      | 2 11 15
		2008-03-05T07:02:10-05:00    | 0 5 8 11
		(GMT  -7)                    | 1 6
		`Straße 5 10:xx`             | 0 7 9
		`ß 10:xx`                    | 0 2
		""")
	void knowsWhereTokensStart(String text, String expected) {
		final List<Integer> expectedStarts = Arrays.stream(expected.split(" ")).map(Integer::valueOf).toList();
		assertEquals(expectedStarts, Tokenizer.tokenize(text).stream().map(Tokenizer.Token::start).toList());
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		march                   | MARCH
		Straße                  | STRASSE
		2008-03-05t07:02        | 2008 03 05 07:02
		""")
	void upperCasesTokens(String text, String expected) {
		assertEquals(Arrays.asList(expected.split(" ")), Tokenizer.tokenize(text).stream().map(Tokenizer.Token::text).toList());
	}
}
