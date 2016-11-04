package com.yahoo.elide.contrib.swagger;

import com.google.gson.annotations.SerializedName;
enum Location {
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

enum Format {
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
}

enum Type {
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
    FILE
}

enum DataType {
    @SerializedName("integer")
    INTEGER,

    @SerializedName("long")
    LONG,

    @SerializedName("long")
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

    @SerializedName("datetime")
    DATETIME,

    @SerializedName("password")
    PASSWORD
}

public class Parameter extends SwaggerComponent {
    private static final String[] REQUIRED = {"name", "in"};
    public String name;
    public Location in;
    public String description;
    public boolean required;
    public Schema schema;
    public Type type;
    public DataType format;
    public boolean allowEmptyValue = false;
    public Items items;
    public Format collectionFormat = Format.CSV;
    // It says this could be any type, but I'm just going to say it's a 
    // String since a human will be reading it and a string can describe anything.
    public String defaultValue;
    public int maximum;
    public boolean exclusiveMaximum;
    public int minimum;
    public boolean exclusiveMinimum;
    public int maxLength;
    public int minLength;
    public String pattern;
    public int maxItems;
    public int minItems;
    public boolean uniqueItems;
    // I'm not 100% sure if this is ever required. Hmmm...
    public Object[] enumeration;
    // I haven't the slightest idea of the significance of this.
    public int multipleOf;

    @Override
    public boolean checkRequired()
    {
        if(!super.checkRequired())
            return false;
        if(in == Location.PATH)
        {
            boolean foundInPaths = true;
            for(String s : Swagger.main.paths.getKeys())
            {
                if(s.equals(name))
                    foundInPaths = true;
            }
            if(!foundInPaths)
                return false;
        }
        if(in == Location.PATH && required == false)
            return false;

        if(in == Location.BODY)
        {
            if(schema == null)
                return false;
        }
        else
        {
            if(type == null)
                return false;
            // TODO: Implement something on the thing that governs this that makes sure that
            // the consumes of that thing is either "multipart/form-data" or "application/x-www-form-urlencoded" or both if the type is "file"
            if(type == DataType.ARRAY)
            {
                if(items == null)
                    return false;
            }
            if(maxLength < 0 || minLength < 0 || maxLength < minLength)
                return false;
            if(minItems < 0 || minItems > maxItems || maxItems < 0)
                return false;
        }
    }
}
