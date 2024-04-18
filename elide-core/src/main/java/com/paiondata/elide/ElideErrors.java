/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Stores the list of errors.
 * <p>
 * Builder example:
 * <pre><code>
 * ElideErrors.builder()
 *     .error(error -> error.message(message).attribute("code", "INTERNAL_SERVER_ERROR"))
 *     .build();
 * </code></pre>
 *
 * @see ElideError
 */
@Getter
public class ElideErrors {
    private final List<ElideError> errors;

    public ElideErrors(List<ElideError> errors) {
        this.errors = errors;
    }

    public static ElideErrorsBuilder builder() {
        return new ElideErrorsBuilder();
    }

    public static class ElideErrorsBuilder {
        private List<ElideError> errors = new ArrayList<>();

        public ElideErrorsBuilder error(Consumer<ElideError.ElideErrorBuilder> error) {
            ElideError.ElideErrorBuilder builder = ElideError.builder();
            error.accept(builder);
            return error(builder.build());
        }

        public ElideErrorsBuilder error(ElideError error) {
            this.errors.add(error);
            return this;
        }

        public ElideErrors build() {
            return new ElideErrors(this.errors);
        }
    }
}
