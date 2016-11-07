package com.yahoo.elide.contrib.swagger;

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
    public boolean checkRequired()
    {
        if(!super.checkRequired())
            return false;
        if(in == Enums.Location.PATH)
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
        if(in == Enums.Location.PATH && required == false)
            return false;

        if(in == Enums.Location.BODY)
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
            if(type == Enums.Type.ARRAY)
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
