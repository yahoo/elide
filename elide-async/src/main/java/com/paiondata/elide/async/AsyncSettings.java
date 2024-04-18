/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async;

import com.paiondata.elide.Settings;

import lombok.Getter;

import java.util.function.Consumer;

/**
 * Settings for Async.
 * <p>
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

    /**
     * Returns a builder with the current values.
     *
     * @return the builder to mutate
     */
    public AsyncSettingsBuilder mutate() {
        return new AsyncSettingsBuilder()
                .enabled(this.enabled)
                .path(this.path)
                .export(newExport -> newExport.enabled(this.getExport().isEnabled()).path(this.getExport().getPath()));
    }

    /**
     * Returns a mutable {@link AsyncSettingsBuilder} for building {@link AsyncSettings}.
     *
     * @return the builder
     */
    public static AsyncSettingsBuilder builder() {
        return new AsyncSettingsBuilder();
    }

    /**
     * A mutable builder for building {@link AsyncSettings}.
     */
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

        /**
         * Customize the export settings.
         *
         * @param export the customizer
         * @return the builder
         */
        public S export(Consumer<Export.ExportBuilder> export) {
            export.accept(this.export);
            return self();
        }
    }
}
