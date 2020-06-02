/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.verify;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
     * Main Method to Verify Signature of Model Tar file.
     * @param args : expects 3 arguments.
     * @throws ParseException
     * @throws IOException
     * @throws KeyStoreException
     * @throws FileNotFoundException
     * @throws SignatureException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     *
     */
    public static void main(String[] args) throws ParseException, InvalidKeyException, NoSuchAlgorithmException,
    SignatureException, FileNotFoundException, KeyStoreException, IOException {

        Options options = prepareOptions();
        CommandLine cli = new DefaultParser().parse(options, args);

        if (cli.hasOption("help")) {
            printHelp(options);
            return;
        }
        if (!cli.hasOption("tarFile") || !cli.hasOption("signatureFile") || !cli.hasOption("publicKeyName")) {
            printHelp(options);
            throw new MissingOptionException("Missing required option");
        }

        String modelTarFile = cli.getOptionValue("tarFile");
        String signatureFile = cli.getOptionValue("signatureFile");
        String publicKeyName = cli.getOptionValue("publicKeyName");
        long tarFileSize = getFileSize(modelTarFile);

        if (verify(readTarContents(modelTarFile), tarFileSize,
                signatureFile, getPublicKey(publicKeyName))) {
            log.info("Successfully Validated " + modelTarFile);
        }
        else {
            log.error("Could not verify " + modelTarFile + " with details provided");
        }
    }

    /**
     * Verify signature of tar.gz.
     * @param fileList : list of files
     * @param sizeOfFile : size of tar file
     * @param signature : file containing signature
     * @param publicKey : public key name
     * @return whether the file can be verified by given key and signature
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public static boolean verify(String fileList, long sizeOfFile, String signature, PublicKey publicKey)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        Signature publicSignature;

        publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update((fileList + sizeOfFile).getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        return publicSignature.verify(signatureBytes);
    }

    /**
     * Get tar file size.
     * @param modelTarFile
     * @return size of tar file
     */
    private static long getFileSize(String modelTarFile) {
        return FileUtils.sizeOf(new File(modelTarFile));
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

    private static String readTarContents(String archiveFile) throws FileNotFoundException, IOException {
        StringBuffer sb = new StringBuffer();
        TarArchiveInputStream archive = new TarArchiveInputStream(
                new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(archiveFile))));
        TarArchiveEntry entry;
        while ((entry = archive.getNextTarEntry()) != null) {
            sb.append(entry.getName());
        }
        return sb.toString();
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
