
package com.github.andreptb.fitnesse.selenium;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

import com.github.andreptb.fitnesse.util.FitnesseMarkup;

/**
 * Utility class designed to wrap infrastructure code necessary to operate with selenium frame API.
 */
public class FrameWebElementHelper {

	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	/**
	 * Enum mapping possible frame relative positions
	 */
	private enum FrameRelativeSelectorType {
		top,
		parent;
	}

	/**
	 * Enum mapping frame selectors
	 */
	private enum FrameSelectorType {
		/**
		 * Relative selector, will look for siblings or parent frame
		 */
		relative((driver, value) -> {
			FrameRelativeSelectorType relativeSelector = FrameRelativeSelectorType.valueOf(value);
			if (relativeSelector == FrameRelativeSelectorType.top) {
				driver.switchTo().defaultContent();
			} else if (relativeSelector == FrameRelativeSelectorType.parent) {
				driver.switchTo().parentFrame();
			}
		}),
		/**
		 * Index selector, will look for the index of the desired frame
		 */
		index((driver, value) -> driver.switchTo().frame(NumberUtils.toInt(value)));

		/**
		 * Function that selects a value in a {@link Select} element
		 */
		private BiConsumer<WebDriver, String> selector;

		private FrameSelectorType(BiConsumer<WebDriver, String> selector) {
			this.selector = selector;
		}
	}

	public boolean select(WebDriverHelper driverHelper, String locator) {
		return driverHelper.doWhenAvailable(locator, (driver, parsedLocator) -> {
			Pair<String, String> keyValue = this.fitnesseMarkup.cleanAndParseKeyValue(parsedLocator.getOriginalSelector(), FitnesseMarkup.KEY_VALUE_SEPARATOR);
			FrameSelectorType frameSelector = EnumUtils.getEnum(FrameSelectorType.class, keyValue.getKey());
			if (frameSelector == null) {
				driver.switchTo().frame(driver.findElement(parsedLocator.getBy()));
				return;
			}
			frameSelector.selector.accept(driver, keyValue.getValue());
		});
	}
}
