package com.lemmingapex.rosemary.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TimeZoneUtilsTest {
	@Test
	public void testDefaultTimeZoneNames() {
		// the exact set of names depends on the JDK's time zone and locale data, so check what should hold on every JDK
		final LinkedCaseInsensitiveMap<TimeZone> map = TimeZoneUtils.getTimezoneNameToTimezoneMap();

		// every region and long time zone id is present, and maps to itself
		Arrays.stream(TimeZone.getAvailableIDs()).filter(id -> id.length() > 3).forEach(id -> assertEquals(id, map.get(id).getID()));
		assertEquals("UTC", map.get("UTC").getID());
		assertEquals("GMT", map.get("GMT").getID());

		// common US abbreviations map to the main US time zones
		final Map<String, String> abbreviations = Map.of(
			"PST", "America/Los_Angeles", "PDT", "America/Los_Angeles",
			"MST", "America/Denver", "MDT", "America/Denver",
			"CST", "America/Chicago", "CDT", "America/Chicago",
			"EST", "America/New_York", "EDT", "America/New_York");
		abbreviations.forEach((abbreviation, id) -> assertEquals(abbreviation, id, map.get(abbreviation).getID()));
		assertNotNull(map.get("CET"));
	}

	@Test
	public void testReturnedMapIsACopy() {
		TimeZoneUtils.getTimezoneNameToTimezoneMap().put("PST", TimeZone.getTimeZone("Asia/Tokyo"));
		TimeZoneUtils.getTimezoneNameToTimezoneMap().remove("EST");
		assertEquals("America/Los_Angeles", TimeZoneUtils.getTimezoneNameToTimezoneMap().get("PST").getID());
		assertNotNull(TimeZoneUtils.getTimezoneNameToTimezoneMap().get("EST"));
	}

	@Test
	public void testNamesDoNotDependOnDefaultLocale() {
		final Locale originalLocale = Locale.getDefault();
		try {
			for (Locale locale : List.of(Locale.GERMANY, Locale.forLanguageTag("tr-TR"), Locale.JAPAN)) {
				Locale.setDefault(locale);
				final LinkedCaseInsensitiveMap<TimeZone> map = TimeZoneUtils.buildTimezoneNameToTimezoneMap();
				assertEquals(locale.toString(), TimeZoneUtils.getTimezoneNameToTimezoneMap().keySet(), map.keySet());
				for (String name : List.of("CET", "PST", "EST", "Pacific/Guam", "PACIFIC/GUAM", "america/indiana/indianapolis")) {
					assertNotNull(locale + " " + name, map.get(name));
				}
			}
		} finally {
			Locale.setDefault(originalLocale);
		}
	}
}
