/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.security.obfuscation;

import com.yahoo.elide.core.security.obfuscation.FunctionIdObfuscator;

import org.springframework.security.crypto.encrypt.BytesEncryptor;

/**
 * {@link com.yahoo.elide.core.security.obfuscation.IdObfuscator} that uses a
 * {@link BytesEncryptor}.
 */
public class BytesEncryptorIdObfuscator extends FunctionIdObfuscator {
    public BytesEncryptorIdObfuscator(BytesEncryptor bytesEncryptor) {
        super(bytesEncryptor::encrypt, bytesEncryptor::decrypt);
    }
}
