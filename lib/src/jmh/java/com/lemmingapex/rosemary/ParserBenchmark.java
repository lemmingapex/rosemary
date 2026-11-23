package com.lemmingapex.rosemary;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Measures how long Rosemary takes to parse typical datetimes, without a pattern.  Run with <code>./gradlew jmh</code>.
 *
 * @see DateTimeFormatterBenchmark
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ParserBenchmark {

	private final RosemaryDateTimeParser parser = new RosemaryDateTimeParser();

	@Param({"March 5, 2008 7:02 pm", "2008-03-05T07:02:00.000+01:00", "2019-03-05 07:02:30 America/Denver", "Wed, 05 Mar 2008 19:02:00 -0700", "03/05/08 21:02"})
	public String text;

	@Benchmark
	public OffsetDateTime parse() {
		return parser.parse(text);
	}
}
