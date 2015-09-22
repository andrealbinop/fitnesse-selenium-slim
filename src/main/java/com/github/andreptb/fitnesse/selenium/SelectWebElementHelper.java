
package com.github.andreptb.fitnesse.selenium;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.support.ui.Select;

import com.github.andreptb.fitnesse.util.FitnesseMarkup;

/**
 * Utility class designed to wrap infrastructure code necessary to operate with selenium {@link Select} API.
 */
public class SelectWebElementHelper {

	/**
	 * HTML input value attribute constant
	 */
	private static final String INPUT_VALUE_ATTRIBUTE = "value";
	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	/**
	 * enum mapping option locator identifier.
	 */
	private enum OptionSelectorType {
		/**
		 * Label selector, will look for the text of the option element
		 */
		label((select, value) -> select.selectByVisibleText(value), select -> select.getFirstSelectedOption().getText()),
		/**
		 * Value selector, will look for the value attribute of the option element
		 */
		value((select, value) -> select.selectByValue(value), select -> select.getFirstSelectedOption().getAttribute(SelectWebElementHelper.INPUT_VALUE_ATTRIBUTE)),
		/**
		 * Value selector, will look for the index of the desired option element
		 */
		index((select, value) -> select.selectByIndex(NumberUtils.toInt(value)), select -> select.getOptions().indexOf(select.getFirstSelectedOption()));

		/**
		 * Function that selects a value in a {@link Select} element
		 */
		private BiConsumer<Select, String> selector;
		/**
		 * Function that retrieves a value from a {@link Select} element
		 */
		private Function<Select, Object> retriever;

		private OptionSelectorType(BiConsumer<Select, String> selector, Function<Select, Object> retriever) {
			this.selector = selector;
			this.retriever = retriever;
		}
	}

	/**
	 * Selects a option of a select element.
	 *
	 * @param driverHelper API that will be used for selenium task execution
	 * @param optionLocator expected to be [type]=value, see {@link OptionSelectorType} for possible types.
	 * @param locator an element locator
	 * @return result Boolean result indication of assertion/operation
	 */
	public boolean select(WebDriverHelper driverHelper, String optionLocator, String locator) {
		String cleanedOptionLocator = this.fitnesseMarkup.clean(optionLocator);
		OptionSelectorType option = parseOptionType(cleanedOptionLocator);
		return driverHelper.doWhenAvailable(locator, (driver, parsedLocator) -> {
			option.selector.accept(new Select(driver.findElement(parsedLocator.getBy())), StringUtils.removeStart(cleanedOptionLocator, option + SeleniumLocatorParser.SELECTOR_TYPE_SEPARATOR));
		});
	}

	/**
	 * Retrieves information from the current selected value in a select element.
	 *
	 * @param driverHelper API that will be used for selenium task execution
	 * @param optionType see {@link OptionSelectorType} for possible types.
	 * @param locator an element locator
	 * @return the information from the current selected element
	 */
	public String selected(WebDriverHelper driverHelper, String optionType, String locator) {
		// value injection fix
		Pair<String, String> optionTypeAndLocatorWithExpectedValue = this.fitnesseMarkup.swapValueToCheck(optionType, locator);
		OptionSelectorType option = parseOptionType(this.fitnesseMarkup.clean(optionTypeAndLocatorWithExpectedValue.getLeft()));
		return driverHelper.getWhenAvailable(optionTypeAndLocatorWithExpectedValue.getRight(), (driver, parsedLocator) -> this.fitnesseMarkup.clean(option.retriever.apply(new Select(driver.findElement(parsedLocator.getBy())))));
	}

	private OptionSelectorType parseOptionType(String optionType) {
		return Optional.ofNullable(EnumUtils.getEnum(OptionSelectorType.class, StringUtils.substringBefore(optionType, SeleniumLocatorParser.SELECTOR_TYPE_SEPARATOR))).orElse(OptionSelectorType.label);
	}
}
