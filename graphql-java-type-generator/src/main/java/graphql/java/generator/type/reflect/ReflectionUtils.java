package graphql.java.generator.type.reflect;

import java.lang.reflect.ParameterizedType;

public class ReflectionUtils {
    public static Class<?> extractClassFromSupportedObject(Object object) {
        if (object == null) return null;
        if (object instanceof ParameterizedType) {
            object = ((ParameterizedType) object).getRawType();
        }
        if (!(object instanceof Class<?>)) {
            object = object.getClass();
        }
        Class<?> clazz = (Class<?>) object;
        return clazz;
    }
}
