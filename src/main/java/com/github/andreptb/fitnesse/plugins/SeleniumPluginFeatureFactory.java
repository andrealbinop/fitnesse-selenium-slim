package com.github.andreptb.fitnesse.plugins;

import org.openqa.selenium.Keys;

import com.github.andreptb.fitnesse.util.FitnesseMarkup;

import fitnesse.plugins.PluginException;
import fitnesse.plugins.PluginFeatureFactoryBase;
import fitnesse.testsystems.slim.tables.SlimTableFactory;

/**
 * Plugin factory registers selenium table type
 */
public class SeleniumPluginFeatureFactory extends PluginFeatureFactoryBase {

	/**
	 * Utility to process FitNesse markup so can be used by Selenium WebDriver
	 */
	private FitnesseMarkup fitnesseMarkup = new FitnesseMarkup();

    /**
	 * SeleniumScriptTable registering
	 *
	 * @param slimTableFactory Instance responsible for registering slim tables
	 */
    @Override
    public void registerSlimTables(SlimTableFactory slimTableFactory) throws PluginException {
		slimTableFactory.addTableType(SeleniumScriptTable.TABLE_KEYWORD, SeleniumScriptTable.class);
		registerSpecialKeysVariables();
    }

	/**
	 * Registers selenium {@link Keys} as system properties, so can be used by wiki pages as ${KEY_XXX}
	 */
	private void registerSpecialKeysVariables() {
		for(Keys key : Keys.values()) {
			this.fitnesseMarkup.registerKeyboardSpecialKey(key.name(), key.toString());
		}
	}
}
