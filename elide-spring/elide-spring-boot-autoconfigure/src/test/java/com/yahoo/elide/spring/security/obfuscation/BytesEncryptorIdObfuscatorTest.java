/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.security.obfuscation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.security.obfuscation.IdObfuscator;
import com.yahoo.elide.core.type.ClassType;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;

/**
 * BytesEncryptorIdObfuscatorTest.
 */
class BytesEncryptorIdObfuscatorTest {

    @Test
    void deobfuscate() {
        AesBytesEncryptor bytesEncryptor = new AesBytesEncryptor("yourPassword", "5c0744940b5c369b");
        IdObfuscator idObfuscator = new BytesEncryptorIdObfuscator(bytesEncryptor);
        Long value = 12345L;
        String obfuscated = idObfuscator.obfuscate(value);
        Long deobfuscated = idObfuscator.deobfuscate(obfuscated, ClassType.LONG_TYPE);
        assertEquals(value, deobfuscated);
    }

    /**
     * This tests that the identifier is stable. In this configuration the
     * AesBytesEncryptor encrypts in CBC mode with an IV of all zeroes.
     */
    @Test
    void stableIdentifier() {
        AesBytesEncryptor bytesEncryptor = new AesBytesEncryptor("yourPassword", "5c0744940b5c369b");
        IdObfuscator idObfuscator = new BytesEncryptorIdObfuscator(bytesEncryptor);
        Long value = 12345L;
        String obfuscated = idObfuscator.obfuscate(value);
        String obfuscated2 = idObfuscator.obfuscate(value);
        assertEquals(obfuscated2, obfuscated);
    }
}
