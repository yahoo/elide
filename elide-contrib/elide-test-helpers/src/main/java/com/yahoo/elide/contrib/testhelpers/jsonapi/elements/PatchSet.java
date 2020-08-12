/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

/**
 * Pat Set.
 */
public class PatchSet extends ArrayList {

   static private final Gson GSON_INSTANCE = new GsonBuilder()
           .serializeNulls().create();

   /**
    * Patch Set.
    *
    * @param patchOperations the set of patch operations
    */
   public PatchSet(PatchOperation... patchOperations) {
      this.addAll(ImmutableList.copyOf(patchOperations));
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
