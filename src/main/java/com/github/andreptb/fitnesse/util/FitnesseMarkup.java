
package com.github.andreptb.fitnesse.util;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;

import fitnesse.ContextConfigurator;
import fitnesse.FitNesseContext;
import fitnesse.testsystems.TestPage;

/**
 * General utilities to process FitNesse markup syntax so can be used by Selenium Fixture
 */
public class FitnesseMarkup {

	/**
	 * Constant of FitnesseRoot files dir (relative path)
	 */
	private static final String FITNESSE_TESTRESULTS_DIR = "files/" + FitNesseContext.testResultsDirectoryName;
	/**
	 * Constant of screenshots dir
	 */
	private static final String SCREENSHOT_DIR = "screenshots";
	/**
	 * Markup which presents image preview and download link
	 */
	private static final String SCREENSHOT_LINK_MARKUP = "<a href=\"{0}{1}/{2}\" target='_blank'><img src=\"{0}{1}/{2}\" height=\"200\"></img</a>";
	/**
	 * Variable name that holds fitnesse root dir
	 */
	private static final String FITNESSE_ROOTPATH_VARIABLE = "FITNESSE_ROOTPATH";
	/**
	 * Variable name that holds FitNesse context root
	 */
	private static final String FITNESSE_CONTEXTROOT_VARIABLE = "ContextRoot";
	/**
	 * Running page path variable
	 */
	private static final String PAGE_PATH_VARIABLE = "RUNNING_PAGE_PATH";
	/**
	 * Running page path name variable
	 */
	private static final String PAGE_NAME_VARIABLE = "RUNNING_PAGE_NAME";
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
	 * Constant representing the value separator in selector [selector]@[atributte]-&gt;[value]
	 */
	public static final String SELECTOR_VALUE_SEPARATOR = "->";
	/**
	 * Constant representing the negation flag in {@link #SELECTOR_VALUE_SEPARATOR}
	 */
	public static final String SELECTOR_VALUE_DENY_INDICATOR = "!";
	/**
	 * Constants representing selector separators [selector]@[atributte]$[value]
	 */
	public static final String SELECTOR_ATTRIBUTE_SEPARATOR = "@";
	/**
	 * Constant representing type separator [type]=[value]
	 */
	public static final String KEY_VALUE_SEPARATOR = "=";
	/**
	 * Constant representing [width]x[height] 'x' separator
	 */
	private static final String WIDTH_HEIGHT_SEPARATOR = "x";

	private static final Pattern WIDTH_HEIGHT_PATTERN = Pattern.compile("(\\d{1,4})x(\\d{1,4})");

