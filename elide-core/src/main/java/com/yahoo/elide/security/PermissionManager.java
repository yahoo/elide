/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.google.common.base.Supplier;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.annotation.UserPermission;
import com.yahoo.elide.audit.InvalidSyntaxException;
import com.yahoo.elide.core.*;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;

import com.yahoo.elide.optimization.UserCheck;
import com.yahoo.elide.security.strategies.AnyField;
import com.yahoo.elide.security.strategies.SpecificField;
import com.yahoo.elide.security.strategies.Strategy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Class responsible for managing the life-cycle and execution of checks.
 */
@Slf4j
public class PermissionManager {
    private final LinkedHashSet<Supplier<Void>> commitChecks = new LinkedHashSet<>();
    private static final SpecificField SPECIFIC_STRATEGY = new SpecificField();
    private static final AnyField ANY_STRATEGY = new AnyField();

    /**
     * Enum describing check combinators.
     */
    public enum CheckMode {
        ANY,
        ALL
    }

    /**
     * Extract a set of permissions from an annotation.
     *
     * @param annotationClass Type of annotation to extract values
     * @param annotation Annotation instance to extract values
     * @param <A> type parameter
     * @return Extracted checks
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> ExtractedChecks extractChecks(Class<A> annotationClass, A annotation) {
        Class<? extends Check>[] anyChecks;
        Class<? extends Check>[] allChecks;
        try {
            anyChecks = (Class<? extends Check>[]) annotationClass
                    .getMethod("any").invoke(annotation, (Object[]) null);
            allChecks = (Class<? extends Check>[]) annotationClass
                    .getMethod("all").invoke(annotation, (Object[]) null);
        } catch (ReflectiveOperationException e) {
            log.warn("Unknown permission: {}, {}", annotationClass.getName(), e);
            throw new InvalidSyntaxException("Unknown permission '" + annotationClass.getName() + "'", e);
        }
        if (anyChecks.length <= 0 && allChecks.length <= 0) {
            log.warn("Unknown permission: {}, {}", annotationClass.getName());
            throw new InvalidSyntaxException("Unknown permission '" + annotationClass.getName() + "'");
        }
        return new ExtractedChecks(anyChecks, allChecks);
    }

    /**
     * Load checks for filter scope.
     *
     * @param userPermission User permission to check
     * @param requestScope Request scope
     * @return Filter scope containing user permission checks.
     */
    public static FilterScope loadChecks(UserPermission userPermission, RequestScope requestScope) {
        if (userPermission == null) {
            return new FilterScope(requestScope);
        }

        Class<? extends UserCheck>[] anyChecks = userPermission.any();
        Class<? extends UserCheck>[] allChecks = userPermission.all();
        Class<? extends Annotation> annotationClass = userPermission.getClass();

        if (anyChecks.length > 0) {
            return new FilterScope(requestScope, CheckMode.ANY, instantiateUserChecks(anyChecks));
        } else if (allChecks.length > 0) {
            return new FilterScope(requestScope, CheckMode.ALL, instantiateUserChecks(allChecks));
        } else {
            log.warn("Unknown user permission '{}'", annotationClass.getName());
            throw new InvalidSyntaxException("Unknown user permission '" + annotationClass.getName() + "'");
        }
    }

    /**
     * Check permission on class.
     *
     * @param annotationClass annotation class
     * @param resource resource
     * @param <A> type parameter
     * @see com.yahoo.elide.annotation.CreatePermission
     * @see com.yahoo.elide.annotation.ReadPermission
     * @see com.yahoo.elide.annotation.UpdatePermission
     * @see com.yahoo.elide.annotation.DeletePermission
     */
    public <A extends Annotation> void checkPermission(Class<A> annotationClass, PersistentResource resource) {
        checkPermission(annotationClass, resource, null);
    }

