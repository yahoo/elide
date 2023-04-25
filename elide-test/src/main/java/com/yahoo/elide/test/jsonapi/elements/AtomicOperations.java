/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.test.jsonapi.elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Atomic Operations.
 */
public class AtomicOperations {

    private static final Gson GSON_INSTANCE = new GsonBuilder()
           .serializeNulls().create();

   Map<String, List<AtomicOperation>> value = new HashMap<>();

   /**
    * Patch Set.
    *
    * @param atomicOperations the set of patch operations
    */
   public AtomicOperations(AtomicOperation... atomicOperations) {
      this.value.put("atomic:operations", Arrays.asList(atomicOperations));
   }

   /**
    * To json string.
    *
    * @return the string
    */
   public String toJSON() {
      return GSON_INSTANCE.toJson(value);
   }
}
