package com.lemmingapex.rosemary;

/**
 * Specifies the order of the date components in a date/time string.
 */
public enum RosemaryDateOrder {
	DD_MM_YY(RosemaryDateTimeState.YEAR_AFTER_MONTH | RosemaryDateTimeState.YEAR_AFTER_DAY | RosemaryDateTimeState.MONTH_AFTER_DAY),
	MM_DD_YY(RosemaryDateTimeState.YEAR_AFTER_MONTH | RosemaryDateTimeState.YEAR_AFTER_DAY | RosemaryDateTimeState.MONTH_BEFORE_DAY),
	MM_YY_DD(RosemaryDateTimeState.YEAR_AFTER_MONTH | RosemaryDateTimeState.YEAR_BEFORE_DAY | RosemaryDateTimeState.MONTH_BEFORE_DAY),
	DD_YY_MM(RosemaryDateTimeState.YEAR_BEFORE_MONTH | RosemaryDateTimeState.YEAR_AFTER_DAY | RosemaryDateTimeState.MONTH_AFTER_DAY),
	YY_DD_MM(RosemaryDateTimeState.YEAR_BEFORE_MONTH | RosemaryDateTimeState.YEAR_BEFORE_DAY | RosemaryDateTimeState.MONTH_AFTER_DAY),
	YY_MM_DD(RosemaryDateTimeState.YEAR_BEFORE_MONTH | RosemaryDateTimeState.YEAR_BEFORE_DAY | RosemaryDateTimeState.MONTH_BEFORE_DAY);

	public final int order;

	RosemaryDateOrder(int order) {
		this.order = order;
	}
}