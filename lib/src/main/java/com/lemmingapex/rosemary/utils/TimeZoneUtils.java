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

/**
 * Contains utility methods for dealing with timezones.
 */
public class TimeZoneUtils {
	private static LinkedCaseInsensitiveMap<TimeZone> timezoneNameToTimezoneMap = null;

	/**
	 * Locale used for timezone abbreviations like PST and CET, so that the recognized names do not depend on the JVM's default locale.
	 */
	private static final Locale TIMEZONE_NAME_LOCALE = Locale.US;

	/**
	 * Returns a map of timezone names to {@link TimeZone} objects.  This is the default implementation that Rosemary will use and recognize.
	 * Each call returns a new copy, so changes to the returned map do not affect other callers.
	 *
	 * @return a case-insensitive map of timezone names to {@link TimeZone} objects
	 */
	public static synchronized LinkedCaseInsensitiveMap<TimeZone> getTimezoneNameToTimezoneMap() {
		if (timezoneNameToTimezoneMap == null) {
			timezoneNameToTimezoneMap = buildTimezoneNameToTimezoneMap();
		}
		return timezoneNameToTimezoneMap.clone();
	}

	/**
	 * Builds the default map of timezone names to {@link TimeZone} objects.
	 *
	 * @return a new case-insensitive map of timezone names to {@link TimeZone} objects
	 */
	static LinkedCaseInsensitiveMap<TimeZone> buildTimezoneNameToTimezoneMap() {
		final Set<String> orderedTimezoneIds = new LinkedHashSet<>();
		// prioritize these US timezones, such that the following abbreviations get mapped correctly below: PST, PDT, MST, MDT, CST, CDT, EST, EDT
		orderedTimezoneIds.add("America/Los_Angeles");
		orderedTimezoneIds.add("America/Denver");
		orderedTimezoneIds.add("America/Chicago");
		orderedTimezoneIds.add("America/New_York");
		orderedTimezoneIds.addAll(Arrays.stream(TimeZone.getAvailableIDs()).filter(id -> id.length() > 3 || id.equals("GMT") || id.equals("UTC")).toList()); // don't add the old weird timezone

		// add the most legitimate timezone names
		final Map<String, TimeZone> timeZoneNameToTimeZone = new LinkedCaseInsensitiveMap<>(Locale.ROOT);
		for (final String zoneIdString : orderedTimezoneIds) {
			final TimeZone timeZone = TimeZone.getTimeZone(zoneIdString);
			timeZoneNameToTimeZone.putIfAbsent(zoneIdString, timeZone);
		}

		// add more obscure timezones names if possible
		for (final Map.Entry<String, TimeZone> timeZoneNameAndTimeZone : new LinkedHashMap<>(timeZoneNameToTimeZone).entrySet()) {
			final TimeZone timeZone = timeZoneNameAndTimeZone.getValue();
			for (int timezoneFormat : List.of(TimeZone.SHORT)) {
				final String standardDisplayName = timeZone.getDisplayName(false, timezoneFormat, TIMEZONE_NAME_LOCALE);
				timeZoneNameToTimeZone.putIfAbsent(standardDisplayName, timeZone);
				if (timeZone.useDaylightTime()) {
					final String daylightDisplayName = timeZone.getDisplayName(true, timezoneFormat, TIMEZONE_NAME_LOCALE);
					timeZoneNameToTimeZone.putIfAbsent(daylightDisplayName, timeZone);
				}
			}
		}

		// TODO: add least legitimate timezone names?

		// sort timezones from the most useful to the least useful
		// add UTC, US*, America*, then the rest of the timezones
		final LinkedCaseInsensitiveMap<TimeZone> sortedTimeZoneNameToTimeZone = new LinkedCaseInsensitiveMap<>(Locale.ROOT);
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

		return sortedTimeZoneNameToTimeZone;
	}
}
