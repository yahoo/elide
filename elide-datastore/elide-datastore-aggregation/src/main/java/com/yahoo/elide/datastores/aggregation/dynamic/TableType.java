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
import static com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage.DEFAULT;
import static com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage.DEFAULT_NAMESPACE;
import static com.yahoo.elide.datastores.aggregation.timegrains.Time.TIME_TYPE;
import static com.yahoo.elide.modelconfig.model.Type.BOOLEAN;
import static com.yahoo.elide.modelconfig.model.Type.COORDINATE;
import static com.yahoo.elide.modelconfig.model.Type.DECIMAL;
import static com.yahoo.elide.modelconfig.model.Type.ENUM_ORDINAL;
import static com.yahoo.elide.modelconfig.model.Type.ENUM_TEXT;
import static com.yahoo.elide.modelconfig.model.Type.INTEGER;
import static com.yahoo.elide.modelconfig.model.Type.MONEY;
import static com.yahoo.elide.modelconfig.model.Type.TEXT;
import static com.yahoo.elide.modelconfig.model.Type.TIME;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.core.type.Method;
import com.yahoo.elide.core.type.Package;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.JoinType;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.annotation.TableSource;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.query.DefaultMetricProjectionMaker;
import com.yahoo.elide.datastores.aggregation.query.MetricProjectionMaker;
import com.yahoo.elide.datastores.aggregation.query.TableSQLMaker;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.modelconfig.model.Argument;
import com.yahoo.elide.modelconfig.model.Dimension;
import com.yahoo.elide.modelconfig.model.Grain;
import com.yahoo.elide.modelconfig.model.Join;
import com.yahoo.elide.modelconfig.model.Measure;
import com.yahoo.elide.modelconfig.model.Table;
import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A dynamic Elide model that wraps a deserialized HJSON table.
 */
public class TableType implements Type<DynamicModelInstance> {
    private static final long serialVersionUID = 8197172323227250923L;
    private static final String SPACE = " ";
    private static final String PERIOD = ".";

