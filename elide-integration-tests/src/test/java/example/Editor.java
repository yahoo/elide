/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.Audit;
import com.paiondata.elide.annotation.ComputedAttribute;
import com.paiondata.elide.annotation.ComputedRelationship;
import com.paiondata.elide.annotation.Exclude;
import com.paiondata.elide.annotation.FilterExpressionPath;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.filter.predicates.NotNullPredicate;
import com.paiondata.elide.core.security.checks.FilterExpressionCheck;
import com.paiondata.elide.core.type.Type;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Model for authors.
 */
@Entity
@Table(name = "editor")
@Include
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

    @Getter @Setter
    private String firstName;

    @Getter @Setter
    private String lastName;

    @Override
    public int hashCode() {
        return naturalKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Editor && ((Editor) obj).naturalKey.equals(naturalKey);
    }

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

    public static class FieldPathFilterExpression extends FilterExpressionCheck<Object> {
        @Override
        public FilterPredicate getFilterExpression(Type<?> entityClass, com.paiondata.elide.core.security.RequestScope requestScope) {
            Path path = super.getFieldPath(entityClass, requestScope, "getEditor", "editor");
            return new NotNullPredicate(path);
        }
    }
}
