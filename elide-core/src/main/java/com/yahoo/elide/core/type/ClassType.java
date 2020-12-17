package com.yahoo.elide.core.type;

import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ClassType<T> implements Type<T> {
    @Getter
    private Class<T> cls;

    public ClassType(Class<T> cls) {
        this.cls = cls;
    }

    @Override
    public String getSimpleName() {
        return cls.getSimpleName();
    }

    @Override
    public String getName() {
        return cls.getName();
    }

    @Override
    public Type<T> getSuperclass() {

        return new ClassType(cls.getSuperclass());
    }
    @Override
    public Field[] getFields() {
        return Arrays.stream(cls.getFields())
                .map(ClassType::constructField).collect(Collectors.toList()).toArray(new Field[0]);
    }
    @Override
    public Field[] getDeclaredFields() {
        return Arrays.stream(cls.getDeclaredFields())
                .map(ClassType::constructField).collect(Collectors.toList()).toArray(new Field[0]);
    }

    @Override
    public Package getPackage() {
        return constructPackage(cls.getPackage());
    }

    @Override
    public Method[] getMethods() {
        return Arrays.stream(cls.getMethods())
                .map(ClassType::constructMethod).collect(Collectors.toList()).toArray(new Method[0]);
    }

    @Override
    public Method[] getDeclaredMethods() {
        return Arrays.stream(cls.getDeclaredMethods())
                .map(ClassType::constructMethod).collect(Collectors.toList()).toArray(new Method[0]);
    }

    @Override
    public boolean isAssignableFrom(Type other) {
        if (other instanceof DynamicType) {
            return false;
        }

        return cls.isAssignableFrom(((ClassType) other).getCls());
    }

    @Override
    public Annotation getAnnotation(Class annotationClass) {
        return cls.getAnnotation(annotationClass);
    }

    @Override
    public Annotation getDeclaredAnnotation(Class annotationClass) {
        return cls.getDeclaredAnnotation(annotationClass);
    }

    @Override
    public Method getMethod(String name, Type[] parameterTypes)  throws NoSuchMethodException, SecurityException {
        Class<?>[] typeParams = Arrays.stream(parameterTypes)
                .map(ClassType.class::cast)
                .map(ClassType::getCls)
                .collect(Collectors.toList()).toArray(new Class[0]);

        return constructMethod(cls.getMethod(name, typeParams));
    }

    public static Field constructField(java.lang.reflect.Field field) {
        return new Field() {

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                return field.isAnnotationPresent(annotationClass);
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return field.getAnnotation(annotationClass);
            }

            @Override
            public int getModifiers() {
                return field.getModifiers();
            }

            @Override
            public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException {
                return field.get(obj);
            }

            @Override
            public Type<?> getType() {
                return new ClassType(field.getType());
            }

            @Override
            public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
                field.set(obj, value);
            }
        };
    }

    public static Method constructMethod(java.lang.reflect.Method method) {
        return new Method() {
            @Override
            public int getModifiers() {
                return method.getModifiers();
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
                return method.isAnnotationPresent(annotation);
            }

            @Override
            public Object invoke(Object obj, Object... args) throws IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException {
                return method.invoke(obj, args);
            }

            @Override
            public Type<?> getReturnType() {
                return new ClassType(method.getReturnType());
            }
        };
    }

    public static Package constructPackage(java.lang.Package pkg) {
        return new Package() {
            @Override
            public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
                return pkg.getDeclaredAnnotation(annotationClass);
            }

            @Override
            public String getName() {
                return pkg.getName();
            }

            @Override
            public Package getParentPackage() {
                String name = pkg.getName();
                int idx = name.lastIndexOf('.');
                return idx == -1 ? null : constructPackage(java.lang.Package.getPackage(name.substring(0, idx)));
            }
        };
    }
}
