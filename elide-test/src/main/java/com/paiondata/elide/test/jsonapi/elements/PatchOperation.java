/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.test.jsonapi.elements;

import java.util.LinkedHashMap;

/**
 * Patch operation.
 */
public class PatchOperation extends LinkedHashMap<String, Object> {

   /**
    * Patch Operation.
    *
    * @param operation the operation type
    * @param path the operation path
    * @param value the operation value
    */
   public PatchOperation(PatchOperationType operation, String path, Resource value) {
      this.put("op", operation.name());
      this.put("path", path);
      this.put("value", value);
   }
}
