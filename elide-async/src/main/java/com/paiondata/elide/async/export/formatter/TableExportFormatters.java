/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import com.paiondata.elide.async.models.ResultType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Contains map of {@link ResultType} to {@link TableExportFormatter}.
 * <p>
 * Use the static factory {@link #builder()} method to prepare an instance.
 *
 */
public class TableExportFormatters extends LinkedHashMap<String, TableExportFormatter> {

    private static final long serialVersionUID = 1L;

    /**
     * Returns a mutable {@link TableExportFormattersBuilder} for building {@link TableExportFormatters}.
     *
     * @return the builder
     */
    public static TableExportFormattersBuilder builder() {
        return new TableExportFormattersBuilder();
    }

    /**
     * Returns a builder with the current values.
     *
     * @return the builder to mutate
     */
    public TableExportFormattersBuilder mutate() {
        return builder().entries(
                entries -> this.entrySet().stream().forEach(entry -> entries.put(entry.getKey(), entry.getValue())));
    }

    /**
     * A mutable builder for building {@link TableExportFormatters}.
     */
    public static class TableExportFormattersBuilder
            extends TableExportFormattersBuilderSupport<TableExportFormattersBuilder> {
        public TableExportFormatters build() {
            TableExportFormatters tableExportFormatters = new TableExportFormatters();
            tableExportFormatters.putAll(this.entries);
            return tableExportFormatters;
        }

        @Override
        public TableExportFormattersBuilder self() {
            return this;
        }
    }

    public abstract static class TableExportFormattersBuilderSupport<S> {
        protected Map<String, TableExportFormatter> entries = new LinkedHashMap<>();

        public abstract S self();

        public S entries(Consumer<Map<String, TableExportFormatter>> entries) {
            entries.accept(this.entries);
            return self();
        }

        public S entry(String key, TableExportFormatter value) {
            this.entries.put(key, value);
            return self();
        }

        public S clear() {
            this.entries.clear();
            return self();
        }
    }
}
