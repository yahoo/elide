/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async;

import com.yahoo.elide.Settings;

import lombok.Getter;

import java.util.function.Consumer;

/**
 * Settings for Async.
 *
 * Use the static factory {@link #builder()} method to prepare an instance.
 */
@Getter
public class AsyncSettings implements Settings {
    @Getter
    public static class Export {
        private final boolean enabled;
        private final String path;

        public Export(boolean enabled, String path) {
            this.enabled = enabled;
            this.path = path;
        }

        public static ExportBuilder builder() {
            return new ExportBuilder();
        }

        public static class ExportBuilder {
            private boolean enabled = false;
            private String path = "/export";

            public ExportBuilder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public ExportBuilder path(String path) {
                this.path = path;
                return this;
            }

            public Export build() {
                return new Export(this.enabled, this.path);
            }
        }
    }

    private final boolean enabled;
    private final String path;
    private final Export export;

    public AsyncSettings(boolean enabled, String path, Export export) {
        this.enabled = enabled;
        this.path = path;
        this.export = export;
    }

    public AsyncSettingsBuilder mutate() {
        AsyncSettingsBuilder builder = new AsyncSettingsBuilder();
        builder.enabled = this.enabled;
        builder.path = this.path;
        builder.export.enabled(this.export.enabled).path(this.export.path);
        return builder;
    }

    public static AsyncSettingsBuilder builder() {
        return new AsyncSettingsBuilder();
    }

    public static class AsyncSettingsBuilder extends AsyncSettingsBuilderSupport<AsyncSettingsBuilder> {
        @Override
        public AsyncSettings build() {
            return new AsyncSettings(this.enabled, this.path, this.export.build());
        }

        @Override
        protected AsyncSettingsBuilder self() {
            return this;
        }
    }

    public abstract static class AsyncSettingsBuilderSupport<S> implements SettingsBuilder {
        protected boolean enabled = false;
        protected String path = "/";
        protected final Export.ExportBuilder export = Export.builder();

        protected abstract S self();

        public S enabled(boolean enabled) {
            this.enabled = enabled;
            return self();
        }

        public S path(String path) {
            this.path = path;
            return self();
        }

        public S export(Consumer<Export.ExportBuilder> export) {
            export.accept(this.export);
            return self();
        }
    }
}
