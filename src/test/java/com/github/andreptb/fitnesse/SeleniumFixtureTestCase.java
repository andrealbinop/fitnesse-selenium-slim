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
        DesiredCapabilities capabilities = DesiredCapabilities.firefox();
        capabilities.setCapability("name", SeleniumFixtureTestCase.class.getName());
        SeleniumFixtureTestCase.seleniumFixture.startBrowserWith(SeleniumFixtureTestCase.BROWSER, capabilities);
    }

    @AfterClass
    public static void quitsSeleniumFixture() {
        SeleniumFixtureTestCase.seleniumFixture.quit();
    }

    @Test
    public void testTitle() {
        SeleniumFixtureTestCase.seleniumFixture.open("http://saucelabs.com/");
        Assert.assertEquals("Sauce Labs: Selenium Testing, Mobile Testing, JS Unit Testing and More", SeleniumFixtureTestCase.seleniumFixture.title());
    }

    @Test
    public void testConnectInvalidBrowser() throws ReflectiveOperationException, MalformedURLException {
        try {
            this.seleniumFixture.startBrowser("invalidBrowser");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Wrong exception message", "Invalid browser [invalidBrowser]", StringUtils.substringBefore(e.getMessage(), "."));
        }
    }

    @Test
    public void testTypeElementFoundById() {
        testTypeElement("id=username");
    }

    @Test
    public void testTypeElementFoundByName() {
        testTypeElement("name=username");
    }

    @Test
    public void testTypeElementFoundByCss() {
        testTypeElement("css=#username");
    }

    @Test
    public void testTypeElementFoundByXpath() {
        testTypeElement("//input[@id='username']");
    }


    private void testTypeElement(String selector) {
        this.seleniumFixture.open("http://saucelabs.com/login");
        String expectedValue = "test";
        this.seleniumFixture.typeIn(expectedValue, selector);
        Assert.assertEquals(selector, expectedValue, this.seleniumFixture.value(selector));
    }

    @Test
    public void testClickElementFoundByLinkText() {
        this.seleniumFixture.open("http://saucelabs.com/login");
        this.seleniumFixture.click("link=Login with GitHub");
        Assert.assertEquals("Unexpected Title", "Sign in · GitHub", SeleniumFixtureTestCase.seleniumFixture.title());
    }

    @Test
    public void testVerifyText() {
        this.seleniumFixture.open("http://saucelabs.com/login");
        Assert.assertEquals("Wrong text", "Welcome back.", this.seleniumFixture.text("css=#login-section h1"));
    }

    @Test
    public void testVerifyElementPresent() {
        this.seleniumFixture.open("http://saucelabs.com/login");
        Assert.assertTrue(this.seleniumFixture.present("id=username"));
    }
}
