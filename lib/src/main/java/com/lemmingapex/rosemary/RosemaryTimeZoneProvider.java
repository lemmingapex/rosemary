package com.lemmingapex.rosemary;

import com.lemmingapex.rosemary.utils.LinkedCaseInsensitiveMap;

import java.util.TimeZone;

/**
 * Provides a map of timezone names to {@link TimeZone} objects.  This can be used to customize the timezone names that Rosemary will recognize.
 */
@FunctionalInterface
public interface RosemaryTimeZoneProvider {
	LinkedCaseInsensitiveMap<TimeZone> timezoneNameToTimezone();
}
