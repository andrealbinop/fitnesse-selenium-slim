package com.github.andreptb.fitnesse.plugins;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.github.andreptb.fitnesse.SeleniumFixture;
import com.github.andreptb.fitnesse.util.FitnesseMarkup;

import fitnesse.testsystems.slim.SlimTestContext;
import fitnesse.testsystems.slim.Table;
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
		return startActor(0, SeleniumFixture.class.getName(), 0);
	}

	/**
	 * Extends ScriptTable assertion to invoke SeleniumFixture screenshot action of browser current state.
	 */
	@Override
	protected List<SlimAssertion> invokeAction(int startingCol, int endingCol, int row, SlimExpectation expectation) {
		List<SlimAssertion> action = super.invokeAction(startingCol, endingCol, row, expectation);
		action.add(makeAssertion(callFunction(getTableType() + "Actor", SeleniumScriptTable.SCREENSHOT_FIXTURE_ACTION, ArrayUtils.EMPTY_OBJECT_ARRAY), new ShowScreenshotExpectation(startingCol, row)));
		return action;
	}

	/**
	 * Expectation implementation to process screenshot of browser current state
	 */
	class ShowScreenshotExpectation extends RowExpectation {

		public ShowScreenshotExpectation(int col, int row) {
			super(col, row);
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
			// when fixtures returns null fitnesse converts to a string with "null" content
			String cleanedActual = StringUtils.remove(actual, "null");
			if(StringUtils.isBlank(cleanedActual)) {
				return SlimTestResult.plain();
			}
			try {
				SeleniumScriptTable.this.table.addColumnToRow(getRow(), SeleniumScriptTable.this.fitnesseMarkup.img(cleanedActual, SeleniumScriptTable.this.getTestContext().getPageToTest()));
			} catch (IOException e) {
				throw new IllegalStateException("Unexpected IO error providing screenshot for test result", e);
			}
			return SlimTestResult.plain();
		}
	}
}
