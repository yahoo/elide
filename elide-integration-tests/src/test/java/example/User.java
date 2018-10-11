/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.OperationCheck;

import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.Transient;


/**
 * Used to test computed attributes.
 */
@Entity
@Include(rootLevel = true)
public class User extends BaseId {
    private int role;

    private String reversedPassword;

    /**
     * @return empty string
     */
    @ComputedAttribute
    @Transient
    public String getPassword() {
        return "";
    }


    /**
     * Sets the password but first reverses it.
     * @param password password to 'encrypt'
     */
    @ComputedAttribute
    @Transient
    public void setPassword(String password) {
        this.reversedPassword = new StringBuilder(password).reverse().toString();
    }

    /**
     * @return the 'encrypted' password
     */
    public String getReversedPassword() {
        return reversedPassword;
    }

    /**
     * Sets the password.  This is intended for Hibernate
     * @param reversedPassword reversed password
     */
    public void setReversedPassword(String reversedPassword) {
        this.reversedPassword = reversedPassword;
    }

    @UpdatePermission(expression = "adminRoleCheck OR updateOnCreate")
    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    static public class AdminRoleCheck extends OperationCheck<User> {
        @Override
        public boolean ok(User user, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return (user.getRole() == 1);
        }

        @Override
        public String checkIdentifier() {
            return "adminRoleCheck";
        }
    }
}
