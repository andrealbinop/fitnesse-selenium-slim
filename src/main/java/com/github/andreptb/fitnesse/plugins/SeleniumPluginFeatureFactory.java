package com.github.andreptb.fitnesse.plugins;

import fitnesse.plugins.PluginException;
import fitnesse.plugins.PluginFeatureFactoryBase;
import fitnesse.testsystems.slim.tables.SlimTableFactory;

/**
 * Plugin factory registers selenium table type
 */
public class SeleniumPluginFeatureFactory extends PluginFeatureFactoryBase {

    /**
     * SeleniumScriptTable registering
     */
    @Override
    public void registerSlimTables(SlimTableFactory slimTableFactory) throws PluginException {
        slimTableFactory.addTableType("selenium", SeleniumScriptTable.class);
    }
}
