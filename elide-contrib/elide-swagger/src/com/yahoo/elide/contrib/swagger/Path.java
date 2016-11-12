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
    public boolean checkRequired(){
        if(!super.checkRequired())
            return false;
        if(!Util.validateRef(ref))
            return false;
        if(Util.hasDuplicates(parameters))
            return false;
        return true;
    }
}
