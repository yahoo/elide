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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;

public class DynamicConfigVerifierTest {

    private static KeyPair kp;
    private static String signature;
    private static String tarContent = null;
    private static final String TAR_FILE_PATH = "src/test/resources/configs.tar.gz";

    @BeforeAll
    public static void setUp() throws Exception {
        createTarGZ();
        kp = generateKeyPair();
        tarContent = DynamicConfigVerifier.readTarContents(TAR_FILE_PATH);
        signature = sign(tarContent, kp.getPrivate());
    }

    @AfterAll
    public static void after() {
        FileUtils.deleteQuietly(FileUtils.getFile(TAR_FILE_PATH));
    }

    @Test
    public void testValidSignature() throws Exception {
        assertTrue(DynamicConfigVerifier.verify(tarContent, signature, kp.getPublic()));
    }

    @Test
    public void testInvalidSignature() throws Exception {
        assertFalse(DynamicConfigVerifier.verify("invalid-signature", signature, kp.getPublic()));
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

    private static void createTarGZ() throws FileNotFoundException, IOException {
        TarArchiveOutputStream tarOutputStream = null;
        try {
            String configPath  = "src/test/resources/configs/";
            tarOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(
                    new BufferedOutputStream(new FileOutputStream(new File(TAR_FILE_PATH)))));
            addFileToTarGz(tarOutputStream, configPath, "");
        } finally {
            tarOutputStream.finish();
            tarOutputStream.close();
        }
    }

    private static void addFileToTarGz(TarArchiveOutputStream tOut, String path, String base) throws IOException {
        File f = new File(path);
        String entryName = base + f.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
        tOut.putArchiveEntry(tarEntry);

        if (f.isFile()) {
            IOUtils.copy(new FileInputStream(f), tOut);
            tOut.closeArchiveEntry();
        } else {
            tOut.closeArchiveEntry();
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToTarGz(tOut, child.getAbsolutePath(), entryName + "/");
                }
            }
        }
    }
}
