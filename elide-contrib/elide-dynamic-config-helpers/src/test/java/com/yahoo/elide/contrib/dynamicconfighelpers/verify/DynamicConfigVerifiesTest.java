/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.verify;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;

public class DynamicConfigVerifiesTest {

    private static KeyPair kp;
    private static String signature;

    @BeforeAll
    public static void setUp() throws Exception {
        kp = generateKeyPair();
        signature = sign("testing-signature5", kp.getPrivate());
    }

    @Test
    public void testValidSignature() throws Exception {
        assertTrue(DynamicConfigVerifier.verify("testing-signature", 5, signature, kp.getPublic()));
    }

    @Test
    public void testInvalidSignature() throws Exception {
        assertFalse(DynamicConfigVerifier.verify("invalid-signature", 5, signature, kp.getPublic()));
    }

    @Test
    public void testHelpArguments() {
        assertDoesNotThrow(() -> DynamicConfigVerifier.main(new String[] { "-h" }));
        assertDoesNotThrow(() -> DynamicConfigVerifier.main(new String[] { "--help" }));
    }

    @Test
    public void testNoArguments() {
        Exception e = assertThrows(MissingOptionException.class, () -> DynamicConfigVerifier.main(null));
        assertTrue(e.getMessage().startsWith("Missing required option"));
    }

    @Test
    public void testOneEmptyArguments() {
        Exception e = assertThrows(MissingOptionException.class,
                () -> DynamicConfigVerifier.main(new String[] { "" }));
        assertTrue(e.getMessage().startsWith("Missing required option"));
    }

    @Test
    public void testMissingArgumentValue() {
        Exception e = assertThrows(MissingArgumentException.class,
                () -> DynamicConfigVerifier.main(new String[] { "--tarFile" }));
        assertTrue(e.getMessage().startsWith("Missing argument for option"));
        e = assertThrows(MissingArgumentException.class, () -> DynamicConfigVerifier.main(new String[] { "-t" }));
        assertTrue(e.getMessage().startsWith("Missing argument for option"));
    }

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
}
