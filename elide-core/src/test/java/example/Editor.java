/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.ComputedRelationship;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.FilterExpressionPath;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.NotNullPredicate;
import com.yahoo.elide.security.FilterExpressionCheck;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Model for authors.
 */
@Entity
@Table(name = "editor")
@Include(rootLevel = true)
@SharePermission
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${editor.name}"})
public class Editor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private Long id;

    @Exclude
    private String naturalKey = UUID.randomUUID().toString();

    @Override
    public int hashCode() {
        return naturalKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Editor)) {
            return false;
        }

        return ((Editor) obj).naturalKey.equals(naturalKey);
    }

    @Getter @Setter
    private String firstName;

    @Getter @Setter
    private String lastName;

    @ComputedAttribute
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Transient
    @ComputedRelationship
    @OneToOne
    @FilterExpressionPath("this")
    @ReadPermission(expression = "Field path editor check")
    public Editor getEditor() {
        return this;
    }

    public static class FieldPathFilterExpression extends FilterExpressionCheck {
        @Override
        public FilterPredicate getFilterExpression(Class entityClass, com.yahoo.elide.security.RequestScope requestScope) {
            Path path = super.getFieldPath(entityClass, requestScope, "getEditor", "editor");
            return new NotNullPredicate(path);
        }
    }
}
