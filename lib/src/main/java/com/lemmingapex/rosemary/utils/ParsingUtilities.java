package com.lemmingapex.rosemary.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Contains utility methods for parsing date/time strings.
 */
public class ParsingUtilities {
	/**
	 * Tries to parse a date using the provided format and locale with {@link java.time}.
	 *
	 * @param dateTimeString
	 * @param format to be interrupted by {@link DateTimeFormatter}.  {@link SimpleDateFormat} will be used if {@link DateTimeFormatter} fails.
	 * @param locale
	 * @return
	 */
	static public OffsetDateTime javaTimeParse(String dateTimeString, String format, Locale locale) {
		try {
			return ZonedDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(format, locale)).toOffsetDateTime();
		} catch (Exception e) {

		}

		try {
			return OffsetDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(format, locale));
		} catch (Exception e) {

		}

		try {
			return OffsetDateTime.of(LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(format, locale).withZone(ZoneId.of("UTC"))), ZoneOffset.UTC);
		} catch (Exception e) {

		}

		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, locale);
			simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Z"));
			Date date = simpleDateFormat.parse(dateTimeString);
			GregorianCalendar c = new GregorianCalendar();
			c.setTime(date);
			return ParsingUtilities.calendarToOffsetDateTime(c).withOffsetSameInstant(ZoneOffset.UTC);
		} catch (Exception e) {

		}

		return null;
	}

	public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 * Tries to parse a datetime using the default {@link ParsingUtilities#DATETIME_FORMAT} with {@link java.time}.
	 *
	 * @param dateTimeString
	 * @return
	 */
	static public OffsetDateTime javaTimeParse(String dateTimeString) {
		try {
			return OffsetDateTime.parse(dateTimeString, DATETIME_FORMAT);
		} catch (DateTimeParseException e) {

		}

		try {
			return OffsetDateTime.parse(dateTimeString);
		} catch (DateTimeParseException e) {

		}

		// Also accept timestamps without an explicit zone and assume them to be in UTC time.
		try {
			return OffsetDateTime.of(LocalDateTime.parse(dateTimeString, DATETIME_FORMAT), ZoneOffset.UTC);
		} catch (DateTimeParseException e) {

		}

		try {
			return OffsetDateTime.of(LocalDateTime.parse(dateTimeString), ZoneOffset.UTC);
		} catch (DateTimeParseException e) {

		}
		return null;
	}

	/**
	 * Converts a {@link GregorianCalendar} to an {@link OffsetDateTime}.
	 * @param calendar
	 * @return
	 */
	static public OffsetDateTime calendarToOffsetDateTime(GregorianCalendar calendar) {
		return calendar.toZonedDateTime().toOffsetDateTime();
	}
}
