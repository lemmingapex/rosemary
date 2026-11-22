package com.lemmingapex.rosemary;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the date and time fields in a datetime string without a pattern, by looking at each token and what has been found so far.
 * Thread-safe.
 */
final class PatternFreeParser {

	private static final List<String> WEEKDAY_NAMES = List.of("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");
	private static final List<String> MONTH_NAMES = List.of("JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER");
	private static final List<String> ORDINAL_NUMBERS = List.of("FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH", "SIXTH", "SEVENTH", "EIGHTH", "NINTH", "TENTH", "ELEVENTH", "TWELFTH", "THIRTEENTH", "FOURTEENTH", "FIFTEENTH", "SIXTEENTH", "SEVENTEENTH", "EIGHTEENTH", "NINETEENTH", "TWENTIETH", "TWENTYFIRST", "TWENTYSECOND", "TWENTYTHIRD", "TWENTYFOURTH", "TWENTYFIFTH", "TWENTYSIXTH", "TWENTYSEVENTH", "TWENTYEIGHTH", "TWENTYNINTH", "THIRTIETH", "THIRTYFIRST");
	private static final List<String> ORDINAL_SUFFIXES = List.of("ST", "ND", "RD", "TH");

	/**
	 * A number, with an optional sign.
	 */
	private static final Pattern NUMBER = Pattern.compile("[+-]?\\d+");

	/**
	 * A time: hours, then optional minutes, seconds and a fraction of a second, then optional AM or PM, then an optional offset.
	 * E.g. 7:02, 07:02:10.019, 8AM, 7:02PM, 22:21:19.913Z or 07:02:10-05:00.
	 */
	private static final Pattern TIME = Pattern.compile("(\\d{1,2})(?::(\\d{1,2})(?::(\\d{1,2})(?:\\.(\\d{1,9}))?)?)?(AM|PM)?(Z|[+-].+)?");

	/**
	 * A time without colons, like ISO 8601's basic format, with a fraction of a second or a time zone: hours and minutes, then optional seconds
	 * and a fraction of a second, then an optional offset.  E.g. 0702Z, 070210Z, 070210.019 or 070210-0500.
	 * Without a fraction or a time zone, it is a number, like 0702.
	 */
	private static final Pattern COMPACT_TIME = Pattern.compile("(\\d{2})(\\d{2})(?:(\\d{2})(?:\\.(\\d{1,9}))?)?(Z|[+-].+)?");

	/**
	 * A number followed by a period, like the day in "5. March 2008".
	 */
	private static final Pattern NUMBER_WITH_PERIOD = Pattern.compile("\\d+\\.");

	/**
	 * A date with colons, year first, like 2024:03:05 in Exif.
	 */
	private static final Pattern COLON_DATE = Pattern.compile("(\\d{4}):(\\d{1,2}):(\\d{1,2})");

	/**
	 * A year joined to a time with seconds by a colon, like 2024:19:02:13 in Apache logs.
	 */
	private static final Pattern YEAR_AND_TIME = Pattern.compile("(\\d{4}):(\\d{1,2}:\\d{1,2}:.+)");

	private final TimeZoneNames timeZoneNames;

	PatternFreeParser(TimeZoneNames timeZoneNames) {
		this.timeZoneNames = timeZoneNames;
	}

