/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.dictionary;

import com.paiondata.elide.core.dictionary.EntityDictionary.EntityDictionaryBuilder;

/**
 * Customizer for customizing a {@link EntityDictionaryBuilder}.
 */
public interface EntityDictionaryBuilderCustomizer {
    void customize(EntityDictionaryBuilder builder);
}
