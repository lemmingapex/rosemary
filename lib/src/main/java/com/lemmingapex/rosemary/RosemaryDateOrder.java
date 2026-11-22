package com.lemmingapex.rosemary;

/**
 * The order of the day, month and year in an ambiguous numeric date, like 01/02/03.
 * <p>
 * The order is only used when the values can't be told apart.  For example, 31/12/2024 is always December 31st,
 * and 2024-12-31 always has the year first, whatever the order.
 */
public enum RosemaryDateOrder {
	/**
	 * Day, month, year: 01/02/03 is February 1st, 2003.
	 */
	DD_MM_YY(false, false, false),
	/**
	 * Month, day, year: 01/02/03 is January 2nd, 2003.  This is the default.
	 */
	MM_DD_YY(false, false, true),
	/**
	 * Month, year, day: 01/02/03 is January 3rd, 2002.
	 */
	MM_YY_DD(false, true, true),
	/**
	 * Day, year, month: 01/02/03 is March 1st, 2002.
	 */
	DD_YY_MM(true, false, false),
	/**
	 * Year, day, month: 01/02/03 is March 2nd, 2001.
	 */
	YY_DD_MM(true, true, false),
	/**
	 * Year, month, day: 01/02/03 is February 3rd, 2001.
	 */
	YY_MM_DD(true, true, true);

	private final boolean yearBeforeMonth;
	private final boolean yearBeforeDay;
	private final boolean monthBeforeDay;

	RosemaryDateOrder(boolean yearBeforeMonth, boolean yearBeforeDay, boolean monthBeforeDay) {
		this.yearBeforeMonth = yearBeforeMonth;
		this.yearBeforeDay = yearBeforeDay;
		this.monthBeforeDay = monthBeforeDay;
	}

	boolean isYearBeforeMonth() {
		return yearBeforeMonth;
	}

	boolean isYearBeforeDay() {
		return yearBeforeDay;
	}

	boolean isMonthBeforeDay() {
		return monthBeforeDay;
	}

	/**
	 * Is the year first, as in {@link #YY_MM_DD} and {@link #YY_DD_MM}?
	 */
	boolean isYearFirst() {
		return yearBeforeMonth && yearBeforeDay;
	}

	/**
	 * The fields in order, e.g. <code>"DMY"</code> for {@link #DD_MM_YY}.
	 */
	String fieldOrder() {
		return "" + name().charAt(0) + name().charAt(3) + name().charAt(6);
	}
}
