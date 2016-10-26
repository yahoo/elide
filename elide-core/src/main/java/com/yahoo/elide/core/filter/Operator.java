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
import java.util.function.Predicate;

/**
 * Operator enum for predicates.
 */
@RequiredArgsConstructor
public enum Operator {
    IN("in", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.in(field, values, dictionary);
        }
    },

    NOT("not", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.notIn(field, values, dictionary);
        }
    },

    PREFIX("prefix", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.prefix(field, values, dictionary);
        }
    },

    POSTFIX("postfix", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.postfix(field, values, dictionary);
        }
    },

    INFIX("infix", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.infix(field, values, dictionary);
        }
    },

    ISNULL("isnull", false) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.isNull(field, dictionary);
        }
    },

    NOTNULL("notnull", false) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.isNotNull(field, dictionary);
        }
    },

    LT("lt", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.lt(field, values, dictionary);
        }
    },

    LE("le", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.le(field, values, dictionary);
        }
    },

    GT("gt", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.gt(field, values, dictionary);
        }
    },

    GE("ge", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.ge(field, values, dictionary);
        }
    },

    TRUE("true", false) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.isTrue();
        }
    },

    FALSE("false", false) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, EntityDictionary dictionary) {
            return Operator.isFalse();
        }
    };

    @Getter private final String notation;
    @Getter private final boolean parameterized;

    /**
     * Returns Operator from query parameter operator notation.
     *
     * @param string operator notation from query parameter
     * @return Operator
     */
    public static Operator fromString(final String string) {
        for (final Operator operator : values()) {
            if (operator.getNotation().equals(string)) {
                return operator;
            }
        }

        throw new InvalidPredicateException("Unknown operator in filter: " + string);
    }

    public abstract <T> Predicate<T> contextualize(
            String field, List<Object> values, EntityDictionary dictionary);

    //
    // Predicate generation
    //

    private static <T> Predicate<T> in(
            String field, List<Object> values, EntityDictionary dictionary) {
        return (T entity) -> {
            Object val = getFieldValue(entity, field, dictionary);

            return val != null && values.stream()
                    .map(v -> CoerceUtil.coerce(v, val.getClass()))
                    .anyMatch(val::equals);
        };
    }

    private static <T> Predicate<T> notIn(String field, List<Object> values, EntityDictionary dictionary) {
        return (T entity) -> {
            Object val = getFieldValue(entity, field, dictionary);

            return val == null || values.stream()
                    .map(v -> CoerceUtil.coerce(v, val.getClass()))
                    .noneMatch(val::equals);
        };
    }

    private static <T> Predicate<T> prefix(
            String field, List<Object> values, EntityDictionary dictionary) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("PREFIX can only take one argument");
            }

            Object val = getFieldValue(entity, field, dictionary);
            String valStr = CoerceUtil.coerce(val, String.class);
            String filterStr = CoerceUtil.coerce(values.get(0), String.class);

            return valStr != null
                    && filterStr != null
                    && valStr.startsWith(filterStr);
        };
    }

    private static <T> Predicate<T> postfix(
            String field, List<Object> values, EntityDictionary dictionary) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("POSTFIX can only take one argument");
            }

            Object val = getFieldValue(entity, field, dictionary);
            String valStr = CoerceUtil.coerce(val, String.class);
            String filterStr = CoerceUtil.coerce(values.get(0), String.class);

            return valStr != null
                    && filterStr != null
                    && valStr.endsWith(filterStr);
        };
    }

    private static <T> Predicate<T> infix(String field, List<Object> values, EntityDictionary dictionary) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("INFIX can only take one argument");
            }

            Object val = getFieldValue(entity, field, dictionary);
            String valStr = CoerceUtil.coerce(val, String.class);
            String filterStr = CoerceUtil.coerce(values.get(0), String.class);

            return valStr != null
                    && filterStr != null
                    && valStr.contains(filterStr);
        };
    }

    private static <T> Predicate<T> isNull(String field, EntityDictionary dictionary) {
        return (T entity) -> {
            Object val = getFieldValue(entity, field, dictionary);
            return val == null;
        };
    }

    private static <T> Predicate<T> isNotNull(String field, EntityDictionary dictionary) {
        return (T entity) -> {
            Object val = getFieldValue(entity, field, dictionary);

            return val != null;
        };
    }

    private static <T> Predicate<T> lt(String field, List<Object> values, EntityDictionary dictionary) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("LT can only take one argument");
            }
            Object val = getFieldValue(entity, field, dictionary);

            return val != null
                    && getComparisonResult(val, values.get(0)) < 0;
        };
    }

    private static <T> Predicate<T> le(String field, List<Object> values, EntityDictionary dictionary) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("LE can only take one argument");
            }
            Object val = getFieldValue(entity, field, dictionary);

            return val != null
                    && getComparisonResult(val, values.get(0)) <= 0;
        };
    }

    private static <T> Predicate<T> gt(String field, List<Object> values, EntityDictionary dictionary) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("LE can only take one argument");
            }
            Object val = getFieldValue(entity, field, dictionary);

            return val != null
                    && getComparisonResult(val, values.get(0)) > 0;
        };
    }

    private static <T> Predicate<T> ge(String field, List<Object> values, EntityDictionary dictionary) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("LE can only take one argument");
            }
            Object val = getFieldValue(entity, field, dictionary);

            return val != null
                    && getComparisonResult(val, values.get(0)) >= 0;
        };
    }

    private static <T> Predicate<T> isTrue() {
        return (T entity) -> {
            return true;
        };
    }

    private static <T> Predicate<T> isFalse() {
        return (T entity) -> {
            return false;
        };
    }

    /**
     * Return value of field/path for given entity.  For example this.book.author
     * @param entity Entity bean
     * @param fieldPath field value/path
     * @param dictionary
     * @return
     */
    public static <T> Object getFieldValue(T entity, String fieldPath, EntityDictionary dictionary) {
        Object val = entity;
        for (String field : fieldPath.split("\\.")) {
            if ("this".equals(field)) {
                continue;
            }
            val = PersistentResource.getValue(val, field, dictionary);
        }
        return val;
    }

    private static int getComparisonResult(Object val, Object rawFilterVal) {
        Object filterVal = CoerceUtil.coerce(rawFilterVal, val.getClass());
        Comparable filterComp = CoerceUtil.coerce(filterVal, Comparable.class);
        Comparable valComp = CoerceUtil.coerce(val, Comparable.class);

        return valComp.compareTo(filterComp);
    }
}
