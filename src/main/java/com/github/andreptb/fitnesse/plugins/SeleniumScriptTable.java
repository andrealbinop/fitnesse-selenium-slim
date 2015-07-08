package com.github.andreptb.fitnesse.plugins;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.github.andreptb.fitnesse.SeleniumFixture;
import com.github.andreptb.fitnesse.util.FitnesseMarkup;

import fitnesse.slim.instructions.ImportInstruction;
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
	public static final String TABLE_KEYWORD = "selenium";
	/**
	 * Table type constant
	 */
	private static final String TABLE_TYPE = SeleniumScriptTable.TABLE_KEYWORD + "Script";
	/**
	 * SeleniumFixture screenshot action
	 */
	private static final String SCREENSHOT_FIXTURE_ACTION = "screenshot";
	/**
	 * Fixture package to auto-import package
	 */
	private static final String SELENIUM_FIXTURE_PACKAGE_TO_IMPORT = "com.github.andreptb.fitnesse";
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
	 * Overrides start actor to force the use of Selenium Fixture. Auto imports selenium fixture if needed
	 */
	@Override
	protected List<SlimAssertion> startActor() {
		List<SlimAssertion> assertions = new ArrayList<>();
		assertions.add(makeAssertion(new ImportInstruction(ImportInstruction.INSTRUCTION, SeleniumScriptTable.SELENIUM_FIXTURE_PACKAGE_TO_IMPORT), SlimExpectation.NOOP_EXPECTATION));
		assertions.addAll(startActor(NumberUtils.INTEGER_ZERO, SeleniumFixture.class.getName(), NumberUtils.INTEGER_ZERO));
		return assertions;
	}

	@Override
	protected List<SlimAssertion> show(int row) {
		if (StringUtils.contains(this.table.getCellContents(NumberUtils.INTEGER_ONE, row), SeleniumScriptTable.SCREENSHOT_FIXTURE_ACTION)) {
			return showScreenshot(row);
		}
		return super.show(row);
	}


	/**
	 * Adds screenshot action to current stack
	 *
	 * @param assertions Current collection of assertions
	 * @param row current table row the action occured
	 * @return asssertion Same collection passed as parameter, to reduce boilerplate code
	 */
	protected List<SlimAssertion> showScreenshot(int row) {
		int lastCol = this.table.getColumnCountInRow(row) - NumberUtils.INTEGER_ONE;
		return invokeAction(NumberUtils.INTEGER_ONE, lastCol, row, new ShowScreenshotExpectation(NumberUtils.INTEGER_ONE, row));
	}


	/**
	 * Expectation implementation to process screenshot of browser current state
	 */
	private class ShowScreenshotExpectation extends RowExpectation {

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
			SeleniumScriptTable.this.table.substitute(getCol(), getRow(), SeleniumScriptTable.this.fitnesseMarkup.imgLink(actual));
			return SlimTestResult.plain();
		}
	}
}
