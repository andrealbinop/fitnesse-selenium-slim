
package com.github.andreptb.fitnesse.selenium;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.reflections.Reflections;

import com.github.andreptb.fitnesse.selenium.SeleniumLocatorParser.WebElementSelector;
import com.github.andreptb.fitnesse.util.FitnesseMarkup;

public class WebDriverHelper {

	private SeleniumLocatorParser parser = new SeleniumLocatorParser();
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	private WebDriver driver;
	/**
	 * @see #setTimeoutInSeconds(int)
	 */
	private int timeoutInSeconds = 20;

	private long lastActionDurationInSeconds;

	/**
	 * HTTP scheme prefix, to detect remote DRIVER
	 */
	private static final String HTTP_PREFIX = "http://";
	/**
	 * Pattern to parse capability string. Expected format: key='value' or key="value"
	 */
	private static final Pattern CAPABILITY_PATTERN = Pattern.compile("(\\w+)=['\"]([^'\"]+)['\"]");

	/**
	 * Creates a {@link WebDriver} instance with desired browser and capabilities. Capabilities should follow a key/value format
	 *
	 * @see #parseCapabilities(String)
	 * @param browser to be initialized. Can be a remote driver URL
	 * @param capabilities string. Should follow a key/value format
	 * @return webDriver instance
	 * @throws MalformedURLException if the remote driver has a malformed URL
	 * @throws ReflectiveOperationException if remote driver class cannot be instantiated
	 */
	public boolean connect(String browser, String capabilities) throws MalformedURLException, ReflectiveOperationException {
		WebDriver driver = null;
		String cleanedBrowser = this.fitnesseMarkup.clean(browser);
		Capabilities parsedCapabilities = parseCapabilities(capabilities);
		if (StringUtils.startsWithIgnoreCase(cleanedBrowser, WebDriverHelper.HTTP_PREFIX)) {
			driver = new RemoteWebDriver(new URL(cleanedBrowser), parsedCapabilities);
		} else {
			Reflections reflections = new Reflections(WebDriver.class.getPackage().getName());
			for (Class<? extends WebDriver> availableDriver : reflections.getSubTypesOf(WebDriver.class)) {
				if (StringUtils.startsWithIgnoreCase(availableDriver.getSimpleName(), cleanedBrowser)) {
					driver = availableDriver.getConstructor(Capabilities.class).newInstance(parsedCapabilities);
					break;
				}
			}
		}
		if (driver == null) {
			throw new WebDriverException(MessageFormat.format("No suitable implementation found for {0} with capabilites: [{1}]", browser, capabilities));
		}
		if (this.driver != null) {
			this.driver.quit();
		}
		this.driver = driver;
		return true;
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
	private Capabilities parseCapabilities(String capabilities) {
		DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
		String cleanedCapabilities = this.fitnesseMarkup.clean(capabilities);
		if (StringUtils.isBlank(cleanedCapabilities)) {
			return desiredCapabilities;
		}
		Matcher matcher = WebDriverHelper.CAPABILITY_PATTERN.matcher(cleanedCapabilities);
		while (matcher.find()) {
			String value = matcher.group(2);
			if (StringUtils.isNotBlank(value)) {
				desiredCapabilities.setCapability(matcher.group(1), value);
			}
		}
		return desiredCapabilities;
	}

	public boolean whenAvailable(Function<WebDriver, Object> consumer) {
		return Boolean.valueOf(getWhenAvailable(consumer));
	}

	public String getWhenAvailable(Function<WebDriver, Object> consumer) {
		if (isBrowserAvailable()) {
			return Objects.toString(consumer.apply(this.driver), null);
		}
		return null;
	}

	public boolean whenAvailable(String from, BiConsumer<WebDriver, WebElementSelector> consumer) {
		return Boolean.valueOf(getWhenAvailable(from, (driver, locator) -> {
			consumer.accept(driver, locator);
			return true;
		}));
	}

	public String getWhenAvailable(String from, BiFunction<WebDriver, WebElementSelector, Object> retriever) {
		this.lastActionDurationInSeconds = NumberUtils.LONG_ZERO;
		if (!isBrowserAvailable()) {
			return null;
		}
		Instant startInstant = Instant.now();
		WebDriverWait wait = new WebDriverWait(this.driver, this.timeoutInSeconds);
		wait.ignoring(InvalidElementStateException.class);
		WebElementSelector locator = this.parser.parse(this.fitnesseMarkup.clean(from));
		try {
			return wait.until((ExpectedCondition<String>) waitingDriver -> evaluate(waitingDriver, locator, retriever, false));
		} catch (TimeoutException e) {
			return evaluate(this.driver, locator, retriever, true);
		} finally {
			this.lastActionDurationInSeconds = Duration.between(startInstant, Instant.now()).getSeconds();
		}
	}

	private String evaluate(WebDriver driver, WebElementSelector locator, BiFunction<WebDriver, WebElementSelector, Object> retriever, boolean disableValueCheck) {
		Object result = retriever.apply(driver, locator);
		String expectedValue = locator.getExpectedValue();
		if (disableValueCheck || StringUtils.isBlank(expectedValue) || this.fitnesseMarkup.compare(expectedValue, result)) {
			return Objects.toString(result, null);
		}
		throw new NoSuchElementException(MessageFormat.format("Element with unexpected value [Expected: {0}, Obtained: {1}]", expectedValue, result));
	}

	public boolean isBrowserAvailable() {
		// http://stackoverflow.com/questions/27616470/webdriver-how-to-check-if-browser-still-exists-or-still-open
		String driverString = ObjectUtils.toString(this.driver);
		return StringUtils.isNotBlank(driverString) && !StringUtils.containsIgnoreCase(driverString, "null");
	}

	/**
	 * @param timeoutInSeconds Timeout to wait for elements to be present. Default is 20 seconds
	 */
	public void setTimeoutInSeconds(int timeoutInSeconds) {
		this.timeoutInSeconds = timeoutInSeconds;
	}

	/**
	 * @return Timeout to wait for elements to be present. Default is 20 seconds
	 */
	public int getTimeoutInSeconds() {
		return this.timeoutInSeconds;
	}

	public long getLastActionDurationInSeconds() {
		return this.lastActionDurationInSeconds;
	}

}
