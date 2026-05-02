/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.yahoo.elide.modelconfig.jsonformats.ElideRSQLFilterFormat;

import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.format.Format;

import java.util.ArrayList;
import java.util.List;

/**
 * The Elide {@link Dialect}.
 */
public class ElideDialect {
    public static final List<Format> FORMATS;

    static {
        List<Format> result = new ArrayList<>();
        result.add(new ElideRSQLFilterFormat());
        FORMATS = result;
    }

    private static class Holder {
        static final Dialect INSTANCE;
        static {
            INSTANCE = Dialect.builder(Dialects.getDraft202012())
                    .formats(FORMATS)
                    // add your custom keywords
                    .build();

        }
    }

    public static Dialect getInstance() {
        return Holder.INSTANCE;
    }
}
