package com.github.andreptb.fitnesse.util;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Utility Selenium class used exclusively by {@link SeleniumElementFinder} to provide {@link ExpectedCondition} implementations, just like {@link ExpectedConditions} do.
 */
class SeleniumElementFinderExpectedConditions {

	/**
	 * Extends {@link ExpectedConditions#presenceOfElementLocated(By)} by returning the current focused element if <code>by == null</code>
	 *
	 * @param by instance o {@link By} representing an element selector
	 * @return instance of {@link ExpectedCondition}
	 */
	ExpectedCondition<WebElement> presenceOfElementLocated(final By by) {
		return new ExpectedCondition<WebElement>() {

			@Override
			public WebElement apply(WebDriver input) {
				if (by == null) {
					return input.switchTo().activeElement();
				}
				return ExpectedConditions.presenceOfElementLocated(by).apply(input);
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
	ExpectedCondition<String> presenceOfElementAttributeLocated(final By by, final String attributeName) {
		final ExpectedCondition<WebElement> condition = SeleniumElementFinderExpectedConditions.this.presenceOfElementLocated(by);
		return new ExpectedCondition<String>() {

			@Override
			public String apply(WebDriver input) {
				WebElement element = condition.apply(input);
				String attributeValue = element.getAttribute(attributeName);
				if (attributeValue == null) {
					throw new NoSuchElementException(String.format("Unable to locate attribute %s@%s", element.getTagName(), attributeName));
				}
				return attributeValue;
			}

		};
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
	ExpectedCondition<Boolean> presenceOrAbsenceOfElementOrAttribute(final By by, final String attributeName, final boolean ensurePresence) {
		final ExpectedCondition<?> condition = attributeName != null ? presenceOfElementAttributeLocated(by, attributeName) : presenceOfElementLocated(by);
		return new ExpectedCondition<Boolean>() {

			@Override
			public Boolean apply(WebDriver input) {
				try {
					condition.apply(input);
				} catch (NotFoundException e) {
					return !ensurePresence;
				}
				return ensurePresence;
			}

		};
	}
}
