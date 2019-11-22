/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.getClassAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;

import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;

/**
 * SQLColumn contains meta data about underlying physical table.
 */
public class SQLColumn extends Column {
    @Getter
    private final String columnName;

    @Getter
    private final String tableAlias;

    @Getter
    private final Path joinPath;

    private String fieldName;

    protected SQLColumn(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        super(tableClass, fieldName, dictionary);

        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(tableClass, JoinTo.class, fieldName);

        if (joinTo == null) {
            this.columnName = dictionary.getAnnotatedColumnName(tableClass, fieldName);
            this.tableAlias = getClassAlias(tableClass);
            this.joinPath = null;
        } else {
            Path path = new Path(tableClass, dictionary, joinTo.path());
            this.columnName = resolveJoinColumn(dictionary, path);
            this.tableAlias = getJoinTableAlias(path);
            this.joinPath = path;
        }
    }

    private static String getJoinTableAlias(Path path) {
        Path.PathElement last = path.lastElement().get();
        Class<?> lastClass = last.getType();

        return getClassAlias(lastClass);
    }

    /**
     * Returns a String that identifies this column in a physical sql table/view.
     *
     * @return e.g. <code>table_alias.column_name</code>
     */
    public String getReference() {
        if (joinPath == null) {
            return getTableAlias() + "." + getColumnName();
        }
        String suffix =  joinPath.getPathElements().get(0).getFieldName();
        return getTableAlias() + "_" + suffix + "." + getColumnName();
    }

    /**
     * Maps a logical entity path into a physical SQL column name.
     *
     * @param path entity join path
     * @return joined column name
     */
    public static String resolveJoinColumn(EntityDictionary dictionary, Path path) {
        Path.PathElement last = path.lastElement().get();
        Class<?> lastClass = last.getType();

        return dictionary.getAnnotatedColumnName(lastClass, last.getFieldName());
    }
}
