package com.lemmingapex.rosemary;

import org.junit.Test;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests methods in {@link RosemaryDateTimeState}.
 */
public class RosemaryDateTimeStateTest {
	@Test
	public void testStateEquality() throws RosemaryDateTimeException {
		final RosemaryDateTimeState rosemaryDateTimeState1 = new RosemaryDateTimeState(RosemaryDateOrder.MM_DD_YY);
		final RosemaryDateTimeState rosemaryDateTimeState2 = new RosemaryDateTimeState(RosemaryDateOrder.MM_DD_YY);
		assertEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);

		Set<RosemaryDateTimeState> rosemaryDateTimeStates = new HashSet<>();
		rosemaryDateTimeStates.add(rosemaryDateTimeState1);
		rosemaryDateTimeStates.add(rosemaryDateTimeState2);
		assertEquals(1, rosemaryDateTimeStates.size());

		rosemaryDateTimeState1.setYear(2030);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState1.setMonth(5);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState1.setDay(13);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState1.setHour(3);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState1.setMinute(37);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState1.setSecond(53);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState1.setMillisecond(313);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState1.setTimeZone(TimeZone.getTimeZone(ZoneId.of("America/New_York")));
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);

		rosemaryDateTimeStates.add(rosemaryDateTimeState2);
		assertEquals(2, rosemaryDateTimeStates.size());

		rosemaryDateTimeState2.setYear(2030);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState2.setMonth(5);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState2.setDay(13);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState2.setHour(3);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState2.setMinute(37);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState2.setSecond(53);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState2.setMillisecond(313);
		assertNotEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
		rosemaryDateTimeState2.setTimeZone(TimeZone.getTimeZone(ZoneId.of("America/New_York")));
		assertEquals(rosemaryDateTimeState1, rosemaryDateTimeState2);
	}

	@Test
	public void testTimePostMeridian() throws RosemaryDateTimeException {
		final RosemaryDateTimeState state = new RosemaryDateTimeState(RosemaryDateOrder.MM_DD_YY);
		assertFalse(state.isTimePostMeridian());

		state.setTimePostMeridian(true);
		assertTrue(state.isTimePostMeridian());
		state.setHour(12);
		assertEquals(Integer.valueOf(12), state.getHour());
		assertTrue(state.isTimePostMeridian());

		state.setTimePostMeridian(false);
		state.setHour(12);
		assertEquals(Integer.valueOf(0), state.getHour());
		assertFalse(state.isTimePostMeridian());

		state.setTimePostMeridian(true);
		state.setHour(7);
		assertEquals(Integer.valueOf(19), state.getHour());

		// the AM/PM indicator only applies to the next hour
		state.setHour(7);
		assertEquals(Integer.valueOf(7), state.getHour());
	}
}
