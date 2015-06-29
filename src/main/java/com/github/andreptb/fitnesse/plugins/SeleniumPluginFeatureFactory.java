package com.github.andreptb.fitnesse.plugins;

import java.text.MessageFormat;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.Keys;

import fitnesse.plugins.PluginException;
import fitnesse.plugins.PluginFeatureFactoryBase;
import fitnesse.testsystems.slim.tables.SlimTableFactory;

/**
 * Plugin factory registers selenium table type
 */
public class SeleniumPluginFeatureFactory extends PluginFeatureFactoryBase {

	/**
	 * Constant used to register selenium special keys as system properties
	 */
	private static final String SELENIUM_SPECIAL_KEY_MARKUP = "KEY_{0}";

    /**
     * SeleniumScriptTable registering
     */
    @Override
    public void registerSlimTables(SlimTableFactory slimTableFactory) throws PluginException {
        slimTableFactory.addTableType("selenium", SeleniumScriptTable.class);
		registerSpecialKeysVariables();
    }

	/**
	 * Registers selenium {@link Keys} as system properties, so can be used by wiki pages as ${KEY_XXX}
	 */
	private void registerSpecialKeysVariables() {
		for(Keys key : Keys.values()) {
			System.setProperty(MessageFormat.format(SeleniumPluginFeatureFactory.SELENIUM_SPECIAL_KEY_MARKUP, StringUtils.upperCase(key.name())), key.toString());
		}
	}
}