    /**
     * Check permission on class.
     *
     * @param annotationClass annotation class
     * @param resource resource
     * @param changeSpec ChangeSpec
     * @param <A> type parameter
     * @see com.yahoo.elide.annotation.CreatePermission
     * @see com.yahoo.elide.annotation.ReadPermission
     * @see com.yahoo.elide.annotation.UpdatePermission
     * @see com.yahoo.elide.annotation.DeletePermission
     */
    public <A extends Annotation> void checkPermission(Class<A> annotationClass,
                                                       PersistentResource resource,
                                                       ChangeSpec changeSpec) {
        A annotation = resource.getDictionary().getAnnotation(resource, annotationClass);

        if (annotation == null) {
            return;
        }

        PermissionManager.ExtractedChecks extracted = PermissionManager.extractChecks(annotationClass, annotation);
        CheckMode mode = extracted.getCheckMode();
        Class<OperationCheck>[] opChecks = extracted.getOperationChecks();
        Class<CommitCheck>[] comChecks = extracted.getCommitChecks();

        try {
            runPermissionChecks(opChecks, mode, resource, changeSpec);
        } catch (ForbiddenAccessException e) {
            if (mode == CheckMode.ALL || comChecks.length < 1) {
                log.debug("Forbidden access at entity-level.");
                throw e;
            }
        }

        // If that succeeds, queue up our commit checks
        if (comChecks.length > 0) {
            commitChecks.add(() -> {
                runPermissionChecks(comChecks, mode, resource, changeSpec);
                return null;
            });
        }
    }

    /**
     * Check for permissions on a class and its fields.
     *
     * @param resource resource
     * @param changeSpec change spec
     * @param annotationClass annotation class
     * @param <A> type parameter
     */
    public <A extends Annotation> void checkFieldAwarePermissions(PersistentResource<?> resource,
                                                                  ChangeSpec changeSpec,
                                                                  Class<A> annotationClass) {
        EntityDictionary dictionary = resource.getDictionary();

        // First gather all checks and modes
        Class<? extends Check>[] classOpChecks = null;
        Class<? extends Check>[] classComChecks = null;
        final ArrayList<Class<? extends Check>[]> fieldOpChecks = new ArrayList<>();
        final ArrayList<Class<? extends Check>[]> fieldComChecks = new ArrayList<>();
        CheckMode classCheckMode = null;
        final ArrayList<CheckMode> fieldCheckModes = new ArrayList<>();

        A annotation = dictionary.getAnnotation(resource, annotationClass);
        if (annotation != null) {
            ExtractedChecks extracted = extractChecks(annotationClass, annotation);
            classOpChecks = extracted.getOperationChecks();
            classComChecks = extracted.getCommitChecks();
            classCheckMode = extracted.getCheckMode();
        }

        // Get all fields
        Class<?> entityClass = resource.getResourceClass();
        List<String> attributes = dictionary.getAttributes(entityClass);
        List<String> relationships = dictionary.getRelationships(entityClass);
        List<String> fields = (attributes != null) ? new ArrayList<>(dictionary.getAttributes(entityClass))
                                                   : new ArrayList<>();
        if (relationships != null) {
            fields.addAll(relationships);
        }

        for (String field : fields) {
            annotation = dictionary.getAttributeOrRelationAnnotation(entityClass, annotationClass, field);
            if (annotation == null) {
                continue;
            }
            ExtractedChecks extracted = extractChecks(annotationClass, annotation);
            fieldOpChecks.add(extracted.getOperationChecks());
            fieldComChecks.add(extracted.getCommitChecks());
            fieldCheckModes.add(extracted.getCheckMode());
        }

        boolean hasDeferredChecks = (!isEmptyCheckArray(classComChecks) || !isEmptyListOfChecks(fieldComChecks))
                && (annotationClass.equals(UpdatePermission.class) || annotationClass.equals(CreatePermission.class));

        // Run operation checks
        fieldAwareExecute(classOpChecks, fieldOpChecks, classCheckMode, fieldCheckModes, resource, changeSpec,
                hasDeferredChecks, ANY_STRATEGY);

        // Need these to be final so we can capture within lambda
        final Class<? extends Check>[] captureClassComChecks = classComChecks;
        final CheckMode captureClassCheckMode = classCheckMode;

        // If that succeeds, queue up the commit checks
        if (hasDeferredChecks) {
            commitChecks.add(() -> {
                fieldAwareExecute(captureClassComChecks, fieldComChecks, captureClassCheckMode,
                        fieldCheckModes, resource, changeSpec, false, ANY_STRATEGY);
                return null;
            });
        }
    }

