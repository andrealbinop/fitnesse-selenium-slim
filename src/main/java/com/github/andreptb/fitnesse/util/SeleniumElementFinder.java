package com.github.andreptb.fitnesse.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Utility class to parse locators and find elements
 */
public class SeleniumElementFinder {

	/**
	 * Selector pattern containing attribute
	 */
	private static final Pattern SELECTOR_WITH_ATTRIBUTE_PATTERN = Pattern.compile("(.+)@(\\w+)$");
	/**
	 * HTML Value attribute, usually used on inputs
	 */
	public static final String INPUT_VALUE_ATTRIBUTE = "value";
	/**
	 * Locator selector typeIn separator constant
	 */
	private static final String SELECTOR_TYPE_SEPARATOR = "=";
	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	/**
	 * enum mapping selector identifier with selector implementation ({@link By} implementations).
	 */
	enum SelectorType {
		id(By.ById.class),
		name(By.ByName.class),
		css(By.ByCssSelector.class),
		xpath(By.ByXPath.class),
		link(By.ByLinkText.class);

		private Class<? extends By> byClass;

		SelectorType(Class<? extends By> byClass) {
			this.byClass = byClass;
		}
	}

	/**
	 * Tries to selects element, waiting until is available. If locator is null, tries to return the current active (focused) element if there is one
	 *
	 * @see #parse(String)
	 * @param driver instance of {@link WebDriver}
	 * @param locator an element locator
	 * @param timeoutInSeconds time to wait for element
	 * @return webElementFound
	 * @throws NoSuchElementException if element don't exist or cannot be found
	 * @throws TimeoutException if timeoutInSeconds is exceeded
	 */
	public WebElement find(WebDriver driver, By locator, int timeoutInSeconds) {
		if (locator == null) {
			return driver.switchTo().activeElement();
		}
		WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
		return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
	}

	/**
	 * Parses locator to an instance of {@link SeleniumLocator}, which by itself contains {@link By} instance and an optional attribute selector. Tries to emulate Selenium IDE searching methods:
	 * <ul>
	 * <li>By id: 'id=&lt;id&gt;'</li>
	 * <li>By name: 'name=&lt;name&gt;'</li>
	 * <li>By css selector: 'css=#&lt;id&gt;'</li>
	 * <li>By link text selector: 'link=#&lt;linktext&gt;'</li>
	 * <li>By xpath selector: 'div[@id=&lt;id&gt;]'</li>
	 * </ul>
	 * Just like SeleniumIDE, attribute name is parsed from selector when element locator is followed by an @ sign and then the name of the attribute. For Example:
	 * <ul>
	 * <li>'id=&lt;id&gt;@&lt;attributeName&gt;'</li>
	 * </ul>
	 *
	 * @see SeleniumLocator
	 * @param locator to be parsed
	 * @return parsedLocator instance of {@link SeleniumLocator}
	 */
	public SeleniumLocator parse(String locator) {
		String cleanedLocator = this.fitnesseMarkup.clean(locator);
		if (StringUtils.isBlank(cleanedLocator)) {
			return new SeleniumLocator(null, null);
		}
		String attribute = null;
		Matcher matcher = SeleniumElementFinder.SELECTOR_WITH_ATTRIBUTE_PATTERN.matcher(cleanedLocator);
		if (matcher.matches()) {
			cleanedLocator = matcher.group(NumberUtils.INTEGER_ONE);
			 attribute = matcher.group(2);
		}
		String selectorPrefix = StringUtils.substringBefore(cleanedLocator, SeleniumElementFinder.SELECTOR_TYPE_SEPARATOR);
		SelectorType selectorType = EnumUtils.getEnum(SelectorType.class, selectorPrefix);
		if(selectorType == null) {
			selectorType = SelectorType.xpath;
		}
		try {
			return new SeleniumLocator(selectorType.byClass.getConstructor(String.class).newInstance(StringUtils.removeStart(cleanedLocator, selectorType + SeleniumElementFinder.SELECTOR_TYPE_SEPARATOR)), attribute);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unexpected failure instantiating selector: " + selectorPrefix, e);
		}
	}

	/**
	 * DTO holding element selector ({@link By} instance) and an optional attribute name.
	 *
	 * @see SeleniumElementFinder#parse(String)
	 */
	public static class SeleniumLocator {

		/**
		 * {@link By} selector implementation, can be null if locator is meant to find the current active element
		 */
		private final By by;
		/**
		 * Attribute selected, can be null
		 */
		private final String attribute;

		public SeleniumLocator(By by, String attribute) {
			this.by = by;
			this.attribute = attribute;
		}

		public By getBy() {
			return this.by;
		}

		public String getAttributeName() {
			return this.attribute;
		}
	}
}
