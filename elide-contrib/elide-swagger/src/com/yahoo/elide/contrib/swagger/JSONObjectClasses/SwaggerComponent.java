package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

import java.lang.reflect.Field;
import java.util.HashMap;

public class SwaggerComponent implements Requirer {
    protected String[] required = {};
    public HashMap<String, Object> patternedFields;
    public void checkRequired() throws SwaggerValidationException {
        for(String field : required)
        {
            Field f;
            try {
                f = this.getClass().getDeclaredField(field);
            }
            catch(NoSuchFieldException e)
            {
                throw new SwaggerValidationException(String.format("The field %s does not exist in %s",
                            field, this.getClass().getName()));
            }
            try {
                if(f.get(this) == null)
                    throw new SwaggerValidationException(String.format("The field %s in %s should not be null",
                                field, this.getClass().getName()));
            }
            catch(IllegalAccessException e)
            {
                throw new SwaggerValidationException(String.format("The field %s does not have the proper access in %s",
                            field, this.getClass().getName()));
            }
        }
    }

    /*
     * This method will throw an error if something isn't valid (hopefully
     * a useful one)
     */
    public static void checkAllRequired(SwaggerComponent head) throws SwaggerValidationException {
        head.checkRequired();
        for(Field f : head.getClass().getFields())
        {
            try {
                if(f.get(head) instanceof SwaggerComponent)
                {
                    SwaggerComponent comp = (SwaggerComponent) f.get(head);
                    SwaggerComponent.checkAllRequired(comp);
                }
                else if(f.get(head) instanceof Requirer)
                {
                    Requirer comp = (Requirer) f.get(head);
                    comp.checkRequired();
                }
            }
            catch (IllegalAccessException e)
            {
                continue;
            }
        }
    }
}
