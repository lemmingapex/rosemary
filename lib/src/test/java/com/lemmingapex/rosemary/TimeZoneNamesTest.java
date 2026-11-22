package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesProvider;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Finding time zones by name.
 */
class TimeZoneNamesTest {

	private static final TimeZoneNames DEFAULT = new TimeZoneNames(TimeZoneNames.DEFAULT_ABBREVIATIONS);

	@Test
	void findsUtcNames() {
		for (String name : TimeZoneNames.UTC_NAMES) {
			assertEquals(ZoneOffset.UTC, DEFAULT.find(name), name);
		}
	}

	@Test
	void findsEveryRegionId() {
		for (String id : ZoneRulesProvider.getAvailableZoneIds()) {
			final String name = TimeZoneNames.upperCase(id);
			if (!TimeZoneNames.UTC_NAMES.contains(name) && !TimeZoneNames.DEFAULT_ABBREVIATIONS.containsKey(name)) {
				assertEquals(ZoneId.of(id), DEFAULT.find(name), id);
			}
		}
	}

	@Test
	void findsAbbreviations() {
		assertEquals(ZoneId.of("America/Los_Angeles"), DEFAULT.find("PST"));
		assertEquals(ZoneId.of("America/Los_Angeles"), DEFAULT.find("PDT"));
		assertEquals(ZoneId.of("Europe/Paris"), DEFAULT.find("CET"));
		assertEquals(ZoneId.of("Asia/Kolkata"), DEFAULT.find("IST"));
	}

	@Test
	void abbreviationsArePreferredOverRegionIds() {
		// EST and MST are also legacy region ids, with fixed offsets
		assertEquals(ZoneId.of("America/New_York"), DEFAULT.find("EST"));
		assertEquals(ZoneId.of("America/Denver"), DEFAULT.find("MST"));
		assertEquals(ZoneId.of("Pacific/Honolulu"), DEFAULT.find("HST"));
	}

	@Test
	void abbreviationsAreCaseInsensitive() {
		final TimeZoneNames names = new TimeZoneNames(Map.of("Sgt", ZoneId.of("Asia/Singapore")));
		assertEquals(ZoneId.of("Asia/Singapore"), names.find("SGT"));
		assertNull(names.find("PST"));
	}

	@Test
	void returnsNullForUnknownNames() {
		assertNull(DEFAULT.find("NOT_A_ZONE"));
		assertNull(DEFAULT.find("MART"));
		assertNull(DEFAULT.find("AMERICA/ATLANTIS"));
	}

	@Test
	void defaultAbbreviationsAreUpperCaseRegions() {
		TimeZoneNames.DEFAULT_ABBREVIATIONS.forEach((name, zone) -> {
			assertEquals(TimeZoneNames.upperCase(name), name);
			assertTrue(ZoneRulesProvider.getAvailableZoneIds().contains(zone.getId()), name + " " + zone);
		});
	}
}