    /**
     * Check for permissions on a specific field.
     *
     * @param resource resource
     * @param changeSpec changepsec
     * @param annotationClass annotation class
     * @param field field to check
     * @param <A> type parameter
     */
    public <A extends Annotation> void checkFieldAwarePermissions(PersistentResource<?> resource,
                                                                  ChangeSpec changeSpec,
                                                                  Class<A> annotationClass,
                                                                  String field) {
        EntityDictionary dictionary = resource.getDictionary();

        // Gather important bits
        Class<? extends Check>[] classOpChecks = null;
        Class<? extends Check>[] classComChecks = null;
        Class<? extends Check>[] fieldOpChecks = null;
        Class<? extends Check>[] fieldComChecks = null;
        CheckMode classCheckMode = null;
        CheckMode fieldCheckMode = null;

        A annotation = dictionary.getAnnotation(resource, annotationClass);
        if (annotation != null) {
            ExtractedChecks extracted = extractChecks(annotationClass, annotation);
            classOpChecks = extracted.getOperationChecks();
            classComChecks = extracted.getCommitChecks();
            classCheckMode = extracted.getCheckMode();
        }

        annotation = dictionary.getAttributeOrRelationAnnotation(resource.getResourceClass(), annotationClass, field);
        if (annotation != null) {
            ExtractedChecks extracted = extractChecks(annotationClass, annotation);
            fieldOpChecks = extracted.getOperationChecks();
            fieldComChecks = extracted.getCommitChecks();
            fieldCheckMode = extracted.getCheckMode();
        }

        // Run checks
        List<Class<? extends Check>[]> fieldOpList = new ArrayList();
        fieldOpList.add(fieldOpChecks);
        fieldAwareExecute(classOpChecks,
                fieldOpList,
                classCheckMode,
                Arrays.asList(fieldCheckMode),
                resource, changeSpec,
                (fieldComChecks != null && fieldComChecks.length > 0),
                SPECIFIC_STRATEGY);

        // Capture these as final for lambda
        final Class<? extends Check>[] capClassComChecks = classComChecks;
        final Class<? extends Check>[] capFieldComChecks = fieldComChecks;
        final CheckMode capClassCheckMode = classCheckMode;
        final CheckMode capFieldCheckMode = fieldCheckMode;

        // Queue up on success
        if ((capFieldComChecks != null && capFieldComChecks.length > 0)
                || (capClassComChecks != null && capClassComChecks.length > 0)) {
            final List<Class<? extends Check>[]> fieldComList = new ArrayList();
            fieldComList.add(fieldComChecks);
            commitChecks.add(() -> {
                fieldAwareExecute(capClassComChecks,
                        fieldComList,
                        capClassCheckMode,
                        Arrays.asList(capFieldCheckMode),
                        resource,
                        changeSpec,
                        false,
                        SPECIFIC_STRATEGY);
                return null;
            });
        }
    }

    /**
     * Execute commmit checks.
     */
    public void executeCommitChecks() {
        commitChecks.forEach(Supplier::get);
    }

    private void runPermissionChecks(Class<? extends Check>[] checks,
                                     CheckMode checkMode,
                                     PersistentResource<?> resource,
                                     ChangeSpec changeSpec) {
        if (resource.getRequestScope().getSecurityMode() == SecurityMode.BYPASS_SECURITY) {
            return;
        }

        for (Class<? extends Check> check : checks) {
            Check handler;
            try {
                handler = check.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                log.debug("Illegal permission check '{}' {}", check.getName(), e);
                throw new InvalidSyntaxException("Illegal permission check '" + check.getName() + "'", e);
            }

            boolean ok = handler.ok(resource.getObject(), resource.getRequestScope(), Optional.ofNullable(changeSpec));

            if (ok && checkMode == CheckMode.ANY) {
                return;
            }

            if (!ok && checkMode == CheckMode.ALL) {
                log.debug("ForbiddenAccess {} {}#{}", check, resource.getType(), resource.getId());
                throw new ForbiddenAccessException();
            }
        }

        if (checkMode == CheckMode.ANY) {
            log.debug("ForbiddenAccess {} {}#{}", Arrays.asList(checks), resource.getType(), resource.getId());
            throw new ForbiddenAccessException();
        }
    }

