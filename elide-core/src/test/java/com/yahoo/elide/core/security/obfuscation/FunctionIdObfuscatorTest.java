/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.obfuscation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.type.ClassType;

import org.junit.jupiter.api.Test;

/**
 * FunctionIdObfuscatorTest.
 */
class FunctionIdObfuscatorTest {
    AesBytesEncryptor bytesEncryptor = new AesBytesEncryptor("yourPassword", "5c0744940b5c369b");

    @Test
    void testLong() {
        IdObfuscator idObfuscator = new FunctionIdObfuscator(bytesEncryptor::encrypt, bytesEncryptor::decrypt);
        String obfuscated = idObfuscator.obfuscate(Long.valueOf(12345L));
        Long value = idObfuscator.deobfuscate(obfuscated, ClassType.LONG_TYPE);
        assertEquals(12345L, value);
    }

    @Test
    void testLongPrimitive() {
        IdObfuscator idObfuscator = new FunctionIdObfuscator(bytesEncryptor::encrypt, bytesEncryptor::decrypt);
        String obfuscated = idObfuscator.obfuscate(12345L);
        Long value = idObfuscator.deobfuscate(obfuscated, ClassType.PRIMITIVE_LONG_TYPE);
        assertEquals(12345L, value);
    }

    @Test
    void testInteger() {
        IdObfuscator idObfuscator = new FunctionIdObfuscator(bytesEncryptor::encrypt, bytesEncryptor::decrypt);
        String obfuscated = idObfuscator.obfuscate(Integer.valueOf(12345));
        Integer value = idObfuscator.deobfuscate(obfuscated, ClassType.INTEGER_TYPE);
        assertEquals(12345, value);
    }

    @Test
    void testIntegerPrimitive() {
        IdObfuscator idObfuscator = new FunctionIdObfuscator(bytesEncryptor::encrypt, bytesEncryptor::decrypt);
        String obfuscated = idObfuscator.obfuscate(12345);
        Integer value = idObfuscator.deobfuscate(obfuscated, ClassType.PRIMITIVE_INTEGER_TYPE);
        assertEquals(12345, value);
    }

    @Test
    void testString() {
        IdObfuscator idObfuscator = new FunctionIdObfuscator(bytesEncryptor::encrypt, bytesEncryptor::decrypt);
        String obfuscated = idObfuscator.obfuscate("The quick brown fox jumped over the lazy dog");
        String value = idObfuscator.deobfuscate(obfuscated, ClassType.STRING_TYPE);
        assertEquals("The quick brown fox jumped over the lazy dog", value);
    }
}
