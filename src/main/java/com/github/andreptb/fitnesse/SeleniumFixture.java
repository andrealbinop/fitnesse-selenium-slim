package com.github.andreptb.fitnesse;

import com.github.andreptb.fitnesse.util.FitnesseMarkup;
import com.github.andreptb.fitnesse.util.SeleniumElementFinder;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.reflections.Reflections;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Slim fixture to execute Selenium commands, see README.md for more information.
 */
public class SeleniumFixture {

	/**
	 * HTTP scheme prefix, to detect remote driver
	 */
	private static final String HTTP_PREFIX = "http://";
	/**
	 * Used as value for selectables such as checkboxes and radios.
	 */
	private static final String SELECTABLE_ON_VALUE = "on";
	/**
	 * Used as value for selectables such as checkboxes and radios.
	 */
	private static final String SELECTABLE_OFF_VALUE = "off";
	/**
	 * HTML Value attribute, usually used on inputs
	 */
	private static final String INPUT_VALUE_ATTRIBUTE = "value";

	/**
	 * Registered databases, the key being database name and value an instance of JdbcTemplate
	 */
    private WebDriver driver;
	/**
	 * Utility to help finding web elements with provided selector
	 */
	private SeleniumElementFinder elementFinder = new SeleniumElementFinder();
	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	/**
	 * Registers the driver to further execute util commands
	 *
	 * <p><code>
	 * | start browser | <i>browser</i> |
	 * </code></p>
	 * @param browser The browser to be used
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean startBrowser(String browser) throws ReflectiveOperationException, MalformedURLException {
		return startBrowserWith(browser, MapUtils.EMPTY_MAP);
	}

	/**
	 * Registers the driver to further execute util commands
	 *
	 * <p><code>
	 * | start browser | <i>browser</i> | with | <i>capabilities</i> |
	 * </code></p>
	 * @param browser The browser to be used
	 * @return result Boolean result indication of assertion/operation
	 */
    public boolean startBrowserWith(String browser, Map<String, ?> capabilities) throws ReflectiveOperationException, MalformedURLException {
		return startBrowserWith(browser, new DesiredCapabilities(capabilities));
	}


	public boolean startBrowserWith(String browser, DesiredCapabilities capabilities) throws MalformedURLException, ReflectiveOperationException {
		if(StringUtils.startsWithIgnoreCase(browser, SeleniumFixture.HTTP_PREFIX)) {
			close();
			this.driver = new RemoteWebDriver(new URL(browser), capabilities);
			return true;
		}
		Reflections reflections = new Reflections(WebDriver.class.getPackage().getName());
		List<String> availableDrivers = new ArrayList<>();
		for(Class<? extends WebDriver> availableDriver : reflections.getSubTypesOf(WebDriver.class)) {
			String name = availableDriver.getSimpleName();
			availableDrivers.add(StringUtils.lowerCase(StringUtils.removeEnd(name, "Driver")));
			if(StringUtils.startsWithIgnoreCase(name, browser)) {
				close();
				this.driver = availableDriver.getConstructor(Capabilities.class).newInstance(capabilities);
				return true;
			}
		}
		throw new IllegalArgumentException(String.format("Invalid browser [%s]. Available: [%s]", browser, StringUtils.join(availableDrivers, ", ")));
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
		this.driver.get(this.fitnesseMarkup.clean(url));
		return true;
	}

	/**
	 * Asserts current page title
	 *
	 * <p><code>
	 * | ensure title | <i>title</i> |
	 * </code></p>
	 * @return result Boolean result indication of assertion/operation
	 */
	public String title() {
		return this.driver.getTitle();
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
		if(this.driver != null) {
			this.driver.close();
		}
		return true;
	}

	/**
	 * Quits the browser
	 *
	 * <p><code>
	 * | quit |
	 * </code></p>
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean quit() {
		if(this.driver != null) {
			this.driver.quit();
		}
		return true;
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
		this.elementFinder.find(this.driver, locator).sendKeys(value);
		return true;
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
		this.elementFinder.find(this.driver, locator).click();
		return true;
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
		WebElement element = this.elementFinder.find(this.driver, locator);
		return this.fitnesseMarkup.clean(element.getAttribute(SeleniumFixture.INPUT_VALUE_ATTRIBUTE));
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
		WebElement element = elementFinder.find(this.driver, locator);
		return this.fitnesseMarkup.clean(element.getText());
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
		return this.elementFinder.contains(this.driver, locator);
	}
}
