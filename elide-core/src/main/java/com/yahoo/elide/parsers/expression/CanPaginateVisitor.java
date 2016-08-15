/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.CheckInstantiator;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.generated.parsers.ExpressionBaseVisitor;
import com.yahoo.elide.generated.parsers.ExpressionParser;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.UserCheck;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Walks a permission expression to determine if any part of the expression must be evaluated in memory.
 * If part of the expression must be evaluated in memory, the data store cannot paginate the result.
 *
 * Here are some examples of what the expected behavior is for combining user and filter expression checks:
 *
 * User Check (TRUE) OR Filter Expression (TRUE)
 *  - Elide will not push the filter predicate to the data store.
 *  - Elide will allow pagination.
 *  - The entire result set will be paginated.
 *  - The entire result set will be returned.
 *
 * User Check (TRUE) OR Filter Expression (FALSE)
 *  - Elide will not push the filter predicate to the data store.
 *  - Elide will allow pagination.
 *  - The entire result set will be paginated.
 *  - The entire result set will be returned.
 *
 * User Check (FALSE) OR Filter Expression (TRUE)
 *  - Elide will push the filter predicate to the data store.
 *  - Elide will allow pagination.
 *  - The filtered result set will be paginated.
 *  - The filtered result set will be returned.
 *
 * User Check (FALSE) OR Filter Expression (FALSE)
 *  - Elide will push the filter predicate to the data store.
 *  - Elide will allow pagination.
 *  - The empty result set will be paginated.
 *  - The empty result set will be returned.
 *
 * User Check (TRUE) AND Filter Expression (TRUE)
 *  - Elide will push the filter predicate to the data store.
 *  - Elide will allow pagination.
 *  - The filtered result set will be paginated.
 *  - The filtered result set will be returned.
 *
 * User Check (TRUE) AND Filter Expression (FALSE)
 *  - Elide will push the filter predicate to the data store.
 *  - Elide will allow pagination.
 *  - The empty result set will be paginated.
 *  - The empty result set will be returned.
 *
 * User Check (FALSE) AND Filter Expression (TRUE)
 *  - Elide will push the filter predicate to the data store.
 *  - Elide will allow pagination.
 *  - The filtered result set will be paginated.
 *  - The empty result set will be returned.
 *
 * User Check (FALSE) AND Filter Expression (FALSE)
 *  - Elide will push the filter predicate to the data store.
 *  - Elide will allow pagination.
 *  - The empty result set will be paginated.
 *  - The empty result set will be returned.
 *
 * More Complex Scenarios:
 *
 * (User Check (TRUE) OR Filter Expression 1 (FALSE)) AND Filter Expression 2 (TRUE)
 *  - Elide WILL push the filter predicate to the data store.
 *  - Elide will allow pagination.
 *  - The filtered (2) result set will be paginated.
 *  - The filtered (2) result set will be returned.
 *
 * (User Check (TRUE) OR Filter Expression 1 (TRUE)) AND Filter Expression 2 (TRUE)
 *  - Elide WILL push the filter predicate (2) to the data store.
 *  - Elide will allow pagination.
 *  - The filtered (2) result set will be paginated.
 *  - The filtered (2) result set will be returned.
 *
 * (User Check (FALSE) OR Filter Expression 1 (TRUE)) AND Filter Expression 2 (TRUE)
 *  - Elide WILL push the filter predicate (1 and 2) to the data store.
 *  - Elide will allow pagination.
 *  - The filtered (1 and 2) result set will be paginated.
 *  - The filtered (1 and 2) result set will be returned.
 *
 * (User Check (FALSE) OR Filter Expression 1 (FALSE)) AND Filter Expression 2 (TRUE)
 *  - Elide WILL push the filter predicate (1 and 2) to the data store.
 *  - Elide will allow pagination.
 *  - The empty result set will be paginated.
 *  - The empty result set will be returned.
 *
 * (User Check (TRUE) AND Filter Expression 1 (FALSE)) OR Filter Expression 2 (TRUE)
 *  - Elide WILL push the filter predicate (1 or 2) to the data store.
 *  - Elide will allow pagination.
 *  - The filtered (2) result set will be paginated.
 *  - The filtered (2) result set will be returned.
 *
 * (User Check (TRUE) AND Filter Expression 1 (TRUE)) OR Filter Expression 2 (TRUE)
 *  - Elide WILL push the filter predicate (1 or 2) to the data store.
 *  - Elide will allow pagination.
 *  - The filtered (1 or 2) result set will be paginated.
 *  - The filtered (1 or 2) result set will be returned.
 *
 * (User Check (FALSE) AND Filter Expression 1 (TRUE)) OR Filter Expression 2 (TRUE)
 *  - Elide WILL push the filter predicate (2) to the data store.
 *  - Elide will allow pagination.
 *  - The filtered (2) result set will be paginated.
 *  - The filtered (2) result set will be returned
 *
 * (User Check (FALSE) AND Filter Expression 1 (FALSE)) OR Filter Expression 2 (TRUE)
 *  - Elide WILL push the filter predicate (2) to the data store.
 *  - Elide will allow pagination.
 *  - The filtered (2) result set will be paginated.
 *  - The filtered (2) result set will be returned
 *
 */
