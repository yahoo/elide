/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite.SyntaxVerifier;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.HasJoinVisitor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.PhysicalReferenceExtractor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.Reference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Column projection that can expand the column into a SQL projection fragment.
 */
public interface SQLColumnProjection extends ColumnProjection {
    public static Logger LOGGER = LoggerFactory.getLogger(SQLColumnProjection.class);

    /**
     * Generate a SQL fragment for this combination column and client arguments.
     * @param source the queryable that contains the column.
     * @param lookupTable symbol table to resolve column name references.
     * @return SQL query String for this column
     */
    default String toSQL(Queryable source, SQLReferenceTable lookupTable) {
        return lookupTable.getResolvedReference(source, getName());
    }

    @Override
    default boolean canNest(Queryable source, SQLReferenceTable lookupTable) {
        SQLDialect dialect = source.getConnectionDetails().getDialect();
        String sql = toSQL(source.getSource(), lookupTable);

        SyntaxVerifier verifier = new SyntaxVerifier(dialect);
        boolean canNest = verifier.verify(sql);
        if (! canNest) {
            LOGGER.debug("Unable to nest {} because {}", this.getName(), verifier.getLastError());
        }

        return canNest;
    }

    @Override
    default Pair<ColumnProjection, Set<ColumnProjection>> nest(Queryable source,
                                                              SQLReferenceTable lookupTable,
                                                              boolean joinInOuter) {

        MetaDataStore store = lookupTable.getMetaDataStore();
        List<Reference> references = new ExpressionParser(store).parse(source, getExpression());

        boolean requiresJoin = references.stream().anyMatch(ref -> ref.accept(new HasJoinVisitor()));

        boolean inProjection = source.getColumnProjection(getName(), getArguments()) != null;

        ColumnProjection outerProjection;
        Set<ColumnProjection> innerProjections = new LinkedHashSet<>();

        if (requiresJoin && joinInOuter) {
            outerProjection = withExpression(getExpression(), inProjection);

            innerProjections = references.stream()
                    .map(ref -> ref.accept(new PhysicalReferenceExtractor(store)))
                    .flatMap(Set::stream)
                    .map(ref -> SQLPhysicalColumnProjection.builder()
                            .name(ref.getName())
                            .build())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            outerProjection = withExpression("{{$" + this.getSafeAlias() + "}}", isProjected());
            innerProjections = new LinkedHashSet<>(Arrays.asList(this));
        }

        return Pair.of(outerProjection, innerProjections);
    }

    SQLColumnProjection withExpression(String expression, boolean project);

    /**
     * Returns whether or not this column is projected in the output (included in SELECT) or
     * only referenced in a filter expression.
     * @return True if part of the output projection.  False otherwise.
     */
    default boolean isProjected() {
        return true;
    }
}
