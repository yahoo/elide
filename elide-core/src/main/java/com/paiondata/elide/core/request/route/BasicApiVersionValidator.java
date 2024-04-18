/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import com.paiondata.elide.core.dictionary.EntityDictionary;

/**
 * BasicApiVersionValidator.
 */
public class BasicApiVersionValidator implements ApiVersionValidator {

    @Override
    public boolean isValidApiVersion(String apiVersion) {
        return (apiVersion != null
                && (EntityDictionary.NO_VERSION.equals(apiVersion) || Character.isDigit(apiVersion.charAt(0))));
    }
}
