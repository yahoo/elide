package graphql.java.generator.field.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import graphql.java.generator.field.strategies.FieldNameStrategy;

public class FieldName_Reflection implements FieldNameStrategy {

    @Override
    public String getFieldName(Object object) {
        if (object instanceof Field) {
            return getFieldNameFromField((Field)object);
        }
        if (object instanceof Method) {
            return getFieldNameFromMethod((Method)object);
        }
        return null;
    }
    
    protected String getFieldNameFromField(Field field) {
        return (field.getName());
    }
    
    protected String getFieldNameFromMethod(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return methodName.substring(3, 4).toLowerCase()
                    + methodName.substring(4);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return methodName.substring(2, 3).toLowerCase()
                    + methodName.substring(3);
        }
        return methodName;
    }
}
