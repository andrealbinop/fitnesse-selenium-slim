package com.github.andreptb.fitnesse;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.github.andreptb.fitnesse.util.FitnesseMarkup;
import com.github.andreptb.fitnesse.util.FixtureWebDriverProvider;
import com.github.andreptb.fitnesse.util.SeleniumElementFinder;

/**
 * Slim fixture to execute Selenium commands, see README.md for more information.
 */
public class SeleniumFixture {

	/**
	 * Browser states, since some actions depends the browser to be in a certain state
	 */
	private enum BrowserState {
		UNITIALIZED,
		STARTED,
		NAVIGATING,
		CLOSED;
	}

	/**
	 * HTML Value attribute, usually used on inputs
	 */
	private static final String INPUT_VALUE_ATTRIBUTE = "value";

	/**
	 * Browser current state
	 */
	private static BrowserState BROWSER_STATE = BrowserState.UNITIALIZED;
	/**
	 * Selenium Web Driver, static so the same DRIVER instance can be used through multiple tables
	 */
	private static WebDriver DRIVER;
	/**
	 * Timeout time to wait for elements to be present. Default is 60 seconds
	 */
	private static int WAIT_TIMEOUT = 60;
	/**
	 * Utility to help creating WebDriver instances
	 */
	private FixtureWebDriverProvider driverProvider = new FixtureWebDriverProvider();
	/**
	 * Utility to help finding web elements with provided selector
	 */
	private SeleniumElementFinder elementFinder = new SeleniumElementFinder();
	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	/**
	 * Registers the DRIVER to further execute selenium commands
	 *
	 * <p><code>
	 * | start browser | <i>browser</i> |
	 * </code></p>
	 * @param browser The browser to be used
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean startBrowser(String browser) throws ReflectiveOperationException, MalformedURLException {
		return startBrowserWith(browser, null);
	}

	/**
	 * Registers the DRIVER to further execute selenium commands. Capabilities should be informed in the following format:
	 * <p>
	 * name='some test' platform='LINUX' version='xx'
	 * </p>
	 * This format was used instead of regular json format since FitNesse uses brackets for variables. Quotes between values must be used when values contains spaces
	 * <p>
	 * <code>
	 * | start browser | <i>browser</i> | with | <i>capabilities</i> |
	 * </code>
	 * </p>
	 *
	 * @param browser The browser to be used
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean startBrowserWith(String browser, String capabilities) throws ReflectiveOperationException, MalformedURLException {
		WebDriver driver = this.driverProvider.createDriver(browser, capabilities);
		if (driver == null) {
			return false;
		}
		close();
		SeleniumFixture.DRIVER = driver;
		SeleniumFixture.BROWSER_STATE = BrowserState.STARTED;
		return true;
	}

	/**
	 * Sets the time to wait for a element or text presence.
	 * Influences the following methods:
	 * <ul>
	 * <li>{@link #present(String)}</li>
	 * </ul>
	 * <code>
	 * | set wait timeout | <i>timeout in seconds</i> |
	 * </code>
	 */
	public boolean setWaitTimeout(int timeoutInSeconds) {
		SeleniumFixture.WAIT_TIMEOUT = timeoutInSeconds;
		return true;
	}

	/**
	 * Returns if browser is available and can be used
	 *
	 * | ensure | browser available |
	 * @return browserStarted
	 */
	public boolean browserAvailable() {
		return SeleniumFixture.BROWSER_STATE != BrowserState.UNITIALIZED;
	}
	/**
	 * Navigates to the the desired url
	 *
	 * <p><code>
	 * | open | <i>url</i> |
	 * </code></p>
	 * @param url to navigate
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean open(String url) {
		if(browserAvailable()) {
			SeleniumFixture.DRIVER.get(this.fitnesseMarkup.clean(url));
			SeleniumFixture.BROWSER_STATE = BrowserState.NAVIGATING;
			return true;
		}
		return false;
	}

	/**
	 * Current page title
	 *
	 * <p><code>
	 * | ensure title | <i>title</i> |
	 * </code></p>
	 * @return result Boolean result indication of assertion/operation
	 */
	public String title() {
		if(browserAvailable()) {
			return SeleniumFixture.DRIVER.getTitle();
		}
		return null;
	}

