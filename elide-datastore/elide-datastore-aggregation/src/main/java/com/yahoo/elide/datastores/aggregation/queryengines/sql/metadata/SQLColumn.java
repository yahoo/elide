/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.generateColumnReference;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.getClassAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.SQLExpression;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLColumn contains meta data about underlying physical table.
 */
public class SQLColumn extends Column {
    @Getter
    private final String reference;

    @Getter
    private final Path joinPath;

    protected SQLColumn(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        super(tableClass, fieldName, dictionary);

        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(tableClass, JoinTo.class, fieldName);
        SQLExpression sqlExpr = dictionary.getAttributeOrRelationAnnotation(
                tableClass, SQLExpression.class, fieldName);

        if (joinTo == null || joinTo.path().equals("")) {
            String physicalReference = getClassAlias(tableClass)
                    + "." + dictionary.getAnnotatedColumnName(tableClass, fieldName);

            this.reference = sqlExpr == null || "".equals(sqlExpr.value())
                    ? physicalReference
                    : sqlExpr.value().replace("%reference", physicalReference);

            this.joinPath = null;
        } else {
            Path path = new Path(tableClass, dictionary, joinTo.path());
            List<String> expressions = new ArrayList<>();

            if (sqlExpr != null && !"".equals(sqlExpr.value())) {
                expressions.add(sqlExpr.value());
            }

            this.reference = generateColumnReference(path, dictionary, expressions);
            this.joinPath = path;
        }
    }
}
