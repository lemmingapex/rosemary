package com.lemmingapex.rosemary;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * These are the main tests for Rosemary.  These tests parse any and every date that can be found.
 */
public class RosemaryParserTest {

	@Test
	public void testREADME() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		assertEquals(OffsetDateTime.parse("1988-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March 05 1988"));
		assertEquals(OffsetDateTime.parse("2024-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("3/5/24"));
		assertEquals(OffsetDateTime.parse("2008-03-05T19:02:13.000+00:00"), rosemaryDateTimeParser.parse("Wednesday March Fifth 2008 7:02:13 pm"));
		assertEquals(OffsetDateTime.parse("2028-06-22T08:00:00.000+00:00"), rosemaryDateTimeParser.parse("22nd of June 2028 at 8am"));
		assertEquals(OffsetDateTime.parse("2019-03-05T07:02:30.000-07:00"), rosemaryDateTimeParser.parse("2019-03-05 07:02:30 America/Denver"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.000-05:00"), rosemaryDateTimeParser.parse("03-05 2008 7:02:10 -05:00"));
		assertEquals(OffsetDateTime.parse("2018-04-03T09:59:00.000-05:00"), rosemaryDateTimeParser.parse("2018-04-03 09:59:00 CST"));
		assertEquals(OffsetDateTime.parse("2010-03-05T07:02:04.488-09:00"), rosemaryDateTimeParser.parse("2010-03-05 07:02:04.488 AKDT"));
		assertEquals(OffsetDateTime.parse("2016-06-30T10:02:27.654-04:00"), rosemaryDateTimeParser.parse("6/30/2016 10:02:27.654 AM(UTC-4)"));
		assertEquals(OffsetDateTime.parse("2003-02-01T04:05:00.000+00:00"), rosemaryDateTimeParser.parse("01-02-03 04:05", RosemaryDateOrder.DD_MM_YY));
		final Calendar calendar = Calendar.getInstance();
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
		final String year = simpleDateFormat.format(calendar.getTime());
		assertEquals(OffsetDateTime.parse(year + "-06-19T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("6/19"));
		assertEquals(OffsetDateTime.parse("1991-01-01T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("1991"));
		final RosemaryDateTimeState rosemaryDateTimeState = new RosemaryDateTimeState(RosemaryDateOrder.DD_MM_YY);
		rosemaryDateTimeState.setYear(2030);
		rosemaryDateTimeState.setMonth(3);
		rosemaryDateTimeState.setDay(1);
		assertEquals(OffsetDateTime.parse("2030-03-01T19:02:00.000+00:00"), rosemaryDateTimeParser.parse("7:02 pm", RosemaryDateOrder.DD_MM_YY, rosemaryDateTimeState));
		assertEquals(OffsetDateTime.parse("2021-01-18T03:00:00.000-05:00"), rosemaryDateTimeParser.parse("01-18-21 03:00 America/New_York", "MM-dd-yy HH:mm VV"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:45.123+00:00"), rosemaryDateTimeParser.parse("1204675245123"));
	}

	@Test
	public void testTypicalDateTimes() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05T07:02:00"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05'T'07:02:00"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.001+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:10.001"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.019+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:10.019"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.190+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:10.190"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.190+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:10.19"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.200+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:10.2"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 7:02 am"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 7:02 AM"));
		assertEquals(OffsetDateTime.parse("2008-03-05T19:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 7:02 PM"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.000+00:00"), rosemaryDateTimeParser.parse("2008/03/05 7:02:10"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.000+00:00"), rosemaryDateTimeParser.parse("03/05 2008 7:02:10"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.000+00:00"), rosemaryDateTimeParser.parse("03-05 2008 7:02:10"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("March 5, 2008 7:02 am"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("Mar 5, 2008 7:02 am"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("Mar 5, 2008 07:02"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("Wednesday March 5th 2008 7:02 am"));
		assertEquals(OffsetDateTime.parse("2008-03-05T19:02:13.000+00:00"), rosemaryDateTimeParser.parse("Wednesday March Fifth 2008 7:02:13 pm"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("03/05/2008 07:02"));
		assertEquals(OffsetDateTime.parse("2022-01-09T04:18:00.000+00:00"), rosemaryDateTimeParser.parse("1/9/22 4:18"));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("03/05/08 07:02"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:02:00.000+00:00"), rosemaryDateTimeParser.parse("03/05/08 0:02"));
		assertEquals(OffsetDateTime.parse("2008-03-05T21:02:00.000+00:00"), rosemaryDateTimeParser.parse("03/05/08 21:02"));
		assertEquals(OffsetDateTime.parse("2008-03-05T19:02:00.000+00:00"), rosemaryDateTimeParser.parse("03-05-2008 7:02 pm"));
		assertEquals(OffsetDateTime.parse("2019-07-20T01:58:15.000+00:00"), rosemaryDateTimeParser.parse("7/20/19 1:58:15'"));
		assertEquals(OffsetDateTime.parse("2016-07-01T12:15:51.000+00:00"), rosemaryDateTimeParser.parse("7/1/2016 12:15:51 pm"));
		assertEquals(OffsetDateTime.parse("2016-07-01T00:15:51.000+00:00"), rosemaryDateTimeParser.parse("7/1/2016 12:15:51 am"));
		assertEquals(OffsetDateTime.parse("2028-06-22T08:00:00.000+00:00"), rosemaryDateTimeParser.parse("22nd of June 2028 at 8am"));
		// TODO: assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("Mar 5, 2008 0702"));
	}

	@Test
	public void testAmbiguousDateOrder() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		assertEquals(OffsetDateTime.parse("2003-01-02T04:05:00.000+00:00"), rosemaryDateTimeParser.parse("01-02-03 04:05"));
		assertEquals(OffsetDateTime.parse("2003-01-02T04:05:00.000+00:00"), rosemaryDateTimeParser.parse("01-02-03 04:05", RosemaryDateOrder.MM_DD_YY));
		assertEquals(OffsetDateTime.parse("2002-01-03T04:05:00.000+00:00"), rosemaryDateTimeParser.parse("01-02-03 04:05", RosemaryDateOrder.MM_YY_DD));
		assertEquals(OffsetDateTime.parse("2003-02-01T04:05:00.000+00:00"), rosemaryDateTimeParser.parse("01-02-03 04:05", RosemaryDateOrder.DD_MM_YY));
		assertEquals(OffsetDateTime.parse("2002-03-01T04:05:00.000+00:00"), rosemaryDateTimeParser.parse("01-02-03 04:05", RosemaryDateOrder.DD_YY_MM));
		assertEquals(OffsetDateTime.parse("2001-02-03T04:05:00.000+00:00"), rosemaryDateTimeParser.parse("01-02-03 04:05", RosemaryDateOrder.YY_MM_DD));
		assertEquals(OffsetDateTime.parse("2001-03-02T04:05:00.000+00:00"), rosemaryDateTimeParser.parse("01-02-03 04:05", RosemaryDateOrder.YY_DD_MM));
	}

	@Test
	public void testTimeZones() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05T07:02:00.000+00:00").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05T07:02:00+00:00").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05T07:02:00-00:00").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T06:02:10.000+00:00"), rosemaryDateTimeParser.parse("03-05 2008 7:02:10 +01:00").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T02:02:10.000+00:00"), rosemaryDateTimeParser.parse("03-05 2008 7:02:10 +05:00").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T02:02:10.000+00:00"), rosemaryDateTimeParser.parse("03-05 2008 7:02:10 (+05:00)").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T08:02:10.000+00:00"), rosemaryDateTimeParser.parse("03-05 2008 7:02:10 -01:00").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.000-05:00"), rosemaryDateTimeParser.parse("03-05 2008 7:02:10 -05:00"));
		assertEquals(OffsetDateTime.parse("2008-03-05T12:02:10.000+00:00"), rosemaryDateTimeParser.parse("03-05 2008 7:02:10 -5").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T06:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05T07:02:00+01:00").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T17:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05T07:02:00-10:00").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:10.789+00:00"), rosemaryDateTimeParser.parse("2008-03-05T07:02:10.789+00:00").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 GMT").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 UTC").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T15:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 America/Los_Angeles").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T15:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 america/los_angeles").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T15:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 AMErica/los_ANGELES").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000-07:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 America/Denver"));
		assertEquals(OffsetDateTime.parse("2008-03-05T13:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 America/Chicago").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T12:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 America/New_York").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T05:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 Africa/Cairo").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T14:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 America/Phoenix").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T12:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 America/Indiana/Indianapolis").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T17:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 Pacific/Honolulu").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T16:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 America/Anchorage").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T02:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 Asia/Karachi").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 Atlantic/Reykjavik").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-04T21:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 Australia/Brisbane").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-04T21:17:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 Australia/Eucla").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 Europe/London").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T06:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 Europe/Zurich").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T06:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 Europe/Paris").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T16:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 AKST").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000-09:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 AKDT"));
		assertEquals(OffsetDateTime.parse("2008-03-05T15:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 PST").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T15:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 PDT").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T14:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 MST").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T14:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 MDT").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T13:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 CST").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-04-03T09:59:00.000-05:00"), rosemaryDateTimeParser.parse("2008-04-03 09:59:00 CDT"));
		assertEquals(OffsetDateTime.parse("2008-03-05T12:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 EST").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2008-03-05T12:02:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-05 07:02:00 EDT").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2018-10-23T22:21:19.913+00:00"), rosemaryDateTimeParser.parse("2018-10-23T22:21:19.913Z").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2016-06-30T14:02:27.000+00:00"), rosemaryDateTimeParser.parse("6/30/2016 10:02:27 GMT-04:00").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2016-06-30T14:02:27.000+00:00"), rosemaryDateTimeParser.parse("6/30/2016 10:02:27 UTC-4").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2016-06-30T10:02:27.000-04:00"), rosemaryDateTimeParser.parse("6/30/2016 10:02:27 AM(UTC-4)"));
		assertEquals(OffsetDateTime.parse("2019-12-23T22:31:14.663+00:00"), rosemaryDateTimeParser.parse("2019-12-23T22:31:14.663Z").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2019-07-20T01:58:15.000+00:00"), rosemaryDateTimeParser.parse("'2019/07/19 18:58:15(GMT  -7)").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2024-01-06T00:22:26.000+00:00"), rosemaryDateTimeParser.parse("Fri, 05 Jan 2024 17:22:26 -0700").withOffsetSameInstant(ZoneOffset.UTC));
		assertEquals(OffsetDateTime.parse("2019-12-10T23:24:08.000-06:00"), rosemaryDateTimeParser.parse("2019/12/10 23:24:08 (GMT  -6)\t"));
		assertEquals(OffsetDateTime.parse("2020-08-11T16:09:59.000+00:00"), rosemaryDateTimeParser.parse("2020/08/11 16:09:59(GMT  +0)"));
		assertEquals(OffsetDateTime.parse("2020-08-05T06:30:24.000-06:00"), rosemaryDateTimeParser.parse("2020/08/05 06:30:24(GMT  -6)"));
		assertEquals(OffsetDateTime.parse("2022-01-13T11:52:08.715-05:00"), rosemaryDateTimeParser.parse("2022-01-13 11:52:08.715 EST\t"));
		assertEquals(OffsetDateTime.parse("2022-06-16T17:55:09.866-04:00"), rosemaryDateTimeParser.parse("\"2022-06-16 17:55:09.866 EDT\""));
		assertEquals(OffsetDateTime.parse("2022-05-11T00:11:31.501-05:00"), rosemaryDateTimeParser.parse("\n2022-05-11 00:11:31.501 CDT\n"));
		assertEquals(OffsetDateTime.parse("2021-09-06T07:49:56.000-04:00"), rosemaryDateTimeParser.parse("2021/09/06 07:49:56(GMT  -4)"));
		assertEquals(OffsetDateTime.parse("2019-04-19T23:55:47.176+00:00"), rosemaryDateTimeParser.parse("2019-04-19T23:55:47.176Z"));
		assertEquals(OffsetDateTime.parse("2019-11-27T08:49:10.000-08:00"), rosemaryDateTimeParser.parse("2019/11/27 08:49:10 (-08:00)"));
		assertEquals(OffsetDateTime.parse("2021-07-30T00:10:03.000+00:00"), rosemaryDateTimeParser.parse("Fri Jul 30 00:10:03 UTC 2021"));
		assertEquals(OffsetDateTime.parse("2022-06-21T15:45:00.000-06:00"), rosemaryDateTimeParser.parse("June 21 2022 3:45 pm America/Mazatlan"));
		assertEquals(OffsetDateTime.parse("2022-06-21T15:45:00.000-04:00"), rosemaryDateTimeParser.parse("June 21 2022 3:45 pm America/Detroit"));
		// TODO: assertEquals(OffsetDateTime.parse("2008-03-05T08:02:10.000+00:00"), rosemaryDateTimeParser.parse("03-05-2008 7:02:10-01:00").withOffsetSameInstant(ZoneOffset.UTC)); // difficult to detect
	}

	@Test
	public void testTimesOnly() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		final Calendar calendar = Calendar.getInstance();
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		final String yearMonthDay = simpleDateFormat.format(calendar.getTime());
		assertEquals(OffsetDateTime.parse(yearMonthDay + "T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("07:02"));
		assertEquals(OffsetDateTime.parse(yearMonthDay + "T19:02:00.000+00:00"), rosemaryDateTimeParser.parse("7:02 pm"));
		assertEquals(OffsetDateTime.parse(yearMonthDay + "T19:00:00.000+00:00"), rosemaryDateTimeParser.parse("7pm"));
		assertEquals(OffsetDateTime.parse(yearMonthDay + "T11:32:09.179+00:00"), rosemaryDateTimeParser.parse("11:32:09.179"));
		assertEquals(OffsetDateTime.parse(yearMonthDay + "T19:03:43.000+00:00"), rosemaryDateTimeParser.parse("19:03:43"));
		assertEquals(OffsetDateTime.parse(yearMonthDay + "T17:22:21.452+00:00"), rosemaryDateTimeParser.parse("17:22:21.452\n"));
		RosemaryDateTimeState state = new RosemaryDateTimeState(RosemaryDateOrder.MM_DD_YY);
		state.setYear(1970);
		state.setMonth(1);
		state.setDay(1);
		assertEquals(OffsetDateTime.parse("1970-01-01T11:53:54.004+00:00"), rosemaryDateTimeParser.parse("11:53:54.004", RosemaryDateOrder.MM_DD_YY, state));
	}

	@Test
	public void testDatesOnly() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		final Calendar calendar = Calendar.getInstance();
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
		final String year = simpleDateFormat.format(calendar.getTime());
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("03-05-2008"));
		assertEquals(OffsetDateTime.parse("2021-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("3/5/21"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("03-05-08"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("003-005-02008"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("5 March 2008"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March 5 2008"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March 5 08"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March 5 '08"));
		assertEquals(OffsetDateTime.parse(year + "-03-01T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March"));
		assertEquals(OffsetDateTime.parse(year + "-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March 5"));
		assertEquals(OffsetDateTime.parse(year + "-06-19T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("6/19"));
		assertEquals(OffsetDateTime.parse(year + "-07-06T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("6/7", RosemaryDateOrder.DD_MM_YY));
		assertEquals(OffsetDateTime.parse("1988-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March 05 1988"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March 0005 002008"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March 5th 2008"));
		assertEquals(OffsetDateTime.parse("2028-06-22T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("22nd June 2028"));
		assertEquals(OffsetDateTime.parse("2028-06-22T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("22nd of June 2028"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March Fifth 2008"));
		assertEquals(OffsetDateTime.parse("1988-03-31T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March Thirty-first 1988"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("The Fifth of March 2008"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("Wednesday March 5th 2008"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("Wed. March 5th 2008"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March, 5 2008"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March,, 5   2008"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("   March, , 5   2008    "));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March - 5 - 2008    "));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("March -- 5 - 2008    "));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("   March,  what?, 5   2008    "));
		assertEquals(OffsetDateTime.parse("2008-03-01T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03"));
		assertEquals(OffsetDateTime.parse("2008-03-19T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("2008-03-19"));
		assertEquals(OffsetDateTime.parse("1970-01-01T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("1970"));
		assertEquals(OffsetDateTime.parse("2008-01-01T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("2008"));
		assertEquals(OffsetDateTime.parse("2089-01-01T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("2089"));
		assertEquals(OffsetDateTime.parse("2008-03-01T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("Mar 2008"));
		assertEquals(OffsetDateTime.parse("2008-03-01T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("03-2008"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("03052008"));
		// TODO: assertEquals(OffsetDateTime.parse("2008-03-05T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("20080305"));
	}

	@Test
	public void testTimeZoneParsingDaylight() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		assertEquals(OffsetDateTime.parse("2022-06-21T15:45:00.000-06:00"), rosemaryDateTimeParser.parse("June 21 2022 3:45 pm MDT"));
		assertEquals(OffsetDateTime.parse("2022-01-21T15:45:00.000-07:00"), rosemaryDateTimeParser.parse("Jan 21 2022 3:45 pm MDT"));
		assertEquals(OffsetDateTime.parse("2022-06-21T15:45:00.000-06:00"), rosemaryDateTimeParser.parse("June 21 2022 3:45 pm MST"));
		assertEquals(OffsetDateTime.parse("2022-01-21T15:45:00.000-07:00"), rosemaryDateTimeParser.parse("Jan 21 2022 3:45 pm MST"));
	}

	@Test
	public void testMilliseconds() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		assertEquals(OffsetDateTime.parse("1970-01-01T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("0"));
		assertEquals(OffsetDateTime.parse("1970-01-01T00:00:01.969+00:00"), rosemaryDateTimeParser.parse("1969"));
		assertEquals(OffsetDateTime.parse("1970-01-01T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("1970"));
		assertEquals(OffsetDateTime.parse("9999-12-31T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("99993112", RosemaryDateOrder.YY_DD_MM));
		assertEquals(OffsetDateTime.parse("1970-01-02T03:46:33.113+00:00"), rosemaryDateTimeParser.parse("99993113"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("1204675200000"));
		assertEquals(OffsetDateTime.parse("2008-03-05T00:00:45.123+00:00"), rosemaryDateTimeParser.parse("1204675245123"));
	}

	@Test
	public void testDefaultState() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		final RosemaryDateOrder rosemaryDateOrder = RosemaryDateOrder.MM_DD_YY;
		final RosemaryDateTimeState defaultDateTimeState = new RosemaryDateTimeState(rosemaryDateOrder);
		defaultDateTimeState.setYear(2030);
		assertEquals(OffsetDateTime.parse("2030-01-01T07:02:00.000+00:00"), rosemaryDateTimeParser.parse("07:02", rosemaryDateOrder, defaultDateTimeState));
		defaultDateTimeState.setMonth(3);
		assertEquals(OffsetDateTime.parse("2030-03-01T19:02:00.000+00:00"), rosemaryDateTimeParser.parse("7:02 pm", rosemaryDateOrder, defaultDateTimeState));
		defaultDateTimeState.setDay(19);
		assertEquals(OffsetDateTime.parse("2030-03-19T11:32:09.179+00:00"), rosemaryDateTimeParser.parse("11:32:09.179", rosemaryDateOrder, defaultDateTimeState));
		defaultDateTimeState.setTimeZone(TimeZone.getTimeZone("America/Denver"));
		assertEquals(OffsetDateTime.parse("2030-03-19T14:23:00.111-06:00"), rosemaryDateTimeParser.parse("14:23:00.111", rosemaryDateOrder, defaultDateTimeState));
	}

	@Test
	public void testDateTimesWithFormats() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		assertEquals(OffsetDateTime.parse("2021-01-18T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("2021-01-18", "yyyy-MM-dd"));
		assertEquals(OffsetDateTime.parse("2021-01-18T15:00:00.000+00:00"), rosemaryDateTimeParser.parse("2021-01-18 3:00 PM", "yyyy-MM-dd h:mm a"));
		assertEquals(OffsetDateTime.parse("2021-01-18T15:00:00.000-05:00"), rosemaryDateTimeParser.parse("2021-01-18 3:00 PM America/New_York", "yyyy-MM-dd h:mm a VV"));
		assertEquals(OffsetDateTime.parse("2021-06-18T15:00:00.000-04:00"), rosemaryDateTimeParser.parse("2021-06-18 3:00 PM America/New_York", "yyyy-MM-dd h:mm a VV"));
		assertEquals(OffsetDateTime.parse("2021-06-18T19:00:00.000+00:00"), rosemaryDateTimeParser.parse("2021-06-18T19:00:00+0000", "yyyy-MM-dd'T'HH:mm:ssXX"));
		assertEquals(OffsetDateTime.parse("2021-06-18T19:00:00.000+04:00"), rosemaryDateTimeParser.parse("20210618 19:00:00+0400", "yyyyMMdd HH:mm:ssXX"));
		assertEquals(OffsetDateTime.parse("2021-09-01T10:26:11.000-05:00"), rosemaryDateTimeParser.parse("20210901 102611 America/Panama", "yyyyMMdd HHmmss VV"));
		assertEquals(OffsetDateTime.parse("2025-01-03T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("25-01-03", List.of("yyyy-MM HH:mm", "yy-MM-dd")));
		assertEquals(OffsetDateTime.parse("2023-05-25T00:00:00.000+00:00"), rosemaryDateTimeParser.parse("25-23-05", List.of("MM/dd", "dd-MM-yy h:mm", "dd-yy-MM")));
	}

	@Test
	public void testExceptions() {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		assertThrows(RosemaryDateTimeException.class, () -> {
			rosemaryDateTimeParser.parse("not a date time");
		});
		assertThrows(RosemaryDateTimeException.class, () -> {
			rosemaryDateTimeParser.parse("");
		});
		assertThrows(RosemaryDateTimeException.class, () -> {
			rosemaryDateTimeParser.parse("  ");
		});
		assertThrows(RosemaryDateTimeException.class, () -> {
			rosemaryDateTimeParser.parse(".:/-");
		});
	}

	@Test
	public void testNull() throws RosemaryDateTimeException {
		final RosemaryDateTimeParser rosemaryDateTimeParser = new RosemaryDateTimeParser();
		assertNull(rosemaryDateTimeParser.parse(null));
	}
}
