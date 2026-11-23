package com.lemmingapex.rosemary;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * A baseline for {@link ParserBenchmark}: {@link DateTimeFormatter} parsing an ISO 8601 datetime, when the format is known.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class DateTimeFormatterBenchmark {

	@Benchmark
	public OffsetDateTime parseIso() {
		return OffsetDateTime.parse("2008-03-05T07:02:00.000+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
