/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dynamic;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.core.type.Method;
import com.yahoo.elide.core.type.Package;
import com.yahoo.elide.core.type.ParameterizedModel;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.modelconfig.model.Table;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TableType implements Type {
    protected Table table;
    private Map<Class<? extends Annotation>, Annotation> annotations;
    private Map<String, Field> fields;

    public TableType(Table table) {
        this.table = table;
        this.annotations = buildAnnotations(table);
    }

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
        return new DynamicModelInstance(this);
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
        return annotations.containsKey(annotationClass);
    }

    @Override
    public Annotation getAnnotation(Class annotationClass) {
        if (annotations.containsKey(annotationClass)) {
            return annotations.get(annotationClass);
        }
        return null;
    }

    @Override
    public Annotation getDeclaredAnnotation(Class annotationClass) {
        return getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotationsByType(Class annotationClass) {
        if (annotations.containsKey(annotationClass)) {
            Annotation [] result = new Annotation[1];
            result[0] = annotations.get(annotationClass);
            return result;
        }
        return new Annotation[0];
    }

    @Override
    public Method getMethod(String name, Type[] parameterTypes) throws NoSuchMethodException {
        return null;
    }

    private Map<Class<? extends Annotation>, Annotation> buildAnnotations(Table table) {
        Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();
        annotations.put(Include.class, new Include() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Include.class;
            }

            @Override
            public boolean rootLevel() {
                return true;
            }

            @Override
            public String type() {
                return table.getName();
            }
        });

        annotations.put(TableMeta.class, new TableMeta() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return TableMeta.class;
            }

            @Override
            public String friendlyName() {
                return table.getFriendlyName();
            }

            @Override
            public String description() {
                return table.getDescription();
            }

            @Override
            public String category() {
                return table.getCategory();
            }

            @Override
            public String[] tags() {
                return table.getTags().toArray(new String[0]);
            }

            @Override
            public String filterTemplate() {
                return table.getFilterTemplate();
            }

            @Override
            public boolean isFact() {
                return table.getIsFact();
            }

            @Override
            public CardinalitySize size() {
                return CardinalitySize.valueOf(table.getCardinality());
            }
        });

        String readPermission = table.getReadAccess();
        if (readPermission != null && !readPermission.isEmpty()) {
            annotations.put(ReadPermission.class, new ReadPermission() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return ReadPermission.class;
                }

                @Override
                public String expression() {
                    return readPermission;
                }
            });
        }
        return annotations;
    }


    private Map<String, Field> buildFields(Table table) {
        Map<String, Field> fields = new HashMap<>();

        table.getDimensions().forEach(dimension -> {
            fields.put(dimension.getName(), new Field() {

                @Override
                public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException {
                    if (! ParameterizedModel.class.isAssignableFrom(obj.getClass()) {
                        throw new IllegalArgumentException("Class is not a dynamic type: " + obj.getClass());
                    }

                    ParameterizedModel model = (ParameterizedModel) obj;

                    return model.invoke(Attribute.builder()
                            .name(dimension.getName())
                            .alias(dimension.getName())
                            .type()
                            .build());
                    return null;
                }

                @Override
                public Type<?> getType() {
                    return null;
                }

                @Override
                public Type<?> getParameterizedType(Type<?> parentType, Optional<Integer> index) {
                    return null;
                }

                @Override
                public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {

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
            });

        });

        return fields;
    }
}
