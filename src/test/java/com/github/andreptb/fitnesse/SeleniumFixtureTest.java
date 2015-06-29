package com.github.andreptb.fitnesse;

import org.junit.runner.RunWith;

import fitnesse.junit.FitNesseRunner;

/**
 * Slim Fixture unit testing
 */
@RunWith(FitNesseRunner.class)
@FitNesseRunner.Suite("FitNesseSeleniumSlim.SeleniumFixtureTests.TitleTest")
@FitNesseRunner.FitnesseDir("fitnesse")
@FitNesseRunner.OutputDir("target/fitnesse")
public class SeleniumFixtureTest {

}
