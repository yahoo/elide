/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.mockito.Mockito.mock;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.OperationCheck;

import com.google.common.collect.Sets;

import example.Author;
import example.Book;
import example.Child;
import example.ComputedBean;
import example.FirstClassFields;
import example.FunWithPermissions;
import example.Invoice;
import example.Job;
import example.Left;
import example.LineItem;
import example.MapColorShape;
import example.NoDeleteEntity;
import example.NoReadEntity;
import example.NoShareEntity;
import example.NoUpdateEntity;
import example.Parent;
import example.Publisher;
import example.Right;
import example.TestCheckMappings;
import example.UpdateAndCreate;
import example.packageshareable.ContainerWithPackageShare;
import example.packageshareable.ShareableWithPackageShare;
import example.packageshareable.UnshareableWithEntityUnshare;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import nocreate.NoCreateEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.ws.rs.core.MultivaluedMap;

public class PersistenceResourceTestSetup extends PersistentResource {
    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);

    protected final ElideSettings elideSettings;

    protected static EntityDictionary initDictionary() {
        EntityDictionary dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(UpdateAndCreate.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);
        dictionary.bindEntity(FunWithPermissions.class);
        dictionary.bindEntity(Job.class);
        dictionary.bindEntity(Left.class);
        dictionary.bindEntity(Right.class);
        dictionary.bindEntity(NoReadEntity.class);
        dictionary.bindEntity(NoDeleteEntity.class);
        dictionary.bindEntity(NoUpdateEntity.class);
        dictionary.bindEntity(NoCreateEntity.class);
        dictionary.bindEntity(NoShareEntity.class);
        dictionary.bindEntity(example.User.class);
        dictionary.bindEntity(FirstClassFields.class);
        dictionary.bindEntity(MapColorShape.class);
        dictionary.bindEntity(PersistentResourceTest.ChangeSpecModel.class);
        dictionary.bindEntity(PersistentResourceTest.ChangeSpecChild.class);
        dictionary.bindEntity(Invoice.class);
        dictionary.bindEntity(LineItem.class);
        dictionary.bindEntity(ComputedBean.class);
        dictionary.bindEntity(ContainerWithPackageShare.class);
        dictionary.bindEntity(ShareableWithPackageShare.class);
        dictionary.bindEntity(UnshareableWithEntityUnshare.class);
        return dictionary;
    }

    protected static ElideSettings initSettings() {
        return new ElideSettingsBuilder(null)
                .withEntityDictionary(initDictionary())
                .withAuditLogger(MOCK_AUDIT_LOGGER)
                .withDefaultMaxPageSize(10)
                .withDefaultPageSize(10)
                .build();
    }

    public PersistenceResourceTestSetup() {
        super(
                new Child(),
                null,
                null, // new request scope + new Child == cannot possibly be a UUID for this object
                new RequestScope(null, null, null, null, null, null,
                        initSettings()
                )
        );

        elideSettings = initSettings();
    }

    protected static Child newChild(int id) {
        Child child = new Child();
        child.setId(id);
        child.setParents(new HashSet<>());
        child.setFriends(new HashSet<>());
        return child;
    }

    protected static Child newChild(int id, String name) {
        Child child = newChild(id);
        child.setName(name);
        return child;
    }

    protected RequestScope buildRequestScope(DataStoreTransaction tx, User user) {
        return buildRequestScope(null, tx, user, null);
    }

    protected RequestScope buildRequestScope(String path, DataStoreTransaction tx, User user, MultivaluedMap<String, String> queryParams) {
        return new RequestScope(null, path, null, tx, user, queryParams, elideSettings);
    }

    protected <T> PersistentResource<T> bootstrapPersistentResource(T obj) {
        return bootstrapPersistentResource(obj, mock(DataStoreTransaction.class));
    }

    protected <T> PersistentResource<T> bootstrapPersistentResource(T obj, DataStoreTransaction tx) {
        User goodUser = new User(1);
        RequestScope requestScope = new RequestScope(null, null, null, tx, goodUser, null, elideSettings);
        return new PersistentResource<>(obj, null, requestScope.getUUIDFor(obj), requestScope);
    }

    protected RequestScope getUserScope(User user, AuditLogger auditLogger) {
        return new RequestScope(null, null, new JsonApiDocument(), null, user, null,
                new ElideSettingsBuilder(null)
                    .withEntityDictionary(dictionary)
                    .withAuditLogger(auditLogger)
                    .build());
    }

    // Testing constructor, setId and non-null empty sets
    protected static Parent newParent(int id) {
        Parent parent = new Parent();
        parent.setId(id);
        parent.setChildren(new HashSet<>());
        parent.setSpouses(new HashSet<>());
        return parent;
    }

    protected Parent newParent(int id, Child child) {
        Parent parent = new Parent();
        parent.setId(id);
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(new HashSet<>());
        return parent;
    }

    /* ChangeSpec-specific test elements */
    @Entity
    @Include
    @CreatePermission(expression = "allow all")
    @ReadPermission(expression = "allow all")
    @UpdatePermission(expression = "deny all")
    @DeletePermission(expression = "allow all")
    public static final class ChangeSpecModel {
        @Id
        public long id;

        @ReadPermission(expression = "deny all")
        @UpdatePermission(expression = "deny all")
        public Function<ChangeSpec, Boolean> checkFunction;

        @UpdatePermission(expression = "changeSpecNonCollection")
        public String testAttr;

        @UpdatePermission(expression = "changeSpecCollection")
        public List<String> testColl;

        @OneToOne
        @UpdatePermission(expression = "changeSpecNonCollection")
        public ChangeSpecChild child;

        @ManyToMany
        @UpdatePermission(expression = "changeSpecCollection")
        public List<Child> otherKids;

        public ChangeSpecModel(final Function<ChangeSpec, Boolean> checkFunction) {
            this.checkFunction = checkFunction;
        }
    }

    @Entity
    @Include
    @EqualsAndHashCode
    @AllArgsConstructor
    @CreatePermission(expression = "allow all")
    @ReadPermission(expression = "allow all")
    @UpdatePermission(expression = "allow all")
    @DeletePermission(expression = "allow all")
    @SharePermission
    public static final class ChangeSpecChild {
        @Id
        public long id;
    }

    public static final class ChangeSpecCollection extends OperationCheck<Object> {

        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (changeSpec.isPresent() && (object instanceof ChangeSpecModel)) {
                ChangeSpec spec = changeSpec.get();
                if (!(spec.getModified() instanceof Collection)) {
                    return false;
                }
                return ((ChangeSpecModel) object).checkFunction.apply(spec);
            }
            throw new IllegalStateException("Something is terribly wrong :(");
        }
    }

    public static final class ChangeSpecNonCollection extends OperationCheck<Object> {

        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (changeSpec.isPresent() && (object instanceof ChangeSpecModel)) {
                return ((ChangeSpecModel) object).checkFunction.apply(changeSpec.get());
            }
            throw new IllegalStateException("Something is terribly wrong :(");
        }
    }

    public static Set<PersistentResource> getRelation(PersistentResource resource, String relation) {
        Optional<FilterExpression> filterExpression =
                resource.getRequestScope().getExpressionForRelation(resource, relation);

        return resource.getRelationCheckedFiltered(relation, filterExpression, Optional.empty(), Optional.empty());
    }
}
