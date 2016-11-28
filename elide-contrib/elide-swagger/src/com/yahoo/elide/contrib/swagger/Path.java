package com.yahoo.elide.contrib.swagger;

public class Path extends SwaggerComponent {
    private static final String[] REQUIRED = {};
    public String ref;
    public Operation get;
    public Operation put;
    public Operation post;
    public Operation delete;
    public Operation options;
    public Operation head;
    public Operation patch;
    public Parameter[] parameters;
    public Path()
    {
        required = REQUIRED;
    }

    @Override
    public void checkRequired(){
        super.checkRequired();

        if(ref != null && !Util.validateRef(ref))
            throw new RuntimeException("The ref is invalid!");
        if(parameters != null && Util.hasDuplicates(parameters))
            throw new RuntimeException("Parameters can't have duplicates");
    }
}
