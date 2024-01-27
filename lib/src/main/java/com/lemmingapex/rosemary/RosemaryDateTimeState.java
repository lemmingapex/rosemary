package com.lemmingapex.rosemary;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Datetime state used by {@link RosemaryDateTimeParser}.
 */
public class RosemaryDateTimeState {
	/**
	 * bit indicating that the year comes before the month.
	 */
	static final int YEAR_BEFORE_MONTH = 0x4;
	/**
	 * bit indicating that the year comes before the day.
	 */
	static final int YEAR_BEFORE_DAY = 0x2;
	/**
	 * bit indicating that the month comes before the day.
	 */
	static final int MONTH_BEFORE_DAY = 0x1;

	/**
	 * bit indicating that the year comes after the month.
	 */
	static final int YEAR_AFTER_MONTH = 0x0;
	/**
	 * bit indicating that the year comes after the day.
	 */
	static final int YEAR_AFTER_DAY = 0x0;
	/**
	 * bit indicating that the month comes after the day.
	 */
	static final int MONTH_AFTER_DAY = 0x0;

	/**
	 * <code>true</code> if year should appear before month.
	 */
	private final boolean yearBeforeMonth;
	/**
	 * <code>true</code> if year should appear before day.
	 */
	private final boolean yearBeforeDay;
	/**
	 * <code>true</code> if month should appear before day.
	 */
	private final boolean monthBeforeDay;

	/**
	 * year.
	 */
	private Integer year = null;
	/**
	 * month (0-11).
	 */
	private Integer month = null;
	/**
	 * day of month.
	 */
	private Integer day = null;
	/**
	 * hour (0-23).
	 */
	private Integer hour = null;
	/**
	 * minute (0-59).
	 */
	private Integer minute = null;
	/**
	 * second (0-59).
	 */
	private Integer second = null;
	/**
	 * millisecond (0-999).
	 */
	private Integer millisecond = null;

	/**
	 * time zone (use default time zone if this is <code>null</code>).
	 */
	private TimeZone timeZone = null;

	/**
	 * <code>true</code> if time is after noon, false otherwise.
	 */
	private boolean timePostMeridian = false;

	/**
	 * Datetime state used by {@link RosemaryDateTimeParser}.
	 *
	 * @param rosemaryParserDateOrder the order in which the date will likely appear
	 */
	public RosemaryDateTimeState(RosemaryDateOrder rosemaryParserDateOrder) {
		yearBeforeMonth = (rosemaryParserDateOrder.order & YEAR_BEFORE_MONTH) == YEAR_BEFORE_MONTH;
		yearBeforeDay = (rosemaryParserDateOrder.order & YEAR_BEFORE_DAY) == YEAR_BEFORE_DAY;
		monthBeforeDay = (rosemaryParserDateOrder.order & MONTH_BEFORE_DAY) == MONTH_BEFORE_DAY;
	}

	/**
	 * Get year.
	 *
	 * @return year >0
	 */
	public Integer getYear() {
		return this.year;
	}

	/**
	 * Get month of year.
	 *
	 * @return month of the year: 1-12
	 */
	public Integer getMonth() {
		return this.month;
	}

	/**
	 * Get day of month.
	 *
	 * @return day of month: 1-31
	 */
	public Integer getDay() {
		return this.day;
	}

	/**
	 * Get hour of day.
	 *
	 * @return hour of the day: 0-23
	 */
	public Integer getHour() {
		return this.hour;
	}

	/**
	 * Get minute in hour.
	 *
	 * @return minute in the hour: 0-59
	 */
	public Integer getMinute() {
		return this.minute;
	}

	/**
	 * Get second.
	 *
	 * @return second: 0-59
	 */
	public Integer getSecond() {
		return this.second;
	}

	/**
	 * Get millisecond.
	 *
	 * @return millisecond: 0-999
	 */
	public Integer getMillisecond() {
		return this.millisecond;
	}

	/**
	 * Get time zone.
	 *
	 * @return time zone (<code>null</code> if none was specified)
	 */
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	/**
	 * Is the year set?
	 *
	 * @return <code>true</code> if a year has been assigned
	 */
	public boolean isYearSet() {
		return (this.year != null);
	}

	/**
	 * Is month of year set?
	 *
	 * @return <code>true</code> if a month has been assigned
	 */
	public boolean isMonthSet() {
		return (this.month != null);
	}

	/**
	 * Is day of month set?
	 *
	 * @return <code>true</code> if a day has been assigned
	 */
	public boolean isDaySet() {
		return this.day != null;
	}

	/**
	 * Is hour set?
	 *
	 * @return <code>true</code> if a hour has been assigned
	 */
	public boolean isHourSet() {
		return (this.hour != null);
	}

	/**
	 * Is minute set?
	 *
	 * @return <code>true</code> if a minute has been assigned
	 */
	public boolean isMinuteSet() {
		return (this.minute != null);
	}

	/**
	 * Is second set?
	 *
	 * @return <code>true</code> if a second has been assigned
	 */
	public boolean isSecondSet() {
		return (this.second != null);
	}

	/**
	 * Is millisecond set?
	 *
	 * @return <code>true</code> if a millisecond has been assigned
	 */
	public boolean isMillisecondSet() {
		return (this.millisecond != null);
	}

	/**
	 * Is the time zone set?
	 *
	 * @return <code>true</code> if a time zone has been assigned
	 */
	public boolean isTimeZoneSet() {
		return this.timeZone != null;
	}

	/**
	 * Is the time post-meridian (afternoon)?
	 *
	 * @return <code>true</code> if time is P.M.
	 */
	public boolean isTimePostMeridian() {
		return (this.timePostMeridian || this.hour > 12);
	}


