
package com.github.andreptb.fitnesse.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import com.github.andreptb.fitnesse.SeleniumFixture;
import com.github.andreptb.fitnesse.util.FitnesseMarkup;

import fitnesse.slim.instructions.CallInstruction;
import fitnesse.slim.instructions.ImportInstruction;
import fitnesse.slim.instructions.Instruction;
import fitnesse.testsystems.TestResult;
import fitnesse.testsystems.slim.SlimTestContext;
import fitnesse.testsystems.slim.Table;
import fitnesse.testsystems.slim.results.SlimExceptionResult;
import fitnesse.testsystems.slim.tables.ScriptTable;
import fitnesse.testsystems.slim.tables.SlimAssertion;
import fitnesse.testsystems.slim.tables.SlimExpectation;

/**
 * Selenium table, works just like ScriptTable, but adds extra features such as step screenshots and provide extra information to the fixture, allowing wait behavior for check and ensure actions
 */
public class SeleniumScriptTable extends ScriptTable {

	/**
	 * JUL Logger instance
	 */
	private static final Logger LOGGER = Logger.getLogger(SeleniumScriptTable.class.getName());

	/**
	 * Table keyword constant
	 */
	public static final String TABLE_KEYWORD = "selenium";
	/**
	 * Table type constant
	 */
	private static final String TABLE_TYPE = SeleniumScriptTable.TABLE_KEYWORD + "Script";
	/**
	 * Fixture package to auto-import package
	 */
	private static final String SELENIUM_FIXTURE_PACKAGE_TO_IMPORT = "com.github.andreptb.fitnesse";

	/**
	 * Constant to reference {@link CallInstruction} args private field
	 */
	private static final String CALL_INSTRUCTION_METHODNAME_FIELD = "methodName";
	/**
	 * Constant to reference {@link CallInstruction} args private field
	 */
	private static final String CALL_INSTRUCTION_ARGS_FIELD = "args";
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
	protected List<SlimAssertion> ensure(int row) {
		List<SlimAssertion> assertions = super.ensure(row);
		injectValueInFirstArg(assertions, false, true);
		return assertions;
	}

	@Override
	protected List<SlimAssertion> reject(int row) {
		List<SlimAssertion> assertions = super.reject(row);
		injectValueInFirstArg(assertions, false, false);
		return assertions;
	}

	@Override
	protected List<SlimAssertion> checkAction(int row) {
		List<SlimAssertion> assertions = super.checkAction(row);
		injectResultToAction(row, assertions, false);
		return assertions;
	}

	@Override
	protected List<SlimAssertion> checkNotAction(int row) {
		List<SlimAssertion> assertions = super.checkNotAction(row);
		injectResultToAction(row, assertions, true);
		return assertions;
	}

	private void injectResultToAction(int row, List<SlimAssertion> assertions, boolean not) {
		String contentToCheck = this.fitnesseMarkup.clean(this.table.getCellContents(this.table.getColumnCountInRow(row) - 1, row));
		if (CollectionUtils.isEmpty(assertions) || StringUtils.isBlank(contentToCheck)) {
			return;
		}
		injectValueInFirstArg(assertions, not, contentToCheck);
	}

	private void injectValueInFirstArg(List<SlimAssertion> assertions, boolean not, Object contentToCheck) {
		SlimAssertion.getInstructions(assertions).forEach(instruction -> {
			try {
				String valueToInject = FitnesseMarkup.SELECTOR_VALUE_SEPARATOR + (not ? FitnesseMarkup.SELECTOR_VALUE_DENY_INDICATOR : StringUtils.EMPTY) + contentToCheck;
				Object args = FieldUtils.readField(instruction, SeleniumScriptTable.CALL_INSTRUCTION_ARGS_FIELD, true);
				Object[] argsToInject;
				if (args instanceof Object[] && ArrayUtils.getLength(args) > NumberUtils.INTEGER_ZERO) {
					argsToInject = (Object[]) args;
					argsToInject[NumberUtils.INTEGER_ZERO] += valueToInject;
				} else {
					argsToInject = ArrayUtils.toArray(valueToInject);
				}
				String methodName = Objects.toString(FieldUtils.readField(instruction, SeleniumScriptTable.CALL_INSTRUCTION_METHODNAME_FIELD, true));
				if (Objects.isNull(MethodUtils.getAccessibleMethod(SeleniumFixture.class, Objects.toString(methodName), ClassUtils.toClass(argsToInject)))) {
					SeleniumScriptTable.LOGGER.fine("Method for instruction not found on SeleniumFixture, injection aborted: " + instruction);
					return;
				}
				FieldUtils.writeField(instruction, SeleniumScriptTable.CALL_INSTRUCTION_ARGS_FIELD, argsToInject);
			} catch (IllegalArgumentException | ReflectiveOperationException e) {
				SeleniumScriptTable.LOGGER.log(Level.FINE, "Failed to inject check value using reflection", e);
			}			
		});
	}
	
	@Override
	protected List<SlimAssertion> actionAndAssign(String symbolName, int row) {
		return super.actionAndAssign(symbolName, row).stream().map(assertion -> {
			Instruction instruction = SlimAssertion.getInstructions(Arrays.asList(assertion)).iterator().next();
			ScreenshotEmbedderSlimExpectation expectation = new ScreenshotEmbedderSlimExpectation(assertion.getExpectation());
			return super.makeAssertion(instruction, expectation);
		}).collect(Collectors.toList());
	}

	@Override
	protected List<SlimAssertion> invokeAction(int startingCol, int endingCol, int row, SlimExpectation expectation) {
		return super.invokeAction(startingCol, endingCol, row, new ScreenshotEmbedderSlimExpectation(expectation));
	}

	private class ScreenshotEmbedderSlimExpectation implements SlimExpectation {

		private SlimExpectation original;

		ScreenshotEmbedderSlimExpectation(SlimExpectation original) {
			this.original = original;
		}

		@Override
		public TestResult evaluateExpectation(Object returnValues) {
			return this.original.evaluateExpectation(returnValues);
		}

		@Override
		public SlimExceptionResult evaluateException(SlimExceptionResult exceptionResult) {
			SlimExceptionResult result = original.evaluateException(exceptionResult);
			if(original instanceof RowExpectation) {
				String screenshot = fitnesseMarkup.imgLinkFromExceptionMessage(exceptionResult.getException());
				if(StringUtils.isNotBlank(screenshot)) {
					SeleniumScriptTable.this.getTable().addColumnToRow(((RowExpectation) original).getRow(), screenshot);
				}
			}
			return result;
		}

	}
}
