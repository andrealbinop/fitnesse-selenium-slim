package com.github.andreptb.fitnesse;

import org.apache.commons.lang.StringUtils;
import org.junit.*;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;

/**
 * Slim Fixture unit testing
 */
public class SeleniumFixtureTestCase {

    /**
     * So remote browser can be used with ci
     */
    private static final String BROWSER = StringUtils.defaultIfBlank(System.getenv("BROWSER"), "firefox");


    private static SeleniumFixture seleniumFixture;

    @BeforeClass
    public static void createSeleniumFixture() throws ReflectiveOperationException, MalformedURLException {
        SeleniumFixtureTestCase.seleniumFixture = new SeleniumFixture();
        SeleniumFixtureTestCase.seleniumFixture.startWith(SeleniumFixtureTestCase.BROWSER, DesiredCapabilities.firefox());
    }

    @AfterClass
    public static void quitsSeleniumFixture() {
        SeleniumFixtureTestCase.seleniumFixture.quit();
    }

    @Test
    public void testAssertTitle() {
        SeleniumFixtureTestCase.seleniumFixture.open("http://www.google.com");
        SeleniumFixtureTestCase.seleniumFixture.assertTitle("Google");
    }

    @Test
    public void testConnectInvalidBrowser() throws ReflectiveOperationException, MalformedURLException {
        try {
            this.seleniumFixture.start("invalidBrowser");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Wrong exception message", "Invalid browser [invalidBrowser]", StringUtils.substringBefore(e.getMessage(), "."));
        }
    }

    @Test
    public void testTypeElementFoundById() {
        testTypeElement("id=lst-ib");
    }

    @Test
    public void testTypeElementFoundByName() {
        testTypeElement("name=q");
    }

    @Test
    public void testTypeElementFoundByCss() {
        testTypeElement("css=#lst-ib");
    }

    @Test
    public void testTypeElementFoundByXpath() {
        testTypeElement("//input[@id='lst-ib']");
    }


    private void testTypeElement(String selector) {
        this.seleniumFixture.open("http://www.google.com");
        String expectedValue = "Selenium - Web Browser Automation";
        this.seleniumFixture.type(selector, expectedValue);
        this.seleniumFixture.assertValue(selector, expectedValue);
        this.seleniumFixture.verifyElementPresent("name=btnG");
    }

    @Test
    public void testClickElementFoundByName() {
        this.seleniumFixture.open("http://www.google.com");
        this.seleniumFixture.click("name=btnI");
        this.seleniumFixture.assertTitle("Google Doodles");
    }

    @Test
    public void testClickElementFoundByLinkText() {
        this.seleniumFixture.open("http://www.google.com/doodles");
        this.seleniumFixture.click("link=About");
        this.seleniumFixture.verifyText("css=#popular-doodles h3", "More Doodles");
    }
}
