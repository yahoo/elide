package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.schema.metric.Sum;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ViewTest extends TestFramework {
    @BeforeAll
    public static void init() {
        TestFramework.init();
    }

    @Test
    public void testViewAttribute() throws Exception {
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryViewIsoCode", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("lowScore"), Sum.class)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("countryViewIsoCode"))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats usa0 = new PlayerStats();
        usa0.setId("0");
        usa0.setLowScore(276);
        usa0.setHighScore(3646);
        usa0.setCountryViewIsoCode("USA");

        PlayerStats hk1 = new PlayerStats();
        hk1.setId("1");
        hk1.setLowScore(72);
        hk1.setHighScore(1000);
        hk1.setCountryViewIsoCode("HKG");

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(hk1, results.get(1));

        // the join would not happen for a view join
        PlayerStats actualStats1 = (PlayerStats) results.get(0);
        assertNull(actualStats1.getCountry());
    }
}
