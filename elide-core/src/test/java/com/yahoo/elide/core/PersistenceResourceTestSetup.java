/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.TestUser;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.OperationCheck;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.google.common.collect.Sets;
import example.Author;
import example.Book;
import example.Child;
import example.Company;
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
import example.UpdateAndCreate;
import example.nontransferable.ContainerWithPackageShare;
import example.nontransferable.NoTransferBiDirectional;
import example.nontransferable.ShareableWithPackageShare;
import example.nontransferable.StrictNoTransfer;
import example.nontransferable.Untransferable;

import io.reactivex.Observable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import nocreate.NoCreateEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import javax.ws.rs.core.MultivaluedMap;

public class PersistenceResourceTestSetup extends PersistentResource {
    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);

    protected final ElideSettings elideSettings;

    protected static LifeCycleHook bookUpdatePrice = mock(LifeCycleHook.class);

    protected static EntityDictionary initDictionary() {
        EntityDictionary dictionary = TestDictionary.getTestDictionary();

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
        dictionary.bindEntity(NoTransferBiDirectional.class);
        dictionary.bindEntity(StrictNoTransfer.class);
        dictionary.bindEntity(Untransferable.class);
        dictionary.bindEntity(Company.class);

        dictionary.bindTrigger(Book.class, "price",
                LifeCycleHookBinding.Operation.UPDATE,
                LifeCycleHookBinding.TransactionPhase.PRESECURITY, bookUpdatePrice);

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
                null, // new request scope + new Child == cannot possibly be a UUID for this object
                new RequestScope(null, null, NO_VERSION, null, null, null, null, null, UUID.randomUUID(),
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
        return new RequestScope(null, path, NO_VERSION, null, tx, user, queryParams, null, UUID.randomUUID(), elideSettings);
    }

    protected <T> PersistentResource<T> bootstrapPersistentResource(T obj) {
        return bootstrapPersistentResource(obj, mock(DataStoreTransaction.class));
    }

    protected <T> PersistentResource<T> bootstrapPersistentResource(T obj, DataStoreTransaction tx) {
        User goodUser = new TestUser("1");
        RequestScope requestScope = new RequestScope(null, null, NO_VERSION, null, tx, goodUser, null, null, UUID.randomUUID(), elideSettings);
        return new PersistentResource<>(obj, requestScope.getUUIDFor(obj), requestScope);
    }

    protected RequestScope getUserScope(User user, AuditLogger auditLogger) {
        return new RequestScope(null, null, NO_VERSION, new JsonApiDocument(), null, user, null, null, UUID.randomUUID(),
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

    protected Company newCompany(String id) {
        final Company company = new Company();
        company.setId(id);
        company.setDescription("company");
        return company;
    }

    /* ChangeSpec-specific test elements */
    @Entity
    @Include(rootLevel = false)
    @CreatePermission(expression = "Prefab.Role.All")
    @ReadPermission(expression = "Prefab.Role.All")
    @UpdatePermission(expression = "Prefab.Role.None")
    @DeletePermission(expression = "Prefab.Role.All")
    public static final class ChangeSpecModel {
        @Id
        public long id;

        @ReadPermission(expression = "Prefab.Role.None")
        @UpdatePermission(expression = "Prefab.Role.None")
        public Predicate<ChangeSpec> checkFunction;

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

        public ChangeSpecModel(final Predicate<ChangeSpec> checkFunction) {
            this.checkFunction = checkFunction;
        }
    }

    @Entity
    @Include(rootLevel = false)
    @EqualsAndHashCode
    @AllArgsConstructor
    @CreatePermission(expression = "Prefab.Role.All")
    @ReadPermission(expression = "Prefab.Role.All")
    @UpdatePermission(expression = "Prefab.Role.All")
    @DeletePermission(expression = "Prefab.Role.All")
    public static final class ChangeSpecChild {
        @Id
        public long id;
    }

    public static final class ChangeSpecCollection extends OperationCheck<Object> {

        @Override
        public boolean ok(Object object, com.yahoo.elide.core.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (changeSpec.isPresent() && (object instanceof ChangeSpecModel)) {
                ChangeSpec spec = changeSpec.get();
                if (!(spec.getModified() instanceof Collection)) {
                    return false;
                }
                return ((ChangeSpecModel) object).checkFunction.test(spec);
            }
            throw new IllegalStateException("Something is terribly wrong :(");
        }
    }

    public static final class ChangeSpecNonCollection extends OperationCheck<Object> {

        @Override
        public boolean ok(Object object, com.yahoo.elide.core.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.filter(c -> object instanceof ChangeSpecModel)
                    .map(c -> ((ChangeSpecModel) object).checkFunction.test(c))
                    .orElseThrow(() -> new IllegalStateException("Something is terribly wrong :("));
        }
    }

    public Set<PersistentResource> getRelation(PersistentResource resource, String relation) {
        Observable<PersistentResource> resources =
                resource.getRelationCheckedFiltered(getRelationship(resource.getResourceType(), relation));

        return resources.toList(LinkedHashSet::new).blockingGet();
    }

    public com.yahoo.elide.core.request.Relationship getRelationship(Type<?> type, String name) {
        return com.yahoo.elide.core.request.Relationship.builder()
                .name(name)
                .alias(name)
                .projection(EntityProjection.builder()
                        .type(type)
                        .build())
                .build();
    }
}
