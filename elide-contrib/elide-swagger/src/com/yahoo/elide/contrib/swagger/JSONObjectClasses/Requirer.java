package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

interface Requirer {
    public abstract void checkRequired() throws SwaggerValidationException;
}
