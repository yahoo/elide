/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql.query;

import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.core.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Pagination.Direction;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.request.Sorting.SortOrder;
import com.yahoo.elide.core.security.obfuscation.IdObfuscator;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.jpql.porting.Query;
import com.yahoo.elide.datastores.jpql.porting.Session;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Abstract class used to construct HQL queries.
 */
public abstract class AbstractHQLQueryBuilder {
    protected final Session session;
    protected final EntityDictionary dictionary;
    protected final CursorEncoder cursorEncoder;
    protected EntityProjection entityProjection;
    protected static final String SPACE = " ";
    protected static final String PERIOD = ".";
    protected static final String COMMA = ",";
    protected static final String FROM = " FROM ";
    protected static final String JOIN = " JOIN ";
    protected static final String LEFT = " LEFT";
    protected static final String FETCH = " FETCH ";
    protected static final String SELECT = "SELECT ";
    protected static final String AS = " AS ";
    protected static final String DISTINCT = "DISTINCT ";
    protected static final String WHERE = " WHERE ";

    protected static final boolean USE_ALIAS = true;
    protected static final boolean NO_ALIAS = false;
    protected Set<String> alreadyJoined = new HashSet<>();

    /**
     * Represents a relationship between two entities.
     */
    public interface Relationship {

        com.yahoo.elide.core.request.Relationship getRelationship();

        Type<?> getParentType();

        default Type<?> getChildType() {
            return getRelationship().getProjection().getType();
        }

        default String getRelationshipName() {
            return getRelationship().getName();
        }

        Object getParent();
    }

    public AbstractHQLQueryBuilder(EntityProjection entityProjection, EntityDictionary dictionary, Session session,
            CursorEncoder cursorEncoder) {
        this.session = session;
        this.dictionary = dictionary;
        this.entityProjection = entityProjection;
        this.cursorEncoder = cursorEncoder;
    }

    public abstract Query build();

    /**
     * Given a collection of filter predicates and a Hibernate query, populates the named parameters in the
     * Hibernate query.
     *
     * @param query The HQL query
     * @param predicates The predicates to extract named parameter values from
     */
    protected void supplyFilterQueryParameters(Query query, Collection<FilterPredicate> predicates) {
        for (FilterPredicate filterPredicate : predicates) {
            if (filterPredicate.getOperator().isParameterized()) {
                boolean shouldEscape = filterPredicate.isMatchingOperator();
                filterPredicate.getParameters().forEach(param ->
                    query.setParameter(param.getName(), shouldEscape ? param.escapeMatching() : param.getValue())
                );
            }
        }
    }

    private static String KEYSET_PARAMETER_PREFIX = "_keysetParameter_";

