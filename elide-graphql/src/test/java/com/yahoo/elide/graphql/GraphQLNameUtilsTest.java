/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.EntityDictionary;
import example.models.inheritance.Character;
import example.models.inheritance.Droid;
import example.models.inheritance.Hero;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class GraphQLNameUtilsTest {

    @Test
    public void testBoundNameMapping() {
        EntityDictionary dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Droid.class);
        dictionary.bindEntity(Hero.class);
        dictionary.bindEntity(Character.class);

        GraphQLNameUtils nameUtils = new GraphQLNameUtils(dictionary);

        assertEquals("droid", nameUtils.toBoundName("Droid"));
        assertEquals("hero", nameUtils.toBoundName("Hero"));
        assertEquals("character", nameUtils.toBoundName("Character"));
    }
}
