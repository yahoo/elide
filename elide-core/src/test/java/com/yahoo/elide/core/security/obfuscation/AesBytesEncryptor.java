/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.obfuscation;

import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AesBytesEncryptor {
    private final SecretKey secretKey;

    public AesBytesEncryptor(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public AesBytesEncryptor(String password, String salt) {
        this(from(password, salt));
    }

    static SecretKey from(String password, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    byte[] encrypt(byte[] byteArray) {
        try {
            byte[] key = secretKey.getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return cipher.doFinal(byteArray);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    byte[] decrypt(byte[] encryptedByteArray) {
        try {
            byte[] key = secretKey.getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return cipher.doFinal(encryptedByteArray);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
