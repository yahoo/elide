/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import com.paiondata.elide.core.PersistentResource;

import java.io.Closeable;
import java.io.IOException;

/**
 * Used for writing {@link PersistentResource} streams.
 */
public interface ResourceWriter extends Closeable {
    void write(PersistentResource<?> resource) throws IOException;
}