	/**
	 * Find the date and time fields in a datetime string.
	 *
	 * @param text the datetime string
	 * @param dateOrder the order of the day, month and year in ambiguous dates
	 * @return the fields that were found
	 * @throws ParseFailure if a token can't be used, with the token's position
	 */
	DateTimeFields parse(String text, RosemaryDateOrder dateOrder) throws ParseFailure {
		final List<Tokenizer.Token> tokens = Tokenizer.tokenize(text);
		final DateTimeFields fields = new DateTimeFields(dateOrder);
		// a number is alone if the other tokens are only words, like "at", weekdays and time zone names
		final boolean loneNumber = tokens.stream().filter(token -> !isWord(withoutNumberPeriod(token.text()))).count() == 1;
		int meridianStart = -1;
		for (int i = 0; i < tokens.size(); i++) {
			final String token = withoutNumberPeriod(tokens.get(i).text());
			final String next = i + 1 < tokens.size() ? tokens.get(i + 1).text() : "";
			try {
				if (NUMBER.matcher(token).matches()) {
					if (token.length() <= 2 && meridian(next) != null && !isDayBeforeMeridian(text, tokens, i, fields)) {
						// an hour followed by AM or PM, like 7 pm
						parseTime(token + meridian(next), fields);
						i++;
					} else {
						parseNumericToken(token, loneNumber, fields);
					}
				} else {
					if (meridian(token) != null) {
						meridianStart = tokens.get(i).start();
					}
					parseNonNumericToken(token, fields);
				}
			} catch (ParseFailure e) {
				throw e.at(tokens.get(i).start());
			}
		}
		if (fields.isMeridianPending()) {
			throw new ParseFailure("AM or PM without an hour").at(meridianStart);
		}
		return fields;
	}

	/**
	 * Remove the period from a number followed by a period, like <code>"5."</code> in "5. March 2008".
	 */
	private static String withoutNumberPeriod(String token) {
		return NUMBER_WITH_PERIOD.matcher(token).matches() ? token.substring(0, token.length() - 1) : token;
	}

	/**
	 * Is a number that is followed by AM or PM the day of the date, rather than the hour?  It is the day if the month is known, the day
	 * isn't, and the number is joined to the date, like 05 in "2008-03-05 PM", or follows a month name, like 5 in "Mar 5 PM".
	 * Otherwise it is the hour, like 7 in "Mar 5 7 PM" or "7 PM Mar 5".
	 */
	private static boolean isDayBeforeMeridian(String text, List<Tokenizer.Token> tokens, int index, DateTimeFields fields) {
		if (!fields.isMonthSet() || fields.isDaySet() || index == 0) {
			return false;
		}
		final int start = tokens.get(index).start();
		final boolean joinedToDate = start >= 2 && "-/.".indexOf(text.charAt(start - 1)) >= 0 && Character.isLetterOrDigit(text.charAt(start - 2));
		return joinedToDate || monthNumber(tokens.get(index - 1).text()) != null;
	}

	/**
	 * Is the token only a word, which doesn't add to the date or time: a weekday, a time zone name, or a word that is ignored, like "at"?
	 */
	private static boolean isWord(String token) {
		return token.chars().noneMatch(Character::isDigit) && meridian(token) == null && monthNumber(token) == null && ordinalNumber(token) == null
			&& !token.equals("NOON") && !token.equals("MIDNIGHT");
	}

	/**
	 * Is the token read as part of a date or time, before time zone names are looked up?  A time zone abbreviation that is such a token,
	 * like MAR, SAT or AM, can't be found in a datetime string.
	 *
	 * @param token upper case token
	 * @return <code>true</code> if the token is a number, a weekday or month name, an ordinal, AM or PM, noon or midnight, a time or an offset
	 */
	static boolean isDateTimeWord(String token) {
		return NUMBER.matcher(withoutNumberPeriod(token)).matches() || isWeekdayName(token) || isOffset(token) || isTime(withCompactMeridian(token))
			|| token.indexOf(':') >= 0 || isCompactTime(token) || meridian(token) != null || monthNumber(token) != null || ordinalNumber(token) != null
			|| token.equals("NOON") || token.equals("MIDNIGHT");
	}

	/**
	 * @return <code>"AM"</code> or <code>"PM"</code> if the token is AM, PM, A.M. or P.M., or <code>null</code> otherwise
	 */
	private static String meridian(String token) {
		final String withoutPeriods = token.replace(".", "");
		return withoutPeriods.equals("AM") || withoutPeriods.equals("PM") ? withoutPeriods : null;
	}

