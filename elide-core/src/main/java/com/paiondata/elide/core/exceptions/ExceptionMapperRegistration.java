/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.ElideErrorResponse;

import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.util.Objects;

/**
 * Maps an exception to an {@link ElideErrorResponse}.
 */
@Getter
public class ExceptionMapperRegistration {
    private final Class<? extends Throwable> supported;
    private final ExceptionMapper<? extends Throwable, ?> exceptionMapper;

    private ExceptionMapperRegistration(Class<? extends Throwable> supported,
            ExceptionMapper<? extends Throwable, ?> exceptionMapper) {
        this.supported = supported;
        this.exceptionMapper = exceptionMapper;
    }

    public boolean isSupported(Throwable e) {
        return e.getClass().isAssignableFrom(supported);
    }

    public static ExceptionMapperRegistrationBuilder builder() {
        return new ExceptionMapperRegistrationBuilder();
    }

    public static class ExceptionMapperRegistrationBuilder {
        protected Class<? extends Throwable> supported = null;
        protected ExceptionMapper<? extends Throwable, ?> exceptionMapper = null;

        public ExceptionMapperRegistrationBuilder supported(Class<? extends Throwable> supported) {
            this.supported = supported;
            return this;
        }

        public ExceptionMapperRegistrationBuilder exceptionMapper(
                ExceptionMapper<? extends Throwable, ?> exceptionMapper) {
            this.exceptionMapper = exceptionMapper;
            return this;
        }

        @SuppressWarnings("unchecked")
        public ExceptionMapperRegistration build() {
            Objects.requireNonNull(this.exceptionMapper, "exceptionMapper should be set");

            if (this.supported != null) {
                return new ExceptionMapperRegistration(this.supported, this.exceptionMapper);
            }

            Class<? extends Throwable> clazz = Throwable.class;
            try {
                clazz = (Class<? extends Throwable>) ((ParameterizedType) exceptionMapper.getClass()
                        .getGenericInterfaces()[0]).getActualTypeArguments()[0];
            } catch (RuntimeException e) {
                // Do nothing
            }
            return new ExceptionMapperRegistration(clazz, this.exceptionMapper);

        }
    }
}
