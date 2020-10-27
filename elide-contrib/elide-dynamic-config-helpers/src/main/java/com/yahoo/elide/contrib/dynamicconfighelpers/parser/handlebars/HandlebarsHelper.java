/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;
import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Grain;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Type;
import com.github.jknack.handlebars.Options;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper class for handlebar template hydration.
 */
public class HandlebarsHelper {

    private static final String EMPTY_STRING = "";
    private static final String STRING = "String";
    private static final String DAY = "Day";
    private static final String HOUR = "Hour";
    private static final String ISOWEEK = "ISOWeek";
    private static final String MINUTE = "Minute";
    private static final String MONTH = "Month";
    private static final String QUARTER = "Quarter";
    private static final String SECOND = "Second";
    private static final String WEEK = "Week";
    private static final String YEAR = "Year";
    private static final String BIGDECIMAL = "BigDecimal";
    private static final String LONG = "Long";
    private static final String BOOLEAN = "Boolean";
    private static final String WHITESPACE_REGEX = "\\s+";
    private static final char SPACE = ' ';
    private static final char UNDERSCORE = '_';
    public static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{(.+?)}}");

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
     * Transform string to upper case and replace space with underscore.
     * @param obj Object representation of the string
     * @return string converted to upper case and spaces replaced
     */
    public String toUpperCaseReplaceSpace(Object obj) {
        return toUpperCase(obj).replace(SPACE, UNDERSCORE);
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
            case DECIMAL:
                return BIGDECIMAL;
            case MONEY:
                return BIGDECIMAL;
            default:
                return STRING;
        }
    }

    /**
     * Get java type name corresponding to the Grain type.
     * @param grain Grain object
     * @return The corresponding java type name
     */
    public String getGrainType(Grain grain) {

        switch (grain.getType()) {
            case DAY:
                return DAY;
            case HOUR:
                return HOUR;
            case ISOWEEK:
                return ISOWEEK;
            case MINUTE:
                return MINUTE;
            case MONTH:
                return MONTH;
            case QUARTER:
                return QUARTER;
            case SECOND:
                return SECOND;
            case WEEK:
                return WEEK;
            case YEAR:
                return YEAR;
            default:
                return DAY;
        }
    }

    /**
     * Flattens a collection to a comma separated string.
     * @param collection [A, B, C]
     * @return "\"A\",\"B\",\"C\""
     */
    public String collectionToString(Collection<String> collection) {
        return collection.stream()
                .map(item -> "\"" + item + "\"")
                .collect(Collectors.joining(","));
    }

    /**
     * Removes whitespace around column references.
     * @param str eg: {{ playerCountry.id}} = {{country_id}}
     * @return String without whitespace around column references eg: {{playerCountry.id}} = {{country_id}}
     */
    public String trimColumnReferences(String str) {
        Matcher matcher = REFERENCE_PARENTHESES.matcher(str);
        String expr = str;
        while (matcher.find()) {
            String reference = matcher.group(1);
            expr = expr.replace(reference, reference.trim());
        }
        return expr;
    }

    /**
     * Get Class Name for provided modelName from static models if available else default value.
     * @param modelName model name.
     * @return class name.
     */
    public String getJoinClassName(String modelName) {
        return ElideDynamicEntityCompiler.getStaticModelClassName(modelName, NO_VERSION,
                        capitalizeFirstLetter(modelName));
    }

    /**
     * Get Class Import for provided modelName from static models if available.
     * @param modelName model name.
     * @return import statement.
     */
    public String getJoinClassImport(String modelName) {
        return ElideDynamicEntityCompiler.getStaticModelClassImport(modelName, NO_VERSION, EMPTY_STRING);
    }
}
