/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.test.jsonapi.elements;

import java.util.LinkedHashMap;

/**
 * Atomic operation reference.
 */
public class Ref extends LinkedHashMap<String, Object> {

   /**
    * Atomic Operation Reference.
    *
    * @param type the type
    * @param id the id
    */
   public Ref(Type type, Id id) {
      this.put("type", type.value);
      this.put("id", id.value);
   }
}
