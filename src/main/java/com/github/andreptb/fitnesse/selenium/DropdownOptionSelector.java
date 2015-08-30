package com.github.andreptb.fitnesse.selenium;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import com.github.andreptb.fitnesse.util.FitnesseMarkup;

public class DropdownOptionSelector {

	/**
	 * HTML input value attribute constant
	 */
	private static final String INPUT_VALUE_ATTRIBUTE = "value";

	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	/**
	 * enum mapping option locator identifier
	 */
	public enum OptionSelectorType {
		label((select, value) -> select.selectByVisibleText(value), select -> select.getFirstSelectedOption().getText()),
		value((select, value) -> select.selectByValue(value), select -> select.getFirstSelectedOption().getAttribute(DropdownOptionSelector.INPUT_VALUE_ATTRIBUTE)),
		index((select, value) -> select.selectByIndex(NumberUtils.toInt(value)), select -> select.getOptions().indexOf(select.getFirstSelectedOption()));

		private BiConsumer<Select, String> selector;
		private Function<Select, Object> retriever;

		private OptionSelectorType(BiConsumer<Select, String> selector, Function<Select, Object> retriever) {
			this.selector = selector;
			this.retriever = retriever;
		}
	}

	public void select(WebElement element, String optionLocator) {
		String cleanedOptionLocator = this.fitnesseMarkup.clean(optionLocator);
		OptionSelectorType option = parseOptionType(cleanedOptionLocator);
		option.selector.accept(new Select(element), StringUtils.stripStart(cleanedOptionLocator, option + WebElementManipulator.SELECTOR_TYPE_SEPARATOR));
	}

	public String selected(WebElement element, String optionType) {
		return this.fitnesseMarkup.clean(parseOptionType(optionType).retriever.apply(new Select(element)));
	}

	private OptionSelectorType parseOptionType(String optionType) {
		return Optional.ofNullable(EnumUtils.getEnum(OptionSelectorType.class, StringUtils.substringBefore(optionType, WebElementManipulator.SELECTOR_TYPE_SEPARATOR))).orElse(OptionSelectorType.label);
	}
}
