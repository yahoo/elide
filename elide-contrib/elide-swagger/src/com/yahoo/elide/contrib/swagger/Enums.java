package com.yahoo.elide.contrib.swagger;

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
    }
}
