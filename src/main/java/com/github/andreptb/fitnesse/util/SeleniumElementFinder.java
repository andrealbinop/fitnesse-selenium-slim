package com.github.andreptb.fitnesse.util;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Utility to find elements supporting different selectors.
 *
 * @see #find(WebDriver, String)
 */
public class SeleniumElementFinder {

    /**
     * Locator selector typeIn separator constant
     */
    private static final String SELECTOR_TYPE_SEPARATOR = "=";
    /**
     * Utility to process FitNesse markup so can be used by Selenium WebDriver
     */
    private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

    enum SelectorType {
        id(By.ById.class),
        name(By.ByName.class),
        css(By.ByCssSelector.class),
        link(By.ByLinkText.class);

        private Class<? extends By> byClass;

        SelectorType(Class<? extends By> byClass) {
            this.byClass = byClass;
        }
    }

    /**
     * Selects element. Tries to emulate util IDE searching methods
     * <ul>
     *     <li>By id: 'id=&lt;id&gt;</li>'
     *     <li>By name: 'name=&lt;name&gt;</li>'
     *     <li>By css selector: 'css=#&lt;id&gt;</li>'
     *     <li>By link text selector: 'link=#&lt;linKtEXT&gt;</li>'
     *     <li>By xpath selector: 'div[@id=&lt;id&gt;]</li>'
     * </ul>
     * @param locator an element locator
     * @return webElementFound
     * @throws NoSuchElementException if element don't exist or cannot be found
     */
    public WebElement find(WebDriver driver, String locator) {
        String cleanedLocator = fitnesseMarkup.clean(locator);
        String selectorPrefix = StringUtils.substringBefore(cleanedLocator, SeleniumElementFinder.SELECTOR_TYPE_SEPARATOR);
        SelectorType selectorType = EnumUtils.getEnum(SelectorType.class, selectorPrefix);
        if(selectorType == null) {
            return driver.findElement(By.xpath(locator));
        }
        try {
            String parsedLocator = StringUtils.removeStart(cleanedLocator, selectorPrefix + SeleniumElementFinder.SELECTOR_TYPE_SEPARATOR);
            return driver.findElement(selectorType.byClass.getConstructor(String.class).newInstance(parsedLocator));
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Unexpected failure instantiating selector: " + selectorPrefix, e);
        }
    }

    /**
     * Checks if element is contained in the page.
     * @see #find(WebDriver, String)
     * @param locator an element locator
     * @return containsElement true if found, false otherwise
     */
    public boolean contains(WebDriver driver, String locator) {
        try {
            find(driver, locator);
        } catch(NoSuchElementException e) {
            return false;
        }
        return true;
    }
}
