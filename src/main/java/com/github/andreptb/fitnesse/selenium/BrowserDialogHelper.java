
package com.github.andreptb.fitnesse.selenium;

import java.util.function.BiFunction;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.github.andreptb.fitnesse.selenium.SeleniumLocatorParser.ByFocus;
import com.github.andreptb.fitnesse.selenium.SeleniumLocatorParser.WebElementSelector;
import com.github.andreptb.fitnesse.util.FitnesseMarkup;

/**
 * Utility class to handle browser native dialogs, such as confirm and alert.
 */
public class BrowserDialogHelper {

	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	/**
	 * Enum holding all possible identifiers used to manipulate browser dialog
	 */
	private enum DialogIdentifier {
		dialog,
		alert,
		confirm,
		cancel;
	}

	/**
	 * Will try to click a browser dialog button in the following conditions:
	 * <ul>
	 * <li>If locator is any of: 'dialog', 'alert', 'dialog=confirm' or 'dialog=cancel'. If there is no alert present {@link NoAlertPresentException} will be thrown</li>
	 * <li>If locator is {@link ByFocus} and there is an alert present. If dialog contains 'confirm' and 'cancel' buttons, 'confirm' will be clicked</li>
	 * </ul>
	 *
	 * @param driver instance of {@link WebDriver} to manipulate dialog
	 * @param parsedLocator instance of {@link WebElementSelector} containing locator context
	 * @return true if dialog was clicked.
	 * @throws NoAlertPresentException if locator prefix is 'dialog' or 'alert' and there is no alert present
	 */
	public boolean click(WebDriver driver, WebElementSelector parsedLocator) {
		return BooleanUtils.isTrue(doIfAvailable(driver, parsedLocator, (alert, value) -> {
			DialogIdentifier action = StringUtils.isBlank(value) ? DialogIdentifier.confirm : DialogIdentifier.valueOf(value);
			if (action == DialogIdentifier.confirm) {
				alert.accept();
			} else if (action == DialogIdentifier.cancel) {
				alert.dismiss();
			}
			return true;
		}));
	}

	public String text(WebDriver driver, WebElementSelector parsedLocator) {
		return doIfAvailable(driver, parsedLocator, (alert, action) -> alert.getText());
	}

	public boolean present(WebDriver driver, WebElementSelector parsedLocator) {
		return BooleanUtils.isTrue(doIfAvailable(driver, parsedLocator, (alert, action) -> true));
	}

	private <T> T doIfAvailable(WebDriver driver, WebElementSelector parsedLocator, BiFunction<Alert, String, T> callback) {
		Pair<String, String> prefixAndLocator = this.fitnesseMarkup.cleanAndParseKeyValue(parsedLocator.getOriginalSelector(), FitnesseMarkup.KEY_VALUE_SEPARATOR);
		DialogIdentifier selectorType = EnumUtils.getEnum(DialogIdentifier.class, prefixAndLocator.getKey());
		if (selectorType != null) {
			return callback.apply(driver.switchTo().alert(), prefixAndLocator.getValue());
		}
		if (!ClassUtils.isAssignable(parsedLocator.getBy().getClass(), ByFocus.class)) {
			return null;
		}
		Alert alert = ExpectedConditions.alertIsPresent().apply(driver);
		if (alert == null) {
			return null;
		}
		return callback.apply(alert, prefixAndLocator.getValue());
	}

}
