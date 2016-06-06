
package com.github.andreptb.fitnesse.selenium;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.EnumUtils;
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
		Pair<String, String> optionTypeAndLocatorWithExpectedValue = this.fitnesseMarkup.swapValueToCheck(optionLocator, locator);
		Pair<OptionSelectorType, String> parsedOptionLocator = parseOptionLocator(optionTypeAndLocatorWithExpectedValue.getKey());
		return driverHelper.doWhenAvailable(optionTypeAndLocatorWithExpectedValue.getValue(), (driver, parsedLocator) -> {
			parsedOptionLocator.getKey().selector.accept(new Select(driver.findElement(parsedLocator.getBy())), parsedOptionLocator.getValue());
		});
	}

	/**
	 * Retrieves information from the current selected value in a select element.
	 *
	 * @param driverHelper API that will be used for selenium task execution
	 * @param optionLocator see {@link OptionSelectorType} for possible types.
	 * @param locator an element locator
	 * @return the information from the current selected element
	 */
	public String selected(WebDriverHelper driverHelper, String optionLocator, String locator) {
		Pair<String, String> optionTypeAndLocatorWithExpectedValue = this.fitnesseMarkup.swapValueToCheck(optionLocator, locator);
		OptionSelectorType optionRetriever = parseOptionLocator(optionTypeAndLocatorWithExpectedValue.getKey()).getKey();
		return driverHelper.getWhenAvailable(optionTypeAndLocatorWithExpectedValue.getValue(), (driver, parsedLocator) -> this.fitnesseMarkup.clean(optionRetriever.retriever.apply(new Select(driver.findElement(parsedLocator.getBy())))));
	}

	private Pair<OptionSelectorType, String> parseOptionLocator(String optionLocator) {
		Pair<String, String> keyValue = this.fitnesseMarkup.cleanAndParseKeyValue(optionLocator, FitnesseMarkup.KEY_VALUE_SEPARATOR);
		// if no type is informed value will be parsed as prefix
		String prefix = keyValue.getKey();
		String value = StringUtils.defaultIfBlank(keyValue.getValue(), prefix);
		return Pair.of(Optional.ofNullable(EnumUtils.getEnum(OptionSelectorType.class, prefix)).orElse(OptionSelectorType.label), value);
	}
}
