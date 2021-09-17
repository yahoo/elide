/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.ErrorObjects;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.yahoo.elide.graphql.parser.GraphQLQuery;
import com.yahoo.elide.graphql.parser.QueryParser;
import com.yahoo.elide.graphql.parser.SubscriptionEntityProjectionMaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Given that web socket APIs are all different across platforms, this class provides an abstraction
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

    /**
     * Constructor.
     * @param wrappedSession The underlying platform session object.
     * @param topicStore The JMS data store.
     * @param elide Elide instance.
     * @param api GraphQL api.
     * @param requestID The request UUID for this request.
     */
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

    /**
     * Handles an incoming GraphQL query.
     * @param request The GraphyQL query.
     */
    public synchronized void handleRequest(String request) {
        if (transaction != null) {
            throw new BadRequestException("Cannot handle more than a single simultaneous request.");
        }

        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();

        List<GraphQLQuery> queries = new ArrayList<>();
        try {
            queries = new QueryParser() {
            }.parseDocument(request, mapper);
        } catch (IOException e) {
            safeSendErrorMessage(ErrorObjects.builder().addError()
                    .withDetail("Bad Request Body'" + request + "'").build());
            return;
        }

        if (queries.size() != 1) {
            safeSendErrorMessage(ErrorObjects.builder().addError()
                    .withDetail("No valid queries found '" + request + "'").build());
            return;
        }

        //TODO - check for 'query' in the query.
        GraphQLQuery query = queries.get(0);

        boolean isVerbose = false;

        try {
            transaction = topicStore.beginReadTransaction();
            elide.getTransactionRegistry().addRunningTransaction(requestID, transaction);

            ElideSettings settings = elide.getElideSettings();

            //TODO - API version is needed here.
            GraphQLProjectionInfo projectionInfo =
                    new SubscriptionEntityProjectionMaker(settings, new HashMap<>(), NO_VERSION)
                            .make(query.getQuery());

            GraphQLRequestScope requestScope = new GraphQLRequestScope(
                    getBaseUrl(),
                    transaction,
                    getUser(),
                    NO_VERSION,
                    settings,
                    projectionInfo,
                    requestID,
                    getParameters());

            isVerbose = requestScope.getPermissionExecutor().isVerbose();

            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .query(query.getQuery())
                    .operationName(query.getOperationName())
                    .variables(query.getVariables())
                    .localContext(requestScope)
                    .build();

            //TODO - log the query.

            ExecutionResult executionResult = api.execute(executionInput);

            //GraphQL schema requests or other queries will take this route.
            //There is no need to close the transaction or session.
            if (!(executionResult.getData() instanceof Publisher)) {
                safeSendMessage(executionResult);
                return;
            }

            Publisher<ExecutionResult> resultPublisher = executionResult.getData();

            if (resultPublisher == null) {
                safeSendMessage(executionResult);
                safeClose();
                return;
            }

            resultPublisher.subscribe(new ExecutionResultSubscriber());
        } catch (RuntimeException e) {
            ElideResponse response = QueryRunner.handleRuntimeException(elide, e, isVerbose);
            safeSendMessage(response.getBody());
            safeClose();
        }
    }

    protected void safeSendMessage(ExecutionResult result) {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        try {
            safeSendMessage(mapper.writeValueAsString(result));
        } catch (IOException e) {
            log.error(e.getMessage());
            safeClose();
        }
    }


    protected void safeSendErrorMessage(ErrorObjects errorObjects) {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        try {
            String errorMessage = mapper.writeValueAsString(errorObjects);
            safeSendMessage(errorMessage);
            log.debug("Invalid Request: {}", errorMessage);
        } catch (IOException e) {
            log.error(e.getMessage());
            safeClose();
        }
    }

    protected void safeSendMessage(String message) {
        try {
            sendMessage(message);
        } catch (IOException e) {
            log.error(e.getMessage());
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

    /**
     * Reactive subscriber for GraphQL results.
     */
    private class ExecutionResultSubscriber implements Subscriber<ExecutionResult> {

        AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

        @Override
        public void onSubscribe(Subscription subscription) {
            subscriptionRef.set(subscription);
            subscription.request(1);
        }

        @Override
        public void onNext(ExecutionResult executionResult) {
            safeSendMessage(executionResult);
            subscriptionRef.get().request(1);
        }

        @Override
        public void onError(Throwable t) {
            log.error(t.getMessage());
            safeClose();
        }

        @Override
        public void onComplete() {
            log.debug("Topic was terminated");
            safeClose();
        }
    }
}
