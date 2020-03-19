/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.Dimension;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Join;

import com.github.jknack.handlebars.Options;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.IOException;
import java.util.Locale;

/**
 * Helper class for handlebar template hydration.
 */
public class HandlebarsHelper {

    private static final String EMPTY_STRING = "";
    private static final String STRING = "String";
    private static final String DATE = "Date";
    private static final String DOUBLE = "Double";
    private static final String LONG = "Long";
    private static final String BOOLEAN = "Boolean";
    private static final String SPACE = " ";
    private static final String FOUR_SPACES = "    ";
    private static final String EIGHT_SPACES = FOUR_SPACES + FOUR_SPACES;
    private static final String NEWLINE = "\n";

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
     * If template matches a string.
     * @param str String representation of the template
     * @param options options object with string to match
     * @return template if matched
     * @throws IOException
     */
    public CharSequence ifContains(String str, Options options) throws IOException {

        String toMatch = options.param(0, null);
        return options.isFalsy(contains(str, toMatch)) ? options.inverse() : options.fn();
    }

    /**
     * If type of dimension matches passed value.
     * @param dim Elide dimension object
     * @param options options object with type/string to match
     * @return template if matched
     * @throws IOException
     */
    public CharSequence ifDimTypeMatches(Dimension dim, Options options) throws IOException {

        String dimDataType = dim.getType().toString();
        String typeToMatch = options.param(0, null);
        boolean result = new EqualsBuilder().append(dimDataType, typeToMatch).isEquals();
        return result ? options.fn() : options.inverse();
    }

    /**
     * If string contains passed value.
     * @param str String to be searched
     * @param search pattern to search
     * @return true/false if matched
     */
    public boolean contains(String str, String search) {

        return (str == null || str.indexOf(search) < 0);
    }

    /**
     * Generate setter method.
     * @param type Type of the field
     * @param fieldName Field Name
     * @return setter method
     */
    public String generateSetterMethod(String type, String fieldName) {

        return "public void set" + capitalizeFirstLetter(fieldName) + "(" + type + SPACE + fieldName + ") {" + NEWLINE
                        + EIGHT_SPACES + "this." + fieldName + " = " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    /**
     * Generate setter method for Dimension Type.
     * @param dim Dimension Object
     * @return setter method
     */
    public String generateDimSetterMethod(Dimension dim) {

        String type = getDimensionType(dim);
        String fieldName = dim.getName();
        return "public void set" + capitalizeFirstLetter(fieldName) + "(" + type + SPACE + fieldName + ") {" + NEWLINE
                        + EIGHT_SPACES + "this." + fieldName + " = " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    /**
     * Generate setter method for Join Type.
     * @param join Join Object
     * @return setter method
     */
    public String generateJoinSetterMethod(Join join) {

        String type = capitalizeFirstLetter(join.getTo());
        String fieldName = join.getName();
        return "public void set" + capitalizeFirstLetter(fieldName) + "(" + type + SPACE + fieldName + ") {" + NEWLINE
                        + EIGHT_SPACES + "this." + fieldName + " = " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    /**
     * Generate getter method.
     * @param type Type of the field
     * @param fieldName field Name
     * @return getter method
     */
    public String generateGetterMethod(String type, String fieldName) {

        return "public " + type + " get" + capitalizeFirstLetter(fieldName) + "() {" + NEWLINE + EIGHT_SPACES
                        + "return " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    /**
     * Generate getter method for Dimension type.
     * @param dim Dimension Object
     * @return getter method
     */
    public String generateDimGetterMethod(Dimension dim) {

        String type = getDimensionType(dim);
        String fieldName = dim.getName();
        return "public " + type + " get" + capitalizeFirstLetter(fieldName) + "() {" + NEWLINE + EIGHT_SPACES
                        + "return " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    /**
     * Generate getter method for Join type.
     * @param join Join Object
     * @return getter method
     */
    public String generateJoinGetterMethod(Join join) {

        String type = capitalizeFirstLetter(join.getTo());
        String fieldName = join.getName();
        return "public " + type + " get" + capitalizeFirstLetter(fieldName) + "() {" + NEWLINE + EIGHT_SPACES
                        + "return " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    /**
     * Get java type name corresponding to the dimension type.
     * @param dim Dimension Object
     * @return The corresponding java type name
     */
    public String getDimensionType(Dimension dim) {

        switch (dim.getType()) {
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
                return DOUBLE;
            case MONEY:
                return DOUBLE;
            default:
                return STRING;
        }
    }
}
