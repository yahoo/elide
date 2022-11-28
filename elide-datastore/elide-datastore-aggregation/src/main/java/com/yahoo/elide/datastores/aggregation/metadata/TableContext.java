/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.TBL_PREFIX;

import com.yahoo.elide.core.request.Argument;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Formatter;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Template;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Context for resolving table arguments in provided expression.
 */
@Getter
@ToString
@Builder
public class TableContext extends HashMap<String, Object> {

    protected final Map<String, Argument> tableArguments;

    private final Handlebars handlebars = new Handlebars()
                    .with(EscapingStrategy.NOOP)
                    .with((Formatter) (value, next) -> {
                        if (value instanceof com.yahoo.elide.core.request.Argument) {
                            return ((com.yahoo.elide.core.request.Argument) value).getValue();
                        }
                        return next.format(value);
                    });

    public Object get(Object key) {

        if (key.equals(TBL_PREFIX)) {
            return TableSubContext.tableSubContextBuilder()
                            .tableArguments(this.tableArguments)
                            .build();
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }

    /**
     * Resolves the handlebars with in expression.
     * @param expression Expression to resolve.
     * @return fully resolved expression.
     * @throws IllegalStateException for any handlebars error.
     */
    public String resolve(String expression) {
        try {
            Template template = handlebars.compileInline(expression);
            return template.apply(this);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
}
