package com.lemmingapex.rosemary;

import java.util.TimeZone;

import com.lemmingapex.rosemary.utils.LinkedCaseInsensitiveMap;

/**
 * Provides a map of timezone names to {@link TimeZone} objects.  This can be used to customize the timezone names that Rosemary will recognize.
 */
@FunctionalInterface
public interface RosemaryTimeZoneProvider {
	LinkedCaseInsensitiveMap<TimeZone> timezoneNameToTimezone();
}
