
package com.github.andreptb.fitnesse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.Iterator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.github.andreptb.fitnesse.util.FitnesseMarkup;
import com.github.andreptb.fitnesse.util.FixtureWebDriverProvider;
import com.github.andreptb.fitnesse.util.SeleniumElementFinder;
import com.github.andreptb.fitnesse.util.SeleniumElementFinder.SeleniumLocator;

import fitnesse.ContextConfigurator;

/**
 * Slim fixture to execute Selenium commands, see README.md for more information.
 */
public class SeleniumFixture {

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
	 * <b>on</b> value constant, used by {@link #value(String)}
	 */
	private static final String ON_VALUE = "on";
	/**
	 * <b>off</b> value constant, used by {@link #value(String)}
	 */
	private static final String OFF_VALUE = "off";
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
	 * Screenshot dir to save
	 */
	private static String SCREENSHOT_DIR = ContextConfigurator.DEFAULT_ROOT + "/files/screenshots";

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
	 * <p>
	 * <code>
	 * | start browser | <i>browser</i> |
	 * </code>
	 * </p>
	 * Registers the DRIVER to further execute selenium commands
	 *
	 * @see #startBrowserWith(String, String)
	 * @param browser The browser to be used
	 * @return result Boolean result indication of assertion/operation
	 * @throws MalformedURLException if the remote driver has a malformed URL
	 * @throws ReflectiveOperationException if remote driver class cannot be instantiated
	 */
	public boolean startBrowser(String browser) throws ReflectiveOperationException, MalformedURLException {
		return startBrowserWith(browser, null);
	}

	/**
	 * <p>
	 * <code>
	 * | start browser | <i>browser</i> | with | <i>capabilities</i> |
	 * </code>
	 * </p>
	 * Registers the DRIVER to further execute selenium commands. Capabilities should be informed in the following format:
	 * <p>
	 * name='some test' platform='LINUX' version='xx'
	 * </p>
	 * This format was used instead of regular json format since FitNesse uses brackets for variables. Quotes between values must be used
	 *
	 * @see FixtureWebDriverProvider#createDriver(String, String)
	 * @param browser The browser to be used
	 * @param capabilities Usually used to configure remote driver, but some local driver also uses. For example: name='some test' platform='LINUX' version='xx'
	 * @return result Boolean result indication of assertion/operation
	 * @throws MalformedURLException if the remote driver has a malformed URL
	 * @throws ReflectiveOperationException if remote driver class cannot be instantiated
	 */
	public boolean startBrowserWith(String browser, String capabilities) throws ReflectiveOperationException, MalformedURLException {
		WebDriver driver = this.driverProvider.createDriver(browser, capabilities);
		if (driver == null) {
			return false;
		}
		quit();
		SeleniumFixture.DRIVER = driver;
		return true;
	}

	/**
	 * <p>
	 * <code>
	 * | ensure | browser available |
	 * </code>
	 * </p>
	 * Returns if browser is available and can be used
	 *
	 * @return browserStarted
	 */
	public boolean browserAvailable() {
		// http://stackoverflow.com/questions/27616470/webdriver-how-to-check-if-browser-still-exists-or-still-open
		String driverString = ObjectUtils.toString(SeleniumFixture.DRIVER);
		return StringUtils.isNotBlank(driverString) && !StringUtils.containsIgnoreCase(driverString, "null");
	}

	/**
	 * <p>
	 * <code>
	 * | $previousTimeout= | set wait timeout | <i>timeout in seconds</i> |
	 * </code>
	 * </p>
	 * Sets the time to wait for a element or text presence.
	 * Influences the following methods:
	 * <ul>
	 * <li>{@link #present(String)}</li>
	 * <li>{@link #value(String)}</li>
	 * <li>{@link #text(String)}</li>
	 * <li></li>
	 * </ul>
	 *
	 * @param timeoutInSeconds wait seconds to timeout
	 * @return previous timeout value
	 */
	public int setWaitTimeout(int timeoutInSeconds) {
		int previousTimeout = SeleniumFixture.WAIT_TIMEOUT;
		SeleniumFixture.WAIT_TIMEOUT = timeoutInSeconds;
		return previousTimeout;
	}

	/**
	 * <p>
	 * <code>
	 * | open | <i>url</i> |
	 * </code>
	 * </p>
	 * Navigates to the the desired url
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
	 * <p>
	 * <code>
	 * | open window | <i>url</i> |
	 * </code>
	 * </p>
	 * Opens a popup window with desired <i>url</i>. After opening the window, you'll need to select it using the selectWindow command.
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
	 * <p>
	 * <code>
	 * | select window | <i>locator</i> |
	 * </code>
	 * </p>
	 * Selects a popup window using a window locator; once a popup window has been selected, all commands go to that window.
	 * Currently supports window search by nameOrHandle, title and current url
	 *
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean selectWindow(String locator) {
		if (!browserAvailable()) {
			return false;
		}
		String currentWindow = SeleniumFixture.DRIVER.getWindowHandle();
		for (String windowId : SeleniumFixture.DRIVER.getWindowHandles()) {
			WebDriver window = SeleniumFixture.DRIVER.switchTo().window(windowId);
			if (this.fitnesseMarkup.compare(locator, windowId) || this.fitnesseMarkup.compare(locator, window.getTitle()) || this.fitnesseMarkup.compare(locator, window.getCurrentUrl())) {
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
	 * <p>
	 * <code>
	 * | current window |
	 * </code>
	 * </p>
	 * Returns current window ID. Returns "null" if no window is available
	 *
	 * @return windowID
	 */
	public String currentWindow() {
		if (browserAvailable()) {
			return SeleniumFixture.DRIVER.getWindowHandle();
		}
		return null;
	}

	/**
	 * <p>
	 * <code>
	 * | ensure title | <i>title</i> |
	 * </code>
	 * </p>
	 * Current page title
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
	 * <p>
	 * <code>
	 * | close |
	 * </code>
	 * </p>
	 * Closes the last tab
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
	 * <p>
	 * <code>
	 * | quit |
	 * </code>
	 * </p>
	 * Quits driver instance, closing all associated windows
	 * 
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean quit() {
		try {
			SeleniumFixture.DRIVER.quit();
			SeleniumFixture.DRIVER = null;
			return true;
		} catch (Throwable e) {
			// quietly quit driver
		}
		return false;
	}

	/**
	 * <p>
	 * <code>
	 * | type | <i>value</i> |
	 * </code>
	 * </p>
	 * Sets the value of the current focused input field, as though you typed it in.
	 * Can also be used to set the value of combo boxes, check boxes, etc. In these cases, value should be the value of the option selected, not the visible text.
	 *
	 * @param value the value to typeIn
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean type(String value) {
		return typeIn(value, SeleniumFixture.CURRENT_ELEMENT_FOCUSED);
	}

	/**
	 * <p>
	 * <code>
	 * | type | <i>value</i> | in | <i>locator</i> |
	 * </code>
	 * </p>
	 * Sets the value of an input field, as though you typed it in.
	 * Can also be used to set the value of combo boxes, check boxes, etc. In these cases, value should be the value of the option selected, not the visible text.
	 *
	 * @param value the value to typeIn
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean typeIn(String value, String locator) {
		if (!browserAvailable()) {
			return false;
		}
		WebElement element = findElement(locator);
		String cleanedValue = this.fitnesseMarkup.clean(value);
		element.clear();
		if (StringUtils.isNotBlank(cleanedValue)) {
			element.sendKeys(cleanedValue);;
		}
		return true;
	}

	/**
	 * <p>
	 * <code>
	 * | send keys | <i>value</i> |
	 * </code>
	 * </p>
	 * Simulates keystroke events on the current focused element, as though you typed the value key-by-key.
	 * This simulates a real user typing every character in the specified string; it is also bound by the limitations of a real user, like not being able to type into a invisible or read only
	 * elements. This is useful for dynamic UI widgets (like auto-completing combo boxes) that require explicit key events.
	 * Unlike the simple "type" command, which forces the specified value into the page directly, this command will not replace the existing content. If you want to replace the existing contents, you
	 * need to use the simple "type" command to set the value of the field to empty string to clear the field and then the "sendKeys" command to send the keystroke for what you want to type.
	 *
	 * @param value the value to typeIn
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean sendKeys(String value) {
		return sendKeysIn(value, SeleniumFixture.CURRENT_ELEMENT_FOCUSED);
	}

	/**
	 * <p>
	 * <code>
	 * | send keys | <i>value</i> | in | <i>locator</i> |
	 * </code>
	 * </p>
	 * Simulates keystroke events on the specified element, as though you typed the value key-by-key.
	 * This simulates a real user typing every character in the specified string; it is also bound by the limitations of a real user, like not being able to type into a invisible or read only
	 * elements. This is useful for dynamic UI widgets (like auto-completing combo boxes) that require explicit key events.
	 * Unlike the simple "type" command, which forces the specified value into the page directly, this command will not replace the existing content. If you want to replace the existing contents, you
	 * need to use the simple "type" command to set the value of the field to empty string to clear the field and then the "sendKeys" command to send the keystroke for what you want to type.
	 *
	 * @param value the value to typeIn
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean sendKeysIn(String value, String locator) {
		if (!browserAvailable()) {
			return false;
		}
		findElement(locator).sendKeys(this.fitnesseMarkup.clean(value));
		return true;
	}

	/**
	 * <p>
	 * <code>
	 * | click |
	 * </code>
	 * </p>
	 * Clicks on the current focused link, button, checkbox or radio button. If the click action causes a new page to load (like a link usually does), call waitForPageToLoad.
	 *
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean click() {
		return click(SeleniumFixture.CURRENT_ELEMENT_FOCUSED);
	}

	/**
	 * <p>
	 * <code>
	 * | click | <i>locator</i> |
	 * </code>
	 * </p>
	 * Clicks on a link, button, checkbox or radio button. If the click action causes a new page to load (like a link usually does), call waitForPageToLoad.
	 *
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean click(String locator) {
		if (browserAvailable()) {
			findElement(locator).click();
			return true;
		}
		return false;
	}

	/**
	 * <p>
	 * <code>
	 * | check | <i>value</i> | <i>locator</i> | <i>expectedValue</i> |
	 * </code>
	 * </p>
	 * Gets the (whitespace-trimmed) value of an input field (or anything else with a value parameter). For checkbox/radio elements, the value will be "on" or "off" depending on whether the element is
	 * checked or not.
	 *
	 * @param locator an element locator
	 * @return value associated with the locator
	 */
	public String value(String locator) {
		if (!browserAvailable()) {
			return null;
		}
		WebElement element = findElement(locator);
		String inputType = element.getAttribute(SeleniumFixture.INPUT_TYPE_ATTRIBUTE);
		if (StringUtils.equals(inputType, INPUT_TYPE_CHECKBOX) || StringUtils.equals(inputType, INPUT_TYPE_RADIO)) {
			return element.isSelected() ? SeleniumFixture.ON_VALUE : SeleniumFixture.OFF_VALUE;
		}
		return this.fitnesseMarkup.clean(element.getAttribute(SeleniumElementFinder.INPUT_VALUE_ATTRIBUTE));
	}

	/**
	 * <p>
	 * <code>
	 * | check | <i>value</i> | <i>attributeLocator</i> | <i>expectedValue</i> |
	 * </code>
	 * </p>
	 * Gets the value of an element attribute. The value of the attribute may differ across browsers (this is the case for the "style" attribute, for example).
	 *
	 * @param attributeLocator an element locator followed by an @ sign and then the name of the attribute, e.g. "foo@bar"
	 * @return attributeValue the value of the specified attribute
	 * @throws IllegalArgumentException if locator do not contain an attribute part such as "id=&lt;id&gt;@&lt;attributeName&gt;"
	 */
	public String attribute(String attributeLocator) {
		if (!browserAvailable()) {
			return null;
		}
		SeleniumLocator parsedLocator = this.elementFinder.parse(attributeLocator);
		String attributeName = parsedLocator.getAttributeName();
		if (StringUtils.isBlank(attributeName)) {
			throw new IllegalArgumentException(MessageFormat.format("No attribute name part found, must be something like: {0}@<attributeName>", this.fitnesseMarkup.clean(attributeLocator)));
		}
		WebElement element = this.elementFinder.find(SeleniumFixture.DRIVER, parsedLocator.getBy(), SeleniumFixture.WAIT_TIMEOUT);
		return this.fitnesseMarkup.clean(element.getAttribute(attributeName));
	}

	/**
	 * <p>
	 * <code>
	 * | check | text | <i>locator</i> | <i>expectedValue</i> |
	 * </code>
	 * </p>
	 * Gets the text of an element. This works for any element that contains text. This command uses either the textContent (Mozilla-like browsers) or the innerText (IE-like browsers) of the element,
	 * which is the rendered text shown to the user.
	 *
	 * @param locator an element locator
	 * @return text associated with the locator
	 */
	public String text(String locator) {
		if (browserAvailable()) {
			return this.fitnesseMarkup.clean(findElement(locator).getText());
		}
		return null;
	}

	/**
	 * Internal method to create a {@link SeleniumLocator} from argument and invoke {@link SeleniumElementFinder#find(WebDriver, org.openqa.selenium.By, int)} with {@link #WAIT_TIMEOUT}.
	 *
	 * @param locator an element locator
	 * @return webElementFound instance of {@link WebElement} associated with the locator
	 * @throws IllegalArgumentException if locator contains an attribute such as "id=&lt;id&gt;@&lt;attributeName&gt;" since callers of this method were not intended to work with attributes
	 */
	private WebElement findElement(String locator) {
		SeleniumLocator parsedLocator = this.elementFinder.parse(locator);
		if (StringUtils.isNotBlank(parsedLocator.getAttributeName())) {
			throw new IllegalArgumentException("Illegal use of attribute selector. It's only allowed for the 'attribute' and 'present' actions");
		}
		return this.elementFinder.find(SeleniumFixture.DRIVER, parsedLocator.getBy(), SeleniumFixture.WAIT_TIMEOUT);
	}

	/**
	 * <p>
	 * <code>
	 * | show | screenshot |
	 * </code>
	 * </p>
	 * Takes screenshot from current browser state and returns to be previewed in test result page.
	 *
	 * @return screenshot saved file absolute path
	 * @throws IOException if something goes wrong while manipulating screenshot file
	 */
	public String screenshot() throws IOException {
		if (!browserAvailable()) {
			return null;
		}
		if (SeleniumFixture.DRIVER instanceof TakesScreenshot) {
			File src = ((TakesScreenshot) SeleniumFixture.DRIVER).getScreenshotAs(OutputType.FILE);
			File dest = new File(SeleniumFixture.SCREENSHOT_DIR, src.getName());
			FileUtils.moveFile(src, dest);
			return dest.getAbsolutePath();
		}
		return null;
	}

	/**
	 * <p>
	 * <code>
	 * | set screenshot dir | <i>dir</i> |
	 * </code>
	 * </p>
	 * Takes screenshot from current browser state and returns to be previewed in test result page.
	 * This method can be removed when <a href="https://github.com/unclebob/fitnesse/pull/755">this issue</a> is resolved
	 *
	 * @param dest directory to save screenshot
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean setScreenshotDir(String dest) {
		SeleniumFixture.SCREENSHOT_DIR = FilenameUtils.normalize(this.fitnesseMarkup.clean(dest));
		return true;
	}

	/**
	 * <p>
	 * <code>
	 * | ensure | present | <i>locator</i> |
	 * </code>
	 * </p>
	 * Verifies that the specified element is somewhere on the page.
	 * There's a little difference from SeleniumIDE version, this method also supports attributes, if the selector is something like "id=&lt;id&gt;@&lt;attributeName&gt;",
	 * this method will return true if the attribute exists on the element with any value.
	 *
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean present(String locator) {
		if (!browserAvailable()) {
			return false;
		}
		SeleniumLocator parsedLocator = this.elementFinder.parse(locator);
		String attributeName = parsedLocator.getAttributeName();
		try {
			WebElement element = this.elementFinder.find(SeleniumFixture.DRIVER, parsedLocator.getBy(), SeleniumFixture.WAIT_TIMEOUT);
			if (StringUtils.isNotBlank(attributeName)) {
				return element.getAttribute(attributeName) != null;
			}
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}
}
