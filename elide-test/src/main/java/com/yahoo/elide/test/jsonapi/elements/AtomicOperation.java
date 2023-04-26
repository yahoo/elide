/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.test.jsonapi.elements;

import java.util.LinkedHashMap;

/**
 * Atomic operation.
 */
public class AtomicOperation extends LinkedHashMap<String, Object> {

   /**
    * Atomic Operation.
    *
    * @param operation the operation type
    * @param href the operation path
    * @param data the operation value
    */
   public AtomicOperation(AtomicOperationCode operation, String href, Resource data) {
      this.put("op", operation.name());
      this.put("href", href);
      this.put("data", data);
   }

   /**
    * Atomic Operation.
    *
    * @param operation the operation type
    * @param ref the operation path
    * @param data the operation value
    */
   public AtomicOperation(AtomicOperationCode operation, Ref ref, Resource data) {
      this.put("op", operation.name());
      this.put("ref", ref);
      this.put("data", data);
   }
}
