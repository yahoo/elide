/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.query;

import com.paiondata.elide.datastores.aggregation.metadata.ColumnContext;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.metadata.PhysicalRefColumnContext;
import com.paiondata.elide.datastores.aggregation.query.ColumnProjection;
import com.paiondata.elide.datastores.aggregation.query.Queryable;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.calcite.SyntaxVerifier;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.ColumnArgReference;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.HasJoinVisitor;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.JoinReference;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.PhysicalReference;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.Reference;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.ReferenceExtractor;
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
     * @param query current Queryable.
     * @param metaDataStore MetaDataStore.
     * @return SQL query String for this column
     */
    default String toSQL(Queryable query, MetaDataStore metaDataStore) {

        ColumnContext context = ColumnContext.builder()
                        .queryable(query)
                        .alias(query.getSource().getAlias())
                        .metaDataStore(metaDataStore)
                        .column(this)
                        .tableArguments(query.getArguments())
                        .build();

        return context.resolve(getExpression());
    }

    /**
     * Generate a partially resolved string for current column's expression. Fully resolves arguments and logical
     * references but keeps physical and join references as is.
     * @param query current Queryable.
     * @param metaDataStore MetaDataStore.
     * @return A String with all arguments and logical references resolved.
     */
    default String toPhysicalReferences(Queryable query, MetaDataStore metaDataStore) {

        PhysicalRefColumnContext context = PhysicalRefColumnContext.physicalRefContextBuilder()
                        .queryable(query)
                        .metaDataStore(metaDataStore)
                        .alias("")
                        .column(this)
                        .tableArguments(query.getArguments())
                        .build();

        return context.resolve(getExpression());
    }

    @Override
    default boolean canNest(Queryable source, MetaDataStore metaDataStore) {
        SQLDialect dialect = source.getConnectionDetails().getDialect();
        String sql = toSQL(source, metaDataStore);

        SyntaxVerifier verifier = new SyntaxVerifier(dialect);
        boolean canNest = verifier.verify(sql);
        if (! canNest) {
            LOGGER.debug("Unable to nest {} because {}", this.getName(), verifier.getLastError());

            return false;
        }

        List<Reference> references = new ExpressionParser(metaDataStore).parse(source, this);

        //Search for any join expression that contains $$column.  If found, we cannot nest
        //because rewriting the SQL in the outer expression will lose the context of the calling $$column.
        return references.stream()
                .map(reference -> reference.accept(new ReferenceExtractor<JoinReference>(
                                JoinReference.class, metaDataStore, ReferenceExtractor.Mode.SAME_QUERY)))
                .flatMap(Set::stream)
                .map(reference -> reference.accept(
                        new ReferenceExtractor<ColumnArgReference>(ColumnArgReference.class, metaDataStore)))
                .flatMap(Set::stream)
                .collect(Collectors.toSet())
                .isEmpty();
    }

    @Override
    default Pair<ColumnProjection, Set<ColumnProjection>> nest(Queryable source,
                                                              MetaDataStore store,
                                                              boolean joinInOuter) {
        List<Reference> references = new ExpressionParser(store).parse(source, getExpression());

        boolean requiresJoin = requiresJoin(references);

        String columnId = source.isRoot() ? getName() : getAlias();
        boolean inProjection = source.getColumnProjection(columnId, getArguments(), true) != null;

        ColumnProjection outerProjection;
        Set<ColumnProjection> innerProjections;

        if (requiresJoin && joinInOuter) {
            String outerProjectionExpression = toPhysicalReferences(source, store);
            outerProjection = withExpression(outerProjectionExpression, inProjection);

            innerProjections = extractPhysicalReferences(source, references, store);
        } else {
            outerProjection = withExpression("{{$" + this.getSafeAlias() + "}}", isProjected());
            innerProjections = new LinkedHashSet<>(Arrays.asList(this));
        }

        return Pair.of(outerProjection, innerProjections);
    }

    <T extends ColumnProjection> T withExpression(String expression, boolean project);

    /**
     * Determines if a particular column projection requires a join to another table.
     * @param source Source table.
     * @param projection The column.
     * @param store The metadata store.
     * @return True if the column requires a join.  False otherwise.
     */
    static boolean requiresJoin(Queryable source, ColumnProjection projection, MetaDataStore store) {
        List<Reference> references = new ExpressionParser(store).parse(source, projection.getExpression());
        return requiresJoin(references);
    }

    /**
     * Determines if a join is required amongst a list of column references.
     * @param references The list of references.
     * @return True if a join is required.  False otherwise.
     */
    static boolean requiresJoin(List<Reference> references) {
        return references.stream().anyMatch(ref -> ref.accept(new HasJoinVisitor()));
    }

    /**
     * Extracts all of the physical column projections that are referenced in a list of references.
     * @param source The calling query.
     * @param references The list of references.
     * @param store The MetaDataStore.
     * @return A set of physical column projections.
     */
    static Set<ColumnProjection> extractPhysicalReferences(
            Queryable source,
            List<Reference> references,
            MetaDataStore store) {
        return references.stream()
                .map(ref -> ref.accept(new ReferenceExtractor<PhysicalReference>(
                                PhysicalReference.class, store, ReferenceExtractor.Mode.SAME_QUERY)))
                .flatMap(Set::stream)
                .map(ref -> SQLPhysicalColumnProjection.builder()
                        .name(ref.getName())
                        .build())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