    /**
     * Check object and all fields for field- and class-level checks.
     *
     * @param classChecks
     * @param fieldChecks
     * @param resource
     * @param changeSpec
     * @return True if done (i.e. no commit checks), false otherwise. Throws a ForbiddenAccessException upon failure.
     */
    private void fieldAwareExecute(Class<? extends Check>[] classChecks,
                                   List<Class<? extends Check>[]> fieldChecks,
                                   CheckMode classMode,
                                   List<CheckMode> fieldModes,
                                   PersistentResource<?> resource,
                                   ChangeSpec changeSpec,
                                   boolean hasDeferredChecks,
                                   Strategy strategy) {
        boolean hasPassingCheck = !isEmptyCheckArray(classChecks)
                || (isEmptyCheckArray(classChecks) && isEmptyListOfChecks(fieldChecks));
        boolean entityFailed = false;

        // Check full object, then all fields
        if (!isEmptyCheckArray(classChecks)) {
            try {
                runPermissionChecks(classChecks, classMode, resource, changeSpec);
                // Check if we can short-circuit the rest of the checks
                if (!strategy.shouldContinueUponEntitySuccess(classMode)) {
                    return;
                }
            } catch (ForbiddenAccessException e) {
                // Ignore this and continue on to checking our fields
                hasPassingCheck = false;
                entityFailed = true;
            }
        }

        // Check fields
        boolean hasFieldChecks = !isEmptyListOfChecks(fieldChecks);
        boolean modeCheckCardinalityMatch = fieldChecks.size() == fieldModes.size();
        if (hasFieldChecks && modeCheckCardinalityMatch) {
            for (int i = 0 ; i < fieldChecks.size() ; ++i) {
                CheckMode mode = fieldModes.get(i);
                try {
                    runPermissionChecks(fieldChecks.get(i), mode, resource, changeSpec);
                    if (mode == CheckMode.ANY) {
                        return;
                    }
                    hasPassingCheck = true;
                } catch (ForbiddenAccessException e) {
                    if (!strategy.shouldContinueUponFieldFailure(mode, hasDeferredChecks)) {
                        log.debug("Failed specific field and not continuing.");
                        throw e;
                    }
                }
            }
        } else if (!modeCheckCardinalityMatch) {
            // NOTE: This should never happen, but unfortunately bugs do occur. Err on the side of caution.
            log.error("Something went wrong internally! The cardinality of fields and check mode arrays differ.");
            throw new ForbiddenAccessException();
        }

        strategy.run(hasPassingCheck, hasDeferredChecks, hasFieldChecks, entityFailed);
    }

    /**
     * Check whether or not an array of checks is empty.
     *
     * @param checks Array of checks
     * @return True if empty, false otherwise.
     */
    private static boolean isEmptyCheckArray(Class<? extends Check>[] checks) {
        return checks == null || checks.length < 1;
    }

    /**
     * Check if a list containing an array of checks is empty.
     *
     * @param checks List of check arrays
     * @return True if empty, false otherwise.
     */
    private static boolean isEmptyListOfChecks(List<Class<? extends Check>[]> checks) {
        if (checks == null || checks.isEmpty()) {
            return true;
        }
        for (Class<? extends Check>[] checkArray : checks) {
            if (!isEmptyCheckArray(checkArray)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Instantiate a list of UserCheck's.
     *
     * @param userCheckClasses Array of classes to instantiate
     * @return List of ordered, instantiated UserCheck's
     */
    private static List<UserCheck> instantiateUserChecks(Class<? extends UserCheck>[] userCheckClasses) {
        List<UserCheck> userChecks = new ArrayList<>(userCheckClasses.length);
        for (Class<? extends UserCheck> checkClass : userCheckClasses) {
            try {
                userChecks.add(checkClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("Could not instantiate UserCheck: {}", checkClass.getName());
                throw new IllegalStateException("Failed to instantiate UserCheck.");
            }
        }
        return userChecks;
    }

    /**
     * Extracted checks.
     */
    @AllArgsConstructor
    public static final class ExtractedChecks {
        private final Class<? extends Check>[] anyChecks;
        private final Class<? extends Check>[] allChecks;

        @SuppressWarnings("unchecked")
        public Class<? extends Check>[] getCompleteSetOfChecks() {
            return (anyChecks.length > 0) ? anyChecks : allChecks;
        }

        public CheckMode getCheckMode() {
            return (anyChecks.length > 0) ? CheckMode.ANY : CheckMode.ALL;
        }

        public Class<CommitCheck>[] getCommitChecks() {
            return getArray(CommitCheck.class);
        }

        public Class<OperationCheck>[] getOperationChecks() {
            return getArray(OperationCheck.class);
        }

        @SuppressWarnings("unchecked")
        private <T extends Check> Class<T>[] getArray(Class<T> cls) {
            Class<? extends Check>[] checks = getCompleteSetOfChecks();
            ArrayList<Class<T>> checksList = new ArrayList<>();
            for (Class<? extends Check> check : checks) {
                if (cls.isAssignableFrom(check)) {
                    checksList.add((Class<T>) check);
                }
            }
            return checksList.toArray(new Class[checksList.size()]);
        }
    }
}
