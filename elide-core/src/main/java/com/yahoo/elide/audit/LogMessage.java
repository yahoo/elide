/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.audit;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.ResourceLineage;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;

import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

/**
 * An audit log message that can be logged to a logger.
 */
public class LogMessage {
    //Supposedly this is thread safe.
    private static ExpressionFactory expressionFactory;
    private static final String CACHE_SIZE = "5000";
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    static {
        Properties properties = new Properties();
        properties.put("javax.el.cacheSize", CACHE_SIZE);
        expressionFactory = new ExpressionFactoryImpl();
    }

    private final String template;
    private final PersistentResource record;
    private final String[] expressions;
    private final int operationCode;

    /**
     * Construct a log message that does not involve any templating.
     * @param template - The unsubstituted text that will be logged.
     * @param code - The operation code of the auditable action.
     */
    public LogMessage(String template, int code) {
        this(template, null, EMPTY_STRING_ARRAY, code);
    }

    /**
     * Construct a log message from an Audit annotation and the record that was updated in some way.
     * @param audit - The annotation containing the type of operation (UPDATE, DELETE, CREATE)
     * @param record - The modified record
     * @throws InvalidSyntaxException if the Audit annotation has invalid syntax.
     */
    public LogMessage(Audit audit, PersistentResource record) throws InvalidSyntaxException {
        this(audit.logStatement(), record, audit.logExpressions(), audit.operation());
    }

    /**
     * Construct a log message.
     * @param template - The log message template that requires variable substitution.
     * @param record - The record which will serve as the data to substitute.
     * @param expressions - A set of UEL expressions that reference record.
     * @param code - The operation code of the auditable action.
     * @throws InvalidSyntaxException the invalid syntax exception
     */
    public LogMessage(String template,
            PersistentResource record,
            String[] expressions,
            int code) throws InvalidSyntaxException {
        this.template = template;
        this.record = record;
        this.expressions = expressions;
        this.operationCode = code;
    }

    /**
     * Gets operation code.
     *
     * @return the operation code
     */
    public int getOperationCode() {
        return operationCode;
    }

    /**
     * Gets message.
     *
     * @return the message
     */
    public String getMessage() {
        SimpleContext ctx = new SimpleContext();

        if (record != null) {
            /* Create a new lineage which includes the passed in record */
            ResourceLineage lineage = new ResourceLineage(record.getLineage(), record);

            for (String name : lineage.getKeys()) {
                List<PersistentResource> values = lineage.getRecord(name);

                ValueExpression expression;
                if (values.size() == 1) {
                    expression = expressionFactory.createValueExpression(values.get(0).getObject(), Object.class);
                } else {
                    List<Object> objects = values.stream().map(PersistentResource::getObject)
                            .collect(Collectors.toList());
                    expression = expressionFactory.createValueExpression(objects, List.class);
                }
                ctx.setVariable(name, expression);
            }

            // Add the OpaqueUser to available expressions as "user"
            final Object user = record.getOpaqueUser();
            ctx.setVariable("user", expressionFactory.createValueExpression(user, Object.class));
        }

        Object[] results = new Object[expressions.length];
        for (int idx = 0; idx < results.length; idx++) {
            String expressionText = expressions[idx];

            ValueExpression expression;
            try {
                expression = expressionFactory.createValueExpression(ctx, expressionText, Object.class);
            } catch (ELException e) {
                throw new InvalidSyntaxException(e);
            }
            Object result = expression.getValue(ctx);
            results[idx] = result;
        }

        try {
            return MessageFormat.format(template, results);
        } catch (IllegalArgumentException e) {
            throw new InvalidSyntaxException(e);
        }
    }

    public RequestScope getRequestScope() {
        if (record != null) {
            return record.getRequestScope();
        }
        return null;
    }

    @Override
    public String toString() {
        return "LogMessage{"
                + "message='" + getMessage() + '\''
                + ", operationCode=" + getOperationCode()
                + '}';
    }
}
