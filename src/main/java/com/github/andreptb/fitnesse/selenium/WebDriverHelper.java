
package com.github.andreptb.fitnesse.selenium;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.ScreenshotException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.UnexpectedTagNameException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.reflections.Reflections;

import com.github.andreptb.fitnesse.selenium.SeleniumLocatorParser.WebElementSelector;
import com.github.andreptb.fitnesse.util.FitnesseMarkup;

/**
 * Utility class that wraps {@link WebDriver} instances. Each {@link #connect(String, String, String)} call
 * will associate a working instance of {@link WebDriver} and will be used until {@link #quit()} is used or another {@link #connect(String, String, String)}
 */
public class WebDriverHelper {

	/**
	 * HTTP scheme prefix, to detect remote DRIVER
	 */
	private static final String HTTP_PREFIX = "http";

	private static final String UNDEFINED_VALUE = "<<undefined_value>>";

	private Logger logger = Logger.getLogger(WebDriverHelper.class.getName());
	private SeleniumLocatorParser parser = new SeleniumLocatorParser();
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();
	private WebDriverCapabilitiesHelper capabilitiesHelper = new WebDriverCapabilitiesHelper();
	private Map<Integer, WebDriver> driverCache = new LinkedHashMap<>();
	private Integer currentDriverId;
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
	 * @see #setTakeScreenshotOnFailure(boolean)
	 */
	private boolean takeScreenshotOnFailure = true;

	private String dryRunWindow;

	/**
	 * Creates a {@link WebDriver} instance with desired browser and capabilities. Capabilities should follow a key/value format
	 *
	 * @see WebDriverCapabilitiesHelper#parse(String, String, String)
	 * @param browser to be initialized. Can be a remote driver URL
	 * @param capabilities string. Should follow a key/value format
	 * @param preferences string. Should follow a key/value format
	 * @throws ReflectiveOperationException if remote driver class cannot be instantiated
	 * @throws IOException if IO error occurs if invalid URL is used when connecting to remote drivers
	 */
	public void connect(String browser, String capabilities, String preferences) throws ReflectiveOperationException, IOException {
		int driverId = new HashCodeBuilder().append(browser).append(capabilities).append(preferences).toHashCode();
		WebDriver driver = this.driverCache.get(driverId);
		if (isBrowserAvailable(driver)) {
			return;
		}
		quit(driverId);
		this.driverCache.put(driverId, createDriverConnection(browser, capabilities, preferences));
		this.currentDriverId = driverId;
	}

