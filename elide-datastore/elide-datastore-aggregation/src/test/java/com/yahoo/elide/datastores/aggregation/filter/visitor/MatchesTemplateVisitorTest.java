/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.filter.visitor;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;


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
    public void exactMatchTest() throws Exception {
        FilterExpression clientExpression = dialect.parseFilterExpression("highScore==123",
                PlayerStats.class, true);

        FilterExpression templateExpression = dialect.parseFilterExpression("highScore==${variable}",
                PlayerStats.class, false, true);

        assertTrue(MatchesTemplateVisitor.isValid(templateExpression, clientExpression));
    }
}
