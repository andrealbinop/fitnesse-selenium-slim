
package com.github.andreptb.fitnesse.selenium;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.github.andreptb.fitnesse.util.FitnesseMarkup;

public class SeleniumLocatorParser {

	/**
	 * Constant representing type separator [type]=[value]
	 */
	public static final String SELECTOR_TYPE_SEPARATOR = "=";

	/**
	 * enum mapping selector identifier with selector implementation ({@link By} implementations).
	 */
	private enum LocatorType {
		id(By.ById.class),
		name(By.ByName.class),
		css(By.ByCssSelector.class),
		xpath(By.ByXPath.class),
		link(By.ByLinkText.class);

		private Class<? extends By> byClass;

		LocatorType(Class<? extends By> byClass) {
			this.byClass = byClass;
		}
	}

	/**
	 * Parses locator to an instance of {@link WebElementSelector}. Tries to emulate Selenium IDE searching methods:
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
	 * @return instance of {@link WebElementSelector}
	 */
	public WebElementSelector parse(String locator) {
		String elementSelector = StringUtils.substringBeforeLast(locator, FitnesseMarkup.SELECTOR_VALUE_SEPARATOR);
		String valueToCheck = StringUtils.substringAfterLast(locator, FitnesseMarkup.SELECTOR_VALUE_SEPARATOR);
		return new WebElementSelector(parseBy(elementSelector), valueToCheck);
	}

	private By parseBy(String selector) {
		if (StringUtils.isBlank(selector)) {
			return new ByFocus();
		}
		String selectorPrefix = StringUtils.substringBefore(selector, SeleniumLocatorParser.SELECTOR_TYPE_SEPARATOR);
		LocatorType selectorType = EnumUtils.getEnum(LocatorType.class, selectorPrefix);
		if (selectorType == null) {
			selectorType = LocatorType.xpath;
		}
		try {
			return selectorType.byClass.getConstructor(String.class).newInstance(StringUtils.removeStart(selector, selectorType + SeleniumLocatorParser.SELECTOR_TYPE_SEPARATOR));
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unexpected failure instantiating selector: " + selectorPrefix, e);
		}
	}

	/**
	 * {@link By} implementation that returns the currently focused element
	 */
	public static class ByFocus extends By {

		@Override
		public List<WebElement> findElements(SearchContext context) {
			if (context instanceof WebDriver) {
				return Arrays.asList(((WebDriver) context).switchTo().activeElement());
			}
			return null;
		}
	}

	public static class WebElementSelector {

		private By by;
		private String expectedValue;

		private WebElementSelector(By by, String expectedValue) {
			this.by = by;
			this.expectedValue = expectedValue;
		}

		public By getBy() {
			return this.by;
		}

		public String getExpectedValue() {
			return this.expectedValue;
		}
	}

}
