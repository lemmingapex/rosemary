package com.lemmingapex.rosemary;

import java.util.TimeZone;

import com.lemmingapex.rosemary.utils.LinkedCaseInsensitiveMap;

@FunctionalInterface
public interface RosemaryTimeZoneProvider {
	LinkedCaseInsensitiveMap<TimeZone> timezoneNameToTimezone();
}
