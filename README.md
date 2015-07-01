fitnesse-selenium-slim [![Build Status](https://travis-ci.org/andreptb/fitnesse-selenium-slim.svg?branch=master)](https://travis-ci.org/andreptb/fitnesse-selenium-slim) [![Coverage Status](https://coveralls.io/repos/andreptb/fitnesse-selenium-slim/badge.svg?branch=master)](https://coveralls.io/r/andreptb/fitnesse-selenium-slim?branch=master) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.andreptb/fitnesse-selenium-slim/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.andreptb/fitnesse-selenium-slim/) [![javadoc](http://javadoc-badge.appspot.com/com.github.andreptb/fitnesse-selenium-slim.svg?label=javadoc)](http://andreptb.github.io/fitnesse-selenium-slim/apidocs/index.html) [![Join the chat at https://gitter.im/andreptb/fitnesse-selenium-slim](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/andreptb/fitnesse-selenium-slim?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
==============

**Important**: This is a work in progress, come back later if you want something usable. Or contact me if you want to contribute.

  [FitNesse](https://github.com/unclebob/fitnesse) Selenium fixture in [slim format](http://www.fitnesse.org/FitNesse.UserGuide.WritingAcceptanceTests.SliM). Resembles [Xebium](http://xebia.github.io/Xebium/), but it's even more similar to [Selenium IDE Firefox Plugin](http://www.seleniumhq.org/projects/ide/). Also gets inspiration from [hsac-fitnesse-fixtures](https://github.com/fhoeben/hsac-fitnesse-fixtures) but doesn't try to "simplify" Selenium IDE development flow. This project is licensed under [MIT](LICENSE).

####  Getting started

Take a look at [this](fitnesse/FitNesseRoot/FitNesseSeleniumSlim/BasicUsageSample/content.txt) FitNesseRoot test. Furthermore, detailed information about the available fixture commands can be found  [here](http://andreptb.github.io/fitnesse-selenium-slim/apidocs/com/github/andreptb/fitnesse/SeleniumFixture.html#startBrowser-java.lang.String-). 

#### Installation

* This module and selenium dependencies must be in [FitNesse classpath](http://www.fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.WritingAcceptanceTests.ClassPath). You can download the jars from [here](http://repo1.maven.org/maven2/com/github/andreptb/fitnesse-selenium-slim/0.1.0/) or with [maven](https://github.com/lvonk/fitnesse-maven-classpath) (see below).
* The [WebDriver](http://www.seleniumhq.org/projects/webdriver/) which the fixture will be used to connect also must be on [FitNesse](https://github.com/unclebob/fitnesse) classpath.

```xml
<dependency>
  <groupId>com.github.andreptb</groupId>
  <artifactId>fitnesse-selenium-slim</artifactId>
  <version>0.1.0</version>
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

#### A word about screenshots

Similar to [hsac-fitnesse-plugin](https://github.com/fhoeben/hsac-fitnesse-plugin), this plugin includes screenshot for each operation. However, if **[RootPath or FitNesseRoot](http://www.fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.AdministeringFitNesse.CommandLineArguments)** arguments are changed, you must configure [SeleniumFixture's](/fitnesse-selenium-slim/src/main/java/com/github/andreptb/fitnesse/SeleniumFixture.java) screenshot dir with the following action (taken from [FitNesseSeleniumSlim.SeleniumFixtureTests.SuiteSetUp](fitnesse/FitNesseRoot/FitNesseSeleniumSlim/SeleniumFixtureTests/SuiteSetUp/content.txt):

```
| set screenshot dir | ${FITNESSE_ROOTPATH}/${FitNesseRoot}/files/testResults/screenshots |
```

**Important:** At this time the screenshot feature won't work with a non-default [context root](http://www.fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.AdministeringFitNesse.ConfigurationFile). It will be supported when [this issue](https://github.com/unclebob/fitnesse/pull/755) is closed.