	/**
	 * <b>on</b> value constant, see {@link #booleanToOnOrOff(Object)}
	 */
	private static final String ON_VALUE = "on";
	/**
	 * <b>off</b> value constant, see {@link #booleanToOnOrOff(Object)}
	 */
	private static final String OFF_VALUE = "off";

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
		boolean not = StringUtils.startsWith(cleanedExpected, FitnesseMarkup.SELECTOR_VALUE_DENY_INDICATOR);
		cleanedExpected = StringUtils.stripStart(cleanedExpected, FitnesseMarkup.SELECTOR_VALUE_DENY_INDICATOR);
		Matcher matcher = FitnesseMarkup.FITNESSE_REGEX_MARKUP_PATTERN.matcher(cleanedExpected);
		boolean result = false;
		if (matcher.matches()) {
			result = cleanedObtained.matches(matcher.group(NumberUtils.INTEGER_ONE));
		} else {
			result = StringUtils.equals(cleanedExpected, cleanedObtained);
		}
		return not ? !result : result;
	}

	/**
	 * Cleans FitNesse markup from symbols such as:
	 * <ul>
	 * <li>Extracts a keyboard special key value from special key markup. See #</li>
	 * <li>Extracts URL only from HTML generated links</li>
	 * <li>Extracts text from HTML wiki page creation suggestion link</li>
	 * <li>Strips undefined variable ocurrences on text</li>
	 * <li>If value is associated with</li>
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
	 * @see #fileAsTestResult(Object, String, TestPage)
	 * @param img File containing the image
	 * @param testPage containing path information to save the screenshot and generate the link
	 * @return Image link
	 * @throws IOException if file can't be moved to {@link #FITNESSE_TESTRESULTS_DIR}
	 */
	public String imgLink(Object img, TestPage testPage) throws IOException {
		String dest = fileAsTestResult(img, FitnesseMarkup.SCREENSHOT_DIR, testPage);
		return MessageFormat.format(FitnesseMarkup.SCREENSHOT_LINK_MARKUP, testPage.getVariable(FitnesseMarkup.FITNESSE_CONTEXTROOT_VARIABLE), FitnesseMarkup.FITNESSE_TESTRESULTS_DIR, dest);
	}

	/**
	 * Moves a file produced from the fixture to the test result dir so test result reports can be organized.
	 *
	 * @param file File produced from the fixture
	 * @param dirInsideTestResult The subdirectory to save the file inside the test result dir
	 * @param testPage containing path information to move the file
	 * @return file new path
	 * @throws IOException if file can't be moved to {@link #FITNESSE_TESTRESULTS_DIR}
	 */
	public String fileAsTestResult(Object file, String dirInsideTestResult, TestPage testPage) throws IOException {
		File src = FileUtils.getFile(clean(file));
		if (!src.exists()) {
			return null;
		}
		String rootPath = FilenameUtils.concat(testPage.getVariable(FitnesseMarkup.FITNESSE_ROOTPATH_VARIABLE), testPage.getVariable(ContextConfigurator.DEFAULT_ROOT));
		String runningPage = testPage.getVariable(FitnesseMarkup.PAGE_PATH_VARIABLE) + "." + testPage.getVariable(FitnesseMarkup.PAGE_NAME_VARIABLE);
		String destFilename = FilenameUtils.concat(runningPage, FilenameUtils.concat(dirInsideTestResult, src.getName()));
		File dest = FileUtils.getFile(rootPath, FitnesseMarkup.FITNESSE_TESTRESULTS_DIR, destFilename);
		FileUtils.moveFile(src, dest);
		return FilenameUtils.normalize(destFilename, true);
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

	public Pair<String, String> swapValueToCheck(String stringWithValue, String stringToGetValue) {
		String expectedValue = StringUtils.substringAfterLast(stringWithValue, FitnesseMarkup.SELECTOR_VALUE_SEPARATOR);
		return Pair.of(StringUtils.substringBeforeLast(stringWithValue, FitnesseMarkup.SELECTOR_VALUE_SEPARATOR), stringToGetValue + FitnesseMarkup.SELECTOR_VALUE_SEPARATOR + expectedValue);
	}

	/**
	 * @param value file path parts
	 * @return normalized a file path, cleaning each path part and joining with current operating system separator
	 */
	public File cleanFile(Object... value) {
		String[] cleanedValues = Arrays.stream(value).map(valueToClean -> FilenameUtils.normalize(clean(valueToClean))).toArray(size -> new String[value.length]);
		return FileUtils.getFile(cleanedValues);
	}

	/**
	 * Cleans and split the value in [key][separator][value] format.
	 *
	 * @param value to parse, should be in [key][separator][value]
	 * @param separator to be used
	 * @return instance of {@link Pair} containing key and value
	 */
	public Pair<String, String> cleanAndParseKeyValue(Object value, String separator) {
		String cleanedValue = clean(value);
		if (!StringUtils.contains(cleanedValue, separator)) {
			return Pair.of(cleanedValue, StringUtils.EMPTY);
		}
		String key = StringUtils.substringBefore(cleanedValue, separator);
		return Pair.of(key, StringUtils.removeStart(cleanedValue, key + separator));
	}

	/**
	 * Cleans and converts <b>true</b> or <b>false</b> value to {@link #ON_VALUE} or {@link #OFF_VALUE}
	 *
	 * @param value to be converted
	 * @return converted value, {@link #ON_VALUE} if value is <b>true</b> or {@link #ON_VALUE} itself
	 */
	public String booleanToOnOrOff(Object value) {
		return onOrOffToBoolean(value) ? FitnesseMarkup.ON_VALUE : FitnesseMarkup.OFF_VALUE;
	}

	/**
	 * Cleans and converts {@link #ON_VALUE} or {@link #OFF_VALUE} value to boolean.
	 *
	 * @param value to be converted
	 * @return converted value, <b>true</b> if value is {@link #ON_VALUE}, <b>false</b> otherwise
	 */
	public boolean onOrOffToBoolean(Object value) {
		String cleanedValue = clean(value);
		return Boolean.valueOf(cleanedValue) || StringUtils.equalsIgnoreCase(cleanedValue, FitnesseMarkup.ON_VALUE);
	}

	/**
	 * @param width Numeric value representing a width value
	 * @param height Numeric value representing a height value
	 * @return formatted width and height. If width=1024 and height=768, will output 1024x768
	 */
	public String formatWidthAndHeight(Object width, Object height) {
		return width + FitnesseMarkup.WIDTH_HEIGHT_SEPARATOR + height;
	}

	/**
	 * @param widthAndHeight {@link String} containing width and height, separated by {@link #WIDTH_HEIGHT_SEPARATOR}. Example: 1920x1080, 1280x720.
	 * @return instance of {@link Pair} containing width in {@link Pair#getLeft()} and height in {@link Pair#getRight()}
	 */
	public Pair<Integer, Integer> parseWidthAndHeight(String widthAndHeight) {
		String cleanedWidthAndHeight = clean(widthAndHeight);
		Matcher matcher = FitnesseMarkup.WIDTH_HEIGHT_PATTERN.matcher(cleanedWidthAndHeight);
		if(!matcher.matches()) {
			throw new IllegalArgumentException("Invalid width and height format, should be something like [width pixels]x[height pixels]. Obtained: " + widthAndHeight);
		}
		return Pair.of(NumberUtils.toInt(matcher.group(1)), NumberUtils.toInt(matcher.group(2)));
	}
}