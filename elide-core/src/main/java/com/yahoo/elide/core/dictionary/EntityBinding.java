/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.dictionary;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.core.dictionary.EntityDictionary.REGULAR_ID_NAME;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.ComputedRelationship;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.annotation.ToMany;
import com.yahoo.elide.annotation.ToOne;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.type.Type;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Entity Dictionary maps JSON API Entity beans to/from Entity type names.
 *
 * @see com.yahoo.elide.annotation.Include#type
 */
public class EntityBinding {

    private static final List<Method> OBJ_METHODS = ImmutableList.copyOf(Object.class.getMethods());
    private static final List<Class<? extends Annotation>> RELATIONSHIP_TYPES =
            Arrays.asList(ManyToMany.class, ManyToOne.class, OneToMany.class, OneToOne.class,
                    ToOne.class, ToMany.class);

    @Getter
    public final Type<?> entityClass;
    public final String jsonApiType;
    @Getter
    public boolean idGenerated;
    @Getter
    private AccessibleObject idField;
    @Getter
    private String idFieldName;
    @Getter
    private Type<?> idType;
    @Getter
    private AccessType accessType;

    @Getter
    private final boolean injected;

    private EntityDictionary dictionary;

    @Getter
    private String apiVersion;

    public final EntityPermissions entityPermissions;
    public final List<String> apiAttributes;
    public final List<String> apiRelationships;
    public final List<Type<?>> inheritedTypes;
    public final ConcurrentLinkedDeque<String> attributesDeque = new ConcurrentLinkedDeque<>();
    public final ConcurrentLinkedDeque<String> relationshipsDeque = new ConcurrentLinkedDeque<>();

    public final ConcurrentHashMap<String, RelationshipType> relationshipTypes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> relationshipToInverse = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, CascadeType[]> relationshipToCascadeTypes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, AccessibleObject> fieldsToValues = new ConcurrentHashMap<>();
    public final MultiValuedMap<Triple<String, LifeCycleHookBinding.Operation, LifeCycleHookBinding.TransactionPhase>,
            LifeCycleHook> fieldTriggers = new HashSetValuedHashMap<>();
    public final MultiValuedMap<Pair<LifeCycleHookBinding.Operation, LifeCycleHookBinding.TransactionPhase>,
            LifeCycleHook> classTriggers = new HashSetValuedHashMap<>();
    public final ConcurrentHashMap<String, Type<?>> fieldsToTypes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> aliasesToFields = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Method, Boolean> requestScopeableMethods = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<AccessibleObject, Set<ArgumentType>> attributeArguments = new ConcurrentHashMap<>();

    public final ConcurrentHashMap<Object, Annotation> annotations = new ConcurrentHashMap<>();

    public static final EntityBinding EMPTY_BINDING = new EntityBinding();
    public static final Set<ArgumentType> EMPTY_ATTRIBUTES_ARGS = Collections.unmodifiableSet(new HashSet<>());
    private static final String ALL_FIELDS = "*";

    /* empty binding constructor */
    private EntityBinding() {
        injected = false;
        jsonApiType = null;
        apiVersion = NO_VERSION;
        apiAttributes = new ArrayList<>();
        apiRelationships = new ArrayList<>();
        inheritedTypes = new ArrayList<>();
        idField = null;
        idType = null;
        entityClass = null;
        entityPermissions = EntityPermissions.EMPTY_PERMISSIONS;
        idGenerated = false;
        dictionary = null;
    }

    /**
     * Constructor
     *
     * @param dictionary Dictionary to use
     * @param cls Entity class
     * @param type Declared Elide type name
     */
    public EntityBinding(EntityDictionary dictionary,
                         Type<?> cls,
                         String type) {
        this(dictionary, cls, type, NO_VERSION, new HashSet<>());
    }

