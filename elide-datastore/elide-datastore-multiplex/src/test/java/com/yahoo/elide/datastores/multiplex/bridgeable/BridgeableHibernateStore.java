/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex.bridgeable;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import com.yahoo.elide.datastores.hibernate5.HibernateTransaction;
import com.yahoo.elide.datastores.multiplex.BridgeableTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexTransaction;
import com.yahoo.elide.example.beans.HibernateUser;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.Collections;
import java.util.Optional;

/**
 * Example bridgeable store implementation.
 */
public class BridgeableHibernateStore extends HibernateStore {
    public BridgeableHibernateStore(SessionFactory aSessionFactory, boolean isScrollEnabled, ScrollMode scrollMode) {
        super(aSessionFactory, isScrollEnabled, scrollMode, BridgeableHibernateTransaction::new);
    }

    @Slf4j
    public static class BridgeableHibernateTransaction extends HibernateTransaction implements BridgeableTransaction {
        protected BridgeableHibernateTransaction(Session session, boolean isScrollEnabled, ScrollMode scrollMode) {
            super(session, isScrollEnabled, scrollMode);
        }

        @Override
        public Object bridgeableLoadObject(MultiplexTransaction muxTx,
                                           Object parent,
                                           String relationName,
                                           Optional<FilterExpression> filterExpression,
                                           RequestScope scope) {
            if (parent.getClass().equals(HibernateUser.class) && "specialAction".equals(relationName)) {
                EntityDictionary dictionary = scope.getDictionary();
                Class<?> entityClass = dictionary.getParameterizedType(parent, relationName);
                HibernateUser user = (HibernateUser) parent;
                return muxTx.loadObject(entityClass,
                        String.valueOf(user.getSpecialActionId()),
                        Optional.empty(),
                        scope);
            }
            log.error("Tried to bridge from parent: {} to relation name: {}", parent, relationName);
            throw new RuntimeException("Unsupported bridging attempted!");
        }

        @Override
        public Object bridgeableLoadObjects(MultiplexTransaction muxTx,
                                            Object parent,
                                            String relationName,
                                            Optional<FilterExpression> filterExpressionOptional,
                                            RequestScope scope) {
            if (parent.getClass().equals(HibernateUser.class) && "redisActions".equals(relationName)) {
                EntityDictionary dictionary = scope.getDictionary();
                Class<?> entityClass = dictionary.getParameterizedType(parent, relationName);
                FilterExpression filterExpression = new Predicate(
                        new Predicate.PathElement(entityClass, "redisActions", String.class, "user_id"),
                        Operator.IN,
                        Collections.singletonList(String.valueOf(((HibernateUser) parent).getId()))
                );
                return muxTx.loadObjects(entityClass,
                        Optional.of(filterExpression),
                        Optional.empty(),
                        Optional.empty(),
                        scope);
            }
            log.error("Tried to bridge from parent: {} to relation name: {}", parent, relationName);
            throw new RuntimeException("Unsupported bridging attempted!");
        }
    }
}