	private WebDriver createDriverConnection(String browser, String capabilities, String preferences) throws MalformedURLException, ReflectiveOperationException {
		WebDriver driver = null;
		String cleanedBrowser = StringUtils.deleteWhitespace(this.parser.parse(browser).getOriginalSelector());
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
			throw new StopTestWithWebDriverException(MessageFormat.format("No suitable implementation found for [{0}] with capabilites: [{1}]", browser, capabilities));
		}
		return driver;
	}

	/**
	 * Quietly quits the current browser instance
	 */
	public void quit() {
		if (quit(this.currentDriverId) && MapUtils.isNotEmpty(this.driverCache)) {
			this.currentDriverId = this.driverCache.keySet().stream().findFirst().get();
		}
	}

	private boolean quit(Integer driverId) {
		try {
			this.driverCache.remove(driverId).quit();
			return true;
		} catch (Exception e) {
			// quits quietly
		}
		return false;
	}

	public boolean doWhenAvailable(String from, BiConsumer<WebDriver, WebElementSelector> callback) {
		getWhenAvailable(from, (driver, selector) -> {
			callback.accept(driver, selector);
			return StringUtils.stripToNull(selector.getExpectedValue());
		});
		return true;
	}

	private String respondForDryRun(WebDriver driver, WebElementSelector locator) {
		String currentWindow = driver.getWindowHandle();
		By selector = locator.getBy();
		if (selector != null) {
			if (!StringUtils.equals(currentWindow, this.dryRunWindow)) {
				driver.switchTo().window(this.dryRunWindow);
			}
			try {
				driver.findElement(locator.getBy());
			} catch (NoSuchElementException e) {
				// element not found means that selenium is running properly
			}
		}
		String expectedValue = locator.getExpectedValue();
		if(StringUtils.startsWith(expectedValue, FitnesseMarkup.SELECTOR_VALUE_DENY_INDICATOR)) {
			return WebDriverHelper.UNDEFINED_VALUE;
		}
		return expectedValue;
	}

	/**
	 * Core function designed to provide callbacks with selenium context necessary to evaluate commands. Applies
	 * the following rules:
	 * <ul>
	 * <li>Builds the context and passes the control to the callback (see {@link SeleniumLocatorParser#parse(String)})</li>
	 * <li>If the callback is unable to find a {@link WebElement} to run commmands, or fails for any other reason, the callback will be reinvoked until a positive return happens or
	 * {@link #getTimeoutInSeconds()} is reached</li>
	 * <li>If the callback returns positively and the result don't match with {@link WebElementSelector#getExpectedValue()}, the callback will be reinvoked until the value matches or
	 * {@link #getTimeoutInSeconds()} is reached</li>
	 * <li>If the callback returns positively and the result match with {@link WebElementSelector#getExpectedValue()} (or {@link WebElementSelector#getExpectedValue()} is empty), the result will be
	 * returned</li>
	 * </ul>
	 *
	 * @param from selenium selector received by the fixture@param from
	 * @param callback The callback to be invoked with {@link WebElementSelector} and {@link WebDriver}
	 * @return the value returned from the callback
	 * @throws StopTestWithWebDriverException if {@link #isBrowserAvailable()} returns false or if {@link #getStopTestOnFirstFailure()} is true and any failure occurs
	 */
	public String getWhenAvailable(String from, BiFunction<WebDriver, WebElementSelector, String> callback) {
		this.lastActionDurationInSeconds = NumberUtils.LONG_ZERO;
		WebElementSelector locator = this.parser.parse(this.fitnesseMarkup.clean(from));
		WebDriver driver = this.driverCache.get(this.currentDriverId);
		if (!isBrowserAvailable()) {
			throw new StopTestWithWebDriverException("No browser instance available, please check if 'start browser' command completed successfuly");
		}
		MutableObject<String> result = new MutableObject<>();
		try {
			if (StringUtils.isNotBlank(this.dryRunWindow)) {
				return respondForDryRun(driver, locator);
			}
			Instant startInstant = Instant.now();
			WebDriverWait wait = new WebDriverWait(driver, this.timeoutInSeconds);
			wait.ignoring(InvalidElementStateException.class);
			wait.ignoring(UnhandledAlertException.class);
			wait.ignoring(UnexpectedTagNameException.class);
			try {
				wait.until((ExpectedCondition<String>) waitingDriver -> {
					evaluate(waitingDriver, locator, callback, false, result);
					return result.getValue();
				});
			} catch (TimeoutException e) {
				if (this.stopTestOnFirstFailure) {
					throw e;
				}
				evaluate(driver, locator, callback, true, result);
			} finally {
				this.lastActionDurationInSeconds = Duration.between(startInstant, Instant.now()).getSeconds();
			}
		} catch (RuntimeException e) {
			throw handleSeleniumException(e, driver);
		}
		return result.getValue();
	}

	private RuntimeException handleSeleniumException(RuntimeException originalException, WebDriver driver) {

		String screenshotData = retrieveScreenshotPathFromException(originalException, driver);
		Throwable cause = Optional.ofNullable(ExceptionUtils.getRootCause(originalException)).orElse(originalException);
		String exceptionMessage = this.fitnesseMarkup.exceptionMessage(StringUtils.substringBefore(cause.getMessage(), StringUtils.LF), screenshotData);
		this.logger.log(Level.INFO, exceptionMessage, cause);
		try {
			Throwable convertedException = this.stopTestOnFirstFailure ? new StopTestWithWebDriverException(exceptionMessage, cause) : cause.getClass().getConstructor(String.class).newInstance(exceptionMessage);
			convertedException.setStackTrace(cause.getStackTrace());
			return (RuntimeException) convertedException;
		} catch (Exception e) {
			this.logger.log(Level.FINE, "Failed to handle selenium failure response", e);
		}
		return originalException;
	}

	private String retrieveScreenshotPathFromException(Throwable originalException, WebDriver driver) {
		if (!this.takeScreenshotOnFailure) {
			return StringUtils.EMPTY;
		}
		try {
			if (originalException instanceof ScreenshotException) {
				return ((ScreenshotException) originalException).getBase64EncodedScreenshot();
			} else if (driver instanceof TakesScreenshot) {
				return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
			}
		} catch (Exception se) {
			this.logger.log(Level.FINE, "Failed to retrieve screenshot after failure", se);
		}
		return StringUtils.EMPTY;
	}

	private void evaluate(WebDriver driver, WebElementSelector locator, BiFunction<WebDriver, WebElementSelector, String> callback, boolean disableValueCheck, MutableObject<String> resultHolder) {
		String result = StringUtils.stripToEmpty(callback.apply(driver, locator));
		resultHolder.setValue(result);
		String expectedValue = locator.getExpectedValue();
		if (disableValueCheck || StringUtils.isBlank(expectedValue) || this.fitnesseMarkup.compare(expectedValue, result)) {
			return;
		}
		throw new NoSuchElementException(MessageFormat.format("Element with unexpected value [Expected: {0}, Obtained: {1}]", expectedValue, result));
	}

	/**
	 * @return if browser is available and can be used
	 */
	public boolean isBrowserAvailable() {
		return isBrowserAvailable(this.driverCache.get(this.currentDriverId));
	}

	private boolean isBrowserAvailable(WebDriver driver) {
		// http://stackoverflow.com/questions/27616470/webdriver-how-to-check-if-browser-still-exists-or-still-open
		String driverString = ObjectUtils.toString(driver);
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

	public boolean getStopTestOnFirstFailure() {
		return this.stopTestOnFirstFailure;
	}

	/**
	 * @return Seconds the last action took to complete
	 */
	public long getLastActionDurationInSeconds() {
		return this.lastActionDurationInSeconds;
	}

	public boolean getTakeScreenshotOnFailure() {
		return this.takeScreenshotOnFailure;
	}

	/**
	 * @param takeScreenshotOnFailure If true, will embed exceptions with screenshot data (if available). Default is <code>true</code>
	 */
	public void setTakeScreenshotOnFailure(boolean takeScreenshotOnFailure) {
		this.takeScreenshotOnFailure = takeScreenshotOnFailure;
	}

	public String getDryRunWindow() {
		return this.dryRunWindow;
	}

	public void setDryRunWindow(String dryRunWindow) {
		this.dryRunWindow = dryRunWindow;
	}

	/**
	 * {@link Exception} class so test can be stopped. See <a href="http://www.fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.WritingAcceptanceTests.SliM.SlimProtocol">FitNesse reference guide
	 * (Aborting a test) section</a>.
	 */
	public static class StopTestWithWebDriverException extends RuntimeException {

		public StopTestWithWebDriverException(String message, Throwable cause) {
			super(message);
		}

		public StopTestWithWebDriverException(String message) {
			super(message);
		}

	}

}
