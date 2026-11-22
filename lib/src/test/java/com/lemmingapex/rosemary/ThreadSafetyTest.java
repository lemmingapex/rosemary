package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One parser can be shared by many threads.
 */
class ThreadSafetyTest {

	private static final List<String> INPUTS = List.of(
		"March 5, 2008 7:02 pm", "2008-03-05T07:02:00.000+01:00", "03/05/08 21:02", "Wed, 05 Mar 2008 19:02:00 -0700",
		"22nd of June 2028 at 8am", "2019-03-05 07:02:30 America/Denver", "6/30/2016 10:02:27.654 AM(UTC-4)", "1204675245123",
		"20240305", "7pm", "2018-04-03 09:59:00 CST", "not a date");

	private static final int THREADS = 8;
	private static final int REPEATS = 2_000;

	@Test
	void givesTheSameResultsFromManyThreads() throws Exception {
		final List<String> expected = new ArrayList<>();
		for (String input : INPUTS) {
			expected.add(parse(input));
		}

		final ExecutorService executor = Executors.newFixedThreadPool(THREADS);
		try {
			final CountDownLatch start = new CountDownLatch(1);
			final List<Future<List<String>>> futures = new ArrayList<>();
			for (int thread = 0; thread < THREADS; thread++) {
				final int offset = thread;
				futures.add(executor.submit(() -> {
					start.await();
					final List<String> mismatches = new ArrayList<>();
					for (int i = 0; i < REPEATS; i++) {
						// each thread works through the inputs in a different order
						final int index = (i + offset) % INPUTS.size();
						final String result = parse(INPUTS.get(index));
						if (!result.equals(expected.get(index))) {
							mismatches.add(INPUTS.get(index) + " -> " + result);
						}
					}
					return mismatches;
				}));
			}
			start.countDown();
			for (Future<List<String>> future : futures) {
				assertEquals(List.of(), future.get());
			}
		} finally {
			executor.shutdownNow();
		}
	}

	private static String parse(String input) {
		try {
			final OffsetDateTime dateTime = PARSER.parse(input);
			return dateTime.toString();
		} catch (RosemaryDateTimeException e) {
			return "error: " + e.getMessage();
		}
	}
}
