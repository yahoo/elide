/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.security.Role;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.UserCheck;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@CreatePermission(any = {Role.ALL.class})
// Make sure we can override both standard checks and UserCheck's
@ReadPermission(all = {YetAnotherPermission.SampleUserCheck.class})
@Include(rootLevel = true)
@Entity
public class YetAnotherPermission {
    private Long id;
    private String hiddenName;
    private String youShouldBeAbleToRead;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHiddenName() {
        return hiddenName;
    }

    public void setHiddenName(String hiddenName) {
        this.hiddenName = hiddenName;
    }

    @ReadPermission(any = {Role.ALL.class})
    public String getYouShouldBeAbleToRead() {
        return youShouldBeAbleToRead;
    }

    public void setYouShouldBeAbleToRead(String youShouldBeAbleToRead) {
        this.youShouldBeAbleToRead = youShouldBeAbleToRead;
    }

    public static final class SampleUserCheck implements UserCheck<YetAnotherPermission> {
        @Override
        public UserPermission userPermission(User user) {
            return DENY;
        }

        @Override
        public boolean ok(PersistentResource<YetAnotherPermission> record) {
            return false;
        }
    }
}
