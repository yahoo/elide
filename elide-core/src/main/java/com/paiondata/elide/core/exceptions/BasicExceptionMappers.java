/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.ElideErrorResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * {@link ExceptionMappers} implementation that delegates to implementations of {@link ExceptionMapper}.
 */
public class BasicExceptionMappers implements ExceptionMappers {
    private final List<ExceptionMapperRegistration> exceptionMapperRegistrations;

    public BasicExceptionMappers(List<ExceptionMapperRegistration> exceptionMapperRegistrations) {
        this.exceptionMapperRegistrations = exceptionMapperRegistrations;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public ElideErrorResponse<Object> toErrorResponse(Throwable exception, ErrorContext errorContext) {
        for (ExceptionMapperRegistration exceptionMapperRegistration : this.exceptionMapperRegistrations) {
            if (exceptionMapperRegistration.isSupported(exception)) {
                ExceptionMapper exceptionMapper = exceptionMapperRegistration.getExceptionMapper();
                try {
                    ElideErrorResponse<Object> response = exceptionMapper.toErrorResponse(exception, errorContext);
                    if (response != null) {
                        return response;
                    }
                } catch (ClassCastException t) {
                    // Ignore because this is due to the specific subclass
                }
            }
        }
        return null;
    }

    public static BasicExceptionMappersBuilder builder() {
        return new BasicExceptionMappersBuilder();
    }

    @Override
    public ExceptionMappersBuilder mutate() {
        return builder().registrations(registrations -> {
            registrations.addAll(this.exceptionMapperRegistrations);
        });
    }

    public static class BasicExceptionMappersBuilder implements ExceptionMappersBuilder {
        private final List<ExceptionMapperRegistration> exceptionMapperRegistrations = new ArrayList<>();

        public BasicExceptionMappersBuilder register(ExceptionMapperRegistration registration) {
            this.exceptionMapperRegistrations.add(registration);
            return this;
        }

        public BasicExceptionMappersBuilder register(Class<? extends Throwable> supported,
                ExceptionMapper<? extends Throwable, ?> exceptionMapper) {
            ExceptionMappersBuilder.super.register(supported, exceptionMapper);
            return this;
        }

        public BasicExceptionMappersBuilder register(ExceptionMapper<? extends Throwable, ?> exceptionMapper) {
            ExceptionMappersBuilder.super.register(exceptionMapper);
            return this;
        }

        public BasicExceptionMappersBuilder registrations(
                Consumer<List<ExceptionMapperRegistration>> exceptionMapperRegistrations) {
            exceptionMapperRegistrations.accept(this.exceptionMapperRegistrations);
            return this;
        }

        /**
         * Builds the {@link ExceptionMapper}.
         *
         * @return the ExceptionMapper
         */
        public BasicExceptionMappers build() {
            return new BasicExceptionMappers(this.exceptionMapperRegistrations);
        }
    }
}
