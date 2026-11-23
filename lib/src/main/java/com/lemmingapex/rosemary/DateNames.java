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
 * A name matches if it is one of the language's names or abbreviations, or the first three or more letters of a full name, if they
 * start only one month's names, like <code>SEPT</code>, or Finnish <code>TAMMIK</code> for <code>tammikuu</code>.
 * <ul>
 * <li>English: the full names, which are the same on every JDK, and their abbreviations, which are their first letters.
 * A plural weekday, like <code>MONDAYS</code>, is a weekday too.</li>
 * <li>Another language: the names and abbreviations in the locale data, in the forms used alone and in a date, like <code>März</code>,
 * <code>Mär</code> or Russian <code>марта</code>.  Abbreviations in the locale data change between JDK versions, but are almost always the
 * first letters of the name, so they are found either way.</li>
 * </ul>
 * The locale's names are looked up first.  Names that contain digits or spaces, like Japanese <code>3月</code>, can't be found in a
 * datetime string, and are left out.  Thread-safe.
 */
final class DateNames {

	private static final TextStyle[] STYLES = {TextStyle.FULL, TextStyle.FULL_STANDALONE, TextStyle.SHORT, TextStyle.SHORT_STANDALONE};
	private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
	private static final int MIN_PREFIX_LENGTH = 3;

	private static final Names ENGLISH_NAMES = englishNames();

	/**
	 * English names only.
	 */
	static final DateNames ENGLISH = new DateNames(Locale.ENGLISH);

	/**
	 * The locale's names, which are empty for English.
	 */
	private final Names localeNames;
	/**
	 * Is "am" a word in the locale's language, like German "am 5. März", meaning "on March 5th"?
	 */
	private final boolean amIsAWord;

	/**
	 * @param locale the locale whose names are recognized, as well as English names
	 */
	DateNames(Locale locale) {
		amIsAWord = locale.getLanguage().equals(Locale.GERMAN.getLanguage());
		localeNames = locale.getLanguage().equals(Locale.ENGLISH.getLanguage()) ? new Names(false) : localeNames(locale);
	}

	private static Names englishNames() {
		final Names names = new Names(true);
		final List<String> weekdays = List.of("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");
		final List<String> months = List.of("JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER",
			"NOVEMBER", "DECEMBER");
		weekdays.forEach(name -> names.addWeekday(name, true));
		for (int i = 0; i < months.size(); i++) {
			names.addMonth(months.get(i), i + 1, true);
		}
		return names.withoutAmbiguousMonths();
	}

	private static Names localeNames(Locale locale) {
		final Names names = new Names(false);
		for (TextStyle style : STYLES) {
			final boolean full = style == TextStyle.FULL || style == TextStyle.FULL_STANDALONE;
			for (Month month : Month.values()) {
				final String name = name(month.getDisplayName(style, locale));
				if (name != null) {
					names.addMonth(name, month.getValue(), full);
				}
			}
			for (DayOfWeek day : DayOfWeek.values()) {
				final String name = name(day.getDisplayName(style, locale));
				if (name != null) {
					names.addWeekday(name, full);
				}
			}
		}
		return names.withoutAmbiguousMonths();
	}

	/**
	 * Translate a month name or abbreviation to its number.
	 *
	 * @param token upper case token
	 * @return the month number 1-12, or <code>null</code> if the token is not a month name
	 */
	Integer monthNumber(String token) {
		final String name = fold(withoutPeriod(token));
		final Integer month = localeNames.monthNumber(name);
		return month != null ? month : ENGLISH_NAMES.monthNumber(name);
	}

	/**
	 * Is the token a weekday name or abbreviation, like <code>WED</code> or <code>THURS.</code>?
	 *
	 * @param token upper case token
	 * @return <code>true</code> if the token is a weekday name
	 */
	boolean isWeekdayName(String token) {
		final String name = fold(withoutPeriod(token));
		return localeNames.isWeekdayName(name) || ENGLISH_NAMES.isWeekdayName(name);
	}

