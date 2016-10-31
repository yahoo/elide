package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class Schema extends SwaggerComponent {
    public static final String[] REQUIRED = {};
    public String title;
    public String ref;
    public Enums.DataType format;
    public String description;
    // default is a Java reserved word, so I have this
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
    public int maxProperties;
    public int minProperties;
    // I guess this is everything this is allowed to be? 
    // I dunno. May the gods help you in filling this one out.
    public Object[] enumeration;
    public int multipleOf;
    public Enums.Type type;
    public Schema()
    {
        required = REQUIRED;
    }
    @Override
    public void checkRequired() throws SwaggerValidationException
    {
        super.checkRequired();
        if(!Util.validateRef(ref))
            throw new SwaggerValidationException("The ref is invalid!");
    }
}
