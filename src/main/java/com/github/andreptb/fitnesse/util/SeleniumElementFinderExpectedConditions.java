
package com.github.andreptb.fitnesse.util;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.google.common.base.Function;

/**
 * Utility Selenium class used exclusively by {@link SeleniumElementFinder} to provide {@link ExpectedCondition} implementations, just like {@link ExpectedConditions} do.
 */
class SeleniumElementFinderExpectedConditions {

	/**
	 * HTML Value attribute, usually used on inputs
	 */
	private static final String INPUT_TYPE_ATTRIBUTE = "type";
	/**
	 * HTML input type radio attribute constant
	 */
	private static final String INPUT_TYPE_RADIO = "radio";
	/**
	 * HTML input type checkbox attribute constant
	 */
	private static final String INPUT_TYPE_CHECKBOX = "checkbox";
	/**
	 * HTML input value attribute constant
	 */
	private static final String INPUT_VALUE_ATTRIBUTE = "value";

	/**
	 * <b>on</b> value constant, used by {@link #value(String)}
	 */
	private static final String ON_VALUE = "on";
	/**
	 * <b>off</b> value constant, used by {@link #value(String)}
	 */
	private static final String OFF_VALUE = "off";
	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	/**
	 * Extends {@link ExpectedConditions#presenceOfElementLocated(By)} by returning the current focused element if <code>by == null</code>
	 *
	 * @param by instance o {@link By} representing an element selector
	 * @return instance of {@link ExpectedCondition}
	 */
	ExpectedCondition<WebElement> presenceOfElementLocated(final By by) {
		return presenceOfElementLocated(by, new Function<WebElement, WebElement>() {

			@Override
			public WebElement apply(WebElement element) {
				return element;
			}
		});
	}

	/**
	 * Extends {@link ExpectedConditions#presenceOfElementLocated(By)} by returning the current focused element if <code>by == null</code>
	 * and requires a callback to properly validate if found element can be returned
	 *
	 * @param by instance o {@link By} representing an element selector
	 * @param condition Callback that allows further validation and filtering for the found element before returning
	 * @return instance of {@link ExpectedCondition}
	 */
	<T> ExpectedCondition<T> presenceOfElementLocated(final By by, final Function<WebElement, T> condition) {
		return new ExpectedCondition<T>() {

			@Override
			public T apply(WebDriver input) {
				WebElement element;
				if (by != null) {
					element = ExpectedConditions.presenceOfElementLocated(by).apply(input);
				} else {
					element = input.switchTo().activeElement();
				}
				return condition.apply(element);
			}

		};
	}

	/**
	 * Implements an {@link ExpectedCondition} which locates element attributes
	 *
	 * @param by instance o {@link By} representing an element selector
	 * @param attributeName Attribute name for the given element to be searched
	 * @return instance of {@link ExpectedCondition}
	 */
	ElementDataExpectedCondition presenceOfElementAttributeLocated(By by, final String attributeName, final String expectedValue) {
		return new ElementDataExpectedCondition(by, expectedValue, new Function<WebElement, String>() {

			@Override
			public String apply(WebElement element) {
				String attributeValue = element.getAttribute(attributeName);
				if (attributeValue == null) {
					throw new NoSuchElementException(String.format("Unable to locate attribute %s@%s", element.getTagName(), attributeName));
				}
				return attributeValue;
			}
		});
	}

	/**
	 * Implements an {@link ExpectedCondition} which returns a boolean value indicating if the selector (and optionally an attribute) is present (or absent) in the page, depending of ensurePresence
	 * argument
	 *
	 * @param by instance o {@link By} representing an element selector
	 * @param attributeName Attribute name for the given element to be searched
	 * @param ensurePresence if <code>true</code> checks if the element is present on the page. Otherwise checks if the element is absent of the page
	 * @return instance of {@link ExpectedCondition}
	 */
	ExpectedCondition<Boolean> presenceOrAbsenceOfElementOrAttribute(By by, String attributeName, final boolean ensurePresence) {
		final ExpectedCondition<?> condition = StringUtils.isNotBlank(attributeName) ? presenceOfElementAttributeLocated(by, attributeName, StringUtils.EMPTY) : presenceOfElementLocated(by);
		return new ExpectedCondition<Boolean>() {

			@Override
			public Boolean apply(WebDriver element) {
				try {
					condition.apply(element);
				} catch (NotFoundException e) {
					return !ensurePresence;
				}
				return ensurePresence;
			}

		};
	}