	/**
	 * Is a numeric year placed before a numeric day of month?
	 *
	 * @return <code>true</code> if year is before day of month
	 */
	public boolean isYearBeforeDay() {
		return this.yearBeforeDay;
	}

	/**
	 * Is a numeric year placed before a numeric month?
	 *
	 * @return <code>true</code> if year is before month
	 */
	public boolean isYearBeforeMonth() {
		return this.yearBeforeMonth;
	}

	/**
	 * Is a numeric month placed before a numeric day of month?
	 *
	 * @return <code>true</code> if month is before day of month
	 */
	public boolean isMonthBeforeDay() {
		return this.monthBeforeDay;
	}

	/**
	 * Set the year.
	 *
	 * @param year 0 - 9999
	 * @throws RosemaryDateTimeException if the value is not a valid year
	 */
	public void setYear(int year) throws RosemaryDateTimeException {
		if (year < 0) {
			throw new RosemaryDateTimeException("Bad year " + year);
		}

		this.year = year;
	}

	/**
	 * Set the month of the year.
	 *
	 * @param month month of the year: 1-12
	 * @throws RosemaryDateTimeException if the value is not a valid month of the year
	 */
	public void setMonth(int month) throws RosemaryDateTimeException {
		if (month < 1 || month > 12) {
			throw new RosemaryDateTimeException("Bad month " + month);
		}

		this.month = month;
	}

	/**
	 * Set the day of month.
	 *
	 * @param day day of month: 1-31
	 * @throws RosemaryDateTimeException if the value is not a valid day of month
	 */
	public void setDay(int day) throws RosemaryDateTimeException {
		if (day < 1 || day > 31) {
			throw new RosemaryDateTimeException("Bad day " + day);
		}

		this.day = day;
	}

	/**
	 * Set the hour of the day.
	 *
	 * @param hour hour of the day: 0-23
	 * @throws RosemaryDateTimeException if the value is not a valid hour of the day
	 */
	public void setHour(int hour) throws RosemaryDateTimeException {
		final int tmpHour;
		if (timePostMeridian) {
			tmpHour = hour + 12;
			timePostMeridian = false;
		} else {
			tmpHour = hour;
		}

		if (tmpHour < 0 || tmpHour > 23) {
			throw new RosemaryDateTimeException("Bad hour " + hour);
		}

		this.hour = tmpHour;
	}

	/**
	 * Set the minute in the hour.
	 *
	 * @param minute minute in the hour: 0-59
	 * @throws RosemaryDateTimeException if the value is not a valid minute in the hour
	 */
	public void setMinute(int minute) throws RosemaryDateTimeException {
		if (minute < 0 || minute > 59) {
			throw new RosemaryDateTimeException("Bad minute " + minute);
		}

		this.minute = minute;
	}

	/**
	 * Set the second.
	 *
	 * @param second second: 0-59
	 * @throws RosemaryDateTimeException if the value is not a valid second
	 */
	public void setSecond(int second) throws RosemaryDateTimeException {
		if (second < 0 || second > 59) {
			throw new RosemaryDateTimeException("Bad second " + second);
		}

		this.second = second;
	}

	/**
	 * Set the millisecond.
	 *
	 * @param millisecond millisecond: 0-999
	 * @throws RosemaryDateTimeException if the value is not a valid millisecond
	 */
	public void setMillisecond(int millisecond) throws RosemaryDateTimeException {
		if (millisecond < 0 || millisecond > 999) {
			throw new RosemaryDateTimeException("Bad millisecond " + millisecond);
		}

		this.millisecond = millisecond;
	}

	/**
	 * Set the time zone.
	 *
	 * @param timeZone time zone
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Set the AM/PM indicator value.
	 *
	 * @param timePostMeridian <code>true</code> if time represented is after noon
	 */
	public void setTimePostMeridian(boolean timePostMeridian) {
		this.timePostMeridian = timePostMeridian;
	}

	/**
	 * Get the date time as a {@link GregorianCalendar}.
	 *
	 * @return {@link GregorianCalendar} representing the date time
	 */
	public GregorianCalendar asCalendar() {
		final GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("Z"));
		cal.clear();
		if (year != null) {
			cal.set(Calendar.YEAR, year);
		}
		if (month != null) {
			cal.set(Calendar.MONTH, month - 1);
		}
		if (day != null) {
			cal.set(Calendar.DATE, day);
		}

		if (hour != null) {
			cal.set(Calendar.HOUR_OF_DAY, hour);
			if (minute != null) {
				cal.set(Calendar.MINUTE, minute);
				if (second != null) {
					cal.set(Calendar.SECOND, second);
					if (millisecond != null) {
						cal.set(Calendar.MILLISECOND, millisecond);
					}
				}
			}

			if (timeZone != null) {
				cal.setTimeZone(timeZone);
			}
		}
		return cal;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RosemaryDateTimeState that = (RosemaryDateTimeState) o;
		return yearBeforeMonth == that.yearBeforeMonth && yearBeforeDay == that.yearBeforeDay && monthBeforeDay == that.monthBeforeDay && timePostMeridian == that.timePostMeridian && Objects.equals(year, that.year) && Objects.equals(month, that.month) && Objects.equals(day, that.day) && Objects.equals(hour, that.hour) && Objects.equals(minute, that.minute) && Objects.equals(second, that.second) && Objects.equals(millisecond, that.millisecond) && Objects.equals(timeZone, that.timeZone);
	}

	@Override
	public int hashCode() {
		return Objects.hash(yearBeforeMonth, yearBeforeDay, monthBeforeDay, year, month, day, hour, minute, second, millisecond, timeZone, timePostMeridian);
	}
}
