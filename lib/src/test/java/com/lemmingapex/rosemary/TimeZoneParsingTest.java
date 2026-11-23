package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

import static com.lemmingapex.rosemary.TestParsers.CLOCK;
import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Time zones: region ids, abbreviations and offsets.
 */
class TimeZoneParsingTest {

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		2008-03-05 07:02:00 America/Los_Angeles            | 2008-03-05T07:02-08:00
		2008-03-05 07:02:00 america/los_angeles            | 2008-03-05T07:02-08:00
		2008-03-05 07:02:00 AMErica/los_ANGELES            | 2008-03-05T07:02-08:00
		2008-03-05 07:02:00 America/Denver                 | 2008-03-05T07:02-07:00
		2008-03-05 07:02:00 America/Chicago                | 2008-03-05T07:02-06:00
		2008-03-05 07:02:00 America/New_York               | 2008-03-05T07:02-05:00
		2008-03-05 07:02:00 Africa/Cairo                   | 2008-03-05T07:02+02:00
		2008-03-05 07:02:00 America/Phoenix                | 2008-03-05T07:02-07:00
		2008-03-05 07:02:00 America/Indiana/Indianapolis   | 2008-03-05T07:02-05:00
		2008-03-05 07:02:00 Pacific/Honolulu               | 2008-03-05T07:02-10:00
		2008-03-05 07:02:00 America/Anchorage              | 2008-03-05T07:02-09:00
		2008-03-05 07:02:00 Asia/Karachi                   | 2008-03-05T07:02+05:00
		2008-03-05 07:02:00 Atlantic/Reykjavik             | 2008-03-05T07:02Z
		2008-03-05 07:02:00 Australia/Brisbane             | 2008-03-05T07:02+10:00
		2008-03-05 07:02:00 Australia/Eucla                | 2008-03-05T07:02+09:45
		2024-03-05 07:02:00 Australia/Eucla                | 2024-03-05T07:02+08:45
		2008-03-05 07:02:00 Europe/London                  | 2008-03-05T07:02Z
		2008-03-05 07:02:00 Europe/Zurich                  | 2008-03-05T07:02+01:00
		2008-03-05 07:02:00 Europe/Paris                   | 2008-03-05T07:02+01:00
		2008-03-05 07:02:00 Europe/Amsterdam               | 2008-03-05T07:02+01:00
		2008-03-05 07:02:00 Pacific/Guam                   | 2008-03-05T07:02+10:00
		2008-03-05 07:02:00 Pacific/Chatham                | 2008-03-05T07:02+13:45
		March 5 2008 7:02pm Africa/Dar_es_Salaam           | 2008-03-05T19:02+03:00
		June 21 2022 3:45 pm America/Mazatlan              | 2022-06-21T15:45-06:00
		June 21 2022 3:45 pm America/Detroit               | 2022-06-21T15:45-04:00
		2008-03-05 07:02:00 Etc/GMT+5                      | 2008-03-05T07:02-05:00
		""")
	void parsesRegionIds(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		2008-03-05 07:02:00 GMT               | 2008-03-05T07:02Z
		2008-03-05 07:02:00 UTC               | 2008-03-05T07:02Z
		2008-03-05 07:02:00 UT                | 2008-03-05T07:02Z
		2008-03-05 07:02:00 Z                 | 2008-03-05T07:02Z
		2008-06-05 07:02:00 GMT               | 2008-06-05T07:02Z
		2008-03-05 07:02:00 AKST              | 2008-03-05T07:02-09:00
		2008-03-05 07:02:00 AKDT              | 2008-03-05T07:02-09:00
		2008-03-05 07:02:00 PST               | 2008-03-05T07:02-08:00
		2008-03-05 07:02:00 PDT               | 2008-03-05T07:02-08:00
		2008-03-05 07:02:00 MST               | 2008-03-05T07:02-07:00
		2008-03-05 07:02:00 MDT               | 2008-03-05T07:02-07:00
		2008-03-05 07:02:00 CST               | 2008-03-05T07:02-06:00
		2008-04-03 09:59:00 CDT               | 2008-04-03T09:59-05:00
		2008-03-05 07:02:00 EST               | 2008-03-05T07:02-05:00
		2008-03-05 07:02:00 EDT               | 2008-03-05T07:02-05:00
		2008-03-05 07:02:00 pst               | 2008-03-05T07:02-08:00
		2010-03-05 07:02:04.488 AKDT          | 2010-03-05T07:02:04.488-09:00
		`2022-01-13 11:52:08.715 EST\t`       | 2022-01-13T11:52:08.715-05:00
		`"2022-06-16 17:55:09.866 EDT"`       | 2022-06-16T17:55:09.866-04:00
		`\n2022-05-11 00:11:31.501 CDT\n`     | 2022-05-11T00:11:31.501-05:00
		2024-07-01 10:00 CET                  | 2024-07-01T10:00+02:00
		2024-01-01 10:00 IST                  | 2024-01-01T10:00+05:30
		2024-01-01 10:00 JST                  | 2024-01-01T10:00+09:00
		2024-01-01 10:00 AEDT                 | 2024-01-01T10:00+11:00
		""")
	void parsesAbbreviations(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	/**
	 * An abbreviation means its region, so the offset depends on the date, whether the abbreviation is for standard or daylight time.
	 */
	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		June 21 2022 3:45 pm MDT   | 2022-06-21T15:45-06:00
		Jan 21 2022 3:45 pm MDT    | 2022-01-21T15:45-07:00
		June 21 2022 3:45 pm MST   | 2022-06-21T15:45-06:00
		Jan 21 2022 3:45 pm MST    | 2022-01-21T15:45-07:00
		June 21 2022 3:45 pm EST   | 2022-06-21T15:45-04:00
		Jan 21 2022 3:45 pm EDT    | 2022-01-21T15:45-05:00
		June 21 2022 3:45 pm CST   | 2022-06-21T15:45-05:00
		Jan 21 2022 3:45 pm CDT    | 2022-01-21T15:45-06:00
		June 21 2022 3:45 pm PST   | 2022-06-21T15:45-07:00
		Jan 21 2022 3:45 pm PDT    | 2022-01-21T15:45-08:00
		June 21 2022 3:45 pm AKST  | 2022-06-21T15:45-08:00
		Jan 21 2022 3:45 pm AKDT   | 2022-01-21T15:45-09:00
		June 21 2022 3:45 pm HAST  | 2022-06-21T15:45-09:00
		Jan 21 2022 3:45 pm HADT   | 2022-01-21T15:45-10:00
		June 21 2022 3:45 pm HST   | 2022-06-21T15:45-10:00
		""")
	void abbreviationsMeanRegions(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		03-05 2008 7:02:10 +01:00                   | 2008-03-05T07:02:10+01:00
		03-05 2008 7:02:10 +05:00                   | 2008-03-05T07:02:10+05:00
		03-05 2008 7:02:10 (+05:00)                 | 2008-03-05T07:02:10+05:00
		03-05 2008 7:02:10 -01:00                   | 2008-03-05T07:02:10-01:00
		03-05 2008 7:02:10 -05:00                   | 2008-03-05T07:02:10-05:00
		03-05 2008 7:02:10 -5                       | 2008-03-05T07:02:10-05:00
		2008-03-05T07:02:00+01:00                   | 2008-03-05T07:02+01:00
		2008-03-05T07:02:00-10:00                   | 2008-03-05T07:02-10:00
		2008-03-05T07:02:00-00:00                   | 2008-03-05T07:02Z
		2008-03-05T07:02:10.789+00:00               | 2008-03-05T07:02:10.789Z
		2018-10-23T22:21:19.913Z                    | 2018-10-23T22:21:19.913Z
		6/30/2016 10:02:27 GMT-04:00                | 2016-06-30T10:02:27-04:00
		6/30/2016 10:02:27 UTC-4                    | 2016-06-30T10:02:27-04:00
		6/30/2016 10:02:27 AM(UTC-4)                | 2016-06-30T10:02:27-04:00
		6/30/2016 10:02:27.654 AM(UTC-4)            | 2016-06-30T10:02:27.654-04:00
		`'2019/07/19 18:58:15(GMT  -7)`             | 2019-07-19T18:58:15-07:00
		`Fri, 05 Jan 2024 17:22:26 -0700`           | 2024-01-05T17:22:26-07:00
		`2019/12/10 23:24:08 (GMT  -6)\t`           | 2019-12-10T23:24:08-06:00
		2020/08/11 16:09:59(GMT  +0)                | 2020-08-11T16:09:59Z
		2019/11/27 08:49:10 (-08:00)                | 2019-11-27T08:49:10-08:00
		2024-03-05 10:00 +05:30                     | 2024-03-05T10:00+05:30
		2024-03-05 10:00 +5:30                      | 2024-03-05T10:00+05:30
		2024-03-05 10:00 +0530                      | 2024-03-05T10:00+05:30
		2024-03-05 10:00 GMT+05:30                  | 2024-03-05T10:00+05:30
		2024-03-05 10:00 GMT+5:30                   | 2024-03-05T10:00+05:30
		2024-03-05 10:00 UTC+0530                   | 2024-03-05T10:00+05:30
		2024-03-05 10:00 UTC+530                    | 2024-03-05T10:00+05:30
		2024-03-05 10:00 UTC-03                     | 2024-03-05T10:00-03:00
		2024-03-05 10:00 +14:00                     | 2024-03-05T10:00+14:00
		2024-03-05 10:00 -18:00                     | 2024-03-05T10:00-18:00
		3/5/2024 7:02 PM +0530                      | 2024-03-05T19:02+05:30
		2008-03-05T07:02:10-01:00                   | 2008-03-05T07:02:10-01:00
		03-05-2008 7:02:10-01:00                    | 2008-03-05T07:02:10-01:00
		2008-03-05 07:02:10.5+05:30                 | 2008-03-05T07:02:10.5+05:30
		2008-03-05 07:02Z                           | 2008-03-05T07:02Z
		10:00 -0500                                 | 2024-06-15T10:00-05:00
		2008-03-05 07:02 5                          | 2008-03-05T07:02+05:00
		2008-03-05 07:02 0530                       | 2008-03-05T07:02+05:30
		2008-03-05 07:02 1800                       | 2008-03-05T07:02+18:00
		""")
	void parsesOffsets(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"2024-03-05 10:00 +19:00", "2024-03-05 10:00 +25:00", "2024-03-05 10:00 UTC+1900", "2024-03-05 10:00 GMT+05:75", "2024-03-05 10:00 +05:30:00",
		"2024-03-05 10:00 -19", "2024-03-05 10:00 +12345", "2024-03-05 10:00 +ab:cd", "2024-03-05 10:00 GMT+", "2024-03-05 10:00 -05:00 -05:00",
		"2024-03-05 10:00 GMTX-5", "2024-03-05 10:00 19", "2024-03-05 10:00 0510", "2024-03-05 10:00 1900", "2024-03-05 10:00 5 6"})
	void rejectsInvalidOffsets(String text) {
		assertThrows(RosemaryDateTimeException.class, () -> PARSER.parse(text));
	}

	/**
	 * An offset is more precise than a name, so it wins, e.g. in email dates (RFC 2822) that add the zone name as a comment.
	 */
	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
		`Tue, 15 Nov 1994 08:12:31 -0500 (EST)`     | 1994-11-15T08:12:31-05:00
		`Tue, 15 Nov 1994 08:12:31 -0400 (EST)`     | 1994-11-15T08:12:31-04:00
		`Tue, 15 Nov 1994 08:12:31 EST -0400`       | 1994-11-15T08:12:31-04:00
		2019/07/19 18:58:15 GMT -7                  | 2019-07-19T18:58:15-07:00
		2019/07/19 18:58:15 UTC+2 (CET)             | 2019-07-19T18:58:15+02:00
		2019/07/19 18:58:15 PST America/Denver      | 2019-07-19T18:58:15-06:00
		""")
	void prefersOffsetsToNames(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		2019-03-05 America/Denver   | 2019-03-05T00:00-07:00
		March 5 2024 EST            | 2024-03-05T00:00-05:00
		March 5 2024 +05:30         | 2024-03-05T00:00+05:30
		""")
	void usesTheTimeZoneForDatesWithoutTimes(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	/**
	 * ISO 8601 datetimes with a region id in brackets, as written by {@link java.time.ZonedDateTime#toString()}.
	 */
	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
		2008-03-05T07:02:00+01:00[Europe/Paris]    | 2008-03-05T07:02+01:00
		2008-03-05T06:02:00Z[Europe/Paris]         | 2008-03-05T07:02+01:00
		""")
	void parsesIsoDatetimesWithRegionIds(String text, OffsetDateTime expected) {
		assertEquals(expected, PARSER.parse(text));
	}

	@Test
	void usesTodayInTheDatetimesTimeZone() {
		// 2024-06-15T12:00Z is already June 16th in Auckland
		assertEquals(OffsetDateTime.parse("2024-06-16T07:00+12:00"), PARSER.parse("7am Pacific/Auckland"));
		assertEquals(OffsetDateTime.parse("2024-06-15T07:00-06:00"), PARSER.parse("7am America/Denver"));
	}

	@Test
	void utcInTheStringOverridesTheDefaultZone() {
		final RosemaryDateTimeParser denver = PARSER.toBuilder().defaultZone(ZoneId.of("America/Denver")).build();
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00Z"), denver.parse("2024-03-05 10:00 UTC"));
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00-07:00"), denver.parse("2024-03-05 10:00"));
	}

	@Test
	void parsingDoesNotDependOnTheDefaultLocale() {
		final Locale originalLocale = Locale.getDefault();
		try {
			// in Turkish, "i".toUpperCase() is "İ", which used to break matching words like "first"
			for (Locale locale : new Locale[] {Locale.forLanguageTag("tr-TR"), Locale.GERMANY, Locale.JAPAN}) {
				Locale.setDefault(locale);
				final RosemaryDateTimeParser parser = RosemaryDateTimeParser.builder().clock(CLOCK).build();
				assertEquals(OffsetDateTime.parse("1988-03-31T00:00Z"), parser.parse("March Thirty-first 1988"), locale.toString());
				assertEquals(OffsetDateTime.parse("2008-03-05T00:00Z"), parser.parse("The fifth of March 2008"), locale.toString());
				assertEquals(OffsetDateTime.parse("2008-03-05T07:02+10:00"), parser.parse("2008-03-05 07:02 Pacific/Guam"), locale.toString());
				assertEquals(OffsetDateTime.parse("2008-03-05T07:02-05:00"), parser.parse("2008-03-05 07:02 america/indiana/indianapolis"), locale.toString());
				assertEquals(OffsetDateTime.parse("2008-03-05T07:02-08:00"), parser.parse("2008-03-05 07:02 pst"), locale.toString());
				assertEquals(OffsetDateTime.parse("2008-03-05T07:02+01:00"), parser.parse("2008-03-05 07:02 CET"), locale.toString());
			}
		} finally {
			Locale.setDefault(originalLocale);
		}
	}
}
