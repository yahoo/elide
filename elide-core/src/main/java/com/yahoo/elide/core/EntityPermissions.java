/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.generated.parsers.ExpressionLexer;
import com.yahoo.elide.generated.parsers.ExpressionParser;
import com.yahoo.elide.security.checks.Check;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extract permissions related annotation data for a model.
 */
@Slf4j
public class EntityPermissions implements CheckInstantiator {
    private static final Class[] PERMISSION_ANNOTATIONS = new Class[]{
            ReadPermission.class,
            CreatePermission.class,
            DeletePermission.class,
            SharePermission.class,
            UpdatePermission.class
    };

    public static final EntityPermissions EMPTY_PERMISSIONS = new EntityPermissions();

    //CHECKSTYLE.OFF: LineLength
    private final ConcurrentHashMap<Class<? extends Annotation>, ParseTree> classPermissions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<Class<? extends Annotation>, ParseTree>> fieldPermissions = new ConcurrentHashMap<>();
    //CHECKSTYLE.ON: LineLength

    private EntityPermissions() {
    }

    public EntityPermissions(Class<?> cls, Collection<AccessibleObject> fieldOrMethodList) {
        initFieldPermissionsMap(fieldOrMethodList);

        for (Class annotationClass : PERMISSION_ANNOTATIONS) {
            bindClassPermissions(cls, annotationClass);

            fieldOrMethodList.stream()
                             .forEach(member -> bindMemberPermissions(member, annotationClass));
        }
    }

    private void initFieldPermissionsMap(Collection<AccessibleObject> fieldOrMethodList) {
        fieldOrMethodList.stream()
                .forEach(member -> {
                    String fieldName = EntityBinding.getFieldName(member);
                    if (fieldName != null) {
                        fieldPermissions.putIfAbsent(fieldName, new ConcurrentHashMap<>());
                    }
                });
    }

    private void bindClassPermissions(Class<?> cls, Class annotationClass) {
        Annotation annotation = cls.getAnnotation(annotationClass);
        if (annotation != null) {
            ParseTree permissions = getPermissionExpressionTree(annotationClass, annotation);
            classPermissions.put(annotationClass, permissions);
        }
    }

    private void bindMemberPermissions(AccessibleObject accessibleObject, Class annotationClass) {
        Annotation annotation = accessibleObject.getAnnotation(annotationClass);
        if (annotation != null) {
            ParseTree permissions = getPermissionExpressionTree(annotationClass, annotation);
            fieldPermissions.get(EntityBinding.getFieldName(accessibleObject))
                            .putIfAbsent(annotationClass, permissions);
        }
    }

    private ParseTree getPermissionExpressionTree(Class annotationClass, Annotation annotation) {
        try {
            String expression = (String) annotationClass.getMethod("expression").invoke(annotation);
            Class<? extends Check>[] allChecks = (Class<? extends Check>[]) annotationClass.getMethod("all")
                                                                                           .invoke(annotation);
            Class<? extends Check>[] anyChecks = (Class<? extends Check>[]) annotationClass.getMethod("any")
                                                                                           .invoke(annotation);

            boolean hasAnyChecks = anyChecks.length > 0;
            boolean hasAllChecks = allChecks.length > 0;
            boolean hasExpression = !expression.isEmpty();

            boolean hasConfiguredChecks = hasAnyChecks || hasAllChecks || hasExpression;
            boolean hasConfiguredOneChecks = hasAnyChecks ^ hasAllChecks ^ hasExpression;

            if (!hasConfiguredChecks || !hasConfiguredOneChecks) {
                log.warn("Poorly configured permission: {} {}", annotationClass.getName(),
                         hasConfiguredChecks ? "more than one set of checks specified" : "no checks specified.");
                throw new IllegalArgumentException("Poorly configured permission '" + annotationClass.getName() + "'");
            }

            if (allChecks.length > 0) {
                expression = listToExpression(allChecks, " and ");
            } else if (anyChecks.length > 0) {
                expression = listToExpression(anyChecks, " or ");
            }

            return parseExpression(expression);
        } catch (ReflectiveOperationException e) {
            log.warn("Unknown permission: {}, {}", annotationClass.getName(), e);
            throw new IllegalArgumentException("Unknown permission '" + annotationClass.getName() + "'", e);
        }
    }

    private String listToExpression(Class<? extends Check>[] allChecks, String conjunction) {
        String expression;
        expression = Arrays.asList(allChecks)
                           .stream()
                           .map(this::instantiateCheck)
                           .map(Check::checkIdentifier)
                           .reduce("",
                                   (current, next) -> current.isEmpty()
                                           ? next
                                           : current + conjunction + next
                           );
        return expression;
    }

    private ParseTree parseExpression(String expression) {
        ANTLRInputStream is = new ANTLRInputStream(expression);
        ExpressionLexer lexer = new ExpressionLexer(is);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                    int charPositionInLine, String msg, RecognitionException e) {
                throw new ParseCancellationException(msg, e);
            }
        });
        ExpressionParser parser = new ExpressionParser(new CommonTokenStream(lexer));
        lexer.reset();
        return parser.start();
    }

    public boolean hasChecksForPermission(Class<? extends Annotation> annotationClass) {
        if (classPermissions.containsKey(annotationClass)) {
            return true;
        }

        for (Map<Class<? extends Annotation>, ParseTree> value : fieldPermissions.values()) {
            if (value.containsKey(annotationClass)) {
                return true;
            }
        }

        return false;
    }

    public ParseTree getClassChecksForPermission(Class<? extends Annotation> annotationClass) {
        return classPermissions.get(annotationClass);
    }

    private static final Map<Class<? extends Annotation>, ParseTree> EMPTY_MAP = new ConcurrentHashMap<>();
    public ParseTree getFieldChecksForPermission(String field, Class<? extends Annotation> annotationClass) {
        return fieldPermissions.getOrDefault(field, EMPTY_MAP).get(annotationClass);
    }
}
