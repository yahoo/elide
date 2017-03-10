package graphql.java.generator.field.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import graphql.java.generator.field.strategies.FieldDeprecationStrategy;

public class FieldDeprecation_Reflection implements FieldDeprecationStrategy {
    
    @Override
    public String getFieldDeprecation(Object object) {
        if (object instanceof Field) {
            return getDeprecationString((Field)object);
        }
        if (object instanceof Method) {
            return getDeprecationString((Method)object);
        }
        return null;
    }
    
    protected String getDeprecationString(Method method) {
        Annotation annotation = method.getAnnotation(Deprecated.class);

        if (annotation instanceof Deprecated) {
            return "Deprecated by annotation";
        }
        return null;
    }

    protected String getDeprecationString(Field field) {
        Annotation annotation = field.getAnnotation(Deprecated.class);

        if (annotation instanceof Deprecated) {
            return "Deprecated by annotation";
        }
        return null;
    }
}
