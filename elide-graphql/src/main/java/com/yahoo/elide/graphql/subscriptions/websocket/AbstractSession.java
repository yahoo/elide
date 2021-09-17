/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.yahoo.elide.graphql.parser.SubscriptionEntityProjectionMaker;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Given that web socket APIs are all different (across platforms), this class provides an abstraction
 * with all the common logic needed to pull subscription messages from Elide.
 * @param <T> The platform specific session type.
 */
@Slf4j
public abstract class AbstractSession<T extends Closeable> implements Closeable {
    protected DataStore topicStore;
    protected DataStoreTransaction transaction;
    protected Elide elide;
    protected GraphQL api;
    protected UUID requestID;
    protected T wrappedSession;

    public AbstractSession(
            T wrappedSession,
            DataStore topicStore,
            Elide elide,
            GraphQL api,
            UUID requestID) {
        this.wrappedSession = wrappedSession;
        this.topicStore = topicStore;
        this.elide = elide;
        this.api = api;
        this.requestID = requestID;
        this.transaction = null;
    }

    /**
     * Return an Elide user object for the session.
     * @return Elide user.
     */
    public abstract User getUser();

    /**
     * Send a text message on the native session.
     * @param message The message to send.
     * @throws IOException
     */
    public abstract void sendMessage(String message) throws IOException;

    /**
     * Return the URL path for this request.
     * @return URL path.
     */
    public abstract String getBaseUrl();

    /**
     * Get a map of parameters for the session.
     * @return map of parameters.
     */
    public abstract Map<String, List<String>> getParameters();

    /**
     * Close this session.
     * @throws IOException
     */
    public synchronized void close() throws IOException {
        if (transaction != null) {
            transaction.close();
            elide.getTransactionRegistry().removeRunningTransaction(requestID);
            transaction = null;
        }
        wrappedSession.close();
    }

    public void handleRequest(String request) {
        synchronized (this) {
            if (transaction != null) {
                throw new BadRequestException("Cannot handle more than a single simultaneous request.");
            }
            transaction = topicStore.beginTransaction();
            elide.getTransactionRegistry().addRunningTransaction(requestID, transaction);
        }

        ElideSettings settings = elide.getElideSettings();

        GraphQLProjectionInfo projectionInfo =
                new SubscriptionEntityProjectionMaker(settings, new HashMap<>(), NO_VERSION)
                        .make(request);

        GraphQLRequestScope requestScope = new GraphQLRequestScope(
                getBaseUrl(),
                transaction,
                getUser(),
                NO_VERSION,
                settings,
                projectionInfo,
                requestID,
                getParameters());

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(request)
                .localContext(requestScope)
                .build();

        ExecutionResult executionResult = api.execute(executionInput);

        if (! (executionResult.getData() instanceof Publisher)) {
            sendMessage(executionResult);
            safeClose();
            return;
        }

        Publisher<ExecutionResult> resultPublisher = executionResult.getData();

        if (resultPublisher == null) {
            sendMessage(executionResult);
            safeClose();
            return;
        }

        AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

        resultPublisher.subscribe(new Subscriber<ExecutionResult>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscriptionRef.set(subscription);
                subscription.request(1);
            }

            @Override
            public void onNext(ExecutionResult executionResult) {
                sendMessage(executionResult);
                subscriptionRef.get().request(1);
            }

            @Override
            public void onError(Throwable t) {
                log.error(t.getMessage());
                safeClose();
            }

            @Override
            public void onComplete() {
                safeClose();
            }
        });
    }

    protected void sendMessage(ExecutionResult result) {
        try {
            sendMessage(result.toString());
        } catch (IOException e) {
            safeClose();
        }
    }

    protected void safeClose() {
        try {
            close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
