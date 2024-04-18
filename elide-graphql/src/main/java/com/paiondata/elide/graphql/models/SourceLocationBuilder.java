/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.models;

import graphql.AssertException;
import graphql.language.SourceLocation;

/**
 * SourceLocationBuilder.
 */
public class SourceLocationBuilder {
    private Integer line = null;
    private Integer column = null;

    public SourceLocationBuilder line(Integer line) {
        this.line = line;
        return this;
    }

    public SourceLocationBuilder column(Integer column) {
        this.column = column;
        return this;
    }

    public SourceLocation build() {
        if (this.line == null || this.column == null) {
            throw new AssertException("Line and column must be set.");
        }
        return new SourceLocation(this.line, this.column);
    }
}
