package com.github.andreptb.fitnesse.util;

import java.io.IOException;
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
	private static final Pattern FITNESSE_REGEX_MARKUP = Pattern.compile("^=~/(.+)/$");

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
		Matcher matcher = FitnesseMarkup.FITNESSE_REGEX_MARKUP.matcher(cleanedExpected);
		if (matcher.matches()) {
			return cleanedObtained.matches(matcher.group(NumberUtils.INTEGER_ONE));
		}
		return StringUtils.equals(cleanedExpected, cleanedObtained);
	}

	/**
	 * Cleans FitNesse markup from symbols such as:
	 * <ul>
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
	public String imgLink(Object img) throws IOException {
		String cleanedImg = FilenameUtils.normalize(ObjectUtils.toString(img), true);
		if(StringUtils.containsIgnoreCase(cleanedImg, FitnesseMarkup.FITNESSE_ROOT_FILES_DIR)) {
			cleanedImg = FitnesseMarkup.FITNESSE_ROOT_FILES_DIR + StringUtils.substringAfter(cleanedImg, FitnesseMarkup.FITNESSE_ROOT_FILES_DIR);
		}
		return MessageFormat.format(FitnesseMarkup.SCREENSHOT_LINK_MARKUP, cleanedImg);
	}
}