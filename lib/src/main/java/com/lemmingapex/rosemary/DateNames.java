package com.lemmingapex.rosemary;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Month and weekday names: English names, and the names of a parser's locale, from the JDK's locale data.
 * <p>
 * Names are matched without case or accents, so <code>FEVRIER</code> is <code>février</code>, and a trailing period is ignored.
 * <ul>
 * <li>English: a name, or an abbreviation of one with at least three letters, like <code>MAR</code> or <code>SEPT.</code></li>
 * <li>Another language: a name or abbreviation from the locale data, in the forms used alone and in a date, like <code>März</code>,
 * <code>Mär</code> or Russian <code>марта</code>, or the first three or more letters of a full name, if they start only one month's names.
 * Abbreviations in the locale data change between JDK versions, but are almost always the first letters of the name, so they are
 * found either way.</li>
 * </ul>
 * Names that contain digits or spaces, like Japanese <code>3月</code>, can't be found in a datetime string, and are left out.
 * Thread-safe.
 */
final class DateNames {

	/**
	 * English names only.
	 */
	static final DateNames ENGLISH = new DateNames(Locale.ENGLISH);

	private static final List<String> ENGLISH_WEEKDAYS = List.of("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");
	private static final List<String> ENGLISH_MONTHS = List.of("JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST",
		"SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER");
	private static final TextStyle[] STYLES = {TextStyle.FULL, TextStyle.FULL_STANDALONE, TextStyle.SHORT, TextStyle.SHORT_STANDALONE};
	private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
	private static final int MIN_PREFIX_LENGTH = 3;

	/**
	 * The locale's month names and abbreviations, folded, and their month numbers.
	 */
	private final Map<String, Integer> months = new HashMap<>();
	/**
	 * The locale's full month names, folded, with their month numbers, to look up abbreviations by their first letters.
	 */
	private final List<Map.Entry<String, Integer>> fullMonths = new ArrayList<>();
	/**
	 * The locale's weekday names and abbreviations, folded.
	 */
	private final Set<String> weekdays = new HashSet<>();
	/**
	 * The locale's full weekday names, folded.
	 */
	private final List<String> fullWeekdays = new ArrayList<>();
	/**
	 * Is "am" a word in the locale's language, like German "am 5. März", meaning "on March 5th"?
	 */
	private final boolean amIsAWord;