	/**
	 * Is the token the start of more than one month's names, like <code>JUI</code> for <code>juin</code> and <code>juillet</code> in French?
	 *
	 * @param token upper case token
	 * @return <code>true</code> if the token could be more than one month
	 */
	boolean isAmbiguousMonth(String token) {
		return localeNames.isAmbiguousMonth(fold(withoutPeriod(token)));
	}

	/**
	 * @return <code>true</code> if "am" is a word in the locale's language, like German "am 5. März", meaning "on March 5th"
	 */
	boolean amIsAWord() {
		return amIsAWord;
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

	/**
	 * One language's month and weekday names, folded, and the rules for matching them.
	 */
	private static final class Names {
		/**
		 * Month names and abbreviations, and their month numbers.
		 */
		private final Map<String, Integer> months = new HashMap<>();
		/**
		 * Full month names, with their month numbers, to look up abbreviations by their first letters.
		 */
		private final List<Map.Entry<String, Integer>> fullMonths = new ArrayList<>();
		/**
		 * Names that are more than one month's name or abbreviation.
		 */
		private final Set<String> ambiguousMonths = new HashSet<>();
		/**
		 * Weekday names and abbreviations.
		 */
		private final Set<String> weekdays = new HashSet<>();
		/**
		 * Full weekday names.
		 */
		private final List<String> fullWeekdays = new ArrayList<>();
		/**
		 * Is a word that starts with a full weekday name a weekday, like English <code>MONDAYS</code>?
		 */
		private final boolean plurals;

		Names(boolean plurals) {
			this.plurals = plurals;
		}

		void addMonth(String name, int month, boolean full) {
			final Integer previous = months.putIfAbsent(name, month);
			if (previous != null && previous != month) {
				ambiguousMonths.add(name);
			}
			if (full) {
				fullMonths.add(Map.entry(name, month));
			}
		}

		void addWeekday(String name, boolean full) {
			weekdays.add(name);
			if (full) {
				fullWeekdays.add(name);
			}
		}

		/**
		 * Drop the names that are more than one month's name, which can't be used.
		 */
		Names withoutAmbiguousMonths() {
			months.keySet().removeAll(ambiguousMonths);
			return this;
		}

		/**
		 * @param name folded name
		 * @return the month number of a name, an abbreviation, or the first letters of only one month's full names, or <code>null</code>
		 */
		Integer monthNumber(String name) {
			final Integer month = months.get(name);
			if (month != null || name.length() < MIN_PREFIX_LENGTH) {
				return month;
			}
			Integer found = null;
			for (Map.Entry<String, Integer> fullMonth : fullMonths) {
				if (fullMonth.getKey().startsWith(name)) {
					if (found != null && !found.equals(fullMonth.getValue())) {
						return null;
					}
					found = fullMonth.getValue();
				}
			}
			return found;
		}

		/**
		 * @param name folded name
		 * @return <code>true</code> if the name is a weekday name, an abbreviation, the first letters of a full name, or, in a language
		 * with plurals, starts with a full name
		 */
		boolean isWeekdayName(String name) {
			if (weekdays.contains(name)) {
				return true;
			}
			if (name.length() < MIN_PREFIX_LENGTH) {
				return false;
			}
			for (String weekday : fullWeekdays) {
				if (weekday.startsWith(name) || (plurals && name.startsWith(weekday))) {
					return true;
				}
			}
			return false;
		}

		/**
		 * @param name folded name
		 * @return <code>true</code> if the name isn't a month name, but is the first letters of more than one month's full names
		 */
		boolean isAmbiguousMonth(String name) {
			if (name.length() < MIN_PREFIX_LENGTH || months.containsKey(name)) {
				return false;
			}
			Integer found = null;
			for (Map.Entry<String, Integer> month : fullMonths) {
				if (month.getKey().startsWith(name)) {
					if (found != null && !found.equals(month.getValue())) {
						return true;
					}
					found = month.getValue();
				}
			}
			return false;
		}
	}
}
