/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class Parameter extends SwaggerComponent {
    private static final String[] REQUIRED = {"name", "in"};
    public String name;
    public Enums.Location in;
    public String description;
    public boolean required;
    public Schema schema;
    public Enums.Type type;
    public Enums.DataType format;
    public boolean allowEmptyValue = false;
    public Items items;
    public Enums.Format collectionFormat = Enums.Format.CSV;
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
    public void checkRequired() throws SwaggerValidationException
    {
        super.checkRequired();

        if(in == Enums.Location.PATH)
        {
            boolean foundInPaths = true;
            for(String s : Swagger.main.paths.keySet())
            {
                if(s.equals(name))
                    foundInPaths = true;
            }
            if(!foundInPaths)
                throw new SwaggerValidationException("You can't have a parameter that goes into the path without having it documented in the paths field in the Swagger object");
        }
        if(in == Enums.Location.PATH && required == false)
            throw new SwaggerValidationException("You can't have an optional path parameter");

        if(in == Enums.Location.BODY)
        {
            if(schema == null)
                throw new SwaggerValidationException("If a parameter is in the body, you have to include a schema to describe how it is formatted");
        }
        else
        {
            if(type == null)
                throw new SwaggerValidationException("If a parameter isn't in the body or the path, it has to have a type");
            if(type == Enums.Type.ARRAY)
            {
                if(items == null)
                    throw new SwaggerValidationException("If the type of the parameter is an array, you have to have an items object to describe the things in the array.");
            }
            if(maxLength < 0 || minLength < 0 || maxLength < minLength)
                throw new SwaggerValidationException("The maxlenth or minlength don't make sense");
            if(minItems < 0 || minItems > maxItems || maxItems < 0)
                throw new SwaggerValidationException("The maxitems or minitems don't make sense");
        }
    }

    @Override 
    public int hashCode()
    {
        int retval = 0;
        if(name != null)
            retval += name.hashCode();
        if(in != null)
            retval += in.hashCode();
        return retval;
    }

    @Override
    public boolean equals(Object o)
    {
        if(o == null)
            return false;
        if(!this.getClass().isInstance(o))
            return false;
        return o.hashCode() == this.hashCode();
    }
}