	/**
	 * @param locale the locale whose names are recognized, as well as English names
	 */
	DateNames(Locale locale) {
		amIsAWord = locale.getLanguage().equals(Locale.GERMAN.getLanguage());
		if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
			return;
		}
		final Set<String> ambiguous = new HashSet<>();
		for (Month month : Month.values()) {
			for (TextStyle style : STYLES) {
				final String name = name(month.getDisplayName(style, locale));
				if (name == null) {
					continue;
				}
				final Integer previous = months.putIfAbsent(name, month.getValue());
				if (previous != null && previous != month.getValue()) {
					ambiguous.add(name);
				}
				if (style == TextStyle.FULL || style == TextStyle.FULL_STANDALONE) {
					fullMonths.add(Map.entry(name, month.getValue()));
				}
			}
		}
		months.keySet().removeAll(ambiguous);
		for (DayOfWeek day : DayOfWeek.values()) {
			for (TextStyle style : STYLES) {
				final String name = name(day.getDisplayName(style, locale));
				if (name != null) {
					weekdays.add(name);
					if (style == TextStyle.FULL || style == TextStyle.FULL_STANDALONE) {
						fullWeekdays.add(name);
					}
				}
			}
		}
	}

	/**
	 * Translate a month name or abbreviation to its number.
	 *
	 * @param token upper case token
	 * @return the month number 1-12, or <code>null</code> if the token is not a month name
	 */
	Integer monthNumber(String token) {
		final String name = fold(withoutPeriod(token));
		if (!months.isEmpty()) {
			final Integer month = months.get(name);
			if (month != null) {
				return month;
			}
			final Integer prefixMonth = monthStartingWith(name);
			if (prefixMonth != null) {
				return prefixMonth;
			}
		}
		if (name.length() < MIN_PREFIX_LENGTH) {
			return null;
		}
		for (int i = 0; i < ENGLISH_MONTHS.size(); i++) {
			if (ENGLISH_MONTHS.get(i).startsWith(name)) {
				return i + 1;
			}
		}
		return null;
	}

	/**
	 * Is the token a weekday name or abbreviation?  In English, an abbreviation has at least three letters, like <code>WED</code>
	 * or <code>THURS.</code>
	 *
	 * @param token upper case token
	 * @return <code>true</code> if the token is a weekday name
	 */
	boolean isWeekdayName(String token) {
		final String name = fold(withoutPeriod(token));
		if (weekdays.contains(name)) {
			return true;
		}
		if (name.length() < MIN_PREFIX_LENGTH) {
			return false;
		}
		for (String weekday : fullWeekdays) {
			if (weekday.startsWith(name)) {
				return true;
			}
		}
		for (String weekday : ENGLISH_WEEKDAYS) {
			if (name.startsWith(weekday) || weekday.startsWith(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is the token the start of more than one month's names, like <code>JUI</code> for <code>juin</code> and <code>juillet</code> in French?
	 *
	 * @param token upper case token
	 * @return <code>true</code> if the token could be more than one month
	 */
	boolean isAmbiguousMonth(String token) {
		final String name = fold(withoutPeriod(token));
		if (name.length() < MIN_PREFIX_LENGTH || months.containsKey(name)) {
			return false;
		}
		return fullMonths.stream().filter(month -> month.getKey().startsWith(name)).map(Map.Entry::getValue).distinct().count() > 1;
	}

	/**
	 * @return <code>true</code> if "am" is a word in the locale's language, like German "am 5. März", meaning "on March 5th"
	 */
	boolean amIsAWord() {
		return amIsAWord;
	}

	/**
	 * Find the month whose full names start with the given letters, like <code>TAMMIK</code> for Finnish <code>tammikuu</code>.
	 *
	 * @return the month number, or <code>null</code> if no month's names, or more than one month's names, start with the letters
	 */
	private Integer monthStartingWith(String name) {
		if (name.length() < MIN_PREFIX_LENGTH) {
			return null;
		}
		Integer found = null;
		for (Map.Entry<String, Integer> month : fullMonths) {
			if (month.getKey().startsWith(name)) {
				if (found != null && !found.equals(month.getValue())) {
					return null;
				}
				found = month.getValue();
			}
		}
		return found;
	}

	/**
	 * Prepare a name from the locale data for lookups.
	 *
	 * @return the name, upper case, without accents or a trailing period, or <code>null</code> if it can't be found in a datetime string
	 * because it is too short, or has digits, spaces or characters that separate tokens, or is read as AM or PM
	 */
	private static String name(String displayName) {
		final String name = fold(withoutPeriod(TimeZoneNames.upperCase(displayName)));
		if (name.length() < 2 || name.equals("AM") || name.equals("PM")
			|| name.chars().anyMatch(c -> !Character.isLetter(c) && c != '.' && c != '-')) {
			return null;
		}
		return name;
	}

	/**
	 * Remove a trailing period from an abbreviated name, like <code>"SEPT."</code>.
	 */
	static String withoutPeriod(String token) {
		return token.endsWith(".") ? token.substring(0, token.length() - 1) : token;
	}

	/**
	 * Remove accents, like <code>MÄRZ</code> to <code>MARZ</code>.  ASCII is returned as it is, quickly.
	 */
	static String fold(String upperCase) {
		for (int i = 0; i < upperCase.length(); i++) {
			if (upperCase.charAt(i) > 0x7F) {
				return COMBINING_MARKS.matcher(Normalizer.normalize(upperCase, Normalizer.Form.NFD)).replaceAll("");
			}
		}
		return upperCase;
	}
}