    /**
     * Constructor
     *
     * @param dictionary Dictionary to use
     * @param cls Entity class
     * @param type Declared Elide type name
     * @param hiddenAnnotations Annotations for hiding a field in API
     */
    public EntityBinding(EntityDictionary dictionary,
                         Type<?> cls,
                         String type,
                         String apiVersion,
                         Set<Class<? extends Annotation>> hiddenAnnotations) {
        this.dictionary = dictionary;
        entityClass = cls;
        jsonApiType = type;
        this.apiVersion = apiVersion;
        inheritedTypes = getInheritedTypes(cls);

        // Map id's, attributes, and relationships
        List<AccessibleObject> fieldOrMethodList = getAllFields();
        injected = shouldInject();

        if (fieldOrMethodList.stream().anyMatch(field -> field.isAnnotationPresent(Id.class))) {
            accessType = AccessType.FIELD;

            /* Add all public methods that are computed OR life cycle hooks */
            fieldOrMethodList.addAll(
                    getInstanceMembers(cls.getMethods(),
                            (method) -> method.isAnnotationPresent(LifeCycleHookBinding.class)
                            || method.isAnnotationPresent(ComputedAttribute.class)
                            || method.isAnnotationPresent(ComputedRelationship.class)
                    )
            );

            //Elide needs to manipulate private fields that are exposed.
            fieldOrMethodList.forEach(field -> field.setAccessible(true));
        } else {
            /* Preserve the behavior of Elide 4.2.6 and earlier */
            accessType = AccessType.PROPERTY;

            fieldOrMethodList.clear();

            /* Add all public fields */
            fieldOrMethodList.addAll(getInstanceMembers(cls.getFields()));

            /* Add all public methods */
            fieldOrMethodList.addAll(getInstanceMembers(cls.getMethods()));
        }

        bindEntityFields(cls, type, fieldOrMethodList, hiddenAnnotations);
        bindTriggerIfPresent();

        apiAttributes = dequeToList(attributesDeque);
        apiRelationships = dequeToList(relationshipsDeque);
        entityPermissions = new EntityPermissions(dictionary, cls, fieldOrMethodList);
    }

    /**
     * Filters a list of class Members to instance methods & fields.
     *
     * @param objects
     * @param <T>
     * @return A list of the filtered members
     */
    private <T extends Member> List<T> getInstanceMembers(T[] objects) {
        return getInstanceMembers(objects, o -> true);
    }

    /**
     * Filters a list of class Members to instance methods & fields.
     *
     * @param objects    The list of Members to filter
     * @param <T>        Concrete Member Type
     * @param filteredBy An additional filter predicate to apply
     * @return A list of the filtered members
     */
    private <T extends Member> List<T> getInstanceMembers(T[] objects, Predicate<T> filteredBy) {
        return Arrays.stream(objects)
                .filter(o -> !Modifier.isStatic(o.getModifiers()))
                .filter(filteredBy)
                .collect(Collectors.toList());
    }

    /**
     * Get all fields of the entity class, including fields of superclasses (excluding Object).
     * @return All fields of the EntityBindings entity class and all superclasses (excluding Object)
     */
    public List<AccessibleObject> getAllFields() {
        List<AccessibleObject> fields = new ArrayList<>();

        fields.addAll(getInstanceMembers(entityClass.getDeclaredFields(), (field) -> !field.isSynthetic()));
        for (Type<?> type : inheritedTypes) {
            fields.addAll(getInstanceMembers(type.getDeclaredFields(), (field) -> !field.isSynthetic()));
        }

        return fields;
    }

    private List<AccessibleObject> getAllMethods() {
        List<AccessibleObject> methods = new ArrayList<>();

        methods.addAll(getInstanceMembers(entityClass.getDeclaredMethods(), (method) -> !method.isSynthetic()));
        for (Type<?> type : inheritedTypes) {
            methods.addAll(getInstanceMembers(type.getDeclaredMethods(), (method) -> !method.isSynthetic()));
        }

        return methods;
    }