public class CanPaginateVisitor extends ExpressionBaseVisitor<Boolean> implements CheckInstantiator {

    public static final Boolean CAN_PAGINATE = true;
    public static final Boolean CANNOT_PAGINATE = false;

    private final EntityDictionary dictionary;


    public CanPaginateVisitor(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }


    @Override
    public Boolean visitNOT(ExpressionParser.NOTContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Boolean visitOR(ExpressionParser.ORContext ctx) {
        boolean lhs = visit(ctx.left);
        boolean rhs = visit(ctx.right);

        /* If either side requires in memory filtering, the data store cannot paginate */
        if (lhs == CANNOT_PAGINATE || rhs == CANNOT_PAGINATE) {
            return CANNOT_PAGINATE;
        }
        return CAN_PAGINATE;
    }

    @Override
    public Boolean visitAND(ExpressionParser.ANDContext ctx) {
        boolean lhs = visit(ctx.left);
        boolean rhs = visit(ctx.right);

        /* If either side requires in memory filtering, the data store cannot paginate */
        if (lhs == CANNOT_PAGINATE || rhs == CANNOT_PAGINATE) {
            return CANNOT_PAGINATE;
        }
        return CAN_PAGINATE;
    }

    @Override
    public Boolean visitPAREN(ExpressionParser.PARENContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Boolean visitPermissionClass(ExpressionParser.PermissionClassContext ctx) {
        Check check = getCheck(dictionary, ctx.getText());

        //Filter expression checks can always be pushed to the DataStore so pagination is possible
        if (FilterExpressionCheck.class.isAssignableFrom(check.getClass())) {
            return CAN_PAGINATE;

        //User Checks have no bearing on pagination since they are true or false for every item in the collection
        } else if (UserCheck.class.isAssignableFrom(check.getClass())) {
            return CAN_PAGINATE;
        //Any in memory check will alter (incorrectly) the paginated result
        } else {
            return CANNOT_PAGINATE;
        }
    }

    /**
     * Determines whether a data store can correctly paginate a collection of resources of a given
     * class for a requested set of fields.
     * @param resourceClass The class of resources that will be paginated
     * @param dictionary Used to look up permissions
     * @param scope Contains the request info including any sparse fields that were requested
     * @return true if the data store can paginate.  false otherwise.
     */
    public static boolean canPaginate(Class<?> resourceClass, EntityDictionary dictionary, RequestScope scope) {

        CanPaginateVisitor visitor = new CanPaginateVisitor(dictionary);

        Class<? extends Annotation> annotationClass = ReadPermission.class;
        ParseTree classPermissions = dictionary.getPermissionsForClass(resourceClass, annotationClass);
        Boolean canPaginateClass = CAN_PAGINATE;
        if (classPermissions != null) {
            canPaginateClass = visitor.visit(classPermissions);
        }

        List<String> fields = dictionary.getAllFields(resourceClass);
        String resourceName = dictionary.getJsonAliasFor(resourceClass);
        Set<String> requestedFields = scope.getSparseFields().getOrDefault(resourceName, Collections.EMPTY_SET);

        for (String field : fields) {
            if (! requestedFields.isEmpty() && ! requestedFields.contains(field)) {
                continue;
            }
            Boolean canPaginateField = canPaginateClass;
            ParseTree fieldPermissions = dictionary.getPermissionsForField(resourceClass, field, annotationClass);
            if (fieldPermissions != null) {
                canPaginateField = visitor.visit(fieldPermissions);
            }
            if (canPaginateField == CANNOT_PAGINATE) {
                return CANNOT_PAGINATE;
            }
        }
        return CAN_PAGINATE;
    }
}
