/*
 * Copyright 2018, MP Objects, http://www.mp-objects.com
 */

package com.github.andreptb.fitnesse.selenium;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.HasIdentity;

import com.github.andreptb.fitnesse.SeleniumFixture;
import com.google.common.base.Strings;

/**
 * A special selector with returns a previously retrieved element. It does perform any actual searching. The found element might no longer valid on the remote, resulting in failures when any actions
 * are performed.
 */
public class ByWebElement extends By implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;

	private transient WebElement element;

	public ByWebElement(String id) {
		if (Strings.isNullOrEmpty(id)) {
			throw new IllegalArgumentException("Invalid WebElement ID");
		}
		this.id = id;
	}

	public ByWebElement(WebElement element) {
		this(((HasIdentity) element).getId());
		this.element = element;
	}

	public String getId() {
		return id;
	}

	@Override
	public WebElement findElement(SearchContext aContext) {
		if (element != null) {
			return element;
		}
		element = SeleniumFixture.getDriver().getCachedElement(id);
		if (element == null) {
			// throw exception because if it is not in the cache it will never become available anymore
			throw new StaleElementReferenceException(String.format("Element with id %s is no longer in the locale cache.", id));
		}
		return element;
	}

	@Override
	public List<WebElement> findElements(SearchContext aContext) {
		WebElement result = findElement(aContext);
		if (result != null) {
			return Collections.singletonList(result);
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public String toString() {
		return "By.webelement: " + id;
	}

	public String toLocator() {
		return String.format("webelement=%s", id);
	}
}
