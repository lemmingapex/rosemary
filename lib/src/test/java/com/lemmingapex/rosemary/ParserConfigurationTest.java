package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;

import java.util.List;
import static com.lemmingapex.rosemary.TestParsers.CLOCK;
import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Configuring a parser with {@link RosemaryDateTimeParser.Builder}.
 */
class ParserConfigurationTest {

	@Test
	void hasDefaults() {
		final RosemaryDateTimeParser parser = new RosemaryDateTimeParser();
		assertEquals(RosemaryDateOrder.MM_DD_YY, parser.getDateOrder());
		assertEquals(ZoneOffset.UTC, parser.getDefaultZone());
		assertEquals(Clock.systemUTC(), parser.getClock());
		assertEquals(Locale.ENGLISH, parser.getLocale());
		assertEquals(RosemaryDateTimeParser.DEFAULT_TWO_DIGIT_YEAR_START, parser.getTwoDigitYearStart());
		assertEquals(RosemaryDateTimeParser.defaultTimeZoneAbbreviations(), parser.getTimeZoneAbbreviations());
	}

	@Test
	void usesTheClockForToday() {
		final RosemaryDateTimeParser parser = RosemaryDateTimeParser.builder().clock(Clock.fixed(Instant.parse("2030-03-01T00:00:00Z"), ZoneOffset.UTC)).build();
		assertEquals(OffsetDateTime.parse("2030-03-01T19:02Z"), parser.parse("7:02 pm"));
		assertEquals(OffsetDateTime.parse("2030-06-19T00:00Z"), parser.parse("6/19"));
		assertEquals(OffsetDateTime.parse("2030-03-05T00:00Z"), parser.parse("5th"));
	}

	@Test
	void usesTheDefaultZone() {
		final RosemaryDateTimeParser parser = RosemaryDateTimeParser.builder()
			.clock(Clock.fixed(Instant.parse("2030-03-19T12:00:00Z"), ZoneOffset.UTC))
			.defaultZone(ZoneId.of("America/Denver"))
			.build();
		assertEquals(OffsetDateTime.parse("2030-03-19T14:23:00.111-06:00"), parser.parse("14:23:00.111"));
		assertEquals(OffsetDateTime.parse("2030-01-19T14:23-07:00"), parser.parse("1/19/2030 14:23"));
		assertEquals(OffsetDateTime.parse("2030-01-19T14:23+09:00"), parser.parse("1/19/2030 14:23 JST"));
	}

	@Test
	void placesTwoDigitYears() {
		assertEquals(OffsetDateTime.parse("1970-03-05T00:00Z"), PARSER.parse("3/5/70"));
		assertEquals(OffsetDateTime.parse("2069-03-05T00:00Z"), PARSER.parse("3/5/69"));
		assertEquals(OffsetDateTime.parse("2000-03-05T00:00Z"), PARSER.parse("3/5/00"));

		final RosemaryDateTimeParser parser = PARSER.toBuilder().twoDigitYearStart(1950).build();
		assertEquals(OffsetDateTime.parse("1950-03-05T00:00Z"), parser.parse("3/5/50"));
		assertEquals(OffsetDateTime.parse("2049-03-05T00:00Z"), parser.parse("3/5/49"));
		assertEquals(1950, parser.getTwoDigitYearStart());

		assertThrows(IllegalArgumentException.class, () -> RosemaryDateTimeParser.builder().twoDigitYearStart(-1));
		assertThrows(IllegalArgumentException.class, () -> RosemaryDateTimeParser.builder().twoDigitYearStart(9901));
	}

	@Test
	void onlyPlacesYearsWrittenWithTwoDigits() {
		assertEquals(OffsetDateTime.parse("0050-03-05T00:00Z"), PARSER.parse("March 5 0050"));
		assertEquals(OffsetDateTime.parse("2050-03-05T00:00Z"), PARSER.parse("March 5 50"));
	}

	@Test
	void addsTimeZoneAbbreviations() {
		final RosemaryDateTimeParser parser = PARSER.toBuilder().timeZoneAbbreviation("SGT", ZoneId.of("Asia/Singapore")).build();
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00+08:00"), parser.parse("2024-03-05 10:00 SGT"));
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00+08:00"), parser.parse("2024-03-05 10:00 sgt"));
		assertEquals(ZoneId.of("Asia/Singapore"), parser.getTimeZoneAbbreviations().get("SGT"));
		// the defaults are still there
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00-08:00"), parser.parse("2024-03-05 10:00 PST"));
		// but not in other parsers
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00Z"), PARSER.parse("2024-03-05 10:00 SGT"));
	}

