# Changelog

## 2.0.0 (unreleased)

2.0 has a new, smaller API built on `java.time`.

### Breaking changes
- Configure parsers with `RosemaryDateTimeParser.builder()`: date order, default time zone, a `Clock` for today's date, the locale for patterns, the century of two-digit years, and time zone abbreviations.  `toBuilder()` starts from an existing parser.
- `RosemaryDateTimeException` is unchecked: it extends `java.time.format.DateTimeParseException`, with `getParsedString()`, and `getErrorIndex()` for the position of the problem in the string.
- Removed `parse(String, RosemaryDateOrder, RosemaryDateTimeState)`, `RosemaryDateTimeState`, `RosemaryTimeZoneProvider`, and the `utils` package (`TimeZoneUtils`, `ParsingUtilities`, `LinkedCaseInsensitiveMap`).
- `RosemaryDateOrder` no longer has a public `order` field.
- Patterns are `java.time.format.DateTimeFormatter` patterns only, with strict dates.  The `SimpleDateFormat` fallback is removed, and the pattern locale is English unless set.
- "Today" comes from the parser's clock, in the datetime's time zone, instead of the system time zone.
- A number by itself is read by its length: 4 digits is a year, 6 or 8 a date, 9 to 11 seconds since the epoch, 12 or 13 milliseconds since the epoch, and 14 `YYYYMMDDhhmmss`.  Other lengths are rejected.  1.x read most short numbers, like `12` or `1969`, as milliseconds since the epoch.
- The recognized time zone abbreviations are a built-in list of the 46 abbreviations used by current time zones, so they no longer depend on the JDK version or the system locale.  Some abbreviations map to a different region, e.g. `CET` is now `Europe/Paris`.
- Rosemary is a Java module, `com.lemmingapex.rosemary`.
- Java 17 or newer is required, as before.

### Added
- Nanosecond precision.
- `T` between the date and time, offsets attached to times (`07:02:10-05:00`, `07:02:10Z`), ISO 8601 basic times (`20080305T070210Z`), and comma fractions (`07:02:10,123`).
- Dotted dates (`05.03.2008`, `05.Mar.2008`), compact dates (`20240305`), and compact datetimes (`20240305193005`).
- Times without colons after a date (`Mar 5, 2008 0702`), hours with AM or PM (`7 pm`), `a.m.` and `p.m.`, `noon` and `midnight`.
- Seconds since the epoch.
- An offset wins over a time zone name, e.g. `-0500 (EST)` in email dates, and `GMT -7`.
- `parseZoned` methods, which return a `ZonedDateTime` that keeps the region, like `America/Denver`, when the string names one.
- `findTimeZone(String)`, which finds a time zone by name the way the parser does.
- `RosemaryDateTimeParser.defaultTimeZoneAbbreviations()` and `getTimeZoneAbbreviations()`, which replace `TimeZoneUtils.getTimezoneNameToTimezoneMap()`.

### Fixed
- Dates with a month name joined by dashes, like `05-Mar-2008` or `Mar-05-2008`, were misread.
- `7A.M.` was ignored.  Other tokens with digits that aren't understood, like `W10`, were ignored too, and are rejected now.
- `13:00AM` was accepted as 13:00.
- AM or PM without an hour, like `2008-03-05 PM`, was ignored.  It is rejected now.
- Time zones whose names end in AM or PM, like `Europe/Amsterdam` and `Pacific/Guam`, threw an exception.
- `12pm` threw an exception, and `12am` was noon.
- Invalid dates, like February 31st, rolled over into the next month.  They are rejected now, with or without a pattern.
- A time zone without a time, like `2019-03-05 America/Denver`, was ignored.
- Offsets like `UTC+0530` were silently ignored, and offsets beyond ±18:00 threw an unchecked `java.time.DateTimeException`.
- Words starting with a month abbreviation, like `junk`, were read as months.
- `UTC` or `GMT` in a string was ignored when the default time zone wasn't UTC.
- A date starting with a full year, like `2024-12-05`, was read in the date order, e.g. as May 12th for `DD_MM_YY`.
- Parsing depended on the system locale, e.g. ordinal words like `first` failed in Turkish.
- Two-digit year rules were applied to years written with four digits, like `0050`.
- Very large numbers overflowed.

## 1.0.0 (2024-03-07)

First release.
