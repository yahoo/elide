/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.ElideErrorResponse;

import java.util.List;
import java.util.function.Consumer;

/**
 * Maps an exception to an {@link ElideErrorResponse}.
 */
public interface ExceptionMappers {
    /**
     * Map the exception to an {@link ElideErrorResponse}.
     *
     * @param exception the exception to map.
     * @param errorContext the error context
     * @return the mapped ElideErrorResponse or null if you do not want to map this error
     */
    ElideErrorResponse<Object> toErrorResponse(Throwable exception, ErrorContext errorContext);

    /**
     * Returns a mutable {@link ExceptionMappersBuilder}.
     *
     * @return the mutable ExceptionMappersBuilder
     */
    ExceptionMappersBuilder mutate();

    /**
     * The mutable builder for {@link ExceptionMappers}.
     */
    public interface ExceptionMappersBuilder {

        /**
         * Adds a {@ExceptionMapperRegistration}.
         *
         * @param exceptionMapperRegistration the registration
         * @return the builder
         */
        ExceptionMappersBuilder register(ExceptionMapperRegistration exceptionMapperRegistration);

        /**
         * Register an {@link ExceptionMapper} with the supported exception class.
         *
         * @param supported the supported exception class
         * @param exceptionMapper the exception mapper
         * @return the builder
         */
        default ExceptionMappersBuilder register(Class<? extends Throwable> supported,
                ExceptionMapper<? extends Throwable, ?> exceptionMapper) {
            return register(ExceptionMapperRegistration.builder().supported(supported).exceptionMapper(exceptionMapper)
                    .build());
        }

        /**
         * Register an {@link ExceptionMapper}.
         *
         * @param exceptionMapper the exception mapper
         * @return the builder
         */
        default ExceptionMappersBuilder register(ExceptionMapper<? extends Throwable, ?> exceptionMapper) {
            return register(ExceptionMapperRegistration.builder().exceptionMapper(exceptionMapper).build());
        }

        /**
         * Customize the list of {@link ExceptionMapperRegistration}.
         *
         * @param exceptionMapperRegistrations the customizer
         * @return the builder
         */
        ExceptionMappersBuilder registrations(Consumer<List<ExceptionMapperRegistration>> exceptionMapperRegistrations);

        /**
         * Creates the {@link ExceptionMappers}.
         *
         * @return the ExceptionMappers
         */
        ExceptionMappers build();
    }
}
