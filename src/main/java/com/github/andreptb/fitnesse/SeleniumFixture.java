
package com.github.andreptb.fitnesse;

import com.github.andreptb.fitnesse.selenium.BrowserDialogHelper;
import com.github.andreptb.fitnesse.selenium.FrameWebElementHelper;
import com.github.andreptb.fitnesse.selenium.SelectWebElementHelper;
import com.github.andreptb.fitnesse.selenium.WebDriverHelper;
import com.github.andreptb.fitnesse.selenium.WebDriverHelper.StopTestWithWebDriverException;
import com.github.andreptb.fitnesse.util.FitnesseMarkup;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.*;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebElement;

import java.io.IOException;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Slim fixture to execute Selenium commands, see README.md for more information.
 */
public class SeleniumFixture {

	/**
	 * HTML Type attribute, usually used on inputs
	 */
	private static final String INPUT_TYPE_ATTRIBUTE = "type";
	/**
	 * HTML value for input type=file
	 */
	private static final String INPUT_TYPE_FILE_VALUE = "file";

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
	 * Window URL with blank page, see {@link #setDryRun(String)}
	 */
	private static final String BLANK_PAGE = "about:blank";

	/**
	 * Instance that wraps {@link WebDriver} providing utility methods to manipulate elements and such. Attribute is static to keep state between table invocations
	 */
	private static WebDriverHelper WEB_DRIVER = new WebDriverHelper();

	/**
	 * Utility to help selecting drop downs
	 */
	private SelectWebElementHelper selectHelper = new SelectWebElementHelper();
	/**
	 * Utility to help selecting frames
	 */
	private FrameWebElementHelper frameHelper = new FrameWebElementHelper();
	/**
	 * Utility to help manipulating browser native dialogs (alert and confirm)
	 */
	private BrowserDialogHelper dialogHelper = new BrowserDialogHelper();
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
	 * @throws ReflectiveOperationException if remote driver class cannot be instantiated
	 * @throws IOException if IO error occurs if invalid URL is used when connecting to remote drivers
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean startBrowser(String browser) throws ReflectiveOperationException, IOException {
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
	 * @param browser The browser to be used
	 * @param capabilities Usually used to configure remote driver, but some local driver also uses. For example: name='some test' platform='LINUX' version='xx'
	 * @throws ReflectiveOperationException if remote driver class cannot be instantiated
	 * @throws IOException if IO error occurs if invalid URL is used when connecting to remote drivers
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean startBrowserWith(String browser, String capabilities) throws ReflectiveOperationException, IOException {
		return startBrowserWithAndPreferences(browser, capabilities, null);
	}

	/**
	 * <p>
	 * <code>
	 * | start browser | <i>browser</i> | with preferences | <i>browser preferences</i> |
	 * </code>
	 * </p>
	 * Registers the DRIVER to further execute selenium commands. Preferences should be informed in the following format:
	 * <p>
	 * name='some test' platform='LINUX' version='xx'
	 * </p>
	 * This format was used instead of regular json format since FitNesse uses brackets for variables. Quotes between values must be used
	 *
	 * @param browser The browser to be used
	 * @param browserPreferences Allows profile configuration for some browser. At this moment supports Chrome and Firefox drivers (local and remote)
	 * @return result Boolean result indication of assertion/operation
	 * @throws ReflectiveOperationException if remote driver class cannot be instantiated
	 * @throws IOException if IO error occurs if invalid URL is used when connecting to remote drivers
	 */
	public boolean startBrowserWithPreferences(String browser, String browserPreferences) throws ReflectiveOperationException, IOException {
		return startBrowserWithAndPreferences(browser, null, browserPreferences);
	}

	/**
	 * <p>
	 * <code>
	 * | start browser | <i>browser</i> | with | <i>capabilities</i> | and preferences | <i>browser preferences</i> |
	 * </code>
	 * </p>
	 * Registers the DRIVER to further execute selenium commands. Capabilities as well as browser preferences should be informed in the following format:
	 * <p>
	 * key1='value1' key2='value2' key3='value3'
	 * </p>
	 * This format was used instead of regular json format since FitNesse uses brackets for variables. Quotes between values must be used
	 *
	 * @param browser The browser to be used
	 * @param capabilities Usually used to configure remote driver, but some local driver also uses. For example: name='some test' platform='LINUX' version='xx'
	 * @param browserPreferences Allows profile configuration for some browser. At this moment supports Chrome and Firefox drivers (local and remote)
	 * @return result Boolean result indication of assertion/operation
	 * @throws ReflectiveOperationException if remote driver class cannot be instantiated
	 * @throws IOException if IO error occurs if invalid URL is used when connecting to remote drivers
	 */
	public boolean startBrowserWithAndPreferences(String browser, String capabilities, String browserPreferences) throws ReflectiveOperationException, IOException {
		SeleniumFixture.WEB_DRIVER.connect(browser, capabilities, browserPreferences);
		return true;
	}

	/**
	 * <p>
	 * <code>
	 * | $previousTimeout= | set wait timeout | <i>timeout in seconds</i> |
	 * </code>
	 * </p>
	 * Sets the time to wait while finding an element.
	 *
	 * @param timeoutInSeconds wait seconds to timeout
	 * @return previous timeout value
	 */
	public int setWaitTimeout(int timeoutInSeconds) {
		int previousTimeoutInSeconds = SeleniumFixture.WEB_DRIVER.getTimeoutInSeconds();
		SeleniumFixture.WEB_DRIVER.setTimeoutInSeconds(timeoutInSeconds);
		return previousTimeoutInSeconds;
	}

	/**
	 * <p>
	 * <code>
	 * | check | last command duration | &lt; <i>duration in seconds</i> |
	 * </code>
	 * </p>
	 *
	 * @return how much time the last command took to complete. Useful to ensure tests perfomance and such. Will be 0 if no commands were executed
	 */
	public long lastCommandDuration() {
		return SeleniumFixture.WEB_DRIVER.getLastActionDurationInSeconds();
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
		return SeleniumFixture.WEB_DRIVER.doWhenAvailable(url, (driver, parsedLocator) -> driver.get(parsedLocator.getOriginalSelector()));
	}

	/**
	 * <p>
	 * <code>
	 * | refresh |
	 * </code>
	 * </p>
	 * Simulates the user clicking the "Refresh" button on their browser.
	 *
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean refresh() {
		return SeleniumFixture.WEB_DRIVER.doWhenAvailable(StringUtils.EMPTY, (driver, parsedLocator) -> driver.navigate().refresh());
	}

	/**
	 * <p>
	 * <code>
	 * | go back |
	 * </code>
	 * </p>
	 * Simulates the user clicking the "back" button on their browser.
	 *
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean goBack() {
		return SeleniumFixture.WEB_DRIVER.doWhenAvailable(StringUtils.EMPTY, (driver, parsedLocator) -> driver.navigate().back());
	}

	/**
	 * <p>
	 * <code>
	 * | check | current url | <i>url</i> |
	 * </code>
	 * </p>
	 *
	 * @return current url defined in browser
	 */
	public String currentUrl() {
		return currentUrl(StringUtils.EMPTY);
	}

	/**
	 * <p>
	 * <code>
	 * | check | current url | <i>url</i> | <i>url</i> |
	 * </code>
	 * </p>
	 * To be used along slim check action. Will respect {@link #setWaitTimeout(int)} before triggering failure. If using selenium table, please ignore this action.
	 *
	 * @param expectedUrl that we'll wait for
	 * @return the current page title
	 */
	public String currentUrl(String expectedUrl) {
		return SeleniumFixture.WEB_DRIVER.getWhenAvailable(expectedUrl, (driver, locator) -> driver.getCurrentUrl());
	}

	private void openWindow(WebDriver driver, String url) {
		String cleansedUrl = this.fitnesseMarkup.clean(url);
		if (CollectionUtils.isEmpty(driver.getWindowHandles())) {
			driver.get(cleansedUrl);
		} else if (driver instanceof JavascriptExecutor) {
			((JavascriptExecutor) driver).executeScript("window.open(arguments[0])", cleansedUrl);
		}
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
		return SeleniumFixture.WEB_DRIVER.doWhenAvailable(url, (driver, parsedLocator) -> openWindow(driver, parsedLocator.getOriginalSelector()));
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
		return SeleniumFixture.WEB_DRIVER.doWhenAvailable(locator, (driver, parsedLocator) -> {
			String parsedWindowLocator = parsedLocator.getOriginalSelector();
			String currentWindow = driver.getWindowHandle();
			for (String windowId : driver.getWindowHandles()) {
				WebDriver window = driver.switchTo().window(windowId);
				if (this.fitnesseMarkup.compare(parsedWindowLocator, windowId) || this.fitnesseMarkup.compare(parsedWindowLocator, window.getTitle()) || this.fitnesseMarkup.compare(parsedWindowLocator, window.getCurrentUrl())) {
					return;
				}
			}
			// if title didn't match anything go back to current window
			if (currentWindow != null) {
				driver.switchTo().window(currentWindow);
			}
			throw new NoSuchElementException("No window found for locator: " + parsedWindowLocator);
		});
	}

	/**
	 * <p>
	 * <code>
	 * | select frame | <i>locator</i> |
	 * </code>
	 * </p>
	 * Selects a frame within the current window. (You may invoke this command multiple times to select nested frames.) To select the parent frame, use "relative=parent" as a locator; to select the
	 * top frame, use "relative=top". You can also select a frame by its 0-based index number; select the first frame with "index=0", or the third frame with "index=2".
	 * Note that you can use all element locators such as <b>id</b>, <b>css</b>, <b>xpath</b> and so on
	 *
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean selectFrame(String locator) {
		return this.frameHelper.select(SeleniumFixture.WEB_DRIVER, locator);
	}

	/**
	 * <p>
	 * <code>
	 * | show | current window |
	 * </code>
	 * </p>
	 * Returns current window ID. Returns "null" if no window is available
	 *
	 * @return windowID
	 */
	public String currentWindow() {
		return currentWindow(StringUtils.EMPTY);
	}

	/**
	 * <p>
	 * <code>
	 * | check | current window | <i>windowHandle</i> | <i>windowHandle</i> |
	 * </code>
	 * </p>
	 * To be used along slim check action. Will respect {@link #setWaitTimeout(int)} before triggering failure. If using selenium table, please ignore this action.
	 *
	 * @param expectedWindowHandle that we'll wait for
	 * @return the current page title
	 */
	public String currentWindow(String expectedWindowHandle) {
		return SeleniumFixture.WEB_DRIVER.getWhenAvailable(expectedWindowHandle, (driver, locator) -> driver.getWindowHandle());
	}

	/**
	 * <p>
	 * <code>
	 * | window maximize |
	 * </code>
	 * </p>
	 * Resize currently selected window to take up the entire screen
	 *
	 * @return the window size after maximizing
	 */
	public String windowMaximize() {
		return SeleniumFixture.WEB_DRIVER.getWhenAvailable(StringUtils.EMPTY, (driver, parsedLocator) -> {
			Window window = driver.manage().window();
			window.maximize();
			return this.fitnesseMarkup.formatWidthAndHeight(window.getSize().getWidth(), window.getSize().getHeight());
		});
	}

	/**
	 * <p>
	 * <code>
	 * | set window size | <i>[width]x[height]</i> |
	 * </code>
	 * </p>
	 * Set the size of the current window. This will change the outer window dimension, not just the view port, synonymous to window.resizeTo() in JS.
	 *
	 * @param widthAndHeight windows size, in [width]x[height] format
	 * @return result Boolean result indication of assertion/operation
	 * @throws IllegalArgumentException if widthAndHeight is malformed
	 */
	public boolean setWindowSize(String widthAndHeight) {
		return SeleniumFixture.WEB_DRIVER.doWhenAvailable(StringUtils.EMPTY, (driver, parsedLocator) -> {
			Pair<Integer, Integer> parsedWidthAndHeight = this.fitnesseMarkup.parseWidthAndHeight(widthAndHeight);
			driver.manage().window().setSize(new Dimension(parsedWidthAndHeight.getLeft(), parsedWidthAndHeight.getRight()));
		});
	}

	/**
	 * <p>
	 * <code>
	 * | window size |
	 * </code>
	 * </p>
	 *
	 * @return windows size, in [width]x[height] format
	 */
	public String windowSize() {
		return windowSize(StringUtils.EMPTY);
	}

	/**
	 * <p>
	 * <code>
	 * | window size |
	 * </code>
	 * </p>
	 * To be used along slim check action. Will respect {@link #setWaitTimeout(int)} before triggering failure. If using selenium table, please ignore this action.
	 *
	 * @param expectedWindowSize Expected window size
	 * @return windows size, in [width]x[height] format
	 */
	public String windowSize(String expectedWindowSize) {
		return SeleniumFixture.WEB_DRIVER.getWhenAvailable(expectedWindowSize, (driver, parsedLocator) -> {
			Dimension dimension = driver.manage().window().getSize();
			return String.format("%dx%d", dimension.getWidth(), dimension.getHeight());
		});
	}

	/**
	 * <p>
	 * <code>
	 * | check | title | <i>title</i> |
	 * </code>
	 * </p>
	 * Current page title
	 *
	 * @return the current page title
	 */
	public String title() {
		return title(StringUtils.EMPTY);
	}

	/**
	 * <p>
	 * <code>
	 * | check | title | <i>title</i> | <i>title</i> |
	 * </code>
	 * </p>
	 * To be used along slim check action. Will respect {@link #setWaitTimeout(int)} before triggering failure. If using selenium table, please ignore this action.
	 *
	 * @param expectedTitle that we'll wait for
	 * @return the current page title
	 */
	public String title(String expectedTitle) {
		return SeleniumFixture.WEB_DRIVER.getWhenAvailable(expectedTitle, (driver, locator) -> driver.getTitle());
	}

	/**
	 * <p>
	 * <code>
	 * | close browser tab |
	 * </code>
	 * </p>
	 * Closes the last tab
	 *
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean closeBrowserTab() {
		return SeleniumFixture.WEB_DRIVER.doWhenAvailable(StringUtils.EMPTY, (driver, parsedLocator) -> {
			driver.close();
			Iterator<String> currentWindows = driver.getWindowHandles().iterator();
			if (currentWindows.hasNext()) {
				// if there's still windows opened focus anyone that's still opened
				driver.switchTo().window(currentWindows.next());
			}
		});
	}

	/**
	 * <p>
	 * <code>
	 * | quit browser |
	 * </code>
	 * </p>
	 * Quits driver instance, closing all associated windows
	 *
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean quitBrowser() {
		SeleniumFixture.WEB_DRIVER.quit();
		return true;
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
		return typeIn(value, StringUtils.EMPTY);
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
		return sendKeysIn(value, locator, true);
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
		return sendKeysIn(value, StringUtils.EMPTY);
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
		return sendKeysIn(value, locator, false);
	}

	private boolean sendKeysIn(String value, String locator, boolean clearBefore) {
		Pair<String, String> valueAndLocator = this.fitnesseMarkup.swapValueToCheck(value, locator);
		return SeleniumFixture.WEB_DRIVER.doWhenAvailable(valueAndLocator.getValue(), (driver, parsedLocator) -> {
			WebElement element = driver.findElement(parsedLocator.getBy());
			String cleanedValue = cleanValueToSend(driver, element, valueAndLocator.getKey());
			if (clearBefore) {
				element.clear();
			}
			if (StringUtils.isNotBlank(cleanedValue)) {
				element.sendKeys(cleanedValue);
			}
		});
	}

	/**
	 * When send keys is being executed in a input file=type {@link LocalFileDetector} must be configured for remote drivers. Additionally,
	 * the file path is expanded to be absolute
	 *
	 * @param driver used to run commands
	 * @param element receiving keys
	 * @param value to be set to input file type
	 * @return value expanded to absolute path if for input file type.
	 */
	private String cleanValueToSend(WebDriver driver, WebElement element, String value) {
		if (!StringUtils.equals(element.getAttribute(SeleniumFixture.INPUT_TYPE_ATTRIBUTE), SeleniumFixture.INPUT_TYPE_FILE_VALUE)) {
			return this.fitnesseMarkup.clean(value);
		}
		// set file detector for remote web elements. Local FirefoxDriver uses RemoteWebElement and
		if (element instanceof RemoteWebElement && !ClassUtils.isAssignable(driver.getClass(), FirefoxDriver.class)) {
			((RemoteWebElement) element).setFileDetector(new LocalFileDetector());
		}
		return this.fitnesseMarkup.cleanFile(value).getAbsolutePath();
	}

	/**
	 * <p>
	 * <code>
	 * | click |
	 * </code>
	 * </p>
	 * Clicks on the current focused link, button, checkbox or radio button.
	 *
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean click() {
		return click(StringUtils.EMPTY);
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
		return SeleniumFixture.WEB_DRIVER.doWhenAvailable(locator, (driver, parsedLocator) -> {
			if (this.dialogHelper.click(driver, parsedLocator)) {
				return;
			}
			WebElement element = driver.findElement(parsedLocator.getBy());
			if (!element.isEnabled()) {
				throw new InvalidElementStateException("Element found but is disabled: " + element);
			}
			element.click();
		});
	}

	/**
	 * <p>
	 * <code>
	 * | select | <i>optionLocator</i> |
	 * </code>
	 * </p>
	 * Select an option from the currently focused drop-down using an option locator.
	 * Option locators provide different ways of specifying options of an HTML Select element (e.g. for selecting a specific option, or for asserting that the selected option satisfies a
	 * specification). There are several forms of Select Option Locator.
	 * <ul>
	 * <li><b>label</b>=<i>labelPattern</i>: matches options based on their labels, i.e. the visible text. (This is the default.)</li>
	 * <li><b>value</b>=<i>valuePattern</i>: matches options based on their values.</li>
	 * <li><b>index</b>=<i>index</i>: matches an option based on its index (offset from zero).</li>
	 * </ul>
	 *
	 * @param optionLocator option locator to be used for select action
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean select(String optionLocator) {
		return selectIn(optionLocator, StringUtils.EMPTY);
	}

	/**
	 * <p>
	 * <code>
	 * | select | <i>optionLocator</i> | in | <i>locator</i> |
	 * </code>
	 * </p>
	 * Select an option from a drop-down using an option locator.
	 * Option locators provide different ways of specifying options of an HTML Select element (e.g. for selecting a specific option, or for asserting that the selected option satisfies a
	 * specification). There are several forms of Select Option Locator.
	 * <ul>
	 * <li><b>label</b>=<i>labelPattern</i>: matches options based on their labels, i.e. the visible text. (This is the default.)</li>
	 * <li><b>value</b>=<i>valuePattern</i>: matches options based on their values.</li>
	 * <li><b>index</b>=<i>index</i>: matches an option based on its index (offset from zero).</li>
	 * </ul>
	 *
	 * @param optionLocator option locator to be used for select action
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean selectIn(String optionLocator, String locator) {
		return this.selectHelper.select(SeleniumFixture.WEB_DRIVER, optionLocator, locator);
	}

	/**
	 * <p>
	 * <code>
	 * | check | selected | <i>optionType</i> |
	 * </code>
	 * </p>
	 * Gets option data for selected option for the currently focused select element. Can return three types of information depending of <i>optionType</i>:
	 * <ul>
	 * <li><b>label</b>: Gets option label (visible text) for selected option in the specified select element.</li>
	 * <li><b>value</b>: Gets option value (value attribute) for selected option in the specified select element.</li>
	 * <li><b>index</b>: Gets option index (option number, starting at 0) for selected option in the specified select element.</li>
	 * </ul>
	 *
	 * @param optionType can be label, value or index
	 * @return the appropriate information associated with the select
	 */
	public String selected(String optionType) {
		return selectedIn(optionType, StringUtils.EMPTY);
	}

	/**
	 * <p>
	 * <code>
	 * | check | selected | <i>optionType</i> | in | <i>locator</i> |
	 * </code>
	 * </p>
	 * Gets option data for selected option in the specified select element. Can return three types of information depending of <i>optionType</i>:
	 * <ul>
	 * <li><b>label</b>: Gets option label (visible text) for selected option in the specified select element.</li>
	 * <li><b>value</b>: Gets option value (value attribute) for selected option in the specified select element.</li>
	 * <li><b>index</b>: Gets option index (option number, starting at 0) for selected option in the specified select element.</li>
	 * </ul>
	 *
	 * @param optionType can be label, value or index
	 * @param locator an element locator
	 * @return the appropriate information associated with the select
	 */
	public String selectedIn(String optionType, String locator) {
		return this.selectHelper.selected(SeleniumFixture.WEB_DRIVER, optionType, locator);
	}

	/**
	 * <p>
	 * <code>
	 * | check | value | <i>locator</i> | <i>expectedValue</i> |
	 * </code>
	 * </p>
	 * Gets the (whitespace-trimmed) value of an input field (or anything else with a value parameter). For checkbox/radio elements, the value will be "on" or "off" depending on whether the element is
	 * checked or not.
	 *
	 * @param locator an element locator
	 * @return value associated with the locator
	 */
	public String value(String locator) {
		return SeleniumFixture.WEB_DRIVER.getWhenAvailable(locator, (driver, parsedLocator) -> {
			WebElement element = driver.findElement(parsedLocator.getBy());
			String inputType = element.getAttribute(SeleniumFixture.INPUT_TYPE_ATTRIBUTE);
			if (StringUtils.equals(inputType, SeleniumFixture.INPUT_TYPE_CHECKBOX) || StringUtils.equals(inputType, SeleniumFixture.INPUT_TYPE_RADIO)) {
				return this.fitnesseMarkup.booleanToOnOrOff(element.isSelected());
			}
			return element.getAttribute(SeleniumFixture.INPUT_VALUE_ATTRIBUTE);
		});
	}

	/**
	 * <p>
	 * <code>
	 * | check | <i>attribute</i> | <i>attributeName</i> | in | <i>locator</i> |  <i>expectedValue</i> |
	 * </code>
	 * </p>
	 * Gets the value of an element attribute. The value of the attribute may differ across browsers (this is the case for the "style" attribute, for example).
	 *
	 * @param attributeName the name of the attribute to retrieve the value from
	 * @param locator an element locator
	 * @return value associated with the locator
	 */
	public String attributeIn(String attributeName, String locator) {
		Pair<String, String> attributeAndLocatorWithValue = this.fitnesseMarkup.swapValueToCheck(attributeName, locator);
		return SeleniumFixture.WEB_DRIVER.getWhenAvailable(attributeAndLocatorWithValue.getRight(), (driver, parsedLocator) -> driver.findElement(parsedLocator.getBy()).getAttribute(this.fitnesseMarkup.clean(attributeAndLocatorWithValue.getLeft())));
	}

	/**
	 * <p>
	 * <code>
	 * | check | text | <i>locator</i> | <i>expectedValue</i> |
	 * </code>
	 * </p>
	 * Gets the text of the current focused element. This works for any element that contains text. This command uses either the textContent (Mozilla-like browsers) or the innerText (IE-like browsers)
	 * of the element,
	 * which is the rendered text shown to the user.
	 * <p>
	 * If a dialog box is being presented on the page (such as an alert dialog), this action will return the dialog text
	 * </p>
	 *
	 * @return text associated with the locator
	 */
	public String text() {
		return text(StringUtils.EMPTY);
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
		return SeleniumFixture.WEB_DRIVER.getWhenAvailable(locator, (driver, parsedLocator) -> {
			return Optional.ofNullable(this.dialogHelper.text(driver, parsedLocator)).orElseGet(() -> {
				return driver.findElement(parsedLocator.getBy()).getText();
			});
		});
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
		return SeleniumFixture.WEB_DRIVER.getWhenAvailable(StringUtils.EMPTY, (driver, parsedLocator) -> {
			if (driver instanceof TakesScreenshot) {
				return this.fitnesseMarkup.imgLink(((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64));
			}
			return null;
		});
	}

	/**
	 * <p>
	 * <code>
	 * | ensure | present | <i>locator</i> |
	 * </code>
	 * </p>
	 * <p>
	 * <code>
	 * | reject | present | <i>locator</i> |
	 * </code>
	 * </p>
	 * Verifies that the specified element is somewhere on the page (or not).
	 * There's a few differences from SeleniumIDE version, this method also supports attributes, if the selector is something like "id=&lt;id&gt;@&lt;attributeName&gt;",
	 * this method will return true if the attribute exists on the element with any value.
	 * <p>
	 * <b>Note</b>: Wait behavior won't work properly without selenium table
	 *
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean present(String locator) {
		return Boolean.valueOf(SeleniumFixture.WEB_DRIVER.getWhenAvailable(locator, (driver, parsedLocator) -> {
			boolean ensuring = Boolean.valueOf(parsedLocator.getExpectedValue());
			boolean elementFound = false;
			try {
				elementFound = this.dialogHelper.present(driver, parsedLocator) || driver.findElement(parsedLocator.getBy()) != null;
			} catch (WebDriverException e) {
				// elemento nao foi encontrado
			}
			return Boolean.toString(ensuring == elementFound ? ensuring : !ensuring);
		}));
	}

	/**
	 * <p>
	 * <code>
	 * | $result= | run script | <i>code</i> |
	 * </code>
	 * </p>
	 * Creates a new "script" tag in the body of the current test window, and adds the specified text into the body of the command.
	 * Scripts run in this way can often be debugged more easily than scripts executed using Selenium's "getEval" command.
	 * Beware that JS exceptions thrown in these script tags aren't managed by Selenium, so you should probably wrap your script in try/catch blocks if there is any chance that the script will throw
	 * an exception.
	 *
	 * @param script the JavaScript snippet to run
	 * @return of the javascript snippet that ran
	 */
	public String runScript(String script) {
		return SeleniumFixture.WEB_DRIVER.getWhenAvailable(script, (driver, parsedLocator) -> {
			if (driver instanceof JavascriptExecutor) {
				return Objects.toString(((JavascriptExecutor) driver).executeScript(parsedLocator.getOriginalSelector()), null);
			}
			return null;
		});
	}


	private String acceptConfigReturnPrevious(String newConfig, boolean oldValue, Consumer<Boolean> setter) {
		setter.accept(this.fitnesseMarkup.onOrOffToBoolean(newConfig));
		return this.fitnesseMarkup.booleanToOnOrOff(oldValue);
	}
	/**
	 * <p>
	 * <code>
	 * | stop test on first failure | true |
	 * </code>
	 * </p>
	 *
	 * @param shouldStop If <b>true</b> or <b>on</b>, the test will stop if a failure occurs in any action
	 * @return previous configuration value. If enabled will return <b>on</b>, <b>off</b> otherwise.
	 */
	public String stopTestOnFirstFailure(String shouldStop) {
		return acceptConfigReturnPrevious(shouldStop, SeleniumFixture.WEB_DRIVER.getStopTestOnFirstFailure(), SeleniumFixture.WEB_DRIVER::setStopTestOnFirstFailure);
	}


	/**
	 * <p>
	 * <code>
	 * | set take screenshot on failure | true |
	 * </code>
	 * </p>
	 * @param shouldTake If <b>true</b> or <b>on</b>, a screenshot will be appended (if possible) to a failed selenium operation.
	 * @return previous configuration value. If enabled will return <b>on</b>, <b>off</b> otherwise.
	 */
	public String setTakeScreenshotOnFailure(String shouldTake) {
		return acceptConfigReturnPrevious(shouldTake, SeleniumFixture.WEB_DRIVER.getTakeScreenshotOnFailure(), SeleniumFixture.WEB_DRIVER::setTakeScreenshotOnFailure);
	}

	public String setDryRun(String enableDryRun) {
		boolean dryRun = this.fitnesseMarkup.onOrOffToBoolean(enableDryRun);
		String dryRunWindow = SeleniumFixture.WEB_DRIVER.getDryRunWindow();
		boolean isDryRunAlreadyEnabled = StringUtils.isNotBlank(dryRunWindow);
		if(!dryRun) {
			SeleniumFixture.WEB_DRIVER.setDryRunWindow(null);
			if (isDryRunAlreadyEnabled) {
				selectWindow(dryRunWindow);
				closeBrowserTab();
			}
			return this.fitnesseMarkup.booleanToOnOrOff(isDryRunAlreadyEnabled);
		}
		if(isDryRunAlreadyEnabled) {
			return FitnesseMarkup.ON_VALUE;
		}
		SeleniumFixture.WEB_DRIVER.doWhenAvailable(null, (driver, parsedLocator) -> {
			Collection<String> previousHandles = driver.getWindowHandles();
			openWindow(driver, SeleniumFixture.BLANK_PAGE);
			Optional<String> dryRunWindowId = driver.getWindowHandles().stream().filter(w -> !previousHandles.contains(w)).findFirst();
			if(!dryRunWindowId.isPresent()) {
				throw new StopTestWithWebDriverException("Unable to create blank window to run test in dry run mode");
			}
			SeleniumFixture.WEB_DRIVER.setDryRunWindow(dryRunWindowId.get());
		});
		return FitnesseMarkup.OFF_VALUE;
	}

	/**
	 * <p>
	 * <code>
	 * | ensure | file exists | <i>file</i> |
	 * </code>
	 * </p>
	 * Checks if a file exists in the local filesystem. Will respect wait timeout until file is available
	 * <p>
	 * <b>Important:</b> If you're using remote browsers such as Selenium Grid or SauceLabs, this action probably won't be useful, unless you have access to the node's remote file system.
	 * </p>
	 *
	 * @param file Path of the file to check if exists
	 * @return if the informed file exists on the filesystem
	 */
	public boolean fileExists(String file) {
		return Boolean.valueOf(SeleniumFixture.WEB_DRIVER.getWhenAvailable(file, (driver, parsedLocator) -> Boolean.toString(this.fitnesseMarkup.cleanFile(parsedLocator.getOriginalSelector()).exists())));
	}
}
