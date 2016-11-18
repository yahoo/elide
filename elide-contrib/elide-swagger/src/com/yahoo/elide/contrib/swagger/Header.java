package com.yahoo.elide.contrib.swagger;

public class Header extends SwaggerComponent {
    public String description;
    public Enums.Type type;
    public Enums.DataType format;
    public Items items;
    public Enums.Format collectionFormat;
    public Header defaultValue;
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
    public Object[] enumeration;
    public int multipleOf;

    @Override
    public void checkRequired()
    {
        super.checkRequired();

        if(type == Enums.Type.ARRAY)
            if(items == null)
                throw new RuntimeException("If the type is an array, then the items (ie, the thing describing what's in the array) can't be null");
    }
}
