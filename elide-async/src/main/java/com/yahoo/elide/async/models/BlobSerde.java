/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.sql.Blob;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialBlob;

/**
 * Serde class for bidirectional conversion from Blob type to String.
 */
@ElideTypeConverter(type = Blob.class, name = "Blob")
public class BlobSerde implements Serde<String, Blob> {
    private static final String EMPTY_STRING = "";

    @Override
    public Blob deserialize(String val) {
        try {
            val = val == null ? EMPTY_STRING : val;
            return new SerialBlob(val.getBytes());
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String serialize(Blob val) {
        try {
            return val == null ? EMPTY_STRING : new String(val.getBytes(1, (int) val.length()));
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
