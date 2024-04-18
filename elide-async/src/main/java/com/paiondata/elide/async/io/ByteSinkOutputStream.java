/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A {@link OutputStream} that sends bytes to a byte sink.
 */
public class ByteSinkOutputStream extends OutputStream {
    private final Consumer<byte[]> byteSink;

    public ByteSinkOutputStream(Consumer<byte[]> byteSink) {
        this.byteSink = Objects.requireNonNull(byteSink, "byteSink should not be null");
    }

    @Override
    public void write(int b) throws IOException {
        byteSink.accept(new byte[] { (byte) b });
    }

    @Override
    public void write(byte[] b) throws IOException {
        byteSink.accept(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] dest = new byte[len];
        System.arraycopy(b, off, dest, 0, len);
        byteSink.accept(dest);
    }
}
