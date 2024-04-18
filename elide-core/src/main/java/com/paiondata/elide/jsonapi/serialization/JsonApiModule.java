/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.serialization;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * JSON API Module.
 */
public class JsonApiModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    public JsonApiModule() {
        super("JsonApiModule", Version.unknownVersion());
        addSerializer(new JsonApiSetSerializer());
        addSerializer(new JsonApiErrorSerializer());
    }
}
