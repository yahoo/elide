/*
 * Copyright 2019, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;

import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper class that checks whether there are undefined fragments or fragment loops in given fragment definitions.
 */
public class FragmentResolver {
    public static Map<String, FragmentDefinition> resolve(List<FragmentDefinition> fragments) {
        final Map<String, FragmentDefinition> fragmentMap = fragments.stream()
                .collect(Collectors.toMap(FragmentDefinition::getName, Function.identity()));

        // make sure there is no fragment loop and undefined fragments in fragment definitions
        final Set<String> fragmentNames = new HashSet<>();
        fragments.forEach(fragmentDefinition -> validateFragment(fragmentMap, fragmentDefinition, fragmentNames));

        return fragmentMap;
    }

    /**
     * Recursive DFS to validate that there is not reference loop in a fragment and there is not un-defined
     * fragments.
     *
     * @param fragmentDefinition fragment to be checked
     * @param fragmentNames fragment names appear in the current check path
     */
    private static void validateFragment(
            Map<String, FragmentDefinition> fragmentMap,
            FragmentDefinition fragmentDefinition,
            Set<String> fragmentNames
    ) {
        String fragmentName = fragmentDefinition.getName();
        if (fragmentNames.contains(fragmentName)) {
            throw new InvalidEntityBodyException("There is a fragment definition loop in: {"
                    + String.join(",", fragmentNames) + "} with " + fragmentName + " duplicated.");
        }

        fragmentNames.add(fragmentName);

        fragmentDefinition.getSelectionSet().getSelections().stream()
                .filter(selection -> selection instanceof FragmentSpread)
                .map(fragment -> ((FragmentSpread) fragment).getName())
                .distinct()
                .forEach(name -> {
                    if (!fragmentMap.containsKey(name)) {
                        throw new InvalidEntityBodyException(String.format("Unknown fragment {%s}.", name));
                    }
                    validateFragment(fragmentMap, fragmentMap.get(name), fragmentNames);
                });

        fragmentNames.remove(fragmentName);
    }
}
