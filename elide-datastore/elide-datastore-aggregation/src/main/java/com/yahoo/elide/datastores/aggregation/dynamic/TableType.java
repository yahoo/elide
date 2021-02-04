/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dynamic;

import static com.yahoo.elide.core.type.ClassType.BIGDECIMAL_TYPE;
import static com.yahoo.elide.core.type.ClassType.BOOLEAN_TYPE;
import static com.yahoo.elide.core.type.ClassType.LONG_TYPE;
import static com.yahoo.elide.core.type.ClassType.STRING_TYPE;
import static com.yahoo.elide.datastores.aggregation.timegrains.Time.TIME_TYPE;
import static com.yahoo.elide.modelconfig.model.Type.TIME;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.core.type.Method;
import com.yahoo.elide.core.type.Package;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanResolver;
import com.yahoo.elide.modelconfig.model.Dimension;
import com.yahoo.elide.modelconfig.model.Grain;
import com.yahoo.elide.modelconfig.model.Measure;
import com.yahoo.elide.modelconfig.model.Table;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.persistence.Id;

public class TableType implements Type {
    protected Table table;
    private Map<Class<? extends Annotation>, Annotation> annotations;
    private Map<String, Field> fields;

    public TableType(Table table) {
        this.table = table;
        this.annotations = buildAnnotations(table);
        this.fields = buildFields(table);
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
        return fields.values().toArray(new Field[0]);
    }

    @Override
    public Field[] getDeclaredFields() {
        return getFields();
    }

    @Override
    public Field getDeclaredField(String name) throws NoSuchFieldException {
        if (fields.containsKey(name)) {
            return fields.get(name);
        }
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
        throw new NoSuchMethodException();
    }

    private static Map<Class<? extends Annotation>, Annotation> buildAnnotations(Table table) {
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

    private static Map<Class<? extends Annotation>, Annotation> buildAnnotations(Measure measure) {
        Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

        annotations.put(ColumnMeta.class, new ColumnMeta() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ColumnMeta.class;
            }

            @Override
            public String friendlyName() {
                return measure.getFriendlyName();
            }

            @Override
            public String description() {
                return measure.getDescription();
            }

            @Override
            public String category() {
                return measure.getCategory();
            }

            @Override
            public String tableSource() {
                return "";
            }

            @Override
            public String[] tags() {
                return measure.getTags().toArray(new String[0]);
            }

            @Override
            public String[] values() {
                return new String[0];
            }

            @Override
            public CardinalitySize size() {
                return CardinalitySize.UNKNOWN;
            }
        });

        annotations.put(MetricFormula.class, new MetricFormula() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return MetricFormula.class;
            }

            @Override
            public String value() {
                return measure.getDefinition();
            }

            @Override
            public Class<? extends QueryPlanResolver> queryPlan() {
                try {
                    return (Class<? extends QueryPlanResolver>) Class.forName(measure.getQueryPlanResolver());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        });

        String readPermission = measure.getReadAccess();
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

    private static Map<Class<? extends Annotation>, Annotation> buildAnnotations(Dimension dimension) {
        Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

        annotations.put(ColumnMeta.class, new ColumnMeta() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ColumnMeta.class;
            }

            @Override
            public String friendlyName() {
                return dimension.getFriendlyName();
            }

            @Override
            public String description() {
                return dimension.getDescription();
            }

            @Override
            public String category() {
                return dimension.getCategory();
            }

            @Override
            public String tableSource() {
                return dimension.getTableSource();
            }

            @Override
            public String[] tags() {
                return dimension.getTags().toArray(new String[0]);
            }

            @Override
            public String[] values() {
                return dimension.getValues().toArray(new String[0]);
            }

            @Override
            public CardinalitySize size() {
                return CardinalitySize.valueOf(dimension.getCardinality());
            }
        });

        annotations.put(DimensionFormula.class, new DimensionFormula() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return DimensionFormula.class;
            }

            @Override
            public String value() {
                return dimension.getDefinition();
            }
        });

        String readPermission = dimension.getReadAccess();
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

        if (dimension.getType() == TIME) {
            annotations.put(Temporal.class, new Temporal() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return Temporal.class;
                }

                @Override
                public TimeGrainDefinition[] grains() {
                    TimeGrainDefinition[] definitions = new TimeGrainDefinition[dimension.getGrains().size()];
                    for (int idx = 0; idx < dimension.getGrains().size(); idx++) {
                        Grain grain = dimension.getGrains().get(idx);
                        definitions[idx] = new TimeGrainDefinition() {

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return TimeGrainDefinition.class;
                            }

                            @Override
                            public TimeGrain grain() {
                                return TimeGrain.valueOf(grain.getType().name());
                            }

                            @Override
                            public String expression() {
                                return grain.getSql();
                            }
                        };
                    }
                    return definitions;
                }

                @Override
                public String timeZone() {
                    return "UTC";
                }
            });
        }

        return annotations;
    }

    private static Map<String, Field> buildFields(Table table) {
        Map<String, Field> fields = new HashMap<>();
        fields.put("id", buildIdField());

        table.getDimensions().forEach(dimension -> {
            fields.put(dimension.getName(),
                    new FieldType(dimension.getName(), getFieldType(dimension.getType()), buildAnnotations(dimension)));
        });

        table.getMeasures().forEach(measure -> {
            fields.put(measure.getName(),
                    new FieldType(measure.getName(), getFieldType(measure.getType()), buildAnnotations(measure)));
        });

        return fields;
    }

    private static Field buildIdField() {
        Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();
        annotations.put(Id.class, new Id() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Id.class;
            }
        });

        return new FieldType("id", LONG_TYPE, annotations);
    }

    private static Type getFieldType(com.yahoo.elide.modelconfig.model.Type inputType) {
        switch (inputType) {
            case TIME:
                return TIME_TYPE;
            case TEXT:
                return STRING_TYPE;
            case MONEY:
                return BIGDECIMAL_TYPE;
            case BOOLEAN:
                return BOOLEAN_TYPE;
            case DECIMAL:
                return BIGDECIMAL_TYPE;
            case INTEGER:
                return LONG_TYPE;
            case COORDINATE:
                return STRING_TYPE;
            default:
                return STRING_TYPE;
        }
    }
}
