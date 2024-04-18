/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.audit;

import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.PersistentResource;
import com.paiondata.elide.core.security.User;

import java.util.Optional;

/**
 * Elide audit entity for a CRUD action.
 */
public interface LogMessage {

    /**
     * Gets message.
     *
     * @return the message
     */
    public String getMessage();

    /**
     * Gets operation code.  The operation code is assigned by the developer to uniquely identify
     * the type of change that is being audited.  Operation code definitions are outside the scope of Elide.
     *
     * @return the operation code
     */
    public int getOperationCode();

    /**
     * Get the user principal associated with the request.
     *
     * @return the user principal.
     */
    public User getUser();

    /**
     * Get the change specification.
     *
     * @return the change specification.
     */
    public Optional<ChangeSpec> getChangeSpec();

    /**
     * Get the resource that was manipulated.
     *
     * @return the resource.
     */
    public PersistentResource getPersistentResource();
}
