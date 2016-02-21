/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.*;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.generated.parsers.ExpressionLexer;
import com.yahoo.elide.generated.parsers.ExpressionParser;
import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Entity Dictionary maps JSON API Entity beans to/from Entity type names.
 *
 * @see com.yahoo.elide.annotation.Include#type
 */
class EntityBinding {
    private static final List<Method> OBJ_METHODS = Arrays.asList(Object.class.getMethods());

    public final String jsonApi;
    public final ConcurrentLinkedDeque<String> attrsDeque;
    public final List<String> attrs;
    public final ConcurrentLinkedDeque<String> relationshipsDeque;
    public final List<String> relationships;
    public final ConcurrentHashMap<String, RelationshipType> relationshipTypes;
    public final ConcurrentHashMap<String, String> relationshipToInverse;
    public final ConcurrentHashMap<String, AccessibleObject> fieldsToValues;
    public final ConcurrentHashMap<String, Class<?>> fieldsToTypes;
    public final ConcurrentHashMap<String, String> aliasesToFields;
    public final ConcurrentHashMap<String, AccessibleObject> accessibleObject;
    public final MultiValueMap<Pair<Class, String>, Method> fieldsToTriggers;

    public final ConcurrentHashMap<Field, ParseTree> fieldToFieldParseTree;
    public ParseTree entityExpressionParseTree;

    @Getter
    private AccessibleObject idField;
    @Getter
    private String idFieldName;
    @Getter
    private Class<?> idType;
    @Getter
    @Setter
    private Initializer initializer;

    public static final EntityBinding EMPTY_BINDING = new EntityBinding();

    /* empty binding constructor */
    private EntityBinding() {
        jsonApi = null;
        idField = null;
        idType = null;
        attrsDeque = null;
        attrs = null;
        relationshipsDeque = null;
        relationships = null;
        relationshipTypes = null;
        relationshipToInverse = null;
        fieldsToValues = null;
        fieldsToTypes = null;
        fieldsToTriggers = new MultiValueMap();
        aliasesToFields = null;
        accessibleObject = null;
        entityExpressionParseTree = null;
        fieldToFieldParseTree = null;
    }

    public EntityBinding(Class<?> cls, String type) {
        // Map id's, attributes, and relationships
        Collection<AccessibleObject> fieldOrMethodList = CollectionUtils.union(
                Arrays.asList(cls.getFields()),
                Arrays.asList(cls.getMethods()));

        jsonApi = type;
        // Initialize our maps for this entity. Duplicates are checked above.
        attrsDeque = new ConcurrentLinkedDeque<>();
        relationshipsDeque = new ConcurrentLinkedDeque<>();
        relationshipTypes = new ConcurrentHashMap<>();
        relationshipToInverse = new ConcurrentHashMap<>();
        fieldsToValues = new ConcurrentHashMap<>();
        fieldsToTypes = new ConcurrentHashMap<>();
        fieldsToTriggers = new MultiValueMap<>();
        aliasesToFields = new ConcurrentHashMap<>();
        accessibleObject = new ConcurrentHashMap<>();
        fieldToFieldParseTree = new ConcurrentHashMap<>();
        bindEntityFields(cls, type, fieldOrMethodList);
        bindAccessibleObjects(cls, fieldOrMethodList);
        bindAnnotations(cls, fieldOrMethodList);

        attrs = dequeToList(attrsDeque);
        relationships = dequeToList(relationshipsDeque);
    }

    /**
     * Parses (using ANTLR) and Binds annotation expressions to the entity binding.
     */
    private void bindAnnotations(Class<?> cls, Collection<AccessibleObject> fieldOrMethodList) {
        // Set the individual parse tree
        ReadPermission expr = cls.getAnnotation(ReadPermission.class);
        if (expr != null) {
            entityExpressionParseTree = parseExpression(expr.expression());
        }
        // Set the hash map/fields
        for (AccessibleObject obj : fieldOrMethodList) {
            if (obj instanceof Field) {
                Field f = (Field) obj;
                ReadPermission r = f.getAnnotation(ReadPermission.class);
                if (r != null) {
                    fieldToFieldParseTree.putIfAbsent(f, parseExpression(r.expression()));
                }
            }
        }
    }

