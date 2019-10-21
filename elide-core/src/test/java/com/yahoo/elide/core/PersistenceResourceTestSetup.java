/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.mockito.Mockito.mock;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.AuditLogger;

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
import example.Right;
import example.TestCheckMappings;
import example.packageshareable.ContainerWithPackageShare;
import example.packageshareable.ShareableWithPackageShare;
import example.packageshareable.UnshareableWithEntityUnshare;
import nocreate.NoCreateEntity;

import java.util.HashSet;

public class PersistenceResourceTestSetup extends PersistentResource {

    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);


    protected final ElideSettings elideSettings;
    void init() {
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
    }

    public PersistenceResourceTestSetup() {
        super(
                new Child(),
                null,
                null, // new request scope + new Child == cannot possibly be a UUID for this object
                new RequestScope(null, null, null, null, null,
                        new ElideSettingsBuilder(null)
                                .withEntityDictionary(new EntityDictionary(TestCheckMappings.MAPPINGS))
                                .withAuditLogger(MOCK_AUDIT_LOGGER)
                                .withDefaultMaxPageSize(10)
                                .withDefaultPageSize(10)
                                .build()
                )
        );

        elideSettings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .withAuditLogger(MOCK_AUDIT_LOGGER)
                .withDefaultMaxPageSize(10)
                .withDefaultPageSize(10)
                .build();
        init();
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
}
