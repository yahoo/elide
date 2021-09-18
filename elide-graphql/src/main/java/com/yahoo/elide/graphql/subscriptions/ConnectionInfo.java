package com.yahoo.elide.graphql.subscriptions;

import com.yahoo.elide.core.security.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ConnectionInfo {
    /**
     * Return an Elide user object for the session.
     * @return Elide user.
     */
    User getUser();


    /**
     * Return the URL path for this request.
     * @return URL path.
     */
    String getBaseUrl();

    /**
     * Get a map of parameters for the session.
     * @return map of parameters.
     */
    Map<String, List<String>> getParameters();
}
