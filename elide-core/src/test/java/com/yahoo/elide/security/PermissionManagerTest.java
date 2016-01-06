/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.audit.InvalidSyntaxException;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;

import org.testng.annotations.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.util.Optional;

public class PermissionManagerTest {

    @Test(expectedExceptions = {InvalidSyntaxException.class})
    public void testBadCheckExtraction() {
        // Non-permission annotation passed in
        PermissionManager.extractChecks(Entity.class, new Entity() {
            @Override
            public String name() {
                return null;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }
        });
    }

    @Test(expectedExceptions = {InvalidSyntaxException.class})
    public void testNoChecks() {
        PermissionManager.extractChecks(UpdatePermission.class, new UpdatePermission() {
            @Override
            public Class<? extends Check>[] any() {
                return new Class[]{};
            }

            @Override
            public Class<? extends Check>[] all() {
                return new Class[]{};
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }
        });
    }

    @Test
    public void testSuccessfulOperationCheck() throws Exception {
        @Entity
        @UpdatePermission(all = {SampleOperationCheck.class})
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);
        requestScope.getPermissionManager().checkPermission(UpdatePermission.class, resource, cspec);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testFailOperationCheckAll() throws Exception {
        @Entity
        @UpdatePermission(all = {SampleOperationCheck.class, Access.NONE.class})
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkPermission(UpdatePermission.class, resource);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testFailOperationCheckAny() throws Exception {
        @Entity
        @UpdatePermission(any = {SampleOperationCheck.class, SampleCommitCheck.class})
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkPermission(UpdatePermission.class, resource);
        // Update permissions are deferred. In the case of "any," commit checks must execute before failure can be detected
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test
    public void testSuccessfulCommitChecks() throws Exception {
        @Entity
        @UpdatePermission(all = {SampleCommitCheck.class})
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);
        requestScope.getPermissionManager().checkPermission(UpdatePermission.class, resource, cspec);
        requestScope.getPermissionManager().executeCommitChecks();
    }


    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testFailCommitChecks() throws Exception {
        @Entity
        @UpdatePermission(all = {SampleCommitCheck.class})
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkPermission(UpdatePermission.class, resource);
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAllAnyField() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFieldAwareSuccessFailureAnyField() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, ReadPermission.class);
    }

