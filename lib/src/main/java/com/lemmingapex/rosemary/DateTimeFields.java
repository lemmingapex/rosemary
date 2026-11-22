package com.lemmingapex.rosemary;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * The date and time fields found so far while parsing one datetime string.  Not thread-safe, and only used for a single parse.
 */
final class DateTimeFields {
	private final RosemaryDateOrder dateOrder;

	private Integer year;
	/**
	 * <code>true</code> if the year was written with one or two digits, like '24, and still needs a century.
	 */
	private boolean twoDigitYear;
	private Integer month;
	private Integer day;
	private Integer hour;
	private Integer minute;
	private Integer second;
	private Integer nano;
	private ZoneId zone;
	/**
	 * <code>true</code> if the time zone is an offset, like -05:00, rather than a name, like EST.
	 */
	private boolean zoneIsOffset;
	/**
	 * <code>true</code> if the year was written first, with three or more digits, like 2024-12-05.
	 */
	private boolean yearWrittenFirst;

	/**
	 * AM/PM indicator to apply to the next hour that is set: <code>true</code> for PM, <code>false</code> for AM, <code>null</code> if none was given.
	 */
	private Boolean pendingPostMeridian;

	DateTimeFields(RosemaryDateOrder dateOrder) {
		this.dateOrder = dateOrder;
	}

	RosemaryDateOrder dateOrder() {
		return dateOrder;
	}

	boolean isYearSet() {
		return year != null;
	}

	boolean isMonthSet() {
		return month != null;
	}

	boolean isDaySet() {
		return day != null;
	}

	boolean isDateSet() {
		return isYearSet() && isMonthSet() && isDaySet();
	}

	boolean isHourSet() {
		return hour != null;
	}

	boolean isOffsetSet() {
		return zoneIsOffset;
	}

	boolean isYearWrittenFirst() {
		return yearWrittenFirst;
	}

	boolean isEmpty() {
		return year == null && month == null && day == null && hour == null && minute == null && second == null && nano == null;
	}

	int year() {
		return year;
	}

	boolean isTwoDigitYear() {
		return twoDigitYear;
	}

	int month() {
		return month;
	}

	int day() {
		return day;
	}

	int hour() {
		return hour;
	}

	ZoneId zone() {
		return zone;
	}

	/**
	 * Set the year.
	 *
	 * @param year the year, as written
	 * @param twoDigit <code>true</code> if the year was written with one or two digits, and still needs a century
	 */
	void setYear(int year, boolean twoDigit) throws ParseFailure {
		if (year < 0 || year > 9999) {
			throw new ParseFailure("Bad year " + year);
		}
		this.yearWrittenFirst = !twoDigit && month == null && day == null;
		this.year = year;
		this.twoDigitYear = twoDigit;
	}

	/**
	 * Move a value that was assumed to be a month or day into the year.  Such values have at most two digits.
	 */
	void setYearFrom(int monthOrDay) throws ParseFailure {
		setYear(monthOrDay, true);
	}

	void setMonth(int month) throws ParseFailure {
		if (month < 1 || month > 12) {
			throw new ParseFailure("Bad month " + month);
		}
		this.month = month;
	}

	void setDay(int day) throws ParseFailure {
		if (day < 1 || day > 31) {
			throw new ParseFailure("Bad day " + day);
		}
		this.day = day;
	}

	/**
	 * Set the hour.  If an AM/PM indicator is pending, it is applied to a 12-hour clock value and then cleared, e.g. 12 AM is hour 0 and 12 PM is hour 12.
	 */
	void setHour(int hour) throws ParseFailure {
		int hourOfDay = hour;
		if (pendingPostMeridian != null) {
			if (hour >= 0 && hour <= 12) {
				hourOfDay = (hour % 12) + (pendingPostMeridian ? 12 : 0);
			} else if (!pendingPostMeridian) {
				// a 24-hour clock value with a redundant PM, like 13:00PM, is kept, but 13:00AM is wrong
				throw new ParseFailure("Bad hour for AM: " + hour);
			}
			pendingPostMeridian = null;
		}
		if (hourOfDay < 0 || hourOfDay > 23) {
			throw new ParseFailure("Bad hour " + hour);
		}
		this.hour = hourOfDay;
	}

