/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.yahoo.elide.graphql.parser.SubscriptionEntityProjectionMaker;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Complete;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Error;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Next;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Ping;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles either a single GraphQL schema request or a subscription request.
 */
@Slf4j
public class RequestHandler implements Closeable {
    protected DataStore topicStore;
    protected DataStoreTransaction transaction;
    protected Elide elide;
    protected GraphQL api;
    protected UUID requestID;
    protected String protocolID;
    protected SessionHandler sessionHandler;
    protected ConnectionInfo connectionInfo;
    protected boolean sendPingOnSubscribe;

    /**
     * Constructor.
     * @param sessionHandler Handles all requests for the given session.
     * @param topicStore The JMS data store.
     * @param elide Elide instance.
     * @param api GraphQL api.
     * @param protocolID The graphql-ws protocol message ID this request.
     * @param requestID The Elide request UUID for this request.
     * @param connectionInfo Meta info about the session.
     */
    public RequestHandler(
            SessionHandler sessionHandler,
            DataStore topicStore,
            Elide elide,
            GraphQL api,
            String protocolID,
            UUID requestID,
            ConnectionInfo connectionInfo,
            boolean sendPingOnSubscribe) {
        this.sessionHandler = sessionHandler;
        this.topicStore = topicStore;
        this.elide = elide;
        this.api = api;
        this.requestID = requestID;
        this.protocolID = protocolID;
        this.connectionInfo = connectionInfo;
        this.transaction = null;
        this.sendPingOnSubscribe = sendPingOnSubscribe;
    }

    /**
     * Close this session.
     * @throws IOException
     */
    public void close() throws IOException {
        log.debug("Closing Request");
        synchronized (this) {
            if (transaction != null) {
                transaction.close();
                elide.getTransactionRegistry().removeRunningTransaction(requestID);
            }
        }
        sessionHandler.close(protocolID);
    }

    /**
     * Handles an incoming GraphQL query.
     * @param subscribeRequest The GraphQL query.
     * @return true if the request is still active (ie. - a subscription).
     */
    public void handleRequest(Subscribe subscribeRequest) {
        if (transaction != null) {
            throw new IllegalStateException("Already handling an active request.");
        }

        boolean isVerbose = false;

        try {
            ExecutionResult executionResult;

            synchronized (this) {
                transaction = topicStore.beginReadTransaction();
                elide.getTransactionRegistry().addRunningTransaction(requestID, transaction);

                ElideSettings settings = elide.getElideSettings();

                GraphQLProjectionInfo projectionInfo =
                        new SubscriptionEntityProjectionMaker(settings,
                                subscribeRequest.getPayload().getVariables(),
                                connectionInfo.getGetApiVersion()).make(subscribeRequest.getPayload().getQuery());

                GraphQLRequestScope requestScope = new GraphQLRequestScope(
                        connectionInfo.getBaseUrl(),
                        transaction,
                        connectionInfo.getUser(),
                        connectionInfo.getGetApiVersion(),
                        settings,
                        projectionInfo,
                        requestID,
                        connectionInfo.getParameters());

                isVerbose = requestScope.getPermissionExecutor().isVerbose();

                ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                        .query(subscribeRequest.getPayload().getQuery())
                        .operationName(subscribeRequest.getPayload().getOperationName())
                        .variables(subscribeRequest.getPayload().getVariables())
                        .localContext(requestScope)
                        .build();

                log.info("Processing GraphQL query:\n{}", subscribeRequest.getPayload().getQuery());

                executionResult = api.execute(executionInput);
            }

            //GraphQL schema requests or other queries will take this route.
            if (!(executionResult.getData() instanceof Publisher)) {
                safeSendNext(executionResult);
                safeSendComplete();
                safeClose();
                return;
            }

            Publisher<ExecutionResult> resultPublisher = executionResult.getData();

            //This would be a subscription creation error.
            if (resultPublisher == null) {
                safeSendError(executionResult.getErrors().toArray(GraphQLError[]::new));
                safeClose();
                return;
            }

            //This would be a subscription creation success.
            resultPublisher.subscribe(new ExecutionResultSubscriber());

        //This would be a subscription creation error.
        } catch (RuntimeException e) {
            log.error("RuntimeException: {}", e.getMessage());
            ElideResponse response = QueryRunner.handleRuntimeException(elide, e, isVerbose);
            safeSendError(response.getBody());
            safeClose();
        }
    }

    protected void safeSendPing() {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        Ping ping = new Ping();

        try {
            sessionHandler.sendMessage(mapper.writeValueAsString(ping));
        } catch (IOException e) {
            log.error(e.getMessage());
            safeClose();
        }
    }

    protected void safeSendNext(ExecutionResult result) {
        log.debug("Sending Next {}", result);
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        Next next = Next.builder()
                .result(result)
                .id(protocolID)
                .build();
        try {
            sessionHandler.sendMessage(mapper.writeValueAsString(next));
        } catch (IOException e) {
            log.error(e.getMessage());
            safeClose();
        }
    }

    protected void safeSendComplete() {
        log.debug("Sending Complete");
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        Complete complete = Complete.builder()
                .id(protocolID)
                .build();
        try {
            sessionHandler.sendMessage(mapper.writeValueAsString(complete));
        } catch (IOException e) {
            log.error(e.getMessage());
            safeClose();
        }
    }

    protected void safeSendError(GraphQLError[] errors) {
        log.debug("Sending Error {}", errors);
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        Error error = Error.builder()
                .id(protocolID)
                .payload(errors)
                .build();
        try {
            sessionHandler.sendMessage(mapper.writeValueAsString(error));
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

            if (sendPingOnSubscribe) {
                safeSendPing();
            }
            subscription.request(1);
        }

        @Override
        public void onNext(ExecutionResult executionResult) {
            log.debug("Next Result");
            safeSendNext(executionResult);
            subscriptionRef.get().request(1);
        }

        @Override
        public void onError(Throwable t) {
            log.error("Topic Error {}", t.getMessage());
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