    @Test
    public void testReadFieldAwareSuccessAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class, "allVisible");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFieldAwareFailureAllSpecificField() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, ReadPermission.class, "allVisible");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFieldAwareFailureAllNoOverride() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, ReadPermission.class, "defaultHidden");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFieldAwareFailureAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, ReadPermission.class, "cannotSeeMe");
    }

    @Test
    public void testReadFieldAwareSuccessAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class, "mayFailInCommit");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFieldAwareFailureAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, ReadPermission.class, "mayFailInCommit");
    }

    @Test
    public void testUpdateFieldAwareSuccessAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "allVisible");
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testUpdateFieldAwareFailureAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, UpdatePermission.class, "allVisible");
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareSuccessAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "mayFailInCommit");
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testUpdateFieldAwareFailureAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, UpdatePermission.class, "mayFailInCommit");
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareEntireOpenBean() {
        PersistentResource resource = newResource(OpenBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, ReadPermission.class);
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, ReadPermission.class, "open");
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFailureFieldAwareOpenBean() {
        PersistentResource resource = newResource(OpenBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, ReadPermission.class, "openAll");
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test
    public void testPassAnyFieldAwareFailOperationSuccessCommit() {
        @Entity
        @Include
        @UpdatePermission(all = {Access.NONE.class, PassingCommitCheck.class})
        class Model {
            @Id
            public Long id;

            @UpdatePermission(any = {Access.NONE.class, PassingCommitCheck.class})
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, UpdatePermission.class);
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test(expectedExceptions = {ForbiddenAccessException.class})
    public void testFailAllFieldAwareSuccessOperationFailCommit() {
        @Entity
        @Include
        @UpdatePermission(all = {Access.NONE.class})
        class Model {
            @Id
            public Long id;

            @UpdatePermission(all = {Access.ALL.class, FailingCommitCheck.class})
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, UpdatePermission.class);
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test
    public void testPassAnySpecificFieldAwareFailOperationSuccessCommit() {
        @Entity
        @Include
        @UpdatePermission(all = {Access.NONE.class, PassingCommitCheck.class})
        class Model {
            @Id
            public Long id;

            @UpdatePermission(any = {Access.NONE.class, PassingCommitCheck.class})
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, UpdatePermission.class, "field");
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test(expectedExceptions = {ForbiddenAccessException.class})
    public void testFailAllSpecificFieldAwareSuccessOperationFailCommit() {
        @Entity
        @Include
        @UpdatePermission(all = {Access.ALL.class})
        class Model {
            @Id
            public Long id;

            @UpdatePermission(all = {Access.ALL.class, FailingCommitCheck.class})
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, UpdatePermission.class, "field");
        requestScope.getPermissionManager().executeCommitChecks();
    }

    @Test(expectedExceptions = {InvalidSyntaxException.class})
    public void testBadInstance() {
        @Entity
        @UpdatePermission(all = {PrivatePermission.class})
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionManager().checkFieldAwarePermissions(resource, null, UpdatePermission.class);
    }

    public <T> PersistentResource newResource(T obj, Class<T> cls) {
        EntityDictionary dictionary = new EntityDictionary();
        dictionary.bindEntity(cls);
        RequestScope requestScope = new RequestScope(null, null, null, dictionary, null, null);
        return new PersistentResource<>(obj, requestScope);
    }

    public PersistentResource newResource(Class cls) {
        EntityDictionary dictionary = new EntityDictionary();
        dictionary.bindEntity(cls);
        RequestScope requestScope = new RequestScope(null, null, null, dictionary, null, null);
        try {
            return new PersistentResource<>(cls.newInstance(), requestScope);
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    public static final class SampleOperationCheck implements OperationCheck<Object> {
        @Override
        public boolean ok(Object object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.isPresent();
        }
    }

    public static final class SampleCommitCheck implements CommitCheck<Object> {
        @Override
        public boolean ok(Object object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.isPresent();
        }
    }

    public static final class PassingCommitCheck implements CommitCheck<Object> {
        @Override
        public boolean ok(Object object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return true;
        }
    }

    public static final class FailingCommitCheck implements CommitCheck<Object> {
        @Override
        public boolean ok(Object object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
        }
    }

    private static final class PrivatePermission implements OperationCheck<Object> {
        @Override
        public boolean ok(Object object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
        }
    }

    @ReadPermission(all = {Access.NONE.class})
    @UpdatePermission(all = {Access.NONE.class})
    @Include
    @Entity
    public static final class SampleBean {
        @Id
        public Long id;

        @ReadPermission(all = {Access.ALL.class, SampleOperationCheck.class})
        @UpdatePermission(all = {Access.ALL.class, SampleCommitCheck.class})
        public String allVisible = "You should see me!";

        public String defaultHidden = "I'm invisible. muwahaha...";

        @ReadPermission(all = {Access.ALL.class, Access.NONE.class})
        @UpdatePermission(all = {Access.ALL.class, Access.NONE.class})
        public String cannotSeeMe = "hidden";

        @ReadPermission(any = {SampleOperationCheck.class})
        @UpdatePermission(any = {SampleCommitCheck.class, Access.NONE.class})
        public String mayFailInCommit = "aw :(";
    }

    @ReadPermission(all = {Access.ALL.class})
    @UpdatePermission(all = {Access.ALL.class})
    @Include
    @Entity
    public static final class OpenBean {
        @Id
        public Long id;

        public String open;

        @ReadPermission(all = {Access.NONE.class, SampleOperationCheck.class})
        @UpdatePermission(all = {SampleCommitCheck.class, SampleOperationCheck.class})
        public String openAll = "all";

        @ReadPermission(any = {SampleOperationCheck.class, SampleOperationCheck.class})
        @UpdatePermission(any = {SampleCommitCheck.class, SampleOperationCheck.class})
        public String openAny = "all";
    }
}
