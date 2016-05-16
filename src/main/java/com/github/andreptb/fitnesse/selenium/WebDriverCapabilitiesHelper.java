
package com.github.andreptb.fitnesse.selenium;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * Utility class to produce browser driver capabilities. Parses and inject default capabilities commonly used in test environments
 */
public class WebDriverCapabilitiesHelper {

	private static final String FIREFOX_ALLOWED_DOWNLOAD_CONTENT_TYPES = "application/msword, application/csv, application/ris, text/csv, image/png, application/pdf, text/html, text/plain, application/zip, application/x-zip, application/x-zip-compressed, application/download, application/octet-stream";
	/**
	 * Pattern to parse capability string. Expected format: key='value' or key="value"
	 */
	private static final Pattern ENCODED_CONFIG_PATTERN = Pattern.compile("\\s*([^=]+)=['\"]([^'\"]+)['\"]");

	/**
	 * Enum to inject preferences and capabilities according to the browser. Will inject default preferences or capabilities if applicable
	 */
	private enum CapabilitiesAndPreferencesInjector {
		chrome((capabilities, preferences) -> {
			ChromeOptions chromeOptions = new ChromeOptions();
			CapabilitiesAndPreferencesInjector.applyIfUndefined("disable-popup-blocking", Boolean.TRUE.toString(), preferences::getOrDefault, preferences::put);
			chromeOptions.setExperimentalOption("prefs", preferences);
			capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
		}),
		firefox((capabilities, preferences) -> {
			FirefoxProfile firefoxProfile = new FirefoxProfile();
			firefoxProfile.setAcceptUntrustedCertificates(true);
			preferences.forEach((key, value) -> firefoxProfile.setPreference(key, value));
			CapabilitiesAndPreferencesInjector.applyIfUndefined("browser.download.folderList", 2, firefoxProfile::getIntegerPreference, firefoxProfile::setPreference);
			CapabilitiesAndPreferencesInjector.applyIfUndefined("plugin.state.java", 2, firefoxProfile::getIntegerPreference, firefoxProfile::setPreference);
			CapabilitiesAndPreferencesInjector.applyIfUndefined("security.enable_java", true, firefoxProfile::getBooleanPreference, firefoxProfile::setPreference);
			CapabilitiesAndPreferencesInjector.applyIfUndefined("browser.helperApps.neverAsk.saveToDisk", WebDriverCapabilitiesHelper.FIREFOX_ALLOWED_DOWNLOAD_CONTENT_TYPES, firefoxProfile::getStringPreference, firefoxProfile::setPreference);
			capabilities.setCapability(FirefoxDriver.PROFILE, firefoxProfile);
		}),
		internetexplorer((capabilities, preferences) -> {
			capabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
		});

		private BiConsumer<DesiredCapabilities, Map<String, String>> injector;

		private CapabilitiesAndPreferencesInjector(BiConsumer<DesiredCapabilities, Map<String, String>> injector) {
			this.injector = injector;
		}

		private static <K, V> void applyIfUndefined(K key, V defaultValue, BiFunction<K, V, V> configProvider, BiConsumer<K, V> configApplier) {
			if (StringUtils.isBlank(Objects.toString(defaultValue, null))) {
				return;
			}
			configApplier.accept(key, configProvider.apply(key, defaultValue));
		}
	}

	/**
	 * Creates {@link Capabilities} from string. Injects default preferences to supported browsers
	 * Supported formats:
	 * <p>
	 * key1='value1' key2='value with space 2' key3='value3'
	 * </p>
	 * <p>
	 * key1="value1" key2="value with space 2" key3="value3"
	 * </p>
	 *
	 * @see CapabilitiesAndPreferencesInjector
	 * @param browser Used to determine default configurations to inject
	 * @param capabilities {@link String}
	 * @param preferences Directory to save browser downloaded files
	 * @return capabilitiesInstance which is an instanceof {@link Capabilities}
	 */
	public DesiredCapabilities parse(String browser, String capabilities, String preferences) {
		DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
		parseFromString(capabilities, desiredCapabilities::setCapability);
		Map<String, String> parsedPreferences = new HashMap<>();
		parseFromString(capabilities, desiredCapabilities::setCapability);
		parseFromString(preferences, parsedPreferences::put);
		CapabilitiesAndPreferencesInjector entry = Optional.ofNullable(EnumUtils.getEnum(CapabilitiesAndPreferencesInjector.class, browser))
			.orElse(EnumUtils.getEnum(CapabilitiesAndPreferencesInjector.class, StringUtils.deleteWhitespace(desiredCapabilities.getBrowserName())));
		if (entry != null) {
			entry.injector.accept(desiredCapabilities, parsedPreferences);
		}
		return desiredCapabilities;
	}

	private <K, V> void parseFromString(String encodedConfig, BiConsumer<String, String> applier) {
		if (StringUtils.isBlank(encodedConfig)) {
			return;
		}
		Matcher matcher = WebDriverCapabilitiesHelper.ENCODED_CONFIG_PATTERN.matcher(encodedConfig);
		while (matcher.find()) {
			String value = matcher.group(2);
			if (StringUtils.isNotBlank(value)) {
				applier.accept(matcher.group(NumberUtils.INTEGER_ONE), value);
			}
		}
	}
}
