<p align="center">
    <img src="rosemary.svg" alt="rosemary logo" width="230"/>
</p>
<h3 align="center">Rosemary</h3>
<p align="center">
a robust and pattern-free datetime parser for java
</p>



# Rosemary
[![Build Status](https://github.com/lemmingapex/rosemary/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/lemmingapex/rosemary/actions) [![Maven Central](https://img.shields.io/maven-central/v/com.lemmingapex/rosemary)](https://central.sonatype.com/artifact/com.lemmingapex/rosemary) [![Javadoc](https://javadoc.io/badge2/com.lemmingapex/rosemary/javadoc.svg)](https://javadoc.io/doc/com.lemmingapex/rosemary)

Rosemary is a robust and pattern-free datetime parser for Java.  Dates and times can be messy.  Rosemary finds the date, time and time zone in strings from diverse sources, without being told the format, and returns a `java.time.OffsetDateTime`.
```java
RosemaryDateTimeParser rosemary = new RosemaryDateTimeParser();
OffsetDateTime dateTime = rosemary.parse("March 05 1988");               // 1988-03-05T00:00Z
rosemary.parse("3/5/24");                                                // 2024-03-05T00:00Z
rosemary.parse("Wednesday March Fifth 2008 7:02:13 pm");                 // 2008-03-05T19:02:13Z
rosemary.parse("22nd of June 2028 at 8am");                              // 2028-06-22T08:00Z
rosemary.parse("2019-03-05 07:02:30 America/Denver");                    // 2019-03-05T07:02:30-07:00 - time zones
rosemary.parse("03-05 2008 7:02:10 -05:00");                             // 2008-03-05T07:02:10-05:00
rosemary.parse("2018-01-03 09:59:00 CST");                               // 2018-01-03T09:59-06:00 - time zone abbreviations
rosemary.parse("6/30/2016 10:02:27.654 AM(UTC-4)");                      // 2016-06-30T10:02:27.654-04:00
rosemary.parse("Tue, 15 Nov 1994 08:12:31 -0500 (EST)");                 // 1994-11-15T08:12:31-05:00 - email dates
rosemary.parse("2024-03-05T10:00:00.123456789Z");                        // 2024-03-05T10:00:00.123456789Z - nanosecond precision
rosemary.parse("05.03.2008", RosemaryDateOrder.DD_MM_YY);                // 2008-03-05T00:00Z - ambiguous dates
rosemary.parse("6/19");                                                  // June 19th of this year
rosemary.parse("7pm");                                                   // today at 19:00
rosemary.parse("1991");                                                  // 1991-01-01T00:00Z
rosemary.parse("20240305");                                              // 2024-03-05T00:00Z - compact dates
rosemary.parse("1709596800");                                            // 2024-03-05T00:00Z - seconds since the epoch
rosemary.parse("1204675245123");                                         // 2008-03-05T00:00:45.123Z - milliseconds since the epoch
rosemary.parse("01-18-21 03:00 America/New_York", "MM-dd-yy HH:mm VV");  // 2021-01-18T03:00-05:00 - optional patterns
```

## Goals
* Pattern-free (optional patterns supported)
* Great time zone support
* Fast: about half a microsecond per datetime, similar to `java.time.format.DateTimeFormatter` with a known pattern
* Simple API

## Get Rosemary
Rosemary needs Java 17 or newer, and has no dependencies.

Gradle dependency:
```groovy
implementation 'com.lemmingapex:rosemary:2.0.0'
```

Maven dependency:
```xml
<dependency>
    <groupId>com.lemmingapex</groupId>
    <artifactId>rosemary</artifactId>
    <version>2.0.0</version>
</dependency>
```

Rosemary is a Java module, `com.lemmingapex.rosemary`.  Upgrading from 1.x?  See [Upgrading from 1.x](#upgrading-from-1x).

## Usage

### Defaults
`new RosemaryDateTimeParser()` uses these defaults, which [`RosemaryDateTimeParser.builder()`](#configuration) can change:
* A datetime without a time zone is in UTC.
* A missing time is midnight.  A missing year, month or day comes from today's date: `"6/19"` is June 19th of this year, and `"7pm"` is today.  Today's date is taken in the datetime's time zone.
* A month without a day is the first of the month, and a year alone is January 1st.
* Words that aren't part of a datetime, like `of`, `at` or `the`, are ignored.  Weekday names are ignored too.
* `parse(null)` returns `null`.

### Ambiguous dates
Dates like `01/02/03` are read as month, day, year by default.  Pass a `RosemaryDateOrder`, or set one with the builder, to read them differently:
```java
rosemary.parse("01/02/03");                              // 2003-01-02T00:00Z
rosemary.parse("01/02/03", RosemaryDateOrder.DD_MM_YY);  // 2003-02-01T00:00Z
rosemary.parse("01/02/03", RosemaryDateOrder.YY_MM_DD);  // 2001-02-03T00:00Z
```
The order is only used when the values can't be told apart.  `31/12/2024` is December 31st in any order, and a date that starts with a full year, like `2024-12-05`, is year, month, day, unless the order puts the year first.

### Time zones
Rosemary understands:
* region ids, like `America/Denver`, in any case
* offsets, like `-05:00`, `-0500`, `-5`, `GMT-5`, `UTC+05:30` or `(GMT -7)`
* `UTC`, `GMT`, `UT`, `Z` and `Zulu`, which always mean a zero offset
* the abbreviations used by current time zones, like `PST`, `CET` or `JST`.  `RosemaryDateTimeParser.defaultTimeZoneAbbreviations()` lists them.

An abbreviation means its region, so `CST` and `CDT` both mean `America/Chicago`, and the offset depends on the date:
```java
rosemary.parse("2024-01-15 10:00 CST");          // 2024-01-15T10:00-06:00
rosemary.parse("2024-07-15 10:00 CST");          // 2024-07-15T10:00-05:00
```
An abbreviation used in several places means the most populous one, e.g. `CST` is `America/Chicago`, not `Asia/Shanghai`, and `IST` is `Asia/Kolkata`.  Use the builder to change or add abbreviations.

An offset is more precise than a name, so it wins when a datetime has both:
```java
rosemary.parse("2024-07-15 10:00 -0400 (EST)");  // 2024-07-15T10:00-04:00
```

`parse` returns an `OffsetDateTime`, which only keeps the offset.  To keep the region, like `America/Denver`, use `parseZoned`, which returns a `ZonedDateTime`.  Date arithmetic then follows the region's daylight saving rules:
```java
rosemary.parseZoned("2024-03-09 12:00 America/Denver");             // 2024-03-09T12:00-07:00[America/Denver]
rosemary.parseZoned("2024-03-09 12:00 America/Denver").plusDays(1); // 2024-03-10T12:00-06:00[America/Denver]
rosemary.parseZoned("2024-07-15 10:00 CST");                        // 2024-07-15T10:00-05:00[America/Chicago]
rosemary.parseZoned("2024-07-15 10:00 -04:00");                     // 2024-07-15T10:00-04:00
```
Each `parse` method has a `parseZoned` version.

To look up a time zone name the way the parser does, use `findTimeZone`:
```java
rosemary.findTimeZone("pst");            // Optional[America/Los_Angeles]
rosemary.findTimeZone("america/denver"); // Optional[America/Denver]
rosemary.findTimeZone("Zulu");           // Optional[Z]
rosemary.findTimeZone("MART");           // Optional.empty
```
`parser.getTimeZoneAbbreviations()` lists the abbreviations a parser recognizes, and `ZoneId.getAvailableZoneIds()` lists the region ids.

### Numbers
A number by itself is read by its length:

| Digits | Meaning | Example |
|---|---|---|
| 4 | year | `1991` |
| 6 or 8 | date in the date order, or YYYYMMDD | `030524`, `03052024`, `20240305` |
| 9 to 11 | seconds since 1970-01-01T00:00:00Z | `1709596800` |
| 12 or 13 | milliseconds since 1970-01-01T00:00:00Z | `1709596800000` |
| 14 | YYYYMMDDhhmmss | `20240305193005` |

Other numbers by themselves, like `12`, are too ambiguous and are rejected.  After a date, a number without a sign is a time, like `0702`, or an offset if there is already a time.

Two-digit years, like `'08`, are 1970-2069 by default.

### Patterns
When the format is known, a [`DateTimeFormatter`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html) pattern can be given instead, or a list of patterns to try in order:
```java
rosemary.parse("01-18-21 03:00 America/New_York", "MM-dd-yy HH:mm VV");  // 2021-01-18T03:00-05:00
rosemary.parse("25-01-03", List.of("yyyy-MM HH:mm", "yy-MM-dd"));         // 2025-01-03T00:00Z
```
Dates are checked strictly, so `2021-02-31` doesn't match `yyyy-MM-dd`.  A pattern without a time zone uses the default zone.

### Errors
A string that can't be parsed throws a `RosemaryDateTimeException`, which is an unchecked `java.time.DateTimeException`:
```java
try {
    rosemary.parse("February 30 2024");
} catch (RosemaryDateTimeException e) {
    e.getMessage();      // Unable to parse "February 30 2024": Bad day 30 for month 2 of 2024
    e.getParsedString(); // February 30 2024
}
```

### Configuration
```java
RosemaryDateTimeParser parser = RosemaryDateTimeParser.builder()
    .dateOrder(RosemaryDateOrder.DD_MM_YY)                                  // order of ambiguous dates
    .defaultZone(ZoneId.of("Europe/London"))                               // zone when the string has none
    .clock(Clock.fixed(Instant.parse("2030-03-01T00:00:00Z"), ZoneOffset.UTC)) // "today" for partial dates
    .timeZoneAbbreviation("SGT", ZoneId.of("Asia/Singapore"))              // add or change an abbreviation
    .build();
parser.parse("5/3 7:02 pm");      // 2030-03-05T19:02Z
parser.parse("5/7 7:02 pm");      // 2030-07-05T19:02+01:00
parser.parse("5/3 7:02 pm SGT");  // 2030-03-05T19:02+08:00
```
The builder can also set the locale for names in patterns (`locale`, English by default), the century of two-digit years (`twoDigitYearStart`), and replace all the abbreviations (`timeZoneAbbreviations`).  `parser.toBuilder()` starts a builder with an existing parser's settings.

### Thread safety
Parsers are immutable and thread-safe.  Create one, and share it.

## Upgrading from 1.x
2.0 has a new, smaller API built on `java.time`, and fixes many parsing bugs.  See the [changelog](CHANGELOG.md) for everything that changed.

* `RosemaryDateTimeException` is unchecked now.  Existing `catch` blocks still work.
* `parse(text, dateOrder, defaultDateTimeState)` is gone.  Use `RosemaryDateTimeParser.builder()` with `clock(...)` for the date, and `defaultZone(...)` for the time zone.
* `new RosemaryDateTimeParser(timeZoneProvider)` is gone.  Use `builder().timeZoneAbbreviations(...)` or `timeZoneAbbreviation(...)`.
* `RosemaryDateTimeState`, `RosemaryTimeZoneProvider`, and the `utils` package (`TimeZoneUtils`, `ParsingUtilities`, `LinkedCaseInsensitiveMap`) are removed.
* Instead of `TimeZoneUtils.getTimezoneNameToTimezoneMap()`, use `findTimeZone(name)` to look up a name, `getTimeZoneAbbreviations()` or `defaultTimeZoneAbbreviations()` for the abbreviations, and `ZoneId.getAvailableZoneIds()` for the region ids.
* Patterns are `DateTimeFormatter` patterns only, with strict dates.  `SimpleDateFormat` patterns aren't supported, and the pattern locale is English unless set with `locale(...)`.
* Some results change:
  * "Today" comes from the clock in the datetime's time zone, instead of the system time zone.
  * A number by itself follows the table above.  1.x read most short numbers as milliseconds since the epoch.
  * The built-in abbreviations no longer depend on the JDK or the system locale.

## Building and Tests
To build and test:
```zsh
./gradlew build
```
The build fails if test coverage drops below 95% of lines or 90% of branches.  To see the coverage report, run `./gradlew jacocoTestReport`, and open `lib/build/reports/jacoco/test/html/index.html`.

The library is compiled with JDK 17, and the tests run on JDK 17 by default.  To run the tests on another JDK (CI runs 17, 21 and 25):
```zsh
./gradlew test --rerun-tasks -PtestJavaVersion=25
```

To run the benchmarks, which takes about 30 seconds:
```zsh
./gradlew jmh
```

To contribute, see [CONTRIBUTING.md](CONTRIBUTING.md).  To publish a release, see [RELEASING.md](RELEASING.md).

## Alternatives
* `java.time.format.DateTimeFormatter` - The built-in Java formatter, which always requires a pattern
* https://github.com/samtingleff/jchronic - Natural language parsing, which fails on many common date formats

## Possible Future Work
* Internationalization and localization: month and weekday names in other languages (help wanted)
* More natural language, e.g. `the day after tomorrow` or `next Tuesday`

## License
[MIT](LICENSE)
