/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

@FunctionalInterface
/**
 * Used to determine the URI prefix for the MediaType Profile URI.
 */
public interface UriPrefixSupplier {
    String getUriPrefix();
}
