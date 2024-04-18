/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils.coerce.converters;

import com.paiondata.elide.core.exceptions.InvalidValueException;

import java.net.MalformedURLException;
import java.net.URL;

public class URLSerde implements Serde<String, URL> {

    @Override
    public URL deserialize(String val) {
        URL url;
        try {
            url = new URL(val);
        } catch (MalformedURLException e) {
            throw new InvalidValueException("Invalid URL " + val);
        }
        return url;
    }

    @Override
    public String serialize(URL val) {
        return val.toString();
    }
}
