
package com.github.andreptb.fitnesse.selenium;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.github.andreptb.fitnesse.util.FitnesseMarkup;

/**
 * Utility class to parse locators and find elements
 */
public class WebElementManipulator {

	/**
	 * Locator selector typeIn separator constant
	 */
	public static final String SELECTOR_TYPE_SEPARATOR = "=";
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


	public <T> T manipulateInputable(WebDriver driver, String locator, int timeoutInSeconds, Function<WebElementContext, T> callback) {
		return manipulate(driver, locator, timeoutInSeconds, context -> {
			WebElement element = context.getElement();
			if (element.isDisplayed() && element.isEnabled()) {
				return callback.apply(context);
			}
			return null;
		});
	}

	public <T> T manipulate(WebDriver driver, String locator, int timeoutInSeconds, Function<WebElementContext, T> callback) {
		WebElementContext context = parse(locator);
		try {
			return manipulate(driver, timeoutInSeconds, input -> retrieve(input, context, true, callback));
		} catch (TimeoutException e) {
			// returns even if expected value differs, fitnesse will fail.
			return retrieve(driver, context, false, callback);
		}
	}

	private <T> T retrieve(WebDriver driver, WebElementContext context, boolean considerExpectedValue, Function<WebElementContext, T> callback) {
		context.setElement(driver.findElement(context.getLocator()));
		T result = callback.apply(context);
		String expectedValue = context.getExpectedValue();
		if (StringUtils.isBlank(expectedValue) || this.fitnesseMarkup.compare(expectedValue, result)) {
			return result;
		}
		return null;
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
	private <T> T manipulate(WebDriver driver, int timeoutInSeconds, ExpectedCondition<T> condition) {
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
	private WebElementContext parse(String locator) {
		String selector = this.fitnesseMarkup.clean(locator);
		String valueToCheck = StringUtils.substringAfterLast(locator, FitnesseMarkup.SELECTOR_VALUE_SEPARATOR);
		String attribute = StringUtils.substringBeforeLast(StringUtils.substringAfterLast(selector, FitnesseMarkup.SELECTOR_ATTRIBUTE_SEPARATOR), FitnesseMarkup.SELECTOR_VALUE_SEPARATOR);
		boolean emptyAttribute = true;
		if (emptyAttribute = !StringUtils.isAlphanumeric(attribute)) {
			attribute = null;
		}
		By by = parseBy(selector, emptyAttribute);
		WebElementContext context = new WebElementContext();
		context.setLocator(by);
		context.setAttribute(attribute);
		context.setExpectedValue(valueToCheck);
		return context;
	}

	private By parseBy(String selector, boolean emptyAttribute) {
		String cleanedSelector = StringUtils.substringBeforeLast(selector, emptyAttribute ? FitnesseMarkup.SELECTOR_VALUE_SEPARATOR : FitnesseMarkup.SELECTOR_ATTRIBUTE_SEPARATOR);
		if (StringUtils.isBlank(cleanedSelector)) {
			return new ByFocus();
		}
		String selectorPrefix = StringUtils.substringBefore(cleanedSelector, WebElementManipulator.SELECTOR_TYPE_SEPARATOR);
		SelectorType selectorType = EnumUtils.getEnum(SelectorType.class, selectorPrefix);
		if (selectorType == null) {
			selectorType = SelectorType.xpath;
		}
		try {
			return selectorType.byClass.getConstructor(String.class).newInstance(StringUtils.removeStart(cleanedSelector, selectorType + WebElementManipulator.SELECTOR_TYPE_SEPARATOR));
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unexpected failure instantiating selector: " + selectorPrefix, e);
		}
	}

	/**
	 * {@link By} implementation that returns the currently focused element
	 */
	static class ByFocus extends By {

		@Override
		public List<WebElement> findElements(SearchContext context) {
			if (context instanceof WebDriver) {
				return Arrays.asList(((WebDriver) context).switchTo().activeElement());
			}
			return null;
		}
	}
}
