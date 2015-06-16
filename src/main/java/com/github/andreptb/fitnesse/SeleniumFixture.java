package com.github.andreptb.fitnesse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIUtils;
import org.junit.Assert;
import org.openqa.selenium.By;
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
	 * Prefix to delegate css selectors to WebDriver
	 */
	private static final String CSS_SELECTOR_PREFIX = "css=";
	/**
	 * Prefix to delegate id selectors to WebDriver
	 */
	private static final String ID_SELECTOR_PREFIX = "id=";

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
	 * Registers the driver to further execute selenium commands
	 *
	 * <p><code>
	 * | start | <i>browser</i> |
	 * </code></p>
	 * @param browser The browser to be used
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean start(String browser) throws ReflectiveOperationException, MalformedURLException {
		return startWith(browser, MapUtils.EMPTY_MAP);
	}

	/**
	 * Registers the driver to further execute selenium commands
	 *
	 * <p><code>
	 * | start | <i>browser</i> | with | <i>capabilities</i> |
	 * </code></p>
	 * @param browser The browser to be used
	 * @return result Boolean result indication of assertion/operation
	 */
    public boolean startWith(String browser, Object capabilities) throws ReflectiveOperationException, MalformedURLException {
		WebDriver selectedDriver = startRemoteOrConnectLocal(browser, capabilities);
		close();
		this.driver = selectedDriver;
		return true;
	}


	private WebDriver startRemoteOrConnectLocal(String browser, Object capabilities) throws MalformedURLException, ReflectiveOperationException {
		DesiredCapabilities desiredCapabilities = null;
		if(capabilities instanceof DesiredCapabilities) {
			desiredCapabilities = (DesiredCapabilities) capabilities;
		} else if(capabilities instanceof Map) {
			desiredCapabilities = new DesiredCapabilities((Map<String, ?>) capabilities);
		}
		if(StringUtils.startsWithIgnoreCase(browser, SeleniumFixture.HTTP_PREFIX)) {
			return new RemoteWebDriver(new URL(browser), desiredCapabilities);
		}
		Reflections reflections = new Reflections(WebDriver.class.getPackage().getName());
		List<String> availableDrivers = new ArrayList<>();
		for(Class<? extends WebDriver> availableDriver : reflections.getSubTypesOf(WebDriver.class)) {
			String name = availableDriver.getSimpleName();
			availableDrivers.add(StringUtils.lowerCase(StringUtils.removeEnd(name, "Driver")));
			if(!StringUtils.startsWithIgnoreCase(name, browser)) {
				continue;
			}
			return availableDriver.getConstructor(Capabilities.class).newInstance(desiredCapabilities);
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
		this.driver.get(url);
		return true;
	}

	/**
	 * Asserts current page title
	 *
	 * <p><code>
	 * | assert title | <i>title</i> |
	 * </code></p>
	 * @param title Expected title
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean assertTitle(String title) {
		Assert.assertEquals("Page Title", title, this.driver.getTitle());
		return true;
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
	 * | type | locator | value |
	 * </code></p>
	 * @param locator an element locator
	 * @param value the value to type
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean type(String locator, String value) {
		findElement(locator).sendKeys(value);
		return true;
	}

	/**
	 * Gets the (whitespace-trimmed) value of an input field (or anything else with a value parameter). For checkbox/radio elements, the value will be "on" or "off" depending on whether the element is checked or not.
	 *
	 * <p><code>
	 * | assert value | locator | value |
	 * </code></p>
	 * @param locator an element locator
	 * @param pattern expected pattern
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean assertValue(String locator, String pattern) {
		WebElement element = findElement(locator);
		String assertMessage = "Unexpected value for: " + element;
		if(StringUtils.equals(pattern, SeleniumFixture.SELECTABLE_ON_VALUE)) {
			Assert.assertTrue(assertMessage, element.isSelected());
		} else if(StringUtils.equals(pattern, SeleniumFixture.SELECTABLE_OFF_VALUE)) {
			Assert.assertFalse(assertMessage, element.isSelected());
		} else {
			Assert.assertEquals(assertMessage, StringUtils.stripToEmpty(pattern), StringUtils.stripToEmpty(element.getAttribute(SeleniumFixture.INPUT_VALUE_ATTRIBUTE)));
		}
		return true;
	}

	/**
	 * Selects element. Tries to emulate selenium IDE searching methods
	 * <ul>
	 *     <li>By id: 'id=&lt;id&gt;</li>'
	 *     <li>By css selector: 'css=#&lt;id&gt;</li>'
	 *     <li>By xpath selector: 'div[@id=#&lt;id&gt;]</li>'
	 * </ul>
 	 * @param locator an element locator
	 * @return webElementFound
	 */
	private WebElement findElement(String locator) {
		if(StringUtils.startsWith(locator, SeleniumFixture.ID_SELECTOR_PREFIX)) {
			return this.driver.findElement(By.id(StringUtils.removeStart(locator, SeleniumFixture.ID_SELECTOR_PREFIX)));
		}
		if(StringUtils.startsWith(locator, SeleniumFixture.CSS_SELECTOR_PREFIX)) {
			return this.driver.findElement(By.cssSelector(StringUtils.removeStart(locator, SeleniumFixture.CSS_SELECTOR_PREFIX)));
		}
		return this.driver.findElement(By.xpath(locator));
	}
}
