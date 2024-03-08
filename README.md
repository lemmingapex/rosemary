<p align="center">
    <a href="https://getbootstrap.com/">
        <img src="rosemary.svg" alt="rosemary logo" width="230"/>
    </a>
</p>
<h3 align="center">Rosemary</h3>
<p align="center">
a robust and pattern-free datetime parser for java
</p>



# Rosemary
[![Build Status](https://github.com/lemmingapex/rosemary/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/lemmingapex/rosemary/actions) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.lemmingapex/rosemary/badge.svg)](https://central.sonatype.com/artifact/com.lemmingapex/rosemary)  
Rosemary is a robust and pattern-free datetime parser for java.  Dates and times can messy.  Rosemary simplifies the process of parsing dates and times from diverse formats, offering flexibility, precision, and easy configuration options for developers.
```java
RosemaryDateTimeParser rosemary = new RosemaryDateTimeParser();
OffsetDateTime parsedDateTime = rosemary.parse("March 05 1988");        // 1988-03-05T00:00:00.000+00:00
rosemary.parse("3/5/24");                                               // 2024-03-05T00:00:00.000+00:00
rosemary.parse("Wednesday March Fifth 2008 7:02:13 pm");                // 2008-03-05T19:02:13.000+00:00
rosemary.parse("22nd of June 2028 at 8am");                             // 2028-06-22T08:00:00.000+00:00
rosemary.parse("2019-03-05 07:02:30 America/Denver");                   // 2019-03-05T07:02:30.000-07:00 - awesome timezone support
rosemary.parse("03-05 2008 7:02:10 -05:00");                            // 2008-03-05T07:02:10.000-05:00
rosemary.parse("2018-04-03 09:59:00 CST");                              // 2018-04-03T09:59:00.000-05:00
rosemary.parse("2010-03-05 07:02:04.488 AKDT");                         // 2010-03-05T07:02:04.488-09:00 - millisecond precision
rosemary.parse("6/30/2016 10:02:27.654 AM(UTC-4)");                     // 2016-06-30T10:02:27.654-04:00
rosemary.parse("01-02-03 04:05", RosemaryDateOrder.DD_MM_YY);           // 2003-02-01T04:05:00.000+00:00 - distinguish between ambiguous dates
rosemary.parse("6/19");                                                 // 20XX-06-19T00:00:00.000+00:00 - uses current year by default
rosemary.parse("1991");                                                 // 1991-01-01T00:00:00.000+00:00
rosemary.parse("7:02 pm", ..., new RosemaryDateTimeState(...));         // 2030-03-01T19:02:00.000+00:00 - can specify the year, month, timezone, etc. e.g. March 2030
rosemary.parse("01-18-21 03:00 America/New_York", "MM-dd-yy HH:mm VV"); // 2021-01-18T03:00:00.000-05:00 - can provide a java.time.format.DateTimeFormatter format
rosemary.parse("1204675245123");                                        // 2008-03-05T00:00:45.123+00:00 - milliseconds since epoch
```

## Goals
* Pattern-free (optional patterns supported)
* Great timezone support
* Fast
* Simple api

## Get Rosemary
Gradle dependency:
```groovy
implementation 'com.lemmingapex:rosemary:1.0.0'
```

Maven dependency:
```xml
<dependency>
    <groupId>com.lemmingapex</groupId>
    <artifactId>rosemary</artifactId>
    <version>1.0.0</version>
</dependency>
```


## License
[MIT](LICENSE)

## Building and Tests
To build:  
```zsh
./gradlew clean && ./gradlew build
```

To test:  
```zsh
./gradlew test --rerun-tasks
```

## Alternatives
* java.time.format.DateTimeFormatter - The built-in java formatter that always requires a pattern
* https://github.com/samtingleff/jchronic - Natural language parsing, fails on many common date formats

## Possible Future Work
* Internationalization and localization (help wanted)  
* Ensure thread safety
* Nanosecond percision support  
* Natural language parsing e.g. `The day after tomorrow`  
