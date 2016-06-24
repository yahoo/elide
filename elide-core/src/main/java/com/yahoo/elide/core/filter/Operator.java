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

/**
 * Operator enum for predicates.
 */
@RequiredArgsConstructor
public enum Operator implements BiFunction<Predicate, EntityDictionary, java.util.function.Predicate> {
    IN("in", true) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                Object val = PersistentResource.getValue(entity, field, dictionary);
                return val != null && values.stream()
                        .map(v -> CoerceUtil.coerce(v, val.getClass()))
                        .anyMatch(val::equals);
            };
        }
    },

    NOT("not", true) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> !IN.getFilterFunction(field, values, dictionary).test(entity);
        }
    },

    PREFIX("prefix", true) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("PREFIX can only take one argument");
                }

                Object val = PersistentResource.getValue(entity, field, dictionary);
                String valStr = CoerceUtil.coerce(val, String.class);
                String filterStr = CoerceUtil.coerce(values.get(0), String.class);
                return valStr != null && filterStr != null && valStr.startsWith(filterStr);
            };
        }
    },

    POSTFIX("postfix", true) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("POSTFIX can only take one argument");
                }

                Object val = PersistentResource.getValue(entity, field, dictionary);
                String valStr = CoerceUtil.coerce(val, String.class);
                String filterStr = CoerceUtil.coerce(values.get(0), String.class);
                return valStr != null && filterStr != null && valStr.endsWith(filterStr);
            };
        }
    },

    INFIX("infix", true) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("INFIX can only take one argument");
                }

                Object val = PersistentResource.getValue(entity, field, dictionary);
                String valStr = CoerceUtil.coerce(val, String.class);
                String filterStr = CoerceUtil.coerce(values.get(0), String.class);
                return valStr != null && filterStr != null && valStr.contains(filterStr);
            };
        }
    },

    ISNULL("isnull", false) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                Object val = PersistentResource.getValue(entity, field, dictionary);
                return val == null;
            };
        }
    },

    NOTNULL("notnull", false) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> !ISNULL.getFilterFunction(field, values, dictionary).test(entity);
        }
    },

    LT("lt", true) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("LT can only take one argument");
                }
                Object val = PersistentResource.getValue(entity, field, dictionary);
                return val != null && getComparisonResult(val, values.get(0)) < 0;
            };
        }
    },

    LE("le", true) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("LE can only take one argument");
                }
                Object val = PersistentResource.getValue(entity, field, dictionary);
                return val != null && getComparisonResult(val, values.get(0)) <= 0;
            };
        }
    },

    GT("gt", true) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("LT can only take one argument");
                }
                Object val = PersistentResource.getValue(entity, field, dictionary);
                return val != null && getComparisonResult(val, values.get(0)) > 0;
            };
        }
    },

    GE("ge", true) {
        @Override
        public java.util.function.Predicate getFilterFunction(
                String field, List<Object> values, EntityDictionary dictionary) {
            return (entity) -> {
                if (values.size() != 1) {
                    throw new InvalidPredicateException("LE can only take one argument");
                }
                Object val = PersistentResource.getValue(entity, field, dictionary);
                return val != null && getComparisonResult(val, values.get(0)) >= 0;
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

    public abstract java.util.function.Predicate getFilterFunction(
            String field, List<Object> values, EntityDictionary dictionary);

    @Override
    public java.util.function.Predicate apply(Predicate predicate, EntityDictionary dictionary) {
        return getFilterFunction(predicate.getField(), predicate.getValues(), dictionary);
    }

    private static int getComparisonResult(Object val, Object rawFilterVal) {
        Object filterVal = CoerceUtil.coerce(rawFilterVal, val.getClass());
        Comparable filterComp = CoerceUtil.coerce(filterVal, Comparable.class);
        Comparable valComp = CoerceUtil.coerce(val, Comparable.class);
        return valComp.compareTo(filterComp);
    }
}
