/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.metadata.FormulaValidator;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.TableContext;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.Reference;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Subselect;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Table stores all resolved physical reference and join paths of all columns.
 */
public class SQLReferenceTable {
    public static final String PERIOD = ".";

    @Getter
    protected final MetaDataStore metaDataStore;

    @Getter
    protected final EntityDictionary dictionary;

    private final ExpressionParser parser;

    //Stores  MAP<Queryable, MAP<fieldName, Reference Tree>>
    protected final Map<Queryable, Map<String, List<Reference>>> referenceTree = new HashMap<>();

    protected final Map<Queryable, TableContext> globalTablesContext = new HashMap<>();

    public SQLReferenceTable(MetaDataStore metaDataStore) {
        this(metaDataStore,
             metaDataStore.getMetaData(ClassType.of(Table.class))
                .stream()
                .map(SQLTable.class::cast)
                .collect(Collectors.toSet()));
    }

    protected SQLReferenceTable(MetaDataStore metaDataStore, Set<Queryable> queryables) {
        this.metaDataStore = metaDataStore;
        this.dictionary = this.metaDataStore.getMetadataDictionary();
        this.parser = new ExpressionParser(metaDataStore);

        queryables
           .stream()
           // If Queryable is root, then its SQLTable.
           // We need to store references only for SQLTable and Nested Queries (Queryable -> Queryable -> SQLTable).
           // In case of Query -> SQLTable. Query doesn't know about all logical references.
           .filter(queryable -> queryable.isNested() || queryable.isRoot())
           .forEach(queryable -> {
               Queryable next = queryable;
               do {
                  resolveAndStoreAllReferencesAndJoins(next);
                  next = next.getSource();
               } while (next.isNested());
           });

        queryables
           .forEach(queryable -> {
               Queryable next = queryable;
               do {
                  initializeTableContext(next);
                  next = next.getSource();
               } while (!next.isRoot());
           });
    }

    /**
     * Get the Reference tree for provided field.
     * @param queryable Query / SQLTable
     * @param fieldName field name.
     * @return {@link Reference} Tree.
     */
    public List<Reference> getReferenceTree(Queryable queryable, String fieldName) {
        return referenceTree.get(queryable).getOrDefault(fieldName, new ArrayList<>());
    }

    public TableContext getGlobalTableContext(Queryable queryable) {
        return globalTablesContext.get(queryable);
    }

    private void initializeTableContext(Queryable queryable) {

        Queryable key = queryable.getSource();

        // Contexts are NOT stored by their sources.
        if (!globalTablesContext.containsKey(queryable)) {

            TableContext tableCtx = TableContext.builder()
                            .queryable(queryable)
                            .alias(key.getAlias())
                            .metaDataStore(metaDataStore)
                            .build();

            globalTablesContext.put(queryable, tableCtx);
        }
    }

    /**
     * Resolve all references and joins for a table and store them in this reference table.
     *
     * @param queryable meta data table
     */
    public void resolveAndStoreAllReferencesAndJoins(Queryable queryable) {

        //References and joins are stored by their source that produces them (rather than the query that asks for them).
        Queryable key = queryable.getSource();

        if (!referenceTree.containsKey(key)) {
            referenceTree.put(key, new HashMap<>());
        }

        FormulaValidator validator = new FormulaValidator(metaDataStore);

        queryable.getColumnProjections().forEach(column -> {
            // validate that there is no reference loop
            validator.parse(queryable, column);

            String fieldName = column.getName();
            referenceTree.get(key).put(fieldName, parser.parse(queryable, column));

        });
    }

    /**
     * Check whether a class is mapped to a subselect query instead of a physical table.
     *
     * @param cls The entity class
     * @return True if the class has {@link Subselect} annotation
     */
    public static boolean hasSql(Type<?> cls) {
        return cls.isAnnotationPresent(Subselect.class) || cls.isAnnotationPresent(FromSubquery.class);
    }

    /**
     * Maps an entity class to a physical table of subselect query, if neither {@link javax.persistence.Table}
     * nor {@link Subselect} annotation is present on this class, try {@link FromTable} and {@link FromSubquery}.
     *
     * @param cls The entity class.
     * @return The physical SQL table or subselect query.
     */
    public static String resolveTableOrSubselect(EntityDictionary dictionary, Type<?> cls) {
        if (hasSql(cls)) {
            if (cls.isAnnotationPresent(FromSubquery.class)) {
                return dictionary.getAnnotation(cls, FromSubquery.class).sql();
            }
            return dictionary.getAnnotation(cls, Subselect.class).value();
        }
        javax.persistence.Table table = dictionary.getAnnotation(cls, javax.persistence.Table.class);

        if (table != null) {
            return resolveTableAnnotation(table);
        }
        FromTable fromTable = dictionary.getAnnotation(cls, FromTable.class);

        return fromTable != null ? fromTable.name() : cls.getSimpleName();
    }

    /**
     * Get the full table name from JPA {@link javax.persistence.Table} annotation.
     *
     * @param table table annotation
     * @return <code>catalog.schema.name</code>
     */
    private static String resolveTableAnnotation(javax.persistence.Table table) {
        StringBuilder fullTableName = new StringBuilder();

        if (!"".equals(table.catalog())) {
            fullTableName.append(table.catalog()).append(".");
        }
        if (!"".equals(table.schema())) {
            fullTableName.append(table.schema()).append(".");
        }
        fullTableName.append(table.name());

        return fullTableName.toString();
    }

    /**
     * Split a string on ".", append quotes around each split and join it back.
     * eg: game.order_details to `game`.`order_details` .
     *
     * @param str column name / alias
     * @param beginQuote prefix char
     * @param endQuote suffix char
     * @return quoted string
     */
    private static String applyQuotes(String str, char beginQuote, char endQuote) {
        if (StringUtils.isBlank(str)) {
            return str;
        }
        if (str.contains(PERIOD)) {
            return beginQuote + str.trim().replace(PERIOD, endQuote + PERIOD + beginQuote) + endQuote;
        }
        return beginQuote + str.trim() + endQuote;
    }

    /**
     * Split a string on ".", append quotes around each split and join it back.
     * eg: game.order_details to `game`.`order_details` .
     *
     * @param str column name / alias
     * @param dialect Elide SQL dialect
     * @return quoted string
     */
    public static String applyQuotes(String str, SQLDialect dialect) {
        return applyQuotes(str, dialect.getBeginQuote(), dialect.getEndQuote());
    }
}
