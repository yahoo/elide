/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import com.paiondata.elide.Serdes.SerdesBuilder;

/**
 * Used to customize the mutable {@link SerdesBuilder}.
 */
public interface SerdesBuilderCustomizer {
    void customize(SerdesBuilder builder);
}
