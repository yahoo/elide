/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.verify;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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

/**
 * Util class to Verify model tar.gz file's RSA signature with available public key in key store.
 */
@Slf4j
public class DynamicConfigVerifier {

    /**
     * Main Method to Verify Signature of Model Tar file.
     * @param args : expects 3 arguments.
     */
    public static void main(String[] args) {

        Options options = prepareOptions();

        try {
            CommandLine cli = new DefaultParser().parse(options, args);

            if (cli.hasOption("help")) {
                printHelp(options);
                return;
            }
            if (!cli.hasOption("tarFile") || !cli.hasOption("signatureFile") || !cli.hasOption("publicKeyName")) {
                printHelp(options);
                System.err.println("Missing required option");
                System.exit(1);
            }

            String modelTarFile = cli.getOptionValue("tarFile");
            String signatureFile = cli.getOptionValue("signatureFile");
            String publicKeyName = cli.getOptionValue("publicKeyName");

            if (verify(readTarContents(modelTarFile), signatureFile, getPublicKey(publicKeyName))) {
                System.out.println("Successfully Validated " + modelTarFile);
            } else {
                System.err.println("Could not verify " + modelTarFile + " with details provided");
                System.exit(2);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(3);
        }
    }

    /**
     * Verify signature of tar.gz.
     * @param fileContent : content Of all config files
     * @param signature : file containing signature
     * @param publicKey : public key name
     * @return whether the file can be verified by given key and signature
     * @throws NoSuchAlgorithmException If no Provider supports a Signature implementation for the SHA256withRSA
     *         algorithm.
     * @throws InvalidKeyException If the {@code publicKey} is invalid.
     * @throws SignatureException If Signature object is not initialized properly.
     */
    public static boolean verify(String fileContent, String signature, PublicKey publicKey)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        Signature publicSignature;

        publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update(fileContent.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        return publicSignature.verify(signatureBytes);
    }

    /**
     * Read Content of all files.
     * @param archiveFile : tar.gz file path
     * @return appended content of all files in tar
     * @throws FileNotFoundException If {@code archiveFile} does not exist.
     * @throws IOException If an I/O error occurs.
     */
    public static String readTarContents(String archiveFile) throws FileNotFoundException, IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader br = null;

        try (TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(
                new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(archiveFile))))) {
            TarArchiveEntry entry = archiveInputStream.getNextTarEntry();
            while (entry  != null) {
                br = new BufferedReader(new InputStreamReader(archiveInputStream));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                entry = archiveInputStream.getNextTarEntry();
            }
        } finally {
            if (br != null) {
               br.close();
            }
        }

        return sb.toString();
    }

    /**
     * Retrieve public key from Key Store.
     * @param keyName : name of the public key
     * @return publickey
     */
    private static PublicKey getPublicKey(String keyName) throws KeyStoreException {
        PublicKey publicKey = null;
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Certificate cert = keyStore.getCertificate(keyName);
        publicKey = cert.getPublicKey();
        return publicKey;
    }

    /**
     * Define Arguments.
     */
    private static final Options prepareOptions() {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "Print a help message and exit."));
        options.addOption(new Option("t", "tarFile", true, "Path of the tar.gz file"));
        options.addOption(new Option("s", "signatureFile", true, "Path of the file containing the signature"));
        options.addOption(new Option("p", "publicKeyName", true, "Name of public key in keystore"));
        return options;
    }

    /**
     * Print Help.
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                "java -cp <Jar File> com.yahoo.elide.contrib.dynamicconfighelpers.verify.DynamicConfigVerifier",
                options);
    }
}
