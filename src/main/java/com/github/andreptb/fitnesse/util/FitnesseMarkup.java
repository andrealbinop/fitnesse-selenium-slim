
package com.github.andreptb.fitnesse.util;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * General utilities to process FitNesse markup syntax so can be used by Selenium Fixture
 */
public class FitnesseMarkup {

	/**
	 * Markup which presents image preview and download link
	 */
	private static final String SCREENSHOT_LINK_MARKUP = "<a href=\"{0}\" target='_blank'><img src=\"{0}\" height=\"200\"></img</a>";
	/**
	 * Constant of FitnesseRoot files dir (relative path)
	 */
	private static final String FITNESSE_ROOT_FILES_DIR = "/files/";
	/**
	 * @see #compare(Object, Object)
	 */
	private static final Pattern FITNESSE_REGEX_MARKUP_PATTERN = Pattern.compile("^=~/(.+)/");

	/**
	 * Constant used to register selenium special keys as system properties
	 */
	private static final String KEYBOARD_SPECIAL_KEY_VARIABLE_MARKUP = "KEY_{0}";

	/**
	 * Constant representing special key format. Needs !--! to properly render html markup
	 */
	private static final String KEYBOARD_SPECIAL_KEY_RENDERING_MARKUP = "!-<span keycode=\"{1}\">$'{'{0}'}'</span>-!";
	/**
	 * Constants representing selector separators [selector]@[atributte]$[value]
	 */
	public static final String SELECTOR_VALUE_SEPARATOR = "$";
	/**
	 * Constants representing selector separators [selector]@[atributte]$[value]
	 */
	public static final String SELECTOR_ATTRIBUTE_SEPARATOR = "@";

	/**
	 * Compares two values emulating FitNesse comparisons:
	 * <p>
	 * http://fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.WritingAcceptanceTests.SliM.ValueComparisons
	 * </p>
	 * For now supports only exact equal and regular expression comparisons
	 *
	 * @param expected value
	 * @param obtained value
	 * @return comparisonResult
	 */
	public boolean compare(Object expected, Object obtained) {
		String cleanedExpected = clean(expected);
		String cleanedObtained = clean(obtained);
		Matcher matcher = FitnesseMarkup.FITNESSE_REGEX_MARKUP_PATTERN.matcher(cleanedExpected);
		if (matcher.matches()) {
			return cleanedObtained.matches(matcher.group(NumberUtils.INTEGER_ONE));
		}
		return StringUtils.equals(cleanedExpected, cleanedObtained);
	}

	/**
	 * Cleans FitNesse markup from symbols such as:
	 * <ul>
	 * <li>Extracts a keyboard special key value from special key markup. See #</li>
	 * <li>Extracts URL only from HTML generated links</li>
	 * <li>Extracts text from HTML wiki page creation suggestion link</li>
	 * <li>Strips undefined variable ocurrences on text</li>
	 * </ul>
	 *
	 * @param symbol to be cleaned
	 * @return cleanedSymbol
	 */
	public String clean(Object symbol) {
		// strips whitespace and accidental "null" string representation of null value
		String strippedSymbol = StringUtils.remove(StringUtils.strip(ObjectUtils.toString(symbol)), "null");
		if (StringUtils.isBlank(strippedSymbol)) {
			return strippedSymbol;
		}
		// transforms keyboard special keys markup
		strippedSymbol = strippedSymbol.replaceAll("<span keycode=\"([^\"]+)\"[^/]+/span>", "$1");
		// removes undefined variable references
		strippedSymbol = strippedSymbol.replaceAll("<span[^>]+>undefined variable:[^<]+</span>", StringUtils.EMPTY);
		// removes create wikipage markup
		strippedSymbol = strippedSymbol.replaceAll("<a[^>]+>\\[\\?\\]</a>", StringUtils.EMPTY);
		// removes undefined variable references
		strippedSymbol = strippedSymbol.replaceAll("<span[^>]+>undefined variable:[^<]+</span>", StringUtils.EMPTY);
		// removes html tags
		return strippedSymbol.replaceAll("</?.[^>]+>", StringUtils.EMPTY);
	}

	/**
	 * Creates img markup to be viewed in test page.
	 * Usually used by fixtures that wants to return a image link for the test result.s
	 *
	 * @param img File containing the image
	 * @return Image link
	 */
	public String imgLink(Object img) {
		String cleanedImg = FilenameUtils.normalize(clean(img), true);
		if (StringUtils.isBlank(cleanedImg)) {
			return null;
		}
		if (StringUtils.containsIgnoreCase(cleanedImg, FitnesseMarkup.FITNESSE_ROOT_FILES_DIR)) {
			cleanedImg = FitnesseMarkup.FITNESSE_ROOT_FILES_DIR + StringUtils.substringAfter(cleanedImg, FitnesseMarkup.FITNESSE_ROOT_FILES_DIR);
		}
		return MessageFormat.format(FitnesseMarkup.SCREENSHOT_LINK_MARKUP, cleanedImg);
	}

	/**
	 * Registers a system property ({@link System#setProperty(String, String)}) allowing user to develop tests referencing special keys such as tab and enter by using variables. For example:
	 * <p>
	 * If <b>keyName="tab"</b> and <b>keyValue="\uE004"</b> then <b>${KEY_TAB}</b> will resolve to <b>"&lt;span keycode=&quot;\uE004&quot;&gt;tab&lt;/span&gt;"</b>
	 * </p>
	 *
	 * @param keyName Special keyboard key name
	 * @param keyValue Special keyboard key value
	 */
	public void registerKeyboardSpecialKey(String keyName, String keyValue) {
		String generatedKeyName = MessageFormat.format(FitnesseMarkup.KEYBOARD_SPECIAL_KEY_VARIABLE_MARKUP, StringUtils.upperCase(keyName));
		System.setProperty(generatedKeyName, MessageFormat.format(FitnesseMarkup.KEYBOARD_SPECIAL_KEY_RENDERING_MARKUP, generatedKeyName, keyValue));
	}
}