
package com.github.andreptb.fitnesse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.openqa.selenium.JavascriptExecutor;
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
	 * HTML Value attribute, usually used on inputs
	 */
	private static final String INPUT_VALUE_ATTRIBUTE = "value";

	/**
	 * Constant representing the selector of the current element focused
	 */
	private static final String CURRENT_ELEMENT_FOCUSED = StringUtils.EMPTY;

	/**
	 * Selenium Web Driver, static so the same DRIVER instance can be used through multiple tables
	 */
	private static WebDriver DRIVER;
	/**
	 * Timeout time to wait for elements to be present. Default is 20 seconds
	 */
	private static int WAIT_TIMEOUT = 20;
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
	 * Register runtime to ensure that WebDriver quits before fixture ends
	 */
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				SeleniumFixture.DRIVER.quit();
			}
		});
	}

	/**
	 * Registers the DRIVER to further execute selenium commands
	 * <p>
	 * <code>
	 * | start browser | <i>browser</i> |
	 * </code>
	 * </p>
	 *
	 * @see #startBrowserWith(String, String)
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
		SeleniumFixture.DRIVER = driver;
		return true;
	}

	/**
	 * Returns if browser is available and can be used
	 * | ensure | browser available |
	 *
	 * @return browserStarted
	 */
	public boolean browserAvailable() {
		// http://stackoverflow.com/questions/27616470/webdriver-how-to-check-if-browser-still-exists-or-still-open
		String driverString = ObjectUtils.toString(SeleniumFixture.DRIVER);
		return driverString != null && !StringUtils.containsIgnoreCase(driverString, "null");
	}

	/**
	 * Sets the time to wait for a element or text presence.
	 * Influences the following methods:
	 * <ul>
	 * <li>{@link #present(String)}</li>
	 * </ul>
	 * <code>
	 * | $previousTimeout= | set wait timeout | <i>timeout in seconds</i> |
	 * </code>
	 *
	 * @return previous timeout value
	 */
	public int setWaitTimeout(int timeoutInSeconds) {
		int previousTimeout = SeleniumFixture.WAIT_TIMEOUT;
		SeleniumFixture.WAIT_TIMEOUT = timeoutInSeconds;
		return previousTimeout;
	}

	/**
	 * Navigates to the the desired url
	 * <p>
	 * <code>
	 * | open | <i>url</i> |
	 * </code>
	 * </p>
	 *
	 * @param url to navigate
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean open(String url) {
		if (browserAvailable()) {
			SeleniumFixture.DRIVER.get(this.fitnesseMarkup.clean(url));
			return true;
		}
		return false;
	}

	/**
	 * Opens a popup window with desired <i>url</i>. After opening the window, you'll need to select it using the selectWindow command.
	 * <p>
	 * <code>
	 * | open window | <i>url</i> |
	 * </code>
	 * </p>
	 *
	 * @see #selectWindow(String)
	 * @param url to navigate
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean openWindow(String url) {
		if (!browserAvailable()) {
			return false;
		}
		if (CollectionUtils.isEmpty(SeleniumFixture.DRIVER.getWindowHandles())) {
			return open(url);
		}
		if (SeleniumFixture.DRIVER instanceof JavascriptExecutor) {
			((JavascriptExecutor) SeleniumFixture.DRIVER).executeScript("window.open(arguments[0])", this.fitnesseMarkup.clean(url));
			return true;
		}
		return false;
	}

	/**
	 * Selects a popup window using a window locator; once a popup window has been selected, all commands go to that window.
	 * Currently only window search by title property is supported
	 * <p>
	 * <code>
	 * | select window | <i>locator</i> |
	 * </code>
	 * </p>
	 *
	 * @param locator
	 * @return
	 */
	public boolean selectWindow(String locator) {
		if (!browserAvailable()) {
			return false;
		}
		String currentWindow = SeleniumFixture.DRIVER.getWindowHandle();
		for (String windowId : SeleniumFixture.DRIVER.getWindowHandles()) {
			WebDriver window = SeleniumFixture.DRIVER.switchTo().window(windowId);
			if (this.fitnesseMarkup.compare(locator, window.getTitle()) || this.fitnesseMarkup.compare(locator, window.getCurrentUrl())) {
				return true;
			}
		}
		// if title didn't match anything go back to current window
		if (currentWindow != null) {
			SeleniumFixture.DRIVER.switchTo().window(currentWindow);
		}
		return false;
	}

	/**
	 * Current page title
	 * <p>
	 * | ensure title | <i>title</i> | </code>
	 * </p>
	 *
	 * @return result Boolean result indication of assertion/operation
	 */
	public String title() {
		if (browserAvailable()) {
			return SeleniumFixture.DRIVER.getTitle();
		}
		return null;
	}

	/**
	 * Closes the last tab
	 * <p>
	 * <code>
	 * | close |
	 * </code>
	 * </p>
	 *
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean close() {
		if (browserAvailable()) {
			SeleniumFixture.DRIVER.close();
			Iterator<String> currentWindows = SeleniumFixture.DRIVER.getWindowHandles().iterator();
			if (currentWindows.hasNext()) {
				// if there's still windows opened focus anyone that's still opened
				SeleniumFixture.DRIVER.switchTo().window(currentWindows.next());
			}
			return true;
		}
		return false;
	}

	/**
	 * Sets the value of the current focused input field, as though you typed it in.
	 * Can also be used to set the value of combo boxes, check boxes, etc. In these cases, value should be the value of the option selected, not the visible text.
	 * Last but no least, if the value matches any special key from {@link Keys}, the special key will be typed without clearing the element. Useful when you need
	 * to type something and then press enter or tab.
	 * <p>
	 * <code>
	 * | type | <i>value</i> |
	 * </code>
	 * </p>
	 *
	 * @param value the value to typeIn
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean type(String value) {
		return typeIn(value, SeleniumFixture.CURRENT_ELEMENT_FOCUSED);
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
		element.clear();
		if (StringUtils.isNotBlank(cleanedValue)) {
			element.sendKeys(cleanedValue);;
		}
		return true;
	}

	/**
	 * Simulates keystroke events on the current focused element, as though you typed the value key-by-key.
	 * This simulates a real user typing every character in the specified string; it is also bound by the limitations of a real user, like not being able to type into a invisible or read only
	 * elements. This is useful for dynamic UI widgets (like auto-completing combo boxes) that require explicit key events.
	 * Unlike the simple "type" command, which forces the specified value into the page directly, this command will not replace the existing content. If you want to replace the existing contents, you
	 * need to use the simple "type" command to set the value of the field to empty string to clear the field and then the "sendKeys" command to send the keystroke for what you want to type.
	 * <p>
	 * <code>
	 * | send keys | <i>value</i> |
	 * </code>
	 * </p>
	 *
	 * @param value the value to typeIn
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean sendKeys(String value) {
		return sendKeysIn(value, SeleniumFixture.CURRENT_ELEMENT_FOCUSED);
	}

	/**
	 * Simulates keystroke events on the specified element, as though you typed the value key-by-key.
	 * This simulates a real user typing every character in the specified string; it is also bound by the limitations of a real user, like not being able to type into a invisible or read only
	 * elements. This is useful for dynamic UI widgets (like auto-completing combo boxes) that require explicit key events.
	 * Unlike the simple "type" command, which forces the specified value into the page directly, this command will not replace the existing content. If you want to replace the existing contents, you
	 * need to use the simple "type" command to set the value of the field to empty string to clear the field and then the "sendKeys" command to send the keystroke for what you want to type.
	 * <p>
	 * <code>
	 * | send keys | <i>value</i> | in | <i>locator</i> |
	 * </code>
	 * </p>
	 *
	 * @param value the value to typeIn
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean sendKeysIn(String value, String locator) {
		if (!browserAvailable()) {
			return false;
		}
		WebElement element = this.elementFinder.find(SeleniumFixture.DRIVER, locator);
		String cleanedValue = this.fitnesseMarkup.clean(value);
		CharSequence keys = EnumUtils.getEnum(Keys.class, StringUtils.upperCase(cleanedValue));
		if (keys == null) {
			keys = cleanedValue;
		}
		element.sendKeys(keys);
		return true;
	}

	/**
	 * Clicks on the current focused link, button, checkbox or radio button. If the click action causes a new page to load (like a link usually does), call waitForPageToLoad.
	 * <p>
	 * <code>
	 * | click |
	 * </code>
	 * </p>
	 *
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean click() {
		return click(SeleniumFixture.CURRENT_ELEMENT_FOCUSED);
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
		if (browserAvailable()) {
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
	 * | check | <i>value</i> | <i>locator</i> | <i>expectedValue</i> |
	 * </code>
	 * </p>
	 *
	 * @param locator an element locator
	 * @return value associated with the locator
	 */
	public String value(String locator) {
		if (browserAvailable()) {
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
	 * | check | text | <i>locator</i> | <i>expectedValue</i> |
	 * </code>
	 * </p>
	 *
	 * @param locator an element locator
	 * @return text associated with the locator
	 */
	public String text(String locator) {
		if (browserAvailable()) {
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
		if (!browserAvailable()) {
			return null;
		}
		if (SeleniumFixture.DRIVER instanceof TakesScreenshot) {
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
		if (browserAvailable()) {
			return this.elementFinder.contains(SeleniumFixture.DRIVER, locator, SeleniumFixture.WAIT_TIMEOUT);
		}
		return false;
	}
}