	/**
	 * Closes the last tab
	 *
	 * <p><code>
	 * | close |
	 * </code></p>
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean close() {
		if(browserAvailable() && SeleniumFixture.BROWSER_STATE != BrowserState.CLOSED) {
			SeleniumFixture.DRIVER.close();
			SeleniumFixture.BROWSER_STATE = BrowserState.CLOSED;
			return true;
		}
		return false;
	}

	/**
	 * Sets the value of an input field, as though you typed it in.
	 * Can also be used to set the value of combo boxes, check boxes, etc. In these cases, value should be the value of the option selected, not the visible text.
	 * Last but no least, if the value matches any special key from {@link Keys}, the special key will be typed without clearing the element. Useful when you need
	 * to type something and then press enter or tab.
	 * <p>
	 * <code>
	 * | type | <i>value</i> | in | <i>locator</i> |
	 * </code>
	 * </p>
	 *
	 * @param value the value to typeIn
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean typeIn(String value, String locator) {
		if (!browserAvailable()) {
			return false;
		}
		WebElement element = this.elementFinder.find(SeleniumFixture.DRIVER, locator);
		String cleanedValue = this.fitnesseMarkup.clean(value);
		Keys specialKey = EnumUtils.getEnum(Keys.class, StringUtils.upperCase(cleanedValue));
		if (specialKey != null) {
			element.sendKeys(specialKey);
			return true;
		}
		element.clear();
		element.sendKeys(cleanedValue);
		return true;
	}

	/**
	 * Clicks on a link, button, checkbox or radio button. If the click action causes a new page to load (like a link usually does), call waitForPageToLoad.
	 * <p>
	 * <code>
	 * | click | <i>locator</i> |
	 * </code>
	 * </p>
	 *
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean click(String locator) {
		if(browserAvailable()) {
			this.elementFinder.find(SeleniumFixture.DRIVER, locator).click();
			return true;
		}
		return false;
	}

	/**
	 * Gets the (whitespace-trimmed) value of an input field (or anything else with a value parameter). For checkbox/radio elements, the value will be "on" or "off" depending on whether the element is
	 * checked or not.
	 * <p>
	 * <code>
	 * | check | <i>value</i> | <i>locator</i> | expectedValue |
	 * </code>
	 * </p>
	 *
	 * @param locator an element locator
	 * @return value associated with the locator
	 */
	public String value(String locator) {
		if(browserAvailable()) {
			WebElement element = this.elementFinder.find(SeleniumFixture.DRIVER, locator);
			return this.fitnesseMarkup.clean(element.getAttribute(SeleniumFixture.INPUT_VALUE_ATTRIBUTE));
		}
		return null;
	}

	/**
	 * Gets the text of an element. This works for any element that contains text. This command uses either the textContent (Mozilla-like browsers) or the innerText (IE-like browsers) of the element,
	 * which is the rendered text shown to the user.
	 * <p>
	 * <code>
	 * | check | text | <i>locator</i> | expectedValue |
	 * </code>
	 * </p>
	 *
	 * @param locator an element locator
	 * @return text associated with the locator
	 */
	public String text(String locator) {
		if(browserAvailable()) {
			WebElement element = this.elementFinder.find(SeleniumFixture.DRIVER, locator);
			return this.fitnesseMarkup.clean(element.getText());
		}
		return null;
	}

	/**
	 * Takes screenshot from current browser state and returns to be previewed in test result page.
	 * <p>
	 * <code>
	 * | show | screenshot |
	 * </code>
	 * </p>
	 *
	 * @return screenshot saved file absolute path
	 */
	public String screenshot() throws IOException {
		if(!browserAvailable() || BrowserState.NAVIGATING != SeleniumFixture.BROWSER_STATE) {
			return null;
		}
		if(SeleniumFixture.DRIVER instanceof TakesScreenshot) {
			return ((TakesScreenshot) SeleniumFixture.DRIVER).getScreenshotAs(OutputType.FILE).getAbsolutePath();
		}
		return null;
	}

	/**
	 * Verifies that the specified element is somewhere on the page.
	 * <p>
	 * <code>
	 * | ensure | present | <i>locator</i> |
	 * </code>
	 * </p>
	 *
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean present(String locator) {
		if(browserAvailable()) {
			return this.elementFinder.contains(SeleniumFixture.DRIVER, locator, SeleniumFixture.WAIT_TIMEOUT);
		}
		return false;
	}
}
