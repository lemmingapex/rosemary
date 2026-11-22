package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static com.lemmingapex.rosemary.TestParsers.CLOCK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fields found while parsing, and how missing fields are filled in.
 */
class DateTimeFieldsTest {

	private static DateTimeFields fields() {
		return new DateTimeFields(RosemaryDateOrder.MM_DD_YY);
	}

	private static ZonedDateTime resolve(DateTimeFields fields) throws ParseFailure {
		return fields.resolve(CLOCK, ZoneOffset.UTC, 1970);
	}

	@Test
	void appliesAmAndPmToTheNextHour() throws ParseFailure {
		final DateTimeFields fields = fields();
		fields.setPendingMeridian(true);
		fields.setHour(12);
		assertEquals(12, fields.hour());

		fields.setPendingMeridian(false);
		fields.setHour(12);
		assertEquals(0, fields.hour());

		fields.setPendingMeridian(true);
		fields.setHour(7);
		assertEquals(19, fields.hour());

		// only the next hour
		fields.setHour(7);
		assertEquals(7, fields.hour());

		// a 24-hour value is kept
		fields.setPendingMeridian(false);
		fields.setHour(13);
		assertEquals(13, fields.hour());
	}

	@Test
	void appliesAmAndPmToTheHourThatWasSet() throws ParseFailure {
		final DateTimeFields fields = fields();
		fields.setHour(12);
		fields.applyMeridianToHour(false);
		assertEquals(0, fields.hour());

		fields.setHour(12);
		fields.applyMeridianToHour(true);
		assertEquals(12, fields.hour());

		fields.setHour(13);
		fields.applyMeridianToHour(true);
		assertEquals(13, fields.hour());
		assertThrows(ParseFailure.class, () -> fields.applyMeridianToHour(false));
	}

	@Test
	void rejectsValuesOutOfRange() {
		final DateTimeFields fields = fields();
		assertThrows(ParseFailure.class, () -> fields.setYear(-1, false));
		assertThrows(ParseFailure.class, () -> fields.setYear(10000, false));
		assertThrows(ParseFailure.class, () -> fields.setMonth(0));
		assertThrows(ParseFailure.class, () -> fields.setMonth(13));
		assertThrows(ParseFailure.class, () -> fields.setDay(0));
		assertThrows(ParseFailure.class, () -> fields.setDay(32));
		assertThrows(ParseFailure.class, () -> fields.setHour(-1));
		assertThrows(ParseFailure.class, () -> fields.setHour(24));
		assertThrows(ParseFailure.class, () -> fields.setMinute(60));
		assertThrows(ParseFailure.class, () -> fields.setSecond(60));
		assertThrows(ParseFailure.class, () -> fields.setNano(1_000_000_000));
		assertThrows(ParseFailure.class, () -> fields.setNano(-1));
		assertTrue(fields.isEmpty());
	}

	@Test
	void rejectsNoFields() {
		assertThrows(ParseFailure.class, () -> resolve(fields()));
	}

	@Test
	void fillsInMissingFields() throws ParseFailure {
		final DateTimeFields monthOnly = fields();
		monthOnly.setMonth(3);
		assertEquals(ZonedDateTime.parse("2024-03-01T00:00Z"), resolve(monthOnly));

		final DateTimeFields yearOnly = fields();
		yearOnly.setYear(2008, false);
		assertEquals(ZonedDateTime.parse("2008-01-01T00:00Z"), resolve(yearOnly));

		final DateTimeFields dayOnly = fields();
		dayOnly.setDay(5);
		assertEquals(ZonedDateTime.parse("2024-06-05T00:00Z"), resolve(dayOnly));

		final DateTimeFields hourOnly = fields();
		hourOnly.setHour(7);
		assertEquals(ZonedDateTime.parse("2024-06-15T07:00Z"), resolve(hourOnly));

		final DateTimeFields secondsOnly = fields();
		secondsOnly.setNano(5);
		assertEquals(ZonedDateTime.parse("2024-06-15T00:00:00.000000005Z"), resolve(secondsOnly));
	}

	@Test
	void usesItsZoneForToday() throws ParseFailure {
		final DateTimeFields fields = fields();
		fields.setHour(7);
		fields.setZoneName(ZoneId.of("Pacific/Kiritimati"));
		// 2024-06-15T12:00Z is 2024-06-16T02:00 in Kiritimati
		assertEquals(ZonedDateTime.of(2024, 6, 16, 7, 0, 0, 0, ZoneId.of("Pacific/Kiritimati")), resolve(fields));
	}

	@Test
	void placesTwoDigitYears() throws ParseFailure {
		final DateTimeFields fields = fields();
		fields.setYear(69, true);
		fields.setMonth(1);
		fields.setDay(1);
		assertEquals(2069, fields.resolve(CLOCK, ZoneOffset.UTC, 1970).getYear());
		assertEquals(1969, fields.resolve(CLOCK, ZoneOffset.UTC, 1969).getYear());
		assertEquals(69, fields.resolve(CLOCK, ZoneOffset.UTC, 0).getYear());
		assertEquals(9969, fields.resolve(CLOCK, ZoneOffset.UTC, 9900).getYear());
		assertTrue(fields.isTwoDigitYear());

		fields.setYear(69, false);
		assertEquals(69, fields.resolve(CLOCK, ZoneOffset.UTC, 1970).getYear());
		assertFalse(fields.isTwoDigitYear());
	}

	@Test
	void prefersOffsetsToNames() throws ParseFailure {
		final DateTimeFields nameFirst = fields();
		nameFirst.setZoneName(ZoneId.of("America/New_York"));
		nameFirst.setZoneOffset(ZoneOffset.ofHours(-4));
		assertEquals(ZoneOffset.ofHours(-4), nameFirst.zone());

		final DateTimeFields offsetFirst = fields();
		offsetFirst.setZoneOffset(ZoneOffset.ofHours(-4));
		offsetFirst.setZoneName(ZoneId.of("America/New_York"));
		assertEquals(ZoneOffset.ofHours(-4), offsetFirst.zone());
		assertTrue(offsetFirst.isOffsetSet());

		// a second offset is an error
		assertThrows(ParseFailure.class, () -> offsetFirst.setZoneOffset(ZoneOffset.ofHours(-5)));

		// a later name replaces an earlier name
		final DateTimeFields twoNames = fields();
		twoNames.setZoneName(ZoneId.of("America/New_York"));
		twoNames.setZoneName(ZoneId.of("America/Denver"));
		assertEquals(ZoneId.of("America/Denver"), twoNames.zone());
		assertFalse(twoNames.isOffsetSet());
	}

	@Test
	void tracksWhetherTheYearWasWrittenFirst() throws ParseFailure {
		final DateTimeFields yearFirst = fields();
		yearFirst.setYear(2024, false);
		assertTrue(yearFirst.isYearWrittenFirst());

		final DateTimeFields yearLast = fields();
		yearLast.setMonth(3);
		yearLast.setYear(2024, false);
		assertFalse(yearLast.isYearWrittenFirst());

		final DateTimeFields twoDigitYear = fields();
		twoDigitYear.setYear(24, true);
		assertFalse(twoDigitYear.isYearWrittenFirst());
	}

	@Test
	void rejectsInvalidDates() throws ParseFailure {
		final DateTimeFields fields = fields();
		fields.setYear(2023, false);
		fields.setMonth(2);
		fields.setDay(29);
		assertThrows(ParseFailure.class, () -> resolve(fields));
	}
}
