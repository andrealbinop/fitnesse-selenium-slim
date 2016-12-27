
package com.github.andreptb.fitnesse;

import fitnesse.junit.FitNesseRunner;
import org.junit.runner.RunWith;

/**
 * Slim Fixture testing. Configured to run FitNesseSeleniumSlim.SeleniumFixtureTests suite.
 */
@RunWith(FitNesseRunner.class)
@FitNesseRunner.Suite(".FitNesseSeleniumSlim.SeleniumFixtureTests")
@FitNesseRunner.FitnesseDir("fitnesse")
@FitNesseRunner.OutputDir("target/fitnesse")
public class SeleniumFixtureTest {

}
