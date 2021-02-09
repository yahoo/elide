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
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.core.type.Method;
import com.yahoo.elide.core.type.Package;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.JoinType;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.modelconfig.model.Dimension;
import com.yahoo.elide.modelconfig.model.Grain;
import com.yahoo.elide.modelconfig.model.Join;
import com.yahoo.elide.modelconfig.model.Measure;
import com.yahoo.elide.modelconfig.model.Table;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Id;

/**
 * A dynamic Elide model that wraps a deserialized HJSON table.
 */
public class TableType implements Type<DynamicModelInstance> {
    public static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{(.+?)}}");
    private static final String SPACE = " ";
    public static final String NEWLINE = System.getProperty("line.separator");

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
        return table.getName();
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
    public DynamicModelInstance newInstance() throws InstantiationException, IllegalAccessException {
        return new DynamicModelInstance(this);
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Override
    public DynamicModelInstance[] getEnumConstants() {
        return null;
    }

    @Override
    public Optional<Class<DynamicModelInstance>> getUnderlyingClass() {
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
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
        if (annotations.containsKey(annotationClass)) {
            A[] result = (A[]) Array.newInstance(annotationClass, 1);
            result[0] = (A) annotations.get(annotationClass);
            return result;
        }
        return (A[]) Array.newInstance(annotationClass, 0);
    }

    @Override
    public Method getMethod(String name, Type[] parameterTypes) throws NoSuchMethodException {
        throw new NoSuchMethodException();
    }

    /**
     * Must be called post construction of all the dynamic types to initialize table join fields.
     * @param tableTypes A map of table name to type.
     */
    public void resolveJoins(Map<String, Type<?>> tableTypes) {
        table.getJoins().forEach(join -> {
            Type joinTableType = tableTypes.get(join.getTo());
            fields.put(join.getName(),
                    new FieldType(join.getName(), joinTableType, buildAnnotations(join)));
        });
    }

    private static Map<Class<? extends Annotation>, Annotation> buildAnnotations(Join join) {
        Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();
        annotations.put(com.yahoo.elide.datastores.aggregation.annotation.Join.class,
                new com.yahoo.elide.datastores.aggregation.annotation.Join() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return com.yahoo.elide.datastores.aggregation.annotation.Join.class;
                    }

                    @Override
                    public String value() {
                        return trimColumnReferences(join.getDefinition());
                    }

                    @Override
                    public JoinType type() {
                        if (join.getType() == null) {
                            return JoinType.LEFT;
                        }
                        return JoinType.valueOf(join.getType().name());
                    }
                });
        return annotations;
    }

    private static Map<Class<? extends Annotation>, Annotation> buildAnnotations(Table table) {
        Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

        if (table.getHidden() == null || table.getHidden() == false) {
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
        } else {
            annotations.put(Exclude.class, new Exclude() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return Exclude.class;
                }
            });
        }

        if (table.getSql() != null && !table.getSql().isEmpty()) {
            annotations.put(FromSubquery.class, new FromSubquery() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return FromSubquery.class;
                }

                @Override
                public String sql() {
                    return table.getSql();
                }

                @Override
                public String dbConnectionName() {
                    return table.getDbConnectionName();
                }
            });
        } else {
            annotations.put(FromTable.class, new FromTable() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return FromTable.class;
                }

                @Override
                public String name() {
                    String tableName = table.getTable();
                    if (table.getSchema() != null && ! table.getSchema().isEmpty()) {
                        return table.getSchema() + "." + tableName;

                    }
                    return tableName;
                }

                @Override
                public String dbConnectionName() {
                    return table.getDbConnectionName();
                }
            });
        }

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
                if (table.getCardinality() == null || table.getCardinality().isEmpty()) {
                    return CardinalitySize.UNKNOWN;
                }
                return CardinalitySize.valueOf(table.getCardinality().toUpperCase(Locale.ENGLISH));
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

        if (measure.getHidden() != null && measure.getHidden() == true) {
            annotations.put(Exclude.class, new Exclude() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return Exclude.class;
                }
            });
        }

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
                return trimColumnReferences(measure.getDefinition());
            }

            @Override
            public Class<? extends QueryPlanResolver> queryPlan() {
                if (measure.getQueryPlanResolver() == null || measure.getQueryPlanResolver().isEmpty()) {
                    return DefaultQueryPlanResolver.class;
                }

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

        if (dimension.getHidden() != null && dimension.getHidden() == true) {
            annotations.put(Exclude.class, new Exclude() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return Exclude.class;
                }
            });
        }

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
                return dimension.getTableSource() == null ? "" : dimension.getTableSource();
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
                if (dimension.getCardinality() == null || dimension.getCardinality().isEmpty()) {
                    return CardinalitySize.UNKNOWN;
                }
                return CardinalitySize.valueOf(dimension.getCardinality().toUpperCase(Locale.ENGLISH));
            }
        });

        annotations.put(DimensionFormula.class, new DimensionFormula() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return DimensionFormula.class;
            }

            @Override
            public String value() {
                return trimColumnReferences(dimension.getDefinition());
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
                    int numGrains = dimension.getGrains() == null ? 0 : dimension.getGrains().size();
                    TimeGrainDefinition[] definitions = new TimeGrainDefinition[numGrains];
                    for (int idx = 0; idx < numGrains; idx++) {
                        Grain grain = dimension.getGrains().get(idx);
                        definitions[idx] = new TimeGrainDefinition() {

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return TimeGrainDefinition.class;
                            }

                            @Override
                            public TimeGrain grain() {
                                if (grain.getType() == null) {
                                    return TimeGrain.DAY;
                                }
                                return TimeGrain.valueOf(grain.getType().name());
                            }

                            @Override
                            public String expression() {
                                String sql = grain.getSql();
                                if (sql == null || sql.isEmpty()) {
                                    return "{{}}";
                                }
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

    /**
     * Removes whitespace around column references.
     * @param str eg: {{ playerCountry.id}} = {{country_id}}
     * @return String without whitespace around column references eg: {{playerCountry.id}} = {{country_id}}
     */
    private static String trimColumnReferences(String str) {
        String expr = replaceNewlineWithSpace(str);
        Matcher matcher = REFERENCE_PARENTHESES.matcher(expr);
        while (matcher.find()) {
            String reference = matcher.group(1);
            expr = expr.replace(reference, reference.trim());
        }
        return expr;
    }

    private static String replaceNewlineWithSpace(String str) {
        return (str == null) ? str : str.replace(NEWLINE, SPACE);
    }
}
