/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.sql.Blob;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

public class BlobSerdeTest {

    @Test
    public void testBlobSerialize() throws SerialException, SQLException {

        String responseBody = "responseBody";
        Blob newBlob = new SerialBlob(responseBody.getBytes());

        BlobSerde blobSerde = new BlobSerde();
        Object actual = blobSerde.serialize(newBlob);
        assertEquals(responseBody, actual);
    }

    @Test
    public void testBlobDeserialize() throws SerialException, SQLException {

        String responseBody = "responseBody";
        Blob expectedBlob = new SerialBlob(responseBody.getBytes());

        BlobSerde blobSerde = new BlobSerde();
        Object actualBlob = blobSerde.deserialize(responseBody);
        assertEquals(expectedBlob, actualBlob);
    }

    @Test
    public void testBlobDeserializeNull() throws SerialException, SQLException {

        Blob expectedBlob = new SerialBlob("".getBytes());

        BlobSerde blobSerde = new BlobSerde();
        Object actualBlob = blobSerde.deserialize(null);
        assertEquals(expectedBlob, actualBlob);
    }

    @Test
    public void testBlobserializeNull() throws SerialException, SQLException {

        BlobSerde blobSerde = new BlobSerde();
        Object actualBlob = blobSerde.serialize(null);
        assertEquals("", actualBlob);
    }
}
