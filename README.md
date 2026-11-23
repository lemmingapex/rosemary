<p align="center">
    <img src="rosemary.svg" alt="Rosemary logo" width="230"/>
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
For more, [corpus.txt](lib/src/test/resources/com/lemmingapex/rosemary/corpus.txt) has over 140 datetimes as real programs and people write them, from email and HTTP headers to logs, databases and spreadsheets, and how Rosemary reads each one.

## Goals
* Pattern-free (optional patterns supported)
* Great time zone support
* Month and weekday names in many languages
* Fast: about a microsecond per datetime.  `java.time.format.DateTimeFormatter`, given the pattern, takes about 0.4 microseconds.
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

Rosemary is a Java module, `com.lemmingapex.rosemary`.

## Usage

### Defaults
`new RosemaryDateTimeParser()` uses these defaults, which [`RosemaryDateTimeParser.builder()`](#configuration) can change:
* A datetime without a time zone is in UTC.
* A missing time is midnight.  A missing year, month or day comes from today's date: `"6/19"` is June 19th of this year, and `"7pm"` is today.  Today's date is taken in the datetime's time zone.
* A month without a day is the first of the month, and a year alone is January 1st.
* Month and weekday names are read in English.  [Other languages](#languages) can be read too.
* Words that aren't part of a datetime, like `of`, `at` or `the`, are ignored.  Weekday names are ignored too, and aren't checked against the date.
* `parse(null)` returns `null`.

### Ambiguous dates
Dates like `01/02/03` are read as month, day, year by default.  Pass a `RosemaryDateOrder`, or set one with the builder, to read them differently:
```java
rosemary.parse("01/02/03");                              // 2003-01-02T00:00Z
rosemary.parse("01/02/03", RosemaryDateOrder.DD_MM_YY);  // 2003-02-01T00:00Z
rosemary.parse("01/02/03", RosemaryDateOrder.YY_MM_DD);  // 2001-02-03T00:00Z
```
The order is only used when the values can't be told apart.  `31/12/2024` is December 31st in any order, and a date that starts with a full year, like `2024-12-05`, is year, month, day, unless the order starts with the year: `YY_DD_MM` reads it as May 12th.

### What the string had
`parse` always returns a complete datetime, so its result for `"March 5"` doesn't say that the year, time and time zone were filled in.  `parseDetailed` also says which parts the string had, and whether the date order decided the date:
```java
RosemaryParsedDateTime parsed = rosemary.parseDetailed("3/5 7pm");
parsed.toOffsetDateTime(); // 2024-03-05T19:00Z
parsed.hasYear();          // false - this year
parsed.hasMonth();         // true
parsed.hasDay();           // true
parsed.hasTime();          // true
parsed.hasZone();          // false - the default zone
parsed.isAmbiguous();      // true - day first, it is May 3rd
```
A date is ambiguous if month first, day first or year first would read the same parts as a different date, like `3/5` or `01/02/03`.  `31/12/2024`, `March 5` and `2024-12-05` aren't.  `toZonedDateTime()` keeps the time zone's region, like `parseZoned`.

### Languages
Month and weekday names are always read in English.  To read them in another language too, set the parser's locale:
```java
RosemaryDateTimeParser french = RosemaryDateTimeParser.builder().locale(Locale.FRENCH).build();
french.parse("mardi 5 mars 2024 10:00");        // 2024-03-05T10:00Z
french.parse("1er févr. 2024");                  // 2024-02-01T00:00Z
french.parse("Tue, 05 Mar 2024 10:00:00 GMT");   // 2024-03-05T10:00Z - English names still work, e.g. in HTTP and email dates
```
The names come from the JDK's locale data, so any language that writes month names as words works, like German, Spanish, Russian, Polish or Greek, including the forms used in a date, like Russian `5 марта 2024`.  Names are matched without case or accents, so `fevrier` is `février`, and an abbreviation can be the first three or more letters of a name, like `sept`.  Letters that start more than one month's name, like French `jui` for `juin` and `juillet`, are rejected.

Chinese, Japanese and Korean dates and times are read in any locale, like `2024年3月5日`, `2024/3/5 下午1:30`, `13時30分` or `2024년 3월 5일`.  The unit after each number says what it is, so the date order isn't needed.  Japanese era years, like `令和6年`, are rejected.

A full JDK has the names of every language.  A smaller runtime made with `jlink` has only English names unless it includes the `jdk.localedata` module, e.g. with `--add-modules jdk.localedata`.

The locale doesn't change the date order.  For dates like `05.03.2024`, set `RosemaryDateOrder.DD_MM_YY` too.

### Spreadsheets
Rosemary reads the date and time formats that Excel, Google Sheets, LibreOffice Calc and Numbers offer, as they appear in cells and CSV exports, like `3/5/2024 19:02`, `5-Mar-24`, `Tuesday, March 5, 2024`, `7:02:13 PM`, `05.03.2024` or `2024年3月5日`.  [corpus.txt](lib/src/test/resources/com/lemmingapex/rosemary/corpus.txt) lists them.  Three things differ between spreadsheets and Rosemary's defaults:
* Spreadsheets in most countries write the day first.  Set the date order to match, like `RosemaryDateOrder.DD_MM_YY`.
* Excel and LibreOffice put two-digit years in 1930-2029 by default, so `3/5/45` is 1945.  Rosemary's default is 1970-2069.  Use `twoDigitYearStart(1930)` to match.
* Excel's month-and-year format writes March 2024 as `Mar-24`, which Excel itself reads back as March 24th, and so does Rosemary.  `Mar-45` is March 2045.
* A date cell formatted as General, or read without its format, is a serial number of days, like `45356` for March 5th, 2024, or `45356.5` for noon that day.  Rosemary rejects these, since a number like `45356` could be anything.  Convert them before parsing: `LocalDate.of(1899, 12, 30).plusDays(45356)`, or from 1904-01-01 for files from Excel for Mac before 2011.

### Time zones
Rosemary understands:
* region ids, like `America/Denver`, in any case
* offsets, like `-05:00`, `-0500`, `-5`, `GMT-5`, `UTC+05:30` or `(GMT -7)`
* `UTC`, `GMT`, `UT`, `Z` and `Zulu`, which always mean a zero offset
* the abbreviations used by current time zones, like `PST`, `CET` or `JST`.  `RosemaryDateTimeParser.defaultTimeZoneAbbreviations()` lists them.

An abbreviation means its region, so `CST` and `CDT` both mean `America/Chicago`, and the offset depends on the date.  This follows how abbreviations are usually written, e.g. `CST` all year for the time in Chicago:
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
A number by itself, or with only words like `at` or `UTC`, is read by its length:

| Digits | Meaning | Example |
|---|---|---|
| 4 | year | `1991` |
| 6 or 8 | date in the date order, or YYYYMMDD | `030524`, `03052024`, `20240305` |
| 9 to 11 | seconds since 1970-01-01T00:00:00Z | `1709596800` |
| 12 or 13 | milliseconds since 1970-01-01T00:00:00Z | `1709596800000` |
| 14 | YYYYMMDDhhmmss | `20240305193005` |

Other numbers by themselves, like `12`, are too ambiguous and are rejected.  After a date, a number without a sign is a time, like `0702`, or an offset if there is already a time.

Two-digit years, like `'08`, are 1970-2069 by default.  The builder's `twoDigitYearStart` changes the range.

### Patterns
When the format is known, a [`DateTimeFormatter`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html) pattern can be given instead, or a list of patterns to try in order:
```java
rosemary.parse("01-18-21 03:00 America/New_York", "MM-dd-yy HH:mm VV");  // 2021-01-18T03:00-05:00
rosemary.parse("25-01-03", List.of("yyyy-MM HH:mm", "yy-MM-dd"));         // 2025-01-03T00:00Z
```
Dates are checked strictly, so `2021-02-31` doesn't match `yyyy-MM-dd`.  A pattern without a time zone uses the default zone.  Missing fields are filled in as they are without a pattern, so `MM-dd` is this year, and `yyyy-MM` is the first of the month.

### Errors
A string that can't be parsed throws a `RosemaryDateTimeException`, which is an unchecked `java.time.format.DateTimeParseException`, like the exceptions `java.time` throws:
```java
try {
    rosemary.parse("2024-03-05 10:xx");
} catch (RosemaryDateTimeException e) {
    e.getMessage();      // Unable to parse "2024-03-05 10:xx": Bad time 10:XX at index 11
    e.getParsedString(); // 2024-03-05 10:xx
    e.getErrorIndex();   // 11
}
```
`getErrorIndex()` is the position of the part of the string that couldn't be used.  It is 0 when the problem is the string as a whole, like an impossible date (`February 30 2024`), or no date or time at all.

### Configuration
```java
RosemaryDateTimeParser parser = RosemaryDateTimeParser.builder()
    .dateOrder(RosemaryDateOrder.DD_MM_YY)                                      // order of ambiguous dates
    .defaultZone(ZoneId.of("Europe/London"))                                   // zone when the string has none
    .clock(Clock.fixed(Instant.parse("2030-03-01T00:00:00Z"), ZoneOffset.UTC)) // "today" for partial dates
    .timeZoneAbbreviation("SGT", ZoneId.of("Asia/Singapore"))                  // add or change an abbreviation
    .build();
parser.parse("5/3 7:02 pm");      // 2030-03-05T19:02Z
parser.parse("5/7 7:02 pm");      // 2030-07-05T19:02+01:00
parser.parse("5/3 7:02 pm SGT");  // 2030-03-05T19:02+08:00
```
The builder can also set the language of month and weekday names (`locale`, see [Languages](#languages)), the century of two-digit years (`twoDigitYearStart`), and replace all the abbreviations (`timeZoneAbbreviations`).  `parser.toBuilder()` starts a builder with an existing parser's settings.

### Thread safety
Parsers are immutable and thread-safe.  Create one, and share it.

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
Other Java libraries for parsing dates and times, by approach.

Pattern-free parsing of many formats:
* [dateparser](https://github.com/sisyphsu/dateparser) - Parses hundreds of formats with built-in, extensible regex rules.  Last release 1.0.11, September 2022.

Natural language, like `next Tuesday` or `the day after tomorrow`:
* [Natty](https://github.com/natty-parser/natty) - Community-maintained fork of the original, which was abandoned in 2017.  Last release 1.1.4, August 2026.
* [jchronic](https://github.com/samtingleff/jchronic) - Port of Ruby's Chronic.  Published only as a snapshot, on Clojars.
* [Hawking](https://github.com/zoho/hawking) - Finds dates in free text, including fiscal years and quarters.  GPL-3.0 licensed.
* [SUTime](https://stanfordnlp.github.io/CoreNLP/sutime.html) - Finds and normalizes dates, times and durations in English text, as part of the Stanford CoreNLP toolkit.

A pattern, or a list of patterns to try:
* `java.time.format.DateTimeFormatter` - The built-in Java formatter.  Rosemary uses it for [patterns](#patterns).
* Apache Commons Lang `DateUtils.parseDate` and `parseDateStrictly` - Try a list of patterns in order.

## Possible Future Work
* Times like `10h30`, and Japanese era years, like `令和6年`
* More natural language, e.g. `the day after tomorrow` or `next Tuesday`

## License
[MIT](LICENSE)
