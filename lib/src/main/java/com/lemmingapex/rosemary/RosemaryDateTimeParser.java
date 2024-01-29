package com.lemmingapex.rosemary;

import com.lemmingapex.rosemary.utils.ParsingUtilities;
import com.lemmingapex.rosemary.utils.TimeZoneUtils;

import java.text.DateFormat;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Rosemary is a robust and pattern-free datetime parser for java. Dates and times can messy. Rosemary simplifies the process of parsing dates and times from diverse formats, offering flexibility, precision, and easy configuration options for developers.
 */
public class RosemaryDateTimeParser {

	private enum TimePlace {
		HOUR, MINUTE, SECOND, MILLISECOND, UNKNOWN;
	}

	private static final List<String> WEEKDAY_NAMES = List.of("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");
	private static final List<String> MONTH_NAMES = List.of("JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER");
	private static final List<String> ORDINAL_NUMBERS = List.of("FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH", "SIXTH", "SEVENTH", "EIGHTH", "NINTH", "TENTH", "ELEVENTH", "TWELFTH", "THIRTEENTH", "FOURTEENTH", "FIFTEENTH", "SIXTEENTH", "SEVENTEENTH", "EIGHTEENTH", "NINETEENTH", "TWENTIETH", "TWENTYFIRST", "TWENTYSECOND", "TWENTYTHIRD", "TWENTYFOURTH", "TWENTYFIFTH", "TWENTYSIXTH", "TWENTYSEVENTH", "TWENTYEIGHTH", "TWENTYNINTH", "THIRTIETH", "THIRTYFIRST");

	private final Map<String, TimeZone> timeZoneNameToTimeZone;

	public RosemaryDateTimeParser(RosemaryTimeZoneProvider timeZoneProvider) {
		this.timeZoneNameToTimeZone = timeZoneProvider.timezoneNameToTimezone();
	}

	public RosemaryDateTimeParser() {
		this(TimeZoneUtils::getTimezoneNameToTimezoneMap);
	}

	/**
	 * Translate a string representation of an ordinal number to the appropriate numeric value.<br>
	 * For example, <code>"1st"</code> would return <code>1</code>, <code>"23rd"</code> would return <code>23</code>,
	 * etc.
	 *
	 * @param str ordinal string
	 * @return the numeric value of the ordinal number, or null if the supplied string is not a valid ordinal number.
	 */
	private static Integer getOrdinalNumber(String str) {
		final int len = (str == null ? 0 : str.length());
		if (len > 2) {
			final String normalizedString = str.trim().replace("-", "").toUpperCase();
			int index = ORDINAL_NUMBERS.indexOf(normalizedString);
			if (index >= 0) {
				return index + 1;
			}

			final String suffix = str.substring(len - 2).toUpperCase();
			final String[] ordinalSuffixes = { "ST", "ND", "RD", "TH" };
			for (String ordinalSuffix : ordinalSuffixes) {
				if (ordinalSuffix.equals(suffix)) {
					try {
						return Integer.parseInt(str.substring(0, len - 2));
					} catch (NumberFormatException nfe) {
						// fall through if number was not parsed
					}
				}
			}
		}
		return null;
	}

