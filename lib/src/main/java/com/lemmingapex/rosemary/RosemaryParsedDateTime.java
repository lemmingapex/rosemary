package com.lemmingapex.rosemary;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A parsed datetime, with what the string had and what was filled in.  Returned by
 * {@link RosemaryDateTimeParser#parseDetailed(String)}.
 * <p>
 * A datetime is complete even when the string isn't: <code>"March 5"</code> is midnight on March 5th of this year, in the default
 * time zone.  These methods tell such a datetime from one that the string gave in full, and say whether the date order decided it:
 * <pre>{@code
 * RosemaryParsedDateTime parsed = rosemary.parseDetailed("3/5");
 * parsed.toOffsetDateTime(); // 2024-03-05T00:00Z
 * parsed.hasYear();          // false: this year
 * parsed.hasTime();          // false: midnight
 * parsed.hasZone();          // false: the default zone
 * parsed.isAmbiguous();      // true: day-first, it is May 3rd
 * }</pre>
 * Immutable and thread-safe.
 */
public final class RosemaryParsedDateTime {

	private final ZonedDateTime dateTime;
	private final boolean hasYear;
	private final boolean hasMonth;
	private final boolean hasDay;
	private final boolean hasTime;
	private final boolean hasZone;
	private final boolean ambiguous;

	RosemaryParsedDateTime(ZonedDateTime dateTime, boolean hasYear, boolean hasMonth, boolean hasDay, boolean hasTime, boolean hasZone,
		boolean ambiguous) {
		this.dateTime = Objects.requireNonNull(dateTime, "dateTime");
		this.hasYear = hasYear;
		this.hasMonth = hasMonth;
		this.hasDay = hasDay;
		this.hasTime = hasTime;
		this.hasZone = hasZone;
		this.ambiguous = ambiguous;
	}

	/**
	 * Get the datetime, as {@link RosemaryDateTimeParser#parseZoned(String)} returns it.
	 *
	 * @return the datetime, with the time zone's region if the string names one
	 */
	public ZonedDateTime toZonedDateTime() {
		return dateTime;
	}

	/**
	 * Get the datetime, as {@link RosemaryDateTimeParser#parse(String)} returns it.
	 *
	 * @return the datetime
	 */
	public OffsetDateTime toOffsetDateTime() {
		return dateTime.toOffsetDateTime();
	}

	/**
	 * Did the string have the year?  If not, the year is this year, according to the parser's clock.
	 *
	 * @return <code>true</code> if the string had the year
	 */
	public boolean hasYear() {
		return hasYear;
	}

	/**
	 * Did the string have the month?  If not, the month is January if the string had only the year, like <code>"2024"</code>,
	 * and this month otherwise.
	 *
	 * @return <code>true</code> if the string had the month
	 */
	public boolean hasMonth() {
		return hasMonth;
	}

	/**
	 * Did the string have the day of the month?  If not, the day is the first if the string had the month or year, like
	 * <code>"March 2024"</code>, and today otherwise, like <code>"7pm"</code>.  A weekday name isn't the day.
	 *
	 * @return <code>true</code> if the string had the day
	 */
	public boolean hasDay() {
		return hasDay;
	}

	/**
	 * Did the string have a time, like <code>7pm</code>, <code>19:02</code> or <code>noon</code>?  If not, the time is midnight.
	 *
	 * @return <code>true</code> if the string had a time
	 */
	public boolean hasTime() {
		return hasTime;
	}

	/**
	 * Did the string have a time zone, like <code>America/Denver</code>, <code>MST</code>, <code>-07:00</code> or <code>Z</code>?
	 * If not, the time zone is the parser's default zone.  A time since the epoch, like <code>1709596800</code>, is in UTC, and has a zone.
	 *
	 * @return <code>true</code> if the string had a time zone
	 */
	public boolean hasZone() {
		return hasZone;
	}

	/**
	 * Would one of the usual date orders read the same parts of the string as a different date?  If so, the parser's date order, or
	 * the one given to {@link RosemaryDateTimeParser#parseDetailed(String, RosemaryDateOrder)}, decided it.  The usual orders are
	 * month first, {@link RosemaryDateOrder#MM_DD_YY}, day first, {@link RosemaryDateOrder#DD_MM_YY}, and year first,
	 * {@link RosemaryDateOrder#YY_MM_DD}.
	 * <p>
	 * <code>01/02/03</code>, <code>3/5</code> and <code>03052024</code> are ambiguous.  <code>31/12/2024</code>, <code>March 5</code>,
	 * <code>2024-12-05</code> and <code>20241205</code> aren't.  An order that finds other parts doesn't count, like year first reading
	 * <code>3/5</code> as March 2003, without a day.
	 *
	 * @return <code>true</code> if one of the usual date orders reads the same parts as a different date
	 */
	public boolean isAmbiguous() {
		return ambiguous;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof RosemaryParsedDateTime other && dateTime.equals(other.dateTime) && hasYear == other.hasYear && hasMonth == other.hasMonth
			&& hasDay == other.hasDay && hasTime == other.hasTime && hasZone == other.hasZone && ambiguous == other.ambiguous;
	}

	@Override
	public int hashCode() {
		return Objects.hash(dateTime, hasYear, hasMonth, hasDay, hasTime, hasZone, ambiguous);
	}

	@Override
	public String toString() {
		return "RosemaryParsedDateTime[dateTime=" + dateTime + ", hasYear=" + hasYear + ", hasMonth=" + hasMonth + ", hasDay=" + hasDay
			+ ", hasTime=" + hasTime + ", hasZone=" + hasZone + ", ambiguous=" + ambiguous + "]";
	}
}
