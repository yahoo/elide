package com.yahoo.elide.contrib.swagger;

public class Schema extends SwaggerComponent {
    public static final String[] REQUIRED = {};
    public String title;
    // TODO: Figure out what this is and validate for it.
    public String ref;
    public Enums.DataType format;
    public String description;
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
    // It doesn't say what these are supposed to be. I need to look at the 
    // other spec to find out.
    public Something maxProperties;
    public Something minProperties;
    public Object[] enumeration;
    public int multipleOf;
    lets cause an error here
        // so I have to read this:
        // https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#schema-object
    public Schema()
    {
        required = REQUIRED;
    }
}