	/**
	 * Determine is the supplied string is a value weekday name.
	 *
	 * @param str weekday name to check
	 * @return <code>true</code> if the supplied string is a weekday name.
	 */
	private static boolean isWeekdayName(String str) {
		if (str != null && str.length() >= 3) {
			final String upperStr = str.toUpperCase();
			for (final String weekDayName : WEEKDAY_NAMES) {
				if (upperStr.startsWith(weekDayName) || weekDayName.startsWith(upperStr)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Convert the supplied month name to its numeric representation. <br>
	 * For example, <code>"January"</code> (or any substring) would return <code>1</code> and <code>"December"</code>
	 * would return <code>12</code>.
	 *
	 * @param str month name
	 * @return the numeric month, or null if the supplied string is not a valid month name.
	 */
	private static Integer monthNameToNumber(String str) {
		if (str != null && str.length() >= 3) {
			final String upperStr = str.toUpperCase();
			for (int i = 0; i < MONTH_NAMES.size(); i++) {
				final String monthName = MONTH_NAMES.get(i);
				if (upperStr.startsWith(monthName.substring(0, 3)) || monthName.startsWith(upperStr)) {
					return i + 1;
				}
			}
		}

		return null;
	}

	/**
	 * Parse a time string.
	 *
	 * @param state parser state
	 * @param timeStr string containing colon-separated time
	 * @throws RosemaryDateTimeException if there is a problem with the time
	 */
	private void parseTime(String timeStr, RosemaryDateTimeState state) throws RosemaryDateTimeException {
		TimePlace place = TimePlace.HOUR;

		String tmpTime;

		final char lastChar = timeStr.charAt(timeStr.length() - 1);
		if (lastChar != 'm' && lastChar != 'M') {
			tmpTime = timeStr;
		} else {
			final char preLast = timeStr.charAt(timeStr.length() - 2);
			if (preLast == 'a' || preLast == 'A') {
				state.setTimePostMeridian(false);
			} else if (preLast == 'p' || preLast == 'P') {
				state.setTimePostMeridian(true);
			} else {
				throw new RosemaryDateTimeException("Bad time: " + timeStr);
			}

			tmpTime = timeStr.substring(0, timeStr.length() - 2);
		}

		final String[] tList = tmpTime.split("[:\\.]");
		for (final String token : tList) {
			final int val;
			try {
				if (place.equals(TimePlace.MILLISECOND)) {
					val = Integer.parseInt((token + "00").substring(0, 3));
				} else {
					val = Integer.parseInt(token);
				}
			} catch (NumberFormatException nfe) {
				throw new RosemaryDateTimeException("Bad " + place.name() + " value: " + token);
			}

			switch (place) {
				case HOUR:
					state.setHour(val);
					place = TimePlace.MINUTE;
					break;
				case MINUTE:
					state.setMinute(val);
					place = TimePlace.SECOND;
					break;
				case SECOND:
					state.setSecond(val);
					place = TimePlace.MILLISECOND;
					break;
				case MILLISECOND:
					state.setMillisecond(val);
					place = TimePlace.UNKNOWN;
					break;
				default:
					throw new RosemaryDateTimeException("Unexpected place value: " + place.name());
			}
		}
	}

	/**
	 * Parse a time zone offset string.
	 *
	 * @param zoneStr string containing colon-separated time zone offset
	 * @param state parser state
	 * @throws RosemaryDateTimeException if there is a problem with the time
	 */
	private void parseTimeZoneOffset(String zoneStr, RosemaryDateTimeState state) throws RosemaryDateTimeException {
		TimePlace place = TimePlace.HOUR;

		String normalizedZoneStr = zoneStr.trim();
		if (normalizedZoneStr.startsWith("GMT") || normalizedZoneStr.startsWith("UTC")) {
			normalizedZoneStr = normalizedZoneStr.substring(3).trim();
		}

		final boolean isNegative = normalizedZoneStr.startsWith("-");
		if (!isNegative && !normalizedZoneStr.startsWith("+")) {
			throw new RosemaryDateTimeException("Bad time zone offset: " + zoneStr);
		}

		int hour = 0;
		int minute = 0;

		final String[] tList = normalizedZoneStr.substring(1).split(":");
		for (String token : tList) {
			final int val;
			try {
				val = Integer.parseInt(token);
			} catch (NumberFormatException nfe) {
				throw new RosemaryDateTimeException("Bad time zone " + place.name() + " offset: " + token);
			}

			switch (place) {
				case HOUR:
					hour = val;
					place = TimePlace.MINUTE;
					break;
				case MINUTE:
					minute = val;
					if (minute > 59) {
						throw new RosemaryDateTimeException("Bad time zone " + place.name() + " offset: " + token);
					}
					place = TimePlace.UNKNOWN;
					break;
				default:
					throw new RosemaryDateTimeException("Unexpected place value " + place);
			}
		}

		final String customID = "GMT" + (isNegative ? "-" : "+") + hour + ":" + (minute < 10 ? "0" : "") + minute;

		state.setTimeZone(TimeZone.getTimeZone(customID));
	}

	/**
	 * Parse a non-numeric token from the datetime string.
	 *
	 * @param token to parse
	 * @param tokens all the tokens
	 * @param state parser state
	 * @throws RosemaryDateTimeException if there was a problem parsing the token
	 */
	private void parseNonNumericToken(String token, String[] tokens, RosemaryDateTimeState state) throws RosemaryDateTimeException {
		// if it's a weekday name, ignore it
		if (RosemaryDateTimeParser.isWeekdayName(token)) {
			return;
		}

		// we already assume dates are GMT or UTC, ignore it
		if (token.equalsIgnoreCase("GMT") || token.equalsIgnoreCase("UTC")) {
			return;
		}

		// does the token look like a timezone offset?
		if ((token.startsWith("+") || token.startsWith("-")) || ((token.startsWith("GMT") || token.startsWith("UTC")) && (token.contains("+") || token.contains("-")))) {
			parseTimeZoneOffset(token, state);
			return;
		}

		// does the token look like a time?
		if (token.indexOf(':') > 0 || (token.length() > 2 && (token.endsWith("AM") || token.endsWith("PM")))) {
			if (Character.isDigit(token.charAt(0))) {
				parseTime(token, state);
				return;
			} else {
				throw new RosemaryDateTimeException("Unrecognized time: " + token);
			}
		}

		// try to parse month name
		Integer tmpMon = RosemaryDateTimeParser.monthNameToNumber(token);

		// if token isn't a month name
		if (tmpMon != null) {
			// if month number is not set, set it and move on
			if (!state.isMonthSet()) {
				state.setMonth(tmpMon);
				return;
			}

			// try to move the current month value to the year or day
			if (!state.isYearSet()) {
				if (state.isDaySet() || state.isYearBeforeDay()) {
					state.setYear(state.getMonth());
					state.setMonth(tmpMon);
				} else {
					state.setDay(state.getMonth());
					state.setMonth(tmpMon);
				}

				return;
			}

			// year was already set, so try to move month value to day
			if (!state.isDaySet()) {
				state.setDay(state.getMonth());
				state.setMonth(tmpMon);
				return;
			}

			// can't move month value to year or day...
			throw new RosemaryDateTimeException("Too many values");
		}

		// maybe it's an ordinal number list "1st", "Fifth", "23rd", etc.
		Integer val = RosemaryDateTimeParser.getOrdinalNumber(token);
		if (val == null) {
			final String upperCaseToken = token.toUpperCase();

			if (upperCaseToken.equals("AM")) {
				if (!state.isHourSet()) {
					state.setTimePostMeridian(false);
				} else {
					state.setHour(state.getHour() % 12);
				}
				return;
			} else if (upperCaseToken.equals("PM")) {
				if (!state.isHourSet()) {
					state.setTimePostMeridian(true);
				} else {
					state.setHour((state.getHour() % 12) + 12);
				}
				return;
			} else {
				TimeZone tz = timeZoneNameToTimeZone.get(upperCaseToken);
				if (tz != null) {
					state.setTimeZone(tz);
					return;
				}
			}
			return;
		}

		// if no day yet, we're done
		if (!state.isDaySet()) {
			state.setDay(val);
			return;
		}

		// if either year or month is not set...
		if (!state.isYearSet() || !state.isMonthSet()) {

			// if day can't be a month, shift it into year
			if (state.getDay() > 12) {
				if (!state.isYearSet()) {
					state.setYear(state.getDay());
					state.setDay(val);
					return;
				}

				// year was already set, maybe we can move it to month
				if (state.getYear() <= 12) {
					state.setMonth(state.getYear());
					state.setYear(state.getDay());
					state.setDay(val);
					return;
				}

				// try to shift day value to either year or month
			} else if (!state.isYearSet()) {
				if (!state.isMonthSet() && !state.isYearBeforeMonth()) {
					state.setMonth(state.getDay());
					state.setDay(val);
					return;
				}

				state.setYear(state.getDay());
				state.setDay(val);
				return;

				// year was set, so we know month is not set
			} else {
				state.setMonth(state.getDay());
				state.setDay(val);
				return;
			}
		}

		throw new RosemaryDateTimeException("Cannot assign ordinal");
	}

	/**
	 * Split a large numeric value into a year/month/date values.
	 *
	 * @param val numeric value to use
	 * @param state parser state
	 * @throws RosemaryDateTimeException if there was a problem splitting the value
	 */
	private void parseNumericBlob(int val, RosemaryDateTimeState state) throws RosemaryDateTimeException {
		if (state.isYearSet() || state.isMonthSet() || state.isDaySet()) {
			throw new RosemaryDateTimeException("Unknown value: " + val);
		}

		int tmpVal = val;
		if (state.isYearBeforeMonth()) {
			if (state.isYearBeforeDay()) {
				final int last = tmpVal % 100;
				tmpVal /= 100;

				final int middle = tmpVal % 100;
				tmpVal /= 100;

				state.setYear(tmpVal);
				if (state.isMonthBeforeDay()) {
					// YYYYMMDD
					state.setMonth(middle);
					state.setDay(last);
				} else {
					// YYYYDDMM
					state.setDay(middle);
					state.setMonth(last);
				}
			} else {
				// DDYYYYMM
				state.setMonth(tmpVal % 100);
				tmpVal /= 100;

				state.setYear(tmpVal % 10000);
				tmpVal /= 10000;

				state.setDay(tmpVal);
			}
		} else if (state.isYearBeforeDay()) {
			// MMYYYYDD
			state.setDay(tmpVal % 100);
			tmpVal /= 100;

			state.setYear(tmpVal % 10000);
			tmpVal /= 10000;

			state.setMonth(tmpVal);
		} else {
			state.setYear(tmpVal % 10000);
			tmpVal /= 10000;

			final int middle = tmpVal % 100;
			tmpVal /= 100;
			if (state.isMonthBeforeDay()) {
				// MMDDYYYY
				state.setDay(middle);
				state.setMonth(tmpVal);
			} else {
				// DDMMYYYY
				state.setDay(tmpVal);
				state.setMonth(middle);
			}
		}
	}

	/**
	 * Use a numeric token from the datetime string.
	 *
	 * @param numericToken numeric value to use
	 * @param tokens all the tokens
	 * @param state parser state
	 * @throws RosemaryDateTimeException if there was a problem parsing the token
	 */
	private void parseNumericToken(long numericToken, String[] tokens, RosemaryDateTimeState state) throws RosemaryDateTimeException {
		// if we've already found 3 values
		if (state.isYearSet() && state.isMonthSet() && state.isDaySet() && state.isTimeZoneSet()) {
			throw new RosemaryDateTimeException("Extra value: " + numericToken);
		}

		// maybe a timezone offset
		if (state.isYearSet() && state.isMonthSet() && state.isDaySet() && !state.isTimeZoneSet()) {
			int timeZoneHour = 0;
			int timeZoneMinute = 0;
			long absNumericToken = Math.abs(numericToken);
			if ((absNumericToken < 24) || (absNumericToken >= 100 && absNumericToken < 2400 && ((absNumericToken%100) < 60) && ((absNumericToken%100)%15 == 0))) {
				boolean isNegative = numericToken < 0;
				if (absNumericToken < 24) {
					timeZoneHour = (int)absNumericToken;
				} else {
					timeZoneHour = ((int)absNumericToken)/100;
					timeZoneMinute = ((int)absNumericToken)%100;
				}
				final String customID = "GMT" + (isNegative ? "-" : "+") + timeZoneHour + ":" + String.format("%02d", timeZoneMinute);
				state.setTimeZone(TimeZone.getTimeZone(customID));
				return;
			}
			throw new RosemaryDateTimeException("Found unknown number");
		}

		// negative numbers
		if (numericToken < 0) {
			throw new RosemaryDateTimeException("Found negative number");
		}

		// parse as milliseconds after epoch, if val doesn't match a YYYYDDMM like format
		if (tokens.length == 1 && (numericToken > 99993112L || numericToken < 1970L)) {
			if (state.isYearSet() || state.isMonthSet() || state.isDaySet()) {
				throw new RosemaryDateTimeException("Unknown value: " + numericToken);
			}
			Calendar tempCalendar = new GregorianCalendar(TimeZone.getTimeZone("Z"));
			tempCalendar.clear();
			tempCalendar.setTimeInMillis(numericToken);

			state.setYear(tempCalendar.get(Calendar.YEAR));
			state.setMonth(tempCalendar.get(Calendar.MONTH) + 1);
			state.setDay(tempCalendar.get(Calendar.DATE));
			state.setHour(tempCalendar.get(Calendar.HOUR_OF_DAY));
			state.setMinute(tempCalendar.get(Calendar.MINUTE));
			state.setSecond(tempCalendar.get(Calendar.SECOND));
			state.setMillisecond(tempCalendar.get(Calendar.MILLISECOND));
			state.setTimeZone(tempCalendar.getTimeZone());
			return;
		}

		int intNumericToken = (int)numericToken;

		if (intNumericToken > 9999) {
			parseNumericBlob(intNumericToken, state);
			return;
		}

		// deal with obvious years first
		if (intNumericToken > 31) {

			// if no year yet, assign it and move on
			if (!state.isYearSet()) {
				state.setYear(intNumericToken);
				return;
			}

			// puke if the year value can't possibly be a day or month
			if (state.getYear() > 31) {
				throw new RosemaryDateTimeException("Bad year");
			}

			// if the year value can't be a month...
			if (state.getYear() > 12) {
				// if day isn't set, use old val as day and new val as year
				if (!state.isDaySet()) {
					state.setDay(state.getYear());
					state.setYear(intNumericToken);
					return;
				}

				// NOTE: both day and year are set

				// try using day value as month
				// value to day and use new value as year
				if (state.getDay() <= 12) {
					state.setMonth(state.getDay());
					state.setDay(state.getYear());
					state.setYear(intNumericToken);
					return;
				}
				throw new RosemaryDateTimeException("Invalid value: " + intNumericToken);
			}

			// else year <= 12
			if (!state.isDaySet() && !state.isMonthSet()) {
				if (state.isMonthBeforeDay()) {
					state.setMonth(state.getYear());
					state.setYear(intNumericToken);
				} else {
					state.setDay(state.getYear());
					state.setYear(intNumericToken);
				}
				return;
			}

			if (!state.isDaySet()) {
				state.setDay(state.getYear());
				state.setYear(intNumericToken);
				return;
			}

			// assume this was a mishandled month
			state.setMonth(state.getYear());
			state.setYear(intNumericToken);
			return;
		}

		// now deal with non-month values
		if (intNumericToken > 12) {

			// if no year value yet...
			if (!state.isYearSet()) {

				// if the day is set, or if we assign year before day...
				if (state.isDaySet() || state.isYearBeforeDay()) {
					state.setYear(intNumericToken);
				} else {
					state.setDay(intNumericToken);
				}
				return;
			}

			// NOTE: year is set

			// if no day value yet, assign it and move on
			if (!state.isDaySet()) {
				state.setDay(intNumericToken);
				return;
			}

			// NOTE: both year and day are set

			throw new RosemaryDateTimeException("Invalid value: " + intNumericToken);
		}

		// NOTE: ambiguous value

		// if year is set, this must be either the month or day
		if (state.isYearSet()) {
			if (state.isMonthSet() || (!state.isDaySet() && !state.isMonthBeforeDay())) {
				state.setDay(intNumericToken);
			} else {
				state.setMonth(intNumericToken);
			}

			return;
		}

		// NOTE: year not set

		// if month is set, this must be either the year or day
		if (state.isMonthSet()) {
			if (state.isDaySet() || state.isYearBeforeDay()) {
				state.setYear(intNumericToken);
			} else {
				state.setDay(intNumericToken);
			}

			return;
		}

		// NOTE: neither year nor month is set

		// if day is set, this must be either the year or month
		if (state.isDaySet()) {
			if (state.isYearBeforeMonth()) {
				state.setYear(intNumericToken);
			} else {
				state.setMonth(intNumericToken);
			}

			return;
		}

		// NOTE: no intNumericToken set yet
		if (state.isYearBeforeMonth()) {
			if (state.isYearBeforeDay()) {
				state.setYear(intNumericToken);
			} else {
				state.setDay(intNumericToken);
			}
		} else if (state.isMonthBeforeDay()) {
			state.setMonth(intNumericToken);
		} else {
			state.setDay(intNumericToken);
		}
	}

	/**
	 * The main entry point into the real parsing of a datetime.  This method is responsible for tokenizing the datetime string and parsing each token.
	 *
	 * @param dateTimeString the datetime to parse
	 * @param rosemaryParserDateOrder the order in which to expect and resolve ambiguous date formats. e.g. 03/04/05 Could be March 4th 2005 or April 3rd 2005 or April 5th 2003
	 * @param defaultDateTimeState the default date or time to use if incomplete information is provided in the dateTimeString.  e.g. {@link RosemaryDateTimeParser#parse(String March 5th)} doesn't specify a year, and the current year is used by default.  Use this to alter the default year, month, timezone, etc.
	 * @return parsed dateTimeString
	 * @throws RosemaryDateTimeException if the dateTimeString is invalid
	 */
	private OffsetDateTime parseInternal(final String dateTimeString, final RosemaryDateOrder rosemaryParserDateOrder, final RosemaryDateTimeState defaultDateTimeState) throws RosemaryDateTimeException {
		if (dateTimeString == null) {
			return null;
		}

		final String upperDateTimeString = dateTimeString.toUpperCase();
		final RosemaryDateTimeState dateTimeState = new RosemaryDateTimeState(rosemaryParserDateOrder);

		// split on dash, unless it is prefixed by whitespace or the text.  if the text is prefixed, then it is likely GMT or UTC and there is a timezone offset nearby
		// split on forward slash, unless it is prefixed and suffixed by A-Z characters, which means it is likely a timezone string like: America/Detroit
		// split on whitespace, comma, parentheses, single quote, double quote.
		final Pattern tokenPattern = Pattern.compile("((?<!([\\s\\h])|[A-Za-z()])-|((?<=[^A-Za-z])/(?=[^A-za-z]))|[\\s\\h,)('\"])+");
		String[] tokens = tokenPattern.split(upperDateTimeString);
		// remove boring tokens that might exist based on the regex
		tokens = Arrays.stream(tokens).filter(token -> {
			return !(token.matches("-+") || token.isBlank());
		}).toArray(String[]::new);
		for (String token : tokens) {
			try {
				// try to decipher next token as a number
				try {
					final long numericToken = Long.parseLong(token);
					parseNumericToken(numericToken, tokens, dateTimeState);
				} catch (NumberFormatException e) {
					parseNonNumericToken(token, tokens, dateTimeState);
				}
			} catch (RosemaryDateTimeException e) {
				throw new RosemaryDateTimeException("Unable to parse: " + dateTimeString, e);
			}
		}

		if (!dateTimeState.isYearSet() && !dateTimeState.isMonthSet() && !dateTimeState.isDaySet() && !dateTimeState.isHourSet() && !dateTimeState.isMinuteSet() && !dateTimeState.isSecondSet() && !dateTimeState.isMillisecondSet()) {
			throw new RosemaryDateTimeException("Unable to parse: " + dateTimeString);
		}

		// if the month is set, but not the day use the first of the month
		if (dateTimeState.isMonthSet() && !dateTimeState.isDaySet()) {
			dateTimeState.setDay(1);
		}

		// if year is set, but not the month and day use the first day on the first month
		if (dateTimeState.isYearSet() && !dateTimeState.isMonthSet() && !dateTimeState.isDaySet()) {
			dateTimeState.setMonth(1);
			dateTimeState.setDay(1);
		}

		// use default state
		if (!dateTimeState.isYearSet() && defaultDateTimeState.isYearSet()) {
			dateTimeState.setYear(defaultDateTimeState.getYear());
		}

		if (!dateTimeState.isMonthSet() && defaultDateTimeState.isMonthSet()) {
			dateTimeState.setMonth(defaultDateTimeState.getMonth());
		}

		if (!dateTimeState.isDaySet() && defaultDateTimeState.isDaySet()) {
			dateTimeState.setDay(defaultDateTimeState.getDay());
		}

		if (!dateTimeState.isHourSet() && defaultDateTimeState.isHourSet()) {
			dateTimeState.setHour(defaultDateTimeState.getHour());
		}

		if (!dateTimeState.isMinuteSet() && defaultDateTimeState.isMinuteSet()) {
			dateTimeState.setMinute(defaultDateTimeState.getMinute());
		}

		if (!dateTimeState.isSecondSet() && defaultDateTimeState.isSecondSet()) {
			dateTimeState.setSecond(defaultDateTimeState.getSecond());
		}

		if (!dateTimeState.isMillisecondSet() && defaultDateTimeState.isMillisecondSet()) {
			dateTimeState.setMillisecond(defaultDateTimeState.getMillisecond());
		}

		if (!dateTimeState.isTimeZoneSet() && defaultDateTimeState.isTimeZoneSet()) {
			dateTimeState.setTimeZone(defaultDateTimeState.getTimeZone());
		}

		// if the month is set, but not the day use the first of the month
		if (dateTimeState.isMonthSet() && !dateTimeState.isDaySet()) {
			dateTimeState.setDay(1);
		}

		// if year is set, but not the month and day use the first day on the first month
		if (dateTimeState.isYearSet() && !dateTimeState.isMonthSet() && !dateTimeState.isDaySet()) {
			dateTimeState.setMonth(1);
			dateTimeState.setDay(1);
		}

		// if day, month and year are missing, but hour, minute, second or millisecond is set, then assume 1970-01-01
		if (!dateTimeState.isYearSet() && !dateTimeState.isMonthSet() && !dateTimeState.isDaySet() && (dateTimeState.isHourSet() || dateTimeState.isMinuteSet() || dateTimeState.isSecondSet() || dateTimeState.isMillisecondSet())) {
			dateTimeState.setYear(1970);
			dateTimeState.setMonth(1);
			dateTimeState.setDay(1);
		}

		if (dateTimeState.isYearSet()) {
			// if year is one or two digits, split it around the millennium
			final Integer tmpYear = dateTimeState.getYear();
			if (tmpYear < 70) {
				dateTimeState.setYear(tmpYear + 2000); // 2000 to 2069
			} else if (tmpYear < 100) {
				dateTimeState.setYear(tmpYear + 1900); // 1970 to 1999
			}
		}

		// throw exception if day, month or year is missing
		final List<Boolean> dayMonthYearMissing = List.of(!dateTimeState.isDaySet(), !dateTimeState.isMonthSet(), !dateTimeState.isYearSet());

		if (dayMonthYearMissing.contains(Boolean.TRUE)) {
			final List<String> dayMonthYear = List.of("day", "month", "year");
			final String missingText = String.join(", ", IntStream.range(0, dayMonthYearMissing.size())
				.filter(i -> dayMonthYearMissing.get(i).equals(Boolean.TRUE))
				.mapToObj(dayMonthYear::get)
				.toList());
			throw new RosemaryDateTimeException("Missing " + missingText + " in: " + dateTimeString);
		}

		return ParsingUtilities.calendarToOffsetDateTime(dateTimeState.asCalendar());
	}

	/**
	 * Parses a datetime using the given format as defined by {@link java.time.format.DateTimeFormatter}.
	 *
	 * @param dateTimeString the datetime to parse
	 * @param formats list of formats to try in order for {@link java.time.format.DateTimeFormatter} or {@link java.text.SimpleDateFormat}
	 * @param locale the locale to use
	 * @return parsed dateTimeString
	 * @throws RosemaryDateTimeException if the dateTimeString is invalid
	 */
	private OffsetDateTime parseInternal(final String dateTimeString, final List<String> formats, final Locale locale) throws RosemaryDateTimeException {
		OffsetDateTime offsetDateTime;
		// try each format in the list
		for (String format : formats) {
			offsetDateTime = ParsingUtilities.javaTimeParse(dateTimeString, format, locale);
			if (offsetDateTime != null) {
				return offsetDateTime;
			}
		}
		// try without formats
		offsetDateTime = ParsingUtilities.javaTimeParse(dateTimeString);
		if (offsetDateTime != null) {
			return offsetDateTime;
		} else {
			throw new RosemaryDateTimeException("Unable to parse " + dateTimeString);
		}
	}

	/**
	 * Parses a datetime using the given format as defined by {@link java.time.format.DateTimeFormatter}.
	 *
	 * @param dateTimeString the datetime to parse
	 * @param formats list of formats to try in order for {@link java.time.format.DateTimeFormatter} or {@link java.text.SimpleDateFormat}
	 * @return parsed dateTimeString
	 * @throws RosemaryDateTimeException if the dateTimeString is invalid
	 */
	private OffsetDateTime parseInternal(final String dateTimeString, final List<String> formats) throws RosemaryDateTimeException {
		Locale locale = Locale.getDefault(Locale.Category.FORMAT);
		formatLoop:
		for (String format : formats) {
			Locale possibleLocale = Locale.forLanguageTag(format);
			for (Locale l : DateFormat.getAvailableLocales()) {
				if (l.equals(possibleLocale)) {
					locale = possibleLocale;
					break formatLoop;
				}
			}
		}
		return parseInternal(dateTimeString, formats, locale);
	}

	/**
	 * Parses a datetime using the given format as defined by {@link java.time.format.DateTimeFormatter}.
	 *
	 * @param dateTimeString the datetime to parse
	 * @param formats list of formats to try in order for {@link java.time.format.DateTimeFormatter} or {@link java.text.SimpleDateFormat}
	 * @return parsed dateTimeString
	 * @throws RosemaryDateTimeException if the dateTimeString is invalid
	 */
	public OffsetDateTime parse(String dateTimeString, List<String> formats) throws RosemaryDateTimeException {
		return parseInternal(dateTimeString, formats);
	}

	/**
	 * Parses a datetime using the given format as defined by {@link java.time.format.DateTimeFormatter}.
	 *
	 * @param dateTimeString the datetime to parse
	 * @param format for {@link java.time.format.DateTimeFormatter} or {@link java.text.SimpleDateFormat}
	 * @return parsed dateTimeString
	 * @throws RosemaryDateTimeException if the dateTimeString is invalid
	 */
	public OffsetDateTime parse(String dateTimeString, String format) throws RosemaryDateTimeException {
		return parse(dateTimeString, List.of(format));
	}

	/**
	 * Parses a datetime.  The datetime can be in any format.  Rosemary will try to determine the correct structure of the datetime.
	 *
	 * @param dateTimeString the datetime to parse
	 * @param rosemaryParserDateOrder the order in which to expect and resolve ambiguous date formats. e.g. 03/04/05 Could be March 4th 2005 or April 3rd 2005 or April 5th 2003
	 * @param defaultDateTimeState the default date or time to use if incomplete information is provided in the dateTimeString.  e.g. {@link RosemaryDateTimeParser#parse(String March 5th)} doesn't specify a year, and the current year is used by default.  Use this to alter the default year, month, timezone, etc.
	 * @return parsed dateTimeString
	 * @throws RosemaryDateTimeException if the dateTimeString is invalid
	 */
	public OffsetDateTime parse(String dateTimeString, RosemaryDateOrder rosemaryParserDateOrder, final RosemaryDateTimeState defaultDateTimeState) throws RosemaryDateTimeException {
		try {
			return parseInternal(dateTimeString, rosemaryParserDateOrder, defaultDateTimeState);
		} catch (RosemaryDateTimeException e1) {
			try {
				return parse(dateTimeString, List.of()); // try java parsers without formats
			} catch (RosemaryDateTimeException e2) {
				// throw the original error
				throw e1;
			}
		}
	}

	/**
	 * Parses a datetime.  The datetime can be in any format.  Rosemary will try to determine the correct structure of the datetime.  Equivalent to {@link RosemaryDateTimeParser#parse(String, RosemaryDateOrder, RosemaryDateTimeState)} with a {@link RosemaryDateTimeState} of current year, month and day obtained from the {@link Calendar#getInstance()}
	 *
	 * @param dateTimeString the datetime to parse
	 * @param rosemaryParserDateOrder the order in which to expect and resolve ambiguous date formats. e.g. 03/04/05 Could be March 4th 2005 or April 3rd 2005 or April 5th 2003
	 * @return parsed dateTimeString
	 * @throws RosemaryDateTimeException if the dateTimeString is invalid
	 */
	public OffsetDateTime parse(String dateTimeString, RosemaryDateOrder rosemaryParserDateOrder) throws RosemaryDateTimeException {
		final RosemaryDateTimeState defaultDateTimeState = new RosemaryDateTimeState(rosemaryParserDateOrder);
		final Calendar defaultCalendar = Calendar.getInstance();
		defaultDateTimeState.setYear(defaultCalendar.get(Calendar.YEAR));
		defaultDateTimeState.setMonth(defaultCalendar.get(Calendar.MONTH) + 1);
		defaultDateTimeState.setDay(defaultCalendar.get(Calendar.DATE));
		return parse(dateTimeString, rosemaryParserDateOrder, defaultDateTimeState);
	}

	/**
	 * Parses a datetime.  The datetime can be in any format.  Rosemary will try to determine the correct structure of the datetime.  Equivalent to {@link RosemaryDateTimeParser#parse(String, RosemaryDateOrder MM_DD_YY)} with {@link RosemaryDateOrder#MM_DD_YY}
	 *
	 * @param dateTimeString the datetime to parse
	 * @return parsed dateTimeString
	 * @throws RosemaryDateTimeException if the dateTimeString is invalid
	 */
	public OffsetDateTime parse(String dateTimeString) throws RosemaryDateTimeException {
		return parse(dateTimeString, RosemaryDateOrder.MM_DD_YY);
	}
}