
package com.github.andreptb.fitnesse.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.reflections.Reflections;

/**
 * Utility to create {@link WebDriver} instance from FitNesse wiki.
 */
public class FixtureWebDriverProvider {

	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	/**
	 * HTTP scheme prefix, to detect remote DRIVER
	 */
	private static final String HTTP_PREFIX = "http://";
	/**
	 * Pattern to parse capability string. Expected format: key='value' or key="value"
	 */
	private static final Pattern CAPABILITY_PATTERN = Pattern.compile("(\\w+)=['\"]([^'\"]+)['\"]");

	/**
	 * Creates {@link WebDriver} instance with desired browser and capabilities. Capabilities should follow a key/value format
	 *
	 * @see #parseCapabilities(String)
	 * @param browser to be initialized. Can be a remote driver URL
	 * @param capabilities string. Should follow a key/value format
	 * @return webDriver instance
	 * @throws MalformedURLException if the remote driver has a malformed URL
	 * @throws ReflectiveOperationException if remote driver class cannot be instantiated
	 */
	public WebDriver createDriver(String browser, String capabilities) throws MalformedURLException, ReflectiveOperationException {
		String cleanedBrowser = this.fitnesseMarkup.clean(browser);
		Capabilities parsedCapabilities = parseCapabilities(capabilities);
		if (StringUtils.startsWithIgnoreCase(cleanedBrowser, FixtureWebDriverProvider.HTTP_PREFIX)) {
			return new RemoteWebDriver(new URL(cleanedBrowser), parsedCapabilities);
		}
		Reflections reflections = new Reflections(WebDriver.class.getPackage().getName());
		for (Class<? extends WebDriver> availableDriver : reflections.getSubTypesOf(WebDriver.class)) {
			if (!StringUtils.startsWithIgnoreCase(availableDriver.getSimpleName(), cleanedBrowser)) {
				continue;
			}
			return availableDriver.getConstructor(Capabilities.class).newInstance(parsedCapabilities);
		}
		return null;
	}

	/**
	 * Creates {@link Capabilities} from string.
	 * Supported formats:
	 * <p>
	 * key1='value1' key2='value with space 2' key3='value3'
	 * </p>
	 * <p>
	 * key1="value1" key2="value with space 2" key3="value3"
	 * </p>
	 *
	 * @see FitnesseMarkup#clean(Object)
	 * @param capabilities {@link String}
	 * @return capabilitiesInstance which is an instanceof {@link Capabilities}
	 */
	protected Capabilities parseCapabilities(String capabilities) {
		DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
		String cleanedCapabilities = this.fitnesseMarkup.clean(capabilities);
		if (StringUtils.isBlank(cleanedCapabilities)) {
			return desiredCapabilities;
		}
		Matcher matcher = FixtureWebDriverProvider.CAPABILITY_PATTERN.matcher(cleanedCapabilities);
		while (matcher.find()) {
			String value = matcher.group(2);
			if (StringUtils.isNotBlank(value)) {
				desiredCapabilities.setCapability(matcher.group(1), value);
			}
		}
		return desiredCapabilities;
	}
}
