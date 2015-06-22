fitnesse-selenium-slim [![Build Status](https://travis-ci.org/andreptb/fitnesse-selenium-slim.svg?branch=selenium_script_table)](https://travis-ci.org/andreptb/fitnesse-selenium-slim) [![Coverage Status](https://coveralls.io/repos/andreptb/fitnesse-selenium-slim/badge.svg?branch=selenium_script_table)](https://coveralls.io/r/andreptb/fitnesse-selenium-slim?branch=selenium_script_table) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.andreptb/fitnesse-selenium-slim/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.andreptb/fitnesse-selenium-slim/)
==============

**Important**: This is a work in progress, come back later if you want something usable. Or contact me if you want to contribute.

  [FitNesse](https://github.com/unclebob/fitnesse) Selenium fixture in [slim format](http://www.fitnesse.org/FitNesse.UserGuide.WritingAcceptanceTests.SliM). Resembles [Xebium](http://xebia.github.io/Xebium/), but it's even more similar to [Selenium IDE Firefox Plugin](http://www.seleniumhq.org/projects/ide/). Also gets inspiration from  [hsac-fitnesse-fixtures](https://github.com/fhoeben/hsac-fitnesse-fixtures) but doesn't try to "simplify" Selenium IDE development flow. This project is licensed under [MIT](LICENSE).

#### Installation

* This module and selenium dependencies must be in [FitNesse classpath](http://www.fitnesse.org/FitNesse.FullReferenceGuide.UserGuide.WritingAcceptanceTests.ClassPath). You can download all necessary jars from [here](https://github.com/andreptb/fitnesse-selenium-slim/releases/download/0.0.1/fitness-selenium-slim-all-jars.zip) or with [maven](https://github.com/lvonk/fitnesse-maven-classpath) (see below).
* The [WebDriver](http://www.seleniumhq.org/projects/webdriver/) which the fixture will be used to connect also must be on [FitNesse](https://github.com/unclebob/fitnesse) classpath.

```xml
<dependency>
  <groupId>com.github.andreptb</groupId>
  <artifactId>fitnesse-selenium-slim</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

#### Testing

* To run JUnit tests using firefox browser:

```
export BROWSER=firefox mvn test
```

* To start FitNesse server and navigate through samples:

```
mvn exec:java -Dexec.mainClass=fitnesseMain.FitNesseMain -Dexec.args="-d fitnesse"
```

####  Sample:

```
| import |
| com.github.andreptb.fitnesse |

| selenium |
| start browser | firefox |
| open | http://www.google.com.br |
| note | types something on google search box |
| type | selenium web browser | in | id=lst-ib |
| note | checks if the input has the desired value |
| check | value | id=lst-ib | selenium web browser |
| close |
```

