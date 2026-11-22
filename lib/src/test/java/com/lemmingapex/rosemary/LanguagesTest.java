package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;
import java.util.Locale;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Month and weekday names in the parser's locale, as well as English.
 */
class LanguagesTest {

	@ParameterizedTest(name = "{0}: {1}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		fr | 5 mars 2024                                | 2024-03-05T00:00Z
		fr | mardi 5 mars 2024 10:00                    | 2024-03-05T10:00Z
		fr | `le 5 juillet 2024 à 10:00`                | 2024-07-05T10:00Z
		fr | 5 févr. 2024                               | 2024-02-05T00:00Z
		fr | 5 fevrier 2024                             | 2024-02-05T00:00Z
		fr | 5 FÉVRIER 2024                             | 2024-02-05T00:00Z
		fr | 05-mars-2024                               | 2024-03-05T00:00Z
		fr | 5 juil. 2024                               | 2024-07-05T00:00Z
		fr | 1er mars 2024                              | 2024-03-01T00:00Z
		fr | 1re mars 2024                              | 2024-03-01T00:00Z
		fr | 5 août 2024                                | 2024-08-05T00:00Z
		fr | 5 decembre 2024                            | 2024-12-05T00:00Z
		de | 5. März 2024                               | 2024-03-05T00:00Z
		de | 5. Marz 2024                               | 2024-03-05T00:00Z
		de | 5. Mär. 2024                               | 2024-03-05T00:00Z
		de | `Dienstag, 5. März 2024 14:30`             | 2024-03-05T14:30Z
		de | am 5. März 2024                            | 2024-03-05T00:00Z
		de | `Dienstag, am 5. März 2024 um 14:30 Uhr`   | 2024-03-05T14:30Z
		de | 5. März 2024 7:02 am                       | 2024-03-05T07:02Z
		de | 5. März 2024 7 pm                          | 2024-03-05T19:00Z
		de | `Di., 5. März 2024`                        | 2024-03-05T00:00Z
		es | `martes, 5 de marzo de 2024`               | 2024-03-05T00:00Z
		es | 5 mar 2024                                 | 2024-03-05T00:00Z
		es | `mar, 5 mar 2024`                          | 2024-03-05T00:00Z
		es | `mar, 7 may 2024`                          | 2024-05-07T00:00Z
		es | 1º de marzo de 2024                        | 2024-03-01T00:00Z
		es | 5 de septiembre de 2024                    | 2024-09-05T00:00Z
		es | 5 sept 2024                                | 2024-09-05T00:00Z
		it | martedì 5 marzo 2024                       | 2024-03-05T00:00Z
		it | 5 giu 2024                                 | 2024-06-05T00:00Z
		pt | `terça-feira, 5 de março de 2024`          | 2024-03-05T00:00Z
		pt | 5 de marco de 2024                         | 2024-03-05T00:00Z
		pt | 5 out. 2024                                | 2024-10-05T00:00Z
		nl | 5 mrt 2024                                 | 2024-03-05T00:00Z
		nl | dinsdag 5 maart 2024                       | 2024-03-05T00:00Z
		sv | 5 mars 2024                                | 2024-03-05T00:00Z
		da | 5. marts 2024                              | 2024-03-05T00:00Z
		nb | 5. mars 2024                               | 2024-03-05T00:00Z
		fi | 5. maaliskuuta 2024                        | 2024-03-05T00:00Z
		fi | 5. maalisk. 2024                           | 2024-03-05T00:00Z
		fi | 5. maalis 2024                             | 2024-03-05T00:00Z
		pl | 5 marca 2024 r.                            | 2024-03-05T00:00Z
		pl | 5 października 2024                        | 2024-10-05T00:00Z
		pl | 5 paź 2024                                 | 2024-10-05T00:00Z
		cs | 5. března 2024                             | 2024-03-05T00:00Z
		cs | 5. června 2024                             | 2024-06-05T00:00Z
		cs | 5. července 2024                           | 2024-07-05T00:00Z
		cs | 5. červen 2024                             | 2024-06-05T00:00Z
		hu | 2024. március 5.                           | 2024-03-05T00:00Z
		ro | 5 martie 2024                              | 2024-03-05T00:00Z
		tr | 5 Mart 2024 Salı                           | 2024-03-05T00:00Z
		tr | 5 Ağustos 2024                             | 2024-08-05T00:00Z
		tr | 5 agustos 2024                             | 2024-08-05T00:00Z
		ru | 5 марта 2024 г.                            | 2024-03-05T00:00Z
		ru | `вторник, 5 марта 2024`                    | 2024-03-05T00:00Z
		ru | 5 мая 2024                                 | 2024-05-05T00:00Z
		ru | 5 сент. 2024                               | 2024-09-05T00:00Z
		uk | 5 березня 2024                             | 2024-03-05T00:00Z
		el | 5 Μαρτίου 2024                             | 2024-03-05T00:00Z
		el | 5 μαΐου 2024                               | 2024-05-05T00:00Z
		ca | 5 de març de 2024                          | 2024-03-05T00:00Z
		ca | 5 d’abril de 2024                          | 2024-04-05T00:00Z
		ca | 5 d'abril de 2024                          | 2024-04-05T00:00Z
		hr | 5. ožujka 2024.                            | 2024-03-05T00:00Z
		id | 5 Maret 2024                               | 2024-03-05T00:00Z
		""")
	void readsNamesInTheLocale(String languageTag, String text, OffsetDateTime expected) {
		assertEquals(expected, parser(languageTag).parse(text));
	}

	/**
	 * Dates from machines, like HTTP and email, are in English whatever the locale.
	 */
	@ParameterizedTest(name = "{0}: {1}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		fr | `Tue, 05 Mar 2024 10:00:00 GMT`            | 2024-03-05T10:00Z
		es | `Tue, 05 Mar 2024 10:00:00 GMT`            | 2024-03-05T10:00Z
		de | `Wed, 05 Jun 2024 10:00:00 GMT`            | 2024-06-05T10:00Z
		ja | `Tue, 05 Mar 2024 10:00:00 GMT`            | 2024-03-05T10:00Z
		ja | 2024-03-05 10:00                           | 2024-03-05T10:00Z
		ru | Fifth of March 2024                        | 2024-03-05T00:00Z
		""")
	void readsEnglishNamesInAnyLocale(String languageTag, String text, OffsetDateTime expected) {
		assertEquals(expected, parser(languageTag).parse(text));
	}

