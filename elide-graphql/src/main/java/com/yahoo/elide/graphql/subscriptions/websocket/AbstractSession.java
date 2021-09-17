package com.yahoo.elide.graphql.subscriptions.websocket;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
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

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractSession implements Closeable {
    protected DataStore topicStore;
    protected DataStoreTransaction transaction;
    protected Elide elide;
    protected GraphQL api;
    protected UUID requestID;

    public AbstractSession(
            DataStore topicStore,
            Elide elide,
            GraphQL api,
            UUID requestID) {
        this.topicStore = topicStore;
        this.elide = elide;
        this.api = api;
        this.requestID = requestID;
        this.transaction = null;
    }

    public abstract User getUser();

    public abstract void sendMessage(String message) throws IOException;

    public abstract String getBaseUrl();

    public abstract Map<String, List<String>> getParameters();

    public void close() throws IOException {
        if (transaction != null) {
            transaction.close();
            elide.getTransactionRegistry().removeRunningTransaction(requestID);
        }
    }

    public void handleRequest(String request) {
        transaction = topicStore.beginTransaction();
        elide.getTransactionRegistry().addRunningTransaction(requestID, transaction);

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
                //LOG
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
            //LOG something.
        }
    }
}
