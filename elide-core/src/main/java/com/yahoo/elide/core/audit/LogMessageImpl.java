/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.audit;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.core.ResourceLineage;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.PersistentResource;
import com.yahoo.elide.core.security.User;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.Principal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.PropertyNotFoundException;
import javax.el.ValueExpression;

/**
 * An audit log message that can be logged to a logger.
 */
@ToString
@EqualsAndHashCode
public class LogMessageImpl implements LogMessage {
    //Supposedly this is thread safe.
    private static final ExpressionFactory EXPRESSION_FACTORY = new ExpressionFactoryImpl();
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final String template;
    private final String[] expressions;

    @Getter
    private final int operationCode;

    @Getter
    private final Optional<ChangeSpec> changeSpec;

    @Getter
    private final User user;

    @Getter
    private final PersistentResource persistentResource;

    /**
     * Construct a log message that does not involve any templating.
     * @param template - The unsubstituted text that will be logged.
     * @param code - The operation code of the auditable action.
     */
    public LogMessageImpl(String template, int code) {
        this(template, null, EMPTY_STRING_ARRAY, code, Optional.empty());
    }

    /**
     * Construct a log message from an Audit annotation and the record that was updated in some way.
     * @param audit - The annotation containing the type of operation (UPDATE, DELETE, CREATE)
     * @param record - The modified record
     * @param changeSpec - Change spec of modified elements (if logging object change). empty otherwise
     * @throws InvalidSyntaxException if the Audit annotation has invalid syntax.
     */
    public LogMessageImpl(Audit audit, PersistentResource record, Optional<ChangeSpec> changeSpec)
            throws InvalidSyntaxException {
        this(audit.logStatement(), record, audit.logExpressions(), audit.operation(), changeSpec);
    }

    /**
     * Construct a log message.
     * @param template - The log message template that requires variable substitution.
     * @param record - The record which will serve as the data to substitute.
     * @param expressions - A set of UEL expressions that reference record.
     * @param code - The operation code of the auditable action.
     * @param changeSpec - the change spec that we want to log
     * @throws InvalidSyntaxException the invalid syntax exception
     */
    public LogMessageImpl(String template,
                          PersistentResource record,
                          String[] expressions,
                          int code,
                          Optional<ChangeSpec> changeSpec) throws InvalidSyntaxException {
        this.template = template;
        this.persistentResource = record;
        this.expressions = expressions;
        this.operationCode = code;
        this.changeSpec = changeSpec;
        this.user = (record == null ? null : record.getRequestScope().getUser());
    }

    @Override
    public String getMessage() {
        final SimpleContext ctx = new SimpleContext();
        final SimpleContext singleElementContext = new SimpleContext();

        if (persistentResource != null) {
            /* Create a new lineage which includes the passed in record */
            com.yahoo.elide.core.PersistentResource internalResource = (
                    com.yahoo.elide.core.PersistentResource) persistentResource;
            ResourceLineage lineage = new ResourceLineage(internalResource.getLineage(), internalResource, null);

            for (String name : lineage.getKeys()) {
                List<com.yahoo.elide.core.PersistentResource> values = lineage.getRecord(name);

                final ValueExpression expression;
                final ValueExpression singleElementExpression;
                if (values.size() == 1) {
                    expression = EXPRESSION_FACTORY.createValueExpression(values.get(0).getObject(), Object.class);
                    singleElementExpression = expression;
                } else {
                    List<Object> objects = values.stream().map(PersistentResource::getObject)
                            .collect(Collectors.toList());
                    expression = EXPRESSION_FACTORY.createValueExpression(objects, List.class);
                    singleElementExpression = EXPRESSION_FACTORY.createValueExpression(values.get(values.size() - 1)
                            .getObject(), Object.class);
                }
                ctx.setVariable(name, expression);
                singleElementContext.setVariable(name, singleElementExpression);
            }

            final Principal user = getUser().getPrincipal();
            if (user != null) {
                final ValueExpression opaqueUserValueExpression = EXPRESSION_FACTORY
                    .createValueExpression(
                        user, Object.class
                    );
                ctx.setVariable("opaqueUser", opaqueUserValueExpression);
                singleElementContext.setVariable("opaqueUser", opaqueUserValueExpression);
            }
        }

        Object[] results = new Object[expressions.length];
        for (int idx = 0; idx < results.length; idx++) {
            String expressionText = expressions[idx];

            final ValueExpression expression;
            final ValueExpression singleElementExpression;
            try {
                expression = EXPRESSION_FACTORY.createValueExpression(ctx, expressionText, Object.class);
                singleElementExpression =
                        EXPRESSION_FACTORY.createValueExpression(singleElementContext, expressionText, Object.class);
            } catch (ELException e) {
                throw new InvalidSyntaxException(e);
            }

            Object result;
            try {
                // Single element expressions are intended to allow for access to ${entityType.field} when there are
                // multiple "entityType" types listed in the lineage. Without this, any access to an entityType
                // without an explicit list index would otherwise result in a 500. Similarly, since we already
                // supported lists (i.e. the ${entityType[idx].field} syntax), this also continues to support that.
                // It should be noted, however, that list indexing is somewhat brittle unless properly accounted for
                // from all possible paths.
                result = singleElementExpression.getValue(singleElementContext);
            } catch (PropertyNotFoundException e) {
                // Try list syntax if not single element
                result = expression.getValue(ctx);
            }
            results[idx] = result;
        }

        try {
            return MessageFormat.format(template, results);
        } catch (IllegalArgumentException e) {
            throw new InvalidSyntaxException(e);
        }
    }
}
