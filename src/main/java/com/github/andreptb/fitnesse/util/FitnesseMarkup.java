package com.github.andreptb.fitnesse.util;

import org.apache.commons.lang.StringUtils;

/**
 * General utilities to process FitNesse markup syntax so can be used by Selenium Fixture
 */
public class FitnesseMarkup {

    /**
     * Extracts the value from various FitNesse symbols such as URL generated links and so on
     * @param symbol to be cleaned
     * @return cleanedSymbol
     */
    public String clean(String symbol) {
        String strippedSymbol = StringUtils.strip(symbol);
        String cleanedUrl = StringUtils.substringBetween(strippedSymbol, "href=\"", "\">");
        if(StringUtils.isNotBlank(cleanedUrl)) {
            return cleanedUrl;
        }
        return strippedSymbol;
    }
}
