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
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.yahoo.elide.graphql.parser.SubscriptionEntityProjectionMaker;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Complete;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Error;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Next;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Subscribe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import graphql.ErrorClassification;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
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
public abstract class RequestHandler<T extends Closeable> implements Closeable {
    protected DataStore topicStore;
    protected DataStoreTransaction transaction;
    protected Elide elide;
    protected GraphQL api;
    protected UUID requestID;
    protected String protocolID;
    protected T wrappedSession;

    /**
     * Constructor.
     * @param wrappedSession The underlying platform session object.
     * @param topicStore The JMS data store.
     * @param elide Elide instance.
     * @param api GraphQL api.
     * @param protocolID The graphql-ws protocol message ID this request.
     * @param requestID The Elide request UUID for this request.
     */
    public RequestHandler(
            T wrappedSession,
            DataStore topicStore,
            Elide elide,
            GraphQL api,
            String protocolID,
            UUID requestID) {
        this.wrappedSession = wrappedSession;
        this.topicStore = topicStore;
        this.elide = elide;
        this.api = api;
        this.requestID = requestID;
        this.protocolID = protocolID;
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
        }
        wrappedSession.close();
    }

    /**
     * Handles an incoming GraphQL query.
     * @param subscribeRequest The GraphyQL query.
     */
    public synchronized void handleRequest(Subscribe subscribeRequest) {
        if (transaction != null) {
            throw new IllegalStateException("Already handling an active request.");
        }

        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();

        boolean isVerbose = false;

        try {
            transaction = topicStore.beginReadTransaction();
            elide.getTransactionRegistry().addRunningTransaction(requestID, transaction);

            ElideSettings settings = elide.getElideSettings();

            //TODO - API version is needed here.
            GraphQLProjectionInfo projectionInfo =
                    new SubscriptionEntityProjectionMaker(settings, new HashMap<>(), NO_VERSION)
                            .make(subscribeRequest.getQuery());

            GraphQLRequestScope requestScope = new GraphQLRequestScope(
                    getBaseUrl(),
                    transaction,
                    getUser(),
                    NO_VERSION, //TODO - API version needs to be set correctly.
                    settings,
                    projectionInfo,
                    requestID,
                    getParameters());

            isVerbose = requestScope.getPermissionExecutor().isVerbose();

            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .query(subscribeRequest.getQuery())
                    .operationName(subscribeRequest.getOperationName())
                    .variables(subscribeRequest.getVariables())
                    .localContext(requestScope)
                    .build();

            //TODO - log the query.

            ExecutionResult executionResult = api.execute(executionInput);

            //GraphQL schema requests or other queries will take this route.
            if (!(executionResult.getData() instanceof Publisher)) {
                safeSendNext(executionResult);
                safeSendComplete();
                safeClose();
                return;
            }

            Publisher<ExecutionResult> resultPublisher = executionResult.getData();

            if (resultPublisher == null) {
                safeSendError(executionResult.getErrors().toArray(GraphQLError[]::new));
                safeClose();
                return;
            }

            resultPublisher.subscribe(new ExecutionResultSubscriber());
        } catch (RuntimeException e) {
            ElideResponse response = QueryRunner.handleRuntimeException(elide, e, isVerbose);
            safeSendError(response.getBody());
            safeClose();
            return;
        }
    }

    protected void safeSendNext(ExecutionResult result) {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        Next next = Next.builder()
                .result(result)
                .id(protocolID)
                .build();
        try {
            sendMessage(mapper.writeValueAsString(next));
        } catch (IOException e) {
            log.error(e.getMessage());
            safeClose();
        }
    }

    protected void safeSendComplete() {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        Complete complete = Complete.builder()
                .id(protocolID)
                .build();
        try {
            sendMessage(mapper.writeValueAsString(complete));
        } catch (IOException e) {
            log.error(e.getMessage());
            safeClose();
        }
    }

    protected void safeSendError(GraphQLError[] errors) {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        Error error = Error.builder()
                .id(protocolID)
                .payload(errors)
                .build();
        try {
            sendMessage(mapper.writeValueAsString(error));
        } catch (IOException e) {
            log.error(e.getMessage());
            safeClose();
        }
    }


    protected void safeSendError(String message) {
        GraphQLError error = new GraphQLError() {
            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public List<SourceLocation> getLocations() {
                return null;
            }

            @Override
            public ErrorClassification getErrorType() {
                return null;
            }
        };

        GraphQLError[] errors = new GraphQLError[1];
        errors[0] = error;
        safeSendError(errors);
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
            safeSendNext(executionResult);
            subscriptionRef.get().request(1);
        }

        @Override
        public void onError(Throwable t) {
            log.error(t.getMessage());
            safeSendError(t.getMessage());
            safeClose();
        }

        @Override
        public void onComplete() {
            log.debug("Topic was terminated");
            safeSendComplete();
            safeClose();
        }
    }
}
