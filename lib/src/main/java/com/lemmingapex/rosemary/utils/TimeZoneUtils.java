package com.lemmingapex.rosemary.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class TimeZoneUtils {
	private static LinkedCaseInsensitiveMap<TimeZone> timezoneNameToTimezoneMap = null;

	public static synchronized LinkedCaseInsensitiveMap<TimeZone> getTimezoneNameToTimezoneMap() {
		if (timezoneNameToTimezoneMap == null) {
			final Set<String> orderedTimezoneIds = new LinkedHashSet<>();
			// prioritize these US timezones, such that the following abbreviations get mapped correctly below: PST, PDT, MST, MDT, CST, CDT, EST, EDT
			orderedTimezoneIds.add("America/Los_Angeles");
			orderedTimezoneIds.add("America/Denver");
			orderedTimezoneIds.add("America/Chicago");
			orderedTimezoneIds.add("America/New_York");
			orderedTimezoneIds.addAll(Arrays.stream(TimeZone.getAvailableIDs()).filter(id -> id.length() > 3 || id.equals("GMT") || id.equals("UTC")).toList()); // don't add the old wierd timezone

			// add the most legitimate timezone names
			final Map<String, TimeZone> timeZoneNameToTimeZone = new LinkedCaseInsensitiveMap<>();
			for (final String zoneIdString : orderedTimezoneIds) {
				final TimeZone timeZone = TimeZone.getTimeZone(zoneIdString);
				timeZoneNameToTimeZone.putIfAbsent(zoneIdString, timeZone);
			}

			// add more obscure timezones names if possible
			for (final Map.Entry<String, TimeZone> timeZoneNameAndTimeZone : new LinkedHashMap<>(timeZoneNameToTimeZone).entrySet()) {
				final TimeZone timeZone = timeZoneNameAndTimeZone.getValue();
				for (int timezoneFormat : List.of(TimeZone.SHORT)) {
					final String standardDisplayName = timeZone.getDisplayName(false, timezoneFormat, Locale.getDefault(Locale.Category.DISPLAY));
					timeZoneNameToTimeZone.putIfAbsent(standardDisplayName, timeZone);
					if (timeZone.useDaylightTime()) {
						final String daylightDisplayName = timeZone.getDisplayName(true, timezoneFormat, Locale.getDefault(Locale.Category.DISPLAY));
						timeZoneNameToTimeZone.putIfAbsent(daylightDisplayName, timeZone);
					}
				}
			}

			// TODO: add least legitimate timezone names?

			// sort timezones from the most useful to the least useful
			// add UTC, US*, America*, then the rest of the timezones
			final LinkedCaseInsensitiveMap<TimeZone> sortedTimeZoneNameToTimeZone = new LinkedCaseInsensitiveMap<>();
			sortedTimeZoneNameToTimeZone.putIfAbsent("UTC", timeZoneNameToTimeZone.get("UTC"));

			List<String> sortedKeys = new ArrayList<>(timeZoneNameToTimeZone.keySet());
			Collections.sort(sortedKeys);
			for (final String timeZoneName : sortedKeys.stream().filter(s -> s.startsWith("US")).toList()) {
				sortedTimeZoneNameToTimeZone.putIfAbsent(timeZoneName, timeZoneNameToTimeZone.get(timeZoneName));
			}
			for (final String timeZoneName : sortedKeys.stream().filter(s -> s.startsWith("America")).toList()) {
				sortedTimeZoneNameToTimeZone.putIfAbsent(timeZoneName, timeZoneNameToTimeZone.get(timeZoneName));
			}
			for (final String timeZoneName : sortedKeys) {
				sortedTimeZoneNameToTimeZone.putIfAbsent(timeZoneName, timeZoneNameToTimeZone.get(timeZoneName));
			}

			timezoneNameToTimezoneMap = sortedTimeZoneNameToTimeZone;
		}
		return timezoneNameToTimezoneMap;
	}
}
