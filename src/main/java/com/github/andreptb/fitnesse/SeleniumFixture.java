package com.github.andreptb.fitnesse;

import java.io.IOException;
import java.net.MalformedURLException;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.github.andreptb.fitnesse.util.FitnesseMarkup;
import com.github.andreptb.fitnesse.util.SeleniumElementFinder;
import com.github.andreptb.fitnesse.util.FixtureWebDriverProvider;

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
	 * HTTP scheme prefix, to detect remote DRIVER
	 */
	private static final String HTTP_PREFIX = "http://";
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
	 * Returns if browser is available and can be used
	 *
	 * | ensure | available |
	 * @return browserStarted
	 */
	public boolean available() {
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
		if(available()) {
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
		if(available()) {
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
		if(available() && SeleniumFixture.BROWSER_STATE != BrowserState.CLOSED) {
			SeleniumFixture.DRIVER.close();
			SeleniumFixture.BROWSER_STATE = BrowserState.CLOSED;
			return true;
		}
		return false;
	}

	/**
	 * Sets the value of an input field, as though you typed it in.
	 * Can also be used to set the value of combo boxes, check boxes, etc. In these cases, value should be the value of the option selected, not the visible text.
	 *
	 * <p><code>
	 * | type | value | in | locator |
	 * </code></p>
	 * @param value the value to typeIn
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean typeIn(String value, String locator) {
		if(available()) {
			WebElement element = this.elementFinder.find(SeleniumFixture.DRIVER, locator);
			element.clear();
			element.sendKeys(value);
			return true;
		}
		return false;
	}

	/**
	 * Clicks on a link, button, checkbox or radio button. If the click action causes a new page to load (like a link usually does), call waitForPageToLoad.
	 * <p><code>
	 * | click | locator |
	 * </code></p>
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean click(String locator) {
		if(available()) {
			this.elementFinder.find(SeleniumFixture.DRIVER, locator).click();
			return true;
		}
		return false;
	}

	/**
	 * Gets the (whitespace-trimmed) value of an input field (or anything else with a value parameter). For checkbox/radio elements, the value will be "on" or "off" depending on whether the element is checked or not.
	 *
	 * <p><code>
	 * | check | value | locator | expectedValue |
	 * </code></p>
	 * @param locator an element locator
	 * @return value associated with the locator
	 */
	public String value(String locator) {
		if(available()) {
			WebElement element = this.elementFinder.find(SeleniumFixture.DRIVER, locator);
			return this.fitnesseMarkup.clean(element.getAttribute(SeleniumFixture.INPUT_VALUE_ATTRIBUTE));
		}
		return null;
	}

	/**
	 * Gets the text of an element. This works for any element that contains text. This command uses either the textContent (Mozilla-like browsers) or the innerText (IE-like browsers) of the element, which is the rendered text shown to the user.
	 *
	 * <p><code>
	 * | check | text | locator | expectedValue |
	 * </code></p>
	 * @param locator an element locator
	 * @return text associated with the locator
	 */
	public String text(String locator) {
		if(available()) {
			WebElement element = this.elementFinder.find(SeleniumFixture.DRIVER, locator);
			return this.fitnesseMarkup.clean(element.getText());
		}
		return null;
	}

	/**
	 * Takes screenshot from current browser state and returns to be previewed in test result page.
	 *
	 * <p><code>
	 * | show | screenshot |
	 * </code></p>
	 * @return screenshot preview and download url
	 */
	public String screenshot() throws IOException {
		if(!available() || BrowserState.NAVIGATING != SeleniumFixture.BROWSER_STATE) {
			return null;
		}
		if(SeleniumFixture.DRIVER instanceof TakesScreenshot) {
			return ((TakesScreenshot) SeleniumFixture.DRIVER).getScreenshotAs(OutputType.FILE).getAbsolutePath();
		}
		return null;
	}

	/**
	 * Verifies that the specified element is somewhere on the page.
	 *
	 * <p><code>
	 * | ensure | present | locator |
	 * </code></p>
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean present(String locator) {
		if(available()) {
			return this.elementFinder.contains(SeleniumFixture.DRIVER, locator);
		}
		return false;
	}
}
