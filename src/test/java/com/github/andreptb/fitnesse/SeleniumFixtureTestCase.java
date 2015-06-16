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
        testSelector("id=lst-ib");
    }

    @Test
    public void testTypeElementFoundByCss() {
        testSelector("css=#lst-ib");
    }

    @Test
    public void testTypeElementFoundByXpath() {
        testSelector("//input[@id='lst-ib']");
    }

    private void testSelector(String selector) {
        this.seleniumFixture.open("http://www.google.com");
        String expectedValue = "selenium web browser";
        this.seleniumFixture.type(selector, expectedValue);
        this.seleniumFixture.assertValue(selector, expectedValue);
    }
}
