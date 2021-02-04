package com.yahoo.elide.modelconfig.model;

import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.core.type.Method;
import com.yahoo.elide.core.type.Package;
import com.yahoo.elide.core.type.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class TableType implements Type {

    @Override
    public String getCanonicalName() {
        return getName();
    }

    @Override
    public String getSimpleName() {
        return getName();
    }

    @Override
    public String getName() {
        return getName();
    }

    @Override
    public Type<?> getSuperclass() {
        return null;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public Package getPackage() {
        return new Package() {
            @Override
            public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
                return null;
            }

            @Override
            public String getName() {
                return "config";
            }

            @Override
            public Package getParentPackage() {
                return null;
            }
        };
    }

    @Override
    public Method[] getMethods() {
        return new Method[0];
    }

    @Override
    public Method[] getDeclaredMethods() {
        return new Method[0];
    }

    @Override
    public Field[] getFields() {
        return new Field[0];
    }

    @Override
    public Field[] getDeclaredFields() {
        return new Field[0];
    }

    @Override
    public Field getDeclaredField(String name) throws NoSuchFieldException {
        return null;
    }

    @Override
    public Method[] getConstructors() {
        Method constructor = new Method() {
            @Override
            public int getParameterCount() {
                return 0;
            }

            @Override
            public Object invoke(Object obj, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                return new ;
            }

            @Override
            public Type<?> getReturnType() {
                return null;
            }

            @Override
            public Type<?> getParameterizedReturnType(Type<?> parentType, Optional<Integer> index) {
                return null;
            }

            @Override
            public Class<?>[] getParameterTypes() {
                return new Class[0];
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                return false;
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }

            @Override
            public int getModifiers() {
                return 0;
            }
        }
        return new Method[0];
    }

    @Override
    public boolean isParameterized() {
        return false;
    }

    @Override
    public boolean hasSuperType() {
        return false;
    }

    @Override
    public Object newInstance() throws InstantiationException, IllegalAccessException {
        return null;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Override
    public Object[] getEnumConstants() {
        return new Object[0];
    }

    @Override
    public Optional<Class> getUnderlyingClass() {
        return Optional.empty();
    }

    @Override
    public boolean isAssignableFrom(Type cls) {
        return false;
    }

    @Override
    public boolean isAnnotationPresent(Class annotationClass) {
        return false;
    }

    @Override
    public Annotation getAnnotation(Class annotationClass) {
        return null;
    }

    @Override
    public Annotation getDeclaredAnnotation(Class annotationClass) {
        return null;
    }

    @Override
    public Annotation[] getAnnotationsByType(Class annotationClass) {
        return new Annotation[0];
    }

    @Override
    public Method getMethod(String name, Type[] parameterTypes) throws NoSuchMethodException {
        return null;
    }
}
