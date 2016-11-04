package com.yahoo.elide.contrib.swagger;

import java.lang.reflect.Field;
import java.util.*;

public class SwaggerComponent {
    protected String[] required = {};
    public boolean checkRequired(){
        for(String field : required)
        {
            Field f;
            try {
                f = this.getClass().getDeclaredField(field);
            }
            catch(NoSuchFieldException e)
            {
                throw new RuntimeException(String.format("The field %s does not exist in %s",
                            field, this.getClass().getName()));
            }
            try {
                if(f.get(this) == null)
                    return false;
            }
            catch(IllegalAccessException e)
            {
                throw new RuntimeException(String.format("The field %s does not have the proper access in %s",
                            field, this.getClass().getName()));
            }
        }
        return true;
    }

    public static boolean checkAllRequired(SwaggerComponent head) {
        if(!head.checkRequired())
            return false;
        boolean children = true;
        for(Field f : head.getClass().getDeclaredFields())
        {
            if(!children)
                return false;

            try {
            if(f.get(head) instanceof SwaggerComponent)
            {
                SwaggerComponent comp = (SwaggerComponent) f.get(head);
                children = children && SwaggerComponent.checkAllRequired(comp);
            }
            }
            catch (IllegalAccessException e)
            {
                continue;
            }
        }
        return children;
    }
}
