package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.metric.Metric;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AggregationDataStoreTest {
    @Spy
    AggregationDataStore aggregationDataStore;

    @Mock
    EntityProjection entityProjection;

    @Mock
    RequestScope requestScope;

    @Spy
    EntityDictionary entityDictionary;

    @Mock
    QueryEngine qE;

    @BeforeEach
    public void init() {
        entityDictionary = new EntityDictionary(new HashMap<>());
        MockitoAnnotations.initMocks(this);
        aggregationDataStore = new AggregationDataStore(qE) {
            @Override
            public void populateEntityDictionary(EntityDictionary dictionary) {
                dictionary.bindEntity(PlayerStats.class);
                return;
            }
        };
        entityDictionary.bindEntity(PlayerStats.class);
        entityDictionary.bindEntity(Country.class);
    }

    // Empty entityProjection should yield empty query
    @Test
    public void testEmptyQueryBuilding() {
        Set<Attribute> attributes = new LinkedHashSet<>();
        Mockito.when(entityProjection.getAttributes()).thenReturn(attributes);
        Class playerStats = PlayerStats.class;
        Mockito.when(entityProjection.getType()).thenReturn(playerStats);
        Mockito.when(requestScope.getDictionary()).thenReturn(entityDictionary);
        Query query = AggregationDataStore.buildQuery(entityProjection, requestScope);

        Query expected = Query.builder()
                                .entityClass(PlayerStats.class)
                                .metrics(Collections.emptySet())
                                .groupDimensions(Collections.emptySet())
                                .timeDimensions(Collections.emptySet())
                                .havingFilter(Optional.empty())
                                .whereFilter(Optional.empty())
                                .sorting(Optional.empty())
                                .pagination(Optional.empty())
                                .scope(requestScope)
                                .build();
        Assert.assertEquals(query, expected);
    }

    // Test entityProjection with certain attributes yields query with corresponding metrics
    @Test
    public void testQueryBuildingWithAttributes() {
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(int.class).name("highScore").build());
        Mockito.when(entityProjection.getAttributes()).thenReturn(attributes);
        Class playerStats = PlayerStats.class;
        Mockito.when(entityProjection.getType()).thenReturn(playerStats);
        Mockito.when(requestScope.getDictionary()).thenReturn(entityDictionary);
        Query query = AggregationDataStore.buildQuery(entityProjection, requestScope);

        Query expected = Query.builder()
                .entityClass(PlayerStats.class)
                .metrics(Collections.emptySet())
                .groupDimensions(Collections.emptySet())
                .timeDimensions(Collections.emptySet())
                .havingFilter(Optional.empty())
                .whereFilter(Optional.empty())
                .sorting(Optional.empty())
                .pagination(Optional.empty())
                .scope(requestScope)
                .build();

        // Check query came back as expected and that attribute was converted to corresponding metric in result.
        Assert.assertTrue(EqualsBuilder.reflectionEquals(query, expected, "metrics"));
        Assert.assertTrue(verifyMetrics(query.getMetrics(), Collections.singletonList("highScore")));
    }

    private boolean verifyMetrics(Set<Metric> metrics, List<String> metricNames) {
        for (String metricName : metricNames) {
            if (!metrics.stream().anyMatch(mn -> mn.getName().equals(metricName))) {
                return false;
            }
        }
        return true;
    }

}
