/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.security.visitors;

import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.security.checks.FilterExpressionCheck;
import com.paiondata.elide.core.security.checks.UserCheck;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.generated.parsers.ExpressionBaseVisitor;
import com.paiondata.elide.generated.parsers.ExpressionParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.annotation.Annotation;
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
public class CanPaginateVisitor
        extends ExpressionBaseVisitor<CanPaginateVisitor.PaginationStatus> {

    /**
     *  All states except for CANNOT_PAGINATE allow for pagination.
     */
      public enum PaginationStatus {
        CAN_PAGINATE,
        USER_CHECK_FALSE,
        USER_CHECK_TRUE,
        CANNOT_PAGINATE
    };

    private final EntityDictionary dictionary;
    private final RequestScope scope;
    private Set<String> requestedFields;


    public CanPaginateVisitor(EntityDictionary dictionary, RequestScope scope, Set<String> requestedFields) {
        this.dictionary = dictionary;
        this.scope = scope;
        this.requestedFields = requestedFields;
    }

    @Override
    public PaginationStatus visitNOT(ExpressionParser.NOTContext ctx) {
        PaginationStatus status = visit(ctx.expression());
        if (status == PaginationStatus.USER_CHECK_FALSE) {
            return PaginationStatus.USER_CHECK_TRUE;
        }

        if (status == PaginationStatus.USER_CHECK_TRUE) {
            return PaginationStatus.USER_CHECK_FALSE;
        }

        /*
         * Pagination status really only depends on whether the check runs in memory or in the DB.  NOT has not bearing
         * on that.
         */
        return status;
    }

    @Override
    public PaginationStatus visitOR(ExpressionParser.ORContext ctx) {
        PaginationStatus lhs = visit(ctx.left);
        PaginationStatus rhs = visit(ctx.right);

        if (lhs == PaginationStatus.USER_CHECK_TRUE || rhs == PaginationStatus.USER_CHECK_TRUE) {
            return PaginationStatus.USER_CHECK_TRUE;
        }

        if (rhs == PaginationStatus.CANNOT_PAGINATE || lhs == PaginationStatus.CANNOT_PAGINATE) {
            return PaginationStatus.CANNOT_PAGINATE;
        }

        return PaginationStatus.CAN_PAGINATE;
    }

    @Override
    public PaginationStatus visitAND(ExpressionParser.ANDContext ctx) {
        PaginationStatus lhs = visit(ctx.left);
        PaginationStatus rhs = visit(ctx.right);

        if (lhs == PaginationStatus.USER_CHECK_FALSE || rhs == PaginationStatus.USER_CHECK_FALSE) {
            return PaginationStatus.USER_CHECK_FALSE;
        }

        if (rhs == PaginationStatus.CANNOT_PAGINATE || lhs == PaginationStatus.CANNOT_PAGINATE) {
            return PaginationStatus.CANNOT_PAGINATE;
        }

        return PaginationStatus.CAN_PAGINATE;
    }

    @Override
    public PaginationStatus visitPAREN(ExpressionParser.PARENContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public PaginationStatus visitPermissionClass(ExpressionParser.PermissionClassContext ctx) {
        Check check = dictionary.getCheckInstance(ctx.getText());

        //Filter expression checks can always be pushed to the DataStore so pagination is possible
        if (check instanceof FilterExpressionCheck) {
            return PaginationStatus.CAN_PAGINATE;
        }
        if (check instanceof UserCheck) {
            if (((UserCheck) check).ok(scope.getUser())) {
                return PaginationStatus.USER_CHECK_TRUE;
            }
            return PaginationStatus.USER_CHECK_FALSE;
        }
        //Any in memory check will alter (incorrectly) the paginated result
        return PaginationStatus.CANNOT_PAGINATE;
    }

    /**
     * Determines whether a data store can correctly paginate a collection of resources of a given
     * class for a requested set of fields.
     * @param resourceClass The class of resources that will be paginated
     * @param dictionary Used to look up permissions
     * @param scope Contains the user object.
     * @param requestedFields Contains the request info including any sparse fields that were requested
     * @return true if the data store can paginate.  false otherwise.
     */
    public static boolean canPaginate(
            Type<?> resourceClass,
            EntityDictionary dictionary,
            RequestScope scope,
            Set<String> requestedFields
    ) {
        CanPaginateVisitor visitor = new CanPaginateVisitor(dictionary, scope, requestedFields);

        Class<? extends Annotation> annotationClass = ReadPermission.class;
        ParseTree classPermissions = dictionary.getPermissionsForClass(resourceClass, annotationClass);
        PaginationStatus canPaginateClass = PaginationStatus.CAN_PAGINATE;

        if (classPermissions != null) {
            canPaginateClass = visitor.visit(classPermissions);
        }

        List<String> fields = dictionary.getAllExposedFields(resourceClass);

        boolean canPaginate = true;
        for (String field : fields) {
            if (! requestedFields.isEmpty() && ! requestedFields.contains(field)) {
                continue;
            }

            PaginationStatus canPaginateField = canPaginateClass;
            ParseTree fieldPermissions = dictionary.getPermissionsForField(resourceClass, field, annotationClass);
            if (fieldPermissions != null) {
                canPaginateField = visitor.visit(fieldPermissions);
            }

            /*
             * If any of the fields can always be seen by the user, the user can see the entire
             * collection of entities (absent any fields which they cannot see).
             */
            if (canPaginateField == PaginationStatus.USER_CHECK_TRUE) {
                return true;
            }

            /*
             * Except for true user checks above, any field which cannot be paginated means the entire
             * collection cannot be paginated.  If one field has a filter expression check and the other has
             * an in memory check, both checks must be evaluated in memory (effectively any in memory check makes
             * all other checks in memory).
             */
            if (canPaginateField == PaginationStatus.CANNOT_PAGINATE) {
                canPaginate = false;
            }
        }
        return canPaginate;
    }
}
