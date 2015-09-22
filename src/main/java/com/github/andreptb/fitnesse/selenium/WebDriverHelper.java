
package com.github.andreptb.fitnesse.selenium;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.reflections.Reflections;

import com.github.andreptb.fitnesse.selenium.SeleniumLocatorParser.WebElementSelector;
import com.github.andreptb.fitnesse.util.FitnesseMarkup;

/**
 * Utility class that wraps {@link WebDriver} with
 */
public class WebDriverHelper {

	private SeleniumLocatorParser parser = new SeleniumLocatorParser();
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();
	private WebDriverCapabilitiesHelper capabilitiesHelper = new WebDriverCapabilitiesHelper();
	private WebDriver driver;
	/**
	 * @see #setTimeoutInSeconds(int)
	 */
	private int timeoutInSeconds = 20;

	/**
	 * @see #getLastActionDurationInSeconds()
	 */
	private long lastActionDurationInSeconds;

	/**
	 * @see #setStopTestOnFirstFailure(boolean)
	 */
	private boolean stopTestOnFirstFailure;
	/**
	 * HTTP scheme prefix, to detect remote DRIVER
	 */
	private static final String HTTP_PREFIX = "http://";

	/**
	 * Creates a {@link WebDriver} instance with desired browser and capabilities. Capabilities should follow a key/value format
	 *
	 * @see WebDriverCapabilitiesHelper#parse(String, String, String)
	 * @param browser to be initialized. Can be a remote driver URL
	 * @param capabilities string. Should follow a key/value format
	 * @param preferences string. Should follow a key/value format
	 * @return webDriver instance
	 * @throws ReflectiveOperationException if remote driver class cannot be instantiated
	 * @throws IOException if IO error occurs if invalid URL is used when connecting to remote drivers
	 */
	public boolean connect(String browser, String capabilities, String preferences) throws ReflectiveOperationException, IOException {
		WebDriver driver = null;
		String cleanedBrowser = StringUtils.deleteWhitespace(this.fitnesseMarkup.clean(browser));
		Capabilities parsedCapabilities = this.capabilitiesHelper.parse(cleanedBrowser, this.fitnesseMarkup.clean(capabilities), this.fitnesseMarkup.clean(preferences));
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
		quit();
		this.driver = driver;
		return true;
	}

	/**
	 * Quietly quits the browser
	 *
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean quit() {
		try {
			this.driver.quit();
		} catch (Exception e) {
			// quits quietly
		}
		return true;
	}

	/**
	 * Invokes the callback ensuring the provision of a suitable driver to run selenium commands.
	 *
	 * @see #getWhenAvailable(String, BiFunction)
	 * @param consumer The callback to be invoked
	 * @return true if the callback runs successfully, false if browser is unavailable or callback fails to run
	 */
	public boolean doWhenAvailable(Function<WebDriver, Object> consumer) {
		return Boolean.valueOf(getWhenAvailable(consumer));
	}

	/**
	 * Invokes the callback ensuring the provision of a suitable driver to run selenium commands and
	 * the parsed selector.
	 * Will respect {@link #timeoutInSeconds} before returning negatively.
	 *
	 * @see #getWhenAvailable(String, BiFunction)
	 * @param from selenium selector received by the fixture
	 * @param consumer The callback to be invoked
	 * @return true if the callback runs successfully, false if browser is unavailable or callback fails to run
	 */
	public boolean doWhenAvailable(String from, BiConsumer<WebDriver, WebElementSelector> consumer) {
		return Boolean.valueOf(getWhenAvailable(from, (driver, locator) -> {
			consumer.accept(driver, locator);
			return true;
		}));
	}

	/**
	 * Invokes the callback ensuring the provision of a suitable driver to run selenium commands and
	 * the parsed selector.
	 * Will respect {@link #timeoutInSeconds} before returning negatively.
	 *
	 * @see #getWhenAvailable(String, BiFunction)
	 * @param function The callback to be invoked
	 * @return the value returned from the callback
	 */
	public String getWhenAvailable(Function<WebDriver, Object> function) {
		return getWhenAvailable(StringUtils.EMPTY, (waitingDriver, locator) -> function.apply(waitingDriver));
	}

	/**
	 * Core function designed to provide callbacks with selenium context necessary to evaluate commands. Applies
	 * the following rules:
	 * <ul>
	 * <li>Returns null if the browser is unavailable, see {@link #isBrowserAvailable()}</li>
	 * <li>Builds the context and passes the control to the callback (see {@link SeleniumLocatorParser#parse(String)})</li>
	 * <li>If the callback is unable to find a {@link WebElement} to run commmands, or fails for any other reason, the callback will be reinvoked until a positive return happens or
	 * {@link #getTimeoutInSeconds()} is reached</li>
	 * <li>If the callback returns positively and the result don't match with {@link WebElementSelector#getExpectedValue()}, the callback will be reinvoked until the value matches or
	 * {@link #getTimeoutInSeconds()} is reached</li>
	 * <li>If the callback returns positively and the result match with {@link WebElementSelector#getExpectedValue()} (or {@link WebElementSelector#getExpectedValue()} is empty), the result will be
	 * returned</li>
	 * </ul>
	 *
	 * @see #evaluate(WebDriver, WebElementSelector, BiFunction, boolean)
	 * @param from selenium selector received by the fixture@param from
	 * @param callback The callback to be invoked with {@link WebElementSelector} and {@link WebDriver}
	 * @return the value returned from the retriever
	 */
	public String getWhenAvailable(String from, BiFunction<WebDriver, WebElementSelector, Object> callback) {
		this.lastActionDurationInSeconds = NumberUtils.LONG_ZERO;
		if (!isBrowserAvailable()) {
			return null;
		}
		try {
			Instant startInstant = Instant.now();
			WebDriverWait wait = new WebDriverWait(this.driver, this.timeoutInSeconds);
			wait.ignoring(InvalidElementStateException.class);
			WebElementSelector locator = this.parser.parse(this.fitnesseMarkup.clean(from));
			try {
				return wait.until((ExpectedCondition<String>) waitingDriver -> evaluate(waitingDriver, locator, callback, false));
			} catch (TimeoutException e) {
				return evaluate(this.driver, locator, callback, true);
			} finally {
				this.lastActionDurationInSeconds = Duration.between(startInstant, Instant.now()).getSeconds();
			}
		} catch (RuntimeException e) {
			if (this.stopTestOnFirstFailure) {
				throw new StopTestCausedBySeleniumActionException(e);
			}
			throw e;
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

	/**
	 * @return if browser is available and can be used
	 */
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

	/**
	 * @param stopTestOnFirstFailure If true, if any error occurs while running selenium actions, test will be stopped.
	 */
	public void setStopTestOnFirstFailure(boolean stopTestOnFirstFailure) {
		this.stopTestOnFirstFailure = stopTestOnFirstFailure;
	}

	/**
	 * @return Seconds the last action took to complete
	 */
	public long getLastActionDurationInSeconds() {
		return this.lastActionDurationInSeconds;
	}

	/**
	 * {@link Exception} class so test can be stopped. See <a href="http://www.fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.WritingAcceptanceTests.SliM.SlimProtocol">FitNesse reference guide
	 * (Aborting a test) section</a>.
	 */
	static class StopTestCausedBySeleniumActionException extends RuntimeException {

		private StopTestCausedBySeleniumActionException(Throwable cause) {
			super(cause);
		}
	}

}
