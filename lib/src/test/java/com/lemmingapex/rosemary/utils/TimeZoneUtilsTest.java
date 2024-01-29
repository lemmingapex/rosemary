package com.lemmingapex.rosemary.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeZoneUtilsTest {
	@Test
	public void testTimeZoneSize() {
		assertEquals(785, TimeZoneUtils.getTimezoneNameToTimezoneMap().size());
	}
}
