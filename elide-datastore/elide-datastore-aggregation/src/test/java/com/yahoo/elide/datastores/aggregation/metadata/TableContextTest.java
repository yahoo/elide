/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import org.junit.jupiter.api.Test;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Id;
import javax.sql.DataSource;

public class TableContextTest {

    private SQLReferenceTable lookupTable;
    private TableContext revenueFactContext;

    public TableContextTest() {
        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(RevenueFact.class));
        models.add(ClassType.of(CurrencyRate.class));

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

        models.stream().forEach(dictionary::bindEntity);

        MetaDataStore metaDataStore = new MetaDataStore(models, true);
        metaDataStore.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        // The query engine populates the metadata store with actual tables.
        new SQLQueryEngine(metaDataStore, new ConnectionDetails(mockDataSource, SQLDialectFactory.getDefaultDialect()));

        lookupTable = new SQLReferenceTable(metaDataStore);

        SQLTable revenueFact = metaDataStore.getTable(ClassType.of(RevenueFact.class));

        revenueFactContext = lookupTable.getGlobalTableContext(revenueFact);
    }

    @Test
    public void testPhysicalReference() {

        assertEquals("MAX(`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact`.`impressions`)",
                        revenueFactContext.get("impressions"));
        assertEquals("`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact_rate`.`provider`",
                        revenueFactContext.get("rateProvider"));
    }

    @Test
    public void testLogicalReference() {

        assertEquals("MAX(`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact`.`impressions`)) * 0.1",
                        revenueFactContext.get("testImpressions"));
        assertEquals("TO_CHAR(`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact_rate`.`conversion_rate`, 999D00)",
                        revenueFactContext.get("conversionRate"));
    }

    @Test
    public void testSqlHelper() {

        // default value of 'format' argument is used for 'conversion_rate' column
        assertEquals("TO_CHAR(SUM(`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact`.`revenue`) * "
                        + "TO_CHAR(`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact_rate`.`conversion_rate`, 999D00), "
                        + "999999D00)",
                        revenueFactContext.get("revenue"));

        // invoking column's value of 'format' argument is used for 'conversionRate' column
        assertEquals("TO_CHAR(SUM(`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact`.`revenue`) * "
                        + "TO_CHAR(`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact_rate`.`conversion_rate`, 999999D00), "
                        + "999999D00)",
                        revenueFactContext.get("revenueUsingSqlHelper"));

        // pinned value of 'aggregation' and 'format' arguments is used.
        assertEquals("SUM(`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact`.`impressions`) / "
                        + "TO_CHAR(SUM(`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact`.`revenue`) * "
                        + "TO_CHAR(`com_yahoo_elide_datastores_aggregation_metadata_RevenueFact_rate`.`conversion_rate`, 999D00), "
                        + "999999D0000)",
                        revenueFactContext.get("impressionsPerUSD"));
    }
}


@EqualsAndHashCode
@ToString
@Data
@FromTable(name = "revenue_fact")
@TableMeta(arguments = {@ArgumentDefinition(name = "testPercentage", type = ValueType.TEXT, defaultValue = "0.1")})
@Include(type = "revenueFact")
class RevenueFact {

    @Id
    private String id;

    @MetricFormula(value = "{{$$column.args.aggregation}}({{$impressions}})",
                    arguments = {@ArgumentDefinition(name = "aggregation", type = ValueType.TEXT,
                                    defaultValue = "MAX")})
    private Integer impressions;

    @MetricFormula(value = "{{impressions}}) * {{$$table.args.testPercentage}}",
                    arguments = {@ArgumentDefinition(name = "aggregation", type = ValueType.TEXT,
                                    defaultValue = "MAX")})
    private Integer testImpressions;

    @MetricFormula(value = "TO_CHAR(SUM({{$revenue}}) * {{rate.conversionRate}}, {{$$column.args.format}})",
                    arguments = {@ArgumentDefinition(name = "format", type = ValueType.TEXT,
                                    defaultValue = "999999D00")})
    private BigDecimal revenue;

    @MetricFormula(value = "TO_CHAR(SUM({{$revenue}}) * {{sql from='rate' column='conversionRate'}}, {{$$column.args.format}})",
                    arguments = {@ArgumentDefinition(name = "format", type = ValueType.TEXT,
                                    defaultValue = "999999D00")})
    private BigDecimal revenueUsingSqlHelper;

    @MetricFormula(value = "{{sql column='impressions[aggregation:SUM]'}} / {{sql column='revenue[format:999999D0000]'}}")
    private Double impressionsPerUSD;

    @DimensionFormula("{{rate.conversionRate}}")
    private String conversionRate;

    @DimensionFormula("{{rate.$provider}}")
    private String rateProvider;

    @Join("{{rate.$id}} = {{$rateId}}")
    private CurrencyRate rate;
}

@EqualsAndHashCode
@ToString
@Data
@FromTable(name = "currency_rate")
@Include(type = "currencyRate")
class CurrencyRate {

    @Id
    private String id;

    @DimensionFormula(value = "TO_CHAR({{$conversion_rate}}, {{$$column.args.format}})",
                    arguments = {@ArgumentDefinition(name = "format", type = ValueType.INTEGER,
                                    defaultValue = "999D00")})
    private String conversionRate;
}