    /**
     * Parses an expression in an annotation using ANTLR.
     *
     * @param annotationMessage
     * @return
     */
    private ParseTree parseExpression(String annotationMessage) {
        ANTLRInputStream is = new ANTLRInputStream(annotationMessage);
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
        return parser.start();
    }


    /**
     * Bind fields of an entity including the Id field, attributes, and relationships.
     *
     * @param cls               Class type to bind fields
     * @param type              JSON API type identifier
     * @param fieldOrMethodList List of fields and methods on entity
     */
    private void bindEntityFields(Class<?> cls, String type, Collection<AccessibleObject> fieldOrMethodList) {
        for (AccessibleObject fieldOrMethod : fieldOrMethodList) {
            bindTrigger(OnCreate.class, fieldOrMethod);
            bindTrigger(OnDelete.class, fieldOrMethod);
            bindTrigger(OnUpdate.class, fieldOrMethod);
            bindTrigger(OnCommit.class, fieldOrMethod);

            if (fieldOrMethod.isAnnotationPresent(Id.class)) {
                bindEntityId(cls, type, fieldOrMethod);
            } else if (fieldOrMethod.isAnnotationPresent(Transient.class)
                    && !fieldOrMethod.isAnnotationPresent(ComputedAttribute.class)) {
                continue; // Transient. Don't serialize
            } else if (!fieldOrMethod.isAnnotationPresent(Exclude.class)) {
                if (fieldOrMethod instanceof Field && Modifier.isTransient(((Field) fieldOrMethod).getModifiers())) {
                    continue; // Transient. Don't serialize
                }
                if (fieldOrMethod instanceof Method && Modifier.isTransient(((Method) fieldOrMethod).getModifiers())) {
                    continue; // Transient. Don't serialize
                }
                if (fieldOrMethod instanceof Field
                        && !fieldOrMethod.isAnnotationPresent(Column.class)
                        && Modifier.isStatic(((Field) fieldOrMethod).getModifiers())) {
                    continue; // Field must have Column annotation?
                }
                bindAttrOrRelation(cls, fieldOrMethod);
            }
        }
    }

    private void bindAccessibleObjects(Class<?> targetClass, Collection<AccessibleObject> fieldOrMethodList) {
        for (AccessibleObject fieldOrMethod : fieldOrMethodList) {
            String fieldName = getFieldName(fieldOrMethod);
            if (fieldName != null) {
                this.accessibleObject.put(fieldName, fieldOrMethod);
            }
        }
    }

    /**
     * Bind an id field to an entity.
     *
     * @param cls           Class type to bind fields
     * @param type          JSON API type identifier
     * @param fieldOrMethod Field or method to bind
     */
    private void bindEntityId(Class<?> cls, String type, AccessibleObject fieldOrMethod) {
        String fieldName = getFieldName(fieldOrMethod);
        Class<?> fieldType = getFieldType(fieldOrMethod);

        //Add id field to type map for the entity
        fieldsToTypes.put(fieldName, fieldType);

        //Set id field, type, and name
        idField = fieldOrMethod;
        idType = fieldType;
        idFieldName = fieldName;

        if (idField != null && !fieldOrMethod.equals(idField)) {
            throw new DuplicateMappingException(type + " " + cls.getName() + ":" + fieldName);
        }
    }

    /**
     * Convert a deque to a list.
     *
     * @param deque Deque to convert
     * @return Deque as a list
     */
    private static List<String> dequeToList(final Deque<String> deque) {
        ArrayList<String> result = new ArrayList<>();
        deque.stream().forEachOrdered(result::add);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableList(result);
    }

