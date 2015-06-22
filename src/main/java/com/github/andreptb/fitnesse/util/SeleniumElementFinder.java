package com.github.andreptb.fitnesse.util;

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
 * Utility to find elements supporting different selectors.
 *
 * @see #find(WebDriver, String)
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

	enum SelectorType {
		id(By.ById.class),
		name(By.ByName.class),
		css(By.ByCssSelector.class),
		link(By.ByLinkText.class);

		private Class<? extends By> byClass;

		SelectorType(Class<? extends By> byClass) {
			this.byClass = byClass;
		}
	}

	/**
	 * Checks if element is contained in the page.
	 *
	 * @see #find(WebDriver, String)
	 * @param driver instance of {@link WebDriver}
	 * @param locator an element locator
	 * @return containsElement true if found, false otherwise
	 */
	public boolean contains(WebDriver driver, String locator) {
		try {
			find(driver, locator);
		} catch (NoSuchElementException e) {
			return false;
		}
		return true;
	}

	/**
	 * Selects element.
	 *
	 * @see #parse(String)
	 * @param driver instance of {@link WebDriver}
	 * @param locator an element locator
	 * @return webElementFound
	 * @throws NoSuchElementException if element don't exist or cannot be found
	 */
	public WebElement find(WebDriver driver, String locator) {
		return driver.findElement(parse(locator));
	}

	/**
	 * Checks if element is contained in the page, waiting for a certain time
	 *
	 * @see #find(WebDriver, String, int)
	 * @param driver instance of {@link WebDriver}
	 * @param locator an element locator
	 * @param timeoutInSeconds time to wait for element
	 * @return containsElement true if found, false otherwise
	 */
	public boolean contains(WebDriver driver, String locator, int timeoutInSeconds) {
		try {
			find(driver, locator, timeoutInSeconds);
		} catch (TimeoutException e) {
			return false;
		}
		return true;
	}

	/**
	 * Tries to selects element, waiting until is available.
	 *
	 * @see #parse(String)
	 * @param driver instance of {@link WebDriver}
	 * @param locator an element locator
	 * @param timeoutInSeconds time to wait for element
	 * @return webElementFound
	 * @throws NoSuchElementException if element don't exist or cannot be found
	 * @throws TimeoutException if timeoutInSeconds is exceeded
	 */
	public WebElement find(WebDriver driver, String locator, int timeoutInSeconds) {
		WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
		return wait.until(ExpectedConditions.presenceOfElementLocated(parse(locator)));
	}

	/**
	 * Parses locator to an instance of {@link By}. Tries to emulate util IDE searching methods
	 * <ul>
	 * <li>By id: 'id=&lt;id&gt;</li>'
	 * <li>By name: 'name=&lt;name&gt;</li>'
	 * <li>By css selector: 'css=#&lt;id&gt;</li>'
	 * <li>By link text selector: 'link=#&lt;linktext&gt;</li>'
	 * <li>By xpath selector: 'div[@id=&lt;id&gt;]</li>'
	 * </ul>
	 *
	 * @param locator to be parse to {@link By} instance
	 * @return selector {@link By} instance selector
	 */
	private By parse(String locator) {
		String cleanedLocator = this.fitnesseMarkup.clean(locator);
		String selectorPrefix = StringUtils.substringBefore(cleanedLocator, SeleniumElementFinder.SELECTOR_TYPE_SEPARATOR);
		SelectorType selectorType = EnumUtils.getEnum(SelectorType.class, selectorPrefix);
		if(selectorType == null) {
			return By.xpath(cleanedLocator);
		}
		String parsedLocator = StringUtils.removeStart(cleanedLocator, selectorPrefix + SeleniumElementFinder.SELECTOR_TYPE_SEPARATOR);
		try {
			return selectorType.byClass.getConstructor(String.class).newInstance(parsedLocator);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unexpected failure instantiating selector: " + selectorPrefix, e);
		}
	}
}
