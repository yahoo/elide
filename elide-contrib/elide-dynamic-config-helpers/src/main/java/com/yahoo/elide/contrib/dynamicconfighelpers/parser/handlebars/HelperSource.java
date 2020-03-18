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

public class HelperSource {

    private static final String EMPTY_STRING = "";
    private static final String STRING = "String";
    private static final String INTEGER = "Integer";
    private static final String DATE = "Date";
    private static final String FLOAT = "Double";
    private static final String LONG = "Long";
    private static final String BOOLEAN = "Boolean";
    private static final String SPACE = " ";
    private static final String FOUR_SPACES = "    ";
    private static final String EIGHT_SPACES = FOUR_SPACES + FOUR_SPACES;
    private static final String NEWLINE = "\n";

    public String capitalizeFirstLetter(String str) {

        return (str == null || str.length() == 0) ? str : str.substring(0, 1).toUpperCase(Locale.ENGLISH)
                + str.substring(1);
    }

    public String toUpperCase(Object obj) {

        return (obj == null) ? EMPTY_STRING : obj.toString().toUpperCase(Locale.ENGLISH);
    }

    public String toLowerCase(Object obj) {

        return (obj == null) ? EMPTY_STRING : obj.toString().toLowerCase(Locale.ENGLISH);
    }

    public CharSequence ifContains(String str, Options options) throws IOException {

        String toMatch = options.param(0, null);
        return options.isFalsy(contains(str, toMatch)) ? options.inverse() : options.fn();
    }

    public CharSequence ifDimTypeMatches(Dimension dim, Options options) throws IOException {

        String dimDataType = dim.getType().toString();
        String typeToMatch = options.param(0, null);
        boolean result = new EqualsBuilder().append(dimDataType, typeToMatch).isEquals();
        return result ? options.fn() : options.inverse();
    }

    public boolean contains(String str, String search) {

        return (str == null || str.indexOf(search) < 0);
    }

    public String generateSetterMethod(String type, String fieldName) {

        return "public void set" + capitalizeFirstLetter(fieldName) + "(" + type + SPACE + fieldName + ") {" + NEWLINE
                        + EIGHT_SPACES + "this." + fieldName + " = " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    public String generateDimSetterMethod(Dimension dim) {

        String type = getDimensonType(dim);
        String fieldName = dim.getName();
        return "public void set" + capitalizeFirstLetter(fieldName) + "(" + type + SPACE + fieldName + ") {" + NEWLINE
                        + EIGHT_SPACES + "this." + fieldName + " = " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    public String generateJoinSetterMethod(Join join) {

        String type = capitalizeFirstLetter(join.getTo());
        String fieldName = join.getTo();
        return "public void set" + capitalizeFirstLetter(fieldName) + "(" + type + SPACE + fieldName + ") {" + NEWLINE
                        + EIGHT_SPACES + "this." + fieldName + " = " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    public String generateGetterMethod(String type, String fieldName) {

        return "public " + type + " get" + capitalizeFirstLetter(fieldName) + "() {" + NEWLINE + EIGHT_SPACES
                        + "return " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    public String generateDimGetterMethod(Dimension dim) {

        String type = getDimensonType(dim);
        String fieldName = dim.getName();
        return "public " + type + " get" + capitalizeFirstLetter(fieldName) + "() {" + NEWLINE + EIGHT_SPACES
                        + "return " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    public String generateJoinGetterMethod(Join join) {

        String type = capitalizeFirstLetter(join.getTo());
        String fieldName = join.getTo();
        return "public " + type + " get" + capitalizeFirstLetter(fieldName) + "() {" + NEWLINE + EIGHT_SPACES
                        + "return " + fieldName + ";" + NEWLINE + FOUR_SPACES + "}";
    }

    public String getDimensonType(Dimension dim) {

        switch (dim.getType()) {
            case BOOLEAN:
                return BOOLEAN;
            case COORDINATE:
                return STRING;
            case INTEGER:
                return INTEGER;
            case TEXT:
                return STRING;
            case TIME:
                return DATE;
            case DECIMAL:
                return FLOAT;
            case MONEY:
                return LONG;
            default:
                return STRING;
        }
    }
}
