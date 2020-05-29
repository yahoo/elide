/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.verify;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;

public class DynamicConfigVerifiesTest {

    private KeyPair kp;
    private String signature;

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair pair = generator.generateKeyPair();
        return pair;
    }

    private static String sign(String data, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(data.getBytes(StandardCharsets.UTF_8));

        byte[] signature = privateSignature.sign();

        return Base64.getEncoder().encodeToString(signature);
    }

    private void setup() throws Exception {
        kp = generateKeyPair();
        signature = sign("testing-signature", kp.getPrivate());
    }

    @Test
    public void testValidSignature() throws Exception {
        setup();
        assertTrue(DynamicConfigVerifier.verify("testing-signature", signature, kp.getPublic()));
    }

    @Test
    public void testInvalidSignature() throws Exception {
        setup();
        assertFalse(DynamicConfigVerifier.verify("invalid-signature", signature, kp.getPublic()));
    }

    @Test
    public void testFileSignature() throws Exception {
        kp = generateKeyPair();
        signature = sign("testing-signature", kp.getPrivate());

        assertTrue(DynamicConfigVerifier.verify("testing-signature", signature, kp.getPublic()));
    }
}
