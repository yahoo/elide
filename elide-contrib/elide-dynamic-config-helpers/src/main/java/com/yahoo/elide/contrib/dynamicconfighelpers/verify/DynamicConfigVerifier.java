/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.verify;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.Base64;

@Slf4j
public class DynamicConfigVerifier {

    /**
     * Main Methode to Verify Signature of Model Tar file.
     * @param args : expects 3 arguments.
     */
    public static void main(String[] args) {
        if (args == null || args.length != 3) {
            usage();
            throw new IllegalStateException("No Arguments provided!");
        }
        String modelTarFile = args[0];
        String signatureFile = args[1];
        String publicKeyName = args[2];

        if (verify(tarContents(modelTarFile), signatureFile, getPublicKey(publicKeyName))) {
            log.info("Successfully Validated " + modelTarFile);
        }
        else {
            log.error("Could not verify " + modelTarFile + " with details provided");
        }
    }

    private static PublicKey getPublicKey(String keyName) {
        PublicKey publicKey = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            Certificate cert = keyStore.getCertificate(keyName);
            publicKey = cert.getPublicKey();
        } catch (KeyStoreException e) {
            log.error(e.getMessage());
            throw new IllegalArgumentException("Key " + keyName + " is not availabe in keystore");
        }
        return publicKey;
    }

    /**
     * Verify signature of tar.gz.
     * @param fileList
     * @param signature
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static boolean verify(String fileList, String signature, PublicKey publicKey) {
        Signature publicSignature;
        try {
            publicSignature = Signature.getInstance("SHA256withRSA");
            publicSignature.initVerify(publicKey);
            publicSignature.update(fileList.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return publicSignature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
        } catch (InvalidKeyException e) {
            log.error(e.getMessage());
        } catch (SignatureException e) {
            log.error(e.getMessage());
        }
        return false;
    }

    private static String tarContents(String archiveFile) {
        StringBuffer sb = new StringBuffer();
        try (TarArchiveInputStream archive = new TarArchiveInputStream(
                new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(archiveFile))))) {
            TarArchiveEntry entry;
            while ((entry = archive.getNextTarEntry()) != null) {
                sb.append(entry.getName());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new IllegalArgumentException("archiveFile " + archiveFile + " is not available");
        }
        return sb.toString();
    }

    private static void usage() {
        log.info("\n Usage: java -cp <Jar File Name>"
                + " com.yahoo.elide.contrib.dynamicconfighelpers.validator.DynamicConfigVerifier\n"
                + " <The name of the tar.gz file\n"
                + " <The name of the file containing the signature \n"
                + " <The name public key \n"
                );
    }
}