	@Test
	void changesTimeZoneAbbreviations() {
		final RosemaryDateTimeParser parser = PARSER.toBuilder().timeZoneAbbreviation("cst", ZoneId.of("Asia/Shanghai")).build();
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00+08:00"), parser.parse("2024-03-05 10:00 CST"));
		// the map is case-insensitive
		assertEquals(ZoneId.of("Asia/Shanghai"), parser.getTimeZoneAbbreviations().get("cst"));
		assertEquals(ZoneId.of("Asia/Shanghai"), parser.getTimeZoneAbbreviations().get("CST"));
		assertEquals(RosemaryDateTimeParser.defaultTimeZoneAbbreviations().size(), parser.getTimeZoneAbbreviations().size());
	}

	@Test
	void replacesTimeZoneAbbreviations() {
		final RosemaryDateTimeParser parser = PARSER.toBuilder().timeZoneAbbreviations(Map.of("Eastern", ZoneId.of("America/New_York"))).build();
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00-05:00"), parser.parse("2024-03-05 10:00 eastern"));
		assertEquals(Map.of("Eastern", ZoneId.of("America/New_York")), parser.getTimeZoneAbbreviations());
		// PST is no longer recognized, so it is ignored
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00Z"), parser.parse("2024-03-05 10:00 PST"));
		// region ids and UTC are always recognized
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00-08:00"), parser.parse("2024-03-05 10:00 America/Los_Angeles"));
		assertEquals(OffsetDateTime.parse("2024-03-05T10:00Z"), parser.parse("2024-03-05 10:00 UTC"));

		final RosemaryDateTimeParser none = PARSER.toBuilder().timeZoneAbbreviations(Map.of()).build();
		assertTrue(none.getTimeZoneAbbreviations().isEmpty());
	}

	@Test
	void rejectsBadTimeZoneAbbreviations() {
		final RosemaryDateTimeParser.Builder builder = RosemaryDateTimeParser.builder();
		final ZoneId zone = ZoneId.of("Asia/Singapore");
		assertThrows(IllegalArgumentException.class, () -> builder.timeZoneAbbreviation("", zone));
		assertThrows(IllegalArgumentException.class, () -> builder.timeZoneAbbreviation("S GT", zone));
		assertThrows(IllegalArgumentException.class, () -> builder.timeZoneAbbreviation("utc", zone));
		assertThrows(IllegalArgumentException.class, () -> builder.timeZoneAbbreviation("Z", zone));
		assertThrows(IllegalArgumentException.class, () -> builder.timeZoneAbbreviations(Map.of("GMT", zone)));
		assertThrows(NullPointerException.class, () -> builder.timeZoneAbbreviation(null, zone));
		assertThrows(NullPointerException.class, () -> builder.timeZoneAbbreviation("SGT", null));
		assertThrows(NullPointerException.class, () -> builder.timeZoneAbbreviations(null));
	}

	@Test
	void rejectsNullSettings() {
		final RosemaryDateTimeParser.Builder builder = RosemaryDateTimeParser.builder();
		assertThrows(NullPointerException.class, () -> builder.dateOrder(null));
		assertThrows(NullPointerException.class, () -> builder.defaultZone(null));
		assertThrows(NullPointerException.class, () -> builder.clock(null));
		assertThrows(NullPointerException.class, () -> builder.locale(null));
		assertThrows(NullPointerException.class, () -> PARSER.parse("6/19", (RosemaryDateOrder) null));
	}