	/**
	 * Same as {@link #presenceOfElementLocated(By)} but element must also be {@link WebElement#isDisplayed()} and {@link WebElement#isEnabled()}
	 *
	 * @param by instance o {@link By} representing an element selector
	 * @return instance of {@link ExpectedCondition}
	 */
	ExpectedCondition<WebElement> presenceOfElementDisplayedAndEnabled(By by) {
		return presenceOfElementLocated(by, new Function<WebElement, WebElement>() {

			@Override
			public WebElement apply(WebElement element) {
				if (element.isDisplayed() && element.isEnabled()) {
					return element;
				}
				return null;
			}
		});
	}

	/**
	 * Implements an {@link ExpectedCondition} which returns the text content associated with the selector. Allows comparing with an expected value
	 * so wait behavior can be implemented for FitNesse value check
	 *
	 * @param by instance o {@link By} representing an element selector
	 * @param expectedValue Optional {@link String} to be compared. This should trigger wait behavior properly in FitNesse check/check not actions
	 * @param fitnesseMarkup instance so {@link FitnesseMarkup#compare(Object, Object)} is available
	 * @return instance of {@link ExpectedCondition}
	 */
	ElementDataExpectedCondition presenceOfElementText(By by, final String expectedValue) {
		return new ElementDataExpectedCondition(by, expectedValue, new Function<WebElement, String>() {

			@Override
			public String apply(WebElement element) {
				return element.getText();
			}
		});
	}

	/**
	 * Implements an {@link ExpectedCondition} which returns the value attribute content associated with the selector. Allows comparing with an expected value
	 * so wait behavior can be implemented for FitNesse value check. Note that checkbox and radios values are interpreted as {@link #ON_VALUE} if checked and {@link #OFF_VALUE} if not
	 *
	 * @param by instance o {@link By} representing an element selector
	 * @param expectedValue Optional {@link String} to be compared. This should trigger wait behavior properly in FitNesse check/check not actions
	 * @param fitnesseMarkup instance so {@link FitnesseMarkup#compare(Object, Object)} is available
	 * @return instance of {@link ExpectedCondition}
	 */
	ElementDataExpectedCondition presenceOfElementValue(By by, final String expectedValue) {
		return new ElementDataExpectedCondition(by, expectedValue, new Function<WebElement, String>() {

			@Override
			public String apply(WebElement element) {
				String inputType = element.getAttribute(SeleniumElementFinderExpectedConditions.INPUT_TYPE_ATTRIBUTE);
				if (StringUtils.equals(inputType, SeleniumElementFinderExpectedConditions.INPUT_TYPE_CHECKBOX) || StringUtils.equals(inputType, SeleniumElementFinderExpectedConditions.INPUT_TYPE_RADIO)) {
					return element.isSelected() ? SeleniumElementFinderExpectedConditions.ON_VALUE : SeleniumElementFinderExpectedConditions.OFF_VALUE;
				}
				return element.getAttribute(SeleniumElementFinderExpectedConditions.INPUT_VALUE_ATTRIBUTE);
			}
		});
	}

	class ElementDataExpectedCondition implements ExpectedCondition<String> {

		private String expectedData;
		private ExpectedCondition<WebElement> condition;
		private Function<WebElement, String> dataProvider;

		public ElementDataExpectedCondition(By by, String expectedData, Function<WebElement, String> dataProvider) {
			this.expectedData = expectedData;
			this.condition = presenceOfElementLocated(by);
			this.dataProvider = dataProvider;
		}

		@Override
		public String apply(WebDriver input) {
			String data = this.dataProvider.apply(this.condition.apply(input));
			if (StringUtils.isBlank(this.expectedData) || SeleniumElementFinderExpectedConditions.this.fitnesseMarkup.compare(this.expectedData, data)) {
				return data;
			}
			return null;
		}

		public void disableDataCheck() {
			this.expectedData = StringUtils.EMPTY;
		}

	}
}
