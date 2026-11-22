package com.lemmingapex.rosemary;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static com.lemmingapex.rosemary.TestParsers.TODAY;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Formats random datetimes in many common formats, and checks that Rosemary reads each string back to the datetime it was made from,
 * without being told the format.  This tests the main promise of pattern-free parsing over many more values than the tables of examples.
 * <p>
 * Each format says what its strings keep: how precise they are, whether they have a time zone, and the date order to read them with.
 * The datetimes are random, but each format has its own fixed seed, so failures can be reproduced, and adding a format doesn't change the
 * datetimes of the others.
 */
class RoundTripTest {

	private static final long SEED = 20261122L;
	private static final int COUNT = 1_000;
	private static final int MAX_REPORTED_FAILURES = 10;

	/**
	 * The smallest unit a format writes.  Smaller fields are not generated, so the string holds the whole datetime.
	 */
	private enum Precision {
		YEAR, MONTH, DAY, MINUTE, SECOND, MILLI, MICRO, NANO;

		boolean has(Precision field) {
			return compareTo(field) >= 0;
		}
	}

	/**
	 * What a format writes for the time zone, and so which time zones the datetimes are generated in.
	 */
	private enum Zones {
		/**
		 * No time zone, so the datetimes are in UTC, the parser's default zone.
		 */
		NONE,
		/**
		 * An offset, like <code>-07:00</code>.  Read back as the same offset.
		 */
		OFFSET,
		/**
		 * A region id, like <code>America/Denver</code>.  Read back as the same region.
		 */
		REGION,
		/**
		 * An abbreviation, like <code>MST</code>, from the regions whose abbreviations Rosemary reads as that region.
		 */
		ABBREVIATION,
		/**
		 * The instant in UTC, like <code>Z</code> or seconds since the epoch.  Read back as the same instant in UTC.
		 */
		INSTANT
	}

