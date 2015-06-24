package com.github.andreptb.fitnesse.plugins;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.github.andreptb.fitnesse.SeleniumFixture;
import com.github.andreptb.fitnesse.util.FitnesseMarkup;

import fitnesse.testsystems.slim.SlimTestContext;
import fitnesse.testsystems.slim.Table;
import fitnesse.testsystems.slim.results.SlimExceptionResult;
import fitnesse.testsystems.slim.results.SlimTestResult;
import fitnesse.testsystems.slim.tables.ScriptTable;
import fitnesse.testsystems.slim.tables.SlimAssertion;
import fitnesse.testsystems.slim.tables.SlimExpectation;

/**
 * Selenium table, works just like ScriptTable, but adds extra features such as step screenshots and such
 */
public class SeleniumScriptTable extends ScriptTable {

	/**
	 * Table keyword constant
	 */
	private static final String TABLE_KEYWORD = "selenium";
	/**
	 * Table type constant
	 */
	private static final String TABLE_TYPE = SeleniumScriptTable.TABLE_KEYWORD + "Script";
	/**
	 * SeleniumFixture screenshot action
	 */
	private static final String SCREENSHOT_FIXTURE_ACTION = "screenshot";

	/**
	 * Utility to process FitNesse markup
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

	public SeleniumScriptTable(Table table, String id, SlimTestContext testContext) {
		super(table, id, testContext);
	}

	@Override
	protected String getTableType() {
		return SeleniumScriptTable.TABLE_TYPE;
	}

	@Override
	protected String getTableKeyword() {
		return SeleniumScriptTable.TABLE_KEYWORD;
	}

	/**
	 * Overrides start actor to force the use of Selenium Fixture. Wouldn't make sense a Selenium Table running fixtures unrelated to Selenium.
	 */
	@Override
	protected List<SlimAssertion> startActor() {
		return startActor(NumberUtils.INTEGER_ZERO, SeleniumFixture.class.getName(), NumberUtils.INTEGER_ZERO);
	}

	/**
	 * Overrides ensure action to add screenshot assertion
	 */
	@Override
	protected List<SlimAssertion> ensure(int row) {
		return configureScreenshot(super.ensure(row), row);
	}

	/**
	 * Overrides reject action to add screenshot assertion
	 */
	@Override
	protected List<SlimAssertion> reject(int row) {
		return configureScreenshot(super.reject(row), row);
	}

	/**
	 * Overrides check action to add screenshot assertion
	 */
	@Override
	protected List<SlimAssertion> checkAction(int row) {
		return configureScreenshot(super.checkAction(row), row);
	}

	/**
	 * Overrides check not action to add screenshot assertion
	 */
	@Override
	protected List<SlimAssertion> checkNotAction(int row) {
		return configureScreenshot(super.checkNotAction(row), row);
	}

	/**
	 * Adds screenshot action to current stack
	 *
	 * @param assertions Current collection of assertions
	 * @param row current table row the action occured
	 * @return asssertion Same collection passed as parameter, to reduce boilerplate code
	 */
	protected List<SlimAssertion> configureScreenshot(List<SlimAssertion> assertions, int row) {
		assertions.add(makeAssertion(callFunction(getTableType() + "Actor", SeleniumScriptTable.SCREENSHOT_FIXTURE_ACTION), new ShowScreenshotExpectation(row, this.table.getColumnCountInRow(row) - 1)));
		return assertions;
	}

	/**
	 * Expectation implementation to process screenshot of browser current state
	 */
	class ShowScreenshotExpectation extends RowExpectation {

		public ShowScreenshotExpectation(int row, int column) {
			super(column, row);
		}

		/**
		 * Delegates screenshot markup to FitnesseMarkup and adds a new column to last action's row
		 *
		 * @param actual The screenshot image filename which fixture screenshot action returned
		 * @param expected Not used
		 * @return testResult Always SlimTestResult#plain()
		 */
		@Override
		protected SlimTestResult createEvaluationMessage(String actual, String expected) {
			String cleanedActual = SeleniumScriptTable.this.fitnesseMarkup.clean(actual);
			if (StringUtils.isNotBlank(cleanedActual)) {
				try {
					SeleniumScriptTable.this.table.substitute(getCol(), getRow(), SeleniumScriptTable.this.fitnesseMarkup.img(cleanedActual, SeleniumScriptTable.this.getTestContext().getPageToTest()));
				} catch (IOException e) {
					throw new IllegalStateException("Unexpected IO error providing screenshot for test result", e);
				}
			}
			return SlimTestResult.plain();
		}

		@Override
		public SlimExceptionResult evaluateException(SlimExceptionResult exceptionResult) {
			return SlimExpectation.NOOP_EXPECTATION.evaluateException(exceptionResult);
		}
	}
}
