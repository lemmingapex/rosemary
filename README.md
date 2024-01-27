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
Rosemary is a robust and pattern-free datetime parser for java 
```java
final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
OffsetDateTime parsedDateTime = rosemaryDateTimeParser.parse("March 05 1988"); 		// 1988-03-05T00:00:00.000+00:00
rosemaryDateTimeParser.parse("3/5/24"); 											// 2024-03-05T00:00:00.000+00:00
rosemaryDateTimeParser.parse("Wednesday March Fifth 2008 7:02:13 pm"); 				// 2008-03-05T19:02:13.000+00:00
rosemaryDateTimeParser.parse("22nd of June 2028 at 8am"); 							// 2028-06-22T08:00:00.000+00:00
rosemaryDateTimeParser.parse("2019-03-05 07:02:30 America/Denver"); 				// 2019-03-05T07:02:30.000-07:00 - awesome timezone support
rosemaryDateTimeParser.parse("03-05 2008 7:02:10 -05:00"); 							// 2008-03-05T07:02:10.000-05:00
rosemaryDateTimeParser.parse("2018-04-03 09:59:00 CST"); 							// 2018-04-03T09:59:00.000-05:00
rosemaryDateTimeParser.parse("2010-03-05 07:02:04.488 AKDT"); 						// 2010-03-05T07:02:04.488-09:00 - millisecond precision
rosemaryDateTimeParser.parse("6/30/2016 10:02:27.654 AM(UTC-4)"); 					// 2016-06-30T10:02:27.654-04:00
rosemaryDateTimeParser.parse("01-02-03 04:05", RosemaryDateOrder.DD_MM_YY); 		// 2003-02-01T04:05:00.000+00:00 - distinguish between ambiguous dates
rosemaryDateTimeParser.parse("6/19"); 												// 20XX-06-19T00:00:00.000+00:00 - uses current year by default
rosemaryDateTimeParser.parse("1991"); 												// 1991-01-01T00:00:00.000+00:00
rosemaryDateTimeParser.parse("7:02 pm", ..., new RosemaryDateTimeState(...)); 		// 2030-03-01T19:02:00.000+00:00 - can specify the year, month, timezone, etc. e.g. March 2030
rosemaryDateTimeParser.parse("1204675245123"); 										// 2008-03-05T00:00:45.123+00:00 - milliseconds since epoch
```

## Goals
* Pattern-free (optional patterns supported)
* Great timezone support
* Fast
* Simple api

## Get Rosemary
TODO

## Run the tests
TODO

## Alternatives
* java.time.format.DateTimeFormatter - The built-in java formatter that always requires a pattern
* https://github.com/samtingleff/jchronic - Natural language parsing, fails on many common date formats

## Possible Future Work
* Internationalization and localization (help wanted)  
* Nanosecond percision support  
* Natural language parsing e.g. `The day after tomorrow`  
