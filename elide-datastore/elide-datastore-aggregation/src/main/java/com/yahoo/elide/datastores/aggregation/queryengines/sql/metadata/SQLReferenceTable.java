/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable.isTableJoin;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.JoinType;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.FormulaValidator;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.TableContext;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Subselect;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    //Stores  MAP<Queryable, MAP<fieldName, reference>>
    protected final Map<Queryable, Map<String, String>> resolvedReferences = new HashMap<>();

    //Stores  MAP<Queryable, MAP<fieldName, join expression>>
    protected final Map<Queryable, Map<String, Set<String>>> resolvedJoinExpressions = new HashMap<>();

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

        queryables
           .stream()
           // If Queryable is root, then its SQLTable.
           // We need to store references only for SQLTable and Nested Queries (Queryable -> Queryable -> SQLTable).
           // In case of Query -> SQLTable. Query doesn't know about all logical references.
           .filter(queryable -> queryable.isNested() || queryable.isRoot())
           .forEach(queryable -> {
               Queryable next = queryable;
               do {
                  initializeTableContext(next);
                  resolveAndStoreAllReferencesAndJoins(next);
                  next = next.getSource();
               } while (next.isNested());
           });
    }

    /**
     * Get the resolved physical SQL reference for a field from storage.
     *
     * @param queryable table class
     * @param fieldName field name
     * @return resolved reference
     */
    public String getResolvedReference(Queryable queryable, String fieldName) {
        return resolvedReferences.get(queryable).get(fieldName);
    }

    /**
     * Get the resolved ON clause expression for a field from referred table.
     *
     * @param queryable table class
     * @param fieldName field name
     * @return resolved ON clause expression
     */
    public Set<String> getResolvedJoinExpressions(Queryable queryable, String fieldName) {
        return resolvedJoinExpressions.get(queryable).getOrDefault(fieldName, new HashSet<>());
    }

    public TableContext getGlobalTableContext(Queryable queryable) {
        return globalTablesContext.get(queryable);
    }

    private void initializeTableContext(Queryable queryable) {

        // Contexts are stored by their source that produces them
        Queryable key = queryable.getSource();
        boolean isNested = queryable.isNested();

        if (!globalTablesContext.containsKey(key)) {

            TableContext tableCtx = TableContext.builder()
                            .queryable(queryable)
                            .alias(key.getAlias())
                            .metaDataStore(metaDataStore)
                            .build();

            queryable.getColumnProjections().forEach(column -> {
                if (!isNested && column.getColumnType() == ColumnType.FIELD) {
                    tableCtx.put(column.getName(), "{{$" + column.getExpression() + "}}");
                } else {
                    tableCtx.put(column.getName(), column.getExpression());
                }
            });

            queryable.getJoins().forEach((name, join) -> {
                SQLTable joinTable = metaDataStore.getTable(join.getJoinTableType());
                tableCtx.addJoin(name, joinTable);
            });

            globalTablesContext.put(key, tableCtx);
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
        SQLDialect dialect = queryable.getSource().getConnectionDetails().getDialect();
        if (!resolvedReferences.containsKey(key)) {
            resolvedReferences.put(key, new HashMap<>());
        }

        if (!resolvedJoinExpressions.containsKey(key)) {
            resolvedJoinExpressions.put(key, new HashMap<>());
        }

        FormulaValidator validator = new FormulaValidator(metaDataStore);
        SQLJoinVisitor joinVisitor = new SQLJoinVisitor(metaDataStore);

        queryable.getColumnProjections().forEach(column -> {
            // validate that there is no reference loop
            validator.parse(queryable, column);

            String fieldName = column.getName();

            resolvedReferences.get(key).put(
                    fieldName,
                    new SQLReferenceVisitor(metaDataStore, key.getAlias(), dialect)
                            .visitColumn(queryable, column));

            Set<JoinPath> joinPaths = joinVisitor.visitColumn(queryable, column);
            resolvedJoinExpressions.get(key).put(fieldName, getJoinClauses(queryable.getSource().getAlias(),
                    joinPaths, dialect));
        });
    }


    /**
     * Create a set of join expressions from join paths.
     *
     * @param parentAlias the parent alias
     * @param joinPaths paths that require joins
     * @return A set of join expressions
     */
    private Set<String> getJoinClauses(String parentAlias, Set<JoinPath> joinPaths, SQLDialect dialect) {
        Set<String> joinExpressions = new LinkedHashSet<>();
        joinPaths.forEach(path -> addJoinClauses(parentAlias, path, joinExpressions, dialect));
        return joinExpressions;
    }

    /**
     * Add a join clause to a set of join clauses.
     *
     * @param parentAlias the parent alias
     * @param joinPath join path
     * @param alreadyJoined A set of joins that have already been computed.
     */
    private void addJoinClauses(String parentAlias, JoinPath joinPath, Set<String> alreadyJoined, SQLDialect dialect) {

        for (Path.PathElement pathElement : joinPath.getPathElements()) {
            String fieldName = pathElement.getFieldName();
            Type<?> parentClass = pathElement.getType();

            // Nothing left to join.
            if (!dictionary.isRelation(parentClass, fieldName) && !isTableJoin(metaDataStore, parentClass, fieldName)) {
                return;
            }

            String joinFragment =
                            extractJoinClause(parentClass, parentAlias, pathElement.getFieldType(), fieldName, dialect);

            alreadyJoined.add(joinFragment);

            parentAlias = appendAlias(parentAlias, fieldName);
        }
    }

    /**
     * Build a single dimension join clause for joining a relationship/join table to the parent table.
     *
     * @param fromClass parent class
     * @param fromAlias parent table alias
     * @param joinClass relationship/join class
     * @param joinField relationship/join field name
     * @return built join clause i.e. <code>LEFT JOIN table1 AS dimension1 ON table0.dim_id = dimension1.id</code>
     */
    private String extractJoinClause(Type<?> fromClass,
                                     String fromAlias,
                                     Type<?> joinClass,
                                     String joinField,
                                     SQLDialect dialect) {

        String joinAlias = appendAlias(fromAlias, joinField);
        String joinColumnName = dictionary.getAnnotatedColumnName(fromClass, joinField);

        // resolve the right hand side of JOIN
        String joinSource = constructTableOrSubselect(joinClass, dialect);

        SQLTable table = metaDataStore.getTable(fromClass);
        SQLJoin join = table.getJoin(joinField);

        String joinKeyword = join == null
                ? dialect.getJoinKeyword(JoinType.LEFT)
                : dialect.getJoinKeyword(join.getJoinType());

        if (join != null && join.getJoinType().equals(JoinType.CROSS)) {
            return String.format("%s %s AS %s",
                    joinKeyword,
                    joinSource,
                    applyQuotes(joinAlias, dialect));
        }

        String joinClause = join == null
                ? String.format(
                        "%s.%s = %s.%s",
                        fromAlias,
                        joinColumnName,
                        joinAlias,
                        dictionary.getAnnotatedColumnName(
                                joinClass,
                                dictionary.getIdFieldName(joinClass)))
                : getJoinClause(fromClass, fromAlias, join.getJoinExpression(), dialect);

        return String.format("%s %s AS %s ON %s",
                joinKeyword,
                joinSource,
                applyQuotes(joinAlias, dialect),
                joinClause);
    }

    /**
     * Resolve references to construct a join ON clause.
     *
     * @param fromClass parent class
     * @param fromAlias parent alias
     * @param expr unresolved ON clause
     * @return string resolved ON clause
     */
    private String getJoinClause(Type<?> fromClass, String fromAlias, String expr, SQLDialect dialect) {
        SQLTable table = new SQLTable(fromClass, dictionary);
        SQLReferenceVisitor visitor =
                        new SQLReferenceVisitor(metaDataStore, fromAlias, dialect);

        return visitor.visitFormulaDimension(table, new SQLColumnProjection() {
            @Override
            public SQLColumnProjection withExpression(String expression, boolean project) {
                return null;
            }

            @Override
            public ValueType getValueType() {
                return null;
            }
            @Override
            public String getName() {
                return null;
            }
            @Override
            public String getExpression() {
                return expr;
            }
            @Override
            public ColumnType getColumnType() {
                return null;
            }
        });
    }

    /**
     * Make a select statement for a table a sub select query.
     *
     * @param cls entity class
     * @return <code>tableName</code> or <code>(subselect query)</code>
     */
    private String constructTableOrSubselect(Type<?> cls, SQLDialect dialect) {
        return isSubselect(cls)
                ? "(" + resolveTableOrSubselect(dictionary, cls) + ")"
                : applyQuotes(resolveTableOrSubselect(dictionary, cls), dialect);
    }

    /**
     * Check whether a class is mapped to a subselect query instead of a physical table.
     *
     * @param cls The entity class
     * @return True if the class has {@link Subselect} annotation
     */
    private static boolean isSubselect(Type<?> cls) {
        return cls.isAnnotationPresent(Subselect.class) || cls.isAnnotationPresent(FromSubquery.class);
    }

    /**
     * Maps an entity class to a physical table of subselect query, if neither {@link javax.persistence.Table}
     * nor {@link Subselect} annotation is present on this class, try {@link FromTable} and {@link FromSubquery}.
     *
     * @param cls The entity class.
     * @return The physical SQL table or subselect query.
     */
    private static String resolveTableOrSubselect(EntityDictionary dictionary, Type<?> cls) {
        if (isSubselect(cls)) {
            if (cls.isAnnotationPresent(FromSubquery.class)) {
                return dictionary.getAnnotation(cls, FromSubquery.class).sql();
            } else {
                return dictionary.getAnnotation(cls, Subselect.class).value();
            }
        } else {
            javax.persistence.Table table = dictionary.getAnnotation(cls, javax.persistence.Table.class);

            if (table != null) {
                return resolveTableAnnotation(table);
            } else {
                FromTable fromTable = dictionary.getAnnotation(cls, FromTable.class);

                return fromTable != null ? fromTable.name() : cls.getSimpleName();
            }
        }
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
