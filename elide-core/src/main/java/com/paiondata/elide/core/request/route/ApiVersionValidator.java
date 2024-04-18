/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

@FunctionalInterface
/**
 * Api Version Validator.
 */
public interface ApiVersionValidator {
    boolean isValidApiVersion(String apiVersion);
}
