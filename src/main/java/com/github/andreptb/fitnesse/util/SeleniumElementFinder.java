
package com.github.andreptb.fitnesse.util;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.github.andreptb.fitnesse.util.SeleniumElementFinderExpectedConditions.ElementDataExpectedCondition;

/**
 * Utility class to parse locators and find elements
 */
public class SeleniumElementFinder {

	/**
	 * Locator selector typeIn separator constant
	 */
	private static final String SELECTOR_TYPE_SEPARATOR = "=";
	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	private SeleniumElementFinderExpectedConditions expectedConditions = new SeleniumElementFinderExpectedConditions();

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
	 * @see #waitUntilFind(WebDriver, int, ExpectedCondition)
	 * @param driver instance of {@link WebDriver}
	 * @param locator to be parsed
	 * @param timeoutInSeconds time to wait for element
	 * @return instance of {@link WebElement} found
	 * @throws TimeoutException if timeoutInSeconds is exceeded and nothing is found
	 */
	public WebElement find(WebDriver driver, String locator, int timeoutInSeconds) {
		return waitUntilFind(driver, timeoutInSeconds, this.expectedConditions.presenceOfElementLocated(parse(locator).getLeft()));
	}

	/**
	 * Tries to selects element, waiting until is available. If locator is null, tries to return the current active (focused) element if there is one. This method will wait until
	 * desired element is ready to receive input, such as keyboard keys and clicks.
	 *
	 * @see #waitUntilFind(WebDriver, int, ExpectedCondition)
	 * @see SeleniumElementFinderExpectedConditions#presenceOfElementDisplayedAndEnabled(By)
	 * @param driver instance of {@link WebDriver}
	 * @param locator to be parsed
	 * @param timeoutInSeconds time to wait for element
	 * @return instance of {@link WebElement} found
	 * @throws TimeoutException if timeoutInSeconds is exceeded and nothing is found
	 */
	public WebElement findToInput(WebDriver driver, String locator, int timeoutInSeconds) {
		return waitUntilFind(driver, timeoutInSeconds, this.expectedConditions.presenceOfElementDisplayedAndEnabled(parse(locator).getLeft()));
	}

	/**
	 * Tries to get an element's attribute, waiting until is available. If the element selector part of the locator is empty, tries to return the current active (focused) element if there is one
	 *
	 * @param driver instance of {@link WebDriver}
	 * @param locator to be parsed
	 * @param timeoutInSeconds time to wait for element
	 * @return attribute value
	 * @throws TimeoutException if timeoutInSeconds is exceeded and nothing is found
	 */
	public String findAttribute(WebDriver driver, String locator, int timeoutInSeconds) {
		Triple<By, String, String> parsedLocator = parse(locator);
		return findWithExpectedResult(driver, timeoutInSeconds, this.expectedConditions.presenceOfElementAttributeLocated(parsedLocator.getLeft(), parsedLocator.getMiddle(), parsedLocator.getRight()));
	}

	/**
	 * Returns if an element or an element's attribute with given locator is present or absent, depending of <b>ensurePresent</b> argument.
	 *
	 * @param driver instance of {@link WebDriver}
	 * @param locator to be parsed
	 * @param timeoutInSeconds time to wait for element
	 * @param ensurePresent if <code>true</code> checks if the element is present on the page. Otherwise checks if the element is absent of the page
	 * @return if an element or attribute with given locator is present on the page
	 */
	public boolean presentOrAbsent(WebDriver driver, String locator, int timeoutInSeconds, boolean ensurePresent) {
		Triple<By, String, String> parsedLocator = parse(locator);
		try {
			return waitUntilFind(driver, timeoutInSeconds, this.expectedConditions.presenceOrAbsenceOfElementOrAttribute(parsedLocator.getLeft(), parsedLocator.getMiddle(), ensurePresent));
		} catch (TimeoutException e) {
			return false;
		}
	}

	public String findText(WebDriver driver, String locator, int timeoutInSeconds) {
		Triple<By, String, String> parsedLocator = parse(locator);
		return findWithExpectedResult(driver, timeoutInSeconds, this.expectedConditions.presenceOfElementText(parsedLocator.getLeft(), parsedLocator.getRight()));
	}

	public String findValue(WebDriver driver, String locator, int timeoutInSeconds) {
		Triple<By, String, String> parsedLocator = parse(locator);
		return findWithExpectedResult(driver, timeoutInSeconds, this.expectedConditions.presenceOfElementValue(parsedLocator.getLeft(), parsedLocator.getRight()));
	}

	private String findWithExpectedResult(WebDriver driver, int timeoutInSeconds, ElementDataExpectedCondition condition) {
		try {
			return waitUntilFind(driver, timeoutInSeconds, condition);
		} catch (TimeoutException e) {
			condition.disableDataCheck();
			return condition.apply(driver);
		}
	}

	/**
	 * Tries to get the return from {@link ExpectedCondition} waiting until configured timeout time
	 *
	 * @param driver instance of {@link WebDriver}
	 * @param timeoutInSeconds time to wait for element
	 * @param condition instance of {@link ExpectedCondition}
	 * @param <T> return type of elementFound
	 * @return elementFound instance of whatever {@link ExpectedCondition} returned
	 * @throws TimeoutException if timeoutInSeconds is exceeded
	 */
	public <T> T waitUntilFind(WebDriver driver, int timeoutInSeconds, final ExpectedCondition<T> condition) {
		WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
		return wait.until(condition);
	}

	/**
	 * Parses locator to an instance of {@link Triple}, which by itself contains {@link By} instance and an optional attribute selector. Tries to emulate Selenium IDE searching methods:
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
	 * @param locator to be parsed
	 * @return parsedLocator instance of {@link Triple}, containing {@link By} selector, attribute name and value to check
	 */
	private Triple<By, String, String> parse(String locator) {
		String selector = this.fitnesseMarkup.clean(locator);
		String valueToCheck = StringUtils.substringAfterLast(locator, FitnesseMarkup.SELECTOR_VALUE_SEPARATOR);
		String attribute = StringUtils.substringBeforeLast(StringUtils.substringAfterLast(selector, FitnesseMarkup.SELECTOR_ATTRIBUTE_SEPARATOR), FitnesseMarkup.SELECTOR_VALUE_SEPARATOR);
		boolean emptyAttribute = true;
		if (emptyAttribute = !StringUtils.isAlphanumeric(attribute)) {
			attribute = null;
		}
		By by = parseBy(selector, emptyAttribute);
		return Triple.of(by, attribute, valueToCheck);
	}

	private By parseBy(String selector, boolean emptyAttribute) {
		String cleanedSelector = StringUtils.substringBeforeLast(selector, emptyAttribute ? FitnesseMarkup.SELECTOR_VALUE_SEPARATOR : FitnesseMarkup.SELECTOR_ATTRIBUTE_SEPARATOR);
		if (StringUtils.isBlank(cleanedSelector)) {
			return null;
		}
		String selectorPrefix = StringUtils.substringBefore(cleanedSelector, SeleniumElementFinder.SELECTOR_TYPE_SEPARATOR);
		SelectorType selectorType = EnumUtils.getEnum(SelectorType.class, selectorPrefix);
		if (selectorType == null) {
			selectorType = SelectorType.xpath;
		}
		try {
			return selectorType.byClass.getConstructor(String.class).newInstance(StringUtils.removeStart(cleanedSelector, selectorType + SeleniumElementFinder.SELECTOR_TYPE_SEPARATOR));
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unexpected failure instantiating selector: " + selectorPrefix, e);
		}
	}
}