	/**
	 * Translate an ordinal number, like <code>"1ST"</code>, <code>"23RD"</code> or <code>"TWENTY-THIRD"</code>, to its numeric value.
	 *
	 * @param token upper case token
	 * @return the numeric value, or <code>null</code> if the token is not an ordinal number
	 */
	static Integer ordinalNumber(String token) {
		final String name = withoutPeriod(token);
		if (name.length() <= 2) {
			return null;
		}
		final int index = ORDINAL_NUMBERS.indexOf(name.replace("-", ""));
		if (index >= 0) {
			return index + 1;
		}
		if (ORDINAL_SUFFIXES.contains(name.substring(name.length() - 2))) {
			try {
				return Integer.parseInt(name.substring(0, name.length() - 2));
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Remove a trailing period from an abbreviated name, like <code>"SEPT."</code>.
	 */
	private static String withoutPeriod(String token) {
		return token.endsWith(".") ? token.substring(0, token.length() - 1) : token;
	}

	/**
	 * Is the token a weekday name, or an abbreviation of one with at least three letters, like <code>"WED"</code> or <code>"THURS."</code>?
	 *
	 * @param token upper case token
	 * @return <code>true</code> if the token is a weekday name
	 */
	static boolean isWeekdayName(String token) {
		final String name = withoutPeriod(token);
		if (name.length() < 3) {
			return false;
		}
		for (String weekdayName : WEEKDAY_NAMES) {
			if (name.startsWith(weekdayName) || weekdayName.startsWith(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Translate a month name, or an abbreviation of one with at least three letters, like <code>"JAN"</code> or <code>"SEPT."</code>, to its number.
	 *
	 * @param token upper case token
	 * @return the month number 1-12, or <code>null</code> if the token is not a month name
	 */
	static Integer monthNumber(String token) {
		final String name = withoutPeriod(token);
		if (name.length() < 3) {
			return null;
		}
		for (int i = 0; i < MONTH_NAMES.size(); i++) {
			if (MONTH_NAMES.get(i).startsWith(name)) {
				return i + 1;
			}
		}
		return null;
	}

	/**
	 * Parse a time, like <code>7:02</code>, <code>07:02:10.019</code>, <code>8AM</code>, <code>22:21:19.913Z</code> or <code>07:02:10-05:00</code>.
	 */
	private static void parseTime(String token, DateTimeFields fields) throws ParseFailure {
		final Matcher time = TIME.matcher(token);
		if (!time.matches()) {
			throw new ParseFailure("Bad time " + token);
		}
		// a second time would replace the first, so neither can be trusted
		if (fields.isHourSet()) {
			throw new ParseFailure("Two times");
		}
		if (time.group(5) != null) {
			fields.setPendingMeridian(time.group(5).equals("PM"));
		}
		fields.setHour(Integer.parseInt(time.group(1)));
		if (time.group(2) != null) {
			fields.setMinute(Integer.parseInt(time.group(2)));
		}
		if (time.group(3) != null) {
			fields.setSecond(Integer.parseInt(time.group(3)));
		}
		if (time.group(4) != null) {
			// e.g. .019 is 19,000,000 nanoseconds
			fields.setNano(Integer.parseInt((time.group(4) + "00000000").substring(0, 9)));
		}
		if (time.group(6) != null) {
			if (time.group(6).equals("Z")) {
				fields.setZoneOffset(ZoneOffset.UTC);
			} else {
				parseTimeZoneOffset(time.group(6), fields);
			}
		}
	}

	/**
	 * Parse a time zone offset, like <code>+05:30</code>, <code>-0700</code>, <code>-7</code>, <code>GMT-7</code> or <code>UTC+0530</code>.
	 */
	private static void parseTimeZoneOffset(String token, DateTimeFields fields) throws ParseFailure {
		String offset = token;
		if (offset.startsWith("GMT") || offset.startsWith("UTC")) {
			offset = offset.substring(3);
		}

		final boolean negative = offset.startsWith("-");
		if (!negative && !offset.startsWith("+")) {
			throw new ParseFailure("Bad time zone offset " + token);
		}
		offset = offset.substring(1);

		final int hours;
		final int minutes;
		try {
			if (offset.contains(":")) {
				final String[] parts = offset.split(":", -1);
				if (parts.length != 2) {
					throw new ParseFailure("Bad time zone offset " + token);
				}
				hours = Integer.parseInt(parts[0]);
				minutes = Integer.parseInt(parts[1]);
			} else if (offset.length() <= 2) {
				hours = Integer.parseInt(offset);
				minutes = 0;
			} else if (offset.length() <= 4) {
				final int value = Integer.parseInt(offset);
				hours = value / 100;
				minutes = value % 100;
			} else {
				throw new ParseFailure("Bad time zone offset " + token);
			}
		} catch (NumberFormatException e) {
			throw new ParseFailure("Bad time zone offset " + token);
		}
		fields.setZoneOffset(offset(negative, hours, minutes));
	}

	/**
	 * Create a fixed offset.
	 *
	 * @throws ParseFailure if the offset is out of range: more than 18 hours, or 60 minutes or more
	 */
	static ZoneOffset offset(boolean negative, int hours, int minutes) throws ParseFailure {
		final int sign = negative ? -1 : 1;
		try {
			return ZoneOffset.ofHoursMinutes(sign * hours, sign * minutes);
		} catch (DateTimeException e) {
			throw new ParseFailure(String.format("Bad time zone offset %s%02d:%02d", negative ? "-" : "+", hours, minutes));
		}
	}

	/**
	 * Is the token a time zone offset, like -05:00 or GMT+5?
	 */
	private static boolean isOffset(String token) {
		return token.startsWith("+") || token.startsWith("-") || ((token.startsWith("GMT") || token.startsWith("UTC")) && (token.contains("+") || token.contains("-")));
	}

	/**
	 * Is the token a time with colons or AM or PM, like 7:02 or 8AM?
	 */
	private static boolean isTime(String token) {
		return Character.isDigit(token.charAt(0)) && (token.indexOf(':') > 0 || (token.length() > 2 && (token.endsWith("AM") || token.endsWith("PM"))));
	}

	/**
	 * Is the token a time without colons, with a fraction of a second or a time zone, like 070210Z?
	 */
	private static boolean isCompactTime(String token) {
		return !NUMBER.matcher(token).matches() && COMPACT_TIME.matcher(token).matches();
	}

	/**
	 * Remove the periods from A.M. or P.M. at the end of a time, like <code>"7A.M."</code> or <code>"7:02P.M"</code>.
	 */
	private static String withCompactMeridian(String token) {
		if (Character.isDigit(token.charAt(0))) {
			for (String suffix : List.of("A.M.", "P.M.", "A.M", "P.M")) {
				if (token.endsWith(suffix)) {
					return token.substring(0, token.length() - suffix.length()) + suffix.charAt(0) + "M";
				}
			}
		}
		return token;
	}

	/**
	 * Use a token that isn't a number: a weekday or month name, an ordinal, a time, AM or PM, noon or midnight, or a time zone.
	 * Other words are ignored, but other tokens with digits are rejected, so that part of a date or time isn't lost.
	 */
	private void parseNonNumericToken(String token, DateTimeFields fields) throws ParseFailure {
		// weekday names don't change the date
		if (isWeekdayName(token)) {
			return;
		}

		// time zone offset, like -05:00 or GMT+5
		if (isOffset(token)) {
			parseTimeZoneOffset(token, fields);
			return;
		}

		// time without colons, like 070210Z or 070210-05:00
		final Matcher compactTime = COMPACT_TIME.matcher(token);
		if (compactTime.matches()) {
			parseCompactTime(compactTime, fields);
			return;
		}

		// a year joined by colons to the month and day, like 2024:03:05, or to a time, like 2024:19:02:13.  An hour never has four digits.
		final Matcher colonDate = COLON_DATE.matcher(token);
		if (colonDate.matches()) {
			if (fields.isYearSet() || fields.isMonthSet() || fields.isDaySet()) {
				throw new ParseFailure("Two dates");
			}
			for (int group = 1; group <= 3; group++) {
				parseNumericToken(colonDate.group(group), false, fields);
			}
			return;
		}
		final Matcher yearAndTime = YEAR_AND_TIME.matcher(token);
		if (yearAndTime.matches()) {
			if (fields.isYearSet()) {
				throw new ParseFailure("Two years");
			}
			fields.setYear(Integer.parseInt(yearAndTime.group(1)), false);
			parseTime(yearAndTime.group(2), fields);
			return;
		}

		// time, like 7:02, 8AM or 8A.M.
		final String time = withCompactMeridian(token);
		if (isTime(time)) {
			parseTime(time, fields);
			return;
		}
		// colons only appear in times and offsets
		if (token.indexOf(':') >= 0) {
			throw new ParseFailure("Unrecognized time " + token);
		}

		final String meridian = meridian(token);
		if (meridian != null) {
			final boolean postMeridian = meridian.equals("PM");
			if (fields.isHourSet()) {
				fields.applyMeridianToHour(postMeridian);
			} else {
				fields.setPendingMeridian(postMeridian);
			}
			return;
		}

		final Integer month = monthNumber(token);
		if (month != null) {
			parseMonthName(month, fields);
			return;
		}

		final Integer ordinal = ordinalNumber(token);
		if (ordinal != null) {
			parseOrdinal(ordinal, fields);
			return;
		}

		if (token.equals("NOON") || token.equals("MIDNIGHT")) {
			if (fields.isHourSet()) {
				throw new ParseFailure("Two times");
			}
			fields.setHour(token.equals("NOON") ? 12 : 0);
			return;
		}

		final ZoneId zone = timeZoneNames.find(token);
		if (zone != null) {
			fields.setZoneName(zone);
			return;
		}

		if (token.chars().anyMatch(Character::isDigit)) {
			throw new ParseFailure("Unrecognized " + token);
		}
		// otherwise ignore the word, e.g. "of", "at" or "the"
	}

	/**
	 * Use a time without colons, like 070210Z, by writing it with colons, like 07:02:10Z.
	 */
	private static void parseCompactTime(Matcher compactTime, DateTimeFields fields) throws ParseFailure {
		final StringBuilder time = new StringBuilder(compactTime.group(1)).append(':').append(compactTime.group(2));
		if (compactTime.group(3) != null) {
			time.append(':').append(compactTime.group(3));
		}
		if (compactTime.group(4) != null) {
			time.append('.').append(compactTime.group(4));
		}
		if (compactTime.group(5) != null) {
			time.append(compactTime.group(5));
		}
		parseTime(time.toString(), fields);
	}

	/**
	 * Use a month name.  If a numeric value was already taken as the month, move it to the year or day.
	 */
	private static void parseMonthName(int month, DateTimeFields fields) throws ParseFailure {
		if (!fields.isMonthSet()) {
			fields.setMonth(month);
			return;
		}

		final RosemaryDateOrder order = fields.dateOrder();
		if (!fields.isYearSet()) {
			if (fields.isDaySet() || order.isYearBeforeDay()) {
				fields.setYearFrom(fields.month());
			} else {
				fields.setDay(fields.month());
			}
			fields.setMonth(month);
			return;
		}

		if (!fields.isDaySet()) {
			fields.setDay(fields.month());
			fields.setMonth(month);
			return;
		}

		throw new ParseFailure("Too many values");
	}

	/**
	 * Use an ordinal number, like 1st or Fifth, which is usually the day.
	 */
	private static void parseOrdinal(int ordinal, DateTimeFields fields) throws ParseFailure {
		if (!fields.isDaySet()) {
			fields.setDay(ordinal);
			return;
		}

		final RosemaryDateOrder order = fields.dateOrder();
		if (!fields.isYearSet() || !fields.isMonthSet()) {
			// if the day can't be a month, move it to the year
			if (fields.day() > 12) {
				if (!fields.isYearSet()) {
					fields.setYearFrom(fields.day());
					fields.setDay(ordinal);
					return;
				}
				// the year was already set, maybe it can be the month
				if (fields.year() <= 12) {
					fields.setMonth(fields.year());
					fields.setYearFrom(fields.day());
					fields.setDay(ordinal);
					return;
				}
			} else if (!fields.isYearSet()) {
				if (!fields.isMonthSet() && !order.isYearBeforeMonth()) {
					fields.setMonth(fields.day());
				} else {
					fields.setYearFrom(fields.day());
				}
				fields.setDay(ordinal);
				return;
			} else {
				// the year is set, so the month isn't
				fields.setMonth(fields.day());
				fields.setDay(ordinal);
				return;
			}
		}

		throw new ParseFailure("Can't use ordinal " + ordinal);
	}

	/**
	 * Use a number.
	 * <ul>
	 * <li>a number with a sign is a time zone offset, like -5 or +0530</li>
	 * <li>a number by itself, or with only words like a time zone name, is a year, a compact date, a compact datetime or a time since the epoch,
	 * depending on its length</li>
	 * <li>after a complete date, a number is a time like 0702 or 19, or, after a time, a time zone offset</li>
	 * <li>otherwise, a number is a compact date like 03052008 if it has 6 or 8 digits and is first, or else a year, month or day</li>
	 * </ul>
	 *
	 * @param loneNumber <code>true</code> if the number is the only token that isn't a word
	 */
	private void parseNumericToken(String token, boolean loneNumber, DateTimeFields fields) throws ParseFailure {
		if (token.startsWith("+") || token.startsWith("-")) {
			parseTimeZoneOffset(token, fields);
			return;
		}

		if (loneNumber) {
			parseLoneNumber(token, fields);
			return;
		}

		if (token.length() == 14) {
			parseCompactDateTime(token, fields);
			return;
		}

		if (fields.isDateSet()) {
			parseNumberAfterDate(token, fields);
			return;
		}

		// the length decides, so a compact date with a small value, like 000305, isn't a year
		if ((token.length() == 6 || token.length() == 8) && !fields.isYearSet() && !fields.isMonthSet() && !fields.isDaySet()) {
			parseCompactDate(token, fields);
			return;
		}

		// leading zeros are allowed, like 0005 or 02008
		final long value = parseLong(token);
		if (value > 9999) {
			throw new ParseFailure("Unknown number " + token);
		}
		// a first value written with four or more digits is a year, even if it's small, like 0008-03-05
		if (token.length() >= 4 && !fields.isYearSet() && !fields.isMonthSet() && !fields.isDaySet()) {
			fields.setYear((int) value, false);
			return;
		}
		parseDateNumber((int) value, token.length() <= 2, fields);
	}

	/**
	 * Use a number that is the whole datetime string.
	 * <ul>
	 * <li>4 digits: a year, like 2024</li>
	 * <li>6 or 8 digits: a date, like 240305 or 20240305, in the date order, or YYYYMMDD</li>
	 * <li>9 to 11 digits: seconds since 1970-01-01T00:00:00Z, like 1709596800</li>
	 * <li>12 or 13 digits: milliseconds since 1970-01-01T00:00:00Z, like 1709596800000</li>
	 * <li>14 digits: a datetime, YYYYMMDDhhmmss</li>
	 * </ul>
	 */
	private static void parseLoneNumber(String token, DateTimeFields fields) throws ParseFailure {
		switch (token.length()) {
			case 4 -> fields.setYear(Integer.parseInt(token), false);
			case 6, 8 -> parseCompactDate(token, fields);
			case 9, 10, 11 -> parseInstant(Instant.ofEpochSecond(parseLong(token)), fields);
			case 12, 13 -> parseInstant(Instant.ofEpochMilli(parseLong(token)), fields);
			case 14 -> parseCompactDateTime(token, fields);
			default -> throw new ParseFailure("Ambiguous number " + token);
		}
	}

	/**
	 * Use a number after a complete date: a time like 19, 0702 or 070210 if there is no time yet, or else a time zone offset like 0530.
	 */
	private static void parseNumberAfterDate(String token, DateTimeFields fields) throws ParseFailure {
		final int value = (int) Math.min(parseLong(token), Integer.MAX_VALUE);
		if (!fields.isHourSet()) {
			switch (token.length()) {
				case 1, 2 -> fields.setHour(value);
				case 4 -> {
					fields.setHour(value / 100);
					fields.setMinute(value % 100);
				}
				case 6 -> {
					fields.setHour(value / 10000);
					fields.setMinute(value / 100 % 100);
					fields.setSecond(value % 100);
				}
				default -> throw new ParseFailure("Unknown number " + token);
			}
			return;
		}

		// e.g. 5, 0500 or 0530
		if (value <= 18) {
			fields.setZoneOffset(offset(false, value, 0));
			return;
		}
		if (value >= 100 && value <= 1800 && (value % 100) < 60 && (value % 100) % 15 == 0) {
			fields.setZoneOffset(offset(false, value / 100, value % 100));
			return;
		}
		throw new ParseFailure("Unknown number " + token);
	}

	/**
	 * Use a number that is part of a date: a year, month or day.
	 *
	 * @param number the number, at most 9999
	 * @param twoDigits <code>true</code> if the number was written with one or two digits
	 */
	private static void parseDateNumber(int number, boolean twoDigits, DateTimeFields fields) throws ParseFailure {
		final RosemaryDateOrder order = fields.dateOrder();

		// more than 31 must be a year
		if (number > 31) {
			if (!fields.isYearSet()) {
				fields.setYear(number, twoDigits);
				return;
			}
			// the year that was set before must have been a day or month
			if (fields.year() > 31) {
				throw new ParseFailure("Two years " + fields.year() + " and " + number);
			}
			if (fields.year() > 12) {
				if (!fields.isDaySet()) {
					fields.setDay(fields.year());
					fields.setYear(number, twoDigits);
					return;
				}
				// both day and year are set, try using the day as the month
				if (fields.day() <= 12) {
					fields.setMonth(fields.day());
					fields.setDay(fields.year());
					fields.setYear(number, twoDigits);
					return;
				}
				throw new ParseFailure("Too many values " + number);
			}

			// the year was set to a value that is at most 12
			if (!fields.isDaySet() && !fields.isMonthSet()) {
				if (order.isMonthBeforeDay()) {
					fields.setMonth(fields.year());
				} else {
					fields.setDay(fields.year());
				}
			} else if (!fields.isDaySet()) {
				fields.setDay(fields.year());
			} else {
				fields.setMonth(fields.year());
			}
			fields.setYear(number, twoDigits);
			return;
		}

		// more than 12 can't be a month
		if (number > 12) {
			if (!fields.isYearSet()) {
				if (fields.isDaySet() || order.isYearBeforeDay()) {
					fields.setYear(number, twoDigits);
				} else {
					fields.setDay(number);
				}
				return;
			}
			if (!fields.isDaySet()) {
				fields.setDay(number);
				return;
			}
			// the day that was set must be the month, e.g. 2024-12-31 in day-first order
			if (!fields.isMonthSet() && fields.day() <= 12) {
				fields.setMonth(fields.day());
				fields.setDay(number);
				return;
			}
			throw new ParseFailure("Too many values " + number);
		}

		// at most 12: could be a day, month or year

		if (fields.isYearSet()) {
			if (fields.isMonthSet() || (!fields.isDaySet() && !isMonthBeforeDay(fields))) {
				fields.setDay(number);
			} else {
				fields.setMonth(number);
			}
			return;
		}

		if (fields.isMonthSet()) {
			if (fields.isDaySet() || order.isYearBeforeDay()) {
				fields.setYear(number, twoDigits);
			} else {
				fields.setDay(number);
			}
			return;
		}

		if (fields.isDaySet()) {
			if (order.isYearBeforeMonth()) {
				fields.setYear(number, twoDigits);
			} else {
				fields.setMonth(number);
			}
			return;
		}

		// nothing is set yet
		if (order.isYearBeforeMonth()) {
			if (order.isYearBeforeDay()) {
				fields.setYear(number, twoDigits);
			} else {
				fields.setDay(number);
			}
		} else if (order.isMonthBeforeDay()) {
			fields.setMonth(number);
		} else {
			fields.setDay(number);
		}
	}

	/**
	 * Does the month come before the day?  A full year written first means year, month, day, like ISO 8601's 2024-12-05,
	 * unless the date order puts the year first.  Otherwise, the date order decides.
	 */
	private static boolean isMonthBeforeDay(DateTimeFields fields) {
		final RosemaryDateOrder order = fields.dateOrder();
		if (fields.isYearWrittenFirst() && !order.isYearFirst()) {
			return true;
		}
		return order.isMonthBeforeDay();
	}

	/**
	 * Use a compact date, like 240305 or 20240305, in the date order, with a two-digit year for 6 digits and a four-digit year for 8 digits.
	 * If that isn't a valid date, 8 digits can also be YYYYMMDD, like ISO 8601's basic format.
	 */
	private static void parseCompactDate(String token, DateTimeFields fields) throws ParseFailure {
		final String order = fields.dateOrder().fieldOrder();
		if (setCompactDate(token, order, fields)) {
			return;
		}
		if (token.length() == 8 && !order.equals("YMD") && setCompactDate(token, "YMD", fields)) {
			return;
		}
		throw new ParseFailure("Bad date " + token);
	}

	/**
	 * Set the date from a compact date in the given field order, like <code>"MDY"</code>, if it is a valid date.
	 *
	 * @return <code>true</code> if the date was valid, and was set
	 */
	private static boolean setCompactDate(String token, String fieldOrder, DateTimeFields fields) throws ParseFailure {
		final int yearLength = token.length() - 4;
		int year = 0;
		int month = 0;
		int day = 0;
		int position = 0;
		for (char field : fieldOrder.toCharArray()) {
			final int length = field == 'Y' ? yearLength : 2;
			final int value = Integer.parseInt(token.substring(position, position + length));
			position += length;
			switch (field) {
				case 'Y' -> year = value;
				case 'M' -> month = value;
				default -> day = value;
			}
		}
		// a two-digit year is checked as if it were in the 2000s, so 29 February is allowed
		if (month < 1 || month > 12 || day < 1 || day > YearMonth.of(yearLength == 2 ? 2000 + year : year, month).lengthOfMonth()) {
			return false;
		}
		fields.setYear(year, yearLength == 2);
		fields.setMonth(month);
		fields.setDay(day);
		return true;
	}

	/**
	 * Use a compact datetime, YYYYMMDDhhmmss.
	 */
	private static void parseCompactDateTime(String token, DateTimeFields fields) throws ParseFailure {
		if (fields.isYearSet() || fields.isMonthSet() || fields.isDaySet() || fields.isHourSet()) {
			throw new ParseFailure("Unknown number " + token);
		}
		if (!setCompactDate(token.substring(0, 8), "YMD", fields)) {
			throw new ParseFailure("Bad date " + token);
		}
		fields.setHour(Integer.parseInt(token.substring(8, 10)));
		fields.setMinute(Integer.parseInt(token.substring(10, 12)));
		fields.setSecond(Integer.parseInt(token.substring(12, 14)));
	}

	/**
	 * Use an instant, which is in UTC.
	 */
	private static void parseInstant(Instant instant, DateTimeFields fields) throws ParseFailure {
		final ZonedDateTime dateTime = instant.atZone(ZoneOffset.UTC);
		fields.setYear(dateTime.getYear(), false);
		fields.setMonth(dateTime.getMonthValue());
		fields.setDay(dateTime.getDayOfMonth());
		fields.setHour(dateTime.getHour());
		fields.setMinute(dateTime.getMinute());
		fields.setSecond(dateTime.getSecond());
		fields.setNano(dateTime.getNano());
		fields.setZoneOffset(ZoneOffset.UTC);
	}

	private static long parseLong(String token) throws ParseFailure {
		try {
			return Long.parseLong(token);
		} catch (NumberFormatException e) {
			throw new ParseFailure("Number out of range " + token);
		}
	}
}