    /**
     * Bind an attribute or relationship.
     *
     * @param cls           Class type to bind fields
     * @param fieldOrMethod Field or method to bind
     */
    private void bindAttrOrRelation(Class<?> cls, AccessibleObject fieldOrMethod) {
        boolean manyToMany = fieldOrMethod.isAnnotationPresent(ManyToMany.class);
        boolean manyToOne = fieldOrMethod.isAnnotationPresent(ManyToOne.class);
        boolean oneToMany = fieldOrMethod.isAnnotationPresent(OneToMany.class);
        boolean oneToOne = fieldOrMethod.isAnnotationPresent(OneToOne.class);
        boolean isRelation = manyToMany || manyToOne || oneToMany || oneToOne;

        String fieldName = getFieldName(fieldOrMethod);

        if (fieldName == null || fieldName.equals("id")
                || fieldName.equals("class") || OBJ_METHODS.contains(fieldOrMethod)) {
            return; // Reserved. Not attributes.
        }

        Class<?> fieldType = getFieldType(fieldOrMethod);

        ConcurrentLinkedDeque<String> fieldList;
        if (isRelation) {
            fieldList = relationshipsDeque;
            RelationshipType type;
            String mappedBy;
            if (oneToMany) {
                type = RelationshipType.ONE_TO_MANY;
                mappedBy = fieldOrMethod.getAnnotation(OneToMany.class).mappedBy();
            } else if (oneToOne) {
                type = RelationshipType.ONE_TO_ONE;
                mappedBy = fieldOrMethod.getAnnotation(OneToOne.class).mappedBy();
            } else if (manyToMany) {
                type = RelationshipType.MANY_TO_MANY;
                mappedBy = fieldOrMethod.getAnnotation(ManyToMany.class).mappedBy();
            } else if (manyToOne) {
                type = RelationshipType.MANY_TO_ONE;
                mappedBy = "";
            } else {
                type = RelationshipType.NONE;
                mappedBy = "";
            }
            relationshipTypes.put(fieldName, type);
            relationshipToInverse.put(fieldName, mappedBy);
        } else {
            fieldList = attrsDeque;
        }

        fieldList.push(fieldName);
        fieldsToValues.put(fieldName, fieldOrMethod);
        fieldsToTypes.put(fieldName, fieldType);
    }

    /**
     * Returns name of field whether public member or method.
     *
     * @param fieldOrMethod field or method
     * @return field or method name
     */
    private static String getFieldName(AccessibleObject fieldOrMethod) {
        if (fieldOrMethod instanceof Field) {
            return ((Field) fieldOrMethod).getName();
        } else {
            Method method = (Method) fieldOrMethod;
            String name = method.getName();

            if (name.startsWith("get") && method.getParameterCount() == 0) {
                name = WordUtils.uncapitalize(name.substring("get".length()));
            } else if (name.startsWith("is") && method.getParameterCount() == 0) {
                name = WordUtils.uncapitalize(name.substring("is".length()));
            } else {
                return null;
            }
            return name;
        }
    }

    /**
     * Returns type of field whether public member or method.
     *
     * @param fieldOrMethod field or method
     * @return field type
     */
    private static Class<?> getFieldType(AccessibleObject fieldOrMethod) {
        if (fieldOrMethod instanceof Field) {
            return ((Field) fieldOrMethod).getType();
        } else {
            return ((Method) fieldOrMethod).getReturnType();
        }
    }

    private <A extends Annotation> void bindTrigger(Class<A> annotationClass, AccessibleObject fieldOrMethod) {
        if (fieldOrMethod instanceof Method && fieldOrMethod.isAnnotationPresent(annotationClass)) {
            A onTrigger = fieldOrMethod.getAnnotation(annotationClass);
            String value;
            try {
                value = (String) annotationClass.getMethod("value").invoke(onTrigger);
            } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
                value = "";
            }
            fieldsToTriggers.put(Pair.of(annotationClass, value), fieldOrMethod);
        }
    }

    public <A extends Annotation> Collection<Method> getTriggers(Class<A> annotationClass, String fieldName) {
        Collection<Method> methods = fieldsToTriggers.getCollection(Pair.of(annotationClass, fieldName));
        return methods == null ? Collections.emptyList() : methods;
    }
}
