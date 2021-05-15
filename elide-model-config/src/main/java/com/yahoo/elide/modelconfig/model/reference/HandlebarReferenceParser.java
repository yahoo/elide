/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.model.reference;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromString;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses dimension, measure or join definition to provide list of relevant {@link HandlebarReference}.
 */
public class HandlebarReferenceParser {

    private static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{(.+?)}}");
    private static final String SQL_HELPER_PREFIX = "sql ";
    public static final String PERIOD = ".";

    private static final String COLUMN_ARGS_PREFIX = "$$column.args.";
    private static final String TABLE_ARGS_PREFIX = "$$table.args.";
    private static final String COLUMN_EXPR = "$$column.expr.";

    private final Handlebars handlebars = new Handlebars()
            .with(EscapingStrategy.NOOP)
            .registerHelper("sql", (context, options) -> {
                String from = options.hash("from");
                String column = options.hash("column");

                // Prefix column with join table name
                return StringUtils.isEmpty(from) ? column : from + PERIOD + column;
            });

    public List<HandlebarReference> parse(String expression) {
        List<HandlebarReference> references = new ArrayList<>();

        List<String> referenceNames = resolveFormulaReferences(expression);

        for (String referenceName : referenceNames) {

            Set<String> fixedArguments = new HashSet<>();

            // Change `sql from='joinName' column='columnName[a1:v1][a2:v2]'` to `joinName.columnName`
            if (referenceName.startsWith(SQL_HELPER_PREFIX)) {
                try {
                    Template template = handlebars.compileInline(toFormulaReference(referenceName));
                    referenceName = template.apply(Collections.emptyMap());
                    int argsIndex = referenceName.indexOf('[');
                    if (argsIndex >= 0) {
                        fixedArguments = getArgumentMapFromString(referenceName.substring(argsIndex)).keySet();
                        referenceName = referenceName.substring(0, argsIndex);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }

            if (referenceName.startsWith(COLUMN_ARGS_PREFIX)) {
                references.add(ColumnArgReference.builder()
                                .argName(referenceName.substring(COLUMN_ARGS_PREFIX.length()))
                                .build());
                continue;
            }

            if (referenceName.startsWith(TABLE_ARGS_PREFIX)) {
                references.add(TableArgReference.builder()
                                .argName(referenceName.substring(TABLE_ARGS_PREFIX.length()))
                                .build());
                continue;
            }

            // ignore $$column.expr
            if (referenceName.equals(COLUMN_EXPR)) {
                continue;
            }

            // ignore physical references
            if (referenceName.lastIndexOf('$') == 0) {
                continue;
            }

            // must be another column in same table or join table
            references.add(ColumnReference.builder()
                            .name(referenceName)
                            .fixedArguments(fixedArguments)
                            .build());
        }

        return references;
    }

    private static List<String> resolveFormulaReferences(String expression) {

        List<String> references = new ArrayList<>();

        if (StringUtils.isNotBlank(expression)) {
            Matcher matcher = REFERENCE_PARENTHESES.matcher(expression);
            while (matcher.find()) {
                references.add(matcher.group(1).trim());
            }
        }

        return references;
    }

    private static String toFormulaReference(String reference) {
        return "{{" + reference + "}}";
    }
}
