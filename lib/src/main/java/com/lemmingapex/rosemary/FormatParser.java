package com.lemmingapex.rosemary;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.List;
import java.util.Locale;

/**
 * Parses datetimes with {@link DateTimeFormatter} patterns.
 */
final class FormatParser {

	private FormatParser() {
	}

	/**
	 * Time fields that are left over when a pattern has part of a time, like an hour of AM or PM without AM or PM.
	 */
	private static final List<ChronoField> TIME_FIELDS = List.of(ChronoField.HOUR_OF_DAY, ChronoField.CLOCK_HOUR_OF_DAY, ChronoField.HOUR_OF_AMPM,
		ChronoField.CLOCK_HOUR_OF_AMPM, ChronoField.AMPM_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND,
		ChronoField.MICRO_OF_SECOND, ChronoField.MILLI_OF_SECOND);

	/**
	 * Parse a datetime with the first of the patterns that matches.
	 * <p>
	 * The pattern doesn't need to include every field, and missing fields are filled in like pattern-free parsing does:
	 * <ul>
	 * <li>a missing time zone is the default zone, and a time zone without a time or without a date is kept</li>
	 * <li>a missing time is midnight</li>
	 * <li>a month without a day is the first of the month, and a year alone is January 1st</li>
	 * <li>a missing year, month or day comes from today's date, according to the clock, in the datetime's time zone</li>
	 * </ul>
	 * Dates are checked strictly, so February 31st doesn't match.
	 *
	 * @param text the datetime string
	 * @param patterns {@link DateTimeFormatter} patterns to try, in order
	 * @param locale locale for names, like month names
	 * @param defaultZone time zone to use if the pattern doesn't have one
	 * @param clock the clock that provides today's date
	 * @return the datetime
	 * @throws ParseFailure if none of the patterns match, with the furthest position any pattern reached, or why the first pattern that
	 * matched the text isn't a valid datetime
	 * @throws IllegalArgumentException if a pattern is invalid
	 */
	static ZonedDateTime parse(String text, List<String> patterns, Locale locale, ZoneId defaultZone, Clock clock) throws ParseFailure {
		int furthestIndex = 0;
		ParseFailure invalid = null;
		for (String pattern : patterns) {
			final TemporalAccessor parsed;
			try {
				parsed = formatter(pattern, locale).parse(text);
			} catch (DateTimeParseException e) {
				// try the next pattern
				furthestIndex = Math.max(furthestIndex, e.getErrorIndex());
				continue;
			}
			try {
				return toZonedDateTime(parsed, defaultZone, clock);
			} catch (ParseFailure e) {
				// the text matches the pattern, but isn't a valid datetime; try the next pattern
				if (invalid == null) {
					invalid = e;
				}
			}
		}
		if (invalid != null) {
			throw invalid;
		}
		throw new ParseFailure("Doesn't match " + (patterns.size() == 1 ? "the pattern " : "any of the patterns ") + String.join(", ", patterns)).at(furthestIndex);
	}

	/**
	 * Parse an ISO 8601 datetime with an offset and a region id, like <code>2008-03-05T07:02:00+01:00[Europe/Paris]</code>,
	 * as written by {@link ZonedDateTime#toString()}.  Pattern-free parsing handles the other ISO 8601 formats.
	 *
	 * @param text the datetime string
	 * @return the datetime, or <code>null</code> if it isn't an ISO 8601 datetime with a time zone
	 */
	static ZonedDateTime parseIsoWithZone(String text) {
		try {
			return ZonedDateTime.parse(text.strip(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/**
	 * Create a formatter that checks dates strictly, instead of changing February 31st to February 29th.  Strict checking needs an era
	 * for year-of-era patterns like yyyy, so the era defaults to the current era.
	 */
	private static DateTimeFormatter formatter(String pattern, Locale locale) {
		return new DateTimeFormatterBuilder()
			.appendPattern(pattern)
			.parseDefaulting(ChronoField.ERA, 1)
			.toFormatter(locale)
			.withResolverStyle(ResolverStyle.STRICT);
	}

	/**
	 * Build the datetime from the parsed fields, filling in the missing ones.
	 *
	 * @throws ParseFailure if there is no date or time, the time is incomplete, or the date isn't valid
	 */
	private static ZonedDateTime toZonedDateTime(TemporalAccessor parsed, ZoneId defaultZone, Clock clock) throws ParseFailure {
		final LocalDate date = parsed.query(TemporalQueries.localDate());
		final LocalTime time = parsed.query(TemporalQueries.localTime());
		final ZoneId zone = parsed.query(TemporalQueries.zone());
		try {
			if (date != null && time != null && zone != null) {
				return ZonedDateTime.from(parsed);
			}
			if (time == null && TIME_FIELDS.stream().anyMatch(parsed::isSupported)) {
				throw new ParseFailure("Incomplete time, like an hour of AM or PM without AM or PM");
			}
			final ZoneId resolvedZone = zone != null ? zone : defaultZone;
			final LocalDateTime local = LocalDateTime.of(date != null ? date : missingDate(parsed, time != null, clock, resolvedZone),
				time != null ? time : LocalTime.MIDNIGHT);
			final ZoneOffset offset = parsed.isSupported(ChronoField.OFFSET_SECONDS) ? ZoneOffset.ofTotalSeconds(parsed.get(ChronoField.OFFSET_SECONDS)) : null;
			return ZonedDateTime.ofLocal(local, resolvedZone, offset);
		} catch (DateTimeException e) {
			throw new ParseFailure(e.getMessage());
		}
	}

	/**
	 * Fill in a date that is missing some fields, like a month and day without a year.
	 *
	 * @param hasTime <code>true</code> if a time was parsed, so a missing date is today
	 * @throws ParseFailure if there is neither a date nor a time
	 */
	private static LocalDate missingDate(TemporalAccessor parsed, boolean hasTime, Clock clock, ZoneId zone) throws ParseFailure {
		Integer year = field(parsed, ChronoField.YEAR);
		Integer month = field(parsed, ChronoField.MONTH_OF_YEAR);
		Integer day = field(parsed, ChronoField.DAY_OF_MONTH);
		if (year == null && month == null && day == null && !hasTime) {
			throw new ParseFailure("No date or time found");
		}
		if (month != null && day == null) {
			day = 1;
		}
		if (year != null && month == null && day == null) {
			month = 1;
			day = 1;
		}
		final LocalDate today = clock.instant().atZone(zone).toLocalDate();
		return LocalDate.of(year != null ? year : today.getYear(), month != null ? month : today.getMonthValue(), day != null ? day : today.getDayOfMonth());
	}

	private static Integer field(TemporalAccessor parsed, ChronoField field) {
		return parsed.isSupported(field) ? parsed.get(field) : null;
	}
}
