/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import javax.annotation.Nullable;

/**
 * The ErrorMapper allows mapping any RuntimeException of your choice into more meaningful
 * CustomErrorExceptions to improved your error response to the client.
 */
@FunctionalInterface
public interface ErrorMapper {
    /**
     * @param origin any Exception not caught by default
     * @return a mapped CustomErrorException or null if you do not want to map this error
     */
    @Nullable CustomErrorException map(Exception origin);
}
