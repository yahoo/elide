/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation.dimensionformula;

import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.google.common.collect.Sets;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.annotations.Formula;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.Setter;

import java.util.Arrays;


public class DimensionFormulaTest {

    public static final ConnectionDetails DUMMY_CONNECTION = new ConnectionDetails(new HikariDataSource(),
                    SQLDialectFactory.getDefaultDialect());

    @Test
    public void testReferenceLoop() {
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(),
                getClassType(Sets.newHashSet(DimensionLoop.class)), true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLQueryEngine(metaDataStore, (unused) -> DUMMY_CONNECTION));
        assertTrue(exception.getMessage().startsWith("Formula reference loop found:"));
    }

    @Test
    public void testCrossClassReferenceLoop() {
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(),
                        getClassType(Sets.newLinkedHashSet(Arrays.asList(LoopCountryA.class, LoopCountryB.class))),
                        true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SQLQueryEngine(metaDataStore, (unused) -> DUMMY_CONNECTION));

        String exception1 = "Formula reference loop found: loopCountryA.inUsa->loopCountryB.inUsa->loopCountryA.inUsa";

        String exception2 = "Formula reference loop found: loopCountryB.inUsa->loopCountryA.inUsa->loopCountryB.inUsa";

        assertTrue(exception1.equals(exception.getMessage()) || exception2.equals(exception.getMessage()));
    }
}

@Data
@Entity
@Include
class LoopCountryA {
    @Setter
    private String id;

    @Setter
    private LoopCountryB countryB;

    @Setter
    private boolean inUsa;

    @Id
    public String getId() {
        return id;
    }

    @ManyToOne
    @JoinColumn(name = "id")
    public LoopCountryB getCountryB() {
        return countryB;
    }

    @DimensionFormula("CASE WHEN {{countryB.inUsa}} = 'United States' THEN true ELSE false END")
    @Formula("CASE WHEN name = 'United States' THEN true ELSE false END")
    public boolean isInUsa() {
        return inUsa;
    }
}

@Data
@Entity
@Include
class LoopCountryB {
    @Setter
    private String id;

    @Setter
    private LoopCountryA countryA;

    @Setter
    private boolean inUsa;

    @Id
    public String getId() {
        return id;
    }

    @ManyToOne
    @JoinColumn(name = "id")
    public LoopCountryA getCountryA() {
        return countryA;
    }

    @DimensionFormula("CASE WHEN {{countryA.inUsa}} = 'United States' THEN true ELSE false END")
    @Formula("CASE WHEN name = 'United States' THEN true ELSE false END")
    public boolean isInUsa() {
        return inUsa;
    }
}