	@Test
	void copiesSettingsWithToBuilder() {
		final RosemaryDateTimeParser parser = RosemaryDateTimeParser.builder()
			.dateOrder(RosemaryDateOrder.DD_MM_YY)
			.defaultZone(ZoneId.of("Europe/Paris"))
			.clock(CLOCK)
			.locale(Locale.FRENCH)
			.twoDigitYearStart(1950)
			.timeZoneAbbreviation("SGT", ZoneId.of("Asia/Singapore"))
			.build();
		final RosemaryDateTimeParser copy = parser.toBuilder().build();
		assertNotSame(parser, copy);
		assertEquals(parser.toString(), copy.toString());
		assertEquals(RosemaryDateOrder.DD_MM_YY, copy.getDateOrder());
		assertEquals(ZoneId.of("Europe/Paris"), copy.getDefaultZone());
		assertEquals(CLOCK, copy.getClock());
		assertEquals(Locale.FRENCH, copy.getLocale());
		assertEquals(1950, copy.getTwoDigitYearStart());
		assertEquals(parser.getTimeZoneAbbreviations(), copy.getTimeZoneAbbreviations());

		// changing the copy's builder doesn't change the original
		final RosemaryDateTimeParser changed = parser.toBuilder().dateOrder(RosemaryDateOrder.YY_MM_DD).timeZoneAbbreviation("XYZ", ZoneOffset.UTC).build();
		assertEquals(RosemaryDateOrder.DD_MM_YY, parser.getDateOrder());
		assertFalse(parser.getTimeZoneAbbreviations().containsKey("XYZ"));
		assertTrue(changed.getTimeZoneAbbreviations().containsKey("XYZ"));
	}

	@Test
	void abbreviationMapsCannotBeChanged() {
		assertThrows(UnsupportedOperationException.class, () -> RosemaryDateTimeParser.defaultTimeZoneAbbreviations().put("SGT", ZoneOffset.UTC));
		assertThrows(UnsupportedOperationException.class, () -> PARSER.getTimeZoneAbbreviations().clear());
	}

	@Test
	void defaultTimeZoneAbbreviationsAreCurrent() {
		final Map<String, ZoneId> abbreviations = RosemaryDateTimeParser.defaultTimeZoneAbbreviations();
		assertEquals(ZoneId.of("America/Los_Angeles"), abbreviations.get("PST"));
		assertEquals(ZoneId.of("America/Chicago"), abbreviations.get("CST"));
		assertEquals(ZoneId.of("Asia/Kolkata"), abbreviations.get("IST"));
		assertFalse(abbreviations.containsKey("UTC"));
		assertFalse(abbreviations.containsKey("GMT"));
		// abbreviations dropped from current time zone data aren't included
		assertFalse(abbreviations.containsKey("MART"));
		assertFalse(abbreviations.containsKey("NOVT"));
	}

	@Test
	void describesItself() {
		assertTrue(PARSER.toString().contains("dateOrder=MM_DD_YY"));
		assertTrue(PARSER.toString().contains("defaultZone=Z"));
	}

	@Test
	void rejectsAbbreviationsThatCanNotBeFound() {
		for (String abbreviation : new String[] {"MAR", "may", "SAT", "Wed", "AM", "p.m.", "NOON", "1ST", "First", "123", "07:00", "0702Z", "+05", "GMT+1",
			"UTC+X", "A,B", "A(B)", "A'B", "A\"B", "X1-2", "A/1", "1.2"}) {
			assertThrows(IllegalArgumentException.class, () -> PARSER.toBuilder().timeZoneAbbreviation(abbreviation, ZoneId.of("Europe/Paris")), abbreviation);
			assertThrows(IllegalArgumentException.class, () -> PARSER.toBuilder().timeZoneAbbreviations(Map.of(abbreviation, ZoneId.of("Europe/Paris"))), abbreviation);
		}
		// names with dashes, slashes and digits that stay one token are fine
		for (String abbreviation : new String[] {"Eastern-Time", "Mountain/Standard", "MSK3", "A.B."}) {
			final RosemaryDateTimeParser parser = PARSER.toBuilder().timeZoneAbbreviation(abbreviation, ZoneId.of("Europe/Paris")).build();
			assertEquals(ZoneId.of("Europe/Paris"), parser.findTimeZone(abbreviation).orElseThrow(), abbreviation);
			assertEquals(OffsetDateTime.parse("2024-03-05T10:00+01:00"), parser.parse("2024-03-05 10:00 " + abbreviation), abbreviation);
		}
	}

	@Test
	void defaultAbbreviationsCanBeFound() {
		RosemaryDateTimeParser.defaultTimeZoneAbbreviations().forEach((abbreviation, zone) -> {
			assertFalse(PatternFreeParser.isDateTimeWord(abbreviation, DateNames.ENGLISH), abbreviation);
			assertEquals(List.of(abbreviation), Tokenizer.tokenize(abbreviation, DateNames.ENGLISH).stream().map(Tokenizer.Token::text).toList());
		});
	}
}