    /**
     * Bind fields of an entity including the Id field, attributes, and relationships.
     *
     * @param cls               Class type to bind fields
     * @param type              JSON API type identifier
     * @param fieldOrMethodList List of fields and methods on entity
     * @param hiddenAnnotations Annotations for hiding a field in API
     */
    private void bindEntityFields(Type<?> cls, String type,
                                  Collection<AccessibleObject> fieldOrMethodList,
                                  Set<Class<? extends Annotation>> hiddenAnnotations) {
        for (AccessibleObject fieldOrMethod : fieldOrMethodList) {
            bindTriggerIfPresent(fieldOrMethod);

            if (fieldOrMethod.isAnnotationPresent(Id.class)) {
                bindEntityId(cls, type, fieldOrMethod);
            } else if (fieldOrMethod.isAnnotationPresent(Transient.class)
                    && !fieldOrMethod.isAnnotationPresent(ComputedAttribute.class)
                    && !fieldOrMethod.isAnnotationPresent(ComputedRelationship.class)) {
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
                bindAttrOrRelation(
                        fieldOrMethod,
                        hiddenAnnotations.stream().anyMatch(fieldOrMethod::isAnnotationPresent));
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
    private void bindEntityId(Type<?> cls, String type, AccessibleObject fieldOrMethod) {
        String fieldName = getFieldName(fieldOrMethod);
        Type<?> fieldType = getFieldType(cls, fieldOrMethod);

        //Add id field to type map for the entity
        fieldsToTypes.put(fieldName, fieldType);

        //Set id field, type, and name
        idField = fieldOrMethod;
        idType = fieldType;
        idFieldName = fieldName;

        fieldsToValues.put(fieldName, fieldOrMethod);

        if (idField != null && !fieldOrMethod.equals(idField)) {
            throw new DuplicateMappingException(type + " " + cls.getName() + ":" + fieldName);
        }
        if (fieldOrMethod.isAnnotationPresent(GeneratedValue.class)) {
            idGenerated = true;
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
     * @param fieldOrMethod Field or method to bind
     * @param isHidden Whether this field is hidden from API
     */
    private void bindAttrOrRelation(AccessibleObject fieldOrMethod, boolean isHidden) {
        boolean isRelation = RELATIONSHIP_TYPES.stream().anyMatch(fieldOrMethod::isAnnotationPresent);

        String fieldName = getFieldName(fieldOrMethod);
        Type<?> fieldType = getFieldType(entityClass, fieldOrMethod);

        if (fieldName == null || REGULAR_ID_NAME.equals(fieldName) || "class".equals(fieldName)
                || OBJ_METHODS.contains(fieldOrMethod)) {
            return; // Reserved
        }

        if (fieldOrMethod instanceof Method) {
            Method method = (Method) fieldOrMethod;
            requestScopeableMethods.put(method, isRequestScopeableMethod(method));
        }

        if (isRelation) {
            bindRelation(fieldOrMethod, fieldName, fieldType, isHidden);
        } else {
            bindAttr(fieldOrMethod, fieldName, fieldType, isHidden);
        }
    }

    /**
     * Bind a relationship to current class
     *
     * @param fieldOrMethod Field or method to bind
     * @param fieldName Field name
     * @param fieldType Field type
     * @param isHidden Whether this field is hidden from API
     */
    private void bindRelation(AccessibleObject fieldOrMethod, String fieldName, Type<?> fieldType, boolean isHidden) {
        boolean manyToMany = fieldOrMethod.isAnnotationPresent(ManyToMany.class);
        boolean manyToOne = fieldOrMethod.isAnnotationPresent(ManyToOne.class);
        boolean oneToMany = fieldOrMethod.isAnnotationPresent(OneToMany.class);
        boolean oneToOne = fieldOrMethod.isAnnotationPresent(OneToOne.class);
        boolean toOne = fieldOrMethod.isAnnotationPresent(ToOne.class);
        boolean toMany = fieldOrMethod.isAnnotationPresent(ToMany.class);
        boolean computedRelationship = fieldOrMethod.isAnnotationPresent(ComputedRelationship.class);

        if (fieldOrMethod.isAnnotationPresent(MapsId.class)) {
            idGenerated = true;
        }

        RelationshipType type;
        String mappedBy = "";
        CascadeType[] cascadeTypes = new CascadeType[0];
        if (oneToMany) {
            type = computedRelationship ? RelationshipType.COMPUTED_ONE_TO_MANY : RelationshipType.ONE_TO_MANY;
            mappedBy = fieldOrMethod.getAnnotation(OneToMany.class).mappedBy();
            cascadeTypes = fieldOrMethod.getAnnotation(OneToMany.class).cascade();
        } else if (oneToOne) {
            type = computedRelationship ? RelationshipType.COMPUTED_ONE_TO_ONE : RelationshipType.ONE_TO_ONE;
            mappedBy = fieldOrMethod.getAnnotation(OneToOne.class).mappedBy();
            cascadeTypes = fieldOrMethod.getAnnotation(OneToOne.class).cascade();
        } else if (manyToMany) {
            type = computedRelationship ? RelationshipType.COMPUTED_MANY_TO_MANY : RelationshipType.MANY_TO_MANY;
            mappedBy = fieldOrMethod.getAnnotation(ManyToMany.class).mappedBy();
            cascadeTypes = fieldOrMethod.getAnnotation(ManyToMany.class).cascade();
        } else if (manyToOne) {
            type = computedRelationship ? RelationshipType.COMPUTED_MANY_TO_ONE : RelationshipType.MANY_TO_ONE;
            cascadeTypes = fieldOrMethod.getAnnotation(ManyToOne.class).cascade();
        } else if (toOne) {
            type = RelationshipType.COMPUTED_ONE_TO_ONE;
        } else if (toMany) {
            type = RelationshipType.COMPUTED_ONE_TO_MANY;
        } else {
            type = computedRelationship ? RelationshipType.COMPUTED_NONE : RelationshipType.NONE;
        }
        relationshipTypes.put(fieldName, type);
        relationshipToInverse.put(fieldName, mappedBy);
        relationshipToCascadeTypes.put(fieldName, cascadeTypes);

        if (!isHidden) {
            relationshipsDeque.push(fieldName);
        }
        fieldsToValues.put(fieldName, fieldOrMethod);
        fieldsToTypes.put(fieldName, fieldType);
    }

    /**
     * Bind an attribute to current class
     *
     * @param fieldOrMethod Field or method to bind
     * @param fieldName Field name
     * @param fieldType Field type
     * @param isHidden Whether this field is hidden from API
     */
    private void bindAttr(AccessibleObject fieldOrMethod, String fieldName, Type<?> fieldType, boolean isHidden) {
        if (!isHidden) {
            attributesDeque.push(fieldName);
        }
        fieldsToValues.put(fieldName, fieldOrMethod);
        fieldsToTypes.put(fieldName, fieldType);
    }

    /**
     * Returns name of field whether public member or method.
     *
     * @param fieldOrMethod field or method
     * @return field or method name
     */
    public static String getFieldName(AccessibleObject fieldOrMethod) {
        if (fieldOrMethod instanceof Field) {
            return ((Field) fieldOrMethod).getName();
        }
        Method method = (Method) fieldOrMethod;
        String name = method.getName();
        boolean hasValidParameterCount = method.getParameterCount() == 0 || isRequestScopeableMethod(method);

        if (name.startsWith("get") && hasValidParameterCount) {
            return StringUtils.uncapitalize(name.substring("get".length()));
        }
        if (name.startsWith("is") && hasValidParameterCount) {
            return StringUtils.uncapitalize(name.substring("is".length()));
        }
        return null;
    }

    /**
     * Check whether or not method expects a RequestScope.
     *
     * @param method Method to check
     * @return True if accepts a RequestScope, false otherwise
     */
    public static boolean isRequestScopeableMethod(Method method) {
        return isComputedMethod(method) && method.getParameterCount() == 1
                && com.yahoo.elide.core.security.RequestScope.class.isAssignableFrom(method.getParameterTypes()[0]);
    }

    /**
     * Check whether or not the provided method described a computed attribute or relationship.
     *
     * @param method Method to check
     * @return True if method is a computed type, false otherwise
     */
    public static boolean isComputedMethod(Method method) {
        return Stream.of(method.getAnnotations())
                .map(Annotation::annotationType)
                .anyMatch(c -> ComputedAttribute.class == c || ComputedRelationship.class == c);
    }

    /**
     * Returns type of field whether member or method.
     *
     * @param parentClass The class which owns the given field or method
     * @param fieldOrMethod field or method
     * @return field type
     */
    public static Type<?> getFieldType(Type<?> parentClass,
                                         AccessibleObject fieldOrMethod) {
        return getFieldType(parentClass, fieldOrMethod, Optional.empty());
    }

    /**
     * Returns type of field whether member or method.
     *
     * @param parentClass The class which owns the given field or method
     * @param fieldOrMethod field or method
     * @param index Optional parameter index for parameterized types that take one or more parameters.  If
     *              an index is provided, the type returned is the parameter type.  Otherwise it is the
     *              parameterized type.
     * @return field type
     */
    public static Type<?> getFieldType(Type<?> parentClass,
                                         AccessibleObject fieldOrMethod,
                                         Optional<Integer> index) {
        Type type;
        if (fieldOrMethod instanceof Field) {
            type = ((Field) fieldOrMethod).getGenericType();
        } else {
            type = ((Method) fieldOrMethod).getGenericReturnType();
        }

        if (type instanceof ParameterizedType && index.isPresent()) {
            type = ((ParameterizedType) type).getActualTypeArguments()[index.get().intValue()];
        }

        return TypeUtils.getRawType(type, parentClass);
    }

    private void bindTriggerIfPresent(AccessibleObject fieldOrMethod) {
        LifeCycleHookBinding[] triggers = fieldOrMethod.getAnnotationsByType(LifeCycleHookBinding.class);
        for (LifeCycleHookBinding trigger : triggers) {
            bindTrigger(trigger, getFieldName(fieldOrMethod));
        }
    }
    private void bindTriggerIfPresent() {
        LifeCycleHookBinding[] triggers = entityClass.getAnnotationsByType(LifeCycleHookBinding.class);
        for (LifeCycleHookBinding trigger : triggers) {
            bindTrigger(trigger);
        }
    }

    public void bindTrigger(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            String fieldOrMethodName,
                            LifeCycleHook hook) {
        Triple<String, LifeCycleHookBinding.Operation, LifeCycleHookBinding.TransactionPhase> key =
                Triple.of(fieldOrMethodName, operation, phase);

        fieldTriggers.put(key, hook);
    }

    private void bindTrigger(LifeCycleHookBinding binding,
                            String fieldOrMethodName) {
        Injector injector = dictionary.getInjector();
        LifeCycleHook hook = injector.instantiate(binding.hook());
        injector.inject(hook);
        bindTrigger(binding.operation(), binding.phase(), fieldOrMethodName, hook);
    }

    public void bindTrigger(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            LifeCycleHook hook) {
        Pair<LifeCycleHookBinding.Operation, LifeCycleHookBinding.TransactionPhase> key =
                Pair.of(operation, phase);

        classTriggers.put(key, hook);
    }

    private void bindTrigger(LifeCycleHookBinding binding) {
        if (binding.oncePerRequest()) {
            bindTrigger(binding, PersistentResource.CLASS_NO_FIELD);
            return;
        }

        Injector injector = dictionary.getInjector();
        LifeCycleHook hook = dictionary.getInjector().instantiate(binding.hook());
        injector.inject(hook);
        bindTrigger(binding.operation(), binding.phase(), hook);
    }

    public <A extends Annotation> Collection<LifeCycleHook> getTriggers(LifeCycleHookBinding.Operation op,
                                                                        LifeCycleHookBinding.TransactionPhase phase,
                                                                        String fieldName) {
        Triple<String, LifeCycleHookBinding.Operation, LifeCycleHookBinding.TransactionPhase> key =
                Triple.of(fieldName, op, phase);
        Collection<LifeCycleHook> bindings = fieldTriggers.get(key);
        return (bindings == null ? Collections.emptyList() : bindings);
    }

    public <A extends Annotation> Collection<LifeCycleHook> getTriggers(LifeCycleHookBinding.Operation op,
                                                                        LifeCycleHookBinding.TransactionPhase phase) {

        Pair<LifeCycleHookBinding.Operation, LifeCycleHookBinding.TransactionPhase> key =
                Pair.of(op, phase);
        Collection<LifeCycleHook> bindings = classTriggers.get(key);
        return (bindings == null ? Collections.emptyList() : bindings);
    }

    /**
     * Cache placeholder for no annotation.
     */
    private static final Annotation NO_ANNOTATION = new Annotation() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    };

    /**
     * Return annotation from class, parents or package.
     *
     * @param annotationClass the annotation class
     * @param <A>             annotation type
     * @return the annotation
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        Annotation annotation = annotations.computeIfAbsent(annotationClass, cls -> Optional.ofNullable(
                EntityDictionary.getFirstAnnotation(entityClass, Collections.singletonList(annotationClass)))
                .orElse(NO_ANNOTATION));
        return annotation == NO_ANNOTATION ? null : annotationClass.cast(annotation);
    }

    /**
     * Return annotation for provided method.
     *
     * @param annotationClass the annotation class
     * @param method the method
     * @param <A> annotation type
     * @return the annotation
     */
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotationClass, String method) {
        Annotation annotation = annotations.computeIfAbsent(Pair.of(annotationClass, method), key -> {
            try {
                return Optional.ofNullable((Annotation) entityClass.getMethod(method).getAnnotation(annotationClass))
                        .orElse(NO_ANNOTATION);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException(e);
            }
        });
        return annotation == NO_ANNOTATION ? null : annotationClass.cast(annotation);
    }

    private boolean shouldInject() {
        boolean hasField = getAllFields().stream()
                .anyMatch(accessibleObject -> accessibleObject.isAnnotationPresent(Inject.class));

        if (hasField) {
            return true;
        }

        boolean hasMethod = getAllMethods().stream()
                .anyMatch(accessibleObject -> accessibleObject.isAnnotationPresent(Inject.class));

        if (hasMethod) {
            return true;
        }

        boolean hasConstructor = Arrays.stream(entityClass.getConstructors())
                .anyMatch(ctor -> ctor.getAnnotation(Inject.class) != null);

        return hasConstructor;
    }

    private List<Type<?>> getInheritedTypes(Type<?> entityCls) {
        ArrayList<Type<?>> results = new ArrayList<>();

        for (Type<?> cls = entityCls.getSuperclass(); null != cls && Object.class != cls; cls = cls.getSuperclass()) {
            results.add(cls);
        }

        return results;
    }


    /**
     * Add a collection of arguments to the attributes of this Entity.
     * @param attribute attribute name to which argument has to be added
     * @param arguments Set of Argument Type for the attribute
     */
    public void addArgumentsToAttribute(String attribute, Set<ArgumentType> arguments) {
        AccessibleObject fieldObject = fieldsToValues.get(attribute);
        if (fieldObject != null && arguments != null) {
            Set<ArgumentType> existingArgs = attributeArguments.get(fieldObject);
            if (existingArgs != null) {
                //Replace any argument names with new value
                existingArgs.addAll(arguments);
            } else {
                attributeArguments.put(fieldObject, new HashSet<>(arguments));
            }
        }
    }

    /**
     * Returns the Collection of all attributes of an argument.
     * @param attribute Name of the argument for ehich arguments are to be retrieved.
     * @return A Set of ArgumentType for the given attribute.
     */
    public Set<ArgumentType> getAttributeArguments(String attribute) {
        AccessibleObject fieldObject = fieldsToValues.get(attribute);
        return (fieldObject != null)
                ? attributeArguments.getOrDefault(fieldObject, EMPTY_ATTRIBUTES_ARGS)
                : EMPTY_ATTRIBUTES_ARGS;
    }
}
