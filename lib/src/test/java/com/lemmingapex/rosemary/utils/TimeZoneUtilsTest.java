package com.lemmingapex.rosemary.utils;

import org.junit.Test;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TimeZoneUtilsTest {
	@Test
	public void testTimeZoneSize() {
		assertEquals(785, TimeZoneUtils.getTimezoneNameToTimezoneMap().size());
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
