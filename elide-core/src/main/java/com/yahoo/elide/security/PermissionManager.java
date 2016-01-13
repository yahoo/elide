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
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.FilterScope;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.SecurityMode;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;

import com.yahoo.elide.optimization.UserCheck;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Class responsible for managing the life-cycle and execution of checks.
 */
@Slf4j
public class PermissionManager {
    private final LinkedHashSet<Supplier<Void>> commitChecks = new LinkedHashSet<>();
    private final HashMap<Class<? extends UserCheck>, Boolean> userCheckCache = new HashMap<>();
    private final HashMap<CheckIdentifier, Boolean> resourceCache = new HashMap<>();

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

        boolean isUpdate = UpdatePermission.class.isAssignableFrom(annotationClass);

        try {
            runPermissionChecks(opChecks, mode, resource, changeSpec, isUpdate);
        } catch (ForbiddenAccessException e) {
            if (mode == CheckMode.ALL || comChecks.length < 1) {
                log.debug("Forbidden access at entity-level.");
                throw e;
            }
        }

        // If that succeeds, queue up our commit checks
        if (!isEmptyCheckArray(comChecks)) {
            commitChecks.add(() -> {
                runPermissionChecks(comChecks, mode, resource, changeSpec, isUpdate);
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
        checkFieldAwarePermissions(resource, changeSpec, annotationClass, null);
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
        // Select strategy
        final Strategy strategy = (field == null)
                                  ? new AnyFieldStrategy(resource, annotationClass, changeSpec)
                                  : new SpecificFieldStrategy(resource, field, annotationClass, changeSpec);

        // Run checks
        strategy.executeOperationChecks();

        // Queue up on success
        if (strategy.hasCommitChecks()) {
            commitChecks.add(() -> {
                strategy.executeCommitChecks();
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
                                     ChangeSpec changeSpec,
                                     boolean isUpdate) {
        if (resource.getRequestScope().getSecurityMode() == SecurityMode.BYPASS_SECURITY) {
            return;
        }

        for (Class<? extends Check> check : checks) {
            CheckIdentifier checkId = new CheckIdentifier(resource, check);

            boolean ok = (isUpdate) ? computeCheck(check, resource, changeSpec)
                : resourceCache.computeIfAbsent(checkId, (id) -> computeCheck(check, resource, changeSpec));

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
     * Compute the value of a check.
     *
     * @param check Check to compute
     * @param resource Resource to compute check for
     * @param changeSpec Change spec
     * @return True if check successful, false otherwise.
     */
    private boolean computeCheck(Class<? extends Check> check, PersistentResource resource, ChangeSpec changeSpec) {
        Check handler;
        try {
            handler = check.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.debug("Illegal permission check '{}' {}", check.getName(), e);
            throw new InvalidSyntaxException("Illegal permission check '" + check.getName() + "'", e);
        }
        return handler.ok(resource.getObject(), resource.getRequestScope(), Optional.ofNullable(changeSpec));
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

    private static <A extends Annotation> boolean containsCommitChecks(Class<? extends Check>[] classChecks,
                                                                       List<Class<? extends Check>[]> fieldChecks,
                                                                       Class<A> annotationClass) {
        for (Class<? extends Check>[] checks : fieldChecks) {
            if (containsCommitChecks(classChecks, checks, annotationClass)) {
                return true;
            }
        }
        return false;
    }

    private static <A extends Annotation> boolean containsCommitChecks(Class<? extends Check>[] classChecks,
                                                                       Class<? extends Check>[] fieldChecks,
                                                                       Class<A> annotationClass) {
        return (!isEmptyCheckArray(classChecks) || !isEmptyCheckArray(fieldChecks))
                && (UpdatePermission.class.isAssignableFrom(annotationClass)
                || CreatePermission.class.isAssignableFrom(annotationClass));
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

    /**
     * Identifier class for checks.
     */
    @AllArgsConstructor
    private final class CheckIdentifier<A extends Annotation> {
        private final PersistentResource resource;
        private final Class<A> annotation;

        public int hashCode() {
            return new HashCodeBuilder(3, 37)
                    .append(resource)
                    .append(annotation)
                    .toHashCode();
        }

        public boolean equals(CheckIdentifier other) {
            if (other != null || other.resource == null || other.annotation == null) {
                return false;
            }
            return resource.equals(other.resource) && annotation.equals(other.annotation);
        }
    }

    /** Security strategy */

    /**
     * Interface to encode field-aware check result response.
     */
    private interface Strategy {

        /**
         * Execute operation checks.
         */
        void executeOperationChecks();

        /**
         * Execute commit checks.
         */
        void executeCommitChecks();

        /**
         * Whether or not the strategy expects to execute commit checks.
         *
         * @return True if commit checks should be run, false otherwise.
         */
        boolean hasCommitChecks();

        /**
         * Status of the check state.
         */
        enum CheckStatus {
            NO_CHECKS_RUN,
            CHECKS_SUCCEEDED,
            CHECKS_FAILED
        }
    }

    /**
     * Strategy for checking a specific field.
     */
    private final class SpecificFieldStrategy implements Strategy {
        private CheckStatus previousStatus;

        private final PersistentResource resource;
        private final ChangeSpec changeSpec;

        private Class<? extends Check>[] classOpChecks;
        private Class<? extends Check>[] classComChecks;
        private Class<? extends Check>[] fieldOpChecks;
        private Class<? extends Check>[] fieldComChecks;
        private CheckMode classCheckMode;
        private CheckMode fieldCheckMode;

        private final boolean containsDeferredChecks;
        private final boolean isUpdate;

        public <A extends Annotation> SpecificFieldStrategy(PersistentResource resource,
                                                            String field,
                                                            Class<A> annotationClass,
                                                            ChangeSpec changeSpec) {
            this.resource = resource;
            this.previousStatus = CheckStatus.NO_CHECKS_RUN;
            this.changeSpec = changeSpec;
            if (field == null) {
                log.error("Problem occurred trying to initialize Specific Field Strategy: null field.");
                throw new IllegalStateException();
            }
            populateChecks(field, annotationClass);
            containsDeferredChecks = containsCommitChecks(classComChecks, fieldComChecks, annotationClass);
            isUpdate = UpdatePermission.class.isAssignableFrom(annotationClass);
        }

        @Override
        public void executeOperationChecks() {
            execute(classOpChecks, fieldOpChecks, containsDeferredChecks);
        }

        @Override
        public void executeCommitChecks() {
            execute(classComChecks, fieldComChecks, false);
        }

        @Override
        public boolean hasCommitChecks() {
            return containsDeferredChecks;
        }

        private void execute(Class<? extends Check>[] classChecks,
                             Class<? extends Check>[] fieldChecks,
                             boolean hasDeferredChecks) {
            boolean entityFailed = false;
            if (!isEmptyCheckArray(classChecks)) {
                try {
                    runPermissionChecks(classChecks, classCheckMode, resource, changeSpec, isUpdate);
                } catch (ForbiddenAccessException e) {
                    // Ignore this and continue on to checking our fields
                    entityFailed = true;
                }
            }

            if (!isEmptyCheckArray(fieldChecks)) {
                try {
                    runPermissionChecks(fieldChecks, fieldCheckMode, resource, changeSpec, isUpdate);
                    previousStatus = CheckStatus.CHECKS_SUCCEEDED;
                    if (fieldCheckMode == CheckMode.ANY) {
                        return;
                    }
                } catch (ForbiddenAccessException e) {
                    if (fieldCheckMode == CheckMode.ALL || !hasDeferredChecks) {
                        // No need to wait if we either (a) require all checks to pass or (b) don't have deferred checks
                        // to wait on
                        log.debug("Failed a definitive check on a field override. Forbidding access.");
                        throw e;
                    }
                    previousStatus = CheckStatus.CHECKS_FAILED;
                }
            } else if (entityFailed && !hasDeferredChecks && (previousStatus != CheckStatus.CHECKS_SUCCEEDED)) {
                throw new ForbiddenAccessException();
            }
        }

        private <A extends Annotation> void populateChecks(String field, Class<A> annotationClass) {
            EntityDictionary dictionary = resource.getDictionary();

            A annotation = dictionary.getAnnotation(resource, annotationClass);
            if (annotation != null) {
                ExtractedChecks extracted = extractChecks(annotationClass, annotation);
                classOpChecks = extracted.getOperationChecks();
                classComChecks = extracted.getCommitChecks();
                classCheckMode = extracted.getCheckMode();
            }

            annotation =
                    dictionary.getAttributeOrRelationAnnotation(resource.getResourceClass(), annotationClass, field);
            if (annotation != null) {
                ExtractedChecks extracted = extractChecks(annotationClass, annotation);
                fieldOpChecks = extracted.getOperationChecks();
                fieldComChecks = extracted.getCommitChecks();
                fieldCheckMode = extracted.getCheckMode();
            }
        }
    }

    /**
     * Strategy for checking that the entity allows access anywhere.
     */
    private final class AnyFieldStrategy implements Strategy {
        private final PersistentResource resource;
        private final ChangeSpec changeSpec;

        private Class<? extends Check>[] classOpChecks;
        private Class<? extends Check>[] classComChecks;
        private List<Class<? extends Check>[]> fieldOpChecks;
        private List<Class<? extends Check>[]> fieldComChecks;
        private CheckMode classCheckMode;
        private List<CheckMode> fieldCheckModes;

        private final boolean containsDeferredChecks;
        private final boolean isUpdate;

        public <A extends Annotation> AnyFieldStrategy(PersistentResource resource,
                                                       Class<A> annotationClass,
                                                       ChangeSpec changeSpec) {
            this.resource = resource;
            this.changeSpec = changeSpec;
            populateChecks(annotationClass);
            containsDeferredChecks = containsCommitChecks(classComChecks, fieldComChecks, annotationClass);
            isUpdate = UpdatePermission.class.isAssignableFrom(annotationClass);
        }

        @Override
        public void executeOperationChecks() {
            execute(classOpChecks, fieldOpChecks, containsDeferredChecks);
        }

        @Override
        public void executeCommitChecks() {
            execute(classComChecks, fieldComChecks, false);
        }

        @Override
        public boolean hasCommitChecks() {
            return containsDeferredChecks;
        }

        private void execute(Class<? extends Check>[] classChecks,
                             List<Class<? extends Check>[]> fieldChecks,
                             boolean hasDeferredChecks) {
            CheckStatus checkStatus = CheckStatus.NO_CHECKS_RUN;

            // Check full object, then all fields
            if (!isEmptyCheckArray(classChecks)) {
                try {
                    runPermissionChecks(classChecks, classCheckMode, resource, changeSpec, isUpdate);
                    // If this is an "any" check, then we're done. If it is an "all" check, we may have commit checks
                    // queued up. This means we really need to check additional fields and queue up those checks as
                    // well.
                    if (!hasDeferredChecks || classCheckMode == CheckMode.ANY) {
                        return;
                    }
                    checkStatus = CheckStatus.CHECKS_SUCCEEDED;
                } catch (ForbiddenAccessException e) {
                    // Ignore this and continue on to checking our fields
                    checkStatus = CheckStatus.CHECKS_FAILED;
                }
            }

            if (fieldChecks != null && !fieldChecks.isEmpty() && fieldChecks.size() == fieldCheckModes.size()) {
                for (int i = 0 ; i < fieldChecks.size() ; ++i) {
                    try {
                        CheckMode mode = fieldCheckModes.get(i);
                        runPermissionChecks(fieldChecks.get(i), mode, resource, changeSpec, isUpdate);
                        if (mode == CheckMode.ANY) {
                            return;
                        }
                        checkStatus = CheckStatus.CHECKS_SUCCEEDED;
                    } catch (ForbiddenAccessException e) {
                        // Ignore and keep looking or queueing
                        if (checkStatus == CheckStatus.NO_CHECKS_RUN) {
                            checkStatus = CheckStatus.CHECKS_FAILED;
                        }
                    }
                }
            }

            // If nothing succeeded, we know nothing is queued up. We should fail out.
            if ((checkStatus == CheckStatus.CHECKS_FAILED) && !hasDeferredChecks) {
                log.debug("Failed any check with status: {} and deferred checks: {}", checkStatus, hasDeferredChecks);
                throw new ForbiddenAccessException();
            }
        }

        private <A extends Annotation> void populateChecks(Class<A> annotationClass) {
            EntityDictionary dictionary = resource.getDictionary();

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

            fieldOpChecks = new ArrayList<>();
            fieldComChecks = new ArrayList<>();
            fieldCheckModes = new ArrayList<>();

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
        }
    }
}
