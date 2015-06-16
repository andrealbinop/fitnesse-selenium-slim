fitnesse-selenium-slim [![Build Status](https://travis-ci.org/andreptb/fitnesse-selenium-slim.svg)](https://travis-ci.org/andreptb/fitnesse-selenium-slim) [![Coverage Status](https://coveralls.io/repos/andreptb/fitnesse-selenium-slim/badge.svg)](https://coveralls.io/r/andreptb/fitnesse-selenium-slim) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.andreptb/fitnesse-selenium-slim/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.andreptb/fitnesse-selenium-slim/)
==============

** Important**: This is a work in progress, come back later if you want something usable. Or contact me if you want to contribute.

  A work in progress [FitNesse](https://github.com/unclebob/fitnesse) Selenium fixture in [slim format](http://www.fitnesse.org/FitNesse.UserGuide.WritingAcceptanceTests.SliM). Resembles [Xebium](http://xebia.github.io/Xebium/), but it's even more similar to [Selenium IDE Firefox Plugin](http://www.seleniumhq.org/projects/ide/). Also gets inspiration from  [hsac-fitnesse-fixtures](https://github.com/fhoeben/hsac-fitnesse-fixtures) but doesn't try to "simplify" Selenium IDE development flow. This project is licensed under [MIT](LICENSE).

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

####  Sample:
```
| import |
| com.github.andreptb.fitnesse |

| library |
| selenium fixture |

| script |
| start | firefox | # Starts firefox browser
| open | http://www.google.com.br | # opens google website
| type | id=lst-ib | selenium web browser | # types "selenium web browser" on google's search input
| assert value | id=lst-ib | selenium web browser | # asserts that google's search input has "selenium web browser" value
```
