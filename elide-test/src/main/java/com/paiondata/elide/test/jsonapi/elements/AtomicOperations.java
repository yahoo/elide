/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.test.jsonapi.elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Atomic Operations.
 */
public class AtomicOperations extends HashMap<String, List<AtomicOperation>> {

    private static final long serialVersionUID = 1L;

    private static final Gson GSON_INSTANCE = new GsonBuilder()
           .serializeNulls().create();

   /**
    * Patch Set.
    *
    * @param atomicOperations the set of patch operations
    */
   public AtomicOperations(AtomicOperation... atomicOperations) {
      this.put("atomic:operations", Arrays.asList(atomicOperations));
   }

   /**
    * To json string.
    *
    * @return the string
    */
   public String toJSON() {
      return GSON_INSTANCE.toJson(this);
   }
}
