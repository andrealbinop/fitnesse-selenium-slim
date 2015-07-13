fitnesse-selenium-slim [![Build Status](https://travis-ci.org/andreptb/fitnesse-selenium-slim.svg?branch=master)](https://travis-ci.org/andreptb/fitnesse-selenium-slim) [![Coverage Status](https://coveralls.io/repos/andreptb/fitnesse-selenium-slim/badge.svg?branch=master)](https://coveralls.io/r/andreptb/fitnesse-selenium-slim?branch=master) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.andreptb/fitnesse-selenium-slim/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.andreptb/fitnesse-selenium-slim/) [![javadoc](http://javadoc-badge.appspot.com/com.github.andreptb/fitnesse-selenium-slim.svg?label=javadoc)](http://andreptb.github.io/fitnesse-selenium-slim/apidocs/index.html) [![Join the chat at https://gitter.im/andreptb/fitnesse-selenium-slim](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/andreptb/fitnesse-selenium-slim?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
==============


[FitNesse](https://github.com/unclebob/fitnesse) Selenium fixture in [slim format](http://www.fitnesse.org/FitNesse.UserGuide.WritingAcceptanceTests.SliM). Resembles [Xebium](http://xebia.github.io/Xebium/), but it's even more similar to [Selenium IDE Firefox Plugin](http://www.seleniumhq.org/projects/ide/). Also gets inspiration from [hsac-fitnesse-fixtures](https://github.com/fhoeben/hsac-fitnesse-fixtures) but doesn't try to "simplify" Selenium IDE development flow. This project is licensed under [MIT](LICENSE).

####  Getting started

Take a look at [this](fitnesse/FitNesseRoot/FitNesseSeleniumSlim/BasicUsageSample/content.txt) FitNesseRoot test. Furthermore, detailed information about the available fixture commands can be found  [here](http://andreptb.github.io/fitnesse-selenium-slim/apidocs/com/github/andreptb/fitnesse/SeleniumFixture.html#startBrowser%28java.lang.String%29).

#### Installation

* This module and selenium dependencies must be in [FitNesse classpath](http://www.fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.WritingAcceptanceTests.ClassPath). You can download the jars from [here](http://repo1.maven.org/maven2/com/github/andreptb/fitnesse-selenium-slim/) or with [maven](https://github.com/lvonk/fitnesse-maven-classpath) (see below).
* The [WebDriver](http://www.seleniumhq.org/projects/webdriver/) which the fixture will be used to connect also must be on [FitNesse](https://github.com/unclebob/fitnesse) classpath.

```xml
<dependency>
  <groupId>com.github.andreptb</groupId>
  <artifactId>fitnesse-selenium-slim</artifactId>
  <version>0.4.0-SNAPSHOT</version>
</dependency>
```

#### Testing and Building
```
BROWSER=firefox mvn test
```

* To start FitNesse server and navigate through samples:

```
mvn exec:java -Dexec.mainClass=fitnesseMain.FitNesseMain -Dexec.args="-d fitnesse"
```

* To build this plugin and add to maven local repository:

```
mvn install -Dgpg.skip
```

#### Screenshots

This plugin provides a screenshot feature, showing the screenshot preview (and link) similar to [hsac-fitnesse-plugin](https://github.com/fhoeben/hsac-fitnesse-plugin). To trigger the screenshot you just need to invoke the screenshot with show action (taken from [FitNesseSeleniumSlim.SeleniumFixtureTests.SameBrowserSessionTests.EnsureTextTest](fitnesse/FitNesseRoot/FitNesseSeleniumSlim/SeleniumFixtureTests/SameBrowserSessionTests/EnsureTextTest/content.txt)):

```
| selenium |
| show | screenshot |
```

Note that if **[RootPath or FitNesseRoot](http://www.fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.AdministeringFitNesse.CommandLineArguments)** arguments are changed, you must configure [SeleniumFixture's](/fitnesse-selenium-slim/src/main/java/com/github/andreptb/fitnesse/SeleniumFixture.java) screenshot dir with the following action (taken from [FitNesseSeleniumSlim.SeleniumFixtureTests.SuiteSetUp](fitnesse/FitNesseRoot/FitNesseSeleniumSlim/SeleniumFixtureTests/SameBrowserSessionTests/SuiteSetUp/content.txt)):

```
| selenium |
| set screenshot dir | ${FITNESSE_ROOTPATH}/${FitNesseRoot}/files/testResults/screenshots |
```

**Important:** At this time the screenshot feature won't work with a non-default [context root](http://www.fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.AdministeringFitNesse.ConfigurationFile). It will be supported when [this version is released](https://github.com/unclebob/fitnesse/issues?q=is%3Aopen+is%3Aissue+milestone%3A%22Next+release%22) is closed.

#### Wait behavior

Every action that searches for elements within the page (**text**, **value**, **click**, **attribute**, **present** and **not present**) will try to find the element until specified timeout configuration is reached. You can change the timeout configuration with the following:

```
| selenium |
| note | changes timeout configuration for 5 seconds (default is 20) |
| set wait timeout | 5 |
```

**Important:** The **present** action will return false if timeout is reached and no element was found with the given selector. **not present** do the other way around.
