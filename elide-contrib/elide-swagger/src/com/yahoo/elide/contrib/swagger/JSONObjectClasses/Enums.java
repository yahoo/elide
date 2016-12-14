/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

import com.google.gson.annotations.SerializedName;

public class Enums {
    public enum Location {
        @SerializedName("query")
        QUERY,

        @SerializedName("header")
        HEADER,

        @SerializedName("path")
        PATH,

        @SerializedName("formData")
        FORM_DATA, 

        @SerializedName("body")
        BODY
    };

    public enum Format {
        @SerializedName("csv")
        CSV, 

        @SerializedName("SSV")
        SSV,

        @SerializedName("TSV")
        TSV,

        @SerializedName("pipes")
        PIPES,

        @SerializedName("multi")
        MULTI
    };

    public enum Type {
        @SerializedName("string")
        STRING, 

        @SerializedName("number")
        NUMBER,

        @SerializedName("integer")
        INTEGER,

        @SerializedName("boolean")
        BOOLEAN,

        @SerializedName("array")
        ARRAY, 

        @SerializedName("file")
        FILE, 
    };

    public enum Scheme {
        @SerializedName("wss")
        WSS, 

        @SerializedName("ws")
        WS, 

        @SerializedName("http")
        HTTP,

        @SerializedName("https")
        HTTPS
    };

    public enum DataType {
        @SerializedName("integer")
        INTEGER,

        @SerializedName("long")
        LONG,

        @SerializedName("float")
        FLOAT,

        @SerializedName("double")
        DOUBLE,

        @SerializedName("string")
        STRING,

        @SerializedName("byte")
        BYTE,

        @SerializedName("binary")
        BINARY,

        @SerializedName("boolean")
        BOOLEAN,

        @SerializedName("date")
        DATE,

        @SerializedName("dateTime")
        DATETIME,

        @SerializedName("password")
        PASSWORD
    };

    public enum SecurityType {
        @SerializedName("basic")
        BASIC, 

        @SerializedName("apiKey")
        APIKEY, 

        @SerializedName("oauth2")
        OAUTH2
    };

    public enum ApiKeyLocation {
        @SerializedName("query")
        QUERY, 

        @SerializedName("header")
        HEADER
    };

    public enum Flow {
        @SerializedName("implicit")
        IMPLICIT,
        @SerializedName("password")
        PASSWORD,
        @SerializedName("application")
        APPLICATION,
        @SerializedName("accessCode")
        ACCESSCODE
    }
}
