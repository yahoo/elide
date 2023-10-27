/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Base {@link ResourceWriter}.
 */
public abstract class ResourceWriterSupport implements ResourceWriter {
    protected OutputStream outputStream;

    public ResourceWriterSupport(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    protected void write(String data) throws IOException {
        this.outputStream.write(data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException {
        this.outputStream.close();
    }
}
