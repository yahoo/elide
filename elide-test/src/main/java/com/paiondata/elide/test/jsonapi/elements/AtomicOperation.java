/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.test.jsonapi.elements;

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
   public AtomicOperation(AtomicOperationCode operation, String href, Data data) {
      this.put("op", operation.name());
      this.put("href", href);
      if (data != null) {
          this.put("data", data.get("data"));
      }
   }

   /**
    * Atomic Operation.
    *
    * @param operation the operation type
    * @param ref the operation path
    * @param data the operation value
    */
   public AtomicOperation(AtomicOperationCode operation, Ref ref, Data data) {
      this.put("op", operation.name());
      this.put("ref", ref);
      if (data != null) {
          this.put("data", data.get("data"));
      }
   }

   /**
    * Atomic Operation.
    *
    * @param operation the operation type
    * @param data the operation value
    */
   public AtomicOperation(AtomicOperationCode operation, Data data) {
      this.put("op", operation.name());
      if (data != null) {
          this.put("data", data.get("data"));
      }
   }
}
