package com.lemmingapex.rosemary;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.lemmingapex.rosemary.TestParsers.PARSER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Parses many generated strings, to check that the parser only ever returns a datetime or throws {@link RosemaryDateTimeException}.
 * The strings are random, but the seed is fixed, so failures can be reproduced.
 */
class RobustnessTest {

	private static final long SEED = 20261122L;
	private static final int COUNT = 20_000;

	/**
	 * Valid datetimes, which are mutated to make similar strings.
	 */
	private static final List<String> SAMPLES = List.of(
		"March 5, 2008 7:02 pm", "2008-03-05T07:02:00.000+01:00", "03/05/08 21:02", "Wed, 05 Mar 2008 19:02:00 -0700",
		"22nd of June 2028 at 8am", "2019-03-05 07:02:30 America/Denver", "6/30/2016 10:02:27.654 AM(UTC-4)", "1204675245123",
		"20240305", "05.03.2008", "Mar 5, 2008 0702", "2024-03-05 10:00:00,123 PST", "The Fifth of March 2008", "noon");

	/**
	 * Characters that appear in datetimes, so random strings get far into the parser.
	 */
	private static final String ALPHABET = "0123456789000111222:::---///...,,  +TZAMPapmJanMarDecSTDGMTUTC()'\"\t";

	private static final List<String> WORDS = List.of("March", "Wed", "PST", "America/Denver", "GMT", "UTC", "Z", "am", "PM", "noon",
		"1st", "Fifth", "of", "at", "the", "2008", "03", "5", "-05:00", "+0530", "7:02", "07:02:10.123", "T", "-", "/", ".");

	@Test
	void onlyThrowsRosemaryDateTimeException() {
		final Random random = new Random(SEED);
		int parsed = 0;
		for (String text : generate(random)) {
			for (RosemaryDateOrder dateOrder : RosemaryDateOrder.values()) {
				if (parse(text, dateOrder) != null) {
					parsed++;
				}
			}
		}
		// make sure the generated strings aren't all rejected, so the test reaches deep into the parser
		final int attempts = COUNT * RosemaryDateOrder.values().length;
		if (parsed < attempts / 10 || parsed > attempts - attempts / 10) {
			fail("Only " + parsed + " of " + attempts + " strings parsed");
		}
	}

	/**
	 * Every result can be written as an ISO 8601 string, and parsed back to the same datetime.
	 */
	@Test
	void readsItsOwnOutput() {
		final Random random = new Random(SEED);
		for (String text : generate(random)) {
			final OffsetDateTime dateTime = parse(text, RosemaryDateOrder.MM_DD_YY);
			if (dateTime != null) {
				assertEquals(dateTime, PARSER.parse(dateTime.toString()), () -> text + " -> " + dateTime);
			}
		}
	}

	@Test
	void parsesWithPatternsSafely() {
		final Random random = new Random(SEED);
		final List<String> patterns = List.of("yyyy-MM-dd HH:mm:ss", "MM/dd/yy", "d MMMM uuuu h:mm a", "yyyyMMdd'T'HHmmssXX", "HH:mm VV");
		for (String text : generate(random)) {
			try {
				assertNotNull(PARSER.parse(text, patterns));
			} catch (RosemaryDateTimeException e) {
				// expected for most strings
			} catch (RuntimeException e) {
				throw new AssertionError("Unexpected exception for \"" + text + "\"", e);
			}
		}
	}

	/**
	 * @return the datetime, or <code>null</code> if the string was rejected with a {@link RosemaryDateTimeException}
	 */
	private static OffsetDateTime parse(String text, RosemaryDateOrder dateOrder) {
		try {
			final OffsetDateTime dateTime = PARSER.parse(text, dateOrder);
			assertNotNull(dateTime, text);
			return dateTime;
		} catch (RosemaryDateTimeException e) {
			assertEquals(text, e.getParsedString());
			return null;
		} catch (RuntimeException e) {
			throw new AssertionError("Unexpected exception for \"" + text + "\" in " + dateOrder, e);
		}
	}

	private static List<String> generate(Random random) {
		final List<String> strings = new ArrayList<>(COUNT);
		for (int i = 0; i < COUNT; i++) {
			strings.add(switch (i % 3) {
				case 0 -> mutate(SAMPLES.get(random.nextInt(SAMPLES.size())), random);
				case 1 -> words(random);
				default -> characters(random);
			});
		}
		return strings;
	}

	/**
	 * Insert, delete or replace a few characters of a valid datetime.
	 */
	private static String mutate(String sample, Random random) {
		final StringBuilder text = new StringBuilder(sample);
		final int mutations = 1 + random.nextInt(3);
		for (int i = 0; i < mutations; i++) {
			final int position = random.nextInt(text.length() + 1);
			final char c = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
			switch (random.nextInt(3)) {
				case 0 -> text.insert(position, c);
				case 1 -> {
					if (position < text.length()) {
						text.deleteCharAt(position);
					}
				}
				default -> {
					if (position < text.length()) {
						text.setCharAt(position, c);
					}
				}
			}
		}
		return text.toString();
	}

	/**
	 * Join a few datetime words and numbers.
	 */
	private static String words(Random random) {
		final StringBuilder text = new StringBuilder();
		final int count = 1 + random.nextInt(6);
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				text.append(random.nextBoolean() ? " " : "");
			}
			text.append(random.nextInt(4) == 0 ? String.valueOf(random.nextInt(100_000)) : WORDS.get(random.nextInt(WORDS.size())));
		}
		return text.toString();
	}

	private static String characters(Random random) {
		final StringBuilder text = new StringBuilder();
		final int length = random.nextInt(25);
		for (int i = 0; i < length; i++) {
			text.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return text.toString();
	}
}
