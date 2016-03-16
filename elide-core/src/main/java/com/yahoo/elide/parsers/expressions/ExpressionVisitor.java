/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expressions;

import com.yahoo.elide.generated.parsers.ExpressionBaseVisitor;
import com.yahoo.elide.generated.parsers.ExpressionParser;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.CommitCheck;
import com.yahoo.elide.security.permissions.ExpressionResultCache;
import com.yahoo.elide.security.permissions.expressions.AndExpression;
import com.yahoo.elide.security.permissions.expressions.DeferredCheckExpression;
import com.yahoo.elide.security.permissions.expressions.DeterministicExpression;
import com.yahoo.elide.security.permissions.expressions.Expression;
import com.yahoo.elide.security.permissions.expressions.ImmediateCheckExpression;
import com.yahoo.elide.security.permissions.expressions.NotExpression;
import com.yahoo.elide.security.permissions.expressions.OrExpression;

/**
 * Expression Visitor.
 */
public class ExpressionVisitor extends ExpressionBaseVisitor<Expression> {
    private final PersistentResource resource;
    private final RequestScope requestScope;
    private final ChangeSpec changeSpec;
    private final ExpressionResultCache cache;


    public ExpressionVisitor(PersistentResource resource,
                             RequestScope requestScope,
                             ChangeSpec changeSpec,
                             ExpressionResultCache cache) {
        this.resource = resource;
        this.requestScope = requestScope;
        this.changeSpec = changeSpec;
        this.cache = cache;
    }

    @Override
    public Expression visitStart(ExpressionParser.StartContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Expression visitNOT(ExpressionParser.NOTContext ctx) {
        // Create a not expression
        return new NotExpression(visitChildren(ctx.expression()));
    }

    @Override
    public Expression visitOR(ExpressionParser.ORContext ctx) {
        return new OrExpression(visitChildren(ctx.left), visitChildren(ctx.right));
    }

    @Override
    public Expression visitAND(ExpressionParser.ANDContext ctx) {
        return new AndExpression(visitChildren(ctx.left), visitChildren(ctx.right));
    }

    @Override
    public Expression visitPAREN(ExpressionParser.PARENContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Expression visitEXPRESSION(ExpressionParser.EXPRESSIONContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Expression visitExpressionClass(ExpressionParser.ExpressionClassContext ctx) {
        try {
            Class checkClass = Class.forName(ctx.getChild(0).getText());


            if (CommitCheck.class.isAssignableFrom(checkClass)) {
                Class<? extends CommitCheck> changeToCommitCheck = checkClass;
                return new DeterministicExpression(
                        new DeferredCheckExpression(
                                changeToCommitCheck.newInstance(),
                                this.resource,
                                this.requestScope,
                                this.changeSpec,
                                this.cache
                        ).evaluate()
                );

            } else {
                Class<? extends Check> changeToCheck = checkClass;
                return new DeterministicExpression(
                        new ImmediateCheckExpression(
                                changeToCheck.newInstance(),
                                this.resource,
                                this.requestScope,
                                this.changeSpec,
                                this.cache
                        ).evaluate()
                );

            }
        } catch (ClassNotFoundException e) {
            // What do I do hmm.
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return visitChildren(ctx);
    }
}
