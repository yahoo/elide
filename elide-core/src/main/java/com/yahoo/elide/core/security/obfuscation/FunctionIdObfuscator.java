/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.obfuscation;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;

/**
 * {@link IdObfuscator} that accepts functions to perform obfuscation and
 * deobfuscation.
 */
public class FunctionIdObfuscator implements IdObfuscator {
    private final Function<byte[], byte[]> obfuscationFunction;
    private final Function<byte[], byte[]> deobfuscationFunction;

    public FunctionIdObfuscator(Function<byte[], byte[]> obfuscationFunction,
            Function<byte[], byte[]> deobfuscationFunction) {
        this.obfuscationFunction = obfuscationFunction;
        this.deobfuscationFunction = deobfuscationFunction;
    }

    @Override
    public String obfuscate(Object id) {
        byte[] result;
        if (id instanceof Long value) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(value);
            result = obfuscationFunction.apply(buffer.array());
        } else if (id instanceof Integer value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            buffer.putInt(value);
            result = obfuscationFunction.apply(buffer.array());
        } else if (id instanceof String value) {
            result = obfuscationFunction.apply(value.getBytes(StandardCharsets.UTF_8));
        } else {
            throw new InvalidValueException("id");
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deobfuscate(String obfuscatedId, Type<?> type) {
        try {
            byte[] value = Base64.getUrlDecoder().decode(obfuscatedId);
            byte[] result = deobfuscationFunction.apply(value);
            if (ClassType.LONG_TYPE.equals(type) || ClassType.PRIMITIVE_LONG_TYPE.equals(type)) {
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.put(result);
                buffer.flip();
                return (T) Long.valueOf(buffer.getLong());
            } else if (ClassType.INTEGER_TYPE.equals(type) || ClassType.PRIMITIVE_INTEGER_TYPE.equals(type)) {
                ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
                buffer.put(result);
                buffer.flip();
                return (T) Integer.valueOf(buffer.getInt());
            } else if (ClassType.STRING_TYPE.equals(type)) {
                return (T) new String(result, StandardCharsets.UTF_8);
            }
            throw new InvalidValueException("id");
        } catch (RuntimeException e) {
            throw new InvalidValueException("id");
        }
    }
}