	/**
	 * A datetime format to test.
	 *
	 * @param name a short description, shown in the test report
	 * @param formatter writes a datetime in the format
	 * @param precision the smallest unit the format writes
	 * @param zones what the format writes for the time zone
	 * @param minYear the first year to generate
	 * @param maxYear the last year to generate
	 * @param dateOrder the date order to parse with
	 * @param timeOnly <code>true</code> if the format has no date, so it is read as today
	 */
	private record Format(String name, Function<ZonedDateTime, String> formatter, Precision precision, Zones zones, int minYear, int maxYear,
		RosemaryDateOrder dateOrder, boolean timeOnly) {

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * Years for formats with four-digit years and no time zone.  Not before 1600, where 8-digit dates like 12011230 could be MMDDYYYY or YYYYMMDD.
	 */
	private static final int[] FOUR_DIGIT_YEARS = {1600, 2400};
	/**
	 * Years that two-digit years are read as, by default.
	 */
	private static final int[] TWO_DIGIT_YEARS = {1970, 2069};
	/**
	 * Years for formats with a time zone.  Since 1970, offsets are whole minutes in the zones used here.
	 */
	private static final int[] ZONED_YEARS = {1970, 2069};
	/**
	 * Years for formats with time zone abbreviations.  Before 1972, London kept British Standard Time all year, written GMT.
	 */
	private static final int[] ABBREVIATION_YEARS = {1980, 2069};
	/**
	 * Years whose times since the epoch have 9 to 11 digits as seconds, and 12 or 13 digits as milliseconds.
	 */
	private static final int[] EPOCH_YEARS = {1974, 2200};
	/**
	 * The year for formats without a year, which are read as this year.
	 */
	private static final int[] THIS_YEAR = {LocalDate.parse(TODAY).getYear(), LocalDate.parse(TODAY).getYear()};

	/**
	 * Zones for offset formats: whole, half and quarter hour offsets, both sides of UTC, with and without daylight saving time.
	 */
	private static final List<ZoneId> OFFSET_ZONES = Stream.of("UTC", "America/Los_Angeles", "America/Denver", "America/Phoenix",
		"America/Chicago", "America/New_York", "America/St_Johns", "America/Sao_Paulo", "Atlantic/Azores", "Europe/London", "Europe/Paris",
		"Europe/Moscow", "Asia/Kolkata", "Asia/Kathmandu", "Asia/Shanghai", "Asia/Tokyo", "Australia/Adelaide", "Australia/Lord_Howe",
		"Pacific/Chatham", "Pacific/Kiritimati", "Pacific/Pago_Pago", "-12:00", "+14:00", "-09:30", "+05:45")
		.map(ZoneId::of).toList();

	/**
	 * Zones for abbreviation formats, whose Java abbreviations, like PST and PDT, are read as the same region.
	 */
	private static final List<ZoneId> ABBREVIATION_ZONES = Stream.of("America/Los_Angeles", "America/Denver", "America/Chicago",
		"America/New_York", "America/Anchorage", "Pacific/Honolulu", "America/Halifax", "Europe/London", "Europe/Paris", "Asia/Tokyo",
		"Asia/Kolkata", "Australia/Sydney", "UTC")
		.map(ZoneId::of).toList();

	/**
	 * Every region id, except the old ids that are also abbreviations or names for UTC, like <code>EST</code> or <code>GMT</code>.
	 * Rosemary reads those as the abbreviation, e.g. <code>EST</code> is America/New_York, not the fixed offset of the <code>EST</code> id.
	 */
	private static final List<ZoneId> REGION_ZONES = ZoneId.getAvailableZoneIds().stream()
		.filter(id -> !RosemaryDateTimeParser.defaultTimeZoneAbbreviations().containsKey(id.toUpperCase(Locale.ROOT)))
		.filter(id -> !TimeZoneNames.UTC_NAMES.contains(id.toUpperCase(Locale.ROOT)))
		.sorted()
		.map(ZoneId::of)
		.toList();

	static Stream<Format> formats() {
		final RosemaryDateOrder mdy = RosemaryDateOrder.MM_DD_YY;
		final RosemaryDateOrder dmy = RosemaryDateOrder.DD_MM_YY;
		final RosemaryDateOrder ymd = RosemaryDateOrder.YY_MM_DD;
		return Stream.of(
			// ISO 8601 and RFC 3339, as written by java.time and most APIs
			format("ISO local date", DateTimeFormatter.ISO_LOCAL_DATE, Precision.DAY, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			format("ISO local datetime", DateTimeFormatter.ISO_LOCAL_DATE_TIME, Precision.NANO, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			format("ISO offset datetime", DateTimeFormatter.ISO_OFFSET_DATE_TIME, Precision.NANO, Zones.OFFSET, ZONED_YEARS, mdy),
			format("ISO zoned datetime, ZonedDateTime.toString()", DateTimeFormatter.ISO_ZONED_DATE_TIME, Precision.NANO, Zones.REGION, ZONED_YEARS, mdy),
			format("ISO instant, Instant.toString()", DateTimeFormatter.ISO_INSTANT, Precision.NANO, Zones.INSTANT, ZONED_YEARS, mdy),
			pattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXX", Precision.MILLI, Zones.OFFSET, ZONED_YEARS, mdy),
			pattern("uuuuMMdd'T'HHmmssX", Precision.SECOND, Zones.OFFSET, ZONED_YEARS, mdy),
			pattern("uuuuMMdd", Precision.DAY, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("uuuuMMddHHmmss", Precision.SECOND, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("uuuu-MM", Precision.MONTH, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("uuuu", Precision.YEAR, Zones.NONE, FOUR_DIGIT_YEARS, mdy),

			// databases and logs
			pattern("uuuu-MM-dd HH:mm:ss", Precision.SECOND, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("uuuu-MM-dd HH:mm:ss.SSS", Precision.MILLI, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("uuuu-MM-dd HH:mm:ss,SSS", Precision.MILLI, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("uuuu-MM-dd HH:mm:ss.SSSSSS", Precision.MICRO, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("uuuu-MM-dd HH:mm:ss xx", Precision.SECOND, Zones.OFFSET, ZONED_YEARS, mdy),
			pattern("uuuu-MM-dd HH:mm:ss VV", Precision.SECOND, Zones.REGION, ZONED_YEARS, mdy),
			pattern("uuuu-MM-dd HH:mm:ss zzz", Precision.SECOND, Zones.ABBREVIATION, ABBREVIATION_YEARS, mdy),
			pattern("uuuu-MM-dd HH:mm:ss.SSSSSSSSS xx zzz", Precision.NANO, Zones.ABBREVIATION, ABBREVIATION_YEARS, mdy),
			pattern("dd/MMM/uuuu:HH:mm:ss xx", Precision.SECOND, Zones.OFFSET, ZONED_YEARS, mdy),
			pattern("MMM d HH:mm:ss", Precision.SECOND, Zones.NONE, THIS_YEAR, mdy),
			pattern("uuuu:MM:dd HH:mm:ss", Precision.SECOND, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("dd-MMM-uuuu HH:mm:ss", Precision.SECOND, Zones.NONE, FOUR_DIGIT_YEARS, mdy),

			// email, HTTP, and the toString() of dates in Java, JavaScript and git
			format("RFC 1123", DateTimeFormatter.RFC_1123_DATE_TIME, Precision.SECOND, Zones.OFFSET, ZONED_YEARS, mdy),
			pattern("EEE, d MMM uuuu HH:mm:ss xx", Precision.SECOND, Zones.OFFSET, ZONED_YEARS, mdy),
			pattern("EEE, dd MMM uuuu HH:mm:ss xx '('zzz')'", Precision.SECOND, Zones.ABBREVIATION, ABBREVIATION_YEARS, mdy),
			pattern("EEEE, dd-MMM-yy HH:mm:ss 'GMT'", Precision.SECOND, Zones.NONE, TWO_DIGIT_YEARS, mdy),
			pattern("EEE MMM d HH:mm:ss uuuu", Precision.SECOND, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("EEE MMM dd HH:mm:ss zzz uuuu", Precision.SECOND, Zones.ABBREVIATION, ABBREVIATION_YEARS, mdy),
			pattern("EEE MMM dd uuuu HH:mm:ss 'GMT'xx", Precision.SECOND, Zones.OFFSET, ZONED_YEARS, mdy),
			pattern("EEE MMM d HH:mm:ss uuuu xx", Precision.SECOND, Zones.OFFSET, ZONED_YEARS, mdy),

			// month, day, year
			pattern("MM/dd/uuuu", Precision.DAY, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("M/d/uuuu h:mm a", Precision.MINUTE, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("M/d/yy h:mm:ss a", Precision.SECOND, Zones.NONE, TWO_DIGIT_YEARS, mdy),
			pattern("MM-dd-yy HH:mm", Precision.MINUTE, Zones.NONE, TWO_DIGIT_YEARS, mdy),

			// day, month, year
			pattern("dd/MM/uuuu HH:mm:ss", Precision.SECOND, Zones.NONE, FOUR_DIGIT_YEARS, dmy),
			pattern("dd.MM.uuuu HH:mm", Precision.MINUTE, Zones.NONE, FOUR_DIGIT_YEARS, dmy),
			pattern("d.M.yy", Precision.DAY, Zones.NONE, TWO_DIGIT_YEARS, dmy),
			pattern("dd-MM-yy", Precision.DAY, Zones.NONE, TWO_DIGIT_YEARS, dmy),

			// year, month, day
			pattern("yy/MM/dd HH:mm", Precision.MINUTE, Zones.NONE, TWO_DIGIT_YEARS, ymd),

			// month names
			pattern("MMMM d, uuuu", Precision.DAY, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("MMMM d, uuuu h:mm a", Precision.MINUTE, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("MMM d, uuuu h:mm:ss a", Precision.SECOND, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("d MMMM uuuu HH:mm", Precision.MINUTE, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("d MMM uuuu", Precision.DAY, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("dd-MMM-yy", Precision.DAY, Zones.NONE, TWO_DIGIT_YEARS, mdy),
			pattern("MMMM uuuu", Precision.MONTH, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			pattern("EEEE, MMMM d, uuuu 'at' h:mm a", Precision.MINUTE, Zones.NONE, FOUR_DIGIT_YEARS, mdy),
			new Format("MMMM d'th', uuuu, with the day as an ordinal", RoundTripTest::ordinalDate, Precision.DAY, Zones.NONE,
				FOUR_DIGIT_YEARS[0], FOUR_DIGIT_YEARS[1], mdy, false),

			// times only, which are today
			time("h:mm a", Precision.MINUTE),
			time("h a", Precision.MINUTE),
			time("HH:mm:ss.SSS", Precision.MILLI),

			// times since the epoch
			new Format("seconds since the epoch", dateTime -> String.valueOf(dateTime.toEpochSecond()), Precision.SECOND, Zones.INSTANT,
				EPOCH_YEARS[0], EPOCH_YEARS[1], mdy, false),
			new Format("milliseconds since the epoch", dateTime -> String.valueOf(dateTime.toInstant().toEpochMilli()), Precision.MILLI,
				Zones.INSTANT, EPOCH_YEARS[0], EPOCH_YEARS[1], mdy, false)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("formats")
	void readsFormattedDateTimes(Format format) {
		final Random random = new Random(SEED ^ format.name().hashCode());
		final List<String> failures = new ArrayList<>();
		int failed = 0;
		for (int i = 0; i < COUNT; i++) {
			final ZonedDateTime dateTime = randomDateTime(format, random);
			final String text = format.formatter().apply(dateTime);
			final String failure = check(format, dateTime, text);
			if (failure != null) {
				failed++;
				if (failures.size() < MAX_REPORTED_FAILURES) {
					failures.add(failure);
				}
			}
		}
		if (failed > 0) {
			fail(failed + " of " + COUNT + " " + format + " strings were misread, e.g.\n  " + String.join("\n  ", failures));
		}
	}

	/**
	 * @return a description of the problem, or <code>null</code> if the string was read correctly
	 */
	private static String check(Format format, ZonedDateTime dateTime, String text) {
		final OffsetDateTime expected = format.zones() == Zones.INSTANT ? dateTime.toInstant().atOffset(ZoneOffset.UTC) : dateTime.toOffsetDateTime();
		try {
			final OffsetDateTime actual = PARSER.parse(text, format.dateOrder());
			if (!expected.equals(actual)) {
				return "\"" + text + "\" -> " + actual + ", expected " + expected;
			}
			if (format.zones() == Zones.REGION) {
				final ZoneId zone = PARSER.parseZoned(text, format.dateOrder()).getZone();
				if (!zone.equals(dateTime.getZone())) {
					return "\"" + text + "\" -> zone " + zone + ", expected " + dateTime.getZone();
				}
			}
			return null;
		} catch (RuntimeException e) {
			return "\"" + text + "\" -> " + e;
		}
	}

	private static ZonedDateTime randomDateTime(Format format, Random random) {
		final Precision precision = format.precision();
		final LocalDate date;
		if (format.timeOnly()) {
			date = LocalDate.parse(TODAY);
		} else {
			final int year = format.minYear() + random.nextInt(format.maxYear() - format.minYear() + 1);
			final int month = precision.has(Precision.MONTH) ? 1 + random.nextInt(12) : 1;
			final int day = precision.has(Precision.DAY) ? 1 + random.nextInt(YearMonth.of(year, month).lengthOfMonth()) : 1;
			date = LocalDate.of(year, month, day);
		}
		final LocalTime time;
		if (precision.has(Precision.MINUTE)) {
			final int hour = random.nextInt(24);
			// "h a" has no minutes, but is precise to the hour, so it's tested as minutes on the hour
			final int minute = format.name().equals("h a") ? 0 : random.nextInt(60);
			final int second = precision.has(Precision.SECOND) ? random.nextInt(60) : 0;
			final int nano = switch (precision) {
				case MILLI -> random.nextInt(1_000) * 1_000_000;
				case MICRO -> random.nextInt(1_000_000) * 1_000;
				case NANO -> random.nextInt(1_000_000_000);
				default -> 0;
			};
			time = LocalTime.of(hour, minute, second, nano);
		} else {
			time = LocalTime.MIDNIGHT;
		}
		final LocalDateTime localDateTime = date.atTime(time);
		return switch (format.zones()) {
			case NONE -> localDateTime.atZone(ZoneOffset.UTC);
			case ABBREVIATION -> localDateTime.atZone(pick(ABBREVIATION_ZONES, random));
			case REGION -> localDateTime.atZone(pick(REGION_ZONES, random));
			case OFFSET, INSTANT -> {
				// in the hour that is repeated when clocks go back, use either offset, since the string has the offset
				final ZonedDateTime zoned = localDateTime.atZone(pick(OFFSET_ZONES, random));
				yield random.nextBoolean() ? zoned.withLaterOffsetAtOverlap() : zoned;
			}
		};
	}

	private static <T> T pick(List<T> values, Random random) {
		return values.get(random.nextInt(values.size()));
	}

	private static Format format(String name, DateTimeFormatter formatter, Precision precision, Zones zones, int[] years, RosemaryDateOrder dateOrder) {
		return new Format(name, formatter::format, precision, zones, years[0], years[1], dateOrder, false);
	}

	private static Format pattern(String pattern, Precision precision, Zones zones, int[] years, RosemaryDateOrder dateOrder) {
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.US);
		return new Format(pattern, formatter::format, precision, zones, years[0], years[1], dateOrder, false);
	}

	private static Format time(String pattern, Precision precision) {
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.US);
		return new Format(pattern, formatter::format, precision, Zones.NONE, THIS_YEAR[0], THIS_YEAR[1], RosemaryDateOrder.MM_DD_YY, true);
	}

	/**
	 * Write a date with the day as an ordinal, like <code>March 5th, 2024</code> or <code>June 22nd, 2028</code>.
	 */
	private static String ordinalDate(ZonedDateTime dateTime) {
		final int day = dateTime.getDayOfMonth();
		final String suffix = (day % 100 >= 11 && day % 100 <= 13) ? "th" : switch (day % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		};
		return DateTimeFormatter.ofPattern("MMMM ", Locale.US).format(dateTime) + day + suffix + ", " + dateTime.getYear();
	}
}
