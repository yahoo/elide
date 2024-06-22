/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.yahoo.elide.modelconfig.jsonformats.ElideRSQLFilterFormat;

import com.networknt.schema.Format;
import com.networknt.schema.JsonMetaSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * The Elide {@link JsonMetaSchema}.
 */
public class ElideMetaSchema {
    public static final List<Format> FORMATS;

    static {
        List<Format> result = new ArrayList<>();
        result.add(new ElideRSQLFilterFormat());
        FORMATS = result;
    }

    private static class Holder {
        static final JsonMetaSchema INSTANCE;
        static {
            INSTANCE = JsonMetaSchema.builder(JsonMetaSchema.getV202012())
                    .formats(FORMATS)
                    // add your custom keywords
                    .build();

        }
    }

    public static JsonMetaSchema getInstance() {
        return Holder.INSTANCE;
    }
}
