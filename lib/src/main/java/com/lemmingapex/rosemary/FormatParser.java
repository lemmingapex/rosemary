package com.lemmingapex.rosemary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;

/**
 * Parses datetimes with {@link DateTimeFormatter} patterns.
 */
final class FormatParser {

	private FormatParser() {
	}

	/**
	 * Parse a datetime with the first of the patterns that matches.
	 * <p>
	 * The pattern doesn't need to include every field: a missing time zone is the default zone, a missing time is midnight,
	 * and a missing date is today.  Dates are checked strictly, so February 31st doesn't match.
	 *
	 * @param text the datetime string
	 * @param patterns {@link DateTimeFormatter} patterns to try, in order
	 * @param locale locale for names, like month names
	 * @param defaultZone time zone to use if the pattern doesn't have one
	 * @param clock the clock that provides today's date
	 * @return the datetime
	 * @throws ParseFailure if none of the patterns match
	 * @throws IllegalArgumentException if a pattern is invalid
	 */
	static ZonedDateTime parse(String text, List<String> patterns, Locale locale, ZoneId defaultZone, Clock clock) throws ParseFailure {
		for (String pattern : patterns) {
			final DateTimeFormatter formatter = formatter(pattern, locale);
			try {
				return toZonedDateTime(formatter.parseBest(text, ZonedDateTime::from, LocalDateTime::from, LocalDate::from, LocalTime::from), defaultZone, clock);
			} catch (DateTimeParseException e) {
				// try the next pattern
			}
		}
		throw new ParseFailure("Doesn't match " + (patterns.size() == 1 ? "the pattern " : "any of the patterns ") + String.join(", ", patterns));
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

	private static ZonedDateTime toZonedDateTime(TemporalAccessor parsed, ZoneId defaultZone, Clock clock) {
		if (parsed instanceof ZonedDateTime zoned) {
			return zoned;
		}
		if (parsed instanceof LocalDateTime localDateTime) {
			return localDateTime.atZone(defaultZone);
		}
		if (parsed instanceof LocalDate date) {
			return date.atStartOfDay(defaultZone);
		}
		final LocalDate today = clock.instant().atZone(defaultZone).toLocalDate();
		return ((LocalTime) parsed).atDate(today).atZone(defaultZone);
	}
}