	/**
	 * The first letters of more than one month are rejected, rather than ignored.
	 */
	@ParameterizedTest(name = "{0}: {1}")
	@CsvSource(delimiter = '|', textBlock = """
		fr | 5 jui 2024     | 2
		cs | 5. červ 2024   | 3
		""")
	void rejectsTheStartOfMoreThanOneMonth(String languageTag, String text, int index) {
		final RosemaryDateTimeException e = assertThrows(RosemaryDateTimeException.class, () -> parser(languageTag).parse(text));
		assertEquals(index, e.getErrorIndex());
	}

	/**
	 * Without the locale, other languages' names are words that are ignored, and English "am" is AM.
	 */
	@Test
	void usesOnlyTheLocalesNames() {
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse("am 5. März 2024"));
		assertThrows(RosemaryDateTimeException.class, () -> parser("fr").parse("am 5. März 2024"));
		assertEquals(OffsetDateTime.parse("2024-03-05T00:00Z"), parser("de").parse("5. März 2024"));
	}

	/**
	 * Japanese, like Chinese and Korean, writes months as numbers, like 3月, so a Japanese locale adds no names.
	 */
	@Test
	void ignoresNamesWithDigits() {
		assertThrows(RosemaryDateTimeException.class, () -> parser("ja").parse("2024年3月5日"));
		assertEquals(OffsetDateTime.parse("2024-03-05T00:00Z"), parser("ja").parse("March 5 2024"));
	}

	@Test
	void englishLocalesUseEnglishNames() {
		for (Locale locale : new Locale[] {Locale.US, Locale.UK, Locale.forLanguageTag("en-IN")}) {
			assertEquals(OffsetDateTime.parse("2024-03-05T00:00Z"), parser(locale.toLanguageTag()).parse("Tue 5 Mar 2024"), locale.toString());
		}
	}

	private static RosemaryDateTimeParser parser(String languageTag) {
		return PARSER.toBuilder().locale(Locale.forLanguageTag(languageTag)).build();
	}
}
