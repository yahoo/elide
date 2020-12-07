/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.validator;

import com.yahoo.elide.generated.parsers.ExpressionBaseVisitor;
import com.yahoo.elide.generated.parsers.ExpressionParser;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Expression Visitor.
 */
@AllArgsConstructor
public class PermissionExpressionVisitor extends ExpressionBaseVisitor<Set<String>> {

    @Override
    public Set<String> visitNOT(ExpressionParser.NOTContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Set<String> visitOR(ExpressionParser.ORContext ctx) {
        Set<String> visit = visit(ctx.left);
        visit.addAll(visit(ctx.right));
        return visit;
    }

    @Override
    public Set<String> visitAND(ExpressionParser.ANDContext ctx) {
        Set<String> visit = visit(ctx.left);
        visit.addAll(visit(ctx.right));
        return visit;
    }

    @Override
    public Set<String> visitPAREN(ExpressionParser.PARENContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Set<String> visitPermissionClass(ExpressionParser.PermissionClassContext ctx) {
        return new HashSet<>(Arrays.asList(ctx.getText()));
    }
}