    public static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{(.+?)}}");
    public static final Pattern NEWLINE = Pattern.compile(System.lineSeparator(), Pattern.LITERAL);

    protected Table table;
    private transient Map<Class<? extends Annotation>, Annotation> annotations;
    private Map<String, Field> fields;
    private Package namespace;

    public TableType(Table table) {
        this(table, DEFAULT_NAMESPACE);
    }

    public TableType(Table table, Package namespace) {
        this.namespace = namespace;
        this.table = table;
        this.annotations = buildAnnotations(table);
        this.fields = buildFields(table);
    }

    @Override
    public String getCanonicalName() {
        return getPackage().getName().concat(PERIOD).concat(getName());
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
        return namespace;
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
    public Method getMethod(String name, Type<?>... parameterTypes) throws NoSuchMethodException {
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

                    @Override
                    public boolean toOne() {
                        return join.getKind() == Join.Kind.TOONE;
                    }
                });
        return annotations;
    }

    private static Map<Class<? extends Annotation>, Annotation> buildAnnotations(Table table) {
        Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

        annotations.put(Include.class, getIncludeAnnotation(table));

        if (StringUtils.isNotEmpty(table.getSql()) || StringUtils.isNotEmpty(table.getMaker())) {
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
                public Class<? extends TableSQLMaker> maker() {
                    if (StringUtils.isEmpty(table.getMaker())) {
                        return null;
                    }

                    try {
                        return Class.forName(table.getMaker()).asSubclass(TableSQLMaker.class);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
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
                    if (StringUtils.isNotEmpty(table.getSchema())) {
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
            public String[] hints() {
                return table.getHints().toArray(new String[0]);
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
            public boolean isHidden() {
                return table.getHidden() != null && table.getHidden();
            }

            @Override
            public CardinalitySize size() {
                if (StringUtils.isEmpty(table.getCardinality())) {
                    return CardinalitySize.UNKNOWN;
                }
                return CardinalitySize.valueOf(table.getCardinality().toUpperCase(Locale.ENGLISH));
            }

            @Override
            public ArgumentDefinition[] arguments() {
                return getArgumentDefinitions(table.getArguments());
            }
        });

        String readPermission = table.getReadAccess();
        if (StringUtils.isNotEmpty(readPermission)) {
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

    private static ArgumentDefinition[] getArgumentDefinitions(List<Argument> arguments) {
        int numArguments = arguments == null ? 0 : arguments.size();
        ArgumentDefinition[] definitions = new ArgumentDefinition[numArguments];
        for (int idx = 0; idx < numArguments; idx++) {
            Argument argument = arguments.get(idx);
            definitions[idx] = new ArgumentDefinition() {

                @Override
                public String name() {
                    return argument.getName();
                }

                @Override
                public String description() {
                    return argument.getDescription();
                }

                @Override
                public ValueType type() {
                    return ValueType.valueOf(argument.getType().toUpperCase(Locale.ROOT));
                }

                @Override
                public TableSource tableSource() {
                    return buildTableSource(argument.getTableSource());
                }

                @Override
                public String[] values() {
                    return argument.getValues().toArray(new String[0]);
                }

                @Override
                public String defaultValue() {
                    Object value = argument.getDefaultValue();
                    return value == null ? null : value.toString();
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return ArgumentDefinition.class;
                }
            };
        }
        return definitions;
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
            public TableSource tableSource() {
                return buildTableSource(null);
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
            public boolean isHidden() {
                return measure.getHidden() != null && measure.getHidden();
            }

            @Override
            public String filterTemplate() {
                return measure.getFilterTemplate();
            }

            @Override
            public CardinalitySize size() {
                return CardinalitySize.UNKNOWN;
            }
        });

        annotations.put(MetricFormula.class, new MetricFormula() {
            @Override
            public ArgumentDefinition[] arguments() {
                return getArgumentDefinitions(measure.getArguments());
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return MetricFormula.class;
            }

            @Override
            public String value() {
                if (measure.getDefinition() != null) {
                    return trimColumnReferences(measure.getDefinition());
                } else {
                    return "";
                }
            }

            @Override
            public Class<? extends MetricProjectionMaker> maker() {
                if (StringUtils.isEmpty(measure.getMaker())) {
                    return DefaultMetricProjectionMaker.class;
                }

                try {
                    return Class.forName(measure.getMaker()).asSubclass(MetricProjectionMaker.class);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        });

        String readPermission = measure.getReadAccess();
        if (StringUtils.isNotEmpty(readPermission)) {
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

    private static TableSource buildTableSource(com.yahoo.elide.modelconfig.model.TableSource source) {
        if (source == null) {
            return buildTableSource(
                    new com.yahoo.elide.modelconfig.model.TableSource("", DEFAULT, "", new HashSet<>()));
        }
        return new TableSource() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return TableSource.class;
            }

            @Override
            public String table() {
                return source.getTable();
            }

            @Override
            public String namespace() {
                return source.getNamespace();
            }

            @Override
            public String column() {
                return source.getColumn();
            }

            @Override
            public String[] suggestionColumns() {
                return source.getSuggestionColumns().toArray(new String[0]);
            }
        };
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
            public TableSource tableSource() {
                return buildTableSource(dimension.getTableSource());
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
            public boolean isHidden() {
                return dimension.getHidden() != null && dimension.getHidden();
            }

            @Override
            public String filterTemplate() {
                return dimension.getFilterTemplate();
            }

            @Override
            public CardinalitySize size() {
                if (StringUtils.isEmpty(dimension.getCardinality())) {
                    return CardinalitySize.UNKNOWN;
                }
                return CardinalitySize.valueOf(dimension.getCardinality().toUpperCase(Locale.ENGLISH));
            }
        });

        annotations.put(DimensionFormula.class, new DimensionFormula() {
            @Override
            public ArgumentDefinition[] arguments() {
                return getArgumentDefinitions(dimension.getArguments());
            }

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
        if (StringUtils.isNotEmpty(readPermission)) {
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

        if (dimension.getType().toUpperCase(Locale.ROOT).equals(ENUM_ORDINAL)) {
            annotations.put(Enumerated.class, getEnumeratedAnnotation(EnumType.ORDINAL));
        }

        if (dimension.getType().toUpperCase(Locale.ROOT).equals(TIME)) {
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
                                if (StringUtils.isEmpty(sql)) {
                                    return "{{$$column.expr}}";
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

    private static Enumerated getEnumeratedAnnotation(EnumType type) {
        return new Enumerated() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Enumerated.class;
            }

            @Override
            public EnumType value() {
                return EnumType.ORDINAL;
            }
        };
    }

    private static Map<String, Field> buildFields(Table table) {
        Map<String, Field> fields = new HashMap<>();
        fields.put("id", buildIdField());

        table.getDimensions().forEach(dimension ->
            fields.put(dimension.getName(),
                    new FieldType(dimension.getName(), getFieldType(dimension.getType()), buildAnnotations(dimension)))
        );

        table.getMeasures().forEach(measure ->
            fields.put(measure.getName(),
                    new FieldType(measure.getName(), getFieldType(measure.getType()), buildAnnotations(measure)))
        );

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

        annotations.put(GeneratedValue.class, new GeneratedValue() {

            @Override
            public GenerationType strategy() {
                return GenerationType.AUTO;
            }

            @Override
            public String generator() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return GeneratedValue.class;
            }
        });

        annotations.put(ColumnMeta.class, new ColumnMeta() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ColumnMeta.class;
            }

            @Override
            public String friendlyName() {
                return "Row Number";
            }

            @Override
            public String description() {
                return "Row number for each record returned by a query.";
            }

            @Override
            public String category() {
                return null;
            }

            @Override
            public TableSource tableSource() {
                return buildTableSource(null);
            }

            @Override
            public String[] tags() {
                return new String[0];
            }

            @Override
            public String[] values() {
                return new String[0];
            }

            @Override
            public boolean isHidden() {
                return false;
            }

            @Override
            public String filterTemplate() {
                return "";
            }

            @Override
            public CardinalitySize size() {
                return CardinalitySize.UNKNOWN;
            }
        });

        return new FieldType("id", LONG_TYPE, annotations);
    }

    private static Type getFieldType(String inputType) {
        switch (inputType.toUpperCase(Locale.ROOT)) {
            case TIME:
                return TIME_TYPE;
            case TEXT:
            case ENUM_ORDINAL:
            case ENUM_TEXT:
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
        return (str == null) ? null : NEWLINE.matcher(str).replaceAll(SPACE);
    }

    @Override
    public String toString() {
        return String.format("TableType{ name=%s }", table.getGlobalName());
    }

    private static Include getIncludeAnnotation(Table table) {
        return new Include() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Include.class;
            }

            @Override
            public boolean rootLevel() {
                return true;
            }

            @Override
            public String description() {
                return table.getDescription();
            }

            @Override
            public String friendlyName() {
                return table.getFriendlyName();
            }

            @Override
            public String name() {
                return table.getName();
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TableType tableType = (TableType) o;
        return table.equals(tableType.table) && namespace.equals(tableType.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, namespace);
    }
}
