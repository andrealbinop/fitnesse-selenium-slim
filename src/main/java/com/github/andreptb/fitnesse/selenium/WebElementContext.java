
package com.github.andreptb.fitnesse.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class WebElementContext {

	private WebElement element;
	private By locator;
	private String attribute;
	private String expectedValue;

	public WebElement getElement() {
		return this.element;
	}

	public void setElement(WebElement element) {
		this.element = element;
	}

	public By getLocator() {
		return this.locator;
	}

	public void setLocator(By locator) {
		this.locator = locator;
	}

	public String getAttribute() {
		return this.attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getExpectedValue() {
		return this.expectedValue;
	}

	public void setExpectedValue(String expectedValue) {
		this.expectedValue = expectedValue;
	}
}