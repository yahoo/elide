package graphql.java.generator.field.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import graphql.java.generator.field.strategies.FieldObjectsStrategy;
import graphql.java.generator.type.reflect.ReflectionUtils;

/**
 * A reflection based, java Method centric class for generating data
 * on GraphQL fields.  This is as opposed to a Field centric way of generating
 * the same fields.
 * @author dwinsor
 *
 */
public class FieldObjects_ReflectionClassMethods implements FieldObjectsStrategy {
    
    /**
     * Looks at {@link java.lang.reflect.Method}s and returns those
     * which are suitable.
     */
    @Override
    public List<Object> getFieldRepresentativeObjects(Object object) {
        Class<?> clazz = ReflectionUtils.extractClassFromSupportedObject(object);
        if (clazz == null) return null;
        
        List<Object> fieldObjects = new ArrayList<Object>();
        Method[] methods = clazz.getMethods();

        methodloop:
        for (int index = 0; index < methods.length; ++index) {
            Method method = methods[index];
            if (method.isSynthetic()) {
                continue;
            }

            /* Skip static methods */
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            /* Skip any methods that take parameters that cycle */
            if (method.getParameterCount() > 0) {
                for (Class<?> parameterType : method.getParameterTypes()) {
                    if (parameterType.equals(clazz)) {
                        continue methodloop;
                    }
                }
            }

            String methodName = method.getName();
            if (!(methodName.startsWith("get") || methodName.startsWith("is"))) {
                continue;
            }
            if (methodName.equals("getClass")) {
                continue;
            }
            fieldObjects.add(method);
        }
        return fieldObjects;
    }
}
