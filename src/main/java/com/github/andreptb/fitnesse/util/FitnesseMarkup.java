package com.github.andreptb.fitnesse.util;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import fitnesse.testsystems.TestPage;

/**
 * General utilities to process FitNesse markup syntax so can be used by Selenium Fixture
 */
public class FitnesseMarkup {

	/**
	 * Screenshot dir suffix constant
	 */
	private static final String SCREENSHOT_DIR_MARKUP = "/files/testResults/{0}/screenshots/{1}";
	/**
	 * Markup which presents image preview and download link
	 */
	private static final String SCREENSHOT_LINK_MARKUP = "<a href=\"{0}{1}\" target='_blank'><img src=\"{0}{1}\" height=\"200\"></img</a>";
	/**
	 * @see #compare(Object, Object)
	 */
	private static final Pattern FITNESSE_REGEX_MARKUP = Pattern.compile("^=~/(.+)/$");
	/**
	 * Rootpath variable key
	 */
	private static final String FITNESSE_ROOTPATH = "FITNESSE_ROOTPATH";
	/**
	 * Running page path variable
	 */
	private static final String RUNNING_PAGE_PATH = "RUNNING_PAGE_PATH";
	/**
	 * FitNesseRoot dir variable key
	 */
	private static final String FITNESSE_ROOT_DIR = "FitNesseRoot";
	/**
	 * FitNesse context path variable key
	 */
	private static final String FITNESSE_CONTEXTROOT = "ContextRoot";

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
	 * Copies the src screenshot file to test result dir and creates img markup to be viewed in test page.
	 * Usually used by fixtures that wants to return a image link for the test result.
	 *
	 * @param img File containing the image
	 * @param slimTestContext
	 * @return Image link
	 */
	public String img(String img, TestPage currentTestPage) throws IOException {
		File src = new File(FilenameUtils.normalize(img));
		if(!src.canRead()) {
			return null;
		}
		String imgUrl = MessageFormat.format(FitnesseMarkup.SCREENSHOT_DIR_MARKUP, currentTestPage.getVariable(FitnesseMarkup.RUNNING_PAGE_PATH), src.getName());
		FileUtils.moveFile(src, FileUtils.getFile(currentTestPage.getVariable(FitnesseMarkup.FITNESSE_ROOTPATH), currentTestPage.getVariable(FitnesseMarkup.FITNESSE_ROOT_DIR), imgUrl));
		return MessageFormat.format(FitnesseMarkup.SCREENSHOT_LINK_MARKUP, StringUtils.stripEnd(currentTestPage.getVariable(FitnesseMarkup.FITNESSE_CONTEXTROOT), "/"), imgUrl);
	}
}