	/**
	 * Apply AM or PM to the hour that was already set.  A 24-hour clock value with a redundant PM, like 13:00 PM, is kept.
	 */
	void applyMeridianToHour(boolean postMeridian) throws ParseFailure {
		if (hour > 12) {
			if (!postMeridian) {
				throw new ParseFailure("Bad hour for AM: " + hour);
			}
			return;
		}
		hour = (hour % 12) + (postMeridian ? 12 : 0);
	}

	/**
	 * Remember AM or PM, and apply it to the next hour that is set.
	 */
	void setPendingMeridian(boolean postMeridian) {
		this.pendingPostMeridian = postMeridian;
	}

	/**
	 * @return <code>true</code> if AM or PM was given, but no hour was set after it
	 */
	boolean isMeridianPending() {
		return pendingPostMeridian != null;
	}

	void setMinute(int minute) throws ParseFailure {
		if (minute < 0 || minute > 59) {
			throw new ParseFailure("Bad minute " + minute);
		}
		this.minute = minute;
	}

	void setSecond(int second) throws ParseFailure {
		if (second < 0 || second > 59) {
			throw new ParseFailure("Bad second " + second);
		}
		this.second = second;
	}

	void setNano(int nano) throws ParseFailure {
		if (nano < 0 || nano > 999_999_999) {
			throw new ParseFailure("Bad fraction of a second " + nano);
		}
		this.nano = nano;
	}

	/**
	 * Set the time zone from a name, like EST or America/New_York.  An offset that was already set is more precise, and is kept,
	 * e.g. in "08:12:31 -0500 (EST)".
	 */
	void setZoneName(ZoneId zone) {
		if (!zoneIsOffset) {
			this.zone = zone;
		}
	}

	/**
	 * Set the time zone from an offset, like -05:00.  It replaces a time zone name, e.g. in "18:58:15 GMT -7".
	 *
	 * @throws ParseFailure if an offset was already set
	 */
	void setZoneOffset(ZoneOffset offset) throws ParseFailure {
		if (zoneIsOffset) {
			throw new ParseFailure("Extra time zone offset " + offset);
		}
		this.zone = offset;
		this.zoneIsOffset = true;
	}

	/**
	 * Fill in the missing fields, and build the datetime.
	 * <ul>
	 * <li>a month without a day is the first of the month, and a year alone is January 1st</li>
	 * <li>a missing year, month or day comes from today's date, according to the clock, in the datetime's time zone</li>
	 * <li>a missing time is midnight, and missing minutes, seconds and fractions of a second are zero</li>
	 * <li>a missing time zone is the default zone</li>
	 * <li>a two-digit year is placed in the century starting at <code>twoDigitYearStart</code></li>
	 * </ul>
	 *
	 * @param clock the clock that provides today's date
	 * @param defaultZone time zone to use if none was found
	 * @param twoDigitYearStart the first year of the 100-year range for two-digit years
	 * @return the datetime
	 * @throws ParseFailure if no date or time was found, or the date or time is invalid
	 */
	ZonedDateTime resolve(Clock clock, ZoneId defaultZone, int twoDigitYearStart) throws ParseFailure {
		if (isEmpty()) {
			throw new ParseFailure("No date or time found");
		}

		if (isMonthSet() && !isDaySet()) {
			day = 1;
		}
		if (isYearSet() && !isMonthSet() && !isDaySet()) {
			month = 1;
			day = 1;
		}

		final ZoneId resolvedZone = zone != null ? zone : defaultZone;
		if (!isYearSet() || !isMonthSet() || !isDaySet()) {
			final LocalDate today = clock.instant().atZone(resolvedZone).toLocalDate();
			if (!isYearSet()) {
				year = today.getYear();
				twoDigitYear = false;
			}
			if (!isMonthSet()) {
				month = today.getMonthValue();
			}
			if (!isDaySet()) {
				day = today.getDayOfMonth();
			}
		}

		int fullYear = year;
		if (twoDigitYear) {
			fullYear = twoDigitYearStart - Math.floorMod(twoDigitYearStart, 100) + year;
			if (fullYear < twoDigitYearStart) {
				fullYear += 100;
			}
		}

		final LocalDate date;
		try {
			date = LocalDate.of(fullYear, month, day);
		} catch (DateTimeException e) {
			throw new ParseFailure("Bad day " + day + " for month " + month + " of " + fullYear);
		}
		final LocalTime time = LocalTime.of(hour != null ? hour : 0, minute != null ? minute : 0, second != null ? second : 0, nano != null ? nano : 0);
		return ZonedDateTime.of(date, time, resolvedZone);
	}
}
