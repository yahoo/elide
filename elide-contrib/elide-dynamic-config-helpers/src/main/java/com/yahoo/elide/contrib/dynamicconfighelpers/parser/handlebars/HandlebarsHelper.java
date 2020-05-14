/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.Type;
import com.github.jknack.handlebars.Options;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Helper class for handlebar template hydration.
 */
public class HandlebarsHelper {

    private static final String EMPTY_STRING = "";
    private static final String STRING = "String";
    private static final String DATE = "Date";
    private static final String BIGDECIMAL = "BigDecimal";
    private static final String LONG = "Long";
    private static final String BOOLEAN = "Boolean";
    private static final String WHITESPACE_REGEX = "\\s+";

    /**
     * Capitalize first letter of the string.
     * @param str string to capitalize first letter
     * @return string with first letter capitalized
     */
    public String capitalizeFirstLetter(String str) {

        return (str == null || str.length() == 0) ? str : str.substring(0, 1).toUpperCase(Locale.ENGLISH)
                + str.substring(1);
    }

    /**
     * LowerCase first letter of the string.
     * @param str string to lower case first letter
     * @return string with first letter lower cased
     */
    public String lowerCaseFirstLetter(String str) {

        return (str == null || str.length() == 0) ? str : str.substring(0, 1).toLowerCase(Locale.ENGLISH)
                + str.substring(1);
    }

    /**
    * Transform string to capitalize first character of each word, change other
    * characters to lower case and remove spaces.
    * @param str String to be transformed
    * @return Capitalize First Letter of Each Word and remove spaces
    */
    public String titleCaseRemoveSpaces(String str) {

        return (str == null || str.length() == 0) ? str
                : String.join(EMPTY_STRING, Arrays.asList(str.trim().split(WHITESPACE_REGEX)).stream().map(
                        s -> toUpperCase(s.substring(0, 1)) + toLowerCase(s.substring(1)))
                        .collect(Collectors.toList()));
    }

    /**
     * Transform string to upper case.
     * @param obj Object representation of the string
     * @return string converted to upper case
     */
    public String toUpperCase(Object obj) {

        return (obj == null) ? EMPTY_STRING : obj.toString().toUpperCase(Locale.ENGLISH);
    }

    /**
     * Transform string to lower case.
     * @param obj Object representation of the string
     * @return string converted to lower case
     */
    public String toLowerCase(Object obj) {

        return (obj == null) ? EMPTY_STRING : obj.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * If type matches passed value.
     * @param type Elide model type object
     * @param options options object with type/string to match
     * @return template if matched
     * @throws IOException IOException
     */
    public CharSequence ifTypeMatches(Object type, Options options) throws IOException {

        String inputType = type.toString();
        String typeToMatch = options.param(0, null);
        return inputType.equals(typeToMatch) ? options.fn() : options.inverse();
    }

    /**
     * Get java type name corresponding to the Elide model type.
     * @param type Elide model type object
     * @return The corresponding java type name
     */
    public String getJavaType(Type type) {

        switch (type) {
            case BOOLEAN:
                return BOOLEAN;
            case COORDINATE:
                return STRING;
            case INTEGER:
                return LONG;
            case TEXT:
                return STRING;
            case TIME:
                return DATE;
            case DECIMAL:
                return BIGDECIMAL;
            case MONEY:
                return BIGDECIMAL;
            default:
                return STRING;
        }
    }
}
