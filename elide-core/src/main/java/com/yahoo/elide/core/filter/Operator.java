/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Operator enum for predicates.
 */
@RequiredArgsConstructor
public enum Operator implements BiFunction<Predicate, EntityDictionary, java.util.function.Predicate> {
    IN("in", true) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                Object val = PersistentResource.getValue(entity, field, dictionary);
                List<Object> coercedValues = values.stream()
                        .map(v -> CoerceUtil.coerce(v, val.getClass())).collect(Collectors.toList());
                return coercedValues.contains(val);
            };
        }
    },
    NOT("not", true) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                Object val = PersistentResource.getValue(entity, field, dictionary);
                List<Object> coercedValues = values.stream()
                        .map(v -> CoerceUtil.coerce(v, val.getClass())).collect(Collectors.toList());
                return !coercedValues.contains(val);
            };
        }
    },
    PREFIX("prefix", true) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("PREFIX can only take one argument");
                }

                Object val = PersistentResource.getValue(entity, field, dictionary);
                String valStr = (String) CoerceUtil.coerce(val, String.class);
                String filterStr = (String) CoerceUtil.coerce(values.get(0), String.class);
                return valStr.startsWith(filterStr);
            };
        }
    },
    POSTFIX("postfix", true) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("POSTFIX can only take one argument");
                }

                Object val = PersistentResource.getValue(entity, field, dictionary);
                String valStr = (String) CoerceUtil.coerce(val, String.class);
                String filterStr = (String) CoerceUtil.coerce(values.get(0), String.class);
                return valStr.endsWith(filterStr);
            };
        }
    },
    INFIX("infix", true) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("INFIX can only take one argument");
                }

                Object val = PersistentResource.getValue(entity, field, dictionary);
                String valStr = (String) CoerceUtil.coerce(val, String.class);
                String filterStr = (String) CoerceUtil.coerce(values.get(0), String.class);
                return valStr.contains(filterStr);
            };
        }
    },
    ISNULL("isnull", false) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                Object val = PersistentResource.getValue(entity, field, dictionary);
                return val == null;
            };
        }
    },
    NOTNULL("notnull", false) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                Object val = PersistentResource.getValue(entity, field, dictionary);
                return val != null;
            };
        }
    },
    LT("lt", true) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("LT can only take one argument");
                }

                Object val = PersistentResource.getValue(entity, field, dictionary);
                Comparable filterComp = (Comparable) CoerceUtil
                        .coerce(CoerceUtil.coerce(values.get(0), val.getClass()), Comparable.class);
                Comparable valComp = (Comparable) CoerceUtil.coerce(val, Comparable.class);
                return valComp.compareTo(filterComp) < 0;
            };
        }
    },
    LE("le", true) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("LE can only take one argument");
                }

                Object val = PersistentResource.getValue(entity, field, dictionary);
                Comparable filterComp = (Comparable) CoerceUtil
                        .coerce(CoerceUtil.coerce(values.get(0), val.getClass()), Comparable.class);
                Comparable valComp = (Comparable) CoerceUtil.coerce(val, Comparable.class);
                return valComp.compareTo(filterComp) <= 0;
            };
        }
    },
    GT("gt", true) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("GT can only take one argument");
                }

                Object val = PersistentResource.getValue(entity, field, dictionary);
                Comparable filterComp = (Comparable) CoerceUtil
                        .coerce(CoerceUtil.coerce(values.get(0), val.getClass()), Comparable.class);
                Comparable valComp = (Comparable) CoerceUtil.coerce(val, Comparable.class);
                return valComp.compareTo(filterComp) > 0;
            };
        }
    },
    GE("ge", true) {
        @Override
        java.util.function.Predicate getFilterFunction(String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("GE can only take one argument");
                }

                Object val = PersistentResource.getValue(entity, field, dictionary);
                Comparable filterComp = (Comparable) CoerceUtil
                        .coerce(CoerceUtil.coerce(values.get(0), val.getClass()), Comparable.class);
                Comparable valComp = (Comparable) CoerceUtil.coerce(val, Comparable.class);
                return valComp.compareTo(filterComp) >= 0;
            };
        }
    };

    @Getter private final String string;
    @Getter private final boolean parameterized;

    /**
     * Returns Operator from query parameter operator string.
     *
     * @param string operator string from query parameter
     * @return Operator
     */
    public static Operator fromString(final String string) {
        for (final Operator operator : values()) {
            if (operator.getString().equals(string)) {
                return operator;
            }
        }

        throw new InvalidPredicateException("Unknown operator in filter: " + string);
    }

    abstract java.util.function.Predicate getFilterFunction(
            String field, List<Object> values, EntityDictionary dictionary);

    @Override
    public java.util.function.Predicate apply(Predicate predicate, EntityDictionary dictionary) {
        return getFilterFunction(predicate.getField(), predicate.getValues(), dictionary);
    }
}
