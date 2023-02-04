/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.dictionary;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.NonTransferable;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.type.AccessibleObject;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.generated.parsers.ExpressionLexer;
import com.yahoo.elide.generated.parsers.ExpressionParser;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extract permissions related annotation data for a model.
 */
@Slf4j
public class EntityPermissions {
    private static final List<Class<? extends Annotation>> PERMISSION_ANNOTATIONS = Arrays.asList(
            ReadPermission.class,
            CreatePermission.class,
            DeletePermission.class,
            NonTransferable.class,
            UpdatePermission.class
    );

    public static final EntityPermissions EMPTY_PERMISSIONS = new EntityPermissions();

    private static final AnnotationBinding EMPTY_BINDING = new AnnotationBinding(null, Collections.emptyMap());
    private final HashMap<Class<? extends Annotation>, AnnotationBinding> bindings = new HashMap<>();

    private static class AnnotationBinding {
        final ParseTree classPermission;
        final Map<String, ParseTree> fieldPermissions;

        public AnnotationBinding(ParseTree classPermission, Map<String, ParseTree> fieldPermissions) {
            this.classPermission = classPermission;
            this.fieldPermissions = fieldPermissions.isEmpty() ? Collections.emptyMap() : fieldPermissions;
        }
    }


    private EntityPermissions() {
    }

    /**
     * Create bindings for entity class to its permission checks.
     * @param cls entity class
     * @param fieldOrMethodList list of fields/methods
     */
    public EntityPermissions(Type<?> cls,
                             Collection<AccessibleObject> fieldOrMethodList)  {
        for (Class<? extends Annotation> annotationClass : PERMISSION_ANNOTATIONS) {
            final Map<String, ParseTree> fieldPermissions = new HashMap<>();
            fieldOrMethodList.stream()
                    .forEach(member -> bindMemberPermissions(fieldPermissions, member, annotationClass));
            if (annotationClass != NonTransferable.class) {
                ParseTree classPermission = bindClassPermissions(cls, annotationClass);
                if (classPermission != null || !fieldPermissions.isEmpty()) {
                    bindings.put(annotationClass, new AnnotationBinding(classPermission, fieldPermissions));
                }
            }
        }
    }

    private ParseTree bindClassPermissions(Type<?> cls, Class<? extends Annotation> annotationClass) {
        Annotation annotation = EntityDictionary.getFirstAnnotation(cls, Arrays.asList(annotationClass));
        return (annotation == null) ? null : getPermissionExpressionTree(annotationClass, annotation);
    }

    private void bindMemberPermissions(Map<String, ParseTree> fieldPermissions,
            AccessibleObject field, Class<? extends Annotation> annotationClass) {
        Annotation annotation = field.getAnnotation(annotationClass);
        if (annotation != null) {
            ParseTree permissions = getPermissionExpressionTree(annotationClass, annotation);
            fieldPermissions.put(EntityBinding.getFieldName(field), permissions);
        }
    }

    private ParseTree getPermissionExpressionTree(Class<? extends Annotation> annotationClass, Annotation annotation) {
        try {
            String expression = (String) annotationClass.getMethod("expression").invoke(annotation);

            boolean hasExpression = !expression.isEmpty();

            if (!hasExpression) {
                log.warn("Poorly configured permission: {} {}", annotationClass.getName(), "no checks specified.");
                throw new IllegalArgumentException("Poorly configured permission '" + annotationClass.getName() + "'");
            }

            return parseExpression(expression);
        } catch (ReflectiveOperationException e) {
            log.warn("Unknown permission: {}, {}", annotationClass.getName(), e.toString());
            throw new IllegalArgumentException("Unknown permission '" + annotationClass.getName() + "'", e);
        }
    }

    public static ParseTree parseExpression(String expression) {
        CharStream is = CharStreams.fromString(expression);
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
        parser.setErrorHandler(new BailErrorStrategy());
        lexer.reset();
        return parser.start();
    }

    /**
     * Does this permission check annotation exist for this entity or any field?
     * @param annotationClass permission class
     * @return true if annotation exists
     */
    public boolean hasChecksForPermission(Class<? extends Annotation> annotationClass) {
        return bindings.containsKey(annotationClass);
    }

    /**
     * Get entity permission ParseTree.
     * @param annotationClass permission class
     * @return entity permission ParseTree or null if none
     */
    public ParseTree getClassChecksForPermission(Class<? extends Annotation> annotationClass) {
        return bindings.getOrDefault(annotationClass, EMPTY_BINDING).classPermission;
    }

    /**
     * Get field permission ParseTree for provided name.
     * @param field provided field name
     * @param annotationClass permission class
     * @return entity permission ParseTree or null if none
     */
    public ParseTree getFieldChecksForPermission(String field, Class<? extends Annotation> annotationClass) {
        return bindings.getOrDefault(annotationClass, EMPTY_BINDING).fieldPermissions.get(field);
    }
}
