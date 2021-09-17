package com.yahoo.elide.graphql.subscriptions.websocket;

import java.io.IOException;

public class AbstractSubscriptionWebSocket {
    //TODO - add a set of open sessions and add session management.

    public void onOpen(AbstractSession session) throws IOException {
        //NOOP
    }

    public void onMessage(AbstractSession session, String message) throws IOException {
        session.handleRequest(message);
    }

    public void onClose(AbstractSession session) throws IOException {
        session.safeClose();
    }

    public void onError(AbstractSession session, Throwable throwable) {
        //LOG
        session.safeClose();
    }
}
