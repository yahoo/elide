/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Operator enum for predicates.
 */
@RequiredArgsConstructor
public enum Operator {
    IN("in", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.in(field, values, requestScope);
        }
    },

    NOT("not", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.notIn(field, values, requestScope);
        }
    },

    PREFIX_CASE_INSENSITIVE("prefixi", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return Operator.prefix(field, values, requestScope, s -> s.toLowerCase(Locale.ENGLISH));
        }
    },

    PREFIX("prefix", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.prefix(field, values, requestScope);
        }
    },

    POSTFIX("postfix", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.postfix(field, values, requestScope);
        }
    },

    POSTFIX_CASE_INSENSITIVE("postfixi", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return Operator.postfix(field, values, requestScope, s -> s.toLowerCase(Locale.ENGLISH));
        }
    },

    INFIX("infix", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.infix(field, values, requestScope);
        }
    },

    INFIX_CASE_INSENSITIVE("infixi", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return Operator.infix(field, values, requestScope, s -> s.toLowerCase(Locale.ENGLISH));
        }
    },

    ISNULL("isnull", false) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.isNull(field, requestScope);
        }
    },

    NOTNULL("notnull", false) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.isNotNull(field, requestScope);
        }
    },

    LT("lt", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.lt(field, values, requestScope);
        }
    },

    LE("le", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.le(field, values, requestScope);
        }
    },

    GT("gt", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.gt(field, values, requestScope);
        }
    },

    GE("ge", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.ge(field, values, requestScope);
        }
    },

    TRUE("true", false) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return Operator.isTrue();
        }
    },

    FALSE("false", false) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
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
            String field, List<Object> values, RequestScope requestScope);

    //
    // Predicate generation
    //

    private static <T> Predicate<T> in(
            String field, List<Object> values, RequestScope requestScope) {
        return (T entity) -> {
            Object val = getFieldValue(entity, field, requestScope);

            return val != null && values.stream()
                    .map(v -> CoerceUtil.coerce(v, val.getClass()))
                    .anyMatch(val::equals);
        };
    }

    private static <T> Predicate<T> notIn(String field, List<Object> values, RequestScope requestScope) {
        return (T entity) -> {
            Object val = getFieldValue(entity, field, requestScope);

            return val == null || values.stream()
                    .map(v -> CoerceUtil.coerce(v, val.getClass()))
                    .noneMatch(val::equals);
        };
    }

    private static <T> Predicate<T> prefix(
            String field, List<Object> values, RequestScope requestScope) {
        return prefix(field, values, requestScope, Function.identity());
    }

    private static <T> Predicate<T> prefix(
            String field, List<Object> values, RequestScope requestScope, Function<String, String> transform) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("PREFIX can only take one argument");
            }

            Object val = getFieldValue(entity, field, requestScope);
            String valStr = CoerceUtil.coerce(val, String.class);
            String filterStr = CoerceUtil.coerce(values.get(0), String.class);

            return valStr != null
                    && filterStr != null
                    && transform.apply(valStr).startsWith(transform.apply(filterStr));
        };
    }

    private static <T> Predicate<T> postfix(
            String field, List<Object> values, RequestScope requestScope) {
        return postfix(field, values, requestScope, Function.identity());
    }

    private static <T> Predicate<T> postfix(
            String field, List<Object> values, RequestScope requestScope, Function<String, String> transform) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("POSTFIX can only take one argument");
            }

            Object val = getFieldValue(entity, field, requestScope);
            String valStr = CoerceUtil.coerce(val, String.class);
            String filterStr = CoerceUtil.coerce(values.get(0), String.class);

            return valStr != null
                    && filterStr != null
                    && transform.apply(valStr).endsWith(transform.apply(filterStr));
        };
    }

    private static <T> Predicate<T> infix(String field, List<Object> values, RequestScope requestScope) {
        return infix(field, values, requestScope, Function.identity());
    }

    private static <T> Predicate<T> infix(
            String field, List<Object> values, RequestScope requestScope, Function<String, String> transform) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("INFIX can only take one argument");
            }

            Object val = getFieldValue(entity, field, requestScope);
            String valStr = CoerceUtil.coerce(val, String.class);
            String filterStr = CoerceUtil.coerce(values.get(0), String.class);

            return valStr != null
                    && filterStr != null
                    && transform.apply(valStr).contains(transform.apply(filterStr));
        };
    }

    private static <T> Predicate<T> isNull(String field, RequestScope requestScope) {
        return (T entity) -> {
            Object val = getFieldValue(entity, field, requestScope);
            return val == null;
        };
    }

    private static <T> Predicate<T> isNotNull(String field, RequestScope requestScope) {
        return (T entity) -> {
            Object val = getFieldValue(entity, field, requestScope);

            return val != null;
        };
    }

    private static <T> Predicate<T> lt(String field, List<Object> values, RequestScope requestScope) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("LT can only take one argument");
            }
            Object val = getFieldValue(entity, field, requestScope);

            return val != null
                    && getComparisonResult(val, values.get(0)) < 0;
        };
    }

    private static <T> Predicate<T> le(String field, List<Object> values, RequestScope requestScope) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("LE can only take one argument");
            }
            Object val = getFieldValue(entity, field, requestScope);

            return val != null
                    && getComparisonResult(val, values.get(0)) <= 0;
        };
    }

    private static <T> Predicate<T> gt(String field, List<Object> values, RequestScope requestScope) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("GT can only take one argument");
            }
            Object val = getFieldValue(entity, field, requestScope);

            return val != null
                    && getComparisonResult(val, values.get(0)) > 0;
        };
    }

    private static <T> Predicate<T> ge(String field, List<Object> values, RequestScope requestScope) {
        return (T entity) -> {
            if (values.size() != 1) {
                throw new InvalidPredicateException("GE can only take one argument");
            }
            Object val = getFieldValue(entity, field, requestScope);

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
     *
     * @param entity Entity bean
     * @param fieldPath field value/path
     * @param requestScope Request scope
     * @return the value of the field
     */
    public static <T> Object getFieldValue(T entity, String fieldPath, RequestScope requestScope) {
        Object val = entity;
        for (String field : fieldPath.split("\\.")) {
            if ("this".equals(field)) {
                continue;
            }
            val = PersistentResource.getValue(val, field, requestScope);
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
