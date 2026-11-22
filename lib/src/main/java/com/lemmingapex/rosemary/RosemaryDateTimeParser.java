package com.lemmingapex.rosemary;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Rosemary is a robust and pattern-free datetime parser.  It finds the date, time and time zone in strings like
 * <code>"March 5, 2008 7:02 pm"</code>, <code>"3/5/08"</code>, <code>"2008-03-05 07:02:00 America/Denver"</code> or
 * <code>"Wed, 05 Mar 2008 19:02:00 -0700"</code>, without being told the format.
 * <pre>{@code
 * RosemaryDateTimeParser rosemary = new RosemaryDateTimeParser();
 * OffsetDateTime dateTime = rosemary.parse("March 5, 2008 7:02 pm"); // 2008-03-05T19:02Z
 * ZonedDateTime zoned = rosemary.parseZoned("2019-03-05 07:02 America/Denver"); // 2019-03-05T07:02-07:00[America/Denver]
 * }</pre>
 * <p>
 * {@code parse} methods return an {@link OffsetDateTime}.  {@code parseZoned} methods return a {@link ZonedDateTime}, which keeps
 * the time zone's region, like America/Denver, when the string names one.
 * <p>
 * Use {@link #builder()} to change the defaults: the order of ambiguous dates like 01/02/03, the time zone, the clock that provides
 * today's date, and more.
 * <p>
 * Instances are immutable and thread-safe.
 */
public final class RosemaryDateTimeParser {

	/**
	 * The default first year for two-digit years: '70 is 1970, and '69 is 2069.
	 */
	public static final int DEFAULT_TWO_DIGIT_YEAR_START = 1970;

	private final RosemaryDateOrder dateOrder;
	private final ZoneId defaultZone;
	private final Clock clock;
	private final Locale locale;
	private final int twoDigitYearStart;
	private final Map<String, ZoneId> timeZoneAbbreviations;
	private final TimeZoneNames timeZoneNames;
	private final PatternFreeParser patternFreeParser;

	/**
	 * Create a parser with the default settings: dates like 01/02/03 are month, day, year, the time zone is UTC unless the string
	 * has one, and today's date comes from the system clock.
	 */
	public RosemaryDateTimeParser() {
		this(new Builder());
	}

	private RosemaryDateTimeParser(Builder builder) {
		this.dateOrder = builder.dateOrder;
		this.defaultZone = builder.defaultZone;
		this.clock = builder.clock;
		this.locale = builder.locale;
		this.twoDigitYearStart = builder.twoDigitYearStart;
		this.timeZoneAbbreviations = caseInsensitiveCopy(builder.timeZoneAbbreviations);
		this.timeZoneNames = new TimeZoneNames(timeZoneAbbreviations);
		this.patternFreeParser = new PatternFreeParser(timeZoneNames);
	}

	/**
	 * Create a builder with the default settings.
	 *
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Create a builder with this parser's settings, to make a parser that differs in some settings.
	 *
	 * @return a new builder
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	/**
	 * The time zone abbreviations recognized by default, like PST and CET, and the time zones they map to.
	 * <p>
	 * An abbreviation maps to a region, not a fixed offset, so CST and CDT both mean America/Chicago, and the offset depends on the date.
	 * Region ids like America/Denver, and UTC, GMT, UT, Z and Zulu, are always recognized and aren't included.
	 *
	 * @return the default abbreviations, sorted by name, with case-insensitive keys.  The map can't be changed.
	 */
	public static Map<String, ZoneId> defaultTimeZoneAbbreviations() {
		return caseInsensitiveCopy(TimeZoneNames.DEFAULT_ABBREVIATIONS);
	}

	/**
	 * @return an unmodifiable copy of the map, sorted by name, with case-insensitive keys
	 */
	private static Map<String, ZoneId> caseInsensitiveCopy(Map<String, ZoneId> abbreviations) {
		final Map<String, ZoneId> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		copy.putAll(abbreviations);
		return Collections.unmodifiableMap(copy);
	}

	/**
	 * Parse a datetime in any format.  Ambiguous dates like 01/02/03 use this parser's date order.
	 * <ul>
	 * <li>a missing time zone is this parser's default zone</li>
	 * <li>a missing time is midnight</li>
	 * <li>a missing year, month or day comes from today's date, e.g. "March 5" is March 5th of this year, and "7pm" is today</li>
	 * <li>a month without a day is the first of the month, and a year alone is January 1st</li>
	 * <li>words that aren't part of a datetime, like "of" or "at", are ignored</li>
	 * </ul>
	 *
	 * @param text the datetime to parse
	 * @return the datetime, or <code>null</code> if <code>text</code> is <code>null</code>
	 * @throws RosemaryDateTimeException if the datetime can't be parsed
	 * @see #parseZoned(String)
	 */
	public OffsetDateTime parse(String text) {
		return toOffsetDateTime(parseZoned(text));
	}

	/**
	 * Parse a datetime in any format, with a date order for ambiguous dates like 01/02/03.  Otherwise the same as {@link #parse(String)}.
	 *
	 * @param text the datetime to parse
	 * @param dateOrder the order of the day, month and year in ambiguous dates
	 * @return the datetime, or <code>null</code> if <code>text</code> is <code>null</code>
	 * @throws RosemaryDateTimeException if the datetime can't be parsed
	 * @see #parseZoned(String, RosemaryDateOrder)
	 */
	public OffsetDateTime parse(String text, RosemaryDateOrder dateOrder) {
		return toOffsetDateTime(parseZoned(text, dateOrder));
	}

	/**
	 * Parse a datetime with a {@link java.time.format.DateTimeFormatter} pattern, like <code>"MM-dd-yy HH:mm VV"</code>.
	 * <p>
	 * The pattern doesn't need to include every field: a missing time zone is this parser's default zone, a missing time is midnight,
	 * and a missing date is today.  Names, like month names, use this parser's locale.
	 *
	 * @param text the datetime to parse
	 * @param pattern the {@link java.time.format.DateTimeFormatter} pattern
	 * @return the datetime, or <code>null</code> if <code>text</code> is <code>null</code>
	 * @throws RosemaryDateTimeException if the datetime doesn't match the pattern
	 * @throws IllegalArgumentException if the pattern is invalid
	 * @see #parseZoned(String, String)
	 */
	public OffsetDateTime parse(String text, String pattern) {
		return toOffsetDateTime(parseZoned(text, pattern));
	}

	/**
	 * Parse a datetime with the first of several {@link java.time.format.DateTimeFormatter} patterns that matches.  Otherwise the same as {@link #parse(String, String)}.
	 *
	 * @param text the datetime to parse
	 * @param patterns the {@link java.time.format.DateTimeFormatter} patterns to try, in order
	 * @return the datetime, or <code>null</code> if <code>text</code> is <code>null</code>
	 * @throws RosemaryDateTimeException if the datetime doesn't match any of the patterns
	 * @throws IllegalArgumentException if there are no patterns, or a pattern is invalid
	 * @see #parseZoned(String, List)
	 */
	public OffsetDateTime parse(String text, List<String> patterns) {
		return toOffsetDateTime(parseZoned(text, patterns));
	}

	/**
	 * Parse a datetime in any format, and keep its time zone.  Otherwise the same as {@link #parse(String)}.
	 * <p>
	 * The time zone is the region if the string names one, like America/Denver, or an abbreviation for one, like MST.
	 * It is the offset if the string has one, like -07:00, and the default zone if the string has neither.
	 *
	 * @param text the datetime to parse
	 * @return the datetime, or <code>null</code> if <code>text</code> is <code>null</code>
	 * @throws RosemaryDateTimeException if the datetime can't be parsed
	 */
	public ZonedDateTime parseZoned(String text) {
		return parseZoned(text, dateOrder);
	}

	/**
	 * Parse a datetime in any format, with a date order for ambiguous dates like 01/02/03, and keep its time zone.
	 * Otherwise the same as {@link #parseZoned(String)}.
	 *
	 * @param text the datetime to parse
	 * @param dateOrder the order of the day, month and year in ambiguous dates
	 * @return the datetime, or <code>null</code> if <code>text</code> is <code>null</code>
	 * @throws RosemaryDateTimeException if the datetime can't be parsed
	 */
	public ZonedDateTime parseZoned(String text, RosemaryDateOrder dateOrder) {
		Objects.requireNonNull(dateOrder, "dateOrder");
		if (text == null) {
			return null;
		}
		try {
			return patternFreeParser.parse(text, dateOrder).resolve(clock, defaultZone, twoDigitYearStart);
		} catch (ParseFailure | DateTimeException e) {
			final ZonedDateTime iso = FormatParser.parseIsoWithZone(text);
			if (iso != null) {
				return iso;
			}
			throw failure(text, e);
		}
	}

	/**
	 * Parse a datetime with a {@link java.time.format.DateTimeFormatter} pattern, and keep its time zone.
	 * Otherwise the same as {@link #parse(String, String)}.
	 *
	 * @param text the datetime to parse
	 * @param pattern the {@link java.time.format.DateTimeFormatter} pattern
	 * @return the datetime, or <code>null</code> if <code>text</code> is <code>null</code>
	 * @throws RosemaryDateTimeException if the datetime doesn't match the pattern
	 * @throws IllegalArgumentException if the pattern is invalid
	 */
	public ZonedDateTime parseZoned(String text, String pattern) {
		Objects.requireNonNull(pattern, "pattern");
		return parseZoned(text, List.of(pattern));
	}

	/**
	 * Parse a datetime with the first of several {@link java.time.format.DateTimeFormatter} patterns that matches, and keep its time zone.
	 * Otherwise the same as {@link #parse(String, List)}.
	 *
	 * @param text the datetime to parse
	 * @param patterns the {@link java.time.format.DateTimeFormatter} patterns to try, in order
	 * @return the datetime, or <code>null</code> if <code>text</code> is <code>null</code>
	 * @throws RosemaryDateTimeException if the datetime doesn't match any of the patterns
	 * @throws IllegalArgumentException if there are no patterns, or a pattern is invalid
	 */
	public ZonedDateTime parseZoned(String text, List<String> patterns) {
		Objects.requireNonNull(patterns, "patterns");
		if (patterns.isEmpty()) {
			throw new IllegalArgumentException("No patterns");
		}
		if (text == null) {
			return null;
		}
		try {
			return FormatParser.parse(text, patterns, locale, defaultZone, clock);
		} catch (ParseFailure e) {
			throw failure(text, e);
		}
	}

	/**
	 * Find the time zone for a name, the same way this parser does when the name is in a datetime string.  Names are case-insensitive.
	 * <ul>
	 * <li>UTC, GMT, UT, Z and Zulu are a zero offset</li>
	 * <li>an abbreviation, like PST, is the region this parser maps it to, like America/Los_Angeles</li>
	 * <li>a region id, like america/denver, is that region</li>
	 * </ul>
	 * Offsets, like -05:00, aren't names.  Use {@link ZoneOffset#of(String)} for them.
	 *
	 * @param name the time zone name
	 * @return the time zone, or empty if this parser doesn't recognize the name
	 */
	public Optional<ZoneId> findTimeZone(String name) {
		Objects.requireNonNull(name, "name");
		return Optional.ofNullable(timeZoneNames.find(TimeZoneNames.upperCase(name.strip())));
	}

	private static OffsetDateTime toOffsetDateTime(ZonedDateTime dateTime) {
		return dateTime == null ? null : dateTime.toOffsetDateTime();
	}

	private static RosemaryDateTimeException failure(String text, Exception e) {
		final String message = "Unable to parse \"" + text + "\": " + e.getMessage();
		// a ParseFailure is internal, and its message says everything
		return e instanceof ParseFailure ? new RosemaryDateTimeException(message, text) : new RosemaryDateTimeException(message, text, e);
	}

	/**
	 * Get the order of the day, month and year in ambiguous dates, like 01/02/03.
	 *
	 * @return the date order
	 */
	public RosemaryDateOrder getDateOrder() {
		return dateOrder;
	}

	/**
	 * Get the time zone used when a datetime doesn't have one.
	 *
	 * @return the default time zone
	 */
	public ZoneId getDefaultZone() {
		return defaultZone;
	}

	/**
	 * Get the clock that provides today's date, for datetimes that don't have a full date.
	 *
	 * @return the clock
	 */
	public Clock getClock() {
		return clock;
	}

	/**
	 * Get the locale used for names, like month names, in {@link java.time.format.DateTimeFormatter} patterns.
	 *
	 * @return the locale
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Get the first year of the 100-year range that two-digit years are placed in.
	 *
	 * @return the first year for two-digit years
	 */
	public int getTwoDigitYearStart() {
		return twoDigitYearStart;
	}

	/**
	 * Get the time zone abbreviations this parser recognizes, like PST, and the time zones they map to.
	 *
	 * @return the abbreviations, sorted by name, with case-insensitive keys.  The map can't be changed.
	 */
	public Map<String, ZoneId> getTimeZoneAbbreviations() {
		return timeZoneAbbreviations;
	}

	@Override
	public String toString() {
		return "RosemaryDateTimeParser[dateOrder=" + dateOrder + ", defaultZone=" + defaultZone + ", clock=" + clock + ", locale=" + locale
			+ ", twoDigitYearStart=" + twoDigitYearStart + ", timeZoneAbbreviations=" + timeZoneAbbreviations.size() + "]";
	}

	/**
	 * Builds a {@link RosemaryDateTimeParser}.  Not thread-safe.
	 */
	public static final class Builder {
		private RosemaryDateOrder dateOrder = RosemaryDateOrder.MM_DD_YY;
		private ZoneId defaultZone = ZoneOffset.UTC;
		private Clock clock = Clock.systemUTC();
		private Locale locale = Locale.ENGLISH;
		private int twoDigitYearStart = DEFAULT_TWO_DIGIT_YEAR_START;
		private final Map<String, ZoneId> timeZoneAbbreviations = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		private Builder() {
			timeZoneAbbreviations.putAll(TimeZoneNames.DEFAULT_ABBREVIATIONS);
		}

		private Builder(RosemaryDateTimeParser parser) {
			dateOrder = parser.dateOrder;
			defaultZone = parser.defaultZone;
			clock = parser.clock;
			locale = parser.locale;
			twoDigitYearStart = parser.twoDigitYearStart;
			timeZoneAbbreviations.putAll(parser.timeZoneAbbreviations);
		}

		/**
		 * Set the order of the day, month and year in ambiguous dates, like 01/02/03.  The default is {@link RosemaryDateOrder#MM_DD_YY}.
		 *
		 * @param dateOrder the date order
		 * @return this builder
		 */
		public Builder dateOrder(RosemaryDateOrder dateOrder) {
			this.dateOrder = Objects.requireNonNull(dateOrder, "dateOrder");
			return this;
		}

		/**
		 * Set the time zone used when a datetime doesn't have one.  The default is UTC.
		 *
		 * @param defaultZone the default time zone
		 * @return this builder
		 */
		public Builder defaultZone(ZoneId defaultZone) {
			this.defaultZone = Objects.requireNonNull(defaultZone, "defaultZone");
			return this;
		}

		/**
		 * Set the clock that provides today's date, for datetimes that don't have a full date, like "March 5" or "7pm".
		 * Today's date is taken in the datetime's time zone.  The default is {@link Clock#systemUTC()}.
		 * <p>
		 * Use a fixed clock, like <code>Clock.fixed(Instant.parse("2030-03-01T00:00:00Z"), ZoneOffset.UTC)</code>, to parse relative to another date.
		 *
		 * @param clock the clock
		 * @return this builder
		 */
		public Builder clock(Clock clock) {
			this.clock = Objects.requireNonNull(clock, "clock");
			return this;
		}

		/**
		 * Set the locale used for names, like month names, in {@link java.time.format.DateTimeFormatter} patterns.  The default is {@link Locale#ENGLISH}.
		 * Pattern-free parsing always uses English names.
		 *
		 * @param locale the locale
		 * @return this builder
		 */
		public Builder locale(Locale locale) {
			this.locale = Objects.requireNonNull(locale, "locale");
			return this;
		}

		/**
		 * Set the first year of the 100-year range that two-digit years, like '08, are placed in.
		 * The default is {@value RosemaryDateTimeParser#DEFAULT_TWO_DIGIT_YEAR_START}, so '70 is 1970 and '69 is 2069.
		 *
		 * @param twoDigitYearStart the first year, 0-9900
		 * @return this builder
		 */
		public Builder twoDigitYearStart(int twoDigitYearStart) {
			if (twoDigitYearStart < 0 || twoDigitYearStart > 9900) {
				throw new IllegalArgumentException("twoDigitYearStart must be 0-9900: " + twoDigitYearStart);
			}
			this.twoDigitYearStart = twoDigitYearStart;
			return this;
		}

		/**
		 * Add a time zone abbreviation, or change the time zone of one.  Abbreviations are case-insensitive.
		 *
		 * @param abbreviation the abbreviation, like <code>"SGT"</code>
		 * @param zone the time zone, like <code>ZoneId.of("Asia/Singapore")</code>
		 * @return this builder
		 * @throws IllegalArgumentException if the abbreviation is blank, contains whitespace, or is a name for UTC
		 */
		public Builder timeZoneAbbreviation(String abbreviation, ZoneId zone) {
			checkAbbreviation(abbreviation);
			timeZoneAbbreviations.put(abbreviation, Objects.requireNonNull(zone, "zone"));
			return this;
		}

		/**
		 * Replace all the time zone abbreviations.  Use an empty map to recognize only region ids, like America/Denver, and UTC.
		 *
		 * @param abbreviations the abbreviations and their time zones
		 * @return this builder
		 * @throws IllegalArgumentException if an abbreviation is blank, contains whitespace, or is a name for UTC
		 * @see RosemaryDateTimeParser#defaultTimeZoneAbbreviations()
		 */
		public Builder timeZoneAbbreviations(Map<String, ZoneId> abbreviations) {
			Objects.requireNonNull(abbreviations, "abbreviations");
			abbreviations.forEach((abbreviation, zone) -> {
				checkAbbreviation(abbreviation);
				Objects.requireNonNull(zone, "zone");
			});
			timeZoneAbbreviations.clear();
			timeZoneAbbreviations.putAll(abbreviations);
			return this;
		}

		private static void checkAbbreviation(String abbreviation) {
			Objects.requireNonNull(abbreviation, "abbreviation");
			if (abbreviation.isBlank() || abbreviation.chars().anyMatch(Character::isWhitespace)) {
				throw new IllegalArgumentException("Bad time zone abbreviation: \"" + abbreviation + "\"");
			}
			if (TimeZoneNames.UTC_NAMES.contains(TimeZoneNames.upperCase(abbreviation))) {
				throw new IllegalArgumentException(abbreviation + " always means UTC, and can't be changed");
			}
		}

		/**
		 * Build the parser.
		 *
		 * @return a new parser
		 */
		public RosemaryDateTimeParser build() {
			return new RosemaryDateTimeParser(this);
		}
	}
}