    /**
     * Adjusts the sorting for keyset pagination. When scrolling backwards the sort
     * order of the root entity needs to be reversed. Also if the sort on the id
     * field is missing this is also appended to the sort order.
     *
     * @param entityProjection the entity projection
     */
    protected void adjustSortingForKeysetPagination(EntityProjection entityProjection) {
        Pagination pagination = entityProjection.getPagination();
        Type<?> entityType = entityProjection.getType();
        // Add missing sorts
        Sorting sorting = entityProjection.getSorting();
        if (sorting == null || sorting.isDefaultInstance()) {
            if (Direction.BACKWARD.equals(pagination.getDirection())) {
                Map<String, Sorting.SortOrder> sortOrder = new LinkedHashMap<>();
                sortOrder.put("id", Sorting.SortOrder.desc);
                sorting = new SortingImpl(sortOrder, entityType, dictionary);
            } else {
                Map<String, Sorting.SortOrder> sortOrder = new LinkedHashMap<>();
                sortOrder.put("id", Sorting.SortOrder.asc);
                sorting = new SortingImpl(sortOrder, entityType, dictionary);
            }
            entityProjection.setSorting(sorting);
        } else {
            // When moving backwards need to amend sort order
            boolean hasIdSort = false;
            Map<String, Sorting.SortOrder> sortOrder = new LinkedHashMap<>();
            for (Entry<String, SortOrder> entry : ((SortingImpl) sorting).getSortRules().entrySet()) {
                if (Direction.BACKWARD.equals(pagination.getDirection())) {
                    if (!entry.getKey().contains(".")) {
                        // root sort adjust direction
                        sortOrder.put(entry.getKey(),
                                SortOrder.asc.equals(entry.getValue()) ? SortOrder.desc : SortOrder.asc);
                    } else {
                        sortOrder.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    // FORWARD
                    sortOrder.put(entry.getKey(), entry.getValue());
                }
                if ("id".equals(entry.getKey())) {
                    hasIdSort = true;
                }
            }
            if (!hasIdSort) {
                sortOrder.put("id", Direction.BACKWARD.equals(pagination.getDirection()) ? SortOrder.desc
                      : SortOrder.asc);
            }
            sorting = new SortingImpl(sortOrder, entityType, dictionary);
            entityProjection.setSorting(sorting);
        }
    }

    /**
     * Derives the keyset pagination clause.
     *
     * @param entityProjection the entity projection
     * @param dictionary the dictionary
     * @param entityAlias the entity alias
     * @return the keyset pagination clause
     */
    protected String getKeysetPaginationClause(EntityProjection entityProjection, EntityDictionary dictionary,
            String entityAlias) {
        Pagination pagination = entityProjection.getPagination();
        if (pagination != null && pagination.getDirection() != null) {
            adjustSortingForKeysetPagination(entityProjection);

            if (pagination.getBefore() != null || pagination.getAfter() != null) {
                Type<?> entityType = entityProjection.getType();
                String idField = dictionary.getIdFieldName(entityType);

                int index = 0;
                StringBuilder builder = new StringBuilder();
                if (Direction.BETWEEN.equals(pagination.getDirection())) {
                    builder.append("(");
                    index = getKeysetPaginationClauseFromPaginationSort(builder, entityProjection, dictionary, idField,
                            Direction.FORWARD, index, entityAlias);
                    builder.append(") AND (");
                    index = getKeysetPaginationClauseFromPaginationSort(builder, entityProjection, dictionary, idField,
                            Direction.BACKWARD, index, entityAlias);
                    builder.append(")");
                    return builder.toString();
                } else {
                    // Direction is forward even for the backward direction as the sorts have already been reversed
                    builder.append("(");
                    index = getKeysetPaginationClauseFromPaginationSort(builder, entityProjection, dictionary, idField,
                            Direction.FORWARD, index, entityAlias);
                    builder.append(")");
                    return builder.toString();
                }
            }
        }
        return "";
    }

    protected void supplyKeysetPaginationQueryParameters(Query query, EntityProjection entityProjection,
            EntityDictionary dictionary) {
        Pagination pagination = entityProjection.getPagination();
        if (pagination.getDirection() != null) {
            Map<String, String> after = null;
            Map<String, String> before = null;
            int index = 0;
            Type<?> entityType = entityProjection.getType();
            Sorting sorting = entityProjection.getSorting();

            String afterCursor = pagination.getAfter();
            String beforeCursor = pagination.getBefore();
            if (afterCursor != null) {
                after = cursorEncoder.decode(afterCursor);
            }
            if (beforeCursor != null) {
                before = cursorEncoder.decode(beforeCursor);
            }

            List<KeysetColumn> fields = getKeysetColumns(sorting);
            IdObfuscator idObfuscator = dictionary.getIdObfuscator();
            String entityIdFieldName = dictionary.getEntityIdFieldName(entityType);
            String idFieldName = null;
            if (idObfuscator != null || entityIdFieldName != null) {
                idFieldName = dictionary.getIdFieldName(entityType);
            }
            if (after != null && !after.isEmpty()) {
                for (KeysetColumn field : fields) {
                    if (entityIdFieldName != null && field.getColumn().equals(idFieldName)) {
                        // If the entity has entity id ignore id columns
                        continue;
                    }
                    Object value = getKeysetColumnValue(after, field, entityType, idFieldName, idObfuscator);
                    query.setParameter(KEYSET_PARAMETER_PREFIX + index++, value);
                }
            }
            if (before != null && !before.isEmpty()) {
                for (KeysetColumn field : fields) {
                    if (entityIdFieldName != null && field.getColumn().equals(idFieldName)) {
                        // If the entity has entity id ignore id columns
                        continue;
                    }
                    Object value = getKeysetColumnValue(before, field, entityType, idFieldName, idObfuscator);
                    query.setParameter(KEYSET_PARAMETER_PREFIX + index++, value);
                }
            }
        }
    }

    protected Object getKeysetColumnValue(Map<String, String> keyset, KeysetColumn field, Type<?> entityType,
            String idFieldName, IdObfuscator idObfuscator) {
        String keyValue = keyset.get(field.getColumn());
        Type<?> fieldType = dictionary.getType(entityType, field.getColumn());
        if (idObfuscator != null && field.getColumn().equals(idFieldName)) {
            return idObfuscator.deobfuscate(keyValue, fieldType);
        } else {
            return CoerceUtil.coerce(keyValue, fieldType);
        }
    }

    public static class KeysetColumn {
        private final String column;
        private final SortOrder sortOrder;

        public KeysetColumn(String column, SortOrder sortOrder) {
            this.column = column;
            this.sortOrder = sortOrder;
        }

        public String getColumn() {
            return column;
        }

        public SortOrder getSortOrder() {
            return sortOrder;
        }

        @Override
        public String toString() {
            return "KeysetColumn [column=" + column + ", sortOrder=" + sortOrder + "]";
        }
    }

    protected List<KeysetColumn> getKeysetColumns(Sorting sorting) {
        List<KeysetColumn> result = new ArrayList<>();
        for (Entry<Path, SortOrder> entry : sorting.getSortingPaths().entrySet()) {
            Path path = entry.getKey();
            if (path.getPathElements().size() == 1) {
                String column = entry.getKey().getFieldPath();
                result.add(new KeysetColumn(column, entry.getValue()));
            }
        }
        return result;
    }

    protected int getKeysetPaginationClauseFromPaginationSort(StringBuilder builder, EntityProjection entityProjection,
            EntityDictionary dictionary, String idField, Direction direction, int index, String entityAlias) {
        Sorting sorting = entityProjection.getSorting();
        List<KeysetColumn> keysetColumns = getKeysetColumns(sorting);
        for (int x = 0; x < keysetColumns.size(); x++) {
            KeysetColumn keysetColumn = keysetColumns.get(x);
            if (x != 0) {
                builder.append(" OR (");
                for (int y = 0; y < x; y++) {
                    KeysetColumn keysetColumnPrevious = keysetColumns.get(y);
                    builder.append(entityAlias + "." + keysetColumnPrevious.getColumn() + " =" + " :"
                            + KEYSET_PARAMETER_PREFIX + (index + y));
                    builder.append(" AND ");
                }
            }
            SortOrder order = keysetColumn.getSortOrder();
            if (Direction.BACKWARD.equals(direction)) {
                if (order == SortOrder.asc) {
                    order = SortOrder.desc;
                } else {
                    order = SortOrder.asc;
                }
            }
            String operator = SortOrder.asc.equals(order) ? ">" : "<";
            builder.append(entityAlias + "." + keysetColumn.getColumn() + " " + operator + " :"
                    + KEYSET_PARAMETER_PREFIX + (index + x));
            if (x != 0) {
                builder.append(")");
            }
        }
        return index + keysetColumns.size();
    }

    /**
     * Extracts all the HQL JOIN clauses from given filter expression.
     * @param filterExpression the filter expression to extract a join clause from
     * @return an HQL join clause
     */
    protected String getJoinClauseFromFilters(FilterExpression filterExpression) {
        return getJoinClauseFromFilters(filterExpression, false);
    }

    /**
     * Extracts all the HQL JOIN clauses from given filter expression.
     * @param filterExpression the filter expression to extract a join clause from
     * @param skipFetches JOIN but don't FETCH JOIN a relationship.
     * @return an HQL join clause
     */
    protected String getJoinClauseFromFilters(FilterExpression filterExpression, boolean skipFetches) {
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor(new ArrayList<>());
        Collection<FilterPredicate> predicates = filterExpression.accept(visitor);

        return predicates.stream()
                .map(predicate -> extractJoinClause(predicate.getPath(), skipFetches))
                .collect(Collectors.joining(SPACE));
    }


    /**
     * Extracts all the HQL JOIN clauses from given filter expression.
     * @param sorting the sort expression to extract a join clause from
     * @return an HQL join clause
     */
    protected String getJoinClauseFromSort(Sorting sorting) {
        return getJoinClauseFromSort(sorting, false);
    }

    /**
     * Extracts all the HQL JOIN clauses from given filter expression.
     * @param sorting the sort expression to extract a join clause from
     * @param skipFetches JOIN but don't FETCH JOIN a relationship.
     * @return an HQL join clause
     */
    protected String getJoinClauseFromSort(Sorting sorting, boolean skipFetches) {
        if (sorting != null && !sorting.isDefaultInstance()) {
            Map<Path, Sorting.SortOrder> validSortingRules = sorting.getSortingPaths();
            return validSortingRules.keySet().stream()
                    .map(path -> extractJoinClause(path, skipFetches))
                    .collect(Collectors.joining(SPACE));
        }
        return "";
    }


    /**
     * Modifies the HQL query to add OFFSET and LIMIT.
     * @param query The HQL query object
     */
    protected void addPaginationToQuery(Query query) {
        Pagination pagination = entityProjection.getPagination();
        if (pagination != null) {
            if (Direction.FORWARD.equals(pagination.getDirection())
                    || Direction.BACKWARD.equals(pagination.getDirection())) {
                // Keyset pagination
                // For keyset pagination adjust the limit by 1 to efficiently determine if there
                // is a next page in the forward direction or a previous page in the backward
                // direction
                query.setFirstResult(0);
                query.setMaxResults(pagination.getLimit() + 1);
            } else {
                // Offset pagination
                query.setFirstResult(pagination.getOffset());
                query.setMaxResults(pagination.getLimit());
            }
        }
    }

    /**
     * Extracts a join clause from a path (if it exists).
     * @param path The path to examine
     * @param skipFetches Don't fetch join
     * @return A HQL string representing the join
     */
    private String extractJoinClause(Path path, boolean skipFetches) {
        StringBuilder joinClause = new StringBuilder();

        String previousAlias = null;

        for (Path.PathElement pathElement : path.getPathElements()) {
            String fieldName = pathElement.getFieldName();
            Type<?> typeClass = dictionary.lookupEntityClass(pathElement.getType());
            String typeAlias = getTypeAlias(typeClass);

            // Nothing left to join.
            if (! dictionary.isRelation(pathElement.getType(), fieldName)) {
                return joinClause.toString();
            }

            String alias = previousAlias == null
                    ? appendAlias(typeAlias, fieldName)
                    : appendAlias(previousAlias, fieldName);

            String joinKey;

            //This is the first path element
            if (previousAlias == null) {
                joinKey = typeAlias + PERIOD + fieldName;
            } else {
                joinKey = previousAlias + PERIOD + fieldName;
            }

            String fetch = "";
            RelationshipType type = dictionary.getRelationshipType(pathElement.getType(), fieldName);

            //This is a to-One relationship belonging to the collection being retrieved.
            if (!skipFetches && entityProjection.getIncludedRelationsName().contains(fieldName) && type.isToOne()
                    && !type.isComputed() && previousAlias == null) {
                fetch = "FETCH ";

            }
            String joinFragment = LEFT + JOIN + fetch + joinKey + SPACE + alias + SPACE;

            if (!alreadyJoined.contains(joinKey)) {
                joinClause.append(joinFragment);
                alreadyJoined.add(joinKey);
            }

            previousAlias = alias;
        }

        return joinClause.toString();
    }

    /**
     * Builds a JOIN clause that eagerly fetches to-one relationships that Hibernate needs to hydrate.
     * @param entityClass The entity class that is being queried in the HQL query.
     * @param alias The HQL alias for the entity class.
     * @return The JOIN clause that can be added to the FROM clause.
     */
    protected String extractToOneMergeJoins(Type<?> entityClass, String alias) {
        return extractToOneMergeJoins(entityClass, alias, (unused) -> false);
    }

    protected String extractToOneMergeJoins(Type<?> entityClass, String alias,
                                            Predicate<String> skipRelation) {
        List<String> relationshipNames = dictionary.getRelationships(entityClass);
        StringBuilder joinString = new StringBuilder("");
        for (String relationshipName : relationshipNames) {
            if (!entityProjection.getIncludedRelationsName().contains(relationshipName)) {
                continue;
            }
            RelationshipType type = dictionary.getRelationshipType(entityClass, relationshipName);
            if (type.isToOne() && !type.isComputed()) {
                if (skipRelation.test(relationshipName)) {
                    continue;
                }
                String joinKey = alias + PERIOD + relationshipName;

                if (alreadyJoined.contains(joinKey)) {
                    continue;
                }

                joinString.append(" LEFT JOIN FETCH ");
                joinString.append(joinKey);
                joinString.append(SPACE);
                alreadyJoined.add(joinKey);
            }
        }
        return joinString.toString();
    }

    /**
     * Returns a sorting object into a HQL ORDER BY string.
     * @param sorting The sorting object passed from the client
     * @return The sorting clause
     */
    protected String getSortClause(final Sorting sorting) {
        String sortingRules = "";
        if (sorting != null && !sorting.isDefaultInstance()) {
            final Map<Path, Sorting.SortOrder> validSortingRules = sorting.getSortingPaths();
            if (!validSortingRules.isEmpty()) {
                final List<String> ordering = new ArrayList<>();
                // pass over the sorting rules
                validSortingRules.forEach((path, order) -> {

                    Type<?> typeClass = dictionary.lookupEntityClass(path.getPathElements().get(0).getType());
                    String prefix = getTypeAlias(typeClass);

                    for (Path.PathElement pathElement : path.getPathElements()) {
                        String fieldName = pathElement.getFieldName();

                        if (dictionary.isComplexAttribute(pathElement.getType(), fieldName)
                                || !dictionary.isRelation(pathElement.getType(), fieldName)) {
                            prefix = prefix + PERIOD + fieldName;
                        } else {
                            prefix = appendAlias(prefix, fieldName);
                        }
                    }
                    ordering.add(prefix + SPACE
                            + (order.equals(Sorting.SortOrder.desc) ? "desc" : "asc"));
                });
                sortingRules = " order by " + StringUtils.join(ordering, COMMA);
            }
        }
        return sortingRules;
    }

    /**
     * Returns whether filter expression contains toMany relationship.
     * @param filterExpression
     * @return true or false
     */
    protected boolean containsOneToMany(FilterExpression filterExpression) {
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor(new ArrayList<>());
        Collection<FilterPredicate> predicates = filterExpression.accept(visitor);

        return predicates.stream()
                .anyMatch(predicate -> FilterPredicate.toManyInPath(dictionary, predicate.getPath()));
    }
}
