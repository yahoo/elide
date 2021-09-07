/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.filter.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import example.Player;
import example.PlayerStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MatchesTemplateVisitorTest {
    private RSQLFilterDialect dialect;
    private Type<?> playerStatsType = ClassType.of(PlayerStats.class);

    @BeforeEach
    public void setup() {
        EntityDictionary dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(Player.class);
        dialect = new RSQLFilterDialect(dictionary);
    }

    @Test
    public void predicateMatchTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("highScore==123",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{foo}}",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        Argument expected = Argument.builder()
                .name("foo")
                .value(123L)
                .build();

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));

        assertEquals(1, extractedArgs.size());
        assertEquals(extractedArgs.get("foo"), expected);
    }

    @Test
    public void predicateMatchWithoutTemplateTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("highScore==123",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore==123",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));
        assertEquals(0, extractedArgs.size());
    }

    @Test
    public void conjunctionContainsTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100;highScore==123",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        Argument expected = Argument.builder()
                .name("variable")
                .value(123L)
                .build();

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));

        assertEquals(1, extractedArgs.size());
        assertEquals(extractedArgs.get("variable"), expected);
    }

    @Test
    public void conjunctionMatchesTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100;highScore==123",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("lowScore>100;highScore=={{variable}}",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        Argument expected = Argument.builder()
                .name("variable")
                .value(123L)
                .build();

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));

        assertEquals(1, extractedArgs.size());
        assertEquals(extractedArgs.get("variable"), expected);
    }

    @Test
    public void mulipleConjunctionOrderTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100;(highScore>=100;highScore<999)",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore>={{low}};highScore<{{high}}",
                playerStatsType, false, true);

        Argument expected1 = Argument.builder()
                .name("low")
                .value(100L)
                .build();

        Argument expected2 = Argument.builder()
                .name("high")
                .value(999L)
                .build();

        Map<String, Argument> extractedArgs = new HashMap<>();

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));
        assertEquals(2, extractedArgs.size());
        assertEquals(extractedArgs.get("low"), expected1);
        assertEquals(extractedArgs.get("high"), expected2);
    }

    @Test
    public void conjunctionDoesNotContainTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100;player.name==Bob*",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        assertFalse(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));
        assertEquals(0, extractedArgs.size());
    }

    @Test
    public void disjunctionContainsTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100,highScore==123",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        assertFalse(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));
        assertEquals(0, extractedArgs.size());
    }

    @Test
    public void disjunctionMatchesTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100,highScore==123",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("lowScore>100,highScore=={{variable}}",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        Argument expected = Argument.builder()
                .name("variable")
                .value(123L)
                .build();

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));

        assertEquals(1, extractedArgs.size());
        assertEquals(extractedArgs.get("variable"), expected);
    }

    @Test
    public void disjunctionDoesNotContainTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100,player.name==Bob*",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        assertFalse(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));
        assertEquals(0, extractedArgs.size());
    }

    @Test
    public void predicateMismatchTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("highScore!=123",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        assertFalse(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));
        assertEquals(0, extractedArgs.size());
    }

    @Test
    public void predicateMismatchWithoutTemplateTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("highScore==123",
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore==456",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        assertFalse(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));
        assertEquals(0, extractedArgs.size());
    }

    @Test
    public void complexExpressionTest() throws Exception {
        String complexExpression = "(lowScore>100;((player.name==Bob*,lowScore>100);(highScore==123)))";
        FilterExpression clientExpression = dialect.parseFilterExpression(complexExpression,
                playerStatsType, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                playerStatsType, false, true);

        Map<String, Argument> extractedArgs = new HashMap<>();
        Argument expected = Argument.builder()
                .name("variable")
                .value(123L)
                .build();

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression, extractedArgs));

        assertEquals(1, extractedArgs.size());
        assertEquals(extractedArgs.get("variable"), expected);
    }
}
