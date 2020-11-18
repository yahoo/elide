/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.filter.visitor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class MatchesTemplateVisitorTest {
    private RSQLFilterDialect dialect;

    @BeforeEach
    public void setup() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(Player.class);
        dialect = new RSQLFilterDialect(dictionary);
    }

    @Test
    public void predicateMatchTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("highScore==123",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                PlayerStats.class, false, true);

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }

    @Test
    public void predicateMatchWithoutTemplateTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("highScore==123",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore==123",
                PlayerStats.class, false, true);

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }

    @Test
    public void conjunctionContainsTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100;highScore==123",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                PlayerStats.class, false, true);

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }

    @Test
    public void conjunctionMatchesTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100;highScore==123",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("lowScore>100;highScore=={{variable}}",
                PlayerStats.class, false, true);

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }

    @Test
    public void conjunctionDoesNotContainTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100;player.name==Bob*",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                PlayerStats.class, false, true);

        assertFalse(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }

    @Test
    public void disjunctionContainsTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100,highScore==123",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                PlayerStats.class, false, true);

        assertFalse(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }

    @Test
    public void disjunctionMatchesTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100,highScore==123",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("lowScore>100,highScore=={{variable}}",
                PlayerStats.class, false, true);

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }

    @Test
    public void disjunctionDoesNotContainTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("lowScore>100,player.name==Bob*",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                PlayerStats.class, false, true);

        assertFalse(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }

    @Test
    public void predicateMismatchTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("highScore!=123",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                PlayerStats.class, false, true);

        assertFalse(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }

    @Test
    public void predicateMismatchWithoutTemplateTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("highScore==123",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore==456",
                PlayerStats.class, false, true);

        assertFalse(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }

    @Test
    public void complexExpressionTest() throws Exception {
        String complexExpression = "(lowScore>100;((player.name==Bob*,lowScore>100);(highScore==123)))";
        FilterExpression clientExpression = dialect.parseFilterExpression(complexExpression,
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore=={{variable}}",
                PlayerStats.class, false, true);

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }
}
