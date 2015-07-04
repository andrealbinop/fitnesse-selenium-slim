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
	 * Overrides start actor to force the use of Selenium Fixture. Auto imports selenium fixture if needed Wouldn't make sense a Selenium Table running fixtures unrelated to Selenium.
	 */
	@Override
	protected List<SlimAssertion> startActor() {
		List<SlimAssertion> assertions = new ArrayList<>();
		assertions.add(makeAssertion(new ImportInstruction(ImportInstruction.INSTRUCTION, SeleniumScriptTable.SELENIUM_FIXTURE_PACKAGE_TO_IMPORT), SlimExpectation.NOOP_EXPECTATION));
		assertions.addAll(startActor(NumberUtils.INTEGER_ZERO, SeleniumFixture.class.getName(), NumberUtils.INTEGER_ZERO));
		return assertions;
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
		assertions.add(makeAssertion(callFunction(getTableType() + "Actor", SeleniumScriptTable.SCREENSHOT_FIXTURE_ACTION), new ShowScreenshotExpectation(row)));
		return assertions;
	}


	/**
	 * Expectation implementation to process screenshot of browser current state
	 */
	class ShowScreenshotExpectation extends RowExpectation {

		public ShowScreenshotExpectation(int row) {
			super(NumberUtils.INTEGER_ZERO, row);
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
				SeleniumScriptTable.this.table.addColumnToRow(getRow(), SeleniumScriptTable.this.fitnesseMarkup.imgLink(cleanedActual));
				fillPreviousRowsWithoutScreenshot();
			}
			return SlimTestResult.plain();
		}

		/**
		 * Adds an empty column for previous columns that didn't have screenshot
		 *
		 * @param row
		 */
		private void fillPreviousRowsWithoutScreenshot() {
			int row = getRow();
			while (--row >= NumberUtils.INTEGER_ZERO) {
				String content = StringUtils.EMPTY;
				if (row == NumberUtils.INTEGER_ZERO) {
					content = SeleniumScriptTable.SCREENSHOT_FIXTURE_ACTION;
				}
				String currentContent = SeleniumScriptTable.this.table.getCellContents(SeleniumScriptTable.this.table.getColumnCountInRow(row) - 1, row);
				if (StringUtils.contains(currentContent, "img") || StringUtils.equals(content, currentContent)) {
					continue;
				}
				SeleniumScriptTable.this.table.addColumnToRow(row, content);
			}
		}
	}
}
