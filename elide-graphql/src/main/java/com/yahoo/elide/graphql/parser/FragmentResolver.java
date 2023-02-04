/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;

import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.Selection;
import graphql.language.SelectionSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class that fetch {@link FragmentDefinition}s from graphQL {@link Document} and store them for future reference.
 */
public class FragmentResolver {
    private final Map<String, FragmentDefinition> fragmentMap = new HashMap<>();

    public boolean contains(String fragmentName) {
        return fragmentMap.containsKey(fragmentName);
    }

    public FragmentDefinition get(String fragmentName) {
        return fragmentMap.get(fragmentName);
    }

    /**
     * Fetch fragments from documents. Only fragment definitions would be processed.
     *
     * @param document graphql document
     */
    public void addFragments(Document document) {
        addFragments(document.getDefinitions().stream()
                .filter(definition -> definition instanceof FragmentDefinition)
                .map(definition -> (FragmentDefinition) definition)
                .collect(Collectors.toList()));
    }

    /**
     * Make sure there is not fragment loop in in-coming definitions and store those fragments.
     *
     * @param fragments fragments to add
     */
    private void addFragments(List<FragmentDefinition> fragments) {
        final Map<String, FragmentDefinition> newFragments = fragments.stream()
                .collect(Collectors.toMap(FragmentDefinition::getName, Function.identity()));

        // make sure there is no fragment loop and undefined fragments in fragment definitions
        final Set<String> fragmentNames = new HashSet<>();
        fragments.forEach(fragmentDefinition -> validateFragment(newFragments, fragmentDefinition, fragmentNames));

        this.fragmentMap.putAll(newFragments);
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

        getNestedFragments(fragmentDefinition.getSelectionSet()).stream()
                .map(FragmentSpread::getName)
                .distinct()
                .forEach(name -> {
                    if (!fragmentMap.containsKey(name)) {
                        throw new InvalidEntityBodyException(String.format("Unknown fragment {%s}.", name));
                    }
                    validateFragment(fragmentMap, fragmentMap.get(name), fragmentNames);
                });

        fragmentNames.remove(fragmentName);
    }

    /**
     * Get nested fragments from a selection set, skip other graphQL {@link Field}s.
     * This is only for fragment loop validation.
     *
     * @param selectionSet graphql selection set
     * @return nested fragments in the selection set
     */
    private static List<FragmentSpread> getNestedFragments(SelectionSet selectionSet) {
        return selectionSet.getSelections().stream()
                .map(FragmentResolver::getNestedFragments)
                .reduce(new ArrayList<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                });
    }

    /**
     * Get nested fragments from a field, skip other graphQL {@link Field}s.
     * This is only for fragment loop validation.
     *
     * @param selection graphql selection
     * @return nested fragments in the selection set of this selection
     */
    private static List<FragmentSpread> getNestedFragments(Selection selection) {
        if (selection instanceof Field) {
            return ((Field) selection).getSelectionSet() == null
                    || ((Field) selection).getSelectionSet().getSelections().isEmpty()
                    ? new ArrayList<>()
                    : getNestedFragments(((Field) selection).getSelectionSet());
        }
        if (selection instanceof FragmentSpread) {
            return Collections.singletonList((FragmentSpread) selection);
        }
        // TODO: support inline fragment
        throw new BadRequestException("Unsupported graphQL selection type: " + selection.getClass());
    }
}
