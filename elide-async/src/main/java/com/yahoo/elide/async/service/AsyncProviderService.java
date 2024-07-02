/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Service that returns the providers needed.
 */
public class AsyncProviderService {
    private final Map<String, Object> providers;

    /**
     * Constructor.
     *
     * @param providers the providers
     */
    public AsyncProviderService(Map<String, Object> providers) {
        this.providers = providers;
    }

    /**
     * Gets the provider.
     *
     * @param <T> the provider type
     * @param clazz the provider type
     * @return the provider
     */
    public <T> T getProvider(Class<T> clazz) {
        return clazz.cast(providers.get(clazz.getName()));
    }

    /**
     * Gets the builder for {@link AsyncProviderService}.
     *
     * @return the builder.
     */
    public static AsyncProviderServiceBuilder builder() {
        return new AsyncProviderServiceBuilder();
    }

    /**
     * Builder for {@link AsyncProviderService}.
     */
    public static class AsyncProviderServiceBuilder
            extends AsyncProviderServiceBuilderSupport<AsyncProviderServiceBuilder> {
        public AsyncProviderService build() {
            return new AsyncProviderService(this.providers);
        }

        @Override
        public AsyncProviderServiceBuilder self() {
            return this;
        }
    }

    /**
     * Builder Support for {@link AsyncProviderService}.
     *
     * @param <S> the builder class
     */
    public static abstract class AsyncProviderServiceBuilderSupport<S> {
        public abstract S self();
        protected Map<String, Object> providers = new HashMap<>();

        /**
         * Sets the providers.
         *
         * @param providers the providers
         * @return the builder
         */
        public S providers(Map<String, Object> providers) {
            this.providers = providers;
            return self();
        }

        /**
         * Customize the providers.
         *
         * @param customizer the customizer
         * @return the builder
         */
        public S providers(Consumer<Map<String, Object>> customizer) {
            customizer.accept(this.providers);
            return self();
        }

        /**
         * Sets the provider.
         *
         * @param <T> the provider type
         * @param clazz the provider class
         * @param provider the provider instance
         * @return the builder
         */
        public <T> S provider(Class<T> clazz, T provider) {
            // The clazz is not obtained from the provider object as it may be a derived class or proxy
            this.providers.put(clazz.getName(), provider);
            return self();
        }
    }
}
