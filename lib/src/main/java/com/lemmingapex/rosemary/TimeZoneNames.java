package com.lemmingapex.rosemary;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Finds time zones by name: UTC and its aliases, time zone abbreviations like PST, and region ids like America/Denver.  Names are case-insensitive.
 */
final class TimeZoneNames {

	/**
	 * Names for UTC.  These are always recognized, and always mean a zero offset, never a region like Europe/London.
	 */
	static final Set<String> UTC_NAMES = Set.of("UTC", "GMT", "UT", "Z", "ZULU");

	/**
	 * The time zone abbreviations recognized by default: the abbreviations used by current time zones in the English (US) CLDR locale data.
	 * <p>
	 * An abbreviation maps to a region, not a fixed offset, so CST and CDT both mean America/Chicago, and the offset depends on the date.
	 * Where an abbreviation is used by several regions, the most populous region is used, e.g. CST is America/Chicago, not Asia/Shanghai,
	 * and IST is Asia/Kolkata, not Asia/Jerusalem or Europe/Dublin.
	 */
	static final Map<String, ZoneId> DEFAULT_ABBREVIATIONS = Map.ofEntries(
		entry("ACDT", "Australia/Adelaide"),
		entry("ACST", "Australia/Adelaide"),
		entry("ADT", "America/Halifax"),
		entry("AEDT", "Australia/Sydney"),
		entry("AEST", "Australia/Sydney"),
		entry("AKDT", "America/Anchorage"),
		entry("AKST", "America/Anchorage"),
		entry("AST", "America/Halifax"),
		entry("AWST", "Australia/Perth"),
		entry("BST", "Europe/London"),
		entry("CAT", "Africa/Maputo"),
		entry("CDT", "America/Chicago"),
		entry("CEST", "Europe/Paris"),
		entry("CET", "Europe/Paris"),
		entry("CST", "America/Chicago"),
		entry("EAT", "Africa/Nairobi"),
		entry("EDT", "America/New_York"),
		entry("EEST", "Europe/Athens"),
		entry("EET", "Europe/Athens"),
		entry("EST", "America/New_York"),
		entry("HADT", "America/Adak"),
		entry("HAST", "America/Adak"),
		entry("HKT", "Asia/Hong_Kong"),
		entry("HST", "Pacific/Honolulu"),
		entry("IDT", "Asia/Jerusalem"),
		entry("IST", "Asia/Kolkata"),
		entry("JST", "Asia/Tokyo"),
		entry("KST", "Asia/Seoul"),
		entry("MDT", "America/Denver"),
		entry("MSK", "Europe/Moscow"),
		entry("MST", "America/Denver"),
		entry("NDT", "America/St_Johns"),
		entry("NST", "America/St_Johns"),
		entry("NZDT", "Pacific/Auckland"),
		entry("NZST", "Pacific/Auckland"),
		entry("PDT", "America/Los_Angeles"),
		entry("PKT", "Asia/Karachi"),
		entry("PST", "America/Los_Angeles"),
		entry("SAST", "Africa/Johannesburg"),
		entry("SST", "Pacific/Pago_Pago"),
		entry("WAT", "Africa/Lagos"),
		entry("WEST", "Europe/Lisbon"),
		entry("WET", "Europe/Lisbon"),
		entry("WIB", "Asia/Jakarta"),
		entry("WIT", "Asia/Jayapura"),
		entry("WITA", "Asia/Makassar")
	);

	/**
	 * Region ids, like America/Denver, keyed by their upper case form.  Built on first use.
	 */
	private static final class RegionIds {
		static final Map<String, ZoneId> BY_UPPER_CASE_ID = new HashMap<>();

		static {
			for (String id : ZoneId.getAvailableZoneIds()) {
				BY_UPPER_CASE_ID.put(upperCase(id), ZoneId.of(id));
			}
		}
	}

	/**
	 * Abbreviations keyed by their upper case form.
	 */
	private final Map<String, ZoneId> abbreviations;

	/**
	 * @param abbreviations the time zone abbreviations to recognize
	 */
	TimeZoneNames(Map<String, ZoneId> abbreviations) {
		final Map<String, ZoneId> byUpperCase = new HashMap<>();
		abbreviations.forEach((name, zone) -> byUpperCase.put(upperCase(name), zone));
		this.abbreviations = Map.copyOf(byUpperCase);
	}

	/**
	 * Find a time zone by name.
	 *
	 * @param name an upper case name, like UTC, PST or AMERICA/DENVER
	 * @return the time zone, or <code>null</code> if the name is not a known time zone
	 */
	ZoneId find(String name) {
		if (UTC_NAMES.contains(name)) {
			return ZoneOffset.UTC;
		}
		final ZoneId abbreviation = abbreviations.get(name);
		if (abbreviation != null) {
			return abbreviation;
		}
		return RegionIds.BY_UPPER_CASE_ID.get(name);
	}

	static String upperCase(String name) {
		return name.toUpperCase(Locale.ROOT);
	}

	private static Map.Entry<String, ZoneId> entry(String abbreviation, String regionId) {
		return Map.entry(abbreviation, ZoneId.of(regionId));
	}
}
