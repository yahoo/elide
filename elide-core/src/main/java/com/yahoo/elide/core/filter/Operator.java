/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidOperatorNegationException;
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
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return in(field, values, requestScope);
        }
    },

    IN_INSENSITIVE("ini", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return in(field, values, requestScope, FOLD_CASE);
        }
    },

    NOT("not", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return (T entity) -> !in(field, values, requestScope).test(entity);
        }
    },

    NOT_INSENSITIVE("noti", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return (T entity) -> !in(field, values, requestScope, FOLD_CASE).test(entity);
        }
    },

    PREFIX_CASE_INSENSITIVE("prefixi", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return prefix(field, values, requestScope, s -> s.toLowerCase(Locale.ENGLISH));
        }
    },

    PREFIX("prefix", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return prefix(field, values, requestScope, Function.identity());
        }
    },

    POSTFIX("postfix", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return postfix(field, values, requestScope, Function.identity());
        }
    },

    POSTFIX_CASE_INSENSITIVE("postfixi", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return postfix(field, values, requestScope, FOLD_CASE);
        }
    },

    INFIX("infix", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return infix(field, values, requestScope, Function.identity());
        }
    },

    INFIX_CASE_INSENSITIVE("infixi", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return infix(field, values, requestScope, FOLD_CASE);
        }
    },

    ISNULL("isnull", false) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return isNull(field, requestScope);
        }
    },

    NOTNULL("notnull", false) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return (val) -> !isNull(field, requestScope).test(val);
        }
    },

    LT("lt", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return lt(field, values, requestScope);
        }
    },

    LE("le", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return le(field, values, requestScope);
        }
    },

    GT("gt", true) {
        @Override
        public <T> Predicate<T> contextualize(
                String field, List<Object> values, RequestScope requestScope) {
            return gt(field, values, requestScope);
        }
    },

    GE("ge", true) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return ge(field, values, requestScope);
        }
    },

    TRUE("true", false) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return isTrue();
        }
    },

    FALSE("false", false) {
        @Override
        public <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope) {
            return isFalse();
        }
    },
    ;

    public static final Function<String, String> FOLD_CASE = s -> s.toLowerCase(Locale.ENGLISH);
    @Getter private final String notation;
    @Getter private final boolean parameterized;
    private Operator negated;

    // initialize negated values
    static {
        GE.negated = LT;
        GT.negated = LE;
        LE.negated = GT;
        LT.negated = GE;
        IN.negated = NOT;
        IN_INSENSITIVE.negated = NOT_INSENSITIVE;
        NOT.negated = IN;
        NOT_INSENSITIVE.negated = IN_INSENSITIVE;
        TRUE.negated = FALSE;
        FALSE.negated = TRUE;
        ISNULL.negated = NOTNULL;
        NOTNULL.negated = ISNULL;
    }

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

    public abstract <T> Predicate<T> contextualize(String field, List<Object> values, RequestScope requestScope);

    //
    // Predicate generation
    //

    //
    // In with strict equality
    private static <T> Predicate<T> in(String field, List<Object> values, RequestScope requestScope) {
        return (T entity) -> {
            Object val = getFieldValue(entity, field, requestScope);

            return val != null && values.stream()
                    .map(v -> CoerceUtil.coerce(v, val.getClass()))
                    .anyMatch(val::equals);
        };
    }

    //
    // String-like In with optional transformation
    private static <T> Predicate<T> in(String field, List<Object> values,
                                       RequestScope requestScope, Function<String, String> transform) {
        return (T entity) -> {
            Object fieldValue = getFieldValue(entity, field, requestScope);

            if (fieldValue == null) {
                return false;
            }

            if (!fieldValue.getClass().isAssignableFrom(String.class)) {
                throw new IllegalStateException("Cannot case insensitive compare non-string values");
            }

            String val = transform.apply((String) fieldValue);
            return val != null && values.stream()
                    .map(v -> transform.apply(CoerceUtil.coerce(v, String.class)))
                    .anyMatch(val::equals);
        };
    }

    //
    // String-like prefix matching with optional transformation
    private static <T> Predicate<T> prefix(String field, List<Object> values,
                                           RequestScope requestScope, Function<String, String> transform) {
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

    //
    // String-like postfix matching with optional transformation
    private static <T> Predicate<T> postfix(String field, List<Object> values,
                                            RequestScope requestScope, Function<String, String> transform) {
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

    //
    // String-like infix matching with optional transformation
    private static <T> Predicate<T> infix(String field, List<Object> values,
                                          RequestScope requestScope, Function<String, String> transform) {
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

    //
    // Null checking
    private static <T> Predicate<T> isNull(String field, RequestScope requestScope) {
        return (T entity) -> getFieldValue(entity, field, requestScope) == null;
    }

    private static <T> Predicate<T> lt(String field, List<Object> values, RequestScope requestScope) {
        return getComparator(field, values, requestScope, compareResult -> compareResult < 0);
    }

    private static <T> Predicate<T> le(String field, List<Object> values, RequestScope requestScope) {
        return getComparator(field, values, requestScope, compareResult -> compareResult <= 0);
    }

    private static <T> Predicate<T> gt(String field, List<Object> values, RequestScope requestScope) {
        return getComparator(field, values, requestScope, compareResult -> compareResult > 0);
    }

    private static <T> Predicate<T> ge(String field, List<Object> values, RequestScope requestScope) {
        return getComparator(field, values, requestScope, compareResult -> compareResult >= 0);
    }

    private static <T> Predicate<T> isTrue() {
        return (T entity) -> true;
    }

    private static <T> Predicate<T> isFalse() {
        return (T entity) -> false;
    }

    /**
     * Return value of field/path for given entity.  For example this.book.author
     *
     * @param <T> the type of entity to retrieve a value from
     * @param entity Entity bean
     * @param fieldPath field value/path
     * @param requestScope Request scope
     * @return the value of the field
     */
    private static <T> Object getFieldValue(T entity, String fieldPath, RequestScope requestScope) {
        Object val = entity;
        for (String field : fieldPath.split("\\.")) {
            if ("this".equals(field)) {
                continue;
            }
            if (val == null) {
                break;
            }
            val = PersistentResource.getValue(val, field, requestScope);
        }
        return val;
    }

    private static <T> Predicate<T> getComparator(String field, List<Object> values,
                                                  RequestScope requestScope, Predicate<Integer> condition) {
        return (T entity) -> {
            if (values.size() == 0) {
                throw new InvalidPredicateException("No value to compare");
            }
            Object fieldVal = getFieldValue(entity, field, requestScope);
            return fieldVal != null
                    && values.stream()
                    .anyMatch(testVal -> condition.test(compare(fieldVal, testVal)));
        };

    }

    private static int compare(Object fieldValue, Object rawTestValue) {
        Object testValue = CoerceUtil.coerce(rawTestValue, fieldValue.getClass());
        Comparable testComp = CoerceUtil.coerce(testValue, Comparable.class);
        Comparable fieldComp = CoerceUtil.coerce(fieldValue, Comparable.class);

        return fieldComp.compareTo(testComp);
    }

    public Operator negate() {
        if (negated == null) {
            throw new InvalidOperatorNegationException();
        }
        return negated;
    }
}
